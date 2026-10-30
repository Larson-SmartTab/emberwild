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
 * THE GEM — a story by Joel Hill.
 * <p>
 * Joel wrote this tale as a console adventure for the team's Java challenge (his first Java program!). This is the
 * same story — the Temple of the Lost Sun, the Cursed Jungle, the Forgotten Kingdom, the three doors, the riddle of the
 * echo, the Cursed King and the Secret Merchant with the suspiciously modern-looking bag — rebuilt on Emberwild's
 * D&D 2024 engine so a whole table can play it together.
 * <pre>
 *  Prologue   The Thirsty Jackal ........ a trading post on the jungle's edge, a professor with a missing partner
 *  Chapter 1  Temple of the Lost Sun .... the ravine, the door of three symbols, the Skeletal Guardian, THE GEM
 *  Chapter 2  The Cursed Jungle ......... the injured explorer, the stones that bite, the Skeletal Champion, the merchant
 *  Chapter 3  The Forgotten Kingdom ..... three ways in, the Ghost Knight, the three doors (sword, crown, eye)
 *  Finale     The Cursed King ........... a blade of dark magic, braziers of blue soulfire, and what you did with the gem
 * </pre>
 * What changed from the console version, and why:
 * <ul>
 *   <li>The relic is the Sun Gem — the missing stone from the Cursed King's crown — so the title pays off, and the
 *       choice you make in Chapter 1 matters in the Finale (destroying it weakens the King, sealing it earns a sun-spirit
 *       ally, claiming it gives you power now… and the King will want it back).</li>
 *   <li>Fights use real D&D rules (attack rolls against Armor Class, spells, conditions), so every class plays
 *       differently: the Ranger's accuracy and the Rogue's skills finally pay off.</li>
 *   <li>The Secret Merchant opens shop mid-adventure too, so gold is worth having before the end.</li>
 *   <li>Rest stops keep Joel's risky long rest — but a hero who falls is handled by the engine, never left walking.</li>
 * </ul>
 * Custom ids are prefixed {@code tg_}.
 */
public final class TheGem implements CampaignScript {

    static final String PRO = "Prologue — Sunreach";
    static final String CH1 = "Chapter 1 — Temple of the Lost Sun";
    static final String CH2 = "Chapter 2 — The Cursed Jungle";
    static final String CH3 = "Chapter 3 — The Forgotten Kingdom";
    static final String FIN = "Finale — The Cursed King";

    @Override
    public Campaign build() {
        Campaign c = Campaign.create("the_gem", "The Gem, a story by Joel Hill", "Joel Hill");
        c.cover("ruins");
        c.synopsis("Three hundred years ago the Sun Gem was pried from the crown of the last king of a forgotten kingdom, and "
                + "the king did not die so much as curdle. Now a professor at a jungle trading post wants her missing partner "
                + "back, and every treasure-hunter on the coast wants the gem. From the Temple of the Lost Sun, through a cursed "
                + "jungle, to a throne room where blue fire burns in a dead king's eyes, a band of heroes must decide what the "
                + "gem is worth — and what to do with a crown nobody should wear. A story by Joel Hill, built for 1-4 players "
                + "starting at level 1 (rising to level 3); companions fill a small party.");
        c.hook("epilogue", "A rain-soaked professor at the feast is hiring for an expedition to the Temple of the Lost Sun", "tg_start");

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
        c.pregen(hero("tg_garrick", Json.obj(
                "name", "Garrick Sunhallow", "pronouns", "he/him", "class", "fighter", "species", "human", "background", "soldier",
                "method", "pointbuy", "scores", scores(15, 13, 14, 8, 12, 10), "increases", Json.obj("str", 2, "con", 1),
                "skills", List.of("athletics", "perception"),
                "color", "#d4ac0d", "emblem", "☀", "hair", "#5d4037", "skin", "#d7a77f", "eyes", "#6e2c00",
                "appearance", "A broad, sun-browned swordsman with a dented breastplate he polishes every night whether it needs it or not.",
                "personality", "Steady, cheerful and very literal. Believes most locked doors are just doors that haven't been pushed hard enough.",
                "backstory", "Garrick guarded caravans along the jungle coast for ten years and never once lost a client — though he did lose a lot of caravans.",
                "motto", "If it's stuck, push. If it's still stuck, push harder.")));
        c.pregen(hero("tg_vex", Json.obj(
                "name", "Vex Nightwhistle", "pronouns", "she/her", "class", "rogue", "species", "halfling", "background", "criminal",
                "method", "pointbuy", "scores", scores(8, 15, 14, 12, 10, 13), "increases", Json.obj("dex", 2, "con", 1),
                "skills", List.of("acrobatics", "investigation", "perception", "persuasion"),
                "color", "#5b2c6f", "emblem", "🗝", "hair", "#1c2833", "skin", "#f0c9a4", "eyes", "#27ae60",
                "appearance", "A tiny halfling in a coat with forty-one pockets, every one of them locked.",
                "personality", "Whistles when she's nervous, which is never. Collects keys to doors she has never seen.",
                "backstory", "Vex once stole a treasure map from the Professor. Then she read it, felt bad, and gave it back with notes.",
                "motto", "Every lock is a riddle. Every riddle has a key.")));
        c.pregen(hero("tg_lyra", Json.obj(
                "name", "Lyra Thornshot", "pronouns", "she/her", "class", "ranger", "species", "elf", "lineage", "wood", "background", "wayfarer",
                "method", "pointbuy", "scores", scores(10, 15, 13, 10, 15, 8), "increases", Json.obj("dex", 2, "wis", 1),
                "skills", List.of("nature", "survival", "stealth"),
                "color", "#1e8449", "emblem", "🏹", "hair", "#7e5109", "skin", "#c8a27a", "eyes", "#58d68d",
                "appearance", "A lean wood elf in leaf-green leathers, a longbow taller than she is slung across her back.",
                "personality", "Quiet, sharp-eyed and dry as summer bark. Speaks to birds more than people, and they answer more often.",
                "backstory", "Lyra grew up on the edge of the Cursed Jungle, listening to the drums nobody plays. She means to find out who doesn't play them.",
                "motto", "Breathe out. Loose. Never miss.")));

        // ---- Companions: AI-controlled, they join only when the party is small.
        Hero odalys = hero("tg_odalys", Json.obj(
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
        Hero pim = hero("tg_pim", Json.obj(
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
        c.monster("tg_temple_skeleton", "Sun-Temple Skeleton").type("undead").role("melee").ac(13).hp(13, "2d8+4").stats(10, 14, 15, 6, 8, 5)
                .cr(0.25, 50).vulnerable("bludgeoning").immune("poison").condImmune("poisoned", "frightened")
                .attack("Corroded Khopesh", 4, "1d6+2", "slashing")
                .attack(new Monsters.Attack("Sun-Bleached Bow", 4, "1d6+2", "piercing").ranged(80, 320))
                .look("#e5e7e9", "💀").blurb("Its armour is gilded with sunbursts. Its loyalty has outlived everything else about it.");
        c.monster("tg_skeletal_guardian", "Skeletal Guardian").type("undead").role("brute").ac(14).hp(30).stats(16, 12, 15, 6, 10, 5)
                .cr(1, 200).vulnerable("bludgeoning").immune("poison").condImmune("poisoned", "frightened")
                .attack(new Monsters.Attack("Temple Halberd", 5, "1d10+3", "slashing").reach(10))
                .look("#f9e79f", "🛡").blurb("The temple's last sentry. It has waited three hundred years to say no to someone.");
        c.monster("tg_skeletal_champion", "Skeletal Champion").type("undead").role("brute").ac(14).hp(40).stats(17, 12, 16, 6, 10, 6)
                .cr(2, 450).multi(2).vulnerable("bludgeoning").immune("poison").condImmune("poisoned", "frightened", "exhaustion")
                .attack(new Monsters.Attack("Jungle-Rusted Greatsword", 5, "1d8+3", "slashing"))
                .special(Monsters.special("bone_rally", "Rally the Bones").shape("summon", 0, 0).summon("tg_temple_skeleton", 1).uses(1))
                .look("#d5dbdb", "⚔").blurb("Vines grow through its ribs. It has been champion of this clearing for longer than the trees.");
        c.monster("tg_jungle_spider", "Mist Spider").type("beast").role("skirmisher").ac(13).hp(18).speed(30).stats(14, 16, 12, 2, 11, 4)
                .cr(0.5, 100).attack(new Monsters.Attack("Bite", 5, "1d6+3", "piercing").plus("1d6", "poison").rider("poisoned", Ability.CON, 11))
                .look("#76448a", "🕷").blurb("As big as a dog and as quiet as the mist it hunts in.");

        // ---- things that follow you to a campfire (Joel's random encounter: bandit, dire wolf or skeleton)
        c.monster("tg_bandit", "Restless Bandit").type("humanoid").role("melee").ac(12).hp(16).stats(12, 14, 12, 10, 10, 10)
                .cr(0.25, 50).attack("Scimitar", 4, "1d6+2", "slashing")
                .attack(new Monsters.Attack("Light Crossbow", 4, "1d8+2", "piercing").ranged(80, 320))
                .look("#935116", "🗡").blurb("Followed your firelight through the dark. Regrets it already.");
        c.monster("tg_dire_wolf", "Hungry Dire Wolf").type("beast").role("brute").ac(13).hp(26).speed(50).stats(17, 15, 15, 3, 12, 7)
                .cr(1, 200).attack(new Monsters.Attack("Bite", 5, "1d10+3", "piercing").rider("prone", Ability.STR, 13)).trait("pack_tactics")
                .look("#566573", "🐺").blurb("Ribs like a xylophone. Teeth like a bear trap.");

        // ---- the Forgotten Kingdom
        c.monster("tg_ghost_knight", "The Ghost Knight").type("undead").role("brute").ac(16).hp(72).speed(30).stats(16, 11, 14, 10, 13, 15)
                .cr(3, 700).multi(2).resist("necrotic", "cold").immune("poison").condImmune("poisoned", "frightened", "charmed", "exhaustion")
                .attack(new Monsters.Attack("Spectral Longsword", 6, "1d10+3", "slashing").plus("1d6", "necrotic"))
                .special(Monsters.special("oath_of_the_hall", "Oath of the Hall").shape("sphere", 10, 0).dmg("2d6", "necrotic").dc(13, Ability.WIS).rider("frightened").recharge(5))
                .look("#85c1e9", "🛡").blurb("Sworn to guard the throne, even now, even from a king who no longer deserves it.");
        c.monster("tg_knight_ally", "The Ghost Knight").type("undead").role("brute").ac(16).hp(52).speed(30).stats(16, 11, 14, 10, 13, 15)
                .cr(3, 0).multi(2).resist("necrotic", "cold").immune("poison").condImmune("poisoned", "frightened", "charmed")
                .attack(new Monsters.Attack("Spectral Longsword", 5, "1d8+3", "slashing").plus("1d6", "radiant"))
                .look("#d6eaf8", "🛡").blurb("He has finally found a cause worth his oath.");
        c.monster("tg_hollow_soldier", "Hollow Soldier").type("undead").role("melee").ac(13).hp(16).stats(13, 12, 14, 6, 8, 5)
                .cr(0.25, 50).resist("necrotic").immune("poison").condImmune("poisoned").attack("Rusted Spear", 4, "1d6+2", "piercing")
                .look("#5d6d7e", "👻").blurb("A palace guard still answering a roll-call that ended three centuries ago.");
        c.monster("tg_hall_guardian", "Hall Guardian").type("construct").role("brute").size(2).ac(15).hp(60).speed(25).stats(18, 8, 16, 3, 10, 1)
                .cr(2, 450).multi(2).resist("piercing", "slashing").immune("poison", "psychic").condImmune("charmed", "frightened", "poisoned", "paralyzed")
                .attack("Marble Fist", 6, "1d10+4", "bludgeoning")
                .look("#aab7b8", "🗿").blurb("A statue of a forgotten king that has decided it would rather be a wall.");
        c.monster("tg_stone_guardian", "Royal Stone Guardian").type("construct").role("brute").size(2).ac(16).hp(56).speed(25).stats(19, 8, 17, 3, 10, 1)
                .cr(3, 700).multi(2).resist("piercing", "slashing").immune("poison", "psychic").condImmune("charmed", "frightened", "poisoned", "paralyzed")
                .attack(new Monsters.Attack("Granite Halberd", 6, "1d10+4", "slashing").reach(10))
                .special(Monsters.special("armory_quake", "Armory Quake").shape("sphere", 10, 0).dmg("2d6", "bludgeoning").dc(13, Ability.DEX).rider("prone").recharge(5))
                .look("#7f8c8d", "🗿").blurb("Twelve feet of animated granite, built to guard the kingdom's weapons — and very good at it.");
        c.monster("tg_animated_blade", "Animated Royal Blade").type("construct").role("skirmisher").ac(15).hp(12).speed(40).fly(40).stats(12, 15, 11, 1, 5, 1)
                .cr(0.5, 100).immune("poison", "psychic").condImmune("charmed", "frightened", "poisoned")
                .attack("Slash", 4, "1d8+2", "slashing").look("#d0d3d4", "🗡").blurb("A sword from the armory racks, still eager for a hand.");

        // ---- the Finale
        c.monster("tg_soulfire_brazier", "Soulfire Brazier").type("object").role("object").ac(11).hp(10).speed(0).stats(10, 1, 10, 1, 1, 1)
                .cr(0, 25).immune("poison", "psychic", "necrotic").vulnerable("bludgeoning")
                .condImmune("charmed", "frightened", "poisoned", "paralyzed", "prone", "incapacitated")
                .attack("None", 0, "0", "fire").look("#5dade2", "🔵").blurb("Blue fire that burns without heat. While it burns, the King cannot die. Knock it over!");
        c.monster("tg_cursed_king", "The Cursed King").type("undead").role("boss").ac(15).hp(72).speed(30).stats(18, 12, 16, 12, 14, 18)
                .cr(4, 1100).saves("con", "wis", "cha").resist("necrotic", "cold").immune("poison").condImmune("poisoned", "frightened", "charmed", "exhaustion")
                .multi(2)
                .attack(new Monsters.Attack("Blade of Dark Magic", 6, "1d8+3", "slashing").plus("1d6", "necrotic"))
                .attack(new Monsters.Attack("Blue Fire Bolt", 6, "2d8", "necrotic").ranged(60, 60).spell())
                .special(Monsters.special("crown_of_blue_fire", "Crown of Blue Fire").shape("cone", 15, 0).dmg("3d6", "necrotic").dc(13, Ability.CON).recharge(5))
                .special(Monsters.special("royal_command", "\"KNEEL!\"").shape("sphere", 10, 0).dmg("1d6", "psychic").dc(12, Ability.WIS).rider("prone"))
                .trait("fed_by_objects", "legendary_resistance")
                .legendary(2, "special:royal_command", "attack:Blade of Dark Magic", "move")
                .look("#1f618d", "👑").blurb("The last king of the Forgotten Kingdom. His crown has an empty setting where the Sun Gem should be.");
        c.monster("tg_sun_spirit", "Spirit of the Lost Sun").type("celestial").role("caster").ac(14).hp(36).speed(30).fly(40).stats(10, 16, 14, 12, 16, 16)
                .cr(2, 0).resist("radiant").immune("necrotic", "poison").condImmune("charmed", "frightened", "poisoned")
                .attack(new Monsters.Attack("Sunbeam", 6, "2d8", "radiant").ranged(60, 120).spell())
                .look("#f9e79f", "☀").blurb("The temple's guardian spirit, repaying you for keeping the gem safe.");
    }

    static void items(Campaign c) {
        c.item("tg_sun_gem", "The Sun Gem", "wondrous",
                "A fist-sized topaz full of slow golden fire. It was the heart of a king's crown. +1 to attack rolls and spell attacks — and it whispers at night.",
                Json.obj("bonus", Json.obj("attack", 1, "spell_attack", 1), "attunable", true, "value", 1000));
        c.item("tg_professors_map", "Professor Quill's Map", "quest",
                "An annotated map of the Temple of the Lost Sun. The margins say things like 'DO NOT step on the snakes (carved)' and 'bridge = bad'.", null);
        // Joel's weapon drops, three tiers per class, as in the original.
        weapon(c, "tg_sunforged_greatsword", "Sunforged Greatsword", 1, 1, "A greatsword with a sunburst pommel that is always warm.");
        weapon(c, "tg_shadowfang_daggers", "Shadowfang Daggers", 1, 1, "A pair of black daggers that make no sound leaving a sheath.");
        weapon(c, "tg_moonpiercer_longbow", "Moonpiercer Longbow", 1, 1, "Pale wood strung with silver; the arrows leave a faint trail of moonlight.");
        weapon(c, "tg_champions_warblade", "Champion's Warblade", 1, 2, "The Skeletal Champion's own blade, scrubbed of rust and still hungry.");
        weapon(c, "tg_phantom_blades", "Phantom Assassin Blades", 1, 2, "Twin blades that flicker half out of the world mid-swing.");
        weapon(c, "tg_dragonstalker_bow", "Dragonstalker's Bow", 1, 2, "A recurve of dark horn, carved with the names of very large animals.");
        weapon(c, "tg_kingslayer_greatblade", "Kingslayer Greatblade", 2, 2, "Forged for the royal guard, and named for the day it was needed.");
        weapon(c, "tg_voidstrike_daggers", "Voidstrike Daggers", 2, 2, "Every cut leaves a thin line of starless black in the air.");
        weapon(c, "tg_worldender_longbow", "Worldender Longbow", 2, 2, "The armory's prize. Its draw weight is 'yes'.");
        // the Secret Merchant's wares (Joel's jokes, with a little magic added)
        c.item("tg_bag_of_holding", "Bag of Holding (Support Edition)", "wondrous",
                "Bigger on the inside. Contains SmartTab terminals, tablets, card readers, tangled cables and one support ticket from 2019. +1 to saving throws (nothing can surprise you after that ticket).",
                Json.obj("bonus", Json.obj("save", 1), "attunable", true, "value", 75));
        c.item("tg_decanter_of_coffee", "Decanter of Boundless Coffee", "wondrous",
                "Produces enough coffee to survive three operations meetings and an endless shift-change handoff. +2 to initiative.",
                Json.obj("bonus", Json.obj("initiative", 2), "attunable", true, "value", 50));
    }

    static void weapon(Campaign c, String id, String name, int attack, int damage, String desc) {
        c.item(id, name + " (+" + attack + ")", "weapon", desc + " +" + attack + " to weapon attack rolls and +" + damage + " to weapon damage.",
                Json.obj("bonus", Json.obj("attack", attack, "damage", damage), "attunable", true, "value", 150 * (attack + damage)));
    }

    // =========================================================================================== prologue
    static void prologue(Campaign c) {
        c.scene("tg_start").chapter(PRO).title("The Edge of the Map").backdrop("swamp").time("dusk").weather("fog").music("explore")
                .text("The road ends at SUNREACH, a trading post nailed to the edge of the jungle like a notice nobody reads. Beyond its palisade the trees rise in a green wall a hundred feet high, steaming in the dusk, loud with frogs and birds and things that are neither.",
                        "Somewhere in there, everyone says, is the TEMPLE OF THE LOST SUN — and somewhere past that, a kingdom that forgot itself. Treasure-hunters go in every season. Fewer come out every season.",
                        "Light and music spill from a long, low tavern with a jackal's skull over the door: THE THIRSTY JACKAL. Someone inside is offering good coin for brave idiots.")
                .choice("Head into the Thirsty Jackal").icon("🍺").go("tg_jackal");

        c.scene("tg_jackal").chapter(PRO).title("The Thirsty Jackal").backdrop("tavern").time("night").music("tavern").anchor("tavern")
                .ambience("crowd", "fire", "rain")
                .aside("Every explorer in Sunreach drinks at the Jackal. Most of them drink to forget the jungle. A few drink to remember it.",
                        "Mind the parrot. It bites, and it lies.")
                .onEnter(companion("tg_odalys", 3), companion("tg_pim", 3))
                .npc("Mama Odu", "Keeper of the Thirsty Jackal", "#784212", "🍺", "Sit, sit! You look like you're going into the jungle. Eat first. The jungle doesn't feed you, it feeds ON you.")
                .text("The Jackal is all bamboo and lamp-smoke and noise. Porters gamble with bone dice by the door. A parrot on the rafters is loudly insulting a mercenary. A cook is chasing a small monkey across the bar, and the monkey is winning.",
                        "In the corner, beneath a wall of maps, a woman in a dusty explorer's coat is pinning up a hand-drawn notice: *WANTED — STEADY HANDS FOR THE TEMPLE OF THE LOST SUN. GOOD PAY. ASK FOR PROF. QUILL.*",
                        "At the bar, a young priestess and a gnome in an enormous pith helmet look up hopefully. They have clearly answered the same notice.")
                .choice("Ask Mama Odu about the Temple of the Lost Sun").icon("🍲").once().check("history", 11).chooser()
                    .pass("\"The sun-priests,\" Mama Odu says, ladling stew. \"They built the door with three signs on it. The sun opens it. The moon and the snake? They bite.\" She taps your bowl. \"The SUN, love. Remember it.\"", "tg_jackal", flag("sun_lore"))
                    .fail("\"Old place. Old stones. Old bones,\" Mama Odu says, and shrugs. \"Eat your stew.\" It is very good stew.", "tg_jackal", heal("1d4"))
                .choice("Join the porters' game of bone dice").icon("🎲").once().check("luck", 11).chooser()
                    .pass("The bones roll your way three times running. The porters groan and push a little pile of silver at you.", "tg_jackal", gold(15))
                    .fail("The bones hate you. The porters are very polite about taking your money.", "tg_jackal", gold(-5))
                .choice("Help the cook catch the thieving monkey").icon("🐒").once().check("acrobatics", 12).chooser()
                    .pass("You vault the bar, snag the monkey mid-leap and hand it back to the cook — along with the healing potion it had stolen from a mercenary's pack. The cook insists you keep the potion.", "tg_jackal", item("potion_healing"))
                    .fail("The monkey runs up your arm, steals your hat, and escapes through the roof. The whole room applauds the monkey.", "tg_jackal")
                .choice("Win over the insulting parrot").icon("🦜").once().check("animal_handling", 12).chooser()
                    .pass("Two crackers and a lot of patience later, the parrot lands on your shoulder and whispers, very clearly: \"Bridge bad. BRIDGE BAD.\" Then it flies away.", "tg_jackal", flag("bridge_warning"))
                    .fail("The parrot calls you something that makes Mama Odu cover the cook's ears.", "tg_jackal")
                .choice("Speak with Professor Quill").icon("📜").go("tg_professor");

        c.scene("tg_professor").chapter(PRO).title("Professor Amara Quill").backdrop("tavern").time("night").music("mystery")
                .npc("Professor Amara Quill", "Archaeologist", "#6e2c00", "🧭", "You're hired if you can walk and you don't faint. Can you walk? Good. Faint later.")
                .text("Professor Quill has ink on her fingers, a pencil behind each ear, and a voice built for lecture halls. She spreads a map across the table and pins it flat with your mugs.",
                        "\"Three hundred years ago, the last king of this coast wore a crown with a stone in it — the SUN GEM. Someone stole it and hid it in the Temple of the Lost Sun. The king went mad. The kingdom went quiet. Nobody's seen either since.\"",
                        "\"My partner, Tomas Reyes, went in after the gem two weeks ago. Tomas is brilliant and stubborn and very, very bad at coming back on time.\" Her jaw tightens. \"Find him. And if you find the gem — don't let it fall into the wrong hands. That includes yours, frankly. A hundred gold for your trouble.\"")
                .choice("\"We'll find Tomas. And the gem.\"").icon("🤝").effect(item("tg_professors_map"), flag("quill_hired")).go("tg_market")
                .choice("\"Two hundred, and we'll find him faster.\"").icon("💰").check("persuasion", 13).chooser()
                    .pass("Quill laughs for the first time. \"Done. Bring him back alive and I'll make it two hundred.\"", "tg_market", item("tg_professors_map"), flag("quill_hired"), flag("reward_up"))
                    .fail("\"One hundred,\" she says, \"and I won't tell Mama Odu you tried to haggle with a worried woman.\" Fair.", "tg_market", item("tg_professors_map"), flag("quill_hired"))
                .choice("\"What else do you know about the temple?\"").icon("🔍").once().check("investigation", 11).chooser()
                    .pass("She taps the map. \"The sun-priests guarded the gem with a door of three symbols and a skeletal guardian. Tomas's last note mentions 'stones that bite' in the jungle beyond. I assume that's a metaphor. Tomas never uses metaphors.\"", "tg_professor", flag("sun_lore"))
                    .fail("\"It's old, it's dangerous and it's been looted twice,\" she says. \"Next question.\"", "tg_professor");

        c.scene("tg_market").chapter(PRO).title("Sunreach at Dawn").backdrop("town").time("dawn").music("explore").shop().anchor("town").checkpoint()
                .text("Dawn steams off the palisade. Sunreach's single street of shops is already open, because in Sunreach the shops never really close — they just sleep in shifts.",
                        "Professor Quill hands you a pouch of travel money, a list of supplies and a look that says *come back*. Beyond the gate, a narrow trail plunges into the green.")
                .choice("Set out for the Temple of the Lost Sun").icon("🌴").effect(gold(10)).go("tg_ravine");
    }

    // =========================================================================================== chapter 1
    static void chapterOne(Campaign c) {
        c.scene("tg_ravine").chapter(CH1).title("The Ravine").backdrop("bridge").time("day").music("explore").anchor("road")
                .text("Two days into the jungle, the trail stops at the lip of a ravine so deep the river at the bottom sounds like rumour. On the far side, half-swallowed by vines, sits an ancient temple of honey-coloured stone with a great carved sun above its door.",
                        "An old rope-and-plank bridge sags across the gap, missing every third board. A goat-track of loose rock zig-zags down one side and up the other.")
                .choice("Cross the old bridge").icon("🌉").check("save:dex", 12).each()
                    .pass("You pick your way across plank by plank, never trusting any of them. The bridge sways and complains, but it holds.", "tg_temple_door")
                    .fail("A plank breaks beneath your feet! You catch the rope with one hand and haul yourself up, bruised and swearing.", "tg_temple_door", damageOne("1d6", "bludgeoning"))
                .choice("Find another route — the rocky trail").icon("🧗").check("survival", 11).group()
                    .pass("You find a safe trail down and up, where the rock is solid and the handholds are friendly.", "tg_temple_door")
                    .fail("The rocky trail cuts your hands and knees on the way down and the way up.", "tg_temple_door", damage("1d4", "slashing", "", 0))
                .choice("Follow the Professor's map to a hidden switchback").icon("🗺").ifItem("tg_professors_map").ifFlag("bridge_warning")
                    .effect(log("The parrot was right, and so was the map: a carved stair, hidden by ferns, winds safely down the ravine wall. The bridge creaks mournfully behind you, cheated."))
                    .go("tg_temple_door");

        c.scene("tg_temple_door").chapter(CH1).title("The Door of Three Symbols").backdrop("ruins").time("day").music("mystery")
                .text("The temple door is a single slab of stone twice your height. Carved into it are three great symbols, each the size of a shield and each worn smooth by centuries of nervous hands:",
                        "A blazing SUN. A crescent MOON. A coiled SERPENT.",
                        "There is no handle, no keyhole, and no doubt at all that pressing the wrong one would be a mistake.")
                .choice("Press the Sun").icon("☀").effect(flag("door_right"))
                    .effect(log("The sun symbol glows like a struck coal. With a sound like a long breath let out, the door rolls aside.")).go("tg_temple_hall")
                .choice("Press the Moon").icon("🌙").check("save:con", 12).each()
                    .pass("A burst of silver magic hammers the doorway — but you brace and weather it. Grinding, grudging, the door opens anyway.", "tg_temple_hall")
                    .fail("The wrong symbol releases a burst of magic that leaves your teeth humming and your skin smoking. The door opens anyway, like it was making a point.", "tg_temple_hall", damageOne("2d4", "radiant"))
                .choice("Press the Serpent").icon("🐍").check("save:con", 12).each()
                    .pass("Green venom-light lashes out of the stone. You shrug it off. The door, sulking, opens.", "tg_temple_hall")
                    .fail("The wrong symbol releases a burst of poisonous magic that knots your stomach. The door opens anyway, like it was making a point.", "tg_temple_hall", damageOne("2d4", "poison"))
                .choice("Study the carvings before you touch anything").icon("🔍").once().check("religion", 12).advantageIf("sun_lore").chooser()
                    .pass("The sun is carved deeper than the others, the stone around it worn by generations of priests' hands. It's the true key. The others are teeth.", "tg_temple_door", flag("door_studied"))
                    .fail("They're all carved beautifully. That's all you've got.", "tg_temple_door");

        c.scene("tg_temple_hall").chapter(CH1).title("The Hall of the Lost Sun").backdrop("hall").time("day").music("eerie")
                .text("Inside, the temple is cool and dim and smells of old incense. Shafts of sun slant through holes in the roof onto a long hall of pillars, each carved with priests holding up the sky.",
                        "At the far end, a stair climbs to an altar where something glows, gold and slow, like an ember that has never heard of going out. Before it stands a figure in gilded armour, leaning on a halberd. It has no face. It has not had one for a very long time.",
                        "As you step into the light, its skull turns towards you with a dry, patient click. Around the hall, the bones of lesser guards begin to stir.")
                .choice("Leave an offering on the sun-priests' shrine").icon("🕯").once().ifGold(5)
                    .effect(gold(-5), inspiration(), log("You lay five gold coins in the offering bowl. The sunlight in the hall brightens for a moment, warm on your face. (Heroic Inspiration!)"))
                    .go("tg_temple_hall")
                .choice("Speak to the guardian with the sun-priests' words").icon("☀").once().ifFlag("door_right").check("religion", 13).chooser()
                    .pass("\"We came by the Sun,\" you say, \"as the priests did.\" The guardian hesitates — long enough for you to take the high ground on the stair before it remembers its orders.", "tg_guardian_fight", flag("guardian_surprised"))
                    .fail("The guardian's jaw opens. Whatever it once would have said, it has forgotten. It lowers its halberd.", "tg_guardian_fight")
                .choice("Draw steel").icon("⚔").go("tg_guardian_fight");

        c.scene("tg_guardian_fight").chapter(CH1).title("The Skeletal Guardian").backdrop("hall").time("day").music("battle").checkpoint()
                .text("The Skeletal Guardian levels its halberd. Behind it, the gold light on the altar pulses like a heartbeat. The lesser bones clatter into line.")
                .choice("Fight the guardian").icon("⚔").battle("tg_guardian")
                    .victory("The guardian crumbles at last, and its bones settle into a neat pile, as if even now it wanted to leave the place tidy.", "tg_relic", xp(100));

        c.scene("tg_relic").chapter(CH1).title("The Gem").backdrop("chapel").time("day").music("mystery")
                .text("An ancient relic floats above the altar, turning slowly in a beam of sunlight: a fist-sized gem of golden topaz, full of slow fire. This is it. THE SUN GEM, prised from a king's crown three hundred years ago.",
                        "Up close, you can hear it. Not with your ears. A voice like warm honey at the back of your mind: *Take me. Wear me. You could be so much more.* And under the honey, very faint, something else — a hollow, furious echo, far away, like a man shouting in an empty palace.",
                        "There is no sign of Tomas here, only his chalk mark on the wall — an arrow pointing deeper into the jungle — and a note: *Gem is cursed. Do NOT touch. Gone ahead. — T.*")
                .choice("Destroy it").icon("💥").check("athletics", 12).chooser()
                    .pass("You bring the guardian's halberd down with everything you have. The gem cracks, then shatters in a burst of golden light — and far away, deep in the jungle, something screams. You have hurt the curse at its root.", "tg_sanctuary", flag("gem_destroyed"), flag("king_weakened"), xp(50))
                    .fail("The gem shrugs off your blow — but on the third strike it cracks, then shatters in a burst of golden light. Far away, something screams. (The effort leaves your arms shaking.)", "tg_sanctuary", flag("gem_destroyed"), flag("king_weakened"), damageOne("1d6", "force"), xp(50))
                .choice("Burn the curse out of it with holy fire").icon("🔥").spell("sacred_flame")
                    .pass("Radiant fire pours down on the gem. It screams — a man's scream, far away — and the honey-voice burns out of it. What's left shatters into harmless golden sand.", "tg_sanctuary", flag("gem_destroyed"), flag("king_weakened"), xp(75))
                .choice("Seal it away").icon("🔒").check("religion", 11).chooser()
                    .pass("You set the gem back upon the altar and speak the sun-priests' sealing words. Stone grinds closed over it, and a warm golden presence breathes through the hall: *Thank you. When you need me, I will come.*", "tg_sanctuary", flag("gem_sealed"), xp(50))
                    .fail("You seal the gem inside the altar as best you can. It isn't elegant, but the honey-voice falls silent, and a faint warmth lingers in the hall like gratitude.", "tg_sanctuary", flag("gem_sealed"), xp(25))
                .choice("Claim its power").icon("✋")
                    .effect(item("tg_sun_gem"), gold(100), xp(75), flag("gem_claimed"),
                            log("You close your hand around the Sun Gem. Power floods up your arm, warm and golden and wonderful, and every coin the priests ever left on the altar seems to roll toward you of its own accord (+100 gold). Somewhere far away, in an empty palace, someone turns his head."))
                    .go("tg_sanctuary");

        c.scene("tg_sanctuary").chapter(CH1).title("A Hidden Sanctuary").backdrop("chapel").time("dusk").music("camp").checkpoint()
                .onEnter(level(2))
                .text("Behind the altar you find a hidden sanctuary: a small room with a spring of clear water, stone benches, and the faded paintings of priests who once slept here between prayers.",
                        "It's safe — or safe enough. Outside, the jungle is turning gold, then grey. Your arms ache from the fight. **(Every hero reaches level 2!)**",
                        "Among the guardian's bones, something glints.")
                .choice("Search the guardian's remains for a weapon").icon("🗡").once().check("investigation", 11).chooser()
                    .pass("Wrapped in the guardian's cloak is a cache of the temple's finest blades, still bright after three centuries.", "tg_weapons_1")
                    .fail("You search the remains, but find no usable weapon — just bones, dust and a very surprised scorpion.", "tg_sanctuary")
                .choice("Short rest — tend your wounds and press on").icon("☕").effect(shortRest()).go("tg_ch1_end")
                .choice("Long rest — sleep here tonight, and risk what follows you").icon("🌙").check("survival", 12).group()
                    .pass("You take turns on watch. Twice something moves in the dark beyond the door. Twice it thinks better of it. You wake fully rested.", "tg_ch1_end", longRest())
                    .fail("You build a small fire and try to sleep. Something has been following you through the darkness…", "tg_ambush_sanctuary");

        c.scene("tg_weapons_1").chapter(CH1).title("The Temple's Blades").backdrop("chapel").time("dusk").music("hope")
                .text("Laid out on the guardian's cloak: a greatsword with a sunburst pommel, a pair of black daggers, and a pale longbow strung with silver. Each one hums faintly in the hand of the right owner.")
                .choice("Claim the Sunforged Greatsword").icon("⚔").once().ifClass("fighter").effect(item("tg_sunforged_greatsword")).go("tg_weapons_1")
                .choice("Claim the Shadowfang Daggers").icon("🗡").once().ifClass("rogue").effect(item("tg_shadowfang_daggers")).go("tg_weapons_1")
                .choice("Claim the Moonpiercer Longbow").icon("🏹").once().ifClass("ranger").effect(item("tg_moonpiercer_longbow")).go("tg_weapons_1")
                .choice("Take whichever suits you best").icon("✨").once().effect(item("weapon_plus_one")).go("tg_weapons_1")
                .choice("Back to the sanctuary").icon("↩").go("tg_sanctuary");

        c.scene("tg_ambush_sanctuary").chapter(CH1).title("Random Encounter!").backdrop("ruins").time("night").music("tension")
                .text("You wake to the crunch of a boot on gravel and a low growl in the dark. A bandit, a dire wolf and a rattling skeleton have all followed your firelight to the same place — and none of them is pleased to share.")
                .choice("Grab your weapons!").icon("⚔").battle("tg_ambush")
                    .victory("You survive the encounter. The rest of the night is quiet, and you manage to recover your strength.", "tg_ch1_end", longRest(), xp(40));

        c.scene("tg_ch1_end").chapter(CH1).title("Chapter 1 Complete").backdrop("ruins").time("dawn").music("hope")
                .text("Morning finds you on the temple steps with Tomas's chalk arrows leading away into the trees. Beyond them the jungle grows darker, thicker and stranger — mist curls between the trunks, and the birdsong stops as though someone has shut a door on it.",
                        "Somewhere in there is Tomas Reyes. Somewhere past that, the Forgotten Kingdom.")
                .choice("Follow Tomas's arrows into the Cursed Jungle").icon("🌴").go("tg_jungle");
    }

    // =========================================================================================== chapter 2
    static void chapterTwo(Campaign c) {
        c.scene("tg_jungle").chapter(CH2).title("The Cursed Jungle").backdrop("forest").time("day").weather("fog").music("eerie")
                .text("The jungle is thick with mist and the sound of distant cries. Every tree is furred with moss; every leaf drips. The light is green and the shadows are greener.",
                        "Under a fallen tree, half-hidden in ferns, a man in a torn explorer's coat raises one bloody hand. \"Oh, thank the Morning Lord,\" he croaks. \"Tell me Amara sent you. Tell me she's furious. It means she's not worried.\"",
                        "It's TOMAS REYES — and his leg is badly hurt.")
                .choice("Help him — bind his wounds").icon("🩹").check("medicine", 10).chooser()
                    .pass("You splint the leg and stop the bleeding. Tomas sags with relief and presses a potion and a purse into your hands. \"Amara gave me these. I clearly wasn't using them properly.\"", "tg_tomas_talk", item("potion_healing"), gold(15), flag("tomas_saved"))
                    .fail("It's ugly work, but you get the leg splinted. Tomas presses a potion and a purse on you anyway. \"For the trouble. And the screaming. Sorry about the screaming.\"", "tg_tomas_talk", item("potion_healing"), gold(15), flag("tomas_saved"))
                .choice("Heal him with magic").icon("✨").spell("cure_wounds")
                    .pass("Warm light knits the bone. Tomas flexes his leg in wonder and pushes a potion and a purse into your hands. \"I owe you a drink. Possibly a building.\"", "tg_tomas_talk", item("potion_healing"), gold(15), flag("tomas_saved"), flag("tomas_strong"))
                .choice("Keep moving").icon("🚶")
                    .effect(log("You keep your distance and move deeper into the jungle. Behind you, Tomas calls out once — then goes quiet."))
                    .go("tg_stones");

        c.scene("tg_tomas_talk").chapter(CH2).title("Tomas Reyes").backdrop("forest").time("day").weather("fog").music("mystery")
                .npc("Tomas Reyes", "The Professor's partner", "#1a5276", "🧭", "Did you touch the gem? Please tell me you didn't touch the gem.")
                .text("Tomas talks fast, the way people do when they've been alone too long. \"The gem is the king's heart — or the piece of it that stayed good. Without it he rotted. With it…\" He shudders. \"With it he'd be whole again. And he'd remember everything.\"",
                        "\"There's a champion in a clearing ahead — a skeleton in a crown of vines. It did this to my leg. And the path before it is paved with stones that bite. Step only on the ones with moss. The moss knows.\"",
                        "\"Go. I'll limp back to Sunreach. Tell Amara… tell her I said she was right. She'll enjoy that.\"")
                .choice("\"Get home safe, Tomas.\"").icon("🤝").effect(flag("moss_tip")).go("tg_stones")
                .choice("Show him the Sun Gem").icon("💎").once().ifItem("tg_sun_gem")
                    .effect(log("Tomas goes white. \"You — oh no. Oh no, no. Listen to me. When you meet the King, he will want it back more than he wants to breathe. Throw it at him. Break it on his crown. It's the only thing that can hurt him from the inside.\""), flag("gem_secret"))
                    .go("tg_tomas_talk");

        c.scene("tg_stones").chapter(CH2).title("The Stones That Bite").backdrop("forest").time("day").weather("fog").music("tension")
                .text("You reach a path covered with strange stones — flat and grey, laid in a neat line through a stretch of jungle where nothing at all is growing. Not a fern. Not a weed. In the trees on either side, little brass tubes peep out of the bark.")
                .choice("Cross the stones carefully").icon("🪨").check("save:dex", 13).each()
                    .pass("You dodge the hidden traps, dancing from stone to stone as poison darts hiss past.", "tg_champion_clearing")
                    .fail("Poison darts strike from the trees! You stumble through, stung and woozy.", "tg_champion_clearing", damageOne("2d4", "poison"))
                .choice("Step only on the mossy stones").icon("🌿").ifFlag("moss_tip").check("perception", 8).group()
                    .pass("Tomas was right. The moss only grows where the stones don't click. You cross without a single dart flying.", "tg_champion_clearing")
                    .fail("Mostly right. One dart finds a shoulder, but it's only one.", "tg_champion_clearing", damageOne("1d4", "poison"))
                .choice("Search for a safer route").icon("🔍").check("survival", 11).group()
                    .pass("You find a safe path around the stones, along a dry streambed.", "tg_champion_clearing")
                    .fail("You stumble into a thorn patch and come out the other side looking like an angry pincushion.", "tg_champion_clearing", damage("1d6", "piercing", "", 0))
                .choice("Burn the dart tubes out of the trees").icon("🔥").spell("fire_bolt")
                    .pass("A few precise bolts of fire, and the brass tubes pop like corn. The stones click harmlessly as you cross.", "tg_champion_clearing");

        c.scene("tg_champion_clearing").chapter(CH2).title("The Champion's Clearing").backdrop("grove").time("dusk").weather("fog").music("tension").checkpoint()
                .text("The stones lead to a clearing ringed by broken pillars. In the middle, sitting on a fallen column with a greatsword across its knees, is a skeleton in a crown of living vines. Moss fills its eye-sockets. Flowers grow from its ribs.",
                        "It has the patience of something that has won every fight it ever had. It stands. Around the clearing, more bones climb out of the leaf-litter, and something large and many-legged shifts in the mist.")
                .choice("Creep around the edge and strike first").icon("🤫").check("stealth", 13).group()
                    .pass("You slip through the mist to the champion's flank before it finds you. Strike now!", "tg_champion_go", flag("champion_surprised"))
                    .fail("A twig snaps. The champion's head turns towards you with a creak of old vine.", "tg_champion_go")
                .choice("Challenge it openly").icon("⚔").go("tg_champion_go");

        c.scene("tg_champion_go").chapter(CH2).title("The Skeletal Champion").backdrop("grove").time("dusk").weather("fog").music("battle")
                .text("The Skeletal Champion raises its greatsword in a salute older than the kingdom. Then it charges.")
                .choice("Fight!").icon("⚔").battle("tg_champion")
                    .victory("The champion falls, and the vines that held it together go limp. For a moment the clearing is perfectly quiet — then, one by one, the birds begin to sing again.", "tg_jungle_camp", xp(150), gold(25));

        c.scene("tg_jungle_camp").chapter(CH2).title("A Sheltered Jungle Campsite").backdrop("camp").time("night").music("camp").shop().anchor("camp").checkpoint()
                .aside("Have you noticed nobody in this story ever asks how the merchant got here? Wise. Very wise.")
                .npc("The Secret Merchant", "Purveyor of Fine Goods", "#7d3c98", "🎒", "Ah, customers! With actual spending money! Don't mind the bag. Everyone minds the bag.")
                .text("You make camp in the lee of a great fig tree, where an old explorers' shelter still has most of a roof. You're not the first to camp here: someone has a fire going already, and a folding table, and a sign that says OPEN.",
                        "Behind the table sits a cheerful merchant with a suspiciously modern-looking bag. You did not see them arrive. You will never see them arrive.",
                        "Through the trees to the north, faint and blue, you can see the towers of a forgotten kingdom.")
                .choice("Buy the Decanter of Boundless Coffee (50 gold)").icon("☕").once().ifGold(50)
                    .effect(gold(-50), item("tg_decanter_of_coffee"), log("It produces enough coffee to survive three operations meetings and an endless shift-change handoff."))
                    .go("tg_jungle_camp")
                .choice("Search the champion's clearing for a weapon").icon("🗡").once().check("investigation", 12).chooser()
                    .pass("Under the champion's bones you find its rack of weapons, cleaned by the rain and waiting.", "tg_weapons_2")
                    .fail("You search the remains, but find no usable weapon. The vines are very protective.", "tg_jungle_camp")
                .choice("Short rest — eat, patch up, and move on at dawn").icon("☕").effect(shortRest()).go("tg_ch2_end")
                .choice("Long rest — sleep the night, and risk the jungle").icon("🌙").check("survival", 12).group()
                    .pass("The merchant's fire keeps the night at bay. You sleep like stones and wake fully rested.", "tg_ch2_end", longRest())
                    .fail("You settle in for a long rest… and wake to find the merchant gone, the fire out, and something circling the camp.", "tg_ambush_camp");

        c.scene("tg_weapons_2").chapter(CH2).title("The Champion's Arms").backdrop("camp").time("night").music("hope")
                .text("A warblade with a rusted guard that has stayed somehow sharp. A pair of blades that flicker half out of the world. A recurve bow of dark horn.")
                .choice("Claim the Champion's Warblade").icon("⚔").once().ifClass("fighter").effect(item("tg_champions_warblade")).go("tg_weapons_2")
                .choice("Claim the Phantom Assassin Blades").icon("🗡").once().ifClass("rogue").effect(item("tg_phantom_blades")).go("tg_weapons_2")
                .choice("Claim the Dragonstalker's Bow").icon("🏹").once().ifClass("ranger").effect(item("tg_dragonstalker_bow")).go("tg_weapons_2")
                .choice("Back to the fire").icon("↩").go("tg_jungle_camp");

        c.scene("tg_ambush_camp").chapter(CH2).title("Random Encounter!").backdrop("camp").time("night").music("tension")
                .text("!!! RANDOM ENCOUNTER !!! Something has been following you through the darkness — and it brought friends.")
                .choice("To arms!").icon("⚔").battle("tg_ambush")
                    .victory("You survive the encounter, but the interruption costs you time. You manage to recover your strength before dawn.", "tg_ch2_end", longRest(), xp(40));

        c.scene("tg_ch2_end").chapter(CH2).title("Chapter 2 Complete").backdrop("forest").time("dawn").music("hope")
                .text("The jungle thins. The mist lifts. And there, filling the valley below you, is a city of black stone and broken towers, its walls wrapped in a cold blue haze that doesn't move with the wind.",
                        "Nobody has walked these streets for three hundred years. Something is walking them now.")
                .choice("Descend into the Forgotten Kingdom").icon("🏰").go("tg_city_wall");
    }

    // =========================================================================================== chapter 3
    static void chapterThree(Campaign c) {
        c.scene("tg_city_wall").chapter(CH3).title("The Forgotten Kingdom").backdrop("ruins").time("dusk").weather("ash").music("ruins").anchor("dungeon").checkpoint()
                .onEnter(level(3), longRest())
                .text("You emerge before a ruined city surrounded by a black stone wall. Banners that were once gold hang in grey rags from every tower. Blue light flickers in the high windows of the palace at its heart. **(Every hero reaches level 3, rested and ready.)**",
                        "The gates are sealed, but three possible entrances remain: the great main gate, a narrow drainage tunnel half-flooded with black water, and a broken watchtower whose shattered upper window still gapes open.")
                .choice("Fighter: force open the main gate with a mighty shove").icon("💪").ifClass("fighter")
                    .effect(gold(10), flag("gate_forced"), log("You set your shoulder to the gate and SHOVE. Three hundred years of rust give way at once. Among the fallen guards' bones beyond, you find 10 gold."))
                    .go("tg_hall_kings")
                .choice("Force open the main gate").icon("🚪").check("athletics", 14).chooser()
                    .pass("With a mighty shove, you force the gate open! Among the fallen guards you find 10 gold.", "tg_hall_kings", gold(10), flag("gate_forced"))
                    .fail("The gate gives way suddenly, crushing your shoulder against the frame.", "tg_hall_kings", damageOne("2d6", "bludgeoning"))
                .choice("Rogue: slip through the drainage tunnel").icon("🗝").ifClass("rogue")
                    .effect(gold(15), log("You slip through quietly, spotting every pressure plate and tripwire. In a smuggler's stash halfway along you find 15 gold."))
                    .go("tg_hall_kings")
                .choice("Slip through the drainage tunnel").icon("🌊").check("save:dex", 13).each()
                    .pass("You slip through quietly and avoid the traps.", "tg_hall_kings")
                    .fail("A hidden blade snaps out of the wall!", "tg_hall_kings", damageOne("1d8", "slashing"))
                .choice("Ranger: climb the broken watchtower").icon("🏹").ifClass("ranger")
                    .effect(item("potion_healing"), log("You climb safely, hand over hand, and from the top you spot a guards' supply cache. Inside: a healing potion, still sealed."))
                    .go("tg_hall_kings")
                .choice("Climb the broken watchtower").icon("🧗").check("athletics", 13).each()
                    .pass("You climb safely and enter through the shattered window.", "tg_hall_kings")
                    .fail("A loose stone gives way beneath your foot, and you bang down a dozen feet before you catch yourself.", "tg_hall_kings", damageOne("1d10", "bludgeoning"));

        c.scene("tg_hall_kings").chapter(CH3).title("The Hall of Fallen Kings").backdrop("hall").time("night").music("eerie")
                .npc("The Ghost Knight", "Last of the Royal Guard", "#5dade2", "🛡", "Only those who prove their worth may approach the throne.")
                .text("You enter the Hall of Fallen Kings. Statues of a dozen crowned men and women line the walls, their faces chiselled off. At the far end, before a pair of great bronze doors, a knight in translucent blue armour stands with his sword point-down on the floor.",
                        "His voice is cold and tired and very old. \"Only those who prove their worth may approach the throne.\"",
                        "Behind him, the floor is cracked; one statue near the wall has fallen, and there is a dark gap behind it.")
                .choice("Challenge the knight to combat").icon("⚔").battle("tg_knight")
                    .victory("The knight drops to one knee. \"Worthy,\" he says, and bows before fading into mist. \"When you face him… I will be there.\"", "tg_barracks", xp(100), gold(20), flag("knight_ally"))
                .choice("Attempt to reason with the spirit").icon("🗣").check("persuasion", 15).advantageIf("tomas_saved").chooser()
                    .pass("You tell him about the gem, the curse, the king his oath has trapped. The knight is silent a long time. \"…My king died three hundred years ago,\" he says at last. \"What sits on the throne is not him.\" He steps aside and presses 30 gold of old coin into your hand. \"End it. I will stand with you.\"", "tg_barracks", gold(30), xp(75), flag("knight_ally"))
                    .fail("The knight sees through your words and attacks!", "tg_knight_angry")
                .choice("Bind his will with a charm").icon("💗").spell("charm_person")
                    .pass("Your magic finds the man inside the oath. The knight lowers his blade. \"Strange,\" he says. \"I had forgotten what it is like to be spoken to kindly.\" He steps aside, and his ghostly coins chime into your hand.", "tg_barracks", gold(30), xp(75), flag("knight_ally"))
                .choice("Search the hall for a way around him").icon("🔦").check("investigation", 14).chooser()
                    .pass("You discover a hidden passage behind the fallen statue. Inside, you find a potion and 20 gold — and a stair that comes out well beyond the knight.", "tg_barracks", item("potion_healing"), gold(20), xp(50))
                    .fail("The passage collapses! A stone catches you on the way out — and the noise wakes the hall's other guardian.", "tg_hall_collapse", damageOne("2d6", "bludgeoning"));

        c.scene("tg_knight_angry").chapter(CH3).title("The Angered Ghost Knight").backdrop("hall").time("night").music("battle")
                .text("\"LIARS!\" The knight's blade comes up blazing blue. All around the hall, the hollow soldiers of the royal guard step out of the walls to answer his call.")
                .choice("Defend yourselves!").icon("⚔").battle("tg_knight_angry")
                    .victory("The knight falls back into mist, his voice fading: \"…Perhaps… you were… telling the truth…\"", "tg_barracks", xp(75), gold(10));

        c.scene("tg_hall_collapse").chapter(CH3).title("The Hall Guardian").backdrop("hall").time("night").music("battle")
                .text("Dust is still falling when one of the great statues pulls its feet off its plinth with a sound like a quarry being torn in half. It has no face. It doesn't need one.")
                .choice("Fight the Hall Guardian").icon("⚔").battle("tg_hall_guardian")
                    .victory("The statue topples and shatters across the floor. Beyond the rubble, the knight is gone, and the way is open.", "tg_barracks", xp(75), gold(10));

        c.scene("tg_barracks").chapter(CH3).title("The Royal Guard's Barracks").backdrop("dungeon").time("night").music("camp").checkpoint()
                .text("You reach the royal guard's abandoned barracks: long rows of bunks, a dead hearth, armour stands holding nothing but dust. It's quiet here. The blue light doesn't reach.",
                        "Beyond the barracks, three doors lead deeper into the palace. An inscription above them reads: *'The crown seeks wisdom, the sword seeks courage, and the eye seeks truth.'*")
                .choice("Search the guards' old infirmary").icon("🧪").once().check("medicine", 10).chooser()
                    .pass("Behind a cracked cabinet: two healing potions, wax seals unbroken after three hundred years. The royal guard took good care of its own.", "tg_barracks", item("potion_healing"), item("potion_healing"))
                    .fail("Dust, bandages that crumble at a touch, and one potion that's mostly sediment — but it'll still do the job.", "tg_barracks", item("potion_healing"))
                .choice("Short rest before the doors").icon("☕").once().effect(shortRest()).go("tg_three_doors")
                .choice("Go straight to the doors").icon("🚪").go("tg_three_doors");

        c.scene("tg_three_doors").chapter(CH3).title("Three Doors").backdrop("hall").time("night").music("mystery")
                .text("*'The crown seeks wisdom, the sword seeks courage, and the eye seeks truth.'*",
                        "A red door marked with a sword. A golden door marked with a crown. A blue door marked with an eye. Somewhere behind all three, a long way off, you hear a slow, heavy heartbeat of stone.")
                .choice("The red door marked with a sword").icon("⚔").go("tg_armory")
                .choice("The golden door marked with a crown").icon("👑").go("tg_treasury")
                .choice("The blue door marked with an eye").icon("👁").go("tg_observatory");

        c.scene("tg_armory").chapter(CH3).title("The Royal Armory").backdrop("dungeon").time("night").music("tension")
                .text("The sword door opens into the Royal Armory. Armour and weapons litter the floor, racks of spears line the walls — and in the centre, a twelve-foot statue of granite holding a halberd opens eyes that glow like coals.",
                        "Around it, swords rise from their racks and hang in the air, points turning towards you.")
                .choice("Face the Royal Stone Guardian").icon("⚔").battle("tg_armory")
                    .victory("The guardian shatters, revealing a treasure chest beneath its plinth — 40 gold, and the armory's finest weapons.", "tg_weapons_3", xp(175), gold(40));

        c.scene("tg_weapons_3").chapter(CH3).title("The Armory's Treasures").backdrop("dungeon").time("night").music("hope")
                .text("In the chest, wrapped in oilcloth: a greatblade forged for the royal guard, a pair of daggers that leave lines of starless black in the air, and a longbow whose draw weight is best described as 'yes'.")
                .choice("Claim the Kingslayer Greatblade").icon("⚔").once().ifClass("fighter").effect(item("tg_kingslayer_greatblade")).go("tg_weapons_3")
                .choice("Claim the Voidstrike Daggers").icon("🗡").once().ifClass("rogue").effect(item("tg_voidstrike_daggers")).go("tg_weapons_3")
                .choice("Claim the Worldender Longbow").icon("🏹").once().ifClass("ranger").effect(item("tg_worldender_longbow")).go("tg_weapons_3")
                .choice("Take a royal blade for whoever needs it").icon("✨").once().effect(item("weapon_plus_one")).go("tg_weapons_3")
                .choice("On to the throne room").icon("👑").go("tg_throne_approach");

        c.scene("tg_treasury").chapter(CH3).title("The Royal Treasury").backdrop("hoard").time("night").music("mystery")
                .text("The crown door leads into the Royal Treasury. It has been looted — twice, by the look of the skeletons — but one thing remains: a cursed chest on a pedestal beneath an empty golden crown-stand.",
                        "The chest is very beautiful. The skeletons around it are very dead.")
                .choice("Open the chest").icon("📦").check("luck", 11).chooser()
                    .pass("The chest opens safely. You find a royal fortune!", "tg_throne_approach", gold(80), xp(100))
                    .fail("The chest is trapped! A burst of fire fills the room.", "tg_treasury_fire")
                .choice("Inspect the chest for magic").icon("🔍").check("investigation", 14).chooser()
                    .pass("You find the rune, nudge it sideways with a hairpin, and disarm the trap. The lid lifts on a royal fortune.", "tg_throne_approach", gold(80), xp(100))
                    .fail("You trigger the trap while inspecting it!", "tg_treasury_fire")
                .choice("Pick the lock with thieves' tools").icon("🗝").ifItem("thieves_tools").check("sleight_of_hand", 12).chooser()
                    .pass("Click. Click. *Click.* The trap disarms with a disappointed hiss, and the lid lifts on a royal fortune.", "tg_throne_approach", gold(80), xp(100))
                    .fail("Click. Click. *Whoosh.*", "tg_treasury_fire")
                .choice("Leave it alone").icon("🚶").effect(xp(25), log("You leave the chest untouched and move on. The skeletons seem to approve.")).go("tg_throne_approach");

        c.scene("tg_treasury_fire").chapter(CH3).title("Fire!").backdrop("hoard").time("night").music("tension")
                .text("Jets of flame roar out of the crown-stand and sweep the room!")
                .choice("Dive for cover!").icon("🔥").check("save:dex", 13).each()
                    .pass("You dive behind a pillar as the fire washes past.", "tg_treasury_after")
                    .fail("The fire catches you before you can get clear.", "tg_treasury_after", damageOne("2d8", "fire"));

        c.scene("tg_treasury_after").chapter(CH3).title("Smoke and Gold").backdrop("hoard").time("night").music("mystery")
                .text("When the smoke clears, the chest stands open. The trap has spent itself. Half the fortune inside has melted into a lump, but half has not.")
                .choice("Take what's left and move on").icon("💰").effect(gold(40), xp(50)).go("tg_throne_approach");

        c.scene("tg_observatory").chapter(CH3).title("The Observatory").backdrop("chapel").time("night").music("mystery")
                .text("The eye door opens into the Observatory. Three tall mirrors direct beams of moonlight towards a sealed magical seal in the floor. A riddle is carved around it:",
                        "*\"I speak without a mouth and answer without ears. I have no body, but I come alive with the wind.\"*")
                .choice("\"A shadow.\"").icon("🌑").go("tg_observatory_wrong")
                .choice("\"An echo.\"").icon("🔊").effect(heal("2d8"), gold(25), xp(100),
                        log("The mirrors align! The seal opens, and a healing aura surrounds you, warm as morning."))
                    .go("tg_throne_approach")
                .choice("\"A dream.\"").icon("💭").go("tg_observatory_wrong")
                .choice("Read the old astronomers' notes for a hint").icon("📖").once().check("arcana", 13).chooser()
                    .pass("A scribbled margin note: *'— tested the riddle on the apprentices. Ran up and down the hall shouting the answer. Hall shouted it back.'*", "tg_observatory")
                    .fail("The notes are mostly star charts and complaints about the food.", "tg_observatory");

        c.scene("tg_observatory_wrong").chapter(CH3).title("Blinding Light").backdrop("chapel").time("night").music("tension")
                .text("The mirrors flare with blinding light!")
                .choice("Shield your eyes!").icon("✋").check("save:con", 13).each()
                    .pass("You turn away just in time. Then you force the seal open through sheer determination.", "tg_throne_approach", xp(50))
                    .fail("The light sears you. Gritting your teeth, you force the seal open through sheer determination.", "tg_throne_approach", damageOne("2d6", "radiant"), xp(50));
    }

    // =========================================================================================== finale
    static void finale(Campaign c) {
        c.scene("tg_throne_approach").chapter(FIN).title("The Throne Room").backdrop("lair").time("night").music("boss").checkpoint()
                .text("You finally reach the throne room. Blue fire burns without heat in two great braziers on either side of a throne of black stone. The forgotten king sits motionless upon it, a crown with one empty setting on his brow.",
                        "His eyes ignite with blue fire.",
                        "\"You have disturbed my kingdom for the last time!\" The king rises, wielding a blade of dark magic.")
                .choice("\"Your kingdom ended three hundred years ago.\" Fight!").icon("⚔").unlessFlag("gem_claimed").go("tg_king_fight")
                .choice("Hurl the Sun Gem at his crown!").icon("💎").ifItem("tg_sun_gem")
                    .effect(takeItem("tg_sun_gem"), flag("king_weakened"),
                            log("You hurl the Sun Gem with everything you have. It strikes the empty setting of his crown and SHATTERS in a burst of golden fire. The King staggers, clutching his head, and for a moment his own voice cries out from inside the curse. (The Cursed King is weakened!)"))
                    .go("tg_king_fight")
                .choice("Keep the gem — and fight him for the crown too").icon("✋").ifFlag("gem_claimed")
                    .effect(log("\"MINE,\" the King breathes, and the gem in your pack flares hot enough to hurt. He can feel it. He will never stop coming for it."), damage("1d6", "fire", "", 0))
                    .go("tg_king_fight");

        c.scene("tg_king_fight").chapter(FIN).title("The Cursed King").backdrop("lair").time("night").music("boss")
                .text("The Cursed King descends from his throne. The blue braziers roar. Somewhere in the walls, the palace's dead begin to stir.")
                .choice("For the kingdom!").icon("⚔").battle("tg_cursed_king")
                    .victory("", "tg_king_falls", xp(250), gold(75));

        c.scene("tg_king_falls").chapter(FIN).title("The King Falls").backdrop("lair").time("night").music("sorrow")
                .text("The Cursed King falls to his knees. The blue fire fades from his eyes, and for one moment he looks only like a very old, very tired man. \"Thank you,\" he whispers — and then he is dust, and the crown rolls across the floor and stops at your feet.",
                        "The palace begins to tremble.",
                        "You have three choices.")
                .choice("Take the crown and rule the kingdom").icon("👑")
                    .effect(flag("ending_crowned"), gold(100), xp(100), log("You take the crown and claim the throne. The remaining spirits kneel before their new ruler. Your new kingdom grants you 100 gold."))
                    .go("tg_escape")
                .choice("Destroy the crown and free the kingdom").icon("💥")
                    .effect(flag("ending_freed"), xp(150), log("You shatter the crown. A wave of light sweeps through the kingdom. The restless spirits are finally free."))
                    .go("tg_escape")
                .choice("Leave the crown and disappear into legend").icon("🌫")
                    .effect(flag("ending_legend"), xp(100), log("You leave the crown untouched. Your names will become a legend told by travellers for generations."))
                    .go("tg_escape");

        c.scene("tg_escape").chapter(FIN).title("The Palace Trembles").backdrop("lair").time("night").music("battle")
                .text("Cracks race up the walls. Pillars groan. Three hundred years of curse are coming undone all at once, and the palace intends to come down with it.")
                .choice("RUN!").icon("🏃").check("athletics", 12).group()
                    .pass("You sprint through falling stone and burst out into the open air as the palace folds in on itself behind you, in a column of dust lit gold by the rising sun.", "tg_epilogue")
                    .fail("Falling masonry batters you all the way down the stairs, but you make it — bruised, filthy and alive — as the palace folds in on itself behind you.", "tg_epilogue", damage("2d6", "bludgeoning", "dex", 12));

        c.scene("tg_epilogue").chapter("Epilogue").title("Back at the Thirsty Jackal").backdrop("tavern").time("night").music("triumph").anchor("epilogue")
                .onEnter(gold(100), longRest())
                .npc("Professor Amara Quill", "Archaeologist", "#6e2c00", "🧭", "You came back. Everyone said you wouldn't. I said you would. I'd like that noted.")
                .text("The Thirsty Jackal is louder than you've ever heard it. Word has run ahead of you, the way it does in Sunreach: the blue light over the Forgotten Kingdom has gone out. The porters have made up a song already. It's terrible. Everyone sings it.",
                        "Professor Quill pays every coin she promised, plus a little extra she pretends she forgot to count.",
                        "Congratulations — you survived all three chapters.")
                .choice("Toast Tomas, who made it home on his own two legs").icon("🍻").once().ifFlag("tomas_saved")
                    .effect(log("Tomas limps over on a crutch and raises a mug. \"Amara says she was right. I said I said she was right. It's going very well.\" Quill rolls her eyes and holds his hand under the table."))
                    .go("tg_epilogue")
                .choice("Ask after Tomas").icon("🕯").once().unlessFlag("tomas_saved")
                    .effect(log("Tomas crawled back into Sunreach two days ago, half-starved and furious, and has been asleep ever since. Quill hasn't left his side. \"He's alive,\" she says. \"No thanks to anyone.\" It stings. It's meant to."))
                    .go("tg_epilogue")
                .choice("Tell Mama Odu you wear a crown now").icon("👑").once().ifFlag("ending_crowned")
                    .effect(log("\"Your Majesty,\" Mama Odu says, and curtseys, and charges you double for everything. \"Royal prices.\""))
                    .go("tg_epilogue")
                .choice("Listen to the spirits' song on the wind").icon("🌅").once().ifFlag("ending_freed")
                    .effect(log("Out past the palisade, the jungle is singing — birds, wind, and something like a choir of very old, very happy voices, going home at last."))
                    .go("tg_epilogue")
                .choice("Hear a stranger tell your legend wrong").icon("📖").once().ifFlag("ending_legend")
                    .effect(log("A traveller at the bar is telling the tale of the heroes who beat the Cursed King and walked away from his crown. He gets every detail wrong. You don't correct him. It's a better story his way."))
                    .go("tg_epilogue")
                .choice("Collect the extra reward Quill promised").icon("💰").once().ifFlag("reward_up").ifFlag("tomas_saved").effect(gold(100)).go("tg_epilogue")
                .choice("Follow a strange figure with a suspiciously modern-looking bag…").icon("🎒").once().ifGold(100).go("tg_secret_merchant")
                .choice("Raise a mug and end the tale").icon("🏁").go("@end");

        // Joel's Secret Merchant, as written — only for customers with actual spending money.
        c.scene("tg_secret_merchant").chapter("Epilogue").title("Secret Merchant Found!").backdrop("tavern").time("night").music("tavern")
                .npc("The Secret Merchant", "Purveyor of Fine Goods", "#7d3c98", "🎒", "Ah, a customer with actual spending money!")
                .text("\"Ah, a customer with actual spending money!\" The merchant opens a suspiciously modern-looking bag.")
                .choice("Bag of Holding — 75 gold").icon("🎒").once().ifGold(75)
                    .effect(gold(-75), item("tg_bag_of_holding"),
                            log("You purchase the Bag of Holding. Inside are SmartTab terminals, tablets, card readers, tangled cables, and one support ticket from 2019. \"Have you tried turning the dungeon off and on again?\""))
                    .go("tg_secret_merchant")
                .choice("Decanter of Boundless Coffee — 50 gold").icon("☕").once().ifGold(50)
                    .effect(gold(-50), item("tg_decanter_of_coffee"),
                            log("You purchase the Decanter of Boundless Coffee. It produces enough coffee to survive three operations meetings and an endless shift-change handoff."))
                    .go("tg_secret_merchant")
                .choice("Walk away").icon("🚶")
                    .effect(log("You walk away. The merchant mutters about missed profits."))
                    .go("tg_epilogue");
    }

    // =========================================================================================== battle maps
    static void encounters(Campaign c) {
        c.encounter("tg_guardian").title("The Skeletal Guardian").backdrop("hall").music("battle").objective("defeat:tg_skeletal_guardian")
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
                .legend('g', "tg_skeletal_guardian").legend('s', "tg_temple_skeleton").extra("tg_temple_skeleton");

        c.encounter("tg_ambush").title("Random Encounter!").backdrop("camp").music("battle")
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
                .legend('w', "tg_dire_wolf").legend('b', "tg_bandit").legend('s', "tg_temple_skeleton").extra("tg_bandit");

        c.encounter("tg_champion").title("The Skeletal Champion").backdrop("grove").music("battle").objective("defeat:tg_skeletal_champion")
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
                .legend('c', "tg_skeletal_champion").legend('s', "tg_temple_skeleton").extra("tg_jungle_spider");

        c.encounter("tg_knight").title("Trial of the Ghost Knight").backdrop("hall").music("battle").objective("defeat:tg_ghost_knight").nonlethal()
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
                .legend('k', "tg_ghost_knight");

        c.encounter("tg_knight_angry").title("The Angered Ghost Knight").backdrop("hall").music("battle").objective("defeat:tg_ghost_knight")
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
                .legend('k', "tg_ghost_knight").legend('h', "tg_hollow_soldier").extra("tg_hollow_soldier");

        c.encounter("tg_hall_guardian").title("The Hall Guardian").backdrop("hall").music("battle").objective("defeat:tg_hall_guardian")
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
                .legend('q', "tg_hall_guardian").legend('h', "tg_hollow_soldier").extra("tg_hollow_soldier");

        c.encounter("tg_armory").title("The Royal Armory").backdrop("dungeon").music("battle").objective("defeat:tg_stone_guardian")
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
                .legend('S', "tg_stone_guardian").legend('a', "tg_animated_blade").extra("tg_animated_blade");

        c.encounter("tg_cursed_king").title("The Cursed King").backdrop("lair").music("boss").objective("defeat:tg_cursed_king")
                .intro("THE CURSED KING wields a blade of dark magic and blue fire. Two SOULFIRE BRAZIERS feed him — while they burn, he heals every turn. Knock them over! He RESISTS necrotic and cold, and his command can force you to KNEEL.")
                .weakenIf("king_weakened")
                .ally("tg_knight_ally", "knight_ally")
                .ally("tg_sun_spirit", "gem_sealed")
                .phase(66, "The King roars, and the floor of the throne room splits — blue soulfire GEYSERS erupt from the cracks!",
                        Json.obj("op", "hazard", "count", 3, "type", "necrotic", "dice", "2d6", "name", "Soulfire geyser", "dc", 13),
                        Json.obj("op", "say", "text", "\"You have disturbed my kingdom for the LAST TIME!\""))
                .phase(40, "\"GUARDS!\" Hollow soldiers claw their way out of the throne-room walls!",
                        Json.obj("op", "summon", "monster", "tg_hollow_soldier", "count", 2))
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
                .legend('K', "tg_cursed_king").legend('b', "tg_soulfire_brazier").extra("tg_hollow_soldier");
    }
}
