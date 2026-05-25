package com.unciv.ui.screens.worldscreen.chat

import com.badlogic.gdx.graphics.Color
import com.unciv.ui.components.extensions.toTextButton
import com.unciv.ui.components.input.onClick
import com.unciv.ui.images.IconTextButton
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.screens.worldscreen.WorldScreen

class OnlineStatusButton(val worldScreen: WorldScreen) : IconTextButton(
    "Online", ImageGetter.getImage("OtherIcons/Chat"), 23
) {
    init {
        width = 95f
        iconCell.pad(3f).center()
        onClick {
            OnlineStatusPopup(worldScreen).open()
        }
        isVisible = worldScreen.gameInfo.isPollingMode()
    }

    fun updatePosition() {
        if (!isVisible) return
        setPosition(
            worldScreen.chatButton.x + worldScreen.chatButton.width + 5f,
            worldScreen.chatButton.y
        )
    }
}
