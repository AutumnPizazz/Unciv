package com.unciv.logic.multiplayer

import com.unciv.json.json
import com.unciv.logic.map.HexCoord

/**
 * The targets a simultaneous-turn order touches, keyed so that a client can tell whether another
 * player already gave an order that touches the same unit or tile in the same turn.
 *
 * Each player only sees their own orders until the turn settles, so without this the second player
 * learns about the collision when their operation is dropped at settlement - by which time the unit
 * already looked as if it had taken the order.
 *
 * The claims are read on the client from the operations that are already being relayed for the turn:
 * the server only stores and forwards them and never decides who got a target, and every client
 * settles the same list to the same result. A claim is only visible once its player uploaded it, so
 * this is a courtesy for the common case - settlement remains what actually resolves a conflict.
 */
object SimultaneousTurnClaims {
    private const val tilePrefix = "tile:"
    private const val unitPrefix = "unit:"

    fun forUnit(unitId: Int) = "$unitPrefix$unitId"
    fun forTile(x: Int, y: Int) = "$tilePrefix$x,$y"

    /**
     * The tile a key is about, or null for keys that do not name one. Only tile keys are mapped: a
     * claimed unit is already visible on the tile it stands on.
     */
    fun tilePositionOf(key: String): HexCoord? {
        if (!key.startsWith(tilePrefix)) return null
        val coordinates = key.removePrefix(tilePrefix).split(',')
        if (coordinates.size != 2) return null
        val x = coordinates[0].toIntOrNull() ?: return null
        val y = coordinates[1].toIntOrNull() ?: return null
        return HexCoord(x, y)
    }

    /**
     * The keys [operation] touches: the unit that acts and the tile it acts on. The payload is read
     * defensively - an operation this client cannot read claims nothing instead of breaking the map.
     */
    fun claimsOf(operation: SimultaneousTurnOperation): List<String> = try {
        when (operation.type) {
            "unit.move" -> json().fromJson(SimultaneousTurnMoveResult::class.java, operation.payload)
                .let { listOf(forUnit(it.unitId), forTile(it.toX, it.toY)) }
            "unit.swap" -> json().fromJson(SimultaneousTurnSwapResult::class.java, operation.payload)
                .let { listOf(forUnit(it.unitId), forTile(it.toX, it.toY)) }
            "unit.attack" -> json().fromJson(SimultaneousTurnAttackResult::class.java, operation.payload)
                .let { listOf(forUnit(it.attackerId), forTile(it.targetX, it.targetY)) }
            "unit.action" -> json().fromJson(SimultaneousTurnUnitActionResult::class.java, operation.payload)
                .let { listOf(forUnit(it.unitId)) }
            else -> stateClaimsOf(operation)
        }
    } catch (_: Exception) {
        emptyList()
    }

    /** A whole-state operation touches every tile it carries a snapshot of. */
    private fun stateClaimsOf(operation: SimultaneousTurnOperation): List<String> {
        if (!SimultaneousTurnOperations.isStateOperationType(operation.type)) return emptyList()
        val state = json().fromJson(SimultaneousTurnGameStateResult::class.java, operation.payload)
        return state.tiles.mapNotNull { snapshot ->
            val coordinates = snapshot.key.split(',')
            if (coordinates.size != 2) return@mapNotNull null
            val x = coordinates[0].toIntOrNull() ?: return@mapNotNull null
            val y = coordinates[1].toIntOrNull() ?: return@mapNotNull null
            forTile(x, y)
        }
    }

    /**
     * The targets other players already gave an order for in [turn], with the player who ordered it,
     * so the UI can both mark a claim and name who holds it. A claim the player cannot see reads as a
     * bug when their order is refused.
     */
    fun claimsByOthers(
        operations: List<SimultaneousTurnOperation>,
        turn: Int,
        playerId: String
    ): Map<String, String> {
        val claims = LinkedHashMap<String, String>()
        for (operation in operations) {
            if (operation.turn != turn || operation.playerId.isEmpty() || operation.playerId == playerId) continue
            for (key in claimsOf(operation)) claims.putIfAbsent(key, operation.playerId)
        }
        return claims
    }

    /** The tiles other players already gave an order for in [turn], with the player who ordered it. */
    fun tileClaimsByOthers(
        operations: List<SimultaneousTurnOperation>,
        turn: Int,
        playerId: String
    ): Map<HexCoord, String> = claimsByOthers(operations, turn, playerId)
        .mapNotNull { (key, owner) -> tilePositionOf(key)?.let { it to owner } }
        .toMap()
}
