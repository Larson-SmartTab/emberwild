package emberwild.story;

import emberwild.util.Json;

import java.util.Map;

/**
 * Story effects. Each is a small map so Java-authored and JSON-authored campaigns behave identically.
 * JSON authors write e.g. {"op":"gold","amount":25}.
 */
public final class Fx {
    private Fx() {}

    public static Map<String, Object> flag(String f) { return Json.obj("op", "flag", "flag", f); }
    public static Map<String, Object> unflag(String f) { return Json.obj("op", "unflag", "flag", f); }
    /** Give an item to the hero who made the choice (or the first hero). */
    public static Map<String, Object> item(String itemId) { return Json.obj("op", "item", "item", itemId); }
    public static Map<String, Object> takeItem(String itemId) { return Json.obj("op", "takeItem", "item", itemId); }
    /** Gold for every hero (negative to pay). */
    public static Map<String, Object> gold(int amount) { return Json.obj("op", "gold", "amount", amount); }
    public static Map<String, Object> xp(int amount) { return Json.obj("op", "xp", "amount", amount); }
    public static Map<String, Object> heal(String dice) { return Json.obj("op", "heal", "dice", dice); }
    /** Damage every hero; if save is given they each roll (half on success). */
    public static Map<String, Object> damage(String dice, String type, String save, int dc) {
        return Json.obj("op", "damage", "dice", dice, "type", type, "save", save, "dc", dc);
    }
    /** Damage only the hero who triggered it. */
    public static Map<String, Object> damageOne(String dice, String type) { return Json.obj("op", "damage", "dice", dice, "type", type, "who", "chooser"); }
    public static Map<String, Object> shortRest() { return Json.obj("op", "rest", "kind", "short"); }
    public static Map<String, Object> longRest() { return Json.obj("op", "rest", "kind", "long"); }
    /** Milestone: every hero reaches at least this level. */
    public static Map<String, Object> level(int lvl) { return Json.obj("op", "level", "level", lvl); }
    public static Map<String, Object> log(String text) { return Json.obj("op", "log", "text", text); }
    public static Map<String, Object> inspiration() { return Json.obj("op", "inspiration"); }
    public static Map<String, Object> condition(String c) { return Json.obj("op", "condition", "condition", c); }
    /** Add an AI companion (a pregen hero id from this campaign) when the party is small. */
    public static Map<String, Object> companion(String pregenId, int ifPartyBelow) { return Json.obj("op", "companion", "id", pregenId, "below", ifPartyBelow); }
    public static Map<String, Object> dismiss(String pregenId) { return Json.obj("op", "dismiss", "id", pregenId); }
    /** Track how an NPC feels about the party. */
    public static Map<String, Object> attitude(String npc, int delta) { return Json.obj("op", "attitude", "npc", npc, "delta", delta); }
    public static Map<String, Object> counter(String name, int delta) { return Json.obj("op", "counter", "name", name, "delta", delta); }
}
