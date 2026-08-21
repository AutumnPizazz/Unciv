package com.unciv.models.ruleset

import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.unique.UniqueTarget
import com.unciv.models.stats.GameResource
import yairm210.purity.annotations.Readonly

/** The storage scope of a mod-defined variable.
 *  - [City]: stored per-city (e.g. loyalty, housing, amenities - simulating Civ6 city stats)
 *  - [Civ]: stored per-civilization (e.g. war weariness) - the original, unchanged behaviour
 *  - [Global]: stored game-wide (e.g. world tension, greenhouse effect, nuclear pollution) */
enum class VariableScope { City, Civ, Global }

/**
 * A mod-defined variable (e.g. war weariness), stored per-scope.
 *
 * Unlike [TileResource], a Variable carries no gameplay semantics by itself -
 * it is a plain integer counter that mods read/write through uniques, conditionals and Lua.
 * It deliberately reuses the [GameResource] parameter channel so that
 * `when above [5] [WarWeariness]` and `Provides [2] [WarWeariness]` read like stats.
 *
 * Three scopes ([VariableScope]) are supported, with separate unique channels:
 * - `civ`: existing behaviour, `when above [5] [WarWeariness]`
 * - `city`: stored per city, uniques carry a [cityFilter], `when above [5] [Loyalty] in this city`
 * - `global`: stored game-wide, uniques carry the `globally` marker, `when above [50] [WorldTension] globally`
 */
class Variable : RulesetObject(), GameResource {

    /** The storage scope of this variable. Must be declared explicitly in Variables.json (validation error when missing). */
    var scope: VariableScope? = null

    /** Default value used when a scope has no record for this variable yet. */
    var default = 0

    /** Optional lower bound; every write (set/add/provides/per-turn settlement/Lua) is clamped to at least this value. */
    var min: Int? = null

    /** Optional upper bound; every write (set/add/provides/per-turn settlement/Lua) is clamped to at most this value. */
    var max: Int? = null

    /** Whether this variable should be shown in the UI (top bar / empire overview). Default: true */
    var isDisplay = true

    /** Whether this variable stays visible even when the display menu collapses. Default: false */
    var isAlwaysDisplay = false

    /** Optional restriction to a single civilization (by name). City/civ variables are unavailable to other civilizations;
     *  declaring it for [VariableScope.Global] is a validation error. Default: null = applies to all. */
    var uniqueTo: String? = null

    /** The effective scope, falling back to [VariableScope.Civ] for records missing an explicit declaration. */
    val resolvedScope: VariableScope get() = scope ?: VariableScope.Civ

    /** Whether this variable is available to the given civilization under [uniqueTo]. */
    @Readonly
    fun isAvailableTo(civInfo: Civilization): Boolean =
        uniqueTo == null || civInfo.matchesFilter(uniqueTo!!)

    /** Clamps [value] to the declared [min]/[max] bounds (no-op when both are null). */
    fun clamp(value: Int): Int {
        var result = value
        if (min != null && result < min!!) result = min!!
        if (max != null && result > max!!) result = max!!
        return result
    }

    /** Adds [amount] to [current] without overflowing, then clamps to the declared bounds. */
    fun clampAdd(current: Int, amount: Int): Int {
        var result = current.toLong() + amount.toLong()
        if (min != null && result < min!!.toLong()) result = min!!.toLong()
        if (max != null && result > max!!.toLong()) result = max!!.toLong()
        return result.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
    }

    override fun getUniqueTarget() = UniqueTarget.Variable

    /** No dedicated civilopedia page for variables - suppress the icon link. */
    override fun makeLink() = ""
}
