# Coding standards (UncivCN branch)

> **This is the engineering handbook of the UncivCN branch: development reference (building / project structure / game state / assets) plus engineering standards — every item comes from an actual pitfall.**
> **Read this document in full before touching docs, doc generators, unique definitions or translation-related code.**

## 1. Building & running

```bash
./gradlew desktop:run                                # Run the desktop version (main dev loop)
./gradlew :tests:test                                # Run all tests
./gradlew :tests:test --tests "com.unciv.testing.BasicTests"   # Run a single test class
./gradlew desktop:dist                               # Build the JAR → desktop/build/libs/Unciv.jar
./gradlew server:run                                 # Run the server
./gradlew server:dist                                # → server/build/libs/UncivServer.jar
java -jar detekt-cli.jar --parallel --report html:detekt/reports.html \
  --config .github/workflows/detekt_config/detekt-warnings.yml    # Detekt check (warnings)
```

Tech stack: Kotlin 2.1.21 · Gradle 8.11.1 · LibGDX 1.14.0 · Ktor 3.2.3 · Kotlinx Serialization · purity-plugin · Detekt

For a from-scratch setup (environment, IDE configuration) see [Building-Locally](../Developers/Building-Locally).

## 2. Project structure

| Module | Responsibility |
|---|---|
| `core/` | 99% of the game code: logic, models, UI, serialization, ruleset. Pure Kotlin, no platform dependencies |
| `desktop/` | Desktop launcher and platform features (JNA notifications, Discord RPC) |
| `android/` | Android launcher; `android/assets/` holds the shared assets for all platforms (images, JSON, translations) |
| `server/` | Multiplayer server (Ktor + WebSocket), packaged separately |
| `tests/` | JUnit 4 + Mockito unit tests |

Key packages (`core/src/com/unciv/`):

| Package | Purpose |
|---|---|
| `models/ruleset/` | Ruleset classes: Nation, Building, BaseUnit, Technology, Policy, etc. |
| `logic/civilization/` | CivilizationInfo and its managers (tech / policy / diplomacy) |
| `logic/city/` | CityInfo, city production / yields / population |
| `logic/map/` | TileMap, TileInfo, pathfinding (BFS, AStar) |
| `logic/map/mapunit/` | Unit instances on the map |
| `logic/battle/` | Combat resolution |
| `logic/automation/` | AI and automation logic (worker automation, next-turn flow) |
| `logic/trade/` | Trade routes and trade deals |
| `ui/screens/wordlscreen/` | Main game screen (where most play time is spent) |
| `ui/screens/cityscreen/` | City management screen |
| `ui/screens/pickerscreens/` | Tech / policy / promotion picker screens |
| `ui/popups/` | Modal popups |
| `json/` | JSON serialization configuration, UncivJson |

For a detailed class overview see [Project-structure-and-major-classes](../Developers/Project-structure-and-major-classes).

## 3. Game state & turn flow

`GameInfo` is the serialization root: it contains `List<CivilizationInfo>` (players, each with a `List<CityInfo>`), a `TileMap` (`List<TileInfo>`, which may hold `MapUnit`s) and a `RuleSet` (**not serialized** — loaded from `android/assets/jsons/`). Every state object holds a `@Transient` parent reference, so the tree is traversable in both directions.

Every turn the `GameInfo` is **cloned first and processed on the new copy** — this guarantees UI-thread safety and deterministic multiplayer (state is sent and received as whole packages).

Serialization details: see [Saved-games-and-transients](../Developers/Saved-games-and-transients).

## 4. Doc site maintenance (VitePress vs upstream mkdocs)

The UncivCN doc site (`docs-vitepress/`) uses VitePress (mkdocs was dropped: no Chinese search support); upstream still uses mkdocs. The two syntaxes are **not compatible** — these are the recorded pitfalls:

| Item | Upstream mkdocs | UncivCN VitePress |
|---|---|---|
| Containers (admonitions) | `!!! note` + content **indented 4 spaces** | `::: note` + content **must not be indented** |
| Block types | all admonition types | built-in tip/info/warning/danger/details; `note` needs a custom registration |
| Abbreviation tooltips | `*[param]: text` auto-renders `<abbr>` | not supported — use a parameter table instead |
| Doc generator output | mkdocs format (indented containers) | VitePress format (non-indented containers) |

### Writing rules (all from real pitfalls)

1. **Never indent container content**: 4 spaces / tabs inside a VitePress container are parsed as a code block (`<pre>`), which does not wrap and pushes long paragraphs off-screen. Applies to generator output and hand-written docs alike.
2. **Never translate JSON literals**: unique texts, parameter names, Countables texts/examples must stay in English — the game matches them verbatim against `UniqueType.uniqueTypeMap`; a translated literal simply won't work, and modders copying from the docs would be misled.
3. **Number repeated parameters**: in unique headings, repeated parameters are shown numbered (`[amount] [amount]` → `[amount]-[amount2]`, matching the translation template's `getTranslatable()`), so readers can tell them apart; example lines still use the raw text (directly copyable into mod JSON).
4. **Auto-generated docs are edited only through generator source**: change the generator, then run `./gradlew desktop:generateDocs`; never hand-edit its output.
5. **Multi-line table cells**: docDescriptions containing newlines break markdown tables; the generator replaces them with `<br>` — do the same when maintaining tables by hand.

## 5. Auto-generated documents

| File | Generator | Maintenance |
|---|---|---|
| `docs/Modders/uniques.md` | `UniqueDocsWriter.write()` | **fully generated** — change the generator source |
| `docs/zh/Modders/uniques.md` | `UniqueDocsWriter.writeChinese()` | **fully generated** — change the generator source |
| `docs/Modders/Unique-parameters.md` | `UniqueDocsWriter.writeCountables()` | marker region auto, rest hand-maintained |
| `docs/zh/Modders/Unique-parameters.md` | `UniqueDocsWriter.writeCountables()` | marker region auto (Chinese translations live in the generator), rest hand-maintained |
| `docs/Modders/Mod-file-structure/6-MergeActions.md` | `MergeActionDocsWriter` | **fully generated** |
| `docs/Modders/Creating-a-UI-skin.md` | `UiElementDocsWriter` | marker region auto, rest hand-maintained |
| `docs/zh/Modders/Creating-a-UI-skin.md` | `UiElementDocsWriter` | marker region auto, rest hand-maintained |
| `docs/Modders/lua-api.lua` | `LuaApiDefinitionWriter` | **fully generated** — signatures & docs from `LuaApiDocs` |
| `docs/Modders/Lua-API-Reference.md` | `LuaModdingDocsWriter.write()` | **fully generated** — from `LuaApiDocs` |
| `docs/zh/Modders/Lua-API-Reference.md` | `LuaModdingDocsWriter.writeChinese()` | **fully generated** — from `LuaApiDocs` |
| All other `docs/`, `docs/zh/` pages | — | hand-maintained |

**Rule**: generated docs are maintained exclusively by "change the generator source + run `./gradlew desktop:generateDocs`"; **never hand-edit the output**. The Chinese translations embedded in generators (`docsSentence` / `countablesTranslate`) are maintained with the source.

**Adding a Lua API**: register it in both `LuaAPI.apiCatalog` and `LuaApiDocs` (signature, EN/ZH descriptions, category), then run `./gradlew desktop:generateDocs` — `lua-api.lua` and both `Lua-API-Reference.md` files regenerate together. `LuaApiDocsTests` fails the build if they drift. The Lua doc generators share the `DocsWriter` base class (pure `generate()` + write-to-disk), also used by `UniqueDocsWriter`.

**No Python**: the CN branch's docs toolchain needs only JDK + Node (VitePress + Node scripts under `docs-vitepress/scripts/`, e.g. `check-docs-links.mjs` for CI link checks). Upstream's `mkdocs.yml` / `mkdocs` workflow are removed on this branch — re-remove them when merging upstream if they reappear.

Local preview: double-click `docs-vitepress/build.bat` (build / open existing / rebuild+restart; the server auto-exits after 5 idle minutes).

## 6. Unique & translation conventions

1. **Translation sources**: every user-visible string must be translatable; entries come from three channels:
   - JSON assets (`android/assets/jsons/`) — collected automatically by `TranslationFileWriter`
   - The Unique system — `UniqueType` / `UniqueParameterType` auto-generate entries
   - Manual templates — Kotlin UI strings are added by hand to `android/assets/jsons/translations/template.properties`
2. **Placeholder rules**:
   - `[]`: both the tag and its content are translated (e.g. `[amount] gold`); **empty `[]` is forbidden** — use meaningful tags (`[amount]`, `[city]`, etc.), and multiple placeholders in one sentence must use different tags
   - `{}`: the content is translated, the surrounding text stays as-is
   - `[]`, `{}`, `<>` must not appear in translatable text — use `()` instead
   - The tests `allTranslationsHaveNoExtraPlaceholders` and `allTranslationsHaveCorrectPlaceholders` verify placeholder consistency
3. **Define `docDescriptionZh` alongside `docDescription`**: whenever you add or change a `docDescription` on a `UniqueType` / `UniqueParameterType`, **also write `docDescriptionZh`** (the Chinese explanation) in the same place — the doc generator reads it automatically and falls back to English when missing. Never maintain big translation maps in the generator.
4. **Never translate JSON literals** (see rule 2): unique texts, parameter names, Countables texts/examples stay in English; only explanatory prose is translated.
5. **Translation channels**:
   - Strings shown in-game → translation templates (`template.properties` / `Simplified_Chinese.properties`)
   - Doc-only strings (`docDescription`, UniqueTarget docs, Countables docs) → source fields (`docDescriptionZh`) or generator-embedded translations (`docsSentence` / `countablesTranslate`)
6. **Key consistency**: translation keys use `getTranslatable()` (numbered repeated placeholders); doc headings show the numbered form, but examples always use the raw text.

## 7. Branch conventions

- **Version** = upstream version + CN sub-version (e.g. upstream 4.21.5 → UncivCN 4.21.5.1; the same upstream version may get `.1`/`.2`/`.3`…, e.g. 4.20.8.1 → 4.20.8.4; the sub-version restarts at `.1` after merging a new upstream), defined in `buildSrc/src/main/kotlin/BuildConfig.kt`
- **Additive changes**: new features must not change upstream behavior; with options off the game behaves like upstream
- **Upstream merges**: regularly merge new code from the upstream master into the UncivCN branch and resolve merge conflicts yourself, making sure upstream features are fully integrated (never dropped because of conflicts). If upstream and the CN branch implemented the same feature differently, ask the user which implementation to adopt before continuing.
- **Doc mirroring**: `docs/zh/` mirrors the English section one-to-one (same path = the translation); Chinese-only content lives only in `docs/zh/UncivCN/` and `docs/zh/Community/`
- **Chinese as first-class**: new UI strings must go into the translation templates; new unique explanations must ship with Chinese

## 8. Changelog & release process

- **Log every change, not just releases**: any non-release change (feature / bugfix / CI / docs site) merged into the branch must add one line to the **Unreleased** section at the top of both `docs/UncivCN/Changelog.md` and `docs/zh/UncivCN/Changelog.md`, in the same commit as the change itself. Entries only accumulate there until the next release.
- **Consolidate on release**: when releasing, move the whole Unreleased section into the new version entry (`## vX.Y.Z.N (build NNNN)`), keeping the same wording, then empty the Unreleased section.
- **Version bump**: only edit `buildSrc/src/main/kotlin/BuildConfig.kt` (`appVersion` + `appCodeNumber` +1). The `syncGameVersion` Gradle task runs before every core build and automatically mirrors the values into the `AUTOMATICALLY GENERATED VERSION DATA` region of `UncivGame.kt` (the in-game version display) — never hand-edit that region.
- **Release tag**: the MSI installer version comes from the git tag (`github.ref_name`, 4-segment form like `4.21.5.3`); every release must push a tag matching the version to trigger the Deploy workflow.
- **Upstream changelog pages**: the English page `docs/Community/Upstream-changelog.md` embeds the full repo-root `changelog.md` at build time (the `upstream-changelog` container in `docs-vitepress/.vitepress/config.ts`), so it stays current automatically after every upstream merge — never maintain an English copy by hand. The Chinese page `docs/zh/Community/Upstream-changelog.md` is **human-translated only, no embedded English**: it covers recent versions (may lag) and links to the EN page for the full history; backfill translations gradually, newest versions first.

### Release checklist & lessons learned (from the 4.21.6.x series)

Before releasing, check off:
- [ ] Version bump in `buildSrc/src/main/kotlin/BuildConfig.kt` (`appVersion` + `appCodeNumber` +1); after a build, confirm `syncGameVersion` mirrored it into `UncivGame.kt` (`VERSION = Version("x.y.z.n", NNNN)`)
- [ ] Changelog: move the whole Unreleased section into the new version entry (both EN and ZH), wording unchanged
- [ ] Version references: the current-version line in `docs/{,zh/}UncivCN/index.md`
- [ ] Local verification: `./gradlew tests:test` **and** `cd docs-vitepress && npm run docs:build` (skipping the latter only surfaces docs failures in CI after the release)
- [ ] Push both the branch **and the tag separately** (`git push origin <branch>` + `git push origin <tag>`); the Deploy workflow triggers on tags only — pushing the branch alone does not release

After releasing, verify GitHub Release asset completeness: `UncivCN-<version>.Apk`, `.jar`, `Windows64.zip`, `Linux64.zip`, `UncivServer-<version>.jar`, `.msi`.

Lessons learned:
- **No bare angle brackets in markdown**: `<gameId>` is parsed by VitePress/Vue as an unclosed HTML tag (`Element is missing end tag`) and breaks the docs build — use `{gameId}` or `&lt;...&gt;`
- **Maven Central 403** (`Could not GET ... 403 Forbidden`): intermittent rate limiting on GitHub runners — re-run the failed job, no code change needed
- **Stale hardcoded names after the app rename**: search for old names (e.g. Dockerfile's `Unciv.jar`, `Unciv-Linux64.zip`) — CN artifacts are all `UncivCN.*`
- **`continue-on-error` jobs hide failures**: e.g. `build-msi` (wix defaults to naming the output after the source file, `unciv.msi`, while the upload path expects `UncivCN.msi`) — always verify the Release assets
- **New strings of CN-only features must get `Simplified_Chinese.properties` translations in the same change**: otherwise Chinese players see English (consider adding a CI check later)

## 9. Mods & assets

- Mods live in `android/assets/mods/` and extend the ruleset via JSON
- Atlases (`Icons.atlas`, `NationIcons.atlas`, etc.) and sound effects live in `android/assets/` (`sounds/` subdirectory)
- Ruleset JSONs live in `android/assets/jsons/`
- In the IDE, mark `android/assets/SaveFiles/` and `android/assets/mods/` as Excluded

## 10. Miscellaneous notes

- The version is defined in `buildSrc/src/main/kotlin/BuildConfig.kt`; semantic versioning, changes recorded in `changelog.md`
- Android builds need `local.properties` (`sdk.dir`) or the `ANDROID_HOME` environment variable
- Android Studio must be set to 4-space Kotlin continuation indent
- Core code must be compatible with all platforms; game logic runs mainly on the main thread — be careful with async operations
- Keep mod compatibility in mind when changing ruleset-related code

## Related

- [Differences vs upstream](./Differences)
- [Changelog](./Changelog)
