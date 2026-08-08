-- Test map script: generates a simple Pangaea-like world using the Lua map API

function GetMapScriptInfo()
    return {
        name = "测试脚本地图",
        description = "使用 Perlin 噪声生成一块大陆，带海岸、山脉、河流和冰层。用于验证 Lua 地图系统。"
    }
end

function GenerateMap(ctx)
    local w = ctx.map.getWidth()
    local h = ctx.map.getHeight()

    -- 1. 用 Perlin 噪声生成陆地/海洋
    for x = 0, w - 1 do
        for y = 0, h - 1 do
            local tile = ctx.map.getTile(x, y)
            if tile ~= nil then
                local n = ctx.perlin(x, y, 42.0)
                if n > 0.0 then
                    tile.setTerrain("Grassland")
                else
                    tile.setTerrain("Ocean")
                end
            end
        end
    end

    -- 2. 气候 + 海岸 + 山脉 + 河流 + 冰层
    ctx.map.generateClimate()
    ctx.map.spreadCoasts(2)
    ctx.map.generateMountains(0.7)
    ctx.map.generateRivers()
    ctx.map.generateIce()

    -- 3. 根据气候选择地形
    for x = 0, w - 1 do
        for y = 0, h - 1 do
            local tile = ctx.map.getTile(x, y)
            if tile and tile.isLand and not tile.isImpassable() then
                local temp = tile.getTemperature()
                if temp < -0.3 then
                    tile.setTerrain("Snow")
                elseif temp < 0.3 then
                    tile.setTerrain("Plains")
                else
                    tile.setTerrain("Desert")
                end
            end
        end
    end

    -- 4. 植被
    for x = 0, w - 1 do
        for y = 0, h - 1 do
            local tile = ctx.map.getTile(x, y)
            if tile and tile.isLand and not tile.isImpassable() then
                local f = ctx.perlin(x, y, 99.0, 3.0, 1)
                if f > 0.4 then
                    tile.addTerrainFeature("Forest")
                elseif f < -0.3 then
                    tile.addTerrainFeature("Jungle")
                end
            end
        end
    end

    ctx.map.convertTerrains()
    ctx.map.assignContinents()
    ctx.map.setTransients()
    ctx.map.normalizeTiles()
    return true
end
