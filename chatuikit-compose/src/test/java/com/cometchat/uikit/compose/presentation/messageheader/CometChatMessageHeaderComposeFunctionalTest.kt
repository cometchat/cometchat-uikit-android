package com.cometchat.uikit.compose.presentation.messageheader

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.messageheader.ui.CometChatMessageHeader
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.shared.views.popupmenu.MenuItem
import com.cometchat.uikit.core.testutils.MockFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Instrumented layer for the compose [CometChatMessageHeader].
 *
 * The pre-existing `CometChatMessageHeaderViewModelTest` never constructs this
 * composable -- it is a Kotest spec over `CometChatMessageHeaderViewModel`, which is
 * why the component sat at 0% while looking tested. These tests host the real thing.
 *
 * Supplying `user` or `group` directly means no SDK call is needed, so the header
 * renders offline and the title, subtitle and affordances are all assertable.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessageHeaderComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val NAME = "Alice"
        const val GROUP_NAME = "Design team"
        const val SLOT = "a custom slot"
    }

    private fun user(): User = MockFactory.createUser(uid = "u1", name = NAME)

    // Must go through the parameter: createUser returns a Mockito mock with stubbed
    // getters, so assigning status afterwards is silently ignored.
    private fun offlineUser(): User = MockFactory.createUser(
        uid = "u1",
        name = NAME,
        status = CometChatConstants.USER_STATUS_OFFLINE,
    )

    private fun group(): Group = MockFactory.createGroup(guid = "g1", name = GROUP_NAME)

    private fun render(content: @Composable () -> Unit) {
        composeRule.setContent { CometChatTheme { content() } }
        composeRule.waitForIdle()
    }

    // ── what a reader sees ──────────────────────────────────────────────────

    @Test
    fun aUserHeaderShowsTheName() {
        render { CometChatMessageHeader(user = user()) }
        composeRule.onNodeWithText(NAME).assertIsDisplayed()
    }

    @Test
    fun aUserHeaderShowsPresence() {
        render { CometChatMessageHeader(user = user()) }
        composeRule.onNodeWithText("Online").assertIsDisplayed()
    }

    @Test
    fun aGroupHeaderShowsTheGroupName() {
        render { CometChatMessageHeader(group = group()) }
        composeRule.onNodeWithText(GROUP_NAME).assertIsDisplayed()
    }

    @Test
    fun theBackAffordanceIsPresentByDefault() {
        render { CometChatMessageHeader(user = user()) }
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun withNeitherUserNorGroup_itStillComposes() {
        render { CometChatMessageHeader() }
    }

    // ── visibility flags ────────────────────────────────────────────────────

    @Test
    fun theBackButtonCanBeHidden() {
        render { CometChatMessageHeader(user = user(), hideBackButton = true) }
        assertEquals(
            0,
            composeRule.onAllNodesWithContentDescription("Back").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun userPresenceCanBeHidden() {
        render { CometChatMessageHeader(user = user(), hideUserStatus = true) }
        assertEquals(
            "the presence subtitle should be gone",
            0,
            composeRule.onAllNodesWithText("Online").fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithText(NAME).assertIsDisplayed()
    }

    @Test
    fun groupStatusCanBeHidden() {
        render { CometChatMessageHeader(group = group(), hideGroupStatus = true) }
        composeRule.onNodeWithText(GROUP_NAME).assertIsDisplayed()
    }

    @Test
    fun theCallButtonsAreHiddenByDefault() {
        // hideVideoCallButton and hideVoiceCallButton both default to true.
        render { CometChatMessageHeader(user = user()) }
        assertEquals(0, composeRule.onAllNodesWithContentDescription("Video Call").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithContentDescription("Voice Call").fetchSemanticsNodes().size)
    }

    @Test
    fun theCallButtonsStayHiddenWhileCallingIsDisabled() {
        // Both flags are only half the gate: the header also requires calling to be
        // enabled in UIKitSettings, which it is not here. Asking for the buttons is
        // therefore not enough to show them, and that is the behaviour worth pinning --
        // an integrator who unhides them without enabling calling gets nothing.
        render {
            CometChatMessageHeader(
                user = user(),
                hideVideoCallButton = false,
                hideVoiceCallButton = false,
            )
        }
        assertEquals(0, composeRule.onAllNodesWithContentDescription("Video Call").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithContentDescription("Voice Call").fetchSemanticsNodes().size)
        composeRule.onNodeWithText(NAME).assertIsDisplayed()
    }

    @Test
    fun theNewChatButtonAppearsWhenAsked() {
        render { CometChatMessageHeader(user = user(), hideNewChatButton = false) }
        composeRule.onNodeWithContentDescription("New chat").assertIsDisplayed()
    }

    @Test
    fun theChatHistoryButtonAppearsWhenAsked() {
        render { CometChatMessageHeader(user = user(), hideChatHistoryButton = false) }
        composeRule.onNodeWithContentDescription("Chat history").assertIsDisplayed()
    }

    @Test
    fun theMenuNeedsOptionsAsWellAsTheFlag() {
        // Gated on `!hideMenuIcon && !options.isNullOrEmpty()` -- unhiding it without
        // supplying options renders nothing.
        render { CometChatMessageHeader(user = user(), hideMenuIcon = false) }
        assertEquals(
            "no options means no menu, whatever the flag says",
            0,
            composeRule.onAllNodesWithContentDescription("Menu").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun theMenuAppearsWithOptions() {
        render {
            CometChatMessageHeader(
                user = user(),
                hideMenuIcon = false,
                options = listOf(MenuItem(id = "m1", name = "Mute")),
            )
        }
        composeRule.onNodeWithContentDescription("Menu").assertIsDisplayed()
    }

    // ── slots ───────────────────────────────────────────────────────────────

    @Test
    fun aTitleViewReplacesTheName() {
        render { CometChatMessageHeader(user = user(), titleView = { _, _ -> Text(SLOT) }) }
        composeRule.onNodeWithText(SLOT).assertIsDisplayed()
        assertEquals(
            "the default title should give way",
            0,
            composeRule.onAllNodesWithText(NAME).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun aSubtitleViewReplacesPresence() {
        render { CometChatMessageHeader(user = user(), subtitleView = { _, _ -> Text(SLOT) }) }
        composeRule.onNodeWithText(SLOT).assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Online").fetchSemanticsNodes().size)
    }

    @Test
    fun aLeadingViewReplacesTheAvatar() {
        render { CometChatMessageHeader(user = user(), leadingView = { _, _ -> Text(SLOT) }) }
        composeRule.onNodeWithText(SLOT).assertIsDisplayed()
    }

    @Test
    fun aTrailingViewRenders() {
        render { CometChatMessageHeader(user = user(), trailingView = { _, _ -> Text(SLOT) }) }
        composeRule.onNodeWithText(SLOT).assertIsDisplayed()
    }

    @Test
    fun anAuxiliaryViewRenders() {
        render { CometChatMessageHeader(user = user(), auxiliaryView = { _, _ -> Text(SLOT) }) }
        composeRule.onNodeWithText(SLOT).assertIsDisplayed()
    }

    @Test
    fun anItemViewReplacesTheWholeRow() {
        render { CometChatMessageHeader(user = user(), itemView = { _, _ -> Text(SLOT) }) }
        composeRule.onNodeWithText(SLOT).assertIsDisplayed()
        assertEquals(
            "itemView should replace the default title too",
            0,
            composeRule.onAllNodesWithText(NAME).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun aSlotReceivesTheUserItWasGiven() {
        // The slots are (User?, Group?) -> Unit, so the header must hand the data down
        // rather than just calling an opaque lambda.
        render {
            CometChatMessageHeader(
                user = user(),
                titleView = { u, _ -> Text(u?.name ?: "no user") },
            )
        }
        composeRule.onNodeWithText(NAME).assertIsDisplayed()
    }

    // ── formatters ──────────────────────────────────────────────────────────

    @Test
    fun theLastSeenFormatterOnlyAppliesWhenOffline() {
        // An online user shows the "Online" string; the formatter is the offline branch.
        render {
            CometChatMessageHeader(
                user = user(),
                lastSeenTextFormatter = { _, u -> "seen by ${u.name}" },
            )
        }
        composeRule.onNodeWithText("Online").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("seen by $NAME").fetchSemanticsNodes().size)
    }

    @Test
    fun anOfflineUserGetsTheFormattedLastSeen() {
        render {
            CometChatMessageHeader(
                user = offlineUser(),
                lastSeenTextFormatter = { _, u -> "seen by ${u.name}" },
            )
        }
        composeRule.onNodeWithText("seen by $NAME").assertIsDisplayed()
    }

    // ── callbacks ───────────────────────────────────────────────────────────

    @Test
    fun backReachesTheIntegrator() {
        var pressed = false
        render { CometChatMessageHeader(user = user(), onBackPress = { pressed = true }) }
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        assertTrue("back should reach the integrator", pressed)
    }

    @Test
    fun newChatReachesTheIntegrator() {
        var clicked = false
        render {
            CometChatMessageHeader(
                user = user(),
                hideNewChatButton = false,
                onNewChatClick = { clicked = true },
            )
        }
        composeRule.onNodeWithContentDescription("New chat").performClick()
        composeRule.waitForIdle()
        assertTrue(clicked)
    }

    @Test
    fun chatHistoryReachesTheIntegrator() {
        var clicked = false
        render {
            CometChatMessageHeader(
                user = user(),
                hideChatHistoryButton = false,
                onChatHistoryClick = { clicked = true },
            )
        }
        composeRule.onNodeWithContentDescription("Chat history").performClick()
        composeRule.waitForIdle()
        assertTrue(clicked)
    }
}
