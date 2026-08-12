<!-- 本文件由 LuaApiReferenceWriter / LuaMapApiReferenceWriter 自动生成，请勿手动编辑。 -->
<!-- Generated from the LuaApiDocs / LuaMapGenApiDocs tables - never edit by hand. -->
<!-- 重新生成 / Regenerate with: ./gradlew desktop:generateDocs -->

# Lua 地图脚本 API 参考

本页面由游戏代码自动生成，列出 `GenerateMap(ctx)` 地图脚本中可用的**生成期专用** API——
一个独立的沙盒上下文，没有 civ/城市/单位。它永远不会与实现脱节（地图脚本的工作方式
见 [Lua 脚本](Lua-Modding.md)）。

编辑器补全：把 LuaLS 语言服务器指向 `lua-map-api.lua`（与主 API 相同的 `.luarc.json`
配置，两个文件都列入 `workspace.library`）。

> **关于签名**：`opts?` 参数为可选表（字段见 `lua-map-api.lua` 中的类型定义）；
> `|nil` 表示该值可能为空。

## ctx — 地图脚本上下文

**属性**:

`params`, `seed`, `map`

**查询方法**:

```lua
ctx.perlin(x, y, seed, opts?) -- Perlin 噪声，大致范围 [-1, 1]
ctx.random()                  -- 基于种子的确定性随机数 [0, 1)
ctx.randomInt(min, max)       -- 基于种子的确定性随机整数 [min, max]（含两端）
```

**写入方法**:

```lua
ctx.log(msg) -- 调试日志
```

## ctx.params — 地图参数

**属性**:

`name`, `type`, `shape`, `worldWrap`, `mirroring`, `symmetryMode`, `size`, `bounds`, `waterThreshold`, `temperatureintensity`, `temperatureShift`, `vegetationRichness`, `rareFeaturesRichness`, `resourceRichness`, `elevationExponent`, `tilesPerBiomeArea`, `maxCoastExtension`, `noRuins`, `noNaturalWonders`, `mapResources`, `strategicBalance`, `legendaryStart`, `mods`, `baseRuleset`

## ctx.params.size — 地图尺寸

**属性**:

`name`, `radius`, `width`, `height`

## ctx.params.bounds — 地块边界

**属性**:

`minX`, `minY`, `maxX`, `maxY`

## ctx.map — 地图操作

**查询方法**:

```lua
map.getWidth()                      -- 地图宽度
map.getHeight()                     -- 地图高度
map.getRadius()                     -- 地图半径
map.getShape()                      -- 地图形状
map.isWrapped()                     -- 地图是否环绕
map.getTile(x, y)                   -- 指定坐标的地块（越界为 nil）
map.getAllTiles()                   -- 全部地块（优先用这个而不是遍历坐标）
map.getStartingLocations()          -- 起始位置列表 {x, y, nation}
map.floodFill(x, y, terrainFilter?) -- BFS 连通地块（可选按地形过滤）
```

**写入方法**:

```lua
map.assignContinents()                            -- 分配大陆编号
map.addStartingLocation(x, y, nationName?)        -- 添加起始位置（nationName 可选）
map.clearStartingLocations()                      -- 清空全部起始位置
map.setTransients()                               -- 设置地块临时数据（其他辅助函数前需先运行）
map.normalizeTiles()                              -- 按规则集规范化全部地块
map.generateClimate()                             -- 按纬度生成温度/湿度
map.spreadCoasts(maxExtension?)                   -- 扩展海岸（默认：params.maxCoastExtension）
map.generateMountains(elevationExponent?)         -- 生成山脉
map.generateRivers()                              -- 生成河流
map.generateIce()                                 -- 生成极地冰盖
map.convertTerrains()                             -- 转换地形
map.normalizeStartPlot(x, y, opts?)               -- 起始地块平衡（食物/产能/奢侈/丘陵）
map.distributeLuxuries(opts?, minDistance?)       -- 分配奢侈资源
map.distributeStrategics(opts?, radius?)          -- 分配战略资源
map.strategicBalanceStarts(opts?, iron?, radius?) -- 平衡起始点附近的战略资源
```

## MapGen Tile — 地块

**属性**:

`position`, `baseTerrain`, `isLand`, `isWater`, `isCoast`, `temperature`, `humidity`, `resourceName`, `resourceAmount`, `improvementName`

**查询方法**:

```lua
tile.getX()                         -- X 坐标
tile.getY()                         -- Y 坐标
tile.isHill()                       -- 是否丘陵
tile.isMountain()                   -- 是否山脉
tile.isImpassable()                 -- 是否不可通行
tile.hasTerrainFeature(featureName) -- 是否有地形特征
tile.getTerrainFeatures()           -- 地形特征列表
tile.getTemperature()               -- 温度值
tile.getHumidity()                  -- 湿度值
tile.getLatitude()                  -- 纬度（赤道为 0，向两极增大）
tile.getLongitude()                 -- 经度
tile.getContinent()                 -- 大陆编号（assignContinents 之后）
tile.hasResource()                  -- 是否有资源
tile.hasImprovement()               -- 是否有改良设施
tile.isRiver()                      -- 是否为河流
tile.isNaturalWonder()              -- 是否为自然奇观
tile.isAdjacentToFreshWater()       -- 是否邻接淡水
tile.getBaseYield(stat)             -- 地块基础产出（无文明视角）
tile.getNeighbors()                 -- 相邻地块
tile.getTilesInDistance(radius)     -- 范围内所有地块
```

**写入方法**:

```lua
tile.setTemperature(v)                 -- 设置温度
tile.setHumidity(v)                    -- 设置湿度
tile.setTerrain(terrainName)           -- 改变基础地形
tile.addTerrainFeature(featureName)    -- 添加地形特征
tile.removeTerrainFeature(featureName) -- 移除地形特征
tile.removeAllTerrainFeatures()        -- 移除全部地形特征
tile.setResource(resourceName, amount) -- 设置资源
tile.removeResource()                  -- 移除资源
tile.setImprovement(improvementName)   -- 设置改良设施
tile.removeImprovement()               -- 移除改良设施
tile.setRoad()                         -- 修建道路
tile.setRailroad()                     -- 修建铁路
tile.removeRoad()                      -- 移除道路
tile.setNaturalWonder(wonderName)      -- 设置自然奇观（空字符串清除）
```
