package com.unciv.logic

import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.json.json
import com.unciv.logic.github.GithubAPI
import com.unciv.models.metadata.GameSettings.PlayerRegion
import io.ktor.client.statement.bodyAsText

/** Result of an update check against the game repository's latest release */
sealed class UpdateCheckResult {
    /** A newer release exists - [latestRelease] carries the new version info */
    data class UpdateAvailable(val latestRelease: GithubAPI.LatestRelease) : UpdateCheckResult()

    /** The running version is already the latest release (or newer) */
    object UpToDate : UpdateCheckResult()

    /** The check failed, usually a network problem reaching the chosen update source */
    object CheckFailed : UpdateCheckResult()
}

object UpdateChecker {
    /** Whether release [tag] is newer than the given [currentVersion] - pure, for tests */
    fun isNewerThan(tag: String, currentVersion: String) =
        compareVersionStrings(tag, currentVersion) > 0

    /** Whether release [tag] is newer than the currently running game version */
    fun isNewerThanCurrent(tag: String) = isNewerThan(tag, UncivGame.VERSION.text)

    /** 中国大陆玩家通过社区服务查询新版本，未设置地区时也使用该服务。 */
    private fun shouldUseCnServer() =
        PlayerRegion.fromStoredName(UncivGame.Current.settings.playerRegion) != PlayerRegion.Overseas

    /**
     * Query the game's latest release. Players in mainland China use the community manifest at
     * `/api/downloads/latest.json`. Other players query GitHub through their selected download source.
     */
    suspend fun checkForUpdates(): UpdateCheckResult {
        val release = try {
            if (shouldUseCnServer()) {
                val url = "${Constants.uncivDownloadServer}/api/downloads/latest.json"
                val resp = UncivKtor.getOrNull(url) ?: return UpdateCheckResult.CheckFailed
                json().fromJson(GithubAPI.LatestRelease::class.java, resp.bodyAsText())
            } else {
                val (owner, repo) = Constants.updateCheckRepo.split("/")
                GithubAPI.fetchLatestRelease(owner, repo)
            }
        } catch (_: Exception) {
            return UpdateCheckResult.CheckFailed
        }
        if (release == null || release.tag_name.isEmpty()) return UpdateCheckResult.CheckFailed
        return if (isNewerThanCurrent(release.tag_name))
            UpdateCheckResult.UpdateAvailable(release)
        else UpdateCheckResult.UpToDate
    }
}
