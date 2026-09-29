package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.models.Attachment
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatImageBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
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
 * Property (prop-matrix) layer for [CometChatImageBubble].
 *
 * Eight integrator params on the `MediaMessage` overload (`modifier` excluded by
 * [Denominator]). Unlike the text bubble there is no text to assert on, so the
 * effects are read from the accessibility semantics the bubble already publishes:
 * a bubble-level "Image message with N image(s)" and the "+N" overflow label.
 * The per-tile "Image N" description does *not* survive: Coil renders its error
 * placeholder under Robolectric, which drops it — so tiles are addressed by click
 * action and by their measured bounds.
 *
 * One rendering carries five attachments, so the grid caps at four tiles and the
 * overflow appears — which is what makes `onMoreClick` reachable in the same pass
 * as `onImageClick`.
 *
 * **On the `style` prop.** `CometChatImageBubbleStyle` declares 23 properties and
 * this composable applies only 9. Twelve of the unapplied ones are inherited
 * container properties — `backgroundColor`, `cornerRadius`, `strokeWidth`,
 * `strokeColor`, `padding`, `senderName*`, `threadIndicator*`, `timestamp*` — and
 * are drawn by `CometChatMessageBubble` instead, which is why a bare image bubble
 * has no background of its own. But **`imageStrokeWidth` and `imageStrokeColor` are
 * read by nothing in Compose** — the only compose consumer of those names is
 * `CometChatCollaborativeBubble`, reading its own style class. The View kit does
 * apply them, onto the single-image card, so this is a toolkit parity gap rather
 * than a property nobody implemented. Pinned from the working side by
 * `imageStroke_isAppliedToTheImageContainer` in the View functional test.
 *
 * Because of this, the `style` entry below asserts a *measurable* effect
 * (`gridSpacing`) rather than merely that the bubble accepted the object.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatImageBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createComposeRule()

    private companion object {
        const val OWNER = "CometChatImageBubble"
        const val CAPTION = "five photos from the trip"
        val GRID_SPACING = 16.dp
    }

    @Test
    fun imageBubble_propMatrix_coversEveryObservableProp() {
        var clickedIndex: Int? = null
        var clickedAttachment: Attachment? = null
        var moreClicked: List<Attachment>? = null
        var longClicked = false

        composeRule.setContent {
            CometChatTheme {
                CometChatImageBubble(
                    message = MockFactory.createMediaMessage(count = 5, caption = CAPTION),
                    alignment = UIKitConstants.MessageBubbleAlignment.RIGHT,
                    style = CometChatImageBubbleStyle.outgoing().copy(gridSpacing = GRID_SPACING),
                    onImageClick = { i, a -> clickedIndex = i; clickedAttachment = a },
                    onMoreClick = { moreClicked = it },
                    onLongClick = { longClicked = true },
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            // The bubble publishes the resolved attachment count in its own
            // content description — the most direct evidence the message was read.
            value("message") {
                composeRule.onNodeWithContentDescription("Image message with 5 image(s)")
                    .assertIsDisplayed()
            }
            value("alignment") {
                composeRule.onNodeWithContentDescription("Image message with 5 image(s)")
                    .assertIsDisplayed()
            }
            // A supplied style has to actually change the rendering, not merely be
            // accepted. Only 9 of this style's 23 props are applied at all (see the
            // class doc), and gridSpacing is one of the few with a measurable
            // effect — so assert the real gap between two tiles.
            value("style") {
                val tiles = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
                val gapPx = tiles[1].boundsInRoot.left - tiles[0].boundsInRoot.right
                val expectedPx = with(composeRule.density) { GRID_SPACING.toPx() }
                assertEquals(
                    "the supplied gridSpacing should set the gap between tiles",
                    expectedPx.toDouble(),
                    gapPx.toDouble(),
                    1.0,
                )
            }
            value("caption") {
                composeRule.onNodeWithText(CAPTION).assertIsDisplayed()
            }
            callback("onImageClick") {
                // Coil renders its error placeholder under Robolectric, which drops the
                // per-tile contentDescription — so tiles are addressed by click action.
                // Index 0 is the first grid tile.
                composeRule.onAllNodes(hasClickAction())[0].performClick()
                composeRule.waitForIdle()
                assertEquals("first tile reports index 0", 0, clickedIndex)
                assertTrue("the tapped attachment is handed back", clickedAttachment != null)
            }
            callback("onMoreClick") {
                // 5 attachments, 4 tiles max -> a "+1" overflow tile.
                composeRule.onNodeWithText("+1").performClick()
                composeRule.waitForIdle()
                assertEquals("the full attachment list is handed back", 5, moreClicked?.size)
            }
            callback("onLongClick") {
                composeRule.onAllNodes(hasClickAction())[0]
                    .performTouchInput { longClick() }
                composeRule.waitForIdle()
                assertTrue("onLongClick should fire on a long press", longClicked)
            }
        }

        val props = matrix.evaluate() + waivedProps()
        val cov = Denominator.summarize(props)

        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }
        println("  [imagebubble compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [imagebubble] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("only textFormatters is waived", 1, cov.waived)
    }

    @Test
    fun incomingAlignment_rendersThroughTheDefaultStyleBranch() {
        // The other half of the `when (alignment)` default-style lambda: LEFT with
        // no explicit style is the only way that branch executes.
        composeRule.setContent {
            CometChatTheme {
                CometChatImageBubble(
                    message = MockFactory.createMediaMessage(count = 1),
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Image message with 1 image(s)")
            .assertIsDisplayed()
    }

    /**
     * `textFormatters` only takes effect through the caption's markdown/mention
     * pipeline, which is already swept by the shared caption tests in
     * `MultiAttachmentBubbleUtilsTest`; there is no image-bubble-specific effect
     * to observe here.
     */
    private fun waivedProps(): List<Prop> = listOf(
        Prop(OWNER, "textFormatters", PropKind.VALUE, waived = true),
    )
}
