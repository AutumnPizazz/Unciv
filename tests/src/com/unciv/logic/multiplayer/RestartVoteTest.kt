package com.unciv.logic.multiplayer

import com.unciv.testing.BaseTestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(BaseTestRunner::class)
class RestartVoteTest {

    private fun newVote(timeoutMinutes: Int = 24 * 60, startedAt: Long = 0) = RestartVote().apply {
        this.timeoutMinutes = timeoutMinutes
        startedAtMillis = startedAt
    }

    @Test
    fun settlesImmediatelyWhenAllVoted() {
        val vote = newVote()
        vote.votes["a"] = true
        vote.votes["b"] = true
        vote.votes["c"] = false
        assertTrue(vote.trySettle(now = 1000, aliveHumanPlayerIds = setOf("a", "b", "c")))
        assertEquals(RestartVoteStatus.SETTLED, vote.status)
        assertTrue(vote.result!!) // 2 yes >= ceil(3/2) = 2
    }

    @Test
    fun doesNotSettleBeforeTimeoutWithMissingVotes() {
        val vote = newVote(timeoutMinutes = 60)
        vote.votes["a"] = true
        assertFalse(vote.trySettle(now = 60 * 60_000L - 1, aliveHumanPlayerIds = setOf("a", "b", "c")))
        assertEquals(RestartVoteStatus.OPEN, vote.status)
    }

    @Test
    fun timeoutCountsNonVotersAsYes() {
        val vote = newVote(timeoutMinutes = 60)
        vote.votes["a"] = true // b and c never vote
        assertTrue(vote.trySettle(now = 60 * 60_000L, aliveHumanPlayerIds = setOf("a", "b", "c")))
        assertTrue(vote.result!!) // 1 yes + 2 default = 3 >= ceil(3/2) = 2
    }

    @Test
    fun explicitMajorityNoRejects() {
        val vote = newVote(timeoutMinutes = 60)
        vote.votes["a"] = true
        vote.votes["b"] = false
        vote.votes["c"] = false
        vote.votes["d"] = false // 3 no in a 4-player game: 3 > floor(4/2) = 2
        assertTrue(vote.trySettle(now = 60 * 60_000L, aliveHumanPlayerIds = setOf("a", "b", "c", "d")))
        assertFalse(vote.result!!)
    }

    @Test
    fun halfNoStillPassesOnTimeout() {
        val vote = newVote(timeoutMinutes = 60)
        vote.votes["a"] = true
        vote.votes["b"] = false
        vote.votes["c"] = false
        vote.votes["d"] = false // 1 yes, 3 no, 2 never vote in a 6-player game
        assertTrue(vote.trySettle(now = 60 * 60_000L, aliveHumanPlayerIds = setOf("a", "b", "c", "d", "e", "f")))
        assertTrue(vote.result!!) // 1 yes + 2 default = 3 >= ceil(6/2) = 3
    }

    @Test
    fun settleIsIdempotent() {
        val vote = newVote()
        vote.votes["a"] = true
        assertTrue(vote.trySettle(0, setOf("a")))
        assertFalse(vote.trySettle(0, setOf("a"))) // already settled - no further transition
        assertEquals(RestartVoteStatus.SETTLED, vote.status)
    }

    @Test
    fun votesOfEliminatedPlayersAreKept() {
        val vote = newVote(timeoutMinutes = 60)
        vote.votes["a"] = true
        vote.votes["b"] = false // b gets eliminated before settling
        // Only a and c are alive now; c never votes, so the timeout settles it
        assertTrue(vote.trySettle(now = 60 * 60_000L, aliveHumanPlayerIds = setOf("a", "c")))
        assertTrue(vote.result!!) // a yes + c default = 2 >= ceil(2/2) = 1; b's no is kept but irrelevant
    }

    @Test
    fun noSettleWithoutPlayers() {
        val vote = newVote()
        assertFalse(vote.trySettle(0, emptySet()))
        assertEquals(RestartVoteStatus.OPEN, vote.status)
    }

    @Test
    fun voteCanBeChangedUntilSettled() {
        val vote = newVote()
        vote.votes["a"] = true
        vote.votes["a"] = false
        assertEquals(false, vote.votes["a"])
    }
}
