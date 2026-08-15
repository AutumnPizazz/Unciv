package com.unciv.logic.scripting

import com.badlogic.gdx.Gdx
import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestAssets
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 保证 Lua 地图脚本文档数据表（LuaMapGenApiDocs）与 API 目录（LuaMapGenAPI.mapGenApiCatalog）
 * 永不漂移，且自动生成的产物（docs/Modders/lua-map-api.lua 与 Lua-Map-API-Reference.md 中英两版）
 * 与生成器输出一致——新增/修改地图脚本 API 后忘记运行 `./gradlew desktop:generateDocs`
 * 会在这里失败。
 */
@RunWith(GdxTestRunner::class)
class LuaMapGenApiDocsTests {

    private fun repoRoot() = run {
        TestAssets.repoRoot()
    }

    /** LuaMapGenApiDocs 条目必须与 mapGenApiCatalog 一一对应（双向，防漏加/多删/拼写漂移） */
    @Test
    fun apiDocsEntriesMatchCatalog() {
        val docsOwners = LuaMapGenApiDocs.entries.keys
        val catalogOwners = LuaMapGenAPI.mapGenApiCatalog.keys
        Assert.assertEquals(
            "LuaMapGenApiDocs owners ${docsOwners.sorted()} != catalog owners ${catalogOwners.sorted()}",
            catalogOwners.sorted(), docsOwners.sorted()
        )
        for ((owner, names) in LuaMapGenAPI.mapGenApiCatalog) {
            val docNames = LuaMapGenApiDocs.entries[owner]!!.map { it.name }.toSet()
            Assert.assertEquals(
                "LuaMapGenApiDocs '$owner' entries differ from mapGenApiCatalog (missing or extra names)",
                names, docNames
            )
        }
    }

    /** 参考文档产物必须与仓库中的文件逐字一致 */
    @Test
    fun generatedApiReferenceMatchesCheckedInFile() {
        val root = repoRoot()
        val expectedEn = LuaMapApiReferenceWriter.generate(language = null)
        val fileEn = Gdx.files.absolute("${root.path}/docs/Modders/Lua-Map-API-Reference.md")
        Assert.assertTrue("Lua-Map-API-Reference.md missing", fileEn.exists())
        Assert.assertEquals(
            "docs/Modders/Lua-Map-API-Reference.md is out of date - run ./gradlew desktop:generateDocs",
            expectedEn, fileEn.readString(Charsets.UTF_8.name())
        )

        val expectedZh = LuaMapApiReferenceWriter.generate(language = "Simplified_Chinese")
        val fileZh = Gdx.files.absolute("${root.path}/docs/zh/Modders/Lua-Map-API-Reference.md")
        Assert.assertTrue("zh/Modders/Lua-Map-API-Reference.md missing", fileZh.exists())
        Assert.assertEquals(
            "docs/zh/Modders/Lua-Map-API-Reference.md is out of date - run ./gradlew desktop:generateDocs",
            expectedZh, fileZh.readString(Charsets.UTF_8.name())
        )
    }

    /** 类型定义产物（lua-map-api.lua）必须与仓库中的文件逐字一致 */
    @Test
    fun generatedTypeDefinitionsMatchCheckedInFile() {
        val root = repoRoot()
        val expected = LuaMapApiDefinitionWriter().generate()
        val file = Gdx.files.absolute("${root.path}/docs/Modders/lua-map-api.lua")
        Assert.assertTrue("lua-map-api.lua missing", file.exists())
        Assert.assertEquals(
            "docs/Modders/lua-map-api.lua is out of date - run ./gradlew desktop:generateDocs",
            expected, file.readString(Charsets.UTF_8.name())
        )
    }

    /** 生成文档必须覆盖 catalog 中每个 API（防御性检查，防止生成逻辑漏输出） */
    @Test
    fun generatedApiReferenceCoversAllCatalogApis() {
        val generated = LuaMapApiReferenceWriter.generate(language = null)
        for ((owner, names) in LuaMapGenAPI.mapGenApiCatalog) {
            for (name in names) {
                val entry = LuaMapGenApiDocs.entries[owner]!!.first { it.name == name }
                // Properties render as a backtick list, methods as owner.name(...) call lines
                val present = if (entry.category == LuaApiDocCategory.Property)
                    generated.contains("`$name`")
                else
                    generated.contains("$owner.$name")
                Assert.assertTrue("Generated API reference is missing $owner.$name", present)
            }
        }
    }
}
