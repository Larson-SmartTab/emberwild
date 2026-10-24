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
 * THE DROWNED CROWN — a salt-stained seafaring tale for Emberwild, written with the same Java DSL as the core campaign.
 * <pre>
 *  Prologue   The Barnacle &amp; Bell ....... a shanty contest, crab racing, arm-wrestling, a fortune-teller, a smuggler brawl
 *  Chapter 1  The Light on Gull Rock ..... the drowned dead at low tide, a trapped stair, the keeper's ghost
 *  Chapter 2  The Weeping Caves .......... tide-pool traps, giant crabs, merfolk scouts OR a sahuagin war-party, a sunken shrine
 *  Chapter 3  The Widow's Wake ........... boarding a ghost ship in the fog, a merrow on deck, a trapped captain's cabin
 *  Finale     The Drowned Palace ......... Mother Brine, the sea hag queen (coral idols, crushing tides, drowned sailors)
 * </pre>
 * Eight possible battles, five booby traps, 50 scenes, dozens of skill checks and spell solutions.
 * Levels 1 → 3 by milestone. Two allies can be earned for the final battle.
 * <p>
 * Copy freely: every scene is a self-contained block, so you can lift one into your own tale and rewire its exits.
 */
public final class DrownedCrown implements CampaignScript {

    // Chapter labels, kept in one place so renaming a chapter is a one-line change.
    static final String PRO = "Prologue";
    static final String CH1 = "Chapter 1 — The Light on Gull Rock";
    static final String CH2 = "Chapter 2 — The Weeping Caves";
    static final String CH3 = "Chapter 3 — The Widow's Wake";
    static final String FIN = "Finale";

    @Override
    public Campaign build() {
        Campaign c = Campaign.create("drowned_crown", "The Drowned Crown", "The Emberwild Team");
        c.cover("coast");
        c.synopsis("A green fog rolls in off the sea, and the fishing port of Gullhaven is losing ships — eleven crews in a month, "
                + "vanished without a scream. At low tide the drowned dead walk the beach, and the old lighthouse on Gull Rock has gone dark. "
                + "From a shanty-roaring tavern to a haunted lighthouse, smugglers' sea caves and the deck of a ghost ship, a band of new heroes "
                + "must dive to the drowned palace of Mother Brine, the sea hag who wears the cursed crown of a long-dead pirate queen. "
                + "Built for 1-4 players starting at level 1 (rising to level 3); companions fill a small party.");
        // Other tales can send their heroes here from their victory feast.
        c.hook("epilogue", "A sea captain at the feast is looking for brave hands — something has stolen the sea's crown", "start");

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
        // ---- The star: a storm-chasing dragonborn wizard who breathes lightning (Mother Brine's bane!).
        Hero vessa = hero("vessa", Json.obj(
                "name", "Vessa Stormscale", "pronouns", "she/her", "class", "wizard", "species", "dragonborn", "lineage", "blue", "background", "sage",
                "method", "pointbuy", "scores", scores(8, 14, 13, 15, 12, 10), "increases", Json.obj("int", 2, "con", 1),
                "skills", List.of("nature", "investigation"),
                "cantrips", List.of("ray_of_frost", "shocking_grasp", "minor_illusion"),
                "spells", List.of("mage_armor", "shield", "chromatic_orb", "charm_person"),
                "color", "#2e86de", "emblem", "⚡", "hair", "#1b2a3a", "skin", "#3c6e9f", "eyes", "#f7d84a",
                "appearance", "Sapphire scales that crackle with static in damp weather, a salt-crusted naval greatcoat with brass buttons she polishes when nervous, and a glass jar on her belt with a tiny thunderstorm inside it.",
                "personality", "Brilliant, blunt and boundlessly curious. Talks to the weather like an old friend, keeps meticulous notes on everything, and gets very excited about clouds.",
                "backstory", "Raised in a lighthouse library on the Stormcoast, Vessa spent her childhood charting the squalls that wrecked ships on her doorstep. When the reports came of a green fog off Gullhaven that obeyed no wind she had ever catalogued, she packed her notebooks, her storm-jar and a very large umbrella.",
                "motto", "Every storm has a pattern. I am the pattern's worst nightmare."));
        vessa.spellbook.clear();
        vessa.spellbook.addAll(List.of("mage_armor", "shield", "chromatic_orb", "charm_person", "thunderwave", "ice_knife"));
        c.pregen(vessa);

        // ---- Extra heroes teammates can claim with one click.
        c.pregen(hero("finn", Json.obj(
                "name", "Finn Brackwater", "pronouns", "he/him", "class", "rogue", "species", "halfling", "lineage", "lightfoot", "background", "criminal",
                "method", "pointbuy", "scores", scores(8, 15, 14, 10, 12, 13), "increases", Json.obj("dex", 2, "con", 1),
                "skills", List.of("perception", "deception", "persuasion", "investigation"),
                "color", "#16a085", "emblem", "🦑", "hair", "#c97c3a", "skin", "#e3b48a", "eyes", "#2ecc71",
                "appearance", "Three feet of freckles, a tricorn hat two sizes too big, and a coat lined with secret pockets that smell faintly of rum and kippers.",
                "personality", "An ex-smuggler who insists he's gone straight. Mostly straight. Straight-ish. Knows every hidey-hole on the coast and everyone who owes him money.",
                "motto", "I don't steal. I re-home.")));
        c.pregen(hero("calypso", Json.obj(
                "name", "Calypso Vey", "pronouns", "she/her", "class", "paladin", "species", "tiefling", "lineage", "infernal", "background", "entertainer",
                "method", "pointbuy", "scores", scores(15, 10, 13, 8, 10, 15), "increases", Json.obj("str", 2, "cha", 1),
                "skills", List.of("persuasion", "athletics"),
                "spells", List.of("cure_wounds", "bless"),
                "color", "#c0392b", "emblem", "🎶", "hair", "#2c1a3a", "skin", "#a8433f", "eyes", "#ffcf40",
                "appearance", "Curling horns hung with tiny brass bells, a voice that carries over a gale, a conch-shell shield, and a battered concertina strapped to her back that she calls 'the Duchess'.",
                "personality", "A shanty-singer who was shipwrecked at sixteen, floated three days on a barrel, and swore an oath to the Lady of the Tides when a dolphin pushed her ashore. Sings while she fights. Fights while she sings. Never met a crowd she couldn't win.",
                "motto", "If you can't sing it, it didn't happen.")));

        // ---- Companions: AI-controlled, they join only when the party is small.
        Hero grunda = hero("grunda", Json.obj(
                "name", "Grunda Hullbreaker", "pronouns", "she/her", "class", "fighter", "species", "orc", "background", "soldier",
                "method", "pointbuy", "scores", scores(15, 13, 14, 8, 12, 10), "increases", Json.obj("str", 2, "con", 1),
                "skills", List.of("athletics", "perception"),
                "color", "#5d7a3a", "emblem", "⚓", "hair", "#1e1e1e", "skin", "#7a9a5e", "eyes", "#c0392b",
                "appearance", "A broad-shouldered orc in a tar-stained bosun's coat, an anchor tattooed on each forearm and a boarding axe she calls 'Gran'.",
                "personality", "Bosun of the lost ship Gannet — she broke her leg the week before it sailed into the fog without her. Gruff as a gale, loyal as the tide, and quietly furious with herself.",
                "motto", "Haul together or drown alone."));
        grunda.npc = true;
        c.pregen(grunda);
        Hero pell = hero("pell", Json.obj(
                "name", "Pell Merriwake", "pronouns", "he/him", "class", "cleric", "species", "halfling", "lineage", "lightfoot", "background", "acolyte",
                "method", "pointbuy", "scores", scores(12, 14, 13, 8, 15, 10), "increases", Json.obj("wis", 2, "cha", 1),
                "skills", List.of("medicine", "persuasion"),
                "cantrips", List.of("sacred_flame", "toll_the_dead", "guidance"),
                "spells", List.of("healing_word", "guiding_bolt", "bless", "cure_wounds"),
                "color", "#48c9b0", "emblem", "🐚", "hair", "#e8d8b0", "skin", "#f0c9a8", "eyes", "#2471a3",
                "appearance", "A round-cheeked halfling priest in sea-green vestments, a conch-shell holy symbol around his neck and sand permanently in his sandals.",
                "personality", "Priest of Marenne, Lady of the Tides. Relentlessly cheerful, terrified of deep water, and determined not to let that stop him.",
                "motto", "The tide always turns. Sometimes it needs a push."));
        pell.npc = true;
        c.pregen(pell);
    }

    // =========================================================================================== monsters
    static void monsters(Campaign c) {
        // ---- Prologue: the Saltjack smugglers and an ally for the brawl
        c.monster("dc_smuggler", "Saltjack Smuggler").type("humanoid").role("melee").ac(12).hp(12).stats(13, 12, 12, 10, 10, 10).cr(0.125, 25)
                .attack("Belaying Pin", 3, "1d6+1", "bludgeoning").attack(new Monsters.Attack("Thrown Bottle", 3, "1d4+1", "bludgeoning").ranged(20, 60))
                .look("#7f8c8d", "🏴").blurb("Smells of tar, cheap rum and bad decisions.");
        c.monster("dc_jory", "Gentleman Jory Thrupp").type("humanoid").role("melee").ac(13).hp(24).stats(12, 15, 12, 12, 10, 14).cr(1, 200)
                .multi(2).attack("Brass Knuckles", 4, "1d4+2", "bludgeoning").attack(new Monsters.Attack("Hurled Tankard", 4, "1d4+2", "bludgeoning").ranged(20, 60))
                .trait("nimble_escape").look("#8e44ad", "🎩").blurb("Captain of the Saltjacks. Silk cravat, gold tooth, no conscience.");
        c.monster("dc_mags", "One-Eye Mags").type("humanoid").role("brute").ac(12).hp(24).stats(17, 11, 15, 10, 12, 12).cr(0.5, 0)
                .attack("Fist Like an Anchor", 5, "1d6+3", "bludgeoning").look("#d35400", "🏴‍☠️").blurb("Wharf-mistress of the South Quay — on your side!");

        // ---- The drowned and the deep
        c.monster("dc_drowned", "Drowned Sailor").type("undead").role("brute").ac(10).hp(16).speed(25).stats(14, 8, 15, 3, 6, 5).cr(0.5, 100)
                .resist("cold").immune("poison").condImmune("poisoned", "charmed", "frightened")
                .attack(new Monsters.Attack("Waterlogged Grip", 4, "1d8+2", "bludgeoning").rider("grappled", Ability.STR, 11))
                .trait("undead_fortitude").look("#3d6b6b", "🧟").blurb("Kelp in its hair, crabs in its pockets, and the sea still running from its mouth.");
        c.monster("dc_keeper_ghost", "Amos Fell, the Drowned Keeper").type("undead").role("caster").ac(13).hp(44).speed(30).fly(30).stats(7, 14, 12, 11, 12, 15).cr(2, 450)
                .resist("cold", "necrotic").immune("poison").condImmune("poisoned", "charmed", "grappled", "prone")
                .multi(2).attack(new Monsters.Attack("Withering Touch", 4, "2d6", "necrotic"))
                .attack(new Monsters.Attack("Lantern Glare", 4, "2d4", "radiant").ranged(60, 60).spell())
                .special(Monsters.special("keeper_wail", "Keeper's Wail").shape("sphere", 10, 0).dmg("2d6", "psychic").dc(12, Ability.WIS).recharge(5).rider("frightened"))
                .look("#a9cce3", "👻").blurb("A grey old man in a sou'wester, his lantern burning with cold blue light.");
        c.monster("dc_snapper", "Grandfather Snapper").type("beast").role("brute").size(2).ac(15).hp(38).speed(30).stats(17, 11, 15, 1, 10, 3).cr(2, 450)
                .multi(2).attack(new Monsters.Attack("Great Claw", 5, "1d8+3", "bludgeoning").rider("grappled", Ability.STR, 13))
                .look("#e74c3c", "🦀").blurb("A crab the size of a rowing boat, its shell crusted with a century of barnacles.");
        c.monster("dc_tide_priestess", "Sahuagin Tide-Priestess").type("fiend").role("caster").ac(12).hp(18).stats(12, 12, 12, 12, 14, 13).cr(1, 200)
                .attack("Shark-Tooth Staff", 3, "1d6+1", "bludgeoning")
                .attack(new Monsters.Attack("Brine Bolt", 4, "2d6", "cold").ranged(60, 60).spell())
                .special(Monsters.special("blood_hymn", "Blood Hymn").shape("heal", 0, 0).dmg("2d6+2", "").uses(1).bonus())
                .trait("pack_tactics").look("#117a65", "🦈").blurb("She sings to the sharks, and the sharks sing back.");

        // ---- Earned allies
        c.monster("dc_keeper_ally", "Amos Fell, Keeper of the Light").type("undead").role("caster").ac(13).hp(30).fly(30).stats(7, 14, 12, 11, 14, 15).cr(2, 0)
                .resist("cold", "necrotic").immune("poison")
                .attack(new Monsters.Attack("Beacon Ray", 5, "2d8", "radiant").ranged(60, 60).spell())
                .special(Monsters.special("guiding_light", "Guiding Light").shape("heal", 0, 0).dmg("2d6+3", "").uses(2).bonus())
                .look("#f9e79f", "🏮").blurb("He kept the light for two hundred years. Tonight he keeps it for you.");
        c.monster("dc_merfolk_ally", "Ondine Kelpbraid").type("humanoid").role("melee").ac(13).hp(28).speed(30).stats(14, 15, 13, 11, 13, 12).cr(1, 0)
                .multi(2).attack(new Monsters.Attack("Coral Trident", 5, "1d8+2", "piercing").reach(10))
                .attack(new Monsters.Attack("Thrown Trident", 5, "1d8+2", "piercing").ranged(20, 60))
                .look("#1abc9c", "🧜").blurb("A merfolk scout with a sharp tongue and a sharper trident. Fighting for her reef.");

        // ---- The ghost ship
        c.monster("dc_merrow", "Barnaby Wrack, the Merrow Bosun").type("monstrosity").role("brute").size(2).ac(13).hp(40).speed(30).stats(18, 10, 15, 8, 10, 9).cr(2, 450)
                .multi(2).attack(new Monsters.Attack("Bite", 5, "1d8+4", "piercing"))
                .attack(new Monsters.Attack("Harpoon", 5, "2d6+4", "piercing").reach(10).rider("pushed", Ability.STR, 13))
                .look("#2e6b5e", "🐟").blurb("A sea ogre in a dead man's bosun's coat, swinging a rusted harpoon.");

        // ---- The boss and her idols
        c.monster("dc_mother_brine", "Mother Brine, the Drowned Queen").type("fey").role("boss").size(2).ac(14).hp(76).speed(30)
                .stats(18, 13, 16, 13, 14, 16).cr(4, 1100).saves("con", "wis", "cha")
                .resist("cold").vulnerable("lightning").immune("poison").condImmune("charmed", "frightened", "poisoned")
                .multi(2)
                .attack(new Monsters.Attack("Barnacled Claw", 6, "1d6+4", "slashing"))
                .attack(new Monsters.Attack("Brine Lash", 6, "2d6", "cold").ranged(60, 60).spell())
                .special(Monsters.special("tide_surge", "Tide Surge").shape("sphere", 10, 0).dmg("2d6+1", "bludgeoning").dc(13, Ability.STR).recharge(5).rider("prone"))
                .special(Monsters.special("undertow", "Undertow").shape("cone", 15, 0).dmg("1d8", "cold").dc(13, Ability.STR).recharge(6).rider("slowed"))
                .trait("fed_by_objects", "legendary_resistance")
                .legendary(2, "attack:Barnacled Claw", "move")
                .look("#16a085", "👑").blurb("A sea hag grown vast on drowned sailors' dreams, the pirate queen's cursed crown sunk into her weedy brow.");
        c.monster("dc_coral_idol", "Coral Idol").type("object").role("object").ac(11).hp(10).speed(0).stats(10, 1, 10, 1, 1, 1).cr(0, 25)
                .immune("cold", "poison", "psychic").condImmune("charmed", "frightened", "poisoned", "paralyzed", "prone", "incapacitated")
                .attack("None", 0, "0", "bludgeoning").look("#ff7f9f", "🪸").blurb("A pink coral totem humming with stolen breath. While it stands, the Queen heals. Smash it!");
    }

    // =========================================================================================== items
    static void items(Campaign c) {
        c.item("dc_stormglass", "Keeper's Stormglass", "wondrous", "Amos Fell's brass storm-barometer. Its needle points at danger. +1 to spell attack rolls and +1 initiative.",
                Json.obj("bonus", Json.obj("spell_attack", 1, "initiative", 1), "attunable", true, "value", 400));
        c.item("dc_tide_pearl", "Tidecaller Pearl", "wondrous", "A black pearl gifted by the merfolk of the Hagstooth Reef. +1 to Armor Class and Constitution saves.",
                Json.obj("bonus", Json.obj("ac", 1, "con_save", 1), "attunable", true, "value", 500));
        c.item("dc_keeper_log", "The Keeper's Log", "quest", "Amos Fell's logbook. The last entry, two hundred years old, describes the night the Widow's Wake sank.", null);
        c.item("dc_jory_ledger", "Jory's Ledger", "quest", "The Saltjacks' account book: eleven crews 'delivered to the Widow' for drowned gold.", null);
        c.item("dc_widow_key", "The Widow's Key", "quest", "A brass key shaped like a seahorse, taken from the captain's cabin of the Widow's Wake.", null);
    }

    // =========================================================================================== prologue
    static void prologue(Campaign c) {
        c.scene("start").chapter(PRO).title("Gullhaven Harbour").backdrop("coast").weather("fog").time("dusk").music("sea")
                .text("The fishing port of GULLHAVEN clings to the cliffs like a barnacle to a hull: crooked houses painted every colour of the sea, gulls arguing on every chimney, and nets drying on every rail. It ought to be beautiful. It almost is.",
                        "But out past the harbour mouth, a fog sits on the water like a sulking cat. It is *green* — the sickly, glowing green of rotten seaweed — and it does not move with the wind. Fishing boats sit idle at their moorings. Nobody is going out. Not any more.",
                        "On the headland, the lighthouse on GULL ROCK stands dark against the dusk. Down at the quay, light and noise spill from the doorway of a tavern whose sign shows a barnacled ship's bell: THE BARNACLE & BELL.")
                .choice("Head for the warm lights of the Barnacle & Bell").icon("🍺").go("tavern_arrive")
                .choice("Study the strange green fog first").icon("🌫").check("arcana", 12)
                    .pass("You lick a finger and hold it to the air. The wind blows east. The fog drifts *west*. Up close, the mist tastes of salt and old pennies, and somewhere inside it you hear — very faintly — a choir of drowned voices humming a sea-shanty in a minor key. This isn't weather. Something is *breathing* it.", "tavern_arrive", flag("fog_read"), xp(25))
                    .fail("It's fog. It's green. It smells of low tide and despair. You decide the tavern is a much better place to think.", "tavern_arrive");

        c.scene("tavern_arrive").chapter(PRO).title("The Barnacle & Bell").backdrop("tavern").time("night").weather("none").music("shanty")
                .onEnter(companion("grunda", 3), companion("pell", 3))
                .npc("Dorrie Bellweather", "Landlady of the Barnacle & Bell", "#e67e22", "🔔", "Close the door, the fog gets in the beer! Sit down, sit down — first chowder's free if you look half-drowned, and you all do.")
                .text("The Barnacle & Bell is a ship's hull turned upside down and nailed to the quay — ribs for rafters, portholes for windows, and a great bronze ship's bell hanging over the bar. DORRIE BELLWEATHER, a gnome with forearms like hams and a voice like a foghorn, rings it every time someone buys a round. It rings a lot.",
                        "The place is heaving. A trio of bearded fishermen are tuning up for a shanty contest, children are shrieking over a sandy track where crabs race for coppers, and a one-eyed woman built like a mooring post is slamming challengers' arms onto a barrel. In a curtained alcove, a fortune-teller with sea-glass eyes shuffles a deck of cards.",
                        "Not everyone is merry. A broad orc in a tar-stained bosun's coat — GRUNDA HULLBREAKER — sits alone, glaring at the fog through a porthole. A round little halfling priest, PELL MERRIWAKE, is lighting candles in tiny paper boats for the missing crews. And by the far window, a young deckhand with a bandaged head flinches every time the door opens.",
                        "(If your party is small, Grunda and Pell throw in with you. \"Somebody's got to keep you lot afloat,\" growls the orc.)")
                .choice("Find a seat and take in the room").icon("🪑").go("tavern");

        c.scene("tavern").chapter(PRO).title("The Common Room").backdrop("tavern").time("night").music("shanty").anchor("tavern")
                .text("The bell clangs, the fiddles saw, and the Barnacle & Bell roars around you like a friendly sea. There is plenty to do on a night like this — and under the laughter, every so often, people glance at the green glow in the portholes and go quiet.")
                .choice("Talk to Dorrie at the bar").icon("🔔").unlessFlag("dorrie_done").go("bar")
                .choice("Enter the shanty contest").icon("🎵").unlessFlag("shanty_done").go("shanty")
                .choice("Bet on the crab races").icon("🦀").unlessFlag("crabs_done").go("crabs")
                .choice("Arm-wrestle the one-eyed wharf-mistress").icon("💪").unlessFlag("mags_done").go("mags")
                .choice("Visit the fortune-teller's alcove").icon("🔮").unlessFlag("zelloe_done").go("zelloe")
                .choice("Check on the frightened young deckhand").icon("🩹").unlessFlag("wick_met").hint("Something tells you this is where the trouble starts.").go("wick")
                .choice("Hear out Harbourmaster Odessa Vane").icon("⚓").ifFlag("brawl_done").unlessFlag("quest_accepted").go("odessa")
                .choice("Turn in for the night").icon("🌙").ifFlag("quest_accepted").go("tavern_night");

        // ---- Dorrie
        c.scene("bar").chapter(PRO).title("Dorrie's Bar").backdrop("tavern").time("night").music("shanty")
                .npc("Dorrie Bellweather", "Landlady of the Barnacle & Bell", "#e67e22", "🔔", "Eleven ships, love. Eleven crews in one month. And not one scream heard from the fog.")
                .text("Dorrie plonks a steaming bowl of chowder in front of you before you've even sat down. Up close, her cheerful foghorn voice has an edge to it, like a rope about to snap.",
                        "\"First the *Merry Halibut*. Then the *Saucy Maud*. Then the *Gannet* — Odessa's boy Tam was captain of that one.\" She glances at the harbourmaster's empty chair by the fire. \"And now the dead come up the beach at low tide. Not *our* dead. Old dead. Sailors in coats nobody's worn for two hundred years.\"")
                .choice("Ask about the dark lighthouse on Gull Rock").icon("🗼").once()
                    .effect(flag("rumor_lighthouse"), log("Dorrie: \"Gull Rock's been dark since the fog came. Keeper's cottage is empty. Folk say old Amos Fell walks up there — the first keeper, two hundred years dead. Says he's angry. Says he's *sorry*. Ghosts are never consistent.\""))
                    .go("bar")
                .choice("Charm her into sharing the real gossip").icon("💗").once().check("persuasion", 12)
                    .pass("Dorrie snorts, then leans in, lowering her foghorn to a mere bellow. \"Jory Thrupp's Saltjacks. Smugglers. Every one of the lost ships, they were seen rowing out into the fog the night before. And they're paying their bar tab in *gold*. Old gold, green with sea-rot.\" She slides a red bottle across the bar. \"Here. You'll be needing this more than me.\"", "bar", item("potion_healing"), flag("know_saltjacks"), attitude("dorrie", 1))
                    .fail("\"Flattery's tuppence a word, love, and you're over budget.\" Dorrie swats you with a dishrag, but she's smiling.", "bar")
                .choice("Buy a round for the whole house (10 gp)").icon("🍺").once().ifGold(10)
                    .effect(gold(-10), inspiration(), attitude("dorrie", 1), log("CLANG! CLANG! CLANG! Dorrie rings the bell like it owes her money and the whole tavern roars your name. Someone starts a shanty about you on the spot. It's terrible. You love it. (Heroic Inspiration!)"))
                    .go("bar")
                .choice("Head back into the common room").icon("↩").effect(flag("dorrie_done")).go("tavern");

        // ---- The shanty contest
        c.scene("shanty").chapter(PRO).title("The Shanty Contest").backdrop("tavern").time("night").music("shanty")
                .npc("The Fenwick Brothers", "Three-Time Shanty Champions", "#2980b9", "🎻", "Ho there, landlubber! Think ye can out-sing the Fenwicks? Nobody out-sings the Fenwicks!")
                .text("The FENWICK BROTHERS — Ned, Ted and Little Jed (who is the largest man you have ever seen) — have won the Barnacle & Bell's shanty cup three years running. They have just finished *\"The Drunken Cod\"* to thunderous stamping, and now they're grinning at you with a challenge in their beards.",
                        "The prize is a silver cup, five gold and — more importantly — bragging rights for a year. The whole tavern turns to see what you've got.")
                .choice("Belt out a rousing shanty of your own").icon("🎵").check("performance", 13)
                    .pass("You climb onto a table and let rip with *\"Haul Away for Gullhaven\"*. By the second verse the whole tavern is stamping; by the third, the Fenwicks are singing harmony. Ned wipes a tear. \"Champion,\" he says hoarsely, and hands you the cup.", "tavern", inspiration(), gold(5), flag("shanty_won"), flag("shanty_done"))
                    .fail("You start strong. Then you forget the words, invent new ones, and accidentally rhyme 'mermaid' with 'unpaid'. The Fenwicks win, but they buy you a drink for sheer nerve.", "tavern", flag("shanty_done"))
                .choice("Put an illusory crab in Ted's beard mid-verse (Minor Illusion)").icon("😈").spell("minor_illusion").check("deception", 11)
                    .pass("A tiny illusory crab peeks out of Ted's beard and waves a claw at the audience. Ted's voice cracks on the high note. Little Jed starts giggling. The Fenwicks collapse into helpless laughter mid-verse, and the judges — Dorrie and a very old parrot — award you the cup on the spot.", "tavern", gold(5), flag("shanty_won"), flag("shanty_done"))
                    .fail("Your insult lands... on the parrot. The parrot is a judge. The parrot never forgets. The Fenwicks win by a beak.", "tavern", flag("shanty_done"))
                .choice("Ask the Fenwicks for the oldest shanty they know").icon("📜").check("history", 12)
                    .pass("Ned's face goes serious. \"There's one we don't sing. *The Widow's Lament.*\" He hums it low: *'Black sails, black crown, the Widow went down, / Her crown drank the sea and the sea drank the town... / Break the crown with the fire from the sky, / Or every sailor who sails will die.'* \"Pirate queen Morwen Blackwater. Wrecked on Gull Rock, two hundred years back. Some say the crown's still down there.\"", "tavern", flag("know_widow"), xp(25), flag("shanty_done"))
                    .fail("\"Old shanties? That's all old Barnaby knew, and the fog took him with the *Saucy Maud*.\" Ned looks away. The music has gone quiet.", "tavern", flag("shanty_done"))
                .choice("Applaud and move on").icon("↩").effect(flag("shanty_done")).go("tavern");

        // ---- Crab racing
        c.scene("crabs").chapter(PRO).title("The Crab Derby").backdrop("tavern").time("night").music("shanty")
                .npc("Little Nell Kipper", "Bookmaker, Age Nine", "#f1c40f", "🦀", "Place yer bets! Sir Pinchalot, Duchess Clackington or Old Sidewise! Two gold a go, no refunds, no crying!")
                .text("A sandy track has been laid down the middle of the floor between two upturned rowing boats. Three crabs with tiny painted shells wait in a starting gate made of spoons. LITTLE NELL KIPPER, nine years old and already the shrewdest businessperson in Gullhaven, takes bets in a bucket.",
                        "*Sir Pinchalot* is the favourite: big, red and vicious. *Duchess Clackington* has a ribbon on her claw. *Old Sidewise* is missing a leg and seems to be asleep.")
                .choice("Bet 2 gp on Old Sidewise and pray").icon("🍀").ifGold(2).check("luck", 13)
                    .effect(gold(-2))
                    .pass("The spoons lift. Sir Pinchalot and the Duchess immediately start fighting. Old Sidewise wakes up, wanders sideways, wanders *forward* by accident, and crosses the line to a stunned silence and then an almighty cheer. Nell pays out ten-to-one through gritted teeth.", "tavern", gold(20), flag("crabs_done"), flag("crab_luck"))
                    .fail("Old Sidewise stays asleep. The Duchess wins by a claw. Nell pockets your gold with a sweet smile that is somehow terrifying.", "tavern", flag("crabs_done"))
                .choice("Study the crabs and bet 2 gp on the smart pick").icon("👁").ifGold(2).check("nature", 12)
                    .effect(gold(-2))
                    .pass("Sir Pinchalot's shell is freshly moulted — soft, nervous, slow. The Duchess has eaten recently. You put your money on the ribbon. She wins by a whisker (crabs don't have whiskers, but still). Nell narrows her eyes at you. \"You're good.\"", "tavern", gold(6), flag("crabs_done"))
                    .fail("You pick Sir Pinchalot for his strong claw-work. Sir Pinchalot uses that claw-work on your finger.", "tavern", flag("crabs_done"))
                .choice("Give Old Sidewise a little help (Minor Illusion)").icon("✨").spell("minor_illusion").check("sleight_of_hand", 12)
                    .pass("A tiny illusory fish appears just ahead of Old Sidewise's nose. He chases it with the grim focus of a true athlete and wins by a mile. Nell spots the shimmer but grins. \"That's cheating. I *like* cheating. Here.\" She pays you anyway and names you an honorary bookie.", "tavern", gold(10), flag("crabs_done"), flag("nell_friend"))
                    .fail("The illusory fish appears in front of Sir Pinchalot instead. He wins by a mile. Nell laughs so hard she falls off her stool.", "tavern", flag("crabs_done"))
                .choice("Just watch and cheer").icon("↩").go("tavern");

        // ---- Arm-wrestling One-Eye Mags
        c.scene("mags").chapter(PRO).title("The Barrel of Broken Elbows").backdrop("tavern").time("night").music("shanty")
                .npc("One-Eye Mags", "Wharf-Mistress of the South Quay", "#d35400", "🏴‍☠️", "Lost the eye to a shark. Shark lost more. You want to try your luck, minnow?")
                .text("ONE-EYE MAGS runs the South Quay like a pirate queen runs a deck. Her eyepatch is embroidered with a tiny anchor, her forearms are covered in tattoos of every ship she's ever unloaded, and the barrel in front of her is scarred with the elbow-dents of a hundred beaten challengers.",
                        "A chalk sign reads: *BEAT MAGS — 15 GOLD. LOSE — HAUL CRATES TILL DAWN.* Nobody has hauled crates till dawn in years. Mags lets them off. She's soft like that.")
                .choice("Grip her hand and wrestle fair and square").icon("💪").check("athletics", 14)
                    .pass("It's like pulling a ship off a sandbar. Veins stand out on both your necks. Then — slowly, impossibly — her knuckles touch wood. The tavern explodes. Mags stares at her hand, then roars with laughter and claps you on the back hard enough to rattle your teeth. \"*Finally!* Someone with a spine!\"", "tavern", gold(15), flag("mags_friend"), flag("mags_done"))
                    .fail("Mags pins you gently, almost apologetically, and helps you rub the feeling back into your arm. \"Good grip, minnow. Bit more chowder, you'll beat me next year.\" She's decided she likes you.", "tavern", flag("mags_friend"), flag("mags_done"))
                .choice("Let a spark of lightning settle it (Shocking Grasp)").icon("⚡").spell("shocking_grasp").check("deception", 12)
                    .pass("At the crucial moment a spark crackles from your palm into hers. Mags's arm jerks, folds, and slams down. She squints at you with her one good eye, rubbing her tingling fingers. \"...Eels,\" she says. \"You've got a grip like an *eel*.\" She hands over the gold, deeply impressed and none the wiser.", "tavern", gold(15), flag("mags_friend"), flag("mags_done"))
                    .fail("The spark is a little enthusiastic. Mags's hair stands straight up and her eyepatch smokes. Long silence. Then she bellows with laughter. \"Cheat! *Cheat!* I love it! Drinks on the sparky one!\"", "tavern", gold(-2), flag("mags_friend"), flag("mags_done"))
                .choice("Swap sea-stories instead of elbows").icon("💬").check("persuasion", 12)
                    .pass("You tell her about the worst storm you ever saw. She tells you about the shark. By the end you're both laughing, she's forgotten the wrestling entirely, and she's offered you the use of her skiff, the *Lucky Mackerel*, whenever you need it.", "tavern", flag("mags_friend"), flag("mags_boat"), flag("mags_done"))
                    .fail("Your story is good, but her shark story is better. Everyone's is. Still, she buys you a drink.", "tavern", flag("mags_friend"), flag("mags_done"))
                .choice("Decline politely").icon("↩").go("tavern");

        // ---- The fortune-teller
        c.scene("zelloe").chapter(PRO).title("Madame Zelloe's Alcove").backdrop("tavern").time("night").music("mystery")
                .npc("Madame Zelloe", "Seer of the Seven Tides", "#8e44ad", "🔮", "Sit, little fish. The cards are wet tonight. The cards are always wet when the dead are walking.")
                .text("Behind a curtain of knotted fishing net sits MADAME ZELLOE, a tiny old woman with eyes of milky sea-glass and rings on every finger, including her thumbs. Her cards are painted on oyster shells. She turns three over without looking: *The Drowned Man. The Crown. The Lighthouse.*",
                        "\"The sea asks you a riddle,\" she croaks. \"Answer it, and I'll tell you what the cards are hiding. *I wear a crown of foam but have no head. I have a bed, but never sleep. I swallow ships, but never eat. What am I?*\"")
                .choice("\"The sea.\"").icon("🌊")
                    .effect(flag("zelloe_riddle"), flag("zelloe_done"), xp(25), log("Zelloe cackles and taps the Crown card. \"Clever fish. Hear me: a hag wears a dead queen's crown, and the crown drinks the sea's grief. Her strength grows in pink stone gardens — break the coral, starve the queen. And remember: storm-fire. The crown fears storm-fire.\""))
                    .go("tavern")
                .choice("\"A whale.\"").icon("🐋")
                    .effect(flag("zelloe_done"), log("\"A whale!\" Zelloe wheezes with laughter. \"A whale with a crown and a bed! Oh, I'll dream of that one.\" She pats your hand. \"No, little fish. But keep your eyes on the lighthouse.\""))
                    .go("tavern")
                .choice("\"A drowned king.\"").icon("👑")
                    .effect(flag("zelloe_done"), log("Zelloe's smile fades. \"Close. Too close. Not a king... a *queen*. But that's not the answer.\" She sweeps the cards away and won't say more."))
                    .go("tavern")
                .choice("Read her face for the answer instead").icon("👁").check("insight", 13)
                    .pass("Every time Zelloe says 'swallow ships' her eyes flick to the porthole and the black water beyond. \"The sea,\" you say. She beams. \"Hear me, then: a hag wears a dead queen's crown. Her strength grows in pink stone gardens — break the coral, starve the queen. And storm-fire... the crown fears storm-fire.\"", "tavern", flag("zelloe_riddle"), flag("zelloe_done"), xp(25))
                    .fail("Zelloe's face is as unreadable as a fogbank. She grins toothlessly. \"Guess, little fish. Guess.\"", "zelloe")
                .choice("Make her crystal ball glow and wink (Thaumaturgy)").icon("😈").spell("thaumaturgy")
                    .pass("Your eyes flare ember-red and the crystal ball on the table pulses with light. Zelloe nearly falls off her stool — then laughs, delighted. \"A real one! Ha! Fine, fine, I'll cheat for a fellow professional. The answer's the sea. And the cards say: break the coral to starve the queen, and storm-fire breaks the crown.\"", "tavern", flag("zelloe_riddle"), flag("zelloe_done"));

        // ---- Wick and the brawl
        c.scene("wick").chapter(PRO).title("The Boy Who Came Back").backdrop("tavern").time("night").music("tension")
                .npc("Wick Larrow", "Cabin Lad of the Gannet (Shaken)", "#5d6d7e", "🩹", "I saw them. In the fog. The crew — they were singing, and walking *into the water*...")
                .text("The deckhand is barely sixteen, with a fresh bandage around his head and the thousand-yard stare of someone who has seen something he can't put down. Grunda, across the room, has gone very still. You realise she's watching him too.",
                        "\"I'm Wick. I was cabin boy on the *Gannet*,\" he whispers. \"I'm the only one who came back. They found me on the beach at low tide. Everyone else...\" He swallows. \"And I saw who rowed out to meet us in the fog. Before the singing started.\"")
                .choice("Speak gently and earn his trust").icon("💬").check("persuasion", 11)
                    .pass("Your calm voice anchors him. \"Saltjacks,\" he breathes. \"Jory Thrupp's boat. They rowed out ahead of us with a green lantern and the fog just... followed them. Then a ship came out of the fog. A *dead* ship, with black sails, and a woman's voice singing from under the water. Captain Tam was the first to walk overboard.\"", "brawl_start", flag("wick_met"), flag("wick_trust"))
                    .fail("He shakes his head, lips pressed white. But his eyes keep sliding to the door, and you realise he isn't afraid of *you*.", "brawl_start", flag("wick_met"))
                .choice("Notice who he keeps glancing at").icon("👁").check("insight", 11)
                    .pass("Not the door — the corner table, where four men in salt-stained coats have stopped pretending to drink. One wears a silk cravat and a gold tooth. \"Saltjacks,\" Wick whispers when he sees you've noticed. \"They led us into the fog. They know I saw.\"", "brawl_start", flag("wick_met"), flag("wick_trust"))
                    .fail("Something is badly wrong with him, but before you can work out what, a chair scrapes back at the corner table.", "brawl_start", flag("wick_met"))
                .choice("Calm him with a warm smile and a little magic (Charm Person)").icon("💗").spell("charm_person")
                    .pass("Golden warmth flickers in your eyes and Wick's shoulders unknot. Everything pours out: the Saltjacks' green lantern, the ghost ship with black sails, the singing from under the waves — and a name the smugglers whispered as they rowed away: *'Mother Brine pays in gold, lads. Mother Brine always pays.'*", "brawl_start", flag("wick_met"), flag("wick_trust"), flag("know_brine"));

        c.scene("brawl_start").chapter(PRO).title("A Word Outside, Lad?").backdrop("tavern").time("night").music("tension")
                .npc("Gentleman Jory Thrupp", "Captain of the Saltjacks", "#8e44ad", "🎩", "Evening, friends. The lad's coming for a little walk with us. Down the pier. Won't be long. Won't be back, either.")
                .text("The smugglers rise from the corner table: two broad-shouldered Saltjacks with belaying pins, a wiry one cracking his knuckles, and their captain — GENTLEMAN JORY THRUPP, silk cravat, gold tooth, and a smile like a gutting knife.",
                        "The fiddles stop. Dorrie's hand drifts toward the bell. Wick grips the table so hard his knuckles go white. Grunda, very slowly, puts down her tankard.",
                        "\"No need for unpleasantness,\" says Jory, \"unless you insist.\"")
                .choice("Stand up. \"The lad stays.\" (Tavern brawl — fists, chairs, no killing)").icon("⚔").battle("dock_brawl")
                    .victory("Jory Thrupp goes over the bar in a spray of chowder and doesn't get up. The tavern erupts. Dorrie rings the bell eleven times — one for every lost ship. \"Well,\" she says, surveying the splintered tables. \"That's the furniture budget gone.\"", "after_brawl", flag("brawl_done"), flag("wick_saved"))
                    .defeat("The world tips like a deck in a storm, and goes dark. You wake on the sawdust to Dorrie pressing a cold fish to your forehead. \"They took the lad,\" she says quietly. \"Dragged him out the door. But you gave Jory a black eye he'll remember.\"", "after_brawl", flag("brawl_done"), flag("wick_taken"))
                .choice("Stare down the hired muscle").icon("😠").once().check("intimidation", 13)
                    .pass("\"Is Jory paying you enough to lose your teeth?\" you ask the two big Saltjacks quietly. They exchange a look... and suddenly remember urgent business at the privy. Jory's left with two nervous deckhands and a lot of attitude.", "brawl_small", flag("muscle_fled"))
                    .fail("The Saltjacks just laugh and roll up their sleeves. Somebody throws a chowder bowl. Somebody else throws a Fenwick. One-Eye Mags cracks her knuckles at the bar. It's going to be the hard way.", "brawl_start")
                .choice("Turn your smile on Jory (Charm Person)").icon("💗").spell("charm_person")
                    .pass("Your magic slides over Jory like warm butter. He blinks at you, lowers his fists, and says: \"You know, you seem lovely. Lads — this is a bad idea.\" His crew stares at him in horror, and two of them back away. In the confusion, you strike first.", "brawl_small", flag("brawl_surprise"), flag("muscle_fled"));

        c.scene("brawl_small").chapter(PRO).title("Odds in Your Favour").backdrop("tavern").time("night").music("brawl")
                .text("With his hired muscle gone, Jory's smile wobbles. He pulls on a set of brass knuckles. \"Fine,\" he snarls. \"The hard way.\"")
                .choice("Brawl!").icon("⚔").battle("dock_brawl_small")
                    .victory("Jory's gold tooth goes spinning across the floor and he goes down after it. The Barnacle & Bell explodes into cheers, and the Fenwicks start a song about you on the spot. It rhymes 'Jory' with 'gory'. It's wonderful.", "after_brawl", flag("brawl_done"), flag("wick_saved"))
                    .defeat("Darkness. You wake to Dorrie's cold fish on your forehead. \"They took the lad.\"", "after_brawl", flag("brawl_done"), flag("wick_taken"));

        c.scene("after_brawl").chapter(PRO).title("Aftermath").backdrop("tavern").time("night").music("tavern")
                .text("The town watch — two elderly fishermen with a lantern and a boathook — arrive at a jog, far too late, followed by a tall woman in a salt-faded harbourmaster's coat. Her hair is iron-grey and cropped short; her face looks like it was carved by the wind. She takes in the wreckage, the groaning smugglers, and you.",
                        "\"Odessa Vane,\" she says. \"Harbourmaster. And I think you and I need to talk.\"")
                .choice("Search the groaning smugglers").icon("🔍").once().check("investigation", 10)
                    .pass("In Jory's waistcoat you find a purse of strange gold coins, green with sea-rot and stamped with a crowned skull — and a scrap of oilcloth marked with a map of the Hagstooth Cliffs and the words *WEEPING CAVES — DROP AT LOW TIDE. THE WIDOW COLLECTS.*", "after_brawl", gold(12), flag("cave_map"))
                    .fail("You find a few coppers, a lot of lint and a half-eaten pickled herring. You leave the herring.", "after_brawl", gold(4))
                .choice("Help Dorrie set the tables right").icon("🧹").once()
                    .effect(attitude("dorrie", 2), flag("dorrie_grateful"), log("Dorrie wipes her hands and pats your cheek. \"You lot are all right. Rooms are free tonight, and don't you dare argue.\""))
                    .go("after_brawl")
                .choice("Speak with Odessa Vane").icon("⚓").go("odessa");

        c.scene("odessa").chapter(PRO).title("The Harbourmaster's Plea").backdrop("tavern").time("night").music("sorrow")
                .npc("Odessa Vane", "Harbourmaster of Gullhaven", "#34495e", "⚓", "I've buried a husband to the sea. I won't bury my son to a fog.")
                .text("Odessa sits down heavily by the fire and pours herself a glass of something dark. Her hands are perfectly steady. Her voice isn't.",
                        "\"Eleven crews in a month. My son Tam's among them — captain of the *Gannet*. Everyone says they're drowned. But the dead coming up the beach are *old* dead, two centuries old. None of them are ours.\" She looks at you. \"So where are ours?\"",
                        "\"Gull Rock lighthouse went dark the night the fog came. The keeper's ghost has scared off everyone I've sent. Light it again, find out what's in that fog, and bring my boy home. The harbour can pay a hundred gold apiece. It's every coin we have.\"")
                .choice("\"We'll bring them home.\" Accept the quest").icon("⚓").effect(flag("quest_accepted"), xp(50)).go("tavern")
                .choice("Ask for more support — you've already bled for this town").icon("💬").check("persuasion", 14)
                    .pass("Odessa studies you for a long moment, then nods. \"Fair.\" She unhooks a pair of potions from her belt. \"From the Shrine of the Tides. And another fifty each when you come back. *If* you come back.\"", "tavern", flag("quest_accepted"), flag("reward_up"), item("potion_healing"), item("potion_healing"), xp(50))
                    .fail("\"A hundred is all Gullhaven has,\" she says, without anger. \"But I'll keep a lamp burning in the harbour office for you. For whatever that's worth.\" You accept.", "tavern", flag("quest_accepted"), xp(50));

        c.scene("tavern_night").chapter(PRO).title("A Song Beneath the Waves").backdrop("tavern").time("night").weather("none").music("eerie")
                .onEnter(longRest())
                .text("Dorrie's rooms are tucked under the eaves, all sloping ceilings and patchwork quilts, and the sound of the sea against the quay is the best lullaby in the world.",
                        "In the dream you are sinking, slow as a feather, through green water. Far below, a palace of coral and drowned ships glows like a sunken lantern. A woman's voice sings — sweet, ancient, rotten as old fish — and a crown of black pearls turns slowly in the dark.",
                        "*\"Come down, little sailors,\"* the voice croons. *\"Come down and sleep with me. The sea is so lonely. The sea is so very hungry.\"*",
                        "You wake gasping, your mouth tasting of salt, the morning sun pouring through the porthole — and on the windowsill, a single wet strand of seaweed that wasn't there last night.")
                .choice("Rise and greet the morning").icon("☀").go("market");

        c.scene("market").chapter(PRO).title("The Fish Market").backdrop("town").time("day").music("explore").shop().anchor("town").checkpoint()
                .onEnter(level(2))
                .npc("Hester Quill", "Chandler & Outfitter", "#2e86c1", "🛒", "Off to Gull Rock? Then you'll want potions. And rope. And potions. And did I mention potions?")
                .text("You wake changed. The brawl, the dream, the weight of Odessa's plea — something has settled into you like ballast in a hull, steel and spell alike. **(Every hero reaches level 2!)**",
                        "Gullhaven's fish market is subdued this morning — half the stalls empty, the fishwives talking in low voices. But HESTER QUILL's chandlery is open as always, piled with rope, lanterns, oilskins and racks of glimmering bottles: healing potions, sticky flasks of alchemist's fire, and *Frostbloom Flasks* of everwinter sap.",
                        "Out on the headland, Gull Rock's lighthouse stands dark. The tide goes out at noon.")
                .choice("Visit the Shrine of the Tides for a blessing").icon("🐚").once()
                    .effect(inspiration(), heal("2d8"), log("A priestess of Marenne pours a cup of seawater over your head. It is freezing. \"May the tide carry you home.\" (Heroic Inspiration)"))
                    .go("market")
                .choice("Check on Wick before you go").icon("🩹").once().ifFlag("wick_saved")
                    .effect(flag("cave_map"), log("Wick, wrapped in a blanket and eating his third breakfast, presses a crumpled chart into your hands. \"The Saltjacks' cave. Under the Hagstooth Cliffs, the Weeping Caves. I heard Jory say it. Find my captain. Please.\""))
                    .go("market")
                .choice("Ask Mags to lend you her skiff").icon("🚣").once().ifFlag("mags_friend")
                    .effect(flag("mags_boat"), log("Mags tosses you a pair of oars. \"The *Lucky Mackerel*. Bring her back in one piece. Or two big pieces. Not lots of little ones.\""))
                    .go("market")
                .choice("Head for the causeway to Gull Rock").icon("🗼").go("causeway");
    }

    // =========================================================================================== chapter 1
    static void chapterOne(Campaign c) {
        c.scene("causeway").chapter(CH1).title("The Low-Tide Causeway").backdrop("coast").weather("fog").time("day").music("eerie").anchor("road")
                .text("At low tide a crooked spine of black rock rises out of the sea, linking the beach to GULL ROCK. The causeway is slick with weed and dotted with tide-pools, and the lighthouse looms at the far end like a dead finger pointing at the sky.",
                        "And on the wet sand, between you and the rock, the drowned dead are walking. A dozen of them, maybe more — sailors in rotted coats and tarred pigtails, two centuries out of fashion, shuffling in slow circles as if searching for something they lost. One of them turns its head toward you. Water pours from its mouth.")
                .choice("Draw steel and cut a path across").icon("⚔").battle("low_tide")
                    .victory("The last drowned sailor slumps into the surf and sinks. Its hand, as it goes, is still clutching a rusted brass button stamped with a crowned skull — the mark of the pirate queen Morwen Blackwater.", "lighthouse_door", xp(25))
                    .defeat("You're driven back, half-drowned, to the beach. When the tide turns the dead sink back into the sea, and you crawl across the causeway with salt in your lungs.", "lighthouse_door")
                .choice("Slip between the dead while they wander").icon("🌑").check("stealth", 12).group()
                    .pass("Rock to rock, pool to pool, you creep past the drowned like crabs in the shallows. One of them sniffs the air as you pass. Then it shuffles on, mumbling a shanty to itself. You reach Gull Rock without a sound.", "lighthouse_door", xp(50))
                    .fail("A boot slips on wet weed with a *splosh* that echoes like a cannon. Every drowned head turns toward you at once.", "low_tide_fight")
                .choice("Row Mags's skiff around the back of the rock").icon("🚣").ifFlag("mags_boat").check("athletics", 11)
                    .pass("You put your backs into the oars and the *Lucky Mackerel* skims around the seaward side of Gull Rock, where the dead don't walk. You tie up at the old keeper's landing, dry-shod and smug.", "lighthouse_door", xp(50))
                    .fail("A wave catches the skiff side-on and slams you into the rocks. You make the landing soaked and bruised, but you make it.", "lighthouse_door", damageOne("1d6", "bludgeoning"))
                .choice("Pray for the restless dead to sleep").icon("🙏").check("religion", 13)
                    .pass("You speak the sailors' prayer — *'Rest, and the tide will carry you home'* — and one by one the drowned stop walking. They stand in the surf, heads bowed, as you pass between them. Several of them, you could swear, take off their hats.", "lighthouse_door", xp(50))
                    .fail("Your prayer is sincere, but the dead are too hungry to listen. They turn toward you with outstretched, dripping hands.", "low_tide_fight");

        c.scene("low_tide_fight").chapter(CH1).title("The Drowned Walk").backdrop("coast").weather("fog").time("day").music("battle")
                .text("They come out of the surf in a ragged, dripping line — and something big and clacking scuttles out behind them.")
                .choice("Fight!").icon("⚔").battle("low_tide")
                    .victory("The drowned slump back into the shallows and sink. You splash the rest of the way to Gull Rock, soaked to the knees and breathing hard.", "lighthouse_door", xp(25))
                    .defeat("You're driven back to the beach and must wait, shivering, until the dead sink with the turning tide. Then you cross.", "lighthouse_door");

        c.scene("lighthouse_door").chapter(CH1).title("The Keeper's Door").backdrop("coast").weather("fog").time("dusk").music("mystery")
                .text("The lighthouse is taller than it looked from the harbour: a tower of salt-white stone streaked with gull droppings, a little keeper's cottage huddled at its base with its windows boarded shut. The great iron door at the foot of the tower is barnacled as if it had spent a century on the sea floor.",
                        "Someone has painted a word on it in tar, in big frightened letters: *DONT.*",
                        "From somewhere high above, very faintly, you hear a man's voice humming a lullaby.")
                .choice("Force the barnacled door").icon("💪").check("athletics", 12)
                    .pass("You set your shoulder to it and *heave*. Barnacles crunch, hinges shriek, and the door grinds open onto a spiral stair winding up into darkness.", "stair")
                    .fail("The door won't budge — until, with a sigh like an old man's, it swings open on its own. Above, the humming stops.", "stair", flag("ghost_warned"))
                .choice("Pick the rusted lock").icon("🖐").ifItem("thieves_tools").check("sleight_of_hand", 12)
                    .pass("The lock is a beautiful old piece of sea-brass, swollen with salt. You coax it with oil and patience until it gives a satisfying *clunk*.", "stair", xp(25))
                    .fail("A pick snaps in the lock. Then the door opens on its own anyway, with a long, patient creak. Above, the humming stops.", "stair", flag("ghost_warned"))
                .choice("Knock politely and ask permission to enter").icon("✋")
                    .effect(flag("ghost_courtesy"), log("You knock three times. The humming above stops. A long, long silence... then the door unlocks itself with a polite little *click*, and far up the tower a dry old voice says: *\"Wipe your feet.\"* You wipe your feet. (The keeper will remember your manners.)"))
                    .go("stair");

        c.scene("stair").chapter(CH1).title("One Hundred and Forty Steps").backdrop("ruins").time("dusk").music("tension")
                .text("The stair spirals up the inside of the tower, a hundred and forty stone steps worn hollow by two centuries of keepers' boots. There's no rail. On one side, the wall; on the other, a dizzy drop down the middle of the tower to the flagstones far below.",
                        "Halfway up, the steps change. Some are newer, darker, set in fresh mortar. Some are older and cracked. And a few of them — if you look closely — sit just a finger's width *lower* than the rest.")
                .choice("Test every step before you trust it").icon("🔍").check("investigation", 13)
                    .pass("You tap each step with a boot heel. The hollow ones ring like bells: pivot-stones, rigged to tip anyone careless into the drop. Somebody built a trap into this stair long ago — to keep pirates *out*. You chalk the safe steps and climb.", "lamp_room", xp(50))
                    .fail("You're sure you've found the pattern. The step under your foot disagrees.", "stair_trap")
                .choice("Rope yourselves together and climb as a team").icon("🪢").ifItem("rope").check("athletics", 11).group()
                    .pass("You tie on at the waist, and when a pivot-stone tips beneath someone's boot, the rope snaps taut and three pairs of hands haul them back. Nobody falls. Everybody swears.", "lamp_room", xp(25))
                    .fail("The stone tips, the rope snaps taut — and drags someone else over the edge with them!", "stair_trap")
                .choice("Charge up two steps at a time").icon("🏃").go("stair_trap");

        c.scene("stair_trap").chapter(CH1).title("The Pivot-Stones").backdrop("ruins").time("dusk").music("tension")
                .text("*CLUNK.* A step tips sideways like a trapdoor, then another, then three more in a rippling cascade, and the stair beneath you becomes a stone slide into the drop!")
                .choice("Grab for the wall!").icon("🧗").check("save:dex", 12).each()
                    .pass("You fling yourself flat against the wall, fingers jammed into the mortar, as the steps tip and settle back into place. Heart hammering, you climb on.", "lamp_room")
                    .fail("You slide, scrape and tumble a full turn of the spiral before you catch a ledge, battered and bleeding.", "lamp_room", damageOne("2d6", "bludgeoning"));

        c.scene("lamp_room").chapter(CH1).title("The Lamp Room").backdrop("coast").weather("fog").time("night").music("eerie")
                .npc("Amos Fell", "The Drowned Keeper", "#a9cce3", "👻", "Out. OUT! I'll not see another ship go down by my light. Not one more. Not EVER.")
                .text("The lamp room crowns the tower: a great iron lantern the size of a cart, its lens of thick green glass cracked and furred with salt, its wick cold for a month. Through the windows the green fog presses against the glass like a face.",
                        "And by the lamp stands a ghost. An old man in a sou'wester and oilskins, grey and translucent, a little lantern in his hand burning with cold blue light. Water drips from his beard and never reaches the floor. AMOS FELL, the first keeper of Gull Rock.",
                        "\"Two hundred years ago I dimmed this light,\" he whispers, \"to wreck a pirate. Morwen Blackwater and her *Widow's Wake*. And it worked. And her crown went into the sea, and the sea has been *hungry* ever since. Every light I light, a ship goes down.\" His eyes blaze. \"So I keep it dark. And I keep YOU out.\"")
                .choice("Relight the great lamp with a Fire Bolt").icon("🔥").spell("fire_bolt")
                    .pass("You loose a bolt of flame straight into the cold wick. It catches with a *WHUMPH*, and light pours through the green lens and out across the sea, burning a long golden road through the fog. Amos stares at it. Out in the bay, a fishing boat that was drifting toward the rocks turns, safe, for home. \"...It's not killing them,\" he whispers. \"It's *guiding* them.\"", "keeper_peace", xp(50))
                .choice("Clean the lens and light the lamp by hand").icon("🔧").check("investigation", 13)
                    .pass("You scrub the salt from the glass, trim the wick, find the old keeper's tinderbox in its iron box. The lamp catches. Light floods out across the bay, and the green fog *recoils* from it like a slug from salt. Amos lowers his lantern, astonished. \"It... pushes her back,\" he breathes.", "keeper_peace", xp(50))
                    .fail("The tinder is wet, the wick crumbles, and the ghost's patience snaps like a rotten spar. \"I said OUT!\"", "ghost_fight")
                .choice("Tell him the truth: he saved the town that night").icon("💬").check("persuasion", 14).advantageIf("ghost_courtesy")
                    .pass("\"Morwen was going to burn Gullhaven,\" you say. \"You stopped her. The crown's curse isn't your fault — but it's *your* light that can fight it.\" The old man's face crumples. Two hundred years of guilt, and nobody ever told him that. \"...Help me light it, then,\" he whispers. You do.", "keeper_peace", xp(50))
                    .fail("\"Words!\" Amos howls. \"Pretty words, like the ones I tell myself every night!\" His lantern flares a terrible cold blue.", "ghost_fight")
                .choice("Pray that he may finally rest").icon("🙏").check("religion", 13)
                    .pass("You speak the keepers' blessing that is carved over every lighthouse door on this coast: *'Keep the light, and the light keeps you.'* Amos goes very still. Then, slowly, he lights his little lantern from the great lamp's wick — and the great lamp catches too.", "keeper_peace", xp(50))
                    .fail("The ghost laughs, a sound like a cracked bell. \"Rest? There's no rest while she wears that crown!\"", "ghost_fight")
                .choice("Attack the ghost").icon("⚔").go("ghost_fight");

        c.scene("ghost_fight").chapter(CH1).title("The Keeper's Wrath").backdrop("coast").weather("fog").time("night").music("battle")
                .text("Amos Fell rises off the floor, his lantern blazing, and from the stairwell below come wet, shuffling footsteps: drowned wreckers, dragged up from the rocks by his grief.")
                .choice("Defend yourselves").icon("⚔").battle("lighthouse")
                    .victory("The ghost's lantern gutters. Amos sinks to his knees, fading, and looks out at the dark sea. \"I'm so tired,\" he whispers. \"Light it. Please. Someone light it.\" And he's gone. You light the great lamp, and golden light pours out across the bay. On the keeper's desk, his logbook lies open.", "lighthouse_log", xp(25))
                    .defeat("The keeper's wail hurls you down the stair. When you come to, the tower is silent and the ghost is gone. You climb back up, light the lamp with shaking hands, and find his logbook lying open on the desk, as if left for you.", "lighthouse_log");

        c.scene("keeper_peace").chapter(CH1).title("The Light Returns").backdrop("coast").time("night").weather("none").music("hope")
                .npc("Amos Fell", "Keeper of the Light", "#f9e79f", "🏮", "Two hundred years, and I'd forgotten what it looked like. The light. Going *out*, instead of in.")
                .onEnter(flag("keeper_ally"), flag("light_lit"))
                .text("The great lamp turns, slow and stately, and its beam sweeps across the bay like a lighthouse keeper's arm across a sleeping child. Where it touches the green fog, the fog *hisses* and draws back. From the town below, faintly, you hear people cheering.",
                        "Amos stands taller in the golden light. The water has stopped dripping from his beard. \"The crown went down with the *Widow's Wake*,\" he tells you. \"And something found it. A sea hag — old Mother Brine, who used to drown fishermen for sport when I was a lad. She wears it now, and it makes her a queen. The fog is her breath. The crews...\" He hesitates. \"I don't think they're dead. Not yet. She *collects* them.\"",
                        "He presses his brass storm-barometer into your hands. \"The smugglers bring her the ships. They hide in the Weeping Caves, under the Hagstooth. Go there. And when you face her...\" his eyes glint, \"I'll keep a light burning for you. Wherever you are.\"")
                .choice("Accept the keeper's stormglass and his blessing").icon("🏮")
                    .effect(item("dc_stormglass"), item("dc_keeper_log"), log("Keeper's Stormglass can be attuned from your character sheet: +1 to spell attacks and +1 initiative. And Amos Fell has promised to come when you need him most."))
                    .go("cave_mouth");

        c.scene("lighthouse_log").chapter(CH1).title("The Keeper's Log").backdrop("coast").time("night").weather("none").music("sorrow")
                .text("The keeper's logbook is two centuries old, its pages swollen with damp, the ink faded brown. Most entries are about weather and gulls. The last one isn't.",
                        "*\"14th of Harvestmoon. Tonight I dimmed the light, and the Widow's Wake struck the Hagstooth. Morwen Blackwater went down singing, the black crown still on her head. God forgive me. The sea took her crown — and I think the sea liked the taste. Already there is something in the water, an old hag who laughs under the waves. She swims toward the wreck.\"*",
                        "Tucked inside the back cover is a smuggler's map someone left here recently: the WEEPING CAVES under the Hagstooth Cliffs, marked with a crowned skull.")
                .choice("Take the log and head for the Weeping Caves").icon("🗺")
                    .effect(item("dc_keeper_log"), flag("cave_map"), flag("light_lit"))
                    .go("cave_mouth");
    }

    // =========================================================================================== chapter 2
    static void chapterTwo(Campaign c) {
        c.scene("cave_mouth").chapter(CH2).title("The Weeping Caves").backdrop("seacave").time("day").music("explore")
                .onEnter(longRest())
                .text("You sleep the night in the old keeper's cottage at the foot of the tower, under a patchwork quilt that smells of pipe smoke, while the great lamp turns above you and keeps the fog at bay. (Long rest.)",
                        "The Hagstooth Cliffs rise sheer from the sea north of Gullhaven, black and jagged as a hag's grin. At their foot, half-hidden by a curtain of falling water, gapes the mouth of the WEEPING CAVES — so called because the walls drip, endlessly, like a giant crying.",
                        "You row in on the morning tide. Inside, the light turns green and gold, reflected off water so clear you can see crabs scuttling ten feet down. The cave smells of salt, tar and — faintly — rum. Smugglers have been here. Recently.")
                .choice("Follow the smugglers' chalk marks").icon("🔍").check("investigation", 12)
                    .pass("Little chalk fish, drawn low on the walls, every one pointing the same way. Smugglers' sign — you know this game. *Head swims left: safe. Head swims right: trouble.* You file it away.", "tidepools", flag("marks_read"), xp(25))
                    .fail("There are chalk marks, but they could be fish, or arrows, or a very bored smuggler's doodles.", "tidepools")
                .choice("Read the smugglers' sign — you know this code").icon("🦑").ifSkillProficient("deception")
                    .effect(flag("marks_read"), log("Little chalk fish on the walls. Head left, safe; head right, trouble. Same code every smuggler on the coast has used since your grandmother was a pickpocket. You grin and lead the way."))
                    .go("tidepools")
                .choice("Row on into the dark").icon("🚣").go("tidepools");

        c.scene("tidepools").chapter(CH2).title("The Glittering Pools").backdrop("seacave").time("day").music("tension")
                .text("The channel ends at a wide shelf of rock pocked with tide-pools, glittering like jewels in the dim light. Anemones wave in them, purple and gold. Tiny fish dart. It is lovely.",
                        "It is also the only way forward, and between the pools the rock is crusted with RAZOR CORAL, white and sharp as broken glass. Some of the pools are only an inch deep. Some of them, if you look closely, don't seem to have a bottom at all.")
                .choice("Follow the chalk fish to the safe path").icon("🐟").ifFlag("marks_read")
                    .effect(xp(25), log("The chalk fish lead you on a winding path from dry stone to dry stone, around every deep pool and razor-reef. Behind you, a curious crab falls into one of the bottomless pools and is never seen again."))
                    .go("crab_grotto")
                .choice("Freeze a path across the pools (Ray of Frost)").icon("❄").spell("ray_of_frost")
                    .pass("Frost crackles across the water, sealing the deep pools under a skin of white ice. You skate across like children on a winter pond, laughing, while the anemones underneath shake their tentacles at you in outrage.", "crab_grotto", xp(25))
                .choice("Pick your way across carefully").icon("👁").check("perception", 13)
                    .pass("You spot the tell-tale dark of the deep pools and the glitter of the razor coral. Step by careful step, you lead the way across.", "crab_grotto", xp(25))
                    .fail("You're sure that pool is shallow. It isn't. Neither is the next one.", "tidepool_trap")
                .choice("Just splash across").icon("🏃").go("tidepool_trap");

        c.scene("tidepool_trap").chapter(CH2).title("Razor and Undertow").backdrop("seacave").time("day").music("tension")
                .text("Your foot plunges into a pool that has no bottom — and the water *pulls*, a hungry undertow dragging you down against walls of razor coral!")
                .choice("Kick free!").icon("🌊").check("save:dex", 12).each()
                    .pass("You twist, kick, and haul yourself onto dry rock, dripping and gasping but unscathed.", "crab_grotto")
                    .fail("The coral slices you as the undertow drags you along it, before you claw your way out, bleeding and spluttering.", "crab_grotto", damageOne("1d10+1", "slashing"));

        c.scene("crab_grotto").chapter(CH2).title("The Smugglers' Larder").backdrop("seacave").time("day").music("tension")
                .text("Beyond the pools the cave widens into a grotto stacked with smugglers' barrels: rum, salt pork, kegs of black powder. And all over them, crawling and clacking and very pleased with themselves, are crabs. Big crabs. Dog-sized crabs.",
                        "In the middle of the grotto, half-buried in a pile of gnawed barrel-staves, something enormous shifts. A claw the size of a door rises out of the pile and snaps, once, with a sound like a cannon going off. GRANDFATHER SNAPPER has woken up.")
                .choice("Fight your way through!").icon("⚔").battle("crabs")
                    .victory("The great crab collapses with a hollow clatter. Among the barrels you find a keg of good rum, a purse of smuggler's coin and — tucked in a crate marked FISH — two potions of healing.", "merfolk", gold(15), item("potion_healing"), item("potion_healing"))
                    .defeat("You retreat into a side-passage, nursing pinched fingers, until the crabs lose interest and go back to eating salt pork. Then you sneak around them.", "merfolk")
                .choice("Distract them with an illusory feast (Minor Illusion)").icon("✨").spell("minor_illusion")
                    .pass("You conjure a shimmering pile of fat, juicy fish on the far side of the grotto. Every crab — Grandfather Snapper included — goes clattering after it in a stampede of legs. You stroll past them, whistling.", "merfolk", xp(50))
                .choice("Bribe them with a smashed barrel of salt pork").icon("🍖").once().check("animal_handling", 13)
                    .pass("You crack open a barrel and roll it, spilling salt pork, toward the back of the cave. The crabs pile on with joyful clacking. Even Grandfather Snapper settles down to feast. Nobody notices you at all.", "merfolk", xp(50))
                    .fail("The crabs ignore the pork. They'd rather have *you*. A dog-sized crab nips you smartly on the way back to cover, and Grandfather Snapper rises out of his pile with a sound like a barn door opening.", "crab_grotto", damageOne("1d6", "bludgeoning"))
                .choice("Creep along the high ledge").icon("🌑").once().check("stealth", 13).group()
                    .pass("Inch by inch, along a ledge no wider than your foot, you skirt the grotto. Below, Grandfather Snapper snores like a foghorn.", "merfolk", xp(50))
                    .fail("A loose stone clatters into the grotto. Every crab turns. Every claw rises. You scramble back down off the ledge before they can reach it — the sneaky way is closed now.", "crab_grotto");

        c.scene("merfolk").chapter(CH2).title("Tridents in the Dark").backdrop("seacave").time("day").music("tension")
                .npc("Ondine Kelpbraid", "Merfolk Scout of the Hagstooth Reef", "#1abc9c", "🧜", "Don't move, air-breathers. Smugglers die in these caves. Give me one reason you're not smugglers.")
                .onEnter(shortRest())
                .text("Past the crab grotto you find a dry side-cave and stop to catch your breath, bind your cuts and share a flask of Dorrie's chowder, still faintly warm. (Short rest.)",
                        "Then on: the passage opens onto a flooded cavern — a lagoon lit by glowing jellyfish drifting under the surface like lanterns. You're halfway along the ledge when the water erupts.",
                        "Three merfolk rise from the lagoon, scales flashing silver and green, coral tridents levelled at your throats. Their leader is a young woman with kelp braided through her hair and a fresh, ugly wound across one shoulder: ONDINE KELPBRAID. Behind her, the other two are bleeding too.",
                        "\"The sahuagin serve the Drowned Queen now,\" she hisses. \"They burned our reef. And your *smugglers* row the sea-devils' captives in, every new moon. So you have one breath to explain yourselves.\"")
                .choice("Tell her the truth: you're hunting Mother Brine").icon("💬").check("persuasion", 13).advantageIf("light_lit")
                    .pass("\"We lit the light on Gull Rock,\" you say. \"Last night. Did you see it?\" Ondine's trident wavers. \"...The fog drew back,\" she says slowly. \"We saw. We thought — we hoped —\" She lowers her weapon. \"Then you're the first good thing to come out of Gullhaven in a month.\"", "merfolk_ally", xp(50))
                    .fail("\"Liars always say that.\" Ondine's eyes narrow — then widen, looking past you. \"*Sea-devils!* They followed us!\" The merfolk dive, and the water churns with fins.", "sahuagin_fight")
                .choice("Offer to heal their wounds (Cure Wounds)").icon("💚").spell("cure_wounds")
                    .pass("You lay a hand on her shoulder and golden light knits Ondine's shoulder closed. She touches it, stunned. Her scouts stare at you as if you'd pulled the moon out of your pocket. \"Smugglers don't heal,\" Ondine says at last. \"Smugglers *take*.\" She lowers her trident. \"Who are you?\"", "merfolk_ally", xp(50))
                .choice("Charm Ondine (Charm Person)").icon("💗").spell("charm_person")
                    .pass("Warmth glimmers in your eyes and Ondine's suspicion melts like ice in summer. \"...You're not like the others,\" she says, lowering her trident. \"You smell of lightning and lamp-oil, not tar.\" (When the charm fades she'll know — but she'll also know you meant no harm.)", "merfolk_ally", xp(50))
                .choice("Sing a sailor's peace-song to them").icon("🎵").check("performance", 13)
                    .pass("You sing the old shanty every sailor on this coast knows, the one about the fisherman who married a mermaid and was happy ever after. Ondine's lips twitch. \"That song is *terrible*,\" she says. \"My grandmother loved it.\" Her trident lowers.", "merfolk_ally", xp(50))
                    .fail("Your voice cracks on the high note. Ondine winces — and then her head whips round. \"Sea-devils! You've led them to us!\"", "sahuagin_fight");

        c.scene("merfolk_ally").chapter(CH2).title("Friends of the Reef").backdrop("seacave").time("day").music("hope")
                .npc("Ondine Kelpbraid", "Merfolk Scout of the Hagstooth Reef", "#1abc9c", "🧜", "When you go down to face the Queen, air-breather, look for me. The reef owes you. The reef always pays its debts.")
                .onEnter(flag("merfolk_ally"))
                .text("Ondine tells you everything. The Drowned Queen's palace lies beneath the bay, in the wreck of the *Widow's Wake* and a dozen older ships. The fog is her breath. The missing sailors sleep in her Pearl Gallery, in bubbles of stolen air, dreaming her dreams — and she feeds on those dreams to grow.",
                        "\"A war-party of sahuagin is camped in the next cavern,\" Ondine adds, \"waiting for the smugglers' next delivery. We can lead you past them through the drowned tunnels — or, if you'd rather, we can hit them together.\"",
                        "She presses a black pearl into your palm. \"For luck. And so I can find you, when it matters.\"")
                .choice("Slip past the sahuagin with the merfolk").icon("🧜")
                    .effect(item("dc_tide_pearl"), xp(50), log("The Tidecaller Pearl can be attuned from your character sheet: +1 AC and +1 to Constitution saves. Ondine leads you through a drowned tunnel, holding your hands as you swim, and you surface beyond the sahuagin camp unseen."))
                    .go("smuggler_den")
                .choice("Ambush the sahuagin war-party together").icon("⚔").effect(item("dc_tide_pearl"), flag("sahuagin_ambush")).battle("sahuagin")
                    .victory("The last sea-devil flees, shrieking, into the deep. Ondine's scouts whoop and slap their tails against the water. \"Not bad, air-breathers,\" Ondine grins. \"Not bad at all.\"", "smuggler_den", xp(50))
                    .defeat("The sea-devils drive you back, but Ondine's scouts drag you into a drowned tunnel and away. You surface, coughing, beyond the camp.", "smuggler_den");

        c.scene("sahuagin_fight").chapter(CH2).title("Sea-Devils").backdrop("seacave").time("day").music("battle")
                .text("The lagoon boils. Shark-toothed sahuagin burst out of the water with spears and claws, and behind them a priestess in a robe of shark-skin raises a staff and begins to shriek a hymn to the Drowned Queen.")
                .choice("Fight for your lives!").icon("⚔").battle("sahuagin")
                    .victory("The last sea-devil flees into the deep, leaving a trail of bubbles. Among the dead you find a shark-tooth amulet carved with a crown, and a pouch of black pearls.", "smuggler_den", gold(20))
                    .defeat("You're driven back along the ledge and must hide in a crevice until the sea-devils swim off. Then you creep on.", "smuggler_den");

        c.scene("smuggler_den").chapter(CH2).title("The Saltjacks' Den").backdrop("seacave").time("day").music("mystery")
                .text("The deepest cavern is the Saltjacks' den: hammocks strung between stalactites, a long table made from a ship's door, a dartboard with Odessa Vane's face pinned to it. It's empty — the smugglers are either in Gullhaven's lock-up or long gone. But they left in a hurry.",
                        "A great sea-chest sits under the table, bound in iron. And on the wall, scratched into the stone, someone has kept a tally: eleven little ships. Eleven crews.")
                .choice("Search Jory's desk for papers").icon("📜").once().check("investigation", 12)
                    .pass("Under a false bottom in the desk drawer: Jory's ledger. Neat columns. *'Merry Halibut — 9 souls — delivered to the Widow — 300 gp drowned gold.' 'Gannet — 12 souls — delivered.'* And on the last page: *'Mother Brine wants a fresh crew for the Widow's Wake. The ship must be manned again by the dark of the moon. Then the crown will rule the whole coast.'*", "smuggler_den", item("dc_jory_ledger"), flag("know_plan"), xp(25))
                    .fail("Mostly unpaid bills and some truly dreadful poetry about a barmaid. You read one. You regret it.", "smuggler_den")
                .choice("Open the iron-bound sea-chest").icon("🗝").once().ifItem("thieves_tools").check("sleight_of_hand", 12)
                    .pass("The lock clicks. Inside: a heap of green-rotten gold coins stamped with the crowned skull, and a fine old cutlass with a pearl in its pommel.", "smuggler_den", gold(25), item("weapon_plus_one"))
                    .fail("The lock fights back and a needle jabs your thumb. Nothing but a sting — the poison dried out years ago — but the lock stays shut.", "smuggler_den", damageOne("1d4", "poison"))
                .choice("Smash the sea-chest open").icon("💪").once().check("athletics", 14)
                    .pass("With a mighty heave the lid tears off its hinges. Inside: green-rotten pirate gold, and a fine old cutlass with a pearl in its pommel.", "smuggler_den", gold(25), item("weapon_plus_one"))
                    .fail("The chest wins. Your shoulder loses.", "smuggler_den")
                .choice("Follow the sound of chanting deeper into the cliff").icon("🐚").go("sunken_shrine");

        c.scene("sunken_shrine").chapter(CH2).title("The Sunken Shrine").backdrop("seacave").time("day").music("mystery")
                .text("A narrow cleft behind the den leads down to a half-flooded chamber — and a shrine far older than any smuggler. A statue of a woman with seaweed hair and a conch held to her lips rises from the water: MARENNE, Lady of the Tides. Pell, if he's with you, makes a small, choked sound and drops to his knees.",
                        "Around the statue's base, an inscription glows faintly blue: *\"I am born in the sky and I die in the sea. I am the only fire the water fears. Name me, and learn the Queen's undoing.\"*",
                        "Beneath it, three sealed niches. One of them, you suspect, does not like wrong answers.")
                .choice("Puzzle out the riddle").icon("🧠").check("arcana", 13)
                    .pass("Fire that the water fears, born in the sky, dying in the sea... You think of thunderheads stalking across the bay on a summer night, and laugh. \"*Lightning.*\"", "shrine_open", xp(25))
                    .fail("Fire... sky... sea... the words slosh around your head without settling. You'll have to guess.", "shrine_guess")
                .choice("Recall the hymns of the Lady of the Tides").icon("🐚").check("religion", 12)
                    .pass("You know this one — it's from the old storm-litany: *'The Lady fears no fire but the fire that falls.'* Lightning. The answer is lightning.", "shrine_open", xp(25))
                    .fail("The hymns blur together into a soggy murmur. You'll have to guess.", "shrine_guess")
                .choice("Answer: \"Storm-fire. Lightning.\"").icon("⚡").ifFlag("zelloe_riddle").go("shrine_open")
                .choice("Answer: \"Lightning\" — the Widow's Lament said so").icon("⚡").ifFlag("know_widow").go("shrine_open");

        c.scene("shrine_guess").chapter(CH2).title("A Guess in the Deep").backdrop("seacave").time("day").music("mystery")
                .text("The inscription glows patiently. The water laps at the statue's feet. Choose your answer.")
                .choice("\"The sun.\"").icon("☀").go("shrine_wrong")
                .choice("\"Lightning.\"").icon("⚡").go("shrine_open")
                .choice("\"A burning ship.\"").icon("🔥").go("shrine_wrong")
                .choice("\"Love.\"").icon("💗").go("shrine_wrong");

        c.scene("shrine_wrong").chapter(CH2).title("The Lady Is Displeased").backdrop("seacave").time("day").music("tension")
                .text("The inscription flares from blue to blinding white. The conch at the statue's lips *sounds* — a deep, booming note — and a wall of freezing seawater bursts from the niches!")
                .choice("Brace yourselves!").icon("🌊").check("save:dex", 13).each()
                    .pass("You leap onto a ledge as the icy wave crashes past. When it drains away, the riddle is waiting again — patient as the tide.", "shrine_guess")
                    .fail("The freezing wave slams you into the wall and leaves you shuddering and blue-lipped.", "shrine_guess", damageOne("2d6", "cold"));

        c.scene("shrine_open").chapter(CH2).title("The Queen's Undoing").backdrop("seacave").time("day").music("hope")
                .onEnter(flag("brine_weakness"))
                .text("At the true answer the conch sings a single sweet note, and the three niches slide open. In the first lies a scroll in a sealed bottle. In the second, a small coral idol, pink and pulsing — and smashed in half, long ago. In the third, a bronze plaque.",
                        "*\"The Black Crown was forged in the storm by a mad sea-witch and stolen by Morwen Blackwater. Whoever wears it rules the drowned — but it is a thing of storm, and storm-fire unmakes it. Strike the wearer with lightning and the crown will crack. Her strength is drawn from coral idols grown from sailors' last breaths: break them, and she starves.\"*",
                        "Beneath it, scratched in a shaky hand: *'I tried. — M.B.'* Morwen Blackwater herself. **(You know Mother Brine's weakness. She will start the final battle weakened!)**")
                .choice("Take the knowledge and make camp").icon("⛺").effect(xp(50)).go("cave_camp");

        c.scene("cave_camp").chapter(CH2).title("Camp at the Edge of the Sea").backdrop("camp").time("night").music("camp").anchor("camp")
                .onEnter(level(3), longRest())
                .text("You make camp on a dry shelf of rock at the cave's seaward mouth, where you can see the bay. A driftwood fire crackles. Far out on the water the green fog glows, and in the middle of it, for a moment, you glimpse black sails.",
                        "You rest, and in the resting something in you grows stronger — the courage of people who have seen the dark and decided to walk into it anyway. **(Every hero reaches level 3 and finishes a long rest! Wizards learn Scorching Ray and Misty Step.)**",
                        "Odessa's cutter, the *Kittiwake*, will meet you here at dawn. Tomorrow, you board the *Widow's Wake*.")
                .choice("Take the first watch").icon("👁").once().check("perception", 13)
                    .pass("Past midnight, you see it: the ghost ship drifting through the fog, and a green lantern bobbing toward it from the shore — a smuggler's boat. You note exactly where the ship anchors. Tomorrow, you'll come at it from the blind side.", "cave_camp", flag("ship_spotted"))
                    .fail("The fire pops. The waves sigh. A gull steals a sausage. You decide everything is fine.", "cave_camp")
                .choice("Tell sea-stories around the fire").icon("🔥").once().check("performance", 11)
                    .pass("You tell the tale of the fisherman who fished up the moon, and by the end even Grunda is laughing into her beard (she doesn't have a beard, but she laughs as if she does). Courage is contagious. (Heroic Inspiration)", "cave_camp", inspiration())
                    .fail("Your story drifts off course somewhere around the second sea serpent. Still, it's nice to laugh.", "cave_camp")
                .choice("Study Jory's ledger by firelight").icon("📒").once().ifItem("dc_jory_ledger").check("investigation", 11)
                    .pass("In the margins, in Jory's cramped hand: *'Wake's bosun is a merrow — keep clear of his harpoon. Captain's cabin is rigged. Lift the seahorse latch, not the skull.'* Useful.", "cave_camp", flag("cabin_secret"))
                    .fail("Columns of numbers blur into soup. You fall asleep with the ledger on your face.", "cave_camp")
                .choice("Bank the fire and sleep").icon("🌙").go("fog_sea");
    }

    // =========================================================================================== chapter 3
    static void chapterThree(Campaign c) {
        c.scene("fog_sea").chapter(CH3).title("Into the Green Fog").backdrop("ship").weather("fog").time("dawn").music("eerie").checkpoint()
                .npc("Odessa Vane", "Harbourmaster of Gullhaven", "#34495e", "⚓", "There she is. The Widow. God help us, she's beautiful.")
                .text("Odessa's cutter slides into the fog at dawn with muffled oars. The world shrinks to grey-green nothing. Sounds die. The water is flat as glass and smells of rot.",
                        "And then, out of the fog, she rises: the WIDOW'S WAKE. A three-masted galleon two hundred years drowned, her hull furred with weed and barnacles, her black sails hanging in tatters, green lanterns burning at her rails. On her deck, figures shamble back and forth — a ghost crew working a ship that sank before your great-grandparents were born.",
                        "Somewhere below her keel, a woman's voice is singing.")
                .choice("Throw grapnels and climb the hull").icon("🪝").check("athletics", 12).group()
                    .pass("The grapnels bite into rotten rail and you swarm up the weed-slick hull like proper pirates. You're over the rail before the dead even notice — and they turn, too slowly, to find you already among them.", "deck_fight", flag("deck_surprise"))
                    .fail("A grapnel tears loose with a shriek of rotten wood. Every drowned head on the deck turns toward you. So much for surprise.", "deck_fight")
                .choice("Let the merfolk carry you up the blind side").icon("🧜").ifFlag("merfolk_ally")
                    .effect(flag("deck_surprise"), log("Ondine's scouts rise silently beside the cutter and lift you, one by one, up the seaward side of the hull, where no lantern shines. You slip over the rail like shadows."))
                    .go("deck_fight")
                .choice("Come at her from the side you scouted last night").icon("👁").ifFlag("ship_spotted")
                    .effect(flag("deck_surprise"), log("You know exactly where the lanterns don't reach. Odessa brings the cutter in under the stern gallery, and you climb aboard through a broken window into the shadows."))
                    .go("deck_fight")
                .choice("Sing the Widow's Lament so the dead think you're crew").icon("🎵").check("performance", 14)
                    .pass("You lift your voice in the old pirate's dirge as the cutter bumps against the hull. On deck the dead stop. They *listen*. They throw down a rope ladder. You climb aboard to a ghostly honour guard — and when they realise you're no crew of theirs, you're already among them.", "deck_fight", flag("deck_surprise"))
                    .fail("You hit a note that the Widow's dead have *never* sung. Every head turns. Every rusty cutlass comes out.", "deck_fight")
                .choice("Bring the cutter alongside and board!").icon("⚔").go("deck_fight");

        c.scene("deck_fight").chapter(CH3).title("All Hands on Deck").backdrop("ship").weather("fog").time("dawn").music("battle")
                .text("The drowned crew lurches toward you, cutlasses rusted, mouths spilling seawater. And from the hatch amidships climbs something huge: a sea-ogre in a dead bosun's coat, dragging a harpoon as long as a mast. \"FRESH CREW,\" it gurgles happily. \"THE QUEEN WILL BE SO PLEASED.\"")
                .choice("Clear the deck!").icon("⚔").battle("deck")
                    .victory("The merrow topples over the rail with a tremendous splash, and the last of the drowned crew collapse into puddles of seawater and kelp. The *Widow's Wake* creaks around you, silent. At the stern, a carved door leads to the captain's cabin.", "cabin_door", xp(50));

        c.scene("cabin_door").chapter(CH3).title("The Captain's Cabin").backdrop("ship").weather("fog").time("dawn").music("tension")
                .text("The captain's door is a work of art: black oak carved with mermaids, sea-serpents and a pirate queen with a crown of pearls. In the centre, two brass latches — one shaped like a SKULL, one like a SEAHORSE.",
                        "Through the keyhole you glimpse candlelight, a great table strewn with charts, and something glittering. Above the lintel, almost hidden in the carving, you spot the black iron tips of half a dozen harpoons, pointing straight down at the doorway.")
                .choice("Lift the seahorse latch, as Jory's ledger said").icon("🐴").ifFlag("cabin_secret")
                    .effect(xp(50), log("The seahorse latch lifts with a gentle *click*. Above, the harpoons stay politely in their slots. Thank you, Jory. You almost feel bad about the black eye."))
                    .go("cabin")
                .choice("Study the latches and the mechanism").icon("🔍").check("investigation", 13)
                    .pass("A thin wire runs from the skull latch up into the carving. The seahorse latch is clean. Pirates, it seems, think everyone will go for the skull. You lift the seahorse and the door swings open.", "cabin", xp(50))
                    .fail("The skull latch looks like the real one. It's very convincing. It's meant to be.", "cabin_trap")
                .choice("Freeze the harpoon springs solid (Ray of Frost)").icon("❄").spell("ray_of_frost")
                    .pass("A whisper of frost into the carving and the springs above the door crackle and seize. You open the door with the skull latch out of sheer spite. Above, the harpoons give a sad, frozen *clunk* and stay put.", "cabin", xp(50))
                .choice("Disarm the trap with thieves' tools").icon("🛠").ifItem("thieves_tools").check("sleight_of_hand", 13)
                    .pass("You find the trip-wire and pinch it between two fingers, ease the tension out of it and tie it off. The harpoons sleep on in their slots.", "cabin", xp(50))
                    .fail("Your pick slips. Something above you goes *thunk*.", "cabin_trap")
                .choice("Kick the door in").icon("🦶").go("cabin_trap");

        c.scene("cabin_trap").chapter(CH3).title("Harpoons!").backdrop("ship").weather("fog").time("dawn").music("tension")
                .text("*THWUNK-THWUNK-THWUNK!* Harpoons slam down from the lintel in a rain of black iron, nailing the deck where you were standing a heartbeat ago!")
                .choice("Throw yourselves aside!").icon("💥").check("save:dex", 13).each()
                    .pass("You roll clear as a harpoon quivers in the planks beside your ear.", "cabin")
                    .fail("A harpoon rakes across you before you can twist away.", "cabin", damageOne("2d6", "piercing"));

        c.scene("cabin").chapter(CH3).title("The Widow's Cabin").backdrop("ship").weather("fog").time("dawn").music("sorrow")
                .text("The cabin is untouched by two centuries of sea: candles burning, charts pinned to the table, a portrait over the bunk of a fierce, laughing woman with a black crown on her head — MORWEN BLACKWATER, the Widow. Under the portrait someone has written in lipstick: *She took it from me. She took my ship. — M.*",
                        "On the table lies the captain's log, its last entry fresh and wet: *'The Queen will crew me again. Twelve fine sailors from the Gannet, sleeping sweet in the Pearl Gallery. When the moon goes dark, the Wake sails again — and every harbour on this coast will feed her.'*",
                        "Then the deck lurches beneath your feet. Water bubbles up through the planks. The *Widow's Wake* is SINKING — not drifting, but diving, nose-first, down toward the green glow far beneath the bay.")
                .choice("Grab the brass key from the table").icon("🗝").once()
                    .effect(item("dc_widow_key"), gold(30), log("A seahorse-shaped brass key, and a purse of the Widow's own gold. Under the key, a note in that same fierce hand: *'For whoever finally kills the hag. Give her my regards.'*"))
                    .go("cabin")
                .choice("Hold on tight and ride the ship down").icon("🌊").go("palace_gate")
                .choice("Look for a pocket of air as the cabin floods").icon("🫧").check("survival", 12)
                    .pass("You spot it: the cabin's carved ceiling is a sealed dome, and as the water rises, the air gathers there in a shining bubble. You press your faces into it, breathing, as the ship plunges into the deep.", "palace_gate", xp(25))
                    .fail("The water rises over your heads. Just as your lungs begin to burn, the ship slams into something with a mighty CRUNCH — and the water drains away through the shattered hull.", "palace_gate", damage("1d6", "bludgeoning", "con", 12));
    }

    // =========================================================================================== finale
    static void finale(Campaign c) {
        c.scene("palace_gate").chapter(FIN).title("The Drowned Palace").backdrop("lair").time("night").weather("none").music("eerie").anchor("dungeon").checkpoint()
                .onEnter(shortRest())
                .text("The *Widow's Wake* has come to rest at the bottom of the bay, and you step off her shattered bow into a dream.",
                        "A palace rises around you, built of wrecks: the hulls of a hundred drowned ships stacked and fused with pink coral into towers and arches and staircases. A great bubble of stolen air holds back the sea overhead, and through it you can see fish drifting past like birds. Everything glows a soft, sickly green. Everything drips.",
                        "Ahead, an arch of whalebone leads deeper in. You catch your breath and bind your wounds as best you can (short rest). From somewhere within, the singing goes on.")
                .choice("Pray to Marenne for courage").icon("🐚").once().check("religion", 12)
                    .pass("You whisper the Lady's name, and far above, faintly, through all that water, you could swear you hear a lighthouse bell. (Heroic Inspiration)", "palace_gate", inspiration())
                    .fail("The deep swallows your prayer. But you said it, and that matters.", "palace_gate")
                .choice("Prepare spells and gear (open your character sheet)").icon("📖").once().effect(log("Take a moment: attune magic items, swap prepared spells, and hand potions to whoever needs them most.")).go("palace_gate")
                .choice("Pass beneath the whalebone arch").icon("🦴").go("jelly_hall");

        c.scene("jelly_hall").chapter(FIN).title("The Hall of Lanterns").backdrop("lair").time("night").music("tension")
                .text("Beyond the arch, a long hall built from the inverted hull of a warship. From its ribs hang hundreds of GHOST JELLYFISH, pale as moonlight, their stinging tendrils drifting all the way down to the floor in a shimmering curtain. They pulse gently. They are very beautiful.",
                        "There's no way around. Only through.")
                .choice("Shock them out of the way (Shocking Grasp)").icon("⚡").spell("shocking_grasp")
                    .pass("You lay a crackling hand on the nearest tendril. Lightning ripples up the curtain in a cascade of blue light, and every jellyfish in the hall *flinches*, curling its tendrils up tight like a startled snail. You walk through a clear corridor, grinning.", "pearl_gallery", xp(50))
                .choice("Find the gaps between the drifting tendrils").icon("👁").check("perception", 14)
                    .pass("There's a rhythm to it — the jellies pulse and drift, and every few heartbeats a gap opens. You lead everyone through in a slow, careful dance.", "pearl_gallery", xp(50))
                    .fail("You see a gap. The gap closes.", "jelly_trap")
                .choice("Recall what stops a jellyfish sting").icon("📖").check("nature", 13)
                    .pass("Jellyfish hate fresh water. You uncork your waterskins and splash a path through the curtain; the tendrils recoil, puckering, and you hurry through.", "pearl_gallery", xp(50))
                    .fail("You're fairly sure jellyfish are afraid of loud noises. They are not.", "jelly_trap")
                .choice("Run through and hope for the best").icon("🏃").go("jelly_trap");

        c.scene("jelly_trap").chapter(FIN).title("A Thousand Stings").backdrop("lair").time("night").music("tension")
                .text("The curtain closes around you like a lover's embrace — and every tendril *burns*!")
                .choice("Endure it and push through!").icon("🪼").check("save:con", 12).each()
                    .pass("You grit your teeth and shoulder through the stinging curtain, eyes streaming, and stumble out the far side mostly unharmed.", "pearl_gallery")
                    .fail("The venom sears through you like liquid fire, and you come out the far side shaking and swollen.", "pearl_gallery", damageOne("2d6", "poison"));

        c.scene("pearl_gallery").chapter(FIN).title("The Pearl Gallery").backdrop("lair").time("night").music("sorrow")
                .text("The next chamber is a gallery of great pearly bubbles, each one big enough to hold a sleeping sailor. And each one does.",
                        "They float in rows, curled up like children: fishermen in oilskins, a cook still holding a ladle, an old man with a fiddle clutched to his chest. Eleven crews. Their lips move in their sleep. They are all humming the same song. In the nearest bubble sleeps a young man with Odessa's iron-grey eyes and a captain's coat — TAM VANE. Grunda, if she's with you, puts her hand on the bubble and says nothing at all.",
                        "\"Don't wake them, dearies,\" says a voice like a rotten tide, echoing from the next chamber. \"They're having *such* lovely dreams. Come through. Come and meet your queen.\"")
                .choice("Try to wake Tam").icon("🫧").once().check("medicine", 13)
                    .pass("You press your palm to the bubble and speak his name, over and over. Tam's eyes flicker open for a heartbeat. \"...Mother?\" he whispers — and then the song drags him back under. But he heard you. He knows someone came. (Heroic Inspiration)", "pearl_gallery", inspiration(), flag("tam_stirred"))
                    .fail("The bubble is warm and hard as glass. Tam sleeps on, humming.", "pearl_gallery")
                .choice("Unlock the gallery's coral cage with the Widow's key").icon("🗝").once().ifItem("dc_widow_key")
                    .effect(flag("crews_freed_early"), log("The seahorse key turns in a lock shaped like a skull, and with a crystalline chime the bubbles drift loose from their moorings. They begin to float, slowly, upward. Wherever the Queen's song is coming from, it falters for just a moment — and in that moment, you hear her *gasp*."))
                    .go("pearl_gallery")
                .choice("Go through to face the Queen").icon("👑").go("throne_room");

        c.scene("throne_room").chapter(FIN).title("The Drowned Throne").backdrop("lair").time("night").music("boss").weather("fog")
                .npc("Mother Brine", "The Drowned Queen", "#16a085", "👑", "Ooh, fresh ones. Warm ones. Dry ones! Kneel, dearies, and I'll let you sleep forever in my lovely pearls.")
                .text("The throne room is the hull of the *Widow's Wake*'s twin, turned on end and hollowed out into a cathedral of coral. Pink CORAL IDOLS stand in a ring around a throne of fused ship-wheels, each one pulsing with stolen breath, threads of glowing mist flowing from them into the figure on the throne.",
                        "MOTHER BRINE is enormous. A sea hag swollen to twice a giant's height on drowned men's dreams, her skin green and slick as a rock at low tide, her hair a nest of kelp crawling with tiny crabs. Her fingers end in barnacled claws. And sunk deep into her weedy brow, black and gleaming, sits the DROWNED CROWN.",
                        "*\"Two hundred years I waited for that silly pirate's crown to sink,\"* she croons, *\"and now the whole sea sings for me. You're too late, dearies. The Wake sails tonight.\"*")
                .choice("\"The Wake's sunk, Mother. And so are you.\"").icon("⚔").battle("boss")
                    .victory("", "victory")
                .choice("Strike with storm-fire — the shrine told you how!").icon("⚡").ifFlag("brine_weakness").battle("boss")
                    .effect(log("You raise your hand and lightning crackles down your arm. Mother Brine's eyes go wide — she *knows* that light. A hairline crack races across the Drowned Crown, and she shrieks like a kettle. (Mother Brine starts the fight weakened and without Legendary Resistance.)"))
                    .victory("", "victory")
                .choice("Pretend to kneel... and strike first").icon("🎭").once().check("deception", 15)
                    .pass("You sink to one knee, head bowed. *\"Oh, good, dearie,\"* the hag purrs, leaning down, her rotten breath washing over you. *\"Good little sailor.\"* Then you look up — and smile.", "boss_surprise")
                    .fail("*\"Oh, you're a liar, dearie. I can smell it. Smells like seaweed and fear.\"* She rises from her throne, claws flexing, and the sea above the palace begins to roar. \"Never mind. You'll sleep all the same.\"", "throne_room");

        c.scene("boss_surprise").chapter(FIN).title("The Queen Strikes Second").backdrop("lair").time("night").music("boss")
                .onEnter(flag("boss_surprised"))
                .text("Mother Brine rears back with a shriek, caught completely off guard, crabs tumbling from her hair.")
                .choice("NOW!").icon("⚔").battle("boss").victory("", "victory");

        c.scene("victory").chapter(FIN).title("The Crown Breaks").backdrop("lair").time("night").music("triumph")
                .text("Mother Brine screams — a sound like every storm that ever drowned a ship — and the Drowned Crown on her brow splits clean in two. She crumbles as you watch, kelp and crab-shells and black water, until there is nothing left on the throne but two halves of a cursed circlet and a smell of very old fish.",
                        "In the Pearl Gallery, the bubbles burst one after another. Sailors gasp awake, coughing, confused, *alive*. Tam Vane stumbles toward you on shaking legs.",
                        "And then, with a groan like a world ending, the bubble of air holding back the sea begins to collapse. SWIM.")
                .choice("Grab the sailors and swim for the surface!").icon("🏊").check("athletics", 12).group()
                    .pass("You kick upward through the green water, a sailor's hand in each of yours, toward a light far above — and the light is GOLD. The lighthouse on Gull Rock is blazing, its beam piercing down through the waves like a rope thrown to the drowning. You follow it up and burst into the dawn, gasping, laughing, alive.", "epilogue", xp(200))
                    .fail("The rushing water batters you against coral and wreckage, and your lungs are screaming by the time you claw your way into the light — but you make it, every one of you, into the cold, glorious dawn.", "epilogue", damage("2d6", "bludgeoning", "con", 12), xp(200));

        c.scene("epilogue").chapter("Epilogue").title("Home to the Barnacle & Bell").backdrop("tavern").time("night").music("triumph").anchor("epilogue")
                .onEnter(gold(100), longRest())
                .npc("Dorrie Bellweather", "Landlady of the Barnacle & Bell", "#e67e22", "🔔", "Well don't just stand there dripping on my floor, heroes! Get in here! Drinks are on the house. Forever. Within reason. Mostly.")
                .text("The fog is gone. It went the moment the crown broke, and the sea off Gullhaven is blue again — just ordinary, wonderful, fish-smelling blue. That night, the whole town crams into the Barnacle & Bell, and Dorrie rings the great bell until her arms give out.",
                        "Eleven crews are home. Odessa Vane holds her son so tightly he squeaks, and she doesn't let go all evening. Grunda gets the *Gannet*'s crew back and immediately starts shouting at them. The Fenwick Brothers have written a new shanty, *\"The Ballad of the Drowned Crown\"*. It has forty-one verses. Every one of them is about you.",
                        "The night is yours. And out past the harbour mouth, other seas are waiting.")
                .choice("Collect the extra reward Odessa promised").icon("💰").once().ifFlag("reward_up").effect(gold(50)).go("epilogue")
                .choice("Hear Odessa's toast").icon("⚓").once()
                    .effect(log("Odessa raises a glass and the room falls silent. \"To the ones who went into the fog,\" she says, \"and came back with all of ours.\" Then, more quietly, just to you: \"There'll always be a berth on any ship in Gullhaven for you. Always.\""))
                    .go("epilogue")
                .choice("Look out at the lighthouse").icon("🏮").once().ifFlag("keeper_ally")
                    .effect(log("Out on Gull Rock, the great lamp turns. For a moment you see a small figure in the lamp room window — an old man in a sou'wester, waving. Then the beam sweeps past, and he's gone. The light burns every night after, though nobody ever sees anyone tend it."))
                    .go("epilogue")
                .choice("A splash at the harbour wall").icon("🧜").once().ifFlag("merfolk_ally")
                    .effect(log("Ondine Kelpbraid surfaces by the quay with a crown of fresh coral for each of you. \"The reef is regrowing,\" she says. \"The reef remembers. Come swim with us sometime, air-breathers.\" She splashes Dorrie and vanishes, laughing."))
                    .go("epilogue")
                .choice("Find Wick by the fire").icon("🩹").once().ifFlag("wick_saved")
                    .effect(log("Wick's bandage is gone, and he's sitting with Tam and the Gannet's crew, telling the story of the brawl for the ninth time. It gets more heroic every time. He winks at you and doesn't stop."))
                    .go("epilogue")
                .choice("Wick walks in the door").icon("🩹").once().ifFlag("wick_taken")
                    .effect(log("The door bangs open and in walks Wick — soaked, grinning, alive. The Saltjacks had stashed him on the Widow's Wake with the rest, and he woke in the Pearl Gallery with everyone else. Grunda picks him up off the floor and refuses to put him down."))
                    .go("epilogue")
                .choice("Raise a toast and end the tale").icon("🏁").go("@end");
    }

    // =========================================================================================== battle maps
    static void encounters(Campaign c) {
        c.encounter("dock_brawl").title("Brawl at the Barnacle & Bell").backdrop("tavern").music("brawl").nonlethal()
                .intro("Tables overturn, chowder flies, and someone grabs a Fenwick. Nobody dies in a proper tavern brawl — but everyone gets hurt.")
                .surpriseIf("brawl_surprise")
                .ally("dc_mags", "mags_friend")
                .map("####################",
                     "#BB..B....###..BBB.#",
                     "#......j..###......#",
                     "#..BB..........BB..#",
                     "#..BB....s.....s...#",
                     "#.........E........#",
                     "#....B..........BB.#",
                     "#..PP....BB........#",
                     "#..PA....BB....s...#",
                     "#..PP..............#",
                     "#BB......BB....BBB.#",
                     "####################")
                .legend('j', "dc_jory").legend('s', "dc_smuggler").extra("dc_smuggler");

        c.encounter("dock_brawl_small").title("Brawl at the Barnacle & Bell").backdrop("tavern").music("brawl").nonlethal()
                .intro("With the hired muscle gone, it's you against Jory and his two most loyal (least clever) Saltjacks.")
                .surpriseIf("brawl_surprise")
                .ally("dc_mags", "mags_friend")
                .map("####################",
                     "#BB..B....###..BBB.#",
                     "#......j..###......#",
                     "#..BB..........BB..#",
                     "#..BB.........s....#",
                     "#.........E........#",
                     "#....B..........BB.#",
                     "#..PP....BB........#",
                     "#..PA....BB....s...#",
                     "#..PP..............#",
                     "#BB......BB....BBB.#",
                     "####################")
                .legend('j', "dc_jory").legend('s', "dc_smuggler").extra("dc_smuggler");

        c.encounter("low_tide").title("The Drowned Walk").backdrop("coast").music("battle")
                .intro("The drowned dead lurch out of the surf. Their grip is cold and strong — don't let them drag you into the water! (Wading through the sea is slow going.)")
                .map("~~~~~~~~~~~~~~~~~~~~~~",
                     "~~~~~~~,,~~~~~~~~~~~~~",
                     "~~~~,,,,,,,,~~~,,,d,~~",
                     ",,,,,,....,,,,,,,,,,~~",
                     ".PP.....B.......,,d,,~",
                     ".PP.......^......E..d~",
                     ",,,,,,....,,,,c,,,c,~~",
                     "~~~,,,,,,,,,,,,,,~~~~~",
                     "~~~~~~~~~,,~~~~d,d~~~~",
                     "~~~~~~~~~~~~~~~~~~~~~~")
                .legend('d', "dc_drowned").legend('c', "giant_crab").extra("dc_drowned");

        c.encounter("lighthouse").title("The Keeper's Wrath").backdrop("coast").music("battle")
                .intro("Amos Fell's ghost blazes with cold fury, and drowned wreckers shamble up from the stair. The great lamp in the middle of the room is scorching hot — keep clear of it, or shove a wrecker into it!")
                .map("#########..#########",
                     "####.....g.....#####",
                     "###.............####",
                     "##...#.......#...###",
                     "#...d...LL........##",
                     "#..d....LL......d..#",
                     "#.................E#",
                     "##...#.......#....##",
                     "###......PP......###",
                     "####....PPPP....####",
                     "#########..#########")
                .legend('g', "dc_keeper_ghost").legend('d', "dc_drowned").extra("dc_drowned");

        c.encounter("crabs").title("Clack Clack CLACK").backdrop("seacave").music("battle")
                .intro("Giant crabs scuttle over the smugglers' barrels, and Grandfather Snapper heaves himself out of his nest. Their claws grab and hold — fight back to back!")
                .map("######################",
                     "#~~~~,,,....~~~~~~~~~#",
                     "#~~~,,,..c.....,,....#",
                     "#~~,,,.......c...S...#",
                     "#,,,....##...........#",
                     "#.......##...B..c....#",
                     "#..^.........B.......#",
                     "#..........E...c.,,,,#",
                     "#.PP....~~~~.........#",
                     "#.PP...~~~~~~....,,,,#",
                     "######################")
                .legend('c', "giant_crab").legend('S', "dc_snapper").extra("giant_crab");

        c.encounter("sahuagin").title("Sea-Devils").backdrop("seacave").music("battle").objective("defeat:dc_tide_priestess")
                .intro("Sahuagin burst from the lagoon, and their tide-priestess begins a blood hymn. Silence the priestess and the war-party will flee!")
                .surpriseIf("sahuagin_ambush")
                .ally("dc_merfolk_ally", "sahuagin_ambush")
                .map("######################",
                     "#~~~~~~~~~~~~~~~~~~~~#",
                     "#~~~~~s~~~~~~~~~~~~~~#",
                     "#~~,,,,,,,,,,,,,,,,~~#",
                     "#,,....B.....p....,,,#",
                     "#.......s......s.....#",
                     "#..##..........##..E.#",
                     "#..##....,,,,..##....#",
                     "#........,,,,........#",
                     "#......PP..A.PP......#",
                     "###########..#########")
                .legend('s', "sahuagin").legend('p', "dc_tide_priestess").extra("sahuagin");

        c.encounter("deck").title("All Hands on Deck").backdrop("ship").music("battle")
                .intro("The Widow's drowned crew and her merrow bosun want you for their crew. The deck is open to the sea at the gaps in the rail — shove the dead overboard and they'll never climb back!")
                .surpriseIf("deck_surprise")
                .ally("dc_merfolk_ally", "merfolk_ally")
                .map("CCCCCCCCCCCCCCCCCCCCCCCC",
                     "CCCBBB.BBBBBB.BBBBBCCCCC",
                     "CCB.......d.......BBCCCC",
                     "CB...d........,....d.BCC",
                     "CB.....##.....M......BCC",
                     "C......##...........E.BC",
                     "CB.....##.....,,.....BCC",
                     "CB.PP......d....d...BBCC",
                     "CCBPP.A.........BB.BCCCC",
                     "CCCBBB.BBBB.BBBBBCCCCCCC",
                     "CCCCCCCCCCCCCCCCCCCCCCCC")
                .legend('d', "dc_drowned").legend('M', "dc_merrow").extra("dc_drowned");

        c.encounter("boss").title("Mother Brine, the Drowned Queen").backdrop("lair").music("boss").objective("defeat:dc_mother_brine")
                .intro("MOTHER BRINE rises from her throne. Three Coral Idols feed her stolen breath — while they stand, she heals every turn (cold can't touch them!). The Drowned Crown is a thing of storm: she is VULNERABLE TO LIGHTNING. Watch for the telegraphed tides!")
                .surpriseIf("boss_surprised")
                .weakenIf("brine_weakness")
                .ally("dc_keeper_ally", "keeper_ally")
                .ally("dc_merfolk_ally", "merfolk_ally")
                .phase(60, "Mother Brine shrieks and the sea above the palace bulges inward — a CRUSHING TIDE is coming down!",
                        Json.obj("op", "hazard", "count", 3, "type", "cold", "dice", "2d6", "name", "Crushing tide", "dc", 13),
                        Json.obj("op", "say", "text", "\"You'll drown, dearies! You'll drown and you'll SING for me!\""))
                .phase(30, "The drowned sailors of two centuries claw their way up through the coral floor to defend their queen!",
                        Json.obj("op", "summon", "monster", "dc_drowned", "count", 2),
                        Json.obj("op", "say", "text", "\"My crown... my lovely crown... NO! The sea is MINE!\""))
                .map("##########################",
                     "#~~~~~~~~~~~~~~~~~~~~~~~~#",
                     "#~~.........x..........~~#",
                     "#~.......,,,....,,,.....~#",
                     "#............b...........#",
                     "#........................#",
                     "#..x..................x.E#",
                     "#....~~~~~.......~~~~~...#",
                     "#....~~~~~.......~~~~~...#",
                     "#........................#",
                     "#.....,,,..........,,,...#",
                     "#........................#",
                     "#........PP....PP........#",
                     "#........PA....AP........#",
                     "##########......##########")
                .legend('b', "dc_mother_brine").legend('x', "dc_coral_idol").legend('d', "dc_drowned").extra("dc_drowned");
    }
}
