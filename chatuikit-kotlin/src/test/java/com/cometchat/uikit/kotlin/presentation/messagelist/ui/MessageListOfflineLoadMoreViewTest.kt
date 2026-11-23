package com.cometchat.uikit.kotlin.presentation.messagelist.ui

import android.os.Looper
import android.view.View
import android.widget.AbsListView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import com.cometchat.uikit.kotlin.R
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-41460 (R7): while an edge shows "Connect to load more", a scroll to that edge starts neither
 * a page read nor the pagination spinner (before the fix the spinner stuck: the blocked read
 * returned without ever toggling `isInProgress`, which is what hides it).
 *
 * The edge flag is raised on the real ViewModel's read state by reflection; the scroll is driven
 * through the list's own scroll handlers.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MessageListOfflineLoadMoreViewTest {

    private lateinit var cometChatMock: MockedStatic<CometChat>

    @Before
    fun setUp() {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
    }

    @After
    fun tearDown() {
        cometChatMock.close()
    }

    private fun user(): User = User().apply {
        uid = "test-user"
        name = "Test User"
    }

    private fun message(id: Long): BaseMessage =
        TextMessage("test-user", "msg-$id", CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            sender = user()
            sentAt = 1_700_000_000L + id
        }

    private fun repository(): MessageListRepository = mock {
        onBlocking { fetchPreviousMessages() }.thenReturn(Result.success(listOf(message(1L), message(2L), message(3L))))
        on { hasMorePreviousMessages() }.thenReturn(true)
        onBlocking { markAsRead(any()) }.thenReturn(Result.success(Unit))
        onBlocking { markAsDelivered(any()) }.thenReturn(Result.success(Unit))
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Suppress("UNCHECKED_CAST")
    private fun raiseOlderEdge(vm: CometChatMessageListViewModel) {
        val field = CometChatMessageListViewModel::class.java.getDeclaredField("offlineReadState")
        field.isAccessible = true
        val state = field.get(vm)
        (state.javaClass.getMethod("getOlderBlocked").invoke(state) as MutableStateFlow<Boolean>).value = true
    }

    /** A touch scroll that reaches the top, through the list's own scroll handlers. */
    private fun scrollToTop(list: CometChatMessageList) {
        CometChatMessageList::class.java.getDeclaredMethod("handleScrollStateChange", Int::class.javaPrimitiveType).apply {
            isAccessible = true
            invoke(list, AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL)
        }
        CometChatMessageList::class.java.getDeclaredMethod("handleScroll").apply {
            isAccessible = true
            invoke(list)
        }
    }

    private fun withList(repository: MessageListRepository, block: (CometChatMessageList, CometChatMessageListViewModel) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val viewModel = CometChatMessageListViewModel(repository = repository, enableListeners = false)
            val list = CometChatMessageList(activity)
            activity.setContentView(list)
            list.setViewModel(viewModel)
            list.setUser(user())
            idle()
            block(list, viewModel)
        }
        scenario.close()
    }

    @Test
    fun `a blocked older edge starts no page read and no spinner`() {
        val repository = repository()
        withList(repository) { list, vm ->
            raiseOlderEdge(vm)
            idle()

            scrollToTop(list)

            assertEquals(View.GONE, list.findViewById<View>(R.id.top_pagination_indicator).visibility)
            idle()
            verifyBlocking(repository, times(1)) { fetchPreviousMessages() }
        }
    }

    @Test
    fun `control - the same scroll with the edge clear reads the older page`() {
        val repository = repository()
        withList(repository) { list, _ ->
            scrollToTop(list)
            idle()
            verifyBlocking(repository, times(2)) { fetchPreviousMessages() }
        }
    }
}
