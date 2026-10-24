package emberwild.story;

import emberwild.rules.Hero;
import emberwild.rules.Monsters;
import emberwild.util.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static emberwild.story.Fx.*;

/**
 * EMBERWILD: THE CINDER TYRANT — the core campaign, written with the same Java DSL your team can use.
 * <pre>
 *  Prologue   The Gilded Antler ......... tavern life, a dice game, arm-wrestling, a bard's ballad, a brawl
 *  Chapter 1  Into the Thornwood ........ goblin toll-bridge OR the haunted fen, a burning dryad, wolves at night
 *  Chapter 2  The Ashes of Briarwick .... a survivor, a trapped chapel, the Choir's zealots
 *  Chapter 3  Cinderdeep Ruins .......... a riddle-door, a warden's hall, the Corridor of Teeth, Sister Cindrel
 *  Finale     The Heart of Cinderdeep ... Vharkas, the Cinder Tyrant (braziers, eruptions, legendary actions)
 * </pre>
 * Seven possible battles, five booby traps, ~45 scenes, dozens of skill checks and spell solutions.
 */
public final class Emberwild implements CampaignScript {

    @Override
    public Campaign build() {
        Campaign c = Campaign.create("emberwild", "Emberwild: The Cinder Tyrant", "The Emberwild Team");
        c.core = true;
        c.cover("tavern");
        c.synopsis("Embers fall with the rain over the frontier town of Harrowmere, and the Thornwood glows at night. "
                + "A cult called the Ashen Choir is stealing villagers to wake Vharkas, a drake of living slag sealed beneath "
                + "the Cinderdeep Ruins three centuries ago. From a rowdy tavern to a burning dryad's grove, a trapped chapel "
                + "and a warden's tomb, a band of brand-new heroes must reach the Heart of Cinderdeep before the Tyrant wakes. "
                + "Built for 1-4 players starting at level 1 (rising to level 3); companions fill a small party.");

        pregens(c);
        monsters(c);
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
        // ---- The star of the show: a charming, element-wielding wood elf wizard.
        Hero sylwen = hero("sylwen", Json.obj(
                "name", "Sylwen Ashbloom", "pronouns", "she/her", "class", "wizard", "species", "elf", "lineage", "wood", "background", "noble",
                "method", "pointbuy", "scores", scores(8, 14, 12, 15, 10, 13), "increases", Json.obj("int", 2, "cha", 1),
                "skills", List.of("arcana", "nature", "deception", "insight", "performance"),
                "cantrips", List.of("fire_bolt", "ray_of_frost", "shocking_grasp"),
                "spells", List.of("mage_armor", "shield", "chromatic_orb", "charm_person"),
                "color", "#e8743b", "emblem", "🔥", "hair", "#c0502a", "skin", "#c99a6e", "eyes", "#4fbf6a",
                "appearance", "Copper-red hair braided with living ivy, freckles like scattered sparks, and green eyes that flicker gold when she casts. Wears a moss-green duelling coat lined with flame-orange silk.",
                "personality", "Warm, quick-witted and outrageously charming. Believes most problems can be talked through — and the rest can be set on fire, frozen, or politely electrocuted.",
                "backstory", "The youngest daughter of House Ashbloom, keepers of the Thornreach groves. Sent to study at the Lyceum of Vael, Sylwen discovered she was less interested in dusty theory than in the four elements singing under her fingertips. When letters from home spoke of fires in the Thornwood that no rain could quench, she left the Lyceum without asking permission.",
                "motto", "Fire for the foe, frost for the fool, and a smile for everyone else."));
        sylwen.spellbook.clear();
        sylwen.spellbook.addAll(List.of("mage_armor", "shield", "chromatic_orb", "charm_person", "burning_hands", "ice_knife"));
        c.pregen(sylwen);

        // ---- Extra heroes teammates can claim with one click.
        c.pregen(hero("kael", Json.obj(
                "name", "Kael Duskmantle", "pronouns", "he/him", "class", "rogue", "species", "tiefling", "lineage", "infernal", "background", "criminal",
                "method", "pointbuy", "scores", scores(8, 15, 14, 10, 12, 13), "increases", Json.obj("dex", 2, "con", 1),
                "skills", List.of("acrobatics", "perception", "deception", "persuasion"),
                "color", "#7d3cff", "emblem", "🗡", "hair", "#1c1c28", "skin", "#b5524e", "eyes", "#ffd34d",
                "appearance", "Ash-grey horns, a lazy grin and a coat with far too many pockets.",
                "personality", "Says he's only in it for the gold. Is lying.", "motto", "Locks are just doors that haven't met me yet.")));
        c.pregen(hero("torva", Json.obj(
                "name", "Torva Stonesong", "pronouns", "she/her", "class", "barbarian", "species", "goliath", "background", "soldier",
                "method", "pointbuy", "scores", scores(15, 13, 14, 8, 12, 10), "increases", Json.obj("str", 2, "con", 1),
                "skills", List.of("athletics", "perception", "survival"),
                "color", "#b03a2e", "emblem", "🪓", "hair", "#d9d0c1", "skin", "#9aa3ad", "eyes", "#6ec6ff",
                "appearance", "Seven and a half feet of tattooed granite with a laugh like a rockslide.",
                "personality", "Loud, loyal, loves a good song and a better fight.", "motto", "Mountains don't kneel.")));
        c.pregen(hero("pip", Json.obj(
                "name", "Pip Thistledown", "pronouns", "they/them", "class", "bard", "species", "halfling", "background", "entertainer",
                "method", "pointbuy", "scores", scores(8, 14, 13, 10, 12, 15), "increases", Json.obj("cha", 2, "dex", 1),
                "skills", List.of("persuasion", "insight", "sleight_of_hand"),
                "color", "#e84393", "emblem", "🎻", "hair", "#f6c35b", "skin", "#e9b48a", "eyes", "#8e5cff",
                "appearance", "A tiny whirlwind of patched velvet, ribbons and a fiddle older than their grandmother.",
                "personality", "Collects stories the way dragons collect gold.", "motto", "Every legend needs a bard. Lucky you!")));

        // ---- Companions: AI-controlled, they join only when the party is small.
        Hero brannoc = hero("brannoc", Json.obj(
                "name", "Brannoc Ironvein", "pronouns", "he/him", "class", "fighter", "species", "dwarf", "background", "soldier",
                "method", "pointbuy", "scores", scores(15, 13, 14, 8, 12, 10), "increases", Json.obj("str", 2, "con", 1),
                "skills", List.of("perception", "survival"),
                "color", "#8d6e63", "emblem", "⚒", "hair", "#8a8a8a", "skin", "#d29b78", "eyes", "#5d4037",
                "appearance", "A grizzled dwarf with a braided grey beard, a dented shield and a scowl that hides a soft heart.",
                "personality", "Retired from the Harrowmere watch. Complains constantly. Never leaves a friend behind.",
                "motto", "Shield up, chin down, keep walking."));
        brannoc.npc = true;
        c.pregen(brannoc);
        Hero wren = hero("wren", Json.obj(
                "name", "Wren Tallowmere", "pronouns", "she/her", "class", "cleric", "species", "human", "background", "acolyte",
                "method", "pointbuy", "scores", scores(12, 14, 13, 8, 15, 10), "increases", Json.obj("wis", 2, "cha", 1),
                "skills", List.of("medicine", "persuasion"),
                "cantrips", List.of("sacred_flame", "toll_the_dead", "guidance"),
                "spells", List.of("healing_word", "guiding_bolt", "bless", "cure_wounds"),
                "color", "#f4d35e", "emblem", "🕯", "hair", "#4a3426", "skin", "#f0c9a8", "eyes", "#3d7ab8",
                "appearance", "A young priestess of the Dawnflame in soot-stained white, a lantern always at her hip.",
                "personality", "Gentle, stubborn, and braver than she believes.", "motto", "Every flame can be a hearth or a pyre. Choose."));
        wren.npc = true;
        c.pregen(wren);
    }

    // =========================================================================================== monsters
    static void monsters(Campaign c) {
        c.monster("big_hollis", "Big Hollis").type("humanoid").role("brute").ac(12).hp(22).stats(17, 10, 15, 8, 10, 12).cr(0.5, 0)
                .attack("Dockhand's Fist", 5, "1d6+3", "bludgeoning").look("#c28b4a", "💪").blurb("Harrowmere's arm-wrestling champion — on your side!");
        c.monster("cindrel_ally", "Sister Cindrel").type("humanoid").role("caster").ac(14).hp(38).stats(10, 14, 12, 13, 16, 16).cr(2, 0)
                .multi(2).attack(new Monsters.Attack("Ember Scimitar", 4, "1d6+2", "slashing"))
                .attack(new Monsters.Attack("Hymn of Dawn", 5, "2d8", "radiant").ranged(60, 60).spell())
                .special(Monsters.special("rally", "Song of Mending").shape("heal", 0, 0).dmg("2d8+3", "").uses(2).bonus())
                .look("#f5b041", "🎶").blurb("She sings a different song now.");
    }

    // =========================================================================================== prologue
    static void prologue(Campaign c) {
        c.scene("start").chapter("Prologue").title("Rain on the Thornwood Road").backdrop("road").weather("emberrain").time("dusk").music("explore")
                .text("The rain has been falling for three days, and it is not alone. Mixed in with every grey drop is a fleck of orange — tiny embers that hiss when they touch the mud and glow for a heartbeat before they die.",
                        "Ahead, the frontier town of HARROWMERE huddles behind its timber wall. Beyond it, the ancient THORNWOOD rises like a dark tide… and deep among its trees, for weeks now, something has been glowing at night.",
                        "The lantern over the door of the GILDED ANTLER swings in the wind, promising a fire, a hot stew and — if the shouting and fiddle music are anything to go by — trouble of the friendly kind.")
                .choice("Push open the door of the Gilded Antler").icon("🚪").go("tavern_arrive")
                .choice("Study the strange embers in the rain first").icon("✨").check("arcana", 12)
                    .pass("You catch an ember on your palm. It doesn't burn — it *hums*. These aren't sparks from wildfire; they are ley-embers, flakes of raw elemental fire shaken loose from something vast and very old that is stirring beneath the forest.", "tavern_arrive", flag("sky_read"), xp(25))
                    .fail("The embers sting your fingers and tell you nothing, except that you are cold, wet and would very much like a drink.", "tavern_arrive");

        c.scene("tavern_arrive").chapter("Prologue").title("The Gilded Antler").backdrop("tavern").time("night").weather("none").music("tavern")
                .onEnter(companion("brannoc", 3), companion("wren", 3))
                .npc("Mira Holloway", "Proprietor of the Gilded Antler", "#d35400", "🍻", "Shut the door, you're letting the weather in! Sit, sit — stew's hot and the ale's honest.")
                .text("Warmth hits you like a hug. The Gilded Antler is all low beams, antler chandeliers and a hearth big enough to roast an ox. A halfling with flour on her apron and a ladle in her fist — MIRA HOLLOWAY — waves you toward the fire.",
                        "The room is loud with life: a wiry gnome rattles dice at a corner table, a mountain of a dockhand slams arms down on a barrel to roars of applause, and a bard with silver-threaded hair tunes a lute by the hearth.",
                        "Not everyone is merry. At the bar, a grizzled dwarf in a watchman's old cloak — BRANNOC IRONVEIN — nurses an ale and glares at the glowing window. Near the hearth, a young priestess, WREN TALLOWMERE, lights candles for the missing villagers of Briarwick. And in the darkest corner, a hooded man flinches every time the door opens.",
                        "(If your party is small, Brannoc and Wren decide to throw in with you — \"Somebody has to keep you lot alive,\" grumbles the dwarf.)")
                .choice("Find a seat and take in the room").icon("🪑").go("tavern");

        c.scene("tavern").chapter("Prologue").title("The Common Room").backdrop("tavern").time("night").music("tavern").anchor("tavern")
                .ambience("crowd", "fire", "rain").aside("Gerrold, if you sing that verse one more time I will set YOU on fire. Now — where was I?", "Ah, the Gilded Antler. Best stew this side of the Thornwood. Worst ale on either side.")
                .text("The fire crackles, the fiddle sings, and the Gilded Antler carries on around you. There is plenty to do on a night like this — and somewhere under the laughter, you can feel the story waiting to begin.")
                .choice("Talk to Mira at the bar").icon("🍻").unlessFlag("mira_done").go("mira")
                .choice("Join the game of Dragon's Bones").icon("🎲").unlessFlag("bones_done").go("bones")
                .choice("Accept Big Hollis's arm-wrestling challenge").icon("💪").unlessFlag("hollis_done").go("hollis")
                .choice("Sit by the hearth and listen to the bard").icon("🎻").unlessFlag("lark_done").go("lark")
                .choice("Read the notice board").icon("📋").unlessFlag("board_read").go("board")
                .choice("Approach the nervous hooded man in the corner").icon("🗺").unlessFlag("vesk_met").hint("Something tells you this is where the trouble starts.").go("vesk")
                .choice("Speak with Elder Maelis").icon("🏛").ifFlag("brawl_done").unlessFlag("quest_accepted").go("maelis")
                .choice("Turn in for the night").icon("🌙").ifFlag("quest_accepted").go("tavern_night");

        // ---- Mira
        c.scene("mira").chapter("Prologue").title("Mira's Bar").backdrop("tavern").time("night").music("tavern")
                .npc("Mira Holloway", "Proprietor of the Gilded Antler", "#d35400", "🍻", "Trouble? Love, there's always trouble. Lately it just smells of smoke.")
                .text("Mira slides a bowl of stew across the bar before you even ask. \"First one's on the house for anyone who looks that wet,\" she says. Up close, you notice the worry lines around her eyes.",
                        "\"Briarwick — the woodcutters' village up the Mill Road — burned a week ago. Not a body found. Not one. And there's hooded folk been coming through buying lamp oil by the barrel, paying in old coins… coins with a dragon stamped on 'em.\"")
                .choice("Ask what she knows about the glowing forest").icon("🔥").once()
                    .effect(flag("rumor_fires"), log("Mira: \"My gran used to say the Thornwood glows when the Tyrant dreams. Vharkas. Old fireside tale — a drake made of molten rock that the elves chained under the hills. Gran also said cats steal breath, so.\""))
                    .go("mira")
                .choice("Turn on the charm for the good gossip").icon("💗").once().check("persuasion", 12)
                    .pass("Mira laughs despite herself and leans in. \"Fine, fine, flatterer. The hooded lot? They wear iron pendants shaped like a burning throat. Call themselves the Ashen Choir. And — here, take this. You'll want it more than the last fool who left it.\" She presses a red, glimmering bottle into your hand.", "mira", item("potion_healing"), flag("know_choir"), attitude("mira", 1))
                    .fail("\"Nice try, love,\" Mira grins, swatting you with a towel. \"Save the sweet talk for the bard.\"", "mira")
                .choice("Buy a round for the whole house (10 gp)").icon("🍺").once().ifGold(10)
                    .effect(gold(-10), inspiration(), attitude("mira", 1), log("A cheer goes up! \"To the stranger!\" Mugs are raised in your honour, and the warmth of it settles in your chest like a promise. (Heroic Inspiration!)"))
                    .go("mira")
                .choice("Head back into the common room").icon("↩").effect(flag("mira_done")).go("tavern");

        // ---- Dragon's Bones
        c.scene("bones").chapter("Prologue").title("Dragon's Bones").backdrop("tavern").time("night").music("tavern")
                .npc("Old Fen", "Gnome Gambler, Retired (Allegedly)", "#16a085", "🎲", "Three bones, one dragon, no crying. Ten gold to sit, lass. Feeling lucky?")
                .text("OLD FEN is a gnome so wrinkled he looks like a walnut wearing a hat. He rattles three carved knucklebones in a dragon-skull cup. \"Rules are simple: roll higher than the dragon. The dragon is me. The dragon always wins.\" The onlookers chuckle. Several of them have empty purses.")
                .choice("Wager 10 gp and roll honestly").icon("🎲").ifGold(10).check("luck", 11)
                    .effect(gold(-10))
                    .pass("The bones clatter… and land on three dragons! The table erupts. Fen stares at the cup like it betrayed him and pushes 25 gold across the table. \"Beginner's luck,\" he mutters.", "tavern", gold(25), flag("bones_done"))
                    .fail("The bones settle on a miserable spread. Fen sweeps your gold away with a happy little hum. \"The dragon always wins!\"", "tavern", flag("bones_done"))
                .choice("Read Fen's tells before you bet").icon("👁").ifGold(10).check("insight", 13)
                    .effect(gold(-10))
                    .pass("Fen's left ear twitches every time his bones are weighted. You bet only when it doesn't. Three rounds later you're 20 gold up and Fen is sulking into his ale.", "tavern", gold(30), flag("bones_done"))
                    .fail("You stare so hard at Fen that he becomes deeply uncomfortable and wins out of spite.", "tavern", flag("bones_done"))
                .choice("Make a moonflower bloom in Fen's ale to distract him").icon("🌸").spell("druidcraft").check("sleight_of_hand", 10)
                    .pass("A tiny white flower uncurls from Fen's tankard. While he shrieks and the crowd howls with laughter, you 'adjust' the bones. The dragon, for once, loses. Even Fen laughs — \"Elf magic! Cheating! Brilliant!\" — and pays up.", "tavern", gold(20), flag("bones_done"), flag("fen_friend"))
                    .fail("The flower blooms beautifully. Fen sniffs it, sneezes, and catches your hand on the bones. \"Cheeky,\" he says, delighted, and keeps playing anyway. You break even.", "tavern", flag("bones_done"))
                .choice("Just watch for a while").icon("↩").go("tavern");

        // ---- Arm-wrestling
        c.scene("hollis").chapter("Prologue").title("The Barrel Throne").backdrop("tavern").time("night").music("tavern")
                .npc("Big Hollis", "Dockhand & Undefeated Arm-Wrestler", "#c28b4a", "💪", "Nobody's beaten me in six years. You wanna try, sparrow?")
                .text("BIG HOLLIS has forearms like mooring posts and a smile with three gaps in it. Ten copper coins sit stacked on the barrel between his elbows — and a hand-painted sign: BEAT HOLLIS, WIN 15 GOLD. LOSE, BUY HIM A DRINK.")
                .choice("Grip his hand and wrestle fair and square").icon("💪").check("athletics", 14)
                    .pass("Veins stand out on both your arms. The crowd goes silent. Then — SLAM! — Hollis's knuckles hit the barrel. He stares at his hand, then bursts into a roaring laugh and lifts you onto his shoulders. \"New champion!\"", "tavern", gold(15), flag("hollis_friend"), flag("hollis_done"))
                    .fail("It's like trying to bend a tree. Hollis wins gently, almost apologetically, and helps you rub the feeling back into your arm. You buy him a drink. He decides he likes you.", "tavern", gold(-1), flag("hollis_friend"), flag("hollis_done"))
                .choice("Let a little lightning do the talking").icon("⚡").spell("shocking_grasp").check("deception", 12)
                    .pass("At the crucial moment a spark leaps from your palm into his. Hollis yelps, his arm folds, and the crowd — who saw nothing — goes wild. Hollis squints at you, rubbing his tingling hand. \"…Elf's got a grip like a thunderstorm,\" he says admiringly. He never finds out.", "tavern", gold(15), flag("hollis_friend"), flag("hollis_done"))
                    .fail("The spark is a little too enthusiastic. Hollis's hair stands straight up. There is a long pause. Then the whole tavern bursts out laughing — Hollis loudest of all. \"Cheater! I love it! Drinks on the sparky one!\"", "tavern", gold(-2), flag("hollis_friend"), flag("hollis_done"))
                .choice("Convince him to throw the match for a good story").icon("💗").check("persuasion", 13)
                    .pass("\"Imagine the songs,\" you murmur. \"'The day Big Hollis was beaten by a stranger.' They'll come from three towns to see the rematch.\" Hollis considers this with great seriousness, then lets you win with tremendous theatrical agony.", "tavern", gold(15), flag("hollis_friend"), flag("hollis_done"))
                    .fail("\"Nice try, sparrow,\" Hollis rumbles, and pins you in a second flat. Still, he slaps you on the back hard enough to rattle your teeth. Friends now, apparently.", "tavern", flag("hollis_friend"), flag("hollis_done"))
                .choice("Decline politely").icon("↩").go("tavern");

        // ---- The bard
        c.scene("lark").chapter("Prologue").title("The Ballad of the Cinder Tyrant").backdrop("tavern").time("night").music("ballad")
                .npc("Lark Vellacourt", "Wandering Bard", "#8e44ad", "🎻", "Sit, friend. This one's old, and the old ones are always true — just not the way you think.")
                .text("The bard LARK sings softly, and the nearest tables hush to listen:",
                        "*\"When the hills ran red and the sky ran black, / Came Vharkas the Tyrant with fire at his back. / He drank up the rivers, he swallowed the sun, / And the folk of the Thornwood bowed down, every one…*",
                        "*…Till Ithrael the Warden, with frost in her hand, / Struck the Heartstone of Winter and bound him to land. / Now he sleeps under Cinder, he dreams in his chains — / Pray the Choir never wakes him, pray rain follows rain.\"*",
                        "Lark lets the last chord fade, and you notice her fingers are trembling.")
                .choice("Join in on the chorus").icon("🎵").check("performance", 12)
                    .pass("Your voice rises with Lark's, and the whole tavern joins the last refrain. When it ends, coins rain onto the hearthstones and Lark clasps your hand. \"You've a voice made for legends. Go make one.\"", "tavern", inspiration(), gold(5), flag("lark_done"))
                    .fail("You hit a note so spectacularly wrong that a dog outside starts howling. Lark laughs until she cries, and somehow the evening is better for it.", "tavern", flag("lark_done"))
                .choice("Ask about the verses nobody sings anymore").icon("📖").check("history", 13)
                    .pass("Lark's eyes sharpen. \"You know there's a last verse? Few do.\" She sings it under her breath: *'Frost in the deep and the warden's heart, / Break his chains and he falls apart.'* \"Cold is his bane. And the Choir… they don't free him. They *feed* him, through braziers, flame by flame. Break the fires, and he's weak.\"", "tavern", flag("know_weakness"), xp(25), flag("lark_done"))
                    .fail("\"Oh, it's just an old song,\" Lark says, a little too quickly, and starts something cheerful about a pig.", "tavern", flag("lark_done"))
                .choice("Applaud and move on").icon("↩").effect(flag("lark_done")).go("tavern");

        c.scene("board").chapter("Prologue").title("The Notice Board").backdrop("tavern").time("night").music("tavern")
                .text("Pinned notices flutter in the draft:",
                        "• **MISSING** — Tomas Reed, woodcutter of Briarwick. Brown hair, one ear. Reward for news.",
                        "• **WOLVES** on the Mill Road. Big ones. Bigger than normal big. Travel in groups.",
                        "• **WANTED** — \"King\" Snikkit, goblin, for highway tolling on the Old Stone Bridge. 30 gp for his crown (it is a teapot lid).",
                        "• **THE DAWNFLAME SHRINE** offers blessings to travellers. Donations welcome. — Sister Wren",
                        "• Someone has scrawled across the bottom in charcoal: *THE CHOIR SINGS SOON.*")
                .choice("Take the Snikkit bounty notice").icon("📋").effect(flag("board_read"), flag("bounty_snikkit")).go("tavern");

        // ---- Vesk & the brawl
        c.scene("vesk").chapter("Prologue").title("The Man in the Corner").backdrop("tavern").time("night").music("tension")
                .ambience("crowd", "fire").aside("Notice how the room got quieter? Taverns always know before you do.")
                .npc("Vesk Marrow", "Cartographer (Terrified)", "#5d6d7e", "🗺", "I don't— I'm not— please, just sit so they can't see my face.")
                .text("The hooded man is younger than he looked — ink-stained fingers, a scholar's squint, and the hollow eyes of someone who hasn't slept in days. A leather map-case is clutched against his chest like a shield. He glances at the door. Then at you. Then at the door again.")
                .choice("Speak gently and earn his trust").icon("💬").check("persuasion", 12)
                    .pass("Your calm voice works on him like warm tea. \"My name's Vesk. I'm a mapmaker. They paid me — the Choir — to chart the old Cinderdeep Ruins, every tunnel. I didn't know what for. Then I heard them singing… and I ran.\" He pushes the map-case into your hands. \"Take it. Please. They'll kill me for it.\"", "brawl_start", flag("vesk_met"), flag("vesk_trust"), item("ember_map"))
                    .fail("He shrinks away. \"Sorry, sorry, I can't—\" But his eyes keep flicking to the door, and you realise he isn't afraid of *you*.", "brawl_start", flag("vesk_met"))
                .choice("Read him: what is he really afraid of?").icon("👁").check("insight", 11)
                    .pass("He's not a criminal; he's prey. You say softly, \"Who's hunting you?\" and he cracks. \"The Ashen Choir. I mapped Cinderdeep for them. I know where they're taking the Briarwick folk.\" He shoves a map-case at you. \"If they get me, at least *you* will know.\"", "brawl_start", flag("vesk_met"), flag("vesk_trust"), item("ember_map"))
                    .fail("Something is badly wrong with him, but before you can work out what, the door bangs open.", "brawl_start", flag("vesk_met"))
                .choice("Cast Charm Person — a warm smile, a little magic").icon("💗").spell("charm_person")
                    .pass("Golden warmth flickers in your eyes. Vesk's shoulders unknot, and he looks at you as if you were his oldest friend. Everything pours out: the maps, the Choir, the captives, *the secret aqueduct entrance they don't guard*. \"You'll stop them, won't you? I knew you would.\" (When the charm fades in an hour he'll know — but he'll also know you were kind.)", "brawl_start", flag("vesk_met"), flag("vesk_trust"), flag("secret_entrance"), item("ember_map"));

        c.scene("brawl_start").chapter("Prologue").title("The Door Slams Open").backdrop("tavern").time("night").music("tension")
                .npc("Choir Enforcer", "Ashen Choir", "#5d4037", "🔨", "Evening, all. We're just here for the mapmaker. Nobody else needs to get hurt.")
                .text("The door crashes against the wall. Rain and embers swirl in around four figures in soot-black armbands — two broad-shouldered enforcers with maces, and a robed acolyte whose hymn smells of smoke. Behind them, a couple of half-drunk dockhands who've been paid to 'help'.",
                        "The music stops. Mira's ladle freezes mid-stir. Vesk makes a small, terrible sound.",
                        "\"The mapmaker,\" repeats the enforcer, \"and the rest of you can finish your stew.\"")
                .choice("Stand up. \"He's with us.\" (Tavern brawl — fists, chairs, no killing)").icon("⚔").battle("tavern_brawl")
                    .victory("The last enforcer crashes through a table and doesn't get up. The common room erupts in cheers and Mira, surveying the wreckage, sighs. \"Well. That's the furniture budget gone.\"", "after_brawl", flag("brawl_done"), flag("vesk_saved"))
                    .defeat("The world spins and goes dark. You wake on the floorboards to Mira pressing a cold cloth to your head. \"They took him,\" she says quietly. \"The mapmaker. But he dropped this.\" She presses a scorched map-case into your hands.", "after_brawl", flag("brawl_done"), flag("vesk_taken"), item("ember_map"))
                .choice("Stare down the hired dockhands first").icon("😠").check("intimidation", 14)
                    .pass("\"Is a few coppers worth your teeth?\" you ask the dockhands quietly. They look at each other… and suddenly remember urgent business at the bar. The cultists are on their own.", "brawl_small", flag("dockhands_fled"))
                    .fail("The dockhands laugh nervously and roll up their sleeves. Here we go.", "brawl_fight")
                .choice("Turn your smile on the enforcer (Charm Person)").icon("💗").spell("charm_person")
                    .pass("Your magic slides over the lead enforcer like warm honey. He lowers his mace and blinks at you. \"…You know, you're right, this is a bad idea.\" His partner stares at him in disbelief — and that moment of confusion is all you need.", "brawl_small", flag("brawl_surprise"), flag("dockhands_fled"));

        c.scene("brawl_fight").chapter("Prologue").title("Chairs Will Fly").backdrop("tavern").time("night").music("battle")
                .text("Big Hollis cracks his knuckles somewhere behind you. Brannoc sighs and puts down his ale. It's on.")
                .choice("Brawl!").icon("⚔").battle("tavern_brawl")
                    .victory("The last enforcer crashes through a table and doesn't get up. The tavern roars its approval. Mira surveys the wreckage. \"Well. That's the furniture budget gone.\"", "after_brawl", flag("brawl_done"), flag("vesk_saved"))
                    .defeat("Darkness. You wake on the floor to Mira's cold cloth. \"They took him,\" she says. \"But he dropped this.\" A scorched map-case.", "after_brawl", flag("brawl_done"), flag("vesk_taken"), item("ember_map"));

        c.scene("brawl_small").chapter("Prologue").title("Odds in Your Favour").backdrop("tavern").time("night").music("battle")
                .text("With their hired muscle gone, the cultists close ranks around their acolyte, maces up. \"Fine,\" snarls the enforcer. \"The hard way.\"")
                .choice("Brawl!").icon("⚔").battle("tavern_brawl_small")
                    .victory("The acolyte's hymn dies in a squeak as she's knocked flat. The Gilded Antler explodes into cheers and someone starts a song about you. It rhymes badly. It's wonderful.", "after_brawl", flag("brawl_done"), flag("vesk_saved"))
                    .defeat("Darkness. You wake to Mira's cold cloth. \"They took the mapmaker. But he dropped this.\"", "after_brawl", flag("brawl_done"), flag("vesk_taken"), item("ember_map"));

        c.scene("after_brawl").chapter("Prologue").title("Aftermath").backdrop("tavern").time("night").music("tavern")
                .text("The town watch arrives at a run, too late as usual, followed by a stooped, white-haired woman leaning on a staff of black oak: ELDER MAELIS, reeve of Harrowmere. She takes one look at the cultists, the wreckage, and you.",
                        "\"Well,\" she says. \"It seems the Thornwood has sent us heroes after all.\"")
                .choice("Search the unconscious cultists").icon("🔍").once().check("investigation", 10)
                    .pass("Among the coins and soot you find an iron pendant shaped like a burning throat, and a folded letter in elegant handwriting: *'Bring the mapmaker to Cinderdeep. The Tyrant must not wake unseen — my sister waits for me in his fire. — Sister Cindrel.'*", "after_brawl", item("choir_sigil"), gold(8), flag("cindrel_letter"))
                    .fail("You find a few coins and a lot of soot. One of the enforcers groans at you. You leave him be.", "after_brawl", gold(5))
                .choice("Help Mira set the tables right").icon("🧹").once()
                    .effect(attitude("mira", 2), flag("mira_grateful"), log("Mira blows a strand of hair from her eyes. \"You lot are all right. Rooms are free tonight, and don't argue.\""))
                    .go("after_brawl")
                .choice("Speak with Elder Maelis").icon("🏛").go("maelis");

        c.scene("maelis").chapter("Prologue").title("The Elder's Plea").backdrop("tavern").time("night").music("tension")
                .npc("Elder Maelis", "Reeve of Harrowmere", "#7f8c8d", "🏛", "I am too old to lie to you, so I won't: what I am about to ask may kill you.")
                .text("Maelis lowers herself into a chair by the fire. \"Briarwick burned eight days ago. Twenty-three souls, gone — not dead, *taken*. The Ashen Choir is marching them north to the Cinderdeep Ruins, where the elves bound Vharkas the Tyrant three hundred years ago.\"",
                        "\"I thought it a madman's cult. Then the Thornwood began to glow. The dark of the moon is three nights hence. If they mean to wake him, that is when.\"",
                        "She slides a heavy purse across the table. \"Harrowmere can pay one hundred gold to each of you. It is everything we have. Bring our people home.\"")
                .choice("\"We'll bring them home.\" Accept the quest").icon("🗡").effect(flag("quest_accepted"), xp(50)).go("tavern")
                .choice("Ask for more support — the town owes you").icon("💬").check("persuasion", 14)
                    .pass("Maelis studies you, then nods slowly. \"Fair. You've already bled for us.\" She adds a second purse and two potions from the shrine. \"Fifty more each on your return. And these, now.\"", "tavern", flag("quest_accepted"), flag("reward_up"), item("potion_healing"), item("potion_healing"), xp(50))
                    .fail("\"One hundred is all Harrowmere has,\" she says, without anger. \"But I'll pray for you, for whatever an old woman's prayers are worth.\" You accept.", "tavern", flag("quest_accepted"), xp(50));

        c.scene("tavern_night").chapter("Prologue").title("A Dream of Fire").backdrop("tavern").time("night").weather("none").music("eerie")
                .onEnter(longRest())
                .text("Mira's rooms are small and warm and smell of lavender. Sleep — or, for elves, the drifting trance — comes quickly.",
                        "In the dream you stand in a cavern of black glass. Rivers of light pulse beneath your feet like veins. Something enormous shifts in the dark, and a voice like a forge-bellows breathes your name.",
                        "*\"Little flame,\"* it says, almost fondly. *\"You love fire as I do. Come to me. Come and see what you could become.\"*",
                        "You wake with the taste of ash in your mouth, the morning sun on your face — and the certainty that something has noticed you.")
                .choice("Rise and greet the morning").icon("☀").go("harrowmere_morning");

        c.scene("harrowmere_morning").chapter("Prologue").title("Market Morning").backdrop("town").time("day").music("explore").shop().anchor("town").checkpoint()
                .onEnter(level(2))
                .npc("Tobin Brask", "Outfitter", "#2e86c1", "🛒", "Heading into the Thornwood? Then you'll want potions. And flasks. And more potions.")
                .text("You wake changed. Last night's brawl, the dream, the weight of the promise you made — something has settled into you, steel and spell alike. **(Every hero reaches level 2!)**",
                        "Harrowmere's market square bustles under a sky that is blue for the first time in days. TOBIN BRASK's outfitter stall is piled high with rope, lanterns and racks of glimmering bottles: healing potions, sticky flasks of alchemist's fire, and Tobin's pride — *Frostbloom Flasks*, everwinter sap that bursts into freezing mist.",
                        "Spend your gold wisely. The Thornwood does not take coin.")
                .choice("Visit the Dawnflame shrine for a blessing").icon("🕯").once()
                    .effect(inspiration(), heal("2d8"), log("Wren's fellow priests anoint your brow with ash and oil. \"May you be a hearth, not a pyre.\" (Heroic Inspiration)"))
                    .go("harrowmere_morning")
                .choice("Check on Vesk before you go").icon("🗺").once().ifFlag("vesk_saved")
                    .effect(flag("secret_entrance"), log("Vesk, bandaged and grateful, taps the map. \"Here — the old aqueduct. It runs under the east wall of Cinderdeep. The Choir doesn't know it's there. I didn't tell them. I'm glad I didn't.\""))
                    .go("harrowmere_morning")
                .choice("Set out along the King's Road").icon("🥾").go("road");
    }

    // =========================================================================================== chapter 1
    static void chapterOne(Campaign c) {
        c.scene("road").chapter("Chapter 1 — Into the Thornwood").title("The King's Road").backdrop("road").time("day").music("explore").anchor("road")
                .text("Harrowmere's wall shrinks behind you. The King's Road winds north between fields of barley that give way, little by little, to the THORNWOOD — oaks so old their roots heave the road stones, thickets of black briar, and a silence that swallows birdsong.",
                        "At a weathered crossroads stone, the way divides. The road continues north to the OLD STONE BRIDGE over the Rillwater. A muddy track branches east into the WEEPING FEN — a day shorter, say the locals, if you don't mind the wet. Or the ghosts.")
                .choice("Stay on the road: the Old Stone Bridge").icon("🌉").go("bridge")
                .choice("Take the shortcut through the Weeping Fen").icon("🌫").go("fen");

        // ---- The goblin bridge
        c.scene("bridge").chapter("Chapter 1 — Into the Thornwood").title("The Old Stone Bridge").backdrop("bridge").time("day").music("goblin")
                .npc("\"King\" Snikkit", "Toll-Taker of the Old Stone Bridge", "#33691e", "👑", "HALT! This is a ROYAL bridge. Royal toll! Ten shinies each, or — or — ROYAL STABBINGS!")
                .text("A barricade of carts, crates and at least one confused sheep blocks the Old Stone Bridge. Atop the pile stands a goblin in a cape made from a tablecloth, a blue teapot lid perched on his head like a crown: SNIKKIT, self-proclaimed King of the Bridge.",
                        "Behind him, a handful of goblins grin with too many teeth. You can't see how many more are hiding in the reeds — but you can *hear* them giggling.")
                .choice("Pay the toll (15 gp for the party)").icon("🪙").ifGold(15)
                    .effect(gold(-15), log("Snikkit bites every coin, pockets them and bows extravagantly. \"Long live the King! Pass, pass, peasants!\""))
                    .go("bridge_pass")
                .choice("Flatter the King outrageously").icon("👑").check("persuasion", 13)
                    .pass("\"Your Majesty,\" you say, with a bow that would make a courtier weep, \"word of your magnificent crown has reached even the Lyceum of Vael.\" Snikkit glows. He *preens*. \"Finally, someone with TASTE.\" He waves you through — and, as a royal favour, whispers that \"the smoky singing people\" paid him to watch for anyone heading to the ruins.", "bridge_pass", flag("snikkit_friend"), flag("ambush_known"), xp(50))
                    .fail("Snikkit narrows his eyes. \"Are you… MOCKING the crown?\" Uh-oh. \"ROYAL STABBINGS!\"", "bridge_fight")
                .choice("Charm the King (Charm Person)").icon("💗").spell("charm_person")
                    .pass("Your spell settles over Snikkit like a warm blanket. His eyes go wide and shiny. \"You,\" he whispers, \"are my BEST FRIEND.\" He gives you the teapot-lid crown (\"to remember me by!\"), tells you about the Choir's watchers in the woods, and orders his goblins to carry your packs across. They do not enjoy this.", "bridge_pass", flag("snikkit_friend"), flag("ambush_known"), flag("snikkit_crown"), gold(30), xp(50))
                .choice("Scan the reeds for hidden goblins first").icon("👁").check("perception", 13)
                    .pass("There — four of them, crouched in the reeds with shortbows, and two more under the bridge. They don't know you've seen them. If it comes to a fight, you'll strike first.", "bridge_fight", flag("goblins_spotted"))
                    .fail("Reeds. Mud. Giggling. That's all you can make out.", "bridge_fight")
                .choice("Claim the bounty: \"Snikkit! In the name of Harrowmere!\"").icon("⚔").ifFlag("bounty_snikkit").battle("bridge")
                    .victory("Snikkit flees in a flurry of tablecloth, leaving his teapot-lid crown spinning on the stones. The surviving goblins scatter into the fen. The bridge — and the bounty — are yours.", "bridge_pass", flag("snikkit_crown"), gold(10), xp(25))
                    .defeat("You wake hogtied on the bridge, pockets empty. Snikkit has drawn a moustache on your face. Eventually, the goblins get bored and wander off. Humiliating — but alive.", "bridge_pass", gold(-20));

        c.scene("bridge_fight").chapter("Chapter 1 — Into the Thornwood").title("Royal Stabbings").backdrop("bridge").time("day").music("battle")
                .text("Snikkit blows a whistle made from a chicken bone. Goblins boil out of the reeds, shrieking with delight.")
                .choice("Fight for the bridge!").icon("⚔").battle("bridge")
                    .victory("Snikkit flees in a flurry of tablecloth, leaving his teapot-lid crown spinning on the stones. The bridge is yours.", "bridge_pass", flag("snikkit_crown"), gold(10), xp(25))
                    .defeat("You wake hogtied, pockets lighter. Snikkit has drawn a moustache on your face. Eventually the goblins get bored and wander off.", "bridge_pass", gold(-20));

        c.scene("bridge_pass").chapter("Chapter 1 — Into the Thornwood").title("Beyond the Rillwater").backdrop("forest").time("day").music("explore")
                .text("North of the bridge the road dwindles to a deer track and the Thornwood closes overhead. The light turns green and gold. Somewhere ahead you smell woodsmoke — but wrong, somehow. Too sweet. Like burning honey.",
                        "The track leads into a hollow where an enormous oak stands alone… and it is burning from the inside.")
                .choice("Hurry toward the burning oak").icon("🌳").go("grove");

        // ---- The haunted fen
        c.scene("fen").chapter("Chapter 1 — Into the Thornwood").title("The Weeping Fen").backdrop("swamp").weather("fog").time("dusk").music("eerie")
                .text("The track sinks into a world of black water, drowned trees and fog so thick you can taste it. Reeds whisper. Somewhere a frog the size of a dog says *grrrup*. And out in the mist, pale lights bob and drift — beckoning, patient, beautiful.",
                        "The locals call them the Fen Lanterns. The locals also don't come here.")
                .choice("Find the firm path through the mire").icon("🧭").check("survival", 13)
                    .pass("You read the fen like a book: the hummocks where reeds grow thick, the tell-tale shine of deep water. Step by careful step, you lead the way.", "fen_heart", xp(25))
                    .fail("You're sure this is the path. You're wrong.", "fen_mire")
                .choice("Ask the reeds which way is safe (Druidcraft)").icon("🌿").spell("druidcraft")
                    .pass("You whisper to the reeds in the old elvish way, and they lean — all of them, ever so slightly — toward a hidden ridge of stone beneath the water. The fen, it seems, doesn't like the Choir either.", "fen_heart", xp(25))
                .choice("Follow the beautiful lights").icon("✧").go("fen_mire");

        c.scene("fen_mire").chapter("Chapter 1 — Into the Thornwood").title("The Sucking Mire").backdrop("swamp").weather("fog").time("dusk").music("tension")
                .text("The ground gives way. One moment there's moss under your boots, the next you're thigh-deep in cold, sucking mud that pulls like hungry hands. The lights in the fog draw closer, flickering with something like delight.")
                .choice("Haul yourselves free!").icon("💪").check("save:str", 12).each()
                    .pass("Mud-soaked and furious, you drag yourselves onto a hummock of solid ground.", "fen_heart")
                    .fail("The mire drags you under for a long, horrible moment before you claw your way out, choking on swamp water.", "fen_heart", damageOne("1d6+1", "bludgeoning"));

        c.scene("fen_heart").chapter("Chapter 1 — Into the Thornwood").title("The Drowned Shrine").backdrop("swamp").weather("fog").time("dusk").music("eerie")
                .text("A ruined shrine rises from the black water, its stones furred with moss. Chained to a pillar is a corpse in Choir robes — a deserter, by the look of it, left here to drown.",
                        "The water around the shrine begins to bubble. Bloated shapes heave up out of the muck, dragging weed and chain. Above them, a single Fen Lantern brightens hungrily.")
                .choice("Stand your ground").icon("⚔").battle("fen")
                    .victory("The last drowned thing sinks back into the fen and the lantern-light gutters out with a thin, disappointed whine. On the deserter's body you find a potion, a few coins, and a note: *'They lied. The Tyrant does not give back the dead. He only eats.'*", "fen_after", item("potion_healing"), gold(12), flag("cindrel_story"))
                    .defeat("You flee the shrine half-drowned, hauling each other through the reeds until the lights lose interest. You've lost your dignity and some blood, but the far edge of the fen is in sight.", "fen_after");

        c.scene("fen_after").chapter("Chapter 1 — Into the Thornwood").title("Out of the Mist").backdrop("forest").time("day").music("explore")
                .text("The fog thins. Solid ground. Birds. You have never been so happy to see a tree. Ahead, in a sun-dappled hollow, an enormous oak stands alone — and smoke is pouring from cracks in its bark. It is burning from the inside.")
                .choice("Hurry toward the burning oak").icon("🌳").go("grove");

        // ---- The dryad
        c.scene("grove").chapter("Chapter 1 — Into the Thornwood").title("The Hollow of Eloweth").backdrop("grove").time("day").weather("embers").music("grove")
                .npc("Eloweth", "Dryad of the Thousand-Year Oak", "#27ae60", "🌳", "It burns… it burns from the heart. Please. Please, little sister of the woods. Help me.")
                .text("The oak is vast — ten people couldn't link arms around it — and from a gash in its trunk juts an iron spike, glowing white-hot. Ember-light pulses along its roots like poison through veins.",
                        "From the bark steps a figure of living wood and leaf, her hair a cascade of autumn, her face streaked with sap like tears: ELOWETH, the dryad bound to this tree. Where the fire touches her, her leaves blacken and curl.",
                        "\"The singing ones drove it in,\" she whispers. \"They feed my heart-fire to their sleeping master. Every hour, I grow less.\"")
                .choice("Freeze the spike with Ray of Frost").icon("❄").spell("ray_of_frost")
                    .pass("A beam of blue-white cold lances from your fingertips. The spike screams — metal can scream, it turns out — and frost races across it, cracking, splitting, until it shatters like glass. The ember-light drains from the roots. Eloweth gasps like someone surfacing from deep water.", "grove_saved", xp(50))
                .choice("Wrench the spike out by force").icon("💪").check("athletics", 13).chooser()
                    .pass("You wrap your cloak around the spike and heave. It fights you, searing through the cloth — and then it tears free with a shriek of tortured metal and falls hissing into the moss.", "grove_saved", damageOne("1d6", "fire"), xp(50))
                    .fail("The spike is welded into the heartwood. Heat roars up your arms and you stagger back, palms blistering. It hasn't moved.", "grove", damageOne("2d6", "fire"))
                .choice("Soothe the oak's heart with Druidcraft and old words").icon("🌿").spell("druidcraft").check("nature", 12)
                    .pass("You lay your hands on the bark and sing the old growing-songs of the Thornreach. The oak *listens*. Slowly, sap wells up around the spike, cool and green, smothering the fire until the iron sizzles, blackens and drops out like a rotten tooth.", "grove_saved", xp(50))
                    .fail("The oak shivers at your touch but the fire is too deep. You need something else — cold, or strength, or faith.", "grove")
                .choice("Pray for the tree's spirit").icon("🙏").check("religion", 13)
                    .pass("You pray, and something answers — a cool breath of dawn wind that curls into the wound and smothers the flame. The spike cools to dull iron and slides free.", "grove_saved", xp(50))
                    .fail("Your prayer is sincere, but the fire doesn't care.", "grove")
                .choice("There's nothing we can do. Move on.").icon("↩")
                    .effect(log("You turn away. Behind you, the dryad's voice fades to a whisper of dry leaves."), flag("eloweth_lost"))
                    .go("camp");

        c.scene("grove_saved").chapter("Chapter 1 — Into the Thornwood").title("Eloweth's Gift").backdrop("grove").time("day").music("grove")
                .npc("Eloweth", "Dryad of the Thousand-Year Oak", "#2ecc71", "🌳", "Little sister. Little brothers and sisters, all of you. The Thornwood will remember this.")
                .onEnter(flag("eloweth_saved"))
                .text("Green floods back into the oak's leaves. Eloweth stands taller, her bark smoothing, flowers opening in her hair. She cups your face in hands that smell of rain.",
                        "\"The singing ones carry their stolen folk to Cinderdeep,\" she says. \"The warden who bound the Tyrant, Ithrael, was my friend once. Her armour still guards the Heartstone shard in the Hall of Wardens. Show her this, and she will know you are friends of the wood.\"",
                        "She weaves a circlet from living briar and moonflower and sets it on your brow. It fits perfectly.")
                .choice("Accept the circlet with thanks").icon("🌸").effect(item("thornwood_circlet"), log("The Thornwood Circlet can be attuned from your character sheet: +1 to Charisma checks and +5 ft Speed.")).go("camp");

        // ---- Camp & wolves
        c.scene("camp").chapter("Chapter 1 — Into the Thornwood").title("Night in the Thornwood").backdrop("camp").time("night").music("camp").anchor("camp")
                .text("You make camp in a ring of standing stones where the trees thin. The fire crackles, small and brave against the enormous dark. Above, through the branches, the forest glows faintly red to the north — closer now.",
                        "The night is long. There are things to do before you sleep.")
                .choice("Take the first watch").icon("👁").once().check("perception", 13)
                    .pass("Two hours past midnight, you see them: green eyes in the dark, circling. Many eyes. One pair much higher off the ground than the rest. You wake the others quietly. Whatever comes, it won't catch you sleeping.", "camp", flag("wolves_spotted"))
                    .fail("The fire pops. An owl calls. You decide everything is fine.", "camp")
                .choice("Tell stories around the fire").icon("🔥").once().check("performance", 11)
                    .pass("You tell the tale of the elf-princess who married a thunderstorm, and by the end even Brannoc is grinning into his beard. Courage, it turns out, is contagious. (Heroic Inspiration)", "camp", inspiration())
                    .fail("Your story wanders off and gets lost somewhere around the third dragon. Still, it's nice to laugh.", "camp")
                .choice("Study Vesk's map by firelight").icon("🗺").once().ifItem("ember_map").check("investigation", 12)
                    .pass("Beneath the Choir's neat markings, Vesk has inked a tiny secret: an old aqueduct tunnel running beneath the east wall of Cinderdeep, marked *'unguarded — they don't know.'*", "camp", flag("secret_entrance"))
                    .fail("Tunnels, halls, stairs… you're too tired to make sense of it.", "camp")
                .choice("Bank the fire and try to sleep").icon("🌙").go("wolves");

        c.scene("wolves").chapter("Chapter 1 — Into the Thornwood").title("Eyes in the Dark").backdrop("camp").time("night").music("tension")
                .text("A howl splits the night — then another, and another, from every side. Shapes slide between the standing stones: grey wolves with thorn-scratched muzzles and embers smouldering in their eyes. Behind them pads something far larger, scarred and black, its fur dusted with ash: ASHMAW, the Choir's hound.",
                        "The fire is the only thing holding them back. It won't hold for long.")
                .choice("Back to back — fight!").icon("⚔").battle("wolves")
                    .victory("Ashmaw's last howl dies in the dark. The surviving wolves melt into the trees, and the forest is quiet again. Around the beast's neck is an iron collar stamped with a burning throat.", "camp_dawn", item("choir_sigil"))
                    .defeat("The wolves drag your packs into the dark and you are left bleeding among the stones until dawn. Somehow, you live.", "camp_dawn")
                .choice("Calm the wolves — they are only hungry and afraid").icon("🐺").check("animal_handling", 14)
                    .pass("You crouch, make yourself small, speak low and steady. One by one the grey wolves stop snarling and slink back into the trees. Only Ashmaw remains, ember-eyed, unmoving — the cult's magic holds him. He charges.", "wolves_small")
                    .fail("A wolf lunges at your outstretched hand. So much for that.", "wolves_fight")
                .choice("Hurl a Fire Bolt into the brush to scatter them").icon("🔥").spell("fire_bolt").check("intimidation", 12)
                    .pass("FWOOM! The dry bracken erupts in a wall of flame. The grey wolves bolt yelping into the dark. Only Ashmaw remains — he walks *through* the fire, eyes blazing.", "wolves_small")
                    .fail("The flames flare and die in the damp bracken. The wolves flinch… then come on anyway.", "wolves_fight");

        c.scene("wolves_fight").chapter("Chapter 1 — Into the Thornwood").title("The Pack Closes In").backdrop("camp").time("night").music("battle")
                .text("They come all at once.")
                .choice("Fight!").icon("⚔").battle("wolves")
                    .victory("Ashmaw's last howl dies in the dark. The survivors melt away into the trees.", "camp_dawn", item("choir_sigil"))
                    .defeat("You survive the night — barely.", "camp_dawn");

        c.scene("wolves_small").chapter("Chapter 1 — Into the Thornwood").title("Ashmaw").backdrop("camp").time("night").music("battle")
                .text("Ashmaw is the size of a pony, muscle and scar and cinder, and he is not alone — one loyal wolf still pads at his side.")
                .choice("Face the alpha").icon("⚔").battle("wolves_small")
                    .victory("The great wolf falls. As the light leaves its eyes the ember-glow fades too, and for a moment it looks like any tired old wolf. Around its neck: an iron collar stamped with a burning throat.", "camp_dawn", item("choir_sigil"), xp(50))
                    .defeat("Ashmaw drags off your rations and leaves you bleeding. You survive till dawn.", "camp_dawn");

        c.scene("camp_dawn").chapter("Chapter 1 — Into the Thornwood").title("Grey Dawn").backdrop("forest").time("dawn").music("explore").checkpoint()
                .onEnter(longRest())
                .text("You sleep in shifts until a grey dawn creeps through the trees. Rested, healed and fed, you break camp.",
                        "The track north is churned with boot-prints and the deep ruts of cart wheels. Many boots. Some of them small. Some of them dragging. The trail leads to a pillar of black smoke on the horizon: BRIARWICK.")
                .choice("Follow the tracks to Briarwick").icon("🥾").go("briarwick");
    }

    // =========================================================================================== chapter 2
    static void chapterTwo(Campaign c) {
        c.scene("briarwick").chapter("Chapter 2 — The Ashes of Briarwick").title("The Ashes of Briarwick").backdrop("village").weather("ash").time("day").music("sorrow")
                .text("Briarwick was a pretty place once — you can tell from the flower boxes, scorched black under the windows, and the painted sign of the Merry Axe, half-burned and swinging on one chain. Now it is ash and silence.",
                        "No bodies. Just doors kicked in, tools dropped mid-task, a child's wooden horse in the road. And from the little stone chapel at the end of the lane, very faint, someone is *singing*.")
                .choice("Search the ruined houses for clues").icon("🔍").once().check("investigation", 12)
                    .pass("In the ash of the headwoman's house you find a page torn from a prayer book, written in a fine hand: *'Lira, my sister, I will bring you back. He promised. Fire took you and fire will return you.' — C.* So that's what drives Sister Cindrel: grief.", "briarwick", flag("cindrel_story"), xp(25))
                    .fail("Soot, broken crockery, and a cat that hisses at you from under a bed. Nothing useful.", "briarwick")
                .choice("Call out for survivors").icon("📣").once().check("perception", 11)
                    .pass("A scrape of wood, a muffled sob — from under a collapsed woodshed. A trapdoor. A root cellar.", "hobb")
                    .fail("Only the wind answers. Then a scrape of wood — under the woodshed? You can't be sure.", "hobb")
                .choice("Follow the singing to the chapel").icon("⛪").go("chapel");

        c.scene("hobb").chapter("Chapter 2 — The Ashes of Briarwick").title("The Root Cellar").backdrop("village").time("day").music("sorrow")
                .npc("Hobb Reed", "Woodcutter of Briarwick", "#795548", "🪓", "Are they— are they gone? The singing ones? Is it safe?")
                .text("In the dark of the root cellar crouches an old man with an axe across his knees and soot in his beard: HOBB REED, father of the missing Tomas. He's been hiding here for eight days, living on turnips and fear.",
                        "\"They came at night, singing,\" he whispers. \"Chained everyone. Took 'em north in carts to the Cinderdeep. There was a woman leading them, in red — she wept while she sang. Wept! And then she burned my house anyway.\"",
                        "He presses a bundle into your hands. \"My Ada's frost-flasks. She made 'em for the summer fires. Take 'em. Bring my boy home.\"")
                .choice("Promise to bring Tomas home").icon("🤝").effect(item("frost_flask"), item("frost_flask"), item("potion_healing"), attitude("hobb", 2)).go("briarwick")
                .choice("Comfort him, and make sure he gets to Harrowmere").icon("💗").check("persuasion", 10)
                    .pass("You talk until the shaking stops. He agrees to walk south to Harrowmere and Mira's warm fire. Before he goes he hands you Ada's flasks and grips your hand. \"You've a good heart. Don't let them burn it.\" (Heroic Inspiration)", "briarwick", item("frost_flask"), item("frost_flask"), item("potion_healing"), inspiration())
                    .fail("He nods, but he won't leave the cellar. Not until he knows. He gives you the flasks anyway.", "briarwick", item("frost_flask"), item("frost_flask"), item("potion_healing"));

        c.scene("chapel").chapter("Chapter 2 — The Ashes of Briarwick").title("The Chapel Door").backdrop("chapel").time("day").music("tension")
                .text("The chapel's stone walls survived the fire, though the windows are black holes. The singing comes from inside — a low, droning hymn in a language that makes your teeth ache. The door stands invitingly ajar.",
                        "*Invitingly.*")
                .choice("Check the doorway for traps").icon("🔍").check("perception", 13)
                    .pass("There: a hair-thin wire across the threshold, running up to a clay pot of lamp oil wedged over the door with a smouldering wick. A firetrap. Cute.", "chapel_trap_found")
                    .fail("Looks fine. You push the door open and step through — *snick* — something tugs at your ankle.", "chapel_trap")
                .choice("Kick the door in and charge!").icon("🦶").go("chapel_trap")
                .choice("Burn it first: Fire Bolt through the window").icon("🔥").spell("fire_bolt")
                    .pass("Your fire bolt streaks through the window and something inside explodes with a WHUMPH — the trap's oil pot, triggered early. A cultist screams in surprise. The singing stops. They know you're here, but they're scrambling.", "chapel_fight", flag("chapel_surprise"));

        c.scene("chapel_trap_found").chapter("Chapter 2 — The Ashes of Briarwick").title("The Firetrap").backdrop("chapel").time("day").music("tension")
                .text("The wire is taut. One wrong move and the oil pot comes down on whoever is standing in the doorway — along with the burning wick.")
                .choice("Disarm it with steady fingers").icon("🖐").check("sleight_of_hand", 12)
                    .pass("You pinch the wick out, ease the tension from the wire and lift the pot down like a sleeping baby. Then — because you're feeling cheeky — you re-rig it to fall on the *inside* of the door.", "chapel_fight", flag("chapel_surprise"), xp(25))
                    .fail("The wire slips. The pot tips — you leap back as it shatters across the threshold in a sheet of flame!", "chapel_trap")
                .choice("Freeze the wick with Ray of Frost").icon("❄").spell("ray_of_frost")
                    .pass("A whisper of frost and the wick goes out with a sad little hiss. The oil congeals in its pot. You step neatly over the wire.", "chapel_fight", flag("chapel_surprise"), xp(25))
                .choice("Step carefully over it").icon("🦶").check("acrobatics", 10).group()
                    .pass("One by one you step over the wire, wincing, holding your breath. Nobody trips it. Inside, the hymn carries on, oblivious.", "chapel_fight", flag("chapel_surprise"))
                    .fail("Someone's boot catches it. The pot tips—", "chapel_trap");

        c.scene("chapel_trap").chapter("Chapter 2 — The Ashes of Briarwick").title("WHUMPH!").backdrop("chapel").time("day").music("tension")
                .text("Burning oil cascades over the threshold in a sheet of orange flame!")
                .choice("Dive clear!").icon("🔥").check("save:dex", 12).each()
                    .pass("You tumble through the flames and come up inside the chapel, smoking slightly.", "chapel_fight")
                    .fail("The burning oil splashes over you before you can roll free.", "chapel_fight", damageOne("2d6", "fire"));

        c.scene("chapel_fight").chapter("Chapter 2 — The Ashes of Briarwick").title("The Choir at Prayer").backdrop("chapel").time("day").music("battle")
                .npc("Choir Zealot", "Ashen Choir", "#922b21", "🔥", "Heretics! Unbelievers! Your ashes will be the Tyrant's first meal!")
                .text("Inside, the pews have been dragged into a ring around a brazier of green-black fire. Robed acolytes kneel around it, and before the altar stands a ZEALOT of the Choir, eyes glowing like coals, a sacrificial knife in his hand.",
                        "\"The Tyrant hears our hymn!\" he howls. \"Let him hear YOU scream!\"")
                .choice("Break up this little sing-along").icon("⚔").battle("chapel")
                    .victory("The brazier topples and its green fire gutters out. On the zealot's body you find orders sealed with red wax: *'All captives to the Heart Chamber by the dark of the moon. Sister Cindrel will lead the final hymn. The braziers must burn until he wakes.'*", "chapel_after", flag("choir_orders"), item("potion_healing"), gold(20))
                    .defeat("You crawl out of the burning chapel and collapse in the ash. When you wake, the cultists are gone — north, toward the ruins.", "chapel_after");

        c.scene("chapel_after").chapter("Chapter 2 — The Ashes of Briarwick").title("North to Cinderdeep").backdrop("forest").time("dusk").music("explore")
                .onEnter(shortRest())
                .text("You bind your wounds in the chapel yard (short rest). To the north the land rises toward a jagged ridge where the forest gives way to black rock — and atop it, outlined in red light like a crown of embers, the broken towers of the CINDERDEEP RUINS.",
                        "The cart tracks lead straight up the ridge. The dark of the moon is tomorrow night.")
                .choice("Climb toward the ruins").icon("⛰").go("ruins_approach");
    }

    // =========================================================================================== chapter 3
    static void chapterThree(Campaign c) {
        c.scene("ruins_approach").chapter("Chapter 3 — Cinderdeep").title("The Cinderdeep Ruins").backdrop("ruins").weather("embers").time("night").music("ruins").anchor("ruins").checkpoint()
                .text("Cinderdeep was an elven fortress once, carved into the cliff above a chasm that glows like an open forge. Its towers are broken, its gates scorched, its courtyards choked with black glass where the stone once ran molten.",
                        "Torches burn at the main gate. You count skeletal sentries in rusted armour, their eye-sockets lit by cult-fire, and an acolyte pacing the wall. Somewhere deep below, the mountain *breathes* — a slow, hot exhalation that stirs the embers in the air.")
                .choice("Storm the main gate").icon("⚔").battle("gate")
                    .victory("The last skeleton collapses in a clatter of bones and the cult-fire in its skull winks out. The gate is yours.", "runes", xp(25))
                    .defeat("You are driven back down the ridge, bloodied. When you return, the gate's defenders have scattered to hunt you — and the way is clear, if you're quick.", "runes")
                .choice("Sneak past the sentries in the shadows").icon("🌑").check("stealth", 12).group()
                    .pass("Shadow to shadow, rubble to rubble, you slip past the skeletons like ghosts. The acolyte on the wall yawns. Nobody sees a thing.", "runes", xp(50))
                    .fail("A loose stone skitters down the scree. Bony heads turn toward you, eye-fires flaring.", "gate_alarm")
                .choice("Take Vesk's secret aqueduct").icon("🗺").ifFlag("secret_entrance").go("aqueduct");

        c.scene("gate_alarm").chapter("Chapter 3 — Cinderdeep").title("Alarm!").backdrop("ruins").time("night").music("battle")
                .text("A bone horn sounds. The skeletons lurch toward you, swords rasping from rusted scabbards.")
                .choice("Fight!").icon("⚔").battle("gate")
                    .victory("Bones scatter across the courtyard. You press on before reinforcements arrive.", "runes", xp(25))
                    .defeat("You retreat, regroup, and eventually slip in through a gap in the broken wall.", "runes");

        c.scene("aqueduct").chapter("Chapter 3 — Cinderdeep").title("The Old Aqueduct").backdrop("cave").time("night").music("eerie")
                .text("Vesk's tunnel is exactly where he said: a dry stone channel, half-collapsed, leading under the east wall. It's a tight crawl — and near the end, the floor drops away into a shaft where you'll have to climb a slick wall of old stonework, the chasm's glow far below.")
                .choice("Climb the slick wall").icon("🧗").check("athletics", 11).each()
                    .pass("Fingers in cracks, toes on ledges, you haul yourselves up into the ruins — unseen, unheard.", "runes", xp(50))
                    .fail("A handhold crumbles and you fall, slamming into the stone before you catch yourself.", "runes", damageOne("1d6", "bludgeoning"))
                .choice("Rope up and go carefully").icon("🪢").ifItem("rope")
                    .effect(xp(50), log("You anchor a rope and climb one at a time. Slow, but nobody falls."))
                    .go("runes");

        c.scene("runes").chapter("Chapter 3 — Cinderdeep").title("The Warden's Door").backdrop("ruins").time("night").music("mystery")
                .text("Deep in the fortress, a great door of white stone bars the way down. It is carved with elvish runes that glimmer faintly blue — the only cold light you've seen in this place. Scorch-marks around it show the Choir has tried to burn it open, and failed.",
                        "Beneath the runes, a riddle is cut into the stone: *\"I am the heart that is cold, the guard that never sleeps, the light that is born in the dark. Speak me, and pass.\"*")
                .choice("Read the Elvish — it's your mother tongue").icon("🧝").ifSpecies("elf")
                    .effect(log("The runes aren't just a riddle; they're a lullaby. Your grandmother sang it. The answer is a word that means 'winter's heart' and 'a star' at once: *Ithrae*."))
                    .go("runes_open")
                .choice("Decipher the runes").icon("📜").check("arcana", 13)
                    .pass("The runes are a warding-spell wrapped in a riddle. The answer is the thing that holds the Tyrant: *the Heartstone* — winter's heart, the warden that never sleeps, a star of cold light in the deep.", "runes_open", xp(25))
                    .fail("The runes swim before your eyes. You'll have to guess.", "runes_guess")
                .choice("Recall the old legends").icon("📖").check("history", 12)
                    .pass("The Warden Ithrael bound Vharkas with the *Heartstone of Winter*. A cold heart, a sleepless guard, light in the dark. That's the answer.", "runes_open", xp(25))
                    .fail("Legends blur together. You'll have to guess.", "runes_guess")
                .choice("Answer: \"The Heartstone.\"").icon("💎").ifFlag("know_weakness").go("runes_open");

        c.scene("runes_guess").chapter("Chapter 3 — Cinderdeep").title("A Guess in the Dark").backdrop("ruins").time("night").music("mystery")
                .text("The riddle waits. The runes glimmer. Choose your answer.")
                .choice("\"Ice.\"").icon("❄").go("runes_wrong")
                .choice("\"The moon.\"").icon("🌙").go("runes_wrong")
                .choice("\"The Heartstone.\"").icon("💎").go("runes_open")
                .choice("\"A sleeping dragon.\"").icon("🐉").go("runes_wrong");

        c.scene("runes_wrong").chapter("Chapter 3 — Cinderdeep").title("The Glyph Awakens").backdrop("ruins").time("night").music("tension")
                .text("The runes flare from blue to blinding white. A glyph of warding detonates across the doorway in a ring of cold fire!")
                .choice("Throw yourselves aside!").icon("💥").check("save:dex", 13).each()
                    .pass("You dive clear as the glyph's blast scours the flagstones. When the light fades, the riddle is waiting again — patient as a cat.", "runes_guess")
                    .fail("The blast catches you full on, searing cold and bright.", "runes_guess", damageOne("2d6", "cold"));

        c.scene("runes_open").chapter("Chapter 3 — Cinderdeep").title("The Way Down").backdrop("ruins").time("night").music("mystery")
                .text("At the true answer, the runes sing — a single pure note, like a finger on a glass rim — and the white door slides back with a sigh of cold air. Beyond, steps descend into a vaulted hall lined with statues of elven knights. At its far end, something in armour stands utterly still.")
                .choice("Descend into the Hall of Wardens").icon("⬇").go("warden_hall");

        c.scene("warden_hall").chapter("Chapter 3 — Cinderdeep").title("The Hall of Wardens").backdrop("hall").time("night").music("mystery")
                .npc("Ithrael's Warden", "Animated Armour of the Last Warden", "#90a4ae", "🛡", "…WHO… COMES… TO THE HEART?")
                .text("The hall is cold, gloriously cold after the heat above. Frost rimes the pillars. At the far end, on a dais, stands a suit of elven plate with no one inside it — a blue gem glowing in its chest where a heart would be. In its gauntlets it holds a shard of crystal that pours out light like a winter star: THE HEARTSTONE SHARD.",
                        "Two skeletons in Choir robes lie crumpled at its feet. They clearly tried to take the shard. The armour's helm turns toward you with a grinding of ancient steel.")
                .choice("Show the Warden Eloweth's circlet").icon("🌸").ifItem("thornwood_circlet")
                    .effect(item("heartstone_shard"), flag("heartstone"), xp(100), log("The armour goes still. The blue gem in its chest flickers — then it kneels, slowly, steel grinding, and holds out the shard in both gauntlets. *\"ELOWETH'S… FRIENDS. ITHRAEL… WOULD HAVE… LIKED YOU. END… HIM.\"*"))
                    .go("blade_hall")
                .choice("Convince the Warden you are Ithrael's heirs").icon("💬").check("persuasion", 15).advantageIf("eloweth_saved")
                    .pass("You speak of the Thornwood, of the Choir, of the people in chains below. The Warden listens. At last it kneels and offers the shard. *\"GO… THEN… AND… WIN.\"*", "blade_hall", item("heartstone_shard"), flag("heartstone"), xp(100))
                    .fail("*\"LIARS. THIEVES. LIKE… THE OTHERS.\"* The Warden raises its sword. Behind it, the crumpled skeletons twitch and rise.", "warden_fight")
                .choice("Take the shard by force").icon("⚔").go("warden_fight");

        c.scene("warden_fight").chapter("Chapter 3 — Cinderdeep").title("The Last Guardian").backdrop("hall").time("night").music("battle")
                .text("The Warden advances, slow and inexorable, and the cult skeletons rise around it with cinders in their skulls.")
                .choice("Defend yourselves").icon("⚔").battle("warden")
                    .victory("The Warden's armour falls apart piece by piece, and the blue gem in its chest dims with something like relief. The shard lies on the frosted flagstones, waiting for you.", "blade_hall", item("heartstone_shard"), flag("heartstone"))
                    .defeat("You stagger from the hall, beaten, but the Warden doesn't pursue. It returns to its post… and lets the shard fall. Perhaps it saw something in you after all.", "blade_hall", item("heartstone_shard"), flag("heartstone"));

        c.scene("blade_hall").chapter("Chapter 3 — Cinderdeep").title("The Corridor of Teeth").backdrop("dungeon").time("night").music("tension")
                .text("Beyond the hall, a long, narrow corridor slopes down toward the heat. The walls are lined with slots — dozens of them, thin as a coin — and the floor is tiled in a careful pattern of black and red stones. Old bloodstains darken the grout.",
                        "From far below drifts the sound of chanting… and weeping.")
                .choice("Search for pressure plates").icon("🔍").check("investigation", 13)
                    .pass("The red tiles sit a hair lower than the black. You chalk a safe path and lead everyone through on tiptoe while the blades sleep in their slots.", "prison", xp(50))
                    .fail("You're fairly sure it's the black tiles. You are wrong.", "blade_trap")
                .choice("Jam the mechanism with Ray of Frost").icon("❄").spell("ray_of_frost")
                    .pass("You fire a ray of frost into the nearest slot. Somewhere inside the wall, gears groan, crackle and seize solid with ice. When you step on a red tile — nothing. Just a sad, frozen *clunk*.", "prison", xp(50))
                .choice("Disarm it with thieves' tools").icon("🛠").ifItem("thieves_tools").check("sleight_of_hand", 13)
                    .pass("You find the release panel, tease out a pin, and the whole corridor sighs as its springs go slack.", "prison", xp(50))
                    .fail("A click, a whir — oh no.", "blade_trap")
                .choice("Sprint through and trust your reflexes").icon("🏃").go("blade_trap");

        c.scene("blade_trap").chapter("Chapter 3 — Cinderdeep").title("Swinging Steel").backdrop("dungeon").time("night").music("tension")
                .text("*Clack-clack-clack-clack* — blades scythe out of the walls in a rippling wave down the corridor!")
                .choice("Duck, dodge, dive!").icon("⚔").check("save:dex", 13).each()
                    .pass("You twist through the storm of steel like a dancer and tumble out the far end.", "prison")
                    .fail("A blade rakes across you before you get clear.", "prison", damageOne("2d6", "slashing"));

        c.scene("prison").chapter("Chapter 3 — Cinderdeep").title("The Cells").backdrop("dungeon").time("night").music("sorrow")
                .text("The corridor opens onto a row of iron-barred cells carved into the rock. Inside, two dozen exhausted faces turn toward your light: the people of Briarwick, chained and soot-streaked and *alive*.",
                        "A young man with one ear presses against the bars. \"Are you — Harrowmere sent you?\" It's TOMAS REED. \"They're going to feed us to it tonight. The singer — Cindrel — she's at the Heart Chamber now. She… she cries, sometimes. I don't think she wants to do it.\"")
                .choice("Break the locks").icon("🔓").check("athletics", 12)
                    .pass("You wrench the rusted locks apart one after another. The villagers pour out, weeping and laughing. Tomas knows the way out — he'll lead them to the aqueduct.", "cindrel_meet", flag("villagers_freed"), xp(100))
                    .fail("The locks hold — but a villager points to a key ring hanging just out of reach on a hook. You hook it down with a broom. Freedom, the undignified way.", "cindrel_meet", flag("villagers_freed"), xp(100))
                .choice("Pick the locks quietly").icon("🖐").check("sleight_of_hand", 11)
                    .pass("Click, click, click. One by one the cells swing open in silence. Tomas leads the villagers out the way you came.", "cindrel_meet", flag("villagers_freed"), xp(100))
                    .fail("Your picks keep slipping, but eventually Tomas just reaches through the bars and points out the key ring on the wall. Everyone is too polite to mention it.", "cindrel_meet", flag("villagers_freed"), xp(100))
                .choice("Free Vesk — he's here too!").icon("🗺").ifFlag("vesk_taken")
                    .effect(flag("villagers_freed"), flag("vesk_saved"), xp(150), log("In the last cell, battered but breathing: Vesk. \"You came,\" he croaks. \"I knew someone would.\" He leads the villagers out through his secret aqueduct."))
                    .go("cindrel_meet");

        c.scene("cindrel_meet").chapter("Chapter 3 — Cinderdeep").title("The Final Hymn").backdrop("lair").time("night").music("cindrel")
                .npc("Sister Cindrel", "Voice of the Ashen Choir", "#c0392b", "🎶", "You shouldn't have come. Please. Please don't make me do this.")
                .text("The passage opens onto a ledge above the Heart Chamber. Below, a lake of magma churns and glows. Three black-iron braziers blaze around a vast shape half-sunk in the molten rock — a shape with horns, and wings, and an eye the size of a cartwheel that is very slowly opening.",
                        "On the ledge, before a ring of acolytes and capering magma mephits, stands a woman in red robes, singing. Her voice is beautiful. Tears are running down her face. SISTER CINDREL turns to face you, an ember-scimitar trembling in her hand.",
                        "\"He promised me Lira,\" she says. \"My sister. She burned in the temple fire, and he promised… *fire took her, fire will return her*.\"")
                .choice("Tell her the truth about the Tyrant's promises").icon("💬").check("persuasion", 15).advantageIf("cindrel_story")
                    .pass("\"He doesn't give back the dead, Cindrel. He *eats*. Ask the deserter you left to drown in the fen. Ask Briarwick.\" Her song falters. The scimitar lowers. For a long moment she looks at the thing in the lava — and you see her see it, truly, for the first time. \"…Lira would have hated me for this,\" she whispers. Then she turns, raises her voice, and sings a different song.", "cindrel_turns", xp(200))
                    .fail("\"You're lying!\" she screams. \"You're all LIARS!\" The acolytes surge forward.", "cindrel_fight")
                .choice("Show her the letter she wrote").icon("✉").ifFlag("cindrel_letter").check("insight", 13)
                    .pass("You hold up her own letter. \"'My sister waits for me in his fire.' Do you really believe that? Look at him, Cindrel. Does that look like a thing that gives?\" She stares at her own words. The scimitar falls from her fingers with a clang.", "cindrel_turns", xp(200))
                    .fail("She snatches it from the air and it burns to ash in her hand. \"He PROMISED.\"", "cindrel_fight")
                .choice("Charm her, and strike while she hesitates").icon("💗").spell("charm_person")
                    .pass("Your magic washes over her and her eyes soften. \"…Friend?\" she whispers — and in that moment of confusion, you strike first. (The enemy is surprised!)", "cindrel_fight", flag("cindrel_surprised"))
                .choice("Attack before the hymn is finished!").icon("⚔").go("cindrel_fight");

        c.scene("cindrel_fight").chapter("Chapter 3 — Cinderdeep").title("Voice of the Choir").backdrop("lair").time("night").music("battle")
                .text("Cindrel's song rises into a shriek and the acolytes take it up. Magma mephits cackle and leap from the lava. Below, the great eye opens a little wider.")
                .choice("Silence the Choir!").icon("⚔").battle("cindrel")
                    .victory("Cindrel falls to her knees, her song broken. \"Lira…\" she whispers, and then, looking past you at the waking Tyrant: \"The braziers. They are his chains *and* his strength. Break them.\" She does not rise again. The hymn is over — but below, the Tyrant is already waking.", "heart_shrine", flag("know_braziers"))
                    .defeat("You're thrown back from the ledge into the passage. Cindrel's hymn reaches its peak — and below, with a sound like a mountain breaking, the Tyrant wakes. There's no time left. You have to go down.", "heart_shrine");

        c.scene("cindrel_turns").chapter("Chapter 3 — Cinderdeep").title("A Different Song").backdrop("lair").time("night").music("hope")
                .npc("Sister Cindrel", "Formerly of the Ashen Choir", "#f5b041", "🎶", "I sang him awake. Let me help you sing him back to sleep — forever.")
                .onEnter(flag("cindrel_ally"), flag("know_braziers"))
                .text("Cindrel's new song is a temple hymn from Harrowmere — a song for the dawn. The acolytes falter, stare at her, and flee into the tunnels. The mephits scatter, hissing.",
                        "\"The braziers,\" she tells you urgently. \"They are his chains *and* his strength. While they burn, he heals. Break them — with anything but fire. And he hates the cold. Gods, how he hates the cold.\"",
                        "Far below, the great eye is fully open now. Cindrel picks up her scimitar. \"I'm coming with you.\"")
                .choice("Welcome her").icon("🤝").go("heart_shrine");
    }

    // =========================================================================================== finale
    static void finale(Campaign c) {
        c.scene("heart_shrine").chapter("Finale").title("The Warden's Shrine").backdrop("hall").time("night").music("hope").checkpoint()
                .onEnter(level(3), longRest())
                .text("At the bottom of the stair, before the final door, you find a tiny shrine cut into the rock: a carving of an elven woman holding a star. ITHRAEL. Cold air breathes from it, and the heat of the chamber beyond can't reach you here.",
                        "You rest, and the shrine's old magic knits your wounds and steadies your hearts. When you rise, you are more than you were. **(Every hero reaches level 3 and finishes a long rest! Wizards learn Scorching Ray and Misty Step.)**",
                        "Beyond the door, the mountain roars.")
                .choice("Pray at Ithrael's shrine").icon("🙏").once().check("religion", 12)
                    .pass("Frost blooms on the carving and a voice like wind over snow whispers: *\"Cold is his bane. Strike true.\"* (Heroic Inspiration)", "heart_shrine", inspiration())
                    .fail("The shrine is silent, but the cold is a comfort.", "heart_shrine")
                .choice("Prepare spells and gear (open your character sheet)").icon("📖").once().effect(log("Take a moment: attune magic items, swap prepared spells, and hand potions to whoever needs them most.")).go("heart_shrine")
                .choice("Open the door to the Heart Chamber").icon("🔥").go("heart_chamber");

        c.scene("heart_chamber").chapter("Finale").title("The Heart of Cinderdeep").backdrop("lair").time("night").music("boss").weather("embers")
                .npc("Vharkas", "The Cinder Tyrant", "#ff5722", "🐉", "Ahhh. The little flame. I dreamed of you. Kneel, and I will make you a queen of embers.")
                .text("The Heart Chamber is a cathedral of black glass around a lake of fire. Three iron braziers blaze on islands of stone, chains of light running from them into the molten lake — and from the lake, with a sound like a forge-door opening, VHARKAS rises.",
                        "He is a drake of living slag: obsidian scales cracked with glowing seams, wings of smoke and cinder, eyes like twin suns. Heat rolls off him in waves that blister the air. When he speaks, the mountain speaks with him.",
                        "*\"I have slept three hundred years,\"* he rumbles, *\"and I am so very hungry. You love fire, little elf. I can taste it on you. Kneel. Burn with me.\"*")
                .choice("\"I love fire. That's why I know how to put one out.\"").icon("❄").battle("boss")
                    .victory("", "victory")
                .choice("Raise the Heartstone shard!").icon("💎").ifFlag("heartstone").battle("boss")
                    .effect(log("The shard blazes with winter light. Frost races across Vharkas's scales, cracking them; he recoils with a roar of pain and fury, his eyes wide with something he hasn't felt in three hundred years: FEAR. (Vharkas starts the fight weakened and without Legendary Resistance.)"))
                    .victory("", "victory")
                .choice("Pretend to kneel… and strike first").icon("🎭").check("deception", 15)
                    .pass("You sink to one knee, head bowed. *\"Good,\"* purrs the Tyrant, lowering his great head. *\"Good, little flame.\"* Then you look up — and smile.", "boss_surprise")
                    .fail("*\"You think I cannot taste a lie?\"* Vharkas laughs, and the laugh is a gout of flame. \"COME, THEN.\"", "boss_now");

        c.scene("boss_surprise").chapter("Finale").title("The Tyrant Strikes Second").backdrop("lair").time("night").music("boss")
                .onEnter(flag("boss_surprised"))
                .text("Vharkas rears back, roaring, caught completely off guard.")
                .choice("NOW!").icon("⚔").battle("boss").victory("", "victory");

        c.scene("boss_now").chapter("Finale").title("Burn").backdrop("lair").time("night").music("boss")
                .text("The Tyrant spreads his wings and the chamber fills with fire.")
                .choice("Fight for your lives!").icon("⚔").battle("boss").victory("", "victory");

        c.scene("victory").chapter("Finale").title("The Tyrant Falls").backdrop("lair").time("night").music("triumph")
                .text("Vharkas screams — a sound like a mountain splitting — and falls. His glow dims seam by seam, scale by scale, until what crashes into the magma lake is only black, cooling stone. The chains of light snap. The braziers gutter out.",
                        "And then the Heart Chamber begins to collapse.",
                        "Cracks race across the black-glass ceiling. The lake surges. Somewhere above, the whole fortress groans. RUN.")
                .choice("Run for the stairs!").icon("🏃").check("athletics", 12).group()
                    .pass("You sprint up the stair as the chamber caves in behind you, a wall of heat and thunder at your heels — and burst out into the cold night air as the Cinderdeep Ruins fold in on themselves and are gone forever.", "epilogue", xp(200))
                    .fail("Falling rock batters you as you flee, but you make it — bruised, bleeding, laughing like lunatics — out into the night as the ruins collapse behind you.", "epilogue", damage("2d6", "bludgeoning", "dex", 12), xp(200));

        c.scene("epilogue").chapter("Epilogue").title("Home to the Gilded Antler").backdrop("tavern").time("night").music("triumph").anchor("epilogue")
                .onEnter(gold(100), longRest())
                .npc("Mira Holloway", "Proprietor of the Gilded Antler", "#d35400", "🍻", "Well don't just stand there dripping on my floor, heroes. Get in here! Drinks are on the house. Forever. Within reason.")
                .text("Three days later, Harrowmere throws the biggest feast in its history. The people of Briarwick are home. Lanterns hang from every eave. The Thornwood is dark again at night — just ordinary, wonderful dark.",
                        "Elder Maelis pays every coin she promised, and Mira has hung a new sign over the hearth with your names painted on it (several misspelled). Lark is already singing a brand new ballad. It is almost entirely untrue and you love it.",
                        "The night is yours. And somewhere out past the edge of the map, other roads are waiting.")
                .choice("Collect the extra reward Maelis promised").icon("💰").once().ifFlag("reward_up").effect(gold(50)).go("epilogue")
                .choice("Listen to Lark's new ballad").icon("🎻").once()
                    .effect(log("*\"…And the Elf with the fire, and the frost, and the smile, / Charmed half of the Thornwood and burned down the rest for a while…\"* Lark winks at you. The whole tavern sings the chorus."))
                    .go("epilogue")
                .choice("Find Cindrel at the Dawnflame shrine").icon("🎶").once().ifFlag("cindrel_ally")
                    .effect(log("Cindrel kneels before the shrine where her sister once sang. She has taken back her temple name. \"I'll spend the rest of my life making this right,\" she says. \"Thank you for giving me the chance.\""))
                    .go("epilogue")
                .choice("A letter arrives from the Thornwood").icon("🌸").once().ifFlag("eloweth_saved")
                    .effect(log("Not a letter — a single perfect moonflower that blooms on your windowsill overnight, and never wilts. Eloweth remembers."))
                    .go("epilogue")
                .choice("A royal proclamation arrives").icon("👑").once().ifFlag("snikkit_friend")
                    .effect(log("Scrawled on a stolen tablecloth: *\"BY ROYAL DEKREE, YOU ARE MADE DUKES OF THE BRIDJE. NO TOLL FOR YOU. EVER. — KING SNIKKIT (STILL KING)\"*"))
                    .go("epilogue")
                .choice("Raise a toast and end the tale").icon("🏁").go("@end");
    }

    // =========================================================================================== battle maps
    static void encounters(Campaign c) {
        c.encounter("tavern_brawl").title("Brawl at the Gilded Antler").backdrop("tavern").music("brawl").nonlethal()
                .intro("Tables overturn, tankards fly, and someone grabs a chair leg. Nobody dies in a proper tavern brawl — but everyone gets hurt.")
                .surpriseIf("brawl_surprise")
                .ally("big_hollis", "hollis_friend")
                .map("####################",
                     "#BB..B....###..BBB.#",
                     "#.......c.###......#",
                     "#..BB.........BB...#",
                     "#..BB....t.....b...#",
                     "#.........E........#",
                     "#....b.........BB..#",
                     "#..PP....BB........#",
                     "#..PA....BB....b...#",
                     "#..PP..............#",
                     "#BB......BB....BBB.#",
                     "####################")
                .legend('t', "thug").legend('b', "tavern_brawler").legend('c', "cultist").extra("tavern_brawler");

        c.encounter("tavern_brawl_small").title("Brawl at the Gilded Antler").backdrop("tavern").music("brawl").nonlethal()
                .intro("With the hired muscle gone, it's you against the Choir.")
                .surpriseIf("brawl_surprise")
                .ally("big_hollis", "hollis_friend")
                .map("####################",
                     "#BB..B....###..BBB.#",
                     "#.......c.###......#",
                     "#..BB.........BB...#",
                     "#..BB....t.........#",
                     "#.........E........#",
                     "#..............BB..#",
                     "#..PP....BB........#",
                     "#..PA....BB....t...#",
                     "#..PP..............#",
                     "#BB......BB....BBB.#",
                     "####################")
                .legend('t', "thug").legend('c', "cultist").extra("tavern_brawler");

        c.encounter("bridge").title("Battle of the Old Stone Bridge").backdrop("bridge").music("goblin").objective("defeat:snikkit")
                .intro("Goblins shriek and scramble over the barricade. Arrows hiss out of the reeds. Defeat Snikkit and the rest will scatter!")
                .surpriseIf("goblins_spotted")
                .map("TTTT,,,~~~~~~~~~,,,TTTTT",
                     "T,,,,,,~~~~~~~~~,,,,,TTT",
                     ",,,,,,,~~~~~~~~~,,,g,,TT",
                     ",PP....=========..B.s.,,",
                     ",PP....=========..B.g.,T",
                     ",,,,,,,~~~~~~~~~,,,a,,,T",
                     "T,,^,,,~~~~~~~~~,,,,,,,T",
                     "TT,,,,,~~~~~~~~~,,,E,,TT",
                     "TTTT,,,~~~~~~~~~,,,,TTTT")
                .legend('s', "snikkit").legend('g', "goblin").legend('a', "goblin_archer").extra("goblin");

        c.encounter("fen").title("The Drowned Shrine").backdrop("swamp").music("eerie")
                .intro("Bloated, weed-wrapped corpses haul themselves out of the black water, and a Fen Lantern drifts above them, glowing with hunger.")
                .map("TT,,~~,,,T,,,~~~,,TTTT",
                     "T,,,~~~,,,,z,,~~,,,,TT",
                     ",,P,,~,,,TT,,,,,,w,,,T",
                     ",PP,,,,,,TT,,~~~,,,,,,",
                     ",,P,,,^,,,,,,~~~,,z,,,",
                     ",,,,,~~~,,,,,,,,,,,E,T",
                     "T,,,,~~~~,,f,,,TT,,,,T",
                     "TT,,,,~~,,,,,,,TT,,,TT",
                     "TTT,,,,,,,,,,~~,,,,TTT")
                .legend('z', "bog_zombie").legend('w', "will_o_wisp").legend('f', "giant_frog").extra("bog_zombie");

        c.encounter("wolves").title("Eyes in the Dark").backdrop("camp").music("battle")
                .intro("The pack closes in around your campfire. Wolves fight together — don't let them surround you! (Shove them into the fire for extra damage.)")
                .surpriseIf("wolves_spotted")
                .map("TTTTTTT,,,,TTTTTTTTT",
                     "TTT,,,,,,,,,,,,,,wTT",
                     "TT,,,,,,,,,,,,,,,,,T",
                     "T,,w,,,,....,,,,,,,T",
                     "T,,,,,,..PP..,,,,,,T",
                     ",,,,,,,..PLP.,,,,,,,",
                     "T,,,,,,..PP..,,,,d,T",
                     "T,,,,,,,....,,,,,,,T",
                     "TT,,,,,,,,,,,,,,,,TT",
                     "TTw,,,,,,,,,,,,,E,TT",
                     "TTTTTTTTT,,,TTTTTTTT")
                .legend('w', "wolf").legend('d', "dire_wolf").extra("wolf");

        c.encounter("wolves_small").title("Ashmaw").backdrop("camp").music("battle")
                .intro("Ashmaw stalks out of the burning brush, its loyal packmate at its side.")
                .map("TTTTTTT,,,,TTTTTTTTT",
                     "TTT,,,,,,,,,,,,,,,TT",
                     "TT,,,,,,,,,,,,,,,,,T",
                     "T,,,,,,,....,,,,,,,T",
                     "T,,,,,,..PP..,,,,,,T",
                     ",,,,,,,..PLP.,,,w,,,",
                     "T,,,,,,..PP..,,,,d,T",
                     "T,,,,,,,....,,,,,,,T",
                     "TT,,,,,,,,,,,,,,,,TT",
                     "TT,,,,,,,,,,,,,,E,TT",
                     "TTTTTTTTT,,,TTTTTTTT")
                .legend('w', "wolf").legend('d', "dire_wolf").extra("wolf");

        c.encounter("chapel").title("The Choir at Prayer").backdrop("chapel").music("battle")
                .intro("The zealot raises his knife and the acolytes rise from their knees, hymns turning to snarls.")
                .surpriseIf("chapel_surprise")
                .map("####################",
                     "#..c....#####....c.#",
                     "#.......#...#......#",
                     "#..BBB..#.z.#..BBB.#",
                     "#.......##.##......#",
                     "#..BBB.........BBB.#",
                     "#.........t........#",
                     "#..BBB....E....BBB.#",
                     "#..................#",
                     "#......PP..PP......#",
                     "#########..#########")
                .legend('z', "cult_fanatic").legend('c', "cultist").legend('t', "thug").extra("cultist");

        c.encounter("gate").title("The Gates of Cinderdeep").backdrop("ruins").music("battle")
                .intro("Rusted swords rasp free. The skeletons' eye-fires flare as they advance, and an acolyte on the rubble begins to chant.")
                .map("######################",
                     "#....s....#####...s..#",
                     "#..B......#...#......#",
                     "#.....B...#.c.#...B..#",
                     "#.........##.##......#",
                     "#..s.....,,,,,....B..#",
                     "#......,,,,^,,,,.....#",
                     "#..B...,,,,,,,,,..E..#",
                     "#........B...........#",
                     "#........PP.PP.......#",
                     "##########....########")
                .legend('s', "skeleton").legend('c', "cultist").extra("skeleton");

        c.encounter("warden").title("The Last Guardian").backdrop("hall").music("battle")
                .intro("Ithrael's Warden lifts its sword. Cult-raised skeletons rise at its sides. (Bludgeoning damage shatters skeletons!)")
                .map("####################",
                     "#.........a........#",
                     "#..#....#..#....#..#",
                     "#..................#",
                     "#..#..s.#..#.s..#..#",
                     "#.........E........#",
                     "#..#....#..#....#..#",
                     "#..................#",
                     "#..#....PPPP....#..#",
                     "#..................#",
                     "#########..#########")
                .legend('a', "animated_armor").legend('s', "skeleton").extra("skeleton");

        c.encounter("cindrel").title("Voice of the Choir").backdrop("lair").music("cindrel").objective("defeat:cindrel")
                .intro("Sister Cindrel's hymn becomes a battle-song. Silence her and the Choir breaks!")
                .surpriseIf("cindrel_surprised")
                .map("######################",
                     "#LLLL.....c.....LLLLL#",
                     "#LL......#..#......LL#",
                     "#L.......#C.#.......L#",
                     "#....m...............#",
                     "#.....c....E....c....#",
                     "#..........B.........#",
                     "#....,,,.......,,,...#",
                     "#....................#",
                     "#.......PP...PP......#",
                     "#########......#######")
                .legend('C', "cindrel").legend('c', "cultist").legend('m', "magma_mephit").extra("magma_mephit");

        c.encounter("boss").title("Vharkas, the Cinder Tyrant").backdrop("lair").music("boss").objective("defeat:vharkas")
                .intro("VHARKAS rises from the magma. Three Binding Braziers feed him strength — while they burn, he regenerates (they're immune to fire!). He is VULNERABLE TO COLD. Watch the glowing cracks: eruptions follow!")
                .surpriseIf("boss_surprised")
                .weakenIf("heartstone")
                .ally("cindrel_ally", "cindrel_ally")
                .phase(60, "Vharkas roars and the chamber floor splits open — magma begins to ERUPT!", Json.obj("op", "lava", "count", 3),
                        Json.obj("op", "say", "text", "\"YOU DARE WOUND ME? I WILL DROWN THIS MOUNTAIN IN FIRE!\""))
                .phase(30, "Molten imps claw their way out of the lake to defend their master!", Json.obj("op", "summon", "monster", "magma_mephit", "count", 2),
                        Json.obj("op", "say", "text", "\"Three hundred years… I will not… go back… into the DARK!\""))
                .map("##########################",
                     "#LLL......x.......x...LLL#",
                     "#LL....................LL#",
                     "#L.......,,,....,,,.....L#",
                     "#............v...........#",
                     "#.......................E#",
                     "#x.......................#",
                     "#....LLLLL.......LLLLL...#",
                     "#....LLLLL.......LLLLL...#",
                     "#........................#",
                     "#.....,,,..........,,,...#",
                     "#........................#",
                     "#........PP....PP........#",
                     "#........PA....AP........#",
                     "##########......##########")
                .legend('v', "vharkas").legend('x', "brazier").legend('m', "magma_mephit").extra("magma_mephit");
    }
}
