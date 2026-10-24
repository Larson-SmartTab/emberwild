# Campaigns folder

Everything in this folder is loaded when Emberwild starts. Two kinds of file work:

| File | How it's loaded | Good for |
|---|---|---|
| `*.json` | Read as data. Also what the in-game **Campaign Forge** saves. | Anyone — no coding needed |
| `*.java` | Compiled with the JDK at start-up. Must implement `emberwild.story.CampaignScript`. | The Java challenge 😉 |

Java uploaded through the browser goes to `pending/` first, because it runs as code. The host approves it from
the Campaign Forge (on the machine running Emberwild), or simply moves the file up into this folder.

Check a file without starting a game:

```
java -jar emberwild.jar --validate campaigns/MyTale.java
```

## How tales mesh together

Scenes in a campaign can be **anchors**. The core campaign has these:

| Anchor | Where it appears |
|---|---|
| `tavern` | The Gilded Antler's common room (Prologue) |
| `town` | Harrowmere market, morning before departure |
| `road` | The King's Road crossroads (Chapter 1) |
| `camp` | Night camp in the Thornwood (Chapter 1) |
| `ruins` | Outside the Cinderdeep Ruins (Chapter 3) |
| `epilogue` | The victory feast |

Add a `hook` to your campaign and your entry scene appears as an extra choice at that anchor in every
other campaign. End your tale with `"go": "@return"` and the party walks back to where they came from,
carrying whatever loot, XP and scars they picked up. Your own campaign can declare anchors too, so others
can hook into *yours*.

## Scene choices at a glance

* **go** — move to another scene (`@return`, `@end` and `@checkpoint` are special targets)
* **check** — a skill (`persuasion`), ability (`str`), saving throw (`save:dex`) or `luck` against a DC.
  `mode`: `best` (the party's best rolls, default), `group` (everyone; half must pass), `each` (everyone, failures
  suffer the fail effects individually — use this for traps), `chooser` (whoever voted for it).
* **spell** — needs a hero with that spell prepared; spends a slot (cantrips are free) then follows `pass`
* **battle** — an encounter id; `victory` / `defeat` outcomes (no defeat outcome = retry from the last checkpoint)
* **requires** — `flag`, `notFlag`, `item`, `class`, `species`, `skill`, `gold`, `visited`, `notVisited`
* **effects** (`op`): `flag`, `unflag`, `item`, `takeItem`, `gold`, `xp`, `heal`, `damage`, `rest`, `level`, `log`,
  `inspiration`, `companion`, `dismiss`, `attitude`, `counter`

## Sound and the lore-teller

Every scene gets a living soundscape worked out from its backdrop, weather and text — a tavern crowd that laughs
and clinks mugs, crackling fire, rain and thunder, wind, surf and gulls, cave drips, birdsong or crickets and owls.
Set it yourself with `ambience` (Java: `.ambience("crowd", "fire")`, JSON: `"ambience": ["crowd", "fire"]`) from:
crowd, fire, rain, storm, wind, sea, cave, forest, snow, quiet.

Give the lore-teller something to say while the party lingers with `aside` (Java: `.aside("Mind the sparks.")`,
JSON: `"asides": ["Mind the sparks."]`). A scene with a battle choice turns the music ominous by itself.

## Battle maps

ASCII, up to 32 × 24. `#` wall · `.` floor · `,` brush (difficult) · `~` water (difficult) · `T` tree (blocks) ·
`B` barrels/tables (half cover, difficult) · `L` lava/fire (2d6 fire) · `=` bridge · `^` hidden spike trap ·
`P` party start · `E` extra-monster spawn · `A` ally spawn · any other letter = a monster from your `legend`.

Monsters can be any from the core bestiary (wolf, goblin, goblin_archer, skeleton, bog_zombie, cultist,
cult_fanatic, thug, magma_mephit, animated_armor, will_o_wisp, giant_frog, dire_wolf, …) or your own.
