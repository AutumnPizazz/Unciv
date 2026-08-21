package com.unciv.logic.map.mapunit

import com.unciv.models.ruleset.Variable
import com.unciv.models.ruleset.VariableScope
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.validation.UniqueValidator
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/** Coverage for the unit-scope per-turn variable yields: base yields, stacking across sources,
 *  percentage bonuses, clamping and cross-scope validation. */
@RunWith(BaseTestRunner::class)
class UnitVariableYieldTests {

    private class Fixture {
        val game = TestGame().apply { makeHexagonalMap(3) }
        val civInfo = game.addCiv()
        val mana: Variable
        val unit: MapUnit

        init {
            mana = game.createVariable(default = 0, scope = VariableScope.Unit)
            unit = game.addDefaultMeleeUnitWithUniques(civInfo, game.getTile(1, 0))
        }

        fun addUnitWithYields(tileX: Int, vararg uniques: String): MapUnit =
            game.addDefaultMeleeUnitWithUniques(civInfo, game.getTile(tileX, 0), *uniques)
    }

    private fun endTurn(unit: MapUnit) = UnitTurnManager(unit).endTurn()

    @Test
    fun testUnitYieldSettlesPerTurn() {
        val f = Fixture()
        val withYield = f.addUnitWithYields(2, "[+2] [${f.mana.name}] per turn")

        endTurn(withYield)
        Assert.assertEquals(2, withYield.getVariable(f.mana.name))
        Assert.assertEquals("units without a yield unique are unaffected", 0, f.unit.getVariable(f.mana.name))
    }

    @Test
    fun testUnitYieldAccumulatesAcrossTurns() {
        val f = Fixture()
        val unit = f.addUnitWithYields(2, "[+1] [${f.mana.name}] per turn")

        endTurn(unit)
        endTurn(unit)
        Assert.assertEquals(2, unit.getVariable(f.mana.name))
    }

    @Test
    fun testUnitYieldStacksAcrossSources() {
        val f = Fixture()
        val withYield = f.addUnitWithYields(2, "[+1] [${f.mana.name}] per turn")
        val promotion = f.game.createUnitPromotion("[+1] [${f.mana.name}] per turn")
        withYield.promotions.addPromotion(promotion.name, isFree = true)

        endTurn(withYield)
        Assert.assertEquals("base unit and promotion yields stack", 2, withYield.getVariable(f.mana.name))
    }

    @Test
    fun testUnitYieldPercentBonus() {
        val f = Fixture()
        val unit = f.addUnitWithYields(2,
            "[+2] [${f.mana.name}] per turn", "[+50]% [${f.mana.name}] per turn")

        endTurn(unit)
        Assert.assertEquals("2 * (1 + 50%) rounds to 3", 3, unit.getVariable(f.mana.name))
    }

    @Test
    fun testUnitYieldPercentBonusesStackAdditively() {
        val f = Fixture()
        val unit = f.addUnitWithYields(2,
            "[+2] [${f.mana.name}] per turn",
            "[+50]% [${f.mana.name}] per turn",
            "[+25]% [${f.mana.name}] per turn")

        endTurn(unit)
        Assert.assertEquals("2 * (1 + 75%) rounds to 4", 4, unit.getVariable(f.mana.name))
    }

    @Test
    fun testUnitYieldRespectsClamp() {
        val f = Fixture()
        f.mana.max = 5
        val unit = f.addUnitWithYields(2, "[+10] [${f.mana.name}] per turn")

        endTurn(unit)
        Assert.assertEquals(5, unit.getVariable(f.mana.name))
    }

    @Test
    fun testUnitYieldDoesNotSettleNonUnitScope() {
        val f = Fixture()
        val civVar = f.game.createVariable(default = 0, scope = VariableScope.Civ)
        val unit = f.addUnitWithYields(2, "[+1] [${civVar.name}] per turn")

        endTurn(unit)
        Assert.assertEquals(0, f.civInfo.getVariable(civVar.name))
    }

    @Test
    fun testUnitYieldValidation() {
        val f = Fixture()
        val civVar = f.game.createVariable(default = 0, scope = VariableScope.Civ)

        fun checkErrors(uniqueText: String): Boolean {
            val unique = Unique(uniqueText)
            val errors = UniqueValidator(f.game.ruleset)
                .checkUnique(unique, false, null, UniqueValidator.allParameterSeverities)
            return errors.isNotOK()
        }

        Assert.assertFalse(checkErrors("[+2] [${f.mana.name}] per turn"))
        Assert.assertFalse(checkErrors("[+50]% [${f.mana.name}] per turn"))
        Assert.assertTrue("non-unit variable must not validate in the unit yield",
            checkErrors("[+2] [${civVar.name}] per turn"))
    }
}
