package emberwild;

import emberwild.game.Game;
import emberwild.server.WebServer;
import emberwild.story.Campaign;
import emberwild.story.CampaignRegistry;
import emberwild.story.Emberwild;
import emberwild.util.Json;

import java.awt.Desktop;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

/**
 * EMBERWILD — a multiplayer D&amp;D (2024 rules) campaign engine in Java.
 * <pre>
 *   java -jar emberwild.jar                  start the table on port 7777 and open the browser
 *   java -jar emberwild.jar --port 8080      pick a port
 *   java -jar emberwild.jar --no-browser     don't open a browser (servers / headless)
 *   java -jar emberwild.jar --validate FILE  check a campaign file (.json or .java) and exit
 *   java -jar emberwild.jar --selftest 5     let the AI play the whole campaign 5 times and report
 * </pre>
 */
public final class Main {
    public static final String VERSION = "1.0.0";

    public static void main(String[] args) throws Exception {
        int port = 7777;
        boolean browser = true;
        int selftest = 0;
        String validate = null;
        Path home = Paths.get(System.getProperty("emberwild.home", ".")).toAbsolutePath().normalize();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--no-browser", "--headless" -> browser = false;
                case "--selftest" -> selftest = i + 1 < args.length && args[i + 1].matches("\\d+") ? Integer.parseInt(args[++i]) : 3;
                case "--validate" -> validate = args[++i];
                case "--home" -> home = Paths.get(args[++i]).toAbsolutePath().normalize();
                case "--help", "-h" -> { usage(); return; }
                default -> { System.err.println("Unknown option " + args[i]); usage(); return; }
            }
        }
        banner();
        Path campaigns = home.resolve("campaigns"), saves = home.resolve("saves");
        CampaignRegistry registry = new CampaignRegistry(campaigns);
        Campaign core = new Emberwild().build();
        core.source = "builtin";
        registry.register(core);
        List<String> coreProblems = registry.validate(core, false);
        for (String p : coreProblems) if (p.startsWith("ERROR") || p.startsWith("WARN")) System.out.println("  [core] " + p);

        if (validate != null) { System.exit(validateFile(registry, Paths.get(validate)) ? 0 : 1); return; }

        seedExamples(campaigns);
        System.out.println("Loading campaigns from " + campaigns + " ...");
        registry.loadFolder();

        if (selftest > 0) { System.exit(emberwild.game.SelfTest.run(registry, saves, selftest) ? 0 : 1); return; }

        Game game = new Game(registry, saves);
        WebServer server;
        try {
            server = new WebServer(game, registry, port);
        } catch (java.net.BindException e) {
            System.err.println("\nPort " + port + " is already in use. Is Emberwild already running? Try: --port " + (port + 1));
            return;
        }
        server.start();
        System.out.println();
        System.out.println("  +--------------------------------------------------------------+");
        System.out.println("  |  The table is ready!                                         |");
        System.out.println("  +--------------------------------------------------------------+");
        List<String> urls = server.urls();
        System.out.println("  You (host):      " + urls.get(0));
        for (int i = 1; i < urls.size(); i++) System.out.println("  Friends on LAN:  " + urls.get(i));
        System.out.println("  Over the internet: share your port " + port + " (port-forward), or use a tunnel such as");
        System.out.println("                     tailscale / ngrok http " + port + " / cloudflared tunnel --url http://localhost:" + port);
        System.out.println("  Campaigns folder:  " + campaigns);
        System.out.println("  Saves folder:      " + saves);
        System.out.println("  Press Ctrl+C to stop.\n");
        if (browser) openBrowser(urls.get(0));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { System.out.println("\nThe tavern lights go out. Farewell!"); server.stop(); game.shutdown(); }));
        Thread.currentThread().join();
    }

    static void usage() {
        System.out.println("""
                Emberwild %s — multiplayer D&D campaign engine
                  --port N          listen on port N (default 7777)
                  --no-browser      don't open a browser window
                  --home DIR        where campaigns/ and saves/ live (default: current folder)
                  --validate FILE   validate a campaign .json or .java and exit
                  --selftest [N]    AI plays the full campaign N times and reports
                """.formatted(VERSION));
    }

    static void banner() {
        // ASCII only: Windows consoles don't always speak UTF-8.
        System.out.println("""

                   _____ __  __ ____  _____ ______        _____ _     ____
                  | ____|  \\/  | __ )| ____|  _ \\ \\      / /_ _| |   |  _ \\
                  |  _| | |\\/| |  _ \\|  _| | |_) \\ \\ /\\ / / | || |   | | | |
                  | |___| |  | | |_) | |___|  _ < \\ V  V /  | || |___| |_| |
                  |_____|_|  |_|____/|_____|_| \\_\\ \\_/\\_/  |___|_____|____/
                             ~ The Cinder Tyrant ~   v%s   (D&D 2024 rules)
                """.formatted(VERSION));
    }

    static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) { Desktop.getDesktop().browse(new URI(url)); return; }
        } catch (Throwable ignored) {}
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            else if (os.contains("mac")) new ProcessBuilder("open", url).start();
            else new ProcessBuilder("xdg-open", url).start();
        } catch (Exception e) { System.out.println("  (Open " + url + " in your browser.)"); }
    }

    /** First run: drop example community campaigns into campaigns/ so people can see how it's done. */
    static void seedExamples(Path dir) {
        try {
            Files.createDirectories(dir);
            Path marker = dir.resolve(".seeded");
            if (Files.exists(marker)) return;
            for (String f : List.of("TheSunkenBell.java", "the_lost_lantern.json")) {
                try (InputStream in = Main.class.getResourceAsStream("/templates/" + f)) {
                    if (in != null && !Files.exists(dir.resolve(f))) Files.copy(in, dir.resolve(f));
                }
            }
            try (InputStream in = Main.class.getResourceAsStream("/templates/README.md")) {
                if (in != null && !Files.exists(dir.resolve("README.md"))) Files.copy(in, dir.resolve("README.md"));
            }
            Files.writeString(marker, "Example campaigns were copied here on first run. Delete this file to restore them.\n");
        } catch (Exception e) { System.err.println("Could not seed example campaigns: " + e.getMessage()); }
    }

    static boolean validateFile(CampaignRegistry registry, Path p) {
        try {
            Campaign c;
            if (p.toString().endsWith(".java")) c = CampaignRegistry.compileJava(p);
            else c = Campaign.fromJson(Json.parseObject(Files.readString(p)));
            for (var m : c.monsters) emberwild.rules.Monsters.register(m);
            List<String> probs = registry.validate(c, true);
            System.out.println("Campaign: " + c.title + " by " + c.author);
            boolean ok = true;
            for (String s : probs) { System.out.println("  " + s); if (s.startsWith("ERROR")) ok = false; }
            System.out.println(ok ? "[OK] Looks good!" : "[!!] Please fix the errors above.");
            return ok;
        } catch (Exception e) {
            System.out.println("[!!] " + e.getMessage());
            return false;
        }
    }
}
