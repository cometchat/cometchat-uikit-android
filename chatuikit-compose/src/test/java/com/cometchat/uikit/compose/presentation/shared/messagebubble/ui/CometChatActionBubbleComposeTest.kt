package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatActionBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Property and instrumented layers for [CometChatActionBubble], the centred system
 * line ("X added Y to the group"). Three overloads: `Action`, `String`, `AnnotatedString`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatActionBubbleComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatActionBubble"
        const val TEXT = "Sender added Receiver to the group"
    }

    @Test
    fun actionBubble_propMatrix_coversEveryProp() {
        var text by mutableStateOf(TEXT)
        var style: CometChatActionBubbleStyle? = null

        composeRule.setContent {
            CometChatTheme {
                style = CometChatActionBubbleStyle.default()
                CometChatActionBubble(text = text, style = style!!)
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("text") {
                composeRule.waitForIdle()
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
                text = "Sender removed Receiver"
                composeRule.waitForIdle()
                composeRule.onNodeWithText("Sender removed Receiver").assertIsDisplayed()
                assertEquals(0, composeRule.onAllNodesWithText(TEXT).fetchSemanticsNodes().size)
                text = TEXT
                composeRule.waitForIdle()
            }

            value("style") {
                composeRule.waitForIdle()
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        println("  [action compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        val uncovered = props.filter { !it.covered }.map { it.name }
        if (uncovered.isNotEmpty()) println("  [action] NOT covered: $uncovered")

        assertEquals("every prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    // ── instrumented ────────────────────────────────────────────────────────

    @Test
    fun theStringOverloadRendersItsText() {
        composeRule.setContent { CometChatTheme { CometChatActionBubble(text = TEXT) } }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    @Test
    fun theAnnotatedStringOverloadRendersItsText() {
        composeRule.setContent {
            CometChatTheme { CometChatActionBubble(text = AnnotatedString(TEXT)) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    @Test
    fun anEmptyStringStillComposes() {
        composeRule.setContent { CometChatTheme { CometChatActionBubble(text = "") } }
        composeRule.waitForIdle()
    }

    @Test
    fun aLongLineWraps() {
        val long = "Sender added Receiver, and a great many other people besides, to the group"
        composeRule.setContent { CometChatTheme { CometChatActionBubble(text = long) } }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(long).assertIsDisplayed()
    }
}
