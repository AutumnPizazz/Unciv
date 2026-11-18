package com.unciv.logic

import com.unciv.json.json
import com.unciv.logic.files.UncivFiles
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestAssets
import com.unciv.testing.TestGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(BaseTestRunner::class)
class GameInfoRulesetTests {
    @Test
    fun loadsRealSaveFromCompatibilityVersionFour() {
        // Reuse the shipped vanilla browser fixture; no Web build or browser is needed to load it.
        val fixture = TestAssets.repoRoot().resolve("web/validation/reflection-field/src/main/resources/validation/browser-game-save.json")
        val saved = UncivFiles.gameInfoFromString(fixture.readText())
        assertEquals(4, saved.version.number)
        assertEquals(1, saved.turns)
        assertEquals("Prince", saved.getDifficulty().name)
        assertEquals(saved.gameParameters.speed, saved.speed.name)
    }

    @Test
    fun savedDifficultyAndSpeedKeepTheirNamesAfterLoad() {
        val saved = savedGame(online = false)
        saved.setTransients()
        assertEquals(saved.difficulty, saved.getDifficulty().name)
        assertEquals(saved.gameParameters.speed, saved.speed.name)
    }

    @Test
    fun missingDifficultyIsReportedForOnlineAndOfflineSaves() {
        for (online in listOf(false, true)) {
            val saved = savedGame(online).apply { difficulty = "Removed difficulty" }
            val error = assertThrows(UncivShowableException::class.java) { saved.setTransients() }
            assertEquals("The ruleset does not define the saved difficulty [Removed difficulty].", error.message)
        }
    }

    @Test
    fun missingGameSpeedIsReportedForOnlineAndOfflineSaves() {
        for (online in listOf(false, true)) {
            val saved = savedGame(online).apply { gameParameters.speed = "Removed speed" }
            val error = assertThrows(UncivShowableException::class.java) { saved.setTransients() }
            assertEquals("The ruleset does not define the saved game speed [Removed speed].", error.message)
        }
    }

    private fun savedGame(online: Boolean): GameInfo {
        val game = TestGame()
        game.makeHexagonalMap(2)
        val player = game.addCiv(game.ruleset.nations.getValue("Rome"), isPlayer = true)
        game.addCity(player, game.getTile(0, 0))
        game.gameInfo.gameParameters.isOnlineMultiplayer = online
        return json().fromJson(GameInfo::class.java, json().toJson(game.gameInfo))
    }
}
