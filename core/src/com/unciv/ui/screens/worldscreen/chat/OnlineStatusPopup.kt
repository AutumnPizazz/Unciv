package com.unciv.ui.screens.worldscreen.chat

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.input.onClick
import com.unciv.ui.images.IconTextButton
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popups.Popup
import com.unciv.ui.screens.worldscreen.WorldScreen

class OnlineStatusPopup(
    private val worldScreen: WorldScreen
) : Popup(screen = worldScreen, scrollable = Scrollability.None) {

    private val playerTable = Table(skin)

    init {
        // Header: title + close button
        val headerLabel = "Online Players".toLabel(fontSize = 24, alignment = Align.center)
        add(headerLabel).left().pad(10f).expandX()
        add(
            ImageButton(ImageGetter.getImage("OtherIcons/Close").drawable)
                .onClick { close() }
        ).right().size(headerLabel.height * 1.3f).row()
        addSeparator()

        add(playerTable).colspan(2).row()
        addSeparator()

        // Refresh button
        val refreshButton = IconTextButton("Refresh", ImageGetter.getImage("OtherIcons/Chat"), 18)
        refreshButton.onClick { refresh() }
        add(refreshButton).colspan(2).center().pad(10f).row()

        // Send initial query and populate
        worldScreen.sendOnlineQuery()
        rebuildPlayerTable()
    }

    private fun rebuildPlayerTable() {
        playerTable.clear()

        val humans = worldScreen.gameInfo.civilizations.filter { it.isHuman() && it.isAlive() }

        if (humans.isEmpty()) {
            playerTable.add("No other players online".toLabel()).center().pad(10f).row()
            return
        }

        for (civ in humans) {
            val isOnline = worldScreen.isPlayerOnline(civ.civName)
            val dotColor = when {
                isOnline -> Color.GREEN
                worldScreen.playerOnlineTimes.containsKey(civ.civName) -> Color.RED
                else -> Color.GRAY
            }

            val dot = "●".toLabel(fontSize = 18).apply { color = dotColor }
            val nameLabel = civ.civName.toLabel(fontSize = 18)

            playerTable.add(dot).padRight(8f)
            playerTable.add(nameLabel).left().pad(5f).row()
        }
    }

    private fun refresh() {
        worldScreen.sendOnlineQuery()
        rebuildPlayerTable()
    }
}
