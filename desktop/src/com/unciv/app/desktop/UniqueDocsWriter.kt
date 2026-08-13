package com.unciv.app.desktop

import com.unciv.logic.map.mapunit.MapUnitCache
import com.unciv.models.ruleset.unique.Countables
import com.unciv.models.ruleset.unique.UniqueFlag
import com.unciv.models.ruleset.unique.UniqueParameterType
import com.unciv.models.ruleset.unique.UniqueTarget
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.translations.fillPlaceholders
import com.unciv.models.translations.getPlaceholderParameters
import com.unciv.utils.Log
import java.io.File

class UniqueDocsWriter : DocsWriter() {
    override val outputFileName get() = uniqueTypesFileName
    override val outputZhFileName get() = uniqueTypesZhFileName
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
        /** Same, for the Chinese docs. */
        private const val countablesZhFileName = "../../docs/zh/Modders/Unique-parameters.md"
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

    /**
     * The translation template key for a unique text: duplicate placeholders get numbered
     * (e.g. `[amount] [amount]` -> `[amount1] [amount2]`), mirroring TranslationFileWriter.getTranslatable().
     * Shown in docs so readers can tell repeated parameters apart.
     */
    private fun translatableKey(text: String): String {
        val newPlaceholders = ArrayList<String>()
        for (placeholderText in text.getPlaceholderParameters()) {
            if (placeholderText !in newPlaceholders) {
                newPlaceholders += placeholderText
            } else {
                var i = 2
                while (placeholderText + i in newPlaceholders) i++
                newPlaceholders += placeholderText + i
            }
        }
        return text.fillPlaceholders(*newPlaceholders.toTypedArray())
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

    override fun write() {
        super.write()
        writeCountables()
    }

    /** Generate the Chinese uniques list (overwrites docs/zh/开发者专区/模组开发/Unique能力列表.md). */
    override fun writeChinese() {
        super.writeChinese()
        writeCountables()
    }

    /** 纯生成 uniques 文档全文（英文或中文版），写盘交给基类 */
    override fun generate(language: String?): String {
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
                lines += doc(targetType.documentationString)
                lines += ":::"
                lines += ""
            }

            for (uniqueType in uniqueTypes) {
                if (uniqueType.getDeprecationAnnotation() != null) continue

                // unique 文本是模组 JSON 中的字面量（须与 UniqueType 逐字匹配才能生效），不可翻译。
                // 但同名参数需编号化显示（[amount] [amount] -> [amount1] [amount2]），
                // 与翻译模板 getTranslatable() 的形式一致，避免读者困惑。
                val uniqueText = if (targetType.modifierType != UniqueTarget.ModifierType.None)
                    "&lt;${translatableKey(uniqueType.text)}&gt;"
                else translatableKey(uniqueType.text)
                // VitePress collapsable container (was mkdocs `??? example "..."`).
                // 注意：容器内容**不能缩进**（4 空格/tab 会被 markdown 当作代码块渲染为 pre），
                // 与 mkdocs admonition 的缩进要求相反。
                lines += "::: details " + uniqueText
                // These blocks join the following lines; an empty line separates paragraphs.
                // The `escapeHtml` helper doubles newlines found in docDescription:
                if (uniqueType.docDescription != null || uniqueType.docDescriptionZh != null) {
                    val docDesc = if (language == "Simplified_Chinese")
                        uniqueType.docDescriptionZh ?: uniqueType.docDescription
                    else uniqueType.docDescription
                    lines += "${docDesc!!.escapeHtml(0)}\n"
                }
                if (uniqueType.parameterTypeMap.isNotEmpty()) {
                    // This one will give examples for _each_ filter in a "tileFilter/specialist/buildingFilter" kind of parameter e.g. "Farm/Merchant/Library":
                    // `val paramExamples = uniqueType.parameterTypeMap.map { it.joinToString("/") { pt -> pt.docExample } }.toTypedArray()`
                    // Might confuse modders to think "/" can go into the _actual_ unique and mean "or", so better show just one ("Farm" in the example above):
                    val paramExamples = uniqueType.parameterTypeMap.map { it.first().docExample }.toTypedArray()
                    // 示例用原始文本填充（可直接复制到模组 JSON）
                    lines += doc("Example: ") + "\"${uniqueType.text.fillPlaceholders(*paramExamples)}\"\n"
                }
                if (uniqueType.flags.contains(UniqueFlag.AcceptsSpeedModifier))
                    lines += doc("This unique's effect can be modified with ") + "&lt;${UniqueType.ModifiedByGameSpeed.text}&gt;\n"
                if (uniqueType.flags.contains(UniqueFlag.AcceptsGameProgressModifier))
                    lines += doc("This unique's effect can be modified with ") + "&lt;${UniqueType.ModifiedByGameProgress.text}&gt;\n"
                if (uniqueType in MapUnitCache.UnitMovementUniques) {
                    lines += doc("Due to performance considerations, this unique is cached, thus conditionals that may change within a turn may not work.") + "\n"
                }
                if (uniqueType.flags.contains(UniqueFlag.NoConditionals))
                    lines += doc("This unique does not support conditionals.") + "\n"
                if (uniqueType.flags.contains(UniqueFlag.HiddenToUsers))
                    lines += doc("This unique is automatically hidden from users.") + "\n"
                lines += doc("Applicable to: ") + uniqueType.allTargets().sorted()
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
            // 多行 docDescription 在 markdown 表格中会撑破表格，换行替换为 <br>
            val descText = if (language == "Simplified_Chinese")
                paramType.docDescriptionZh ?: paramType.docDescription
            else paramType.docDescription
            val description = descText!!.replace("\n", "<br>") + punctuation
            lines += "| `" + paramType.parameterName + "` | " + description + " |"
        }

        return lines.joinToString("\n")
    }

    private fun writeCountables() {
        writeCountablesFile(countablesFileName, language = null)
        writeCountablesFile(countablesZhFileName, language = "Simplified_Chinese")
    }

    /**
     * Doc-only translations for the Countables section of Unique-parameters.md.
     * Literals (countable text/example, e.g. `turns`, `[cityFilter] Cities`) must stay as-is:
     * they are used verbatim in mod JSON.
     */
    private fun countablesTranslate(text: String, language: String?): String {
        if (language != "Simplified_Chinese") return text
        if (text.startsWith("Stat name ("))  // dynamic: contains Stat enum names (literals)
            return "统计名称（" + text.removePrefix("Stat name (").removeSuffix(")") + "）"
        if (text.startsWith("Resource name - From [TileResources.json]("))
            return "资源名称 - 来自 [TileResources.json](/zh/Modders/Mod-file-structure/3-Map-related-JSON-files#tileresources-json)"
        return when (text) {
            "Integer constant - any positive or negative integer number" -> "整数常量 - 任何正整数或负整数"
            "Number of turns played" -> "已进行的回合数"
            "Always starts at zero irrespective of game speed or start era" -> "无论游戏速度或开始时代如何，始终从零开始"
            "The current year" -> "当前年份"
            "Depends on game speed or start era, negative for years BC" -> "取决于游戏速度或开始时代，公元前年份为负数"
            "The number of cities the relevant Civilization owns" -> "相关文明拥有的城市数量"
            "The number of units the relevant Civilization owns" -> "相关文明拥有的单位数量"
            "The population of the relevant City" -> "相关城市的人口"
            "The total population of the relevant Civilization" -> "相关文明的总人口"
            "The current health of the relevant Unit (0 to its Max HP)" -> "相关单位的当前生命值（0 到其最大生命值）"
            "The accumulated experience of the relevant Unit" -> "相关单位累计的经验"
            "The level of the relevant Unit (number of promotions + 1)" -> "相关单位的等级（晋升次数 + 1）"
            "The stored happiness points towards the next Golden Age" -> "为下一次黄金时代累积的笑脸点数"
            "The remaining turns of the current Golden Age" -> "当前黄金时代剩余回合数"
            "The number of technologies researched by the relevant Civilization" -> "相关文明已研究的科技数量"
            "The number of policies adopted by the relevant Civilization" -> "相关文明已采用的政策数量"
            "The combat strength of the relevant City" -> "相关城市的战斗强度"
            "Stat/Resource Per Turn" -> "统计/资源每回合"
            "Gets the stat *reserve*, not the amount per turn (can be city stats or civilization stats, depending on where the unique is used)" ->
                "获取统计*储备*，而不是每回合的数量（可以是城市统计或文明统计，取决于 unique 在何处使用）"
            "Gets the amount of a stat or resource the civilization gains per turn" -> "获取文明每回合获得的统计或资源数量"
            "The number of units being carried by this unit" -> "该单位携带的单位数量"
            "Only counts transported units matching the filter. For use with 'when number of' conditionals." ->
                "仅计算匹配过滤器的运输单位。用于 'when number of' 条件。"
            "Counts researched matching technologies for the relevant Civilization" -> "统计相关文明已研究的匹配科技"
            "Repeatable technologies, like Future Tech, are only counted once" -> "可重复科技（如未来科技）只计一次"
            "Number of the era the current player is in" -> "当前玩家所处时代的编号"
            "Zero-based index of the Era in Eras.json." -> "Eras.json 中时代的从零开始的索引。"
            "A game speed modifier for a specific Stat, as percentage" -> "特定产出的游戏速度修正，以百分比表示"
            "Chooses an appropriate field from the Speeds.json entry the player has chosen." -> "从玩家选择的 Speeds.json 条目中选择合适的字段。"
            "It is returned multiplied by 100." -> "返回值乘以 100。"
            "Food and Happiness return the generic `modifier` field." -> "食物和笑脸返回通用的 `modifier` 字段。"
            "Other fields like `goldGiftModifier` or `barbarianModifier` are not accessible with this Countable." ->
                "其他字段如 `goldGiftModifier` 或 `barbarianModifier` 不能通过此 Countable 访问。"
            "Evaluate expressions!" -> "评估表达式！"
            "Expressions support arbitrary math operations, and can include other countables, when surrounded by square brackets." ->
                "表达式支持任意数学运算，用方括号包围时可以包含其他 countable。"
            "For example, since `Cities` is a countable, and `[Melee] units` is a countable, you can have something like: `([[Melee] units] + 1) / [Cities]` (the whitespace is optional but helps readability)" ->
                "例如，`Cities` 是 countable，`[Melee] units` 也是 countable，你可以写类似：`([[Melee] units] + 1) / [Cities]`（空格可选，但有助于阅读）"
            "Since on translation, the brackets are removed, the expression will be displayed as `(Melee units + 1) / Cities`" ->
                "由于翻译时会移除方括号，表达式将显示为 `(Melee units + 1) / Cities`"
            "Supported operations between 2 values are: +, -, *, /, %, ^" -> "2 个值之间支持的操作是：+、-、*、/、%、^"
            "Supported operations on 1 value are: - (negation), √ (square root), abs (absolute value - turns negative into positive), sqrt (square root), floor (round down), ceil (round up)" ->
                "1 个值上支持的操作是：-（否定）、√（平方根）、abs（绝对值 - 将负数变为正数）、sqrt（平方根）、floor（向下取整）、ceil（向上取整）"
            "Supported functions:" -> "支持的函数："
            "Can be city stats or civilization stats, depending on where the unique is used" -> "可以是城市统计或文明统计，取决于 unique 在何处使用"
            "For example: If a unique is placed on a building, then the retrieved resources will be of the city. If placed on a policy, they will be of the civilization." ->
                "例如：如果 unique 放在建筑上，获取的资源属于城市；如果放在政策上，则属于文明。"
            "This can make a difference for e.g. local resources, which are counted per city." -> "这对例如按城市计数的本地资源会有影响。"
            else -> text
        }
    }

    private fun writeCountablesFile(fileName: String, language: String?) {
        val file = File(fileName)
        val oldContent = try {
            file.readText(Charsets.UTF_8)
        } catch (ex: Throwable) {
            Log.error("Can't read $fileName", ex)
            return
        }
        val truncateBegin = oldContent.indexOf(countablesBeginMarker)
        if (truncateBegin < 0)
            Log.error("Can't find `%s` in %s", countablesBeginMarker, fileName)
        val truncateEnd = oldContent.indexOf(countablesEndMarker)
        if (truncateEnd < 0)
            Log.error("Can't find `%s` in %s", countablesEndMarker, fileName)
        if (truncateBegin < 0 || truncateEnd < 0) return
        if (truncateEnd < truncateBegin) {
            Log.error("Inverted Countables markers in %s", countablesEndMarker, fileName)
            return
        }

        val examplePrefix = if (language == null) "Example: " else "示例："
        val newContent = StringBuilder(oldContent.length)
        newContent.append(oldContent, 0, truncateBegin + countablesBeginMarker.length)
        newContent.appendLine()

        // documentationHeader 形如 "`turns` - Number of turns played"（反引号部分是 JSON 字面量，
        // 描述部分可翻译）；也可能是纯描述文本。
        val backtickHeader = Regex("^(`[^`]+` - )(.*)$")
        for (countable in Countables.entries) {
            if (countable.getDeprecationAnnotation() != null) continue
            var header = countable.documentationHeader
            if (language != null) {
                val m = backtickHeader.find(header)
                header = if (m != null)
                    m.groupValues[1] + countablesTranslate(m.groupValues[2], language)
                else
                    countablesTranslate(header, language)
            }
            newContent.appendLine("-   $header")
            newContent.appendLine("    - $examplePrefix`Only available <when number of [${countable.example}] is more than [0]>`") // Sublist
            for (extraLine in countable.documentationStrings) {
                newContent.append("    - ") // Sublist
                newContent.appendLine(countablesTranslate(extraLine, language))
            }
        }

        newContent.appendLine()
        newContent.append(oldContent, truncateEnd, oldContent.length)
        try {
            file.writeText(newContent.toString(), Charsets.UTF_8)
        } catch (ex: Throwable) {
            Log.error("Can't write $fileName", ex)
            return
        }
    }
}
