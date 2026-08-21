package com.unciv.logic.city

import com.unciv.logic.city.managers.CityTurnManager
import com.unciv.models.ruleset.PerpetualConstruction
import com.unciv.models.ruleset.VariableScope
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

/** Coverage for production conversion into a mod-defined variable. */
@RunWith(BaseTestRunner::class)
class VariableProductionConversionTests {

    private class Fixture(scope: VariableScope = VariableScope.City, bonus: String? = null) {
        val game = TestGame().apply { makeHexagonalMap(3) }
        val variable = game.createVariable(default = 0, scope = scope)
        val conversionUnique = "Enables conversion of city production to [${variable.name}]"
        val bonusUnique = bonus?.let { "Production to [${variable.name}] conversion in cities changed by [$it]%" }
        init {
            game.ruleset.addGlobalUniques(conversionUnique, *listOfNotNull(bonusUnique).toTypedArray())
            game.gameInfo.setGlobalTransients()
        }
        val civ = game.addCiv()
        val city = game.addCity(civ, game.getTile(1, 0))
        val conversion = PerpetualConstruction.getConstruction(variable.name, game.ruleset) as PerpetualConstruction.VariableConversion
    }

    @Test
    fun testVariableConversionIsAvailableAndSettled() {
        val f = Fixture()
        Assert.assertTrue(f.conversion.isBuildable(f.city.cityConstructions))
        f.city.cityConstructions.addToQueue(f.conversion)
        f.city.cityStats.update()

        val expected = (f.city.cityStats.currentCityStats.production * 0.25f).roundToInt()
        Assert.assertEquals(expected, f.city.cityStats.variableYields[f.variable.name] ?: 0)

        CityTurnManager(f.city).startTurn()
        Assert.assertEquals(expected, f.city.getVariable(f.variable.name))
    }

    @Test
    fun testVariableConversionBonusChangesRate() {
        val f = Fixture(bonus = "+100")
        f.city.cityConstructions.addToQueue(f.conversion)
        f.city.cityStats.update()

        val expected = (f.city.cityStats.currentCityStats.production * 0.5f).roundToInt()
        Assert.assertEquals(expected, f.city.cityStats.variableYields[f.variable.name] ?: 0)
    }

    @Test
    fun testVariableConversionUsesEachScopeStorage() {
        val scopes = listOf(VariableScope.City, VariableScope.Civ, VariableScope.Global)
        for (scope in scopes) {
            val f = Fixture(scope)
            f.city.cityConstructions.addToQueue(f.conversion)
            f.city.cityStats.update()
            val expected = (f.city.cityStats.currentCityStats.production * 0.25f).roundToInt()
            CityTurnManager(f.city).startTurn()
            val actual = when (scope) {
                VariableScope.City -> f.city.getVariable(f.variable.name)
                VariableScope.Civ -> f.civ.getVariable(f.variable.name)
                VariableScope.Global -> f.game.gameInfo.getVariable(f.variable.name)
                VariableScope.Unit -> throw AssertionError("unit scope cannot be a city production conversion")
            }
            Assert.assertEquals("scope $scope", expected, actual)
        }
    }
}
