# UncivCN Changelog

Version rule: upstream version + CN sub-version (`.1`, `.2`, `.3`…; the same upstream version can have multiple CN sub-versions, e.g. 4.20.8.1 → 4.20.8.4; restarts at `.1` after merging a new upstream, e.g. 4.21.5 → 4.21.5.1).

## v4.21.5.1 (build 1242) — current

- Merged upstream 4.21.5 (AI gold/war logic fixes, CPU performance improvements, city-state start optimizations, etc.; see upstream [changelog.md](https://github.com/AutumnPizazz/Unciv/blob/UncivCN/changelog.md))
- Reverted the bundled UCCC mod — the game no longer bundles any mods
- New: export/import new game settings to clipboard
- Adjusted the citizen auto-lock button logic
- Fixed a stats-display cascade bug, aligned with upstream master

## v4.21.0.2 (2026-08-01)

- Fixed some UCCC mod bugs

## v4.21.0.1 (2026-08-01)

- Followed recent upstream updates (incl. symmetric/mirror maps, stats panel merged from upstream)
- Since both branches made similar changes to the victory panel, reverted "disable experimental stats panel when civ-score panel is disabled" — upstream wins
- Completed missing Chinese localization
- Default multiplayer server changed to `http://sp.unciv.cn:30123`
- New citizen auto-lock shortcut button ("Auto lock" at the bottom-right of the city screen; permanently toggles unlocked/locked citizens)
- Bundled the UCCC mod
- RNG re-roll on load no longer available in multiplayer

## v4.20.17.2 (2026-07-12)

- New unit pins / map pins features (see [Features](./Features#unit-pins--map-pins-420172))
- Lifted the radius parity restriction for rotationally symmetric maps
- Disabling the civ-score panel also disables the experimental stats panel
- Save version isolation (old clients can't load new saves)
- Citizens no longer starve when training settlers and other food→production units

## v4.20.17.1 (2026-07-04)

- Followed over a month of upstream updates

## v4.20.8.4 (2026-05-26)

- The built-in mod checker can lint Lua errors in mods
- Polling multiplayer shows other players' online status

## v4.20.8.3 (2026-05-23)

- Enhanced the Lua system
- Events can be hooked with Lua

## v4.20.8.2 (2026-05-22)

- Mods support [Lua scripting](/Modders/Lua-Modding)
- Fixed a bug where the "Elite Education" policy in Gods & Kings didn't grant great people
- Amount parameters accept Countables; several new Countables parameter types
- Fixed a bug where TRY_INJECT units had their cost overwritten to 0

## v4.20.8.1 (2026-05-21)

- Major: new [polling multiplayer](./Polling-multiplayer)

## v4.20.7.4 (2026-05-19)

- Center-symmetric maps now distribute resources symmetrically too

## v4.20.7.3 (2026-05-19)

- Extended the mod JSON system; see [MergeAction tutorial](/Modders/Mod-file-structure/6-MergeActions)

---

## v4.20.7.2

**Released**: 2026-05-19

- Android builds no longer bundle the CoeHarMod mod
- New three mirrored map modes, center-symmetric in 2/3/6-fold patterns

## v4.20.6.3

**Released**: 2026-05-17

- New mod features:

| English text | Meaning |
| --- | --- |
| `Hidden from city screen` | No longer shown in the city panel |
| `Can be built [amount] times in each city` | Can be built [amount] times in a single city |

## v4.20.6.1

**Released**: 2026-05-16

- Synced two months of upstream updates
- Folded the previous random-result-variability changes into a settings toggle

## v4.19.16.2

**Released**: 2026-03-02

- Ancient-ruin random results can be re-rolled by reloading a save

## v4.19.16.1

**Released**: 2026-03-01

- City-state quest random results can be re-rolled by reloading a save

## v4.19.15-cn2

**Released**: 2026-02-28

- New mod features:

| English text | Meaning |
| --- | --- |
| `Attacks also target [mapUnitFilter] units within [positiveAmount] tiles` | Attacks also hit [mapUnitFilter] units within [positiveAmount] tiles |
| `Attacks also target [mapUnitFilter] units within [positiveAmount] tiles, with damage decreasing by distance` | Attacks also hit [mapUnitFilter] units within [positiveAmount] tiles, damage decreasing with distance |
| `Takes [relativeAmount]% damage from own area attacks` | Takes only [relativeAmount]% damage from its own area attacks |
| `Takes [relativeAmount]% counter damage from each unit hit by its area attacks` | Takes [relativeAmount]% counter damage from each unit hit by its area attacks |

- Added stats panel data export to CSV

## v4.19.15

**Released**: 2026-02-26

- Android builds bundled the [CoeHarMod](https://github.com/AutumnPizazz/CoeHarMod) mod
- Adjacent cities can swap tiles; mods must declare the unique "Allow cities to claim tiles" in their ModOptions.json
