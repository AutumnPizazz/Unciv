package com.unciv.models.ruleset

import com.unciv.logic.city.City
import com.unciv.logic.city.CityConstructions
import com.unciv.models.Counter
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.stats.Stat
import com.unciv.ui.components.fonts.Fonts
import yairm210.purity.annotations.Readonly
import kotlin.math.roundToInt

sealed class PerpetualConstruction(override var name: String, val description: String) : IConstruction {
    @Readonly
    abstract fun getProductionTooltip(city: City, withIcon: Boolean = false): String

    /** The displayed production conversion denominator. Non-conversion perpetuals return 0. */
    @Readonly
    open fun getConversionRate(city: City): Int = 0

    abstract class StatConversion(
        val stat: Stat
    ) : PerpetualConstruction(stat.name, "Convert production to [${stat.name}] at a rate of [rate] to 1") {
        override fun getProductionTooltip(city: City, withIcon: Boolean): String
            = "\r\n${(city.cityStats.currentCityStats.production / getConversionRate(city)).roundToInt()}${if (withIcon) stat.character else ""}/${Fonts.turn}"

        @Readonly
        override fun getConversionRate(city: City): Int = (1 / city.cityStats.getStatConversionRate(stat)).roundToInt()

        override fun isBuildable(cityConstructions: CityConstructions): Boolean {
            val city = cityConstructions.city
            if (stat == Stat.Faith && !city.civ.gameInfo.isReligionEnabled())
                return false

            val context = city.state
            @Suppress("DEPRECATION") // forEachMatchingUnique doesn't allow a `return true` exit
            return city.civ.getMatchingUniques(UniqueType.EnablesStatProduction, context)
                .any { it.params[0] == stat.name }
        }
    }

    /** A ruleset-defined perpetual conversion whose output is a mod-defined variable. */
    class VariableConversion(
        val variable: Variable
    ) : PerpetualConstruction(
        variable.name,
        "Convert production to [${variable.name}] at a rate of [rate] to 1"
    ) {
        @Readonly
        override fun getProductionTooltip(city: City, withIcon: Boolean): String {
            val amount = (city.cityStats.currentCityStats.production * city.cityStats.getVariableConversionRate(variable.name)).roundToInt()
            return "\r\n$amount/${Fonts.turn}"
        }

        @Readonly
        override fun getConversionRate(city: City): Int =
            (1 / city.cityStats.getVariableConversionRate(variable.name)).roundToInt().coerceAtLeast(1)

        override fun isBuildable(cityConstructions: CityConstructions): Boolean {
            val context = cityConstructions.city.state
            return cityConstructions.city.civ.getMatchingUniques(UniqueType.EnablesStatProduction, context)
                .any { it.params[0] == variable.name }
        }
    }

    @Readonly
    override fun shouldBeDisplayed(cityConstructions: CityConstructions) = isBuildable(cityConstructions)
    @Readonly
    override fun getStockpiledResourceRequirements(state: GameContext) = Counter.ZERO
    @Readonly
    override fun getResourceRequirementsPerTurn(state: GameContext?) = Counter.ZERO
    @Readonly
    override fun requiredResources(state: GameContext): Set<String> = emptySet()

    object Science : StatConversion(Stat.Science)
    object Gold : StatConversion(Stat.Gold)
    object Culture : StatConversion(Stat.Culture)
    object Faith : StatConversion(Stat.Faith)
    object Food : StatConversion(Stat.Food)
    object Idle : PerpetualConstruction("Nothing", "The city will not produce anything.") {
        override fun getProductionTooltip(city: City, withIcon: Boolean) = ""
        override fun isBuildable(cityConstructions: CityConstructions) = true
    }

    private object Mapper {
        /** Map of built-in names to instances. Variable conversions are ruleset-specific and are resolved separately. */
        val perpetualConstructionsMap: Map<String, PerpetualConstruction> =
            mapOf(Science.name to Science, Gold.name to Gold, Culture.name to Culture, Faith.name to Faith, Food.name to Food, Idle.name to Idle)
    }

    companion object {
        val perpetualConstructionsMap get() = Mapper.perpetualConstructionsMap

        /** Resolves a serialized perpetual-construction name in a particular ruleset. */
        @Readonly
        fun getConstruction(name: String, ruleset: Ruleset): PerpetualConstruction? =
            Mapper.perpetualConstructionsMap[name] ?: ruleset.variables[name]?.let { VariableConversion(it) }

        /** Built-in conversions plus all variable conversions available to this city. */
        @Readonly
        fun getAvailableConstructions(cityConstructions: CityConstructions): Sequence<PerpetualConstruction> {
            val builtIns = Mapper.perpetualConstructionsMap.values.asSequence()
            val variableConversions = cityConstructions.city.getRuleset().variables.values.asSequence()
                .map { VariableConversion(it) }
            return builtIns + variableConversions
        }

        /** @return whether [name] represents a perpetual construction in the given ruleset. */
        @Readonly
        fun isNamePerpetual(name: String, ruleset: Ruleset): Boolean =
            name.isEmpty() || getConstruction(name, ruleset) != null

        /** Legacy overload for callers that only know the built-in map. */
        @Readonly
        fun isNamePerpetual(name: String) = name.isEmpty() || name in Mapper.perpetualConstructionsMap
    }
}
