package com.unciv.logic.multiplayer

import com.unciv.logic.map.HexCoord

/** A target of a simultaneous-turn action that [owner] reserved for one turn. */
data class SimultaneousTurnReservation(
    val key: String = "",
    val owner: String = ""
)

/**
 * Targets of simultaneous-turn actions, keyed so that two players cannot act on the same object in
 * the same turn. Each player only sees their own actions until settlement, so without this the second
 * player learns about the collision when their operation is rejected - by which time they have already
 * given the unit an order that looks like it worked.
 *
 * The first player to reserve a target keeps it for that turn; the reservations are dropped by the
 * settlement pass, so a player who quits cannot block a target beyond the turn they quit in.
 */
object SimultaneousTurnReservations {
    private const val tilePrefix = "tile:"
    private const val unitPrefix = "unit:"

    fun forUnit(unitId: Int) = "$unitPrefix$unitId"
    fun forTile(x: Int, y: Int) = "$tilePrefix$x,$y"

    /**
     * The tile a reservation is about, or null for keys that do not name one. Only tile keys are
     * mapped: a reserved unit is already visible on the tile it stands on.
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
     * Tiles claimed for this turn by players other than [playerId], for the map marks that make a
     * claim visible. A claim the player cannot see reads as a bug when their order is refused.
     */
    fun tilePositionsReservedByOthers(
        reservations: List<SimultaneousTurnReservation>,
        playerId: String
    ): Set<HexCoord> = reservations
        .asSequence()
        .filter { it.owner != playerId }
        .mapNotNull { tilePositionOf(it.key) }
        .toSet()
}
