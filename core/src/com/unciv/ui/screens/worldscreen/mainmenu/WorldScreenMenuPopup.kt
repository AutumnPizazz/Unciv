package com.unciv.ui.screens.worldscreen.mainmenu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.unciv.ui.components.input.KeyboardBinding
import com.unciv.ui.components.input.onLongPress
import com.unciv.ui.popups.ConfirmPopup
import com.unciv.ui.popups.Popup
import com.unciv.ui.popups.ToastPopup
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.screens.savescreens.LoadGameScreen
import com.unciv.ui.screens.victoryscreen.VictoryScreen
import com.unciv.ui.screens.worldscreen.WorldScreen
import com.unciv.utils.Concurrency
import com.unciv.utils.debug
import com.unciv.utils.launchOnGLThread

/** The in-game menu called from the "Hamburger" button top-left
 *
 *  Popup automatically opens as soon as it's initialized
 */
class WorldScreenMenuPopup(
    val worldScreen: WorldScreen,
    expertMode: Boolean = false
) : Popup(worldScreen, scrollable = Scrollability.All) {
    private val singleColumn: Boolean
    private fun <T: Actor?> Cell<T>.nextColumn() {
        if (!singleColumn && column == 0) return
        row()
    }

    init {
        worldScreen.autoPlay.stopAutoPlay()
        defaults().fillX()

        val showSave = !worldScreen.gameInfo.gameParameters.isOnlineMultiplayer
        val showMusic = worldScreen.game.musicController.isMusicAvailable()
        val showConsole = showSave && expertMode
        val showRestartVote = canStartRestartVote()
        val buttonCount = 8 + (if (showSave) 1 else 0) + (if (showMusic) 1 else 0) + (if (showConsole) 1 else 0) + (if (showRestartVote) 1 else 0)

        val emptyPrefHeight = this.prefHeight
        val firstCell = addButton("Main menu") {
            worldScreen.game.goToMainMenu()
        }
        singleColumn = worldScreen.isCrampedPortrait() ||
            2 * prefWidth > maxPopupWidth ||  // Very coarse: Assume width of translated "Main menu" is representative
            buttonCount * (prefHeight - emptyPrefHeight) + emptyPrefHeight < maxPopupHeight
        firstCell.nextColumn()

        addButton("Civilopedia", KeyboardBinding.Civilopedia) {
            close()
            worldScreen.openCivilopedia()
        }.nextColumn()
        if (showSave)
            addButton("Save game", KeyboardBinding.SaveGame) {
                close()
                worldScreen.openSaveGameScreen()
            }.nextColumn()
        addButton("Load game", KeyboardBinding.LoadGame) {
            close()
            worldScreen.game.pushScreen(LoadGameScreen())
        }.nextColumn()
        addButton("Start new game", KeyboardBinding.NewGame) {
            close()
            worldScreen.openNewGameScreen()
        }.nextColumn()
        if (showRestartVote)
            addButton("Start restart vote") {
                close()
                val askPopup = ConfirmPopup(
                    worldScreen,
                    "Are you sure you want to start a restart vote? All players will be asked to agree to restart the game with the same setup.",
                    "Yes",
                ) {
                    startRestartVote()
                }
                askPopup.open()
            }.nextColumn()
        addButton("Victory status", KeyboardBinding.VictoryScreen) {
            close()
            worldScreen.game.pushScreen(VictoryScreen(worldScreen))
        }.nextColumn()
        val optionsCell = addButton("Options", KeyboardBinding.Options) {
            close()
            worldScreen.openOptionsPopup()
        }
        optionsCell.actor.onLongPress {
            close()
            worldScreen.openOptionsPopup(withDebug = true)
        }
        optionsCell.nextColumn()
        if (showMusic)
            addButton("Music", KeyboardBinding.MusicPlayer) {
                close()
                WorldScreenMusicPopup(worldScreen).open(force = true)
            }.nextColumn()

        if (showConsole)
            addButton("Developer Console", KeyboardBinding.DeveloperConsole) {
                close()
                worldScreen.openDeveloperConsole()
            }.nextColumn()

        addButton("Exit") {
            close()
            Gdx.app.exit()
        }.apply { actor.style = BaseScreen.skin.get("negative", TextButtonStyle::class.java) }
            .nextColumn()

        addCloseButton().run { colspan(if (singleColumn || column == 1) 1 else 2) }
        pack()

        open(force = true)
    }

    /** Whether a restart vote can be started right now: online game, votes enabled, exactly the
     *  configured turn, no vote file known yet, and we are a player (not a spectator). */
    private fun canStartRestartVote(): Boolean {
        val gameInfo = worldScreen.gameInfo
        if (!gameInfo.gameParameters.isOnlineMultiplayer) return false
        if (worldScreen.viewingCiv.isSpectator()) return false
        val restartVoteTurn = gameInfo.gameParameters.restartVoteTurn
        if (restartVoteTurn <= 0 || gameInfo.turns != restartVoteTurn) return false
        return worldScreen.game.onlineMultiplayer.getCachedRestartVote(gameInfo.gameId) == null
    }

    private fun startRestartVote() {
        val gameInfo = worldScreen.gameInfo
        val gameId = gameInfo.gameId
        val turn = gameInfo.turns
        val timeoutMinutes = gameInfo.gameParameters.restartVoteTimeoutMinutes
        Concurrency.run("StartRestartVote") {
            val vote = try {
                worldScreen.game.onlineMultiplayer.startRestartVote(gameId, turn, timeoutMinutes)
            } catch (ex: Exception) {
                debug("Could not start restart vote: %s", ex.message)
                null
            }
            launchOnGLThread {
                if (vote != null) {
                    worldScreen.openRestartVotePopup()
                } else {
                    ToastPopup("Could not start the restart vote!", worldScreen, 3000)
                }
            }
        }
    }
}
