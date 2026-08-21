package com.unciv.logic.city

import com.unciv.logic.city.managers.CityTurnManager
import com.unciv.models.ruleset.VariableScope
import com.unciv.models.ruleset.unique.Unique
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/** Coverage for per-turn variable yields (module A): `[+2] [MyVar]` in the [stats] parameter slot is
 *  collected per city and settled into the variable's scope at turn start, mirroring the stat model. */
@RunWith(BaseTestRunner::class)
class VariableYieldTests {

    private class Fixture {
        val game = TestGame().apply { makeHexagonalMap(3) }
        val civInfo = game.addCiv()
        val city = game.addCity(civInfo, game.getTile(1, 0))
    }

    private fun addBuilding(f: Fixture, vararg uniques: String) {
        val building = f.game.createBuilding(*uniques)
        f.city.cityConstructions.addBuilding(building)
    }

    //region City-scope yields

    @Test
    fun testCityVariableYieldFromBuilding() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        addBuilding(f, "[+2 ${variable.name}]")

        f.city.cityStats.update()
        Assert.assertEquals(2, f.city.cityStats.variableYields[variable.name])

        CityTurnManager(f.city).startTurn()
        Assert.assertEquals(2, f.city.getVariable(variable.name))
    }

    @Test
    fun testCityVariableYieldFromBuildingWithCityFilter() {
        // StatsPerCity form: `[+2 MyVar] [in this city]`
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        addBuilding(f, "[+2 ${variable.name}] [in this city]")

        CityTurnManager(f.city).startTurn()
        Assert.assertEquals(2, f.city.getVariable(variable.name))
    }

    @Test
    fun testCityFilteredVariableYieldSkipsNonMatchingCity() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        f.game.ruleset.addGlobalUniques("[+2 ${variable.name}] [in capital]")
        f.game.gameInfo.setGlobalTransients()
        val otherCity = f.game.addCity(f.civInfo, f.game.getTile(2, 0))

        CityTurnManager(f.city).startTurn()
        CityTurnManager(otherCity).startTurn()

        Assert.assertEquals(2, f.city.getVariable(variable.name))
        Assert.assertEquals(0, otherCity.getVariable(variable.name))
    }

    @Test
    fun testNonLocalBuildingVariableYieldsAndPercentApplyInAllCities() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        addBuilding(f, "[+1 ${variable.name}] [in all cities]", "[+100]% [${variable.name}] [in all cities]")
        val otherCity = f.game.addCity(f.civInfo, f.game.getTile(2, 0))

        CityTurnManager(f.city).startTurn()
        CityTurnManager(otherCity).startTurn()

        Assert.assertEquals(2, f.city.getVariable(variable.name))
        Assert.assertEquals(2, otherCity.getVariable(variable.name))
    }

    @Test
    fun testCityVariableYieldRespectsClamp() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 50, scope = VariableScope.City)
        variable.min = 0
        variable.max = 100
        addBuilding(f, "[+200 ${variable.name}]")

        CityTurnManager(f.city).startTurn()
        Assert.assertEquals(100, f.city.getVariable(variable.name))
    }

    //endregion

    //region Civ-scope yields

    @Test
    fun testCivVariableYieldFromBuilding() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Civ)
        addBuilding(f, "[+2 ${variable.name}]")

        CityTurnManager(f.city).startTurn()
        Assert.assertEquals(2, f.civInfo.getVariable(variable.name))
    }

    @Test
    fun testCivVariableYieldStacksPerCityLikeStats() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Civ)
        addBuilding(f, "[+2 ${variable.name}]")
        val secondCity = f.game.addCity(f.civInfo, f.game.getTile(2, 0))
        val building = f.game.createBuilding("[+2 ${variable.name}]")
        secondCity.cityConstructions.addBuilding(building)

        CityTurnManager(f.city).startTurn()
        CityTurnManager(secondCity).startTurn()
        // Mirrors the stat model: each city contributes its own yields
        Assert.assertEquals(4, f.civInfo.getVariable(variable.name))
    }

    //endregion

    //region Global-scope yields

    @Test
    fun testGlobalVariableYieldFromBuilding() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Global)
        addBuilding(f, "[+2 ${variable.name}]")

        CityTurnManager(f.city).startTurn()
        Assert.assertEquals(2, f.game.gameInfo.getVariable(variable.name))
    }

    @Test
    fun testGlobalVariableYieldStacksPerBuildingInstance() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Global)
        addBuilding(f, "[+1 ${variable.name}]")
        val secondCity = f.game.addCity(f.civInfo, f.game.getTile(2, 0))
        val building = f.game.createBuilding("[+1 ${variable.name}]")
        secondCity.cityConstructions.addBuilding(building)

        CityTurnManager(f.city).startTurn()
        CityTurnManager(secondCity).startTurn()
        // Q26A: every source instance stacks onto the single game-wide value
        Assert.assertEquals(2, f.game.gameInfo.getVariable(variable.name))
    }

    //endregion

    //region Mixed stat+variable entries

    @Test
    fun testMixedStatsAndVariableYields() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        addBuilding(f, "[+2 Gold, +1 ${variable.name}]")

        f.city.cityStats.update()
        // The stat part must still work: +2 Gold city yield
        Assert.assertTrue("building gold yield must be present, got ${f.city.cityStats.currentCityStats.gold}",
            f.city.cityStats.currentCityStats.gold >= 2f)
        // And the variable part is collected separately
        Assert.assertEquals(1, f.city.cityStats.variableYields[variable.name])

        CityTurnManager(f.city).startTurn()
        Assert.assertEquals(1, f.city.getVariable(variable.name))
    }

    @Test
    fun testBuildingWithOnlyVariableStatsHasNoStatYields() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        addBuilding(f, "[+2 ${variable.name}]")

        f.city.cityStats.update()
        Assert.assertEquals(2, f.city.cityStats.variableYields[variable.name])
    }

    @Test
    fun testLenientStatsSkipUnknownEntries() {
        val stats = Unique("[+2 Gold, +1 NotADeclaredVariable]").stats
        Assert.assertEquals(2f, stats.gold)
        Assert.assertFalse(stats.isEmpty())
    }

    //endregion

    //region Percentage bonuses on yields

    @Test
    fun testVariableYieldPercentBonus() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        addBuilding(f, "[+2 ${variable.name}]", "[+50]% [${variable.name}]") // 2 * 1.5 = 3

        CityTurnManager(f.city).startTurn()
        Assert.assertEquals(3, f.city.getVariable(variable.name))
    }

    @Test
    fun testVariableYieldPercentBonusesAreAdditive() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        addBuilding(f, "[+2 ${variable.name}]", "[+50]% [${variable.name}]", "[+50]% [${variable.name}]") // 2 * 2.0 = 4

        CityTurnManager(f.city).startTurn()
        Assert.assertEquals(4, f.city.getVariable(variable.name))
    }

    @Test
    fun testVariableYieldPercentBonusWithCityFilter() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        addBuilding(f, "[+2 ${variable.name}]", "[+50]% [${variable.name}] [in this city]")

        CityTurnManager(f.city).startTurn()
        Assert.assertEquals(3, f.city.getVariable(variable.name))
    }

    @Test
    fun testVariablePercentBonusAppliesToCivAndGlobalScopes() {
        val f = Fixture()
        val civVariable = f.game.createVariable(default = 0, scope = VariableScope.Civ)
        val globalVariable = f.game.createVariable(default = 0, scope = VariableScope.Global)
        addBuilding(f, "[+2 ${civVariable.name}]", "[+50]% [${civVariable.name}]",
            "[+2 ${globalVariable.name}]", "[+50]% [${globalVariable.name}]")

        CityTurnManager(f.city).startTurn()
        Assert.assertEquals(3, f.civInfo.getVariable(civVariable.name))
        Assert.assertEquals(3, f.game.gameInfo.getVariable(globalVariable.name))
    }

    //endregion
}
