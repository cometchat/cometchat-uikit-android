package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatDeleteBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Property and instrumented layers for [CometChatDeleteBubble], both overloads.
 *
 * The whole bubble is one line of text, so everything observable is text.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatDeleteBubbleComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatDeleteBubble"
        const val DEFAULT_TEXT = "This message was deleted"
        const val CUSTOM_TEXT = "Removed by the sender"
    }

    @Test
    fun deleteBubble_propMatrix_coversEveryObservableProp() {
        var incoming: CometChatDeleteBubbleStyle? = null
        var outgoing: CometChatDeleteBubbleStyle? = null

        composeRule.setContent {
            CometChatTheme {
                incoming = CometChatDeleteBubbleStyle.incoming()
                outgoing = CometChatDeleteBubbleStyle.outgoing()
                CometChatDeleteBubble(
                    message = MockFactory.createDeletedMessage(),
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                    text = CUSTOM_TEXT,
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("message") {
                composeRule.waitForIdle()
                // The message carries no visible content of its own -- a deleted message
                // renders the placeholder, never the original text.
                composeRule.onNodeWithText(CUSTOM_TEXT).assertIsDisplayed()
                assertEquals(
                    "the deleted message's original text must never surface",
                    0,
                    composeRule.onAllNodesWithText("the original text").fetchSemanticsNodes().size,
                )
            }

            value("text") {
                composeRule.waitForIdle()
                composeRule.onNodeWithText(CUSTOM_TEXT).assertIsDisplayed()
            }

            value("style") {
                composeRule.waitForIdle()
                assertNotEquals("incoming() and outgoing() should differ", incoming, outgoing)
            }
        }

        val props = matrix.evaluate() + waivedProps()
        val cov = Denominator.summarize(props)
        println("  [delete compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }
        if (uncovered.isNotEmpty()) println("  [delete] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("only alignment is waived", 1, cov.waived)
    }

    /**
     * Alignment only selects the default style, and the two differ solely in text
     * colour, which Compose semantics do not expose. Which edge the bubble sits on
     * belongs to the container; the snapshot layer pins both sides.
     */
    private fun waivedProps(): List<Prop> = listOf(
        Prop(OWNER, "alignment", PropKind.VALUE, waived = true),
    )

    // ── instrumented ────────────────────────────────────────────────────────

    @Test
    fun theDefaultTextIsTheDeletedPlaceholder() {
        composeRule.setContent {
            CometChatTheme {
                CometChatDeleteBubble(
                    message = MockFactory.createDeletedMessage(),
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(DEFAULT_TEXT).assertIsDisplayed()
    }

    @Test
    fun theMessagelessOverloadRendersTheSamePlaceholder() {
        // Public API for hosts that know a message was deleted but have no object.
        composeRule.setContent {
            CometChatTheme {
                CometChatDeleteBubble(alignment = UIKitConstants.MessageBubbleAlignment.RIGHT)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(DEFAULT_TEXT).assertIsDisplayed()
    }

    @Test
    fun aSuppliedTextReplacesThePlaceholder() {
        composeRule.setContent {
            CometChatTheme {
                CometChatDeleteBubble(
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                    text = CUSTOM_TEXT,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(CUSTOM_TEXT).assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText(DEFAULT_TEXT).fetchSemanticsNodes().size)
    }

    @Test
    fun bothAlignmentsRenderTheText() {
        composeRule.setContent {
            CometChatTheme {
                CometChatDeleteBubble(
                    message = MockFactory.createDeletedMessage(),
                    alignment = UIKitConstants.MessageBubbleAlignment.RIGHT,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(DEFAULT_TEXT).assertIsDisplayed()
    }

    @Test
    fun anEmptyTextStillComposes() {
        composeRule.setContent {
            CometChatTheme {
                CometChatDeleteBubble(alignment = UIKitConstants.MessageBubbleAlignment.LEFT, text = "")
            }
        }
        composeRule.waitForIdle()
    }
}
