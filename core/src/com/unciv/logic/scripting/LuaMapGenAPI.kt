package com.unciv.logic.scripting

import com.unciv.Constants
import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.TileMap
import com.unciv.logic.map.mapgenerator.MapGenerationRandomness
import com.unciv.logic.map.mapgenerator.RiverGenerator
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.map.tile.TileNormalizer
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.tile.TerrainType
import com.unciv.models.ruleset.unique.UniqueType
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

object LuaMapGenAPI {

    private fun LuaValue.safeToInt(): Int {
        val d = this.todouble()
        if (d.isNaN() || d.isInfinite()) {
            com.unciv.utils.Log.error("LuaMapGen: argument is NaN/Infinity, using 0")
            return 0
        }
        if (d < Int.MIN_VALUE.toDouble() || d > Int.MAX_VALUE.toDouble()) {
            com.unciv.utils.Log.error("LuaMapGen: argument $d overflows Int range, clamping")
            return if (d < 0) Int.MIN_VALUE else Int.MAX_VALUE
        }
        return d.toInt()
    }

    /**
     * Builds the Lua context for map generation scripts.
     * Unlike [LuaAPI.buildContext], there are no civs/cities/units during map generation.
     *
     * The context provides:
     * - params: read-only MapParameters table
     * - perlin(x, y, seed, scale, nOctaves, persistence, lacunarity): Perlin noise
     * - random(): double in [0.0, 1.0)
     * - randomInt(min, max): integer in [min, max] (inclusive)
     * - map: the TileMap being generated, with tile manipulation methods
     * - log(msg): write to debug log
     */
    fun buildMapGenContext(
        tileMap: TileMap,
        ruleset: Ruleset,
        randomness: MapGenerationRandomness
    ): LuaValue {
        val ctx = LuaValue.tableOf()

        ctx.set("params", buildParamsTable(tileMap))
        ctx.set("seed", LuaValue.valueOf(tileMap.mapParameters.seed.toDouble()))
        ctx.set("perlin", PerlinNoiseFunction(tileMap, randomness))
        ctx.set("random", buildRandomFunction(randomness))
        ctx.set("randomInt", buildRandomIntFunction(randomness))
        ctx.set("map", buildMapTable(tileMap, ruleset, randomness))
        ctx.set("log", luaFunction { args ->
            com.unciv.utils.Log.debug("LuaMapGen: ${args.arg(1).tojstring()}")
            LuaValue.NIL
        })

        return ctx
    }

    // region params
    private fun buildParamsTable(map: TileMap): LuaValue {
        val params = map.mapParameters
        val t = LuaValue.tableOf()

        t.set("name", LuaValue.valueOf(params.name))
        t.set("type", LuaValue.valueOf(params.type))
        t.set("shape", LuaValue.valueOf(params.shape))
        t.set("worldWrap", LuaValue.valueOf(params.worldWrap))
        t.set("mirroring", LuaValue.valueOf(params.mirroring))
        t.set("symmetryMode", LuaValue.valueOf(params.symmetryMode))

        // map size
        val size = LuaValue.tableOf()
        size.set("name", LuaValue.valueOf(params.mapSize.name))
        size.set("radius", LuaValue.valueOf(params.mapSize.radius))
        size.set("width", LuaValue.valueOf(params.mapSize.width))
        size.set("height", LuaValue.valueOf(params.mapSize.height))
        t.set("size", size)

        // Actual tile coordinate bounds (rectangular maps are centered on 0,0, so
        // coordinates can be negative - iterate with map.getAllTiles() to stay safe)
        val bounds = LuaValue.tableOf()
        bounds.set("minX", LuaValue.valueOf(map.leftX))
        bounds.set("minY", LuaValue.valueOf(map.bottomY))
        bounds.set("maxX", LuaValue.valueOf(map.leftX + params.mapSize.width - 1))
        bounds.set("maxY", LuaValue.valueOf(map.bottomY + params.mapSize.height - 1))
        t.set("bounds", bounds)

        // Advanced parameters
        t.set("waterThreshold", LuaValue.valueOf(params.waterThreshold.toDouble()))
        t.set("temperatureintensity", LuaValue.valueOf(params.temperatureintensity.toDouble()))
        t.set("temperatureShift", LuaValue.valueOf(params.temperatureShift.toDouble()))
        t.set("vegetationRichness", LuaValue.valueOf(params.vegetationRichness.toDouble()))
        t.set("rareFeaturesRichness", LuaValue.valueOf(params.rareFeaturesRichness.toDouble()))
        t.set("resourceRichness", LuaValue.valueOf(params.resourceRichness.toDouble()))
        t.set("elevationExponent", LuaValue.valueOf(params.elevationExponent.toDouble()))
        t.set("tilesPerBiomeArea", LuaValue.valueOf(params.tilesPerBiomeArea))
        t.set("maxCoastExtension", LuaValue.valueOf(params.maxCoastExtension))

        t.set("noRuins", LuaValue.valueOf(params.noRuins))
        t.set("noNaturalWonders", LuaValue.valueOf(params.noNaturalWonders))
        t.set("mapResources", LuaValue.valueOf(params.mapResources))
        t.set("strategicBalance", LuaValue.valueOf(params.getStrategicBalance()))
        t.set("legendaryStart", LuaValue.valueOf(params.getLegendaryStart()))

        // mods list
        val modsArr = LuaTable()
        params.mods.forEachIndexed { i, mod -> modsArr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(mod)) }
        t.set("mods", modsArr)

        t.set("baseRuleset", LuaValue.valueOf(params.baseRuleset))

        return t
    }
    // endregion

    // region rng
    private fun buildRandomFunction(randomness: MapGenerationRandomness): LuaValue {
        return luaFunction {
            LuaValue.valueOf(randomness.RNG.nextDouble())
        }
    }

    private fun buildRandomIntFunction(randomness: MapGenerationRandomness): LuaValue {
        return luaFunction { args ->
            val min = args.arg(1).safeToInt()
            val max = args.arg(2).safeToInt()
            LuaValue.valueOf(randomness.RNG.nextInt(min, max + 1))
        }
    }
    // endregion

    // region map
    private fun buildMapTable(map: TileMap, ruleset: Ruleset, randomness: MapGenerationRandomness): LuaValue {
        val t = LuaValue.tableOf()

        // Dimensions
        val params = map.mapParameters
        t.set("getWidth", luaFunction { LuaValue.valueOf(params.mapSize.width) })
        t.set("getHeight", luaFunction { LuaValue.valueOf(params.mapSize.height) })
        t.set("getRadius", luaFunction { LuaValue.valueOf(params.mapSize.radius) })
        t.set("getShape", luaFunction { LuaValue.valueOf(params.shape) })
        t.set("isWrapped", luaFunction { LuaValue.valueOf(params.worldWrap) })

        // Tile access
        t.set("getTile", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val tile = map.getOrNull(x, y)
            if (tile != null) buildMapGenTileTable(tile, ruleset) else LuaValue.NIL
        })

        // Iterate all tiles
        t.set("getAllTiles", luaFunction {
            val arr = LuaTable()
            map.values.forEachIndexed { i, tile ->
                arr.set(LuaValue.valueOf(i + 1), buildMapGenTileTable(tile, ruleset))
            }
            arr
        })

        // Map-level operations
        t.set("assignContinents", luaFunction {
            map.assignContinents(TileMap.AssignContinentsMode.Assign)
            LuaValue.NIL
        })

        t.set("addStartingLocation", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val nationName = if (args.narg() >= 3) args.arg(3).tojstring() else ""
            val tile = map.getOrNull(x, y) ?: return@luaFunction LuaValue.FALSE
            val result = map.addStartingLocation(nationName, tile)
            LuaValue.valueOf(result)
        })

        t.set("getStartingLocations", luaFunction {
            val arr = LuaTable()
            map.startingLocations.forEachIndexed { i, loc ->
                val entry = LuaValue.tableOf()
                entry.set("x", LuaValue.valueOf(loc.position.x))
                entry.set("y", LuaValue.valueOf(loc.position.y))
                entry.set("nation", LuaValue.valueOf(loc.nation))
                arr.set(LuaValue.valueOf(i + 1), entry)
            }
            arr
        })

        t.set("clearStartingLocations", luaFunction {
            map.clearStartingLocations()
            LuaValue.NIL
        })

        t.set("setTransients", luaFunction {
            map.setTransients(ruleset)
            LuaValue.NIL
        })

        t.set("normalizeTiles", luaFunction {
            for (tile in map.values)
                TileNormalizer.normalizeToRuleset(tile, ruleset)
            LuaValue.NIL
        })

        // region Flood fill
        t.set("floodFill", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val terrainFilter = if (args.narg() >= 3) args.arg(3).tojstring() else null
            val tiles = floodFill(map, x, y, terrainFilter)
            val arr = LuaTable()
            tiles.forEachIndexed { i, tile -> arr.set(LuaValue.valueOf(i + 1), buildMapGenTileTable(tile, ruleset)) }
            arr
        })
        // endregion

        // region Generation helpers
        t.set("generateClimate", luaFunction {
            generateClimate(map, randomness)
            LuaValue.NIL
        })

        t.set("spreadCoasts", luaFunction { args ->
            val maxExtension = if (args.narg() >= 1) args.arg(1).safeToInt()
                else map.mapParameters.maxCoastExtension
            spreadCoasts(map, ruleset, randomness, maxExtension)
            LuaValue.NIL
        })

        t.set("generateMountains", luaFunction { args ->
            val elevationExponent = if (args.narg() >= 1) args.arg(1).todouble().toFloat()
                else map.mapParameters.elevationExponent
            generateMountains(map, ruleset, randomness, elevationExponent)
            LuaValue.NIL
        })

        t.set("generateRivers", luaFunction {
            val resultingTiles = mutableSetOf<Tile>()
            RiverGenerator(map, randomness, ruleset).spawnRivers(resultingTiles)
            convertTerrains(map, ruleset, resultingTiles)
            LuaValue.NIL
        })

        t.set("generateIce", luaFunction {
            generateIce(map, ruleset, randomness)
            LuaValue.NIL
        })

        t.set("convertTerrains", luaFunction {
            convertTerrains(map, ruleset, map.values)
            LuaValue.NIL
        })

        // BBG/TeamPVP-style composable helpers (replace MapRegions dependency)

        t.set("normalizeStartPlot", luaFunction { args ->
            val x = args.arg(1).safeToInt()
            val y = args.arg(2).safeToInt()
            val opts = if (args.narg() >= 3 && args.arg(3).istable()) args.arg(3).checktable() else null
            normalizeStartPlot(map, ruleset, randomness, x, y, opts)
            LuaValue.NIL
        })

        t.set("distributeLuxuries", luaFunction { args ->
            val opts = if (args.narg() >= 1 && args.arg(1).istable()) args.arg(1).checktable() else null
            distributeLuxuries(map, ruleset, randomness, opts)
            LuaValue.NIL
        })

        t.set("distributeStrategics", luaFunction { args ->
            val opts = if (args.narg() >= 1 && args.arg(1).istable()) args.arg(1).checktable() else null
            distributeStrategics(map, ruleset, randomness, opts)
            LuaValue.NIL
        })

        t.set("strategicBalanceStarts", luaFunction { args ->
            val opts = if (args.narg() >= 1 && args.arg(1).istable()) args.arg(1).checktable() else null
            strategicBalanceStarts(map, ruleset, randomness, opts)
            LuaValue.NIL
        })
        // endregion

        return t
    }

    // region generation algorithms

    /** BFS flood fill from (startX, startY), collecting connected tiles with matching [terrainFilter]. */
    private fun floodFill(map: TileMap, startX: Int, startY: Int, terrainFilter: String?): List<Tile> {
        val start = map.getOrNull(startX, startY) ?: return emptyList()
        // Start tile must match the filter too
        if (terrainFilter != null && start.baseTerrain != terrainFilter) return emptyList()
        val visited = LinkedHashSet<Tile>()
        val queue = ArrayDeque<Tile>()
        queue.add(start)
        while (queue.isNotEmpty()) {
            val tile = queue.removeFirst()
            if (!visited.add(tile)) continue
            for (neighbor in tile.neighbors) {
                if (neighbor in visited) continue
                if (terrainFilter == null || neighbor.baseTerrain == terrainFilter)
                    queue.add(neighbor)
            }
        }
        return visited.toList()
    }

    /** Generates temperature and humidity for all tiles using Perlin noise + latitude. */
    private fun generateClimate(map: TileMap, randomness: MapGenerationRandomness) {
        val params = map.mapParameters
        val humiditySeed = randomness.RNG.nextInt().toDouble()
        val temperatureSeed = randomness.RNG.nextInt().toDouble()
        val scale = params.tilesPerBiomeArea.toDouble()
        val tempIntensity = params.temperatureintensity
        val tempShift = params.temperatureShift
        val humidityShift = if (tempShift > 0) -tempShift / 2 else 0f

        for (tile in map.values) {
            val humidityRandom = randomness.getPerlinNoise(tile, humiditySeed, scale = scale, nOctaves = 1)
            tile.humidity = ((humidityRandom + 1.0) / 2.0 + humidityShift).coerceIn(0.0..1.0)

            val expectedTemperature = if (params.shape == MapShape.flatEarth) {
                val radiusRatio = sqrt(
                    (abs(tile.latitude) / map.maxLatitude.toFloat()).pow(2) +
                    (abs(tile.longitude) / map.maxLongitude.toFloat()).pow(2)
                )
                when {
                    radiusRatio < 0.5 -> ((0.05 - radiusRatio) / (0.05 - 0.5) * 2.0 - 1.0)
                    radiusRatio > 0.5 -> ((radiusRatio - 0.5) / (0.95 - 0.5) * (-2.0) + 1.0)
                    else -> 1.0
                }
            } else {
                1.0 - 2.0 * abs(tile.latitude) / map.maxLatitude
            }

            val randomTemperature = randomness.getPerlinNoise(tile, temperatureSeed, scale = scale, nOctaves = 1)
            var temperature = (5.0 * expectedTemperature + randomTemperature) / 6.0
            temperature = abs(temperature).pow(1.0 - tempIntensity) * temperature.sign
            temperature = (temperature + tempShift).coerceIn(-1.0..1.0)
            tile.temperature = temperature
        }
    }

    /** Spreads coast terrain from land edges, iterating up to [maxExtension] times. */
    private fun spreadCoasts(map: TileMap, ruleset: Ruleset, randomness: MapGenerationRandomness, maxExtension: Int) {
        val coastTerrains = ruleset.terrains.values.filter { it.isCoast }.toList()
        if (coastTerrains.isEmpty()) return

        val oceanNames = ruleset.terrains.values.filter { it.type == TerrainType.Water && !it.isCoast && !it.isFreshwater }.map { it.name }.toSet()

        for (i in 1..maxExtension) {
            val toCoast = mutableListOf<Tile>()
            for (tile in map.values) {
                if (tile.baseTerrain !in oceanNames) continue
                for (neighbor in tile.neighbors) {
                    if (neighbor.isLand) { toCoast.add(tile); break }
                    else if (neighbor.getBaseTerrain().isCoast && randomness.RNG.nextBoolean())
                        { toCoast.add(tile); break }
                }
            }
            for (tile in toCoast) {
                val coast = coastTerrains.filter { it.type.matchesTerrainTempAndHumidity(tile) }
                    .ifEmpty { coastTerrains }
                    .random(randomness.RNG)
                tile.setBaseTerrain(coast)
            }
        }
    }

    private fun TerrainType.matchesTerrainTempAndHumidity(tile: Tile): Boolean {
        // Simplified matching: check if terrain can occur at the tile's temperature
        val temp = tile.temperature ?: return true
        return when {
            temp < -0.6 && this == TerrainType.Water -> false // too cold for coast
            else -> true
        }
    }

    /** Generates mountain chains and hill groups via Perlin noise. */
    private fun generateMountains(map: TileMap, ruleset: Ruleset, randomness: MapGenerationRandomness, elevationExponent: Float) {
        val mountainTerrains = ruleset.terrains.values.filter {
            it.hasUnique(UniqueType.OccursInChains) && !it.hasUnique(UniqueType.NoNaturalGeneration)
        }
        val hillTerrains = ruleset.terrains.values.filter {
            it.hasUnique(UniqueType.OccursInGroups) && !it.hasUnique(UniqueType.NoNaturalGeneration)
        }

        if (mountainTerrains.isEmpty() && hillTerrains.isEmpty()) return

        val elevationSeed = randomness.RNG.nextInt().toDouble()
        val scale = map.mapParameters.tilesPerBiomeArea.toDouble()

        for (tile in map.values) {
            if (!tile.isLand || tile.isImpassible()) continue

            val elevation = randomness.getPerlinNoise(tile, elevationSeed, scale = scale, nOctaves = 6)
            val adjusted = abs(elevation).pow(elevationExponent.toDouble()) * elevation.sign

            if (adjusted > 0.55 && mountainTerrains.isNotEmpty()) {
                val mountain = mountainTerrains.random(randomness.RNG)
                tile.setBaseTerrain(mountain)
            } else if (adjusted > 0.25 && hillTerrains.isNotEmpty()) {
                val hill = hillTerrains.random(randomness.RNG)
                tile.setBaseTerrain(hill)
            }
        }
    }

    /** Generates ice at polar latitudes. */
    private fun generateIce(map: TileMap, ruleset: Ruleset, randomness: MapGenerationRandomness) {
        map.setTransients(ruleset)  // ensure terrain transients are current

        val iceTerrains = ruleset.terrains.values.filter { it.isIce }
        if (iceTerrains.isEmpty()) return

        val oceanTerrains = ruleset.terrains.values.filter { it.isOcean && !it.isIce }
        val temperatureSeed = randomness.RNG.nextInt().toDouble()
        val scale = map.mapParameters.tilesPerBiomeArea.toDouble()
        val tempIntensity = map.mapParameters.temperatureintensity
        val tempShift = map.mapParameters.temperatureShift

        for (tile in map.values) {
            if (oceanTerrains.none { it.name == tile.baseTerrain } || tile.terrainFeatures.isNotEmpty()) continue

            val randomTemperature = randomness.getPerlinNoise(tile, temperatureSeed, scale = scale, nOctaves = 1)
            val latitudeTemperature = 1.0 - 2.0 * abs(tile.latitude) / map.maxLatitude
            var iceTemp = (latitudeTemperature + randomTemperature) / 2.0
            iceTemp = abs(iceTemp).pow(1.0 - tempIntensity) * iceTemp.sign
            iceTemp = (iceTemp + tempShift).coerceIn(-1.0..1.0)

            val iceTerrain = iceTerrains.randomOrNull(randomness.RNG) ?: continue
            if (iceTemp < -0.3) {
                tile.addTerrainFeature(iceTerrain.name)
            }
        }
    }

    /** Applies "Changes Terrain Near" uniques to convert terrains. */
    private fun convertTerrains(map: TileMap, ruleset: Ruleset, tiles: Iterable<Tile>) {
        for (tile in tiles) {
            val conversionUnique = tile.getBaseTerrain()
                .getMatchingUniques(UniqueType.ChangesTerrain, com.unciv.models.ruleset.unique.GameContext(tile = tile))
                .firstOrNull { tile.isAdjacentTo(it.params[1]) }
                ?: continue
            val terrain = ruleset.terrains[conversionUnique.params[0]] ?: continue

            if (terrain.type != TerrainType.TerrainFeature)
                tile.baseTerrain = terrain.name
            else if (!terrain.occursOn.contains(tile.lastTerrain.name)) continue
            else tile.addTerrainFeature(terrain.name)
            tile.setTerrainTransients()
        }
    }

    // region BBG/TeamPVP-style composable helpers

    /** Normalize a starting plot following BBG/TeamPVP conventions.
     *  Options table (all optional):
     *  - freshwater (bool, default true)
     *  - minFood (int, default 4) — minimum food in inner ring (7 tiles)
     *  - minProd (int, default 3) — minimum production in inner ring
     *  - minLuxuries (int, default 2) — minimum unique luxury types within 3 tiles
     *  - maxBlocking (int, default 2) — max impassable/water tiles in inner ring
     *  - minHills (int, default 2) — minimum hill tiles in inner ring
     */
    private fun normalizeStartPlot(
        map: TileMap, ruleset: Ruleset, randomness: MapGenerationRandomness,
        startX: Int, startY: Int, opts: LuaTable?
    ) {
        val start = map.getOrNull(startX, startY) ?: return
        val freshwater = opts?.get("freshwater")?.toboolean() ?: true
        val minFood = opts?.get("minFood")?.safeToInt() ?: 4
        val minProd = opts?.get("minProd")?.safeToInt() ?: 3
        val minLuxuries = opts?.get("minLuxuries")?.safeToInt() ?: 2
        val maxBlocking = opts?.get("maxBlocking")?.safeToInt() ?: 2
        val minHills = opts?.get("minHills")?.safeToInt() ?: 2

        val innerRing = (listOf(start) + start.neighbors.toList()).toMutableList()

        // 1. Remove ice from inner ring
        for (tile in innerRing) {
            val iceFeatures = tile.terrainFeatures.filter { ruleset.terrains[it]?.isIce == true }
            for (f in iceFeatures) tile.removeTerrainFeature(f)
        }

        // 2. Ensure freshwater (add river if needed)
        if (freshwater) {
            val hasFreshwater = start.neighbors.any {
                start.isConnectedByRiver(it) || it.getBaseTerrain().isFreshwater
            }
            if (!hasFreshwater) {
                for (neighbor in start.neighbors) {
                    if (neighbor.isLand && !start.isConnectedByRiver(neighbor)) {
                        start.setConnectedByRiver(neighbor, true)
                        break
                    }
                }
            }
        }

        // Yield counting helpers
        val statFood = com.unciv.models.stats.Stat.Food
        val statProd = com.unciv.models.stats.Stat.Production

        fun innerFood(): Int = innerRing.sumOf {
            ((it.stats.getTileStats(null)[statFood] ?: 0f)).toInt()
        }
        fun innerProd(): Int = innerRing.sumOf {
            ((it.stats.getTileStats(null)[statProd] ?: 0f)).toInt()
        }
        fun innerHills(): Int = innerRing.count { it.isHill() }
        fun innerBlocking(): Int = innerRing.count { it.isImpassible() || it.isWater }

        // Resource pools for normalization
        val foodResources = ruleset.tileResources.values.filter {
            it.resourceType == com.unciv.models.ruleset.tile.ResourceType.Bonus &&
            (it[statFood] ?: 0f) >= 1f
        }
        val prodResources = ruleset.tileResources.values.filter {
            it.resourceType == com.unciv.models.ruleset.tile.ResourceType.Strategic &&
            (it[statProd] ?: 0f) >= 1f
        }
        val hillFeatures = ruleset.terrains.values.filter {
            it.hasUnique(UniqueType.OccursInGroups)
        }
        val allLuxuries = ruleset.tileResources.values.filter {
            it.resourceType == com.unciv.models.ruleset.tile.ResourceType.Luxury
        }

        // 3. Boost food
        var foodAttempts = 0
        while (innerFood() < minFood && foodAttempts < 10) {
            val flatInner = innerRing.filter { it.isLand && !it.isHill() && !it.isImpassible() && it.tileResource == null }
            val foodRes = foodResources.randomOrNull(randomness.RNG) ?: break
            val candidate = flatInner.firstOrNull { foodRes.generatesNaturallyOn(it) }
            if (candidate == null) { foodAttempts++; continue }
            candidate.tileResource = foodRes
            candidate.resourceAmount = 2
            foodAttempts++
        }

        // 4. Boost production (add hills or prod resources)
        var prodAttempts = 0
        while (innerProd() < minProd && prodAttempts < 10) {
            val flatInner = innerRing.filter { it.isLand && !it.isHill() && !it.isImpassible() && it.tileResource == null }
            if (flatInner.isNotEmpty() && hillFeatures.isNotEmpty() && randomness.RNG.nextBoolean()) {
                val hill = hillFeatures.random(randomness.RNG)
                val candidate = flatInner.firstOrNull { hill.occursOn.contains(it.lastTerrain.name) }
                if (candidate != null) candidate.addTerrainFeature(hill.name)
            } else if (flatInner.isNotEmpty() && prodResources.isNotEmpty()) {
                val res = prodResources.random(randomness.RNG)
                val candidate = flatInner.firstOrNull { res.generatesNaturallyOn(it) }
                if (candidate != null) { candidate.tileResource = res; candidate.resourceAmount = 2 }
            }
            prodAttempts++
        }

        // 5. Ensure minimum hills
        var hillAttempts = 0
        while (innerHills() < minHills && hillAttempts < 10) {
            val flatInner = innerRing.filter { it.isLand && !it.isHill() && !it.isImpassible() }
            val hill = hillFeatures.randomOrNull(randomness.RNG) ?: break
            val candidate = flatInner.firstOrNull { hill.occursOn.contains(it.lastTerrain.name) }
            if (candidate != null) candidate.addTerrainFeature(hill.name)
            hillAttempts++
        }

        // 6. Fix excessive blocking tiles
        val excessBlocking = innerBlocking() - maxBlocking
        if (excessBlocking > 0) {
            val toFix = innerRing.filter { it.isImpassible() || it.isWater }.take(excessBlocking)
            for (tile in toFix) {
                val safeTerrain = ruleset.terrains["Grassland"] ?: ruleset.terrains["Plains"]
                    ?: ruleset.terrains.values.firstOrNull { it.type == TerrainType.Land && !it.impassable }
                if (safeTerrain != null) tile.setBaseTerrain(safeTerrain)
            }
        }

        // 7. Ensure luxury variety (radius 3)
        val radius3 = start.getTilesInDistance(3).toList()
        val existingLux = radius3.mapNotNull { it.tileResource?.name }
            .filter { name -> name in allLuxuries.map { l -> l.name } }.toMutableSet()
        var luxAttempts = 0
        while (existingLux.size < minLuxuries && luxAttempts < 10) {
            val available = allLuxuries.filter { it.name !in existingLux }
            val luxury = available.randomOrNull(randomness.RNG) ?: break
            val candidates = radius3.filter {
                it.isLand && !it.isImpassible() && it.tileResource == null &&
                luxury.generatesNaturallyOn(it)
            }
            val chosen = candidates.randomOrNull(randomness.RNG)
            if (chosen == null) { luxAttempts++; continue }
            chosen.tileResource = luxury; chosen.resourceAmount = 2
            existingLux.add(luxury.name)
            luxAttempts++
        }
    }

    /** Distribute luxury resources per starting location. Options: perPlayer (default 2), minDistance (default 4). */
    private fun distributeLuxuries(
        map: TileMap, ruleset: Ruleset, randomness: MapGenerationRandomness, opts: LuaTable?
    ) {
        val starts = map.startingLocations.map { it.position }.ifEmpty { return }
        val perPlayer = opts?.get("perPlayer")?.safeToInt() ?: 2
        val minDistance = opts?.get("minDistance")?.safeToInt() ?: 4
        val luxuries = ruleset.tileResources.values.filter {
            it.resourceType == com.unciv.models.ruleset.tile.ResourceType.Luxury
        }.toMutableList()
        if (luxuries.isEmpty()) return

        for (startPos in starts) {
            val start = map[HexCoord(startPos.x, startPos.y)]
            val region = start.getTilesInDistance(7).toList()
            val placedHere = mutableSetOf<String>()
            repeat(perPlayer) {
                val available = luxuries.filter { it.name !in placedHere }
                    .ifEmpty { luxuries }
                val luxury = available.random(randomness.RNG)
                val candidates = region.filter { tile ->
                    tile.isLand && !tile.isImpassible() && tile.tileResource == null &&
                    luxury.generatesNaturallyOn(tile) &&
                    tile.getTilesInDistance(minDistance).none { it.tileResource?.name == luxury.name }
                }
                val chosen = candidates.randomOrNull(randomness.RNG) ?: return@repeat
                chosen.tileResource = luxury
                chosen.resourceAmount = 2
                placedHere.add(luxury.name)
            }
        }
    }

    /** Distribute strategic resources per starting location. Options: perPlayer (default 2), radius (default 7). */
    private fun distributeStrategics(
        map: TileMap, ruleset: Ruleset, randomness: MapGenerationRandomness, opts: LuaTable?
    ) {
        val starts = map.startingLocations.map { it.position }.ifEmpty { return }
        val perPlayer = opts?.get("perPlayer")?.safeToInt() ?: 2
        val searchRadius = opts?.get("radius")?.safeToInt() ?: 7
        val strategics = ruleset.tileResources.values.filter {
            it.resourceType == com.unciv.models.ruleset.tile.ResourceType.Strategic
        }
        if (strategics.isEmpty()) return

        for (startPos in starts) {
            val start = map[HexCoord(startPos.x, startPos.y)]
            val region = start.getTilesInDistance(searchRadius).toList()
            for (strategic in strategics) {
                repeat(perPlayer) {
                    val candidates = region.filter { tile ->
                        tile.isLand && !tile.isImpassible() && tile.tileResource == null &&
                        strategic.generatesNaturallyOn(tile)
                    }
                    val chosen = candidates.randomOrNull(randomness.RNG) ?: return@repeat
                    chosen.tileResource = strategic
                    chosen.resourceAmount = if (strategic.name.contains("horse", ignoreCase = true)) 4 else 2
                }
            }
        }
    }

    /** Second-pass strategic balance: guarantees specific strategics within radius of each start.
     *  Options: horses (default 0), iron (default 0), radius (default 5). */
    private fun strategicBalanceStarts(
        map: TileMap, ruleset: Ruleset, randomness: MapGenerationRandomness, opts: LuaTable?
    ) {
        val starts = map.startingLocations.map { it.position }.ifEmpty { return }
        val searchRadius = opts?.get("radius")?.safeToInt() ?: 5
        val guarantees = linkedMapOf<String, Int>()
        if ((opts?.get("horses")?.safeToInt() ?: 0) > 0)
            guarantees["Horses"] = opts!!.get("horses").safeToInt()
        if ((opts?.get("iron")?.safeToInt() ?: 0) > 0)
            guarantees["Iron"] = opts!!.get("iron").safeToInt()

        val allStrategics = ruleset.tileResources.values.filter {
            it.resourceType == com.unciv.models.ruleset.tile.ResourceType.Strategic
        }

        for (startPos in starts) {
            val start = map[HexCoord(startPos.x, startPos.y)]
            val region = start.getTilesInDistance(searchRadius).toList()

            for ((resourceName, needed) in guarantees) {
                val strategic = allStrategics.firstOrNull {
                    it.name.contains(resourceName, ignoreCase = true)
                } ?: continue
                val current = region.count { it.tileResource?.name == strategic.name }
                var toAdd = (needed - current).coerceAtLeast(0)
                var attempts = 0
                while (toAdd > 0 && attempts < 20) {
                    val candidates = region.filter { tile ->
                        tile.isLand && !tile.isImpassible() && tile.tileResource == null &&
                        strategic.generatesNaturallyOn(tile)
                    }
                    val chosen = candidates.randomOrNull(randomness.RNG) ?: break
                    chosen.tileResource = strategic
                    chosen.resourceAmount = if (strategic.name.contains("horse", ignoreCase = true)) 4 else 2
                    toAdd--
                    attempts++
                }
            }
        }
    }

    // endregion

    private fun buildMapGenTileTable(tile: Tile, ruleset: Ruleset): LuaValue {
        val t = LuaValue.tableOf()

        // Position
        val pos = LuaValue.tableOf()
        pos.set("x", LuaValue.valueOf(tile.position.x))
        pos.set("y", LuaValue.valueOf(tile.position.y))
        t.set("position", pos)
        t.set("getX", luaFunction { LuaValue.valueOf(tile.position.x) })
        t.set("getY", luaFunction { LuaValue.valueOf(tile.position.y) })

        // Terrain
        t.set("baseTerrain", LuaValue.valueOf(tile.baseTerrain))
        t.set("isLand", LuaValue.valueOf(tile.isLand))
        t.set("isWater", LuaValue.valueOf(tile.isWater))
        t.set("isCoast", LuaValue.valueOf(tile.baseTerrain == "Coast"))
        t.set("isHill", luaFunction { LuaValue.valueOf(tile.isHill()) })
        t.set("isMountain", luaFunction { LuaValue.valueOf(tile.isImpassible()) })
        t.set("isImpassable", luaFunction { LuaValue.valueOf(tile.isImpassible()) })

        // Terrain features
        t.set("hasTerrainFeature", luaFunction { args ->
            LuaValue.valueOf(tile.terrainFeatures.contains(args.arg(1).tojstring()))
        })
        t.set("getTerrainFeatures", luaFunction {
            val arr = LuaTable()
            tile.terrainFeatures.sorted().forEachIndexed { i, f ->
                arr.set(LuaValue.valueOf(i + 1), LuaValue.valueOf(f))
            }
            arr
        })

        // Climate (read/write)
        t.set("temperature", LuaValue.valueOf(tile.temperature ?: 0.0))
        t.set("setTemperature", luaFunction { args ->
            tile.temperature = args.arg(1).todouble()
            LuaValue.NIL
        })
        t.set("getTemperature", luaFunction {
            LuaValue.valueOf(tile.temperature ?: 0.0)
        })
        t.set("humidity", LuaValue.valueOf(tile.humidity ?: 0.0))
        t.set("setHumidity", luaFunction { args ->
            tile.humidity = args.arg(1).todouble()
            LuaValue.NIL
        })
        t.set("getHumidity", luaFunction {
            LuaValue.valueOf(tile.humidity ?: 0.0)
        })
        t.set("getLatitude", luaFunction { LuaValue.valueOf(tile.latitude) })
        t.set("getLongitude", luaFunction { LuaValue.valueOf(tile.longitude) })

        // Continent
        t.set("getContinent", luaFunction { LuaValue.valueOf(tile.getContinent()) })

        // Resource
        t.set("hasResource", luaFunction { LuaValue.valueOf(tile.tileResource != null) })
        t.set("resourceName", LuaValue.valueOf(tile.tileResource?.name ?: ""))
        t.set("resourceAmount", LuaValue.valueOf(tile.resourceAmount))

        // Improvement
        t.set("hasImprovement", luaFunction { LuaValue.valueOf(tile.tileImprovement != null) })
        t.set("improvementName", LuaValue.valueOf(tile.improvement ?: ""))

        // Rivers
        t.set("isRiver", luaFunction {
            LuaValue.valueOf(tile.neighbors.any { tile.isConnectedByRiver(it) })
        })

        // Natural wonder
        t.set("isNaturalWonder", luaFunction { LuaValue.valueOf(tile.isNaturalWonder()) })

        // Freshwater detection
        t.set("isAdjacentToFreshWater", luaFunction {
            val hasRiver = tile.neighbors.any { tile.isConnectedByRiver(it) }
            val hasLake = tile.neighbors.any { n ->
                val bt = n.getBaseTerrain()
                bt.isFreshwater
            }
            LuaValue.valueOf(hasRiver || hasLake)
        })

        // Yield evaluation (useful for start normalization and resource placement)
        t.set("getBaseYield", luaFunction { args ->
            val stat = com.unciv.models.stats.Stat.safeValueOf(args.arg(1).tojstring())
                ?: return@luaFunction LuaValue.valueOf(0.0)
            val stats = tile.stats.getTileStats(null)
            LuaValue.valueOf((stats[stat] ?: 0f).toDouble())
        })

        // Neighbors
        t.set("getNeighbors", luaFunction {
            val arr = LuaTable()
            tile.neighbors.forEachIndexed { i, n ->
                arr.set(LuaValue.valueOf(i + 1), buildMapGenTileTable(n, ruleset))
            }
            arr
        })
        t.set("getTilesInDistance", luaFunction { args ->
            val radius = args.arg(1).safeToInt()
            val arr = LuaTable()
            @Suppress("DEPRECATION")
            tile.getTilesInDistance(radius).forEachIndexed { i, t2 ->
                arr.set(LuaValue.valueOf(i + 1), buildMapGenTileTable(t2, ruleset))
            }
            arr
        })

        // Write operations
        t.set("setTerrain", luaFunction { args ->
            val terrain = ruleset.terrains[args.arg(1).tojstring()]
            if (terrain != null) tile.setBaseTerrain(terrain)
            LuaValue.NIL
        })
        t.set("addTerrainFeature", luaFunction { args ->
            tile.addTerrainFeature(args.arg(1).tojstring())
            LuaValue.NIL
        })
        t.set("removeTerrainFeature", luaFunction { args ->
            tile.removeTerrainFeature(args.arg(1).tojstring())
            LuaValue.NIL
        })
        t.set("removeAllTerrainFeatures", luaFunction {
            tile.removeTerrainFeatures()
            LuaValue.NIL
        })
        t.set("setResource", luaFunction { args ->
            val resource = ruleset.tileResources[args.arg(1).tojstring()]
            val amount = args.arg(2).safeToInt()
            tile.tileResource = resource
            tile.resourceAmount = amount
            LuaValue.NIL
        })
        t.set("removeResource", luaFunction {
            tile.tileResource = null
            tile.resourceAmount = 0
            LuaValue.NIL
        })
        t.set("setImprovement", luaFunction { args ->
            val improvement = ruleset.tileImprovements[args.arg(1).tojstring()]
            if (improvement != null) tile.setImprovementBasic(improvement)
            LuaValue.NIL
        })
        t.set("removeImprovement", luaFunction {
            tile.removeImprovement()
            LuaValue.NIL
        })
        t.set("setRoad", luaFunction {
            val roadStatus = com.unciv.logic.map.tile.RoadStatus.Road
            tile.setRoadStatus(roadStatus, null)
            LuaValue.NIL
        })
        t.set("setRailroad", luaFunction {
            val roadStatus = com.unciv.logic.map.tile.RoadStatus.Railroad
            tile.setRoadStatus(roadStatus, null)
            LuaValue.NIL
        })
        t.set("removeRoad", luaFunction {
            tile.removeRoad()
            LuaValue.NIL
        })

        // Natural wonder
        t.set("setNaturalWonder", luaFunction { args ->
            val wonderName = args.arg(1).tojstring()
            if (wonderName.isEmpty() || !ruleset.terrains.containsKey(wonderName)) {
                tile.naturalWonder = null
            } else {
                tile.naturalWonder = wonderName
                TileNormalizer.normalizeToRuleset(tile, ruleset)
            }
            LuaValue.NIL
        })

        return t
    }
    // endregion
}

/**
 * Lua-callable Perlin noise function exposed to map scripts.
 *
 * Supports two calling conventions:
 * - perlin(x, y, seed) — minimal call with defaults for scale/nOctaves/etc.
 * - perlin{x, y, seed, scale=..., nOctaves=...} — named-parameter style via Lua table
 *
 * LuaJ 3.0.1 does not reliably dispatch 4+ individual args through invoke(),
 * so extended parameters are passed as a table.
 */
private class PerlinNoiseFunction(
    private val map: com.unciv.logic.map.TileMap,
    private val randomness: MapGenerationRandomness
) : org.luaj.vm2.LuaFunction() {

    override fun call(
        arg1: org.luaj.vm2.LuaValue,
        arg2: org.luaj.vm2.LuaValue,
        arg3: org.luaj.vm2.LuaValue
    ): org.luaj.vm2.LuaValue = compute(arg1, arg2, arg3, null)

    override fun call(arg: org.luaj.vm2.LuaValue): org.luaj.vm2.LuaValue {
        // Table-based call: perlin{x=0, y=0, seed=42.0, scale=4.0}
        if (!arg.istable())
            error("perlin() requires either 3 args (x, y, seed) or a table {x, y, seed, ...}")
        val t = arg.checktable()
        val x = t.get("x")
        val y = t.get("y")
        val seed = t.get("seed")
        return compute(x, y, seed, t)
    }

    override fun call(): org.luaj.vm2.LuaValue =
        error("perlin() requires 3 arguments (x, y, seed)")

    override fun call(
        arg1: org.luaj.vm2.LuaValue,
        arg2: org.luaj.vm2.LuaValue
    ): org.luaj.vm2.LuaValue =
        error("perlin() requires 3 arguments (x, y, seed)")

    /**
     * Same luaj quirk as the [luaFunction] wrapper: calls with 4+ arguments (here: the options
     * table) are routed through [LuaValue.invoke], which the base [LuaFunction] does not override -
     * falling through to the `__call` metamethod lookup and throwing "attempt to call function".
     */
    override fun invoke(varargs: org.luaj.vm2.Varargs): org.luaj.vm2.Varargs = when (varargs.narg()) {
        0 -> error("perlin() requires 3 arguments (x, y, seed)")
        1 -> call(varargs.arg1())
        2 -> call(varargs.arg1(), varargs.arg(2))
        3 -> call(varargs.arg1(), varargs.arg(2), varargs.arg(3))
        else -> {
            val opts = if (varargs.arg(4).istable()) varargs.arg(4).checktable() else null
            compute(varargs.arg1(), varargs.arg(2), varargs.arg(3), opts)
        }
    }

    private fun compute(
        xVal: org.luaj.vm2.LuaValue,
        yVal: org.luaj.vm2.LuaValue,
        seedVal: org.luaj.vm2.LuaValue,
        opts: org.luaj.vm2.LuaTable?
    ): org.luaj.vm2.LuaValue {
        val x = xVal.safeToInt()
        val y = yVal.safeToInt()
        val seed = seedVal.todouble()
        val scale = opts?.get("scale")?.todoubleOrNull() ?: 30.0
        val nOctaves = opts?.get("nOctaves")?.tointOrNull() ?: 6
        val persistence = opts?.get("persistence")?.todoubleOrNull() ?: 0.5
        val lacunarity = opts?.get("lacunarity")?.todoubleOrNull() ?: 2.0

        val tile = map.getOrNull(x, y)
            ?: return org.luaj.vm2.LuaValue.valueOf(0.0)
        val result = randomness.getPerlinNoise(tile, seed, nOctaves, persistence, lacunarity, scale)
        return org.luaj.vm2.LuaValue.valueOf(result)
    }

    private fun org.luaj.vm2.LuaValue.todoubleOrNull(): Double? =
        if (isnil()) null else todouble()

    private fun org.luaj.vm2.LuaValue.tointOrNull(): Int? =
        if (isnil()) null else safeToInt()

    private fun org.luaj.vm2.LuaValue.safeToInt(): Int {
        val d = this.todouble()
        if (d.isNaN() || d.isInfinite()) {
            com.unciv.utils.Log.error("LuaMapGen: argument is NaN/Infinity, using 0")
            return 0
        }
        if (d < Int.MIN_VALUE.toDouble() || d > Int.MAX_VALUE.toDouble()) {
            com.unciv.utils.Log.error("LuaMapGen: argument $d overflows Int range, clamping")
            return if (d < 0) Int.MIN_VALUE else Int.MAX_VALUE
        }
        return d.toInt()
    }
}
