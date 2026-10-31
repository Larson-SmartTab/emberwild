# Ready-to-upload campaign files

For Emberwild servers that don't have a campaign built in yet. Upload through the **Campaign Forge** (⚒ → upload),
or drop the file into the server's `campaigns/` folder and restart.

| File | Notes |
|---|---|
| `TheGem.json` | **The Gem, a story by Joel Hill** — recommended. Data only, so it goes live immediately (no approval) and works on any Emberwild 2.x server. |
| `RelicCrownShadow.json` | **The Relic of Crown and Shadow, a story by Joel Hill** — Joel's second edition of The Gem: four ways across the ravine, a fourth door symbol, post-battle chests (watch for the Treasure Mimic), two merchant camps at his prices, the toll, the tracker, the perimeter wall, the knight's tribute and the hidden shadow door (levels 1-3). Data only; works on any Emberwild 2.x server, including ones with The Gem built in (different id). Source: `src/emberwild/story/RelicCrownShadow.java`. |
| `AnotherMainStation.json` | **Another Main Station** — the SmartTab one-shot (Payam, Chad the Toast rep, and ANOTHER main station). Data only; works on any Emberwild 2.x server. Source: `src/emberwild/story/AnotherMainStation.java`. |
| `SmartTabMountainQuest.json` | **SmartTab Mountain Quest, a story by Vijay** — onboarding, the MagTek Mines, the ExpiTrans Bridge, the Toast King, and three endings (levels 1-4). Data only; works on any Emberwild 2.x server. Source: `src/emberwild/story/MountainQuest.java`. |
| `ToastLegion.json` | **SmartTab POS vs the Toast Legion, a story by Dre Ryman** — Toast Goblins, Gary's IT shop, the Toast KDS Ogre and Toast Tech Support (levels 1-3). Data only; works on any Emberwild 2.x server. Source: `src/emberwild/story/ToastLegion.java`. |
| `TheGem.java` | The same tale as Java source. Java uploads wait for the host to approve them in the Forge. Compiles on Emberwild 2.1 and later. |

Emberwild 2.3+ already includes The Gem, so don't upload it there (the id `the_gem` is taken).
