# UncivCN Changelog

Version rule: upstream version + CN sub-version (`.1`, `.2`, `.3`…; the same upstream version can have multiple CN sub-versions, e.g. 4.20.8.1 → 4.20.8.4; restarts at `.1` after merging a new upstream, e.g. 4.21.5 → 4.21.5.1).

## Unreleased

- Modding: unit maintenance and max HP now show in the Civilopedia - non-default `maxHP` appears in the stats line, and in the Civilization 6 style maintenance system each unit lists its per-turn maintenance cost (`{Unit upkeep}: N {Gold}`, based on `maintenanceCost` × `unitMaintenanceBaseCost`)
- Modding: Civilization 6 style unit maintenance (opt-in via the "Uses the Civilization 6 style unit maintenance system" ModOptions unique) - per-unit fixed maintenance from the new `maintenanceCost` Units.json field, configurable `unitMaintenanceBaseCost` constant, flat per-unit Gold reductions ("Reduces unit maintenance by [amount] Gold", combinable with unit filters), no game-progress inflation and no default free units; legacy maintenance unchanged
- Modding: fix REMOVE_FIELD merge resetting scalar fields by source value type instead of field type (crashed on float fields like the new maintenanceCost)
- Modding: `[mapUnitFilter] Units` Countable 支持自定义 tag 计数（如 `[[Class.Tag] Units]`）的文档示例修正为方括号语法（`{Tag}` 花括号仅用于 AND 组合）；CoeHarMod 的全民动员补贴由 80 条逐单位重复 unique 合并为一条 Countable 表达式 + 单位 tag
- Modding: `[amount]`/`[positiveAmount]`/`[nonNegativeAmount]` parameters now fully honor their documented Countable-expression support - radii, amounts, free-unit counts, unit heal/damage/XP/status values and turn conditions previously crashed or mis-parsed on Countable expressions; `getResourceAmount` overloads unified (stockpiles first) so Countable resource references resolve consistently in city and civ contexts
- Modding: Countable docs now show how `[mapUnitFilter] Units` counts custom tag uniques (e.g. `[{Class.Tag} Units]`), letting mods merge identical per-object event uniques into one Countable expression
- Tests: testMOD fixture moved from `android/assets/mods/` to `tests/src/test/resources/testMOD` (test-only fixture, loaded via classpath with a new `TestAssets` helper that also centralizes repo-root lookup; `mods/` was already excluded from release packaging, the move keeps the dev-runtime mod list clean)
- Modding/Lua: new "Tile yield is modified by [luaFunction]" unique - Lua receives the engine-computed tile yield as `ctx.tileStats` and may return a table with new yields (stats present override engine values, absent ones keep them, nil leaves the yield unchanged); `ctx.tileStats` is registered in the Lua API docs, testMOD gains tile yield hook tests
- Modding: new trigger uniques - "upon attacking" / "upon being attacked" (attacker-only and defender-only combat hooks, covering melee, ranged and air attacks), "upon pillaging a [tileFilter] tile" (unit-level and civ-level) and "upon finishing razing a city"
- Modding: new conditionals - "if unit is fortified", "if unit is embarked" and "if this city is being razed" - usable on any unique
- Modding: new reverse uniques - "Lose control over [positiveAmount] tiles [cityFilter]" (city centers never lost, most recently acquired lost first), "Lose a spy" (prefers an idle spy), "End a golden age" (fires the golden-age-end triggers like a natural end), and "Hide up to [positiveAmount/'all'] [tileFilter] within a [positiveAmount] tile radius" (un-explores tiles; tiles in unit/city sight range become visible again on the next update)
- Modding: new unique "Lose control over [tileFilter] tiles in a [nonNegativeAmount]-tile radius" - tiles owned by the triggering civilization within the radius become unowned again; city centers are never affected
- Modding: the mod checker now whitelists `.luarc.json` in the mod root folder (LuaLS editor config - LuaLS only reads it from the workspace root); docs tell modders to keep this exact file name
- Modding: `modDependencies` in ModOptions.json now use `recommendedVersion` (exact match, recommended wording) instead of the `version` range; the `ModVersionRange` helper class is removed
- Modding: new `recommendedGameVersion` field in ModOptions.json - a mod version can declare the exact game version it was made for (exact match, `n.n.n`/`n.n.n.n`/`-patchN`); the installed mod list and the info pane show it right away (e.g. `Version 0.1.0 · game 4.21.7.1`), and a non-blocking warning appears when the running game version differs; the `gameVersionRange` field is removed
- Modding/Lua: more combat support - new read-only combat prediction queries on units (getAttackingStrengthAgainst / getDefendingStrengthAgainst / predictDamageTo / predictDamageFrom) and new trigger uniques (upon bombarding / upon being bombarded / upon withdrawing from melee combat)
- Modding/Lua: Lua can now take over the combat strength and damage formulas - new uniques "Combat strength is modified by [luaFunction]", "Combat damage dealt is modified by [luaFunction]" and "Combat damage received is modified by [luaFunction]"; Lua receives the raw combat inputs (base strength + modifier factor + individual modifier list for strength; attacker/defender strength + randomness factor + health ratio + damage direction for damage) and returning nil falls back to the engine formula
- Modding/Lua: combat-fired Lua triggers can now see the opponent - `ctx.otherCiv` (diplomacy & combat), `ctx.attacker`/`ctx.defender`/`ctx.target` and `ctx.combatAction` are exposed to `TriggerLuaFunction`/`ConditionalLuaCheck`
- Modding/Lua: new unit trigger uniques: upon capturing a unit / upon being captured / upon intercepting a unit / upon being intercepted
- Docs: the Units.json field reference now documents the `maxHP` field (default 100, adjustable via the `[relativeAmount] Max HP` unique); the city strength formula in Miscellaneous JSON files now uses the garrisoned unit's HP percentage instead of assuming a max HP of 100
- CI: trimmed the workflows triggered by daily branch pushes - the Docker image publish workflow no longer runs on every push (kept the daily schedule, release tags, PR build check and manual dispatch), conflict marking no longer re-scans on branch pushes (still runs for PRs), and pure docs-only changes skip the code test workflows (Build and test / Detekt) since the docs workflow recompiles via generateDocs

## 4.21.8.1 (build 1253)

- Merged upstream 4.21.8 (17 commits): the View refactor continues (#15280) - fog-of-war view usage unified (`getGameViewConsideringForOfWar`), Empire overview tabs migrated to Views, trade UI view-ified in two steps, CityScreen migrated to `TileView`; new non-vanilla ranking type Tiles Explored (enabled by the `Show additional stat types` new-game option) and spectator slot for max players (Max players with spectator); crash-screen OOM, tech picker and notification overview ANRs, rivers on water near Rock of Gibraltar, and dev-console resource filter visibility fixed; docs reorganized (`Simulations` moved to Developers and renamed `Testing AI changes`, Regions merged into the map JSON doc, trailer audio credits merged into Credits)
- Conflict resolution on merge: CN version bumped to 4.21.8.1 (build 1253); Simplified Chinese translations added for Show additional stat types and Tiles Explored; EN/ZH docs followed the upstream reorganization (standalone Other/Regions, Other/Simulations and Credits_trailer pages removed, VitePress sidebar updated); lua-api.lua / lua-map-api.lua regenerated via generateDocs (version header sync)
- Difficulty: AI unhappiness modifier for King and above is now 100/90/85/75 (was 90/85/75/60), matching verified in-game values
- Modding: unit max HP is now moddable - `maxHP` field in Units.json (default 100) plus the new `[relativeAmount] Max HP` unique; combat wounded penalty now scales with HP percentage, AI/UI thresholds made relative, Lua `unit.getMaxHealth()` reflects the moddable max HP
- Main menu bottom-right buttons: the Discord entry is now a QQ group link (qm.qq.com), added a Baidu Tieba button, and the GitHub button points to the CN fork repository; the About page's repository/changelog/README links point to the UncivCN fork (with version anchors fixed)
- Docs site links adapt to the client language: Simplified/Traditional Chinese clients jump to the Chinese section (/zh/), other languages jump to the English section
- CI: detekt / Docker release workflows now trigger on the UncivCN branch (previously bound to upstream master and never ran), and release detection only accepts 4-segment version numbers; removed the upstream uncivbot auto-release bot (CN releases manually)
- In-game wiki links now point to the CN docs site (club.unciv.cn), and the load-failure notice email is now hurxwork@qq.com
- Code cleanup & bugfixes from the full-project code review: dead code and commented-out blocks removed, `ConditionalBuildingBuiltAll` city filters and multi-segment `{A} {B}`/`non-[X]` filters fixed, `LongPriorityQueue.remove` no longer deletes the wrong element, `stateBasedRandom` no longer crashes headless map-generation tests
- CI: fixed detekt analysis failures - removed deprecated config properties (`OptionalWhenBraces`; `ForbiddenComment` `values`/`customMessage` now `comments`) and added the missing end-of-file newline in `UnitPresenter.kt`

## 4.21.7.2 (build 1252)

- Android: update popup downloads the APK in-game with progress and installs it via FileProvider (unknown-sources permission guided)
- Android: downloads are interruption-safe (`.part` + atomic rename), Install/Redownload buttons, copy to the public Downloads folder, old packages cleaned up
- Main menu "Download latest version" now lists the platform's installers from the release assets and downloads the chosen one directly

## 4.21.7.1 (build 1251)

- Merged upstream 4.21.7 / 4.21.7-patch1 / patch2 (41 commits): View refactor continues (#15280), AI war-logic fixes, Stealth Bomber Evasion, MP average turn times, minimap ANR fix, Gradle 9.4.1 + LibGDX 1.14.2 + target SDK 36; CN notes/pins and auto-lock adapted to the new View APIs
- CI: fixed `unciv-lua-api` vsix packaging
- Modder tooling: Lua API definitions auto-deployed to the docs site; `unciv-lua-api` VSCode extension syncs them into LuaLS
- Docs: Lua API and map-script references generated from Kotlin data tables; toolchain no longer needs Python
- Lua API: added `unit.getEraNumber()` and `civ.discoverTech()`
- Mods: CoeHarMod now a git submodule (standalone repo), rules consolidated via Lua

## 4.21.6.6 (build 1250)

- Automatic update check on the main menu (mirror fallback); "Mod download source" renamed "Download source"
- Fixed Lua runtime crash on function-call arguments in custom API functions
- Lua API expansion (~100 new methods), `ConditionalLuaCheck` unique, `TriggerUponTradeMade` hook
- Lua map scripts: mods can ship a map generator (`scripts/`), new "Lua Generated" map type
- Lua sandbox hardening; mod checker covers map-script APIs

## 4.21.6.5 (build 1249)

- Docs: Lua sections rewritten, EmmyLua type definitions + `LuaStarterMod` template, mod-ci CLI
- Lua runtime errors show line numbers; sandbox escape via `package.loaded` closed; instruction budget against runaway loops
- New: multiplayer restart votes (host enables, everyone votes, timeout = agree, auto-restart)
- CI: Release title is the plain version number

## 4.21.6.4 (build 1248)

- CI: Release notes now contain the CN changelog in EN + ZH
- New: "Mod download source" setting with China mirror support
- New-game screen: save/load setups to named slots; clipboard buttons moved; "Reset to defaults" replaced by a built-in "Default setup"
- Docs: release checklist & lessons learned added to Coding-standards

## 4.21.6.3 (build 1247)

- Fixed Docker build copying stale `Unciv.jar` name; fixed MSI missing from releases

## 4.21.6.2 (build 1246)

- Fixed 47 missing + 10 empty Simplified Chinese translations for CN-specific UI strings

## 4.21.6.1 (build 1245)

- Notes keyed by gameId (no more leaking across saves)
- Map/unit pins: long-press / Alt+click editing, note bubbles, crash fix, CN translations
- ModOptions.json: `modVersion` / `gameVersionRange` / `modDependencies`
- Fixed duplicate size rows on the new-game screen; aligned bundled rulesets with upstream
- Merged upstream 4.21.6 (CPU perf, AI worker adjacency, MP upload indicator)
- Docs site fixes (~250 anchors), in-game version auto-synced from BuildConfig.kt

## 4.21.5.3 (build 1244)

- Fixed CI test failures (translation template spaces, test mods tracked in repo)
- Deploy no longer posts to Discord

## 4.21.5.2 (build 1243)

- Official VitePress docs site (Chinese full-text search, EN/CN switch)
- `docDescriptionZh`: unique docs in Chinese
- Local APK signing, UncivCN artifact names

## 4.21.5.1 (build 1242)

- Merged upstream 4.21.5 (AI war-logic fixes, CPU perf)
- Unbundled the UCCC mod; new: export/import game setup to clipboard; citizen auto-lock tweaks; stats-display bug fixed

## 4.21.0.2 (2026-08-01)

- Fixed UCCC mod bugs

## 4.21.0.1 (2026-08-01)

- Synced recent upstream (mirror maps, stats panel); completed Chinese localization
- Default MP server `sp.unciv.cn:30123`; citizen auto-lock button; bundled UCCC mod
- RNG re-roll on load disabled in multiplayer

## 4.20.17.2 (2026-07-12)

- New: unit/map pins; radius parity restriction lifted; save version isolation; no more starvation when training settlers

## 4.20.17.1 (2026-07-04)

- Followed a month of upstream updates

## 4.20.8.4 (2026-05-26)

- Mod checker lints Lua errors; polling MP shows online status

## 4.20.8.3 (2026-05-23)

- Lua system enhanced; events hookable with Lua

## 4.20.8.2 (2026-05-22)

- Mods support [Lua scripting](/Modders/Lua-Modding)
- Fixed G&K "Elite Education" not granting great people; TRY_INJECT cost bug
- Amount parameters accept Countables

## 4.20.8.1 (2026-05-21)

- New: [polling multiplayer](./Polling-multiplayer)

## 4.20.7.4 (2026-05-19)

- Center-symmetric maps distribute resources symmetrically

## 4.20.7.3 (2026-05-19)

- Extended mod JSON system, see [MergeAction tutorial](/Modders/Mod-file-structure/6-MergeActions)

## 4.20.7.2

**Released**: 2026-05-19

- Android builds no longer bundle CoeHarMod
- New: three mirrored map modes (2/3/6-fold center symmetry)

## 4.20.6.3

**Released**: 2026-05-17

- New mod uniques:

| English text | Meaning |
| --- | --- |
| `Hidden from city screen` | No longer shown in the city panel |
| `Can be built [amount] times in each city` | Can be built [amount] times in a single city |

## 4.20.6.1

**Released**: 2026-05-16

- Synced two months of upstream updates
- Random-result variability folded into a settings toggle

## 4.19.16.2

**Released**: 2026-03-02

- Ancient-ruin results re-rollable by reloading a save

## 4.19.16.1

**Released**: 2026-03-01

- City-state quest results re-rollable by reloading a save

## 4.19.15-cn2

**Released**: 2026-02-28

- New area-attack mod uniques (multi-target, distance falloff, self/counter damage)
- Stats panel CSV export

## 4.19.15

**Released**: 2026-02-26

- Android bundles [CoeHarMod](https://github.com/AutumnPizazz/CoeHarMod)
- Adjacent cities can swap tiles (opt-in via ModOptions unique)
