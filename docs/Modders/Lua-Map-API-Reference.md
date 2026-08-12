<!-- 本文件由 LuaApiReferenceWriter / LuaMapApiReferenceWriter 自动生成，请勿手动编辑。 -->
<!-- Generated from the LuaApiDocs / LuaMapGenApiDocs tables - never edit by hand. -->
<!-- 重新生成 / Regenerate with: ./gradlew desktop:generateDocs -->

# Lua Map-Script API Reference

This page is generated from the game code and lists the **generation-only** API available
inside `GenerateMap(ctx)` map scripts - a separate, sandboxed context with no civs, cities
or units. It can never drift from the implementation (see [Lua Mod Scripts](Lua-Modding.md)
for how map scripts work).

For editor autocompletion point the LuaLS language server at `lua-map-api.lua` (same
`.luarc.json` setup as the main API, listing both files under `workspace.library`).

> **Note on signatures**: `opts?` parameters are optional tables (see the type
> definitions in `lua-map-api.lua` for their fields); `|nil` means the value may be absent.

## ctx — Map-Script Context

**Properties**:

`params`, `seed`, `map`

**Query methods**:

```lua
ctx.perlin(x, y, seed, opts?) -- Perlin noise in roughly [-1, 1]
ctx.random()                  -- Seeded deterministic random in [0, 1)
ctx.randomInt(min, max)       -- Seeded deterministic random integer in [min, max] (inclusive)
```

**Write methods**:

```lua
ctx.log(msg) -- Write debug output to Unciv's log
```

## ctx.params — Map Parameters

**Properties**:

`name`, `type`, `shape`, `worldWrap`, `mirroring`, `symmetryMode`, `size`, `bounds`, `waterThreshold`, `temperatureintensity`, `temperatureShift`, `vegetationRichness`, `rareFeaturesRichness`, `resourceRichness`, `elevationExponent`, `tilesPerBiomeArea`, `maxCoastExtension`, `noRuins`, `noNaturalWonders`, `mapResources`, `strategicBalance`, `legendaryStart`, `mods`, `baseRuleset`

## ctx.params.size — Map Size

**Properties**:

`name`, `radius`, `width`, `height`

## ctx.params.bounds — Tile Bounds

**Properties**:

`minX`, `minY`, `maxX`, `maxY`

## ctx.map — Map Manipulation

**Query methods**:

```lua
map.getWidth()                      -- Map width
map.getHeight()                     -- Map height
map.getRadius()                     -- Map radius
map.getShape()                      -- Map shape
map.isWrapped()                     -- Map wraps around
map.getTile(x, y)                   -- Tile at coordinates (nil when out of bounds)
map.getAllTiles()                   -- All tiles (prefer over iterating coordinates)
map.getStartingLocations()          -- List of {x, y, nation} starting locations
map.floodFill(x, y, terrainFilter?) -- BFS-connected tiles (optionally filtered by terrain)
```

**Write methods**:

```lua
map.assignContinents()                            -- Assign continent numbers
map.addStartingLocation(x, y, nationName?)        -- Add a starting location (nationName optional)
map.clearStartingLocations()                      -- Clear all starting locations
map.setTransients()                               -- Set transient tile data (run before other helpers)
map.normalizeTiles()                              -- Normalize every tile against the ruleset
map.generateClimate()                             -- Assign temperature/humidity by latitude
map.spreadCoasts(maxExtension?)                   -- Spread coasts (default: params.maxCoastExtension)
map.generateMountains(elevationExponent?)         -- Generate mountain ranges
map.generateRivers()                              -- Generate rivers
map.generateIce()                                 -- Generate polar ice
map.convertTerrains()                             -- Convert terrains
map.normalizeStartPlot(x, y, opts?)               -- Balance a start plot (food/production/luxuries/hills)
map.distributeLuxuries(opts?, minDistance?)       -- Distribute luxury resources
map.distributeStrategics(opts?, radius?)          -- Distribute strategic resources
map.strategicBalanceStarts(opts?, iron?, radius?) -- Balance strategic resources near starts
```

## MapGen Tile — Tile

**Properties**:

`position`, `baseTerrain`, `isLand`, `isWater`, `isCoast`, `temperature`, `humidity`, `resourceName`, `resourceAmount`, `improvementName`

**Query methods**:

```lua
tile.getX()                         -- X coordinate
tile.getY()                         -- Y coordinate
tile.isHill()                       -- Hill tile
tile.isMountain()                   -- Mountain tile
tile.isImpassable()                 -- Impassable tile
tile.hasTerrainFeature(featureName) -- Has terrain feature
tile.getTerrainFeatures()           -- Terrain feature names
tile.getTemperature()               -- Temperature value
tile.getHumidity()                  -- Humidity value
tile.getLatitude()                  -- Latitude (0 at the equator, grows poleward)
tile.getLongitude()                 -- Longitude
tile.getContinent()                 -- Continent number (after assignContinents)
tile.hasResource()                  -- Has a resource
tile.hasImprovement()               -- Has an improvement
tile.isRiver()                      -- Tile has a river
tile.isNaturalWonder()              -- Is a natural wonder
tile.isAdjacentToFreshWater()       -- Adjacent to fresh water
tile.getBaseYield(stat)             -- Base tile yield for a stat (no civ view)
tile.getNeighbors()                 -- Adjacent tiles
tile.getTilesInDistance(radius)     -- All tiles within range
```

**Write methods**:

```lua
tile.setTemperature(v)                 -- Set temperature
tile.setHumidity(v)                    -- Set humidity
tile.setTerrain(terrainName)           -- Change base terrain
tile.addTerrainFeature(featureName)    -- Add terrain feature
tile.removeTerrainFeature(featureName) -- Remove terrain feature
tile.removeAllTerrainFeatures()        -- Remove all terrain features
tile.setResource(resourceName, amount) -- Set resource
tile.removeResource()                  -- Remove resource
tile.setImprovement(improvementName)   -- Set improvement
tile.removeImprovement()               -- Remove improvement
tile.setRoad()                         -- Build a road
tile.setRailroad()                     -- Build a railroad
tile.removeRoad()                      -- Remove road/railroad
tile.setNaturalWonder(wonderName)      -- Set natural wonder (empty string clears)
```
