package emberwild.rules;

import java.util.Locale;

/** The 18 skills of the 2024 Player's Handbook. */
public enum Skill {
    ACROBATICS(Ability.DEX, "Acrobatics"), ANIMAL_HANDLING(Ability.WIS, "Animal Handling"), ARCANA(Ability.INT, "Arcana"),
    ATHLETICS(Ability.STR, "Athletics"), DECEPTION(Ability.CHA, "Deception"), HISTORY(Ability.INT, "History"),
    INSIGHT(Ability.WIS, "Insight"), INTIMIDATION(Ability.CHA, "Intimidation"), INVESTIGATION(Ability.INT, "Investigation"),
    MEDICINE(Ability.WIS, "Medicine"), NATURE(Ability.INT, "Nature"), PERCEPTION(Ability.WIS, "Perception"),
    PERFORMANCE(Ability.CHA, "Performance"), PERSUASION(Ability.CHA, "Persuasion"), RELIGION(Ability.INT, "Religion"),
    SLEIGHT_OF_HAND(Ability.DEX, "Sleight of Hand"), STEALTH(Ability.DEX, "Stealth"), SURVIVAL(Ability.WIS, "Survival");

    public final Ability ability;
    public final String label;

    Skill(Ability a, String label) { this.ability = a; this.label = label; }

    public String key() { return name().toLowerCase(Locale.ROOT); }

    public static Skill of(String s) {
        if (s == null) return null;
        String k = s.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        for (Skill sk : values()) if (sk.key().equals(k)) return sk;
        return null;
    }
}
