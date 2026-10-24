package emberwild.rules;

import emberwild.util.Json;

import java.util.*;

/**
 * Spell catalogue (2024 Player's Handbook versions, levels 0-2). Each spell is data; the combat engine
 * resolves attack/save/auto/heal/buff spells generically and handles the few unique riders by id.
 */
public final class Spells {
    public enum Kind { ATTACK, SAVE, AUTO, HEAL, BUFF, UTILITY }
    public enum Area { SINGLE, MULTI, CONE, SPHERE, CUBE, SELF, LINE }
    public enum Cast { ACTION, BONUS, REACTION }

    public static final class SpellDef implements Json.JsonAble {
        public String id, name, school = "evocation", desc = "";
        public int level;
        public Cast cast = Cast.ACTION;
        public int range = 60;          // feet; 0 = self, 5 = touch
        public Kind kind = Kind.ATTACK;
        public String dice = "";        // damage or healing dice (healing adds spellcasting mod)
        public String damageType = "";
        public Ability save;
        public boolean halfOnSave;
        public Area area = Area.SINGLE;
        public int areaSize;            // feet (cone length, sphere radius, cube side)
        public int targets = 1;         // MULTI: number of targets / rays / darts
        public String upcastDice = "";  // extra dice per slot above base level
        public int upcastTargets;       // extra targets per slot above base level
        public String condition = "";   // rider condition id
        public int conditionRounds = 1;
        public boolean concentration;
        public boolean cantripScales = true;
        public boolean elemental;
        public boolean targetsAllies;
        public Set<String> lists = new LinkedHashSet<>();

        SpellDef(String id, String name, int level) { this.id = id; this.name = name; this.level = level; }

        SpellDef cast(Cast c) { cast = c; return this; }
        SpellDef range(int r) { range = r; return this; }
        SpellDef attack(String d, String type) { kind = Kind.ATTACK; dice = d; damageType = type; return this; }
        SpellDef save(Ability a, String d, String type, boolean half) { kind = Kind.SAVE; save = a; dice = d; damageType = type; halfOnSave = half; return this; }
        SpellDef auto(String d, String type) { kind = Kind.AUTO; dice = d; damageType = type; return this; }
        SpellDef heal(String d) { kind = Kind.HEAL; dice = d; targetsAllies = true; return this; }
        SpellDef buff() { kind = Kind.BUFF; return this; }
        SpellDef utility() { kind = Kind.UTILITY; return this; }
        SpellDef area(Area a, int size) { area = a; areaSize = size; return this; }
        SpellDef multi(int n) { area = Area.MULTI; targets = n; return this; }
        SpellDef up(String d) { upcastDice = d; return this; }
        SpellDef upTargets(int n) { upcastTargets = n; return this; }
        SpellDef cond(String c, int rounds) { condition = c; conditionRounds = rounds; return this; }
        SpellDef conc() { concentration = true; return this; }
        SpellDef school(String s) { school = s; return this; }
        SpellDef elem() { elemental = true; return this; }
        SpellDef allies() { targetsAllies = true; return this; }
        SpellDef lists(String... l) { lists.addAll(Arrays.asList(l)); return this; }
        SpellDef desc(String d) { desc = d; return this; }

        public boolean isCantrip() { return level == 0; }

        public String rangeLabel() {
            if (range == 0) return area == Area.SELF || area == Area.SINGLE ? "Self" : "Self (" + areaSize + " ft " + area.name().toLowerCase() + ")";
            if (range == 5) return "Touch";
            return range + " ft";
        }

        @Override public Object toJson() {
            return Json.obj("id", id, "name", name, "level", level, "school", school, "cast", cast, "range", range,
                    "rangeLabel", rangeLabel(), "kind", kind, "dice", dice, "damageType", damageType,
                    "save", save == null ? null : save.key(), "area", area, "areaSize", areaSize, "targets", targets,
                    "concentration", concentration, "elemental", elemental, "targetsAllies", targetsAllies,
                    "lists", lists, "desc", desc);
        }
    }

    private static final Map<String, SpellDef> ALL = new LinkedHashMap<>();

    private static SpellDef def(String id, String name, int level) {
        SpellDef s = new SpellDef(id, name, level);
        ALL.put(id, s);
        return s;
    }

    public static SpellDef get(String id) { return id == null ? null : ALL.get(id); }

    public static Collection<SpellDef> all() { return ALL.values(); }

    public static List<SpellDef> forList(String list, int maxLevel) {
        List<SpellDef> out = new ArrayList<>();
        for (SpellDef s : ALL.values()) if (s.lists.contains(list) && s.level <= maxLevel) out.add(s);
        return out;
    }

    /** Cantrip damage dice scale at character levels 5/11/17. */
    public static String scaledCantripDice(SpellDef s, int charLevel) {
        if (!s.isCantrip() || !s.cantripScales || s.dice.isEmpty()) return s.dice;
        int mult = charLevel >= 17 ? 4 : charLevel >= 11 ? 3 : charLevel >= 5 ? 2 : 1;
        if (mult == 1) return s.dice;
        String d = s.dice;
        int idx = d.indexOf('d');
        int n = idx <= 0 ? 1 : Integer.parseInt(d.substring(0, idx));
        return (n * mult) + d.substring(idx);
    }

    static {
        // ---------------------------------------------------------------- cantrips
        def("fire_bolt", "Fire Bolt", 0).range(120).attack("1d10", "fire").elem().lists("wizard", "sorcerer")
                .desc("Hurl a mote of fire. Ranged spell attack, 1d10 fire. Unattended flammables ignite.");
        def("ray_of_frost", "Ray of Frost", 0).range(60).attack("1d8", "cold").cond("slowed", 1).elem().lists("wizard", "sorcerer")
                .desc("A frigid beam. Ranged spell attack, 1d8 cold, and the target's Speed drops by 10 ft until your next turn.");
        def("shocking_grasp", "Shocking Grasp", 0).range(5).attack("1d8", "lightning").cond("no_reactions", 1).elem().lists("wizard", "sorcerer")
                .desc("Lightning springs from your hand. Melee spell attack, 1d8 lightning; the target can't take Opportunity Attacks until its next turn.");
        def("acid_splash", "Acid Splash", 0).range(60).save(Ability.DEX, "1d6", "acid", false).area(Area.SPHERE, 5).elem().lists("wizard", "sorcerer")
                .desc("A bubble of acid bursts in a 5-ft-radius sphere. Dexterity save or 1d6 acid.");
        def("sacred_flame", "Sacred Flame", 0).range(60).save(Ability.DEX, "1d8", "radiant", false).lists("cleric").school("evocation")
                .desc("Flame-like radiance descends on a creature. Dexterity save or 1d8 radiant. Ignores cover.");
        def("toll_the_dead", "Toll the Dead", 0).range(60).save(Ability.WIS, "1d8", "necrotic", false).lists("cleric", "wizard").school("necromancy")
                .desc("A dolorous bell. Wisdom save or 1d8 necrotic (1d12 if the target is already wounded).");
        def("vicious_mockery", "Vicious Mockery", 0).range(60).save(Ability.WIS, "1d6", "psychic", false).cond("sapped", 1).lists("bard").school("enchantment")
                .desc("A string of magical insults. Wisdom save or 1d6 psychic and Disadvantage on its next attack roll.");
        def("starry_wisp", "Starry Wisp", 0).range(60).attack("1d8", "radiant").lists("bard").desc("A mote of starlight. Ranged spell attack, 1d8 radiant; the target sheds light and can't be invisible.");
        def("druidcraft", "Druidcraft", 0).range(30).utility().lists("druid").school("transmutation")
                .desc("Whisper to the wild: predict weather, make a flower bloom, or create a harmless sensory effect.");
        def("light", "Light", 0).range(5).utility().lists("wizard", "cleric", "bard", "sorcerer").desc("An object sheds bright light in a 20-ft radius for an hour.");
        def("mage_hand", "Mage Hand", 0).range(30).utility().lists("wizard", "bard", "sorcerer").school("conjuration").desc("A spectral hand manipulates objects at range.");
        def("minor_illusion", "Minor Illusion", 0).range(30).utility().lists("wizard", "bard", "sorcerer").school("illusion").desc("Create a sound or an image of an object.");
        def("prestidigitation", "Prestidigitation", 0).range(10).utility().lists("wizard", "bard", "sorcerer").school("transmutation").desc("Minor magical tricks: clean, chill, warm, flavour, sparks.");
        def("guidance", "Guidance", 0).range(5).utility().lists("cleric", "druid").school("divination").desc("Touch a willing creature: it adds 1d4 to one ability check. (Adds 1d4 to party skill checks in story scenes.)");
        def("thaumaturgy", "Thaumaturgy", 0).range(30).utility().lists("cleric").school("transmutation").desc("A minor wonder: a booming voice, tremors, flickering flames.");

        // ---------------------------------------------------------------- 1st level
        def("shield", "Shield", 1).cast(Cast.REACTION).range(0).buff().school("abjuration").lists("wizard", "sorcerer")
                .desc("Reaction when hit by an attack: +5 AC until the start of your next turn, possibly turning the hit into a miss.");
        def("mage_armor", "Mage Armor", 1).range(5).buff().school("abjuration").lists("wizard", "sorcerer")
                .desc("Base AC becomes 13 + Dexterity modifier for 8 hours (while unarmored).");
        def("magic_missile", "Magic Missile", 1).range(120).auto("1d4+1", "force").multi(3).upTargets(1).lists("wizard", "sorcerer")
                .desc("Three glowing darts that never miss, each 1d4+1 force. One more dart per slot level above 1st.");
        def("burning_hands", "Burning Hands", 1).range(0).save(Ability.DEX, "3d6", "fire", true).area(Area.CONE, 15).up("1d6").elem().lists("wizard", "sorcerer")
                .desc("A 15-ft cone of flame. Dexterity save, 3d6 fire, half on success. +1d6 per slot above 1st.");
        def("thunderwave", "Thunderwave", 1).range(0).save(Ability.CON, "2d8", "thunder", true).area(Area.CUBE, 15).up("1d8").cond("pushed", 0).elem().lists("wizard", "sorcerer", "bard")
                .desc("A 15-ft cube of thunder from you. Constitution save, 2d8 thunder and pushed 10 ft; half and no push on success.");
        def("chromatic_orb", "Chromatic Orb", 1).range(90).attack("3d8", "fire").up("1d8").elem().school("evocation").lists("wizard", "sorcerer")
                .desc("Hurl a 4-inch orb of acid, cold, fire, lightning, poison or thunder: 3d8. If two damage dice match, the orb leaps to a second target within 30 ft.");
        def("ice_knife", "Ice Knife", 1).range(60).attack("1d10", "piercing").up("1d6").elem().school("conjuration").lists("wizard", "sorcerer")
                .desc("A shard of ice: ranged spell attack for 1d10 piercing, then it explodes. Each creature within 5 ft of the target makes a Dexterity save or takes 2d6 cold.");
        def("charm_person", "Charm Person", 1).range(30).save(Ability.WIS, "", "", false).cond("charmed", 10).school("enchantment").lists("wizard", "sorcerer", "bard")
                .desc("A humanoid makes a Wisdom save (with Advantage if you're fighting it) or is Charmed by you for an hour. In story scenes, this can sway an NPC outright.");
        def("sleep", "Sleep", 1).range(60).save(Ability.WIS, "", "", false).area(Area.SPHERE, 5).cond("incapacitated", 2).conc().school("enchantment").lists("wizard", "sorcerer", "bard")
                .desc("Creatures in a 5-ft-radius sphere make Wisdom saves or are Incapacitated, then fall Unconscious if they fail again.");
        def("cure_wounds", "Cure Wounds", 1).range(5).heal("2d8").up("2d8").school("abjuration").lists("cleric", "bard", "paladin", "ranger", "druid")
                .desc("Touch: regain 2d8 + spellcasting modifier HP. +2d8 per slot above 1st.");
        def("healing_word", "Healing Word", 1).cast(Cast.BONUS).range(60).heal("2d4").up("2d4").school("abjuration").lists("cleric", "bard", "druid")
                .desc("Bonus Action, 60 ft: a creature regains 2d4 + spellcasting modifier HP. +2d4 per slot above 1st.");
        def("guiding_bolt", "Guiding Bolt", 1).range(120).attack("4d6", "radiant").up("1d6").cond("guided", 1).lists("cleric")
                .desc("A flash of light. Ranged spell attack, 4d6 radiant, and the next attack roll against the target has Advantage.");
        def("bless", "Bless", 1).range(30).buff().allies().multi(3).upTargets(1).cond("blessed", 10).conc().school("enchantment").lists("cleric", "paladin")
                .desc("Up to three creatures add 1d4 to attack rolls and saving throws for 1 minute (Concentration).");
        def("shield_of_faith", "Shield of Faith", 1).cast(Cast.BONUS).range(60).buff().allies().cond("shield_of_faith", 10).conc().school("abjuration").lists("cleric", "paladin")
                .desc("Bonus Action: a creature gains +2 AC for 10 minutes (Concentration).");
        def("hunters_mark", "Hunter's Mark", 1).cast(Cast.BONUS).range(90).buff().cond("marked", 10).conc().school("divination").lists("ranger")
                .desc("Bonus Action: mark a creature; your weapon hits deal +1d6 force damage to it (Concentration).");
        def("divine_smite", "Divine Smite", 1).cast(Cast.BONUS).range(5).buff().up("1d8").lists("paladin")
                .desc("Bonus Action right after hitting with a melee weapon: +2d8 radiant (+1d8 vs Fiends/Undead, +1d8 per slot above 1st).");
        def("dissonant_whispers", "Dissonant Whispers", 1).range(60).save(Ability.WIS, "3d6", "psychic", true).up("1d6").cond("frightened", 1).school("enchantment").lists("bard")
                .desc("A discordant melody only the target hears. Wisdom save, 3d6 psychic and it is Frightened; half on success.");
        def("faerie_fire", "Faerie Fire", 1).range(60).save(Ability.DEX, "", "", false).area(Area.SPHERE, 10).cond("outlined", 10).conc().lists("bard", "druid")
                .desc("Creatures in a 20-ft cube are outlined in light on a failed Dexterity save: attacks against them have Advantage.");
        def("heroism", "Heroism", 1).range(5).buff().allies().cond("heroism", 10).conc().school("enchantment").lists("bard", "paladin")
                .desc("A willing creature is immune to Frightened and gains temporary HP equal to your spellcasting modifier each turn.");

        // ---------------------------------------------------------------- 2nd level
        def("scorching_ray", "Scorching Ray", 2).range(120).attack("2d6", "fire").multi(3).upTargets(1).elem().lists("wizard", "sorcerer")
                .desc("Three rays of fire; make a ranged spell attack for each, 2d6 fire per hit.");
        def("shatter", "Shatter", 2).range(60).save(Ability.CON, "3d8", "thunder", true).area(Area.SPHERE, 10).up("1d8").elem().lists("wizard", "sorcerer", "bard")
                .desc("A painful ringing in a 10-ft-radius sphere. Constitution save, 3d8 thunder, half on success.");
        def("misty_step", "Misty Step", 2).cast(Cast.BONUS).range(30).utility().school("conjuration").lists("wizard", "sorcerer")
                .desc("Bonus Action: teleport up to 30 ft to a space you can see. Perfect for escaping claws.");
        def("hold_person", "Hold Person", 2).range(60).save(Ability.WIS, "", "", false).cond("paralyzed", 10).conc().school("enchantment").lists("wizard", "cleric", "bard", "sorcerer")
                .desc("A humanoid makes a Wisdom save or is Paralyzed (it repeats the save at the end of each of its turns).");
        def("spiritual_weapon", "Spiritual Weapon", 2).cast(Cast.BONUS).range(60).attack("1d8", "force").cond("spiritual_weapon", 10).conc().lists("cleric")
                .desc("Bonus Action: a floating spectral weapon strikes (1d8 + mod force) and can strike again as a Bonus Action each turn.");
        def("flaming_sphere", "Flaming Sphere", 2).cast(Cast.ACTION).range(60).save(Ability.DEX, "2d6", "fire", true).area(Area.SPHERE, 5).up("1d6").elem().conc().school("conjuration").lists("wizard", "sorcerer")
                .desc("A ball of fire rolls through the battle: creatures within 5 ft of it make a Dexterity save or take 2d6 fire.");
        def("aid", "Aid", 2).range(30).buff().allies().multi(3).cond("aided", 99).school("abjuration").lists("cleric", "paladin", "bard")
                .desc("Up to three allies' maximum and current HP increase by 5 for 8 hours.");
        def("lesser_restoration", "Lesser Restoration", 2).range(5).utility().allies().school("abjuration").lists("cleric", "bard", "paladin", "ranger")
                .desc("End one condition (Blinded, Deafened, Paralyzed or Poisoned) on a creature.");

        // ---------------------------------------------------------------- 3rd level
        def("fireball", "Fireball", 3).range(150).save(Ability.DEX, "8d6", "fire", true).area(Area.SPHERE, 20).up("1d6").elem().lists("wizard", "sorcerer")
                .desc("A bright streak blossoms into an explosion of flame. Every creature in a 20-ft-radius sphere makes a Dexterity save: 8d6 fire, half on success. +1d6 per slot above 3rd. Mind your friends.");
        def("lightning_bolt", "Lightning Bolt", 3).range(0).save(Ability.DEX, "8d6", "lightning", true).area(Area.LINE, 100).up("1d6").elem().lists("wizard", "sorcerer")
                .desc("A stroke of lightning 100 ft long and 5 ft wide blasts out from you. Dexterity save: 8d6 lightning, half on success.");
        def("spirit_guardians", "Spirit Guardians", 3).range(0).save(Ability.WIS, "3d8", "radiant", true).area(Area.SELF, 15).up("1d8").conc().school("conjuration").lists("cleric")
                .desc("Protective spirits flit around you in a 15-ft emanation. Enemies that start their turn there (or when you cast it) make a Wisdom save: 3d8 radiant, half on success (Concentration).");
        def("mass_healing_word", "Mass Healing Word", 3).cast(Cast.BONUS).range(60).heal("2d4").multi(6).up("1d4").school("abjuration").lists("cleric", "bard")
                .desc("Bonus Action: up to six creatures you can see regain 2d4 + your spellcasting modifier HP.");
        def("revivify", "Revivify", 3).range(5).utility().allies().school("necromancy").lists("cleric", "paladin")
                .desc("Touch a creature that has died within the last minute: it returns to life with 1 HP.");
        def("hypnotic_pattern", "Hypnotic Pattern", 3).range(120).save(Ability.WIS, "", "", false).area(Area.SPHERE, 15).cond("incapacitated", 10).conc().school("illusion").lists("wizard", "bard", "sorcerer")
                .desc("A twisting pattern of colours in a 30-ft cube: Wisdom save or the creature is Charmed and Incapacitated, mesmerised, until it takes damage (Concentration).");

    }
}
