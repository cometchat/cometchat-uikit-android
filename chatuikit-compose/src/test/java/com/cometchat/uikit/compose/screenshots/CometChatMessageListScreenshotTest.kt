package com.cometchat.uikit.compose.screenshots

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.messagelist.ui.CometChatMessageList
import com.cometchat.uikit.compose.presentation.utils.RoborazziConfig
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.darkColorScheme
import com.cometchat.uikit.compose.theme.lightColorScheme
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Snapshot layer for the compose [CometChatMessageList].
 *
 * Replaces a suite that never constructed the component: it stacked bubbles into a
 * plain Column, so its baselines were pictures of a stand-in.
 *
 * Rows come from a ViewModel seeded directly rather than from the SDK: the fetch is
 * what needs a backend, the rendering is not. `autoFetch = false` keeps the component
 * off the network, and `hideLoadingState = true` is required for the capture to
 * terminate at all -- the loading shimmer is an infinite animation, so Compose never
 * reports idle and Roborazzi waits forever.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessageListScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/messagelist"
        )
    )

    private companion object {
        val canvasLight = Color(0xFFF4F4F4)
        val canvasDark = Color(0xFF121212)
    }

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubLoggedInUser() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After fun closeStatic() = cometChat.close()

    private fun capture(
        dark: Boolean = false,
        rtl: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContent {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                CometChatTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (dark) canvasDark else canvasLight)
                    ) { content() }
                }
                }
            }
        }
        scenario.onActivity { activity ->
            activity.window.decorView
                .findViewById<ViewGroup>(android.R.id.content)
                .getChildAt(0)
                .captureRoboImage(roborazziOptions = RoborazziConfig.options())
        }
        scenario.close()
    }

    // sentAt is pinned: this factory otherwise defaults to the current clock, which
    // renders "Today" and a wall-clock time and makes the baseline drift daily.
    private fun conversation(): List<BaseMessage> = listOf(
        MockFactory.createTextMessage(id = 1L, text = "Morning! Are we still on for the review?", senderUid = "u1", receiverId = "me", sentAt = MockFactory.FIXED_SENT_AT),
        MockFactory.createTextMessage(id = 2L, text = "Yes, 3pm works for me.", senderUid = "me", receiverId = "u1", sentAt = MockFactory.FIXED_SENT_AT),
        MockFactory.createTextMessage(id = 3L, text = "I pushed the latest deck to the shared drive.", senderUid = "me", receiverId = "u1", sentAt = MockFactory.FIXED_SENT_AT),
        MockFactory.createTextMessage(id = 4L, text = "Got it, taking a look now.", senderUid = "u1", receiverId = "me", sentAt = MockFactory.FIXED_SENT_AT),
    )

    // One empty page, or the list paginates forever: an unstubbed fetch never clears
    // "has more", so the visible-items collector re-fetches on every frame and the
    // capture never reaches idle.
    private fun inertRepository(): MessageListRepository = mock {
        onBlocking { fetchPreviousMessages() } doReturn Result.success(emptyList())
        onBlocking { fetchNextMessages(any()) } doReturn Result.success(emptyList())
    }

    private fun seededViewModel(user: User): CometChatMessageListViewModel =
        CometChatMessageListViewModel(inertRepository(), enableListeners = false).apply {
            setUser(user)
            conversation().forEach { addMessage(it) }
        }

    private fun list(): @Composable () -> Unit = {
        val alice = remember { MockFactory.createUser(uid = "u1", name = "Alice") }
        CometChatMessageList(
            viewModel = remember { seededViewModel(alice) },
            user = alice,
            autoFetch = false,
            hideLoadingState = true,
            disableReceipt = true,
        )
    }

    @Test fun userConversation() = capture(content = list())

    @Test fun userConversation_dark() = capture(dark = true, content = list())

    // ── right-to-left ───────────────────────────────────────────────────────

    /**
     * The list is the widest directional surface in the kit — the bubble column, the
     * avatars and the receipts all mirror. Nothing in the repo checked either direction
     * before this.
     */
    @Test fun userConversation_rtl() = capture(rtl = true, content = list())
}
