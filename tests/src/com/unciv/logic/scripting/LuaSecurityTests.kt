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
 * Security and robustness tests for the Lua sandbox:
 * sandbox escape vectors, execution budget (infinite loops), error reporting details.
 */
@RunWith(GdxTestRunner::class)
class LuaSecurityTests {

    private lateinit var testGame: TestGame

    @Before
    fun setUp() {
        testGame = TestGame()
        testGame.makeHexagonalMap(5, "Grassland")
    }

    /** Writes a Lua script to a temp dir's scripts/ subdirectory, calls [LuaScriptManager.loadScripts], and cleans up. */
    private fun loadLuaScriptToMod(modName: String, scriptName: String, scriptContent: String): Ruleset {
        val tempDir = Files.createTempDirectory("luaSecurityTest")
        val scriptsDir = tempDir.resolve("scripts")
        Files.createDirectories(scriptsDir)
        scriptsDir.resolve(scriptName).writeText(scriptContent)
        val mod = Ruleset().apply { name = modName }
        LuaScriptManager.loadScripts(Gdx.files.absolute(tempDir.toAbsolutePath().toString()), modName, mod)
        Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
        return mod
    }

    //region Sandbox escape

    @Test
    fun packageLibraryIsRemoved() {
        // package.loaded previously retained full references to io/os/luajava/coroutine,
        // giving mod scripts complete sandbox escape (arbitrary file IO, process execution, Java reflection)
        val mod = loadLuaScriptToMod("escapeMod", "check.lua", """
            function checkSandbox(ctx)
                return package == nil
            end
        """.trimIndent())
        Assert.assertTrue("Script should load without error",
            mod.luaErrors.none { it.severity == LuaScriptErrorSeverity.ERROR })

        val (foundMod, func) = LuaScriptManager.getFunction("escapeMod", "checkSandbox") ?: run {
            Assert.fail("checkSandbox function not found"); return
        }
        val civ = testGame.addCiv(isPlayer = true)
        val ctx = LuaAPI.buildContext(civ, null, null, null, "", GameContext(civ), foundMod)
        var result = false
        LuaScriptManager.callFunction(func, ctx, civ, "checkSandbox", onSuccess = { result = it }, modName = foundMod)
        Assert.assertTrue("package should be nil inside the sandbox", result)
    }

    @Test
    fun escapedLibrariesNotReachableThroughPackage() {
        // Even if a script tries common escape idioms, io/os/luajava must not be reachable
        val mod = loadLuaScriptToMod("escapeMod2", "check2.lua", """
            function checkEscape(ctx)
                -- package is nil, so indexing it must error (luaj throws on nil index) -
                -- any non-error path means the escaped library is reachable
                local ok, err = pcall(function()
                    return package.loaded.io
                end)
                if ok then
                    ctx.log("ESCAPE: package.loaded.io returned " .. tostring(err))
                    return false
                end
                local ok2, err2 = pcall(function()
                    return package.loaded.luajava.bindClass
                end)
                if ok2 then
                    ctx.log("ESCAPE: package.loaded.luajava reachable")
                    return false
                end
                return true
            end
        """.trimIndent())
        Assert.assertTrue(mod.luaErrors.none { it.severity == LuaScriptErrorSeverity.ERROR })

        val (foundMod, func) = LuaScriptManager.getFunction("escapeMod2", "checkEscape") ?: return
        val civ = testGame.addCiv(isPlayer = true)
        val ctx = LuaAPI.buildContext(civ, null, null, null, "", GameContext(civ), foundMod)
        var result = false
        LuaScriptManager.callFunction(func, ctx, civ, "checkEscape", onSuccess = { result = it }, modName = foundMod)
        Assert.assertTrue("package.loaded must not expose io/os/luajava", result)
    }

    //endregion

    //region Execution budget (infinite loops)

    @Test
    fun infiniteLoopAtLoadTimeIsInterrupted() {
        // Top-level `while true do end` must not hang the loader; the instruction budget
        // interrupts it and the error is collected like a syntax error
        val mod = loadLuaScriptToMod("loopLoadMod", "bad.lua", "while true do end\n")
        val budgetErrors = mod.luaErrors.filter {
            it.severity == LuaScriptErrorSeverity.ERROR && it.message.contains("budget", ignoreCase = true)
        }
        Assert.assertTrue(
            "Infinite loop at load time should produce a budget error, got: ${mod.luaErrors.map { it.message }}",
            budgetErrors.isNotEmpty()
        )
    }

    @Test
    fun infiniteLoopAtRuntimeIsInterrupted() {
        val mod = loadLuaScriptToMod("loopRunMod", "spin.lua", """
            function spinForever(ctx)
                while true do end
            end
        """.trimIndent())
        val (foundMod, func) = LuaScriptManager.getFunction("loopRunMod", "spinForever") ?: run {
            Assert.fail("spinForever not found"); return
        }
        val civ = testGame.addCiv(isPlayer = true)
        val ctx = LuaAPI.buildContext(civ, null, null, null, "", GameContext(civ), foundMod)
        var success = true
        LuaScriptManager.callFunction(func, ctx, civ, "spinForever", onSuccess = { success = it }, modName = foundMod)
        Assert.assertFalse("Runaway loop should report failure instead of hanging", success)
    }

    @Test
    fun budgetErrorDoesNotPoisonLaterCalls() {
        // After a budget-exceeded call, the next call to the same mod must work normally
        val mod = loadLuaScriptToMod("budgetMod", "mix.lua", """
            function spin(ctx)
                while true do end
            end
            function fine(ctx)
                return true
            end
        """.trimIndent())
        val civ = testGame.addCiv(isPlayer = true)

        val (foundMod, spinFunc) = LuaScriptManager.getFunction("budgetMod", "spin")!!
        var spinResult = true
        LuaScriptManager.callFunction(spinFunc, LuaAPI.buildContext(civ, null, null, null, "", GameContext(civ), foundMod),
            civ, "spin", onSuccess = { spinResult = it }, modName = foundMod)
        Assert.assertFalse("First spin call should fail on budget", spinResult)

        val (foundMod2, fineFunc) = LuaScriptManager.getFunction("budgetMod", "fine")!!
        var fineResult = false
        LuaScriptManager.callFunction(fineFunc, LuaAPI.buildContext(civ, null, null, null, "", GameContext(civ), foundMod2),
            civ, "fine", onSuccess = { fineResult = it }, modName = foundMod2)
        Assert.assertTrue("Later call to same mod should work after budget reset", fineResult)
    }

    //endregion

    //region Error reporting details

    @Test
    fun runtimeErrorPopupContainsLineNumber() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        civ.gameInfo.modLuaStorage.getOrPut("lineMod") { HashMap() }

        val errorFunc = object : org.luaj.vm2.LuaFunction() {
            override fun call(arg: org.luaj.vm2.LuaValue): org.luaj.vm2.LuaValue =
                throw org.luaj.vm2.LuaError("myScript.lua:7: attempt to call a nil value (global 'addGoldd')")
        }
        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), "lineMod")
        LuaScriptManager.callFunction(errorFunc, ctx, civ, "myFunc", onSuccess = {}, modName = "lineMod")

        val newAlert = civ.popupAlerts.lastOrNull()
        Assert.assertNotNull("PopupAlert should be created", newAlert)
        Assert.assertTrue("Popup should contain function name: ${newAlert?.value}",
            newAlert?.value?.contains("myFunc") == true)
        Assert.assertTrue("Popup should contain line number: ${newAlert?.value}",
            newAlert?.value?.contains("line 7") == true)
    }

    @Test
    fun runtimeErrorsRecordedForNonHumanCiv() {
        // AI-triggered Lua errors previously only went to the log; now they must be
        // recorded in the ruleset error list so mod authors see them in the mod checker
        val civ = testGame.addCiv(isPlayer = false)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        val popupCountBefore = civ.popupAlerts.size
        val ruleset = civ.gameInfo.ruleset
        val errorCountBefore = ruleset.luaErrors.size

        val errorFunc = object : org.luaj.vm2.LuaFunction() {
            override fun call(): org.luaj.vm2.LuaValue =
                throw org.luaj.vm2.LuaError("aiScript.lua:3: boom")
        }
        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), "aiMod")
        LuaScriptManager.callFunction(errorFunc, ctx, civ, "aiFunc", onSuccess = {}, modName = "aiMod")

        val newErrors = ruleset.luaErrors.filter { it.severity == LuaScriptErrorSeverity.WARNING }
        Assert.assertTrue(
            "AI civ runtime Lua error should be recorded for the mod checker, got: ${newErrors.map { it.message }}",
            newErrors.any { it.message.contains("aiFunc") }
        )
        // Non-human civ must not get a Lua error popup (nothing to show to)
        Assert.assertEquals("No Lua popup should be created for AI civ", popupCountBefore, civ.popupAlerts.size)
        Assert.assertTrue("No LuaError alert for AI civ",
            civ.popupAlerts.none { it.type == com.unciv.logic.civilization.AlertType.LuaError })
        Assert.assertTrue("ruleset.luaErrors should have grown", ruleset.luaErrors.size > errorCountBefore)
    }

    //endregion
}
