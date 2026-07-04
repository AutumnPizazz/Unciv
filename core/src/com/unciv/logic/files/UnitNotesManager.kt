package com.unciv.logic.files

import com.badlogic.gdx.files.FileHandle
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.map.mapunit.MapUnit

/**
 * Manages per-player, per-save private notes on foreign units.
 * Notes are stored locally as a companion file to the save file (e.g., "MyGame_notes").
 * They are NOT part of shared game state and are invisible to other players.
 */
object UnitNotesManager {

    private const val NOTES_SUFFIX = "_notes"

    /** Build a note key for a unit: "ownerCivName|unitId" */
    fun noteKey(unit: MapUnit): String = "${unit.owner}|${unit.id}"

    /** Get the notes companion file for a save file name */
    fun getNotesFile(gameName: String): FileHandle? {
        if (gameName.isBlank()) return null
        return UncivGame.Current.files.getSave("$gameName$NOTES_SUFFIX")
    }

    /** Get the notes companion file for the current game */
    fun getNotesFile(gameInfo: GameInfo): FileHandle? {
        val fileName = gameInfo.loadedSaveFileName ?: return null
        return getNotesFile(fileName)
    }

    /** Load all notes for the current game */
    fun loadNotes(gameInfo: GameInfo): HashMap<String, String> {
        val file = getNotesFile(gameInfo) ?: return HashMap()
        if (!file.exists()) return HashMap()
        return try {
            json().fromJson(HashMap::class.java, file) as? HashMap<String, String> ?: HashMap()
        } catch (ex: Exception) {
            HashMap()
        }
    }

    /** Save all notes for the current game */
    fun saveNotes(gameInfo: GameInfo, notes: HashMap<String, String>) {
        val file = getNotesFile(gameInfo) ?: return
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

    /** Delete the notes companion file for a given save name. Returns true if a file was deleted. */
    fun deleteNotesFile(gameName: String): Boolean {
        val file = getNotesFile(gameName) ?: return false
        return if (file.exists()) file.delete() else false
    }
}
