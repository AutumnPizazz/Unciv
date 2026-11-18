package com.unciv.models.metadata

import com.badlogic.gdx.utils.JsonReader
import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.multiplayer.storage.MultiplayerServer
import com.unciv.models.ruleset.unique.GameContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.random.Random

class GameSettingsTests {
    @Test
    fun `Legacy server settings cannot change the community endpoint or reuse other servers' passwords`() {
        val legacySettings = """
            {
                "multiplayerServer": "Dropbox",
                "multiplayer": {
                    "server": "https://example.com",
                    "passwords": {"https://example.com": "other-password"}
                }
            }
        """.trimIndent()
        val settings = json().fromJson(GameSettings::class.java, legacySettings)
        settings.doMigrations(JsonReader().parse(legacySettings))

        assertEquals(Constants.uncivMultiplayerServer, MultiplayerServer().getServerUrl())
        assertNull(settings.multiplayer.getCurrentServerPassword())
    }

    @Test
    fun `Existing community credentials survive the removal of custom servers`() {
        val settings = json().fromJson(GameSettings::class.java, """
            {"multiplayer": {
                "server": "https://example.com",
                "userId": "existing-player",
                "passwords": {"${Constants.uncivMultiplayerServer}": "community-password"}
            }}
        """.trimIndent())

        assertEquals("existing-player", settings.multiplayer.getUserId())
        assertEquals("community-password", settings.multiplayer.getCurrentServerPassword())
    }

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
