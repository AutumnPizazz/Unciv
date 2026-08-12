# Lua Mod Scripts

Since version 4.20.8.2, Unciv supports Lua scripts in mods. Using the `TriggerLuaFunction` unique, you declare a trigger in JSON and delegate complex logic to Lua.

## Quick Start

### Directory Structure

Create a `scripts/` folder in your mod root, containing `.lua` files:

```
MyMod/
├── jsons/
│   └── Buildings.json
├── scripts/
│   └── myFunctions.lua
└── ...
```

### Declaring a Trigger

Use `TriggerLuaFunction` in JSON uniques. It can appear on any object supporting `Triggerable`:

- **Building / Wonder** — when built
- **Tech** — when researched
- **Policy** — when adopted
- **Era** — when entering a new era
- **Event / EventChoice** — when the event fires
- **Unit / Promotion** — when the unit performs actions
- **GlobalUniques** — with a trigger condition for periodic execution

```json
{
    "name": "Palace",
    "uniques": ["Trigger the function [myMod:onPalaceBuilt] with [[Gold] + 50]"]
}
```

### Writing Lua Functions

```lua
function onPalaceBuilt(ctx)
    local gold = tonumber(ctx.parameter) or 0
    ctx.civ.addNotification("Lua script fired! Gold param: " .. tostring(gold))
    ctx.civ.addGold(gold)
    return true
end
```

> **Key conventions**:
> 1. The function receives exactly one argument — the `ctx` context table
> 2. The `[parameter]` from the unique is available via `ctx.parameter`
> 3. `game`, `civ`, `city`, `unit` are not globals — they live under `ctx`
> 4. API functions use `.` syntax (e.g. `civ.addGold(500)`), not `:` syntax

> **⚠️ Most common mistake: confusing where arguments come from**
>
> The engine passes exactly **one argument** — the `ctx` context table. Whatever you name the first parameter, it is always `ctx`.
>
> ```lua
> -- ❌ Wrong (easy to trip on)
> function onTech(ctx, n)         -- n is always nil! The engine only passes one argument
>     local civ = game.getCurrentPlayer()  -- game is nil! It is not a global
>     civ.addGold(n)              -- n is nil, nothing happens
> end
>
> -- ✅ Correct
> function onTech(ctx)
>     local n = tonumber(ctx.parameter)   -- the parameter comes from ctx.parameter
>     local civ = ctx.game.getCurrentPlayer()  -- game is a field of ctx
>     civ.addGold(n)
> end
> ```
>
> Three rules to remember:
> 1. **The first parameter is always `ctx`** — the context table injected by the engine
> 2. **`game`, `civ`, `city` are not globals** — they all live under `ctx`
> 3. **The unique's parameter comes from `ctx.parameter`** — it is a string; use `tonumber()` when you need a number

### Lifecycle Hooks

Combine `TriggerLuaFunction` with trigger conditions for per-turn execution. Place the unique in `GlobalUniques.json`:

```json
// GlobalUniques.json
"uniques": [
    "Trigger the function [myMod:onTurnStart] with [] <upon turn start>",
    "Trigger the function [myMod:onTurnEnd] with [] <upon turn end>"
]
```

Also supported: `<upon discovering [techFilter] technology>`, `<upon conquering a city>`, `<upon founding a city>`, etc.
Also supported: `<upon completing a trade with [civFilter] Civilizations>` fires for both sides of any accepted trade (including AI auto-accepted ones). Other warfare/diplomacy hooks: `<upon declaring war on [civFilter] Civilizations>`, `<upon being declared war on by [civFilter] Civilizations>`, `<upon entering a war with [civFilter] Civilizations>`, `<upon signing a peace treaty with [civFilter] Civilizations>`, `<upon losing a city>`.

## Unique Syntax

```
"Trigger the function [luaFunction] with [parameter]"
```

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `[luaFunction]` | Function reference, format `modName:functionName`. Omitting the mod prefix searches all loaded mods (specifying it is recommended). | `myMod:onWarDeclared` |
| `[parameter]` | Free text, supports embedded Countable expressions resolved at runtime. | `[Gold] * 3 + [Culture]` |

Countable expressions are resolved to strings before Lua execution. For example, if gold is 500, `[Gold] + 50` becomes `"500 + 50"`. Parse it in Lua with `tonumber(ctx.parameter)`.

## The ctx Context Object

| Field | Type | Description |
|-------|------|-------------|
| `ctx.parameter` | string | Resolved parameter value |
| `ctx.civ` | table | Triggering civilization (always present) |
| `ctx.city` | table | Triggering city (may be nil) |
| `ctx.unit` | table | Triggering unit (may be nil) |
| `ctx.tile` | table | Triggering tile (may be nil) |
| `ctx.game` | table | Global game state |
| `ctx.store` | table | Persistent per-mod key-value storage |
| `ctx.log(msg)` | function | Write debug output to Unciv's log |
| `ctx.count(expr)` | function | Evaluate a Countable expression at runtime |
| `ctx.evaluateConditional(condition)` | function | Evaluate a conditional string, returns boolean |
| `ctx.random()` | function | Deterministic random in [0, 1) - same call sequence on the same game state yields the same values (online-multiplayer safe) |
| `ctx.randomInt(min, max)` | function | Deterministic random integer in [min, max] (inclusive) |

## API Reference

The complete list of every context-table function and property is generated from the game code and can never go stale — see [Lua API Reference](Lua-API-Reference.md).

The sections below cover specific topics in depth:

## unit.base — Unit Template

The `unit.base` sub-table is read-only and holds the unit template (type definition):

```lua
unit.base.name              -- "Warrior"
unit.base.strength          -- 8
unit.base.rangedStrength    -- 0
unit.base.cost              -- 40
unit.base.movement          -- 2
unit.base.range             -- 1
unit.base.unitType          -- "Melee"
unit.base.requiredResource  -- "" or e.g. "Iron"
unit.base.requiredTech      -- "" or e.g. "Bronze Working"
unit.base.obsoleteTech      -- "" or the obsoleting tech
unit.base.upgradesTo        -- "Swordsman"
unit.base.replaces          -- "Warrior"
unit.base.uniqueTo          -- Exclusive civilization name
unit.base.promotions        -- Initial promotion names
```

## game.findTiles — Tile Search

`findTiles` takes a Lua table of criteria and returns a list of matching tiles. Supported keys:

| Key | Type | Description |
|-----|------|-------------|
| `resource` | string | Resource name (e.g. `"Iron"`) |
| `terrain` | string | Base terrain name (e.g. `"Grassland"`) |
| `terrainFeature` | string | Terrain feature name (e.g. `"Forest"`) |
| `improvement` | string | Improvement name (e.g. `"Farm"`) |
| `owned` | boolean | Tile is owned by some civ |
| `owner` | string | Owning civilization name |
| `isCoast` | boolean | Coastal tile |
| `isLand` | boolean | Land tile |
| `isWater` | boolean | Water tile |
| `isHill` | boolean | Hill tile |
| `maxDistance` + `centerX` + `centerY` | number | Spatial range constraint (all three required) |
| `maxResults` | number | Result cap (default 500) |

```lua
-- All iron on the map
local ironTiles = game.findTiles({ resource = "Iron" })

-- Unowned forest tiles within 5 tiles of (10, 15)
local nearbyForest = game.findTiles({
    terrainFeature = "Forest",
    owned = false,
    maxDistance = 5,
    centerX = 10,
    centerY = 15
})

-- All owned coastal tiles
local ownedCoastal = game.findTiles({ isCoast = true, owned = true })
```

## ctx.store — Persistent Storage

Cross-turn, cross-save key-value storage, automatically isolated per mod. All values are stored as strings:

```lua
ctx.store.set("key", "value")
local val = ctx.store.get("key", "default")
local num = tonumber(ctx.store.get("count", "0"))
```

Storage data is saved in the game file and persists across save/load cycles.

## ctx.evaluateConditional — Conditional Evaluation

Reuses Unciv's built-in conditional system:

```lua
if ctx.evaluateConditional("when at war") then
    -- at war
end
if ctx.evaluateConditional("in coastal cities") then
    -- city is coastal
end
if ctx.evaluateConditional("when number of [Cities] is greater than [5]") then
    -- has more than 5 cities
end
```

## Lua Conditions

Beyond runtime evaluation via `ctx.evaluateConditional`, you can plug a Lua function **directly into the unique condition system**. Any unique can use the condition

```
<if [myMod:myCondition] returns true>
```

The function receives the usual `ctx` table and must return `true` for the unique to apply:

```json
{
    "name": "Rich King",
    "uniques": [
        "[+2 Gold] <if [myMod:isRich] returns true>"
    ]
}
```

```lua
-- scripts/myMod.lua
function isRich(ctx)
    return ctx.civ.getGold() > 1000
end
```

Rules and caveats:

1. **The function is a pure query** - conditions are evaluated very frequently (every time the unique is checked), keep the function cheap and side-effect-free. Missing functions simply evaluate to `false` (never crash); the mod checker reports them at load time.
2. **Performance**: each check crosses the Lua interop boundary. Prefer built-in conditions for hot paths; use Lua conditions for logic that can't be expressed otherwise.
3. Combined with triggers it behaves like any other condition: `"Trigger the function [myMod:onX] with [] <if [myMod:shouldX] returns true> <upon turn start>"` only fires when both the trigger and the Lua condition match.
4. The civ filter convention applies to `[civFilter]` parameters everywhere: use `[All]` to match every civilization (an empty `[]` matches none).

## Complete Example

```lua
-- scripts/rewards.lua
function eraScalingGold(ctx)
    local civ = ctx.civ
    local era = civ.getEra()

    local rewards = {
        ["Ancient era"] = 50,
        ["Classical era"] = 150,
        ["Medieval era"] = 300,
        ["Renaissance era"] = 600,
        ["Industrial era"] = 1200,
        ["Modern era"] = 2500,
        ["Atomic era"] = 5000,
        ["Information era"] = 10000
    }

    local amount = rewards[era] or 50
    civ.addGold(amount)
    civ.addNotification("Received " .. tostring(amount) .. " Gold for entering " .. era .. "!")
    ctx.log("Era reward: " .. tostring(amount) .. " Gold for era " .. era)

    if #civ.getTechsResearched() > 10 then
        local capital = civ.getCapital()
        if capital ~= nil then
            capital.addPopulation(1)
            ctx.log("Bonus: +1 population in capital")
        end
    end

    return true
end
```

Corresponding JSON (in `ModOptions.json` or `Eras.json`):

```json
"uniques": [
    "Trigger the function [myMod:eraScalingGold] with [Gold]"
]
```

## Editor Setup (autocompletion & type hints)

While the game itself validates your scripts (see [Checking Your Mod](#checking-your-mod)), you can get autocompletion, hover docs and immediate typo detection while writing with the Lua language server:

1. **Install the Lua language server**: in VSCode, install **Lua** by sumneko (the LuaLS language server).
2. **Install the Unciv Lua API extension** *(recommended, never go stale)*: download `unciv-lua-api.vsix` from the latest [UncivCN release](https://github.com/AutumnPizazz/Unciv/releases) and install it via **Extensions → ⋯ → Install from VSIX…**. On every VSCode start it pulls the latest `lua-api.lua` / `lua-map-api.lua` from the UncivCN docs site (cloud-deployed on every release) into `~/.unciv/lua-api/` — no manual updates, ever. Offline it keeps the last synced copy.
3. **Configure LuaLS once**: run **Unciv: Configure Lua API autocompletion** from the command palette — it adds `~/.unciv/lua-api` (absolute path) to your user-level `Lua.workspace.library`, which applies to every mod workspace.

If you prefer a fully manual setup, create `.luarc.json` in your mod folder and copy the definitions beside it (e.g. from the repo's `docs/Modders/`):

```json
{
    "runtime.version": "Lua 5.2",
    "workspace.library": [
        ".lua-api"
    ],
    "diagnostics.globals": ["ctx"]
}
```

Optionally add this to `.vscode/extensions.json` so collaborators are prompted to install the language server:

```json
{
    "recommendations": ["sumneko.lua"]
}
```

You now get: `ctx.` autocompletion (civ/city/unit/tile/game/store), method-name completion and hover docs (e.g. `ctx.civ.addGold(`), and instant red squiggles for typos like `ctx.civ.addGoldd(...)`. Map scripts (`GenerateMap(ctx)`) get the same treatment from `lua-map-api.lua` — both files are managed by the extension automatically.

> **Limitations**: the definition file is generated from the API catalog with best-effort signatures - parameter/return types are precise for common patterns and loose (`fun(...)`) for the rest. When in doubt, trust the in-game mod checker or `mod-ci` (they are authoritative), and check the function's actual behavior in-game.

## Lua Map Scripts

Since the map-generation rewrite, mods can provide **entire map generators in Lua**. A mod whose `scripts/` folder defines these two functions shows up as a new map type:

| Function | Purpose |
|----------|---------|
| `GetMapScriptInfo()` | Returns `{ name = "...", description = "..." }` - shown in the new-game screen |
| `GenerateMap(ctx)` | Builds the map; return `true` on success |

On the new-game screen pick **Map Type → Lua Generated**, then select the script. The engine creates an all-ocean `TileMap` of the configured size and calls `GenerateMap(ctx)`, then normalizes every tile against the ruleset.

The full, always-up-to-date reference of the map-script ctx (`ctx.params`, `ctx.map` helpers, the generation tile table) is generated from the game code - see [Lua Map-Script API Reference](Lua-Map-API-Reference.md).

> **Important**: iterate tiles with `map.getAllTiles()` rather than assuming coordinates start at 0 - rectangular maps are centered on (0,0), so `params.bounds.minX`/`minY` are negative.
>
> `GenerateMap` and `GetMapScriptInfo` are **reserved names**: the mod checker rejects any in-game `TriggerLuaFunction` that references them, since they only run during generation.

A complete copy-and-rename example lives at `docs/Modders/examples/LuaMapScriptExample/` (see its README).

For editor autocompletion of map scripts, point the LuaLS language server at `docs/Modders/lua-map-api.lua` (same `.luarc.json` setup as [Editor Setup](#editor-setup-autocompletion--type-hints), listing both `unciv-api.lua` and `lua-map-api.lua` under `workspace.library`); the definitions are generated from the engine code, so they never go stale.

## Notes

- **Sandbox**: The Lua environment is restricted. `os.*`, `io.*`, `coroutine.*`, `require`, `debug.*`, `string.dump`, the `package` library (and its `package.loaded` table), file operations, and metatable operations are disabled. Scripts that try to access them fail with an error.
- **Runaway loops are cut off**: Every script load and every function call has an instruction budget (about a second of CPU). An accidental `while true do end` is interrupted with a "budget exceeded" error instead of freezing the game.
- **Performance**: Lua calls have cross-language overhead. Avoid high-frequency trigger paths. Prefer JSON uniques for simple stat modifiers.
- **Snapshot properties**: context-table *properties* (e.g. `city.name`, `unit.health`, `civ.name`) are snapshots taken when the table is built. After a write (e.g. `city.setName(...)`, `unit.setHealth(...)`), confirm the result with a query method (`civ.getCityNames()`, `unit.getDamage()`, ...) - the property keeps its old value until a new ctx is built.
- **Persistent storage**: `ctx.store` values are stored as strings in the save file. Use `tostring()`/`tonumber()` for non-string data.
- **Pathfinding cost**: `unit.findPathTo()` uses A* multi-turn pathfinding and may be expensive on large maps. Avoid calling it in high-frequency loops.
- **Function names**: Must be unique within a mod. Use `modName:functionName` for cross-mod references. Names must match `[a-zA-Z_][a-zA-Z0-9_]*` (optionally prefixed with `modName:`); no dashes or special characters.
- **Return value**: Functions should return `true` (success) or `false` (failure). Returning `false` may cause the trigger to be treated as inactive. Note that Lua's truthiness applies: `return 0` and `return nil` count as **failure**, while `return ""` or `return 1` count as success.

## Checking Your Mod

Unciv checks your Lua scripts in several layers:

1. **Mod checker / mod manager (in-game)**: When a mod is loaded, its `scripts/*.lua` files are compiled; syntax errors show up as red errors and a missing `modName:functionName` reference as a yellow warning. Open **Options → Mods → Mod checker** to see the full report.
2. **Static API spelling check**: Direct calls to unknown APIs such as `ctx.civ.addGoldd(...)` are flagged with a suggestion ("did you mean: addGold, addStat…"). This runs both in the in-game mod checker and in the CLI tool below.
3. **Runtime errors**: Errors thrown while a function runs (wrong argument types, nil calls…) show a popup to the human player with the script name and line number (`Function 'x' error at line N`). Errors triggered during AI turns are recorded in the mod checker's error list instead of a popup, so you still see them. The same errors are also written to the game log.
4. **CLI (CI / offline)**: From a mod's root folder, run the desktop build as `Unciv mod-ci` (or `java -jar Unciv.jar mod-ci`). It loads the mod headlessly, runs all JSON validation **and** all Lua checks (syntax, function references, API spelling) and exits with code 1 when there are errors — suitable for a CI pipeline.

> **Tip**: write a tiny Lua function that exercises your API calls (`function test(ctx) ctx.civ.addGold(1) ... return true end`) and trigger it from a debug unique in `GlobalUniques.json` to smoke-test logic in-game.

> **New to Lua modding?** Start from the starter template at `docs/Modders/examples/LuaStarterMod/` — a complete, copy-and-rename mod with per-turn hooks, a parameter example and an API smoke test (see its README).
