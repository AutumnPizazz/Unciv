# UncivCN Changelog

Version rule: upstream version + CN sub-version (`.1`, `.2`, `.3`…; the same upstream version can have multiple CN sub-versions, e.g. 4.20.8.1 → 4.20.8.4; restarts at `.1` after merging a new upstream, e.g. 4.21.5 → 4.21.5.1).

::: tip
Upstream (vanilla) release notes: [official Unciv changelog](https://github.com/yairm210/Unciv/blob/master/changelog.md)
:::

## Unreleased

- Fix: in-game update checks now verify CN download server synchronization
- Multiplayer: simultaneous turns now advance after all players submit
- Multiplayer: simultaneous-turn submissions no longer overwrite each other

## 4.21.10.3 (build 1258)

- Multiplayer: added the experimental simultaneous-turn operation protocol
- Multiplayer: simultaneous-turn operation files are deduplicated by player and sequence
- Multiplayer: simultaneous turns now replay unit orders without rerolling combat
- Multiplayer: fortify, sleep and skip orders are included in simultaneous turns
- Multiplayer: simultaneous-turn settlement now uses an atomic server lock
- Multiplayer: expanded simultaneous-turn snapshots to diplomacy popups, events and special unit UI
- Multiplayer: automated, Lua and triggered unit effects are replayed from result snapshots
- Multiplayer: simultaneous turns now record city production, technology and policy choices
- Multiplayer: complex unit actions now replay modded state changes
- Modding: unit-scope variables - attach integer counters to individual units (see [Variables.json](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json))
- Modding: unit experience is now a variable, old saves migrate automatically (see [Variables.json](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json))
- Modding: variables support city, civilization and global scopes (see [Variables.json](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json))
- Modding: variables support bounds, per-turn yields, percentage bonuses, purchase costs, production conversion (see [Variables.json](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json))
- Modding: variables support Lua read/write and scope-aware UI (see [Variables.json](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json))
- Modding: civilization-scope variables now need an explicit scope declaration
- Fix: variable purchases charge exact cost, values survive reloads, no AI crashes
- Modding: variables work in all countable positions and Set triggerables (see [Variables.json](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json))
- Merged upstream: fixed repair with only a pillaged road left, and an ANR when opening the Civilopedia
- Merged upstream: CN features adapted to the new architecture, behavior unchanged
- Docs: CoeHarMod changelog moved to the mods section (see [changelog](/Community/Mods/CoeHarMod/changelog/))
- Docs: upstream changelog replaced by a link at the top of this page
- Docs: changelog entries limited to 30 words; big changes may be split into multiple entries

## 4.21.10.2 (build 1257)

- Update: updates are now routed by region - mainland China uses the official CN server (see [server-ts/README.md](https://github.com/blyrin/unciv-srv/blob/main/README.md))
- Update: first launch asks your region (changeable in Options - Advanced)
- Merged upstream: fixed several spectator and city-screen crashes

## 4.21.10.1 (build 1256)

- Modding: new civilization-scope variables with conditionals, triggers and Lua (see [Variables.json](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json))
- Modding: variable display (top bar, resources overview) is configurable (see [Variables.json](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json))
- Modding: conditional amounts (e.g. `when above [amount] ...`, HP/movement/population thresholds) now accept Countable expressions like `[Cities]` - see [Unique parameters](/Modders/Unique-parameters#countable)
- Merged upstream: UI and crash fixes, squashed promotion notifications
- Fix: rotational-symmetry map generation glitches (wrapped/mirror maps) (see [Features](/UncivCN/Features))

## 4.21.8.3 (build 1255)

- Multiplayer: new "Forbid reloading" option - no redoing turns by re-entering
- Modding: new immediate production overflow unique (see [uniques](/Modders/uniques))

## 4.21.8.2 (build 1254)

- Fix: map pin (tile note) previews no longer render unrevealed resources ahead of time (e.g. no oil in the Ancient era)
- Modding: Civ6-style unit maintenance (opt-in), upkeep shown in Civilopedia (see [Units.json](/Modders/Mod-file-structure/4-Unit-related-JSON-files) and [uniques](/Modders/uniques))
- Modding: [amount]-type parameters now fully support Countable expressions (radii, amounts, free units, heal/damage/XP values, turn conditions) - see [Unique parameters](/Modders/Unique-parameters)
- Modding: new trigger, conditional and reverse uniques (combat / pillage / raze triggers, fortified / embarked conditionals, lose control over tiles / lose a spy / end a golden age / hide explored tiles) - see [uniques](/Modders/uniques)
- Modding/Lua: Lua can modify tile yields, take over combat strength and damage formulas, and react to combat / capture events with full attacker-defender context - see [Lua Modding](/Modders/Lua-Modding)
- Modding: new ModOptions.json version fields (`recommendedGameVersion`, `recommendedVersion` dependencies) with a non-blocking warning when the game version differs - see [ModOptions.json](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files)
- Modding: merge (REMOVE_FIELD) crash on float fields fixed - see [MergeActions](/Modders/Mod-file-structure/6-MergeActions)

## 4.21.8.1 (build 1253)

- Merged upstream: crash fixes, new optional "Tiles Explored" ranking
- Difficulty: AI unhappiness modifier for King and above is now 100/90/85/75 (was 90/85/75/60)
- Modding: unit max HP is now moddable - `maxHP` field plus the new "[relativeAmount] Max HP" unique; wounded penalty scales with HP percentage - see [Units.json](/Modders/Mod-file-structure/4-Unit-related-JSON-files)
- Main menu bottom-right buttons: Discord entry replaced by a QQ group link, Baidu Tieba button added, GitHub button points to the CN fork; About page links updated
- Docs site links adapt to the client language (Chinese clients get the /zh/ section)
- In-game wiki links point to the CN docs site
- Code cleanup & bugfixes from a full-project review (dead code removed, city/filter bugs fixed)

## 4.21.7.2 (build 1252)

- Android: update popup downloads the APK in-game with progress and installs it (unknown-sources permission guided)
- Android: downloads are interruption-safe, Install/Redownload buttons, copy to the public Downloads folder, old packages cleaned up
- Main menu "Download latest version" lists the platform's installers and downloads the chosen one directly

## 4.21.7.1 (build 1251)

- Merged upstream 4.21.7 / 4.21.7-patch1 / patch2 (41 commits): AI war-logic fixes, Stealth Bomber Evasion, MP average turn times, minimap ANR fix, Gradle 9.4.1 + LibGDX 1.14.2
- Lua API: added unit.getEraNumber() and civ.discoverTech() - see [Lua API Reference](/Modders/Lua-API-Reference)
- Mods: CoeHarMod moved to its own repo as a git submodule, rules consolidated via Lua

## 4.21.6.6 (build 1250)

- Automatic update check on the main menu (mirror fallback); "Mod download source" renamed "Download source"
- Fixed Lua runtime crash on function-call arguments in custom API functions
- Lua API expansion (~100 new methods), ConditionalLuaCheck unique, TriggerUponTradeMade hook - see [Lua Modding](/Modders/Lua-Modding)
- Lua map scripts: mods can ship a map generator (`scripts/`), new "Lua Generated" map type
- Lua sandbox hardening; mod checker covers map-script APIs

## 4.21.6.5 (build 1249)

- Lua runtime errors now show line numbers; sandbox escape via package.loaded closed; instruction budget against runaway loops
- New: multiplayer restart votes (host enables, everyone votes, timeout = agree, auto-restart)

## 4.21.6.4 (build 1248)

- New: "Mod download source" setting with China mirror support
- New-game screen: save/load setups to named slots; clipboard buttons moved; "Reset to defaults" replaced by a built-in "Default setup"

## 4.21.6.3 (build 1247)

- Fixed Docker build copying stale Unciv.jar name; fixed MSI missing from releases

## 4.21.6.2 (build 1246)

- Fixed 47 missing + 10 empty Simplified Chinese translations for CN-specific UI strings

## 4.21.6.1 (build 1245)

- Notes keyed by gameId (no more leaking across saves)
- Map/unit pins: long-press / Alt+click editing, note bubbles, crash fix, CN translations
- Fixed duplicate size rows on the new-game screen; aligned bundled rulesets with upstream
- Merged upstream 4.21.6 (CPU perf, AI worker adjacency, MP upload indicator)

## 4.21.5.3 (build 1244)

- Fixed CI test failures (translation template spaces, test mods tracked in repo); deployment notifications cleaned up

## 4.21.5.2 (build 1243)

- Official VitePress docs site (Chinese full-text search, EN/CN switch)
- Unique documentation descriptions now available in Chinese
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
