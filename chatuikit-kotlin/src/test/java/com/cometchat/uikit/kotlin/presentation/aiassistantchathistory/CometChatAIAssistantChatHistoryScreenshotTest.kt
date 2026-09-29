package com.cometchat.uikit.kotlin.presentation.aiassistantchathistory

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatAIAssistantChatHistoryViewModel
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.aiassistantchathistory.ui.CometChatAIAssistantChatHistory
import com.cometchat.uikit.kotlin.presentation.utils.RoborazziConfig
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Assert.assertEquals
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
import org.robolectric.shadows.ShadowLooper

/**
 * Snapshot layer for the **View** [CometChatAIAssistantChatHistory].
 *
 * The suite this replaces hand-assembled the header, the list rows and both state
 * layouts out of raw views and never constructed the component, so its fifteen
 * baselines were pictures of a stand-in. These capture the real view in each of its
 * four states.
 *
 * Getting there takes two seams. The view resolves its ViewModel through
 * `ViewModelProvider(owner, factory)`, which returns whatever is already cached under
 * that key — so seeding the Activity's own store first hands the component a ViewModel
 * this test holds a reference to. And that ViewModel's `uiState` is backed by a private
 * field, so the only way to move it is `fetchMessages()`, which calls `fetchPrevious`
 * on a `MessagesRequest` the ViewModel builds internally; intercepting the construction
 * of that SDK object decides what comes back.
 *
 * The Compose twin does the same, with the ViewModel passed in as a parameter instead
 * of seeded.
 *
 * Run:
 *   ./gradlew :chatuikit-kotlin:recordRoborazziDebug  --tests "*.CometChatAIAssistantChatHistoryScreenshotTest"
 *   ./gradlew :chatuikit-kotlin:compareRoborazziDebug --tests "*.CometChatAIAssistantChatHistoryScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAIAssistantChatHistoryScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/aiassistantchathistory"
        )
    )

    private companion object {
        const val CANVAS_LIGHT = 0xFFEEEEEE.toInt()
        const val CANVAS_DARK = 0xFF121212.toInt()

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
     * Ids have to be passed in rather than assigned: MockFactory returns a Mockito mock
     * whose `id` is stubbed. `sentAt` is pinned for the same reason as the Compose twin
     * — the factory defaults it to the current time and the list draws a date from it.
     */
    private fun history(count: Int): List<BaseMessage> =
        (1..count).map { i ->
            MockFactory.createTextMessage(id = i.toLong(), text = "Chat $i", sentAt = FIXED_SENT_AT)
        }

    /**
     * Intercepts the SDK request the ViewModel builds and decides what `fetchPrevious`
     * does. One page, then the end of the list — the ViewModel paginates, and a stub
     * that keeps returning the same page makes it append the same rows twice. Both
     * nulls leave the callback un-resolved, which is how loading is reached.
     */
    private fun answerWith(messages: List<BaseMessage>? = null, failure: CometChatException? = null) {
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

    /** Which state layout is on screen, by visible id. */
    private fun CometChatAIAssistantChatHistory.visibleState(): String = when {
        findViewById<View>(R.id.rv_chat_history)?.visibility == View.VISIBLE -> "content"
        findViewById<View>(R.id.empty_state_view)?.visibility == View.VISIBLE -> "empty"
        findViewById<View>(R.id.error_state_view)?.visibility == View.VISIBLE -> "error"
        findViewById<View>(R.id.shimmer_parent_layout)?.visibility == View.VISIBLE -> "loading"
        else -> "none"
    }

    private fun capture(expect: String, rtl: Boolean = false) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)

            // Seed the Activity's ViewModel store before the view looks: ViewModelProvider
            // hands back the cached instance for the key regardless of the factory the
            // component passes, so this is the one it will observe.
            val viewModel = CometChatAIAssistantChatHistoryViewModel(enableListeners = false)
            ViewModelProvider(
                activity,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModel as T
                },
            )[CometChatAIAssistantChatHistoryViewModel::class.java]

            viewModel.setUser(MockFactory.createUser(uid = "assistant", name = "Assistant"))
            ShadowLooper.idleMainLooper()

            val history = CometChatAIAssistantChatHistory(activity)
            val isNight = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val container = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(if (isNight) CANVAS_DARK else CANVAS_LIGHT)
                addView(
                    history,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
            activity.setContentView(container)
            // The root has to be attached before the direction is set; the ldrtl
            // qualifier does nothing here. See RtlLayoutDirectionHarnessTest.
            if (rtl) container.layoutDirection = View.LAYOUT_DIRECTION_RTL
            ShadowLooper.idleMainLooper()

            val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.EXACTLY)
            repeat(2) {
                container.measure(widthSpec, heightSpec)
                container.layout(0, 0, 1080, 2160)
                ShadowLooper.idleMainLooper()
            }

            // A size check alone passes while a list renders empty, so the guard is about
            // which state is on screen — and, for content, that it has rows.
            assertTrue("the view should have been laid out", history.width > 0 && history.height > 0)
            assertEquals("wrong state on screen", expect, history.visibleState())
            if (expect == "content") {
                val rows = history.findViewById<ViewGroup>(R.id.rv_chat_history)
                assertTrue("the list should have bound its rows", rows.childCount > 0)
            }

            container.captureRoboImage(roborazziOptions = RoborazziConfig.options())
        }
        scenario.close()
    }

    // ── content ─────────────────────────────────────────────────────────────

    @Test
    fun content() {
        answerWith(messages = history(6))
        capture("content")
    }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test
    fun content_dark() {
        answerWith(messages = history(6))
        capture("content")
    }

    @Test
    fun content_singleChat() {
        answerWith(messages = history(1))
        capture("content")
    }

    // ── empty ───────────────────────────────────────────────────────────────

    @Test
    fun empty() {
        answerWith(messages = emptyList())
        capture("empty")
    }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test
    fun empty_dark() {
        answerWith(messages = emptyList())
        capture("empty")
    }

    // ── error ───────────────────────────────────────────────────────────────

    @Test
    fun error() {
        answerWith(failure = CometChatException("ERR_FETCH", "Could not load your chats", "history"))
        capture("error")
    }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test
    fun error_dark() {
        answerWith(failure = CometChatException("ERR_FETCH", "Could not load your chats", "history"))
        capture("error")
    }

    // ── loading ─────────────────────────────────────────────────────────────

    /** The request never calls back, so the shimmer stays up. */
    @Test
    fun loading() {
        answerWith()
        capture("loading")
    }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test
    fun loading_dark() {
        answerWith()
        capture("loading")
    }

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test
    fun content_rtl() {
        answerWith(messages = history(6))
        capture("content", rtl = true)
    }
}
