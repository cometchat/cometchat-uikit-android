package com.cometchat.uikit.compose.presentation.threadheader.ui

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.compose.R
import com.cometchat.uikit.compose.presentation.messagelist.ui.MessageListComposeTestHelper
import com.cometchat.uikit.compose.presentation.threadheader.viewmodel.ThreadHeaderViewModel
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.lightColorScheme
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.UIKitSettings
import com.cometchat.uikit.core.testutils.MockFactory
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.whenever

/**
 * Compose instrumented tests for the thread-subscription bell on CometChatThreadHeader (P3, ENG-37569).
 *
 * The bell (an un-subscribed thread renders bell-off → accessible name "Subscribe to thread") shows only
 * when the `enableThreadSubscription` gate is on, a parent message is present, and the control is not
 * hidden. The gate is set by initializing [CometChatUIKit] before composition; it is default-ON, so
 * the gate-off case opts out explicitly and `@After` restores the default.
 *
 * Run with:
 *   ./gradlew :chatuikit-compose:connectedDebugAndroidTest --tests "*CometChatThreadHeaderSubscriptionComposeTest"
 */
@RunWith(AndroidJUnit4::class)
class CometChatThreadHeaderSubscriptionComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Accessible name of the bell when the thread is not subscribed (bell-off). Resolved from the
    // same string resource the bell renders, so the test tracks copy changes.
    private val unmuteDescription: String
        get() = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.cometchat_thread_unmute)

    @Before
    fun setup() {
        MessageListComposeTestHelper.ensureInitialized()
    }

    @After
    fun restoreThreadSubscriptionGate() {
        // The gate lives on CometChatUIKit's process-wide settings and is default-ON, so a gate-off
        // case must not be left behind for whatever runs next.
        setThreadSubscriptionGate(true)
    }

    @Suppress("DEPRECATION")

    private fun arg(name: String): String? =
        InstrumentationRegistry.getArguments().getString(name)?.takeIf { it.isNotBlank() }

    /**
     * Flips the `enableThreadSubscription` gate the bell reads, by initializing [CometChatUIKit]
     * before composition.
     *
     * **Runs on the main thread.** [CometChatUIKit.init] asserts its own threading contract
     * (ENG-38658 / X5) and throws `IllegalStateException` off the main thread; a JUnit test body
     * runs on the instrumentation thread, not the main one. `runOnMainSync` blocks until the
     * block returns, so `authenticationSettings` is assigned before `setContent` composes the
     * header — which is the ordering every assertion here depends on. The View-side twin of this
     * test needs no such wrapping because it initializes inside `Fragment.onCreateView`, already
     * on the main thread.
     *
     * Credentials come from instrumentation runner arguments (ENG-38650), never hardcoded — same
     * argument names as [MessageListComposeTestHelper] and the sample apps' `E2ETestConfig`. The
     * placeholder fallbacks are deliberate and sufficient: this test never authenticates, and the
     * assertions only read the gate off the settings, so the app ID and auth key are inert. Do not
     * substitute real credentials — a value that works offline keeps this suite runnable without
     * secrets.
     */
    private fun setThreadSubscriptionGate(enabled: Boolean) {
        val settings = UIKitSettings.UIKitSettingsBuilder()
            .setAppId(arg("appId") ?: "YOUR_APP_ID")
            .setRegion(arg("region") ?: "in")
            .setAuthKey(arg("authKey") ?: "YOUR_AUTH_KEY")
            .setEnableThreadSubscription(enabled)
            .build()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            CometChatUIKit.init(ApplicationProvider.getApplicationContext(), settings, null)
        }
    }

    /**
     * The default fixture roots the thread in a group. [oneToOneParentMessage] covers the 1-1 case,
     * where the bell is offered too (ENG-38903) — the control follows threading's own scope.
     */
    private fun parentMessage(id: Long = 200L): BaseMessage {
        val message = MockFactory.createTextMessage(
            id = id,
            senderUid = "user-1",
            text = "Parent message",
            receiverId = "group-1",
            receiverType = CometChatConstants.RECEIVER_TYPE_GROUP,
            sentAt = 1735689600L
        )
        whenever(message.replyCount).thenReturn(3)
        return message
    }

    private fun oneToOneParentMessage(id: Long = 200L): BaseMessage {
        val message = MockFactory.createTextMessage(
            id = id,
            senderUid = "user-1",
            text = "Parent message",
            sentAt = 1735689600L
        )
        whenever(message.replyCount).thenReturn(3)
        return message
    }

    @Test
    fun bellIsShownWhenGateOn() {
        setThreadSubscriptionGate(true)
        val message = parentMessage()
        val viewModel = ThreadHeaderViewModel(enableListeners = false).apply { setParentMessage(message) }

        composeTestRule.setContent {
            CometChatTheme(colorScheme = lightColorScheme()) {
                CometChatThreadHeader(
                    modifier = Modifier.fillMaxWidth(),
                    parentMessage = message,
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(unmuteDescription).assertIsDisplayed()
    }

    @Test
    fun bellIsHiddenWhenGateOff() {
        setThreadSubscriptionGate(false)
        val message = parentMessage()
        val viewModel = ThreadHeaderViewModel(enableListeners = false).apply { setParentMessage(message) }

        composeTestRule.setContent {
            CometChatTheme(colorScheme = lightColorScheme()) {
                CometChatThreadHeader(
                    modifier = Modifier.fillMaxWidth(),
                    parentMessage = message,
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(unmuteDescription).assertDoesNotExist()
    }

    @Test
    fun bellIsHiddenWhenHideFlagSet() {
        setThreadSubscriptionGate(true)
        val message = parentMessage()
        val viewModel = ThreadHeaderViewModel(enableListeners = false).apply { setParentMessage(message) }

        composeTestRule.setContent {
            CometChatTheme(colorScheme = lightColorScheme()) {
                CometChatThreadHeader(
                    modifier = Modifier.fillMaxWidth(),
                    parentMessage = message,
                    viewModel = viewModel,
                    hideThreadSubscription = true
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(unmuteDescription).assertDoesNotExist()
    }
}
