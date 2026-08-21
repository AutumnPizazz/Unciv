package com.unciv.logic.map.mapunit

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

/** Coverage for the unit-scope variable unique channels: conditions, triggers, countable
 *  resolution, [unitFilter] targeting and cross-scope validation. */
@RunWith(BaseTestRunner::class)
class UnitVariableUniqueTests {

    private class Fixture {
        val game = TestGame().apply { makeHexagonalMap(3) }
        val civInfo = game.addCiv()
        val meleeUnit = game.addDefaultMeleeUnitWithUniques(civInfo, game.getTile(1, 0))
        val rangedUnit = game.addDefaultRangedUnitWithUniques(civInfo, game.getTile(2, 0))
    }

    //region Unit-scope conditionals

    @Test
    fun testUnitConditionalInThisUnit() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        val conditional = Unique("<when above [5] [${variable.name}] on [this unit]>").modifiers.first()
        val unitContext = GameContext(f.meleeUnit)

        f.meleeUnit.setVariable(variable.name, 3)
        Assert.assertFalse(Conditionals.conditionalApplies(null, conditional, unitContext))

        f.meleeUnit.setVariable(variable.name, 8)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, unitContext))
    }

    @Test
    fun testUnitConditionalBelowAndBetween() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        val below = Unique("<when below [5] [${variable.name}] on [this unit]>").modifiers.first()
        val between = Unique("<when between [2] and [4] [${variable.name}] on [this unit]>").modifiers.first()
        val unitContext = GameContext(f.meleeUnit)

        f.meleeUnit.setVariable(variable.name, 3)
        Assert.assertTrue(Conditionals.conditionalApplies(null, below, unitContext))
        Assert.assertTrue(Conditionals.conditionalApplies(null, between, unitContext))

        f.meleeUnit.setVariable(variable.name, 9)
        Assert.assertFalse(Conditionals.conditionalApplies(null, below, unitContext))
        Assert.assertFalse(Conditionals.conditionalApplies(null, between, unitContext))
    }

    @Test
    fun testUnitConditionalRequiresUnitContext() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        val conditional = Unique("<when above [5] [${variable.name}] on [this unit]>").modifiers.first()
        // No relevant unit -> conditional must not apply
        val civContext = GameContext(civInfo = f.civInfo)
        Assert.assertFalse(Conditionals.conditionalApplies(null, conditional, civContext))
    }

    @Test
    fun testUnitConditionalRespectsUnitFilter() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        // "Melee" only matches the melee unit - the ranged unit must fail the condition even with a high value
        val conditional = Unique("<when above [5] [${variable.name}] on [Melee]>").modifiers.first()

        f.meleeUnit.setVariable(variable.name, 99)
        f.rangedUnit.setVariable(variable.name, 99)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, GameContext(f.meleeUnit)))
        Assert.assertFalse("non-melee unit must not match the Melee filter",
            Conditionals.conditionalApplies(null, conditional, GameContext(f.rangedUnit)))
    }

    //endregion

    //region Unit-scope triggers

    @Test
    fun testUnitTriggerProvideInThisUnit() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        val triggered = UniqueTriggerActivation.triggerUnique(
            Unique("Instantly provides [4] [${variable.name}] on [this unit]"), f.meleeUnit)
        Assert.assertTrue(triggered)
        Assert.assertEquals(4, f.meleeUnit.getVariable(variable.name))
        Assert.assertEquals("only the contextual unit is affected", 0, f.rangedUnit.getVariable(variable.name))
    }

    @Test
    fun testUnitTriggerProvideMatchingFilterFromCivContext() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        val triggered = UniqueTriggerActivation.triggerUnique(
            Unique("Instantly provides [3] [${variable.name}] on [Melee]"), f.civInfo)
        Assert.assertTrue(triggered)
        Assert.assertEquals(3, f.meleeUnit.getVariable(variable.name))
        Assert.assertEquals("ranged unit must not match the Melee filter", 0, f.rangedUnit.getVariable(variable.name))
    }

    @Test
    fun testUnitTriggerConsumeGainAndSet() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 10, scope = VariableScope.Unit)
        f.meleeUnit.setVariable(variable.name, 10)

        UniqueTriggerActivation.triggerUnique(Unique("Instantly consumes [3] [${variable.name}] on [this unit]"), f.meleeUnit)
        Assert.assertEquals(7, f.meleeUnit.getVariable(variable.name))

        UniqueTriggerActivation.triggerUnique(Unique("Instantly gain [2] [${variable.name}] on [this unit]"), f.meleeUnit)
        Assert.assertEquals(9, f.meleeUnit.getVariable(variable.name))

        UniqueTriggerActivation.triggerUnique(Unique("Set [${variable.name}] to [15] on [this unit]"), f.meleeUnit)
        Assert.assertEquals(15, f.meleeUnit.getVariable(variable.name))
    }

    @Test
    fun testUnitTriggerSetWithCountableUsesUnitContext() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        f.meleeUnit.health = 37
        val triggered = UniqueTriggerActivation.triggerUnique(
            Unique("Set [${variable.name}] to [Unit Health] on [this unit]"), f.meleeUnit)
        Assert.assertTrue(triggered)
        Assert.assertEquals(37, f.meleeUnit.getVariable(variable.name))
    }

    @Test
    fun testUnitTriggerThisUnitWithoutUnitContextFails() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        val triggered = UniqueTriggerActivation.triggerUnique(
            Unique("Instantly provides [4] [${variable.name}] on [this unit]"), f.civInfo)
        Assert.assertFalse("'this unit' without a unit context must not activate", triggered)
        Assert.assertEquals(0, f.meleeUnit.getVariable(variable.name))
    }

    @Test
    fun testUnitTriggerRespectsClamp() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        variable.max = 10
        UniqueTriggerActivation.triggerUnique(Unique("Instantly provides [50] [${variable.name}] on [this unit]"), f.meleeUnit)
        Assert.assertEquals(10, f.meleeUnit.getVariable(variable.name))
    }

    //endregion

    //region Countable resolution

    @Test
    fun testUnitVariableCountableInUnitContext() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        f.meleeUnit.setVariable(variable.name, 7)
        val conditional = Unique("<when number of [${variable.name}] is more than [5]>").modifiers.first()
        val unitContext = GameContext(f.meleeUnit)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, unitContext))
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
    fun testUnitVariableInCivConditionalIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        Assert.assertTrue("unit variable must not validate in the civ conditional",
            checkErrors(f.game, "when above [5] [${variable.name}]"))
    }

    @Test
    fun testUnitVariableInCityConditionalIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        Assert.assertTrue("unit variable must not validate in the city conditional",
            checkErrors(f.game, "when above [5] [${variable.name}] [in this city]"))
    }

    @Test
    fun testUnitVariableInGlobalConditionalIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        Assert.assertTrue("unit variable must not validate in the global conditional",
            checkErrors(f.game, "when above [5] [${variable.name}] globally"))
    }

    @Test
    fun testCivVariableInUnitConditionalIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Civ)
        Assert.assertTrue("civ variable must not validate in the unit conditional",
            checkErrors(f.game, "when above [5] [${variable.name}] on [this unit]"))
    }

    @Test
    fun testCityVariableInUnitConditionalIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.City)
        Assert.assertTrue("city variable must not validate in the unit conditional",
            checkErrors(f.game, "when above [5] [${variable.name}] on [this unit]"))
    }

    @Test
    fun testUnitVariableInGenericTriggerIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        Assert.assertTrue("unit variable must not validate in the generic civ trigger",
            checkErrors(f.game, "Instantly provides [4] [${variable.name}]"))
    }

    @Test
    fun testCivVariableInUnitTriggerIsAnError() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 0, scope = VariableScope.Civ)
        Assert.assertTrue("civ variable must not validate in the unit trigger",
            checkErrors(f.game, "Instantly provides [4] [${variable.name}] on [this unit]"))
    }

    @Test
    fun testCorrectScopeCombinationsPass() {
        val f = Fixture()
        val unitVariable = f.game.createVariable(default = 0, scope = VariableScope.Unit)
        val cityVariable = f.game.createVariable(default = 0, scope = VariableScope.City)
        val civVariable = f.game.createVariable(default = 0, scope = VariableScope.Civ)
        val globalVariable = f.game.createVariable(default = 0, scope = VariableScope.Global)

        Assert.assertFalse(checkErrors(f.game, "when above [5] [${unitVariable.name}] on [this unit]"))
        Assert.assertFalse(checkErrors(f.game, "when above [5] [${unitVariable.name}] on [Melee]"))
        Assert.assertFalse(checkErrors(f.game, "when between [2] and [4] [${unitVariable.name}] on [this unit]"))
        Assert.assertFalse(checkErrors(f.game, "Instantly provides [4] [${unitVariable.name}] on [this unit]"))
        Assert.assertFalse(checkErrors(f.game, "Instantly consumes [4] [${unitVariable.name}] on [Melee]"))
        Assert.assertFalse(checkErrors(f.game, "Instantly gain [4] [${unitVariable.name}] on [this unit]"))
        Assert.assertFalse(checkErrors(f.game, "Set [${unitVariable.name}] to [5] on [this unit]"))
        // sanity: the other three channels still pass with their own variables
        Assert.assertFalse(checkErrors(f.game, "when above [5] [${civVariable.name}]"))
        Assert.assertFalse(checkErrors(f.game, "when above [5] [${cityVariable.name}] [in this city]"))
        Assert.assertFalse(checkErrors(f.game, "when above [5] [${globalVariable.name}] globally"))
    }

    //endregion
}
