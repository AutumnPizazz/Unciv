package com.unciv.logic.multiplayer.storage

import com.unciv.Constants
import com.badlogic.gdx.utils.SerializationException
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.BackwardCompatibility.migrateCivID
import com.unciv.logic.GameInfo
import com.unciv.logic.GameInfoPreview
import com.unciv.logic.UncivKtor
import com.unciv.logic.files.FileConversions
import com.unciv.logic.files.UncivFiles
import com.unciv.logic.multiplayer.RealtimeTurnActionEntry
import com.unciv.logic.multiplayer.RealtimeTurnActionFeed
import com.unciv.logic.multiplayer.RealtimeTurnActionRequest
import com.unciv.logic.multiplayer.RealtimeTurnReadyRequest
import com.unciv.logic.multiplayer.ServerFeatureSet
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.jetbrains.annotations.VisibleForTesting

/**
 * Allows access to games on the UncivCN community server.
 *
 * For low-level access only, use [UncivGame.onlineMultiplayer] on [UncivGame.Current] if you're looking to load/save a game.
 *
 * @see FileStorage
 */
@Suppress("RedundantSuspendModifier") // Methods can take a long time, so force users to use them in a coroutine to not get ANRs on Android
class MultiplayerServer private constructor(
    private val serverUrl: String,
    private val authenticationHeader: Map<String, String>?
) {
    /** The background turn checker supplies its own authentication header. */
    constructor(authenticationHeader: Map<String, String>? = null) :
        this(Constants.uncivMultiplayerServer, authenticationHeader)

    companion object {
        @VisibleForTesting
        fun forTesting(serverUrl: String, authenticationHeader: Map<String, String>? = null) =
            MultiplayerServer(serverUrl, authenticationHeader)
    }

    private var featureSet = ServerFeatureSet()
    fun getFeatureSet() = featureSet
    fun setFeatureSet(value: ServerFeatureSet) {
        if (featureSet != value) {
            UncivGame.Current.worldScreen?.chatButton?.refreshVisibility()
            featureSet = value
        }
    }

    fun getServerUrl() = serverUrl

    fun fileStorage(): FileStorage = serverStorage()

    private fun serverStorage(): UncivServerFileStorage {
        val authHeader = if (authenticationHeader != null) authenticationHeader else {
            val settings = UncivGame.Current.settings.multiplayer
            mapOf("Authorization" to settings.getAuthHeader())
        }

        return UncivServerFileStorage(serverUrl, authHeader)
    }

    /**
     * Checks if the server is alive and sets its [ServerFeatureSet] accordingly.
     * @return true if the server is alive, false otherwise
     */
    suspend fun checkServerStatus(): Boolean {
        val resp = UncivKtor.client.get("$serverUrl/isalive")

        if (resp.status.isSuccess()) {
            setFeatureSet(
                try {
                    json().fromJson(ServerFeatureSet::class.java, resp.bodyAsText())
                } catch (_: Exception) {
                    // The server does not support server feature set - not an error!
                    ServerFeatureSet()
                }
            )

            return true
        }

        return false
    }


    /**
     * @return true if the authentication was successful or the server does not support authentication.
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws MultiplayerAuthException if the authentication failed
     */
    fun authenticate(password: String?): Boolean {
        if (featureSet.authVersion == 0) return true

        val settings = UncivGame.Current.settings.multiplayer

        val success = fileStorage().authenticate(
            userId = settings.getUserId(), password = password ?: settings.getCurrentServerPassword() ?: ""
        )
        if (password != null && success) {
            settings.setCurrentServerPassword(password)
        }
        return success
    }

    /**
     * @return true if setting the password was successful, false otherwise.
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws MultiplayerAuthException if the authentication failed
     */
    fun setPassword(password: String): Boolean {
        if (featureSet.authVersion > 0 && fileStorage().setPassword(newPassword = password)) {
            UncivGame.Current.settings.multiplayer.setCurrentServerPassword(password)
            return true
        }

        return false
    }


    /**
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws MultiplayerAuthException if the authentication failed
     */
    suspend fun uploadGame(gameInfo: GameInfo, withPreview: Boolean, roomCode: String? = null) {
        if (gameInfo.isSimultaneousTurnsMode()) {
            check(withPreview)
            if (roomCode.isNullOrBlank()) throw MultiplayerRoomCodeException()
            if (!saveSimultaneousTurn(gameInfo, lease = null, roomCode = roomCode))
                throw FileStorageConflictException()
            return
        }
        val zippedGameInfo = UncivFiles.gameInfoToString(gameInfo, forceZip = true, updateChecksum = true)
        fileStorage().saveFileData(gameInfo.gameId, zippedGameInfo)

        // We upload the preview after the game because otherwise the following race condition will happen:
        // Current player ends turn -> Uploads Game Preview
        // Other player checks for updates -> Downloads Game Preview
        // Current player starts game upload
        // Other player sees update in preview -> Downloads game, gets old state
        // Current player finishes uploading game
        if (withPreview) {
            tryUploadGamePreview(gameInfo.asPreview())
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    /**
     * Used to upload only the preview of a game. If the preview is uploaded together with (before/after)
     * the gameInfo, it is recommended to use tryUploadGame(gameInfo, withPreview = true)
     *
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws MultiplayerAuthException if the authentication failed
     *
     * @see uploadGame
     * @see GameInfo.asPreview
     */
    suspend fun tryUploadGamePreview(gameInfo: GameInfoPreview) {
        val zippedGameInfo = UncivFiles.gameInfoToString(gameInfo)
        fileStorage().saveFileData("${gameInfo.gameId}_Preview", zippedGameInfo)
    }

    /**
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws FileNotFoundException if the file can't be found
     */
    /** Uploads the operation sidecar without changing the authoritative game save. */
    suspend fun uploadSimultaneousTurnOperations(
        gameId: String,
        operations: List<com.unciv.logic.multiplayer.SimultaneousTurnOperation>
    ) = com.unciv.logic.multiplayer.SimultaneousTurnOperations.upload(this, gameId, operations)

    /** Downloads the operation sidecar; callers should treat missing files as an empty log. */
    suspend fun downloadSimultaneousTurnOperations(
        gameId: String
    ): List<com.unciv.logic.multiplayer.SimultaneousTurnOperation> =
        com.unciv.logic.multiplayer.SimultaneousTurnOperations.download(this, gameId)

    suspend fun appendRealtimeTurnAction(
        gameId: String,
        request: RealtimeTurnActionRequest
    ): RealtimeTurnActionEntry? {
        val response = fileStorage().appendRealtimeTurnAction(gameId, json().toJson(request)) ?: return null
        return json().fromJson(RealtimeTurnActionEntry::class.java, response)
    }

    suspend fun downloadRealtimeTurnActions(gameId: String, turn: Int, after: Long): RealtimeTurnActionFeed? {
        val response = fileStorage().loadRealtimeTurnActions(gameId, turn, after) ?: return null
        return json().fromJson(RealtimeTurnActionFeed::class.java, response)
    }

    suspend fun markRealtimeTurnReady(gameId: String, turn: Int, clientSequence: Long): RealtimeTurnActionEntry? {
        val request = json().toJson(RealtimeTurnReadyRequest(turn, clientSequence))
        val response = fileStorage().markRealtimeTurnReady(gameId, request) ?: return null
        return json().fromJson(RealtimeTurnActionEntry::class.java, response)
    }

    suspend fun appendSimultaneousTurnOperations(gameId: String, operations: String): Boolean =
        fileStorage().appendSimultaneousTurnOperations(gameId, operations)

    suspend fun loadSimultaneousTurnOperations(gameId: String): String? =
        fileStorage().loadSimultaneousTurnOperations(gameId)

    suspend fun acquireSimultaneousTurnSettlementLock(gameId: String, turn: Int): String? =
        serverStorage().acquireSimultaneousTurnSettlementLock(gameId, turn)

    suspend fun renewSimultaneousTurnSettlementLock(gameId: String, lease: String): Boolean =
        serverStorage().renewSimultaneousTurnSettlementLock(gameId, lease)

    suspend fun releaseSimultaneousTurnSettlementLock(gameId: String, lease: String) {
        serverStorage().releaseSimultaneousTurnSettlementLock(gameId, lease)
    }

    private fun saveSimultaneousTurn(
        gameInfo: GameInfo, lease: String?, roomCode: String? = null,
        restartFrom: String? = null, restartClaim: Long = 0,
    ): Boolean = serverStorage().saveSimultaneousTurn(
        gameInfo.gameId,
        UncivFiles.gameInfoToString(gameInfo, forceZip = true, updateChecksum = true),
        UncivFiles.gameInfoToString(gameInfo.asPreview()),
        lease,
        roomCode,
        restartFrom,
        restartClaim,
    )

    suspend fun commitSimultaneousTurn(gameInfo: GameInfo, lease: String): Boolean = saveSimultaneousTurn(gameInfo, lease)

    suspend fun restartSimultaneousGame(gameInfo: GameInfo, roomCode: String, oldGameId: String, claim: Long): Boolean =
        saveSimultaneousTurn(gameInfo, lease = null, roomCode = roomCode, restartFrom = oldGameId, restartClaim = claim)

    suspend fun tryDownloadGame(gameId: String): GameInfo {
        val gameInfo = UncivFiles.gameInfoFromStream { fileStorage().loadFileData(gameId) }
        gameInfo.gameParameters.multiplayerServerUrl = serverUrl
        return gameInfo
    }


    /**
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws MultiplayerFileNotFoundException if the file can't be found
     */
    suspend fun downloadGame(gameId: String): GameInfo {
        val latestGame = tryDownloadGame(gameId)
        latestGame.isUpToDate = true
        return latestGame
    }

    /**
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws FileNotFoundException if the file can't be found
     */
    suspend fun tryDownloadGamePreview(gameId: String): GameInfoPreview {
        val preview = FileConversions.readJson(fileStorage().loadFileData("${gameId}_Preview"), GameInfoPreview::class.java)
            ?: throw SerializationException("Empty game preview for $gameId")
        preview.migrateCivID()
        return preview
    }
}
