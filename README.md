# Emberwild — The Cinder Tyrant

A multiplayer D&D campaign engine written in Java. One person hosts; everyone else joins from a browser on
Windows, Mac, Linux, a tablet or a phone. Java runs the whole game: the 2024 rules, dice, story, tactical
combat, enemy AI, saves and the web server. The browser only draws it.

It ships with **Emberwild: The Cinder Tyrant**, a full campaign that takes roughly 1–2 hours: tavern life,
a road trip through a haunted forest, five booby traps, seven possible battles and a boss fight against
a drake made of living slag. The star of the show is the pre-made hero **Sylwen Ashbloom**, a charming
wood elf wizard who solves problems with fire, frost, lightning and a smile.

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
- **Sound & voice** (☰ menu): procedural music and effects, and an optional narrator that reads each scene.

Keyboard: **D** dice · **C** character sheet · **1–9** pick a choice · **Esc** cancel targeting.

---

## Rules coverage (D&D 2024 / "5.5e")

- Character creation in the 2024 order: **class → background → species → ability scores**. Backgrounds grant
  the ability increases (+2/+1 or +1/+1/+1 among three abilities), two skills and an Origin feat. Point buy,
  standard array or 4d6-drop-lowest.
- 8 classes (Barbarian, Bard, Cleric, Fighter, Paladin, Ranger, Rogue, Wizard) levels 1–3 with their 2024
  features and a subclass at level 3; 10 species with lineages; 12 backgrounds; 10 Origin feats; 40+ spells.
- Weapon Mastery (Topple, Sap, Vex, Slow, Push, Cleave, Graze), Heroic Inspiration, one-spell-slot-per-turn,
  Potent Cantrip, Arcane Recovery, Bardic Inspiration & Cutting Words, Sneak Attack, Divine Smite as a bonus
  action spell, Rage & Reckless Attack, Second Wind & Action Surge, and more.
- Monsters use the 2024 stat-block shape: multiattack, recharge abilities, legendary actions and
  resistance, Pack Tactics, Nimble Escape, Undead Fortitude, damage resistances / immunities /
  vulnerabilities.
- Levelling: milestones (level 2 after the first night, level 3 before the finale) plus XP from battles.

---

## Build your own tale (the team challenge)

Your teammates can add campaigns three ways, and they all **mesh**: every tale can hook into the others.

| How | Who it's for |
|---|---|
| **Campaign Forge** (⚒ in the top bar) | A visual editor in the browser: scenes, choices, checks, battle maps. Publishes straight into the realm. |
| **JSON** file in `campaigns/` | Hand-written data. Example: `campaigns/the_lost_lantern.json` |
| **Java** file in `campaigns/` | The real challenge. Implement `CampaignScript` with the same fluent DSL the core campaign uses. Example: `campaigns/TheSunkenBell.java` |

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
  game/                     Game (shared table, votes, checks, effects, saves), Combat (tactical engine + AI), Unit, SelfTest
  server/WebServer.java     zero-dependency HTTP + Server-Sent Events (JDK HttpServer, virtual threads)
  util/Json.java            tiny JSON reader/writer
resources/web/              the browser client (canvas backdrops, 3D dice, battle map, builder, forge)
resources/templates/        example campaigns copied into campaigns/ on first run
```

- **No dependencies.** Plain JDK 21. Build with `./build.sh` (macOS/Linux) or `build.bat` (Windows).
- **Self-test:** `java -jar emberwild.jar --selftest 9` lets the AI play the whole campaign nine times with
  one to three human heroes and prints per-battle win rates. All runs must reach the end.
- Command-line options: `--port N`, `--no-browser`, `--home DIR`, `--validate FILE`, `--selftest [N]`.
- All dice are rolled on the server with `SecureRandom`; clients only animate the results they're sent.
