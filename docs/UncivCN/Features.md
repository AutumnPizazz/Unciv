# UncivCN Features

This page describes the features the UncivCN branch adds over upstream. Version in brackets = when the feature was introduced.

## Lua scripting (since 4.20.8.2, continuously extended)

Mods can attach Lua scripts for logic far beyond plain JSON:

- **Triggers**: the `TriggerLuaFunction` unique (buildings/techs/policies/eras/events/units/global uniques) and game lifecycle hooks (`<upon turn start>` etc.)
- **ctx context object**: scripts can access `civ`, `city`, `unit`, `tile`, `game` APIs, plus `ctx.parameter` (Countable-resolved), `ctx.store` (persistent storage), `ctx.count` / `ctx.evaluateConditional`
- **Safety**: a hardened sandbox (no `io`/`os`/`luajava`/`package` escape, `string.dump` removed) and an instruction budget per script load / function call so runaway loops can't freeze the game
- **Error reporting**: runtime errors show the script name and line number to players; errors raised during AI turns are recorded in the mod checker instead of being lost
- **Tooling**: the in-game mod checker and the `mod-ci` CLI check Lua syntax, function references and API spelling (with suggestions); generated EmmyLua type definitions (`docs/Modders/lua-api.lua`) give autocompletion and hover docs in LuaLS-capable editors; a starter template (`docs/Modders/examples/LuaStarterMod/`) gets you going in minutes; convenience APIs like `game.findTiles`

Full tutorial: [Lua modding](/Modders/Lua-Modding).

## Mirrored and ring maps (since 4.20.7.3 / 4.20.7.4 / 4.21.0)

- **Three mirrored map modes**: selectable when creating a game; resources are also mirrored symmetrically (4.20.7.4)
- **Ring maps**: the map wraps around; the radius parity restriction for rotationally symmetric maps is lifted (4.20.17.2)

## Unit pins / map pins (4.20.17.2)

Two independent toggles on the world screen, left of the minimap in the bottom-right:

- **Unit pin**: when enabled, click any unit (including foreign units) to attach a note, shown right below the unit's figure; click again to edit or delete; a picker appears when multiple units share a tile
- **Map pin**: click any tile (including empty ones) to attach a note, shown on the tile with priority over food/production/gold icons

**Editing notes (4.21.6.1)** - no mode switch needed, normal clicking is never hijacked:
- Mobile: long-press a tile / unit to edit its note (when no unit is selected; with a selected unit long-press still moves it, as before)
- Desktop: Alt+click a tile / unit to edit its note
- The toggles remain as an alternative: when enabled, plain clicks edit notes (legacy behavior)

**Display (4.21.6.1)**: note bubbles are truncated to 8 characters so they never cover the map; tapping a bubble shows the full note with Edit / Delete actions (mobile has no hover). The tile-notes toggle now uses a star icon, and both toggles show tooltips on desktop.

Notes are stored in the `{saveName}_notes` file.

## Tile claim (pre-upstream, continuously maintained)

Cities can claim nearby tiles and swap tiles; the feature keeps adapting as upstream evolves.
Since 4.21.6.1 the bundled base rulesets (Vanilla / Gods & Kings) no longer enable it by default - a mod opts in via the `"Allow cities to claim tiles"` ModOptions unique (e.g. CoeHarMod).

## Auto-lock citizens button (4.21.0.1)

A new **Auto lock** button in the bottom-right of the city screen **permanently toggles** between "don't lock citizens" and "lock citizens" working-tile strategies, replacing the original manual per-turn locking.

## RNG re-rollable on load (4.21.0.1)

In single-player, loading a save no longer produces the same RNG sequence. Since 4.21.0.1 this is **disabled in multiplayer** to keep games fair.

## Export / import new game settings to clipboard (4.21.5.1)

When creating a game you can copy all current settings to the clipboard, or paste them back, making it easy to share opening configurations.

## Polling multiplayer (4.20.8.1)

Adds a **polling multiplayer** mode on top of the dynamic-turn system: the same turn is split into fixed-length time slices, and the save is passed between players slice by slice, capping the wait from "as long as the opponent takes" to a few seconds.

- After checking Online Multiplayer when creating a game, a Polling interval dropdown appears (Off / 5s / 10s / 15s / 20s / 30s)
- Online status of other players is visible (4.20.8.4)

Full mechanics: [Polling multiplayer](./Polling-multiplayer).

## Default server (4.21.0.1)

The default multiplayer server is `http://sp.unciv.cn:30123`.

## Save version isolation (4.20.17.2)

Older clients cannot load saves from newer versions, preventing save corruption from version mixing.

## MergeAction extended JSON system (4.20.7.3)

Extends `_mergeAction` (field-level merging) beyond upstream: mods can incrementally modify ruleset objects instead of replacing them wholesale.
Also adds several new Countables parameter types and Amount-compatible Countables (4.20.8.2).

Tutorial: [Merge Actions](/Modders/Mod-file-structure/6-MergeActions).

## Mod version requirements (4.21.6.1)

Mods can declare a version and compatibility requirements in `ModOptions.json`, checked with warnings only (never blocking):
- `modVersion`: `n.n.n`, defaults to `0.0.1`, shown in the mod manager
- `recommendedGameVersion`: the exact game version this mod version was made for (e.g. `4.21.7.1`); shown right in the installed mod list; a warning appears when the running game version differs
- `modDependencies`: dependency mods with recommended exact versions; a missing dependency or a version mismatch produces a warning

Warnings appear in the mod manager (warning mark + info pane), in the new-game mod selection, and in the mod checker (Options → Locate mod errors).

For players: a warning just means the mod may not work as intended on the current game version or with the currently loaded mods - the mod stays usable.
Mod authors: full reference with version format details and examples in [ModOptions.json version requirements](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#version-requirements-uncivcn).

## History: bundled UCCC mod (4.21.0.1 → reverted in 4.21.5.1)

4.21.0.1 shipped with the UCCC mod bundled in the game; since 4.21.5.1 the game no longer bundles any mods — download mods separately.
