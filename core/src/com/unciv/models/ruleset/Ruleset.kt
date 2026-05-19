package com.unciv.models.ruleset

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.json.fromJsonFile
import com.unciv.json.json
import com.unciv.logic.BackwardCompatibility.updateDeprecations
import java.lang.reflect.Modifier
import kotlin.collections.set
import com.unciv.logic.GameInfo
import com.unciv.logic.map.tile.RoadStatus
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.ruleset.nation.CityStateType
import com.unciv.models.ruleset.nation.Difficulty
import com.unciv.models.ruleset.nation.Nation
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.tech.Era
import com.unciv.models.ruleset.tech.TechColumn
import com.unciv.models.ruleset.tech.Technology
import com.unciv.models.ruleset.tile.Terrain
import com.unciv.models.ruleset.tile.TileImprovement
import com.unciv.models.ruleset.tile.TileResource
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.models.ruleset.unique.IHasUniques
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.Promotion
import com.unciv.models.ruleset.unit.UnitNameGroup
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.models.ruleset.validation.RulesetValidator
import com.unciv.models.ruleset.validation.UniqueValidator
import com.unciv.models.stats.GameResource
import com.unciv.models.stats.INamed
import com.unciv.models.stats.Stat
import com.unciv.models.stats.SubStat
import com.unciv.models.translations.tr
import com.unciv.ui.screens.civilopediascreen.ICivilopediaText
import com.unciv.utils.Log
import org.jetbrains.annotations.VisibleForTesting
import yairm210.purity.annotations.Readonly
import kotlin.collections.set

enum class RulesetFile(
    val filename: String,
    @Readonly val getRulesetObjects: Ruleset.() -> Sequence<IRulesetObject> = { emptySequence() },
    @Readonly val getUniques: Ruleset.() -> Sequence<Unique> = { getRulesetObjects().flatMap { it.uniqueObjects } }
) {
    Beliefs("Beliefs.json", { beliefs.values.asSequence() }),
    Buildings("Buildings.json", { buildings.values.asSequence() }),
    Eras("Eras.json", { eras.values.asSequence() }),
    Religions("Religions.json"),
    Nations("Nations.json", { nations.values.asSequence() }),
    Policies("Policies.json", { policies.values.asSequence() }),
    Techs("Techs.json", { technologies.values.asSequence() }),
    Terrains("Terrains.json", { terrains.values.asSequence() }),
    Tutorials("Tutorials.json", { tutorials.values.asSequence() }),
    TileImprovements("TileImprovements.json", { tileImprovements.values.asSequence() }),
    TileResources("TileResources.json", { tileResources.values.asSequence() }),
    Specialists("Specialists.json"),
    Units("Units.json", { units.values.asSequence() }),
    UnitPromotions("UnitPromotions.json", { unitPromotions.values.asSequence() }),
    UnitNameGroup("UnitNameGroups.json", { unitNameGroups.values.asSequence() }),
    UnitTypes("UnitTypes.json", { unitTypes.values.asSequence() }),
    VictoryTypes("VictoryTypes.json"),
    CityStateTypes("CityStateTypes.json", getUniques =
        { cityStateTypes.values.asSequence().flatMap { it.allyBonusUniqueMap.getAllUniques() + it.friendBonusUniqueMap.getAllUniques() } }),
    Personalities("Personalities.json", { personalities.values.asSequence() }),
    Events("Events.json", { events.values.asSequence() + events.values.flatMap { it.choices } }),
    GlobalUniques("GlobalUniques.json", { sequenceOf(globalUniques) }),
    ModOptions("ModOptions.json", getUniques = { modOptions.uniqueObjects.asSequence() }),
    Speeds("Speeds.json", { speeds.values.asSequence() }),
    Difficulties("Difficulties.json"),
    Quests("Quests.json"),
    Ruins("Ruins.json", { ruinRewards.values.asSequence() });
}

class Ruleset {

    /** If (and only if) this Ruleset is a mod, this will be the source folder.
     *  In other words, this is `null` for built-in and combined rulesets.
     */
    var folderLocation: FileHandle? = null

    /** A Ruleset instance can represent a built-in ruleset, a mod or a combined ruleset.
     *
     *  `name` will be the built-in's fullName, the mod's name as displayed (same as folder name),
     *  or in the case of combined rulesets it will be empty.
     *
     *  @see toString
     *  @see BaseRuleset.fullName
     *  @see RulesetCache.getComplexRuleset
     */
    var name = ""

    /** The list of mods that made up this Ruleset, including the base ruleset. */
    val mods = LinkedHashSet<String>()

    //region Json fields
    val beliefs = LinkedHashMap<String, Belief>()
    val buildings = LinkedHashMap<String, Building>()
    val difficulties = LinkedHashMap<String, Difficulty>()
    val eras = LinkedHashMap<String, Era>()
    val speeds = LinkedHashMap<String, Speed>()
    /** Only [Ruleset.load], [GameInfo], [BaseUnit] and [RulesetValidator] should access this directly.
     *  All other uses should call [GameInfo.getGlobalUniques] instead. */
    internal var globalUniques = GlobalUniques()
    val nations = LinkedHashMap<String, Nation>()
    val policies = LinkedHashMap<String, Policy>()
    val policyBranches = LinkedHashMap<String, PolicyBranch>()
    val religions = ArrayList<String>()
    val ruinRewards = LinkedHashMap<String, RuinReward>()
    val quests = LinkedHashMap<String, Quest>()
    val specialists = LinkedHashMap<String, Specialist>()
    val technologies = LinkedHashMap<String, Technology>()
    val techColumns = ArrayList<TechColumn>()
    val terrains = LinkedHashMap<String, Terrain>()
    val tileImprovements = LinkedHashMap<String, TileImprovement>()
    val tileResources = LinkedHashMap<String, TileResource>()
    val tutorials = LinkedHashMap<String, Tutorial>()
    val units = LinkedHashMap<String, BaseUnit>()
    val unitPromotions = LinkedHashMap<String, Promotion>()
    val unitNameGroups = LinkedHashMap<String, UnitNameGroup>()
    val unitTypes = LinkedHashMap<String, UnitType>()
    var victories = LinkedHashMap<String, Victory>()
    var cityStateTypes = LinkedHashMap<String, CityStateType>()
    val personalities = LinkedHashMap<String, Personality>()
    val events = LinkedHashMap<String, Event>()
    var modOptions = ModOptions()
    //endregion

    //region cache fields
    val greatGeneralUnits by lazy {
        units.values.filter { it.hasUnique(UniqueType.GreatPersonFromCombat, GameContext.IgnoreConditionals) }
    }

    val tileRemovals by lazy { tileImprovements.values.filter { it.name.startsWith(Constants.remove) } }
    val nonRoadTileRemovals by lazy { tileRemovals.filter { rulesetImprovement ->
            RoadStatus.entries.toTypedArray().none { it.removeAction == rulesetImprovement.name } } }

    /** Contains all happiness levels that moving *from* them, to one *below* them, can change uniques that apply */
    val allHappinessLevelsThatAffectUniques by lazy {
        sequence {
            for (unique in this@Ruleset.allUniques())
                for (conditional in unique.modifiers){
                    if (conditional.type == UniqueType.ConditionalWhenBelowAmountStatResource
                        && conditional.params[1] == "Happiness") yield(conditional.params[0].toInt())
                    if (conditional.type == UniqueType.ConditionalWhenAboveAmountStatResource
                        && conditional.params[1] == "Happiness") yield(conditional.params[0].toInt())
                    if (conditional.type == UniqueType.ConditionalWhenBetweenStatResource
                        && conditional.params[2] == "Happiness"){
                        yield(conditional.params[0].toInt())
                        yield(conditional.params[1].toInt() + 1)
                    }
                    if (conditional.type == UniqueType.ConditionalHappy) yield(0)
                }
        }.toSet()
    }

    val roadImprovement: TileImprovement? by lazy { RoadStatus.Road.improvement(this) }
    val railroadImprovement: TileImprovement? by lazy { RoadStatus.Railroad.improvement(this) }
    //endregion

    //region MergeAction support
    /** Raw JsonValue arrays for JSON files that contain "_mergeAction", keyed by filename.
     *  Populated during [load] and consumed during [add] for conditional resolution. */
    val rawJsonArrays = HashMap<String, JsonValue>()

    //endregion

    fun clone(): Ruleset {
        val newRuleset = Ruleset()
        newRuleset.add(this)
        // Make sure the clone is recognizable - e.g. startNewGame fallback when a base mod was removed needs this
        newRuleset.name = name
        newRuleset.modOptions.isBaseRuleset = modOptions.isBaseRuleset
        return newRuleset
    }

    fun getGameResource(resourceName: String): GameResource? = Stat.safeValueOf(resourceName)
        ?: SubStat.safeValueOf(resourceName)
        ?: tileResources[resourceName]

    private inline fun <reified T : INamed> createHashmap(items: Array<T>): LinkedHashMap<String, T> {
        val hashMap = LinkedHashMap<String, T>(items.size)
        for (item in items) {
            val itemName = try { item.name }
            catch (_: Exception) {
                throw Exception("${T::class.simpleName} is missing a name!")
            }

            hashMap[itemName] = item
            (item as? IRulesetObject)?.originRuleset = name // RULESET name
        }
        return hashMap
    }

    fun add(ruleset: Ruleset) {
        val baseRulesetName: String = if (ruleset.modOptions.isBaseRuleset) ruleset.name
            else mods.firstOrNull()?.let { RulesetCache[it]?.name } ?: ""
        val mergeContext = MergeContext(
            loadedMods = mods + ruleset.mods + ruleset.name,
            baseRuleset = baseRulesetName,
            currentRuleset = this
        )

        // Helper: resolve merge actions from raw JSON or fall back to putAll
        @Suppress("UNCHECKED_CAST")
        fun <T : IRulesetObject> mergeOrPutAll(
            filename: String,
            targetMap: LinkedHashMap<String, T>,
            sourceMap: LinkedHashMap<String, T>,
            arrayClass: Class<*>
        ) {
            val rawJson = ruleset.rawJsonArrays[filename]
            if (rawJson != null) {
                val resolvedList = resolveConditionals(rawJson, mergeContext)
                if (resolvedList.isNotEmpty()) {
                    val deserialized = deserializeResolvedList<T>(resolvedList, arrayClass)
                    processObjects(targetMap, deserialized, this)
                }
            } else {
                targetMap.putAll(sourceMap)
            }
        }

        mergeOrPutAll("Beliefs.json", beliefs, ruleset.beliefs, Array<Belief>::class.java)
        ruleset.modOptions.beliefsToRemove
            .flatMap { beliefsToRemove ->
                beliefs.filter { it.value.matchesFilter(beliefsToRemove) }.keys
            }.toSet().forEach {
                beliefs.remove(it)
            }

        ruleset.modOptions.buildingsToRemove
            .flatMap { buildingToRemove ->
                buildings.filter { it.value.matchesFilter(buildingToRemove) }.keys
            }.toSet().forEach {
                buildings.remove(it)
            }
        mergeOrPutAll("Buildings.json", buildings, ruleset.buildings, Array<Building>::class.java)

        mergeOrPutAll("Difficulties.json", difficulties, ruleset.difficulties, Array<Difficulty>::class.java)
        mergeOrPutAll("Eras.json", eras, ruleset.eras, Array<Era>::class.java)
        mergeOrPutAll("Speeds.json", speeds, ruleset.speeds, Array<Speed>::class.java)

        globalUniques = GlobalUniques.combine(globalUniques, ruleset.globalUniques)

        ruleset.modOptions.nationsToRemove
            .flatMap { nationToRemove ->
                nations.filter { it.value.matchesFilter(nationToRemove) }.keys
            }.toSet().forEach {
                nations.remove(it)
            }
        mergeOrPutAll("Nations.json", nations, ruleset.nations, Array<Nation>::class.java)

        /** We must remove all policies from a policy branch otherwise we have policies that cannot be picked
         *  but are still considered "available" */
        fun removePolicyBranch(policyBranch: PolicyBranch){
            policyBranches.remove(policyBranch.name)
            for (policy in policyBranch.policies)
                policies.remove(policy.name)
        }

        ruleset.modOptions.policyBranchesToRemove
            .flatMap { policyBranchToRemove ->
                policyBranches.filter { it.value.matchesFilter(policyBranchToRemove) }.values
            }.toSet().forEach {
                removePolicyBranch(it)
            }

        val overriddenPolicyBranches = policyBranches
            .filter { it.key in ruleset.policyBranches }.map { it.value }
        for (policyBranch in overriddenPolicyBranches) removePolicyBranch(policyBranch)

        mergeOrPutAll("Policies.json", policyBranches, ruleset.policyBranches, Array<PolicyBranch>::class.java)
        policies.putAll(ruleset.policies)

        // Remove the policies
        ruleset.modOptions.policiesToRemove
            .flatMap { policyToRemove ->
                policies.filter { it.value.matchesFilter(policyToRemove) }.keys
            }.toSet().forEach {
                policies.remove(it)
            }

        // Remove the policies if they exist in the policy branches too
        for (policyToRemove in ruleset.modOptions.policiesToRemove) {
            for (branch in policyBranches.values) {
                branch.policies.removeAll { it.matchesFilter(policyToRemove) }
            }
        }

        quests.putAll(ruleset.quests)

        // Remove associated Religions, including when they're favored by Nations
        religions.addAll(ruleset.religions)
        religions.removeAll(ruleset.modOptions.religionsToRemove)
        nations.filter { it.value.favoredReligion in ruleset.modOptions.religionsToRemove }
            .forEach { it.value.favoredReligion = null }

        mergeOrPutAll("Ruins.json", ruinRewards, ruleset.ruinRewards, Array<RuinReward>::class.java)
        specialists.putAll(ruleset.specialists)

        ruleset.modOptions.techsToRemove
            .flatMap { techToRemove ->
                technologies.filter { it.value.matchesFilter(techToRemove) }.keys
            }.toSet().forEach {
                technologies.remove(it)
            }
        technologies.putAll(ruleset.technologies)
        techColumns.addAll(ruleset.techColumns)

        mergeOrPutAll("Terrains.json", terrains, ruleset.terrains, Array<Terrain>::class.java)
        mergeOrPutAll("TileImprovements.json", tileImprovements, ruleset.tileImprovements, Array<TileImprovement>::class.java)
        mergeOrPutAll("TileResources.json", tileResources, ruleset.tileResources, Array<TileResource>::class.java)
        mergeOrPutAll("Tutorials.json", tutorials, ruleset.tutorials, Array<Tutorial>::class.java)
        mergeOrPutAll("UnitTypes.json", unitTypes, ruleset.unitTypes, Array<UnitType>::class.java)
        victories.putAll(ruleset.victories)
        cityStateTypes.putAll(ruleset.cityStateTypes)

        ruleset.modOptions.unitsToRemove
            .flatMap { unitToRemove ->
                units.filter { it.apply { value.setRuleset(this@Ruleset) }.value.matchesFilter(unitToRemove) }.keys
            }.toSet().forEach {
                units.remove(it)
            }
        mergeOrPutAll("Units.json", units, ruleset.units, Array<BaseUnit>::class.java)

        mergeOrPutAll("Personalities.json", personalities, ruleset.personalities, Array<Personality>::class.java)
        mergeOrPutAll("Events.json", events, ruleset.events, Array<Event>::class.java)

        modOptions.uniques.addAll(ruleset.modOptions.uniques)
        modOptions.constants.merge(ruleset.modOptions.constants)

        mergeOrPutAll("UnitPromotions.json", unitPromotions, ruleset.unitPromotions, Array<Promotion>::class.java)
        mergeOrPutAll("UnitNameGroups.json", unitNameGroups, ruleset.unitNameGroups, Array<UnitNameGroup>::class.java)

        mods += ruleset.mods
    }

    //region MergeAction processing

    /** Recursively resolve control blocks and filter conditionally-skipped objects at the JsonValue level.
     *  Returns a flat list of JsonValue objects ready for deserialization. */
    fun resolveConditionals(
        jsonArray: JsonValue,
        context: MergeContext
    ): List<JsonValue> {
        val result = mutableListOf<JsonValue>()

        for (element in jsonArray) {
            val mergeAction = element.get("_mergeAction")

            if (mergeAction != null && mergeAction.has("then")) {
                // Control block — evaluate condition and expand the chosen branch
                val conditionNode = mergeAction.get("if")
                val pickThen = conditionNode != null && evaluateCondition(conditionNode, context)
                val branch = if (pickThen) mergeAction.get("then") else mergeAction.get("else")
                if (branch != null)
                    result.addAll(resolveConditionals(branch, context))
            } else {
                // Object-level condition — skip if condition fails
                val conditionNode = mergeAction?.get("if")
                if (conditionNode != null && !evaluateCondition(conditionNode, context))
                    continue
                result.add(element)
            }
        }

        return result
    }

    /** Evaluate a single condition node (JsonValue) against the given [MergeContext]. */
    fun evaluateCondition(conditionNode: JsonValue, context: MergeContext): Boolean {
        // Combinators
        if (conditionNode.has("not"))
            return !evaluateCondition(conditionNode.get("not"), context)
        if (conditionNode.has("and")) {
            val children = conditionNode.get("and")
            for (child in children)
                if (!evaluateCondition(child, context)) return false
            return true
        }
        if (conditionNode.has("or")) {
            val children = conditionNode.get("or")
            for (child in children)
                if (evaluateCondition(child, context)) return true
            return false
        }

        // Environment conditions
        if (conditionNode.has("mod_loaded"))
            return context.loadedMods.contains(conditionNode.getString("mod_loaded"))
        if (conditionNode.has("mod_author")) {
            val author = conditionNode.getString("mod_author")
            return context.loadedMods.any { modName ->
                RulesetCache[modName]?.modOptions?.author == author
            }
        }
        if (conditionNode.has("base_ruleset"))
            return conditionNode.getString("base_ruleset") == context.baseRuleset
        if (conditionNode.has("game_version"))
            return evaluateGameVersion(conditionNode.getString("game_version"))

        // Object conditions
        if (conditionNode.has("object_exists")) {
            val spec = conditionNode.getString("object_exists")
            val parts = spec.split(":", limit = 2)
            if (parts.size != 2) return false
            return context.currentRuleset.objectExists(parts[0], parts[1])
        }
        if (conditionNode.has("object_count")) {
            val check = conditionNode.get("object_count")
            val typeName = check.getString("type")
            val count = context.currentRuleset.countObjectsOfType(typeName)
            return evaluateCountCheck(check, count)
        }
        if (conditionNode.has("object_has_unique")) {
            val spec = conditionNode.getString("object_has_unique")
            val parts = spec.split(":", limit = 3)
            if (parts.size != 3) return false
            return context.currentRuleset.objectHasUnique(parts[0], parts[1], parts[2])
        }
        if (conditionNode.has("any_object_has_unique")) {
            val uniqueText = conditionNode.getString("any_object_has_unique")
            return context.currentRuleset.anyObjectHasUnique(uniqueText)
        }
        if (conditionNode.has("object_has_field")) {
            val check = conditionNode.get("object_has_field")
            return context.currentRuleset.objectHasField(check)
        }
        if (conditionNode.has("object_field_equals")) {
            val check = conditionNode.get("object_field_equals")
            return context.currentRuleset.objectFieldEquals(check)
        }
        if (conditionNode.has("object_field_contains")) {
            val check = conditionNode.get("object_field_contains")
            return context.currentRuleset.objectFieldContains(check)
        }
        if (conditionNode.has("object_field_compare")) {
            val check = conditionNode.get("object_field_compare")
            return context.currentRuleset.objectFieldCompare(check)
        }

        return true // Empty condition = always pass
    }

    private fun evaluateGameVersion(versionSpec: String): Boolean {
        val currentVersion = UncivGame.VERSION.text
        val op: String
        val version: String
        when {
            versionSpec.startsWith(">=") -> { op = ">="; version = versionSpec.removePrefix(">=").trim() }
            versionSpec.startsWith("<=") -> { op = "<="; version = versionSpec.removePrefix("<=").trim() }
            versionSpec.startsWith(">") -> { op = ">"; version = versionSpec.removePrefix(">").trim() }
            versionSpec.startsWith("<") -> { op = "<"; version = versionSpec.removePrefix("<").trim() }
            else -> { op = "=="; version = versionSpec.trim() }
        }
        val comparison = compareVersions(currentVersion, version)
        return when (op) {
            ">=" -> comparison >= 0
            "<=" -> comparison <= 0
            ">" -> comparison > 0
            "<" -> comparison < 0
            else -> comparison == 0
        }
    }

    private fun compareVersions(a: String, b: String): Int {
        val aParts = a.split(".").map { it.toIntOrNull() ?: 0 }
        val bParts = b.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(aParts.size, bParts.size)
        for (i in 0 until maxLen) {
            val aVal = aParts.getOrElse(i) { 0 }
            val bVal = bParts.getOrElse(i) { 0 }
            if (aVal != bVal) return aVal.compareTo(bVal)
        }
        return 0
    }

    private fun evaluateCountCheck(check: JsonValue, count: Int): Boolean {
        if (check.has("greater_than") && count <= check.get("greater_than").asInt()) return false
        if (check.has("greater_than_or_equal") && count < check.get("greater_than_or_equal").asInt()) return false
        if (check.has("less_than") && count >= check.get("less_than").asInt()) return false
        if (check.has("less_than_or_equal") && count > check.get("less_than_or_equal").asInt()) return false
        if (check.has("equal") && count != check.get("equal").asInt()) return false
        return true
    }

    /** Check if an object of the given type and name exists in the ruleset. */
    fun objectExists(type: String, name: String): Boolean {
        return getObjectMap(type)?.containsKey(name) ?: false
    }

    /** Count objects of the given type. */
    fun countObjectsOfType(type: String): Int {
        return getObjectMap(type)?.size ?: 0
    }

    /** Check if a specific object has a specific unique string. */
    fun objectHasUnique(type: String, name: String, uniqueText: String): Boolean {
        val obj = getObjectMap(type)?.get(name) as? IHasUniques ?: return false
        return obj.uniques.any { it == uniqueText }
    }

    /** Check if any object in the ruleset has the given unique string. */
    fun anyObjectHasUnique(uniqueText: String): Boolean {
        for (file in RulesetFile.entries) {
            val map = file.getRulesetObjects(this)
            if (map.any { it.uniques.any { u -> u == uniqueText } }) return true
        }
        return false
    }

    private fun objectHasField(check: JsonValue): Boolean {
        val spec = check.getString("object")
        val fieldName = check.getString("field")
        val parts = spec.split(":", limit = 2)
        if (parts.size != 2) return false
        val obj = getObjectMap(parts[0])?.get(parts[1]) ?: return false
        return isFieldSet(obj, fieldName)
    }

    private fun objectFieldEquals(check: JsonValue): Boolean {
        val spec = check.getString("object")
        val fieldName = check.getString("field")
        val parts = spec.split(":", limit = 2)
        if (parts.size != 2) return false
        val obj = getObjectMap(parts[0])?.get(parts[1]) ?: return false
        val expectedValue = check.get("equals")
        if (expectedValue == null) return !isFieldSet(obj, fieldName)
        val actualValue = getFieldValue(obj, fieldName)
        return jsonValueEquals(expectedValue, actualValue)
    }

    private fun objectFieldContains(check: JsonValue): Boolean {
        val spec = check.getString("object")
        val fieldName = check.getString("field")
        val parts = spec.split(":", limit = 2)
        if (parts.size != 2) return false
        val obj = getObjectMap(parts[0])?.get(parts[1]) ?: return false
        val searchValue = check.get("value") ?: return false
        val fieldValue = getFieldValue(obj, fieldName)
        if (fieldValue is Collection<*>) {
            return fieldValue.any { element -> jsonValueMatches(searchValue, element) }
        }
        val fieldStr = fieldValue?.toString() ?: ""
        return fieldStr.contains(searchValue.asString())
    }

    private fun objectFieldCompare(check: JsonValue): Boolean {
        val spec = check.getString("object")
        val fieldName = check.getString("field")
        val parts = spec.split(":", limit = 2)
        if (parts.size != 2) return false
        val obj = getObjectMap(parts[0])?.get(parts[1]) ?: return false
        val fieldValue = getFieldValue(obj, fieldName)
        val numValue = when (fieldValue) {
            is Number -> fieldValue.toDouble()
            else -> return false
        }
        return evaluateCompareCheck(check, numValue)
    }

    private fun evaluateCompareCheck(check: JsonValue, value: Double): Boolean {
        if (check.has("greater_than") && value <= check.getDouble("greater_than")) return false
        if (check.has("greater_than_or_equal") && value < check.getDouble("greater_than_or_equal")) return false
        if (check.has("less_than") && value >= check.getDouble("less_than")) return false
        if (check.has("less_than_or_equal") && value > check.getDouble("less_than_or_equal")) return false
        if (check.has("equal") && value != check.getDouble("equal")) return false
        return true
    }

    /** Get the ruleset object map by type name (e.g., "Building" → buildings, "Unit" → units). */
    private fun getObjectMap(type: String): LinkedHashMap<String, *>? {
        return when (type) {
            "Building" -> buildings
            "Unit" -> units
            "Nation" -> nations
            "Technology" -> technologies
            "Policy" -> policies
            "Belief" -> beliefs
            "Terrain" -> terrains
            "TileImprovement" -> tileImprovements
            "TileResource" -> tileResources
            "Promotion" -> unitPromotions
            "Era" -> eras
            "Quest" -> quests
            "RuinReward" -> ruinRewards
            "Specialist" -> specialists
            "Tutorial" -> tutorials
            "Difficulty" -> difficulties
            "UnitType" -> unitTypes
            "Victory" -> victories
            "CityStateType" -> cityStateTypes
            "Personality" -> personalities
            "Event" -> events
            "Speed" -> speeds
            "UnitNameGroup" -> unitNameGroups
            else -> null
        }
    }

    private fun isFieldSet(obj: Any, fieldName: String): Boolean {
        val value = getFieldValue(obj, fieldName) ?: return false
        return when (value) {
            is String -> value.isNotEmpty()
            is Number -> value.toDouble() != 0.0
            is Boolean -> value
            is Collection<*> -> value.isNotEmpty()
            else -> true
        }
    }

    private fun getFieldValue(obj: Any, fieldName: String): Any? {
        return try {
            val field = obj::class.java.declaredFields.firstOrNull { it.name == fieldName }
            field?.apply { isAccessible = true }?.get(obj)
        } catch (_: Exception) {
            null
        }
    }

    private fun jsonValueEquals(jsonValue: JsonValue, kotlinValue: Any?): Boolean {
        if (kotlinValue == null) return jsonValue.isNull
        val jvStr = if (jsonValue.isString) jsonValue.asString() else jsonValue.toString()
        return jvStr == kotlinValue.toString()
    }

    private fun jsonValueMatches(jsonValue: JsonValue, kotlinValue: Any?): Boolean {
        if (kotlinValue == null) return false
        val jvStr = if (jsonValue.isString) jsonValue.asString() else jsonValue.toString()
        return jvStr == kotlinValue.toString()
    }

    /** Deserialize a resolved JsonValue list to typed objects. */
    @Suppress("UNCHECKED_CAST")
    fun <T : IRulesetObject> deserializeResolvedList(
        resolvedList: List<JsonValue>,
        arrayClass: Class<*>
    ): List<T> {
        val jsonText = "[" + resolvedList.joinToString(",") { it.toJson(JsonWriter.OutputType.json) } + "]"
        val array = json().fromJson(arrayClass, jsonText) as Array<T>
        for (item in array)
            (item).originRuleset = name
        return array.toList()
    }

    /** Process a list of objects against the target map, dispatching based on each object's _mergeAction. */
    fun <T : IRulesetObject> processObjects(
        target: LinkedHashMap<String, T>,
        sourceList: List<T>,
        ruleset: Ruleset
    ) {
        for (obj in sourceList) {
            val existing = target[obj.name]
            val action = obj._mergeAction?.action

            when (action) {
                "TRY_INJECT" -> {
                    if (existing != null) existing.mergeFields(obj)
                }
                "CREATE_OR_REPLACE" -> {
                    target[obj.name] = obj
                }
                "REMOVE" -> {
                    target.remove(obj.name)
                }
                "REMOVE_FIELD" -> {
                    existing?.removeFields(obj)
                }
                null -> {
                    target[obj.name] = obj
                }
            }

            // Clear _mergeAction after processing (not persisted to saves)
            obj._mergeAction = null
        }
    }

    //endregion

    //region MergeField extension methods

    private fun Any.isDefaultForField(): Boolean {
        return when (this) {
            is String -> isEmpty()
            is Int -> this == 0 || this == -1  // -1 is the common "unset" sentinel in Unciv
            is Long -> this == 0L || this == -1L
            is Number -> toDouble() == 0.0
            is Boolean -> !this
            is Collection<*> -> isEmpty()
            else -> false
        }
    }

    /** Append non-default fields from [source] into this object.
     *  Scalar fields are overwritten if source has a non-default value.
     *  Collection fields are appended (source elements added to target). */
    fun IRulesetObject.mergeFields(source: IRulesetObject) {
        val processed = HashSet<String>()
        var clazz: Class<*> = this::class.java
        while (clazz != Any::class.java) {
            for (field in clazz.declaredFields) {
                if (field.modifiers and Modifier.STATIC != 0) continue
                if (field.name in processed) continue
                processed.add(field.name)

                // Never touch merge metadata, identity fields, or lazy delegates
                if (field.name == "_mergeAction" || field.name == "name" || field.name == "originRuleset") continue
                if (field.name.endsWith("\$delegate")) continue

                field.isAccessible = true
                val sourceValue = field.get(source) ?: continue

                if (sourceValue is Collection<*>) {
                    if (sourceValue.isEmpty()) continue
                    @Suppress("UNCHECKED_CAST")
                    val targetCollection = field.get(this) as? MutableCollection<Any?> ?: continue
                    // Deduplicate: don't add elements already present in the target
                    val newElements = sourceValue.filter { it !in targetCollection }
                    if (newElements.isEmpty()) continue
                    targetCollection.addAll(newElements)
                } else {
                    if (sourceValue.isDefaultForField()) continue
                    field.set(this, sourceValue)
                }
            }
            clazz = clazz.superclass
        }
    }

    /** Remove fields or array elements specified in [source].
     *  For scalar fields: reset to default value.
     *  For collection fields: remove matching elements (supports "*" wildcard at end of string). */
    fun IRulesetObject.removeFields(source: IRulesetObject) {
        val processed = HashSet<String>()
        var clazz: Class<*> = this::class.java
        while (clazz != Any::class.java) {
            for (field in clazz.declaredFields) {
                if (field.modifiers and Modifier.STATIC != 0) continue
                if (field.name in processed) continue
                processed.add(field.name)
                if (field.name == "_mergeAction" || field.name == "name" || field.name == "originRuleset") continue
                if (field.name.endsWith("\$delegate")) continue
                field.isAccessible = true

                val sourceValue = field.get(source) ?: continue

                if (sourceValue.isDefaultForField()) continue

                if (sourceValue is Collection<*>) {
                    if (sourceValue.isEmpty()) continue
                    val targetValue = field.get(this)
                    @Suppress("UNCHECKED_CAST")
                    (targetValue as? MutableCollection<Any?>)?.let { target ->
                        for (item in sourceValue) {
                            if (item is String && item.endsWith("*")) {
                                val prefix = item.removeSuffix("*")
                                target.removeAll {
                                    it is String && it.startsWith(prefix)
                                }
                            } else {
                                target.remove(item)
                            }
                        }
                    }
                } else {
                    // Reset scalar field to default
                    val defaultValue = when (sourceValue) {
                        is String -> ""
                        is Number -> if (sourceValue is Double || sourceValue is Float) 0.0 else 0
                        is Boolean -> false
                        else -> null
                    }
                    if (defaultValue != null)
                        field.set(this, defaultValue)
                }
            }
            clazz = clazz.superclass
        }
    }

    //endregion

    fun clear() {
        beliefs.clear()
        buildings.clear()
        difficulties.clear()
        eras.clear()
        speeds.clear()
        globalUniques = GlobalUniques()
        mods.clear()
        nations.clear()
        policies.clear()
        policyBranches.clear()
        quests.clear()
        religions.clear()
        ruinRewards.clear()
        specialists.clear()
        technologies.clear()
        techColumns.clear()
        terrains.clear()
        tileImprovements.clear()
        tileResources.clear()
        tutorials.clear()
        unitPromotions.clear()
        unitNameGroups.clear()
        units.clear()
        unitTypes.clear()
        victories.clear()
        cityStateTypes.clear()
        personalities.clear()
        events.clear()
    }

    @Readonly
    fun allRulesetObjects(): Sequence<IRulesetObject> = RulesetFile.entries.asSequence().flatMap { it.getRulesetObjects(this) }
    @Readonly
    fun allUniques(): Sequence<Unique> = RulesetFile.entries.asSequence().flatMap { it.getUniques(this) }
    @Readonly fun allICivilopediaText(): Sequence<ICivilopediaText> = allRulesetObjects() + events.values.flatMap { it.choices }

    /** Check whether a JSON file contains "_mergeAction" and if so, store the parsed
     *  [JsonValue] tree for later conditional resolution during [add]. */
    private fun checkForMergeActions(file: FileHandle): Boolean {
        if (!file.exists()) return false
        val jsonText = file.readString(Charsets.UTF_8.name())
        if ("_mergeAction" in jsonText) {
            rawJsonArrays[file.name()] = JsonReader().parse(jsonText)
            return true
        }
        return false
    }

    fun load(folderHandle: FileHandle) {
        fun RulesetFile.file() = folderHandle.child(filename)

        // Note: Most files are loaded using createHashmap, which sets originRuleset automatically.
        // For other files containing IRulesetObject's we'll have to remember to do so manually - e.g. Tech.
        val modOptionsFile = RulesetFile.ModOptions.file()
        if (modOptionsFile.exists()) {
            try {
                modOptions = json().fromJsonFile(ModOptions::class.java, modOptionsFile)
                modOptions.updateDeprecations()
            } catch (ex: Exception) {
                Log.error("Failed to get modOptions from json file", ex)
            }
        }

        val techFile = RulesetFile.Techs.file()
        checkForMergeActions(techFile)
        if (techFile.exists()) {
            val techColumns = json().fromJsonFile(Array<TechColumn>::class.java, techFile)
            for (techColumn in techColumns) {
                this.techColumns.add(techColumn)
                for (tech in techColumn.techs) {
                    if (tech.cost == 0) tech.cost = techColumn.techCost
                    tech.column = techColumn
                    tech.originRuleset = name
                    technologies[tech.name] = tech
                }
            }
        }

        val buildingsFile = RulesetFile.Buildings.file()
        checkForMergeActions(buildingsFile)
        if (buildingsFile.exists()) buildings += createHashmap(json().fromJsonFile(Array<Building>::class.java, buildingsFile))

        val terrainsFile = RulesetFile.Terrains.file()
        checkForMergeActions(terrainsFile)
        if (terrainsFile.exists()) {
            terrains += createHashmap(json().fromJsonFile(Array<Terrain>::class.java, terrainsFile))
            for (terrain in terrains.values) {
                terrain.originRuleset = name
                terrain.setTransients()
            }
        }

        val resourcesFile = RulesetFile.TileResources.file()
        checkForMergeActions(resourcesFile)
        if (resourcesFile.exists()) tileResources += createHashmap(json().fromJsonFile(Array<TileResource>::class.java, resourcesFile))

        val improvementsFile = RulesetFile.TileImprovements.file()
        checkForMergeActions(improvementsFile)
        if (improvementsFile.exists()) tileImprovements += createHashmap(json().fromJsonFile(Array<TileImprovement>::class.java, improvementsFile))

        val erasFile = RulesetFile.Eras.file()
        checkForMergeActions(erasFile)
        if (erasFile.exists()) eras += createHashmap(json().fromJsonFile(Array<Era>::class.java, erasFile))
        // While `eras.values.toList()` might seem more logical, eras.values is a MutableCollection and
        // therefore does not guarantee keeping the order of elements like a LinkedHashMap does.
        // Using map{} sidesteps this problem
        eras.map { it.value }.withIndex().forEach { it.value.eraNumber = it.index }

        val speedsFile = RulesetFile.Speeds.file()
        checkForMergeActions(speedsFile)
        if (speedsFile.exists()) {
            speeds += createHashmap(json().fromJsonFile(Array<Speed>::class.java, speedsFile))
        }

        val unitTypesFile = RulesetFile.UnitTypes.file()
        checkForMergeActions(unitTypesFile)
        if (unitTypesFile.exists()) unitTypes += createHashmap(json().fromJsonFile(Array<UnitType>::class.java, unitTypesFile))

        val unitsFile = RulesetFile.Units.file()
        checkForMergeActions(unitsFile)
        if (unitsFile.exists()) units += createHashmap(json().fromJsonFile(Array<BaseUnit>::class.java, unitsFile))

        val promotionsFile = RulesetFile.UnitPromotions.file()
        checkForMergeActions(promotionsFile)
        if (promotionsFile.exists()) unitPromotions += createHashmap(json().fromJsonFile(Array<Promotion>::class.java, promotionsFile))

        val unitNameGroupsFile = RulesetFile.UnitNameGroup.file()
        checkForMergeActions(unitNameGroupsFile)
        if (unitNameGroupsFile.exists()) unitNameGroups += createHashmap(json().fromJsonFile(Array<UnitNameGroup>::class.java, unitNameGroupsFile))

        val questsFile = RulesetFile.Quests.file()
        checkForMergeActions(questsFile)
        if (questsFile.exists()) quests += createHashmap(json().fromJsonFile(Array<Quest>::class.java, questsFile))

        val specialistsFile = RulesetFile.Specialists.file()
        checkForMergeActions(specialistsFile)
        if (specialistsFile.exists()) specialists += createHashmap(json().fromJsonFile(Array<Specialist>::class.java, specialistsFile))

        val policiesFile = RulesetFile.Policies.file()
        checkForMergeActions(policiesFile)
        if (policiesFile.exists()) {
            policyBranches += createHashmap(
                json().fromJsonFile(Array<PolicyBranch>::class.java, policiesFile)
            )
            for (branch in policyBranches.values) {
                // Setup this branch
                branch.requires = ArrayList()
                branch.branch = branch
                for (victoryType in victories.values) {
                    if (victoryType.name !in branch.priorities.keys) {
                        branch.priorities[victoryType.name] = 0
                    }
                }
                policies[branch.name] = branch

                // Append child policies of this branch
                for (policy in branch.policies) {
                    policy.branch = branch
                    policy.originRuleset = name
                    if (policy.requires == null) {
                        policy.requires = arrayListOf(branch.name)
                    }

                    if (policy != branch.policies.last()) {
                        // If mods override a previous policy's location, we don't want that policy to stick around,
                        // because it leads to softlocks on the policy picker screen
                        val conflictingLocationPolicy = policies.values.firstOrNull {
                            it.branch.name == policy.branch.name
                                && it.column == policy.column
                                && it.row == policy.row
                        }
                        if (conflictingLocationPolicy != null)
                            policies.remove(conflictingLocationPolicy.name)
                    }
                    policies[policy.name] = policy

                }

                // Add a finisher
                branch.policies.last().name =
                    branch.name + Policy.branchCompleteSuffix
            }
        }

        val beliefsFile = RulesetFile.Beliefs.file()
        checkForMergeActions(beliefsFile)
        if (beliefsFile.exists())
            beliefs += createHashmap(json().fromJsonFile(Array<Belief>::class.java, beliefsFile))

        val religionsFile = RulesetFile.Religions.file()
        if (religionsFile.exists())
            religions += json().fromJsonFile(Array<String>::class.java, religionsFile).toList()

        val ruinRewardsFile = RulesetFile.Ruins.file()
        checkForMergeActions(ruinRewardsFile)
        if (ruinRewardsFile.exists())
            ruinRewards += createHashmap(json().fromJsonFile(Array<RuinReward>::class.java, ruinRewardsFile))

        val nationsFile = RulesetFile.Nations.file()
        checkForMergeActions(nationsFile)
        if (nationsFile.exists()) {
            nations += createHashmap(json().fromJsonFile(Array<Nation>::class.java, nationsFile))
            for (nation in nations.values) nation.setTransients()
        }

        val difficultiesFile = RulesetFile.Difficulties.file()
        checkForMergeActions(difficultiesFile)
        if (difficultiesFile.exists())
            difficulties += createHashmap(json().fromJsonFile(Array<Difficulty>::class.java, difficultiesFile))

        val globalUniquesFile = RulesetFile.GlobalUniques.file()
        checkForMergeActions(globalUniquesFile)
        if (globalUniquesFile.exists()) {
            globalUniques = json().fromJsonFile(GlobalUniques::class.java, globalUniquesFile)
            globalUniques.originRuleset = name
        }

        val victoryTypesFile = RulesetFile.VictoryTypes.file()
        checkForMergeActions(victoryTypesFile)
        if (victoryTypesFile.exists()) {
            victories += createHashmap(json().fromJsonFile(Array<Victory>::class.java, victoryTypesFile))
        }

        val cityStateTypesFile = RulesetFile.CityStateTypes.file()
        checkForMergeActions(cityStateTypesFile)
        if (cityStateTypesFile.exists()) {
            cityStateTypes += createHashmap(json().fromJsonFile(Array<CityStateType>::class.java, cityStateTypesFile))
        }

        val personalitiesFile = RulesetFile.Personalities.file()
        checkForMergeActions(personalitiesFile)
        if (personalitiesFile.exists()) {
            personalities += createHashmap(json().fromJsonFile(Array<Personality>::class.java, personalitiesFile))
        }

        val eventsFile = RulesetFile.Events.file()
        checkForMergeActions(eventsFile)
        if (eventsFile.exists()) {
            events += createHashmap(json().fromJsonFile(Array<Event>::class.java, eventsFile))
        }

        // Tutorials exist per builtin ruleset or mod, but there's also a global file that's always loaded
        // Note we can't rely on UncivGame.Current here, so we do the same thing getBuiltinRulesetFileHandle in RulesetCache does
        if (Gdx.files != null) { // we're not running console mode
            val globalTutorialsFile = Gdx.files.internal("jsons").child(RulesetFile.Tutorials.filename)
            if (globalTutorialsFile.exists())
                tutorials += createHashmap(json().fromJsonFile(Array<Tutorial>::class.java, globalTutorialsFile))
        }

        val tutorialsFile = RulesetFile.Tutorials.file()
        checkForMergeActions(tutorialsFile)
        if (tutorialsFile.exists())
            tutorials += createHashmap(json().fromJsonFile(Array<Tutorial>::class.java, tutorialsFile))

        // Add objects that might not be present in base ruleset mods, but are required
        if (modOptions.isBaseRuleset) {
            val fallbackRuleset by lazy { RulesetCache.getVanillaRuleset() } // clone at most once
            // This one should be temporary
            if (unitTypes.isEmpty()) {
                unitTypes.putAll(fallbackRuleset.unitTypes)
            }

            // These should be permanent
            if (!ruinRewardsFile.exists())
                ruinRewards.putAll(fallbackRuleset.ruinRewards)

            if (!globalUniquesFile.exists()) {
                globalUniques = fallbackRuleset.globalUniques
            }
            // If we have no victories, add all the default victories
            if (victories.isEmpty()) victories.putAll(fallbackRuleset.victories)

            if (speeds.isEmpty()) speeds.putAll(fallbackRuleset.speeds)
            if (difficulties.isEmpty()) difficulties.putAll(fallbackRuleset.difficulties)

            if (cityStateTypes.isEmpty())
                for (cityStateType in fallbackRuleset.cityStateTypes.values)
                    cityStateTypes[cityStateType.name] = CityStateType().apply {
                        name = cityStateType.name
                        color = cityStateType.color
                        friendBonusUniques = ArrayList(cityStateType.friendBonusUniques.filter {
                            UniqueValidator(this@Ruleset).checkUnique(
                                Unique(it),
                                false,
                                null
                            ).isEmpty()
                        })
                        allyBonusUniques = ArrayList(cityStateType.allyBonusUniques.filter {
                            UniqueValidator(this@Ruleset).checkUnique(
                                Unique(it),
                                false,
                                null
                            ).isEmpty()
                        })
                    }

            updateResourceTransients()
        }
    }

    /** Building costs are unique in that they are dependant on info in the technology part.
     *  This means that if you add a building in a mod, you want it to depend on the original tech values.
     *  Alternatively, if you edit a tech column's building costs, you want it to affect all buildings in that column.
     *  This deals with that
     *  */
    internal fun updateBuildingCosts() {
        for (building in buildings.values) {
            if (building.cost != -1) continue
            if (building.getMatchingUniques(UniqueType.Unbuildable).any { it.modifiers.isEmpty() }) continue
            val column = building.techColumn(this) ?: continue
            building.cost = if (building.isAnyWonder()) column.wonderCost else column.buildingCost
        }
    }

    /** Introduced to support UniqueType.ImprovesResources: gives a resource the chance to scan improvements */
    internal fun updateResourceTransients() {
        for (resource in tileResources.values)
            resource.setTransients(this)
    }

    @VisibleForTesting
    /** For use by class TestGame. Use only before triggering the globalUniques.uniqueObjects lazy. */
    fun addGlobalUniques(vararg uniques: String) {
        globalUniques.uniques.addAll(uniques)
    }

    /** Used for displaying a RuleSet's name */
    override fun toString() = when {
        name.isNotEmpty() -> name
        mods.size == 1 && RulesetCache[mods.first()]!!.modOptions.isBaseRuleset -> mods.first()
        else -> "Combined RuleSet ($mods)"
    }

    @Readonly
    fun getSummary(): String {
        val stringList = ArrayList<String>()
        if (modOptions.isBaseRuleset) stringList += "Base Ruleset"
        if (technologies.isNotEmpty()) stringList += "[${technologies.size}] Techs"
        if (nations.isNotEmpty()) stringList += "[${nations.size}] Nations"
        if (units.isNotEmpty()) stringList += "[${units.size}] Units"
        if (buildings.isNotEmpty()) stringList += "[${buildings.size}] Buildings"
        if (tileResources.isNotEmpty()) stringList += "[${tileResources.size}] Resources"
        if (tileImprovements.isNotEmpty()) stringList += "[${tileImprovements.size}] Improvements"
        if (religions.isNotEmpty()) stringList += "[${religions.size}] Religions"
        if (beliefs.isNotEmpty()) stringList += "[${beliefs.size}] Beliefs"
        return stringList.joinToString { it.tr() }
    }

    fun getErrorList(tryFixUnknownUniques: Boolean = false) = RulesetValidator.create(this, tryFixUnknownUniques).getErrorList()
}
