-- Smoke test for the Lua API - returns true only if every exercise passes.
-- Enable it in GlobalUniques.json by uncommenting the selfCheck trigger line,
-- start a new game, then check the log for "[LuaStarterMod] test API: ..." lines.

function selfCheck(ctx)
    local civ = ctx.civ
    local results = {}

    -- Numeric API
    civ.addGold(5)
    results.gold = civ.getGold() >= 5

    -- String parameter from unique
    results.parameter = type(ctx.parameter) == "string"

    -- Civ queries
    results.era = type(civ.getEra()) == "string"
    results.cities = type(civ.getCities()) == "table"

    -- City API (guard: might not exist in every context)
    if ctx.city ~= nil then
        results.cityName = type(ctx.city.name) == "string"
        ctx.city.addToQueue("Scout")
        results.queue = #ctx.city.getConstructionQueue() >= 1
        ctx.city.clearQueue()
    else
        results.city = true -- skipped, no city in this context
    end

    -- Conditional evaluation
    results.conditional = type(ctx.evaluateConditional("when at war")) == "boolean"

    -- Persistent store
    ctx.store.set("check", "ok")
    results.store = ctx.store.get("check") == "ok"

    local allPassed = true
    for name, passed in pairs(results) do
        if not passed then
            ctx.log("test API FAIL: " .. name)
            allPassed = false
        end
    end
    ctx.log("test API: " .. (allPassed and "ALL PASSED" or "FAILED"))
    return allPassed
end
