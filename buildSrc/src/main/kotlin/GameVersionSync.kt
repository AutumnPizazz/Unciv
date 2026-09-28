package com.unciv.build

import java.io.File

/**
 * 版本号读写的公共逻辑，供 [SyncGameVersionTask] 与 [BumpVersionTask] 复用。
 */

/** 解析 `BuildConfig.kt` 文本中的 `appVersion` / `appCodeNumber`。 */
internal fun parseBuildConfig(text: String): Pair<String, String> {
    val version = Regex("""appVersion = "([^"]*)"""").find(text)?.groupValues?.get(1)
        ?: error("appVersion not found in BuildConfig.kt")
    val code = Regex("""appCodeNumber = (\d+)""").find(text)?.groupValues?.get(1)
        ?: error("appCodeNumber not found in BuildConfig.kt")
    return version to code
}

/**
 * 把给定版本号写回 `core/src/com/unciv/UncivGame.kt` 的
 * "AUTOMATICALLY GENERATED VERSION DATA" 区域（游戏内显示的版本号来源）。
 * 返回 true 表示文件确实被改写。
 */
internal fun syncUncivGameVersion(rootDir: File, version: String, code: String): Boolean {
    val gameInfoFile = File(rootDir, "core/src/com/unciv/UncivGame.kt")
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
    if (replacement == gameInfoText) return false
    gameInfoFile.writeText(replacement)
    return true
}

/**
 * 在 changelog 的「未发布」标题（[unreleasedHeading]，如 `## Unreleased` / `## 未发布`）
 * 之后插入新的版本小节 [heading]，使原本挂在「未发布」下的条目归入新版本。
 * 若该版本小节已存在则报错（避免重复插入）。
 *
 * [audienceHeadings] 非空时，还会为清空后的「未发布」小节补回这些受众分组标题，
 * 让后续条目继续按受众归类（约定见两个 changelog 顶部的说明）。
 */
internal fun insertChangelogVersionSection(
    file: File,
    version: String,
    heading: String,
    unreleasedHeading: String,
    audienceHeadings: List<String> = emptyList()
) {
    val text = file.readText()
    if (text.contains("## $version ") || text.contains("## $version（")) {
        error("${file.path} 已存在 $version 小节，请检查是否重复升级")
    }
    val idx = text.indexOf(unreleasedHeading)
    if (idx < 0) error("${file.path} 中未找到「$unreleasedHeading」标题")
    val lineEnd = text.indexOf('\n', idx)
    if (lineEnd < 0) error("${file.path} 的「$unreleasedHeading」位于文件末尾，无法插入")
    val before = text.substring(0, lineEnd)
    val after = text.substring(lineEnd + 1)
    val freshUnreleased =
        if (audienceHeadings.isEmpty()) ""
        else audienceHeadings.joinToString("\n\n") + "\n\n"
    file.writeText("$before\n\n$freshUnreleased$heading\n$after")
}
