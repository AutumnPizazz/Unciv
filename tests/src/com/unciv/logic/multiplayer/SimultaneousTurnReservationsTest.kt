package com.unciv.logic.multiplayer

import com.unciv.logic.map.HexCoord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Reservation keys are a protocol between every client and the server: they are compared as plain
 * strings, so a client that spells a target differently would never notice that it was taken.
 */
class SimultaneousTurnReservationsTest {
    @Test
    fun unitKeysUseTheUnitId() {
        assertEquals("unit:42", SimultaneousTurnReservations.forUnit(42))
    }

    @Test
    fun tileKeysUseTheTileCoordinates() {
        assertEquals("tile:3,-2", SimultaneousTurnReservations.forTile(3, -2))
        assertEquals("tile:-1,0", SimultaneousTurnReservations.forTile(-1, 0))
    }

    @Test
    fun onlyTileKeysNameAPosition() {
        assertEquals(HexCoord(3, -2), SimultaneousTurnReservations.tilePositionOf("tile:3,-2"))
        assertNull(SimultaneousTurnReservations.tilePositionOf("unit:42"))
        assertNull("a malformed key must not break the map marks", SimultaneousTurnReservations.tilePositionOf("tile:3"))
        assertNull(SimultaneousTurnReservations.tilePositionOf("tile:a,b"))
    }

    @Test
    fun claimedTilesOfOtherPlayersBecomePositions() {
        val reservations = listOf(
            SimultaneousTurnReservation("tile:3,-2", "playerA"),
            SimultaneousTurnReservation(SimultaneousTurnReservations.forUnit(7), "playerA"),
            SimultaneousTurnReservation("tile:-1,4", "playerB"),
            SimultaneousTurnReservation("tile:8,8", "playerB")
        )

        assertEquals(
            setOf(HexCoord(-1, 4), HexCoord(8, 8)),
            SimultaneousTurnReservations.tilePositionsReservedByOthers(reservations, "playerA")
        )
    }

    @Test
    fun claimedTilesCarryTheirClaimantForTheHint() {
        val reservations = listOf(
            SimultaneousTurnReservation("tile:3,-2", "playerA"),
            SimultaneousTurnReservation("tile:-1,4", "playerB"),
            SimultaneousTurnReservation(SimultaneousTurnReservations.forUnit(7), "playerB"),
            SimultaneousTurnReservation("tile:5,5", "")
        )

        assertEquals(
            mapOf(HexCoord(-1, 4) to "playerB"),
            SimultaneousTurnReservations.tilesReservedByOthers(reservations, "playerA")
        )
    }
}
