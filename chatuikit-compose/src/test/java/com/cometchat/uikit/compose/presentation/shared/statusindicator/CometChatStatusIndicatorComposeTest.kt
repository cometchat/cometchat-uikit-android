package com.cometchat.uikit.compose.presentation.shared.statusindicator

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.statusindicator.CometChatStatusIndicator
import com.cometchat.uikit.compose.presentation.shared.statusindicator.StatusIndicator
import com.cometchat.uikit.compose.theme.CometChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier

/**
 * ENG-38681 — Compose CometChatStatusIndicator matrix (style) + render.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatStatusIndicatorComposeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyStyleField_isOverridable() {
        lateinit var base: CometChatStatusIndicatorStyle
        lateinit var custom: CometChatStatusIndicatorStyle
        rule.setContent {
            CometChatTheme {
                base = CometChatStatusIndicatorStyle.default()
                val c = Color(0xFFAB12CD); val d = 77.dp; val p = ColorPainter(Color(0xFF00C0FF))
                custom = base.copy(
                    strokeWidth = d, strokeColor = c, cornerRadius = d, backgroundColor = c,
                    onlineIcon = p, privateGroupIcon = p, protectedGroupIcon = p, size = d,
                )
                CometChatStatusIndicator(status = StatusIndicator.ONLINE, style = custom)
            }
        }
        rule.waitForIdle()
        val fields = CometChatStatusIndicatorStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers)
        }
        fields.forEach { it.isAccessible = true }
        val unchanged = fields.filter { it.get(base) == it.get(custom) }
        assertEquals("every style field overridden; missed=${unchanged.map { it.name }}", emptyList<String>(), unchanged.map { it.name })
        assertEquals("expected 8 style fields", 8, fields.size)
    }

    @Test
    fun rendersForStatuses() {
        rule.setContent {
            CometChatTheme {
                androidx.compose.foundation.layout.Row {
                    CometChatStatusIndicator(status = StatusIndicator.ONLINE)
                    CometChatStatusIndicator(status = StatusIndicator.PRIVATE_GROUP)
                }
            }
        }
        rule.waitForIdle()
        rule.onRoot().assertExists()
    }
}
