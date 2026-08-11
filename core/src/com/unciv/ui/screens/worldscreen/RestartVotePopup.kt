package com.unciv.ui.screens.worldscreen

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.logic.multiplayer.RestartVote
import com.unciv.logic.multiplayer.RestartVoteStatus
import com.unciv.ui.components.extensions.toTextButton
import com.unciv.ui.components.input.onClick
import com.unciv.ui.popups.Popup
import com.unciv.ui.popups.ToastPopup
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.screens.worldscreen.WorldScreen
import com.unciv.utils.Concurrency
import com.unciv.utils.launchOnGLThread
import com.unciv.models.translations.tr
import kotlin.math.max

/**
 * Popup showing the current restart vote state of the game and letting the player vote yes/no.
 * Contents are rebuilt on every [refresh] (driven by [com.unciv.logic.multiplayer.RestartVoteUpdated]
 * events and by the next-turn guard), keeping labels and buttons up to date.
 */
class RestartVotePopup(
    val worldScreen: WorldScreen
) : Popup(worldScreen) {
    private val gameInfo = worldScreen.gameInfo
    private val gameId = gameInfo.gameId
    private val myPlayerId = UncivGame.Current.settings.multiplayer.getUserId()

    private val statusLabel = Label("", BaseScreen.skin)
    private val votesLabel = Label("", BaseScreen.skin)
    private val timeLeftLabel = Label("", BaseScreen.skin)
    private val myVoteLabel = Label("", BaseScreen.skin)
    private val yesButton = "Yes".toTextButton()
    private val noButton = "No".toTextButton()

    init {
        addGoodSizedLabel("Restart vote").row()
        add(statusLabel).row()
        add(votesLabel).row()
        add(timeLeftLabel).row()
        add(myVoteLabel).row()
        val buttonTable = Table()
        buttonTable.add(yesButton).pad(5f)
        buttonTable.add(noButton).pad(5f)
        add(buttonTable).row()
        addCloseButton()

        yesButton.onClick { castVote(true) }
        noButton.onClick { castVote(false) }
        pack()
        refresh()
    }

    fun refresh() {
        val vote = worldScreen.game.onlineMultiplayer.getCachedRestartVote(gameId)
        if (vote == null) {
            close()
            return
        }
        updateLabels(vote)
        pack()
    }

    private fun updateLabels(vote: RestartVote) {
        val yesCount = vote.votes.count { it.value }
        val noCount = vote.votes.size - yesCount
        votesLabel.setText("Yes: [$yesCount]   No: [$noCount]".tr())
        timeLeftLabel.setText("Time left: [${formatTimeLeft(vote)}]".tr())

        if (vote.status == RestartVoteStatus.OPEN) {
            statusLabel.setText("[${getCivName(vote.initiatorPlayerId)}] wants to restart the game".tr())
            if (vote.hasVoted(myPlayerId)) {
                val myVote = vote.votes[myPlayerId]!!
                myVoteLabel.setText("Your vote: [${if (myVote) "Yes" else "No"}]".tr())
                yesButton.isVisible = false
                noButton.isVisible = false
            } else {
                myVoteLabel.setText("")
                yesButton.isVisible = true
                noButton.isVisible = true
            }
        } else {
            statusLabel.setText(
                if (vote.result == true) "Restart approved - starting a new game...".tr()
                else "Restart rejected".tr()
            )
            myVoteLabel.setText("")
            yesButton.isVisible = false
            noButton.isVisible = false
        }
    }

    private fun formatTimeLeft(vote: RestartVote): String {
        val remainingSeconds = max(
            0L,
            (vote.startedAtMillis + vote.timeoutMinutes * 60_000L - System.currentTimeMillis()) / 1000L
        )
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60
        if (hours >= 24) return "${hours / 24}d ${hours % 24}h"
        if (hours > 0) return "${hours}h ${minutes}m"
        return "${minutes}m"
    }

    private fun getCivName(playerId: String): String {
        val civ = gameInfo.civilizations.firstOrNull { it.playerId == playerId }
        return civ?.civName ?: playerId
    }

    private fun castVote(voteValue: Boolean) {
        yesButton.isDisabled = true
        noButton.isDisabled = true
        Concurrency.run("CastRestartVote") {
            val result = try {
                worldScreen.game.onlineMultiplayer.castRestartVote(gameId, voteValue)
            } catch (ex: Exception) {
                null
            }
            launchOnGLThread {
                if (result == null) {
                    // Vote failed (network issue etc.) - re-enable the buttons so the player can retry
                    yesButton.isDisabled = false
                    noButton.isDisabled = false
                    ToastPopup("Could not submit your vote - please try again!", worldScreen, 3000)
                }
                refresh()
            }
        }
    }
}
