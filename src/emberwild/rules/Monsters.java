package emberwild.rules;

import emberwild.util.Json;

import java.util.*;

/**
 * Monster stat blocks (2024 Monster Manual style, trimmed to what the tactical engine uses).
 * Campaign authors can register their own via JSON ("monsters": [...]) or the Java DSL.
 */
public final class Monsters {

    public static final class Attack {
        public String name, damage, type;
        public int toHit, reach = 5, range = 0, longRange = 0;
        public String extra = "", extraType = "";
        public String rider = "";           // prone | grappled | poisoned | slowed | pushed | burning | frightened
        public Ability riderSave = null;
        public int riderDC = 0;
        public boolean spell;               // spell attack (for flavour / effects)

        public Attack() {}
        public Attack(String name, int toHit, String damage, String type) { this.name = name; this.toHit = toHit; this.damage = damage; this.type = type; }

        public Attack reach(int r) { reach = r; return this; }
        public Attack ranged(int r, int l) { range = r; longRange = l; return this; }
        public Attack plus(String dice, String t) { extra = dice; extraType = t; return this; }
        public Attack rider(String r, Ability save, int dc) { rider = r; riderSave = save; riderDC = dc; return this; }
        public Attack spell() { spell = true; return this; }
        public boolean isRanged() { return range > 0; }

        Object json() {
            return Json.obj("name", name, "toHit", toHit, "damage", damage, "type", type, "reach", reach, "range", range,
                    "extra", extra, "extraType", extraType, "rider", rider);
        }

        static Attack from(Map<String, Object> m) {
            Attack a = new Attack(Json.str(m, "name", "Strike"), Json.integer(m, "toHit", 3), Json.str(m, "damage", "1d6+1"), Json.str(m, "type", "bludgeoning"));
            a.reach = Json.integer(m, "reach", 5);
            a.range = Json.integer(m, "range", 0);
            a.longRange = Json.integer(m, "longRange", a.range * 3);
            a.extra = Json.str(m, "extra", "");
            a.extraType = Json.str(m, "extraType", "");
            a.rider = Json.str(m, "rider", "");
            a.riderSave = Ability.of(Json.str(m, "riderSave", null));
            a.riderDC = Json.integer(m, "riderDC", 0);
            a.spell = Json.bool(m, "spell", false);
            return a;
        }
    }

    /** Special action: breath weapons, bursts, heals, summons. */
    public static final class Special {
        public String id, name, shape = "cone"; // cone | line | sphere | self | single | heal | summon
        public int size = 15, range = 0, dc = 12, recharge = 0, uses = 0;
        public String damage = "", type = "";
        public Ability save = Ability.DEX;
        public boolean half = true, bonus = false;
        public String rider = "";
        public String summon = "";          // monster id to summon
        public int summonCount = 0;
        public String text = "";

        public Special() {}
        public Special(String id, String name) { this.id = id; this.name = name; }

        public Special shape(String s, int size, int range) { shape = s; this.size = size; this.range = range; return this; }
        public Special dmg(String d, String t) { damage = d; type = t; return this; }
        public Special dc(int d, Ability a) { dc = d; save = a; return this; }
        public Special recharge(int r) { recharge = r; return this; }
        public Special uses(int u) { uses = u; return this; }
        public Special bonus() { bonus = true; return this; }
        public Special rider(String r) { rider = r; return this; }
        public Special summon(String id, int n) { shape = "summon"; summon = id; summonCount = n; return this; }
        public Special text(String t) { text = t; return this; }
        public Special noHalf() { half = false; return this; }

        Object json() {
            return Json.obj("id", id, "name", name, "shape", shape, "size", size, "range", range, "dc", dc,
                    "recharge", recharge, "uses", uses, "damage", damage, "type", type, "save", save == null ? null : save.key(), "text", text);
        }

        static Special from(Map<String, Object> m) {
            Special s = new Special(Json.str(m, "id", "special"), Json.str(m, "name", "Special"));
            s.shape = Json.str(m, "shape", "cone");
            s.size = Json.integer(m, "size", 15);
            s.range = Json.integer(m, "range", 0);
            s.dc = Json.integer(m, "dc", 12);
            s.recharge = Json.integer(m, "recharge", 0);
            s.uses = Json.integer(m, "uses", 0);
            s.damage = Json.str(m, "damage", "");
            s.type = Json.str(m, "type", "");
            s.save = Ability.of(Json.str(m, "save", "dex"));
            s.half = Json.bool(m, "half", true);
            s.bonus = Json.bool(m, "bonus", false);
            s.rider = Json.str(m, "rider", "");
            s.summon = Json.str(m, "summon", "");
            s.summonCount = Json.integer(m, "summonCount", 0);
            s.text = Json.str(m, "text", "");
            return s;
        }
    }

    public static final class MonsterDef implements Json.JsonAble {
        public String id, name, type = "humanoid", role = "melee"; // melee | ranged | caster | skirmisher | brute | boss | object
        public int size = 1;          // squares per side (1 = Medium/Small, 2 = Large)
        public int ac = 12, hp = 10, speed = 30, fly = 0;
        public String hpDice = "";
        public int[] scores = {10, 10, 10, 10, 10, 10};
        public Set<String> saveProf = new HashSet<>();
        public int prof = 2;
        public double cr = 0.25;
        public int xp = 50;
        public Set<String> resist = new HashSet<>(), immune = new HashSet<>(), vulnerable = new HashSet<>(), condImmune = new HashSet<>();
        public List<Attack> attacks = new ArrayList<>();
        public int multiattack = 1;
        public List<Special> specials = new ArrayList<>();
        public Set<String> traits = new LinkedHashSet<>(); // pack_tactics, nimble_escape, undead_fortitude, death_burst, aggressive, magic_resistance, flame_aura, regeneration, avoid_melee
        public int legendary = 0;
        public List<String> legendaryActions = new ArrayList<>(); // "attack:Tail", "special:wing_gust", "move"
        public String color = "#a33", glyph = "☠", blurb = "";
        public boolean humanoid() { return type.equals("humanoid"); }

        public MonsterDef() {}
        public MonsterDef(String id, String name) { this.id = id; this.name = name; }

        public int mod(Ability a) { return Ability.mod(scores[a.ordinal()]); }
        public int saveBonus(Ability a) { return mod(a) + (saveProf.contains(a.key()) ? prof : 0); }

        // ---------------------------------------------------------- fluent builder (used by the Java DSL)
        public MonsterDef type(String t) { type = t; return this; }
        public MonsterDef role(String r) { role = r; return this; }
        public MonsterDef size(int s) { size = s; return this; }
        public MonsterDef ac(int a) { ac = a; return this; }
        public MonsterDef hp(int h, String dice) { hp = h; hpDice = dice; return this; }
        public MonsterDef hp(int h) { hp = h; return this; }
        public MonsterDef speed(int s) { speed = s; return this; }
        public MonsterDef fly(int f) { fly = f; return this; }
        public MonsterDef stats(int str, int dex, int con, int in, int wis, int cha) { scores = new int[]{str, dex, con, in, wis, cha}; return this; }
        public MonsterDef saves(String... a) { saveProf.addAll(Arrays.asList(a)); return this; }
        public MonsterDef cr(double c, int x) { cr = c; xp = x; prof = c >= 5 ? 3 : 2; return this; }
        public MonsterDef resist(String... t) { resist.addAll(Arrays.asList(t)); return this; }
        public MonsterDef immune(String... t) { immune.addAll(Arrays.asList(t)); return this; }
        public MonsterDef vulnerable(String... t) { vulnerable.addAll(Arrays.asList(t)); return this; }
        public MonsterDef condImmune(String... t) { condImmune.addAll(Arrays.asList(t)); return this; }
        public MonsterDef attack(Attack a) { attacks.add(a); return this; }
        public MonsterDef attack(String name, int toHit, String dmg, String type) { attacks.add(new Attack(name, toHit, dmg, type)); return this; }
        public MonsterDef multi(int n) { multiattack = n; return this; }
        public MonsterDef special(Special s) { specials.add(s); return this; }
        public MonsterDef trait(String... t) { traits.addAll(Arrays.asList(t)); return this; }
        public MonsterDef legendary(int n, String... acts) { legendary = n; legendaryActions.addAll(Arrays.asList(acts)); return this; }
        public MonsterDef look(String color, String glyph) { this.color = color; this.glyph = glyph; return this; }
        public MonsterDef blurb(String b) { blurb = b; return this; }

        @Override public Object toJson() {
            List<Object> at = new ArrayList<>(); for (Attack a : attacks) at.add(a.json());
            List<Object> sp = new ArrayList<>(); for (Special s : specials) sp.add(s.json());
            Map<String, Object> sc = new LinkedHashMap<>();
            for (Ability a : Ability.values()) sc.put(a.key(), scores[a.ordinal()]);
            return Json.obj("id", id, "name", name, "type", type, "role", role, "size", size, "ac", ac, "hp", hp, "speed", speed,
                    "fly", fly, "scores", sc, "cr", cr, "xp", xp, "resist", resist, "immune", immune, "vulnerable", vulnerable,
                    "attacks", at, "multiattack", multiattack, "specials", sp, "traits", traits, "legendary", legendary,
                    "legendaryActions", legendaryActions, "color", color, "glyph", glyph, "blurb", blurb, "saves", saveProf);
        }

        public static MonsterDef from(Map<String, Object> m) {
            MonsterDef d = new MonsterDef(Json.str(m, "id", "monster"), Json.str(m, "name", "Monster"));
            d.type = Json.str(m, "type", "humanoid");
            d.role = Json.str(m, "role", "melee");
            d.size = Math.max(1, Math.min(3, Json.integer(m, "size", 1)));
            d.ac = Json.integer(m, "ac", 12);
            d.hp = Math.max(1, Json.integer(m, "hp", 10));
            d.speed = Json.integer(m, "speed", 30);
            d.fly = Json.integer(m, "fly", 0);
            Map<String, Object> sc = Json.map(m, "scores");
            if (sc != null) for (Ability a : Ability.values()) d.scores[a.ordinal()] = Json.integer(sc, a.key(), 10);
            d.saveProf.addAll(Json.strings(m, "saves"));
            d.cr = Json.dbl(m, "cr", 0.25);
            d.xp = Json.integer(m, "xp", 50);
            d.prof = d.cr >= 5 ? 3 : 2;
            d.resist.addAll(Json.strings(m, "resist"));
            d.immune.addAll(Json.strings(m, "immune"));
            d.vulnerable.addAll(Json.strings(m, "vulnerable"));
            d.condImmune.addAll(Json.strings(m, "conditionImmune"));
            for (Map<String, Object> a : Json.maps(m, "attacks")) d.attacks.add(Attack.from(a));
            if (d.attacks.isEmpty()) d.attacks.add(new Attack("Strike", 3, "1d6+1", "bludgeoning"));
            d.multiattack = Math.max(1, Json.integer(m, "multiattack", 1));
            for (Map<String, Object> s : Json.maps(m, "specials")) d.specials.add(Special.from(s));
            d.traits.addAll(Json.strings(m, "traits"));
            d.legendary = Json.integer(m, "legendary", 0);
            d.legendaryActions.addAll(Json.strings(m, "legendaryActions"));
            d.color = Json.str(m, "color", "#a33");
            d.glyph = Json.str(m, "glyph", "☠");
            d.blurb = Json.str(m, "blurb", "");
            return d;
        }
    }

    private static final Map<String, MonsterDef> ALL = new LinkedHashMap<>();

    public static MonsterDef get(String id) { return id == null ? null : ALL.get(id); }
    public static boolean exists(String id) { return ALL.containsKey(id); }
    public static Collection<MonsterDef> all() { return ALL.values(); }

    public static synchronized MonsterDef register(MonsterDef d) { ALL.put(d.id, d); return d; }

    private static MonsterDef m(String id, String name) { return register(new MonsterDef(id, name)); }

    private static Special sp(String id, String name) { return new Special(id, name); }

    static {
        // -------------------------------------------------------------- tavern & town
        m("tavern_brawler", "Tavern Brawler").type("humanoid").role("melee").ac(11).hp(11, "2d8+2").stats(14, 12, 12, 9, 10, 10)
                .cr(0.125, 25).attack("Haymaker", 4, "1d4+2", "bludgeoning").attack(new Attack("Hurled Tankard", 3, "1d4+1", "bludgeoning").ranged(20, 60))
                .look("#b5651d", "🍺").blurb("Big arms, short temper, and four ales past sensible.");
        m("thug", "Ashen Choir Thug").type("humanoid").role("melee").ac(12).hp(16, "3d8+3").stats(15, 11, 13, 9, 10, 11)
                .cr(0.5, 100).multi(2).attack("Mace", 4, "1d6+2", "bludgeoning").attack(new Attack("Heavy Crossbow", 2, "1d10", "piercing").ranged(100, 400))
                .trait("pack_tactics").look("#5d4037", "🔨").blurb("Hired muscle bearing the cult's soot-black armband.");
        m("cultist", "Ashen Acolyte").type("humanoid").role("caster").ac(12).hp(11, "2d8+2").stats(10, 12, 12, 10, 13, 11)
                .cr(0.25, 50).attack("Ritual Sickle", 3, "1d4+1", "slashing")
                .attack(new Attack("Cinder Lash", 4, "2d4", "fire").ranged(60, 60).spell())
                .look("#7b241c", "🕯").blurb("A robed devotee whose hymns smell of smoke.");
        m("cult_fanatic", "Choir Zealot").type("humanoid").role("caster").ac(13).hp(27, "6d8").stats(11, 14, 12, 10, 13, 14)
                .cr(2, 450).saves("wis").multi(2).attack(new Attack("Sacrificial Knife", 4, "1d4+2", "piercing").plus("1d6", "fire"))
                .attack(new Attack("Searing Hymn", 4, "2d6", "fire").ranged(60, 60).spell())
                .special(sp("cinder_burst", "Cinder Burst").shape("sphere", 10, 60).dmg("2d6", "fire").dc(12, Ability.DEX).recharge(5))
                .look("#922b21", "🔥").blurb("A true believer. Their eyes glow like banked coals.");

        // -------------------------------------------------------------- wilderness
        m("wolf", "Thornwood Wolf").type("beast").role("skirmisher").ac(12).hp(11, "2d8+2").speed(40).stats(14, 15, 12, 3, 12, 6)
                .cr(0.25, 50).attack(new Attack("Bite", 4, "1d6+2", "piercing").rider("prone", Ability.STR, 11))
                .trait("pack_tactics").look("#6d6d6d", "🐺").blurb("Lean, grey and hungry, with thorn-scratched muzzles.");
        m("dire_wolf", "Ashmaw, Dire Wolf").type("beast").role("brute").size(2).ac(14).hp(22, "5d10").speed(50).stats(17, 15, 15, 3, 12, 7)
                .cr(1, 200).attack(new Attack("Bite", 5, "1d10+3", "piercing").rider("prone", Ability.STR, 13))
                .trait("pack_tactics").look("#3b3b3b", "🐺").blurb("The pack's scarred alpha, its fur dusted with cinders.");
        m("goblin", "Goblin Warrior").type("humanoid").role("skirmisher").ac(15).hp(10, "3d6").stats(8, 15, 10, 10, 8, 8)
                .cr(0.25, 50).attack("Scimitar", 4, "1d6+2", "slashing").attack(new Attack("Shortbow", 4, "1d6+2", "piercing").ranged(80, 320))
                .trait("nimble_escape").look("#558b2f", "👺").blurb("Sharp teeth, sharper scimitar, no sense of personal space.");
        m("goblin_archer", "Goblin Sniper").type("humanoid").role("ranged").ac(13).hp(10, "3d6").stats(8, 16, 10, 10, 10, 8)
                .cr(0.25, 50).attack("Dagger", 5, "1d4+3", "piercing").attack(new Attack("Shortbow", 5, "1d6+3", "piercing").ranged(80, 320))
                .trait("nimble_escape", "avoid_melee").look("#689f38", "🏹").blurb("Perched where you'd least like it to be.");
        m("snikkit", "Snikkit the Toll-Taker").type("humanoid").role("melee").ac(15).hp(20, "6d6").stats(10, 15, 12, 12, 8, 14)
                .cr(1, 200).multi(2).attack("Notched Scimitar", 4, "1d6+2", "slashing").attack(new Attack("Javelin", 2, "1d6", "piercing").ranged(30, 120))
                .trait("nimble_escape").look("#33691e", "👑").blurb("Self-crowned king of the Old Stone Bridge. The crown is a teapot lid.");
        m("bog_zombie", "Fen-Drowned Zombie").type("undead").role("brute").ac(8).hp(15, "2d8+6").speed(20).stats(13, 6, 16, 3, 6, 5)
                .cr(0.25, 50).saves("wis").immune("poison").condImmune("poisoned", "charmed", "frightened")
                .attack(new Attack("Slam", 3, "1d8+1", "bludgeoning").plus("1d4", "necrotic"))
                .trait("undead_fortitude").look("#4e5d3b", "🧟").blurb("Swamp-bloated, mossy, and absolutely relentless.");
        m("giant_frog", "Giant Marsh Toad").type("beast").role("melee").ac(11).hp(18, "4d8").speed(30).stats(12, 13, 11, 2, 10, 3)
                .cr(0.25, 50).attack(new Attack("Bite", 3, "1d6+1", "piercing").plus("1d4", "poison").rider("grappled", Ability.STR, 11))
                .look("#5f7a2e", "🐸").blurb("A toad the size of a pony with a tongue like a wet rope.");
        m("will_o_wisp", "Marsh Wisp").type("undead").role("skirmisher").ac(15).hp(13, "6d4").speed(0).fly(50).stats(1, 28, 10, 13, 14, 11)
                .cr(1, 200).resist("acid", "cold", "fire", "necrotic", "thunder", "bludgeoning", "piercing", "slashing").immune("lightning", "poison")
                .condImmune("poisoned", "prone", "grappled", "charmed")
                .attack(new Attack("Shock", 4, "2d6", "lightning").spell())
                .trait("avoid_melee").look("#9ad4ff", "✧").blurb("A drifting lantern-light that feeds on fear.");

        // -------------------------------------------------------------- dungeon
        m("skeleton", "Ember-Bound Skeleton").type("undead").role("melee").ac(14).hp(13, "2d8+4").stats(10, 16, 15, 6, 8, 5)
                .cr(0.25, 50).vulnerable("bludgeoning").immune("poison").condImmune("poisoned", "charmed", "frightened")
                .attack("Shortsword", 5, "1d6+3", "piercing").attack(new Attack("Shortbow", 5, "1d6+3", "piercing").ranged(80, 320))
                .look("#d7ccc8", "💀").blurb("Old bones held together by smouldering cult-fire.");
        m("animated_armor", "Warden Armor").type("construct").role("brute").ac(18).hp(33, "6d8+6").speed(25).stats(14, 11, 13, 1, 3, 1)
                .cr(1, 200).multi(2).immune("poison", "psychic").condImmune("charmed", "frightened", "poisoned", "paralyzed")
                .attack("Gauntlet Slam", 4, "1d6+2", "bludgeoning")
                .look("#90a4ae", "🛡").blurb("The hollow armor of a warden who swore to guard this place forever.");
        m("magma_mephit", "Magma Mephit").type("elemental").role("skirmisher").ac(11).hp(18, "4d6+4").speed(30).fly(30).stats(8, 12, 12, 7, 10, 10)
                .cr(0.5, 100).immune("fire", "poison").vulnerable("cold").condImmune("poisoned")
                .attack(new Attack("Claw", 3, "1d4+1", "slashing").plus("1d4", "fire"))
                .special(sp("fire_breath", "Fire Breath").shape("cone", 15, 0).dmg("2d6", "fire").dc(11, Ability.DEX).recharge(6))
                .trait("death_burst").look("#ff7043", "🔥").blurb("A cackling imp of slag and spite.");
        m("cindrel", "Sister Cindrel, Voice of the Choir").type("humanoid").role("caster").ac(14).hp(38, "7d8+7").stats(10, 14, 12, 13, 16, 16)
                .cr(2, 450).saves("wis", "cha").multi(2)
                .attack(new Attack("Ember Scimitar", 4, "1d6+2", "slashing").plus("1d6", "fire"))
                .attack(new Attack("Hymn of Cinders", 5, "2d8", "fire").ranged(60, 60).spell())
                .special(sp("burning_hands", "Burning Hands").shape("cone", 15, 0).dmg("3d6", "fire").dc(12, Ability.DEX).recharge(6))
                .special(sp("rally", "Choir's Call").shape("heal", 0, 0).dmg("2d8+3", "").uses(1).bonus())
                .look("#c0392b", "🎶").blurb("Once a temple singer of Harrowmere. The Choir gave her a new song.");
        m("brazier", "Binding Brazier").type("object").role("object").ac(11).hp(10).speed(0).stats(10, 1, 10, 1, 1, 1)
                .cr(0, 25).immune("fire", "poison", "psychic").condImmune("charmed", "frightened", "poisoned", "paralyzed", "prone", "incapacitated")
                .attack("None", 0, "0", "fire").look("#ff9800", "🜂").blurb("A black-iron brazier feeding the Tyrant's flame. Smash it!");

        // -------------------------------------------------------------- the boss
        m("vharkas", "Vharkas, the Cinder Tyrant").type("dragon").role("boss").size(2).ac(15).hp(76, "9d10+27").speed(40).fly(60)
                .stats(19, 12, 17, 14, 13, 16).cr(4, 1100).saves("dex", "con", "wis", "cha")
                .immune("fire").vulnerable("cold").condImmune("charmed", "frightened")
                .multi(2)
                .attack(new Attack("Bite", 6, "1d10+4", "piercing").plus("1d6", "fire").reach(10))
                .attack(new Attack("Claw", 6, "1d6+4", "slashing"))
                .special(sp("cinder_breath", "Cinder Breath").shape("cone", 30, 0).dmg("4d6", "fire").dc(13, Ability.DEX).recharge(6))
                .special(sp("wing_gust", "Wing Gust").shape("sphere", 10, 0).dmg("2d4", "bludgeoning").dc(13, Ability.STR).rider("prone"))
                .trait("flame_aura", "legendary_resistance")
                .legendary(2, "attack:Claw", "special:wing_gust", "move")
                .look("#ff5722", "🐉").blurb("A drake of living slag, sealed beneath Thornreach for three hundred years. It remembers being worshipped.");
    }

    // helper chained setters for Special
    public static Special special(String id, String name) { return new Special(id, name); }
}
