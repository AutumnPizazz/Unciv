package com.unciv.logic.civilization

import com.unciv.models.ruleset.unique.Conditionals
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueTriggerActivation
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(BaseTestRunner::class)
class VariableTests {
    private val game = TestGame().apply { makeHexagonalMap(2) }
    private val civInfo = game.addCiv()

    @Test
    fun testVariableDefaultFallsBack() {
        val variable = game.createVariable(default = 5)
        Assert.assertEquals(5, civInfo.getVariable(variable.name))
        Assert.assertEquals(5, civInfo.getVariable(variable.name)) // repeated read is stable
    }

    @Test
    fun testSetAndAddVariable() {
        val variable = game.createVariable(default = 0)
        civInfo.setVariable(variable.name, 10)
        Assert.assertEquals(10, civInfo.getVariable(variable.name))

        civInfo.addVariable(variable.name, 3)
        Assert.assertEquals(13, civInfo.getVariable(variable.name))

        // Explicit zero stays zero and must NOT fall back to the ruleset default
        civInfo.setVariable(variable.name, 0)
        Assert.assertEquals(0, civInfo.getVariable(variable.name))
    }

    @Test
    fun testNegativeVariablesAllowed() {
        val variable = game.createVariable(default = 0)
        civInfo.addVariable(variable.name, -4)
        Assert.assertEquals(-4, civInfo.getVariable(variable.name))
    }

    @Test
    fun testGetGameResourceResolvesVariable() {
        val variable = game.createVariable(default = 7)
        val resource = game.ruleset.getGameResource(variable.name)
        Assert.assertNotNull("Variable name must resolve through getGameResource", resource)
        civInfo.setVariable(variable.name, 4)
        Assert.assertEquals(4, civInfo.getGameResource(resource!!))
    }

    @Test
    fun testAddGameResourceVariableBranch() {
        val variable = game.createVariable(default = 0)
        val resource = game.ruleset.getGameResource(variable.name)!!
        civInfo.addGameResource(resource, 9)
        Assert.assertEquals(9, civInfo.getVariable(variable.name))
    }

    @Test
    fun testConditionalWhenAboveVariable() {
        val variable = game.createVariable(default = 0)
        val conditional = Unique("<when above [5] [${variable.name}]>").modifiers.first()
        val gameContext = GameContext(civInfo = civInfo)

        civInfo.setVariable(variable.name, 3)
        Assert.assertFalse(Conditionals.conditionalApplies(null, conditional, gameContext))

        civInfo.setVariable(variable.name, 8)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, gameContext))
    }

    @Test
    fun testConditionalWhenBetweenVariable() {
        val variable = game.createVariable(default = 0)
        val conditional = Unique("<when between [2] and [4] [${variable.name}]>").modifiers.first()
        val gameContext = GameContext(civInfo = civInfo)

        civInfo.setVariable(variable.name, 1)
        Assert.assertFalse(Conditionals.conditionalApplies(null, conditional, gameContext))
        civInfo.setVariable(variable.name, 3)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, gameContext))
        civInfo.setVariable(variable.name, 5)
        Assert.assertFalse(Conditionals.conditionalApplies(null, conditional, gameContext))
    }

    @Test
    fun testOneTimeProvideAndConsumeVariable() {
        val variable = game.createVariable(default = 0)

        UniqueTriggerActivation.triggerUnique(Unique("Instantly provides [4] [${variable.name}]"), civInfo)
        Assert.assertEquals(4, civInfo.getVariable(variable.name))

        UniqueTriggerActivation.triggerUnique(Unique("Instantly consumes [1] [${variable.name}]"), civInfo)
        Assert.assertEquals(3, civInfo.getVariable(variable.name))

        // Consume may go negative
        UniqueTriggerActivation.triggerUnique(Unique("Instantly consumes [10] [${variable.name}]"), civInfo)
        Assert.assertEquals(-7, civInfo.getVariable(variable.name))
    }

    @Test
    fun testOneTimeGainVariable() {
        val variable = game.createVariable(default = 0)
        UniqueTriggerActivation.triggerUnique(Unique("Instantly gain [6] [${variable.name}]"), civInfo)
        Assert.assertEquals(6, civInfo.getVariable(variable.name))
    }

    @Test
    fun testVariablesSurviveClone() {
        val variable = game.createVariable(default = 1)
        civInfo.setVariable(variable.name, 42)
        val clonedCiv = civInfo.clone()
        Assert.assertEquals(42, clonedCiv.getVariable(variable.name))
    }

    @Test
    fun testOldSaveWithoutVariablesFallsBackToDefault() {
        val variable = game.createVariable(default = 3)
        // Simulate an old save: no record in the map at all
        civInfo.variables.clear()
        Assert.assertEquals(3, civInfo.getVariable(variable.name))
    }

    @Test
    fun testUnknownVariableNameReturnsZero() {
        Assert.assertEquals(0, civInfo.getVariable("NoSuchVariable"))
    }

    @Test
    fun testValidatorAcceptsVariableInConditional() {
        val variable = game.createVariable(default = 0)
        val unique = Unique("when above [5] [${variable.name}]")
        val errors = com.unciv.models.ruleset.validation.UniqueValidator(game.ruleset)
            .checkUnique(unique, false, null, com.unciv.models.ruleset.validation.UniqueValidator.extensionModParameterSeverities)
        Assert.assertFalse("Variable name must pass parameter validation", errors.isNotOK())
    }

    @Test
    fun testValidatorAcceptsVariableInProvides() {
        val variable = game.createVariable(default = 0)
        val unique = Unique("Instantly provides [4] [${variable.name}]")
        val errors = com.unciv.models.ruleset.validation.UniqueValidator(game.ruleset)
            .checkUnique(unique, false, null, com.unciv.models.ruleset.validation.UniqueValidator.extensionModParameterSeverities)
        Assert.assertFalse("Variable name must pass parameter validation in Provides", errors.isNotOK())
    }
}
