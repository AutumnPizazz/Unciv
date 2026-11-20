package com.unciv.logic.multiplayer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex

/** Serializes and batches operation uploads while a player is working through a simultaneous turn. */
class SimultaneousTurnOperationUploadQueue(
    private val upload: suspend (List<SimultaneousTurnOperation>) -> Unit,
    private val onUploaded: (List<SimultaneousTurnOperation>) -> Unit = {},
    private val onFailure: (Exception) -> Unit = {},
    private val debounceMillis: Long = 250,
    private val retryMillis: Long = 3000
) {
    private data class OperationKey(val turn: Int, val playerId: String, val sequence: Long)

    private val pendingLock = Any()
    private val pending = LinkedHashMap<OperationKey, SimultaneousTurnOperation>()
    private val wakeups = Channel<Unit>(Channel.CONFLATED)
    private val uploadMutex = Mutex()

    fun enqueue(operation: SimultaneousTurnOperation) {
        synchronized(pendingLock) {
            val key = keyOf(operation)
            if (!pending.containsKey(key)) pending[key] = operation
        }
        wakeups.trySend(Unit)
    }

    /** Keeps retrying transient failures; failed batches remain queued until the server accepts them. */
    suspend fun run() {
        for (ignored in wakeups) {
            delay(debounceMillis)
            while (true) {
                try {
                    flushPending()
                    break
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Exception) {
                    onFailure(ex)
                    delay(retryMillis)
                }
            }
        }
    }

    suspend fun flushPending() {
        uploadMutex.lock()
        try {
            while (true) {
                val batch = synchronized(pendingLock) { pending.values.toList() }
                if (batch.isEmpty()) return
                upload(batch)
                removeUploaded(batch)
                onUploaded(batch)
            }
        } finally {
            uploadMutex.unlock()
        }
    }

    /** Uploads the full turn log and its done marker as one acknowledged submission. */
    suspend fun uploadForSubmission(operations: List<SimultaneousTurnOperation>) {
        uploadMutex.lock()
        try {
            upload(operations)
            removeUploaded(operations)
            onUploaded(operations)
        } finally {
            uploadMutex.unlock()
        }
    }

    private fun removeUploaded(operations: List<SimultaneousTurnOperation>) {
        synchronized(pendingLock) {
            for (operation in operations) {
                val key = keyOf(operation)
                if (pending[key] == operation) pending.remove(key)
            }
        }
    }

    private fun keyOf(operation: SimultaneousTurnOperation) =
        OperationKey(operation.turn, operation.playerId, operation.sequence)
}
