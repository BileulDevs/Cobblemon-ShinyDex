# Cobblemon ShinyDex

A Cobblemon add-on that turns Pokédex completion into a real goal: track your progress region by region, and earn a Shiny Charm that actually improves your shiny odds — once you've earned the right to wear it.

Runs on **Fabric** and **NeoForge**, Minecraft **1.21.1**, Cobblemon **1.8.0**.

Species are read from Cobblemon's *implemented* list, so Pokémon added by datapacks and addons are picked up automatically — no update needed on this side.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green?logo=minecraft)](https://www.minecraft.net)
[![Fabric](https://img.shields.io/badge/Fabric-supported-dbb37d?logo=fabric)](https://fabricmc.net)
[![NeoForge](https://img.shields.io/badge/NeoForge-supported-e04e14)](https://neoforged.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## Features

### Shiny Charm

A wearable accessory that raises your shiny encounter rate. It is not a starting item: the charm drops when you complete the **National Pokédex**, and it only works for a player who has that advancement. Equipping it any other way does nothing.

The rate is configurable and reloadable without restarting the server.

### Pokédex tracking

Progress is tracked for all ten regions — Kanto, Johto, Hoenn, Sinnoh, Unova, Kalos, Alola, Galar, Paldea and National.

For each one you can see how many species are implemented, how many you've seen, how many you've caught, and the exact completion percentage. The missing list is paginated, colour-coded by type, and the page arrows are clickable.

### Advancements

One advancement per region, plus the National Pokédex as the final challenge — which is what awards the Shiny Charm.

### Languages

English and French. Pokémon names come from Cobblemon's own translations, so they follow whatever language pack you have installed and cover every species Cobblemon knows.

---

## Commands

| Command | Permission | What it does |
|---|---|---|
| `/shinydex info` | all | Current shiny rate, as a ratio and a percentage |
| `/shinydex check` | all | Progress across every region, one line each |
| `/shinydex check <region>` | all | Progress for one region |
| `/shinydex missing <region> [page]` | all | Species you still need, 30 per page |
| `/shinydex completion <region>` | all | Full breakdown: implemented, seen, caught, percentage, status |
| `/shinydex reload` | OP level 2 | Reload the config file without restarting |

Valid regions: `kanto`, `johto`, `hoenn`, `sinnoh`, `unova`, `kalos`, `alola`, `galar`, `paldea`, `national`. `unys` is accepted as an alias for Unova.

The `missing` list shows the Pokédex number in gold and the species name in its primary type's colour. Page arrows at the bottom are clickable — no need to retype the command to move through the list.

```
/shinydex missing kanto
/shinydex missing unova 2
/shinydex completion national
```

---

## Configuration

A file is created at `config/shinydex.json` on first launch:

```json
{
  "shinyCharmSpawnChance": 3750
}
```

`shinyCharmSpawnChance` is the denominator of the shiny chance: 3750 means roughly 1 in 3750 for a player wearing the charm. Lower it for a more generous server, raise it for a harsher one.

Out-of-range values are clamped rather than crashing, and the corrected value is written back to the file so you can see what actually applied. Run `/shinydex reload` to pick up changes without a restart.

---

## Installation

1. Install Cobblemon 1.8.0 for Minecraft 1.21.1.
2. Install the accessory mod for your loader:
   - **Fabric** — [Trinkets](https://modrinth.com/mod/trinkets) (plus Fabric API and Fabric Language Kotlin)
   - **NeoForge** — [Curios](https://modrinth.com/mod/curios) (plus Kotlin for Forge)
3. Drop the ShinyDex jar matching your loader into `mods/`.
4. Launch.

### Dependencies

| | Fabric | NeoForge |
|---|---|---|
| Minecraft | 1.21.1 | 1.21.1 |
| Cobblemon | 1.8.0 | 1.8.0 |
| Accessory slot | Trinkets 3.10.0+ | Curios 9.5.1+ |
| Also required | Fabric API, Fabric Language Kotlin | Kotlin for Forge |

The charm uses the **necklace** slot. On NeoForge the mod declares that slot itself, so Curios works out of the box with no extra configuration.

---

## Server notes

Progress checks are throttled and skip any region already completed, so the mod's tick cost stays close to zero on an established server regardless of player count.

The National Pokédex requirement is enforced where it matters — at spawn time, not just at the inventory slot. A charm obtained through a command, another mod, or a bug still has no effect without the advancement.

---

## Building

```
./gradlew build
```

Jars land in `fabric/build/libs/` and `neoforge/build/libs/`. The `-dev` and `-sources` jars are build artefacts; the one to ship is the plain versioned jar.

The project is a standard Architectury multiloader layout: shared code in `common/`, loader-specific wiring in `fabric/` and `neoforge/`.

---

## Issues

Bug reports and suggestions go to the [issue tracker](https://github.com/BileulDevs/Cobblemon-ShinyDex/issues). For a shiny-rate problem, please include your `config/shinydex.json` and which loader you're on.