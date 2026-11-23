package com.cometchat.uikit.kotlin.presentation.messagelist.ui

import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import com.cometchat.uikit.kotlin.R
import kotlinx.coroutines.channels.Channel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-41551 / D83 on the View message list: a read that fails with nothing shown shows the error
 * view (the app's own view when it set one), and a read that fails while rows stay on screen keeps
 * them and still reaches the app's onError. The offline reads raising either case are covered in
 * `MessageListOfflineReadsTest`; here the list's rendering of them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MessageListOfflineErrorViewTest {

    private lateinit var cometChatMock: MockedStatic<CometChat>
    private val offline = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")

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

    private fun repository(page: Result<List<BaseMessage>>): MessageListRepository = mock {
        onBlocking { fetchPreviousMessages() }.thenReturn(page)
        on { hasMorePreviousMessages() }.thenReturn(true)
        onBlocking { markAsRead(any()) }.thenReturn(Result.success(Unit))
        onBlocking { markAsDelivered(any()) }.thenReturn(Result.success(Unit))
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Suppress("UNCHECKED_CAST")
    private fun reportReadError(vm: CometChatMessageListViewModel, error: CometChatException) {
        val field = CometChatMessageListViewModel::class.java.getDeclaredField("offlineReadState")
        field.isAccessible = true
        val state = field.get(vm)
        (state.javaClass.getMethod("getReadErrorQueue").invoke(state) as Channel<CometChatException>).trySend(error)
    }

    private fun withList(
        repository: MessageListRepository,
        setUp: (CometChatMessageList) -> Unit,
        block: (CometChatMessageList, CometChatMessageListViewModel) -> Unit
    ) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val viewModel = CometChatMessageListViewModel(repository = repository, enableListeners = false)
            val list = CometChatMessageList(activity)
            activity.setContentView(list)
            setUp(list)
            list.setViewModel(viewModel)
            list.setUser(user())
            idle()
            block(list, viewModel)
        }
        scenario.close()
    }

    @Test
    fun `nothing shown - the app's error view wins and onError fires`() {
        val errors = mutableListOf<Throwable>()
        withList(
            repository(Result.failure(offline)),
            setUp = { list ->
                list.setErrorStateView(TextView(list.context).apply { text = "app error view" })
                list.setOnError { errors += it }
            }
        ) { list, _ ->
            val container = list.findViewById<View>(R.id.custom_view_container)
            assertEquals(View.VISIBLE, container.visibility)
            assertEquals(View.GONE, list.findViewById<View>(R.id.error_state_view).visibility)
            assertEquals(View.GONE, list.findViewById<View>(R.id.message_list_layout).visibility)
            assertEquals(listOf<Throwable>(offline), errors)
        }
    }

    @Test
    fun `nothing shown and no app view - the kit's error view shows`() {
        withList(repository(Result.failure(offline)), setUp = {}) { list, _ ->
            assertEquals(View.VISIBLE, list.findViewById<View>(R.id.error_state_view).visibility)
            assertEquals(View.GONE, list.findViewById<View>(R.id.message_list_layout).visibility)
            // Offline off: today's error text, never the offline state (D84 rule 11).
            assertEquals(View.VISIBLE, list.findViewById<View>(R.id.tv_error_subtitle).visibility)
        }
    }

    @Test
    fun `D84 - offline on, nothing shown and no app view - the kit's offline state, onError once`() {
        cometChatMock.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(true)
        val errors = mutableListOf<Throwable>()
        withList(repository(Result.failure(offline)), setUp = { list -> list.setOnError { errors += it } }) { list, _ ->
            assertEquals(View.VISIBLE, list.findViewById<View>(R.id.error_state_view).visibility)
            assertEquals(
                list.context.getString(com.cometchat.uikit.core.R.string.cometchat_connect_to_load_messages),
                list.findViewById<TextView>(R.id.tv_error_title).text.toString()
            )
            assertEquals(View.GONE, list.findViewById<View>(R.id.tv_error_subtitle).visibility)
            assertEquals(listOf<Throwable>(offline), errors)
        }
    }

    @Test
    fun `D84 - offline on, an online error with nothing shown is today's error view`() {
        cometChatMock.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(true)
        withList(repository(Result.failure(CometChatException("ERR_SERVER", "boom"))), setUp = {}) { list, _ ->
            assertEquals(View.VISIBLE, list.findViewById<View>(R.id.error_state_view).visibility)
            assertEquals(View.VISIBLE, list.findViewById<View>(R.id.tv_error_subtitle).visibility)
        }
    }

    @Test
    fun `rows shown - a failed read keeps them, no error view, and onError still fires`() {
        val errors = mutableListOf<Throwable>()
        withList(
            repository(Result.success(listOf(message(1L), message(2L)))),
            setUp = { list -> list.setOnError { errors += it } }
        ) { list, vm ->
            reportReadError(vm, offline)
            idle()

            assertEquals(View.VISIBLE, list.findViewById<View>(R.id.message_list_layout).visibility)
            assertEquals(View.GONE, list.findViewById<View>(R.id.error_state_view).visibility)
            assertEquals(listOf<Throwable>(offline), errors)
        }
    }

    @Test
    fun `an error raised before the list binds reaches onError once, and a re-bind does not repeat it`() {
        val errors = mutableListOf<Throwable>()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val vm = CometChatMessageListViewModel(
                repository = repository(Result.success(listOf(message(1L)))),
                enableListeners = false
            )
            reportReadError(vm, offline) // the open failed before any view collected

            val list = CometChatMessageList(activity)
            activity.setContentView(list)
            list.setOnError { errors += it }
            list.setViewModel(vm)
            list.setUser(user())
            idle()
            assertEquals(listOf<Throwable>(offline), errors)

            list.setViewModel(vm) // re-bind: a new collector
            idle()
            assertEquals(listOf<Throwable>(offline), errors)
        }
        scenario.close()
    }
}
