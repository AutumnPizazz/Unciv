package com.unciv.view

import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression test: [TileView.forSingleTile] must render previews for real game tiles
 * whose zeroBasedIndex is non-zero (used by the map pin editor popup).
 * See the ArrayIndexOutOfBoundsException in TileMapView.getTile with a 1-tile map.
 */
@RunWith(GdxTestRunner::class)
class TileViewForSingleTileTests {

    private lateinit var testGame: TestGame

    @Before
    fun setUp() {
        testGame = TestGame()
        testGame.makeHexagonalMap(3)
        testGame.addCiv()
    }

    @Test
    fun `forSingleTile works for a real game tile with non-zero index`() {
        val tile = testGame.tileMap[1, 1]  // a tile somewhere in the middle of the map
        assert(tile.zeroBasedIndex > 0)
        val view = TileView.forSingleTile(tile)
        assertNotNull(view)
    }

    @Test
    fun `forSingleTile works for a freshly created tile with index zero`() {
        val tile = testGame.tileMap[0, 0]
        assert(tile.zeroBasedIndex == 0)
        assertNotNull(TileView.forSingleTile(tile))
    }
}
