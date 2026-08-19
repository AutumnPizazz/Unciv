package com.unciv.models.ruleset

import com.unciv.models.ruleset.unique.UniqueTarget
import com.unciv.models.stats.GameResource

/**
 * A mod-defined global variable (e.g. war weariness), stored per-civilization.
 *
 * Unlike [TileResource], a Variable carries no gameplay semantics by itself -
 * it is a plain integer counter that mods read/write through uniques, conditionals and Lua.
 * It deliberately reuses the [GameResource] parameter channel so that
 * `when above [5] [WarWeariness]` and `Provides [2] [WarWeariness]` read like stats.
 */
class Variable : RulesetObject(), GameResource {

    /** Default value used when a civilization has no record for this variable yet. */
    var default = 0

    /** Whether this variable should be shown in the UI (top bar / empire overview). Default: true */
    var isDisplay = true

    /** Whether this variable stays visible even when the display menu collapses. Default: false */
    var isAlwaysDisplay = false

    /** Optional restriction to a single civilization (by name). Default: null = applies to all. */
    var uniqueTo: String? = null

    override fun getUniqueTarget() = UniqueTarget.Variable

    /** No dedicated civilopedia page for variables - suppress the icon link. */
    override fun makeLink() = ""
}
