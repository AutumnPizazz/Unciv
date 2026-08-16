package com.unciv.ui.screens.worldscreen

import com.unciv.UncivGame
import com.unciv.logic.multiplayer.isUsersTurn
import com.unciv.utils.Concurrency
import com.unciv.utils.debug
import com.unciv.utils.launchOnGLThread
import kotlinx.coroutines.Job

/**
 * Local snapshot scheduling for the "forbid reload" multiplayer option.
 *
 * While the player plays their turn, the current [com.unciv.logic.GameInfo] is saved to a local file
 * shortly after each state change (debounced). On re-entering the game, [com.unciv.logic.multiplayer.Multiplayer]
 * resumes from this snapshot instead of the server's turn-start state, so quitting and reloading can
 * no longer undo the turn. All heavy work (clone, serialization, disk write) happens off the main thread.
 */
class LocalSnapshotHandler(private val worldScreen: WorldScreen) {
    private val debounceMillis = 3000L
    private var dirtySince = 0L
    @Volatile private var saveInProgress = false
    @Volatile private var saveInProgressJob: Job? = null

    /** Whether this mode is active: online multiplayer game with the forbidReload option set. */
    fun isActive() = worldScreen.gameInfo.gameParameters.isOnlineMultiplayer
        && worldScreen.gameInfo.gameParameters.forbidReload

    /** Whether we should snapshot right now: active, it's our own turn, and we are not a spectator. */
    private fun isPlayersTurn() = worldScreen.gameInfo.isUsersTurn()

    /** Called when the game state may have changed (debounced: only the first call within the window counts). */
    fun markDirty() {
        if (!isActive() || !isPlayersTurn()) return
        dirtySince = System.currentTimeMillis()
    }

    /** Called every frame from the main loop; triggers the actual save once the debounce window passed. */
    fun update() {
        if (!isActive() || !isPlayersTurn() || saveInProgress || dirtySince == 0L) return
        if (System.currentTimeMillis() - dirtySince < debounceMillis) return
        dirtySince = 0L
        saveInProgress = true
        val snapshot = try {
            worldScreen.gameInfo.clone() // on main thread: avoids concurrent modification while serializing
        } catch (ex: Exception) {
            saveInProgress = false
            debug("Failed to clone game for local snapshot: %s", ex.message)
            return
        }
        saveInProgressJob = Concurrency.runOnNonDaemonThreadPool("SaveLocalSnapshot") {
            try {
                UncivGame.Current.onlineMultiplayer.multiplayerFiles.saveLocalSnapshot(snapshot)
                debug("Saved local snapshot for %s (turn %s)", snapshot.gameId, snapshot.turns)
            } catch (ex: Exception) {
                debug("Failed to save local snapshot: %s", ex.message)
            } finally {
                saveInProgress = false
            }
        }
    }

    /**
     * Saves the current state immediately (used before exiting the game), then calls [completion]
     * on the main thread. Guarantees the snapshot is on disk before the process shuts down, so a
     * quit-and-reload cannot fall back to the server's turn-start state.
     */
    fun flush(completion: () -> Unit) {
        if (!isActive()) {
            completion()
            return
        }
        val snapshot = try {
            worldScreen.gameInfo.clone()
        } catch (ex: Exception) {
            debug("Failed to clone game for snapshot flush: %s", ex.message)
            completion()
            return
        }
        dirtySince = 0L
        val inFlightSave = saveInProgressJob // wait for a debounced save to finish to avoid concurrent writes
        Concurrency.runOnNonDaemonThreadPool("FlushLocalSnapshot") {
            inFlightSave?.join()
            try {
                UncivGame.Current.onlineMultiplayer.multiplayerFiles.saveLocalSnapshot(snapshot)
            } catch (ex: Exception) {
                debug("Failed to flush local snapshot: %s", ex.message)
            }
            launchOnGLThread {
                completion()
            }
        }
    }
}
