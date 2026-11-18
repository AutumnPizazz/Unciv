package com.unciv.ui.popups.options

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.files.IMediaFinder.LabeledSounds
import com.unciv.logic.multiplayer.storage.AuthStatus
import com.unciv.logic.multiplayer.storage.FileStorageRateLimitReached
import com.unciv.logic.multiplayer.storage.MultiplayerAuthException
import com.unciv.models.translations.tr
import com.unciv.ui.audio.SoundPlayer
import com.unciv.ui.components.extensions.addSeparator
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.extensions.toTextButton
import com.unciv.ui.components.input.onClick
import com.unciv.ui.components.widgets.UncivTextField
import com.unciv.ui.popups.options.MultiplayerSelectBoxHelpers.RefreshSelectOptions
import com.unciv.ui.popups.AuthPopup
import com.unciv.ui.popups.Popup
import com.unciv.utils.Concurrency
import com.unciv.utils.launchOnGLThread
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

internal class MultiplayerTab(
    optionsPopup: OptionsPopup
) : OptionsPopupTab(optionsPopup), MultiplayerSelectBoxHelpers {
    private val mpSettings by settings::multiplayer
    private val mpServer by game.onlineMultiplayer::multiplayerServer

    override fun lateInitialize() {
        addCheckbox(
            "Enable multiplayer status button in singleplayer games",
            mpSettings::statusButtonInSinglePlayer, updateWorld = true
        )

        addSeparator()

        val curRefreshSelect = RefreshSelectOptions(
            mpSettings::currentGameRefreshDelay,
            createRefreshOptions(ChronoUnit.SECONDS, 3, 5) +
            createRefreshOptions(ChronoUnit.SECONDS, 10, 20, 30, 60)
        )
        addSelectBox("Update status of currently played game every:", curRefreshSelect::value, curRefreshSelect.items)

        val allRefreshSelect = RefreshSelectOptions(
            mpSettings::allGameRefreshDelay,
            createRefreshOptions(ChronoUnit.SECONDS, 15, 30) +
            createRefreshOptions(ChronoUnit.MINUTES, 1, 2, 5, 15)
        )
        addSelectBox("In-game, update status of all games every:", allRefreshSelect::value, allRefreshSelect.items)

        addSeparator()

        addTurnCheckerOptions()

        val labeledSounds = LabeledSounds() // This is a cache but only used until the SelectBoxes are filled
        fun soundsProvider() = synchronized(labeledSounds) {
            labeledSounds.getLabeledSounds().asFlow().map { MultiplayerSelectBoxHelpers.UncivSoundLabeled(it) }
        }
        val currentSoundProxy = MultiplayerSelectBoxHelpers.UncivSoundProxy(mpSettings::currentGameTurnNotificationSound)
        val otherSoundProxy = MultiplayerSelectBoxHelpers.UncivSoundProxy(mpSettings::otherGameTurnNotificationSound)
        addAsyncSelectBox("Sound notification for when it's your turn in your currently open game:", currentSoundProxy::value, ::soundsProvider) {
            SoundPlayer.play(it.value)
        }
        addAsyncSelectBox("Sound notification for when it's your turn in any other game:", otherSoundProxy::value, ::soundsProvider) {
            SoundPlayer.play(it.value)
        }

        addSeparator()

        addMultiplayerServerOptions()

        super.lateInitialize()
    }

    private fun addMultiplayerServerOptions() {
        val connectionToServerButton = "Check connection".toTextButton()
        val serverIpTable = Table()
        serverIpTable.add("Server address".toLabel()).colspan(2).row()
        serverIpTable.add(mpServer.getServerUrl().toLabel()).colspan(2).padBottom(8f).row()

        serverIpTable.add(connectionToServerButton.onClick {
            val popup = Popup(optionsPopup.stageToShowOn).apply {
                addGoodSizedLabel("Awaiting response...").row()
                open(true)
            }

            successfullyConnectedToServer { connectionSuccess, authSuccess ->
                if (authSuccess == false) {
                    popup.close()
                    AuthPopup(optionsPopup.stageToShowOn) { success ->
                        popup.apply {
                            reuseWith(if (success) "Success!" else "Failed!", true)
                            open(true)
                        }
                    }.open(true)
                } else if (connectionSuccess) {
                    if (authSuccess == true) popup.reuseWith("Success!", true)
                    else popup.reuseWith("Auth rejected for unknown reasons, please try again.", true)
                } else {
                    popup.reuseWith("Failed!", true)
                }
            }
        }).colspan(2).row()

        if (mpServer.getFeatureSet().authVersion > 0) {
            val passwordTextField = UncivTextField(
                "Password", mpSettings.getCurrentServerPassword().orEmpty()
            )
            passwordTextField.isPasswordMode = true
            val setPasswordButton = "Set password".toTextButton()

            serverIpTable.add("Set password".toLabel()).padTop(16f).colspan(2).row()
            serverIpTable.add(passwordTextField).colspan(2).growX().padBottom(8f).row()

            // initially assume no password
            val authStatusLabel = "Set a password to secure your userId".toLabel()

            val password = mpSettings.getCurrentServerPassword()
            if (password != null) {
                authStatusLabel.setText("Validating your authentication status...")
                Concurrency.run {
                    val userId = mpSettings.getUserId()
                    val authStatus = mpServer.fileStorage()
                        .checkAuthStatus(userId, password)

                    val newAuthStatusText = when (authStatus) {
                        AuthStatus.UNAUTHORIZED -> "Your current password was rejected from the server"
                        AuthStatus.UNREGISTERED -> "You userId is unregistered! Set password to secure your userId"
                        AuthStatus.VERIFIED -> "Your current password has been succesfully verified"
                        AuthStatus.UNKNOWN -> "Your authentication status could not be determined"
                    }

                    Concurrency.runOnGLThread {
                        authStatusLabel.setText(newAuthStatusText.tr())
                    }
                }
            }

            val passwordStatusTable = Table().apply {
                add(authStatusLabel)
                add(setPasswordButton.onClick {
                    setPassword(passwordTextField.text, optionsPopup)
                }).padLeft(16f)
            }

            serverIpTable.add(passwordStatusTable).colspan(2).row()
        }

        add(serverIpTable).colspan(2).fillX().row()
    }

    private fun addTurnCheckerOptions() {
        // at the moment the notification service only exists on Android
        if (Gdx.app.type != Application.ApplicationType.Android) return

        addCheckbox("Enable out-of-game turn notifications", mpSettings::turnCheckerEnabled) {
            reopenOptions(force = true)
        }

        if (!mpSettings.turnCheckerEnabled) return

        val turnCheckerSelect = RefreshSelectOptions(
            mpSettings::turnCheckerDelay,
            createRefreshOptions(ChronoUnit.SECONDS, 30) +
            createRefreshOptions(ChronoUnit.MINUTES, 1, 2, 5, 15)
        )
        addSelectBox("Out-of-game, update status of all games every:", turnCheckerSelect::value, turnCheckerSelect.items)

        addCheckbox("Show persistent notification for turn notifier service", mpSettings::turnCheckerPersistentNotificationEnabled)

        addSeparator()
    }

    private fun successfullyConnectedToServer(action: (Boolean, Boolean?) -> Unit) {
        Concurrency.run("TestIsAlive") {
            try {
                val connectionSuccess = mpServer.checkServerStatus()
                var authSuccess: Boolean? = null
                if (connectionSuccess) {
                    try {
                        authSuccess = mpServer.authenticate(null)
                    } catch (_: MultiplayerAuthException) {
                        authSuccess = false
                    } catch (_: Throwable) {
                        // We ignore the exception here, because we handle the failed auth onGLThread
                    }
                }
                launchOnGLThread {
                    action(connectionSuccess, authSuccess)
                }
            } catch (_: Exception) {
                launchOnGLThread {
                    action(false, false)
                }
            }
        }
    }

    private fun setPassword(password: String, optionsPopup: OptionsPopup) {
        if (password.isBlank())
            return

        val popup = Popup(optionsPopup.stageToShowOn).apply {
            addGoodSizedLabel("Awaiting response...").row()
            open(true)
        }

        if (password.length < 6) {
            popup.reuseWith("Password must be at least 6 characters long", true)
            return
        }

        if (mpServer.getFeatureSet().authVersion == 0) {
            popup.reuseWith("This server does not support authentication", true)
            return
        }

        successfullySetPassword(password) { success, ex ->
            if (success) {
                popup.reuseWith(
                    "Password set successfully for server [${mpServer.getServerUrl()}]",
                    true
                )
            } else {
                if (ex is MultiplayerAuthException) {
                    AuthPopup(optionsPopup.stageToShowOn) { authSuccess ->
                        // If auth was successful, try to set password again
                        if (authSuccess) {
                            popup.close()
                            setPassword(password, optionsPopup)
                        } else {
                            popup.reuseWith("Failed to set password!", true)
                        }
                    }.open(true)
                    return@successfullySetPassword
                }

                val message = when (ex) {
                    is FileStorageRateLimitReached -> "Server limit reached! Please wait for [${ex.limitRemainingSeconds}] seconds"
                    else -> "Failed to set password!"
                }

                popup.reuseWith(message, true)
            }
        }
    }

    private fun successfullySetPassword(password: String, action: (Boolean, Exception?) -> Unit) {
        Concurrency.run("SetPassword") {
            try {
                val setSuccess = mpServer.setPassword(password)
                launchOnGLThread {
                    action(setSuccess, null)
                }
            } catch (ex: Exception) {
                launchOnGLThread {
                    action(false, ex)
                }
            }
        }
    }
}
