package emberwild.rules;

import emberwild.util.Json;

import java.util.*;

/** The eight playable classes, levels 1-3, following the 2024 Player's Handbook. */
public final class Classes {

    public record Feature(int level, String id, String name, String desc) {
        Object json() { return Json.obj("level", level, "id", id, "name", name, "desc", desc); }
    }

    public static final class ClassDef implements Json.JsonAble {
        public final String id, name, tagline;
        public int hitDie;
        public Ability primary;
        public Ability[] saves;
        public List<Skill> skillChoices = new ArrayList<>();
        public int skillCount = 2;
        public String armor = "none";        // none | light | medium | heavy | unarmored_barbarian
        public int armorBase = 10;           // base AC of starting armor
        public int dexCap = 99;              // max DEX bonus to AC from armor
        public boolean shield;
        public List<String> weapons = new ArrayList<>();
        public List<Feature> features = new ArrayList<>();
        public String subclass = "";          // fixed subclass at level 3
        // spellcasting
        public Ability spellAbility;
        public String spellList;
        public int[] cantrips = {0, 0, 0};
        public int[] prepared = {0, 0, 0};
        public int[][] slots = {{}, {}, {}};
        public List<String> alwaysPrepared = new ArrayList<>(); // by level 1-3 via "L:spell"
        public List<String> defaultCantrips = new ArrayList<>(), defaultSpells = new ArrayList<>();
        public String color = "#888";
        public String glyph = "⚔";

        ClassDef(String id, String name, String tagline) { this.id = id; this.name = name; this.tagline = tagline; }

        public boolean caster() { return spellAbility != null; }

        public int[] slotsAt(int level) { return slots[Math.max(0, Math.min(2, level - 1))]; }

        public int cantripsAt(int level) { return cantrips[Math.max(0, Math.min(2, level - 1))]; }

        public int preparedAt(int level) { return prepared[Math.max(0, Math.min(2, level - 1))]; }

        public int maxSpellLevel(int level) { return slotsAt(level).length; }

        public List<String> alwaysPreparedAt(int level) {
            List<String> out = new ArrayList<>();
            for (String s : alwaysPrepared) {
                String[] p = s.split(":");
                if (Integer.parseInt(p[0]) <= level) out.add(p[1]);
            }
            return out;
        }

        public List<Feature> featuresUpTo(int level) {
            List<Feature> out = new ArrayList<>();
            for (Feature f : features) if (f.level <= level) out.add(f);
            return out;
        }

        @Override public Object toJson() {
            List<Object> f = new ArrayList<>();
            for (Feature x : features) f.add(x.json());
            List<Object> sk = new ArrayList<>();
            for (Skill s : skillChoices) sk.add(s.key());
            List<Object> sl = new ArrayList<>();
            for (int[] row : slots) { List<Object> r = new ArrayList<>(); for (int v : row) r.add(v); sl.add(r); }
            return Json.obj("id", id, "name", name, "tagline", tagline, "hitDie", hitDie, "primary", primary.key(),
                    "saves", List.of(saves[0].key(), saves[1].key()), "skillChoices", sk, "skillCount", skillCount,
                    "weapons", weapons, "features", f, "subclass", subclass,
                    "spellAbility", spellAbility == null ? null : spellAbility.key(), "spellList", spellList,
                    "cantrips", cantrips, "prepared", prepared, "slots", sl,
                    "defaultCantrips", defaultCantrips, "defaultSpells", defaultSpells, "color", color, "glyph", glyph);
        }
    }

    private static final Map<String, ClassDef> ALL = new LinkedHashMap<>();

    public static ClassDef get(String id) { return id == null ? null : ALL.get(id.toLowerCase(Locale.ROOT)); }
    public static Collection<ClassDef> all() { return ALL.values(); }

    private static ClassDef c(String id, String name, String tag) { ClassDef d = new ClassDef(id, name, tag); ALL.put(id, d); return d; }

    private static List<Skill> sk(Skill... s) { return new ArrayList<>(Arrays.asList(s)); }

    static {
        ClassDef b = c("barbarian", "Barbarian", "A fierce warrior fuelled by primal Rage.");
        b.hitDie = 12; b.primary = Ability.STR; b.saves = new Ability[]{Ability.STR, Ability.CON};
        b.skillChoices = sk(Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.INTIMIDATION, Skill.NATURE, Skill.PERCEPTION, Skill.SURVIVAL);
        b.armor = "unarmored_barbarian"; b.weapons = List.of("greataxe", "handaxe");
        b.subclass = "Path of the Berserker"; b.color = "#c0392b"; b.glyph = "🪓";
        b.features.addAll(List.of(
                new Feature(1, "rage", "Rage", "Bonus Action: +2 damage on Strength attacks, Resistance to bludgeoning, piercing and slashing, Advantage on Strength checks and saves. 2 uses (3 at level 3)."),
                new Feature(1, "unarmored_defense", "Unarmored Defense", "AC = 10 + Dexterity + Constitution while not wearing armor."),
                new Feature(1, "weapon_mastery", "Weapon Mastery", "Use the mastery properties of your weapons (Greataxe: Cleave, Handaxe: Vex)."),
                new Feature(2, "reckless_attack", "Reckless Attack", "Attack with Advantage on Strength attacks this turn; attacks against you have Advantage until your next turn."),
                new Feature(2, "danger_sense", "Danger Sense", "Advantage on Dexterity saving throws."),
                new Feature(3, "frenzy", "Frenzy (Berserker)", "While raging, the first Reckless hit each turn deals an extra 2d6 damage."),
                new Feature(3, "primal_knowledge", "Primal Knowledge", "Gain proficiency in another skill; use Strength for some checks while raging.")));

        ClassDef bd = c("bard", "Bard", "An inspiring magician whose power echoes the music of creation.");
        bd.hitDie = 8; bd.primary = Ability.CHA; bd.saves = new Ability[]{Ability.DEX, Ability.CHA};
        bd.skillChoices = sk(Skill.values()); bd.skillCount = 3;
        bd.armor = "light"; bd.armorBase = 11; bd.weapons = List.of("rapier", "dagger");
        bd.subclass = "College of Lore"; bd.color = "#8e44ad"; bd.glyph = "🎻";
        bd.spellAbility = Ability.CHA; bd.spellList = "bard"; bd.cantrips = new int[]{2, 2, 2}; bd.prepared = new int[]{4, 5, 6};
        bd.slots = new int[][]{{2}, {3}, {4, 2}};
        bd.defaultCantrips = List.of("vicious_mockery", "starry_wisp");
        bd.defaultSpells = List.of("healing_word", "dissonant_whispers", "charm_person", "faerie_fire", "shatter", "heroism");
        bd.features.addAll(List.of(
                new Feature(1, "bardic_inspiration", "Bardic Inspiration", "Bonus Action: give an ally a d6 they add to a failed d20 Test. Uses = Charisma modifier per Long Rest."),
                new Feature(1, "spellcasting", "Spellcasting", "Cast bard spells using Charisma."),
                new Feature(2, "expertise", "Expertise", "Double proficiency in two skills (Persuasion and Performance)."),
                new Feature(2, "jack_of_all_trades", "Jack of All Trades", "Add half your Proficiency Bonus to ability checks without proficiency."),
                new Feature(3, "cutting_words", "Cutting Words (Lore)", "Reaction: spend Bardic Inspiration to subtract the die from an enemy's attack roll, possibly turning a hit into a miss."),
                new Feature(3, "bonus_proficiencies", "Bonus Proficiencies", "Gain proficiency in three skills.")));

        ClassDef cl = c("cleric", "Cleric", "A priestly champion who wields divine magic.");
        cl.hitDie = 8; cl.primary = Ability.WIS; cl.saves = new Ability[]{Ability.WIS, Ability.CHA};
        cl.skillChoices = sk(Skill.HISTORY, Skill.INSIGHT, Skill.MEDICINE, Skill.PERSUASION, Skill.RELIGION);
        cl.armor = "medium"; cl.armorBase = 13; cl.dexCap = 2; cl.shield = true; cl.weapons = List.of("mace");
        cl.subclass = "Life Domain"; cl.color = "#f1c40f"; cl.glyph = "☀";
        cl.spellAbility = Ability.WIS; cl.spellList = "cleric"; cl.cantrips = new int[]{3, 3, 3}; cl.prepared = new int[]{4, 5, 6};
        cl.slots = new int[][]{{2}, {3}, {4, 2}};
        cl.alwaysPrepared = List.of("3:aid", "3:bless", "3:cure_wounds", "3:lesser_restoration");
        cl.defaultCantrips = List.of("sacred_flame", "toll_the_dead", "guidance");
        cl.defaultSpells = List.of("healing_word", "guiding_bolt", "bless", "shield_of_faith", "spiritual_weapon", "hold_person");
        cl.features.addAll(List.of(
                new Feature(1, "spellcasting", "Spellcasting", "Cast cleric spells using Wisdom."),
                new Feature(1, "divine_order", "Divine Order (Protector)", "Trained for the front line: heavy-armor discipline grants +1 AC."),
                new Feature(2, "channel_divinity", "Channel Divinity", "2 uses per rest. Divine Spark: heal an ally 1d8 + Wisdom, or blast a foe for 1d8 + Wisdom radiant. Turn Undead: undead nearby flee."),
                new Feature(3, "disciple_of_life", "Disciple of Life (Life Domain)", "Healing spells restore an extra 2 + the spell's level HP."),
                new Feature(3, "preserve_life", "Preserve Life", "Channel Divinity: restore HP equal to 5 × your level, split among wounded allies.")));

        ClassDef f = c("fighter", "Fighter", "A master of martial combat, skilled with a variety of weapons and armor.");
        f.hitDie = 10; f.primary = Ability.STR; f.saves = new Ability[]{Ability.STR, Ability.CON};
        f.skillChoices = sk(Skill.ACROBATICS, Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.HISTORY, Skill.INSIGHT, Skill.INTIMIDATION, Skill.PERCEPTION, Skill.PERSUASION, Skill.SURVIVAL);
        f.armor = "heavy"; f.armorBase = 16; f.dexCap = 0; f.shield = true; f.weapons = List.of("longsword", "javelin");
        f.subclass = "Champion"; f.color = "#7f8c8d"; f.glyph = "🛡";
        f.features.addAll(List.of(
                new Feature(1, "fighting_style", "Fighting Style (Defense)", "+1 AC while wearing armor."),
                new Feature(1, "second_wind", "Second Wind", "Bonus Action: regain 1d10 + Fighter level HP. 2 uses; one returns on a Short Rest."),
                new Feature(1, "weapon_mastery", "Weapon Mastery", "Use the mastery properties of your weapons (Longsword: Sap, Javelin: Slow)."),
                new Feature(2, "action_surge", "Action Surge", "Once per rest, take one additional action on your turn."),
                new Feature(2, "tactical_mind", "Tactical Mind", "Spend a Second Wind use to add 1d10 to a failed ability check."),
                new Feature(3, "improved_critical", "Improved Critical (Champion)", "Your weapon attacks score a Critical Hit on a roll of 19 or 20."),
                new Feature(3, "remarkable_athlete", "Remarkable Athlete", "Advantage on Initiative and Strength (Athletics) checks.")));

        ClassDef p = c("paladin", "Paladin", "A devout warrior bound to a sacred oath.");
        p.hitDie = 10; p.primary = Ability.STR; p.saves = new Ability[]{Ability.WIS, Ability.CHA};
        p.skillChoices = sk(Skill.ATHLETICS, Skill.INSIGHT, Skill.INTIMIDATION, Skill.MEDICINE, Skill.PERSUASION, Skill.RELIGION);
        p.armor = "heavy"; p.armorBase = 16; p.dexCap = 0; p.shield = true; p.weapons = List.of("longsword", "javelin");
        p.subclass = "Oath of Devotion"; p.color = "#3498db"; p.glyph = "⚜";
        p.spellAbility = Ability.CHA; p.spellList = "paladin"; p.cantrips = new int[]{0, 0, 0}; p.prepared = new int[]{2, 3, 4};
        p.slots = new int[][]{{2}, {2}, {3}};
        p.alwaysPrepared = List.of("1:divine_smite", "3:shield_of_faith");
        p.defaultSpells = List.of("bless", "cure_wounds", "heroism");
        p.features.addAll(List.of(
                new Feature(1, "lay_on_hands", "Lay on Hands", "Bonus Action: touch a creature and restore HP from a pool of 5 × Paladin level."),
                new Feature(1, "spellcasting", "Spellcasting", "Cast paladin spells using Charisma. Divine Smite is always prepared."),
                new Feature(1, "weapon_mastery", "Weapon Mastery", "Use the mastery properties of your weapons."),
                new Feature(2, "fighting_style", "Fighting Style (Defense)", "+1 AC while wearing armor."),
                new Feature(2, "paladins_smite", "Paladin's Smite", "Cast Divine Smite once per Long Rest without a spell slot."),
                new Feature(3, "sacred_weapon", "Sacred Weapon (Devotion)", "Channel Divinity: for 10 minutes add your Charisma modifier to weapon attack rolls; your weapon sheds light."),
                new Feature(3, "channel_divinity", "Channel Divinity", "1 use per Short Rest.")));

        ClassDef r = c("ranger", "Ranger", "A wandering warrior imbued with primal magic.");
        r.hitDie = 10; r.primary = Ability.DEX; r.saves = new Ability[]{Ability.STR, Ability.DEX};
        r.skillChoices = sk(Skill.ANIMAL_HANDLING, Skill.ATHLETICS, Skill.INSIGHT, Skill.INVESTIGATION, Skill.NATURE, Skill.PERCEPTION, Skill.STEALTH, Skill.SURVIVAL); r.skillCount = 3;
        r.armor = "light"; r.armorBase = 12; r.weapons = List.of("longbow", "shortsword");
        r.subclass = "Hunter"; r.color = "#27ae60"; r.glyph = "🏹";
        r.spellAbility = Ability.WIS; r.spellList = "ranger"; r.prepared = new int[]{2, 3, 4};
        r.slots = new int[][]{{2}, {2}, {3}};
        r.alwaysPrepared = List.of("1:hunters_mark");
        r.defaultSpells = List.of("cure_wounds", "lesser_restoration");
        r.features.addAll(List.of(
                new Feature(1, "spellcasting", "Spellcasting", "Cast ranger spells using Wisdom."),
                new Feature(1, "favored_enemy", "Favored Enemy", "Hunter's Mark is always prepared; cast it twice per Long Rest without a slot."),
                new Feature(1, "weapon_mastery", "Weapon Mastery", "Use the mastery properties of your weapons (Longbow: Slow, Shortsword: Vex)."),
                new Feature(2, "fighting_style", "Fighting Style (Archery)", "+2 to attack rolls with ranged weapons."),
                new Feature(2, "deft_explorer", "Deft Explorer", "Expertise in Survival; two extra languages."),
                new Feature(3, "hunters_prey", "Hunter's Prey: Colossus Slayer", "Once per turn, deal an extra 1d8 to a creature that is missing any HP."),
                new Feature(3, "hunters_lore", "Hunter's Lore", "Know the immunities, resistances and vulnerabilities of your marked quarry.")));

        ClassDef ro = c("rogue", "Rogue", "A dexterous expert in stealth and subterfuge.");
        ro.hitDie = 8; ro.primary = Ability.DEX; ro.saves = new Ability[]{Ability.DEX, Ability.INT};
        ro.skillChoices = sk(Skill.ACROBATICS, Skill.ATHLETICS, Skill.DECEPTION, Skill.INSIGHT, Skill.INTIMIDATION, Skill.INVESTIGATION, Skill.PERCEPTION, Skill.PERSUASION, Skill.SLEIGHT_OF_HAND, Skill.STEALTH); ro.skillCount = 4;
        ro.armor = "light"; ro.armorBase = 11; ro.weapons = List.of("rapier", "shortbow");
        ro.subclass = "Thief"; ro.color = "#34495e"; ro.glyph = "🗡";
        ro.features.addAll(List.of(
                new Feature(1, "expertise", "Expertise", "Double proficiency in two skills (Stealth and Sleight of Hand)."),
                new Feature(1, "sneak_attack", "Sneak Attack", "Once per turn, +1d6 damage (2d6 at level 3) with a finesse or ranged weapon when you have Advantage or an ally is next to the target."),
                new Feature(1, "weapon_mastery", "Weapon Mastery", "Use the mastery properties of your weapons (Rapier: Vex, Shortbow: Vex)."),
                new Feature(2, "cunning_action", "Cunning Action", "Bonus Action: Dash, Disengage or Hide."),
                new Feature(3, "steady_aim", "Steady Aim", "Bonus Action: if you haven't moved, gain Advantage on your next attack this turn."),
                new Feature(3, "fast_hands", "Fast Hands (Thief)", "Use an item (potion, flask, thieves' tools) as a Bonus Action.")));

        ClassDef wz = c("wizard", "Wizard", "A scholarly magic-user capable of manipulating the structures of reality.");
        wz.hitDie = 6; wz.primary = Ability.INT; wz.saves = new Ability[]{Ability.INT, Ability.WIS};
        wz.skillChoices = sk(Skill.ARCANA, Skill.HISTORY, Skill.INSIGHT, Skill.INVESTIGATION, Skill.MEDICINE, Skill.NATURE, Skill.RELIGION);
        wz.armor = "none"; wz.weapons = List.of("quarterstaff", "dagger");
        wz.subclass = "Evoker"; wz.color = "#e67e22"; wz.glyph = "🔥";
        wz.spellAbility = Ability.INT; wz.spellList = "wizard"; wz.cantrips = new int[]{3, 3, 3}; wz.prepared = new int[]{4, 5, 6};
        wz.slots = new int[][]{{2}, {3}, {4, 2}};
        wz.defaultCantrips = List.of("fire_bolt", "ray_of_frost", "shocking_grasp");
        wz.defaultSpells = List.of("shield", "chromatic_orb", "burning_hands", "charm_person", "mage_armor", "magic_missile");
        wz.features.addAll(List.of(
                new Feature(1, "spellcasting", "Spellcasting", "Cast wizard spells from your spellbook using Intelligence. Change prepared spells after a Long Rest."),
                new Feature(1, "ritual_adept", "Ritual Adept", "Cast any Ritual spell in your spellbook without preparing it."),
                new Feature(1, "arcane_recovery", "Arcane Recovery", "Once per day on a Short Rest, recover expended spell slots (combined level up to half your Wizard level, rounded up)."),
                new Feature(2, "scholar", "Scholar", "Expertise in Arcana."),
                new Feature(3, "evocation_savant", "Evocation Savant (Evoker)", "Add two Evocation spells to your spellbook for free."),
                new Feature(3, "potent_cantrip", "Potent Cantrip", "When a creature succeeds on a save against your damaging cantrip, or your cantrip attack misses, it still takes half damage.")));
    }
}
