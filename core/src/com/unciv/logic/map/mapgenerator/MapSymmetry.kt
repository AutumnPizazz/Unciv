package com.unciv.logic.map.mapgenerator

import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.SymmetryMode
import com.unciv.logic.map.TileMap
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.map.tile.TileNormalizer

/**
 * 旋转对称地图的轨道权威(单一权威,供各生成器共享)。
 *
 * 概念定义:
 * - 轨道(orbit): 一个格子及其旋转同伴的集合。2 折 = 180°、3 折 = 120°、6 折 = 60°。
 * - 规范格(canonical): 轨道内按存储坐标字典序最小的格子,是所有随机决策与状态
 *   复制的源。
 * - 步数(steps60): 成员相对规范格顺时针旋转的 60° 单位数(规范格为 0;3 折时成员
 *   步数为 2 或 4,2 折为 3,6 折为 1..5)。
 *
 * 环形地图(world wrap)说明:
 * 六边形 wrap 的等价关系是平移对 (x,y) ~ (x±radius, y∓radius)(见
 * [TileMap.getIfTileExistsOrNull] 的左右绕回查找)。本类把轨道提升到覆盖空间
 * (universal cover)计算旋转,再经 [tileAt] 把覆盖坐标解析回存储格,从而正确处理
 * 接缝附近的情况:那里的轨道可能不完整(成员数 < 折数),不再是旧实现(散落在
 * MapGenerator 内的 buildCanonicalMapping + getIfTileExistsOrNull ?: continue)
 * 的静默失效。
 *
 * 设计目标:生成管线的"规范扇区决策 + 轨道即时同步"模式下,每个生成器只轮询
 * [canonicalTiles] 做决策,再用 [stampInto] 同步到轨道;对称性由构造保证,由
 * [verify] 校验,而非事后补丁。
 */
class MapSymmetry(private val tileMap: TileMap, val mode: String) {

    val isActive: Boolean = mode != SymmetryMode.none

    /** 折数:2(180°)、3(120°)、6(60°);[SymmetryMode.none] 时为 0 */
    val fold: Int = when (mode) {
        SymmetryMode.twoFold -> 2
        SymmetryMode.threeFold -> 3
        SymmetryMode.sixFold -> 6
        else -> 0
    }

    private val worldWrap = tileMap.mapParameters.worldWrap
    private val radius = tileMap.mapParameters.mapSize.radius

    /** 单个轨道:规范格 + 按 steps60 索引的成员(轨道不完整时有缺项) */
    class Orbit(val canonical: Tile) {
        val members = LinkedHashMap<Int, Tile>()
        val size get() = members.size
        init { members[0] = canonical }
    }

    private val orbits = ArrayList<Orbit>()
    private val orbitByTile = HashMap<Tile, Orbit>()
    private lateinit var canonicalTilesList: List<Tile>

    /** 所有轨道的规范格,按首次遇到的顺序;生成器应只轮询该列表做决策 */
    val canonicalTiles: List<Tile> get() = canonicalTilesList

    init {
        if (!isActive) {
            canonicalTilesList = emptyList()
        } else {
            checkShape()
            buildOrbits()
        }
    }

    private fun checkShape() {
        check(tileMap.mapParameters.shape == MapShape.hexagonal) {
            "Rotational symmetry requires hexagonal maps, got ${tileMap.mapParameters.shape}"
        }
    }

    // region Orbit construction

    /**
     * 构建轨道分组。轨道键 = 轨道内全部成员(经旋转 + 绕回解析)的存储位置排序列表,
     * 天然消除覆盖空间 tie 歧义;同一轨道的所有格子必然得到相同键,不同轨道必然不同。
     */
    private fun buildOrbits() {
        val groups = LinkedHashMap<List<HexCoord>, MutableList<Tile>>()
        for (tile in tileMap.values) {
            val key = orbitPositionKey(tile)
            groups.getOrPut(key) { mutableListOf() }.add(tile)
        }
        for ((_, members) in groups) {
            val canonical = members.minWith(compareBy({ it.position.x }, { it.position.y }))
            val orbit = Orbit(canonical)
            if (members.size > 1) {
                val anchor = lift(canonical.position)
                val stepSize = 6 / fold
                for (member in members) {
                    if (member === canonical) continue
                    var found = -1
                    for (k in 1 until fold) {
                        if (tileAt(rotatePow(anchor, k * stepSize)) === member) {
                            found = k * stepSize  // steps60 以 60° 为单位,不是索引 k
                            break
                        }
                    }
                    if (found < 0)
                        error("Tile ${member.position} is not a rotation of canonical ${canonical.position} (mode=$mode)")
                    orbit.members[found] = member
                }
            }
            orbits.add(orbit)
            for ((_, member) in orbit.members)
                orbitByTile[member] = orbit
        }
        canonicalTilesList = orbits.map { it.canonical }
    }

    /** 轨道键:tile 的轨道成员(旋转 + 绕回解析到存储格)的存储位置,按字典序排序。
     *  成员可能少于 fold(接缝附近轨道不完整)。 */
    private fun orbitPositionKey(tile: Tile): List<HexCoord> {
        val stepSize = 6 / fold
        val from = lift(tile.position)
        return (0 until fold)
            .mapNotNull { k -> tileAt(rotatePow(from, k * stepSize))?.position }
            .sortedWith(compareBy({ it.x }, { it.y }))
    }

    // endregion

    // region Public API

    /** 某格所属的轨道;非对称模式下返回 null */
    fun orbitOf(tile: Tile): Orbit? = orbitByTile[tile]

    fun isCanonical(tile: Tile): Boolean = canonicalOf(tile) === tile

    /** 轨道规范格;非对称模式下为自身 */
    fun canonicalOf(tile: Tile): Tile = orbitByTile[tile]?.canonical ?: tile

    /** 成员相对规范格顺时针旋转的 steps60(60° 单位);规范格为 0 */
    fun stepsFromCanonical(tile: Tile): Int = orbitByTile[tile]?.let { orbit ->
        orbit.members.entries.firstOrNull { it.value === tile }?.key ?: 0
    } ?: 0

    /** 轨道成员按旋转序(steps60 升序)返回;非对称模式下为仅含自身的列表 */
    fun orbitTiles(tile: Tile): List<Tile> =
        orbitByTile[tile]?.members?.toSortedMap()?.values?.toList() ?: listOf(tile)

    /**
     * 把 [source] 的生成状态完整复制到 [target](不含河流边,见 [synchronizeRivers])。
     * 这是轨道同步的唯一写入入口:地形/特征/自然奇观/资源/改良/温度/湿度。
     */
    fun stampInto(target: Tile, source: Tile, steps60: Int) {
        // 先清除目标旧奇观,避免残留奇观在后续 normalize 里按 turnsInto 改回地形
        target.naturalWonder = null
        // 直接用字符串地形:生成阶段(如 applyTerrain)可能只赋值 baseTerrain 字符串而不刷新
        // 瞬态,getBaseTerrain()(瞬态对象)可能过期;统一以字符串为准并在此刷新瞬态
        target.baseTerrain = source.baseTerrain
        target.setTerrainFeatures(source.terrainFeatures)
        target.naturalWonder = source.naturalWonder
        target.tileResource = source.tileResource
        target.resourceAmount = source.resourceAmount
        target.setImprovementBasic(source.tileImprovement)
        target.temperature = source.temperature
        target.humidity = source.humidity

        target.setTerrainTransients()
        TileNormalizer.normalizeToRuleset(target, target.ruleset)
    }

    /**
     * 同步河流边到整个轨道:每条边以规范端为权威源,旋转后写到轨道成员间的对应边。
     * 河流标志由边的"较低端"唯一持有(见 [Tile.isConnectedByRiver]),写入是确定性的;
     * 从规范端读取时,若持有者在非规范格,需要先完成一轮写入(调用两次即可幂等收敛)。
     */
    fun synchronizeRivers() {
        if (!isActive) return
        for (canonical in canonicalTiles) {
            val orbit = orbitByTile[canonical] ?: continue
            for (neighbor in canonical.neighbors) {
                val clockPos = tileMap.getNeighborTileClockPosition(canonical, neighbor)
                if (clockPos == -1) continue
                val hasRiver = canonical.isConnectedByRiver(neighbor)
                for ((steps, member) in orbit.members) {
                    if (member === canonical) continue
                    val rotatedClockPos = rotateDirection(clockPos, steps)
                    val rotatedNeighbor = tileMap.getClockPositionNeighborTile(member, rotatedClockPos) ?: continue
                    member.setConnectedByRiver(rotatedNeighbor, hasRiver)
                }
            }
        }
    }

    /**
     * 校验当前地图的轨道一致性:对比每个轨道成员与规范格的生成状态
     * (地形/特征/自然奇观/资源/改良/温度/湿度/大陆 + 旋转后的河流边)。
     * 返回不一致描述列表,空列表即完全对称。供 DEBUG 与测试使用,而非运行时兜底。
     */
    fun verify(): List<String> {
        val errors = mutableListOf<String>()
        if (!isActive) return errors
        for (orbit in orbits) {
            val c = orbit.canonical
            for ((steps60, member) in orbit.members) {
                if (member === c) continue
                val where = "orbit@${c.position} vs ${member.position} (steps60=$steps60)"
                if (member.baseTerrain != c.baseTerrain)
                    errors += "$where: baseTerrain ${member.baseTerrain} != ${c.baseTerrain}"
                if (member.terrainFeatures != c.terrainFeatures)
                    errors += "$where: terrainFeatures ${
                        member.terrainFeatures} != ${c.terrainFeatures}"
                if (member.naturalWonder != c.naturalWonder)
                    errors += "$where: naturalWonder ${member.naturalWonder} != ${c.naturalWonder}"
                if (member.resource != c.resource || member.resourceAmount != c.resourceAmount)
                    errors += "$where: resource ${member.resource}x${member.resourceAmount} != ${c.resource}x${c.resourceAmount}"
                if (member.improvement != c.improvement)
                    errors += "$where: improvement ${member.improvement} != ${c.improvement}"
                if (member.temperature != c.temperature || member.humidity != c.humidity)
                    errors += "$where: temperature/humidity ${member.temperature}/${member.humidity} != ${c.temperature}/${c.humidity}"
                if (member.getContinent() != c.getContinent())
                    errors += "$where: continent ${member.getContinent()} != ${c.getContinent()}"

                for (neighbor in c.neighbors) {
                    val clockPos = tileMap.getNeighborTileClockPosition(c, neighbor)
                    if (clockPos == -1) continue
                    val rotatedClockPos = rotateDirection(clockPos, steps60)
                    val rotatedNeighbor = tileMap.getClockPositionNeighborTile(member, rotatedClockPos) ?: continue
                    if (member.isConnectedByRiver(rotatedNeighbor) != c.isConnectedByRiver(neighbor))
                        errors += "$where: river edge to ${neighbor.position} not symmetric"
                }
            }
        }
        return errors
    }

    // endregion

    // region Coordinate math

    /** 坐标的类内"近原点代表"([worldWrap] 时经 ±(radius, -radius) 平移);非 wrap 时为自身 */
    fun lift(coord: HexCoord): HexCoord {
        if (!worldWrap) return coord
        val candidates = arrayOf(
            coord,
            HexCoord.of(coord.x + radius, coord.y - radius),
            HexCoord.of(coord.x - radius, coord.y + radius)
        )
        return candidates.minWith(compareBy({ it.x * it.x + it.y * it.y }, { it.x }, { it.y }))
    }

    /** 覆盖坐标 → 存储格:先查精确坐标,再查 wrap 绕回(依赖 [TileMap.getIfTileExistsOrNull]) */
    fun tileAt(cover: HexCoord): Tile? =
        tileMap.getIfTileExistsOrNull(cover.x, cover.y)

    /** 轴向坐标绕原点顺时针旋转 60° */
    private fun rotate60(coord: HexCoord): HexCoord = HexCoord.of(coord.x - coord.y, coord.x)

    /** 应用 [rotate60] 共 [times] 次 */
    private fun rotatePow(coord: HexCoord, times: Int): HexCoord {
        var result = coord
        repeat(times) { result = rotate60(result) }
        return result
    }

    /** 时钟方向按步骤(60° 单位)旋转:2=TR, 4=BR, 6=B, 8=BL, 10=TL, 12=T */
    private fun rotateDirection(clockPos: Int, steps60: Int): Int =
        ((clockPos - 2 + steps60 * 2) % 12 + 12) % 12 + 2

    // endregion
}
