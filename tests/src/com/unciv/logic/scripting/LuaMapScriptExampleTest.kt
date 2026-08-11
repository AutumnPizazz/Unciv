package com.unciv.logic.scripting

import com.badlogic.gdx.Gdx
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapSize
import com.unciv.logic.map.TileMap
import com.unciv.logic.map.mapgenerator.MapGenerationRandomness
import com.unciv.models.ruleset.Ruleset
import com.unciv.testing.GdxTestRunner
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end test for the Lua map script example mod shipped in
 * docs/Modders/examples/LuaMapScriptExample/ - the same checks a mod author
 * would hit when using a Lua map script.
 */
@RunWith(GdxTestRunner::class)
class LuaMapScriptExampleTest {

    private fun exampleDir() = run {
        // Locate the repo root by walking up from the bundled testMOD (android/assets/mods/testMOD)
        val testModFile = Gdx.files.internal("mods/testMOD").file().canonicalFile
        val repoRoot = testModFile.parentFile!!.parentFile!!.parentFile!!.parentFile!!
        Gdx.files.absolute("${repoRoot.path}/docs/Modders/examples/LuaMapScriptExample")
    }

    @Test
    fun exampleScriptIsDiscoveredAndGeneratesLand() {
        val dir = exampleDir()
        Assert.assertNotNull("LuaMapScriptExample mod folder not found", dir)

        val testGame = com.unciv.testing.TestGame()
        val mod = testGame.ruleset
        LuaScriptManager.clearMod("LuaMapScriptExample")
        val loaded = LuaScriptManager.loadScripts(dir!!, "LuaMapScriptExample", mod)
        Assert.assertTrue("mapScript.lua should load", loaded.contains("mapScript"))

        // Discovery: the mod must be reported as a map script via GetMapScriptInfo
        val scripts = LuaScriptManager.getMapScripts(mod)
        val info = scripts.firstOrNull { it.modName == "LuaMapScriptExample" }
        Assert.assertNotNull("getMapScripts should discover the example", info)
        Assert.assertTrue("script name should be reported", info!!.name.isNotEmpty())
        Assert.assertEquals("LuaMapScriptExample:GenerateMap", info.ref)

        // Generation: build an all-ocean map, run GenerateMap, expect land
        val mapParameters = mod.mapParametersForTest()
        val randomness = MapGenerationRandomness()
        randomness.seedRNG(12345L)
        val map = TileMap(mapParameters.mapSize.width, mapParameters.mapSize.height, mod, mapParameters.worldWrap)
        map.mapParameters = mapParameters
        for (tile in map.values) {
            tile.baseTerrain = com.unciv.Constants.ocean
            tile.setTerrainTransients()
        }

        val (foundMod, func) = LuaScriptManager.getFunction("LuaMapScriptExample", "GenerateMap")
            ?: run { Assert.fail("GenerateMap not found"); return }
        Assert.assertEquals("LuaMapScriptExample", foundMod)

        val ctx = LuaMapGenAPI.buildMapGenContext(map, mod, randomness)
        var success = false
        LuaScriptManager.callFunction(func, ctx, onSuccess = { success = it }, modName = foundMod)
        Assert.assertTrue("GenerateMap should return true", success)

        val landTiles = map.values.count { it.isLand }
        Assert.assertTrue("map should contain land tiles, got $landTiles", landTiles > 0)
        val startingLocations = map.startingLocations
        Assert.assertEquals("example should add one starting location", 1, startingLocations.size)
    }

    private fun Ruleset.mapParametersForTest() =
        com.unciv.logic.map.MapParameters().apply {
            shape = MapShape.rectangular
            mapSize = MapSize.Tiny
            name = ""
            type = com.unciv.logic.map.MapType.scripted
            mapScript = "LuaMapScriptExample"
        }
}
