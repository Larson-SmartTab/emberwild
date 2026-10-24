package emberwild.rules;

import emberwild.util.Json;

import java.util.*;

/**
 * A player character built under the 2024 rules: class -> background (ability increases, skills, Origin feat)
 * -> species -> ability scores. Derived numbers (AC, saves, spell DC, ...) are always computed, never stored.
 */
public final class Hero implements Json.JsonAble {
    public String id = UUID.randomUUID().toString().substring(0, 8);
    public String name = "Hero", player = "", pronouns = "she/her";
    public String speciesId = "human", lineageId = "", classId = "fighter", backgroundId = "soldier";
    public int level = 1, xp = 0;
    /** Final ability scores (base + background increases). */
    public int[] scores = {10, 10, 10, 10, 10, 10};
    public Set<String> skills = new LinkedHashSet<>(), expertise = new LinkedHashSet<>();
    public int hp, maxHp, tempHp;
    public int hitDice;
    public List<String> cantrips = new ArrayList<>(), prepared = new ArrayList<>(), spellbook = new ArrayList<>();
    public int[] slots = new int[0], slotsMax = new int[0];
    public Map<String, Integer> resources = new LinkedHashMap<>(), resourcesMax = new LinkedHashMap<>();
    public List<String> inventory = new ArrayList<>(), attuned = new ArrayList<>();
    public int gold = 15;
    public Set<String> conditions = new LinkedHashSet<>();
    public boolean mageArmor = false, inspiration = false, dead = false, stable = false;
    public int deathSuccesses = 0, deathFailures = 0;
    public String weaponId = "", offWeaponId = "";
    // presentation
    public String color = "#e67e22", emblem = "", appearance = "", personality = "", backstory = "", motto = "";
    public String hair = "#6b3e26", skin = "#d8a47f", eyes = "#3fa34d";
    public boolean npc = false; // AI-controlled companion

    // ------------------------------------------------------------------ derived values
    public ClassDefRef cls() { return new ClassDefRef(Classes.get(classId)); }
    public Classes.ClassDef classDef() { return Classes.get(classId); }
    public Species.SpeciesDef species() { return Species.get(speciesId); }
    public Backgrounds.BackgroundDef background() { return Backgrounds.get(backgroundId); }

    public int score(Ability a) { return scores[a.ordinal()]; }
    public int mod(Ability a) { return Ability.mod(score(a)); }
    public int prof() { return 2 + (level - 1) / 4; }

    public boolean hasTrait(String t) {
        Species.SpeciesDef s = species();
        if (s == null) return false;
        if (s.traits.contains(t)) return true;
        Species.Lineage l = s.lineage(lineageId);
        return l != null && l.traits.contains(t);
    }

    public boolean hasFeature(String id) {
        Classes.ClassDef c = classDef();
        if (c == null) return false;
        for (Classes.Feature f : c.featuresUpTo(level)) if (f.id().equals(id)) return true;
        return false;
    }

    public boolean hasFeat(String feat) {
        Backgrounds.BackgroundDef b = background();
        return b != null && b.feat.equals(feat);
    }

    public int itemBonus(String key) {
        int t = 0;
        for (String id : attuned) {
            Items.Item it = Items.item(id);
            if (it != null) t += it.bonus.getOrDefault(key, 0);
        }
        return t;
    }

    public int speed() {
        Species.SpeciesDef s = species();
        int sp = s == null ? 30 : s.speed;
        Species.Lineage l = s == null ? null : s.lineage(lineageId);
        if (l != null && l.speed > 0) sp = l.speed;
        if (hasFeature("fast_movement")) sp += 10;
        return sp + itemBonus("speed");
    }

    public int ac() {
        Classes.ClassDef c = classDef();
        int dex = mod(Ability.DEX);
        int ac;
        if (c == null) ac = 10 + dex;
        else switch (c.armor) {
            case "unarmored_barbarian" -> ac = 10 + dex + mod(Ability.CON);
            case "none" -> ac = mageArmor ? 13 + dex : 10 + dex;
            default -> ac = c.armorBase + Math.min(dex, c.dexCap);
        }
        if (c != null && c.shield) ac += 2;
        if (c != null && !c.armor.equals("none") && !c.armor.equals("unarmored_barbarian")
                && (hasFeature("fighting_style") && (classId.equals("fighter") || classId.equals("paladin")))) ac += 1;
        if (hasFeature("divine_order")) ac += 1;
        return ac + itemBonus("ac");
    }

    public int initiative() {
        int i = mod(Ability.DEX) + itemBonus("initiative");
        if (hasFeat("alert")) i += prof();
        return i;
    }

    public boolean proficientSave(Ability a) {
        Classes.ClassDef c = classDef();
        if (c == null) return false;
        for (Ability s : c.saves) if (s == a) return true;
        return false;
    }

    public int saveBonus(Ability a) {
        int b = mod(a) + (proficientSave(a) ? prof() : 0) + itemBonus("save");
        if (a == Ability.CON) b += itemBonus("con_save");
        return b;
    }

    public int skillBonus(Skill s) {
        int b = mod(s.ability);
        if (expertise.contains(s.key())) b += prof() * 2;
        else if (skills.contains(s.key())) b += prof();
        else if (hasFeature("jack_of_all_trades")) b += prof() / 2;
        if (s.ability == Ability.CHA) b += itemBonus("cha_check");
        return b;
    }

    public int checkBonus(Ability a) {
        int b = mod(a);
        if (hasFeature("jack_of_all_trades")) b += prof() / 2;
        if (a == Ability.CHA) b += itemBonus("cha_check");
        return b;
    }

    public int passivePerception() { return 10 + skillBonus(Skill.PERCEPTION); }

    public Ability spellAbility() { Classes.ClassDef c = classDef(); return c == null ? null : c.spellAbility; }

    /** Species cantrips (e.g. wood elf Druidcraft, tiefling Fire Bolt) use the best mental stat. */
    public Ability castingAbilityFor(String spell) {
        Ability a = spellAbility();
        if (a != null) return a;
        Ability best = Ability.INT;
        for (Ability x : new Ability[]{Ability.WIS, Ability.CHA}) if (score(x) > score(best)) best = x;
        return best;
    }

    public int spellDC() { Ability a = spellAbility(); return 8 + prof() + (a == null ? mod(Ability.CHA) : mod(a)) + itemBonus("spell_dc"); }
    public int spellDCFor(String spell) { return 8 + prof() + mod(castingAbilityFor(spell)) + itemBonus("spell_dc"); }
    public int spellAttack() { Ability a = spellAbility(); return prof() + (a == null ? mod(Ability.CHA) : mod(a)) + itemBonus("spell_attack"); }
    public int spellAttackFor(String spell) { return prof() + mod(castingAbilityFor(spell)) + itemBonus("spell_attack"); }

    public Set<String> resistances() {
        Set<String> r = new HashSet<>(Species.resistances(speciesId, lineageId));
        for (String dt : List.of("fire", "cold", "acid", "lightning", "poison", "necrotic", "thunder")) if (itemBonus("resist_" + dt) > 0) r.add(dt);
        if (resources.containsKey("raging_now") && resources.get("raging_now") > 0) {
            r.add("bludgeoning"); r.add("piercing"); r.add("slashing");
        }
        return r;
    }

    public Items.Weapon weapon() {
        Items.Weapon w = Items.weapon(weaponId);
        return w == null ? Items.weapon("unarmed") : w;
    }

    /** Attack ability for a weapon (finesse picks the better of STR/DEX). */
    public Ability weaponAbility(Items.Weapon w) {
        if (w.ranged) return Ability.DEX;
        if (w.finesse) return mod(Ability.DEX) >= mod(Ability.STR) ? Ability.DEX : Ability.STR;
        return Ability.STR;
    }

    public int weaponAttack(Items.Weapon w) {
        int b = mod(weaponAbility(w)) + prof() + itemBonus("attack");
        if (w.ranged && hasFeature("fighting_style") && classId.equals("ranger")) b += 2;
        return b;
    }

    public int weaponDamageMod(Items.Weapon w) { return mod(weaponAbility(w)) + itemBonus("damage"); }

    public boolean canCast(String spellId) {
        return cantrips.contains(spellId) || prepared.contains(spellId) || alwaysPrepared().contains(spellId);
    }

    public List<String> alwaysPrepared() {
        Classes.ClassDef c = classDef();
        return c == null ? List.of() : c.alwaysPreparedAt(level);
    }

    public List<String> allSpells() {
        List<String> out = new ArrayList<>(cantrips);
        for (String s : alwaysPrepared()) if (!out.contains(s)) out.add(s);
        for (String s : prepared) if (!out.contains(s)) out.add(s);
        return out;
    }

    /** Lowest slot level >= min that still has a slot, or 0. */
    public int lowestSlot(int min) {
        for (int i = Math.max(1, min); i <= slots.length; i++) if (slots[i - 1] > 0) return i;
        return 0;
    }

    public boolean spendSlot(int lvl) {
        if (lvl < 1 || lvl > slots.length || slots[lvl - 1] <= 0) return false;
        slots[lvl - 1]--;
        return true;
    }

    public boolean down() { return hp <= 0; }

    public int res(String k) { return resources.getOrDefault(k, 0); }

    public boolean spend(String k) {
        int v = res(k);
        if (v <= 0) return false;
        resources.put(k, v - 1);
        return true;
    }

    // ------------------------------------------------------------------ rest, healing, levels
    public int heal(int amt) {
        if (dead || amt <= 0) return 0;
        int before = Math.max(0, hp);
        hp = Math.min(maxHp, Math.max(0, hp) + amt);
        if (hp > 0) { stable = false; deathSuccesses = 0; deathFailures = 0; conditions.remove("unconscious"); }
        return hp - before;
    }

    public void longRest() {
        if (dead) { dead = false; } // story revival by the temple of Harrowmere (keeps the table together)
        recompute(false);
        hp = maxHp;
        tempHp = 0;
        hitDice = level;
        slots = slotsMax.clone();
        resources.putAll(resourcesMax);
        conditions.clear();
        deathSuccesses = deathFailures = 0;
        stable = false;
        if (hasTrait("resourceful")) inspiration = true;
        if (classId.equals("wizard") && spellbook.contains("mage_armor") && prepared.contains("mage_armor") && slots.length > 0 && slots[0] > 0) {
            // A wizard who knows Mage Armor casts it every morning — it lasts 8 hours.
            slots[0]--;
            mageArmor = true;
        }
    }

    public String shortRest() {
        StringBuilder sb = new StringBuilder();
        if (hitDice > 0 && hp < maxHp) {
            int healed = 0;
            Classes.ClassDef c = classDef();
            while (hitDice > 0 && hp < maxHp - 2) {
                hitDice--;
                int r = Dice.die(c.hitDie) + mod(Ability.CON);
                healed += heal(Math.max(1, r));
            }
            if (healed > 0) sb.append(name).append(" spends Hit Dice and recovers ").append(healed).append(" HP. ");
        }
        if (hasFeature("arcane_recovery") && res("arcane_recovery") > 0) {
            int budget = (level + 1) / 2;
            int rec = 0;
            for (int i = Math.min(slots.length, 5); i >= 1 && budget > 0; i--) {
                while (budget >= i && slots[i - 1] < slotsMax[i - 1]) { slots[i - 1]++; budget -= i; rec++; }
            }
            if (rec > 0) { spend("arcane_recovery"); sb.append(name).append(" uses Arcane Recovery to regain ").append(rec).append(" spell slot(s). "); }
        }
        // short-rest resources
        for (String k : List.of("second_wind", "channel_divinity", "action_surge", "adrenaline_rush")) {
            if (resourcesMax.containsKey(k)) {
                if (k.equals("second_wind")) resources.put(k, Math.min(resourcesMax.get(k), res(k) + 1));
                else resources.put(k, resourcesMax.get(k));
            }
        }
        if (classId.equals("warlock")) slots = slotsMax.clone();
        return sb.toString().trim();
    }

    public static final int MAX_LEVEL = 5;
    public static int xpForLevel(int lvl) { return switch (lvl) { case 2 -> 300; case 3 -> 900; case 4 -> 2700; case 5 -> 6500; default -> 0; }; }

    /** Recalculate max HP, slots and resources for the current level. Keeps current HP proportional. */
    public void recompute(boolean keepCurrent) {
        Classes.ClassDef c = classDef();
        if (c == null) return;
        int con = mod(Ability.CON);
        int max = c.hitDie + con + (level - 1) * (c.hitDie / 2 + 1 + con);
        if (hasTrait("dwarven_toughness")) max += level;
        if (hasFeat("tough")) max += 2 * level;
        max += itemBonus("hp_per_level") * level;
        max = Math.max(1, max);
        int diff = max - maxHp;
        maxHp = max;
        if (keepCurrent) hp = Math.max(hp, 0) + Math.max(0, diff);
        hp = Math.min(hp, maxHp);

        int[] newMax = c.caster() ? c.slotsAt(level).clone() : new int[0];
        int[] ns = new int[newMax.length];
        for (int i = 0; i < ns.length; i++) {
            int had = i < slots.length ? slots[i] : 0, hadMax = i < slotsMax.length ? slotsMax[i] : 0;
            ns[i] = Math.min(newMax[i], had + Math.max(0, newMax[i] - hadMax));
        }
        slotsMax = newMax;
        slots = ns;

        Map<String, Integer> rm = new LinkedHashMap<>();
        switch (classId) {
            case "barbarian" -> rm.put("rage", level >= 3 ? 3 : 2);
            case "bard" -> rm.put("bardic_inspiration", Math.max(1, mod(Ability.CHA)));
            case "cleric" -> { if (level >= 2) rm.put("channel_divinity", 2); }
            case "fighter" -> { rm.put("second_wind", 2); if (level >= 2) rm.put("action_surge", 1); }
            case "paladin" -> { rm.put("lay_on_hands", 5 * level); if (level >= 2) rm.put("free_smite", 1); if (level >= 3) rm.put("channel_divinity", 1); }
            case "ranger" -> rm.put("free_mark", 2);
            case "wizard" -> rm.put("arcane_recovery", 1);
            default -> {}
        }
        if (hasTrait("breath_weapon")) rm.put("breath_weapon", prof());
        if (hasTrait("stones_endurance")) rm.put("stones_endurance", prof());
        if (hasTrait("relentless_endurance")) rm.put("relentless_endurance", 1);
        if (hasTrait("adrenaline_rush")) rm.put("adrenaline_rush", prof());
        if (hasTrait("healing_hands")) rm.put("healing_hands", 1);
        if (hasFeat("lucky")) rm.put("luck", prof());
        if (hasFeat("magic_initiate_wizard")) rm.put("free_magic_missile", 1);
        if (hasFeat("magic_initiate_cleric")) rm.put("free_healing_word", 1);
        rm.put("potion_belt", 0);
        for (var e : rm.entrySet()) {
            int prevMax = resourcesMax.getOrDefault(e.getKey(), 0);
            int cur = resources.getOrDefault(e.getKey(), 0);
            resources.put(e.getKey(), Math.min(e.getValue(), cur + Math.max(0, e.getValue() - prevMax)));
        }
        resourcesMax.clear();
        resourcesMax.putAll(rm);
        resources.keySet().removeIf(k -> !rm.containsKey(k) && !k.equals("raging_now"));
    }

    /** Level up: more HP, slots, prepared spells and features. Returns a summary of what was gained. */
    public List<String> levelUp(int to) {
        List<String> gained = new ArrayList<>();
        if (to <= level || to > 5) return gained;
        Classes.ClassDef c = classDef();
        int from = level;
        level = to;
        if (from < 4 && to >= 4) {
            // Ability Score Improvement: +2 primary (cap 20); spill over to Constitution
            Ability pa = c.primary;
            if (c.spellAbility != null) pa = c.spellAbility;
            int before = scores[pa.ordinal()];
            scores[pa.ordinal()] = Math.min(20, before + 2);
            int left = 2 - (scores[pa.ordinal()] - before);
            if (left > 0) scores[Ability.CON.ordinal()] = Math.min(20, scores[Ability.CON.ordinal()] + left);
            gained.add("+2 " + pa.full);
        }
        recompute(true);
        for (Classes.Feature f : c.features) if (f.level() > from && f.level() <= to) gained.add(f.name());
        if (c.caster()) {
            // auto-prepare new spells from the class defaults so nobody stalls the table mid-session
            int want = c.preparedAt(level);
            prepared.removeIf(x -> alwaysPrepared().contains(x)); // domain/oath spells don't use up prepared picks
            List<String> prefs = new ArrayList<>();
            if (level >= 5) {
                switch (classId) {
                    case "wizard" -> prefs.addAll(List.of("fireball", "lightning_bolt"));
                    case "cleric" -> prefs.addAll(List.of("spirit_guardians", "mass_healing_word", "revivify"));
                    case "bard" -> prefs.addAll(List.of("hypnotic_pattern", "mass_healing_word"));
                    case "paladin" -> prefs.addAll(List.of("aid"));
                    case "ranger" -> prefs.addAll(List.of("lesser_restoration"));
                    default -> {}
                }
            }
            if (level >= 3) {
                // Signature 2nd-level picks per class, then the class defaults.
                switch (classId) {
                    case "wizard" -> prefs.addAll(List.of("scorching_ray", "misty_step"));
                    case "cleric" -> prefs.addAll(List.of("spiritual_weapon"));
                    case "bard" -> prefs.addAll(List.of("shatter"));
                    default -> {}
                }
            }
            prefs.addAll(c.defaultSpells);
            List<String> added = new ArrayList<>();
            for (String s : prefs) {
                Spells.SpellDef sd = Spells.get(s);
                if (sd == null || sd.level > c.maxSpellLevel(level) || prepared.contains(s) || alwaysPrepared().contains(s)) continue;
                boolean signature = (level >= 3 && sd.level == 2 && level < 5) || (level >= 5 && sd.level == 3);
                if (prepared.size() >= want && !signature) continue;
                prepared.add(s);
                added.add(s);
                if (classId.equals("wizard") && !spellbook.contains(s)) spellbook.add(s);
            }
            // Over the limit? Retire the least essential 1st-level spells (never Shield, Mage Armor or Charm Person).
            Set<String> keep = Set.of("shield", "mage_armor", "charm_person", "chromatic_orb", "healing_word", "cure_wounds", "bless", "guiding_bolt", "hunters_mark", "scorching_ray", "misty_step", "spiritual_weapon");
            for (int i = 0; i < prepared.size() && prepared.size() > want; ) {
                String s = prepared.get(i);
                Spells.SpellDef sd = Spells.get(s);
                if (sd != null && sd.level == 1 && !keep.contains(s)) { prepared.remove(i); added.remove(s); }
                else i++;
            }
            for (String s : added) gained.add("Spell: " + Spells.get(s).name);
            if (classId.equals("wizard") && level >= 3 && !spellbook.contains("shatter")) spellbook.add("shatter");
            if (classId.equals("wizard") && level >= 5) for (String x : List.of("fireball", "lightning_bolt")) if (!spellbook.contains(x)) spellbook.add(x);
            for (String cn : c.defaultCantrips) if (cantrips.stream().filter(x -> { Spells.SpellDef d = Spells.get(x); return d != null && d.lists.contains(c.spellList); }).count() < c.cantripsAt(level) && !cantrips.contains(cn)) { cantrips.add(cn); gained.add("Cantrip: " + Spells.get(cn).name); }
            for (Spells.SpellDef extra : Spells.forList(c.spellList, 0)) if (cantrips.stream().filter(x -> { Spells.SpellDef d = Spells.get(x); return d != null && d.lists.contains(c.spellList); }).count() < c.cantripsAt(level) && !cantrips.contains(extra.id) && extra.kind != Spells.Kind.UTILITY) { cantrips.add(extra.id); gained.add("Cantrip: " + extra.name); }
        }
        if (classId.equals("bard") && level >= 2) { expertise.add("persuasion"); expertise.add("performance"); skills.add("persuasion"); skills.add("performance"); }
        if (classId.equals("wizard") && level >= 2 && skills.contains("arcana")) expertise.add("arcana");
        if (classId.equals("ranger") && level >= 2) { skills.add("survival"); expertise.add("survival"); }
        gained.add("Hit Points: " + maxHp);
        return gained;
    }

    // ------------------------------------------------------------------ construction & validation
    /**
     * Build a hero from a builder payload. base scores must be legal (point buy, standard array, or 3..18 rolled);
     * background increases (+2/+1 or +1/+1/+1) must target that background's abilities.
     */
    @SuppressWarnings("unchecked")
    public static Hero fromSpec(Map<String, Object> m, List<String> errors) {
        Hero h = new Hero();
        h.name = clip(Json.str(m, "name", "Nameless"), 40);
        if (h.name.isBlank()) h.name = "Nameless";
        h.player = clip(Json.str(m, "player", ""), 40);
        h.pronouns = clip(Json.str(m, "pronouns", "they/them"), 20);
        h.classId = Json.str(m, "class", "fighter").toLowerCase(Locale.ROOT);
        h.speciesId = Json.str(m, "species", "human").toLowerCase(Locale.ROOT);
        h.lineageId = Json.str(m, "lineage", "");
        h.backgroundId = Json.str(m, "background", "soldier").toLowerCase(Locale.ROOT);
        Classes.ClassDef c = Classes.get(h.classId);
        Species.SpeciesDef s = Species.get(h.speciesId);
        Backgrounds.BackgroundDef b = Backgrounds.get(h.backgroundId);
        if (c == null) { errors.add("Unknown class '" + h.classId + "'"); c = Classes.get("fighter"); h.classId = "fighter"; }
        if (s == null) { errors.add("Unknown species '" + h.speciesId + "'"); s = Species.get("human"); h.speciesId = "human"; }
        if (b == null) { errors.add("Unknown background '" + h.backgroundId + "'"); b = Backgrounds.get("soldier"); h.backgroundId = "soldier"; }
        Species.Lineage lin = s.lineage(h.lineageId);
        h.lineageId = lin == null ? "" : lin.id;

        Map<String, Object> base = Json.map(m, "scores");
        int[] sc = new int[6];
        for (Ability a : Ability.values()) sc[a.ordinal()] = base == null ? 10 : Json.integer(base, a.key(), 10);
        String method = Json.str(m, "method", "pointbuy");
        if (method.equals("pointbuy")) {
            int cost = 0;
            for (int v : sc) {
                if (v < 8 || v > 15) { errors.add("Point buy scores must be 8-15"); break; }
                cost += v <= 13 ? v - 8 : (v == 14 ? 7 : 9);
            }
            if (cost > 27) errors.add("Point buy total is " + cost + " (max 27)");
        } else if (method.equals("standard")) {
            int[] sorted = sc.clone(); Arrays.sort(sorted);
            if (!Arrays.equals(sorted, new int[]{8, 10, 12, 13, 14, 15})) errors.add("Standard array must use 15, 14, 13, 12, 10, 8");
        } else {
            for (int v : sc) if (v < 3 || v > 18) { errors.add("Rolled scores must be 3-18"); break; }
        }
        Map<String, Object> inc = Json.map(m, "increases");
        int total = 0;
        if (inc != null) {
            for (var e : inc.entrySet()) {
                Ability a = Ability.of(e.getKey());
                int v = e.getValue() instanceof Number n ? n.intValue() : 0;
                if (a == null || v <= 0) continue;
                boolean allowed = false;
                for (Ability x : b.abilities) if (x == a) allowed = true;
                if (!allowed) { errors.add(b.name + " can't increase " + a.full); continue; }
                if (v > 2) { errors.add("A single increase can't exceed +2"); v = 2; }
                sc[a.ordinal()] += v;
                total += v;
            }
        }
        if (total > 3) errors.add("Background increases total more than +3");
        if (total < 3) { // auto-apply +2 primary / +1 next so nobody is short-changed
            Ability first = b.abilities[0], second = b.abilities[1];
            for (Ability x : b.abilities) if (x == c.primary) first = x;
            for (Ability x : b.abilities) if (x != first) { second = x; break; }
            int need = 3 - total;
            if (need >= 2 && sc[first.ordinal()] < 20) { sc[first.ordinal()] += 2; need -= 2; }
            if (need >= 1) sc[second.ordinal()] += 1;
        }
        for (int i = 0; i < 6; i++) sc[i] = Math.min(20, sc[i]);
        h.scores = sc;

        // skills: background + species + class choices (+ Skilled feat choices)
        for (Skill k : b.skills) h.skills.add(k.key());
        for (Skill k : s.skills) h.skills.add(k.key());
        List<String> chosen = Json.strings(m, "skills");
        int classPicks = 0, freePicks = 0, freeAllowed = b.feat.equals("skilled") ? 3 : 0;
        for (String k : chosen) {
            Skill sk = Skill.of(k);
            if (sk == null || h.skills.contains(sk.key())) continue;
            if (classPicks < c.skillCount && c.skillChoices.contains(sk)) { h.skills.add(sk.key()); classPicks++; }
            else if (freePicks < freeAllowed) { h.skills.add(sk.key()); freePicks++; }
        }
        for (Skill sk : c.skillChoices) { // fill any remaining class picks
            if (classPicks >= c.skillCount) break;
            if (h.skills.add(sk.key())) classPicks++;
        }
        if (h.classId.equals("rogue")) { h.expertise.add("stealth"); h.expertise.add("sleight_of_hand"); h.skills.add("stealth"); h.skills.add("sleight_of_hand"); }

        // spells
        if (c.caster()) {
            List<String> cs = Json.strings(m, "cantrips"), ps = Json.strings(m, "spells");
            for (String id : cs) {
                Spells.SpellDef sd = Spells.get(id);
                if (sd != null && sd.isCantrip() && sd.lists.contains(c.spellList) && h.cantrips.size() < c.cantripsAt(1) && !h.cantrips.contains(id)) h.cantrips.add(id);
            }
            for (String id : c.defaultCantrips) if (h.cantrips.size() < c.cantripsAt(1) && !h.cantrips.contains(id)) h.cantrips.add(id);
            for (String id : ps) {
                Spells.SpellDef sd = Spells.get(id);
                if (sd != null && sd.level == 1 && sd.lists.contains(c.spellList) && h.prepared.size() < c.preparedAt(1) && !h.prepared.contains(id)) h.prepared.add(id);
            }
            for (String id : c.defaultSpells) {
                Spells.SpellDef sd = Spells.get(id);
                if (h.prepared.size() < c.preparedAt(1) && sd != null && sd.level == 1 && !h.prepared.contains(id) && !c.alwaysPreparedAt(1).contains(id)) h.prepared.add(id);
            }
            if (h.classId.equals("wizard")) {
                h.spellbook.addAll(h.prepared);
                for (String id : c.defaultSpells) if (h.spellbook.size() < 6 && !h.spellbook.contains(id)) h.spellbook.add(id);
            }
        }
        if (lin != null) for (String cn : lin.cantrips) if (!h.cantrips.contains(cn)) h.cantrips.add(cn);
        if (b.feat.equals("magic_initiate_wizard")) for (String cn : List.of("fire_bolt", "prestidigitation")) if (!h.cantrips.contains(cn)) h.cantrips.add(cn);
        if (b.feat.equals("magic_initiate_cleric")) for (String cn : List.of("sacred_flame", "guidance")) if (!h.cantrips.contains(cn)) h.cantrips.add(cn);

        // gear
        h.weaponId = c.weapons.isEmpty() ? "unarmed" : c.weapons.get(0);
        h.offWeaponId = c.weapons.size() > 1 ? c.weapons.get(1) : "";
        h.inventory.add("potion_healing");
        if (h.classId.equals("rogue")) h.inventory.add("thieves_tools");
        h.inventory.add("rope");
        h.inventory.add("torch");
        h.gold = b.gold + 10;

        // presentation
        h.color = safeColor(Json.str(m, "color", c.color), c.color);
        h.hair = safeColor(Json.str(m, "hair", "#6b3e26"), "#6b3e26");
        h.skin = safeColor(Json.str(m, "skin", "#d8a47f"), "#d8a47f");
        h.eyes = safeColor(Json.str(m, "eyes", "#3fa34d"), "#3fa34d");
        h.emblem = clip(Json.str(m, "emblem", c.glyph), 4);
        h.appearance = clip(Json.str(m, "appearance", ""), 600);
        h.personality = clip(Json.str(m, "personality", ""), 600);
        h.backstory = clip(Json.str(m, "backstory", ""), 2000);
        h.motto = clip(Json.str(m, "motto", ""), 140);

        h.recompute(false);
        h.longRest();
        if (h.classId.equals("wizard") && h.prepared.contains("mage_armor")) {
            h.mageArmor = true; // cast at dawn
        }
        return h;
    }

    private static String clip(String s, int n) { return s == null ? "" : (s.length() > n ? s.substring(0, n) : s); }

    private static String safeColor(String s, String def) { return s != null && s.matches("#[0-9a-fA-F]{6}") ? s : def; }

    // ------------------------------------------------------------------ serialisation
    @Override public Object toJson() {
        Classes.ClassDef c = classDef();
        Species.SpeciesDef s = species();
        Backgrounds.BackgroundDef b = background();
        Species.Lineage lin = s == null ? null : s.lineage(lineageId);
        Map<String, Object> sc = new LinkedHashMap<>(), mods = new LinkedHashMap<>(), saves = new LinkedHashMap<>();
        for (Ability a : Ability.values()) { sc.put(a.key(), score(a)); mods.put(a.key(), mod(a)); saves.put(a.key(), saveBonus(a)); }
        Map<String, Object> sk = new LinkedHashMap<>();
        for (Skill k : Skill.values()) sk.put(k.key(), Json.obj("bonus", skillBonus(k), "prof", skills.contains(k.key()), "expert", expertise.contains(k.key()), "label", k.label, "ability", k.ability.key()));
        List<Object> spells = new ArrayList<>();
        for (String id : allSpells()) { Spells.SpellDef sd = Spells.get(id); if (sd != null) spells.add(sd); }
        List<Object> inv = new ArrayList<>();
        for (String id : inventory) { Items.Item it = Items.item(id); if (it != null) inv.add(it); }
        List<Object> feats = new ArrayList<>();
        if (c != null) for (Classes.Feature f : c.featuresUpTo(level)) feats.add(Json.obj("name", f.name(), "desc", f.desc()));
        if (b != null) { Backgrounds.Feat ft = Backgrounds.FEATS.get(b.feat); if (ft != null) feats.add(Json.obj("name", ft.name() + " (Origin feat)", "desc", ft.desc())); }
        List<Object> traits = new ArrayList<>();
        if (s != null) for (String[] t : s.traitText) traits.add(Json.obj("name", t[0], "text", t[1]));
        Items.Weapon w = weapon();
        return Json.obj(
                "id", id, "name", name, "player", player, "pronouns", pronouns, "npc", npc,
                "species", speciesId, "speciesName", s == null ? speciesId : s.name,
                "lineage", lineageId, "lineageName", lin == null ? "" : lin.name,
                "class", classId, "className", c == null ? classId : c.name, "subclass", c != null && level >= 3 ? c.subclass : "",
                "background", backgroundId, "backgroundName", b == null ? backgroundId : b.name,
                "level", level, "xp", xp, "nextXp", xpForLevel(level + 1), "prof", prof(),
                "scores", sc, "mods", mods, "saves", saves, "skills", sk,
                "hp", hp, "maxHp", maxHp, "tempHp", tempHp, "hitDice", hitDice,
                "ac", ac(), "speed", speed(), "initiative", initiative(), "passivePerception", passivePerception(),
                "spellDC", c != null && c.caster() ? spellDC() : 0, "spellAttack", c != null && c.caster() ? spellAttack() : 0,
                "slots", slots, "slotsMax", slotsMax, "spells", spells, "spellbook", spellbook, "prepared", prepared, "cantrips", cantrips,
                "resources", resources, "resourcesMax", resourcesMax,
                "weapon", w, "weaponAttack", weaponAttack(w), "weaponDamage", w.dice + (weaponDamageMod(w) >= 0 ? "+" : "") + weaponDamageMod(w),
                "inventory", inv, "attuned", attuned, "gold", gold, "conditions", conditions,
                "mageArmor", mageArmor, "inspiration", inspiration, "dead", dead, "stable", stable,
                "deathSuccesses", deathSuccesses, "deathFailures", deathFailures,
                "features", feats, "traits", traits,
                "color", color, "emblem", emblem, "hair", hair, "skin", skin, "eyes", eyes,
                "appearance", appearance, "personality", personality, "backstory", backstory, "motto", motto);
    }

    /** Persistence format (raw fields, used for saves and exported character files). */
    public Map<String, Object> save() {
        Map<String, Object> sc = new LinkedHashMap<>();
        for (Ability a : Ability.values()) sc.put(a.key(), score(a));
        return Json.obj("id", id, "name", name, "player", player, "pronouns", pronouns, "species", speciesId, "lineage", lineageId,
                "class", classId, "background", backgroundId, "level", level, "xp", xp, "finalScores", sc,
                "skillList", new ArrayList<>(skills), "expertise", new ArrayList<>(expertise), "hp", hp, "tempHp", tempHp, "hitDice", hitDice,
                "cantrips", cantrips, "prepared", prepared, "spellbook", spellbook, "slotsLeft", slots,
                "resources", resources, "inventory", inventory, "attuned", attuned, "gold", gold,
                "conditions", new ArrayList<>(conditions), "mageArmor", mageArmor, "inspiration", inspiration, "dead", dead,
                "weapon", weaponId, "offWeapon", offWeaponId, "npc", npc,
                "color", color, "emblem", emblem, "hair", hair, "skin", skin, "eyes", eyes,
                "appearance", appearance, "personality", personality, "backstory", backstory, "motto", motto);
    }

    public static Hero load(Map<String, Object> m) {
        Hero h = new Hero();
        h.id = Json.str(m, "id", h.id);
        h.name = clip(Json.str(m, "name", "Hero"), 40);
        h.player = clip(Json.str(m, "player", ""), 40);
        h.pronouns = Json.str(m, "pronouns", "they/them");
        h.speciesId = Json.str(m, "species", "human");
        h.lineageId = Json.str(m, "lineage", "");
        h.classId = Json.str(m, "class", "fighter");
        h.backgroundId = Json.str(m, "background", "soldier");
        if (Classes.get(h.classId) == null) h.classId = "fighter";
        if (Species.get(h.speciesId) == null) h.speciesId = "human";
        if (Backgrounds.get(h.backgroundId) == null) h.backgroundId = "soldier";
        h.level = Math.max(1, Math.min(MAX_LEVEL, Json.integer(m, "level", 1)));
        h.xp = Json.integer(m, "xp", 0);
        Map<String, Object> sc = Json.map(m, "finalScores");
        for (Ability a : Ability.values()) h.scores[a.ordinal()] = Math.max(1, Math.min(20, Json.integer(sc, a.key(), 10)));
        h.skills.addAll(Json.strings(m, "skillList"));
        h.expertise.addAll(Json.strings(m, "expertise"));
        h.cantrips.addAll(Json.strings(m, "cantrips"));
        h.prepared.addAll(Json.strings(m, "prepared"));
        h.spellbook.addAll(Json.strings(m, "spellbook"));
        h.inventory.addAll(Json.strings(m, "inventory"));
        h.attuned.addAll(Json.strings(m, "attuned"));
        h.gold = Json.integer(m, "gold", 0);
        h.weaponId = Json.str(m, "weapon", "dagger");
        h.offWeaponId = Json.str(m, "offWeapon", "");
        h.npc = Json.bool(m, "npc", false);
        h.color = safeColor(Json.str(m, "color", "#e67e22"), "#e67e22");
        h.emblem = clip(Json.str(m, "emblem", ""), 4);
        h.hair = safeColor(Json.str(m, "hair", "#6b3e26"), "#6b3e26");
        h.skin = safeColor(Json.str(m, "skin", "#d8a47f"), "#d8a47f");
        h.eyes = safeColor(Json.str(m, "eyes", "#3fa34d"), "#3fa34d");
        h.appearance = Json.str(m, "appearance", "");
        h.personality = Json.str(m, "personality", "");
        h.backstory = Json.str(m, "backstory", "");
        h.motto = Json.str(m, "motto", "");
        h.recompute(false);
        h.longRest();
        h.hp = Math.min(h.maxHp, Json.integer(m, "hp", h.maxHp));
        h.tempHp = Json.integer(m, "tempHp", 0);
        h.hitDice = Json.integer(m, "hitDice", h.level);
        List<Object> sl = Json.list(m, "slotsLeft");
        for (int i = 0; i < h.slots.length && i < sl.size(); i++) if (sl.get(i) instanceof Number n) h.slots[i] = Math.min(h.slotsMax[i], n.intValue());
        Map<String, Object> rs = Json.map(m, "resources");
        if (rs != null) for (var e : rs.entrySet()) if (e.getValue() instanceof Number n && h.resourcesMax.containsKey(e.getKey())) h.resources.put(e.getKey(), n.intValue());
        h.conditions.clear();
        h.conditions.addAll(Json.strings(m, "conditions"));
        h.mageArmor = Json.bool(m, "mageArmor", false);
        h.inspiration = Json.bool(m, "inspiration", false);
        h.dead = Json.bool(m, "dead", false);
        return h;
    }

    /** Helper wrapper (keeps a nullable class reference readable at call sites). */
    public record ClassDefRef(Classes.ClassDef def) {}
}
