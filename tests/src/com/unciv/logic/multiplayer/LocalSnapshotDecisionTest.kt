package com.unciv.logic.multiplayer

import com.unciv.logic.GameInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure decision behind the "forbid reload" multiplayer option
 * ([resolveLocalSnapshotForEntry]).
 *
 * The whole point of the feature is that a game entry resumes from the local snapshot - which is at
 * least as new as the server's turn-start state for the same turn - so quitting and reloading can no
 * longer go back to the start of the turn to redo moves. The snapshot must therefore be used exactly
 * when it is that fresh entry-time case, and the server state must win in every other scenario
 * (option off, non-online game, in-game sync, missing/stale/mismatched snapshot).
 */
class LocalSnapshotDecisionTest {

    private fun game(
        gameId: String = "g",
        turns: Int = 1,
        currentPlayer: String = "Rome",
        online: Boolean = false,
        forbidReload: Boolean = false
    ): GameInfo = GameInfo().apply {
        this.gameId = gameId
        this.turns = turns
        this.currentPlayer = currentPlayer
        gameParameters.isOnlineMultiplayer = online
        gameParameters.forbidReload = forbidReload
    }

    private val onlineForbidGame =
        game(gameId = "g1", turns = 7, currentPlayer = "Rome", online = true, forbidReload = true)

    @Test
    fun `same-turn entry with a snapshot resumes from the snapshot`() {
        val snapshot = game(gameId = "g1", turns = 7, currentPlayer = "Rome")

        val resolved = resolveLocalSnapshotForEntry(onlineForbidGame, currentGameId = null, localSnapshot = snapshot)

        assertSame("Entry at the same turn must resume from the local snapshot", snapshot, resolved)
        assertTrue("Resumed snapshot must be flagged up-to-date", resolved!!.isUpToDate)
    }

    @Test
    fun `server state wins when forbid reload option is off`() {
        val server = game(gameId = "g1", turns = 7, currentPlayer = "Rome", online = true, forbidReload = false)
        val snapshot = game(gameId = "g1", turns = 7, currentPlayer = "Rome")

        assertNull("Without the option a snapshot must never replace the server state",
            resolveLocalSnapshotForEntry(server, null, snapshot))
    }

    @Test
    fun `server state wins for a single-player game`() {
        val server = game(gameId = "g1", turns = 7, currentPlayer = "Rome", online = false, forbidReload = true)
        val snapshot = game(gameId = "g1", turns = 7, currentPlayer = "Rome")

        assertNull("Snapshots must only apply to online games",
            resolveLocalSnapshotForEntry(server, null, snapshot))
    }

    @Test
    fun `server state wins when the same game is already running locally`() {
        val snapshot = game(gameId = "g1", turns = 7, currentPlayer = "Rome")

        // Same gameId currently open locally -> this is an in-game server sync, not a game entry
        assertNull("An in-game sync must keep the server state",
            resolveLocalSnapshotForEntry(onlineForbidGame, currentGameId = "g1", localSnapshot = snapshot))
    }

    @Test
    fun `server state wins when there is no local snapshot`() {
        assertNull("Missing snapshot must fall back to the server state",
            resolveLocalSnapshotForEntry(onlineForbidGame, currentGameId = null, localSnapshot = null))
    }

    @Test
    fun `server state wins when the snapshot is from an earlier turn`() {
        val staleSnapshot = game(gameId = "g1", turns = 6, currentPlayer = "Rome")

        val resolved = resolveLocalSnapshotForEntry(onlineForbidGame, currentGameId = null, localSnapshot = staleSnapshot)

        assertNull("A snapshot from another turn must not be used", resolved)
        assertFalse("A rejected snapshot must not be flagged up-to-date", staleSnapshot.isUpToDate)
    }

    @Test
    fun `server state wins when the snapshot is from a later turn`() {
        val futureSnapshot = game(gameId = "g1", turns = 8, currentPlayer = "Rome")

        // The server state is authoritative about which turn is being played right now
        assertNull("A snapshot from another turn must not be used",
            resolveLocalSnapshotForEntry(onlineForbidGame, currentGameId = null, localSnapshot = futureSnapshot))
    }

    @Test
    fun `server state wins when the snapshot is for a different current player`() {
        val snapshot = game(gameId = "g1", turns = 7, currentPlayer = "Greece")

        assertNull("A snapshot for a different current player must not be used",
            resolveLocalSnapshotForEntry(onlineForbidGame, currentGameId = null, localSnapshot = snapshot))
    }

    @Test
    fun `accepted snapshot is flagged up-to-date regardless of its previous flag`() {
        val snapshot = game(gameId = "g1", turns = 7, currentPlayer = "Rome").apply { isUpToDate = false }

        val resolved = resolveLocalSnapshotForEntry(onlineForbidGame, currentGameId = null, localSnapshot = snapshot)

        assertSame(snapshot, resolved)
        assertTrue("Accepted snapshot must always be flagged up-to-date", resolved!!.isUpToDate)
    }
}
