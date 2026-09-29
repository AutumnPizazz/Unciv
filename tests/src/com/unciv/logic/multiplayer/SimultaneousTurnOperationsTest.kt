package com.unciv.logic.multiplayer

import com.unciv.json.json
import com.unciv.models.UnitActionType
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun replayAppliesLaterOperationsAfterAnUnreplayableOne() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        val before = testGame.gameInfo.clone()
        before.setTransients()
        civ.variables["modEffect"] = 37
        val result = SimultaneousTurnOperations.captureGameStateChange(
            UnitActionType.TriggerUnique, before, testGame.gameInfo
        )!!
        val validOperation = SimultaneousTurnOperation(type = "game.state", payload = json().toJson(result))
        // References a unit that does not exist: settlement must skip it, not abort the pass.
        val invalidOperation = SimultaneousTurnOperation(
            type = "unit.move",
            payload = json().toJson(
                SimultaneousTurnMoveResult(
                    unitId = 999999, owner = civ.civName,
                    fromX = 0, fromY = 0, toX = 1, toY = 1, hp = 100, movement = 1f
                )
            )
        )

        val authoritative = before.clone()
        authoritative.setTransients()
        val failed = SimultaneousTurnReplay.replay(authoritative, listOf(invalidOperation, validOperation))

        assertEquals(listOf(invalidOperation), failed)
        assertEquals(37, authoritative.getCivilization(civ.civName).variables["modEffect"])
    }

    @Test
    fun snapshotWhitelistCoversAutomatedAndDeferredActions() {
        val snapshotted = listOf(
            UnitActionType.Automate, UnitActionType.Explore, UnitActionType.Upgrade,
            UnitActionType.Transform, UnitActionType.ConnectRoad, UnitActionType.DisbandUnit,
            UnitActionType.FoundCity, UnitActionType.TriggerUnique
        )
        for (type in snapshotted) {
            assertTrue("$type must be snapshotted", SimultaneousTurnOperations.requiresGameStateSnapshot(type))
        }
        // Flag-only actions are already recorded as lightweight unit.action operations
        val lightweight = listOf(
            UnitActionType.Guard, UnitActionType.Sleep, UnitActionType.Fortify,
            UnitActionType.SetUp, UnitActionType.Paradrop, UnitActionType.Skip,
            UnitActionType.StopAutomation, UnitActionType.StopExploration, UnitActionType.StopMovement
        )
        for (type in lightweight) {
            assertFalse("$type should stay a unit.action op", SimultaneousTurnOperations.requiresGameStateSnapshot(type))
        }
    }

    @Test
    fun unchangedStateProducesNoSnapshot() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        testGame.gameInfo.setTransients()
        val before = testGame.gameInfo.clone()
        before.setTransients()

        assertNull(
            SimultaneousTurnOperations.captureGameStateChange(
                UnitActionType.Automate, before, testGame.gameInfo
            )
        )
    }

    @Test
    fun automatedMoveSnapshotCarriesUnitMovement() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        testGame.gameInfo.setTransients()
        val before = testGame.gameInfo.clone()
        before.setTransients()

        // Simulate what UnitAutomation does to an automated unit without touching the map
        unit.currentMovement = 0f
        val result = SimultaneousTurnOperations.captureGameStateChange(
            UnitActionType.Automate, before, testGame.gameInfo
        )!!
        val operation = SimultaneousTurnOperation(type = "game.state", payload = json().toJson(result))

        val authoritative = before.clone()
        authoritative.setTransients()
        assertTrue(SimultaneousTurnReplay.apply(authoritative, operation))

        val replayedTile = authoritative.tileMap[0, 0]
        val tileUnit = replayedTile.militaryUnit ?: replayedTile.civilianUnit
        assertEquals("tile unit movement", 0f, tileUnit!!.currentMovement, 0.0001f)
        val civUnits = authoritative.getCivilization(civ.civName).units.getCivUnits()
            .filter { it.id == unit.id }.toList()
        assertEquals("civ unit list must not keep a stale duplicate", 1, civUnits.size)
        assertEquals("civ unit movement", 0f, civUnits.single().currentMovement, 0.0001f)
    }
}
