package emberwild.game;

import emberwild.rules.*;
import emberwild.story.Campaign;
import emberwild.util.Json;

import java.util.*;

/**
 * Grid-based tactical combat following the 2024 rules where it matters at the table:
 * initiative, Action / Bonus Action / Reaction / movement economy, opportunity attacks, advantage &amp; disadvantage,
 * critical hits, saving throws, concentration, death saving throws, conditions, Weapon Mastery, spell slots
 * (one slotted spell per turn), legendary actions and boss phases.
 */
public final class Combat {
    final Game game;
    final Campaign.Encounter enc;
    final int w, h;
    final char[][] tiles;
    final List<Unit> units = new ArrayList<>();
    final List<Unit> order = new ArrayList<>();
    int turn = -1, round = 0;
    boolean over = false;
    String result = "";
    final Set<Integer> revealedTraps = new HashSet<>();
    final List<int[]> warnings = new ArrayList<>();   // telegraphed eruption cells
    final List<int[]> scorched = new ArrayList<>();
    final Set<Integer> phasesDone = new HashSet<>();
    boolean lavaActive = false;
    int lavaCount = 4;
    final Map<String, List<String[]>> concTargets = new HashMap<>();
    int seq = 0;
    int xpPool = 0;
    long turnStartedAt = System.currentTimeMillis();
    final String objectiveText;

    static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    Combat(Game game, Campaign.Encounter enc, List<Hero> heroes, Set<String> flags) {
        this.game = game;
        this.enc = enc;
        int ww = 0;
        for (String r : enc.map) ww = Math.max(ww, r.length());
        this.w = ww;
        this.h = enc.map.size();
        this.tiles = new char[h][w];
        List<int[]> partySpots = new ArrayList<>(), extraSpots = new ArrayList<>(), allySpots = new ArrayList<>();
        List<Object[]> foeSpots = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            String row = enc.map.get(y);
            for (int x = 0; x < w; x++) {
                char c = x < row.length() ? row.charAt(x) : '#';
                if (c == ' ') c = '.';
                if (c == 'P') { partySpots.add(new int[]{x, y}); c = '.'; }
                else if (c == 'E') { extraSpots.add(new int[]{x, y}); c = '.'; }
                else if (c == 'A') { allySpots.add(new int[]{x, y}); c = '.'; }
                else if (enc.legend.containsKey(String.valueOf(c))) {
                    foeSpots.add(new Object[]{x, y, enc.legend.get(String.valueOf(c))});
                    c = '.';
                }
                else if (Character.isLetter(c) && "TLB".indexOf(c) < 0) c = '.';
                tiles[y][x] = c;
            }
        }
        // party
        int pi = 0;
        for (Hero hr : heroes) {
            if (hr.dead) continue;
            Unit u = new Unit("h" + (++seq), hr);
            u.owner = hr.player;
            int[] spot = pi < partySpots.size() ? partySpots.get(pi) : nearFree(partySpots.isEmpty() ? new int[]{1, 1} : partySpots.get(0), 1);
            pi++;
            if (spot == null) spot = new int[]{1, 1};
            u.x = spot[0]; u.y = spot[1];
            if (hr.hp <= 0) { hr.hp = 1; } // nobody starts a fight at 0
            units.add(u);
        }
        // foes
        for (Object[] f : foeSpots) spawn((String) f[2], (int) f[0], (int) f[1], "foe");
        int extraHeroes = Math.max(0, heroes.size() - 3);
        for (int i = 0; i < extraHeroes && !enc.extraPerHero.isEmpty(); i++) {
            for (String mid : enc.extraPerHero) {
                int[] base = extraSpots.isEmpty() ? (foeSpots.isEmpty() ? new int[]{w - 2, h / 2} : new int[]{(int) foeSpots.get(0)[0], (int) foeSpots.get(0)[1]}) : extraSpots.get(i % extraSpots.size());
                Monsters.MonsterDef d = Monsters.get(mid);
                int[] spot = nearFree(base, d == null ? 1 : d.size);
                if (spot != null) spawn(mid, spot[0], spot[1], "foe");
            }
        }
        // allies earned through the story (e.g. a redeemed foe)
        int ai = 0;
        for (Map<String, Object> a : enc.allies) {
            String fl = Json.str(a, "ifFlag", "");
            if (!fl.isEmpty() && !flags.contains(fl)) continue;
            int[] base = ai < allySpots.size() ? allySpots.get(ai) : (partySpots.isEmpty() ? new int[]{1, 1} : partySpots.get(0));
            ai++;
            Monsters.MonsterDef d = Monsters.get(Json.str(a, "monster", ""));
            if (d == null) continue;
            int[] spot = nearFree(base, d.size);
            if (spot != null) { Unit u = spawn(d.id, spot[0], spot[1], "party"); u.isAlly = true; u.name = Json.str(a, "name", d.name); }
        }
        // weakened boss (the party found its weakness)
        if (!enc.weakenIf.isEmpty() && flags.contains(enc.weakenIf)) {
            for (Unit u : units) if (u.isBoss()) {
                u.setHp((int) Math.round(u.maxHp() * 0.7));
                u.legRes = 0;
            }
        }
        String obj = switch (enc.objective) {
            case "survive" -> "Survive " + enc.roundsToSurvive + " rounds";
            default -> enc.objective.startsWith("defeat:") ? "Defeat " + Optional.ofNullable(Monsters.get(enc.objective.substring(7))).map(m -> m.name).orElse("the leader")
                    : (enc.nonlethal ? "Knock out all opponents (non-lethal)" : "Defeat all enemies");
        };
        this.objectiveText = obj;
    }

    Unit spawn(String monsterId, int x, int y, String side) {
        Monsters.MonsterDef d = Monsters.get(monsterId);
        if (d == null) return null;
        Unit u = new Unit("m" + (++seq), d, side);
        u.x = x; u.y = y;
        // number duplicates for clarity: "Thornwood Wolf 2"
        long same = units.stream().filter(o -> o.def != null && o.def.id.equals(monsterId)).count();
        if (same > 0) u.name = d.name + " " + (same + 1);
        units.add(u);
        return u;
    }

    int[] nearFree(int[] base, int size) {
        for (int r = 0; r < Math.max(w, h); r++) {
            for (int dy = -r; dy <= r; dy++) for (int dx = -r; dx <= r; dx++) {
                if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                int x = base[0] + dx, y = base[1] + dy;
                if (canStand(null, x, y, size, false)) return new int[]{x, y};
            }
        }
        return null;
    }

    // ===================================================================================== lifecycle
    void begin(boolean surprise) {
        game.fx(Json.obj("t", "battle", "title", enc.title, "intro", enc.intro));
        if (!enc.intro.isBlank()) game.log(enc.intro, "narration");
        // reveal traps the party notices (passive Perception vs DC 13)
        int best = 0;
        for (Unit u : units) if (u.isHero()) best = Math.max(best, u.passivePerception());
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (tiles[y][x] == '^' && best >= 13) revealedTraps.add(y * w + x);
        if (!revealedTraps.isEmpty()) game.log("Keen eyes spot " + revealedTraps.size() + " hidden trap(s) on the field.", "info");
        // initiative
        for (Unit u : units) {
            int bonus = u.isHero() ? u.hero.initiative() : u.mod(Ability.DEX);
            boolean adv = surprise && u.side.equals("party");
            boolean dis = surprise && u.side.equals("foe");
            if (u.isHero() && u.hero.hasFeature("remarkable_athlete")) adv = true;
            Dice.Roll r = Dice.d20(bonus, adv, dis, u.isHero() && u.hero.hasTrait("lucky"));
            u.init = u.isObject() ? -99 : r.total;
            if (!u.isObject()) game.rollFx(r, u.name + " — Initiative", u.isHero() ? u.hero.player : "", true);
        }
        order.addAll(units);
        order.removeIf(Unit::isObject);
        order.sort((a, b) -> b.init != a.init ? b.init - a.init : (a.side.equals("party") ? -1 : 1));
        StringBuilder sb = new StringBuilder("Initiative: ");
        for (Unit u : order) sb.append(u.name).append(" (").append(u.init).append(")  ");
        game.log(sb.toString().trim(), "info");
        if (surprise) game.log("The enemy is caught off guard! (Surprise: they roll initiative with Disadvantage)", "good");
        round = 1;
        turn = -1;
        nextTurn();
    }

    Unit current() { return turn >= 0 && turn < order.size() ? order.get(turn) : null; }

    void nextTurn() {
        if (over) return;
        Unit prev = current();
        if (prev != null && prev.alive()) endOfTurn(prev);
        if (checkEnd()) return;
        // legendary action after another creature's turn
        if (prev != null && !prev.isBoss()) legendaryAction(prev);
        if (checkEnd()) return;
        for (int guard = 0; guard < order.size() + 2; guard++) {
            turn++;
            if (turn >= order.size()) {
                turn = 0;
                round++;
                if (enc.roundsToSurvive > 0 && round > enc.roundsToSurvive) { finish("victory"); return; }
                game.log("— Round " + round + " —", "round");
            }
            Unit u = order.get(turn);
            if (u.gone) continue;
            if (u.isHero() && u.hero.dead) continue;
            startOfTurn(u);
            if (over) return;
            if (checkEnd()) return;
            if (!u.alive()) continue;
            if (u.down() && (u.isHero() && (u.hero.stable || u.hero.dead))) continue;
            if (u.down() && !u.isHero()) continue;
            game.fx(Json.obj("t", "turn", "id", u.id, "name", u.name, "side", u.side, "round", round));
            turnStartedAt = System.currentTimeMillis();
            final Unit cu = u;
            if (u.down()) { // the death save was rolled in startOfTurn
                game.schedule(() -> advanceFrom(cu), 900);
                game.changed();
                return;
            }
            if (u.incapacitated()) {
                game.log(u.name + " is " + (u.has("asleep") ? "fast asleep" : "unable to act") + ".", "info");
                game.schedule(() -> advanceFrom(cu), 900);
                game.changed();
                return;
            }
            boolean auto = u.ai || (u.isHero() && game.shouldAutoPlay(u.hero));
            if (auto) game.schedule(() -> runAi(u), u.isHero() ? 900 : 750);
            game.changed();
            return;
        }
        game.changed();
    }

    /** Called from scheduled tasks: end the given unit's turn and move on (ignored if the turn already moved on). */
    void advanceFrom(Unit u) {
        synchronized (game) {
            if (over || current() != u) return;
            nextTurn();
        }
    }

    void startOfTurn(Unit u) {
        u.action = true; u.bonus = true; u.reaction = true; u.slotUsed = false; u.disengaged = false;
        u.reckless = false; u.sneakUsed = false; u.colossusUsed = false; u.frenzyUsed = false; u.moved = false;
        u.move = u.speed();
        u.remove("shield"); u.remove("dodging"); u.remove("no_reactions"); u.remove("reckless_target"); u.remove("hidden_used");
        if (u.has("raging") && u.isHero()) { /* rage lasts while you fight */ }
        if (u.isBoss()) u.legendary = u.def.legendary;
        // recharge specials
        if (u.def != null) for (Monsters.Special s : u.def.specials) {
            if (s.recharge > 0 && !u.ready.getOrDefault(s.id, true)) {
                int r = Dice.die(6);
                if (r >= s.recharge) { u.ready.put(s.id, true); game.log(u.name + "'s " + s.name + " recharges! (rolled " + r + ")", "warn"); }
            }
        }
        // death saving throws
        if (u.isHero() && u.down() && !u.hero.dead && !u.hero.stable) {
            if (enc.nonlethal) { u.hero.stable = true; }
            else {
                Dice.Roll r = Dice.d20(0, false, false, u.hero.hasTrait("lucky"));
                if (u.bardDie > 0 && r.total < 10) { int b = Dice.die(u.bardDie); r.total += b; r.modifier += b; u.bardDie = 0; }
                game.rollFx(r, u.name + " — Death Saving Throw", u.hero.player, false);
                if (r.natural == 20) {
                    u.hero.hp = 1; u.hero.deathFailures = 0; u.hero.deathSuccesses = 0; u.remove("unconscious"); u.remove("prone");
                    game.log(u.name + " rolls a natural 20 and surges back to consciousness with 1 HP!", "good");
                    game.fx(Json.obj("t", "heal", "id", u.id, "amount", 1));
                } else if (r.natural == 1) { u.hero.deathFailures += 2; game.log(u.name + " — natural 1: two death save failures.", "bad"); }
                else if (r.total >= 10) { u.hero.deathSuccesses++; game.log(u.name + " — death save success (" + u.hero.deathSuccesses + "/3)", "info"); }
                else { u.hero.deathFailures++; game.log(u.name + " — death save failure (" + u.hero.deathFailures + "/3)", "bad"); }
                if (u.hero.deathSuccesses >= 3) { u.hero.stable = true; game.log(u.name + " is stable.", "good"); }
                if (u.hero.deathFailures >= 3) { die(u); }
            }
            return;
        }
        // ongoing effects
        if (u.has("burning")) {
            int d = Dice.roll("1d4").total;
            game.log(u.name + " burns for " + d + " fire damage.", "bad");
            damage(u, d, "fire", null, false, "Burning");
            if (!u.alive() || u.down()) return;
        }
        if (u.has("heroism") && u.isHero()) {
            int t = Math.max(1, findCasterMod(u, "heroism"));
            u.hero.tempHp = Math.max(u.hero.tempHp, t);
        }
        // boss aura & braziers
        for (Unit b : units) {
            if (!b.active() || b.def == null || !b.def.traits.contains("flame_aura") || b.side.equals(u.side)) continue;
            if (dist(b, u) <= 1) {
                int d = Dice.roll("1d6").total;
                game.log("Heat radiating from " + b.name + " scorches " + u.name + " (" + d + " fire).", "bad");
                damage(u, d, "fire", b, false, "Flame Aura");
                if (!u.alive() || u.down()) return;
            }
        }
        if (u.isBoss() && u.def.traits.contains("flame_aura")) {
            int braziers = 0;
            for (Unit o : units) if (o.isObject() && o.active() && o.side.equals(u.side)) braziers++;
            if (braziers > 0 && u.hp() < u.maxHp()) {
                int amt = 3 * braziers;
                u.setHp(Math.min(u.maxHp(), u.hp() + amt));
                game.log("The " + braziers + " Binding Brazier(s) flare — " + u.name + " regains " + amt + " HP! (Destroy the braziers!)", "warn");
                game.fx(Json.obj("t", "heal", "id", u.id, "amount", amt));
            }
            if (lavaActive) eruptAndTelegraph();
        }
        // terrain hazards
        if (tile(u.x, u.y) == 'L' && !u.flies()) {
            Dice.Roll d = Dice.roll("2d6");
            game.log(u.name + " starts the turn in lava! (" + d.total + " fire)", "bad");
            damage(u, d.total, "fire", null, false, "Lava");
        }
        // escape a grapple automatically (uses Athletics / Acrobatics)
        if (u.has("grappled") && u.active()) {
            int bonus = u.isHero() ? Math.max(u.hero.skillBonus(Skill.ATHLETICS), u.hero.skillBonus(Skill.ACROBATICS)) : Math.max(u.mod(Ability.STR), u.mod(Ability.DEX));
            Dice.Roll r = Dice.d20(bonus, u.isHero() && u.hero.hasTrait("powerful_build"), false, false);
            game.rollFx(r, u.name + " — Escape Grapple (DC 12)", u.isHero() ? u.hero.player : "", false);
            if (r.total >= 12) { u.remove("grappled"); u.move = u.speed(); game.log(u.name + " wrenches free of the grapple!", "good"); }
            else u.move = 0;
        }
    }

    int findCasterMod(Unit target, String cond) {
        for (var e : concTargets.entrySet()) for (String[] t : e.getValue()) if (t[0].equals(target.id) && t[1].equals(cond)) {
            Unit c = byId(e.getKey());
            if (c != null && c.isHero()) return c.hero.mod(c.hero.castingAbilityFor(""));
        }
        return 2;
    }

    void endOfTurn(Unit u) {
        // timed conditions tick down at the end of the affected creature's turn
        List<String> expired = new ArrayList<>();
        for (var e : u.cond.entrySet()) {
            if (e.getValue() >= 90) continue; // effectively permanent for this fight
            int v = e.getValue() - 1;
            if (v <= 0) expired.add(e.getKey()); else e.setValue(v);
        }
        for (String c : expired) {
            u.cond.remove(c);
            if (c.equals("charmed")) u.charmedBy = "";
            if (c.equals("vexed")) u.vexedBy = "";
            if (Set.of("prone", "sapped", "guided", "vexed", "slowed", "shield", "dodging", "no_reactions", "savage_used", "cleaved",
                    "hidden", "steady_aim", "reckless_target", "ending", "autoplaying").contains(c)) continue;
            game.log(u.name + " is no longer " + c.replace('_', ' ') + ".", "info");
        }
        // repeated saves
        if (u.has("paralyzed") && u.active()) {
            Dice.Roll r = Dice.d20(u.saveBonus(Ability.WIS), false, false, false);
            int dc = condDC(u, "paralyzed");
            game.rollFx(r, u.name + " — Wisdom save vs Hold Person (DC " + dc + ")", "", false);
            if (r.total >= dc) { u.remove("paralyzed"); game.log(u.name + " breaks free of the magical paralysis!", "info"); }
        }
        if (u.has("burning") && u.active()) {
            Dice.Roll r = Dice.d20(u.saveBonus(Ability.DEX), false, false, false);
            if (r.total >= 10) { u.remove("burning"); game.log(u.name + " beats out the flames.", "info"); }
        }
        if (u.isHero()) u.hero.conditions.clear();
    }

    int condDC(Unit u, String cond) {
        for (var e : concTargets.entrySet()) for (String[] t : e.getValue()) if (t[0].equals(u.id) && t[1].equals(cond)) {
            Unit c = byId(e.getKey());
            if (c != null && c.isHero()) return c.hero.spellDC();
        }
        return 13;
    }

    boolean checkEnd() {
        if (over) return true;
        boolean partyUp = false, foesUp = false;
        for (Unit u : units) {
            if (!u.alive() || u.isObject()) continue;
            if (u.side.equals("party") && !u.down()) partyUp = true;
            if (u.side.equals("foe") && !u.down() && u.fleeing == 0) foesUp = true;
        }
        if (!partyUp) { finish("defeat"); return true; }
        if (enc.objective.startsWith("defeat:")) {
            String target = enc.objective.substring(7);
            boolean bossUp = false;
            for (Unit u : units) if (u.def != null && u.def.id.equals(target) && u.side.equals("foe") && u.alive() && !u.down()) bossUp = true;
            if (!bossUp) {
                for (Unit u : units) if (u.side.equals("foe") && u.alive() && !u.isObject() && !u.down()) { u.gone = true; game.fx(Json.obj("t", "flee", "id", u.id)); }
                game.log("With their leader fallen, the remaining foes scatter!", "good");
                finish("victory");
                return true;
            }
        }
        if (!foesUp && !enc.objective.equals("survive")) { finish("victory"); return true; }
        return false;
    }

    void finish(String res) {
        if (over) return;
        over = true;
        result = res;
        for (Unit u : units) {
            if (!u.isHero()) continue;
            Hero hr = u.hero;
            hr.conditions.clear();
            hr.tempHp = 0;
            hr.resources.remove("raging_now");
            if (res.equals("victory") && !hr.dead && hr.hp <= 0) { hr.hp = 1; hr.stable = false; hr.deathFailures = 0; hr.deathSuccesses = 0; game.log(hr.name + " is patched up and back on their feet (1 HP).", "info"); }
        }
        game.fx(Json.obj("t", "battleEnd", "result", res, "title", enc.title));
        game.log(res.equals("victory") ? "★ Victory! " + enc.title + " is won." : "✖ Defeat… the party has fallen.", res.equals("victory") ? "good" : "bad");
        game.schedule(() -> { synchronized (game) { game.combatEnded(this); } }, 2200);
        game.changed();
    }

    void die(Unit u) {
        if (u.isHero()) {
            u.hero.dead = true;
            u.hero.hp = 0;
            game.log("☠ " + u.name + " has died. (They can be revived at the next long rest.)", "bad");
            game.fx(Json.obj("t", "death", "id", u.id));
        } else {
            u.gone = true;
            u.setHp(0);
            xpPool += u.xp();
            game.fx(Json.obj("t", "death", "id", u.id));
            if (u.isObject()) game.log("💥 " + u.name + " shatters!", "good");
            else game.log(enc.nonlethal ? u.name + " is knocked out cold!" : u.name + " falls!", "good");
            dropConcentration(u);
            if (u.def.traits.contains("death_burst") && !enc.nonlethal) {
                game.log(u.name + " explodes in a burst of molten slag!", "warn");
                game.fx(Json.obj("t", "area", "shape", "sphere", "cells", sphereCells(u.x, u.y, 1), "dtype", "fire", "from", u.id));
                for (Unit t : new ArrayList<>(units)) {
                    if (t == u || !t.alive() || t.down() || dist(u, t) > 1) continue;
                    Dice.Roll s = Dice.d20(t.saveBonus(Ability.DEX), false, false, false);
                    int d = Dice.roll("2d4").total;
                    if (s.total >= 11) d /= 2;
                    damage(t, d, "fire", null, false, "Death Burst");
                }
            }
        }
        // a charmed creature whose charmer falls is freed
        for (Unit o : units) if (o.charmedBy.equals(u.id)) { o.remove("charmed"); o.charmedBy = ""; }
    }

    // ===================================================================================== geometry
    char tile(int x, int y) { return x < 0 || y < 0 || x >= w || y >= h ? '#' : tiles[y][x]; }
    boolean wall(int x, int y) { char c = tile(x, y); return c == '#'; }
    boolean blocksWalk(int x, int y) { char c = tile(x, y); return c == '#' || c == 'T'; }
    boolean blocksSight(int x, int y) { char c = tile(x, y); return c == '#' || c == 'T'; }

    int stepCost(Unit u, int x, int y) {
        int worst = 5;
        for (int dy = 0; dy < (u == null ? 1 : u.size); dy++) for (int dx = 0; dx < (u == null ? 1 : u.size); dx++) {
            char c = tile(x + dx, y + dy);
            if ((c == ',' || c == '~' || c == 'B') && !(u != null && (u.flies() || (u.isHero() && u.hero.itemBonus("ignore_difficult") > 0)))) worst = 10;
        }
        return worst;
    }

    Unit occupant(int x, int y, Unit except) {
        for (Unit u : units) {
            if (u == except || u.gone) continue;
            if (u.isHero() && u.hero.dead) continue;
            if (x >= u.x && x < u.x + u.size && y >= u.y && y < u.y + u.size) return u;
        }
        return null;
    }

    boolean canStand(Unit u, int x, int y, int size, boolean ignoreUnits) {
        for (int dy = 0; dy < size; dy++) for (int dx = 0; dx < size; dx++) {
            int cx = x + dx, cy = y + dy;
            if (cx < 0 || cy < 0 || cx >= w || cy >= h) return false;
            if (wall(cx, cy)) return false;
            if (tile(cx, cy) == 'T' && !(u != null && u.flies())) return false;
            if (!ignoreUnits && occupant(cx, cy, u) != null) return false;
        }
        return true;
    }

    boolean canPass(Unit u, int x, int y) {
        for (int dy = 0; dy < u.size; dy++) for (int dx = 0; dx < u.size; dx++) {
            int cx = x + dx, cy = y + dy;
            if (cx < 0 || cy < 0 || cx >= w || cy >= h || wall(cx, cy)) return false;
            if (tile(cx, cy) == 'T' && !u.flies()) return false;
            Unit o = occupant(cx, cy, u);
            if (o != null && !o.side.equals(u.side) && !o.incapacitated() && !o.isObject()) return false;
            if (o != null && o.isObject()) return false;
        }
        return true;
    }

    /** Gap in squares between two footprints (0 = overlapping, 1 = adjacent). */
    static int gap(int ax, int ay, int as, int bx, int by, int bs) {
        int dx = Math.max(0, Math.max(ax - (bx + bs - 1), bx - (ax + as - 1)));
        int dy = Math.max(0, Math.max(ay - (by + bs - 1), by - (ay + as - 1)));
        return Math.max(dx, dy);
    }

    int dist(Unit a, Unit b) { return gap(a.x, a.y, a.size, b.x, b.y, b.size); }
    int feet(Unit a, Unit b) { return dist(a, b) * 5; }

    boolean los(Unit a, Unit b) { return los(a.x + a.size / 2.0, a.y + a.size / 2.0, b.x + b.size / 2.0, b.y + b.size / 2.0, a, b); }

    boolean los(double ax, double ay, double bx, double by, Unit a, Unit b) {
        // sample along the line; walls/trees block. Large creatures see over small obstacles at their own cells.
        double dx = bx - ax, dy = by - ay;
        int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) * 3) + 1;
        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            int cx = (int) Math.floor(ax + dx * t), cy = (int) Math.floor(ay + dy * t);
            if (a != null && cx >= a.x && cx < a.x + a.size && cy >= a.y && cy < a.y + a.size) continue;
            if (b != null && cx >= b.x && cx < b.x + b.size && cy >= b.y && cy < b.y + b.size) continue;
            if (blocksSight(cx, cy)) return false;
        }
        return true;
    }

    boolean losToCell(Unit a, int x, int y) { return los(a.x + a.size / 2.0, a.y + a.size / 2.0, x + 0.5, y + 0.5, a, null) || (a.x == x && a.y == y); }

    /** Half cover from barrels/tables between attacker and target: +2 AC. */
    int cover(Unit a, Unit t) {
        double ax = a.x + a.size / 2.0, ay = a.y + a.size / 2.0, bx = t.x + t.size / 2.0, by = t.y + t.size / 2.0;
        double dx = bx - ax, dy = by - ay;
        int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) * 3) + 1;
        for (int i = 1; i < steps; i++) {
            double tt = i / (double) steps;
            int cx = (int) Math.floor(ax + dx * tt), cy = (int) Math.floor(ay + dy * tt);
            if (cx >= a.x && cx < a.x + a.size && cy >= a.y && cy < a.y + a.size) continue;
            if (cx >= t.x && cx < t.x + t.size && cy >= t.y && cy < t.y + t.size) continue;
            if (tile(cx, cy) == 'B') return 2;
        }
        return 0;
    }

    /** Dijkstra from the unit's position. Returns cost (feet) per cell index, plus predecessor links. */
    Map<Integer, Integer> paths(Unit u, Map<Integer, Integer> prev, boolean aiAvoidHazards) {
        Map<Integer, Integer> cost = new HashMap<>();
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        int start = u.y * w + u.x;
        cost.put(start, 0);
        pq.add(new int[]{0, start});
        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            if (cur[0] > cost.getOrDefault(cur[1], Integer.MAX_VALUE)) continue;
            int cx = cur[1] % w, cy = cur[1] / w;
            for (int[] d : DIRS) {
                int nx = cx + d[0], ny = cy + d[1];
                if (!canPass(u, nx, ny)) continue;
                if (d[0] != 0 && d[1] != 0 && (blocksWalk(cx + d[0], cy) && blocksWalk(cx, cy + d[1])) && !u.flies()) continue;
                int c = cur[0] + stepCost(u, nx, ny);
                if (aiAvoidHazards) {
                    if (tile(nx, ny) == 'L' && !u.flies()) c += 40;
                    for (int[] wcell : warnings) if (wcell[0] == nx && wcell[1] == ny) c += 15;
                }
                int key = ny * w + nx;
                if (c < cost.getOrDefault(key, Integer.MAX_VALUE)) {
                    cost.put(key, c);
                    if (prev != null) prev.put(key, cur[1]);
                    pq.add(new int[]{c, key});
                }
            }
        }
        return cost;
    }

    /** Cells a hero may end movement in this turn, with their cost. */
    Map<String, Integer> reachable(Unit u) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (u.move <= 0) return out;
        int budget = u.move;
        if (u.has("prone")) budget -= u.speed() / 2;
        Map<Integer, Integer> cost = paths(u, null, false);
        for (var e : cost.entrySet()) {
            if (e.getValue() <= budget && e.getValue() > 0) {
                int x = e.getKey() % w, y = e.getKey() / w;
                if (canStand(u, x, y, u.size, false)) out.put(x + "," + y, e.getValue());
            }
        }
        return out;
    }

    List<int[]> pathTo(Unit u, int tx, int ty, boolean ai) {
        Map<Integer, Integer> prev = new HashMap<>();
        Map<Integer, Integer> cost = paths(u, prev, ai);
        int key = ty * w + tx;
        if (!cost.containsKey(key)) return null;
        LinkedList<int[]> p = new LinkedList<>();
        int k = key, start = u.y * w + u.x;
        while (k != start) {
            p.addFirst(new int[]{k % w, k / w, cost.get(k)});
            Integer pk = prev.get(k);
            if (pk == null) return null;
            k = pk;
        }
        return p;
    }

    /** Walk along a path, paying movement, provoking opportunity attacks and triggering traps/lava. */
    void walk(Unit u, List<int[]> path) {
        if (path == null || path.isEmpty()) return;
        if (u.has("prone")) {
            u.remove("prone");
            u.move -= u.speed() / 2;
            game.log(u.name + " stands up.", "info");
        }
        List<Object> trail = new ArrayList<>();
        trail.add(List.of(u.x, u.y));
        int spent = 0;
        for (int[] step : path) {
            int c = step[2] - spent;
            if (c > u.move) break;
            int ox = u.x, oy = u.y;
            // opportunity attacks: leaving reach of a hostile creature
            if (!u.disengaged && !u.has("teleporting")) {
                for (Unit e : new ArrayList<>(units)) {
                    if (!e.active() || e.side.equals(u.side) || !e.reaction || e.incapacitated() || e.has("no_reactions") || e.isObject()) continue;
                    if (e.charmedBy.equals(u.id)) continue;
                    int reach = meleeReach(e);
                    if (reach <= 0) continue;
                    int before = gap(ox, oy, u.size, e.x, e.y, e.size), after = gap(step[0], step[1], u.size, e.x, e.y, e.size);
                    if (before <= reach / 5 && after > reach / 5) {
                        e.reaction = false;
                        game.log(e.name + " makes an Opportunity Attack against " + u.name + "!", "warn");
                        flushTrail(u, trail);
                        trail.clear();
                        trail.add(List.of(u.x, u.y));
                        opportunityAttack(e, u);
                        if (!u.active()) return;
                    }
                }
            }
            u.x = step[0]; u.y = step[1];
            u.move -= c;
            spent = step[2];
            u.moved = true;
            trail.add(List.of(u.x, u.y));
            // hazards
            if (tile(u.x, u.y) == '^' && u.side.equals("party")) {
                tiles[u.y][u.x] = '.';
                revealedTraps.remove(u.y * w + u.x);
                flushTrail(u, trail);
                trail.clear(); trail.add(List.of(u.x, u.y));
                Dice.Roll s = Dice.d20(u.saveBonus(Ability.DEX), false, false, u.isHero() && u.hero.hasTrait("lucky"));
                game.rollFx(s, u.name + " — Dexterity save vs hidden trap (DC 13)", u.isHero() ? u.hero.player : "", false);
                Dice.Roll d = Dice.roll("2d6");
                if (s.total >= 13) { game.log(u.name + " leaps clear of a spiked snare as it snaps shut!", "good"); }
                else {
                    game.log("A hidden spike trap bites into " + u.name + "! (" + d.total + " piercing) Movement halted.", "bad");
                    damage(u, d.total, "piercing", null, false, "Trap");
                    u.move = 0;
                    break;
                }
            }
            if (tile(u.x, u.y) == 'L' && !u.flies()) {
                flushTrail(u, trail);
                trail.clear(); trail.add(List.of(u.x, u.y));
                Dice.Roll d = Dice.roll("2d6");
                game.log(u.name + " wades through lava! (" + d.total + " fire)", "bad");
                damage(u, d.total, "fire", null, false, "Lava");
                if (!u.active()) return;
            }
        }
        flushTrail(u, trail);
        game.changed();
    }

    void flushTrail(Unit u, List<Object> trail) {
        if (trail.size() > 1) game.fx(Json.obj("t", "move", "id", u.id, "path", new ArrayList<>(trail)));
    }

    int meleeReach(Unit e) {
        if (e.isHero()) {
            Items.Weapon wp = e.hero.weapon();
            return wp.ranged ? 0 : 5;
        }
        int r = 0;
        for (Monsters.Attack a : e.def.attacks) if (!a.isRanged() && !a.name.equals("None")) r = Math.max(r, a.reach);
        return r;
    }

    void opportunityAttack(Unit e, Unit t) {
        if (e.isHero()) {
            Items.Weapon wp = e.hero.weapon();
            weaponAttack(e, t, wp, true);
        } else {
            for (Monsters.Attack a : e.def.attacks) if (!a.isRanged()) { monsterAttack(e, t, a); break; }
        }
    }

    // ===================================================================================== attacks
    record Hit(boolean hit, boolean crit, int total, int natural, boolean adv) {}

    /** The core attack roll, including advantage/disadvantage bookkeeping and reactions that can turn a hit into a miss. */
    Hit attackRoll(Unit a, Unit t, int bonus, boolean melee, boolean ranged, int rangeFt, int longFt, String label) {
        boolean adv = false, dis = false;
        List<String> why = new ArrayList<>();
        int d = feet(a, t);
        if (t.has("prone")) { if (d <= 5) { adv = true; why.add("target prone"); } else { dis = true; why.add("target prone at range"); } }
        if (t.has("paralyzed") || t.has("asleep") || t.has("unconscious") || t.down()) { adv = true; why.add("target helpless"); }
        if (t.has("outlined")) { adv = true; why.add("Faerie Fire"); }
        if (t.has("guided")) { adv = true; t.remove("guided"); why.add("Guiding Bolt"); }
        if (t.vexedBy.equals(a.id)) { adv = true; t.vexedBy = ""; t.remove("vexed"); why.add("Vex"); }
        if (a.helpedVs.equals(t.id)) { adv = true; a.helpedVs = ""; why.add("Help"); }
        if (a.has("hidden")) { adv = true; a.remove("hidden"); why.add("hidden"); }
        if (a.has("steady_aim")) { adv = true; a.remove("steady_aim"); why.add("Steady Aim"); }
        if (a.reckless && melee) { adv = true; why.add("Reckless"); }
        if (t.has("reckless_target") && melee) { adv = true; why.add("target reckless"); }
        if (a.def != null && a.def.traits.contains("pack_tactics")) {
            for (Unit o : units) if (o != a && o.active() && o.side.equals(a.side) && dist(o, t) <= 1) { adv = true; why.add("Pack Tactics"); break; }
        }
        if (a.has("poisoned")) { dis = true; why.add("poisoned"); }
        if (a.has("frightened")) { dis = true; why.add("frightened"); }
        if (a.has("prone")) { dis = true; why.add("prone"); }
        if (a.has("sapped")) { dis = true; a.remove("sapped"); why.add("Sap"); }
        if (t.has("dodging") && !t.incapacitated()) { dis = true; why.add("target dodging"); }
        if (ranged) {
            for (Unit o : units) if (o.active() && !o.side.equals(a.side) && !o.incapacitated() && !o.isObject() && dist(o, a) <= 1) { dis = true; why.add("enemy adjacent"); break; }
            if (rangeFt > 0 && d > rangeFt) { dis = true; why.add("long range"); }
        }
        int bless = a.has("blessed") ? Dice.die(4) : 0;
        Dice.Roll r = Dice.d20(bonus + bless, adv, dis, a.isHero() && a.hero.hasTrait("lucky"));
        String mode = (adv && !dis) ? " (Advantage: " + String.join(", ", why) + ")" : (dis && !adv) ? " (Disadvantage: " + String.join(", ", why) + ")" : (adv ? " (Adv & Dis cancel)" : "");
        int ac = t.ac() + cover(a, t);
        boolean critRange = r.natural == 20 || (r.natural == 19 && a.isHero() && a.hero.hasFeature("improved_critical") && !label.startsWith("spell"));
        boolean hit = critRange || (r.natural != 1 && r.total >= ac);
        // Bardic Inspiration rescues a miss
        if (!hit && a.bardDie > 0 && r.natural != 1) {
            int b = Dice.die(a.bardDie);
            a.bardDie = 0;
            r.total += b; r.modifier += b;
            game.log(a.name + " uses Bardic Inspiration (+" + b + ")!", "good");
            hit = r.total >= ac;
        }
        game.rollFx(r, a.name + " → " + t.name + ": " + label + mode, a.isHero() ? a.hero.player : "", false);
        boolean crit = critRange;
        if (hit && !crit && (t.has("paralyzed") || t.has("asleep") || t.has("unconscious")) && d <= 5) { crit = true; game.log("Automatic critical hit on a helpless target!", "warn"); }
        // Shield (reaction, +5 AC)
        if (hit && !critRange && t.isHero() && t.reaction && t.autoShield && t.hero.canCast("shield") && !t.has("no_reactions") && !t.incapacitated()
                && r.total < ac + 5 && t.hero.lowestSlot(1) > 0) {
            int lvl = t.hero.lowestSlot(1);
            t.hero.spendSlot(lvl);
            t.reaction = false;
            t.add("shield", 1);
            hit = false;
            game.log("✦ " + t.name + " snaps up a shimmering SHIELD (+5 AC) — the attack glances off! (reaction, level " + lvl + " slot)", "spell");
            game.fx(Json.obj("t", "shield", "id", t.id));
        }
        // Cutting Words (bard ally reaction)
        if (hit && !critRange && !a.isHero()) {
            for (Unit bd : units) {
                if (!bd.isHero() || !bd.active() || !bd.reaction || bd.side.equals(a.side) || !bd.hero.hasFeature("cutting_words") || bd.hero.res("bardic_inspiration") <= 0) continue;
                if (feet(bd, a) > 60) continue;
                int cw = Dice.die(6);
                if (r.total - cw < ac) {
                    bd.hero.spend("bardic_inspiration");
                    bd.reaction = false;
                    hit = false;
                    game.log("✦ " + bd.name + " lets loose Cutting Words (-" + cw + ") and " + a.name + " flubs the attack!", "spell");
                    break;
                }
            }
        }
        // Monster Shield-like reaction: none. Goliath/others handled in damage().
        if (crit && hit) game.log("CRITICAL HIT!", "crit");
        return new Hit(hit, crit && hit, r.total, r.natural, adv && !dis);
    }

    void weaponAttack(Unit a, Unit t, Items.Weapon wp, boolean opportunity) {
        Hero hr = a.hero;
        boolean melee = !wp.ranged && feet(a, t) <= 5;
        boolean thrown = !wp.ranged && !melee && wp.thrown;
        boolean ranged = wp.ranged || thrown;
        int bonus = hr.weaponAttack(wp);
        if (thrown) bonus = hr.mod(hr.weaponAbility(wp)) + hr.prof() + hr.itemBonus("attack");
        int rangeFt = wp.ranged ? wp.range : (thrown ? wp.thrownRange : 5);
        if (hr.hasFeature("sacred_weapon") && a.has("sacred_weapon")) bonus += Math.max(1, hr.mod(Ability.CHA));
        if (a.has("raging") && !ranged && hr.weaponAbility(wp) == Ability.STR) { /* advantage on STR checks only */ }
        Hit res = attackRoll(a, t, bonus, melee, ranged, rangeFt, wp.longRange, wp.name + (opportunity ? " (Opportunity)" : thrown ? " (thrown)" : ""));
        String kind = ranged ? "ranged" : "melee";
        if (!res.hit()) {
            game.fx(Json.obj("t", "attack", "from", a.id, "to", t.id, "hit", false, "kind", kind, "weapon", wp.id));
            if (wp.mastery.equals("graze") && !opportunity) {
                int g = Math.max(0, hr.weaponDamageMod(wp));
                if (g > 0) { game.log("Graze: " + t.name + " still takes " + g + " damage.", "info"); damage(t, g, wp.type, a, false, "Graze"); }
            } else game.log(a.name + " misses " + t.name + ".", "miss");
            return;
        }
        String dice = wp.dice;
        if (!wp.versatile.isEmpty() && !hr.classDef().shield && hr.offWeaponId.isEmpty()) dice = wp.versatile;
        Dice.Roll dr = Dice.damage(dice, res.crit());
        if (hr.hasFeat("savage_attacker") && !a.has("savage_used")) {
            Dice.Roll second = Dice.damage(dice, res.crit());
            if (second.total > dr.total) dr = second;
            a.add("savage_used", 1);
        }
        int dmg = Math.max(1, dr.total + hr.weaponDamageMod(wp));
        if (dice.equals("1")) dmg = Math.max(1, 1 + hr.mod(Ability.STR));
        List<String> extras = new ArrayList<>();
        if (a.has("raging") && hr.weaponAbility(wp) == Ability.STR && !ranged) { dmg += 2; extras.add("Rage +2"); }
        // Sneak Attack
        if (hr.hasFeature("sneak_attack") && !a.sneakUsed && (wp.finesse || ranged)) {
            boolean allyNear = false;
            for (Unit o : units) if (o != a && o.active() && o.side.equals(a.side) && dist(o, t) <= 1) allyNear = true;
            boolean noDis = !a.has("poisoned") && !a.has("frightened");
            if ((allyNear && noDis) || res.adv()) {
                int n = (hr.level + 1) / 2;
                Dice.Roll sa = Dice.damage(n + "d6", res.crit());
                dmg += sa.total; a.sneakUsed = true; extras.add("Sneak Attack " + sa.total);
            }
        }
        if (t.markedBy.equals(a.id)) { Dice.Roll hm = Dice.damage("1d6", res.crit()); dmg += hm.total; extras.add("Hunter's Mark " + hm.total); }
        if (hr.hasFeature("hunters_prey") && !a.colossusUsed && t.hp() < t.maxHp()) { Dice.Roll cs = Dice.damage("1d8", res.crit()); dmg += cs.total; a.colossusUsed = true; extras.add("Colossus Slayer " + cs.total); }
        if (hr.hasFeature("frenzy") && a.reckless && a.has("raging") && !a.frenzyUsed) { Dice.Roll fz = Dice.damage("2d6", res.crit()); dmg += fz.total; a.frenzyUsed = true; extras.add("Frenzy " + fz.total); }
        game.fx(Json.obj("t", "attack", "from", a.id, "to", t.id, "hit", true, "crit", res.crit(), "kind", kind, "weapon", wp.id));
        game.log(a.name + " hits " + t.name + " with " + wp.name + " for " + dmg + " " + wp.type + (extras.isEmpty() ? "" : " (" + String.join(", ", extras) + ")") + ".", "hit");
        damage(t, dmg, wp.type, a, res.crit(), wp.name);
        // Divine Smite
        if (a.smiteArmed && !ranged && t.alive() && (a.bonus || hr.res("free_smite") > 0)) {
            int lvl = hr.res("free_smite") > 0 ? 1 : hr.lowestSlot(1);
            if (lvl > 0 && (hr.res("free_smite") > 0 || (!a.slotUsed && hr.spendSlot(lvl)))) {
                if (hr.res("free_smite") > 0) hr.spend("free_smite"); else a.slotUsed = true;
                a.bonus = false;
                a.smiteArmed = false;
                int nd = 1 + lvl + (t.creatureType().equals("undead") || t.creatureType().equals("fiend") ? 1 : 0);
                Dice.Roll sm = Dice.damage(nd + "d8", res.crit());
                game.log("✦ DIVINE SMITE! Radiant fire crashes down for " + sm.total + ".", "spell");
                game.fx(Json.obj("t", "spell", "spell", "divine_smite", "from", a.id, "to", t.id, "dtype", "radiant"));
                damage(t, sm.total, "radiant", a, res.crit(), "Divine Smite");
            }
        }
        // Weapon Mastery
        if (t.alive() && !t.down() && !opportunity) mastery(a, t, wp);
        // Flame aura retaliation
        if (!ranged && t.def != null && t.def.traits.contains("flame_aura") && a.active()) {
            int fd = Dice.roll("1d6").total;
            game.log("Searing heat lashes back at " + a.name + " (" + fd + " fire).", "bad");
            damage(a, fd, "fire", t, false, "Flame Aura");
        }
    }

    void mastery(Unit a, Unit t, Items.Weapon wp) {
        Hero hr = a.hero;
        if (!hr.hasFeature("weapon_mastery") && !hr.classId.equals("rogue")) return;
        int dc = 8 + hr.mod(hr.weaponAbility(wp)) + hr.prof();
        switch (wp.mastery) {
            case "topple" -> {
                if (t.size > 2 || t.isObject()) return;
                Dice.Roll s = Dice.d20(t.saveBonus(Ability.CON), false, false, false);
                if (s.total < dc) { t.add("prone", 99); game.log("Topple! " + t.name + " is knocked Prone.", "good"); }
            }
            case "sap" -> { t.add("sapped", 2); game.log("Sap: " + t.name + " has Disadvantage on its next attack.", "info"); }
            case "vex" -> { t.vexedBy = a.id; t.add("vexed", 2); game.log("Vex: " + a.name + " has Advantage on the next attack against " + t.name + ".", "info"); }
            case "slow" -> { t.add("slowed", 1); game.log("Slow: " + t.name + "'s Speed drops by 10 ft.", "info"); }
            case "push" -> { if (t.size <= 2) push(a, t, 2); }
            case "cleave" -> {
                if (a.has("cleaved")) return;
                for (Unit o : units) {
                    if (o == t || !o.active() || o.side.equals(a.side) || o.isObject() || dist(o, t) > 1 || dist(o, a) > 1) continue;
                    a.add("cleaved", 1);
                    game.log("Cleave! The swing carries on into " + o.name + ".", "info");
                    Hit r = attackRoll(a, o, hr.weaponAttack(wp), true, false, 5, 0, wp.name + " (Cleave)");
                    if (r.hit()) { int d = Dice.damage(wp.dice, r.crit()).total; damage(o, d, wp.type, a, r.crit(), "Cleave"); game.log("Cleave hits " + o.name + " for " + d + ".", "hit"); }
                    break;
                }
            }
            default -> {}
        }
    }

    void push(Unit from, Unit t, int squares) {
        int dx = Integer.signum(t.x - from.x), dy = Integer.signum(t.y - from.y);
        if (dx == 0 && dy == 0) dx = 1;
        List<Object> trail = new ArrayList<>();
        trail.add(List.of(t.x, t.y));
        for (int i = 0; i < squares; i++) {
            int nx = t.x + dx, ny = t.y + dy;
            if (!canStand(t, nx, ny, t.size, false)) break;
            t.x = nx; t.y = ny;
            trail.add(List.of(t.x, t.y));
        }
        if (trail.size() > 1) { game.fx(Json.obj("t", "move", "id", t.id, "path", trail, "push", true)); game.log(t.name + " is pushed back " + ((trail.size() - 1) * 5) + " ft.", "info"); }
        if (tile(t.x, t.y) == 'L' && !t.flies()) { int d = Dice.roll("2d6").total; game.log(t.name + " is shoved into lava! (" + d + " fire)", "good"); damage(t, d, "fire", from, false, "Lava"); }
    }

    void monsterAttack(Unit a, Unit t, Monsters.Attack at) {
        boolean ranged = at.isRanged() && dist(a, t) > 1;
        Hit res = attackRoll(a, t, at.toHit, !ranged, ranged, at.range, at.longRange, at.name);
        String kind = at.spell ? "spell" : ranged ? "ranged" : "melee";
        game.fx(Json.obj("t", "attack", "from", a.id, "to", t.id, "hit", res.hit(), "crit", res.crit(), "kind", kind, "dtype", at.type, "name", at.name));
        if (!res.hit()) { game.log(a.name + "'s " + at.name + " misses " + t.name + ".", "miss"); return; }
        int dmg = Math.max(0, Dice.damage(at.damage, res.crit()).total);
        String msg = a.name + " hits " + t.name + " with " + at.name + " for " + dmg + " " + at.type;
        int extra = 0;
        if (!at.extra.isEmpty()) { extra = Dice.damage(at.extra, res.crit()).total; msg += " + " + extra + " " + at.extraType; }
        game.log(msg + ".", "hurt");
        damage(t, dmg, at.type, a, res.crit(), at.name);
        if (extra > 0 && t.alive()) damage(t, extra, at.extraType, a, false, at.name);
        if (!at.rider.isEmpty() && t.active()) {
            boolean saved = false;
            if (at.riderSave != null && !t.condImmune(at.rider)) {
                Dice.Roll s = Dice.d20(t.saveBonus(at.riderSave), at.rider.equals("grappled") && t.isHero() && t.hero.hasTrait("powerful_build"), false, t.isHero() && t.hero.hasTrait("lucky"));
                game.rollFx(s, t.name + " — " + at.riderSave.full + " save vs " + at.rider + " (DC " + at.riderDC + ")", t.isHero() ? t.hero.player : "", false);
                saved = s.total >= at.riderDC;
            }
            if (!saved && !t.condImmune(at.rider)) {
                switch (at.rider) {
                    case "prone" -> { t.add("prone", 99); game.log(t.name + " is knocked Prone!", "bad"); }
                    case "grappled" -> { t.add("grappled", 99); game.log(t.name + " is Grappled!", "bad"); }
                    case "burning" -> { t.add("burning", 99); game.log(t.name + " catches fire!", "bad"); }
                    default -> { t.add(at.rider, 2); game.log(t.name + " is " + at.rider + "!", "bad"); }
                }
            }
        }
        if (at.type.equals("fire") && !at.extraType.equals("fire") && t.isHero()) { /* flavour */ }
    }

    // ===================================================================================== damage & healing
    int damage(Unit t, int amount, String type, Unit src, boolean crit, String sourceName) {
        if (!t.alive() || amount <= 0) return 0;
        String why = "";
        if (t.immune().contains(type)) { game.log(t.name + " is immune to " + type + "!", "info"); game.fx(Json.obj("t", "dmg", "id", t.id, "amount", 0, "dtype", type, "note", "IMMUNE")); return 0; }
        if (t.vulnerable().contains(type)) { amount *= 2; why = "VULNERABLE"; game.log(t.name + " is vulnerable to " + type + " — double damage!", "crit"); }
        else if (t.resist().contains(type)) { amount = amount / 2; why = "resisted"; }
        // Goliath: Stone's Endurance (reaction)
        if (t.isHero() && t.hero.hasTrait("stones_endurance") && t.reaction && amount >= 6 && t.hero.res("stones_endurance") > 0 && !t.down()) {
            int red = Dice.roll("1d12").total + t.hero.mod(Ability.CON);
            t.hero.spend("stones_endurance");
            t.reaction = false;
            amount = Math.max(0, amount - red);
            game.log(t.name + " braces with Stone's Endurance (-" + red + ").", "good");
        }
        // temporary hit points first
        int tmp = t.isHero() ? t.hero.tempHp : t.temp;
        int absorbed = Math.min(tmp, amount);
        if (t.isHero()) t.hero.tempHp -= absorbed; else t.temp -= absorbed;
        amount -= absorbed;
        game.fx(Json.obj("t", "dmg", "id", t.id, "amount", amount + absorbed, "dtype", type, "crit", crit, "note", why));
        if (amount <= 0) return 0;
        if (t.has("asleep")) { t.remove("asleep"); t.remove("prone"); game.log(t.name + " jolts awake!", "info"); }
        if (src != null && t.charmedBy.length() > 0) {
            Unit ch = byId(t.charmedBy);
            if (ch != null && ch.side.equals(src.side)) { t.remove("charmed"); t.charmedBy = ""; game.log("The charm on " + t.name + " shatters as it is attacked!", "info"); }
        }
        if (t.isHero()) {
            Hero hr = t.hero;
            if (hr.hp <= 0) {
                if (enc.nonlethal) return 0;
                hr.deathFailures += crit ? 2 : 1;
                hr.stable = false;
                game.log(t.name + " takes damage while dying! (" + hr.deathFailures + "/3 failures)", "bad");
                if (hr.deathFailures >= 3) die(t);
                return amount;
            }
            int over = amount - hr.hp;
            hr.hp -= amount;
            if (hr.hp <= 0) {
                if (!enc.nonlethal && over >= hr.maxHp) { hr.hp = 0; die(t); return amount; }
                if (hr.hasTrait("relentless_endurance") && hr.res("relentless_endurance") > 0 && !enc.nonlethal) {
                    hr.spend("relentless_endurance");
                    hr.hp = 1;
                    game.log(t.name + " refuses to fall! (Relentless Endurance — 1 HP)", "good");
                } else {
                    hr.hp = 0;
                    t.add("prone", 99);
                    hr.deathSuccesses = 0; hr.deathFailures = 0;
                    hr.stable = enc.nonlethal;
                    t.remove("raging"); hr.resources.remove("raging_now");
                    game.log(enc.nonlethal ? t.name + " is knocked senseless!" : "⚠ " + t.name + " falls unconscious and is dying!", "bad");
                    dropConcentration(t);
                    game.fx(Json.obj("t", "down", "id", t.id));
                }
            } else concentrationCheck(t, amount);
        } else {
            int newHp = t.hp() - amount;
            if (newHp <= 0 && t.def.traits.contains("undead_fortitude") && !type.equals("radiant") && !crit && !enc.nonlethal) {
                Dice.Roll s = Dice.d20(t.saveBonus(Ability.CON), false, false, false);
                if (s.total >= 5 + amount) { t.setHp(1); game.log(t.name + " lurches back up! (Undead Fortitude)", "warn"); return amount; }
            }
            t.setHp(Math.max(0, newHp));
            if (t.hp() <= 0) die(t);
            else { concentrationCheck(t, amount); checkPhases(t); }
        }
        return amount;
    }

    void concentrationCheck(Unit t, int amount) {
        if (t.conc.isEmpty()) return;
        int dc = Math.min(30, Math.max(10, amount / 2));
        Dice.Roll s = Dice.d20(t.saveBonus(Ability.CON), false, false, false);
        game.rollFx(s, t.name + " — Concentration (DC " + dc + ")", t.isHero() ? t.hero.player : "", false);
        if (s.total < dc) { game.log(t.name + " loses concentration on " + Optional.ofNullable(Spells.get(t.conc)).map(x -> x.name).orElse(t.conc) + "!", "bad"); dropConcentration(t); }
    }

    void dropConcentration(Unit c) {
        if (c.conc.isEmpty()) return;
        List<String[]> l = concTargets.remove(c.id);
        if (l != null) for (String[] e : l) {
            Unit u = byId(e[0]);
            if (u == null) continue;
            u.remove(e[1]);
            if (e[1].equals("marked")) u.markedBy = "";
            if (e[1].equals("charmed")) u.charmedBy = "";
        }
        c.remove("spiritual_weapon"); c.remove("flaming_sphere");
        c.conc = "";
    }

    void addConc(Unit caster, String spell, Unit target, String cond) {
        concTargets.computeIfAbsent(caster.id, k -> new ArrayList<>()).add(new String[]{target.id, cond});
    }

    int heal(Unit t, int amount, String from) {
        if (!t.alive() || amount <= 0) return 0;
        if (t.isHero()) {
            int got = t.hero.heal(amount);
            t.remove("prone");
            game.fx(Json.obj("t", "heal", "id", t.id, "amount", got));
            game.log(t.name + " regains " + got + " HP" + (from.isEmpty() ? "" : " (" + from + ")") + ".", "heal");
            return got;
        }
        int before = t.hp();
        t.setHp(Math.min(t.maxHp(), t.hp() + amount));
        game.fx(Json.obj("t", "heal", "id", t.id, "amount", t.hp() - before));
        game.log(t.name + " regains " + (t.hp() - before) + " HP.", "heal");
        return t.hp() - before;
    }

    // ===================================================================================== boss phases & hazards
    void checkPhases(Unit t) {
        if (!t.isBoss()) return;
        int pct = (int) Math.round(100.0 * t.hp() / t.maxHp());
        for (int i = 0; i < enc.phases.size(); i++) {
            if (phasesDone.contains(i)) continue;
            Map<String, Object> ph = enc.phases.get(i);
            if (pct > Json.integer(ph, "atHpPercent", 50)) continue;
            phasesDone.add(i);
            String text = Json.str(ph, "text", "");
            game.fx(Json.obj("t", "phase", "text", text, "id", t.id));
            game.log("⚡ " + text, "phase");
            for (Map<String, Object> fx : Json.maps(ph, "effects")) {
                switch (Json.str(fx, "op", "")) {
                    case "summon" -> {
                        int n = Json.integer(fx, "count", 2);
                        for (int k = 0; k < n; k++) {
                            int[] spot = nearFree(new int[]{t.x, t.y}, 1);
                            if (spot == null) break;
                            Unit s = spawn(Json.str(fx, "monster", "magma_mephit"), spot[0], spot[1], t.side);
                            if (s == null) break;
                            s.summoned = true;
                            s.init = t.init - 1;
                            insertAfter(t, s);
                            game.fx(Json.obj("t", "summon", "id", s.id));
                        }
                    }
                    case "lava" -> { lavaActive = true; lavaCount = Json.integer(fx, "count", 4); telegraph(); }
                    case "heal" -> heal(t, Dice.roll(Json.str(fx, "dice", "2d8")).total, "");
                    case "say" -> game.log(Json.str(fx, "text", ""), "boss");
                    case "flag" -> game.flags.add(Json.str(fx, "flag", ""));
                    default -> {}
                }
            }
        }
    }

    void telegraph() {
        warnings.clear();
        List<int[]> heroesCells = new ArrayList<>();
        for (Unit u : units) if (u.side.equals("party") && u.active()) heroesCells.add(new int[]{u.x, u.y});
        Random r = new Random();
        int placed = 0, guard = 0;
        // at least half of the eruptions land under heroes so they must move
        Collections.shuffle(heroesCells);
        for (int[] c : heroesCells) { if (placed >= Math.max(1, lavaCount / 2)) break; warnings.add(c); placed++; }
        while (placed < lavaCount && guard++ < 400) {
            int x = r.nextInt(w), y = r.nextInt(h);
            if (wall(x, y) || tile(x, y) == 'T') continue;
            boolean dup = false;
            for (int[] c : warnings) if (c[0] == x && c[1] == y) dup = true;
            if (dup) continue;
            warnings.add(new int[]{x, y});
            placed++;
        }
        game.log("The ground cracks and glows in " + warnings.size() + " places — molten rock will erupt there next round!", "warn");
        game.fx(Json.obj("t", "telegraph", "cells", cellsJson(warnings)));
    }

    void eruptAndTelegraph() {
        if (!warnings.isEmpty()) {
            game.fx(Json.obj("t", "erupt", "cells", cellsJson(warnings)));
            scorched.clear();
            scorched.addAll(warnings);
            for (int[] c : warnings) {
                for (Unit u : new ArrayList<>(units)) {
                    if (!u.active() || u.isBoss() || u.isObject() || u.flies()) continue;
                    if (c[0] >= u.x && c[0] < u.x + u.size && c[1] >= u.y && c[1] < u.y + u.size) {
                        Dice.Roll s = Dice.d20(u.saveBonus(Ability.DEX), false, false, u.isHero() && u.hero.hasTrait("lucky"));
                        game.rollFx(s, u.name + " — Dexterity save vs eruption (DC 13)", u.isHero() ? u.hero.player : "", false);
                        int d = Dice.roll("2d6").total;
                        if (s.total >= 13) d /= 2;
                        game.log("Magma erupts beneath " + u.name + "! (" + d + " fire)", "bad");
                        damage(u, d, "fire", null, false, "Eruption");
                    }
                }
            }
        }
        telegraph();
    }

    static List<Object> cellsJson(List<int[]> cells) {
        List<Object> o = new ArrayList<>();
        for (int[] c : cells) o.add(List.of(c[0], c[1]));
        return o;
    }

    void legendaryAction(Unit after) {
        for (Unit b : units) {
            if (!b.isBoss() || !b.active() || b.legendary <= 0 || b.incapacitated() || !b.side.equals("foe")) continue;
            if (after.side.equals(b.side)) continue;
            // only spend after hero turns, one per turn
            Unit target = null;
            int best = 99;
            for (Unit u : units) if (u.side.equals("party") && u.active() && dist(b, u) < best) { best = dist(b, u); target = u; }
            if (target == null) return;
            int near = 0;
            for (Unit u : units) if (u.side.equals("party") && u.active() && dist(b, u) <= 2) near++;
            b.legendary--;
            if (near >= 2) {
                Monsters.Special gust = null;
                for (Monsters.Special s : b.def.specials) if (s.id.equals("wing_gust")) gust = s;
                if (gust != null) { game.log("⟡ Legendary Action: " + b.name + " beats its wings!", "boss"); useSpecial(b, gust, b.x, b.y); return; }
            }
            if (best <= 1) {
                Monsters.Attack claw = b.def.attacks.get(b.def.attacks.size() - 1);
                game.log("⟡ Legendary Action: " + b.name + " lashes out!", "boss");
                monsterAttack(b, target, claw);
            } else {
                game.log("⟡ Legendary Action: " + b.name + " prowls closer.", "boss");
                Unit tt = target;
                b.move = b.speed() / 2;
                approach(b, tt, meleeReachSquares(b));
            }
            return;
        }
    }

    // ===================================================================================== areas
    List<int[]> sphereCells(int cx, int cy, int radiusSquares) {
        List<int[]> out = new ArrayList<>();
        for (int y = cy - radiusSquares; y <= cy + radiusSquares; y++) for (int x = cx - radiusSquares; x <= cx + radiusSquares; x++) {
            if (x < 0 || y < 0 || x >= w || y >= h || wall(x, y)) continue;
            double d = Math.hypot(x - cx, y - cy);
            if (d <= radiusSquares + 0.5) out.add(new int[]{x, y});
        }
        return out;
    }

    /** A cone from the origin unit towards (tx,ty), `len` squares long. Matches the client preview exactly. */
    List<int[]> coneCells(Unit o, int tx, int ty, int len) {
        List<int[]> out = new ArrayList<>();
        double ox = o.x + (o.size - 1) / 2.0, oy = o.y + (o.size - 1) / 2.0;
        double ang = Math.atan2(ty - oy, tx - ox);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            if (wall(x, y)) continue;
            if (x >= o.x && x < o.x + o.size && y >= o.y && y < o.y + o.size) continue;
            double dx = x - ox, dy = y - oy;
            double d = Math.hypot(dx, dy) - (o.size - 1) / 2.0;
            if (d > len + 0.3 || d < 0.5) continue;
            double a = Math.atan2(dy, dx) - ang;
            while (a > Math.PI) a -= 2 * Math.PI;
            while (a < -Math.PI) a += 2 * Math.PI;
            if (Math.abs(a) <= Math.toRadians(30)) out.add(new int[]{x, y});
        }
        return out;
    }

    /** A cube of `len` squares whose near face touches the caster, pushed toward (tx,ty). */
    List<int[]> cubeCells(Unit o, int tx, int ty, int len) {
        int dx = Integer.signum(tx - o.x), dy = Integer.signum(ty - o.y);
        if (dx == 0 && dy == 0) dx = 1;
        int x0, y0;
        if (dx > 0) x0 = o.x + o.size; else if (dx < 0) x0 = o.x - len; else x0 = o.x - (len - o.size) / 2;
        if (dy > 0) y0 = o.y + o.size; else if (dy < 0) y0 = o.y - len; else y0 = o.y - (len - o.size) / 2;
        List<int[]> out = new ArrayList<>();
        for (int y = y0; y < y0 + len; y++) for (int x = x0; x < x0 + len; x++) if (x >= 0 && y >= 0 && x < w && y < h && !wall(x, y)) out.add(new int[]{x, y});
        return out;
    }

    List<Unit> unitsIn(List<int[]> cells, Unit exclude) {
        List<Unit> out = new ArrayList<>();
        for (Unit u : units) {
            if (u == exclude || !u.alive() || u.gone) continue;
            if (u.isHero() && u.hero.dead) continue;
            boolean in = false;
            for (int[] c : cells) if (c[0] >= u.x && c[0] < u.x + u.size && c[1] >= u.y && c[1] < u.y + u.size) { in = true; break; }
            if (in) out.add(u);
        }
        return out;
    }

    // ===================================================================================== monster specials
    void useSpecial(Unit u, Monsters.Special s, int tx, int ty) {
        if (s.recharge > 0) u.ready.put(s.id, false);
        if (s.uses > 0) u.uses.merge(s.id, -1, Integer::sum);
        String shape = s.shape;
        if (shape.equals("heal")) {
            Unit best = null;
            for (Unit o : units) if (o.side.equals(u.side) && o.active() && !o.isObject() && (best == null || o.hp() * 1.0 / o.maxHp() < best.hp() * 1.0 / best.maxHp())) best = o;
            if (best != null) {
                game.log(u.name + " uses " + s.name + "!", "boss");
                game.fx(Json.obj("t", "spell", "spell", "heal", "from", u.id, "to", best.id, "dtype", "radiant"));
                heal(best, Dice.roll(s.damage).total, s.name);
            }
            return;
        }
        if (shape.equals("summon")) {
            game.log(u.name + " uses " + s.name + "!", "boss");
            for (int k = 0; k < Math.max(1, s.summonCount); k++) {
                int[] spot = nearFree(new int[]{u.x, u.y}, 1);
                if (spot == null) break;
                Unit m = spawn(s.summon, spot[0], spot[1], u.side);
                if (m == null) break;
                m.summoned = true;
                insertAfter(u, m);
                game.fx(Json.obj("t", "summon", "id", m.id));
            }
            return;
        }
        List<int[]> cells = switch (shape) {
            case "cone" -> coneCells(u, tx, ty, s.size / 5);
            case "sphere" -> s.range == 0 ? sphereAround(u, s.size / 5) : sphereCells(tx, ty, s.size / 5);
            case "line" -> lineCells(u, tx, ty, s.size / 5);
            default -> sphereCells(tx, ty, 0);
        };
        game.log("🔥 " + u.name + " unleashes " + s.name + "!", "boss");
        game.fx(Json.obj("t", "area", "shape", shape, "cells", cellsJson(cells), "dtype", s.type, "from", u.id, "name", s.name));
        for (Unit t : unitsIn(cells, u)) {
            if (t.side.equals(u.side) || t.isObject()) continue;
            boolean adv = t.isHero() && s.save == Ability.DEX && t.hero.hasFeature("danger_sense");
            if (t.has("dodging") && s.save == Ability.DEX) adv = true;
            Dice.Roll r = Dice.d20(t.saveBonus(s.save), adv, false, t.isHero() && t.hero.hasTrait("lucky"));
            if (t.bardDie > 0 && r.total < s.dc) { int b = Dice.die(t.bardDie); t.bardDie = 0; r.total += b; r.modifier += b; }
            game.rollFx(r, t.name + " — " + s.save.full + " save vs " + s.name + " (DC " + s.dc + ")", t.isHero() ? t.hero.player : "", false);
            int d = s.damage.isEmpty() ? 0 : Dice.roll(s.damage).total;
            boolean ok = r.total >= s.dc;
            if (ok) d = s.half ? d / 2 : 0;
            if (d > 0) { game.log(t.name + (ok ? " partly avoids " : " is caught by ") + s.name + " (" + d + " " + s.type + ").", ok ? "info" : "hurt"); damage(t, d, s.type, u, false, s.name); }
            else if (ok) game.log(t.name + " avoids " + s.name + "!", "good");
            if (!ok && !s.rider.isEmpty() && t.active()) {
                if (s.rider.equals("prone")) { t.add("prone", 99); push(u, t, 2); }
                else t.add(s.rider, 2);
                game.log(t.name + " is " + s.rider + "!", "bad");
            }
        }
    }

    List<int[]> sphereAround(Unit u, int r) {
        List<int[]> out = new ArrayList<>();
        for (int y = u.y - r; y < u.y + u.size + r; y++) for (int x = u.x - r; x < u.x + u.size + r; x++) {
            if (x < 0 || y < 0 || x >= w || y >= h || wall(x, y)) continue;
            if (x >= u.x && x < u.x + u.size && y >= u.y && y < u.y + u.size) continue;
            out.add(new int[]{x, y});
        }
        return out;
    }

    List<int[]> lineCells(Unit u, int tx, int ty, int len) {
        List<int[]> out = new ArrayList<>();
        double ox = u.x + (u.size - 1) / 2.0, oy = u.y + (u.size - 1) / 2.0;
        double ang = Math.atan2(ty - oy, tx - ox);
        for (int i = 1; i <= len; i++) {
            int x = (int) Math.round(ox + Math.cos(ang) * i), y = (int) Math.round(oy + Math.sin(ang) * i);
            if (wall(x, y)) break;
            out.add(new int[]{x, y});
        }
        return out;
    }

    /** Add a summoned creature to the initiative order right after its summoner without disturbing whose turn it is. */
    void insertAfter(Unit summoner, Unit s) {
        int idx = order.indexOf(summoner);
        int at = idx < 0 ? order.size() : idx + 1;
        order.add(at, s);
        if (at <= turn) turn++;
    }

    Unit byId(String id) {
        for (Unit u : units) if (u.id.equals(id)) return u;
        return null;
    }

    Unit unitForHero(String heroId) {
        for (Unit u : units) if (u.isHero() && u.hero.id.equals(heroId)) return u;
        return null;
    }

    // ===================================================================================== hero actions
    /** Execute a player's combat action. Returns an error message, or null on success. */
    String act(Unit u, Map<String, Object> req) {
        if (over) return "The battle is over.";
        if (current() != u) return "It's not " + u.name + "'s turn.";
        if (u.down()) return u.name + " is down.";
        String type = Json.str(req, "type", "");
        String err = switch (type) {
            case "move" -> doMove(u, Json.integer(req, "x", u.x), Json.integer(req, "y", u.y));
            case "attack" -> doAttack(u, byId(Json.str(req, "target", "")));
            case "cast" -> doCast(u, req);
            case "feature" -> doFeature(u, req);
            case "item" -> doItem(u, req);
            case "dash" -> { if (!u.action) yield "No action left."; u.action = false; u.move += u.speed(); game.log(u.name + " Dashes.", "info"); yield null; }
            case "disengage" -> { if (!u.action) yield "No action left."; u.action = false; u.disengaged = true; game.log(u.name + " Disengages.", "info"); yield null; }
            case "dodge" -> { if (!u.action) yield "No action left."; u.action = false; u.add("dodging", 1); game.log(u.name + " takes the Dodge action.", "info"); yield null; }
            case "hide" -> doHide(u, false);
            case "help" -> doHelp(u, byId(Json.str(req, "target", "")));
            case "toggleShield" -> { u.autoShield = !u.autoShield; yield null; }
            case "smite" -> { u.smiteArmed = !u.smiteArmed; game.log(u.name + (u.smiteArmed ? " readies a Divine Smite for the next melee hit." : " lowers the smite."), "info"); yield null; }
            case "end" -> {
                if (u.has("ending")) yield "Already ending the turn.";
                u.add("ending", 1);
                game.log(u.name + " ends the turn.", "info");
                game.schedule(() -> advanceFrom(u), 250);
                yield null;
            }
            default -> "Unknown action '" + type + "'";
        };
        if (err == null) { checkEnd(); game.changed(); }
        return err;
    }

    String doMove(Unit u, int x, int y) {
        if (u.move <= 0) return "No movement left.";
        Map<String, Integer> r = reachable(u);
        if (!r.containsKey(x + "," + y)) return "Can't reach that square.";
        walk(u, pathTo(u, x, y, false));
        return null;
    }

    String doAttack(Unit u, Unit t) {
        if (t == null || !t.alive()) return "Pick a target.";
        if (!u.action) return "No action left.";
        if (t.side.equals(u.side)) return "That's an ally!";
        if (u.charmedBy.equals(t.id)) return u.name + " is charmed and can't attack " + t.name + ".";
        Items.Weapon wp = u.hero.weapon();
        int d = feet(u, t);
        if (wp.ranged) { if (d > wp.longRange) return "Out of range (" + wp.longRange + " ft)."; }
        else if (d > 5) {
            if (!wp.thrown) return "Too far for " + wp.name + " — move closer.";
            if (d > wp.longRange) return "Out of throwing range.";
        }
        if ((wp.ranged || d > 5) && !los(u, t)) return "No line of sight.";
        u.action = false;
        u.remove("savage_used");
        weaponAttack(u, t, wp, false);
        if (u.hero.classId.equals("barbarian") && u.reckless) u.add("reckless_target", 1);
        return null;
    }

    String doHide(Unit u, boolean bonusAction) {
        if (bonusAction ? !u.bonus : !u.action) return bonusAction ? "No bonus action left." : "No action left.";
        boolean seen = false;
        for (Unit o : units) if (o.active() && !o.side.equals(u.side) && dist(o, u) <= 1) seen = true;
        if (seen) return "Can't hide while an enemy is right next to you.";
        if (bonusAction) u.bonus = false; else u.action = false;
        Dice.Roll r = Dice.d20(u.hero.skillBonus(Skill.STEALTH), false, false, u.hero.hasTrait("lucky"));
        game.rollFx(r, u.name + " — Stealth (Hide, DC 15)", u.hero.player, false);
        if (r.total >= 15) { u.add("hidden", 3); game.log(u.name + " slips out of sight. (Advantage on the next attack)", "good"); }
        else game.log(u.name + " fails to hide.", "info");
        return null;
    }

    String doHelp(Unit u, Unit t) {
        if (!u.action) return "No action left.";
        if (t == null) return "Pick a target.";
        if (t.side.equals(u.side)) {
            if (t.has("asleep")) { u.action = false; t.remove("asleep"); t.remove("prone"); game.log(u.name + " shakes " + t.name + " awake!", "good"); return null; }
            if (t.down() && !t.hero.stable && dist(u, t) <= 1) {
                u.action = false;
                Dice.Roll r = Dice.d20(u.hero.skillBonus(Skill.MEDICINE), false, false, u.hero.hasTrait("lucky"));
                game.rollFx(r, u.name + " — Medicine to stabilize " + t.name + " (DC 10)", u.hero.player, false);
                if (r.total >= 10) { t.hero.stable = true; game.log(t.name + " is stabilized.", "good"); } else game.log("The bleeding won't stop…", "bad");
                return null;
            }
            return "Help an ally by targeting an enemy next to you, or a dying/sleeping ally.";
        }
        if (dist(u, t) > 1) return "You must be next to the enemy to Help.";
        u.action = false;
        for (Unit o : units) if (o != u && o.side.equals(u.side) && o.active()) o.helpedVs = t.id;
        game.log(u.name + " distracts " + t.name + " — the next ally attack has Advantage!", "info");
        return null;
    }

    String doItem(Unit u, Map<String, Object> req) {
        String item = Json.str(req, "item", "");
        Hero hr = u.hero;
        if (!hr.inventory.contains(item)) return "You don't have that.";
        Items.Item it = Items.item(item);
        if (it == null) return "Unknown item.";
        switch (it.kind) {
            case "potion" -> {
                Unit t = byId(Json.str(req, "target", u.id));
                if (t == null) t = u;
                boolean self = t == u;
                if (!self && dist(u, t) > 1) return "You must be next to an ally to feed them a potion.";
                if (self) { if (!u.bonus) return "Drinking a potion takes a Bonus Action."; u.bonus = false; }
                else { if (!u.action) return "Giving a potion takes an Action."; u.action = false; }
                hr.inventory.remove(item);
                if (it.use.isEmpty()) { game.log(u.name + " drinks " + it.name + ".", "info"); return null; }
                Dice.Roll r = Dice.roll(it.use);
                game.rollFx(r, it.name, hr.player, false);
                heal(t, r.total, it.name);
                if (t.isHero()) { t.hero.stable = false; }
                return null;
            }
            case "bomb" -> {
                if (!u.action) return "Throwing takes an Action.";
                Unit t = byId(Json.str(req, "target", ""));
                if (t == null || t.side.equals(u.side)) return "Pick an enemy within 20 ft.";
                if (feet(u, t) > 20) return "Too far to throw (20 ft).";
                if (!los(u, t)) return "No line of sight.";
                u.action = false;
                hr.inventory.remove(item);
                if (item.equals("frost_flask")) {
                    List<int[]> cells = sphereCells(t.x, t.y, 1);
                    game.fx(Json.obj("t", "area", "shape", "sphere", "cells", cellsJson(cells), "dtype", "cold", "from", u.id, "name", it.name));
                    game.log(u.name + " hurls a " + it.name + "!", "spell");
                    for (Unit v : unitsIn(cells, null)) {
                        if (v.side.equals(u.side)) continue;
                        Dice.Roll s = Dice.d20(v.saveBonus(Ability.DEX), false, false, false);
                        int d = Dice.roll(it.use).total;
                        if (s.total >= 13) d /= 2; else v.add("slowed", 2);
                        damage(v, d, "cold", u, false, it.name);
                    }
                } else {
                    Hit r = attackRoll(u, t, hr.prof() + hr.mod(Ability.DEX), false, true, 20, 60, it.name);
                    game.fx(Json.obj("t", "attack", "from", u.id, "to", t.id, "hit", r.hit(), "kind", "ranged", "dtype", it.damageType));
                    if (r.hit()) {
                        int d = Dice.damage(it.use, r.crit()).total;
                        game.log(u.name + "'s " + it.name + " bursts over " + t.name + " for " + d + " " + it.damageType + "!", "hit");
                        damage(t, d, it.damageType, u, r.crit(), it.name);
                        if (t.alive() && it.damageType.equals("fire") && !t.immune().contains("fire")) t.add("burning", 99);
                    } else game.log("The flask shatters harmlessly.", "miss");
                }
                return null;
            }
            default -> { return it.name + " can't be used in combat."; }
        }
    }

    String doFeature(Unit u, Map<String, Object> req) {
        Hero hr = u.hero;
        String id = Json.str(req, "id", "");
        Unit t = byId(Json.str(req, "target", ""));
        switch (id) {
            case "second_wind" -> {
                if (!u.bonus) return "No bonus action left.";
                if (!hr.spend("second_wind")) return "No uses left.";
                u.bonus = false;
                Dice.Roll r = Dice.roll("1d10+" + hr.level);
                game.rollFx(r, u.name + " — Second Wind", hr.player, false);
                heal(u, r.total, "Second Wind");
            }
            case "action_surge" -> {
                if (u.action) return "You still have your action — use it first.";
                if (!hr.spend("action_surge")) return "No uses left.";
                u.action = true;
                game.log("⚡ " + u.name + " surges with ACTION SURGE — another action!", "spell");
            }
            case "rage" -> {
                if (!u.bonus) return "No bonus action left.";
                if (u.has("raging")) return "Already raging.";
                if (!hr.spend("rage")) return "No rages left.";
                u.bonus = false;
                u.add("raging", 99);
                hr.resources.put("raging_now", 1);
                game.fx(Json.obj("t", "spell", "spell", "rage", "from", u.id, "to", u.id, "dtype", "fire"));
                game.log("🔥 " + u.name + " flies into a RAGE! (+2 damage, resist physical damage)", "spell");
            }
            case "reckless" -> {
                if (u.reckless) return "Already reckless.";
                u.reckless = true;
                game.log(u.name + " attacks recklessly! (Advantage, but enemies get Advantage too)", "warn");
            }
            case "cunning_dash" -> { if (!u.bonus) return "No bonus action left."; u.bonus = false; u.move += u.speed(); game.log(u.name + " darts away (Cunning Action: Dash).", "info"); }
            case "cunning_disengage" -> { if (!u.bonus) return "No bonus action left."; u.bonus = false; u.disengaged = true; game.log(u.name + " slips free (Cunning Action: Disengage).", "info"); }
            case "cunning_hide" -> { return doHide(u, true); }
            case "steady_aim" -> {
                if (!u.bonus) return "No bonus action left.";
                if (u.moved) return "Steady Aim requires you not to have moved this turn.";
                u.bonus = false; u.move = 0; u.add("steady_aim", 1);
                game.log(u.name + " takes Steady Aim.", "info");
            }
            case "bardic_inspiration" -> {
                if (!u.bonus) return "No bonus action left.";
                if (t == null || !t.side.equals(u.side) || t == u) return "Pick an ally.";
                if (feet(u, t) > 60) return "Out of range (60 ft).";
                if (!hr.spend("bardic_inspiration")) return "No inspiration left.";
                u.bonus = false;
                t.bardDie = 6;
                game.fx(Json.obj("t", "spell", "spell", "inspire", "from", u.id, "to", t.id, "dtype", "psychic"));
                game.log("🎵 " + u.name + " inspires " + t.name + " (d6)!", "spell");
            }
            case "lay_on_hands" -> {
                if (!u.bonus) return "No bonus action left.";
                if (t == null || !t.side.equals(u.side)) t = u;
                if (dist(u, t) > 1) return "Touch range.";
                int pool = hr.res("lay_on_hands");
                if (pool <= 0) return "Your pool is empty.";
                int need = Math.max(1, t.maxHp() - Math.max(0, t.hp()));
                int amt = Math.min(pool, need);
                hr.resources.put("lay_on_hands", pool - amt);
                u.bonus = false;
                heal(t, amt, "Lay on Hands");
            }
            case "divine_spark" -> {
                if (!u.action) return "No action left.";
                if (t == null) return "Pick a target.";
                if (feet(u, t) > 30) return "Out of range (30 ft).";
                if (!hr.spend("channel_divinity")) return "No Channel Divinity left.";
                u.action = false;
                Dice.Roll r = Dice.roll("1d8+" + Math.max(0, hr.mod(Ability.WIS)));
                if (t.side.equals(u.side)) { heal(t, r.total, "Divine Spark"); }
                else {
                    Dice.Roll s = Dice.d20(t.saveBonus(Ability.CON), false, false, false);
                    int d = s.total >= hr.spellDC() ? r.total / 2 : r.total;
                    game.fx(Json.obj("t", "spell", "spell", "sacred_flame", "from", u.id, "to", t.id, "dtype", "radiant"));
                    game.log("Divine Spark sears " + t.name + " for " + d + " radiant.", "spell");
                    damage(t, d, "radiant", u, false, "Divine Spark");
                }
            }
            case "preserve_life" -> {
                if (!u.action) return "No action left.";
                if (!hr.spend("channel_divinity")) return "No Channel Divinity left.";
                u.action = false;
                int pool = 5 * hr.level;
                game.log("✦ " + u.name + " channels Preserve Life!", "spell");
                for (Unit o : units) {
                    if (pool <= 0) break;
                    if (!o.side.equals(u.side) || !o.alive() || feet(u, o) > 30) continue;
                    int cap = o.maxHp() / 2 - Math.max(0, o.hp());
                    if (cap <= 0) continue;
                    int amt = Math.min(cap, pool);
                    pool -= amt;
                    heal(o, amt, "Preserve Life");
                }
            }
            case "breath_weapon" -> {
                if (!u.action) return "No action left.";
                if (hr.res("breath_weapon") <= 0) return "No breath left.";
                hr.spend("breath_weapon");
                u.action = false;
                Species.Lineage lin = hr.species().lineage(hr.lineageId);
                String dt = lin == null || lin.damageType.isEmpty() ? "fire" : lin.damageType;
                int tx = Json.integer(req, "x", u.x + 1), ty = Json.integer(req, "y", u.y);
                List<int[]> cells = coneCells(u, tx, ty, 3);
                int dc = 8 + hr.mod(Ability.CON) + hr.prof();
                game.fx(Json.obj("t", "area", "shape", "cone", "cells", cellsJson(cells), "dtype", dt, "from", u.id, "name", "Breath Weapon"));
                game.log("🐲 " + u.name + " exhales a blast of " + dt + "!", "spell");
                for (Unit v : unitsIn(cells, u)) {
                    if (v.side.equals(u.side)) continue;
                    Dice.Roll s = Dice.d20(v.saveBonus(Ability.DEX), false, false, false);
                    int d = Dice.roll(hr.level >= 5 ? "2d10" : "1d10").total;
                    if (s.total >= dc) d /= 2;
                    damage(v, d, dt, u, false, "Breath Weapon");
                }
            }
            case "healing_hands" -> {
                if (!u.action) return "No action left.";
                if (t == null || !t.side.equals(u.side)) t = u;
                if (dist(u, t) > 1) return "Touch range.";
                if (!hr.spend("healing_hands")) return "Used already.";
                u.action = false;
                heal(t, Dice.roll(hr.prof() + "d4").total, "Healing Hands");
            }
            case "adrenaline_rush" -> {
                if (!u.bonus) return "No bonus action left.";
                if (!hr.spend("adrenaline_rush")) return "No uses left.";
                u.bonus = false; u.move += u.speed(); hr.tempHp = Math.max(hr.tempHp, hr.prof());
                game.log(u.name + " surges forward with Adrenaline Rush!", "info");
            }
            case "spiritual_weapon_strike" -> {
                if (!u.bonus) return "No bonus action left.";
                if (!u.has("spiritual_weapon")) return "No spiritual weapon.";
                if (t == null || t.side.equals(u.side)) return "Pick an enemy within 60 ft.";
                if (feet(u, t) > 60) return "Out of range.";
                u.bonus = false;
                spellAttack(u, t, Spells.get("spiritual_weapon"), "1d8", "force", 0, "Spiritual Weapon", true);
            }
            case "sphere_ram" -> {
                if (!u.bonus) return "No bonus action left.";
                if (!u.has("flaming_sphere")) return "No flaming sphere.";
                u.bonus = false;
                int tx = Json.integer(req, "x", u.x), ty = Json.integer(req, "y", u.y);
                areaSave(u, Spells.get("flaming_sphere"), sphereCells(tx, ty, 1), "2d6", 2);
            }
            case "sacred_weapon" -> {
                if (!u.action) return "No action left.";
                if (!hr.spend("channel_divinity")) return "No Channel Divinity left.";
                u.action = false; u.add("sacred_weapon", 99);
                game.log("✦ " + u.name + "'s weapon blazes with holy light (Sacred Weapon: +" + Math.max(1, hr.mod(Ability.CHA)) + " to hit).", "spell");
            }
            case "free_mark" -> { // Ranger favored enemy: Hunter's Mark without a slot
                Map<String, Object> r2 = new LinkedHashMap<>(req);
                r2.put("spell", "hunters_mark");
                r2.put("free", "free_mark");
                return doCast(u, r2);
            }
            default -> { return "Unknown feature."; }
        }
        return null;
    }

    // ===================================================================================== spells
    String doCast(Unit u, Map<String, Object> req) {
        Hero hr = u.hero;
        Spells.SpellDef s = Spells.get(Json.str(req, "spell", ""));
        if (s == null) return "Unknown spell.";
        if (!hr.canCast(s.id)) return hr.name + " doesn't have " + s.name + " prepared.";
        String free = Json.str(req, "free", "");
        if (s.id.equals("magic_missile") && hr.res("free_magic_missile") > 0 && !hr.canCast("magic_missile")) free = "free_magic_missile";
        if (s.id.equals("healing_word") && hr.res("free_healing_word") > 0 && !hr.canCast("healing_word")) free = "free_healing_word";
        int lvl = 0;
        if (!s.isCantrip()) {
            if (!free.isEmpty() && hr.res(free) > 0) lvl = s.level;
            else {
                lvl = Json.integer(req, "level", s.level);
                if (lvl < s.level) lvl = s.level;
                if (lvl > hr.slots.length || hr.slots[lvl - 1] <= 0) {
                    int alt = hr.lowestSlot(s.level);
                    if (alt == 0) return "No spell slots left for " + s.name + ".";
                    lvl = alt;
                }
                if (u.slotUsed) return "You've already cast a spell with a slot this turn.";
                free = "";
            }
        }
        if (s.cast == Spells.Cast.REACTION) return s.name + " is cast automatically as a reaction when you're hit (toggle Auto-Shield).";
        if (s.cast == Spells.Cast.BONUS && !u.bonus) return "No bonus action left.";
        if (s.cast == Spells.Cast.ACTION && !u.action) return "No action left.";
        // targets
        List<Unit> targets = new ArrayList<>();
        for (String tid : Json.strings(req, "targets")) { Unit t = byId(tid); if (t != null && t.alive()) targets.add(t); }
        int px = Json.integer(req, "x", -1), py = Json.integer(req, "y", -1);
        String element = Json.str(req, "element", "fire");
        if (!Set.of("acid", "cold", "fire", "lightning", "poison", "thunder").contains(element)) element = "fire";
        String err = validateTargets(u, s, targets, px, py, lvl);
        if (err != null) return err;
        // pay
        if (!s.isCantrip()) {
            if (!free.isEmpty()) hr.spend(free);
            else { hr.spendSlot(lvl); u.slotUsed = true; }
        }
        if (s.cast == Spells.Cast.BONUS) u.bonus = false; else u.action = false;
        if (s.concentration) { if (!u.conc.isEmpty()) dropConcentration(u); u.conc = s.id; }
        game.log("✦ " + u.name + " casts " + s.name + (lvl > s.level ? " at level " + lvl : "") + "!", "spell");
        resolveSpell(u, s, lvl, targets, px, py, element);
        return null;
    }

    String validateTargets(Unit u, Spells.SpellDef s, List<Unit> targets, int px, int py, int lvl) {
        switch (s.area) {
            case SINGLE, MULTI -> {
                if (s.kind == Spells.Kind.UTILITY && s.id.equals("misty_step")) {
                    if (px < 0) return "Pick a square to teleport to.";
                    if (Math.max(Math.abs(px - u.x), Math.abs(py - u.y)) * 5 > 30) return "Too far (30 ft).";
                    if (!canStand(u, px, py, u.size, false)) return "That square is blocked.";
                    if (!losToCell(u, px, py)) return "You must see the destination.";
                    return null;
                }
                if (s.kind == Spells.Kind.BUFF && (s.id.equals("mage_armor")) ) return null;
                if (targets.isEmpty()) return "Pick a target.";
                int max = s.area == Spells.Area.MULTI ? s.targets + Math.max(0, lvl - s.level) * s.upcastTargets : 1;
                if (s.kind == Spells.Kind.AUTO || s.id.equals("scorching_ray")) max = s.targets + Math.max(0, lvl - s.level) * Math.max(1, s.upcastTargets);
                if (targets.size() > max) return "Too many targets (max " + max + ").";
                for (Unit t : targets) {
                    if (feet(u, t) > Math.max(5, s.range)) return t.name + " is out of range (" + s.rangeLabel() + ").";
                    if (s.range > 5 && !los(u, t)) return "No line of sight to " + t.name + ".";
                    boolean ally = t.side.equals(u.side);
                    if (s.targetsAllies && !ally) return s.name + " targets allies.";
                    if (s.kind == Spells.Kind.HEAL && !ally) return "Heal your allies, not your foes!";
                    if ((s.kind == Spells.Kind.ATTACK || s.kind == Spells.Kind.AUTO || s.kind == Spells.Kind.SAVE) && ally) return "That's an ally!";
                    if ((s.id.equals("charm_person") || s.id.equals("hold_person")) && !t.humanoid()) return s.name + " only affects humanoids.";
                    if (u.charmedBy.equals(t.id)) return "You're charmed by " + t.name + ".";
                }
                return null;
            }
            case CONE, CUBE -> { if (px < 0) return "Pick a direction."; return null; }
            case SPHERE -> {
                if (px < 0) return "Pick a point.";
                int d = Math.max(0, gap(u.x, u.y, u.size, px, py, 1)) * 5;
                if (d > s.range) return "Out of range (" + s.range + " ft).";
                if (!losToCell(u, px, py)) return "No line of sight to that point.";
                return null;
            }
            default -> { return null; }
        }
    }

    void resolveSpell(Unit u, Spells.SpellDef s, int lvl, List<Unit> targets, int px, int py, String element) {
        Hero hr = u.hero;
        int up = Math.max(0, lvl - s.level);
        String dice = s.isCantrip() ? Spells.scaledCantripDice(s, hr.level) : s.dice;
        if (up > 0 && !s.upcastDice.isEmpty()) dice = addDice(dice, s.upcastDice, up);
        switch (s.kind) {
            case ATTACK -> {
                if (s.id.equals("scorching_ray")) {
                    int rays = 3 + up;
                    for (int i = 0; i < rays; i++) {
                        Unit t = targets.get(i % targets.size());
                        if (!t.alive() || t.down()) { t = nextAliveFoe(u, t); if (t == null) break; }
                        spellAttack(u, t, s, "2d6", "fire", 0, "Scorching Ray " + (i + 1), false);
                    }
                    return;
                }
                if (s.id.equals("chromatic_orb")) {
                    Unit t = targets.get(0);
                    Hit r = attackRoll(u, t, hr.spellAttackFor(s.id), feet(u, t) <= 5, true, s.range, s.range, "spell: Chromatic Orb (" + element + ")");
                    game.fx(Json.obj("t", "spell", "spell", s.id, "from", u.id, "to", t.id, "hit", r.hit(), "dtype", element));
                    if (r.hit()) {
                        Dice.Roll d = Dice.damage(dice, r.crit());
                        game.log("A crackling orb of " + element + " hits " + t.name + " for " + d.total + "!", "hit");
                        damage(t, d.total, element, u, r.crit(), s.name);
                        Set<Integer> faces = new HashSet<>();
                        boolean dup = false;
                        for (int[] f : d.dice) if (!faces.add(f[1])) dup = true;
                        if (dup) {
                            Unit next = null;
                            for (Unit o : units) if (o != t && o.active() && !o.side.equals(u.side) && feet(o, t) <= 30 && (next == null || feet(o, t) < feet(next, t))) next = o;
                            if (next != null) {
                                game.log("Matching dice! The orb LEAPS to " + next.name + "!", "crit");
                                spellAttack(u, next, s, dice, element, 0, "Chromatic Orb (leap)", false);
                            }
                        }
                    } else game.log("The orb misses " + t.name + ".", "miss");
                    return;
                }
                if (s.id.equals("spiritual_weapon")) {
                    u.add("spiritual_weapon", 99);
                    spellAttack(u, targets.get(0), s, addDice("1d8", "1d8", up / 2), "force", 0, "Spiritual Weapon", true);
                    return;
                }
                Unit t = targets.get(0);
                spellAttack(u, t, s, dice, s.damageType, 0, s.name, false);
                if (s.id.equals("ice_knife")) {
                    List<int[]> cells = sphereCells(t.x, t.y, 1);
                    game.fx(Json.obj("t", "area", "shape", "sphere", "cells", cellsJson(cells), "dtype", "cold", "from", u.id, "name", "Ice Knife"));
                    String cold = addDice("2d6", "1d6", up);
                    for (Unit v : unitsIn(cells, null)) {
                        if (v == u || !v.alive()) continue;
                        Dice.Roll sv = Dice.d20(v.saveBonus(Ability.DEX), false, false, false);
                        game.rollFx(sv, v.name + " — Dexterity save vs Ice Knife burst (DC " + hr.spellDCFor(s.id) + ")", v.isHero() ? v.hero.player : "", false);
                        if (sv.total < hr.spellDCFor(s.id)) { int d = Dice.roll(cold).total; game.log("The ice shard explodes — " + v.name + " takes " + d + " cold.", "hit"); damage(v, d, "cold", u, false, "Ice Knife"); }
                    }
                }
            }
            case AUTO -> { // Magic Missile
                int darts = s.targets + up;
                game.log(darts + " glowing darts streak out!", "spell");
                for (int i = 0; i < darts; i++) {
                    Unit t = targets.get(i % targets.size());
                    if (!t.alive() || t.down()) { t = nextAliveFoe(u, t); if (t == null) break; }
                    int d = Dice.roll("1d4+1").total;
                    game.fx(Json.obj("t", "spell", "spell", s.id, "from", u.id, "to", t.id, "hit", true, "dtype", "force", "i", i));
                    damage(t, d, "force", u, false, s.name);
                }
            }
            case SAVE -> {
                List<Unit> victims = new ArrayList<>();
                List<int[]> cells = null;
                switch (s.area) {
                    case CONE -> cells = coneCells(u, px, py, s.areaSize / 5);
                    case CUBE -> cells = cubeCells(u, px, py, s.areaSize / 5);
                    case SPHERE -> cells = sphereCells(px, py, Math.max(0, s.areaSize / 5));
                    default -> victims.addAll(targets);
                }
                if (cells != null) {
                    game.fx(Json.obj("t", "area", "shape", s.area.name().toLowerCase(), "cells", cellsJson(cells), "dtype", s.damageType.isEmpty() ? "arcane" : s.damageType, "from", u.id, "name", s.name, "spell", s.id));
                    for (Unit v : unitsIn(cells, u)) {
                        // you choose who is affected by Sleep / Bless-like spells; damaging areas hit everyone (careful!)
                        if (s.dice.isEmpty() && v.side.equals(u.side)) continue;
                        victims.add(v);
                    }
                    if (s.id.equals("flaming_sphere")) u.add("flaming_sphere", 99);
                } else {
                    for (Unit v : victims) game.fx(Json.obj("t", "spell", "spell", s.id, "from", u.id, "to", v.id, "hit", true, "dtype", s.damageType.isEmpty() ? "psychic" : s.damageType));
                }
                for (Unit v : victims) saveSpell(u, s, v, dice);
            }
            case HEAL -> {
                Unit t = targets.get(0);
                Dice.Roll r = Dice.roll(dice);
                int amt = r.total + Math.max(0, hr.mod(hr.castingAbilityFor(s.id)));
                if (hr.hasFeature("disciple_of_life")) amt += 2 + lvl;
                game.rollFx(r, s.name, hr.player, false);
                game.fx(Json.obj("t", "spell", "spell", s.id, "from", u.id, "to", t.id, "hit", true, "dtype", "radiant"));
                heal(t, amt, s.name);
                if (t.isHero()) t.hero.stable = false;
            }
            case BUFF -> {
                switch (s.id) {
                    case "mage_armor" -> { hr.mageArmor = true; game.fx(Json.obj("t", "shield", "id", u.id)); game.log(u.name + " is wrapped in shimmering force (AC " + hr.ac() + ").", "spell"); }
                    case "bless" -> { for (Unit t : targets) { t.add("blessed", 10); addConc(u, s.id, t, "blessed"); game.fx(Json.obj("t", "spell", "spell", s.id, "from", u.id, "to", t.id, "dtype", "radiant")); } game.log("Blessed: " + names(targets) + ".", "spell"); }
                    case "shield_of_faith" -> { Unit t = targets.get(0); t.add("shield_of_faith", 99); addConc(u, s.id, t, "shield_of_faith"); game.fx(Json.obj("t", "shield", "id", t.id)); }
                    case "heroism" -> { Unit t = targets.get(0); t.add("heroism", 99); t.remove("frightened"); addConc(u, s.id, t, "heroism"); if (t.isHero()) t.hero.tempHp = Math.max(t.hero.tempHp, Math.max(1, hr.mod(Ability.CHA))); }
                    case "hunters_mark" -> {
                        Unit t = targets.get(0);
                        if (t.side.equals(u.side)) { game.log("Hunter's Mark needs an enemy.", "info"); return; }
                        t.markedBy = u.id; t.add("marked", 99); addConc(u, s.id, t, "marked");
                        game.fx(Json.obj("t", "spell", "spell", s.id, "from", u.id, "to", t.id, "dtype", "force"));
                        game.log(t.name + " is marked as " + u.name + "'s quarry.", "spell");
                    }
                    case "aid" -> { for (Unit t : targets) { if (t.isHero()) { t.hero.tempHp = Math.max(t.hero.tempHp, 5 + 5 * up); } game.fx(Json.obj("t", "heal", "id", t.id, "amount", 5)); } game.log("Aid bolsters " + names(targets) + ".", "spell"); }
                    case "divine_smite" -> { u.smiteArmed = true; game.log(u.name + " readies a smite.", "info"); }
                    default -> game.log(s.name + " takes effect.", "spell");
                }
            }
            case UTILITY -> {
                switch (s.id) {
                    case "misty_step" -> {
                        game.fx(Json.obj("t", "teleport", "id", u.id, "from", List.of(u.x, u.y), "to", List.of(px, py)));
                        u.x = px; u.y = py;
                        game.log(u.name + " vanishes in silver mist and reappears 30 ft away!", "spell");
                    }
                    case "lesser_restoration" -> {
                        Unit t = targets.isEmpty() ? u : targets.get(0);
                        for (String c : List.of("poisoned", "paralyzed", "blinded", "frightened")) t.remove(c);
                        game.log(t.name + " is cleansed.", "spell");
                    }
                    default -> game.log(s.name + " has little effect in the heat of battle.", "info");
                }
            }
        }
    }

    static String names(List<Unit> l) {
        List<String> n = new ArrayList<>();
        for (Unit u : l) n.add(u.name);
        return String.join(", ", n);
    }

    Unit nextAliveFoe(Unit u, Unit near) {
        Unit best = null;
        for (Unit o : units) if (o.active() && !o.side.equals(u.side) && !o.isObject() && feet(u, o) <= 120 && los(u, o) && (best == null || dist(o, near) < dist(best, near))) best = o;
        return best;
    }

    void spellAttack(Unit u, Unit t, Spells.SpellDef s, String dice, String type, int bonusDmg, String label, boolean addMod) {
        Hero hr = u.hero;
        boolean melee = s.range <= 5;
        Hit r = attackRoll(u, t, hr.spellAttackFor(s.id), melee, !melee, s.range, s.range, "spell: " + label);
        game.fx(Json.obj("t", "spell", "spell", s.id, "from", u.id, "to", t.id, "hit", r.hit(), "dtype", type));
        if (!r.hit()) {
            if (s.isCantrip() && hr.hasFeature("potent_cantrip")) {
                int d = Math.max(1, Dice.roll(dice).total / 2);
                game.log("Potent Cantrip: even missing, " + label + " singes " + t.name + " for " + d + ".", "info");
                damage(t, d, type, u, false, label);
            } else game.log(label + " misses " + t.name + ".", "miss");
            return;
        }
        int d = Dice.damage(dice, r.crit()).total + bonusDmg + (addMod ? Math.max(0, hr.mod(hr.castingAbilityFor(s.id))) : 0);
        game.log(label + " hits " + t.name + " for " + d + " " + type + ".", "hit");
        damage(t, d, type, u, r.crit(), label);
        if (t.alive() && !t.down() && !s.condition.isEmpty()) {
            switch (s.condition) {
                case "slowed" -> { t.add("slowed", 1); game.log(t.name + " is slowed by frost (-10 ft).", "info"); }
                case "no_reactions" -> { t.add("no_reactions", 1); game.log(t.name + " is jolted and can't take reactions.", "info"); }
                case "guided" -> { t.add("guided", 2); game.log(t.name + " glows — the next attack against it has Advantage.", "info"); }
                default -> t.add(s.condition, Math.max(1, s.conditionRounds));
            }
        }
        // Fire Bolt ignites unattended flammables: barrels/tables burst into flame
        if (type.equals("fire") && s.id.equals("fire_bolt")) { /* flavour handled by client */ }
    }

    void saveSpell(Unit u, Spells.SpellDef s, Unit v, String dice) {
        Hero hr = u.hero;
        int dc = hr.spellDCFor(s.id);
        if ((s.id.equals("sleep")) && (v.creatureType().equals("undead") || v.creatureType().equals("construct") || v.isObject() || v.condImmune("unconscious"))) { game.log(v.name + " is unaffected by Sleep.", "info"); return; }
        if (s.condition.equals("charmed") && v.condImmune("charmed")) { game.log(v.name + " is immune to being charmed!", "info"); return; }
        if (s.condition.equals("paralyzed") && v.condImmune("paralyzed")) { game.log(v.name + " can't be paralyzed.", "info"); return; }
        if (v.isObject() && s.dice.isEmpty()) return;
        boolean adv = false;
        if (s.id.equals("charm_person")) adv = true; // you're fighting it
        if (v.isHero() && s.condition.equals("charmed") && v.hero.hasTrait("fey_ancestry")) adv = true;
        if (v.isHero() && s.save == Ability.DEX && v.hero.hasFeature("danger_sense")) adv = true;
        if (v.has("dodging") && s.save == Ability.DEX) adv = true;
        if (v.def != null && v.def.traits.contains("magic_resistance")) adv = true;
        boolean autoFail = (v.has("paralyzed") || v.has("asleep") || v.has("unconscious")) && (s.save == Ability.DEX || s.save == Ability.STR);
        Dice.Roll r = Dice.d20(v.saveBonus(s.save), adv, false, v.isHero() && v.hero.hasTrait("lucky"));
        game.rollFx(r, v.name + " — " + s.save.full + " save vs " + s.name + " (DC " + dc + ")", v.isHero() ? v.hero.player : "", false);
        boolean ok = !autoFail && r.total >= dc;
        if (!ok && v.legRes > 0 && v.isBoss() && (!s.condition.isEmpty() || s.dice.length() > 0 && Dice.average(dice) >= 10)) {
            v.legRes--;
            ok = true;
            game.log("⟡ " + v.name + " uses LEGENDARY RESISTANCE to shrug it off! (" + v.legRes + " left)", "boss");
        }
        int d = 0;
        if (!dice.isEmpty()) {
            String dd = dice;
            if (s.id.equals("toll_the_dead") && v.hp() < v.maxHp()) dd = dice.replace("d8", "d12");
            d = Dice.roll(dd).total;
            if (ok) d = s.halfOnSave ? d / 2 : (s.isCantrip() && hr.hasFeature("potent_cantrip") ? d / 2 : 0);
        }
        if (d > 0) { game.log(v.name + (ok ? " resists partially: " : " takes ") + d + " " + s.damageType + ".", ok ? "info" : "hit"); damage(v, d, s.damageType, u, false, s.name); }
        else if (ok) game.log(v.name + " resists " + s.name + ".", "miss");
        if (!ok && v.alive() && !s.condition.isEmpty()) {
            switch (s.condition) {
                case "pushed" -> push(u, v, 2);
                case "charmed" -> { v.add("charmed", 10); v.charmedBy = u.id; addConc(u, s.id, v, "charmed"); game.log("💗 " + v.name + " is CHARMED by " + u.name + " and won't attack her!", "spell"); }
                case "incapacitated" -> { v.add("asleep", 10); v.add("prone", 99); addConc(u, s.id, v, "asleep"); game.log("💤 " + v.name + " slumps to the ground, fast asleep.", "spell"); }
                case "paralyzed" -> { v.add("paralyzed", 99); addConc(u, s.id, v, "paralyzed"); game.log("⛓ " + v.name + " is held rigid — Paralyzed!", "spell"); }
                case "outlined" -> { v.add("outlined", 10); addConc(u, s.id, v, "outlined"); game.log(v.name + " is outlined in violet faerie fire.", "spell"); }
                case "frightened" -> { v.add("frightened", 1); game.log(v.name + " recoils in terror.", "spell"); }
                case "sapped" -> { v.add("sapped", 2); }
                default -> v.add(s.condition, Math.max(1, s.conditionRounds));
            }
        }
    }

    void areaSave(Unit u, Spells.SpellDef s, List<int[]> cells, String dice, int lvl) {
        game.fx(Json.obj("t", "area", "shape", "sphere", "cells", cellsJson(cells), "dtype", s.damageType, "from", u.id, "name", s.name, "spell", s.id));
        for (Unit v : unitsIn(cells, u)) saveSpell(u, s, v, dice);
    }

    static String addDice(String base, String per, int times) {
        if (times <= 0 || per.isEmpty()) return base;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d*)d(\\d+)").matcher(per);
        if (!m.find()) return base;
        int n = (m.group(1).isEmpty() ? 1 : Integer.parseInt(m.group(1))) * times;
        return base + "+" + n + "d" + m.group(2);
    }

    // ===================================================================================== AI
    void runAi(Unit u) {
        synchronized (game) {
            if (over || current() != u) return;
            try {
                if (u.isHero()) heroAi(u); else monsterAi(u);
            } catch (Exception e) {
                game.log("(" + u.name + " hesitates — " + e.getClass().getSimpleName() + ")", "info");
                e.printStackTrace();
            }
            checkEnd();
            game.changed();
            if (!over) game.schedule(() -> advanceFrom(u), 900);
        }
    }

    List<Unit> foesOf(Unit u) {
        List<Unit> l = new ArrayList<>();
        for (Unit o : units) if (o.active() && !o.side.equals(u.side) && !(o.isObject() && u.side.equals("foe"))) l.add(o);
        return l;
    }

    int meleeReachSquares(Unit u) { int r = meleeReach(u); return Math.max(1, r / 5); }

    /** Move toward a target until within `reachSquares`. Returns true if in reach afterwards. */
    boolean approach(Unit u, Unit t, int reachSquares) {
        if (dist(u, t) <= reachSquares) return true;
        if (u.move <= 0) return false;
        Map<Integer, Integer> prev = new HashMap<>();
        Map<Integer, Integer> cost = paths(u, prev, true);
        int bestKey = -1, bestCost = Integer.MAX_VALUE;
        for (var e : cost.entrySet()) {
            int x = e.getKey() % w, y = e.getKey() / w;
            if (gap(x, y, u.size, t.x, t.y, t.size) <= reachSquares && canStand(u, x, y, u.size, false) && e.getValue() < bestCost) { bestCost = e.getValue(); bestKey = e.getKey(); }
        }
        if (bestKey < 0) {
            // no route: get as close as possible
            int bestD = dist(u, t);
            for (var e : cost.entrySet()) {
                int x = e.getKey() % w, y = e.getKey() / w;
                int d = gap(x, y, u.size, t.x, t.y, t.size);
                if (e.getValue() <= u.move && canStand(u, x, y, u.size, false) && d < bestD) { bestD = d; bestKey = e.getKey(); }
            }
            if (bestKey < 0) return false;
        }
        List<int[]> path = pathTo(u, bestKey % w, bestKey / w, true);
        if (path == null) return false;
        // truncate at movement budget and at a standable square
        List<int[]> cut = new ArrayList<>();
        int budget = u.move - (u.has("prone") ? u.speed() / 2 : 0);
        for (int[] p : path) { if (p[2] > budget) break; cut.add(p); }
        while (!cut.isEmpty() && !canStand(u, cut.get(cut.size() - 1)[0], cut.get(cut.size() - 1)[1], u.size, false)) cut.remove(cut.size() - 1);
        walk(u, cut);
        return u.active() && dist(u, t) <= reachSquares;
    }

    /** Move to a square within range & sight of the target but not adjacent to any enemy. */
    void kite(Unit u, Unit t, int rangeSquares) {
        Map<Integer, Integer> cost = paths(u, null, true);
        int bestKey = -1;
        double bestScore = -1e9;
        for (var e : cost.entrySet()) {
            if (e.getValue() > u.move) continue;
            int x = e.getKey() % w, y = e.getKey() / w;
            if (!canStand(u, x, y, u.size, false) && !(x == u.x && y == u.y)) continue;
            int d = gap(x, y, u.size, t.x, t.y, t.size);
            if (d > rangeSquares) continue;
            if (!los(x + 0.5, y + 0.5, t.x + t.size / 2.0, t.y + t.size / 2.0, null, t)) continue;
            int nearest = 99;
            for (Unit f : foesOf(u)) nearest = Math.min(nearest, gap(x, y, u.size, f.x, f.y, f.size));
            double score = Math.min(nearest, 6) * 3 - e.getValue() / 10.0 + (tile(x, y) == 'B' ? 0 : 0) - (tile(x, y) == 'L' ? 50 : 0);
            if (score > bestScore) { bestScore = score; bestKey = e.getKey(); }
        }
        if (bestKey >= 0 && bestKey != u.y * w + u.x) {
            boolean adj = false;
            for (Unit f : foesOf(u)) if (dist(u, f) <= 1 && !f.isObject()) adj = true;
            if (adj && u.def != null && u.def.traits.contains("nimble_escape")) { u.disengaged = true; u.bonus = false; game.log(u.name + " slips away nimbly.", "info"); }
            walk(u, pathTo(u, bestKey % w, bestKey / w, true));
        }
    }

    Unit pickTarget(Unit u, boolean ranged) {
        Unit best = null;
        double bestScore = -1e9;
        for (Unit t : foesOf(u)) {
            if (t.down() && u.side.equals("foe")) continue;      // monsters don't finish the dying (usually)
            if (u.charmedBy.equals(t.id)) continue;
            double s = -dist(u, t) * 2.0;
            s += (1.0 - t.hp() / (double) Math.max(1, t.maxHp())) * 6;
            s -= t.ac() * 0.3;
            if (!t.conc.isEmpty()) s += 3;
            if (t.isObject()) {
                // the party should smash whatever is empowering the boss
                boolean empowering = units.stream().anyMatch(b -> b.isBoss() && b.active() && b.side.equals(t.side) && b.def.traits.contains("flame_aura"));
                s += u.side.equals("party") ? (empowering ? 7 : -4) : -99;
            }
            if (ranged && !los(u, t)) s -= 10;
            if (u.def != null && u.def.traits.contains("pack_tactics")) for (Unit o : units) if (o != u && o.active() && o.side.equals(u.side) && dist(o, t) <= 1) s += 3;
            if (s > bestScore) { bestScore = s; best = t; }
        }
        return best;
    }

    void monsterAi(Unit u) {
        Monsters.MonsterDef d = u.def;
        if (u.isObject()) return;
        List<Unit> foes = foesOf(u);
        foes.removeIf(f -> u.charmedBy.equals(f.id));
        if (foes.isEmpty()) {
            if (!u.charmedBy.isEmpty()) game.log(u.name + " gazes adoringly at " + byId(u.charmedBy).name + " and does nothing hostile.", "spell");
            return;
        }
        // morale: badly hurt goblins/brawlers may flee
        if ((d.type.equals("humanoid") || d.type.equals("beast")) && !u.isBoss() && u.hp() <= u.maxHp() / 4 && Dice.chance() < 0.2 && d.cr < 1) {
            game.log(u.name + " panics and flees the fight!", "good");
            u.gone = true;
            xpPool += u.xp() / 2;
            game.fx(Json.obj("t", "flee", "id", u.id));
            return;
        }
        // 1) bonus-action specials (healing)
        for (Monsters.Special s : d.specials) {
            if (!s.shape.equals("heal") || (s.uses > 0 && u.uses.getOrDefault(s.id, 0) <= 0)) continue;
            boolean need = false;
            for (Unit o : units) if (o.side.equals(u.side) && o.active() && !o.isObject() && o.hp() < o.maxHp() / 2) need = true;
            if (need) { useSpecial(u, s, u.x, u.y); break; }
        }
        // 2) area specials when they catch 2+ foes (or the boss when it can)
        for (Monsters.Special s : d.specials) {
            if (s.shape.equals("heal") || !u.ready.getOrDefault(s.id, true)) continue;
            if (s.uses > 0 && u.uses.getOrDefault(s.id, 0) <= 0) continue;
            if (s.id.equals("wing_gust") && u.isBoss()) continue; // saved for legendary actions
            if (s.shape.equals("summon")) {
                long allies = units.stream().filter(o -> o.side.equals(u.side) && o.active() && !o.isObject()).count();
                if (allies < 3) { useSpecial(u, s, u.x, u.y); return; }
                continue;
            }
            int[] best = bestAreaAim(u, s);
            if (best != null && best[2] >= (u.isBoss() ? 1 : 2)) {
                if (s.shape.equals("cone") || s.range == 0) {
                    // get close enough first
                    Unit near = pickTarget(u, false);
                    if (near != null && dist(u, near) > s.size / 5) { approach(u, near, Math.max(1, s.size / 5 - 1)); best = bestAreaAim(u, s); }
                }
                if (best != null && best[2] >= 1) { useSpecial(u, s, best[0], best[1]); return; }
            }
        }
        // 3) attacks
        Monsters.Attack melee = null, ranged = null;
        for (Monsters.Attack a : d.attacks) { if (a.name.equals("None")) continue; if (a.isRanged()) { if (ranged == null) ranged = a; } else if (melee == null) melee = a; }
        boolean preferRanged = ranged != null && (d.role.equals("ranged") || d.role.equals("caster") || d.traits.contains("avoid_melee") || melee == null);
        Unit t = pickTarget(u, preferRanged);
        if (t == null) return;
        if (preferRanged) {
            int rs = ranged.range / 5;
            boolean adjacent = false;
            for (Unit f : foes) if (!f.isObject() && dist(u, f) <= 1) adjacent = true;
            if ((adjacent && melee == null) || (adjacent && d.traits.contains("avoid_melee")) || dist(u, t) > rs || !los(u, t)) kite(u, t, rs);
            else if (adjacent && melee != null) { /* fight in melee instead */ }
            Unit tt = pickTarget(u, true);
            if (tt == null) return;
            boolean adjNow = dist(u, tt) <= meleeReachSquares(u);
            for (int i = 0; i < d.multiattack; i++) {
                if (!tt.alive() || (tt.down() && u.side.equals("foe"))) { tt = pickTarget(u, true); if (tt == null) break; }
                if (adjNow && melee != null && dist(u, tt) <= melee.reach / 5) monsterAttack(u, tt, melee);
                else if (dist(u, tt) <= ranged.longRange / 5 && los(u, tt)) monsterAttack(u, tt, ranged);
                if (!u.active()) return;
            }
            return;
        }
        boolean inReach = approach(u, t, Math.max(1, melee.reach / 5));
        if (!u.active()) return;
        if (!inReach) {
            // try another target already in reach
            for (Unit o : foesOf(u)) if (dist(u, o) <= melee.reach / 5 && !(o.down() && u.side.equals("foe")) && !u.charmedBy.equals(o.id)) { t = o; inReach = true; break; }
        }
        if (!inReach && ranged != null) {
            Unit rt = pickTarget(u, true);
            if (rt != null && dist(u, rt) <= ranged.longRange / 5 && los(u, rt)) { monsterAttack(u, rt, ranged); return; }
        }
        if (!inReach) { u.move += u.speed(); approach(u, t, Math.max(1, melee.reach / 5)); return; } // Dash
        int idx = 0;
        List<Monsters.Attack> meleeList = new ArrayList<>();
        for (Monsters.Attack a : d.attacks) if (!a.isRanged() && !a.name.equals("None")) meleeList.add(a);
        for (int i = 0; i < d.multiattack; i++) {
            if (!t.alive() || (t.down() && u.side.equals("foe"))) {
                t = null;
                for (Unit o : foesOf(u)) if (dist(u, o) <= melee.reach / 5 && !o.down() && !u.charmedBy.equals(o.id)) { t = o; break; }
                if (t == null) break;
            }
            Monsters.Attack a = meleeList.get(idx++ % meleeList.size());
            if (dist(u, t) > a.reach / 5) a = melee;
            monsterAttack(u, t, a);
            if (!u.active()) return;
        }
        // nimble escape: goblins hit and fade back
        if (d.traits.contains("nimble_escape") && u.move > 0 && ranged != null && t != null && t.alive()) kite(u, t, ranged.range / 5);
    }

    /** Best aim for an area special: {x, y, score}. Score = foes hit - 2*allies hit. */
    int[] bestAreaAim(Unit u, Monsters.Special s) {
        int[] best = null;
        List<int[]> aims = new ArrayList<>();
        for (Unit f : foesOf(u)) if (!f.isObject()) aims.add(new int[]{f.x, f.y});
        if (s.range == 0 && !s.shape.equals("cone")) aims = List.of(new int[]{u.x, u.y});
        for (int[] a : aims) {
            List<int[]> cells = switch (s.shape) {
                case "cone" -> coneCells(u, a[0], a[1], s.size / 5);
                case "sphere" -> s.range == 0 ? sphereAround(u, s.size / 5) : sphereCells(a[0], a[1], s.size / 5);
                case "line" -> lineCells(u, a[0], a[1], s.size / 5);
                default -> List.of(a);
            };
            if (s.range > 0 && gap(u.x, u.y, u.size, a[0], a[1], 1) * 5 > s.range) continue;
            int score = 0;
            for (Unit v : unitsIn(cells, u)) {
                if (v.isObject()) continue;
                if (v.side.equals(u.side)) score -= 2; else if (!v.down()) score += 1;
            }
            if (best == null || score > best[2]) best = new int[]{a[0], a[1], score};
        }
        return best;
    }

    /** Simple but competent hero AI — used for companions, absent players and the self-test. */
    void heroAi(Unit u) {
        Hero hr = u.hero;
        // 0. emergency healing
        Unit dying = null;
        for (Unit o : units) if (o.side.equals("party") && o.isHero() && o.alive() && o.down()) { dying = o; break; }
        if (dying != null) {
            if (hr.canCast("healing_word") && hr.lowestSlot(1) > 0 && feet(u, dying) <= 60 && u.bonus && !u.slotUsed) {
                if (doCast(u, Json.obj("spell", "healing_word", "level", hr.lowestSlot(1), "targets", List.of(dying.id))) == null) dying = null;
            }
            if (dying != null && hr.canCast("cure_wounds") && hr.lowestSlot(1) > 0 && !u.slotUsed) {
                if (approach(u, dying, 1) && doCast(u, Json.obj("spell", "cure_wounds", "level", hr.lowestSlot(1), "targets", List.of(dying.id))) == null) dying = null;
            }
            if (dying != null && hr.res("lay_on_hands") > 0 && u.bonus) {
                if (approach(u, dying, 1) && doFeature(u, Json.obj("id", "lay_on_hands", "target", dying.id)) == null) dying = null;
            }
            if (dying != null && hr.inventory.contains("potion_healing") && u.action) {
                if (approach(u, dying, 1)) doItem(u, Json.obj("item", "potion_healing", "target", dying.id));
            }
        }
        // 1. self-care
        if (u.hp() < u.maxHp() * 0.4) {
            if (hr.res("second_wind") > 0 && u.bonus) doFeature(u, Json.obj("id", "second_wind"));
            else if (hr.inventory.contains("potion_healing") && u.bonus) doItem(u, Json.obj("item", "potion_healing", "target", u.id));
            else if (hr.canCast("healing_word") && u.bonus && hr.lowestSlot(1) > 0 && !u.slotUsed) doCast(u, Json.obj("spell", "healing_word", "targets", List.of(u.id)));
        }
        // heal a badly hurt ally with healing word
        if (u.bonus && hr.canCast("healing_word") && hr.lowestSlot(1) > 0 && !u.slotUsed) {
            for (Unit o : units) if (o.side.equals("party") && o.active() && o.hp() < o.maxHp() * 0.3 && feet(u, o) <= 60) { doCast(u, Json.obj("spell", "healing_word", "targets", List.of(o.id))); break; }
        }
        if (hr.classId.equals("barbarian") && !u.has("raging") && hr.res("rage") > 0 && u.bonus) doFeature(u, Json.obj("id", "rage"));
        if (hr.classId.equals("cleric") && round == 1 && hr.canCast("bless") && hr.lowestSlot(1) > 0 && u.action && u.conc.isEmpty()) {
            List<String> ids = new ArrayList<>();
            for (Unit o : units) if (o.side.equals("party") && o.active() && feet(u, o) <= 30 && ids.size() < 3) ids.add(o.id);
            if (ids.size() >= 2) doCast(u, Json.obj("spell", "bless", "targets", ids));
        }
        if (hr.classId.equals("cleric") && u.bonus && !u.slotUsed && hr.canCast("spiritual_weapon") && hr.lowestSlot(2) > 0 && u.conc.isEmpty()) {
            Unit t = pickTarget(u, true);
            if (t != null && feet(u, t) <= 60) doCast(u, Json.obj("spell", "spiritual_weapon", "level", 2, "targets", List.of(t.id)));
        } else if (u.has("spiritual_weapon") && u.bonus) {
            Unit t = pickTarget(u, true);
            if (t != null && feet(u, t) <= 60) doFeature(u, Json.obj("id", "spiritual_weapon_strike", "target", t.id));
        }
        if (!u.action || foesOf(u).isEmpty()) return;
        // flasks: frost against fire creatures, alchemist's fire against everything else that isn't immune
        for (String flask : List.of("frost_flask", "alchemists_fire")) {
            if (!hr.inventory.contains(flask)) continue;
            Unit best = null;
            for (Unit f : foesOf(u)) {
                if (f.isObject() || feet(u, f) > 20 || !los(u, f)) continue;
                boolean good = flask.equals("frost_flask") ? f.vulnerable().contains("cold") || f.isBoss() : !f.immune().contains("fire") && f.hp() > 15;
                if (good && (best == null || f.hp() > best.hp())) best = f;
            }
            if (best != null && doItem(u, Json.obj("item", flask, "target", best.id)) == null) return;
        }
        Items.Weapon wp = hr.weapon();
        boolean caster = hr.classDef().caster() && (hr.classId.equals("wizard") || hr.classId.equals("cleric") || hr.classId.equals("bard") || hr.classId.equals("sorcerer"));
        Unit t = pickTarget(u, caster || wp.ranged);
        if (t == null) return;
        if (caster) {
            // area spell if 2+ foes clustered nearby
            for (String sid : List.of("burning_hands", "thunderwave", "shatter")) {
                if (!hr.canCast(sid) || hr.lowestSlot(Spells.get(sid).level) == 0 || u.slotUsed) continue;
                Spells.SpellDef s = Spells.get(sid);
                for (Unit f : foesOf(u)) {
                    List<int[]> cells = s.area == Spells.Area.CONE ? coneCells(u, f.x, f.y, s.areaSize / 5) : s.area == Spells.Area.CUBE ? cubeCells(u, f.x, f.y, s.areaSize / 5) : sphereCells(f.x, f.y, s.areaSize / 5);
                    if (s.area == Spells.Area.SPHERE && feet(u, f) > s.range) continue;
                    int foesHit = 0, alliesHit = 0;
                    for (Unit v : unitsIn(cells, u)) { if (v.side.equals(u.side)) alliesHit++; else if (!v.isObject()) foesHit++; }
                    if (foesHit >= 2 && alliesHit == 0) {
                        if (doCast(u, Json.obj("spell", sid, "x", f.x, "y", f.y, "level", hr.lowestSlot(s.level))) == null) return;
                    }
                }
            }
            // boss fight: spend slots on big single-target spells
            boolean bossFight = units.stream().anyMatch(o -> o.isBoss() && o.active());
            List<String> prefs = new ArrayList<>();
            if (bossFight || t.hp() > 15) prefs.addAll(List.of("scorching_ray", "chromatic_orb", "guiding_bolt", "magic_missile", "dissonant_whispers"));
            if (hr.classId.equals("cleric")) prefs.addAll(List.of("sacred_flame", "toll_the_dead"));
            prefs.addAll(List.of("ray_of_frost", "fire_bolt", "starry_wisp", "vicious_mockery", "sacred_flame", "toll_the_dead", "acid_splash", "shocking_grasp"));
            // braziers are immune to fire: use cold/lightning against objects
            for (String sid : prefs) {
                Spells.SpellDef s = Spells.get(sid);
                if (s == null || !hr.canCast(sid)) continue;
                if (!s.isCantrip() && (hr.lowestSlot(s.level) == 0 || u.slotUsed)) continue;
                if (s.cast == Spells.Cast.BONUS && !u.bonus) continue;
                Unit tt = t;
                if (s.damageType.equals("fire") && tt.immune().contains("fire") && !sid.equals("chromatic_orb")) continue;
                if (s.range <= 5) { if (dist(u, tt) > 1) continue; }
                else {
                    if (feet(u, tt) > s.range || !los(u, tt)) kite(u, tt, s.range / 5);
                    if (feet(u, tt) > s.range || !los(u, tt)) continue;
                }
                Map<String, Object> req = Json.obj("spell", sid, "targets", List.of(tt.id), "level", s.isCantrip() ? 0 : hr.lowestSlot(s.level));
                if (sid.equals("chromatic_orb")) req.put("element", tt.vulnerable().contains("cold") ? "cold" : tt.immune().contains("fire") ? "lightning" : "fire");
                if (sid.equals("ray_of_frost") || sid.equals("fire_bolt") || s.kind == Spells.Kind.ATTACK || s.kind == Spells.Kind.SAVE || s.kind == Spells.Kind.AUTO) {
                    // keep away from melee before blasting
                    boolean adj = false;
                    for (Unit f : foesOf(u)) if (dist(u, f) <= 1 && !f.isObject()) adj = true;
                    if (adj && s.range > 5 && u.move > 0) kite(u, tt, s.range / 5);
                    if (feet(u, tt) > s.range || !los(u, tt)) continue;
                    if (doCast(u, req) == null) return;
                }
            }
        }
        // weapon attack
        if (wp.ranged) {
            if (feet(u, t) > wp.range || !los(u, t)) kite(u, t, wp.range / 5);
            if (feet(u, t) <= wp.longRange && los(u, t)) doAttack(u, t);
        } else {
            if (approach(u, t, 1)) {
                if (hr.classId.equals("barbarian") && hr.hasFeature("reckless_attack")) u.reckless = true;
                doAttack(u, t);
                if (hr.res("action_surge") > 0 && t.active() && !u.action && hr.classId.equals("fighter")) { doFeature(u, Json.obj("id", "action_surge")); if (t.active()) doAttack(u, t); }
            } else if (wp.thrown && feet(u, t) <= wp.longRange && los(u, t)) doAttack(u, t);
            else if (u.action) { u.action = false; u.move += u.speed(); approach(u, t, 1); }
        }
        if (hr.classId.equals("rogue") && u.bonus && u.move > 0) {
            boolean adj = false;
            for (Unit f : foesOf(u)) if (dist(u, f) <= 1 && !f.isObject()) adj = true;
            if (adj && hr.hasFeature("cunning_action")) { doFeature(u, Json.obj("id", "cunning_disengage")); kite(u, t, wp.ranged ? wp.range / 5 : 6); }
        }
    }

    // ===================================================================================== client view
    Map<String, Object> view(String forPlayer) {
        List<Object> rows = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < w; x++) {
                char c = tiles[y][x];
                if (c == '^' && !revealedTraps.contains(y * w + x)) c = '.';
                sb.append(c);
            }
            rows.add(sb.toString());
        }
        List<Object> us = new ArrayList<>();
        for (Unit u : units) if (!(u.gone && u.side.equals("foe") && !u.isObject() && false)) us.add(u);
        List<Object> ord = new ArrayList<>();
        for (Unit u : order) ord.add(u.id);
        Unit cur = current();
        Map<String, Object> m = Json.obj("title", enc.title, "round", round, "w", w, "h", h, "tiles", rows, "units", us,
                "order", ord, "turn", cur == null ? null : cur.id, "over", over, "result", result, "objective", objectiveText,
                "warnings", cellsJson(warnings), "scorched", cellsJson(scorched), "backdrop", enc.backdrop, "music", enc.music,
                "nonlethal", enc.nonlethal);
        if (cur != null && cur.isHero() && !cur.ai) {
            m.put("turnInfo", Json.obj("action", cur.action, "bonus", cur.bonus, "reaction", cur.reaction, "move", cur.move,
                    "slotUsed", cur.slotUsed, "autoShield", cur.autoShield, "smiteArmed", cur.smiteArmed, "reckless", cur.reckless));
            m.put("reach", reachable(cur));
            m.put("options", options(cur));
        }
        return m;
    }

    Map<String, Object> options(Unit u) {
        Hero hr = u.hero;
        Items.Weapon wp = hr.weapon();
        List<Object> spells = new ArrayList<>();
        for (String id : hr.allSpells()) {
            Spells.SpellDef s = Spells.get(id);
            if (s == null) continue;
            String reason = "";
            if (s.cast == Spells.Cast.REACTION) reason = "Reaction (automatic)";
            else if (id.equals("divine_smite")) reason = "Use the Smite toggle";
            else if (s.kind == Spells.Kind.UTILITY && !Set.of("misty_step", "lesser_restoration").contains(id)) reason = "Not useful in combat";
            else if (s.cast == Spells.Cast.BONUS && !u.bonus) reason = "No bonus action";
            else if (s.cast == Spells.Cast.ACTION && !u.action) reason = "No action";
            else if (!s.isCantrip() && hr.lowestSlot(s.level) == 0 && !(id.equals("hunters_mark") && hr.res("free_mark") > 0)) reason = "No slots";
            else if (!s.isCantrip() && u.slotUsed) reason = "Already used a slot this turn";
            List<Object> lv = new ArrayList<>();
            if (!s.isCantrip()) for (int i = s.level; i <= hr.slots.length; i++) if (hr.slots[i - 1] > 0) lv.add(i);
            int maxT = s.area == Spells.Area.MULTI ? s.targets : 1;
            spells.add(Json.obj("id", s.id, "name", s.name, "level", s.level, "cast", s.cast, "kind", s.kind, "area", s.area,
                    "areaSize", s.areaSize, "range", s.range, "targets", maxT, "upTargets", s.upcastTargets, "levels", lv,
                    "available", reason.isEmpty(), "reason", reason, "desc", s.desc, "dice", s.isCantrip() ? Spells.scaledCantripDice(s, hr.level) : s.dice,
                    "damageType", s.damageType, "targetsAllies", s.targetsAllies || s.kind == Spells.Kind.HEAL, "elements", id.equals("chromatic_orb"),
                    "concentration", s.concentration, "selfOnly", id.equals("mage_armor")));
        }
        List<Object> feats = new ArrayList<>();
        java.util.function.BiConsumer<Object[], Boolean> add = (a, ok) -> feats.add(Json.obj("id", a[0], "name", a[1], "cost", a[2], "target", a[3], "desc", a[4], "available", ok));
        if (hr.resourcesMax.containsKey("second_wind")) add.accept(new Object[]{"second_wind", "Second Wind (" + hr.res("second_wind") + ")", "bonus", "none", "Regain 1d10 + level HP."}, u.bonus && hr.res("second_wind") > 0);
        if (hr.resourcesMax.containsKey("action_surge")) add.accept(new Object[]{"action_surge", "Action Surge", "free", "none", "Take one more action this turn."}, !u.action && hr.res("action_surge") > 0);
        if (hr.resourcesMax.containsKey("rage")) add.accept(new Object[]{"rage", "Rage (" + hr.res("rage") + ")", "bonus", "none", "+2 melee damage, resist physical damage."}, u.bonus && !u.has("raging") && hr.res("rage") > 0);
        if (hr.hasFeature("reckless_attack")) add.accept(new Object[]{"reckless", "Reckless Attack", "free", "none", "Advantage on melee attacks this turn; enemies get Advantage on you."}, !u.reckless && u.action);
        if (hr.hasFeature("cunning_action")) {
            add.accept(new Object[]{"cunning_dash", "Cunning Dash", "bonus", "none", "Dash as a Bonus Action."}, u.bonus);
            add.accept(new Object[]{"cunning_disengage", "Cunning Disengage", "bonus", "none", "Disengage as a Bonus Action."}, u.bonus);
            add.accept(new Object[]{"cunning_hide", "Cunning Hide", "bonus", "none", "Hide as a Bonus Action."}, u.bonus);
        }
        if (hr.hasFeature("steady_aim")) add.accept(new Object[]{"steady_aim", "Steady Aim", "bonus", "none", "Advantage on your next attack (can't move)."}, u.bonus && !u.moved);
        if (hr.resourcesMax.containsKey("bardic_inspiration")) add.accept(new Object[]{"bardic_inspiration", "Bardic Inspiration (" + hr.res("bardic_inspiration") + ")", "bonus", "ally", "Give an ally a d6 to rescue a failed roll."}, u.bonus && hr.res("bardic_inspiration") > 0);
        if (hr.resourcesMax.containsKey("lay_on_hands")) add.accept(new Object[]{"lay_on_hands", "Lay on Hands (" + hr.res("lay_on_hands") + " HP)", "bonus", "ally", "Heal by touch from your pool."}, u.bonus && hr.res("lay_on_hands") > 0);
        if (hr.resourcesMax.containsKey("channel_divinity") && hr.classId.equals("cleric")) {
            add.accept(new Object[]{"divine_spark", "Divine Spark (" + hr.res("channel_divinity") + ")", "action", "any", "Heal an ally or sear a foe for 1d8 + WIS."}, u.action && hr.res("channel_divinity") > 0);
            if (hr.hasFeature("preserve_life")) add.accept(new Object[]{"preserve_life", "Preserve Life", "action", "none", "Restore HP to wounded allies within 30 ft."}, u.action && hr.res("channel_divinity") > 0);
        }
        if (hr.classId.equals("paladin")) {
            add.accept(new Object[]{"smite", u.smiteArmed ? "Divine Smite: ARMED" : "Arm Divine Smite", "toggle", "none", "Your next melee hit adds 2d8 radiant (spends a slot + bonus action)."}, true);
            if (hr.hasFeature("sacred_weapon")) add.accept(new Object[]{"sacred_weapon", "Sacred Weapon", "action", "none", "Add CHA to weapon attacks."}, u.action && hr.res("channel_divinity") > 0 && !u.has("sacred_weapon"));
        }
        if (hr.resourcesMax.containsKey("free_mark")) add.accept(new Object[]{"free_mark", "Favored Enemy: Hunter's Mark (" + hr.res("free_mark") + ")", "bonus", "enemy", "Mark a foe without a slot."}, u.bonus && hr.res("free_mark") > 0 && u.conc.isEmpty());
        if (hr.resourcesMax.containsKey("breath_weapon")) add.accept(new Object[]{"breath_weapon", "Breath Weapon (" + hr.res("breath_weapon") + ")", "action", "cone15", "15-ft cone, DEX save."}, u.action && hr.res("breath_weapon") > 0);
        if (hr.resourcesMax.containsKey("healing_hands")) add.accept(new Object[]{"healing_hands", "Healing Hands", "action", "ally", "Heal by touch."}, u.action && hr.res("healing_hands") > 0);
        if (hr.resourcesMax.containsKey("adrenaline_rush")) add.accept(new Object[]{"adrenaline_rush", "Adrenaline Rush (" + hr.res("adrenaline_rush") + ")", "bonus", "none", "Dash + temporary HP."}, u.bonus && hr.res("adrenaline_rush") > 0);
        if (u.has("spiritual_weapon")) add.accept(new Object[]{"spiritual_weapon_strike", "Spiritual Weapon Strike", "bonus", "enemy", "The spectral weapon attacks."}, u.bonus);
        if (u.has("flaming_sphere")) add.accept(new Object[]{"sphere_ram", "Ram Flaming Sphere", "bonus", "point5", "Roll the sphere into foes."}, u.bonus);
        List<Object> items = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String it : hr.inventory) counts.merge(it, 1, Integer::sum);
        for (var e : counts.entrySet()) {
            Items.Item it = Items.item(e.getKey());
            if (it == null || !(it.kind.equals("potion") || it.kind.equals("bomb"))) continue;
            items.add(Json.obj("id", it.id, "name", it.name, "count", e.getValue(), "kind", it.kind, "desc", it.desc,
                    "available", it.kind.equals("potion") ? (u.bonus || u.action) : u.action));
        }
        return Json.obj("weapon", Json.obj("name", wp.name, "toHit", hr.weaponAttack(wp), "damage", wp.dice + "+" + hr.weaponDamageMod(wp) + " " + wp.type,
                        "ranged", wp.ranged, "range", wp.ranged ? wp.range : 5, "longRange", wp.longRange, "thrown", wp.thrown, "mastery", wp.mastery),
                "spells", spells, "features", feats, "items", items, "spellAttack", hr.spellAttack(), "spellDC", hr.spellDC(),
                "slots", hr.slots, "slotsMax", hr.slotsMax);
    }
}
