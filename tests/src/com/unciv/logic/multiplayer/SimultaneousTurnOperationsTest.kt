package com.unciv.logic.multiplayer

import com.unciv.json.json
import com.unciv.models.UnitActionType
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(BaseTestRunner::class)
class SimultaneousTurnOperationsTest {
    @Test
    fun mergeIsIdempotentAndHasStableOrder() {
        val first = SimultaneousTurnOperation(turn = 3, playerId = "b", sequence = 2, type = "move")
        val second = SimultaneousTurnOperation(turn = 3, playerId = "a", sequence = 1, type = "move")
        val duplicate = first.copy(payload = "different")

        val result = SimultaneousTurnOperations.merge(listOf(first), listOf(duplicate, second))

        assertEquals(listOf(second, first), result)
    }

    @Test
    fun encodeAndDecodeRoundTrip() {
        val operation = SimultaneousTurnOperation(4, "player", 7, "skip", "{}", 123)

        assertEquals(listOf(operation), SimultaneousTurnOperations.decode(
            SimultaneousTurnOperations.encode(listOf(operation))
        ))
    }

    @Test
    fun gameStateResultReplaysSerializedSideEffects() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        val before = testGame.gameInfo.clone()
        before.setTransients()

        civ.variables["modEffect"] = 37
        testGame.getTile(0, 0).improvement = "Farm"
        val result = SimultaneousTurnOperations.captureGameStateChange(
            UnitActionType.TriggerUnique, before, testGame.gameInfo
        )!!
        val authoritative = before.clone()
        authoritative.setTransients()
        val operation = SimultaneousTurnOperation(
            type = "game.state",
            payload = json().toJson(result)
        )
        val decoded = json().fromJson(SimultaneousTurnGameStateResult::class.java, operation.payload)
        assertEquals(1, decoded.civilizations.size)
        assertEquals(37, json().fromJson(
            com.unciv.logic.civilization.Civilization::class.java,
            decoded.civilizations.single().after
        ).variables["modEffect"])

        assertTrue(SimultaneousTurnReplay.apply(authoritative, operation))
        assertEquals(37, authoritative.civilizations.single().variables["modEffect"])
        assertEquals(37, authoritative.getCivilization(civ.civName).variables["modEffect"])
        assertEquals("Farm", authoritative.tileMap[0, 0].improvement)
    }

    @Test
    fun gameStateResultRejectsConflictingComponent() {
        val testGame = TestGame()
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        val before = testGame.gameInfo.clone()
        before.setTransients()
        civ.variables["modEffect"] = 37
        val result = SimultaneousTurnOperations.captureGameStateChange(
            UnitActionType.TriggerUnique, before, testGame.gameInfo
        )!!
        val authoritative = before.clone()
        authoritative.setTransients()
        authoritative.getCivilization(civ.civName).variables["modEffect"] = 1

        val applied = SimultaneousTurnReplay.apply(authoritative, SimultaneousTurnOperation(
            type = "game.state",
            payload = json().toJson(result)
        ))

        assertFalse(applied)
        assertEquals(1, authoritative.getCivilization(civ.civName).variables["modEffect"])
    }
}
