package com.unciv.logic

import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.logic.github.GithubAPI

/** Result of an update check against the game repository's latest GitHub release */
sealed class UpdateCheckResult {
    /** A newer release exists - [latestRelease] carries the new version info */
    data class UpdateAvailable(val latestRelease: GithubAPI.LatestRelease) : UpdateCheckResult()

    /** The running version is already the latest release (or newer) */
    object UpToDate : UpdateCheckResult()

    /** The check failed, usually a network problem reaching the active download source */
    object CheckFailed : UpdateCheckResult()
}

object UpdateChecker {
    /** Whether release [tag] is newer than the given [currentVersion] - pure, for tests */
    fun isNewerThan(tag: String, currentVersion: String) =
        compareVersionStrings(tag, currentVersion) > 0

    /** Whether release [tag] is newer than the currently running game version */
    fun isNewerThanCurrent(tag: String) = isNewerThan(tag, UncivGame.VERSION.text)

    /** Query the latest release of [Constants.updateCheckRepo].
     *  Goes through the active download source, so mirrors also work for the update check. */
    suspend fun checkForUpdates(): UpdateCheckResult {
        val (owner, repo) = Constants.updateCheckRepo.split("/")
        val release = try {
            GithubAPI.fetchLatestRelease(owner, repo)
        } catch (_: Exception) {
            return UpdateCheckResult.CheckFailed
        }
        if (release == null) return UpdateCheckResult.CheckFailed
        return if (isNewerThanCurrent(release.tag_name))
            UpdateCheckResult.UpdateAvailable(release)
        else UpdateCheckResult.UpToDate
    }
}
