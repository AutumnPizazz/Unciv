package com.unciv.models.ruleset.unique

import com.unciv.models.ruleset.Ruleset
import com.unciv.models.stats.Stat
import yairm210.purity.annotations.Readonly

/** Extracts mod-defined variable entries from a [Unique]'s `[stats]`-style parameter.
 *
 *  A stats parameter like `+2 Gold, +1 MyVar` mixes built-in stats and mod variables.
 *  [Stats.parseLenient][com.unciv.models.stats.Stats.parseLenient] takes care of the stat part;
 *  this parser collects the variable part (`MyVar -> +1`). Entries whose name is neither a
 *  built-in stat nor a ruleset variable are silently ignored (they fail validation elsewhere). */
object VariableStatsParser {

    private val entryRegex = Regex("([+-])(\\d+) (.+)")

    /** Extracts the variable amounts from a [stats]-style parameter, keyed by variable name. */
    @Readonly
    fun extract(statsParam: String, ruleset: Ruleset): Map<String, Int> {
        val result = LinkedHashMap<String, Int>()
        statsParam.split(", ").forEach { entry ->
            val match = entryRegex.matchEntire(entry) ?: return@forEach
            val name = match.groupValues[3]
            if (Stat.isStat(name)) return@forEach // built-in stat - handled by Stats.parseLenient
            val variable = ruleset.variables[name] ?: return@forEach // not a variable either
            val amount = match.groupValues[2].toInt() * (if (match.groupValues[1] == "-") -1 else 1)
            result[variable.name] = (result[variable.name] ?: 0) + amount
        }
        return result
    }
}
