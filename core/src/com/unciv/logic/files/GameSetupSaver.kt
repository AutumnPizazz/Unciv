package com.unciv.logic.files

import com.badlogic.gdx.files.FileHandle
import com.unciv.UncivGame

/**
 * Persists new-game setups ("Save current setup" / "Load saved setup" on the New Game screen)
 * as named files under [SETUP_FOLDER].
 *
 * The files hold the same text format as the clipboard export (see
 * [com.unciv.models.metadata.GameSetupClipboard]), so a saved slot can be restored
 * without the clipboard.
 */
object GameSetupSaver {
    const val SETUP_FOLDER = "SaveFiles/GameSetup"
    const val SETUP_FILE_EXTENSION = ".uncivsetup"

    private fun getFolder() = UncivGame.Current.files.getLocalFile(SETUP_FOLDER)

    fun getFile(name: String) = getFolder().child("$name$SETUP_FILE_EXTENSION")

    /** Save [setupText] to the slot [name], overwriting an existing slot of the same name. */
    fun save(setupText: String, name: String) {
        val file = getFile(name)
        file.parent().mkdirs()
        file.writeString(setupText, false, Charsets.UTF_8.name())
    }

    /** All saved setups, sorted by slot name. */
    fun listSetups(): List<FileHandle> {
        val folder = getFolder()
        if (!folder.exists()) return emptyList()
        return folder.list()
            .filter { !it.isDirectory && it.name().endsWith(SETUP_FILE_EXTENSION) }
            .sortedBy { it.nameWithoutExtension() }
    }

    /** Delete the saved setup [setup]. */
    fun delete(setup: FileHandle) = setup.delete()
}
