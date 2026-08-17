package com.unciv.logic.map.mapgenerator

import com.badlogic.gdx.math.Vector2
import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.logic.GameInfo
import com.unciv.logic.map.*
import com.unciv.logic.map.mapgenerator.mapregions.MapRegions
import com.unciv.logic.map.tile.Tile
import com.unciv.models.Counter
import com.unciv.models.metadata.GameParameters
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.tile.Terrain
import com.unciv.models.ruleset.tile.TerrainType
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.ui.screens.mapeditorscreen.MapGeneratorSteps
import com.unciv.logic.map.tile.TileNormalizer
import com.unciv.models.ruleset.tile.TileImprovement
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.utils.debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlin.math.E
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.math.ulp
import kotlin.sequences.filter


/** Map generator, used by new game, map editor and main menu background
 *
 *  Class instance only keeps [ruleset] and [coroutineScope] for easier access, input and output are through methods, namely [generateMap] and [generateSingleStep].
 *
 *  @param ruleset The Ruleset supplying terrain and resource definitions
 *  @param coroutineScope Enables early abort if this returns `isActive == false`
 */
class MapGenerator(val ruleset: Ruleset, private val coroutineScope: CoroutineScope? = null) {

    companion object {
        private const val consoleTimings = false

        /**
         * @return Radius in range 0.0 (center of map) to 1.0 (edge of map).
         */
        internal fun getTileRadius(tile: Tile, tileMap: TileMap): Double {
            // Numbers betwee 0.0-1.0
            val latitudeRatio = abs(tile.latitude) / tileMap.maxLatitude.toDouble()
            val longitudeRatio = abs(tile.longitude) / tileMap.maxLongitude.toDouble()
            return sqrt(latitudeRatio.pow(2) + longitudeRatio.pow(2))
        }
    }

    private var randomness = MapGenerationRandomness()

    /** 旋转对称轨道权威,由 [generateMap] / [generateSingleStep] 按当前地图参数构造。
     *  （管线改造进行中:当前仅构造,尚未接入各生成步骤） */
    private lateinit var symmetry: MapSymmetry

    private val terrainConditions = ruleset.terrains.values.asSequence()
        .filter { !it.hasUnique(UniqueType.NoNaturalGeneration)}
        .flatMap { it.getGenerationConditions() }
        .toList()
        .sortedBy { it.isConstrained }
    private val baseTerrainPicker = terrainConditions
        .filter{ it.terrain.type == TerrainType.Land || it.terrain.type == TerrainType.Water }
    private val terrainFeaturePicker = terrainConditions
        .filter{ it.terrain.type == TerrainType.TerrainFeature }

    /** Associates [terrain] with a range of temperatures and a range of humidities (both open to closed) */
    internal class TerrainOccursRange(
        val terrain: Terrain,
        val tempFrom: Float, val tempTo: Float,
        val humidFrom: Float, val humidTo: Float,
        val isConstrained: Boolean
    ) {
        val name get() = terrain.name
        val occursInChains: Boolean = terrain.hasUnique(UniqueType.OccursInChains)
        val occursInGroups: Boolean = terrain.hasUnique(UniqueType.OccursInGroups)
        val hasVegitation: Boolean = terrain.hasUnique(UniqueType.Vegetation)
        val isRough: Boolean get() = terrain.isRough
        val isFreshwater: Boolean get() = terrain.isFreshwater
        val isCoast: Boolean get() = terrain.isCoast
        val isOcean: Boolean get() = terrain.isOcean
        val rareFeature: Boolean = terrain.hasUnique(UniqueType.RareFeature)
        
        /** builds a [TerrainOccursRange] for [terrain] from a [unique] (type [UniqueType.TileGenerationConditions]) */
        constructor(terrain: Terrain, unique: Unique)
                : this(terrain,
            unique.params[0].toFloatMakeInclusive(-1f), unique.params[1].toFloat(),
            unique.params[2].toFloatMakeInclusive(0f), unique.params[3].toFloat(),
            isConstrained = true)
        
        /** builds a [TerrainOccursRange] for [terrain] without a [unique] (type [UniqueType.TileGenerationConditions]) */
        constructor(terrain: Terrain)
            : this(terrain, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, isConstrained = false)
        
        
        private fun matchesBaseTerrain(tile: Tile): Boolean {
            return if (terrain.type == TerrainType.Water) 
                    tile.getBaseTerrain().type == TerrainType.Water
                        && tile.getBaseTerrain().isFreshwater == isFreshwater
                        && tile.getBaseTerrain().isCoast == isCoast
                else if (terrain.type == TerrainType.Land)
                    tile.getBaseTerrain().type == TerrainType.Land
                        && tile.getBaseTerrain().hasUnique(UniqueType.OccursInChains) == occursInChains
                else
                    terrain.occursOn.contains(tile.lastTerrain.name)
        }
        
        /** Checks if both [temperature] and [humidity] satisfy their ranges (>From, <=To)
         *  Note the lowest allowed limit has been made inclusive (temp -1 nudged down by 1 [Float.ulp], humidity at 0)
         */
        // Yes this does implicit conversions Float/Double
        fun matchesHumidityAndTemp(tile: Tile) = matchesHumidity(tile, tile.temperature?:0.0)
        
        fun matchesHumidity(tile: Tile, overrideTileTemp: Double) =
            tempFrom < overrideTileTemp && overrideTileTemp <= tempTo &&
                humidFrom < (tile.humidity ?: 0.0) && (tile.humidity?:0.0) <= humidTo
        
        fun matchesTempAndTerrain(tile: Tile) = matchesBaseTerrain(tile) && matchesHumidityAndTemp(tile)
        
        fun matchesTempAndTerrain(tile: Tile, overrideTileTemp: Double) = matchesBaseTerrain(tile) && matchesHumidity(tile, overrideTileTemp)

        fun maybeSnow() = terrain.type == TerrainType.Land 
            && tempFrom <= -1 && tempTo <= -.5 
            && humidFrom >= -.1 && humidTo >= 1 
            && !rareFeature

        override fun toString(): String {
            return name
        }

        companion object {
            /** A [toFloat] that also nudges the value slightly down if it matches [limit] to make the resulting range inclusive on the lower end */
            private fun String.toFloatMakeInclusive(limit: Float): Float {
                val result = toFloat()
                if (result != limit) return result
                return result - result.ulp
            }
        }
    }
    private fun Terrain.getGenerationConditions() =
        getMatchingUniques(UniqueType.TileGenerationConditions)
            .map { unique -> TerrainOccursRange(this, unique) }
            .ifEmpty { sequenceOf(TerrainOccursRange(this)) }

    fun generateMap(mapParameters: MapParameters, gameParameters: GameParameters = GameParameters(), gameInfo: GameInfo? = null): TileMap {
        val mapSize = mapParameters.mapSize
        val mapType = mapParameters.type

        if (mapParameters.seed == 0L)
            mapParameters.seed = System.currentTimeMillis()

        // Symmetry constraints: hexagonal shape only, odd radius required for center tile
        if (mapParameters.symmetryMode != SymmetryMode.none) {
            if (mapParameters.shape != MapShape.hexagonal)
                mapParameters.shape = MapShape.hexagonal
            if (mapParameters.mapSize.radius % 2 == 0)
                mapParameters.mapSize = MapSize(mapParameters.mapSize.radius + 1)
        }

        randomness.seedRNG(mapParameters.seed)

        val map: TileMap = if (mapParameters.shape == MapShape.rectangular)
            TileMap(mapSize.width, mapSize.height, ruleset, mapParameters.worldWrap)
        else
            TileMap(mapSize.radius, ruleset, mapParameters.worldWrap)

        mapParameters.createdWithVersion = UncivGame.VERSION.toSerializeString()
        map.mapParameters = mapParameters
        symmetry = MapSymmetry(map, mapParameters.symmetryMode)

        if (mapType == MapType.empty) {
            for (tile in map.values) {
                tile.baseTerrain = Constants.ocean
                tile.setTerrainTransients()
            }

            return map
        }

        if (consoleTimings) debug("\nMapGenerator run with parameters %s", mapParameters)
        runAndMeasure("MapLandmassGenerator") {
            MapLandmassGenerator(map, ruleset, randomness, symmetry).generateLand()
        }
        runAndMeasure("applyHumidityAndTemperature") {
            applyHumidityAndTemperature(map)
        }
        runAndMeasure("raiseMountainsAndHills") {
            MapElevationGenerator(map, ruleset, terrainConditions, randomness, symmetry).raiseMountainsAndHills()
        }
        // 陆地/温湿/起伏已按规范扇区生成并即时同步轨道 —— 无需再事后强制地形对称
        // (原 Phase 1 调用已移除;Phase 1b 待湖泊/植被/冰改造后移除)

        runAndMeasure("spawnLakesAndCoasts") {
            spawnLakesAndCoasts(map)
        }
        runAndMeasure("spawnVegetation") {
            spawnVegetation(map)
        }
        runAndMeasure("spawnRareFeatures") {
            spawnRareFeatures(map)
        }
        runAndMeasure("spawnIce") {
            spawnIce(map)
        }
        // 植被/冰/稀有特征已按规范扇区生成并即时同步轨道
        // (原 Phase 1b 调用已移除)

        runAndMeasure("assignContinents") {
            map.assignContinents(TileMap.AssignContinentsMode.Assign)
        }

        runAndMeasure("RiverGenerator") {
            RiverGenerator(map, randomness, ruleset).spawnRivers()
        }
        // 河流生成后立即把边同步到轨道,后续阶段(convertTerrains 等)在对称状态上进行
        if (symmetry.isActive)
            symmetry.synchronizeRivers()
        convertTerrains(map.values)

        // Phase 2: 全图对称同步(完整轨道映射)在区域分配之前执行,
        // 使起始位置归一化在对称地形上进行。
        runAndMeasure("enforceSymmetry") {
            enforceSymmetry(map)
        }

        // Region based map generation - not used when generating maps in map editor
        val civilizations = gameInfo?.civilizations
        val isMapEditor = civilizations?.isEmpty() ?: true
        if (! isMapEditor) {
            map.gameInfo = gameInfo
            val regions = MapRegions(ruleset)
            runAndMeasure("generateRegions") {
                regions.generateRegions(map, civilizations.count { ruleset.nations[it.civName]!!.isMajorCiv })
            }
            runAndMeasure("assignRegions") {
                regions.assignRegions(map, civilizations.filter { ruleset.nations[it.civName]!!.isMajorCiv }, gameParameters)
            }
            // Natural wonders need to go before most resources since there is a minimum distance
            runAndMeasure("NaturalWonderGenerator") {
                NaturalWonderGenerator(ruleset, randomness).spawnNaturalWonders(map)
            }
            runAndMeasure("placeResourcesAndMinorCivs") {
                regions.placeResourcesAndMinorCivs(map, civilizations.filter { ruleset.nations[it.civName]!!.isCityState })
            }
        } else {
            runAndMeasure("NaturalWonderGenerator") {
                NaturalWonderGenerator(ruleset, randomness).spawnNaturalWonders(map)
            }
            // Fallback spread resources function - used when generating maps in map editor
            runAndMeasure("spreadResources") { spreadResources(map) }
        }

        runAndMeasure("spreadAncientRuins") { spreadAncientRuins(map) }
        // Move non-canonical starts to canonical positions so each civ's start
        // is on a "master" tile whose symmetric partners have identical terrain
        if (map.mapParameters.symmetryMode != SymmetryMode.none)
            distributeStartingLocations(map)

        // Phase 3: 全部生成步骤完成后的最终对称同步(完整映射,一次到位)
        if (map.mapParameters.symmetryMode != SymmetryMode.none) {
            enforceSymmetry(map)
        } else {
            mirror(map)
        }

        if (isMapEditor)
            mirror(map)
        
        // Map generation may generate incompatible terrain/feature combinations
        for (tile in map.values)
            TileNormalizer.normalizeToRuleset(tile, ruleset)

        return map
    }
    
    private fun flipTopBottom(vector: HexCoord): HexCoord = HexCoord.of(-vector.y, -vector.x)
    private fun flipLeftRight(vector: HexCoord): HexCoord = HexCoord.of(vector.y, vector.x)

    // region Symmetry rotation functions
    /** Rotate axial hex coordinate 60° clockwise around origin.
     *  Derived from cubic rotation (a,b,c) -> (-c,-a,-b). */
    private fun rotate60(coord: HexCoord): HexCoord =
        HexCoord.of(coord.x - coord.y, coord.x)

    /** Rotate axial hex coordinate 120° clockwise around origin. */
    private fun rotate120(coord: HexCoord): HexCoord =
        HexCoord.of(-coord.y, coord.x - coord.y)

    /** Rotate axial hex coordinate 180° around origin. */
    private fun rotate180(coord: HexCoord): HexCoord =
        HexCoord.of(-coord.x, -coord.y)

    /** Return all symmetric partners (excluding self) for [coord] under [mode]. */
    private fun symmetryPartners(coord: HexCoord, mode: String): List<HexCoord> = when (mode) {
        SymmetryMode.twoFold -> listOf(rotate180(coord))
        SymmetryMode.threeFold -> {
            val r120 = rotate120(coord)
            listOf(r120, rotate120(r120))
        }
        SymmetryMode.sixFold -> {
            val r1 = rotate60(coord)
            val r2 = rotate60(r1)
            val r3 = rotate60(r2)
            val r4 = rotate60(r3)
            val r5 = rotate60(r4)
            listOf(r1, r2, r3, r4, r5)
        }
        else -> emptyList()
    }

    /** True if [coord] is the lexicographically smallest among its symmetry group,
     *  i.e. it belongs to the basic sector (wedge). */
    private fun isCanonicalCoord(coord: HexCoord, mode: String): Boolean {
        val partners = symmetryPartners(coord, mode)
        return partners.all { coord.x < it.x || (coord.x == it.x && coord.y <= it.y) }
    }

    /** Compute rotation steps (in units of 60°) from canonical [from] to [to]. */
    private fun rotationSteps(from: HexCoord, to: HexCoord, mode: String): Int = when (mode) {
        SymmetryMode.twoFold -> 3
        SymmetryMode.threeFold -> if (rotate120(from) == to) 2 else 4
        SymmetryMode.sixFold -> {
            var cur = from
            for (s in 1..5) { cur = rotate60(cur); if (cur == to) return s }
            0
        }
        else -> 0
    }

    /** Rotate a clock-position direction by [steps] × 60°.
     *  Clock positions: 2=TR, 4=BR, 6=B, 8=BL, 10=TL, 12=T. */
    private fun rotateDirection(clockPos: Int, steps: Int): Int =
        ((clockPos - 2 + steps * 2) % 12 + 12) % 12 + 2
    // endregion

    /** 全图对称同步:从规范格复制全字段到轨道成员,再统一同步河流边。
     *  作为阶段兜底与最终校验前的收口(完整轨道映射,无需二次执行)。 */
    private fun enforceSymmetry(map: TileMap) {
        if (!symmetry.isActive) return
        for (tile in symmetry.canonicalTiles)
            stampSector(tile)
        symmetry.synchronizeRivers()
    }

    private fun mirror(map: TileMap) {
        val mirroringType = map.mapParameters.mirroring
        
        fun getMirrorTile(tile: Tile): Tile? {
            val mirrorTileVector = when (mirroringType) {
                MirroringType.topbottom -> if (tile.getRow() <= 0) return null else flipTopBottom(tile.position)
                MirroringType.leftright -> if (tile.getColumn() <= 0) return null else flipLeftRight(tile.position)
                MirroringType.aroundCenterTile -> if (tile.getRow() <= 0) return null else flipLeftRight(flipTopBottom(tile.position))
                MirroringType.fourway -> when {
                    tile.getRow() < 0 && tile.getColumn() < 0 -> return null
                    tile.getRow() < 0 && tile.getColumn() >= 0 -> flipLeftRight(tile.position)
                    tile.getRow() >= 0 && tile.getColumn() < 0 -> flipTopBottom(tile.position)
                    else -> flipLeftRight(flipTopBottom(tile.position))
                }

                else -> return null
            }
            return map.getIfTileExistsOrNull(mirrorTileVector.x, mirrorTileVector.y)
        }
        
        fun copyTile(tile: Tile) {
            val mirrorTile = getMirrorTile(tile) ?: return
            
            tile.setBaseTerrain(mirrorTile.getBaseTerrain())
            tile.naturalWonder = mirrorTile.naturalWonder
            tile.setTerrainFeatures(mirrorTile.terrainFeatures)
            tile.tileResource = mirrorTile.tileResource
            tile.resourceAmount = mirrorTile.resourceAmount
            tile.setImprovementBasic(mirrorTile.tileImprovement)
            
            for (neighbor in tile.neighbors){
                val neighborMirror = getMirrorTile(neighbor) ?: continue
                if (neighborMirror !in mirrorTile.neighbors) continue // we landed on the edge here
                tile.setConnectedByRiver(neighbor, mirrorTile.isConnectedByRiver(neighborMirror))
            }
        }

        if (map.mapParameters.mirroring == MirroringType.none) return
        for (tile in map.values) {
            copyTile(tile)
        }
    }

    private fun buildCanonicalMapping(map: TileMap, mode: String): Map<Tile, Pair<Tile, Int>> {
        val result = LinkedHashMap<Tile, Pair<Tile, Int>>()  // deterministic iteration order
        for (tile in map.values) {
            val coord = tile.position
            if (isCanonicalCoord(coord, mode)) continue

            val partners = symmetryPartners(coord, mode)
            val allCoords = listOf(coord) + partners
            val bestCoord = allCoords.minWith(compareBy({ it.x }, { it.y }))
            if (bestCoord == coord) continue

            val canonicalTile = map.getIfTileExistsOrNull(bestCoord.x, bestCoord.y) ?: continue
            val steps = rotationSteps(bestCoord, coord, mode)
            result[tile] = canonicalTile to steps
        }
        return result
    }

    /** Phase 1: Enforce terrain/feature symmetry BEFORE rivers are generated.
     *  Called after assignContinents, before RiverGenerator. */
    private fun applyTerrainSymmetry(map: TileMap) {
        val mode = map.mapParameters.symmetryMode
        if (mode == SymmetryMode.none) return

        // Clear all river flags — rivers will be regenerated and then symmetrized
        for (tile in map.values) {
            tile.hasBottomRightRiver = false
            tile.hasBottomRiver = false
            tile.hasBottomLeftRiver = false
        }

        val mapping = buildCanonicalMapping(map, mode)
        for ((target, pair) in mapping) {
            val (source, _) = pair
            // Copy terrain using the same pattern as the existing mirror() function
            target.setBaseTerrain(source.getBaseTerrain())
            target.setTerrainFeatures(source.terrainFeatures)
            target.temperature = source.temperature
            target.humidity = source.humidity
            target.setTerrainTransients()
            // Ensure resulting combination is valid per ruleset
            TileNormalizer.normalizeToRuleset(target, target.ruleset)
        }
    }

    /** Comprehensive symmetry enforcement, called at the end of map generation.
     *  Copies all terrain, features, resources, natural wonders, and rivers
     *  from canonical tiles to their symmetric counterparts, with proper
     *  rotation of river edge directions.  Mirrors the approach of [mirror]. */
    private fun applySymmetry(map: TileMap) {
        val mode = map.mapParameters.symmetryMode
        if (mode == SymmetryMode.none) return

        val mapping = buildCanonicalMapping(map, mode)

        for ((target, pair) in mapping) {
            val (source, steps) = pair

            target.setBaseTerrain(source.getBaseTerrain())
            target.setTerrainFeatures(source.terrainFeatures)
            target.naturalWonder = source.naturalWonder
            target.tileResource = source.tileResource
            target.resourceAmount = source.resourceAmount
            target.setImprovementBasic(source.tileImprovement)
            target.temperature = source.temperature
            target.humidity = source.humidity

            // River edges: clear target first, then copy with rotation
            target.hasBottomRightRiver = false
            target.hasBottomRiver = false
            target.hasBottomLeftRiver = false

            for (neighbor in source.neighbors) {
                val clockPos = map.getNeighborTileClockPosition(source, neighbor)
                if (clockPos == -1) continue
                val rotatedClockPos = rotateDirection(clockPos, steps)
                val rotatedNeighbor =
                    map.getClockPositionNeighborTile(target, rotatedClockPos) ?: continue
                if (source.isConnectedByRiver(neighbor))
                    target.setConnectedByRiver(rotatedNeighbor, true)
            }

            target.setTerrainTransients()
            TileNormalizer.normalizeToRuleset(target, target.ruleset)
        }
    }

    /** Phase 1b: Re-symmetrize terrain features after vegetation/ice generation.
     *  Perlin noise may not be fully symmetric for 3-fold and 6-fold rotations.
     *  Copies only terrainFeatures from canonical to non-canonical tiles —
     *  baseTerrain (including coasts) is already symmetric from Phase 1. */
    private fun applyFeatureSymmetry(map: TileMap) {
        val mode = map.mapParameters.symmetryMode
        if (mode == SymmetryMode.none) return

        val mapping = buildCanonicalMapping(map, mode)
        for ((target, pair) in mapping) {
            val (source, _) = pair
            target.setTerrainFeatures(source.terrainFeatures)
            target.setTerrainTransients()
        }
    }

    /** After region assignment: reassign starting locations so civilizations
     *  are placed at symmetric positions across petals. Within each symmetry
     *  family (one canonical position), fills all F petals, then assigns the
     *  original nations across them. Surplus canonical groups are discarded. */
    private fun distributeStartingLocations(map: TileMap) {
        val mode = map.mapParameters.symmetryMode
        if (mode == SymmetryMode.none || map.startingLocations.isEmpty()) return

        val foldCount = when (mode) {
            SymmetryMode.twoFold -> 2
            SymmetryMode.threeFold -> 3
            SymmetryMode.sixFold -> 6
            else -> return
        }

        // Group starts by canonical coordinate, keep the best (first) start per petal
        val groups = LinkedHashMap<HexCoord, MutableMap<Int, TileMap.StartingLocation>>()
        for (loc in map.startingLocations) {
            val partners = symmetryPartners(loc.position, mode)
            val allCoords = listOf(loc.position) + partners
            val canonical = allCoords.minWith(compareBy({ it.x }, { it.y }))
            val step = rotationSteps(canonical, loc.position, mode)
            groups.getOrPut(canonical) { mutableMapOf() }
                .putIfAbsent(step, loc)
        }

        // Collect all nations from the original starts, preserving order
        val allNations = map.startingLocations.map { it.nation }

        // Pick canonical groups to fill. Each group provides exactly foldCount positions.
        // We need enough groups so that total positions >= number of nations.
        val sortedCanonicals = groups.keys.toList()  // preserve order (first-assigned = best)
        val result = mutableListOf<TileMap.StartingLocation>()
        var nationIdx = 0

        for (canonical in sortedCanonicals) {
            if (nationIdx >= allNations.size) break

            for (step in 0 until foldCount) {
                if (nationIdx >= allNations.size) break
                // step 0 = canonical, step s > 0 = s * (6/foldCount) × 60°
                // 2-fold: 0° → 180°.  3-fold: 0° → 120° → 240°.  6-fold: 0° → 60° → 120°...
                val rotateTimes = step * (6 / foldCount)
                val pos = if (rotateTimes == 0) canonical
                    else (1..rotateTimes).fold(canonical) { c, _ -> rotate60(c) }
                if (!map.contains(pos.x, pos.y)) continue
                val tile = map.getIfTileExistsOrNull(pos.x, pos.y) ?: continue
                if (!tile.isLand || tile.isImpassible()) continue

                result.add(TileMap.StartingLocation(
                    position = pos,
                    nation = allNations[nationIdx],
                    usage = map.startingLocations.firstOrNull { it.nation == allNations[nationIdx] }?.usage
                        ?: TileMap.StartingLocation.Usage.Player
                ))
                nationIdx++
            }
        }

        // If we didn't fill all nations (unlikely), keep remaining as-is
        if (nationIdx < allNations.size) {
            for (loc in map.startingLocations) {
                if (result.none { it.nation == loc.nation })
                    result.add(loc)
            }
        }

        map.startingLocations.clear()
        map.startingLocations.addAll(result)
    }

    fun generateSingleStep(map: TileMap, step: MapGeneratorSteps) {
        if (map.mapParameters.seed == 0L)
            map.mapParameters.seed = System.currentTimeMillis()

        symmetry = MapSymmetry(map, map.mapParameters.symmetryMode)
        randomness.seedRNG(map.mapParameters.seed)

        runAndMeasure("SingleStep $step") {
            when (step) {
                MapGeneratorSteps.None -> Unit
                MapGeneratorSteps.All -> throw IllegalArgumentException("MapGeneratorSteps.All cannot be used in generateSingleStep")
                MapGeneratorSteps.Landmass -> MapLandmassGenerator(map, ruleset, randomness, symmetry).generateLand()
                MapGeneratorSteps.HumidityAndTemperature -> applyHumidityAndTemperature(map)
                MapGeneratorSteps.Elevation -> MapElevationGenerator(map, ruleset, terrainConditions, randomness, symmetry).raiseMountainsAndHills()
                MapGeneratorSteps.LakesAndCoast -> spawnLakesAndCoasts(map)
                MapGeneratorSteps.Vegetation -> spawnVegetation(map)
                MapGeneratorSteps.RareFeatures -> spawnRareFeatures(map)
                MapGeneratorSteps.Ice -> spawnIce(map)
                MapGeneratorSteps.Continents -> map.assignContinents(TileMap.AssignContinentsMode.Reassign)
                MapGeneratorSteps.NaturalWonders -> NaturalWonderGenerator(ruleset, randomness).spawnNaturalWonders(map)
                MapGeneratorSteps.Rivers -> {
                    val resultingTiles = mutableSetOf<Tile>()
                    RiverGenerator(map, randomness, ruleset).spawnRivers(resultingTiles)
                    convertTerrains(resultingTiles)
                }
                MapGeneratorSteps.Resources -> spreadResources(map)
                MapGeneratorSteps.AncientRuins -> spreadAncientRuins(map)
                MapGeneratorSteps.Symmetry -> {
                    applyTerrainSymmetry(map)
                    applyFeatureSymmetry(map)
                    applySymmetry(map)
                }
            }
        }
    }


    private fun runAndMeasure(text: String, action: ()->Unit) {
        if (coroutineScope?.isActive == false) return
        if (!consoleTimings) return action()
        val startNanos = System.nanoTime()
        action()
        val delta = System.nanoTime() - startNanos
        debug("MapGenerator.%s took %s.%sms", text, delta/1000000L, (delta/10000L).rem(100))
    }

    fun convertTerrains(tiles: Iterable<Tile>) = Helpers.convertTerrains(ruleset, tiles)
    object Helpers {
        fun convertTerrains(ruleset: Ruleset, tiles: Iterable<Tile>) {
            for (tile in tiles) {
                val conversionUnique =
                    tile.getBaseTerrain().getMatchingUniques(UniqueType.ChangesTerrain, GameContext(tile = tile))
                        .firstOrNull { tile.isAdjacentTo(it.params[1]) }
                        ?: continue
                val terrain = ruleset.terrains[conversionUnique.params[0]] ?: continue

                if (terrain.type != TerrainType.TerrainFeature)
                    tile.baseTerrain = terrain.name
                else if (!terrain.occursOn.contains(tile.lastTerrain.name)) continue
                else
                    tile.addTerrainFeature(terrain.name)
                tile.setTerrainTransients()
            }
        }
    }

    private fun spreadCoast(map: TileMap, coasts: List<TerrainOccursRange>) {
        repeat(map.mapParameters.maxCoastExtension) {
            val toCoast = mutableListOf<Tile>()
            // 决策只在规范格做(neighbor 判定在对称网格上对轨道等价;RNG 每轨道只消费一次)
            for (tile in canonicalTiles(map)) {
                if (!tile.isOcean) continue
                var shouldCoast = false
                for (neighborTile in tile.getTilesInDistance(1)) {
                    if (neighborTile.isLand) {
                        shouldCoast = true
                        break
                    } else if (neighborTile.getBaseTerrain().isCoast) {
                        val randbool = randomness.RNG.nextBoolean()
                        if (randbool) {
                            shouldCoast = true
                        }
                        break
                    }
                }
                if (shouldCoast) toCoast.add(tile)
            }
            for (tile in toCoast) {
                val coast = coasts.filter { it.matchesHumidityAndTemp(tile) }.ifEmpty { coasts }.random(randomness.RNG)
                tile.baseTerrain = coast.name
                tile.setTransients()
                stampClimate(tile)
            }
        }
    }

    private fun spawnLakesAndCoasts(map: TileMap) {
        val lakeTerrains = terrainConditions.filter { it.isFreshwater && !it.rareFeature && it.terrain.type == TerrainType.Water }
        if (lakeTerrains.isNotEmpty()) {
            //define lakes
            val waterTiles = map.values.filter { it.isWater }.toHashSet()

            val tilesInArea = HashSet<Tile>()
            val tilesToCheck = ArrayDeque<Tile>()

            val maxLakeSize = ruleset.modOptions.constants.maxLakeSize

            // 对称模式下,聚类本身天然对称(地形对称);同一轨道上的湖簇共享一次随机抽取,
            // 保证各扇区的湖地形一致
            val lakeTerrainByOrbit = HashMap<Tile, TerrainOccursRange>()

            while (waterTiles.isNotEmpty()) {
                val initialWaterTile = waterTiles.first()
                waterTiles.remove(initialWaterTile)
                tilesInArea += initialWaterTile
                tilesToCheck += initialWaterTile

                // Floodfill to cluster water tiles
                while (tilesToCheck.isNotEmpty()) {
                    val tileWeAreChecking = tilesToCheck.removeFirst()
                    for (vector in tileWeAreChecking.neighbors){
                        if (vector in tilesInArea) continue
                        if (vector !in waterTiles) continue
                        tilesInArea += vector
                        tilesToCheck += vector
                        waterTiles -= vector
                    }
                }

                val lakeTerrain = if (symmetry.isActive) {
                    val canonical = symmetry.canonicalOf(initialWaterTile)
                    lakeTerrainByOrbit.getOrPut(canonical) {
                        lakeTerrains.filter { it.matchesHumidityAndTemp(initialWaterTile) }
                            .ifEmpty {lakeTerrains}
                            .random(randomness.RNG)
                    }
                } else {
                    lakeTerrains.filter { it.matchesHumidityAndTemp(initialWaterTile) }
                        .ifEmpty {lakeTerrains}
                        .random(randomness.RNG)
                }
                if (tilesInArea.size <= maxLakeSize) {
                    for (tile in tilesInArea) {
                        tile.baseTerrain = lakeTerrain.name
                        tile.setTransients()
                    }
                }
                tilesInArea.clear()
            }
        }

        //Coasts
        val coasts = terrainConditions.filter { it.isCoast && !it.rareFeature }
        if (coasts.isNotEmpty()) {
            spreadCoast(map, coasts)
        }
    }

    private fun spreadAncientRuins(map: TileMap) {
        val ruinsEquivalents = ruleset.tileImprovements.filter {
            it.value.isAncientRuinsEquivalent()
        }
        if (map.mapParameters.noRuins || ruinsEquivalents.isEmpty()) return

        fun isPlaceable(improvement: TileImprovement, tile: Tile) =
            tile.improvementFunctions.canImprovementBeBuiltHere(improvement, gameContext = GameContext.IgnoreConditionals)

        val suitableTiles = map.values.filter { it.isLand && !it.isImpassible()
                && ruinsEquivalents.values.any { improvement -> isPlaceable(improvement, it) } }
        val locations = randomness.chooseSpreadOutLocations(
                (suitableTiles.size * ruleset.modOptions.constants.ancientRuinCountMultiplier).roundToInt(),
                suitableTiles,
                map.mapParameters.mapSize.radius)
        for (tile in locations) {
            val rng = GameContext(tile = tile).stateBasedRandom("MapGenerator.spreadAncientRuins")
            val ruins = ruinsEquivalents.values.filter { isPlaceable(it, tile) }.random(rng)
            tile.setImprovementBasic(ruins)
        }
    }

    private fun spreadResources(tileMap: TileMap) {
        val mapRadius = tileMap.mapParameters.mapSize.radius
        for (tile in tileMap.values)
            tile.tileResource = null

        val snowTerrains = terrainConditions.filter { it.maybeSnow()}
        spreadStrategicResources(tileMap, mapRadius, snowTerrains)
        spreadResources(tileMap, mapRadius, ResourceType.Luxury, snowTerrains)
        spreadResources(tileMap, mapRadius, ResourceType.Bonus, snowTerrains)
    }

    // Here, we need each specific resource to be spread over the map - it matters less if specific resources are near each other
    private fun spreadStrategicResources(tileMap: TileMap, mapRadius: Int, snowTerrains: List<TerrainOccursRange>) {
        val strategicResources = ruleset.tileResources.values.filter { it.resourceType == ResourceType.Strategic }
        // passable land tiles (no mountains, no wonders) without resources yet
        // can't be next to NW
        val candidateTiles = tileMap.values.filter { it.resource == null && !it.isImpassible()
                && it.neighbors.none { neighbor -> neighbor.isNaturalWonder() }}
        val totalNumberOfResources = candidateTiles.count { it.isLand } * tileMap.mapParameters.resourceRichness
        val resourcesPerType = (totalNumberOfResources/strategicResources.size).toInt()
        for (resource in strategicResources) {
            // remove the tiles where previous resources have been placed
            val suitableTiles = candidateTiles
                    .filterNot { snowTerrains.any {terrain -> it.baseTerrain == terrain.name } && it.isHill() }
                    .filter { it.resource == null && resource.generatesNaturallyOn(it) }

            val locations = randomness.chooseSpreadOutLocations(resourcesPerType, suitableTiles, mapRadius)

            for (location in locations) location.setTileResource(resource, rng = randomness.RNG)
        }
    }

    /**
     * Spreads resources of type [resourceType] picking locations at a minimum distance from each other,
     * which is determined from [mapRadius] and then tuned down until the desired number fits.
     * [MapParameters.resourceRichness] used to control how many resources to spawn.
     */
    private fun spreadResources(tileMap: TileMap, mapRadius: Int, resourceType: ResourceType, snowTerrains: List<TerrainOccursRange>) {
        val resourcesOfType = ruleset.tileResources.values.filter { it.resourceType == resourceType }

        val suitableTiles = tileMap.values
                .filterNot { snowTerrains.any {terrain -> it.baseTerrain == terrain.name } && it.isHill() }
                .filter { it.resource == null
                    && it.neighbors.none { neighbor -> neighbor.isNaturalWonder() }
                    && resourcesOfType.any { r -> r.generatesNaturallyOn(it) }
                }
        val numberOfResources = tileMap.values.count { it.isLand && !it.isImpassible() } *
                tileMap.mapParameters.resourceRichness
        val locations = randomness.chooseSpreadOutLocations(numberOfResources.toInt(), suitableTiles, mapRadius)

        val resourceToNumber = Counter<String>()

        for (tile in locations) {
            val possibleResources = resourcesOfType.filter { it.generatesNaturallyOn(tile) }
            if (possibleResources.isEmpty()) continue
            val resourceWithLeastAssignments = possibleResources.minByOrNull { resourceToNumber[it.name] }!!
            resourceToNumber.add(resourceWithLeastAssignments.name, 1)
            tile.setTileResource(resourceWithLeastAssignments, rng = randomness.RNG)
        }
    }


    /**
     * [MapParameters.tilesPerBiomeArea] to set biomes size
     * [MapParameters.temperatureintensity] to favor very high and very low temperatures
     * [MapParameters.temperatureShift] to shift temperature towards cold (negative) or hot (positive)
     */
    /** 规范扇区迭代源:对称模式只轮询规范格,非对称退化为全图(行为与改造前一致) */
    private fun canonicalTiles(tileMap: TileMap): Iterable<Tile> =
        if (symmetry.isActive) symmetry.canonicalTiles else tileMap.values

    /** 把规范格的完整生成状态同步到整个轨道(与装饰步骤共用) */
    private fun stampSector(canonical: Tile) {
        if (!symmetry.isActive) return
        val orbit = symmetry.orbitOf(canonical) ?: return
        for ((steps, member) in orbit.members) {
            if (member === canonical) continue
            symmetry.stampInto(member, canonical, steps)
        }
    }

    /** 把规范格的温度/湿度/地形决策同步到整个轨道 */
    private fun stampClimate(canonical: Tile) {
        if (!symmetry.isActive) return
        val orbit = symmetry.orbitOf(canonical) ?: return
        for ((_, member) in orbit.members) {
            if (member === canonical) continue
            member.temperature = canonical.temperature
            member.humidity = canonical.humidity
            member.baseTerrain = canonical.baseTerrain
            member.setTerrainTransients()
        }
    }

    private fun applyHumidityAndTemperature(tileMap: TileMap) {
        val humiditySeed = randomness.RNG.nextInt().toDouble()
        val temperatureSeed = randomness.RNG.nextInt().toDouble()

        tileMap.setTransients(ruleset)

        val scale = tileMap.mapParameters.tilesPerBiomeArea.toDouble()
        val temperatureintensity = tileMap.mapParameters.temperatureintensity
        val temperatureShift = tileMap.mapParameters.temperatureShift
        val humidityShift = if (temperatureShift > 0) -temperatureShift / 2 else 0f

        // List is OK here as it's only sequentially scanned
        val landTerrains = baseTerrainPicker.filter { it.terrain.type == TerrainType.Land && !it.terrain.impassable && !it.isRough && !it.rareFeature }
        val coastTerrains = baseTerrainPicker.filter { it.terrain.isCoast && !it.rareFeature }
        val oceanTerrains = baseTerrainPicker.filter { it.terrain.isOcean && !it.rareFeature }
        val noTerrainUniques = landTerrains.none { it.isConstrained}
        val elevationTerrains = baseTerrainPicker.filter {  it.occursInChains }.mapTo(mutableSetOf()) { it.name }
        
        /**
         * @return Temperature at the provided tile before adding noise etc..
         * May be outside of range -1.0 to 1.0, so make sure to coerce it at some point.
         */
        fun getExpectedTemperature(tile: Tile): Double {
            /** Latitude in range -1.0 (south pole equivalent) to +1.0 (north pole equivalent). */
            val normalizedLatitude =
                if (tileMap.mapParameters.shape == MapShape.flatEarth) 2 * getTileRadius(tile, tileMap) - 1
                else tile.latitude.toDouble() / tileMap.maxLatitude
            /**
             * Flat earth temperature should be -1.0 at latitudes ±0.9, and -1.111 at latitudes ±1.0.
             * Instead of adjusting the temperature later, we can adjust the latitude here.
             * This way, custom map types don't have to worry as much about flat earth logic.
             */
            val adjustedLatitude = 
                if (tileMap.mapParameters.shape == MapShape.flatEarth) normalizedLatitude * 10.0 / 9.0
                else normalizedLatitude
            /** This part translates from latitude to temperature. */
            return when (tileMap.mapParameters.type) {
                // Starting temperature is -0.4 across most (southern ~75%) of the map, but declines to -1.0 in the north so ice can spawn
                MapType.boreal -> -0.4 - 0.6 * E.pow(5 * adjustedLatitude - 5)
                /** Cold poles, warm equator. Most map types use this function. */
                else -> 1.0 - 2.0 * abs(adjustedLatitude)
            }
        }

        for (tile in canonicalTiles(tileMap)) {
            if (tile.baseTerrain in elevationTerrains) {
                stampClimate(tile)
                continue
            }

            val humidityRandom = randomness.getPerlinNoise(tile, humiditySeed, scale = scale, nOctaves = 1)
            val humidity = ((humidityRandom + 1.0) / 2.0 + humidityShift).coerceIn(0.0..1.0)
            tile.humidity = humidity

            val expectedTemperature = getExpectedTemperature(tile)

            val randomTemperature = randomness.getPerlinNoise(tile, temperatureSeed, scale = scale, nOctaves = 1)
            var temperature = (5.0 * expectedTemperature + randomTemperature) / 6.0
            temperature = abs(temperature).pow(1.0 - temperatureintensity) * temperature.sign
            temperature = (temperature + temperatureShift).coerceIn(-1.0..1.0)
            tile.temperature = temperature

            // Old, static map generation rules - necessary for existing base ruleset mods to continue to function
            if (noTerrainUniques) {
                val autoTerrain = when {
                    temperature < -0.4 -> if (humidity < 0.5) Constants.snow else Constants.tundra
                    temperature < 0.8 -> if (humidity < 0.5) Constants.plains else Constants.grassland
                    temperature <= 1.0 -> if (humidity < 0.7) Constants.desert else Constants.plains
                    else -> {
                        debug("applyHumidityAndTemperature: Invalid temperature %s", temperature)
                        stampClimate(tile)
                        continue
                    }
                }
                if (ruleset.terrains.containsKey(autoTerrain)) {
                    tile.baseTerrain = autoTerrain
                    tile.setTerrainTransients()
                }
                stampClimate(tile)
                continue
            }

            val terrains = 
                if (tile.isLand) landTerrains 
                else if (tile.getBaseTerrain().isCoast) coastTerrains
                else oceanTerrains
            val matchingTerrain = terrains.filter{ it.matchesTempAndTerrain(tile) }.ifEmpty { terrains }.randomOrNull(randomness.RNG)

            if (matchingTerrain != null) {
                tile.baseTerrain = matchingTerrain.name
                tile.setTerrainTransients()
            } else {
                debug("applyHumidityAndTemperature: No terrain found for temperature: %s, humidity: %s", temperature, humidity)
            }
            stampClimate(tile)
        }
    }

    /**
     * [MapParameters.vegetationRichness] is the threshold for vegetation spawn
     */
    private fun spawnVegetation(tileMap: TileMap) {
        val vegetationSeed = randomness.RNG.nextInt().toDouble()
        val vegetationTerrains = terrainFeaturePicker.filter { it.hasVegitation && !it.rareFeature }
            .ifEmpty {terrainFeaturePicker.filter { Constants.vegetation.contains(it.name)}}
        val candidateTerrains = vegetationTerrains.flatMap{ it.terrain.occursOn }
        // some map types are more forested than others
        val vegetationRichness = tileMap.mapParameters.vegetationRichness + when (tileMap.mapParameters.type) {
            MapType.boreal -> +0.10
            else -> 0.0
        }
        // 植被决策只在规范格做:噪声按规范坐标评估,特征随机每轨道一次,随后同步轨道
        for (tile in canonicalTiles(tileMap).filter { it.baseTerrain in candidateTerrains
                && it.lastTerrain.name in candidateTerrains }) {

            val vegetation = (randomness.getPerlinNoise(tile, vegetationSeed, scale = 3.0, nOctaves = 1) + 1.0) / 2.0

            if (vegetation <= vegetationRichness) {
                val possibleVegetation = vegetationTerrains.filter { it.matchesTempAndTerrain(tile)
                    && NaturalWonderGenerator.fitsTerrainUniques(it.terrain, tile)
                }
                if (possibleVegetation.isEmpty()) continue
                val randomVegetation = possibleVegetation.random(randomness.RNG)
                tile.hasVegetation = true
                tile.addTerrainFeature(randomVegetation.name)
                stampSector(tile)
            }
        }
    }

    /**
     * [MapParameters.rareFeaturesRichness] is the probability of spawning a rare feature
     */
    private fun spawnRareFeatures(tileMap: TileMap) {
        val rareFeatures = terrainFeaturePicker.filter { it.rareFeature }
        for (tile in canonicalTiles(tileMap).filter { it.terrainFeatures.isEmpty() }) {
            if (randomness.RNG.nextDouble() <= tileMap.mapParameters.rareFeaturesRichness) {
                val hillFeature = tile.getHillTerrain()
                val possibleFeatures = rareFeatures.filter { it.matchesTempAndTerrain(tile)
                    && (hillFeature == null || it.terrain.occursOn.contains(hillFeature.name))
                    && NaturalWonderGenerator.fitsTerrainUniques(it.terrain, tile)
                }
                if (possibleFeatures.any()) {
                    tile.addTerrainFeature(possibleFeatures.random(randomness.RNG).name)
                    stampSector(tile)
                }
            }
        }
    }

    /**
     * [MapParameters.temperatureintensity] as in [applyHumidityAndTemperature]
     */
    private fun spawnIce(tileMap: TileMap) {
        val oceanTerrains: List<TerrainOccursRange> = baseTerrainPicker.filter { it.terrain.isOcean && it.tempFrom<= -1 }
            .ifEmpty { terrainFeaturePicker.filter { it.terrain.isOcean} }
        val iceTerrains: List<TerrainOccursRange> = terrainFeaturePicker.filter { it.terrain.isIce }

        if (iceTerrains.isEmpty()) return

        if (tileMap.mapParameters.shape == MapShape.flatEarth) {
            spawnFlatEarthIceWalls(tileMap, iceTerrains, oceanTerrains)
        }

        tileMap.setTransients(ruleset)
        val temperatureSeed = randomness.RNG.nextInt().toDouble()
        for (tile in canonicalTiles(tileMap)) {
            if (oceanTerrains.none { it.name == tile.baseTerrain} || tile.terrainFeatures.isNotEmpty())
                continue

            val randomTemperature = randomness.getPerlinNoise(tile, temperatureSeed, scale = tileMap.mapParameters.tilesPerBiomeArea.toDouble(), nOctaves = 1)
            val latitudeTemperature = 1.0 - 2.0 * abs(tile.latitude) / tileMap.maxLatitude
            var iceTemperature = ((latitudeTemperature + randomTemperature) / 2.0)
            iceTemperature = abs(iceTemperature).pow(1.0 - tileMap.mapParameters.temperatureintensity) * iceTemperature.sign
            iceTemperature = (iceTemperature + tileMap.mapParameters.temperatureShift).coerceIn(-1.0..1.0)
            // This is quite different from the normal tile temperature. TODO: Unify

            val iceTerrain = iceTerrains
                .filter { it.matchesTempAndTerrain(tile, iceTemperature) 
                    && NaturalWonderGenerator.fitsTerrainUniques(it.terrain, tile)
                }.map { it.terrain.name }
                .randomOrNull(randomness.RNG)
            if (iceTerrain != null) {
                tile.addTerrainFeature(iceTerrain)
                stampSector(tile)
            }
        }
    }

    private fun spawnFlatEarthIceWalls(tileMap: TileMap, iceTerrains: List<TerrainOccursRange>, oceanTerrains: List<TerrainOccursRange>) {
        val snowTerrains = baseTerrainPicker.filter { it.maybeSnow() }
        val mountainTerrains = baseTerrainPicker.filter { it.terrain.impassable && it.occursInChains && !it.rareFeature }
        val allArcticTerrains = iceTerrains + snowTerrains + mountainTerrains

        // Skip the tile loop if nothing can be done in it
        if (allArcticTerrains.isEmpty()) return

        // Flat Earth needs a 1 tile wide perimeter of ice/mountain/snow and a 2 radius cluster of ice in the center.
        for (tile in tileMap.values) {
            val isCenterTile = tile.latitude == 0 && tile.longitude == 0
            val isEdgeTile = tile.neighbors.count() < 6

            // Make center tiles ice or snow or mountain depending on availability
            if (isCenterTile) {
                spawnFlatEarthCenterIceWall(tile, iceTerrains, mountainTerrains, oceanTerrains)
            }

            // Make edge tiles randomly ice or snow or mountain if available
            if (isEdgeTile) {
                spawnFlatEarthEdgeIceWall(tile, allArcticTerrains, iceTerrains)
            }
        }
    }
    
    private fun spawnBestIce(
        tile: Tile,
        iceTerrains: List<TerrainOccursRange>,
        mountainTerrains: List<TerrainOccursRange>,
        oceanTerrains: List<TerrainOccursRange>) {
        val ice = iceTerrains.filter { it.matchesTempAndTerrain(tile, -1.0) }.randomOrNull(randomness.RNG)
            ?: iceTerrains.randomOrNull(randomness.RNG)
        if (ice != null) {
            if (!ice.matchesTempAndTerrain(tile, -1.0)) {
                val fallbackBase = oceanTerrains.filter { ice.terrain.occursOn.contains (it.name) }
                    .ifEmpty { oceanTerrains.filter {it.isOcean} }
                    .randomOrNull(randomness.RNG)
                if (fallbackBase != null)
                    tile.baseTerrain = fallbackBase.name          
            }
            tile.removeTerrainFeatures()
            tile.addTerrainFeature(ice.terrain.name)
            tile.setTerrainTransients()
        } else {
            val mountain =
                mountainTerrains.filter { it.matchesHumidity(tile, -1.0) }.randomOrNull(randomness.RNG)
                    ?: mountainTerrains.random(randomness.RNG)
            tile.baseTerrain = mountain.terrain.name
            tile.removeTerrainFeatures()
        }
        tile.setTerrainTransients()
    }

    private fun spawnFlatEarthCenterIceWall(
        tile: Tile, 
        iceTerrains: List<TerrainOccursRange>, 
        mountainTerrains: List<TerrainOccursRange>,
        oceanTerrains: List<TerrainOccursRange>) {
        // Spawn ice on center tile
        spawnBestIce(tile, iceTerrains, mountainTerrains, oceanTerrains)

        // Spawn circle of ice around center tile
        for (neighbor in tile.neighbors) {
            spawnBestIce(neighbor, iceTerrains, mountainTerrains, oceanTerrains)

            // Spawn partial circle of ice around circle of ice
            for (neighbor2 in neighbor.neighbors) {
                // Do nothing most of the time at random.
                if (randomness.RNG.nextDouble() > 0.75)
                    spawnBestIce(neighbor2, iceTerrains, mountainTerrains, oceanTerrains)
            }
        }
    }

    private fun spawnRandomIce(tile: Tile, arcticTerrain: TerrainOccursRange, iceTerrains: List<TerrainOccursRange>) {
        tile.removeTerrainFeatures()
        if (arcticTerrain.terrain.type == TerrainType.Land && arcticTerrain.terrain.impassable) {
            tile.baseTerrain = arcticTerrain.terrain.name
        }  else if (arcticTerrain.terrain.type == TerrainType.Land) {
            // legacy code forced ice on snow even though that's impossible
            tile.baseTerrain = arcticTerrain.terrain.name
            tile.addTerrainFeature(iceTerrains.random(randomness.RNG).terrain.name)
        } else if (arcticTerrain.terrain.type == TerrainType.TerrainFeature && arcticTerrain.matchesTempAndTerrain(tile)) {
            tile.addTerrainFeature(arcticTerrain.terrain.name)                
        } else {
            // legacy code forced ice on ocean without checking if possible
            tile.baseTerrain = Constants.ocean
            tile.addTerrainFeature(arcticTerrain.terrain.name)
        }
        tile.setTerrainTransients()
    }

    private fun spawnFlatEarthEdgeIceWall(tile: Tile, arcticTerrains: List<TerrainOccursRange>, iceTerrains: List<TerrainOccursRange>) {
        val arcticTerrain = arcticTerrains.filter { it.matchesTempAndTerrain(tile, -1.0) }.randomOrNull(randomness.RNG)
            ?: arcticTerrains.filter { it.matchesHumidity(tile, -1.0) }.randomOrNull(randomness.RNG)
            ?: arcticTerrains.random(randomness.RNG)
        spawnRandomIce(tile, arcticTerrain, iceTerrains)

        // Spawn partial circle of arctic tiles next to the edge
        for (neighbor in tile.neighbors) {
            val neighborIsEdgeTile = neighbor.neighbors.count() < 6
            // Do not redo edge tile. It is already done.
            // Do nothing most of the time at random.
            if (!neighborIsEdgeTile && randomness.RNG.nextDouble() >= 0.75) {
                spawnRandomIce(tile, arcticTerrain, iceTerrains)
            }
        }
    }
}
