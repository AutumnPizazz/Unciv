package com.unciv.logic.city

import com.unciv.UncivGame
import com.unciv.logic.map.HexCoord
import com.unciv.models.ruleset.Building
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the immediate production overflow system
 * ([UniqueType.ProductionOverflowImmediateTransfer]): uncapped overflow that does not receive the
 * completed construction's production bonuses, is applied to the next queue entry on the same turn
 * (receiving that construction's bonuses, chaining through the queue), and units completed through
 * it can move immediately. Without the unique, the vanilla system (cap + next-turn use) is kept.
 *
 * City setup: a production building provides +10 production and +50% production while constructing
 * buildings, so unbuffed production is 10 and buffed production is 15 (multiplier 1.5).
 */
@RunWith(BaseTestRunner::class)
class ProductionOverflowTest {
    private lateinit var game: TestGame
    private lateinit var city: City

    private val overflowUniqueText = UniqueType.ProductionOverflowImmediateTransfer.text

    @Before
    fun setUp() {
        game = TestGame()
        game.makeHexagonalMap(4)
        UncivGame.Current.settings.autoAssignCityProduction = false // keep the AI automation out of the tests
    }

    /**
     * Must be called before [setupCity]: the ModOptions unique cache is lazily initialized the first
     * time the ruleset modOptions are touched (e.g. by addCiv/addCity), and uniques added after that
     * are not seen.
     */
    private fun enableImmediateOverflow() {
        game.gameInfo.ruleset.modOptions.uniques.add(overflowUniqueText)
    }

    private fun setupCity() {
        val civ = game.addCiv(isPlayer = true)
        city = game.addCity(civ, game.getTile(HexCoord.Zero), replacePalace = true, initialPopulation = 0)

        val productionFactory = game.createBuilding(
            "[+9 Production] [in this city]", // +1 from the city center tile = 10 unbuffed, 15 buffed
            "[+50]% Production when constructing [Building] buildings [in all cities]",
            "[+50]% Production when constructing [All] units [in all cities]",
        )
        city.cityConstructions.addBuilding(productionFactory)
        city.cityStats.update()
        assertEquals(10f, city.cityStats.unbuffedProduction)
    }

    private fun testBuilding(cost: Int): Building = game.createBuilding().apply { this.cost = cost }

    /** Queue [constructions] and refresh the stats so the building production bonus applies to them. */
    private fun queue(vararg constructions: String) {
        for (construction in constructions) city.cityConstructions.addToQueue(construction)
        city.cityStats.update()
        assertEquals(1.5f, city.cityStats.productionMultiplier, 0.001f)
    }

    @Test
    fun `overflow does not receive the completed construction's bonuses`() {
        enableImmediateOverflow()
        setupCity()
        val building = testBuilding(50)
        queue(building.name)

        val constructions = city.cityConstructions
        repeat(3) {
            constructions.addProductionPoints(15)
            constructions.constructIfEnough() // 15 / 30 / 45 - not enough yet
        }
        assertEquals(45, constructions.inProgressConstructions[building.name])
        constructions.addProductionPoints(15)
        constructions.constructIfEnough() // 60 >= 50: buffed overflow is 10, unbuffed is 10/1.5 ≈ 7

        assertTrue(constructions.isBuilt(building.name))
        assertEquals(7, constructions.productionOverflow) // not 10 - the +50% bonus was stripped
    }

    @Test
    fun `overflow immediately rolls over to the next construction with its bonuses`() {
        enableImmediateOverflow()
        setupCity()
        val b1 = testBuilding(10)
        val b2 = testBuilding(40)
        queue(b1.name, b2.name)

        city.cityConstructions.addProductionPoints(15)
        city.cityConstructions.constructIfEnough()

        assertTrue(city.cityConstructions.isBuilt(b1.name))
        // overflow 5 buffed -> 5/1.5 ≈ 3 unbuffed, invested as 3*1.5 = 4.5 -> 5 into b2 on the same turn
        assertEquals(5, city.cityConstructions.inProgressConstructions[b2.name])
        assertEquals(0, city.cityConstructions.productionOverflow)
    }

    @Test
    fun `overflow chains through the queue`() {
        enableImmediateOverflow()
        setupCity()
        val b1 = testBuilding(10)
        val b2 = testBuilding(10)
        val b3 = testBuilding(10)
        queue(b1.name, b2.name, b3.name)

        city.cityConstructions.addProductionPoints(45)
        city.cityConstructions.constructIfEnough()

        // b1: overflow 45-10=35 buffed -> 35/1.5 ≈ 23 unbuffed
        // b2: 23*1.5 = 34.5 -> 35 invested, overflow 25 buffed -> 25/1.5 ≈ 17 unbuffed
        // b3: 17*1.5 = 25.5 -> 26 invested, overflow 16 buffed -> 16/1.5 ≈ 11 unbuffed
        // queue exhausted, the remaining 11 stay in the pool
        assertTrue(city.cityConstructions.isBuilt(b1.name))
        assertTrue(city.cityConstructions.isBuilt(b2.name))
        assertTrue(city.cityConstructions.isBuilt(b3.name))
        assertEquals(11, city.cityConstructions.productionOverflow)
    }

    @Test
    fun `unit completed through immediate overflow can move immediately`() {
        enableImmediateOverflow()
        setupCity()
        val unit = game.createBaseUnit().apply { cost = 10; movement = 2 }
        queue(unit.name)

        city.cityConstructions.addProductionPoints(15)
        city.cityConstructions.constructIfEnough()

        val constructedUnit = city.civ.units.getCivUnits().single()
        assertEquals(constructedUnit.getMaxMovement().toFloat(), constructedUnit.currentMovement)
    }

    @Test
    fun `vanilla fallback keeps the cap and does not roll over`() {
        setupCity()
        val b1 = testBuilding(10)
        val b2 = testBuilding(40)
        queue(b1.name, b2.name)

        city.cityConstructions.addProductionPoints(15)
        city.cityConstructions.constructIfEnough()

        assertTrue(city.cityConstructions.isBuilt(b1.name))
        assertEquals(5, city.cityConstructions.productionOverflow) // capped, buffed, kept for next turn
        assertFalse(city.cityConstructions.inProgressConstructions.containsKey(b2.name)) // no same-turn rollover

        // next endTurn: the overflow is added to the next construction 1:1 (vanilla behavior)
        city.cityConstructions.endTurn(city.cityStats.currentCityStats)
        assertEquals(20, city.cityConstructions.inProgressConstructions[b2.name]) // 15 production + 5 overflow
    }

    @Test
    fun `stored overflow is applied immediately when changing the current construction`() {
        enableImmediateOverflow()
        setupCity()
        val b1 = testBuilding(10)
        queue(b1.name)
        city.cityConstructions.addProductionPoints(15)
        city.cityConstructions.constructIfEnough()
        assertEquals(3, city.cityConstructions.productionOverflow) // (15-10)/1.5 ≈ 3, queue now empty

        val b2 = testBuilding(40)
        city.cityConstructions.setCurrentConstruction(b2.name) // same-turn use: 3*1.5 = 4.5 -> 5 invested

        assertEquals(0, city.cityConstructions.productionOverflow)
        assertEquals(5, city.cityConstructions.inProgressConstructions[b2.name])
    }

    @Test
    fun `endTurn applies stored overflow with the current construction's bonuses as fallback`() {
        enableImmediateOverflow()
        setupCity()
        val b1 = testBuilding(10)
        queue(b1.name)
        city.cityConstructions.addProductionPoints(15)
        city.cityConstructions.constructIfEnough()
        assertEquals(3, city.cityConstructions.productionOverflow) // queue empty, overflow pooled

        // Simulate a queue change that bypassed setCurrentConstruction (defensive fallback path,
        // e.g. moveEntryToTop), then let endTurn apply the pool with the new construction's bonuses
        val b2 = testBuilding(40)
        city.cityConstructions.constructionQueue.add(b2.name)
        city.cityStats.update()
        city.cityConstructions.endTurn(city.cityStats.currentCityStats)

        assertEquals(20, city.cityConstructions.inProgressConstructions[b2.name]) // 15 + 3*1.5 ≈ 5
        assertEquals(0, city.cityConstructions.productionOverflow)
    }

    @Test
    fun `endTurn rolls the turn's production into the next construction on the same turn`() {
        enableImmediateOverflow()
        setupCity()
        val b1 = testBuilding(10)
        val b2 = testBuilding(40)
        queue(b1.name, b2.name)

        city.cityConstructions.endTurn(city.cityStats.currentCityStats) // 15 production invested
        city.cityConstructions.constructIfEnough() // b1 completed, overflow rolls to b2 on the same turn

        assertTrue(city.cityConstructions.isBuilt(b1.name))
        assertEquals(5, city.cityConstructions.inProgressConstructions[b2.name])
        assertEquals(0, city.cityConstructions.productionOverflow)
    }
}
