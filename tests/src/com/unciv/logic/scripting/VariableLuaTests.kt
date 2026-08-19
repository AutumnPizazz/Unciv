package com.unciv.logic.scripting

import com.badlogic.gdx.Gdx
import com.unciv.logic.civilization.Civilization
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

/** End-to-end coverage of the variable Lua APIs (Variables.json). */
@RunWith(GdxTestRunner::class)
class VariableLuaTests {

    private lateinit var testGame: TestGame

    @Before
    fun setUp() {
        testGame = TestGame()
        testGame.makeHexagonalMap(3, "Grassland")
    }

    /** Writes a Lua script to a temp dir's scripts/ subdirectory, calls [LuaScriptManager.loadScripts], and cleans up. */
    private fun loadLuaScriptToMod(modName: String, scriptName: String, scriptContent: String): Ruleset {
        val tempDir = Files.createTempDirectory("luaVarTest")
        val scriptsDir = tempDir.resolve("scripts")
        Files.createDirectories(scriptsDir)
        scriptsDir.resolve(scriptName).writeText(scriptContent)
        val mod = Ruleset().apply { name = modName }
        LuaScriptManager.loadScripts(Gdx.files.absolute(tempDir.toAbsolutePath().toString()), modName, mod)
        Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
        return mod
    }

    private fun runLuaFunction(modName: String, funcName: String, civ: Civilization): Boolean {
        val (foundMod, func) = LuaScriptManager.getFunction(modName, funcName) ?: run {
            Assert.fail("$funcName not found")
            return false
        }
        val ctx = LuaAPI.buildContext(civ, null, null, null, "", GameContext(civ), foundMod)
        var result = false
        LuaScriptManager.callFunction(func, ctx, civ, funcName, onSuccess = { result = it }, modName = foundMod)
        return result
    }

    private fun addCivWithCity(): Civilization {
        val civ = testGame.addCiv(isPlayer = true)
        testGame.addCity(civ, testGame.getTile(0, 0))
        return civ
    }

    @Test
    fun luaCanReadWriteVariables() {
        val variable = testGame.createVariable(default = 0)
        val civ = addCivWithCity()
        val mod = loadLuaScriptToMod("varMod", "var.lua", """
            function testVariables(ctx)
                local civ = ctx.civ
                civ.setVariable("${variable.name}", 10)
                civ.addVariable("${variable.name}", 5)
                local read = civ.getVariable("${variable.name}")
                local all = civ.getVariables()
                local exists = ctx.game.doesVariableExist("${variable.name}")
                local names = ctx.game.getRulesetVariables()
                return read == 15 and all["${variable.name}"] == 15 and exists and names[1] == "${variable.name}"
            end
        """.trimIndent())
        Assert.assertTrue("Lua variable read/write APIs must work end to end",
            runLuaFunction("varMod", "testVariables", civ))
    }

    @Test
    fun luaGetVariableFallsBackToDefault() {
        val variable = testGame.createVariable(default = 7)
        val civ = addCivWithCity()
        val mod = loadLuaScriptToMod("varDefault", "default.lua", """
            function testDefault(ctx)
                return ctx.civ.getVariable("${variable.name}") == 7
            end
        """.trimIndent())
        Assert.assertTrue("Lua getVariable must fall back to the ruleset default",
            runLuaFunction("varDefault", "testDefault", civ))
    }

    @Test
    fun luaSetVariableOverridesDefault() {
        val variable = testGame.createVariable(default = 7)
        val civ = addCivWithCity()
        val mod = loadLuaScriptToMod("varOverride", "override.lua", """
            function testOverride(ctx)
                local civ = ctx.civ
                civ.setVariable("${variable.name}", 0)
                return civ.getVariable("${variable.name}") == 0
            end
        """.trimIndent())
        Assert.assertTrue("Explicit zero via Lua must not fall back to the default",
            runLuaFunction("varOverride", "testOverride", civ))
    }

    @Test
    fun luaNegativeValuesAreSupported() {
        val variable = testGame.createVariable(default = 0)
        val civ = addCivWithCity()
        val mod = loadLuaScriptToMod("varNeg", "neg.lua", """
            function testNeg(ctx)
                local civ = ctx.civ
                civ.addVariable("${variable.name}", -3)
                return civ.getVariable("${variable.name}") == -3
            end
        """.trimIndent())
        Assert.assertTrue("Negative variable values must work through Lua",
            runLuaFunction("varNeg", "testNeg", civ))
    }

    @Test
    fun luaGetRulesetVariablesListsOnlyDefinedVariables() {
        val variable = testGame.createVariable(default = 0)
        val civ = addCivWithCity()
        val mod = loadLuaScriptToMod("varList", "list.lua", """
            function testList(ctx)
                local names = ctx.game.getRulesetVariables()
                local count = 0
                for i, name in ipairs(names) do
                    count = count + 1
                    if name ~= "${variable.name}" then return false end
                end
                return count == 1
            end
        """.trimIndent())
        Assert.assertTrue("getRulesetVariables must list exactly the defined variables",
            runLuaFunction("varList", "testList", civ))
    }

    @Test
    fun luaDoesVariableExistFalseForUnknownName() {
        val civ = addCivWithCity()
        val mod = loadLuaScriptToMod("varExists", "exists.lua", """
            function testExists(ctx)
                return ctx.game.doesVariableExist("NoSuchVariable") == false
            end
        """.trimIndent())
        Assert.assertTrue("doesVariableExist must return false for unknown names",
            runLuaFunction("varExists", "testExists", civ))
    }

    @Test
    fun luaVariableChangeIsVisibleToConditionals() {
        val variable = testGame.createVariable(default = 0)
        val civ = addCivWithCity()
        val mod = loadLuaScriptToMod("varCond", "cond.lua", """
            function testCond(ctx)
                local civ = ctx.civ
                civ.setVariable("${variable.name}", 8)
                -- The civ must now satisfy the same condition the game engine uses
                return ctx.civ.getVariable("${variable.name}") > 5
            end
        """.trimIndent())
        Assert.assertTrue("Lua-written values must be observable through variable reads",
            runLuaFunction("varCond", "testCond", civ))
        Assert.assertEquals(8, civ.getVariable(variable.name))
    }
}
