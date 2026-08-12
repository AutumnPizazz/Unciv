package com.unciv.logic.scripting

/**
 * 生成面向模组作者的 Lua API 参考文档（`docs/Modders/Lua-API-Reference.md` 与
 * `docs/zh/Modders/Lua-API-Reference.md`），内容全部来自 [LuaApiDocs]，
 * 与 [LuaAPI.apiCatalog] 保持一致（由 LuaApiDocsTests 校验），文档永不与实现漂移。
 *
 * 教程部分（`Lua-Modding.md`）由人工维护并链接到本文件；新增/修改 API 时只需
 * 同步 [LuaApiDocs] 与 [LuaAPI.apiCatalog]，再运行 `./gradlew desktop:generateDocs`。
 * **严禁人工编辑本生成产物。**
 *
 * 地图脚本（map-gen）API 的参考文档由 [LuaMapApiReferenceWriter] 生成，
 * 两者共用底层的 [generateReference]。
 */
object LuaApiReferenceWriter {

    private val ownerOrder = listOf("civ", "city", "unit", "tile", "game", "ctx", "store")

    private fun ownerTitle(owner: String, language: String?): String = when (owner) {
        "civ" -> if (language == null) "civ — Civilization" else "civ — 文明"
        "city" -> if (language == null) "city — City" else "city — 城市"
        "unit" -> if (language == null) "unit — Unit" else "unit — 单位"
        "tile" -> if (language == null) "tile — Tile" else "tile — 地块"
        "game" -> if (language == null) "game — Global" else "game — 全局"
        "ctx" -> if (language == null) "ctx — Context" else "ctx — 上下文"
        "store" -> if (language == null) "ctx.store — Persistent Storage" else "ctx.store — 持久化存储"
        else -> owner
    }

    /** 生成全文；language 为 "Simplified_Chinese" 时输出中文版 */
    fun generate(language: String?): String {
        val title = if (language == null) "Lua API Reference" else "Lua API 参考"
        val intro = if (language == null) {
            """
            This page is generated from the game code and lists every function and property of the
            Lua context tables (`ctx`). It can never drift from the implementation - when a new API is
            added, this page is regenerated together with the type definitions (`lua-api.lua`).

            For tutorials, triggers and editor setup see [Lua Mod Scripts](Lua-Modding.md).

            > **Note on signatures**: types follow the EmmyLua style used by the LuaLS language server
            > (`lua-api.lua`); `|nil` means the value may be absent, `string[]` a list of strings.
            """.trimIndent()
        } else {
            """
            本页面由游戏代码自动生成，列出 Lua 上下文表（`ctx`）的全部函数与属性。它永远不会与实现
            脱节——新增 API 时，本页面与类型定义（`lua-api.lua`）会一起重新生成。

            教程、触发方式与编辑器设置见 [Lua 脚本](Lua-Modding.md)。

            > **关于签名**：类型采用 LuaLS 语言服务器（`lua-api.lua`）使用的 EmmyLua 风格；
            > `|nil` 表示该值可能为空，`string[]` 表示字符串列表。
            """.trimIndent()
        }
        return generateReference(ownerOrder, ::ownerTitle, title, intro, LuaApiDocs.entries, language)
    }
}

/**
 * 生成面向模组作者的 Lua 地图脚本 API 参考文档（`docs/Modders/Lua-Map-API-Reference.md` 与
 * `docs/zh/Modders/Lua-Map-API-Reference.md`），内容全部来自 [LuaMapGenApiDocs]，
 * 与 [LuaMapGenAPI.mapGenApiCatalog] 保持一致（由 LuaMapGenApiDocsTests 校验）。
 *
 * 地图脚本的 ctx 是**生成期专用**的独立 API（没有 civ/city/unit），与游戏内
 * [LuaApiReferenceWriter] 分开成页；教程部分（`Lua-Modding.md` 的 "Lua Map Scripts" 章节）
 * 由人工维护并链接到本文件。
 */
object LuaMapApiReferenceWriter {

    private val ownerOrder = listOf("ctx", "params", "size", "bounds", "map", "tile")

    private fun ownerTitle(owner: String, language: String?): String = when (owner) {
        "ctx" -> if (language == null) "ctx — Map-Script Context" else "ctx — 地图脚本上下文"
        "params" -> if (language == null) "ctx.params — Map Parameters" else "ctx.params — 地图参数"
        "size" -> if (language == null) "ctx.params.size — Map Size" else "ctx.params.size — 地图尺寸"
        "bounds" -> if (language == null) "ctx.params.bounds — Tile Bounds" else "ctx.params.bounds — 地块边界"
        "map" -> if (language == null) "ctx.map — Map Manipulation" else "ctx.map — 地图操作"
        "tile" -> if (language == null) "MapGen Tile — Tile" else "MapGen Tile — 地块"
        else -> owner
    }

    /** 生成全文；language 为 "Simplified_Chinese" 时输出中文版 */
    fun generate(language: String?): String {
        val title = if (language == null) "Lua Map-Script API Reference" else "Lua 地图脚本 API 参考"
        val intro = if (language == null) {
            """
            This page is generated from the game code and lists the **generation-only** API available
            inside `GenerateMap(ctx)` map scripts - a separate, sandboxed context with no civs, cities
            or units. It can never drift from the implementation (see [Lua Mod Scripts](Lua-Modding.md)
            for how map scripts work).

            For editor autocompletion point the LuaLS language server at `lua-map-api.lua` (same
            `.luarc.json` setup as the main API, listing both files under `workspace.library`).

            > **Note on signatures**: `opts?` parameters are optional tables (see the type
            > definitions in `lua-map-api.lua` for their fields); `|nil` means the value may be absent.
            """.trimIndent()
        } else {
            """
            本页面由游戏代码自动生成，列出 `GenerateMap(ctx)` 地图脚本中可用的**生成期专用** API——
            一个独立的沙盒上下文，没有 civ/城市/单位。它永远不会与实现脱节（地图脚本的工作方式
            见 [Lua 脚本](Lua-Modding.md)）。

            编辑器补全：把 LuaLS 语言服务器指向 `lua-map-api.lua`（与主 API 相同的 `.luarc.json`
            配置，两个文件都列入 `workspace.library`）。

            > **关于签名**：`opts?` 参数为可选表（字段见 `lua-map-api.lua` 中的类型定义）；
            > `|nil` 表示该值可能为空。
            """.trimIndent()
        }
        return generateReference(ownerOrder, ::ownerTitle, title, intro, LuaMapGenApiDocs.entries, language)
    }
}

/**
 * 参考文档生成核心：每个 owner 一节，按类别分「属性 / 查询方法 / 写入方法」，
 * 方法以 `owner.name(args)` + 对齐注释的 Lua 代码块展示。
 * 游戏内 API 与地图脚本 API 共用。
 */
internal fun generateReference(
    ownerOrder: List<String>,
    ownerTitle: (String, String?) -> String,
    title: String,
    intro: String,
    entries: Map<String, List<LuaApiDocEntry>>,
    language: String?
): String {
    val categoryTitle = { category: LuaApiDocCategory ->
        when (category) {
            LuaApiDocCategory.Property -> if (language == null) "Properties" else "属性"
            LuaApiDocCategory.Query -> if (language == null) "Query methods" else "查询方法"
            LuaApiDocCategory.Write -> if (language == null) "Write methods" else "写入方法"
        }
    }

    /** 从 EmmyLua 签名提取调用形式：fun(a: T, b: U) -> "a, b"；fun() -> "" */
    fun callArguments(signature: String): String {
        if (!signature.startsWith("fun(")) return ""
        val params = signature.removePrefix("fun(").substringBefore(")").trim()
        if (params.isEmpty()) return ""
        return params.split(", ").joinToString(", ") { it.substringBefore(":") }
    }

    val out = StringBuilder()
    out.appendLine("<!-- 本文件由 LuaApiReferenceWriter / LuaMapApiReferenceWriter 自动生成，请勿手动编辑。 -->")
    out.appendLine("<!-- Generated from the LuaApiDocs / LuaMapGenApiDocs tables - never edit by hand. -->")
    out.appendLine("<!-- 重新生成 / Regenerate with: ./gradlew desktop:generateDocs -->")
    out.appendLine()
    out.appendLine("# $title")
    out.appendLine()
    out.appendLine(intro)
    out.appendLine()

    for (owner in ownerOrder) {
        val ownerEntries = entries[owner] ?: continue
        out.appendLine("## ${ownerTitle(owner, language)}")
        out.appendLine()
        for (category in LuaApiDocCategory.entries) {
            val members = ownerEntries.filter { it.category == category }
            if (members.isEmpty()) continue
            out.appendLine("**${categoryTitle(category)}**:")
            out.appendLine()
            if (category == LuaApiDocCategory.Property) {
                out.appendLine("`" + members.joinToString("`, `") { it.name } + "`")
                out.appendLine()
                continue
            }
            out.appendLine("```lua")
            // 行宽 = owner. 前缀（含点号）+ 方法名 + 参数 + 括号 + 至少 1 个分隔空格
            val width = members.maxOf {
                owner.length + 1 + it.name.length + callArguments(it.signature).length + 2
            } + 1
            for (entry in members) {
                val call = "${owner}.${entry.name}(${callArguments(entry.signature)})"
                val desc = if (language == null) entry.desc else entry.descZh
                out.appendLine(call.padEnd(width) + "-- " + desc)
            }
            out.appendLine("```")
            out.appendLine()
        }
    }
    return out.toString().trimEnd() + "\n"
}
