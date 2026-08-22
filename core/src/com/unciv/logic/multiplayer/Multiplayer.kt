package com.unciv.logic.multiplayer

import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.GameInfoPreview
import com.unciv.logic.GameStarter
import com.unciv.logic.automation.civilization.NextTurnAutomation
import com.unciv.logic.civilization.NotificationCategory
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.event.EventBus
import com.unciv.logic.multiplayer.chat.ChatStore
import com.unciv.logic.multiplayer.storage.FileStorageRateLimitReached
import com.unciv.logic.multiplayer.storage.MultiplayerAuthException
import com.unciv.logic.multiplayer.storage.MultiplayerFileNotFoundException
import com.unciv.logic.multiplayer.storage.MultiplayerServer
import com.unciv.models.metadata.GameParameters
import com.unciv.models.metadata.GameSetupInfo
import com.unciv.models.metadata.GameSettings
import com.unciv.ui.components.extensions.isLargerThan
import com.unciv.utils.Concurrency
import com.unciv.utils.Dispatcher
import com.unciv.utils.Log
import com.unciv.utils.debug
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import org.jetbrains.annotations.VisibleForTesting
import yairm210.purity.annotations.Readonly
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * The local snapshot to resume from when entering a "forbid reload" game, or null to use the server state.
 *
 * Pure decision (no file/state access) kept public so it can be unit-tested - see LocalSnapshotDecisionTest.
 * A snapshot only replaces the server state when:
 * - the game is an online multiplayer game with the [GameParameters.forbidReload] option set,
 * - this is a game *entry*, not an in-game sync (the same game is not already open locally),
 * - a local snapshot exists and is from the same turn and current player as the server state - then it is
 *   at least as new as the server state, because the server only receives the turn-start upload while the
 *   player plays the turn locally. This is what makes quit-and-reload unable to undo the turn.
 */
@VisibleForTesting
fun resolveLocalSnapshotForEntry(
    serverGame: GameInfo,
    currentGameId: String?,
    localSnapshot: GameInfo?
): GameInfo? {
    if (!serverGame.gameParameters.isOnlineMultiplayer || !serverGame.gameParameters.forbidReload) return null
    if (currentGameId == serverGame.gameId) return null // in-game update, keep server state
    val snapshot = localSnapshot ?: return null
    if (snapshot.turns != serverGame.turns || snapshot.currentPlayer != serverGame.currentPlayer) return null
    snapshot.isUpToDate = true
    return snapshot
}


/**
 * How often files can be checked for new multiplayer games (could be that the user modified their file system directly). More checks within this time period
 * will do nothing.
 */
private val FILE_UPDATE_THROTTLE_PERIOD = Duration.ofSeconds(60)

/** How long a restart-executing client may stay silent before others take over. */
private const val RESTART_TAKEOVER_TIMEOUT_MS = 2 * 60 * 1000L

/**
 * Provides *online* multiplayer functionality to the rest of the game.
 * Multiplayer data is a mix of local files ([multiplayerFiles]) and server data ([multiplayerServer]).
 * This class handles functions that require a mix of both.
 *
 * See the file of [com.unciv.logic.multiplayer.HasMultiplayerGameName] for all available [EventBus] events.
 */
class Multiplayer {
    /** Handles SERVER DATA only */
    val multiplayerServer = MultiplayerServer()
    /** Handles LOCAL FILES only */
    val multiplayerFiles = MultiplayerFiles()


    private val lastFileUpdate: AtomicReference<Instant?> = AtomicReference()
    private val lastAllGamesRefresh: AtomicReference<Instant?> = AtomicReference()
    private val lastCurGameRefresh: AtomicReference<Instant?> = AtomicReference()

    val games: Set<MultiplayerGamePreview> get() = multiplayerFiles.savedGames.values.toSet()
    val multiplayerGameUpdater: Job

    private val events = EventBus.EventReceiver()

    init {
        /** WebSocket chat signals announce restart vote state changes; re-read the authoritative
         *  server files on receipt (our own signals loop back, so polling players get them too). */
        events.receive(RestartVoteSignalReceived::class) { signal ->
            Concurrency.run("RefreshRestartVote") {
                refreshRestartVote(signal.gameId)
            }
        }

        /** We have 2 'async processes' that update the multiplayer games:
         * A. This one, which as part of *this process* runs refreshes for all OS's
         * B. MultiplayerTurnCheckWorker, which *as an Android worker* runs refreshes *even when the game is closed*.
         *    Only for Android, obviously
         */
        multiplayerGameUpdater = flow<Unit> {
            while (true) {
                delay(500)
                if (!currentCoroutineContext().isActive) return@flow
                val multiplayerSettings: GameSettings.GameSettingsMultiplayer
                try { // Fails in unknown cases - cannot debug :/ This is just so it doesn't appear in GP analytics
                    multiplayerSettings = UncivGame.Current.settings.multiplayer
                } catch (ex:Exception){ continue }

                val currentGame = getCurrentGame()
                val preview = currentGame?.preview
                if (currentGame != null && (usesCustomServer() || preview == null || (!preview.isUsersTurn() && preview.gameParameters.simultaneousTurns != true))) {
                    throttle(lastCurGameRefresh, multiplayerSettings.currentGameRefreshDelay, {}, {}) { currentGame.requestUpdate() }
                }

                if (currentGame != null && preview != null) {
                    // Keep the restart vote state fresh: also settles timed-out votes and picks up
                    // finished restarts. WebSocket signals just accelerate this - polling is the fallback.
                    throttle(lastRestartVoteRefresh, multiplayerSettings.currentGameRefreshDelay, {}, {}) {
                        refreshRestartVote(preview.gameId)
                    }
                }

                val doNotUpdate = if (currentGame == null) listOf() else listOf(currentGame)
                throttle(lastAllGamesRefresh, multiplayerSettings.allGameRefreshDelay, {}, {}) { requestUpdate(doNotUpdate = doNotUpdate) }
            }
        }.launchIn(CoroutineScope(Dispatcher.DAEMON))
    }

    @Readonly
    private fun getCurrentGame(): MultiplayerGamePreview? {
        val gameInfo = UncivGame.Current.gameInfo
        return if (gameInfo != null && gameInfo.gameParameters.isOnlineMultiplayer) {
            multiplayerFiles.getGameByGameId(gameInfo.gameId)
        } else null
    }

    /**
     * Requests an update of all multiplayer game state. Does automatic throttling to try to prevent hitting rate limits.
     *
     * Use [forceUpdate] = true to circumvent this throttling.
     *
     * Fires: [MultiplayerGameUpdateStarted], [MultiplayerGameUpdated], [MultiplayerGameUpdateUnchanged], [MultiplayerGameUpdateFailed]
     */
    suspend fun requestUpdate(forceUpdate: Boolean = false, doNotUpdate: List<MultiplayerGamePreview> = listOf()) {
        val fileThrottleInterval = if (forceUpdate) Duration.ZERO else FILE_UPDATE_THROTTLE_PERIOD
        // An exception only happens muhere if the files can't be listed, should basically never happen
        throttle(lastFileUpdate, fileThrottleInterval, {}, {}, action = {multiplayerFiles.updateSavesFromFiles()})

        for (game in multiplayerFiles.savedGames.values.toList()) { // since updates are long, .toList for immutability
            if (game in doNotUpdate) continue
            // Any games that haven't been updated in 2 weeks (!) are inactive, don't waste your time
            if (Duration.between(Instant.ofEpochMilli(game.fileHandle.lastModified()), Instant.now())
                .isLargerThan(Duration.ofDays(14))) continue
            game.requestUpdate(forceUpdate) // DO NOT spawn in thread, since that leads to OOMs when many games try at once
        }
    }


    /**
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     */
    suspend fun createGame(newGame: GameInfo) {
        multiplayerServer.uploadGame(newGame, withPreview = true)
        multiplayerFiles.addGame(newGame)
    }

    /**
     * @param gameName if this is null or blank, will use the gameId as the game name
     * @return the final name the game was added under
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws MultiplayerFileNotFoundException if the file can't be found
     */
    suspend fun addGame(gameId: String, gameName: String? = null) {
        val saveFileName = if (gameName.isNullOrBlank()) gameId else gameName
        val gamePreview: GameInfoPreview = try {
            multiplayerServer.tryDownloadGamePreview(gameId)
        } catch (_: MultiplayerFileNotFoundException) {
            // Game is so old that a preview could not be found on dropbox lets try the real gameInfo instead
            multiplayerServer.tryDownloadGame(gameId).asPreview()
        }
        multiplayerFiles.addGame(gamePreview, saveFileName)
    }


    /**
     * Resigns from the given multiplayer [game]. Can only resign if it's currently the user's turn,
     * to ensure that no one else can upload the game in the meantime.
     *
     * Fires [MultiplayerGameUpdated]
     *
     * @param responsibleCivNameOrPlayerId Who caused the player to resign? Can be the name of a civ, or for example a player id
     *
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws MultiplayerFileNotFoundException if the file can't be found
     * @throws MultiplayerAuthException if the authentication failed
     * @return false if it's not the user's turn and thus resigning did not happen
     */
    suspend fun resignPlayer(game: MultiplayerGamePreview, playerCivName: String, responsibleCivNameOrPlayerId: String): String {
        val preview = game.preview ?: throw game.error!!
        // download to work with the latest game state
        val gameInfo = multiplayerServer.tryDownloadGame(preview.gameId)

        if (gameInfo.currentPlayer != preview.currentPlayer) {
            game.updatePreview(gameInfo.asPreview())
            return "Game was out of sync with server - updated"
        }

        val playerCiv = gameInfo.getCivilization(playerCivName)

        //Set civ info to AI
        playerCiv.playerType = PlayerType.AI
        playerCiv.playerId = ""

        //call next turn so turn gets simulated by AI
        if (gameInfo.currentPlayer == playerCivName) gameInfo.nextTurn()

        //Add notification so everyone knows what happened
        //call for every civ cause AI players are skipped anyway

        val notificationText = if (responsibleCivNameOrPlayerId == playerCivName || responsibleCivNameOrPlayerId.isEmpty()) {
            "[$playerCivName] resigned and is now controlled by AI"
        } else {
            "[$playerCivName] was forcibly resigned by [$responsibleCivNameOrPlayerId] and is now controlled by AI"
        }

        for (civ in gameInfo.civilizations)
            civ.addNotification(notificationText, NotificationCategory.General, playerCivName)

        multiplayerServer.uploadGame(gameInfo, withPreview = true)
        game.updatePreview(gameInfo.asPreview())
        return ""
    }

    /**
     * Returns false if game was not up to date
     * Returned value indicates an error string - will be null if successful
     * We always pass in the player name to ensure if the button was clicked twice we don't skip 2 turns
     *
     * @param responsibleCivNameOrPlayerId Who skipped the player's turn? Can be the name of a civ, or for example a player id
     */
    suspend fun skipCurrentPlayerTurn(game: MultiplayerGamePreview, playerCivName: String, responsibleCivNameOrPlayerId: String): String? {
        val preview = game.preview ?: return game.error!!.message
        // download to work with the latest game state
        val gameInfo: GameInfo
        try {
            gameInfo = multiplayerServer.tryDownloadGame(preview.gameId)
        }
        catch (ex: Exception){
            return ex.message
        }

        if (gameInfo.currentPlayer != preview.currentPlayer) {
            game.updatePreview(gameInfo.asPreview())
            return "The game was out of sync with the server"
        }

        if (gameInfo.currentPlayer != playerCivName) {
            return "Could not skip turn - current player is [${gameInfo.currentPlayer}], not [$playerCivName]"
        }

        val playerCiv = gameInfo.getCurrentPlayerCivilization()
        NextTurnAutomation.automateCivMoves(playerCiv, false)
        gameInfo.nextTurn()

        //Add notification so everyone knows what happened
        //call for every civ cause AI players are skipped anyway

        val notificationText = if (responsibleCivNameOrPlayerId == playerCivName || responsibleCivNameOrPlayerId.isEmpty()) {
            "[$playerCivName] skipped their own turn"
        } else {
            "[$playerCivName]'s turn was skipped by [$responsibleCivNameOrPlayerId]"
        }

        for (civ in gameInfo.civilizations)
            civ.addNotification(notificationText, NotificationCategory.General, playerCiv.civName)

        multiplayerServer.uploadGame(gameInfo, withPreview = true)
        game.updatePreview(gameInfo.asPreview())
        return null
    }

    //region Restart vote

    private val restartVoteCache = ConcurrentHashMap<String, RestartVote>()
    private val lastRestartVoteRefresh = AtomicReference<Instant?>()

    /** Current cached restart vote state for [gameId], refreshed by polling and WebSocket signals. */
    @Readonly
    fun getCachedRestartVote(gameId: String): RestartVote? = restartVoteCache[gameId]

    private fun restartVoteFileName(gameId: String) = "${gameId}_restartvote"
    private fun restartMarkerFileName(gameId: String) = "${gameId}_restart"

    /** Downloads the restart vote for [gameId]; null when no vote exists. */
    suspend fun fetchRestartVote(gameId: String): RestartVote? {
        return try {
            val data = multiplayerServer.fileStorage().loadFileData(restartVoteFileName(gameId))
            json().fromJson(RestartVote::class.java, data)
        } catch (_: MultiplayerFileNotFoundException) {
            null
        }
    }

    private suspend fun saveRestartVote(gameId: String, vote: RestartVote) {
        multiplayerServer.fileStorage().saveFileData(restartVoteFileName(gameId), json().toJson(vote))
    }

    /** Downloads the restart marker for [gameId]; null when no restart is in progress. */
    suspend fun fetchRestartMarker(gameId: String): RestartMarker? {
        return try {
            val data = multiplayerServer.fileStorage().loadFileData(restartMarkerFileName(gameId))
            json().fromJson(RestartMarker::class.java, data)
        } catch (_: MultiplayerFileNotFoundException) {
            null
        }
    }

    private suspend fun saveRestartMarker(gameId: String, marker: RestartMarker) {
        multiplayerServer.fileStorage().saveFileData(restartMarkerFileName(gameId), json().toJson(marker))
    }

    /**
     * Downloads the restart vote for [gameId], settles it when possible, updates the cache and fires
     * [RestartVoteUpdated] when the state changed. Also picks up finished restarts.
     * Serves as both the polling fallback and the receiver-side handler for WebSocket vote signals.
     * Never throws: it runs on the background updater and a corrupt vote file must not kill the
     * polling loop (nor trigger the crash screen via the WebSocket signal path).
     */
    suspend fun refreshRestartVote(gameId: String) {
        try {
            refreshRestartVoteInternal(gameId)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            debug("Restart vote refresh failed for %s: %s", gameId, ex.message)
        }
    }

    private suspend fun refreshRestartVoteInternal(gameId: String) {
        val vote = fetchRestartVote(gameId)
        val old = restartVoteCache[gameId]
        if (vote == null) {
            if (old != null) {
                restartVoteCache.remove(gameId)
                withContext(Dispatcher.GL) { EventBus.send(RestartVoteUpdated(gameId)) }
            }
            return
        }
        val settledNow = if (vote.status == RestartVoteStatus.OPEN) {
            vote.trySettle(System.currentTimeMillis(), getAliveHumanPlayerIdsFor(gameId))
        } else false
        if (settledNow) {
            saveRestartVote(gameId, vote)
            broadcastRestartVoteSignal(gameId, "settled")
        }
        if (old == null || old.status != vote.status || old.result != vote.result || old.votes != vote.votes) {
            restartVoteCache[gameId] = vote
            withContext(Dispatcher.GL) { EventBus.send(RestartVoteUpdated(gameId)) }
        }
        if (vote.status == RestartVoteStatus.SETTLED && vote.result == true) {
            executeRestart(gameId) // no-op if someone else is already executing
        }
        checkRestartAndSwitch(gameId)
    }

    private fun getAliveHumanPlayerIdsFor(gameId: String): Set<String> {
        val gameInfo = UncivGame.Current.gameInfo
        return if (gameInfo != null && gameInfo.gameId == gameId) gameInfo.getAliveHumanPlayerIds() else emptySet()
    }

    /**
     * Initiates a restart vote for [gameId]. The initiator automatically votes yes.
     * @return the active vote (the existing one if a vote is already running)
     */
    suspend fun startRestartVote(gameId: String, turn: Int, timeoutMinutes: Int): RestartVote? {
        val existing = fetchRestartVote(gameId)
        if (existing != null) {
            restartVoteCache[gameId] = existing
            withContext(Dispatcher.GL) { EventBus.send(RestartVoteUpdated(gameId)) }
            return existing
        }
        val playerId = UncivGame.Current.settings.multiplayer.getUserId()
        val vote = RestartVote().apply {
            targetTurn = turn
            initiatorPlayerId = playerId
            startedAtMillis = System.currentTimeMillis()
            this.timeoutMinutes = timeoutMinutes
            votes[playerId] = true
        }
        saveRestartVote(gameId, vote)
        // Re-read to detect a concurrent initiator: last write wins, only one vote survives
        val confirmed = fetchRestartVote(gameId) ?: vote
        restartVoteCache[gameId] = confirmed
        withContext(Dispatcher.GL) { EventBus.send(RestartVoteUpdated(gameId)) }
        broadcastRestartVoteSignal(gameId, "start")
        return confirmed
    }

    /**
     * Casts (or changes) the vote of the current user for [gameId].
     * @return the updated vote, or null if there is no active vote
     */
    suspend fun castRestartVote(gameId: String, voteValue: Boolean): RestartVote? {
        val playerId = UncivGame.Current.settings.multiplayer.getUserId()
        var current = fetchRestartVote(gameId) ?: return null
        if (current.status != RestartVoteStatus.OPEN) return current
        current.votes[playerId] = voteValue
        val alivePlayerIds = getAliveHumanPlayerIdsFor(gameId)
        val settledNow = current.trySettle(System.currentTimeMillis(), alivePlayerIds)
        saveRestartVote(gameId, current)
        // Re-read and re-merge once to survive a concurrent write losing our vote
        val confirmed = fetchRestartVote(gameId) ?: current
        if (confirmed.status == RestartVoteStatus.OPEN && confirmed.votes[playerId] != voteValue) {
            confirmed.votes[playerId] = voteValue
            confirmed.trySettle(System.currentTimeMillis(), alivePlayerIds)
            saveRestartVote(gameId, confirmed)
        }
        restartVoteCache[gameId] = confirmed
        withContext(Dispatcher.GL) { EventBus.send(RestartVoteUpdated(gameId)) }
        broadcastRestartVoteSignal(gameId, if (confirmed.status == RestartVoteStatus.SETTLED) "settled" else "cast")
        return confirmed
    }

    /**
     * Executes the restart of [gameId] after a passed vote. Guarded by a [RestartMarker] so that
     * only one client generates the new game; a crashed executor is taken over after
     * [RESTART_TAKEOVER_TIMEOUT_MS].
     * @return the new gameId, or null if the restart is handled elsewhere / can't be done
     */
    suspend fun executeRestart(gameId: String): String? {
        val gameInfo = UncivGame.Current.gameInfo ?: return null
        if (gameInfo.gameId != gameId) return null
        val vote = fetchRestartVote(gameId) ?: return null
        if (vote.status != RestartVoteStatus.SETTLED || vote.result != true) return null

        val myPlayerId = UncivGame.Current.settings.multiplayer.getUserId()
        val existingMarker = fetchRestartMarker(gameId)
        if (existingMarker != null && existingMarker.status == RestartMarkerStatus.DONE) {
            return existingMarker.newGameId.ifEmpty { null }
        }
        if (existingMarker != null && existingMarker.executorPlayerId != myPlayerId
            && System.currentTimeMillis() - existingMarker.startedAtMillis < RESTART_TAKEOVER_TIMEOUT_MS
        ) return null // Someone else is executing

        val startedAt = System.currentTimeMillis()
        saveRestartMarker(gameId, RestartMarker().apply {
            executorPlayerId = myPlayerId
            startedAtMillis = startedAt
        })

        val newGame = try {
            GameStarter.startNewGame(GameSetupInfo(gameInfo))
        } catch (ex: Exception) {
            Log.error("Restart vote: could not create the new game", ex)
            return null // marker stays RESTARTING; another client takes over after the timeout
        }
        // Double-check ownership after the slow generation: a takeover may have happened
        val currentMarker = fetchRestartMarker(gameId)
        if (currentMarker == null || (currentMarker.executorPlayerId != myPlayerId
                && currentMarker.status == RestartMarkerStatus.RESTARTING)) return null

        multiplayerServer.uploadGame(newGame, withPreview = true)
        saveRestartMarker(gameId, RestartMarker().apply {
            status = RestartMarkerStatus.DONE
            executorPlayerId = myPlayerId
            startedAtMillis = startedAt
            newGameId = newGame.gameId
        })
        broadcastRestartVoteSignal(gameId, "restarted")
        return newGame.gameId
    }

    /**
     * Checks whether the restart of [gameId] is done and, if so, downloads and loads the new game.
     * Only auto-switches while the player is actually in the game; in other screens the regular
     * update loop takes care of the previews.
     * @return true if the player was switched to the restarted game
     */
    suspend fun checkRestartAndSwitch(gameId: String): Boolean {
        if (UncivGame.Current.worldScreen?.gameInfo?.gameId != gameId) return false
        val marker = fetchRestartMarker(gameId) ?: return false
        if (marker.status != RestartMarkerStatus.DONE || marker.newGameId.isEmpty()) return false
        if (UncivGame.Current.gameInfo?.gameId == marker.newGameId) return false
        downloadGame(marker.newGameId)
        return true
    }

    /**
     * Broadcasts a restart vote signal through the WebSocket chat channel. Best-effort only:
     * the authoritative state is always re-read from the server files, so servers without chat
     * support (e.g. Dropbox) simply fall back to polling.
     */
    private fun broadcastRestartVoteSignal(gameId: String, type: String) {
        val playerId = UncivGame.Current.settings.multiplayer.getUserId()
        val civName = UncivGame.Current.gameInfo?.civilizations?.firstOrNull { it.playerId == playerId }?.civName
            ?: playerId
        ChatStore.getChatByGameId(gameId).requestMessageSend(civName, "${RestartVote.PROTOCOL_PREFIX}$type")
    }

    //endregion

    /**
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws MultiplayerFileNotFoundException if the file can't be found
     */
    suspend fun downloadGame(game: MultiplayerGamePreview) {
        val preview = game.preview ?: throw game.error!!
        downloadGame(preview.gameId)
    }

    /** Downloads game, and updates it locally
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws MultiplayerFileNotFoundException if the file can't be found
     */
    suspend fun downloadGame(gameId: String) = coroutineScope {
        val serverGame = multiplayerServer.downloadGame(gameId)
        // With the "forbid reload" option, a player entering the game resumes their own turn from the
        // locally saved snapshot (which is at least as new as the server's turn-start state for the same
        // turn) instead of reloading the server state - this is what makes reload-to-undo impossible.
        val gameInfo = localSnapshotForEntry(serverGame) ?: serverGame
        val preview = gameInfo.asPreview()
        val onlineGame = multiplayerFiles.getGameByGameId(gameId)
        val onlinePreview = onlineGame?.preview
        if (onlineGame == null) {
            createGame(gameInfo)
        } else if (onlinePreview != null && hasNewerGameState(preview, onlinePreview)) {
            onlineGame.updatePreview(preview)
        }
        UncivGame.Current.loadGame(gameInfo)
    }

    /**
     * The local snapshot to resume from when entering a game, or null to use the server state:
     * only for games with the "forbid reload" option, when no such game is currently running locally
     * (i.e. this is a game *entry*, not an in-game sync), and when the snapshot is from the same
     * turn as the server state - then the snapshot is at least as new as the server state, since the
     * server only receives the turn-start upload while the player plays the turn locally.
     */
    private fun localSnapshotForEntry(serverGame: GameInfo): GameInfo? =
        resolveLocalSnapshotForEntry(
            serverGame,
            UncivGame.Current.gameInfo?.gameId,
            multiplayerFiles.loadLocalSnapshot(serverGame.gameId)
        )

    /**
     * Checks if the given game is current and loads it, otherwise loads the game from the server
     */
    suspend fun downloadGame(gameInfo: GameInfo) = coroutineScope {
        val gameId = gameInfo.gameId
        val preview = multiplayerServer.tryDownloadGamePreview(gameId)
        if (hasLatestGameState(gameInfo, preview)) {
            gameInfo.isUpToDate = true
            UncivGame.Current.loadGame(gameInfo)
        } else {
            downloadGame(gameId)
        }
    }




    /**
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws MultiplayerFileNotFoundException if the file can't be found
     * @throws MultiplayerAuthException if the authentication failed
     */
    suspend fun updateGame(gameInfo: GameInfo) {
        debug("Updating remote game %s", gameInfo.gameId)
        multiplayerServer.uploadGame(gameInfo, withPreview = true)
        val game = multiplayerFiles.getGameByGameId(gameInfo.gameId)
        debug("Existing OnlineMultiplayerGame: %s", game)
        if (game == null) {
            multiplayerFiles.addGame(gameInfo)
        } else {
            game.updatePreview(gameInfo.asPreview())
        }
    }

    /**
     * Checks if [gameInfo] and [preview] are up-to-date with each other.
     */
    @Readonly
    fun hasLatestGameState(gameInfo: GameInfo, preview: GameInfoPreview): Boolean {
        // TODO look into how to maybe extract interfaces to not make this take two different methods
        return gameInfo.currentPlayer == preview.currentPlayer
                && gameInfo.turns == preview.turns
    }


    /**
     * Checks if [preview1] has a more recent game state than [preview2]
     */
    @Readonly
    private fun hasNewerGameState(preview1: GameInfoPreview, preview2: GameInfoPreview): Boolean {
        return preview1.turns > preview2.turns
    }

    companion object {
        fun usesCustomServer() = UncivGame.Current.settings.multiplayer.getServer() != Constants.dropboxMultiplayerServer
        fun usesDropbox() = !usesCustomServer()
    }
}

/**
 * Calls the given [action] when [lastSuccessfulExecution] lies further in the past than [throttleInterval].
 *
 * Also updates [lastSuccessfulExecution] to [Instant.now], but only when [action] did not result in an exception.
 *
 * Any exception thrown by [action] is propagated.
 *
 * @return true if the update happened
 */
suspend fun <T> throttle(
    lastSuccessfulExecution: AtomicReference<Instant?>,
    throttleInterval: Duration,
    onNoExecution: () -> T,
    onFailed: (Throwable) -> T,
    action: suspend () -> T
): T {
    val lastExecution = lastSuccessfulExecution.get()
    val now = Instant.now()
    val shouldRunAction = lastExecution == null || Duration.between(lastExecution, now).isLargerThan(throttleInterval)
    return if (shouldRunAction) {
        attemptAction(lastSuccessfulExecution, onNoExecution, onFailed, action)
    } else {
        onNoExecution()
    }
}

/**
 * Attempts to run the [action], changing [lastSuccessfulExecution], but only if no other thread changed [lastSuccessfulExecution] in the meantime
 * and [action] did not throw an exception.
 */
suspend fun <T> attemptAction(
    lastSuccessfulExecution: AtomicReference<Instant?>,
    onNoExecution: () -> T,
    onFailed: (Throwable) -> T = { throw it },
    action: suspend () -> T
): T {
    val lastExecution = lastSuccessfulExecution.get()
    val now = Instant.now()
    return if (lastSuccessfulExecution.compareAndSet(lastExecution, now)) {
        try {
            action()
        } catch (e: Throwable) {
            lastSuccessfulExecution.compareAndSet(now, lastExecution)
            onFailed(e)
        }
    } else {
        onNoExecution()
    }
}


fun GameInfoPreview.isUsersTurn() = civilizations.firstOrNull { it.civID == currentPlayer }?.playerId == UncivGame.Current.settings.multiplayer.getUserId()
fun GameInfo.isUsersTurn() = currentPlayer.isNotEmpty() && getCivilization(currentPlayer).playerId == UncivGame.Current.settings.multiplayer.getUserId()
