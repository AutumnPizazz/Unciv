package com.unciv.logic.map.mapunit

import com.unciv.logic.civilization.Civilization
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/** Coverage for the religious-strength-loss migration to the unit variable `ReligiousStrengthLost`:
 *  property forwarding, legacy migration, per-turn loss in foreign territory and the
 *  base-ruleset destruction combination. */
@RunWith(BaseTestRunner::class)
class ReligionMigrationTests {

    private class Fixture {
        val game = TestGame().apply { makeHexagonalMap(3) }
        val civInfo = game.addCiv()
        val unit = game.addDefaultMeleeUnitWithUniques(civInfo, game.getTile(1, 0))
    }

    @Test
    fun testReligiousStrengthLostIsBackedByTheVariable() {
        val f = Fixture()
        f.unit.religiousStrengthLost = 100
        Assert.assertEquals(100, f.unit.getVariable(MapUnit.religiousStrengthLostVariableName))
        Assert.assertEquals(100, f.unit.religiousStrengthLost)
    }

    @Test
    fun testVariableWriteReflectedInProperty() {
        val f = Fixture()
        f.unit.setVariable(MapUnit.religiousStrengthLostVariableName, 75)
        Assert.assertEquals(75, f.unit.religiousStrengthLost)
    }

    @Test
    fun testClampedToVariableMin() {
        val f = Fixture()
        f.unit.religiousStrengthLost = -5
        Assert.assertEquals(0, f.unit.religiousStrengthLost)
    }

    @Test
    fun testLegacyFieldServesBeforeMigration() {
        val f = Fixture()
        // Simulate an old save: the field carries the value while the unit's civ is not yet set
        val unit = MapUnit()
        unit.religiousStrengthLost = 60
        unit.civ = f.civInfo
        Assert.assertEquals("legacy field keeps serving", 60, unit.religiousStrengthLost)
        // migration writes the variable through the property and zeroes the field
        unit.religiousStrengthLost = unit.religiousStrengthLost
        Assert.assertEquals(60, unit.getVariable(MapUnit.religiousStrengthLostVariableName))
        Assert.assertEquals(60, unit.religiousStrengthLost)
    }

    @Test
    fun testCloneAndSerializationPreserveValue() {
        val f = Fixture()
        f.unit.religiousStrengthLost = 300
        val cloned = f.unit.clone()
        Assert.assertEquals(300, cloned.getVariable(MapUnit.religiousStrengthLostVariableName))
        Assert.assertEquals(300, cloned.religiousStrengthLost)
    }

    //region Missionary end-to-end behaviour (base-ruleset combination)

    private fun missionaryInForeignTerritory(): Triple<TestGame, Civilization, MapUnit> {
        val game = TestGame().apply { makeHexagonalMap(3) }
        val civInfo = game.addCiv()
        val otherCiv = game.addCiv()
        val otherCity = game.addCity(otherCiv, game.getTile(2, 0))
        val foreignTile = game.getTile(2, 1)
        game.addTileToCity(otherCity, foreignTile)
        foreignTile.setOwningCity(otherCity)
        val missionary = game.addUnit("Missionary", civInfo, foreignTile)
        return Triple(game, civInfo, missionary)
    }

    @Test
    fun testMissionaryLosesStrengthInForeignTerritory() {
        val (_, _, missionary) = missionaryInForeignTerritory()
        UnitTurnManager(missionary).endTurn()
        Assert.assertEquals("missionary loses 250 per turn in foreign territory",
            250, missionary.getVariable(MapUnit.religiousStrengthLostVariableName))
        Assert.assertFalse("one turn of loss must not destroy the missionary", missionary.isDestroyed)
    }

    @Test
    fun testMissionarySurvivesBelowThresholdTurnAndDestroysAfter() {
        val (_, _, missionary) = missionaryInForeignTerritory()
        missionary.setVariable(MapUnit.religiousStrengthLostVariableName, 750)
        UnitTurnManager(missionary).endTurn()
        Assert.assertEquals("750 + 250 = 1000, checked at the *start* of the next turn",
            1000, missionary.getVariable(MapUnit.religiousStrengthLostVariableName))
        Assert.assertFalse("the check runs before this turn's loss", missionary.isDestroyed)

        UnitTurnManager(missionary).endTurn()
        Assert.assertTrue("1000 >= 1000 destroys the missionary at the next turn start",
            missionary.isDestroyed)
    }

    @Test
    fun testMissionaryDestroyedWhenStrengthFullyLost() {
        val (_, _, missionary) = missionaryInForeignTerritory()
        missionary.setVariable(MapUnit.religiousStrengthLostVariableName, 1000)
        UnitTurnManager(missionary).endTurn()
        Assert.assertTrue(missionary.isDestroyed)
    }

    //endregion
}
