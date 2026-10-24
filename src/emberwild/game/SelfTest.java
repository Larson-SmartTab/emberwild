package emberwild.game;

import emberwild.rules.Hero;
import emberwild.story.CampaignRegistry;
import emberwild.util.Json;

import java.nio.file.Path;
import java.util.*;

/**
 * Headless playtest: a bot party (Sylwen + companions, or more heroes) plays the whole core campaign with the
 * combat AI, picking story choices at random. Reports battles, deaths, the route taken and whether the end was reached.
 * Used to check the campaign can always be finished and that the encounters are winnable.
 */
public final class SelfTest {
    public static boolean run(CampaignRegistry registry, Path saves, int runs) throws Exception {
        int finished = 0, totalWon = 0, totalLost = 0;
        Map<String, int[]> byBattle = new TreeMap<>();
        Set<String> scenesSeen = new TreeSet<>();
        long t0 = System.currentTimeMillis();
        for (int run = 1; run <= runs; run++) {
            int extra = run % 3 == 0 ? 2 : (run % 3 == 2 ? 1 : 0); // vary party size: 1, 2 or 3 human heroes
            Game g = new Game(registry, saves.resolve("selftest"));
            g.fast = true;
            Random rnd = new Random(run * 7919L);
            String pid;
            synchronized (g) {
                pid = g.join("selftest-bot", "Bot", false).id;
                g.claimPregen(pid, "emberwild", "sylwen");
                if (extra >= 1) { String p2 = g.join("selftest-bot2", "Bot2", false).id; g.claimPregen(p2, "emberwild", "torva"); }
                if (extra >= 2) { String p3 = g.join("selftest-bot3", "Bot3", false).id; g.claimPregen(p3, "emberwild", "kael"); }
                Map<String, Object> r = g.start(pid, "emberwild");
                if (!Json.bool(r, "ok", false)) throw new IllegalStateException("start failed: " + r);
            }
            List<String> route = new ArrayList<>();
            String lastScene = "";
            int steps = 0;
            long deadline = System.currentTimeMillis() + Long.getLong("selftest.ms", 120_000L);
            String lastBattle = "";
            Set<Combat> seenCombats = Collections.newSetFromMap(new IdentityHashMap<>());
            while (System.currentTimeMillis() < deadline) {
                Thread.sleep(2);
                synchronized (g) {
                    if (g.phase.equals("end")) break;
                    if (g.combat != null) {
                        if (!lastBattle.equals(g.combat.enc.id + g.combat.hashCode()) && Boolean.getBoolean("selftest.verbose")) {
                            StringBuilder sb = new StringBuilder("    * " + g.combat.enc.id + ": ");
                            for (Unit x : g.combat.units) sb.append(x.name.split(" ")[0]).append("(").append(x.hp()).append("/").append(x.maxHp()).append(x.isHero() ? " sl" + java.util.Arrays.toString(x.hero.slots) : "").append(") ");
                            sb.append(" flags=").append(g.flags.contains("heartstone") ? "HS " : "").append(g.flags.contains("cindrel_ally") ? "CA" : "");
                            System.out.println(sb);
                        }
                        lastBattle = g.combat.enc.id + g.combat.hashCode();

                        Unit u = g.combat.current();
                        if (u != null && u.isHero() && !u.ai && !u.has("autoplaying") && !g.combat.over && !u.down()) {
                            g.combatAction(g.hero(u.hero.id) == null ? pid : u.hero.player, Json.obj("type", "auto"));
                        }
                        continue;
                    }
                    if (!g.pending.isEmpty() || g.check != null) {
                        for (Game.PendingRoll pr : new ArrayList<>(g.pending)) if (pr.result == null) g.roll(pr.playerId, pr.heroId, false);
                        continue;
                    }
                    if (!g.sceneId.equals(lastScene)) { lastScene = g.sceneId; route.add(g.sceneId); scenesSeen.add(g.sceneId); }
                    List<Map<String, Object>> ch = new ArrayList<>(g.visibleChoices());
                    ch.removeIf(m -> m.get("disabled") != null);
                    if (ch.isEmpty()) { System.out.println("  [!!] stuck at " + g.sceneId); break; }
                    // prefer choices that move the story on; avoid endless loops
                    Map<String, Object> pick = ch.get(rnd.nextInt(ch.size()));
                    if (steps > 300) pick = ch.get(ch.size() - 1);
                    Set<String> voters = new LinkedHashSet<>();
                    for (Hero h : g.heroes) if (!h.npc) voters.add(h.player);
                    for (String voter : voters) {
                        if (!g.phase.equals("story") || !g.pending.isEmpty() || g.check != null || g.combat != null) break;
                        String err = g.vote(voter, (String) pick.get("id"));
                        if (err != null && !err.startsWith("Waiting") && !err.startsWith("That option")) System.out.println("  vote error: " + err + " @" + g.sceneId);
                    }
                    steps++;
                }
            }
            synchronized (g) {
                boolean ok = g.phase.equals("end");
                if (ok) finished++;
                totalWon += g.battlesWon;
                totalLost += g.battlesLost;
                StringBuilder party = new StringBuilder();
                for (Hero h : g.heroes) party.append(h.name.split(" ")[0]).append(" L").append(h.level).append(h.dead ? "†" : "").append(" ");
                System.out.printf("  Run %d (%d heroes): %s  battles won %d, lost %d, %d scenes, %d choices. Party: %s%n",
                        run, g.heroes.size(), ok ? "[OK] reached the end" : "[!!] did not finish (last: " + g.sceneId + "/" + lastBattle + ")",
                        g.battlesWon, g.battlesLost, route.size(), steps, party);
                if (Boolean.getBoolean("selftest.verbose") && g.battlesLost > 10) {
                    int start = 0;
                    for (int i = 0; i < g.log.size(); i++) if (String.valueOf(g.log.get(i).get("text")).startsWith("VHARKAS rises")) start = i;
                    for (int i = Math.max(0, start - 30); i < Math.min(g.log.size(), start + 60); i++) System.out.println("    || " + g.log.get(i).get("text"));
                }
                if (Boolean.getBoolean("selftest.verbose")) { StringBuilder hs = new StringBuilder("    history: "); for (String[] b : g.battleHistory) hs.append(b[0]).append(b[1].equals("victory") ? "✓" : "✗").append(b[2]).append(" "); System.out.println(hs); }
                for (String[] b : g.battleHistory) {
                    int[] r = byBattle.computeIfAbsent(b[0], k -> new int[4]);
                    r[b[1].equals("victory") ? 0 : 1]++;
                    r[2] += Integer.parseInt(b[2]);
                }
                if (!ok) {
                    System.out.println("    route: " + String.join(" → ", route));
                    int n = g.log.size();
                    for (int i = Math.max(0, n - 12); i < n; i++) System.out.println("    | " + g.log.get(i).get("text"));
                }
            }
            g.shutdown();
        }
        System.out.println("\n  Battle results (won / lost / avg rounds):");
        for (var e : byBattle.entrySet()) System.out.printf("    %-20s %3d / %3d   %.1f%n", e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2] / (double) Math.max(1, e.getValue()[0] + e.getValue()[1]));
        System.out.printf("%nSelf-test: %d/%d runs finished, %d battles won, %d lost, %d distinct scenes visited, %.1fs%n",
                finished, runs, totalWon, totalLost, scenesSeen.size(), (System.currentTimeMillis() - t0) / 1000.0);
        return finished == runs;
    }
}
