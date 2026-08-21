package com.unciv.logic.map.mapunit

import com.unciv.json.json
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/** Coverage for the XP migration to the unit-scope variable `Experience`: property forwarding,
 *  legacy save migration, clamping and serialization consistency. */
@RunWith(BaseTestRunner::class)
class ExperienceMigrationTests {

    private class Fixture {
        val game = TestGame().apply { makeHexagonalMap(3) }
        val civInfo = game.addCiv()
        val unit = game.addDefaultMeleeUnitWithUniques(civInfo, game.getTile(1, 0))
    }

    @Test
    fun testXPIsBackedByTheExperienceVariable() {
        val f = Fixture()
        f.unit.promotions.XP = 30
        Assert.assertEquals(30, f.unit.getVariable(UnitPromotions.experienceVariableName))
        Assert.assertEquals(30, f.unit.promotions.XP)
    }

    @Test
    fun testXPAdditiveWrites() {
        val f = Fixture()
        f.unit.promotions.XP = 10
        f.unit.promotions.XP += 5
        Assert.assertEquals(15, f.unit.promotions.XP)
        Assert.assertEquals(15, f.unit.getVariable(UnitPromotions.experienceVariableName))
    }

    @Test
    fun testVariableWriteReflectedInXP() {
        val f = Fixture()
        f.unit.setVariable(UnitPromotions.experienceVariableName, 50)
        Assert.assertEquals(50, f.unit.promotions.XP)
    }

    @Test
    fun testXPClampedToVariableMin() {
        val f = Fixture()
        // Experience is defined with min 0 in the base ruleset
        f.unit.promotions.XP = -5
        Assert.assertEquals(0, f.unit.promotions.XP)
        Assert.assertEquals(0, f.unit.getVariable(UnitPromotions.experienceVariableName))
    }

    @Test
    fun testLegacyXPFieldServesBeforeMigration() {
        val f = Fixture()
        val promotions = UnitPromotions()
        promotions.XP = 37 // unit not initialized - lands in the legacy backing field
        promotions.setTransients(f.unit)
        Assert.assertEquals("legacy field keeps serving before migration", 37, promotions.XP)
        // migration writes the variable through the property and zeroes the field
        promotions.XP = promotions.XP
        Assert.assertEquals(37, f.unit.getVariable(UnitPromotions.experienceVariableName))
        Assert.assertEquals(37, promotions.XP)
        // serialization no longer carries the legacy value (XP:0 is a default and gets skipped)
        val promJson = json().toJson(promotions)
        Assert.assertFalse("serialized promotions should not carry XP but was: $promJson", "\"XP\"" in promJson)
    }

    @Test
    fun testOldSaveXPFieldFallsBackWhenNoMigrationRuns() {
        // Simulate an old save: the XP field carries the value and there is no variable record
        val f = Fixture()
        val promotions = UnitPromotions()
        promotions.XP = 25
        promotions.setTransients(f.unit)
        Assert.assertEquals("reads still return the legacy value", 25, promotions.XP)
        // after the first write the variable takes over
        promotions.XP += 5
        Assert.assertEquals(30, promotions.XP)
        Assert.assertEquals(30, f.unit.getVariable(UnitPromotions.experienceVariableName))
    }

    @Test
    fun testXPAndVariablesSerializeTogether() {
        val f = Fixture()
        f.unit.promotions.XP = 42
        val jsonText = json().toJson(f.unit)
        Assert.assertTrue("variables should serialize Experience but was: $jsonText", "\"variables\":{\"Experience\":42}" in jsonText)
        Assert.assertFalse("promotions should not serialize XP (default) but was: $jsonText", "\"XP\"" in jsonText)

        val roundTripped = json().fromJson(MapUnit::class.java, jsonText)
        roundTripped.civ = f.civInfo
        roundTripped.setTransients(f.game.gameInfo.ruleset)
        Assert.assertEquals(42, roundTripped.promotions.XP)
        Assert.assertEquals(42, roundTripped.getVariable(UnitPromotions.experienceVariableName))
    }

    @Test
    fun testOldSaveXPFieldReadsThroughJson() {
        // An old save carries "XP":37 inside promotions; Gdx Json writes the backing field on load
        val promotions = json().fromJson(UnitPromotions::class.java, "{\"XP\":37}")
        Assert.assertEquals(37, promotions.XP) // unit not initialized - legacy field serves
        val f = Fixture()
        promotions.setTransients(f.unit)
        Assert.assertEquals(37, promotions.XP)
        // the migration write moves it into the variable
        promotions.XP = promotions.XP
        Assert.assertEquals(37, f.unit.getVariable(UnitPromotions.experienceVariableName))
        Assert.assertEquals(37, promotions.XP)
    }

    @Test
    fun testClonePreservesXP() {
        val f = Fixture()
        f.unit.promotions.XP = 33
        val cloned = f.unit.clone()
        cloned.setTransients(f.game.gameInfo.ruleset)
        Assert.assertEquals(33, cloned.promotions.XP)
        Assert.assertEquals(33, cloned.getVariable(UnitPromotions.experienceVariableName))
    }
}
