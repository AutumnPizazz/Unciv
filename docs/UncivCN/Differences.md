# UncivCN vs Upstream

> This page only lists **gameplay content** differences. Engineering/coding conventions are in [Coding standards](./Coding-standards), including doc-site maintenance details.

| Feature | Upstream Unciv | UncivCN |
|---|---|---|
| Multiplayer | Dynamic turns (strict sequential) | Dynamic turns + **polling multiplayer** (time-slice rotation, wait capped at seconds) |
| Multiplayer server | Official server | Default `sp.unciv.cn:30123` |
| Multiplayer fairness | — | RNG re-roll on load disabled online |
| Map types | Standard maps | + **three mirrored map modes** (mirrored resources) + **ring maps** (no radius parity restriction) |
| Mod scripting | Pure JSON | + **Lua scripting** (Unique triggers / lifecycle hooks / ctx API) |
| Mod JSON | Whole-object replacement / basic `_mergeAction` | **MergeAction field-level merge extension**, new Countables parameters |
| Mod checker | Checks JSON | + lints Lua errors |
| Tile ownership | None | **tile-claim** (cities claim/swap tiles) |
| Citizen management | Manual locking | + **auto-lock button** (permanent toggle) |
| Map annotations | None | **unit pins / map pins** (notes in `{saveName}_notes`) |
| Game setup | Manual settings | **export/import settings to clipboard** |
| Save compatibility | No isolation | **version isolation** (old clients can't load new saves) |
| Bundled mods | None | 4.21.0.1 bundled UCCC; reverted in 4.21.5.1 — no mods bundled anymore |
| Localization | Multilingual | + full Simplified Chinese localization (incl. Unique translations) |
| Game content | Upstream | Always tracks the latest upstream (currently based on 4.21.6) |

## Notes

- All UncivCN features are **additive** — upstream behavior is unchanged; with options off it behaves like upstream
- Chinese UI and docs are first-class: all Uniques and UI strings have Chinese translations
- Version = upstream version + CN sub-version (`.1`, `.2`, `.3`…; the same upstream version can have multiple CN sub-versions; restarts at `.1` after merging a new upstream), tracked in [Changelog](./Changelog)

## Related

- [Coding standards](./Coding-standards)
- [Changelog](./Changelog)
- [Features](./Features)
