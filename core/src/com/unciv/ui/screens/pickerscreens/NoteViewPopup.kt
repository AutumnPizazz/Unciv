package com.unciv.ui.screens.pickerscreens

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.models.translations.tr
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.extensions.toTextButton
import com.unciv.ui.components.input.onClick
import com.unciv.ui.popups.Popup
import com.unciv.ui.screens.basescreen.BaseScreen

/**
 * Popup showing the full text of a tile/unit note, with Edit / Delete / Close actions.
 * Used when tapping a truncated note bubble on the map (mobile has no hover).
 */
fun NoteViewPopup(
    screen: BaseScreen,
    note: String,
    icon: Group? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val popup = Popup(screen)

    val content = Table()
    if (icon != null) {
        content.add(icon).padRight(10f).top()
    }
    val noteLabel = note.toLabel().apply {
        wrap = true
        setAlignment(Align.topLeft)
    }
    val maxWidth = screen.stage.width * 0.6f
    content.add(noteLabel).width(maxWidth).row()

    val buttons = Table()
    if (onEdit != null) {
        buttons.add("Edit".toTextButton().onClick {
            popup.remove()
            onEdit()
        }).pad(5f)
    }
    if (onDelete != null) {
        buttons.add("Delete".toTextButton().onClick {
            popup.remove()
            onDelete()
        }).pad(5f)
    }
    buttons.add("Close".toTextButton().onClick { popup.remove() }).pad(5f)

    popup.add(content).row()
    popup.add(buttons).row()
    popup.open()
}
