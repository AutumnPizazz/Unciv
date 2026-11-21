package com.unciv.ui.screens.worldscreen.worldmap

import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.battle.AttackableTile
import com.unciv.logic.multiplayer.SimultaneousTurnOperation
import com.unciv.logic.multiplayer.SimultaneousTurnOperations
import com.unciv.logic.multiplayer.SimultaneousTurnReplay
import com.unciv.logic.multiplayer.SimultaneousTurnStateAction
import com.unciv.models.UnitActionType
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import com.unciv.ui.screens.worldscreen.UndoHandler
import com.unciv.ui.screens.worldscreen.WorldScreen
import com.unciv.ui.screens.worldscreen.bottombar.BattleTable
import com.unciv.ui.screens.worldscreen.unit.UnitTable
import com.unciv.view.AttackableTileView
import com.unciv.view.GameView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mockito.mock

@RunWith(BaseTestRunner::class)
class SimultaneousTurnMapActionsTest {
    @Test
    fun mapSpyAssignmentReplaysFromTheTurnStart() {
        val fixture = Fixture()
        val city = fixture.game.addCity(fixture.civ, fixture.game.getTile(0, 0))
        val spy = fixture.civ.espionageManager.addSpy()
        val turnStart = fixture.turnStart()
        val overlay = MoveSpyOverlayButtonData(fixture.view.getSpyView(spy), fixture.view.getForeignCityView(city))

        invokeCallback(overlay, "moveSpyAndReturnToEspionage", fixture.holder)

        assertEquals(city.location, spy.getCityOrNull()?.location)
        fixture.replay(turnStart, SimultaneousTurnStateAction.Espionage.name)
        val replayedSpy = turnStart.getCivilization(fixture.civ.civID).espionageManager.spyList.single()
        assertEquals(city.location, replayedSpy.getCityOrNull()?.location)
    }

    @Test
    fun roadOrderReplaysEvenWhenAutomationDoesNotMoveTheWorker() {
        val fixture = Fixture()
        val origin = fixture.game.getTile(0, 0)
        val destination = fixture.game.getTile(1, 0)
        val worker = fixture.game.addUnit("Worker", fixture.civ, origin)
        val turnStart = fixture.turnStart()
        val overlay = ConnectRoadOverlayButtonData(fixture.view.getMapUnitView(worker), fixture.view.getTile(destination))

        invokeCallback(overlay, "connectRoadToTargetTile", fixture.holder, worker, destination.position)

        fixture.replay(turnStart, UnitActionType.Automate.name)
        val replayed = turnStart.tileMap[origin.position].civilianUnit!!
        assertEquals(destination.position, replayed.automatedRoadConnectionDestination)
        assertEquals(Constants.connectRoadAction, replayed.action)
        assertTrue(replayed.automated)
    }

    @Test
    fun attackButtonRecordsMovementWhenTheTargetHasDisappeared() {
        val fixture = Fixture()
        val unit = fixture.game.addUnit("Warrior", fixture.civ, fixture.game.getTile(0, 0))
        val destination = fixture.game.getTile(1, 0)
        val preparation = AttackableTile(destination, fixture.game.getTile(2, 0), 1f, null)
        val target = AttackableTileView(preparation, fixture.civ, gameView = fixture.view)
        val attacker = fixture.view.getMapUnitView(unit).asCombatant()
        val table = mock(BattleTable::class.java)
        setField(table, "worldScreen", fixture.screen)
        val turnStart = fixture.turnStart()

        invokeCallback(table, "onAttackButtonClicked", attacker, attacker, target)

        assertEquals(destination.position, unit.currentTile.position)
        fixture.replay(turnStart, SimultaneousTurnStateAction.Combat.name)
        assertEquals(unit.id, turnStart.tileMap[destination.position].militaryUnit?.id)
    }

    private class Fixture {
        val game = TestGame().apply { makeHexagonalMap(4) }
        val civ = game.addCiv(game.ruleset.nations["Babylon"]!!, isPlayer = true)
        val view = GameView(game.gameInfo, civ)
        private val operations = ArrayList<SimultaneousTurnOperation>()
        private val unitTable = mock(UnitTable::class.java)
        private val undoHandler = mock(UndoHandler::class.java)
        private val client = mock(UncivGame::class.java)
        val holder = mock(WorldMapHolder::class.java) { invocation ->
            if (invocation.method.name.startsWith("getWorldScreen")) screen
            else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        val screen: WorldScreen = mock(WorldScreen::class.java) { invocation ->
            val name = invocation.method.name
            when {
                name == "getCanChangeState" -> true
                name == "getGameInfo" -> game.gameInfo
                name == "getGame" -> client
                name == "getMapHolder" -> holder
                name.startsWith("getUndoHandler") -> undoHandler
                name.startsWith("getBottomUnitTable") -> unitTable
                name == "runAndRecordSimultaneousGameStateChange" || name == "beginSimultaneousGameStateSnapshot" ->
                    invocation.callRealMethod()
                name == "recordSimultaneousGameStateChange" -> {
                    val before = invocation.getArgument<GameInfo>(1)
                    val action = invocation.getArgument<Any>(0)
                    val result = when (action) {
                        is UnitActionType -> SimultaneousTurnOperations.captureGameStateChange(action, before, game.gameInfo)
                        is SimultaneousTurnStateAction -> SimultaneousTurnOperations.captureGameStateChange(action, before, game.gameInfo)
                        else -> error("Unexpected action $action")
                    }
                    if (result != null) operations += SimultaneousTurnOperation(
                        game.gameInfo.turns, civ.playerId, operations.size.toLong(),
                        SimultaneousTurnOperations.stateOperationType(action.name), json().toJson(result)
                    )
                    null
                }
                else -> Answers.RETURNS_DEFAULTS.answer(invocation)
            }
        }

        init {
            civ.playerId = "player-a"
            game.gameInfo.gameParameters.isOnlineMultiplayer = true
            game.gameInfo.gameParameters.simultaneousTurns = true
            game.gameInfo.setTransients()
            setField(screen, "gameInfo", game.gameInfo)
        }

        fun turnStart() = game.gameInfo.clone().apply { setTransients() }

        fun replay(turnStart: GameInfo, action: String) {
            assertEquals(SimultaneousTurnOperations.stateOperationType(action), operations.single().type)
            val failures = SimultaneousTurnReplay.replayOrExplain(turnStart, operations)
            assertTrue("The $action operation should replay: ${failures.map { it.reason }}", failures.isEmpty())
        }
    }

    companion object {
        private fun setField(receiver: Any, name: String, value: Any) {
            receiver.javaClass.getDeclaredField(name).apply {
                isAccessible = true
                set(receiver, value)
            }
        }

        private fun invokeCallback(receiver: Any, name: String, vararg args: Any) {
            receiver.javaClass.declaredMethods.single { it.name == name }.apply {
                isAccessible = true
                invoke(receiver, *args)
            }
        }
    }
}
