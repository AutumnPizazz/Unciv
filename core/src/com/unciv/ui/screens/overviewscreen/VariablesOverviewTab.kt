package com.unciv.ui.screens.overviewscreen

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.Constants
import com.unciv.logic.city.City
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.Variable
import com.unciv.models.ruleset.VariableScope
import com.unciv.ui.components.extensions.addSeparator
import com.unciv.ui.components.extensions.toStringSigned
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.widgets.ExpanderTab
import com.unciv.ui.images.ImageGetter
import com.unciv.view.CivView

/** Read-only scope-aware view of mod-defined variables and their current per-turn changes. */
class VariablesOverviewTab(
    viewingPlayer: CivView,
    overviewScreen: EmpireOverviewScreen
) : EmpireOverviewTab(viewingPlayer, overviewScreen) {

    private val viewer = viewingPlayer.getCiv()
    private val ruleset = viewer.gameInfo.ruleset

    init {
        defaults().pad(10f).top()
        update()
    }

    private fun visibleCivs(): List<Civilization> {
        val civs = sequenceOf(viewer) + viewer.getKnownCivs()
        return civs.filter { !it.isDefeated() && !it.isSpectator() }.distinct().toList()
    }

    private fun visibleCities(): List<City> = visibleCivs().flatMap { civ ->
        civ.cities.filter { city -> city.civ == viewer || city.getCenterTile().isVisible(viewer) }
    }

    private fun isVisibleForCiv(variable: Variable, civ: Civilization): Boolean =
        variable.isDisplay && (variable.uniqueTo == null || civ.matchesFilter(variable.uniqueTo!!))

    private fun addVariableRow(table: Table, variable: Variable, label: String, value: Int, perTurn: Int) {
        table.add(ImageGetter.getVariableIcon(variable.name, 22f)).size(22f).padRight(5f)
        table.add(label.toLabel(hideIcons = true)).left().padRight(15f)
        val valueText = if (perTurn == 0) value.toString() else "$value (${perTurn.toStringSigned()})"
        table.add(valueText.toLabel()).left().row()
    }

    private fun addGlobalSection() {
        val variables = ruleset.variables.values.filter { it.isDisplay && it.resolvedScope == VariableScope.Global }
        if (variables.isEmpty()) return

        add("Global".toLabel(fontSize = Constants.headingFontSize)).left().row()
        addSeparator().row()
        val table = Table()
        table.defaults().pad(4f)
        for (variable in variables) {
            val perTurn = viewer.gameInfo.civilizations.flatMap { it.cities }
                .sumOf { it.cityStats.getSettledVariableYield(variable.name) }
            addVariableRow(table, variable, variable.name, viewer.gameInfo.getVariable(variable.name), perTurn)
        }
        add(table).left().row()
    }

    private fun addCivSection() {
        val civVariables = ruleset.variables.values.filter { it.resolvedScope == VariableScope.Civ }
        if (civVariables.isEmpty()) return

        add("Civilizations".toLabel(fontSize = Constants.headingFontSize)).left().row()
        addSeparator().row()
        for (civ in visibleCivs()) {
            val variables = civVariables.filter { isVisibleForCiv(it, civ) }
            if (variables.isEmpty()) continue
            val table = Table()
            table.defaults().pad(4f)
            for (variable in variables) {
                addVariableRow(table, variable, variable.name, civ.getVariable(variable.name),
                    civ.cities.sumOf { it.cityStats.getSettledVariableYield(variable.name) })
            }
            val icon = ImageGetter.getNationIcon(civ.nation.name).apply { setSize(22f, 22f) }
            add(ExpanderTab(civ.civName, icon = icon, startsOutOpened = civ == viewer,
                defaultPad = 4f, headerPad = 5f, initContent = { it.add(table).left() })).left().row()
        }
    }

    private fun addCitySection() {
        val cityVariables = ruleset.variables.values.filter { it.resolvedScope == VariableScope.City }
        if (cityVariables.isEmpty()) return

        add("Cities".toLabel(fontSize = Constants.headingFontSize)).left().row()
        addSeparator().row()
        for (city in visibleCities()) {
            val variables = cityVariables.filter { isVisibleForCiv(it, city.civ) }
            if (variables.isEmpty()) continue
            val table = Table()
            table.defaults().pad(4f)
            for (variable in variables)
                addVariableRow(table, variable, variable.name, city.getVariable(variable.name),
                    city.cityStats.getSettledVariableYield(variable.name))
            add(ExpanderTab("${city.civ.civName}: ${city.name}",
                icon = ImageGetter.getVariableIcon(variables.first().name, 22f),
                startsOutOpened = city.civ == viewer, defaultPad = 4f, headerPad = 5f,
                initContent = { it.add(table).left() })).left().row()
        }
    }

    private fun update() {
        clear()
        addGlobalSection()
        addCivSection()
        addCitySection()
        pack()
    }
}
