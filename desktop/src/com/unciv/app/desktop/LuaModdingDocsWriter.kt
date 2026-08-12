package com.unciv.app.desktop

import com.unciv.logic.scripting.LuaAPI
import com.unciv.logic.scripting.LuaApiDocs
import com.unciv.logic.scripting.LuaApiReferenceWriter

/**
 * 面向模组作者的 Lua API 参考文档生成器（中英两版）。
 *
 * 产物：`docs/Modders/Lua-API-Reference.md` 与 `docs/zh/Modders/Lua-API-Reference.md`，
 * 内容全部来自 [LuaApiDocs]，教程部分（Lua-Modding.md）由人工维护并链接到本文件。
 * 新增/修改 Lua API 时同步 [LuaApiDocs] 与 [LuaAPI.apiCatalog]，
 * 再运行 `./gradlew desktop:generateDocs`。**严禁人工编辑产物。**
 */
class LuaModdingDocsWriter : DocsWriter() {
    override val outputFileName = "../../docs/Modders/Lua-API-Reference.md"
    override val outputZhFileName = "../../docs/zh/Modders/Lua-API-Reference.md"

    override fun generate(language: String?): String = LuaApiReferenceWriter.generate(language)
}
