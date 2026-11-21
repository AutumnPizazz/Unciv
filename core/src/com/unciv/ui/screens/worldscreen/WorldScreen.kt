package com.unciv.ui.screens.worldscreen

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.utils.Align
import com.unciv.Constants
import com.unciv.GUI
import com.unciv.UncivGame
import com.unciv.logic.GameInfo
import com.unciv.view.GameView
import com.unciv.view.MapUnitView
import com.unciv.view.getCapital
import com.unciv.logic.UncivShowableException
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.civilization.diplomacy.DiplomaticStatus
import com.unciv.logic.event.EventBus
import com.unciv.logic.map.HexCoord
import com.unciv.json.json
import com.unciv.logic.multiplayer.MultiplayerGameUpdated
import com.unciv.logic.multiplayer.OnlineStatusUpdated
import com.unciv.logic.multiplayer.RealtimeTurnActionReceived
import com.unciv.logic.multiplayer.RealtimeTurnActions
import com.unciv.logic.multiplayer.RestartVoteStatus
import com.unciv.logic.multiplayer.RestartVoteUpdated
import com.unciv.logic.multiplayer.SimultaneousTurnFailedOperation
import com.unciv.logic.multiplayer.SimultaneousTurnFailureReason
import com.unciv.logic.multiplayer.SimultaneousTurnGameStateResult
import com.unciv.logic.multiplayer.SimultaneousTurnOperation
import com.unciv.logic.multiplayer.SimultaneousTurnOperationUploadQueue
import com.unciv.logic.multiplayer.SimultaneousTurnOperationRejectedException
import com.unciv.logic.multiplayer.SimultaneousTurnOperations
import com.unciv.logic.multiplayer.SimultaneousTurnReplay
import com.unciv.logic.multiplayer.SimultaneousTurnClaims
import com.unciv.logic.multiplayer.SimultaneousTurnStateAction
import com.unciv.logic.multiplayer.chat.ChatWebSocket
import com.unciv.logic.multiplayer.getAliveHumanPlayerIds
import com.unciv.logic.multiplayer.storage.FileStorageRateLimitReached
import com.unciv.logic.multiplayer.storage.MultiplayerAuthException
import com.unciv.logic.multiplayer.storage.MultiplayerTurnActionConflictException
import com.unciv.logic.trade.TradeEvaluation
import com.unciv.models.TutorialTrigger
import com.unciv.models.UnitActionType
import com.unciv.models.metadata.GameSetupInfo
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.translations.tr
import com.unciv.ui.components.extensions.centerX
import com.unciv.ui.components.extensions.darken
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.input.KeyShortcutDispatcherVeto
import com.unciv.ui.components.input.KeyboardBinding
import com.unciv.ui.components.input.KeyboardPanningListener
import com.unciv.ui.components.input.onClick
import com.unciv.ui.components.widgets.ExpanderTab
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
import com.unciv.ui.screens.worldscreen.minimap.MapOverlayToggleButton
import com.unciv.ui.screens.worldscreen.minimap.Minimap
import com.unciv.ui.screens.worldscreen.minimap.MinimapHolder
import com.unciv.ui.screens.worldscreen.status.AutoPlayStatusButton
import com.unciv.ui.screens.worldscreen.status.MultiplayerStatusButton
import com.unciv.ui.screens.worldscreen.status.NextTurnButton
import com.unciv.ui.screens.worldscreen.status.NextTurnProgress
import com.unciv.ui.screens.worldscreen.status.SmallUnitButton
import com.unciv.ui.screens.worldscreen.status.StatusButtons
import com.unciv.ui.screens.worldscreen.topbar.WorldScreenTopBar
import com.unciv.ui.screens.worldscreen.topbar.WorldScreenTopBar.OverviewAndSupplyTable
import com.unciv.ui.screens.worldscreen.topbar.WorldScreenTopBar.SelectedCivilizationTable
import com.unciv.ui.screens.worldscreen.topbar.WorldScreenTopBarResources
import com.unciv.ui.screens.worldscreen.topbar.WorldScreenTopBarStats
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import yairm210.purity.annotations.Readonly
import java.util.Timer
import kotlin.concurrent.timer


/**
 *  Layout overview
 *  # CENTER
 *  -   [WorldMapHolder]: Center / background
 *  # TOP
 *  -   [WorldScreenTopBar]: Top
 *      -   [WorldScreenTopBarStats]
 *      -   [WorldScreenTopBarResources]
 *      -   [SelectedCivilizationTable] ... [OverviewAndSupplyTable]
 *  -   `tutorialTaskTable`: on-demand, top center just below WorldScreenTopBarResources
 *  # LEFT
 *  -   [TechPolicyDiplomacyButtons]: Upper left
 *  -   [ChatButton]: Left just below Tech & co
 *  -   [UnitActionsTable]: Left just above Unit, dynamic height, tries to squeeze between Tech & Unit
 *  -   [UnitTable]: Bottom left corner, displays what is selected (Unit or City)
 *  # BOTTOM
 *  -   [BattleTable]: Bottom center, only on demand
 *  # RIGHT
 *  -   [StatusButtons] contains [NextTurnButton], [AutoPlayStatusButton], [MultiplayerStatusButton], [SmallUnitButton] (Skip/Cycle):
 *      dynamically switches to vertical to avoid collision with TechPolicyDiplomacyButtons
 *  -   [NotificationsScroll]: Z-order **below** all other elements, can extend the whole right side,
 *      but tries to ensure the entire content can be scrolled between StatusButtons and TileInfoTable/Minimap.
 *  -   [TileInfoTable]: just above the Minimap
 *  -   [MinimapHolder] contains the actual [Minimap] and the five [MapOverlayToggleButton]s
 *  -   [ZoomButtonPair]: optional, just to the left of MinimapHolder
 */

/**
 * Do not create this screen without seriously thinking about the implications: this is the single most memory-intensive class in the application.
 * There really should ever be only one in memory at the same time, likely managed by [UncivGame].
 *
 * @param gameInfo The game state the screen should represent
 * @param viewingCiv The currently active [Civilization]
 * @param restoreState
 */
class WorldScreen(
    val gameInfo: GameInfo,
    val autoPlay: AutoPlay,
    internal val viewingCiv: Civilization,
    restoreState: RestoreState? = null
) : BaseScreen() {
    /** When set, causes the screen to update in the next [render] event */
    var shouldUpdate = false

    private val reusedSimultaneousTurn = restoreState?.simultaneousTurn?.takeIf { it.gameInfo === gameInfo }
    @Transient
    private val simultaneousTurnOperations = ArrayList(reusedSimultaneousTurn?.appliedOperations.orEmpty())
    private val simultaneousTurnLocalOperationSequences = HashSet(
        reusedSimultaneousTurn?.appliedOperations.orEmpty().map { it.sequence }
    )
    private val simultaneousTurnOperationsLock = Any()
    private val realtimeTurnSyncMutex = Mutex()
    @Volatile
    private var lastRealtimeTurnServerSequence = reusedSimultaneousTurn?.serverSequence ?: 0L
    @Volatile
    private var realtimeTurnReplayBlocked = false
    @Volatile
    private var realtimeTurnRecoveryRequired = false
    @Transient
    @Volatile
    private var simultaneousTurnOperationsRestored = !gameInfo.isSimultaneousTurnsMode()
    internal val isRestoringSimultaneousTurn: Boolean
        get() = !simultaneousTurnOperationsRestored && !viewingCiv.isSpectator && viewingCiv.playerId.isNotEmpty()
    @Transient
    private var simultaneousTurnOperationUploadJob: Job? = null
    @Transient
    private var simultaneousTurnRestoreJob: Job? = null
    @Transient
    private var realtimeTurnSyncJob: Job? = null
    private val simultaneousTurnOutbox by lazy {
        game.onlineMultiplayer.multiplayerFiles.simultaneousTurnOutbox(
            gameInfo.gameId, gameInfo.turns, gameInfo.currentTurnStartTime, viewingCiv.playerId
        )
    }
    private val simultaneousTurnOperationUploader by lazy {
        SimultaneousTurnOperationUploadQueue(
            upload = { operations ->
                for (operation in operations) {
                    uploadRealtimeTurnOperation(operation)
                }
            },
            onUploaded = { syncRealtimeTurnActions() },
            onFailure = { ex ->
                debug("Could not upload simultaneous-turn operations (game %s, turn %d): %s", gameInfo.gameId, gameInfo.turns, ex)
            },
            onRejected = { operation, ex ->
                recoverRejectedRealtimeTurnOperation(operation, ex)
            }
        )
    }

    private suspend fun uploadRealtimeTurnOperation(operation: SimultaneousTurnOperation) {
        val server = game.onlineMultiplayer.multiplayerServer
        try {
            val entry = checkNotNull(server.appendRealtimeTurnAction(gameInfo.gameId, RealtimeTurnActions.requestFor(operation))) {
                "The multiplayer server does not support real-time simultaneous turns"
            }
            if (entry.kind != "action" || entry.operations.singleOrNull()?.sameAction(operation) != true) {
                val cause = IllegalStateException("The server acknowledged a different action for this client sequence")
                throw SimultaneousTurnOperationRejectedException(operation, cause)
            }
        } catch (ex: MultiplayerTurnActionConflictException) {
            throw SimultaneousTurnOperationRejectedException(operation, ex)
        }
    }

    /** The state this screen started the turn from, used to detect local changes that no recorded
     *  operation covers. Only kept for simultaneous-turn games, where settlement replays operations
     *  on the turn-start save instead of trusting the client's final state. */
    private val simultaneousTurnStartSnapshot: GameInfo? =
        if (reusedSimultaneousTurn != null) reusedSimultaneousTurn.turnStart
        else if (gameInfo.isSimultaneousTurnsMode()) {
            gameInfo.clone()
        } else null


    /** Indicates it's the player's ([viewingCiv]) turn */
    var isPlayersTurn = if (gameInfo.isSimultaneousTurnsMode()) false else viewingCiv.isCurrentPlayer()
        internal set

    /** Records a completed local operation for the future simultaneous-turn settlement pass. */
    fun recordSimultaneousTurnOperation(type: String, payload: Any) =
        recordSimultaneousTurnOperations(type, listOf(payload))

    private fun recordSimultaneousTurnOperations(type: String, payloads: List<Any>) {
        if (!gameInfo.isSimultaneousTurnsMode()) return
        val playerId = viewingCiv.playerId
        if (playerId.isEmpty() || payloads.isEmpty()) return
        synchronized(simultaneousTurnOperationsLock) {
            if (realtimeTurnRecoveryRequired) return
            val operations = payloads.map { payload ->
                SimultaneousTurnOperation(
                    turn = gameInfo.turns,
                    playerId = playerId,
                    sequence = gameInfo.nextSimultaneousOperationSequence++,
                    type = type,
                    payload = json().toJson(payload),
                    createdAtMillis = System.currentTimeMillis()
                )
            }
            try {
                simultaneousTurnOutbox.record(operations)
            } catch (ex: Exception) {
                // The action already ran. Still send it to the server if the device cannot journal it.
                Log.error("Could not save the local simultaneous-turn action batch", ex)
                Gdx.app.postRunnable {
                    if (game.worldScreen == this)
                        ToastPopup("Could not save actions locally. Wait for sync before leaving the game.".tr(), this, 5000)
                }
            }
            simultaneousTurnOperations.addAll(operations)
            simultaneousTurnLocalOperationSequences.addAll(operations.map { it.sequence })
            simultaneousTurnOperationUploader.enqueueBatch(operations)
        }
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
        recordSimultaneousTurnOperations(
            SimultaneousTurnOperations.stateOperationType(actionName),
            listOf(result)
        )
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
        synchronized(simultaneousTurnOperationsLock) {
            SimultaneousTurnOperations.merge(emptyList(), simultaneousTurnOperations)
        }
     // only this class is allowed to make changes

    /** Indicates that a game failed to upload, and needs to be uploaded */
    var failedUpload = false
        private set

    /** Defers reopening an unfulfilled free-Great-Person choice until the player requests it. */
    internal var deferFreeGreatPersonPicker = false

    /** Selected civilization, used in spectator and replay mode, equals viewingCiv in ordinary games */
    var selectedCiv = viewingCiv
        internal set
    /** The [selectedCiv]'s perspective. For non-spectators this equals the viewingGameView.
     *  Only ever differs from [spectatorGameView] for spectators. */
    var selectedGameView = GameView(gameInfo, selectedCiv, spectatorMode = viewingCiv.isSpectator)
        internal set

    /** The [viewingCiv]'s own perspective.
     * This should only ever be used A. for spectators B. when forOfWar is false, for specific UI elements */
    private val spectatorGameView by lazy {
        if (!viewingCiv.isSpectator) throw Exception("Managed to disable fog of war without being spectator?!")
        GameView(gameInfo, viewingCiv, spectatorMode = true)
    }
    fun getGameViewConsideringForOfWar() = if (fogOfWar) selectedGameView else spectatorGameView

    /** UI toggle only (spectator mode): whether the map/tile panel should show [selectedGameView]'s
     *  (fogged) vision or [spectatorGameView]'s (the spectator's own, unrestricted) vision. */
    var fogOfWar = true

    /** `true` when it's the player's turn unless he is a spectator */
    val canChangeState
        get() = isPlayersTurn && !viewingCiv.isSpectator && simultaneousTurnOperationsRestored &&
            !realtimeTurnRecoveryRequired && !realtimeTurnReplayBlocked

    val mapHolder = WorldMapHolder(this, gameInfo.tileMap)

    internal var waitingForAutosave = false

    // Floating Widgets going counter-clockwise
    internal val topBar = WorldScreenTopBar(this)
    internal val techPolicyAndDiplomacy = TechPolicyDiplomacyButtons(this)
    internal val chatButton = ChatButton(this)
    internal val onlineStatusButton = OnlineStatusButton(this)
    internal val unitActionsTable = UnitActionsTable(this)
    /** Bottom left widget holding information about a selected unit or city */
    internal val bottomUnitTable = UnitTable(this)
    internal val battleTable = BattleTable(this)
    private val zoomController = ZoomButtonPair(mapHolder)
    internal val minimapWrapper = MinimapHolder(mapHolder)
    internal val bottomTileInfoTable = TileInfoTable(this)
    internal val notificationsScroll = NotificationsScroll(this)
    internal val nextTurnButton = NextTurnButton(this)
    internal val statusButtons = StatusButtons(nextTurnButton)
    internal val smallUnitButton = SmallUnitButton(this, statusButtons)
    private val tutorialTaskTable = TutorialTaskTable(this)

    private var nextTurnUpdateJob: Job? = null
    /** Turn the server already accepted as ready, so retries don't submit it again. */
    @Transient
    private var simultaneousTurnDoneUploadedForTurn = -1
    private var simultaneousTurnReadyClientSequence = -1L
    /** Polls the authoritative action log so a missed WebSocket signal cannot strand a player. */
    private var simultaneousTurnWatcherJob: Job? = null
    /**
     * Turn and reason of the last submission/settlement failure shown to the player. The pass retries
     * every few seconds, so the same failure must not produce a new ToastPopup each time.
     */
    @Transient
    private var simultaneousTurnFailureReportedForTurn = -1
    @Transient
    private var simultaneousTurnFailureReported: String? = null
    /** Watches the operations relayed for this turn, so the targets other players claimed can be shown. */
    private var simultaneousTurnClaimWatcherJob: Job? = null
    /** Targets other players' operations of the current turn touch, with the player who ordered each. */
    @Transient
    private var simultaneousTurnClaimsByOthers: Map<String, String> = emptyMap()
    @Transient
    private var simultaneousTurnClaimsTurn = -1

    /**
     * Tiles another player already gave an order for in the turn being played, for the map marks that
     * make a claim visible before an order is refused. Derived from the operations relayed for the
     * turn, so no server has to arbitrate anything; stale claims are dropped as soon as the turn
     * advances.
     */
    val simultaneousTurnClaimedTiles: Set<HexCoord>
        get() = if (simultaneousTurnClaimsTurn == gameInfo.turns)
            simultaneousTurnClaimsByOthers.keys.mapNotNull { SimultaneousTurnClaims.tilePositionOf(it) }.toSet()
        else emptySet()

    /**
     * The civilization whose order already touches [position] in the turn being played, or null when
     * nobody's does. Lets the UI refuse an order where the player can see it, instead of leaving them
     * to discover the collision at settlement.
     */
    fun simultaneousTurnClaimantOf(position: HexCoord): String? =
        simultaneousTurnClaimantOfKeys(listOf(SimultaneousTurnClaims.forTile(position.x, position.y)))

    /** The civilization whose order already touches one of [keys], or null when all of them are free. */
    private fun simultaneousTurnClaimantOfKeys(keys: List<String>): String? {
        if (simultaneousTurnClaimsTurn != gameInfo.turns) return null
        val owner = keys.firstNotNullOfOrNull { simultaneousTurnClaimsByOthers[it] } ?: return null
        return gameInfo.civilizations.firstOrNull { it.playerId == owner }?.civName ?: owner
    }

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
        mapHolder.setCenterPosition(tileToCenterOn, immediately = true, selectUnit = !viewingCiv.isSpectator)

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
            events.receive(RealtimeTurnActionReceived::class, { it.gameId == gameId }) { signal ->
                if (!gameInfo.isSimultaneousTurnsMode() || signal.turn != gameInfo.turns) return@receive
                syncRealtimeTurnActions()
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
                simultaneousTurnOperationUploadJob = Concurrency.run("SimultaneousTurnOperationUpload") {
                    simultaneousTurnOperationUploader.run()
                }
                startSimultaneousTurnWatcher()
                startSimultaneousTurnClaimWatcher()
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
        simultaneousTurnOperationUploadJob?.cancel()
        simultaneousTurnRestoreJob?.cancel()
        realtimeTurnSyncJob?.cancel()
        simultaneousTurnWatcherJob?.cancel()
        simultaneousTurnWatcherJob = null
        simultaneousTurnClaimWatcherJob?.cancel()
        simultaneousTurnClaimWatcherJob = null
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
        game.pushScreen { EmpireOverviewScreen(selectedGameView.civView, category, selection) }
    }

    fun openNewGameScreen() {
        val newGameSetupInfo = GameSetupInfo(gameInfo)
        newGameSetupInfo.mapParameters.reseed()
        val newGameScreen = NewGameScreen(newGameSetupInfo)
        game.pushScreen { newGameScreen }
    }

    fun openSaveGameScreen() {
        // See #10353 - we don't support locally saving an online multiplayer game
        if (gameInfo.gameParameters.isOnlineMultiplayer) return
        game.pushScreen { SaveGameScreen(gameInfo) }
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
        globalShortcuts.add(KeyboardBinding.VictoryScreen) { game.pushScreen { VictoryScreen(this) } }
        globalShortcuts.add(KeyboardBinding.EmpireOverviewStats) { openEmpireOverview(EmpireOverviewCategories.Stats) }
        globalShortcuts.add(KeyboardBinding.EmpireOverviewResources) { openEmpireOverview(EmpireOverviewCategories.Resources) }
        globalShortcuts.add(KeyboardBinding.QuickSave) { QuickSave.save(gameInfo, this) }
        globalShortcuts.add(KeyboardBinding.QuickLoad) { QuickSave.load(this) }
        globalShortcuts.add(KeyboardBinding.ViewCapitalCity) {
            val capital = gameInfo.getCurrentPlayerCivilization().getCapital()
            if (capital != null && !mapHolder.setCenterPosition(capital.location.toHexCoord()))
                game.pushScreen { CityScreen(selectedGameView.getCityView(capital)) }
        }
        globalShortcuts.add(KeyboardBinding.Options) { // Game Options
            openOptionsPopup { nextTurnButton.update() }
        }
        globalShortcuts.add(KeyboardBinding.SaveGame) { openSaveGameScreen() }    //   Save
        globalShortcuts.add(KeyboardBinding.LoadGame) { game.pushScreen { LoadGameScreen() } }    //   Load
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
        if (gameInfo.civilizations.count { it.isHuman() && !it.isSpectator } > 1) return
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
                    game.pushScreen { MainMenuScreen() }
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
            bottomTileInfoTable.update(getGameViewConsideringForOfWar().civView, mapHolder.selectedTile)
            bottomTileInfoTable.x = stage.width - bottomTileInfoTable.width
            bottomTileInfoTable.y = if (game.settings.showMinimap) minimapWrapper.height + 5f else 0f

            battleTable.update()
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
                    game.pushScreen { DiplomaticVoteResultScreen(gameInfo.diplomaticVictoryVotesCast, viewingCiv) }
                !gameInfo.oneMoreTurnMode && (viewingCiv.isDefeated() || gameInfo.checkForVictory()) ->
                    game.pushScreen { VictoryScreen(this) }
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

        // Late: the task card is fitted into the room left by the widgets around it, so they must have their final size
        if (uiEnabled) tutorialTaskTable.update()

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

    fun setSelectedCiv(civ: Civilization) {
        if (!viewingCiv.isSpectator) throw Exception("Tried to select a different civ when not spectator!")
        selectedCiv = civ
        selectedGameView = GameView(gameInfo, civ, viewingCiv.isSpectator)
    }

    private fun updateSelectedCiv() {
        // Only spectators/replays follow the selection's owner. For ordinary players selectedCiv must stay viewingCiv:
        // otherwise a selected unit that just changed owner (gifted to a city-state, captured) would switch the
        // whole view to its new owner, defeat UnitPresenter's "no longer ours" deselection, and leave a stale selection.
        if (!viewingCiv.isSpectator) return
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
        internal var simultaneousTurn: SimultaneousTurnScreenState? = null
    }

    internal class SimultaneousTurnScreenState(
        val gameInfo: GameInfo,
        val turnStart: GameInfo,
        val appliedOperations: List<SimultaneousTurnOperation>,
        val serverSequence: Long
    )

    fun getRestoreState(): RestoreState {
        val state = RestoreState(mapHolder, selectedCiv.civID, viewingCiv.civID, fogOfWar)
        val turnStart = simultaneousTurnStartSnapshot
        if (turnStart != null)
            state.simultaneousTurn = SimultaneousTurnScreenState(
                gameInfo, turnStart, getSimultaneousTurnOperations(), lastRealtimeTurnServerSequence
            )
        return state
    }

    private fun restore(restoreState: RestoreState) {

        // This is not the case if you have a multiplayer game where you play as 2 civs
        if (viewingCiv.civID != restoreState.viewingCivName) return

        mapHolder.zoom(restoreState.zoom)
        mapHolder.scrollX = restoreState.scrollX
        mapHolder.scrollY = restoreState.scrollY
        mapHolder.updateVisualScroll()

        if (!viewingCiv.isSpectator) return
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
        if (viewingCiv.isSpectator) return
        if (canVoteOnRestart()) openRestartVotePopup()
    }

    /** Whether there is an open restart vote and we haven't voted yet. */
    private fun canVoteOnRestart(): Boolean {
        if (!gameInfo.gameParameters.isOnlineMultiplayer) return false
        val vote = game.onlineMultiplayer.getCachedRestartVote(gameInfo.gameId) ?: return false
        if (vote.status != RestartVoteStatus.OPEN) return false
        val playerId = game.settings.multiplayer.getUserId()
        return !vote.hasVoted(playerId) && playerId in gameInfo.getAliveHumanPlayerIds()
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

    /** Handles "I'm done" without blocking automatic passes or settlement retries. */
    internal fun finishTurnFromPlayer() {
        if (!isPlayersTurn || isNextTurnUpdateRunning() || restartVoteBlocksNextTurn()) return
        if (gameInfo.isSimultaneousTurnsMode()) finishSimultaneousTurn()
        else finishPollingTurn()
    }

    /** Marks the current player as done and either passes to the next player or advances the turn. */
    private fun finishPollingTurn() {
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

    /** The last ready player settles the turn by replaying the authoritative real-time action log. */
    private fun finishSimultaneousTurn() {
        if (!simultaneousTurnOperationsRestored || realtimeTurnReplayBlocked || realtimeTurnRecoveryRequired || isNextTurnUpdateRunning()) return
        isPlayersTurn = false
        shouldUpdate = true
        val progressBar = NextTurnProgress(nextTurnButton)
        progressBar.start(this)

        nextTurnUpdateJob = Concurrency.runOnNonDaemonThreadPool("SimultaneousPass") {
            val submittedTurn = gameInfo.turns
            var submitted = simultaneousTurnDoneUploadedForTurn == submittedTurn
            try {
                if (!submitted) {
                    checkForUnrecordedSimultaneousTurnChanges()
                    if (!simultaneousTurnOperationUploader.flushPending() || realtimeTurnRecoveryRequired || realtimeTurnReplayBlocked ||
                        game.worldScreen !== this@WorldScreen || game.gameInfo !== gameInfo) return@runOnNonDaemonThreadPool
                    if (simultaneousTurnReadyClientSequence < 0)
                        simultaneousTurnReadyClientSequence = gameInfo.nextSimultaneousOperationSequence++
                    game.onlineMultiplayer.multiplayerServer.markRealtimeTurnReady(
                        gameInfo.gameId, submittedTurn, simultaneousTurnReadyClientSequence
                    ) ?: throw Exception("The multiplayer server does not support real-time simultaneous turns")
                    simultaneousTurnDoneUploadedForTurn = submittedTurn
                    submitted = true
                }

                val server = game.onlineMultiplayer.multiplayerServer
                val feed = server.downloadRealtimeTurnActions(gameInfo.gameId, submittedTurn, 0)
                    ?: throw Exception("The multiplayer server does not support real-time simultaneous turns")
                if (!feed.allPlayersReady) {
                    progressBar.increment()
                    launchOnGLThread { nextTurnButton.update() }
                    return@runOnNonDaemonThreadPool
                }
                val lease = server.acquireSimultaneousTurnSettlementLock(gameInfo.gameId, submittedTurn)
                if (lease == null) {
                    val preview = server.tryDownloadGamePreview(gameInfo.gameId)
                    if (preview.turns > submittedTurn && game.gameInfo == gameInfo) {
                        val latest = server.downloadGame(gameInfo.gameId)
                        if (latest.turns > submittedTurn && game.gameInfo == gameInfo)
                            launchOnGLThread { startNewScreenJob(latest, autoPlay) }
                    }
                    launchOnGLThread { nextTurnButton.update() }
                    return@runOnNonDaemonThreadPool
                }

                // Keep the lock alive: rebuilding and advancing a large turn can exceed the server's
                // staleness window, and another client must not take settlement over midway.
                val lockRenewalJob = startSimultaneousTurnLockRenewal(gameInfo.gameId, lease)
                try {
                    val turnStart = server.downloadGame(gameInfo.gameId)
                    if (turnStart.turns != submittedTurn) {
                        if (turnStart.turns > submittedTurn && game.gameInfo == gameInfo)
                            launchOnGLThread { startNewScreenJob(turnStart, autoPlay) }
                        return@runOnNonDaemonThreadPool
                    }
                    val settlementFeed = server.downloadRealtimeTurnActions(gameInfo.gameId, submittedTurn, 0)
                        ?: throw Exception("The multiplayer server does not support real-time simultaneous turns")
                    if (!settlementFeed.allPlayersReady)
                        return@runOnNonDaemonThreadPool
                    val turnOperations = settlementFeed.actions
                        .filter { it.kind == "action" }
                        .flatMap { it.operations }
                    val failedOperations = SimultaneousTurnReplay.replayOrExplain(turnStart, turnOperations)
                    if (failedOperations.isNotEmpty()) {
                        debug(
                            "Simultaneous turn settlement blocked by %d un-replayable operations (game %s, turn %d): %s",
                            failedOperations.size, gameInfo.gameId, turnStart.turns,
                            failedOperations.joinToString {
                                "${it.operation.playerId}/${it.operation.sequence}:${it.operation.type}"
                            }
                        )
                        launchOnGLThread {
                            realtimeTurnReplayBlocked = true
                            shouldUpdate = true
                            ToastPopup(
                                "Some accepted actions could not be applied. Turn settlement is blocked.".tr(),
                                this@WorldScreen, 6000
                            )
                        }
                        return@runOnNonDaemonThreadPool
                    }
                    if (!server.renewSimultaneousTurnSettlementLock(gameInfo.gameId, lease))
                        return@runOnNonDaemonThreadPool
                    turnStart.nextTurnPolling(progressBar)
                    if (!game.onlineMultiplayer.commitSimultaneousTurn(turnStart, lease))
                        return@runOnNonDaemonThreadPool
                    turnStart.isUpToDate = true
                    val skippedNames = turnStart.civilizations
                        .filter { it.playerId in settlementFeed.timedOutPlayerIds && !it.isSpectator }
                        .joinToString(", ") { it.civName }
                    if (skippedNames.isNotEmpty())
                        launchOnGLThread {
                            ToastPopup(
                                "Timed out waiting for [$skippedNames] - their turn was skipped".tr(),
                                this@WorldScreen, 4000
                            )
                        }
                    if (game.gameInfo == gameInfo)
                        launchOnGLThread { startNewScreenJob(turnStart, autoPlay) }
                } finally {
                    lockRenewalJob.cancel()
                    server.releaseSimultaneousTurnSettlementLock(gameInfo.gameId, lease)
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                if (realtimeTurnRecoveryRequired || game.worldScreen !== this@WorldScreen) return@runOnNonDaemonThreadPool
                if (!submitted) {
                    val serverReady = try {
                        val feed = game.onlineMultiplayer.multiplayerServer
                            .downloadRealtimeTurnActions(gameInfo.gameId, submittedTurn, 0)
                        feed != null && (viewingCiv.playerId in feed.readyPlayerIds ||
                            viewingCiv.playerId in feed.timedOutPlayerIds)
                    } catch (_: Exception) {
                        false
                    }
                    if (serverReady) {
                        simultaneousTurnDoneUploadedForTurn = submittedTurn
                        submitted = true
                    }
                }
                val retryLocally = !submitted
                debug("Simultaneous turn pass failed (game %s, turn %d): %s", gameInfo.gameId, submittedTurn, ex)
                launchOnGLThread {
                    if (realtimeTurnRecoveryRequired || game.worldScreen !== this@WorldScreen) return@launchOnGLThread
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
     * Returns a message naming the player whose order already touches one of [keys] this turn, or
     * null when the action may proceed.
     *
     * The claim is read on this client from the operations relayed for the turn: the server only
     * stores and forwards them, and settlement replays the same list, so both players agree on who
     * loses a contested target without anyone arbitrating it. A player who has not uploaded yet is
     * invisible here, which is why this is only a courtesy - settlement still resolves the conflict.
     */
    fun simultaneousTurnTargetsClaimedBy(keys: List<String>): String? {
        if (!gameInfo.isSimultaneousTurnsMode()) return null
        val claimant = simultaneousTurnClaimantOfKeys(keys) ?: return null
        debug("Simultaneous-turn targets %s are already claimed by %s", keys, claimant)
        return "That target is already claimed by [$claimant] this turn".tr()
    }

    /**
     * Runs [action] unless another player's order already touches one of the [keys] it is about to
     * touch, in which case the player is told who got there first instead.
     *
     * The claim is local state, so unlike the network round trip this replaces, [action] may run
     * inline; only the toast is posted, because callers may be background coroutines.
     */
    fun runIfSimultaneousTurnTargetsAreFree(keys: List<String>, action: () -> Unit) {
        val claimedBy = simultaneousTurnTargetsClaimedBy(keys)
        if (claimedBy == null) action()
        else Gdx.app.postRunnable { ToastPopup(claimedBy, this@WorldScreen, 3000) }
    }

    /**
     * Keeps the settlement lock alive while a long turn is being rebuilt and advanced, so that
     * another client does not take the settlement over midway. A renewal that fails means the lock is
     * gone (or unreachable) and ends the loop. The final write checks the lease again on the server.
     */
    private fun startSimultaneousTurnLockRenewal(gameId: String, lease: String): Job {
        return Concurrency.run("SimultaneousTurnLockRenewal") {
            while (isActive) {
                delay(30_000)
                if (!game.onlineMultiplayer.multiplayerServer.renewSimultaneousTurnSettlementLock(gameId, lease))
                    return@run
            }
        }
    }

    /** Periodically checks completion markers as a fallback for unavailable WebSocket signals. */
    private fun startSimultaneousTurnWatcher() {
        simultaneousTurnWatcherJob?.cancel()
        simultaneousTurnWatcherJob = Concurrency.run("SimultaneousTurnWatcher") {
            while (isActive) {
                delay(2000)
                if (!gameInfo.isSimultaneousTurnsMode() || !simultaneousTurnOperationsRestored) continue
                syncRealtimeTurnActions()
            }
        }
    }

    /**
     * Periodically derives, from the operations relayed for this turn, which targets other players
     * already gave an order for, so the map can mark them. A claim the player cannot see looks like a
     * bug when an order on that target is refused, so the marks come from what the players themselves
     * uploaded - not from a server verdict, because the server only stores and forwards operations.
     * Claims change rarely, so a slow poll is enough, and it runs during this player's turn as well -
     * that is exactly when the marks are needed.
     */
    private fun startSimultaneousTurnClaimWatcher() {
        simultaneousTurnClaimWatcherJob?.cancel()
        simultaneousTurnClaimWatcherJob = Concurrency.run("SimultaneousTurnClaimWatcher") {
            while (isActive) {
                delay(3000)
                if (!gameInfo.isSimultaneousTurnsMode()) continue
                val playerId = viewingCiv.playerId
                if (playerId.isEmpty()) continue
                val turn = gameInfo.turns
                val claims = try {
                    val feed = game.onlineMultiplayer.multiplayerServer
                        .downloadRealtimeTurnActions(gameInfo.gameId, turn, 0)
                        ?: throw Exception("The multiplayer server does not support real-time simultaneous turns")
                    SimultaneousTurnClaims.claimsByOthers(feed.actions, playerId)
                } catch (ex: Exception) {
                    debug("Could not read real-time turn actions (game %s, turn %d): %s", gameInfo.gameId, turn, ex)
                    continue
                }
                launchOnGLThread {
                    if (claims == simultaneousTurnClaimsByOthers && turn == simultaneousTurnClaimsTurn)
                        return@launchOnGLThread
                    simultaneousTurnClaimsByOthers = claims
                    simultaneousTurnClaimsTurn = turn
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
        if (!gameInfo.isSimultaneousTurnsMode() || simultaneousTurnOperationsRestored) return
        val playerId = viewingCiv.playerId
        if (playerId.isEmpty() || viewingCiv.isSpectator) return
        val turn = gameInfo.turns
        simultaneousTurnRestoreJob = Concurrency.run("RestoreSimultaneousTurn") {
            var legacyRejectedSequence: Long? = null
            var recoveryHadConflicts = false
            while (isActive && !simultaneousTurnOperationsRestored) {
                val restored = try {
                    val server = game.onlineMultiplayer.multiplayerServer
                    val initialFeed = server.downloadRealtimeTurnActions(gameInfo.gameId, turn, 0)
                        ?: throw Exception("The multiplayer server does not support real-time simultaneous turns")
                    val storedActions = initialFeed.actions.filter { it.kind == "action" }
                    val storedOperations = storedActions.flatMap { it.operations }
                    val legacyOperations = try {
                        server.downloadSimultaneousTurnOperations(gameInfo.gameId)
                    } catch (_: Exception) {
                        emptyList()
                    }
                    val storedBySequence = storedOperations.filter { it.playerId == playerId }.associateBy { it.sequence }
                    val legacyMine = legacyOperations.filter { it.playerId == playerId && it.turn == turn && it.type != "done" }
                        .sortedBy { it.sequence }
                    for (operation in legacyMine) {
                        if (legacyRejectedSequence != null) break
                        if (playerId in initialFeed.readyPlayerIds || playerId in initialFeed.timedOutPlayerIds) break
                        val accepted = storedBySequence[operation.sequence]
                        if (accepted != null) {
                            if (accepted.sameAction(operation)) continue
                            legacyRejectedSequence = operation.sequence
                            recoveryHadConflicts = true
                            break
                        }
                        try {
                            uploadRealtimeTurnOperation(operation)
                        } catch (_: SimultaneousTurnOperationRejectedException) {
                            legacyRejectedSequence = operation.sequence
                            recoveryHadConflicts = true
                            break
                        }
                    }
                    if (legacyRejectedSequence != null) simultaneousTurnOutbox.discardFrom(legacyRejectedSequence)
                    val currentFeed = server.downloadRealtimeTurnActions(gameInfo.gameId, turn, 0)
                        ?: throw Exception("The multiplayer server does not support real-time simultaneous turns")
                    val acceptedOperations = currentFeed.actions.filter { it.kind == "action" }.flatMap { it.operations }
                    val currentPlayerReady = playerId in currentFeed.readyPlayerIds || playerId in currentFeed.timedOutPlayerIds
                    val recovered = simultaneousTurnOutbox.recover(acceptedOperations, currentPlayerReady, ::uploadRealtimeTurnOperation)
                    recoveryHadConflicts = recoveryHadConflicts || recovered.hasConflicts
                    if (legacyOperations.any { it.playerId == playerId && it.turn == turn && it.type == "done" }
                        && !currentPlayerReady && !recoveryHadConflicts) {
                        val clientSequence = maxOf(
                            gameInfo.nextSimultaneousOperationSequence,
                            (legacyMine.maxOfOrNull { it.sequence } ?: 0L) + 1,
                            (recovered.operations.maxOfOrNull { it.sequence } ?: 0L) + 1
                        )
                        checkNotNull(server.markRealtimeTurnReady(gameInfo.gameId, turn, clientSequence)) {
                            "The multiplayer server does not support real-time simultaneous turns"
                        }
                        gameInfo.nextSimultaneousOperationSequence = clientSequence + 1
                    }
                    val finalFeed = server.downloadRealtimeTurnActions(gameInfo.gameId, turn, 0)
                        ?: throw Exception("The multiplayer server does not support real-time simultaneous turns")
                    finalFeed
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Exception) {
                    debug("Could not restore real-time turn actions (game %s, turn %d): %s", gameInfo.gameId, turn, ex)
                    delay(2000)
                    continue
                }
                if (gameInfo.isUpToDate) simultaneousTurnOutbox.discardOlderTurns()
                launchOnGLThread {
                    if (!gameInfo.isSimultaneousTurnsMode() || gameInfo.turns != turn
                        || simultaneousTurnOperationsRestored || game.worldScreen != this@WorldScreen) return@launchOnGLThread
                    applyRealtimeTurnFeed(restored, initialRestore = true)
                    gameInfo.nextSimultaneousOperationSequence = maxOf(
                        gameInfo.nextSimultaneousOperationSequence,
                        (getSimultaneousTurnOperations().maxOfOrNull { it.sequence } ?: 0L) + 1
                    )
                    simultaneousTurnOperationsRestored = true
                    shouldUpdate = true
                    if (recoveryHadConflicts && !realtimeTurnReplayBlocked)
                        ToastPopup("Some actions could not be restored.".tr(), this@WorldScreen, 5000)
                    if (restored.allPlayersReady && !realtimeTurnReplayBlocked)
                        finishSimultaneousTurn()
                }
                return@run
            }
        }
    }

    private fun syncRealtimeTurnActions() {
        if (!gameInfo.isSimultaneousTurnsMode() || !simultaneousTurnOperationsRestored || viewingCiv.isSpectator ||
            realtimeTurnRecoveryRequired || realtimeTurnReplayBlocked)
            return
        if (!realtimeTurnSyncMutex.tryLock()) return
        realtimeTurnSyncJob = Concurrency.run("SyncRealtimeTurnActions") {
            try {
                while (isActive && gameInfo.isSimultaneousTurnsMode()) {
                    val turn = gameInfo.turns
                    val feed = game.onlineMultiplayer.multiplayerServer
                        .downloadRealtimeTurnActions(gameInfo.gameId, turn, lastRealtimeTurnServerSequence)
                        ?: throw Exception("The multiplayer server does not support real-time simultaneous turns")
                    val applied = CompletableDeferred<Unit>()
                    launchOnGLThread {
                        try {
                            if (game.worldScreen === this@WorldScreen && gameInfo.turns == turn && !realtimeTurnRecoveryRequired) {
                                applyRealtimeTurnFeed(feed, initialRestore = false)
                                shouldUpdate = true
                                if (feed.allPlayersReady && !isPlayersTurn && !realtimeTurnReplayBlocked)
                                    finishSimultaneousTurn()
                            }
                        } finally {
                            applied.complete(Unit)
                        }
                    }
                    applied.await()
                    if (realtimeTurnReplayBlocked || realtimeTurnRecoveryRequired || game.worldScreen !== this@WorldScreen ||
                        feed.latestSequence <= lastRealtimeTurnServerSequence) break
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                debug("Could not sync real-time turn actions (game %s, turn %d): %s", gameInfo.gameId, gameInfo.turns, ex)
            } finally {
                realtimeTurnSyncMutex.unlock()
            }
        }
    }

    private fun applyRealtimeTurnFeed(feed: com.unciv.logic.multiplayer.RealtimeTurnActionFeed, initialRestore: Boolean) {
        if (feed.turn != gameInfo.turns || realtimeTurnReplayBlocked) return
        val playerId = viewingCiv.playerId
        val ownOperations = ArrayList<SimultaneousTurnOperation>()
        val failures = ArrayList<SimultaneousTurnFailedOperation>()
        actionLoop@ for (action in feed.actions.sortedBy { it.sequence }) {
            if (action.sequence <= lastRealtimeTurnServerSequence) continue
            if (action.kind == "action") {
                for (operation in action.operations) {
                    if (operation.turn != feed.turn) {
                        failures += SimultaneousTurnFailedOperation(
                            operation, SimultaneousTurnFailureReason.Unreadable
                        )
                        break@actionLoop
                    }
                    if (operation.playerId == playerId) ownOperations += operation
                    val alreadyAppliedLocally = operation.playerId == playerId &&
                        operation.sequence in simultaneousTurnLocalOperationSequences
                    if (alreadyAppliedLocally) continue
                    val reason = SimultaneousTurnReplay.applyOrExplain(gameInfo, operation)
                    if (reason != null) {
                        failures += SimultaneousTurnFailedOperation(operation, reason)
                        break@actionLoop
                    }
                    if (operation.playerId == playerId)
                        simultaneousTurnLocalOperationSequences += operation.sequence
                }
            }
            lastRealtimeTurnServerSequence = maxOf(lastRealtimeTurnServerSequence, action.sequence)
        }
        synchronized(simultaneousTurnOperationsLock) {
            val existing = simultaneousTurnOperations.toList()
            simultaneousTurnOperations.clear()
            simultaneousTurnOperations.addAll(
                SimultaneousTurnOperations.merge(existing, ownOperations)
            )
        }
        if (ownOperations.isNotEmpty()) {
            gameInfo.nextSimultaneousOperationSequence = maxOf(
                gameInfo.nextSimultaneousOperationSequence,
                (ownOperations.maxOf { it.sequence }) + 1
            )
        }
        if (playerId in feed.readyPlayerIds || playerId in feed.timedOutPlayerIds) {
            simultaneousTurnDoneUploadedForTurn = feed.turn
            isPlayersTurn = false
        } else if (initialRestore) {
            isPlayersTurn = true
        }
        if (failures.isNotEmpty()) {
            realtimeTurnReplayBlocked = true
            isPlayersTurn = false
            Log.error(
                "Could not apply %d accepted real-time turn actions (game %s, turn %d): %s",
                failures.size, gameInfo.gameId, feed.turn,
                failures.joinToString { "${it.operation.playerId}/${it.operation.sequence}:${it.operation.type}" }
            )
            ToastPopup("Some accepted actions could not be applied. Turn settlement is blocked.".tr(), this, 6000)
        }
    }

    private suspend fun recoverRejectedRealtimeTurnOperation(operation: SimultaneousTurnOperation, cause: Exception) {
        // Freeze this screen before pruning the journal or replacing the game. A concurrent submit
        // must see a rejected flush even if it was waiting while the uploader rebuilt the state.
        realtimeTurnRecoveryRequired = true
        try {
            synchronized(simultaneousTurnOperationsLock) {
                simultaneousTurnOutbox.discardFrom(operation.sequence)
            }
            withGLContext {
                if (game.worldScreen === this@WorldScreen) {
                    isPlayersTurn = false
                    shouldUpdate = true
                    ToastPopup("An action conflicted with another player and the game is resynchronizing.".tr(), this@WorldScreen, 5000)
                }
            }
            debug("Discarded simultaneous-turn operations from sequence %d after rejection: %s", operation.sequence, cause)
            val server = game.onlineMultiplayer.multiplayerServer
            val authoritative = server.downloadGame(gameInfo.gameId)
            if (game.worldScreen !== this || game.gameInfo !== gameInfo) return
            // The new screen restores the accepted feed from the turn-start save exactly once.
            authoritative.isUpToDate = true
            game.loadGame(authoritative, autoPlay)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            debug("Could not rebuild real-time turn state after a conflict: %s", ex)
            Gdx.app.postRunnable {
                if (game.worldScreen !== this@WorldScreen) return@postRunnable
                realtimeTurnReplayBlocked = true
                shouldUpdate = true
                ToastPopup("The turn could not be resynchronized. Do not continue playing.".tr(), this@WorldScreen, 6000)
            }
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

    /** The unit [cycleThroughDueUnits] returned last - the next search starts after it */
    private var lastCycledUnit: MapUnitView? = null

    /** Returns the next due unit, but preferably not [unitToSkip]: this is returned only if it is the only remaining due unit
     *  (not with the alternate cycle order, which never returns it). */
    private fun cycleThroughDueUnits(unitToSkip: MapUnitView?): MapUnitView? {
        val civView = selectedGameView.getCivView(viewingCiv)
        val nextDueUnit = if (GUI.getSettings().alternateUnitCycleOrder) {
            val center = unitToSkip?.getTile()
                ?: civView.getCapital()?.getCenterTile()
                ?: selectedGameView.tileMapView.getTile(HexCoord.Zero)
            val dueUnits = civView.getUnits().filter { it.due && it.isIdle() && it != unitToSkip }
            if (center == null) dueUnits.firstOrNull()
            else dueUnits.minByOrNull { it.getTile().aerialDistanceTo(center) }
        } else {
            // Search starts right after the unit we last cycled to
            val units = civView.getUnits()
            val start = units.indexOf(lastCycledUnit) + 1 // not found: 0
            val dueUnits = (units.drop(start) + units.take(start)).filter { it.due && it.isIdle() }
            dueUnits.firstOrNull { it != unitToSkip } ?: dueUnits.firstOrNull()
        }
        if (nextDueUnit != null) lastCycledUnit = nextDueUnit
        return nextDueUnit
    }

    fun switchToNextUnit(resetDue: Boolean = true) {
        // Try to select something new if we already have the next pending unit selected.
        if (bottomUnitTable.selectedUnit != null && resetDue)
            bottomUnitTable.selectedUnit!!.setDue(false)
        val nextDueUnit = cycleThroughDueUnits(bottomUnitTable.selectedUnit)
        if (nextDueUnit != null) {
            mapHolder.setCenterPosition(
                nextDueUnit.getTile().position(),
                immediately = false,
                selectUnit = false
            )
            bottomUnitTable.selectUnit(nextDueUnit)
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
            if (game.worldScreen !== this@WorldScreen || realtimeTurnRecoveryRequired || realtimeTurnReplayBlocked) return@timer
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
        displayTutorial(TutorialTrigger.NaturalWonders) { viewingCiv.naturalWonders.isNotEmpty() }
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
    settlementFailures: List<SimultaneousTurnFailedOperation> = emptyList()
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
        // Lists the actions settlement refused instead of dropping them silently, and says how many
        // there were before the list is expanded. See docs_plan/simultaneous-turns-v1-action-inventory.md §7.5.
        if (settlementFailures.isNotEmpty()) {
            withGLContext {
                val report = SimultaneousTurnOperations
                    .describeFailedSimultaneousTurnOperations(gameInfo, settlementFailures)
                    ?: return@withGLContext
                val expander = ExpanderTab(
                    "Some actions could not be applied".tr() + " (${settlementFailures.size})",
                    startsOutOpened = true
                )
                val reportLabel = report.toLabel()
                reportLabel.wrap = true
                expander.innerTable.add(reportLabel).width(newWorldScreen.stage.width / 2)
                Popup(newWorldScreen)
                    .apply {
                        add(expander).row()
                        addCloseButton()
                    }
                    .open()
            }
        }
    }
}
