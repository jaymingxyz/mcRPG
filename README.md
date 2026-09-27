# mcRPG

A Minecraft RPG plugin for Spigot and Paper, built on [mcMMO](https://github.com/mcMMO-Dev/mcMMO). Players train 19 skills by playing normally (mining, fighting, farming, brewing and more), but nobody masters everything: each player picks two **Specializations**, whole categories of skills that level much faster than the rest.

## How it works

- **Specializations:** choose a skill category as your Primary Specialization (1.25× XP) and another as your Secondary (1.0× XP) with `/choosespecialization` (`/csp`). Every skill in those categories earns that rate, and every other skill still levels at 0.35× XP.
- **Choices stick:** the only way to change a Specialization is `/abandonspecialization` (`/asp`), and every skill in the abandoned category keeps just 10% of its XP.
- **Slow, meaningful progress:** leveling follows mcMMO's Standard mode, with no XP boost perks and no XP rate events.
- **Salvage and Smelting are full skills:** they earn their own XP, and Salvage Mastery returns more materials as you level.
- **Focused on skills:** mcMMO's parties and chat channels are removed.

## Skill categories

| Category | Skills |
|----------|--------|
| Melee Combat | Swords, Axes, Maces, Spears |
| Ranged Combat | Archery, Crossbows, Tridents |
| Metallurgy | Mining, Smelting, Excavation |
| Botany | Woodcutting, Herbalism, Alchemy |
| Blacksmithing | Repair, Salvage |
| Survivalism | Taming, Acrobatics, Fishing, Unarmed |

## Getting started

mcRPG needs Spigot or Paper for Minecraft 1.20.5 or newer (Maces need 1.21, Spears 1.21.11) and Java 17 or newer. Put `mcRPG.jar` in your server's `plugins` folder and restart. Players use `/choosespecialization` (or `/csp`) to open the Specialization menu and `/rpgstats` to see their skills.

mcRPG doesn't import mcMMO player data, so it's meant for fresh installs.

## Status

Early development: mcRPG is currently being tested on a small servers with friends. Expect (and report) bugs!

## Credits and license

mcRPG is a fork of mcMMO, created by nossr50 and developed by the mcMMO contributors, and it follows mcMMO's development branch. Like mcMMO, it's licensed under the GNU GPL v3; see [LICENSE](LICENSE) and [NOTICE](NOTICE). mcRPG isn't affiliated with or endorsed by the mcMMO project, and it isn't related to other plugins named McRPG.
