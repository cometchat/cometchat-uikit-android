package com.cometchat.uikit.compose.presentation.shared.mediarecorder

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.mediarecorder.style.CometChatAudioVisualizerStyle
import com.cometchat.uikit.compose.presentation.shared.mediarecorder.ui.CometChatAudioVisualizer
import com.cometchat.uikit.compose.theme.CometChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier

/**
 * ENG-38681 — Compose CometChatAudioVisualizer matrix (style, all scalar) +
 * render at a non-default amplitude.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAudioVisualizerComposeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyStyleField_isOverridable() {
        lateinit var base: CometChatAudioVisualizerStyle
        lateinit var custom: CometChatAudioVisualizerStyle
        rule.setContent {
            CometChatTheme {
                base = CometChatAudioVisualizerStyle.default()
                val c = Color(0xFFAB12CD); val d = 77.dp
                custom = base.copy(
                    chunkColor = c, barColor = c, activeBarColor = c,
                    chunkWidth = d, chunkSpacing = d, chunkMinHeight = d,
                    chunkMaxHeight = d, chunkCornerRadius = d, chunkCount = 99,
                )
                CometChatAudioVisualizer(amplitude = 0.7f, style = custom)
            }
        }
        rule.waitForIdle()
        val fields = CometChatAudioVisualizerStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers)
        }
        fields.forEach { it.isAccessible = true }
        val unchanged = fields.filter { it.get(base) == it.get(custom) }
        assertEquals("every style field overridden; missed=${unchanged.map { it.name }}", emptyList<String>(), unchanged.map { it.name })
        assertEquals("expected 9 style fields", 9, fields.size)
    }

    @Test
    fun rendersAtAmplitude() {
        rule.setContent { CometChatTheme { CometChatAudioVisualizer(amplitude = 0.9f) } }
        rule.waitForIdle()
        rule.onRoot().assertExists()
    }
}
