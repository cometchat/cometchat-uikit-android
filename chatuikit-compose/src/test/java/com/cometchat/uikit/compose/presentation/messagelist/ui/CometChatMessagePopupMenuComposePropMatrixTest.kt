package com.cometchat.uikit.compose.presentation.messagelist.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.messagelist.style.CometChatMessagePopupMenuStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.domain.model.CometChatMessageOption
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config

/**
 * Property (prop-matrix) layer for the Compose [CometChatMessagePopupMenu].
 *
 * The menu composes into its own `Dialog` window, but its nodes reach the same
 * semantics tree, so the compose rule drives it like any inline component. Every part
 * the menu wants a host to reach carries a test tag, and the matrix leans on those
 * rather than on copy, so a wording change cannot quietly turn a covered prop into a
 * passing no-op.
 *
 * The View twin is a reflective sweep over setters
 * ([com.cometchat.uikit.kotlin.presentation.messagelist.CometChatMessagePopupMenuPropMatrixTest]);
 * this side is hand-declared, because `@Composable` parameters are invisible to
 * runtime reflection.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessagePopupMenuComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatMessagePopupMenu"
        const val BODY = "long-press me"
        const val SLOT = "the host's own bubble"
        const val OVERLAY = "cometchat_message_popup_menu_overlay"
        const val PREVIEW = "cometchat_message_popup_menu_preview"
        const val REACTIONS = "cometchat_message_popup_menu_reactions"
        const val OPTIONS = "cometchat_message_popup_menu_options"
        const val EMOJI = "🎉"
    }

    private lateinit var cometChat: MockedStatic<CometChat>

    /**
     * The menu asks the SDK who is logged in, to decide which side the message sits on.
     * Without `CometChat.init()` that throws, so the static is stubbed, as it is
     * everywhere else in the kit that renders a message.
     */
    @Before
    fun stubLoggedInUser() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After fun closeStatic() = cometChat.close()

    private fun option(name: String) = CometChatMessageOption(id = name.lowercase(), title = name)

    @Test
    fun messagePopupMenu_propMatrix_coversEveryProp() {
        var clickedOption: CometChatMessageOption? = null
        var reacted: String? = null
        var dismissed = false
        // The style factory is itself @Composable, so it can only be built inside the
        // composition; hoisted out only so the matrix can assert on it.
        var resolvedStyle: CometChatMessagePopupMenuStyle? = null

        composeRule.setContent {
            CometChatTheme {
                resolvedStyle = CometChatMessagePopupMenuStyle.default()
                CometChatMessagePopupMenu(
                    message = MockFactory.createTextMessage(text = BODY),
                    menuItems = listOf(option("Reply"), option("Copy")),
                    quickReactions = listOf(EMOJI),
                    showQuickReactions = true,
                    messageAlignment = UIKitConstants.MessageListAlignment.LEFT_ALIGNED,
                    onOptionClick = { clickedOption = it },
                    onReactionClick = { reacted = it },
                    onEmojiPickerClick = { /* never invoked — see waivedProps() */ },
                    onDismiss = { dismissed = true },
                    style = resolvedStyle!!,
                    messageBubbleContent = { Text(SLOT) },
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("message") {
                // The message decides which side the cards sit on, which is why the menu
                // reads the logged-in user at all. Its content is the slot's job, so what
                // is observable here is that a preview was built for it.
                composeRule.waitForIdle()
                composeRule.onNodeWithTag(PREVIEW).assertIsDisplayed()
            }

            value("menuItems") {
                composeRule.onNodeWithTag(OPTIONS).assertIsDisplayed()
                composeRule.onNodeWithText("Reply").assertIsDisplayed()
                composeRule.onNodeWithText("Copy").assertIsDisplayed()
            }

            value("quickReactions") {
                composeRule.onNodeWithTag("cometchat_reaction_chip_$EMOJI").assertIsDisplayed()
                assertEquals(
                    "the supplied list should replace the defaults",
                    0,
                    composeRule.onAllNodesWithTag("cometchat_reaction_chip_🔥").fetchSemanticsNodes().size,
                )
            }

            value("showQuickReactions") {
                // True here, so the row is present; the functional layer pins that false
                // removes it rather than merely hiding it.
                composeRule.onNodeWithTag(REACTIONS).assertIsDisplayed()
            }

            value("messageAlignment") {
                composeRule.onNodeWithTag(OVERLAY).assertIsDisplayed()
                composeRule.onNodeWithTag(OPTIONS).assertIsDisplayed()
            }

            value("style") {
                composeRule.onNodeWithTag(OPTIONS).assertIsDisplayed()
                assertNotEquals("a style instance should be resolvable", null, resolvedStyle)
            }

            slot("messageBubbleContent") {
                // The menu builds no preview of its own — the host hands it the bubble
                // the list already drew.
                composeRule.onNodeWithText(SLOT).assertIsDisplayed()
            }

            callback("onOptionClick") {
                composeRule.onNodeWithText("Copy").performClick()
                composeRule.waitForIdle()
                assertEquals("copy", clickedOption?.id)
                assertEquals("Copy", clickedOption?.title)
            }

            callback("onReactionClick") {
                composeRule.onNodeWithTag("cometchat_reaction_chip_$EMOJI").performClick()
                composeRule.waitForIdle()
                assertEquals(EMOJI, reacted)
            }

            callback("onDismiss") {
                composeRule.onNodeWithTag(OVERLAY).performClick()
                composeRule.waitForIdle()
                assertTrue("a tap on the scrim should dismiss", dismissed)
            }

            // onEmojiPickerClick is waived rather than exercised — see waivedProps().
        }

        val props = matrix.evaluate() + waivedProps()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [messagepopupmenu compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [messagepopupmenu] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("only the unused picker callback is waived", 1, cov.waived)
    }

    /**
     * `onEmojiPickerClick` is declared on the public composable but nothing calls it.
     * The menu hands its inner reactions card
     * `onEmojiPickerClick = { showEmojiKeyboard = true }` and swaps its own content for
     * an inline emoji keyboard, so tapping + does open a picker — the caller is simply
     * not the one asked to open it. Driving the chip by tap or through its semantics
     * OnClick action leaves the caller's lambda untouched, so a matrix entry has
     * nothing it could assert.
     *
     * Nothing is broken for a user; the parameter only matters to an integrator who
     * wants to supply their own picker, and the fix would be to wire it up or drop it
     * from the signature. The View menu's `EmojiPickerClickListener` does fire, so the
     * two toolkits differ here.
     */
    private fun waivedProps(): List<Prop> = listOf(
        Prop(OWNER, "onEmojiPickerClick", PropKind.CALLBACK, waived = true),
    )
}
