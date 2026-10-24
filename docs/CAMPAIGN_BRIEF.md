# Brief for writing a built-in Emberwild campaign

You are writing ONE full-length campaign for Emberwild, a multiplayer D&D 2024 campaign engine in Java.
It will ship built-in AND as an editable starter template for a team of Java developers, so the code
must be clean, well-commented and a pleasure to read and copy from.

## Read first (in this repo)
- `src/emberwild/story/Emberwild.java` — the core campaign. **Copy its structure and quality bar exactly**
  (pregens(), monsters(), chapter methods, encounters()). Match its prose quality: vivid, funny, warm,
  second-person present tense, 2-4 short paragraphs per scene, named NPCs with personality.
- `src/emberwild/story/Campaign.java` — the DSL (Scene, Choice, Outcome, Encounter builders).
- `src/emberwild/story/Fx.java` — story effects (flag, item, gold, xp, heal, damage/damageOne, rest, level,
  log, inspiration, companion, attitude...).
- `src/emberwild/rules/Monsters.java` — the bestiary (ids you can use) + MonsterDef builder for custom monsters.
- `src/emberwild/rules/Items.java`, `Spells.java`, `Classes.java`, `Species.java`, `Backgrounds.java` — ids.
- `resources/templates/README.md` — map symbols and choice types.

## File rules
- Create exactly one file: `src/emberwild/story/<ClassName>.java`, `package emberwild.story;`,
  `public final class <ClassName> implements CampaignScript`. Also add explicit imports
  (`import emberwild.story.Campaign;` etc. and `import static emberwild.story.Fx.*;`) so the file still
  compiles if someone deletes the package line and drops it in the campaigns/ folder as a template.
- Do NOT modify any other file. If you find an engine bug, describe it in your final report instead.
- Custom monsters go in the campaign via `c.monster(id, name)...` (see how Emberwild defines big_hollis).
  Prefix custom ids with a short campaign tag to avoid clashes (e.g. `dc_sea_hag`).
- Custom items via `c.item(id, name, kind, desc, Json.obj("bonus", Json.obj("ac", 1), "attunable", true, "value", 300))`.

## Size and shape (this is a FULL campaign — roughly 1-2 hours of play)
- 38-50 scenes, 5 chapters (Prologue + 3 chapters + Finale), 6-8 possible battles (some optional/branching),
  4-6 booby traps (use `.check("save:dex", DC).each()` with `damageOne(...)` fail effects, plus clever
  spell/skill bypasses), 25+ skill checks, several spell solutions (charm_person, fire_bolt, ray_of_frost,
  druidcraft, shocking_grasp, mage_hand isn't learnable by default — prefer spells pregens actually know).
- A social hub early on (tavern / town) with 4-6 optional activities, one shop scene (`.shop()`), an NPC quest-giver,
  meaningful branches that reconverge, flags that pay off later (an ally in the boss fight via
  `encounter.ally(monsterId, flag)`, a boss weakness via `encounter.weakenIf(flag)`, surprise via `surpriseIf`).
- Milestone levelling with `Fx.level(n)` in onEnter of key scenes, long rests at sensible points,
  `.checkpoint()` before major fights (defeat with no defeat outcome sends the party back to the last checkpoint).
- A climactic boss encounter with `.phase(hpPercent, text, effects...)` using ops:
  `Json.obj("op","hazard","count",3,"type","acid","dice","2d6","name","Acid geysers","dc",13)` (telegraphed eruptions),
  `Json.obj("op","summon","monster",id,"count",2)`, `Json.obj("op","say","text","...")`.
  Optional: destructible objects that heal the boss — give the boss trait `"fed_by_objects"` and put
  `role("object")` monsters on the map on the boss's side (like Vharkas's braziers).
- Epilogue scene with `.anchor("epilogue")` and optional flag-dependent payoffs, ending with `.go("@end")`.
- Declare anchors so other tales can hook in: put `.anchor("tavern")` on the hub scene, `.anchor("road")` or
  `.anchor("camp")` somewhere mid-campaign, `.anchor("dungeon")` before the final dungeon, `.anchor("epilogue")`.
  Also add ONE `c.hook(...)` so your tale appears inside the core Emberwild campaign (e.g. anchor "epilogue":
  "A ship captain at the feast offers you passage…") pointing at your start scene. (When reached via a hook the
  party keeps playing your whole tale; your epilogue's `@end` will end the session — that's fine.)
- 3 claimable pregens (varied classes/species, fun personalities) + 2 npc companions (`hero.npc = true`, used with
  `companion(id, 3)` in an early scene's onEnter exactly like Emberwild's Brannoc & Wren).

## Available values
- backdrops: tavern, town, road, forest, bridge, swamp, grove, camp, village, chapel, ruins, cave, dungeon, hall,
  lair, coast, ship, snow, crypt, mine, seacave, hoard
- time: day, dawn, dusk, night · weather: none, rain, emberrain, embers, fog, ash
- music: tavern, ballad, brawl, explore, camp, grove, eerie, mystery, sorrow, tension, goblin, ruins, battle,
  cindrel, boss, hope, triumph, sea, shanty, snow, dungeon, dragon
- battle map tiles: `#` wall · `.` floor · `,` brush (difficult) · `~` water (difficult) · `T` tree (blocks) ·
  `B` barrel/crate (half cover, difficult) · `L` fire/lava hazard · `G` acid/poison pool hazard · `C` chasm
  (impassable, shove enemies in to kill them!) · `I` ice (difficult) · `=` bridge · `^` hidden trap ·
  `P` party start (give at least 4) · `E` extra-monster spawn · `A` ally spawn · other letters → `.legend(...)`.
  Maps up to 32×24; aim for 18-26 wide, 10-14 tall, interesting terrain that rewards tactics.
- Levels go 1→5. Heroes get Extra Attack and 3rd-level spells (Fireball, Lightning Bolt, Spirit Guardians...) at 5.
- Encounter scaling: base design for 3 heroes; `.extra(monsterIds...)` added per hero beyond 3.

## Balance & testing (mandatory)
Build:   `cd <repo> && mkdir -p build/classes && javac -Xlint:-unchecked -d build/classes -encoding UTF-8 $(find src -name '*.java') && cp -r resources/* build/classes/`
Validate + full AI playthroughs (all must reach the end):
`java -cp build/classes emberwild.Main --home /tmp/<yourname> --selftest 6 <campaign_id>`
Balance each encounter at the level the party will be (10 runs, 1 = only first pregen + companions to fill 3):
`java -cp build/classes emberwild.Main --home /tmp/<yourname> --battletest <campaign_id> <encounter> <level> none 10 1`
Targets: ordinary fights 70-95% AI win rate, bosses 50-75% (players beat the AI). Tune with HP/AC/count, not by
removing drama. Any `[<id>] ERROR` printed at startup must be fixed; WARNs should be fixed too.

## Report back
Path of your file, scene/battle/trap counts, the battletest table for every encounter, the selftest summary line,
and any engine issues you noticed.
