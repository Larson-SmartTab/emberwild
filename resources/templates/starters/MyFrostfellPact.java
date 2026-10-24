/*
 * STARTER TEMPLATE — a complete, playable copy of the built-in campaign "frostfell_pact".
 *
 * To make it your own:
 *   1. Copy this file up one folder, into campaigns/ (files in starters/ are not loaded).
 *   2. Rename it (and the class) to something new, e.g. MyGreatTale.java / class MyGreatTale.
 *   3. Change the id in Campaign.create("my_frostfell_pact", ...) to a unique id, plus the title and author.
 *   4. Edit scenes, NPCs, monsters and encounters; then check it:  java -jar emberwild.jar --validate campaigns/MyGreatTale.java
 *   5. Restart Emberwild and pick your tale in the lobby. Balance fights with --battletest (see README).
 */
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
 * THE FROSTFELL PACT — a winter tale for Emberwild, written with the same Java DSL as the core campaign.
 * <pre>
 *  Prologue   The Antlered Hearth ......... a lantern festival, snowballs, hot cider, a sled race, a yak, wolves
 *  Chapter 1  The High Pass ............... an ice bridge over a chasm OR an avalanche trail and a yeti; a frozen hermit
 *  Chapter 2  The Frostfell Monastery ..... ice mephits, the Door of Four Seasons, a spirit to thaw, the Queen's huntress
 *  Chapter 3  The Glacier Throne .......... ice sentinels, the cracking Mirror Lake, a last camp under the stars
 *  Finale     The Winter Queen ............ Isolde (Rime Totems, falling icicles, summoned mephits, legendary actions)
 * </pre>
 * Eight possible battles, five booby traps, 49 scenes, levels 2 to 4. Fire is your friend up here: most of what
 * lives on the mountain is vulnerable to it, and the Winter Queen most of all.
 * <p>
 * Copy freely! Every custom id is prefixed with {@code ff_} so it can't clash with other tales.
 */
public final class MyFrostfellPact implements CampaignScript {

    // Chapter labels, kept in one place so renaming a chapter is a one-line change.
    static final String PRO = "Prologue — The Lantern Festival";
    static final String CH1 = "Chapter 1 — The High Pass";
    static final String CH2 = "Chapter 2 — The Frostfell Monastery";
    static final String CH3 = "Chapter 3 — The Glacier Throne";
    static final String FIN = "Finale — The Winter Queen";

    @Override
    public Campaign build() {
        Campaign c = Campaign.create("my_frostfell_pact", "The Frostfell Pact (Your Edition)", "Your Name Here");
        c.cover("snow");
        c.synopsis("Winter has not ended in the mountain village of Highmarch for three years. The yaks are thin, the cider "
                + "is running low, and every Midwinter the Great Lantern is snuffed out by white wolves that come down from the "
                + "Glacier Throne, where a frost witch called the Winter Queen keeps an old and terrible bargain. From a rowdy "
                + "lantern festival to an ice bridge over nothing, a frozen monastery and a throne of glacier-glass, a band of "
                + "heroes must bring back the spring — by fire, by cunning, or by finding the words a lonely queen has waited "
                + "three years to hear. Built for 1-4 players from level 2 (rising to level 4); companions fill a small party.");
        c.hook("epilogue", "A frost-bitten messenger stumbles into the feast — Highmarch has had no spring for three years", "ff_start");

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
        // ---- The star of the show: a fire-loving tiefling wizard in a mountain that hates fire.
        Hero ember = hero("ff_marisol", Json.obj(
                "name", "Marisol Cinderwick", "pronouns", "she/her", "class", "wizard", "species", "tiefling", "lineage", "infernal", "background", "sage",
                "method", "pointbuy", "scores", scores(8, 14, 14, 15, 10, 10), "increases", Json.obj("int", 2, "con", 1),
                "skills", List.of("arcana", "history", "investigation", "nature"),
                "cantrips", List.of("fire_bolt", "ray_of_frost", "shocking_grasp"),
                "spells", List.of("mage_armor", "shield", "burning_hands", "charm_person"),
                "color", "#ff7a2f", "emblem", "🔥", "hair", "#2b1a2e", "skin", "#c4473f", "eyes", "#ffcc33",
                "appearance", "Crimson skin, curled horns hung with tiny brass bells, and a scarf so long it has its own weather. Her breath steams even indoors. Especially indoors.",
                "personality", "Cheerful, chatty and permanently, theatrically cold. Treats every problem as a fascinating lecture topic, and most lectures end with something on fire.",
                "backstory", "A junior archivist of the Lyceum of Vael who wrote a very long paper titled 'On the Persistence of Unnatural Winters, With Diagrams'. Nobody read it. Then the letters from Highmarch started arriving, and Marisol packed six scarves, four books and a kettle and went to see for herself.",
                "motto", "Every winter ends. Some just need a push."));
        ember.spellbook.clear();
        ember.spellbook.addAll(List.of("mage_armor", "shield", "burning_hands", "charm_person", "chromatic_orb", "magic_missile"));
        c.pregen(ember);

        // ---- Extra heroes teammates can claim with one click.
        c.pregen(hero("ff_hilde", Json.obj(
                "name", "Hilde Ironmitten", "pronouns", "she/her", "class", "paladin", "species", "dwarf", "background", "soldier",
                "method", "pointbuy", "scores", scores(15, 12, 14, 8, 10, 13), "increases", Json.obj("str", 2, "con", 1),
                "skills", List.of("persuasion", "insight"),
                "color", "#5dade2", "emblem", "🧶", "hair", "#d35400", "skin", "#e0ac8a", "eyes", "#2e86c1",
                "appearance", "A square-built dwarf in frost-blue plate with a knitted cover over her helm, complete with a bobble.",
                "personality", "Knits during sieges. Hugs too hard. Swore an oath to the Hearth-Mother to keep every door warm, and takes it very literally.",
                "motto", "Shield up, kettle on.")));
        c.pregen(hero("ff_nib", Json.obj(
                "name", "Nib Snowberry", "pronouns", "he/him", "class", "rogue", "species", "halfling", "background", "charlatan",
                "method", "pointbuy", "scores", scores(8, 15, 14, 12, 10, 13), "increases", Json.obj("dex", 2, "cha", 1),
                "skills", List.of("acrobatics", "perception", "insight", "persuasion", "athletics", "investigation", "performance"),
                "color", "#9b59b6", "emblem", "🌰", "hair", "#f5f5f5", "skin", "#f1c9a5", "eyes", "#6c3483",
                "appearance", "A white-haired halfling in a coat of patched furs, a tray of roast chestnuts hung round his neck.",
                "personality", "Sells hot chestnuts at every festival in the north, and picks every pocket he sells them to. Gives most of it back. Mostly.",
                "motto", "Chestnuts! Warm hands, warm hearts, light purses!")));

        // ---- Companions: AI-controlled, they join only when the party is small.
        Hero ragna = hero("ff_ragna", Json.obj(
                "name", "Ragna Hollowhorn", "pronouns", "she/her", "class", "fighter", "species", "goliath", "background", "soldier",
                "method", "pointbuy", "scores", scores(15, 13, 14, 8, 12, 10), "increases", Json.obj("str", 2, "con", 1),
                "skills", List.of("perception", "survival"),
                "color", "#7f8c8d", "emblem", "🏔", "hair", "#ecf0f1", "skin", "#95a5a6", "eyes", "#48c9b0",
                "appearance", "A grey-skinned goliath mountain guide with one horn snapped short and a coil of rope over each shoulder.",
                "personality", "Has led a hundred travellers over the High Pass and lost only three, which she will tell you about whether you ask or not.",
                "motto", "The mountain doesn't hate you. It just doesn't care. I do."));
        ragna.npc = true;
        c.pregen(ragna);
        Hero fennick = hero("ff_fennick", Json.obj(
                "name", "Brother Fennick Lanternmoss", "pronouns", "he/him", "class", "cleric", "species", "human", "background", "acolyte",
                "method", "pointbuy", "scores", scores(12, 14, 13, 8, 15, 10), "increases", Json.obj("wis", 2, "cha", 1),
                "skills", List.of("medicine", "persuasion"),
                "cantrips", List.of("sacred_flame", "toll_the_dead", "guidance"),
                "spells", List.of("healing_word", "guiding_bolt", "bless", "cure_wounds"),
                "color", "#f7dc6f", "emblem", "🏮", "hair", "#7b4b2a", "skin", "#f3d2b5", "eyes", "#a04000",
                "appearance", "A gangly young priest of the Hearth-Mother, all elbows and earnestness, with a paper lantern on a pole.",
                "personality", "Anxious, kind and quietly unbreakable. Apologises to doors before he opens them.",
                "motto", "Somebody has to carry the light. It may as well be me. Sorry."));
        fennick.npc = true;
        c.pregen(fennick);
    }

    // =========================================================================================== monsters & items
    static void monsters(Campaign c) {
        // ---- Prologue
        c.monster("ff_frostjaw", "Frostjaw Brother").type("humanoid").role("melee").ac(11).hp(16).stats(15, 11, 13, 8, 9, 10).cr(0.125, 25)
                .attack("Mittened Haymaker", 5, "1d6+3", "bludgeoning")
                .attack(new Monsters.Attack("Packed Snowball", 3, "1d4", "bludgeoning").ranged(30, 60))
                .look("#5d6d7e", "⛄").blurb("One of three enormous brothers who have never once lost a snowball fight fairly.");
        c.monster("ff_sten", "\"Little\" Sten Frostjaw").type("humanoid").role("brute").ac(13).hp(36).stats(17, 10, 15, 8, 9, 11).cr(1, 200)
                .multi(2).attack("Two-Mitten Wallop", 5, "1d8+3", "bludgeoning")
                .look("#34495e", "⛄").blurb("The littlest Frostjaw. He is seven feet tall.");
        c.monster("ff_frostjaw_ally", "Ulf Frostjaw").type("humanoid").role("melee").ac(12).hp(20).stats(16, 11, 14, 8, 9, 10).cr(0.25, 0)
                .attack("Big Mitten", 5, "1d6+3", "bludgeoning").look("#85929e", "⛄").blurb("He threw a snowball at your head yesterday. Today he's throwing wolves.");
        c.monster("ff_snow_wolf", "Highmarch Snow Wolf").type("beast").role("skirmisher").ac(12).hp(11, "2d8+2").speed(40).stats(14, 15, 12, 3, 12, 6)
                .cr(0.25, 50).resist("cold").attack(new Monsters.Attack("Bite", 4, "1d6+2", "piercing").rider("prone", Ability.STR, 11))
                .trait("pack_tactics").look("#d6eaf8", "🐺").blurb("White as the drifts it hides in, and twice as cold.");
        c.monster("ff_queens_hound", "Hound of the Winter Queen").type("monstrosity").role("brute").ac(13).hp(24).speed(40).stats(16, 13, 14, 5, 12, 7)
                .cr(1, 200).immune("cold").vulnerable("fire")
                .attack(new Monsters.Attack("Rime Bite", 4, "1d8+3", "piercing").plus("1d4", "cold"))
                .special(Monsters.special("frost_breath", "Frost Breath").shape("cone", 15, 0).dmg("2d6", "cold").dc(12, Ability.DEX).recharge(6))
                .trait("pack_tactics").look("#aed6f1", "🐺").blurb("A wolf with frost for blood and a collar of living ice.");

        // ---- Chapter 1
        c.monster("ff_rimefang", "Rimefang, the Bridge-Warden").type("monstrosity").role("brute").size(2).ac(13).hp(55).speed(40).stats(17, 13, 14, 7, 12, 8)
                .cr(2, 450).immune("cold").vulnerable("fire")
                .attack(new Monsters.Attack("Bite", 5, "2d6+3", "piercing").rider("prone", Ability.STR, 12))
                .special(Monsters.special("cold_breath", "Cold Breath").shape("cone", 15, 0).dmg("3d6", "cold").dc(12, Ability.DEX).recharge(5))
                .trait("pack_tactics").look("#d0ece7", "🐺").blurb("A winter wolf who speaks like a duchess and bites like a bear trap.");
        c.monster("ff_grumblegut", "Grumblegut the Yeti").type("monstrosity").role("brute").size(2).ac(12).hp(55).speed(40).stats(18, 12, 16, 6, 12, 7)
                .cr(2, 450).multi(2).immune("cold").vulnerable("fire")
                .attack(new Monsters.Attack("Claw", 6, "1d8+4", "slashing").plus("1d4", "cold"))
                .special(Monsters.special("chilling_roar", "Chilling Roar").shape("sphere", 10, 0).dmg("2d6", "cold").dc(12, Ability.CON).recharge(6))
                .look("#f2f3f4", "🦍").blurb("Eight feet of matted white fur and a stomach that hasn't been full in three years.");
        c.monster("ff_yeti_cub", "Grumblegut Junior").type("monstrosity").role("melee").ac(11).hp(14).speed(30).stats(13, 12, 13, 5, 10, 8)
                .cr(0.25, 50).immune("cold").vulnerable("fire").attack("Tiny Claw", 3, "1d6+1", "slashing")
                .look("#fdfefe", "🐻").blurb("Adorable. Furious. Bitey.");

        // ---- Chapter 2
        c.monster("ff_kessa", "Kessa Rimeheart, the Queen's Huntress").type("humanoid").role("ranged").ac(14).hp(40).stats(12, 16, 13, 11, 14, 12)
                .cr(2, 450).saves("dex", "wis").multi(2)
                .attack(new Monsters.Attack("Rimewood Bow", 5, "1d8+3", "piercing").plus("1d4", "cold").ranged(80, 320))
                .attack(new Monsters.Attack("Hunting Knife", 5, "1d6+3", "slashing"))
                .special(Monsters.special("pinning_volley", "Pinning Volley").shape("sphere", 10, 80).dmg("2d6", "cold").dc(12, Ability.DEX).rider("slowed").recharge(5))
                .look("#5499c7", "🏹").blurb("She never misses. She wishes, tonight, that she could.");
        c.monster("ff_kessa_ally", "Kessa Rimeheart").type("humanoid").role("ranged").ac(14).hp(40).stats(12, 16, 13, 11, 14, 12)
                .cr(2, 0).multi(2)
                .attack(new Monsters.Attack("Rimewood Bow", 5, "1d8+3", "piercing").ranged(80, 320))
                .attack(new Monsters.Attack("Hunting Knife", 5, "1d6+3", "slashing"))
                .look("#76d7c4", "🏹").blurb("Hunting the cold that took her queen.");

        // ---- Chapter 3
        c.monster("ff_ice_sentinel", "Rime Sentinel").type("construct").role("melee").ac(14).hp(30).speed(30).stats(16, 10, 15, 3, 10, 3)
                .cr(1, 200).resist("piercing", "slashing").immune("cold", "poison").vulnerable("fire")
                .condImmune("charmed", "frightened", "poisoned", "paralyzed")
                .attack(new Monsters.Attack("Glacier Fist", 6, "1d8+3", "bludgeoning").plus("1d4", "cold"))
                .look("#aed6f1", "🗿").blurb("A knight carved from blue ice, still keeping a watch nobody remembers setting.");

        // ---- Finale
        c.monster("ff_rime_totem", "Rime Totem").type("object").role("object").ac(11).hp(12).speed(0).stats(10, 1, 10, 1, 1, 1)
                .cr(0, 25).immune("cold", "poison", "psychic").vulnerable("fire")
                .condImmune("charmed", "frightened", "poisoned", "paralyzed", "prone", "incapacitated")
                .attack("None", 0, "0", "cold").look("#85c1e9", "❄").blurb("A spire of humming ice that feeds the Queen's winter. Melt it!");
        c.monster("ff_isolde", "Isolde, the Winter Queen").type("humanoid").role("boss").ac(15).hp(140).speed(30).stats(10, 14, 16, 15, 14, 18)
                .cr(4, 1100).saves("con", "wis", "cha").immune("cold").vulnerable("fire").condImmune("charmed", "frightened")
                .multi(2)
                .attack(new Monsters.Attack("Rime Scepter", 7, "1d8+3", "bludgeoning").plus("1d8", "cold"))
                .attack(new Monsters.Attack("Winter's Kiss", 6, "2d6", "cold").ranged(60, 60).spell().rider("slowed", Ability.CON, 13))
                .special(Monsters.special("frostfell_breath", "Breath of the Frostfell").shape("cone", 30, 0).dmg("4d6", "cold").dc(13, Ability.CON).recharge(5))
                .special(Monsters.special("rime_nova", "Rime Nova").shape("sphere", 10, 0).dmg("2d6", "cold").dc(13, Ability.DEX).rider("slowed"))
                .trait("fed_by_objects", "legendary_resistance")
                .legendary(2, "special:rime_nova", "attack:Winter's Kiss", "move")
                .look("#d6eaf8", "👑").blurb("Once Highmarch's Lantern-Keeper. Now the crowned heart of a winter that will not end.");
        c.monster("ff_mabel", "Mabel, Festival Yak").type("beast").role("brute").ac(11).hp(30).speed(40).stats(17, 10, 15, 2, 11, 5)
                .cr(1, 0).resist("cold").attack(new Monsters.Attack("Gore", 5, "1d8+3", "piercing").rider("prone", Ability.STR, 12))
                .look("#a0522d", "🐂").blurb("She followed you up a glacier. Nobody knows how. Nobody asks.");
    }

    static void items(Campaign c) {
        c.item("ff_ashgrove_lantern", "Hrolla's Little Lantern", "quest",
                "A battered tin lantern with a candle stub inside. Elder Hrolla carried it to every festival for sixty years.", null);
        c.item("ff_spring_ember", "Ember of Spring", "wondrous",
                "A warm green spark in a bead of amber, the gift of the spring-child Lissel. +1 to spell attack rolls and spell save DC.",
                Json.obj("bonus", Json.obj("spell_attack", 1, "spell_dc", 1), "attunable", true, "value", 500));
        c.item("ff_everwarm_mittens", "Father Osk's Everwarm Mittens", "wondrous",
                "Hand-knitted, slightly singed, and never cold. +1 to initiative and +1 to Constitution saving throws.",
                Json.obj("bonus", Json.obj("initiative", 1, "con_save", 1), "attunable", true, "value", 300));
    }

    // =========================================================================================== prologue
    static void prologue(Campaign c) {
        c.scene("ff_start").chapter(PRO).title("The Last Road to Highmarch").backdrop("snow").time("dusk").music("snow")
                .onEnter(level(2))
                .text("The snow on the road to HIGHMARCH comes up to your knees, and it has no business being there. It is the middle of what should be summer. The calendars say so. The almanacs say so. The snow, unimpressed, keeps falling.",
                        "Ahead, a village of steep-roofed longhouses clings to the mountainside like barnacles on a whale. Paper lanterns bob along every eave — red, gold, green — and drums and laughter drift down through the dusk. It is the Midwinter Lantern Festival. It has been Midwinter here for three years.",
                        "Above it all, filling half the sky, rises the GLACIER THRONE: a frozen river of blue-white ice that pours off the summit like a waterfall stopped mid-scream. Something up there is watching the lanterns. You can feel it.",
                        "(You arrive as seasoned adventurers: every hero starts at **level 2**.)")
                .choice("Follow the music to the Antlered Hearth").icon("🏮").go("ff_hearth_arrive")
                .choice("Catch a snowflake and study it").icon("❄").check("arcana", 12)
                    .pass("The flake sits on your glove without melting. Under a squint it is perfect — and so is the next one, and the next. Every flake on this mountain is *exactly the same*. Natural snow doesn't do that. Snow made by a bargain does.", "ff_hearth_arrive", flag("snow_read"), xp(25))
                    .fail("It melts. Then another lands on your nose. Then one goes down your collar. You decide the study of snow can wait until you are indoors.", "ff_hearth_arrive")
                .choice("Read the tracks around the village gate").icon("🐾").check("survival", 12)
                    .pass("Wolf prints circle the palisade — big ones, frost-rimed, crossing and recrossing as if the pack were pacing. They are fresh. Whatever they're waiting for, it's tonight.", "ff_hearth_arrive", flag("wolves_expected"), xp(25))
                    .fail("Tracks. Lots of tracks. Some of them are probably yours.", "ff_hearth_arrive");

        c.scene("ff_hearth_arrive").chapter(PRO).title("The Antlered Hearth").backdrop("tavern").time("night").music("tavern")
                .onEnter(companion("ff_ragna", 3), companion("ff_fennick", 3))
                .npc("Torvald Hearthsson", "Keeper of the Antlered Hearth", "#a04000", "🍺", "IN! In, in, in! Close the door before the winter follows you like a lost dog!")
                .text("The ANTLERED HEARTH is a lodge built around a fireplace you could park a cart in. Antlers cover every wall — elk, deer, one that might have belonged to something with opinions — and from each one hangs a festival lantern. The air smells of pine smoke, wet wool and hot spiced cider.",
                        "The keeper, TORVALD HEARTHSSON, is a bear of a man who greets you with a hug you did not consent to and a mug you absolutely did. He is already weeping gently, because the bard is playing and the bard always makes him weep.",
                        "At the bar, a grey-skinned goliath with one snapped horn — RAGNA HOLLOWHORN, the pass guide — is arguing with a nervous young priest holding a paper lantern on a pole. \"Nobody climbs to the Throne, Fennick.\" \"Somebody has to, Ragna. Sorry.\"",
                        "(If your party is small, Ragna and Brother Fennick decide they're coming with you. \"Fine,\" sighs Ragna. \"But if you die, I'm telling it as a funny story.\")")
                .choice("Find a bench near the fire").icon("🪵").go("ff_hearth");

        c.scene("ff_hearth").chapter(PRO).title("The Midwinter Festival").backdrop("tavern").time("night").music("tavern").anchor("tavern")
                .text("The Antlered Hearth roars around you: drums in the corner, children chasing a greased piglet between the tables, steam rising from a cauldron of cider the size of a bathtub. Through the frosted windows you can see the village square, where the GREAT LANTERN waits on its pole, unlit, for midnight.",
                        "Nobody here has seen a spring in three years. You'd never know it from the noise. Highmarch, it seems, has decided that if it cannot be warm, it can at least be loud.")
                .choice("Enter the snowball-throwing contest").icon("☃").unlessFlag("snowball_done").go("ff_snowball")
                .choice("Accept Granny Thorvild's hot-cider challenge").icon("🍵").unlessFlag("cider_done").go("ff_cider")
                .choice("Sign up for the Widowmaker sled race").icon("🛷").unlessFlag("sled_done").go("ff_sled")
                .choice("Chat with the yak herder (and her yak)").icon("🐂").unlessFlag("yak_done").go("ff_yak")
                .choice("Listen to the festival bard").icon("🎻").unlessFlag("bard_done").go("ff_bard")
                .choice("Speak with Elder Hrolla Ashgrove").icon("🧓").unlessFlag("quest_accepted").hint("The old woman by the fire has been watching you since you came in.").go("ff_hrolla")
                .choice("Head out for the Lighting of the Great Lantern").icon("🏮").ifFlag("quest_accepted").go("ff_lantern");

        // ---- Snowballs and the Frostjaw brothers
        c.scene("ff_snowball").chapter(PRO).title("The Snowball Contest").backdrop("snow").time("night").music("tavern")
                .npc("\"Little\" Sten Frostjaw", "Undefeated Snowball Champion (Fairly, Mostly)", "#5d6d7e", "⛄", "Hit the bell on the pole. From the line. No magic. No crying. No hitting Granny.")
                .text("Behind the lodge, a brass bell hangs from a pole forty paces away. The FROSTJAW BROTHERS — Ivar, Ulf and \"Little\" Sten, who is the biggest — have rung it nineteen times tonight and are insufferable about it.",
                        "A crowd of children and well-oiled farmers chants your name, or a rough approximation of it. Sten hands you a snowball with great ceremony. It has a rock in it. You suspect this is traditional.")
                .choice("Wind up and throw with everything you've got").icon("☃").check("athletics", 13)
                    .pass("The snowball sails in a perfect arc and hits the bell with a *BWONNNG* that echoes off the mountain. The crowd goes berserk. Sten stares at the bell like it has betrayed his family, then hands over the prize purse with a sulky \"…beginner's arm.\"", "ff_hearth", gold(10), flag("snowball_done"), flag("snowball_champ"))
                    .fail("Your snowball goes very high and comes down very precisely on Granny Thorvild. She does not turn around. She simply raises one finger. The crowd falls silent in awe.", "ff_hearth", flag("snowball_done"))
                .choice("Pack the perfect snowball with a whisper of frost").icon("❄").spell("ray_of_frost").check("sleight_of_hand", 11)
                    .pass("A thread of cold from your fingertips turns the snowball into a dense, glittering sphere of ice. It flies true — *BWONNNG!* — and the bell spins a full circle on its hook. Ivar Frostjaw squints at you. \"That was magic.\" \"That was *technique*.\"", "ff_hearth", gold(10), flag("snowball_done"), flag("snowball_champ"))
                    .fail("You overdo it. The snowball becomes a ball of solid ice, and it hits Ulf Frostjaw square in the forehead. He sits down in the snow. Then he stands up. Then he rolls up his sleeves.", "ff_brawl")
                .choice("Tell the Frostjaws their mother throws better").icon("😏")
                    .effect(log("\"Our MOTHER,\" says Ivar, very quietly, \"is the finest thrower in Highmarch.\" \"I know,\" you say. \"I've seen her.\" The brothers look at each other and crack their knuckles in unison."))
                    .go("ff_brawl")
                .choice("Just watch the contest").icon("↩").go("ff_hearth");

        c.scene("ff_brawl").chapter(PRO).title("A Frostjaw Family Matter").backdrop("tavern").time("night").music("brawl")
                .text("The Frostjaw brothers drag the argument inside, as is apparently tradition, and whistle up a few large cousins for balance. Torvald sighs, moves the cider cauldron somewhere safe, and announces \"NO WEAPONS, NO BITING, NO FIRE INDOORS!\" to the room at large. Several people sit down to watch.",
                        "Sten pulls off his mittens, one finger at a time. \"Nothing personal,\" he says. \"It's the festival.\"")
                .choice("Brawl! (fists, mugs and benches — nobody dies)").icon("👊").battle("hearth_brawl")
                    .victory("The last Frostjaw topples into a snowdrift by the door with a contented sigh. Then they're all up, laughing, slapping your back hard enough to dislodge fillings, and Torvald pours everyone a mug of something restorative. \"Anyone who fights like that,\" says Sten, \"is a Frostjaw now. Honorary. Wolves come, we stand with you.\"", "ff_hearth", flag("snowball_done"), flag("frostjaw_friends"), shortRest(), xp(50))
                    .defeat("You wake up on a bench with a cold cloth on your face and a Frostjaw on either side, both looking guilty. \"We went a bit hard,\" Ulf admits. \"It's the festival.\" Torvald charges you for a broken bench, then pours you a restorative mug on the house.", "ff_hearth", flag("snowball_done"), shortRest(), gold(-3));

        // ---- Hot cider
        c.scene("ff_cider").chapter(PRO).title("Granny Thorvild's Challenge").backdrop("tavern").time("night").music("tavern")
                .npc("Granny Thorvild Ninefingers", "Ninety-Four Years Old, Undefeated", "#884ea0", "🍵", "Sit, sweetling. Cider's hot. Hotter than you. First to finish the jug wins. Last one to faint pays.")
                .text("GRANNY THORVILD is very small, very old, wrapped in nine shawls, and has never been beaten at hot cider in seventy years. Two steaming clay jugs sit on the table between you. The cider is spiced with something that makes your eyes water from across the room.",
                        "A crowd gathers. Money changes hands. Most of it is being bet on Granny.")
                .choice("Drink! Drink! Drink!").icon("🍵").check("save:con", 13).chooser()
                    .pass("You drain the jug, slam it down, and breathe out a cloud of cinnamon steam. Granny Thorvild finishes a heartbeat later, stares at you, and then laughs so hard she has to be held up. \"FINALLY,\" she wheezes. \"Seventy years! Somebody give this one my winnings!\" (Heroic Inspiration!)", "ff_hearth", inspiration(), gold(15), flag("cider_done"), flag("cider_champ"))
                    .fail("Halfway down, the room starts gently rotating. You wake some time later wearing one of Granny's shawls and a moustache somebody drew on you with soot. Granny pats your cheek. \"Good try, sweetling.\"", "ff_hearth", damageOne("1d4", "poison"), flag("cider_done"))
                .choice("Watch Granny's technique before you drink").icon("👁").check("insight", 12)
                    .pass("Her mug never empties because she never drinks it — she's tipping it into a hollow walking stick under the table! You raise an eyebrow at her. She raises one back. \"Keep my secret,\" she whispers, \"and take this for your trouble.\" She presses a corked bottle of something red into your hand.", "ff_hearth", item("potion_healing"), attitude("thorvild", 2), flag("cider_done"))
                    .fail("You watch her intently for a full minute. She watches you back, sips, and wins out of pure spite.", "ff_hearth", flag("cider_done"))
                .choice("Heat Granny's jug to scalding with a Fire Bolt").icon("🔥").spell("fire_bolt").check("deception", 12)
                    .pass("A tiny, *tiny* fire bolt flicks under the table. Granny's cider begins to bubble. She takes a sip, blinks, and has to wait for it to cool — and you win by a whole swallow. \"Sweetling,\" she says, \"you're a dreadful cheat. I love you. Never do it again.\"", "ff_hearth", gold(15), flag("cider_done"), flag("cider_champ"))
                    .fail("Granny's cider boils. She drinks it anyway, boiling, without blinking, and wins. The crowd goes very quiet. \"I was a girl,\" says Granny, \"in the Red Summer. Hot doesn't frighten me.\"", "ff_hearth", flag("cider_done"))
                .choice("Politely decline").icon("↩").go("ff_hearth");

        // ---- The sled race
        c.scene("ff_sled").chapter(PRO).title("The Widowmaker Run").backdrop("snow").time("night").music("tension")
                .npc("Sigge the Loud", "Sled-Race Announcer", "#c0392b", "📣", "SIX SLEDS! ONE HILL! A FROZEN POND AT THE BOTTOM! AND NO REFUNDS FOR THE DEAD!")
                .text("The WIDOWMAKER RUN is a quarter-mile slope of packed ice lined with torches, ending in a frozen millpond and a pile of hay that is mostly there for morale. Six sleds wait at the top. One of them has a yak painted on it. One has teeth marks.",
                        "At the finish line stands the race judge: a tall woman in a white fox mask, a bow on her back, who watches the sledders with a stillness that has nothing to do with the cold.")
                .choice("Push off and ride it like you stole it").icon("🛷").check("luck", 11)
                    .pass("Wind roars. Torches blur. You take the last bend on one runner and hit the pond spinning like a top — and come to rest, dizzy and grinning, past the finish line. FIRST! The masked judge tilts her head at you. \"Fearless,\" she says softly. \"Or foolish. Up here, there's no difference.\"", "ff_hearth", gold(15), flag("sled_done"), flag("sled_champion"), flag("saw_kessa"))
                    .fail("Your sled hits a bump, leaves the ground, and lands in the hay pile. Most of you lands in the hay pile. The masked judge helps you out by the collar without a word.", "ff_hearth", damageOne("1d4", "bludgeoning"), flag("sled_done"), flag("saw_kessa"))
                .choice("Steer by muscle and nerve").icon("💪").check("athletics", 13)
                    .pass("You haul the runners through every bend like you're wrestling an ox and roar across the pond in a spray of ice. A win! The masked judge marks your name on her slate — then, oddly, underlines it.", "ff_hearth", gold(15), flag("sled_done"), flag("sled_champion"), flag("saw_kessa"))
                    .fail("You wrestle the sled. The sled wins. You arrive at the bottom separately.", "ff_hearth", damageOne("1d4", "bludgeoning"), flag("sled_done"), flag("saw_kessa"))
                .choice("Draft behind Dagny's yak-sled").icon("🐂").check("animal_handling", 12)
                    .pass("You tuck in behind the herder's yak, Mabel, who runs downhill like a furry avalanche, then whip past her at the bottom. Mabel looks betrayed. Dagny is delighted. You win.", "ff_hearth", gold(15), flag("sled_done"), flag("sled_champion"), flag("saw_kessa"))
                    .fail("Mabel stops dead halfway down to eat a lantern. You do not stop. You meet her very suddenly.", "ff_hearth", damageOne("1d4", "bludgeoning"), flag("sled_done"), flag("saw_kessa"))
                .choice("Maybe next year").icon("↩").go("ff_hearth");

        // ---- The yak herder
        c.scene("ff_yak").chapter(PRO).title("Dagny and Mabel").backdrop("tavern").time("night").music("tavern")
                .npc("Dagny Muckle", "Yak Herder & Collector of Gossip", "#a0522d", "🐂", "Don't mind Mabel. She's friendly. She's eaten three hats tonight but she's friendly.")
                .text("DAGNY MUCKLE has brought her yak indoors. Nobody has told her she can't, and nobody is going to. MABEL the yak is the size of a haywain, smells powerfully of yak, and is calmly eating the festival bunting.",
                        "Dagny herself is a wiry woman with a pipe, a voice like a gate hinge and the single most complete knowledge of other people's business in the northern mountains. She pats the bench beside her. \"Sit. Talk. Tell me something I don't know, and I'll tell you three things you shouldn't.\"")
                .choice("Trade gossip with Dagny").icon("💬").once()
                    .effect(flag("know_isolde"), log("Dagny leans in. \"The witch on the glacier? Everybody calls her the Winter Queen. But I knew her as Isolde Ashgrove — she kept the Great Lantern lit, every festival, since she was twelve. Elder Hrolla's girl.\" She taps her pipe. \"Don't say that name near Hrolla. Nobody does.\""))
                    .go("ff_yak")
                .choice("Ask what she isn't saying").icon("👁").once().check("insight", 11)
                    .pass("Under the jokes, Dagny's hands are shaking. \"I had fifty yaks, three springs ago. I've thirty-one. There's no grass under the snow. Another winter and Highmarch eats its herds, and then it eats its seed corn, and then…\" She stops. \"Bring the spring back. Please.\"", "ff_yak", flag("know_starving"), xp(25))
                    .fail("\"Me? Nothing! I'm an open book!\" She is a locked book in a chained chest at the bottom of a lake.", "ff_yak")
                .choice("Make friends with Mabel").icon("🐂").once().check("animal_handling", 12)
                    .pass("You scratch the spot behind Mabel's horns that every yak secretly wants scratched. She groans like a collapsing barn and leans her entire weight on you. Dagny gasps. \"She *likes* you. She doesn't like *me*.\"", "ff_yak", flag("mabel_friend"))
                    .fail("Mabel considers your outstretched hand, then eats your hat. It was a nice hat.", "ff_yak")
                .choice("Buy Mabel a honey-cake (2 gp)").icon("🍯").once().ifGold(2)
                    .effect(gold(-2), flag("mabel_friend"), log("Mabel eats the honey-cake, the plate, and a little of your sleeve, and then follows you around the lodge with enormous, devoted eyes. You have a yak now. Possibly forever."))
                    .go("ff_yak")
                .choice("Leave Dagny to her pipe").icon("↩").effect(flag("yak_done")).go("ff_hearth");

        // ---- The bard
        c.scene("ff_bard").chapter(PRO).title("The Lantern-Keeper's Bargain").backdrop("tavern").time("night").music("ballad")
                .npc("Pell Winterwren", "Festival Bard", "#2874a6", "🎻", "This one's new. Well — three years new. Sit close. It's a quiet one.")
                .text("The bard PELL WINTERWREN plays a fiddle with frost on its strings. The room hushes as the song begins, and Torvald starts crying immediately:",
                        "*\"When the Red Summer came with its tongues of flame, / And the forest ran burning, and nobody came, / The Lantern-Keeper climbed where the old cold sleeps, / And she bargained her heart to the Frostfell deeps…*",
                        "*…'Let no fire ever harm them,' she cried to the ice, / And the Frostfell said 'Granted' — but named her a price. / Now the snow never leaves and the lanterns go dark, / And a queen on a throne keeps a cold, cold heart.\"*",
                        "Pell lets the last note hang in the air, and nobody claps for a long moment.")
                .choice("Ask about the verse she didn't sing").icon("📖").check("history", 13)
                    .pass("Pell's eyebrows rise. \"You know there's another? My gran sang it.\" She hums it low: *'What's given in fear can be taken in love; / Bring spring to the Keeper, and the crown will come off.'* \"Old songs are mostly lies. But the good lies are true in the middle.\"", "ff_hearth", flag("know_pact"), flag("bard_done"), xp(25))
                    .fail("\"Oh, that's all of it,\" Pell says, a little too fast, and launches into something about a goat who became mayor.", "ff_hearth", flag("bard_done"))
                .choice("Join in on the next song").icon("🎵").check("performance", 12)
                    .pass("Pell swings into a stomping reel and you match her note for note. By the second chorus the whole lodge is on its feet, and Torvald has lifted a table over his head for reasons nobody understands. (Heroic Inspiration!)", "ff_hearth", inspiration(), gold(5), flag("bard_done"))
                    .fail("You start the chorus in a completely different key. Pell, a professional, follows you into it. It's somehow worse, and somehow wonderful.", "ff_hearth", flag("bard_done"))
                .choice("Applaud and move on").icon("↩").effect(flag("bard_done")).go("ff_hearth");

        // ---- The quest giver
        c.scene("ff_hrolla").chapter(PRO).title("Elder Hrolla Ashgrove").backdrop("tavern").time("night").music("tension")
                .npc("Elder Hrolla Ashgrove", "Elder of Highmarch", "#6e2c00", "🧓", "You've the look of folk who climb things. Sit down. I've a mountain I need climbed.")
                .text("ELDER HROLLA ASHGROVE is older than the lodge and twice as weathered, a woman built from pine-root and stubbornness, with a clay pipe clamped in her teeth and a small tin lantern on the table before her. She lights the pipe from it without looking.",
                        "\"Three years ago, the Red Summer near burned this valley to the rock. Then the winter came — and it stayed. Up on the Glacier Throne sits a witch who calls herself Queen, and her wolves come down every Midwinter to put out our Great Lantern. Every year they take a little more: a herd, a barn, a child's fingers to the frost.\"",
                        "\"I can't pay you much. Fifty gold each, and Torvald's cider for life. Climb to the Throne. End the winter. And if the Queen will hear reason…\" Her jaw works on the pipe. \"…tell her that's enough now.\"")
                .choice("\"We'll bring back the spring.\" Accept").icon("🌱")
                    .effect(flag("quest_accepted"), item("ff_ashgrove_lantern"), xp(50), log("Hrolla pushes the little tin lantern across the table. \"Take this. Light it at the top. It's… an old festival custom.\" She doesn't meet your eyes."))
                    .go("ff_hearth")
                .choice("\"Who is she, Elder? Really?\"").icon("👁").check("insight", 13).advantageIf("know_isolde")
                    .pass("The pipe trembles. For a long moment Hrolla is just an old woman. \"Isolde. My daughter. She kept the Great Lantern. When the fire came she climbed up there to save us, and she *did*, damn her, and she never came down.\" She pushes the little lantern into your hands. \"This was hers. If there's anything left of my girl up there, show her this.\"", "ff_hearth", flag("quest_accepted"), flag("hrolla_truth"), item("ff_ashgrove_lantern"), xp(75))
                    .fail("\"A witch,\" says Hrolla flatly, \"and a queen, and none of your business.\" But she gives you the little tin lantern anyway. \"Light it at the top. Festival custom.\"", "ff_hearth", flag("quest_accepted"), item("ff_ashgrove_lantern"), xp(50))
                .choice("Ask for better pay — you'll be climbing a glacier").icon("💰").check("persuasion", 14)
                    .pass("Hrolla barks a laugh. \"Cheeky. Good. You'll need cheek up there.\" She adds a pouch of old silver and two potions from the Hearth-Mother's shrine. \"Fifty more each when you're back. *When*, mind.\"", "ff_hearth", flag("quest_accepted"), flag("reward_up"), item("ff_ashgrove_lantern"), item("potion_healing"), item("potion_healing"), xp(50))
                    .fail("\"Fifty is all Highmarch has,\" says Hrolla, without heat. \"And the cider. The cider's worth more than gold.\" She pushes a little tin lantern at you. \"Light that at the top.\"", "ff_hearth", flag("quest_accepted"), item("ff_ashgrove_lantern"), xp(50));

        // ---- The lantern lighting & the wolves
        c.scene("ff_lantern").chapter(PRO).title("The Lighting of the Great Lantern").backdrop("town").time("night").music("tension")
                .npc("Elder Hrolla Ashgrove", "Elder of Highmarch", "#6e2c00", "🧓", "Every year they come. Every year we light it anyway. That's what Highmarch is.")
                .text("At midnight the whole village packs into the square, breath steaming, mittened hands clutching candles. Hrolla raises a torch to the GREAT LANTERN — a cage of iron and coloured glass the size of a cottage — and for one golden heartbeat the square is warm.",
                        "Then a howl rolls down from the Glacier Throne, and every candle in Highmarch goes out at once.",
                        "Shapes pour over the rooftops: white wolves with frost on their muzzles, and among them a larger beast with a collar of living ice, its breath crackling in the air. The Great Lantern gutters. Children scream. The wolves aren't here for meat. They're here for the *light*.")
                .choice("Defend the festival!").icon("⚔").battle("festival_wolves")
                    .victory("The last wolf flees yelping over the rooftops, and Hrolla — still holding her torch — relights the Great Lantern with a defiant roar. The square explodes in cheers. For the first time in three Midwinters, the light stays lit.", "ff_after_wolves", flag("lantern_saved"), xp(50))
                    .defeat("The wolves knock you sprawling and snuff the Great Lantern before melting back into the dark. When you come round, the villagers are relighting candles one by one. \"Every year,\" Hrolla says, kneeling beside you. \"But every year we light it anyway.\"", "ff_after_wolves")
                .choice("Relight the Great Lantern with a Fire Bolt!").icon("🔥").spell("fire_bolt")
                    .pass("Your fire bolt streaks across the square and punches into the Great Lantern's heart. It ROARS back to life, coloured light blazing across the snow — and the wolves recoil, blinking and snarling, dazzled by the glare. Now!", "ff_wolves_fight", flag("wolves_dazzled"))
                .choice("Rally the villagers into a wall of torches").icon("📣").check("persuasion", 12)
                    .pass("\"TORCHES! TOGETHER! NOW!\" Something in your voice cuts through the panic. Villagers relight their brands from Hrolla's torch and lock shoulders in a blazing ring around the children. The wolves falter, caught between the fire and you.", "ff_wolves_fight", flag("wolves_dazzled"))
                    .fail("Nobody can hear you over the screaming. The wolves are already among the stalls.", "ff_wolves_fight");

        c.scene("ff_wolves_fight").chapter(PRO).title("Wolves in the Square").backdrop("town").time("night").music("battle")
                .text("Hoarfrost spreads across the cobbles where the Queen's Hound walks. The pack fans out to surround you, low and silent, and the Hound fixes you with eyes like chips of glacier.")
                .choice("Fight!").icon("⚔").battle("festival_wolves")
                    .victory("The Hound falls, and the frost on the cobbles melts into ordinary puddles. The pack scatters. Hrolla relights the Great Lantern with a defiant roar, and the square erupts in cheers.", "ff_after_wolves", flag("lantern_saved"), xp(50))
                    .defeat("You're bowled over and the wolves vanish into the dark, but the villagers drag you to shelter. \"Every year,\" Hrolla says. \"And every year we light it anyway.\"", "ff_after_wolves");

        c.scene("ff_after_wolves").chapter(PRO).title("Hrolla's Longhouse").backdrop("tavern").time("night").weather("none").music("camp")
                .onEnter(longRest())
                .text("Hrolla takes you home to her longhouse, wraps you in furs, and feeds you a stew so thick a spoon stands up in it. The Frostjaws post themselves at her door with clubs. Torvald brings cider. Mabel, somehow, is in the barn.",
                        "On a shelf above the hearth sits a painting of a laughing young woman holding a lantern. Hrolla sees you looking and turns it to face the wall.",
                        "You sleep long and deep, and when you wake the snow outside is glowing gold with dawn.")
                .choice("Examine the Queen's Hound's ice collar").icon("🔍").once().check("investigation", 12)
                    .pass("The collar is carved, not frozen: a single ring of ice that never melts, engraved with a snowflake inside a lantern. The lantern-keeper's mark, frozen over. Whoever sent the wolves once *lit* the Great Lantern.", "ff_after_wolves", flag("hound_collar"), xp(25))
                    .fail("It's a collar. Made of ice. It is very cold and you have now lost the feeling in two fingers.", "ff_after_wolves")
                .choice("Head into the village for supplies").icon("🛒").go("ff_outfitter");

        c.scene("ff_outfitter").chapter(PRO).title("Snowplume Outfitters").backdrop("town").time("day").music("explore").shop().anchor("town").checkpoint()
                .npc("Bergit Snowplume", "Outfitter", "#117a65", "🧣", "Going up the Throne? Then you'll want warm socks. And potions. And a will. I sell all three!")
                .text("BERGIT SNOWPLUME's shop is a cosy cave of furs, ropes, snowshoes and crampons, with a stove in the middle that she refuses to let anyone stand too close to. Shelves of bottles glimmer: healing potions, flasks of alchemist's fire, and her pride — *Frostbloom Flasks*, which she admits are \"not really the thing up there, dear, but the yetis hate them\".",
                        "\"Alchemist's fire,\" she says, tapping a crate. \"That's what you want. Everything on that mountain is made of cold, and cold hates fire.\"",
                        "Spend wisely. The Glacier Throne doesn't take coin.")
                .choice("Ask Hearth-Mother's shrine for a blessing").icon("🕯").once()
                    .effect(inspiration(), heal("2d8"), log("Brother Fennick's fellow priests daub a smear of warm ash on your brow. \"May your hearth stay lit.\" (Heroic Inspiration)"))
                    .go("ff_outfitter")
                .choice("Ask Bergit about the High Pass").icon("🗺").once()
                    .effect(flag("pass_advice"), log("\"Two ways up. The High Road crosses the Skaldvik Ice Bridge — fast, but there's a winter wolf who's taken to sitting on it like a tollkeeper. Or the old Goat Trail round the shoulder — long, and avalanche country, and there's a yeti in the caves who's very hungry and very grumpy. Pick your poison, dear. I'd pick the one with fewer teeth.\""))
                    .go("ff_outfitter")
                .choice("Set out for the High Pass").icon("🥾").go("ff_pass");
    }

    // =========================================================================================== chapter 1
    static void chapterOne(Campaign c) {
        c.scene("ff_pass").chapter(CH1).title("The Frostfell Road").backdrop("snow").time("day").music("snow").anchor("road")
                .text("Highmarch shrinks to a smudge of lantern-smoke below you. The road climbs through drifts of that perfect, identical snow, between pines so laden they look like sugared ghosts. Ragna walks ahead, testing every step with a pole. Fennick walks behind, apologising to the snow.",
                        "At a cairn of stacked stones hung with frozen prayer-ribbons, the road splits. Up and left, the HIGH ROAD climbs toward the SKALDVIK ICE BRIDGE, a blue arch across a chasm. Right, the old GOAT TRAIL winds around the mountain's shoulder beneath overhanging cornices of snow.")
                .choice("Take the High Road to the ice bridge").icon("🌉").go("ff_bridge")
                .choice("Take the old Goat Trail").icon("🐐").go("ff_goat_trail")
                .choice("Read the weather before you choose").icon("🌬").once().check("survival", 12)
                    .pass("Wind from the north, cornices loaded and groaning: the Goat Trail is a slide waiting to happen. The High Road is clearer — but there's a smear of fresh blood on the snow up that way, and big paw prints. Neither is safe. At least now you know *how* each is unsafe.", "ff_pass", xp(25))
                    .fail("The sky is white. The ground is white. The air is white. You report this to the party. Ragna pats your shoulder.", "ff_pass");

        // ---- Route A: the ice bridge
        c.scene("ff_bridge").chapter(CH1).title("The Skaldvik Ice Bridge").backdrop("bridge").time("day").music("tension")
                .text("The SKALDVIK ICE BRIDGE is beautiful the way a sword is beautiful: a natural arch of blue ice no wider than a cart, spanning a chasm so deep the bottom is just a rumour. Wind sings across it in a thin, high note. Old rope rails, frozen stiff, hang from posts at either end.",
                        "Paw prints the size of dinner plates lead onto the bridge. None lead off it.")
                .choice("Test the ice before you trust it").icon("🔍").check("investigation", 12)
                    .pass("You tap your way out with a pole and find the bridge's secret: a hairline crack running down its middle, and a safe line along the left edge. Halfway across you also spot, half-buried in the snow on the far side, three white wolves lying perfectly still. Waiting.", "ff_bridge_fight", flag("bridge_spotted"), xp(25))
                    .fail("It looks solid. It sounds solid. Two steps out, it stops being solid.", "ff_bridge_crack")
                .choice("Scan the far side for trouble").icon("👁").check("perception", 13)
                    .pass("What you took for snowdrifts are breathing. Wolves — three of them, and a huge one curled behind a boulder, its fur glittering with rime. They think they're hidden. They are wrong.", "ff_bridge_fight", flag("bridge_spotted"))
                    .fail("Snow, snow, rock, snow. You start across.", "ff_bridge_crack")
                .choice("Cross quickly, before you think about it").icon("🏃").go("ff_bridge_crack");

        c.scene("ff_bridge_crack").chapter(CH1).title("The Ice Gives Way").backdrop("bridge").time("day").music("tension")
                .text("*CRACK.* A seam opens across the bridge like a grin. Chunks of blue ice drop away into the chasm and do not make a sound when they land, because they haven't landed yet.")
                .choice("Jump for the solid ice!").icon("❄").check("save:dex", 12).each()
                    .pass("You leap the widening crack and skid to a stop on the far side, heart hammering.", "ff_bridge_fight")
                    .fail("Your boot plunges through into the crevasse; you slam against the ice before Ragna's rope hauls you out, battered and frozen to the bone.", "ff_bridge_fight", damageOne("1d8", "cold"));

        c.scene("ff_bridge_fight").chapter(CH1).title("Rimefang").backdrop("bridge").time("day").music("battle")
                .npc("Rimefang", "Winter Wolf, Self-Appointed Bridge-Warden", "#d0ece7", "🐺", "Travellers. How lovely. The Queen pays me in silence and you will pay me in meat.")
                .text("A wolf the size of a pony rises from behind the boulder, and frost blooms on the stones where it steps. When it speaks, its voice is soft and well-bred, like a duchess at a funeral. \"I am RIMEFANG. This bridge belongs to the Winter Queen, and the Winter Queen has lent it to me.\"",
                        "The pack slinks out around it, low and white. \"Turn back,\" Rimefang suggests, \"or go down. The chasm is very patient.\"",
                        "(The bridge is narrow and the chasm is deep. Shove a wolf off the edge and it's gone for good.)")
                .choice("\"We'll take the bridge.\" Fight!").icon("⚔").battle("ice_bridge")
                    .victory("Rimefang's last howl echoes down into the chasm and fades. The pack bolts into the pines. On the far side of the bridge the High Road climbs on toward a thread of chimney smoke, high on the mountain.", "ff_hermit", xp(25))
                    .defeat("You're driven back off the bridge, bloodied, and the wolves lose interest in you as the light fails. At dusk you creep across, one by one, and follow a thread of chimney smoke higher up the mountain.", "ff_hermit")
                .choice("Flash fire at the wolf and make it think twice").icon("🔥").once().spell("fire_bolt").check("intimidation", 13)
                    .pass("Your fire bolt scorches the snow at Rimefang's paws in a hiss of steam. The great wolf flinches, ears flat — and suddenly remembers urgent business elsewhere. \"…Another time,\" it murmurs, and the pack melts into the pines. You cross unchallenged.", "ff_hermit", xp(100))
                    .fail("The fire bolt hisses into the snow. Rimefang watches the steam rise and laughs, a dreadful dry little sound. \"Fire, on my bridge? How very rude.\"", "ff_bridge_fight");

        // ---- Route B: the goat trail
        c.scene("ff_goat_trail").chapter(CH1).title("The Goat Trail").backdrop("snow").time("day").music("snow")
                .text("The Goat Trail is a shelf of rock barely wide enough for one person, with a cliff on the right and a long white nothing on the left. Overhead, huge lips of snow overhang the path like frozen waves about to break. They creak in the wind.",
                        "Ragna puts a finger to her lips. \"Quiet now,\" she breathes. \"The mountain's listening.\"")
                .choice("Move silently beneath the cornices").icon("🤫").check("stealth", 12).group()
                    .pass("Step by soft step, breath held, you slip beneath the hanging snow. Somewhere above, it creaks, and settles, and holds. You reach the far end and let out a breath you've been holding for a quarter-mile.", "ff_yeti_cave", xp(50))
                    .fail("Somebody sneezes. Somebody always sneezes. Above you, the mountain answers.", "ff_avalanche")
                .choice("Bring the snow down early with a Fire Bolt").icon("🔥").spell("fire_bolt")
                    .pass("You loft a fire bolt into the biggest cornice ahead of you. It slumps, groans — and a roaring river of snow pours across the trail *in front* of you and over the edge, harmlessly, into the void. When the white dust settles, the way is clear. Ragna looks at you with new respect. \"…Do that again sometime.\"", "ff_yeti_cave", xp(50))
                .choice("Rope up and hug the cliff").icon("🪢").ifItem("rope")
                    .effect(log("Ragna ties you all together with a knot she claims is called 'the Grandmother's Embrace'. You inch along with your backs to the cliff. Slow, cold — and safe."))
                    .go("ff_yeti_cave");

        c.scene("ff_avalanche").chapter(CH1).title("AVALANCHE!").backdrop("snow").time("day").music("tension")
                .text("With a sound like the sky tearing, the cornice above you lets go. A white wall comes thundering down the slope, swallowing pines whole.")
                .choice("Dive for the overhang!").icon("💨").check("save:dex", 13).each()
                    .pass("You fling yourself under a lip of rock as the avalanche roars over your head and away down the mountain.", "ff_yeti_cave")
                    .fail("The snow hits you like a falling house and drags you twenty feet before you claw your way out, gasping.", "ff_yeti_cave", damageOne("2d6", "bludgeoning"));

        c.scene("ff_yeti_cave").chapter(CH1).title("Grumblegut's Larder").backdrop("cave").time("dusk").music("eerie")
                .text("By dusk you find shelter: a cave mouth hung with icicles, out of the wind. Inside, it is almost warm. It also smells strongly of wet dog, and there are bones stacked very neatly along one wall. Goat bones. Mostly.",
                        "Then the light from the cave mouth is blotted out. Eight feet of matted white fur stands there, holding a dead goat in one hand and an expression of profound, personal betrayal on its face. This is GRUMBLEGUT'S house. And you are in it. Behind its legs, a smaller, fluffier yeti peeks out and growls.")
                .choice("Stand and fight!").icon("⚔").battle("yeti_den")
                    .victory("Grumblegut collapses with a groan like a felled tree, and Junior scampers out into the snow, wailing. You feel slightly awful. You also have a warm cave. At dawn, you spot a curl of chimney smoke higher up the mountain.", "ff_hermit", xp(25))
                    .defeat("The yeti flings you out into the snow one by one, then sits in the cave mouth and eats its goat at you, pointedly. You shelter in a snowbank until dawn, then follow a curl of chimney smoke up the mountain.", "ff_hermit")
                .choice("Offer it your rations — slowly").icon("🍖").check("animal_handling", 14)
                    .pass("You set down a packet of dried meat, a wedge of cheese and Torvald's emergency honey-cake, and back away. Grumblegut sniffs. Eats. Burps a burp that rattles the icicles. Then it lies down across the cave mouth like a furry wall — and lets you sleep behind it, sheltered from the wind. In the morning, it points one huge finger up the mountain, toward a curl of chimney smoke.", "ff_hermit", flag("grumblegut_fed"), xp(100))
                    .fail("Grumblegut eats the rations, then looks at you with the expression of someone who has been served the starter. You are the main course.", "ff_yeti_fight")
                .choice("Scare it off with Burning Hands").icon("🔥").spell("burning_hands")
                    .pass("A fan of flame bursts from your fingers and fills the cave mouth. Grumblegut shrieks like a kettle, grabs Junior, and flees into the night with its fur smoking. You feel a bit bad about it. You also have a warm cave. At dawn, you spot chimney smoke higher up.", "ff_hermit", xp(50));

        c.scene("ff_yeti_fight").chapter(CH1).title("The Main Course").backdrop("cave").time("dusk").music("battle")
                .text("Grumblegut drops the goat, beats its chest with both fists and ROARS. Frost explodes across the cave walls. Junior roars too. It's more of a squeak.")
                .choice("Fight!").icon("⚔").battle("yeti_den")
                    .victory("The yeti falls with a groan like a felled tree, and Junior scampers off into the snow. At dawn you spot a curl of chimney smoke higher up the mountain.", "ff_hermit", xp(25))
                    .defeat("You're flung out into the snow one by one. You survive the night in a drift and stagger on at dawn toward a curl of chimney smoke.", "ff_hermit");

        // ---- The hermit (both routes rejoin here)
        c.scene("ff_hermit").chapter(CH1).title("The Hermit in the Ice").backdrop("snow").time("day").music("mystery")
                .npc("Father Osk", "Last Monk of the Frostfell Monastery", "#5d6d7e", "🍵", "Ah! Visitors! Forgive me for not getting up. I have been sitting down for three years.")
                .text("The smoke comes from a stone hut perched on a ledge, its door hanging open. Inside, a fire burns in a little iron stove — and beside it, frozen from the chest down into a solid block of blue ice, sits an old man in monk's robes, still holding a cup of tea.",
                        "He blinks at you. His eyebrows are magnificent and entirely frozen. \"FATHER OSK,\" he says. \"I would offer you tea, but as you can see, mine has been in my hand since the winter began. I asked the Queen to reconsider. She asked the ice to reconsider me.\"",
                        "The stove, he explains, is the only reason he's alive. He keeps it lit with his feet. Don't ask how.")
                .choice("Melt the ice with a careful Fire Bolt").icon("🔥").spell("fire_bolt").check("arcana", 11)
                    .pass("You trace a slow, controlled line of fire around the block. The ice groans, sweats and splits — and Father Osk tips forward out of it, tea and all, into your arms. \"Ohhh,\" he sighs, \"*legs*. I'd forgotten about legs.\"", "ff_hermit_hut", flag("osk_freed"), xp(50))
                    .fail("The ice melts! So do Father Osk's eyebrows. He sits in a puddle, smoking faintly, and says with great dignity, \"Thank you. I never liked them.\"", "ff_hermit_hut", flag("osk_freed"), xp(25))
                .choice("Blast it open with Burning Hands").icon("🔥").spell("burning_hands")
                    .pass("A fan of flame washes over the block and it bursts into slush and steam. Osk emerges pink, soaked and laughing. \"Subtle! I like subtle!\" He drinks his tea. It's still cold. He drinks it anyway.", "ff_hermit_hut", flag("osk_freed"), xp(50))
                .choice("Chip him free with picks and knives").icon("⛏").check("athletics", 12).chooser()
                    .pass("Chip, chip, crack. An hour later, sweating in the cold, you lever away the last chunk and Osk stands up for the first time in three years, wobbling like a newborn goat.", "ff_hermit_hut", flag("osk_freed"), xp(50))
                    .fail("The ice is old and harder than iron. Your blade skids, and your knuckles meet the block. Your hand goes numb right up to the elbow.", "ff_hermit", damageOne("1d4", "cold"))
                .choice("Stoke the stove and wait it out").icon("🪵").check("survival", 11)
                    .pass("You pack the stove with pine, build a reflecting wall of stones around Osk and keep the fire roaring through the night. By morning he's sitting in a warm puddle, toes wriggling.", "ff_hermit_hut", flag("osk_freed"), xp(25))
                    .fail("You feed the fire all night, badly. By dawn the ice has *mostly* melted, and Ragna kicks the rest loose with a grunt. \"Should've done that first,\" she says.", "ff_hermit_hut", flag("osk_freed"));

        c.scene("ff_hermit_hut").chapter(CH1).title("Father Osk's Kettle").backdrop("camp").time("night").music("camp")
                .onEnter(longRest())
                .npc("Father Osk", "Last Monk of the Frostfell Monastery (Thawed)", "#5d6d7e", "🍵", "Tea first. Then the terrible truth. Then more tea. That's the proper order.")
                .text("Osk insists on making you tea and dinner, and you rest in his hut through the long night while the wind screams outside. Over the third pot, he tells you what he knows.",
                        "\"Isolde came up the mountain in the Red Summer, with the forest burning behind her. She climbed past my monastery to the Throne, where the FROSTFELL sleeps — the oldest cold in the world, older than mountains. And she asked it: *let no fire ever harm Highmarch.* And the Frostfell said yes.\"",
                        "\"The trouble with asking a winter to protect you from fire is that it only knows one way to do it.\" He stirs his tea. \"It made her its Queen, and it never let the spring come back. My brothers fled. I stayed to argue. You've seen how that went.\"")
                .choice("Ask how the pact can be broken").icon("📜").once()
                    .effect(flag("know_pact"), log("\"The Frostfell keeps the winter. Isolde keeps the Frostfell. And Spring — little Lissel, the spring-child who came every year to end the snows — the Queen's sentinels caught her and froze her inside my monastery. Free Lissel, and the winter loses its grip. Reach Isolde's heart, and it loses its *queen*.\""))
                    .go("ff_hermit_hut")
                .choice("Ask about the monastery door").icon("🚪").once()
                    .effect(flag("osk_hint"), log("\"The inner sanctum has a riddle-door. Four seasons, one question: *which of us must come next?* The answer has been the same for a thousand years, and it's been missing for three.\" He taps his nose. \"Spring. The answer is always spring.\""))
                    .go("ff_hermit_hut")
                .choice("Accept a parting gift").icon("🧤").once()
                    .effect(item("ff_everwarm_mittens"), log("Osk presses a pair of lumpy hand-knitted mittens on you. \"I knitted them with my toes while frozen. They have never once been cold. Attune to them from your character sheet: +1 initiative, +1 Constitution saves.\""))
                    .go("ff_hermit_hut")
                .choice("Sleep, and climb to the monastery at dawn").icon("🌄").go("ff_monastery_gate");
    }

    // =========================================================================================== chapter 2
    static void chapterTwo(Campaign c) {
        c.scene("ff_monastery_gate").chapter(CH2).title("The Frostfell Monastery").backdrop("ruins").time("dawn").music("eerie").checkpoint()
                .onEnter(level(3))
                .text("You wake in Osk's hut feeling sharper, stronger, stranger — as if the mountain air has burned something useless out of you. **(Every hero reaches level 3! Wizards learn Scorching Ray and Misty Step.)**",
                        "An hour's climb brings you to the FROSTFELL MONASTERY: a cluster of domed halls and bell towers half-swallowed by a glacier that has flowed right over its walls. Ice fills the windows like stained glass. Somewhere inside, something is giggling.",
                        "Two ways in: the main CLOISTER, where the giggling is coming from, or a crack in the glacier that leads down into the ICE CAVES beneath the halls.")
                .choice("Study the frescoes above the gate").icon("🖼").once().check("religion", 12)
                    .pass("The frescoes show the wheel of the year: a sleeping old man for Winter, a laughing child crowned with buds for Spring, a sun-king for Summer, a lady of falling leaves for Autumn. Someone has scratched the child's face out with a knife. Recently.", "ff_monastery_gate", flag("season_lore"), xp(25))
                    .fail("Monks. Snow. More monks. One of them seems to be riding a goat. You lose interest.", "ff_monastery_gate")
                .choice("Enter through the cloister").icon("🏛").go("ff_cloister")
                .choice("Squeeze down into the ice caves").icon("🧊").go("ff_ice_caves");

        // ---- The cloister route
        c.scene("ff_cloister").chapter(CH2).title("The Giggling Cloister").backdrop("ruins").time("day").music("goblin")
                .text("In the cloister's frozen courtyard, four ICE MEPHITS — little winged imps of ice and spite — are playing a game that seems to involve kicking a monk's skull through the arches and shrieking. They have built a snowman of the abbot. It is anatomically insulting.",
                        "The biggest mephit spots you, freezes mid-kick, and grins with a mouth full of icicles. \"OOOH,\" it whispers. \"*Warm ones.*\"")
                .choice("Attack before they swarm you!").icon("⚔").battle("mephit_cloister")
                    .victory("The last mephit bursts into a puff of glittering slush. Beyond the courtyard, a passage leads to a great round door carved with four faces.", "ff_seasons_door", xp(25))
                    .defeat("The mephits pelt you out of the cloister with ice and abuse, then get bored and fly off. You slip back in later and find the passage to a great round door carved with four faces.", "ff_seasons_door")
                .choice("Challenge them to a better game").icon("🎲").check("performance", 13)
                    .pass("\"Skull-kicking? How *provincial*.\" You teach them Hot Potato with a lump of coal. They are entranced. They are terrible at it. They love you. When you leave, they wave you off toward the inner sanctum, squealing \"COME BACK! COME BACK AND BE HOT AT US!\"", "ff_seasons_door", flag("mephits_charmed"), xp(75))
                    .fail("Your game has rules. Mephits hate rules. They scream \"BORING!\" in unison and attack.", "ff_cloister_fight")
                .choice("Bluff: you're the Queen's inspectors").icon("🎭").check("deception", 13)
                    .pass("\"Royal inspection!\" you bark. \"Who built that snowman? Disgraceful. The Queen will hear of it.\" The mephits panic, scatter, and frantically start rebuilding the snow-abbot more respectfully. Nobody stops you walking right through.", "ff_seasons_door", flag("mephits_charmed"), xp(75))
                    .fail("\"The Queen,\" says the biggest mephit, \"doesn't HAVE inspectors.\" It smiles. \"She has US.\"", "ff_cloister_fight");

        c.scene("ff_cloister_fight").chapter(CH2).title("Mephit Mayhem").backdrop("ruins").time("day").music("battle")
                .text("The mephits explode into the air like a flock of furious snowflakes, cackling and spitting frost.")
                .choice("Swat them out of the sky!").icon("⚔").battle("mephit_cloister")
                    .victory("The last mephit bursts into slush. Beyond the courtyard, a passage leads to a great round door carved with four faces.", "ff_seasons_door", xp(25))
                    .defeat("The mephits drive you off and fly away bored. Later you sneak through to a great round door carved with four faces.", "ff_seasons_door");

        // ---- The ice-cave route
        c.scene("ff_ice_caves").chapter(CH2).title("The Hall of Hanging Knives").backdrop("cave").time("day").music("tension")
                .text("Beneath the monastery, the glacier has hollowed out a cathedral of blue ice. The ceiling is a forest of icicles — thousands of them, some longer than spears, pointing straight down like a held breath.",
                        "The floor is scattered with shattered ones. And with a few unlucky goats. Something brought those icicles down. Something can bring them down again.")
                .choice("Look for what triggers them").icon("👁").check("perception", 13)
                    .pass("There — threads of frost strung ankle-high across the floor, nearly invisible, each running up the wall to the icicles above. A tripwire made of winter. You step over each one like a dancer.", "ff_seasons_door", xp(50))
                    .fail("Looks clear. You step forward and feel something brittle snap against your shin—", "ff_icicle_trap")
                .choice("Bring them down from a safe distance (Fire Bolt)").icon("🔥").spell("fire_bolt")
                    .pass("Your fire bolt strikes the ceiling's thickest cluster. With a sound like a thousand glasses breaking, the icicles rain down across the cavern — *ahead* of you — and shatter into glittering rubble. Ragna applauds. Fennick apologises to the icicles.", "ff_seasons_door", xp(50))
                .choice("Run for it").icon("🏃").go("ff_icicle_trap");

        c.scene("ff_icicle_trap").chapter(CH2).title("The Knives Fall").backdrop("cave").time("day").music("tension")
                .text("*Tink.* Then *TINK-TINK-TINK-TINK* — and the ceiling comes down in a glittering storm of ice spears!")
                .choice("Dodge!").icon("❄").check("save:dex", 13).each()
                    .pass("You weave through the falling spears and dive clear as they shatter all around you.", "ff_seasons_door")
                    .fail("An icicle the length of your arm slams down and drives into you before shattering.", "ff_seasons_door", damageOne("2d6", "piercing"));

        // ---- The riddle of the seasons
        c.scene("ff_seasons_door").chapter(CH2).title("The Door of Four Seasons").backdrop("hall").time("day").music("mystery")
                .onEnter(shortRest())
                .text("Both ways through the monastery end in the same quiet antechamber, where a monk's brazier still holds a few warm coals. You huddle round it and catch your breath (short rest).",
                        "The inner sanctum is sealed by a great round door of white stone carved with four faces: a sleeping old man, a laughing child, a crowned sun-king and a lady weeping leaves. The old man's face is bright and polished. The child's is furred with frost.",
                        "Words circle the rim: *\"I sleep, and then I wake. I blaze, and then I fall. One of us has stayed too long. Name who must come next, and the door will open for them.\"*")
                .choice("Speak Father Osk's answer: \"Spring.\"").icon("🌱").ifFlag("osk_hint").go("ff_spirit")
                .choice("Read the faces as an old temple riddle").icon("🙏").check("religion", 13).advantageIf("season_lore")
                    .pass("The wheel of the year: the old man is Winter, and Winter has stayed too long. Who comes after Winter? The laughing child. *Spring.*", "ff_spirit", xp(25))
                    .fail("You stare at the faces until they start staring back. You'll have to guess.", "ff_seasons_guess")
                .choice("Work it out from the seasons themselves").icon("🌿").check("nature", 12)
                    .pass("Sleep, wake, blaze, fall: winter, spring, summer, autumn. The one who has stayed too long is Winter — so the one who must come next is *Spring*.", "ff_spirit", xp(25))
                    .fail("Sleep… wake… blaze… fall… breakfast? You'll have to guess.", "ff_seasons_guess");

        c.scene("ff_seasons_guess").chapter(CH2).title("Who Comes Next?").backdrop("hall").time("day").music("mystery")
                .text("The four faces wait in silence. Frost creeps a little further over the child's smile.")
                .choice("\"Winter.\"").icon("❄").go("ff_seasons_wrong")
                .choice("\"Summer.\"").icon("☀").go("ff_seasons_wrong")
                .choice("\"Autumn.\"").icon("🍂").go("ff_seasons_wrong")
                .choice("\"Spring.\"").icon("🌱").go("ff_spirit");

        c.scene("ff_seasons_wrong").chapter(CH2).title("The Breath of Winter").backdrop("hall").time("day").music("tension")
                .text("The old man's stone eyes snap open, and a blast of killing cold roars out of his mouth!")
                .choice("Brace yourselves!").icon("🥶").check("save:con", 13).each()
                    .pass("You hunch against the blast, teeth chattering, and ride it out. The old man's eyes close again. The riddle waits.", "ff_seasons_guess")
                    .fail("The cold bites through furs, skin and bone. Frost blooms in your hair.", "ff_seasons_guess", damageOne("2d6", "cold"));

        // ---- The spring-child
        c.scene("ff_spirit").chapter(CH2).title("The Spirit in the Ice").backdrop("hall").time("day").music("sorrow")
                .npc("Lissel", "The Spring-Child (Frozen)", "#58d68d", "🌱", "…warm… is someone… warm…?")
                .text("The door grinds open on the monks' inner sanctum: a round hall where a pillar of ice rises from floor to ceiling. Frozen inside it, curled up like a sleeping cat, is a child made of green light — hair of new leaves, cheeks of apple blossom, a crown of buds that have not opened in three years.",
                        "This is LISSEL, the spring-child, who came every year to end the winter. Her eyes flicker open behind the ice and find you. Her lips move. Where her breath touches the ice, it fogs, very faintly.",
                        "The pillar is thick, old and hums with the Queen's magic. Ice like this doesn't care for picks and prayers. It might care about *fire*.")
                .choice("Thaw her with a gentle stream of Fire Bolts").icon("🔥").spell("fire_bolt")
                    .pass("You play fire over the pillar, bolt after careful bolt, until it weeps and sags and finally splits with a sigh. Lissel tumbles out into your arms, warm as a sunny windowsill, and the hall fills with the smell of cut grass.", "ff_spirit_freed", xp(100))
                .choice("Melt the pillar with Burning Hands").icon("🔥").spell("burning_hands")
                    .pass("A roaring fan of flame engulfs the pillar. The ice screams, cracks and bursts into steam, and out of the steam steps Lissel, blinking and laughing, buds on her crown bursting open one by one.", "ff_spirit_freed", xp(100))
                .choice("Hold your hands to the ice and sing a spring song").icon("🎵").check("performance", 13).group()
                    .pass("You lay your palms on the ice and sing — a planting song, a festival reel, anything with sunshine in it. The pillar warms under your hands. Lissel sings with you, faint and sweet, and the ice melts from the inside out.", "ff_spirit_freed", xp(100))
                    .fail("Your song is lovely. The ice does not care for lovely. It bites your palms with frost and holds on.", "ff_spirit", damage("1d4", "cold", "con", 11))
                .choice("Smash the pillar open").icon("💪").check("athletics", 14).chooser()
                    .pass("You put your shoulder into it once, twice, three times — and the pillar shatters. Lissel tumbles free, dazed, and pats your face with tiny warm hands. \"Ow,\" she says. \"Thank you. Ow.\"", "ff_spirit_freed", xp(75))
                    .fail("The ice is harder than you are. Your shoulder goes numb and something in it pops.", "ff_spirit", damageOne("1d6", "cold"))
                .choice("Leave her. There's no time.").icon("↩")
                    .effect(flag("spirit_left"), log("You turn away. Behind you, the faint fog of the spring-child's breath slowly fades from the ice."))
                    .go("ff_kessa_meet");

        c.scene("ff_spirit_freed").chapter(CH2).title("Lissel's Gift").backdrop("hall").time("day").music("hope")
                .onEnter(flag("spring_freed"))
                .npc("Lissel", "The Spring-Child", "#58d68d", "🌱", "You're WARM! You're all so WARM! Oh, I've missed warm!")
                .text("Where Lissel's bare feet touch the floor, frost melts and moss pushes up between the flagstones. She runs around the hall touching everything, and everything she touches tries very hard to bloom.",
                        "Then she stops, and her face falls. \"Isolde froze me. She didn't want to. She cried. The Frostfell made her — it can't let me reach the village, because if spring comes, the bargain breaks.\" She looks up at you. \"But she's still *in* there. Under all the cold. I can feel her, like a seed under snow.\"",
                        "She closes your hand around a bead of amber with a warm green spark inside. \"Take a little bit of me up there. Fire's good. But *spring* is better.\"")
                .choice("Accept the Ember of Spring").icon("🌱")
                    .effect(item("ff_spring_ember"), log("The Ember of Spring can be attuned from your character sheet: +1 to spell attack rolls and spell save DC."))
                    .go("ff_kessa_meet");

        // ---- The Queen's huntress
        c.scene("ff_kessa_meet").chapter(CH2).title("The Queen's Huntress").backdrop("hall").time("day").music("tension")
                .npc("Kessa Rimeheart", "Huntress of the Winter Queen", "#5499c7", "🏹", "Put the child back. Please. You don't know what you're doing.")
                .text("An arrow thrums into the floor at your feet, its head rimed with frost. In the doorway stands a tall woman in white furs, a rimewood bow drawn to her cheek, two snow wolves at her heels. Around her neck hangs a white fox mask — the sled-race judge from Highmarch.",
                        "\"KESSA RIMEHEART,\" she says. \"I hunt for the Queen.\" Her voice is steady. Her bow is not. \"Isolde and I grew up together. When the Red Summer came she climbed this mountain alone, and I followed too late. The pact is the only thing keeping her alive. If spring comes, it breaks — and I lose her for good.\"",
                        "\"So go home,\" she whispers. \"Or I'll have to stop you.\"")
                .choice("Read what she's truly afraid of").icon("👁").once().check("insight", 13)
                    .pass("It isn't you. It isn't even the pact. She's been watching her oldest friend turn to ice for three years, and she's terrified that there's nothing left to save. Underneath the threat is a question: *is there?*", "ff_kessa_meet", flag("kessa_understood"), xp(25))
                    .fail("She's angry and armed. That's all you can read, and frankly it's enough.", "ff_kessa_meet")
                .choice("Show her Lissel, warm and alive").icon("🌱").ifFlag("spring_freed").check("persuasion", 12)
                    .pass("Lissel steps out from behind you and takes Kessa's free hand. Frost melts from Kessa's glove. \"She's still in there,\" the spring-child says. \"I felt her cry when she froze me. Let them try.\" The bow lowers. Kessa's jaw trembles. \"…If you're wrong, I'll never forgive you.\" \"If we're right?\" \"Then I'll owe you everything.\"", "ff_kessa_turns", xp(150))
                    .fail("Kessa flinches from the child's warmth as if it burns. \"No. NO. I've seen what happens when the cold lets go.\" She draws.", "ff_kessa_fight")
                .choice("\"Isolde isn't being kept alive — she's being kept.\"").icon("💬").check("persuasion", 15).advantageIf("kessa_understood")
                    .pass("\"The Frostfell isn't protecting her, Kessa. It's *wearing* her. You've watched her disappear for three years. Help us bring her back.\" A long silence. Then Kessa unstrings her bow, very slowly, like someone putting down something heavy. \"…Tell me what you need.\"", "ff_kessa_turns", xp(150))
                    .fail("\"Pretty words. I've heard the Queen say prettier ones to the snow.\" She whistles, and the wolves spring.", "ff_kessa_fight")
                .choice("Charm her and strike while she hesitates").icon("💗").spell("charm_person")
                    .pass("Warmth shimmers in your eyes and Kessa's bow dips. \"…Who are you?\" she murmurs, confused — and in that moment of doubt, you move first. (The enemy is surprised!)", "ff_kessa_fight", flag("kessa_surprised"))
                .choice("Draw steel").icon("⚔").go("ff_kessa_fight");

        c.scene("ff_kessa_fight").chapter(CH2).title("The Huntress Strikes").backdrop("hall").time("day").music("battle")
                .text("Kessa leaps back onto the gallery, nocking two arrows at once. Her wolves spread out across the frozen floor. \"I'm sorry,\" she says, and means it, and looses.")
                .choice("Bring her down — but try not to kill her").icon("⚔").battle("kessa_fight")
                    .victory("Kessa's bow snaps. She drops to her knees on the ice, breathing hard, and doesn't reach for her knife. \"…Go, then,\" she says hoarsely. \"Save her if you can. I couldn't.\" When you look back from the doorway, she's gone.", "ff_monastery_exit", flag("kessa_spared"), xp(50))
                    .defeat("Kessa's arrows pin you down until you can't stand. She stands over you a long moment, then lowers her bow. \"Go home,\" she whispers, and leaves you in the snow. You don't go home.", "ff_monastery_exit");

        c.scene("ff_kessa_turns").chapter(CH2).title("The Huntress Turns").backdrop("hall").time("day").music("hope")
                .npc("Kessa Rimeheart", "No Longer the Queen's", "#76d7c4", "🏹", "The Queen has totems on the Throne — spires of ice that feed her. Melt them. And for pity's sake don't let her breathe on you.")
                .onEnter(flag("kessa_ally"))
                .text("Kessa sends her wolves home with a word. Then she sits down very suddenly on a frozen bench, as if her strings have been cut, and laughs a small, cracked laugh. \"Three years,\" she says. \"Three years, and all I had to do was let a child hold my hand.\"",
                        "She tells you everything: the Avenue of Sentinels, the Mirror Lake, the Throne itself, and the RIME TOTEMS that pour the Frostfell's strength into the Queen. \"Fire melts them fast. Fire hurts *her* worse.\" A pause. \"Don't kill her if there's any other way. Please.\"",
                        "She restrings her bow. \"I'll meet you at the Throne. I know a quicker way up — for one.\"")
                .choice("\"We'll see you at the top.\"").icon("🤝").go("ff_monastery_exit");

        c.scene("ff_monastery_exit").chapter(CH2).title("The Bell Tower").backdrop("snow").time("dusk").music("snow")
                .onEnter(shortRest())
                .text("You climb the monastery's tallest bell tower and catch your breath among the frozen bells (short rest). From up here you can see the whole of the GLACIER THRONE: a frozen river of blue ice pouring off the summit, and at its head, a spire of glacier-glass glittering in the last light.",
                        "Between you and it runs an avenue of ice statues, and beyond that, a frozen lake that shines like a mirror.",
                        "Somewhere far below, very faintly, you can hear the Great Lantern's festival drums. Highmarch is still loud. Highmarch is still waiting.")
                .choice("Climb on toward the Throne").icon("🏔").go("ff_glacier");
    }

    // =========================================================================================== chapter 3
    static void chapterThree(Campaign c) {
        c.scene("ff_glacier").chapter(CH3).title("The Avenue of Sentinels").backdrop("snow").time("day").music("tension").anchor("dungeon").checkpoint()
                .text("The path to the Throne runs straight up the glacier between two rows of statues: knights carved from blue ice, twice the height of a man, each holding a sword point-down before it. There are fourteen. Their faces are smooth and blank. Snow does not settle on them.",
                        "On either side of the avenue, crevasses split the glacier into the deep blue dark. The wind drops to nothing. It is very, very quiet.")
                .choice("Study the statues before you pass").icon("🔍").check("investigation", 13)
                    .pass("Thirteen of the statues have snow in the creases of their armour. Three don't — and their feet are not frozen to the glacier. You point them out silently, and when they lurch into life, you are already moving.", "ff_sentinel_fight", flag("sentinels_spotted"), xp(25))
                    .fail("Statues. Big, cold, beautiful statues. You walk between them. One of them turns its head.", "ff_sentinel_fight")
                .choice("Test the nearest statue with Burning Hands").icon("🔥").spell("burning_hands")
                    .pass("You wash flame across the nearest statue. It *screams* — a grinding, glacial shriek — and lurches forward with its armour running like candle wax. Three of them, not one, but you've caught them before they were ready.", "ff_sentinel_fight", flag("sentinels_spotted"))
                .choice("Slip past along the crevasse edge").icon("🤫").check("stealth", 14).group()
                    .pass("You edge along the lip of the crevasse, single file, in the statues' blind spots. Fourteen blank faces never turn. When you reach the top of the avenue, you all breathe again.", "ff_cracking_ice", xp(100))
                    .fail("The ice beneath someone's boot creaks. As one, three of the statues lift their heads.", "ff_sentinel_fight");

        c.scene("ff_sentinel_fight").chapter(CH3).title("The Sentinels Wake").backdrop("snow").time("day").music("battle")
                .text("Three sentinels step down from their plinths with a sound like a glacier calving. Their swords lift. Frost rolls off them in waves.",
                        "(The avenue is edged by crevasses — a good shove will send a sentinel down into the dark. And they really, really don't like fire.)")
                .choice("Fight!").icon("⚔").battle("sentinels")
                    .victory("The last sentinel topples and shatters across the glacier in a spray of blue shards. Beyond the avenue, the path drops to the shore of a vast frozen lake.", "ff_cracking_ice", xp(25))
                    .defeat("You retreat down the glacier, battered. When you creep back at dusk, the sentinels have returned to their plinths — but they don't wake for a second pass. Beyond them, a vast frozen lake.", "ff_cracking_ice");

        c.scene("ff_cracking_ice").chapter(CH3).title("The Mirror Lake").backdrop("snow").time("dusk").music("mystery")
                .text("Below the Throne lies a frozen lake so clear and still it reflects the sky perfectly. Walking onto it is like walking on air. Beneath the surface, very deep down, you can see things: a sunken sled, a frozen deer, the pale faces of the Queen's wolves who came this way and fell through.",
                        "The ice is etched with patterns — snowflakes, mostly, and here and there, a tiny carved flower. As you watch, a crack zigzags across a snowflake tile with a sound like a pistol shot.")
                .choice("Step only on the flower tiles").icon("🌼").check("investigation", 13)
                    .pass("The snowflakes are traps — thin as paper. The flowers are spring's mark, thick and true. You hop from flower to flower across the lake, and the cracks chase you but never catch you.", "ff_camp", xp(50))
                    .fail("You're sure it's the snowflakes. It is not the snowflakes.", "ff_crack_trap")
                .choice("Refreeze the cracks with Ray of Frost").icon("❄").spell("ray_of_frost")
                    .pass("For once on this mountain, cold is your friend. You fire ray after ray into the spreading cracks and they seal shut behind a crust of fresh white ice. It's slow, careful work — and you cross without a single slip.", "ff_camp", xp(50))
                .choice("Sled across on your shields — you've done the Widowmaker!").icon("🛷").ifFlag("sled_champion")
                    .effect(xp(50), log("You throw down a shield, pile on, and kick off. The ice cracks behind you in a racing line, but you're faster: you hit the far shore at full speed and plough into a snowbank, whooping. Sigge the Loud would be proud."))
                    .go("ff_camp")
                .choice("Spread out and cross lightly").icon("🦶").check("acrobatics", 12).group()
                    .pass("You spread your weight, step soft, and glide across the lake like dancers. The cracks never quite catch up.", "ff_camp")
                    .fail("Someone steps wrong. The lake shrieks—", "ff_crack_trap");

        c.scene("ff_crack_trap").chapter(CH3).title("The Lake Breaks").backdrop("snow").time("dusk").music("tension")
                .text("The ice shatters in a starburst around you, and black water heaves up through the cracks!")
                .choice("Leap for the solid ice!").icon("🧊").check("save:dex", 13).each()
                    .pass("You leap from floe to floe and haul yourself out onto the far shore, soaked but whole.", "ff_camp")
                    .fail("You plunge into water so cold it feels like fire. Hands drag you out, gasping and blue.", "ff_camp", damageOne("2d6", "cold"));

        c.scene("ff_camp").chapter(CH3).title("Camp Beneath the Throne").backdrop("camp").time("night").music("camp").anchor("camp")
                .onEnter(level(4), longRest())
                .text("You make your last camp in the lee of a boulder below the Throne. The fire is small and brave. The stars are enormous. Ragna tells the story of the three travellers she lost, and for once it isn't funny, and for once nobody minds.",
                        "You sleep, and wake stronger than you have ever been: steadier hands, surer magic, a fire inside you that the cold can't reach. **(Every hero reaches level 4 and finishes a long rest!)**",
                        "Above you, the Glacier Throne glitters. Tomorrow it ends, one way or another.")
                .choice("Tell stories round the fire").icon("🔥").once().check("performance", 11)
                    .pass("You tell the tale of Granny Thorvild and the boiling cider, and by the end even Ragna is laughing so hard she has to lie down. Courage, it turns out, is warm. (Heroic Inspiration)", "ff_camp", inspiration())
                    .fail("Your story wanders off somewhere around the third yak and never comes back. Still. It's nice to laugh.", "ff_camp")
                .choice("Talk with Kessa by the fire").icon("🏹").once().ifFlag("kessa_ally")
                    .effect(log("Kessa arrives out of the dark, as promised, and sits close to the fire. \"She used to light the Great Lantern standing on my shoulders,\" she says. \"Every year. She'd say: one day I'll make it big enough to see from the top of the world.\" A small smile. \"I suppose she got her wish.\""))
                    .go("ff_camp")
                .choice("Hold Lissel's Ember to the flames").icon("🌱").once().ifFlag("spring_freed")
                    .effect(heal("2d8"), log("The Ember of Spring glows. Around your camp, tiny green shoots push up through the snow, and the air smells, impossibly, of rain on grass. Your wounds feel lighter."))
                    .go("ff_camp")
                .choice("Keep watch over the glacier").icon("👁").once().check("perception", 12)
                    .pass("Near midnight, you see it: a figure in white standing at the edge of the Throne, high above, looking down at your fire. Just looking. After a long while, she turns away. You don't think she meant you harm. You think she was cold.", "ff_camp", flag("saw_isolde"))
                    .fail("Stars. Ice. Wind. A long, cold, uneventful watch.", "ff_camp")
                .choice("Climb to the Throne at dawn").icon("🏔").go("ff_throne_stair");

        c.scene("ff_throne_stair").chapter(CH3).title("The Stair of Winter").backdrop("hall").time("dawn").music("eerie").checkpoint()
                .text("A stair carved into the glacier spirals up into the heart of the Throne. The walls are clear as glass, and frozen inside them, you see the Red Summer: burning pines, fleeing deer, sparks caught mid-flight like fireflies in amber — all of it stopped, perfect, forever.",
                        "At the top, a doorway of blue ice. Cold pours from it like water. Somewhere beyond, a woman is humming the Lantern-Keeper's song.")
                .choice("Light Hrolla's little lantern").icon("🏮").once().ifItem("ff_ashgrove_lantern")
                    .effect(inspiration(), log("The candle stub in Hrolla's lantern catches with a tiny, stubborn flame. It shouldn't be able to burn in this cold. It burns anyway. (Heroic Inspiration)"))
                    .go("ff_throne_stair")
                .choice("Prepare spells and gear (open your character sheet)").icon("📖").once()
                    .effect(log("Take a moment: attune magic items, swap prepared spells, and hand alchemist's fire to whoever throws best."))
                    .go("ff_throne_stair")
                .choice("Step through into the Throne").icon("👑").go("ff_throne");
    }

    // =========================================================================================== finale
    static void finale(Campaign c) {
        c.scene("ff_throne").chapter(FIN).title("The Glacier Throne").backdrop("lair").time("day").music("boss")
                .npc("Isolde", "The Winter Queen", "#d6eaf8", "👑", "You've come such a long way to make Highmarch burn.")
                .text("The Throne Hall is a cathedral of ice, its ceiling lost in blue shadow. Spires of humming rime stand around a great seat of glacier-glass, pouring cold into the woman who sits upon it.",
                        "She is young. That is the first shock. ISOLDE ASHGROVE has her mother's jaw and a crown of icicles, and her skin has the faint blue glow of deep ice. Her breath doesn't steam. Her eyes are the colour of the bottom of the lake. On a pedestal beside the throne, under a dome of ice, sits a small iron lantern, unlit.",
                        "\"I know why you're here,\" she says softly. \"Everyone in Highmarch thinks I'm cruel. But the fire is still out there, waiting, and the moment I let go, it will come back. I saved them. I am *still* saving them.\" Behind her, the Frostfell's cold rises like a tide.")
                .choice("Tell her what her winter has truly cost").icon("💬").ifFlag("spring_freed").check("persuasion", 14).advantageIf("hrolla_truth")
                    .pass("You tell her about Dagny's thin yaks, and Granny Thorvild who remembers the Red Summer and isn't afraid, and Lissel, warm and alive, who still feels her crying under the ice. You hold out Hrolla's lantern. \"Your mother says: that's enough now.\" Isolde's hands begin to shake. Frost-tears roll down her cheeks — and the Frostfell, sensing its Queen slipping away, *howls*.", "ff_isolde_wavers", xp(200))
                    .fail("\"You don't understand,\" Isolde whispers. \"Nobody does.\" She rises, and the hall grows colder.", "ff_isolde_wrath")
                .choice("\"Isolde. Your mother sent us. She wants you home.\"").icon("🏮").ifFlag("hrolla_truth").check("persuasion", 16)
                    .pass("The name lands like a stone through ice. \"…Mother?\" For a moment she is only a frightened young woman on a cold chair. Then the crown flares white and the Frostfell surges through her, fighting to keep its Queen.", "ff_isolde_wavers", xp(200))
                    .fail("\"My mother,\" says Isolde, \"is warm and safe because of me. Go home and tell her so.\" She rises.", "ff_isolde_wrath")
                .choice("Strike while she's still talking").icon("🎭").check("stealth", 15)
                    .pass("While she speaks, you circle into the shadows between the totems — and when she rises, you're already at her side.", "ff_isolde_wrath", flag("isolde_surprised"))
                    .fail("\"Please,\" says Isolde, without even looking at you, \"I can hear your heartbeat from here.\"", "ff_isolde_wrath")
                .choice("\"Then we'll end your winter by force.\"").icon("🔥").battle("isolde")
                    .victory("", "ff_victory");

        c.scene("ff_isolde_wavers").chapter(FIN).title("The Queen Wavers").backdrop("lair").time("day").music("boss")
                .onEnter(flag("isolde_wavers"))
                .text("Isolde staggers back against her throne, clawing at the crown of icicles. \"I can't — it won't — *let me go*—\" The rime totems flare. The Frostfell pours into her, and her eyes go white. When she speaks again, it is with two voices, hers and something vast and old.",
                        "*\"SHE IS MINE. SHE ASKED. SHE WILL ALWAYS BE MINE.\"*",
                        "But you can see it now: the Queen is fighting it too. The cold is thinner around her heart. **(Isolde starts the fight weakened and without Legendary Resistance.)** Break the Frostfell's hold, and you might bring her home.")
                .choice("Break the Frostfell's hold!").icon("🔥").battle("isolde")
                    .victory("", "ff_victory");

        c.scene("ff_isolde_wrath").chapter(FIN).title("The Winter Queen's Wrath").backdrop("lair").time("day").music("boss")
                .text("Isolde rises from the Throne, and the temperature drops so fast the air itself begins to crackle. Her crown blazes white. Above you, the ceiling groans, and icicles the size of pine trees shiver loose.",
                        "\"Then I'll protect them from *you*, too.\"")
                .choice("Fight for the spring!").icon("⚔").battle("isolde")
                    .victory("", "ff_victory");

        c.scene("ff_victory").chapter(FIN).title("The Thaw").backdrop("lair").time("day").music("triumph")
                .text("The crown of icicles shatters. A shockwave of warm wind rolls out of the Throne Hall, and everywhere — on the walls, the floor, the throne — the ice begins to *weep*. Water runs. Rime totems slump into puddles. Somewhere deep in the glacier, something vast and old sighs, rolls over, and goes back to sleep.",
                        "Isolde lies at the foot of the throne, very still. Then the glacier beneath your feet groans like a ship's hull — and the whole Throne begins to slide.",
                        "RUN.")
                .choice("Run for the stair!").icon("🏃").check("athletics", 12).group()
                    .pass("You sprint down the weeping stair with the glacier cracking at your heels, carrying Isolde between you, and burst out into sunlight — *real* sunlight, gold and warm — as the Glacier Throne collapses into a roaring river of meltwater behind you.", "ff_epilogue", xp(200))
                    .fail("Falling ice batters you all the way down, but you make it, bruised, soaked and laughing like lunatics, carrying Isolde between you — out into sunlight as the Glacier Throne becomes a river behind you.", "ff_epilogue", damage("2d6", "bludgeoning", "dex", 12), xp(200));

        c.scene("ff_epilogue").chapter("Epilogue").title("Spring Comes to Highmarch").backdrop("village").time("day").music("triumph").anchor("epilogue")
                .onEnter(gold(100), longRest())
                .npc("Elder Hrolla Ashgrove", "Elder of Highmarch", "#6e2c00", "🧓", "Well. Don't just stand there dripping. Come in. All of you. Torvald's crying again — it's a whole thing.")
                .text("By the time you reach Highmarch, the snow is going. It runs off the steep roofs in ropes of silver, chuckles down every lane, and pools in the square around the Great Lantern. Under it, the grass is pale and flattened and *green*. Children are lying in it. Dagny's yaks are eating it. Mabel is eating Dagny's hat.",
                        "Highmarch throws a second festival, and this one is called simply THE THAW. Torvald weeps through all of it. Pell sings a new song about you that rhymes \"glacier\" with \"face, sir\", and nobody minds. Elder Hrolla pays every coin she promised, and then some.",
                        "Somewhere high above, the mountain is green again. And somewhere out past the edge of the map, other roads are waiting.")
                .choice("Bring Isolde home to her mother").icon("🏮").once().ifFlag("isolde_wavers")
                    .effect(log("Isolde wakes in her mother's longhouse with her own warm colour back in her cheeks and her own voice in her throat. Hrolla doesn't say anything at all. She just turns the painting above the hearth back around to face the room. That night, Isolde lights the Great Lantern — standing on Kessa's shoulders, if she's here, and on Torvald's if not — and it can be seen from the top of the world."))
                    .go("ff_epilogue")
                .choice("Hang Isolde's lantern on the Great Lantern").icon("🕯").once().unlessFlag("isolde_wavers")
                    .effect(log("Isolde does not wake. The Frostfell kept too much of her. Hrolla takes her daughter's little iron lantern from you without a word, and that night she hangs it, lit, at the very top of the Great Lantern. \"She saved us,\" she says at last. \"Twice. Once badly, and once well.\" Highmarch never puts it out again."))
                    .go("ff_epilogue")
                .choice("Watch Lissel run through the square").icon("🌱").once().ifFlag("spring_freed")
                    .effect(log("The spring-child runs laughing through Highmarch, and wherever she goes, flowers burst from window-boxes, cracks in the cobbles and Ragna's beard. Ragna pretends to be furious. She leaves them in for a week."))
                    .go("ff_epilogue")
                .choice("Find Kessa at the edge of the festival").icon("🏹").once().ifFlag("kessa_ally")
                    .effect(log("Kessa has hung up her white fox mask on the lodge wall, among the antlers. \"I'm a huntress with nothing left to hunt,\" she says. Then she grins. \"Torvald says he needs someone to judge the sled race. Properly, this time.\""))
                    .go("ff_epilogue")
                .choice("Let Mabel lead the festival parade").icon("🐂").once().ifFlag("mabel_friend")
                    .effect(log("Mabel leads the parade wearing a crown of spring flowers and most of the festival bunting. Dagny swears she's never seen a yak look so smug. Mabel has fought a Winter Queen. Mabel is allowed to be smug."))
                    .go("ff_epilogue")
                .choice("Accept the Frostjaws' rematch").icon("☃").once().ifFlag("frostjaw_friends")
                    .effect(log("There's no snow left for a snowball fight, so the Frostjaw brothers challenge you to a mud-ball fight instead. It is the greatest battle of the age. Nobody wins. Everybody has to be hosed down."))
                    .go("ff_epilogue")
                .choice("Collect the extra reward Hrolla promised").icon("💰").once().ifFlag("reward_up").effect(gold(50)).go("ff_epilogue")
                .choice("Raise a mug of Torvald's cider and end the tale").icon("🏁").go("@end");
    }

    // =========================================================================================== battle maps
    static void encounters(Campaign c) {
        c.encounter("hearth_brawl").title("Brawl at the Antlered Hearth").backdrop("tavern").music("brawl").nonlethal()
                .intro("Benches scrape, mugs fly, and the enormous Frostjaw clan rolls up its sleeves. No weapons, no fire, no biting — nobody dies in a festival brawl, but everybody hurts.")
                .map("######################",
                     "#BB....B.....###..BB.#",
                     "#.......f....###..f..#",
                     "#..BB...........BB...#",
                     "#..BB......s....BB...#",
                     "#..........E.........#",
                     "#...B..........f..B..#",
                     "#........f...........#",
                     "#..PP.....BB.........#",
                     "#..PP.....BB....BB...#",
                     "#BB.............BB...#",
                     "######################")
                .legend('f', "ff_frostjaw").legend('s', "ff_sten").extra("ff_frostjaw");

        c.encounter("festival_wolves").title("Wolves in the Square").backdrop("town").music("battle")
                .intro("The Queen's wolves have come to snuff the Great Lantern. Protect the festival! (The lantern's brazier is burning — shove a wolf into it.)")
                .surpriseIf("wolves_dazzled")
                .ally("ff_frostjaw_ally", "frostjaw_friends")
                .map("######################",
                     "#..B....,,,,,....B...#",
                     "#.w.....,,,,,.....w..#",
                     "#.......,LLL,........#",
                     "#..B....,LLL,....B...#",
                     "#..........h.........#",
                     "#.....E.........w....#",
                     "#..B.............B...#",
                     "#....................#",
                     "#.....PP....PP.......#",
                     "#.....PA....AP.......#",
                     "######################")
                .legend('w', "ff_snow_wolf").legend('h', "ff_queens_hound").extra("ff_snow_wolf");

        c.encounter("ice_bridge").title("Rimefang's Bridge").backdrop("bridge").music("battle")
                .intro("The Skaldvik Ice Bridge spans a bottomless chasm, and the ice around it is slick. Shove a wolf over the edge and it never comes back!")
                .surpriseIf("bridge_spotted")
                .map("TTTT,,,,CCCCCCCC,,,,TTTT",
                     "T,,,,,,,CCCCCCCC,,,,,,TT",
                     ",,,,II..CCCCCCCC..IIw,,T",
                     ",PP.III.CCCCCCCC.III..,,",
                     ",PP..II.========.II.r..,",
                     ",PP.III.========.III...,",
                     ",,..II..CCCCCCCC..II.E,,",
                     "T,,,,,..CCCCCCCC.w,,w,TT",
                     "TT,,,,,,CCCCCCCC,,,,,TTT",
                     "TTTT,,,,CCCCCCCC,,,,TTTT")
                .legend('r', "ff_rimefang").legend('w', "ff_snow_wolf").extra("ff_snow_wolf");

        c.encounter("yeti_den").title("Grumblegut's Larder").backdrop("cave").music("battle")
                .intro("Grumblegut wants his cave back, and Junior wants a snack. Yetis are VULNERABLE TO FIRE — and the icy floor makes footing treacherous.")
                .map("######################",
                     "######,,,,,,,,,,######",
                     "####,,....II.....,,###",
                     "###,....III..y.....,##",
                     "##,.....II.......j..,#",
                     "#,.........II...j...,#",
                     "#,..B.....IIII...E..,#",
                     "#,.......,,,,.......,#",
                     "##,....PP....PP....,##",
                     "###,...PP....PP...,###",
                     "######,,,,..,,,,######")
                .legend('y', "ff_grumblegut").legend('j', "ff_yeti_cub").extra("ff_yeti_cub");

        c.encounter("mephit_cloister").title("Mephit Mayhem").backdrop("ruins").music("battle")
                .intro("Ice mephits swirl through the frozen cloister, shrieking with glee. They're VULNERABLE TO FIRE AND BLUDGEONING — and they burst when they die, so don't stand too close!")
                .map("######################",
                     "#..m.....#..#.....m..#",
                     "#.#...#........#...#.#",
                     "#......IIIIIIII......#",
                     "#.#...#I~~~~~~I#...#.#",
                     "#......I~~~~~~I...m..#",
                     "#.#...#IIIIIIII#...#.#",
                     "#....m.....E.....m...#",
                     "#.#...#...m....#...#.#",
                     "#.......PP..PP.......#",
                     "#########......#######")
                .legend('m', "ice_mephit").extra("ice_mephit");

        c.encounter("kessa_fight").title("The Queen's Huntress").backdrop("hall").music("battle").objective("defeat:ff_kessa")
                .intro("Kessa fights from the far gallery while her wolves close in. Bring her down and the fight is over (she'll yield, not die).")
                .surpriseIf("kessa_surprised")
                .map("######################",
                     "#....##...k...##.....#",
                     "#....##.......##.....#",
                     "#...........B........#",
                     "#..w....IIIIII....w..#",
                     "#.......IIIIII.......#",
                     "#....B...........B...#",
                     "#..........E....h....#",
                     "#....................#",
                     "#.......PP..PP.......#",
                     "#########......#######")
                .legend('k', "ff_kessa").legend('w', "ff_snow_wolf").legend('h', "ff_queens_hound").extra("ff_snow_wolf");

        c.encounter("sentinels").title("The Sentinels Wake").backdrop("snow").music("battle")
                .intro("Rime Sentinels stride down the Avenue. They RESIST piercing and slashing but are VULNERABLE TO FIRE — and crevasses line the avenue, so shove them into the dark!")
                .surpriseIf("sentinels_spotted")
                .map("CCCCCC..............CCCC",
                     "CCCC.....s......s.....CC",
                     "CC.....III....III.....CC",
                     "C......III....III......C",
                     "C..........s...........C",
                     "CC.......IIIIII.......CC",
                     "CCC......IIIIII..E...CCC",
                     "CC....................CC",
                     "C......PP......PP......C",
                     "CC.....PP......PP.....CC",
                     "CCCC................CCCC")
                .legend('s', "ff_ice_sentinel").extra("ff_ice_sentinel");

        c.encounter("isolde").title("Isolde, the Winter Queen").backdrop("lair").music("boss").objective("defeat:ff_isolde")
                .intro("ISOLDE rises from the Glacier Throne. Three RIME TOTEMS feed her the Frostfell's strength — while they stand, she heals every turn (they're VULNERABLE TO FIRE!). She is IMMUNE TO COLD and VULNERABLE TO FIRE. Crevasses split the hall. When the ceiling cracks, watch for falling icicles!")
                .surpriseIf("isolde_surprised")
                .weakenIf("isolde_wavers")
                .ally("ff_kessa_ally", "kessa_ally")
                .ally("ff_mabel", "mabel_friend")
                .phase(66, "The Frostfell roars through its Queen and the ceiling splits — icicles begin to FALL!",
                        Json.obj("op", "hazard", "count", 3, "type", "cold", "dice", "2d6", "name", "Falling icicles", "dc", 13),
                        Json.obj("op", "say", "text", "\"I won't let the fire come back. I WON'T!\""))
                .phase(40, "Ice mephits tear themselves out of the walls, shrieking to defend their Queen!",
                        Json.obj("op", "summon", "monster", "ice_mephit", "count", 2),
                        Json.obj("op", "say", "text", "*\"SHE IS MINE. SHE ASKED. SHE WILL ALWAYS BE MINE.\"*"))
                .phase(15, "Isolde's crown cracks down the middle. For a heartbeat, her own voice breaks through.",
                        Json.obj("op", "say", "text", "\"…Mother? Is it… is it spring?\""))
                .map("##########################",
                     "#CCC.....t...q....t...CCC#",
                     "#CC....................CC#",
                     "#C....III........III....C#",
                     "#.....III...,,...III.....#",
                     "#..........t.....E.......#",
                     "#...CC..............CC...#",
                     "#...CC....I....I....CC...#",
                     "#........................#",
                     "#.....III........III.....#",
                     "#.........PP..PP.........#",
                     "#.........PA..AP.........#",
                     "##########################")
                .legend('q', "ff_isolde").legend('t', "ff_rime_totem").extra("ice_mephit");
    }
}
