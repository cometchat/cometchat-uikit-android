package com.cometchat.uikit.kotlin.presentation.search

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.domain.repository.SearchRepository
import com.cometchat.uikit.core.domain.repository.SearchTwoPhaseRepository
import com.cometchat.uikit.core.domain.usecase.FetchConversationsUseCase
import com.cometchat.uikit.core.domain.usecase.FetchMessagesUseCase
import com.cometchat.uikit.core.offline.TwoPhaseResult
import com.cometchat.uikit.core.state.SearchUIState
import com.cometchat.uikit.core.viewmodel.CometChatSearchViewModel
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.search.ui.CometChatSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-41549 (Views twin of `SearchOfflineMissComposeTest`): offline, a name searched, no saved
 * match. The real ViewModel decides "Connect to search" (D84 rule 7) and the view shows it with
 * "Search needs an internet connection.", never the error state, and `onError` does not fire.
 *
 * Run: ./gradlew :chatuikit-kotlin:testDebugUnitTest --tests "*SearchOfflineMissViewTest"
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SearchOfflineMissViewTest {

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
    }

    @Test
    fun offlineNameSearch_noSavedMatch_isConnectToSearch_withSubtitle_noOnError() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val view = CometChatSearch(activity)
            val container = FrameLayout(activity)
            container.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            activity.setContentView(container)
            idle()

            val repo = OfflineRepository(notConnected)
            val vm = CometChatSearchViewModel(FetchConversationsUseCase(repo), FetchMessagesUseCase(repo))
            val errors = mutableListOf<CometChatException>()
            view.setOnError { errors += it }
            view.setViewModel(vm)
            idle()

            vm.searchConversationsAndMessages("alice", emptySet())
            idle()
            container.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.EXACTLY),
            )
            container.layout(0, 0, 1080, 2160)
            idle()

            assertEquals(SearchUIState.Empty, vm.uiState.value)
            assertTrue(vm.showConnectToSearch.value)
            val title = view.findViewById<TextView>(R.id.tv_empty_state_title)
            val subtitle = view.findViewById<TextView>(R.id.tv_empty_state_subtitle)
            assertTrue("empty state shown", title.isShown)
            assertEquals(activity.getString(com.cometchat.uikit.core.R.string.cometchat_connect_to_search_messages), title.text.toString())
            assertEquals(
                activity.getString(com.cometchat.uikit.core.R.string.cometchat_connect_to_search_messages_subtitle),
                subtitle.text.toString()
            )
            assertFalse("no error state", view.findViewById<View>(R.id.error_state_view).isShown)
            assertEquals(emptyList<CometChatException>(), errors)
        }
        scenario.close()
    }
}
