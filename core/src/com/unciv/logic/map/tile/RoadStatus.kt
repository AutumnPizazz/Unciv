package com.unciv.logic.map.tile

import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unique.UniqueType
import yairm210.purity.annotations.Readonly

/**
 * You can use RoadStatus.name to identify [Road] and [Railroad]
 * in string-based identification, as done in [improvement].
 *
 * Note: Order is important, [ordinal] _is_ compared - please interpret as "roadLevel".
 */
enum class RoadStatus(
    val upkeep: Int = 0,
    val movement: Float = 1f,
    val movementImproved: Float = 1f,
    val removeAction: String? = null
) : IsPartOfGameInfoSerialization {

    None,
    Road (1, 0.5f, 1/3f, "Remove Road"),
    Railroad (2, 0.1f, 0.1f, "Remove Railroad");

    /**
     * Returns the improvement for this road status.
     * For backward compatibility, returns the improvement with matching name.
     * If no matching improvement found, returns null.
     */
    @Readonly fun improvement(ruleset: Ruleset) = ruleset.tileImprovements[this.name]

}
