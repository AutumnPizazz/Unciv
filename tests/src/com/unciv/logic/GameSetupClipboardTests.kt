package com.unciv.logic

import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapSize
import com.unciv.models.metadata.GameParameters
import com.unciv.models.metadata.GameSetupClipboard
import com.unciv.models.metadata.GameSetupInfo
import com.unciv.models.metadata.Player
import com.unciv.testing.GdxTestRunner
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(GdxTestRunner::class)
class GameSetupClipboardTests {

    private val playerId1 = "11111111-1111-1111-1111-111111111111"
    private val playerId2 = "22222222-2222-2222-2222-222222222222"

    private fun makeSetup() = GameSetupInfo(
        GameParameters().apply {
            difficulty = "Deity"
            speed = "Quick"
            randomNumberOfPlayers = true
            minNumberOfPlayers = 2
            maxNumberOfPlayers = 5
            players.clear()
            players.add(Player("Rome", PlayerType.Human, playerId1))
            players.add(Player("Greece", PlayerType.Human, playerId2))
            players.add(Player("Siam", PlayerType.AI, ""))
            randomNumberOfCityStates = true
            numberOfCityStates = 8
            noCityRazing = true
            noBarbarians = true
            ragingBarbarians = true
            oneCityChallenge = true
            nuclearWeaponsEnabled = false
            espionageEnabled = true
            noStartBias = true
            shufflePlayerOrder = true
            victoryTypes = arrayListOf("Domination", "Science", "Diplomatic")
            startingEra = "Industrial era"
            showCivilizationStats = false
            showDemographics = true
            showRankings = false
            showCharts = true
            hideOtherCivilizationStats = true
            isOnlineMultiplayer = true
            multiplayerServerUrl = "https://example.com/server"
            anyoneCanSpectate = false
            minutesUntilSkipTurn = 60
            minutesUntilForceResign = 120
            minutesRecoveredPerTurn = 30
            pollingIntervalSeconds = 10
            requireSameVersion = true
            baseRuleset = "Civ V - Vanilla"
            mods = LinkedHashSet(listOf("Test Mod", "Another Mod"))
            maxTurns = 300
            acceptedModCheckErrors = "some errors"
        },
        MapParameters().apply {
            name = "MyMap"
            type = "Pangaea"
            shape = "Hexagonal"
            mapSize = MapSize(8)
            noRuins = true
            noNaturalWonders = true
            worldWrap = true
            strategicBalance = true
            legendaryStart = true
            seed = 123456789L
            tilesPerBiomeArea = 10
            maxCoastExtension = 3
            elevationExponent = 0.7f
            temperatureintensity = 0.65f
            temperatureShift = 0.1f
            vegetationRichness = 0.5f
            rareFeaturesRichness = 0.2f
            resourceRichness = 0.3f
            waterThreshold = 0.05f
            mods = LinkedHashSet(listOf("Map Mod"))
            baseRuleset = "Civ V - Vanilla"
        }
    )

    @Test
    fun roundTripPreservesAllValues() {
        val original = makeSetup()
        val encoded = GameSetupClipboard.encode(original)
        Assert.assertTrue(encoded.startsWith("UncivGameSetup:1:"))

        val decoded = GameSetupClipboard.decode(encoded)
        val params = decoded.gameParameters
        Assert.assertEquals("Deity", params.difficulty)
        Assert.assertEquals("Quick", params.speed)
        Assert.assertTrue(params.randomNumberOfPlayers)
        Assert.assertEquals(2, params.minNumberOfPlayers)
        Assert.assertEquals(5, params.maxNumberOfPlayers)
        Assert.assertTrue(params.randomNumberOfCityStates)
        Assert.assertEquals(8, params.numberOfCityStates)
        Assert.assertTrue(params.noCityRazing)
        Assert.assertTrue(params.noBarbarians)
        Assert.assertTrue(params.ragingBarbarians)
        Assert.assertTrue(params.oneCityChallenge)
        Assert.assertFalse(params.nuclearWeaponsEnabled)
        Assert.assertTrue(params.espionageEnabled)
        Assert.assertTrue(params.noStartBias)
        Assert.assertTrue(params.shufflePlayerOrder)
        Assert.assertEquals(arrayListOf("Domination", "Science", "Diplomatic"), params.victoryTypes)
        Assert.assertEquals("Industrial era", params.startingEra)
        Assert.assertEquals(false, params.showCivilizationStats)
        Assert.assertTrue(params.showDemographics)
        Assert.assertFalse(params.showRankings)
        Assert.assertTrue(params.showCharts)
        Assert.assertTrue(params.hideOtherCivilizationStats)
        Assert.assertTrue(params.isOnlineMultiplayer)
        Assert.assertEquals("https://example.com/server", params.multiplayerServerUrl)
        Assert.assertFalse(params.anyoneCanSpectate)
        Assert.assertEquals(60, params.minutesUntilSkipTurn)
        Assert.assertEquals(120, params.minutesUntilForceResign)
        Assert.assertEquals(30, params.minutesRecoveredPerTurn)
        Assert.assertEquals(10, params.pollingIntervalSeconds)
        Assert.assertTrue(params.requireSameVersion)
        Assert.assertEquals("Civ V - Vanilla", params.baseRuleset)
        Assert.assertEquals(LinkedHashSet(listOf("Test Mod", "Another Mod")), params.mods)
        Assert.assertEquals(300, params.maxTurns)
        Assert.assertEquals("some errors", params.acceptedModCheckErrors)

        // Multiplayer player IDs must survive the round trip
        Assert.assertEquals(3, params.players.size)
        Assert.assertEquals("Rome", params.players[0].chosenCiv)
        Assert.assertEquals(PlayerType.Human, params.players[0].playerType)
        Assert.assertEquals(playerId1, params.players[0].playerId)
        Assert.assertEquals("Greece", params.players[1].chosenCiv)
        Assert.assertEquals(playerId2, params.players[1].playerId)
        Assert.assertEquals("Siam", params.players[2].chosenCiv)
        Assert.assertEquals(PlayerType.AI, params.players[2].playerType)
        Assert.assertEquals("", params.players[2].playerId)

        val map = decoded.mapParameters
        Assert.assertEquals("MyMap", map.name)
        Assert.assertEquals("Pangaea", map.type)
        Assert.assertEquals("Hexagonal", map.shape)
        Assert.assertEquals(8, map.mapSize.radius)
        Assert.assertTrue(map.noRuins)
        Assert.assertTrue(map.noNaturalWonders)
        Assert.assertTrue(map.worldWrap)
        Assert.assertTrue(map.strategicBalance)
        Assert.assertTrue(map.legendaryStart)
        Assert.assertEquals(123456789L, map.seed)
        Assert.assertEquals(10, map.tilesPerBiomeArea)
        Assert.assertEquals(3, map.maxCoastExtension)
        Assert.assertEquals(0.7f, map.elevationExponent, 0.0001f)
        Assert.assertEquals(0.65f, map.temperatureintensity, 0.0001f)
        Assert.assertEquals(0.1f, map.temperatureShift, 0.0001f)
        Assert.assertEquals(0.5f, map.vegetationRichness, 0.0001f)
        Assert.assertEquals(0.2f, map.rareFeaturesRichness, 0.0001f)
        Assert.assertEquals(0.3f, map.resourceRichness, 0.0001f)
        Assert.assertEquals(0.05f, map.waterThreshold, 0.0001f)
        Assert.assertEquals(LinkedHashSet(listOf("Map Mod")), map.mods)
    }

    @Test
    fun decodeRejectsInvalidText() {
        val invalidInputs = listOf(
            "",
            "random text",
            "UncivGameSetup:2:c29tZXRoaW5n",
            "UncivGameSetup:1:!!!not base64!!!",
            "UncivGameSetup:1:",
            "OtherFormat:1:c29tZXRoaW5n"
        )
        for (invalid in invalidInputs) {
            try {
                GameSetupClipboard.decode(invalid)
                Assert.fail("Expected decode to throw for: '$invalid'")
            } catch (_: IllegalArgumentException) {
                // expected
            }
        }
    }

    @Test
    fun copyValuesIntoKeepsTargetInstancesAndCopiesValues() {
        val target = GameSetupInfo()  // default values
        val targetParameters = target.gameParameters
        val targetMapParameters = target.mapParameters
        val source = makeSetup()

        GameSetupClipboard.copyValuesInto(target, source)

        // The instances themselves must be preserved (the UI holds references to them)
        Assert.assertSame(targetParameters, target.gameParameters)
        Assert.assertSame(targetMapParameters, target.mapParameters)

        // All values must be copied
        Assert.assertEquals("Deity", target.gameParameters.difficulty)
        Assert.assertEquals("Quick", target.gameParameters.speed)
        Assert.assertTrue(target.gameParameters.isOnlineMultiplayer)
        Assert.assertEquals(3, target.gameParameters.players.size)
        Assert.assertEquals(playerId1, target.gameParameters.players[0].playerId)
        Assert.assertEquals(playerId2, target.gameParameters.players[1].playerId)
        Assert.assertEquals(123456789L, target.mapParameters.seed)
        Assert.assertEquals("Pangaea", target.mapParameters.type)
        Assert.assertEquals(LinkedHashSet(listOf("Test Mod", "Another Mod")), target.gameParameters.mods)

        // The default target values must be replaced, not merged with stale ones
        Assert.assertNotEquals(GameParameters().difficulty, target.gameParameters.difficulty)
        Assert.assertNotEquals(MapParameters().seed, target.mapParameters.seed)
    }

    @Test
    fun copyValuesIntoDoesNotTouchTransientMapFile() {
        // mapFile is @Transient and must not be part of the serialized setup at all
        val target = GameSetupInfo()
        GameSetupClipboard.copyValuesInto(target, makeSetup())
        Assert.assertNull(target.mapFile)
    }
}
