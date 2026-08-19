package com.unciv.models.ruleset

import com.unciv.models.ModConstants
import com.unciv.models.ruleset.unique.IHasUniques
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueMap
import com.unciv.models.ruleset.unique.UniqueTarget

class ModOptions : IHasUniques {
    //region Modder choices
    var isBaseRuleset = false
    var techsToRemove = HashSet<String>()
    var buildingsToRemove = HashSet<String>()
    var unitsToRemove = HashSet<String>()
    var nationsToRemove = HashSet<String>()
    var policyBranchesToRemove = HashSet<String>()
    var policiesToRemove = HashSet<String>()
    var beliefsToRemove = HashSet<String>()
    var religionsToRemove = HashSet<String>()
    val constants = ModConstants()
    var unitset: String? = null
    var tileset: String? = null

    //region Variables (see Variables.json)
    /** When the number of displayed variables exceeds this, only always-display variables stay in the top bar.
     *  0 (default) = no limit. */
    var variableMenuThreshold = 0
    /** Number of always-display variables kept visible when the top bar menu collapses.
     *  0 (default) = all always-display variables. */
    var alwaysDisplayVariableCount = 0
    //endregion
    //endregion

    //region Metadata, automatic
    var modUrl = ""
    var defaultBranch = "master"
    var author = ""
    var lastUpdated = ""
    var modSize = 0
    var topics = mutableListOf<String>()
    //endregion

    //region Version requirements
    /** Mod version, format n.n.n; defaults to 0.0.1 when missing */
    var modVersion = "0.0.1"
    /**
     * Recommended game version this mod version was made for, exact match (n.n.n / n.n.n.n / -patchN);
     * empty = not declared.
     */
    var recommendedGameVersion = ""
    /** Dependency mods with version requirements */
    var modDependencies = mutableListOf<ModDependency>()
    //endregion

    //region Version requirement checks
    /**
     * Returns a warning text when the current game version does not exactly match [recommendedGameVersion],
     * or when the declared recommended version is invalid. Returns null when not declared or matched.
     *
     * The returned text keeps `[...]` placeholders filled with actual values, ready for `tr()`.
     */
    fun getGameVersionWarning(currentGameVersion: String): String? {
        val recommended = recommendedGameVersion.trim()
        if (recommended.isEmpty()) return null
        val recommendedVersion = ModVersion.parse(recommended)
        if (recommendedVersion == null)
            return "Invalid recommendedGameVersion '[version]' in mod '[modName]'"
                .replace("[version]", "[$recommended]").replace("[modName]", "[$name]")
        val currentVersion = ModVersion.parse(currentGameVersion) ?: return null
        if (currentVersion == recommendedVersion) return null
        return "Mod '[modName]' version [modVersion] recommends game version [recommended], you are running [current]"
            .replace("[modName]", "[$name]").replace("[modVersion]", "[$modVersion]")
            .replace("[recommended]", "[$recommended]").replace("[current]", "[$currentGameVersion]")
    }

    /**
     * Returns the dependency declarations that are not satisfied:
     * the dependency mod is not loaded, its version differs from the recommended version,
     * or the recommended version declaration itself is invalid. Empty when no dependencies or all satisfied.
     *
     * @param loadedModVersions map of loaded mod name to its declared [modVersion]
     */
    fun getUnsatisfiedDependencies(loadedModVersions: Map<String, String>): List<ModDependency> {
        if (modDependencies.isEmpty()) return emptyList()
        return modDependencies.filter { dep ->
            val loadedVersion = loadedModVersions[dep.name]
            if (loadedVersion == null) return@filter true // not loaded
            if (dep.recommendedVersion.isBlank()) return@filter false // any version
            val recommended = ModVersion.parse(dep.recommendedVersion)
                ?: return@filter true // invalid declaration -> treat as unsatisfied
            recommended != ModVersion.parseOrDefault(loadedVersion)
        }
    }
    //endregion

    //region IHasUniques
    override var name = "ModOptions"
    override var uniques = ArrayList<String>()

    @delegate:Transient
    override val uniqueObjects: List<Unique> by lazy (::uniqueObjectsProvider)
    @delegate:Transient
    override val uniqueMap: UniqueMap by lazy(::uniqueMapProvider)

    override fun getUniqueTarget() = UniqueTarget.ModOptions
    //endregion
}

/** A dependency mod with an optional recommended version (exact match) */
class ModDependency {
    /** Name of the dependency mod */
    var name = ""
    /** Recommended version of the dependency (exact match, n.n.n); empty = any version */
    var recommendedVersion = ""
}
