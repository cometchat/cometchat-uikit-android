package com.cometchat.uikit.compose.presentation.incomingcall.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.lightColorScheme
import com.cometchat.uikit.core.viewmodel.CometChatIncomingCallViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Compose instrumented tests for CometChatIncomingCall component.
 *
 * Tests verify:
 * - Caller name is displayed
 * - Call type text is displayed (audio/video)
 * - Accept button is displayed and clickable
 * - Decline button is displayed and clickable
 * - Accept button click triggers callback
 * - Decline button click triggers callback
 *
 * Architecture:
 *   [Mock Call] → [Real CometChatIncomingCall Composable] → [Compose Test assertions]
 *
 * The ViewModel is created with enableListeners=false to avoid SDK initialization.
 * Custom onRejectClick callback is used to avoid real SDK reject calls.
 * Sound is disabled to avoid audio playback in test environment.
 *
 * Requirements: 7.1, 7.4, 7.5, 7.6, 7.7, 7.10, 7.11
 *
 * Run with:
 *   ./gradlew :chatuikit-compose:connectedDebugAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=com.cometchat.uikit.compose.presentation.incomingcall.ui.CometChatIncomingCallComponentTest
 */
@RunWith(AndroidJUnit4::class)
class CometChatIncomingCallComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─────────────────────────────────────────────────────────────────────────
    // Mock Factory
    // ─────────────────────────────────────────────────────────────────────────

    private fun createMockUser(
        uid: String = "user-1",
        name: String = "Test Caller"
    ): User {
        val user = mock(User::class.java)
        `when`(user.uid).thenReturn(uid)
        `when`(user.name).thenReturn(name)
        `when`(user.avatar).thenReturn(null)
        `when`(user.status).thenReturn("online")
        return user
    }

    private fun createMockCall(
        sessionId: String = "session-1",
        callType: String = CometChatConstants.CALL_TYPE_AUDIO,
        caller: User = createMockUser()
    ): Call {
        val call = mock(Call::class.java)
        `when`(call.sessionId).thenReturn(sessionId)
        `when`(call.type).thenReturn(callType)
        `when`(call.callInitiator).thenReturn(caller)
        return call
    }

    private fun createViewModel(): CometChatIncomingCallViewModel {
        return CometChatIncomingCallViewModel(enableListeners = false)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 1: Incoming call displays caller name
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun incomingCall_displaysCallerName() {
        val caller = createMockUser(name = "Alice Johnson")
        val call = createMockCall(caller = caller)
        val viewModel = createViewModel()

        composeTestRule.setContent {
            CometChatTheme(colorScheme = lightColorScheme()) {
                CometChatIncomingCall(
                    call = call,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    viewModel = viewModel,
                    disableSoundForCalls = true,
                    onRejectClick = { /* no-op to avoid SDK call */ }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Alice Johnson").assertIsDisplayed()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 2: Incoming audio call displays correct call type text
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun incomingCall_displaysAudioCallTypeText() {
        val call = createMockCall(callType = CometChatConstants.CALL_TYPE_AUDIO)
        val viewModel = createViewModel()

        composeTestRule.setContent {
            CometChatTheme(colorScheme = lightColorScheme()) {
                CometChatIncomingCall(
                    call = call,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    viewModel = viewModel,
                    disableSoundForCalls = true,
                    onRejectClick = { /* no-op */ }
                )
            }
        }

        composeTestRule.waitForIdle()
        // The text is "Incoming audio Call" (format: "Incoming %s Call" with %s = "audio")
        composeTestRule.onNodeWithText("Incoming audio Call", substring = true)
            .assertIsDisplayed()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 3: Incoming video call displays correct call type text
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun incomingCall_displaysVideoCallTypeText() {
        val call = createMockCall(callType = CometChatConstants.CALL_TYPE_VIDEO)
        val viewModel = createViewModel()

        composeTestRule.setContent {
            CometChatTheme(colorScheme = lightColorScheme()) {
                CometChatIncomingCall(
                    call = call,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    viewModel = viewModel,
                    disableSoundForCalls = true,
                    onRejectClick = { /* no-op */ }
                )
            }
        }

        composeTestRule.waitForIdle()
        // The text is "Incoming video Call" (format: "Incoming %s Call" with %s = "video")
        composeTestRule.onNodeWithText("Incoming video Call", substring = true)
            .assertIsDisplayed()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 4: Incoming call displays accept button
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun incomingCall_displaysAcceptButton() {
        val caller = createMockUser(name = "Bob")
        val call = createMockCall(caller = caller)
        val viewModel = createViewModel()

        composeTestRule.setContent {
            CometChatTheme(colorScheme = lightColorScheme()) {
                CometChatIncomingCall(
                    call = call,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    viewModel = viewModel,
                    disableSoundForCalls = true,
                    onRejectClick = { /* no-op */ }
                )
            }
        }

        composeTestRule.waitForIdle()
        // Accept button has content description "Accept <callerName>"
        composeTestRule.onNodeWithContentDescription("Accept Bob")
            .assertIsDisplayed()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 5: Incoming call displays decline button
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun incomingCall_displaysDeclineButton() {
        val caller = createMockUser(name = "Bob")
        val call = createMockCall(caller = caller)
        val viewModel = createViewModel()

        composeTestRule.setContent {
            CometChatTheme(colorScheme = lightColorScheme()) {
                CometChatIncomingCall(
                    call = call,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    viewModel = viewModel,
                    disableSoundForCalls = true,
                    onRejectClick = { /* no-op */ }
                )
            }
        }

        composeTestRule.waitForIdle()
        // Decline button has content description "Decline <callerName>"
        composeTestRule.onNodeWithContentDescription("Decline Bob")
            .assertIsDisplayed()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 6: Accept button click triggers callback (via ViewModel acceptCall)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun incomingCall_acceptButtonClick_triggersCallback() {
        val caller = createMockUser(name = "Charlie")
        val call = createMockCall(caller = caller)
        val viewModel = createViewModel()
        val acceptRequested = AtomicBoolean(false)

        // Ringing v2: the accept button runs `viewModel.acceptCall()`, which marks the call
        // accepted at once (no SDK round trip). The component answers that by launching
        // CometChatOngoingCallActivity and finishing the host activity, which takes the
        // test's own activity with it ("No compose hierarchies found"). `onAcceptRequested`
        // is the app hook that sits in front of that step; capturing `proceed` without
        // calling it verifies the click wiring and keeps the test activity alive.
        composeTestRule.setContent {
            CometChatTheme(colorScheme = lightColorScheme()) {
                CometChatIncomingCall(
                    call = call,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    viewModel = viewModel,
                    disableSoundForCalls = true,
                    onAcceptRequested = { _ -> acceptRequested.set(true) },
                    onRejectClick = { /* no-op */ }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Accept Charlie")
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue("Accept tap should reach onAcceptRequested", acceptRequested.get())
        // The host activity is still here because `proceed` was never invoked.
        composeTestRule.onNodeWithContentDescription("Accept Charlie")
            .assertIsDisplayed()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 7: Decline button click triggers callback
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun incomingCall_declineButtonClick_triggersCallback() {
        val caller = createMockUser(name = "Diana")
        val call = createMockCall(caller = caller)
        val viewModel = createViewModel()
        val rejectClicked = AtomicBoolean(false)

        composeTestRule.setContent {
            CometChatTheme(colorScheme = lightColorScheme()) {
                CometChatIncomingCall(
                    call = call,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    viewModel = viewModel,
                    disableSoundForCalls = true,
                    onRejectClick = { _ ->
                        rejectClicked.set(true)
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Decline Diana")
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue(
            "Reject click callback was not triggered",
            rejectClicked.get()
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 8: Accept button displays correct text
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun incomingCall_acceptButton_displaysCorrectText() {
        val call = createMockCall()
        val viewModel = createViewModel()

        composeTestRule.setContent {
            CometChatTheme(colorScheme = lightColorScheme()) {
                CometChatIncomingCall(
                    call = call,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    viewModel = viewModel,
                    disableSoundForCalls = true,
                    onRejectClick = { /* no-op */ }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Accept").assertIsDisplayed()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 9: Decline button displays correct text
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun incomingCall_declineButton_displaysCorrectText() {
        val call = createMockCall()
        val viewModel = createViewModel()

        composeTestRule.setContent {
            CometChatTheme(colorScheme = lightColorScheme()) {
                CometChatIncomingCall(
                    call = call,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    viewModel = viewModel,
                    disableSoundForCalls = true,
                    onRejectClick = { /* no-op */ }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Decline").assertIsDisplayed()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEST 10: Component renders with dark theme without crash
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun component_rendersWithDarkTheme() {
        val caller = createMockUser(name = "Eve")
        val call = createMockCall(caller = caller)
        val viewModel = createViewModel()

        composeTestRule.setContent {
            CometChatTheme(colorScheme = com.cometchat.uikit.compose.theme.darkColorScheme()) {
                CometChatIncomingCall(
                    call = call,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    viewModel = viewModel,
                    disableSoundForCalls = true,
                    onRejectClick = { /* no-op */ }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Eve").assertIsDisplayed()
    }
}
