package com.cometchat.uikit.kotlin.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * [CometChatTheme.getIncomingCallTheme] — the View UIKit's theme as colours for the Push SDK's
 * incoming-call screen. Both modes must come from the theme's own day and night values, whatever
 * mode the device is in when the app starts.
 *
 * Run: ./gradlew :chatuikit-kotlin:testDebugUnitTest --tests "*CometChatThemeIncomingCallThemeTest"
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatThemeIncomingCallThemeTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val styleRes = R.style.CometChatTheme_DayNight

    @After
    fun tearDown() {
        // The setters write a process-wide cache; never leak one into another test.
        clearSuccessOverride()
    }

    @Test
    fun lightAndDarkComeFromTheThemesDayAndNightValues() {
        val theme = CometChatTheme.getIncomingCallTheme(context, styleRes)

        assertEquals(0xFFFFFFFF.toInt(), theme.light.background)
        assertEquals(0xFF141414.toInt(), theme.dark.background)
        assertNotEquals(theme.light.callerName, theme.dark.callerName)
    }

    @Test
    @Config(qualifiers = "night")
    fun aDeviceInDarkModeStillGetsTheLightColoursForLight() {
        val theme = CometChatTheme.getIncomingCallTheme(context, styleRes)

        assertEquals(0xFFFFFFFF.toInt(), theme.light.background)
        assertEquals(0xFF141414.toInt(), theme.dark.background)
    }

    @Test
    fun theSharedMappingIsApplied() {
        val theme = CometChatTheme.getIncomingCallTheme(context, styleRes)

        with(theme.light) {
            // Labels sit under the buttons: secondary text, not white.
            assertEquals(subtitle, acceptLabel)
            assertEquals(subtitle, declineLabel)
            assertEquals(avatarText, acceptIcon)
            assertEquals(avatarText, declineIcon)
            assertNotEquals(acceptButton, declineButton)
        }
    }

    @Test
    fun aColourSetInCodeAppliesToBothModes() {
        CometChatTheme.setSuccessColor(0xFF123456.toInt())

        val theme = CometChatTheme.getIncomingCallTheme(context, styleRes)

        assertEquals(0xFF123456.toInt(), theme.light.acceptButton)
        assertEquals(0xFF123456.toInt(), theme.dark.acceptButton)
    }

    private fun clearSuccessOverride() {
        val cache = CometChatTheme::class.java.getDeclaredField("themeAttributeCache")
            .apply { isAccessible = true }
            .get(CometChatTheme) as MutableMap<*, *>
        cache.remove(R.attr.cometchatSuccessColor)
    }
}
