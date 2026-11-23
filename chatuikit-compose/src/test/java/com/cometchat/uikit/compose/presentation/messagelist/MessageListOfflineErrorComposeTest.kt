package com.cometchat.uikit.compose.presentation.messagelist

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.messagelist.ui.CometChatMessageList
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.state.MessageListUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import kotlinx.coroutines.channels.Channel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

/**
 * ENG-41551 / D83 on the Compose message list: a read that fails with nothing shown shows the
 * error view (the app's `errorView` when passed), and a read that fails while rows stay on screen
 * keeps them and still reaches the app's `onError`. The offline reads raising either case are
 * covered in `MessageListOfflineReadsTest`; here the list's rendering of them.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class MessageListOfflineErrorComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var cometChat: MockedStatic<CometChat>
    private val offline = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")

    @Before
    fun mockStatic() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
        // These are offline-on scenarios: the list reads the offline flag once when it binds.
        cometChat.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(true)
    }

    @After
    fun closeStatic() = cometChat.close()

    // Sent by the logged-in user, so the list marks nothing read: a MessageRead event would reach
    // view models other test classes leave alive in this JVM.
    private fun message(id: Long): BaseMessage =
        TextMessage("u1", "msg-$id", CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            sender = MockFactory.createUser(uid = "me", name = "Me")
            sentAt = 1_700_000_000L + id
        }

    private fun viewModel(page: Result<List<BaseMessage>>): CometChatMessageListViewModel {
        val repository: MessageListRepository = mock {
            onBlocking { fetchPreviousMessages() }.thenReturn(page)
            on { hasMorePreviousMessages() }.thenReturn(true)
            onBlocking { markAsRead(any()) }.thenReturn(Result.success(Unit))
            onBlocking { markAsDelivered(any()) }.thenReturn(Result.success(Unit))
        }
        return CometChatMessageListViewModel(repository = repository, enableListeners = false)
    }

    @Suppress("UNCHECKED_CAST")
    private fun reportReadError(vm: CometChatMessageListViewModel, error: CometChatException) {
        val field = CometChatMessageListViewModel::class.java.getDeclaredField("offlineReadState")
        field.isAccessible = true
        val state = field.get(vm)
        (state.javaClass.getMethod("getReadErrorQueue").invoke(state) as Channel<CometChatException>).trySend(error)
    }

    private fun textCount(t: String) = composeRule.onAllNodesWithText(t).fetchSemanticsNodes().size

    @Test
    fun nothingShown_appErrorViewWins_andOnErrorFiresOnce_recompositionDoesNotRepeat() {
        val vm = viewModel(Result.failure(offline))
        val errors = mutableListOf<CometChatException>()
        var label by mutableStateOf("app error view")
        composeRule.setContent {
            CometChatTheme {
                CometChatMessageList(
                    viewModel = vm,
                    user = MockFactory.createUser(uid = "u1", name = "Alice"),
                    errorView = { Text(label) },
                    onError = { errors += it },
                )
            }
        }
        composeRule.waitForIdle()

        assertEquals(1, textCount("app error view"))
        assertEquals(listOf(offline), errors)

        label = "app error view, recomposed" // the error view recomposes; the error is the same
        composeRule.waitForIdle()
        assertEquals(1, textCount("app error view, recomposed"))
        assertEquals(listOf(offline), errors)
    }

    @Test
    fun rowsShown_failedReadKeepsThem_noErrorView_andOnErrorFires() {
        // Rows land through addItems (no fetch, so no mark-as-read event reaches other tests'
        // view models in this JVM); the list renders them as the cache phase would.
        val vm = viewModel(Result.success(emptyList()))
        vm.addItems(listOf(message(1L), message(2L)))
        val errors = mutableListOf<CometChatException>()
        composeRule.setContent {
            CometChatTheme {
                CometChatMessageList(
                    viewModel = vm,
                    user = MockFactory.createUser(uid = "u1", name = "Alice"),
                    autoFetch = false,
                    disableReceipt = true,
                    errorView = { Text("app error view") },
                    onError = { errors += it },
                )
            }
        }
        composeRule.waitForIdle()

        reportReadError(vm, offline)
        composeRule.waitForIdle()

        assertEquals(0, textCount("app error view"))
        assertTrue(vm.uiState.value is MessageListUIState.Loaded)
        assertEquals(2, vm.messages.value.size)
        assertEquals(listOf(offline), errors)
    }

    @Test
    fun errorRaisedBeforeComposition_reachesOnErrorOnce_recompositionDoesNotRepeat() {
        val vm = viewModel(Result.success(emptyList()))
        vm.addItems(listOf(message(1L)))
        reportReadError(vm, offline) // the open failed before the list was composed
        val errors = mutableListOf<CometChatException>()
        var hideLoading by mutableStateOf(false)
        composeRule.setContent {
            CometChatTheme {
                CometChatMessageList(
                    viewModel = vm,
                    user = MockFactory.createUser(uid = "u1", name = "Alice"),
                    autoFetch = false,
                    disableReceipt = true,
                    hideLoadingState = hideLoading,
                    onError = { errors += it },
                )
            }
        }
        composeRule.waitForIdle()
        assertEquals(listOf(offline), errors)

        hideLoading = true // recomposition with a new parameter
        composeRule.waitForIdle()
        assertEquals(listOf(offline), errors)
    }

    @Test
    fun d84_nothingShown_offline_noAppErrorView_isTheOfflineState_noRetry_onErrorOnce() {
        val vm = viewModel(Result.failure(offline))
        val errors = mutableListOf<CometChatException>()
        composeRule.setContent {
            CometChatTheme {
                CometChatMessageList(
                    viewModel = vm,
                    user = MockFactory.createUser(uid = "u1", name = "Alice"),
                    onError = { errors += it },
                )
            }
        }
        composeRule.waitForIdle()

        val title = composeRule.activity.getString(com.cometchat.uikit.core.R.string.cometchat_connect_to_load_messages)
        assertEquals(1, textCount(title))
        assertEquals("no Retry", 0, textCount(composeRule.activity.getString(com.cometchat.uikit.compose.R.string.cometchat_retry)))
        assertEquals(listOf(offline), errors)
    }

    @Test
    fun d84_nothingShown_onlineError_isTheErrorView_withRetry() {
        val boom = CometChatException("ERR_SERVER", "boom")
        val vm = viewModel(Result.failure(boom))
        composeRule.setContent {
            CometChatTheme {
                CometChatMessageList(viewModel = vm, user = MockFactory.createUser(uid = "u1", name = "Alice"))
            }
        }
        composeRule.waitForIdle()

        val title = composeRule.activity.getString(com.cometchat.uikit.core.R.string.cometchat_connect_to_load_messages)
        assertEquals(0, textCount(title))
        assertEquals(1, textCount(composeRule.activity.getString(com.cometchat.uikit.compose.R.string.cometchat_retry)))
    }
}
