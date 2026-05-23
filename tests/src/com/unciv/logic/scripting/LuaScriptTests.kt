package com.unciv.logic.scripting

import com.badlogic.gdx.Gdx
import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.MapSize
import com.unciv.logic.map.TileMap
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.RulesetCache
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueTriggerActivation
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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
}
