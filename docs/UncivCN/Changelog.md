# UncivCN Changelog

Version rule: upstream version + CN sub-version (`.1`, `.2`, `.3`…; the same upstream version can have multiple CN sub-versions, e.g. 4.20.8.1 → 4.20.8.4; restarts at `.1` after merging a new upstream, e.g. 4.21.5 → 4.21.5.1).

## Unreleased

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
