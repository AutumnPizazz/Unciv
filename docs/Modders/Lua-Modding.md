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

### Unique Syntax

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

## API Reference

### civ — Civilization

**Identity properties**: `id`, `name`, `isHuman`, `isAI`, `isAlive`, `isMajorCiv`, `isCityState`, `isBarbarian`, `isSpectator`

**Methods**:

```lua
-- Stats
civ.getGold()                        -- Gold
civ.getHappiness()                   -- Happiness
civ.getStat("Science")               -- Stat reserve value
civ.getStatYield("Production")       -- Per-turn yield
civ.getGoldPerTurn()                 -- Net gold per turn
civ.getResourceAmount("Iron")        -- Stockpiled resource amount
civ.hasResource("Horses")            -- Has at least 1
civ.getEra()                         -- Era name
civ.getEraNumber()                   -- 0-based era index
civ.getCityCount()                   -- Number of cities

-- Tech
civ.isResearched("Agriculture")      -- Has researched
civ.canResearch("Philosophy")        -- Can research
civ.getResearchingTech()             -- Currently researching tech name
civ.getResearchProgress("Writing")   -- Accumulated science
civ.getTechCount()                   -- Number of researched techs
civ.getTechsResearched()             -- List of researched tech names
civ.getAvailableTechs()              -- List of available tech names
civ.grantTech("Agriculture")         -- Instantly grant a tech

-- Policies
civ.hasPolicy("Oligarchy")           -- Has adopted
civ.canAdoptPolicy()                 -- Can adopt any policy
civ.getAdoptedPolicyCount()          -- Number of adopted policies
civ.getAdoptedPolicies()             -- List of adopted policy names
civ.getAvailablePolicyBranches()     -- List of all policy branch names
civ.grantPolicy("Oligarchy")         -- Instantly adopt a policy

-- Diplomacy
civ.isAtWarWith("Greece")            -- At war
civ.hasOpenBordersWith("Greece")     -- Open borders
civ.isAlliedWith("City-State")       -- Allied city-state
civ.getDiplomaticStatus("Greece")    -- Diplomatic status string
civ.getInfluence("City-State")       -- Influence with city-state
civ.getKnownCivs()                   -- Known civilization names
civ.addInfluence("City-State", 15)   -- Add influence
civ.declareWarOn("Greece")           -- Declare war

-- Religion
civ.hasReligion()                    -- Founded a religion
civ.getReligionName()                -- Religion name
civ.getFaith()                       -- Faith amount

-- Units & Cities
civ.getCities()                      -- List of city tables
civ.getCity("Rome")                  -- City table by name
civ.getCapital()                     -- Capital city table
civ.getUnits()                       -- List of unit tables
civ.getUnitsMatching("Melee")        -- Filter units by type
civ.getUnitCount()                   -- Total unit count
civ.getSpyCount()                    -- Spy count

-- Golden Age
civ.isGoldenAge()                    -- In a golden age
civ.getGoldenAgeTurnsRemaining()     -- Turns remaining

-- Unique lookup (searches: nation + researched techs + adopted policies + current era)
civ.hasUnique("unique text")

-- Write operations
civ.addGold(500)
civ.addStat("Science", 100)
civ.addStats("+2 Gold, +3 Culture")
civ.addResource("Iron", 5)
civ.consumeResource("Iron", 2)
civ.triggerGoldenAge(10)
civ.grantFreeGreatPerson()
civ.setLeaderTitle("Emperor")
civ.getLeaderTitle()                 -- Current leader title
civ.addNotification("text")
civ.addNotificationAt("text", x, y)
civ.addFreeTech()
civ.addUnit("Warrior")
civ.addUnitAtCity("Warrior", "Rome")
civ.addUnitAtTile("Warrior", 10, 5)
civ.addRebelUnit("Barbarian Axeman")
```

### city — City

**Properties**: `id`, `name`, `isCapital`, `isCoastal`, `isPuppet`, `isBeingRazed`, `isConnectedToCapital`, `population`, `health`

```lua
-- Queries
city.getStatYield("Production")      -- Single stat yield
city.getAllYields()                  -- All yields as a table
city.hasBuilding("Library")          -- Has building
city.getBuiltBuildings()             -- List of built building names
city.getBuildingCount()              -- Total building count
city.getWonderCount()                -- Wonder count
city.getPosition()                   -- {x, y} coordinate table
city.getCenterTile()                 -- City center tile object
city.getTiles()                      -- Owned tile coordinates
city.getCurrentConstruction()        -- Currently producing item name
city.getConstructionQueue()          -- Construction queue
city.getMajorityReligion()           -- Majority religion name
city.isHolyCity()                    -- Is a holy city
city.hasUnique("unique text")        -- Check built buildings for this unique

-- Write operations
city.addPopulation(1)
city.addBuilding("Library")
city.removeBuilding("Library")
city.setProduction("Library")
city.addToQueue("Walls")
city.clearQueue()
```

### unit — Unit

**Properties**: `id`, `name`, `instanceName`, `isCivilian`, `isMilitary`, `isRanged`, `isEmbarked`, `isFortified`, `isAutomated`, `health`

**`unit.base` sub-table (read-only unit template)**:

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

```lua
-- Queries
unit.getRange()
unit.getMovement()
unit.getCurrentMovement()
unit.getXP()
unit.hasPromotion("Shock I")
unit.getPromotions()
unit.getPromotionCount()
unit.hasStatus("Fortification")
unit.getStatusTurns("Fortification")
unit.getPosition()                   -- {x, y}
unit.canMoveTo(x, y)
unit.getOwner()
unit.isOwnedBy("Rome")
unit.hasUnique("unique text")        -- Searches unit type + promotion uniques

-- Write operations
unit.healBy(25)
unit.takeDamage(30)
unit.addXP(10)
unit.addPromotion("Shock I")
unit.removePromotion("Shock I")
unit.addMovement(2)
unit.useMovement(1.5)
unit.upgrade()
unit.destroy()
unit.teleportTo(x, y)

-- Pathfinding
unit.canReach(x, y)                  -- Can reach target
unit.findPathTo(x, y)                -- Returns {{x,y}, {x,y}, ...} or nil

-- Combat
local result = unit.attackTile(x, y)
-- Returns {attackerDamage=n, defenderDamage=m} or false (not attackable)
```

### tile — Tile

**Properties**: `position`, `baseTerrain`, `isLand`, `isWater`, `resourceName`, `resourceAmount`, `improvementName`

```lua
-- Queries
tile.getX(), tile.getY()
tile.isCoast()
tile.isHill()
tile.isMountain()
tile.hasTerrainFeature("Forest")
tile.getTerrainFeatures()
tile.isImpassable()
tile.isRiver()
tile.hasResource()
tile.hasImprovement()
tile.isPillaged()
tile.getYield()                      -- Returns {Food=2, Production=1, ...}
tile.isOwned()
tile.getOwner()
tile.isOwnedBy("Rome")
tile.isCityCenter()
tile.getOwningCity()
tile.isExploredBy("Rome")
tile.hasMilitaryUnit()
tile.hasCivilianUnit()
tile.getUnits()
tile.getNeighbors()
tile.getNeighborAt(0)
tile.getTilesInDistance(3)

-- Write operations
tile.setTerrain("Plains")
tile.addTerrainFeature("Forest")
tile.removeTerrainFeature("Forest")
tile.setImprovement("Farm")
tile.removeImprovement()
tile.removeResource()
tile.setResource("Iron", 6)
tile.setRoad()
tile.setRailroad()
tile.removeRoad()
```

### game — Global

**Properties**: `turn`, `speed`, `difficulty`

```lua
-- Queries
game.getYear()
game.getCurrentPlayer()
game.getCiv("Rome")
game.getCivById("uuid...")
game.getAllCivs()
game.getAliveMajorCivs()
game.getAliveCityStates()
game.getBarbarianCiv()

-- Map
game.getTile(x, y)
game.getMapWidth()
game.getMapHeight()
game.isWrapped()
game.getTilesNear(x, y, radius)

-- Tile search
game.findTiles({
    terrain = "Grassland",      -- Base terrain
    terrainFeature = "Forest",  -- Terrain feature
    resource = "Iron",          -- Resource name
    improvement = "Farm",       -- Improvement name
    owned = false,              -- Is owned
    owner = "Rome",             -- Owner civilization name
    isCoast = true,             -- Coast tile
    isLand = true,              -- Land tile
    isWater = true,             -- Water tile
    isHill = true,              -- Hill tile
    maxResults = 10,            -- Result limit (default 500)
    centerX = 10, centerY = 15,  -- Used with maxDistance
    maxDistance = 5             -- Spatial range constraint
})

-- Ruleset queries
game.getRulesetBuildings()
game.getRulesetUnits()
game.getRulesetTechs()
game.getRulesetPolicies()
game.getRulesetEras()
game.getRulesetPromotions()
game.doesBuildingExist("Name")
game.doesUnitExist("Name")

-- Write operations
game.addGlobalNotification("text")
game.revealEntireMap("Rome")
game.revealTilesAround("Rome", x, y, radius)
```

### game.findTiles — Tile Search

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

### ctx.store — Persistent Storage

Cross-turn, cross-save key-value storage, automatically isolated per mod. All values are stored as strings:

```lua
ctx.store.set("key", "value")
local val = ctx.store.get("key", "default")
local num = tonumber(ctx.store.get("count", "0"))
```

Storage data is saved in the game file and persists across save/load cycles.

### ctx.evaluateConditional — Conditional Evaluation

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

1. **Install the extension**: in VSCode, install **Lua** by sumneko (the LuaLS language server).
2. **Add the API definitions**: copy the generated type definitions `docs/Modders/lua-api.lua` from the Unciv repo (or download the release artifact) into your mod, e.g. `MyMod/.lua-api/unciv-api.lua`.
3. **Point LuaLS at them**: create `.luarc.json` in your mod folder:

```json
{
    "runtime.version": "Lua 5.2",
    "workspace.library": [
        ".lua-api"
    ],
    "diagnostics.globals": ["ctx"]
}
```

Optionally add this to `.vscode/extensions.json` so collaborators are prompted to install the extension:

```json
{
    "recommendations": ["sumneko.lua"]
}
```

You now get: `ctx.` autocompletion (civ/city/unit/tile/game/store), method-name completion and hover docs (e.g. `ctx.civ.addGold(`), and instant red squiggles for typos like `ctx.civ.addGoldd(...)`.

> **Limitations**: the definition file is generated from the API catalog with best-effort signatures - parameter/return types are precise for common patterns and loose (`fun(...)`) for the rest. When in doubt, trust the in-game mod checker or `mod-ci` (they are authoritative), and check the function's actual behavior in-game.

## Notes

- **Sandbox**: The Lua environment is restricted. `os.*`, `io.*`, `coroutine.*`, `require`, `debug.*`, `string.dump`, the `package` library (and its `package.loaded` table), file operations, and metatable operations are disabled. Scripts that try to access them fail with an error.
- **Runaway loops are cut off**: Every script load and every function call has an instruction budget (about a second of CPU). An accidental `while true do end` is interrupted with a "budget exceeded" error instead of freezing the game.
- **Performance**: Lua calls have cross-language overhead. Avoid high-frequency trigger paths. Prefer JSON uniques for simple stat modifiers.
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
