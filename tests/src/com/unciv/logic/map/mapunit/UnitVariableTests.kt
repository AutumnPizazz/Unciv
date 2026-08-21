package com.unciv.logic.map.mapunit

import com.unciv.json.json
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.tile.Tile
import com.unciv.models.ruleset.VariableScope
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/** Coverage for the unit variable scope: storage, fallback, clamp, uniqueTo isolation,
 *  clone, serialization and GameContext routing. */
@RunWith(BaseTestRunner::class)
class UnitVariableTests {
    private val game = TestGame().apply { makeHexagonalMap(3) }
    private val civInfo = game.addCiv()

    private fun addUnit(civ: Civilization = civInfo, tile: Tile = game.getTile(1, 0)): MapUnit =
        game.addDefaultMeleeUnitWithUniques(civ, tile)

    @Test
    fun testUnitVariableStoreAndFallback() {
        val variable = game.createVariable(default = 5, scope = VariableScope.Unit)
        val unit = addUnit()

        Assert.assertEquals(5, unit.getVariable(variable.name)) // falls back to ruleset default

        unit.setVariable(variable.name, 10)
        Assert.assertEquals(10, unit.getVariable(variable.name))

        unit.addVariable(variable.name, 3)
        Assert.assertEquals(13, unit.getVariable(variable.name))
    }

    @Test
    fun testUnitVariablesArePerUnit() {
        val variable = game.createVariable(default = 0, scope = VariableScope.Unit)
        val unit1 = addUnit()
        val unit2 = addUnit(tile = game.getTile(2, 0))

        unit1.setVariable(variable.name, 7)
        Assert.assertEquals(7, unit1.getVariable(variable.name))
        Assert.assertEquals(0, unit2.getVariable(variable.name))
    }

    @Test
    fun testUniqueToRestrictsUnitVariables() {
        val otherCiv = game.addCiv()
        val unit = addUnit()
        val otherUnit = addUnit(otherCiv, game.getTile(2, 0))
        val uniqueTo = civInfo.nation.name

        val variable = game.createVariable(default = 0, scope = VariableScope.Unit).apply { this.uniqueTo = uniqueTo }
        unit.setVariable(variable.name, 7)
        otherUnit.setVariable(variable.name, 9)
        Assert.assertEquals(7, unit.getVariable(variable.name))
        Assert.assertEquals(0, otherUnit.getVariable(variable.name))
    }

    @Test
    fun testUnitVariableClamp() {
        val variable = game.createVariable(default = 0, scope = VariableScope.Unit)
        variable.min = 0
        variable.max = 100
        val unit = addUnit()

        unit.setVariable(variable.name, 200)
        Assert.assertEquals(100, unit.getVariable(variable.name))

        unit.addVariable(variable.name, 50)
        Assert.assertEquals(100, unit.getVariable(variable.name)) // stays clamped

        unit.addVariable(variable.name, -500)
        Assert.assertEquals(0, unit.getVariable(variable.name))
    }

    @Test
    fun testUnitVariablesSurviveClone() {
        val variable = game.createVariable(default = 1, scope = VariableScope.Unit)
        val unit = addUnit()
        unit.setVariable(variable.name, 42)

        val cloned = unit.clone()
        Assert.assertEquals(42, cloned.getVariable(variable.name))
    }

    @Test
    fun testUnitVariableSerializationRoundTrip() {
        val variable = game.createVariable(default = 5, scope = VariableScope.Unit)
        val unit = addUnit()
        unit.setVariable(variable.name, 42)

        val jsonText = json().toJson(unit)
        Assert.assertTrue("\"variables\":{\"${variable.name}\":42}" in jsonText)

        val roundTripped = json().fromJson(MapUnit::class.java, jsonText)
        roundTripped.civ = civInfo
        Assert.assertEquals(42, roundTripped.getVariable(variable.name))
    }

    @Test
    fun testOldUnitSaveWithoutVariablesFallsBackToDefault() {
        val variable = game.createVariable(default = 7, scope = VariableScope.Unit)
        val unit = addUnit()
        unit.setVariable(variable.name, 42)

        val jsonText = json().toJson(unit)
        val withoutVariables = Regex("\"variables\":\\{[^}]*\\},?").replaceFirst(jsonText, "")
        Assert.assertFalse("precondition: variables field removed, json: $jsonText", "\"variables\"" in withoutVariables)

        val roundTripped = json().fromJson(MapUnit::class.java, withoutVariables)
        roundTripped.civ = civInfo
        Assert.assertEquals(7, roundTripped.getVariable(variable.name))
    }

    @Test
    fun testGameContextUnitRouting() {
        val variable = game.createVariable(default = 5, scope = VariableScope.Unit)
        val unit = addUnit()
        unit.setVariable(variable.name, 42)

        // with a relevant unit the per-unit value is used
        Assert.assertEquals(42, GameContext(unit).getVariableAmount(variable.name))
        // with only a civilization context (no unit) the ruleset default is used
        Assert.assertEquals(5, GameContext(civInfo).getVariableAmount(variable.name))
    }

    @Test
    fun testNonUnitScopeVariablesAreRejectedOnUnits() {
        val cityVariable = game.createVariable(default = 3, scope = VariableScope.City)
        val civVariable = game.createVariable(default = 4, scope = VariableScope.Civ)
        val globalVariable = game.createVariable(default = 6, scope = VariableScope.Global)
        val unit = addUnit()

        unit.setVariable(cityVariable.name, 9)
        unit.setVariable(civVariable.name, 8)
        unit.setVariable(globalVariable.name, 7)
        Assert.assertEquals(0, unit.getVariable(cityVariable.name))
        Assert.assertEquals(0, unit.getVariable(civVariable.name))
        Assert.assertEquals(0, unit.getVariable(globalVariable.name))
    }
}
