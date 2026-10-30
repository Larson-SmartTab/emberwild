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
 * SMARTTAB POS VS THE TOAST LEGION — a story by Dre Ryman, from the team's Java challenge (DNDDre.java), rebuilt for
 * Emberwild.
 * <pre>
 *  Chapter 1  The Lobby Network ...... Toast Goblins eating toast block the server closet (fight, or sneak past)
 *  Chapter 2  The Server Closet ...... a dusty supply crate, or a full diagnostic on the rack
 *  Chapter 3  The IT Supply Shop ..... Gary's folding table of IT gear
 *  Chapter 4  The Kitchen ............ a jammed ticket printer and the Toast KDS Ogre
 *  Chapter 5  Final Boss ............. Toast Tech Support. Your call is important to them. Please hold.
 * </pre>
 * Same story, same five reps and the same shop as the original. Uptime is hit points, Signal is Charisma (the 8-Port
 * Switch adds +3 to Charisma checks), Reboot Kits and toast are drinkable (edible) healing, and "Call SmartTab Support" is
 * a one-use throwable. Fixed on the way: Gary charges what his sign says, healing can't push you past your maximum,
 * charging the Ogre actually gets you the first hit, and your rank comes from how you played, not how little you spent.
 * Custom ids are prefixed {@code tl_}.
 */
public final class ToastLegion implements CampaignScript {

    static final String CH1 = "Chapter 1 — The Lobby Network";
    static final String CH2 = "Chapter 2 — The Server Closet";
    static final String CH3 = "Chapter 3 — The IT Supply Shop";
    static final String CH4 = "Chapter 4 — The Kitchen";
    static final String CH5 = "Chapter 5 — Final Boss (Toast Tech Support)";

    @Override
    public Campaign build() {
        Campaign c = Campaign.create("toast_legion", "SmartTab POS vs the Toast Legion, a story by Dre Ryman", "Dre Ryman");
        c.cover("tavern");
        c.synopsis("The Golden Fork just signed with SmartTab POS. But Toast has sent its legion to keep the venue locked into "
                + "their contract: Toast Goblins eating toast in the lobby, a Toast KDS Ogre in the kitchen, and at the end of the "
                + "hall, Toast Tech Support itself. Your call is important to them. Please hold. A story by Dre Ryman for 1-4 "
                + "SmartTab technicians from level 1 (rising to level 3); Golden Fork staff fill a small crew.");

        pregens(c);
        monsters(c);
        items(c);
        story(c);
        encounters(c);
        return c;
    }

    // =========================================================================================== the technicians
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
        // The same five SmartTab technicians as the original's character select.
        c.pregen(hero("tl_drive_smasher", Json.obj(
                "name", "The Drive Smasher", "pronouns", "he/him", "class", "fighter", "species", "human", "background", "soldier",
                "method", "pointbuy", "scores", scores(15, 13, 14, 8, 12, 10), "increases", Json.obj("str", 2, "con", 1),
                "skills", List.of("athletics", "perception"),
                "color", "#2e86c1", "emblem", "💽", "hair", "#3e2723", "skin", "#d7a77f", "eyes", "#5d4037",
                "appearance", "SmartTab polo, tool bag, and the calm of someone who has never met a hard drive he couldn't break.",
                "personality", "Balanced. Good at everything, great at nothing, and somehow always the one they send to Monroe.",
                "motto", "Another day, another venue.")));
        c.pregen(hero("tl_jd", Json.obj(
                "name", "MR janitor JD", "pronouns", "he/him", "class", "barbarian", "species", "dwarf", "background", "farmer",
                "method", "pointbuy", "scores", scores(15, 13, 14, 8, 12, 10), "increases", Json.obj("str", 2, "con", 1),
                "skills", List.of("athletics", "intimidation"),
                "color", "#7d6608", "emblem", "🧹", "hair", "#212121", "skin", "#c68642", "eyes", "#4e342e",
                "appearance", "SmartTab polo, a mop he refuses to explain, and more Uptime than anyone in the van.",
                "personality", "Cleans up every mess: cable runs, bar floors, other people's installs.",
                "motto", "I'll clean it up. I always clean it up.")));
        c.pregen(hero("tl_larson_jr", Json.obj(
                "name", "Larson Jr", "pronouns", "he/him", "class", "wizard", "species", "human", "background", "sage",
                "method", "pointbuy", "scores", scores(8, 14, 14, 15, 12, 8), "increases", Json.obj("int", 2, "con", 1),
                "skills", List.of("arcana", "investigation"),
                "cantrips", List.of("fire_bolt", "shocking_grasp", "ray_of_frost"),
                "spells", List.of("mage_armor", "shield", "magic_missile", "charm_person"),
                "color", "#8e44ad", "emblem", "📶", "hair", "#6d4c41", "skin", "#f1c27d", "eyes", "#1565c0",
                "appearance", "SmartTab polo, three chargers, and the most Bandwidth in the company.",
                "personality", "Treats every network like a puzzle and every puzzle like a personal challenge.",
                "motto", "Have you tried turning it off and on again? I have. Twice.")));
        c.pregen(hero("tl_vijay", Json.obj(
                "name", "Vijay", "pronouns", "he/him", "class", "bard", "species", "human", "background", "entertainer",
                "method", "pointbuy", "scores", scores(8, 14, 13, 10, 12, 15), "increases", Json.obj("cha", 2, "dex", 1),
                "skills", List.of("persuasion", "deception", "insight"),
                "color", "#16a085", "emblem", "📱", "hair", "#1b1b1b", "skin", "#a1665e", "eyes", "#3e2723",
                "appearance", "SmartTab polo, perfect posture, and the strongest Signal in the van.",
                "personality", "Can pair any device and win any argument. Usually at the same time.",
                "motto", "Full bars. Every time.")));
        c.pregen(hero("tl_dre", Json.obj(
                "name", "Dre", "pronouns", "he/him", "class", "rogue", "species", "human", "background", "criminal",
                "method", "pointbuy", "scores", scores(10, 15, 14, 12, 10, 10), "increases", Json.obj("dex", 2, "con", 1),
                "skills", List.of("acrobatics", "athletics", "intimidation", "perception"),
                "color", "#c0392b", "emblem", "🥊", "hair", "#000000", "skin", "#8d5524", "eyes", "#3e2723",
                "appearance", "SmartTab polo with the sleeves rolled up. Hits harder; has a little less Uptime to spare.",
                "personality", "Quiet until somebody insults the product.",
                "motto", "Say it again, Chad.")));

        // ---- The Golden Fork's staff: they back you up when the crew is small.
        Hero marisol = hero("tl_marisol", Json.obj(
                "name", "Marisol, Golden Fork Bar Manager", "pronouns", "she/her", "class", "cleric", "species", "human", "background", "acolyte",
                "method", "pointbuy", "scores", scores(12, 12, 14, 8, 15, 11), "increases", Json.obj("wis", 2, "cha", 1),
                "skills", List.of("medicine", "insight"),
                "cantrips", List.of("sacred_flame", "guidance", "toll_the_dead"),
                "spells", List.of("healing_word", "cure_wounds", "bless", "guiding_bolt"),
                "color", "#d4ac0d", "emblem", "🍴", "hair", "#3e2723", "skin", "#c68642", "eyes", "#3e2723",
                "appearance", "Runs the Golden Fork's bar and signed the SmartTab contract herself. Keeps a first-aid kit next to the bitters.",
                "personality", "Has been waiting three years to say \"we're not renewing\" to Toast. Will not be put on hold.",
                "motto", "We signed. We're staying."));
        marisol.npc = true;
        c.pregen(marisol);
        Hero tony = hero("tl_tony", Json.obj(
                "name", "Tony, Golden Fork Dishwasher", "pronouns", "he/him", "class", "fighter", "species", "human", "background", "guard",
                "method", "pointbuy", "scores", scores(15, 12, 14, 8, 13, 10), "increases", Json.obj("str", 2, "wis", 1),
                "skills", List.of("athletics", "intimidation"),
                "color", "#5d6d7e", "emblem", "🧽", "hair", "#111111", "skin", "#d7a77f", "eyes", "#3e2723",
                "appearance", "Forearms like hams, a dish sprayer holstered like a sidearm.",
                "personality", "Has scrubbed burnt toast off a thousand sheet pans. Takes this Toast business personally.",
                "motto", "Everything comes clean eventually."));
        tony.npc = true;
        c.pregen(tony);
    }

    // =========================================================================================== the Toast Legion
    static void monsters(Campaign c) {
        c.monster("tl_toast_goblin", "Toast Goblin").type("humanoid").role("skirmisher").ac(13).hp(15).stats(8, 14, 10, 8, 8, 8)
                .cr(0.25, 50)
                .attack(new Monsters.Attack("Declines Your Card Reader", 4, "1d6+2", "slashing"))
                .attack(new Monsters.Attack("Throws Burnt Toast", 4, "1d6+2", "fire").ranged(30, 60))
                .look("#d68910", "🍞").blurb("Eats toast. Throws toast. Logs in to attack. Toast's finest.");
        c.monster("tl_kds_ogre", "Toast KDS Ogre").type("giant").role("brute").size(2).ac(12).hp(48).stats(18, 8, 16, 5, 7, 7)
                .cr(2, 450)
                .attack(new Monsters.Attack("Hot Sheet Pan of Toast", 6, "2d8+3", "bludgeoning").plus("1d4", "fire"))
                .special(Monsters.special("counter_slam", "Slams His Fists on the Counter").shape("sphere", 10, 0).dmg("2d6", "bludgeoning").dc(13, Ability.DEX).rider("prone").recharge(5))
                .look("#a04000", "👹").blurb("A hulking ogre who guards the kitchen screens. Every ticket on his KDS says TOAST.");
        c.monster("tl_tech_support", "Toast Tech Support").type("fiend").role("boss").ac(15).hp(118).stats(12, 14, 16, 14, 12, 18)
                .cr(3, 700).multi(2).resist("psychic").condImmune("charmed", "frightened")
                .attack(new Monsters.Attack("Puts You on Hold", 7, "1d10+4", "psychic").rider("slowed", Ability.WIS, 13))
                .attack(new Monsters.Attack("Transfers You to Another Department", 7, "2d6+3", "force").ranged(60, 60).spell())
                .special(Monsters.special("please_hold", "\"Your Call Is Important to Us\"").shape("sphere", 15, 0).dmg("2d8", "psychic").dc(13, Ability.WIS).rider("frightened").recharge(5))
                .trait("legendary_resistance").legendary(1, "attack:Transfers You to Another Department", "move")
                .look("#7d3c98", "☎").blurb("A lone figure in the glow of a server rack. \"Thank you for calling Toast Support. Please hold.\"");
        c.monster("tl_ticket", "Escalation Ticket").type("construct").role("skirmisher").ac(11).hp(9).fly(30).stats(6, 14, 10, 1, 5, 1)
                .cr(0.125, 25).attack("Paper Cut (Priority: Low)", 4, "1d6+1", "slashing")
                .look("#f9e79f", "🎫").blurb("Opened at 9:02. Assigned to nobody. Status: Pending.");
    }

    static void items(Campaign c) {
        c.item("tl_reboot_kit", "Reboot Kit", "potion", "Power-cycle yourself. Restores 10 Uptime (hit points). Bonus Action.",
                Json.obj("use", "10", "value", 1500));
        c.item("tl_toast", "A Piece of Toast", "potion", "Taken off a Toast Goblin. Restores 5 Uptime (hit points). Bonus Action.",
                Json.obj("use", "5", "value", 0));
        c.item("tl_support_call", "SmartTab Support (24/7)", "bomb",
                "Once per fight: call SmartTab Support and a 24/7 engineer remotes in. Ranged attack (20 ft): 2d8 force damage.",
                Json.obj("use", "2d8", "damageType", "force", "value", 0));
        // Gary's shop (attune to them from your character sheet)
        c.item("tl_udm_pro", "UDM Pro", "wondrous", "Mounted in the rack. More Bandwidth: +1 to spell attacks and spell save DC, +1 initiative.",
                Json.obj("bonus", Json.obj("spell_attack", 1, "spell_dc", 1, "initiative", 1), "attunable", true, "value", 3000));
        c.item("tl_ups", "UPS Battery Backup", "wondrous", "The UPS kicks in! +5 Max Uptime (hit points) per level.",
                Json.obj("bonus", Json.obj("hp_per_level", 5), "attunable", true, "value", 5000));
        c.item("tl_cable_tester", "Cable Tester", "weapon", "You test every cable in the rack and fix the weak ones. +1 to weapon attack rolls and +2 to weapon damage.",
                Json.obj("bonus", Json.obj("attack", 1, "damage", 2), "attunable", true, "value", 6000));
        c.item("tl_switch", "8-Port Switch", "wondrous", "Wired in. Better Signal: +3 to Charisma checks.",
                Json.obj("bonus", Json.obj("cha_check", 3), "attunable", true, "value", 4000));
    }

    // =========================================================================================== the story
    static void story(Campaign c) {
        c.scene("tl_start").chapter("SmartTab POS vs the Toast Legion").title("The Golden Fork").backdrop("town").time("day").music("explore")
                .onEnter(companion("tl_marisol", 3), companion("tl_tony", 3))
                .text("The GOLDEN FORK just signed with SmartTab POS.",
                        "But Toast has sent its legion to keep the venue locked into their contract.",
                        "You're a Level 1 SmartTab Technician, and you've got a van full of terminals to install.")
                .choice("Grab your kit and head in").icon("🧰")
                    .effect(item("tl_reboot_kit"), item("tl_reboot_kit"), item("tl_toast"),
                            log("You grab a couple of Reboot Kits and a slice of toast for the road. Know your enemy."))
                    .go("tl_lobby");

        c.scene("tl_lobby").chapter(CH1).title("The Lobby Network").backdrop("tavern").time("day").music("tension").checkpoint()
                .ambience("crowd")
                .aside("Somewhere in this building, a Toast Goblin is eating its fourth slice. It won't be its last.")
                .text("You arrive at the venue to install SmartTab POS on every terminal.",
                        "The Toast legion has already flooded the network with Toast Goblins eating toast! Two of them are blocking the door to the server closet.")
                .choice("Fight the Toast Goblins").icon("⚔").battle("tl_goblins")
                    .victory("The goblins log out in a shower of crumbs. One drops its toast.", "tl_closet", gold(20000), item("tl_toast"), xp(100))
                .choice("Try to sneak past them").icon("🤫").check("stealth", 12).group()
                    .pass("You slipped past the goblins, took their toast, and found 5,000 gold in a dropped cash drawer!", "tl_closet", gold(5000), item("tl_toast"), item("tl_toast"), xp(100))
                    .fail("You tripped over an ethernet cable! The Toast Goblins spot you!", "tl_goblins_spotted");

        c.scene("tl_goblins_spotted").chapter(CH1).title("Spotted!").backdrop("tavern").time("day").music("battle")
                .text("Toast Goblin #1 throws a piece of toast and logs in to attack! Toast Goblin #2 is right behind it.")
                .choice("Fight!").icon("⚔").battle("tl_goblins")
                    .victory("The goblins log out in a shower of crumbs. One drops its toast.", "tl_closet", gold(20000), item("tl_toast"), xp(100));

        c.scene("tl_closet").chapter(CH2).title("The Server Closet").backdrop("dungeon").time("day").music("mystery")
                .onEnter(level(2))
                .text("Inside the closet, you see a dusty supply crate next to a rack of blinking switches. **(You're now level 2.)**")
                .choice("Open the supply crate").icon("📦").check("investigation", 10).chooser()
                    .pass("Nice! You found a spare router, 2 toast, and 25,000 gold in unclaimed tips!", "tl_shop", heal("10"), item("tl_toast"), item("tl_toast"), gold(25000), flag("rank_senior"), flag("rank_any"))
                    .fail("Ouch! It was a power surge trap! But you still find 10,000 gold.", "tl_shop", damageOne("4", "lightning"), gold(10000))
                .choice("Run a full diagnostic on the rack").icon("🩺")
                    .effect(longRest(), log("The diagnostic passes! Uptime and Bandwidth are back to max."))
                    .go("tl_shop");

        c.scene("tl_shop").chapter(CH3).title("Gary's IT Supply Shop").backdrop("dungeon").time("day").music("explore").checkpoint()
                .npc("Gary", "IT Supply Vendor", "#2874a6", "🧰", "Folding table, best prices in the building. Only prices in the building.")
                .text("Behind the server racks, a vendor named GARY has a folding table full of IT gear.",
                        "(Gear goes into your pack. Open your character sheet to **attune** to the UDM Pro, UPS, Cable Tester or 8-Port Switch — up to three at once.)")
                .choice("Cat6 Ethernet Cable — 2,000 gold (+10 Uptime)").icon("🔌").ifGold(2000)
                    .effect(gold(-2000), heal("10"), log("You swap in a fresh Cat6 cable."))
                    .go("tl_shop")
                .choice("UDM Pro — 3,000 gold (more Bandwidth)").icon("📡").once().ifGold(3000)
                    .effect(gold(-3000), item("tl_udm_pro"), log("You mount the UDM Pro in the rack."))
                    .go("tl_shop")
                .choice("UPS Battery Backup — 5,000 gold (more Max Uptime)").icon("🔋").once().ifGold(5000)
                    .effect(gold(-5000), item("tl_ups"), heal("10"), log("The UPS kicks in!"))
                    .go("tl_shop")
                .choice("Reboot Kit — 1,500 gold").icon("🧰").ifGold(1500)
                    .effect(gold(-1500), item("tl_reboot_kit"), log("You stash a Reboot Kit in your bag."))
                    .go("tl_shop")
                .choice("Cable Tester — 6,000 gold (+1 attack, +2 damage)").icon("🧪").once().ifGold(6000)
                    .effect(gold(-6000), item("tl_cable_tester"), log("You test every cable in the rack and fix the weak ones!"))
                    .go("tl_shop")
                .choice("8-Port Switch — 4,000 gold (+3 Signal)").icon("🔀").once().ifGold(4000)
                    .effect(gold(-4000), item("tl_switch"), log("You wire in the 8-Port Switch."))
                    .go("tl_shop")
                .choice("Leave the shop").icon("🚪")
                    .effect(log("You pack up your gear and move on."))
                    .go("tl_kitchen");

        c.scene("tl_kitchen").chapter(CH4).title("The Kitchen").backdrop("tavern").time("dusk").music("tension")
                .ambience("fire", "crowd")
                .text("The ticket printer is jammed and orders are piling up everywhere.",
                        "A hulking Toast KDS Ogre and some Toast Goblins stand guard over the kitchen screens.")
                .choice("Fix the ticket printer first").icon("🖨").once().check("investigation", 12).chooser()
                    .pass("The printer fires back up and the line cooks cheer! They give you a Reboot Kit, 1 toast, and 3,000 gold.", "tl_kitchen", item("tl_reboot_kit"), item("tl_toast"), gold(3000), flag("rank_legendary"), flag("rank_any"))
                    .fail("The printer sparks and shocks you!", "tl_kitchen", damageOne("3", "lightning"))
                .choice("Charge the Ogre").icon("💨").effect(flag("ogre_charged"), log("You charge before the Ogre can get his fists off the counter!")).battle("tl_ogre")
                    .victory("The Ogre topples into the dish pit. Every KDS screen in the kitchen flips from TOAST to SMARTTAB.", "tl_hall", gold(12000), item("tl_toast"), xp(300))
                .choice("Face the Ogre").icon("⚔").battle("tl_ogre")
                    .victory("The Ogre topples into the dish pit. Every KDS screen in the kitchen flips from TOAST to SMARTTAB.", "tl_hall", gold(12000), item("tl_toast"), xp(300));

        c.scene("tl_hall").chapter(CH5).title("Toast Tech Support").backdrop("dungeon").time("night").music("boss").checkpoint()
                .onEnter(level(3), shortRest())
                .npc("Toast Tech Support", "Your Call Is Important", "#7d3c98", "☎", "Thank you for calling Toast Support. Your call is important to us. Please hold.")
                .text("**(You're now level 3.)**",
                        "At the end of the hall, a lone figure sits in the glow of a server rack.",
                        "\"Thank you for calling Toast Support. Your call is important to us. Please hold.\"",
                        "(Your phone has SmartTab Support on speed dial — **one call per fight**.)")
                .choice("Refuse to hold").icon("⚔").effect(item("tl_support_call")).battle("tl_support")
                    .victory("", "tl_win", gold(40000), item("tl_toast"), xp(300));

        c.scene("tl_win").chapter("You Win").title("SmartTab Keeps the Venue!").backdrop("tavern").time("night").music("triumph")
                .ambience("crowd")
                .text("*** YOU BEAT TOAST TECH SUPPORT!! ***",
                        "The Golden Fork's terminals flicker to life, every one of them running SmartTab POS.",
                        "**=== YOU WIN — SMARTTAB KEEPS THE VENUE! ===**")
                .choice("RANK: Legendary Technician").icon("🏆").once().ifFlag("rank_legendary")
                    .effect(log("SmartTab opens a new office on every block.")).go("tl_win")
                .choice("RANK: Senior Technician").icon("🥈").once().ifFlag("rank_senior").unlessFlag("rank_legendary")
                    .effect(log("The Golden Fork sends you a fruit basket.")).go("tl_win")
                .choice("RANK: Rookie Technician").icon("🥉").once().unlessFlag("rank_any")
                    .effect(log("The venue is safe, but the network is held together by zip ties.")).go("tl_win")
                .choice("Pack up the van").icon("🏁").go("@end");
    }

    // =========================================================================================== battle maps
    static void encounters(Campaign c) {
        c.encounter("tl_goblins").title("Toast Goblins in the Lobby").backdrop("tavern").music("battle")
                .intro("Toast Goblins are blocking the server closet, throwing burnt toast and declining card readers. Log them out!")
                .map("######################",
                     "#....BB.....g....BB..#",
                     "#......###########...#",
                     "#..g...#.........#...#",
                     "#......#....E....#...#",
                     "#..BB..##.......##.BB#",
                     "#....................#",
                     "#..BB.........g..BB..#",
                     "#.......PP..PP.......#",
                     "#.......PP..PP.......#",
                     "######################")
                .legend('g', "tl_toast_goblin").extra("tl_toast_goblin");

        c.encounter("tl_ogre").title("The Toast KDS Ogre").backdrop("tavern").music("battle").objective("defeat:tl_kds_ogre")
                .intro("The Toast KDS Ogre slams his fists on the counter, and his Toast Goblins grab the sheet pans. The fryers are HOT — don't stand in them, but feel free to shove a goblin in.")
                .surpriseIf("ogre_charged")
                .map("######################",
                     "#BBBBBB..LLLL..BBBBBB#",
                     "#....................#",
                     "#...g.....O......g...#",
                     "#..BBBB........BBBB..#",
                     "#..........E.........#",
                     "#..BBBB........BBBB..#",
                     "#....................#",
                     "#.......PP..PP.......#",
                     "#.......PP..PP.......#",
                     "######################")
                .legend('O', "tl_kds_ogre").legend('g', "tl_toast_goblin").extra("tl_toast_goblin");

        c.encounter("tl_support").title("Toast Tech Support").backdrop("dungeon").music("boss").objective("defeat:tl_tech_support")
                .intro("TOAST TECH SUPPORT puts you on hold (slowed), transfers you to another department, and plays the hold message for everyone nearby. Escalation tickets pile up as the call drags on.")
                .phase(66, "\"Your call has been escalated.\" Escalation Tickets flutter out of the server rack!",
                        Json.obj("op", "summon", "monster", "tl_ticket", "count", 2))
                .phase(33, "Toast Tech Support is getting desperate! The hold music gets LOUDER.",
                        Json.obj("op", "hazard", "count", 3, "type", "thunder", "dice", "2d6", "name", "Hold music", "dc", 13),
                        Json.obj("op", "say", "text", "\"Please continue to hold. Your estimated wait time is... forty-five minutes.\""))
                .map("######################",
                     "#BBBB....#####...BBBB#",
                     "#........#.S.#.......#",
                     "#.....................",
                     "#..##...........##...#",
                     "#..##......E....##...#",
                     "#....................#",
                     "#..BB............BB..#",
                     "#.......PP..PP.......#",
                     "#.......PP..PP.......#",
                     "######################")
                .legend('S', "tl_tech_support").extra("tl_ticket");
    }
}
