package com.unciv.app.desktop

import com.unciv.logic.scripting.LuaMapApiReferenceWriter

/**
 * 面向模组作者的 Lua 地图脚本 API 参考文档生成器（中英两版）。
 *
 * 产物：`docs/Modders/Lua-Map-API-Reference.md` 与 `docs/zh/Modders/Lua-Map-API-Reference.md`，
 * 内容全部来自 [LuaMapGenApiDocs]，教程部分（`Lua-Modding.md` 的 "Lua Map Scripts" 章节）
 * 由人工维护并链接到本文件。
 * 新增/修改地图脚本 API 时同步 [LuaMapGenApiDocs] 与 [LuaMapGenAPI.mapGenApiCatalog]，
 * 再运行 `./gradlew desktop:generateDocs`。**严禁人工编辑产物。**
 */
class LuaMapModdingDocsWriter : DocsWriter() {
    override val outputFileName = "../../docs/Modders/Lua-Map-API-Reference.md"
    override val outputZhFileName = "../../docs/zh/Modders/Lua-Map-API-Reference.md"

    override fun generate(language: String?): String = LuaMapApiReferenceWriter.generate(language)
}
