package com.unciv.logic.multiplayer

import com.unciv.json.json
import com.unciv.logic.map.HexCoord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Claim keys are a protocol between every client: they are compared as plain strings, and both the
 * hint this client shows and the conflict settlement resolves must agree on who touched a target.
 * They are derived from the operations the server only relayed, so the server never decides.
 */
class SimultaneousTurnClaimsTest {
    @Test
    fun unitKeysUseTheUnitId() {
        assertEquals("unit:42", SimultaneousTurnClaims.forUnit(42))
    }

    @Test
    fun tileKeysUseTheTileCoordinates() {
        assertEquals("tile:3,-2", SimultaneousTurnClaims.forTile(3, -2))
        assertEquals("tile:-1,0", SimultaneousTurnClaims.forTile(-1, 0))
    }

    @Test
    fun onlyTileKeysNameAPosition() {
        assertEquals(HexCoord(3, -2), SimultaneousTurnClaims.tilePositionOf("tile:3,-2"))
        assertNull(SimultaneousTurnClaims.tilePositionOf("unit:42"))
        assertNull("a malformed key must not break the map marks", SimultaneousTurnClaims.tilePositionOf("tile:3"))
        assertNull(SimultaneousTurnClaims.tilePositionOf("tile:a,b"))
    }

    @Test
    fun movesAndSwapsClaimTheUnitAndTheDestinationTile() {
        val move = json().toJson(SimultaneousTurnMoveResult(unitId = 7, toX = 3, toY = -2))
        assertEquals(
            listOf("unit:7", "tile:3,-2"),
            SimultaneousTurnClaims.claimsOf(operation("unit.move", move))
        )

        val swap = json().toJson(SimultaneousTurnSwapResult(unitId = 7, toX = -1, toY = 4))
        assertEquals(
            listOf("unit:7", "tile:-1,4"),
            SimultaneousTurnClaims.claimsOf(operation("unit.swap", swap))
        )
    }

    @Test
    fun attacksClaimTheAttackerAndTheTargetTile() {
        val attack = json().toJson(SimultaneousTurnAttackResult(attackerId = 7, targetX = 3, targetY = -2))
        assertEquals(
            listOf("unit:7", "tile:3,-2"),
            SimultaneousTurnClaims.claimsOf(operation("unit.attack", attack))
        )
    }

    @Test
    fun unitActionsClaimTheUnit() {
        val action = json().toJson(SimultaneousTurnUnitActionResult(unitId = 7, action = "Fortify"))
        assertEquals(listOf("unit:7"), SimultaneousTurnClaims.claimsOf(operation("unit.action", action)))
    }

    @Test
    fun stateOperationsClaimEveryTileTheySnapshot() {
        val state = json().toJson(
            SimultaneousTurnGameStateResult(
                action = "Trade",
                // arrayListOf, like the game itself builds these: libgdx Json writes a class name for
                // Kotlin's listOf and cannot read it back (no no-arg constructor).
                tiles = arrayListOf(
                    SimultaneousTurnComponentSnapshot(key = "3,-2"),
                    SimultaneousTurnComponentSnapshot(key = "not a tile")
                )
            )
        )
        assertEquals(listOf("tile:3,-2"), SimultaneousTurnClaims.claimsOf(operation("state.Trade", state)))
    }

    @Test
    fun operationsThatCannotBeReadClaimNothing() {
        assertTrue(SimultaneousTurnClaims.claimsOf(operation("unit.move", "{ not json")).isEmpty())
        assertTrue("an unknown operation type must not break the map", SimultaneousTurnClaims.claimsOf(operation("mystery", "{}")).isEmpty())
    }

    @Test
    fun claimsByOthersIgnoresThisPlayerAndOtherTurns() {
        val claims = SimultaneousTurnClaims.claimsByOthers(
            listOf(
                operation("unit.move", json().toJson(SimultaneousTurnMoveResult(unitId = 1, toX = 3, toY = -2)), playerId = "playerA"),
                operation("unit.move", json().toJson(SimultaneousTurnMoveResult(unitId = 2, toX = 5, toY = 5)), playerId = "playerB"),
                operation("unit.move", json().toJson(SimultaneousTurnMoveResult(unitId = 3, toX = 8, toY = 8)), playerId = "playerB", turn = 2),
                operation("unit.move", json().toJson(SimultaneousTurnMoveResult(unitId = 4, toX = 9, toY = 9)), playerId = "")
            ),
            turn = 1,
            playerId = "playerA"
        )

        assertEquals(
            mapOf("unit:2" to "playerB", "tile:5,5" to "playerB"),
            claims
        )
    }

    @Test
    fun claimsKeepTheFirstPlayerThatOrderedATarget() {
        val claims = SimultaneousTurnClaims.claimsByOthers(
            listOf(
                operation("unit.move", json().toJson(SimultaneousTurnMoveResult(unitId = 1, toX = 3, toY = -2)), playerId = "playerB"),
                operation("unit.move", json().toJson(SimultaneousTurnMoveResult(unitId = 2, toX = 3, toY = -2)), playerId = "playerC")
            ),
            turn = 1,
            playerId = "playerA"
        )

        assertEquals("playerB", claims["tile:3,-2"])
    }

    @Test
    fun tileClaimsBecomePositionsWithTheirClaimant() {
        val claims = SimultaneousTurnClaims.tileClaimsByOthers(
            listOf(
                operation("unit.move", json().toJson(SimultaneousTurnMoveResult(unitId = 1, toX = 3, toY = -2)), playerId = "playerB"),
                operation("unit.action", json().toJson(SimultaneousTurnUnitActionResult(unitId = 7)), playerId = "playerB")
            ),
            turn = 1,
            playerId = "playerA"
        )

        assertEquals(mapOf(HexCoord(3, -2) to "playerB"), claims)
    }


    private fun operation(
        type: String,
        payload: String,
        playerId: String = "playerA",
        turn: Int = 1
    ) = SimultaneousTurnOperation(turn = turn, playerId = playerId, sequence = 1, type = type, payload = payload)
}
