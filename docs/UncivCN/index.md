# The UncivCN Branch

## What is this?

**UncivCN** is a community fork of the open source game [Unciv](https://github.com/yairm210/Unciv) (a Civ V remake in LibGDX, supporting Android / Desktop / Server), deeply customized for Chinese-speaking players: it adds multiplayer, map, modding and UI features on top of the original, while staying fully in sync with upstream updates.

- Upstream repository: https://github.com/yairm210/Unciv
- UncivCN repository: https://github.com/AutumnPizazz/Unciv
- Current version: **4.21.5.1** (based on upstream 4.21.5)

## Relationship with upstream

- **In sync**: upstream master is merged into UncivCN regularly. Version number = upstream version + CN sub-version (e.g. 4.21.5 → 4.21.5.1; the same upstream version can have multiple CN sub-versions .1/.2/.3…, e.g. 4.20.8.1 → 4.20.8.4), always tracking the latest upstream development
- **Additive changes**: all UncivCN features are implemented incrementally; upstream behavior is unaffected
- **Fork point**: UncivCN has evolved independently since 2026 ([4c23bb4dc](https://github.com/AutumnPizazz/Unciv/commit/4c23bb4dc)), with its own versioning and release cadence

## Feature overview

| Category | Features |
|---|---|
| Multiplayer | [Polling multiplayer](./Polling-multiplayer), default server (sp.unciv.cn), save-scumming RNG disabled online |
| Maps | Three mirrored map modes, wraparound (ring) maps, no radius parity restriction |
| Modding | [Lua scripting](./Features#lua-scripting-since-4-20-8-2-continuously-extended), [MergeAction extended JSON](./Features#mergeaction-extended-json-system-4-20-7-3), tile-claim |
| UI | Unit pins / map pins, auto-lock citizens button, export/import game settings to clipboard |
| Other | Save version isolation, RNG re-rollable on load (single-player) |

Details in [Features](./Features) and [Differences vs upstream](./Differences).

## Building

```bash
./gradlew desktop:run        # run desktop version
./gradlew :tests:test        # run all tests
./gradlew desktop:dist       # build JAR → desktop/build/libs/Unciv.jar
./gradlew server:run         # run multiplayer server
```

Android builds need `local.properties` (`sdk.dir`) or the `ANDROID_HOME` environment variable.

The version is defined in `buildSrc/src/main/kotlin/BuildConfig.kt`; changes are tracked in [Changelog](./Changelog).
