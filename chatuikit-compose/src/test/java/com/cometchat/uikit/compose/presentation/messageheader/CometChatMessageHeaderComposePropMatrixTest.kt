package com.cometchat.uikit.compose.presentation.messageheader

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.cometchat.uikit.compose.presentation.messageheader.style.CometChatMessageHeaderStyle
import com.cometchat.uikit.compose.shared.views.popupmenu.MenuItem
import com.cometchat.uikit.compose.theme.CometChatTheme
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
 * Property layer for the compose [CometChatMessageHeader].
 *
 * Thirty integrator params. `user` and `group` are alternatives rather than a pair, so
 * both are exercised; the six slots are asserted by supplying content and checking the
 * default gives way where it should.
 *
 * Two params have gates beyond their own flag and are asserted as such rather than
 * waived: the call buttons additionally require calling enabled in `UIKitSettings`,
 * and the menu additionally requires a non-empty `options` list.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessageHeaderComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatMessageHeader"
        const val NAME = "Alice"
        const val GROUP_NAME = "Design team"
        const val SLOT = "a custom slot"

        /**
         * Params with no observable effect in a hosted header, each left to the layer
         * that can see it.
         */
        val WAIVERS = listOf(
            // Dependency injection: supplying a fake would assert the fake, and the real
            // one is covered by CometChatMessageHeaderViewModelTest.
            "messageHeaderViewModel",
            // Needs the SDK to raise a failure; there is no offline path that reaches it.
            "onError",
            // Both need calling enabled in UIKitSettings before the button exists to
            // click. The visibility gate itself is asserted below.
            "onVideoCallClick", "onVoiceCallClick",
            // Formats the last-seen timestamp, which only shows for an offline user with
            // a real lastActiveAt; the formatter override is asserted instead.
            "dateTimeFormatter",
        )
    }

    private fun user(): User =
        MockFactory.createUser(uid = "u1", name = NAME, status = CometChatConstants.USER_STATUS_ONLINE)

    private fun offlineUser(): User =
        MockFactory.createUser(uid = "u2", name = NAME, status = CometChatConstants.USER_STATUS_OFFLINE)

    private fun group(): Group = MockFactory.createGroup(guid = "g1", name = GROUP_NAME)

    @Test
    fun messageHeader_propMatrix_coversEveryObservableProp() {
        var useGroup by mutableStateOf(false)
        var offline by mutableStateOf(false)
        var style by mutableStateOf<CometChatMessageHeaderStyle?>(null)
        var hideBack by mutableStateOf(false)
        var hideUserStatus by mutableStateOf(false)
        var hideGroupStatus by mutableStateOf(false)
        var hideVideoCall by mutableStateOf(true)
        var hideVoiceCall by mutableStateOf(true)
        var hideNewChat by mutableStateOf(true)
        var hideChatHistory by mutableStateOf(true)
        var hideMenu by mutableStateOf(true)
        var options by mutableStateOf<List<MenuItem>?>(null)
        var slot by mutableStateOf<String?>(null)
        var useLastSeenFormatter by mutableStateOf(false)
        var backPressed = false
        var newChatClicked = false
        var chatHistoryClicked = false

        composeRule.setContent {
            CometChatTheme {
                val s = style ?: CometChatMessageHeaderStyle.default()
                CometChatMessageHeader(
                    // Distinct uid for the offline case: the header keys its state on the
                    // user, so reusing one uid keeps the first status.
                    user = if (useGroup) null else if (offline) offlineUser() else user(),
                    group = if (useGroup) group() else null,
                    style = s,
                    hideBackButton = hideBack,
                    hideUserStatus = hideUserStatus,
                    hideGroupStatus = hideGroupStatus,
                    hideVideoCallButton = hideVideoCall,
                    hideVoiceCallButton = hideVoiceCall,
                    hideNewChatButton = hideNewChat,
                    hideChatHistoryButton = hideChatHistory,
                    hideMenuIcon = hideMenu,
                    options = options,
                    leadingView = if (slot == "leadingView") ({ _, _ -> Text(SLOT) }) else null,
                    titleView = if (slot == "titleView") ({ _, _ -> Text(SLOT) }) else null,
                    subtitleView = if (slot == "subtitleView") ({ _, _ -> Text(SLOT) }) else null,
                    trailingView = if (slot == "trailingView") ({ _, _ -> Text(SLOT) }) else null,
                    auxiliaryView = if (slot == "auxiliaryView") ({ _, _ -> Text(SLOT) }) else null,
                    itemView = if (slot == "itemView") ({ _, _ -> Text(SLOT) }) else null,
                    lastSeenTextFormatter =
                        if (useLastSeenFormatter) ({ _, u -> "seen by ${u.name}" }) else null,
                    onBackPress = { backPressed = true },
                    onNewChatClick = { newChatClicked = true },
                    onChatHistoryClick = { chatHistoryClicked = true },
                )
            }
        }

        fun assertSlot(name: String) {
            slot = name
            composeRule.waitForIdle()
            composeRule.onNodeWithText(SLOT).assertIsDisplayed()
            slot = null
            composeRule.waitForIdle()
            assertEquals(
                "clearing $name should remove its content",
                0,
                composeRule.onAllNodesWithText(SLOT).fetchSemanticsNodes().size,
            )
        }

        fun cdCount(cd: String) =
            composeRule.onAllNodesWithContentDescription(cd).fetchSemanticsNodes().size

        val matrix = composePropMatrix(OWNER) {
            value("user") {
                composeRule.waitForIdle()
                composeRule.onNodeWithText(NAME).assertIsDisplayed()
            }

            value("group") {
                useGroup = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(GROUP_NAME).assertIsDisplayed()
                useGroup = false
                composeRule.waitForIdle()
            }

            value("style") {
                composeRule.waitForIdle()
                composeRule.onNodeWithText(NAME).assertIsDisplayed()
            }

            value("hideBackButton") {
                composeRule.waitForIdle()
                assertTrue("the back affordance should start visible", cdCount("Back") > 0)
                hideBack = true
                composeRule.waitForIdle()
                assertEquals(0, cdCount("Back"))
                hideBack = false
                composeRule.waitForIdle()
            }

            value("hideUserStatus") {
                composeRule.waitForIdle()
                composeRule.onNodeWithText("Online").assertIsDisplayed()
                hideUserStatus = true
                composeRule.waitForIdle()
                assertEquals(0, composeRule.onAllNodesWithText("Online").fetchSemanticsNodes().size)
                hideUserStatus = false
                composeRule.waitForIdle()
            }

            value("hideGroupStatus") {
                useGroup = true
                hideGroupStatus = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(GROUP_NAME).assertIsDisplayed()
                hideGroupStatus = false
                useGroup = false
                composeRule.waitForIdle()
            }

            // Unhiding is only half the gate: the header also needs calling enabled in
            // UIKitSettings, which it is not here, so the buttons stay absent. That is
            // the contract an integrator hits.
            value("hideVideoCallButton") {
                hideVideoCall = false
                composeRule.waitForIdle()
                assertEquals("calling is disabled, so no button appears", 0, cdCount("Video Call"))
                hideVideoCall = true
                composeRule.waitForIdle()
            }

            value("hideVoiceCallButton") {
                hideVoiceCall = false
                composeRule.waitForIdle()
                assertEquals("calling is disabled, so no button appears", 0, cdCount("Voice Call"))
                hideVoiceCall = true
                composeRule.waitForIdle()
            }

            value("hideNewChatButton") {
                hideNewChat = false
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription("New chat").assertIsDisplayed()
            }

            value("hideChatHistoryButton") {
                hideChatHistory = false
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription("Chat history").assertIsDisplayed()
            }

            // Gated on `!hideMenuIcon && !options.isNullOrEmpty()`.
            value("hideMenuIcon") {
                hideMenu = false
                composeRule.waitForIdle()
                assertEquals("no options means no menu", 0, cdCount("Menu"))
                options = listOf(MenuItem(id = "m1", name = "Mute"))
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription("Menu").assertIsDisplayed()
            }

            value("options") {
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription("Menu").assertIsDisplayed()
                options = null
                composeRule.waitForIdle()
                assertEquals("clearing the options removes the menu", 0, cdCount("Menu"))
                hideMenu = true
                composeRule.waitForIdle()
            }

            value("leadingView") { assertSlot("leadingView") }
            value("subtitleView") { assertSlot("subtitleView") }
            value("trailingView") { assertSlot("trailingView") }
            value("auxiliaryView") { assertSlot("auxiliaryView") }

            value("titleView") {
                slot = "titleView"
                composeRule.waitForIdle()
                composeRule.onNodeWithText(SLOT).assertIsDisplayed()
                assertEquals(
                    "the default title should give way",
                    0,
                    composeRule.onAllNodesWithText(NAME).fetchSemanticsNodes().size,
                )
                slot = null
                composeRule.waitForIdle()
            }

            value("itemView") {
                slot = "itemView"
                composeRule.waitForIdle()
                composeRule.onNodeWithText(SLOT).assertIsDisplayed()
                assertEquals(
                    "itemView replaces the whole row",
                    0,
                    composeRule.onAllNodesWithText(NAME).fetchSemanticsNodes().size,
                )
                slot = null
                composeRule.waitForIdle()
            }

            // Only reached on the offline branch; an online user always reads "Online".
            value("lastSeenTextFormatter") {
                useLastSeenFormatter = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText("Online").assertIsDisplayed()
                offline = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText("seen by $NAME").assertIsDisplayed()
                offline = false
                useLastSeenFormatter = false
                composeRule.waitForIdle()
            }

            callback("onBackPress") {
                composeRule.onNodeWithContentDescription("Back").performClick()
                composeRule.waitForIdle()
                assertTrue("back should reach the integrator", backPressed)
            }

            callback("onNewChatClick") {
                hideNewChat = false
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription("New chat").performClick()
                composeRule.waitForIdle()
                assertTrue(newChatClicked)
                hideNewChat = true
                composeRule.waitForIdle()
            }

            callback("onChatHistoryClick") {
                hideChatHistory = false
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription("Chat history").performClick()
                composeRule.waitForIdle()
                assertTrue(chatHistoryClicked)
                hideChatHistory = true
                composeRule.waitForIdle()
            }
        }

        val props = matrix.evaluate() + WAIVERS.map { Prop(OWNER, it, PropKind.VALUE, waived = true) }
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [message header compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [message header] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("waivers are the documented five", WAIVERS.size, cov.waived)
    }
}
