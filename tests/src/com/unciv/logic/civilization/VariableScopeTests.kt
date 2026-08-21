package com.unciv.logic.civilization

import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.models.ruleset.VariableScope
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueTriggerActivation
import com.unciv.models.ruleset.validation.RulesetValidator
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/** Coverage for the three variable scopes (city / civ / global): storage, fallback, clamp,
 *  serialization and validation rules. */
@RunWith(BaseTestRunner::class)
class VariableScopeTests {
    private val game = TestGame().apply { makeHexagonalMap(3) }
    private val civInfo = game.addCiv()

    @Test
    fun testVariableScopeJsonUsesLowercaseAndReadsCaseInsensitive() {
        val lower = json().fromJson(com.unciv.models.ruleset.Variable::class.java,
            "{\"name\":\"X\",\"scope\":\"city\"}")
        val upper = json().fromJson(com.unciv.models.ruleset.Variable::class.java,
            "{\"name\":\"X\",\"scope\":\"City\"}")
        Assert.assertEquals(VariableScope.City, lower.scope)
        Assert.assertEquals(VariableScope.City, upper.scope)
        Assert.assertTrue(json().toJson(lower).contains("\"scope\":\"city\""))
    }
    //region City-level variables

    @Test
    fun testCityVariableStoreAndFallback() {
        val variable = game.createVariable(default = 5, scope = VariableScope.City)
        val city = game.addCity(civInfo, game.getTile(1, 0))

        Assert.assertEquals(5, city.getVariable(variable.name)) // falls back to ruleset default

        city.setVariable(variable.name, 10)
        Assert.assertEquals(10, city.getVariable(variable.name))

        city.addVariable(variable.name, 3)
        Assert.assertEquals(13, city.getVariable(variable.name))
    }

    @Test
    fun testCityVariablesArePerCity() {
        val variable = game.createVariable(default = 0, scope = VariableScope.City)
        val city1 = game.addCity(civInfo, game.getTile(1, 0))
        val city2 = game.addCity(civInfo, game.getTile(2, 0))

        city1.setVariable(variable.name, 7)
        Assert.assertEquals(7, city1.getVariable(variable.name))
        Assert.assertEquals(0, city2.getVariable(variable.name))
    }

    @Test
    fun testUniqueToRestrictsCityAndCivVariables() {
        val otherCiv = game.addCiv()
        val city = game.addCity(civInfo, game.getTile(1, 0))
        val otherCity = game.addCity(otherCiv, game.getTile(2, 0))
        val uniqueTo = civInfo.nation.name

        val civVariable = game.createVariable(default = 0, scope = VariableScope.Civ).apply { this.uniqueTo = uniqueTo }
        civInfo.setVariable(civVariable.name, 7)
        otherCiv.setVariable(civVariable.name, 9)
        Assert.assertEquals(7, civInfo.getVariable(civVariable.name))
        Assert.assertEquals(0, otherCiv.getVariable(civVariable.name))

        val cityVariable = game.createVariable(default = 0, scope = VariableScope.City).apply { this.uniqueTo = uniqueTo }
        city.setVariable(cityVariable.name, 7)
        otherCity.setVariable(cityVariable.name, 9)
        Assert.assertEquals(7, city.getVariable(cityVariable.name))
        Assert.assertEquals(0, otherCity.getVariable(cityVariable.name))
    }

    @Test
    fun testScopedGameResourceRoutesFromCityContext() {
        val city = game.addCity(civInfo, game.getTile(1, 0))
        val cityVariable = game.createVariable(scope = VariableScope.City)
        val civVariable = game.createVariable(scope = VariableScope.Civ)
        val globalVariable = game.createVariable(scope = VariableScope.Global)

        city.addGameResource(cityVariable, 1)
        city.addGameResource(civVariable, 2)
        city.addGameResource(globalVariable, 3)

        Assert.assertEquals(1, city.getVariable(cityVariable.name))
        Assert.assertEquals(2, civInfo.getVariable(civVariable.name))
        Assert.assertEquals(3, game.gameInfo.getVariable(globalVariable.name))
        Assert.assertEquals(2, city.getGameResource(civVariable))
        Assert.assertEquals(3, city.getGameResource(globalVariable))
    }

    @Test
    fun testCityVariableClamp() {
        val variable = game.createVariable(default = 50, scope = VariableScope.City)
        variable.min = 0
        variable.max = 100
        val city = game.addCity(civInfo, game.getTile(1, 0))

        city.setVariable(variable.name, 200)
        Assert.assertEquals(100, city.getVariable(variable.name))

        city.addVariable(variable.name, 50)
        Assert.assertEquals(100, city.getVariable(variable.name)) // stays clamped

        city.addVariable(variable.name, -500)
        Assert.assertEquals(0, city.getVariable(variable.name))
    }

    @Test
    fun testCityVariablesSurviveClone() {
        val variable = game.createVariable(default = 1, scope = VariableScope.City)
        val city = game.addCity(civInfo, game.getTile(1, 0))
        city.setVariable(variable.name, 42)

        val cloned = city.clone()
        Assert.assertEquals(42, cloned.getVariable(variable.name))
    }

    @Test
    fun testCityVariableSerializationRoundTrip() {
        val variable = game.createVariable(default = 5, scope = VariableScope.City)
        val city = game.addCity(civInfo, game.getTile(1, 0))
        city.setVariable(variable.name, 42)

        val jsonText = json().toJson(city)
        Assert.assertTrue("\"variables\":{\"${variable.name}\":42}" in jsonText)

        val roundTripped = json().fromJson(com.unciv.logic.city.City::class.java, jsonText)
        roundTripped.civ = civInfo
        Assert.assertEquals(42, roundTripped.getVariable(variable.name))
    }

    @Test
    fun testOldCitySaveWithoutVariablesFallsBackToDefault() {
        val variable = game.createVariable(default = 7, scope = VariableScope.City)
        val city = game.addCity(civInfo, game.getTile(1, 0))
        city.setVariable(variable.name, 42)

        val jsonText = json().toJson(city)
        val withoutVariables = Regex("\"variables\":\\{[^}]*\\},").replaceFirst(jsonText, "")
        Assert.assertFalse("precondition: variables field removed", "\"variables\"" in withoutVariables)

        val roundTripped = json().fromJson(com.unciv.logic.city.City::class.java, withoutVariables)
        roundTripped.civ = civInfo
        Assert.assertEquals(7, roundTripped.getVariable(variable.name))
    }

    //endregion

    //region Global variables

    @Test
    fun testGlobalVariableStoreAndFallback() {
        val variable = game.createVariable(default = 5, scope = VariableScope.Global)

        Assert.assertEquals(5, game.gameInfo.getVariable(variable.name))

        game.gameInfo.setVariable(variable.name, 10)
        Assert.assertEquals(10, game.gameInfo.getVariable(variable.name))

        game.gameInfo.addVariable(variable.name, 3)
        Assert.assertEquals(13, game.gameInfo.getVariable(variable.name))
    }

    @Test
    fun testGlobalVariableClamp() {
        val variable = game.createVariable(default = 0, scope = VariableScope.Global)
        variable.min = -10
        variable.max = 10

        game.gameInfo.setVariable(variable.name, 100)
        Assert.assertEquals(10, game.gameInfo.getVariable(variable.name))

        game.gameInfo.addVariable(variable.name, -100)
        Assert.assertEquals(-10, game.gameInfo.getVariable(variable.name))
    }

    @Test
    fun testGlobalVariableSerializationRoundTrip() {
        val variable = game.createVariable(default = 5, scope = VariableScope.Global)
        game.gameInfo.setVariable(variable.name, 42)
        val jsonText = json().toJson(game.gameInfo)
        Assert.assertTrue("\"variables\":{\"${variable.name}\":42}" in jsonText)

        val roundTripped = json().fromJson(GameInfo::class.java, jsonText)
        roundTripped.ruleset = game.gameInfo.ruleset
        Assert.assertEquals(42, roundTripped.getVariable(variable.name))
    }

    @Test
    fun testGlobalVariablesSurviveGameClone() {
        val variable = game.createVariable(default = 5, scope = VariableScope.Global)
        game.gameInfo.setVariable(variable.name, 42)

        val cloned = game.gameInfo.clone()
        cloned.ruleset = game.ruleset
        Assert.assertEquals(42, cloned.getVariable(variable.name))
    }

    @Test
    fun testOldGameSaveWithoutVariablesFallsBackToDefault() {
        val variable = game.createVariable(default = 7, scope = VariableScope.Global)
        game.gameInfo.setVariable(variable.name, 42)
        val jsonText = json().toJson(game.gameInfo)
        val withoutVariables = Regex("\"variables\":\\{[^}]*\\}").replace(jsonText, "") // removes civ-level and game-level variables alike
        Assert.assertFalse("precondition: variables field removed", "\"variables\"" in withoutVariables)

        val roundTripped = json().fromJson(GameInfo::class.java, withoutVariables)
        roundTripped.ruleset = game.gameInfo.ruleset
        Assert.assertEquals(7, roundTripped.getVariable(variable.name))
    }

    //endregion

    //region Civ-level clamp

    @Test
    fun testCivVariableClamp() {
        val variable = game.createVariable(default = 0, scope = VariableScope.Civ)
        variable.min = 0
        variable.max = 100

        civInfo.setVariable(variable.name, 500)
        Assert.assertEquals(100, civInfo.getVariable(variable.name))

        civInfo.addVariable(variable.name, 50)
        Assert.assertEquals(100, civInfo.getVariable(variable.name))

        civInfo.addVariable(variable.name, -1000)
        Assert.assertEquals(0, civInfo.getVariable(variable.name))
    }

    @Test
    fun testCivVariableGenericGainRespectsClamp() {
        val variable = game.createVariable(default = 0, scope = VariableScope.Civ)
        variable.max = 10

        val triggered = UniqueTriggerActivation.triggerUnique(
            Unique("Instantly gain [50] [${variable.name}]"), civInfo)
        Assert.assertTrue(triggered)
        Assert.assertEquals(10, civInfo.getVariable(variable.name))
    }

    @Test
    fun testVariableAddDoesNotOverflowBeforeClamping() {
        val city = game.addCity(civInfo, game.getTile(1, 0))
        for (scope in VariableScope.entries) {
            val variable = game.createVariable(default = 0, scope = scope)
            variable.min = -100
            variable.max = 100
            when (scope) {
                VariableScope.City -> {
                    city.setVariable(variable.name, 90)
                    city.addVariable(variable.name, Int.MAX_VALUE)
                    Assert.assertEquals(100, city.getVariable(variable.name))
                    city.setVariable(variable.name, -90)
                    city.addVariable(variable.name, Int.MIN_VALUE)
                    Assert.assertEquals(-100, city.getVariable(variable.name))
                }
                VariableScope.Civ -> {
                    civInfo.setVariable(variable.name, 90)
                    civInfo.addVariable(variable.name, Int.MAX_VALUE)
                    Assert.assertEquals(100, civInfo.getVariable(variable.name))
                    civInfo.setVariable(variable.name, -90)
                    civInfo.addVariable(variable.name, Int.MIN_VALUE)
                    Assert.assertEquals(-100, civInfo.getVariable(variable.name))
                }
                VariableScope.Global -> {
                    game.gameInfo.setVariable(variable.name, 90)
                    game.gameInfo.addVariable(variable.name, Int.MAX_VALUE)
                    Assert.assertEquals(100, game.gameInfo.getVariable(variable.name))
                    game.gameInfo.setVariable(variable.name, -90)
                    game.gameInfo.addVariable(variable.name, Int.MIN_VALUE)
                    Assert.assertEquals(-100, game.gameInfo.getVariable(variable.name))
                }
                VariableScope.Unit -> {
                    val unit = game.addDefaultMeleeUnitWithUniques(civInfo, game.getTile(1, 0))
                    unit.setVariable(variable.name, 90)
                    unit.addVariable(variable.name, Int.MAX_VALUE)
                    Assert.assertEquals(100, unit.getVariable(variable.name))
                    unit.setVariable(variable.name, -90)
                    unit.addVariable(variable.name, Int.MIN_VALUE)
                    Assert.assertEquals(-100, unit.getVariable(variable.name))
                }
            }
        }
    }

    @Test
    fun testUnknownVariableIsNotClamped() {
        // No ruleset definition -> no min/max -> value stored as-is
        civInfo.setVariable("NoSuchVariable", 999)
        Assert.assertEquals(999, civInfo.getVariable("NoSuchVariable"))
        civInfo.addVariable("NoSuchVariable", 1)
        Assert.assertEquals(1000, civInfo.getVariable("NoSuchVariable"))
    }

    //endregion

    //region Validation

    @Test
    fun testVariableMissingScopeIsAnError() {
        val cleanGame = TestGame().apply { makeHexagonalMap(3) }
        cleanGame.ruleset.variables["NoScope"] = com.unciv.models.ruleset.Variable().apply { name = "NoScope" } // scope stays null
        val errors = RulesetValidator.create(cleanGame.ruleset).getErrorList()
        Assert.assertTrue("Missing scope must be reported",
            errors.any { "missing a scope declaration" in it.text })
    }

    @Test
    fun testGlobalVariableWithUniqueToIsAnError() {
        val cleanGame = TestGame().apply { makeHexagonalMap(3) }
        cleanGame.createVariable(default = 0, scope = VariableScope.Global).apply { uniqueTo = "SomeCiv" }
        val errors = RulesetValidator.create(cleanGame.ruleset).getErrorList()
        Assert.assertTrue("Global variable with uniqueTo must be reported",
            errors.any { "must not declare uniqueTo" in it.text })
    }

    @Test
    fun testVariableMinGreaterThanMaxIsAnError() {
        val cleanGame = TestGame().apply { makeHexagonalMap(3) }
        cleanGame.createVariable(default = 0, scope = VariableScope.City).apply { min = 100; max = 0 }
        val errors = RulesetValidator.create(cleanGame.ruleset).getErrorList()
        Assert.assertTrue("min > max must be reported",
            errors.any { "min greater than max" in it.text })
    }

    @Test
    fun testVariableDefaultOutsideMinMaxIsAnError() {
        val cleanGame = TestGame().apply { makeHexagonalMap(3) }
        cleanGame.createVariable(default = 50, scope = VariableScope.Civ).apply { min = 0; max = 10 }
        val errors = RulesetValidator.create(cleanGame.ruleset).getErrorList()
        Assert.assertTrue("default outside min/max must be reported",
            errors.any { "default outside of its min/max range" in it.text })
    }

    @Test
    fun testVariableWithConstructionNameIsAnError() {
        val cleanGame = TestGame().apply { makeHexagonalMap(3) }
        val buildingName = cleanGame.ruleset.buildings.keys.first()
        val perpetualName = "Nothing"
        for (name in listOf(buildingName, perpetualName)) {
            cleanGame.ruleset.variables[name] = com.unciv.models.ruleset.Variable().apply {
                this.name = name
                scope = VariableScope.City
            }
        }

        val errors = RulesetValidator.create(cleanGame.ruleset).getErrorList()
        Assert.assertTrue("Building name collision must be reported",
            errors.any { buildingName in it.text && "collides with a construction name" in it.text })
        Assert.assertTrue("Perpetual construction name collision must be reported",
            errors.any { perpetualName in it.text && "collides with a construction name" in it.text })
    }

    @Test
    fun testExplicitScopeDeclarationsPassValidation() {
        val cleanGame = TestGame().apply { makeHexagonalMap(3) }
        cleanGame.createVariable(default = 0, scope = VariableScope.City)
        cleanGame.createVariable(default = 0, scope = VariableScope.Civ)
        cleanGame.createVariable(default = 0, scope = VariableScope.Global)
        val errors = RulesetValidator.create(cleanGame.ruleset).getErrorList()
        Assert.assertFalse("Explicit scopes must not be reported",
            errors.any { "missing a scope declaration" in it.text })
    }

    //endregion
}
