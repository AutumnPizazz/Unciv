package com.unciv.logic.files

import com.badlogic.gdx.Gdx
import com.unciv.UncivGame
import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Notes must never leak across games. They are keyed by gameId since 4.21.6.1 -
 * one game shares notes through its manual save, autosave and save-as copies,
 * while different games are strictly isolated (previously the shared "Autosave"
 * file name leaked notes between games, and unsaved new games lost their notes).
 */
@RunWith(GdxTestRunner::class)
class UnitNotesManagerTests {

    private lateinit var game: TestGame

    @Before
    fun setUp() {
        game = TestGame()
        game.makeHexagonalMap(3)
        game.addCiv()
        UncivGame.Current.files = UncivFiles(Gdx.files)  // TestGame resets Current - set files after
    }

    @After
    fun tearDown() {
        // Clean up companion note files created by these tests (they persist on disk)
        for (gameId in listOf("game-A", "game-B", "game-shared", "game-1", "game-2", "fresh-game", "migrated-game"))
            UnitNotesManager.deleteNotesFile(gameId)
        UnitNotesManager.deleteLegacyNotesFile("OldSave")
    }

    @Test
    fun `notes are isolated between different games`() {
        // Game A
        game.gameInfo.gameId = "game-A"
        UnitNotesManager.setTileNote(game.gameInfo, 1, 1, "Note from A")
        assertEquals("Note from A", UnitNotesManager.getTileNote(game.gameInfo, 1, 1))

        // Switch to game B - must not see A's notes
        game.gameInfo.gameId = "game-B"
        assertNull("B must not see A's tile note", UnitNotesManager.getTileNote(game.gameInfo, 1, 1))
        UnitNotesManager.setTileNote(game.gameInfo, 1, 1, "Note from B")
        assertEquals("Note from B", UnitNotesManager.getTileNote(game.gameInfo, 1, 1))

        // Back to A - A's note must still be there
        game.gameInfo.gameId = "game-A"
        assertEquals("Note from A", UnitNotesManager.getTileNote(game.gameInfo, 1, 1))
    }

    @Test
    fun `autosave and manual save of the same game share notes`() {
        // The same gameId reached via different file names (autosave = "Autosave") must share notes
        game.gameInfo.gameId = "game-shared"
        game.gameInfo.loadedSaveFileName = "Autosave"
        UnitNotesManager.setTileNote(game.gameInfo, 1, 1, "Shared note")
        game.gameInfo.loadedSaveFileName = "MyGame"
        assertEquals("Shared note", UnitNotesManager.getTileNote(game.gameInfo, 1, 1))
    }

    @Test
    fun `autosave of a different game does not leak notes`() {
        // Game 1 autosaves a note...
        game.gameInfo.gameId = "game-1"
        game.gameInfo.loadedSaveFileName = "Autosave"
        UnitNotesManager.setTileNote(game.gameInfo, 1, 1, "Note from game 1")

        // ...then a new game takes over the same "Autosave" file name
        game.gameInfo.gameId = "game-2"
        game.gameInfo.loadedSaveFileName = "Autosave"
        assertNull("Autosave of game 2 must not see game 1's note", UnitNotesManager.getTileNote(game.gameInfo, 1, 1))
    }

    @Test
    fun `unsaved new game keeps its notes`() {
        // A new game always has a gameId, even before the first save
        game.gameInfo.gameId = "fresh-game"
        game.gameInfo.loadedSaveFileName = null
        UnitNotesManager.setTileNote(game.gameInfo, 2, 2, "Fresh note")
        assertEquals("Fresh note", UnitNotesManager.getTileNote(game.gameInfo, 2, 2))
    }

    @Test
    fun `legacy saveName notes are migrated to gameId naming`() {
        // Simulate a legacy "<saveName>_notes" file
        game.gameInfo.gameId = "migrated-game"
        game.gameInfo.loadedSaveFileName = "OldSave"
        val legacyFile = UncivGame.Current.files.getSave("OldSave_notes")
        legacyFile.writeString("{\"tile|3,3\":\"Legacy note\"}", false, Charsets.UTF_8.name())
        try {
            assertEquals("Legacy note", UnitNotesManager.getTileNote(game.gameInfo, 3, 3))
            // The note must now live under the gameId naming
            val newFile = UncivGame.Current.files.getSave("notes_migrated-game")
            assert(newFile.exists())
            assert(!legacyFile.exists())
            // And a new note must persist across cache reloads
            UnitNotesManager.setTileNote(game.gameInfo, 3, 4, "New note")
            game.gameInfo.gameId = "migrated-game"  // same game - cached
            assertEquals("New note", UnitNotesManager.getTileNote(game.gameInfo, 3, 4))
        } finally {
            legacyFile.delete()
            UncivGame.Current.files.getSave("notes_migrated-game").delete()
        }
    }
}
