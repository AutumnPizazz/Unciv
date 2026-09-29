package com.unciv.logic.multiplayer

import org.junit.Assert.assertEquals
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
}
