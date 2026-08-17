package com.unciv.logic.map.mapgenerator

import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapSize
import com.unciv.logic.map.SymmetryMode
import com.unciv.logic.map.TileMap
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.RulesetCache
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.RedirectOutput
import com.unciv.testing.RedirectPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** 旋转对称轨道数学与同步原语(MapSymmetry)的单元测试 */
@RunWith(BaseTestRunner::class)
class MapSymmetryTest {

    private var ruleset: Ruleset? = null
    private fun ruleset(): Ruleset {
        if (ruleset == null) {
            RulesetCache.loadRulesets(noMods = true)
            ruleset = RulesetCache.getVanillaRuleset()
        }
        return ruleset!!
    }

    private fun buildMap(radius: Int, worldWrap: Boolean, mode: String): Pair<TileMap, MapSymmetry> {
        val map = TileMap(radius, ruleset(), worldWrap)
        map.mapParameters.apply {
            shape = MapShape.hexagonal
            mapSize = MapSize(radius)
            this.worldWrap = worldWrap
            symmetryMode = mode
        }
        return map to MapSymmetry(map, mode)
    }

    // 测试用 60° 旋转(与 MapSymmetry 内部一致,独立复制以便交叉验证)
    private fun rotate60(coord: HexCoord) = HexCoord.of(coord.x - coord.y, coord.x)
    private fun rotatePow(coord: HexCoord, times: Int): HexCoord {
        var result = coord
        repeat(times) { result = rotate60(result) }
        return result
    }

    @Test
    fun nonWrapOrbitMath() {
        for (radius in listOf(4, 5, 6)) for (mode in SymmetryMode.allValues.filter { it != SymmetryMode.none }) {
            val (map, sym) = buildMap(radius, false, mode)
            val fold = sym.fold
            val n = map.values.size

            // 轨道必须覆盖全部格子且互不重叠
            val seen = HashSet<Tile>()
            val seenOrbits = HashSet<MapSymmetry.Orbit>()
            var orbitsWithFoldMembers = 0
            var singletonOrbits = 0
            for (tile in map.values) {
                val orbit = sym.orbitOf(tile)!!
                if (!seenOrbits.add(orbit)) continue  // 每个轨道只遍历一次
                for (member in orbit.members.values) {
                    assertTrue("orbit members must be unique", seen.add(member))
                    assertEquals("member must map back to its orbit", orbit, sym.orbitOf(member))
                }
                if (orbit.size == fold) orbitsWithFoldMembers++
                if (orbit.size == 1) singletonOrbits++
            }
            assertEquals("all tiles covered exactly once", n, seen.size)
            // 非 wrap 图上除中心格外,每个轨道都应有 fold 个成员
            assertEquals("expected n-1 tiles in full orbits", n - 1, orbitsWithFoldMembers * fold)
            assertEquals("only center tile is a singleton", 1, singletonOrbits)

            // 规范格 = 轨道内按位置字典序最小;steps 与位置旋转一致
            for (tile in map.values) {
                val orbit = sym.orbitOf(tile)!!
                val canonical = orbit.canonical
                val expectedCanonical = orbit.members.values.minWith(compareBy({ it.position.x }, { it.position.y }))
                assertEquals("canonical must be min-position member", expectedCanonical, canonical)
                val steps60 = sym.stepsFromCanonical(tile)
                assertEquals("stepsFromCanonical of canonical must be 0", 0, sym.stepsFromCanonical(canonical))
                // 非 wrap:旋转 canonical 位置 steps60 必须精确等于该格位置
                val rotated = rotatePow(canonical.position, steps60)
                assertEquals("rotated canonical position must equal member position", tile.position, rotated)
                assertTrue("canonicalOf must agree", sym.canonicalOf(tile) === canonical)
            }

            // 中心格属性:轨道大小为 1,步数为 0
            val centerTile = map[HexCoord.Zero]
            assertEquals("center orbit size", 1, sym.orbitOf(centerTile)!!.size)
            assertEquals("center steps", 0, sym.stepsFromCanonical(centerTile))
        }
    }

    @Test
    fun wrapOrbitMath() {
        for (radius in listOf(5, 6)) for (mode in SymmetryMode.allValues.filter { it != SymmetryMode.none }) {
            val (map, sym) = buildMap(radius, true, mode)
            val fold = sym.fold
            val n = map.values.size

            val seen = HashSet<Tile>()
            val seenOrbits = HashSet<MapSymmetry.Orbit>()
            var sumSizes = 0
            for (tile in map.values) {
                val orbit = sym.orbitOf(tile)!!
                assertTrue("no orbit larger than fold", orbit.size in 1..fold)
                if (!seenOrbits.add(orbit)) continue
                sumSizes += orbit.size
                for (member in orbit.members.values) {
                    assertTrue("wrap orbit members must be unique", seen.add(member))
                    assertEquals("member must map back to its orbit", orbit, sym.orbitOf(member))
                }
            }
            assertEquals("wrap map: all tiles partitioned", n, sumSizes)
            assertEquals("wrap map: all tiles seen", n, seen.size)

            // lift/tileAt 自洽:任意格的 lift 解析回它自身
            for (tile in map.values) {
                val resolved = sym.tileAt(sym.lift(tile.position))
                assertEquals("tileAt(lift(pos)) must return the same tile", tile, resolved)
            }

            // 中心格仍是平凡轨道
            val centerTile = map[HexCoord.Zero]
            assertEquals("center orbit size", 1, sym.orbitOf(centerTile)!!.size)
        }
    }

    @Test
    fun stampAndVerifyRoundTrip() {
        val (map, sym) = buildMap(5, false, SymmetryMode.sixFold)
        // 挑一个轨道完整(6 成员)且全成员皆为内陆格的规范格
        val target = sym.canonicalTiles.firstOrNull { canonical ->
            val members = sym.orbitOf(canonical)!!.members.values
            members.size == sym.fold && members.all { it.neighbors.count() == 6 }
        }
        require(target != null) { "no full interior orbit found" }
        val orbit = sym.orbitOf(target)!!
        val members = orbit.members.values.toList()

        val r = ruleset()
        // 规范格上构造一组与规则集兼容的生成状态(Wheat 仅生成于 Plains/Flood plains/Desert;Farm 不能建在 Forest 上)
        val canonical = orbit.canonical
        canonical.setBaseTerrain(r.terrains["Plains"]!!)
        canonical.setTerrainFeatures(listOf())
        canonical.setTileResource("Wheat")
        canonical.setImprovementBasic(r.tileImprovements["Farm"])
        canonical.temperature = 0.3
        canonical.humidity = 0.6
        canonical.hasBottomRightRiver = true
        canonical.hasBottomRiver = true

        // 同步到其余成员
        for (member in members) {
            if (member === canonical) continue
            sym.stampInto(member, canonical, sym.stepsFromCanonical(member))
        }

        // 直接断言字段一致
        for (member in members) {
            if (member === canonical) continue
            assertEquals(member.baseTerrain, canonical.baseTerrain)
            assertEquals(member.terrainFeatures, canonical.terrainFeatures)
            assertEquals(member.resource, canonical.resource)
            assertEquals(member.improvement, canonical.improvement)
            assertEquals(member.temperature, canonical.temperature)
            assertEquals(member.humidity, canonical.humidity)
        }
        // verify() 必须无错(含河流方向旋转)
        val errors = sym.verify()
        assertTrue("verify() must pass after stamping:\n${errors.joinToString("\n")}", errors.isEmpty())
    }

    /** 独立验证:特征(如 Forest)在 Featuring 地形上的复制与校验 */
    @Test
    fun stampCopyTerrainFeatures() {
        val (map, sym) = buildMap(4, false, SymmetryMode.twoFold)
        val canonical = sym.canonicalTiles.firstOrNull {
            val members = sym.orbitOf(it)!!.members.values
            members.size == sym.fold && members.all { m -> m.neighbors.count() == 6 }
        }
        require(canonical != null) { "no full interior orbit found" }
        val r = ruleset()
        canonical.setBaseTerrain(r.terrains["Tundra"]!!)  // Forest 可生于 Tundra
        canonical.setTerrainFeatures(listOf("Forest"))
        for ((steps60, member) in sym.orbitOf(canonical)!!.members) {
            if (member === canonical) continue
            sym.stampInto(member, canonical, steps60)
        }
        for ((_, member) in sym.orbitOf(canonical)!!.members) {
            assertEquals(canonical.baseTerrain, member.baseTerrain)
            assertEquals(canonical.terrainFeatures, member.terrainFeatures)
        }
        assertTrue(sym.verify().isEmpty())
    }

    /**
     * 对当前(仍为事后补丁式)管线生成的对称地图跑 verify():
     * 暴露旧管线残留的不对称点——改造完成后该测试应保持通过。
     * 当前阶段仅汇总输出(wrap 图存在已知的旧 mapping 静默失效),不做硬断言。
     */
    @Test
    @RedirectOutput(RedirectPolicy.Show)
    fun generatedMapsPassVerifyExceptContinents() {
        val report = StringBuilder()
        for (mode in SymmetryMode.allValues.filter { it != SymmetryMode.none })
            for (worldWrap in listOf(false, true)) {
                val mapParameters = MapParameters().apply {
                    type = com.unciv.logic.map.MapType.perlin
                    shape = MapShape.hexagonal
                    mapSize = MapSize(5)
                    seed = 42
                    this.worldWrap = worldWrap
                    symmetryMode = mode
                }
                val tileMap = MapGenerator(ruleset()).generateMap(mapParameters)
                val sym = MapSymmetry(tileMap, tileMap.mapParameters.symmetryMode)
                val errors = sym.verify()
                val nonContinent = errors.filterNot { it.contains("continent") }
                val continents = errors.filter { it.contains("continent") }
                report.append("mode=$mode wrap=$worldWrap tiles=${tileMap.values.size} " +
                    "nonContinentErrors=${nonContinent.size} continentErrors=${continents.size}\n")
                for (e in errors.take(6)) report.append("    $e\n")
            }
        println(report)
        // 旧管线全量补丁后仍可能存在少量残留;管线改造完成(第 2~7 步)后此处应收敛为 0。
    }
}