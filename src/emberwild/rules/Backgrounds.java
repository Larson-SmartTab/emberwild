package emberwild.rules;

import emberwild.util.Json;

import java.util.*;

/** 2024 backgrounds: each grants ability score increases among three abilities, two skills and an Origin feat. */
public final class Backgrounds {

    public static final class BackgroundDef implements Json.JsonAble {
        public final String id, name, desc, feat;
        public final Ability[] abilities;
        public final Skill[] skills;
        public final int gold;

        BackgroundDef(String id, String name, String desc, Ability[] ab, Skill[] sk, String feat, int gold) {
            this.id = id; this.name = name; this.desc = desc; this.abilities = ab; this.skills = sk; this.feat = feat; this.gold = gold;
        }

        @Override public Object toJson() {
            List<Object> a = new ArrayList<>(); for (Ability x : abilities) a.add(x.key());
            List<Object> s = new ArrayList<>(); for (Skill x : skills) s.add(x.key());
            Feat f = FEATS.get(feat);
            return Json.obj("id", id, "name", name, "desc", desc, "abilities", a, "skills", s, "feat", feat,
                    "featName", f == null ? feat : f.name, "featDesc", f == null ? "" : f.desc, "gold", gold);
        }
    }

    public record Feat(String id, String name, String desc) {}

    private static final Map<String, BackgroundDef> ALL = new LinkedHashMap<>();
    public static final Map<String, Feat> FEATS = new LinkedHashMap<>();

    public static BackgroundDef get(String id) { return id == null ? null : ALL.get(id.toLowerCase(Locale.ROOT)); }
    public static Collection<BackgroundDef> all() { return ALL.values(); }

    private static void b(String id, String name, String desc, Ability a1, Ability a2, Ability a3, Skill s1, Skill s2, String feat, int gold) {
        ALL.put(id, new BackgroundDef(id, name, desc, new Ability[]{a1, a2, a3}, new Skill[]{s1, s2}, feat, gold));
    }

    private static void feat(String id, String name, String desc) { FEATS.put(id, new Feat(id, name, desc)); }

    static {
        feat("alert", "Alert", "Add your Proficiency Bonus to Initiative.");
        feat("tough", "Tough", "Your Hit Point maximum increases by 2 per level.");
        feat("lucky", "Lucky", "Luck Points equal to your Proficiency Bonus per Long Rest: spend one to gain Advantage on a d20 Test.");
        feat("savage_attacker", "Savage Attacker", "Once per turn, roll weapon damage dice twice and use either roll.");
        feat("skilled", "Skilled", "Proficiency in three skills of your choice.");
        feat("magic_initiate_wizard", "Magic Initiate (Wizard)", "Learn two wizard cantrips and cast Magic Missile once per Long Rest without a slot.");
        feat("magic_initiate_cleric", "Magic Initiate (Cleric)", "Learn two cleric cantrips and cast Healing Word once per Long Rest without a slot.");
        feat("tavern_brawler", "Tavern Brawler", "Unarmed strikes deal 1d4 + Strength; reroll 1s on damage; push a creature 5 ft once per turn.");
        feat("musician", "Musician", "After a rest, play a song to give up to your Proficiency Bonus allies Heroic Inspiration.");
        feat("healer", "Healer", "Use a healer's kit to let a creature spend a Hit Die and add your Proficiency Bonus.");

        b("acolyte", "Acolyte", "You devoted yourself to service in a temple.", Ability.INT, Ability.WIS, Ability.CHA, Skill.INSIGHT, Skill.RELIGION, "magic_initiate_cleric", 8);
        b("charlatan", "Charlatan", "Once you were old enough to order an ale, you found the con.", Ability.DEX, Ability.CON, Ability.CHA, Skill.DECEPTION, Skill.SLEIGHT_OF_HAND, "skilled", 15);
        b("criminal", "Criminal", "You eked out a living in dark alleys, cutting purses or burgling shops.", Ability.DEX, Ability.CON, Ability.INT, Skill.SLEIGHT_OF_HAND, Skill.STEALTH, "alert", 16);
        b("entertainer", "Entertainer", "You spent much of your youth following travelling fairs and carnivals.", Ability.STR, Ability.DEX, Ability.CHA, Skill.ACROBATICS, Skill.PERFORMANCE, "musician", 11);
        b("farmer", "Farmer", "You grew up close to the land.", Ability.STR, Ability.CON, Ability.WIS, Skill.ANIMAL_HANDLING, Skill.NATURE, "tough", 30);
        b("guard", "Guard", "Your feet ache from countless hours at your post.", Ability.STR, Ability.INT, Ability.WIS, Skill.ATHLETICS, Skill.PERCEPTION, "alert", 12);
        b("noble", "Noble", "You were raised in a castle, amid wealth, power and privilege.", Ability.STR, Ability.INT, Ability.CHA, Skill.HISTORY, Skill.PERSUASION, "skilled", 29);
        b("sage", "Sage", "You spent your formative years travelling between manors and monasteries, studying lore.", Ability.CON, Ability.INT, Ability.WIS, Skill.ARCANA, Skill.HISTORY, "magic_initiate_wizard", 8);
        b("soldier", "Soldier", "You began training for war as soon as you reached adulthood.", Ability.STR, Ability.DEX, Ability.CON, Skill.ATHLETICS, Skill.INTIMIDATION, "savage_attacker", 14);
        b("wayfarer", "Wayfarer", "You grew up on the streets surrounded by similarly ill-fated castoffs.", Ability.DEX, Ability.WIS, Ability.CHA, Skill.INSIGHT, Skill.STEALTH, "lucky", 16);
        b("hermit", "Hermit", "You spent your early years secluded in a hut or monastery beyond the nearest settlement.", Ability.CON, Ability.WIS, Ability.CHA, Skill.MEDICINE, Skill.RELIGION, "healer", 16);
        b("sailor", "Sailor", "You lived as a seafarer, wind at your back and decks swaying beneath your feet.", Ability.STR, Ability.DEX, Ability.WIS, Skill.ACROBATICS, Skill.PERCEPTION, "tavern_brawler", 20);
    }
}
