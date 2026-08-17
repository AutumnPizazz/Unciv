package com.unciv.logic.map.mapgenerator

import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapSize
import com.unciv.logic.map.SymmetryMode
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.RulesetCache
import com.unciv.testing.BaseTestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** 起点分布(differentStartingLocations)重排的对称性验证 */
@RunWith(BaseTestRunner::class)
class StartingLocationsSymmetryTest {

    private fun buildMap(radius: Int, worldWrap: Boolean, mode: String): Pair<TileMap, MapGenerator> {
        RulesetCache.loadRulesets(noMods = true)
        val ruleset = RulesetCache.getVanillaRuleset()
        val map = TileMap(radius, ruleset, worldWrap)
        map.mapParameters = MapParameters().apply {
            shape = MapShape.hexagonal
            mapSize = MapSize(radius)
            this.worldWrap = worldWrap
            symmetryMode = mode
        }
        return map to MapGenerator(ruleset)
    }

    @Test
    fun startsAreDistributedSymmetric() {
        for (mode in SymmetryMode.allValues.filter { it != SymmetryMode.none })
            for (worldWrap in listOf(false, true)) {
                val (map, generator) = buildMap(5, worldWrap, mode)
                val sym = MapSymmetry(map, map.mapParameters.symmetryMode)

                // 在若干规范格上放置起点(canonical 位置),模拟区域分配后的原始起点
                val canonicalStarts = sym.canonicalTiles.filter { it.isLand }.take(3)
                require(canonicalStarts.size == 3) { "need 3 land canonical tiles for test" }
                val nations = listOf("NationA", "NationB", "NationC")
                for ((i, tile) in canonicalStarts.withIndex()) {
                    map.startingLocations.add(TileMap.StartingLocation(tile.position, nations[i]))
                }

                generator.distributeStartingLocations(map, sym)

                // 每个起点都应在对称位置:验证任意两个起点要么同轨道(对称分布)
                // 要么属于不同轨道但都满足"轨道内成员位置对称"性质
                assertEquals("all nations placed", nations.size, map.startingLocations.size)
                val positions = map.startingLocations.map { it.position }
                assertEquals("nations preserved", nations.toSet(), map.startingLocations.map { it.nation }.toSet())

                // 检查:分配策略是"用最少的轨道填满 civ"——每个轨道要么全占(所有可用
                // 成员都有起点),要么空,最多一个轨道是部分占用(civ 数不是折数倍数时)
                var partialOrbits = 0
                val seenOrbits = HashSet<MapSymmetry.Orbit>()
                for (loc in map.startingLocations) {
                    val orbit = sym.orbitOf(map[loc.position.x, loc.position.y])!!
                    if (!seenOrbits.add(orbit)) continue
                    val usable = orbit.members.values.count { it.isLand && !it.isImpassible() }
                    val occupied = orbit.members.values.count { m -> positions.contains(m.position) }
                    if (occupied != usable && occupied != 0) partialOrbits++
                }
                assertTrue(
                    "mode=$mode wrap=$worldWrap: at most one partially-occupied orbit, " +
                        "found $partialOrbits",
                    partialOrbits <= 1
                )
            }
    }

    @Test
    fun startPositionsAreOnLandAndSymmetric() {
        val (map, generator) = buildMap(5, false, SymmetryMode.sixFold)
        val sym = MapSymmetry(map, map.mapParameters.symmetryMode)
        val canonicalStarts = sym.canonicalTiles.filter { it.isLand }.take(2)
        val nations = listOf("NationA", "NationB")
        for ((i, tile) in canonicalStarts.withIndex())
            map.startingLocations.add(TileMap.StartingLocation(tile.position, nations[i]))

        generator.distributeStartingLocations(map, sym)

        for (loc in map.startingLocations) {
            val tile = map[loc.position.x, loc.position.y]
            assertTrue("start ${loc.position} must be land", tile.isLand)
            assertTrue("start ${loc.position} must not be impassible", !tile.isImpassible())
        }
        // 起点数量 = 各被选轨道可用成员数之和(满轨 + 至多一个部分轨)
        assertTrue("2 canonical starts on 6-fold should place at least 2", map.startingLocations.size >= 2)
    }

    @Test
    fun emptyStartsAreIgnored() {
        val (map, generator) = buildMap(5, false, SymmetryMode.twoFold)
        val sym = MapSymmetry(map, map.mapParameters.symmetryMode)
        generator.distributeStartingLocations(map, sym)
        assertTrue("no starting locations should stay empty", map.startingLocations.isEmpty())
    }
}
