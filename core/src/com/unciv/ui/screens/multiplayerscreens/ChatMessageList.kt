package com.unciv.ui.screens.multiplayerscreens

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.logic.event.EventBus
import com.unciv.logic.multiplayer.OnlineMultiplayer
import com.unciv.logic.multiplayer.apiv2.ChatMessage
import com.unciv.logic.multiplayer.apiv2.IncomingChatMessage
import com.unciv.models.translations.tr
import com.unciv.ui.components.AutoScrollPane
import com.unciv.ui.components.extensions.formatShort
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.popups.InfoPopup
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.utils.concurrency.Concurrency
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Simple list for messages from a multiplayer chat
 *
 * You most likely want to center this actor in a parent and wrap it in a
 * [AutoScrollPane] and set [AutoScrollPane.setScrollingDisabled] for the X direction.
 * You **must** set [parent] to this actor's parent, otherwise width calculations won't
 * work, which is required to avoid scrolling in X direction due to overfull lines of text.
 *
 * @sample
 *      val chatMessages = ChatMessageList(UUID.randomUUID())
 *      val chatScroll = AutoScrollPane(chatMessages, skin)
 *      chatScroll.setScrollingDisabled(true, false)
 *
 * Another good way is to use the [ChatTable] directly.
 */
class ChatMessageList(private val chatRoomUUID: UUID, private val mp: OnlineMultiplayer): Table() {
    private val events = EventBus.EventReceiver()
    private var messageCache: MutableList<ChatMessage> = mutableListOf()

    init {
        defaults().expandX().space(5f)
        recreate(messageCache)
        triggerRefresh()

        events.receive(IncomingChatMessage::class, { it.chatUUID == chatRoomUUID }) {
            messageCache.add(it.message)
            Concurrency.runOnGLThread {
                recreate(messageCache)
            }
        }
    }

    /**
     * Send a [message] to the chat room by dispatching a coroutine which handles it
     *
     * Use [suppress] to avoid showing an [InfoPopup] for any failures.
     */
    fun sendMessage(message: String, suppress: Boolean = false) {
        Concurrency.run {
            if (suppress) {
                messageCache.add(mp.api.chat.send(message, chatRoomUUID)!!)
            } else {
                InfoPopup.wrap(stage) {
                    messageCache.add(mp.api.chat.send(message, chatRoomUUID)!!)
                }
            }
        }
    }

    /**
     * Trigger a background refresh of the chat messages
     *
     * This will update the messages of the chat room by querying the server
     * and then recreate the message list in a separate coroutine.
     * Use [suppress] to avoid showing an [InfoPopup] for any failures.
     */
    fun triggerRefresh(suppress: Boolean = false) {
        Concurrency.runOnGLThread {
            val s = stage
            Concurrency.run {
                if (suppress) {
                    val chatInfo = mp.api.chat.get(chatRoomUUID, true)
                    if (chatInfo != null) {
                        Concurrency.runOnGLThread {
                            messageCache = chatInfo.messages.toMutableList()
                            recreate(chatInfo.messages)
                        }
                    }
                } else {
                    InfoPopup.wrap(s) {
                        val chatInfo = mp.api.chat.get(chatRoomUUID, false)
                        if (chatInfo != null) {
                            Concurrency.runOnGLThread {
                                messageCache = chatInfo.messages.toMutableList()
                                recreate(chatInfo.messages)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Recreate the table of messages using the given list of chat messages
     */
    fun recreate(messages: List<ChatMessage>) {
        clearChildren()
        if (messages.isEmpty()) {
            val label = "No messages here yet".toLabel()
            label.setAlignment(Align.center)
            add(label).fillX().fillY().center()
            return
        }

        val now = Instant.now()
        for (message in messages) {
            row()
            val time = "[${Duration.between(message.createdAt, now).formatShort()}] ago".tr()
            val label = Label("${message.sender.displayName} (${message.sender.username}) $time:\n${message.message}", BaseScreen.skin)
            label.setAlignment(Align.left)
            label.wrap = true
            val cell = add(label)
            cell.fillX()
        }
    }

}
