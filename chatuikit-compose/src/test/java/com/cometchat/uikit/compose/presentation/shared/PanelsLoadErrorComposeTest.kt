package com.cometchat.uikit.compose.presentation.shared

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
import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Reaction
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.notificationfeed.ui.CometChatNotificationFeed
import com.cometchat.uikit.compose.presentation.reactionlist.ui.CometChatReactionList
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.domain.usecase.FetchReactionsUseCase
import com.cometchat.uikit.core.domain.usecase.RemoveReactionUseCase
import com.cometchat.uikit.core.state.NotificationFeedUIState
import com.cometchat.uikit.core.state.ReactionListUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatNotificationFeedViewModel
import com.cometchat.uikit.core.viewmodel.CometChatReactionListViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

/**
 * ENG-41551 D83 — the Compose reactions list and notification feed: the app's error / empty view
 * wins over the kit's, and a load error that keeps cached rows still reaches `onError` without
 * covering them. The ViewModels' flows are driven directly (no live SDK).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class PanelsLoadErrorComposeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var cometChat: MockedStatic<CometChat>
    private val notConnected = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")

    @Before
    fun setUp() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After
    fun tearDown() = cometChat.close()

    private class OnePageFetch(private val rows: List<Reaction>) : FetchReactionsUseCase(mock()) {
        private var served = false
        override suspend fun invoke(request: ReactionsRequest): Result<List<Reaction>> {
            if (served) return Result.success(emptyList())
            served = true
            return Result.success(rows)
        }
    }

    private class NoRemove : RemoveReactionUseCase(mock()) {
        override suspend fun invoke(messageId: Long, emoji: String): Result<BaseMessage> =
            Result.failure(IllegalStateException("not exercised"))
    }

    private fun textCount(t: String) = rule.onAllNodesWithText(t).fetchSemanticsNodes().size

    @Suppress("UNCHECKED_CAST")
    private fun <T> field(target: Any, name: String): T =
        target.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(target) as T

    @Test
    fun reactionList_errorWithRows_keepsRows_andFiresOnError_thenAppErrorViewWinsWhenNothingShown() {
        val vm = CometChatReactionListViewModel(
            OnePageFetch(listOf(MockFactory.createReaction(uid = "u1", name = "Alice", emoji = "👍"))),
            NoRemove(),
            enableListeners = false
        )
        val errors = mutableListOf<CometChatException>()
        rule.setContent {
            CometChatTheme {
                CometChatReactionList(
                    baseMessage = MockFactory.createTextMessage(text = "hello"),
                    viewModel = vm,
                    errorView = { Text("APP_ERROR") },
                    onError = { errors += it },
                )
            }
        }
        rule.waitForIdle()
        assertTrue(textCount("Alice") > 0)

        field<Channel<CometChatException>>(vm, "_loadErrorWithRows").trySend(notConnected)
        rule.waitForIdle()
        assertEquals(listOf(notConnected), errors)
        assertTrue("cached rows stay", textCount("Alice") > 0)
        assertEquals("no error view over rows", 0, textCount("APP_ERROR"))

        field<MutableStateFlow<List<Reaction>>>(vm, "_reactedUsers").value = emptyList()
        field<MutableStateFlow<ReactionListUIState>>(vm, "_uiState").value = ReactionListUIState.Error(notConnected)
        rule.waitForIdle()
        assertEquals("the app's error view wins", 1, textCount("APP_ERROR"))
        assertEquals(2, errors.size)
    }

    @Test
    fun reactionList_disposed_cancelsTheReconnectRead() {
        val vm = CometChatReactionListViewModel(OnePageFetch(emptyList()), NoRemove(), enableListeners = false)
        var shown by mutableStateOf(true)
        rule.setContent {
            CometChatTheme {
                if (shown) CometChatReactionList(baseMessage = MockFactory.createTextMessage(text = "hello"), viewModel = vm)
            }
        }
        rule.waitForIdle()
        val refill = field<Any>(vm, "offlineRefill")
        refill.javaClass.getMethod("arm").invoke(refill)
        assertEquals(true, refill.javaClass.getMethod("isArmed").invoke(refill))

        shown = false // the sheet closed
        rule.waitForIdle()
        assertEquals("a dismissed list never reads on reconnect", false, refill.javaClass.getMethod("isArmed").invoke(refill))
    }

    @Test
    fun notificationFeed_appViewsWin_andErrorWithRowsFiresOnError() {
        val vm = CometChatNotificationFeedViewModel(enableListeners = false)
        val errors = mutableListOf<CometChatException>()
        rule.setContent {
            CometChatTheme {
                CometChatNotificationFeed(
                    viewModel = vm,
                    emptyStateView = { Text("APP_EMPTY") },
                    errorStateView = { _, _ -> Text("APP_ERROR") },
                    onError = { errors += it },
                )
            }
        }
        rule.waitForIdle()
        val state = field<MutableStateFlow<NotificationFeedUIState>>(vm, "_uiState")

        state.value = NotificationFeedUIState.Error(notConnected)
        rule.waitForIdle()
        assertEquals("the app's error view wins", 1, textCount("APP_ERROR"))
        assertTrue(errors.contains(notConnected))

        state.value = NotificationFeedUIState.Empty
        rule.waitForIdle()
        assertEquals("the app's empty view wins", 1, textCount("APP_EMPTY"))

        errors.clear()
        state.value = NotificationFeedUIState.Content(emptyList())
        rule.waitForIdle()
        field<Channel<CometChatException>>(vm, "_loadErrorWithRows").trySend(notConnected)
        rule.waitForIdle()
        assertEquals(listOf(notConnected), errors)
        assertEquals("no error view over rows", 0, textCount("APP_ERROR"))
    }

    // ---- D84 rule 3: offline with nothing shown and no app error view is the kit's offline state.

    private fun str(res: Int) = rule.activity.getString(res)

    @Test
    fun d84_reactionList_offlineState_whenNothingShown() {
        val vm = CometChatReactionListViewModel(OnePageFetch(emptyList()), NoRemove(), enableListeners = false)
        val errors = mutableListOf<CometChatException>()
        rule.setContent {
            CometChatTheme {
                CometChatReactionList(
                    baseMessage = MockFactory.createTextMessage(text = "hello"),
                    viewModel = vm,
                    onError = { errors += it },
                )
            }
        }
        rule.waitForIdle()
        field<MutableStateFlow<Boolean>>(vm, "_connectToLoad").value = true
        field<MutableStateFlow<ReactionListUIState>>(vm, "_uiState").value = ReactionListUIState.Error(notConnected)
        rule.waitForIdle()
        assertEquals(1, textCount(str(com.cometchat.uikit.core.R.string.cometchat_connect_to_load_reactions)))
        assertEquals(listOf(notConnected), errors)
    }

    @Test
    fun d84_notificationFeed_offlineState_whenNothingShown_noRetry() {
        val vm = CometChatNotificationFeedViewModel(enableListeners = false)
        rule.setContent { CometChatTheme { CometChatNotificationFeed(viewModel = vm) } }
        rule.waitForIdle()
        field<MutableStateFlow<Boolean>>(vm, "_connectToLoad").value = true
        field<MutableStateFlow<NotificationFeedUIState>>(vm, "_uiState").value = NotificationFeedUIState.Error(notConnected)
        rule.waitForIdle()
        assertEquals(1, textCount(str(com.cometchat.uikit.core.R.string.cometchat_connect_to_load_notifications)))
        assertEquals("no Retry", 0, textCount("Retry"))
    }
}
