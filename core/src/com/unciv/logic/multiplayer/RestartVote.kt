package com.unciv.logic.multiplayer

import com.unciv.logic.GameInfo
import yairm210.purity.annotations.Readonly

/** Lifecycle of a restart vote. Persisted to the multiplayer server as `{gameId}_restartvote`. */
enum class RestartVoteStatus { OPEN, SETTLED }

/**
 * A restart vote for an online multiplayer game.
 *
 * Stored as a separate file on the multiplayer server (never inside the game file), so that any
 * player - not just the current-turn player - can safely vote without conflicting with turn uploads.
 *
 * Settlement rule:
 * - The vote settles as soon as ALL alive human players have voted, or when the timeout
 *   ([timeoutMinutes] after [startedAtMillis]) is reached - players who didn't vote by then
 *   count as agreeing (默认同意).
 * - Restart passes iff (yes votes + non-voters) >= ceil(alive human players / 2),
 *   i.e. rejection requires more than half of the players to explicitly vote no.
 *
 * Offline players keep their vote: they can still vote after coming back online, as long as the
 * vote is still [RestartVoteStatus.OPEN].
 */
class RestartVote {
    /** Signal prefix for restart vote messages relayed through the WebSocket chat channel.
     *  Messages are only signals - the authoritative state is always the server-side vote file. */
    companion object {
        const val PROTOCOL_PREFIX = "[vote]"
    }

    /** Global turn number when the vote was initiated (== GameParameters.restartVoteTurn). */
    var targetTurn = 0
    /** playerId of the player who initiated the vote. */
    var initiatorPlayerId = ""
    /** Epoch millis when the vote was initiated - base of the timeout calculation. */
    var startedAtMillis = 0L
    /** Timeout in minutes; players who didn't vote by then count as agreeing. */
    var timeoutMinutes = 24 * 60
    /** playerId -> true (yes) / false (no). Casting again overwrites (allows changing the vote). */
    var votes = LinkedHashMap<String, Boolean>()
    var status = RestartVoteStatus.OPEN
    /** Settlement result: true = restart, false = rejected. null while [status] is OPEN. */
    var result: Boolean? = null

    /** Whether the given player already cast a vote. */
    @Readonly
    fun hasVoted(playerId: String) = votes.containsKey(playerId)

    /**
     * Settles the vote when possible. Deterministic and idempotent - safe to call from any client;
     * concurrent calls with the same inputs produce the same result.
     *
     * @param now Current time in epoch millis
     * @param aliveHumanPlayerIds playerIds of currently alive human (non-spectator) players
     * @return true if THIS call performed the OPEN -> SETTLED transition
     */
    fun trySettle(now: Long, aliveHumanPlayerIds: Set<String>): Boolean {
        if (status == RestartVoteStatus.SETTLED) return false
        if (aliveHumanPlayerIds.isEmpty()) return false

        val allVoted = aliveHumanPlayerIds.all { it in votes }
        val timeoutReached = now - startedAtMillis >= timeoutMinutes * 60_000L
        if (!allVoted && !timeoutReached) return false

        // Votes of players who were later eliminated/resigned are kept - their intent was expressed.
        val yesCount = votes.count { it.value } + aliveHumanPlayerIds.count { it !in votes }
        // Passes iff yesCount >= ceil(N/2); yesCount * 2 >= N is the equivalent integer check.
        result = yesCount * 2 >= aliveHumanPlayerIds.size
        status = RestartVoteStatus.SETTLED
        return true
    }
}

/** Execution state of a decided restart, persisted as `{gameId}_restart` on the multiplayer server. */
enum class RestartMarkerStatus { RESTARTING, DONE }

/**
 * Guards the "exactly once" execution of a restart.
 *
 * [RestartMarkerStatus.RESTARTING] is written by the first client that picks the restart up;
 * if that client crashes before finishing, others take over after a takeover timeout.
 */
class RestartMarker {
    var status = RestartMarkerStatus.RESTARTING
    /** playerId of the client currently (or last) executing the restart. */
    var executorPlayerId = ""
    /** Epoch millis when the current executor started. */
    var startedAtMillis = 0L
    /** gameId of the restarted game - valid once [status] is DONE. */
    var newGameId = ""
}

/** playerIds of all currently alive human (non-spectator) players of [this] game. */
fun GameInfo.getAliveHumanPlayerIds(): Set<String> = civilizations
    .filter { it.isHuman() && it.isAlive() && !it.isSpectator() }
    .map { it.playerId }
    .toSet()
