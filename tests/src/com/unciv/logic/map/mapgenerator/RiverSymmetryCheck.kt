package com.unciv.logic.map.mapgenerator

import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapSize
import com.unciv.logic.map.MapType
import com.unciv.logic.map.SymmetryMode
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.RulesetCache
import com.unciv.testing.BaseTestRunner
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 河流对称回归测试:对称模式下河道旋转重放后必须保持轨道对称;
 * 大地图上应能正常生成河流(多个 seed 抽查,排除无远水格的地形巧合)。
 */
@RunWith(BaseTestRunner::class)
class RiverSymmetryCheck {
    @Test
    fun riversGeneratedAndSymmetric() {
        RulesetCache.loadRulesets(noMods = true)
        val ruleset = RulesetCache.getVanillaRuleset()

        // 抽查:3 折非 wrap 的多个 seed 下能生成河流(排除地形巧合导致全图无远水格)
        var seedsWithRivers = 0
        for (seed in listOf(7L, 11L, 23L, 42L, 99L)) {
            val params = MapParameters().apply {
                type = MapType.perlin
                shape = MapShape.hexagonal
                mapSize = MapSize(15)
                this.seed = seed
                worldWrap = false
                symmetryMode = SymmetryMode.threeFold
            }
            val map = MapGenerator(ruleset).generateMap(params)
            if (countRiverEdges(map) > 0) seedsWithRivers++
        }
        assertTrue("at least some seeds should produce rivers on a size-15 map", seedsWithRivers >= 2)

        for (mode in SymmetryMode.allValues.filter { it != SymmetryMode.none })
            for (worldWrap in listOf(false, true)) {
                val params = MapParameters().apply {
                    type = MapType.perlin
                    shape = MapShape.hexagonal
                    mapSize = MapSize(15) // 足够大,确保有远水格可作河源
                    seed = 7
                    this.worldWrap = worldWrap
                    symmetryMode = mode
                }
                val map = MapGenerator(ruleset).generateMap(params)
                val riverEdgeCount = countRiverEdges(map)
                val sym = MapSymmetry(map, params.symmetryMode)
                val errors = sym.verify()
                assertTrue(
                    "mode=$mode wrap=$worldWrap riverEdges=$riverEdgeCount errors=${errors.size}\n" +
                        errors.joinToString("\n").take(2000),
                    errors.isEmpty()
                )
            }
    }

    private fun countRiverEdges(map: com.unciv.logic.map.TileMap): Int =
        map.values.sumOf { t: Tile ->
            listOf(t.hasBottomRiver, t.hasBottomLeftRiver, t.hasBottomRightRiver).count { it }
        }
}
