package com.unciv.logic.scripting

import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapSize
import com.unciv.logic.map.TileMap
import com.unciv.logic.map.mapgenerator.MapGenerationRandomness
import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.tile.TileNormalizer
import com.unciv.models.ruleset.Ruleset
import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaFunction
import com.badlogic.gdx.Gdx

@RunWith(GdxTestRunner::class)
class LuaMapGenAPITests {

    private lateinit var testGame: TestGame
    private lateinit var ruleset: Ruleset
    private lateinit var randomness: MapGenerationRandomness

    @Before
    fun setUp() {
        testGame = TestGame()
        testGame.makeHexagonalMap(5, "Grassland")
        ruleset = testGame.ruleset
        randomness = MapGenerationRandomness()
        randomness.seedRNG(12345L)
    }

    private fun buildContext(): LuaValue =
        LuaMapGenAPI.buildMapGenContext(testGame.tileMap, ruleset, randomness)

    private fun buildContextRectangular(): LuaValue {
        testGame.makeRectangularMap(10, 15, "Grassland")
        testGame.tileMap.mapParameters.shape = MapShape.rectangular
        return LuaMapGenAPI.buildMapGenContext(testGame.tileMap, ruleset, randomness)
    }

    // endregion

    // region Context structure
    @Test
    fun contextHasAllTopLevelKeys() {
        val ctx = buildContext()
        val table = ctx.checktable()

        val keys = listOf("params", "seed", "perlin", "random", "randomInt", "map", "log")
        for (key in keys) {
            Assert.assertFalse("Context should contain key '$key'", table.get(key).isnil())
        }
    }

    @Test
    fun seedMatchesMapParameters() {
        val ctx = buildContext()
        val seed = ctx.checktable().get("seed")
        Assert.assertEquals(
            testGame.tileMap.mapParameters.seed.toDouble(),
            seed.todouble(), 0.0
        )
    }
    // endregion

    // region Perlin noise
    @Test
    fun perlinIsCallableFromKotlin() {
        val ctx = buildContext()
        val perlin = ctx.checktable().get("perlin")
        Assert.assertTrue("perlin should be a LuaFunction",
            perlin is LuaFunction)
        // Should be callable with 3 args (the minimum required)
        val result = (perlin as LuaFunction).call(
            LuaValue.valueOf(0), LuaValue.valueOf(0), LuaValue.valueOf(42.0)
        )
        Assert.assertFalse("perlin(0,0,42) should return non-nil", result.isnil())
    }

    @Test
    fun perlinReturnsValueInValidRange() {
        val ctx = buildContext()
        val perlinFunc = ctx.checktable().get("perlin").checkfunction()

        val samples = mutableListOf<Double>()
        for (x in -2..2) {
            for (y in -2..2) {
                val tile = testGame.tileMap.getOrNull(x, y) ?: continue
                val result = perlinFunc.call(
                    LuaValue.valueOf(x), LuaValue.valueOf(y),
                    LuaValue.valueOf(42.0)
                ).todouble()
                samples.add(result)
            }
        }

        Assert.assertTrue("Perlin should return some non-zero values",
            samples.any { it != 0.0 })
        for (s in samples) {
            Assert.assertTrue("Perlin value $s should be in [-1, 1]",
                s >= -1.0 && s <= 1.0)
        }
    }

    @Test
    fun perlinUsesDefaultParameters() {
        val ctx = buildContext()
        val perlinFunc = ctx.checktable().get("perlin").checkfunction()

        val r1 = perlinFunc.call(
            LuaValue.valueOf(0), LuaValue.valueOf(0),
            LuaValue.valueOf(99.0)
        ).todouble()

        // Same defaults via table: defaults should match 3-arg call
        val opts = LuaValue.tableOf()
        opts.set("x", LuaValue.valueOf(0))
        opts.set("y", LuaValue.valueOf(0))
        opts.set("seed", LuaValue.valueOf(99.0))
        opts.set("scale", LuaValue.valueOf(30.0))
        opts.set("nOctaves", LuaValue.valueOf(6))
        opts.set("persistence", LuaValue.valueOf(0.5))
        opts.set("lacunarity", LuaValue.valueOf(2.0))
        val r2 = perlinFunc.call(opts).todouble()

        Assert.assertEquals("Perlin with defaults should match explicit defaults",
            r1, r2, 0.0000001)
    }

    @Test
    fun perlinSameInputSameOutput() {
        val ctx = buildContext()
        val perlinFunc = ctx.checktable().get("perlin").checkfunction()

        val r1 = perlinFunc.call(
            LuaValue.valueOf(1), LuaValue.valueOf(2), LuaValue.valueOf(42.0)
        ).todouble()
        val r2 = perlinFunc.call(
            LuaValue.valueOf(1), LuaValue.valueOf(2), LuaValue.valueOf(42.0)
        ).todouble()
        Assert.assertEquals("Perlin should be deterministic for same input", r1, r2, 0.0)
    }

    @Test
    fun perlinReturnsZeroForOutOfBoundsCoords() {
        val ctx = buildContext()
        val perlinFunc = ctx.checktable().get("perlin").checkfunction()

        val result = perlinFunc.call(
            LuaValue.valueOf(999), LuaValue.valueOf(999),
            LuaValue.valueOf(42.0)
        ).todouble()
        Assert.assertEquals("Perlin for OOB coords should return 0", 0.0, result, 0.0)
    }
    // endregion

    // region RNG
    @Test
    fun randomReturnsValueInRange() {
        val ctx = buildContext()
        val randomFunc = ctx.checktable().get("random").checkfunction()

        val samples = (1..100).map { randomFunc.call().todouble() }

        for (s in samples) {
            Assert.assertTrue("random() should be >= 0, got $s", s >= 0.0)
            Assert.assertTrue("random() should be < 1, got $s", s < 1.0)
        }
    }

    @Test
    fun randomIntReturnsValueInRange() {
        val ctx = buildContext()
        val randomIntFunc = ctx.checktable().get("randomInt").checkfunction()

        val samples = (1..200).map {
            randomIntFunc.call(LuaValue.valueOf(5), LuaValue.valueOf(15)).toint()
        }

        for (s in samples) {
            Assert.assertTrue("randomInt(5,15) should be >= 5, got $s", s >= 5)
            Assert.assertTrue("randomInt(5,15) should be <= 15, got $s", s <= 15)
        }
        // With 200 samples in range [5,15], we should see variation
        Assert.assertTrue("Should see multiple distinct values",
            samples.toSet().size >= 3)
    }

    @Test
    fun rngIsDeterministicWithSameSeed() {
        val ctx1 = buildContext()
        val seq1 = (1..10).map { ctx1.checktable().get("randomInt").checkfunction()
            .call(LuaValue.valueOf(1), LuaValue.valueOf(100)).toint() }

        randomness.seedRNG(12345L) // Re-seed
        val ctx2 = buildContext()
        val seq2 = (1..10).map { ctx2.checktable().get("randomInt").checkfunction()
            .call(LuaValue.valueOf(1), LuaValue.valueOf(100)).toint() }

        Assert.assertArrayEquals("Same seed should produce same sequence",
            seq1.toTypedArray(), seq2.toTypedArray())
    }
    // endregion

    // region Map dimensions
    @Test
    fun mapDimensionsOnHexagonalMap() {
        val ctx = buildContext()
        val map = ctx.checktable().get("map").checktable()

        Assert.assertEquals(5, map.get("getRadius").checkfunction().call().toint())
        Assert.assertEquals("Hexagonal", map.get("getShape").checkfunction().call().tojstring())
        Assert.assertFalse(map.get("isWrapped").checkfunction().call().toboolean())
    }

    @Test
    fun mapDimensionsOnRectangularMap() {
        val ctx = buildContextRectangular()
        val map = ctx.checktable().get("map").checktable()

        Assert.assertEquals(15, map.get("getWidth").checkfunction().call().toint())
        Assert.assertEquals(10, map.get("getHeight").checkfunction().call().toint())
        Assert.assertEquals("Rectangular", map.get("getShape").checkfunction().call().tojstring())
    }
    // endregion

    // region Tile access
    @Test
    fun getTileReturnsValidTileForValidCoords() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0))
        Assert.assertFalse("getTile(0,0) should return non-nil", tile.isnil())
        val tileTable = tile.checktable()
        Assert.assertEquals("Grassland", tileTable.get("baseTerrain").tojstring())
    }

    @Test
    fun getTileReturnsNilForOutOfBounds() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(999), LuaValue.valueOf(999))
        Assert.assertTrue("getTile for OOB coords should return nil", tile.isnil())
    }

    @Test
    fun getAllTilesReturnsCorrectCount() {
        val ctx = buildContext()
        val getAllTiles = ctx.checktable().get("map").checktable().get("getAllTiles").checkfunction()

        val tiles = getAllTiles.call().checktable()
        val expectedCount = testGame.tileMap.values.count()

        // Count entries in Lua table (1-indexed)
        var count = 0
        var i = 1
        while (!tiles.get(i).isnil()) { count++; i++ }

        Assert.assertEquals("getAllTiles should return correct tile count",
            expectedCount, count)
    }
    // endregion

    // region Tile properties
    @Test
    fun tileReadOnlyPropertiesAreCorrect() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        val kotlinTile = testGame.tileMap.getOrNull(0, 0)!!

        Assert.assertEquals(kotlinTile.baseTerrain, tile.get("baseTerrain").tojstring())
        Assert.assertEquals(kotlinTile.isLand, tile.get("isLand").toboolean())
        Assert.assertEquals(kotlinTile.isWater, tile.get("isWater").toboolean())
        Assert.assertEquals(kotlinTile.baseTerrain == "Coast", tile.get("isCoast").toboolean())

        val isHill = tile.get("isHill").checkfunction().call().toboolean()
        Assert.assertEquals(kotlinTile.isHill(), isHill)

        val isMountain = tile.get("isMountain").checkfunction().call().toboolean()
        Assert.assertEquals(kotlinTile.isImpassible(), isMountain)

        val isImpassable = tile.get("isImpassable").checkfunction().call().toboolean()
        Assert.assertEquals(kotlinTile.isImpassible(), isImpassable)
    }

    @Test
    fun tilePositionIsCorrect() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(2), LuaValue.valueOf(-1)).checktable()
        val pos = tile.get("position").checktable()

        Assert.assertEquals(2, pos.get("x").toint())
        Assert.assertEquals(-1, pos.get("y").toint())
        Assert.assertEquals(2, tile.get("getX").checkfunction().call().toint())
        Assert.assertEquals(-1, tile.get("getY").checkfunction().call().toint())
    }

    @Test
    fun tileHasResourceAndResourceNameCorrect() {
        val ctx = buildContext()
        val tile = testGame.getTile(HexCoord(0, 0))
        tile.tileResource = ruleset.tileResources["Iron"]
        tile.resourceAmount = 6

        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()
        val luaTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()

        Assert.assertTrue(luaTile.get("hasResource").checkfunction().call().toboolean())
        Assert.assertEquals("Iron", luaTile.get("resourceName").tojstring())
        Assert.assertEquals(6, luaTile.get("resourceAmount").toint())
    }

    @Test
    fun tileHasImprovementAndNameCorrect() {
        val ctx = buildContext()
        val tile = testGame.getTile(HexCoord(0, 0))
        val farm = ruleset.tileImprovements["Farm"]
        if (farm != null) tile.setImprovementBasic(farm)

        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()
        val luaTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()

        Assert.assertTrue(luaTile.get("hasImprovement").checkfunction().call().toboolean())
        Assert.assertEquals("Farm", luaTile.get("improvementName").tojstring())
    }

    @Test
    fun tileIsRiverWorks() {
        val ctx = buildContext()
        val tile = testGame.getTile(HexCoord(0, 0))
        val neighbor = testGame.getTile(HexCoord(1, 0))
        tile.setConnectedByRiver(neighbor, true)

        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()
        val luaTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()

        Assert.assertTrue(luaTile.get("isRiver").checkfunction().call().toboolean())
    }

    @Test
    fun tileIsNaturalWonderWorks() {
        val ctx = buildContext()
        val tile = testGame.getTile(HexCoord(0, 0))
        tile.naturalWonder = "Old Faithful"

        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()
        val luaTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()

        Assert.assertTrue(luaTile.get("isNaturalWonder").checkfunction().call().toboolean())
    }
    // endregion

    // region Tile terrain manipulation
    @Test
    fun setTerrainChangesBaseTerrain() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        Assert.assertEquals("Grassland", tile.get("baseTerrain").tojstring())

        tile.get("setTerrain").checkfunction().call(LuaValue.valueOf("Plains"))
        val updatedTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        Assert.assertEquals("Plains", updatedTile.get("baseTerrain").tojstring())

        // Verify Kotlin-side change
        val kotlinTile = testGame.getTile(HexCoord(0, 0))
        Assert.assertEquals("Plains", kotlinTile.baseTerrain)
    }

    @Test
    fun addAndRemoveTerrainFeature() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        tile.get("addTerrainFeature").checkfunction().call(LuaValue.valueOf("Forest"))

        val updatedTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        Assert.assertTrue(
            updatedTile.get("hasTerrainFeature").checkfunction()
                .call(LuaValue.valueOf("Forest")).toboolean()
        )

        // Remove it
        updatedTile.get("removeTerrainFeature").checkfunction().call(LuaValue.valueOf("Forest"))
        val finalTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        Assert.assertFalse(
            finalTile.get("hasTerrainFeature").checkfunction()
                .call(LuaValue.valueOf("Forest")).toboolean()
        )
    }

    @Test
    fun getTerrainFeaturesReturnsAllFeatures() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        tile.get("addTerrainFeature").checkfunction().call(LuaValue.valueOf("Forest"))
        tile.get("addTerrainFeature").checkfunction().call(LuaValue.valueOf("Jungle"))

        val features = tile.get("getTerrainFeatures").checkfunction().call().checktable()
        val featureSet = mutableSetOf<String>()
        var i = 1
        while (!features.get(i).isnil()) { featureSet.add(features.get(i).tojstring()); i++ }

        Assert.assertTrue(featureSet.contains("Forest"))
        Assert.assertTrue(featureSet.contains("Jungle"))
    }

    @Test
    fun removeAllTerrainFeaturesClearsAll() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        tile.get("addTerrainFeature").checkfunction().call(LuaValue.valueOf("Forest"))
        tile.get("addTerrainFeature").checkfunction().call(LuaValue.valueOf("Jungle"))
        tile.get("removeAllTerrainFeatures").checkfunction().call()

        val featuresCall = tile.get("getTerrainFeatures").checkfunction().call()
        Assert.assertTrue("Features should be empty after removeAll",
            featuresCall.checktable().get(1).isnil())
    }

    @Test
    fun setTerrainWithInvalidNameDoesNotCrash() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()
        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()

        // Should not throw, just no-op
        tile.get("setTerrain").checkfunction().call(LuaValue.valueOf("NonexistentTerrainXYZ"))
        Assert.assertEquals("Grassland", tile.get("baseTerrain").tojstring())
    }

    @Test
    fun setBaseTerrainClearsIncompatibleFeatures() {
        val ctx = buildContext()
        val tile = testGame.getTile(HexCoord(0, 0))
        // Jungle occurs on Plains but Grassland is the current terrain
        tile.setBaseTerrain(ruleset.terrains["Plains"]!!)
        tile.addTerrainFeature("Jungle")

        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()
        val luaTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        Assert.assertTrue(
            luaTile.get("hasTerrainFeature").checkfunction()
                .call(LuaValue.valueOf("Jungle")).toboolean()
        )

        // Change terrain to something Jungle doesn't occur on — setBaseTerrain
        // follows setTerrain → Tile.setBaseTerrain which calls normalizeToRuleset
        luaTile.get("setTerrain").checkfunction().call(LuaValue.valueOf("Snow"))
        val finalTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        // Jungle should be removed because it doesn't occur on Snow
        Assert.assertFalse(
            finalTile.get("hasTerrainFeature").checkfunction()
                .call(LuaValue.valueOf("Jungle")).toboolean()
        )
    }
    // endregion

    // region Map-level operations
    @Test
    fun assignContinentsAssignsContinentIds() {
        val ctx = buildContext()
        val tileMap = testGame.tileMap

        // Set up some water tiles to form continents
        tileMap.getOrNull(0, 0)!!.setBaseTerrain(ruleset.terrains["Ocean"]!!)
        tileMap.getOrNull(0, 1)!!.setBaseTerrain(ruleset.terrains["Ocean"]!!)

        tileMap.assignContinents(TileMap.AssignContinentsMode.Clear)
        for (tile in tileMap.values) Assert.assertEquals(-1, tile.getContinent())

        val map = ctx.checktable().get("map").checktable()
        map.get("assignContinents").checkfunction().call()

        // At least some tiles should have continent IDs assigned
        val continentIds = tileMap.values.map { it.getContinent() }.filter { it >= 0 }
        Assert.assertTrue("At least some tiles should have continent IDs",
            continentIds.isNotEmpty())
    }

    @Test
    fun addAndGetStartingLocations() {
        val ctx = buildContext()
        val map = ctx.checktable().get("map").checktable()

        map.get("addStartingLocation").checkfunction().call(
            LuaValue.valueOf(0), LuaValue.valueOf(0), LuaValue.valueOf("TestNation")
        )
        map.get("addStartingLocation").checkfunction().call(
            LuaValue.valueOf(2), LuaValue.valueOf(3), LuaValue.valueOf("AnotherNation")
        )

        val locations = map.get("getStartingLocations").checkfunction().call().checktable()
        val found = mutableListOf<Pair<String, String>>()
        var i = 1
        while (!locations.get(i).isnil()) {
            val entry = locations.get(i).checktable()
            found.add(entry.get("nation").tojstring() to
                "${entry.get("x").toint()},${entry.get("y").toint()}")
            i++
        }

        Assert.assertEquals(2, found.size)
        Assert.assertTrue(found.any { it.first == "TestNation" && it.second == "0,0" })
        Assert.assertTrue(found.any { it.first == "AnotherNation" && it.second == "2,3" })
    }

    @Test
    fun clearStartingLocationsRemovesAll() {
        val ctx = buildContext()
        val map = ctx.checktable().get("map").checktable()

        map.get("addStartingLocation").checkfunction().call(
            LuaValue.valueOf(0), LuaValue.valueOf(0), LuaValue.valueOf("CivA")
        )

        val before = map.get("getStartingLocations").checkfunction().call().checktable()
        Assert.assertFalse(before.get(1).isnil())

        map.get("clearStartingLocations").checkfunction().call()

        val after = map.get("getStartingLocations").checkfunction().call().checktable()
        Assert.assertTrue("Starting locations should be empty after clear",
            after.get(1).isnil())
    }

    @Test
    fun addStartingLocationReturnsFalseForInvalidCoords() {
        val ctx = buildContext()
        val map = ctx.checktable().get("map").checktable()

        val result = map.get("addStartingLocation").checkfunction().call(
            LuaValue.valueOf(999), LuaValue.valueOf(999), LuaValue.valueOf("Test")
        )
        Assert.assertFalse(result.toboolean())
    }
    // endregion

    // region Tile resource manipulation
    @Test
    fun setAndRemoveResource() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        tile.get("setResource").checkfunction().call(
            LuaValue.valueOf("Iron"), LuaValue.valueOf(4)
        )

        val updatedTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        Assert.assertTrue(updatedTile.get("hasResource").checkfunction().call().toboolean())
        Assert.assertEquals("Iron", updatedTile.get("resourceName").tojstring())
        Assert.assertEquals(4, updatedTile.get("resourceAmount").toint())

        updatedTile.get("removeResource").checkfunction().call()
        val finalTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        Assert.assertFalse(finalTile.get("hasResource").checkfunction().call().toboolean())
        Assert.assertEquals("", finalTile.get("resourceName").tojstring())
    }
    // endregion

    // region Tile improvement manipulation
    @Test
    fun setAndRemoveImprovement() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        tile.get("setImprovement").checkfunction().call(LuaValue.valueOf("Farm"))

        val updatedTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        Assert.assertTrue(updatedTile.get("hasImprovement").checkfunction().call().toboolean())
        Assert.assertEquals("Farm", updatedTile.get("improvementName").tojstring())

        updatedTile.get("removeImprovement").checkfunction().call()
        val finalTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        Assert.assertFalse(finalTile.get("hasImprovement").checkfunction().call().toboolean())
        Assert.assertEquals("", finalTile.get("improvementName").tojstring())
    }

    @Test
    fun setImprovementInvalidNameDoesNotCrash() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()
        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()

        tile.get("setImprovement").checkfunction().call(LuaValue.valueOf("NonexistentImprovementXYZ"))
        Assert.assertFalse(tile.get("hasImprovement").checkfunction().call().toboolean())
    }
    // endregion

    // region Tile roads
    @Test
    fun setAndRemoveRoad() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        tile.get("setRoad").checkfunction().call()

        val kotlinTile = testGame.getTile(HexCoord(0, 0))
        Assert.assertEquals(
            com.unciv.logic.map.tile.RoadStatus.Road,
            kotlinTile.roadStatus
        )

        tile.get("removeRoad").checkfunction().call()
        Assert.assertEquals(
            com.unciv.logic.map.tile.RoadStatus.None,
            kotlinTile.roadStatus
        )
    }

    @Test
    fun setRailroadWorks() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        tile.get("setRailroad").checkfunction().call()

        val kotlinTile = testGame.getTile(HexCoord(0, 0))
        Assert.assertEquals(
            com.unciv.logic.map.tile.RoadStatus.Railroad,
            kotlinTile.roadStatus
        )
    }
    // endregion

    // region Tile natural wonder
    @Test
    fun setAndClearNaturalWonder() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        tile.get("setNaturalWonder").checkfunction().call(LuaValue.valueOf("Old Faithful"))

        val kotlinTile = testGame.getTile(HexCoord(0, 0))
        Assert.assertEquals("Old Faithful", kotlinTile.naturalWonder)

        // Clear by passing empty string
        tile.get("setNaturalWonder").checkfunction().call(LuaValue.valueOf(""))
        Assert.assertNull(kotlinTile.naturalWonder)
    }

    @Test
    fun setNaturalWonderInvalidNameDoesNotCrash() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()
        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()

        tile.get("setNaturalWonder").checkfunction().call(LuaValue.valueOf("FakeWonderXYZ"))
        val kotlinTile = testGame.getTile(HexCoord(0, 0))
        Assert.assertNull(kotlinTile.naturalWonder)
    }
    // endregion

    // region Tile neighbors
    @Test
    fun getNeighborsReturnsValidTiles() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        val neighbors = tile.get("getNeighbors").checkfunction().call().checktable()

        val kotlinNeighbors = testGame.getTile(HexCoord(0, 0)).neighbors.toList()
        var count = 0
        var i = 1
        while (!neighbors.get(i).isnil()) { count++; i++ }

        Assert.assertEquals(kotlinNeighbors.size, count)

        // Verify first neighbor has correct coordinates
        val firstNeighbor = neighbors.get(1).checktable()
        val nPos = firstNeighbor.get("position").checktable()
        Assert.assertEquals(
            kotlinNeighbors.first().position.x,
            nPos.get("x").toint()
        )
        Assert.assertEquals(
            kotlinNeighbors.first().position.y,
            nPos.get("y").toint()
        )
    }

    @Test
    fun getTilesInDistanceReturnsExpectedCount() {
        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()

        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        val tilesInDist = tile.get("getTilesInDistance").checkfunction()
            .call(LuaValue.valueOf(1)).checktable()

        // Distance 1 from center = center + all neighbors
        val kotlinTiles = testGame.getTile(HexCoord(0, 0))
            .getTilesInDistance(1).toList()

        var count = 0
        var i = 1
        while (!tilesInDist.get(i).isnil()) { count++; i++ }
        Assert.assertEquals(kotlinTiles.size, count)
    }
    // endregion

    // region Map params
    @Test
    fun paramsContainsAllExpectedKeys() {
        val ctx = buildContext()
        val params = ctx.checktable().get("params").checktable()

        val keys = listOf(
            "name", "type", "shape", "worldWrap", "mirroring", "symmetryMode",
            "size", "waterThreshold", "temperatureintensity", "temperatureShift",
            "vegetationRichness", "rareFeaturesRichness", "resourceRichness",
            "elevationExponent", "tilesPerBiomeArea", "maxCoastExtension",
            "noRuins", "noNaturalWonders", "mapResources", "strategicBalance",
            "legendaryStart", "mods", "baseRuleset"
        )
        for (key in keys) {
            Assert.assertFalse("Params should contain '$key'", params.get(key).isnil())
        }
    }

    @Test
    fun paramsSizeTableIsCorrect() {
        val ctx = buildContext()
        val size = ctx.checktable().get("params").checktable().get("size").checktable()

        Assert.assertEquals(5, size.get("radius").toint())
        Assert.assertTrue(size.get("name").tojstring().isNotEmpty())
    }

    @Test
    fun paramsAdvancedValuesAreCorrect() {
        val ctx = buildContext()
        val params = ctx.checktable().get("params").checktable()
        val mapParams = testGame.tileMap.mapParameters

        Assert.assertEquals(mapParams.waterThreshold.toDouble(),
            params.get("waterThreshold").todouble(), 0.0001)
        Assert.assertEquals(mapParams.temperatureintensity.toDouble(),
            params.get("temperatureintensity").todouble(), 0.0001)
        Assert.assertEquals(mapParams.vegetationRichness.toDouble(),
            params.get("vegetationRichness").todouble(), 0.0001)
        Assert.assertEquals(mapParams.tilesPerBiomeArea,
            params.get("tilesPerBiomeArea").toint())
    }
    // endregion

    // region normalizeTiles
    @Test
    fun normalizeTilesDoesNotCrash() {
        val ctx = buildContext()
        // Create an invalid tile state: Forest on Ocean
        val tile = testGame.getTile(HexCoord(0, 0))
        tile.setBaseTerrain(ruleset.terrains["Ocean"]!!)
        tile.addTerrainFeature("Forest") // Forest shouldn't be on Ocean

        val map = ctx.checktable().get("map").checktable()
        map.get("normalizeTiles").checkfunction().call()

        // Forest should have been removed by normalization
        Assert.assertFalse(tile.terrainFeatures.contains("Forest"))
    }
    // endregion

    // region setTransients
    @Test
    fun setTransientsDoesNotCrash() {
        val ctx = buildContext()
        val map = ctx.checktable().get("map").checktable()

        // Modify terrain then set transients
        val getTile = map.get("getTile").checkfunction()
        val tile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        tile.get("setTerrain").checkfunction().call(LuaValue.valueOf("Plains"))

        map.get("setTransients").checkfunction().call()
        // Should not throw — no explicit assertions beyond no-exception
    }
    // endregion

    // region Context immutability
    @Test
    fun contextDoesNotLeakGameObjects() {
        val ctx = buildContext()
        val table = ctx.checktable()

        // Game context keys should NOT exist in map gen context
        Assert.assertTrue(table.get("civ").isnil())
        Assert.assertTrue(table.get("city").isnil())
        Assert.assertTrue(table.get("unit").isnil())
        Assert.assertTrue(table.get("store").isnil())
        Assert.assertTrue(table.get("count").isnil())
    }
    // endregion

    // region Full integration: Lua map script
    private fun runLuaScript(scriptBody: String, assertSuccess: Boolean = true): Boolean {
        val tempDir = java.nio.file.Files.createTempDirectory("mapGenTest")
        val scriptsDir = java.nio.file.Files.createDirectory(tempDir.resolve("scripts"))
        val scriptPath = scriptsDir.resolve("mapScript.lua")
        java.nio.file.Files.writeString(scriptPath,
            """
            function GenerateMap(ctx)
            $scriptBody
            end
            """.trimIndent()
        )
        val mod = Ruleset().apply { name = "mapGenTestMod" }
        LuaScriptManager.loadScripts(
            com.badlogic.gdx.Gdx.files.absolute(tempDir.toAbsolutePath().toString()),
            "mapGenTestMod", mod
        )

        java.nio.file.Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEach { java.nio.file.Files.delete(it) }

        val (_, func) = LuaScriptManager.getFunction("mapGenTestMod", "GenerateMap") ?: run {
            Assert.fail("GenerateMap function not found after loading script")
            return false
        }

        val ctx = buildContext()
        var success = false
        LuaScriptManager.callFunction(func, ctx, onSuccess = { success = it })
        if (assertSuccess) Assert.assertTrue("Lua script should return true", success)
        return success
    }

    @Test
    fun luaScriptCanLog() {
        runLuaScript("""
            ctx.log("hello from Lua map script")
            return true
        """.trimIndent())
    }

    @Test
    fun luaScriptCanAccessMapWidth() {
        runLuaScript("""
            local w = ctx.map.getWidth()
            return w > 0
        """.trimIndent())
    }

    @Test
    fun luaScriptCanCallPerlin3Args() {
        // Test with minimum 3 args (uses call(arg, arg, arg) dispatch)
        runLuaScript("""
            local n = ctx.perlin(0, 0, 42.0)
            ctx.log("perlin(0,0,42) returned: " .. n)
            return true
        """.trimIndent())
    }

    @Test
    fun luaScriptCanCallPerlinTable() {
        // Test with table-based parameter passing
        runLuaScript("""
            local n = ctx.perlin{x=0, y=0, seed=42.0, scale=4.0, nOctaves=1}
            ctx.log("perlin table call returned: " .. n)
            return true
        """.trimIndent())
    }

    @Test
    fun luaScriptCanSetTerrain() {
        runLuaScript("""
            local tile = ctx.map.getTile(0, 0)
            tile.setTerrain("Plains")
            return true
        """.trimIndent())
        Assert.assertEquals("Plains", testGame.getTile(HexCoord(0, 0)).baseTerrain)
    }

    @Test
    fun luaScriptGeneratesMapViaApi() {
        runLuaScript("""
            local w = ctx.map.getWidth()
            local h = ctx.map.getHeight()
            local changed = 0
            for x = 0, w - 1 do
                for y = 0, h - 1 do
                    local tile = ctx.map.getTile(x, y)
                    if tile ~= nil then
                        local noise = ctx.perlin(x, y, 42.0)
                        if noise > 0.0 then
                            tile.setTerrain("Plains")
                        else
                            tile.setTerrain("Ocean")
                        end
                        changed = changed + 1
                    end
                end
            end
            ctx.map.assignContinents()
            ctx.map.setTransients()
            ctx.map.normalizeTiles()
            return changed > 0
        """.trimIndent())
        val tileMap = testGame.tileMap
        val terrainTypes = tileMap.values.map { it.baseTerrain }.toSet()
        Assert.assertTrue("Map should have multiple terrain types after script",
            terrainTypes.size >= 2 || terrainTypes.contains("Plains"))
    }
    // endregion

    // region Map script discovery & safety
    @Test
    fun getMapScriptsReturnsEmptyWhenNoMapScripts() {
        LuaScriptManager.clear()
        val scripts = LuaScriptManager.getMapScripts(testGame.ruleset)
        Assert.assertTrue("Should return empty list when no map scripts defined",
            scripts.isEmpty())
    }

    @Test
    fun getMapScriptsDiscoversMapScript() {
        // Load a mod with a map script definition
        val tempDir = java.nio.file.Files.createTempDirectory("mapScriptTest")
        val scriptsDir = java.nio.file.Files.createDirectory(tempDir.resolve("scripts"))
        java.nio.file.Files.writeString(scriptsDir.resolve("myMap.lua"),
            """
            function GetMapScriptInfo()
                return {
                    name = "Test Map Script",
                    description = "A test map script for unit tests"
                }
            end

            function GenerateMap(ctx)
                ctx.log("Generating test map")
                return true
            end
            """.trimIndent()
        )
        val mod = com.unciv.models.ruleset.Ruleset().apply {
            name = "mapScriptTestMod"
            mods.add("mapScriptTestMod")
        }
        LuaScriptManager.loadScripts(
            com.badlogic.gdx.Gdx.files.absolute(tempDir.toAbsolutePath().toString()),
            "mapScriptTestMod", mod
        )

        java.nio.file.Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEach { java.nio.file.Files.delete(it) }

        val scripts = LuaScriptManager.getMapScripts(mod)
        Assert.assertEquals("Should discover 1 map script", 1, scripts.size)
        val s = scripts[0]
        Assert.assertEquals("mapScriptTestMod", s.modName)
        Assert.assertEquals("Test Map Script", s.name)
        Assert.assertEquals("A test map script for unit tests", s.description)
        Assert.assertEquals("mapScriptTestMod:GenerateMap", s.ref)
    }

    @Test
    fun getMapScriptsSkipsIncompleteDefinition() {
        // Missing GenerateMap → should not be discovered
        val tempDir = java.nio.file.Files.createTempDirectory("incompleteMapTest")
        val scriptsDir = java.nio.file.Files.createDirectory(tempDir.resolve("scripts"))
        java.nio.file.Files.writeString(scriptsDir.resolve("incomplete.lua"),
            """
            function GetMapScriptInfo()
                return { name = "Incomplete", description = "" }
            end
            -- No GenerateMap function!
            """.trimIndent()
        )
        val mod = com.unciv.models.ruleset.Ruleset().apply {
            name = "incompleteMapTestMod"
            mods.add("incompleteMapTestMod")
        }
        LuaScriptManager.loadScripts(
            com.badlogic.gdx.Gdx.files.absolute(tempDir.toAbsolutePath().toString()),
            "incompleteMapTestMod", mod
        )

        java.nio.file.Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEach { java.nio.file.Files.delete(it) }

        val scripts = LuaScriptManager.getMapScripts(mod)
        Assert.assertTrue("Incomplete map script (no GenerateMap) should be skipped",
            scripts.isEmpty())
    }

    @Test
    fun isMapGenFunctionIdentifiesReservedNames() {
        Assert.assertTrue("GenerateMap should be a map gen function",
            LuaScriptManager.isMapGenFunction("GenerateMap"))
        Assert.assertTrue("GetMapScriptInfo should be a map gen function",
            LuaScriptManager.isMapGenFunction("GetMapScriptInfo"))
        Assert.assertFalse("Regular function should not be map gen",
            LuaScriptManager.isMapGenFunction("onTurnStart"))
        Assert.assertFalse("Random name should not be map gen",
            LuaScriptManager.isMapGenFunction("myCustomFunc"))
    }

    @Test
    fun triggerLuaFunctionWithGenerateMapShowsError() {
        // Build a ruleset with TriggerLuaFunction referencing GenerateMap
        val mod = com.unciv.models.ruleset.Ruleset().apply {
            name = "badTriggerMod"
            mods.add("badTriggerMod")
        }
        val building = com.unciv.models.ruleset.Building().apply {
            name = "EvilBuilding"
            uniques.add("Trigger the function [badTriggerMod:GenerateMap] with []")
        }
        mod.buildings[building.name] = building

        val errorList = mod.getErrorList()
        val mapGenErrors = errorList.filter {
            it.text.contains("GenerateMap") && it.text.contains("map generation")
        }
        Assert.assertTrue(
            "Should detect TriggerLuaFunction referencing map-gen function, got: ${errorList.map { it.text }}",
            mapGenErrors.isNotEmpty()
        )
        // Must be a red Error
        val firstError = mapGenErrors.first()
        Assert.assertEquals(
            com.unciv.models.ruleset.validation.RulesetErrorSeverity.Error,
            firstError.errorSeverityToReport
        )
    }

    @Test
    fun triggerLuaFunctionWithGetMapScriptInfoShowsError() {
        val mod = com.unciv.models.ruleset.Ruleset().apply {
            name = "badTriggerMod2"
            mods.add("badTriggerMod2")
        }
        val building = com.unciv.models.ruleset.Building().apply {
            name = "EvilBuilding2"
            uniques.add("Trigger the function [badTriggerMod2:GetMapScriptInfo] with []")
        }
        mod.buildings[building.name] = building

        val errorList = mod.getErrorList()
        Assert.assertTrue(
            "Should detect TriggerLuaFunction referencing GetMapScriptInfo",
            errorList.any { it.text.contains("GetMapScriptInfo") && it.text.contains("map generation") }
        )
    }

    @Test
    fun mapScriptGenerateMapCanBeCalledDirectly() {
        // Verify GenerateMap CAN be called via LuaScriptManager (it's only blocked from TriggerLuaFunction)
        val tempDir = java.nio.file.Files.createTempDirectory("validMapScript")
        val scriptsDir = java.nio.file.Files.createDirectory(tempDir.resolve("scripts"))
        java.nio.file.Files.writeString(scriptsDir.resolve("validMap.lua"),
            """
            function GetMapScriptInfo()
                return { name = "Valid Map", description = "Test" }
            end

            function GenerateMap(ctx)
                ctx.log("GenerateMap executed")
                return true
            end
            """.trimIndent()
        )
        val mod = com.unciv.models.ruleset.Ruleset().apply {
            name = "validMapScriptMod"
            mods.add("validMapScriptMod")
        }
        LuaScriptManager.loadScripts(
            com.badlogic.gdx.Gdx.files.absolute(tempDir.toAbsolutePath().toString()),
            "validMapScriptMod", mod
        )

        java.nio.file.Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEach { java.nio.file.Files.delete(it) }

        val (_, func) = LuaScriptManager.getFunction("validMapScriptMod", "GenerateMap")
            ?: run { Assert.fail("GenerateMap should be found"); return }

        val ctx = buildContext()
        var success = false
        LuaScriptManager.callFunction(func, ctx, onSuccess = { success = it })
        Assert.assertTrue("GenerateMap should be directly callable", success)
    }

    @Test
    fun mapScriptRefFormat() {
        val info = MapScriptInfo("myMod", "My Custom Map", "A beautiful custom world")
        Assert.assertEquals("myMod:GenerateMap", info.ref)
    }
    // endregion

    // region Generation helpers
    @Test
    fun floodFillReturnsConnectedTiles() {
        val ctx = buildContext()
        val floodFill = ctx.checktable().get("map").checktable().get("floodFill").checkfunction()

        // All tiles on a Grassland map are connected
        val result = floodFill.call(LuaValue.valueOf(0), LuaValue.valueOf(0), LuaValue.valueOf("Grassland"))
        Assert.assertFalse("floodFill should return non-nil", result.isnil())
        val tiles = result.checktable()
        Assert.assertFalse("Should have at least 1 tile", tiles.get(1).isnil())
        // A hex-5 map with all Grassland should have all 61 tiles
        var count = 0; var i = 1
        while (!tiles.get(i).isnil()) { count++; i++ }
        Assert.assertTrue("Should find many connected grassland tiles", count > 10)
    }

    @Test
    fun floodFillWithWrongTerrainReturnsEmpty() {
        val ctx = buildContext()
        val floodFill = ctx.checktable().get("map").checktable().get("floodFill").checkfunction()

        // No ocean tiles on a Grassland map
        val result = floodFill.call(LuaValue.valueOf(0), LuaValue.valueOf(0), LuaValue.valueOf("Ocean"))
        val tiles = result.checktable()
        // Only the center tile doesn't match — floodFill won't add it since it's not Ocean
        Assert.assertTrue("floodFill with mismatched terrain should return empty",
            tiles.get(1).isnil())
    }

    @Test
    fun floodFillWithoutFilterReturnsAllConnected() {
        val ctx = buildContext()
        val floodFill = ctx.checktable().get("map").checktable().get("floodFill").checkfunction()

        // No filter = any terrain
        val result = floodFill.call(LuaValue.valueOf(0), LuaValue.valueOf(0))
        val tiles = result.checktable()
        var count = 0; var i = 1
        while (!tiles.get(i).isnil()) { count++; i++ }
        Assert.assertEquals("Without filter should return all connected tiles",
            testGame.tileMap.values.count(), count)
    }

    @Test
    fun generateClimateSetsTemperatureAndHumidity() {
        // Create a mixed-terrain map first (land + water)
        testGame.makeHexagonalMap(5, "Grassland")
        val tileMap = testGame.tileMap
        // Make some water
        tileMap.getOrNull(0, 0)!!.setBaseTerrain(ruleset.terrains["Coast"]!!)
        tileMap.getOrNull(0, 1)!!.setBaseTerrain(ruleset.terrains["Ocean"]!!)

        val ctx = buildContext()
        val map = ctx.checktable().get("map").checktable()
        map.get("generateClimate").checkfunction().call()

        // Check that at least some tiles have non-null temperature and humidity
        val samples = tileMap.values.filter { it.temperature != null && it.humidity != null }
        Assert.assertTrue("generateClimate should set temperature and humidity on most tiles",
            samples.size >= tileMap.values.count() * 0.9)
    }

    @Test
    fun spreadCoastsCreatesCoastTiles() {
        // Create grassland map, then set some tiles to Ocean manually
        testGame.makeHexagonalMap(5, "Grassland")
        val tileMap = testGame.tileMap
        // Set a ring of tiles around (2,0) to Ocean → these should become Coast
        val center = tileMap.getOrNull(2, 0)!!
        for (neighbor in center.neighbors.toList()) {
            neighbor.setBaseTerrain(ruleset.terrains["Ocean"]!!)
        }
        // Set climate
        for (tile in tileMap.values) {
            tile.temperature = 0.0
            tile.humidity = 0.5
        }

        val ctx = buildContext()
        val map = ctx.checktable().get("map").checktable()
        map.get("spreadCoasts").checkfunction().call(LuaValue.valueOf(1))

        val coastCount = tileMap.values.count { it.baseTerrain == "Coast" }
        Assert.assertTrue("spreadCoasts should create coast tiles around land (got $coastCount)",
            coastCount > 0)
    }

    @Test
    fun generateRiversDoesNotCrash() {
        testGame.makeHexagonalMap(5, "Grassland")
        // Set temperature and humidity so rivers have valid terrain
        for (tile in testGame.tileMap.values) {
            tile.temperature = 0.0
            tile.humidity = 0.5
        }

        val ctx = buildContext()
        val map = ctx.checktable().get("map").checktable()
        map.get("generateRivers").checkfunction().call()
        // Should not throw — success is measured by survival
    }

    @Test
    fun generateIcePlacesIceAtPoles() {
        testGame.makeHexagonalMap(10, "Ocean")
        val tileMap = testGame.tileMap
        tileMap.setTransients(ruleset)

        // Pre-set low temperature near edges (polar) so ice can spawn
        for (tile in tileMap.values) {
            val latRatio = abs(tile.latitude).toDouble() / tileMap.maxLatitude
            tile.temperature = if (latRatio > 0.7) -0.8 else 0.5
            tile.humidity = 0.5
        }
        // Make polar tiles Ocean (not Coast) so ice can spawn on them
        for (tile in tileMap.values) {
            if ((tile.temperature ?: 0.0) < -0.3) {
                tile.setBaseTerrain(ruleset.terrains["Ocean"]!!)
            }
        }

        val ctx = buildContext()
        val map = ctx.checktable().get("map").checktable()
        map.get("generateIce").checkfunction().call()

        val iceCount = tileMap.values.count {
            it.terrainFeatures.any { f -> ruleset.terrains[f]?.isIce == true }
        }
        Assert.assertTrue("generateIce should place ice at poles (got $iceCount)",
            iceCount > 0)
    }

    @Test
    fun luaScriptCanUseGenerationHelpers() {
        runLuaScript("""
            ctx.map.generateClimate()

            local w = ctx.map.getWidth()
            local h = ctx.map.getHeight()
            for x = 0, w - 1 do
                for y = 0, h - 1 do
                    local tile = ctx.map.getTile(x, y)
                    if tile ~= nil then
                        local temp = tile.getTemperature()
                        if temp < -0.3 then
                            tile.setTerrain("Snow")
                        elseif temp < 0.3 then
                            tile.setTerrain("Plains")
                        else
                            tile.setTerrain("Desert")
                        end
                    end
                end
            end

            ctx.map.spreadCoasts(2)
            ctx.map.generateMountains(0.7)
            ctx.map.generateRivers()
            ctx.map.assignContinents()
            ctx.map.setTransients()
            ctx.map.normalizeTiles()
            return true
        """.trimIndent())
    }
    // endregion

    // region BBG/TeamPVP-style helpers
    @Test
    fun isAdjacentToFreshWaterDetectsRiver() {
        val ctx = buildContext()
        val tile = testGame.getTile(HexCoord(0, 0))
        val neighbor = testGame.getTile(HexCoord(1, 0))
        tile.setConnectedByRiver(neighbor, true)

        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()
        val luaTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        val isFresh = luaTile.get("isAdjacentToFreshWater").checkfunction().call()
        Assert.assertTrue("Tile with river adjacency should have freshwater", isFresh.toboolean())
    }

    @Test
    fun isAdjacentToFreshWaterDetectsLake() {
        testGame.makeHexagonalMap(5, "Grassland")
        val tileMap = testGame.tileMap
        val center = tileMap.getOrNull(0, 0)!!
        // Place a lake tile next to center
        val neighbor = center.neighbors.first()
        neighbor.setBaseTerrain(ruleset.terrains.values.first { it.isFreshwater })

        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()
        val luaTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        val isFresh = luaTile.get("isAdjacentToFreshWater").checkfunction().call()
        Assert.assertTrue("Tile next to a lake should have freshwater", isFresh.toboolean())
    }

    @Test
    fun getBaseYieldReturnsFoodForGrassland() {
        testGame.makeHexagonalMap(5, "Grassland")
        val tileMap = testGame.tileMap
        tileMap.setTransients(ruleset)

        val ctx = buildContext()
        val getTile = ctx.checktable().get("map").checktable().get("getTile").checkfunction()
        val luaTile = getTile.call(LuaValue.valueOf(0), LuaValue.valueOf(0)).checktable()
        val food = luaTile.get("getBaseYield").checkfunction()
            .call(LuaValue.valueOf("Food")).todouble()
        Assert.assertTrue("Grassland should have some food yield (got $food)",
            food >= 1.0)
    }

    @Test
    fun normalizeStartPlotImprovesYields() {
        // Desert start — minimal food and production
        testGame.makeHexagonalMap(10, "Desert")
        val tileMap = testGame.tileMap
        tileMap.setTransients(ruleset)
        // Place a starting location
        tileMap.addStartingLocation("TestCiv", tileMap.getOrNull(0, 0)!!)

        val ctx = buildContext()
        val map = ctx.checktable().get("map").checktable()
        // Call normalizeStartPlot on the start position
        map.get("normalizeStartPlot").checkfunction().call(
            LuaValue.valueOf(0), LuaValue.valueOf(0)
        )

        // Verify the tile has resources or terrain modifications now
        val start = tileMap.getOrNull(0, 0)!!
        val innerRing = listOf(start) + start.neighbors.toList()
        val food = innerRing.sumOf {
            (it.stats.getTileStats(null)[com.unciv.models.stats.Stat.Food] ?: 0f).toInt()
        }
        val prod = innerRing.sumOf {
            (it.stats.getTileStats(null)[com.unciv.models.stats.Stat.Production] ?: 0f).toInt()
        }
        Assert.assertTrue("normalizeStartPlot should add food to desert start (got $food)",
            food >= 3)
        Assert.assertTrue("normalizeStartPlot should add production to desert start (got $prod)",
            prod >= 1)
    }

    @Test
    fun normalizeStartPlotEnsuresFreshwater() {
        testGame.makeHexagonalMap(5, "Grassland")
        val tileMap = testGame.tileMap
        tileMap.setTransients(ruleset)

        val ctx = buildContext()
        val map = ctx.checktable().get("map").checktable()
        map.get("normalizeStartPlot").checkfunction().call(
            LuaValue.valueOf(0), LuaValue.valueOf(0)
        )

        // After normalization, the start tile should have freshwater (river added)
        val start = tileMap.getOrNull(0, 0)!!
        val hasFreshwater = start.neighbors.any {
            start.isConnectedByRiver(it) || it.getBaseTerrain().isFreshwater
        }
        Assert.assertTrue("normalizeStartPlot should add freshwater", hasFreshwater)
    }

    @Test
    fun distributeLuxuriesPlacesResourcesNearStarts() {
        testGame.makeHexagonalMap(10, "Grassland")
        val tileMap = testGame.tileMap
        tileMap.setTransients(ruleset)
        tileMap.addStartingLocation("TestCiv", tileMap.getOrNull(0, 0)!!)
        tileMap.addStartingLocation("TestCiv2", tileMap.getOrNull(5, 5)!!)

        val ctx = buildContext()
        val map = ctx.checktable().get("map").checktable()
        val opts = LuaValue.tableOf()
        opts.set("perPlayer", LuaValue.valueOf(2))
        map.get("distributeLuxuries").checkfunction().call(opts)

        // Count luxury resources near starts
        val start1 = tileMap.getOrNull(0, 0)!!
        val luxNearStart1 = start1.getTilesInDistance(7).count {
            it.tileResource?.let { r -> r.resourceType == com.unciv.models.ruleset.tile.ResourceType.Luxury } == true
        }
        Assert.assertTrue("distributeLuxuries should place luxuries near start 1 (got $luxNearStart1)",
            luxNearStart1 >= 1)
    }

    @Test
    fun distributeStrategicsPlacesPerStart() {
        testGame.makeHexagonalMap(10, "Grassland")
        val tileMap = testGame.tileMap
        tileMap.setTransients(ruleset)
        tileMap.addStartingLocation("CivA", tileMap.getOrNull(0, 0)!!)
        tileMap.addStartingLocation("CivB", tileMap.getOrNull(5, 5)!!)

        val ctx = buildContext()
        val map = ctx.checktable().get("map").checktable()
        val opts = LuaValue.tableOf()
        opts.set("perPlayer", LuaValue.valueOf(2))
        opts.set("radius", LuaValue.valueOf(7))
        map.get("distributeStrategics").checkfunction().call(opts)

        val start1 = tileMap.getOrNull(0, 0)!!
        val stratNear1 = start1.getTilesInDistance(7).count {
            it.tileResource?.let { r -> r.resourceType == com.unciv.models.ruleset.tile.ResourceType.Strategic } == true
        }
        Assert.assertTrue("distributeStrategics should place strategics near start (got $stratNear1)",
            stratNear1 >= 1)
    }

    @Test
    fun strategicBalanceStartsGuaranteesResources() {
        testGame.makeHexagonalMap(10, "Grassland")
        val tileMap = testGame.tileMap
        tileMap.setTransients(ruleset)
        tileMap.addStartingLocation("CivA", tileMap.getOrNull(0, 0)!!)

        val ctx = buildContext()
        val map = ctx.checktable().get("map").checktable()
        val opts = LuaValue.tableOf()
        opts.set("horses", LuaValue.valueOf(2))
        opts.set("iron", LuaValue.valueOf(2))
        opts.set("radius", LuaValue.valueOf(5))
        map.get("strategicBalanceStarts").checkfunction().call(opts)

        val start = tileMap.getOrNull(0, 0)!!
        val horseCount = start.getTilesInDistance(5).count {
            it.tileResource?.name?.contains("Horse", ignoreCase = true) == true
        }
        val ironCount = start.getTilesInDistance(5).count {
            it.tileResource?.name?.contains("Iron", ignoreCase = true) == true
        }
        Assert.assertTrue("strategicBalanceStarts should place horses near start (got $horseCount)",
            horseCount >= 1)
        Assert.assertTrue("strategicBalanceStarts should place iron near start (got $ironCount)",
            ironCount >= 1)
    }

    @Test
    fun luaScriptFullBbGStyleGeneration() {
        runLuaScript("""
            -- Full BBG-style map generation pipeline
            local w = ctx.map.getWidth()
            local h = ctx.map.getHeight()

            -- 1. Landmass via Perlin
            for x = 0, w - 1 do
                for y = 0, h - 1 do
                    local tile = ctx.map.getTile(x, y)
                    if tile ~= nil then
                        local n = ctx.perlin(x, y, 42.0)
                        if n > 0.0 then tile.setTerrain("Grassland")
                        else tile.setTerrain("Ocean") end
                    end
                end
            end

            -- 2. Climate and terrain features
            ctx.map.generateClimate()
            ctx.map.spreadCoasts(2)
            ctx.map.generateMountains(0.7)
            ctx.map.generateRivers()
            ctx.map.generateIce()

            -- 3. Custom terrain by climate
            for x = 0, w - 1 do
                for y = 0, h - 1 do
                    local tile = ctx.map.getTile(x, y)
                    if tile and tile.isLand and not tile.isImpassable() then
                        local temp = tile.getTemperature()
                        if temp < -0.3 then tile.setTerrain("Snow")
                        elseif temp < 0.3 then tile.setTerrain("Plains")
                        else tile.setTerrain("Desert") end
                    end
                end
            end

            ctx.map.convertTerrains()
            ctx.map.assignContinents()

            -- 4. Place starting locations
            ctx.map.addStartingLocation(0, 0, "TestCiv")

            -- 5. BBG-style normalization
            ctx.map.normalizeStartPlot(0, 0, {
                freshwater = true,
                minFood = 4,
                minProd = 3,
                minLuxuries = 2,
                maxBlocking = 2,
                minHills = 2
            })

            -- 6. Resource distribution
            ctx.map.distributeLuxuries({perPlayer = 2, minDistance = 4})
            ctx.map.distributeStrategics({perPlayer = 2, radius = 7})

            -- 7. Strategic balance (second pass)
            ctx.map.strategicBalanceStarts({horses = 2, iron = 2, radius = 5})

            ctx.map.setTransients()
            ctx.map.normalizeTiles()
            return true
        """.trimIndent())
    }
    // endregion

    // region Catalog sync

    @Test
    fun mapGenApiCatalogMatchesRuntimeRegistration() {
        // Every API the engine registers on the map-script context tables must be listed in the
        // static catalog consumed by the CLI mod checker - otherwise mod-ci misses typos.
        // Locate the repo root by walking up from the bundled testMOD (android/assets/mods/testMOD)
        val testModFile = Gdx.files.internal("mods/testMOD").file().canonicalFile
        val repoRoot = testModFile.parentFile!!.parentFile!!.parentFile!!.parentFile!!
        val apiSource = java.nio.file.Files.readString(
            repoRoot.toPath().resolve("core/src/com/unciv/logic/scripting/LuaMapGenAPI.kt")
        )

        val registered = Regex("""\w+\.set\("([A-Za-z]+)"\s*,""")
            .findAll(apiSource)
            .map { it.groupValues[1] }
            .toSet()
        Assert.assertTrue("no APIs extracted from LuaMapGenAPI.kt", registered.size > 30)

        // All registered names must appear somewhere in the catalog (any table - the catalog
        // groups them by owner but a plain name check is enough to catch omissions).
        // `nation`/`x`/`y` are fields of the getStartingLocations() result table, not APIs.
        val resultTableFields = setOf("nation", "x", "y")
        val catalogNames = LuaMapGenAPI.mapGenApiCatalog.values.flatten().toSet()
        val missing = registered - catalogNames - resultTableFields
        Assert.assertTrue(
            "mapGenApiCatalog is missing: ${missing.sorted().joinToString(", ")}",
            missing.isEmpty()
        )

        // And every catalog name must actually be registered (catches renamed/removed APIs).
        val stale = catalogNames - registered - setOf("position")
        Assert.assertTrue(
            "mapGenApiCatalog lists unregistered names: ${stale.sorted().joinToString(", ")}",
            stale.isEmpty()
        )
    }
    // endregion
}
