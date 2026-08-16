# CoeHarMod Changelog

## 📌 Version history

::: tip Latest version
**v3.3.11** - adapted for Unciv 4.21.8.2
:::

## v3.3.11 - for 4.21.8.2

### ⚖️ Maintenance system rework

- Migrated to the Civilization 6 style unit maintenance system: the per-level tiered maintenance uniques gated by command level are gone, replaced by the `maintenanceCost` field + the `unitMaintenanceBaseCost` global constant - fixed per-unit upkeep, no game-progress inflation, no default free units
- Cleaned up redundant Comments and orphan translations left over from the migration

### 📜 National mobilization optimization

- National mobilization subsidy: 80 per-unit repeated uniques merged into one Countable expression + a unit tag, greatly simplifying the ruleset

### 🤖 Lua system rework

- Made good use of the Lua system: the general/admiral aura (command level decides which eras it covers) and AI civic unlocking by era are now Lua implementations - the former per-era/per-entry 60+ uniques were removed
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
