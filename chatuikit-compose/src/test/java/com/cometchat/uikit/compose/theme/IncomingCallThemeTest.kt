package com.cometchat.uikit.compose.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [incomingCallTheme] / [toIncomingCallColors] — the Compose UIKit's colour schemes as colours for
 * the Push SDK's incoming-call screen, by the mapping the View UIKit shares.
 *
 * Run: ./gradlew :chatuikit-compose:testDebugUnitTest --tests "*IncomingCallThemeTest"
 */
class IncomingCallThemeTest : FunSpec({

    test("each field comes from the scheme token the shared mapping names") {
        val scheme = lightColorScheme()
        val c = scheme.toIncomingCallColors()

        c.background shouldBe scheme.backgroundColor1.toArgb()
        c.callerName shouldBe scheme.textColorPrimary.toArgb()
        c.subtitle shouldBe scheme.textColorSecondary.toArgb()
        c.avatarBackground shouldBe scheme.primary.toArgb()
        c.avatarText shouldBe scheme.colorWhite.toArgb()
        c.acceptButton shouldBe scheme.successColor.toArgb()
        c.acceptIcon shouldBe scheme.colorWhite.toArgb()
        c.acceptLabel shouldBe scheme.textColorSecondary.toArgb()
        c.declineButton shouldBe scheme.errorColor.toArgb()
        c.declineIcon shouldBe scheme.colorWhite.toArgb()
        c.declineLabel shouldBe scheme.textColorSecondary.toArgb()
    }

    test("by default light and dark are the UIKit's own schemes, and they differ") {
        val theme = incomingCallTheme()

        theme.light shouldBe lightColorScheme().toIncomingCallColors()
        theme.dark shouldBe darkColorScheme().toIncomingCallColors()
        theme.light.background shouldNotBe theme.dark.background
    }

    test("an app's custom schemes are what the incoming-call screen gets") {
        val brand = Color(0xFF0057FF)
        val theme = incomingCallTheme(
            light = lightColorScheme(primary = brand),
            dark = darkColorScheme(primary = brand)
        )

        theme.light.avatarBackground shouldBe brand.toArgb()
        theme.dark.avatarBackground shouldBe brand.toArgb()
    }
})
