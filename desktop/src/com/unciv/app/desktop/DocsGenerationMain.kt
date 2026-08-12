package com.unciv.app.desktop

/**
 * 轻量文档生成入口：不启动 LibGDX 游戏，只运行全部文档生成器。
 *
 * 供本地与 CI 使用：`./gradlew desktop:generateDocs`
 *
 * 生成内容：
 *  - UniqueDocsWriter —— 英文 uniques.md + 中文 Unique能力列表.md + Unique-parameters.md
 *  - UiElementDocsWriter —— Creating-a-UI-skin.md 中的 UI 元素表
 *  - MergeActionDocsWriter —— 6-MergeActions.md
 *  - LuaApiDefinitionWriter —— lua-api.lua（EmmyLua 类型定义）
 *  - LuaModdingDocsWriter —— Lua-API-Reference.md（模组作者 API 参考，中英两版）
 *
 * 注意：工作目录必须为 assets 目录（build.gradle.kts 中已配置 workingDir），
 * 生成器使用相对路径 ../../docs/ 写回文档。
 */
fun main() {
    UniqueDocsWriter().write()
    UniqueDocsWriter().writeChinese()
    UiElementDocsWriter().write()
    MergeActionDocsWriter().write()
    com.unciv.logic.scripting.LuaApiDefinitionWriter().write()
    LuaModdingDocsWriter().write()
    LuaModdingDocsWriter().writeChinese()
    println("Docs generation complete.")
}
