package com.unciv.logic.map.mapunit.movement

import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.RoadStatus
import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(GdxTestRunner::class)
class MovementCostTest {
    private lateinit var testGame: TestGame

    @Before
    fun setUp() {
        testGame = TestGame()
        testGame.makeHexagonalMap(5)
    }

    @Test
    fun basicRoadShouldHaveTier0MovementCost() {
        // given
        val civ = testGame.addCiv()
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        val tile1 = testGame.getTile(0, 0)
        val tile2 = testGame.getTile(1, 0)
        tile1.setRoadStatus(RoadStatus.Road, civ)
        tile2.setRoadStatus(RoadStatus.Road, civ)

        // when
        val movementCost = MovementCost.getMovementCostBetweenAdjacentTiles(unit, tile1, tile2)

        // then
        assertEquals(0.5f, movementCost, 0.001f)
    }

    @Test
    fun railroadShouldHaveTier2MovementCost() {
        // given
        val civ = testGame.addCiv()
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        val tile1 = testGame.getTile(0, 0)
        val tile2 = testGame.getTile(1, 0)
        tile1.setRoadStatus(RoadStatus.Railroad, civ)
        tile2.setRoadStatus(RoadStatus.Railroad, civ)

        // when
        val movementCost = MovementCost.getMovementCostBetweenAdjacentTiles(unit, tile1, tile2)

        // then
        assertEquals(0.1f, movementCost, 0.001f)
    }

    @Test
    fun roadWithRoadMovementSpeedUniqueShouldHaveTier1MovementCost() {
        // given
        val civ = testGame.addCiv("Improves movement speed on roads")
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        val tile1 = testGame.getTile(0, 0)
        val tile2 = testGame.getTile(1, 0)
        tile1.setRoadStatus(RoadStatus.Road, civ)
        tile2.setRoadStatus(RoadStatus.Road, civ)

        // when
        val movementCost = MovementCost.getMovementCostBetweenAdjacentTiles(unit, tile1, tile2)

        // then
        assertEquals(1/3f, movementCost, 0.001f)
    }

    @Test
    fun roadWithRoadSpeedTierBonusPlus1ShouldHaveTier1MovementCost() {
        // given
        val civ = testGame.addCiv("Road speed tier [+1]")
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        val tile1 = testGame.getTile(0, 0)
        val tile2 = testGame.getTile(1, 0)
        tile1.setRoadStatus(RoadStatus.Road, civ)
        tile2.setRoadStatus(RoadStatus.Road, civ)

        // when
        val movementCost = MovementCost.getMovementCostBetweenAdjacentTiles(unit, tile1, tile2)

        // then
        assertEquals(1/3f, movementCost, 0.001f)
    }

    @Test
    fun roadWithRoadSpeedTierBonusPlus2ShouldHaveTier2MovementCost() {
        // given
        val civ = testGame.addCiv("Road speed tier [+2]")
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        val tile1 = testGame.getTile(0, 0)
        val tile2 = testGame.getTile(1, 0)
        tile1.setRoadStatus(RoadStatus.Road, civ)
        tile2.setRoadStatus(RoadStatus.Road, civ)

        // when
        val movementCost = MovementCost.getMovementCostBetweenAdjacentTiles(unit, tile1, tile2)

        // then
        assertEquals(0.1f, movementCost, 0.001f)
    }

    @Test
    fun roadWithRoadSpeedTierBonusMinus1ShouldStillHaveMinimumTier0() {
        // given
        val civ = testGame.addCiv("Road speed tier [-1]")
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        val tile1 = testGame.getTile(0, 0)
        val tile2 = testGame.getTile(1, 0)
        tile1.setRoadStatus(RoadStatus.Road, civ)
        tile2.setRoadStatus(RoadStatus.Road, civ)

        // when
        val movementCost = MovementCost.getMovementCostBetweenAdjacentTiles(unit, tile1, tile2)

        // then - should be clamped to minimum tier 0
        assertEquals(0.5f, movementCost, 0.001f)
    }

    @Test
    fun roadWithRoadSpeedTierBonusPlus10ShouldBeClampedToMaximumTier2() {
        // given
        val civ = testGame.addCiv("Road speed tier [+10]")
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        val tile1 = testGame.getTile(0, 0)
        val tile2 = testGame.getTile(1, 0)
        tile1.setRoadStatus(RoadStatus.Road, civ)
        tile2.setRoadStatus(RoadStatus.Road, civ)

        // when
        val movementCost = MovementCost.getMovementCostBetweenAdjacentTiles(unit, tile1, tile2)

        // then - should be clamped to maximum tier 2
        assertEquals(0.1f, movementCost, 0.001f)
    }

    @Test
    fun multipleRoadSpeedTierBonusesShouldStackAdditively() {
        // given - add a civ with stacked uniques
        val civ = testGame.addCiv("Road speed tier [+1]", "Road speed tier [+1]")
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        val tile1 = testGame.getTile(0, 0)
        val tile2 = testGame.getTile(1, 0)
        tile1.setRoadStatus(RoadStatus.Road, civ)
        tile2.setRoadStatus(RoadStatus.Road, civ)

        // when
        val movementCost = MovementCost.getMovementCostBetweenAdjacentTiles(unit, tile1, tile2)

        // then - +1 + 1 = tier 2
        assertEquals(0.1f, movementCost, 0.001f)
    }

    @Test
    fun roadMovementSpeedAndRoadSpeedTierBonusShouldStack() {
        // given
        val civ = testGame.addCiv("Improves movement speed on roads", "Road speed tier [+1]")
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        val tile1 = testGame.getTile(0, 0)
        val tile2 = testGame.getTile(1, 0)
        tile1.setRoadStatus(RoadStatus.Road, civ)
        tile2.setRoadStatus(RoadStatus.Road, civ)

        // when
        val movementCost = MovementCost.getMovementCostBetweenAdjacentTiles(unit, tile1, tile2)

        // then - tier 0 + 1 (RoadMovementSpeed) + 1 (RoadSpeedTierBonus) = tier 2
        assertEquals(0.1f, movementCost, 0.001f)
    }

    @Test
    fun improvementWithTier0ShouldHaveTier0MovementCost() {
        // given
        val civ = testGame.addCiv()
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        val tile1 = testGame.getTile(0, 0)
        val tile2 = testGame.getTile(1, 0)
        val customRoad = testGame.createTileImprovement("Functions for movement at tier [0]")
        testGame.addCity(civ, tile1)
        tile1.improvementFunctions.setImprovement(customRoad.name, civ)
        tile2.improvementFunctions.setImprovement(customRoad.name, civ)

        // when
        val movementCost = MovementCost.getMovementCostBetweenAdjacentTiles(unit, tile1, tile2)

        // then - tier 0 = 0.5
        assertEquals(0.5f, movementCost, 0.001f)
    }

    @Test
    fun improvementWithTier1ShouldHaveTier1MovementCost() {
        // given
        val civ = testGame.addCiv()
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        val tile1 = testGame.getTile(0, 0)
        val tile2 = testGame.getTile(1, 0)
        val customRoad = testGame.createTileImprovement("Functions for movement at tier [1]")
        testGame.addCity(civ, tile1)
        tile1.improvementFunctions.setImprovement(customRoad.name, civ)
        tile2.improvementFunctions.setImprovement(customRoad.name, civ)

        // when
        val movementCost = MovementCost.getMovementCostBetweenAdjacentTiles(unit, tile1, tile2)

        // then - tier 1 = 1/3
        assertEquals(1/3f, movementCost, 0.001f)
    }

    @Test
    fun improvementWithTier2ShouldHaveTier2MovementCost() {
        // given
        val civ = testGame.addCiv()
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        val tile1 = testGame.getTile(0, 0)
        val tile2 = testGame.getTile(1, 0)
        val customRoad = testGame.createTileImprovement("Functions for movement at tier [2]")
        testGame.addCity(civ, tile1)
        tile1.improvementFunctions.setImprovement(customRoad.name, civ)
        tile2.improvementFunctions.setImprovement(customRoad.name, civ)

        // when
        val movementCost = MovementCost.getMovementCostBetweenAdjacentTiles(unit, tile1, tile2)

        // then - tier 2 = 0.1
        assertEquals(0.1f, movementCost, 0.001f)
    }

    @Test
    fun improvementWithTierAndRoadSpeedTierBonusShouldStack() {
        // given
        val civ = testGame.addCiv("Road speed tier [+1]")
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        val tile1 = testGame.getTile(0, 0)
        val tile2 = testGame.getTile(1, 0)
        val customRoad = testGame.createTileImprovement("Functions for movement at tier [0]")
        testGame.addCity(civ, tile1)
        tile1.improvementFunctions.setImprovement(customRoad.name, civ)
        tile2.improvementFunctions.setImprovement(customRoad.name, civ)

        // when
        val movementCost = MovementCost.getMovementCostBetweenAdjacentTiles(unit, tile1, tile2)

        // then - tier 0 + 1 = tier 1 = 1/3
        assertEquals(1/3f, movementCost, 0.001f)
    }

    @Test
    fun improvementWithTier1AndRoadMovementSpeedShouldStack() {
        // given
        val civ = testGame.addCiv("Improves movement speed on roads")
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(0, 0))
        val tile1 = testGame.getTile(0, 0)
        val tile2 = testGame.getTile(1, 0)
        val customRoad = testGame.createTileImprovement("Functions for movement at tier [1]")
        testGame.addCity(civ, tile1)
        tile1.improvementFunctions.setImprovement(customRoad.name, civ)
        tile2.improvementFunctions.setImprovement(customRoad.name, civ)

        // when
        val movementCost = MovementCost.getMovementCostBetweenAdjacentTiles(unit, tile1, tile2)

        // then - tier 1 + 1 = tier 2 = 0.1
        assertEquals(0.1f, movementCost, 0.001f)
    }
}

