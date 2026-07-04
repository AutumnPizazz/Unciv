package com.unciv.ui.screens.worldscreen.unit.presenter

import com.badlogic.gdx.utils.Align
import com.unciv.logic.files.UnitNotesManager
import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.translations.tr
import com.unciv.ui.components.extensions.surroundWithCircle
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.fonts.Fonts
import com.unciv.ui.components.input.onClick
import com.unciv.ui.components.widgets.UnitIconGroup
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.screens.pickerscreens.PromotionPickerScreen
import com.unciv.ui.screens.pickerscreens.UnitNotePopup
import com.unciv.ui.screens.pickerscreens.UnitRenamePopup
import com.unciv.ui.screens.worldscreen.WorldScreen
import com.unciv.ui.screens.worldscreen.unit.UnitTable
import yairm210.purity.annotations.Readonly

class UnitPresenter(private val unitTable: UnitTable, private val worldScreen: WorldScreen) : UnitTable.Presenter {

    val selectedUnit : MapUnit?
        get() = selectedUnits.firstOrNull()
    
    /** This is in preparation for multi-select and multi-move  */
    val selectedUnits = ArrayList<MapUnit>()

    // Whether the (first) selected unit is in unit-swapping mode
    var selectedUnitIsSwapping = false

    // Whether the (first) selected unit is in road-connecting mode
    var selectedUnitIsConnectingRoad = false

    /** Whether the currently selected unit is a foreign unit being viewed (not our own) */
    private var viewingForeignUnit = false

    override val position: HexCoord?
        get() = selectedUnit?.currentTile?.position

    fun selectUnit(unit: MapUnit? = null, append: Boolean = false) {
        if (!append) selectedUnits.clear()
        if (unit != null) {
            selectedUnits.add(unit)
            unit.actionsOnDeselect()
        }
        viewingForeignUnit = unit != null
                && unit.civ != worldScreen.viewingCiv
                && !worldScreen.viewingCiv.isSpectator()
        selectedUnitIsSwapping = false
        selectedUnitIsConnectingRoad = false
    }

    override fun update() {
        val unit = selectedUnit ?: return

        // The unit that was selected no longer exists on the map
        val disappeared = unit !in unit.getTile().getUnits()

        if (disappeared) {
            // If we were viewing a foreign unit, clean up its note
            if (viewingForeignUnit) {
                UnitNotesManager.deleteNote(worldScreen.gameInfo, unit)
            }
            unitTable.selectUnit()
            worldScreen.shouldUpdate = true
            return
        }

        // The unit changed ownership to another civ (captured from us)
        val captured = unit.civ != worldScreen.viewingCiv && !worldScreen.viewingCiv.isSpectator()
        if (captured && !viewingForeignUnit) {
            // Our unit was captured - deselect
            unitTable.selectUnit()
            worldScreen.shouldUpdate = true
            return
        }

        // If a foreign unit was captured BY us, transfer note to instanceName
        if (!captured && viewingForeignUnit) {
            val note = UnitNotesManager.getNote(worldScreen.gameInfo, unit)
            if (note != null) {
                unit.instanceName = note
                UnitNotesManager.deleteNote(worldScreen.gameInfo, unit)
            }
            viewingForeignUnit = false
        }

        val isForeign = unit.civ != worldScreen.viewingCiv && !worldScreen.viewingCiv.isSpectator()

        // set texts - this is valid even when it's the same unit, because movement points and health change
        // single selected unit
        if (selectedUnits.size == 1) with(unitTable) {
            separator.isVisible = true
            nameLabelText = buildNameLabelText(unit)
            unitNameLabel.clearListeners()

            if (isForeign) {
                // Foreign unit: clicking name label opens note editor instead of rename
                unitNameLabel.onClick {
                    UnitNotePopup(
                        screen = worldScreen,
                        unit = unit,
                        gameInfo = worldScreen.gameInfo,
                        actionOnClose = {
                            unitNameLabel.setText(buildNameLabelText(unit))
                            shouldUpdate = true
                        }
                    )
                }
            } else {
                // Own unit: clicking name label opens rename popup
                unitNameLabel.onClick {
                    if (!worldScreen.canChangeState) return@onClick
                    UnitRenamePopup(
                        screen = worldScreen,
                        unit = unit,
                        actionOnClose = {
                            unitNameLabel.setText(buildNameLabelText(unit))
                            shouldUpdate = true
                        }
                    )
                }
            }

            descriptionTable.clear()
            descriptionTable.defaults().pad(2f)
            descriptionTable.add(Fonts.movement + unit.getMovementString()).padRight(10f)

            if (!unit.isCivilian())
                descriptionTable.add(Fonts.strength + unit.baseUnit.strength.tr()).padRight(10f)

            if (unit.baseUnit.rangedStrength != 0)
                descriptionTable.add(Fonts.rangedStrength + unit.baseUnit.rangedStrength.tr())
                    .padRight(10f)

            if (unit.baseUnit.isRanged())
                descriptionTable.add(Fonts.range + unit.getRange().tr()).padRight(10f)

            val interceptionRange = unit.getInterceptionRange()
            if (interceptionRange > 0) {
                descriptionTable.add(ImageGetter.getStatIcon("InterceptRange")).size(20f)
                descriptionTable.add(interceptionRange.tr()).padRight(10f)
            }

            if (!unit.isCivilian()) {
                descriptionTable.add("XP".toLabel().apply {
                    onClick {
                        if (selectedUnit == null) return@onClick
                        worldScreen.game.pushScreen(PromotionPickerScreen(unit))
                    }
                })
                descriptionTable.add(
                    unit.promotions.XP.tr() + "/" + unit.promotions.xpForNextPromotion().tr()
                )
            }

            if (unit.baseUnit.religiousStrength > 0) {
                descriptionTable.add(ImageGetter.getStatIcon("ReligiousStrength")).size(20f)
                descriptionTable.add((unit.baseUnit.religiousStrength - unit.religiousStrengthLost).tr())
            }

            // Show note for foreign units
            if (isForeign) {
                descriptionTable.row()
                val note = UnitNotesManager.getNote(worldScreen.gameInfo, unit)
                if (note != null) {
                    descriptionTable.add("\uD83D\uDCDD".toLabel()).padRight(5f)
                    descriptionTable.add(note.toLabel()).padRight(10f)
                } else {
                    descriptionTable.add("Add Note".tr().toLabel().apply {
                        onClick {
                            UnitNotePopup(
                                screen = worldScreen,
                                unit = unit,
                                gameInfo = worldScreen.gameInfo,
                                actionOnClose = {
                                    unitNameLabel.setText(buildNameLabelText(unit))
                                    shouldUpdate = true
                                }
                            )
                        }
                    })
                }
            }

            if (unit.promotions.promotions.size != promotionsTable.children.size) // The unit has been promoted! Reload promotions!
                shouldUpdate = true
        } else with(unitTable) { // multiple selected units
            nameLabelText = ""
            descriptionTable.clear()
        }
    }

    override fun updateWhenNeeded() {
        val unit = selectedUnit ?: return
        // single selected unit
        if (selectedUnits.size == 1) with(unitTable) {

            unitIconHolder.add(UnitIconGroup(unit, 30f)).pad(5f)

            for (promotion in unit.promotions.getPromotions(true))
                if (!promotion.hasUnique(UniqueType.NotShownOnWorldScreen))
                    promotionsTable.add(ImageGetter.getPromotionPortrait(promotion.name, 20f))
                        .padBottom(2f)

            for (status in unit.statusMap.values) {
                if (status.uniques.any { it.type == UniqueType.NotShownOnWorldScreen }) continue

                val group = ImageGetter.getPromotionPortrait(status.name)
                val turnsLeft = "${status.turnsLeft}${Fonts.turn}".toLabel(fontSize = 8)
                    .surroundWithCircle(15f, color = ImageGetter.CHARCOAL)
                group.addActor(turnsLeft)
                turnsLeft.setPosition(group.width, 0f, Align.bottomRight)
                promotionsTable.add(group).padBottom(2f)
            }

            // Since Clear also clears the listeners, we need to re-add them every time
            promotionsTable.onClick {
                if (selectedUnit == null || promotionsTable.children.isEmpty) return@onClick
                worldScreen.game.pushScreen(PromotionPickerScreen(unit))
            }

            unitIconHolder.onClick {
                worldScreen.openCivilopedia(unit.baseUnit.makeLink())
            }
        } else { // multiple selected units
            for (selectedUnit in selectedUnits)
                unitTable.unitIconHolder.add(UnitIconGroup(selectedUnit, 30f)).pad(5f)
        }
    }

    @Readonly
    private fun buildNameLabelText(unit: MapUnit) : String {
        var nameLabelText = unit.displayName().tr(true)
        if (unit.health < 100) nameLabelText += " (${unit.health.tr()})"
        val isForeign = unit.civ != worldScreen.viewingCiv && !worldScreen.viewingCiv.isSpectator()
        if (isForeign) nameLabelText += " (${unit.civ.civName.tr()})"
        return nameLabelText
    }

}
