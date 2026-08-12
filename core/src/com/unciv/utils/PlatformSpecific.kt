package com.unciv.utils

import java.util.Locale

interface PlatformSpecific {

    /** Notifies player that his multiplayer turn started */
    fun notifyTurnStarted() {}

    /** Install system audio hooks */
    fun installAudioHooks() {}

    /** If not null, this is the path to the directory in which to store the local files - mods, saves, maps, etc */
    var customDataDirectory: String?

    /** If the OS localizes all error messages, this should provide a lookup */
    fun getSystemErrorMessage(errorCode: Int): String? = null

    fun getGcCount(): Int

    /** Get system locale, on Android 13+ app-specific locale */
    fun getDefaultLocale(): Locale = Locale.getDefault()

    /**
     * If not null, the absolute path of a writable folder for in-game downloaded installer
     * packages (the APK on Android). Platforms returning `null` (desktop) download via the browser.
     */
    fun getInstallerDownloadFolder(): String? = null

    /** Open the system installer for a downloaded APK - no-op on platforms without one */
    fun installDownloadedApk(apkFilePath: String) {}
}
