// ============================================================================================
//  THE SUNKEN BELL — an example community campaign written in Java.
//
//  Drop a file like this into the campaigns/ folder (or upload it in the Campaign Forge) and
//  Emberwild compiles it at start-up. It hooks into the core campaign's tavern, so players
//  hear about it at the Gilded Antler and can wander off on this side-quest mid-game.
//
//  Rules for Java campaigns:
//   * one public class, no package line, implementing emberwild.story.CampaignScript
//   * build() returns a Campaign made with the fluent DSL below
//   * end your tale with .go("@return") to send the party back where they came from
//     (or "@end" if it's a standalone adventure)
//
//  Check it without starting a game:   java -jar emberwild.jar --validate campaigns/TheSunkenBell.java
// ============================================================================================

import emberwild.story.Campaign;
import emberwild.story.CampaignScript;
import emberwild.util.Json;

import static emberwild.story.Fx.*;

public class TheSunkenBell implements CampaignScript {

    @Override
    public Campaign build() {
        Campaign c = Campaign.create("sunken_bell", "The Sunken Bell", "Example Author");
        c.synopsis("Every night at midnight a bell tolls beneath the black water of Lake Mirrow, and every night another fisherman walks into the lake. A short side-tale for level 1-3 heroes.");
        c.cover("swamp");

        // Hook: shows up as a choice in the Gilded Antler's common room (anchor "tavern").
        // Other anchors in the core campaign: "town", "road", "camp", "ruins", "epilogue".
        c.hook("tavern", "Listen to the old fisherman muttering about a bell under the lake", "bell_rumor");

        // A custom monster. Stats follow the 2024 stat-block shape.
        c.monster("drowned_ringer", "The Drowned Bellringer")
                .type("undead").role("brute").ac(13).hp(30).speed(30)
                .stats(16, 10, 15, 6, 10, 8).cr(1, 200)
                .immune("poison").condImmune("poisoned", "charmed").vulnerable("radiant")
                .multi(2)
                .attack("Bell-Rope Lash", 5, "1d8+3", "bludgeoning")
                .special(emberwild.rules.Monsters.special("toll", "Deathly Toll").shape("sphere", 10, 0).dmg("2d6", "thunder").dc(12, emberwild.rules.Ability.CON).recharge(5))
                .trait("undead_fortitude")
                .look("#3d6b7a", "🔔").blurb("A bloated sexton still clutching the rope of his drowned bell.");

        c.scene("bell_rumor").chapter("The Sunken Bell").title("An Old Man's Tale").backdrop("tavern").time("night").music("tavern")
                .npc("Old Garrow", "Fisherman", "#3d6b7a", "🎣", "You hear it too, don't you? *Dong… dong…* Every midnight.")
                .text("A fisherman with hands like knotted rope stares into his cider.",
                        "\"There was a chapel on the old shore, before the flood. Bell and all. Now the bell rings under the water, and my brother walked into the lake to answer it.\"")
                .choice("\"Show us where.\"").icon("🔔").go("bell_shore")
                .choice("Buy him a drink and leave him be").icon("🍺").effect(gold(-1)).go("@return");

        c.scene("bell_shore").chapter("The Sunken Bell").title("The Shore of Lake Mirrow").backdrop("swamp").weather("fog").time("night").music("eerie")
                .text("The lake is glass-still. At midnight the toll begins — *dong… dong…* — and you feel a cold tug behind your eyes, a whisper: *come down, come down*.")
                .choice("Resist the call").icon("🧠").check("save:wis", 12).each()
                    .pass("You grit your teeth and the whisper slides off you.", "bell_dive")
                    .fail("You've waded in up to your waist before you snap out of it, freezing.", "bell_dive", damageOne("1d4", "cold"))
                .choice("Sing louder than the bell").icon("🎵").check("performance", 13)
                    .pass("Your song drowns out the toll and the whisper loses its grip on everyone.", "bell_dive", inspiration())
                    .fail("The bell is louder.", "bell_dive");

        c.scene("bell_dive").chapter("The Sunken Bell").title("The Drowned Chapel").backdrop("swamp").time("night").music("tension")
                .text("Garrow's boat carries you over the bell's grave. Below, the chapel's spire glimmers. And rising up the rope — hand over rotten hand — comes its bellringer.")
                .choice("Fight it on the shore").icon("⚔").battle("bell_fight")
                    .victory("The ringer collapses into weed and silt. The bell goes silent for the first time in fifty years. Garrow weeps, and presses a fire opal into your hand — his brother's.", "@return", item("gem"), gold(20), xp(150), flag("bell_silenced"))
                    .defeat("You drag each other out of the water, gasping. The bell tolls on. Maybe another night.", "@return");

        c.encounter("bell_fight").title("The Drowned Bellringer").backdrop("swamp").music("battle")
                .intro("The bellringer heaves itself out of the lake, rope whistling.")
                .map("~~~~~~~~~~~~~~~~~~",
                     "~~~~~~~~r~~~~~~~~~",
                     "~~~~,,,,,,,,,~~~~~",
                     ",,,,,,,,T,,,,,,,,,",
                     ",,,..........,,,,,",
                     ",,...PP..PP....,,,",
                     ",,,.............,,",
                     "TT,,,,,,,,,,,,,,TT")
                .legend('r', "drowned_ringer").extra("bog_zombie");
        return c;
    }
}
