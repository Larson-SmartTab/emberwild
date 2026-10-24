package emberwild.util;

import java.util.*;

/**
 * Tiny dependency-free JSON reader/writer.
 * Objects -> LinkedHashMap<String,Object>, arrays -> ArrayList<Object>,
 * numbers -> Long or Double, plus String/Boolean/null.
 */
public final class Json {
    private Json() {}

    // ------------------------------------------------------------------ writing
    public static String write(Object o) {
        StringBuilder sb = new StringBuilder(256);
        write(sb, o, -1, 0);
        return sb.toString();
    }

    public static String pretty(Object o) {
        StringBuilder sb = new StringBuilder(256);
        write(sb, o, 2, 0);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(StringBuilder sb, Object o, int indent, int depth) {
        if (o == null) { sb.append("null"); return; }
        if (o instanceof JsonAble j) o = j.toJson();
        if (o instanceof String s) { quote(sb, s); return; }
        if (o instanceof Enum<?> e) { quote(sb, e.name().toLowerCase()); return; }
        if (o instanceof Double d) {
            if (d.isNaN() || d.isInfinite()) sb.append("0");
            else if (d == Math.rint(d) && Math.abs(d) < 1e15) sb.append(d.longValue());
            else sb.append(d);
            return;
        }
        if (o instanceof Float f) { write(sb, f.doubleValue(), indent, depth); return; }
        if (o instanceof Number || o instanceof Boolean) { sb.append(o); return; }
        if (o instanceof Map<?, ?> m) {
            if (m.isEmpty()) { sb.append("{}"); return; }
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                nl(sb, indent, depth + 1);
                quote(sb, String.valueOf(e.getKey()));
                sb.append(indent >= 0 ? ": " : ":");
                write(sb, e.getValue(), indent, depth + 1);
            }
            nl(sb, indent, depth);
            sb.append('}');
            return;
        }
        if (o instanceof Collection<?> c) {
            if (c.isEmpty()) { sb.append("[]"); return; }
            sb.append('[');
            boolean first = true;
            for (Object v : c) {
                if (!first) sb.append(',');
                first = false;
                nl(sb, indent, depth + 1);
                write(sb, v, indent, depth + 1);
            }
            nl(sb, indent, depth);
            sb.append(']');
            return;
        }
        if (o instanceof int[] arr) { List<Object> l = new ArrayList<>(); for (int v : arr) l.add(v); write(sb, l, indent, depth); return; }
        if (o instanceof Object[] arr) { write(sb, Arrays.asList(arr), indent, depth); return; }
        quote(sb, o.toString());
    }

    private static void nl(StringBuilder sb, int indent, int depth) {
        if (indent < 0) return;
        sb.append('\n');
        sb.append(" ".repeat(indent * depth));
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20 || c == ' ' || c == ' ') sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------------ reading
    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i < p.s.length()) throw p.err("Unexpected trailing content");
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object o = parse(text);
        if (!(o instanceof Map)) throw new IllegalArgumentException("Expected a JSON object at the top level");
        return (Map<String, Object>) o;
    }

    private static final class Parser {
        final String s; int i;
        Parser(String s) { this.s = s; }

        IllegalArgumentException err(String msg) {
            int line = 1, col = 1;
            for (int k = 0; k < Math.min(i, s.length()); k++) { if (s.charAt(k) == '\n') { line++; col = 1; } else col++; }
            return new IllegalArgumentException(msg + " (line " + line + ", column " + col + ")");
        }

        void ws() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == '﻿') i++;
                else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '/') { // allow // comments (friendly for authors)
                    while (i < s.length() && s.charAt(i) != '\n') i++;
                } else break;
            }
        }

        Object value() {
            if (i >= s.length()) throw err("Unexpected end of input");
            char c = s.charAt(i);
            switch (c) {
                case '{': return obj();
                case '[': return arr();
                case '"': return str();
                case 't': expect("true"); return Boolean.TRUE;
                case 'f': expect("false"); return Boolean.FALSE;
                case 'n': expect("null"); return null;
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) return num();
                    throw err("Unexpected character '" + c + "'");
            }
        }

        void expect(String w) {
            if (!s.startsWith(w, i)) throw err("Expected '" + w + "'");
            i += w.length();
        }

        Map<String, Object> obj() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; ws();
            if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
            while (true) {
                ws();
                if (i >= s.length() || s.charAt(i) != '"') throw err("Expected a quoted key");
                String k = str();
                ws();
                if (i >= s.length() || s.charAt(i) != ':') throw err("Expected ':' after key \"" + k + "\"");
                i++; ws();
                m.put(k, value());
                ws();
                if (i >= s.length()) throw err("Unterminated object");
                char c = s.charAt(i++);
                if (c == '}') return m;
                if (c != ',') { i--; throw err("Expected ',' or '}'"); }
                ws();
                if (i < s.length() && s.charAt(i) == '}') { i++; return m; } // tolerate trailing comma
            }
        }

        List<Object> arr() {
            List<Object> l = new ArrayList<>();
            i++; ws();
            if (i < s.length() && s.charAt(i) == ']') { i++; return l; }
            while (true) {
                ws();
                l.add(value());
                ws();
                if (i >= s.length()) throw err("Unterminated array");
                char c = s.charAt(i++);
                if (c == ']') return l;
                if (c != ',') { i--; throw err("Expected ',' or ']'"); }
                ws();
                if (i < s.length() && s.charAt(i) == ']') { i++; return l; }
            }
        }

        String str() {
            StringBuilder sb = new StringBuilder();
            i++;
            while (true) {
                if (i >= s.length()) throw err("Unterminated string");
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (i >= s.length()) throw err("Bad escape");
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            if (i + 4 > s.length()) throw err("Bad unicode escape");
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> sb.append(e);
                    }
                } else sb.append(c);
            }
        }

        Object num() {
            int st = i;
            if (s.charAt(i) == '-') i++;
            boolean frac = false;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c >= '0' && c <= '9') i++;
                else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') { frac = true; i++; }
                else break;
            }
            String n = s.substring(st, i);
            try {
                if (!frac) return Long.parseLong(n);
                return Double.parseDouble(n);
            } catch (NumberFormatException ex) { throw err("Bad number '" + n + "'"); }
        }
    }

    // ------------------------------------------------------------------ helpers
    public static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int k = 0; k + 1 < kv.length; k += 2) m.put(String.valueOf(kv[k]), kv[k + 1]);
        return m;
    }

    public static String str(Map<String, Object> m, String k, String def) {
        Object v = m == null ? null : m.get(k);
        return v == null ? def : String.valueOf(v);
    }

    public static int integer(Map<String, Object> m, String k, int def) {
        Object v = m == null ? null : m.get(k);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) try { return Integer.parseInt(s.trim()); } catch (Exception ignored) {}
        return def;
    }

    public static double dbl(Map<String, Object> m, String k, double def) {
        Object v = m == null ? null : m.get(k);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) try { return Double.parseDouble(s.trim()); } catch (Exception ignored) {}
        return def;
    }

    public static boolean bool(Map<String, Object> m, String k, boolean def) {
        Object v = m == null ? null : m.get(k);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(Map<String, Object> m, String k) {
        Object v = m == null ? null : m.get(k);
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Map<String, Object> m, String k) {
        Object v = m == null ? null : m.get(k);
        if (v instanceof List) return (List<Object>) v;
        if (v == null) return new ArrayList<>();
        List<Object> one = new ArrayList<>();
        one.add(v);
        return one;
    }

    public static List<String> strings(Map<String, Object> m, String k) {
        List<String> out = new ArrayList<>();
        for (Object o : list(m, k)) if (o != null) out.add(String.valueOf(o));
        return out;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> maps(Map<String, Object> m, String k) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : list(m, k)) if (o instanceof Map) out.add((Map<String, Object>) o);
        return out;
    }

    /** Anything that can render itself as JSON-compatible data. */
    public interface JsonAble { Object toJson(); }
}
