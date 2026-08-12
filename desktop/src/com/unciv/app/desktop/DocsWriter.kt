package com.unciv.app.desktop

import java.io.File

/**
 * 文档生成器基类：统一「纯生成 + 写盘」流程，供各文档生成器复用。
 *
 * - [generate] 纯生成字符串，不碰磁盘，测试可直接调用；
 * - [write] / [writeChinese] 把产物写入磁盘。generateDocs 任务的工作目录为
 *   android/assets，各生成器用相对路径 `../../docs/` 写回仓库文档目录；
 * - [loadTranslations] 加载指定语言的翻译模板，供生成器内的文档专用文案翻译使用。
 *
 * 实现类只需提供输出路径与 [generate]；新增 API 文档时继承本类即可。
 * 产物维护原则：只改生成器源码再跑 `./gradlew desktop:generateDocs`，严禁人工编辑产物。
 */
abstract class DocsWriter {

    /** 英文产物路径（相对 assets 工作目录） */
    protected abstract val outputFileName: String

    /** 中文产物路径（相对 assets 工作目录） */
    protected abstract val outputZhFileName: String

    open fun write() {
        File(outputFileName).writeText(generate(language = null))
    }

    open fun writeChinese() {
        File(outputZhFileName).writeText(generate(language = "Simplified_Chinese"))
    }

    /** 纯生成产物全文；language 为 "Simplified_Chinese" 时输出中文版 */
    abstract fun generate(language: String?): String

    /** 加载翻译模板（jsons/translations/<language>.properties），供生成器内文案翻译 */
    protected fun loadTranslations(language: String): Map<String, String> {
        val file = File("jsons/translations/$language.properties")
        if (!file.exists()) return emptyMap()
        val translations = LinkedHashMap<String, String>()
        for (line in file.readLines(Charsets.UTF_8)) {
            if (line.isBlank() || line.startsWith('#')) continue
            val split = line.split(" = ", limit = 2)
            if (split.size == 2 && split[1].isNotEmpty()) {
                translations[split[0].replace("\\n", "\n")] = split[1].replace("\\n", "\n")
            }
        }
        return translations
    }
}
