package com.cometchat.uikit.compose.presentation.search

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.compose.presentation.search.ui.CometChatSearch
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.domain.repository.SearchRepository
import com.cometchat.uikit.core.domain.repository.SearchTwoPhaseRepository
import com.cometchat.uikit.core.domain.usecase.FetchConversationsUseCase
import com.cometchat.uikit.core.domain.usecase.FetchMessagesUseCase
import com.cometchat.uikit.core.offline.TwoPhaseResult
import com.cometchat.uikit.core.state.SearchUIState
import com.cometchat.uikit.core.viewmodel.CometChatSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-41549, the QA path: offline, the search screen opened from the chat list, a name typed.
 * Both reads end not-connected with no saved match. The screen shows "Connect to search" with
 * "Search needs an internet connection." (D84 rule 7), never the error state, and `onError` does
 * not fire. Unlike the flag-driven D84 tests, this runs the real ViewModel decision end to end
 * over a scripted repository, so the two halves cannot drift apart.
 *
 * Run: ./gradlew :chatuikit-compose:testDebugUnitTest --tests "*SearchOfflineMissComposeTest"
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SearchOfflineMissComposeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = UnconfinedTestDispatcher()
    private val notConnected = CometChatException("ERROR_INTERNET_UNAVAILABLE", "offline")

    /** The kit's offline gate is internal to core; its test seam is set through its static setter. */
    private fun offlineGate(on: Boolean?) {
        Class.forName("com.cometchat.uikit.core.offline.OfflineGate")
            .getMethod("setTestOverride", java.lang.Boolean::class.java)
            .invoke(null, on)
    }

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        offlineGate(true)
    }

    @After fun tearDown() {
        offlineGate(null)
        Dispatchers.resetMain()
    }

    private class OfflineRepository(private val error: CometChatException) : SearchRepository, SearchTwoPhaseRepository {
        override fun getConversationsTwoPhase(request: ConversationsRequest): Flow<TwoPhaseResult<Conversation>> =
            flowOf(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(error))
        override fun getMessagesTwoPhase(request: MessagesRequest): Flow<TwoPhaseResult<BaseMessage>> =
            flowOf(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(error))
        override suspend fun getConversations(request: ConversationsRequest): Result<List<Conversation>> =
            Result.failure(error)
        override suspend fun getMessages(request: MessagesRequest): Result<List<BaseMessage>> = Result.failure(error)
        override fun hasMoreConversations(): Boolean = false
        override fun hasMoreMessages(): Boolean = false
    }

    private fun idle() {
        dispatcher.scheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()
        rule.waitForIdle()
    }

    private fun str(res: Int) = rule.activity.getString(res)

    @Test
    fun offlineNameSearch_noSavedMatch_isConnectToSearch_withSubtitle_noOnError() {
        val repo = OfflineRepository(notConnected)
        val vm = CometChatSearchViewModel(FetchConversationsUseCase(repo), FetchMessagesUseCase(repo))
        val errors = mutableListOf<CometChatException>()
        rule.setContent { CometChatTheme { CometChatSearch(viewModel = vm, onError = { errors += it }) } }
        idle()

        vm.searchConversationsAndMessages("alice", emptySet())
        idle()

        assertEquals(SearchUIState.Empty, vm.uiState.value)
        assertTrue(vm.showConnectToSearch.value)
        rule.onNodeWithText(str(com.cometchat.uikit.core.R.string.cometchat_connect_to_search_messages)).assertIsDisplayed()
        rule.onNodeWithText(str(com.cometchat.uikit.core.R.string.cometchat_connect_to_search_messages_subtitle)).assertIsDisplayed()
        rule.onNodeWithText(str(com.cometchat.uikit.compose.R.string.cometchat_something_went_wrong_please_try_again))
            .assertDoesNotExist()
        assertEquals(emptyList<CometChatException>(), errors)
    }
}
