package com.unciv.logic.civilization

import com.unciv.models.ruleset.VariableScope
import com.unciv.models.ruleset.unique.Conditionals
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueTriggerActivation
import com.unciv.models.ruleset.validation.UniqueValidator
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/** Coverage for the three separate unique channels (city / civ / global): conditions, triggers,
 *  countable resolution, cross-scope validation and clamping through uniques.
 *  Every test builds its own game to avoid shared-state pollution. */
@RunWith(BaseTestRunner::class)
class VariableThreeScopeTests {

    private class Fixture {
        val game = TestGame().apply { makeHexagonalMap(3) }
        val civInfo = game.addCiv()
        val city = game.addCity(civInfo, game.getTile(1, 0))
    }

    //region City-scope conditionals

    @Test
    fun testCityConditionalInThisCity() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        val conditional = Unique("<when above [5] [${variable.name}] [in this city]>").modifiers.first()
        val cityContext = GameContext(civInfo = f.civInfo, city = f.city)

        f.city.setVariable(variable.name, 3)
        Assert.assertFalse(Conditionals.conditionalApplies(null, conditional, cityContext))

        f.city.setVariable(variable.name, 8)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, cityContext))
    }

    @Test
    fun testCityConditionalBelowAndBetween() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        val below = Unique("<when below [5] [${variable.name}] [in this city]>").modifiers.first()
        val between = Unique("<when between [2] and [4] [${variable.name}] [in this city]>").modifiers.first()
        val cityContext = GameContext(civInfo = f.civInfo, city = f.city)

        f.city.setVariable(variable.name, 3)
        Assert.assertTrue(Conditionals.conditionalApplies(null, below, cityContext))
        Assert.assertTrue(Conditionals.conditionalApplies(null, between, cityContext))

        f.city.setVariable(variable.name, 9)
        Assert.assertFalse(Conditionals.conditionalApplies(null, below, cityContext))
        Assert.assertFalse(Conditionals.conditionalApplies(null, between, cityContext))
    }

    @Test
    fun testCityConditionalRequiresCityContext() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        val conditional = Unique("<when above [5] [${variable.name}] [in this city]>").modifiers.first()
        // No relevant city -> conditional must not apply
        val civContext = GameContext(civInfo = f.civInfo)
        Assert.assertFalse(Conditionals.conditionalApplies(null, conditional, civContext))
    }

    @Test
    fun testCityConditionalRespectsCityFilter() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        val otherCity = f.game.addCity(f.civInfo, f.game.getTile(2, 0))
        // "in capital" only matches the capital - otherCity is not the capital, so the condition fails there
        val conditional = Unique("<when above [5] [${variable.name}] [in capital]>").modifiers.first()
        val otherCityContext = GameContext(civInfo = f.civInfo, city = otherCity)

        otherCity.setVariable(variable.name, 99)
        Assert.assertFalse("non-capital city must not match 'in capital'",
            Conditionals.conditionalApplies(null, conditional, otherCityContext))
    }

    //endregion

    //region Global-scope conditionals

    @Test
    fun testGlobalConditionalAnyContext() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Global)
        val conditional = Unique("<when above [50] [${variable.name}] globally>").modifiers.first()
        val civContext = GameContext(civInfo = f.civInfo)
        val cityContext = GameContext(civInfo = f.civInfo, city = f.city)

        f.game.gameInfo.setVariable(variable.name, 30)
        Assert.assertFalse(Conditionals.conditionalApplies(null, conditional, civContext))

        f.game.gameInfo.setVariable(variable.name, 60)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, civContext))
        Assert.assertTrue("global conditionals work from a city context too",
            Conditionals.conditionalApplies(null, conditional, cityContext))
    }

    @Test
    fun testGlobalConditionalBelowAndBetween() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Global)
        val below = Unique("<when below [50] [${variable.name}] globally>").modifiers.first()
        val between = Unique("<when between [10] and [20] [${variable.name}] globally>").modifiers.first()
        val civContext = GameContext(civInfo = f.civInfo)

        f.game.gameInfo.setVariable(variable.name, 15)
        Assert.assertTrue(Conditionals.conditionalApplies(null, below, civContext))
        Assert.assertTrue(Conditionals.conditionalApplies(null, between, civContext))

        f.game.gameInfo.setVariable(variable.name, 70)
        Assert.assertFalse(Conditionals.conditionalApplies(null, below, civContext))
        Assert.assertFalse(Conditionals.conditionalApplies(null, between, civContext))
    }

    //endregion

    //region Civ-scope conditionals still work

    @Test
    fun testCivConditionalStillWorks() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Civ)
        val conditional = Unique("<when above [5] [${variable.name}]>").modifiers.first()
        val cityContext = GameContext(civInfo = f.civInfo, city = f.city)

        f.civInfo.setVariable(variable.name, 8)
        Assert.assertTrue("civ conditional resolves from a city context to the civ value",
            Conditionals.conditionalApplies(null, conditional, cityContext))
    }

    //endregion

    //region City-scope triggers

    @Test
    fun testCityTriggerProvideInThisCity() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        val triggered = UniqueTriggerActivation.triggerUnique(
            Unique("Instantly provides [4] [${variable.name}] [in this city]"), f.city)
        Assert.assertTrue(triggered)
        Assert.assertEquals(4, f.city.getVariable(variable.name))
    }

    @Test
    fun testCityTriggerProvideInAllCitiesFromCityContext() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        val otherCity = f.game.addCity(f.civInfo, f.game.getTile(2, 0))
        val triggered = UniqueTriggerActivation.triggerUnique(
            Unique("Instantly provides [3] [${variable.name}] [in all cities]"), f.city)
        Assert.assertTrue(triggered)
        Assert.assertEquals(3, f.city.getVariable(variable.name))
        Assert.assertEquals(3, otherCity.getVariable(variable.name))
    }

    @Test
    fun testCityTriggerConsumeGainAndSet() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 10, scope = VariableScope.City)
        f.city.setVariable(variable.name, 10)

        UniqueTriggerActivation.triggerUnique(Unique("Instantly consumes [3] [${variable.name}] [in this city]"), f.city)
        Assert.assertEquals(7, f.city.getVariable(variable.name))

        UniqueTriggerActivation.triggerUnique(Unique("Instantly gain [2] [${variable.name}] [in this city]"), f.city)
        Assert.assertEquals(9, f.city.getVariable(variable.name))

        UniqueTriggerActivation.triggerUnique(Unique("Set [${variable.name}] to [15] [in this city]"), f.city)
        Assert.assertEquals(15, f.city.getVariable(variable.name))
    }

    @Test
    fun testCityTriggerRespectsClamp() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 50, scope = VariableScope.City)
        variable.min = 0
        variable.max = 100
        UniqueTriggerActivation.triggerUnique(Unique("Instantly provides [200] [${variable.name}] [in this city]"), f.city)
        Assert.assertEquals(100, f.city.getVariable(variable.name))
    }

    //endregion

    //region Global-scope triggers

    @Test
    fun testGlobalTriggerProvide() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Global)
        val triggered = UniqueTriggerActivation.triggerUnique(
            Unique("Instantly provides [4] [${variable.name}] globally"), f.civInfo)
        Assert.assertTrue(triggered)
        Assert.assertEquals(4, f.game.gameInfo.getVariable(variable.name))
    }

    @Test
    fun testGlobalTriggerFromCityContext() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Global)
        UniqueTriggerActivation.triggerUnique(Unique("Instantly provides [2] [${variable.name}] globally"), f.city)
        Assert.assertEquals(2, f.game.gameInfo.getVariable(variable.name))
    }

    @Test
    fun testGlobalTriggerConsumeGainAndSet() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Global)
        f.game.gameInfo.setVariable(variable.name, 10)

        UniqueTriggerActivation.triggerUnique(Unique("Instantly consumes [3] [${variable.name}] globally"), f.civInfo)
        Assert.assertEquals(7, f.game.gameInfo.getVariable(variable.name))

        UniqueTriggerActivation.triggerUnique(Unique("Instantly gain [2] [${variable.name}] globally"), f.civInfo)
        Assert.assertEquals(9, f.game.gameInfo.getVariable(variable.name))

        UniqueTriggerActivation.triggerUnique(Unique("Set [${variable.name}] to [15] globally"), f.civInfo)
        Assert.assertEquals(15, f.game.gameInfo.getVariable(variable.name))
    }

    @Test
    fun testGlobalTriggerRespectsClamp() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Global)
        variable.max = 10
        UniqueTriggerActivation.triggerUnique(Unique("Instantly provides [50] [${variable.name}] globally"), f.civInfo)
        Assert.assertEquals(10, f.game.gameInfo.getVariable(variable.name))
    }

    //endregion

    //region Countable resolution by scope

    @Test
    fun testCityVariableCountableInCityContext() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        f.city.setVariable(variable.name, 7)
        val conditional = Unique("<when number of [${variable.name}] is more than [5]>").modifiers.first()
        val cityContext = GameContext(civInfo = f.civInfo, city = f.city)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, cityContext))
    }

    @Test
    fun testGlobalVariableCountableInCivContext() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Global)
        f.game.gameInfo.setVariable(variable.name, 7)
        val conditional = Unique("<when number of [${variable.name}] is more than [5]>").modifiers.first()
        val civContext = GameContext(civInfo = f.civInfo)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, civContext))
    }

    //endregion

    //region Cross-scope validation

    private fun checkErrors(game: TestGame, uniqueText: String): Boolean {
        val unique = Unique(uniqueText)
        val errors = UniqueValidator(game.ruleset)
            .checkUnique(unique, false, null, UniqueValidator.allParameterSeverities)
        return errors.isNotOK()
    }

    @Test
    fun testCityVariableInCivConditionalIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        Assert.assertTrue("city variable must not validate in the civ conditional",
            checkErrors(f.game, "when above [5] [${variable.name}]"))
    }

    @Test
    fun testGlobalVariableInCivConditionalIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Global)
        Assert.assertTrue("global variable must not validate in the civ conditional",
            checkErrors(f.game, "when above [5] [${variable.name}]"))
    }

    @Test
    fun testWrongScopeCivConditionalDoesNotEvaluate() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        f.city.setVariable(variable.name, 99)
        val conditional = Unique("<when above [5] [${variable.name}]>").modifiers.first()
        Assert.assertFalse(Conditionals.conditionalApplies(null, conditional, GameContext(civInfo = f.civInfo, city = f.city)))
    }

    @Test
    fun testWrongScopeGenericTriggerDoesNotActivate() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Global)
        Assert.assertFalse(UniqueTriggerActivation.triggerUnique(
            Unique("Instantly provides [4] [${variable.name}]"), f.civInfo))
        Assert.assertEquals(0, f.game.gameInfo.getVariable(variable.name))
    }

    @Test
    fun testCivVariableInCityConditionalIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Civ)
        Assert.assertTrue("civ variable must not validate in the city conditional",
            checkErrors(f.game, "when above [5] [${variable.name}] [in this city]"))
    }

    @Test
    fun testCityVariableInGlobalConditionalIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        Assert.assertTrue("city variable must not validate in the global conditional",
            checkErrors(f.game, "when above [5] [${variable.name}] globally"))
    }

    @Test
    fun testCityVariableInCivProvideIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        Assert.assertTrue("city variable must not validate in the civ provide",
            checkErrors(f.game, "Instantly provides [4] [${variable.name}]"))
    }

    @Test
    fun testGlobalVariableInCivGainIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Global)
        Assert.assertTrue("global variable must not validate in the civ gain",
            checkErrors(f.game, "Instantly gain [4] [${variable.name}]"))
    }

    @Test
    fun testCivVariableInGlobalProvideIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Civ)
        Assert.assertTrue("civ variable must not validate in the global provide",
            checkErrors(f.game, "Instantly provides [4] [${variable.name}] globally"))
    }

    @Test
    fun testDerivedStatsYieldWithVariableIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Civ)
        Assert.assertTrue("derived yields must not accept variables",
            checkErrors(f.game, "[+2 ${variable.name}] per [2] population [in this city]"))
    }

    @Test
    fun testCorrectScopeCombinationsPass() {
        val f = Fixture()
        val cityVariable = f.game.createVariable(default = 0, scope = VariableScope.City)
        val civVariable = f.game.createVariable(default = 0, scope = VariableScope.Civ)
        val globalVariable = f.game.createVariable(default = 0, scope = VariableScope.Global)

        Assert.assertFalse(checkErrors(f.game, "when above [5] [${civVariable.name}]"))
        Assert.assertFalse(checkErrors(f.game, "when above [5] [${cityVariable.name}] [in this city]"))
        Assert.assertFalse(checkErrors(f.game, "when above [5] [${globalVariable.name}] globally"))
        Assert.assertFalse(checkErrors(f.game, "Instantly provides [4] [${civVariable.name}]"))
        Assert.assertFalse(checkErrors(f.game, "Instantly provides [4] [${cityVariable.name}] [in this city]"))
        Assert.assertFalse(checkErrors(f.game, "Instantly provides [4] [${globalVariable.name}] globally"))
        Assert.assertFalse(checkErrors(f.game, "Instantly gain [4] [${civVariable.name}]"))
        Assert.assertFalse(checkErrors(f.game, "Set [${cityVariable.name}] to [5] [in this city]"))
        Assert.assertFalse(checkErrors(f.game, "Set [${globalVariable.name}] to [5] globally"))
    }

    //endregion
}
