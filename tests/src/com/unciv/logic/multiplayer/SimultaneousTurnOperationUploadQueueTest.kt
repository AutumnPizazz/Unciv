package com.unciv.logic.multiplayer

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SimultaneousTurnOperationUploadQueueTest {
    @Test
    fun failedBatchRemainsQueuedUntilTheServerAcceptsIt() = runBlocking {
        val operation = SimultaneousTurnOperation(1, "player", 1, "unit.move", "{}")
        val accepted = ArrayList<SimultaneousTurnOperation>()
        var attempts = 0
        val queue = SimultaneousTurnOperationUploadQueue(
            upload = { batch ->
                attempts++
                if (attempts == 1) throw IOException("temporary failure")
                accepted.addAll(batch)
            }
        )
        queue.enqueueBatch(listOf(operation))

        var failed = false
        try {
            queue.flushPending()
        } catch (_: IOException) {
            failed = true
        }
        assertTrue("the first upload should fail", failed)
        assertTrue("the failed batch must not be treated as uploaded", accepted.isEmpty())

        queue.flushPending()

        assertEquals(2, attempts)
        assertEquals(listOf(operation), accepted)
    }

    @Test
    fun permanentlyRejectedActionAlsoDiscardsLaterDependentActions() = runBlocking {
        val rejected = SimultaneousTurnOperation(1, "player", 1, "unit.move", "{}")
        val accepted = SimultaneousTurnOperation(1, "player", 2, "unit.action", "{}")
        val uploaded = ArrayList<SimultaneousTurnOperation>()
        val rejectedOperations = ArrayList<SimultaneousTurnOperation>()
        val queue = SimultaneousTurnOperationUploadQueue(
            upload = { batch ->
                if (batch.first() == rejected)
                    throw SimultaneousTurnOperationRejectedException(rejected, IOException("conflict"))
                uploaded.addAll(batch)
            },
            onRejected = { operation, _ -> rejectedOperations += operation }
        )
        queue.enqueueBatch(listOf(rejected, accepted))
        queue.flushPending()

        assertEquals(listOf(rejected), rejectedOperations)
        assertTrue("later actions must not upload from a stale local state", uploaded.isEmpty())
    }
}
