package com.unciv.ui.components.tilegroups

import com.unciv.UncivGame
import com.unciv.dev.FontDesktop
import com.unciv.models.tilesets.TileSetCache
import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestGame
import com.unciv.ui.components.fonts.Fonts
import com.unciv.ui.images.ImageGetter
import com.unciv.view.CivView
import com.unciv.view.GameView
import com.unciv.view.TileView
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression test: map pin (tile note) previews - which render tiles as force-visible
 * TileGroups with the viewing civ passed in - must not reveal strategic resources whose
 * revealing tech has not been researched yet (e.g. no Oil in the Ancient era).
 *
 * This mirrors the exact build chain of `TileNotePopup.getTileGroupIcon`:
 * `TileView.forSingleTile` + `isForceVisible = true` + `update(viewer)`.
 * The regression was in TileLayerTerrain, which threw away the viewing civ
 * (`getViewableResource(if (isForceVisible) null else viewingCiv)`), so the
 * pixel-terrain layer (on by default via `showPixelImprovements`) leaked resources
 * even though the resource icon layer had been fixed.
 */
@RunWith(GdxTestRunner::class)
class MapPinPreviewResourceVisibilityTests {

    private lateinit var testGame: TestGame
    private lateinit var civ: com.unciv.logic.civilization.Civilization

    @Before
    fun setUp() {
        testGame = TestGame()
        testGame.makeHexagonalMap(3)
        civ = testGame.addCiv()
        Fonts.fontImplementation = FontDesktop()
        ImageGetter.setNewRuleset(testGame.ruleset)
        TileSetCache.loadTileSetConfigs()
    }

    @Test
    fun `force-visible preview with viewer does not render unrevealed strategic resource`() {
        val tile = testGame.getTile(1, 1)
        val oil = testGame.ruleset.tileResources["Oil"]!!
        tile.tileResource = oil
        tile.setTerrainTransients()
        // Sanity: the test civ starts in the Ancient era without the revealing tech
        assertFalse(civ.tech.isRevealed(oil))

        val tileGroup = buildForceVisiblePreview(tile, GameView(testGame.gameInfo, civ).civView)

        val terrainImages = tileGroup.layerTerrain.tileBaseImages.map { it.name }
        assertTrue(
            "Pixel-terrain layer leaked unrevealed resource Oil: $terrainImages",
            terrainImages.none { it.contains("Oil") }
        )
        assertFalse("Resource icon layer visible for unrevealed Oil", tileGroup.layerResource.isVisible)
    }

    @Test
    fun `force-visible preview without viewer shows all resources like map editor`() {
        val tile = testGame.getTile(1, 1)
        tile.tileResource = testGame.ruleset.tileResources["Oil"]!!
        tile.setTerrainTransients()

        val tileGroup = buildForceVisiblePreview(tile, viewer = null)

        val terrainImages = tileGroup.layerTerrain.tileBaseImages.map { it.name }
        assertTrue("Civ-agnostic preview should show Oil: $terrainImages", terrainImages.any { it.contains("Oil") })
        assertTrue("Resource icon layer should be visible civ-agnostically", tileGroup.layerResource.isVisible)
    }

    @Test
    fun `force-visible preview with viewer shows resource once revealing tech is researched`() {
        val tile = testGame.getTile(1, 1)
        val oil = testGame.ruleset.tileResources["Oil"]!!
        tile.tileResource = oil
        tile.setTerrainTransients()
        civ.tech.addTechnology(oil.revealedBy!!)
        assertTrue(civ.tech.isRevealed(oil))

        val tileGroup = buildForceVisiblePreview(tile, GameView(testGame.gameInfo, civ).civView)

        val terrainImages = tileGroup.layerTerrain.tileBaseImages.map { it.name }
        assertTrue("Pixel-terrain layer should show researched Oil: $terrainImages", terrainImages.any { it.contains("Oil") })
        assertTrue("Resource icon layer should show researched Oil", tileGroup.layerResource.isVisible)
    }

    /** Mirrors the build chain of `TileNotePopup.getTileGroupIcon` */
    private fun buildForceVisiblePreview(tile: com.unciv.logic.map.tile.Tile, viewer: CivView?): TileGroup {
        tile.setTerrainTransients()
        return TileGroup(
            TileView.forSingleTile(tile, viewer?.getCiv()),
            TileSetStrings(tile.ruleset, UncivGame.Current.settings)
        ).apply {
            isForceVisible = true
            isForMapEditorIcon = true
            update(viewer)
        }
    }
}
