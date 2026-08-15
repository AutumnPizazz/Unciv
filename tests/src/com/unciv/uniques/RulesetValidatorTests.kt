package com.unciv.uniques

import com.unciv.models.ruleset.Building
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.ruleset.nation.Personality
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.ruleset.validation.RulesetErrorSeverity
import com.unciv.models.translations.fillPlaceholders
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(BaseTestRunner::class)
class RulesetValidatorTests {

    private fun addPersonality(game: TestGame, name: String, vararg uniques: String): Personality {
        return Personality().apply {
            this.name = name
            uniques.toCollection(this.uniques)
            game.ruleset.personalities[name] = this
        }
    }

    private fun hasRecursiveResourceUniqueError(game: TestGame, modifierText: String): Boolean {
        return game.ruleset.getErrorList().any {
            it.errorSeverityToReport == RulesetErrorSeverity.Error
                && it.text.contains(modifierText)
                && it.text.contains("recursive evaluation loop")
        }
    }

    @Test
    fun `ruleset validator warns about spy name collision with ruleset object`() {
        val game = TestGame()
        game.ruleset.buildings["Park"] = Building().apply {
            name = "Park"
            originRuleset = "Test mod"
            uniques.add("Unbuildable")
        }
        game.ruleset.nations.values.first().spyNames = arrayListOf("Park")

        val errors = game.ruleset.getErrorList()

        assertTrue(errors.any {
            it.errorSeverityToReport == RulesetErrorSeverity.WarningOptionsOnly
                && it.text.contains("\"Park\"")
                && it.text.contains("Building")
                && it.text.contains("Nation.spyNames")
        })
    }

    @Test
    fun `ruleset validator warns about non ruleset object name collisions`() {
        val game = TestGame()
        game.ruleset.name = "Test mod"
        game.ruleset.buildings["Park"] = Building().apply {
            name = "Park"
            originRuleset = "Test mod"
            uniques.add("Unbuildable")
        }
        game.ruleset.religions.add("Park")

        val errors = game.ruleset.getErrorList()

        assertTrue(errors.any {
            it.errorSeverityToReport == RulesetErrorSeverity.WarningOptionsOnly
                && it.text.contains("\"Park\"")
                && it.text.contains("Building")
                && it.text.contains("Religion")
        })
    }

    @Test
    fun `ruleset validator warns when mod name collision has empty origin`() {
        val game = TestGame()
        game.ruleset.name = ""
        game.ruleset.buildings["Park"] = Building().apply {
            name = "Park"
            originRuleset = BaseRuleset.Civ_V_GnK.fullName
            uniques.add("Unbuildable")
        }
        game.ruleset.religions.add("Park")

        val errors = game.ruleset.getErrorList()

        assertTrue(errors.any {
            it.errorSeverityToReport == RulesetErrorSeverity.WarningOptionsOnly
                && it.text.contains("\"Park\"")
                && it.text.contains("Building")
                && it.text.contains("Religion")
        })
    }

    @Test
    fun `ruleset validator warns about leader name collisions`() {
        val game = TestGame()
        game.ruleset.buildings["Park"] = Building().apply {
            name = "Park"
            originRuleset = "Test mod"
            uniques.add("Unbuildable")
        }
        game.ruleset.nations.values.first().leaderName = "Park"

        val errors = game.ruleset.getErrorList()

        assertTrue(errors.any {
            it.errorSeverityToReport == RulesetErrorSeverity.WarningOptionsOnly
                && it.text.contains("\"Park\"")
                && it.text.contains("Building")
                && it.text.contains("Nation.leaderName")
        })
    }

    @Test
    fun `ruleset validator warns about city name collisions`() {
        val game = TestGame()
        game.ruleset.buildings["Park"] = Building().apply {
            name = "Park"
            originRuleset = "Test mod"
            uniques.add("Unbuildable")
        }
        game.ruleset.nations.values.first().cities = arrayListOf("Park")

        val errors = game.ruleset.getErrorList()

        assertTrue(errors.any {
            it.errorSeverityToReport == RulesetErrorSeverity.WarningOptionsOnly
                && it.text.contains("\"Park\"")
                && it.text.contains("Building")
                && it.text.contains("Nation.cities")
        })
    }

    @Test
    fun `ruleset validator accepts personality without uniques`() {
        val game = TestGame()
        addPersonality(game, "Reserved")

        val errors = game.ruleset.getErrorList()

        assertFalse(errors.any { it.text.contains("Personality Uniques are not supported") })
    }

    @Test
    fun `ruleset validator accepts supported personality uniques`() {
        val game = TestGame()
        val unique = UniqueType.WillNotBuild.text.fillPlaceholders("Melee")
        addPersonality(game, "Builder Avoider", unique)

        val errors = game.ruleset.getErrorList()

        assertFalse(errors.any { it.text.contains("Personality Uniques are not supported") })
        assertFalse(errors.any { it.text.contains(unique) })
    }

    @Test
    fun `ruleset validator warns about non personality uniques on personalities`() {
        val game = TestGame()
        addPersonality(game, "Unsupported Unique Holder", UniqueType.Unbuildable.text)

        val errors = game.ruleset.getErrorList()

        assertTrue(errors.any {
            it.errorSeverityToReport == RulesetErrorSeverity.Warning
                && it.text.contains(UniqueType.Unbuildable.text)
                && it.text.contains("not allowed on its target type")
        })
    }

    @Test
    fun `ruleset validator rejects resource unique for every citywide resource countable`() {
        val game = TestGame()
        val citywideResource = game.createResource(UniqueType.CityResource.text)
        val providedResource = game.createResource()

        game.createBuilding("Provides [1] [${providedResource.name}] <for every [${citywideResource.name}]>")

        assertTrue(hasRecursiveResourceUniqueError(game, "for every [${citywideResource.name}]"))
    }

    @Test
    fun `ruleset validator rejects resource unique when above citywide resource conditional`() {
        val game = TestGame()
        val citywideResource = game.createResource(UniqueType.CityResource.text)
        val providedResource = game.createResource()

        game.createBuilding("Provides [1] [${providedResource.name}] <when above [1] [${citywideResource.name}]>")

        assertTrue(hasRecursiveResourceUniqueError(game, "when above [1] [${citywideResource.name}]"))
    }

    @Test
    fun `ruleset validator accepts resource unique for every normal resource countable`() {
        val game = TestGame()
        val normalResource = game.createResource()
        val providedResource = game.createResource()

        game.createBuilding("Provides [1] [${providedResource.name}] <for every [${normalResource.name}]>")

        assertFalse(hasRecursiveResourceUniqueError(game, "for every [${normalResource.name}]"))
    }

    @Test
    fun `ruleset validator accepts resource unique for every non resource countable`() {
        val game = TestGame()
        val providedResource = game.createResource()
        game.createBuilding("Provides [1] [${providedResource.name}] <for every [Cities]>")
        assertFalse(hasRecursiveResourceUniqueError(game, "for every [Cities]"))
    }

    @Test
    fun `CoeHarMod quanMinDongYuan merged unique loads`() {
        val coeHarDir = com.badlogic.gdx.Gdx.files.absolute(
            System.getProperty("user.dir") + "/mods/CoeHarMod")
        if (!coeHarDir.isDirectory) return // CoeHarMod 未检出（CI 环境）时跳过

        val ruleset = com.unciv.models.ruleset.Ruleset().apply { name = "CoeHarMod" }
        ruleset.load(coeHarDir.child("jsons"))

        // 加载无 Error 级问题
        val errors = ruleset.getErrorList().filter { it.errorSeverityToReport == RulesetErrorSeverity.Error }
        assertTrue("CoeHarMod should load without errors, got: ${errors.map { it.text }.take(3)}", errors.isEmpty())

        // 全民动员：新版为一条 Countable 表达式 + 单位 tag（submodule 指针可能未同步时旧版为逐单位 unique）
        val building = ruleset.buildings["Mil.quanmindongyuan"]
        assertTrue("Mil.quanmindongyuan missing", building != null)
        assertTrue("QuanMinDongYuan mechanism missing", building!!.uniques.any {
            it.contains("QuanMinDongYuan") && it.contains("Units] [Gold] <upon turn end>")
        } || ruleset.units.values.any { unit ->
            unit.uniques.any { it.contains("Gain [1] [Gold] <upon turn end>") }
        })
    }

    //endregion
}
