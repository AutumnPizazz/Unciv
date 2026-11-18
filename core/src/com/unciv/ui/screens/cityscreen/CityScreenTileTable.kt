package com.unciv.ui.screens.cityscreen

import com.unciv.ui.screens.basescreen.BaseScreen
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.unciv.logic.map.tile.Tile
import com.unciv.ui.objectdescriptions.TileDescription
import com.unciv.models.stats.Stat
import com.unciv.models.stats.Stats
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.ui.audio.SoundPlayer
import yairm210.purity.annotations.Readonly
import com.unciv.ui.components.extensions.darken
import com.unciv.ui.components.extensions.disable
import com.unciv.ui.components.extensions.isEnabled
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.extensions.toTextButton
import com.unciv.ui.components.input.KeyboardBinding
import com.unciv.ui.components.input.onActivation
import com.unciv.ui.components.input.onClick
import com.unciv.ui.components.widgets.BasicTileInfo
import com.unciv.ui.popups.AnimatedMenuPopup
import com.unciv.ui.popups.AnimatedMenuPopup.Companion.addContextMenu
import com.unciv.view.CityView
import com.unciv.view.ForeignCityView
import com.unciv.view.TileView
import kotlin.math.roundToInt

class CityScreenTileTable(
    private val cityScreen: CityScreen,
    override val cityView: CityView = cityScreen.cityView
) : BasicTileInfo(
    cityScreen, cityView.viewingCiv(),
    cityView,
    cityScreen.isSpying,
    "CityScreen/CityScreenTileTable/InnerTable", 2f, "CityScreen/CityScreenTileTable/Background"
) {
    // For pick-up by UiElementDocsWriter
    // skinStrings.getUiBackground("CityScreen/CityScreenTileTable/InnerTable")
    // skinStrings.getUiBackground("CityScreen/CityScreenTileTable/Background")

    override fun Table.addAdditionalContent(tileView: TileView) {
        val autoLock = "Auto-Lock".toTextButton()
        autoLock.color = if (cityView.getCity().autoLockTiles) BaseScreen.skin.getColor("selection") else Color.WHITE
        autoLock.isEnabled = cityScreen.canChangeState
        autoLock.onActivation {
            cityView.toggleAutoLockTiles()
            cityScreen.updateAsync()
        }
        add(autoLock).row()

        val selectedTile = cityView.getTile(tileView)
        if (canClaimTile(selectedTile)) {
            val claim = "Claim tile".toTextButton()
            claim.isEnabled = cityScreen.canChangeState
            claim.onActivation(binding = KeyboardBinding.ClaimTile) {
                claim.disable()
                cityScreen.claimTile(selectedTile)
            }
            add(claim).row()
        }

        if (cityView.canBuyTile(tileView))
            addBuyTileButton(tileView)

        addClickableCityReference(tileView.owningCity(), "Owned")
        addClickableCityReference(tileView.getWorkingCity(), "Worked")

        if (cityView.isWorked(tileView)) {
            if (tileView.isLocked())
                addLockOrUnlockButton(tileView, "Unlock", CityView::tryUnlockTile)
            else
                addLockOrUnlockButton(tileView, "Lock", CityView::tryLockTile)
        }

        if (tileView.isCityCenter()) {
            val otherCityView = tileView.owningCity()?.tryGetCityView()
            if (otherCityView != null && otherCityView != cityView)
                add("Move to city".toTextButton().onClick { jumpToCity(otherCityView) })
        }
    }

    private fun Table.addBuyTileButton(tileView: TileView) {
        val goldCostOfTile = cityView.getGoldCostOfTile(tileView)
        val buyTileButton = "Buy for [$goldCostOfTile] gold".toTextButton()
        buyTileButton.onActivation(binding = KeyboardBinding.BuyTile) {
            buyTileButton.disable()
            cityScreen.askToBuyTile(tileView)
        }
        buyTileButton.addContextMenu { TileBuyMenu(buyTileButton) }
        buyTileButton.isEnabled = cityScreen.canChangeState && cityView.viewingCiv().hasStatToBuy(Stat.Gold, goldCostOfTile)
        add(buyTileButton).padTop(5f).row()
    }

    private fun Table.addLockOrUnlockButton(tileView: TileView, label: String, action: CityView.(TileView) -> Boolean) {
        val lockButton = label.toTextButton()
        lockButton.onClick {
            cityView.action(tileView)
            update(tileView)
            cityScreen.updateAsync()
        }
        if (!cityScreen.canChangeState) lockButton.disable()
        add(lockButton).padTop(5f).row()
    }

    private fun Table.addClickableCityReference(otherCity: ForeignCityView?, ref: String) {
        if (otherCity == null) return
        val label = "$ref by [${otherCity.name}]".toLabel()
        add(label).row()
        val otherCityView = otherCity.tryGetCityView() ?: return
        if (otherCityView == cityView) return
        label.onClick { jumpToCity(otherCityView) }
    }

    private fun jumpToCity(otherCityView: CityView) {
        cityScreen.game.replaceCurrentScreen { CityScreen(otherCityView) }
    }

    /** Checks if a tile can be claimed by this city.
     * A tile is claimable if:
     * - It is owned by another city of the same civilization
     * - It is adjacent to this city
     * - It is not a city center
     * - It is not in the first ring of either city (distance 1 from city center)
     * - It is within the claim range for both cities (both city work ranges)
     * - It does not have an improvement created by CreatesOneImprovement
     * - The ruleset allows tile claiming (ModOptions.AllowTileClaim)
     */
    @Readonly
    private fun canClaimTile(tile: Tile): Boolean {
        val owningCity = tile.getCity()
        val city = cityView.getCity()
        return when {
            owningCity == null -> false
            owningCity.civ != city.civ -> false
            owningCity == city -> false
            tile.isCityCenter() -> false
            city.expansion.isFirstRingTile(tile) -> false
            owningCity.expansion.isFirstRingTile(tile) -> false
            !city.expansion.isWithinClaimRange(tile) -> false
            !owningCity.expansion.isWithinClaimRange(tile) -> false
            tile.improvementCreatedByCreatesOneImprovement != null -> false
            !city.civ.gameInfo.ruleset.modOptions.hasUnique(UniqueType.AllowTileClaim) -> false
            else -> tile.neighbors.any { it.getCity() == city }
        }
    }

    private inner class TileBuyMenu(buyTileButton: TextButton) : AnimatedMenuPopup(stage, buyTileButton) {
        override fun createContentTable(): Table? {
            val maxRing = cityView.getWorkRange()
            val counts = IntArray(maxRing + 1) { countBuyableInRing(it) }
            if (counts.sum() < 2) return null
            return super.createContentTable()!!.apply {
                add("Currently you have [${cityView.viewingCiv().gold}] [Gold].".toLabel(alignment = Align.center)).growX().row()
                for (ring in 0..maxRing) {
                    val count = counts[ring]
                    if (count == 0 || ring > 0 && count == counts[ring - 1]) continue
                    val cost = getRingCost(ring)
                    val text = "Buy [$count] tiles in ring [$ring] for [$cost][${Stat.Gold.character}]"
                    val button = getButton(text, KeyboardBinding.None) { buyRing(ring) }
                    button.isDisabled = !cityScreen.canChangeState || cost > cityView.viewingCiv().gold
                    add(button).row()
                }
            }
        }

        @Readonly private fun getRing(ring: Int) =
            cityView.getCenterTile().getExploredTilesInDistance(ring)
                .filter { cityView.canBuyTile(it) } // This respects the requirement that **this** city must already own a neighbor
        @Readonly private fun countBuyableInRing(ring: Int) =
            getRing(ring).count()
        @Readonly private fun getRingCost(ring: Int) =
            getRing(ring).withIndex()
                .sumOf { cityView.getGoldCostOfTile(it.value, it.index) }

        private fun buyRing(ring: Int) {
            for (tileView in getRing(ring)) {
                if (!cityView.tryBuyTile(tileView))
                    break
            }
            SoundPlayer.play(Stat.Gold.purchaseSound)
            cityScreen.game.replaceCurrentScreen { CityScreen(cityView) } // update doesn't redo the tiles
        }
    }
}
