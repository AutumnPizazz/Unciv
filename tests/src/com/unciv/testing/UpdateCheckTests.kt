package com.unciv.testing

import com.unciv.logic.UpdateChecker
import com.unciv.logic.compareVersionStrings
import com.unciv.logic.github.GithubAPI
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(BaseTestRunner::class)
class UpdateCheckTests {
    private fun releaseWithAssets(vararg names: String) = GithubAPI.LatestRelease().apply {
        assets.addAll(names.map { GithubAPI.ReleaseAsset().apply { name = it } })
    }
    @Test
    fun versionComparison() {
        // CN fork scheme: four segments (upstream major.minor.patch.cn-subversion)
        Assert.assertEquals(0, compareVersionStrings("4.21.6.5", "4.21.6.5"))
        Assert.assertTrue(compareVersionStrings("4.21.6.6", "4.21.6.5") > 0)
        Assert.assertTrue(compareVersionStrings("4.21.6.5", "4.21.6.6") < 0)
        Assert.assertTrue(compareVersionStrings("4.21.7.1", "4.21.6.9") > 0)

        // Upstream release tags: patch suffix and optional 'v' prefix
        Assert.assertTrue(compareVersionStrings("4.21.7-patch2", "4.21.7") > 0)
        Assert.assertTrue(compareVersionStrings("4.21.7-patch2", "4.21.7-patch10") < 0)
        Assert.assertTrue(compareVersionStrings("v4.21.6", "4.21.5.9") > 0)

        // Missing trailing segments count as zero
        Assert.assertTrue(compareVersionStrings("4.21.6.5", "4.21.6") > 0)
        Assert.assertTrue(compareVersionStrings("4.21.6", "4.21.6.5") < 0)
    }

    @Test
    fun isNewerThanCurrent() {
        Assert.assertTrue(UpdateChecker.isNewerThan("4.21.6.6", "4.21.6.5"))
        Assert.assertFalse(UpdateChecker.isNewerThan("4.21.6.5", "4.21.6.5"))
        Assert.assertFalse(UpdateChecker.isNewerThan("4.21.6.4", "4.21.6.5"))
    }

    /** Asset names mirror the real release layout of the CN fork repo (AutumnPizazz/Unciv) */
    private val fullRelease = releaseWithAssets(
        "linuxFilesForJar-4.21.7.1.zip",
        "unciv-lua-api-4.21.7.1.vsix",
        "UncivCN-4.21.7.1.Apk",  // note the capital extension
        "UncivCN-4.21.7.1.jar",
        "UncivCN-4.21.7.1.msi",
        "UncivCN-Linux64-4.21.7.1.zip",
        "UncivCN-Windows64-4.21.7.1.zip",
        "UncivServer-4.21.7.1.jar",
    )

    @Test
    fun pickDownloadAsset_platformSpecific() {
        Assert.assertEquals("UncivCN-4.21.7.1.Apk", fullRelease.pickDownloadAsset("android")?.name)
        Assert.assertEquals("UncivCN-4.21.7.1.msi", fullRelease.pickDownloadAsset("windows")?.name)
        Assert.assertEquals("UncivCN-Linux64-4.21.7.1.zip", fullRelease.pickDownloadAsset("linux")?.name)
    }

    @Test
    fun listDownloadAssets_preferenceOrder() {
        // Windows gets the full choice: MSI installer first, then portable zip, then universal jar
        Assert.assertEquals(
            listOf("UncivCN-4.21.7.1.msi", "UncivCN-Windows64-4.21.7.1.zip", "UncivCN-4.21.7.1.jar"),
            fullRelease.listDownloadAssets("windows").map { it.name }
        )
        Assert.assertEquals(
            listOf("UncivCN-Linux64-4.21.7.1.zip", "UncivCN-4.21.7.1.jar"),
            fullRelease.listDownloadAssets("linux").map { it.name }
        )
        Assert.assertEquals(
            listOf("UncivCN-4.21.7.1.Apk"),
            fullRelease.listDownloadAssets("android").map { it.name }
        )
        Assert.assertEquals(
            listOf("UncivCN-4.21.7.1.jar"),
            fullRelease.listDownloadAssets("mac").map { it.name }
        )
    }

    @Test
    fun listDownloadAssets_emptyForUnknownOrServerOnly() {
        Assert.assertTrue(fullRelease.listDownloadAssets("web").isEmpty())
        Assert.assertTrue(fullRelease.listDownloadAssets("").isEmpty())
        Assert.assertTrue(
            releaseWithAssets("UncivServer-4.21.7.1.jar", "linuxFilesForJar-4.21.7.1.zip")
                .listDownloadAssets("windows").isEmpty()
        )
    }

    @Test
    fun pickDownloadAsset_neverPicksServerOrHelperPackages() {
        // Only server jar and helper files available - nothing may be offered
        val onlyServer = releaseWithAssets("UncivServer-4.21.7.1.jar", "linuxFilesForJar-4.21.7.1.zip")
        Assert.assertNull(onlyServer.pickDownloadAsset("windows"))
        Assert.assertNull(onlyServer.pickDownloadAsset("linux"))
        Assert.assertNull(onlyServer.pickDownloadAsset("mac"))
    }

    @Test
    fun pickDownloadAsset_fallsBackToUniversalJar() {
        // Windows without msi/zip falls back to the runnable jar; mac has no native package
        val noWindowsPackage = releaseWithAssets("UncivCN-4.21.7.1.jar", "UncivCN-Linux64-4.21.7.1.zip")
        Assert.assertEquals("UncivCN-4.21.7.1.jar", noWindowsPackage.pickDownloadAsset("windows")?.name)
        Assert.assertEquals("UncivCN-4.21.7.1.jar", fullRelease.pickDownloadAsset("mac")?.name)
        Assert.assertEquals("UncivCN-Linux64-4.21.7.1.zip", noWindowsPackage.pickDownloadAsset("linux")?.name)
    }

    @Test
    fun pickDownloadAsset_androidAndUnknownPlatforms() {
        // A jar is useless on Android, unknown platforms get nothing
        val jarOnly = releaseWithAssets("UncivCN-4.21.7.1.jar")
        Assert.assertNull(jarOnly.pickDownloadAsset("android"))
        Assert.assertNull(fullRelease.pickDownloadAsset("web"))
        Assert.assertNull(fullRelease.pickDownloadAsset(""))
    }

    @Test
    fun isReleaseInstallerUrl_matchesReleaseDownloadsOnly() {
        // GitHub release 安装包下载 URL 匹配；GitHub API、模组 zip、发布页不匹配
        Assert.assertTrue(GithubAPI.isReleaseInstallerUrl(
            "https://github.com/AutumnPizazz/Unciv/releases/download/4.21.10.1/UncivCN-4.21.10.1.Apk"
        ))
        Assert.assertFalse(GithubAPI.isReleaseInstallerUrl(
            "https://api.github.com/repos/AutumnPizazz/Unciv/releases/latest"
        ))
        Assert.assertFalse(GithubAPI.isReleaseInstallerUrl(
            "https://github.com/SomeAuthor/SomeMod/archive/refs/heads/master.zip"
        ))
    }

    @Test
    fun cnServerUrlFor_mapsReleaseDownloads() {
        // GitHub release 安装包 URL → CN 官方下载服务器 /dl/<tag>/<file>
        Assert.assertEquals(
            "http://sp.unciv.cn:30123/dl/4.21.10.1/UncivCN-4.21.10.1.Apk",
            GithubAPI.cnServerUrlFor(
                "https://github.com/AutumnPizazz/Unciv/releases/download/4.21.10.1/UncivCN-4.21.10.1.Apk"
            )
        )
        // 非安装包 URL 原样返回
        val apiUrl = "https://api.github.com/repos/AutumnPizazz/Unciv/releases/latest"
        Assert.assertEquals(apiUrl, GithubAPI.cnServerUrlFor(apiUrl))
    }
}
