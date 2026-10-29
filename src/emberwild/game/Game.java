package emberwild.game;

import emberwild.rules.*;
import emberwild.rules.Skill;
import emberwild.story.Campaign;
import emberwild.story.CampaignRegistry;
import emberwild.util.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * The shared table. Every player's browser talks to this one object; all state changes happen under its monitor
 * and are pushed to everyone over Server-Sent Events.
 */
public final class Game {
    public final CampaignRegistry registry;
    final Path saveDir;
    final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "emberwild-tick"); t.setDaemon(true); return t; });
    public boolean fast = false;            // self-test: no animation delays

    // ---------------------------------------------------------------- table state
    public String phase = "lobby";          // lobby | story | combat | end
    String campaignId = "", sceneCampaign = "", sceneId = "";
    final Deque<String[]> returnStack = new ArrayDeque<>();
    String[] checkpoint = null;
    public final Set<String> flags = new LinkedHashSet<>();
    final Map<String, Integer> counters = new LinkedHashMap<>(), attitudes = new LinkedHashMap<>();
    final Set<String> visited = new LinkedHashSet<>(), chosenOnce = new HashSet<>();
    public final List<Hero> heroes = new ArrayList<>();
    final Map<String, Player> players = new LinkedHashMap<>();
    final Map<String, String> votes = new LinkedHashMap<>();
    long voteDeadline = 0;
    ScheduledFuture<?> voteTimer;
    final List<PendingRoll> pending = new ArrayList<>();
    Map<String, Object> outcome = null;      // last outcome banner
    Combat combat;
    Campaign.Choice battleChoice;
    String battleCampaign;
    final LinkedList<Map<String, Object>> log = new LinkedList<>();
    final List<Map<String, Object>> outbox = new ArrayList<>();
    boolean dirty = true;
    long version = 0;
    int battlesWon = 0, battlesLost = 0;
    long startedAt = 0;
    final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    String lastError = "";

    public static final class Player {
        public String id, name, color = "#e0b050";
        public boolean host;
        public transient boolean trusted;
        public int connections;
        public long lastSeen = System.currentTimeMillis();
        public boolean autoplay;
        Object json() { return Json.obj("id", id, "name", name, "color", color, "host", host, "online", connections > 0, "autoplay", autoplay); }
    }

    static final class PendingRoll {
        String heroId, playerId, label, choiceKey;
        int bonus;
        boolean adv, dis;
        Dice.Roll result;
        long deadline;
    }

    PendingCheck check;
    static final class PendingCheck {
        Campaign.Choice choice;
        String campaign;
        String actorHero;
    }

    public final DungeonMaster dm;
    final LinkedList<String> dialog = new LinkedList<>();
    boolean dmBusy = false;
    PendingRoll roleplay;
    String roleplaySkill = "";
    int roleplayDc = 0;
    Map<String, Object> roleplayCtx;
    final Set<String> roleplayRewards = new HashSet<>();

    public Game(CampaignRegistry registry, Path saveDir) {
        this.registry = registry;
        this.saveDir = saveDir;
        this.dm = new DungeonMaster(saveDir == null ? null : saveDir.toAbsolutePath().getParent().resolve("emberwild-settings.json"));
        exec.scheduleAtFixedRate(this::flush, 200, 90, TimeUnit.MILLISECONDS);
        exec.scheduleAtFixedRate(this::tickTimeouts, 1, 1, TimeUnit.SECONDS);
    }

    // ===================================================================================== infrastructure
    public void schedule(Runnable r, long ms) {
        exec.schedule(() -> {
            try { r.run(); } catch (Throwable t) { t.printStackTrace(); }
        }, fast ? 0 : ms, TimeUnit.MILLISECONDS);
    }

    public synchronized void changed() { dirty = true; }

    public synchronized void fx(Map<String, Object> event) { outbox.add(event); dirty = true; }

    public synchronized void log(String text, String style) {
        if (text == null || text.isBlank()) return;
        Map<String, Object> e = Json.obj("text", text, "style", style, "ts", System.currentTimeMillis());
        log.add(e);
        while (log.size() > 400) log.removeFirst();
        outbox.add(Json.obj("t", "log", "entry", e));
        dirty = true;
    }

    /** Broadcast a dice roll so every table member sees the same faces tumble. */
    public synchronized void rollFx(Dice.Roll r, String label, String playerId, boolean quiet) {
        r.label = label;
        Player p = players.get(playerId);
        outbox.add(Json.obj("t", "roll", "roll", r, "label", label, "player", p == null ? "" : p.name, "quiet", quiet));
        String nat = r.natural == 20 ? " ✦NAT 20✦" : r.natural == 1 ? " ✖nat 1" : "";
        log.add(Json.obj("text", "🎲 " + label + ": " + r + nat, "style", "roll", "ts", System.currentTimeMillis()));
        while (log.size() > 400) log.removeFirst();
        dirty = true;
    }

    public void addListener(Consumer<String> l) { listeners.add(l); }
    public void removeListener(Consumer<String> l) { listeners.remove(l); }

    void flush() {
        String msg;
        synchronized (this) {
            if (!dirty && outbox.isEmpty()) return;
            version++;
            Map<String, Object> payload = Json.obj("v", version, "fx", new ArrayList<>(outbox));
            outbox.clear();
            if (dirty) payload.put("state", state());
            dirty = false;
            msg = Json.write(payload);
        }
        for (Consumer<String> l : listeners) {
            try { l.accept(msg); } catch (Exception e) { listeners.remove(l); }
        }
    }

    void tickTimeouts() {
        synchronized (this) {
            long now = System.currentTimeMillis();
            // pending rolls: auto-roll for anyone who wandered off
            for (PendingRoll p : pending) {
                if (p.result == null && now > p.deadline) {
                    Player pl = players.get(p.playerId);
                    log((pl == null ? "Someone" : pl.name) + " took too long — the dice roll themselves.", "info");
                    roll(p.playerId, p.heroId, true);
                    return;
                }
            }
            // combat: an absent player's hero is auto-played after a grace period
            if (combat != null && !combat.over) {
                Unit u = combat.current();
                if (u != null && u.isHero() && !u.ai && !u.down() && shouldAutoPlay(u.hero) && now - combat.turnStartedAt > 4000 && !u.has("autoplaying")) {
                    u.add("autoplaying", 1);
                    log(u.name + "'s player is away — their hero acts on instinct.", "info");
                    schedule(() -> combat.runAi(u), 200);
                }
            }
            if (voteDeadline > 0 && now > voteDeadline) { voteDeadline = 0; resolveVotes(true); }
        }
    }

    public boolean shouldAutoPlay(Hero h) {
        if (h.npc) return true;
        Player p = players.get(h.player);
        return p == null || p.autoplay || (p.connections <= 0 && System.currentTimeMillis() - p.lastSeen > 8000);
    }

    // ===================================================================================== players & heroes
    public synchronized Player join(String id, String name, boolean local) { return join(id, name, local, true); }

    /**
     * @param trusted     the browser is on this machine, or presented the host key: becomes (and stays) a host
     * @param firstIsHost with no host key configured, the first person at the table becomes host
     */
    public synchronized Player join(String id, String name, boolean trusted, boolean firstIsHost) {
        Player p = id == null ? null : players.get(id);
        if (p == null) {
            p = new Player();
            p.id = id != null && id.matches("[a-zA-Z0-9-]{6,40}") ? id : UUID.randomUUID().toString();
            players.put(p.id, p);
            String[] palette = {"#e0b050", "#5fb3e0", "#e06a5f", "#7cd17c", "#c084fc", "#f0a050", "#50d0c0", "#f078b0"};
            p.color = palette[(players.size() - 1) % palette.length];
        }
        if (name != null && !name.isBlank()) p.name = name.trim().substring(0, Math.min(24, name.trim().length()));
        if (p.name == null) p.name = "Adventurer " + players.size();
        boolean anyHost = players.values().stream().anyMatch(x -> x.host);
        if (trusted) { p.host = true; p.trusted = true; }
        else if (!anyHost && firstIsHost) p.host = true;
        p.lastSeen = System.currentTimeMillis();
        log("👋 " + p.name + " pulls up a chair at the table.", "join");
        changed();
        return p;
    }

    public synchronized void connect(String pid, boolean on) {
        Player p = players.get(pid);
        if (p == null) return;
        p.connections = Math.max(0, p.connections + (on ? 1 : -1));
        p.lastSeen = System.currentTimeMillis();
        changed();
    }

    public synchronized boolean isHost(String pid) { Player p = players.get(pid); return p != null && p.host; }
    public synchronized boolean isTrusted(String pid) { Player p = players.get(pid); return p != null && p.trusted; }

    public synchronized Map<String, Object> createHero(String pid, Map<String, Object> spec) {
        Player p = players.get(pid);
        if (p == null) return Json.obj("ok", false, "errors", List.of("Join the table first."));
        List<String> errors = new ArrayList<>();
        Hero h = Hero.fromSpec(spec, errors);
        if (!errors.isEmpty() && Json.bool(spec, "strict", false)) return Json.obj("ok", false, "errors", errors);
        h.player = pid;
        return addHero(h, errors);
    }

    public synchronized Map<String, Object> importHero(String pid, Map<String, Object> saved) {
        if (!players.containsKey(pid)) return Json.obj("ok", false, "errors", List.of("Join the table first."));
        Hero h = Hero.load(saved);
        h.id = UUID.randomUUID().toString().substring(0, 8);
        h.player = pid;
        h.npc = false;
        return addHero(h, new ArrayList<>());
    }

    public synchronized Map<String, Object> claimPregen(String pid, String campaign, String pregenId) {
        Campaign c = registry.get(campaign);
        if (c == null) return Json.obj("ok", false, "errors", List.of("No such campaign"));
        for (Hero pg : c.pregens) {
            if (pg.id.equals(pregenId)) {
                Hero h = Hero.load(pg.save());
                h.id = UUID.randomUUID().toString().substring(0, 8);
                h.player = pid;
                h.npc = false;
                return addHero(h, new ArrayList<>());
            }
        }
        return Json.obj("ok", false, "errors", List.of("No such pre-made hero"));
    }

    Map<String, Object> addHero(Hero h, List<String> warnings) {
        long mine = heroes.stream().filter(x -> x.player.equals(h.player) && !x.npc).count();
        if (mine >= 2) return Json.obj("ok", false, "errors", List.of("Each player can bring at most two heroes."));
        // join at the party's level so late arrivals aren't left behind
        int lvl = 1;
        for (Hero o : heroes) if (!o.npc) lvl = Math.max(lvl, o.level);
        if (h.level < lvl) h.levelUp(lvl);
        h.hp = h.maxHp;
        heroes.add(h);
        log("⚔ " + h.name + ", " + h.species().name + " " + h.classDef().name + ", joins the party!", "join");
        fx(Json.obj("t", "heroJoined", "id", h.id, "name", h.name));
        balanceCompanions();
        changed();
        return Json.obj("ok", true, "hero", h.id, "warnings", warnings);
    }

    /** Take over a hero whose player is gone (e.g. after loading a save on another machine). */
    public synchronized Map<String, Object> claimHero(String pid, String heroId) {
        Hero h = hero(heroId);
        if (h == null || h.npc) return Json.obj("ok", false, "error", "No such hero");
        Player owner = players.get(h.player);
        if (owner != null && owner.connections > 0 && !owner.id.equals(pid) && !isHost(pid)) return Json.obj("ok", false, "error", owner.name + " is playing that hero.");
        h.player = pid;
        log(players.get(pid).name + " takes up " + h.name + ".", "join");
        changed();
        return Json.obj("ok", true);
    }

    public synchronized boolean removeHero(String pid, String heroId) {
        Iterator<Hero> it = heroes.iterator();
        while (it.hasNext()) {
            Hero h = it.next();
            if (h.id.equals(heroId) && (h.player.equals(pid) || isHost(pid))) {
                if (combat != null && !combat.over) return false;
                it.remove();
                log(h.name + " leaves the party.", "info");
                balanceCompanions();
                changed();
                return true;
            }
        }
        return false;
    }

    long humanHeroes() { return heroes.stream().filter(h -> !h.npc).count(); }

    /** Companions fill the party up to three; they step aside when enough real players arrive. */
    void balanceCompanions() {
        while (humanHeroes() + heroes.stream().filter(h -> h.npc).count() > 3 && heroes.stream().anyMatch(h -> h.npc)) {
            Hero npc = null;
            for (int i = heroes.size() - 1; i >= 0; i--) if (heroes.get(i).npc) { npc = heroes.get(i); break; }
            if (npc == null) break;
            if (combat != null && !combat.over) break;
            heroes.remove(npc);
            log(npc.name + " nods to the newcomers and steps back — \"You've got this. I'll keep the road clear.\"", "info");
        }
    }

    public synchronized Map<String, Object> prepare(String pid, String heroId, List<String> spells) {
        Hero h = hero(heroId);
        if (h == null || (!h.player.equals(pid) && !isHost(pid))) return Json.obj("ok", false, "error", "Not your hero");
        if (combat != null && !combat.over) return Json.obj("ok", false, "error", "You can't change prepared spells mid-battle.");
        Classes.ClassDef c = h.classDef();
        if (!c.caster()) return Json.obj("ok", false, "error", "Not a spellcaster");
        int max = c.preparedAt(h.level);
        List<String> out = new ArrayList<>();
        for (String s : spells) {
            Spells.SpellDef sd = Spells.get(s);
            if (sd == null || sd.isCantrip() || sd.level > c.maxSpellLevel(h.level) || !sd.lists.contains(c.spellList)) continue;
            if (h.classId.equals("wizard") && !h.spellbook.contains(s)) continue;
            if (h.alwaysPrepared().contains(s) || out.contains(s)) continue;
            if (out.size() < max) out.add(s);
        }
        h.prepared.clear();
        h.prepared.addAll(out);
        log(h.name + " prepares: " + String.join(", ", out.stream().map(x -> Spells.get(x).name).toList()) + ".", "info");
        changed();
        return Json.obj("ok", true);
    }

    public synchronized Map<String, Object> attune(String pid, String heroId, String itemId) {
        Hero h = hero(heroId);
        if (h == null || (!h.player.equals(pid) && !isHost(pid))) return Json.obj("ok", false, "error", "Not your hero");
        Items.Item it = Items.item(itemId);
        if (it == null || !h.inventory.contains(itemId)) return Json.obj("ok", false, "error", "You don't carry that.");
        if (h.attuned.contains(itemId)) { h.attuned.remove(itemId); log(h.name + " ends attunement to " + it.name + ".", "info"); }
        else {
            if (!it.attunable) return Json.obj("ok", false, "error", "That item doesn't need attunement.");
            if (h.attuned.size() >= 3) return Json.obj("ok", false, "error", "You can attune to at most three items.");
            h.attuned.add(itemId);
            log("✨ " + h.name + " attunes to " + it.name + ".", "good");
        }
        int hpBefore = h.hp;
        h.recompute(true);
        h.hp = Math.min(h.maxHp, Math.max(hpBefore, h.hp));
        changed();
        return Json.obj("ok", true);
    }

    public synchronized Map<String, Object> giveItem(String pid, String fromHero, String toHero, String itemId) {
        Hero a = hero(fromHero), b = hero(toHero);
        if (a == null || b == null || (!a.player.equals(pid) && !isHost(pid))) return Json.obj("ok", false, "error", "Not your hero");
        if (!a.inventory.remove(itemId)) return Json.obj("ok", false, "error", "Item not found");
        a.attuned.remove(itemId);
        b.inventory.add(itemId);
        log(a.name + " hands " + Items.item(itemId).name + " to " + b.name + ".", "info");
        changed();
        return Json.obj("ok", true);
    }

    Hero hero(String id) { for (Hero h : heroes) if (h.id.equals(id)) return h; return null; }

    List<Hero> heroesOf(String pid) { List<Hero> l = new ArrayList<>(); for (Hero h : heroes) if (h.player.equals(pid) && !h.npc) l.add(h); return l; }

    // ===================================================================================== campaign flow
    public synchronized Map<String, Object> start(String pid, String campaign) {
        Campaign c = registry.get(campaign);
        if (c == null) return Json.obj("ok", false, "error", "Unknown campaign");
        if (!phase.equals("lobby") && !isHost(pid)) return Json.obj("ok", false, "error", "A tale is already under way — only the host can start a different one.");
        if (heroes.stream().noneMatch(h -> !h.npc)) return Json.obj("ok", false, "error", "Create or pick at least one hero first.");
        campaignId = c.id;
        sceneCampaign = ""; sceneId = "";
        flags.clear(); counters.clear(); attitudes.clear(); visited.clear(); chosenOnce.clear(); returnStack.clear();
        votes.clear(); pending.clear(); outcome = null; combat = null; checkpoint = null;
        battlesWon = battlesLost = 0;
        heroes.removeIf(h -> h.npc);
        for (Hero h : heroes) h.longRest();
        startedAt = System.currentTimeMillis();
        phase = "story";
        log("📜 The tale begins: " + c.title + " — by " + c.author, "title");
        enter(c.id, c.start, true);
        return Json.obj("ok", true);
    }

    Campaign.Scene scene() {
        Campaign c = registry.get(sceneCampaign);
        return c == null ? null : c.scenes.get(sceneId);
    }

    void enter(String campaign, String id, boolean runEffects) {
        for (int guard = 0; guard < 20; guard++) {
            if (id == null || id.isEmpty()) return;
            switch (id) {
                case "@return" -> {
                    String[] back = returnStack.poll();
                    if (back == null) { endCampaign(); return; }
                    log("↩ The road leads back to familiar ground.", "info");
                    campaign = back[0]; id = back[1]; runEffects = false;
                    continue;
                }
                case "@end" -> { endCampaign(); return; }
                case "@checkpoint" -> {
                    if (checkpoint == null) { Campaign c = registry.get(campaignId); campaign = c.id; id = c.start; }
                    else { campaign = checkpoint[0]; id = checkpoint[1]; }
                    runEffects = false;
                    continue;
                }
                case "@restart" -> { Campaign c = registry.get(campaignId); campaign = c.id; id = c.start; continue; }
                default -> {}
            }
            Campaign c = registry.get(campaign);
            if (c == null || !c.scenes.containsKey(id)) { log("(The story stumbles: missing scene '" + id + "')", "bad"); return; }
            Campaign.Scene s = c.scenes.get(id);
            if (campaign.equals(sceneCampaign) && id.equals(sceneId) && phase.equals("story")) runEffects = false; // looping back to the same scene
            sceneCampaign = campaign;
            sceneId = id;
            visited.add(campaign + ":" + id);
            votes.clear();
            voteDeadline = 0;
            phase = "story";
            if (s.checkpoint) { checkpoint = new String[]{campaign, id}; autosave(); }
            fx(Json.obj("t", "scene", "id", id, "title", s.title, "chapter", s.chapter, "backdrop", s.backdrop, "music", s.music));
            if (!s.title.isEmpty()) log("— " + (s.chapter.isEmpty() ? "" : s.chapter + ": ") + s.title + " —", "scene");
            if (runEffects) for (Map<String, Object> f : s.onEnter) apply(f, null, null);
            if (!s.autoGo.isEmpty()) { id = s.autoGo; runEffects = true; continue; }
            changed();
            return;
        }
    }

    void endCampaign() {
        phase = "end";
        long mins = (System.currentTimeMillis() - startedAt) / 60000;
        log("🏁 The tale is told. (" + mins + " minutes, " + battlesWon + " battles won)", "title");
        fx(Json.obj("t", "end"));
        autosave();
        changed();
    }

    /** Choices visible to the table right now, including hooks into other campaigns. */
    List<Map<String, Object>> visibleChoices() {
        List<Map<String, Object>> out = new ArrayList<>();
        Campaign.Scene s = scene();
        if (s == null) return out;
        for (Campaign.Choice ch : s.choices) {
            String key = sceneCampaign + ":" + s.id + ":" + ch.id;
            if (ch.once && chosenOnce.contains(key)) continue;
            String why = unmet(ch.requires);
            if (why != null && why.startsWith("hide")) continue;
            Map<String, Object> m = Json.obj("id", ch.id, "label", ch.label, "hint", ch.hint, "icon", ch.icon, "check", ch.check, "dc", ch.dc,
                    "mode", ch.mode, "spell", ch.spell, "battle", ch.battle, "disabled", why, "votes", voters(ch.id));
            if (!ch.check.isEmpty()) m.put("checkLabel", checkLabel(ch.check));
            if (!ch.spell.isEmpty()) { Spells.SpellDef sd = Spells.get(ch.spell); m.put("spellName", sd == null ? ch.spell : sd.name); m.put("caster", casterFor(ch.spell, null) == null ? null : casterFor(ch.spell, null).name); }
            out.add(m);
        }
        // hooks: other campaigns meshing into this moment
        for (Map<String, Object> hk : registry.hooksFor(s.anchors, sceneCampaign)) {
            String id = "hook:" + hk.get("campaign") + ":" + hk.get("entry");
            if (visited.contains(hk.get("campaign") + ":" + hk.get("entry"))) continue;
            out.add(Json.obj("id", id, "label", hk.get("label"), "hint", "A tale by " + hk.get("author") + " — \"" + hk.get("title") + "\"", "icon", "📜",
                    "hook", true, "votes", voters(id)));
        }
        if (out.isEmpty()) {
            if (!returnStack.isEmpty()) out.add(Json.obj("id", "@return", "label", "Return to the main road", "icon", "↩", "votes", voters("@return")));
            else out.add(Json.obj("id", "@end", "label", "The End", "icon", "🏁", "votes", voters("@end")));
        }
        return out;
    }

    List<String> voters(String choiceId) {
        List<String> l = new ArrayList<>();
        for (var e : votes.entrySet()) if (e.getValue().equals(choiceId)) l.add(e.getKey());
        return l;
    }

    static String checkLabel(String check) {
        if (check.equals("luck")) return "Luck (d20)";
        if (check.startsWith("save:")) { Ability a = Ability.of(check.substring(5)); return (a == null ? check : a.full) + " save"; }
        Skill sk = Skill.of(check);
        if (sk != null) return sk.label;
        Ability a = Ability.of(check);
        return a == null ? check : a.full;
    }

    /** null = available; "hide…" = hidden; other text = shown but disabled with that reason. */
    String unmet(Map<String, Object> rq) {
        if (rq == null || rq.isEmpty()) return null;
        String f = Json.str(rq, "flag", null);
        if (f != null && !flags.contains(f)) return "hide";
        String nf = Json.str(rq, "notFlag", null);
        if (nf != null && flags.contains(nf)) return "hide";
        String v = Json.str(rq, "visited", null);
        if (v != null && !visited.contains(v.contains(":") ? v : sceneCampaign + ":" + v)) return "hide";
        String nv = Json.str(rq, "notVisited", null);
        if (nv != null && visited.contains(nv.contains(":") ? nv : sceneCampaign + ":" + nv)) return "hide";
        String it = Json.str(rq, "item", null);
        if (it != null && heroes.stream().noneMatch(h -> h.inventory.contains(it))) return "hide";
        String cl = Json.str(rq, "class", null);
        if (cl != null && heroes.stream().noneMatch(h -> h.classId.equals(cl))) return "Requires a " + cl;
        String sp = Json.str(rq, "species", null);
        if (sp != null && heroes.stream().noneMatch(h -> h.speciesId.equals(sp))) return "Requires a " + sp;
        String sk = Json.str(rq, "skill", null);
        if (sk != null && heroes.stream().noneMatch(h -> h.skills.contains(sk))) return "Requires proficiency in " + checkLabel(sk);
        int g = Json.integer(rq, "gold", 0);
        if (g > 0 && heroes.stream().mapToInt(h -> h.gold).sum() < g) return "Need " + g + " gp";
        return null;
    }

    Hero casterFor(String spell, String pid) {
        Spells.SpellDef sd = Spells.get(spell);
        Hero best = null;
        for (Hero h : heroes) {
            if (h.dead || !h.canCast(spell)) continue;
            if (sd != null && !sd.isCantrip() && h.lowestSlot(sd.level) == 0) continue;
            if (pid != null && h.player.equals(pid)) return h;
            if (best == null) best = h;
        }
        return best;
    }

    public synchronized String vote(String pid, String choiceId) {
        if (!phase.equals("story")) return "Not now.";
        if (!pending.isEmpty() || check != null) return "Waiting on the dice!";
        if (heroesOf(pid).isEmpty() && !isHost(pid)) return "Create a hero to take part in decisions.";
        boolean known = visibleChoices().stream().anyMatch(m -> m.get("id").equals(choiceId) && m.get("disabled") == null);
        if (!known) return "That option isn't available.";
        votes.put(pid, choiceId);
        changed();
        resolveVotes(false);
        return null;
    }

    public synchronized void force(String pid) { if (isHost(pid)) resolveVotes(true); }

    void resolveVotes(boolean force) {
        if (votes.isEmpty()) return;
        Set<String> voters = new HashSet<>();
        for (Hero h : heroes) if (!h.npc) { Player p = players.get(h.player); if (p != null && (p.connections > 0 || fast)) voters.add(p.id); }
        boolean all = votes.keySet().containsAll(voters);
        if (!all && !force) {
            if (voteDeadline == 0) voteDeadline = System.currentTimeMillis() + 30000;
            changed();
            return;
        }
        // majority; ties broken by the first vote cast
        Map<String, Integer> tally = new LinkedHashMap<>();
        for (String c : votes.values()) tally.merge(c, 1, Integer::sum);
        String win = null; int best = -1;
        for (var e : tally.entrySet()) if (e.getValue() > best) { best = e.getValue(); win = e.getKey(); }
        String firstVoter = null;
        for (var e : votes.entrySet()) if (e.getValue().equals(win)) { firstVoter = e.getKey(); break; }
        votes.clear();
        voteDeadline = 0;
        choose(win, firstVoter);
    }

    void choose(String choiceId, String pid) {
        outcome = null;
        if (choiceId.equals("@return") || choiceId.equals("@end")) { enter(sceneCampaign, choiceId, true); return; }
        if (choiceId.startsWith("hook:")) {
            String[] p = choiceId.split(":", 3);
            returnStack.push(new String[]{sceneCampaign, sceneId});
            Campaign c = registry.get(p[1]);
            log("📜 The party follows a new thread: \"" + (c == null ? p[1] : c.title) + "\"" + (c == null ? "" : " by " + c.author), "title");
            enter(p[1], p[2], true);
            return;
        }
        Campaign.Scene s = scene();
        if (s == null) return;
        Campaign.Choice ch = null;
        for (Campaign.Choice c : s.choices) if (c.id.equals(choiceId)) ch = c;
        if (ch == null) return;
        if (ch.once) chosenOnce.add(sceneCampaign + ":" + s.id + ":" + ch.id);
        Hero actor = actorFor(pid);
        log("➤ " + ch.label, "choice");
        for (Map<String, Object> f : ch.effects) apply(f, actor, null);
        if (!ch.spell.isEmpty()) {
            Hero caster = casterFor(ch.spell, pid);
            Spells.SpellDef sd = Spells.get(ch.spell);
            if (caster == null) { outcome = Json.obj("text", "Nobody has the magic (or the spell slots) for that.", "success", false); changed(); return; }
            if (sd != null && !sd.isCantrip()) caster.spendSlot(caster.lowestSlot(sd.level));
            log("✦ " + caster.name + " casts " + (sd == null ? ch.spell : sd.name) + "!", "spell");
            fx(Json.obj("t", "storySpell", "spell", ch.spell, "hero", caster.id, "dtype", sd == null ? "arcane" : sd.damageType));
            if (ch.check.isEmpty()) { applyOutcome(ch.pass, caster, true); return; }
            actor = caster;
        }
        if (!ch.check.isEmpty()) { beginCheck(ch, actor, pid); return; }
        if (!ch.battle.isEmpty()) { beginBattle(ch); return; }
        if (!ch.go.isEmpty()) enter(sceneCampaign, ch.go, true);
        else changed();
    }

    Hero actorFor(String pid) {
        if (pid != null) for (Hero h : heroes) if (h.player.equals(pid) && !h.dead && !h.npc) return h;
        for (Hero h : heroes) if (!h.dead) return h;
        return heroes.isEmpty() ? null : heroes.get(0);
    }

    // ===================================================================================== checks with interactive dice
    int bonusFor(Hero h, String check) {
        if (check.equals("luck")) return 0;
        if (check.startsWith("save:")) { Ability a = Ability.of(check.substring(5)); return a == null ? 0 : h.saveBonus(a); }
        Skill sk = Skill.of(check);
        if (sk != null) return h.skillBonus(sk);
        Ability a = Ability.of(check);
        return a == null ? 0 : h.checkBonus(a);
    }

    void beginCheck(Campaign.Choice ch, Hero actor, String pid) {
        check = new PendingCheck();
        check.choice = ch;
        check.campaign = sceneCampaign;
        check.actorHero = actor == null ? null : actor.id;
        pending.clear();
        List<Hero> rollers = new ArrayList<>();
        switch (ch.mode) {
            case "group", "each" -> { for (Hero h : heroes) if (!h.dead) rollers.add(h); }
            case "chooser" -> { if (actor != null) rollers.add(actor); }
            default -> { // best: the hero with the highest bonus steps up
                Hero best = null;
                for (Hero h : heroes) if (!h.dead && (best == null || bonusFor(h, ch.check) > bonusFor(best, ch.check))) best = h;
                if (best != null) rollers.add(best);
            }
        }
        if (rollers.isEmpty()) { check = null; return; }
        boolean adv = !ch.advantageIf.isEmpty() && flags.contains(ch.advantageIf);
        // Guidance: a cleric/druid in the party adds 1d4 (cantrip) - folded in at roll time
        for (Hero h : rollers) {
            PendingRoll p = new PendingRoll();
            p.heroId = h.id;
            p.playerId = h.player;
            p.bonus = bonusFor(h, ch.check);
            p.adv = adv || (ch.check.startsWith("save:") && h.hasTrait("gnomish_cunning") && Set.of("int", "wis", "cha").contains(ch.check.substring(5)));
            p.label = h.name + " — " + checkLabel(ch.check) + " (DC " + ch.dc + ")";
            p.deadline = System.currentTimeMillis() + 45000;
            pending.add(p);
        }
        String who = rollers.size() == 1 ? rollers.get(0).name + " steps up" : "Everyone rolls";
        log("🎲 " + who + ": " + checkLabel(ch.check) + " check, DC " + ch.dc + (adv ? " (with Advantage!)" : ""), "check");
        // AI companions and absent players roll straight away
        for (PendingRoll p : new ArrayList<>(pending)) {
            Hero h = hero(p.heroId);
            if (h != null && shouldAutoPlay(h)) roll(p.playerId, p.heroId, true);
        }
        changed();
    }

    /** A player clicks the big d20. */
    public synchronized String roll(String pid, String heroId, boolean auto) {
        PendingRoll target = null;
        for (PendingRoll p : pending) if (p.result == null && (p.heroId.equals(heroId) || (heroId == null && p.playerId.equals(pid)))) { target = p; break; }
        if (target == null) return "Nothing to roll right now.";
        if (!auto && !target.playerId.equals(pid) && !isHost(pid)) return "That's someone else's roll.";
        Hero h = hero(target.heroId);
        int guidance = 0;
        if (check != null && !check.choice.check.startsWith("save:") && heroes.stream().anyMatch(x -> x.canCast("guidance") && !x.dead)) guidance = Dice.die(4);
        Dice.Roll r = Dice.d20(target.bonus + guidance, target.adv, false, h != null && h.hasTrait("lucky"));
        if (h != null && h.inspiration && check != null && r.total < check.choice.dc && r.natural != 20 && target != roleplay) {
            // Heroic Inspiration: reroll a failure
            h.inspiration = false;
            Dice.Roll r2 = Dice.d20(target.bonus + guidance, target.adv, false, h.hasTrait("lucky"));
            log("✨ " + h.name + " spends Heroic Inspiration to reroll!", "good");
            if (r2.total > r.total) r = r2;
        }
        target.result = r;
        rollFx(r, target.label + (guidance > 0 ? " (+" + guidance + " Guidance)" : ""), target.playerId, false);
        if (pending.stream().allMatch(p -> p.result != null)) schedule(check != null ? this::finishCheck : this::finishRoleplay, 1900);
        changed();
        return null;
    }

    void finishCheck() {
        synchronized (this) {
            if (check == null) return;
            Campaign.Choice ch = check.choice;
            int ok = 0;
            List<Hero> failed = new ArrayList<>();
            Hero bestRoller = null;
            int bestTotal = -99;
            for (PendingRoll p : pending) {
                Hero h = hero(p.heroId);
                boolean pass = p.result.total >= ch.dc || (p.result.natural == 20 && ch.check.startsWith("save:"));
                if (pass) ok++; else if (h != null) failed.add(h);
                if (p.result.total > bestTotal) { bestTotal = p.result.total; bestRoller = h; }
            }
            boolean success = switch (ch.mode) {
                case "group" -> ok * 2 >= pending.size();
                case "each" -> ok > 0;
                default -> ok > 0;
            };
            Hero actor = bestRoller != null ? bestRoller : hero(check.actorHero);
            pending.clear();
            check = null;
            if (ch.mode.equals("each")) {
                // per-hero consequences: everyone who failed suffers the fail effects
                Campaign.Outcome o = success ? ch.pass : ch.fail;
                StringBuilder sb = new StringBuilder(o == null ? "" : o.text());
                if (ch.fail != null) for (Hero h : failed) for (Map<String, Object> f : ch.fail.effects()) apply(f, h, List.of(h));
                if (success && ch.pass != null) for (Map<String, Object> f : ch.pass.effects()) apply(f, actor, null);
                if (!failed.isEmpty() && success && ch.fail != null && !ch.fail.text().isEmpty()) sb.append("\n\n").append(names(failed)).append(": ").append(ch.fail.text());
                outcome = Json.obj("text", fill(sb.toString(), actor), "success", success, "failed", names(failed));
                if (!outcome.get("text").toString().isBlank()) log(fill(sb.toString(), actor), success ? "pass" : "fail");
                String go = o == null || o.go().isEmpty() ? ch.go : o.go();
                fx(Json.obj("t", "checkResult", "success", success));
                if (!go.isEmpty()) enter(sceneCampaign, go, true); else changed();
                return;
            }
            fx(Json.obj("t", "checkResult", "success", success));
            applyOutcome(success ? ch.pass : ch.fail, actor, success);
            if ((success ? ch.pass : ch.fail) == null && !ch.go.isEmpty()) enter(sceneCampaign, ch.go, true);
        }
    }

    static String names(List<Hero> l) { List<String> n = new ArrayList<>(); for (Hero h : l) n.add(h.name); return String.join(", ", n); }

    void applyOutcome(Campaign.Outcome o, Hero actor, boolean success) {
        if (o == null) { changed(); return; }
        if (!o.text().isBlank()) {
            String t = fill(o.text(), actor);
            outcome = Json.obj("text", t, "success", success);
            log(t, success ? "pass" : "fail");
        }
        for (Map<String, Object> f : o.effects()) apply(f, actor, null);
        if (!o.go().isEmpty()) {
            Map<String, Object> keep = outcome;
            enter(sceneCampaign, o.go(), true);
            outcome = keep;
        }
        changed();
    }

    String fill(String text, Hero actor) {
        if (text == null) return "";
        String party = names(heroes.stream().filter(h -> !h.dead).toList());
        Hero lead = actor != null ? actor : (heroes.isEmpty() ? null : heroes.get(0));
        return text.replace("{hero}", lead == null ? "the hero" : lead.name).replace("{party}", party.isEmpty() ? "the party" : party);
    }

    // ===================================================================================== effects
    void apply(Map<String, Object> f, Hero actor, List<Hero> scope) {
        String op = Json.str(f, "op", "");
        List<Hero> targets = scope != null ? scope : heroes.stream().filter(h -> !h.dead).toList();
        if (actor == null && !heroes.isEmpty()) actor = heroes.stream().filter(h -> !h.npc && !h.dead).findFirst().orElse(heroes.get(0));
        switch (op) {
            case "flag" -> flags.add(Json.str(f, "flag", ""));
            case "unflag" -> flags.remove(Json.str(f, "flag", ""));
            case "item" -> {
                String id = Json.str(f, "item", "");
                Items.Item it = Items.item(id);
                if (it == null || actor == null) return;
                actor.inventory.add(id);
                log("🎁 " + actor.name + " receives " + it.name + "!", "loot");
                fx(Json.obj("t", "loot", "item", it, "hero", actor.name));
            }
            case "takeItem" -> {
                String id = Json.str(f, "item", "");
                for (Hero h : heroes) if (h.inventory.remove(id)) { h.attuned.remove(id); break; }
            }
            case "gold" -> {
                int amt = Json.integer(f, "amount", 0);
                if (amt < 0) {
                    // the party chips in, richest first
                    int need = -amt;
                    List<Hero> rich = new ArrayList<>(heroes);
                    rich.sort((a, b) -> b.gold - a.gold);
                    for (Hero h : rich) { int pay = Math.min(h.gold, need); h.gold -= pay; need -= pay; if (need <= 0) break; }
                    log("💰 The party pays " + (-amt) + " gp.", "info");
                } else {
                    for (Hero h : targets) h.gold += amt;
                    log("💰 " + (targets.size() > 1 ? "Each hero gains " : "Gained ") + amt + " gp.", "loot");
                    fx(Json.obj("t", "gold", "amount", amt));
                }
            }
            case "xp" -> { int amt = Json.integer(f, "amount", 0); for (Hero h : targets) h.xp += amt; log("✦ " + amt + " XP each.", "xp"); checkLevels(); }
            case "heal" -> {
                for (Hero h : targets) { int got = h.heal(Dice.roll(Json.str(f, "dice", "1d8")).total); if (got > 0) log(h.name + " recovers " + got + " HP.", "heal"); }
            }
            case "damage" -> {
                String dice = Json.str(f, "dice", "1d6"), type = Json.str(f, "type", "bludgeoning");
                String save = Json.str(f, "save", null);
                int dc = Json.integer(f, "dc", 12);
                List<Hero> who = "chooser".equals(Json.str(f, "who", "")) && actor != null && scope == null ? List.of(actor) : targets;
                for (Hero h : who) {
                    int d = Dice.roll(dice).total;
                    if (save != null && !save.isEmpty()) {
                        Ability a = Ability.of(save);
                        Dice.Roll r = Dice.d20(a == null ? 0 : h.saveBonus(a), false, false, h.hasTrait("lucky"));
                        rollFx(r, h.name + " — " + (a == null ? save : a.full) + " save (DC " + dc + ")", h.player, true);
                        if (r.total >= dc) d /= 2;
                    }
                    if (h.resistances().contains(type)) d /= 2;
                    int before = h.hp;
                    h.hp = Math.max(0, h.hp - d);
                    log("💥 " + h.name + " takes " + d + " " + type + " damage.", "hurt");
                    fx(Json.obj("t", "storyHurt", "hero", h.id, "amount", d, "dtype", type));
                    if (h.hp == 0 && before > 0) { h.hp = 1; log(h.name + " staggers but stays on their feet (1 HP).", "bad"); }
                }
            }
            case "rest" -> {
                if (Json.str(f, "kind", "short").equals("long")) {
                    for (Hero h : heroes) h.longRest();
                    log("🌙 The party takes a long rest. HP, spell slots and abilities are restored." + (heroes.stream().anyMatch(h -> h.mageArmor) ? " (Mage Armor is cast anew at dawn.)" : ""), "rest");
                } else {
                    for (Hero h : heroes) { String s = h.shortRest(); if (!s.isEmpty()) log("☕ " + s, "rest"); }
                    log("☕ The party takes a short rest.", "rest");
                }
                fx(Json.obj("t", "rest"));
            }
            case "level" -> {
                int lvl = Json.integer(f, "level", 2);
                for (Hero h : heroes) if (h.level < lvl) { h.xp = Math.max(h.xp, Hero.xpForLevel(lvl)); }
                checkLevels(Math.min(Hero.MAX_LEVEL, lvl));
            }
            case "log", "say" -> log(fill(Json.str(f, "text", ""), actor), "narration");
            case "inspiration" -> { if (actor != null) { actor.inspiration = true; log("✨ " + actor.name + " gains Heroic Inspiration! (A failed check will be rerolled.)", "good"); } }
            case "condition" -> {}
            case "companion" -> {
                int below = Json.integer(f, "below", 3);
                String id = Json.str(f, "id", "");
                if (heroes.size() >= below) return;
                if (heroes.stream().anyMatch(h -> h.npc && h.name.equals(pregenName(id)))) return;
                Campaign c = registry.get(sceneCampaign);
                if (c == null) return;
                for (Hero pg : c.pregens) if (pg.id.equals(id)) {
                    Hero h = Hero.load(pg.save());
                    h.npc = true;
                    h.player = "npc";
                    h.id = "npc-" + id;
                    int lvl = 1;
                    for (Hero o : heroes) lvl = Math.max(lvl, o.level);
                    if (h.level < lvl) h.levelUp(lvl);
                    h.hp = h.maxHp;
                    heroes.add(h);
                    log("🤝 " + h.name + " joins the party as a companion!", "join");
                    fx(Json.obj("t", "heroJoined", "id", h.id, "name", h.name));
                }
            }
            case "dismiss" -> {
                String id = Json.str(f, "id", "");
                heroes.removeIf(h -> h.npc && h.id.equals("npc-" + id));
            }
            case "attitude" -> attitudes.merge(Json.str(f, "npc", ""), Json.integer(f, "delta", 0), Integer::sum);
            case "counter" -> counters.merge(Json.str(f, "name", ""), Json.integer(f, "delta", 1), Integer::sum);
            default -> {}
        }
        changed();
    }

    String pregenName(String id) {
        Campaign c = registry.get(sceneCampaign);
        if (c != null) for (Hero pg : c.pregens) if (pg.id.equals(id)) return pg.name;
        return id;
    }

    void checkLevels() { checkLevels(3); }

    /** XP alone advances heroes to level 3; story milestones (Fx.level) may lift that cap up to Hero.MAX_LEVEL. */
    void checkLevels(int cap) {
        for (Hero h : heroes) {
            while (h.level < Math.max(3, cap) && h.xp >= Hero.xpForLevel(h.level + 1)) {
                List<String> gains = h.levelUp(h.level + 1);
                log("⭐ LEVEL UP! " + h.name + " reaches level " + h.level + ": " + String.join(", ", gains), "level");
                fx(Json.obj("t", "levelUp", "hero", h.id, "name", h.name, "level", h.level, "gains", gains));
            }
        }
    }

    // ===================================================================================== battles
    void beginBattle(Campaign.Choice ch) {
        Campaign c = registry.get(sceneCampaign);
        Campaign.Encounter e = c == null ? null : c.encounters.get(ch.battle);
        if (e == null) { log("(Missing encounter " + ch.battle + ")", "bad"); return; }
        battleChoice = ch;
        battleCampaign = sceneCampaign;
        List<Hero> party = new ArrayList<>();
        for (Hero h : heroes) if (!h.dead) party.add(h);
        combat = new Combat(this, e, party, flags);
        phase = "combat";
        boolean surprise = !e.surpriseIf.isEmpty() && flags.contains(e.surpriseIf);
        combat.begin(surprise);
        changed();
    }

    public final List<String[]> battleHistory = new ArrayList<>();

    void combatEnded(Combat c) {
        if (combat != c) return;
        boolean won = c.result.equals("victory");
        battleHistory.add(new String[]{c.enc.id, c.result, String.valueOf(c.round), String.valueOf(heroes.stream().filter(h -> h.dead).count())});
        if (won) {
            battlesWon++;
            int share = heroes.isEmpty() ? 0 : c.xpPool / Math.max(1, heroes.size());
            if (share > 0) { for (Hero h : heroes) h.xp += share; log("✦ Each hero earns " + share + " XP.", "xp"); }
            checkLevels();
        } else battlesLost++;
        Campaign.Choice ch = battleChoice;
        combat = null;
        phase = "story";
        if (ch == null) { changed(); return; }
        Campaign.Outcome o = won ? ch.victory : ch.defeat;
        if (!won && o == null) {
            // no story consequence written: regroup at the last checkpoint
            for (Hero h : heroes) h.longRest();
            outcome = Json.obj("text", "Darkness… then voices, and the smell of a campfire. You wake, aching but whole, at a place you remember. It isn't over. The fight can be tried again.", "success", false);
            log(outcome.get("text").toString(), "fail");
            enter(sceneCampaign, "@checkpoint", false);
            return;
        }
        // a story-continuing defeat: survivors limp on with at least half their hit points, so one loss doesn't chain into the next
        if (!won) for (Hero h : heroes) { if (!h.dead) h.hp = Math.max(h.hp, Math.max(1, h.maxHp / 2)); }
        applyOutcome(o, actorFor(null), won);
        if (o != null && o.go().isEmpty() && !ch.go.isEmpty()) enter(sceneCampaign, ch.go, true);
        autosave();
    }

    public synchronized String combatAction(String pid, Map<String, Object> req) {
        if (combat == null) return "No battle in progress.";
        Unit u = combat.current();
        if (u == null) return "No one is acting.";
        if (!u.isHero()) return "Wait for your turn.";
        if (!u.hero.player.equals(pid) && !isHost(pid)) return "It's " + u.name + "'s turn.";
        if (Json.str(req, "type", "").equals("auto")) {
            if (!u.has("autoplaying")) { u.add("autoplaying", 1); schedule(() -> combat.runAi(u), 100); }
            return null;
        }
        String err = combat.act(u, req);
        if (err != null) lastError = err;
        return err;
    }

    // ===================================================================================== shop
    static final List<String> DEFAULT_STOCK = List.of("potion_healing", "potion_greater_healing", "alchemists_fire", "frost_flask", "antitoxin", "thieves_tools", "rope", "torch");

    public synchronized String buy(String pid, String heroId, String itemId, boolean sell) {
        Campaign.Scene s = scene();
        if (s == null || !s.shop || !phase.equals("story")) return "There's no shop here.";
        Hero h = hero(heroId);
        if (h == null || (!h.player.equals(pid) && !isHost(pid))) return "Not your hero.";
        Items.Item it = Items.item(itemId);
        if (it == null) return "Unknown item.";
        if (sell) {
            if (!h.inventory.remove(itemId)) return "You don't have that.";
            h.attuned.remove(itemId);
            int price = Math.max(1, it.value / 2);
            h.gold += price;
            log("🪙 " + h.name + " sells " + it.name + " for " + price + " gp.", "info");
        } else {
            if (!DEFAULT_STOCK.contains(itemId)) return "Not for sale here.";
            if (h.gold < it.value) return "Not enough gold (" + it.value + " gp).";
            h.gold -= it.value;
            h.inventory.add(itemId);
            log("🪙 " + h.name + " buys " + it.name + " for " + it.value + " gp.", "info");
        }
        changed();
        return null;
    }

    // ===================================================================================== in-character dialogue
    /**
     * A player speaks (kind "say") or acts (kind "do") in character. Everyone hears it in that hero's voice; then the
     * Dungeon Master improvises a reaction (NPC line + narration) and may call for a roleplay check.
     */
    public synchronized String say(String pid, String heroId, String text, String kind) {
        Player p = players.get(pid);
        if (p == null) return "Join first.";
        if (text == null || text.isBlank()) return "Say something!";
        text = text.trim();
        if (text.length() > 300) text = text.substring(0, 300);
        Hero h = hero(heroId);
        if (h == null) for (Hero x : heroes) if (x.player.equals(pid) && !x.npc) { h = x; break; }
        if (h == null || (!h.player.equals(pid) && !isHost(pid))) return "Speak as one of your own heroes.";
        if (!kind.equals("do")) kind = "say";
        String shown = kind.equals("say") ? h.name + ": “" + text + "”" : h.name + " " + text;
        Map<String, Object> e = Json.obj("text", shown, "style", kind.equals("say") ? "speech" : "deed", "who", h.name, "color", h.color, "ts", System.currentTimeMillis());
        log.add(e); while (log.size() > 400) log.removeFirst();
        outbox.add(Json.obj("t", "log", "entry", e));
        fx(Json.obj("t", "speech", "hero", h.id, "name", h.name, "text", text, "kind", kind, "voice", "hero:" + h.id, "pronouns", h.pronouns, "species", h.speciesId, "color", h.color));
        dialog.add(h.name + (kind.equals("say") ? ": \"" + text + "\"" : " " + text));
        while (dialog.size() > 8) dialog.removeFirst();
        if (dmBusy) { changed(); return null; }
        // context for the DM
        Campaign c = registry.get(sceneCampaign);
        Campaign.Scene sc = scene();
        String npc = sc != null && sc.npc != null ? Json.str(sc.npc, "name", "") : "";
        Map<String, Object> ctx = Json.obj("campaign", c == null ? "" : c.title, "scene", sc == null ? "" : sc.title, "sceneText", sc == null ? "" : sc.text,
                "npc", phase.equals("combat") ? "" : npc, "npcTitle", sc != null && sc.npc != null ? Json.str(sc.npc, "title", "") : "",
                "party", names(heroes), "recent", String.join("\n", dialog), "hero", h.name,
                "heroDesc", h.species().name + " " + h.classDef().name + ", " + h.pronouns, "text", text, "kind", kind, "heroId", h.id,
                "npcColor", sc != null && sc.npc != null ? Json.str(sc.npc, "color", "#888") : "#888", "inCombat", phase.equals("combat"));
        dmBusy = true;
        fx(Json.obj("t", "dmThinking", "on", true));
        changed();
        Thread.ofVirtual().start(() -> {
            DungeonMaster.Reply r;
            try { r = dm.respond(ctx); } catch (Exception ex) { r = dm.rules(ctx); }
            dmReply(ctx, r);
        });
        return null;
    }

    synchronized void dmReply(Map<String, Object> ctx, DungeonMaster.Reply r) {
        dmBusy = false;
        fx(Json.obj("t", "dmThinking", "on", false));
        String npc = r.npc() == null || r.npc().isBlank() ? Json.str(ctx, "npc", "") : r.npc();
        if (r.npcLine() != null && !r.npcLine().isBlank()) {
            String line = r.npcLine().replaceAll("^[\"“]|[\"”]$", "");
            String who = npc.isBlank() ? "A voice" : npc;
            Map<String, Object> e = Json.obj("text", who + ": “" + line + "”", "style", "npcspeech", "who", who, "color", Json.str(ctx, "npcColor", "#888"), "ts", System.currentTimeMillis());
            log.add(e); outbox.add(Json.obj("t", "log", "entry", e));
            fx(Json.obj("t", "speech", "npc", who, "name", who, "text", line, "kind", "say", "voice", "npc:" + who, "color", Json.str(ctx, "npcColor", "#888")));
            dialog.add(who + ": \"" + line + "\"");
        }
        if (r.narration() != null && !r.narration().isBlank()) {
            log(r.narration(), "narration");
            fx(Json.obj("t", "narrate", "text", r.narration()));
        }
        boolean canCheck = !r.skill().isEmpty() && r.dc() > 0 && phase.equals("story") && pending.isEmpty() && check == null && !Json.bool(ctx, "inCombat", false);
        if (canCheck) {
            Hero h = hero(Json.str(ctx, "heroId", ""));
            Skill sk = Skill.of(r.skill());
            if (h != null && sk != null) {
                PendingRoll p = new PendingRoll();
                p.heroId = h.id; p.playerId = h.player; p.bonus = h.skillBonus(sk);
                p.label = h.name + " — Roleplay: " + sk.label + " (DC " + r.dc() + ")";
                p.deadline = System.currentTimeMillis() + 45000;
                p.choiceKey = "roleplay";
                pending.add(p);
                roleplay = p; roleplaySkill = sk.label; roleplayDc = r.dc(); roleplayCtx = ctx;
                log("🎭 The DM calls for a " + sk.label + " check (DC " + r.dc() + ")!", "check");
                if (shouldAutoPlay(h)) roll(p.playerId, p.heroId, true);
            }
        }
        changed();
    }

    void finishRoleplay() {
        Map<String, Object> ctx;
        boolean success;
        Hero h;
        synchronized (this) {
            if (roleplay == null || roleplay.result == null) { pending.clear(); roleplay = null; changed(); return; }
            success = roleplay.result.total >= roleplayDc || roleplay.result.natural == 20;
            h = hero(roleplay.heroId);
            ctx = new LinkedHashMap<>(roleplayCtx);
            ctx.put("skillLabel", roleplaySkill);
            pending.clear();
            roleplay = null;
            fx(Json.obj("t", "checkResult", "success", success));
            String key = sceneCampaign + ":" + sceneId + ":" + (h == null ? "" : h.id);
            if (success && h != null && !h.inspiration && roleplayRewards.add(key)) {
                h.inspiration = true;
                log("✨ Great roleplay! " + h.name + " gains Heroic Inspiration.", "good");
            }
            dmBusy = true;
            changed();
        }
        Thread.ofVirtual().start(() -> {
            String t;
            try { t = dm.outcome(ctx, success); } catch (Exception e) { t = success ? "It works!" : "It doesn't quite work."; }
            synchronized (this) {
                dmBusy = false;
                outcome = Json.obj("text", t, "success", success);
                log(t, success ? "pass" : "fail");
                fx(Json.obj("t", "narrate", "text", t));
                changed();
            }
        });
    }

    // ===================================================================================== chat & free dice
    public synchronized void chat(String pid, String text) {
        Player p = players.get(pid);
        if (p == null || text == null || text.isBlank()) return;
        text = text.trim();
        if (text.length() > 400) text = text.substring(0, 400);
        if (text.startsWith("/roll ") || text.startsWith("/r ")) { freeRoll(pid, text.substring(text.indexOf(' ') + 1)); return; }
        Map<String, Object> e = Json.obj("text", text, "style", "chat", "who", p.name, "color", p.color, "ts", System.currentTimeMillis());
        log.add(e);
        outbox.add(Json.obj("t", "log", "entry", e));
        dirty = true;
    }

    public synchronized String freeRoll(String pid, String expr) {
        Player p = players.get(pid);
        if (p == null) return "Join first.";
        if (!Dice.valid(expr)) return "Try something like 1d20+5 or 4d6kh3.";
        Dice.Roll r = Dice.roll(expr);
        rollFx(r, p.name + " rolls " + expr, pid, false);
        return null;
    }

    public synchronized void setAutoplay(String pid, boolean on) { Player p = players.get(pid); if (p != null) { p.autoplay = on; changed(); } }

    // ===================================================================================== saves
    public synchronized Map<String, Object> snapshot() {
        List<Object> hs = new ArrayList<>();
        for (Hero h : heroes) hs.add(h.save());
        List<Object> rs = new ArrayList<>();
        for (String[] r : returnStack) rs.add(List.of(r[0], r[1]));
        return Json.obj("format", "emberwild-save-1", "saved", System.currentTimeMillis(), "phase", phase.equals("combat") ? "story" : phase,
                "campaign", campaignId, "sceneCampaign", sceneCampaign, "scene", sceneId, "returnStack", rs,
                "checkpoint", checkpoint == null ? null : List.of(checkpoint[0], checkpoint[1]),
                "flags", new ArrayList<>(flags), "counters", counters, "attitudes", attitudes, "visited", new ArrayList<>(visited),
                "once", new ArrayList<>(chosenOnce), "heroes", hs, "battlesWon", battlesWon, "startedAt", startedAt,
                "players", players.values().stream().map(p -> Json.obj("id", p.id, "name", p.name, "color", p.color)).toList());
    }

    void autosave() {
        if (heroes.isEmpty() || campaignId.isEmpty()) return;
        try {
            Files.createDirectories(saveDir);
            Files.writeString(saveDir.resolve("autosave.json"), Json.pretty(snapshot()), StandardCharsets.UTF_8);
        } catch (IOException e) { System.err.println("Autosave failed: " + e.getMessage()); }
    }

    public synchronized String save(String name) {
        String safe = name == null ? "save" : name.replaceAll("[^A-Za-z0-9_-]", "_");
        if (safe.isBlank()) safe = "save";
        try {
            Files.createDirectories(saveDir);
            Files.writeString(saveDir.resolve(safe + ".json"), Json.pretty(snapshot()), StandardCharsets.UTF_8);
            log("💾 Game saved as \"" + safe + "\".", "info");
            return null;
        } catch (IOException e) { return e.getMessage(); }
    }

    public List<String> saves() {
        List<String> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(saveDir, "*.json")) { for (Path p : ds) out.add(p.getFileName().toString().replace(".json", "")); }
        catch (IOException ignored) {}
        Collections.sort(out);
        return out;
    }

    public synchronized String load(String name) {
        try {
            Map<String, Object> m = Json.parseObject(Files.readString(saveDir.resolve(name.replaceAll("[^A-Za-z0-9_-]", "_") + ".json")));
            return restore(m);
        } catch (Exception e) { return "Couldn't load: " + e.getMessage(); }
    }

    @SuppressWarnings("unchecked")
    public synchronized String restore(Map<String, Object> m) {
        if (registry.get(Json.str(m, "campaign", "")) == null) return "That save needs campaign '" + Json.str(m, "campaign", "") + "', which isn't loaded.";
        combat = null; pending.clear(); check = null; votes.clear(); outcome = null;
        campaignId = Json.str(m, "campaign", "");
        sceneCampaign = Json.str(m, "sceneCampaign", campaignId);
        sceneId = Json.str(m, "scene", "");
        returnStack.clear();
        for (Object o : Json.list(m, "returnStack")) if (o instanceof List<?> l && l.size() == 2) returnStack.add(new String[]{String.valueOf(l.get(0)), String.valueOf(l.get(1))});
        List<Object> cp = Json.list(m, "checkpoint");
        checkpoint = cp.size() == 2 ? new String[]{String.valueOf(cp.get(0)), String.valueOf(cp.get(1))} : null;
        flags.clear(); flags.addAll(Json.strings(m, "flags"));
        visited.clear(); visited.addAll(Json.strings(m, "visited"));
        chosenOnce.clear(); chosenOnce.addAll(Json.strings(m, "once"));
        counters.clear(); attitudes.clear();
        Map<String, Object> cs = Json.map(m, "counters"); if (cs != null) cs.forEach((k, v) -> counters.put(k, ((Number) v).intValue()));
        Map<String, Object> at = Json.map(m, "attitudes"); if (at != null) at.forEach((k, v) -> attitudes.put(k, ((Number) v).intValue()));
        heroes.clear();
        for (Map<String, Object> hm : Json.maps(m, "heroes")) {
            Hero h = Hero.load(hm);
            h.player = Json.str(hm, "player", "");
            h.npc = Json.bool(hm, "npc", false);
            if (h.npc) h.player = "npc";
            heroes.add(h);
        }
        // players re-claim their heroes by id; unknown owners are handed to the host
        for (Map<String, Object> pm : Json.maps(m, "players")) {
            String id = Json.str(pm, "id", "");
            if (!players.containsKey(id)) {
                Player p = new Player(); p.id = id; p.name = Json.str(pm, "name", "Player"); p.color = Json.str(pm, "color", "#e0b050");
                players.put(id, p);
            }
        }
        // playing alone (e.g. "Continue your last adventure" from a new browser): take up every hero nobody is connected to
        List<Player> here = players.values().stream().filter(p -> p.connections > 0).toList();
        if (here.size() == 1) for (Hero h : heroes) {
            Player o = players.get(h.player);
            if (!h.npc && (o == null || o.connections == 0)) h.player = here.get(0).id;
        }
        battlesWon = Json.integer(m, "battlesWon", 0);
        startedAt = (long) Json.dbl(m, "startedAt", System.currentTimeMillis());
        phase = Json.str(m, "phase", "story");
        log("📂 Game loaded. The tale resumes…", "title");
        changed();
        return null;
    }

    // ===================================================================================== state for clients
    synchronized Map<String, Object> state() {
        Campaign c = registry.get(campaignId);
        Campaign.Scene s = scene();
        List<Object> ps = new ArrayList<>();
        for (Player p : players.values()) ps.add(p.json());
        List<Object> pend = new ArrayList<>();
        for (PendingRoll p : pending) {
            Player pl = players.get(p.playerId);
            pend.add(Json.obj("hero", p.heroId, "player", p.playerId, "playerName", pl == null ? "Companion" : pl.name, "label", p.label,
                    "bonus", p.bonus, "adv", p.adv, "done", p.result != null, "total", p.result == null ? null : p.result.total,
                    "secondsLeft", Math.max(0, (p.deadline - System.currentTimeMillis()) / 1000)));
        }
        Map<String, Object> sc = null;
        if (s != null) {
            Campaign sceneOwner = registry.get(sceneCampaign);
            sc = Json.obj("id", s.id, "title", s.title, "chapter", s.chapter, "backdrop", s.backdrop, "weather", s.weather, "time", s.time,
                    "music", s.music, "text", fill(s.text, null), "npc", s.npc, "choices", phase.equals("story") ? visibleChoices() : List.of(),
                    "shop", s.shop, "ambience", s.ambience, "asides", s.asides, "campaign", sceneCampaign, "campaignTitle", sceneOwner == null ? "" : sceneOwner.title,
                    "author", sceneOwner == null ? "" : sceneOwner.author);
        }
        List<Object> stock = new ArrayList<>();
        if (s != null && s.shop) for (String id : DEFAULT_STOCK) stock.add(Items.item(id));
        List<Object> camps = new ArrayList<>();
        for (Campaign k : registry.all()) {
            Map<String, Object> sum = k.summary();
            List<Object> pg = new ArrayList<>();
            for (Hero h : k.pregens) if (!h.npc) pg.add(Json.obj("id", h.id, "name", h.name, "summary", h.species().name + " " + h.classDef().name, "motto", h.motto, "color", h.color, "emblem", h.emblem));
            sum.put("pregenList", pg);
            camps.add(sum);
        }
        List<Map<String, Object>> tail = new ArrayList<>(log.subList(Math.max(0, log.size() - 150), log.size()));
        return Json.obj("phase", phase,
                "campaign", c == null ? null : Json.obj("id", c.id, "title", c.title, "author", c.author),
                "scene", sc, "outcome", outcome, "players", ps, "heroes", heroes, "pending", pend,
                "voteSeconds", voteDeadline == 0 ? 0 : Math.max(0, (voteDeadline - System.currentTimeMillis()) / 1000),
                "combat", combat == null ? null : combat.view(null), "stock", stock, "campaigns", camps, "log", tail,
                "stats", Json.obj("battlesWon", battlesWon, "battlesLost", battlesLost, "minutes", startedAt == 0 ? 0 : (System.currentTimeMillis() - startedAt) / 60000),
                "flagCount", flags.size(), "dm", dm.describe(), "dmBusy", dmBusy);
    }

    public synchronized String stateJson() { return Json.write(Json.obj("v", version, "fx", List.of(), "state", state())); }

    public void shutdown() { exec.shutdownNow(); }
}
