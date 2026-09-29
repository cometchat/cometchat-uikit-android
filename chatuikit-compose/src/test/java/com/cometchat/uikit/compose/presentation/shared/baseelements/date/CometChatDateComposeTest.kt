package com.cometchat.uikit.compose.presentation.shared.baseelements.date

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.baseelements.date.CometChatDate
import com.cometchat.uikit.compose.theme.CometChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier

/**
 * ENG-38681 — Compose CometChatDate matrix (style) + functional (custom date string).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatDateComposeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun everyScalarStyleField_isOverridable() {
        lateinit var base: DateStyle
        lateinit var custom: DateStyle
        rule.setContent {
            CometChatTheme {
                base = DateStyle.default()
                val c = Color(0xFFAB12CD); val d = 77.dp; val ts = TextStyle(fontSize = 33.sp)
                custom = base.copy(
                    backgroundColor = c, borderColor = c, borderWidth = d, cornerRadius = d,
                    textColor = c, textStyle = ts, textAlign = TextAlign.Justify,
                )
                CometChatDate(customDateString = "seed", style = custom)
            }
        }
        rule.waitForIdle()
        val scalarFields = DateStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers) && it.name != "typography"
        }
        scalarFields.forEach { it.isAccessible = true }
        val unchanged = scalarFields.filter { it.get(base) == it.get(custom) }
        assertEquals("every scalar style field overridden; missed=${unchanged.map { it.name }}", emptyList<String>(), unchanged.map { it.name })
        assertEquals("expected 7 scalar style fields", 7, scalarFields.size)
    }

    @Test
    fun customDateString_renders() {
        rule.setContent { CometChatTheme { CometChatDate(customDateString = "PM-DATE") } }
        rule.waitForIdle()
        rule.onNodeWithText("PM-DATE").assertIsDisplayed()
    }
}
