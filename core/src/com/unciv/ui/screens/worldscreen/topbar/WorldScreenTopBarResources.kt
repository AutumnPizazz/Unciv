package com.unciv.ui.screens.worldscreen.topbar

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.Variable
import com.unciv.models.ruleset.VariableScope
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.tile.TileResource
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.translations.tr
import com.unciv.ui.components.MayaCalendar
import com.unciv.ui.components.YearTextUtil
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.extensions.toStringSigned
import com.unciv.ui.components.fonts.Fonts
import com.unciv.ui.components.input.onClick
import com.unciv.ui.components.widgets.ScalingTableWrapper
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.screens.overviewscreen.EmpireOverviewCategories
import com.unciv.ui.screens.victoryscreen.VictoryScreen

internal class WorldScreenTopBarResources(topbar: WorldScreenTopBar) : ScalingTableWrapper() {
    private val turnsLabel = "Turns: 0/400".toLabel()
    private val pollingLabel = "".toLabel()
    private data class ResourceActors(val resource: TileResource, val label: Label, val icon: Group)
    private val resourceActors = ArrayList<ResourceActors>(12)
    private data class VariableActors(val variable: Variable, val label: Label, val icon: Group)
    private val variableActors = ArrayList<VariableActors>(4)
    private val resourcesWrapper = Table()
    val worldScreen = topbar.worldScreen

    // Note: For a proper functioning of the "shift floating buttons down when cramped" feature, it is
    // important that this entire Widget has only the bare minimum padding to its left and right.
    // #7193 did let the resources and turn label swap places, with the side effect of the "first resource getting extra left padding"
    // now being on the outside - the buttons moved out of the way despite there being visually ample space.

    private companion object {
        const val defaultPad = 5f
        const val extraPadBetweenLabelAndResources = 20f
        const val bottomPad = 10f
        const val extraPadBetweenResources = 5f
        const val outerHorizontalPad = 2f
        const val iconSize = 20f
        const val resourceAmountDescentTweak = 3f
    }

    init {
        defaults().space(extraPadBetweenLabelAndResources)
            .pad(defaultPad, outerHorizontalPad, bottomPad, outerHorizontalPad)

        resourcesWrapper.defaults().space(defaultPad)
        resourcesWrapper.touchable = Touchable.enabled

        turnsLabel.onClick {
            if (worldScreen.selectedCiv.isLongCountDisplay()) {
                val gameInfo = worldScreen.selectedCiv.gameInfo
                MayaCalendar.openPopup(worldScreen, worldScreen.selectedCiv, gameInfo.getYear())
            } else {
                worldScreen.game.pushScreen(VictoryScreen(worldScreen))
            }
        }
        resourcesWrapper.onClick {
            worldScreen.openEmpireOverview(EmpireOverviewCategories.Resources)
        }

        val strategicResources = worldScreen.gameInfo.ruleset.tileResources.values
            .filter { it.resourceType == ResourceType.Strategic && !it.isCityWide }
        for (resource in strategicResources) {
            val resourceImage = ImageGetter.getResourcePortrait(resource.name, iconSize)
            val resourceLabel = "0".toLabel()
            resourceActors += ResourceActors(resource, resourceLabel, resourceImage)
        }

        val displayVariables = worldScreen.gameInfo.ruleset.variables.values.filter {
            it.isDisplay && it.resolvedScope != VariableScope.City &&
                (it.uniqueTo == null || worldScreen.selectedCiv.matchesFilter(it.uniqueTo!!))
        }
        val modOptions = worldScreen.gameInfo.ruleset.modOptions
        val shownVariables = if (modOptions.variableMenuThreshold > 0 && displayVariables.size > modOptions.variableMenuThreshold) {
            // Menu collapsed: keep only always-display variables (capped by alwaysDisplayVariableCount)
            val alwaysDisplay = displayVariables.filter { it.isAlwaysDisplay }
            if (modOptions.alwaysDisplayVariableCount > 0)
                alwaysDisplay.take(modOptions.alwaysDisplayVariableCount)
            else alwaysDisplay
        } else displayVariables
        for (variable in shownVariables) {
            val variableIcon = ImageGetter.getVariableIcon(variable.name, iconSize)
            val variableLabel = "0".toLabel()
            variableActors += VariableActors(variable, variableLabel, variableIcon)
        }

        add(turnsLabel)
        add(pollingLabel)

        // in case the icons are configured higher than a label, we add a dummy - height will be measured once before it's updated
        if (resourceActors.isNotEmpty() || variableActors.isNotEmpty()) {
            resourcesWrapper.add(resourceActors.firstOrNull()?.icon ?: variableActors.first().icon)
            add(resourcesWrapper)
        }
    }

    fun update(civInfo: Civilization) {
        resetScale()

        val yearText = YearTextUtil.toYearText(
            civInfo.gameInfo.getYear(), civInfo.isLongCountDisplay()
        )
        turnsLabel.setText(Fonts.turn + "\u2004" + civInfo.gameInfo.turns.tr() + "\u2004|\u2004" + yearText) // U+2004: Three-Per-Em Space

        // Show polling countdown with color when in polling mode and it's the player's turn
        if (civInfo.gameInfo.isPollingMode() && worldScreen.isPlayersTurn) {
            val remaining = worldScreen.pollingSecondsRemaining
            val total = civInfo.gameInfo.gameParameters.pollingIntervalSeconds
            val ratio = remaining.toFloat() / total
            pollingLabel.color = when {
                ratio > 0.5f -> Color.GREEN
                ratio > 0.25f -> Color.GOLD
                else -> Color.CORAL
            }
            pollingLabel.setText("  |  ${remaining}s")
            pollingLabel.isVisible = true
        } else {
            pollingLabel.isVisible = false
        }

        resourcesWrapper.clearChildren()
        val civResources = civInfo.getCivResourcesByName()
        val civResourceSupply = civInfo.getCivResourceSupply()
        for ((index, resourceActors) in resourceActors.withIndex()) {
            val (resource, label, icon) = resourceActors

            if (resource.hasUnique(UniqueType.NotShownOnWorldScreen, civInfo.state)) continue

            val amount = civResources[resource.name] ?: 0

            if (!civInfo.canSeeResource(resource) && amount == 0) // You cannot trade for resources you cannot process yourself yet
                continue

            resourcesWrapper.add(icon).padLeft(if (index == 0) 0f else extraPadBetweenResources)

            if (!resource.isStockpiled)
                label.setText(amount.tr())
            else {
                val perTurn = civResourceSupply.firstOrNull { it.resource == resource }?.amount ?: 0
                if (perTurn == 0) label.setText(amount.tr())
                else label.setText("${amount.tr()} (${perTurn.toStringSigned()})")
            }
            resourcesWrapper.add(label).padTop(resourceAmountDescentTweak)  // digits don't have descenders, so push them down a little
        }

        for ((index, variableActors) in variableActors.withIndex()) {
            val (variable, label, icon) = variableActors
            val amount = when (variable.resolvedScope) {
                VariableScope.Civ -> civInfo.getVariable(variable.name)
                VariableScope.Global -> civInfo.gameInfo.getVariable(variable.name)
                VariableScope.City -> continue
                VariableScope.Unit -> continue
            }
            val perTurn = getVariablePerTurn(civInfo, variable)
            resourcesWrapper.add(icon).padLeft(if (index == 0 && resourceActors.isEmpty()) 0f else extraPadBetweenResources)
            label.setText(if (perTurn == 0) amount.tr() else "${amount.tr()} (${perTurn.toStringSigned()})")
            resourcesWrapper.add(label).padTop(resourceAmountDescentTweak)
        }

        scaleTo(worldScreen.stage.width)
    }

    private fun getVariablePerTurn(civInfo: Civilization, variable: Variable): Int {
        val cities = if (variable.resolvedScope == VariableScope.Global)
            civInfo.gameInfo.civilizations.asSequence().flatMap { it.cities.asSequence() }
        else civInfo.cities.asSequence()
        return cities.sumOf { it.cityStats.getSettledVariableYield(variable.name) }
    }
}
