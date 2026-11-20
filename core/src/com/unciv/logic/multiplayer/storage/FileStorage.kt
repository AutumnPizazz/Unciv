package com.unciv.logic.multiplayer.storage

import com.unciv.logic.UncivShowableException
import java.util.Date
import java.io.FileNotFoundException  // Kdoc only
import java.io.InputStream

class FileStorageConflictException : Exception()
class MultiplayerRoomCodeException : UncivShowableException("The room authorization code is invalid, expired, or already used.")
class MultiplayerTurnActionConflictException(val serverMessage: String) : Exception(serverMessage)
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
     * Returns the file contents as a stream so they can be parsed as they arrive. The caller must close it.
     * @throws FileStorageRateLimitReached if the file storage backend can't handle any additional actions for a time
     * @throws FileNotFoundException if the file can't be found
     */
    fun loadFileData(fileName: String): InputStream
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
    /** Submits one client-computed real-time action batch; returns the server-assigned sequence. */
    fun appendRealtimeTurnAction(gameId: String, request: String): String? = null
    /** Loads the real-time action log after the supplied server sequence. */
    fun loadRealtimeTurnActions(gameId: String, turn: Int, after: Long): String? = null
    /** Marks the authenticated player ready for the next turn. */
    fun markRealtimeTurnReady(gameId: String, request: String): String? = null
    fun checkAuthStatus(userId: String, password: String): AuthStatus
}
