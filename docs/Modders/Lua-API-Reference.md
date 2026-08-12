<!-- 本文件由 LuaApiReferenceWriter / LuaMapApiReferenceWriter 自动生成，请勿手动编辑。 -->
<!-- Generated from the LuaApiDocs / LuaMapGenApiDocs tables - never edit by hand. -->
<!-- 重新生成 / Regenerate with: ./gradlew desktop:generateDocs -->

# Lua API Reference

This page is generated from the game code and lists every function and property of the
Lua context tables (`ctx`). It can never drift from the implementation - when a new API is
added, this page is regenerated together with the type definitions (`lua-api.lua`).

For tutorials, triggers and editor setup see [Lua Mod Scripts](Lua-Modding.md).

> **Note on signatures**: types follow the EmmyLua style used by the LuaLS language server
> (`lua-api.lua`); `|nil` means the value may be absent, `string[]` a list of strings.

## civ — Civilization

**Properties**:

`id`, `name`, `isHuman`, `isAI`, `isAlive`, `isMajorCiv`, `isCityState`, `isBarbarian`, `isSpectator`

**Query methods**:

```lua
civ.getNation()                     -- Nation name
civ.getLeaderName()                 -- Leader name
civ.getScore()                      -- Current score
civ.getForce()                      -- Military might ranking value
civ.getGold()                       -- Gold
civ.getHappiness()                  -- Happiness
civ.getStat(stat)                   -- Stat reserve value
civ.getStatYield(stat)              -- Per-turn yield
civ.getGoldPerTurn()                -- Net gold per turn
civ.getSciencePerTurn()             -- Net science per turn
civ.getCulturePerTurn()             -- Net culture per turn
civ.getFoodPerTurn()                -- Net food per turn
civ.getProductionPerTurn()          -- Net production per turn
civ.getResourceAmount(resourceName) -- Stockpiled resource amount
civ.hasResource(resourceName)       -- Has at least 1
civ.getResourceStockpiles()         -- Table {resourceName = amount}
civ.getEra()                        -- Era name
civ.getEraNumber()                  -- 0-based era index
civ.isResearched(techName)          -- Has researched
civ.canResearch(techName)           -- Can research
civ.getResearchingTech()            -- Currently researching tech name
civ.getResearchProgress(techName)   -- Accumulated science
civ.getTechCount()                  -- Number of researched techs
civ.getTechsResearched()            -- List of researched tech names
civ.getAvailableTechs()             -- List of available tech names
civ.getTechCost(techName)           -- Base research cost of a tech
civ.hasPolicy(policyName)           -- Has adopted
civ.canAdoptPolicy()                -- Can adopt any policy
civ.getAdoptedPolicyCount()         -- Number of adopted policies
civ.getAdoptedPolicies()            -- List of adopted policy names
civ.getAvailablePolicyBranches()    -- List of all policy branch names
civ.getCultureNeededForNextPolicy() -- Culture needed for the next policy
civ.isAtWarWith(civName)            -- At war
civ.hasOpenBordersWith(civName)     -- Open borders
civ.isAlliedWith(civName)           -- Allied city-state
civ.getDiplomaticStatus(civName)    -- Diplomatic status string
civ.getDiplomaticStatuses()         -- Table {civName = statusName} for all known civs
civ.getProximityTo(civName)         -- Proximity string (None/Neighbors/Close/Far)
civ.hasEmbassyWith(civName)         -- Embassy established either way
civ.getInfluence(civName)           -- Influence with city-state
civ.getKnownCivs()                  -- Known civilization names
civ.hasReligion()                   -- Founded a religion
civ.getReligionName()               -- Religion name
civ.getFaith()                      -- Faith amount
civ.getCities()                     -- List of city tables
civ.getCity(cityName)               -- City table by name
civ.getCapital()                    -- Capital city table
civ.getCityCount()                  -- Number of cities
civ.getCityNames()                  -- List of city names
civ.getTotalPopulation()            -- Sum of all city populations
civ.getWondersBuilt()               -- List of built wonder names
civ.getUnits()                      -- List of unit tables
civ.getUnitsMatching(filter)        -- Filter units by type
civ.getUnitCount()                  -- Total unit count
civ.isGoldenAge()                   -- In a golden age
civ.getGoldenAgeTurnsRemaining()    -- Turns remaining
civ.getSpyCount()                   -- Spy count
civ.getSpies()                      -- List of {name, rank, action, location} tables
civ.getLeaderTitle()                -- Current leader title
civ.hasUnique(uniqueText)           -- Searches: nation + researched techs + adopted policies + current era
```

**Write methods**:

```lua
civ.grantTech(techName)                   -- Instantly grant a tech
civ.discoverTech(techName)                -- Instantly discover a tech (same as grantTech)
civ.grantPolicy(policyName)               -- Instantly adopt a policy
civ.addInfluence(civName, amount)         -- Add influence
civ.declareWarOn(civName)                 -- Declare war
civ.makePeaceWith(civName)                -- Sign peace (no-op when not at war)
civ.addSpy()                              -- Add a spy
civ.addGold(amount)                       -- Add gold
civ.setGold(amount)                       -- Set gold to exactly this amount
civ.addStat(stat, amount)                 -- Add to a stat reserve
civ.addStats(statsText)                   -- Apply a stats text like "+2 Gold, +3 Culture"
civ.addResource(resourceName, amount)     -- Add a strategic resource
civ.consumeResource(resourceName, amount) -- Consume a strategic resource
civ.triggerGoldenAge(turns)               -- Start a golden age for N turns (omit for default length)
civ.grantFreeGreatPerson()                -- Grant a free great person
civ.setLeaderTitle(title)                 -- Set the leader title
civ.addNotification(text)                 -- Popup an in-game notification
civ.addNotificationAt(text, x, y)         -- Notification that jumps to a map position
civ.addFreeTech()                         -- Grant a free tech
civ.addUnit(unitName)                     -- Spawn a unit for this civ
civ.addUnitAtCity(unitName, cityName)     -- Spawn a unit at a city
civ.addUnitAtTile(unitName, x, y)         -- Spawn a unit at a tile
civ.addRebelUnit(unitName)                -- Spawn a rebel unit
```

## city — City

**Properties**:

`id`, `name`, `isCapital`, `isCoastal`, `isPuppet`, `isBeingRazed`, `isConnectedToCapital`, `population`, `health`

**Query methods**:

```lua
city.getStatYield(stat)        -- Single stat yield
city.getAllYields()            -- All yields as a table
city.getFood()                 -- Current food yield
city.getFoodSurplus()          -- Net food per turn (negative when starving)
city.getFoodStorage()          -- Stored food toward growth
city.getFoodNeeded()           -- Food needed for the next population
city.getProductionProgress()   -- Production already invested in current construction
city.getProductionCost()       -- Total production cost of current construction
city.getTurnsToCompletion()    -- Estimated turns to finish current construction
city.getGarrisonedUnit()       -- Garrison unit table (or nil)
city.getStrength()             -- City combat strength
city.getSpecialistCount()      -- Assigned specialists
city.getUnemployedCount()      -- Free (unassigned) population
city.getBuiltWonders()         -- Built wonder names
city.isInResistance()          -- City is in resistance after conquest
city.hasBuilding(buildingName) -- Has building
city.getBuiltBuildings()       -- List of built building names
city.getBuildingCount()        -- Total building count
city.getWonderCount()          -- Wonder count
city.getPosition()             -- {x, y} coordinate table
city.getCenterTile()           -- City center tile object
city.getTiles()                -- Owned tile coordinates
city.getCurrentConstruction()  -- Currently producing item name
city.getConstructionQueue()    -- Construction queue
city.getMajorityReligion()     -- Majority religion name
city.isHolyCity()              -- Is a holy city
city.hasUnique(uniqueText)     -- Check built buildings for this unique
```

**Write methods**:

```lua
city.addPopulation(amount)        -- Add population
city.setPopulation(count)         -- Set population exactly (min 1)
city.addFood(amount)              -- Add stored food
city.addProduction(amount)        -- Add production to current construction
city.addHealth(amount)            -- Heal the city
city.setName(newName)             -- Rename the city
city.addBuilding(buildingName)    -- Free-build a building
city.removeBuilding(buildingName) -- Remove a building
city.sellBuilding(buildingName)   -- Sell a building for gold
city.setProduction(itemName)      -- Set the current construction item
city.addToQueue(itemName)         -- Append to the construction queue
city.clearQueue()                 -- Clear the whole construction queue
```

## unit — Unit

**Properties**:

`id`, `name`, `instanceName`, `isCivilian`, `isMilitary`, `isRanged`, `isEmbarked`, `isFortified`, `isAutomated`, `base`, `health`

**Query methods**:

```lua
unit.getRange()                  -- Attack range
unit.getEraNumber()              -- 0-based era index of the unit's owner civilization
unit.getMovement()               -- Maximum movement
unit.getCurrentMovement()        -- Remaining movement
unit.getXP()                     -- Experience points
unit.getMaxHealth()              -- 100 (max HP)
unit.getDamage()                 -- Max health minus current health
unit.getAttacksLeft()            -- Attacks remaining this turn
unit.getVisibilityRange()        -- Sight range in tiles
unit.getAction()                 -- Current action string ("Fortify", "moveTo x,y", ...)
unit.canAttack()                 -- Can attack this turn
unit.canPillage()                -- Current tile has something to pillage
unit.isInEnemyTerritory()        -- Standing in enemy territory
unit.isInFriendlyTerritory()     -- Standing in own territory
unit.isGreatPerson()             -- Is a great person
unit.getReligionDisplayName()    -- Religion of this unit ("" if none)
unit.hasPromotion(promotionName) -- Has promotion
unit.hasUnique(uniqueText)       -- Searches unit type + promotion uniques
unit.getPromotions()             -- Promotion names
unit.getPromotionCount()         -- Number of promotions
unit.hasStatus(statusName)       -- Has status
unit.getStatusTurns(statusName)  -- Status turns remaining
unit.getPosition()               -- {x, y} coordinate table
unit.canMoveTo(x, y)             -- Can move to tile
unit.getOwner()                  -- Owner civilization name
unit.isOwnedBy(civName)          -- Owned by civilization
unit.findPathTo(x, y)            -- Returns path {{x,y}, {x,y}, ...} or nil (uses A* multi-turn pathfinding)
unit.canReach(x, y)              -- Can reach target tile
```

**Write methods**:

```lua
unit.healBy(amount)                 -- Heal
unit.takeDamage(amount)             -- Take damage
unit.addXP(amount)                  -- Add experience
unit.setXP(amount)                  -- Set XP exactly
unit.setHealth(amount)              -- Set health exactly (clamped)
unit.addPromotion(promotionName)    -- Add promotion
unit.removePromotion(promotionName) -- Remove promotion
unit.addMovement(amount)            -- Add movement
unit.useMovement(amount)            -- Spend movement
unit.setStatus(statusName, turns)   -- Apply a unit status for N turns
unit.setAttacksLeft(count)          -- Set attacks remaining
unit.fortify()                      -- Fortify (action = "Fortify")
unit.moveByPath(path)               -- Move along a findPathTo() path; returns steps taken
unit.upgrade()                      -- Upgrade for free
unit.destroy()                      -- Destroy the unit
unit.attackTile(x, y)               -- Attack a unit/city at a tile; returns {attackerDamage=n, defenderDamage=m} or false (not attackable)
unit.teleportTo(x, y)               -- Teleport to a tile
```

## tile — Tile

**Properties**:

`position`, `baseTerrain`, `isLand`, `isWater`, `isCoast`, `isHill`, `isMountain`, `resourceName`, `resourceAmount`, `improvementName`

**Query methods**:

```lua
tile.getX()                         -- X coordinate
tile.getY()                         -- Y coordinate
tile.hasTerrainFeature(featureName) -- Has terrain feature
tile.getTerrainFeatures()           -- Terrain feature names
tile.isImpassable()                 -- Impassable tile
tile.isRiver()                      -- Tile has a river
tile.isAdjacentToCoast()            -- Adjacent to coast
tile.hasRoad()                      -- Has a road
tile.hasRailroad()                  -- Has a railroad
tile.hasNaturalWonder()             -- Has a natural wonder
tile.getNaturalWonder()             -- Natural wonder name ("" if none)
tile.hasResource()                  -- Has a resource
tile.hasImprovement()               -- Has an improvement
tile.isPillaged()                   -- Tile is pillaged
tile.getYield()                     -- Yield table {Food=2, Production=1, ...}
tile.isOwned()                      -- Owned by some civ
tile.getOwner()                     -- Owner civilization name
tile.isOwnedBy(civName)             -- Owned by civilization
tile.isFriendlyTerritory(civName)   -- Friendly territory for the civ
tile.isEnemyTerritory(civName)      -- Enemy territory for the civ
tile.isCityCenter()                 -- City center tile
tile.getOwningCity()                -- Owning city name
tile.isExploredBy(civName)          -- Explored by the civ
tile.getDistanceTo(x, y)            -- Aerial distance in tiles (-1 if out of map)
tile.isAdjacentTo(x, y)             -- Adjacent to tile
tile.hasMilitaryUnit()              -- Has a military unit
tile.hasCivilianUnit()              -- Has a civilian unit
tile.getUnits()                     -- Units on this tile
tile.getNeighbors()                 -- Adjacent tiles
tile.getNeighborAt(direction)       -- Tile in a direction (0-5)
tile.getTilesInDistance(radius)     -- All tiles within range
```

**Write methods**:

```lua
tile.setExplored(civName, explored)    -- Set/clear exploration for a civ
tile.setTerrain(terrainName)           -- Change base terrain
tile.addTerrainFeature(featureName)    -- Add terrain feature
tile.removeTerrainFeature(featureName) -- Remove terrain feature
tile.setImprovement(improvementName)   -- Set improvement
tile.removeImprovement()               -- Remove improvement
tile.removeResource()                  -- Remove resource
tile.setResource(resourceName, amount) -- Set resource
tile.setRoad()                         -- Build a road
tile.setRailroad()                     -- Build a railroad
tile.removeRoad()                      -- Remove road/railroad
```

## game — Global

**Properties**:

`turn`, `speed`, `difficulty`

**Query methods**:

```lua
game.getYear()                             -- Current year
game.getCurrentPlayer()                    -- Current player civilization name
game.getCurrentPlayerCiv()                 -- Current player civ table (or nil)
game.getCiv(civName)                       -- Civ table by name
game.getCivById(id)                        -- Civ table by ID
game.getAllCivs()                          -- All civ tables
game.getCivNames()                         -- All civilization names
game.getHumanCivs()                        -- Human civilizations
game.getAliveMajorCivs()                   -- Alive major civilizations
game.getAliveCityStates()                  -- Alive city-states
game.getBarbarianCiv()                     -- Barbarian civilization
game.getTile(x, y)                         -- Tile table at coordinates
game.findTiles(criteria)                   -- Search the map by criteria table (see the findTiles section)
game.getMapWidth()                         -- Map width
game.getMapHeight()                        -- Map height
game.getMapName()                          -- Map name
game.getMapType()                          -- Map type
game.isWrapped()                           -- Map wraps around
game.getTilesNear(x, y, radius)            -- Tiles within radius
game.getEraNames()                         -- All era names
game.getVictoryTypes()                     -- Enabled victory types
game.getMods()                             -- Enabled mod names
game.getBaseRuleset()                      -- Base ruleset name
game.getRulesetBuildings()                 -- All building names
game.getRulesetUnits()                     -- All unit names
game.getRulesetTechs()                     -- All tech names
game.getRulesetPolicies()                  -- All policy names
game.getRulesetEras()                      -- All era names
game.getRulesetPromotions()                -- All promotion names
game.getRulesetTerrains()                  -- Terrain names
game.getRulesetResources()                 -- Resource names
game.getRulesetImprovements()              -- Improvement names
game.getRulesetNations()                   -- Nation names
game.getRulesetReligions()                 -- Religion names
game.getRulesetBeliefs()                   -- Belief names
game.getRulesetEvents()                    -- Event names
game.getRulesetNaturalWonders()            -- Natural wonder names
game.getRulesetUnitTypes()                 -- Unit type names
game.doesBuildingExist(buildingName)       -- Building exists in the ruleset
game.doesUnitExist(unitName)               -- Unit exists in the ruleset
game.doesTechExist(techName)               -- Tech exists in the ruleset
game.doesPolicyExist(policyName)           -- Policy exists in the ruleset
game.doesEraExist(eraName)                 -- Era exists in the ruleset
game.doesPromotionExist(promotionName)     -- Promotion exists in the ruleset
game.doesTerrainExist(terrainName)         -- Terrain exists in the ruleset
game.doesResourceExist(resourceName)       -- Resource exists in the ruleset
game.doesImprovementExist(improvementName) -- Improvement exists in the ruleset
game.doesNationExist(nationName)           -- Nation exists in the ruleset
game.doesBeliefExist(beliefName)           -- Belief exists in the ruleset
game.doesEventExist(eventName)             -- Event exists in the ruleset
```

**Write methods**:

```lua
game.addGlobalNotification(text)              -- Notify all human players
game.revealEntireMap(civName)                 -- Reveal the whole map for a civ
game.revealTilesAround(civName, x, y, radius) -- Reveal tiles around a position for a civ
```

## ctx — Context

**Properties**:

`parameter`, `city`, `unit`, `tile`, `civ`, `game`, `store`

**Query methods**:

```lua
ctx.count(expr)                    -- Evaluate a Countable expression at runtime
ctx.evaluateConditional(condition) -- Evaluate a conditional string, returns boolean
ctx.random()                       -- Deterministic random in [0, 1) - same call sequence on the same game state yields the same values
ctx.randomInt(min, max)            -- Deterministic random integer in [min, max] (inclusive)
```

**Write methods**:

```lua
ctx.log(msg) -- Write debug output to Unciv's log
```

## ctx.store — Persistent Storage

**Query methods**:

```lua
store.get(key, default) -- Read a value; returns default when absent
```

**Write methods**:

```lua
store.set(key, value) -- Store a value (all values are stored as strings)
```
