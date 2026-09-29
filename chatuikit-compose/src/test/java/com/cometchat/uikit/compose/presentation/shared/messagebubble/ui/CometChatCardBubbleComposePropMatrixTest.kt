package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Property layer for [CometChatCardBubble].
 *
 * The rendering and interaction layers already existed; this adds the matrix. The
 * bubble resolves its label through `getFallbackText()` → `getText()` → a string
 * resource, so the label is the observable that carries the message through.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatCardBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatCardBubble"
        const val FALLBACK = "Card Message"
    }

    @Test
    fun cardBubble_propMatrix_coversEveryObservableProp() {
        var alignment by mutableStateOf(UIKitConstants.MessageBubbleAlignment.LEFT)

        composeRule.setContent {
            CometChatTheme {
                // No card payload, so the bubble takes its fallback path and the label
                // is predictable. The full card path is covered by the pre-existing
                // rendering tests.
                CometChatCardBubble(
                    message = MockFactory.createCardMessage(cardJson = null, fallbackText = FALLBACK),
                    alignment = alignment,
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("message") {
                composeRule.waitForIdle()
                composeRule.onNodeWithText(FALLBACK).assertIsDisplayed()
            }

            value("alignment") {
                composeRule.waitForIdle()
                for (a in UIKitConstants.MessageBubbleAlignment.entries) {
                    alignment = a
                    composeRule.waitForIdle()
                    composeRule.onNodeWithText(FALLBACK).assertIsDisplayed()
                }
                alignment = UIKitConstants.MessageBubbleAlignment.LEFT
                composeRule.waitForIdle()
            }
        }

        val props = matrix.evaluate() + waivedProps()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [card compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [card] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("only onCardAction is waived", 1, cov.waived)
    }

    /**
     * Firing it needs a card with real interactive elements, which the fallback path
     * has none of. Driven in the pre-existing `CometChatCardBubbleComposeInteractionTest`.
     */
    private fun waivedProps(): List<Prop> = listOf(
        Prop(OWNER, "onCardAction", PropKind.CALLBACK, waived = true),
    )
}
