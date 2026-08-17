package com.unciv.logic.map.mapgenerator

import com.unciv.Constants
import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.TileMap
import com.unciv.logic.map.mapgenerator.RiverGenerator.Companion.continueRiverOn
import com.unciv.logic.map.mapgenerator.RiverGenerator.RiverCoordinate
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.utils.debug
import kotlin.math.roundToInt

/**
 *  Handles River generation for [MapGenerator], [UniqueType.OneTimeChangeTerrain] and console.
 *
 *  Map generation follows the [vertices of map hexes][RiverCoordinate]: [spawnRivers].
 *  In-game new rivers work on edges: [continueRiverOn]
 */
class RiverGenerator(
    private val tileMap: TileMap,
    private val randomness: MapGenerationRandomness,
    ruleset: Ruleset,
    private val symmetry: MapSymmetry
) {
    private val riverCountMultiplier = ruleset.modOptions.constants.riverCountMultiplier
    private val minRiverLength = ruleset.modOptions.constants.minRiverLength
    private val maxRiverLength = ruleset.modOptions.constants.maxRiverLength

    fun spawnRivers(resultingTiles: MutableSet<Tile>? = null) {
        if (tileMap.values.none { it.isWater }) return
        val numberOfRivers = (tileMap.values.count { it.isLand } * riverCountMultiplier).roundToInt()

        var optionalTiles = tileMap.values.asSequence()
            .filter { it.getBaseTerrain().isMountain && it.isFarEnoughFromWater() }
            .toMutableList()
        if (optionalTiles.size < numberOfRivers)
            optionalTiles.addAll(tileMap.values.filter { it.isHill() && it.isFarEnoughFromWater() })
        if (optionalTiles.size < numberOfRivers)
            optionalTiles =
                tileMap.values.filter { it.isLand && it.isFarEnoughFromWater() }.toMutableList()

        val mapRadius = tileMap.mapParameters.mapSize.radius
        // 对称模式:只在规范格选起点(每轨道一个),河道生成后旋转重放到各扇区
        val starts = if (symmetry.isActive)
            optionalTiles.filter { symmetry.isCanonical(it) }
        else optionalTiles
        val riverStarts = randomness.chooseSpreadOutLocations(
            if (symmetry.isActive) (numberOfRivers / symmetry.fold).coerceAtLeast(1) else numberOfRivers,
            starts, mapRadius)
        for (tile in riverStarts) spawnRiver(tile, resultingTiles)
    }

    private fun Tile.isFarEnoughFromWater(): Boolean {
        for (distance in 1 until minRiverLength) {
            if (getTilesAtDistance(distance).any { it.isWater }) return false
        }
        return true
    }

    fun getClosestWaterTile(tile: Tile): Tile? {
        for (distance in 1..maxRiverLength) {
            val waterTiles = tile.getTilesAtDistance(distance).filter { it.isWater }
            if (waterTiles.any())
                return waterTiles.toList().random(randomness.RNG)
        }
        return null
    }

    private fun spawnRiver(initialPosition: Tile, resultingTiles: MutableSet<Tile>?) {
        val endPosition = getClosestWaterTile(initialPosition)
            ?: error("No water found for river destination")
        spawnRiver(initialPosition, endPosition, resultingTiles)
    }

    /** Spawns a river from [initialPosition] to [endPosition].
     *  If [resultingTiles] is supplied, it will contain all affected tiles, for map editor.
     *  对称模式下,规范起点生成的河道会旋转重放到轨道各扇区。 */
    fun spawnRiver(initialPosition: Tile, endPosition: Tile, resultingTiles: MutableSet<Tile>?) {
        // Recommendation: Draw a bunch of hexagons on paper before trying to understand this, it's super helpful!

        var riverCoordinate = RiverCoordinate(tileMap, initialPosition.position,
                RiverCoordinate.BottomRightOrLeft.entries.random(randomness.RNG))
        val edges = mutableListOf<Pair<HexCoord, HexCoord>>()

        repeat(maxRiverLength) {     // Arbitrary max on river length, otherwise this will go in circles - rarely
            if (riverCoordinate.getAdjacentTiles().any { it.isWater }) return
            val possibleCoordinates = riverCoordinate.getAdjacentPositions()
            if (possibleCoordinates.none()) return // end of the line
            val newCoordinate = possibleCoordinates
                .groupBy { newCoordinate ->
                    newCoordinate.getAdjacentTiles().map { it.aerialDistanceTo(endPosition) }
                        .minOrNull()!!
                }
                .minByOrNull { it.key }!!
                    .component2().random(randomness.RNG)

            // set one new river edge in place
            riverCoordinate.paintTo(newCoordinate, resultingTiles)?.let { edges.add(it) }
            // Move on
            riverCoordinate = newCoordinate
        }
        debug("River reached max length!")

        // 对称模式:把整条河道旋转到轨道各扇区
        if (symmetry.isActive && edges.isNotEmpty()) {
            val orbit = symmetry.orbitOf(initialPosition) ?: return
            for ((steps, member) in orbit.members) {
                if (member === initialPosition) continue
                for ((a, b) in edges) {
                    val rotatedA = rotateCoord(a, steps)
                    val rotatedB = rotateCoord(b, steps)
                    val tileA = tileMap.getIfTileExistsOrNull(rotatedA.x, rotatedA.y) ?: continue
                    val tileB = tileMap.getIfTileExistsOrNull(rotatedB.x, rotatedB.y) ?: continue
                    tileA.setConnectedByRiver(tileB, true)
                    if (resultingTiles != null) {
                        resultingTiles.add(tileA)
                        resultingTiles.add(tileB)
                    }
                }
            }
        }
    }

    /** 轴向坐标顺时针旋转 [steps60] × 60° */
    private fun rotateCoord(coord: HexCoord, steps60: Int): HexCoord {
        var result = coord
        repeat(steps60) { result = HexCoord.of(result.x - result.y, result.x) }
        return result
    }

    /** Describes a _Vertex_ on our hexagonal grid via a neighboring hex and clock direction, normalized
     * such that always the north-most hex and one of the two clock directions 5 / 7 o'clock are used. */
    class RiverCoordinate(
        private val tileMap: TileMap,
        private val position: HexCoord,
        private val bottomRightOrLeft: BottomRightOrLeft
    ) {
        enum class BottomRightOrLeft {
            /** 7 O'Clock of the tile */
            BottomLeft,

            /** 5 O'Clock of the tile */
            BottomRight
        }

        private val x = position.x
        private val y = position.y
        // Depending on the tile instance, some of the following will never be used. Tested with lazies: ~2% slower
        private val myTile = tileMap[position]
        private val myTopLeft = tileMap.getIfTileExistsOrNull(x + 1, y)
        private val myBottomLeft = tileMap.getIfTileExistsOrNull(x, y - 1)
        private val myTopRight = tileMap.getIfTileExistsOrNull(x, y + 1)
        private val myBottomRight = tileMap.getIfTileExistsOrNull(x - 1, y)
        private val myBottomCenter = tileMap.getIfTileExistsOrNull(x - 1, y - 1)

        /** Lists the three neighboring vertices which have their anchor hex on the map
         * (yes some positions on the map's outer border will be included, some not) */
        fun getAdjacentPositions(): Sequence<RiverCoordinate> = sequence {
            // What's nice is that adjacents are always the OPPOSITE in terms of right-left - rights are adjacent to only lefts, and vice-versa
            // This means that a lot of obviously-wrong assignments are simple to spot
            if (bottomRightOrLeft == BottomRightOrLeft.BottomLeft) {
                yield(RiverCoordinate(tileMap, position, BottomRightOrLeft.BottomRight)) // same tile, other side
                if (myTopLeft != null)
                    yield(RiverCoordinate(tileMap, myTopLeft.position, BottomRightOrLeft.BottomRight)) // tile to MY top-left, take its bottom right corner
                if (myBottomLeft != null)
                    yield(RiverCoordinate(tileMap, myBottomLeft.position, BottomRightOrLeft.BottomRight)) // Tile to MY bottom-left, take its bottom right
            } else {
                yield(RiverCoordinate(tileMap, position, BottomRightOrLeft.BottomLeft)) // same tile, other side
                if (myTopRight != null)
                    yield(RiverCoordinate(tileMap, myTopRight.position, BottomRightOrLeft.BottomLeft)) // tile to MY top-right, take its bottom left
                if (myBottomRight != null)
                    yield(RiverCoordinate(tileMap, myBottomRight.position, BottomRightOrLeft.BottomLeft))  // tile to MY bottom-right, take its bottom left
            }
        }

        /** Lists the three neighboring hexes to this vertex which are on the map */
        fun getAdjacentTiles(): Sequence<Tile> = sequence {
            yield(myTile)
            myBottomCenter?.let { yield(it) }  // tile directly below us,
            if (bottomRightOrLeft == BottomRightOrLeft.BottomLeft)
                myBottomLeft?.let { yield(it) }  // tile to our bottom-left
            else
                myBottomRight?.let { yield(it) }  // tile to our bottom-right
        }

        /** 画一条河岸边,返回该边两端的位置(供对称旋转重放);无法确定时返回 null */
        fun paintTo(newCoordinate: RiverCoordinate, resultingTiles: MutableSet<Tile>?): Pair<HexCoord, HexCoord>? {
            return if (newCoordinate.position == position) // same tile, switched right-to-left
                paintBottom(resultingTiles)
            else if (bottomRightOrLeft == BottomRightOrLeft.BottomRight) {
                if (newCoordinate.getAdjacentTiles().contains(myTile)) // moved from our 5 O'Clock to our 3 O'Clock
                    paintBottomRight(resultingTiles)
                else // moved from our 5 O'Clock down in the 5 O'Clock direction - this is the 8 O'Clock river of the tile to our 4 O'Clock!
                    newCoordinate.paintBottomLeft(resultingTiles)
            } else { // bottomRightOrLeft == BottomRightOrLeft.BottomLeft
                if (newCoordinate.getAdjacentTiles().contains(myTile)) // moved from our 7 O'Clock to our 9 O'Clock
                    paintBottomLeft(resultingTiles)
                else // moved from our 7 O'Clock down in the 7 O'Clock direction
                    newCoordinate.paintBottomRight(resultingTiles)
            }
        }

        private fun paintBottom(resultingTiles: MutableSet<Tile>?): Pair<HexCoord, HexCoord>? {
            myTile.hasBottomRiver = true
            if (resultingTiles == null) return null
            resultingTiles.add(myTile)
            myBottomCenter?.let { resultingTiles.add(it) }
            return myBottomCenter?.let { myTile.position to it.position }
        }
        private fun paintBottomLeft(resultingTiles: MutableSet<Tile>?): Pair<HexCoord, HexCoord>? {
            myTile.hasBottomLeftRiver = true
            if (resultingTiles == null) return null
            resultingTiles.add(myTile)
            myBottomLeft?.let { resultingTiles.add(it) }
            return myBottomLeft?.let { myTile.position to it.position }
        }
        private fun paintBottomRight(resultingTiles: MutableSet<Tile>?): Pair<HexCoord, HexCoord>? {
            myTile.hasBottomRightRiver = true
            if (resultingTiles == null) return null
            resultingTiles.add(myTile)
            myBottomRight?.let { resultingTiles.add(it) }
            return myBottomRight?.let { myTile.position to it.position }
        }

        /** Count edges with a river from this vertex */
        @Suppress("unused")  // Keep as how-to just in case
        fun numberOfConnectedRivers(): Int = sequence {
            yield(myTile.hasBottomRiver)
            if (bottomRightOrLeft == BottomRightOrLeft.BottomLeft) {
                yield(myTile.hasBottomLeftRiver)
                yield(myBottomLeft?.hasBottomRightRiver == true)
            } else {
                yield(myTile.hasBottomRightRiver)
                yield(myBottomRight?.hasBottomLeftRiver == true)
            }
        }.count { it }
    }

    enum class RiverDirections(private val clockPosition: Int) {
        North(12),
        NorthEast(2),
        SouthEast(4),
        South(6),
        SouthWest(8),
        NorthWest(10);
        fun getNeighborTile(selectedTile: Tile): Tile? =
            selectedTile.tileMap.getClockPositionNeighborTile(selectedTile, clockPosition)
        companion object {
            val names get() = entries.map { it.name }
        }
    }

    companion object {
        /** [UniqueType.OneTimeChangeTerrain] tries to place a "River" feature.
         *
         *  Operates on *edges* - while [spawnRiver] hops from [vertex][RiverCoordinate] to vertex!
         *  Placed here to make comparison easier, even though the implementation has nothing else in common.
         *  @return success - one edge of [tile] has a new river
         */
        fun continueRiverOn(tile: Tile): Boolean {
            if (!tile.isLand) return false
            val tileMap = tile.tileMap

            /** Helper to prioritize a tile edge for river placement - accesses [tile] as closure,
             *  and considers the edge common with [otherTile] in direction [clockPosition].
             *
             *  Will consider two additional tiles - those that are neighbor to both [tile] and [otherTile],
             *  and four other edges - those connecting to "our" edge.
             */
            class NeighborData(val otherTile: Tile) {
                val clockPosition = tileMap.getNeighborTileClockPosition(tile, otherTile)
                // Accesses `tile` as closure
                val isConnectedByRiver = tile.isConnectedByRiver(otherTile)
                val edgeLeadsToSea: Boolean
                val connectedRiverCount: Int
                val verticesFormYCount: Int

                init {
                    // Similar: private fun Tile.getLeftSharedNeighbor in TileLayerBorders
                    val leftSharedNeighbor = tileMap.getClockPositionNeighborTile(tile, (clockPosition - 2) % 12)
                    val rightSharedNeighbor = tileMap.getClockPositionNeighborTile(tile, (clockPosition + 2) % 12)
                    edgeLeadsToSea = leftSharedNeighbor?.isWater == true || rightSharedNeighbor?.isWater == true
                    connectedRiverCount = sequence {
                        yield(leftSharedNeighbor?.isConnectedByRiver(tile))
                        yield(leftSharedNeighbor?.isConnectedByRiver(otherTile))
                        yield(rightSharedNeighbor?.isConnectedByRiver(tile))
                        yield(rightSharedNeighbor?.isConnectedByRiver(otherTile))
                    }.count { it == true }
                    verticesFormYCount = sequence {
                        yield(leftSharedNeighbor?.run { isConnectedByRiver(tile) && isConnectedByRiver(otherTile) })
                        yield(rightSharedNeighbor?.run { isConnectedByRiver(tile) && isConnectedByRiver(otherTile) })
                    }.count { it == true }
                }

                fun getPriority(edgeToSeaPriority: Int) =
                    // choose a priority - only order matters, not magnitude
                    when {
                        isConnectedByRiver -> -9 // ensures this isn't chosen, otherwise "cannot place another river" would have bailed
                        edgeLeadsToSea -> edgeToSeaPriority + connectedRiverCount - 3 * verticesFormYCount
                        // Just 6 possible cases left:
                        //  * Connect two bends = -2
                        //  * Connect a bend to nothing = -1 // debatable!
                        //  * Connect nothing = 0
                        //  * Connect a bend with an open end = 1
                        //  * Connect to one open end = 2
                        //  * Connect two open ends = 3 (make that 4 to simplify)
                        verticesFormYCount == 2 -> -2
                        verticesFormYCount == 1 -> connectedRiverCount * 2 - 5
                        else -> connectedRiverCount * 2
                    }
            }

            // Collect data (includes tiles we already have a river edge with - need the stats)
            val viableNeighbors = tile.neighbors.filter { it.isLand }.map { NeighborData(it) }.toList()
            if (viableNeighbors.all { it.isConnectedByRiver }) return false // cannot place another river

            // Greatly encourage connecting to sea unless the tile already has a river to sea, in which case slightly discourage another one
            val edgeToSeaPriority = if (viableNeighbors.none { it.isConnectedByRiver && it.edgeLeadsToSea }) 9 else -1

            val rng = GameContext(tile = tile).stateBasedRandom("RiverGenerator.continueRiverOn")
            val choice = viableNeighbors
                .groupBy { it.getPriority(edgeToSeaPriority) } // Assign and group by priorities
                .maxBy { it.key }.value // Get the List with best priority - can't be empty
                .random(rng)

            return tile.setConnectedByRiver(choice.otherTile, newValue = true, convertTerrains = true)
        }
    }
}
