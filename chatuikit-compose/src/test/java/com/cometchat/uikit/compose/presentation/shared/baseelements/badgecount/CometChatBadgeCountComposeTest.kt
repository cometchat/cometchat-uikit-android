package com.cometchat.uikit.compose.presentation.shared.baseelements.badgecount

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.baseelements.badgecount.CometChatBadgeCount
import com.cometchat.uikit.compose.theme.CometChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier

/**
 * ENG-38681 — Compose CometChatBadgeCount matrix (style) + functional (count
 * renders). Value-class style data class; typography (theme object) waived.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatBadgeCountComposeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyScalarStyleField_isOverridable() {
        lateinit var base: BadgeCountStyle
        lateinit var custom: BadgeCountStyle
        rule.setContent {
            CometChatTheme {
                base = BadgeCountStyle.default()
                val c = Color(0xFFAB12CD); val d = 77.dp; val ts = TextStyle(fontSize = 33.sp)
                custom = base.copy(
                    backgroundColor = c, borderColor = c, borderWidth = d, cornerRadius = d,
                    textColor = c, textStyle = ts, size = d,
                )
                CometChatBadgeCount(count = 3, style = custom)
            }
        }
        rule.waitForIdle()
        val scalarFields = BadgeCountStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers) && it.name != "typography"
        }
        scalarFields.forEach { it.isAccessible = true }
        val unchanged = scalarFields.filter { it.get(base) == it.get(custom) }
        assertEquals("every scalar style field overridden; missed=${unchanged.map { it.name }}", emptyList<String>(), unchanged.map { it.name })
        assertEquals("expected 7 scalar style fields", 7, scalarFields.size)
    }

    @Test
    fun count_rendersTheNumber() {
        rule.setContent { CometChatTheme { CometChatBadgeCount(count = 42) } }
        rule.waitForIdle()
        rule.onNodeWithText("42").assertIsDisplayed()
    }
}
