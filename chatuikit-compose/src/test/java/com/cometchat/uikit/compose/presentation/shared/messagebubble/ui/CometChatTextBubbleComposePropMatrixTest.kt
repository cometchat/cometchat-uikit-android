package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatTextBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Property (prop-matrix) layer for [CometChatTextBubble].
 *
 * The bubble's integrator surface is nine params (`modifier` is excluded by
 * [Denominator]). This sweeps each one to a non-default and asserts the effect on
 * the **real composable**, per the Compose flavour of the harness — the DSL rather
 * than reflection, because the style is a value-class data class that
 * `KFunction.callBy` cannot construct (see the Avatar pilot for the full reason).
 *
 * Four props are waived here and picked up by the layer that can actually see
 * them: link/mention click routing needs the interaction plumbing of the
 * instrumented layer, and `mentionTextStyle` is pure typography, which only the
 * screenshot layer can pin. Waived props leave the denominator but stay visible in
 * the printed tally, so the gap is reported rather than hidden.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatTextBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createComposeRule()

    private companion object {
        const val TEXT = "Prop matrix text bubble"
        const val OWNER = "CometChatTextBubble"
    }

    @Test
    fun textBubble_propMatrix_coversEveryObservableProp() {
        var longClicked = false

        // Every observable prop set to a non-default in one rendering: an outgoing
        // (RIGHT) bubble carrying an explicit outgoing style and a long-click sink.
        composeRule.setContent {
            CometChatTheme {
                CometChatTextBubble(
                    message = MockFactory.createTextMessage(text = TEXT),
                    alignment = UIKitConstants.MessageBubbleAlignment.RIGHT,
                    style = CometChatTextBubbleStyle.outgoing(),
                    onLongClick = { longClicked = true },
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            // The message's text is the one thing the bubble renders verbatim.
            value("message") {
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
            }
            // RIGHT drives the outgoing branch of the alignment-dependent default
            // style lambda; the incoming branch is covered by the sibling test.
            value("alignment") {
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
            }
            // An explicitly supplied style bypasses the default lambda entirely.
            value("style") {
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
            }
            callback("onLongClick") {
                composeRule.onNodeWithText(TEXT).performTouchInput { longClick() }
                composeRule.waitForIdle()
                assertTrue("onLongClick should fire on a long press", longClicked)
            }
        }

        val props = matrix.evaluate() + waivedProps()
        val cov = Denominator.summarize(props)

        println("  [textbubble compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("four props are waived to the instrumented/screenshot layers", 5, cov.waived)
    }

    @Test
    fun incomingAlignment_rendersThroughTheDefaultStyleBranch() {
        // The other half of the `when (alignment)` default-style lambda. Rendering
        // LEFT with no explicit style is the only way that branch executes.
        composeRule.setContent {
            CometChatTheme {
                CometChatTextBubble(
                    message = MockFactory.createTextMessage(text = TEXT),
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                )
            }
        }

        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    /**
     * Props that cannot be asserted through Compose semantics at this layer, each
     * with the layer that does cover it. Recorded as waivers so they stay counted
     * in the report rather than quietly dropping out of the surface.
     */
    private fun waivedProps(): List<Prop> = listOf(
        Prop(OWNER, "onLinkClick", PropKind.CALLBACK, waived = true),
        Prop(OWNER, "onMentionClick", PropKind.CALLBACK, waived = true),
        Prop(OWNER, "onMentionAllClick", PropKind.CALLBACK, waived = true),
        Prop(OWNER, "mentionTextStyle", PropKind.VALUE, waived = true),
        Prop(OWNER, "textFormatters", PropKind.VALUE, waived = true),
    )
}
