package com.unciv.ui.screens.pickerscreens

import com.badlogic.gdx.scenes.scene2d.Group
import com.unciv.UncivGame
import com.unciv.logic.GameInfo
import com.unciv.logic.files.UnitNotesManager
import com.unciv.logic.map.tile.Tile
import com.unciv.models.translations.tr
import com.unciv.ui.components.tilegroups.TileGroup
import com.unciv.ui.components.tilegroups.TileSetStrings
import com.unciv.ui.popups.AskTextPopup
import com.unciv.ui.screens.worldscreen.WorldScreen
import com.unciv.view.CivView
import com.unciv.view.TileView

/** Popup for editing a tile note (map pin) */
fun TileNotePopup(
    worldScreen: WorldScreen,
    tile: Tile,
    gameInfo: GameInfo,
    onComplete: () -> Unit
) {
    val existingNote = UnitNotesManager.getTileNote(gameInfo, tile.position.x, tile.position.y)

    AskTextPopup(
        screen = worldScreen,
        label = "Add note for tile".tr(),
        icon = tile.getTileGroupIcon(viewer = worldScreen.getGameViewConsideringForOfWar().civView),
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

/** Renders the tile's texture set (terrain, resources, improvements, rivers) as a preview icon */
internal fun Tile.getTileGroupIcon(size: Float = 80f, viewer: CivView? = null): Group {
    setTerrainTransients()
    return TileGroup(
        TileView.forSingleTile(this, viewer?.getCiv()),
        TileSetStrings(ruleset, UncivGame.Current.settings),
        size * 36f / 54f  // TileGroup normally spills out of its bounding box
    ).apply {
        isForceVisible = true
        isForMapEditorIcon = true
        update(viewer)
    }
}
