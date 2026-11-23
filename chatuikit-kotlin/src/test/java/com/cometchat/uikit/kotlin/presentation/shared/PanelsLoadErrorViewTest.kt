package com.cometchat.uikit.kotlin.presentation.shared

import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.state.NotificationFeedUIState
import com.cometchat.uikit.core.state.ReactionListUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatNotificationFeedViewModel
import com.cometchat.uikit.core.viewmodel.CometChatReactionListViewModel
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.notificationfeed.ui.CometChatNotificationFeed
import com.cometchat.uikit.kotlin.presentation.reactionlist.ui.CometChatReactionList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-41551 D83 — the View reactions list and notification feed: the app's error view wins over
 * the kit's, and a load error that keeps cached rows still reaches `onError` without covering
 * them. The ViewModels' flows are driven directly (no live SDK).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PanelsLoadErrorViewTest {

    private lateinit var cometChat: MockedStatic<CometChat>
    private val notConnected = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After
    fun tearDown() {
        cometChat.close()
        Dispatchers.resetMain()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> field(target: Any, name: String): T =
        target.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(target) as T

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun reactionList_errorWithRows_firesOnError_andAppErrorViewWinsWhenNothingShown() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val view = CometChatReactionList(activity)
            val appError = TextView(activity).apply { text = "APP_ERROR" }
            val errors = mutableListOf<CometChatException>()
            view.setErrorView(appError)
            view.setOnError { errors += it }
            val vm = CometChatReactionListViewModel(mock(), mock(), enableListeners = false)
            view.setViewModel(vm)
            val state = field<MutableStateFlow<ReactionListUIState>>(vm, "_uiState")

            state.value = ReactionListUIState.Content
            idle()
            field<Channel<CometChatException>>(vm, "_loadErrorWithRows").trySend(notConnected)
            idle()
            assertEquals(listOf(notConnected), errors)
            assertEquals("no error view over rows", null, appError.parent)

            state.value = ReactionListUIState.Error(notConnected)
            idle()
            val errorLayout = appError.parent as View
            assertEquals("the app's error view wins", View.VISIBLE, errorLayout.visibility)
            assertEquals(2, errors.size)
        }
        scenario.close()
    }

    @Test
    fun reactionList_detachedFromWindow_cancelsTheReconnectRead() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val view = CometChatReactionList(activity)
            val vm = CometChatReactionListViewModel(mock(), mock(), enableListeners = false)
            view.setViewModel(vm)
            val container = android.widget.FrameLayout(activity)
            activity.setContentView(container)
            container.addView(view)
            idle()
            val refill = field<Any>(vm, "offlineRefill")
            refill.javaClass.getMethod("arm").invoke(refill)
            assertEquals(true, refill.javaClass.getMethod("isArmed").invoke(refill))

            container.removeView(view) // the sheet closed
            idle()
            assertEquals("a dismissed list never reads on reconnect", false, refill.javaClass.getMethod("isArmed").invoke(refill))
        }
        scenario.close()
    }

    @Test
    fun notificationFeed_errorWithRows_firesOnError_withoutTheErrorView() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val view = CometChatNotificationFeed(activity)
            val errors = mutableListOf<CometChatException>()
            view.setOnError { errors += it }
            val vm = CometChatNotificationFeedViewModel(enableListeners = false)
            CometChatNotificationFeed::class.java.getDeclaredField("viewModel").apply { isAccessible = true }.set(view, vm)
            CometChatNotificationFeed::class.java
                .getDeclaredMethod("observeViewModel", LifecycleOwner::class.java).apply { isAccessible = true }
                .invoke(view, activity)
            val state = field<MutableStateFlow<NotificationFeedUIState>>(vm, "_uiState")
            val errorView = field<View>(view, "errorView")

            state.value = NotificationFeedUIState.Content(emptyList())
            idle()
            field<Channel<CometChatException>>(vm, "_loadErrorWithRows").trySend(notConnected)
            idle()
            assertEquals(listOf(notConnected), errors)
            assertEquals("no error view over rows", View.GONE, errorView.visibility)

            state.value = NotificationFeedUIState.Error(notConnected)
            idle()
            assertEquals(View.VISIBLE, errorView.visibility)
            assertEquals(2, errors.size)
        }
        scenario.close()
    }

    // ---- D84 rule 3: offline with nothing shown is the kit's offline state (no Retry).

    @Test
    fun d84_reactionList_offlineState_whenNothingShown() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val view = CometChatReactionList(activity)
            val errors = mutableListOf<CometChatException>()
            view.setOnError { errors += it }
            val vm = CometChatReactionListViewModel(mock(), mock(), enableListeners = false)
            view.setViewModel(vm)
            field<MutableStateFlow<Boolean>>(vm, "_connectToLoad").value = true
            field<MutableStateFlow<ReactionListUIState>>(vm, "_uiState").value = ReactionListUIState.Error(notConnected)
            idle()
            val text = view.findViewById<TextView>(R.id.tv_error_state).text.toString()
            assertEquals(activity.getString(com.cometchat.uikit.core.R.string.cometchat_connect_to_load_reactions), text)
            assertEquals(listOf(notConnected), errors)
        }
        scenario.close()
    }

    @Test
    fun d84_notificationFeed_offlineState_whenNothingShown_noRetry() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val view = CometChatNotificationFeed(activity)
            val vm = CometChatNotificationFeedViewModel(enableListeners = false)
            CometChatNotificationFeed::class.java.getDeclaredField("viewModel").apply { isAccessible = true }.set(view, vm)
            CometChatNotificationFeed::class.java
                .getDeclaredMethod("observeViewModel", LifecycleOwner::class.java).apply { isAccessible = true }
                .invoke(view, activity)
            field<MutableStateFlow<Boolean>>(vm, "_connectToLoad").value = true
            field<MutableStateFlow<NotificationFeedUIState>>(vm, "_uiState").value = NotificationFeedUIState.Error(notConnected)
            idle()
            assertEquals(View.VISIBLE, field<View>(view, "errorView").visibility)
            assertEquals(
                activity.getString(com.cometchat.uikit.core.R.string.cometchat_connect_to_load_notifications),
                field<TextView>(view, "errorTitleView").text.toString()
            )
            assertEquals("no Retry", View.GONE, field<View>(view, "retryButton").visibility)
            assertEquals("no subtitle", View.GONE, field<View>(view, "errorSubtitleView").visibility)

            // A later online error is today's view again.
            field<MutableStateFlow<Boolean>>(vm, "_connectToLoad").value = false
            field<MutableStateFlow<NotificationFeedUIState>>(vm, "_uiState").value = NotificationFeedUIState.Loading
            idle()
            field<MutableStateFlow<NotificationFeedUIState>>(vm, "_uiState").value =
                NotificationFeedUIState.Error(CometChatException("ERR_SERVER", "boom"))
            idle()
            assertEquals("Oops!", field<TextView>(view, "errorTitleView").text.toString())
            assertEquals(View.VISIBLE, field<View>(view, "retryButton").visibility)
        }
        scenario.close()
    }
}
