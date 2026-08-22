# CoeHarMod Changelog

## 📌 Version history

::: tip Latest version
**v3.3.11** - adapted for Unciv 4.21.8.2
:::

## Unreleased

- Declared all Variables.json entries as civilization-scope, gameplay unchanged
- Migrated 8 resource-faked counters (Governor Titles, Diplomatic Favor, Influence, Monk count, etc.) to official variables (see [Variables.json](/Modders/Mod-file-structure/5-Miscellaneous-JSON-files#variables-json))
- Top-bar variable display is now configurable per variable
- Adapted recommended game version to 4.21.10.2

## v3.3.12 - for 4.21.8.2

- Enabled immediate production overflow: applies on the same turn, finished units can move instantly
- Cleaned up all green mod-checker entries (166 unused tag declarations removed)
- Fixed all 38 mod-checker warnings

## v3.3.11 - for 4.21.8.2

### ⚖️ Maintenance system rework

- Migrated to Civ6-style unit maintenance: fixed upkeep, no inflation
- Cleaned up redundant Comments and orphan translations left over from the migration

### 📜 National mobilization optimization

- National mobilization subsidy: 80 per-unit repeated uniques merged into one Countable expression + a unit tag, greatly simplifying the ruleset

### 🤖 Lua system rework

- General/admiral auras and era-based AI civics now run on Lua
- Resource data converted to tables; Lua functions renamed to PascalCase, eliminating LuaLS diagnostics

### 🧹 Cleanup & tooling

- Removed commented-out old rule definitions, 1700+ lines in total
- GitHub Action no longer publishes Releases: any tag now syncs core files to main
- Removed the outdated project-memory doc and the python helper tools; gitignore now ignores LuaLS local files

## v3.3.10 - for 4.19.15

- Monks can now appear in the Civilopedia
- Fixed the monk cap behaving abnormally
- Fixed the Oracle's faith-buy discount actually increasing costs
- Nerfed Byzantine heavy cavalry happiness: linear growth changed to square-root growth

## v3.3.9 - for 4.19.12

### 🏛️ Building adjustments

- **Normal building price scaling removed**: deleted the price-increase effect of all normal buildings
- **District landmark scaling doubled**: district landmark buildings' price increase doubled

### 📜 Opening experience

- **Policy card unlock**: new games auto-open all policy cards, simplifying the flow
- **Era restriction**: games can only start in the Ancient era, avoiding bugs from later-era starts
- **Governor point reminder**: event reminder when surplus governor points are unused

### ⚖️ Happiness system

- Adjusted the happiness bonus: every point of per-city average happiness gives all cities +2% of all yields (science, culture, faith, production, gold; not food)
- Population growth limit: no growth when average happiness is below -4 (≤ -5)
- Rebel trigger: may spawn rebels when average happiness is below -9 (≤ -10)
- Fixed negative values in the happiness system not working

### 🔧 Dev tooling

- Improved the GitHub Actions workflow so main only contains core files
- Added a core-file definition module (whitelist mechanism)
- Added automatic Release publishing and a local packaging tool
- Updated release.yml
- Migrated the docs project, with math formula support

## v3.3.8 - for 4.19.11

### 🎨 Docs site restructure

- [x] Reorganized the docs directory into original-game section, CoeHarMod section, and other-mods section

## v3.3.7 - for 4.19.11

*(Buildings, units and balance adjustments — see the [Chinese changelog](/zh/Community/Mods/CoeHarMod/更新日志/) for details.)*

## v3.3.6 - for 4.19.10

- *(Balance and bug-fix batch — details in the [Chinese changelog](/zh/Community/Mods/CoeHarMod/更新日志/).)*

## v3.3.5 - for 4.19.9

- *(Balance and bug-fix batch — details in the [Chinese changelog](/zh/Community/Mods/CoeHarMod/更新日志/).)*

## v3.3.4 - for 4.19.9

- *(Balance and bug-fix batch — details in the [Chinese changelog](/zh/Community/Mods/CoeHarMod/更新日志/).)*

## v3.3.3 - for 4.19.9

- *(Balance and bug-fix batch — details in the [Chinese changelog](/zh/Community/Mods/CoeHarMod/更新日志/).)*

## v3.3.2 - for 4.19.9

- *(Balance and bug-fix batch — details in the [Chinese changelog](/zh/Community/Mods/CoeHarMod/更新日志/).)*

## v3.3.1 - for 4.19.9

- *(Major update: new content and rebalance — details in the [Chinese changelog](/zh/Community/Mods/CoeHarMod/更新日志/).)*

## v3.3.0 - for 4.19.9

- *(Major update — details in the [Chinese changelog](/zh/Community/Mods/CoeHarMod/更新日志/).)*

## v3.2.x series (for 4.19.8/4.19.9)

- *(Balance and bug-fix batches — details in the [Chinese changelog](/zh/Community/Mods/CoeHarMod/更新日志/).)*
