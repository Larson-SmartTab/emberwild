package emberwild.story;

import emberwild.rules.Hero;
import emberwild.rules.Monsters;
import emberwild.util.Json;

import java.util.*;

/**
 * A campaign is a graph of {@link Scene}s plus the monsters, items, encounters and pre-generated heroes it needs.
 * <p>
 * The same model is produced either from JSON (Campaign Forge / *.json in the campaigns folder) or from Java
 * using the fluent DSL below (*.java in the campaigns folder implementing {@link CampaignScript}).
 * <p>
 * Campaigns plug into each other through <b>hooks</b>: a scene can declare anchors (e.g. "tavern", "road",
 * "epilogue") and any loaded campaign can attach an entry choice to an anchor. When the side-story ends with
 * {@code go("@return")}, the party walks back to where they came from. That is how team stories mesh together.
 */
public final class Campaign implements Json.JsonAble {
    public String id, title, author = "Unknown", synopsis = "", version = "1.0", start = "start";
    public String cover = "tavern";
    public boolean core = false;
    public String source = "builtin";
    public final Map<String, Scene> scenes = new LinkedHashMap<>();
    public final Map<String, Encounter> encounters = new LinkedHashMap<>();
    public final List<Monsters.MonsterDef> monsters = new ArrayList<>();
    public final List<Map<String, Object>> items = new ArrayList<>();
    public final List<Hook> hooks = new ArrayList<>();
    public final List<Hero> pregens = new ArrayList<>();
    public final Map<String, Object> shopStock = new LinkedHashMap<>();

    // ====================================================================== DSL entry points
    public static Campaign create(String id, String title, String author) {
        Campaign c = new Campaign();
        c.id = slug(id);
        c.title = title;
        c.author = author;
        return c;
    }

    public Campaign synopsis(String s) { synopsis = s; return this; }
    public Campaign start(String sceneId) { start = sceneId; return this; }
    public Campaign cover(String backdrop) { cover = backdrop; return this; }
    public Campaign version(String v) { version = v; return this; }

    /** Attach this campaign to an anchor in other campaigns ("tavern", "road", "camp", "ruins", "epilogue"). */
    public Campaign hook(String anchor, String label, String entrySceneId) {
        hooks.add(new Hook(anchor, label, entrySceneId));
        return this;
    }

    public Scene scene(String id) {
        Scene s = new Scene(this, id);
        scenes.put(id, s);
        if (scenes.size() == 1 && "start".equals(start) && !scenes.containsKey("start")) start = id;
        return s;
    }

    public Encounter encounter(String id) {
        Encounter e = new Encounter(id);
        encounters.put(id, e);
        return e;
    }

    public Monsters.MonsterDef monster(String id, String name) {
        Monsters.MonsterDef d = new Monsters.MonsterDef(id, name);
        monsters.add(d);
        return d;
    }

    public Campaign monster(Monsters.MonsterDef d) { monsters.add(d); return this; }

    public Campaign item(String id, String name, String kind, String desc, Map<String, Object> extra) {
        Map<String, Object> m = Json.obj("id", id, "name", name, "kind", kind, "desc", desc);
        if (extra != null) m.putAll(extra);
        items.add(m);
        return this;
    }

    public Campaign pregen(Hero h) { pregens.add(h); return this; }

    public static String slug(String s) {
        String r = s == null ? "campaign" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_").replaceAll("^_+|_+$", "");
        return r.isEmpty() ? "campaign" : r;
    }

    // ====================================================================== model
    public record Hook(String anchor, String label, String entry) {
        Object json() { return Json.obj("anchor", anchor, "label", label, "entry", entry); }
    }

    /** One page of the story. */
    public static final class Scene implements Json.JsonAble {
        transient final Campaign owner;
        public final String id;
        public String title = "", chapter = "", backdrop = "road", weather = "none", time = "day", music = "explore";
        public String text = "";
        public Map<String, Object> npc;            // name, title, color, glyph, line, mood
        public final List<Choice> choices = new ArrayList<>();
        public final List<Map<String, Object>> onEnter = new ArrayList<>();
        public final Set<String> anchors = new LinkedHashSet<>();
        public String autoGo = "";                 // jump straight on (after onEnter) - for pure narration chains
        public boolean shop = false, checkpoint = false;

        Scene(Campaign owner, String id) { this.owner = owner; this.id = id; }

        public Scene title(String t) { title = t; return this; }
        public Scene chapter(String c) { chapter = c; return this; }
        public Scene backdrop(String b) { backdrop = b; return this; }
        public Scene weather(String w) { weather = w; return this; }
        public Scene time(String t) { time = t; return this; }
        public Scene music(String m) { music = m; return this; }
        public Scene text(String t) { text = t; return this; }
        public Scene text(String... paragraphs) { text = String.join("\n\n", paragraphs); return this; }
        public Scene anchor(String... a) { anchors.addAll(Arrays.asList(a)); return this; }
        public Scene shop() { shop = true; return this; }
        public Scene checkpoint() { checkpoint = true; return this; }
        public Scene then(String sceneId) { autoGo = sceneId; return this; }
        public Scene onEnter(Map<String, Object>... effects) { onEnter.addAll(Arrays.asList(effects)); return this; }

        /** Speaking NPC shown with a portrait card. glyph is any emoji/character. */
        public Scene npc(String name, String title, String color, String glyph, String line) {
            npc = Json.obj("name", name, "title", title, "color", color, "glyph", glyph, "line", line);
            return this;
        }

        public Choice choice(String label) {
            Choice c = new Choice(this, "c" + (choices.size() + 1), label);
            choices.add(c);
            return c;
        }

        public Campaign done() { return owner; }

        @Override public Object toJson() {
            return Json.obj("id", id, "title", title, "chapter", chapter, "backdrop", backdrop, "weather", weather,
                    "time", time, "music", music, "text", text, "npc", npc, "choices", choices, "onEnter", onEnter,
                    "anchors", anchors, "autoGo", autoGo, "shop", shop, "checkpoint", checkpoint);
        }
    }

    /** An option the table can vote for. */
    public static final class Choice implements Json.JsonAble {
        transient final Scene scene;
        public String id, label, hint = "", icon = "";
        public final Map<String, Object> requires = new LinkedHashMap<>();
        public boolean once = false;
        public String go = "";
        public final List<Map<String, Object>> effects = new ArrayList<>();
        // checks
        public String check = "";          // skill key, ability key, or "save:dex"
        public int dc = 0;
        public String mode = "best";       // best | group | chooser | each
        public String advantageIf = "";    // flag that grants advantage
        public Outcome pass, fail;
        // spells
        public String spell = "";          // spell the caster must have; spends a slot (cantrips free)
        // battles
        public String battle = "";
        public Outcome victory, defeat;

        Choice(Scene scene, String id, String label) { this.scene = scene; this.id = id; this.label = label; }

        public Choice id(String i) { id = i; return this; }
        public Choice hint(String h) { hint = h; return this; }
        public Choice icon(String i) { icon = i; return this; }
        public Choice once() { once = true; return this; }
        public Choice go(String sceneId) { go = sceneId; return this; }
        public Choice effect(Map<String, Object>... fx) { effects.addAll(Arrays.asList(fx)); return this; }

        public Choice ifFlag(String f) { requires.put("flag", f); return this; }
        public Choice unlessFlag(String f) { requires.put("notFlag", f); return this; }
        public Choice ifItem(String i) { requires.put("item", i); return this; }
        public Choice ifClass(String c) { requires.put("class", c); return this; }
        public Choice ifSpecies(String s) { requires.put("species", s); return this; }
        public Choice ifGold(int g) { requires.put("gold", g); return this; }
        public Choice ifSkillProficient(String s) { requires.put("skill", s); return this; }
        public Choice ifVisited(String sceneId) { requires.put("visited", sceneId); return this; }
        public Choice unlessVisited(String sceneId) { requires.put("notVisited", sceneId); return this; }

        /** Ability check / skill check / saving throw ("save:dex"). */
        public Choice check(String skillOrAbility, int dc) { check = skillOrAbility; this.dc = dc; return this; }
        public Choice group() { mode = "group"; return this; }
        public Choice each() { mode = "each"; return this; }
        public Choice chooser() { mode = "chooser"; return this; }
        public Choice advantageIf(String flag) { advantageIf = flag; return this; }
        public Choice pass(String text, String goTo, Map<String, Object>... fx) { pass = new Outcome(text, goTo, Arrays.asList(fx)); return this; }
        public Choice fail(String text, String goTo, Map<String, Object>... fx) { fail = new Outcome(text, goTo, Arrays.asList(fx)); return this; }

        /** Cast a spell in the story: requires a hero who has it prepared, spends a slot, then follows {@link #pass}. */
        public Choice spell(String spellId) { spell = spellId; return this; }

        public Choice battle(String encounterId) { battle = encounterId; return this; }
        public Choice victory(String text, String goTo, Map<String, Object>... fx) { victory = new Outcome(text, goTo, Arrays.asList(fx)); return this; }
        public Choice defeat(String text, String goTo, Map<String, Object>... fx) { defeat = new Outcome(text, goTo, Arrays.asList(fx)); return this; }

        /** Continue the fluent chain with the next choice of the same scene. */
        public Choice choice(String label) { return scene.choice(label); }
        public Scene scene() { return scene; }
        public Campaign done() { return scene.owner; }

        @Override public Object toJson() {
            return Json.obj("id", id, "label", label, "hint", hint, "icon", icon, "requires", requires, "once", once,
                    "go", go, "effects", effects, "check", check, "dc", dc, "mode", mode, "advantageIf", advantageIf,
                    "pass", pass, "fail", fail, "spell", spell, "battle", battle, "victory", victory, "defeat", defeat);
        }
    }

    public record Outcome(String text, String go, List<Map<String, Object>> effects) implements Json.JsonAble {
        public Outcome { effects = effects == null ? new ArrayList<>() : new ArrayList<>(effects); text = text == null ? "" : text; go = go == null ? "" : go; }
        @Override public Object toJson() { return Json.obj("text", text, "go", go, "effects", effects); }

        static Outcome from(Map<String, Object> m) {
            if (m == null) return null;
            return new Outcome(Json.str(m, "text", ""), Json.str(m, "go", ""), Json.maps(m, "effects"));
        }
    }

    /** A tactical battle on an ASCII grid. */
    public static final class Encounter implements Json.JsonAble {
        public final String id;
        public String title = "Battle", intro = "", backdrop = "forest", music = "battle", objective = "defeat_all";
        public final List<String> map = new ArrayList<>();
        public final Map<String, String> legend = new LinkedHashMap<>();  // single letter -> monster id
        public final List<String> extraPerHero = new ArrayList<>();       // monster ids added per hero beyond 2
        public boolean nonlethal = false;
        public String surpriseIf = "";       // flag: enemies are surprised (party gets advantage on initiative + free round)
        public String weakenIf = "";         // flag: boss starts weakened
        public int roundsToSurvive = 0;
        public final List<Map<String, Object>> phases = new ArrayList<>(); // {atHpPercent, text, effects:[...]} for bosses
        public final List<Map<String, Object>> allies = new ArrayList<>(); // {monster, ifFlag} - NPCs fighting on your side

        public Encounter(String id) { this.id = id; }

        public Encounter title(String t) { title = t; return this; }
        public Encounter intro(String t) { intro = t; return this; }
        public Encounter backdrop(String b) { backdrop = b; return this; }
        public Encounter music(String m) { music = m; return this; }
        public Encounter objective(String o) { objective = o; return this; }
        public Encounter map(String... rows) { map.clear(); map.addAll(Arrays.asList(rows)); return this; }
        public Encounter legend(char c, String monsterId) { legend.put(String.valueOf(c), monsterId); return this; }
        public Encounter extra(String... monsterIds) { extraPerHero.addAll(Arrays.asList(monsterIds)); return this; }
        public Encounter nonlethal() { nonlethal = true; return this; }
        public Encounter surpriseIf(String flag) { surpriseIf = flag; return this; }
        public Encounter weakenIf(String flag) { weakenIf = flag; return this; }
        public Encounter survive(int rounds) { roundsToSurvive = rounds; objective = "survive"; return this; }
        public Encounter phase(int atHpPercent, String text, Map<String, Object>... fx) {
            phases.add(Json.obj("atHpPercent", atHpPercent, "text", text, "effects", new ArrayList<>(Arrays.asList(fx))));
            return this;
        }
        public Encounter ally(String monsterId, String ifFlag) { allies.add(Json.obj("monster", monsterId, "ifFlag", ifFlag)); return this; }

        @Override public Object toJson() {
            return Json.obj("id", id, "title", title, "intro", intro, "backdrop", backdrop, "music", music, "objective", objective,
                    "map", map, "legend", legend, "extraPerHero", extraPerHero, "nonlethal", nonlethal, "surpriseIf", surpriseIf,
                    "weakenIf", weakenIf, "roundsToSurvive", roundsToSurvive, "phases", phases, "allies", allies);
        }

        static Encounter from(String id, Map<String, Object> m) {
            Encounter e = new Encounter(id);
            e.title = Json.str(m, "title", "Battle");
            e.intro = Json.str(m, "intro", "");
            e.backdrop = Json.str(m, "backdrop", "forest");
            e.music = Json.str(m, "music", "battle");
            e.objective = Json.str(m, "objective", "defeat_all");
            e.map.addAll(Json.strings(m, "map"));
            Map<String, Object> lg = Json.map(m, "legend");
            if (lg != null) for (var en : lg.entrySet()) e.legend.put(en.getKey(), String.valueOf(en.getValue()));
            e.extraPerHero.addAll(Json.strings(m, "extraPerHero"));
            e.nonlethal = Json.bool(m, "nonlethal", false);
            e.surpriseIf = Json.str(m, "surpriseIf", "");
            e.weakenIf = Json.str(m, "weakenIf", "");
            e.roundsToSurvive = Json.integer(m, "roundsToSurvive", 0);
            e.phases.addAll(Json.maps(m, "phases"));
            e.allies.addAll(Json.maps(m, "allies"));
            return e;
        }
    }

    // ====================================================================== JSON
    @Override public Object toJson() {
        List<Object> hk = new ArrayList<>();
        for (Hook h : hooks) hk.add(h.json());
        List<Object> pg = new ArrayList<>();
        for (Hero h : pregens) pg.add(h.save());
        return Json.obj("id", id, "title", title, "author", author, "synopsis", synopsis, "version", version, "start", start,
                "cover", cover, "hooks", hk, "scenes", scenes, "encounters", encounters, "monsters", monsters, "items", items,
                "pregens", pg, "shop", shopStock);
    }

    /** Lightweight listing for the lobby. */
    public Map<String, Object> summary() {
        List<Object> hk = new ArrayList<>();
        for (Hook h : hooks) hk.add(h.json());
        int battles = encounters.size();
        int checks = 0;
        for (Scene s : scenes.values()) for (Choice c : s.choices) if (!c.check.isEmpty()) checks++;
        return Json.obj("id", id, "title", title, "author", author, "synopsis", synopsis, "version", version, "core", core,
                "cover", cover, "scenes", scenes.size(), "battles", battles, "checks", checks, "hooks", hk, "source", source,
                "pregens", pregens.size());
    }

    public static Campaign fromJson(Map<String, Object> m) {
        Campaign c = create(Json.str(m, "id", Json.str(m, "title", "campaign")), Json.str(m, "title", "Untitled Tale"), Json.str(m, "author", "Unknown"));
        c.synopsis = Json.str(m, "synopsis", "");
        c.version = Json.str(m, "version", "1.0");
        c.cover = Json.str(m, "cover", "road");
        for (Map<String, Object> h : Json.maps(m, "hooks")) c.hooks.add(new Hook(Json.str(h, "anchor", "tavern"), Json.str(h, "label", c.title), Json.str(h, "entry", "")));
        Object scenesObj = m.get("scenes");
        List<Map<String, Object>> sceneList = new ArrayList<>();
        if (scenesObj instanceof Map<?, ?> sm) {
            for (var e : sm.entrySet()) {
                @SuppressWarnings("unchecked") Map<String, Object> sv = (Map<String, Object>) e.getValue();
                if (sv != null) { sv = new LinkedHashMap<>(sv); sv.putIfAbsent("id", String.valueOf(e.getKey())); sceneList.add(sv); }
            }
        } else sceneList = Json.maps(m, "scenes");
        for (Map<String, Object> sm : sceneList) {
            Scene s = c.scene(Json.str(sm, "id", "scene" + c.scenes.size()));
            s.title = Json.str(sm, "title", "");
            s.chapter = Json.str(sm, "chapter", "");
            s.backdrop = Json.str(sm, "backdrop", "road");
            s.weather = Json.str(sm, "weather", "none");
            s.time = Json.str(sm, "time", "day");
            s.music = Json.str(sm, "music", "explore");
            Object t = sm.get("text");
            if (t instanceof List<?> tl) { List<String> parts = new ArrayList<>(); for (Object o : tl) parts.add(String.valueOf(o)); s.text = String.join("\n\n", parts); }
            else s.text = Json.str(sm, "text", "");
            s.npc = Json.map(sm, "npc");
            s.onEnter.addAll(Json.maps(sm, "onEnter"));
            s.anchors.addAll(Json.strings(sm, "anchors"));
            s.autoGo = Json.str(sm, "autoGo", "");
            s.shop = Json.bool(sm, "shop", false);
            s.checkpoint = Json.bool(sm, "checkpoint", false);
            int n = 0;
            for (Map<String, Object> cm : Json.maps(sm, "choices")) {
                Choice ch = s.choice(Json.str(cm, "label", "Continue"));
                ch.id = Json.str(cm, "id", "c" + (++n));
                ch.hint = Json.str(cm, "hint", "");
                ch.icon = Json.str(cm, "icon", "");
                Map<String, Object> rq = Json.map(cm, "requires");
                if (rq != null) ch.requires.putAll(rq);
                ch.once = Json.bool(cm, "once", false);
                ch.go = Json.str(cm, "go", "");
                ch.effects.addAll(Json.maps(cm, "effects"));
                ch.check = Json.str(cm, "check", "");
                ch.dc = Json.integer(cm, "dc", 0);
                ch.mode = Json.str(cm, "mode", "best");
                ch.advantageIf = Json.str(cm, "advantageIf", "");
                ch.pass = Outcome.from(Json.map(cm, "pass"));
                ch.fail = Outcome.from(Json.map(cm, "fail"));
                ch.spell = Json.str(cm, "spell", "");
                ch.battle = Json.str(cm, "battle", "");
                ch.victory = Outcome.from(Json.map(cm, "victory"));
                ch.defeat = Outcome.from(Json.map(cm, "defeat"));
            }
        }
        c.start = Json.str(m, "start", c.scenes.isEmpty() ? "start" : c.scenes.keySet().iterator().next());
        Map<String, Object> enc = Json.map(m, "encounters");
        if (enc != null) for (var e : enc.entrySet()) {
            @SuppressWarnings("unchecked") Map<String, Object> em = (Map<String, Object>) e.getValue();
            c.encounters.put(e.getKey(), Encounter.from(e.getKey(), em));
        }
        for (Map<String, Object> mm : Json.maps(m, "monsters")) c.monsters.add(Monsters.MonsterDef.from(mm));
        c.items.addAll(Json.maps(m, "items"));
        for (Map<String, Object> hm : Json.maps(m, "pregens")) c.pregens.add(Hero.load(hm));
        return c;
    }
}
