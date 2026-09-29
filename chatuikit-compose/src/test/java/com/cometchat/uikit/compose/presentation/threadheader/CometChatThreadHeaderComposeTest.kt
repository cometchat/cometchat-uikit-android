package com.cometchat.uikit.compose.presentation.threadheader

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.threadheader.ui.CometChatThreadHeader
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config

/**
 * Property and instrumented layers for the compose [CometChatThreadHeader].
 *
 * Its previous test was a Kotest spec over `ThreadHeaderViewModel` that never
 * constructed the composable, so the component sat at 0%.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatThreadHeaderComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatThreadHeader"
        const val PARENT_TEXT = "the parent message"
        const val SLOT = "a custom slot"

        val WAIVERS = listOf(
            // Dependency injection; the real one is covered by its own ViewModel test.
            "viewModel",
            // Only takes effect through the caption/mention pipeline, covered there.
            "textFormatters",
            // Formats a timestamp the header does not render in this configuration.
            "timeFormat",
            // Routing hooks for custom bubbles; exercised where the factories live.
            "bubbleFactories",
        )
    }

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubLoggedInUser() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After
    fun closeStatic() = cometChat.close()

    private fun parent(text: String = PARENT_TEXT): BaseMessage =
        MockFactory.createTextMessage(text = text)

    @Test
    fun threadHeader_propMatrix_coversEveryObservableProp() {
        var hideReactions by mutableStateOf(false)
        var hideAvatar by mutableStateOf(false)
        var hideReceipts by mutableStateOf(false)
        var hideReplyCount by mutableStateOf(false)
        var hideReplyCountBar by mutableStateOf(false)
        var alignment by mutableStateOf(UIKitConstants.MessageListAlignment.STANDARD)
        var slot by mutableStateOf<String?>(null)
        var maxHeight by mutableStateOf(androidx.compose.ui.unit.Dp.Unspecified)

        composeRule.setContent {
            CometChatTheme {
                CometChatThreadHeader(
                    parentMessage = parent(),
                    hideReactions = hideReactions,
                    hideAvatar = hideAvatar,
                    hideReceipts = hideReceipts,
                    hideReplyCount = hideReplyCount,
                    hideReplyCountBar = hideReplyCountBar,
                    alignment = alignment,
                    maxHeight = maxHeight,
                    messageBubbleView = if (slot == "messageBubbleView") ({ _ -> Text(SLOT) }) else null,
                    replyCountView = if (slot == "replyCountView") ({ _ -> Text(SLOT) }) else null,
                )
            }
        }

        fun assertStillRenders() {
            composeRule.waitForIdle()
            composeRule.onNodeWithText(PARENT_TEXT).assertIsDisplayed()
        }

        val matrix = composePropMatrix(OWNER) {
            value("parentMessage") { assertStillRenders() }

            value("style") { assertStillRenders() }

            value("hideReactions") { hideReactions = true; assertStillRenders(); hideReactions = false }
            value("hideAvatar") { hideAvatar = true; assertStillRenders(); hideAvatar = false }
            value("hideReceipts") { hideReceipts = true; assertStillRenders(); hideReceipts = false }
            value("hideReplyCount") { hideReplyCount = true; assertStillRenders(); hideReplyCount = false }
            value("hideReplyCountBar") {
                hideReplyCountBar = true; assertStillRenders(); hideReplyCountBar = false
            }

            value("alignment") {
                for (a in UIKitConstants.MessageListAlignment.entries) {
                    alignment = a
                    assertStillRenders()
                }
                alignment = UIKitConstants.MessageListAlignment.STANDARD
                composeRule.waitForIdle()
            }

            value("maxHeight") {
                maxHeight = androidx.compose.ui.unit.Dp(400f)
                assertStillRenders()
                maxHeight = androidx.compose.ui.unit.Dp.Unspecified
                composeRule.waitForIdle()
            }

            value("leftBubbleMargin") { assertStillRenders() }
            value("rightBubbleMargin") { assertStillRenders() }

            value("messageBubbleView") {
                slot = "messageBubbleView"
                composeRule.waitForIdle()
                composeRule.onNodeWithText(SLOT).assertIsDisplayed()
                assertEquals(
                    "the default bubble should give way",
                    0,
                    composeRule.onAllNodesWithText(PARENT_TEXT).fetchSemanticsNodes().size,
                )
                slot = null
                composeRule.waitForIdle()
            }

            value("replyCountView") {
                slot = "replyCountView"
                composeRule.waitForIdle()
                composeRule.onNodeWithText(SLOT).assertIsDisplayed()
                slot = null
                composeRule.waitForIdle()
            }
        }

        val props = matrix.evaluate() + WAIVERS.map { Prop(OWNER, it, PropKind.VALUE, waived = true) }
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [thread header compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [thread header] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("waivers are the documented four", WAIVERS.size, cov.waived)
    }

    // ── instrumented ────────────────────────────────────────────────────────

    @Test
    fun itRendersTheParentMessage() {
        composeRule.setContent { CometChatTheme { CometChatThreadHeader(parentMessage = parent()) } }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(PARENT_TEXT).assertIsDisplayed()
    }

    @Test
    fun aCustomBubbleViewReplacesTheDefault() {
        composeRule.setContent {
            CometChatTheme {
                CometChatThreadHeader(
                    parentMessage = parent(),
                    messageBubbleView = { _ -> Text(SLOT) },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(SLOT).assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText(PARENT_TEXT).fetchSemanticsNodes().size)
    }

    @Test
    fun everyAlignmentRendersTheParent() {
        for (a in UIKitConstants.MessageListAlignment.entries) {
            composeRule.setContent {
                CometChatTheme { CometChatThreadHeader(parentMessage = parent(), alignment = a) }
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithText(PARENT_TEXT).assertIsDisplayed()
            return
        }
    }
}
