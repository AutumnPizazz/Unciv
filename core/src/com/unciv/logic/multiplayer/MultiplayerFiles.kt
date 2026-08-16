package com.unciv.logic.multiplayer

import com.badlogic.gdx.files.FileHandle
import com.unciv.UncivGame
import com.unciv.logic.GameInfo
import com.unciv.logic.GameInfoPreview
import com.unciv.logic.event.EventBus
import com.unciv.utils.debug
import yairm210.purity.annotations.Readonly
import java.time.Instant
import java.util.*

/** Files that are stored locally */
class MultiplayerFiles {
    internal val files = UncivGame.Current.files
    internal val savedGames: MutableMap<FileHandle, MultiplayerGamePreview> = Collections.synchronizedMap(mutableMapOf())

    internal fun updateSavesFromFiles() {
        val saves = files.getMultiplayerSaves()

        val removedSaves = savedGames.keys - saves.toSet()
        for (saveFile in removedSaves) {
            deleteGame(saveFile)
        }

        val newSaves = saves - savedGames.keys
        for (saveFile in newSaves) {
            addGame(saveFile)
        }
    }

    /**
     * Deletes the game from disk, does not delete it remotely.
     */
    fun deleteGame(multiplayerGamePreview: MultiplayerGamePreview) {
        deleteGame(multiplayerGamePreview.fileHandle)
    }

    private fun deleteGame(fileHandle: FileHandle) {
        files.deleteSave(fileHandle)

        val game = savedGames[fileHandle] ?: return

        debug("Deleting game %s with id %s", fileHandle.name(), game.preview?.gameId)
        val gameId = game.preview?.gameId
        if (gameId != null) deleteLocalSnapshot(gameId)
        savedGames.remove(game.fileHandle)
    }

    internal fun addGame(newGame: GameInfo) {
        val newGamePreview = newGame.asPreview()
        addGame(newGamePreview, newGamePreview.gameId)
    }

    internal fun addGame(preview: GameInfoPreview, saveFileName: String) {
        val fileHandle = files.saveMultiplayerGamePreview(preview, saveFileName)
        return addGame(fileHandle, preview)
    }

    private fun addGame(fileHandle: FileHandle, preview: GameInfoPreview? = null) {
        debug("Adding game %s", fileHandle.name())
        val game = MultiplayerGamePreview(fileHandle, preview, if (preview != null) Instant.now() else null)
        savedGames[fileHandle] = game
    }

    @Readonly
    fun getGameByName(name: String): MultiplayerGamePreview? {
        return savedGames.values.firstOrNull { it.name == name }
    }

    @Readonly
    fun getGameByGameId(gameId: String): MultiplayerGamePreview? {
        return savedGames.values.firstOrNull { it.preview?.gameId == gameId }
    }

    /**
     * Local snapshot used by the "forbid reload" option: a full [GameInfo] saved locally during the
     * player's turn, so that re-entering the game resumes the turn instead of reloading the server's
     * turn-start state. These files are filtered out of [UncivFiles.getMultiplayerSaves] so they are
     * never parsed as game previews.
     */
    private fun localSnapshotFile(gameId: String) = files.getMultiplayerSave("${gameId}_localstate")

    fun saveLocalSnapshot(gameInfo: GameInfo) {
        files.saveGame(gameInfo, localSnapshotFile(gameInfo.gameId))
    }

    fun loadLocalSnapshot(gameId: String): GameInfo? {
        val file = localSnapshotFile(gameId)
        if (!file.exists()) return null
        return try {
            files.loadGameFromFile(file)
        } catch (ex: Exception) {
            debug("Failed to load local snapshot for %s: %s", gameId, ex.message)
            null
        }
    }

    fun deleteLocalSnapshot(gameId: String) {
        files.deleteSave(localSnapshotFile(gameId))
    }


    /**
     * Fires [MultiplayerGameNameChanged]
     */
    fun changeGameName(game: MultiplayerGamePreview, newName: String, onException: (Exception?)->Unit) {
        debug("Changing name of game %s to", game.name, newName)
        val oldPreview = game.preview ?: throw game.error!!
        val oldLastUpdate = game.getLastUpdate()
        val oldName = game.name

        val newFileHandle = files.saveMultiplayerGamePreview(oldPreview, newName, onException)
        val newGame = MultiplayerGamePreview(newFileHandle, oldPreview, oldLastUpdate)
        savedGames[newFileHandle] = newGame

        savedGames.remove(game.fileHandle)
        files.deleteSave(game.fileHandle)
        EventBus.send(MultiplayerGameNameChanged(oldName, newName))
    }
}
