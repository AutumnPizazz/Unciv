package com.unciv.logic.files

import com.badlogic.gdx.files.FileHandle
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.map.mapunit.MapUnit

/**
 * Manages per-player, per-game private notes on foreign units.
 *
 * Notes are keyed by **gameId** (not by save file name): one game - whether reached through
 * its manual save, autosave, or a save-as copy - shares the same notes, while different games
 * are strictly isolated. This fixes cross-save leaks that happened through the shared
 * "Autosave" file name and notes getting lost in unsaved new games.
 *
 * Notes are stored locally as a companion file of the save (e.g., "notes_<gameId>").
 * They are NOT part of shared game state and are invisible to other players.
 * Legacy files ("<saveName>_notes") are migrated on first load.
 */
object UnitNotesManager {

    /** Legacy naming: "<saveName>_notes" */
    private const val NOTES_SUFFIX = "_notes"
    /** Current naming: "notes_<gameId>" */
    private const val NOTES_PREFIX = "notes_"

    /** In-memory cache of loaded notes, keyed by gameId */
    private var cachedGameId: String? = null
    private var cachedNotes: HashMap<String, String>? = null

    /** Build a note key for a unit: "owner|unitId" */
    fun noteKey(unit: MapUnit): String = "${unit.owner}|${unit.id}"

    /** Build a note key for a tile: "tile|x,y" */
    fun tileNoteKey(x: Int, y: Int): String = "tile|$x,$y"

    /** Get the notes companion file for a game (keyed by its gameId) */
    fun getNotesFile(gameInfo: GameInfo): FileHandle? {
        val gameId = gameInfo.gameId
        if (gameId.isBlank()) return null
        return UncivGame.Current.files.getSave("$NOTES_PREFIX$gameId")
    }

    /** Legacy notes file ("<saveName>_notes") for migration of older saves */
    private fun getLegacyNotesFile(gameInfo: GameInfo): FileHandle? {
        val fileName = gameInfo.loadedSaveFileName
        if (fileName.isNullOrBlank()) return null
        return UncivGame.Current.files.getSave("$fileName$NOTES_SUFFIX")
    }

    /** Load all notes for the current game (with caching) */
    fun loadNotes(gameInfo: GameInfo): HashMap<String, String> {
        val gameId = gameInfo.gameId
        if (gameId.isBlank()) return HashMap()

        // Return cached notes if the game hasn't changed
        if (cachedGameId == gameId && cachedNotes != null)
            return cachedNotes!!

        val notesFile = getNotesFile(gameInfo) ?: return HashMap()
        if (!notesFile.exists()) {
            // Migration: did an older version store notes under "<saveName>_notes"?
            val legacyFile = getLegacyNotesFile(gameInfo)
            if (legacyFile != null && legacyFile.exists()) {
                val legacyNotes = readNotesFrom(legacyFile)
                cachedGameId = gameId
                cachedNotes = legacyNotes
                saveNotes(gameInfo, legacyNotes)
                legacyFile.delete()
                return cachedNotes!!
            }
            cachedGameId = gameId
            cachedNotes = HashMap()
            return cachedNotes!!
        }
        cachedGameId = gameId
        cachedNotes = readNotesFrom(notesFile)
        return cachedNotes!!
    }

    private fun readNotesFrom(file: FileHandle): HashMap<String, String> =
        try {
            json().fromJson(HashMap::class.java, file) as? HashMap<String, String> ?: HashMap()
        } catch (ex: Exception) {
            cachedGameId = null
            cachedNotes = null
            HashMap()
        }

    /** Save all notes for the current game */
    fun saveNotes(gameInfo: GameInfo, notes: HashMap<String, String>) {
        val file = getNotesFile(gameInfo) ?: return
        cachedGameId = gameInfo.gameId
        cachedNotes = notes  // Update cache
        if (notes.isEmpty()) {
            if (file.exists()) file.delete()
            return
        }
        file.writeString(json().toJson(notes), false, Charsets.UTF_8.name())
    }

    /** Get the note for a specific foreign unit */
    fun getNote(gameInfo: GameInfo, unit: MapUnit): String? {
        val notes = loadNotes(gameInfo)
        return notes[noteKey(unit)]
    }

    /** Set (add or update) the note for a specific foreign unit */
    fun setNote(gameInfo: GameInfo, unit: MapUnit, note: String) {
        val notes = loadNotes(gameInfo)
        if (note.isBlank()) {
            notes.remove(noteKey(unit))
        } else {
            notes[noteKey(unit)] = note
        }
        saveNotes(gameInfo, notes)
    }

    /** Delete the note for a specific foreign unit */
    fun deleteNote(gameInfo: GameInfo, unit: MapUnit) {
        val notes = loadNotes(gameInfo)
        notes.remove(noteKey(unit))
        saveNotes(gameInfo, notes)
    }

    /** Get the note for a specific tile */
    fun getTileNote(gameInfo: GameInfo, x: Int, y: Int): String? {
        val notes = loadNotes(gameInfo)
        return notes[tileNoteKey(x, y)]
    }

    /** Set (add or update) the note for a specific tile */
    fun setTileNote(gameInfo: GameInfo, x: Int, y: Int, note: String) {
        val notes = loadNotes(gameInfo)
        if (note.isBlank()) {
            notes.remove(tileNoteKey(x, y))
        } else {
            notes[tileNoteKey(x, y)] = note
        }
        saveNotes(gameInfo, notes)
    }

    /** Delete the note for a specific tile */
    fun deleteTileNote(gameInfo: GameInfo, x: Int, y: Int) {
        val notes = loadNotes(gameInfo)
        notes.remove(tileNoteKey(x, y))
        saveNotes(gameInfo, notes)
    }

    /** Delete the notes companion file for a given gameId (current naming). Returns true if a file was deleted. */
    fun deleteNotesFile(gameId: String): Boolean {
        if (gameId.isBlank()) return false
        val file = UncivGame.Current.files.getSave("$NOTES_PREFIX$gameId")
        return if (file.exists()) file.delete() else false
    }

    /** Delete a legacy notes companion file for a given save name. Returns true if a file was deleted. */
    fun deleteLegacyNotesFile(saveName: String): Boolean {
        if (saveName.isBlank()) return false
        val file = UncivGame.Current.files.getSave("$saveName$NOTES_SUFFIX")
        return if (file.exists()) file.delete() else false
    }
}
