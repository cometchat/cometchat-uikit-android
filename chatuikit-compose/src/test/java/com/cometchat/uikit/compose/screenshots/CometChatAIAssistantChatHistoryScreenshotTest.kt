package com.cometchat.uikit.compose.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.aiassistantchathistory.ui.CometChatAIAssistantChatHistory
import com.cometchat.uikit.compose.presentation.utils.RoborazziConfig
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.darkColorScheme
import com.cometchat.uikit.compose.theme.lightColorScheme
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatAIAssistantChatHistoryViewModel
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Snapshot layer for the Compose [CometChatAIAssistantChatHistory].
 *
 * The suite this replaces built the whole history list out of raw Compose primitives
 * and never called the component, so its eighteen baselines were pictures of a
 * stand-in. These capture the real thing in each of its four states.
 *
 * Reaching those states needs a seam the component does not advertise. The composable
 * takes an injectable `viewModel`, which is half of it, but the ViewModel's `uiState`
 * is backed by a private field and is not open, so a subclass cannot drive it either.
 * The only route in is `fetchMessages()`, which calls `fetchPrevious` on a
 * `MessagesRequest` the ViewModel builds internally — so the construction of that SDK
 * object is what gets intercepted here. Resolve with messages for content, with none
 * for empty, fail for error, and never resolve for loading.
 *
 * Run:
 *   ./gradlew :chatuikit-compose:recordRoborazziDebug  --tests "*.CometChatAIAssistantChatHistoryScreenshotTest"
 *   ./gradlew :chatuikit-compose:compareRoborazziDebug --tests "*.CometChatAIAssistantChatHistoryScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAIAssistantChatHistoryScreenshotTest {

    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule(order = 1)
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/aiassistantchathistory"
        )
    )

    private companion object {
        /** A fixed instant, so the list's date header does not drift with the clock. */
        const val FIXED_SENT_AT = 1_700_000_000L
    }

    private lateinit var cometChat: MockedStatic<CometChat>
    private var requests: MockedConstruction<MessagesRequest>? = null

    @Before
    fun stubLoggedInUser() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After
    fun closeMocks() {
        requests?.close()
        cometChat.close()
    }

    /**
     * Two fixture details the list depends on. Ids have to be distinct, because the
     * LazyColumn keys its rows on `message.id` and throws on a repeat — and they have to
     * be passed in, since MockFactory returns a Mockito mock whose `id` is stubbed, so
     * assigning it afterwards is silently dropped. And `sentAt` has to be pinned:
     * the factory defaults it to the current time, and the list draws a date header from
     * it, which would put today's date in the baseline.
     */
    private fun history(count: Int): List<BaseMessage> =
        (1..count).map { i ->
            MockFactory.createTextMessage(
                id = i.toLong(),
                text = "Chat $i",
                sentAt = FIXED_SENT_AT,
            )
        }

    /**
     * Intercepts the SDK request the ViewModel builds, and decides what its
     * `fetchPrevious` does. Both nulls leave the callback un-resolved, which is how the
     * loading state is reached.
     */
    private fun answerWith(messages: List<BaseMessage>? = null, failure: CometChatException? = null) {
        // One page, then the end of the list. The ViewModel paginates, and a stub that
        // keeps handing back the same page makes it append the same messages twice —
        // which the list then rejects, since it keys rows on the message id.
        var served = false
        requests = Mockito.mockConstruction(MessagesRequest::class.java) { request, _ ->
            Mockito.doAnswer { invocation ->
                @Suppress("UNCHECKED_CAST")
                val listener = invocation.arguments[0]
                    as CometChat.CallbackListener<List<BaseMessage>>
                when {
                    messages != null && !served -> {
                        served = true
                        listener.onSuccess(messages)
                    }
                    messages != null -> listener.onSuccess(emptyList())
                    failure != null -> listener.onError(failure)
                    else -> Unit // still in flight
                }
                Unit
            }.`when`(request).fetchPrevious(Mockito.any())
        }
    }

    private fun capture(
        dark: Boolean = false,
        emptyStateView: (@Composable () -> Unit)? = null,
        errorStateView: (@Composable () -> Unit)? = null,
    ) {
        // The ViewModel has to be built while the construction mock is active: setUser
        // builds the SDK request and kicks off the first fetch in one go.
        val viewModel = CometChatAIAssistantChatHistoryViewModel(enableListeners = false)
        viewModel.setUser(MockFactory.createUser(uid = "assistant", name = "Assistant"))

        composeRule.setContent {
            CometChatTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                CometChatAIAssistantChatHistory(
                    viewModel = viewModel,
                    onCloseClick = {},
                    onNewChatClick = {},
                    emptyStateView = emptyStateView,
                    errorStateView = errorStateView,
                )
            }
        }
        composeRule.waitForIdle()

        val decor = composeRule.activity.window.decorView
        // A screenshot test that photographs an empty frame still passes, so make that
        // impossible: the capture must have a size before anything is written.
        assertTrue("the history should have been laid out", decor.width > 0 && decor.height > 0)
        decor.captureRoboImage(roborazziOptions = RoborazziConfig.options())
    }

    // ── content ─────────────────────────────────────────────────────────────

    @Test
    fun content() {
        answerWith(messages = history(6))
        capture()
    }

    @Test
    fun content_dark() {
        answerWith(messages = history(6))
        capture(dark = true)
    }

    @Test
    fun content_singleChat() {
        answerWith(messages = history(1))
        capture()
    }

    // ── empty ───────────────────────────────────────────────────────────────

    @Test
    fun empty() {
        answerWith(messages = emptyList())
        capture()
    }

    @Test
    fun empty_dark() {
        answerWith(messages = emptyList())
        capture(dark = true)
    }

    @Test
    fun empty_customSlot() {
        answerWith(messages = emptyList())
        capture(emptyStateView = { Text("No conversations yet") })
    }

    // ── error ───────────────────────────────────────────────────────────────

    @Test
    fun error() {
        answerWith(failure = CometChatException("ERR_FETCH", "Could not load your chats", "history"))
        capture()
    }

    @Test
    fun error_dark() {
        answerWith(failure = CometChatException("ERR_FETCH", "Could not load your chats", "history"))
        capture(dark = true)
    }

    @Test
    fun error_customSlot() {
        answerWith(failure = CometChatException("ERR_FETCH", "Could not load your chats", "history"))
        capture(errorStateView = { Text("Something went wrong") })
    }

    // ── loading ─────────────────────────────────────────────────────────────

    /** The request never calls back, so the loading state stays up. */
    @Test
    fun loading() {
        answerWith()
        capture()
    }

    @Test
    fun loading_dark() {
        answerWith()
        capture(dark = true)
    }
}
