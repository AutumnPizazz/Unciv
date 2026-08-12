package com.unciv.app

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Debug
import android.provider.Settings
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.FileProvider
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidGraphics
import com.badlogic.gdx.math.Rectangle
import com.unciv.UncivGame
import com.unciv.logic.IdChecker
import com.unciv.logic.event.EventBus
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.screens.basescreen.UncivStage
import com.unciv.utils.Concurrency
import com.unciv.utils.isUUID
import java.io.File
import java.util.Locale

class AndroidGame(private val activity: Activity) : UncivGame() {

    private var lastOrientation = activity.resources.configuration.orientation

    fun addScreenObscuredListener() {
        val contentView = (Gdx.graphics as AndroidGraphics).view
        contentView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

            /** [onGlobalLayout] gets triggered not only when the [View.getWindowVisibleDisplayFrame]
             * changes, but also on other things. So we need to check if that was actually
             * the thing that changed. */
            private var lastFrame: Rect? = null
            private var lastVisibleStage: Rectangle? = null

            override fun onGlobalLayout() {

                if (!isInitialized || screen == null)
                    return

                val currentFrame = Rect()
                contentView.getWindowVisibleDisplayFrame(currentFrame)

                val stage = (screen as BaseScreen).stage
                val horizontalRatio = stage.width / contentView.width
                val verticalRatio = stage.height / contentView.height

                // Android coordinate system has the origin in the top left,
                // while GDX uses bottom left.

                val visibleStage = Rectangle(
                    currentFrame.left * horizontalRatio,
                    (contentView.height - currentFrame.bottom)  * verticalRatio,
                    currentFrame.width() * horizontalRatio,
                    currentFrame.height() * verticalRatio
                )

                if (lastFrame == currentFrame && lastVisibleStage == visibleStage)
                    return
                lastFrame = currentFrame
                lastVisibleStage = visibleStage

                val currentOrientation = activity.resources.configuration.orientation
                if (lastOrientation != currentOrientation) {
                    lastOrientation = currentOrientation
                    return
                }

                Concurrency.runOnGLThread {
                    EventBus.send(UncivStage.VisibleAreaChanged(visibleStage))
                }
            }
        })
    }

    /** This is needed in onCreate _and_ onNewIntent to open links and notifications
     *  correctly even if the app was not running */
    fun setDeepLinkedGame(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) {
            deepLinkedMultiplayerGame = null
        }
        val uri: Uri? = intent.data
        val idParam = uri?.getQueryParameter("id") //legacy game url
        deepLinkedMultiplayerGame = 
            if (idParam != null && idParam.isUUID()) idParam
            else if (IdChecker.isGameDeepLink(uri.toString())) IdChecker.checkAndReturnUuiId(uri.toString())
            else null
    }

    fun isInitializedProxy() = super.isInitialized

    override fun getGcCount(): Int = 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Debug.getRuntimeStat("art.gc.gc-count").toInt() else 0

    override fun getDefaultLocale(): Locale =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) super.getDefaultLocale()
        else activity.resources.configuration.locales.get(0)

    /** In-game downloaded installer packages (the APK) go to the app-specific external folder -
     *  no storage permission needed, and shareable to the system installer via [FileProvider] */
    override fun getInstallerDownloadFolder(): String? =
        activity.getExternalFilesDir("installers")?.absolutePath

    /** Open the system installer for a downloaded APK - the game runs in the background meanwhile */
    override fun installDownloadedApk(apkFilePath: String) {
        // API 26+ requires a per-app permission for installing unknown apps - guide the player there first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivity(settingsIntent)
            return
        }
        try {
            val apkFile = File(apkFilePath)
            val apkUri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(installIntent)
        } catch (_: Exception) {
            // File missing or provider misconfigured - nothing sensible to show here
        }
    }
}
