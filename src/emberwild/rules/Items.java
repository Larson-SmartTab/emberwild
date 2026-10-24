package emberwild.rules;

import emberwild.util.Json;

import java.util.*;

/** Weapons (with 2024 Weapon Mastery properties), armor and treasure. */
public final class Items {

    public static final class Weapon implements Json.JsonAble {
        public final String id, name, dice, type, mastery;
        public boolean finesse, ranged, thrown, twoHanded, light, heavy;
        public int range = 5, longRange = 0;
        public String versatile = "";

        Weapon(String id, String name, String dice, String type, String mastery) {
            this.id = id; this.name = name; this.dice = dice; this.type = type; this.mastery = mastery;
        }

        Weapon finesse() { finesse = true; return this; }
        Weapon light() { light = true; return this; }
        Weapon heavy() { heavy = true; twoHanded = true; return this; }
        Weapon twoHanded() { twoHanded = true; return this; }
        Weapon ranged(int n, int l) { ranged = true; range = n; longRange = l; return this; }
        Weapon thrown(int n, int l) { thrown = true; longRange = l; thrownRange = n; return this; }
        Weapon versatile(String d) { versatile = d; return this; }
        public int thrownRange = 0;

        @Override public Object toJson() {
            return Json.obj("id", id, "name", name, "dice", dice, "type", type, "mastery", mastery,
                    "finesse", finesse, "ranged", ranged, "thrown", thrown, "range", ranged ? range : 5,
                    "thrownRange", thrownRange, "longRange", longRange);
        }
    }

    public static final class Item implements Json.JsonAble {
        public final String id, name, kind, desc;
        public String use = "";      // heal dice for potions, damage for bombs
        public String damageType = "";
        public boolean attunable;
        public Map<String, Integer> bonus = new LinkedHashMap<>(); // ac, save, spell_attack, spell_dc, attack, damage, hp, speed, initiative
        public int value;            // gold piece value

        Item(String id, String name, String kind, String desc) { this.id = id; this.name = name; this.kind = kind; this.desc = desc; }

        Item use(String u) { use = u; return this; }
        Item dmg(String t) { damageType = t; return this; }
        Item attune() { attunable = true; return this; }
        Item bonus(String k, int v) { bonus.put(k, v); return this; }
        Item value(int v) { value = v; return this; }

        @Override public Object toJson() {
            return Json.obj("id", id, "name", name, "kind", kind, "desc", desc, "use", use, "damageType", damageType,
                    "attunable", attunable, "bonus", bonus, "value", value);
        }
    }

    private static final Map<String, Weapon> WEAPONS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    private static Weapon w(String id, String name, String dice, String type, String mastery) {
        Weapon x = new Weapon(id, name, dice, type, mastery);
        WEAPONS.put(id, x);
        return x;
    }

    private static Item i(String id, String name, String kind, String desc) {
        Item x = new Item(id, name, kind, desc);
        ITEMS.put(id, x);
        return x;
    }

    public static Weapon weapon(String id) { return WEAPONS.get(id); }
    public static Item item(String id) { return ITEMS.get(id); }
    public static Collection<Item> allItems() { return ITEMS.values(); }
    public static Collection<Weapon> allWeapons() { return WEAPONS.values(); }

    /** Register a custom item from a submitted campaign. */
    public static synchronized void register(Map<String, Object> m) {
        String id = Json.str(m, "id", null);
        if (id == null || ITEMS.containsKey(id)) return;
        Item x = i(id, Json.str(m, "name", id), Json.str(m, "kind", "trinket"), Json.str(m, "desc", ""));
        x.use = Json.str(m, "use", "");
        x.damageType = Json.str(m, "damageType", "");
        x.attunable = Json.bool(m, "attunable", false);
        x.value = Json.integer(m, "value", 0);
        Map<String, Object> b = Json.map(m, "bonus");
        if (b != null) for (var e : b.entrySet()) if (e.getValue() instanceof Number n) x.bonus.put(e.getKey(), n.intValue());
    }

    static {
        // Weapons — damage, type, Weapon Mastery property (2024 PHB)
        w("unarmed", "Unarmed Strike", "1", "bludgeoning", "");
        w("dagger", "Dagger", "1d4", "piercing", "nick").finesse().light().thrown(20, 60);
        w("quarterstaff", "Quarterstaff", "1d6", "bludgeoning", "topple").versatile("1d8");
        w("mace", "Mace", "1d6", "bludgeoning", "sap");
        w("handaxe", "Handaxe", "1d6", "slashing", "vex").light().thrown(20, 60);
        w("javelin", "Javelin", "1d6", "piercing", "slow").thrown(30, 120);
        w("spear", "Spear", "1d6", "piercing", "sap").thrown(20, 60).versatile("1d8");
        w("shortsword", "Shortsword", "1d6", "piercing", "vex").finesse().light();
        w("scimitar", "Scimitar", "1d6", "slashing", "nick").finesse().light();
        w("rapier", "Rapier", "1d8", "piercing", "vex").finesse();
        w("longsword", "Longsword", "1d8", "slashing", "sap").versatile("1d10");
        w("warhammer", "Warhammer", "1d8", "bludgeoning", "push").versatile("1d10");
        w("battleaxe", "Battleaxe", "1d8", "slashing", "topple").versatile("1d10");
        w("greataxe", "Greataxe", "1d12", "slashing", "cleave").heavy();
        w("greatsword", "Greatsword", "2d6", "slashing", "graze").heavy();
        w("maul", "Maul", "2d6", "bludgeoning", "topple").heavy();
        w("shortbow", "Shortbow", "1d6", "piercing", "vex").ranged(80, 320).twoHanded();
        w("longbow", "Longbow", "1d8", "piercing", "slow").ranged(150, 600).heavy();
        w("light_crossbow", "Light Crossbow", "1d8", "piercing", "slow").ranged(80, 320).twoHanded();
        w("sling", "Sling", "1d4", "bludgeoning", "slow").ranged(30, 120);

        // Consumables
        i("potion_healing", "Potion of Healing", "potion", "A red, glimmering draught. Restores 2d4+2 HP. Drinking is a Bonus Action.").use("2d4+2").value(50);
        i("potion_greater_healing", "Potion of Greater Healing", "potion", "Restores 4d4+4 HP. Bonus Action.").use("4d4+4").value(150);
        i("alchemists_fire", "Alchemist's Fire", "bomb", "Thrown flask (20 ft). Ranged attack; on a hit the target takes 1d4 fire and burns (1d4 each turn).").use("2d4").dmg("fire").value(50);
        i("frost_flask", "Frostbloom Flask", "bomb", "A flask of everwinter sap. Thrown (20 ft): 5-ft burst, Dexterity save DC 13 or 2d6 cold and Slowed.").use("2d6").dmg("cold").value(60);
        i("antitoxin", "Antitoxin", "potion", "Advantage on saves against poison for an hour.").value(50);
        i("thieves_tools", "Thieves' Tools", "tool", "Picks and tension wrenches. Lets anyone attempt to disarm traps and pick locks.").value(25);
        i("rope", "Hempen Rope (50 ft)", "gear", "Sturdy rope. Useful for climbing, crossing and binding.").value(1);
        i("torch", "Torches", "gear", "Bright light in a 20-ft radius for an hour.").value(1);

        // Magic items (attunable)
        i("emberglass_focus", "Emberglass Focus", "wondrous", "A shard of volcanic glass that hums with warmth. +1 to spell attack rolls and spell save DC.").attune().bonus("spell_attack", 1).bonus("spell_dc", 1).value(500);
        i("cloak_of_protection", "Cloak of Protection", "wondrous", "+1 to Armor Class and saving throws.").attune().bonus("ac", 1).bonus("save", 1).value(500);
        i("thornwood_circlet", "Thornwood Circlet", "wondrous", "A living crown of briar and moonflower given by the dryad Eloweth. +1 to Charisma checks and +5 ft Speed.").attune().bonus("cha_check", 1).bonus("speed", 5).value(400);
        i("ring_of_the_hearth", "Ring of the Hearth", "ring", "A brass band warm to the touch. Resistance to fire damage.").attune().bonus("resist_fire", 1).value(600);
        i("weapon_plus_one", "Runed Blade (+1)", "weapon", "A weapon etched with Thornreach runes. +1 to weapon attack and damage rolls.").attune().bonus("attack", 1).bonus("damage", 1).value(500);
        i("boots_of_the_fen", "Boots of the Fen", "wondrous", "Moss-soft boots. Ignore difficult terrain; +1 initiative.").attune().bonus("ignore_difficult", 1).bonus("initiative", 1).value(300);
        i("amulet_of_vitality", "Amulet of Vitality", "wondrous", "A heartwood pendant. +1 hit point per level and +1 Constitution saves.").attune().bonus("hp_per_level", 1).bonus("con_save", 1).value(400);

        // Story treasures
        i("ember_map", "Vesk's Ember Map", "quest", "A scorched map of the Thornwood marking the Cinderdeep Ruins.");
        i("choir_sigil", "Ashen Choir Sigil", "quest", "An iron pendant shaped like a burning throat. Cult members wear these.");
        i("heartstone_shard", "Heartstone Shard", "quest", "A fragment of the warden-stone that once held Vharkas. It thrums with cold.");
        i("gem", "Fire Opal", "treasure", "A gem worth 100 gp.").value(100);
    }
}
