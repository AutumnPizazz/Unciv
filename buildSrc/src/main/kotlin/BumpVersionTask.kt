package com.unciv.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * 一键升版本：`./gradlew bumpVersion -PnewVersion=4.22.4.3 [-PnewCode=1265]`
 * 或用 `./gradlew bumpVersion -Pbump=patch` 在当前版本末尾的 CN 子版本号上 +1。
 *
 * 会改动：
 *  - `buildSrc/src/main/kotlin/BuildConfig.kt`（版本真源）
 *  - `core/src/com/unciv/UncivGame.kt`（游戏内显示的版本号区域）
 *  - `docs/UncivCN/Changelog.md` 与 `docs/zh/UncivCN/Changelog.md`（插入新版本小节）
 *
 * 之后由 `./gradlew bumpVersion` 的 finalizer `:desktop:generateDocs`
 * 重新生成 `docs/Modders/lua-api.lua` / `lua-map-api.lua`（依赖 core 用新版本重编译）。
 *
 * 不自动提交、不打 tag —— 交由人工 review 后按发布流程处理。
 */
open class BumpVersionTask : DefaultTask() {

    /** 显式版本号，如 `4.22.4.3`（来自 -PnewVersion）。 */
    @get:Input @get:Optional
    var newVersion: String? = null

    /** 显式 build 号，如 `1265`（来自 -PnewCode）；缺省为当前 +1。 */
    @get:Input @get:Optional
    var newCode: String? = null

    /** 自增模式（来自 -Pbump）：目前支持 `patch`（CN 子版本号 +1）。 */
    @get:Input @get:Optional
    var bump: String? = null

    @TaskAction
    fun bumpVersion() {
        val rootDir: File = project.rootProject.projectDir
        val buildConfigFile = File(rootDir, "buildSrc/src/main/kotlin/BuildConfig.kt")
        val (currentVersion, currentCode) = parseBuildConfig(buildConfigFile.readText())

        val targetVersion = newVersion?.trim()?.takeIf { it.isNotEmpty() }
            ?: bump?.let { computeBumpedVersion(currentVersion, it) }
            ?: throw GradleException(
                "请指定新版本号：-PnewVersion=X.Y.Z[.W]，或用 -Pbump=patch 在末尾 CN 子版本号上 +1"
            )
        val targetCode = newCode?.trim()?.takeIf { it.isNotEmpty() }
            ?: (currentCode.toInt() + 1).toString()

        if (!Regex("""\d+(\.\d+){1,3}""").matches(targetVersion)) {
            throw GradleException("版本号格式不合法：$targetVersion（期望形如 4.22.4.3）")
        }
        if (!Regex("""\d+""").matches(targetCode)) {
            throw GradleException("build 号格式不合法：$targetCode（期望纯数字）")
        }
        if (targetVersion == currentVersion && targetCode == currentCode) {
            logger.lifecycle("版本未变化（$currentVersion / build $currentCode），无事可做")
            return
        }

        // 1. 版本真源
        var text = buildConfigFile.readText()
        text = text.replaceFirst(Regex("""appVersion = "[^"]*""""), """appVersion = "$targetVersion"""")
        text = text.replaceFirst(Regex("""appCodeNumber = \d+"""), "appCodeNumber = $targetCode")
        buildConfigFile.writeText(text)
        logger.lifecycle("BuildConfig.kt：$currentVersion / $currentCode  ->  $targetVersion / $targetCode")

        // 2. 游戏内显示的版本号
        if (syncUncivGameVersion(rootDir, targetVersion, targetCode)) {
            logger.lifecycle("UncivGame.kt：已同步为 $targetVersion / build $targetCode")
        }

        // 3. changelog 插入新版本小节（中英各一，格式随各自既有风格）
        insertChangelogVersionSection(
            File(rootDir, "docs/UncivCN/Changelog.md"),
            targetVersion,
            "## $targetVersion (build $targetCode)",
            "## Unreleased"
        )
        insertChangelogVersionSection(
            File(rootDir, "docs/zh/UncivCN/Changelog.md"),
            targetVersion,
            "## $targetVersion（build $targetCode）",
            "## 未发布"
        )
        logger.lifecycle("Changelog：已为中英两版插入 $targetVersion（build $targetCode）小节")

        logger.lifecycle("下一步：人工 review 后提交，并打 tag $targetVersion 推送（标签会触发发布工作流）")
    }

    /** 在当前 CN 子版本号上 +1：`4.22.4` -> `4.22.4.1`，`4.22.4.2` -> `4.22.4.3`。 */
    private fun computeBumpedVersion(current: String, mode: String): String {
        if (!mode.equals("patch", ignoreCase = true)) {
            throw GradleException("不支持的 -Pbump=$mode（目前仅支持 patch）")
        }
        val parts = current.split(".").toMutableList()
        if (parts.size < 4) {
            parts.add("1")
        } else {
            parts[3] = (parts[3].toIntOrNull()
                ?: throw GradleException("当前版本末位不是数字：$current")).plus(1).toString()
        }
        return parts.joinToString(".")
    }
}
