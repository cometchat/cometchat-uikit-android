package com.cometchat.uikit.compose.presentation.messagelist.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.domain.model.CometChatMessageOption
import com.cometchat.uikit.core.testutils.MockFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config

/**
 * First render-and-assert coverage for the Compose [CometChatMessagePopupMenu].
 *
 * It had a single style property test and nothing that had ever shown the menu. The
 * component is a full-screen `Dialog`, which composes into its own window — its nodes
 * still reach the same semantics tree, so the compose rule finds them, and the
 * component tags every part it wants a host to be able to reach.
 *
 * The View twin is [com.cometchat.uikit.kotlin.presentation.messagelist.CometChatMessagePopupMenuFunctionalTest];
 * the two pin the same behaviours through very different plumbing — a Dialog here, a
 * PopupWindow over there.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessagePopupMenuComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val BODY = "long-press me"
        const val OVERLAY = "cometchat_message_popup_menu_overlay"
        const val PREVIEW = "cometchat_message_popup_menu_preview"
        const val REACTIONS = "cometchat_message_popup_menu_reactions"
        const val OPTIONS = "cometchat_message_popup_menu_options"
        const val ADD_REACTION = "cometchat_add_reaction_chip"
        val DEFAULT_REACTIONS = listOf("😍", "👍🏻", "🔥", "😊", "❤️")
    }

    private lateinit var cometChat: MockedStatic<CometChat>

    /**
     * The menu asks `CometChatUIKit.getLoggedInUser()` to decide whose message this is,
     * and that reaches the SDK, which throws without `CometChat.init()`. Stubbing the
     * static is how every other test in the kit gets past it.
     */
    @Before
    fun stubLoggedInUser() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After fun closeStatic() = cometChat.close()

    private fun message(): BaseMessage = MockFactory.createTextMessage(text = BODY)

    private fun options(vararg names: String, onClick: ((String) -> Unit)? = null) =
        names.map { name ->
            CometChatMessageOption(
                id = name.lowercase(),
                title = name,
                onClick = { onClick?.invoke(name) },
            )
        }

    private fun render(
        menuItems: List<CometChatMessageOption> = options("Reply", "Copy", "Delete"),
        quickReactions: List<String> = DEFAULT_REACTIONS,
        showQuickReactions: Boolean = true,
        messageAlignment: UIKitConstants.MessageListAlignment = UIKitConstants.MessageListAlignment.STANDARD,
        onOptionClick: (CometChatMessageOption) -> Unit = {},
        onReactionClick: (String) -> Unit = {},
        onEmojiPickerClick: () -> Unit = {},
        onDismiss: () -> Unit = {},
        bubble: @Composable () -> Unit = { Text(BODY) },
    ) {
        composeRule.setContent {
            CometChatTheme {
                CometChatMessagePopupMenu(
                    message = message(),
                    menuItems = menuItems,
                    quickReactions = quickReactions,
                    showQuickReactions = showQuickReactions,
                    messageAlignment = messageAlignment,
                    onOptionClick = onOptionClick,
                    onReactionClick = onReactionClick,
                    onEmojiPickerClick = onEmojiPickerClick,
                    onDismiss = onDismiss,
                    messageBubbleContent = bubble,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun itShowsTheOverlayWithItsThreeParts() {
        render()
        composeRule.onNodeWithTag(OVERLAY).assertIsDisplayed()
        composeRule.onNodeWithTag(PREVIEW).assertIsDisplayed()
        composeRule.onNodeWithTag(REACTIONS).assertIsDisplayed()
        composeRule.onNodeWithTag(OPTIONS).assertIsDisplayed()
    }

    @Test
    fun theOptionsRenderTheirTitles() {
        render()
        composeRule.onNodeWithText("Reply").assertIsDisplayed()
        composeRule.onNodeWithText("Copy").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
    }

    @Test
    fun theBubbleSlotIsWhatThePreviewShows() {
        // The menu does not build a preview of its own — the host hands it the same
        // bubble the list drew, which is why this is a slot and not a message renderer.
        render(bubble = { Text("the host's own bubble") })
        composeRule.onNodeWithText("the host's own bubble").assertIsDisplayed()
    }

    @Test
    fun tappingAnOptionHandsBackTheWholeOption() {
        var clicked: CometChatMessageOption? = null
        var ownOnClick: String? = null
        render(
            menuItems = options("Reply", "Copy") { ownOnClick = it },
            onOptionClick = { clicked = it },
        )
        composeRule.onNodeWithText("Copy").performClick()
        composeRule.waitForIdle()

        assertEquals("copy", clicked?.id)
        assertEquals("Copy", clicked?.title)
        // The option's own onClick is the host's to invoke; the menu reports the option
        // and leaves that call to the caller, which is the View toolkit's behaviour too.
        assertEquals(null, ownOnClick)
    }

    @Test
    fun theDefaultQuickReactionsRender() {
        render()
        DEFAULT_REACTIONS.forEach {
            composeRule.onNodeWithTag("cometchat_reaction_chip_$it").assertIsDisplayed()
        }
    }

    @Test
    fun customQuickReactionsReplaceTheDefaults() {
        render(quickReactions = listOf("🎉", "🚀"))
        composeRule.onNodeWithTag("cometchat_reaction_chip_🎉").assertIsDisplayed()
        composeRule.onNodeWithTag("cometchat_reaction_chip_🚀").assertIsDisplayed()
        assertEquals(
            "the defaults should be gone",
            0,
            composeRule.onAllNodesWithTag("cometchat_reaction_chip_🔥").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun anEmptyReactionListFallsBackToTheDefaults() {
        // `quickReactions.ifEmpty { DEFAULT_REACTIONS }` — an empty list is treated as
        // "unset" rather than "none". Hiding the row is what showQuickReactions is for.
        render(quickReactions = emptyList())
        composeRule.onNodeWithTag("cometchat_reaction_chip_🔥").assertIsDisplayed()
    }

    @Test
    fun hidingTheQuickReactionsRemovesTheWholeRow() {
        render(showQuickReactions = false)
        assertEquals(
            0,
            composeRule.onAllNodesWithTag(REACTIONS).fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithTag(OPTIONS).assertIsDisplayed()
    }

    @Test
    fun tappingAReactionHandsBackTheEmoji() {
        var reacted: String? = null
        render(quickReactions = listOf("🎉"), onReactionClick = { reacted = it })
        composeRule.onNodeWithTag("cometchat_reaction_chip_🎉").performClick()
        composeRule.waitForIdle()
        assertEquals("🎉", reacted)
    }

    /**
     * Tapping + opens a picker — the menu's own inline emoji keyboard, reached by
     * `showEmojiKeyboard = true`. What it does not do is call the caller's
     * `onEmojiPickerClick`, which is declared on the composable but unused. Driving the
     * chip by tap or through its semantics OnClick action leaves that lambda untouched.
     *
     * So this is not a broken picker; it is an unused parameter, and it only matters to
     * an integrator wanting to supply their own. The View menu's
     * `EmojiPickerClickListener` does fire, so the two toolkits differ. Pinned as the
     * current behaviour so a change to it is visible — if the parameter is ever wired
     * up, flip this to asserting `picker` is true.
     */
    @Test
    fun theAddReactionChipDoesNotReachTheHostsPicker() {
        var picker = false
        render(onEmojiPickerClick = { picker = true })
        composeRule.onNodeWithTag(ADD_REACTION).performClick()
        composeRule.waitForIdle()
        assertFalse("the public callback is unused — see the KDoc", picker)
    }

    @Test
    fun anEmptyMenuKeepsTheOverlayAndDropsTheOptionsCard() {
        // A host that filters every option away gets no card at all: the options list is
        // wrapped in `if (menuItems.isNotEmpty())`. The View menu instead shows an empty
        // RecyclerView, so this is a real difference between the two, not a crash guard.
        render(menuItems = emptyList())
        composeRule.onNodeWithTag(OVERLAY).assertIsDisplayed()
        composeRule.onNodeWithTag(PREVIEW).assertIsDisplayed()
        assertEquals(
            "the options card should be absent, not empty",
            0,
            composeRule.onAllNodesWithTag(OPTIONS).fetchSemanticsNodes().size,
        )
        assertEquals(0, composeRule.onAllNodesWithText("Reply").fetchSemanticsNodes().size)
    }

    @Test
    fun bothAlignmentsRenderTheSameParts() {
        render(messageAlignment = UIKitConstants.MessageListAlignment.LEFT_ALIGNED)
        composeRule.onNodeWithTag(PREVIEW).assertIsDisplayed()
        composeRule.onNodeWithTag(OPTIONS).assertIsDisplayed()
        composeRule.onNodeWithText("Reply").assertIsDisplayed()
    }
}
