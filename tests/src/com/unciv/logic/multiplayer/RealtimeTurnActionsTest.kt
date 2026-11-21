package com.unciv.logic.multiplayer

import com.unciv.json.json
import com.unciv.testing.BaseTestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(BaseTestRunner::class)
class RealtimeTurnActionsTest {

    @Test
    fun movementConflictsClaimUnitAndBothTiles() {
        val operation = SimultaneousTurnOperation(
            turn = 3,
            playerId = "player-a",
            sequence = 41,
            type = "unit.move",
            payload = json().toJson(SimultaneousTurnMoveResult(
                unitId = 7,
                owner = "Rome",
                fromX = 2,
                fromY = 4,
                toX = 3,
                toY = 5
            ))
        )

        assertEquals(
            listOf("tile:2,4", "tile:3,5", "unit:Rome:7"),
            RealtimeTurnActions.conflictKeys(listOf(operation))
        )
    }

    @Test
    fun stateChangesClaimEveryChangedComponentAndGlobalField() {
        val before = SimultaneousTurnGlobalState(lastUnitId = 4)
        val after = before.copy(lastUnitId = 5)
        val result = SimultaneousTurnGameStateResult(
            action = "FoundCity",
            globalBefore = json().toJson(before),
            globalAfter = json().toJson(after),
            civilizations = arrayListOf(SimultaneousTurnComponentSnapshot(key = "Rome")),
            tiles = arrayListOf(SimultaneousTurnComponentSnapshot(key = "8,9")),
            religions = arrayListOf(SimultaneousTurnComponentSnapshot(key = "Sun")),
            luaStoreChanges = arrayListOf(SimultaneousTurnLuaStoreChange(modName = "M", key = "K"))
        )
        val operation = SimultaneousTurnOperation(
            turn = 0,
            playerId = "player-a",
            sequence = 1,
            type = "state.FoundCity",
            payload = json().toJson(result)
        )

        assertEquals(
            listOf("civ:Rome", "global:lastUnitId", "lua:M:K", "religion:Sun", "tile:8,9"),
            RealtimeTurnActions.conflictKeys(listOf(operation))
        )
    }

    @Test
    fun tileStateChangesAlsoClaimUnitsLeavingOrEnteringTheTile() {
        val result = SimultaneousTurnGameStateResult(
            tiles = arrayListOf(SimultaneousTurnComponentSnapshot(
                key = "2,4",
                before = "{\"militaryUnit\":{\"owner\":\"Rome\",\"id\":7}}",
                after = "{\"civilianUnit\":{\"owner\":\"Greece\",\"id\":9}}"
            ))
        )
        val operation = SimultaneousTurnOperation(
            turn = 0,
            playerId = "player-a",
            sequence = 1,
            type = "state.FoundCity",
            payload = json().toJson(result)
        )

        assertEquals(
            listOf("tile:2,4", "unit:Greece:9", "unit:Rome:7"),
            RealtimeTurnActions.conflictKeys(listOf(operation))
        )
    }

    @Test
    fun retryPreservesTheWirePayloadOfAnOperationWithoutTimestamp() {
        val saved = """[{"turn":2,"playerId":"player-a","sequence":12,"type":"unit.action","payload":"{}"}]"""
        val operation = SimultaneousTurnOperations.decode(saved).single()
        val wire = json().toJson(RealtimeTurnActions.requestFor(operation))
        val reloaded = SimultaneousTurnOperations.decode(SimultaneousTurnOperations.encode(listOf(operation))).single()

        assertEquals(0L, operation.createdAtMillis)
        assertFalse(wire.contains("\"createdAtMillis\""))
        assertEquals(wire, json().toJson(RealtimeTurnActions.requestFor(reloaded)))
    }

    @Test
    fun requestAndFeedJsonRoundTripPreservesOperationData() {
        val operation = SimultaneousTurnOperation(
            turn = 2,
            playerId = "player-a",
            sequence = 12,
            type = "unit.action",
            payload = "{}",
            createdAtMillis = 12345L
        )
        val request = RealtimeTurnActions.requestFor(operation)
        val parsedRequest = json().fromJson(RealtimeTurnActionRequest::class.java, json().toJson(request))
        assertEquals(request.turn, parsedRequest.turn)
        assertEquals(request.clientSequence, parsedRequest.clientSequence)
        assertEquals(request.conflictKeys, parsedRequest.conflictKeys)
        assertEquals(operation, parsedRequest.operations.single())

        val feed = RealtimeTurnActionFeed(
            turn = 2,
            after = 8,
            latestSequence = 12,
            actions = arrayListOf(RealtimeTurnActionEntry(
                turn = 2,
                sequence = 12,
                playerId = "player-a",
                clientSequence = 12,
                kind = "action",
                conflictKeys = request.conflictKeys,
                operations = arrayListOf(operation)
            )),
            readyPlayerIds = arrayListOf("player-b"),
            allPlayersReady = false
        )
        val parsedFeed = json().fromJson(RealtimeTurnActionFeed::class.java, json().toJson(feed))
        assertEquals(feed.latestSequence, parsedFeed.latestSequence)
        assertEquals(operation, parsedFeed.actions.single().operations.single())
        assertEquals(feed.readyPlayerIds, parsedFeed.readyPlayerIds)
    }
}
