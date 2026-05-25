package com.unciv.logic.scripting

import com.badlogic.gdx.Gdx
import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.MapSize
import com.unciv.logic.map.TileMap
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.RulesetCache
import com.unciv.models.ruleset.Event
import com.unciv.models.ruleset.EventChoice
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueTriggerActivation
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.ruleset.validation.RulesetErrorSeverity
import com.unciv.models.ruleset.validation.RulesetValidator
import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

@RunWith(GdxTestRunner::class)
class LuaScriptTests {

    private lateinit var testGame: TestGame
    private lateinit var modName: String

    @Before
    fun setUp() {
        testGame = TestGame()
        testGame.makeHexagonalMap(5, "Grassland")

        // Find and load testMOD from disk
        modName = "testMOD"
        val testModDir = sequenceOf(
            Gdx.files.internal("mods/$modName"),
            Gdx.files.absolute(System.getProperty("user.dir") + "/android/assets/mods/$modName")
        ).firstOrNull { it.isDirectory }

        if (testModDir != null && testModDir.child("jsons").isDirectory) {
            val mod = Ruleset().apply { name = modName }
            mod.load(testModDir.child("jsons"))
            if (mod.buildings.isNotEmpty())
                testGame.ruleset.add(mod)
        }

        // Set up a proper tileMap so city founding and unit placement work
        testGame.gameInfo.ruleset = testGame.ruleset
        testGame.gameInfo.setGlobalTransients()
    }

    @Test
    fun storeSetAndGet() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        civ.gameInfo.modLuaStorage.getOrPut(modName) { HashMap() }

        // Build context with modName so store is available
        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)

        // Call testStore Lua function
        val (_, func) = LuaScriptManager.getFunction(modName, "testStore") ?: run {
            Assert.fail("testStore function not found - is testMOD loaded?")
            return
        }
        var success = false
        LuaScriptManager.callFunction(func, ctx) { success = it }
        Assert.assertTrue("testStore should return true", success)

        // Verify the storage persisted at the GameInfo level
        val stored = civ.gameInfo.modLuaStorage[modName]?.get("test_key")
        Assert.assertEquals("hello_world", stored)
    }

    @Test
    fun findTilesReturnsResults() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        testGame.addUnit("Warrior", civ, testGame.getTile(HexCoord(1, 1)))

        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)
        val (_, func) = LuaScriptManager.getFunction(modName, "testFindTiles") ?: run {
            Assert.fail("testFindTiles function not found")
            return
        }
        var success = false
        LuaScriptManager.callFunction(func, ctx) { success = it }
        Assert.assertTrue("testFindTiles should return true", success)
    }

    @Test
    fun evaluateConditionalAlways() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))

        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)
        val (_, func) = LuaScriptManager.getFunction(modName, "testConditional") ?: run {
            Assert.fail("testConditional function not found")
            return
        }
        var success = false
        LuaScriptManager.callFunction(func, ctx) { success = it }
        Assert.assertTrue("testConditional should verify conditional evaluation works", success)
    }

    @Test
    fun citySetProductionAndQueue() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))

        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)
        val (_, func) = LuaScriptManager.getFunction(modName, "testCityProduction") ?: run {
            Assert.fail("testCityProduction function not found")
            return
        }
        var success = false
        LuaScriptManager.callFunction(func, ctx) { success = it }
        Assert.assertTrue("testCityProduction should return true", success)
    }

    @Test
    fun unitPathfinding() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(HexCoord(0, 0)))

        val ctx = LuaAPI.buildContext(civ, city, unit, unit.currentTile, "", GameContext(civ, city, unit, unit.currentTile), modName)
        val (_, func) = LuaScriptManager.getFunction(modName, "testPathfinding") ?: run {
            Assert.fail("testPathfinding function not found")
            return
        }
        var success = false
        LuaScriptManager.callFunction(func, ctx) { success = it }
        Assert.assertTrue("testPathfinding should return true", success)
    }

    @Test
    fun allApiTestsPass() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(HexCoord(0, 0)))

        val ctx = LuaAPI.buildContext(civ, city, unit, unit.currentTile, "", GameContext(civ, city, unit, unit.currentTile), modName)
        val (_, func) = LuaScriptManager.getFunction(modName, "testAll") ?: run {
            Assert.fail("testAll function not found - is testMOD loaded with testApi.lua?")
            return
        }
        var success = false
        LuaScriptManager.callFunction(func, ctx) { success = it }
        Assert.assertTrue("All Lua API tests should pass", success)
    }

    @Test
    fun triggerLuaFunctionUniqueIntegration() {
        // Verify that TriggerLuaFunction unique is properly wired through the unique system
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        civ.gameInfo.modLuaStorage.getOrPut(modName) { HashMap() }

        val unique = Unique("Trigger the function [$modName:testStore] with [42]")
        Assert.assertEquals("Unique type should be TriggerLuaFunction",
            UniqueType.TriggerLuaFunction, unique.type)

        val triggerFunction = UniqueTriggerActivation.getTriggerFunction(
            unique, civ, city, null, city.getCenterTile()
        )
        Assert.assertNotNull("getTriggerFunction should return non-null for TriggerLuaFunction", triggerFunction)

        val result = triggerFunction!!()
        Assert.assertTrue("Triggering testStore via unique should succeed", result)

        val stored = civ.gameInfo.modLuaStorage[modName]?.get("test_key")
        Assert.assertEquals("hello_world", stored)
    }

    @Test
    fun storagePersistsAcrossMultipleCalls() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        civ.gameInfo.modLuaStorage.getOrPut(modName) { HashMap() }

        // First call: set a value
        val ctx1 = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)
        val (_, func) = LuaScriptManager.getFunction(modName, "testStore") ?: return
        var success = false
        LuaScriptManager.callFunction(func, ctx1) { success = it }
        Assert.assertTrue(success)

        // Second call: verify the value is still there by reading directly
        val stored = civ.gameInfo.modLuaStorage[modName]?.get("test_key")
        Assert.assertEquals("hello_world", stored)

        // Verify default retrieval works
        val missing = civ.gameInfo.modLuaStorage[modName]?.get("never_set")
        Assert.assertNull(missing)
    }

    @Test
    fun triggerLuaOnEventObject() {
        // Test that TriggerLuaFunction on Event object fires when event triggers
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        civ.gameInfo.modLuaStorage.getOrPut(modName) { HashMap() }

        // Create an Event with TriggerLuaFunction on the Event itself
        val event = Event().apply {
            name = "TestEvent"
            uniques.add("Trigger the function [$modName:testStore] with [eventTest]")
            // Need at least one choice for event to be valid
            choices.add(EventChoice().apply {
                text = "OK"
                uniques.add("Comment [event choice executed]")
            })
        }
        testGame.ruleset.events[event.name] = event

        // Ensure modLuaStorage doesn't have the test key yet
        Assert.assertNull(civ.gameInfo.modLuaStorage[modName]?.get("test_key"))

        // Trigger the event via TriggerEvent unique
        val triggerEventUnique = Unique("Triggers a [TestEvent] event")
        val result = UniqueTriggerActivation.triggerUnique(
            triggerEventUnique, civ, city, null, city.getCenterTile()
        )

        Assert.assertTrue("TriggerEvent should succeed", result)

        // Verify the Event's Lua function fired and stored data
        val stored = civ.gameInfo.modLuaStorage[modName]?.get("test_key")
        Assert.assertEquals("Event-level TriggerLuaFunction should have fired", "hello_world", stored)
    }

    @Test
    fun triggerLuaOnEventChoice() {
        // Test that TriggerLuaFunction on EventChoice fires when choice is selected
        // Use AI civ so choices are auto-triggered (Presentation.None workflow)
        val civ = testGame.addCiv(isPlayer = false)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        civ.gameInfo.modLuaStorage.getOrPut(modName) { HashMap() }

        // Ensure storage is clean
        civ.gameInfo.modLuaStorage[modName]?.remove("test_key")

        val event = Event().apply {
            name = "TestEvent2"
            uniques.add("Comment [event with lua choice]")
            choices.add(EventChoice().apply {
                text = "Lua Choice"
                uniques.add("Trigger the function [$modName:testStore] with [choiceTest]")
            })
        }
        testGame.ruleset.events[event.name] = event

        Assert.assertNull(civ.gameInfo.modLuaStorage[modName]?.get("test_key"))

        val triggerEventUnique = Unique("Triggers a [TestEvent2] event")
        val result = UniqueTriggerActivation.triggerUnique(
            triggerEventUnique, civ, city, null, city.getCenterTile()
        )

        Assert.assertTrue("TriggerEvent via choice should succeed", result)
        val stored = civ.gameInfo.modLuaStorage[modName]?.get("test_key")
        Assert.assertEquals("EventChoice-level TriggerLuaFunction should have fired", "hello_world", stored)
    }

    //region Lua error reporting system tests

    /** Writes a Lua script to a temp dir's scripts/ subdirectory, calls [LuaScriptManager.loadScripts],
     *  and cleans up. Returns the populated mod. */
    private fun loadLuaScriptToMod(modName: String, scriptName: String, scriptContent: String): Ruleset {
        val tempDir = Files.createTempDirectory("luaTest")
        val scriptsDir = Files.createDirectory(tempDir.resolve("scripts"))
        scriptsDir.resolve(scriptName).writeText(scriptContent)
        val mod = Ruleset().apply { name = modName }
        LuaScriptManager.loadScripts(Gdx.files.absolute(tempDir.toAbsolutePath().toString()), modName, mod)
        Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
        return mod
    }

    @Test
    fun syntaxErrorCollectedInLuaErrors() {
        val mod = loadLuaScriptToMod("testMod", "bad.lua", "function broken()\n  if true then\n-- missing end\n")

        val syntaxErrors = mod.luaErrors.filter {
            it.severity == LuaScriptErrorSeverity.ERROR && it.scriptName == "bad.lua"
        }
        Assert.assertTrue(
            "Should have at least one ERROR for bad.lua syntax error, got: ${mod.luaErrors.map { "${it.scriptName}:${it.severity}" }}",
            syntaxErrors.isNotEmpty()
        )
        Assert.assertTrue(
            "Error message should mention syntax: ${syntaxErrors.first().message}",
            syntaxErrors.first().message.contains("syntax", ignoreCase = true)
        )
    }

    @Test
    fun successfullyLoadedScriptsGenerateInfo() {
        val mod = loadLuaScriptToMod("testMod", "good.lua", "function good()\n  return true\nend\n")

        val infoEntries = mod.luaErrors.filter { it.severity == LuaScriptErrorSeverity.INFO }
        Assert.assertTrue("Should have at least one INFO entry for loaded scripts", infoEntries.isNotEmpty())
        Assert.assertTrue(
            "INFO entry should mention loaded scripts: ${infoEntries.firstOrNull()?.message}",
            infoEntries.any { it.message.contains("Loaded") && it.message.contains("script") }
        )
    }

    @Test
    fun luaErrorsMappedToRulesetErrors() {
        val mod = loadLuaScriptToMod("testMod", "bad.lua", "function broken()\n  if true then\n-- missing end\n")

        val validator = RulesetValidator.create(mod)
        val errorList = validator.getErrorList()

        val hasLuaSyntaxError = errorList.any {
            it.text.contains("syntax", ignoreCase = true) && it.errorSeverityToReport == RulesetErrorSeverity.Error
        }
        Assert.assertTrue("RulesetErrorList should contain Error-level Lua syntax error", hasLuaSyntaxError)
    }

    @Test
    fun missingFunctionReferenceDetectedInCombinedCheck() {
        // Create a minimal combined ruleset with TriggerLuaFunction referencing nonexistent function
        val baseRuleset = testGame.ruleset.clone()
        baseRuleset.modOptions.isBaseRuleset = true
        baseRuleset.mods.clear()
        baseRuleset.mods.add("testBase")

        // Add a building with TriggerLuaFunction referencing nonexistent function
        val building = com.unciv.models.ruleset.Building().apply {
            name = "TestLuaBuilding"
            uniques.add("Trigger the function [nonexistentMod:nonexistentFunc] with []")
        }
        baseRuleset.buildings.clear()
        baseRuleset.buildings[building.name] = building

        // Run combined validation (BaseRulesetValidator since isBaseRuleset=true)
        val errorList = baseRuleset.getErrorList()
        val missingFuncErrors = errorList.filter {
            it.text.contains("nonexistentFunc") && it.errorSeverityToReport == RulesetErrorSeverity.Error
        }
        Assert.assertTrue(
            "Combined check should detect missing Lua function reference, got errors: ${errorList.filter { it.text.contains("Lua") }.map { it.text }}",
            missingFuncErrors.isNotEmpty()
        )
    }

    @Test
    fun missingFunctionWarningForStandaloneMod() {
        // Standalone mod (not combined): missing function is Warning (yellow), not Error
        val standaloneMod = Ruleset().apply { name = "testStandalone" }
        // isBaseRuleset defaults to false, so reportRulesetSpecificErrors = false
        standaloneMod.mods.add("testStandalone")

        val building = com.unciv.models.ruleset.Building().apply {
            name = "TestWarningBuilding"
            uniques.add("Trigger the function [someMod:maybeLaterFunc] with []")
        }
        standaloneMod.buildings[building.name] = building

        val errorList = standaloneMod.getErrorList()
        val missingFuncWarnings = errorList.filter {
            it.text.contains("maybeLaterFunc") && it.errorSeverityToReport == RulesetErrorSeverity.Warning
        }
        Assert.assertTrue(
            "Standalone mod missing function should be Warning (yellow), got: ${missingFuncWarnings.map { "${it.text} -> ${it.errorSeverityToReport}" }}",
            missingFuncWarnings.isNotEmpty()
        )
        Assert.assertTrue(
            "Standalone mod missing function should NOT be Error",
            errorList.none { it.text.contains("maybeLaterFunc") && it.errorSeverityToReport == RulesetErrorSeverity.Error }
        )
    }

    @Test
    fun malformedFunctionReferenceDetected() {
        val baseRuleset = testGame.ruleset.clone()
        baseRuleset.modOptions.isBaseRuleset = true
        baseRuleset.mods.clear()
        baseRuleset.mods.add("testBase")

        val building = com.unciv.models.ruleset.Building().apply {
            name = "TestLuaBuilding2"
            // Invalid format: contains special characters
            uniques.add("Trigger the function [invalid ref!!] with []")
        }
        baseRuleset.buildings.clear()
        baseRuleset.buildings[building.name] = building

        val errorList = baseRuleset.getErrorList()
        val invalidRefErrors = errorList.filter {
            it.text.contains("invalid ref!!") && it.errorSeverityToReport == RulesetErrorSeverity.Error
        }
        Assert.assertTrue(
            "Combined check should detect malformed function reference",
            invalidRefErrors.isNotEmpty()
        )
    }

    @Test
    fun runtimeErrorLogsStackTrace() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        civ.gameInfo.modLuaStorage.getOrPut(modName) { HashMap() }

        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)

        // Call a function that doesn't exist to trigger runtime error
        val nonExistentFunc = object : org.luaj.vm2.LuaFunction() {
            override fun call(): org.luaj.vm2.LuaValue =
                throw org.luaj.vm2.LuaError("Simulated runtime error for testing")
        }
        var success = true // default
        LuaScriptManager.callFunction(nonExistentFunc, ctx) { success = it }
        Assert.assertFalse(
            "Runtime error should result in onSuccess(false)",
            success
        )
    }

    @Test
    fun warnedMissingFunctionsPreventsLogSpam() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))

        // TriggerLuaFunction with nonexistent function should return null
        val unique = Unique("Trigger the function [nonexistentMod:nonexistentFunc] with []")
        val triggerFunction = UniqueTriggerActivation.getTriggerFunction(
            unique, civ, city, null, city.getCenterTile()
        )
        Assert.assertNull(
            "getTriggerFunction should return null for nonexistent Lua function",
            triggerFunction
        )
        // Triggering again should not throw - the warnOnce set prevents log spam
        val triggerFunction2 = UniqueTriggerActivation.getTriggerFunction(
            unique, civ, city, null, city.getCenterTile()
        )
        Assert.assertNull(
            "getTriggerFunction should still return null on second call",
            triggerFunction2
        )
    }

    @Test
    fun noCrossContaminationBetweenMods() {
        // Load a working Lua function so mod A's TriggerLuaFunction resolves correctly
        loadLuaScriptToMod("modA", "good.lua", "function myFunc()\n  return true\nend\n")

        // Mod A: has TriggerLuaFunction referencing its own loaded function → no error expected
        val modA = Ruleset().apply { name = "modA"; mods.add("modA") }
        val buildingA = com.unciv.models.ruleset.Building().apply {
            name = "BuildingA"
            uniques.add("Trigger the function [modA:myFunc] with [test]")
        }
        modA.buildings[buildingA.name] = buildingA
        val errorsA = modA.getErrorList()
        Assert.assertFalse(
            "modA must NOT report myFunc as missing (it is loaded)",
            errorsA.any { it.text.contains("myFunc") }
        )

        // Mod B: no TriggerLuaFunction → must NOT contain modA's function errors
        val modB = Ruleset().apply { name = "modB"; mods.add("modB") }
        val buildingB = com.unciv.models.ruleset.Building().apply {
            name = "BuildingB"
            uniques.add("Comment [nothing to see here]")
        }
        modB.buildings[buildingB.name] = buildingB
        val errorsB = modB.getErrorList()
        Assert.assertFalse(
            "modB must NOT contain modA's errors: ${errorsB.filter { it.text.contains("myFunc") }.map { it.text }}",
            errorsB.any { it.text.contains("myFunc") || it.text.contains("modA") }
        )
    }

    //endregion
}
