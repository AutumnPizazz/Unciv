package com.unciv.ui.screens.worldscreen.worldmap

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.multiplayer.SimultaneousTurnOperation
import com.unciv.logic.multiplayer.SimultaneousTurnOperations
import com.unciv.logic.multiplayer.SimultaneousTurnReplay
import com.unciv.logic.multiplayer.SimultaneousTurnStateAction
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import com.unciv.ui.components.MapArrowType
import com.unciv.ui.components.MiscArrowTypes
import com.unciv.ui.components.UnitMovementMemoryType
import com.unciv.ui.screens.worldscreen.WorldScreen
import com.unciv.ui.screens.worldscreen.unit.UnitTable
import com.unciv.view.GameView
import com.unciv.view.MapUnitView
import com.unciv.view.TileView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mockito.doCallRealMethod
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@RunWith(BaseTestRunner::class)
class WorldMapMovementOverlayTest {
    private lateinit var game: TestGame
    private lateinit var selectedCiv: Civilization
    private lateinit var enemy: Civilization
    private lateinit var restrictedView: GameView
    private lateinit var spectatorView: GameView

    private data class Arrow(val from: HexCoord, val to: HexCoord, val type: MapArrowType)

    @Before
    fun setUp() {
        game = TestGame()
        game.makeHexagonalMap(4)
        selectedCiv = game.addCiv(isPlayer = true)
        enemy = game.addCiv()
        val spectator = game.addCiv(game.ruleset.nations[Constants.spectator]!!, isPlayer = true)
        spectator.viewableTiles = game.tileMap.values.toSet()
        restrictedView = GameView(game.gameInfo, selectedCiv, spectatorMode = true)
        spectatorView = GameView(game.gameInfo, spectator, spectatorMode = true)
    }

    @Test
    fun foreignHistoryIsHiddenOnExploredTilesOutsideCurrentVision() {
        val unit = unitWithHistory(enemy, HexCoord(-1, 0), HexCoord.Zero)
        unit.attacksSinceTurnStart.add(HexCoord(1, 0))
        for (tile in game.tileMap.values) tile.setExplored(selectedCiv, true)
        selectedCiv.viewableTiles = emptySet()

        assertTrue(drawOverlay(restrictedView).isEmpty())

        selectedCiv.viewableTiles = game.tileMap.values.toSet()
        assertEquals(setOf(
            Arrow(HexCoord(-1, 0), HexCoord.Zero, UnitMovementMemoryType.UnitMoved),
            Arrow(HexCoord.Zero, HexCoord(1, 0), MiscArrowTypes.UnitHasAttacked)
        ), drawOverlay(restrictedView).toSet())
    }

    @Test
    fun spectatorArrowsIncludeEndpointsUnexploredBySelectedCivilization() {
        val unit = unitWithHistory(enemy, HexCoord(-1, 0), HexCoord.Zero)
        unit.attacksSinceTurnStart.add(HexCoord(1, 0))
        enemy.attacksSinceTurnStart.add(Civilization.HistoricalAttackMemory(null, HexCoord(2, 0), HexCoord(2, 1)))
        assertFalse(game.getTile(HexCoord.Zero).isExplored(selectedCiv))
        assertTrue(drawOverlay(restrictedView).isEmpty())

        assertEquals(setOf(
            Arrow(HexCoord(-1, 0), HexCoord.Zero, UnitMovementMemoryType.UnitMoved),
            Arrow(HexCoord.Zero, HexCoord(1, 0), MiscArrowTypes.UnitHasAttacked),
            Arrow(HexCoord(2, 0), HexCoord(2, 1), MiscArrowTypes.UnitHasAttacked)
        ), drawOverlay(spectatorView).toSet())
    }

    @Test
    fun selectedUnitStillFiltersHistoryWhenMapPerspectiveChanges() {
        val selectedUnit = unitWithHistory(selectedCiv, HexCoord(-1, 0), HexCoord.Zero)
        selectedUnit.attacksSinceTurnStart.add(HexCoord(0, 1))
        val otherUnit = unitWithHistory(enemy, HexCoord(2, 0), HexCoord(3, 0))
        otherUnit.attacksSinceTurnStart.add(HexCoord(3, 1))
        otherUnit.attacksSinceTurnStart.add(HexCoord.Zero)

        assertEquals(setOf(
            Arrow(HexCoord(-1, 0), HexCoord.Zero, UnitMovementMemoryType.UnitMoved),
            Arrow(HexCoord.Zero, HexCoord(0, 1), MiscArrowTypes.UnitHasAttacked),
            Arrow(HexCoord(3, 0), HexCoord.Zero, MiscArrowTypes.UnitHasAttacked)
        ), drawOverlay(spectatorView, restrictedView.getMapUnitView(selectedUnit)).toSet())
    }

    @Test
    fun spectatorPerspectiveKeepsPlannedRoutesForSelectedCivilization() {
        val selectedUnit = game.addUnit("Warrior", selectedCiv, game.getTile(0, 0))
        selectedUnit.action = "moveTo 1,0"
        val otherOwnUnit = game.addUnit("Warrior", selectedCiv, game.getTile(-3, 0))
        otherOwnUnit.action = "moveTo -2,0"
        val enemyUnit = game.addUnit("Warrior", enemy, game.getTile(3, 0))
        enemyUnit.action = "moveTo 3,1"

        assertEquals(setOf(
            Arrow(HexCoord.Zero, HexCoord(1, 0), MiscArrowTypes.UnitMoving),
            Arrow(HexCoord(-3, 0), HexCoord(-2, 0), MiscArrowTypes.UnitMoving)
        ), drawOverlay(spectatorView, restrictedView.getMapUnitView(selectedUnit)).toSet())
    }

    @Test
    fun singleTapMoveRecordsUnitMovementInSimultaneousTurns() {
        val game = TestGame()
        game.makeHexagonalMap(4)
        val civ = game.addCiv(game.ruleset.nations["Babylon"]!!, isPlayer = true)
        val view = GameView(game.gameInfo, civ)
        game.gameInfo.gameParameters.isOnlineMultiplayer = true
        game.gameInfo.gameParameters.simultaneousTurns = true
        civ.playerId = "player-a"
        UncivGame.Current.settings.singleTapMove = true
        game.gameInfo.setTransients()
        val origin = game.getTile(0, 0)
        val unit = game.addUnit("Warrior", civ, origin)
        val target = view.getTile(game.getTile(1, 0))
        val turnStart = game.gameInfo.clone()
        turnStart.setTransients()
        val unitView = view.getMapUnitView(unit)
        val unitTable = mock(UnitTable::class.java)
        `when`(unitTable.selectedUnits).thenReturn(listOf(unitView))
        val operations = ArrayList<SimultaneousTurnOperation>()
        val screen = mock(WorldScreen::class.java) { invocation ->
            when (invocation.method.name) {
                "isPlayersTurn", "getCanChangeState" -> true
                "runIfSimultaneousTurnTargetsAreFree", "runAndRecordSimultaneousGameStateChange",
                    "beginSimultaneousGameStateSnapshot" -> invocation.callRealMethod()
                "recordSimultaneousGameStateChange" -> {
                    val action = invocation.getArgument<SimultaneousTurnStateAction>(0)
                    val before = invocation.getArgument<GameInfo>(1)
                    val result = SimultaneousTurnOperations.captureGameStateChange(action, before, game.gameInfo)
                    if (result != null) operations += SimultaneousTurnOperation(
                        game.gameInfo.turns, civ.playerId, 1,
                        SimultaneousTurnOperations.stateOperationType(action.name), json().toJson(result)
                    )
                    null
                }
                else -> if (invocation.method.name.startsWith("getBottomUnitTable")) unitTable
                    else Answers.RETURNS_DEFAULTS.answer(invocation)
            }
        }
        WorldScreen::class.java.getDeclaredField("gameInfo").apply {
            isAccessible = true
            set(screen, game.gameInfo)
        }
        val holder = mock(WorldMapHolder::class.java) { invocation ->
            if (invocation.method.name.startsWith("getWorldScreen")) screen
            else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        for ((name, value) in mapOf(
            "worldScreen" to screen,
            "unitMovementPaths" to HashMap<MapUnitView, ArrayList<TileView>>(),
            "unitConnectRoadPaths" to HashMap<MapUnitView, List<TileView>>()
        )) {
            WorldMapHolder::class.java.getDeclaredField(name).apply {
                isAccessible = true
                set(holder, value)
            }
        }
        doCallRealMethod().`when`(holder).onTileClicked(target)
        val glTasks = LinkedBlockingQueue<Runnable>()
        val originalApp = Gdx.app
        Gdx.app = mock(Application::class.java) { invocation ->
            if (invocation.method.name == "postRunnable") glTasks.add(invocation.getArgument(0))
            Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        try {
            holder.onTileClicked(target)
            val move = glTasks.poll(5, TimeUnit.SECONDS)
            assertNotNull("The map click should schedule movement", move)
            move!!.run()
        } finally {
            Gdx.app = originalApp
        }

        assertEquals(target.position(), unit.getTile().position)
        assertEquals(SimultaneousTurnOperations.stateOperationType(SimultaneousTurnStateAction.Move.name), operations.single().type)
        val failures = SimultaneousTurnReplay.replay(turnStart, operations)
        assertTrue("The recorded move should replay on the turn-start save: ${failures.map { it.type }}", failures.isEmpty())
        assertEquals(unit.id, turnStart.tileMap[target.position()].militaryUnit?.id)
        assertEquals(null, turnStart.tileMap[origin.position].militaryUnit)
    }

    private fun unitWithHistory(civ: Civilization, from: HexCoord, to: HexCoord): MapUnit {
        val unit = game.addUnit("Warrior", civ, game.getTile(to))
        unit.movementMemories = arrayListOf(MapUnit.UnitMovementMemory(from, UnitMovementMemoryType.UnitMoved))
        return unit
    }

    /** Run the real overlay method, intercepting drawing so the test needs no graphics context. */
    private fun drawOverlay(gameView: GameView, selectedUnit: MapUnitView? = null): List<Arrow> {
        val unitTable = mock(UnitTable::class.java)
        `when`(unitTable.selectedUnit).thenReturn(selectedUnit)
        val screen = mock(WorldScreen::class.java) { invocation ->
            if (invocation.method.name.startsWith("getBottomUnitTable")) unitTable
            else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        `when`(screen.selectedGameView).thenReturn(restrictedView)
        val arrows = ArrayList<Arrow>()
        val holder = mock(WorldMapHolder::class.java) { invocation ->
            if (invocation.method.name == "addArrow") {
                val from = invocation.getArgument<TileView>(0).position()
                val to = invocation.getArgument<TileView>(1).position()
                // Zero-length segments recorded when a unit has not moved have no visible arrow.
                if (from != to) arrows.add(Arrow(from, to, invocation.getArgument(2)))
                null
            } else Answers.RETURNS_DEFAULTS.answer(invocation)
        }
        WorldMapHolder::class.java.getDeclaredField("worldScreen").apply {
            isAccessible = true
            set(holder, screen)
        }
        val plannedUnits = restrictedView.civView.getUnits().asSequence()
        val updateMovementOverlay = WorldMapHolder::class.java.methods.single {
            it.name.startsWith("updateMovementOverlay") && it.parameterCount == 2
        }
        updateMovementOverlay.invoke(doCallRealMethod().`when`(holder), gameView, plannedUnits)
        updateMovementOverlay.invoke(holder, gameView, plannedUnits)
        return arrows
    }
}
