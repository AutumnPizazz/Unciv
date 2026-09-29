package com.unciv.logic.multiplayer.storage

import com.unciv.logic.UncivShowableException
import com.unciv.logic.multiplayer.SimultaneousTurnReservation
import java.util.Date
import java.io.FileNotFoundException  // Kdoc only

class FileStorageConflictException : Exception()
class FileStorageRateLimitReached(val limitRemainingSeconds: Int) : UncivShowableException("Server limit reached! Please wait for [${limitRemainingSeconds}] seconds")
class MultiplayerFileNotFoundException(cause: Throwable?) : UncivShowableException("File could not be found on the multiplayer server", cause)
class MultiplayerAuthException(cause: Throwable?) : UncivShowableException("Authentication failed", cause)

interface FileMetaData {
    fun getLastModified(): Date?
}

enum class AuthStatus {
    UNAUTHORIZED,
    UNREGISTERED,
    VERIFIED,
    UNKNOWN
}

interface FileStorage {
    /**
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws MultiplayerAuthException if the authentication failed
     */
    fun saveFileData(fileName: String, data: String)
    /**
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws FileNotFoundException if the file can't be found
     */
    fun loadFileData(fileName: String): String
    /**
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws FileNotFoundException if the file can't be found
     */
    fun getFileMetaData(fileName: String): FileMetaData
    /**
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws FileNotFoundException if the file can't be found
     * @throws MultiplayerAuthException if the authentication failed
     */
    fun deleteFile(fileName: String)
    /**
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws MultiplayerAuthException if the authentication failed
     */
    fun authenticate(userId: String, password: String): Boolean
    /**
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws MultiplayerAuthException if the authentication failed
     */
    fun setPassword(newPassword: String): Boolean
    
    /** Atomically appends simultaneous-turn operations. Returns false if unsupported. */
    fun appendSimultaneousTurnOperations(gameId: String, operations: String): Boolean = false
    /** Loads the atomically stored simultaneous-turn operations, or null if unsupported. */
    fun loadSimultaneousTurnOperations(gameId: String): String? = null
    /** Atomically claims settlement for one simultaneous-turn game turn. */
    fun acquireSimultaneousTurnSettlementLock(gameId: String, turn: Int, owner: String): Boolean = false
    /** Extends an existing settlement claim owned by [owner]. Returns false if unsupported or not held. */
    fun renewSimultaneousTurnSettlementLock(gameId: String, turn: Int, owner: String): Boolean = false
    /** Releases a settlement claim owned by [owner]. */
    fun releaseSimultaneousTurnSettlementLock(gameId: String, turn: Int, owner: String) {}
    /**
     * Claims the [keys] of the targets a simultaneous-turn action is about to touch for [owner].
     * Returns the targets another player claimed first, empty when every target was granted,
     * or null when this backend cannot arbitrate reservations.
     */
    fun reserveSimultaneousTurnKeys(
        gameId: String,
        turn: Int,
        owner: String,
        keys: List<String>
    ): List<SimultaneousTurnReservation>? = null
    /** Targets claimed by any player for [turn], or null when this backend cannot arbitrate reservations. */
    fun listSimultaneousTurnReservations(gameId: String, turn: Int): List<SimultaneousTurnReservation>? = null

    fun checkAuthStatus(userId: String, password: String): AuthStatus
}
