package emberwild.rules;

import java.util.Locale;

/** The six ability scores. */
public enum Ability {
    STR("Strength"), DEX("Dexterity"), CON("Constitution"), INT("Intelligence"), WIS("Wisdom"), CHA("Charisma");

    public final String full;

    Ability(String full) { this.full = full; }

    public String key() { return name().toLowerCase(Locale.ROOT); }

    public static int mod(int score) { return Math.floorDiv(score - 10, 2); }

    public static Ability of(String s) {
        if (s == null) return null;
        String k = s.trim().toLowerCase(Locale.ROOT);
        for (Ability a : values()) if (a.key().equals(k) || a.full.toLowerCase(Locale.ROOT).equals(k)) return a;
        return null;
    }
}
