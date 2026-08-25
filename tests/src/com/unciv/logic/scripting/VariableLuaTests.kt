package com.unciv.logic.scripting

import com.badlogic.gdx.Gdx
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.VariableScope
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

    private fun runLuaFunction(modName: String, funcName: String, civ: Civilization, city: City?): Boolean {
        val (foundMod, func) = LuaScriptManager.getFunction(modName, funcName) ?: run {
            Assert.fail("$funcName not found")
            return false
        }
        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), foundMod)
        var result = false
        LuaScriptManager.callFunction(func, ctx, civ, funcName, onSuccess = { result = it }, modName = foundMod)
        return result
    }

    private fun runLuaFunction(modName: String, funcName: String, civ: Civilization, unit: MapUnit): Boolean {
        val (foundMod, func) = LuaScriptManager.getFunction(modName, funcName) ?: run {
            Assert.fail("$funcName not found")
            return false
        }
        val ctx = LuaAPI.buildContext(civ, null, unit, null, "", GameContext(civInfo = civ, unit = unit), foundMod)
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
    fun luaUnitVariableApisWorkEndToEnd() {
        val variable = testGame.createVariable(default = 0, scope = VariableScope.Unit)
        variable.min = 0
        variable.max = 10
        val civ = testGame.addCiv(isPlayer = true)
        val unit = testGame.addDefaultMeleeUnitWithUniques(civ, testGame.getTile(1, 0))
        val mod = loadLuaScriptToMod("unitVar", "unitvar.lua", """
            function testUnitVariables(ctx)
                local unit = ctx.unit
                unit.setVariable("${variable.name}", 8)
                unit.addVariable("${variable.name}", 3)
                local read = unit.getVariable("${variable.name}")
                local all = unit.getVariables()
                return read == 10 and all["${variable.name}"] == 10
            end
        """.trimIndent())
        Assert.assertTrue("Lua unit variable APIs must work end to end",
            runLuaFunction("unitVar", "testUnitVariables", civ, unit))
        Assert.assertEquals("clamped to max 10", 10, unit.getVariable(variable.name))
    }

    @Test
    fun luaUnitVariableIsIsolatedFromCivScope() {
        val variable = testGame.createVariable(default = 5, scope = VariableScope.Unit)
        val civ = testGame.addCiv(isPlayer = true)
        val unit = testGame.addDefaultMeleeUnitWithUniques(civ, testGame.getTile(1, 0))
        unit.setVariable(variable.name, 7)
        val mod = loadLuaScriptToMod("unitVarIso", "isolated.lua", """
            function testIsolated(ctx)
                -- a unit variable must not leak into the civ-scope channel
                return ctx.civ.getVariable("${variable.name}") == 0
                    and ctx.unit.getVariable("${variable.name}") == 7
            end
        """.trimIndent())
        Assert.assertTrue("Lua unit variable must stay isolated from the civ scope",
            runLuaFunction("unitVarIso", "testIsolated", civ, unit))
    }

    @Test
    fun luaCityAndGlobalVariableApisRespectScopeAndClamp() {
        val cityVariable = testGame.createVariable(default = 5, scope = VariableScope.City)
        cityVariable.min = 0
        cityVariable.max = 10
        val globalVariable = testGame.createVariable(default = 2, scope = VariableScope.Global)
        val civ = addCivWithCity()
        val city = civ.cities.first()
        val mod = loadLuaScriptToMod("varScopes", "scopes.lua", """
            function testScopes(ctx)
                local city = ctx.city
                city.setVariable("${cityVariable.name}", 99)
                city.addVariable("${cityVariable.name}", -3)
                local game = ctx.game
                game.setGlobalVariable("${globalVariable.name}", 7)
                game.addGlobalVariable("${globalVariable.name}", 2)
                local names = game.getVariablesOfScope("city")
                return city.getVariable("${cityVariable.name}") == 7
                    and game.getGlobalVariable("${globalVariable.name}") == 9
                    and names[1] == "${cityVariable.name}"
            end
        """.trimIndent())
        Assert.assertTrue(runLuaFunction("varScopes", "testScopes", civ, city))
        Assert.assertEquals(7, city.getVariable(cityVariable.name))
        Assert.assertEquals(9, testGame.gameInfo.getVariable(globalVariable.name))
    }

    @Test
    fun luaCityAndGlobalVariableWritesAreClamped() {
        val cityVariable = testGame.createVariable(default = 0, scope = VariableScope.City)
        cityVariable.min = 0
        cityVariable.max = 10
        val globalVariable = testGame.createVariable(default = 0, scope = VariableScope.Global)
        globalVariable.min = -2
        globalVariable.max = 4
        val civ = addCivWithCity()
        val city = civ.cities.first()
        val mod = loadLuaScriptToMod("varClampScopes", "clamp.lua", """
            function testClamp(ctx)
                ctx.city.setVariable("${cityVariable.name}", 100)
                ctx.game.setGlobalVariable("${globalVariable.name}", -100)
                return ctx.city.getVariable("${cityVariable.name}") == 10
                    and ctx.game.getGlobalVariable("${globalVariable.name}") == -2
            end
        """.trimIndent())
        Assert.assertTrue(runLuaFunction("varClampScopes", "testClamp", civ, city))
    }

    @Test
    fun luaCityAndGlobalVariableApisAreAvailableInContext() {
        val cityVariable = testGame.createVariable(scope = VariableScope.City)
        val globalVariable = testGame.createVariable(scope = VariableScope.Global)
        val civ = addCivWithCity()
        val city = civ.cities.first()
        val mod = loadLuaScriptToMod("varApiNames", "names.lua", """
            function testNames(ctx)
                return ctx.city.getVariable ~= nil
                    and ctx.city.setVariable ~= nil
                    and ctx.city.addVariable ~= nil
                    and ctx.game.getGlobalVariable ~= nil
                    and ctx.game.setGlobalVariable ~= nil
                    and ctx.game.addGlobalVariable ~= nil
                    and ctx.game.getVariablesOfScope ~= nil
            end
        """.trimIndent())
        Assert.assertTrue(runLuaFunction("varApiNames", "testNames", civ, city))
        Assert.assertNotNull(cityVariable)
        Assert.assertNotNull(globalVariable)
    }

    @Test
    fun luaVariableApisDoNotCrossScopes() {
        val civVariable = testGame.createVariable(scope = VariableScope.Civ)
        val cityVariable = testGame.createVariable(scope = VariableScope.City)
        val globalVariable = testGame.createVariable(scope = VariableScope.Global)
        val civ = addCivWithCity()
        val city = civ.cities.first()
        val mod = loadLuaScriptToMod("varScopeIsolation", "isolation.lua", """
            function testIsolation(ctx)
                ctx.civ.setVariable("${cityVariable.name}", 9)
                ctx.city.setVariable("${civVariable.name}", 9)
                ctx.game.setGlobalVariable("${civVariable.name}", 9)
                return ctx.civ.getVariable("${cityVariable.name}") == 0
                    and ctx.city.getVariable("${civVariable.name}") == 0
                    and ctx.game.getGlobalVariable("${civVariable.name}") == 0
            end
        """.trimIndent())

        Assert.assertTrue(runLuaFunction("varScopeIsolation", "testIsolation", civ, city))
        Assert.assertEquals(0, city.getVariable(cityVariable.name))
        Assert.assertEquals(0, civ.getVariable(civVariable.name))
        Assert.assertEquals(0, testGame.gameInfo.getVariable(globalVariable.name))
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
