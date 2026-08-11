package com.unciv.logic.scripting

import com.badlogic.gdx.Gdx
import com.unciv.logic.map.HexCoord
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files
import kotlin.io.path.writeText

/**
 * Lua API robustness and the static mod-checker tooling:
 * float argument sanitization, the static API catalog and its CLI consumption.
 */
@RunWith(GdxTestRunner::class)
class LuaApiRobustnessTests {

    private lateinit var testGame: TestGame

    @Before
    fun setUp() {
        testGame = TestGame()
        testGame.makeHexagonalMap(5, "Grassland")
    }

    /** Writes a Lua script to a temp dir's scripts/ subdirectory, calls [LuaScriptManager.loadScripts], and cleans up. */
    private fun loadLuaScriptToMod(modName: String, scriptName: String, scriptContent: String): Ruleset {
        val tempDir = Files.createTempDirectory("luaRobustTest")
        val scriptsDir = tempDir.resolve("scripts")
        Files.createDirectories(scriptsDir)
        scriptsDir.resolve(scriptName).writeText(scriptContent)
        val mod = Ruleset().apply { name = modName }
        LuaScriptManager.loadScripts(Gdx.files.absolute(tempDir.toAbsolutePath().toString()), modName, mod)
        Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
        return mod
    }

    //region Float argument safety

    @Test
    fun nanFloatArgumentsAreSanitized() {
        // unit.useMovement(0/0) must not poison currentMovement with NaN
        val mod = loadLuaScriptToMod("nanMod", "nan.lua", """
            function testNan(ctx)
                ctx.unit.useMovement(0/0)
                return ctx.unit.getCurrentMovement() == 2
            end
        """.trimIndent())
        val civ = testGame.addCiv(isPlayer = true)
        testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(HexCoord(0, 0)))

        val (foundMod, func) = LuaScriptManager.getFunction("nanMod", "testNan") ?: run {
            Assert.fail("testNan not found"); return
        }
        val ctx = LuaAPI.buildContext(civ, null, unit, unit.currentTile, "", GameContext(unit), foundMod)
        var result = false
        LuaScriptManager.callFunction(func, ctx, civ, "testNan", onSuccess = { result = it }, modName = foundMod)
        Assert.assertTrue("NaN argument should be sanitized to 0 (movement unchanged)", result)
        Assert.assertFalse("currentMovement must not be NaN", unit.currentMovement.isNaN())
    }

    //endregion

    //region Static checker & API catalog

    @Test
    fun apiCatalogMatchesRuntimeRegistration() {
        // Force runtime registration by building a context, then verify every registered
        // API is listed in the static catalog - otherwise the CLI mod checker won't know it
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(HexCoord(0, 0)))
        LuaAPI.buildContext(civ, city, unit, unit.currentTile, "", GameContext(civ, city, unit, unit.currentTile), "catMod")

        Assert.assertTrue("Runtime registration should have populated knownApiMethods",
            LuaAPI.knownApiMethods.isNotEmpty())
        for ((owner, names) in LuaAPI.knownApiMethods) {
            for (name in names) {
                Assert.assertTrue(
                    "Runtime API '$owner.$name' is missing from LuaAPI.apiCatalog",
                    name in LuaAPI.apiCatalog[owner].orEmpty()
                )
            }
        }
        for ((owner, names) in LuaAPI.apiCatalog) {
            for (name in names) {
                Assert.assertTrue(
                    "Catalog API '$owner.$name' is missing from runtime registration (catalog out of sync)",
                    name in LuaAPI.knownApiMethods[owner].orEmpty()
                )
            }
        }
    }

    @Test
    fun staticCheckerCatchesApiTypos() {
        val tempDir = java.nio.file.Files.createTempDirectory("luaCheck")
        val scriptsDir = tempDir.resolve("scripts")
        java.nio.file.Files.createDirectories(scriptsDir)
        scriptsDir.resolve("typo.lua").writeText("""
            -- ctx.civ.addGoldd(100)  <- in a comment, must NOT be reported
            function onEvent(ctx)
                ctx.civ.addGoldd(100)
                ctx.city.getPopultion()
                ctx.game.getTilee(1, 2)
                ctx.civ.addGold(50)   -- valid
                ctx.unit.base.name    -- valid property chain
                return true
            end
        """.trimIndent())
        scriptsDir.resolve("ok.lua").writeText("function fine(ctx)\n  return ctx.civ.addGold(1) ~= nil\nend\n")

        val errors = LuaModStaticChecker.checkApiUsage(
            com.badlogic.gdx.Gdx.files.absolute(tempDir.toAbsolutePath().toString() + "/scripts")
        )
        val messages = errors.map { it.message }
        org.junit.Assert.assertTrue(
            "Should flag ctx.civ.addGoldd typo, got: $messages",
            messages.any { it.contains("ctx.civ.addGoldd") }
        )
        org.junit.Assert.assertTrue(
            "Should flag ctx.city.getPopultion typo, got: $messages",
            messages.any { it.contains("ctx.city.getPopultion") }
        )
        org.junit.Assert.assertTrue(
            "Should flag ctx.game.getTilee typo, got: $messages",
            messages.any { it.contains("ctx.game.getTilee") }
        )
        org.junit.Assert.assertFalse(
            "Comment contents must not be flagged, got: $messages",
            messages.any { it.contains("ctx.civ.addGoldd(100)  <- in a comment") || messages.count { it.contains("addGoldd") } > 1 }
        )
        org.junit.Assert.assertTrue(
            "Valid calls must not be flagged, got: $messages",
            // valid calls are on lines 6 (civ.addGold) and 7 (unit.base.name)
            messages.none { it.contains("line 6") } && messages.none { it.contains("line 7") }
        )
        org.junit.Assert.assertTrue(
            "All findings should be warnings, got: $messages",
            errors.all { it.severity == LuaScriptErrorSeverity.WARNING }
        )
        java.nio.file.Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach { java.nio.file.Files.delete(it) }
    }

    @Test
    fun testModScriptsPassStaticCheck() {
        // The bundled testMOD scripts must be clean - keeps the checker honest about false positives
        val testModDir = sequenceOf(
            Gdx.files.internal("mods/testMOD"),
            Gdx.files.absolute(System.getProperty("user.dir") + "/android/assets/mods/testMOD")
        ).firstOrNull { it.isDirectory } ?: run { return }
        val errors = LuaModStaticChecker.checkApiUsage(testModDir.child("scripts"))
        Assert.assertTrue(
            "testMOD should pass the static Lua check, got: ${errors.map { it.message }}",
            errors.isEmpty()
        )
    }

    @Test
    fun modCiLuaFlowCatchesErrors() {
        // Simulates the desktop `mod-ci` CLI flow (without image packing):
        // ruleset.load collects Lua syntax errors, the static checker collects API typos,
        // and RulesetValidator surfaces both as ruleset errors
        val testModDir = sequenceOf(
            Gdx.files.internal("mods/testMOD"),
            Gdx.files.absolute(System.getProperty("user.dir") + "/android/assets/mods/testMOD")
        ).firstOrNull { it.isDirectory } ?: run { return }

        val ruleset = Ruleset().apply { name = "testMOD" }
        ruleset.load(testModDir.child("jsons"))
        ruleset.luaErrors.addAll(LuaModStaticChecker.checkApiUsage(testModDir.child("scripts")))

        val errorList = com.unciv.models.ruleset.validation.RulesetValidator.create(ruleset, true).getErrorList()
        Assert.assertFalse(
            "testMOD should have no Error-level issues in the mod-ci flow, got: ${errorList.filter { it.errorSeverityToReport == com.unciv.models.ruleset.validation.RulesetErrorSeverity.Error }.map { it.text }}",
            errorList.any { it.errorSeverityToReport == com.unciv.models.ruleset.validation.RulesetErrorSeverity.Error }
        )
        Assert.assertTrue(
            "mod-ci flow should report the loaded Lua scripts as OK info",
            errorList.any { it.text.contains("Loaded") && it.text.contains("script") }
        )
    }

    //endregion
}
