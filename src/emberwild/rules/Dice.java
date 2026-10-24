package emberwild.rules;

import emberwild.util.Json;

import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-side dice. Every roll in the game is made here so all players see the same, fair result.
 * Supports expressions like "1d20+5", "2d6+1d4-1", "4d6kh3", "8d6".
 */
public final class Dice {
    private static final SecureRandom RNG = new SecureRandom();
    private static final Pattern TERM = Pattern.compile("([+-]?)(\\d*)d(\\d+)(?:k([hl])(\\d+))?|([+-]?)(\\d+)");

    private Dice() {}

    public static int die(int sides) { return sides <= 1 ? 1 : RNG.nextInt(sides) + 1; }

    public static int between(int lo, int hi) { return lo + RNG.nextInt(Math.max(1, hi - lo + 1)); }

    public static double chance() { return RNG.nextDouble(); }

    public static <T> T pick(List<T> l) { return l.isEmpty() ? null : l.get(RNG.nextInt(l.size())); }

    /** A finished roll; serialised straight to the client so the dice tray can animate the exact faces. */
    public static final class Roll implements Json.JsonAble {
        public final String expr;
        public final List<int[]> dice = new ArrayList<>(); // {sides, face, kept(1/0)}
        public int modifier;
        public int total;
        public String label = "";
        public String mode = "normal"; // normal | advantage | disadvantage
        public int natural = -1;       // natural d20 face when it is a d20 test

        Roll(String expr) { this.expr = expr; }

        public boolean nat20() { return natural == 20; }
        public boolean nat1() { return natural == 1; }

        @Override public Object toJson() {
            List<Object> d = new ArrayList<>();
            for (int[] x : dice) d.add(Json.obj("sides", x[0], "face", x[1], "kept", x[2] == 1));
            return Json.obj("expr", expr, "dice", d, "modifier", modifier, "total", total,
                    "label", label, "mode", mode, "natural", natural);
        }

        @Override public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int[] x : dice) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(x[2] == 1 ? String.valueOf(x[1]) : "~" + x[1] + "~");
            }
            String mod = modifier == 0 ? "" : (modifier > 0 ? " + " + modifier : " - " + (-modifier));
            return "[" + sb + "]" + mod + " = " + total;
        }
    }

    /** Roll any dice expression. Unknown text is ignored rather than crashing a session. */
    public static Roll roll(String expr) {
        String clean = expr == null ? "0" : expr.replace(" ", "").toLowerCase(Locale.ROOT);
        Roll r = new Roll(clean);
        Matcher m = TERM.matcher(clean);
        int total = 0;
        while (m.find()) {
            if (m.group(3) != null) {
                int sign = "-".equals(m.group(1)) ? -1 : 1;
                int count = m.group(2).isEmpty() ? 1 : Math.min(100, Integer.parseInt(m.group(2)));
                int sides = Math.min(1000, Integer.parseInt(m.group(3)));
                int[] faces = new int[count];
                for (int k = 0; k < count; k++) faces[k] = die(sides);
                boolean[] kept = new boolean[count];
                Arrays.fill(kept, true);
                if (m.group(4) != null) {
                    int keep = Math.min(count, Integer.parseInt(m.group(5)));
                    Integer[] idx = new Integer[count];
                    for (int k = 0; k < count; k++) idx[k] = k;
                    boolean high = m.group(4).equals("h");
                    Arrays.sort(idx, (a, b) -> high ? faces[b] - faces[a] : faces[a] - faces[b]);
                    Arrays.fill(kept, false);
                    for (int k = 0; k < keep; k++) kept[idx[k]] = true;
                }
                for (int k = 0; k < count; k++) {
                    r.dice.add(new int[]{sides, faces[k], kept[k] ? 1 : 0});
                    if (kept[k]) total += sign * faces[k];
                }
            } else if (m.group(7) != null) {
                int sign = "-".equals(m.group(6)) ? -1 : 1;
                r.modifier += sign * Integer.parseInt(m.group(7));
            }
        }
        r.total = total + r.modifier;
        return r;
    }

    /** Damage roll; a critical hit doubles the dice (not the modifier) as in the 2024 rules. */
    public static Roll damage(String expr, boolean crit) {
        if (!crit) return roll(expr);
        Matcher m = Pattern.compile("(\\d*)d(\\d+)").matcher(expr.replace(" ", ""));
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int n = m.group(1).isEmpty() ? 1 : Integer.parseInt(m.group(1));
            m.appendReplacement(sb, (n * 2) + "d" + m.group(2));
        }
        m.appendTail(sb);
        Roll r = roll(sb.toString());
        r.label = "critical";
        return r;
    }

    /** A d20 Test: ability check, attack roll or saving throw. */
    public static Roll d20(int bonus, boolean adv, boolean dis, boolean halflingLucky) {
        boolean a = adv && !dis, d = dis && !adv;
        int f1 = die(20), f2 = (a || d) ? die(20) : -1;
        if (halflingLucky && f1 == 1) f1 = die(20);
        if (halflingLucky && f2 == 1) f2 = die(20);
        Roll r = new Roll("1d20" + (bonus >= 0 ? "+" + bonus : String.valueOf(bonus)));
        int chosen = f1;
        if (a) { chosen = Math.max(f1, f2); r.mode = "advantage"; }
        if (d) { chosen = Math.min(f1, f2); r.mode = "disadvantage"; }
        int keptIdx = 0;
        if (a) keptIdx = f1 >= f2 ? 0 : 1;
        if (d) keptIdx = f1 <= f2 ? 0 : 1;
        r.dice.add(new int[]{20, f1, keptIdx == 0 ? 1 : 0});
        if (f2 > 0) r.dice.add(new int[]{20, f2, keptIdx == 1 ? 1 : 0});
        r.natural = chosen;
        r.modifier = bonus;
        r.total = chosen + bonus;
        return r;
    }

    public static int average(String expr) {
        Matcher m = TERM.matcher(expr.replace(" ", "").toLowerCase(Locale.ROOT));
        double t = 0;
        while (m.find()) {
            if (m.group(3) != null) {
                int sign = "-".equals(m.group(1)) ? -1 : 1;
                int count = m.group(2).isEmpty() ? 1 : Integer.parseInt(m.group(2));
                t += sign * count * (Integer.parseInt(m.group(3)) + 1) / 2.0;
            } else if (m.group(7) != null) {
                t += ("-".equals(m.group(6)) ? -1 : 1) * Integer.parseInt(m.group(7));
            }
        }
        return (int) Math.floor(t);
    }

    public static boolean valid(String expr) {
        if (expr == null || expr.isBlank() || expr.length() > 60) return false;
        return expr.replace(" ", "").toLowerCase(Locale.ROOT).matches("([+-]?(\\d*d\\d+(k[hl]\\d+)?|\\d+))+");
    }
}
