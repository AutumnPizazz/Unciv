package com.unciv.uniques

import com.unciv.ui.components.fonts.Fonts
import com.unciv.ui.objectdescriptions.BaseUnitDescriptions
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(BaseTestRunner::class)
class BaseUnitDescriptionsTests {
    private lateinit var game: TestGame

    @Before
    fun initTheWorld() {
        game = TestGame()
        game.makeHexagonalMap(2)
    }

    private fun createUnit(maxHP: Int = 100, maintenanceCost: Float = 1f) =
        game.createBaseUnit("Sword").apply {
            movement = 2
            strength = 8
            this.maxHP = maxHP
            this.maintenanceCost = maintenanceCost
        }

    private fun civilopediaLines(maxHP: Int = 100, maintenanceCost: Float = 1f) =
        BaseUnitDescriptions.getCivilopediaTextLines(createUnit(maxHP, maintenanceCost), game.ruleset)

    @Test
    fun `max HP shows in civilopedia when non-default`() {
        val lines = civilopediaLines(maxHP = 200)
        Assert.assertTrue("expected 200${Fonts.health} in: ${lines.map { it.text }}",
            lines.any { it.text.contains("200") && it.text.contains(Fonts.health.toString()) })
    }

    @Test
    fun `default max HP is not shown in civilopedia`() {
        val lines = civilopediaLines()
        Assert.assertFalse("default max HP should not be listed, got: ${lines.map { it.text }}",
            lines.any { it.text.contains(Fonts.health.toString()) })
    }

    @Test
    fun `maintenance cost shows in civilopedia in civ6 mode`() {
        // 开关必须在 modOptions 缓存首次访问前添加
        game.ruleset.modOptions.uniques.add("Uses the Civilization 6 style unit maintenance system")
        game.ruleset.modOptions.constants.unitMaintenanceBaseCost = 1.0
        val lines = civilopediaLines(maintenanceCost = 3f)
        Assert.assertTrue("expected maintenance line in: ${lines.map { it.text }}",
            lines.any { it.text.contains("Unit upkeep") && it.text.contains("3") && it.text.contains("Gold") })
    }

    @Test
    fun `maintenance cost not shown without civ6 mode`() {
        val lines = civilopediaLines(maintenanceCost = 3f)
        Assert.assertFalse("legacy mode should not list maintenance, got: ${lines.map { it.text }}",
            lines.any { it.text.contains("Unit upkeep") })
    }

    @Test
    fun `zero maintenance cost not shown in civilopedia`() {
        game.ruleset.modOptions.uniques.add("Uses the Civilization 6 style unit maintenance system")
        val lines = civilopediaLines(maintenanceCost = 0f)
        Assert.assertFalse("zero maintenance should not be listed, got: ${lines.map { it.text }}",
            lines.any { it.text.contains("Unit upkeep") })
    }
}
