package com.unciv.uniques

import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.tile.Tile
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(BaseTestRunner::class)
class UnitMaxHealthTests {
    private lateinit var game: TestGame

    @Before
    fun initTheWorld() {
        game = TestGame()
        game.makeHexagonalMap(2)
    }

    /** A tile with no units on it - so each unit in a test gets its own tile */
    private fun freeTile(): Tile =
        game.tileMap.values.first { it.militaryUnit == null && it.civilianUnit == null }

    /** Creates a melee unit with the given [maxHP] and uniques, owned by a new civ */
    private fun newMeleeUnit(maxHP: Int = 100, vararg uniques: String): MapUnit {
        val baseUnit = game.createBaseUnit("Sword", *uniques)
        baseUnit.movement = 2
        baseUnit.strength = 8
        baseUnit.maxHP = maxHP
        return game.addUnit(baseUnit.name, game.addCiv(), freeTile())
    }

    @Test
    fun `default max HP is 100`() {
        val unit = newMeleeUnit()
        assertEquals(100, unit.getMaxHealth())
        assertEquals(100, unit.health)
    }

    @Test
    fun `units start at their maxHP from the ruleset`() {
        val unit = newMeleeUnit(maxHP = 200)
        assertEquals(200, unit.getMaxHealth())
        assertEquals(200, unit.health)
    }

    @Test
    fun `healBy clamps to the dynamic max HP`() {
        val unit = newMeleeUnit(maxHP = 200)
        unit.takeDamage(50)
        assertEquals(150, unit.health)
        unit.healBy(1000)
        assertEquals(200, unit.health)
    }

    @Test
    fun `takeDamage clamps negative damage to the dynamic max HP`() {
        val unit = newMeleeUnit(maxHP = 200)
        unit.takeDamage(-500) // "cheating modders", e.g. negative tile damage
        assertEquals(200, unit.health)
    }

    @Test
    fun `wounded filter respects max HP`() {
        val fullHealthUnit = newMeleeUnit(maxHP = 200)
        assertFalse(fullHealthUnit.matchesFilter("Wounded"))

        val woundedUnit = newMeleeUnit(maxHP = 200)
        woundedUnit.takeDamage(50)
        assertTrue(woundedUnit.matchesFilter("Wounded"))
        assertTrue(woundedUnit.matchesFilter("wounded units"))
    }

    @Test
    fun `MaxHealth unique adds to max HP`() {
        val unit = newMeleeUnit(maxHP = 100, "[+50] Max HP")
        assertEquals(150, unit.getMaxHealth())
        assertEquals(150, unit.health)
    }

    @Test
    fun `MaxHealth unique with unit filter only applies to matching units`() {
        val meleeUnit = newMeleeUnit(maxHP = 100, "[+50] Max HP <for [Melee] units>")
        assertEquals(150, meleeUnit.getMaxHealth())

        val rangedBaseUnit = game.createBaseUnit("Archery", "[+50] Max HP <for [Melee] units>")
        rangedBaseUnit.movement = 2
        rangedBaseUnit.strength = 5
        rangedBaseUnit.rangedStrength = 7
        rangedBaseUnit.range = 2
        val rangedUnit = game.addUnit(rangedBaseUnit.name, game.addCiv(), freeTile())
        assertEquals(100, rangedUnit.getMaxHealth())
    }

    @Test
    fun `MaxHealth unique with other conditionals is ignored`() {
        val unit = newMeleeUnit(maxHP = 100, "[+50] Max HP <when below [10] HP>")
        assertEquals(100, unit.getMaxHealth())
        unit.takeDamage(95) // Even when the conditional would be true, Max HP must stay constant
        assertEquals(100, unit.getMaxHealth())
        assertEquals(5, unit.health)
    }

    @Test
    fun `MaxHealth unique on the civ applies to its units`() {
        val civ = game.addCiv("[+50] Max HP <for [All] units>")
        val unit = game.addUnit("Warrior", civ, freeTile())
        assertEquals(150, unit.getMaxHealth())
        assertEquals(150, unit.health)
    }

    @Test
    fun `negative MaxHealth uniques cannot reduce max HP below 1`() {
        val unit = newMeleeUnit(maxHP = 100, "[-150] Max HP")
        assertEquals(1, unit.getMaxHealth())
        assertEquals(1, unit.health)
    }

    @Test
    fun `maxHP field of 0 or less is reset to 1`() {
        val zeroMaxHPUnit = newMeleeUnit(maxHP = 0)
        assertEquals(1, zeroMaxHPUnit.getMaxHealth())
        assertEquals(1, zeroMaxHPUnit.health)

        val negativeMaxHPUnit = newMeleeUnit(maxHP = -50)
        assertEquals(1, negativeMaxHPUnit.getMaxHealth())
        assertEquals(1, negativeMaxHPUnit.health)
    }

    @Test
    fun `copyStatisticsTo clamps health to the new units max HP`() {
        val upgradedFrom = newMeleeUnit(maxHP = 200)
        val upgradedTo = newMeleeUnit(maxHP = 100)
        upgradedFrom.copyStatisticsTo(upgradedTo)
        assertEquals(100, upgradedTo.health)
    }

    @Test
    fun `setTransients clamps old save health to the new max HP`() {
        val unit = newMeleeUnit(maxHP = 200)
        assertEquals(200, unit.health)
        game.ruleset.units[unit.name]!!.maxHP = 50 // mod changed the ruleset
        unit.setTransients(game.ruleset)
        assertEquals(50, unit.health)
    }
}
