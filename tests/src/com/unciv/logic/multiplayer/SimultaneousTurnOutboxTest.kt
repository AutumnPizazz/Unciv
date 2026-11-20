package com.unciv.logic.multiplayer

import com.badlogic.gdx.files.FileHandle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class SimultaneousTurnOutboxTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val first = SimultaneousTurnOperation(1, "player", 10, "unit.move", "{}")
    private val second = first.copy(sequence = 11)

    @Test
    fun recoveredLogSkipsAlreadyAcceptedOperationsAndRetriesAfterFailure() = runBlocking {
        val directory = FileHandle(temporaryFolder.root)
        SimultaneousTurnOutbox(directory, 1, 100, "player").record(listOf(first, second))
        val reopened = SimultaneousTurnOutbox(directory, 1, 100, "player")
        val acknowledged = first.copy(createdAtMillis = first.createdAtMillis + 1)
        try {
            reopened.recover(listOf(acknowledged)) {
                assertEquals(listOf(second), it)
                throw IOException("offline")
            }
            fail("Recovery must not succeed before the pending action is acknowledged")
        } catch (_: IOException) {
            // Restart after the transient upload failure.
        }

        val retried = ArrayList<SimultaneousTurnOperation>()
        val recovered = reopened.recover(listOf(acknowledged)) { retried.addAll(it) }
        assertEquals(listOf(second), retried)
        assertEquals(listOf(acknowledged, second), recovered.operations)
        assertFalse(recovered.hasConflicts)
    }

    @Test
    fun submittedTurnDoesNotUploadMoreActions() = runBlocking {
        val outbox = SimultaneousTurnOutbox(FileHandle(temporaryFolder.root), 1, 100, "player")
        outbox.record(listOf(first, second))
        val done = first.copy(sequence = 12, type = "done")
        val recovered = outbox.recover(listOf(first, done)) { fail("Cannot extend a submitted turn") }
        assertEquals(listOf(first, done), recovered.operations)
        assertTrue(recovered.hasConflicts)
    }

    @Test
    fun realtimeReadyPreventsRecoveryOfUnacknowledgedActions() = runBlocking {
        val directory = FileHandle(temporaryFolder.root)
        val outbox = SimultaneousTurnOutbox(directory, 1, 100, "player")
        outbox.record(listOf(first, second))

        val recovered = outbox.recover(listOf(first), serverReady = true) {
            fail("A ready player cannot append actions")
        }

        assertTrue(recovered.hasConflicts)
        assertEquals(
            listOf(second.copy(createdAtMillis = 0)),
            recovered.rejectedOperations.map { it.copy(createdAtMillis = 0) }
        )
        outbox.discardRejectedOperations(recovered.rejectedOperations.mapTo(HashSet()) { it.sequence })
        val reopened = SimultaneousTurnOutbox(directory, 1, 100, "player")
        assertFalse(reopened.recover(listOf(first), serverReady = true) {
            fail("Discarded actions must not be retried after reload")
        }.hasConflicts)
    }

    @Test
    fun pendingActionsAreScopedToThePlayerAndTurnStartArchive() = runBlocking {
        val directory = FileHandle(temporaryFolder.root)
        SimultaneousTurnOutbox(directory, 1, 100, "player").record(listOf(first))
        for (outbox in listOf(
            SimultaneousTurnOutbox(directory, 1, 101, "player"),
            SimultaneousTurnOutbox(directory, 2, 200, "player"),
            SimultaneousTurnOutbox(directory, 1, 100, "other")
        )) {
            assertTrue(outbox.recover(emptyList()) { fail("Cannot upload a stale action") }.operations.isEmpty())
        }
    }
}
