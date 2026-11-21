package com.unciv.logic.multiplayer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex

class SimultaneousTurnOperationRejectedException(
    val operation: SimultaneousTurnOperation,
    cause: Exception
) : Exception(cause.message, cause)

/** Serializes and batches operation uploads while a player is working through a simultaneous turn. */
class SimultaneousTurnOperationUploadQueue(
    private val upload: suspend (List<SimultaneousTurnOperation>) -> Unit,
    private val onUploaded: (List<SimultaneousTurnOperation>) -> Unit = {},
    private val onFailure: (Exception) -> Unit = {},
    private val onRejected: suspend (SimultaneousTurnOperation, Exception) -> Unit = { _, _ -> },
    private val debounceMillis: Long = 250,
    private val retryMillis: Long = 3000
) {
    private data class OperationKey(val turn: Int, val playerId: String, val sequence: Long)

    private val pendingLock = Any()
    private val pending = LinkedHashMap<OperationKey, SimultaneousTurnOperation>()
    private val wakeups = Channel<Unit>(Channel.CONFLATED)
    private val uploadMutex = Mutex()
    private var rejected = false

    fun enqueueBatch(operations: List<SimultaneousTurnOperation>) {
        synchronized(pendingLock) {
            if (rejected) return
            for (operation in operations) {
                val key = keyOf(operation)
                if (!pending.containsKey(key)) pending[key] = operation
            }
        }
        wakeups.trySend(Unit)
    }

    /** Keeps retrying transient failures; failed batches remain queued until the server accepts them. */
    suspend fun run() {
        for (ignored in wakeups) {
            delay(debounceMillis)
            while (true) {
                try {
                    if (!flushPending()) return
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

    /** Returns false when this turn state was rejected and must be rebuilt before submission. */
    suspend fun flushPending(): Boolean {
        uploadMutex.lock()
        try {
            if (rejected) return false
            while (true) {
                val batch = synchronized(pendingLock) { pending.values.toList() }
                if (batch.isEmpty()) return true
                try {
                    upload(batch)
                    removeUploaded(batch)
                    onUploaded(batch)
                } catch (ex: SimultaneousTurnOperationRejectedException) {
                    synchronized(pendingLock) {
                        rejected = true
                        pending.clear()
                    }
                    onRejected(ex.operation, ex)
                    return false
                }
            }
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
