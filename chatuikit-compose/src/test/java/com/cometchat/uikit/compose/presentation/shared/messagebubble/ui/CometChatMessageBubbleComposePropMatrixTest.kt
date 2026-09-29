package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatTextBubbleStyle
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
 * Property layer for [CometChatMessageBubble] — by far the widest surface in the kit,
 * and the one every other bubble is hosted by.
 *
 * The props divide into three kinds, and they are treated differently on purpose:
 *
 * - **Slots** — each one is asserted by supplying custom content and checking it
 *   renders. `contentView` additionally checks the routed child gives way.
 * - **Behaviour and suppression flags** — driven and asserted on what they change.
 * - **Per-type child styles** — twelve of them, one per message type. Only the one
 *   matching the message under test has any effect, so `textBubbleStyle` is exercised
 *   here and the other eleven are waived rather than faked: each is asserted where
 *   that bubble's own tests live, which is the only place its effect is visible.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessageBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatMessageBubble"
        const val TEXT = "Hello"
        const val SLOT = "a custom slot"

        /** The eleven child styles that a text message cannot exercise. */
        val CHILD_STYLES = listOf(
            "imageBubbleStyle", "videoBubbleStyle", "audioBubbleStyle", "fileBubbleStyle",
            "deleteBubbleStyle", "actionBubbleStyle", "callActionBubbleStyle",
            "meetCallBubbleStyle", "pollBubbleStyle", "stickerBubbleStyle",
            "collaborativeBubbleStyle",
        )

        /**
         * Params that carry no observable effect for a plain text message hosted on its
         * own, with the reason each is left to another layer.
         */
        val OTHER_WAIVERS = listOf(
            // Dependency injection: substituting a factory would assert the fake.
            "factory",
            // Reaction affordances need a message carrying reactions, which belongs to
            // the reaction-list component rather than the container.
            "onReactionClick", "onReactionLongClick", "onAddMoreReactionsClick",
            // Needs a message with replyCount > 0; the thread indicator is its own unit.
            "onThreadRepliesClick",
            // Needs a quoted message; the preview is rendered by the reply view.
            "onMessagePreviewClick",
            // Mention plumbing is asserted in the text bubble, which owns the spans.
            "onMentionClick", "onMentionAllClick", "mentionTextStyle", "textFormatters",
            // Timestamp formatting is asserted through the snapshot layer, which is the
            // only place the rendered string is visible.
            "timeFormat", "dateTimeFormatter",
            // Wrapper styles resolved per alignment; the snapshot layer pins the fills.
            "style", "incomingMessageBubbleStyle", "outgoingMessageBubbleStyle", "bubbleStyles",
        )
    }

    @Test
    fun messageBubble_propMatrix_coversEveryObservableProp() {
        var alignment by mutableStateOf(UIKitConstants.MessageBubbleAlignment.LEFT)
        var showAvatar by mutableStateOf(true)
        var leadingAlpha by mutableStateOf(1f)
        var statusInfoAlpha by mutableStateOf(1f)
        var timeStampAlignment by mutableStateOf(UIKitConstants.TimeStampAlignment.BOTTOM)
        var hideModerationView by mutableStateOf(false)
        var hideReceipts by mutableStateOf(false)
        var hideReactions by mutableStateOf(false)
        var isAgentChat by mutableStateOf(false)
        var highlightedId by mutableStateOf(-1L)
        var highlightAlpha by mutableStateOf(0f)
        var textStyleOverride by mutableStateOf<CometChatTextBubbleStyle?>(null)
        var slot by mutableStateOf<String?>(null)
        var longClicked = false

        val message = MockFactory.createTextMessage(text = TEXT)
        // The style factories are @Composable, so resolve inside the tree.
        var outgoingTextStyle: CometChatTextBubbleStyle? = null

        composeRule.setContent {
            CometChatTheme {
                outgoingTextStyle = CometChatTextBubbleStyle.outgoing()
                CometChatMessageBubble(
                    message = message,
                    alignment = alignment,
                    shouldShowDefaultAvatar = showAvatar,
                    leadingAlpha = leadingAlpha,
                    statusInfoAlpha = statusInfoAlpha,
                    timeStampAlignment = timeStampAlignment,
                    hideModerationView = hideModerationView,
                    hideReceipts = hideReceipts,
                    hideReactions = hideReactions,
                    isAgentChat = isAgentChat,
                    highlightedMessageId = highlightedId,
                    highlightAlpha = highlightAlpha,
                    textBubbleStyle = textStyleOverride,
                    leadingView = if (slot == "leadingView") ({ Text(SLOT) }) else null,
                    headerView = if (slot == "headerView") ({ Text(SLOT) }) else null,
                    replyView = if (slot == "replyView") ({ Text(SLOT) }) else null,
                    contentView = if (slot == "contentView") ({ Text(SLOT) }) else null,
                    bottomView = if (slot == "bottomView") ({ Text(SLOT) }) else null,
                    statusInfoView = if (slot == "statusInfoView") ({ Text(SLOT) }) else null,
                    threadView = if (slot == "threadView") ({ Text(SLOT) }) else null,
                    footerView = if (slot == "footerView") ({ Text(SLOT) }) else null,
                    onLongClick = { longClicked = true },
                )
            }
        }

        /** Supply a slot and assert its content reaches the screen. */
        fun assertSlotRenders(name: String) {
            slot = name
            composeRule.waitForIdle()
            composeRule.onNodeWithText(SLOT).assertIsDisplayed()
            slot = null
            composeRule.waitForIdle()
            assertEquals(
                "clearing $name should remove its content again",
                0,
                composeRule.onAllNodesWithText(SLOT).fetchSemanticsNodes().size,
            )
        }

        val matrix = composePropMatrix(OWNER) {
            value("message") {
                composeRule.waitForIdle()
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
                composeRule
                    .onNodeWithContentDescription(
                        "Message bubble: ${CometChatConstants.CATEGORY_MESSAGE} ${CometChatConstants.MESSAGE_TYPE_TEXT}",
                    )
                    .assertIsDisplayed()
            }

            value("alignment") {
                composeRule.waitForIdle()
                for (a in UIKitConstants.MessageBubbleAlignment.entries) {
                    alignment = a
                    composeRule.waitForIdle()
                    composeRule.onNodeWithText(TEXT).assertIsDisplayed()
                }
                alignment = UIKitConstants.MessageBubbleAlignment.LEFT
                composeRule.waitForIdle()
            }

            // The routed child must give way to an explicit content override.
            value("contentView") {
                slot = "contentView"
                composeRule.waitForIdle()
                composeRule.onNodeWithText(SLOT).assertIsDisplayed()
                assertEquals(
                    "the routed text bubble should give way",
                    0,
                    composeRule.onAllNodesWithText(TEXT).fetchSemanticsNodes().size,
                )
                slot = null
                composeRule.waitForIdle()
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
            }

            value("leadingView") { assertSlotRenders("leadingView") }
            value("headerView") { assertSlotRenders("headerView") }
            value("replyView") { assertSlotRenders("replyView") }
            value("bottomView") { assertSlotRenders("bottomView") }
            value("statusInfoView") { assertSlotRenders("statusInfoView") }
            value("threadView") { assertSlotRenders("threadView") }
            value("footerView") { assertSlotRenders("footerView") }

            value("shouldShowDefaultAvatar") {
                showAvatar = false
                composeRule.waitForIdle()
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
                showAvatar = true
                composeRule.waitForIdle()
            }

            // Batch grouping draws these invisibly rather than removing them, so the
            // content must survive an alpha of zero.
            value("leadingAlpha") {
                leadingAlpha = 0f
                composeRule.waitForIdle()
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
                leadingAlpha = 1f
                composeRule.waitForIdle()
            }

            value("statusInfoAlpha") {
                statusInfoAlpha = 0f
                composeRule.waitForIdle()
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
                statusInfoAlpha = 1f
                composeRule.waitForIdle()
            }

            value("timeStampAlignment") {
                for (t in UIKitConstants.TimeStampAlignment.entries) {
                    timeStampAlignment = t
                    composeRule.waitForIdle()
                    composeRule.onNodeWithText(TEXT).assertIsDisplayed()
                }
                timeStampAlignment = UIKitConstants.TimeStampAlignment.BOTTOM
                composeRule.waitForIdle()
            }

            value("hideModerationView") {
                hideModerationView = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
                hideModerationView = false
                composeRule.waitForIdle()
            }

            value("hideReceipts") {
                hideReceipts = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
                hideReceipts = false
                composeRule.waitForIdle()
            }

            value("hideReactions") {
                hideReactions = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
                hideReactions = false
                composeRule.waitForIdle()
            }

            value("isAgentChat") {
                isAgentChat = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
                isAgentChat = false
                composeRule.waitForIdle()
            }

            value("highlightedMessageId") {
                highlightedId = message.id
                highlightAlpha = 0.5f
                composeRule.waitForIdle()
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
                highlightedId = -1L
                composeRule.waitForIdle()
            }

            value("highlightAlpha") {
                highlightedId = message.id
                highlightAlpha = 1f
                composeRule.waitForIdle()
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
                highlightedId = -1L
                highlightAlpha = 0f
                composeRule.waitForIdle()
            }

            // The one child style a text message can actually exercise.
            value("textBubbleStyle") {
                composeRule.waitForIdle()
                textStyleOverride = outgoingTextStyle
                composeRule.waitForIdle()
                composeRule.onNodeWithText(TEXT).assertIsDisplayed()
                textStyleOverride = null
                composeRule.waitForIdle()
            }

            callback("onLongClick") {
                composeRule.onNodeWithText(TEXT).performTouchInput { longClick() }
                composeRule.waitForIdle()
                assertTrue("a long press should reach the integrator", longClicked)
            }
        }

        val props = matrix.evaluate() + waivedProps()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [container compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [container] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals(
            "waivers should be the eleven unreachable child styles plus the documented rest",
            CHILD_STYLES.size + OTHER_WAIVERS.size,
            cov.waived,
        )
    }

    private fun waivedProps(): List<Prop> =
        (CHILD_STYLES + OTHER_WAIVERS).map { Prop(OWNER, it, PropKind.VALUE, waived = true) }
}
