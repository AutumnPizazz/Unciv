package com.unciv.ui.popups

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.ui.components.extensions.isEnabled
import com.unciv.ui.components.extensions.toCheckBox
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.extensions.toTextButton
import com.unciv.ui.components.input.onChange
import com.unciv.ui.components.input.onClick
import com.unciv.ui.screens.basescreen.BaseScreen

/**
 * 首次启动时要求玩家阅读并接受社区服务条款、并声明已满 18 周岁的弹窗。
 *
 * 本构建的社区服务（联机、下载、文档站）定位为仅面向成年人（见 `docs/Terms-of-Service.md`），
 * 所以这里是一个必须处理的闸门：不勾选则“Agree and continue”不可用，
 * 选择“Decline and exit”会结束游戏进程。
 *
 * 接受结果写入 [com.unciv.models.metadata.GameSettings.hasAcceptedCommunityTerms] 并立即保存，
 * 之后不再弹出。
 */
class CommunityTermsPopup(stageToShowOn: Stage) : Popup(stageToShowOn) {
    constructor(screen: BaseScreen) : this(screen.stage)

    init {
        addGoodSizedLabel("Community services terms", size = Constants.headingFontSize).colspan(2).row()

        addGoodSizedLabel(
            "The community services of this build - online multiplayer, in-game downloads and the " +
                "documentation site - are free, non-commercial and maintained by volunteers. " +
                "They are not an official service and are intended for adults only."
        ).colspan(2).row()

        addGoodSizedLabel(
            "If you are under 18, please use these services only with your guardian's consent, or do not " +
                "use them at all. By continuing, you confirm that you are at least 18 years old and that " +
                "you accept the Terms of Service and the Privacy Policy."
        ).colspan(2).row()

        val docsURL = Constants.wikiURLForLanguage()
        val linksRow = Table()
        val termsButton = "Terms of Service".toTextButton()
        termsButton.onClick { Gdx.net.openURI("${docsURL}Terms-of-Service") }
        linksRow.add(termsButton).pad(5f)
        val privacyButton = "Privacy Policy".toTextButton()
        privacyButton.onClick { Gdx.net.openURI("${docsURL}Privacy-Policy") }
        linksRow.add(privacyButton).pad(5f)
        add(linksRow).colspan(2).row()

        val acceptCheckBox = "I am at least 18 years old and accept the Terms of Service and the Privacy Policy".toCheckBox()
        add(acceptCheckBox).colspan(2).pad(10f).row()

        val agreeButton = addOKButton(
            "Agree and continue",
            style = BaseScreen.skin.get("positive", TextButtonStyle::class.java)
        ) {
            UncivGame.Current.settings.hasAcceptedCommunityTerms = true
            UncivGame.Current.settings.save()
        }.actor
        agreeButton.isEnabled = acceptCheckBox.isChecked
        acceptCheckBox.onChange { agreeButton.isEnabled = acceptCheckBox.isChecked }

        addButton("Decline and exit", style = BaseScreen.skin.get("negative", TextButtonStyle::class.java)) {
            Gdx.app.exit()
        }
        equalizeLastTwoButtonWidths()
    }
}
