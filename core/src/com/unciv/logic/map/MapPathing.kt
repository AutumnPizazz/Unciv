package com.unciv.logic.map

import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.utils.Log
import yairm210.purity.annotations.Readonly

//TODO: Eventually, all path generation in the game should be moved into here.
object MapPathing {

    /**
     * We prefer the worker to prioritize paths connected by existing roads. If a tile has a road, but the civ has the ability
     * to upgrade it to a railroad, we consider it to be a railroad for pathing since it will be upgraded.
     * Otherwise, we set every tile to have equal value since building a road on any of them makes the original movement cost irrelevant.
     */
    @Suppress("UNUSED_PARAMETER") // While `from` is unused, this function should stay close to the signatures expected by the AStar `heuristic` parameter.
    @Readonly
    internal fun roadPreferredMovementCost(civ: Civilization, from: Tile, to: Tile): Float{
        // hasRoadConnection accounts for civs that treat jungle/forest as roads
        // Ignore road over river penalties.
        if ((to.hasRoadConnection(civ, false) || to.hasRailroadConnection(false)))
            return .5f

        return 1f
    }

    @Readonly
    fun isValidRoadPathTile(civ: Civilization, tile: Tile): Boolean {
        val roadImprovement = tile.ruleset.roadImprovement
        val railRoadImprovement = tile.ruleset.railroadImprovement
        
        if (tile.isWater) return false
        if (tile.isImpassible()) return false
        if (!civ.hasExplored(tile)) return false
        if (!tile.canCivPassThrough(civ)) return false
        
        return tile.hasRoadConnection(civ, false)
                || tile.hasRailroadConnection(false)
                || roadImprovement != null && tile.improvementFunctions.canBuildImprovement(roadImprovement, civ.state)
                || railRoadImprovement != null && tile.improvementFunctions.canBuildImprovement(railRoadImprovement,civ.state)
    }

    /**
     * Calculates the path for a road construction between two tiles.
     *
     * This function uses the A* search algorithm to find an optimal path for road construction between two specified tiles.
     *
     * @param civ The civlization that will construct the road.
     * @param startTile The starting tile of the path.
     * @param endTile The destination tile of the path.
     * @return A sequence of tiles representing the path from startTile to endTile, or null if no valid path is found.
     */
    @Readonly
    fun getRoadPath(civ: Civilization, startTile: Tile, endTile: Tile): List<Tile>? {
        return getConnection(civ,
            startTile,
            endTile,
            ::isValidRoadPathTile,
            ::roadPreferredMovementCost
        ) { _, _, _ -> 0f }
    }

    /**
     * Gets the connection to the end tile. This does not take into account tile movement costs.
     * Takes in a civilization instead of a specific unit.
     */
    @Readonly
    fun getConnection(civ: Civilization,
        startTile: Tile,
        endTile: Tile,
        predicate: (Civilization, Tile) -> Boolean,
        cost: (Civilization, Tile, Tile) -> Float = { _, _, _ -> 1f },
        heuristic: (Civilization, Tile, Tile) -> Float = { _, from, to -> from.aerialDistanceTo(to).toFloat() }
    ): List<Tile>? {
        val astar = AStar(
                startTile,
                predicate = { tile -> predicate(civ, tile) },
                cost = { from, to -> cost(civ, from, to) },
                heuristic = { from, to -> heuristic(civ, from, to) }
        )
        while (true) {
            if (astar.hasEnded()) {
                // We failed to find a path
                Log.debug("getConnection failed at AStar search size ${astar.size()}")
                return null
            }
            if (!astar.hasReachedTile(endTile)) {
                astar.nextStep()
                continue
            }
            // Found a path.
            return astar.getPathTo(endTile)
                    .toList()
                    .reversed()
        }
    }

}
