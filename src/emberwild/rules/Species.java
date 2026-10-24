package emberwild.rules;

import emberwild.util.Json;

import java.util.*;

/** 2024 species and lineages. Traits are flags the engine checks by id. */
public final class Species {

    public static final class Lineage {
        public final String id, name, desc;
        public final List<String> cantrips = new ArrayList<>();
        public final Set<String> traits = new LinkedHashSet<>();
        public int speed = 0;      // overrides species speed if > 0
        public String damageType = ""; // dragonborn / tiefling flavour
        Lineage(String id, String name, String desc) { this.id = id; this.name = name; this.desc = desc; }
        Lineage cantrip(String c) { cantrips.add(c); return this; }
        Lineage trait(String... t) { traits.addAll(Arrays.asList(t)); return this; }
        Lineage speed(int s) { speed = s; return this; }
        Lineage dmg(String t) { damageType = t; return this; }
        Object json() { return Json.obj("id", id, "name", name, "desc", desc, "cantrips", cantrips, "traits", traits, "speed", speed, "damageType", damageType); }
    }

    public static final class SpeciesDef implements Json.JsonAble {
        public final String id, name, desc;
        public int speed = 30;
        public int darkvision = 0;
        public final Set<String> traits = new LinkedHashSet<>();
        public final List<Skill> skills = new ArrayList<>();
        public final Map<String, Lineage> lineages = new LinkedHashMap<>();
        public final List<String[]> traitText = new ArrayList<>();

        SpeciesDef(String id, String name, String desc) { this.id = id; this.name = name; this.desc = desc; }

        SpeciesDef speed(int s) { speed = s; return this; }
        SpeciesDef dark(int d) { darkvision = d; return this; }
        SpeciesDef trait(String id, String name, String text) { traits.add(id); traitText.add(new String[]{name, text}); return this; }
        SpeciesDef skill(Skill s) { skills.add(s); return this; }
        Lineage lineage(String id, String name, String desc) { Lineage l = new Lineage(id, name, desc); lineages.put(id, l); return l; }

        public Lineage lineage(String id) {
            if (lineages.isEmpty()) return null;
            Lineage l = id == null ? null : lineages.get(id);
            return l != null ? l : lineages.values().iterator().next();
        }

        @Override public Object toJson() {
            List<Object> lin = new ArrayList<>();
            for (Lineage l : lineages.values()) lin.add(l.json());
            List<Object> tt = new ArrayList<>();
            for (String[] t : traitText) tt.add(Json.obj("name", t[0], "text", t[1]));
            List<Object> sk = new ArrayList<>();
            for (Skill s : skills) sk.add(s.key());
            return Json.obj("id", id, "name", name, "desc", desc, "speed", speed, "darkvision", darkvision,
                    "traits", tt, "skills", sk, "lineages", lin);
        }
    }

    private static final Map<String, SpeciesDef> ALL = new LinkedHashMap<>();

    public static SpeciesDef get(String id) { return id == null ? null : ALL.get(id.toLowerCase(Locale.ROOT)); }
    public static Collection<SpeciesDef> all() { return ALL.values(); }

    private static SpeciesDef s(String id, String name, String desc) { SpeciesDef d = new SpeciesDef(id, name, desc); ALL.put(id, d); return d; }

    static {
        SpeciesDef aas = s("aasimar", "Aasimar", "Mortals touched by the Upper Planes, carrying a spark of celestial light.").dark(60)
                .trait("celestial_resistance", "Celestial Resistance", "Resistance to Necrotic and Radiant damage.")
                .trait("healing_hands", "Healing Hands", "Action, once per Long Rest: a touched creature regains d4s equal to your Proficiency Bonus.")
                .trait("light_bearer", "Light Bearer", "You know the Light cantrip.");
        aas.lineage("celestial", "Celestial Spark", "Radiant soul.").cantrip("light");

        SpeciesDef dr = s("dragonborn", "Dragonborn", "Proud, draconic people who breathe the element of their ancestors.").dark(60)
                .trait("breath_weapon", "Breath Weapon", "Replace an attack: 15-ft cone, Dexterity save (DC 8 + Con + PB) or 1d10 damage of your ancestry. Uses = Proficiency Bonus per Long Rest.")
                .trait("draconic_resistance", "Damage Resistance", "Resistance to your ancestry's damage type.");
        dr.lineage("red", "Red (Fire)", "Fire breath.").dmg("fire");
        dr.lineage("blue", "Blue (Lightning)", "Lightning breath.").dmg("lightning");
        dr.lineage("white", "White (Cold)", "Cold breath.").dmg("cold");
        dr.lineage("black", "Black (Acid)", "Acid breath.").dmg("acid");
        dr.lineage("green", "Green (Poison)", "Poison breath.").dmg("poison");

        SpeciesDef dw = s("dwarf", "Dwarf", "Stout folk raised from the earth, enduring as stone.").dark(120)
                .trait("dwarven_resilience", "Dwarven Resilience", "Resistance to Poison damage; Advantage on saves to avoid being Poisoned.")
                .trait("dwarven_toughness", "Dwarven Toughness", "+1 Hit Point maximum per level.")
                .trait("stonecunning", "Stonecunning", "Sense tremors and hidden passages in stone (Advantage to find stonework traps).");
        dw.lineage("hill", "Hill Dwarf", "Hearty and wise.");

        SpeciesDef el = s("elf", "Elf", "Fey-touched people of timeless grace with a deep bond to magic and the wild.").dark(60)
                .trait("fey_ancestry", "Fey Ancestry", "Advantage on saves to avoid or end the Charmed condition.")
                .trait("keen_senses", "Keen Senses", "Proficiency in Perception.")
                .trait("trance", "Trance", "You don't sleep; a 4-hour trance grants a Long Rest.")
                .skill(Skill.PERCEPTION);
        el.lineage("wood", "Wood Elf", "Swift and attuned to the forest: Speed 35 ft, Druidcraft cantrip (Longstrider at 3rd level).").speed(35).cantrip("druidcraft");
        el.lineage("high", "High Elf", "Steeped in arcane tradition: Prestidigitation cantrip.").cantrip("prestidigitation");
        el.lineage("drow", "Drow", "Of the Underdark: Darkvision 120 ft, Light cantrip.").cantrip("light").trait("superior_darkvision");

        SpeciesDef gn = s("gnome", "Gnome", "Small, clever and endlessly curious.").dark(60)
                .trait("gnomish_cunning", "Gnomish Cunning", "Advantage on Intelligence, Wisdom and Charisma saving throws.");
        gn.lineage("forest", "Forest Gnome", "Minor Illusion cantrip; speak with small beasts.").cantrip("minor_illusion");
        gn.lineage("rock", "Rock Gnome", "Mending and Prestidigitation; tinker clockwork toys.").cantrip("prestidigitation");

        SpeciesDef go = s("goliath", "Goliath", "Towering descendants of giants.").speed(35)
                .trait("stones_endurance", "Stone's Endurance", "Reaction when you take damage: reduce it by 1d12 + Constitution modifier. Uses = Proficiency Bonus per Long Rest.")
                .trait("powerful_build", "Powerful Build", "Advantage on checks to end the Grappled condition; count as Large for carrying.");
        go.lineage("stone", "Stone Giant", "Stone's Endurance.");

        SpeciesDef ha = s("halfling", "Halfling", "Small, brave and remarkably lucky.")
                .trait("lucky", "Luck", "When you roll a 1 on a d20 Test, reroll it.")
                .trait("brave", "Brave", "Advantage on saves to avoid or end the Frightened condition.")
                .trait("naturally_stealthy", "Naturally Stealthy", "Hide behind creatures larger than you.");
        ha.lineage("lightfoot", "Lightfoot", "Nimble and quiet.");

        SpeciesDef hu = s("human", "Human", "Ambitious, adaptable and found everywhere.")
                .trait("resourceful", "Resourceful", "Gain Heroic Inspiration whenever you finish a Long Rest.")
                .trait("skillful", "Skillful", "Proficiency in one extra skill (Insight).")
                .skill(Skill.INSIGHT);
        hu.lineage("human", "Human", "Versatile.");

        SpeciesDef orc = s("orc", "Orc", "Strong, tireless people blessed by the god Gruumsh with endurance.").dark(120)
                .trait("adrenaline_rush", "Adrenaline Rush", "Bonus Action: Dash and gain temporary HP equal to your Proficiency Bonus. Uses = Proficiency Bonus per rest.")
                .trait("relentless_endurance", "Relentless Endurance", "Once per Long Rest, when reduced to 0 HP but not killed outright, drop to 1 HP instead.");
        orc.lineage("orc", "Orc", "Relentless.");

        SpeciesDef ti = s("tiefling", "Tiefling", "Bearers of a fiendish legacy, wreathed in infernal power.").dark(60)
                .trait("fiendish_legacy", "Fiendish Legacy", "Infernal: Resistance to Fire damage and the Fire Bolt cantrip.")
                .trait("otherworldly_presence", "Otherworldly Presence", "You know Thaumaturgy.");
        ti.lineage("infernal", "Infernal", "Fire resistance; Fire Bolt.").cantrip("fire_bolt").cantrip("thaumaturgy").dmg("fire");
        ti.lineage("abyssal", "Abyssal", "Poison resistance; Acid Splash.").cantrip("acid_splash").cantrip("thaumaturgy").dmg("poison");
    }

    /** Damage types this hero resists because of species/lineage. */
    public static Set<String> resistances(String species, String lineage) {
        Set<String> out = new HashSet<>();
        SpeciesDef s = get(species);
        if (s == null) return out;
        Lineage l = s.lineage(lineage);
        switch (s.id) {
            case "aasimar" -> { out.add("necrotic"); out.add("radiant"); }
            case "dragonborn", "tiefling" -> { if (l != null && !l.damageType.isEmpty()) out.add(l.damageType); }
            case "dwarf" -> out.add("poison");
            default -> {}
        }
        return out;
    }
}
