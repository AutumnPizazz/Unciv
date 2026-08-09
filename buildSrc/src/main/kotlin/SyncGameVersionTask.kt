package com.unciv.build

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * 把 `buildSrc/src/main/kotlin/BuildConfig.kt` 的 `appVersion` / `appCodeNumber`
 * 同步到 `core/src/com/unciv/UncivGame.kt` 的
 * "AUTOMATICALLY GENERATED VERSION DATA" 区域（游戏内显示的版本号来源）。
 *
 * 上游 Unciv 由 uncivbot 脚本在发版时改写该区域；本 fork 不发版脚本，
 * 改为构建时自动同步，避免只改 BuildConfig.kt 导致游戏内版本号滞后。
 * 仅在值不一致时改写文件，一致时保持 git 工作区干净。
 */
open class SyncGameVersionTask : DefaultTask() {

    @TaskAction
    fun sync() {
        val buildConfigFile = project.rootProject.file("buildSrc/src/main/kotlin/BuildConfig.kt")
        val buildConfigText = buildConfigFile.readText()
        val version = Regex("""appVersion = "([^"]*)"""").find(buildConfigText)?.groupValues?.get(1)
            ?: error("appVersion not found in ${buildConfigFile.path}")
        val code = Regex("""appCodeNumber = (\d+)""").find(buildConfigText)?.groupValues?.get(1)
            ?: error("appCodeNumber not found in ${buildConfigFile.path}")

        val gameInfoFile = project.rootProject.file("core/src/com/unciv/UncivGame.kt")
        val gameInfoText = gameInfoFile.readText()
        val regionRegex = Regex(
            "(//region AUTOMATICALLY GENERATED VERSION DATA - DO NOT CHANGE THIS REGION, INCLUDING THIS COMMENT\\n)" +
                "(\\s*)val VERSION = Version\\(\"[^\"]*\", \\d*\\)\\n" +
                "(\\s*//endregion)"
        )
        val replacement = regionRegex.replace(gameInfoText) { match ->
            val indent = match.groupValues[2].takeWhile { it.isWhitespace() }
            match.groupValues[1] + indent + "val VERSION = Version(\"$version\", $code)\n" + match.groupValues[3]
        }
        if (replacement == gameInfoText) {
            logger.info("Game version already in sync: {} / build {}", version, code)
            return
        }
        gameInfoFile.writeText(replacement)
        logger.lifecycle("Synced UncivGame.kt game version to {} / build {} (was out of sync)", version, code)
    }
}
