package com.unciv.logic.civilization

import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapSize
import com.unciv.testing.GdxTestRunner
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(GdxTestRunner::class)
class ExploredRegionTest {
    /**
     * The explored region's bounds are mutated in place while exploring, so a clone must own copies.
     * Otherwise exploring on a cloned game (e.g. a simultaneous-turn replay clone) silently expands
     * the original game's explored region, which makes recorded state snapshots conflict.
     */
    @Test
    fun cloneDoesNotShareMutableBoundsWithOriginal() {
        val region = ExploredRegion()
        region.setMapParameters(
            MapParameters().apply {
                shape = MapShape.hexagonal
                mapSize = MapSize(4)
            }
        )
        region.checkTilePosition(HexCoord(0, 0), null)
        val widthBefore = region.getWidth()
        val heightBefore = region.getHeight()

        val clone = region.clone()
        clone.checkTilePosition(HexCoord(2, 0), null)

        Assert.assertEquals(
            "expanding a cloned explored region must not expand the original",
            widthBefore,
            region.getWidth()
        )
        Assert.assertEquals(heightBefore, region.getHeight())
    }
}
