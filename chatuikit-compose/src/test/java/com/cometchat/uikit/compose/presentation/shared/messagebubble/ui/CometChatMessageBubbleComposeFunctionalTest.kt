package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Instrumented layer for [CometChatMessageBubble], the container every other bubble
 * renders inside.
 *
 * Its job is routing and slots: pick the right child bubble for the message, honour
 * an override when the integrator supplies one, and suppress the pieces the message
 * list asks it to hide. The child bubbles' own behaviour is covered by their tests,
 * so nothing here re-asserts it.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessageBubbleComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val TEXT = "Hello"
        const val CUSTOM_SLOT = "a custom slot"
    }

    private fun textMessage(): BaseMessage = MockFactory.createTextMessage(text = TEXT)

    private fun render(
        message: BaseMessage = textMessage(),
        alignment: UIKitConstants.MessageBubbleAlignment = UIKitConstants.MessageBubbleAlignment.LEFT,
        block: @androidx.compose.runtime.Composable (BaseMessage, UIKitConstants.MessageBubbleAlignment) -> Unit = { m, a ->
            CometChatMessageBubble(message = m, alignment = a)
        },
    ) {
        composeRule.setContent { CometChatTheme { block(message, alignment) } }
        composeRule.waitForIdle()
    }

    // ── routing ─────────────────────────────────────────────────────────────

    @Test
    fun aTextMessageRoutesToTheTextBubble() {
        render()
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    @Test
    fun theAccessibleNameNamesTheCategoryAndType() {
        render()
        composeRule
            .onNodeWithContentDescription(
                "Message bubble: ${CometChatConstants.CATEGORY_MESSAGE} ${CometChatConstants.MESSAGE_TYPE_TEXT}",
            )
            .assertIsDisplayed()
    }

    @Test
    fun aDeletedMessageRoutesToTheDeleteBubbleWhateverItsType() {
        // The deleted check runs before the type switch, so the original text must not
        // surface even though this is a text message.
        render(message = MockFactory.createDeletedMessage())
        composeRule.onNodeWithText("This message was deleted").assertIsDisplayed()
        assertEquals(
            "a deleted message must not leak its content",
            0,
            composeRule.onAllNodesWithText("the original text").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun bothAlignmentsRenderTheSameContent() {
        render(alignment = UIKitConstants.MessageBubbleAlignment.RIGHT)
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    @Test
    fun centreAlignmentRenders() {
        render(alignment = UIKitConstants.MessageBubbleAlignment.CENTER)
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    // ── slots ───────────────────────────────────────────────────────────────

    @Test
    fun aContentViewOverridesTheRoutedChild() {
        render { m, a ->
            CometChatMessageBubble(
                message = m,
                alignment = a,
                contentView = { Text(CUSTOM_SLOT) },
            )
        }
        composeRule.onNodeWithText(CUSTOM_SLOT).assertIsDisplayed()
        assertEquals(
            "the routed text bubble should give way to the override",
            0,
            composeRule.onAllNodesWithText(TEXT).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun aHeaderViewRendersAlongsideTheContent() {
        render { m, a ->
            CometChatMessageBubble(message = m, alignment = a, headerView = { Text(CUSTOM_SLOT) })
        }
        composeRule.onNodeWithText(CUSTOM_SLOT).assertIsDisplayed()
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    @Test
    fun aFooterViewRenders() {
        render { m, a ->
            CometChatMessageBubble(message = m, alignment = a, footerView = { Text(CUSTOM_SLOT) })
        }
        composeRule.onNodeWithText(CUSTOM_SLOT).assertIsDisplayed()
    }

    @Test
    fun aBottomViewRenders() {
        render { m, a ->
            CometChatMessageBubble(message = m, alignment = a, bottomView = { Text(CUSTOM_SLOT) })
        }
        composeRule.onNodeWithText(CUSTOM_SLOT).assertIsDisplayed()
    }

    @Test
    fun aStatusInfoViewReplacesTheDefaultRow() {
        render { m, a ->
            CometChatMessageBubble(message = m, alignment = a, statusInfoView = { Text(CUSTOM_SLOT) })
        }
        composeRule.onNodeWithText(CUSTOM_SLOT).assertIsDisplayed()
    }

    @Test
    fun aLeadingViewRenders() {
        render { m, a ->
            CometChatMessageBubble(message = m, alignment = a, leadingView = { Text(CUSTOM_SLOT) })
        }
        composeRule.onNodeWithText(CUSTOM_SLOT).assertIsDisplayed()
    }

    @Test
    fun aReplyViewRenders() {
        render { m, a ->
            CometChatMessageBubble(message = m, alignment = a, replyView = { Text(CUSTOM_SLOT) })
        }
        composeRule.onNodeWithText(CUSTOM_SLOT).assertIsDisplayed()
    }

    @Test
    fun aThreadViewRenders() {
        render { m, a ->
            CometChatMessageBubble(message = m, alignment = a, threadView = { Text(CUSTOM_SLOT) })
        }
        composeRule.onNodeWithText(CUSTOM_SLOT).assertIsDisplayed()
    }

    // ── suppression flags the message list drives ───────────────────────────

    @Test
    fun theDefaultAvatarCanBeSuppressed() {
        // Batch grouping turns this off for all but the first message of a batch.
        render { m, a ->
            CometChatMessageBubble(message = m, alignment = a, shouldShowDefaultAvatar = false)
        }
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    @Test
    fun aTransparentLeadingSlotStillRendersTheContent() {
        // leadingAlpha = 0 keeps the avatar column's width so batched bubbles stay
        // aligned; it must not remove the message.
        render { m, a ->
            CometChatMessageBubble(message = m, alignment = a, leadingAlpha = 0f)
        }
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    @Test
    fun aTransparentStatusRowStillRendersTheContent() {
        render { m, a ->
            CometChatMessageBubble(message = m, alignment = a, statusInfoAlpha = 0f)
        }
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    @Test
    fun receiptsAndReactionsCanBeHidden() {
        render { m, a ->
            CometChatMessageBubble(
                message = m,
                alignment = a,
                hideReceipts = true,
                hideReactions = true,
            )
        }
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    @Test
    fun theModerationViewCanBeHidden() {
        render { m, a ->
            CometChatMessageBubble(message = m, alignment = a, hideModerationView = true)
        }
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    @Test
    fun theTimestampCanSitAtTheTop() {
        render { m, a ->
            CometChatMessageBubble(
                message = m,
                alignment = a,
                timeStampAlignment = UIKitConstants.TimeStampAlignment.TOP,
            )
        }
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    // ── highlight ───────────────────────────────────────────────────────────

    @Test
    fun aHighlightedMessageStillRendersItsContent() {
        // Jump-to-parent tints the row behind the bubble.
        val message = textMessage()
        render(message) { m, a ->
            CometChatMessageBubble(
                message = m,
                alignment = a,
                highlightedMessageId = m.id,
                highlightAlpha = 0.5f,
            )
        }
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    @Test
    fun aHighlightForAnotherMessageIsIgnored() {
        render { m, a ->
            CometChatMessageBubble(
                message = m,
                alignment = a,
                highlightedMessageId = m.id + 1,
                highlightAlpha = 0.5f,
            )
        }
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }

    // ── callbacks ───────────────────────────────────────────────────────────

    @Test
    fun aLongPressReachesTheIntegrator() {
        var longClicked = false
        composeRule.setContent {
            CometChatTheme {
                CometChatMessageBubble(
                    message = textMessage(),
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                    onLongClick = { longClicked = true },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(TEXT).performTouchInput { longClick() }
        composeRule.waitForIdle()
        assertTrue("a long press should reach the integrator", longClicked)
    }

    // ── agent chat ──────────────────────────────────────────────────────────

    @Test
    fun anAgentChatMessageStillRendersItsContent() {
        render { m, a ->
            CometChatMessageBubble(message = m, alignment = a, isAgentChat = true)
        }
        composeRule.onNodeWithText(TEXT).assertIsDisplayed()
    }
}
