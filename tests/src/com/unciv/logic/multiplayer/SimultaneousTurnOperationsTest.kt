package com.unciv.logic.multiplayer

import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.models.UnitActionType
import com.unciv.models.metadata.GameParameters
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun aMoveIsAppliedExactlyOnceEvenThoughItsSnapshotHoldsItToo() {
        // WorldMapHolder records a whole-component snapshot (cloned before the move) and a
        // unit.move result for the same click, in that order. Both describe the same movement, so
        // replaying them must not lose the move - applying the snapshot first used to leave the
        // unit.move op rejected, because the unit was no longer on its recorded origin.
        val testGame = TestGame()
        testGame.makeHexagonalMap(3)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        testGame.gameInfo.setTransients()
        val turnStart = testGame.gameInfo.clone()
        turnStart.setTransients()

        val gameInfoBefore = testGame.gameInfo.clone()
        val originX = testGame.getTile(0, 0).position.x
        val originY = testGame.getTile(0, 0).position.y
        val destination = testGame.getTile(1, 0)
        val destinationX = destination.position.x
        val destinationY = destination.position.y
        unit.movement.moveToTile(destination)

        val operations = listOf(
            SimultaneousTurnOperation(
                type = "game.state",
                payload = json().toJson(
                    SimultaneousTurnOperations.captureGameStateChange(
                        UnitActionType.TriggerUnique, gameInfoBefore, testGame.gameInfo
                    )!!
                )
            ),
            SimultaneousTurnOperation(
                type = "unit.move",
                payload = json().toJson(
                    SimultaneousTurnMoveResult(
                        unitId = unit.id, owner = civ.civName,
                        fromX = originX, fromY = originY,
                        toX = destinationX, toY = destinationY,
                        hp = unit.health, movement = unit.currentMovement
                    )
                )
            )
        )

        val authoritative = turnStart.clone()
        authoritative.setTransients()
        val failed = SimultaneousTurnReplay.replay(authoritative, operations)

        assertEquals(emptyList<SimultaneousTurnOperation>(), failed)
        val replayed = authoritative.getCivilization(civ.civName).units.getUnitById(unit.id)!!
        assertEquals(destinationX, replayed.currentTile.position.x)
        assertEquals(destinationY, replayed.currentTile.position.y)
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
    fun gameStateResultAcceptsRawSnapshotWithoutTransients() {
        // WorldScreen.beginSimultaneousGameStateSnapshot() hands captureGameStateChange() a raw
        // GameInfo.clone(), and TileMap.clone() leaves the transient tileMatrix empty. The diff must
        // not index that matrix, or every real snapshot would crash.
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        val before = testGame.gameInfo.clone()
        assertEquals(0, before.tileMap.tileMatrix.size)
        civ.variables["modEffect"] = 37
        testGame.getTile(0, 0).improvement = "Farm"
        val result = SimultaneousTurnOperations.captureGameStateChange(
            UnitActionType.TriggerUnique, before, testGame.gameInfo
        )!!
        assertEquals(1, result.civilizations.size)
        assertEquals(listOf("0,0"), result.tiles.map { it.key })
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
    fun splitGameStateResultKeepsUnrelatedChangesWhenOneComponentConflicts() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(3)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        testGame.gameInfo.setTransients()
        val before = testGame.gameInfo.clone()
        before.setTransients()
        val global = json().toJson(SimultaneousTurnOperations.currentGlobalState(before))

        civ.variables["modEffect"] = 37
        testGame.getTile(2, 2).improvement = "Farm"
        val result = SimultaneousTurnGameStateResult(
            action = "TriggerUnique",
            globalBefore = global,
            globalAfter = global,
            civilizations = listOf(SimultaneousTurnComponentSnapshot(
                civ.civName,
                json().toJson(before.getCivilization(civ.civName)),
                json().toJson(testGame.gameInfo.getCivilization(civ.civName))
            )),
            tiles = listOf(
                SimultaneousTurnComponentSnapshot(
                    "1,1",
                    json().toJson(before.tileMap[1, 1]),
                    json().toJson(testGame.gameInfo.tileMap[1, 1])
                ),
                SimultaneousTurnComponentSnapshot(
                    "2,2",
                    json().toJson(before.tileMap[2, 2]),
                    json().toJson(testGame.gameInfo.tileMap[2, 2])
                )
            )
        )

        val parts = SimultaneousTurnOperations.splitGameStateResult(result)
        assertEquals(3, parts.size)
        assertTrue(parts.all {
            it.civilizations.size + it.tiles.size + it.religions.size == 1
        })

        val authoritative = before.clone()
        authoritative.setTransients()
        // Another player changed the same tile in an incompatible way while ours was in flight.
        authoritative.tileMap[1, 1].improvement = "Road"

        val applied = parts.map { SimultaneousTurnReplay.apply(authoritative, SimultaneousTurnOperation(
            type = "game.state",
            payload = json().toJson(it)
        )) }

        assertFalse(applied[parts.indexOfFirst { it.tiles.any { tile -> tile.key == "1,1" } }])
        assertEquals(37, authoritative.getCivilization(civ.civName).variables["modEffect"])
        assertEquals("Farm", authoritative.tileMap[2, 2].improvement)
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
    fun unsupportedActionsAreRefusedAndEverythingElseStaysPlayable() {
        // Simultaneous turns v1 refuses a few actions instead of recording them halfway: they touch
        // objects shared between players (religions, city-state diplomacy, the great-person and
        // gifted-unit pools) or they need a confirmation the other clients never see. The UI refuses
        // them visibly, so this list must stay in sync with that refusal.
        val refused = listOf(
            UnitActionType.ConductTradeMission, UnitActionType.AddInCapital, UnitActionType.GiftUnit,
            UnitActionType.FoundReligion, UnitActionType.EnhanceReligion, UnitActionType.SpreadReligion,
            UnitActionType.RemoveHeresy, UnitActionType.ConnectRoad, UnitActionType.Pillage,
            UnitActionType.Paradrop
        )
        for (type in refused) {
            assertTrue("$type must be refused in simultaneous turns",
                SimultaneousTurnOperations.isUnsupportedUnitAction(type))
        }

        // Most of them would otherwise still be "recorded" through the generic snapshot whitelist,
        // which is exactly the silent-corruption shape the refusal removes.
        for (type in listOf(
            UnitActionType.Pillage, UnitActionType.ConductTradeMission, UnitActionType.ConnectRoad,
            UnitActionType.FoundReligion, UnitActionType.SpreadReligion, UnitActionType.EnhanceReligion,
            UnitActionType.RemoveHeresy, UnitActionType.AddInCapital, UnitActionType.GiftUnit
        )) {
            assertTrue("$type should otherwise fall into the generic snapshot",
                SimultaneousTurnOperations.requiresGameStateSnapshot(type))
        }

        // Everything else must stay playable - refusing more than necessary would cost the beta whole
        // features for no gain.
        val playable = listOf(
            UnitActionType.FoundCity, UnitActionType.ConstructImprovement, UnitActionType.Repair,
            UnitActionType.CreateImprovement, UnitActionType.Promote, UnitActionType.Upgrade,
            UnitActionType.Transform, UnitActionType.DisbandUnit, UnitActionType.Automate,
            UnitActionType.Explore, UnitActionType.HurryResearch, UnitActionType.HurryPolicy,
            UnitActionType.HurryWonder, UnitActionType.HurryBuilding, UnitActionType.Fortify,
            UnitActionType.Sleep, UnitActionType.Skip, UnitActionType.Guard, UnitActionType.SetUp,
            UnitActionType.SwapUnits, UnitActionType.StopMovement, UnitActionType.StopAutomation
        )
        for (type in playable) {
            assertFalse("$type must stay playable in simultaneous turns",
                SimultaneousTurnOperations.isUnsupportedUnitAction(type))
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

    @Test
    fun settlementConvergesForIndependentPlayers() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(3)
        val civA = testGame.addCiv(testGame.ruleset.nations.values.elementAt(0), isPlayer = true)
        val civB = testGame.addCiv(testGame.ruleset.nations.values.elementAt(1), isPlayer = true)
        val unitA = testGame.addUnit("Warrior", civA, testGame.getTile(0, 0))
        val unitB = testGame.addUnit("Warrior", civB, testGame.getTile(0, 1))
        testGame.gameInfo.setTransients()

        // Every client begins the simultaneous turn from this exact serialized state
        val turnStart = testGame.gameInfo.clone()
        turnStart.setTransients()

        // Player A performs a whole-state change (what automated movement records)
        val beforeA = testGame.gameInfo.clone()
        beforeA.setTransients()
        unitA.currentMovement = 0f
        civA.variables["aStep"] = 1
        val opA = SimultaneousTurnOperation(
            turn = 1, playerId = civA.civName, sequence = 1, type = "game.state",
            payload = json().toJson(
                SimultaneousTurnOperations.captureGameStateChange(UnitActionType.Automate, beforeA, testGame.gameInfo)!!
            )
        )

        // Player B moves one of its own units
        val fromB = unitB.currentTile.position
        unitB.movement.moveToTile(testGame.getTile(1, 1))
        assertEquals("move must reach the destination", 1, unitB.currentTile.position.x)
        assertEquals("move must reach the destination", 1, unitB.currentTile.position.y)
        val opB = SimultaneousTurnOperation(
            turn = 1, playerId = civB.civName, sequence = 1, type = "unit.move",
            payload = json().toJson(
                SimultaneousTurnMoveResult(
                    unitId = unitB.id, owner = civB.civName,
                    fromX = fromB.x, fromY = fromB.y, toX = 1, toY = 1,
                    hp = unitB.health, movement = unitB.currentMovement
                )
            )
        )

        val settlement = turnStart.clone()
        settlement.setTransients()
        val failed = SimultaneousTurnReplay.replay(settlement, listOf(opA, opB))
        assertTrue("operations must replay: $failed", failed.isEmpty())

        // Both players' effects coexist in the settled state
        val settledA = settlement.getCivilization(civA.civName)
        assertEquals(1, settledA.variables["aStep"])
        assertEquals("settled unit movement", 0f, settledA.units.getUnitById(unitA.id)!!.currentMovement, 0.0001f)
        assertEquals("no duplicate for player A", 1, settledA.units.getCivUnits().count { it.id == unitA.id })

        val settledB = settlement.getCivilization(civB.civName).units.getUnitById(unitB.id)!!
        assertEquals(1, settledB.currentTile.position.x)
        assertEquals(1, settledB.currentTile.position.y)
        assertEquals("no duplicate for player B", 1,
            settlement.getCivilization(civB.civName).units.getCivUnits().count { it.id == unitB.id })

        // Whole-state operations are idempotent: replaying one again must not duplicate units
        assertTrue(SimultaneousTurnReplay.apply(settlement, opA))
        assertEquals("re-applying a state snapshot must stay idempotent", 1,
            settlement.getCivilization(civA.civName).units.getCivUnits().count { it.id == unitA.id })
    }

    @Test
    fun diffUnrecordedStateIsEmptyAfterAFaithfulReplay() {
        // Regression: diffUnrecordedState() normalized its two sides differently, so setTransients()
        // re-derived Civilization.lastSeenImprovement on one side only and every simultaneous turn
        // reported a phantom unrecorded change (recording a redundant catch-all operation each turn).
        val testGame = TestGame()
        testGame.makeHexagonalMap(3)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.elementAt(1), isPlayer = true)
        testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        testGame.gameInfo.setTransients()

        val turnStart = testGame.gameInfo.clone()
        turnStart.setTransients()

        // The settled state: the player changed one variable, captured as a game.state operation.
        val current = turnStart.clone()
        current.setTransients()
        val currentCiv = current.getCivilization(civ.civName)
        val before = current.clone()
        before.setTransients()
        currentCiv.variables["step"] = 1
        val operation = SimultaneousTurnOperation(
            turn = 1, playerId = currentCiv.civName, sequence = 1, type = "game.state",
            payload = json().toJson(
                SimultaneousTurnOperations
                    .captureGameStateChange(UnitActionType.Automate, before, current)!!
            )
        )

        assertNull(
            "replaying the recorded operation must reproduce the settled state exactly",
            SimultaneousTurnOperations.diffUnrecordedState(turnStart, current, listOf(operation))
        )
    }

    @Test
    fun everyOperationPayloadRoundTripsThroughTheSerializer() {
        // libgdx Json instantiates via a no-arg constructor, so every parameter of these classes
        // needs a default. Without that, deserialization throws and settlement silently drops the
        // operation - this must never regress.
        val move = json().fromJson(
            SimultaneousTurnMoveResult::class.java,
            json().toJson(SimultaneousTurnMoveResult(1, "civ", 2, 3, 4, 5, 100, 1.5f))
        )
        assertEquals(1, move.unitId)
        assertEquals("civ", move.owner)
        assertEquals(4, move.toX)
        assertEquals(1.5f, move.movement, 0.0001f)

        assertEquals(2, json().fromJson(
            SimultaneousTurnAttackResult::class.java,
            json().toJson(SimultaneousTurnAttackResult(1, 2, "civ", 100, 0, 4, 5))
        ).targetId)

        val action = json().fromJson(
            SimultaneousTurnUnitActionResult::class.java,
            json().toJson(SimultaneousTurnUnitActionResult(1, "civ", "Fortify", true, 100, 1f, true, true))
        )
        assertEquals("Fortify", action.action)
        assertTrue(action.escorting)
        assertTrue(action.automated)

        assertEquals(1.5f, json().fromJson(
            SimultaneousTurnSwapResult::class.java,
            json().toJson(SimultaneousTurnSwapResult(1, "civ", 2, 3, 4, 5, 100, 1.5f))
        ).movement, 0.0001f)

        assertNotNull(json().fromJson(
            SimultaneousTurnGameStateResult::class.java,
            json().toJson(SimultaneousTurnGameStateResult())
        ))
    }

    @Test
    fun reconciliationCapturesUnrecordedStateChanges() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        testGame.gameInfo.setTransients()
        val turnStart = testGame.gameInfo.clone()
        turnStart.setTransients()

        // A change made through a code path that forgot to record an operation
        civ.variables["unrecorded"] = 5

        val result = SimultaneousTurnOperations.diffUnrecordedState(
            turnStart, testGame.gameInfo, emptyList()
        )
        assertNotNull("reconciliation must catch unrecorded changes", result)

        val settlement = turnStart.clone()
        settlement.setTransients()
        val operation = SimultaneousTurnOperation(
            type = "game.state", payload = json().toJson(result)
        )
        assertTrue(SimultaneousTurnReplay.apply(settlement, operation))
        assertEquals(5, settlement.getCivilization(civ.civName).variables["unrecorded"])
    }

    @Test
    fun unrecordedStateChangesAreDescribedForTheReport() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        testGame.gameInfo.setTransients()
        val turnStart = testGame.gameInfo.clone()
        turnStart.setTransients()

        // A change made through a code path that forgot to record an operation
        civ.variables["unrecorded"] = 5

        val result = SimultaneousTurnOperations.diffUnrecordedState(
            turnStart, testGame.gameInfo, emptyList()
        )
        assertNotNull("the unrecorded change must be detected so it can be reported", result)
        val description = SimultaneousTurnOperations.describeGameStateResult(result!!)
        // Naming the component and the field is what makes the report actionable: it has to lead to the
        // code path that changed the state without recording an operation.
        assertTrue("the report must name the changed civilization: $description", description.contains(civ.civName))
        assertTrue("the report must name the changed field: $description", description.contains("variables"))
    }

    @Test
    fun reconciliationProducesNoOperationWhenEveryChangeWasRecorded() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        testGame.gameInfo.setTransients()
        val turnStart = testGame.gameInfo.clone()
        turnStart.setTransients()

        val before = testGame.gameInfo.clone()
        before.setTransients()
        civ.variables["recorded"] = 9
        val recorded = SimultaneousTurnOperations.captureGameStateChange(
            UnitActionType.TriggerUnique, before, testGame.gameInfo
        )!!
        val operation = SimultaneousTurnOperation(
            turn = testGame.gameInfo.turns, playerId = civ.civName, sequence = 1,
            type = "game.state", payload = json().toJson(recorded)
        )

        assertNull(
            "an already-recorded change must not be duplicated",
            SimultaneousTurnOperations.diffUnrecordedState(
                turnStart, testGame.gameInfo, listOf(operation)
            )
        )
    }

    @Test
    fun snapshotMustBeRecordedBeforeGranularOperationsSoItIsNotRejected() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(3)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        testGame.gameInfo.setTransients()
        val turnStart = testGame.gameInfo.clone()
        turnStart.setTransients()

        // An action that sets a unit flag and then moves. The snapshot describes before -> after-move.
        val before = testGame.gameInfo.clone()
        before.setTransients()
        unit.action = UnitActionType.Explore.value
        val granular = SimultaneousTurnOperation(
            turn = 1, playerId = civ.civName, sequence = 0, type = "unit.action",
            payload = json().toJson(
                SimultaneousTurnUnitActionResult(
                    unitId = unit.id, owner = civ.civName, action = unit.action,
                    due = unit.due, health = unit.health, movement = unit.currentMovement,
                    escorting = unit.isEscorting()
                )
            )
        )
        unit.movement.moveToTile(testGame.getTile(0, 1))
        val snapshot = SimultaneousTurnOperation(
            turn = 1, playerId = civ.civName, sequence = 0, type = "game.state",
            payload = json().toJson(
                SimultaneousTurnOperations.captureGameStateChange(
                    UnitActionType.Explore, before, testGame.gameInfo
                )!!
            )
        )

        // Settlement replays in merged (turn, playerId, sequence) order. When the granular op was
        // recorded first, the unit flag no longer matches either side of the snapshot, so the whole
        // snapshot is rejected and the move it carried is lost. This is the bug that made every
        // recording site order its snapshot call before the granular one.
        val buggyOps = SimultaneousTurnOperations.merge(
            emptyList(),
            listOf(granular.copy(sequence = 1), snapshot.copy(sequence = 2))
        )
        assertEquals(listOf("unit.action", "game.state"), buggyOps.map { it.type })
        val buggy = turnStart.clone()
        buggy.setTransients()
        assertEquals(
            "a snapshot recorded after a granular op must be rejected",
            listOf(snapshot.copy(sequence = 2)),
            SimultaneousTurnReplay.replay(buggy, buggyOps)
        )
        assertEquals(0, buggy.getCivilization(civ.civName).units.getUnitById(unit.id)!!.currentTile.position.y)

        // Snapshot first: it applies cleanly, and the granular operation is a harmless no-op on top.
        val fixedOps = SimultaneousTurnOperations.merge(
            emptyList(),
            listOf(snapshot.copy(sequence = 1), granular.copy(sequence = 2))
        )
        assertEquals(listOf("game.state", "unit.action"), fixedOps.map { it.type })
        val fixed = turnStart.clone()
        fixed.setTransients()
        val failed = SimultaneousTurnReplay.replay(fixed, fixedOps)
        assertTrue("snapshot first must replay cleanly: $failed", failed.isEmpty())
        assertEquals(1, fixed.getCivilization(civ.civName).units.getUnitById(unit.id)!!.currentTile.position.y)
    }

    @Test
    fun settlementCombinesRecordedReconciledAndOtherPlayerOperations() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(3)
        val civA = testGame.addCiv(testGame.ruleset.nations.values.elementAt(0), isPlayer = true)
        val civB = testGame.addCiv(testGame.ruleset.nations.values.elementAt(1), isPlayer = true)
        val unitA = testGame.addUnit("Warrior", civA, testGame.getTile(0, 0))
        val unitB = testGame.addUnit("Warrior", civB, testGame.getTile(0, 1))
        testGame.gameInfo.setTransients()
        val turnStart = testGame.gameInfo.clone()
        turnStart.setTransients()
        val turn = testGame.gameInfo.turns

        // Player A performs a recorded whole-state change (automated movement)...
        val beforeA = testGame.gameInfo.clone()
        beforeA.setTransients()
        unitA.currentMovement = 0f
        val recordedMove = SimultaneousTurnOperation(
            turn = turn, playerId = civA.civName, sequence = 1, type = "game.state",
            payload = json().toJson(
                SimultaneousTurnOperations.captureGameStateChange(
                    UnitActionType.Automate, beforeA, testGame.gameInfo
                )!!
            )
        )

        // ...then changes something else through a path that forgot to record an operation.
        civA.variables["sneaky"] = 7
        val reconciled = SimultaneousTurnOperations.diffUnrecordedState(
            turnStart, testGame.gameInfo, listOf(recordedMove)
        )
        assertNotNull("reconciliation must catch the unrecorded change", reconciled)
        val catchAll = SimultaneousTurnOperation(
            turn = turn, playerId = civA.civName, sequence = 2, type = "game.state",
            payload = json().toJson(reconciled!!)
        )

        // Player B moves one of its own units normally.
        val fromB = unitB.currentTile.position
        unitB.movement.moveToTile(testGame.getTile(1, 1))
        val opB = SimultaneousTurnOperation(
            turn = turn, playerId = civB.civName, sequence = 1, type = "unit.move",
            payload = json().toJson(
                SimultaneousTurnMoveResult(
                    unitB.id, civB.civName, fromB.x, fromB.y, 1, 1, unitB.health, unitB.currentMovement
                )
            )
        )

        val operations = SimultaneousTurnOperations.merge(emptyList(), listOf(recordedMove, catchAll, opB))
        val settlement = turnStart.clone()
        settlement.setTransients()
        val failed = SimultaneousTurnReplay.replay(settlement, operations)
        assertTrue("all operations must replay: $failed", failed.isEmpty())

        val settledA = settlement.getCivilization(civA.civName)
        assertEquals("recorded change", 0f, settledA.units.getUnitById(unitA.id)!!.currentMovement, 0.0001f)
        assertEquals("reconciled change must survive settlement", 7, settledA.variables["sneaky"])
        assertEquals("no duplicate for player A", 1, settledA.units.getCivUnits().count { it.id == unitA.id })

        val settledB = settlement.getCivilization(civB.civName).units.getUnitById(unitB.id)!!
        assertEquals(1, settledB.currentTile.position.x)
        assertEquals(1, settledB.currentTile.position.y)
    }

    @Test
    fun replayClearsNullableTileFieldsThatBecameNull() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        val tile = testGame.getTile(0, 0)
        val improvementName = testGame.ruleset.tileImprovements.keys.first()
        tile.improvement = improvementName
        testGame.gameInfo.setTransients()
        val turnStart = testGame.gameInfo.clone()
        turnStart.setTransients()

        // A snapshot captured while the improvement was destroyed...
        val before = testGame.gameInfo.clone()
        before.setTransients()
        tile.improvement = null
        val result = SimultaneousTurnOperations.captureGameStateChange(
            UnitActionType.TriggerUnique, before, testGame.gameInfo
        )!!
        assertTrue(
            "the destroyed improvement must show up as a tile diff",
            result.tiles.any { it.key == "0,0" }
        )

        // ...is applied to a peer whose tile still carries the improvement. libgdx omits null fields,
        // so readFields would leave the stale "Farm" behind unless the field is cleared first.
        val settlement = turnStart.clone()
        settlement.setTransients()
        assertEquals(improvementName, settlement.tileMap[0, 0].improvement)
        val operation = SimultaneousTurnOperation(
            type = "game.state", payload = json().toJson(result)
        )
        assertTrue(SimultaneousTurnReplay.apply(settlement, operation))
        assertNull(
            "a destroyed improvement must not survive readFields into the live tile",
            settlement.tileMap[0, 0].improvement
        )
    }

    @Test
    fun stoppingAutomationIsRestoredByReplay() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        unit.automated = true
        testGame.gameInfo.setTransients()

        val operation = SimultaneousTurnOperation(
            type = "unit.action",
            payload = json().toJson(
                SimultaneousTurnUnitActionResult(
                    unitId = unit.id, owner = civ.civName,
                    action = unit.action, due = unit.due, health = unit.health,
                    movement = unit.currentMovement, escorting = unit.isEscorting(),
                    automated = false
                )
            )
        )
        assertTrue(SimultaneousTurnReplay.apply(testGame.gameInfo, operation))
        assertFalse(
            "a stop-automation action must be replayed, otherwise the unit keeps automating",
            unit.automated
        )
    }

    @Test
    fun aTurnTimeoutIsDisabledByZero() {
        assertFalse(
            SimultaneousTurnOperations.hasSimultaneousTurnTimedOut(
                lastActivityMillis = 0L,
                waitStartedAtMillis = 0L,
                nowMillis = Long.MAX_VALUE,
                timeoutMinutes = 0
            )
        )
    }

    @Test
    fun aTurnTimesOutOnlyAfterTheConfiguredWait() {
        val waitStarted = 1_000_000L
        assertFalse(
            "just under the timeout the client must keep waiting",
            SimultaneousTurnOperations.hasSimultaneousTurnTimedOut(
                lastActivityMillis = 0L,
                waitStartedAtMillis = waitStarted,
                nowMillis = waitStarted + 5 * 60_000L - 1,
                timeoutMinutes = 5
            )
        )
        assertTrue(
            SimultaneousTurnOperations.hasSimultaneousTurnTimedOut(
                lastActivityMillis = 0L,
                waitStartedAtMillis = waitStarted,
                nowMillis = waitStarted + 5 * 60_000L,
                timeoutMinutes = 5
            )
        )
    }

    @Test
    fun recentActivityPostponesTheTurnTimeout() {
        val waitStarted = 1_000_000L
        val now = waitStarted + 10 * 60_000L
        assertTrue(
            SimultaneousTurnOperations.hasSimultaneousTurnTimedOut(
                lastActivityMillis = 0L,
                waitStartedAtMillis = waitStarted,
                nowMillis = now,
                timeoutMinutes = 5
            )
        )
        assertFalse(
            "a player who acted a minute ago must not be skipped",
            SimultaneousTurnOperations.hasSimultaneousTurnTimedOut(
                lastActivityMillis = now - 60_000L,
                waitStartedAtMillis = waitStarted,
                nowMillis = now,
                timeoutMinutes = 5
            )
        )
    }

    @Test
    fun theTurnTimeoutIsCopiedWithTheGameParameters() {
        val parameters = GameParameters()
        assertEquals(5, parameters.simultaneousTurnTimeoutMinutes)
        parameters.simultaneousTurnTimeoutMinutes = 17
        assertEquals(17, parameters.clone().simultaneousTurnTimeoutMinutes)
    }

    @Test
    fun failedOperationsAreReportedWithTheCivThatLostThem() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(3)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        val otherCiv = testGame.addCiv(testGame.ruleset.nations.values.last(), isPlayer = true)
        civ.playerId = "player-a"
        otherCiv.playerId = "player-b"

        assertNull(
            "a settlement without failures must not nag the player",
            SimultaneousTurnOperations.describeFailedSimultaneousTurnOperations(
                testGame.gameInfo, emptyList()
            )
        )

        val report = SimultaneousTurnOperations.describeFailedSimultaneousTurnOperations(
            testGame.gameInfo,
            listOf(
                SimultaneousTurnOperation(turn = 1, playerId = "player-a", sequence = 4, type = "unit.attack"),
                SimultaneousTurnOperation(turn = 1, playerId = "player-b", sequence = 5, type = "unit.move")
            )
        )

        assertNotNull(report)
        assertTrue(report!!.contains(civ.civName))
        assertTrue("the other player's civ must be named too", report.contains(otherCiv.civName))
        assertTrue(report.contains("unit.attack"))
        assertTrue(report.contains("unit.move"))
    }

    @Test
    fun aComponentSnapshotCarriesOnlyTheFieldsThatChanged() {
        // A whole-object snapshot makes the operation proportional to the component, not to the
        // change: a goody hut used to ship ~4 KB of civilization JSON to describe 24 bytes of gold and
        // science. Recording only the changed fields keeps the payload proportional to the change.
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        val before = testGame.gameInfo.clone()
        before.setTransients()
        // Normalize the live game the same way: setTransients() itself rewrites exploredRegion and the
        // great-person counters, so an unnormalized "after" would report changes nobody made.
        testGame.gameInfo.setTransients()
        val fullCivJsonLength = json().toJson(civ).length

        civ.variables["modEffect"] = 37
        val result = SimultaneousTurnOperations.captureGameStateChange(
            UnitActionType.TriggerUnique, before, testGame.gameInfo
        )!!

        val snapshot = result.civilizations.single()
        assertEquals(listOf("variables"), snapshot.changedFields)
        assertFalse(
            "an unchanged field must not be shipped with the operation",
            snapshot.after!!.contains("civName")
        )
        assertTrue(
            "a one-field change must not carry most of the civilization",
            snapshot.after!!.length * 2 < fullCivJsonLength
        )

        val authoritative = before.clone()
        authoritative.setTransients()
        assertTrue(SimultaneousTurnReplay.apply(authoritative, SimultaneousTurnOperation(
            type = "game.state",
            payload = json().toJson(result)
        )))
        assertEquals(37, authoritative.getCivilization(civ.civName).variables["modEffect"])
    }

    @Test
    fun aFieldLevelSnapshotMergesInsteadOfRejectingAnUnrelatedField() {
        // The old whole-object check rejected the operation as soon as anything in the component
        // moved, even a field this operation never touched. Per-field checking keeps the conflict
        // local to the fields the operation actually describes.
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        val before = testGame.gameInfo.clone()
        before.setTransients()
        testGame.gameInfo.setTransients()

        civ.variables["modEffect"] = 37
        val result = SimultaneousTurnOperations.captureGameStateChange(
            UnitActionType.TriggerUnique, before, testGame.gameInfo
        )!!

        val authoritative = before.clone()
        authoritative.setTransients()
        // Another operation already moved a field this one does not mention.
        val unrelatedFieldValue = authoritative.getCivilization(civ.civName).totalTurnTimeSeconds + 5
        authoritative.getCivilization(civ.civName).totalTurnTimeSeconds = unrelatedFieldValue

        val applied = SimultaneousTurnReplay.apply(authoritative, SimultaneousTurnOperation(
            type = "game.state",
            payload = json().toJson(result)
        ))

        assertTrue("an unrelated field must not reject the operation", applied)
        assertEquals(37, authoritative.getCivilization(civ.civName).variables["modEffect"])
        assertEquals(
            unrelatedFieldValue,
            authoritative.getCivilization(civ.civName).totalTurnTimeSeconds
        )
    }

    @Test
    fun anEscortedSwapIsAppliedFromItsRecordedResultInsteadOfBeingReplayed() {
        // A swap of an escort pair moves two units on each side, and the swap result alone cannot
        // express that - so the recorder captures a state snapshot first, like a move does. Replaying
        // both must reproduce the actor's state, with the snapshot carrying the pair.
        val fixture = escortedSwapFixture()
        val operations = listOf(fixture.snapshotOperation, fixture.swapOperation)
        val authoritative = fixture.turnStart.clone()
        authoritative.setTransients()

        val failed = SimultaneousTurnReplay.replay(authoritative, operations)

        assertEquals(emptyList<SimultaneousTurnOperation>(), failed)
        assertNull(
            "the recorded snapshot and swap must reproduce the swapped state exactly",
            SimultaneousTurnOperations.diffUnrecordedState(
                fixture.turnStart, fixture.actor, operations
            )
        )
        assertEscortPairsSwapped(fixture, authoritative)
    }

    @Test
    fun aSwapsResultSnapshotAloneReproducesBothEscortPairs() {
        // This snapshot holding the whole pair is what makes the escort branch of applySwap a
        // compatibility path for old operations rather than the way current swaps are settled.
        val fixture = escortedSwapFixture()
        val operations = listOf(fixture.snapshotOperation)
        val authoritative = fixture.turnStart.clone()
        authoritative.setTransients()

        val failed = SimultaneousTurnReplay.replay(authoritative, operations)

        assertEquals(emptyList<SimultaneousTurnOperation>(), failed)
        assertNull(
            "the snapshot alone must describe the whole pair swap",
            SimultaneousTurnOperations.diffUnrecordedState(
                fixture.turnStart, fixture.actor, operations
            )
        )
        assertEscortPairsSwapped(fixture, authoritative)
    }

    @Test
    fun aSwapRecordedWithoutASnapshotStillFallsBackToTheMovementRules() {
        // Operations uploaded by older clients carry no snapshot, so re-deriving the pair is the only
        // way to apply them - that path must keep working.
        val fixture = escortedSwapFixture()
        val authoritative = fixture.turnStart.clone()
        authoritative.setTransients()

        val failed = SimultaneousTurnReplay.replay(authoritative, listOf(fixture.swapOperation))

        assertEquals(emptyList<SimultaneousTurnOperation>(), failed)
        assertEscortPairsSwapped(fixture, authoritative)
    }

    /** One civ with an escorting military+settler pair, and a lone military unit next to it. */
    private class EscortedSwapFixture(
        val civ: Civilization,
        val origin: Tile,
        val destination: Tile,
        val warrior: MapUnit,
        val settler: MapUnit,
        val swappedWarrior: MapUnit,
        val turnStart: GameInfo,
        val actor: GameInfo,
        val snapshotOperation: SimultaneousTurnOperation,
        val swapOperation: SimultaneousTurnOperation
    )

    private fun escortedSwapFixture(): EscortedSwapFixture {
        val testGame = TestGame()
        testGame.makeHexagonalMap(3)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        // A player can only swap with their own units, and the escort pair is the interesting case:
        // it moves two units while the recorded swap result names only one of them.
        val otherCiv = testGame.addCiv(testGame.ruleset.nations.values.elementAt(1), isPlayer = true)
        testGame.addUnit("Warrior", otherCiv, testGame.getTile(0, 2))
        val origin = testGame.getTile(0, 0)
        val destination = testGame.getTile(1, 0)
        val warrior = testGame.addUnit("Warrior", civ, origin)
        val settler = testGame.addUnit("Settler", civ, origin)
        warrior.startEscorting()
        val swappedWarrior = testGame.addUnit("Warrior", civ, destination)
        testGame.gameInfo.setTransients()

        val turnStart = testGame.gameInfo.clone()
        turnStart.setTransients()
        // The acting client's own state, and the swap it performed on it.
        val actor = turnStart.clone()
        actor.setTransients()
        val before = actor.clone()
        before.setTransients()
        val actorWarrior = actor.getCivilization(civ.civName).units.getUnitById(warrior.id)!!
        actorWarrior.movement.swapMoveToTile(actor.tileMap[destination.position], keepEscorting = true)

        return EscortedSwapFixture(
            civ = civ, origin = origin, destination = destination,
            warrior = warrior, settler = settler, swappedWarrior = swappedWarrior,
            turnStart = turnStart, actor = actor,
            snapshotOperation = SimultaneousTurnOperation(
                type = "game.state",
                payload = json().toJson(
                    SimultaneousTurnOperations.captureGameStateChange(
                        UnitActionType.TriggerUnique, before, actor
                    )!!
                )
            ),
            swapOperation = SimultaneousTurnOperation(
                type = "unit.swap",
                payload = json().toJson(
                    SimultaneousTurnSwapResult(
                        unitId = warrior.id, owner = civ.civName,
                        fromX = origin.position.x, fromY = origin.position.y,
                        toX = destination.position.x, toY = destination.position.y,
                        health = actorWarrior.health, movement = actorWarrior.currentMovement
                    )
                )
            )
        )
    }

    private fun assertEscortPairsSwapped(fixture: EscortedSwapFixture, gameInfo: GameInfo) {
        assertUnitOn(gameInfo, fixture.civ.civName, fixture.warrior.id, fixture.destination.position)
        assertUnitOn(gameInfo, fixture.civ.civName, fixture.settler.id, fixture.destination.position)
        assertUnitOn(gameInfo, fixture.civ.civName, fixture.swappedWarrior.id, fixture.origin.position)
        val warrior = gameInfo.getCivilization(fixture.civ.civName).units.getUnitById(fixture.warrior.id)!!
        assertTrue("the pair must still escort each other after the swap", warrior.isEscorting())
        val settler = gameInfo.getCivilization(fixture.civ.civName).units.getUnitById(fixture.settler.id)!!
        assertTrue("the civilian half of the pair must travel with it", settler.isEscorting())
    }

    private fun assertUnitOn(gameInfo: GameInfo, civName: String, unitId: Int, position: HexCoord) {
        val unit = gameInfo.getCivilization(civName).units.getUnitById(unitId)
        assertNotNull("unit $unitId must still exist", unit)
        assertEquals("unit $unitId x", position.x, unit!!.currentTile.position.x)
        assertEquals("unit $unitId y", position.y, unit.currentTile.position.y)
    }
}
