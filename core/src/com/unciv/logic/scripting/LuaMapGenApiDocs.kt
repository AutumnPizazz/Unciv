package com.unciv.logic.scripting

/**
 * 每个 Lua 地图脚本 API 的文档元数据：签名、中英文说明、类别。
 *
 * 与 [LuaMapGenAPI.mapGenApiCatalog] 条目一一对应（由 LuaMapGenApiDocsTests 双向校验强制），
 * 是以下产物的唯一文案来源：
 *  - `docs/Modders/lua-map-api.lua`（EmmyLua 类型定义，[LuaMapApiDefinitionWriter]）
 *  - `docs/Modders/Lua-Map-API-Reference.md` / `docs/zh/Modders/Lua-Map-API-Reference.md`
 *    （面向模组作者的生成期 API 参考文档，[LuaMapApiReferenceWriter]）
 *
 * 新增或修改地图脚本 API 时：同步改这里与 [LuaMapGenAPI.mapGenApiCatalog]，然后运行
 * `./gradlew desktop:generateDocs` 重新生成产物。**严禁人工编辑生成产物。**
 *
 * 注意：map-gen 的 tile 表与游戏内 ctx 的 tile 表是两套独立 API（例如这里的
 * `isHill`/`isMountain` 是函数而游戏内是布尔属性），不要混用 [LuaApiDocs]。
 */
object LuaMapGenApiDocs {

    /** 属性条目 */
    private fun p(name: String, signature: String, desc: String, descZh: String) =
        LuaApiDocEntry(name, signature, desc, descZh, LuaApiDocCategory.Property)

    /** 查询方法条目 */
    private fun q(name: String, signature: String, desc: String, descZh: String) =
        LuaApiDocEntry(name, signature, desc, descZh, LuaApiDocCategory.Query)

    /** 写入方法条目 */
    private fun w(name: String, signature: String, desc: String, descZh: String) =
        LuaApiDocEntry(name, signature, desc, descZh, LuaApiDocCategory.Write)

    // region ctx

    private val ctxEntries = listOf(
        p("params", "UncivMapParams", "Read-only MapParameters (size/bounds/advanced settings)", "只读 MapParameters（含尺寸/边界/高级参数）"),
        p("seed", "number", "The map seed", "地图种子"),
        q("perlin", "fun(x: number, y: number, seed: number, opts?: UncivPerlinOpts): number", "Perlin noise in roughly [-1, 1]", "Perlin 噪声，大致范围 [-1, 1]"),
        q("random", "fun(): number", "Seeded deterministic random in [0, 1)", "基于种子的确定性随机数 [0, 1)"),
        q("randomInt", "fun(min: number, max: number): number", "Seeded deterministic random integer in [min, max] (inclusive)", "基于种子的确定性随机整数 [min, max]（含两端）"),
        p("map", "UncivMapGenMap", "TileMap manipulation table", "地图操作表"),
        w("log", "fun(msg: string)", "Write debug output to Unciv's log", "调试日志"),
    )

    // endregion

    // region params

    private val paramsEntries = listOf(
        p("name", "string", "Map name", "地图名"),
        p("type", "string", "Map type (e.g. MapType.scripted = \"Scripted\")", "地图类型（如 MapType.scripted = \"Scripted\"）"),
        p("shape", "string", "MapShape (\"Rectangular\"/\"Hexagonal\"/\"Flat Earth Hexagonal\")", "MapShape（\"Rectangular\"/\"Hexagonal\"/\"Flat Earth Hexagonal\"）"),
        p("worldWrap", "boolean", "Map wraps around", "地图是否环绕"),
        p("mirroring", "string", "Mirroring setting", "镜像设置"),
        p("symmetryMode", "string", "Symmetry mode", "对称模式"),
        p("size", "UncivMapSize", "Map size {name, radius, width, height}", "地图尺寸 {name, radius, width, height}"),
        p("bounds", "UncivMapBounds", "Real tile coordinate range (iterate with map.getAllTiles())", "实际地块坐标范围（遍历请用 map.getAllTiles()）"),
        p("waterThreshold", "number", "Water threshold", "水域阈值"),
        p("temperatureintensity", "number", "Temperature intensity", "温度强度"),
        p("temperatureShift", "number", "Temperature shift", "温度偏移"),
        p("vegetationRichness", "number", "Vegetation richness", "植被丰富度"),
        p("rareFeaturesRichness", "number", "Rare features richness", "稀有特征丰富度"),
        p("resourceRichness", "number", "Resource richness", "资源丰富度"),
        p("elevationExponent", "number", "Elevation exponent", "海拔指数"),
        p("tilesPerBiomeArea", "number", "Tiles per biome area", "每个生物群系区域的地块数"),
        p("maxCoastExtension", "number", "Max coast extension", "最大海岸扩展"),
        p("noRuins", "boolean", "No ancient ruins", "无远古遗迹"),
        p("noNaturalWonders", "boolean", "No natural wonders", "无自然奇观"),
        p("mapResources", "string", "Map resources setting", "地图资源设置"),
        p("strategicBalance", "boolean", "Strategic balance", "战略资源平衡"),
        p("legendaryStart", "boolean", "Legendary start", "传奇开局"),
        p("mods", "string[]", "Enabled mod names", "启用的模组列表"),
        p("baseRuleset", "string", "Base ruleset name", "基础规则集名"),
    )

    // endregion

    // region size

    private val sizeEntries = listOf(
        p("name", "string", "Size name", "尺寸名"),
        p("radius", "number", "Map radius (hex maps)", "地图半径（六边形地图）"),
        p("width", "number", "Map width", "地图宽度"),
        p("height", "number", "Map height", "地图高度"),
    )

    // endregion

    // region bounds

    private val boundsEntries = listOf(
        p("minX", "number", "Actual minimum X (negative on rectangular maps)", "实际最小 X（矩形地图为负）"),
        p("minY", "number", "Actual minimum Y", "实际最小 Y"),
        p("maxX", "number", "Actual maximum X", "实际最大 X"),
        p("maxY", "number", "Actual maximum Y", "实际最大 Y"),
    )

    // endregion

    // region map

    private val mapEntries = listOf(
        q("getWidth", "fun(): number", "Map width", "地图宽度"),
        q("getHeight", "fun(): number", "Map height", "地图高度"),
        q("getRadius", "fun(): number", "Map radius", "地图半径"),
        q("getShape", "fun(): string", "Map shape", "地图形状"),
        q("isWrapped", "fun(): boolean", "Map wraps around", "地图是否环绕"),
        q("getTile", "fun(x: number, y: number): UncivMapGenTile|nil", "Tile at coordinates (nil when out of bounds)", "指定坐标的地块（越界为 nil）"),
        q("getAllTiles", "fun(): UncivMapGenTile[]", "All tiles (prefer over iterating coordinates)", "全部地块（优先用这个而不是遍历坐标）"),
        w("assignContinents", "fun()", "Assign continent numbers", "分配大陆编号"),
        w("addStartingLocation", "fun(x: number, y: number, nationName?: string): boolean", "Add a starting location (nationName optional)", "添加起始位置（nationName 可选）"),
        q("getStartingLocations", "fun(): UncivMapStartingLocation[]", "List of {x, y, nation} starting locations", "起始位置列表 {x, y, nation}"),
        w("clearStartingLocations", "fun()", "Clear all starting locations", "清空全部起始位置"),
        w("setTransients", "fun()", "Set transient tile data (run before other helpers)", "设置地块临时数据（其他辅助函数前需先运行）"),
        w("normalizeTiles", "fun()", "Normalize every tile against the ruleset", "按规则集规范化全部地块"),
        q("floodFill", "fun(x: number, y: number, terrainFilter?: string): UncivMapGenTile[]", "BFS-connected tiles (optionally filtered by terrain)", "BFS 连通地块（可选按地形过滤）"),
        w("generateClimate", "fun()", "Assign temperature/humidity by latitude", "按纬度生成温度/湿度"),
        w("spreadCoasts", "fun(maxExtension?: number)", "Spread coasts (default: params.maxCoastExtension)", "扩展海岸（默认：params.maxCoastExtension）"),
        w("generateMountains", "fun(elevationExponent?: number)", "Generate mountain ranges", "生成山脉"),
        w("generateRivers", "fun()", "Generate rivers", "生成河流"),
        w("generateIce", "fun()", "Generate polar ice", "生成极地冰盖"),
        w("convertTerrains", "fun()", "Convert terrains", "转换地形"),
        w("normalizeStartPlot", "fun(x: number, y: number, opts?: UncivStartPlotOpts)", "Balance a start plot (food/production/luxuries/hills)", "起始地块平衡（食物/产能/奢侈/丘陵）"),
        w("distributeLuxuries", "fun(opts?: {perPlayer?: number, minDistance?: number})", "Distribute luxury resources", "分配奢侈资源"),
        w("distributeStrategics", "fun(opts?: {perPlayer?: number, radius?: number})", "Distribute strategic resources", "分配战略资源"),
        w("strategicBalanceStarts", "fun(opts?: {horses?: number, iron?: number, radius?: number})", "Balance strategic resources near starts", "平衡起始点附近的战略资源"),
    )

    // endregion

    // region tile

    private val tileEntries = listOf(
        p("position", "{x: number, y: number}", "Tile coordinates", "地块坐标"),
        q("getX", "fun(): number", "X coordinate", "X 坐标"),
        q("getY", "fun(): number", "Y coordinate", "Y 坐标"),
        p("baseTerrain", "string", "Base terrain name", "基础地形名"),
        p("isLand", "boolean", "Land tile", "是否陆地"),
        p("isWater", "boolean", "Water tile", "是否水域"),
        p("isCoast", "boolean", "Coast tile", "是否沿海"),
        q("isHill", "fun(): boolean", "Hill tile", "是否丘陵"),
        q("isMountain", "fun(): boolean", "Mountain tile", "是否山脉"),
        q("isImpassable", "fun(): boolean", "Impassable tile", "是否不可通行"),
        q("hasTerrainFeature", "fun(featureName: string): boolean", "Has terrain feature", "是否有地形特征"),
        q("getTerrainFeatures", "fun(): string[]", "Terrain feature names", "地形特征列表"),
        p("temperature", "number", "Temperature value", "温度值"),
        w("setTemperature", "fun(v: number)", "Set temperature", "设置温度"),
        q("getTemperature", "fun(): number", "Temperature value", "温度值"),
        p("humidity", "number", "Humidity value", "湿度值"),
        w("setHumidity", "fun(v: number)", "Set humidity", "设置湿度"),
        q("getHumidity", "fun(): number", "Humidity value", "湿度值"),
        q("getLatitude", "fun(): number", "Latitude (0 at the equator, grows poleward)", "纬度（赤道为 0，向两极增大）"),
        q("getLongitude", "fun(): number", "Longitude", "经度"),
        q("getContinent", "fun(): number", "Continent number (after assignContinents)", "大陆编号（assignContinents 之后）"),
        q("hasResource", "fun(): boolean", "Has a resource", "是否有资源"),
        p("resourceName", "string", "Resource name (\"\" if none)", "资源名（无则为 \"\"）"),
        p("resourceAmount", "number", "Resource amount", "资源数量"),
        q("hasImprovement", "fun(): boolean", "Has an improvement", "是否有改良设施"),
        p("improvementName", "string", "Improvement name (\"\" if none)", "改良设施名（无则为 \"\"）"),
        q("isRiver", "fun(): boolean", "Tile has a river", "是否为河流"),
        q("isNaturalWonder", "fun(): boolean", "Is a natural wonder", "是否为自然奇观"),
        q("isAdjacentToFreshWater", "fun(): boolean", "Adjacent to fresh water", "是否邻接淡水"),
        q("getBaseYield", "fun(stat: string): number", "Base tile yield for a stat (no civ view)", "地块基础产出（无文明视角）"),
        q("getNeighbors", "fun(): UncivMapGenTile[]", "Adjacent tiles", "相邻地块"),
        q("getTilesInDistance", "fun(radius: number): UncivMapGenTile[]", "All tiles within range", "范围内所有地块"),
        w("setTerrain", "fun(terrainName: string)", "Change base terrain", "改变基础地形"),
        w("addTerrainFeature", "fun(featureName: string)", "Add terrain feature", "添加地形特征"),
        w("removeTerrainFeature", "fun(featureName: string)", "Remove terrain feature", "移除地形特征"),
        w("removeAllTerrainFeatures", "fun()", "Remove all terrain features", "移除全部地形特征"),
        w("setResource", "fun(resourceName: string, amount: number)", "Set resource", "设置资源"),
        w("removeResource", "fun()", "Remove resource", "移除资源"),
        w("setImprovement", "fun(improvementName: string)", "Set improvement", "设置改良设施"),
        w("removeImprovement", "fun()", "Remove improvement", "移除改良设施"),
        w("setRoad", "fun()", "Build a road", "修建道路"),
        w("setRailroad", "fun()", "Build a railroad", "修建铁路"),
        w("removeRoad", "fun()", "Remove road/railroad", "移除道路"),
        w("setNaturalWonder", "fun(wonderName: string)", "Set natural wonder (empty string clears)", "设置自然奇观（空字符串清除）"),
    )

    // endregion

    /**
     * owner → 条目列表（顺序即文档展示顺序）。
     * 条目名集合必须与 [LuaMapGenAPI.mapGenApiCatalog] 完全一致（测试强制）。
     */
    val entries: Map<String, List<LuaApiDocEntry>> = mapOf(
        "ctx" to ctxEntries,
        "params" to paramsEntries,
        "size" to sizeEntries,
        "bounds" to boundsEntries,
        "map" to mapEntries,
        "tile" to tileEntries,
    )

    /** 条目名按 owner 索引，供生成器与测试使用 */
    val entriesByName: Map<String, Map<String, LuaApiDocEntry>> = entries.mapValues { (_, list) ->
        list.associateBy { it.name }
    }
}
