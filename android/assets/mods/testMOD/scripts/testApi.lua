-- Lua API feature tests for the test suite

function testStore(ctx)
    -- Test set and get
    ctx.store.set("test_key", "hello_world")
    local val = ctx.store.get("test_key", "")
    if val ~= "hello_world" then
        ctx.log("FAIL: testStore - expected 'hello_world', got '" .. tostring(val) .. "'")
        return false
    end

    -- Test default value for missing key
    local def = ctx.store.get("nonexistent_key", "default_val")
    if def ~= "default_val" then
        ctx.log("FAIL: testStore - expected default 'default_val', got '" .. tostring(def) .. "'")
        return false
    end

    return true
end

function testFindTiles(ctx)
    local tiles = ctx.game.findTiles({isLand = true})
    if #tiles == 0 then
        ctx.log("FAIL: testFindTiles - no land tiles found")
        return false
    end

    -- Verify each returned tile has expected fields
    local first = tiles[1]
    if first.getX == nil or first.getY == nil then
        ctx.log("FAIL: testFindTiles - tile missing getX/getY")
        return false
    end

    return true
end

function testConditional(ctx)
    -- "when at war" should be false in a test game with one civ
    local notAtWar = ctx.evaluateConditional("when at war")
    if notAtWar then
        ctx.log("FAIL: testConditional - 'when at war' should be false in a one-civ game")
        return false
    end
    return true
end

function testCityProduction(ctx)
    local city = ctx.civ.getCapital()
    if city == nil then
        ctx.log("FAIL: testCityProduction - no capital")
        return false
    end

    city.setProduction("Warrior")
    local current = city.getCurrentConstruction()
    if current ~= "Warrior" then
        ctx.log("FAIL: testCityProduction - expected 'Warrior', got '" .. tostring(current) .. "'")
        return false
    end

    city.addToQueue("Scout")
    local queue = city.getConstructionQueue()
    if #queue < 2 then
        ctx.log("FAIL: testCityProduction - queue too short: " .. tostring(#queue))
        return false
    end

    city.clearQueue()
    return true
end

function testPathfinding(ctx)
    local unit = ctx.unit
    if unit == nil then
        ctx.log("FAIL: testPathfinding - no unit")
        return false
    end

    local pos = unit.getPosition()
    -- canReach adjacent tile
    local can = unit.canReach(pos.x + 1, pos.y)
    -- (result depends on terrain; just check it doesn't crash)

    -- findPathTo returns a table
    local path = unit.findPathTo(pos.x + 1, pos.y)
    if path == nil then
        -- unreachable may return nil, that's OK for this test
        return true
    end

    -- Verify path is a table
    if #path > 0 then
        local firstNode = path[1]
        if firstNode.x == nil then
            ctx.log("FAIL: testPathfinding - path node missing x")
            return false
        end
    end

    return true
end

function testAll(ctx)
    ctx.log("=== Lua API Test Suite ===")
    local results = {}

    results.store = testStore(ctx)
    ctx.log("testStore: " .. tostring(results.store))

    results.findTiles = testFindTiles(ctx)
    ctx.log("testFindTiles: " .. tostring(results.findTiles))

    results.conditional = testConditional(ctx)
    ctx.log("testConditional: " .. tostring(results.conditional))

    results.cityProduction = testCityProduction(ctx)
    ctx.log("testCityProduction: " .. tostring(results.cityProduction))

    results.pathfinding = testPathfinding(ctx)
    ctx.log("testPathfinding: " .. tostring(results.pathfinding))

    local allPassed = true
    for name, passed in pairs(results) do
        if not passed then
            ctx.log("FAIL: " .. name)
            allPassed = false
        end
    end

    ctx.log("=== Lua API Tests " .. (allPassed and "PASSED" or "FAILED") .. " ===")
    return allPassed
end
