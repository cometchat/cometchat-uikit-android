package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatTextBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Instrumented layer for [CometChatTextBubble].
 *
 * Hosts the real composable and drives it, which is the only way to reach the
 * markdown segment renderers ([CodeBlockBubble], [BlockquoteBubble]) and the
 * segment splitter — the paths the property layer waived, because Compose exposes
 * no semantics for them.
 *
 * It does NOT recover `onLinkClick` routing: that resolves a tap through the text
 * layout result, which does not work under Robolectric. See
 * [tappingALink_doesNotCrash_butUrlRoutingIsNotObservableUnderRobolectric] — the
 * gap is asserted explicitly rather than left to look like coverage.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatTextBubbleComposeFunctionalTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        text: String,
        alignment: UIKitConstants.MessageBubbleAlignment = UIKitConstants.MessageBubbleAlignment.LEFT,
        onLinkClick: ((String) -> Unit)? = null,
    ) {
        composeRule.setContent {
            CometChatTheme {
                CometChatTextBubble(
                    message = MockFactory.createTextMessage(text = text),
                    alignment = alignment,
                    style = when (alignment) {
                        UIKitConstants.MessageBubbleAlignment.RIGHT -> CometChatTextBubbleStyle.outgoing()
                        else -> CometChatTextBubbleStyle.incoming()
                    },
                    onLinkClick = onLinkClick,
                )
            }
        }
    }

    @Test
    fun plainText_rendersVerbatim() {
        render("just a plain message")

        composeRule.onNodeWithText("just a plain message").assertIsDisplayed()
    }

    @Test
    fun markdownLink_rendersItsLabelWithoutTheSyntax() {
        // What IS reachable offline: the link label renders and the `[..](..)`
        // syntax is consumed.
        render(text = "[CometChat](https://cometchat.com)", onLinkClick = {})

        composeRule.onNodeWithText("CometChat").assertIsDisplayed()
    }

    @Test
    fun tappingALink_doesNotCrash_butUrlRoutingIsNotObservableUnderRobolectric() {
        // MEASURED GAP, not a passing assertion dressed up as one.
        //
        // ClickableLinkText resolves a tap to a URL by asking the BasicText layout
        // result for the character offset, then looking up URL_ANNOTATION_TAG at
        // that offset. Under Robolectric that lookup yields nothing — verified with
        // both a bare URL and markdown link syntax, with the label rendering and the
        // tap landing on the node in both cases. `onLongClick` on the same modifier
        // DOES fire, so gestures are delivered; it is specifically the
        // offset-to-annotation resolution that is unavailable.
        //
        // So onLinkClick routing is covered by NO offline layer: the screenshot
        // layer renders but never taps. It needs a real-device instrumented test.
        // Tracked rather than silently waived.
        var clicked: String? = null
        render(text = "[CometChat](https://cometchat.com)", onLinkClick = { clicked = it })

        composeRule.onNodeWithText("CometChat").performClick()
        composeRule.waitForIdle()

        assertNull("documenting the gap: routing does not resolve offline", clicked)
    }

    @Test
    fun markdownEmphasis_isStrippedFromTheRenderedText() {
        // "**bold**" must render as "bold" — the markers are consumed by the
        // inline parser, which is what the position map then has to compensate for.
        render("a **bold** word")

        composeRule.onNodeWithText("a bold word").assertIsDisplayed()
    }

    @Test
    fun fencedCodeBlock_rendersAsACodeSegment() {
        render("before\n```\nval x = 1\n```\nafter")

        // The fence is stripped and the code body is rendered by CodeBlockBubble.
        composeRule.onNodeWithText("val x = 1", substring = true).assertIsDisplayed()
    }

    @Test
    fun blockquote_rendersQuotedTextWithoutTheMarker() {
        render("> quoted line")

        composeRule.onNodeWithText("quoted line", substring = true).assertIsDisplayed()
    }

    @Test
    fun outgoingBubble_rendersThroughTheOutgoingStyle() {
        render("outgoing body", alignment = UIKitConstants.MessageBubbleAlignment.RIGHT)

        composeRule.onNodeWithText("outgoing body").assertIsDisplayed()
    }

    @Test
    fun emptyText_rendersWithoutCrashing() {
        // Robustness: an empty body still has to compose — the segment splitter
        // must not index past the end of an empty string.
        render("")

        composeRule.waitForIdle()
        // Nothing to assert beyond this: the test is that composing and
        // settling above did not throw. A constant assertion here would only
        // disguise that.
    }
}
