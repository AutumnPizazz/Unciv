package com.unciv.build

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

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
        val (version, code) = parseBuildConfig(buildConfigFile.readText())
        val changed = syncUncivGameVersion(project.rootProject.projectDir, version, code)
        if (changed) {
            logger.lifecycle("Synced UncivGame.kt game version to {} / build {} (was out of sync)", version, code)
        } else {
            logger.info("Game version already in sync: {} / build {}", version, code)
        }
    }
}
