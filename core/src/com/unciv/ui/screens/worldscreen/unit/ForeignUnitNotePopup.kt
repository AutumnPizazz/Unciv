package com.unciv.ui.screens.worldscreen.unit

import com.unciv.logic.files.UnitNotesManager
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.models.translations.tr
import com.unciv.ui.components.extensions.surroundWithCircle
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popups.Popup
import com.unciv.ui.screens.pickerscreens.UnitNotePopup
import com.unciv.ui.screens.worldscreen.WorldScreen

/** Popup shown when clicking a foreign unit, displaying unit info and a note editing button */
class ForeignUnitNotePopup(
    val worldScreen: WorldScreen,
    val unit: MapUnit
) : Popup(worldScreen) {

    init {
        val unitName = unit.displayName().tr()
        val civName = unit.civ.civName.tr()

        // Unit icon
        add(ImageGetter.getUnitIcon(unit.baseUnit).surroundWithCircle(60f)).padBottom(5f).row()
        // Unit name + civ
        add("$unitName ($civName)".toLabel(fontSize = 20)).padBottom(10f).row()

        // Existing note display
        val note = UnitNotesManager.getNote(worldScreen.gameInfo, unit)
        if (note != null) {
            add(("\uD83D\uDCDD " + note).toLabel()).padBottom(10f).row()
        }

        // Edit Note button
        addOKButton("Edit Note".tr()) {
            close()
            UnitNotePopup(worldScreen, unit, worldScreen.gameInfo) {
                ForeignUnitNotePopup(worldScreen, unit).open()
            }
        }
    }
}
