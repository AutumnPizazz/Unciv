package com.unciv.ui.popups.options

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.ui.screens.civilopediascreen.FormattedLine
import com.unciv.ui.screens.civilopediascreen.MarkupRenderer

internal class AboutTab(
    optionsPopup: OptionsPopup
): OptionsPopupTab(optionsPopup) {
    init {
        renderTo(this)
    }

    companion object {
        /** Get "About" content in a separate Table without the [OptionsPopupTab] contract */
        // MainMenuscreen adds this to a Popup - one Table wrapper could be saved by using renderTo(Popup.innerTable), but that would be longer code
        fun asTable() = Table().apply { renderTo(this) }

        private fun renderTo(table: Table) {
            table.pad(20f)
            // The changelog has no patches, and anchors per release tag omit the dots
            // CN changelog titles carry the build number: "## 4.21.7.2 (build 1252)" → anchor "42172-build-1252"
            val versionAnchor = Regex("""\.|-patch\d+$""").replace(UncivGame.VERSION.text, "") + "-build-" + UncivGame.VERSION.number
            val lines = sequence {
                yield(FormattedLine(extraImage = "banner", imageSize = 240f, centered = true))
                yield(FormattedLine())
                yield(FormattedLine("{Version}: ${UncivGame.VERSION.toNiceString()}", link = "${Constants.uncivRepoURL}blob/UncivCN/docs/UncivCN/Changelog.md#$versionAnchor"))
                yield(FormattedLine("See online Readme", link = "${Constants.uncivRepoURL}blob/UncivCN/README.md#unciv---foss-civ-v-for-androiddesktop"))
                yield(FormattedLine("Visit repository", link = Constants.uncivRepoURL))
                yield(FormattedLine("Visit the wiki", link = Constants.wikiURL))
            }
            MarkupRenderer.renderTo(table, lines.asIterable())
        }
    }
}
