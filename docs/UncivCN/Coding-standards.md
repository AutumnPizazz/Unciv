# Coding standards (UncivCN branch)

> **This is the engineering standards summary of the UncivCN branch — every item comes from an actual pitfall.**
> **Read this document in full before touching docs, doc generators, unique definitions or translation-related code.**

## 1. Doc site maintenance (VitePress vs upstream mkdocs)

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

## 2. Auto-generated documents

| File | Generator | Maintenance |
|---|---|---|
| `docs/Modders/uniques.md` | `UniqueDocsWriter.write()` | **fully generated** — change the generator source |
| `docs/zh/Modders/uniques.md` | `UniqueDocsWriter.writeChinese()` | **fully generated** — change the generator source |
| `docs/Modders/Unique-parameters.md` | `UniqueDocsWriter.writeCountables()` | marker region auto, rest hand-maintained |
| `docs/zh/Modders/Unique-parameters.md` | `UniqueDocsWriter.writeCountables()` | marker region auto (Chinese translations live in the generator), rest hand-maintained |
| `docs/Modders/Mod-file-structure/6-MergeActions.md` | `MergeActionDocsWriter` | **fully generated** |
| `docs/Modders/Creating-a-UI-skin.md` | `UiElementDocsWriter` | marker region auto, rest hand-maintained |
| `docs/zh/Modders/Creating-a-UI-skin.md` | `UiElementDocsWriter` | marker region auto, rest hand-maintained |
| All other `docs/`, `docs/zh/` pages | — | hand-maintained |

**Rule**: generated docs are maintained exclusively by "change the generator source + run `./gradlew desktop:generateDocs`"; **never hand-edit the output**. The Chinese translations embedded in generators (`docsSentence` / `countablesTranslate`) are maintained with the source.

Local preview: double-click `docs-vitepress/build.bat` (build / open existing / rebuild+restart; the server auto-exits after 5 idle minutes).

## 3. Unique & translation conventions

1. **Define `docDescriptionZh` alongside `docDescription`**: whenever you add or change a `docDescription` on a `UniqueType` / `UniqueParameterType`, **also write `docDescriptionZh`** (the Chinese explanation) in the same place — the doc generator reads it automatically and falls back to English when missing. Never maintain big translation maps in the generator.
2. **Never translate JSON literals** (see rule 2): unique texts, parameter names, Countables texts/examples stay in English; only explanatory prose is translated.
3. **Translation channels**:
   - Strings shown in-game → translation templates (`template.properties` / `Simplified_Chinese.properties`)
   - Doc-only strings (`docDescription`, UniqueTarget docs, Countables docs) → source fields (`docDescriptionZh`) or generator-embedded translations (`docsSentence` / `countablesTranslate`)
4. **Key consistency**: translation keys use `getTranslatable()` (numbered repeated placeholders); doc headings show the numbered form, but examples always use the raw text.

## 4. Branch conventions

- **Version** = upstream version + CN sub-version (e.g. upstream 4.21.5 → UncivCN 4.21.5.1; the same upstream version may get `.1`/`.2`/`.3`…, e.g. 4.20.8.1 → 4.20.8.4; the sub-version restarts at `.1` after merging a new upstream), defined in `buildSrc/src/main/kotlin/BuildConfig.kt`
- **Additive changes**: new features must not change upstream behavior; with options off the game behaves like upstream
- **Doc mirroring**: `docs/zh/` mirrors the English section one-to-one (same path = the translation); Chinese-only content lives only in `docs/zh/UncivCN/` and `docs/zh/Community/`
- **Chinese as first-class**: new UI strings must go into the translation templates; new unique explanations must ship with Chinese

## 5. Changelog & release process

- **Log every change, not just releases**: any non-release change (feature / bugfix / CI / docs site) merged into the branch must add one line to the **Unreleased** section at the top of both `docs/UncivCN/Changelog.md` and `docs/zh/UncivCN/Changelog.md`, in the same commit as the change itself. Entries only accumulate there until the next release.
- **Consolidate on release**: when releasing, move the whole Unreleased section into the new version entry (`## vX.Y.Z.N (build NNNN)`), keeping the same wording, then empty the Unreleased section.
- **Version bump**: only edit `buildSrc/src/main/kotlin/BuildConfig.kt` (`appVersion` + `appCodeNumber` +1). The `syncGameVersion` Gradle task runs before every core build and automatically mirrors the values into the `AUTOMATICALLY GENERATED VERSION DATA` region of `UncivGame.kt` (the in-game version display) — never hand-edit that region.
- **Release tag**: the MSI installer version comes from the git tag (`github.ref_name`, 4-segment form like `4.21.5.3`); every release must push a tag matching the version to trigger the Deploy workflow.
- **Upstream changelog pages**: the English page `docs/Community/Upstream-changelog.md` embeds the full repo-root `changelog.md` at build time (the `upstream-changelog` container in `docs-vitepress/.vitepress/config.ts`), so it stays current automatically after every upstream merge — never maintain an English copy by hand. The Chinese page `docs/zh/Community/Upstream-changelog.md` is **human-translated only, no embedded English**: it covers recent versions (may lag) and links to the EN page for the full history; backfill translations gradually, newest versions first.

## Related

- [Differences vs upstream](./Differences)
- [Changelog](./Changelog)
