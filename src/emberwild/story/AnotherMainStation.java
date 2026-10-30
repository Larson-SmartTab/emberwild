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
 * ANOTHER MAIN STATION — a SmartTab one-shot from the team's Java challenge (DndGame.java), rebuilt for Emberwild.
 * <pre>
 *  Chapter 1  The Back Office ......... swap in ANOTHER new main station and pair the Dynaflexes
 *  Chapter 2  Meanwhile, at the Bar ... Chad the Toast rep pulls up a stool and starts talking
 *  Chapter 3  The Van ................. hot dogs, iPads, a POS bank, Monsters, a spare Dynaflex, a box of iPhones
 *  Chapter 4  The Trash Talk .......... three rounds of comebacks (how many land changes the fight)
 *  Chapter 5  The Bar Fight ........... Chad, flying pint glasses, and the old main station hitting Payam in the head
 *  Aftermath  ......................... does Monroe stay SmartTab?
 * </pre>
 * Same story, same lines and the same five reps as the original. What changed for Emberwild:
 * Uptime is hit points, Signal is Charisma (the Box of iPhones adds +3 to Charisma checks), Monster Energy and fries
 * are drinkable healing, and "Call SmartTab Support on speaker" is a one-use throwable that does psychic damage to egos.
 * Your comeback choice now matters (each one uses a different skill), and your rank comes from how well you did,
 * not from how little you spent in the van. Custom ids are prefixed {@code am_}.
 */
public final class AnotherMainStation implements CampaignScript {

    static final String CH1 = "Chapter 1 — The Back Office";
    static final String CH2 = "Chapter 2 — Meanwhile, at the Bar";
    static final String CH3 = "Chapter 3 — The Van";
    static final String CH4 = "Chapter 4 — The Trash Talk";
    static final String CH5 = "Chapter 5 — The Bar Fight";
    static final String END = "The Aftermath";

    @Override
    public Campaign build() {
        Campaign c = Campaign.create("another_main_station", "Another Main Station", "SmartTab Support Team");
        c.cover("tavern");
        c.synopsis("It's a Friday afternoon, and you're pulling the SmartTab van up to Monroe. Again. The last main station got "
                + "broken, and Payam the bartender swears he didn't drop it. Your job: swap in ANOTHER new main station, get the "
                + "Dynaflexes paired back up, and get out before the rush. Then Chad the Toast rep walks in. A comedy one-shot "
                + "for 1-4 SmartTab reps at level 1 (about 30 minutes); bar staff fill a small crew.");

        pregens(c);
        monsters(c);
        items(c);
        story(c);
        trashTalk(c);
        fightAndAftermath(c);
        encounters(c);
        return c;
    }

    // =========================================================================================== the reps
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
        // The five SmartTab reps from the original, in the same order.
        c.pregen(hero("am_drive_smasher", Json.obj(
                "name", "The Drive Smasher", "pronouns", "he/him", "class", "fighter", "species", "human", "background", "soldier",
                "method", "pointbuy", "scores", scores(15, 13, 14, 8, 12, 10), "increases", Json.obj("str", 2, "con", 1),
                "skills", List.of("athletics", "perception"),
                "color", "#2e86c1", "emblem", "💽", "hair", "#3e2723", "skin", "#d7a77f", "eyes", "#5d4037",
                "appearance", "SmartTab polo, tool bag, and the calm of someone who has never met a hard drive he couldn't break.",
                "personality", "Balanced. Good at everything, great at nothing, and somehow always the one they send to Monroe.",
                "motto", "Another day, another main station.")));
        c.pregen(hero("am_jd", Json.obj(
                "name", "MR janitor JD", "pronouns", "he/him", "class", "barbarian", "species", "dwarf", "background", "farmer",
                "method", "pointbuy", "scores", scores(15, 13, 14, 8, 12, 10), "increases", Json.obj("str", 2, "con", 1),
                "skills", List.of("athletics", "intimidation"),
                "color", "#7d6608", "emblem", "🧹", "hair", "#212121", "skin", "#c68642", "eyes", "#4e342e",
                "appearance", "SmartTab polo, a mop he refuses to explain, and more Uptime than anyone in the van.",
                "personality", "Cleans up every mess: cable runs, bar floors, other people's installs.",
                "motto", "I'll clean it up. I always clean it up.")));
        c.pregen(hero("am_larson_jr", Json.obj(
                "name", "Larson Jr", "pronouns", "he/him", "class", "wizard", "species", "human", "background", "sage",
                "method", "pointbuy", "scores", scores(8, 14, 14, 15, 12, 8), "increases", Json.obj("int", 2, "con", 1),
                "skills", List.of("arcana", "investigation"),
                "cantrips", List.of("fire_bolt", "shocking_grasp", "ray_of_frost"),
                "spells", List.of("mage_armor", "shield", "magic_missile", "charm_person"),
                "color", "#8e44ad", "emblem", "📶", "hair", "#6d4c41", "skin", "#f1c27d", "eyes", "#1565c0",
                "appearance", "SmartTab polo, three chargers, and the most Bandwidth in the company.",
                "personality", "Treats every network like a puzzle and every puzzle like a personal challenge.",
                "motto", "Have you tried turning it off and on again? I have. Twice.")));
        c.pregen(hero("am_vijay", Json.obj(
                "name", "Vijay", "pronouns", "he/him", "class", "bard", "species", "human", "background", "entertainer",
                "method", "pointbuy", "scores", scores(8, 14, 13, 10, 12, 15), "increases", Json.obj("cha", 2, "dex", 1),
                "skills", List.of("persuasion", "deception", "insight"),
                "color", "#16a085", "emblem", "📱", "hair", "#1b1b1b", "skin", "#a1665e", "eyes", "#3e2723",
                "appearance", "SmartTab polo, perfect posture, and the strongest Signal in the van.",
                "personality", "Can pair any device and win any argument. Usually at the same time.",
                "motto", "Full bars. Every time.")));
        c.pregen(hero("am_dre", Json.obj(
                "name", "Dre", "pronouns", "he/him", "class", "rogue", "species", "human", "background", "criminal",
                "method", "pointbuy", "scores", scores(10, 15, 14, 12, 10, 10), "increases", Json.obj("dex", 2, "con", 1),
                "skills", List.of("acrobatics", "athletics", "intimidation", "perception"),
                "color", "#c0392b", "emblem", "🥊", "hair", "#000000", "skin", "#8d5524", "eyes", "#3e2723",
                "appearance", "SmartTab polo with the sleeves rolled up. Hits harder; has a little less Uptime to spare.",
                "personality", "Quiet until somebody insults the product.",
                "motto", "Say it again, Chad.")));

        // ---- Monroe's staff: they back you up when the crew is small.
        Hero rosa = hero("am_rosa", Json.obj(
                "name", "Rosa, Monroe's Line Cook", "pronouns", "she/her", "class", "cleric", "species", "human", "background", "acolyte",
                "method", "pointbuy", "scores", scores(12, 12, 14, 8, 15, 11), "increases", Json.obj("wis", 2, "cha", 1),
                "skills", List.of("medicine", "insight"),
                "cantrips", List.of("sacred_flame", "guidance", "toll_the_dead"),
                "spells", List.of("healing_word", "cure_wounds", "bless", "guiding_bolt"),
                "color", "#e67e22", "emblem", "🍟", "hair", "#4a2511", "skin", "#c68642", "eyes", "#3e2723",
                "appearance", "Kitchen whites, a fry basket, and the patience of someone who has worked a Friday rush.",
                "personality", "Patches everybody up with fries and opinions. Has never once liked Chad.",
                "motto", "Eat something. Then hit him."));
        rosa.npc = true;
        c.pregen(rosa);
        Hero ray = hero("am_ray", Json.obj(
                "name", "Big Ray, Monroe's Door Guy", "pronouns", "he/him", "class", "fighter", "species", "human", "background", "guard",
                "method", "pointbuy", "scores", scores(15, 12, 14, 8, 13, 10), "increases", Json.obj("str", 2, "wis", 1),
                "skills", List.of("athletics", "intimidation"),
                "color", "#34495e", "emblem", "🚪", "hair", "#111111", "skin", "#7b4b2a", "eyes", "#3e2723",
                "appearance", "Six and a half feet of black T-shirt that says STAFF.",
                "personality", "Checks IDs, breaks up fights, and is very tired of Toast reps.",
                "motto", "Not in my bar."));
        ray.npc = true;
        c.pregen(ray);
    }

    // =========================================================================================== monsters & gear
    static void monsters(Campaign c) {
        c.monster("am_chad", "Chad the Toast Rep").type("humanoid").role("brute").ac(13).hp(70).stats(15, 12, 14, 10, 8, 13)
                .cr(1, 200).multi(2)
                .attack(new Monsters.Attack("Wet Bar Rag", 5, "1d8+3", "bludgeoning"))
                .attack(new Monsters.Attack("Thrown Pint Glass", 5, "1d6+2", "bludgeoning").ranged(20, 60))
                .special(Monsters.special("bar_stool", "Chucks a Bar Stool").shape("sphere", 5, 30).dmg("2d6", "bludgeoning").dc(12, Ability.DEX).recharge(5))
                .special(Monsters.special("beer_cooler", "Tackle into the Beer Cooler").shape("sphere", 5, 0).dmg("2d6", "bludgeoning").dc(12, Ability.STR).rider("prone").recharge(5))
                .look("#e74c3c", "🍞").blurb("The Toast rep. Shirt tucked into his jeans. Six months free, then fifteen years of contract.");
        c.monster("am_chad_confident", "Chad the Toast Rep (Confident)").type("humanoid").role("brute").ac(14).hp(76).stats(15, 12, 14, 10, 8, 15)
                .cr(1, 200).multi(2)
                .attack(new Monsters.Attack("Wet Bar Rag", 5, "1d8+3", "bludgeoning"))
                .attack(new Monsters.Attack("Thrown Toast Handheld", 5, "1d8+2", "bludgeoning").ranged(20, 60))
                .special(Monsters.special("bar_stool", "Chucks a Bar Stool").shape("sphere", 5, 30).dmg("2d6", "bludgeoning").dc(12, Ability.DEX).recharge(5))
                .special(Monsters.special("beer_cooler", "Tackle into the Beer Cooler").shape("sphere", 5, 0).dmg("2d6", "bludgeoning").dc(12, Ability.STR).rider("prone").recharge(5))
                .look("#e74c3c", "🍞").blurb("He won the trash talk 3-0, and he will not stop mentioning it.");
        c.monster("am_lime_bowl", "Bowl of Limes").type("object").role("ranged").ac(10).hp(4).speed(0).stats(1, 1, 1, 1, 1, 1)
                .cr(0, 10).condImmune("charmed", "frightened", "poisoned", "prone")
                .attack(new Monsters.Attack("Lime Juice in the Eye", 3, "1d4", "acid").ranged(15, 15))
                .look("#7dcea0", "🍋").blurb("Chad threw it. It's still somehow attacking you.");
        c.monster("am_toast_intern", "Toast Sales Intern").type("humanoid").role("melee").ac(11).hp(11).stats(11, 12, 11, 10, 8, 12)
                .cr(0.125, 25).attack("Clipboard", 4, "1d6+1", "bludgeoning")
                .look("#f5b7b1", "📋").blurb("Chad brought backup. It's his first week. He has been told this is 'a team-building exercise'.");
    }

    static void items(Campaign c) {
        // consumables (drink them in or out of a fight)
        c.item("am_monster_energy", "Monster Energy", "potion", "Crack it and chug it. Restores 10 Uptime (hit points). Bonus Action.",
                Json.obj("use", "10", "value", 1500));
        c.item("am_fries", "Basket of Fries", "potion", "Stolen off the bar. Restores 5 Uptime (hit points). Bonus Action.",
                Json.obj("use", "5", "value", 0));
        c.item("am_support_call", "SmartTab Support (on speaker)", "bomb",
                "Once per fight: call SmartTab Support on speaker. They pick up on the first ring and roast Toast so hard the whole bar goes \"OHHHH.\" Ranged attack (20 ft): 2d8 psychic damage to the target's ego.",
                Json.obj("use", "2d8", "damageType", "psychic", "value", 0));
        // the van (attune to them from your character sheet)
        c.item("am_spare_ipad", "Spare iPad", "wondrous", "For the new main station. More Bandwidth: +1 to spell attacks and spell save DC, +1 initiative.",
                Json.obj("bonus", Json.obj("spell_attack", 1, "spell_dc", 1, "initiative", 1), "attunable", true, "value", 3000));
        c.item("am_pos_bank", "POS Bank", "wondrous", "Charges the iPhones and Dynaflexes — and you. +5 Max Uptime (hit points) per level.",
                Json.obj("bonus", Json.obj("hp_per_level", 5), "attunable", true, "value", 5000));
        c.item("am_spare_dynaflex", "Spare Dynaflex", "weapon", "Paired to a backup iPhone and clipped to your belt. +1 to weapon attack rolls and +2 to weapon damage.",
                Json.obj("bonus", Json.obj("attack", 1, "damage", 2), "attunable", true, "value", 6000));
        c.item("am_box_of_iphones", "Box of iPhones", "wondrous", "For the servers. Better Signal: +3 to Charisma checks (Persuasion, Deception, Performance, Intimidation).",
                Json.obj("bonus", Json.obj("cha_check", 3), "attunable", true, "value", 4000));
    }

    // =========================================================================================== chapters 1-3
    static void story(Campaign c) {
        c.scene("am_start").chapter("Another Main Station").title("Friday Afternoon at Monroe").backdrop("town").time("day").music("explore")
                .onEnter(companion("am_rosa", 3), companion("am_ray", 3))
                .text("It's a Friday afternoon, and you're pulling the SmartTab van up to MONROE. Again.",
                        "The last main station got broken. Payam the bartender swears he didn't drop it.",
                        "Your job: swap in ANOTHER new main station, get the Dynaflexes paired back up, and get out before the rush.")
                .choice("Grab the tool bag and head inside").icon("🧰")
                    .effect(item("am_monster_energy"), item("am_monster_energy"), item("am_fries"),
                            log("You stuff a couple of Monsters and a basket of fries from yesterday into your tool bag. Professional."))
                    .go("am_back_office");

        c.scene("am_back_office").chapter(CH1).title("The Back Office").backdrop("tavern").time("day").music("mystery")
                .ambience("crowd")
                .aside("Every rep has a Monroe. This is yours.")
                .npc("Payam", "Bartender at Monroe", "#1f618d", "🍸", "I didn't drop it.")
                .text("You walk in, wave to Payam at the bar, and drop the old broken main station on the bar on your way past.",
                        "In the back office, the new main station is still in the box.")
                .choice("Swap in the new main station and pair the Dynaflexes, the iPads and the iPhones").icon("🖥").check("investigation", 10).chooser()
                    .pass("The new main station boots up first try, and every Dynaflex pairs right away! The manager signs off on the install and pays you 25,000 gold.", "am_meanwhile", gold(25000), xp(50), flag("clean_install"))
                    .fail("The power strip under the desk sparks while you plug in the new main station! It boots up on the second try. The manager pays you 15,000 gold.", "am_meanwhile", damageOne("4", "lightning"), gold(15000))
                .choice("Run a full diagnostic on Monroe's network first").icon("🩺")
                    .effect(shortRest(), inspiration(), gold(20000),
                            log("The diagnostic comes back clean. You swap in the new main station and the Dynaflexes pair up. You know this network cold now (Heroic Inspiration). The manager pays you 20,000 gold for the install."))
                    .go("am_meanwhile");

        c.scene("am_meanwhile").chapter(CH2).title("Meanwhile, at the Bar").backdrop("tavern").time("day").music("tension")
                .ambience("crowd")
                .npc("Chad", "The Toast Rep", "#e74c3c", "🍞", "Payam, my man. Is that ANOTHER broken SmartTab station?")
                .text("While you're in the back, the front door opens.",
                        "Chad, the Toast rep, walks in, grabs a seat at the bar, and orders a drink like he lives there. He sees the SmartTab van out front. Then he sees the old broken main station sitting on the bar.",
                        "\"Payam, my man. Is that ANOTHER broken SmartTab station? Toast hardware doesn't break like that. I'll get you six months free and beat whatever they're charging. Their support? Good luck. You'll be on hold till Christmas.\"",
                        "Payam just keeps pouring drinks and doesn't say anything.")
                .choice("Back in the office, you realize you need more gear…").icon("🚐").go("am_van");

        c.scene("am_van").chapter(CH3).title("The Van").backdrop("town").time("day").music("explore")
                .aside("Nobody knows how long that hot dog has been in the van. Nobody asks.")
                .text("You slip out the kitchen door to the SmartTab van and haul the back doors open. Everything a rep could need, and some things nobody needs, is in here somewhere.",
                        "(Gear you grab goes into your pack. Open your character sheet to **attune** to the iPad, POS Bank, Dynaflex or iPhones — up to three at once.)")
                .choice("Gas station hot dog — 2,000 gold (+10 Uptime)").icon("🌭").ifGold(2000)
                    .effect(gold(-2000), heal("10"), log("You eat a gas station hot dog that's been in the van since Tuesday."))
                    .go("am_van")
                .choice("Spare iPad — 3,000 gold (more Bandwidth)").icon("📲").once().ifGold(3000)
                    .effect(gold(-3000), item("am_spare_ipad"), log("You grab a spare iPad for the new main station."))
                    .go("am_van")
                .choice("POS Bank — 5,000 gold (more Max Uptime)").icon("🔋").once().ifGold(5000)
                    .effect(gold(-5000), item("am_pos_bank"), heal("10"), log("You grab a fresh POS bank to charge the iPhones and Dynaflexes."))
                    .go("am_van")
                .choice("Monster Energy — 1,500 gold").icon("🥤").ifGold(1500)
                    .effect(gold(-1500), item("am_monster_energy"), log("You shove a Monster in your pocket."))
                    .go("am_van")
                .choice("Spare Dynaflex — 6,000 gold (+1 attack, +2 damage)").icon("💳").once().ifGold(6000)
                    .effect(gold(-6000), item("am_spare_dynaflex"), log("You pair a spare Dynaflex to a backup iPhone and clip it to your belt."))
                    .go("am_van")
                .choice("Box of iPhones — 4,000 gold (+3 Signal)").icon("📦").once().ifGold(4000)
                    .effect(gold(-4000), item("am_box_of_iphones"), log("You grab a box of iPhones for the servers."))
                    .go("am_van")
                .choice("Close up the van and head back inside").icon("🚪")
                    .effect(log("You slam the van doors and head back in through the kitchen."))
                    .go("am_trash_talk");
    }

    // =========================================================================================== chapter 4
    static final String[] CHAD = {
            "\"How many main stations is that now, Payam? Three? SmartTab must sell them by the dozen.\"",
            "\"I'll undercut whatever you're paying. Toast will give Monroe six months free.\"",
            "\"SmartTab support? Hope you like hold music.\""
    };
    static final String[][] COMEBACKS = {
            {"\"At least our stations get replaced the SAME DAY. How's your backlog, Chad?\"",
             "\"You're sitting in a SmartTab bar, drinking a drink rung up on a SmartTab station.\"",
             "\"Bro, your shirt is tucked into your jeans.\""},
            {"\"Six months free, then 15 years locked in a contract. Read the fine print, Payam.\"",
             "\"Free? Monroe left Toast for a reason, Chad.\"",
             "\"You can't even afford your own drink. Payam, check his tab.\""},
            {"\"Our support picks up on the first ring. Toast support is the reason you're here begging.\"",
             "\"Funny, Toast is the one with the 45 minute hold time.\"",
             "\"Hold music? You ARE hold music.\""}
    };
    // each comeback lands differently: the facts (Persuasion), the needle (Insight), the roast (Performance)
    static final String[] STYLE = {"persuasion", "insight", "performance"};
    static final String[] STYLE_ICON = {"📊", "🎯", "🔥"};

    static void trashTalk(Campaign c) {
        c.scene("am_trash_talk").chapter(CH4).title("The Trash Talk").backdrop("tavern").time("dusk").music("tension")
                .ambience("crowd")
                .npc("Chad", "The Toast Rep", "#e74c3c", "🍞", "Oh look, it's the guy who keeps breaking Payam's stations.")
                .text("You walk out of the back office with your tool bag… and hear Chad talking trash about SmartTab to Payam.",
                        "Chad spins around on his bar stool. \"Oh look, it's the guy who keeps breaking Payam's stations.\"",
                        "It's on. **Three rounds.** Every comeback that lands rattles him before the fight starts.")
                .choice("Step up to the bar").icon("🍸").go(talkId(0, 0));

        // round r (0-2) with w comebacks landed so far — the scene graph keeps the score
        for (int r = 0; r < 3; r++) {
            for (int w = 0; w <= r; w++) {
                Campaign.Scene s = c.scene(talkId(r, w)).chapter(CH4).title("Round " + (r + 1) + (w > 0 ? " (" + w + " landed)" : "")).backdrop("tavern").time("dusk").music("tension")
                        .ambience("crowd")
                        .npc("Chad", "The Toast Rep", "#e74c3c", "🍞", CHAD[r].replace("\"", ""))
                        .text("**Chad:** " + CHAD[r], "Your comeback:");
                for (int k = 0; k < 3; k++) {
                    String next = r == 2 ? "am_talk_result_" + w : talkId(r + 1, w);
                    String nextWin = r == 2 ? "am_talk_result_" + (w + 1) : talkId(r + 1, w + 1);
                    s.choice(COMEBACKS[r][k]).icon(STYLE_ICON[k]).check(STYLE[k], 12).chooser()
                            .pass("**You:** " + COMEBACKS[r][k] + "\n\nThe whole bar goes \"OHHHH.\" Even Payam laughs. Chad turns red.", nextWin)
                            .fail("**You:** " + COMEBACKS[r][k] + "\n\nIt comes out wrong and nobody laughs. Chad smirks. Your pride takes 2 damage.", next, damageOne("2", "psychic"));
                }
            }
        }

        c.scene("am_talk_result_3").chapter(CH4).title("3–0!").backdrop("tavern").time("dusk").music("brawl")
                .onEnter(flag("chad_rattled"), flag("rank_legendary"), xp(50))
                .text("You won the trash talk 3-0! The whole bar is on its feet. Chad is so rattled he starts the fight already hurt. **(Chad starts weakened.)**")
                .choice("Here it comes…").icon("⚔").go("am_brawl_start");
        c.scene("am_talk_result_2").chapter(CH4).title("2–1").backdrop("tavern").time("dusk").music("brawl")
                .onEnter(flag("rank_senior"), xp(25))
                .text("Two out of three. The bar's on your side, and Chad knows it.")
                .choice("Here it comes…").icon("⚔").go("am_brawl_start");
        c.scene("am_talk_result_1").chapter(CH4).title("1–2").backdrop("tavern").time("dusk").music("brawl")
                .onEnter(flag("rank_rookie"))
                .text("One landed. Two didn't. Chad is grinning into his drink.")
                .choice("Here it comes…").icon("⚔").go("am_brawl_start");
        c.scene("am_talk_result_0").chapter(CH4).title("0–3").backdrop("tavern").time("dusk").music("brawl")
                .onEnter(flag("chad_confident"), flag("rank_rookie"))
                .text("Chad won the trash talk 3-0. He's feeling confident. **(Chad starts the fight stronger.)**")
                .choice("Here it comes…").icon("⚔").go("am_brawl_start");
    }

    static String talkId(int round, int wins) { return "am_talk_r" + (round + 1) + "_w" + wins; }

    // =========================================================================================== chapter 5 & the end
    static void fightAndAftermath(Campaign c) {
        c.scene("am_brawl_start").chapter(CH5).title("The Bar Fight").backdrop("tavern").time("dusk").music("brawl").checkpoint()
                .ambience("crowd")
                .text("Chad stands up, knocks your tool bag off the bar, and shoves you.",
                        "You shove him back. A bar stool goes over. Then everything starts flying.",
                        "(Your phone has SmartTab Support on speed dial — **one call per fight**. Monsters and fries heal you as a Bonus Action.)")
                .choice("Throw down!").icon("⚔").unlessFlag("chad_confident").effect(item("am_support_call")).battle("am_brawl")
                    .victory("", "am_aftermath", gold(15000), item("am_fries"), xp(50))
                    .defeat("", "am_game_over")
                .choice("Throw down!").icon("⚔").ifFlag("chad_confident").effect(item("am_support_call")).battle("am_brawl_confident")
                    .victory("", "am_aftermath", gold(15000), item("am_fries"), xp(50))
                    .defeat("", "am_game_over");

        c.scene("am_game_over").chapter(CH5).title("Game Over — Monroe Switches to Toast").backdrop("tavern").time("night").music("sorrow")
                .text("Chad lands the last hit and you go down on the bar mat.",
                        "Payam, holding a bag of ice to his head, sighs and hands Chad a Toast contract to leave on the bar.",
                        "**=== GAME OVER — Monroe switches to Toast ===**")
                .choice("Run it back (try the fight again)").icon("🔁").effect(shortRest()).go("am_brawl_start")
                .choice("Accept defeat and end the tale").icon("🏁").go("@end");

        c.scene("am_aftermath").chapter(END).title("The Aftermath").backdrop("tavern").time("night").music("triumph")
                .ambience("crowd")
                .npc("Payam", "Bartender at Monroe", "#1f618d", "🧊", "Good news. It was the old one. It was already broken.")
                .text("Chad limps out the front door, mumbling that he's \"emailing corporate.\"",
                        "Payam sits on the ice machine with a bag of ice on his head. The manager comes out of the back, looks at the mess, and asks who threw the main station.",
                        "Nobody says anything.",
                        "Payam finally speaks up: \"Good news. It was the old one. It was already broken.\" The manager thinks about it for a second, shrugs, and signs Monroe up for another year of SmartTab.",
                        "**=== YOU WIN — MONROE STAYS SMARTTAB ===**")
                .choice("RANK: Legendary Rep").icon("🏆").once().ifFlag("rank_legendary")
                    .effect(log("Payam names a shot after you. It's called \"The Main Station.\""))
                    .go("am_aftermath")
                .choice("RANK: Senior Rep").icon("🥈").once().ifFlag("rank_senior")
                    .effect(log("Payam forgives you. Mostly."))
                    .go("am_aftermath")
                .choice("RANK: Rookie Rep").icon("🥉").once().ifFlag("rank_rookie")
                    .effect(log("Monroe stays SmartTab, but Payam wants a helmet behind the bar."))
                    .go("am_aftermath")
                .choice("Clock out").icon("🏁").go("@end");
    }

    // =========================================================================================== the bar
    static void encounters(Campaign c) {
        String[] bar = {
                "######################",
                "#BBBBBBBBBBBBBBBB....#",
                "#....................#",
                "#..c......l..........#",
                "#.....BB.......BB....#",
                "#.....BB...E...BB....#",
                "#....................#",
                "#..BB......BB....BB..#",
                "#..BB......BB....BB..#",
                "#......PP....PP......#",
                "#......PP....PP......#",
                "######################"};
        for (String id : new String[]{"am_brawl", "am_brawl_confident"}) {
            boolean confident = id.endsWith("confident");
            Campaign.Encounter e = c.encounter(id).title("Bar Fight at Monroe").backdrop("tavern").music("brawl").nonlethal()
                    .objective("defeat:" + (confident ? "am_chad_confident" : "am_chad"))
                    .intro("Chad throws pint glasses, bar stools and limes, and tackles people into the beer cooler. Knock him out (nobody dies in a bar fight). Use your SmartTab Support call, chug a Monster when you need it — and watch out for the old main station.")
                    .phase(50, "In the chaos, Chad grabs the OLD broken main station you left on the bar. You grab the other end — a tug of war — and you both let go at the exact same time. It spins across the bar and… BONK! It hits Payam right in the head. The whole room goes silent. A second later Payam pops back up holding a bag of ice, points straight at Chad, and whips him with a wet bar rag!",
                            Json.obj("op", "say", "text", "Payam: \"YOU started this!\""))
                    .phase(33, "Chad is getting desperate!",
                            Json.obj("op", "say", "text", "Chad: \"I'm emailing CORPORATE about this!\""))
                    .map(bar)
                    .legend('c', confident ? "am_chad_confident" : "am_chad").legend('l', "am_lime_bowl").extra("am_toast_intern");
            if (!confident) e.weakenIf("chad_rattled");
        }
    }
}
