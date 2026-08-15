-- Lua API feature tests for the extended API surface (testNewApi)
-- Exercises the API additions: civ/city/unit/tile/game queries & writes, ctx.random.

function testNewApi(ctx)
    ctx.log("=== Extended Lua API Test Suite ===")
    local civ = ctx.civ
    local game = ctx.game
    local city = ctx.city
    local unit = ctx.unit
    local tile = ctx.tile
    local results = {}

    local check = function(name, cond, extra)
        if not cond then
            results[name] = false
            if extra ~= nil then
                ctx.log("FAIL: " .. name .. " - " .. tostring(extra))
            end
        else
            results[name] = true
        end
    end

    -- civ: identity & rankings
    check("civ.getNation", type(civ.getNation()) == "string" and #civ.getNation() > 0)
    check("civ.getLeaderName", type(civ.getLeaderName()) == "string")
    check("civ.getScore", type(civ.getScore()) == "number")
    check("civ.getForce", type(civ.getForce()) == "number")

    -- civ: per-turn yields
    check("civ.getSciencePerTurn", type(civ.getSciencePerTurn()) == "number")
    check("civ.getCulturePerTurn", type(civ.getCulturePerTurn()) == "number")
    check("civ.getFoodPerTurn", type(civ.getFoodPerTurn()) == "number")
    check("civ.getProductionPerTurn", type(civ.getProductionPerTurn()) == "number")

    -- civ: tech & policy costs
    check("civ.getTechCost", civ.getTechCost("Agriculture") > 0)
    check("civ.getCultureNeededForNextPolicy", type(civ.getCultureNeededForNextPolicy()) == "number")

    -- civ: cities
    check("civ.getTotalPopulation", civ.getTotalPopulation() >= 1)
    check("civ.getCityNames", type(civ.getCityNames()) == "table" and #civ.getCityNames() >= 1)
    check("civ.getWondersBuilt", type(civ.getWondersBuilt()) == "table")

    -- civ: resources & diplomacy
    check("civ.getResourceStockpiles", type(civ.getResourceStockpiles()) == "table")
    check("civ.getDiplomaticStatuses", type(civ.getDiplomaticStatuses()) == "table")
    check("civ.getProximityTo", type(civ.getProximityTo(civ.name)) == "string")
    check("civ.hasEmbassyWith", civ.hasEmbassyWith(civ.name) == false)
    check("civ.makePeaceWith", civ.makePeaceWith(civ.name) == nil) -- no-op in peace, must not crash

    -- civ: spies
    check("civ.getSpies", type(civ.getSpies()) == "table")
    civ.addSpy()
    check("civ.addSpy", #civ.getSpies() >= 1)

    -- civ: setGold
    civ.setGold(200)
    check("civ.setGold", civ.getGold() == 200)

    check("city.getFood", type(city.getFood()) == "number")
    check("city.getFoodSurplus", type(city.getFoodSurplus()) == "number")
    check("city.getFoodStorage", type(city.getFoodStorage()) == "number")
    check("city.getFoodNeeded", city.getFoodNeeded() > 0)
    city.addFood(10)
    check("city.addFood", city.getFoodStorage() >= 10)

    -- city: production（先设置生产项目，快照属性请用查询方法验证）
    city.setProduction("Warrior")
    check("city.getProductionCost", city.getProductionCost() > 0)
    city.addProduction(5)
    check("city.addProduction", city.getProductionProgress() >= 5)
    check("city.getTurnsToCompletion", type(city.getTurnsToCompletion()) == "number")

    -- city: military & population
    check("city.getGarrisonedUnit", city.getGarrisonedUnit() == nil or type(city.getGarrisonedUnit()) == "table")
    check("city.getStrength", type(city.getStrength()) == "number")
    check("city.getSpecialistCount", type(city.getSpecialistCount()) == "number")
    check("city.getUnemployedCount", type(city.getUnemployedCount()) == "number")
    check("city.getBuiltWonders", type(city.getBuiltWonders()) == "table")
    check("city.isInResistance", city.isInResistance() == false)

    -- city: writes（属性是调用时的快照，写入后用动态查询验证）
    city.setPopulation(3)
    check("city.setPopulation", civ.getTotalPopulation() >= 3)
    city.addHealth(10)
    check("city.addHealth", city.health > 0)
    city.setName("LuaCity")
    local foundName = false
    for _, n in ipairs(civ.getCityNames()) do
        if n == "LuaCity" then foundName = true end
    end
    check("city.setName", foundName)
    if game.doesBuildingExist("Test Building") then
        city.addBuilding("Test Building")
        city.sellBuilding("Test Building")
        check("city.sellBuilding", city.hasBuilding("Test Building") == false)
    else
        check("city.sellBuilding", true) -- skipped, building not in ruleset
    end

    check("unit.getMaxHealth", unit.getMaxHealth() == 100)
    check("unit.getDamage", unit.getDamage() == 0)
    check("unit.getAttacksLeft", type(unit.getAttacksLeft()) == "number")
    check("unit.getVisibilityRange", unit.getVisibilityRange() >= 1)
    check("unit.canAttack", type(unit.canAttack()) == "boolean")
    check("unit.canPillage", type(unit.canPillage()) == "boolean")
    check("unit.isInEnemyTerritory", unit.isInEnemyTerritory() == false)
    check("unit.isInFriendlyTerritory", unit.isInFriendlyTerritory() == true)
    check("unit.isGreatPerson", unit.isGreatPerson() == false)
    check("unit.getReligionDisplayName", unit.getReligionDisplayName() == "")

    -- unit: writes（health 是快照，用动态 getDamage 验证）
    unit.setHealth(50)
    check("unit.setHealth", unit.getDamage() == 50)
    unit.setXP(10)
    check("unit.setXP", unit.getXP() == 10)
    unit.setStatus("TestStatus", 3)
    check("unit.setStatus", unit.hasStatus("TestStatus") and unit.getStatusTurns("TestStatus") == 3)
    unit.setAttacksLeft(0)
    check("unit.setAttacksLeft", unit.getAttacksLeft() == 0)
    unit.fortify()
    check("unit.fortify", unit.getAction() == "Fortify")

    -- unit: moveByPath along a found path
    local pos = unit.getPosition()
    local path = unit.findPathTo(pos.x, pos.y)
    local steps = unit.moveByPath(path or {})
    check("unit.moveByPath", type(steps) == "number")

    check("tile.hasRoad", tile.hasRoad() == false)
    check("tile.hasRailroad", tile.hasRailroad() == false)
    check("tile.hasNaturalWonder", tile.hasNaturalWonder() == false)
    check("tile.getNaturalWonder", tile.getNaturalWonder() == "")
    check("tile.isAdjacentToCoast", type(tile.isAdjacentToCoast()) == "boolean")

    -- tile: territory & geometry
    check("tile.isFriendlyTerritory", tile.isFriendlyTerritory(civ.name) == true)
    check("tile.isEnemyTerritory", tile.isEnemyTerritory(civ.name) == false)
    check("tile.getDistanceTo", type(tile.getDistanceTo(tile.getX(), tile.getY())) == "number")
    check("tile.isAdjacentTo", type(tile.isAdjacentTo(tile.getX(), tile.getY())) == "boolean")
    tile.setExplored(civ.name, true)
    check("tile.setExplored", tile.isExploredBy(civ.name) == true)

    check("game.getCivNames", type(game.getCivNames()) == "table" and #game.getCivNames() >= 1)
    check("game.getHumanCivs", type(game.getHumanCivs()) == "table")
    check("game.getCurrentPlayerCiv", game.getCurrentPlayerCiv() == nil or type(game.getCurrentPlayerCiv()) == "table")

    -- game: map info
    check("game.getMapName", type(game.getMapName()) == "string")
    check("game.getMapType", type(game.getMapType()) == "string")

    -- game: ruleset lists
    check("game.getEraNames", #game.getEraNames() > 0)
    check("game.getVictoryTypes", type(game.getVictoryTypes()) == "table")
    check("game.getMods", type(game.getMods()) == "table")
    check("game.getBaseRuleset", type(game.getBaseRuleset()) == "string")
    check("game.getRulesetTerrains", #game.getRulesetTerrains() > 0)
    check("game.getRulesetResources", type(game.getRulesetResources()) == "table")
    check("game.getRulesetImprovements", type(game.getRulesetImprovements()) == "table")
    check("game.getRulesetNations", #game.getRulesetNations() > 0)
    check("game.getRulesetReligions", type(game.getRulesetReligions()) == "table")
    check("game.getRulesetBeliefs", type(game.getRulesetBeliefs()) == "table")
    check("game.getRulesetEvents", type(game.getRulesetEvents()) == "table")
    check("game.getRulesetNaturalWonders", type(game.getRulesetNaturalWonders()) == "table")
    check("game.getRulesetUnitTypes", #game.getRulesetUnitTypes() > 0)

    -- game: existence checks
    check("game.doesTechExist", game.doesTechExist("Agriculture") == true)
    check("game.doesTechExist.neg", game.doesTechExist("NoSuchTech") == false)
    check("game.doesPolicyExist", type(game.doesPolicyExist("Oligarchy")) == "boolean")
    check("game.doesEraExist", game.doesEraExist("Ancient era") == true)
    check("game.doesPromotionExist", type(game.doesPromotionExist("Shock I")) == "boolean")
    check("game.doesTerrainExist", game.doesTerrainExist("Grassland") == true)
    check("game.doesResourceExist", type(game.doesResourceExist("Iron")) == "boolean")
    check("game.doesImprovementExist", game.doesImprovementExist("Farm") == true)
    check("game.doesNationExist", type(game.doesNationExist("Rome")) == "boolean")
    check("game.doesBeliefExist", type(game.doesBeliefExist("God King")) == "boolean")
    check("game.doesEventExist", type(game.doesEventExist("Test Event")) == "boolean")

    local r = ctx.random()
    check("ctx.random", type(r) == "number" and r >= 0 and r < 1)
    local ri = ctx.randomInt(1, 10)
    check("ctx.randomInt", type(ri) == "number" and ri >= 1 and ri <= 10)

    local allPassed = true
    local failed = {}
    for name, passed in pairs(results) do
        if not passed then
            ctx.log("FAIL: " .. name)
            failed[#failed + 1] = name
            allPassed = false
        end
    end
    ctx.store.set("testNewApi_failures", table.concat(failed, ","))
    ctx.log("=== Extended Lua API Tests " .. (allPassed and "PASSED" or "FAILED") .. " ===")
    return allPassed
end
