package com.unciv.logic.multiplayer

import com.badlogic.gdx.Gdx
import com.unciv.UncivGame
import com.unciv.logic.GameInfo
import com.unciv.logic.GameStarter
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.files.UncivFiles
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapSize
import com.unciv.models.metadata.GameParameters
import com.unciv.models.metadata.GameSetupInfo
import com.unciv.models.metadata.GameSettings
import com.unciv.models.metadata.Player
import com.unciv.models.ruleset.RulesetCache
import com.unciv.testing.BaseTestRunner
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(BaseTestRunner::class)
class LocalSnapshotTest {

    private lateinit var files: UncivFiles
    private lateinit var game: GameInfo
    private lateinit var settingsBackup: GameSettings

    @Before
    fun prepare() {
        RulesetCache.loadRulesets(noMods = true)

        val param = GameParameters().apply {
            numberOfCityStates = 0
            players.clear()
            players.add(Player("Rome", PlayerType.Human))
            players.add(Player("Greece"))
        }
        val mapParameters = MapParameters().apply {
            mapSize = MapSize.Tiny
            seed = 42L
        }
        val setup = GameSetupInfo(param, mapParameters)
        UncivGame.Current = UncivGame()
        files = UncivFiles(Gdx.files)
        UncivGame.Current.files = files
        settingsBackup = files.getGeneralSettings()
        UncivGame.Current.settings = GameSettings()

        game = GameStarter.startNewGame(setup)
        game.gameId = "local-snapshot-test-game"
        UncivGame.Current.gameInfo = game
    }

    @After
    fun cleanup() {
        settingsBackup.save()
    }

    @Test
    fun localSnapshotRoundTrip() {
        game.turns = 42
        game.currentPlayer = "Rome"

        val multiplayerFiles = MultiplayerFiles()
        multiplayerFiles.saveLocalSnapshot(game)

        val loaded = multiplayerFiles.loadLocalSnapshot(game.gameId)
        Assert.assertNotNull("Saved local snapshot must be loadable", loaded)
        Assert.assertEquals(42, loaded!!.turns)
        Assert.assertEquals("Rome", loaded.currentPlayer)
        Assert.assertEquals(game.gameId, loaded.gameId)
    }

    @Test
    fun snapshotsDoNotAppearAsMultiplayerPreviews() {
        val multiplayerFiles = MultiplayerFiles()
        multiplayerFiles.saveLocalSnapshot(game)

        val saveNames = files.getMultiplayerSaves().map { it.name() }.toList()
        Assert.assertFalse("Local snapshot must not be listed as a multiplayer game preview",
            saveNames.any { it.endsWith("_localstate") })
    }

    @Test
    fun loadMissingSnapshotReturnsNull() {
        val multiplayerFiles = MultiplayerFiles()
        Assert.assertNull("Missing snapshot must load as null", multiplayerFiles.loadLocalSnapshot("no-such-game"))
    }

    @Test
    fun forbidReloadMultiplayerGameDoesNotWriteAutoSave() {
        game.gameParameters.isOnlineMultiplayer = true
        game.gameParameters.forbidReload = true
        // Clear any autosave leftover from other tests
        files.getSaves().filter { it.name().contains("Autosave") }.forEach { files.deleteSave(it) }
        kotlinx.coroutines.runBlocking { files.autosaves.requestAutoSave(game).join() }
        val autoSaveFiles = files.getSaves().filter { it.name().contains("Autosave") }.toList()
        Assert.assertTrue("Forbid-reload multiplayer game must not be written to the autosave slot", autoSaveFiles.isEmpty())
    }
}
