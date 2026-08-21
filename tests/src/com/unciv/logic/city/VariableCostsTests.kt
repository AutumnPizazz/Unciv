package com.unciv.logic.city

import com.unciv.logic.automation.civilization.ReligionAutomation
import com.unciv.models.ruleset.BeliefType
import com.unciv.models.ruleset.VariableScope
import com.unciv.models.ruleset.unique.Unique
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import com.unciv.ui.screens.worldscreen.unit.actions.UnitActionModifiers
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/** Coverage for variable costs and purchases (module C): `May buy ... for [X] [MyVar]` with scope-aware
 *  deduction, and unit-action stockpile costs `costs [X] [MyVar]`. */
@RunWith(BaseTestRunner::class)
class VariableCostsTests {

    private class Fixture {
        val game = TestGame().apply { makeHexagonalMap(3) }
        val civInfo = game.addCiv()
        val city = game.addCity(civInfo, game.getTile(1, 0))
    }

    private fun addBuyBuilding(f: Fixture, vararg uniques: String) {
        val building = f.game.createBuilding(*uniques)
        f.city.cityConstructions.addBuilding(building)
    }

    //region Purchases with variables

    @Test
    fun testCanBePurchasedWithVariable() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 100, scope = VariableScope.Civ)
        addBuyBuilding(f, "May buy [All] units for [10] [${variable.name}] [in this city]")

        val unit = f.game.createBaseUnit()
        Assert.assertTrue(unit.canBePurchasedWithVariable(f.city, variable.name))
        Assert.assertFalse("Faith purchase must not be allowed without a matching unique",
            unit.canBePurchasedWithStat(f.city, com.unciv.models.stats.Stat.Faith))
    }

    @Test
    fun testVariableBuyCost() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 100, scope = VariableScope.Civ)
        addBuyBuilding(f, "May buy [All] units for [10] [${variable.name}] [in this city]")

        val unit = f.game.createBaseUnit()
        Assert.assertEquals(10, unit.getVariableBuyCost(f.city, variable.name))
    }

    @Test
    fun testPurchaseAllowedWhenBalanceSufficient() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 100, scope = VariableScope.Civ)
        addBuyBuilding(f, "May buy [All] units for [10] [${variable.name}] [in this city]")

        val unit = f.game.createBaseUnit()
        Assert.assertTrue(f.city.cityConstructions.isConstructionPurchaseAllowed(unit, variable.name, 10))
    }

    @Test
    fun testPurchaseNotAllowedWhenBalanceInsufficient() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 5, scope = VariableScope.Civ)
        addBuyBuilding(f, "May buy [All] units for [10] [${variable.name}] [in this city]")

        val unit = f.game.createBaseUnit()
        Assert.assertFalse(f.city.cityConstructions.isConstructionPurchaseAllowed(unit, variable.name, 10))
    }

    @Test
    fun testPurchaseCannotPartiallySpendPastVariableMinimum() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 10, scope = VariableScope.Civ)
        variable.min = 5
        addBuyBuilding(f, "May buy [All] units for [10] [${variable.name}] [in this city]")

        val unit = f.game.createBaseUnit()
        Assert.assertFalse(f.city.cityConstructions.isConstructionPurchaseAllowed(unit, variable.name, 10))
    }

    @Test
    fun testPurchaseDeductsFromCivScope() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 100, scope = VariableScope.Civ)
        addBuyBuilding(f, "May buy [All] units for [10] [${variable.name}] [in this city]")

        val unit = f.game.createBaseUnit()
        val bought = f.city.cityConstructions.purchaseConstruction(unit, -1, automatic = false, variableName = variable.name)
        Assert.assertTrue(bought)
        Assert.assertEquals(90, f.civInfo.getVariable(variable.name))
    }

    @Test
    fun testPurchaseDeductsFromCityScope() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 100, scope = VariableScope.City)
        addBuyBuilding(f, "May buy [All] units for [10] [${variable.name}] [in this city]")

        val unit = f.game.createBaseUnit()
        val bought = f.city.cityConstructions.purchaseConstruction(unit, -1, automatic = false, variableName = variable.name)
        Assert.assertTrue(bought)
        Assert.assertEquals(90, f.city.getVariable(variable.name))
    }

    @Test
    fun testPurchaseDeductsFromGlobalScope() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 100, scope = VariableScope.Global)
        addBuyBuilding(f, "May buy [All] units for [10] [${variable.name}] [in this city]")

        val unit = f.game.createBaseUnit()
        val bought = f.city.cityConstructions.purchaseConstruction(unit, -1, automatic = false, variableName = variable.name)
        Assert.assertTrue(bought)
        Assert.assertEquals(90, f.game.gameInfo.getVariable(variable.name))
    }

    @Test
    fun testBuildingMayBuyVariableCostAndPurchase() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 100, scope = VariableScope.Civ)
        addBuyBuilding(f, "May buy [All] buildings for [10] [${variable.name}] [in this city]")

        val building = f.game.createBuilding()
        Assert.assertTrue(building.canBePurchasedWithVariable(f.city, variable.name))
        Assert.assertEquals(10, building.getVariableBuyCost(f.city, variable.name))
        Assert.assertTrue(f.city.cityConstructions.isConstructionPurchaseAllowed(building, variable.name, 10))

        Assert.assertTrue(f.city.cityConstructions.purchaseConstruction(building, -1, automatic = false, variableName = variable.name))
        Assert.assertTrue(f.city.cityConstructions.isBuilt(building.name))
        Assert.assertEquals(90, f.civInfo.getVariable(variable.name))
    }

    @Test
    fun testConstructionSpecificVariablePurchaseUnique() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 100, scope = VariableScope.City)
        val building = f.game.createBuilding("Can be purchased for [20] [${variable.name}] [in this city]")

        Assert.assertTrue(building.canBePurchasedWithVariable(f.city, variable.name))
        Assert.assertEquals(20, building.getVariableBuyCost(f.city, variable.name))
        Assert.assertTrue(f.city.cityConstructions.isConstructionPurchaseAllowed(building, variable.name, 20))
    }

    @Test
    fun testBuildingVariablePurchaseDiscount() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 100, scope = VariableScope.Civ)
        addBuyBuilding(f,
            "May buy [All] buildings for [100] [${variable.name}] [in this city]",
            "[${variable.name}] cost of purchasing [All] buildings [-50]%")

        val building = f.game.createBuilding()
        Assert.assertEquals(50, building.getVariableBuyCost(f.city, variable.name))
    }



    @Test
    fun testVariableBuyCostsAreNotRoundedToTens() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 100, scope = VariableScope.Civ)
        addBuyBuilding(f,
            "May buy [All] units for [5] [${variable.name}] [in this city]",
            "May buy [All] buildings for [5] [${variable.name}] [in this city]")

        Assert.assertEquals(5, f.game.createBaseUnit().getVariableBuyCost(f.city, variable.name))
        Assert.assertEquals(5, f.game.createBuilding().getVariableBuyCost(f.city, variable.name))
    }

    @Test
    fun testUnitActionVariableCostBlocksAndCharges() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 10, scope = VariableScope.Civ)
        val unit = f.game.addUnit("Warrior", f.civInfo, f.game.getTile(1, 0))
        val action = Unique("Can transform to [Warrior] <costs [5] [${variable.name}]>")

        Assert.assertTrue(UnitActionModifiers.canActivateSideEffects(unit, action))
        UnitActionModifiers.activateSideEffects(unit, action)
        Assert.assertEquals(5, f.civInfo.getVariable(variable.name))

        f.civInfo.setVariable(variable.name, 4)
        Assert.assertFalse(UnitActionModifiers.canActivateSideEffects(unit, action))
    }

    @Test
    fun testUnitActionVariableCostRespectsClamp() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 10, scope = VariableScope.Civ)
        variable.min = 0
        f.civInfo.addVariable(variable.name, -5)
        Assert.assertEquals(5, f.civInfo.getVariable(variable.name))
        f.civInfo.addVariable(variable.name, -100)
        Assert.assertEquals(0, f.civInfo.getVariable(variable.name))
    }

    @Test
    fun testUnitActionVariableCostCannotPartiallySpendPastMinimum() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 10, scope = VariableScope.Civ)
        variable.min = 5
        val unit = f.game.addUnit("Warrior", f.civInfo, f.game.getTile(1, 0))
        val action = Unique("Can transform to [Warrior] <costs [10] [${variable.name}]>")

        Assert.assertFalse(UnitActionModifiers.canActivateSideEffects(unit, action))
    }

    @Test
    fun testReligionAiCanRateVariablePurchaseBelief() {
        val f = Fixture()
        val variable = f.game.createVariable(default = 10, scope = VariableScope.Civ)
        val belief = f.game.createBelief(BeliefType.Founder,
            "May buy [All] units for [10] [${variable.name}] [in all cities]")

        Assert.assertTrue(ReligionAutomation.rateBelief(f.civInfo, belief).isFinite())
    }

    //endregion
}
