package com.unciv.logic.multiplayer

import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.AlertType
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.PopupAlert
import com.unciv.logic.files.UncivFiles
import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.logic.scripting.LuaAPI
import com.unciv.models.UnitActionType
import com.unciv.models.metadata.GameParameters
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.models.stats.Stat
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import com.unciv.view.GameView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.luaj.vm2.LuaValue

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
    fun twoCapitalsReplayFromTheSameTurnStartWithoutClaimingEachOthersCivilization() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(10)
        val game = testGame.gameInfo
        game.gameParameters.isOnlineMultiplayer = true
        game.gameParameters.simultaneousTurns = true
        val rome = testGame.addCiv(testGame.ruleset.nations.getValue("Rome"), isPlayer = true)
        val greece = testGame.addCiv(testGame.ruleset.nations.getValue("Greece"), isPlayer = true)
        testGame.addUnit("Settler", rome, testGame.getTile(-5, 0))
        testGame.addUnit("Settler", greece, testGame.getTile(5, 0))
        val warrior = testGame.addUnit("Warrior", rome, testGame.getTile(-6, 0))
        warrior.due = false
        val start = json().toJson(game)
        fun foundCapital(civName: String): SimultaneousTurnOperation {
            val local = UncivFiles.gameInfoFromString(start)
            val before = local.clone()
            val civ = local.getCivilization(civName)
            val settler = civ.units.getCivUnits().first { it.name == "Settler" }
            civ.addCity(settler.currentTile.position, settler)
            settler.destroy()
            val result = SimultaneousTurnOperations.captureGameStateChange(UnitActionType.FoundCity, before, local)!!
            return SimultaneousTurnOperation(type = "state.FoundCity", payload = json().toJson(result))
        }

        val romanCapital = foundCapital(rome.civName)
        val greekCapital = foundCapital(greece.civName)
        val romanKeys = RealtimeTurnActions.conflictKeys(listOf(romanCapital)).toSet()
        val greekKeys = RealtimeTurnActions.conflictKeys(listOf(greekCapital)).toSet()
        assertTrue(romanKeys.intersect(greekKeys).isEmpty())
        assertFalse(SimultaneousTurnClaims.forUnit(warrior.id) in greekKeys)

        for (operations in listOf(listOf(romanCapital, greekCapital), listOf(greekCapital, romanCapital))) {
            val observer = UncivFiles.gameInfoFromString(start)
            for (operation in operations) assertNull(SimultaneousTurnReplay.applyOrExplain(observer, operation))
            val settledRome = observer.getCivilization(rome.civName)
            val settledGreece = observer.getCivilization(greece.civName)
            assertEquals(HexCoord(-5, 0), settledRome.cities.single().location)
            assertEquals(HexCoord(5, 0), settledGreece.cities.single().location)
            assertEquals(com.unciv.logic.civilization.Proximity.Close, settledRome.getProximity(greece.civName))
            assertEquals(settledRome.getProximity(greece.civName), settledGreece.getProximity(rome.civName))
            assertFalse(settledRome.units.getUnitById(warrior.id)!!.due)
        }
    }

    @Test
    fun foundingAfterCyclingAUnitReplaysWithoutChangingThePeersUnitQueue() {
        val testGame = TestGame().apply { makeHexagonalMap(4) }
        val game = testGame.gameInfo
        game.gameParameters.isOnlineMultiplayer = true
        game.gameParameters.simultaneousTurns = true
        val civ = testGame.addCiv(testGame.ruleset.nations.getValue("Rome"), isPlayer = true)
        val warrior = testGame.addUnit("Warrior", civ, testGame.getTile(-1, 1))
        val settler = testGame.addUnit("Settler", civ, testGame.getTile(0, 0))
        val observer = UncivFiles.gameInfoFromString(json().toJson(game))
        val beforeMove = game.clone()
        warrior.movement.moveToTile(testGame.getTile(0, 1))
        val move = checkNotNull(SimultaneousTurnOperations.captureGameStateChange(SimultaneousTurnStateAction.Move, beforeMove, game))
        warrior.due = false // Automatic unit cycling happens after recording the move.
        val beforeCity = game.clone()
        civ.addCity(settler.currentTile.position, settler)
        settler.destroy()
        val found = checkNotNull(SimultaneousTurnOperations.captureGameStateChange(UnitActionType.FoundCity, beforeCity, game))
        val moveOperation = SimultaneousTurnOperation(type = "state.Move", payload = json().toJson(move))
        val foundOperation = SimultaneousTurnOperation(type = "state.FoundCity", payload = json().toJson(found))
        assertTrue(found.tiles.any { it.key == "0,1" })
        assertNull(SimultaneousTurnReplay.applyOrExplain(observer, moveOperation))
        assertNull(SimultaneousTurnReplay.applyOrExplain(observer, foundOperation))
        val peer = observer.getCivilization(civ.civID)
        assertEquals(HexCoord(0, 0), peer.cities.single().location)
        assertEquals(HexCoord(0, 1), peer.units.getUnitById(warrior.id)!!.currentTile.position)
        assertTrue(peer.units.getUnitById(warrior.id)!!.due)
    }

    @Test
    fun firstContactMoveReplaysAcrossDifferentIntroDismissalStates() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(5)
        val game = testGame.gameInfo
        val china = testGame.addCiv(testGame.ruleset.nations.getValue("China"), isPlayer = true)
        val france = testGame.addCiv(testGame.ruleset.nations.getValue("France"), isPlayer = true)
        game.gameParameters.isOnlineMultiplayer = true
        game.gameParameters.simultaneousTurns = true
        UncivGame.Current.settings.tutorialTasksCompleted.add("Meet another civilization")
        val warrior = testGame.addUnit("Warrior", china, testGame.getTile(-2, 0))
        val frenchWarrior = testGame.addUnit("Warrior", france, testGame.getTile(1, 0))
        assertFalse(china.knows(france))
        for (civ in listOf(china, france)) {
            civ.popupAlerts.clear()
            civ.popupAlerts.add(PopupAlert(AlertType.StartIntro, ""))
        }

        val turnStart = json().toJson(game)
        val observer = UncivFiles.gameInfoFromString(turnStart)
        val observerFrance = observer.getCivilization(france.civID)
        // Each game window closes only its own opening popup.
        china.popupAlerts.clear()
        observerFrance.popupAlerts.clear()
        val before = game.clone()
        warrior.movement.moveToTile(testGame.getTile(-1, 0))
        assertTrue(china.knows(france))
        val result = SimultaneousTurnOperations.captureGameStateChange(SimultaneousTurnStateAction.Move, before, game)!!
        val operation = SimultaneousTurnOperation(type = "state.Move", payload = json().toJson(result))

        assertNull("The observer must replay the accepted move", SimultaneousTurnReplay.applyOrExplain(observer, operation))
        assertEquals(HexCoord(-1, 0), observer.getCivilization(china.civID).units.getUnitById(warrior.id)!!.currentTile.position)
        assertEquals(listOf(AlertType.FirstContact), observerFrance.popupAlerts.map { it.type })
        assertNull(SimultaneousTurnReplay.applyOrExplain(observer, operation))
        assertEquals(listOf(AlertType.FirstContact), observerFrance.popupAlerts.map { it.type })

        val peerBefore = observer.clone()
        observerFrance.popupAlerts.clear()
        observerFrance.units.getUnitById(frenchWarrior.id)!!.movement.moveToTile(observer.tileMap[2, 0])
        val response = SimultaneousTurnOperations.captureGameStateChange(SimultaneousTurnStateAction.Move, peerBefore, observer)!!
        val peerOperation = SimultaneousTurnOperation(type = "state.Move", payload = json().toJson(response))

        val settlement = UncivFiles.gameInfoFromString(turnStart)
        assertEquals(emptyList<SimultaneousTurnOperation>(), SimultaneousTurnReplay.replay(settlement, listOf(operation, peerOperation)))
        settlement.nextTurnPolling()
        assertEquals(1, settlement.turns)
        assertEquals(HexCoord(-1, 0), settlement.getCivilization(china.civID).units.getUnitById(warrior.id)!!.currentTile.position)
        assertEquals(HexCoord(2, 0), settlement.getCivilization(france.civID).units.getUnitById(frenchWarrior.id)!!.currentTile.position)
    }

    @Test
    fun revealingATileDoesNotClaimOrOverwriteItsOccupant() {
        val testGame = TestGame().apply { makeHexagonalMap(4) }
        val china = testGame.addCiv(testGame.ruleset.nations.getValue("China"), isPlayer = true)
        val france = testGame.addCiv(testGame.ruleset.nations.getValue("France"), isPlayer = true)
        val origin = testGame.getTile(1, 0)
        val warrior = testGame.addUnit("Warrior", france, origin)
        val game = testGame.gameInfo
        val observer = UncivFiles.gameInfoFromString(json().toJson(game))
        val before = game.clone()
        origin.setExplored(china, true)
        val result = checkNotNull(SimultaneousTurnOperations.captureGameStateChange(SimultaneousTurnStateAction.Move, before, game))
        val operation = SimultaneousTurnOperation(type = "state.Move", payload = json().toJson(result))
        val keys = RealtimeTurnActions.requestFor(operation).conflictKeys
        assertFalse("Exploration must not reserve the tile", "tile:1,0" in keys)
        assertFalse("Exploration must not reserve its occupant", "unit:${france.civID}:${warrior.id}" in keys)

        val observerFrance = observer.getCivilization(france.civID)
        val observerOrigin = observer.tileMap[origin.position]
        val destination = observer.tileMap[2, 0]
        observerFrance.units.getUnitById(warrior.id)!!.movement.moveToTile(destination)
        observerOrigin.improvement = "Farm"

        assertNull(SimultaneousTurnReplay.applyOrExplain(observer, operation))
        assertEquals(warrior.id, destination.militaryUnit?.id)
        assertNull(observerOrigin.militaryUnit)
        assertEquals("Farm", observerOrigin.improvement)
        assertTrue(observerOrigin.isExplored(observer.getCivilization(china.civID)))
        assertTrue(observerOrigin.isExplored(observerFrance))
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
    fun paidUpgradeFromUnitViewReplaysUnitsAndGold() {
        for (upgradeCount in 1..2) {
            val testGame = TestGame()
            testGame.makeHexagonalMap(3)
            val upgradedUnit = testGame.ruleset.units.getValue("Composite Bowman")
            val civ = testGame.addCiv(testGame.ruleset.nations.getValue("Rome"), isPlayer = true)
            testGame.addCity(civ, testGame.getTile(0, 0))
            civ.tech.addTechnology("Construction", false)
            val units = (0 until upgradeCount).map { testGame.addUnit("Archer", civ, testGame.getTile(it, 0)) }
            civ.addGold(1000)
            civ.units.clearUnits()
            testGame.gameInfo.setTransients()
            val before = testGame.gameInfo.clone()
            val view = GameView(testGame.gameInfo, civ)
            for (unit in units) assertTrue(view.getMapUnitView(unit).tryUpgradeTo(upgradedUnit))
            assertTrue(civ.gold < 1000)

            val result = SimultaneousTurnOperations.captureGameStateChange(UnitActionType.Upgrade, before, testGame.gameInfo)!!
            val operations = SimultaneousTurnOperations.splitGameStateResult(result).mapIndexed { index, part ->
                SimultaneousTurnOperation(
                    sequence = index.toLong(),
                    type = SimultaneousTurnOperations.stateOperationType(UnitActionType.Upgrade.name),
                    payload = json().toJson(part)
                )
            }
            before.setTransients()
            assertEquals(emptyList<SimultaneousTurnFailedOperation>(), SimultaneousTurnReplay.replayOrExplain(before, operations))
            val replayedCiv = before.getCivilization(civ.civName)
            assertEquals(civ.gold, replayedCiv.gold)
            assertEquals(List(upgradeCount) { upgradedUnit.name }, replayedCiv.units.getCivUnits().map { it.name }.toList())
        }
    }

    @Test
    fun upgradeThatSpendsAllGoldReplaysZeroBalance() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(3)
        val civ = testGame.addCiv(testGame.ruleset.nations.getValue("Rome"), isPlayer = true)
        testGame.addCity(civ, testGame.getTile(0, 0))
        civ.tech.addTechnology("Construction", false)
        val unit = testGame.addUnit("Archer", civ, testGame.getTile(0, 0))
        val upgrade = testGame.ruleset.units.getValue("Composite Bowman")
        val upgradeCost = unit.upgrade.getCostOfUpgrade(upgrade)
        civ.addGold(upgradeCost)
        testGame.gameInfo.setTransients()
        val before = testGame.gameInfo.clone()
        before.setTransients()

        val view = GameView(testGame.gameInfo, civ)
        assertTrue(view.getMapUnitView(unit).tryUpgradeTo(upgrade))
        assertEquals(0, civ.gold)

        val result = SimultaneousTurnOperations.captureGameStateChange(
            UnitActionType.Upgrade, before, testGame.gameInfo
        )!!
        val operations = SimultaneousTurnOperations.splitGameStateResult(result).mapIndexed { index, part ->
            SimultaneousTurnOperation(
                sequence = index.toLong(),
                type = SimultaneousTurnOperations.stateOperationType(UnitActionType.Upgrade.name),
                payload = json().toJson(part)
            )
        }
        before.setTransients()
        assertEquals(emptyList<SimultaneousTurnFailedOperation>(), SimultaneousTurnReplay.replayOrExplain(before, operations))
        assertEquals(0, before.getCivilization(civ.civName).gold)
    }

    @Test
    fun simultaneousUnitPurchasesReceiveUniqueIds() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(5)
        val civA = testGame.addCiv(testGame.ruleset.nations.getValue("Rome"), isPlayer = true)
        val civB = testGame.addCiv(testGame.ruleset.nations.getValue("Egypt"), isPlayer = true)
        val cityA = testGame.addCity(civA, testGame.getTile(0, 0))
        val cityB = testGame.addCity(civB, testGame.getTile(4, 0))
        civA.addGold(1000)
        civB.addGold(1000)
        testGame.gameInfo.setTransients()
        val turnStart = testGame.gameInfo.clone()
        turnStart.setTransients()

        fun purchaseUnit(civName: String, cityPosition: HexCoord, playerId: String): List<SimultaneousTurnOperation> {
            val client = turnStart.clone()
            client.setTransients()
            val civ = client.getCivilization(civName)
            val city = civ.cities.first { it.location == cityPosition }
            val unit = client.ruleset.units.getValue("Warrior")
            repeat(2) {
                assertTrue(city.cityConstructions.purchaseConstruction(unit.name, -1, false, Stat.Gold))
            }
            val result = SimultaneousTurnOperations.captureGameStateChange(
                UnitActionType.ConstructImprovement, turnStart, client
            )!!
            return SimultaneousTurnOperations.splitGameStateResult(result).mapIndexed { index, part ->
                SimultaneousTurnOperation(
                    playerId = playerId,
                    sequence = index.toLong(),
                    type = SimultaneousTurnOperations.stateOperationType(UnitActionType.ConstructImprovement.name),
                    payload = json().toJson(part)
                )
            }
        }

        val operations = purchaseUnit(civA.civName, cityA.location, "player-a") +
            purchaseUnit(civB.civName, cityB.location, "player-b")
        assertEquals(emptyList<SimultaneousTurnFailedOperation>(), SimultaneousTurnReplay.replayOrExplain(turnStart, operations))

        val units = turnStart.civilizations.flatMap { it.units.getCivUnits().toList() }
        assertEquals(4, units.size)
        assertEquals(4, units.map { it.id }.toSet().size)
    }

    @Test
    fun fieldLevelTileSnapshotPreservesUnchangedUnits() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(3)
        val civ = testGame.addCiv(testGame.ruleset.nations.getValue("Rome"), isPlayer = true)
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        testGame.gameInfo.setTransients()
        val before = testGame.gameInfo.clone()
        before.setTransients()
        val after = testGame.gameInfo.clone()
        after.setTransients()
        after.tileMap[0, 0].improvementIsPillaged = true

        val result = SimultaneousTurnOperations.captureGameStateChange(
            UnitActionType.Repair, before, after
        )!!
        val tileSnapshot = SimultaneousTurnComponentSnapshot(
            "0,0", "{\"improvementIsPillaged\":false}", "{\"improvementIsPillaged\":true}",
            arrayListOf("improvementIsPillaged")
        )
        val partialResult = result.copy(tiles = arrayListOf(tileSnapshot))
        val operation = SimultaneousTurnOperation(
            type = SimultaneousTurnOperations.stateOperationType(UnitActionType.Repair.name),
            payload = json().toJson(partialResult)
        )

        assertTrue(SimultaneousTurnReplay.apply(before, operation))
        val replayedUnit = before.getCivilization(civ.civName).units.getUnitById(unit.id)
        assertNotNull(replayedUnit)
        assertTrue(before.tileMap[0, 0].improvementIsPillaged)
    }

    @Test
    fun recordedLuaStorageWritesMergePerKeyAndRejectConflictingActions() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val civ = testGame.addCiv(testGame.ruleset.nations.getValue("Rome"), isPlayer = true)
        val start = testGame.gameInfo
        start.setTransients()

        fun record(key: String, value: String, gold: Int = 0): SimultaneousTurnOperation {
            val after = start.clone().apply { setTransients() }
            val actor = after.getCivilization(civ.civName)
            val store = LuaAPI.buildContext(actor, null, null, null, "", GameContext(actor), "Test Mod").get("store")
            assertEquals("missing", store.get("get").call(LuaValue.valueOf(key), LuaValue.valueOf("missing")).tojstring())
            assertTrue(after.modLuaStorage.isEmpty())
            store.get("set").call(LuaValue.valueOf(key), LuaValue.valueOf(value))
            actor.addGold(gold)
            val state = SimultaneousTurnOperations.captureGameStateChange(UnitActionType.TriggerUnique, start, after)!!
            return SimultaneousTurnOperation(type = "state.TriggerUnique", payload = json().toJson(
                SimultaneousTurnOperations.splitGameStateResult(state).single()
            ))
        }

        val first = record("count", "1")
        val independent = record("other", "2")
        val stale = record("count", "3", gold = 50)
        val replayed = start.clone().apply { setTransients() }
        assertEquals(listOf(SimultaneousTurnFailedOperation(stale, SimultaneousTurnFailureReason.WorldChanged)),
            SimultaneousTurnReplay.replayOrExplain(replayed, listOf(first, independent, stale)))
        assertEquals(mapOf("count" to "1", "other" to "2"), replayed.modLuaStorage["Test Mod"])
        assertEquals(civ.gold, replayed.getCivilization(civ.civName).gold)
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
            UnitActionType.StopMovement, UnitActionType.StopAutomation
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
    fun aSpectatorIsNeverRequiredToSubmitATurn() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(3)
        val player = testGame.addCiv(isPlayer = true).apply { playerId = "player" }
        testGame.addUnit("Warrior", player, testGame.getTile(0, 0))
        val spectator = testGame.addCiv(testGame.ruleset.nations.getValue(Constants.spectator), isPlayer = true)
        spectator.playerId = "spectator"
        val game = testGame.gameInfo
        game.turns = 3
        val previousDone = SimultaneousTurnOperation(turn = 2, playerId = player.playerId, type = "done")
        val currentDone = previousDone.copy(turn = 3)

        assertTrue(spectator.isSpectator && spectator.isHuman() && spectator.isAlive())
        assertEquals(setOf(player.playerId), SimultaneousTurnOperations.getUnsubmittedPlayerIds(game, listOf(previousDone)))
        assertTrue(SimultaneousTurnOperations.getUnsubmittedPlayerIds(game, listOf(previousDone, currentDone)).isEmpty())
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
                failedOperation(1, "player-a", 4, "unit.attack", SimultaneousTurnFailureReason.WorldChanged),
                failedOperation(1, "player-b", 5, "unit.move", SimultaneousTurnFailureReason.UnitGone)
            )
        )

        assertNotNull(report)
        assertTrue(report!!.contains(civ.civName))
        assertTrue("the other player's civ must be named too", report.contains(otherCiv.civName))
        assertTrue("the report must name the action, not its type: $report", report.contains("Attack"))
        assertTrue("the report must name the action, not its type: $report", report.contains("Move unit"))
        assertFalse("the report must not leak the internal type: $report", report.contains("unit.attack"))
        assertTrue(
            "the report must say why each action was lost: $report",
            report.contains(SimultaneousTurnFailureReason.WorldChanged.sentence) &&
                report.contains(SimultaneousTurnFailureReason.UnitGone.sentence)
        )
    }

    @Test
    fun aStateChangeRecordsTheActionThatCausedIt() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        val before = testGame.gameInfo.clone()
        before.setTransients()

        civ.variables["modEffect"] = 37
        val result = SimultaneousTurnOperations.captureGameStateChange(
            SimultaneousTurnStateAction.Trade, before, testGame.gameInfo
        )!!
        assertEquals("Trade", result.action)

        val operationType = SimultaneousTurnOperations.stateOperationType(result.action)
        assertEquals("state.Trade", operationType)
        assertTrue(SimultaneousTurnOperations.isStateOperationType(operationType))
        assertTrue(
            "the generic type older clients recorded must keep replaying",
            SimultaneousTurnOperations.isStateOperationType("game.state")
        )
        assertFalse(SimultaneousTurnOperations.isStateOperationType("unit.move"))

        val authoritative = before.clone()
        authoritative.setTransients()
        assertTrue(SimultaneousTurnReplay.apply(authoritative, SimultaneousTurnOperation(
            type = operationType,
            payload = json().toJson(result)
        )))
        assertEquals(37, authoritative.getCivilization(civ.civName).variables["modEffect"])
    }

    @Test
    fun failedActionsAreNamedInsteadOfTheirOperationType() {
        // Every state change used to be reported as "game.state", and unit operations as their
        // internal types, which told the player losing the action nothing.
        assertEquals("Trade", SimultaneousTurnOperations.describeOperationType("state.Trade"))
        assertEquals("Automate", SimultaneousTurnOperations.describeOperationType("state.Automate"))
        assertEquals("Move unit", SimultaneousTurnOperations.describeOperationType("unit.move"))
        assertEquals("Attack", SimultaneousTurnOperations.describeOperationType("unit.attack"))
        assertEquals("Swap units", SimultaneousTurnOperations.describeOperationType("unit.swap"))
        assertEquals("Action", SimultaneousTurnOperations.describeOperationType("unit.action"))
        assertEquals("game.state", SimultaneousTurnOperations.describeOperationType("game.state"))
        assertEquals("state.Unknown", SimultaneousTurnOperations.describeOperationType("state.Unknown"))

        // A unit action operation can say which action the unit was given.
        assertEquals(
            "Guard",
            SimultaneousTurnOperations.describeOperation(
                SimultaneousTurnOperation(
                    type = "unit.action",
                    payload = json().toJson(SimultaneousTurnUnitActionResult(unitId = 7, owner = "player-a", action = "Guard"))
                )
            )
        )
        // Without a readable action it still gets a name instead of the internal type.
        assertEquals(
            "Action",
            SimultaneousTurnOperations.describeOperation(
                SimultaneousTurnOperation(type = "unit.action", payload = "not json")
            )
        )
        assertEquals(
            "Move unit",
            SimultaneousTurnOperations.describeOperation(SimultaneousTurnOperation(type = "unit.move"))
        )

        val testGame = TestGame()
        testGame.makeHexagonalMap(3)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        val otherCiv = testGame.addCiv(testGame.ruleset.nations.values.last(), isPlayer = true)
        civ.playerId = "player-a"
        otherCiv.playerId = "player-b"
        // Adding a civ can leave notifications of its own behind, so count the delta instead of the total.
        val civNotificationsBefore = civ.notifications.size
        val otherCivNotificationsBefore = otherCiv.notifications.size

        val notified = SimultaneousTurnOperations.notifyPlayersOfFailedOperations(
            testGame.gameInfo,
            listOf(
                failedOperation(1, "player-a", 4, "state.Trade", SimultaneousTurnFailureReason.WorldChanged),
                failedOperation(1, "player-a", 5, "state.Trade", SimultaneousTurnFailureReason.WorldChanged),
                failedOperation(1, "player-b", 6, "unit.move", SimultaneousTurnFailureReason.UnitGone)
            )
        )

        assertEquals(listOf("player-a", "player-b"), notified)
        // Two failed operations of the same kind are one problem, not two.
        assertEquals("one notification per distinct action", civNotificationsBefore + 1, civ.notifications.size)
        val text = civ.notifications.last().text
        assertTrue("the notification must name the action: $text", text.contains("[Trade]"))
        assertFalse("the operation type must not leak to the player: $text", text.contains("game.state"))
        assertTrue(
            "the notification must name the reason: $text",
            text.contains("[${SimultaneousTurnFailureReason.WorldChanged.sentence}]")
        )
        assertEquals(
            "an operation that is not a state change also gets one notification",
            otherCivNotificationsBefore + 1, otherCiv.notifications.size
        )
        assertTrue(
            "a unit operation is named instead of leaking its internal type",
            otherCiv.notifications.last().text.contains("[Move unit]")
        )
        assertFalse(
            "the unit operation type must not leak to the player",
            otherCiv.notifications.last().text.contains("unit.move")
        )
        assertTrue(
            "a unit operation names its own reason",
            otherCiv.notifications.last().text.contains("[${SimultaneousTurnFailureReason.UnitGone.sentence}]")
        )
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
    fun reorderingObjectKeysDoesNotMakeAnActionConflict() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        testGame.gameInfo.setTransients()
        civ.variables = hashMapOf("a" to 1, "b" to 2)
        assertTrue(json().toJson(civ).contains(""""variables":{"a":1,"b":2}"""))
        val global = json().toJson(SimultaneousTurnOperations.currentGlobalState(testGame.gameInfo))
        val action = SimultaneousTurnGameStateResult(
            globalBefore = global,
            globalAfter = global,
            civilizations = arrayListOf(SimultaneousTurnComponentSnapshot(
                key = civ.civName,
                before = """{"variables":{"b":2,"a":1}}""",
                after = """{"variables":{"b":2,"a":3}}""",
                changedFields = arrayListOf("variables")
            ))
        )
        assertTrue(SimultaneousTurnReplay.apply(testGame.gameInfo, SimultaneousTurnOperation(
            type = "game.state", payload = json().toJson(action)
        )))
        assertEquals(mapOf("a" to 3, "b" to 2), testGame.gameInfo.getCivilization(civ.civName).variables)
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

    @Test
    fun ignoredOperationsAreReportedToThePlayerWhoLostThem() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val humanCiv = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        humanCiv.playerId = "playerA"
        val aiCiv = testGame.addCiv(testGame.ruleset.nations.values.elementAt(1))
        aiCiv.playerId = "playerB"
        val notificationsBefore = humanCiv.notifications.size
        val aiNotificationsBefore = aiCiv.notifications.size

        val failed = listOf(
            failedOperation(3, "playerA", 1, "unit.move", SimultaneousTurnFailureReason.UnitGone),
            failedOperation(3, "playerA", 2, "unit.attack", SimultaneousTurnFailureReason.WorldChanged),
            // An AI civ, and a playerId that no longer exists, must not break the report.
            failedOperation(3, "playerB", 3, "unit.move", SimultaneousTurnFailureReason.UnitGone),
            failedOperation(3, "playerC", 4, "unit.move", SimultaneousTurnFailureReason.UnitGone)
        )

        val notified = SimultaneousTurnOperations.notifyPlayersOfFailedOperations(testGame.gameInfo, failed)

        assertEquals(listOf("playerA"), notified)
        // Every distinct lost action is reported on its own, so that the message can name it
        // in the language of whichever client displays it.
        assertEquals("one notification per distinct lost action", notificationsBefore + 2, humanCiv.notifications.size)
        val texts = humanCiv.notifications.drop(notificationsBefore).map { it.text }
        assertTrue(
            "the report must name the lost actions, not their types: $texts",
            texts.any { it.contains("[Move unit]") } && texts.any { it.contains("[Attack]") }
        )
        assertFalse(
            "the report must not leak the internal operation types: $texts",
            texts.any { it.contains("unit.move") || it.contains("unit.attack") }
        )
        assertTrue(
            "the report must say why each action was lost: $texts",
            texts.any { it.contains("[${SimultaneousTurnFailureReason.UnitGone.sentence}]") } &&
                texts.any { it.contains("[${SimultaneousTurnFailureReason.WorldChanged.sentence}]") }
        )
        assertTrue(
            "the report must not describe the operations in bulk: $texts",
            texts.all { it.contains("could not apply one of your actions") }
        )
        assertEquals("AI civs have no use for these notifications", aiNotificationsBefore, aiCiv.notifications.size)
    }

    @Test
    fun failedOperationsCarryTheReasonTheyWereRefused() {
        // "Which action was lost" is not actionable on its own (plan 7.5): the player needs to know
        // whether the unit is gone, whether the world had already moved on, or whether the operation
        // was corrupt - the three cases need different reactions.
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)
        val civ = testGame.addCiv(testGame.ruleset.nations.values.first(), isPlayer = true)
        civ.playerId = "player-a"
        val before = testGame.gameInfo.clone()
        before.setTransients()
        civ.variables["modEffect"] = 37
        val worldSnapshot = SimultaneousTurnOperations.captureGameStateChange(
            UnitActionType.TriggerUnique, before, testGame.gameInfo
        )!!

        // The unit this move names never existed on the authoritative board.
        val missingUnit = SimultaneousTurnOperation(
            type = "unit.move",
            payload = json().toJson(
                SimultaneousTurnMoveResult(
                    unitId = 999999, owner = civ.civName,
                    fromX = 0, fromY = 0, toX = 1, toY = 1, hp = 100, movement = 1f
                )
            )
        )
        // A snapshot may only be applied while the fields it changes still hold their before value.
        val contestedSnapshot = SimultaneousTurnOperation(
            type = "game.state", payload = json().toJson(worldSnapshot)
        )
        val corruptPayload = SimultaneousTurnOperation(type = "unit.move", payload = "{not json")

        val authoritative = before.clone()
        authoritative.setTransients()
        // A third value: neither the snapshot's before (absent) nor its after (37).
        authoritative.getCivilization(civ.civName).variables["modEffect"] = 999

        val failures = SimultaneousTurnReplay.replayOrExplain(
            authoritative, listOf(missingUnit, contestedSnapshot, corruptPayload)
        )

        assertEquals(
            "every refused operation must carry its reason, in order",
            listOf(
                SimultaneousTurnFailureReason.UnitGone,
                SimultaneousTurnFailureReason.WorldChanged,
                SimultaneousTurnFailureReason.Unreadable
            ),
            failures.map { it.reason }
        )
        assertEquals(
            "the reason must stay attached to its own operation",
            listOf(missingUnit, contestedSnapshot, corruptPayload),
            failures.map { it.operation }
        )
    }

    private fun failedOperation(
        turn: Int,
        playerId: String,
        sequence: Long,
        type: String,
        reason: SimultaneousTurnFailureReason
    ) = SimultaneousTurnFailedOperation(
        SimultaneousTurnOperation(turn = turn, playerId = playerId, sequence = sequence, type = type),
        reason
    )
}
