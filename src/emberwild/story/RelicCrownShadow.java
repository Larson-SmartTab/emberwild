package emberwild.story;

import emberwild.rules.Ability;
import emberwild.rules.Hero;
import emberwild.rules.Monsters;
import emberwild.story.Campaign;
import emberwild.story.CampaignScript;
import emberwild.util.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static emberwild.story.Fx.*;

/**
 * THE RELIC OF CROWN AND SHADOW — a story by Joel Hill.
 * <p>
 * Joel's second version of "The Gem", his console adventure for the team's Java challenge. Same tale (the Temple of the
 * Lost Sun, the Cursed Jungle, the Forgotten Kingdom, the Cursed King) with everything he added in the rewrite:
 * <ul>
 *   <li>Four ways across the ravine (bridge, shallow river, bribe a scout, climb the cliffs) and a fourth door symbol
 *       (the Skull).</li>
 *   <li>A treasure chest after hard fights, which might be a Treasure Mimic (a natural 1 when you open it, about the
 *       same 5% chance as Joel's), holding a potion, gold, Alchemist's Fire, a whetstone or reinforced plating.</li>
 *   <li>Two merchant camps at Joel's prices: the Chapter 1 Outskirts and the Jungle Gate Outpost.</li>
 *   <li>Demand a toll from the injured explorer, toss rocks at the biting stones or hire a tracker, search the
 *       perimeter wall, bribe the Ghost Knight with a tribute, and the hidden fourth door marked with a shadow.</li>
 *   <li>The class weapon drops are gone (as in Joel's rewrite); upgrades come from chests and merchants instead.</li>
 * </ul>
 * Joel's toll says "lose karma"; here it costs you Tomas's trust (no advantage with the Ghost Knight later), and the
 * Professor hears about it.
 * Custom ids are prefixed {@code rc_}.
 */
public final class RelicCrownShadow implements CampaignScript {

    static final String PRO = "Prologue — Sunreach";
    static final String CH1 = "Chapter 1 — Temple of the Lost Sun";
    static final String CH2 = "Chapter 2 — The Cursed Jungle";
    static final String CH3 = "Chapter 3 — The Forgotten Kingdom";
    static final String FIN = "Finale — The Cursed King";

    @Override
    public Campaign build() {
        Campaign c = Campaign.create("relic_crown_shadow", "The Relic of Crown and Shadow, a story by Joel Hill", "Joel Hill");
        c.cover("ruins");
        c.synopsis("Three hundred years ago the Sun Gem was pried from the crown of the last king of a forgotten kingdom, and "
                + "the king did not die so much as curdle. Now a professor at a jungle trading post wants her missing partner "
                + "back, and every treasure-hunter on the coast wants the gem. From the Temple of the Lost Sun, through a cursed "
                + "jungle, to a throne room where blue fire burns in a dead king's eyes, a band of heroes must decide what the "
                + "relic is worth — and what to do with a crown nobody should wear. Joel Hill's expanded second edition of The Gem, with treasure chests (and mimics), merchant camps, and new paths at every turn. Built for 1-4 players "
                + "starting at level 1 (rising to level 3); companions fill a small party.");
        c.hook("epilogue", "A rain-soaked professor is hiring brave idiots to find a relic of crown and shadow", "rc_start");

        pregens(c);
        monsters(c);
        items(c);
        prologue(c);
        chapterOne(c);
        chapterTwo(c);
        chapterThree(c);
        finale(c);
        encounters(c);
        return c;
    }

    // =========================================================================================== heroes
    static Map<String, Object> scores(int str, int dex, int con, int in, int wis, int cha) {
        return Json.obj("str", str, "dex", dex, "con", con, "int", in, "wis", wis, "cha", cha);
    }

    static Hero hero(String id, Map<String, Object> spec) {
        List<String> errs = new ArrayList<>();
        Hero h = Hero.fromSpec(spec, errs);
        if (!errs.isEmpty()) throw new IllegalStateException("Pregen " + id + ": " + errs);
        h.id = id;
        return h;
    }

    static void pregens(Campaign c) {
        // Joel's three classes: the Fighter (high HP and attack), the Rogue (nimble and skilled), the Ranger (balanced and accurate).
        c.pregen(hero("rc_garrick", Json.obj(
                "name", "Garrick Sunhallow", "pronouns", "he/him", "class", "fighter", "species", "human", "background", "soldier",
                "method", "pointbuy", "scores", scores(15, 13, 14, 8, 12, 10), "increases", Json.obj("str", 2, "con", 1),
                "skills", List.of("athletics", "perception"),
                "color", "#d4ac0d", "emblem", "☀", "hair", "#5d4037", "skin", "#d7a77f", "eyes", "#6e2c00",
                "appearance", "A broad, sun-browned swordsman with a dented breastplate he polishes every night whether it needs it or not.",
                "personality", "Steady, cheerful and very literal. Believes most locked doors are just doors that haven't been pushed hard enough.",
                "backstory", "Garrick guarded caravans along the jungle coast for ten years and never once lost a client — though he did lose a lot of caravans.",
                "motto", "If it's stuck, push. If it's still stuck, push harder.")));
        c.pregen(hero("rc_vex", Json.obj(
                "name", "Vex Nightwhistle", "pronouns", "she/her", "class", "rogue", "species", "halfling", "background", "criminal",
                "method", "pointbuy", "scores", scores(8, 15, 14, 12, 10, 13), "increases", Json.obj("dex", 2, "con", 1),
                "skills", List.of("acrobatics", "investigation", "perception", "persuasion"),
                "color", "#5b2c6f", "emblem", "🗝", "hair", "#1c2833", "skin", "#f0c9a4", "eyes", "#27ae60",
                "appearance", "A tiny halfling in a coat with forty-one pockets, every one of them locked.",
                "personality", "Whistles when she's nervous, which is never. Collects keys to doors she has never seen.",
                "backstory", "Vex once stole a treasure map from the Professor. Then she read it, felt bad, and gave it back with notes.",
                "motto", "Every lock is a riddle. Every riddle has a key.")));
        c.pregen(hero("rc_lyra", Json.obj(
                "name", "Lyra Thornshot", "pronouns", "she/her", "class", "ranger", "species", "elf", "lineage", "wood", "background", "wayfarer",
                "method", "pointbuy", "scores", scores(10, 15, 13, 10, 15, 8), "increases", Json.obj("dex", 2, "wis", 1),
                "skills", List.of("nature", "survival", "stealth"),
                "color", "#1e8449", "emblem", "🏹", "hair", "#7e5109", "skin", "#c8a27a", "eyes", "#58d68d",
                "appearance", "A lean wood elf in leaf-green leathers, a longbow taller than she is slung across her back.",
                "personality", "Quiet, sharp-eyed and dry as summer bark. Speaks to birds more than people, and they answer more often.",
                "backstory", "Lyra grew up on the edge of the Cursed Jungle, listening to the drums nobody plays. She means to find out who doesn't play them.",
                "motto", "Breathe out. Loose. Never miss.")));

        // ---- Companions: AI-controlled, they join only when the party is small.
        Hero odalys = hero("rc_odalys", Json.obj(
                "name", "Sister Odalys Brightwater", "pronouns", "she/her", "class", "cleric", "species", "human", "background", "acolyte",
                "method", "pointbuy", "scores", scores(12, 12, 14, 8, 15, 11), "increases", Json.obj("wis", 2, "cha", 1),
                "skills", List.of("medicine", "religion"),
                "cantrips", List.of("sacred_flame", "guidance", "toll_the_dead"),
                "spells", List.of("healing_word", "cure_wounds", "bless", "guiding_bolt"),
                "color", "#f7dc6f", "emblem", "🌞", "hair", "#4a2511", "skin", "#a0522d", "eyes", "#f5b041",
                "appearance", "A tall priestess of the Morning Lord in sun-bleached robes, a brass sunburst on a cord at her throat.",
                "personality", "Warm, blunt and absolutely fearless about the dead. \"They're just people who've stopped being polite.\"",
                "motto", "Dawn always comes. Sometimes it needs an escort."));
        odalys.npc = true;
        c.pregen(odalys);
        Hero pim = hero("rc_pim", Json.obj(
                "name", "Pim Fizzlecap", "pronouns", "he/him", "class", "wizard", "species", "gnome", "background", "sage",
                "method", "pointbuy", "scores", scores(8, 14, 14, 15, 12, 8), "increases", Json.obj("int", 2, "con", 1),
                "skills", List.of("arcana", "history"),
                "cantrips", List.of("fire_bolt", "ray_of_frost", "shocking_grasp"),
                "spells", List.of("mage_armor", "shield", "magic_missile", "charm_person"),
                "color", "#3498db", "emblem", "✨", "hair", "#ecf0f1", "skin", "#f5cba7", "eyes", "#2e86c1",
                "appearance", "A gnome in an explorer's pith helmet three sizes too large, with a notebook for every pocket.",
                "personality", "The Professor's apprentice. Takes notes on everything, including things that are actively trying to eat him.",
                "motto", "Fascinating! Run!"));
        pim.npc = true;
        c.pregen(pim);
    }

    // =========================================================================================== monsters & items
    static void monsters(Campaign c) {
        // ---- the dead of the temple and the jungle
        c.monster("rc_temple_skeleton", "Sun-Temple Skeleton").type("undead").role("melee").ac(13).hp(13, "2d8+4").stats(10, 14, 15, 6, 8, 5)
                .cr(0.25, 50).vulnerable("bludgeoning").immune("poison").condImmune("poisoned", "frightened")
                .attack("Corroded Khopesh", 4, "1d6+2", "slashing")
                .attack(new Monsters.Attack("Sun-Bleached Bow", 4, "1d6+2", "piercing").ranged(80, 320))
                .look("#e5e7e9", "💀").blurb("Its armour is gilded with sunbursts. Its loyalty has outlived everything else about it.");
        c.monster("rc_skeletal_guardian", "Skeletal Guardian").type("undead").role("brute").ac(14).hp(30).stats(16, 12, 15, 6, 10, 5)
                .cr(1, 200).vulnerable("bludgeoning").immune("poison").condImmune("poisoned", "frightened")
                .attack(new Monsters.Attack("Temple Halberd", 5, "1d10+3", "slashing").reach(10))
                .look("#f9e79f", "🛡").blurb("The temple's last sentry. It has waited three hundred years to say no to someone.");
        c.monster("rc_skeletal_champion", "Skeletal Champion").type("undead").role("brute").ac(14).hp(40).stats(17, 12, 16, 6, 10, 6)
                .cr(2, 450).multi(2).vulnerable("bludgeoning").immune("poison").condImmune("poisoned", "frightened", "exhaustion")
                .attack(new Monsters.Attack("Jungle-Rusted Greatsword", 5, "1d8+3", "slashing"))
                .special(Monsters.special("bone_rally", "Rally the Bones").shape("summon", 0, 0).summon("rc_temple_skeleton", 1).uses(1))
                .look("#d5dbdb", "⚔").blurb("Vines grow through its ribs. It has been champion of this clearing for longer than the trees.");
        c.monster("rc_jungle_spider", "Mist Spider").type("beast").role("skirmisher").ac(13).hp(18).speed(30).stats(14, 16, 12, 2, 11, 4)
                .cr(0.5, 100).attack(new Monsters.Attack("Bite", 5, "1d6+3", "piercing").plus("1d6", "poison").rider("poisoned", Ability.CON, 11))
                .look("#76448a", "🕷").blurb("As big as a dog and as quiet as the mist it hunts in.");

        // ---- things that follow you to a campfire (Joel's random encounter: bandit, dire wolf or skeleton)
        c.monster("rc_bandit", "Restless Bandit").type("humanoid").role("melee").ac(12).hp(16).stats(12, 14, 12, 10, 10, 10)
                .cr(0.25, 50).attack("Scimitar", 4, "1d6+2", "slashing")
                .attack(new Monsters.Attack("Light Crossbow", 4, "1d8+2", "piercing").ranged(80, 320))
                .look("#935116", "🗡").blurb("Followed your firelight through the dark. Regrets it already.");
        c.monster("rc_dire_wolf", "Hungry Dire Wolf").type("beast").role("brute").ac(13).hp(26).speed(50).stats(17, 15, 15, 3, 12, 7)
                .cr(1, 200).attack(new Monsters.Attack("Bite", 5, "1d10+3", "piercing").rider("prone", Ability.STR, 13)).trait("pack_tactics")
                .look("#566573", "🐺").blurb("Ribs like a xylophone. Teeth like a bear trap.");

        // ---- the Forgotten Kingdom
        c.monster("rc_ghost_knight", "The Ghost Knight").type("undead").role("brute").ac(16).hp(72).speed(30).stats(16, 11, 14, 10, 13, 15)
                .cr(3, 700).multi(2).resist("necrotic", "cold").immune("poison").condImmune("poisoned", "frightened", "charmed", "exhaustion")
                .attack(new Monsters.Attack("Spectral Longsword", 6, "1d10+3", "slashing").plus("1d6", "necrotic"))
                .special(Monsters.special("oath_of_the_hall", "Oath of the Hall").shape("sphere", 10, 0).dmg("2d6", "necrotic").dc(13, Ability.WIS).rider("frightened").recharge(5))
                .look("#85c1e9", "🛡").blurb("Sworn to guard the throne, even now, even from a king who no longer deserves it.");
        c.monster("rc_knight_ally", "The Ghost Knight").type("undead").role("brute").ac(16).hp(52).speed(30).stats(16, 11, 14, 10, 13, 15)
                .cr(3, 0).multi(2).resist("necrotic", "cold").immune("poison").condImmune("poisoned", "frightened", "charmed")
                .attack(new Monsters.Attack("Spectral Longsword", 5, "1d8+3", "slashing").plus("1d6", "radiant"))
                .look("#d6eaf8", "🛡").blurb("He has finally found a cause worth his oath.");
        c.monster("rc_hollow_soldier", "Hollow Soldier").type("undead").role("melee").ac(13).hp(16).stats(13, 12, 14, 6, 8, 5)
                .cr(0.25, 50).resist("necrotic").immune("poison").condImmune("poisoned").attack("Rusted Spear", 4, "1d6+2", "piercing")
                .look("#5d6d7e", "👻").blurb("A palace guard still answering a roll-call that ended three centuries ago.");
        c.monster("rc_hall_guardian", "Hall Guardian").type("construct").role("brute").size(2).ac(15).hp(60).speed(25).stats(18, 8, 16, 3, 10, 1)
                .cr(2, 450).multi(2).resist("piercing", "slashing").immune("poison", "psychic").condImmune("charmed", "frightened", "poisoned", "paralyzed")
                .attack("Marble Fist", 6, "1d10+4", "bludgeoning")
                .look("#aab7b8", "🗿").blurb("A statue of a forgotten king that has decided it would rather be a wall.");
        c.monster("rc_stone_guardian", "Royal Stone Guardian").type("construct").role("brute").size(2).ac(16).hp(56).speed(25).stats(19, 8, 17, 3, 10, 1)
                .cr(3, 700).multi(2).resist("piercing", "slashing").immune("poison", "psychic").condImmune("charmed", "frightened", "poisoned", "paralyzed")
                .attack(new Monsters.Attack("Granite Halberd", 6, "1d10+4", "slashing").reach(10))
                .special(Monsters.special("armory_quake", "Armory Quake").shape("sphere", 10, 0).dmg("2d6", "bludgeoning").dc(13, Ability.DEX).rider("prone").recharge(5))
                .look("#7f8c8d", "🗿").blurb("Twelve feet of animated granite, built to guard the kingdom's weapons — and very good at it.");
        c.monster("rc_animated_blade", "Animated Royal Blade").type("construct").role("skirmisher").ac(15).hp(12).speed(40).fly(40).stats(12, 15, 11, 1, 5, 1)
                .cr(0.5, 100).immune("poison", "psychic").condImmune("charmed", "frightened", "poisoned")
                .attack("Slash", 4, "1d8+2", "slashing").look("#d0d3d4", "🗡").blurb("A sword from the armory racks, still eager for a hand.");

        // ---- the Finale
        c.monster("rc_soulfire_brazier", "Soulfire Brazier").type("object").role("object").ac(11).hp(10).speed(0).stats(10, 1, 10, 1, 1, 1)
                .cr(0, 25).immune("poison", "psychic", "necrotic").vulnerable("bludgeoning")
                .condImmune("charmed", "frightened", "poisoned", "paralyzed", "prone", "incapacitated")
                .attack("None", 0, "0", "fire").look("#5dade2", "🔵").blurb("Blue fire that burns without heat. While it burns, the King cannot die. Knock it over!");
        c.monster("rc_cursed_king", "The Cursed King").type("undead").role("boss").ac(16).hp(72).speed(30).stats(18, 12, 16, 12, 14, 18)
                .cr(4, 1100).saves("con", "wis", "cha").resist("necrotic", "cold").immune("poison").condImmune("poisoned", "frightened", "charmed", "exhaustion")
                .multi(2)
                .attack(new Monsters.Attack("Blade of Dark Magic", 7, "1d8+3", "slashing").plus("1d6", "necrotic"))
                .attack(new Monsters.Attack("Blue Fire Bolt", 6, "2d8", "necrotic").ranged(60, 60).spell())
                .special(Monsters.special("crown_of_blue_fire", "Crown of Blue Fire").shape("cone", 15, 0).dmg("3d6", "necrotic").dc(13, Ability.CON).recharge(5))
                .special(Monsters.special("royal_command", "\"KNEEL!\"").shape("sphere", 10, 0).dmg("1d6", "psychic").dc(12, Ability.WIS).rider("prone"))
                .trait("fed_by_objects", "legendary_resistance")
                .legendary(2, "special:royal_command", "attack:Blade of Dark Magic", "move")
                .look("#1f618d", "👑").blurb("The last king of the Forgotten Kingdom. His crown has an empty setting where the Sun Gem should be.");
        c.monster("rc_mimic", "Treasure Mimic").type("monstrosity").role("brute").ac(13).hp(32).speed(15).stats(17, 12, 15, 5, 13, 8)
                .cr(1, 200).immune("acid").condImmune("prone")
                .attack(new Monsters.Attack("Bite", 5, "1d8+3", "piercing").plus("1d4", "acid"))
                .attack(new Monsters.Attack("Sticky Pseudopod", 5, "1d6+3", "bludgeoning").rider("grappled", Ability.STR, 13))
                .look("#a04000", "📦").blurb("It was a chest. Then it was teeth. It is still, technically, a chest.");
        c.monster("rc_sun_spirit", "Spirit of the Lost Sun").type("celestial").role("caster").ac(14).hp(36).speed(30).fly(40).stats(10, 16, 14, 12, 16, 16)
                .cr(2, 0).resist("radiant").immune("necrotic", "poison").condImmune("charmed", "frightened", "poisoned")
                .attack(new Monsters.Attack("Sunbeam", 6, "2d8", "radiant").ranged(60, 120).spell())
                .look("#f9e79f", "☀").blurb("The temple's guardian spirit, repaying you for keeping the gem safe.");
    }

    static void items(Campaign c) {
        c.item("rc_sun_gem", "The Sun Gem", "wondrous",
                "A fist-sized topaz full of slow golden fire. It was the heart of a king's crown. +1 to attack rolls and spell attacks — and it whispers at night.",
                Json.obj("bonus", Json.obj("attack", 1, "spell_attack", 1), "attunable", true, "value", 1000));
        c.item("rc_professors_map", "Professor Quill's Map", "quest",
                "An annotated map of the Temple of the Lost Sun. The margins say things like 'DO NOT step on the snakes (carved)' and 'bridge = bad'.", null);
        // Joel's v2 upgrades: from chests and the merchant camps. (Each one counts once per hero; attune to apply.)
        upgrade(c, "rc_whetstone", "Whetstone and Fine Oil", "damage", "Found in a chest. Your weapon edge is honed to a bright line. +1 to weapon damage.", 35);
        upgrade(c, "rc_plating", "Reinforced Plating", "ac", "Found in a chest. Riveted plates that fit any armour. +1 to Armor Class.", 40);
        upgrade(c, "rc_edge_outskirts", "Weapon Edge Upgrade (Outskirts)", "damage", "The Outskirts trader regrinds your weapon. +1 to weapon damage.", 35);
        upgrade(c, "rc_armor_outskirts", "Armor Upgrade (Outskirts)", "ac", "The Outskirts trader patches and reinforces your armour or shield. +1 to Armor Class.", 40);
        upgrade(c, "rc_edge_outpost", "Weapon Edge Upgrade (Jungle Gate)", "damage", "A jungle smith's edge, keen as a fang. +1 to weapon damage.", 35);
        upgrade(c, "rc_armor_outpost", "Armor Upgrade (Jungle Gate)", "ac", "Hardened hide and bronze studs from the Jungle Gate Outpost. +1 to Armor Class.", 40);
        // the Secret Merchant's wares (Joel's jokes, with a little magic added)
        c.item("rc_bag_of_holding", "Bag of Holding (Support Edition)", "wondrous",
                "Bigger on the inside. Contains SmartTab terminals, tablets, card readers, tangled cables and one support ticket from 2019. +1 to saving throws (nothing can surprise you after that ticket).",
                Json.obj("bonus", Json.obj("save", 1), "attunable", true, "value", 75));
        c.item("rc_decanter_of_coffee", "Decanter of Boundless Coffee", "wondrous",
                "Produces enough coffee to survive three operations meetings and an endless shift-change handoff. +2 to initiative.",
                Json.obj("bonus", Json.obj("initiative", 2), "attunable", true, "value", 50));
    }

    static void upgrade(Campaign c, String id, String name, String key, String desc, int value) {
        c.item(id, name, "wondrous", desc, Json.obj("bonus", Json.obj(key, 1), "attunable", true, "value", value));
    }

    /** Joel's post-combat chest: open it (a natural 1 means it was a Treasure Mimic all along) or leave it. */
    static void chest(Campaign c, String id, String chapter, String backdrop, String time, String next, String found, Map<String, Object>... reward) {
        c.scene(id).chapter(chapter).title("A Heavy Chest").backdrop(backdrop).time(time).music("mystery")
                .text("Among the defeated foe's spoils sits a heavy chest, iron-banded and very, very quiet.")
                .choice("Open the chest").icon("📦").check("luck", 2).chooser()
                    .pass(found, next, reward)
                    .fail("IT'S A TRAP! The chest snaps open with razor-sharp teeth — a MIMIC!", id + "_mimic")
                .choice("Leave it alone").icon("🚶").effect(log("You decide it's too risky and walk away. The chest seems… disappointed.")).go(next);
        c.scene(id + "_mimic").chapter(chapter).title("Treasure Mimic!").backdrop(backdrop).time(time).music("battle")
                .text("The 'chest' unfolds a tongue like a wet carpet and a great many teeth. Its lid is stuck to your sleeve.")
                .choice("Fight the mimic!").icon("⚔").battle("rc_mimic")
                    .victory("The mimic shudders and goes still. Inside its gullet, among the bones of less careful adventurers: a few coins.", next, xp(40), gold(10));
    }

    // =========================================================================================== prologue
    static void prologue(Campaign c) {
        c.scene("rc_start").chapter(PRO).title("The Edge of the Map").backdrop("swamp").time("dusk").weather("fog").music("explore")
                .text("The road ends at SUNREACH, a trading post nailed to the edge of the jungle like a notice nobody reads. Beyond its palisade the trees rise in a green wall a hundred feet high, steaming in the dusk, loud with frogs and birds and things that are neither.",
                        "Somewhere in there, everyone says, is the TEMPLE OF THE LOST SUN — and somewhere past that, a kingdom that forgot itself. Treasure-hunters go in every season. Fewer come out every season.",
                        "Light and music spill from a long, low tavern with a jackal's skull over the door: THE THIRSTY JACKAL. Someone inside is offering good coin for brave idiots.")
                .choice("Head into the Thirsty Jackal").icon("🍺").go("rc_jackal");

        c.scene("rc_jackal").chapter(PRO).title("The Thirsty Jackal").backdrop("tavern").time("night").music("tavern").anchor("tavern")
                .ambience("crowd", "fire", "rain")
                .aside("Every explorer in Sunreach drinks at the Jackal. Most of them drink to forget the jungle. A few drink to remember it.",
                        "Mind the parrot. It bites, and it lies.")
                .onEnter(companion("rc_odalys", 3), companion("rc_pim", 3))
                .npc("Mama Odu", "Keeper of the Thirsty Jackal", "#784212", "🍺", "Sit, sit! You look like you're going into the jungle. Eat first. The jungle doesn't feed you, it feeds ON you.")
                .text("The Jackal is all bamboo and lamp-smoke and noise. Porters gamble with bone dice by the door. A parrot on the rafters is loudly insulting a mercenary. A cook is chasing a small monkey across the bar, and the monkey is winning.",
                        "In the corner, beneath a wall of maps, a woman in a dusty explorer's coat is pinning up a hand-drawn notice: *WANTED — STEADY HANDS FOR THE TEMPLE OF THE LOST SUN. GOOD PAY. ASK FOR PROF. QUILL.*",
                        "At the bar, a young priestess and a gnome in an enormous pith helmet look up hopefully. They have clearly answered the same notice.")
                .choice("Ask Mama Odu about the Temple of the Lost Sun").icon("🍲").once().check("history", 11).chooser()
                    .pass("\"The sun-priests,\" Mama Odu says, ladling stew. \"They built the door with three signs on it. The sun opens it. The moon and the snake? They bite.\" She taps your bowl. \"The SUN, love. Remember it.\"", "rc_jackal", flag("sun_lore"))
                    .fail("\"Old place. Old stones. Old bones,\" Mama Odu says, and shrugs. \"Eat your stew.\" It is very good stew.", "rc_jackal", heal("1d4"))
                .choice("Join the porters' game of bone dice").icon("🎲").once().check("luck", 11).chooser()
                    .pass("The bones roll your way three times running. The porters groan and push a little pile of silver at you.", "rc_jackal", gold(15))
                    .fail("The bones hate you. The porters are very polite about taking your money.", "rc_jackal", gold(-5))
                .choice("Help the cook catch the thieving monkey").icon("🐒").once().check("acrobatics", 12).chooser()
                    .pass("You vault the bar, snag the monkey mid-leap and hand it back to the cook — along with the healing potion it had stolen from a mercenary's pack. The cook insists you keep the potion.", "rc_jackal", item("potion_healing"))
                    .fail("The monkey runs up your arm, steals your hat, and escapes through the roof. The whole room applauds the monkey.", "rc_jackal")
                .choice("Win over the insulting parrot").icon("🦜").once().check("animal_handling", 12).chooser()
                    .pass("Two crackers and a lot of patience later, the parrot lands on your shoulder and whispers, very clearly: \"Bridge bad. BRIDGE BAD.\" Then it flies away.", "rc_jackal", flag("bridge_warning"))
                    .fail("The parrot calls you something that makes Mama Odu cover the cook's ears.", "rc_jackal")
                .choice("Speak with Professor Quill").icon("📜").go("rc_professor");

        c.scene("rc_professor").chapter(PRO).title("Professor Amara Quill").backdrop("tavern").time("night").music("mystery")
                .npc("Professor Amara Quill", "Archaeologist", "#6e2c00", "🧭", "You're hired if you can walk and you don't faint. Can you walk? Good. Faint later.")
                .text("Professor Quill has ink on her fingers, a pencil behind each ear, and a voice built for lecture halls. She spreads a map across the table and pins it flat with your mugs.",
                        "\"Three hundred years ago, the last king of this coast wore a crown with a stone in it — the SUN GEM. Someone stole it and hid it in the Temple of the Lost Sun. The king went mad. The kingdom went quiet. Nobody's seen either since.\"",
                        "\"My partner, Tomas Reyes, went in after the gem two weeks ago. Tomas is brilliant and stubborn and very, very bad at coming back on time.\" Her jaw tightens. \"Find him. And if you find the gem — don't let it fall into the wrong hands. That includes yours, frankly. A hundred gold for your trouble.\"")
                .choice("\"We'll find Tomas. And the gem.\"").icon("🤝").effect(item("rc_professors_map"), flag("quill_hired")).go("rc_market")
                .choice("\"Two hundred, and we'll find him faster.\"").icon("💰").check("persuasion", 13).chooser()
                    .pass("Quill laughs for the first time. \"Done. Bring him back alive and I'll make it two hundred.\"", "rc_market", item("rc_professors_map"), flag("quill_hired"), flag("reward_up"))
                    .fail("\"One hundred,\" she says, \"and I won't tell Mama Odu you tried to haggle with a worried woman.\" Fair.", "rc_market", item("rc_professors_map"), flag("quill_hired"))
                .choice("\"What else do you know about the temple?\"").icon("🔍").once().check("investigation", 11).chooser()
                    .pass("She taps the map. \"The sun-priests guarded the gem with a door of three symbols and a skeletal guardian. Tomas's last note mentions 'stones that bite' in the jungle beyond. I assume that's a metaphor. Tomas never uses metaphors.\"", "rc_professor", flag("sun_lore"))
                    .fail("\"It's old, it's dangerous and it's been looted twice,\" she says. \"Next question.\"", "rc_professor");

        c.scene("rc_market").chapter(PRO).title("Sunreach at Dawn").backdrop("town").time("dawn").music("explore").shop().anchor("town").checkpoint()
                .text("Dawn steams off the palisade. Sunreach's single street of shops is already open, because in Sunreach the shops never really close — they just sleep in shifts.",
                        "Professor Quill hands you a pouch of travel money, a list of supplies and a look that says *come back*. Beyond the gate, a narrow trail plunges into the green.")
                .choice("Set out for the Temple of the Lost Sun").icon("🌴").effect(gold(10)).go("rc_ravine");
    }

    // =========================================================================================== chapter 1
    static void chapterOne(Campaign c) {
        c.scene("rc_ravine").chapter(CH1).title("The Ravine").backdrop("bridge").time("day").music("explore").anchor("road")
                .text("Two days into the jungle, the trail stops at the lip of a ravine so deep the river at the bottom sounds like rumour. On the far side, half-swallowed by vines, sits an ancient temple of honey-coloured stone with a great carved sun above its door.",
                        "An old rope-and-plank bridge sags across the gap, missing every third board. Far below, the river runs shallow in places. The cliffs are sheer, and somewhere in the trees a scout is watching you and pretending not to.")
                .choice("Cross the old bridge").icon("🌉").check("save:dex", 12).each()
                    .pass("You pick your way across plank by plank, never trusting any of them. The bridge sways and complains, but it holds.", "rc_temple_door")
                    .fail("A plank breaks beneath your feet! You catch the rope with one hand and haul yourself up, bruised and swearing.", "rc_temple_door", damageOne("1d6", "bludgeoning"))
                .choice("Search for a shallow river crossing").icon("🌊").check("survival", 10).group()
                    .pass("You find a calm, shallow path down to the river and across it, where the rock is solid and the handholds are friendly.", "rc_temple_door")
                    .fail("The current is stronger than it looked. You make it across, battered against the rocks.", "rc_temple_door", damage("1d4", "bludgeoning", "", 0))
                .choice("Pay 5 gold to bribe a hidden forest scout").icon("💰").ifGold(5)
                    .effect(gold(-5), log("A scout in leaf-green paint melts out of the trees, bites your coin, and guides you safely across the gorge by a path you would never have found."))
                    .go("rc_temple_door")
                .choice("Climb down and scale the steep ravine cliffs").icon("🧗").check("athletics", 14).each()
                    .pass("You scale the sheer cliff with expert agility, and the view from the top is worth every handhold.", "rc_temple_door", xp(30))
                    .fail("A handhold gives way! You tumble down the rocky cliffside before you catch a root.", "rc_temple_door", damageOne("2d6", "bludgeoning"))
                .choice("Follow the Professor's map to a hidden switchback").icon("🗺").ifItem("rc_professors_map").ifFlag("bridge_warning")
                    .effect(log("The parrot was right, and so was the map: a carved stair, hidden by ferns, winds safely down the ravine wall. The bridge creaks mournfully behind you, cheated."))
                    .go("rc_temple_door");

        c.scene("rc_temple_door").chapter(CH1).title("The Door of Four Symbols").backdrop("ruins").time("day").music("mystery")
                .text("The temple door is a single slab of stone twice your height. Carved into it are four great symbols, each the size of a shield and each worn smooth by centuries of nervous hands:",
                        "A blazing SUN. A crescent MOON. A coiled SERPENT. A SKULL, carved deep with ancient warnings.",
                        "There is no handle, no keyhole, and no doubt at all that pressing the wrong one would be a mistake.")
                .choice("Press the Sun").icon("☀").effect(flag("door_right"))
                    .effect(log("The sun symbol glows like a struck coal. With a sound like a long breath let out, the door rolls aside.")).go("rc_temple_hall")
                .choice("Press the Moon").icon("🌙").check("save:con", 12).each()
                    .pass("A burst of silver magic hammers the doorway — but you brace and weather it. Grinding, grudging, the door opens anyway.", "rc_temple_hall")
                    .fail("The moon symbol releases a burst of freezing frost that rimes your armour white. The door opens anyway, like it was making a point.", "rc_temple_hall", damageOne("2d4", "cold"))
                .choice("Press the Serpent").icon("🐍").check("save:dex", 12).each()
                    .pass("A poisoned dart hisses out of the serpent's mouth. You twist aside. The door, sulking, opens.", "rc_temple_hall")
                    .fail("A poisoned dart shoots out of the serpent's mouth and finds you. The door opens anyway, like it was making a point.", "rc_temple_hall", damageOne("2d4+1", "poison"))
                .choice("Press the Skull").icon("💀").check("save:dex", 13).each()
                    .pass("The ceiling lets go! You throw yourself clear as the debris comes down. When the dust settles, the door is open.", "rc_temple_hall")
                    .fail("The skull symbol unleashes a trap, burying you in falling debris. You dig yourself out. The door is open, smugly.", "rc_temple_hall", damageOne("2d6+1", "bludgeoning"))
                .choice("Study the carvings before you touch anything").icon("🔍").once().check("religion", 12).advantageIf("sun_lore").chooser()
                    .pass("The sun is carved deeper than the others, the stone around it worn by generations of priests' hands. It's the true key. The others are teeth — and the skull is the worst of them.", "rc_temple_door", flag("door_studied"))
                    .fail("They're all carved beautifully. That's all you've got.", "rc_temple_door");

        c.scene("rc_temple_hall").chapter(CH1).title("The Hall of the Lost Sun").backdrop("hall").time("day").music("eerie")
                .text("Inside, the temple is cool and dim and smells of old incense. Shafts of sun slant through holes in the roof onto a long hall of pillars, each carved with priests holding up the sky.",
                        "At the far end, a stair climbs to an altar where something glows, gold and slow, like an ember that has never heard of going out. Before it stands a figure in gilded armour, leaning on a halberd. It has no face. It has not had one for a very long time.",
                        "As you step into the light, its skull turns towards you with a dry, patient click. Around the hall, the bones of lesser guards begin to stir.")
                .choice("Leave an offering on the sun-priests' shrine").icon("🕯").once().ifGold(5)
                    .effect(gold(-5), inspiration(), log("You lay five gold coins in the offering bowl. The sunlight in the hall brightens for a moment, warm on your face. (Heroic Inspiration!)"))
                    .go("rc_temple_hall")
                .choice("Speak to the guardian with the sun-priests' words").icon("☀").once().ifFlag("door_right").check("religion", 13).chooser()
                    .pass("\"We came by the Sun,\" you say, \"as the priests did.\" The guardian hesitates — long enough for you to take the high ground on the stair before it remembers its orders.", "rc_guardian_fight", flag("guardian_surprised"))
                    .fail("The guardian's jaw opens. Whatever it once would have said, it has forgotten. It lowers its halberd.", "rc_guardian_fight")
                .choice("Draw steel").icon("⚔").go("rc_guardian_fight");

        c.scene("rc_guardian_fight").chapter(CH1).title("The Skeletal Guardian").backdrop("hall").time("day").music("battle").checkpoint()
                .text("The Skeletal Guardian levels its halberd. Behind it, the gold light on the altar pulses like a heartbeat. The lesser bones clatter into line.")
                .choice("Fight the guardian").icon("⚔").battle("rc_guardian")
                    .victory("The guardian crumbles at last, and its bones settle into a neat pile, as if even now it wanted to leave the place tidy.", "rc_chest_guardian", xp(100));
        chest(c, "rc_chest_guardian", CH1, "hall", "day", "rc_relic", "You carefully unlatch the lid… Inside you find a glass vial — a Healing Potion!", item("potion_healing"));

        c.scene("rc_relic").chapter(CH1).title("The Gem").backdrop("chapel").time("day").music("mystery")
                .text("An ancient relic floats above the altar, turning slowly in a beam of sunlight: a fist-sized gem of golden topaz, full of slow fire. This is it. THE SUN GEM, prised from a king's crown three hundred years ago.",
                        "Up close, you can hear it. Not with your ears. A voice like warm honey at the back of your mind: *Take me. Wear me. You could be so much more.* And under the honey, very faint, something else — a hollow, furious echo, far away, like a man shouting in an empty palace.",
                        "There is no sign of Tomas here, only his chalk mark on the wall — an arrow pointing deeper into the jungle — and a note: *Gem is cursed. Do NOT touch. Gone ahead. — T.*")
                .choice("Destroy it").icon("💥").check("athletics", 12).chooser()
                    .pass("You bring the guardian's halberd down with everything you have. The gem cracks, then shatters in a burst of golden light — and far away, deep in the jungle, something screams. You have hurt the curse at its root.", "rc_sanctuary", flag("gem_destroyed"), flag("king_weakened"), xp(50))
                    .fail("The gem shrugs off your blow — but on the third strike it cracks, then shatters in a burst of golden light. Far away, something screams. (The effort leaves your arms shaking.)", "rc_sanctuary", flag("gem_destroyed"), flag("king_weakened"), damageOne("1d6", "force"), xp(50))
                .choice("Burn the curse out of it with holy fire").icon("🔥").spell("sacred_flame")
                    .pass("Radiant fire pours down on the gem. It screams — a man's scream, far away — and the honey-voice burns out of it. What's left shatters into harmless golden sand.", "rc_sanctuary", flag("gem_destroyed"), flag("king_weakened"), xp(75))
                .choice("Seal it away").icon("🔒").check("religion", 11).chooser()
                    .pass("You set the gem back upon the altar and speak the sun-priests' sealing words. Stone grinds closed over it, and a warm golden presence breathes through the hall: *Thank you. When you need me, I will come.*", "rc_sanctuary", flag("gem_sealed"), xp(50))
                    .fail("You seal the gem inside the altar as best you can. It isn't elegant, but the honey-voice falls silent, and a faint warmth lingers in the hall like gratitude.", "rc_sanctuary", flag("gem_sealed"), xp(25))
                .choice("Claim its power").icon("✋")
                    .effect(item("rc_sun_gem"), gold(100), xp(75), flag("gem_claimed"),
                            log("You close your hand around the Sun Gem. Power floods up your arm, warm and golden and wonderful, and every coin the priests ever left on the altar seems to roll toward you of its own accord (+100 gold). Somewhere far away, in an empty palace, someone turns his head."))
                    .go("rc_sanctuary");

        c.scene("rc_sanctuary").chapter(CH1).title("A Hidden Sanctuary").backdrop("chapel").time("dusk").music("camp").checkpoint()
                .onEnter(level(2))
                .text("Behind the altar you find a hidden sanctuary: a small room with a spring of clear water, stone benches, and the faded paintings of priests who once slept here between prayers.",
                        "It's safe — or safe enough. Outside, the jungle is turning gold, then grey. Your arms ache from the fight. **(Every hero reaches level 2!)**",
                        "Somewhere outside, beyond the ravine, you saw the smoke of a trader's campfire.")
                .choice("Short rest — tend your wounds and press on").icon("☕").effect(shortRest()).go("rc_ch1_end")
                .choice("Long rest — sleep here tonight, and risk what follows you").icon("🌙").check("survival", 12).group()
                    .pass("You take turns on watch. Twice something moves in the dark beyond the door. Twice it thinks better of it. You wake fully rested.", "rc_ch1_end", longRest())
                    .fail("You build a small fire and try to sleep. Something has been following you through the darkness…", "rc_ambush_sanctuary");

        c.scene("rc_ambush_sanctuary").chapter(CH1).title("Random Encounter!").backdrop("ruins").time("night").music("tension")
                .text("You wake to the crunch of a boot on gravel and a low growl in the dark. A bandit, a dire wolf and a rattling skeleton have all followed your firelight to the same place — and none of them is pleased to share.")
                .choice("Grab your weapons!").icon("⚔").battle("rc_ambush")
                    .victory("You survive the encounter. The rest of the night is quiet, and you manage to recover your strength.", "rc_chest_ambush1", longRest(), xp(40));
        chest(c, "rc_chest_ambush1", CH1, "ruins", "night", "rc_ch1_end", "You carefully unlatch the lid… Inside you find a pouch containing 12 gold!", gold(12));

        c.scene("rc_ch1_end").chapter(CH1).title("Chapter 1 Complete").backdrop("ruins").time("dawn").music("hope")
                .text("Morning finds you on the temple steps with Tomas's chalk arrows leading away into the trees. Beyond them the jungle grows darker, thicker and stranger — mist curls between the trunks, and the birdsong stops as though someone has shut a door on it.",
                        "Somewhere in there is Tomas Reyes. Somewhere past that, the Forgotten Kingdom.")
                .choice("Visit the trader's camp on the way").icon("🛒").go("rc_outskirts")
                .choice("Follow Tomas's arrows into the Cursed Jungle").icon("🌴").go("rc_jungle");

        // Joel v2: the merchant camp after Chapter 1
        c.scene("rc_outskirts").chapter(CH1).title("Merchant Camp: Chapter 1 Outskirts").backdrop("camp").time("day").music("tavern").checkpoint()
                .npc("The Traveling Trader", "Merchant Camp", "#b9770e", "🛒", "Potions, fire in a bottle, a sharper edge, a thicker hide. Coin first, heroics after.")
                .text("A traveling trader has set up camp near your path: a mule, a striped awning, and a folding table of goods weighed down with river stones.",
                        "\"Fifteen for a potion,\" the trader says. \"Twenty-five for Alchemist's Fire. I'll put an edge on your blade for thirty-five, and for forty I'll make your armour something to be proud of.\"")
                .choice("Buy a Healing Potion (15 gold)").icon("🧪").ifGold(15).effect(gold(-15), item("potion_healing"), log("You purchased a healing potion!")).go("rc_outskirts")
                .choice("Buy a flask of Alchemist's Fire (25 gold)").icon("🔥").ifGold(25).effect(gold(-25), item("alchemists_fire"), log("You purchased a flask of Alchemist's Fire!")).go("rc_outskirts")
                .choice("Upgrade your weapon edge, +1 damage (35 gold)").icon("🗡").once().ifGold(35).effect(gold(-35), item("rc_edge_outskirts"), log("You sharpened your weapon! (Attune to it to apply the +1.)")).go("rc_outskirts")
                .choice("Upgrade your armor or shield, +1 AC (40 gold)").icon("🛡").once().ifGold(40).effect(gold(-40), item("rc_armor_outskirts"), log("You upgraded your gear! (Attune to it to apply the +1.)")).go("rc_outskirts")
                .choice("Leave the merchant and press onward").icon("🌴").effect(log("You pack up and continue your journey.")).go("rc_jungle");
    }

    // =========================================================================================== chapter 2
    static void chapterTwo(Campaign c) {
        c.scene("rc_jungle").chapter(CH2).title("The Cursed Jungle").backdrop("forest").time("day").weather("fog").music("eerie")
                .text("The jungle is thick with mist and the sound of distant cries. Every tree is furred with moss; every leaf drips. The light is green and the shadows are greener.",
                        "Under a fallen tree, half-hidden in ferns, a man in a torn explorer's coat raises one bloody hand. \"Oh, thank the Morning Lord,\" he croaks. \"Tell me Amara sent you. Tell me she's furious. It means she's not worried.\"",
                        "It's TOMAS REYES — and his leg is badly hurt.")
                .choice("Help him — bind his wounds").icon("🩹").check("medicine", 10).chooser()
                    .pass("You splint the leg and stop the bleeding. Tomas sags with relief and presses a potion and a purse into your hands. \"Amara gave me these. I clearly wasn't using them properly.\"", "rc_tomas_talk", item("potion_healing"), gold(15), flag("tomas_saved"), flag("tomas_trusts"))
                    .fail("It's ugly work, but you get the leg splinted. Tomas presses a potion and a purse on you anyway. \"For the trouble. And the screaming. Sorry about the screaming.\"", "rc_tomas_talk", item("potion_healing"), gold(15), flag("tomas_saved"), flag("tomas_trusts"))
                .choice("Heal him with magic").icon("✨").spell("cure_wounds")
                    .pass("Warm light knits the bone. Tomas flexes his leg in wonder and pushes a potion and a purse into your hands. \"I owe you a drink. Possibly a building.\"", "rc_tomas_talk", item("potion_healing"), gold(15), flag("tomas_saved"), flag("tomas_trusts"), flag("tomas_strong"))
                .choice("Demand a toll for your aid").icon("💰")
                    .effect(gold(25), flag("toll_demanded"), flag("tomas_saved"), log("You shake him down for valuables, collecting 25 gold, then bind his leg with bad grace. Tomas pays without a word. He won't forget it — and neither, you suspect, will you."))
                    .go("rc_stones")
                .choice("Keep moving").icon("🚶")
                    .effect(log("You keep your distance and move deeper into the jungle. Behind you, Tomas calls out once — then goes quiet."))
                    .go("rc_stones");

        c.scene("rc_tomas_talk").chapter(CH2).title("Tomas Reyes").backdrop("forest").time("day").weather("fog").music("mystery")
                .npc("Tomas Reyes", "The Professor's partner", "#1a5276", "🧭", "Did you touch the gem? Please tell me you didn't touch the gem.")
                .text("Tomas talks fast, the way people do when they've been alone too long. \"The gem is the king's heart — or the piece of it that stayed good. Without it he rotted. With it…\" He shudders. \"With it he'd be whole again. And he'd remember everything.\"",
                        "\"There's a champion in a clearing ahead — a skeleton in a crown of vines. It did this to my leg. And the path before it is paved with stones that bite. Step only on the ones with moss. The moss knows.\"",
                        "\"Go. I'll limp back to Sunreach. Tell Amara… tell her I said she was right. She'll enjoy that.\"")
                .choice("\"Get home safe, Tomas.\"").icon("🤝").effect(flag("moss_tip")).go("rc_stones")
                .choice("Show him the Sun Gem").icon("💎").once().ifItem("rc_sun_gem")
                    .effect(log("Tomas goes white. \"You — oh no. Oh no, no. Listen to me. When you meet the King, he will want it back more than he wants to breathe. Throw it at him. Break it on his crown. It's the only thing that can hurt him from the inside.\""), flag("gem_secret"))
                    .go("rc_tomas_talk");

        c.scene("rc_stones").chapter(CH2).title("The Stones That Bite").backdrop("forest").time("day").weather("fog").music("tension")
                .text("You reach a path covered with strange stones — flat and grey, laid in a neat line through a stretch of jungle where nothing at all is growing. Not a fern. Not a weed. In the trees on either side, little brass tubes peep out of the bark.")
                .choice("Cross the stones carefully").icon("🪨").check("save:dex", 13).each()
                    .pass("You dodge the hidden traps, dancing from stone to stone as poison darts hiss past.", "rc_champion_clearing")
                    .fail("Poison darts strike from the trees! You stumble through, stung and woozy.", "rc_champion_clearing", damageOne("2d4", "poison"))
                .choice("Step only on the mossy stones").icon("🌿").ifFlag("moss_tip").check("perception", 8).group()
                    .pass("Tomas was right. The moss only grows where the stones don't click. You cross without a single dart flying.", "rc_champion_clearing")
                    .fail("Mostly right. One dart finds a shoulder, but it's only one.", "rc_champion_clearing", damageOne("1d4", "poison"))
                .choice("Search for a safer route").icon("🔍").check("survival", 11).group()
                    .pass("You find a safe path around the stones, along a dry streambed.", "rc_champion_clearing")
                    .fail("You stumble into a thorn patch and come out the other side looking like an angry pincushion.", "rc_champion_clearing", damage("1d6", "piercing", "", 0))
                .choice("Toss heavy rocks ahead to trigger the traps").icon("🪨")
                    .effect(xp(15), log("You lob rock after rock onto the stones. Darts hiss and clatter harmlessly into the dirt until the brass tubes are empty. Clever."))
                    .go("rc_champion_clearing")
                .choice("Pay a local jungle tracker 10 gold to guide you around").icon("🧭").ifGold(10)
                    .effect(gold(-10), log("The tracker leads you cleanly through the hazard without a scratch, and vanishes back into the green with your coin."))
                    .go("rc_champion_clearing")
                .choice("Burn the dart tubes out of the trees").icon("🔥").spell("fire_bolt")
                    .pass("A few precise bolts of fire, and the brass tubes pop like corn. The stones click harmlessly as you cross.", "rc_champion_clearing");

        c.scene("rc_champion_clearing").chapter(CH2).title("The Champion's Clearing").backdrop("grove").time("dusk").weather("fog").music("tension").checkpoint()
                .text("The stones lead to a clearing ringed by broken pillars. In the middle, sitting on a fallen column with a greatsword across its knees, is a skeleton in a crown of living vines. Moss fills its eye-sockets. Flowers grow from its ribs.",
                        "It has the patience of something that has won every fight it ever had. It stands. Around the clearing, more bones climb out of the leaf-litter, and something large and many-legged shifts in the mist.")
                .choice("Creep around the edge and strike first").icon("🤫").check("stealth", 13).group()
                    .pass("You slip through the mist to the champion's flank before it finds you. Strike now!", "rc_champion_go", flag("champion_surprised"))
                    .fail("A twig snaps. The champion's head turns towards you with a creak of old vine.", "rc_champion_go")
                .choice("Challenge it openly").icon("⚔").go("rc_champion_go");

        c.scene("rc_champion_go").chapter(CH2).title("The Skeletal Champion").backdrop("grove").time("dusk").weather("fog").music("battle")
                .text("The Skeletal Champion raises its greatsword in a salute older than the kingdom. Then it charges.")
                .choice("Fight!").icon("⚔").battle("rc_champion")
                    .victory("The champion falls, and the vines that held it together go limp. For a moment the clearing is perfectly quiet — then, one by one, the birds begin to sing again.", "rc_chest_champion", xp(150), gold(25));
        chest(c, "rc_chest_champion", CH2, "grove", "dusk", "rc_jungle_camp", "You carefully unlatch the lid… Inside you discover a whetstone and fine oil, upgrading your weapon edge! (Attune to it for +1 damage.)", item("rc_whetstone"));

        c.scene("rc_jungle_camp").chapter(CH2).title("Merchant Camp: Jungle Gate Outpost").backdrop("camp").time("night").music("camp").shop().anchor("camp").checkpoint()
                .aside("Have you noticed nobody in this story ever asks how the merchant got here? Wise. Very wise.")
                .npc("The Traveling Trader", "Jungle Gate Outpost", "#b9770e", "🛒", "You again! Same prices. The jungle's been good for business.")
                .text("At the Jungle Gate, an old explorers' outpost still has most of a roof. You're not the first here: someone has a fire going already, and a folding table, and a sign that says OPEN.",
                        "Behind the table sits the same traveling trader you met at the Outskirts. You did not see them pass you. You will never see them pass you.",
                        "Through the trees to the north, faint and blue, you can see the towers of a forgotten kingdom.")
                .choice("Buy a Healing Potion (15 gold)").icon("🧪").ifGold(15).effect(gold(-15), item("potion_healing"), log("You purchased a healing potion!")).go("rc_jungle_camp")
                .choice("Buy a flask of Alchemist's Fire (25 gold)").icon("🔥").ifGold(25).effect(gold(-25), item("alchemists_fire"), log("You purchased a flask of Alchemist's Fire!")).go("rc_jungle_camp")
                .choice("Upgrade your weapon edge, +1 damage (35 gold)").icon("🗡").once().ifGold(35).effect(gold(-35), item("rc_edge_outpost"), log("You sharpened your weapon! (Attune to it to apply the +1.)")).go("rc_jungle_camp")
                .choice("Upgrade your armor or shield, +1 AC (40 gold)").icon("🛡").once().ifGold(40).effect(gold(-40), item("rc_armor_outpost"), log("You upgraded your gear! (Attune to it to apply the +1.)")).go("rc_jungle_camp")
                .choice("Short rest — eat, patch up, and move on at dawn").icon("☕").effect(shortRest()).go("rc_ch2_end")
                .choice("Long rest — sleep the night, and risk the jungle").icon("🌙").check("survival", 12).group()
                    .pass("The trader's fire keeps the night at bay. You sleep like stones and wake fully rested.", "rc_ch2_end", longRest())
                    .fail("You settle in for a long rest… and wake to find the trader gone, the fire out, and something circling the camp.", "rc_ambush_camp");

        c.scene("rc_ambush_camp").chapter(CH2).title("Random Encounter!").backdrop("camp").time("night").music("tension")
                .text("!!! RANDOM ENCOUNTER !!! Something has been following you through the darkness — and it brought friends.")
                .choice("To arms!").icon("⚔").battle("rc_ambush")
                    .victory("You survive the encounter, but the interruption costs you time. You manage to recover your strength before dawn.", "rc_chest_ambush2", longRest(), xp(40));
        chest(c, "rc_chest_ambush2", CH2, "camp", "night", "rc_ch2_end", "You carefully unlatch the lid… Inside you find a sealed glass flask of Alchemist's Fire!", item("alchemists_fire"));

        c.scene("rc_ch2_end").chapter(CH2).title("Chapter 2 Complete").backdrop("forest").time("dawn").music("hope")
                .text("The jungle thins. The mist lifts. And there, filling the valley below you, is a city of black stone and broken towers, its walls wrapped in a cold blue haze that doesn't move with the wind.",
                        "Nobody has walked these streets for three hundred years. Something is walking them now.")
                .choice("Descend into the Forgotten Kingdom").icon("🏰").go("rc_city_wall");
    }

    // =========================================================================================== chapter 3
    static void chapterThree(Campaign c) {
        c.scene("rc_city_wall").chapter(CH3).title("The Forgotten Kingdom").backdrop("ruins").time("dusk").weather("ash").music("ruins").anchor("dungeon").checkpoint()
                .onEnter(level(3), longRest())
                .text("You emerge before a ruined city surrounded by a black stone wall. Banners that were once gold hang in grey rags from every tower. Blue light flickers in the high windows of the palace at its heart. **(Every hero reaches level 3, rested and ready.)**",
                        "The gates are sealed, but four possible entrances remain: the great main gate, a narrow drainage tunnel half-flooded with black water, a broken watchtower whose shattered upper window still gapes open, and a stretch of ancient perimeter wall where one foundation stone looks loose.")
                .choice("Fighter: force open the main gate with a mighty shove").icon("💪").ifClass("fighter")
                    .effect(gold(10), flag("gate_forced"), log("You set your shoulder to the gate and SHOVE. Three hundred years of rust give way at once. Among the fallen guards' bones beyond, you find 10 gold."))
                    .go("rc_hall_kings")
                .choice("Force open the main gate").icon("🚪").check("athletics", 14).chooser()
                    .pass("With a mighty shove, you force the gate open! Among the fallen guards you find 10 gold.", "rc_hall_kings", gold(10), flag("gate_forced"))
                    .fail("The gate gives way suddenly, crushing your shoulder against the frame.", "rc_hall_kings", damageOne("2d6", "bludgeoning"))
                .choice("Rogue: slip through the drainage tunnel").icon("🗝").ifClass("rogue")
                    .effect(gold(15), log("You slip through quietly, spotting every pressure plate and tripwire. In a smuggler's stash halfway along you find 15 gold."))
                    .go("rc_hall_kings")
                .choice("Slip through the drainage tunnel").icon("🌊").check("save:dex", 13).each()
                    .pass("You slip through quietly and avoid the traps.", "rc_hall_kings")
                    .fail("A hidden blade snaps out of the wall!", "rc_hall_kings", damageOne("1d8", "slashing"))
                .choice("Ranger: climb the broken watchtower").icon("🏹").ifClass("ranger")
                    .effect(item("potion_healing"), log("You climb safely, hand over hand, and from the top you spot a guards' supply cache. Inside: a healing potion, still sealed."))
                    .go("rc_hall_kings")
                .choice("Climb the broken watchtower").icon("🧗").check("athletics", 13).each()
                    .pass("You climb safely and enter through the shattered window.", "rc_hall_kings")
                    .fail("A loose stone gives way beneath your foot, and you bang down a dozen feet before you catch yourself.", "rc_hall_kings", damageOne("1d10", "bludgeoning"))
                .choice("Search the perimeter wall for a loosened foundation stone").icon("🧱").check("investigation", 12).chooser()
                    .pass("You pry at the perimeter wall. A hidden panel slides open, revealing a safe shortcut into the courtyard.", "rc_hall_kings", xp(25))
                    .fail("The wall shifts abruptly, trapping your fingers and bruising your arm. You squeeze through anyway.", "rc_hall_kings", damageOne("1d6+1", "bludgeoning"));

        c.scene("rc_hall_kings").chapter(CH3).title("The Hall of Fallen Kings").backdrop("hall").time("night").music("eerie")
                .npc("The Ghost Knight", "Last of the Royal Guard", "#5dade2", "🛡", "Only those who prove their worth may approach the throne.")
                .text("You enter the Hall of Fallen Kings. Statues of a dozen crowned men and women line the walls, their faces chiselled off. At the far end, before a pair of great bronze doors, a knight in translucent blue armour stands with his sword point-down on the floor.",
                        "His voice is cold and tired and very old. \"Only those who prove their worth may approach the throne.\"",
                        "Behind him, the floor is cracked; one statue near the wall has fallen, and there is a dark gap behind it.")
                .choice("Challenge the knight to combat").icon("⚔").battle("rc_knight")
                    .victory("The knight drops to one knee. \"Worthy,\" he says, and bows before fading into mist. \"When you face him… I will be there.\"", "rc_chest_knight", xp(100), gold(20), flag("knight_ally"))
                .choice("Toss a pouch of 20 gold as a tribute to distract the spirit").icon("💰").ifGold(20)
                    .effect(gold(-20), xp(40), log("You toss a glittering pile of gold. The greedy phantom pauses to inspect it, coin by coin, allowing you past. He will not stand with you at the end — but he will not stand against you either."))
                    .go("rc_barracks")
                .choice("Attempt to reason with the spirit").icon("🗣").check("persuasion", 15).advantageIf("tomas_trusts").chooser()
                    .pass("You tell him about the gem, the curse, the king his oath has trapped. The knight is silent a long time. \"…My king died three hundred years ago,\" he says at last. \"What sits on the throne is not him.\" He steps aside and presses 30 gold of old coin into your hand. \"End it. I will stand with you.\"", "rc_barracks", gold(30), xp(75), flag("knight_ally"))
                    .fail("The knight sees through your words and attacks!", "rc_knight_angry")
                .choice("Bind his will with a charm").icon("💗").spell("charm_person")
                    .pass("Your magic finds the man inside the oath. The knight lowers his blade. \"Strange,\" he says. \"I had forgotten what it is like to be spoken to kindly.\" He steps aside, and his ghostly coins chime into your hand.", "rc_barracks", gold(30), xp(75), flag("knight_ally"))
                .choice("Search the hall for a way around him").icon("🔦").check("investigation", 14).chooser()
                    .pass("You discover a hidden passage behind the fallen statue. Inside, you find a potion and 20 gold — and a stair that comes out well beyond the knight.", "rc_barracks", item("potion_healing"), gold(20), xp(50))
                    .fail("The passage collapses! A stone catches you on the way out — and the noise wakes the hall's other guardian.", "rc_hall_collapse", damageOne("2d6", "bludgeoning"));

        c.scene("rc_knight_angry").chapter(CH3).title("The Angered Ghost Knight").backdrop("hall").time("night").music("battle")
                .text("\"LIARS!\" The knight's blade comes up blazing blue. All around the hall, the hollow soldiers of the royal guard step out of the walls to answer his call.")
                .choice("Defend yourselves!").icon("⚔").battle("rc_knight_angry")
                    .victory("The knight falls back into mist, his voice fading: \"…Perhaps… you were… telling the truth…\"", "rc_chest_knight_angry", xp(75), gold(10));
        chest(c, "rc_chest_knight", CH3, "hall", "night", "rc_barracks", "You carefully unlatch the lid… Inside you find reinforced plating, upgrading your armor! (Attune to it for +1 AC.)", item("rc_plating"));
        chest(c, "rc_chest_knight_angry", CH3, "hall", "night", "rc_barracks", "You carefully unlatch the lid… Inside you find a Healing Potion and a pouch of 15 gold!", item("potion_healing"), gold(15));

        c.scene("rc_hall_collapse").chapter(CH3).title("The Hall Guardian").backdrop("hall").time("night").music("battle")
                .text("Dust is still falling when one of the great statues pulls its feet off its plinth with a sound like a quarry being torn in half. It has no face. It doesn't need one.")
                .choice("Fight the Hall Guardian").icon("⚔").battle("rc_hall_guardian")
                    .victory("The statue topples and shatters across the floor. Beyond the rubble, the knight is gone, and the way is open.", "rc_chest_hall", xp(75), gold(10));
        chest(c, "rc_chest_hall", CH3, "hall", "night", "rc_barracks", "You carefully unlatch the lid… Inside you find a sealed glass flask of Alchemist's Fire!", item("alchemists_fire"));

        c.scene("rc_barracks").chapter(CH3).title("The Royal Guard's Barracks").backdrop("dungeon").time("night").music("camp").checkpoint()
                .text("You reach the royal guard's abandoned barracks: long rows of bunks, a dead hearth, armour stands holding nothing but dust. It's quiet here. The blue light doesn't reach.",
                        "Beyond the barracks, three doors lead deeper into the palace — and, if you look closely, a fourth. An inscription above them reads: *'The crown seeks wisdom, the sword seeks courage, and the eye seeks truth.'*")
                .choice("Search the guards' old infirmary").icon("🧪").once().check("medicine", 10).chooser()
                    .pass("Behind a cracked cabinet: two healing potions, wax seals unbroken after three hundred years. The royal guard took good care of its own.", "rc_barracks", item("potion_healing"), item("potion_healing"))
                    .fail("Dust, bandages that crumble at a touch, and one potion that's mostly sediment — but it'll still do the job.", "rc_barracks", item("potion_healing"))
                .choice("Short rest before the doors").icon("☕").once().effect(shortRest()).go("rc_three_doors")
                .choice("Go straight to the doors").icon("🚪").go("rc_three_doors");

        c.scene("rc_three_doors").chapter(CH3).title("The Doors").backdrop("hall").time("night").music("mystery")
                .text("*'The crown seeks wisdom, the sword seeks courage, and the eye seeks truth.'*",
                        "A red door marked with a sword. A golden door marked with a crown. A blue door marked with an eye. Somewhere behind all three, a long way off, you hear a slow, heavy heartbeat of stone.")
                .choice("The red door marked with a sword").icon("⚔").go("rc_armory")
                .choice("The golden door marked with a crown").icon("👑").go("rc_treasury")
                .choice("The blue door marked with an eye").icon("👁").go("rc_observatory")
                .choice("The obsidian door marked with a shifting shadow (hidden path)").icon("🌑").go("rc_shadow_path");

        c.scene("rc_shadow_path").chapter(CH3).title("The Corridor of Shadows").backdrop("dungeon").time("night").music("eerie")
                .text("The obsidian door leads into a pitch-black corridor of shadows. Your light shrinks to the size of a coin. Something in the dark is breathing in time with you.")
                .choice("Navigate the darkness").icon("🌑").check("perception", 14).group()
                    .pass("You navigate the darkness flawlessly, discovering a hidden stash of supplies and lore: a healing potion, 35 gold, and the royal guard's own notes on the King.", "rc_throne_approach", item("potion_healing"), gold(35), xp(75))
                    .fail("You stumble through unseen abyssal traps in the dark! You come out the far side bleeding, but you come out.", "rc_throne_approach", damage("2d6", "necrotic", "dex", 13));

        c.scene("rc_armory").chapter(CH3).title("The Royal Armory").backdrop("dungeon").time("night").music("tension")
                .text("The sword door opens into the Royal Armory. Armour and weapons litter the floor, racks of spears line the walls — and in the centre, a twelve-foot statue of granite holding a halberd opens eyes that glow like coals.",
                        "Around it, swords rise from their racks and hang in the air, points turning towards you.")
                .choice("Face the Royal Stone Guardian").icon("⚔").battle("rc_armory")
                    .victory("The guardian shatters, and 40 gold spills from its plinth. Beneath it sits a heavy chest.", "rc_chest_armory", xp(175), gold(40));
        chest(c, "rc_chest_armory", CH3, "dungeon", "night", "rc_throne_approach", "You carefully unlatch the lid… Inside, wrapped in oilcloth: a Healing Potion and a flask of Alchemist's Fire.", item("potion_healing"), item("alchemists_fire"));

        c.scene("rc_treasury").chapter(CH3).title("The Royal Treasury").backdrop("hoard").time("night").music("mystery")
                .text("The crown door leads into the Royal Treasury. It has been looted — twice, by the look of the skeletons — but one thing remains: a cursed chest on a pedestal beneath an empty golden crown-stand.",
                        "The chest is very beautiful. The skeletons around it are very dead.")
                .choice("Open the chest").icon("📦").check("luck", 11).chooser()
                    .pass("The chest opens safely. You find a royal fortune!", "rc_throne_approach", gold(80), xp(100))
                    .fail("The chest is trapped! A burst of fire fills the room.", "rc_treasury_fire")
                .choice("Inspect the chest for magic").icon("🔍").check("investigation", 14).chooser()
                    .pass("You find the rune, nudge it sideways with a hairpin, and disarm the trap. The lid lifts on a royal fortune.", "rc_throne_approach", gold(80), xp(100))
                    .fail("You trigger the trap while inspecting it!", "rc_treasury_fire")
                .choice("Pick the lock with thieves' tools").icon("🗝").ifItem("thieves_tools").check("sleight_of_hand", 12).chooser()
                    .pass("Click. Click. *Click.* The trap disarms with a disappointed hiss, and the lid lifts on a royal fortune.", "rc_throne_approach", gold(80), xp(100))
                    .fail("Click. Click. *Whoosh.*", "rc_treasury_fire")
                .choice("Leave it alone").icon("🚶").effect(xp(25), log("You leave the chest untouched and move on. The skeletons seem to approve.")).go("rc_throne_approach");

        c.scene("rc_treasury_fire").chapter(CH3).title("Fire!").backdrop("hoard").time("night").music("tension")
                .text("Jets of flame roar out of the crown-stand and sweep the room!")
                .choice("Dive for cover!").icon("🔥").check("save:dex", 13).each()
                    .pass("You dive behind a pillar as the fire washes past.", "rc_treasury_after")
                    .fail("The fire catches you before you can get clear.", "rc_treasury_after", damageOne("2d8", "fire"));

        c.scene("rc_treasury_after").chapter(CH3).title("Smoke and Gold").backdrop("hoard").time("night").music("mystery")
                .text("When the smoke clears, the chest stands open. The trap has spent itself. Half the fortune inside has melted into a lump, but half has not.")
                .choice("Take what's left and move on").icon("💰").effect(gold(40), xp(50)).go("rc_throne_approach");

        c.scene("rc_observatory").chapter(CH3).title("The Observatory").backdrop("chapel").time("night").music("mystery")
                .text("The eye door opens into the Observatory. Three tall mirrors direct beams of moonlight towards a sealed magical seal in the floor. A riddle is carved around it:",
                        "*\"I speak without a mouth and answer without ears. I have no body, but I come alive with the wind.\"*")
                .choice("\"A shadow.\"").icon("🌑").go("rc_observatory_wrong")
                .choice("\"An echo.\"").icon("🔊").effect(heal("2d8"), gold(25), xp(100),
                        log("The mirrors align! The seal opens, and a healing aura surrounds you, warm as morning."))
                    .go("rc_throne_approach")
                .choice("\"A dream.\"").icon("💭").go("rc_observatory_wrong")
                .choice("Read the old astronomers' notes for a hint").icon("📖").once().check("arcana", 13).chooser()
                    .pass("A scribbled margin note: *'— tested the riddle on the apprentices. Ran up and down the hall shouting the answer. Hall shouted it back.'*", "rc_observatory")
                    .fail("The notes are mostly star charts and complaints about the food.", "rc_observatory");

        c.scene("rc_observatory_wrong").chapter(CH3).title("Blinding Light").backdrop("chapel").time("night").music("tension")
                .text("The mirrors flare with blinding light!")
                .choice("Shield your eyes!").icon("✋").check("save:con", 13).each()
                    .pass("You turn away just in time. Then you force the seal open through sheer determination.", "rc_throne_approach", xp(50))
                    .fail("The light sears you. Gritting your teeth, you force the seal open through sheer determination.", "rc_throne_approach", damageOne("2d6", "radiant"), xp(50));
    }

    // =========================================================================================== finale
    static void finale(Campaign c) {
        c.scene("rc_throne_approach").chapter(FIN).title("The Throne Room").backdrop("lair").time("night").music("boss").checkpoint()
                .text("You finally reach the throne room. Blue fire burns without heat in two great braziers on either side of a throne of black stone. The forgotten king sits motionless upon it, a crown with one empty setting on his brow.",
                        "His eyes ignite with blue fire.",
                        "\"You have disturbed my kingdom for the last time!\" The king rises, wielding a blade of dark magic.")
                .choice("\"Your kingdom ended three hundred years ago.\" Fight!").icon("⚔").unlessFlag("gem_claimed").go("rc_king_fight")
                .choice("Hurl the Sun Gem at his crown!").icon("💎").ifItem("rc_sun_gem")
                    .effect(takeItem("rc_sun_gem"), flag("king_weakened"),
                            log("You hurl the Sun Gem with everything you have. It strikes the empty setting of his crown and SHATTERS in a burst of golden fire. The King staggers, clutching his head, and for a moment his own voice cries out from inside the curse. (The Cursed King is weakened!)"))
                    .go("rc_king_fight")
                .choice("Keep the gem — and fight him for the crown too").icon("✋").ifFlag("gem_claimed")
                    .effect(log("\"MINE,\" the King breathes, and the gem in your pack flares hot enough to hurt. He can feel it. He will never stop coming for it."), damage("1d6", "fire", "", 0))
                    .go("rc_king_fight");

        c.scene("rc_king_fight").chapter(FIN).title("The Cursed King").backdrop("lair").time("night").music("boss")
                .text("The Cursed King descends from his throne. The blue braziers roar. Somewhere in the walls, the palace's dead begin to stir.")
                .choice("For the kingdom!").icon("⚔").battle("rc_cursed_king")
                    .victory("", "rc_king_falls", xp(250), gold(75));

        c.scene("rc_king_falls").chapter(FIN).title("The King Falls").backdrop("lair").time("night").music("sorrow")
                .text("The Cursed King falls to his knees. The blue fire fades from his eyes, and for one moment he looks only like a very old, very tired man. \"Thank you,\" he whispers — and then he is dust, and the crown rolls across the floor and stops at your feet.",
                        "The palace begins to tremble.",
                        "You have three choices.")
                .choice("Take the crown and rule the kingdom").icon("👑")
                    .effect(flag("ending_crowned"), gold(100), xp(100), log("You take the crown and claim the throne. The remaining spirits kneel before their new ruler. Your new kingdom grants you 100 gold."))
                    .go("rc_escape")
                .choice("Destroy the crown and free the kingdom").icon("💥")
                    .effect(flag("ending_freed"), xp(150), log("You shatter the crown. A wave of light sweeps through the kingdom. The restless spirits are finally free."))
                    .go("rc_escape")
                .choice("Leave the crown and disappear into legend").icon("🌫")
                    .effect(flag("ending_legend"), xp(100), log("You leave the crown untouched. Your names will become a legend told by travellers for generations."))
                    .go("rc_escape");

        c.scene("rc_escape").chapter(FIN).title("The Palace Trembles").backdrop("lair").time("night").music("battle")
                .text("Cracks race up the walls. Pillars groan. Three hundred years of curse are coming undone all at once, and the palace intends to come down with it.")
                .choice("RUN!").icon("🏃").check("athletics", 12).group()
                    .pass("You sprint through falling stone and burst out into the open air as the palace folds in on itself behind you, in a column of dust lit gold by the rising sun.", "rc_epilogue")
                    .fail("Falling masonry batters you all the way down the stairs, but you make it — bruised, filthy and alive — as the palace folds in on itself behind you.", "rc_epilogue", damage("2d6", "bludgeoning", "dex", 12));

        c.scene("rc_epilogue").chapter("Epilogue").title("Back at the Thirsty Jackal").backdrop("tavern").time("night").music("triumph").anchor("epilogue")
                .onEnter(gold(100), longRest())
                .npc("Professor Amara Quill", "Archaeologist", "#6e2c00", "🧭", "You came back. Everyone said you wouldn't. I said you would. I'd like that noted.")
                .text("The Thirsty Jackal is louder than you've ever heard it. Word has run ahead of you, the way it does in Sunreach: the blue light over the Forgotten Kingdom has gone out. The porters have made up a song already. It's terrible. Everyone sings it.",
                        "Professor Quill pays every coin she promised, plus a little extra she pretends she forgot to count.",
                        "Congratulations — you survived all three chapters of THE RELIC OF CROWN AND SHADOW.")
                .choice("Toast Tomas, who made it home on his own two legs").icon("🍻").once().ifFlag("tomas_saved")
                    .effect(log("Tomas limps over on a crutch and raises a mug. \"Amara says she was right. I said I said she was right. It's going very well.\" Quill rolls her eyes and holds his hand under the table."))
                    .go("rc_epilogue")
                .choice("Ask after Tomas").icon("🕯").once().unlessFlag("tomas_saved")
                    .effect(log("Tomas crawled back into Sunreach two days ago, half-starved and furious, and has been asleep ever since. Quill hasn't left his side. \"He's alive,\" she says. \"No thanks to anyone.\" It stings. It's meant to."))
                    .go("rc_epilogue")
                .choice("Tell Mama Odu you wear a crown now").icon("👑").once().ifFlag("ending_crowned")
                    .effect(log("\"Your Majesty,\" Mama Odu says, and curtseys, and charges you double for everything. \"Royal prices.\""))
                    .go("rc_epilogue")
                .choice("Listen to the spirits' song on the wind").icon("🌅").once().ifFlag("ending_freed")
                    .effect(log("Out past the palisade, the jungle is singing — birds, wind, and something like a choir of very old, very happy voices, going home at last."))
                    .go("rc_epilogue")
                .choice("Hear a stranger tell your legend wrong").icon("📖").once().ifFlag("ending_legend")
                    .effect(log("A traveller at the bar is telling the tale of the heroes who beat the Cursed King and walked away from his crown. He gets every detail wrong. You don't correct him. It's a better story his way."))
                    .go("rc_epilogue")
                .choice("Collect the extra reward Quill promised").icon("💰").once().ifFlag("reward_up").ifFlag("tomas_saved").effect(gold(100)).go("rc_epilogue")
                .choice("Follow a strange figure with a suspiciously modern-looking bag…").icon("🎒").once().ifGold(100).go("rc_secret_merchant")
                .choice("Face Professor Quill about the toll").icon("😬").once().ifFlag("toll_demanded")
                    .effect(log("\"Tomas told me about the toll,\" Quill says quietly. \"Twenty-five gold, from a man with a broken leg.\" She counts out your reward anyway, every coin, and does not look at you while she does it."))
                    .go("rc_epilogue")
                .choice("Raise a mug and end the tale").icon("🏁")
                    .effect(log("Thank you for playing THE RELIC OF CROWN AND SHADOW!"))
                    .go("@end");

        // Joel's Secret Merchant, as written — only for customers with actual spending money.
        c.scene("rc_secret_merchant").chapter("Epilogue").title("Secret Merchant Found!").backdrop("tavern").time("night").music("tavern")
                .npc("The Secret Merchant", "Purveyor of Fine Goods", "#7d3c98", "🎒", "Ah, a customer with actual spending money!")
                .text("\"Ah, a customer with actual spending money!\" The merchant opens a suspiciously modern-looking bag.")
                .choice("Bag of Holding — 75 gold").icon("🎒").once().ifGold(75)
                    .effect(gold(-75), item("rc_bag_of_holding"),
                            log("You purchase the Bag of Holding. Inside are SmartTab terminals, tablets, card readers, tangled cables, and one support ticket from 2019. \"Have you tried turning the dungeon off and on again?\""))
                    .go("rc_secret_merchant")
                .choice("Decanter of Boundless Coffee — 50 gold").icon("☕").once().ifGold(50)
                    .effect(gold(-50), item("rc_decanter_of_coffee"),
                            log("You purchase the Decanter of Boundless Coffee. It produces enough coffee to survive three operations meetings and an endless shift-change handoff."))
                    .go("rc_secret_merchant")
                .choice("Walk away").icon("🚶")
                    .effect(log("You walk away. The merchant mutters about missed profits."))
                    .go("rc_epilogue");
    }

    // =========================================================================================== battle maps
    static void encounters(Campaign c) {
        c.encounter("rc_guardian").title("The Skeletal Guardian").backdrop("hall").music("battle").objective("defeat:rc_skeletal_guardian")
                .intro("The Skeletal Guardian holds the altar stair with its long halberd (10-ft reach). Skeletons are VULNERABLE TO BLUDGEONING — maces and hammers shatter them.")
                .surpriseIf("guardian_surprised")
                .map("######################",
                     "#......#..BB..#......#",
                     "#..s...#..g...#...s..#",
                     "#......#......#......#",
                     "#..##..........##....#",
                     "#..##....E.....##....#",
                     "#....................#",
                     "#..##..........##....#",
                     "#..##..........##....#",
                     "#.......PP..PP.......#",
                     "#.......PP..PP.......#",
                     "######################")
                .legend('g', "rc_skeletal_guardian").legend('s', "rc_temple_skeleton").extra("rc_temple_skeleton");

        c.encounter("rc_ambush").title("Random Encounter!").backdrop("camp").music("battle")
                .intro("A Restless Bandit, a Hungry Dire Wolf and a Restless Skeleton have all followed your firelight. Fight back to back!")
                .map("TTTTTT,,,,,,,,,,TTTTTT",
                     "TT,,,,,..w.....,,,,,TT",
                     "T,,,.........E....,,,T",
                     ",,....B......B......,,",
                     ",.......LL..........b,",
                     ",.....PPLLPP........,,",
                     ",,....PP..PP.......s,,",
                     "T,,,.....B.....s...,,T",
                     "TT,,,,,........,,,,,TT",
                     "TTTTTT,,,,,,,,,,TTTTTT")
                .legend('w', "rc_dire_wolf").legend('b', "rc_bandit").legend('s', "rc_temple_skeleton").extra("rc_bandit");

        c.encounter("rc_champion").title("The Skeletal Champion").backdrop("grove").music("battle").objective("defeat:rc_skeletal_champion")
                .intro("The Skeletal Champion fights with a greatsword and can call more bones from the earth once. Bludgeoning shatters skeletons!")
                .surpriseIf("champion_surprised")
                .map("TTTT,,,,,,,,,,,,,,TTTT",
                     "TT,,,..##....##..,,,TT",
                     "T,,....##.c..##...,,,T",
                     ",,..................,,",
                     ",..s......,,.....s...,",
                     ",.......,,,,,,.......,",
                     ",,..##..,,E,,,..##..,,",
                     "T,..##..........##..,T",
                     "T,,.....PP..PP.....,,T",
                     "TT,,,...PP..PP...,,,TT",
                     "TTTT,,,,,,,,,,,,,,TTTT")
                .legend('c', "rc_skeletal_champion").legend('s', "rc_temple_skeleton").extra("rc_jungle_spider");

        c.encounter("rc_knight").title("Trial of the Ghost Knight").backdrop("hall").music("battle").objective("defeat:rc_ghost_knight").nonlethal()
                .intro("A trial of worth: defeat the Ghost Knight in single combat (no one dies — he yields when beaten). His Oath of the Hall can frighten those near him.")
                .map("######################",
                     "#..##...######...##..#",
                     "#..##....####....##..#",
                     "#.........k..........#",
                     "#....................#",
                     "#..##.....E......##..#",
                     "#..##............##..#",
                     "#....................#",
                     "#.......PP..PP.......#",
                     "#.......PP..PP.......#",
                     "######################")
                .legend('k', "rc_ghost_knight");

        c.encounter("rc_knight_angry").title("The Angered Ghost Knight").backdrop("hall").music("battle").objective("defeat:rc_ghost_knight")
                .intro("The Ghost Knight attacks, and the hollow soldiers of the royal guard answer his call. Defeat the knight to end it.")
                .map("######################",
                     "#..##...######...##..#",
                     "#..##.h..####..h.##..#",
                     "#.........k..........#",
                     "#....................#",
                     "#..##.....E......##..#",
                     "#..##............##..#",
                     "#....................#",
                     "#.......PP..PP.......#",
                     "#.......PP..PP.......#",
                     "######################")
                .legend('k', "rc_ghost_knight").legend('h', "rc_hollow_soldier").extra("rc_hollow_soldier");

        c.encounter("rc_hall_guardian").title("The Hall Guardian").backdrop("hall").music("battle").objective("defeat:rc_hall_guardian")
                .intro("A statue of a forgotten king tears itself off its plinth. It RESISTS piercing and slashing — use bludgeoning or magic. Rubble litters the hall.")
                .map("######################",
                     "#..B......q.......B..#",
                     "#....................#",
                     "#..##..........##....#",
                     "#..##..h....B..##..h.#",
                     "#........E...........#",
                     "#..##....B.....##..h.#",
                     "#..##..........##....#",
                     "#.......PP..PP.......#",
                     "#.......PP..PP.......#",
                     "######################")
                .legend('q', "rc_hall_guardian").legend('h', "rc_hollow_soldier").extra("rc_hollow_soldier");

        c.encounter("rc_armory").title("The Royal Armory").backdrop("dungeon").music("battle").objective("defeat:rc_stone_guardian")
                .intro("The Royal Stone Guardian wakes, and the armory's swords fly from their racks. The Guardian RESISTS piercing and slashing; its Armory Quake knocks everyone nearby prone.")
                .map("######################",
                     "#B.B.B.B.B..B.B.B.B.B#",
                     "#....................#",
                     "#..a......S.......a..#",
                     "#....................#",
                     "#..BB.....E......BB..#",
                     "#..BB............BB..#",
                     "#....................#",
                     "#.......PP..PP.......#",
                     "#.......PP..PP.......#",
                     "######################")
                .legend('S', "rc_stone_guardian").legend('a', "rc_animated_blade").extra("rc_animated_blade");

        c.encounter("rc_mimic").title("Treasure Mimic!").backdrop("dungeon").music("battle").objective("defeat:rc_mimic")
                .intro("The chest was a MIMIC. Its sticky pseudopod can grapple you, and its bite drips acid. Kill it before it eats anyone's boots.")
                .map("################",
                     "#..............#",
                     "#......m.......#",
                     "#..............#",
                     "#.....E........#",
                     "#..............#",
                     "#....PP..PP....#",
                     "#....PP..PP....#",
                     "################")
                .legend('m', "rc_mimic");

        c.encounter("rc_cursed_king").title("The Cursed King").backdrop("lair").music("boss").objective("defeat:rc_cursed_king")
                .intro("THE CURSED KING wields a blade of dark magic and blue fire. Two SOULFIRE BRAZIERS feed him — while they burn, he heals every turn. Knock them over! He RESISTS necrotic and cold, and his command can force you to KNEEL.")
                .weakenIf("king_weakened")
                .ally("rc_knight_ally", "knight_ally")
                .ally("rc_sun_spirit", "gem_sealed")
                .phase(66, "The King roars, and the floor of the throne room splits — blue soulfire GEYSERS erupt from the cracks!",
                        Json.obj("op", "hazard", "count", 3, "type", "necrotic", "dice", "2d6", "name", "Soulfire geyser", "dc", 13),
                        Json.obj("op", "say", "text", "\"You have disturbed my kingdom for the LAST TIME!\""))
                .phase(40, "\"GUARDS!\" Hollow soldiers claw their way out of the throne-room walls!",
                        Json.obj("op", "summon", "monster", "rc_hollow_soldier", "count", 2))
                .phase(15, "The King's blue fire gutters. For a moment his own voice breaks through.",
                        Json.obj("op", "say", "text", "\"…Is it… morning? Has the sun… come back?\""))
                .map("########################",
                     "#....b......K.......b..#",
                     "#......................#",
                     "#...##....,,,,....##...#",
                     "#...##....,,,,....##...#",
                     "#...........E..........#",
                     "#......................#",
                     "#...##............##...#",
                     "#...##............##...#",
                     "#........PP..PP........#",
                     "#........PA..AP........#",
                     "########################")
                .legend('K', "rc_cursed_king").legend('b', "rc_soulfire_brazier").extra("rc_hollow_soldier");
    }
}
