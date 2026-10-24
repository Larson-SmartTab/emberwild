# Emberwild — The Cinder Tyrant

A multiplayer D&D campaign engine written in Java. One person hosts; everyone else joins from a browser on
Windows, Mac, Linux, a tablet or a phone. Java runs the whole game: the 2024 rules, dice, story, tactical
combat, enemy AI, saves and the web server. The browser only draws it.

It ships with **four full campaigns** (each roughly 1–2 hours) that link together into one long saga:

| Tale | Levels | Highlights | Final boss |
|---|---|---|---|
| **Emberwild: The Cinder Tyrant** | 1 → 3 | Tavern night, haunted Thornwood, five traps, slag drake | Vharkas, the Cinder Tyrant |
| **The Drowned Crown** | 1 → 3 | Shanty contests & crab races, a haunted lighthouse, sahuagin, a ghost ship | Mother Brine, the sea hag |
| **The Frostfell Pact** | 2 → 4 | Lantern festival, ice bridges & avalanches, riddle doors, a gentle ending if you earn it | Isolde, the Winter Queen |
| **Wyrmfire Deep** | 3 → 5 | Mining town, kobolds you can befriend, pendulum halls, a legendary sword | **Nyxathrax, a young black dragon on her hoard** 🐉 |

The star of the show is the pre-made hero **Sylwen Ashbloom**, a charming wood elf wizard who solves problems
with fire, frost, lightning and a smile. Every tale also has its own pre-made heroes and companions.

---

## Play in 60 seconds

1. Install **Java 21 or newer** — <https://adoptium.net/> (choose the JDK, not the JRE).
2. Double-click the launcher:
   - **Windows:** `Play Emberwild.bat`
   - **macOS:** `Play Emberwild.command` (first time: right-click → Open)
   - **Anywhere:** `java -jar emberwild.jar`
3. Your browser opens at `http://localhost:7777`. Type your name, take **Sylwen Ashbloom** (or build your own
   hero), and press **Begin this tale**.

### Bring friends

The console prints a "Friends on LAN" address such as `http://192.168.1.20:7777`. Anyone on the same network
opens it in a browser — no install needed. Over the internet, use any of these:

| Option | What to do |
|---|---|
| Tailscale (easiest, private) | Everyone installs Tailscale; share `http://<your-tailscale-ip>:7777` |
| ngrok | `ngrok http 7777` and share the https link |
| Cloudflare tunnel | `cloudflared tunnel --url http://localhost:7777` |
| Port forward | Forward TCP 7777 on your router to the host machine |

Everything happens live: when one player moves, rolls or votes, everybody sees it.

---

## How a session plays

- **Story scenes** are painted on an animated, full-screen backdrop (rain, embers, fog, fireflies, a
  flickering hearth, a lava lake) with narration on a vellum page and speaking NPCs.
- **Choices are voted on.** Everyone with a hero votes; the majority wins (30-second timer once someone
  votes; the host can press *Decide now*). Solo play is instant.
- **Skill checks use real dice.** When the story calls for one, the player whose hero is best at it gets a
  big *Roll d20* button and a 3D d20 tumbles onto the table for everyone. Some checks have the whole group
  roll; traps make *each* hero save separately.
- **Spells and class abilities matter outside combat**: Charm Person a frightened mapmaker, freeze a trap's
  gears with Ray of Frost, make a flower bloom in a gambler's ale with Druidcraft.
- **Battles** switch to a tactical grid: initiative, movement with opportunity attacks, Action / Bonus
  Action / Reaction economy, spells with area templates you aim with the mouse, Weapon Mastery, conditions,
  concentration, death saving throws, legendary actions, a boss with phases and telegraphed eruptions.
  Anyone who steps away is played by the AI so the table never stalls.
- **Companions** (Brannoc the dwarf fighter and Wren the cleric) join automatically when the party has
  fewer than three heroes, and step aside when more players arrive.
- **Free dice tray** (press **D**): roll anything (`4d6kh3`, `2d20kh1`, `8d6`) for the whole table to see.
- **Saves**: the host can save and load from the ☰ menu; the game also autosaves at checkpoints.
- **Speak in character** (box under the story): pick your hero, choose 💬 *Say* or ✋ *Do*, type — or press 🎤
  and talk — and press *Speak*. Your line is voiced aloud for the whole table, and the NPC in the scene answers
  in their own voice. Good roleplay can earn a skill check and Heroic Inspiration. The chat box also has an
  *In character* mode.
- **Voices** (☰ → Voices): a narrator, a different voice for every NPC and hero (matched to species, gender
  and monster type), and battle chatter in speech bubbles. See *Natural voices* below to make them sound human.
- **Sound**: procedural music for every mood (tavern, sea shanty, snow, dungeon, dragon) and effects.
- **Continue** from the lobby: the game autosaves, and the host gets a *Continue your last adventure* button.

Keyboard: **D** dice · **C** character sheet · **1–9** pick a choice · **Esc** cancel targeting.

### Battles

Every hero and monster is drawn as a full-body miniature (armour, weapons, robes, wings, tails, spider legs)
that steps, lunges, casts and flinches. Health bars and conditions float above their heads, and enemies taunt
you in speech bubbles. Maps have pillars, difficult terrain, lava, acid pools, ice and **chasms** — use the
**Shove** action to push enemies over the edge. Bosses have phases, legendary actions, hazards, minions and
objects that feed them (smash the idols, totems or eggs!).

---

## The AI Dungeon Master (optional)

Out of the box, NPCs answer your in-character lines with a built-in rules engine. Plug in a language model and
they improvise properly — staying in character and never changing the plot. Configure it in **☰ → AI Dungeon
Master** (host only), or on the command line:

| Provider | Command |
|---|---|
| **Ollama** (free, local) | `java -jar emberwild.jar --ai ollama --ai-model gemma3` (uses `http://localhost:11434`; Docker is fine) |
| Any OpenAI-compatible server (LM Studio, vLLM, OpenAI…) | `--ai openai --ai-url http://host:port/v1 --ai-model NAME` |
| Anthropic Claude | set `ANTHROPIC_API_KEY`, then `--ai anthropic` |
| Built-in rules (default) | `--ai rules` |

The settings are saved in `emberwild-settings.json`. *Test* in the menu checks the connection.

## Natural voices (optional)

By default each browser uses its best built-in voices (Edge's "Natural" voices and macOS Siri/Premium voices
sound great). For studio-quality voices everyone at the table hears, install **Piper**, a free offline neural
text-to-speech engine:

1. Download Piper for your OS from <https://github.com/rhasspy/piper/releases> and unzip it into a `piper/`
   folder next to `emberwild.jar` (so `piper/piper` or `piper\piper.exe` exists).
2. Download one or more voices from <https://huggingface.co/rhasspy/piper-voices> — each needs the `.onnx` **and**
   the `.onnx.json` file — into a `voices/` folder next to the jar. Good picks: `en_GB-alba-medium`,
   `en_US-ryan-high`, `en_GB-northern_english_male-medium`, `en_US-lessac-high`.
3. Restart. The console says `Voices: Piper neural TTS (N voices)`, and ☰ → Voices lets you pick Piper.

Elsewhere: `--piper PATH`, `--voices DIR` or the `EMBERWILD_PIPER` environment variable.

---

## Rules coverage (D&D 2024 / "5.5e")

- Character creation in the 2024 order: **class → background → species → ability scores**. Backgrounds grant
  the ability increases (+2/+1 or +1/+1/+1 among three abilities), two skills and an Origin feat. Point buy,
  standard array or 4d6-drop-lowest.
- 8 classes (Barbarian, Bard, Cleric, Fighter, Paladin, Ranger, Rogue, Wizard) **levels 1–5** with their 2024
  features: subclass at 3, Ability Score Improvement at 4, Extra Attack, 3rd-level spells (Fireball, Lightning
  Bolt, Spirit Guardians, Revivify, Hypnotic Pattern, Mass Healing Word), Uncanny Dodge and more at 5.
  10 species with lineages; 12 backgrounds; 10 Origin feats; 50+ spells.
- Weapon Mastery (Topple, Sap, Vex, Slow, Push, Cleave, Graze), Heroic Inspiration, one-spell-slot-per-turn,
  Potent Cantrip, Arcane Recovery, Bardic Inspiration & Cutting Words, Sneak Attack, Divine Smite as a bonus
  action spell, Rage & Reckless Attack, Second Wind & Action Surge, and more.
- The Shove action (push 5 ft or knock prone) as part of the Attack action.
- 50+ monsters use the 2024 stat-block shape: multiattack, recharge abilities, legendary actions and
  resistance, Pack Tactics, Nimble Escape, Undead Fortitude, damage resistances / immunities /
  vulnerabilities.
- Levelling: story milestones plus XP from battles. XP alone goes to level 3; later tales grant 4 and 5 as milestones.

---

## Build your own tale (the team challenge)

Your teammates can add campaigns three ways, and they all **mesh**: every tale can hook into the others.

| How | Who it's for |
|---|---|
| **Campaign Forge** (⚒ in the top bar) | A visual editor in the browser: scenes, choices, checks, battle maps. Publishes straight into the realm. |
| **JSON** file in `campaigns/` | Hand-written data. Example: `campaigns/the_lost_lantern.json` |
| **Java** file in `campaigns/` | The real challenge. Implement `CampaignScript` with the same fluent DSL the core campaign uses. Example: `campaigns/TheSunkenBell.java` |
| **Starter templates** in `campaigns/starters/` | Complete copies of The Drowned Crown, The Frostfell Pact and Wyrmfire Deep. Copy one up into `campaigns/`, rename the class and id, and rewrite it into your own full-length tale. |

A Java tale looks like this:

```java
import emberwild.story.*;
import static emberwild.story.Fx.*;

public class MyTale implements CampaignScript {
    public Campaign build() {
        Campaign c = Campaign.create("my_tale", "The Whispering Well", "Jordan");
        c.hook("tavern", "Ask about the well that talks back", "well");   // appears in the Gilded Antler

        c.scene("well").title("The Whispering Well").backdrop("village").time("dusk").music("eerie")
            .npc("The Well", "Ancient and Bored", "#3d6b7a", "🪣", "Drop a coin, hear a secret.")
            .text("The old well hums a tune nobody taught it.")
            .choice("Drop a coin").icon("🪙").effect(gold(-1)).check("insight", 12)
                .pass("The well whispers where the bandits hid their loot.", "loot", xp(50))
                .fail("The well tells a very long joke with no ending.", "@return")
            .choice("Charm the well").spell("charm_person").pass("Wells can be charmed. Who knew?", "loot")
            .choice("Leave").go("@return");

        c.scene("loot").title("Bandit Gold").text("Exactly where the well said.")
            .choice("Grab it and go").effect(gold(40), item("potion_healing")).go("@return");
        return c;
    }
}
```

- Drop the file in `campaigns/` and restart, or upload it in the Forge (the host approves Java before it
  runs, because it is code).
- Check it without starting a game: `java -jar emberwild.jar --validate campaigns/MyTale.java`
- Balance your fights: `java -jar emberwild.jar --battletest my_tale my_encounter 3 none 40` plays an
  encounter 40 times at level 3 and prints the win rate (aim for 70–95% for normal fights, 50–75% for bosses).
  `docs/CAMPAIGN_BRIEF.md` is the full writing guide the bundled tales were built from.
- Anchors in the core campaign: `tavern`, `town`, `road`, `camp`, `ruins`, `epilogue`. End your tale with
  `go("@return")` and the party walks back to where they left the main story, keeping any loot and XP.
- Characters can be written in Java too: the character builder's **Copy as Java** button produces the code.

`campaigns/README.md` lists every choice type, effect and map symbol.

---

## For developers

```
src/emberwild/
  Main.java                 command line, start-up, browser launch
  rules/                    2024 rules data and maths: Dice, Hero, Classes, Species, Backgrounds, Spells, Items, Monsters
  story/                    Campaign model + fluent DSL, Fx effects, loader/validator/compiler, the core campaign (Emberwild.java)
  story/*.java              the four built-in tales: Emberwild, DrownedCrown, FrostfellPact, WyrmfireDeep
  game/                     Game (shared table, votes, checks, effects, saves), Combat (tactical engine + AI), Unit,
                            DungeonMaster (rules / Ollama / OpenAI-compatible / Anthropic), SelfTest
  server/WebServer.java     zero-dependency HTTP + Server-Sent Events (JDK HttpServer, virtual threads)
  server/Tts.java           optional Piper neural text-to-speech
  util/Json.java            tiny JSON reader/writer
resources/web/              the browser client (canvas backdrops, 3D dice, battle map + miniatures, voices, builder, forge)
resources/templates/        example campaigns and starter templates copied into campaigns/ on first run
```

- **No dependencies.** Plain JDK 21. Build with `./build.sh` (macOS/Linux) or `build.bat` (Windows).
- **Self-test:** `java -jar emberwild.jar --selftest 9 [campaign_id]` lets the AI play a whole campaign nine
  times with one to three heroes and prints per-battle win rates. All runs must reach the end.
- Command-line options: `--port N`, `--no-browser`, `--home DIR`, `--validate FILE`, `--selftest [N] [id]`,
  `--battletest ID ENCOUNTER LEVEL [flags|none] [runs]`, `--ai`, `--ai-model`, `--ai-url`, `--piper`, `--voices`.
- All dice are rolled on the server with `SecureRandom`; clients only animate the results they're sent.
