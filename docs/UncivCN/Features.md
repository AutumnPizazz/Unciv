# UncivCN Features

This page describes the features the UncivCN branch adds over upstream. Version in brackets = when the feature was introduced.

## Lua scripting (since 4.20.8.2, continuously extended)

Mods can attach Lua scripts for logic far beyond plain JSON:

- **Triggers**: via Unique syntax (e.g. `LuaEvent`) or game lifecycle hooks
- **ctx context object**: scripts can access `civ`, `city`, `unit`, `tile`, `game` APIs
- **Tooling**: the built-in mod checker can lint mod Lua errors (4.20.8.4); convenience APIs like `game.findTiles` (after 4.20.8.2)

Full tutorial: [Lua modding](/Modders/Lua-Modding).

## Mirrored and ring maps (since 4.20.7.3 / 4.20.7.4 / 4.21.0)

- **Three mirrored map modes**: selectable when creating a game; resources are also mirrored symmetrically (4.20.7.4)
- **Ring maps**: the map wraps around; the radius parity restriction for rotationally symmetric maps is lifted (4.20.17.2)

## Unit pins / map pins (4.20.17.2)

Two independent toggles on the world screen, left of the minimap in the bottom-right:

- **Unit pin**: when enabled, click any unit (including foreign units) to attach a note, shown right below the unit's figure; click again to edit or delete; a picker appears when multiple units share a tile
- **Map pin**: click any tile (including empty ones) to attach a note, shown on the tile with priority over food/production/gold icons

Notes are stored in the `{saveName}_notes` file.

## Tile claim (pre-upstream, continuously maintained)

Cities can claim nearby tiles and swap tiles; the feature keeps adapting as upstream evolves.

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

## History: bundled UCCC mod (4.21.0.1 → reverted in 4.21.5.1)

4.21.0.1 shipped with the UCCC mod bundled in the game; since 4.21.5.1 the game no longer bundles any mods — download mods separately.
