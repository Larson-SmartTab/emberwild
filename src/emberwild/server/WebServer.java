package emberwild.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import emberwild.game.Game;
import emberwild.rules.*;
import emberwild.story.Campaign;
import emberwild.story.CampaignRegistry;
import emberwild.util.Json;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Zero-dependency web server: serves the browser client from the jar and exposes a small JSON API.
 * Live updates go out over Server-Sent Events, so it works through most firewalls, proxies and tunnels.
 */
public final class WebServer {
    private final Game game;
    private final CampaignRegistry registry;
    private final HttpServer http;
    public final int port;

    public WebServer(Game game, CampaignRegistry registry, int port) throws IOException {
        this.game = game;
        this.registry = registry;
        this.port = port;
        this.http = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 64);
        http.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        http.createContext("/", this::handle);
    }

    public void start() { http.start(); }
    public void stop() { http.stop(0); }

    // ------------------------------------------------------------------------------------------ routing
    private void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            if (method.equals("OPTIONS")) { send(ex, 204, "text/plain", new byte[0]); return; }
            if (path.equals("/") || path.equals("/index.html")) { resource(ex, "/web/index.html"); return; }
            if (path.startsWith("/static/")) { resource(ex, "/web/" + path.substring(8)); return; }
            if (path.equals("/favicon.ico")) { resource(ex, "/web/favicon.svg"); return; }
            if (path.equals("/api/events")) { events(ex); return; }
            if (!path.startsWith("/api/")) { text(ex, 404, "Not found"); return; }
            String pid = pid(ex);
            Map<String, Object> body = method.equals("POST") && !path.equals("/api/campaign/submit-java") ? readJson(ex) : Map.of();
            Object res = route(ex, path, pid, body);
            if (res == null) return; // already responded
            json(ex, 200, res);
        } catch (IllegalArgumentException e) {
            json(ex, 400, Json.obj("ok", false, "error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            json(ex, 500, Json.obj("ok", false, "error", e.toString()));
        }
    }

    private Object route(HttpExchange ex, String path, String pid, Map<String, Object> b) throws IOException {
        boolean local = isLocal(ex);
        switch (path) {
            case "/api/state": return rawJson(ex, game.stateJson());
            case "/api/info": return Json.obj("urls", urls(), "port", port, "local", local, "version", emberwild.Main.VERSION);
            case "/api/join": {
                Game.Player p = game.join(Json.str(b, "id", null), Json.str(b, "name", null), local);
                return Json.obj("ok", true, "id", p.id, "name", p.name, "host", p.host, "color", p.color);
            }
            case "/api/rules": return rules();
            case "/api/hero/create": return game.createHero(pid, Json.map(b, "spec") == null ? b : Json.map(b, "spec"));
            case "/api/hero/import": return game.importHero(pid, Json.map(b, "hero") == null ? b : Json.map(b, "hero"));
            case "/api/hero/pregen": return game.claimPregen(pid, Json.str(b, "campaign", ""), Json.str(b, "id", ""));
            case "/api/hero/claim": return game.claimHero(pid, Json.str(b, "hero", ""));
            case "/api/hero/remove": return Json.obj("ok", game.removeHero(pid, Json.str(b, "hero", "")));
            case "/api/hero/prepare": return game.prepare(pid, Json.str(b, "hero", ""), Json.strings(b, "spells"));
            case "/api/hero/attune": return game.attune(pid, Json.str(b, "hero", ""), Json.str(b, "item", ""));
            case "/api/hero/give": return game.giveItem(pid, Json.str(b, "from", ""), Json.str(b, "to", ""), Json.str(b, "item", ""));
            case "/api/hero/export": {
                String id = query(ex).getOrDefault("id", "");
                Map<String, Object> snap = game.snapshot();
                for (Map<String, Object> h : Json.maps(snap, "heroes")) if (id.equals(Json.str(h, "id", ""))) {
                    ex.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + Json.str(h, "name", "hero").replaceAll("[^A-Za-z0-9]", "_") + ".hero.json\"");
                    return rawJson(ex, Json.pretty(Json.obj("format", "emberwild-hero-1", "hero", h)));
                }
                return Json.obj("ok", false, "error", "No such hero");
            }
            case "/api/start": return game.start(pid, Json.str(b, "campaign", ""));
            case "/api/vote": return err(game.vote(pid, Json.str(b, "choice", "")));
            case "/api/force": game.force(pid); return Json.obj("ok", true);
            case "/api/roll": return err(game.roll(pid, Json.str(b, "hero", null), false));
            case "/api/act": return err(game.combatAction(pid, b));
            case "/api/shop": return err(game.buy(pid, Json.str(b, "hero", ""), Json.str(b, "item", ""), Json.bool(b, "sell", false)));
            case "/api/chat": game.chat(pid, Json.str(b, "text", "")); return Json.obj("ok", true);
            case "/api/dice": return err(game.freeRoll(pid, Json.str(b, "expr", "1d20")));
            case "/api/autoplay": game.setAutoplay(pid, Json.bool(b, "on", false)); return Json.obj("ok", true);
            case "/api/save": { if (!game.isHost(pid)) return deny(); return err(game.save(Json.str(b, "name", "save"))); }
            case "/api/load": { if (!game.isHost(pid)) return deny(); return err(game.load(Json.str(b, "name", "autosave"))); }
            case "/api/saves": return Json.obj("saves", game.saves());
            case "/api/campaigns": {
                List<Object> l = new ArrayList<>();
                for (Campaign c : registry.all()) l.add(c.summary());
                return Json.obj("campaigns", l, "errors", registry.loadErrors(), "pending", registry.pending());
            }
            case "/api/campaign": {
                Campaign c = registry.get(query(ex).getOrDefault("id", ""));
                if (c == null) return Json.obj("ok", false, "error", "No such campaign");
                if ("1".equals(query(ex).get("download"))) ex.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + c.id + ".json\"");
                return rawJson(ex, Json.pretty(c));
            }
            case "/api/campaign/validate": return registry.submitJson(Json.write(b), true);
            case "/api/campaign/submit": {
                Map<String, Object> r = registry.submitJson(Json.write(b), false);
                if (Json.bool(r, "saved", false)) { game.log("📜 A new tale joins the realm: \"" + Json.str(r, "title", "") + "\"", "title"); game.changed(); }
                return r;
            }
            case "/api/campaign/submit-java": {
                String src = new String(ex.getRequestBody().readNBytes(500_000), StandardCharsets.UTF_8);
                String name = query(ex).getOrDefault("name", "Campaign.java");
                Map<String, Object> r = registry.submitJava(name, src);
                if (Json.bool(r, "ok", false)) game.log("📜 A Java tale (" + Json.str(r, "file", "") + ") awaits the host's approval in the Campaign Forge.", "info");
                return r;
            }
            case "/api/campaign/pending": {
                if (!game.isHost(pid)) return deny();
                String f = query(ex).get("file");
                if (f != null) return Json.obj("file", f, "source", registry.pendingSource(f));
                return Json.obj("pending", registry.pending());
            }
            case "/api/campaign/approve": {
                if (!game.isHost(pid) || !local) return Json.obj("ok", false, "problems", List.of("ERROR Only the host, on the machine running Emberwild, can approve Java campaigns (they run as code)."));
                Map<String, Object> r = registry.approve(Json.str(b, "file", ""));
                if (Json.bool(r, "ok", false)) { game.log("📜 A new Java tale joins the realm: \"" + Json.str(r, "title", "") + "\"", "title"); game.changed(); }
                return r;
            }
            case "/api/campaign/reject": { if (!game.isHost(pid)) return deny(); return Json.obj("ok", registry.reject(Json.str(b, "file", ""))); }
            case "/api/campaign/reload": {
                if (!game.isHost(pid)) return deny();
                registry.loadFolder();
                game.changed();
                return Json.obj("ok", true, "errors", registry.loadErrors());
            }
            case "/api/template/json": return rawText(ex, CampaignRegistry.readResource("/templates/the_lost_lantern.json"), "application/json", "the_lost_lantern.json");
            case "/api/template/java": return rawText(ex, CampaignRegistry.readResource("/templates/TheSunkenBell.java"), "text/x-java", "TheSunkenBell.java");
            default: text(ex, 404, "Unknown API " + path); return null;
        }
    }

    private Map<String, Object> rules() {
        List<Object> spells = new ArrayList<>(Spells.all());
        List<Object> mons = new ArrayList<>(Monsters.all());
        List<Object> items = new ArrayList<>(Items.allItems());
        Map<String, Object> feats = new LinkedHashMap<>();
        for (var f : Backgrounds.FEATS.values()) feats.put(f.id(), Json.obj("name", f.name(), "desc", f.desc()));
        List<Object> skills = new ArrayList<>();
        for (Skill s : Skill.values()) skills.add(Json.obj("id", s.key(), "label", s.label, "ability", s.ability.key()));
        return Json.obj("classes", new ArrayList<>(Classes.all()), "species", new ArrayList<>(Species.all()),
                "backgrounds", new ArrayList<>(Backgrounds.all()), "spells", spells, "monsters", mons, "items", items,
                "feats", feats, "skills", skills);
    }

    // ------------------------------------------------------------------------------------------ SSE
    private void events(HttpExchange ex) throws IOException {
        String pid = query(ex).getOrDefault("pid", "");
        ex.getResponseHeaders().add("Content-Type", "text/event-stream; charset=utf-8");
        ex.getResponseHeaders().add("Cache-Control", "no-cache");
        ex.getResponseHeaders().add("X-Accel-Buffering", "no");
        ex.sendResponseHeaders(200, 0);
        OutputStream os = ex.getResponseBody();
        LinkedBlockingQueue<String> q = new LinkedBlockingQueue<>(500);
        Consumer<String> listener = msg -> { if (!q.offer(msg)) { q.clear(); q.offer(game.stateJson()); } };
        game.connect(pid, true);
        game.addListener(listener);
        try {
            write(os, game.stateJson());
            while (true) {
                String msg = q.poll(15, TimeUnit.SECONDS);
                if (msg == null) { os.write(": ping\n\n".getBytes(StandardCharsets.UTF_8)); os.flush(); continue; }
                write(os, msg);
            }
        } catch (IOException | InterruptedException ignored) {
        } finally {
            game.removeListener(listener);
            game.connect(pid, false);
            try { ex.close(); } catch (Exception ignored) {}
        }
    }

    private static void write(OutputStream os, String msg) throws IOException {
        os.write(("data: " + msg + "\n\n").getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    // ------------------------------------------------------------------------------------------ helpers
    private static Map<String, Object> err(String e) { return e == null ? Json.obj("ok", true) : Json.obj("ok", false, "error", e); }
    private static Map<String, Object> deny() { return Json.obj("ok", false, "error", "Only the host can do that."); }

    private static String pid(HttpExchange ex) {
        String h = ex.getRequestHeaders().getFirst("X-Player");
        if (h != null) return h;
        return query(ex).getOrDefault("pid", "");
    }

    private static boolean isLocal(HttpExchange ex) {
        InetAddress a = ex.getRemoteAddress().getAddress();
        return a.isLoopbackAddress();
    }

    static Map<String, String> query(HttpExchange ex) {
        Map<String, String> m = new HashMap<>();
        String q = ex.getRequestURI().getRawQuery();
        if (q == null) return m;
        for (String part : q.split("&")) {
            int i = part.indexOf('=');
            if (i < 0) continue;
            m.put(URLDecoder.decode(part.substring(0, i), StandardCharsets.UTF_8), URLDecoder.decode(part.substring(i + 1), StandardCharsets.UTF_8));
        }
        return m;
    }

    private static Map<String, Object> readJson(HttpExchange ex) throws IOException {
        byte[] data = ex.getRequestBody().readNBytes(2_000_000);
        if (data.length == 0) return new LinkedHashMap<>();
        Object o = Json.parse(new String(data, StandardCharsets.UTF_8));
        if (o instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) o;
            return m;
        }
        throw new IllegalArgumentException("Expected a JSON object");
    }

    private Object rawJson(HttpExchange ex, String s) throws IOException { send(ex, 200, "application/json; charset=utf-8", s.getBytes(StandardCharsets.UTF_8)); return null; }

    private Object rawText(HttpExchange ex, String s, String type, String filename) throws IOException {
        if (s == null) { text(ex, 404, "Missing template"); return null; }
        ex.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        send(ex, 200, type + "; charset=utf-8", s.getBytes(StandardCharsets.UTF_8));
        return null;
    }

    private static void json(HttpExchange ex, int code, Object o) throws IOException { send(ex, code, "application/json; charset=utf-8", Json.write(o).getBytes(StandardCharsets.UTF_8)); }
    private static void text(HttpExchange ex, int code, String s) throws IOException { send(ex, code, "text/plain; charset=utf-8", s.getBytes(StandardCharsets.UTF_8)); }

    private static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, body.length == 0 ? -1 : body.length);
        if (body.length > 0) try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        else ex.close();
    }

    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8", "js", "application/javascript; charset=utf-8", "css", "text/css; charset=utf-8",
            "svg", "image/svg+xml", "png", "image/png", "json", "application/json", "woff2", "font/woff2", "ico", "image/x-icon");

    private void resource(HttpExchange ex, String path) throws IOException {
        if (path.contains("..")) { text(ex, 400, "Bad path"); return; }
        try (InputStream in = WebServer.class.getResourceAsStream(path)) {
            if (in == null) { text(ex, 404, "Not found: " + path); return; }
            String ext = path.substring(path.lastIndexOf('.') + 1);
            send(ex, 200, TYPES.getOrDefault(ext, "application/octet-stream"), in.readAllBytes());
        }
    }

    public List<String> urls() {
        List<String> out = new ArrayList<>();
        out.add("http://localhost:" + port);
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && !a.isLinkLocalAddress()) out.add("http://" + a.getHostAddress() + ":" + port);
                }
            }
        } catch (SocketException ignored) {}
        return out;
    }
}
