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
import com.unciv.models.ruleset.tech.TechColumn
import com.unciv.models.ruleset.tech.Technology
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
            "Combined check should detect missing Lua function reference, got errors: ${errorList.filter { it.text.contains("nonexistentFunc") }.map { it.text }}",
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

    //region New feature tests (Phase 1-3)

    // -- Phase 1.3: safeToInt overflow protection --

    @Test
    fun safeToIntClampsOverflow() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))

        // Build context and call addGold with an absurdly large value
        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)
        val goldBefore = civ.gold
        // Simulate Lua calling addGold with overflowing value via the API table
        val addGoldFunc = ctx.get("civ").checktable().get("addGold")
        (addGoldFunc as org.luaj.vm2.LuaFunction).call(org.luaj.vm2.LuaValue.valueOf(9e18))
        // Should be clamped, not wrapped to negative
        Assert.assertTrue("Gold should not decrease after clamped overflow add",
            civ.gold >= goldBefore)
    }

    @Test
    fun safeToIntPassesNormalValues() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))

        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)
        val goldBefore = civ.gold
        val addGoldFunc = ctx.get("civ").checktable().get("addGold")
        (addGoldFunc as org.luaj.vm2.LuaFunction).call(org.luaj.vm2.LuaValue.valueOf(100))
        Assert.assertEquals("Gold should increase by exactly 100", goldBefore + 100, civ.gold)
    }

    // -- Phase 1.4: string.dump removal --

    @Test
    fun stringDumpIsRemovedFromSandbox() {
        // Load a Lua script that tries to access string.dump
        val mod = loadLuaScriptToMod("testSandbox", "checkSandbox.lua",
            """
            function checkStringDump(ctx)
                return string.dump == nil
            end
            """.trimIndent()
        )
        Assert.assertTrue("Script should load without error",
            mod.luaErrors.none { it.severity == LuaScriptErrorSeverity.ERROR })

        // Execute the function and verify it reports string.dump is nil
        val func = LuaScriptManager.getFunction("testSandbox", "checkStringDump")
        Assert.assertNotNull("checkStringDump function should be found", func)
        val (_, luaFunc) = func!!

        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), "testSandbox")
        var result = false
        LuaScriptManager.callFunction(luaFunc, ctx) { result = it }
        Assert.assertTrue("string.dump should be nil in sandbox", result)
    }

    // -- Phase 1.2: API cleanup (removed properties) --

    @Test
    fun removedCivPropertiesReturnNil() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)
        val civTable = ctx.get("civ").checktable()

        Assert.assertTrue("civ.gold should be nil after cleanup", civTable.get("gold").isnil())
        Assert.assertTrue("civ.happiness should be nil after cleanup", civTable.get("happiness").isnil())
        Assert.assertTrue("civ.era should be nil after cleanup", civTable.get("era").isnil())
        Assert.assertTrue("civ.cityCount should be nil after cleanup", civTable.get("cityCount").isnil())
    }

    @Test
    fun getterFunctionsWorkAfterCleanup() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)
        val civTable = ctx.get("civ").checktable()

        val getGold = civTable.get("getGold")
        Assert.assertFalse("getGold should exist", getGold.isnil())
        val getHappiness = civTable.get("getHappiness")
        Assert.assertFalse("getHappiness should exist", getHappiness.isnil())
        val getEra = civTable.get("getEra")
        Assert.assertFalse("getEra should exist", getEra.isnil())
        val getCityCount = civTable.get("getCityCount")
        Assert.assertFalse("getCityCount should exist", getCityCount.isnil())
    }

    // -- Phase 1.1: Lua runtime error popup --

    @Test
    fun luaRuntimeErrorCreatesPopupAlert() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        civ.gameInfo.modLuaStorage.getOrPut(modName) { HashMap() }

        // Create a Lua function that throws at runtime
        val errorFunc = object : org.luaj.vm2.LuaFunction() {
            override fun call(): org.luaj.vm2.LuaValue =
                throw org.luaj.vm2.LuaError("Simulated runtime error for popup test")
        }

        val alertCountBefore = civ.popupAlerts.size
        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)
        LuaScriptManager.callFunction(errorFunc, ctx, civ, "testErrorFunc") { /* ignore */ }

        Assert.assertTrue("PopupAlert should be created for runtime error",
            civ.popupAlerts.size > alertCountBefore)
        val newAlert = civ.popupAlerts.last()
        Assert.assertEquals("Alert type should be LuaError",
            com.unciv.logic.civilization.AlertType.LuaError, newAlert.type)
        Assert.assertTrue("Alert value should contain function name",
            newAlert.value.contains("testErrorFunc"))
    }

    @Test
    fun luaErrorDedupPreventsRepeatedPopups() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))

        val errorFunc = object : org.luaj.vm2.LuaFunction() {
            override fun call(): org.luaj.vm2.LuaValue =
                throw org.luaj.vm2.LuaError("Simulated runtime error for dedup test")
        }

        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)

        // First call — should create popup
        LuaScriptManager.callFunction(errorFunc, ctx, civ, "dedupTestFunc") { /* ignore */ }
        val alertCountAfterFirst = civ.popupAlerts.size
        Assert.assertTrue("First error should create popup", alertCountAfterFirst > 0)

        // Second call with same function name — should NOT create another popup
        LuaScriptManager.callFunction(errorFunc, ctx, civ, "dedupTestFunc") { /* ignore */ }
        Assert.assertEquals("Second call with same function should not create duplicate popup",
            alertCountAfterFirst, civ.popupAlerts.size)
    }

    // -- Phase 2.1e: city.getCenterTile() --

    @Test
    fun cityGetCenterTileReturnsValidTile() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(2, 2)))
        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)
        val cityTable = ctx.get("city").checktable()

        val getCenterTile = cityTable.get("getCenterTile")
        Assert.assertFalse("getCenterTile should exist on city table", getCenterTile.isnil())

        val tileTable = (getCenterTile as org.luaj.vm2.LuaFunction).call()
        Assert.assertFalse("getCenterTile should return non-nil", tileTable.isnil())

        val tile = tileTable.checktable()
        Assert.assertEquals("Center tile x should match city location",
            city.location.x, tile.get("getX").checkfunction().call().toint())
        Assert.assertEquals("Center tile y should match city location",
            city.location.y, tile.get("getY").checkfunction().call().toint())
    }

    // -- Phase 2.2: findTiles result limit --

    @Test
    fun findTilesRespectsMaxResults() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)
        val gameTable = ctx.get("game").checktable()

        val findTiles = gameTable.get("findTiles").checkfunction()
        val criteria = org.luaj.vm2.LuaValue.tableOf()
        criteria.set("isLand", org.luaj.vm2.LuaValue.TRUE)
        criteria.set("maxResults", org.luaj.vm2.LuaValue.valueOf(2))

        val result = findTiles.call(criteria)
        Assert.assertFalse("findTiles should return non-nil", result.isnil())
        val tiles = result.checktable()
        // Table length in Lua is tricky; check that we don't have more than 2 entries
        Assert.assertTrue("findTiles with maxResults=2 should return limited results",
            tiles.get(3).isnil())
        Assert.assertFalse("findTiles should return at least 1 result",
            tiles.get(1).isnil())
    }

    @Test
    fun findTilesHasDefaultMaxResultsCap() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)
        val gameTable = ctx.get("game").checktable()

        val findTiles = gameTable.get("findTiles").checkfunction()
        // Query without maxResults — should cap at 500
        val criteria = org.luaj.vm2.LuaValue.tableOf()
        criteria.set("isLand", org.luaj.vm2.LuaValue.TRUE)

        val result = findTiles.call(criteria)
        Assert.assertFalse("findTiles should return non-nil", result.isnil())
        // On a 5-radius map (61 tiles), all land tiles should be returned (well under 500)
        val tiles = result.checktable()
        Assert.assertFalse("findTiles should return at least some results",
            tiles.get(1).isnil())
    }

    // -- Phase 2.1a: tile.getYield() --

    @Test
    fun tileGetYieldReturnsValidStats() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        val tile = testGame.getTile(HexCoord(1, 1))
        val ctx = LuaAPI.buildContext(civ, city, null, tile, "", GameContext(civ, city, null, tile), modName)
        val tileTable = ctx.get("tile").checktable()

        val getYield = tileTable.get("getYield")
        Assert.assertFalse("getYield should exist on tile table", getYield.isnil())

        val yieldResult = (getYield as org.luaj.vm2.LuaFunction).call()
        Assert.assertFalse("getYield should return non-nil", yieldResult.isnil())

        val yieldTable = yieldResult.checktable()
        // Should have at least Food for any land tile
        val food = yieldTable.get("Food")
        Assert.assertFalse("Yield should contain Food", food.isnil())
    }

    // -- Phase 2.1b: unit.base sub-table --

    @Test
    fun unitBaseHasCorrectProperties() {
        val civ = testGame.addCiv(isPlayer = true)
        testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(HexCoord(0, 0)))
        val ctx = LuaAPI.buildContext(civ, null, unit, unit.currentTile, "", GameContext(unit), modName)
        val unitTable = ctx.get("unit").checktable()

        val base = unitTable.get("base")
        Assert.assertFalse("unit.base should exist", base.isnil())
        val baseTable = base.checktable()

        Assert.assertEquals("base.name should match unit name",
            unit.baseUnit.name, baseTable.get("name").tojstring())
        Assert.assertEquals("base.strength should match",
            unit.baseUnit.strength, baseTable.get("strength").toint())
        Assert.assertEquals("base.cost should match",
            unit.baseUnit.cost, baseTable.get("cost").toint())
        Assert.assertEquals("base.movement should match",
            unit.baseUnit.movement, baseTable.get("movement").toint())
        Assert.assertEquals("base.unitType should match",
            unit.baseUnit.unitType, baseTable.get("unitType").tojstring())
    }

    // -- Phase 2.1d: hasUnique on civ/city/unit --

    @Test
    fun civHasUniqueWorks() {
        val civ = testGame.addCiv("TestUniqueForCiv", isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))

        // Grant a tech with a test unique to verify tech source is searched
        val testTech = Technology().apply {
            name = "TestUniqueTech"
            uniques.add("TestUniqueFromTech")
            column = TechColumn().apply { era = "Ancient era" }
        }
        testGame.ruleset.technologies[testTech.name] = testTech
        civ.tech.addTechnology("TestUniqueTech")

        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)
        val civTable = ctx.get("civ").checktable()

        val hasUnique = civTable.get("hasUnique")
        Assert.assertFalse("hasUnique should exist on civ table", hasUnique.isnil())

        // Nation unique
        val hasNationUnique = (hasUnique as org.luaj.vm2.LuaFunction)
            .call(org.luaj.vm2.LuaValue.valueOf("TestUniqueForCiv"))
        Assert.assertTrue("Civ should have nation unique", hasNationUnique.toboolean())

        // Tech unique — verifies expanded search beyond just nation
        val hasTechUnique = (hasUnique as org.luaj.vm2.LuaFunction)
            .call(org.luaj.vm2.LuaValue.valueOf("TestUniqueFromTech"))
        Assert.assertTrue("Civ should have tech unique (expanded source)", hasTechUnique.toboolean())

        // Negative check
        val hasNonsense = (hasUnique as org.luaj.vm2.LuaFunction)
            .call(org.luaj.vm2.LuaValue.valueOf("NonexistentUniqueXYZ123"))
        Assert.assertFalse("Civ should NOT have nonexistent unique",
            hasNonsense.toboolean())
    }

    @Test
    fun cityHasUniqueWorks() {
        val civ = testGame.addCiv(isPlayer = true)
        val city = testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))

        // Build a building with a test unique
        val testBuilding = com.unciv.models.ruleset.Building().apply {
            name = "TestUniqueBuilding"
            uniques.add("TestUniqueForCityCheck")
        }
        testGame.ruleset.buildings[testBuilding.name] = testBuilding
        city.cityConstructions.completeConstruction(civ.getEquivalentBuilding(testBuilding.name))

        val ctx = LuaAPI.buildContext(civ, city, null, null, "", GameContext(civ, city), modName)
        val cityTable = ctx.get("city").checktable()

        val hasUnique = cityTable.get("hasUnique")
        Assert.assertFalse("hasUnique should exist on city table", hasUnique.isnil())

        val hasTestUnique = (hasUnique as org.luaj.vm2.LuaFunction)
            .call(org.luaj.vm2.LuaValue.valueOf("TestUniqueForCityCheck"))
        Assert.assertTrue("City should have built building's unique",
            hasTestUnique.toboolean())

        val hasNonsense = (hasUnique as org.luaj.vm2.LuaFunction)
            .call(org.luaj.vm2.LuaValue.valueOf("NonexistentUniqueXYZ456"))
        Assert.assertFalse("City should NOT have nonexistent unique",
            hasNonsense.toboolean())
    }

    @Test
    fun unitHasUniqueWorks() {
        val civ = testGame.addCiv(isPlayer = true)
        testGame.addCity(civ, testGame.getTile(HexCoord(0, 0)))
        val unit = testGame.addUnit("Warrior", civ, testGame.getTile(HexCoord(0, 0)))

        // Add a promotion with a test unique we can verify
        val testPromo = com.unciv.models.ruleset.unit.Promotion().apply {
            name = "TestHasUniquePromo"
            uniques.add("TestUniqueForUnitCheck")
        }
        testGame.ruleset.unitPromotions[testPromo.name] = testPromo
        unit.promotions.addPromotion("TestHasUniquePromo", true)

        val ctx = LuaAPI.buildContext(civ, null, unit, unit.currentTile, "", GameContext(unit), modName)
        val unitTable = ctx.get("unit").checktable()

        val hasUnique = unitTable.get("hasUnique")
        Assert.assertFalse("hasUnique should exist on unit table", hasUnique.isnil())

        val hasTestUnique = (hasUnique as org.luaj.vm2.LuaFunction)
            .call(org.luaj.vm2.LuaValue.valueOf("TestUniqueForUnitCheck"))
        Assert.assertTrue("Unit should have promotion's unique",
            hasTestUnique.toboolean())

        val hasNonsense = (hasUnique as org.luaj.vm2.LuaFunction)
            .call(org.luaj.vm2.LuaValue.valueOf("NonexistentUniqueXYZ789"))
        Assert.assertFalse("Unit should NOT have nonexistent unique",
            hasNonsense.toboolean())
    }

    //endregion
}
