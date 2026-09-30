# mcRPG

A Minecraft RPG plugin for Paper, which pushes players to specialize and work together. Forked from and inspired by [mcMMO](https://github.com/mcMMO-Dev/mcMMO). Players train 19 skills by playing normally (mining, fighting, farming, brewing and more), but nobody masters everything. Each player picks two **Specializations**, whole categories of skills that level much faster than the rest.

**[Read the wiki](https://github.com/jaymingxyz/mcRPG/wiki)** for how Specializations work and a page on every skill.

## How it works

- **Specializations:** choose a skill category as your Primary Specialization (1.25× XP) and another as your Secondary (1.0× XP) with `/specialization` (`/csp`). Every skill in those categories earns that rate, and every other skill still levels at 0.35× XP.
- **Choices stick:** the only way to change a Specialization is `/abandonspecialization` (`/asp`), and every skill in the abandoned category keeps just 10% of its XP.
- **Category passives:** specialize in Survivalism, Blacksmithing, Metallurgy or Botany and train its skills to a combined 25 levels to unlock a bonus for leather, chainmail or copper armor, or for wooden tools.
- **Salvage and Smelting are full skills:** they earn their own XP, and Salvage Mastery returns more materials as you level.
- **A menu for every skill:** `/skills` shows your progress and explains every skill and ability in game.
- **Slow, meaningful progress:** leveling follows mcMMO's Standard mode, with no XP boost perks and no XP rate events.
- **Focused on skills:** mcMMO's parties and chat channels are removed.

## Skill categories

| Category | Skills | Passive |
|----------|--------|---------|
| Melee Combat | Swords, Axes, Maces, Spears | None |
| Ranged Combat | Archery, Crossbows, Tridents | None |
| Metallurgy | Mining, Smelting, Excavation | Copper Mastery |
| Botany | Woodcutting, Herbalism, Alchemy | Wooden Mastery |
| Blacksmithing | Repair, Salvage | Chainmail Mastery |
| Survivalism | Taming, Acrobatics, Fishing, Unarmed | Leather Mastery |

## Getting started

**Players:** pick your Specializations with `/specialization` (or `/csp`), browse the skills with `/skills`, and check your levels with `/rpgstats`. The wiki's [Getting Started](https://github.com/jaymingxyz/mcRPG/wiki/Getting-Started) page covers the rest.

**Server owners:** mcRPG is built for and exclusively tested on **Paper 26.3** and Java 17 or newer. Download `mcRPG.jar` from [Releases](https://github.com/jaymingxyz/mcRPG/releases), put it in your server's `plugins` folder and restart. See [Server Setup](https://github.com/jaymingxyz/mcRPG/wiki/Server-Setup) for the config files and storage options. Some other versions may work, use at your own risk.

mcRPG doesn't import mcMMO player data, so it's meant for fresh installs. Don't run it alongside mcMMO.

## Status

mcRPG 0.5.0 is the first beta, and it's being tested on a small server with friends. Expect bugs, and please [report them](https://github.com/jaymingxyz/mcRPG/issues)!

## Credits and license

mcRPG is a fork of mcMMO, created by nossr50 and developed by the mcMMO contributors, and it follows mcMMO's development branch. Like mcMMO, it's licensed under the GNU GPL v3; see [LICENSE](LICENSE) and [NOTICE](NOTICE). mcRPG isn't affiliated with or endorsed by the mcMMO project, and it isn't related to other plugins named McRPG.
