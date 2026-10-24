package emberwild.story;

import emberwild.rules.Items;
import emberwild.rules.Monsters;
import emberwild.util.Json;

import javax.tools.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the core campaign plus every community campaign in the campaigns/ folder:
 * <ul>
 *   <li>*.json — data campaigns (from the Campaign Forge or written by hand). Loaded automatically.</li>
 *   <li>*.java — campaigns written against the Java DSL. Compiled in-process with the JDK compiler.
 *       Java uploaded over the network waits in campaigns/pending until the host approves it, because it runs as code.</li>
 * </ul>
 */
public final class CampaignRegistry {
    private final Map<String, Campaign> campaigns = new ConcurrentHashMap<>();
    private final Map<String, List<String>> loadErrors = new ConcurrentHashMap<>();
    private final Path dir;

    public CampaignRegistry(Path dir) { this.dir = dir; }

    public Path dir() { return dir; }
    public Path pendingDir() { return dir.resolve("pending"); }

    public Campaign get(String id) { return id == null ? null : campaigns.get(id); }
    public Collection<Campaign> all() {
        List<Campaign> l = new ArrayList<>(campaigns.values());
        l.sort(Comparator.comparing((Campaign c) -> !c.core).thenComparing(c -> c.title));
        return l;
    }
    public Map<String, List<String>> loadErrors() { return loadErrors; }

    public synchronized void register(Campaign c) {
        for (Monsters.MonsterDef m : c.monsters) Monsters.register(m);
        for (Map<String, Object> it : c.items) Items.register(it);
        campaigns.put(c.id, c);
    }

    /** Hooks from every other campaign that attach to the given anchors. */
    public List<Map<String, Object>> hooksFor(Collection<String> anchors, String currentCampaign) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (anchors.isEmpty()) return out;
        for (Campaign c : all()) {
            if (c.id.equals(currentCampaign)) continue;
            for (Campaign.Hook h : c.hooks) {
                if (anchors.contains(h.anchor())) out.add(Json.obj("campaign", c.id, "label", h.label(), "entry", h.entry(), "author", c.author, "title", c.title));
            }
        }
        return out;
    }

    // -------------------------------------------------------------------------------- loading
    public void loadFolder() {
        try {
            Files.createDirectories(dir);
            Files.createDirectories(pendingDir());
        } catch (IOException e) { System.err.println("Could not create " + dir + ": " + e.getMessage()); return; }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            List<Path> files = new ArrayList<>();
            for (Path p : ds) if (Files.isRegularFile(p)) files.add(p);
            files.sort(Comparator.comparing(Path::toString));
            for (Path p : files) {
                String n = p.getFileName().toString();
                if (n.endsWith(".json")) loadJsonFile(p);
                else if (n.endsWith(".java")) loadJavaFile(p);
            }
        } catch (IOException e) {
            System.err.println("Could not read campaigns folder: " + e.getMessage());
        }
    }

    public Campaign loadJsonFile(Path p) {
        String key = p.getFileName().toString();
        try {
            Campaign c = Campaign.fromJson(Json.parseObject(Files.readString(p, StandardCharsets.UTF_8)));
            c.source = "json:" + key;
            List<String> problems = validate(c, false);
            if (problems.stream().anyMatch(s -> s.startsWith("ERROR"))) { loadErrors.put(key, problems); System.err.println("  ! " + key + ": " + problems); return null; }
            register(c);
            loadErrors.remove(key);
            System.out.println("  + Loaded campaign \"" + c.title + "\" by " + c.author + " (" + key + ")");
            return c;
        } catch (Exception e) {
            loadErrors.put(key, List.of("ERROR " + e.getMessage()));
            System.err.println("  ! " + key + ": " + e.getMessage());
            return null;
        }
    }

    public Campaign loadJavaFile(Path p) {
        String key = p.getFileName().toString();
        try {
            Campaign c = compileJava(p);
            c.source = "java:" + key;
            List<String> problems = validate(c, false);
            if (problems.stream().anyMatch(s -> s.startsWith("ERROR"))) { loadErrors.put(key, problems); System.err.println("  ! " + key + ": " + problems); return null; }
            register(c);
            loadErrors.remove(key);
            System.out.println("  + Compiled & loaded Java campaign \"" + c.title + "\" by " + c.author + " (" + key + ")");
            return c;
        } catch (Exception e) {
            loadErrors.put(key, List.of("ERROR " + e.getMessage()));
            System.err.println("  ! " + key + ": " + e.getMessage());
            return null;
        }
    }

    /** Compile a single-file CampaignScript against the running game and instantiate it. */
    public static Campaign compileJava(Path source) throws Exception {
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        if (jc == null) throw new IllegalStateException("Java campaigns need a JDK (not just a JRE) to compile. Install a JDK 21+ or submit JSON instead.");
        Path out = Files.createTempDirectory("emberwild-campaign");
        String cp = System.getProperty("java.class.path");
        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm = jc.getStandardFileManager(diags, Locale.ROOT, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjects(source.toFile());
            List<String> opts = List.of("-classpath", cp, "-d", out.toString(), "-proc:none", "-nowarn", "-encoding", "UTF-8");
            boolean ok = jc.getTask(null, fm, diags, opts, null, units).call();
            if (!ok) {
                StringBuilder sb = new StringBuilder("Compile failed:");
                for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
                    if (d.getKind() == Diagnostic.Kind.ERROR) sb.append(" [line ").append(d.getLineNumber()).append("] ").append(d.getMessage(Locale.ROOT));
                }
                throw new IllegalArgumentException(sb.toString());
            }
        }
        URLClassLoader loader = new URLClassLoader(new URL[]{out.toUri().toURL()}, CampaignRegistry.class.getClassLoader());
        List<Path> classes = new ArrayList<>();
        try (var walk = Files.walk(out)) { walk.filter(f -> f.toString().endsWith(".class")).forEach(classes::add); }
        for (Path cf : classes) {
            String name = out.relativize(cf).toString().replace(File.separatorChar, '.').replaceAll("\\.class$", "");
            if (name.contains("$")) continue;
            Class<?> k = loader.loadClass(name);
            if (CampaignScript.class.isAssignableFrom(k)) {
                CampaignScript script = (CampaignScript) k.getDeclaredConstructor().newInstance();
                Campaign c = script.build();
                if (c == null) throw new IllegalArgumentException(name + ".build() returned null");
                return c;
            }
        }
        throw new IllegalArgumentException("No public class implementing emberwild.story.CampaignScript was found");
    }

    // -------------------------------------------------------------------------------- submission
    /** Save a submitted JSON campaign. Returns the validation report. */
    public Map<String, Object> submitJson(String body, boolean dryRun) {
        List<String> problems;
        Campaign c;
        try {
            c = Campaign.fromJson(Json.parseObject(body));
        } catch (Exception e) {
            return Json.obj("ok", false, "problems", List.of("ERROR Could not read JSON: " + e.getMessage()));
        }
        problems = validate(c, true);
        boolean ok = problems.stream().noneMatch(s -> s.startsWith("ERROR"));
        if (ok && !dryRun) {
            try {
                Path p = dir.resolve(c.id + ".json");
                Files.writeString(p, Json.pretty(c), StandardCharsets.UTF_8);
                c.source = "json:" + p.getFileName();
                register(c);
            } catch (IOException e) { return Json.obj("ok", false, "problems", List.of("ERROR Could not save: " + e.getMessage())); }
        }
        return Json.obj("ok", ok, "id", c.id, "title", c.title, "problems", problems, "summary", c.summary(), "saved", ok && !dryRun);
    }

    /** Java source goes to pending/ - it is code, so the host must approve it before it runs. */
    public Map<String, Object> submitJava(String fileName, String body) {
        String safe = fileName == null ? "Campaign.java" : Paths.get(fileName).getFileName().toString().replaceAll("[^A-Za-z0-9_.]", "");
        if (!safe.endsWith(".java")) safe = safe + ".java";
        if (body.length() > 400_000) return Json.obj("ok", false, "problems", List.of("ERROR File too large"));
        try {
            Files.createDirectories(pendingDir());
            Files.writeString(pendingDir().resolve(safe), body, StandardCharsets.UTF_8);
        } catch (IOException e) { return Json.obj("ok", false, "problems", List.of("ERROR " + e.getMessage())); }
        return Json.obj("ok", true, "pending", true, "file", safe,
                "problems", List.of("INFO Java campaigns run as code, so they wait for the host to approve them (Campaign Forge → Pending)."));
    }

    public List<String> pending() {
        List<String> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(pendingDir(), "*.java")) { for (Path p : ds) out.add(p.getFileName().toString()); }
        catch (IOException ignored) {}
        Collections.sort(out);
        return out;
    }

    public String pendingSource(String file) {
        try { return Files.readString(pendingDir().resolve(Paths.get(file).getFileName().toString())); } catch (IOException e) { return ""; }
    }

    public Map<String, Object> approve(String file) {
        Path src = pendingDir().resolve(Paths.get(file).getFileName().toString());
        if (!Files.exists(src)) return Json.obj("ok", false, "problems", List.of("ERROR No such pending file"));
        try {
            Campaign c = compileJava(src);
            List<String> problems = validate(c, true);
            if (problems.stream().anyMatch(s -> s.startsWith("ERROR"))) return Json.obj("ok", false, "problems", problems);
            Path dest = dir.resolve(src.getFileName());
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
            c.source = "java:" + dest.getFileName();
            register(c);
            return Json.obj("ok", true, "id", c.id, "title", c.title, "problems", problems);
        } catch (Exception e) {
            return Json.obj("ok", false, "problems", List.of("ERROR " + e.getMessage()));
        }
    }

    public boolean reject(String file) {
        try { return Files.deleteIfExists(pendingDir().resolve(Paths.get(file).getFileName().toString())); } catch (IOException e) { return false; }
    }

    // -------------------------------------------------------------------------------- validation
    private static final Set<String> SPECIAL_TARGETS = Set.of("@return", "@end", "@checkpoint", "@restart");
    private static final Set<String> OPS = Set.of("flag", "unflag", "item", "takeItem", "gold", "xp", "heal", "damage", "rest", "level", "log",
            "inspiration", "condition", "companion", "dismiss", "attitude", "counter", "summon", "lava", "say", "weaken");

    /** Returns problems, each prefixed ERROR / WARN / INFO. */
    public List<String> validate(Campaign c, boolean strictIds) {
        List<String> p = new ArrayList<>();
        if (c.title == null || c.title.isBlank()) p.add("ERROR Campaign needs a title");
        if (c.scenes.isEmpty()) { p.add("ERROR Campaign has no scenes"); return p; }
        if (!c.scenes.containsKey(c.start)) p.add("ERROR Start scene '" + c.start + "' does not exist");
        if (strictIds && campaigns.containsKey(c.id) && campaigns.get(c.id).core) p.add("ERROR '" + c.id + "' is reserved by the core campaign; pick another id");
        Set<String> monsterIds = new HashSet<>();
        for (Monsters.MonsterDef m : c.monsters) monsterIds.add(m.id);
        Set<String> itemIds = new HashSet<>();
        for (Map<String, Object> it : c.items) itemIds.add(Json.str(it, "id", ""));
        Set<String> targets = new HashSet<>();
        for (Campaign.Scene s : c.scenes.values()) {
            String where = "Scene '" + s.id + "'";
            if (s.text.isBlank()) p.add("WARN " + where + " has no narration text");
            if (s.choices.isEmpty() && s.autoGo.isEmpty() && !s.id.equals("end")) p.add("WARN " + where + " has no choices (it will offer 'The End')");
            if (!s.autoGo.isEmpty()) targets.add(s.autoGo);
            checkEffects(p, where, s.onEnter, itemIds);
            for (Campaign.Choice ch : s.choices) {
                String cw = where + " choice \"" + ch.label + "\"";
                if (!ch.go.isEmpty()) targets.add(ch.go);
                for (Campaign.Outcome o : new Campaign.Outcome[]{ch.pass, ch.fail, ch.victory, ch.defeat}) {
                    if (o == null) continue;
                    if (!o.go().isEmpty()) targets.add(o.go());
                    checkEffects(p, cw, o.effects(), itemIds);
                }
                checkEffects(p, cw, ch.effects, itemIds);
                if (!ch.check.isEmpty()) {
                    String k = ch.check.startsWith("save:") ? ch.check.substring(5) : ch.check;
                    if (!k.equals("luck") && emberwild.rules.Skill.of(k) == null && emberwild.rules.Ability.of(k) == null) p.add("ERROR " + cw + " checks unknown skill/ability '" + ch.check + "'");
                    if (ch.dc < 1 || ch.dc > 30) p.add("WARN " + cw + " has unusual DC " + ch.dc);
                    if (ch.pass == null || ch.fail == null) p.add("WARN " + cw + " should define both pass and fail outcomes");
                }
                if (!ch.spell.isEmpty() && emberwild.rules.Spells.get(ch.spell) == null) p.add("ERROR " + cw + " requires unknown spell '" + ch.spell + "'");
                if (!ch.battle.isEmpty()) {
                    if (!c.encounters.containsKey(ch.battle)) p.add("ERROR " + cw + " starts unknown encounter '" + ch.battle + "'");
                    if (ch.victory == null) p.add("WARN " + cw + " has no victory outcome");
                }
                if (ch.go.isEmpty() && ch.check.isEmpty() && ch.battle.isEmpty() && ch.spell.isEmpty() && ch.effects.isEmpty())
                    p.add("WARN " + cw + " does nothing (no go/check/battle/spell/effects)");
            }
        }
        for (String t : targets) if (!c.scenes.containsKey(t) && !SPECIAL_TARGETS.contains(t)) p.add("ERROR Link to missing scene '" + t + "'");
        for (Campaign.Encounter e : c.encounters.values()) {
            String where = "Encounter '" + e.id + "'";
            if (e.map.isEmpty()) { p.add("ERROR " + where + " has no map"); continue; }
            if (e.map.size() > 24) p.add("ERROR " + where + " map taller than 24 rows");
            int w = 0; boolean party = false;
            for (String row : e.map) { w = Math.max(w, row.length()); if (row.indexOf('P') >= 0) party = true; }
            if (w > 32) p.add("ERROR " + where + " map wider than 32 columns");
            if (!party) p.add("ERROR " + where + " map needs at least one 'P' (party start) tile");
            boolean enemy = false;
            for (String row : e.map) for (char ch : row.toCharArray()) {
                String mid = e.legend.get(String.valueOf(ch));
                if (mid != null) { enemy = true; if (!monsterIds.contains(mid) && !Monsters.exists(mid)) p.add("ERROR " + where + " uses unknown monster '" + mid + "'"); }
                else if (Character.isLetter(ch) && "PEATLB".indexOf(ch) < 0) p.add("ERROR " + where + " map letter '" + ch + "' has no legend entry (terrain letters are P E A T L B)");
                else if (!Character.isLetter(ch) && "#.,~=^ ".indexOf(ch) < 0) p.add("WARN " + where + " map symbol '" + ch + "' is unknown and will be treated as floor");
            }
            if (!enemy && !e.objective.equals("survive")) p.add("WARN " + where + " has no enemies on the map");
            for (String x : e.extraPerHero) if (!monsterIds.contains(x) && !Monsters.exists(x)) p.add("ERROR " + where + " extra monster '" + x + "' unknown");
        }
        for (Campaign.Hook h : c.hooks) {
            if (!c.scenes.containsKey(h.entry())) p.add("ERROR Hook '" + h.label() + "' points to missing scene '" + h.entry() + "'");
        }
        if (c.hooks.isEmpty() && !c.core) p.add("INFO No hooks: this tale is playable on its own but won't appear inside other campaigns. Add a hook to 'tavern', 'road', 'camp', 'ruins' or 'epilogue' to mesh it into Emberwild.");
        // reachability
        Set<String> seen = new HashSet<>();
        Deque<String> q = new ArrayDeque<>();
        q.add(c.start);
        for (Campaign.Hook h : c.hooks) q.add(h.entry());
        while (!q.isEmpty()) {
            String id = q.poll();
            if (!seen.add(id)) continue;
            Campaign.Scene s = c.scenes.get(id);
            if (s == null) continue;
            if (!s.autoGo.isEmpty()) q.add(s.autoGo);
            for (Campaign.Choice ch : s.choices) {
                if (!ch.go.isEmpty()) q.add(ch.go);
                for (Campaign.Outcome o : new Campaign.Outcome[]{ch.pass, ch.fail, ch.victory, ch.defeat}) if (o != null && !o.go().isEmpty()) q.add(o.go());
            }
        }
        for (String id : c.scenes.keySet()) if (!seen.contains(id)) p.add("WARN Scene '" + id + "' can never be reached");
        int battles = c.encounters.size(), scenes = c.scenes.size();
        p.add("INFO " + scenes + " scenes, " + battles + " encounters, " + c.monsters.size() + " custom monsters, " + c.hooks.size() + " hooks");
        return p;
    }

    private void checkEffects(List<String> p, String where, List<Map<String, Object>> fx, Set<String> itemIds) {
        for (Map<String, Object> f : fx) {
            String op = Json.str(f, "op", "");
            if (!OPS.contains(op)) p.add("ERROR " + where + " has unknown effect op '" + op + "'");
            if ((op.equals("item") || op.equals("takeItem"))) {
                String it = Json.str(f, "item", "");
                if (Items.item(it) == null && !itemIds.contains(it)) p.add("ERROR " + where + " gives unknown item '" + it + "'");
            }
            if (op.equals("damage") && !emberwild.rules.Dice.valid(Json.str(f, "dice", ""))) p.add("ERROR " + where + " damage dice '" + Json.str(f, "dice", "") + "' is not valid");
        }
    }

    /** Export a template / the example JSON for authors. */
    public static String readResource(String path) {
        try (InputStream in = CampaignRegistry.class.getResourceAsStream(path)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { return null; }
    }
}
