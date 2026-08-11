# LuaMapScriptExample

A complete, copy-and-rename example **Lua map script** mod. It generates a simple
Perlin-noise island map: raises land, assigns continents, adds climate/mountains/coasts/
rivers, places one balanced starting location with luxury and strategic resources.

## How Lua map scripts work

A mod that ships a `scripts/` folder with a `.lua` file defining **both** of these
functions becomes a selectable map type in the new-game screen:

| Function | Purpose |
|----------|---------|
| `GetMapScriptInfo()` | Returns `{ name = "...", description = "..." }` shown in the UI |
| `GenerateMap(ctx)` | Builds the map; returns `true` on success |

When the player picks **Lua Generated** as the map type and this script, the game
creates an all-ocean `TileMap` of the configured size, calls `GenerateMap(ctx)` and
normalizes the result against the ruleset. `ctx` exposes `params`, `seed`, `perlin`,
`random`/`randomInt`, `log` and the `map` manipulation table (see the header comment
in `scripts/mapScript.lua` for the full API list).

## Try it

1. Copy this folder into `UncivMods/` (or use the in-game mod import) and enable it.
2. Start a new game, set **Map Type → Lua Generated**, pick "Lua Example: Perlin Islands".
3. Pick your map size/shape, then start the game.

The example intentionally uses a small API subset; the engine provides many more
helpers (`floodFill`, `generateIce`, `strategicBalanceStarts`, tile-level writes...)
that are exercised by the test suite in `tests/src/com/unciv/logic/scripting/LuaMapGenAPITests.kt`.
