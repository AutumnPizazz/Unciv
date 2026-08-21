# UncivCN Changelog

Version rule: upstream version + CN sub-version (`.1`, `.2`, `.3`…; the same upstream version can have multiple CN sub-versions, e.g. 4.20.8.1 → 4.20.8.4; restarts at `.1` after merging a new upstream, e.g. 4.21.5 → 4.21.5.1).

::: tip
Upstream (vanilla) release notes: [official Unciv changelog](https://github.com/yairm210/Unciv/blob/master/changelog.md)
:::

## Unreleased

- Modding: Variables.json variables now support city, civilization and global scopes, bounds, per-turn yields, percentage bonuses, purchase costs, production conversion, scope-specific uniques, Lua read/write APIs and scope-aware UI; existing civ variables remain available with an explicit `scope` declaration - see [Variables.json](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json) and [uniques](/Modders/uniques)

- Fix: Scoped Variables.json values now persist through game-state copies, enforce ownership and scope, honor city filters and bounds, charge exact variable purchase costs, and avoid AI/stat-parser crashes.

- Modding: variables (Variables.json) are now accepted in every countable position (e.g. `when number of [X] is more than [Y]`) and in the `Set [X] to [countable]` triggerable, so mods can move more resource-faked counters to variables - see [Variables.json](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json); CoeHarMod serves as a reference implementation

- Merged upstream 7 commits: View refactoring continues (#15280) - unified View equality comparison, foreign-civ view access tightened (`getCiv()`), `viewingCiv` made private (a spectator posing as a player is now consistently treated as that player); fixed repair when only a pillaged road remains and an ANR when opening the Civilopedia; CN features (map pins/unit notes visibility, multiplayer chat online status, restart vote) migrated to the equivalent new API

- Docs: docs site restructured - the English-side CoeHarMod changelog moved to [/Community/Mods/CoeHarMod/changelog/](/Community/Mods/CoeHarMod/changelog/) (roadmap at [/Community/Mods/CoeHarMod/changelog/roadmap](/Community/Mods/CoeHarMod/changelog/roadmap)), community images on the English side use English filenames, and Chinese pages now reference the English-side images instead of storing duplicates

- Docs: the docs site no longer mirrors the upstream changelog - upstream release notes are available via the [official Unciv changelog](https://github.com/yairm210/Unciv/blob/master/changelog.md) linked above

## 4.21.10.2 (build 1257)

- Update: update checks and installer downloads are now routed by player region - players in mainland China (a one-time dialog asks on first launch, changeable in Options - Advanced) use the official CN download server (`server-ts` installer hosting, same domain as the multiplayer server), which works even when github.com is unreachable; the server hosts only the latest version (old ones cleaned up automatically) with rate limiting and concurrency protection, and pulls installers from GitHub through a mirror (gh-proxy) itself after each release. Everyone else follows their download source setting as before. Mod downloads are unaffected - see [server-ts/README.md](https://github.com/AutumnPizazz/Unciv/blob/UncivCN/server-ts/README.md)

- Merged upstream 4.21.10-patch1/patch2 (9 commits): fixed crashes when a spectator selects a foreign city or unit, when a city is razed mid-click, and when activating two city screen arrows at once; cleaner "unit(s) can promote" notification (single notification when many units are promotable); timers report when the app is paused

## 4.21.10.1 (build 1256)

- Modding: new mod-defined global variables (Variables.json) - per-civilization integer counters (e.g. war weariness) that work with `when above/below/between` conditionals, `Instantly provides/consumes/gain` triggerables and Lua (`civ.getVariable/setVariable/addVariable`, `game.getRulesetVariables/doesVariableExist`); display in the top bar and Resources overview is configurable per variable and via ModOptions (`variableMenuThreshold`, `alwaysDisplayVariableCount`) - see [Variables.json](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json)
- Modding: conditional amounts (e.g. `when above [amount] ...`, HP/movement/population thresholds) now accept Countable expressions like `[Cities]` - see [Unique parameters](/Modders/Unique-parameters#countable)

- Merged upstream 4.21.10 (46 commits): View refactor continues (tile/unit view layer, spectator fixes); promotion notifications squashed when many units are promoted; legion can no longer repair; improvement picker no longer hides researchable improvements; victory screen civ list keeps its scroll position; charts use live data; spectators hidden in global politics; defeated singleplayer players get full map visibility; WLTKD demand rewrite mid-celebration fixed; spaceship parts no longer sold for resources; puppeting cities via console; several Android and crash fixes; Kotlin upgraded to 2.4.10
- Fix: map pin (tile note) previews no longer reveal strategic resources before their revealing tech is researched (e.g. no Oil in the Ancient era)
- Map generation with rotational symmetry rebuilt: symmetry is now applied during generation instead of as a post-process - fixes broken 3/6-fold symmetry on wrapped maps, mirror-continent inconsistencies, and resource/wonder copy glitches; generation performance unchanged - see [Features](/UncivCN/Features)

## 4.21.8.3 (build 1255)

- Multiplayer: new game option "Forbid reloading" - quitting and re-entering the game can no longer return to the turn-start state to redo moves (you resume from your latest turn state instead)
- Modding: new ModOptions unique "Production overflow applies immediately to the next construction" - uncapped overflow flows into the next build on the same turn, and units finished by immediate overflow can move right away; vanilla behavior unchanged without the unique - see [uniques](/Modders/uniques)

## 4.21.8.2 (build 1254)

- Fix: map pin (tile note) previews no longer render unrevealed resources ahead of time (e.g. no oil in the Ancient era)
- Modding: Civilization 6 style unit maintenance (opt-in via a ModOptions unique) - fixed per-unit upkeep, flat Gold reductions, no inflation over game progress, legacy system unchanged; maintenance and max HP now also show in the Civilopedia - see [Units.json](/Modders/Mod-file-structure/4-Unit-related-JSON-files) and [uniques](/Modders/uniques)
- Modding: [amount]-type parameters now fully support Countable expressions (radii, amounts, free units, heal/damage/XP values, turn conditions) - see [Unique parameters](/Modders/Unique-parameters)
- Modding: new trigger, conditional and reverse uniques (combat / pillage / raze triggers, fortified / embarked conditionals, lose control over tiles / lose a spy / end a golden age / hide explored tiles) - see [uniques](/Modders/uniques)
- Modding/Lua: Lua can modify tile yields, take over combat strength and damage formulas, and react to combat / capture events with full attacker-defender context - see [Lua Modding](/Modders/Lua-Modding)
- Modding: new ModOptions.json version fields (`recommendedGameVersion`, `recommendedVersion` dependencies) with a non-blocking warning when the game version differs - see [ModOptions.json](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files)
- Modding: merge (REMOVE_FIELD) crash on float fields fixed - see [MergeActions](/Modders/Mod-file-structure/6-MergeActions)

## 4.21.8.1 (build 1253)

- Merged upstream 4.21.8 (17 commits): View refactor continues, new "Tiles Explored" ranking type (optional), spectator slot for max players, crash-screen OOM and several ANR fixes, rivers on water near Rock of Gibraltar fixed
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
