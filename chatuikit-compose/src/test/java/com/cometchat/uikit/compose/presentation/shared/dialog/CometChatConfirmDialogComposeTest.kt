package com.cometchat.uikit.compose.presentation.shared.dialog

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.theme.CometChatTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier

/**
 * ENG-38681 — Compose CometChatConfirmDialog matrix (style, scalar + icon) +
 * functional (title renders; positive/negative clicks fire their callbacks).
 * The 4 TextStyle fields are waived (composite typography, matrixed elsewhere).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatConfirmDialogComposeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val textStyleFields = setOf(
        "titleTextStyle", "subtitleTextStyle", "positiveButtonTextStyle", "negativeButtonTextStyle"
    )

    @Test
    fun everyScalarStyleField_isOverridable() {
        lateinit var base: CometChatConfirmDialogStyle
        lateinit var custom: CometChatConfirmDialogStyle
        rule.setContent {
            CometChatTheme {
                base = CometChatConfirmDialogStyle.default()
                val c = Color(0xFFAB12CD); val d = 77.dp; val p = ColorPainter(Color(0xFF00C0FF))
                custom = base.copy(
                    backgroundColor = c, cornerRadius = d, strokeColor = c, strokeWidth = d, elevation = d,
                    icon = p, iconTint = c, iconBackgroundColor = c, iconSize = d, iconBackgroundSize = d,
                    titleTextColor = c, subtitleTextColor = c,
                    positiveButtonTextColor = c, positiveButtonBackgroundColor = c, positiveButtonStrokeColor = c,
                    positiveButtonStrokeWidth = d, positiveButtonCornerRadius = d,
                    negativeButtonTextColor = c, negativeButtonBackgroundColor = c, negativeButtonStrokeColor = c,
                    negativeButtonStrokeWidth = d, negativeButtonCornerRadius = d,
                )
                CometChatConfirmDialog(
                    title = "T", subtitle = "S", positiveButtonText = "OK", negativeButtonText = "Cancel",
                    style = custom, onPositiveClick = {}, onNegativeClick = {},
                )
            }
        }
        rule.waitForIdle()
        val scalarFields = CometChatConfirmDialogStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers) && it.name !in textStyleFields
        }
        scalarFields.forEach { it.isAccessible = true }
        val unchanged = scalarFields.filter { it.get(base) == it.get(custom) }
        assertEquals("every scalar style field overridden; missed=${unchanged.map { it.name }}", emptyList<String>(), unchanged.map { it.name })
        assertEquals("expected 22 scalar style fields", 22, scalarFields.size)
    }

    @Test
    fun renders_andButtonClicksFire() {
        var positive = false; var negative = false
        rule.setContent {
            CometChatTheme {
                CometChatConfirmDialog(
                    title = "Delete?", subtitle = "This cannot be undone",
                    positiveButtonText = "Confirm", negativeButtonText = "Dismiss",
                    onPositiveClick = { positive = true }, onNegativeClick = { negative = true },
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Delete?").assertIsDisplayed()
        rule.onNodeWithText("Confirm").performClick()
        rule.onNodeWithText("Dismiss").performClick()
        assertTrue("positive click should fire", positive)
        assertTrue("negative click should fire", negative)
    }
}
