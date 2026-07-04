package com.unciv.ui.screens.pickerscreens

import com.unciv.logic.GameInfo
import com.unciv.logic.files.UnitNotesManager
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.models.translations.tr
import com.unciv.ui.components.extensions.surroundWithCircle
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popups.AskTextPopup
import com.unciv.ui.screens.basescreen.BaseScreen

/** Popup for adding/editing a private note on a foreign unit */
class UnitNotePopup(
    val screen: BaseScreen,
    val unit: MapUnit,
    val gameInfo: GameInfo,
    val actionOnClose: () -> Unit
) {
    init {
        val existingNote = UnitNotesManager.getNote(gameInfo, unit)
        val label = "Note for [${unit.baseUnit.name}]".tr() + " (${unit.civ.civName.tr()})"

        AskTextPopup(
            screen,
            label = label,
            icon = ImageGetter.getUnitIcon(unit.baseUnit).surroundWithCircle(80f),
            defaultText = existingNote ?: "",
            maxLength = 64,
            actionOnOk = { userInput ->
                if (userInput.isBlank() && existingNote == null) {
                    // No existing note and blank input: do nothing
                } else if (userInput.isBlank()) {
                    // Clearing an existing note
                    UnitNotesManager.deleteNote(gameInfo, unit)
                } else {
                    UnitNotesManager.setNote(gameInfo, unit, userInput)
                }
                actionOnClose()
            }
        ).open()
    }
}
