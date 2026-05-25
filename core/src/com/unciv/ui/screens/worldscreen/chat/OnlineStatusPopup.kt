package com.unciv.ui.screens.worldscreen.chat

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.utils.Align
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.input.onClick
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popups.Popup
import com.unciv.ui.screens.worldscreen.WorldScreen

class OnlineStatusPopup(
    private val worldScreen: WorldScreen
) : Popup(screen = worldScreen, scrollable = Scrollability.None) {

    init {
        // Header: title + close button
        val headerLabel = "Online Players".toLabel(fontSize = 24, alignment = Align.center)
        add(headerLabel).left().pad(10f).expandX()
        add(
            ImageButton(ImageGetter.getImage("OtherIcons/Close").drawable)
                .onClick { close() }
        ).right().size(headerLabel.height * 1.3f).row()
        addSeparator()

        val humans = worldScreen.gameInfo.civilizations.filter { it.isHuman() && it.isAlive() }

        if (humans.isEmpty()) {
            add("No other players online".toLabel()).center().pad(10f).row()
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

            add(dot).padRight(8f)
            add(nameLabel).left().pad(5f).row()
        }
    }
}
