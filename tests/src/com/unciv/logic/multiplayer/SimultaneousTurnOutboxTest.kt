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

    private val first = SimultaneousTurnOperation(1, "player", 10, "unit.move", "{}", createdAtMillis = 42)
    private val second = first.copy(sequence = 11)

    @Test
    fun recoveredLogSkipsAlreadyAcceptedOperationsAndRetriesAfterFailure() = runBlocking {
        val directory = FileHandle(temporaryFolder.root)
        SimultaneousTurnOutbox(directory, 1, 100, "player").record(listOf(first, second))
        val reopened = SimultaneousTurnOutbox(directory, 1, 100, "player")
        val acknowledged = first.copy(createdAtMillis = first.createdAtMillis + 1)
        try {
            reopened.recover(listOf(acknowledged)) {
                assertEquals(second, it)
                throw IOException("offline")
            }
            fail("Recovery must not succeed before the pending action is acknowledged")
        } catch (_: IOException) {
            // Restart after the transient upload failure.
        }

        val retried = ArrayList<SimultaneousTurnOperation>()
        val recovered = reopened.recover(listOf(acknowledged)) { retried.add(it) }
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
        assertEquals(listOf(first), recovered.operations)
        val reopened = SimultaneousTurnOutbox(directory, 1, 100, "player")
        assertFalse(reopened.recover(listOf(first), serverReady = true) {
            fail("Discarded actions must not be retried after reload")
        }.hasConflicts)
    }

    @Test
    fun rejectedRecoveryPreservesAcceptedPrefixAndPermanentlyDropsPendingSuffix() = runBlocking {
        val directory = FileHandle(temporaryFolder.root)
        val outbox = SimultaneousTurnOutbox(directory, 1, 100, "player")
        val third = first.copy(sequence = 12)
        val fourth = first.copy(sequence = 13)
        outbox.record(listOf(third, fourth))
        outbox.record(listOf(first, second))
        val uploads = ArrayList<SimultaneousTurnOperation>()
        val recovered = outbox.recover(listOf(third)) { operation ->
            uploads.add(operation)
            if (operation == second)
                throw SimultaneousTurnOperationRejectedException(operation, IOException("conflict"))
        }
        assertEquals(listOf(first, second), uploads)
        assertTrue(recovered.hasConflicts)
        assertEquals(listOf(first, third), recovered.operations)

        val reopened = SimultaneousTurnOutbox(directory, 1, 100, "player")
        assertFalse(reopened.recover(listOf(first, third)) { fail("The rejected suffix must not be retried") }.hasConflicts)
        val replacement = fourth.copy(payload = "{\"replacement\":true}")
        reopened.record(listOf(replacement))
        assertEquals(listOf(first, third, replacement), reopened.recover(listOf(first, third)) {
            assertEquals(replacement, it)
        }.operations)
    }

    @Test
    fun differentActionAtSameSequenceDropsTheLocalSuffix() = runBlocking {
        val directory = FileHandle(temporaryFolder.root)
        val outbox = SimultaneousTurnOutbox(directory, 1, 100, "player")
        val authoritative = first.copy(payload = "{\"fromServer\":true}")
        outbox.record(listOf(first, second))

        val recovered = outbox.recover(listOf(authoritative)) { fail("Cannot upload from a different local state") }

        assertTrue(recovered.hasConflicts)
        assertEquals(listOf(authoritative), recovered.operations)
        assertFalse(outbox.recover(listOf(authoritative)) { fail("The local suffix must be gone") }.hasConflicts)
    }

    @Test
    fun interruptedDiscardFinishesBeforeRecoveryUploadsAnything() = runBlocking {
        val directory = FileHandle(temporaryFolder.root)
        val outbox = SimultaneousTurnOutbox(directory, 1, 100, "player")
        outbox.record(listOf(first))
        outbox.record(listOf(second))
        val turnDirectory = directory.list().single().list().single()
        turnDirectory.child("discard-from").writeString(second.sequence.toString(), false)

        val recovered = outbox.recover(listOf(first)) { fail("A persisted discard must finish before upload") }
        assertTrue(recovered.hasConflicts)
        assertEquals(listOf(first), recovered.operations)
        assertFalse(turnDirectory.child("discard-from").exists())
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
