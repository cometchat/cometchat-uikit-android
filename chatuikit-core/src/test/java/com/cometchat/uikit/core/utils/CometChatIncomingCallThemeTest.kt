package com.cometchat.uikit.core.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.view.ContextThemeWrapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * [CometChatIncomingCallTheme] — the UIKit's half of the Push SDK incoming call theme.
 *
 * A. fromThemeColors: the token mapping both kits share (agreed with Flutter, 15 Oct 2026)
 * B. fromTheme: both modes are read up front, each from a context forced to that mode
 *
 * Run: ./gradlew :chatuikit-core:testDebugUnitTest --tests "*CometChatIncomingCallThemeTest"
 */
class CometChatIncomingCallThemeTest : FunSpec({

    val background1 = 0xFFFFFFFF.toInt()
    val textPrimary = 0xFF141414.toInt()
    val textSecondary = 0xFF727272.toInt()
    val primary = 0xFF6852D6.toInt()
    val success = 0xFF09C26F.toInt()
    val error = 0xFFF44649.toInt()
    val white = 0xFFFFFFFE.toInt()

    fun colors(bg: Int = background1) = CometChatIncomingCallTheme.Colors.fromThemeColors(
        backgroundColor1 = bg,
        textColorPrimary = textPrimary,
        textColorSecondary = textSecondary,
        primaryColor = primary,
        successColor = success,
        errorColor = error,
        colorWhite = white
    )

    // -- A. fromThemeColors --------------------------------------------------------------------

    test("A1 the full-page background is backgroundColor1 and the texts follow the text colours") {
        val c = colors()
        c.background shouldBe background1
        c.callerName shouldBe textPrimary
        c.subtitle shouldBe textSecondary
    }

    test("A2 the avatar is the primary colour with white initials") {
        val c = colors()
        c.avatarBackground shouldBe primary
        c.avatarText shouldBe white
    }

    test("A3 Answer is success and Decline is error, both with white icons") {
        val c = colors()
        c.acceptButton shouldBe success
        c.acceptIcon shouldBe white
        c.declineButton shouldBe error
        c.declineIcon shouldBe white
    }

    test("A4 the labels sit under the buttons, so they take the secondary text colour, not white") {
        val c = colors()
        c.acceptLabel shouldBe textSecondary
        c.declineLabel shouldBe textSecondary
    }

    // -- B. fromTheme --------------------------------------------------------------------------

    fun device(nightNow: Boolean, forced: MutableList<Int>, themeRes: Int = 0): Context {
        val current = Configuration().apply {
            uiMode = if (nightNow) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        val resources = mock<Resources> { on { configuration } doReturn current }
        val context = mock<Context> {
            on { this.resources } doReturn resources
            on { applicationInfo } doReturn ApplicationInfo().apply { theme = themeRes }
        }
        whenever(context.createConfigurationContext(any())).thenAnswer { invocation ->
            val mode = (invocation.arguments[0] as Configuration).uiMode and Configuration.UI_MODE_NIGHT_MASK
            forced += mode
            mock<Context>()
        }
        return context
    }

    test("B1 light and dark are both read, whatever mode the device is in now") {
        listOf(false, true).forEach { nightNow ->
            val forced = mutableListOf<Int>()
            val read = mutableListOf<Context>()

            CometChatIncomingCallTheme.fromTheme(device(nightNow, forced), 0) { ctx ->
                read += ctx
                colors(bg = if (read.size == 1) 1 else 2)
            }

            forced shouldBe listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)
            read.size shouldBe 2
        }
    }

    test("B2 the light read becomes light and the dark read becomes dark") {
        var calls = 0
        val theme = CometChatIncomingCallTheme.fromTheme(device(nightNow = true, forced = mutableListOf()), 0) {
            calls++
            colors(bg = calls)
        }

        theme.light.background shouldBe 1
        theme.dark.background shouldBe 2
    }

    test("B3 with no theme to apply, the reader gets the forced context itself") {
        val forced = mutableListOf<Int>()
        val context = device(nightNow = false, forced = forced, themeRes = 0)
        val seen = mutableListOf<Context>()

        CometChatIncomingCallTheme.fromTheme(context) { ctx -> seen += ctx; colors() }

        seen.none { it is ContextThemeWrapper } shouldBe true
    }

    test("B4 the app's manifest theme is applied on top of the forced context") {
        val context = device(nightNow = false, forced = mutableListOf(), themeRes = 0x7f0f0001)
        val seen = mutableListOf<Context>()

        CometChatIncomingCallTheme.fromTheme(context) { ctx -> seen += ctx; colors() }

        seen.size shouldBe 2
        seen.forEach { it.shouldBeInstanceOf<ContextThemeWrapper>() }
    }

    test("B5 the theme is a value: the same colours compare equal") {
        val a = CometChatIncomingCallTheme(colors(), colors(bg = 2))
        val b = CometChatIncomingCallTheme(colors(), colors(bg = 2))
        a shouldBe b
    }
})
