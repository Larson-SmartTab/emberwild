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
 * SMARTTAB MOUNTAIN QUEST — a story by Vijay, from the team's Java challenge (SmartTabMountainQuest.java), rebuilt for
 * Emberwild.
 * <pre>
 *  SmartTab HQ ........ onboarding with Lady Kellie, the Paper Jam Goblin, Payam's first call
 *  Part 1 ............. the MagTek Mines: Dre the Goblin, the lift cable, the hidden button, card-reader monsters,
 *                       a tampered reader, and Sir Declines-a-Lot
 *  Part 2 ............. the ExpiTrans Bridge: APPROVED and DECLINED planks, and the Chargeback Troll
 *  Part 3 ............. SmartTab Mountain: Cowboy Steve, Sage Larson and JR, Lord Latency, the Gear-Screen Revenant,
 *                       and the white pill
 *  The Summit ......... the Toast King, Squire Square, MAIN STATION OFFLINE, Payam drives up a mountain with no road
 *  Endings ............ BACK ONLINE, QUICK CHAT, or CHURNED
 * </pre>
 * The original's boss tricks become choices you make before (and phases during) each fight: the 10-beep reset weakens
 * Sir Declines-a-Lot, a ping test weakens Lord Latency, the hidden button sends the Revenant home without a fight, Squire
 * Square can switch sides, and the cancellation clause cracks the Toast King's armor. Tips are gold. Custom ids are
 * prefixed {@code mq_}.
 */
public final class MountainQuest implements CampaignScript {

    static final String HQ = "SmartTab HQ";
    static final String P1 = "Part 1 — The MagTek Mines";
    static final String P2 = "Part 2 — The ExpiTrans Bridge";
    static final String P3 = "Part 3 — SmartTab Mountain";
    static final String TOP = "The Summit";
    static final String END = "Credits";

    @Override
    public Campaign build() {
        Campaign c = Campaign.create("smarttab_mountain_quest", "SmartTab Mountain Quest, a story by Vijay", "Vijay");
        c.cover("snow");
        c.synopsis("One mountain. One Main Station that won't come back online. Up to four new support agents. Every station in "
                + "the valley is stuck on the gear screen, bars can't close tabs, and somewhere a bartender is taking orders on a "
                + "napkin. From onboarding with Lady Kellie, through the MagTek Mines and over the ExpiTrans Bridge, up to a "
                + "summit where a giant chrome toaster wears a crown — and Payam will not stop calling. A story by Vijay for "
                + "1-4 players from level 1 (rising to level 4); fellow new hires fill a small crew.");

        pregens(c);
        monsters(c);
        items(c);
        hq(c);
        mines(c);
        bridge(c);
        mountain(c);
        summit(c);
        encounters(c);
        return c;
    }

    // =========================================================================================== the new hires
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
        // The four roles from the original. (Create your own hero in the lobby to use your own name.)
        c.pregen(hero("mq_richard_parker", Json.obj(
                "name", "Richard Parker", "pronouns", "he/him", "class", "bard", "species", "human", "background", "entertainer",
                "method", "pointbuy", "scores", scores(8, 14, 13, 10, 12, 15), "increases", Json.obj("cha", 2, "dex", 1),
                "skills", List.of("persuasion", "insight", "deception"),
                "color", "#2e86c1", "emblem", "🎧", "hair", "#4e342e", "skin", "#e0ac69", "eyes", "#5d4037",
                "appearance", "Support Agent. Brand-new headset, brand-new lanyard, has never seen Life of Pi.",
                "personality", "Can say \"I completely understand your frustration\" 400 times a day and mean it every time.",
                "motto", "I completely understand your frustration.")));
        c.pregen(hero("mq_field_tech", Json.obj(
                "name", "Jordan the Field Tech", "pronouns", "they/them", "class", "fighter", "species", "human", "background", "soldier",
                "method", "pointbuy", "scores", scores(15, 13, 14, 8, 12, 10), "increases", Json.obj("str", 2, "con", 1),
                "skills", List.of("athletics", "perception"),
                "color", "#d35400", "emblem", "🔦", "hair", "#212121", "skin", "#c68642", "eyes", "#3e2723",
                "appearance", "Field Tech. Flashlight, knee pads, and a signed waiver. The toughest new hire.",
                "personality", "Has a special move called Power Cycle Slam and is waiting for any excuse to use it.",
                "motto", "Hold the switch down. Count to thirty.")));
        c.pregen(hero("mq_bartender", Json.obj(
                "name", "Sam the Bartender", "pronouns", "she/her", "class", "cleric", "species", "human", "background", "acolyte",
                "method", "pointbuy", "scores", scores(12, 12, 14, 8, 15, 11), "increases", Json.obj("wis", 2, "cha", 1),
                "skills", List.of("medicine", "insight"),
                "cantrips", List.of("sacred_flame", "guidance", "toll_the_dead"),
                "spells", List.of("healing_word", "cure_wounds", "bless", "guiding_bolt"),
                "color", "#16a085", "emblem", "🍸", "hair", "#7b3f00", "skin", "#f1c27d", "eyes", "#1b5e20",
                "appearance", "Bartender. Hired because \"Fridays have been rough.\" Pours a Double that could wake the dead.",
                "personality", "Keeps everyone on their feet and the tips coming in.",
                "motto", "Make it a double.")));
        c.pregen(hero("mq_network_wizard", Json.obj(
                "name", "Alex the Network Wizard", "pronouns", "he/him", "class", "wizard", "species", "human", "background", "sage",
                "method", "pointbuy", "scores", scores(8, 14, 14, 15, 12, 8), "increases", Json.obj("int", 2, "con", 1),
                "skills", List.of("arcana", "investigation"),
                "cantrips", List.of("fire_bolt", "shocking_grasp", "ray_of_frost"),
                "spells", List.of("magic_missile", "shield", "mage_armor", "charm_person"),
                "color", "#8e44ad", "emblem", "🪄", "hair", "#5d4037", "skin", "#f5cba7", "eyes", "#1565c0",
                "appearance", "Network Wizard. His wand is an ethernet cable. Lady Kellie said that was close enough.",
                "personality", "Fragile, brilliant, and convinced every problem is DNS.",
                "motto", "Ping first. Ask questions later.")));

        // ---- the rest of the new-hire class, who tag along when the crew is small
        Hero casey = hero("mq_casey", Json.obj(
                "name", "Casey (New Hire, Bartender)", "pronouns", "she/her", "class", "cleric", "species", "human", "background", "acolyte",
                "method", "pointbuy", "scores", scores(12, 12, 14, 8, 15, 11), "increases", Json.obj("wis", 2, "cha", 1),
                "skills", List.of("medicine", "persuasion"),
                "cantrips", List.of("sacred_flame", "guidance", "toll_the_dead"),
                "spells", List.of("healing_word", "cure_wounds", "bless", "guiding_bolt"),
                "color", "#f39c12", "emblem", "🍹", "hair", "#000000", "skin", "#a0522d", "eyes", "#3e2723",
                "appearance", "Another new hire. Brought her own shaker.",
                "personality", "Pours doubles for anyone who goes down.",
                "motto", "Hydrate. Then fight."));
        casey.npc = true;
        c.pregen(casey);
        Hero morgan = hero("mq_morgan", Json.obj(
                "name", "Morgan (New Hire, Field Tech)", "pronouns", "he/him", "class", "fighter", "species", "dwarf", "background", "soldier",
                "method", "pointbuy", "scores", scores(15, 13, 14, 8, 12, 10), "increases", Json.obj("str", 2, "con", 1),
                "skills", List.of("athletics", "survival"),
                "color", "#7f8c8d", "emblem", "🧰", "hair", "#8d6e63", "skin", "#d7a77f", "eyes", "#4e342e",
                "appearance", "Another new hire. Already has a zip tie behind each ear.",
                "personality", "Got stuck behind a beer cooler once. Doesn't talk about it.",
                "motto", "Leave some slack."));
        morgan.npc = true;
        c.pregen(morgan);
    }

    // =========================================================================================== monsters
    static void monsters(Campaign c) {
        c.monster("mq_paper_jam_goblin", "The Paper Jam Goblin").type("humanoid").role("skirmisher").ac(11).hp(20).stats(8, 14, 10, 6, 8, 6)
                .cr(0.25, 50).attack("Crumpled Receipt", 3, "1d4+1", "slashing").attack("Ankle Bite", 3, "1d4", "piercing")
                .look("#f7f9f9", "🧾").blurb("A little gremlin made of crumpled receipts. Tastes like thermal paper.");
        c.monster("mq_poltergeist", "The Please Remove Card Poltergeist").type("undead").role("caster").ac(12).hp(26).fly(30).stats(4, 14, 12, 6, 10, 12)
                .cr(0.5, 100).resist("necrotic").immune("poison").condImmune("poisoned", "prone")
                .attack(new Monsters.Attack("\"PLEASE REMOVE CARD\"", 4, "1d8+2", "psychic").ranged(30, 30).spell())
                .special(Monsters.special("remove_card", "PLEASE. REMOVE. CARD.").shape("cone", 15, 0).dmg("2d4", "thunder").dc(11, Ability.CON).recharge(5))
                .look("#d6eaf8", "👻").blurb("A ghost from an old card reader. It only knows one sentence. You don't have a card in.");
        c.monster("mq_three_headed_reader", "The Three-Headed Reader").type("construct").role("brute").ac(12).hp(34).stats(14, 10, 14, 3, 8, 3)
                .cr(1, 200).multi(2).immune("poison", "psychic").condImmune("charmed", "frightened", "poisoned")
                .attack(new Monsters.Attack("INSERT!", 4, "1d6+2", "bludgeoning"))
                .attack(new Monsters.Attack("SWIPE!", 4, "1d6+2", "slashing"))
                .attack(new Monsters.Attack("TAP. TAP. TAP.", 4, "1d6+2", "bludgeoning"))
                .look("#5d6d7e", "💳").blurb("One head screams INSERT, one screams SWIPE, one screams TAP. Whatever you do, two of them are mad.");
        c.monster("mq_mimic", "The Mimic").type("monstrosity").role("brute").ac(12).hp(42).speed(15).stats(16, 10, 14, 5, 10, 6)
                .cr(1, 200).attack(new Monsters.Attack("Packing-Tape Bite", 5, "1d8+3", "piercing").rider("grappled", Ability.STR, 12))
                .look("#c8a165", "📦").blurb("A brand-new card reader, still sealed. The box just says FREE. It has teeth.");
        c.monster("mq_sir", "Sir Declines-a-Lot").type("construct").role("boss").ac(15).hp(62).stats(16, 10, 15, 6, 10, 10)
                .cr(2, 450).multi(2).immune("poison", "psychic").condImmune("charmed", "frightened", "poisoned")
                .attack(new Monsters.Attack("\"Your Health Has Been DECLINED\"", 5, "1d8+3", "slashing"))
                .attack(new Monsters.Attack("Shield of Expired Cards", 5, "1d6+3", "bludgeoning").rider("prone", Ability.STR, 12))
                .special(Monsters.special("declined", "DECLINED!").shape("sphere", 10, 0).dmg("2d6", "psychic").dc(12, Ability.WIS).rider("frightened").recharge(5))
                .trait("legendary_resistance").legendary(1, "attack:Shield of Expired Cards", "move")
                .look("#922b21", "🛡").blurb("A knight built out of old card readers. His visor reads DECLINED.");
        c.monster("mq_troll", "The Chargeback Troll").type("giant").role("boss").size(2).ac(13).hp(66).stats(18, 9, 16, 7, 9, 8)
                .cr(3, 700).multi(2)
                .attack(new Monsters.Attack("40-Page Dispute Letter", 6, "1d8+4", "bludgeoning"))
                .attack(new Monsters.Attack("\"I'm Disputing That Foot\"", 6, "1d6+4", "bludgeoning").rider("prone", Ability.STR, 13))
                .trait("fed_by_objects")
                .look("#1e8449", "🧌").blurb("A huge troll in a green visor, buried up to his neck in paperwork. Every open dispute heals him.");
        c.monster("mq_dispute", "Open Dispute").type("object").role("object").ac(10).hp(7).speed(0).stats(1, 1, 10, 1, 1, 1)
                .cr(0, 25).immune("poison", "psychic").vulnerable("fire").condImmune("charmed", "frightened", "poisoned", "paralyzed", "prone", "incapacitated")
                .attack("None", 0, "0", "bludgeoning").look("#f9e79f", "📑").blurb("A stack of chargeback paperwork. While it's open, the troll keeps getting his money back. Resolve it (hit it)!");
        c.monster("mq_latency", "Lord Latency").type("undead").role("boss").ac(15).hp(105).stats(12, 14, 14, 16, 12, 12)
                .cr(3, 700).multi(2).resist("cold", "necrotic").immune("poison").condImmune("poisoned", "exhaustion")
                .attack(new Monsters.Attack("Staff (Arrives Late)", 6, "1d10+3", "bludgeoning"))
                .attack(new Monsters.Attack("Delayed Point", 6, "2d6+2", "psychic").ranged(60, 60).spell())
                .special(Monsters.special("lag_spike", "LAG SPIKE!").shape("sphere", 5, 60).dmg("2d8+3", "force").dc(13, Ability.DEX).recharge(5))
                .trait("legendary_resistance").legendary(1, "attack:Delayed Point", "move")
                .look("#5b2c6f", "💀").blurb("A skeleton in a robe of tangled patch cables. Everything he does arrives four seconds late. Runs build 3.10.");
        c.monster("mq_latency_fast", "Lord Latency (All 12 Gigs)").type("undead").role("boss").ac(15).hp(105).stats(12, 16, 14, 16, 12, 12)
                .cr(3, 700).multi(2).resist("cold", "necrotic").immune("poison").condImmune("poisoned", "exhaustion")
                .attack(new Monsters.Attack("Staff (Right On Time)", 8, "1d10+4", "bludgeoning"))
                .attack(new Monsters.Attack("Instant Point", 8, "2d6+3", "psychic").ranged(60, 60).spell())
                .special(Monsters.special("lag_spike", "NO LAG AT ALL!").shape("sphere", 5, 60).dmg("2d8+3", "force").dc(13, Ability.DEX).recharge(5))
                .trait("legendary_resistance").legendary(1, "attack:Instant Point", "move")
                .look("#5b2c6f", "💀").blurb("You reseated his RAM. Now all 12 gigs are registering, his words show up on time, and somehow that's worse.");
        c.monster("mq_packet", "Dropped Packet").type("construct").role("skirmisher").ac(13).hp(16).speed(40).stats(8, 16, 10, 3, 8, 3)
                .cr(0.5, 100).immune("poison", "psychic").attack("Request Timed Out", 5, "1d8+3", "force")
                .look("#a569bd", "📦").blurb("It never came back. Now it's back, and it's angry about the wait.");
        c.monster("mq_revenant", "The Gear-Screen Revenant").type("construct").role("brute").ac(14).hp(88).stats(17, 8, 16, 3, 8, 3)
                .cr(2, 450).multi(2).immune("poison", "psychic").condImmune("charmed", "frightened", "poisoned")
                .attack(new Monsters.Attack("Loading Ram", 6, "1d10+3", "bludgeoning"))
                .special(Monsters.special("spinning_gear", "The Gear Spins Faster").shape("sphere", 10, 0).dmg("2d8", "bludgeoning").dc(13, Ability.DEX).rider("prone").recharge(5))
                .look("#85929e", "⚙").blurb("A station stuck on the gear screen for three days. It's not angry. It's just loading.");
        c.monster("mq_toast_king", "The Toast King").type("construct").role("boss").size(2).ac(15).hp(130).stats(18, 10, 18, 12, 12, 16)
                .cr(5, 1800).multi(2).resist("fire").immune("poison").condImmune("charmed", "frightened", "poisoned")
                .attack(new Monsters.Attack("Two Slices, Still Hot", 7, "1d10+4", "fire"))
                .attack(new Monsters.Attack("Reads You His Contract", 6, "2d6+2", "psychic").ranged(60, 60).spell())
                .special(Monsters.special("burnt_toast", "POP! Burnt Toast").shape("cone", 15, 0).dmg("3d8", "fire").dc(14, Ability.DEX).recharge(5))
                .special(Monsters.special("auto_renewal", "The Auto-Renewal Clause").shape("sphere", 10, 0).dmg("2d6", "psychic").dc(13, Ability.WIS).rider("slowed"))
                .trait("legendary_resistance").legendary(2, "special:auto_renewal", "attack:Two Slices, Still Hot", "move")
                .look("#d5d8dc", "👑").blurb("A giant chrome toaster wearing a crown, a sales-rep lanyard hanging off his lever. He's set to dark.");
        c.monster("mq_toast_crumb", "Flying Crouton").type("construct").role("skirmisher").ac(12).hp(9).fly(40).stats(6, 14, 10, 1, 5, 1)
                .cr(0.125, 25).resist("fire").attack("Crunch", 4, "1d6+1", "fire").look("#d68910", "🍞").blurb("The Toast King's spare slices. Still warm.");
        // allies you can earn
        c.monster("mq_jr", "JR the Apprentice").type("humanoid").role("skirmisher").ac(13).hp(22).stats(10, 14, 12, 14, 12, 10)
                .cr(0.5, 0).attack("Patch Cable Smack", 4, "1d4+3", "bludgeoning").look("#27ae60", "🔌")
                .blurb("Sage Larson's son. Has been ready for months. (Weeks.)");
        c.monster("mq_square", "Squire Square").type("humanoid").role("ranged").ac(13).hp(26).stats(8, 14, 12, 12, 10, 14)
                .cr(1, 0).attack(new Monsters.Attack("Card Reader Swipe", 6, "1d10+4", "slashing").ranged(30, 30))
                .look("#212f3d", "◼").blurb("Switched sides. Goes through on the first try.");
        c.monster("mq_dre", "Dre the Goblin").type("humanoid").role("skirmisher").ac(14).hp(24).stats(10, 16, 12, 12, 12, 10)
                .cr(0.5, 0).attack("Headset Cord Whip", 5, "1d6+3", "slashing").look("#1abc9c", "🎧")
                .blurb("Ran up the entire mountain. Somebody said wrong main IP.");
    }

    static void items(Campaign c) {
        c.item("mq_energy_drink", "Energy Drink", "potion", "A big heal: restores 2d8+8 hit points. (Includes the Red Bull tax.) Bonus Action.",
                Json.obj("use", "2d8+8", "value", 25));
        c.item("mq_hold_music", "Hold Music", "bomb", "Nobody at SmartTab actually puts anyone on hold, so nothing has ever heard this song. Ranged (20 ft): 2d6 psychic as the target stands there tapping its foot. (Cowboy Steve says it doesn't work on Payam. It makes him stronger.)",
                Json.obj("use", "2d6", "damageType", "psychic", "value", 15));
        c.item("mq_zip_ties", "Bag of Zip Ties", "wondrous", "They won't fix it, but they'll keep it from comin' loose. +1 Armor Class (attune from your sheet).",
                Json.obj("bonus", Json.obj("ac", 1), "attunable", true, "value", 15));
    }

    // =========================================================================================== HQ
    static void hq(Campaign c) {
        c.scene("mq_start").chapter(HQ).title("Your First Day").backdrop("hall").time("day").music("explore").checkpoint()
                .onEnter(companion("mq_casey", 3), companion("mq_morgan", 3))
                .aside("Every great quest starts with paperwork. This one starts with forty pages of it.")
                .npc("Lady Kellie", "Keeper of the Paperwork", "#7d3c98", "📋", "Name?")
                .text("Your first day. The new hires are lined up across from LADY KELLIE, Keeper of the Paperwork.",
                        "\"Sign here, here, and here. Initial there. This one's the onboarding packet. All 40 pages. Nobody reads it.\"")
                .choice("\"Richard Parker.\"").icon("🐯").once()
                    .effect(log("Lady Kellie: \"Like the tiger from Life of Pi?\" — \"Never seen it.\" — (long pause)"))
                    .go("mq_start")
                .choice("The Support Agent steps up").icon("🎧").once().ifClass("bard")
                    .effect(log("Lady Kellie: \"Support Agent. Say 'I completely understand your frustration.'\" — \"I completely understand your frustration.\" — \"Perfect. You'll be saying that about 400 times a day.\""))
                    .go("mq_start")
                .choice("The Field Tech steps up").icon("🔦").once().ifClass("fighter")
                    .effect(log("Lady Kellie: \"Field Tech. Here's a flashlight, knee pads, and a waiver. Sign that one first. The last Field Tech got stuck behind a beer cooler for three hours.\""))
                    .go("mq_start")
                .choice("The Bartender steps up").icon("🍸").once().ifClass("cleric")
                    .effect(log("Lady Kellie: \"Bartender? We're a tech company.\" (She thinks about it.) \"Actually, Fridays have been rough. You're hired.\""))
                    .go("mq_start")
                .choice("The Network Wizard steps up").icon("🪄").once().ifClass("wizard")
                    .effect(log("Lady Kellie: \"Network Wizard. Do you have a wand?\" (An ethernet cable is held up.) \"Close enough.\""))
                    .go("mq_start")
                .choice("Actually read the onboarding packet").icon("📖").once().check("investigation", 15).chooser()
                    .pass("You read the whole thing. Lady Kellie has never seen anyone do this. She makes a note in your file.", "mq_start", flag("packet_read"), xp(25))
                    .fail("You skim it, like everyone else.", "mq_start")
                .choice("Sign everything and head to the conference room").icon("✍").effect(gold(40), item("mq_energy_drink"), item("mq_energy_drink"), item("mq_hold_music"), item("mq_zip_ties")).go("mq_conference");

        c.scene("mq_conference").chapter(HQ).title("The Big Conference Room").backdrop("hall").time("day").music("tension")
                .npc("Baron Daniel Baron", "(Yes, the Baron is named Baron)", "#1a5276", "🎩", "The Main Station on top of SmartTab Mountain has gone dark.")
                .text("Lady Kellie: \"Congratulations, you're all officially employed. Your first emergency starts in five minutes.\"",
                        "Five minutes later, everyone is in the big conference room. BARON DANIEL BARON stands at the front. (Yes, the Baron is named Baron. Nobody asks.)",
                        "\"The Main Station on top of SmartTab Mountain has gone dark. Every station in the valley is stuck on the gear screen. Bars can't close tabs. Kitchens can't print tickets. Somewhere out there, a bartender is taking orders on a napkin.\"",
                        "CAPTAIN JD slides a headset across the table to each of you. \"Everybody else is on the phones. You're all new, none of you have a queue yet, and you've all got working legs. Congratulations, you're going up the mountain.\"")
                .choice("Grab your headset and head out").icon("🎧").go("mq_printer");

        c.scene("mq_printer").chapter(HQ).title("The Office Printer").backdrop("hall").time("day").music("tension")
                .text("On the way out, the office printer starts making a noise. A very bad noise.",
                        "*** THE PAPER JAM GOBLIN *** — A little gremlin made of crumpled receipts crawls out of the paper tray.")
                .choice("Deal with it").icon("🧾").battle("mq_printer")
                    .victory("The goblin flees into the paper tray. The printer spits out one ticket that just says \"sorry.\"", "mq_first_fix", gold(5))
                    .defeat("The goblin wins this round, then gets bored and wanders back into the paper tray. The printer spits out one ticket that just says \"sorry.\"", "mq_first_fix");

        c.scene("mq_first_fix").chapter(HQ).title("First Fix of the Day").backdrop("hall").time("day").music("hope")
                .onEnter(longRest())
                .text("The orange light on the printer is blinking. Someone presses Feed. It clears. First fix of the day.",
                        "Then a desk phone rings. Nobody has even sat down yet.")
                .choice("Answer it").icon("☎").go("mq_payam1");

        c.scene("mq_payam1").chapter(HQ).title("Payam, Monroe").backdrop("hall").time("day").music("tension")
                .npc("Payam", "Monroe", "#c0392b", "☎", "My station's been down four minutes. FOUR.")
                .text("\"This is Payam. Monroe. My station's been down four minutes. FOUR. Do you know how much money I lose in four minutes?\"")
                .choice("\"I'm sorry, we're working on it right now.\"").icon("🙏").check("save:wis", 10).chooser()
                    .pass("Payam: \"Work faster.\" Click. You stay calm the whole time. He doesn't say thank you. Nobody expected him to. (+10 tips)", "mq_mines", gold(10))
                    .fail("Payam: \"Work faster.\" Click. You let out a tiny sigh. He heard it. Payam calls back: \"Did you just sigh at me? Fuck you.\" Click.", "mq_mines", damageOne("2", "psychic"))
                .choice("\"Have you tried turning it off and on?\"").icon("🔌")
                    .effect(log("Payam: \"That's YOUR job.\" He hangs up. You feel ten years older."), damageOne("1d4", "psychic"))
                    .go("mq_mines")
                .choice("\"Can I put you on a brief hold?\"").icon("⏸")
                    .effect(log("You've never put anyone on hold in your life. You picked a bad time to start. Payam: \"Do NOT put me on hold. Fuck you.\" Click. You feel ten years older."), damageOne("1d4", "psychic"))
                    .go("mq_mines");
    }

    // =========================================================================================== Part 1
    static void mines(Campaign c) {
        c.scene("mq_mines").chapter(P1).title("The MagTek Mines").backdrop("mine").time("day").music("dungeon").checkpoint()
                .npc("Dre the Goblin", "Guide", "#1abc9c", "🎧", "Stay close, and don't touch anything that's blinking.")
                .text("At the foot of SmartTab Mountain, the road turns into a mine. Beeps echo out of the dark. Two beeps. Four beeps. Ten beeps.",
                        "A goblin in a headset is waiting at the entrance. This is DRE.")
                .choice("Ask Dre about the goblin in the office printer").icon("🧾").once()
                    .effect(log("Dre: \"That was my cousin Gary. We don't talk about Gary.\""))
                    .go("mq_mines")
                .choice("Keep walking").icon("🚶").go("mq_lift");

        c.scene("mq_lift").chapter(P1).title("The Lift").backdrop("mine").time("day").music("mystery")
                .npc("Dre the Goblin", "Guide", "#1abc9c", "🎧", "Somebody set this cable up way too tight.")
                .text("The lift down into the mines keeps flipping between WE'LL BE BACK MOMENTARILY and working.",
                        "Dre: \"Somebody set this cable up way too tight. Every time the lift moves, it yanks itself out.\" He hands you the cable.")
                .choice("Reboot the lift").icon("🔄")
                    .effect(log("It reboots. It works for ten seconds. Then WE'LL BE BACK MOMENTARILY. Dre: \"Rebooting doesn't fix a cable.\""))
                    .go("mq_lift")
                .choice("Kick it").icon("🦶")
                    .effect(log("Your foot hurts. The lift doesn't care."), damageOne("1", "bludgeoning"))
                    .go("mq_lift")
                .choice("Reseat the cable and leave some slack").icon("🔌").check("sleight_of_hand", 10).chooser()
                    .pass("You reseat it and leave some slack. The lift hums to life and stays on. Dre nods. That's the only compliment anyone's getting today.", "mq_tunnel", xp(25))
                    .fail("You reseat it, but pull it tight again out of habit. It pops right back out. Try again.", "mq_lift");

        c.scene("mq_tunnel").chapter(P1).title("The Hidden Button").backdrop("mine").time("day").music("mystery")
                .onEnter(flag("hidden_button"))
                .text("Halfway down the tunnel, a station in the wall is stuck on the gear screen.",
                        "Dre: \"Watch. Top right corner, there's a hidden button.\" He taps it. A banner pops up with the IP. It's pointed at the wrong main. Dre fixes it in about four seconds.",
                        "\"Remember that. Hidden button, top right. You're gonna need it.\" Everyone writes it on their hand.",
                        "Something drifts out of an old card reader further down the tunnel…")
                .choice("Face it").icon("👻").battle("mq_poltergeist")
                    .victory("The Poltergeist finally gets its card removed and fades away.", "mq_payam2", gold(10), xp(50));

        c.scene("mq_payam2").chapter(P1).title("Payam, Again").backdrop("mine").time("day").music("tension")
                .npc("Payam", "Monroe", "#c0392b", "☎", "Why is it so loud on your end?")
                .text("Your phone rings. The caller ID says MONROE. Dre sees it and backs away slowly.",
                        "\"Why is it so loud on your end?\" — \"I'm in a mine.\" — \"I don't care where you are. My cocktail server can't clock out. It says Access Denied. She wants to go home. I want her to go home. I'm not paying her to stand there.\"")
                .choice("Reboot the station").icon("🔄")
                    .effect(log("Payam: \"I rebooted it six times. It reboots fine. She still can't clock out.\""), damageOne("2", "psychic"))
                    .go("mq_payam2")
                .choice("Turn off \"fingerprint required\" so she can clock out with her code, then turn it back on and re-enroll her").icon("☝").go("mq_payam2_fp")
                .choice("Tell her to stay").icon("✋")
                    .effect(log("Payam: \"Did you just tell MY employee to stay? I'M the one who tells people to stay.\""), damageOne("2", "psychic"))
                    .go("mq_payam2");

        c.scene("mq_payam2_fp").chapter(P1).title("Clocked Out").backdrop("mine").time("day").music("mystery")
                .text("She clocks out with her code. Now, don't forget the second half…")
                .choice("Turn fingerprints back on and re-enroll her").icon("☝").check("investigation", 12).chooser()
                    .pass("You turn \"fingerprint required\" back on and re-enroll her. Payam hangs up without a word. That's as close as he gets to thank you. (+15 tips)", "mq_reader", gold(15))
                    .fail("You forget to turn it back on. An hour later, Payam calls back: \"Why is someone named GARY clocked in at my bar?\" You turn it back on. Somewhere in the mine, Dre gets very quiet. (+5 tips)", "mq_reader", damageOne("2", "psychic"), gold(5));

        c.scene("mq_reader").chapter(P1).title("The Three-Headed Reader").backdrop("mine").time("day").music("tension")
                .text("Deeper in, a card reader with three heads blocks the tunnel. One screams INSERT, one screams SWIPE, one screams TAP. Whatever you do, two of them are mad about it.")
                .choice("Fight it").icon("💳").battle("mq_reader")
                    .victory("All three heads go quiet at once. For the first time, they agree on something.", "mq_free_box", gold(10), xp(50));

        c.scene("mq_free_box").chapter(P1).title("FREE").backdrop("mine").time("day").music("mystery")
                .text("A brand-new card reader, still sealed in the box, sits in the middle of the tunnel. The box just says FREE.")
                .choice("Reach for it").icon("📦").battle("mq_mimic")
                    .victory("The Mimic stops chewing. Inside: a few tips somebody else reached for first.", "mq_tamper", gold(15), xp(50))
                .choice("Look a little closer first").icon("🔍").once().check("perception", 12).chooser()
                    .pass("It has teeth. It has packing tape on the teeth. You leave it alone, and it sulks.", "mq_tamper", xp(25))
                    .fail("It looks like a perfectly good free card reader.", "mq_free_box")
                .choice("Nothing is free. Keep walking.").icon("🚶").go("mq_tamper");

        c.scene("mq_tamper").chapter(P1).title("Four Lights").backdrop("mine").time("day").music("sorrow")
                .npc("Dre the Goblin", "Guide", "#1abc9c", "🎧", "Four lights. That's tamper. It's done.")
                .text("In the dark, a card reader sits alone. Four lights blinking, over and over.")
                .choice("Do the 10-beep reset").icon("🔟")
                    .effect(log("You hold the button and count. Eight... nine... ten. The four lights keep blinking. It didn't even notice. Dre: \"Four lights means tamper. Tamper means it's done. Don't be a hero.\""))
                    .go("mq_tamper")
                .choice("Give it a smack").icon("✋")
                    .effect(log("That felt good. It didn't fix anything."))
                    .go("mq_tamper")
                .choice("Box it up and send for a replacement").icon("📦")
                    .effect(log("It's the right call. It's also the saddest box anyone has ever taped. Dre: \"New one's on the way. When it shows up, unplug the old one, plug in the new one, and do a full reboot on the station. And Velcro it so it sticks up.\""))
                    .go("mq_sir_gate");

        c.scene("mq_sir_gate").chapter(P1).title("Sir Declines-a-Lot").backdrop("mine").time("day").music("boss").checkpoint()
                .npc("Sir Declines-a-Lot", "Guardian of the Mine Exit", "#922b21", "🛡", "Halt! I decline your request to pass.")
                .text("At the bottom of the mine, something enormous is standing in front of the exit. It clanks.",
                        "\"Halt! I decline your request to pass.\" — \"I didn't request anything.\" — \"Declined.\"")
                .choice("Look at his lights").icon("💡").once().check("investigation", 12).chooser()
                    .pass("Two lights, not four. Dre, from behind a rock: \"He's not in tamper, he's just confused. Hold the button!\"", "mq_sir_gate", flag("sir_confused"))
                    .fail("Lots of lights. Blinking. You're not sure what they mean.", "mq_sir_gate")
                .choice("Do the 10-beep reset on him").icon("🔟").ifFlag("sir_confused").check("save:wis", 10).chooser()
                    .pass("...eight, nine, ten. Sir Declines-a-Lot freezes. Reboots. When he comes back up, his armor is gone and he's forgotten how to decline. (He starts the fight weakened.)", "mq_sir_fight", flag("sir_reset"))
                    .fail("You lose count at seven. The reset didn't take. He raises his shield. DECLINED.", "mq_sir_fight")
                .choice("Fight").icon("⚔").go("mq_sir_fight");

        c.scene("mq_sir_fight").chapter(P1).title("DECLINED").backdrop("mine").time("day").music("boss")
                .text("Sir Declines-a-Lot draws a sword made of expired cards.")
                .choice("Fight Sir Declines-a-Lot").icon("⚔").battle("mq_sir")
                    .victory("Sir Declines-a-Lot drops to one knee. His visor flickers one last time: APPROVED.", "mq_mines_end", gold(40), xp(100));

        c.scene("mq_mines_end").chapter(P1).title("Out of the Mines").backdrop("road").time("day").music("hope")
                .onEnter(level(2), longRest(), item("mq_energy_drink"))
                .npc("Dre the Goblin", "Guide", "#1abc9c", "🎧", "If you ever see the white pill flashing white, leave it alone.")
                .text("Dre: \"I've got a queue. One more thing. If you ever see the white pill flashing white, leave it alone. It's still booting. Wait for blue.\" Dre heads back toward HQ.",
                        "**LEVEL UP!** Everyone is now level 2.",
                        "Captain JD (on Slack): \"nice work. keep going.\" Buttram had Joel send an Energy Drink up the mountain with a note: \"Payam called again.\"")
                .choice("On to the bridge").icon("🌉").go("mq_bridge");
    }

    // =========================================================================================== Part 2
    static void bridge(Campaign c) {
        c.scene("mq_bridge").chapter(P2).title("The ExpiTrans Bridge").backdrop("bridge").time("day").music("tension").checkpoint()
                .text("The only way up the mountain crosses a canyon. The bridge is old. Half the planks say APPROVED. Half say DECLINED.",
                        "Everybody has to cross, so everybody rolls.")
                .choice("Only step on APPROVED planks").icon("✅").check("save:dex", 12).each()
                    .pass("You step only on APPROVED planks and make it across without a wobble.", "mq_payam3")
                    .fail("You step on a DECLINED plank. Insufficient support. You drop through, land on a ledge, and climb back up sticking to the APPROVED ones this time.", "mq_payam3", damageOne("1d6", "bludgeoning"));

        c.scene("mq_payam3").chapter(P2).title("Your Manager").backdrop("bridge").time("day").music("tension")
                .npc("Payam", "Monroe", "#c0392b", "☎", "I want to talk to your manager.")
                .text("As everyone steps off the bridge, a phone rings. MONROE.",
                        "\"I want to talk to your manager.\" — \"Captain JD is...\" — \"I want to talk to HIS manager.\" — \"That'd be Baron Daniel Baron.\" — \"Then put the Baron on.\"",
                        "Somewhere, Baron Daniel Baron's phone starts ringing.")
                .choice("Keep your cool").icon("🧘").check("save:wis", 10).chooser()
                    .pass("You stay calm the whole time. He doesn't say thank you. Nobody expected him to. (+10 tips)", "mq_troll_gate", gold(10))
                    .fail("You let out a tiny sigh. He heard it. \"Did you just sigh at me? Fuck you.\" Click.", "mq_troll_gate", damageOne("2", "psychic"));

        c.scene("mq_troll_gate").chapter(P2).title("Under the Bridge").backdrop("bridge").time("dusk").music("boss")
                .npc("Chargeback Troll", "Under the Bridge", "#1e8449", "🧌", "WHO GOES THERE? Toll is 100 tips.")
                .text("On the far side, there's a little hut with a lantern in the window and a sign: EXPITRANS. And under the end of the bridge, something huge stirs.",
                        "\"WHO GOES THERE? Toll is 100 tips. Also, I'm disputing your last three transactions.\" — \"I haven't bought anything.\" — \"Then you won't mind.\"",
                        "(Every open dispute heals him. Somebody needs to resolve those disputes.)")
                .choice("Pay the toll").icon("💸").ifGold(100)
                    .effect(gold(-100), log("You pay the troll 100 tips. He immediately disputes the payment and attacks anyway."))
                    .battle("mq_troll")
                    .victory("The troll sinks back under the bridge, mumbling about a 45-day window. A business card flutters out of his visor. It just says TOAST.", "mq_bridge_end", gold(50), xp(150))
                .choice("Fight the troll").icon("⚔").battle("mq_troll")
                    .victory("The troll sinks back under the bridge, mumbling about a 45-day window. A business card flutters out of his visor. It just says TOAST.", "mq_bridge_end", gold(50), xp(150));

        c.scene("mq_bridge_end").chapter(P2).title("TOAST").backdrop("road").time("dusk").music("mystery")
                .onEnter(level(3), longRest())
                .text("The business card just says TOAST. Somebody is behind all of this.",
                        "**LEVEL UP!** Everyone is now level 3.")
                .choice("Head for the mountain trail").icon("⛰").go("mq_steve");
    }

    // =========================================================================================== Part 3
    static void mountain(Campaign c) {
        c.scene("mq_steve").chapter(P3).title("Cowboy Steve's Supplies").backdrop("road").time("day").music("explore").checkpoint()
                .npc("Cowboy Steve", "Purveyor of Supplies", "#a04000", "🤠", "Howdy, partners. Headin' up the mountain?")
                .text("At the base of the trail, there's a covered wagon with a hand-painted sign: COWBOY STEVE'S SUPPLIES.",
                        "\"Howdy, partners. Headin' up the mountain? You'll want some Energy Drinks. Price includes the Red Bull tax. Don't look at me, I don't make the rules. Zip ties are over here. They won't fix it, but they'll keep it from comin' loose.\"",
                        "(The tips are shared, so don't get greedy.)")
                .choice("Energy Drink — 20 tips (+5 Red Bull tax)").icon("⚡").ifGold(25)
                    .effect(gold(-25), item("mq_energy_drink"), log("20 for the drink, 5 for the Red Bull tax."))
                    .go("mq_steve")
                .choice("Hold Music — 15 tips").icon("🎵").ifGold(15)
                    .effect(gold(-15), item("mq_hold_music"), log("Cowboy Steve says it doesn't work on Payam. It makes him stronger."))
                    .go("mq_steve")
                .choice("Zip Ties — 15 tips").icon("🪢").ifGold(15)
                    .effect(gold(-15), item("mq_zip_ties"))
                    .go("mq_steve")
                .choice("Head up the mountain").icon("⛰")
                    .effect(log("Cowboy Steve: \"Happy trails.\""))
                    .go("mq_sage");

        c.scene("mq_sage").chapter(P3).title("Sage Larson").backdrop("village").time("day").music("hope")
                .onEnter(longRest(), flag("jr_joined"))
                .npc("Sage Larson", "Hermit of the Mountain", "#6e2c00", "🍵", "You've come about the Main Station.")
                .text("Halfway up the mountain, there's a hut with smoke coming out of the chimney. The door opens before anyone knocks.",
                        "\"You've come about the Main Station.\" — \"I haven't said anything yet.\" — \"You were going to.\"",
                        "\"Before you touch anything, listen for the heartbeat between the station and the main. Let it beat the whole time. Count the beats that never come back.\"",
                        "JR: \"He means run a ping test first, leave it running, and look for dropped packets.\" — Sage Larson: \"I was getting there.\"",
                        "Sage Larson hands everyone a cup of tea before anyone can ask for one. (Everyone's back to full HP.) \"Take my son with you. He needs field experience.\" — JR: \"I've been ready for months.\" — \"Weeks.\"",
                        "**JR the Apprentice joins the party!** He'll smack enemies with a patch cable.")
                .choice("Thank Sage Larson and head up the trail").icon("🙏").go("mq_latency_gate");

        c.scene("mq_latency_gate").chapter(P3).title("Lord Latency").backdrop("snow").time("day").music("boss").checkpoint()
                .npc("Lord Latency", "Runs on Build 3.10", "#5b2c6f", "💀", "Welcome. ...You're too late.")
                .text("The trail gets steep. Up ahead, a skeleton in a robe of tangled patch cables is blocking the path. His mouth is already moving.",
                        "\"Welcome.\" (He said that four seconds ago.) \"You're too late.\" — \"Says the guy whose words show up after his mouth stops moving.\"",
                        "\"I am ancient. I run on build 3.10.\" — JR: \"Engineers won't even look at anything under 8.\" — \"...I know. That's why I work for Toast now. My king is waiting at the summit.\"")
                .choice("Run a ping test first (like Sage Larson said)").icon("📶").effect(flag("pinged"),
                        log("You start a ping test to the Main Station and leave it running. JR: \"900 milliseconds. Normal's 50. It's not you, it's the network.\" (Lord Latency starts the fight weakened.)"))
                    .battle("mq_latency")
                    .victory("Lord Latency fades out like a dropped connection. Four seconds later, everyone hears him say \"ow.\"", "mq_latency_end", gold(30), xp(200))
                .choice("Reseat his RAM").icon("🧠").effect(gold(15),
                        log("He's only registering 4 of his 12 gigs. You reseat it. Now he's got all 12. He's way faster, and his words show up on time. Somehow that's worse. JR: \"Old habits.\" (+15 tips for excellent customer service. He hits harder now.)"))
                    .battle("mq_latency_fast")
                    .victory("Lord Latency fades out like a dropped connection. Right on time, he says \"ow.\"", "mq_latency_end", gold(30), xp(200))
                .choice("Just fight").icon("⚔").battle("mq_latency")
                    .victory("Lord Latency fades out like a dropped connection. Four seconds later, everyone hears him say \"ow.\"", "mq_latency_end", gold(30), xp(200));

        c.scene("mq_latency_end").chapter(P3).title("Higher Up").backdrop("snow").time("dusk").music("mystery")
                .onEnter(level(4), longRest())
                .text("**LEVEL UP!** Everyone is now level 4.",
                        "Higher up, a station sits in the snow, stuck on the gear screen. It turns toward the party. Slowly.",
                        "Everyone looks down at their hands. Dre's note is still there.")
                .choice("Hidden button, top right").icon("👆").ifFlag("hidden_button").check("sleight_of_hand", 10).chooser()
                    .pass("The banner pops up. It's been pointed at the wrong main this whole time. You repoint it. The Revenant finds its way home and fades away. It was never evil. It was just lost.", "mq_white_pill", gold(20), xp(100))
                    .fail("You tap the top LEFT. That's the power icon. Wrong corner. It lurches toward you…", "mq_revenant")
                .choice("Fight it").icon("⚔").go("mq_revenant");

        c.scene("mq_revenant").chapter(P3).title("The Gear-Screen Revenant").backdrop("snow").time("dusk").music("battle")
                .text("The Revenant crashes toward you. It isn't trying to. It's just loading.")
                .choice("Fight the Revenant").icon("⚙").battle("mq_revenant")
                    .victory("The gear stops spinning. The Revenant finally finishes loading — straight into a login screen — and goes still.", "mq_white_pill", gold(20), xp(100));

        c.scene("mq_white_pill").chapter(P3).title("The White Pill").backdrop("snow").time("night").music("mystery")
                .text("Right below the summit, a giant white pill-shaped tower is blocking the path. Its light is off. Nothing past it has power.",
                        "JR: \"Dad would say follow the cable.\"")
                .choice("Kick it").icon("🦶")
                    .effect(log("You kick it. It's a network device, not a vending machine. The light stays off."))
                    .go("mq_white_pill")
                .choice("Follow its power cable, unplug it, wait 30 seconds, plug it back in").icon("🔌")
                    .effect(log("You follow the cable. It's black, not white, which is why nobody could find it. Unplug. Thirty seconds. Plug it back in. The light comes on solid white. Then it starts flashing white."))
                    .go("mq_flashing")
                .choice("Wait for it to come back on by itself").icon("⏳")
                    .effect(log("Nothing happens. It isn't getting power. It's never coming back on by itself."))
                    .go("mq_white_pill");

        c.scene("mq_flashing").chapter(P3).title("Flashing White").backdrop("snow").time("night").music("tension")
                .text("Flashing white. Flashing white. Flashing white.",
                        "JR: \"Didn't Dre say something about this?\"")
                .choice("Unplug it again, it's taking too long").icon("🔌")
                    .effect(log("Back to solid white. Then flashing white again. JR sighs the same sigh his dad does."))
                    .go("mq_flashing")
                .choice("Smack it").icon("✋")
                    .effect(log("Still flashing white. Now your hand hurts too."), damageOne("1", "bludgeoning"))
                    .go("mq_flashing")
                .choice("Leave it alone and wait for blue").icon("🔵").check("save:wis", 12).group()
                    .pass("Flashing white... Flashing white... Solid BLUE. The tower hums, and the path to the summit lights up. Nobody did anything, and it was the right call.", "mq_summit", xp(100))
                    .fail("Somebody's hand drifts toward the power cable. JR slaps it away. Try again.", "mq_flashing");
    }

    // =========================================================================================== the summit
    static void summit(Campaign c) {
        c.scene("mq_summit").chapter(TOP).title("The Summit").backdrop("snow").time("night").music("boss").checkpoint()
                .npc("The Toast King", "With Squire Square", "#d5d8dc", "👑", "Ah. SmartTab.")
                .text("At the summit, the Main Station sits on a rock, completely dark. Standing over it is a giant chrome toaster wearing a crown. A sales rep lanyard hangs off his lever. Next to him, a small, nervous square is holding his cape.",
                        "\"Every venue in this valley that goes offline gets a call from one of my reps within the hour. I've already got someone headed to Monroe.\" — \"Monroe? Payam's Monroe?\" — \"...Is that bad?\" — JR: \"You'll see.\"",
                        "Squire Square: \"We have free hardware!\" — The Toast King: \"Not now, Square.\"")
                .choice("Recruit Squire Square").icon("◼").once().check("persuasion", 12).chooser()
                    .pass("\"SmartTab actually answers the phone.\" Squire Square has never heard of that. \"Do you guys have free hardware?\" — \"We have Cowboy Steve.\" — \"...I'm in.\" (Squire Square switches sides!)", "mq_summit", flag("square_joined"))
                    .fail("Squire Square says he'll think about it. He won't.", "mq_summit")
                .choice("Find the cancellation clause").icon("📜").once().check("investigation", 12).chooser()
                    .pass("Page 47. The 90-day out clause. His contract can't hold anyone anymore. (The Toast King starts the fight weakened.)", "mq_summit", flag("clause_found"))
                    .fail("It's 140 pages. You're on page 3.", "mq_summit")
                .choice("Fight the Toast King").icon("⚔").battle("mq_toast_king")
                    .victory("", "mq_offline", xp(300));

        c.scene("mq_offline").chapter(TOP).title("MAIN STATION OFFLINE").backdrop("snow").time("night").music("battle")
                .onEnter(flag("dre_here"))
                .npc("Dre the Goblin", "Out of breath", "#1abc9c", "🎧", "Somebody say wrong main IP?")
                .text("The Toast King wobbles. Then he slams his lever down. Every station in the valley flips to MAIN STATION OFFLINE.",
                        "\"Somebody say wrong main IP?\" Dre bursts in, completely out of breath. He ran up the entire mountain.",
                        "You tap the top right corner. The banner pops up. Every station in the valley is pointed at a Toast server.")
                .choice("Repoint every station to the real Main Station").icon("👆").check("investigation", 12).advantageIf("dre_here").chooser()
                    .pass("Dre repoints them with you. One by one, every station in the valley turns back toward the real Main Station. The Toast King: \"Fine. Keep your venues. ...Can I at least have Monroe?\" His crown falls off. He's just a toaster now. Squire Square picks up the crown and quietly puts it in his pocket.", "mq_payam4")
                    .fail("Dre: \"Again!\" The Toast King burns you mid-fix.", "mq_offline", damageOne("1d6+1", "fire"));

        c.scene("mq_payam4").chapter(TOP).title("A Car at the Summit").backdrop("snow").time("night").music("tension")
                .npc("Payam", "Monroe", "#c0392b", "🚗", "I drove up. You were taking too long.")
                .text("The Main Station is right there. You pick it up. It's still warm.",
                        "Then a car pulls up at the summit. There is no road up here. Nobody knows how he did it.",
                        "\"I drove up. You were taking too long. Is it fixed? It's not fixed. Why isn't it fixed? Also, my bartenders want the old keyboard back.\"",
                        "From the ground, the Toast King looks up. \"Is this Monroe?\" — Payam: \"Who the fuck is this toaster?\"",
                        "You're holding the Main Station.")
                .choice("Plug the Main Station back in").icon("🔌").effect(flag("ending_online")).go("mq_back_online")
                .choice("Throw the Main Station at Payam's head").icon("🎯").check("luck", 2).chooser()
                    .pass("You wind up. You've never been more sure of anything. BONK. Payam stops talking. For the first time, Payam stops talking.", "mq_quick_chat", flag("ending_threw"))
                    .fail("You wind up... it slips, bounces twice, and lands perfectly in its slot. Everything comes back online. Payam: \"Took you long enough. Fuck you.\" He drives back down the mountain. He does not say thank you.", "mq_back_online", flag("ending_online"))
                .choice("Let Toast have Monroe").icon("🍞").effect(flag("ending_churned")).go("mq_churned");

        c.scene("mq_back_online").chapter(END).title("Back Online").backdrop("town").time("dusk").music("triumph")
                .text("One by one, the lights come back across the valley. Gear screens turn into login screens. Somewhere, a bartender throws away a napkin.",
                        "Back at SmartTab HQ, Buttram tells Joel he can finally take his headset off. Then Buttram takes his off too.",
                        "Sage Larson: \"I knew it would work.\" — JR: \"He did. He always does.\" — Dre: \"Nobody unplugged the white pill while it was flashing, right?\" — Everyone: \"Nobody unplugged it.\"",
                        "Baron Daniel Baron: \"Outstanding work. Take the rest of the day off.\" — Captain JD: \"It's 4:58.\"",
                        "Then the phone rings. Everyone freezes. \"Hey, it's Mark Piazza from Old Almaden. Everything's working great. Just wanted to call and say thanks.\"",
                        "**ENDING: BACK ONLINE**")
                .choice("Roll the credits").icon("🎬").go("mq_credits");

        c.scene("mq_quick_chat").chapter(END).title("Quick Chat").backdrop("hall").time("dusk").music("sorrow")
                .text("JR quietly picks up the Main Station and plugs it back in. The valley comes back online. Nobody mentions what just happened.",
                        "Payam sits up. He looks around. \"...Is it fixed?\" — \"It's fixed.\" — \"Fuck you.\" He gets back in his car and drives down a mountain that has no road.",
                        "That night, Monroe leaves SmartTab a one-star review: \"Got hit in the head with a Main Station. Worked fine afterward though.\"",
                        "Captain JD (on Slack): \"heard what happened. legend.\" ... \"dont tell kellie i said that\"",
                        "Then your calendar pings. *Lady Kellie: Quick chat? Location: My office. Duration: As long as it takes. Attached: Onboarding Packet, page 31.*")
                .choice("Open page 31").icon("📖").once().unlessFlag("packet_read")
                    .effect(log("Page 31 is one sentence, in bold: DO NOT THROW HARDWARE AT THE CLIENTS. Baron Daniel Baron adds a page to the onboarding packet. It's 41 pages now."))
                    .go("mq_quick_chat")
                .choice("You already know what page 31 says").icon("📖").once().ifFlag("packet_read")
                    .effect(log("You read the whole onboarding packet. You knew exactly what page 31 says. You did it anyway. Baron Daniel Baron adds a page to the onboarding packet. It's 41 pages now."))
                    .go("mq_quick_chat")
                .choice("ENDING: QUICK CHAT — roll the credits").icon("🎬").go("mq_credits");

        c.scene("mq_churned").chapter(END).title("Churned").backdrop("town").time("day").music("sorrow")
                .text("You take the crown out of Squire Square's pocket, put it back on the Toast King, and hand him a sticky note with Payam's number on it.",
                        "The Toast King: \"Pleasure doing business.\" — Payam: \"Fuck you. All of you.\"",
                        "JR plugs the Main Station back in. The valley comes back online. Every venue except one. Monroe switches to Toast that afternoon.",
                        "Six hours later, Toast's support line gets its first call from Payam. By Friday, Toast's entire support team has quit. The Toast King moves back into a kitchen. He makes toast now. He seems happier.",
                        "On Monday morning, the phone rings at SmartTab HQ. \"It's Payam. Monroe. I want to come back.\" — Baron Daniel Baron: \"We'll think about it.\" — Buttram: \"Joel, take this one.\" — Joel: \"I'll take this one.\"",
                        "**ENDING: CHURNED**")
                .choice("Roll the credits").icon("🎬").go("mq_credits");

        c.scene("mq_credits").chapter(END).title("Thanks for Playing SmartTab Mountain Quest").backdrop("hall").time("night").music("triumph")
                .text("**Starring:** your new hires, Dre the Goblin, Sage Larson, JR the Apprentice, Captain JD, Lady Kellie, Baron Daniel Baron, Buttram (Senior Holder of the Line), Joel (Holder of the Line), Cowboy Steve, Lord Latency, The Toast King, Squire Square, and Payam the Demanding ....... as himself.",
                        "*A story by Vijay.*")
                .choice("No stations were harmed in the making of this game.").icon("🎬").once().ifFlag("ending_online")
                    .effect(log("No stations were harmed in the making of this game.")).go("mq_credits")
                .choice("No stations were harmed in the making of this game. One was thrown.").icon("🎯").once().ifFlag("ending_threw")
                    .effect(log("No stations were harmed in the making of this game. One was thrown.")).go("mq_credits")
                .choice("No stations were harmed in the making of this game. One venue was churned. It came back.").icon("🍞").once().ifFlag("ending_churned")
                    .effect(log("No stations were harmed in the making of this game. One venue was churned. It came back.")).go("mq_credits")
                .choice("Clock out").icon("🏁").go("@end");
    }

    // =========================================================================================== battle maps
    static void encounters(Campaign c) {
        c.encounter("mq_printer").title("The Paper Jam Goblin").backdrop("hall").music("brawl").nonlethal()
                .intro("A gremlin of crumpled receipts and its paper friends pour out of the printer. Nobody gets hurt on day one (much).")
                .map("######################",
                     "#BBBB..........BBBB..#",
                     "#.......g............#",
                     "#..BB......BB....BB..#",
                     "#..BB...g..BB....BB..#",
                     "#.........E..........#",
                     "#..BB......BB....BB..#",
                     "#..BB......BB....BB..#",
                     "#.......PP..PP.......#",
                     "#.......PP..PP.......#",
                     "######################")
                .legend('g', "mq_paper_jam_goblin").extra("mq_paper_jam_goblin");

        c.encounter("mq_poltergeist").title("The Please Remove Card Poltergeist").backdrop("mine").music("battle")
                .intro("\"PLEASE REMOVE CARD.\" You don't have a card in. It doesn't care.")
                .map("######################",
                     "####,,,,,,....,,,,####",
                     "##,,......p.......,,##",
                     "#,.....B......B.....,#",
                     "#,..........E.......,#",
                     "#,...p.........p....,#",
                     "#,.....B......B.....,#",
                     "##,.....PP..PP....,,##",
                     "###,....PP..PP...,,###",
                     "######################")
                .legend('p', "mq_poltergeist").extra("mq_poltergeist");

        c.encounter("mq_reader").title("The Three-Headed Reader").backdrop("mine").music("battle")
                .intro("INSERT! SWIPE! TAP! Whatever you do, two of them are mad about it.")
                .map("######################",
                     "####,,,,,,....,,,,####",
                     "##,,......r.......,,##",
                     "#,.....B......B.....,#",
                     "#,..........E.......,#",
                     "#,...............g..,#",
                     "#,.....B......B.....,#",
                     "##,.....PP..PP....,,##",
                     "###,....PP..PP...,,###",
                     "######################")
                .legend('r', "mq_three_headed_reader").legend('g', "mq_paper_jam_goblin").extra("mq_poltergeist");

        c.encounter("mq_mimic").title("The Mimic").backdrop("mine").music("battle")
                .intro("The box says FREE. It has teeth.")
                .map("######################",
                     "####,,,,,,....,,,,####",
                     "##,,..............,,##",
                     "#,.....B..m...B.....,#",
                     "#,..........E.......,#",
                     "#,..................,#",
                     "#,.....B......B.....,#",
                     "##,.....PP..PP....,,##",
                     "###,....PP..PP...,,###",
                     "######################")
                .legend('m', "mq_mimic").extra("mq_paper_jam_goblin");

        c.encounter("mq_sir").title("Sir Declines-a-Lot").backdrop("mine").music("boss").objective("defeat:mq_sir")
                .intro("A knight built out of old card readers blocks the exit. His visor reads DECLINED. (Did you look at his lights?)")
                .weakenIf("sir_reset")
                .phase(50, "Dre, from behind a rock: \"Look at his lights! He's not in tamper, he's just confused!\"",
                        Json.obj("op", "say", "text", "\"DECLINED. DECLINED. DECL—\""))
                .map("######################",
                     "#####....######....###",
                     "###.......####......##",
                     "##.........S.........#",
                     "#..B..............B..#",
                     "#.........E..........#",
                     "#..B..............B..#",
                     "#....................#",
                     "##......PP..PP......##",
                     "###.....PP..PP.....###",
                     "######################")
                .legend('S', "mq_sir").extra("mq_three_headed_reader");

        c.encounter("mq_troll").title("The Chargeback Troll").backdrop("bridge").music("boss").objective("defeat:mq_troll")
                .intro("The Chargeback Troll is buried in paperwork. While his OPEN DISPUTES stand, he gets his HP back every turn — resolve them (smash the paperwork)! The canyon is right there: shove him in if you can.")
                .phase(60, "Somebody's phone rings mid-fight. Buttram: \"Buttram here. I've got Joel on the line too. I found the signed receipts. They were in the drawer under the other drawer. Joel, send them.\" Joel: \"Sending them now.\"",
                        Json.obj("op", "say", "text", "Buttram: \"Also, Payam called. Twice. Joel took both.\" Joel: \"I took both.\""))
                .map("CCCC,,,,,,,,,,,,,,CCCC",
                     "CCC,,..d........,,,CCC",
                     "CC,..................C",
                     "C,.........q.........C",
                     ",..............d.....,",
                     ",..........E.........,",
                     "CCCCC============CCCCC",
                     ",...........d........,",
                     ",.......PP..PP.......,",
                     "CC,.....PP..PP......CC",
                     "CCCC,,,,,,,,,,,,,,CCCC")
                .legend('q', "mq_troll").legend('d', "mq_dispute").extra("mq_paper_jam_goblin");

        for (String id : new String[]{"mq_latency", "mq_latency_fast"}) {
            Campaign.Encounter e = c.encounter(id).title("Lord Latency").backdrop("snow").music("boss").objective("defeat:" + id)
                    .intro("Everything Lord Latency does arrives four seconds late — except the LAG SPIKES. Dropped Packets swarm around him. JR is with you, smacking things with a patch cable.")
                    .ally("mq_jr", "jr_joined")
                    .phase(50, "LAG SPIKE! Everyone's screens freeze… and everything lands at once.",
                            Json.obj("op", "say", "text", "\"...ow.\" (He said that four seconds ago.)"))
                    .map("TTTT,,,,,,,,,,,,,,TTTT",
                         "TT,,.....,,,,.....,,TT",
                         "T,.......IIII.......,T",
                         ",.........y..........,",
                         ",...II..x.......II...,",
                         ",..........E.........,",
                         ",...II.......x..II...,",
                         "T,..................,T",
                         "TT,.....PP..PP....,,TT",
                         "TTT,....PP..PA...,,TTT",
                         "TTTT,,,,,,,,,,,,,,TTTT")
                    .legend('y', id).legend('x', "mq_packet").extra("mq_packet");
            if (id.equals("mq_latency")) e.weakenIf("pinged");
        }

        c.encounter("mq_revenant").title("The Gear-Screen Revenant").backdrop("snow").music("battle").objective("defeat:mq_revenant")
                .intro("A station stuck on the gear screen for three days. It's not angry. It's just loading.")
                .ally("mq_jr", "jr_joined")
                .map("TTTT,,,,,,,,,,,,,,TTTT",
                     "TT,,..............,,TT",
                     "T,.......IIII.......,T",
                     ",.........R..........,",
                     ",...II..........II...,",
                     ",..........E.........,",
                     ",...II..........II...,",
                     "T,..................,T",
                     "TT,.....PP..PP....,,TT",
                     "TTT,....PP..PA...,,TTT",
                     "TTTT,,,,,,,,,,,,,,TTTT")
                .legend('R', "mq_revenant").extra("mq_toast_crumb");

        c.encounter("mq_toast_king").title("The Toast King").backdrop("snow").music("boss").objective("defeat:mq_toast_king")
                .intro("THE TOAST KING. Both slots glow red. He's set to dark, his Auto-Renewal Clause slows anyone it catches, and he has three outage spells ready. JR fights with you — and Squire Square, if you recruited him.")
                .weakenIf("clause_found")
                .ally("mq_jr", "jr_joined")
                .ally("mq_square", "square_joined")
                .phase(75, "THE TOAST KING CASTS: TRANSACTION REVERSAL. A receipt prints. A closed tab pops back open! JR: \"The system never reverses on its own. Someone hit Refund!\" You recharge the tab — but he's already toasted himself back up a little.",
                        Json.obj("op", "heal", "dice", "2d10"),
                        Json.obj("op", "say", "text", "\"See? The system reverses on its own. You'd never have this problem with Toast.\""))
                .phase(50, "THE TOAST KING CASTS: THE VANISHING. Every open tab disappears — and Flying Croutons pour out of his slots! (Somebody had the screen filtered. Press the pink Drop All Filters button!)",
                        Json.obj("op", "summon", "monster", "mq_toast_crumb", "count", 3))
                .phase(25, "THE TOAST KING CASTS: THE FAKE REBOOT. His slots go dark... four seconds later, they're glowing red again. JR: \"That was suspiciously fast.\" The summit's rocks start sparking!",
                        Json.obj("op", "hazard", "count", 3, "type", "fire", "dice", "2d6", "name", "Burnt toast", "dc", 13),
                        Json.obj("op", "say", "text", "\"Not now, Square.\""))
                .map("TTTT,,,,,,,,,,,,,,TTTT",
                     "TT,,.....,,K,.....,,TT",
                     "T,.......IIII.......,T",
                     ",.....B........B.....,",
                     ",...II..........II...,",
                     ",..........E.........,",
                     ",...II..........II...,",
                     "T,......B....B......,T",
                     "TT,.....PP..PP....,,TT",
                     "TTT,....PA..AP...,,TTT",
                     "TTTT,,,,,,,,,,,,,,TTTT")
                .legend('K', "mq_toast_king").extra("mq_toast_crumb");
    }
}
