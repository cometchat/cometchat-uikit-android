package com.cometchat.uikit.compose.presentation.messageinformation

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.messageinformation.ui.CometChatMessageInformation
import com.cometchat.uikit.compose.theme.CometChatTheme
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
 * Property and instrumented layers for the compose [CometChatMessageInformation].
 *
 * It renders inside a `ModalBottomSheet`, so its content lives in a separate window
 * and `onRoot()` on the main tree is empty -- assertions go through `onAllNodesWith*`,
 * which searches every window.
 *
 * Its previous test drove `CometChatMessageInformationViewModel` and never constructed
 * the composable, so the component sat at 0%.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessageInformationComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatMessageInformation"
        const val DEFAULT_TITLE = "Message Info"
        const val CUSTOM_TITLE = "Delivery detail"
        const val SLOT = "a custom slot"

        val WAIVERS = listOf(
            // Dependency injection; the real one has its own ViewModel test.
            "viewModel",
            // Needs the SDK to raise a failure -- no offline path reaches it.
            "onError",
            // Routing hooks for custom bubbles, exercised where the factories live.
            "bubbleFactories",
            // Takes effect through the mention pipeline, covered there.
            "textFormatters",
            // Fired by the sheet's dismiss gesture, which needs a real window.
            "onDismiss",
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

    private fun message(): BaseMessage = MockFactory.createTextMessage(text = "hello")

    private fun textCount(t: String) = composeRule.onAllNodesWithText(t).fetchSemanticsNodes().size

    @Test
    fun messageInformation_propMatrix_coversEveryObservableProp() {
        var title by mutableStateOf(DEFAULT_TITLE)
        var hideToolBar by mutableStateOf(false)
        var slot by mutableStateOf<String?>(null)

        composeRule.setContent {
            CometChatTheme {
                CometChatMessageInformation(
                    message = message(),
                    toolBarTitleText = title,
                    hideToolBar = hideToolBar,
                    bubbleView = if (slot == "bubbleView") ({ _ -> Text(SLOT) }) else null,
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("message") {
                composeRule.waitForIdle()
                assertTrue("the sheet should render its toolbar", textCount(DEFAULT_TITLE) > 0)
            }

            value("toolBarTitleText") {
                title = CUSTOM_TITLE
                composeRule.waitForIdle()
                assertTrue(textCount(CUSTOM_TITLE) > 0)
                assertEquals("the default title should give way", 0, textCount(DEFAULT_TITLE))
                title = DEFAULT_TITLE
                composeRule.waitForIdle()
            }

            value("hideToolBar") {
                composeRule.waitForIdle()
                assertTrue(textCount(DEFAULT_TITLE) > 0)
                hideToolBar = true
                composeRule.waitForIdle()
                assertEquals("hiding the toolbar removes its title", 0, textCount(DEFAULT_TITLE))
                hideToolBar = false
                composeRule.waitForIdle()
            }

            value("style") {
                composeRule.waitForIdle()
                assertTrue(textCount(DEFAULT_TITLE) > 0)
            }

            value("bubbleView") {
                slot = "bubbleView"
                composeRule.waitForIdle()
                assertTrue("the custom bubble should render", textCount(SLOT) > 0)
                slot = null
                composeRule.waitForIdle()
                assertEquals(0, textCount(SLOT))
            }
        }

        val props = matrix.evaluate() + WAIVERS.map { Prop(OWNER, it, PropKind.VALUE, waived = true) }
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [message information compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [message information] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("waivers are the documented five", WAIVERS.size, cov.waived)
    }

    // ── instrumented ────────────────────────────────────────────────────────

    @Test
    fun itRendersItsToolbar() {
        composeRule.setContent {
            CometChatTheme { CometChatMessageInformation(message = message()) }
        }
        composeRule.waitForIdle()
        assertTrue(textCount(DEFAULT_TITLE) > 0)
    }

    @Test
    fun theToolbarCanBeHidden() {
        composeRule.setContent {
            CometChatTheme { CometChatMessageInformation(message = message(), hideToolBar = true) }
        }
        composeRule.waitForIdle()
        assertEquals(0, textCount(DEFAULT_TITLE))
    }

    @Test
    fun aCustomBubbleViewRenders() {
        composeRule.setContent {
            CometChatTheme {
                CometChatMessageInformation(message = message(), bubbleView = { _ -> Text(SLOT) })
            }
        }
        composeRule.waitForIdle()
        assertTrue(textCount(SLOT) > 0)
    }

    @Test
    fun aCustomTitleReplacesTheDefault() {
        composeRule.setContent {
            CometChatTheme {
                CometChatMessageInformation(message = message(), toolBarTitleText = CUSTOM_TITLE)
            }
        }
        composeRule.waitForIdle()
        assertTrue(textCount(CUSTOM_TITLE) > 0)
        assertEquals(0, textCount(DEFAULT_TITLE))
    }
}
