package com.unciv.ui.screens.pickerscreens

import com.unciv.logic.GameInfo
import com.unciv.logic.files.UnitNotesManager
import com.unciv.logic.map.tile.Tile
import com.unciv.models.translations.tr
import com.unciv.ui.components.extensions.surroundWithCircle
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popups.AskTextPopup
import com.unciv.ui.screens.worldscreen.WorldScreen

/** Popup for editing a tile note (map pin) */
fun TileNotePopup(
    worldScreen: WorldScreen,
    tile: Tile,
    gameInfo: GameInfo,
    onComplete: () -> Unit
) {
    val tilePos = "${tile.position.x},${tile.position.y}"
    val existingNote = UnitNotesManager.getTileNote(gameInfo, tile.position.x, tile.position.y)
    val label = "Add note for tile".tr() + " ($tilePos)"

    AskTextPopup(
        screen = worldScreen,
        label = label,
        icon = ImageGetter.getImage("OtherIcons/ExclamationMark").surroundWithCircle(80f),
        defaultText = existingNote ?: "",
        maxLength = 64,
        actionOnOk = { note ->
            if (note.isBlank() && existingNote == null) {
                // No existing note and blank input: do nothing
            } else if (note.isBlank()) {
                UnitNotesManager.deleteTileNote(gameInfo, tile.position.x, tile.position.y)
            } else {
                UnitNotesManager.setTileNote(gameInfo, tile.position.x, tile.position.y, note)
            }
            onComplete()
        }
    ).open()
}
