# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build and run desktop version (main dev workflow)
rtk ./gradlew desktop:run

# Run all tests
rtk ./gradlew :tests:test

# Run a single test class
rtk ./gradlew :tests:test --tests "com.unciv.testing.BasicTests"

# Build desktop JAR
rtk ./gradlew desktop:dist
# Output: desktop/build/libs/Unciv.jar

# Run server
rtk ./gradlew server:run

# Build server JAR
rtk ./gradlew server:dist
# Output: server/build/libs/UncivServer.jar

# Lint with detekt (warnings)
rtk java -jar detekt-cli.jar --parallel --report html:detekt/reports.html --config .github/workflows/detekt_config/detekt-warnings.yml
```

## Project Architecture

**Unciv** = open-source Kotlin remake of Civilization V using LibGDX.

### Modules
- **core/** — 99% of game code: logic, models, UI, serialization, rulesets. Platform-independent Kotlin.
- **desktop/** — Desktop launcher + platform-specific (JNA for notifications, Discord RPC).
- **android/** — Android launcher + assets (`android/assets/`) shared by all platforms.
- **server/** — Multiplayer server (Ktor + WebSocket). Packaged as separate jar.
- **tests/** — JUnit 4 + Mockito. Run via `:tests:test`.

### Key packages (`core/src/com/unciv/`)
| Package | Purpose |
|---------|---------|
| `models/ruleset/` | Ruleset classes: `Nation`, `Building`, `BaseUnit`, `Technology`, `Policy`, `Terrain`, `TileImprovement`, `TileResource`, `Promotion` |
| `logic/civilization/` | `CivilizationInfo` + managers (`TechManager`, `PolicyManager`, `DiplomacyManager`, etc.) |
| `logic/city/` | `CityInfo`, `CityConstructions`, `CityStats`, `PopulationManager` |
| `logic/map/` | `TileMap`, `TileInfo`, pathfinding (`BFS`, `AStar`) |
| `logic/map/mapunit/` | `MapUnit` (unit instance on map) |
| `logic/battle/` | Combat resolution (`Battle`, `ICombatant`) |
| `logic/automation/` | AI and automation logic (worker automation, next-turn sequence) |
| `logic/trade/` | Trade routes and trade logic |
| `ui/screens/wordlscreen/` | Main gameplay screen (90% of play time) |
| `ui/screens/cityscreen/` | City management screen |
| `ui/screens/pickerscreens/` | Tech/Policy/Promotion picker screens |
| `ui/popups/` | Modal dialogs |
| `json/` | JSON serialization config, `UncivJson` |

### Game State Hierarchy
`GameInfo` is the serialization root, containing:
- `List<CivilizationInfo>` — players (each has `List<CityInfo>`)
- `TileMap` — contains `List<TileInfo>` (each may hold `MapUnit`)
- `RuleSet` — NOT serialized; loaded from `android/assets/jsons/` JSON files

Each state object holds a `@Transient` parent reference, so the tree is traversable in all directions.

### Next Turn Flow
Each turn clones `GameInfo` and processes on a new copy — this ensures thread safety (UI remains responsive) and multiplayer determinism (state can be sent/received as a whole).

## Design Conventions

### Crashing Early
When game state is invalid, crash immediately rather than silently continuing. This surfaces bugs faster.

### No Premature Abstraction
Don't create interfaces for single implementations. Don't abstract until there's a second concrete need. `if(x != null)` is preferred over `.let{}` and `?:`.

### Coding Style
- `for(item in list)` — not `list.forEach{}` (better debugger behavior)
- `if(x != null)` — not `.let{}` or `?:` (more readable for contributors from Java/C#)
- Kotlin continuation indent: **4 spaces** (non-default, set in IDE)
- Remove trailing whitespace before commits on non-translation files (trailing spaces in `template.properties` are significant)

### Purity Annotations (`purity-plugin`)
- `@Readonly` — marks methods with no side effects
- `@Cache` — marks cache methods

## Translation System

All user-visible strings must be translatable. There are three sources:

1. **JSON assets** (`android/assets/jsons/`) — strings in game data files are auto-collected by `TranslationFileWriter`
2. **Unique system** — `UniqueType` and `UniqueParameterType` generate translation entries automatically
3. **Manual templates** — UI strings in Kotlin code must be manually added to `android/assets/jsons/translations/template.properties`

**Placeholder rules:**
- `[]` square brackets — label + content are both translated (e.g., `[amount] gold`)
- `{}` curly brackets — content translated, surrounding text verbatim
- `[]`, `{}`, `<>` cannot appear in translatable text; use `()` instead
- Empty `[]` is forbidden — always use meaningful labels like `[amount]`, `[city]`, etc.
- Tests `allTranslationsHaveNoExtraPlaceholders` and `allTranslationsHaveCorrectPlaceholders` validate placeholder consistency

## Modding System

- Mods extend rulesets via JSON files in `android/assets/mods/`
- Everything is parameterized through the `Unique` system — minimize adding new object types, maximize unique combinations
- Modding philosophy: Conditional uniques that can apply everywhere > special-case properties
- Type-checking for modders is documented in `docs/Modders/Type-checking.md`

## Assets

- `android/assets/` — shared assets for all platforms (images, sounds, JSON rulesets, translations)
- Image atlases: `Icons.atlas`, `NationIcons.atlas`, etc. in `android/assets/`
- Sound: `android/assets/sounds/`
- `android/assets/SaveFiles/` and `android/assets/mods/` should be excluded from indexing in IDE
