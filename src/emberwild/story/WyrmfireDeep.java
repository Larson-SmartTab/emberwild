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
 * WYRMFIRE DEEP: THE DRAGON BENEATH BLACKSPIRE — a classic dungeon crawl that ends in a dragon's hoard.
 * <pre>
 *  Prologue   Blackspire & the Gilded Pickaxe .. a drinking song, Dragon's Hoard, a kobold nobody trusts, a rival party, a scuffle
 *  Chapter 1  The Upper Mines .................. a collapsing tunnel, giant vermin, a runaway mine cart, the Ashtail kobolds
 *  Chapter 2  The Halls of Khaz Morduin ........ a riddle door, a ghoul-haunted crypt, the Pendulum Hall, a flameskull, the Ancestor Forge
 *  Chapter 3  The Drowned Galleries ............ acid geysers, the Gutbreakers on the Great Bridge, High Wyrmspeaker Skarr
 *  Finale     The Hoard of Nyxathrax ........... the Black Mother: acid breath, geysers, egg-fed healing, kobold zealots
 * </pre>
 * Eight possible battles (several avoidable), six booby traps, 53 scenes, levels 3 → 5.
 * <p>
 * Things worth copying into your own tale:
 * <ul>
 *   <li>Flags that pay off later: {@code kobold_peace} gives you an ally in the dragon fight, {@code dragonbane} weakens the
 *       dragon ({@link Campaign.Encounter#weakenIf}), {@code skarr_surprise} lets you strike first ({@link Campaign.Encounter#surpriseIf}).</li>
 *   <li>A boss built in three acts with {@link Campaign.Encounter#phase}: geysers at 66%, summoned zealots at 33%.</li>
 *   <li>Destructible "Dragon Eggs" (role {@code object}) that heal a boss with the {@code fed_by_objects} trait.</li>
 * </ul>
 */
public final class WyrmfireDeep implements CampaignScript {

    // Chapter labels, kept in one place so every scene of a chapter matches exactly.
    static final String PRO = "Prologue";
    static final String CH1 = "Chapter 1 — The Upper Mines";
    static final String CH2 = "Chapter 2 — The Halls of Khaz Morduin";
    static final String CH3 = "Chapter 3 — The Drowned Galleries";
    static final String FIN = "Finale";
    static final String EPI = "Epilogue";

    @Override
    public Campaign build() {
        Campaign c = Campaign.create("wyrmfire_deep", "Wyrmfire Deep: The Dragon Beneath Blackspire", "The Emberwild Team");
        c.cover("hoard");
        c.synopsis("The mines of Blackspire have flooded with acid, the miners are vanishing one shift at a time, and the kobolds "
                + "whisper about \"the Black Mother\" as if she might be listening. She is. From a rowdy dwarven tavern down through "
                + "collapsing tunnels, the lost halls of Khaz Morduin and the drowned acid galleries, a band of heroes must reach the "
                + "hoard of NYXATHRAX, a black dragon who has decided Blackspire is her larder. "
                + "Built for 1-4 players starting at level 3 (rising to level 5); companions fill a small party.");

        // Other tales can offer this dungeon from their own victory feast.
        c.hook("epilogue", "A dwarf foreman at the feast is hiring heroes — something enormous is nesting under Blackspire", "start");

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
        // ---- A tiefling scholar who has written the definitive guide to dungeons. She has never been in one.
        Hero footnote = hero("footnote", Json.obj(
                "name", "Footnote Ashgrave", "pronouns", "she/her", "class", "wizard", "species", "tiefling", "lineage", "infernal", "background", "sage",
                "method", "pointbuy", "scores", scores(8, 14, 14, 15, 12, 8), "increases", Json.obj("int", 2, "con", 1),
                "skills", List.of("arcana", "history", "investigation", "insight"),
                "cantrips", List.of("ray_of_frost", "shocking_grasp", "minor_illusion"),
                "spells", List.of("mage_armor", "shield", "charm_person", "magic_missile"),
                "color", "#6c5ce7", "emblem", "📚", "hair", "#20132e", "skin", "#9b4d6e", "eyes", "#ffd166",
                "appearance", "Curled horns hung with brass reading-lamps, ink up to both elbows, and a satchel so stuffed with notes it has its own centre of gravity.",
                "personality", "Cheerful, precise and catastrophically over-prepared. Corrects monsters' grammar mid-fight. Cites her own book as a primary source.",
                "backstory", "Footnote Ashgrave is the author of *A Gentlewizard's Guide to Dungeons (Unabridged, Eleven Volumes)*, the standard text on traps, tombs and treasure. Her publisher has started asking awkward questions about her field research. When word came that a real dragon was nesting beneath Blackspire, she packed eleven volumes, one quill, and absolutely no rope.",
                "motto", "It's all in Volume Four. Probably."));
        footnote.spellbook.clear();
        footnote.spellbook.addAll(List.of("mage_armor", "shield", "charm_person", "magic_missile", "sleep", "ice_knife"));
        c.pregen(footnote);

        // ---- A black-scaled dragonborn paladin with a deeply embarrassing family tree.
        c.pregen(hero("sulvara", Json.obj(
                "name", "Sulvara Inkscale", "pronouns", "she/her", "class", "paladin", "species", "dragonborn", "lineage", "black", "background", "soldier",
                "method", "pointbuy", "scores", scores(15, 8, 14, 8, 12, 14), "increases", Json.obj("str", 2, "con", 1),
                "skills", List.of("athletics", "persuasion", "intimidation"),
                "color", "#2d3436", "emblem", "🛡", "hair", "#1e272e", "skin", "#2f3e2f", "eyes", "#a3cb38",
                "appearance", "Seven feet of glossy black scales, a polished tower shield, and a jaw that drips a little acid when she's nervous.",
                "personality", "Noble, earnest and terrified that the dragon under Blackspire is her great-great-great-grand-auntie. Has rehearsed an apology to the whole world, just in case.",
                "motto", "Family reunions are overrated.")));

        // ---- A halfling trap-finder who finds every trap. Usually with his face.
        c.pregen(hero("tobble", Json.obj(
                "name", "Tobble \"Tripwire\" Underbough", "pronouns", "he/him", "class", "rogue", "species", "halfling", "lineage", "lightfoot", "background", "criminal",
                "method", "pointbuy", "scores", scores(8, 15, 14, 13, 12, 10), "increases", Json.obj("dex", 2, "con", 1),
                "skills", List.of("perception", "investigation", "acrobatics", "deception"),
                "color", "#e17055", "emblem", "🪤", "hair", "#c97b3a", "skin", "#e8b48a", "eyes", "#2d98da",
                "appearance", "A tiny man in a patched leather coat with forty-one pockets, singed eyebrows and a magnificent collection of bandages.",
                "personality", "Unflappable, lucky in all the wrong ways, and genuinely proud of his scars. Keeps a tally of traps triggered on the inside of his hat.",
                "motto", "Found it!")));

        // ---- Companions: AI-controlled, they join only when the party is small.
        Hero hulda = hero("hulda", Json.obj(
                "name", "Hulda Deepdelve", "pronouns", "she/her", "class", "fighter", "species", "dwarf", "lineage", "hill", "background", "guard",
                "method", "pointbuy", "scores", scores(15, 13, 14, 8, 12, 10), "increases", Json.obj("str", 2, "wis", 1),
                "skills", List.of("athletics", "perception"),
                "color", "#a0522d", "emblem", "⛏", "hair", "#b5651d", "skin", "#d29b78", "eyes", "#4b3621",
                "appearance", "A square, soot-streaked miner with braids like hawsers, a dented lamp-helm and a notched old longsword named Grandma (it was her grandma's).",
                "personality", "Thirty years underground. Counts everything: coins, steps, enemies. Trusts stone more than people, and kobolds least of all.",
                "motto", "Stone remembers. So do I."));
        hulda.npc = true;
        c.pregen(hulda);
        Hero fizzwick = hero("fizzwick", Json.obj(
                "name", "Fizzwick Lanternbottom", "pronouns", "he/him", "class", "cleric", "species", "gnome", "lineage", "rock", "background", "acolyte",
                "method", "pointbuy", "scores", scores(12, 14, 13, 8, 15, 10), "increases", Json.obj("wis", 2, "cha", 1),
                "skills", List.of("medicine", "religion"),
                "cantrips", List.of("sacred_flame", "toll_the_dead", "guidance"),
                "spells", List.of("healing_word", "guiding_bolt", "bless", "cure_wounds"),
                "color", "#fdcb6e", "emblem", "🏮", "hair", "#f5f6fa", "skin", "#f0c9a8", "eyes", "#0984e3",
                "appearance", "A white-bearded gnome priest of the Lantern Saint, patron of miners, with a brass censer that doubles as a very small lamp.",
                "personality", "Endlessly sunny. Blesses everything: weapons, boots, suspicious mushrooms. Believes every dark place is just a light that hasn't arrived yet.",
                "motto", "Lamps lit, chins up, onward and downward!"));
        fizzwick.npc = true;
        c.pregen(fizzwick);
    }

    // =========================================================================================== monsters
    static void monsters(Campaign c) {
        // ---- The Golden Spurs, a rival treasure-hunting company (brawl versions, then a friendly version).
        c.monster("wd_glint", "Sir Percival Glint").type("humanoid").role("melee").ac(16).hp(36).stats(15, 12, 13, 10, 9, 15).cr(1, 200)
                .multi(2).attack("Gilded Gauntlet", 4, "1d8+2", "bludgeoning")
                .look("#f1c40f", "✨").blurb("Leader of the Golden Spurs. His armour has never been dirty. He intends to keep it that way.");
        c.monster("wd_rival_bruiser", "Bran Oakhart").type("humanoid").role("brute").ac(12).hp(26).stats(17, 10, 15, 8, 10, 9).cr(0.5, 100)
                .multi(2).attack("Hug of Doom", 5, "1d6+3", "bludgeoning")
                .look("#27ae60", "💪").blurb("The Golden Spurs' muscle. Desperately wants everyone to get along. Will hit you until you do.");
        c.monster("wd_rival_archer", "Quillon Vey").type("humanoid").role("ranged").ac(13).hp(18).stats(9, 16, 12, 12, 13, 11).cr(0.5, 100)
                .attack("Elbow", 3, "1d4+1", "bludgeoning").attack(new Monsters.Attack("Blunted Arrow", 5, "1d6+3", "bludgeoning").ranged(60, 120))
                .trait("avoid_melee").look("#16a085", "🏹").blurb("The company's elf archer and official chronicler. Narrates your defeat in real time.");
        c.monster("wd_glint_ally", "Sir Percival Glint").type("humanoid").role("melee").ac(16).hp(40).stats(16, 12, 14, 10, 10, 15).cr(2, 0)
                .multi(2).attack("Gleaming Longsword", 5, "1d8+3", "slashing")
                .look("#f1c40f", "✨").blurb("The Golden Spurs ride with you! Well, walk. The horses refused.");

        // ---- Kobolds of the Ashtail clan and the Black Mother's chosen.
        c.monster("wd_kobold_dragonshield", "Kobold Dragonshield").type("humanoid").role("melee").ac(15).hp(18).stats(12, 15, 12, 8, 9, 10).cr(1, 200)
                .multi(2).attack("Spear", 4, "1d6+2", "piercing").trait("pack_tactics")
                .look("#556b2f", "🛡").blurb("Carries a shed dragon scale as a shield and an unshakeable sense of destiny.");
        c.monster("wd_kobold_zealot", "Kobold Zealot").type("humanoid").role("skirmisher").ac(12).hp(10).stats(8, 15, 10, 8, 8, 10).cr(0.25, 50)
                .attack(new Monsters.Attack("Acid-Dipped Dagger", 4, "1d4+2", "piercing").plus("1d4", "acid")).trait("pack_tactics")
                .look("#4b5320", "🦎").blurb("Has painted MOTHER on its own forehead. Backwards.");
        c.monster("wd_skritch_ally", "Skritch & the Ashtail Clutch").type("humanoid").role("ranged").ac(13).hp(30).stats(8, 16, 12, 10, 12, 10).cr(1, 0)
                .multi(2).attack("Many Little Daggers", 5, "1d4+3", "piercing").attack(new Monsters.Attack("Sling Volley", 5, "1d6+3", "bludgeoning").ranged(30, 120))
                .look("#e67e22", "🦎").blurb("A dozen kobolds moving as one. Skritch is in charge. Mostly.");
        c.monster("wd_skarr", "High Wyrmspeaker Skarr").type("humanoid").role("boss").ac(15).hp(60).stats(8, 15, 14, 12, 14, 16).cr(2, 450)
                .saves("wis", "cha").multi(2)
                .attack("Fang-Tipped Staff", 4, "1d6+2", "piercing")
                .attack(new Monsters.Attack("Black Mother's Spit", 5, "2d6", "acid").ranged(60, 60).spell())
                .special(Monsters.special("acid_spray", "Acid Spray").shape("cone", 15, 0).dmg("3d6", "acid").dc(12, Ability.DEX).recharge(5))
                .special(Monsters.special("scale_mend", "Scale-Mend Chant").shape("heal", 0, 0).dmg("2d6+2", "").uses(2).bonus())
                .look("#8e44ad", "🪬").blurb("The Black Mother's voice, priest and favourite. Cruel, clever, and wearing a crown of her shed teeth.");

        // ---- Halls of Khaz Morduin.
        c.monster("wd_forge_skull", "Durgrim's Burning Skull").type("undead").role("caster").ac(13).hp(46).speed(0).fly(40).stats(1, 17, 14, 16, 10, 11)
                .cr(3, 700).multi(2).resist("lightning", "necrotic", "piercing").immune("fire", "poison")
                .condImmune("charmed", "frightened", "paralyzed", "poisoned", "prone")
                .attack(new Monsters.Attack("Forge Ray", 5, "2d6", "fire").ranged(30, 30).spell())
                .special(Monsters.special("forge_blast", "Forge Blast").shape("sphere", 15, 90).dmg("5d6", "fire").dc(13, Ability.DEX).recharge(5))
                .trait("avoid_melee").look("#ff7043", "💀").blurb("The skull of the last forgemaster, still guarding his forge. Still angry about it.");

        // ---- NYXATHRAX, the Black Mother: built on the bestiary's young black dragon, tuned for a level-5 party.
        c.monster("wd_nyxathrax", "Nyxathrax, the Black Mother").type("dragon").role("boss").size(2).ac(17).hp(105, "14d10+28").speed(40).fly(80)
                .stats(19, 14, 17, 12, 11, 15).cr(5, 1800).saves("dex", "con", "wis", "cha")
                .immune("acid").condImmune("frightened", "charmed").multi(2)
                .attack(new Monsters.Attack("Bite", 7, "2d6+4", "piercing").plus("1d8", "acid").reach(10))
                .attack(new Monsters.Attack("Claw", 7, "2d6+4", "slashing"))
                .special(Monsters.special("acid_breath", "Acid Breath").shape("line", 60, 60).dmg("8d8", "acid").dc(14, Ability.DEX).recharge(6))
                .special(Monsters.special("dread_roar", "Dread Roar").shape("sphere", 30, 0).dmg("2d6", "thunder").dc(13, Ability.WIS).rider("frightened").uses(1))
                // The combat AI saves the special with id "wing_gust" for legendary actions instead of spamming it every turn,
                // so the tail sweep borrows that id. Its display name is still "Tail Sweep".
                .special(Monsters.special("wing_gust", "Tail Sweep").shape("sphere", 10, 0).dmg("1d8+4", "bludgeoning").dc(13, Ability.DEX).rider("prone"))
                .trait("legendary_resistance", "fed_by_objects")
                .legendary(2, "special:wing_gust", "attack:Claw", "move")
                .look("#1e3a1e", "🐉").blurb("A black dragon the length of a mine-cart train, soaked in acid and spite. She calls the miners her 'little polishers'.");
        c.monster("wd_dragon_egg", "Black Dragon Egg").type("object").role("object").ac(13).hp(12).speed(0).stats(10, 1, 14, 1, 1, 1)
                .cr(0, 25).immune("acid", "poison", "psychic").condImmune("charmed", "frightened", "poisoned", "paralyzed", "prone", "incapacitated")
                .attack("None", 0, "0", "acid").look("#2c3e50", "🥚").blurb("A glossy black egg the size of a barrel. It pulses — and the Black Mother pulses with it. Smash it!");
    }

    // =========================================================================================== items
    static void items(Campaign c) {
        c.item("wd_wyrmbane", "Wyrmbane, the Coalbrow Blade", "weapon",
                "Star-iron forged in the Ancestor Forge and quenched in dwarven ale. +1 to weapon attack and damage rolls; dragons can feel it coming.",
                Json.obj("bonus", Json.obj("attack", 1, "damage", 1), "attunable", true, "value", 600));
        c.item("wd_acidglass_mantle", "Acid-Glass Mantle", "wondrous",
                "A cloak of woven dwarven glass-thread that sheds acid like rain. +1 to Armor Class and saving throws.",
                Json.obj("bonus", Json.obj("ac", 1, "save", 1), "attunable", true, "value", 500));
        c.item("wd_coalbrow_signet", "Coalbrow Signet", "quest",
                "Foreman Dagna's family ring, stamped with the anvil of Khaz Morduin. Old dwarven doors and older dwarven ghosts respect it.", null);
        c.item("wd_lucky_rock", "Skritch's Lucky Rock", "trinket",
                "A smooth grey pebble. Skritch swears it is extremely lucky. It has a face drawn on it.", Json.obj("value", 1));
    }

    // =========================================================================================== prologue
    static void prologue(Campaign c) {
        c.scene("start").chapter(PRO).title("The Road to Blackspire").backdrop("town").weather("ash").time("dusk").music("explore")
                .onEnter(level(3))
                .text("BLACKSPIRE clings to the side of its mountain like a barnacle with ambitions: crooked stone houses, chimneys coughing coal-smoke, and above it all the black spike of rock that gave the town its name. For three hundred years the dwarves and humans here have dug silver out of the mountain's belly.",
                        "Lately, the mountain has started digging back. The stream that runs through town hisses where it touches stones. Dead fish float belly-up, half-dissolved. The mine whistle shrieks at all hours, and every time it does, people stop in the street and count each other.",
                        "At the end of the high street, warm light spills from a tavern with a gilded pick nailed over the door: THE GILDED PICKAXE. Someone inside is singing about beards. Loudly.")
                .choice("Head for the Gilded Pickaxe").icon("🚪").go("tavern_arrive")
                .choice("Kneel and study the hissing stream").icon("🧪").check("nature", 12)
                    .pass("You dip a stick into the water. It comes out smoking, the wood pitted and black. This isn't mine runoff — it's *acid*, thick and oily, the kind that only one creature in the world breathes. Somewhere under this mountain is a black dragon. A big one.", "tavern_arrive", flag("acid_lore"), xp(50))
                    .fail("You dip a finger in. That was a mistake. You spend the walk to the tavern blowing on it.", "tavern_arrive", damageOne("1d4", "acid"));

        c.scene("tavern_arrive").chapter(PRO).title("The Gilded Pickaxe").backdrop("tavern").time("night").music("tavern")
                .onEnter(companion("hulda", 3), companion("fizzwick", 3))
                .npc("Gretta Mattock", "Landlady of the Gilded Pickaxe", "#c0392b", "🍺", "Boots off the tables, picks on the hooks, and if you've come about the dragon, you'll want a drink first.")
                .text("The Gilded Pickaxe is a cave someone turned into a tavern and then forgot to stop. Lanterns hang from old mine-props, the bar is a single slab of slate, and the walls are covered in pickaxes — each one, says a plaque, belonging to a miner who never came back up. There are a lot of new pickaxes.",
                        "Behind the bar, GRETTA MATTOCK — a broad half-orc with flour on her tusks — pulls ale with both hands. A table of dwarven miners is singing. A gnome card-shark is fleecing anyone who sits down. And by the hearth, a party of gleaming adventurers in matching gold capes is making a terrible fuss over something small and scaly.",
                        "At the end of the bar, a dwarf in a soot-black miner's coat lifts her head. HULDA DEEPDELVE has been underground for thirty years and looks it. Next to her, a beaming gnome priest with a lantern-censer — FIZZWICK LANTERNBOTTOM — waves enthusiastically at you as if you were old friends.",
                        "(If your party is small, Hulda and Fizzwick decide to come along. \"Someone has to keep the surface-folk alive,\" grunts Hulda. \"Hooray!\" says Fizzwick.)")
                .choice("Find a table and take in the room").icon("🪑").go("tavern");

        c.scene("tavern").chapter(PRO).title("The Common Room").backdrop("tavern").time("night").music("tavern").anchor("tavern")
                .text("The Gilded Pickaxe roars along around you: mugs clank, dice rattle, and every so often the floor trembles as something far below shifts its weight. Nobody mentions it. Everybody notices.",
                        "There's plenty to do before the night is through — and somewhere in this room is the job that brought you here.")
                .choice("Join the miners' drinking song").icon("🎵").unlessFlag("song_done").go("song")
                .choice("Sit down for a round of Dragon's Hoard").icon("🎲").unlessFlag("hoard_done").go("hoard_game")
                .choice("Ask Gretta what's going on in the mines").icon("🍺").unlessFlag("gretta_done").go("gretta")
                .choice("Investigate the commotion by the hearth").icon("🦎").unlessFlag("skritch_met").hint("Someone small is in big trouble.").go("skritch")
                .choice("Share a table with the Golden Spurs").icon("✨").ifFlag("skritch_met").unlessFlag("rivals_done").go("rivals")
                .choice("Speak with Foreman Dagna Coalbrow").icon("⛏").ifFlag("skritch_met").unlessFlag("quest_accepted").go("dagna")
                .choice("Turn in for the night").icon("🌙").ifFlag("quest_accepted").go("market");

        // ---- The drinking song
        c.scene("song").chapter(PRO).title("Dig, Dig, Drink!").backdrop("tavern").time("night").music("tavern")
                .npc("Brokk Hammerhand", "Shift Captain & Choirmaster", "#7f8c8d", "🍻", "Ye can't sing it sober and ye can't sing it alone. Grab a mug!")
                .text("A dozen dwarves bang their mugs on the table in rhythm, roaring the old miners' song:",
                        "*\"Oh the mountain's got silver and the mountain's got gold, / And the mountain's got secrets a thousand years old — / So we DIG (dig!) and we DIG (dig!) till the lanterns run dry, / And we DRINK (drink!) to the lads who'll not see the sky!\"*",
                        "The last line lands heavy. A few miners look at the wall of pickaxes. Then BROKK HAMMERHAND, a grey-bearded slab of a dwarf, slams down a tankard the size of a bucket and grins at you. \"Your turn, surface-folk.\"")
                .choice("Sing the next verse — loud and proud").icon("🎵").check("performance", 12)
                    .pass("You invent a verse on the spot about a dragon who choked on a dwarf's beard. The table *explodes*. Brokk weeps with laughter and hammers you on the back. \"That's going in the song! Forever!\" (Heroic Inspiration)", "tavern", inspiration(), flag("song_done"), attitude("miners", 1))
                    .fail("You hit a note that makes a lantern crack. There's a horrible silence — then Brokk roars with laughter and makes you drink the forfeit. It's terrible ale. You're one of them now.", "tavern", flag("song_done"), attitude("miners", 1))
                .choice("Drain the Bottomless Tankard in one go").icon("🍺").check("con", 13)
                    .pass("You tip it back… and back… and back. When you slam it down empty, the whole table is standing on its chairs chanting. Brokk presses his own lucky silver into your palm. \"Ye've the gut of a dwarf!\"", "tavern", gold(15), flag("song_done"), attitude("miners", 1))
                    .fail("About halfway through, the tankard wins. You spend a short while getting to know the floor. The miners tuck a cushion under your head, which is honestly very sweet.", "tavern", flag("song_done"))
                .choice("Applaud and move on").icon("↩").effect(flag("song_done")).go("tavern");

        // ---- Dragon's Hoard (liar's dice with coins and cups)
        c.scene("hoard_game").chapter(PRO).title("Dragon's Hoard").backdrop("tavern").time("night").music("tavern")
                .npc("Lady Fortuna Grubb", "Gnome Card-Shark (Retired, Allegedly)", "#16a085", "🎲", "Everyone hides a hoard, everyone claims a hoard, and the biggest liar is the dragon. Ten gold to play, darling.")
                .text("LADY FORTUNA GRUBB is a gnome in a velvet waistcoat with more rings than fingers. Each player hides coins under a copper cup and bids on how much gold lies under ALL the cups. Call a bluff and you take the pot; get it wrong and you pay double.",
                        "\"The dragon,\" she explains sweetly, \"never shows her hoard. Neither should you.\" She has won eleven games in a row. Nobody can work out how.")
                .choice("Wager 10 gp and trust your luck").icon("🍀").ifGold(10).check("luck", 11)
                    .effect(gold(-10))
                    .pass("Fortuna calls your bid a bluff. You lift your cup: every coin you claimed is there, and then some. The table gasps. Fortuna pays out 30 gold with the smile of someone mentally rewriting her will.", "tavern", gold(30), flag("hoard_done"))
                    .fail("Your bid is bold. Fortuna's is bolder. She lifts her cup on a gleaming pile and sweeps your coins away. \"The dragon always knows,\" she purrs.", "tavern", flag("hoard_done"))
                .choice("Bluff outrageously").icon("🎭").ifGold(10).check("deception", 13)
                    .effect(gold(-10))
                    .pass("You claim a hoard so enormous it should be impossible. Fortuna studies your face for a long, long moment… and folds. Under your cup: two coppers and a button. The table howls. Fortuna laughs until she cries and pays you 30 gold. \"Oh, I *like* you.\"", "tavern", gold(30), flag("hoard_done"), flag("fortuna_friend"))
                    .fail("You blink. Twice. Fortuna's eyes light up like a cat's. \"Bluff,\" she says, and she's right. You pay double.", "tavern", gold(-5), flag("hoard_done"))
                .choice("Watch Fortuna's hands instead of the cups").icon("👁").ifGold(10).check("insight", 14)
                    .effect(gold(-10))
                    .pass("There — every time she lifts her cup, a coin slides out of her ring. A trick ring! You lean over and whisper, \"Lovely ring.\" Fortuna freezes, then slides you 25 gold and a note: *Ask me about the Black Mother sometime.* She knows more than she lets on.", "tavern", gold(25), flag("hoard_done"), flag("fortuna_friend"), flag("know_eggs"))
                    .fail("You watch her hands so hard you forget to watch the game. You lose. Of course you lose.", "tavern", flag("hoard_done"))
                .choice("Just watch for a while").icon("↩").go("tavern");

        // ---- Gretta and the rumours
        c.scene("gretta").chapter(PRO).title("Gretta's Bar").backdrop("tavern").time("night").music("tavern")
                .npc("Gretta Mattock", "Landlady of the Gilded Pickaxe", "#c0392b", "🍺", "Twelve miners gone in a month. Twelve. And the Foreman's own brother last of all.")
                .text("Gretta slides a mushroom pie across the slate. \"Eat. You look like you're about to do something stupid, and nobody should do something stupid hungry.\"",
                        "\"It started with the acid. Water in the deep tunnels turned black and started eating boots. Then the kobolds came up out of the lower mines, chattering about their *Black Mother* this and *Black Mother* that. Then miners started going missing. Not dead, mind. No bodies. Just — gone, picks and all.\"")
                .choice("Ask about the Black Mother").icon("🐉").once()
                    .effect(flag("know_mother"), log("Gretta lowers her voice. \"The kobolds say she came up from the deep sea-caves, following the old dwarf halls. Black as pitch, breath like a tannery fire. They say she makes the miners *polish her gold*. Every coin. Forever.\" She shudders. \"Worst job in the world, and I've worked a pie shop at harvest.\""))
                    .go("gretta")
                .choice("Charm her for the good gossip").icon("💗").once().check("persuasion", 12)
                    .pass("Gretta snorts, but she's smiling. \"Fine. You want gossip? The old dwarf halls — Khaz Morduin — had a forge that could make dragon-killing steel. Every miner's grandmother tells it. Here.\" She presses a bottle into your hand. \"Pie's on me too.\"", "gretta", flag("know_forge"), item("potion_healing"), attitude("gretta", 1))
                    .fail("\"Nice try, sweetheart. Gossip's for regulars.\" She refills your mug anyway.", "gretta")
                .choice("Buy a round for the whole house (10 gp)").icon("🍻").once().ifGold(10)
                    .effect(gold(-10), inspiration(), attitude("miners", 1), log("A roar goes up. \"To the strangers!\" Forty mugs rise. For a moment the trembling floor doesn't matter at all. (Heroic Inspiration!)"))
                    .go("gretta")
                .choice("Head back into the common room").icon("↩").effect(flag("gretta_done")).go("tavern");

        // ---- Skritch, the kobold nobody trusts
        c.scene("skritch").chapter(PRO).title("A Kobold in the Corner").backdrop("tavern").time("night").music("tension")
                .npc("Sir Percival Glint", "Leader of the Golden Spurs", "#f1c40f", "✨", "Stand aside, citizen. This vermin is a SPY of the dragon, and the Golden Spurs are dispensing JUSTICE.")
                .text("By the hearth, a kobold no taller than your belt is backed against the wall, clutching a sack of pebbles. His scales are ash-grey, his tail is bandaged, and he is shaking so hard his teeth click.",
                        "Looming over him are THE GOLDEN SPURS: SIR PERCIVAL GLINT, a square-jawed human in spotless gilded plate; BRAN OAKHART, an enormous half-orc who looks deeply uncomfortable; and QUILLON VEY, an elf archer scribbling in a notebook. \"*And the brave Spurs cornered the foul beast,*\" Quillon narrates.",
                        "\"I am not spy!\" squeaks the kobold. \"I am SKRITCH! I am refugee! I sell lucky rocks!\" He holds one up. It has a face drawn on it.")
                .choice("Step between them. \"Pick on someone your own size.\" (Tavern scuffle — no killing)").icon("⚔").battle("brawl")
                    .victory("Sir Percival lands on his back in a spray of ale, staring at the ceiling. \"…My armour,\" he whispers. \"There is *beer* on my armour.\" The tavern roars. Bran helps him up, looking secretly relieved. Quillon is still writing: \"*…and the Spurs were bested, but only technically.*\"", "skritch_after", flag("skritch_met"), flag("brawl_won"), attitude("skritch", 2))
                    .defeat("You wake up under a table with Fizzwick patting your cheek. The Spurs have swaggered off, but Skritch is still there, crouched beside you with a wet rag. \"You fought for Skritch,\" he says, amazed. \"Nobody fights for Skritch.\"", "skritch_after", flag("skritch_met"), attitude("skritch", 2))
                .choice("Talk Glint down with honeyed words").icon("💬").check("persuasion", 13)
                    .pass("\"Sir Percival, surely a knight of your *renown* doesn't need to bully something the size of a boot? Think how it'd look in the chronicle.\" Quillon's quill pauses. Glint considers his reflection in his gauntlet, then sniffs. \"…Quite right. Beneath us. Carry on, citizen.\" The Spurs retire to their table, and Glint even nods at you with grudging respect.", "skritch_after", flag("skritch_met"), flag("rivals_respect"), attitude("skritch", 1))
                    .fail("\"Are you questioning the HONOUR of the Golden Spurs?\" Glint draws himself up. Bran sighs and cracks his knuckles. \"Sorry,\" he mumbles. \"He gets like this.\"", "skritch_brawl")
                .choice("Charm Sir Percival (Charm Person)").icon("💗").spell("charm_person")
                    .pass("Your magic settles over Glint like warm sunlight. He blinks at you, then at the kobold, then at his own raised fist. \"Good heavens. What am I doing? You're absolutely right, my dear friend.\" He pats Skritch on the head and buys him a drink. (When the charm wears off he'll be confused but, oddly, still fond of you.)", "skritch_after", flag("skritch_met"), flag("rivals_respect"), attitude("skritch", 1))
                .choice("Stay out of it").icon("↩")
                    .effect(flag("skritch_met"), flag("skritch_snubbed"), log("You turn back to your drink. Behind you there's a thump, a squeak, and the door banging. When you look again, the kobold is gone — and the Spurs are toasting their 'victory'."))
                    .go("tavern");

        c.scene("skritch_brawl").chapter(PRO).title("Chairs Will Fly").backdrop("tavern").time("night").music("brawl")
                .text("Glint whips off his cape with a flourish. Bran rolls up his sleeves, apologising. Quillon nocks a blunted arrow and declares, \"*Chapter Nine: The Tavern Incident.*\"",
                        "Gretta sighs and moves the good mugs behind the bar.")
                .choice("Brawl!").icon("⚔").battle("brawl")
                    .victory("Glint ends up sitting in the hearth-ash with his helm on backwards. The tavern cheers. Bran helps everyone up, including you, and says \"Good fight!\" with real feeling.", "skritch_after", flag("skritch_met"), flag("brawl_won"), attitude("skritch", 2))
                    .defeat("You wake to Fizzwick's lantern in your face and Skritch dabbing your forehead with a wet rag. \"You fought for Skritch,\" he whispers. \"Nobody ever fights for Skritch.\"", "skritch_after", flag("skritch_met"), attitude("skritch", 2));

        c.scene("skritch_after").chapter(PRO).title("Skritch of the Ashtail").backdrop("tavern").time("night").music("tavern")
                .npc("Skritch", "Kobold Refugee & Rock Merchant", "#e67e22", "🦎", "You are big and kind. This is very confusing for Skritch. Here — free lucky rock.")
                .text("Skritch scrambles onto your table and presents you with a pebble. It has a face drawn on it. The face is smiling.",
                        "\"Skritch is from the Ashtail clan, in the upper mines,\" he explains, very fast. \"Skritch's people are not bad! We only dig and sing and eat beetles! But the Black Mother came and her Wyrmspeaker, SKARR, says all kobolds must serve her or be food. Skritch said no. So Skritch is refugee.\"",
                        "Hulda folds her arms. \"Kobolds lie like other folk breathe.\" Skritch looks at the floor.")
                .choice("\"We believe you, Skritch.\" (Earn his trust)").icon("🤝").check("insight", 12)
                    .pass("You look him in the eye and see it: not a spy, just a small, frightened person who has been brave for a very long time. Skritch's tail curls with joy. \"If you go into the mines,\" he whispers, \"Skritch will talk to Grandmother Ssiss. Ashtail will listen. Maybe.\"", "tavern", flag("skritch_trust"), item("wd_lucky_rock"), attitude("skritch", 2))
                    .fail("You *want* to believe him, but Hulda's suspicion is catching. Skritch notices. He leaves the pebble on the table anyway and slips away into the crowd.", "tavern", item("wd_lucky_rock"))
                .choice("Buy him a proper meal").icon("🍲").check("persuasion", 10)
                    .pass("Skritch eats three bowls of stew, a pie, and part of the bowl. Somewhere around the second helping he decides you are his best friends in the world. \"Skritch will help you! Skritch knows the Ashtail ways!\"", "tavern", flag("skritch_trust"), item("wd_lucky_rock"), gold(-2), attitude("skritch", 2))
                    .fail("He eats like a starving thing, mumbles thanks, and vanishes before you can ask anything else.", "tavern", item("wd_lucky_rock"), gold(-2))
                .choice("Leave him to it").icon("↩").go("tavern");

        // ---- The Golden Spurs
        c.scene("rivals").chapter(PRO).title("The Golden Spurs").backdrop("tavern").time("night").music("tavern")
                .npc("Sir Percival Glint", "Leader of the Golden Spurs", "#f1c40f", "✨", "Ah. *You.* Sit, sit. The Spurs are always gracious to the competition. Briefly.")
                .text("The Golden Spurs have the best table in the house and have paid extra for it. Sir Percival polishes his breastplate with a silk cloth. Bran is building a tiny tower out of sugar lumps. Quillon looks up from his notebook. \"*Enter: the competition,*\" he murmurs.",
                        "\"We're after the dragon too, naturally,\" says Glint. \"Fame. Glory. A hoard the size of a cathedral. There's only room for one company of heroes in this ballad, and frankly, our capes match.\"")
                .choice("Propose an alliance: split the hoard, share the glory").icon("🤝").check("persuasion", 13).advantageIf("brawl_won")
                    .pass("Glint opens his mouth to refuse. Bran kicks him under the table. Quillon whispers something about \"*a stirring tale of unlikely friendship.*\" Glint sighs. \"Very well. Should our paths cross below, the Spurs will fight beside you. But I get top billing.\"", "tavern", flag("rivals_friends"), flag("rivals_done"), attitude("glint", 2))
                    .fail("\"Share?\" Glint laughs like you told a joke at a funeral. \"The Spurs do not *share*. We'll see you down there — if you make it past the front door.\"", "tavern", flag("rivals_done"), flag("rivals_annoyed"))
                .choice("Challenge Bran to an arm-wrestle for bragging rights").icon("💪").check("athletics", 14)
                    .pass("Bran's arm is a tree trunk. Yours is, briefly, a slightly stronger tree trunk. SLAM! Bran stares at his hand, then bursts into happy tears and hugs you. \"You're the strongest friend I've ever had!\" Glint sighs. \"Fine. FINE. Allies.\"", "tavern", flag("rivals_friends"), flag("rivals_done"), attitude("glint", 1))
                    .fail("Bran pins you gently and then apologises for three minutes. \"You were very good,\" he insists. Glint smirks. \"The Spurs win again.\"", "tavern", flag("rivals_done"))
                .choice("Prank Glint: swap his polish for boot-black").icon("😈").check("sleight_of_hand", 12)
                    .pass("By the time Glint notices, his breastplate is the colour of a coal-scuttle. Bran laughs so hard he falls off his chair. Quillon writes it down. Glint does not speak to you again. Ever. Probably.", "tavern", flag("rivals_done"), flag("rivals_annoyed"), inspiration())
                    .fail("Glint catches your wrist. \"Boot-black? On a Glint?\" He is genuinely hurt. The Spurs leave early, muttering.", "tavern", flag("rivals_done"), flag("rivals_annoyed"))
                .choice("Leave them to their sugar tower").icon("↩").go("tavern");

        // ---- The quest giver
        c.scene("dagna").chapter(PRO).title("Foreman Dagna Coalbrow").backdrop("tavern").time("night").music("tension")
                .npc("Dagna Coalbrow", "Foreman of the Blackspire Mines", "#7f8c8d", "⛏", "I don't need heroes. I need my miners back. If that makes you heroes, fine.")
                .text("A dwarf with an eyepatch, a pipe, and soot ground permanently into the creases of her face sits alone by the wall of pickaxes. FOREMAN DAGNA COALBROW has run the Blackspire mines for twenty years. Tonight she looks every minute of it.",
                        "\"Twelve gone,\" she says, without preamble. \"My brother Bodric last of all. The deep tunnels are flooding with something that eats iron, the kobolds have gone mad, and the Baron's men won't go below the third level. I've seen you scrap. You'll do.\"",
                        "She drops a heavy purse on the table. \"A hundred gold apiece now, and the Coalbrow share of whatever's down there if you bring my people home. Below the upper mines are the old halls of Khaz Morduin — my ancestors' halls. Whatever's down there came up through them.\"")
                .choice("\"We'll bring them home.\" Accept the job").icon("⛏").effect(flag("quest_accepted"), gold(100), xp(100)).go("tavern")
                .choice("Ask for something from her ancestors' halls").icon("💍").check("history", 13)
                    .pass("\"Khaz Morduin had doors that only opened for Coalbrows,\" you say. Dagna studies you a long moment, then pulls a heavy iron ring from her finger. \"My grandmother's signet. The old doors will know it. So will the old ghosts.\" She grips your hand. \"Bring it back. Bring *him* back.\"", "tavern", flag("quest_accepted"), item("wd_coalbrow_signet"), flag("coalbrow_signet"), gold(100), xp(100))
                    .fail("\"The halls have been dead three hundred years,\" Dagna says. \"Nothing of mine will help you there.\" She shakes your hand anyway.", "tavern", flag("quest_accepted"), gold(100), xp(100))
                .choice("Haggle for hazard pay").icon("💰").check("persuasion", 14)
                    .pass("Dagna barks a laugh — the first one in weeks, by the look on Gretta's face. \"Ha! Cheeky. Fine.\" She adds two potions from the mine's infirmary. \"Don't make me regret it.\"", "tavern", flag("quest_accepted"), item("potion_healing"), item("potion_healing"), gold(100), xp(100))
                    .fail("\"I'm paying you with my brother's wages,\" she says quietly. You stop haggling.", "tavern", flag("quest_accepted"), gold(100), xp(100));

        c.scene("market").chapter(PRO).title("Blackspire Market").backdrop("town").time("day").weather("ash").music("explore").shop().anchor("town").checkpoint()
                .onEnter(longRest())
                .npc("Pell Brightwick", "Outfitter & Lamp-Seller", "#2e86c1", "🛒", "Going down, are you? Potions are three shelves left. Coffins are two shelves right. Joke! Mostly.")
                .text("You sleep in Gretta's tiny, pie-scented rooms — mostly. Some time after midnight the whole building shudders, and through the floorboards, impossibly deep, comes a sound like an enormous bellows drawing breath… then a long, low, satisfied *purr*. In the morning there is a crust of black, sizzling dew on every windowsill in Blackspire. (Long rest.)",
                        "Blackspire's market huddles around the mine-head under a sky the colour of an old kettle. Miners' wives sell pasties; a priest of the Lantern Saint blesses lamps; and PELL BRIGHTWICK's outfitter's cart groans with rope, lantern oil, healing potions and flasks of alchemist's fire.",
                        "Beyond the stalls, the mine-head gapes in the mountainside, its timbers stained black. A cold wind breathes out of it, smelling of vinegar and old coins.")
                .choice("Receive the Lantern Saint's blessing").icon("🏮").once()
                    .effect(inspiration(), heal("2d8"), log("Fizzwick's fellow priests hang a tiny lamp around your neck. \"May your light never fail in the deep.\" (Heroic Inspiration)"))
                    .go("market")
                .choice("Let Skritch draw you a map of the upper mines").icon("🗺").once().ifFlag("skritch_trust")
                    .effect(flag("skritch_map"), log("Skritch scratches a map on the back of a pie-tin with his claw. \"Old ventilation tunnel here — kobolds use it. No collapse, no rats. Shortcut! Skritch will meet you at the Ashtail warren.\""))
                    .go("market")
                .choice("Descend into the Blackspire mines").icon("⛏").go("mine_mouth");
    }

    // =========================================================================================== chapter 1
    static void chapterOne(Campaign c) {
        c.scene("mine_mouth").chapter(CH1).title("The Mouth of the Mine").backdrop("mine").time("day").music("explore")
                .npc("Dagna Coalbrow", "Foreman of the Blackspire Mines", "#7f8c8d", "⛏", "Main shaft's the fast way down. It's also the way the tunnels started falling in. Your call.")
                .text("Dagna walks you to the mine-head herself. Behind her, the miners of Blackspire stand in a silent crowd, lanterns in hand, watching you go where they won't.",
                        "Two ways lead down. The MAIN SHAFT drops steeply along the old cart-rails toward the third level, where the tunnels have been groaning for weeks. The OLD VENTILATION TUNNEL is narrow, tight, and — if Skritch's map is right — a kobold road.")
                .choice("Take the main shaft").icon("⬇").go("collapse")
                .choice("Squeeze down Skritch's ventilation tunnel").icon("🗺").ifFlag("skritch_map").go("vent")
                .choice("Rally the watching miners with a speech").icon("📣").once().check("persuasion", 12)
                    .pass("\"We'll bring them home,\" you tell the crowd, \"or we'll die trying — and we don't plan on dying.\" A cheer goes up, and old Brokk Hammerhand pushes through to press a flask of his best into your hands. \"For courage.\" It is mostly courage and partly paint-stripper. (Heroic Inspiration)", "mine_mouth", inspiration(), attitude("miners", 1))
                    .fail("Your speech is sincere, if a bit long. Somewhere near the end a miner starts snoring. Still, they clap.", "mine_mouth");

        c.scene("vent").chapter(CH1).title("The Ventilation Tunnel").backdrop("mine").time("night").music("explore")
                .text("Skritch's tunnel is barely wide enough for Hulda's shoulders and smells powerfully of kobold. But it's dry, it's quiet, and every few yards someone has scratched a tiny smiling face into the rock — Ashtail trail-marks.",
                        "It winds past the worst of the upper mines and lets out on a ledge above the old cart depot — just above a nest of something that skitters.")
                .choice("Drop down quietly and strike first").icon("🌑").check("stealth", 11).group()
                    .pass("You lower yourselves one by one, silent as falling soot. Below, the vermin haven't noticed a thing.", "cart_depot", flag("vermin_surprise"), xp(50))
                    .fail("Someone's boot scrapes the ledge. A pebble clatters down — and dozens of eyes swivel up toward you.", "cart_depot");

        c.scene("collapse").chapter(CH1).title("The Groaning Tunnel").backdrop("mine").time("night").music("tension")
                .text("Past the second level, the main shaft levels out into a long tunnel shored up with timber props. Every one of them is groaning. Dust trickles from the ceiling in thin, steady streams, like sand through an hourglass.",
                        "Halfway along, a fresh crack runs across the ceiling — and you notice something nasty: some of the props have been *gnawed*. Small teeth. Lots of them.")
                .choice("Spot which props are about to give").icon("🔍").check("investigation", 13)
                    .pass("Three props, gnawed nearly through, right where the crack is widest. You step around them like a dancer avoiding a sleeping dog. Behind you, as the last of you passes, one quietly snaps — and holds.", "cart_depot", xp(50))
                    .fail("They all look the same: old, groaning, terrifying. You pick a path and hope.", "collapse_trap")
                .choice("Shore up the worst props as you go").icon("🪵").check("athletics", 13)
                    .pass("You wedge fallen timbers under the gnawed props and hammer them home. The tunnel's groaning settles to a mutter. Hulda nods approvingly. \"Not bad. For a surface-dweller.\"", "cart_depot", xp(50))
                    .fail("You jam a timber into place — and the whole ceiling shifts with a horrible crunch.", "collapse_trap")
                .choice("Run for it!").icon("🏃").go("collapse_trap");

        c.scene("collapse_trap").chapter(CH1).title("Cave-In!").backdrop("mine").time("night").music("tension")
                .text("CRACK — CRACK — CRACK! The props go like a row of snapping fingers, and the ceiling comes down in a roaring avalanche of rock and dust!")
                .choice("Dive clear!").icon("🪨").check("save:dex", 13).each()
                    .pass("You fling yourself forward as the tunnel collapses behind you, and come up coughing, filthy and alive.", "cart_depot")
                    .fail("A slab of rock catches you as you dive. Your ears ring and the world goes white for a moment.", "cart_depot", damageOne("2d6", "bludgeoning"));

        c.scene("cart_depot").chapter(CH1).title("The Old Cart Depot").backdrop("mine").time("night").music("tension")
                .text("The tunnel opens into a cavern full of rusting mine-carts and rotting crates — the old cart depot, abandoned when the acid came. Thick grey webs drape the ceiling like wet laundry, and the floor is a carpet of gnawed bones and dropped pickaxe handles.",
                        "Something the size of a dog squeaks. Then something else, much larger, *clicks*. Rats — giant ones — boil out from under the carts, and above them, a bloated shape with too many eyes lowers itself on a silver thread.")
                .choice("Fight the vermin!").icon("⚔").battle("vermin")
                    .victory("The last rat flees squealing into a crack in the wall and the spider curls up like a dead hand. Among the webs you find a cocooned miner's satchel: a few coins, a healing potion, and a note scrawled in charcoal — *\"kobolds took Fenn and Ollie DOWN not out — they want us ALIVE\"*.", "cart_chase", item("potion_healing"), gold(15), flag("miners_alive"))
                    .defeat("You retreat into a side-tunnel, bleeding, while the rats squabble over your dropped rations. When they finally lose interest, the depot is clear enough to cross.", "cart_chase")
                .choice("Burn the webs with a Fire Bolt").icon("🔥").spell("fire_bolt")
                    .pass("The fire bolt hits the webs and they go up like paper lanterns — WHOOMPH! The spider drops out of the burning canopy, legs flailing, right into the middle of its own rats. They're in chaos. Strike now!", "cart_fight", flag("vermin_surprise"))
                .choice("Throw your rations to distract the rats").icon("🧀").check("animal_handling", 13)
                    .pass("You scatter rations in a wide arc and the rats dive on them, fighting each other more than you. Only the biggest few — and the spider — come at you, and they're distracted.", "cart_fight", flag("vermin_surprise"))
                    .fail("The rats eat the rations, and then look at you as if to say: *what else have you got?*", "cart_fight");

        c.scene("cart_fight").chapter(CH1).title("Squeak and Skitter").backdrop("mine").time("night").music("battle")
                .text("The depot erupts with squealing and clicking. Rats, webs, and eight hairy legs — here they come.")
                .choice("Fight!").icon("⚔").battle("vermin")
                    .victory("The last rat flees and the spider curls up dead. In the webs, a cocooned satchel: coins, a potion, and a charcoal note — *\"kobolds took Fenn and Ollie DOWN not out — they want us ALIVE\"*.", "cart_chase", item("potion_healing"), gold(15), flag("miners_alive"))
                    .defeat("You fall back, bloody, until the vermin lose interest. Eventually the way is clear.", "cart_chase");

        // ---- The runaway mine cart (group checks)
        c.scene("cart_chase").chapter(CH1).title("Runaway!").backdrop("mine").time("night").music("brawl")
                .text("A shrill whistle echoes from the far tunnel — kobold lookouts! Somewhere above, a lever goes *clunk*, and a wave of chittering laughter rolls down the shaft. A timber wall at the end of the depot swings open, and a wall of loose ore starts sliding toward you.",
                        "There's only one way out: the big ore-cart on the downhill rails. Its brake has been cut. Everyone in. NOW.")
                .choice("Everyone in — push off and jump aboard!").icon("🛒").check("athletics", 12).group()
                    .pass("You heave the cart into motion, sprint alongside, and pile in just as the ore-slide thunders past behind you. The cart picks up speed. A LOT of speed.", "cart_junction", xp(50))
                    .fail("The cart lurches off before everyone's aboard — you scramble in over the back, bruised and tangled, as gravel rains down on you.", "cart_junction", damage("1d6", "bludgeoning", "dex", 12));

        c.scene("cart_junction").chapter(CH1).title("The Junction").backdrop("mine").time("night").music("brawl")
                .text("The cart screams down the rails, sparks fountaining from the wheels. Ahead, the tunnel splits at a rusty junction lever — one line drops into darkness, the other veers left past a low, jagged beam. Kobolds on a ledge above are pelting you with rocks and cheering.",
                        "\"LEFT!\" yells Hulda. \"RIGHT!\" yells Fizzwick. \"*Chapter twelve,*\" you imagine Quillon Vey narrating, \"*in which everyone died.*\"")
                .choice("Hit the lever — and everybody DUCK!").icon("🙇").check("save:dex", 12).each()
                    .pass("You throw yourself flat as the beam whips past an inch above your skull.", "cart_crash")
                    .fail("The beam clips you across the shoulders. Stars explode behind your eyes.", "cart_crash", damageOne("1d8", "bludgeoning"))
                .choice("Read the old rail markings").icon("🔍").check("investigation", 13)
                    .pass("Dwarven rail-marks — a little anvil on the right-hand switch. *Safe line.* You kick the lever and the cart swings smoothly onto a track with no beams at all. Hulda grunts. \"Knew that.\"", "cart_crash", xp(50))
                    .fail("The marks are worn smooth. You guess left. The beam is very close and very hard.", "cart_crash", damage("1d8", "bludgeoning", "dex", 12))
                .choice("Zap the kobolds' ledge with Shocking Grasp as you pass").icon("⚡").spell("shocking_grasp")
                    .pass("You lean out and slap the iron rail-post as you rocket by. Lightning races up it into the kobolds' metal ledge — they leap, yelping, and drop the rock they were about to throw on the junction lever. The lever clunks over. Safe track. Everyone cheers, including, weirdly, one kobold.", "cart_crash", xp(50));

        c.scene("cart_crash").chapter(CH1).title("End of the Line").backdrop("mine").time("night").music("tension")
                .onEnter(shortRest())
                .text("The rails end at a heap of sand that someone, long ago, piled there for exactly this reason. The cart buries its nose in it and stops dead. You do not. You fly, land, roll, and come to rest in a heap at the edge of a vast cavern. For a while, nobody moves. You lie in the sand, bandaging scrapes and waiting for the world to stop spinning (short rest).",
                        "Ledges climb the cavern walls like the tiers of a theatre, honeycombed with tiny tunnels. Little fires flicker in them. Hundreds of yellow eyes blink at you from the dark. You have arrived — rather loudly — at the Ashtail warren.")
                .choice("Stand up slowly with your hands empty").icon("🖐").go("warren_edge")
                .choice("Scan the ledges for a way around").icon("👁").check("perception", 13)
                    .pass("There — a tripwire strung across the only path up, tied to a net of rocks. And beyond it, a ledge path along the cavern wall that the kobolds aren't watching.", "warren_edge", flag("tripwire_seen"))
                    .fail("Eyes. Lots of eyes. That's all you can make out.", "warren_edge");

        c.scene("warren_edge").chapter(CH1).title("The Ashtail Warren").backdrop("mine").time("night").music("goblin")
                .npc("Chieftain Grizzlefang", "Warleader of the Ashtail Kobolds", "#a0522d", "🦎", "INTRUDERS! The Black Mother hungers! The Black Mother will be PLEASED with us!")
                .text("A kobold in a helmet made from a dented cooking-pot leaps onto a rock: CHIEFTAIN GRIZZLEFANG, armed with a spear twice his height. Behind him, up on the ledges, slingers whirl their slings. A big kobold with a shield of shed black dragon scale steps forward, hissing.",
                        "But at the back, an ancient, wrinkled kobold with milky eyes sits wrapped in blankets, watching you — GRANDMOTHER SSISS. She hasn't said a word.")
                .choice("Let Skritch speak for you").icon("🦎").ifFlag("skritch_trust").go("parley")
                .choice("Speak to Grandmother Ssiss directly").icon("💬").check("persuasion", 15)
                    .pass("You bow low to the old kobold. \"Grandmother. We're not here for your people. We're here for the Black Mother.\" Ssiss's milky eyes narrow. Then she raps her stick on the rock, once. Every sling stops whirling.", "parley")
                    .fail("Grizzlefang shrieks before you finish. \"LIES! ATTACK!\" Rocks start flying.", "kobold_fight")
                .choice("Boom like a dragon (Thaumaturgy)").icon("🐉").spell("thaumaturgy").check("intimidation", 13)
                    .pass("Your voice swells to thunder and the cavern shakes: \"**WHO DARES THREATEN MY GUESTS?**\" Half the kobolds faint on the spot. Grizzlefang drops his spear. Grandmother Ssiss, alone, cackles — she isn't fooled, but she's impressed. She raps her stick: *talk.*", "parley")
                    .fail("Your voice booms magnificently… and squeaks at the end. Grizzlefang narrows his eyes. \"That is NOT the Black Mother.\" Here come the rocks.", "kobold_fight")
                .choice("Sneak up the hidden ledge path").icon("🌑").ifFlag("tripwire_seen").check("stealth", 13).group()
                    .pass("While Grizzlefang shouts at the place you were standing, you're already gone — up the wall, past the warren, and into the dark tunnel beyond. Nobody saw a thing.", "khaz_gate", xp(100))
                    .fail("A kobold child spots you, points, and screams with delight. So much for sneaking.", "kobold_fight")
                .choice("Charge up the path!").icon("⚔").go("kobold_trap");

        c.scene("kobold_trap").chapter(CH1).title("Tripwire").backdrop("mine").time("night").music("tension")
                .text("Your boot catches a tripwire you didn't see. Above you a kobold squeaks \"*YES!*\" — and a net full of rocks swings down from the ceiling!")
                .choice("Dodge!").icon("🪨").check("save:dex", 13).each()
                    .pass("You twist aside as the rocks thunder past.", "kobold_fight")
                    .fail("The net slams into you and drags you sprawling.", "kobold_fight", damageOne("2d6", "bludgeoning"));

        c.scene("kobold_fight").chapter(CH1).title("Ambush at the Warren").backdrop("mine").time("night").music("battle")
                .text("Slings whirr. Spears bristle. Grizzlefang howls \"FOR THE BLACK MOTHER!\" and the kobolds pour down from the ledges, while the slingers stay up high where your swords can't reach.")
                .choice("Fight your way through!").icon("⚔").battle("kobold_ambush")
                    .victory("Grizzlefang's pot-helmet goes spinning off into the dark and the kobolds scatter, wailing. Grandmother Ssiss watches you pass with unreadable eyes. \"Skarr will hear of this,\" she rasps. \"Skarr hears everything.\"", "khaz_gate", gold(20))
                    .defeat("Rocks rain down until you're forced back down the tunnel. When you creep back, the kobolds have retreated into their burrows — and the way past is open. They've let you through. You're not sure why.", "khaz_gate");

        c.scene("parley").chapter(CH1).title("Grandmother Ssiss").backdrop("mine").time("night").music("mystery")
                .npc("Grandmother Ssiss", "Eldest of the Ashtail", "#bdc3c7", "👵", "The Black Mother ate my grandchildren's courage, one by one. Tell me why I should give her yours.")
                .text("Skritch scrambles up beside the old kobold and chatters at her very fast, waving his arms in your direction. Grizzlefang protests. Ssiss smacks him with her stick without looking.",
                        "\"Skarr the Wyrmspeaker,\" she says at last, \"took the brave ones down to the Drowned Galleries to serve the Mother. He takes your miners too — to polish her hoard, forever. The Ashtail are tired of being *food*. If you go down to kill her…\" Her milky eyes fix on you. \"Convince me you can.\"")
                .choice("Promise the Ashtail freedom when she falls").icon("🤝").check("persuasion", 13).advantageIf("skritch_trust")
                    .pass("You speak plainly: no more tribute, no more fear. When you finish, Ssiss is quiet a long while. Then she puts a shaking claw on Skritch's head. \"Go with them, little rock-seller. Take the Clutch. When the Mother bleeds, the Ashtail will be there.\"", "kobold_peace")
                    .fail("\"Pretty words,\" Ssiss says. \"Skarr had pretty words too.\" Grizzlefang raises his spear with a gleeful shriek.", "kobold_fight")
                .choice("Charm Grizzlefang to win over the warriors (Charm Person)").icon("💗").spell("charm_person")
                    .pass("Grizzlefang's eyes go huge and shiny. \"You… you are FRIEND,\" he breathes, and immediately starts making a speech about how he always liked you. The warriors cheer their chief. Ssiss rolls her eyes but nods. \"Fine. The Ashtail will help.\"", "kobold_peace")
                .choice("Show her the gnawed props and the dead miners' note").icon("📜").ifFlag("miners_alive").check("insight", 12)
                    .pass("\"Skarr's kobolds gnawed those props to trap miners,\" you say. \"He's using your people to do his dirty work, and the Mother doesn't care who dies.\" Ssiss's claws tighten on her stick. \"…Yes. Yes, that is his way.\" She nods once. \"We will help you.\"", "kobold_peace")
                    .fail("Ssiss listens, but Grizzlefang shouts you down: \"LIES!\" The warriors take up the cry.", "kobold_fight");

        c.scene("kobold_peace").chapter(CH1).title("The Ashtail Alliance").backdrop("mine").time("night").music("hope")
                .npc("Skritch", "Kobold Refugee, Now Kobold Hero", "#e67e22", "🦎", "Skritch is GOING ON QUEST! Skritch has never been on a quest! Skritch packed rocks!")
                .onEnter(flag("kobold_peace"), xp(150))
                .text("The cavern erupts in a cacophony of happy squeaking. Kobolds pour out of every tunnel to stare at you, sniff you, and offer you roasted beetles (they're crunchy and taste faintly of hazelnut).",
                        "Grandmother Ssiss gives you a bone tablet scratched with a map of the deep ways. \"The Drowned Galleries. The Great Bridge, where Skarr keeps his Gutbreakers. And the hatchery, where he sings to the Mother's eggs.\" She taps the tablet. \"Eggs. She feeds on them, somehow. Break them and she weakens.\"",
                        "Skritch has been made captain of the Ashtail Clutch — twelve volunteers armed with slings and enthusiasm. \"We follow behind,\" he promises. \"When you fight the big one, look for us!\"")
                .choice("Accept the beetles, the map and the friendship").icon("🦎").effect(flag("know_eggs"), item("potion_healing")).go("khaz_gate");
    }

    // =========================================================================================== chapter 2
    static void chapterTwo(Campaign c) {
        c.scene("khaz_gate").chapter(CH2).title("The Door of Khaz Morduin").backdrop("hall").time("night").music("mystery")
                .onEnter(shortRest())
                .text("Beyond the warren, the miners' tunnels end at a wall of perfect dressed stone and an iron lift-cage on chains thick as your arm. You catch your breath (short rest), then crank yourselves down into the dark. Hulda runs her hand over the stone. \"Khaz Morduin,\" she breathes. \"My grandfather swore it was a story.\"",
                        "The lift groans to a halt in a vaulted gatehall so vast your lanterns can't find the ceiling. Before you stands a door of black iron thirty feet tall, carved with dwarf kings and a great anvil — and at its heart, a face of bronze with flame-coloured gem eyes.",
                        "As you approach, the bronze lips move. *\"I HAVE A HEART THAT NEVER BEATS, A MOUTH THAT NEVER EATS. I AM FED ON COAL AND BREATHE OUT FLAME. I GAVE THE DWARVES THEIR WEALTH AND FAME. NAME ME, AND ENTER.\"*",
                        "Around the doorstep, the floor is scorched black in neat, worrying circles.")
                .choice("Press the Coalbrow signet to the door").icon("💍").ifItem("wd_coalbrow_signet")
                    .effect(xp(100), log("The gem eyes flare and the bronze face *smiles*. \"COALBROW. WELCOME HOME.\" The great door swings open without a sound."))
                    .go("hall_of_kings")
                .choice("Puzzle out the riddle").icon("🤔").check("investigation", 13)
                    .pass("A heart that never beats, a mouth that never eats, fed coal, breathing flame, source of dwarven fame… \"It's a *forge*,\" you say. The bronze face beams. The door grinds open.", "hall_of_kings", xp(50))
                    .fail("You turn it over and over. Dragon? Mine? Gold? The answer dances just out of reach. You'll have to guess.", "riddle_guess")
                .choice("Recall the lore of the dwarf-holds").icon("📖").check("history", 12)
                    .pass("Every dwarf-hold had a riddle-door, and every riddle-door asked about the thing the hold was proudest of. For Khaz Morduin — famed for dragon-steel — that's the *forge*.", "hall_of_kings", xp(50))
                    .fail("Dwarf-holds, dwarf-kings, dwarf-beards… the lore runs together. Time to guess.", "riddle_guess")
                .choice("Ask Hulda").icon("⛏").go("riddle_guess");

        c.scene("riddle_guess").chapter(CH2).title("Name Me").backdrop("hall").time("night").music("mystery")
                .text("The bronze face waits, patient as stone. Hulda shrugs. \"Don't look at me. My family dug for silver, not riddles.\" Fizzwick offers a cheerful \"A lantern?\" and is ignored by everyone, including the door.")
                .choice("\"A dragon.\"").icon("🐉").go("riddle_wrong")
                .choice("\"A forge.\"").icon("🔥").go("hall_of_kings")
                .choice("\"Gold.\"").icon("🪙").go("riddle_wrong")
                .choice("\"A lantern.\"").icon("🏮").go("riddle_wrong");

        c.scene("riddle_wrong").chapter(CH2).title("WRONG").backdrop("hall").time("night").music("tension")
                .text("The bronze face frowns. The gem eyes flare white-hot — and jets of flame roar out of hidden vents all around the doorstep!")
                .choice("Throw yourselves flat!").icon("🔥").check("save:dex", 13).each()
                    .pass("You hit the floor as the fire roars overhead, singeing the air.", "riddle_guess")
                    .fail("The flames wash over you before you can drop.", "riddle_guess", damageOne("2d6", "fire"));

        c.scene("hall_of_kings").chapter(CH2).title("The Hall of Kings").backdrop("crypt").time("night").music("eerie")
                .text("Beyond the door lies the burial hall of Khaz Morduin's kings: rows of stone sarcophagi beneath an avenue of carved pillars, each lid bearing a dwarf in full armour, axe on chest.",
                        "Half the lids have been pushed aside. From the far end of the hall comes the sound of wet, contented chewing. Pale, hunched shapes crouch over the open tombs, and behind them, standing perfectly still in the gloom, is a skeleton twice the height of a man — with the horned skull of a bull.",
                        "\"The Labyrinth Warden,\" whispers Hulda, horrified. \"They used to keep a minotaur to guard the halls. They never stopped.\"")
                .choice("Fight for the kings' rest").icon("⚔").battle("crypt")
                    .victory("The minotaur's skull rolls to a stop at Hulda's feet, and the last ghoul crumples. Fizzwick goes from tomb to tomb with his lantern, murmuring prayers and pushing the lids gently back into place.", "crypt_after")
                    .defeat("You stagger back out through the riddle-door and wedge it shut. Hours later, when you dare to look again, the ghouls have slunk back into the deep tombs. The way is clear — for now.", "crypt_after")
                .choice("Sense the evil and call on the light").icon("🙏").check("religion", 13)
                    .pass("You raise a holy symbol and speak the old dwarven rite of rest. Light flares from the carved kings on the lids — and the ghouls *shriek*, flinching from the glow, blinded. Strike while they cower!", "crypt_fight", flag("crypt_surprise"))
                    .fail("Your prayer echoes off the stone. The ghouls look up from their meal. They look *interested*.", "crypt_fight")
                .choice("Creep through the shadows between the tombs").icon("🌑").check("stealth", 14).group()
                    .pass("Tomb to tomb, pillar to pillar, you slip past. The ghouls are too busy eating to notice — but the minotaur's empty eye-sockets turn, slowly, to follow you. It does nothing. Yet.", "crypt_after", xp(50))
                    .fail("Fizzwick's lantern swings and clinks against a sarcophagus. Every head in the hall turns.", "crypt_fight");

        c.scene("crypt_fight").chapter(CH2).title("The Hungry Dead").backdrop("crypt").time("night").music("battle")
                .text("The ghouls come scrabbling over the tombs on all fours. Behind them, with a grinding of ancient bone, the Labyrinth Warden lowers its horns.")
                .choice("Stand and fight!").icon("⚔").battle("crypt")
                    .victory("The minotaur's skull rolls to rest at Hulda's feet and the last ghoul collapses. Fizzwick goes tomb to tomb, gently closing the lids.", "crypt_after")
                    .defeat("You retreat and barricade yourselves until the ghouls lose interest. The hall is quiet again, for now.", "crypt_after");

        c.scene("crypt_after").chapter(CH2).title("The Last Miner").backdrop("crypt").time("night").music("sorrow")
                .npc("Ollie Thatch", "Missing Miner (Found!)", "#95a5a6", "🪔", "Is it— are you real? Oh gods, you're real. Have you got any food? Anything? A boot?")
                .text("In the last tomb, curled up inside an empty sarcophagus with the lid pulled almost shut, you find a skinny young human in a miner's smock: OLLIE THATCH, one of the twelve. He has been hiding in a dead king's coffin for nine days.",
                        "\"The kobolds took us down through the halls,\" he gabbles, between mouthfuls of your rations. \"To the Drowned Galleries, to *her*. She's got the others polishing her gold — Bodric Coalbrow, Fenn, all of them. Every coin, every day, and if it's not shiny enough…\" He swallows. \"I ran when the ghouls came. Wrong way. Ended up here.\"")
                .choice("Search the tombs for anything useful").icon("🔍").once().check("investigation", 12)
                    .pass("In the King's own sarcophagus, beneath his folded hands, you find a scroll-case of star-iron. Inside: the forging-runes for *dragonbane steel* — and a dwarven healing draught.", "crypt_after", flag("know_forge"), item("potion_greater_healing"), xp(50))
                    .fail("Bones, dust, and a very old, very offended beetle.", "crypt_after")
                .choice("Send Ollie back up toward the lift").icon("🪔").once()
                    .effect(flag("ollie_saved"), log("You give Ollie a lantern and directions. \"Tell Dagna we're coming,\" you say. He hugs every one of you — Hulda twice — and runs."))
                    .go("crypt_after")
                .choice("Press on deeper into the halls").icon("➡").go("pendulums");

        c.scene("pendulums").chapter(CH2).title("The Pendulum Hall").backdrop("hall").time("night").music("tension")
                .text("The way on is a long, narrow gallery spanned by twelve enormous bronze axe-blades, hanging from the vaulted ceiling on chains. They are swinging — slowly, silently, perfectly — in a staggered rhythm that leaves, for a heartbeat at a time, a gap just wide enough to step through.",
                        "Inscribed over the entrance in dwarven runes: *THE UNWORTHY DO NOT PASS. THE WORTHY PASS SLIGHTLY TRIMMED.*",
                        "Tobble would have a name for this. He'd call it *Tuesday*.")
                .choice("Study the rhythm and call the steps").icon("🔍").check("investigation", 14)
                    .pass("Three-two-three, pause, four-one-four… You chant the count and the party moves like dancers through the swinging blades. On the far side, someone laughs shakily. Nobody has lost so much as an eyebrow.", "ancestors", xp(100))
                    .fail("You *think* you have the pattern. The seventh blade disagrees.", "pendulum_trap")
                .choice("Freeze the chain-wheel with Ray of Frost").icon("❄").spell("ray_of_frost")
                    .pass("You trace the chains up to a great bronze wheel in the ceiling and hit its axle with a ray of frost. Ice blooms. The wheel groans, shudders… and stops. Twelve axes hang motionless. You stroll through.", "ancestors", xp(100))
                .choice("Jam the mechanism with a mine-cart axle").icon("🔧").check("athletics", 15)
                    .pass("You shove an iron axle into the nearest blade's pivot-housing. With a shriek of tortured bronze, the whole rhythm stutters and stops. Hulda nods. \"Dwarven engineering. Strong, clever, and you can always break it with a bigger bit of iron.\"", "ancestors", xp(100))
                    .fail("The axle goes in, comes out again at great speed, and nearly takes your head with it. The blades swing on.", "pendulum_trap")
                .choice("Disarm the trigger plate with thieves' tools").icon("🛠").ifItem("thieves_tools").check("sleight_of_hand", 14)
                    .pass("You find the counterweight release under a loose flagstone and ease the pin out. Twelve axes sigh to a halt like a sleepy orchestra.", "ancestors", xp(100))
                    .fail("A click. A whir. Oh, no.", "pendulum_trap")
                .choice("Just run for it").icon("🏃").go("pendulum_trap");

        c.scene("pendulum_trap").chapter(CH2).title("The Worthy, Slightly Trimmed").backdrop("hall").time("night").music("tension")
                .text("WHOOSH — WHOOSH — WHOOSH! The great blades sweep past in a storm of bronze!")
                .choice("Duck, weave, dive!").icon("🪓").check("save:dex", 14).each()
                    .pass("You tumble out of the far end of the gallery with your hair fractionally shorter.", "ancestors")
                    .fail("A blade rakes across you on its upswing.", "ancestors", damageOne("2d8", "slashing"));

        c.scene("ancestors").chapter(CH2).title("The Hall of Ancestors").backdrop("hall").time("night").music("camp").anchor("camp")
                .onEnter(level(4), longRest())
                .text("Beyond the pendulums lies a quiet, round chamber lit by a ring of everburning lamps: the Hall of Ancestors, where the dwarves of Khaz Morduin came to sleep before great deeds. Stone bunks line the walls, and a spring of clean, cold water runs through a channel in the floor.",
                        "You sleep the sleep of the deep earth. When you wake, something has settled into your bones — steadier hands, sharper eyes, a deeper well of power. **(Every hero reaches level 4 and finishes a long rest!)**",
                        "Hulda sits by the lamps, staring at a carved line of names on the wall. One of them is DEEPDELVE.")
                .choice("Ask Hulda about the names").icon("⛏").once().check("insight", 11)
                    .pass("\"My great-great-grandfather,\" she says gruffly. \"Thought it was a story. All of it. Thought *I* was the story — the one who never got to be anything but a miner.\" She clears her throat. \"Anyway. Let's go kill a dragon.\" (Heroic Inspiration)", "ancestors", inspiration())
                    .fail("\"Old names,\" she grunts. \"Old stone.\" She doesn't say any more.", "ancestors")
                .choice("Let Fizzwick bless the party's weapons").icon("🏮").once()
                    .effect(log("Fizzwick blesses every weapon, every boot, the spring, two bunks, and a passing beetle. \"Can't be too careful!\""))
                    .go("ancestors")
                .choice("Prepare spells and gear (open your character sheet)").icon("📖").once()
                    .effect(log("Take a moment: attune magic items, swap prepared spells, and hand potions to whoever needs them most."))
                    .go("ancestors")
                .choice("Follow the smell of old smoke to the Ancestor Forge").icon("🔥").go("forge_door");

        c.scene("forge_door").chapter(CH2).title("The Forgemaster's Skull").backdrop("hall").time("night").music("tension")
                .npc("Durgrim's Burning Skull", "Last Forgemaster of Khaz Morduin", "#ff7043", "💀", "THIEVES! GRAVE-ROBBERS! THREE HUNDRED YEARS I HAVE GUARDED THIS FORGE AND NOT ONE OF YOU HAS WIPED YOUR BOOTS!")
                .text("The Ancestor Forge is a cathedral of soot: a hearth the size of a house, anvils like altars, bellows big enough to sleep in. Everything is cold. Everything except the skull floating above the great anvil, wreathed in roaring green fire.",
                        "It was a dwarf once — you can tell from the bronze beard-rings still clinging to its jaw. Now it is DURGRIM, the last forgemaster, and he is extremely cross. Two suits of ancient forge-guard armour clank to life at his sides.")
                .choice("Show him the Coalbrow signet").icon("💍").ifItem("wd_coalbrow_signet").check("persuasion", 12)
                    .pass("The green fire flickers. \"…A COALBROW? The Coalbrows were my *apprentices*! Terrible at riveting! Wonderful singers!\" The armour clanks to a halt. \"Fine. FINE. Use the forge. But you *will* clean the anvil.\"", "forge", xp(150))
                    .fail("\"A RING? ANYONE CAN STEAL A RING!\" The green fire roars higher.", "forge_fight")
                .choice("Claim you've come to finish his life's work").icon("💬").check("history", 15).advantageIf("know_forge")
                    .pass("\"The dragon-steel, forgemaster. Your masterwork. The dragon has come again — let us finish what you started.\" A long silence. Then the skull lets out a sound like a crackling sob. \"…No one has asked me to *work* in three hundred years.\" The armour clanks aside.", "forge", xp(150))
                    .fail("\"FLATTERY! I invented flattery! I flattered a king into a moat once!\" He blazes up, furious.", "forge_fight")
                .choice("Fight the forgemaster").icon("⚔").go("forge_fight");

        c.scene("forge_fight").chapter(CH2).title("The Forge Guardian").backdrop("hall").time("night").music("battle")
                .text("Durgrim's skull screams like a kettle and spits a gout of green fire. The forge-guard armour raises its hammers and marches.")
                .choice("Fight!").icon("⚔").battle("flameskull")
                    .victory("The skull's fire gutters to a pale flicker and it drops onto the anvil with a clunk. \"…Fine,\" it mutters. \"Use the forge. *Clean the anvil.*\" Then it goes dark — resting, not destroyed. Dwarves are stubborn even after death.", "forge", xp(50))
                    .defeat("You're driven out into the Hall of Ancestors, singed and battered. When you creep back hours later, Durgrim is asleep on his anvil, snoring flames. You tiptoe past him to the forge.", "forge");

        // One scene, two stages: the "forge_lit" flag hides the lighting choices and reveals the forging ones.
        c.scene("forge").chapter(CH2).title("The Ancestor Forge").backdrop("hall").time("night").music("ruins")
                .text("The great hearth is cold, but a heap of star-iron ingots lies beside the anvil, shimmering faintly blue — enough for one masterwork, maybe; with real skill, a little more. Carved above the forge in dwarven runes: *WHAT IS FORGED HERE REMEMBERS WHAT IT WAS FORGED FOR.*",
                        "First things first: you need fire. A *lot* of fire. Durgrim's skull, if it's awake, mutters from the anvil-top: \"Hotter. HOTTER. Hit it like it owes you money!\"")
                .choice("Hurl a Fire Bolt into the hearth").icon("🔥").unlessFlag("forge_lit").spell("fire_bolt")
                    .pass("Your bolt hits the coal and the forge *wakes* with a roar like a dragon clearing its throat. Fire races along ancient channels; the bellows begin to pump by themselves. The whole hall glows gold.", "forge", flag("forge_lit"))
                .choice("Work the giant bellows together").icon("💨").unlessFlag("forge_lit").check("athletics", 13).group()
                    .pass("Heave! Heave! HEAVE! The bellows wheeze, then roar, and the coals catch with a whoosh. The forge is lit.", "forge", flag("forge_lit"))
                    .fail("It takes far longer than it should, and somebody's eyebrows go missing, but eventually the forge catches.", "forge", flag("forge_lit"), damageOne("1d6", "fire"))
                .choice("Coax the coals with Prestidigitation").icon("✨").unlessFlag("forge_lit").spell("prestidigitation")
                    .pass("A shower of tiny sparks from Fizzwick's fingers dances into the coal. One catches. Then ten. Then all of them, with a deep, contented *whumph*.", "forge", flag("forge_lit"))
                .choice("Forge Wyrmbane, a dragon-slaying blade").icon("🗡").ifFlag("forge_lit").unlessFlag("forged")
                    .effect(item("wd_wyrmbane"), flag("dragonbane"), flag("forged"), xp(100),
                            log("You hammer the star-iron into a blade and quench it in the cold spring. It comes out singing — a thin, high note that sets your teeth on edge. Every dragon within a mile will hear that note and *flinch*. (Wyrmbane: attune for +1 attack and damage. The Black Mother will start the final battle weakened and without Legendary Resistance.)"))
                    .go("forge")
                .choice("Forge an Acid-Glass Mantle").icon("🧥").ifFlag("forge_lit").unlessFlag("forged")
                    .effect(item("wd_acidglass_mantle"), flag("forged"), xp(100),
                            log("You draw the star-iron into glass-thread so fine it flows like silk, and weave a mantle that sheds acid like rain off a roof. (Acid-Glass Mantle: attune for +1 AC and saving throws.)"))
                    .go("forge")
                .choice("Try to forge both with masterful smithing").icon("⚒").once().ifFlag("forge_lit").unlessFlag("forged").check("athletics", 16)
                    .pass("You work like a dwarf possessed, hammer ringing until your arms burn — and from one heap of star-iron you draw BOTH a singing blade and a glass-thread mantle. Durgrim's skull is weeping green sparks. \"*Beautiful. BEAUTIFUL.*\" (You forge Wyrmbane AND an Acid-Glass Mantle!)", "forge", item("wd_wyrmbane"), item("wd_acidglass_mantle"), flag("dragonbane"), flag("forged"), xp(150))
                    .fail("You strike too hard, too fast. Half the star-iron cracks and crumbles. From what's left you can make one thing — choose.", "forge")
                .choice("Leave the forge and descend").icon("⬇").go("galleries");
    }

    // =========================================================================================== chapter 3
    static void chapterThree(Campaign c) {
        c.scene("galleries").chapter(CH3).title("The Drowned Galleries").backdrop("seacave").time("night").music("dungeon").anchor("dungeon").checkpoint()
                .onEnter(shortRest())
                .text("You rest a while at the top of the Drowned Stair (short rest), then descend. Below Khaz Morduin, the dwarves' carved halls give way to something older: vast natural galleries where the sea once came in, millennia ago, and carved the rock into ribs and arches. The sea is gone. What fills the galleries now is worse.",
                        "Black acid lies in still, oily pools, steaming gently. The air stings your eyes. Every so often, with a deep *gloop*, a pool bubbles — and a spout of acid hisses up to splatter the ceiling.",
                        "Somewhere ahead, far away, you hear coins clinking. Thousands of them. And something enormous, breathing slow.")
                .choice("Take the narrow ledge along the gallery wall").icon("🧗").check("acrobatics", 13).group()
                    .pass("Single file, backs to the rock, you edge along the ledge above the pools. A geyser erupts beneath you and spatters harmlessly on the stone. You make it across.", "rivals_again", xp(50))
                    .fail("The ledge crumbles under someone's boot and you slide down toward the pools — right into the geysers' range.", "acid_trap")
                .choice("Freeze the nearest geyser vent with Ray of Frost").icon("❄").spell("ray_of_frost")
                    .pass("Your ray seals the vent under a plug of frosted acid. The pool goes still and you hurry across the shallows before it thaws. Behind you, as the last one steps clear, it erupts with an angry *pop*.", "rivals_again", xp(50))
                .choice("Read the geysers' rhythm and time the dash").icon("⏱").check("survival", 14)
                    .pass("Every geyser has a pulse, like a heartbeat. You count it, wait, wait — GO! — and the whole party sprints through the gap between eruptions.", "rivals_again", xp(50))
                    .fail("You *almost* have the pattern. Almost.", "acid_trap")
                .choice("Neutralise the pools with quicklime from the mine stores").icon("🧪").ifFlag("acid_lore")
                    .effect(xp(100), log("You recognised dragon-acid back at the stream in Blackspire — and acid hates lime. You scatter sacks of quicklime from an old dwarven storeroom across the shallows. The pools hiss, foam, and turn to harmless grey sludge. You stroll across. Hulda looks at you with new respect."))
                    .go("rivals_again")
                .choice("Wade straight through the shallows").icon("🥾").go("acid_trap");

        c.scene("acid_trap").chapter(CH3).title("The Geysers Erupt").backdrop("seacave").time("night").music("tension")
                .text("*GLOOP — HISSSS!* The pools around you heave and burst, spraying fountains of black acid into the air!")
                .choice("Run!").icon("🧪").check("save:dex", 13).each()
                    .pass("You sprint through the falling droplets and out the other side, smoking slightly.", "rivals_again")
                    .fail("A spout of acid catches you full on. Your armour fizzes. So does your skin.", "rivals_again", damageOne("2d8", "acid"));

        c.scene("rivals_again").chapter(CH3).title("The Golden Spurs, Tarnished").backdrop("seacave").time("night").music("tension")
                .npc("Sir Percival Glint", "Leader of the Golden Spurs (Mildly Dissolving)", "#f1c40f", "✨", "Ah! Hello! Fancy meeting you here. We are absolutely not stuck. This is a tactical pause.")
                .text("In the middle of a wide acid lake, on a crumbling rock island no bigger than a dining table, stand the Golden Spurs. Glint's cape is in tatters. Bran is holding Quillon above his head to keep the elf's boots out of the rising acid. Quillon is still writing: \"*…and our heroes faced their final chapter with dignity—*\"",
                        "\"WE COULD USE A ROPE,\" Bran bellows cheerfully.")
                .choice("Throw them a rope and haul them across").icon("🪢").ifFlag("rivals_friends")
                    .effect(flag("rivals_saved"), xp(50), log("You throw the line and the Spurs scramble across, Bran carrying Quillon like a parcel. Glint shakes your hand, slightly smoking. \"As agreed, allies! The Golden Spurs ride with you. Well — walk.\" He presses a potion into your hand."), item("potion_healing"))
                    .go("bridge")
                .choice("Rescue them anyway (Athletics)").icon("💪").unlessFlag("rivals_friends").check("athletics", 13)
                    .pass("You lash a rope to a stalagmite, wade out as far as you dare, and haul. One, two, three gleaming adventurers flop onto the shore like landed fish. Glint stares at you for a long moment. \"…You saved us. After all that.\" He bows, stiffly and sincerely. \"The Golden Spurs are in your debt. We fight beside you.\"", "bridge", flag("rivals_friends"), flag("rivals_saved"), xp(100))
                    .fail("The rope slips. Bran ends up wading out himself, carrying everyone, howling as the acid eats his boots. On shore, Glint sniffs. \"We would have managed.\" They limp off back toward the surface.", "bridge", damageOne("1d6", "acid"))
                .choice("Bridge the gap with a Misty Step and a rope").icon("✨").unlessFlag("rivals_friends").spell("misty_step")
                    .pass("You vanish in a puff of silver mist and reappear on the island, rope in hand, to the Spurs' enormous surprise. Two minutes later they're all ashore. Quillon writes furiously. \"*…rescued by a wizard, which we shall never speak of again.*\" Glint bows. \"The Spurs fight beside you.\"", "bridge", flag("rivals_friends"), flag("rivals_saved"), xp(100))
                .choice("Leave them to their tactical pause").icon("↩").unlessFlag("rivals_friends")
                    .effect(log("\"WE'RE FINE!\" Glint calls after you. \"COMPLETELY FINE!\" A few minutes later you hear Bran splashing them all to shore. They'll live. They will not be helping."))
                    .go("bridge");

        c.scene("bridge").chapter(CH3).title("The Great Bridge of Morduin").backdrop("bridge").time("night").music("tension")
                .npc("Gutgrinder", "Ogre, Toll-Keeper of the Bridge", "#7d8f4e", "👹", "BRIDGE IS CLOSED. BRIDGE IS ALWAYS CLOSED. UNLESS YOU IS LUNCH. THEN BRIDGE IS OPEN.")
                .text("A chasm splits the galleries, so deep that the lantern you drop over the edge simply gets smaller until it's gone. Across it stretches the Great Bridge of Morduin — a single span of dwarf-stone, wide enough for two carts abreast, with no rail.",
                        "On the far side, the Black Mother's GUTBREAKERS have made camp: two hulking bugbears with hammers, and a vast, slack-jawed ogre named GUTGRINDER sitting on a pile of stolen pickaxes, picking his teeth with a shovel.",
                        "(The bridge has no railings. A good shove, and a foe goes over the edge…)")
                .choice("Charge across the bridge!").icon("⚔").battle("bridge")
                    .victory("Gutgrinder's bellow fades away beneath you as he tumbles into the dark — or crashes down on the bridge, it's hard to tell in the chaos. The bugbears flee. Among the pickaxes you find a stash of loot the Gutbreakers stole from the miners.", "bridge_far", gold(40), item("potion_healing"))
                    .defeat("You're hammered back across the bridge. You regroup, bandage up, and later, while the Gutbreakers sleep off their victory feast, you tiptoe past.", "bridge_far")
                .choice("Pose as tribute-bearers for the Black Mother").icon("🎭").check("deception", 14).advantageIf("kobold_peace")
                    .pass("\"Tribute for the Mother!\" you call, holding up a sack that clinks (it's rocks; Skritch taught you that). Gutgrinder squints, scratches, and waves you through. \"Mother likes shinies. Go on.\" You stroll past three monsters who could have killed you all, smiling.", "bridge_far", xp(150))
                    .fail("Gutgrinder sniffs your sack. \"…Is rocks.\" He looks hurt, then furious. \"IS ROCKS!\"", "bridge_fight")
                .choice("Conjure the illusion of a rampaging dragon (Minor Illusion)").icon("🐉").spell("minor_illusion").check("deception", 12)
                    .pass("A shadow with wings flickers across the far wall, accompanied by a roar that's mostly you, cupping your hands. Gutgrinder screams like a kettle and dives behind his pickaxe-pile. The bugbears follow. You stroll across the bridge before they work out the dragon had your voice.", "bridge_far", xp(150))
                    .fail("The illusion is… a bit small. More of a newt. Gutgrinder laughs so hard he falls off his pickaxes, then gets up very angry.", "bridge_fight");

        c.scene("bridge_fight").chapter(CH3).title("The Gutbreakers").backdrop("bridge").time("night").music("battle")
                .text("Gutgrinder heaves himself up, shovel in one fist and a greatclub in the other. The bugbears grin their long, unpleasant grins and stride onto the bridge.")
                .choice("Hold the bridge!").icon("⚔").battle("bridge")
                    .victory("The last Gutbreaker goes over the edge with a long, fading wail. Among the stolen pickaxes you find a stash of loot.", "bridge_far", gold(40), item("potion_healing"))
                    .defeat("You're driven back. Later, while the Gutbreakers snore, you sneak across.", "bridge_far");

        c.scene("bridge_far").chapter(CH3).title("The Singing in the Dark").backdrop("seacave").time("night").music("eerie")
                .onEnter(shortRest())
                .text("Beyond the bridge you take a moment in a dry niche to bind wounds and drink from your waterskins (short rest). Then a tunnel slopes down through rock that grows warmer and wetter with every step. The acid smell thickens. And you hear it: a hissing, rhythmic chant, dozens of small voices — *Mother, Mother, Mother* — and above them all, one cruel, high voice leading the song.",
                        "The tunnel opens onto a ledge above the HATCHERY: a steaming cavern of acid pools where great black eggs lie nestled in heaps of coins. Kobold zealots kneel around them. Chained to a rock, filthy and exhausted, sit five dwarven miners — and among them, a red-bearded dwarf with Dagna's jaw: BODRIC COALBROW.",
                        "Above it all, on a throne of skulls and pickaxe-heads, sits a kobold in a crown of dragon teeth: HIGH WYRMSPEAKER SKARR.")
                .choice("Creep down for a surprise attack").icon("🌑").check("stealth", 14).group()
                    .pass("Down the rocks, through the steam, behind the egg-heaps… Skarr is mid-verse when you rise up behind his guards.", "hatchery", flag("skarr_surprise"), xp(50))
                    .fail("A pebble skitters. The chant stops dead. Skarr's head swivels toward you with a nasty little smile.", "hatchery")
                .choice("Signal Skritch's Clutch to cause a distraction").icon("🦎").ifFlag("kobold_peace")
                    .effect(flag("skarr_surprise"), log("Skritch's head pops up from a crack in the far wall. You wave. He grins and vanishes — and a moment later, from the opposite side of the cavern, a dozen kobold voices shriek \"THE MOTHER IS ANGRY! THE MOTHER WANTS SNACKS!\" Every zealot turns the wrong way."))
                    .go("hatchery")
                .choice("Walk in openly and challenge Skarr").icon("⚔").go("hatchery");

        c.scene("hatchery").chapter(CH3).title("High Wyrmspeaker Skarr").backdrop("seacave").time("night").music("battle")
                .npc("High Wyrmspeaker Skarr", "Voice of the Black Mother", "#8e44ad", "🪬", "More polishers! The Mother will be SO pleased. Or more snacks. She is not fussy, and neither am I.")
                .text("Skarr rises from his throne, staff crackling with green-black acid. His dragonshields lock their scale-shields together in front of him, and slingers scramble up the rocks behind.",
                        "\"Do you know what she promised me?\" he hisses. \"When she has eaten Blackspire, she will eat the Baron's town, and the next, and the next — and I will stand at her side and *watch*.\" He smiles with too many teeth. \"Kill them. Gently. Their bones polish coins beautifully.\"")
                .choice("Bring down the Wyrmspeaker!").icon("⚔").battle("shaman")
                    .victory("Skarr shrieks as his crown of teeth goes clattering across the stones, and he topples backward into an acid pool with a final, furious *hisssss*. The zealots flee. You smash the chains on the miners — Bodric Coalbrow grips your hand so hard it hurts.", "skarr_after", flag("miners_freed"))
                    .defeat("You're driven back up the slope. But in the confusion, the chained miners heave their chains loose and swarm Skarr's throne with picks and fury. When you return, the Wyrmspeaker has fled into the deep, and the miners are free — battered, but free.", "skarr_after", flag("miners_freed"))
                .choice("Charm a dragonshield to turn on his master (Charm Person)").icon("💗").spell("charm_person")
                    .pass("The biggest dragonshield's eyes go soft. It turns, blinks at Skarr, and says, \"…You is not very nice, actually.\" In the chaos that follows, you strike first!", "skarr_fight", flag("skarr_surprise"));

        c.scene("skarr_fight").chapter(CH3).title("Scales and Spit").backdrop("seacave").time("night").music("battle")
                .text("Skarr screams in fury as his own guard wavers. \"TRAITOR! KILL THEM ALL!\"")
                .choice("Strike!").icon("⚔").battle("shaman")
                    .victory("Skarr topples into an acid pool with a final *hisssss*. The zealots flee, and you smash the miners' chains. Bodric Coalbrow grips your hand so hard it hurts.", "skarr_after", flag("miners_freed"))
                    .defeat("You fall back — but the miners break their own chains in the chaos and drive Skarr off with their bare hands. They're free.", "skarr_after", flag("miners_freed"));

        c.scene("skarr_after").chapter(CH3).title("Bodric Coalbrow").backdrop("seacave").time("night").music("hope")
                .npc("Bodric Coalbrow", "Rescued Miner & Dagna's Brother", "#c0392b", "⛏", "Tell my sister I'm sorry I was late for supper. Nineteen days late. Gods, I'm starving.")
                .text("The miners are thin and filthy and their hands are raw from polishing, but they're *alive*. Bodric Coalbrow, Dagna's red-bearded brother, organises them with the brisk efficiency of a family of foremen.",
                        "\"She's through there,\" he says, pointing to a vast arch at the far end of the hatchery, steaming and glittering. \"The hoard. She sleeps on it. She's *huge*. And those eggs —\" he nods at the heaps — \"she's bound to them, somehow. Every time one of us dropped one, she screamed like we'd stabbed her.\"")
                .choice("Smash the eggs here in the hatchery").icon("🥚").once()
                    .effect(flag("know_eggs"), xp(50), log("You smash the eggs in the hatchery heaps — they're cold, dead, decoys. The real ones, Bodric realises, must be in the hoard with her. \"She keeps the living ones close. Smash those, and she's weaker.\""))
                    .go("skarr_after")
                .choice("Search Skarr's throne").icon("🔍").once().check("investigation", 12)
                    .pass("Under the throne's cushion of stolen beards: Skarr's prayer-scrolls, a purse of the Mother's own gold, and a vial of something thick and green labelled in kobold scrawl *MOTHER'S FAVOURITE — DO NOT DRINK* (it's a healing potion; kobolds label everything that way).", "skarr_after", gold(50), item("potion_greater_healing"))
                    .fail("Bones, pebbles, and a surprising number of hand-knitted socks.", "skarr_after")
                .choice("Send the miners up to safety and make camp").icon("🏕").effect(log("Bodric leads the miners back toward the bridge and the long climb home. \"Come back,\" he tells you. \"Dagna'll never forgive me if you don't.\"")).go("last_camp");

        c.scene("last_camp").chapter(CH3).title("The Last Camp").backdrop("camp").time("night").music("camp").checkpoint()
                .onEnter(level(5), longRest())
                .text("You make camp in a dry side-cave just short of the great arch. Fizzwick hangs his lantern from a stalactite, Hulda sharpens Grandma the longsword, and for a few hours, the only sound is the slow, enormous breathing from beyond the arch, like the sea.",
                        "You sleep, and wake, and something has changed. You are not the heroes who walked into the Gilded Pickaxe. **(Every hero reaches level 5 and finishes a long rest! Martial heroes gain Extra Attack; wizards learn Fireball and Lightning Bolt; clerics learn Spirit Guardians.)**",
                        "Beyond the arch, coins clink as something huge rolls over in its sleep.")
                .choice("Share a meal and a story").icon("🔥").once().check("performance", 12)
                    .pass("You tell the tale of the dwarf who tricked a dragon into eating a lit lantern, and by the end Hulda is laughing so hard she has to put Grandma down. Courage, it turns out, is catching. (Heroic Inspiration)", "last_camp", inspiration())
                    .fail("Your story wanders off somewhere around the fourth dwarf. But Fizzwick claps anyway, and it's nice to laugh.", "last_camp")
                .choice("Pray to the Lantern Saint with Fizzwick").icon("🏮").once().check("religion", 12)
                    .pass("Fizzwick's lantern flares bright as morning, and for a moment the whole cave is full of warm gold light. \"She heard,\" he whispers, delighted. \"She *always* hears.\" (Heroic Inspiration)", "last_camp", inspiration())
                    .fail("The lantern flickers. The cave stays dark. Fizzwick pats your hand. \"She's busy. She'll catch up.\"", "last_camp")
                .choice("Sulvara: rehearse your apology to great-great-great-grand-auntie").icon("🐲").once().ifSpecies("dragonborn")
                    .effect(log("\"Dear Auntie,\" you practise, \"sorry about the stabbing.\" Hulda snorts. Fizzwick says it's very heartfelt. Tobble says you should open with a compliment. You decide to wing it."))
                    .go("last_camp")
                .choice("Prepare spells and gear (open your character sheet)").icon("📖").once()
                    .effect(log("Last chance: attune Wyrmbane or the Mantle, prepare your new spells, and hand out every potion you have."))
                    .go("last_camp")
                .choice("Pass beneath the great arch").icon("🐉").go("lair_approach");
    }

    // =========================================================================================== finale
    static void finale(Campaign c) {
        c.scene("lair_approach").chapter(FIN).title("The Hoard of Nyxathrax").backdrop("hoard").time("night").music("dragon").weather("fog")
                .npc("Nyxathrax", "The Black Mother", "#1e3a1e", "🐉", "Ahhh. Fresh polishers. You're early. I do love punctual food.")
                .text("The cavern beyond the arch is so big it has its own weather: a thin, stinging mist that hangs over a sea of gold. Coins, cups, crowns, shields, the treasure of three hundred years of dead dwarves and a thousand robbed caravans, heaped in dunes taller than houses. Acid pools steam between the dunes. A chasm splits the floor like a wound.",
                        "In the middle of it all, a hill of gold *moves*. Scales slide out from under the coins, black and glossy as wet coal. Wings unfold, ragged and enormous. A horned head rises on a neck like a mine-cart train, acid drooling from its jaws to smoke on the treasure below.",
                        "NYXATHRAX, the Black Mother, regards you with eyes like two cold green lamps. Three great black eggs nestle in the gold around her, pulsing faintly. *\"Little things,\"* she purrs, *\"you have walked a very long way to die somewhere shiny.\"*")
                .choice("\"We've come for the miners. And for you.\"").icon("⚔").battle("nyxathrax")
                    .victory("", "dragon_falls")
                .choice("Raise Wyrmbane — let her hear it sing!").icon("🗡").ifFlag("dragonbane").battle("nyxathrax")
                    .effect(log("You draw Wyrmbane and it *sings* — that thin, high, terrible note. Nyxathrax recoils as if struck, her scales rippling, and for the first time in a century, something in those cold green eyes looks like FEAR. (The Black Mother begins the battle weakened and without Legendary Resistance.)"))
                    .victory("", "dragon_falls")
                .choice("Speak to her in Draconic, dragonborn to dragon").icon("🐲").ifSpecies("dragonborn").check("deception", 13)
                    .pass("You speak in the old tongue of dragons: *\"Great-great-great-grand-auntie. It's me. Family.\"* Nyxathrax's head tilts. Her eyes narrow, then widen, then — for one baffled, dragonish moment — she *hesitates*. That moment is all you need.", "lair_surprise")
                    .fail("*\"Family?\"* she hisses, amused. *\"I eat family. It's the best part of the holidays.\"*", "lair_now")
                .choice("Smash the nearest egg before she's fully awake").icon("🥚").ifFlag("know_eggs").check("athletics", 12)
                    .pass("You know her secret. You sprint across the coins and bring your weapon down on the nearest egg with a CRACK that echoes like a bell. It holds — just — but Nyxathrax SCREAMS as if you'd struck her heart, and lunges off-balance, all claws and fury. You're ready for her. She isn't ready for you.", "lair_surprise")
                    .fail("You charge the egg, slip on a dune of loose coins and slide, flailing, almost into her jaws. *\"Clumsy,\"* she purrs.", "lair_now")
                .choice("Offer her a tribute… and strike while she counts it").icon("🪙").check("deception", 15).advantageIf("know_mother")
                    .pass("You toss a fat purse onto the hoard. Nyxathrax's head snaps toward the clink of coins — she can't help herself, she *has* to count it — and in that instant, you attack.", "lair_surprise")
                    .fail("*\"Forty-three gold, a button and a lucky rock,\"* she says without looking. *\"I can count a purse by its sound, little thief. Now DIE.\"*", "lair_now");

        c.scene("lair_surprise").chapter(FIN).title("The Black Mother Strikes Second").backdrop("hoard").time("night").music("dragon")
                .onEnter(flag("dragon_surprised"))
                .text("Nyxathrax rears back, hissing in outrage, caught completely off guard.")
                .choice("NOW!").icon("⚔").battle("nyxathrax").victory("", "dragon_falls");

        c.scene("lair_now").chapter(FIN).title("Acid and Gold").backdrop("hoard").time("night").music("dragon")
                .text("The Black Mother spreads her wings, and the whole hoard shivers like a struck bell.")
                .choice("Fight for your lives!").icon("⚔").battle("nyxathrax").victory("", "dragon_falls");

        c.scene("dragon_falls").chapter(FIN).title("The Black Mother Falls").backdrop("hoard").time("night").music("triumph")
                .text("Nyxathrax's final shriek shakes stalactites from the ceiling. She claws at the air, at the gold, at you — and then her great head crashes down onto her hoard, sending a tidal wave of coins rolling across the cavern. The green light dies in her eyes.",
                        "For a moment, there is only the sound of coins settling. Then the acid pools begin to *boil*. Without the dragon's will to hold them, the geysers are erupting all at once, and the cavern floor is cracking open. RUN!")
                .choice("Grab what you can carry and run!").icon("🏃").check("athletics", 13).group()
                    .pass("You stuff your pockets, your boots, your hats and your mouths with gold and sprint for the arch as the hoard-cavern caves in behind you in a roar of acid and stone. You burst out into the hatchery, laughing like lunatics, as the dust settles over the Black Mother's grave.", "epilogue", gold(150), xp(300))
                    .fail("A geyser erupts beneath the arch as you dive through. You make it — singed, smoking, fizzing slightly — with less gold than you'd hoped, but alive.", "epilogue", damage("2d6", "acid", "dex", 13), gold(75), xp(300))
                .choice("Forget the gold — just run!").icon("💨")
                    .effect(xp(300), log("You leave the gold behind and sprint. Behind you, the hoard-cavern caves in with a roar. Nobody gets so much as a splash. Fizzwick, wheezing, says it's the most sensible thing anyone has done all week."))
                    .go("epilogue");

        c.scene("epilogue").chapter(EPI).title("Blackspire Celebrates").backdrop("tavern").time("night").music("triumph").anchor("epilogue")
                .onEnter(gold(100), longRest())
                .npc("Dagna Coalbrow", "Foreman of the Blackspire Mines", "#7f8c8d", "⛏", "Twelve went down. Twelve came up. Drinks are on the Coalbrows. FOREVER. Within reason.")
                .text("Blackspire throws a feast that rattles the windows. Every miner is home. The stream runs clear again for the first time in months, and Gretta has hung twelve pickaxes back on their owners' hooks — and a brand-new, gleaming one over the bar, engraved with your names (two of them misspelled).",
                        "Dagna pays out the Coalbrow share of the hoard herself, coin by coin, in front of the whole town — then grabs Bodric by the beard and doesn't let go for a full minute. Brokk Hammerhand leads a new verse of the drinking song. It is about you. It rhymes \"dragon\" with \"flagon\" eleven times.",
                        "The night is yours. And somewhere beyond the edge of the map, other dungeons are waiting.")
                .choice("Split the treasure fairly").icon("💰").once()
                    .effect(gold(50), log("You tip your share of the hoard onto Gretta's slate and divide it, coin by coin. It takes three hours, two arguments, and one emergency pie. Everyone ends up exactly equal, except whoever has the most pockets, who somehow has an extra ruby. Nobody can prove anything."))
                    .go("epilogue")
                .choice("Toast the Ashtail kobolds").icon("🦎").once().ifFlag("kobold_peace")
                    .effect(log("Skritch arrives at the feast with Grandmother Ssiss on his shoulders and a sack of lucky rocks for everyone. Gretta gives the Ashtail a table by the fire. Hulda, grumbling, shares her ale with Skritch. By midnight they are singing together. Blackspire has a kobold quarter now. It is very tidy."))
                    .go("epilogue")
                .choice("Read the Golden Spurs' chronicle").icon("📜").once().ifFlag("rivals_friends")
                    .effect(log("Quillon Vey presents you with a freshly bound volume: *THE GOLDEN SPURS AND FRIENDS (MOSTLY FRIENDS) VERSUS THE BLACK MOTHER.* Sir Percival has top billing. You are described as \"helpful.\" Bran has added a page of drawings of everyone hugging."))
                    .go("epilogue")
                .choice("Return Dagna's signet ring").icon("💍").once().ifItem("wd_coalbrow_signet")
                    .effect(gold(50), log("Dagna turns the ring over in her fingers, then presses it back into your palm. \"Keep it. The old doors know you now. Any Coalbrow hall is your hall.\" She pays you fifty gold anyway, because she is a Coalbrow and Coalbrows pay their debts."))
                    .go("epilogue")
                .choice("Find Ollie Thatch in the crowd").icon("🪔").once().ifFlag("ollie_saved")
                    .effect(item("potion_greater_healing"), log("Ollie, the miner you found hiding in a dead king's coffin, has eaten roughly his own weight in pie. He presses a stoppered bottle into your hands. \"From the infirmary. Dagna said you'd earned it. Also — I'm never going underground again. I'm going to be a baker.\""))
                    .go("epilogue")
                .choice("Play one last round of Dragon's Hoard with Lady Fortuna").icon("🎲").once().ifFlag("fortuna_friend")
                    .effect(gold(25), log("Fortuna deals you in with a wink. \"Real dragon-slayers. Gods. Now *that's* a bluff nobody would call.\" She loses on purpose, beautifully, and you both know it."))
                    .go("epilogue")
                .choice("Visit Durgrim's forge one last time").icon("💀").once().ifFlag("forged")
                    .effect(log("Word comes up from the deep that a burning skull has been seen giving smithing lessons to the young miners of Blackspire. He is, by all accounts, a terrible teacher. He is also the happiest skull in the world."))
                    .go("epilogue")
                .choice("Raise a toast and end the tale").icon("🏁").go("@end");
    }

    // =========================================================================================== battle maps
    static void encounters(Campaign c) {
        // ---- Prologue: a non-lethal scuffle with the Golden Spurs.
        c.encounter("brawl").title("Scuffle at the Gilded Pickaxe").backdrop("tavern").music("brawl").nonlethal()
                .intro("Tables flip, tankards fly, and Sir Percival shouts \"NOT THE FACE!\" Nobody dies in a proper tavern scuffle — but everyone gets hurt.")
                .map("####################",
                     "#BB...B....##.tBBB.#",
                     "#......q...##......#",
                     "#..BB..........BB..#",
                     "#..BB....g.....BB..#",
                     "#........E.....t...#",
                     "#...b..........B...#",
                     "#..PP....BB........#",
                     "#..PP....BB....,,..#",
                     "#..PP.........t....#",
                     "#BB......BB....BBB.#",
                     "####################")
                .legend('g', "wd_glint").legend('b', "wd_rival_bruiser").legend('q', "wd_rival_archer").legend('t', "tavern_brawler")
                .extra("tavern_brawler");

        // ---- Chapter 1: rats and a spider in the cart depot.
        c.encounter("vermin").title("The Old Cart Depot").backdrop("mine").music("battle")
                .intro("Giant rats pour out from under the rusting carts while a giant spider lowers itself from the webs. (Webs are difficult terrain; carts give half cover.)")
                .surpriseIf("vermin_surprise")
                .map("####################",
                     "#,,,,#......r..s.,,#",
                     "#,r,,#..B.......,,,#",
                     "#..r......r....#,,,#",
                     "#.......####.......#",
                     "#..B....#..#..r..Br#",
                     "#.......####.......#",
                     "#..................#",
                     "#....PP......E.....#",
                     "#....PP............#",
                     "####################")
                .legend('r', "giant_rat").legend('s', "giant_spider").extra("giant_rat", "giant_rat");

        // ---- Chapter 1: the Ashtail ambush. Slingers sit on ledges reached by narrow ramps.
        c.encounter("kobold_ambush").title("Ambush at the Ashtail Warren").backdrop("mine").music("goblin")
                .intro("Slingers whirl on the high ledges while warriors swarm below, led by a big dragonshield — Grizzlefang's champion. Kobolds fight dirty in packs: don't let them surround you! (Watch for the trap on the path.)")
                .map("######################",
                     "#s..#....d......#..s.#",
                     "#...#.......s...#....#",
                     "#.......k....k.......#",
                     "###.#####....######.##",
                     "#..k.,,.....B......k.#",
                     "#..B.....^......B....#",
                     "#.......,,,....E.....#",
                     "#....B.......,,..B...#",
                     "#.......PP..PP.......#",
                     "#....................#",
                     "######################")
                .legend('k', "kobold").legend('s', "kobold_slinger").legend('d', "wd_kobold_dragonshield").extra("kobold", "kobold");

        // ---- Chapter 2: the Hall of Kings.
        c.encounter("crypt").title("The Hall of Kings").backdrop("crypt").music("battle")
                .intro("Ghouls scrabble over the tombs while the Labyrinth Warden lowers its horns. (Ghoul claws can paralyse! Bludgeoning damage shatters the minotaur skeleton.)")
                .surpriseIf("crypt_surprise")
                .map("####################",
                     "#B.B.B.#..#.B.B.B..#",
                     "#......#..#........#",
                     "#..g.......m.......#",
                     "#......#.......g...#",
                     "#B.B.B.#..#.B.B.B..#",
                     "#..................#",
                     "#...,,,......,,,...#",
                     "#.......E..........#",
                     "#.......PPPP.......#",
                     "#..................#",
                     "#########..#########")
                .legend('g', "ghoul").legend('m', "minotaur_skeleton").extra("ghoul");

        // ---- Chapter 2: Durgrim's skull and his forge-guards.
        c.encounter("flameskull").title("The Forge Guardian").backdrop("hall").music("battle")
                .intro("Durgrim's burning skull hovers over the cold forge while his forge-guard armour marches. (The skull resists piercing — and the forge-pits burn anyone who stands in them.)")
                .map("####################",
                     "#LL......f......LL.#",
                     "#L..#..........#..L#",
                     "#...#...LLLL...#...#",
                     "#.......LLLL.......#",
                     "#..a............a..#",
                     "#...#..........#...#",
                     "#...#....E.....#...#",
                     "#..................#",
                     "#.......PPPP.......#",
                     "#########..#########")
                .legend('f', "wd_forge_skull").legend('a', "animated_armor").extra("skeleton");

        // ---- Chapter 3: the Great Bridge. Shove them into the chasm!
        c.encounter("bridge").title("The Great Bridge of Morduin").backdrop("bridge").music("battle")
                .intro("The Gutbreakers stride onto a railless bridge over a bottomless chasm. Shove them off the edge! (The Golden Spurs fight beside you if you befriended them.)")
                .ally("wd_glint_ally", "rivals_saved")
                .map("########################",
                     "#.....CCCCCCCCCCCC.....#",
                     "#..B..CCCCCCCCCCCC..u.n#",
                     "#.....CCCCCCCCCCCC.....#",
                     "#.PP..============..o..#",
                     "#.PP..============.....#",
                     "#.A...CCCCCCCCCCCC.u...#",
                     "#.....CCCCCCCCCCCC..u..#",
                     "#..B..CCCCCCCCCCCC..E.n#",
                     "########################")
                .legend('o', "ogre").legend('u', "bugbear").legend('n', "gnoll").extra("bugbear");

        // ---- Chapter 3: Skarr's hatchery (acid pools!).
        c.encounter("shaman").title("High Wyrmspeaker Skarr").backdrop("seacave").music("battle").objective("defeat:wd_skarr")
                .intro("Skarr's dragonshields lock shields while slingers scramble up the rocks. Bring down the Wyrmspeaker! (Black pools are acid — don't stand in them.)")
                .surpriseIf("skarr_surprise")
                .phase(50, "Skarr shrieks a prayer to the Black Mother — and kobold zealots burst out of the egg-heaps!",
                        Json.obj("op", "summon", "monster", "wd_kobold_zealot", "count", 2),
                        Json.obj("op", "say", "text", "\"MOTHER! MOTHER, YOUR CHILDREN NEED YOU!\""))
                .map("######################",
                     "#~~~..s.....GG...s.~~#",
                     "#~~....d...GGG..d...~#",
                     "#.....,,....S.......~#",
                     "#..GG..........GG....#",
                     "#..GG....B..B..GG....#",
                     "#........E...........#",
                     "#....,,........,,....#",
                     "#..B......PPPP....B..#",
                     "#....................#",
                     "#~~~~..........~~~~~~#",
                     "######################")
                .legend('S', "wd_skarr").legend('d', "wd_kobold_dragonshield").legend('s', "kobold_slinger").extra("wd_kobold_zealot");

        // ---- FINALE: the Black Mother on her hoard.
        c.encounter("nyxathrax").title("Nyxathrax, the Black Mother").backdrop("hoard").music("dragon").objective("defeat:wd_nyxathrax")
                .intro("NYXATHRAX uncoils from her hoard. Her three DRAGON EGGS feed her strength — while they pulse, she heals every turn (smash them!). Her ACID BREATH is a long line: never stand in a row. Gold piles give cover; the acid pools burn; the chasm swallows anyone shoved into it.")
                .surpriseIf("dragon_surprised")
                .weakenIf("dragonbane")
                .ally("wd_skritch_ally", "kobold_peace")
                .ally("wd_glint_ally", "rivals_saved")
                .phase(66, "Nyxathrax shrieks and beats her vast wings, rising above her hoard — and the cavern floor cracks open. ACID GEYSERS begin to erupt!",
                        Json.obj("op", "hazard", "count", 3, "type", "acid", "dice", "2d6", "name", "Acid geysers", "dc", 13),
                        Json.obj("op", "say", "text", "\"You scratched me. YOU SCRATCHED ME. I will melt you down and polish you into my hoard!\""))
                .phase(33, "The Black Mother screams for her children — and kobold zealots pour out of the coin-dunes, shrieking her name!",
                        Json.obj("op", "summon", "monster", "wd_kobold_zealot", "count", 2),
                        Json.obj("op", "say", "text", "\"CHILDREN! DEFEND YOUR MOTHER! Die for me, it's what you're FOR!\""))
                .map("##########################",
                     "#BBB..e.......#.....e.BBB#",
                     "#BB.....................B#",
                     "#B.....GG....N......GG...#",
                     "#......GG...........GG...#",
                     "#..#..........e.......#..#",
                     "#.......CCCC....CCCC.....#",
                     "#.......CCCC....CCCC.....#",
                     "#..B..................B..#",
                     "#...,,..#..........#.,,..#",
                     "#..GG...............GG...#",
                     "#.......................E#",
                     "#....B....PP..PP....B....#",
                     "#.........PA..AP.........#",
                     "##########################")
                .legend('N', "wd_nyxathrax").legend('e', "wd_dragon_egg").extra("wd_kobold_zealot");
    }
}
