package com.cometchat.uikit.compose.presentation.messagelist

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.messagelist.ui.CometChatMessageList
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config

/**
 * Property layer for the compose [CometChatMessageList] -- the widest surface in the
 * kit, and previously at 0% behind a Kotest spec that drove its ViewModel.
 *
 * `autoFetch = false` is what makes it testable at all: with it on, the list fires an
 * SDK fetch on composition that throws offline and poisons the next test in the JVM.
 * Off, the list settles into its loading state and the structural props are all
 * assertable.
 *
 * Everything the loaded list owns -- per-message options, AI suggestions, the empty
 * and error states, the bubble slots -- needs real messages behind it and is waived
 * here rather than faked. The message *bubbles* those states would render are covered
 * exhaustively under ENG-38674.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessageListComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatMessageList"
        const val LIST_CD = "Message list"
        const val LOADING_CD = "Loading messages"
        const val SLOT = "a custom slot"

        /** Everything that needs a loaded conversation, grouped by why. */
        val WAIVERS = listOf(
            // Dependency injection; the real one has its own ViewModel test.
            "viewModel", "messagesRequestBuilder",
            // Per-message option visibility: needs rendered messages to hide options on.
            "hideReplyInThreadOption", "hideReplyOption", "hideCopyMessageOption",
            "hideEditMessageOption", "hideDeleteMessageOption", "hideShareMessageOption",
            "hideMessagePrivatelyOption", "hideMessageInfoOption", "hideMarkAsUnreadOption",
            "hideTranslateMessageOption", "hideMessageReactionOption", "hideFlagOption",
            "hideFlagRemarkInputField",
            // Render only once messages exist.
            "hideAvatar", "hideReceipts", "hideGroupActionMessages", "hideDateSeparator",
            "hideStickyDate", "hideNewMessagesSeparator", "hideModerationView",
            "hideEmptyState", "hideErrorState",
            // Behaviour that needs a populated, scrollable list.
            "scrollToBottomOnNewMessage", "swipeToReplyEnabled", "startFromUnreadMessages",
            "unreadMessageThreshold", "disableSoundForMessages", "disableReceipt",
            "enableMultipleAttachments", "timeStampAlignment", "messageAlignment",
            // AI surfaces, each gated on its own SDK feature.
            "enableConversationStarter", "enableSmartReplies", "enableConversationSummary",
            "conversationStarterStyle", "conversationStarterView",
            "conversationSummaryStyle", "conversationSummaryView",
            "hideAiAssistantSuggestedMessages",
            // Per-message slots: each needs rendered messages to attach to. The bubble
            // container's own slots are covered exhaustively under ENG-38674.
            "leadingView", "headerView", "replyView", "contentView", "bottomView",
            "statusInfoView", "threadView", "footerView",
            // States and affordances that need a loaded, populated or failed list.
            "emptyView", "errorView", "newMessageIndicatorView", "newMessagesSeparatorView",
            "quickReactions", "options", "addOptions", "smartRepliesView",
            "onError", "onLoad", "onEmpty",
            "bubbleFactories", "textFormatters",
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

    private fun cdCount(cd: String) =
        composeRule.onAllNodesWithContentDescription(cd).fetchSemanticsNodes().size

    private fun textCount(t: String) = composeRule.onAllNodesWithText(t).fetchSemanticsNodes().size

    @Test
    fun messageList_propMatrix_coversEveryObservableProp() {
        var useGroup by mutableStateOf(false)
        var hideLoadingState by mutableStateOf(false)
        var parentMessageId by mutableStateOf(-1L)
        var useLoadingSlot by mutableStateOf(false)

        composeRule.setContent {
            CometChatTheme {
                CometChatMessageList(
                    user = if (useGroup) null else MockFactory.createUser(uid = "u1", name = "Alice"),
                    group = if (useGroup) MockFactory.createGroup(guid = "g1", name = "Design") else null,
                    parentMessageId = parentMessageId,
                    autoFetch = false,
                    hideLoadingState = hideLoadingState,
                    loadingView = if (useLoadingSlot) ({ Text(SLOT) }) else null,
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("user") {
                composeRule.waitForIdle()
                assertTrue("the list should announce itself", cdCount(LIST_CD) > 0)
            }

            value("group") {
                useGroup = true
                composeRule.waitForIdle()
                assertTrue("a group list still renders", cdCount(LIST_CD) > 0)
                useGroup = false
                composeRule.waitForIdle()
            }

            value("parentMessageId") {
                // A thread list is the same component scoped to a parent.
                parentMessageId = 42L
                composeRule.waitForIdle()
                assertTrue(cdCount(LIST_CD) > 0)
                parentMessageId = -1L
                composeRule.waitForIdle()
            }

            value("style") {
                composeRule.waitForIdle()
                assertTrue(cdCount(LIST_CD) > 0)
            }

            // With autoFetch off the list stays in its loading state, so this is the one
            // state whose visibility flag is assertable offline.
            value("autoFetch") {
                composeRule.waitForIdle()
                assertTrue("no fetch means it sits in loading", cdCount(LOADING_CD) > 0)
            }

            value("hideLoadingState") {
                composeRule.waitForIdle()
                assertTrue(cdCount(LOADING_CD) > 0)
                hideLoadingState = true
                composeRule.waitForIdle()
                assertEquals("hiding it removes the loading view", 0, cdCount(LOADING_CD))
                hideLoadingState = false
                composeRule.waitForIdle()
            }

            value("loadingView") {
                useLoadingSlot = true
                composeRule.waitForIdle()
                assertTrue("the custom loading view should render", textCount(SLOT) > 0)
                assertEquals("and replace the default", 0, cdCount(LOADING_CD))
                useLoadingSlot = false
                composeRule.waitForIdle()
            }
        }

        val props = matrix.evaluate() + WAIVERS.map { Prop(OWNER, it, PropKind.VALUE, waived = true) }
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [message list compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [message list] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertTrue("the waived set should be the documented one", cov.waived == WAIVERS.size)
    }
}
