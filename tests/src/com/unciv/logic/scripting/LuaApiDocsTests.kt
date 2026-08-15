package com.unciv.logic.scripting

import com.badlogic.gdx.Gdx
import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestAssets
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 保证 Lua 文档数据表（LuaApiDocs）与 API 目录（LuaAPI.apiCatalog）永不漂移，
 * 且自动生成的 API 参考文档（docs/Modders/Lua-API-Reference.md 中英两版）
 * 与生成器输出一致——新增/修改 Lua API 后忘记运行 `./gradlew desktop:generateDocs`
 * 会在这里失败。
 */
@RunWith(GdxTestRunner::class)
class LuaApiDocsTests {

    private fun repoRoot() = run {
        TestAssets.repoRoot()
    }

    /** LuaApiDocs 条目必须与 apiCatalog 一一对应（双向，防漏加/多删/拼写漂移） */
    @Test
    fun apiDocsEntriesMatchCatalog() {
        val docsOwners = LuaApiDocs.entries.keys
        val catalogOwners = LuaAPI.apiCatalog.keys
        Assert.assertEquals(
            "LuaApiDocs owners ${docsOwners.sorted()} != apiCatalog owners ${catalogOwners.sorted()}",
            catalogOwners.sorted(), docsOwners.sorted()
        )
        for ((owner, names) in LuaAPI.apiCatalog) {
            val docNames = LuaApiDocs.entries[owner]!!.map { it.name }.toSet()
            Assert.assertEquals(
                "LuaApiDocs '$owner' entries differ from apiCatalog (missing or extra names)",
                names, docNames
            )
        }
    }

    /** 生成产物必须与仓库中的文件逐字一致（防改数据表后忘重新生成） */
    @Test
    fun generatedApiReferenceMatchesCheckedInFile() {
        val root = repoRoot()
        val expectedEn = LuaApiReferenceWriter.generate(language = null)
        val fileEn = Gdx.files.absolute("${root.path}/docs/Modders/Lua-API-Reference.md")
        Assert.assertTrue("Lua-API-Reference.md missing", fileEn.exists())
        Assert.assertEquals(
            "docs/Modders/Lua-API-Reference.md is out of date - run ./gradlew desktop:generateDocs",
            expectedEn, fileEn.readString(Charsets.UTF_8.name())
        )

        val expectedZh = LuaApiReferenceWriter.generate(language = "Simplified_Chinese")
        val fileZh = Gdx.files.absolute("${root.path}/docs/zh/Modders/Lua-API-Reference.md")
        Assert.assertTrue("zh/Modders/Lua-API-Reference.md missing", fileZh.exists())
        Assert.assertEquals(
            "docs/zh/Modders/Lua-API-Reference.md is out of date - run ./gradlew desktop:generateDocs",
            expectedZh, fileZh.readString(Charsets.UTF_8.name())
        )
    }

    /** 生成文档必须覆盖 catalog 中每个 API（防御性检查，防止生成逻辑漏输出） */
    @Test
    fun generatedApiReferenceCoversAllCatalogApis() {
        val generated = LuaApiReferenceWriter.generate(language = null)
        for ((owner, names) in LuaAPI.apiCatalog) {
            for (name in names) {
                val entry = LuaApiDocs.entries[owner]!!.first { it.name == name }
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
