package emberwild.game;

import emberwild.rules.*;
import emberwild.util.Json;

import java.util.*;

/** One token on the battle map: a hero (backed by its {@link Hero}) or a monster instance. */
public final class Unit implements Json.JsonAble {
    public final String id;
    public String name;
    public String side;               // "party" or "foe"
    public final Hero hero;           // null for monsters
    public final Monsters.MonsterDef def; // null for heroes
    private int mhp, mmax;            // monster HP (heroes use hero.hp)
    public int temp;
    public int x, y, size = 1;
    public int init;
    public final Map<String, Integer> cond = new LinkedHashMap<>();
    public String charmedBy = "", vexedBy = "", markedBy = "", helpedVs = "";
    public boolean action, bonus, reaction = true, slotUsed, disengaged, reckless, sneakUsed, colossusUsed, frenzyUsed, moved;
    public int move;                  // feet of movement left this turn
    public String conc = "";          // spell id being concentrated on
    public final Map<String, Boolean> ready = new HashMap<>();   // special id -> recharged
    public final Map<String, Integer> uses = new HashMap<>();    // special id -> uses left
    public int legendary, legRes;
    public boolean ai;                // controlled by the engine
    public String owner = "";         // player id controlling this hero
    public boolean gone;              // dead/destroyed and removed from play
    public boolean autoShield = true, smiteArmed = false;
    public int bardDie = 0;           // Bardic Inspiration die held (sides)
    public int fleeing = 0;
    public boolean summoned, isAlly;  // NPC fighting on the party's side
    public String color, glyph;

    Unit(String id, Hero h) {
        this.id = id; this.hero = h; this.def = null; this.name = h.name; this.side = "party";
        this.color = h.color; this.glyph = h.emblem == null || h.emblem.isBlank() ? h.name.substring(0, 1) : h.emblem;
        this.ai = h.npc;
    }

    Unit(String id, Monsters.MonsterDef d, String side) {
        this.id = id; this.hero = null; this.def = d; this.name = d.name; this.side = side;
        this.mhp = d.hp; this.mmax = d.hp; this.size = d.size; this.color = d.color; this.glyph = d.glyph; this.ai = true;
        for (Monsters.Special s : d.specials) { ready.put(s.id, true); if (s.uses > 0) uses.put(s.id, s.uses); }
        this.legendary = d.legendary;
        this.legRes = d.traits.contains("legendary_resistance") ? 2 : 0;
    }

    public boolean isHero() { return hero != null; }
    public boolean isObject() { return def != null && def.role.equals("object"); }
    public boolean isBoss() { return def != null && def.role.equals("boss"); }

    public int hp() { return hero != null ? hero.hp : mhp; }
    public int maxHp() { return hero != null ? hero.maxHp : mmax; }
    public void setHp(int v) { if (hero != null) hero.hp = v; else mhp = v; }
    public void setMaxHp(int v) { if (hero == null) { mmax = v; mhp = Math.min(mhp, v); } }

    public boolean down() { return hp() <= 0; }
    public boolean alive() { return !gone && !(hero != null && hero.dead); }
    public boolean active() { return alive() && !down(); }
    public boolean has(String c) { return cond.containsKey(c); }
    public void add(String c, int rounds) { cond.merge(c, rounds, Math::max); }
    public void remove(String c) { cond.remove(c); }

    public boolean incapacitated() {
        return down() || has("incapacitated") || has("paralyzed") || has("asleep") || has("unconscious") || has("stunned");
    }

    public int mod(Ability a) { return hero != null ? hero.mod(a) : def.mod(a); }

    public int saveBonus(Ability a) {
        int b = hero != null ? hero.saveBonus(a) : def.saveBonus(a);
        if (has("blessed")) b += Dice.die(4);
        return b;
    }

    public int ac() {
        int a = hero != null ? hero.ac() : def.ac;
        if (has("shield")) a += 5;
        if (has("shield_of_faith")) a += 2;
        return a;
    }

    public int speed() {
        int s = hero != null ? hero.speed() : (def.fly > 0 ? Math.max(def.fly, def.speed) : def.speed);
        if (has("slowed")) s -= 10;
        if (has("grappled") || has("restrained")) s = 0;
        if (has("raging") && hero != null && hero.classId.equals("barbarian")) s += 0;
        return Math.max(0, s);
    }

    public boolean flies() { return def != null && def.fly > 0; }

    public Set<String> resist() {
        Set<String> r = new HashSet<>(hero != null ? hero.resistances() : def.resist);
        if (has("raging")) { r.add("bludgeoning"); r.add("piercing"); r.add("slashing"); }
        return r;
    }
    public Set<String> immune() { return hero != null ? Set.of() : def.immune; }
    public Set<String> vulnerable() { return hero != null ? Set.of() : def.vulnerable; }
    public boolean condImmune(String c) { return def != null && def.condImmune.contains(c); }

    public boolean humanoid() { return hero != null || def.humanoid(); }
    public String creatureType() { return hero != null ? "humanoid" : def.type; }

    public int passivePerception() { return hero != null ? hero.passivePerception() : 10 + def.mod(Ability.WIS); }

    public int xp() { return def == null ? 0 : def.xp; }

    @Override public Object toJson() {
        List<Object> cs = new ArrayList<>();
        for (var e : cond.entrySet()) cs.add(e.getKey());
        return Json.obj("id", id, "name", name, "side", side, "x", x, "y", y, "size", size, "hp", hp(), "maxHp", maxHp(),
                "temp", hero != null ? hero.tempHp : temp, "ac", ac(), "glyph", glyph, "color", color, "conds", cs,
                "hero", hero != null, "heroId", hero != null ? hero.id : null, "owner", owner, "ai", ai, "gone", gone,
                "down", down(), "dead", hero != null && hero.dead, "conc", conc, "init", init, "object", isObject(),
                "boss", isBoss(), "speed", speed(), "monster", def != null ? def.id : null, "ally", isAlly,
                "stable", hero != null && hero.stable, "deathS", hero != null ? hero.deathSuccesses : 0, "deathF", hero != null ? hero.deathFailures : 0,
                "blurb", def != null ? def.blurb : "", "bardDie", bardDie, "flies", flies());
    }
}
