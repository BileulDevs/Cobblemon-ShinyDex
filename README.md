# Cobblemon ShinyDex

A Cobblemon add-on that turns Pokédex completion into a real goal: track your progress region by region, and earn a Shiny Charm that actually improves your shiny odds — once you've earned the right to wear it.

Runs on **Fabric** and **NeoForge**, Minecraft **1.21.1**, Cobblemon **1.8.0 or later**.

Species are read from Cobblemon's *implemented* list, so Pokémon added by datapacks and addons are picked up automatically — no update needed on this side.

[![CurseForge](https://img.shields.io/curseforge/dt/1328181?logo=curseforge&label=Downloads&color=f16436)](https://www.curseforge.com/minecraft/mc-mods/cobblemon-shiny-dex)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green?logo=minecraft)](https://www.minecraft.net)
[![Fabric](https://img.shields.io/badge/Fabric-supported-dbb37d?logo=fabric)](https://fabricmc.net)
[![NeoForge](https://img.shields.io/badge/NeoForge-supported-e04e14)](https://neoforged.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## Features

### Shiny Charm

A wearable accessory that raises your shiny encounter rate. It is not a starting item: by default the charm is awarded when you complete the **National Pokédex**, and it only works for a player who has earned it. Equipping it any other way does nothing.

Everything about it is configurable and reloadable without restarting the server: the rate, which regions are required, whether legendaries, mythicals, paradox Pokémon and Ultra Beasts count, whether species must be caught or only seen — or whether the charm can be earned at all.

### Pokédex tracking

Progress is tracked for all ten regions — Kanto, Johto, Hoenn, Sinnoh, Unova, Kalos, Alola, Galar, Paldea and National.

For each one you can see how many species are implemented, how many you've seen, how many you've caught, and the exact completion percentage. The missing list is paginated, colour-coded by type, and the page arrows are clickable.

### Advancements

One advancement per region, plus the National Pokédex as the final challenge, and a **Shiny Charm** advancement that awards the charm once the server's requirement is met.

### Languages

English, French, German, Spanish (Spain and Mexico), Italian, Brazilian Portuguese, Russian, Ukrainian, Polish, Dutch, Turkish, Japanese, Korean and Chinese (Simplified and Traditional). Pokémon names come from Cobblemon's own translations, so they follow whatever language pack you have installed and cover every species Cobblemon knows.

---

## Commands

| Command | Permission | What it does |
|---|---|---|
| `/shinydex info` | all | Current shiny rate, as a ratio and a percentage |
| `/shinydex check` | all | Progress across every region, one line each |
| `/shinydex check <region>` | all | Progress for one region |
| `/shinydex missing <region> [page]` | all | Species you still need, 30 per page |
| `/shinydex completion <region>` | all | Full breakdown: implemented, seen, caught, percentage, status |
| `/shinydex charm` | all | Shiny Charm status, the rules in force, and your progress under those rules |
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

A file is created at `config/cobblemon_shinydex/main.json` on first launch:

```json
{
  "configVersion": 1,
  "shinyCharm": {
    "enabled": true,
    "spawnChance": 3750,
    "requireUnlock": true
  },
  "unlock": {
    "enabled": true,
    "regions": ["national"],
    "mode": "caught",
    "includeLegendaries": true,
    "includeMythicals": true,
    "includeParadox": true,
    "includeUltraBeasts": true,
    "extraExcludedLabels": []
  }
}
```

### `shinyCharm` — what the charm does

| Setting | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Master switch. `false` turns the charm off entirely: no shiny boost and nobody can earn it. Pokédex tracking keeps working. |
| `spawnChance` | `3750` | Denominator of the shiny chance for a player wearing the charm: 3750 means roughly 1 in 3750. |
| `requireUnlock` | `true` | `true`: the charm only works for — and can only be equipped by — a player who earned it. `false`: anyone wearing one benefits, however they got it. |

### `unlock` — how the charm is earned

| Setting | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Whether meeting the requirement awards the charm. Players who already earned it keep it. |
| `regions` | `["national"]` | Regions that must **all** be completed. Any of `kanto`, `johto`, `hoenn`, `sinnoh`, `unova` (or `unys`), `kalos`, `alola`, `galar`, `paldea`, `national`. An empty list `[]` means no requirement: every player receives the charm on their next check. |
| `mode` | `"caught"` | `"caught"`: every counted species must be caught. `"seen"`: seeing it is enough. |
| `includeLegendaries` | `true` | Count legendary Pokémon. |
| `includeMythicals` | `true` | Count mythical Pokémon. |
| `includeParadox` | `true` | Count paradox Pokémon. |
| `includeUltraBeasts` | `true` | Count Ultra Beasts. |
| `extraExcludedLabels` | `[]` | Any other Cobblemon species labels to leave out, e.g. `["baby"]`. |

These rules only apply to the charm. Region advancements and `/shinydex check`, `missing` and `completion` always measure the full Pokédex, caught. Use `/shinydex charm` to see your progress under the charm's rules.

Once a player has earned the charm, they keep it: tightening the rules later does not take it back.

### Validation

Out-of-range or unknown values are corrected rather than crashing — unknown regions are dropped, an invalid `mode` falls back to `caught` — and the corrected file is written back so you can see what actually applied. A `regions` list where nothing is valid is reset to `["national"]` rather than treated as "no requirement".

A config from an older version (`config/shinydex.json`, `{ "shinyCharmSpawnChance": ... }`) is migrated automatically: its rate is carried over to `config/cobblemon_shinydex/main.json`, and the old file is renamed `shinydex.json.old`.

Run `/shinydex reload` to pick up changes without a restart.

---

## Installation

1. Install Cobblemon 1.8.0 or later for Minecraft 1.21.1.
2. Install the accessory mod for your loader:
   - **Fabric** — [Trinkets](https://modrinth.com/mod/trinkets) (plus Fabric API and Fabric Language Kotlin)
   - **NeoForge** — [Curios](https://modrinth.com/mod/curios) (plus Kotlin for Forge)
3. Drop the ShinyDex jar matching your loader into `mods/`.
4. Launch.

### Dependencies

| | Fabric | NeoForge |
|---|---|---|
| Minecraft | 1.21.1 | 1.21.1 |
| Cobblemon | 1.8.0+ | 1.8.0+ |
| Accessory slot | Trinkets 3.10.0+ | Curios 9.5.1+ |
| Also required | Fabric API, Fabric Language Kotlin | Kotlin for Forge |

The charm uses the **necklace** slot. On NeoForge the mod declares that slot itself, so Curios works out of the box with no extra configuration.

---

## Server notes

Progress checks are throttled and skip any region already completed, so the mod's tick cost stays close to zero on an established server regardless of player count.

The unlock requirement is enforced where it matters — at spawn time, not just at the inventory slot. A charm obtained through a command, another mod, or a bug still has no effect without the advancement (unless `requireUnlock` is `false`).

Players who completed the National Pokédex on an earlier version keep their charm and are not given a second one.

---

## Building

```
./gradlew build
```

Jars land in `fabric/build/libs/` and `neoforge/build/libs/`. The `-dev` and `-sources` jars are build artefacts; the one to ship is the plain versioned jar.

The project is a standard Architectury multiloader layout: shared code in `common/`, loader-specific wiring in `fabric/` and `neoforge/`.

---

## Issues

Bug reports and suggestions go to the [issue tracker](https://github.com/BileulDevs/Cobblemon-ShinyDex/issues). For a shiny-rate problem, please include your `config/cobblemon_shinydex/main.json` and which loader you're on.