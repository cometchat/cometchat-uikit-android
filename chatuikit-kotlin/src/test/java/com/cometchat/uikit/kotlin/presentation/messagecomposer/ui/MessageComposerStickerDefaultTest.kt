package com.cometchat.uikit.kotlin.presentation.messagecomposer.ui

import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.kotlin.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-38135 — the sticker button's **default** in the View stack.
 *
 * QA filed this as "sticker option is missing in Kotlin but present in Compose". It was never
 * missing: the whole sticker feature ships in `chatuikit-kotlin` (keyboard, adapters, bubbles,
 * icon, click handler). The only difference was one default — Compose's `hideStickersButton`
 * defaults to `false`, Kotlin's `hideStickerButton` defaulted to `true` — so the View stack
 * hid a fully working button. Kotlin now matches Compose.
 *
 * A default is exactly the kind of value a later refactor can flip back without anyone
 * noticing, which is what these tests exist to prevent. They assert the **rendered** state of
 * `ivSticker`, not the private flag, so they fail on any regression that reaches the user —
 * a reverted default, a lost `updateButtonVisibility()` call, or a broken agent-chat guard.
 *
 * Run: ./gradlew :chatuikit-kotlin:testDebugUnitTest --tests "*MessageComposerStickerDefaultTest"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MessageComposerStickerDefaultTest {

    // The composer resolves the logged-in user while binding; mocked so the test never needs
    // CometChat.init(). A null logged-in user is fine for button visibility.
    private lateinit var cometChatMock: MockedStatic<CometChat>

    @Before
    fun setUp() {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
    }

    @After
    fun tearDown() {
        cometChatMock.close()
    }

    /** A plain 1:1 peer — no agentic role, so none of the agent-chat suppression applies. */
    private fun regularUser(): User = User().apply {
        uid = "user-1"
        name = "John Doe"
    }

    /**
     * An agent peer. [com.cometchat.uikit.core.utils.AgentChatDetector.isAgentChat] keys off
     * `role`, so the role — not the uid — is what makes this an agent chat.
     */
    private fun agentUser(): User = User().apply {
        uid = "agent-1"
        name = "Support Agent"
        role = UIKitConstants.AIConstants.AGENTIC_USER
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    /** Builds a real composer inside a themed activity and hands it to [block]. */
    private fun withComposer(block: (CometChatMessageComposer) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val composer = CometChatMessageComposer(activity)
            activity.setContentView(composer)
            idle()
            block(composer)
        }
        scenario.close()
    }

    private fun CometChatMessageComposer.stickerButton(): ImageView = findViewById(R.id.ivSticker)

    // ==================== The default ====================

    @Test
    fun `sticker button is shown by default in a one-to-one chat`() {
        withComposer { composer ->
            composer.setUser(regularUser())
            idle()

            assertEquals(
                "The sticker button must be visible with no configuration — this is the ENG-38135 " +
                    "default flip that brings the View stack in line with Compose.",
                View.VISIBLE,
                composer.stickerButton().visibility
            )
        }
    }

    @Test
    fun `sticker button is shown by default in a group chat`() {
        withComposer { composer ->
            composer.setGroup(
                Group().apply {
                    guid = "group-1"
                    name = "Test Group"
                    groupType = CometChatConstants.GROUP_TYPE_PUBLIC
                }
            )
            idle()

            assertEquals(
                "The default applies to group conversations too; nothing in the group path narrows it.",
                View.VISIBLE,
                composer.stickerButton().visibility
            )
        }
    }

    // ==================== The agent-chat suppression (invariant 1) ====================

    @Test
    fun `agent chat still forces the sticker button off`() {
        withComposer { composer ->
            composer.setUser(agentUser())
            idle()

            assertEquals(
                "Agent chats suppress stickers regardless of the default — setUser() forces " +
                    "hideStickerButton = true, mirroring Compose's effectiveHideStickersButton.",
                View.GONE,
                composer.stickerButton().visibility
            )
        }
    }

    @Test
    fun `agent chat refuses an explicit request to show the sticker button`() {
        withComposer { composer ->
            composer.setUser(agentUser())
            idle()

            // setHideStickerButton(false) early-returns for agent chats; an integrator cannot
            // opt an agent conversation back into stickers.
            composer.setHideStickerButton(false)
            idle()

            assertEquals(
                "setHideStickerButton(false) must stay a no-op in an agent chat.",
                View.GONE,
                composer.stickerButton().visibility
            )
        }
    }

    // ==================== Integrators can still opt out (invariant 3) ====================

    @Test
    fun `setHideStickerButton true still hides the sticker button`() {
        withComposer { composer ->
            composer.setUser(regularUser())
            idle()
            // Precondition: it is on, so the assertion below is about the setter, not the default.
            assertEquals(View.VISIBLE, composer.stickerButton().visibility)

            composer.setHideStickerButton(true)
            idle()

            assertTrue(
                "An integrator who explicitly opts out must keep getting no sticker button.",
                composer.stickerButton().visibility != View.VISIBLE
            )
        }
    }
}
