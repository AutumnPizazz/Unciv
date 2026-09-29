package com.unciv.logic.multiplayer

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
    fun forUnit(unitId: Int) = "unit:$unitId"
    fun forTile(x: Int, y: Int) = "tile:$x,$y"
}
