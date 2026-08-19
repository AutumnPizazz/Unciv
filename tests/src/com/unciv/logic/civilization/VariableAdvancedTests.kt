package com.unciv.logic.civilization

import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unique.Conditionals
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueTriggerActivation
import com.unciv.models.ruleset.validation.RulesetValidator
import com.unciv.models.stats.Stat
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/** Deep coverage of the mod-defined variable system: serialization, condition edges, city context,
 *  notifications, ruleset merging and validation. */
@RunWith(BaseTestRunner::class)
class VariableAdvancedTests {
    private val game = TestGame().apply { makeHexagonalMap(3) }
    private val civInfo = game.addCiv()
    private val gameContext = GameContext(civInfo = civInfo)

    //region Serialization

    @Test
    fun testVariablesSurviveCivilizationJsonRoundTrip() {
        val variable = game.createVariable(default = 5)
        civInfo.setVariable(variable.name, 42)
        civInfo.setVariable("UndefinedVariable", 9)

        val roundTripped = json().fromJson(Civilization::class.java, json().toJson(civInfo))
        roundTripped.gameInfo = game.gameInfo

        Assert.assertEquals(42, roundTripped.getVariable(variable.name))
        Assert.assertEquals(9, roundTripped.getVariable("UndefinedVariable"))
    }

    @Test
    fun testVariablesSurviveGameInfoJsonRoundTrip() {
        val variable = game.createVariable(default = 5)
        civInfo.setVariable(variable.name, 42)
        val jsonText = json().toJson(game.gameInfo)
        Assert.assertTrue("variables must be part of the save data", "\"variables\"" in jsonText)

        val roundTripped = json().fromJson(GameInfo::class.java, jsonText)
        // NOTE: setTransients() would reload the ruleset from RulesetCache and lose test-injected nations,
        // so we wire the ruleset manually - variables live in the serialized civilizations either way.
        roundTripped.ruleset = game.gameInfo.ruleset
        val civ = roundTripped.civilizations.first { it.civName == civInfo.civName }
        civ.gameInfo = roundTripped
        Assert.assertEquals(42, civ.getVariable(variable.name))
    }

    @Test
    fun testVariablesFieldSerializesAsNameToIntMap() {
        val variable = game.createVariable(default = 0)
        civInfo.setVariable(variable.name, 42)
        val jsonText = json().toJson(civInfo)
        Assert.assertTrue("variables must serialize as {name: value}",
            "\"variables\":{\"${variable.name}\":42}" in jsonText)
    }

    @Test
    fun testOldSaveWithoutVariablesFieldFallsBackToDefault() {
        val variable = game.createVariable(default = 7)
        // Craft a Civilization json without any variables field (as pre-feature saves would look)
        val jsonText = json().toJson(civInfo)
        val withoutVariables = Regex("\"variables\":\\{[^}]*\\},").replaceFirst(jsonText, "")
        Assert.assertFalse("precondition: variables field removed", "\"variables\"" in withoutVariables)

        val roundTripped = json().fromJson(Civilization::class.java, withoutVariables)
        roundTripped.gameInfo = game.gameInfo
        Assert.assertEquals(7, roundTripped.getVariable(variable.name))
    }

    //endregion

    //region Conditionals - edges

    @Test
    fun testConditionalWhenBelowVariable() {
        val variable = game.createVariable()
        val conditional = Unique("<when below [5] [${variable.name}]>").modifiers.first()

        civInfo.setVariable(variable.name, 3)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, gameContext))
        civInfo.setVariable(variable.name, 8)
        Assert.assertFalse(Conditionals.conditionalApplies(null, conditional, gameContext))
    }

    @Test
    fun testConditionalWhenAboveVariableExactBoundary() {
        val variable = game.createVariable()
        val conditional = Unique("<when above [5] [${variable.name}]>").modifiers.first()

        // "above" is strict - equal value must NOT apply
        civInfo.setVariable(variable.name, 5)
        Assert.assertFalse(Conditionals.conditionalApplies(null, conditional, gameContext))
        civInfo.setVariable(variable.name, 6)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, gameContext))
    }

    @Test
    fun testConditionalWhenAboveVariableModifiedByGameSpeed() {
        val variable = game.createVariable()
        val unique = Unique("[+10] Strength <when above [5] [${variable.name}]> <(modified by game speed)>")
        val conditional = unique.modifiers.first { it.type == com.unciv.models.ruleset.unique.UniqueType.ConditionalWhenAboveAmountStatResource }

        game.gameInfo.speed.modifier = 2f
        try {
            // threshold becomes 5 * 2 = 10
            civInfo.setVariable(variable.name, 8)
            Assert.assertFalse(Conditionals.conditionalApplies(unique, conditional, gameContext))
            civInfo.setVariable(variable.name, 12)
            Assert.assertTrue(Conditionals.conditionalApplies(unique, conditional, gameContext))
        } finally {
            game.gameInfo.speed.modifier = 1f
        }
    }

    @Test
    fun testConditionalWhenAboveVariableWithCountableAmount() {
        // [amount] accepts Countable expressions, e.g. a threshold tied to the number of cities
        val variable = game.createVariable()
        val conditional = Unique("<when above [Cities] [${variable.name}]>").modifiers.first()

        game.addCity(civInfo, game.getTile(1, 0))
        civInfo.setVariable(variable.name, 0)
        Assert.assertFalse(Conditionals.conditionalApplies(null, conditional, gameContext))
        civInfo.setVariable(variable.name, 2)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, gameContext))
    }

    @Test
    fun testConditionalWhenAboveStatWithCountableAmount() {
        // The stat path of when above must accept Countable amounts too
        val conditional = Unique("<when above [Cities] [Culture]>").modifiers.first()

        game.addCity(civInfo, game.getTile(1, 0))
        civInfo.policies.addCulture(0)
        Assert.assertFalse(Conditionals.conditionalApplies(null, conditional, gameContext))
        civInfo.policies.addCulture(2)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, gameContext))
    }

    @Test
    fun testConditionalPopulationWithCountableAmount() {
        // Population conditionals accept Countable amounts as well (needs a relevant city context)
        val conditional = Unique("<in cities with at least [Cities] [Population]>").modifiers.first()
        val city = game.addCity(civInfo, game.getTile(1, 0)) // city population 1 >= city count 1
        val cityContext = GameContext(civInfo = civInfo, city = city)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, cityContext))
    }

    @Test
    fun testVariableConditionalIgnoresRelevantCityLevel() {
        // Variables are civ-level: a relevant city must still resolve to its civilization's value
        val variable = game.createVariable(default = 0)
        val city = game.addCity(civInfo, game.getTile(1, 0))
        civInfo.setVariable(variable.name, 11)
        val cityContext = GameContext(city = city)
        val conditional = Unique("<when above [10] [${variable.name}]>").modifiers.first()
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, cityContext))
    }

    //endregion

    //region Triggerables - city context & notifications
    @Test
    fun testOneTimeProvideVariableInCityContext() {
        val variable = game.createVariable(default = 0)
        val city = game.addCity(civInfo, game.getTile(1, 0))

        val triggered = UniqueTriggerActivation.triggerUnique(
            Unique("Instantly provides [4] [${variable.name}]"), city)
        Assert.assertTrue("unique must trigger on a city", triggered)
        Assert.assertEquals(4, civInfo.getVariable(variable.name))
    }

    @Test
    fun testOneTimeConsumeVariableInCityContext() {
        val variable = game.createVariable(default = 10)
        val city = game.addCity(civInfo, game.getTile(1, 0))
        civInfo.setVariable(variable.name, 10)

        UniqueTriggerActivation.triggerUnique(
            Unique("Instantly consumes [3] [${variable.name}]"), city)
        Assert.assertEquals(7, civInfo.getVariable(variable.name))
    }

    @Test
    fun testOneTimeProvideVariableCreatesNotification() {
        val variable = game.createVariable(default = 0)
        val humanCiv = game.addCiv(isPlayer = true)
        UniqueTriggerActivation.triggerUnique(
            Unique("Instantly provides [4] [${variable.name}]"),
            humanCiv, notification = "You have gained [4] [${variable.name}]")
        Assert.assertTrue("providing a variable should notify the player",
            humanCiv.notifications.isNotEmpty())
    }

    @Test
    fun testOneTimeProvideUnknownVariableDoesNotTrigger() {
        // A name that is neither a stockpiled resource nor a defined variable must be a no-op
        val triggered = UniqueTriggerActivation.triggerUnique(
            Unique("Instantly provides [4] [NoSuchThing]"), civInfo)
        Assert.assertFalse(triggered)
    }

    @Test
    fun testOneTimeGainVariableModifiedByGameSpeed() {
        val variable = game.createVariable(default = 0)
        game.gameInfo.speed.modifier = 2f
        try {
            UniqueTriggerActivation.triggerUnique(
                Unique("Instantly gain [4] [${variable.name}] <(modified by game speed)>"), civInfo)
            Assert.assertEquals(8, civInfo.getVariable(variable.name))
        } finally {
            game.gameInfo.speed.modifier = 1f
        }
    }

    @Test
    fun testOneTimeGainVariableNegativeAmount() {
        val variable = game.createVariable(default = 5)
        UniqueTriggerActivation.triggerUnique(
            Unique("Instantly gain [-2] [${variable.name}]"), civInfo)
        Assert.assertEquals(3, civInfo.getVariable(variable.name))
    }

    //endregion

    //region Isolation & coexistence

    @Test
    fun testVariablesArePerCivilization() {
        val variable = game.createVariable(default = 0)
        val otherCiv = game.addCiv()

        civInfo.setVariable(variable.name, 10)
        Assert.assertEquals(0, otherCiv.getVariable(variable.name))

        otherCiv.addVariable(variable.name, 5)
        Assert.assertEquals(10, civInfo.getVariable(variable.name))
        Assert.assertEquals(5, otherCiv.getVariable(variable.name))
    }

    @Test
    fun testStatConditionalsStillWorkAlongsideVariables() {
        // Defining variables must not change how stat conditions resolve
        game.createVariable(default = 0)
        val conditional = Unique("<when above [0] [Culture]>").modifiers.first()

        civInfo.policies.addCulture(100)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, gameContext))
    }

    @Test
    fun testResourceConditionalsStillWorkAlongsideVariables() {
        game.createVariable(default = 0)
        val conditional = Unique("<when above [0] [Iron]>").modifiers.first()
        civInfo.gainStockpiledResource(game.ruleset.tileResources["Iron"]!!, 3)
        Assert.assertTrue(Conditionals.conditionalApplies(null, conditional, gameContext))
    }

    @Test
    fun testVariableNameDoesNotCollideWithStatInParsing() {
        // A variable named like a stat is rejected by validation, but parsing must stay deterministic
        // (getGameResource prefers stats over variables)
        val variable = game.createVariable(default = 0)
        variable.name = "Gold"
        val resolved = game.ruleset.getGameResource("Gold")
        Assert.assertTrue("stat must win the resolution order", resolved is com.unciv.models.stats.Stat)
    }

    //endregion

    //region Ruleset merging & defaults

    @Test
    fun testRulesetMergeCombinesVariables() {
        val variable1 = game.createVariable(default = 1)
        val extension = Ruleset().apply { name = "ExtMod" }
        val variable2 = com.unciv.models.ruleset.Variable().apply {
            name = "ExtVar"
            default = 3
        }
        extension.variables["ExtVar"] = variable2

        game.ruleset.add(extension)

        Assert.assertTrue(game.ruleset.variables.containsKey(variable1.name))
        Assert.assertEquals(3, game.ruleset.variables["ExtVar"]!!.default)
        Assert.assertEquals(3, civInfo.getVariable("ExtVar"))
    }

    @Test
    fun testChangingDefaultDoesNotAffectRecordedValue() {
        val variable = game.createVariable(default = 5)
        civInfo.setVariable(variable.name, 42)
        variable.default = 99
        Assert.assertEquals(42, civInfo.getVariable(variable.name))
    }

    @Test
    fun testRemovingVariableFromRulesetKeepsStoredValue() {
        val variable = game.createVariable(default = 5)
        civInfo.setVariable(variable.name, 42)
        game.ruleset.variables.remove(variable.name)
        // No ruleset definition anymore: the recorded value must still win over the (now absent) default
        Assert.assertEquals(42, civInfo.getVariable(variable.name))
    }

    //endregion

    //region Validation

    @Test
    fun testVariableCollidingWithStatNameIsAnError() {
        // Use a clean TestGame: the shared one has a minimal test Nation that makes full validation crash
        val cleanGame = TestGame().apply { makeHexagonalMap(3) }
        cleanGame.createVariable(default = 0).apply { name = Stat.Gold.name }
        val errors = RulesetValidator.create(cleanGame.ruleset).getErrorList()
        Assert.assertTrue("Variable colliding with a stat must be reported",
            errors.any { "collides with a stat name" in it.text })
    }

    @Test
    fun testVariableCollidingWithResourceNameIsAnError() {
        val cleanGame = TestGame().apply { makeHexagonalMap(3) }
        cleanGame.createVariable(default = 0).apply { name = "Iron" }
        val errors = RulesetValidator.create(cleanGame.ruleset).getErrorList()
        Assert.assertTrue("Variable colliding with a tile resource must be reported",
            errors.any { "collides with a tile resource name" in it.text })
    }

    @Test
    fun testCleanRulesetWithoutVariableNameCollisionsHasNoVariableErrors() {
        val cleanGame = TestGame().apply { makeHexagonalMap(3) }
        cleanGame.createVariable(default = 0).apply { name = "WarWeariness" }
        val errors = RulesetValidator.create(cleanGame.ruleset).getErrorList()
        Assert.assertFalse("A non-colliding variable must not be reported",
            errors.any { "collides with a stat name" in it.text || "collides with a tile resource name" in it.text })
    }

    //endregion
}
