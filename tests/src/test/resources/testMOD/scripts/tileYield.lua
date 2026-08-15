-- Tile yield hook tests for the test suite

function testTileYield(ctx)
    -- Double every stat of the engine-computed yield
    local result = {}
    for k, v in pairs(ctx.tileStats) do
        result[k] = v * 2
    end
    return result
end

function testTileYieldOverride(ctx)
    -- Only food is overridden, all other stats keep their engine values
    return { food = 99 }
end

function testTileYieldNil(ctx)
    -- Returning nil leaves the yield unchanged
    return nil
end
