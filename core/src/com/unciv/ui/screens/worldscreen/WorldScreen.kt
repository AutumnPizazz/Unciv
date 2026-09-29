package com.unciv.ui.screens.worldscreen

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.logic.GameInfo
import com.unciv.view.GameView
import com.unciv.logic.UncivShowableException
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.civilization.diplomacy.DiplomaticStatus
import com.unciv.logic.event.EventBus
import com.unciv.logic.map.HexCoord
import com.unciv.json.json
import com.unciv.logic.multiplayer.MultiplayerGameUpdated
import com.unciv.logic.multiplayer.OnlineStatusUpdated
import com.unciv.logic.multiplayer.RestartVoteStatus
import com.unciv.logic.multiplayer.RestartVoteUpdated
import com.unciv.logic.multiplayer.SimultaneousTurnGameStateResult
import com.unciv.logic.multiplayer.SimultaneousTurnOperation
import com.unciv.logic.multiplayer.SimultaneousTurnOperationReceived
import com.unciv.logic.multiplayer.SimultaneousTurnOperations
import com.unciv.logic.multiplayer.SimultaneousTurnReplay
import com.unciv.logic.multiplayer.SimultaneousTurnReservations
import com.unciv.logic.multiplayer.SimultaneousTurnStateAction
import com.unciv.logic.multiplayer.chat.ChatWebSocket
import com.unciv.logic.multiplayer.storage.MultiplayerFileNotFoundException
import com.unciv.logic.multiplayer.storage.FileStorageRateLimitReached
import com.unciv.logic.multiplayer.storage.MultiplayerAuthException
import com.unciv.logic.trade.TradeEvaluation
import com.unciv.models.TutorialTrigger
import com.unciv.models.UnitActionType
import com.unciv.models.metadata.GameSetupInfo
import com.unciv.models.ruleset.Event
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.translations.tr
import com.unciv.ui.components.extensions.centerX
import com.unciv.ui.components.extensions.darken
import com.unciv.ui.components.input.KeyShortcutDispatcherVeto
import com.unciv.ui.components.input.KeyboardBinding
import com.unciv.ui.components.input.KeyboardPanningListener
import com.unciv.ui.components.input.onClick
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popups.AuthPopup
import com.unciv.ui.popups.Popup
import com.unciv.ui.popups.ToastPopup
import com.unciv.ui.popups.hasOpenPopups
import com.unciv.ui.popups.options.OptionsPopupPages
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.screens.cityscreen.CityScreen
import com.unciv.ui.screens.devconsole.DevConsolePopup
import com.unciv.ui.screens.mainmenuscreen.MainMenuScreen
import com.unciv.ui.screens.newgamescreen.NewGameScreen
import com.unciv.ui.screens.overviewscreen.EmpireOverviewCategories
import com.unciv.ui.screens.overviewscreen.EmpireOverviewScreen
import com.unciv.ui.screens.pickerscreens.DiplomaticVoteResultScreen
import com.unciv.ui.screens.pickerscreens.GreatPersonPickerScreen
import com.unciv.ui.screens.savescreens.LoadGameScreen
import com.unciv.ui.screens.savescreens.QuickSave
import com.unciv.ui.screens.savescreens.SaveGameScreen
import com.unciv.ui.screens.victoryscreen.VictoryScreen
import com.unciv.ui.screens.worldscreen.bottombar.BattleTable
import com.unciv.ui.screens.worldscreen.bottombar.TileInfoTable
import com.unciv.ui.screens.worldscreen.chat.ChatButton
import com.unciv.ui.screens.worldscreen.chat.OnlineStatusButton
import com.unciv.ui.screens.worldscreen.mainmenu.WorldScreenMusicPopup
import com.unciv.ui.screens.worldscreen.minimap.MinimapHolder
import com.unciv.ui.screens.worldscreen.status.AutoPlayStatusButton
import com.unciv.ui.screens.worldscreen.status.MultiplayerStatusButton
import com.unciv.ui.screens.worldscreen.status.NextTurnButton
import com.unciv.ui.screens.worldscreen.status.NextTurnProgress
import com.unciv.ui.screens.worldscreen.status.SmallUnitButton
import com.unciv.ui.screens.worldscreen.status.StatusButtons
import com.unciv.ui.screens.worldscreen.topbar.WorldScreenTopBar
import com.unciv.ui.screens.worldscreen.unit.AutoPlay
import com.unciv.ui.screens.worldscreen.unit.UnitTable
import com.unciv.ui.screens.worldscreen.unit.actions.UnitActionsTable
import com.unciv.ui.screens.worldscreen.worldmap.WorldMapHolder
import com.unciv.ui.screens.worldscreen.worldmap.WorldMapTileUpdater.updateTiles
import com.unciv.utils.Concurrency
import com.unciv.utils.Log
import com.unciv.utils.debug
import com.unciv.utils.launchOnGLThread
import com.unciv.utils.launchOnThreadPool
import com.unciv.utils.withGLContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import yairm210.purity.annotations.Readonly
import java.util.Timer
import kotlin.concurrent.timer

/**
 * Do not create this screen without seriously thinking about the implications: this is the single most memory-intensive class in the application.
 * There really should ever be only one in memory at the same time, likely managed by [UncivGame].
 *
 * @param gameInfo The game state the screen should represent
 * @param viewingCiv The currently active [civilization][Civilization]
 * @param restoreState
 */
class WorldScreen(
    val gameInfo: GameInfo,
    val autoPlay: AutoPlay,
    private val viewingCiv: Civilization,
    restoreState: RestoreState? = null
) : BaseScreen() {
    /** When set, causes the screen to update in the next render event. */
    var shouldUpdate = false

    @Transient
    private val simultaneousTurnOperations = ArrayList<SimultaneousTurnOperation>()

    /** The state this screen started the turn from, used to detect local changes that no recorded
     *  operation covers. Only kept for simultaneous-turn games, where settlement replays operations
     *  on the turn-start save instead of trusting the client's final state. */
    private val simultaneousTurnStartSnapshot: GameInfo? =
        if (gameInfo.isSimultaneousTurnsMode()) {
            // The archive a client reloads mid-turn carries the counter from [GameInfo.nextTurnPolling],
            // which resets it to 0. Operations this player already uploaded in this turn were numbered
            // from that same value, and the server's (turn, playerId, sequence) dedup keeps the first
            // one it saw - so restarting at 0 would silently discard every new operation. Jump the
            // counter past anything earlier sessions can have used before recording this session.
            gameInfo.nextSimultaneousOperationSequence = maxOf(
                gameInfo.nextSimultaneousOperationSequence,
                System.currentTimeMillis()
            )
            gameInfo.clone()
        } else null


    /** Indicates it's the player's ([viewingCiv]) turn */
    var isPlayersTurn = viewingCiv.isCurrentPlayer() || gameInfo.isSimultaneousTurnsMode()
        internal set

    /** Records a completed local operation for the future simultaneous-turn settlement pass. */
    fun recordSimultaneousTurnOperation(type: String, payload: Any) {
        if (!gameInfo.isSimultaneousTurnsMode()) return
        val playerId = viewingCiv.playerId
        if (playerId.isEmpty()) return
        val operation = SimultaneousTurnOperation(
            turn = gameInfo.turns,
            playerId = playerId,
            sequence = gameInfo.nextSimultaneousOperationSequence++,
            type = type,
            payload = json().toJson(payload)
        )
        simultaneousTurnOperations.add(operation)
        ChatWebSocket.sendOperationSignal(gameInfo.gameId, operation.turn, playerId, operation.sequence)
    }

    /** Clones the game state before an operation that will be recorded as a full state diff.
     *  Split out from [runAndRecordSimultaneousGameStateChange] so callers that must run the
     *  operation on a background thread can still clone on the thread they choose.
     *  Returns null when the operation does not need a snapshot (or this is not a simultaneous game). */
    fun beginSimultaneousGameStateSnapshot(type: UnitActionType): GameInfo? =
        if (gameInfo.isSimultaneousTurnsMode()
            && SimultaneousTurnOperations.requiresGameStateSnapshot(type)
        ) gameInfo.clone() else null

    /** A state change that is not a unit action is only recorded by this snapshot, so it always needs one. */
    fun beginSimultaneousGameStateSnapshot(action: SimultaneousTurnStateAction): GameInfo? =
        if (gameInfo.isSimultaneousTurnsMode()) gameInfo.clone() else null

    fun runAndRecordSimultaneousGameStateChange(type: UnitActionType, action: () -> Unit) {
        val before = beginSimultaneousGameStateSnapshot(type)
        action()
        recordSimultaneousGameStateChange(type, before)
    }

    fun runAndRecordSimultaneousGameStateChange(action: SimultaneousTurnStateAction, block: () -> Unit) {
        val before = beginSimultaneousGameStateSnapshot(action)
        block()
        recordSimultaneousGameStateChange(action, before)
    }

    fun recordSimultaneousGameStateChange(type: UnitActionType, before: GameInfo?) {
        if (before == null || !gameInfo.isSimultaneousTurnsMode()) return
        recordStateChangeOperations(
            type.name, SimultaneousTurnOperations.captureGameStateChange(type, before, gameInfo)
        )
    }

    fun recordSimultaneousGameStateChange(action: SimultaneousTurnStateAction, before: GameInfo?) {
        if (before == null || !gameInfo.isSimultaneousTurnsMode()) return
        recordStateChangeOperations(
            action.name, SimultaneousTurnOperations.captureGameStateChange(action, before, gameInfo)
        )
    }

    private fun recordStateChangeOperations(actionName: String, result: SimultaneousTurnGameStateResult?) {
        if (result == null) return
        // Record one operation per component: the replay rejects a state operation as a whole when any
        // of its civs/tiles/religions no longer matches, so a conflict on a single component would
        // otherwise discard all the unrelated changes captured in the same diff.
        for (part in SimultaneousTurnOperations.splitGameStateResult(result))
            recordSimultaneousTurnOperation(SimultaneousTurnOperations.stateOperationType(actionName), part)
    }

    /**
     * Sentinel for simultaneous turns: the settlement host rebuilds the turn by replaying recorded
     * operations on the turn-start save, so a state change made by a code path that did not record an
     * operation would be lost. Diff the state our recorded operations reproduce against our actual
     * state. A remaining difference means a missing recording call - a bug - so report it instead of
     * patching it with a catch-all operation, which is what used to happen and made both the desync and
     * the code path that caused it invisible. Must run before the upload.
     */
    private fun CoroutineScope.checkForUnrecordedSimultaneousTurnChanges() {
        val turnStart = simultaneousTurnStartSnapshot ?: return
        if (!gameInfo.isSimultaneousTurnsMode() || turnStart.turns != gameInfo.turns) return
        val result = SimultaneousTurnOperations.diffUnrecordedState(
            turnStart, gameInfo, getSimultaneousTurnOperations()
        ) ?: return
        Log.error(
            "Simultaneous turn %d of game %s changed state without recording an operation: %s",
            gameInfo.turns, gameInfo.gameId,
            SimultaneousTurnOperations.describeGameStateResult(result)
        )
        launchOnGLThread {
            ToastPopup(
                "Some changes you made this turn could not be recorded and may be lost. Please report this.".tr(),
                this@WorldScreen, 5000
            )
        }
    }

    /** Returns a stable snapshot for a future settlement worker. */
    fun getSimultaneousTurnOperations(): List<SimultaneousTurnOperation> =
        SimultaneousTurnOperations.merge(emptyList(), simultaneousTurnOperations)
     // only this class is allowed to make changes

    /** Indicates that a game failed to upload, and needs to be uploaded */
    var failedUpload = false
        private set

    /** Defers reopening an unfulfilled free-Great-Person choice until the player requests it. */
    internal var deferFreeGreatPersonPicker = false

    /** Selected civilization, used in spectator and replay mode, equals viewingCiv in ordinary games */
    var selectedCiv = viewingCiv
        internal set
    /** The [selectedCiv]'s perspective. For non-spectators this this equals the viewingGameView.
     *  Only ever differs from [spectatorGameView] for spectators. */
    var selectedGameView = GameView(gameInfo, selectedCiv, spectatorMode = viewingCiv.isSpectator())
        internal set

    /** The [viewingCiv]'s own perspective.
     * This should only ever be used A. for spectators B. when forOfWar is false, for specific UI elements */
    private val spectatorGameView by lazy {
        if (!viewingCiv.isSpectator()) throw Exception("Managed to disable fog of war without being spectator?!")
        GameView(gameInfo, viewingCiv, spectatorMode = true)
    }  
    fun getGameViewConsideringForOfWar() = if (fogOfWar) selectedGameView else spectatorGameView    

    /** UI toggle only (spectator mode): whether the map/tile panel should show [selectedGameView]'s
     *  (fogged) vision or [spectatorGameView]'s (the spectator's own, unrestricted) vision. */
    var fogOfWar = true

    /** `true` when it's the player's turn unless he is a spectator */
    val canChangeState
        get() = isPlayersTurn && !viewingCiv.isSpectator()

    val mapHolder = WorldMapHolder(this, gameInfo.tileMap)

    internal var waitingForAutosave = false

    // Floating Widgets going counter-clockwise
    internal val topBar = WorldScreenTopBar(this)
    internal val techPolicyAndDiplomacy = TechPolicyDiplomacyButtons(this)
    internal val chatButton = ChatButton(this)
    internal val onlineStatusButton = OnlineStatusButton(this)
    private val unitActionsTable = UnitActionsTable(this)
    /** Bottom left widget holding information about a selected unit or city */
    internal val bottomUnitTable = UnitTable(this)
    private val battleTable = BattleTable(this)
    private val zoomController = ZoomButtonPair(mapHolder)
    internal val minimapWrapper = MinimapHolder(mapHolder)
    private val bottomTileInfoTable = TileInfoTable(this)
    internal val notificationsScroll = NotificationsScroll(this)
    internal val nextTurnButton = NextTurnButton(this)
    private val statusButtons = StatusButtons(nextTurnButton)
    internal val smallUnitButton = SmallUnitButton(this, statusButtons)
    private val tutorialTaskTable = Table().apply {
        background = skinStrings.getUiBackground("WorldScreen/TutorialTaskTable", tintColor = skinStrings.skinConfig.baseColor.darken(0.5f))
    }
    private var tutorialTaskTableHash = 0

    private var nextTurnUpdateJob: Job? = null
    @Transient
    private var simultaneousTurnSettlementRetryPending = false
    /** Turn whose "done" marker the server already accepted, so retries don't re-upload it forever. */
    @Transient
    private var simultaneousTurnDoneUploadedForTurn = -1
    /** Turn this client started waiting for the other players on, and when it started. */
    @Transient
    private var simultaneousTurnWaitStartedForTurn = -1
    @Transient
    private var simultaneousTurnWaitStartedAt = 0L
    /** Turn whose "we continued without someone" message was already shown, so it is not repeated. */
    @Transient
    private var simultaneousTurnTimeoutReportedForTurn = -1
    /** Watches the sidecar operation list so a missed WebSocket signal cannot strand a player. */
    private var simultaneousTurnWatcherJob: Job? = null
    /** Set once the server answers 404 for reservations, so older servers are not asked on every action. */
    private var simultaneousTurnReservationsUnsupported = false
    /**
     * Turn and reason of the last submission/settlement failure shown to the player. The pass retries
     * every few seconds, so the same failure must not produce a new ToastPopup each time.
     */
    @Transient
    private var simultaneousTurnFailureReportedForTurn = -1
    @Transient
    private var simultaneousTurnFailureReported: String? = null
    /** Watches the targets other players claimed for this turn, so the claims can be shown on the map. */
    private var simultaneousTurnReservationWatcherJob: Job? = null
    /** Tiles other players claimed this turn, and the turn they were read for. */
    @Transient
    private var simultaneousTurnReservedTilesOnMap: Set<HexCoord> = emptySet()
    @Transient
    private var simultaneousTurnReservedTilesTurn = -1

    /**
     * Tiles another player claimed for the turn being played, for the map marks that make a claim
     * visible before an order is refused. Empty on servers that cannot arbitrate reservations, and
     * stale claims are dropped as soon as the turn advances - the authority only keeps the
     * reservations of the turn they were made in.
     */
    val simultaneousTurnReservedTiles: Set<HexCoord>
        get() = if (simultaneousTurnReservedTilesTurn == gameInfo.turns) simultaneousTurnReservedTilesOnMap else emptySet()

    /** Countdown timer for polling multiplayer mode. */
    private var pollingTimerJob: Job? = null
    /** Seconds remaining in the current polling window. Updated by the timer coroutine. */
    var pollingSecondsRemaining: Int = 0

    /** Map of civilization name -> last response timestamp (millis) for online status tracking. */
    val playerOnlineTimes = mutableMapOf<String, Long>()
    private val onlineTimeoutMs = 30_000L

    private val events = EventBus.EventReceiver()

    private var uiEnabled = true

    internal val undoHandler = UndoHandler(this)

    /** Local snapshot scheduling for the "forbid reload" multiplayer option. */
    internal val localSnapshotHandler = LocalSnapshotHandler(this)


    init {
        // notifications are right-aligned, they take up only as much space as necessary.
        notificationsScroll.width = stage.width / 2

        minimapWrapper.x = stage.width - minimapWrapper.width

        // This is the most memory-intensive operation we have currently, most OutOfMemory errors will occur here
        mapHolder.addTiles()
        mapHolder.reloadMaxZoom()

        // resume music (in case choices from the menu lead to instantiation of a new WorldScreen)
        UncivGame.Current.musicController.resume()

        stage.addActor(mapHolder)
        stage.scrollFocus = mapHolder
        stage.addActor(notificationsScroll)  // very low in z-order, so we're free to let it extend _below_ tile info and minimap if we want
        stage.addActor(tutorialTaskTable)    // behind topBar!
        stage.addActor(topBar)
        stage.addActor(statusButtons)
        stage.addActor(techPolicyAndDiplomacy)
        stage.addActor(chatButton)
        stage.addActor(onlineStatusButton)

        stage.addActor(zoomController)
        zoomController.isVisible = UncivGame.Current.settings.showZoomButtons

        stage.addActor(bottomUnitTable)
        stage.addActor(unitActionsTable)
        stage.addActor(bottomTileInfoTable)
        stage.addActor(minimapWrapper)
        battleTable.width = stage.width / 3
        battleTable.x = stage.width / 3
        stage.addActor(battleTable)

        val tileToCenterOn: HexCoord =
                when {
                    viewingCiv.getCapital() != null -> viewingCiv.getCapital()!!.location.toHexCoord()
                    viewingCiv.units.getCivUnits().any() -> viewingCiv.units.getCivUnits().first().getTile().position
                    else -> HexCoord.Zero
                }

        mapHolder.isAutoScrollEnabled = Gdx.app.type == Application.ApplicationType.Desktop && game.settings.mapAutoScroll
        mapHolder.mapPanningSpeed = game.settings.mapPanningSpeed

        // Don't select unit and change selectedCiv when centering as spectator
        mapHolder.setCenterPosition(tileToCenterOn, immediately = true, selectUnit = !viewingCiv.isSpectator())

        tutorialController.allTutorialsShowedCallback = { shouldUpdate = true }

        addKeyboardListener() // for map panning by W,S,A,D
        addKeyboardPresses()  // shortcut keys like F1


        if (gameInfo.gameParameters.isOnlineMultiplayer && !gameInfo.isUpToDate)
            isPlayersTurn = false // until we're up to date, don't let the player do anything

        if (gameInfo.gameParameters.isOnlineMultiplayer) {
            val gameId = gameInfo.gameId
            events.receive(MultiplayerGameUpdated::class, { it.preview.gameId == gameId }) {
                if (isNextTurnUpdateRunning()
                    || game.onlineMultiplayer.hasLatestGameState(gameInfo, it.preview)
                ) return@receive
                if (gameInfo.isSimultaneousTurnsMode()) {
                    // Mid-turn previews only differ in transient fields; reloading on those would
                    // discard the operations the local player already recorded. Only adopt the
                    // server state once it has actually advanced to a later turn.
                    if (it.preview.turns <= gameInfo.turns) return@receive
                    Concurrency.run("Load latest simultaneous-turn state") {
                        if (isNextTurnUpdateRunning() || game.gameInfo != gameInfo) return@run
                        val latestGame = try {
                            game.onlineMultiplayer.multiplayerServer.downloadGame(gameId)
                        } catch (_: Exception) {
                            return@run
                        }
                        if (latestGame.turns <= gameInfo.turns) return@run
                        launchOnGLThread { startNewScreenJob(latestGame, autoPlay) }
                    }
                    return@receive
                }
                Concurrency.run("Load latest multiplayer state") {
                    loadLatestMultiplayerState()
                    sendOnlineQuery()
                }
            }
            events.receive(SimultaneousTurnOperationReceived::class, { it.gameId == gameId }) { signal ->
                if (!gameInfo.isSimultaneousTurnsMode() || signal.turn != gameInfo.turns) return@receive
                retrySimultaneousTurnAfterSignal()
            }
            events.receive(OnlineStatusUpdated::class, { it.gameId == gameId }) { update ->
                playerOnlineTimes[update.civName] = System.currentTimeMillis()
                shouldUpdate = true
            }
            events.receive(RestartVoteUpdated::class, { it.gameId == gameId }) {
                onRestartVoteUpdated()
            }
        }

        if (gameInfo.isPollingMode() || gameInfo.isSimultaneousTurnsMode()) {
            ChatWebSocket.start()  // ensure push notifications for game updates
            if (gameInfo.isPollingMode() && isPlayersTurn)
                startPollingTimer()
            if (gameInfo.isSimultaneousTurnsMode()) {
                startSimultaneousTurnWatcher()
                startSimultaneousTurnReservationWatcher()
                restoreSimultaneousTurnOperations()
            }

            playerOnlineTimes[viewingCiv.civName] = System.currentTimeMillis()
        }

        if (restoreState != null) restore(restoreState)

        // don't run update() directly, because the UncivGame.worldScreen should be set so that the city buttons and tile groups
        //  know what the viewing civ is.
        shouldUpdate = true
    }

    override fun dispose() {
        resizeDeferTimer?.cancel()
        stopPollingTimer()
        simultaneousTurnWatcherJob?.cancel()
        simultaneousTurnWatcherJob = null
        simultaneousTurnReservationWatcherJob?.cancel()
        simultaneousTurnReservationWatcherJob = null
        events.stopReceiving()
        statusButtons.dispose()
        super.dispose()
    }

    override fun getCivilopediaRuleset() = gameInfo.ruleset

    // Handle disabling and re-enabling WASD listener while Options are open
    override fun openOptionsPopup(startingPage: OptionsPopupPages, withDebug: Boolean, onClose: () -> Unit) {
        val oldListener = stage.root.listeners.filterIsInstance<KeyboardPanningListener>().firstOrNull()
        if (oldListener != null) {
            stage.removeListener(oldListener)
            oldListener.dispose()
        }
        super.openOptionsPopup(startingPage, withDebug) {
            addKeyboardListener()
            onClose()
        }
    }

    fun openEmpireOverview(category: EmpireOverviewCategories? = null, selection: String = "") {
        game.pushScreen{ EmpireOverviewScreen(selectedGameView.civView, category, selection) }
    }

    fun openNewGameScreen() {
        val newGameSetupInfo = GameSetupInfo(gameInfo)
        newGameSetupInfo.mapParameters.reseed()
        val newGameScreen = NewGameScreen(newGameSetupInfo)
        game.pushScreen{ newGameScreen }
    }

    fun openSaveGameScreen() {
        // See #10353 - we don't support locally saving an online multiplayer game
        if (gameInfo.gameParameters.isOnlineMultiplayer) return
        game.pushScreen{ SaveGameScreen(gameInfo) }
    }

    private fun addKeyboardPresses() {
        globalShortcuts.add(KeyboardBinding.DeselectOrQuit) { backButtonAndESCHandler() }

        // Space and N are assigned in NextTurnButton constructor
        // Functions that have a big button are assigned there (WorldScreenTopBar, TechPolicyDiplomacyButtons..)
        globalShortcuts.add(KeyboardBinding.Civilopedia) { openCivilopedia() }
        globalShortcuts.add(KeyboardBinding.EmpireOverviewTrades) { openEmpireOverview(EmpireOverviewCategories.Trades) }
        globalShortcuts.add(KeyboardBinding.EmpireOverviewUnits) { openEmpireOverview(EmpireOverviewCategories.Units) }
        globalShortcuts.add(KeyboardBinding.EmpireOverviewPolitics) { openEmpireOverview(EmpireOverviewCategories.Politics) }
        globalShortcuts.add(KeyboardBinding.EmpireOverviewNotifications) { openEmpireOverview(EmpireOverviewCategories.Notifications) }
        globalShortcuts.add(KeyboardBinding.VictoryScreen) { game.pushScreen{ VictoryScreen(this) } }
        globalShortcuts.add(KeyboardBinding.EmpireOverviewStats) { openEmpireOverview(EmpireOverviewCategories.Stats) }
        globalShortcuts.add(KeyboardBinding.EmpireOverviewResources) { openEmpireOverview(EmpireOverviewCategories.Resources) }
        globalShortcuts.add(KeyboardBinding.QuickSave) { QuickSave.save(gameInfo, this) }
        globalShortcuts.add(KeyboardBinding.QuickLoad) { QuickSave.load(this) }
        globalShortcuts.add(KeyboardBinding.ViewCapitalCity) {
            val capital = gameInfo.getCurrentPlayerCivilization().getCapital()
            if (capital != null && !mapHolder.setCenterPosition(capital.location.toHexCoord()))
                game.pushScreen{ CityScreen(selectedGameView.getCityView(capital)) }
        }
        globalShortcuts.add(KeyboardBinding.Options) { // Game Options
            openOptionsPopup { nextTurnButton.update() }
        }
        globalShortcuts.add(KeyboardBinding.SaveGame) { openSaveGameScreen() }    //   Save
        globalShortcuts.add(KeyboardBinding.LoadGame) { game.pushScreen{ LoadGameScreen() } }    //   Load
        globalShortcuts.add(KeyboardBinding.QuitGame) { game.popScreen() }    //   WorldScreen is the last screen, so this quits
        globalShortcuts.add(KeyboardBinding.NewGame) { openNewGameScreen() }
        globalShortcuts.add(KeyboardBinding.MusicPlayer) {
            WorldScreenMusicPopup(this).open(force = true)
        }
        globalShortcuts.add(Input.Keys.NUMPAD_ADD) { this.mapHolder.zoomIn() }    //   '+' Zoom
        globalShortcuts.add(Input.Keys.NUMPAD_SUBTRACT) { this.mapHolder.zoomOut() }    //   '-' Zoom
        globalShortcuts.add(KeyboardBinding.ToggleUI) { toggleUI() }
        globalShortcuts.add(KeyboardBinding.ToggleYieldDisplay) { minimapWrapper.yieldImageButton.toggle() }
        globalShortcuts.add(KeyboardBinding.ToggleWorkedTilesDisplay) { minimapWrapper.populationImageButton.toggle() }
        globalShortcuts.add(KeyboardBinding.ToggleMovementDisplay) { minimapWrapper.movementsImageButton.toggle() }
        globalShortcuts.add(KeyboardBinding.ToggleResourceDisplay) { minimapWrapper.resourceImageButton.toggle() }
        globalShortcuts.add(KeyboardBinding.ToggleImprovementDisplay) { minimapWrapper.improvementsImageButton.toggle() }

        globalShortcuts.add(KeyboardBinding.DeveloperConsole, action = ::openDeveloperConsole)
    }

    @Readonly
    fun openDeveloperConsole() {
        // No cheating unless you're by yourself, ignoring a possible spectator
        if (gameInfo.civilizations.count { it.isHuman() && !it.isSpectator() } > 1) return
        DevConsolePopup(this)
    }

    private fun toggleUI() {
        uiEnabled = !uiEnabled
        topBar.isVisible = uiEnabled
        statusButtons.isVisible = uiEnabled
        techPolicyAndDiplomacy.isVisible = uiEnabled
        tutorialTaskTable.isVisible = uiEnabled
        bottomTileInfoTable.isVisible = uiEnabled
        unitActionsTable.isVisible = uiEnabled
        notificationsScroll.isVisible = uiEnabled
        minimapWrapper.isVisible = uiEnabled
        bottomUnitTable.isVisible = uiEnabled
        if (uiEnabled) battleTable.update() else battleTable.isVisible = false
    }

    private fun addKeyboardListener() {
        stage.addListener(KeyboardPanningListener(mapHolder, allowWASD = true))
    }

    // We contain a map...
    override fun getShortcutDispatcherVetoer() = KeyShortcutDispatcherVeto.createTileGroupMapDispatcherVetoer()

    private suspend fun loadLatestMultiplayerState(): Unit = coroutineScope {
        if (game.screen != this@WorldScreen) return@coroutineScope // User already went somewhere else

        val loadingGamePopup = Popup(this@WorldScreen)
        launchOnGLThread {
            loadingGamePopup.addGoodSizedLabel("Loading latest game state...")
            loadingGamePopup.open()
        }

        try {
            debug("loadLatestMultiplayerState current game: gameId: %s, turn: %s, curCiv: %s",
                gameInfo.gameId, gameInfo.turns, gameInfo.currentPlayer)
            val latestGame = game.onlineMultiplayer.multiplayerServer.downloadGame(gameInfo.gameId)
            debug("loadLatestMultiplayerState downloaded game: gameId: %s, turn: %s, curCiv: %s",
                latestGame.gameId, latestGame.turns, latestGame.currentPlayer)
            if (viewingCiv.civID == latestGame.currentPlayer || viewingCiv.civID == Constants.spectator) {
                game.notifyTurnStarted()
            }
            launchOnGLThread {
                loadingGamePopup.close()
            }
            startNewScreenJob(latestGame, autoPlay)
        } catch (ex: Throwable) {
            launchOnGLThread {
                val (message) = LoadGameScreen.getLoadExceptionMessage(ex, "Couldn't download the latest game state!")
                loadingGamePopup.clear()
                loadingGamePopup.addGoodSizedLabel(message).colspan(2).row()
                loadingGamePopup.addButton("Retry") {
                    launchOnThreadPool("Load latest multiplayer state after error") {
                        loadLatestMultiplayerState()
                    }
                }.right()
                loadingGamePopup.addButton("Main menu") {
                    game.pushScreen{ MainMenuScreen() }
                }.left()
            }
        }
    }

    // This is private so that we will set the shouldUpdate to true instead.
    // That way, not only do we save a lot of unnecessary updates, we also ensure that all updates are called from the main GL thread
    // and we don't get any silly concurrency problems!
    private fun update() {

        if (uiEnabled) {
            displayTutorialsOnUpdate()

            updateSelectedCiv()

            bottomUnitTable.update()

            minimapWrapper.update(getGameViewConsideringForOfWar().civView.getCiv())
            bottomTileInfoTable.civView = getGameViewConsideringForOfWar().civView
            bottomTileInfoTable.updateTileTable(mapHolder.selectedTile)
            bottomTileInfoTable.x = stage.width - bottomTileInfoTable.width
            bottomTileInfoTable.y = if (game.settings.showMinimap) minimapWrapper.height + 5f else 0f

            battleTable.update()

            displayTutorialTaskOnUpdate()
        }

        mapHolder.resetArrows()
        if (UncivGame.Current.settings.showUnitMovements) {
            mapHolder.updateMovementOverlay(
                getGameViewConsideringForOfWar(),
                selectedGameView.civView.getUnits().asSequence(),
            )
        }

        zoomController.isVisible = UncivGame.Current.settings.showZoomButtons

        // if we use the clone, then when we update viewable tiles
        // it doesn't update the explored tiles of the civ... need to think about that harder
        // it causes a bug when we move a unit to an unexplored tile (for instance a cavalry unit which can move far)

        mapHolder.updateTiles(getGameViewConsideringForOfWar().civView)

        topBar.update(selectedCiv)
        if (tutorialTaskTable.isVisible)
            tutorialTaskTable.y = topBar.getYForTutorialTask() - tutorialTaskTable.height

        if (techPolicyAndDiplomacy.update())
            displayTutorial(TutorialTrigger.OtherCivEncountered)

        if (uiEnabled) {
            // UnitActionsTable measures geometry (its own y, techPolicyAndDiplomacy and fogOfWarButton), so call update this late
            unitActionsTable.y = bottomUnitTable.height
            unitActionsTable.update(bottomUnitTable.selectedUnit?.getUnit())
        }

        // If the game has ended, lets stop AutoPlay
        if (autoPlay.isAutoPlaying() && !gameInfo.oneMoreTurnMode && (viewingCiv.isDefeated() || gameInfo.checkForVictory())) {
            autoPlay.stopAutoPlay()
        }

        if (!hasOpenPopups() && !autoPlay.isAutoPlaying() && isPlayersTurn) {
            when {
                viewingCiv.shouldShowDiplomaticVotingResults() ->
                    UncivGame.Current.pushScreen{ DiplomaticVoteResultScreen(gameInfo.diplomaticVictoryVotesCast, viewingCiv) }
                !gameInfo.oneMoreTurnMode && (viewingCiv.isDefeated() || gameInfo.checkForVictory()) ->
                    game.pushScreen{ VictoryScreen(this) }
                hasPendingFreeGreatPerson() && !deferFreeGreatPersonPicker ->
                    openGreatPersonPicker()
                viewingCiv.popupAlerts.any() -> AlertPopup(this, viewingCiv.popupAlerts.first())
                viewingCiv.tradeRequests.isNotEmpty() -> {
                    // In the meantime this became invalid, perhaps because we accepted previous trades
                    for (tradeRequest in viewingCiv.tradeRequests.toList())
                        if (!TradeEvaluation().isTradeValid(tradeRequest.trade, viewingCiv,
                                gameInfo.getCivilization(tradeRequest.requestingCiv)))
                            viewingCiv.tradeRequests.remove(tradeRequest)

                    if (viewingCiv.tradeRequests.isNotEmpty()) // if a valid one still exists
                        TradePopup(this).open()
                }
            }
        }

        updateGameplayButtons()

        val coveredNotificationsTop = stage.height - statusButtons.y
        val coveredNotificationsBottom = (bottomTileInfoTable.height + bottomTileInfoTable.y)
//                (if (game.settings.showMinimap) minimapWrapper.height else 0f)
        notificationsScroll.update(viewingCiv.notifications, coveredNotificationsTop, coveredNotificationsBottom)

        val posZoomFromRight = if (game.settings.showMinimap) minimapWrapper.width
        else bottomTileInfoTable.width
        zoomController.setPosition(stage.width - posZoomFromRight - 10f, 10f, Align.bottomRight)
    }

    @Readonly
    internal fun hasPendingFreeGreatPerson() = viewingCiv.greatPeople.freeGreatPeople > 0

    internal fun openGreatPersonPicker() {
        deferFreeGreatPersonPicker = false
        game.pushScreen { GreatPersonPickerScreen(this, viewingCiv) }
    }

    private fun getCurrentTutorialTask(): Event? {
        if (!game.settings.tutorialTasksCompleted.contains("Create a trade route")) {
            if (viewingCiv.cache.citiesConnectedToCapitalToMediums.any { it.key.civ == viewingCiv })
                game.settings.addCompletedTutorialTask("Create a trade route")
        }
        val stateForConditionals = viewingCiv.state
        return gameInfo.ruleset.events.values.firstOrNull {
            it.presentation == Event.Presentation.Floating &&
                it.isAvailable(stateForConditionals)
        }
    }

    private fun displayTutorialsOnUpdate() {

        displayTutorial(TutorialTrigger.Introduction)

        displayTutorial(TutorialTrigger.EnemyCityNeedsConqueringWithMeleeUnit) {
            viewingCiv.diplomacy.values.asSequence()
                    .filter { it.diplomaticStatus == DiplomaticStatus.War }
                    .map { it.otherCiv } // we're now lazily enumerating over CivilizationInfo's we're at war with
                    .flatMap { it.cities.asSequence() } // ... all *their* cities
                    .filter { it.health == 1 } // ... those ripe for conquering
                    .flatMap { it.getCenterTile().getTilesInDistance(2) }
                    // ... all tiles around those in range of an average melee unit
                    // -> and now we look for a unit that could do the conquering because it's ours
                    //    no matter whether civilian, air or ranged, tell user he needs melee
                    .any { it.getUnits().any { unit -> unit.civ == viewingCiv } }
        }
        displayTutorial(TutorialTrigger.AfterConquering) { viewingCiv.cities.any { it.hasJustBeenConquered } }

        displayTutorial(TutorialTrigger.InjuredUnits) { gameInfo.getCurrentPlayerCivilization().units.getCivUnits().any { it.health < it.getMaxHealth() } }

        displayTutorial(TutorialTrigger.Workers) {
            gameInfo.getCurrentPlayerCivilization().units.getCivUnits().any {
                it.cache.hasUniqueToBuildImprovements && it.isCivilian() && !it.isGreatPerson()
            }
        }
    }

    private fun displayTutorialTaskOnUpdate() {
        fun setInvisible() {
            tutorialTaskTable.isVisible = false
            tutorialTaskTable.clear()
            tutorialTaskTableHash = 0
        }
        if (!game.settings.showTutorials || viewingCiv.isDefeated()) return setInvisible()
        val tutorialTask = getCurrentTutorialTask() ?: return setInvisible()

        if (!UncivGame.Current.isTutorialTaskCollapsed) {
            val hash = tutorialTask.hashCode()  // Default implementation is OK - we see the same instance or not
            if (hash != tutorialTaskTableHash) {
                val renderEvent = RenderEvent(tutorialTask, this) {
                    shouldUpdate = true
                }
                if (!renderEvent.isValid) return setInvisible()
                tutorialTaskTable.clear()
                tutorialTaskTable.add(renderEvent).pad(10f)
                tutorialTaskTableHash = hash
            }
        } else {
            tutorialTaskTable.clear()
            tutorialTaskTable.add(ImageGetter.getImage("OtherIcons/HiddenTutorialTask").apply { setSize(30f,30f) }).pad(5f)
            tutorialTaskTableHash = 0
        }
        tutorialTaskTable.pack()
        tutorialTaskTable.centerX(stage)
        tutorialTaskTable.y = topBar.getYForTutorialTask() - tutorialTaskTable.height
        tutorialTaskTable.onClick {
            UncivGame.Current.isTutorialTaskCollapsed = !UncivGame.Current.isTutorialTaskCollapsed
            displayTutorialTaskOnUpdate()
        }
        tutorialTaskTable.isVisible = true
    }

    fun setSelectedCiv(civ: Civilization) {
        selectedCiv = civ
        selectedGameView = GameView(gameInfo, civ, viewingCiv.isSpectator())
    }

    private fun updateSelectedCiv() {
        setSelectedCiv(when {
            bottomUnitTable.selectedUnit != null -> bottomUnitTable.selectedUnit!!.civ().getCiv()
            bottomUnitTable.selectedCity != null -> bottomUnitTable.selectedCity!!.owningCiv().getCiv()
            else -> viewingCiv
        })
    }

    class RestoreState(
        mapHolder: WorldMapHolder,
        val selectedCivName: String,
        val viewingCivName: String,
        val fogOfWar: Boolean
    ) {
        val zoom = mapHolder.scaleX
        val scrollX = mapHolder.scrollX
        val scrollY = mapHolder.scrollY
    }

    @Readonly
    fun getRestoreState(): RestoreState {
        return RestoreState(mapHolder, selectedCiv.civID, viewingCiv.civID, fogOfWar)
    }

    private fun restore(restoreState: RestoreState) {

        // This is not the case if you have a multiplayer game where you play as 2 civs
        if (viewingCiv.civID == restoreState.viewingCivName) {
            mapHolder.zoom(restoreState.zoom)
            mapHolder.scrollX = restoreState.scrollX
            mapHolder.scrollY = restoreState.scrollY
            mapHolder.updateVisualScroll()
        }

        setSelectedCiv(gameInfo.getCivilization(restoreState.selectedCivName))
        fogOfWar = restoreState.fogOfWar
    }

    //region Restart vote

    private var restartVotePopup: RestartVotePopup? = null

    /** Refreshes an open vote popup, or auto-opens one when a vote needs our input. */
    private fun onRestartVoteUpdated() {
        val popup = restartVotePopup
        if (popup != null && popup.hasParent()) popup.refresh()
        else maybeAutoOpenRestartVotePopup()
    }

    /** Opens the vote popup if there is an open vote we haven't voted on yet. */
    private fun maybeAutoOpenRestartVotePopup() {
        if (viewingCiv.isSpectator()) return
        if (canVoteOnRestart()) openRestartVotePopup()
    }

    /** Whether there is an open restart vote and we haven't voted yet. */
    private fun canVoteOnRestart(): Boolean {
        if (!gameInfo.gameParameters.isOnlineMultiplayer) return false
        val vote = game.onlineMultiplayer.getCachedRestartVote(gameInfo.gameId) ?: return false
        if (vote.status != RestartVoteStatus.OPEN) return false
        return !vote.hasVoted(game.settings.multiplayer.getUserId())
    }

    fun openRestartVotePopup() {
        if (restartVotePopup?.hasParent() == true) return
        restartVotePopup = RestartVotePopup(this).apply { open(force = true) }
    }

    /**
     * While a restart vote is open, players who haven't voted yet cannot end their turn -
     * they must handle the vote first.
     * @return true if the next turn was blocked
     */
    private fun restartVoteBlocksNextTurn(): Boolean {
        if (!canVoteOnRestart()) return false
        openRestartVotePopup()
        ToastPopup("You must vote on the restart vote before ending your turn!", this, 3000)
        return true
    }

    //endregion

    fun nextTurn() {
        if (restartVoteBlocksNextTurn()) return
        isPlayersTurn = false
        shouldUpdate = true
        val progressBar = NextTurnProgress(nextTurnButton)
        progressBar.start(this)

        // on a separate thread so the user can explore their world while we're passing the turn
        nextTurnUpdateJob = Concurrency.runOnNonDaemonThreadPool("NextTurn") {
            debug("Next turn starting")
            val startTime = System.currentTimeMillis()
            val originalGameInfo = gameInfo
            val gameInfoClone = originalGameInfo.clone()
            gameInfoClone.setTransients()  // this can get expensive on large games, not the clone itself

            progressBar.increment()

            gameInfoClone.nextTurn(progressBar, true)

            if (originalGameInfo.gameParameters.isOnlineMultiplayer) {
                // outer try-catch for non-auth exceptions
                try {
                    // keep retrying if upload fails AND reauthentication succeeds
                    var retryUpload: Boolean
                    do {
                        try {
                            game.onlineMultiplayer.updateGame(gameInfoClone)
                            // upload succeeded
                            retryUpload = false
                        } catch (_: MultiplayerAuthException) {
                            // true only if authentication succeeds (the popup permits retries)
                            // false only if user closes the auth popup or the popup init crashes
                            val authResult = CompletableDeferred<Boolean>()
                            launchOnGLThread {
                                try {
                                    AuthPopup(this@WorldScreen, authResult::complete).open(true)
                                } catch (ex: Exception) {
                                    // GL thread crashed during AuthPopup init, let's wrap up
                                    authResult.complete(false)
                                    // ensure exception is passed to crash handler
                                    throw ex
                                }
                            }
                            retryUpload = authResult.await()
                        }
                    } while (retryUpload)
                } catch (ex: Exception) { // non-auth exceptions
                    when (ex) {
                        is FileStorageRateLimitReached -> {
                            val message = "Server limit reached! Please wait for [${ex.limitRemainingSeconds}] seconds"
                            launchOnGLThread {
                                val cantUploadNewGamePopup = Popup(this@WorldScreen)
                                cantUploadNewGamePopup.addGoodSizedLabel(message).row()
                                cantUploadNewGamePopup.addCloseButton()
                                cantUploadNewGamePopup.open()
                            }
                        }
                        else -> {
                            val message = "Could not upload game! Reason: [${ex.message ?: "Unknown"}]"
                            launchOnGLThread {
                                val cantUploadNewGamePopup = Popup(this@WorldScreen)
                                cantUploadNewGamePopup.addGoodSizedLabel(message).row()
                                cantUploadNewGamePopup.addButton("Copy to clipboard") {
                                    Gdx.app.clipboard.contents = ex.stackTraceToString()
                                }
                                cantUploadNewGamePopup.addCloseButton()
                                cantUploadNewGamePopup.open()
                            }
                        }
                    }

                    this@WorldScreen.failedUpload = true // Since we couldn't push the new game clone, then we need to try again
                    this@WorldScreen.shouldUpdate = true
                    return@runOnNonDaemonThreadPool
                }
            }

            if (game.gameInfo != originalGameInfo) // while this was turning we loaded another game
                return@runOnNonDaemonThreadPool

            debug("Next turn took %sms", System.currentTimeMillis() - startTime)

            // Special case: when you are the only alive human player, the game will always be up to date
            if (gameInfo.gameParameters.isOnlineMultiplayer
                    && gameInfoClone.civilizations.count { it.isAlive() && it.playerType == PlayerType.Human } == 1) {
                gameInfoClone.isUpToDate = true
            }

            progressBar.increment()

            startNewScreenJob(gameInfoClone, autoPlay)
        }
    }

    /** Start the countdown timer for the current player's polling window. */
    private fun startPollingTimer() {
        stopPollingTimer()
        pollingSecondsRemaining = gameInfo.gameParameters.pollingIntervalSeconds
        pollingTimerJob = Concurrency.run("PollingTimer") {
            while (isActive && pollingSecondsRemaining > 0) {
                delay(1000)
                pollingSecondsRemaining--
                shouldUpdate = true
            }
            if (isActive && isPlayersTurn) {
                launchOnGLThread {
                    passPollingTurn()
                }
            }
        }
    }

    /** Cancel the polling countdown timer. */
    private fun stopPollingTimer() {
        pollingTimerJob?.cancel()
        pollingTimerJob = null
        pollingSecondsRemaining = 0
    }

    /** Send a one-shot online status query to all other players in this game. */
    fun sendOnlineQuery() {
        playerOnlineTimes[viewingCiv.civName] = System.currentTimeMillis()
        ChatWebSocket.requestMessageSend(
            com.unciv.logic.multiplayer.chat.Message.OnlineQuery(
                gameInfo.gameId, viewingCiv.civName
            )
        )
    }

    /** Returns true if the given civName has responded to an online status query within [onlineTimeoutMs]. */
    fun isPlayerOnline(civName: String): Boolean {
        val lastSeen = playerOnlineTimes[civName] ?: return false
        return (System.currentTimeMillis() - lastSeen) < onlineTimeoutMs
    }

    /** Called when the player clicks "I'm done".
     *  Marks the current player as done and either passes to the next player or advances the turn. */
    fun finishPollingTurn() {
        if (!isPlayersTurn || isNextTurnUpdateRunning()) return
        isPlayersTurn = false
        shouldUpdate = true
        stopPollingTimer()
        val progressBar = NextTurnProgress(nextTurnButton)
        progressBar.start(this)

        nextTurnUpdateJob = Concurrency.runOnNonDaemonThreadPool("PollingPass") {
            val originalGameInfo = gameInfo
            val gameInfoClone = originalGameInfo.clone()
            gameInfoClone.setTransients()

            progressBar.increment()

            // Mark current player as done for this turn
            gameInfoClone.playersFinishedThisTurn.add(viewingCiv.civID)

            if (gameInfoClone.allHumansFinishedPollingTurn()) {
                gameInfoClone.nextTurnPolling(progressBar)
                if (gameInfoClone.civilizations.count { it.isAlive() && it.playerType == PlayerType.Human } == 1)
                    gameInfoClone.isUpToDate = true
            } else {
                val nextHuman = gameInfoClone.findNextActiveHumanInPolling()
                if (nextHuman != null) {
                    gameInfoClone.currentPlayer = nextHuman.civID
                    gameInfoClone.currentPlayerCiv = nextHuman
                    gameInfoClone.currentTurnStartTime = System.currentTimeMillis()
                }
            }

            uploadPollingResult(originalGameInfo, gameInfoClone, progressBar)
        }
    }

    /**
     * Completes a simultaneous turn without uploading the local game snapshot. The last player
     * to finish owns settlement for this turn and rebuilds the authoritative snapshot by replaying
     * the sidecar operations from the turn-start save.
     */
    fun finishSimultaneousTurn() {
        // A player who already submitted is waiting for the last player. An operation signal
        // may call this again to perform the settlement check, but never while its upload job runs.
        if ((!isPlayersTurn && !gameInfo.isSimultaneousTurnsMode()) || isNextTurnUpdateRunning()) return
        isPlayersTurn = false
        shouldUpdate = true
        val progressBar = NextTurnProgress(nextTurnButton)
        progressBar.start(this)

        nextTurnUpdateJob = Concurrency.runOnNonDaemonThreadPool("SimultaneousPass") {
            val playerId = viewingCiv.playerId
            val submittedTurn = gameInfo.turns
            var submitted = simultaneousTurnDoneUploadedForTurn == submittedTurn
            try {
                if (!submitted) {
                    // Loudly report any local state change that was made without recording an
                    // operation: settlement cannot replay it, and it must not pass unnoticed.
                    checkForUnrecordedSimultaneousTurnChanges()
                    val done = SimultaneousTurnOperation(
                        turn = submittedTurn,
                        playerId = playerId,
                        sequence = gameInfo.nextSimultaneousOperationSequence++,
                        type = "done"
                    )
                    game.onlineMultiplayer.multiplayerServer.uploadSimultaneousTurnOperations(
                        gameInfo.gameId, getSimultaneousTurnOperations() + done
                    )
                    simultaneousTurnDoneUploadedForTurn = submittedTurn
                    submitted = true
                    ChatWebSocket.sendOperationSignal(gameInfo.gameId, done.turn, playerId, done.sequence)
                }

                val allOperations = try {
                    game.onlineMultiplayer.multiplayerServer.downloadSimultaneousTurnOperations(gameInfo.gameId)
                } catch (_: MultiplayerFileNotFoundException) {
                    emptyList()
                }
                val expectedPlayerIds = gameInfo.civilizations
                    .filter { it.isHuman() && it.isAlive() }
                    .map { it.playerId }
                    .toSet()
                val submittedPlayerIds = allOperations
                    .filter { it.turn == submittedTurn && it.type == "done" }
                    .map { it.playerId }
                    .toSet()
                if (simultaneousTurnPendingPlayerIds(allOperations).isNotEmpty()) {
                    progressBar.increment()
                    launchOnGLThread { nextTurnButton.update() }
                    return@runOnNonDaemonThreadPool
                }
                val skippedPlayerIds = expectedPlayerIds - submittedPlayerIds
                if (skippedPlayerIds.isNotEmpty() && simultaneousTurnTimeoutReportedForTurn != submittedTurn) {
                    simultaneousTurnTimeoutReportedForTurn = submittedTurn
                    val skippedNames = gameInfo.civilizations
                        .filter { it.playerId in skippedPlayerIds }
                        .joinToString(", ") { it.civName }
                    launchOnGLThread {
                        ToastPopup(
                            "Timed out waiting for [$skippedNames] - their turn was skipped".tr(),
                            this@WorldScreen, 4000
                        )
                    }
                }

                val lockAcquired = game.onlineMultiplayer.multiplayerServer
                    .acquireSimultaneousTurnSettlementLock(gameInfo.gameId, submittedTurn, playerId)
                if (!lockAcquired) {
                    // Another client is settling this turn. Keep waiting and let the watcher/signal
                    // retry: re-enabling isPlayersTurn here stranded every non-settler on the previous
                    // turn, because the only full-game reload path is guarded by
                    // isNextTurnUpdateRunning/isPlayersTurn.
                    launchOnGLThread { nextTurnButton.update() }
                    return@runOnNonDaemonThreadPool
                }

                // Keep the lock alive: rebuilding and advancing a large turn can exceed the server's
                // staleness window, and another client must not take settlement over midway.
                val lockRenewalJob = startSimultaneousTurnLockRenewal(gameInfo.gameId, submittedTurn, playerId)
                try {
                    val turnStart = game.onlineMultiplayer.multiplayerServer.tryDownloadGame(gameInfo.gameId)
                    if (turnStart.turns != submittedTurn) {
                        // Someone else settled while the lock was being acquired. Never run
                        // nextTurnPolling on an already-advanced save - that would skip a whole turn.
                        if (turnStart.turns > submittedTurn && game.gameInfo == gameInfo)
                            launchOnGLThread { startNewScreenJob(turnStart, autoPlay) }
                        return@runOnNonDaemonThreadPool
                    }
                    val turnOperations = allOperations.filter { it.turn == turnStart.turns && it.type != "done" }
                    val failedOperations = SimultaneousTurnReplay.replay(turnStart, turnOperations)
                    val failedOperationsReport = SimultaneousTurnOperations
                        .describeFailedSimultaneousTurnOperations(turnStart, failedOperations)
                    if (failedOperationsReport != null) {
                        // An un-replayable operation must not lock the game forever, but it must not
                        // vanish either. This used to be a debug line only: the turn advanced as if the
                        // operation had been applied and the player it belonged to was never told.
                        debug(
                            "Simultaneous turn settlement skipped %d un-replayable operations " +
                                "(game %s, turn %d): %s",
                            failedOperations.size, gameInfo.gameId, turnStart.turns,
                            failedOperations.joinToString { "${it.playerId}/${it.sequence}:${it.type}" }
                        )
                    }
                    if (!stillOwnsSimultaneousTurnSettlementLock(gameInfo.gameId, submittedTurn, playerId)) {
                        // Another client took the settlement over while this save was downloaded.
                        // Advancing it here would settle the same turn twice; leave it to the new owner.
                        debug(
                            "Simultaneous turn settlement aborted: the lock for game %s turn %d is no longer ours",
                            gameInfo.gameId, submittedTurn
                        )
                        return@runOnNonDaemonThreadPool
                    }
                    turnStart.nextTurnPolling(progressBar)
                    if (!stillOwnsSimultaneousTurnSettlementLock(gameInfo.gameId, submittedTurn, playerId)) {
                        // nextTurnPolling can take a while. Uploading a turn that the new owner already
                        // settled would overwrite its result with a differently replayed one.
                        debug(
                            "Simultaneous turn upload aborted: the lock for game %s turn %d is no longer ours",
                            gameInfo.gameId, submittedTurn
                        )
                        return@runOnNonDaemonThreadPool
                    }
                    // Tell the players whose actions were skipped, not just the one settling the turn.
                    SimultaneousTurnOperations.notifyPlayersOfFailedOperations(turnStart, failedOperations)
                    game.onlineMultiplayer.updateGame(turnStart)
                    if (game.gameInfo == gameInfo)
                        launchOnGLThread {
                            startNewScreenJob(
                                turnStart, autoPlay,
                                settlementReport = failedOperationsReport
                                    ?.let { "Some actions could not be applied".tr() + "\n\n" + it }
                            )
                        }
                } finally {
                    lockRenewalJob.cancel()
                    game.onlineMultiplayer.multiplayerServer.releaseSimultaneousTurnSettlementLock(
                        gameInfo.gameId, submittedTurn, playerId
                    )
                }
            } catch (ex: Exception) {
                // If the "done" marker never reached the server the player must be able to press
                // Done again; otherwise stay in the waiting state so the watcher can retry.
                val retryLocally = !submitted
                debug("Simultaneous turn pass failed (game %s, turn %d): %s", gameInfo.gameId, submittedTurn, ex)
                launchOnGLThread {
                    if (retryLocally) isPlayersTurn = true
                    shouldUpdate = true
                    nextTurnButton.update()
                    reportSimultaneousTurnFailure(submittedTurn, ex, retryLocally)
                }
            }
        }
    }

    /**
     * Tells the player why their turn did not reach the server. Before this the failure was only
     * logged: a rejected or lost upload looked exactly like a successful one, and the player kept
     * playing as if their actions had been recorded.
     */
    private fun reportSimultaneousTurnFailure(turn: Int, ex: Exception, retryLocally: Boolean) {
        val reason = (ex.message ?: ex.javaClass.simpleName).tr()
        if (turn == simultaneousTurnFailureReportedForTurn && reason == simultaneousTurnFailureReported) return
        simultaneousTurnFailureReportedForTurn = turn
        simultaneousTurnFailureReported = reason
        // Both halves are translated separately, and the reason is not put in square brackets: a server
        // message can itself contain brackets, which a placeholder-bearing string cannot carry.
        val lead = if (retryLocally)
            "Could not submit your turn - please try again"
        else
            "Could not finish the turn"
        ToastPopup("${lead.tr()}\n$reason", this, 5000)
    }

    /**
     * True while this client still owns the settlement lock for [turn]. Renewal is the only server
     * call that tells "still mine" apart from "taken over", so it is also the ownership check used
     * right before a settlement advances the game.
     *
     * The server hands a stale lock to the next client, so a settlement that lost its lock must stop
     * instead of uploading a turn that the new owner is already settling: two clients advancing the
     * same turn from the same turn-start save would replay the same operations differently and
     * overwrite each other.
     */
    private suspend fun stillOwnsSimultaneousTurnSettlementLock(gameId: String, turn: Int, owner: String): Boolean {
        return try {
            game.onlineMultiplayer.multiplayerServer
                .renewSimultaneousTurnSettlementLock(gameId, turn, owner)
        } catch (ex: Exception) {
            debug("Could not verify the settlement lock (game %s, turn %d): %s", gameId, turn, ex)
            false
        }
    }

    /**
     * Claims the targets a simultaneous-turn action is about to touch, so that two players cannot act
     * on the same unit or tile in the same turn. Both players only see their own orders until the turn
     * is settled, so the second player would otherwise learn about the collision when replay drops
     * their operation - after the unit already looked like it had received the order.
     *
     * Returns null when the action may proceed - this player got every target, or the server cannot
     * arbitrate reservations - and a message naming the player who got there first otherwise.
     */
    suspend fun reserveSimultaneousTurnActionTargets(keys: List<String>): String? {
        if (!gameInfo.isSimultaneousTurnsMode() || simultaneousTurnReservationsUnsupported) return null
        val playerId = viewingCiv.playerId
        if (playerId.isEmpty()) return null
        val conflicts = try {
            game.onlineMultiplayer.multiplayerServer
                .reserveSimultaneousTurnKeys(gameInfo.gameId, gameInfo.turns, playerId, keys)
        } catch (ex: Exception) {
            // Never block an action because the reservation request failed: replay still rejects real
            // conflicts, while refusing every action on a network hiccup would make the game unplayable.
            debug("Could not reserve simultaneous-turn targets (game %s, turn %d): %s", gameInfo.gameId, gameInfo.turns, ex)
            return null
        } ?: run {
            // 404: this server predates reservations, so keep the old behaviour for the rest of the session
            simultaneousTurnReservationsUnsupported = true
            return null
        }
        if (conflicts.isEmpty()) return null
        val claimed = conflicts.first()
        val claimant = gameInfo.civilizations.firstOrNull { it.playerId == claimed.owner }?.civName ?: claimed.owner
        debug("Simultaneous-turn target %s is already claimed by %s", claimed.key, claimed.owner)
        return "That target is already claimed by [$claimant] this turn".tr()
    }

    /**
     * Runs [action] once this player claimed the [keys] it is about to touch, and shows a toast
     * instead when another player got there first this turn.
     *
     * Callers are GL-thread click handlers and the reservation is a blocking HTTP request, so in a
     * simultaneous game the claim happens on a background thread and [action] runs a frame or two
     * later on the GL thread. Ordinary games keep the old synchronous behaviour.
     */
    fun runWithSimultaneousTurnTargetsReserved(keys: List<String>, action: () -> Unit) {
        if (!gameInfo.isSimultaneousTurnsMode() || simultaneousTurnReservationsUnsupported) {
            action()
            return
        }
        Concurrency.run("SimultaneousTurnReservation") {
            val blockedBy = reserveSimultaneousTurnActionTargets(keys)
            launchOnGLThread {
                if (blockedBy == null) action()
                else ToastPopup(blockedBy, this@WorldScreen, 3000)
            }
        }
    }

    /**
     * Keeps the settlement lock alive while a long turn is being rebuilt and advanced, so that
     * another client does not take the settlement over midway. A renewal that fails means the lock is
     * gone (or unreachable) and ends the loop - [stillOwnsSimultaneousTurnSettlementLock] is what
     * actually stops the settlement before it advances the game.
     */
    private fun startSimultaneousTurnLockRenewal(gameId: String, turn: Int, owner: String): Job {
        return Concurrency.run("SimultaneousTurnLockRenewal") {
            while (isActive) {
                delay(30_000)
                if (!stillOwnsSimultaneousTurnSettlementLock(gameId, turn, owner)) {
                    debug("The settlement lock for game %s turn %d is no longer ours", gameId, turn)
                    return@run
                }
            }
        }
    }

    /**
     * Human players this client is still legitimately waiting for. A player who submitted "done" is
     * never included, and a player silent for `simultaneousTurnTimeoutMinutes` is dropped so a client
     * that quit can no longer freeze the turn for everyone else.
     */
    private fun simultaneousTurnPendingPlayerIds(
        allOperations: List<SimultaneousTurnOperation>
    ): Set<String> {
        val humanPlayerIds = gameInfo.civilizations
            .filter { it.isHuman() && it.isAlive() }
            .map { it.playerId }
            .toSet()
        val finishedPlayerIds = allOperations
            .filter { it.turn == gameInfo.turns && it.type == "done" }
            .map { it.playerId }
            .toSet()
        val pendingPlayerIds = humanPlayerIds - finishedPlayerIds
        val timeoutMinutes = gameInfo.gameParameters.simultaneousTurnTimeoutMinutes
        if (pendingPlayerIds.isEmpty() || timeoutMinutes <= 0) return pendingPlayerIds
        if (simultaneousTurnWaitStartedForTurn != gameInfo.turns) {
            simultaneousTurnWaitStartedForTurn = gameInfo.turns
            simultaneousTurnWaitStartedAt = System.currentTimeMillis()
        }
        val now = System.currentTimeMillis()
        return pendingPlayerIds.filterNot { playerId ->
            val lastActivity = allOperations
                .filter { it.turn == gameInfo.turns && it.playerId == playerId }
                .maxOfOrNull { it.createdAtMillis } ?: 0L
            SimultaneousTurnOperations.hasSimultaneousTurnTimedOut(
                lastActivityMillis = lastActivity,
                waitStartedAtMillis = simultaneousTurnWaitStartedAt,
                nowMillis = now,
                timeoutMinutes = timeoutMinutes
            )
        }.toSet()
    }

    /** Periodically checks completion markers as a fallback for unavailable WebSocket signals. */
    private fun startSimultaneousTurnWatcher() {
        simultaneousTurnWatcherJob?.cancel()
        simultaneousTurnWatcherJob = Concurrency.run("SimultaneousTurnWatcher") {
            while (isActive) {
                delay(2000)
                if (!gameInfo.isSimultaneousTurnsMode() || isPlayersTurn || isNextTurnUpdateRunning()) continue
                try {
                    val operations = game.onlineMultiplayer.multiplayerServer
                        .downloadSimultaneousTurnOperations(gameInfo.gameId)
                    if (simultaneousTurnPendingPlayerIds(operations).isEmpty()) {
                        launchOnGLThread {
                            if (gameInfo.isSimultaneousTurnsMode() && !isPlayersTurn && !isNextTurnUpdateRunning())
                                finishSimultaneousTurn()
                        }
                    }
                } catch (_: Exception) {
                    // The next poll retries transient network failures.
                }
            }
        }
    }

    /**
     * Periodically reads the targets other players claimed for this turn, so the map can mark them.
     * A claim the player cannot see looks like a bug when an order on that target is refused, so the
     * marks come from the authority instead of being discovered at settlement. Claims change rarely,
     * so a slow poll is enough, and it runs during this player's turn as well - that is exactly when
     * the marks are needed.
     */
    private fun startSimultaneousTurnReservationWatcher() {
        simultaneousTurnReservationWatcherJob?.cancel()
        simultaneousTurnReservationWatcherJob = Concurrency.run("SimultaneousTurnReservationWatcher") {
            while (isActive) {
                delay(3000)
                if (!gameInfo.isSimultaneousTurnsMode() || simultaneousTurnReservationsUnsupported) continue
                val playerId = viewingCiv.playerId
                if (playerId.isEmpty()) continue
                val turn = gameInfo.turns
                val reservations = try {
                    game.onlineMultiplayer.multiplayerServer
                        .listSimultaneousTurnReservations(gameInfo.gameId, turn)
                } catch (ex: Exception) {
                    debug("Could not read simultaneous-turn reservations (game %s, turn %d): %s", gameInfo.gameId, turn, ex)
                    continue
                } ?: run {
                    // 404: this server predates reservations, so stop asking for the rest of the session
                    simultaneousTurnReservationsUnsupported = true
                    continue
                }
                val claimedTiles = SimultaneousTurnReservations.tilePositionsReservedByOthers(reservations, playerId)
                launchOnGLThread {
                    if (claimedTiles == simultaneousTurnReservedTilesOnMap && turn == simultaneousTurnReservedTilesTurn)
                        return@launchOnGLThread
                    simultaneousTurnReservedTilesOnMap = claimedTiles
                    simultaneousTurnReservedTilesTurn = turn
                    shouldUpdate = true
                }
            }
        }
    }

    /**
     * The save a client loads mid-turn is the turn-start state, because the server archive is only
     * rewritten when a turn settles. Replay the operations this player already uploaded so they see
     * the actions they took before reloading instead of having to redo them. When the player had
     * already submitted, resume as submitted so the settlement can still be driven from this client.
     */
    private fun restoreSimultaneousTurnOperations() {
        if (!gameInfo.isSimultaneousTurnsMode()) return
        val playerId = viewingCiv.playerId
        if (playerId.isEmpty()) return
        val turn = gameInfo.turns
        Concurrency.run("RestoreSimultaneousTurn") {
            val operations = try {
                game.onlineMultiplayer.multiplayerServer
                    .downloadSimultaneousTurnOperations(gameInfo.gameId)
            } catch (_: Exception) {
                return@run
            }
            val mine = operations.filter { it.turn == turn && it.playerId == playerId }
            if (mine.isEmpty()) return@run
            launchOnGLThread {
                if (!gameInfo.isSimultaneousTurnsMode() || gameInfo.turns != turn) return@launchOnGLThread
                val replayable = mine.filter { it.type != "done" }
                val merged = SimultaneousTurnOperations.merge(simultaneousTurnOperations, replayable)
                simultaneousTurnOperations.clear()
                simultaneousTurnOperations.addAll(merged)
                SimultaneousTurnReplay.replay(gameInfo, replayable)
                gameInfo.nextSimultaneousOperationSequence = maxOf(
                    gameInfo.nextSimultaneousOperationSequence,
                    (mine.maxOfOrNull { it.sequence } ?: 0L) + 1
                )
                gameInfo.setTransients()
                if (mine.any { it.type == "done" }) {
                    simultaneousTurnDoneUploadedForTurn = turn
                    isPlayersTurn = false
                }
                shouldUpdate = true
            }
        }
    }

    /** Re-checks the done markers after another simultaneous-turn client submits. */
    private fun retrySimultaneousTurnAfterSignal() {
        if (simultaneousTurnSettlementRetryPending) return
        simultaneousTurnSettlementRetryPending = true
        Concurrency.run("SimultaneousPassSignal") {
            repeat(40) {
                delay(250)
                if (!simultaneousTurnSettlementRetryPending) return@run
                launchOnGLThread {
                    if (gameInfo.isSimultaneousTurnsMode() && !isPlayersTurn && !isNextTurnUpdateRunning()) {
                        simultaneousTurnSettlementRetryPending = false
                        finishSimultaneousTurn()
                    }
                }
            }
            simultaneousTurnSettlementRetryPending = false
        }
    }


    private fun passPollingTurn() {
        if (!isPlayersTurn || isNextTurnUpdateRunning()) return
        isPlayersTurn = false
        shouldUpdate = true
        stopPollingTimer()
        val progressBar = NextTurnProgress(nextTurnButton)
        progressBar.start(this)

        nextTurnUpdateJob = Concurrency.runOnNonDaemonThreadPool("PollingPass") {
            val originalGameInfo = gameInfo
            val gameInfoClone = originalGameInfo.clone()
            gameInfoClone.setTransients()

            progressBar.increment()

            // NOT marking as done — find someone else to pass to
            val nextHuman = gameInfoClone.findNextActiveHumanInPolling()
            if (nextHuman != null) {
                gameInfoClone.currentPlayer = nextHuman.civID
                gameInfoClone.currentPlayerCiv = nextHuman
                gameInfoClone.currentTurnStartTime = System.currentTimeMillis()
            }
            // If null (only this player is left), we stay on the current player —
            // startNewScreenJob will restart the timer

            uploadPollingResult(originalGameInfo, gameInfoClone, progressBar)
        }
    }

    private suspend fun uploadPollingResult(
        originalGameInfo: GameInfo,
        gameInfoClone: GameInfo,
        progressBar: NextTurnProgress
    ) {
        if (originalGameInfo.gameParameters.isOnlineMultiplayer) {
            try {
                game.onlineMultiplayer.updateGame(gameInfoClone)
            } catch (ex: Exception) {
                this@WorldScreen.isPlayersTurn = true
                this@WorldScreen.shouldUpdate = true
                return
            }
        }

        if (game.gameInfo != originalGameInfo)
            return

        progressBar.increment()
        startNewScreenJob(gameInfoClone, autoPlay)
    }

    fun switchToNextUnit(resetDue: Boolean = true) {
        // Try to select something new if we already have the next pending unit selected.
        if (bottomUnitTable.selectedUnit != null && resetDue)
            bottomUnitTable.selectedUnit!!.getUnit().due = false
        val nextDueUnit = viewingCiv.units.cycleThroughDueUnits(bottomUnitTable.selectedUnit?.getUnit())
        if (nextDueUnit != null) {
            mapHolder.setCenterPosition(
                nextDueUnit.currentTile.position,
                immediately = false,
                selectUnit = false
            )
            bottomUnitTable.selectUnit(selectedGameView.getForeignMapUnitView(nextDueUnit).tryGetMapUnitView()!!)
        } else {
            mapHolder.removeAction(mapHolder.blinkAction)
            mapHolder.selectedTile = null
            bottomUnitTable.selectUnit()
        }
        shouldUpdate = true
    }

    @Readonly
    internal fun isNextTurnUpdateRunning(): Boolean {
        val job = nextTurnUpdateJob
        return job != null && job.isActive
    }

    private fun updateGameplayButtons() {
        nextTurnButton.update()

        updateAutoPlayStatusButton()
        updateMultiplayerStatusButton()

        statusButtons.update(false)
        val maxWidth = stage.width - techPolicyAndDiplomacy.width - 25f
        if(statusButtons.width > maxWidth) {
            statusButtons.update(true)
        }
        statusButtons.setPosition(stage.width - statusButtons.width - 10f, topBar.y - statusButtons.height - 10f)

        // Update chat button position to always be below techPolicyAndDiplomacy
        chatButton.updatePosition()
        onlineStatusButton.updatePosition()
    }

    private fun updateAutoPlayStatusButton() {
        if (statusButtons.autoPlayStatusButton == null) {
            if (game.settings.autoPlay.showAutoPlayButton)
                statusButtons.autoPlayStatusButton = AutoPlayStatusButton(this, nextTurnButton)
        } else {
            if (!game.settings.autoPlay.showAutoPlayButton) {
                statusButtons.autoPlayStatusButton = null
                autoPlay.stopAutoPlay()
            }
        }
    }

    private fun updateMultiplayerStatusButton() {
        if (gameInfo.gameParameters.isOnlineMultiplayer || game.settings.multiplayer.statusButtonInSinglePlayer) {
            if (statusButtons.multiplayerStatusButton != null) return
            statusButtons.multiplayerStatusButton = MultiplayerStatusButton(this,
                game.onlineMultiplayer.multiplayerFiles.getGameByGameId(gameInfo.gameId))
        } else {
            if (statusButtons.multiplayerStatusButton == null) return
            statusButtons.multiplayerStatusButton = null
        }
    }


    private var resizeDeferTimer: Timer? = null

    override fun resize(width: Int, height: Int) {
        resizeDeferTimer?.cancel()
        if (resizeDeferTimer == null && stage.viewport.screenWidth == width && stage.viewport.screenHeight == height) return
        resizeDeferTimer = timer("Resize", daemon = true, 500L, Long.MAX_VALUE) {
            resizeDeferTimer?.cancel()
            resizeDeferTimer = null
            startNewScreenJob(gameInfo, autoPlay, true) // start over
        }
    }

    override fun render(delta: Float) {
        //  This is so that updates happen in the MAIN THREAD, where there is a GL Context,
        //    otherwise images will not load properly!
        if (shouldUpdate && resizeDeferTimer == null) {
            shouldUpdate = false
            localSnapshotHandler.markDirty()

            // Since updating the worldscreen can take a long time, *especially* the first time, we disable input processing to avoid ANRs
            Gdx.input.inputProcessor = null
            update()
            showTutorialsOnNextTurn()
            if (Gdx.input.inputProcessor == null) // Update may have replaced the worldscreen with a GreatPersonPickerScreen etc, so the input would already be set
                Gdx.input.inputProcessor = stage
        }

        localSnapshotHandler.update()

        super.render(delta)
    }


    private fun showTutorialsOnNextTurn() {
        if (!game.settings.showTutorials || autoPlay.isAutoPlaying()) return
        displayTutorial(TutorialTrigger.SlowStart)
        displayTutorial(TutorialTrigger.CityExpansion) { viewingCiv.cities.any { it.expansion.tilesClaimed() > 0 } }
        displayTutorial(TutorialTrigger.BarbarianEncountered) { viewingCiv.viewableTiles.any { it.getUnits().any { unit -> unit.civ.isBarbarian } } }
        displayTutorial(TutorialTrigger.RoadsAndRailroads) { viewingCiv.cities.size > 2 }
        displayTutorial(TutorialTrigger.Happiness) { viewingCiv.getHappiness() < 5 }
        displayTutorial(TutorialTrigger.Unhappiness) { viewingCiv.getHappiness() < 0 }
        displayTutorial(TutorialTrigger.GoldenAge) { viewingCiv.goldenAges.isGoldenAge() }
        displayTutorial(TutorialTrigger.IdleUnits) { gameInfo.turns >= 50 && game.settings.checkForDueUnits }
        displayTutorial(TutorialTrigger.ContactMe) { gameInfo.turns >= 100 }
        val resources = viewingCiv.detailedCivResources.asSequence().filter { it.origin == "All" }  // Avoid full list copy
        displayTutorial(TutorialTrigger.LuxuryResource) { resources.any { it.resource.resourceType == ResourceType.Luxury } }
        displayTutorial(TutorialTrigger.StrategicResource) { resources.any { it.resource.resourceType == ResourceType.Strategic } }
        displayTutorial(TutorialTrigger.EnemyCity) {
            viewingCiv.getKnownCivs().filter { viewingCiv.isAtWarWith(it) }
                    .flatMap { it.cities.asSequence() }.any { viewingCiv.hasExplored(it.getCenterTile()) }
        }
        displayTutorial(TutorialTrigger.Embarking) { viewingCiv.hasUnique(UniqueType.LandUnitEmbarkation) }
        displayTutorial(TutorialTrigger.NaturalWonders) { viewingCiv.naturalWonders.size > 0 }
        displayTutorial(TutorialTrigger.WeLoveTheKingDay) { viewingCiv.cities.any { it.demandedResource != "" } }
    }

    private fun backButtonAndESCHandler() {

        // Deselect Unit
        if (bottomUnitTable.selectedUnit != null) {
            bottomUnitTable.selectUnit()
            shouldUpdate = true
            return
        }

        // Deselect city
        if (bottomUnitTable.selectedCity != null) {
            bottomUnitTable.selectUnit()
            shouldUpdate = true
            return
        }

        if (bottomUnitTable.selectedSpy != null) {
            bottomUnitTable.selectSpy(null)
            shouldUpdate = true
            return
        }

        game.popScreen()
    }

    fun autoSave() {
        waitingForAutosave = true
        shouldUpdate = true
        UncivGame.Current.files.autosaves.requestAutoSave(gameInfo, true).invokeOnCompletion {
            // only enable the user to next turn once we've saved the current one
            waitingForAutosave = false
            shouldUpdate = true
        }
    }
}

/** This exists so that no reference to the current world screen remains, so the old world screen can get garbage collected during [UncivGame.loadGame]. */
private fun startNewScreenJob(
    gameInfo: GameInfo,
    autoPlay: AutoPlay,
    autosaveDisabled: Boolean = false,
    settlementReport: String? = null
) {
    Concurrency.run {
        val newWorldScreen = try {
            UncivGame.Current.loadGame(gameInfo, autoPlay)
        } catch (notAPlayer: UncivShowableException) {
            val (message) = LoadGameScreen.getLoadExceptionMessage(notAPlayer)
            withGLContext {
                UncivGame.Current.goToMainMenu { mainMenu -> ToastPopup(message, mainMenu) }
            }
            return@run
        } catch (_: OutOfMemoryError) {
            withGLContext {
                UncivGame.Current.goToMainMenu { mainMenu -> ToastPopup("Not enough memory on phone to load game!", mainMenu) }
            }
            return@run
        }

        val shouldAutoSave = !autosaveDisabled
                && gameInfo.turns % UncivGame.Current.settings.turnsBetweenAutosaves == 0
        if (shouldAutoSave) {
            newWorldScreen.autoSave()
        }

        // Shown on the *new* screen: a popup opened on the screen being replaced would go away with it.
        if (settlementReport != null) {
            withGLContext {
                Popup(newWorldScreen)
                    .apply {
                        addGoodSizedLabel(settlementReport)
                        addCloseButton()
                    }
                    .open()
            }
        }
    }
}
