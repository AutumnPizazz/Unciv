package com.unciv.models.metadata

import com.unciv.UncivGame
import com.unciv.logic.GameInfo
import com.unciv.models.ruleset.unique.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.random.Random

class GameSettingsTests {
    private fun getRandomSources(
        allowRandomVariance: Boolean,
        isOnlineMultiplayer: Boolean
    ): Pair<Random, Random> {
        UncivGame.Current = UncivGame().apply {
            settings = GameSettings().apply {
                this.allowRandomVariance = allowRandomVariance
            }
        }
        val gameInfo = GameInfo().apply {
            gameParameters.isOnlineMultiplayer = isOnlineMultiplayer
        }
        val context = GameContext(gameInfo = gameInfo)
        return context.stateBasedRandom("GameSettingsTests") to
            context.stateBasedRandom("GameSettingsTests")
    }

    private fun assertDeterministic(sources: Pair<Random, Random>) {
        assertNotSame(sources.first, sources.second)
        assertEquals(sources.first.nextLong(), sources.second.nextLong())
    }

    private fun assertVariable(sources: Pair<Random, Random>) {
        assertSame(Random, sources.first)
        assertSame(sources.first, sources.second)
    }

    @Test
    fun `Random variance is disabled for offline games when setting is off`() {
        assertDeterministic(getRandomSources(
            allowRandomVariance = false,
            isOnlineMultiplayer = false
        ))
    }

    @Test
    fun `Random variance is enabled for offline games when setting is on`() {
        assertVariable(getRandomSources(
            allowRandomVariance = true,
            isOnlineMultiplayer = false
        ))
    }

    @Test
    fun `Random variance is disabled for online multiplayer when setting is off`() {
        assertDeterministic(getRandomSources(
            allowRandomVariance = false,
            isOnlineMultiplayer = true
        ))
    }

    @Test
    fun `Random variance is disabled for online multiplayer when setting is on`() {
        assertDeterministic(getRandomSources(
            allowRandomVariance = true,
            isOnlineMultiplayer = true
        ))
    }

    @Test
    fun `Hotseat games retain random variance when setting is on`() {
        assertVariable(getRandomSources(
            allowRandomVariance = true,
            isOnlineMultiplayer = false
        ))
    }
}
