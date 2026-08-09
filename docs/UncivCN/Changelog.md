# UncivCN Changelog

Version rule: upstream version + CN sub-version (`.1`, `.2`, `.3`…; the same upstream version can have multiple CN sub-versions, e.g. 4.20.8.1 → 4.20.8.4; restarts at `.1` after merging a new upstream, e.g. 4.21.5 → 4.21.5.1).

## Unreleased

- Fixed crash when opening the map pin editor on a tile: the tile texture preview used TileMapView sized by tile count, which overflowed for real game tiles with a non-zero index (now sized by highest index too, with regression tests)
- Map pin editor popup: removed the tile coordinates from the title and replaced the placeholder icon with a live preview of the selected tile's texture set (terrain/resources/improvements/rivers)
- Aligned bundled base rulesets (Vanilla / Gods & Kings) with upstream: removed the CN-only "Allow cities to claim tiles" from the G&K ModOptions (tile-claim is now opt-in via mod ModOptions) and the CN-only Great General 8-turn Golden Age unique (upstream #13308)
- Fixed new-game screen: setting world size to Custom and toggling symmetry repeatedly stacked duplicate radius/width/height input rows (the hexagonal/rectangular size tables were rebuilt without clearing)
- New: mod version requirements in `ModOptions.json` — `modVersion` (n.n.n, default 0.0.1), `gameVersionRange` (min~max, empty = all versions) and `modDependencies` (exact or ranged version requirements); unsatisfied requirements show warnings in the mod manager, new-game mod selection and mod checker, never blocking
- Fixed dead links across the docs site: 14 broken `](`-corrupted links in the Chinese UncivCN pages, plus ~250 wrong heading anchors (VitePress slug format) in the Modders docs (both EN and ZH, incl. the doc generators `UniqueType.kt` / `Countables.kt` / `UniqueDocsWriter` / `MergeActionDocsWriter`); verified against a fresh VitePress build
- Merged upstream 4.21.6: CPU performance improvements (city baseline computed once, ~20% faster next-turn in some saves), AI workers consider future adjacencies for improvements, visually indicate failed MP upload, nation-colored chat names, OneTimeGainStat re-parameterized to `[civWideStat]` with a modding warning, test runner overhaul (see upstream [changelog.md](https://github.com/AutumnPizazz/Unciv/blob/UncivCN/changelog.md))
- Fixed the docs site showing unstyled text under the custom domain: site artifacts are now deployed to the `Unciv/` subdirectory with a root redirect page
- Fixed duplicate `/Unciv` prefix in the navbar logo path
- In-game version display is now synced automatically from `BuildConfig.kt` to `UncivGame.kt` at build time (previously a manual release could forget this and ship a stale in-game version)
- Slimmed down `AGENTS.md` to behavior rules only; development reference (build commands, project structure, game state model, assets) moved into Coding-standards, which is now the engineering handbook (bilingual)
- Added upstream-merge convention: resolve merge conflicts with upstream master yourself and fully integrate its features; when upstream and CN implement the same feature differently, ask the user which implementation to adopt

## v4.21.5.3 (build 1244)

- Fixed CI test failures: 13 translation templates were missing the trailing space (breaking `TranslationTests`); test mods `testMOD` / `testMapScript` are now tracked in the repo (previously git-ignored, which broke Lua-script and merge-action tests in CI), with `testMOD/jsons/Buildings.json` added
- Deploy workflow no longer posts release announcements to Discord (GitHub Release upload kept)

## v4.21.5.2 (build 1243)

- Official docs site launched: a brand-new VitePress site (replacing mkdocs) with full-text Chinese search, one-click EN/CN switching, and one-click copy on unique listings
  - The English section fully reuses upstream docs; the Chinese section is a complete translation mirror (Modders / Developers / Translating / Other and all other pages)
  - New UncivCN section (about / features / changelog / differences / coding standards / polling multiplayer) and community section (guides / mods / code analysis / upstream changelog), all bilingual
- Unique documentation now in Chinese: new `docDescriptionZh` mechanism — modders can read Chinese explanations of uniques on the docs site
- Build & release pipeline adapted: local APK signing (zipalign + apksigner V3), artifacts named UncivCN; new signing regression CI

## v4.21.5.1 (build 1242)


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

- New unit pins / map pins features (see [Features](./Features#unit-pins-map-pins-4-20-17-2))
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
