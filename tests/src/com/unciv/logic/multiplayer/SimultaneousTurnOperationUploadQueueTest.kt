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
        queue.enqueue(operation)

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
    fun submissionUploadsTheWholeTurnLogAndDoneMarkerTogether() = runBlocking {
        val action = SimultaneousTurnOperation(1, "player", 1, "unit.move", "{}")
        val done = SimultaneousTurnOperation(1, "player", 2, "done", "{}")
        val uploaded = ArrayList<List<SimultaneousTurnOperation>>()
        val queue = SimultaneousTurnOperationUploadQueue(
            upload = { batch -> uploaded.add(ArrayList(batch)) }
        )
        queue.enqueue(action)
        queue.flushPending()

        queue.uploadForSubmission(listOf(action, done))

        assertEquals(2, uploaded.size)
        assertEquals(listOf(action), uploaded[0])
        assertEquals(listOf(action, done), uploaded[1])
    }
}
