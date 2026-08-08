package com.unciv.app.desktop

import com.unciv.logic.map.mapunit.MapUnitCache
import com.unciv.models.ruleset.unique.Countables
import com.unciv.models.ruleset.unique.UniqueFlag
import com.unciv.models.ruleset.unique.UniqueParameterType
import com.unciv.models.ruleset.unique.UniqueTarget
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.translations.fillPlaceholders
import com.unciv.utils.Log
import java.io.File

class UniqueDocsWriter {
    companion object {
        /** Where the UniqueType file is to be overwritten,
         *  relative to the current (assets) directory (not incluenced by `--data-dir=`). */
        private const val uniqueTypesFileName = "../../docs/Modders/uniques.md"

        /** Where the Chinese UniqueType file is to be overwritten,
         *  relative to the current (assets) directory. */
        private const val uniqueTypesZhFileName = "../../docs/zh/Modders/uniques.md"

        /** Where the Countables documentation is to inserted,
         *  relative to the current (assets) directory (not incluenced by `--data-dir=`). */
        private const val countablesFileName = "../../docs/Modders/Unique-parameters.md"
        /** Where in the documentation file the Countables are to be inserted, start marker. */
        private const val countablesBeginMarker = "[//]: # (Countables automatically generated BEGIN)"
        /** Where in the documentation file the Countables are to be inserted, end marker. An empty line will always be inserted above it. */
        private const val countablesEndMarker = "[//]: # (Countables automatically generated END)"

        /**
         * Switch from each Unique shown once under one UniqueTarget heading chosen from targetTypes (`true`)
         * to showing each Unique repeatedly under each UniqueTarget heading it applies to (`false`).
         */
        private const val showUniqueOnOneTarget = false

        /** Switch **on** the display of _inherited_ UniqueTargets in "Applicable to:" */
        private const val showInheritedTargets = false

        private fun UniqueTarget.allTargets(): Sequence<UniqueTarget> = sequence {
            if (showInheritedTargets && inheritsFrom != null) yieldAll(inheritsFrom!!.allTargets())
            yield(this@allTargets)
        }
        private fun UniqueType.allTargets(): Sequence<UniqueTarget> =
            targetTypes.asSequence().flatMap { it.allTargets() }.distinct()
        private fun UniqueTarget.allUniqueTypes(): Sequence<UniqueType> =
            UniqueType.entries.asSequence().filter {
                this in it.targetTypes
            }

        /** Doc-only sentences (not in the translation templates), translated for the Chinese docs output. */
        private fun docsSentence(sentence: String, language: String?): String {
            if (language != "Simplified_Chinese") return sentence
            return when (sentence) {
                "Example: " -> "示例："
                "Applicable to: " -> "适用范围："
                "This unique's effect can be modified with " -> "此词条的效果可被 "
                "Due to performance considerations, this unique is cached, thus conditionals that may change within a turn may not work." ->
                    "由于性能考虑，此词条会被缓存，回合内可能变化的条件可能不生效。"
                "This unique does not support conditionals." -> "此词条不支持条件。"
                "This unique is automatically hidden from users." -> "此词条自动对用户隐藏。"
                // UniqueTarget.documentationString translations (doc-only, not in translation templates)
                "Uniques that have immediate, one-time effects. These can be added to techs to trigger when researched, to policies to trigger when adopted, to eras to trigger when reached, to buildings to trigger when built. Alternatively, you can add a TriggerCondition to them to make them into Global uniques that activate upon a specific event.They can also be added to units to grant them the ability to trigger this effect as an action, which can be modified with UnitActionModifier and UnitTriggerCondition conditionals." ->
                    "具有即时、一次性效果的词条。可添加到科技（研究时触发）、政策（采用时触发）、时代（到达时触发）、建筑（建造时触发）；或为其添加触发条件（TriggerCondition），使其成为在特定事件时激活的全局词条。也可添加到单位，赋予其将效果作为行动触发的能力（可用 UnitActionModifier / UnitTriggerCondition 条件修饰）。"
                "Uniques that have immediate, one-time effects on a unit.They can be added to units (on unit, unit type, or promotion) to grant them the ability to trigger this effect as an action, which can be modified with UnitActionModifier and UnitTriggerCondition conditionals." ->
                    "对单位产生即时、一次性效果的词条。可添加到单位（单位、单位类型或晋升），赋予其将效果作为行动触发的能力（可用 UnitActionModifier / UnitTriggerCondition 条件修饰）。"
                "Special conditionals that can be added to Triggerable uniques, to make them activate upon specific actions." ->
                    "可添加到触发型词条的特殊条件，使它们在特定行动时激活。"
                "Special conditionals that can be added to UnitTriggerable uniques, to make them activate upon specific actions." ->
                    "可添加到单位触发型词条的特殊条件，使它们在特定行动时激活。"
                "Modifiers that can be added to other uniques to limit when they will be active" ->
                    "可添加到其他词条的修饰符，用于限制其生效时机"
                "Modifiers that can be added to other uniques changing user experience, not their behavior" ->
                    "可添加到其他词条的修饰符，改变用户体验而非行为"
                "Modifiers that can be added to UnitAction uniques as conditionals" ->
                    "可作为条件添加到单位行动词条的修饰符"
                "Uniques that apply globally. Civs gain the abilities of these uniques from nation uniques, reached eras, researched techs, adopted policies, built buildings, religion 'founder' uniques, owned resources, and ruleset-wide global uniques." ->
                    "全局生效的词条。文明从国家词条、已到达的时代、已研究的科技、已采用的政策、已建造的建筑、宗教「创始人」词条、拥有的资源以及规则集全局词条中获得这些能力。"
                "Uniques for Founder and Enhancer type Beliefs, that will apply to the founder of this religion" ->
                    "创始人及增强类信条的词条，作用于该宗教的创始人"
                "Uniques for Pantheon and Follower type beliefs, that will apply to each city where the religion is the majority religion" ->
                    "万神殿与追随者类信条的词条，作用于该宗教为主导宗教的每座城市"
                "Speed uniques will be treated as part of GlobalUniques for the Speed selected in a game" ->
                    "速度词条将作为所选游戏速度的 GlobalUniques 的一部分"
                "Difficulty uniques will be treated as part of GlobalUniques for the Difficulty selected in a game" ->
                    "难度词条将作为所选游戏难度的 GlobalUniques 的一部分"
                "Uniques that affect a unit's actions, and can be modified by UnitActionModifiers" ->
                    "影响单位行动的词条，可用 UnitActionModifiers 修饰"
                "Uniques that can be added to units, unit types, or promotions" ->
                    "可添加到单位、单位类型或晋升的词条"
                else -> sentence
            }
        }

    }

    @Suppress("unused")  // was used in the past? 
    /** Create the anchor-navigation part to append to a link for a given header */
    fun toLink(string: String): String {
        return "#" + string.split(' ').joinToString("-") { it.lowercase() }
    }

    // Thanks https://github.com/ktorio/ktor/blob/d89d41ef6dc91479e6c13c25eb306abc15040b8e/ktor-utils/common/src/io/ktor/util/Text.kt#L7-L28
    /** An escaping routine specifically for mkdocs input: `<>` are escaped **unless** within backticks, and newlines are doubled */
    private fun String.escapeHtml(indent: Int = 0) = buildString(capacity = length + indent + 6) {
        var inCodeBlock = false
        for (char in this@escapeHtml) {
            when(char) {
                //'\'' -> append("&#x27;") // not necessary for this case
                //'\"' -> append("&quot;") // not necessary for this case
                '`' -> {
                    inCodeBlock = !inCodeBlock
                    append('`')
                }
                '&' -> append("&amp;")
                '<' -> append(if (inCodeBlock) "<" else "&lt;")
                '>' -> append(if (inCodeBlock) ">" else "&gt;")
                '\n' -> append("\n\n" + "\t".repeat(indent))
                else -> append(char)
            }
        }
    }

    fun write() {
        writeUniqueTypes(uniqueTypesFileName, language = null)
        writeCountables()
    }

    /** Generate the Chinese uniques list (overwrites docs/zh/开发者专区/模组开发/Unique能力列表.md). */
    fun writeChinese() {
        writeUniqueTypes(uniqueTypesZhFileName, language = "Simplified_Chinese")
    }

    private fun loadTranslations(language: String): Map<String, String> {
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

    private fun writeUniqueTypes(outputFileName: String, language: String?) {
        val translations = language?.let { loadTranslations(it) } ?: emptyMap()
        fun tr(text: String) = translations[text] ?: text
        fun doc(text: String) = docsSentence(text, language)

        // This will output each unique only once, even if it has several targets.
        // Each is grouped under the UniqueTarget is is allowed for with the lowest enum ordinal.
        // UniqueTarget.inheritsFrom is _not_ resolved for this.
        // The UniqueType are shown in enum order within their group, and groups are ordered
        // by their UniqueTarget.ordinal as well - source code order.
        val targetTypesToUniques: Map<UniqueTarget, List<UniqueType>> =
            if (showUniqueOnOneTarget)
                UniqueType.entries
                    .groupBy { it.targetTypes.minOrNull()!! }
                    .toSortedMap()
            else
        // if, on the other hand, we wish to list every UniqueType with multiple targets under
        // _each_ of the groups it is applicable to, then this might do:
                UniqueTarget.entries.asSequence().associateWith { target ->
                    target.allTargets().flatMap { inheritedTarget ->
                        inheritedTarget.allUniqueTypes()
                    }.distinct().toList()
                }

        val capacity = 25 + targetTypesToUniques.size + UniqueType.entries.size * (if (showUniqueOnOneTarget) 3 else 16)
        val lines = ArrayList<String>(capacity)
        if (language == null) {
            lines += "# Uniques"
            lines += "An overview of uniques can be found [here](../Developers/Uniques.md)"
            lines += "\nSimple unique parameters are explained by mouseover. Complex parameters are explained in [Unique parameter types](Unique-parameters.md)"
        } else {
            lines += "---"
            lines += "title: Unique 能力列表"
            lines += "---"
            lines += ""
            lines += "<!-- 本文件由 desktop/src/com/unciv/app/desktop/UniqueDocsWriter.kt 自动生成，请勿手动编辑 -->"
            lines += ""
            lines += "# Unique 能力列表"
            lines += ""
            lines += "> 本列表由游戏代码自动生成，随版本保持最新。"
            lines += "> Uniques 概述可以在[这里](../Developers/Uniques.md)找到。"
            lines += "> 简单的 Unique 参数见文末参数表，复杂的参数在 [Unique 参数类型](Unique-parameters.md) 中说明。"
        }
        lines += ""

        for ((targetType, uniqueTypes) in targetTypesToUniques) {
            if (uniqueTypes.isEmpty()) continue
            lines += "## " + if (language == null) targetType.name + " uniques" else targetType.name + " uniques（" + tr(targetType.name) + "词条）"

            if (targetType.documentationString.isNotEmpty()) {
                // VitePress admonition container (was mkdocs `!!! note ""`)
                lines += "::: note"
                lines += ""
                lines += "    " + doc(targetType.documentationString)
                lines += ":::"
                lines += ""
            }

            for (uniqueType in uniqueTypes) {
                if (uniqueType.getDeprecationAnnotation() != null) continue

                // unique 文本是模组 JSON 中的字面量（须与 UniqueType 逐字匹配才能生效），不可翻译
                val uniqueText = if (targetType.modifierType != UniqueTarget.ModifierType.None)
                    "&lt;${uniqueType.text}&gt;"
                else uniqueType.text
                // VitePress collapsable container (was mkdocs `??? example "..."`)
                lines += "::: details " + uniqueText
                // These blocks will join all indented lines that follow, they need an empty line followed by more indented lines to render one break.
                // Thus, all optional lines up to "Applicable" get an extra `\n`, and the `escapeHtml` helper doubles newlines found in docDescription:
                if (uniqueType.docDescription != null)
                    lines += "\t${tr(uniqueType.docDescription!!).escapeHtml(1)}\n"
                if (uniqueType.parameterTypeMap.isNotEmpty()) {
                    // This one will give examples for _each_ filter in a "tileFilter/specialist/buildingFilter" kind of parameter e.g. "Farm/Merchant/Library":
                    // `val paramExamples = uniqueType.parameterTypeMap.map { it.joinToString("/") { pt -> pt.docExample } }.toTypedArray()`
                    // Might confuse modders to think "/" can go into the _actual_ unique and mean "or", so better show just one ("Farm" in the example above):
                    val paramExamples = uniqueType.parameterTypeMap.map { it.first().docExample }.toTypedArray()
                    lines += "\t" + doc("Example: ") + "\"${uniqueText.fillPlaceholders(*paramExamples)}\"\n"
                }
                if (uniqueType.flags.contains(UniqueFlag.AcceptsSpeedModifier))
                    lines += "\t" + doc("This unique's effect can be modified with ") + "&lt;${UniqueType.ModifiedByGameSpeed.text}&gt;\n"
                if (uniqueType.flags.contains(UniqueFlag.AcceptsGameProgressModifier))
                    lines += "\t" + doc("This unique's effect can be modified with ") + "&lt;${UniqueType.ModifiedByGameProgress.text}&gt;\n"
                if (uniqueType in MapUnitCache.UnitMovementUniques) {
                    lines += "\t" + doc("Due to performance considerations, this unique is cached, thus conditionals that may change within a turn may not work.") + "\n"
                }
                if (uniqueType.flags.contains(UniqueFlag.NoConditionals))
                    lines += "\t" + doc("This unique does not support conditionals.") + "\n"
                if (uniqueType.flags.contains(UniqueFlag.HiddenToUsers))
                    lines += "\t" + doc("This unique is automatically hidden from users.") + "\n"
                lines += "\t" + doc("Applicable to: ") + uniqueType.allTargets().sorted()
                    .joinToString(if (language == null) ", " else "，") { it.name }
                lines += ""
                lines += ":::"
            }
        }

        // 参数说明表（mkdocs 缩写 `*[param]: desc` 语法 VitePress 不识别，改为标准表格）
        lines += ""
        if (language == null) {
            lines += "## Unique parameter types"
            lines += ""
            lines += "| Parameter | Description |"
        } else {
            lines += "## Unique 参数类型"
            lines += ""
            lines += "| 参数 | 说明 |"
        }
        lines += "|---|---|"
        // order irrelevant for rendered wiki, but could potentially reduce source control differences
        for (paramType in UniqueParameterType.entries.asSequence().sortedBy { it.parameterName }) {
            if (paramType.docDescription == null) continue
            val punctuation = if (paramType.docDescription!!.last().category == '.'.category) "" else "."
            lines += "| `" + paramType.parameterName + "` | " + tr(paramType.docDescription!!) + punctuation + " |"
        }

        File(outputFileName).writeText(lines.joinToString("\n"))
    }

    private fun writeCountables() {
        val file = File(countablesFileName)
        val oldContent = try {
            file.readText(Charsets.UTF_8)
        } catch (ex: Throwable) {
            Log.error("Can't read $countablesFileName", ex)
            return
        }
        val truncateBegin = oldContent.indexOf(countablesBeginMarker)
        if (truncateBegin < 0)
            Log.error("Can't find `%s` in %s", countablesBeginMarker, countablesFileName)
        val truncateEnd = oldContent.indexOf(countablesEndMarker)
        if (truncateEnd < 0)
            Log.error("Can't find `%s` in %s", countablesEndMarker, countablesFileName)
        if (truncateBegin < 0 || truncateEnd < 0) return
        if (truncateEnd < truncateBegin) {
            Log.error("Inverted Countables markers in %s", countablesEndMarker, countablesFileName)
            return
        }

        val newContent = StringBuilder(oldContent.length)
        newContent.append(oldContent, 0, truncateBegin + countablesBeginMarker.length)
        newContent.appendLine()

        for (countable in Countables.entries) {
            if (countable.getDeprecationAnnotation() != null) continue
            newContent.appendLine("-   ${countable.documentationHeader}")
            newContent.appendLine("    - Example: `Only available <when number of [${countable.example}] is more than [0]>`") // Sublist
            for (extraLine in countable.documentationStrings) {
                newContent.append("    - ") // Sublist
                newContent.appendLine(extraLine)
            }
        }

        newContent.appendLine()
        newContent.append(oldContent, truncateEnd, oldContent.length)
        try {
            file.writeText(newContent.toString(), Charsets.UTF_8)
        } catch (ex: Throwable) {
            Log.error("Can't write $countablesFileName", ex)
            return
        }
    }
}
