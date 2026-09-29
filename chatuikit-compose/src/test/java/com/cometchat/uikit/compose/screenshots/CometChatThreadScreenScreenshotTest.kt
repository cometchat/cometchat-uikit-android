package com.cometchat.uikit.compose.screenshots

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.messagelist.ui.CometChatMessageList
import com.cometchat.uikit.compose.presentation.threadheader.ui.CometChatThreadScreen
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
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Snapshot layer for [CometChatThreadScreen], which had no coverage of any kind.
 *
 * The replies go through the screen's own `messageListView` slot. Its default list
 * fetches from the SDK, and an unstubbed fetch never clears "has more", so the list
 * re-fetches on every frame and the capture never reaches idle.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatThreadScreenScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/threadscreen"
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

    // sentAt is pinned: this factory otherwise defaults to the current clock, which
    // renders "Today" and a wall-clock time and makes the baseline drift daily.
    private fun parent(): BaseMessage = MockFactory.createTextMessage(
        id = 1L,
        text = "Can everyone review the deck before Thursday?",
        senderUid = "u1",
        receiverId = "me",
        sentAt = MockFactory.FIXED_SENT_AT,
    ).apply {
        // Resolve the count before stubbing: building mocks inside a thenReturn
        // leaves Mockito with unfinished stubbing.
        val count = replies().size
        whenever(replyCount).thenReturn(count)
    }

    private fun replies(): List<BaseMessage> = listOf(
        MockFactory.createTextMessage(id = 2L, text = "On it.", senderUid = "me", receiverId = "u1", sentAt = MockFactory.FIXED_SENT_AT),
        MockFactory.createTextMessage(id = 3L, text = "Slide 4 needs the new numbers.", senderUid = "u1", receiverId = "me", sentAt = MockFactory.FIXED_SENT_AT),
        MockFactory.createTextMessage(id = 4L, text = "Updated and pushed.", senderUid = "me", receiverId = "u1", sentAt = MockFactory.FIXED_SENT_AT),
    )

    private fun inertRepository(): MessageListRepository = mock {
        onBlocking { fetchPreviousMessages() } doReturn Result.success(emptyList())
        onBlocking { fetchNextMessages(any()) } doReturn Result.success(emptyList())
    }

    private fun seededViewModel(user: User): CometChatMessageListViewModel =
        CometChatMessageListViewModel(inertRepository(), enableListeners = false).apply {
            setUser(user)
            replies().forEach { addMessage(it) }
        }

    private fun capture(dark: Boolean = false) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContent {
                CometChatTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (dark) canvasDark else canvasLight)
                    ) { ThreadScreen() }
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

    @Composable
    private fun ThreadScreen() {
        val alice = remember { MockFactory.createUser(uid = "u1", name = "Alice") }
        CometChatThreadScreen(
            parentMessage = remember { parent() },
            user = alice,
            messageListView = { _, _, _ ->
                CometChatMessageList(
                    viewModel = remember { seededViewModel(alice) },
                    user = alice,
                    autoFetch = false,
                    hideLoadingState = true,
                    disableReceipt = true,
                )
            },
        )
    }

    @Test fun threadWithReplies() = capture()

    @Test fun threadWithReplies_dark() = capture(dark = true)
}
