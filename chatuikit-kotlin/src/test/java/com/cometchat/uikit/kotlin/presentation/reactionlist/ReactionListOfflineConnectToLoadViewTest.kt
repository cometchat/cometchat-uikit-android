package com.cometchat.uikit.kotlin.presentation.reactionlist

import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Reaction
import com.cometchat.chat.models.ReactionCount
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.ReactionListRepository
import com.cometchat.uikit.core.domain.repository.ReactionListTwoPhaseRepository
import com.cometchat.uikit.core.domain.usecase.FetchReactionsUseCase
import com.cometchat.uikit.core.domain.usecase.RemoveReactionUseCase
import com.cometchat.uikit.core.offline.TwoPhaseResult
import com.cometchat.uikit.core.viewmodel.CometChatReactionListViewModel
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.reactionlist.ui.CometChatReactionList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-41640 (B): the View reactions sheet, opened for a message whose reactors were never loaded,
 * shows its error text below the reaction tabs, as the Compose sheet does: offline it is
 * "Connect to load reactions" (D84 rule 3), online it is the normal error text. Before the fix the
 * error state sat below a full-height sibling and was laid out with no height, so the sheet showed
 * the tabs and nothing else.
 *
 * Run: ./gradlew :chatuikit-kotlin:testDebugUnitTest --tests "*ReactionListOfflineConnectToLoadViewTest"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReactionListOfflineConnectToLoadViewTest {

    private lateinit var cometChatMock: MockedStatic<CometChat>
    private val notConnected = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")
    private val serverError = CometChatException("ERR_SERVER", "boom")

    @Before
    fun setUp() {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        cometChatMock.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(User("me", "Me"))
    }

    @After
    fun tearDown() {
        cometChatMock.close()
    }

    /** Every read fails with [error]: the two-phase read (offline on) and today's read (offline off). */
    private class FailingRepository(private val error: CometChatException) :
        ReactionListRepository, ReactionListTwoPhaseRepository {
        override suspend fun fetchReactions(request: ReactionsRequest): Result<List<Reaction>> =
            Result.failure(error)

        override suspend fun removeReaction(messageId: Long, emoji: String): Result<BaseMessage> =
            Result.failure(error)

        override fun fetchReactionsTwoPhase(request: ReactionsRequest): Flow<TwoPhaseResult<Reaction>> =
            flowOf(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(error))
    }

    private fun message(): BaseMessage =
        TextMessage("peer", "hi", CometChatConstants.RECEIVER_TYPE_USER).apply {
            id = 1L
            sender = User("peer", "Peer")
            reactions = listOf(ReactionCount().apply {
                reaction = "👍"
                count = 1
            })
        }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    /** Opens the sheet the way the message list does, at a fixed sheet size, and lays it out. */
    private fun withSheet(error: CometChatException, block: (CometChatReactionList) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val repo = FailingRepository(error)
            val vm = CometChatReactionListViewModel(
                FetchReactionsUseCase(repo),
                RemoveReactionUseCase(repo),
                enableListeners = false
            )
            val list = CometChatReactionList(activity)
            activity.setContentView(list)
            list.setViewModel(vm)
            list.setSelectedReaction("All")
            list.setBaseMessage(message())
            idle()
            list.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY)
            )
            list.layout(0, 0, 1080, 1200)
            block(list)
        }
        scenario.close()
    }

    private fun assertErrorTextShown(list: CometChatReactionList, expected: String) {
        val errorText = list.findViewById<TextView>(R.id.tv_error_state)
        assertEquals(expected, errorText.text.toString())
        assertTrue("error text is not shown", errorText.isShown)
        assertTrue("error text has no height", errorText.height > 0)
        // The tabs stay above it, as in the Compose sheet.
        assertEquals(View.VISIBLE, list.findViewById<View>(R.id.recyclerViewEmojis).visibility)
        assertEquals(View.GONE, list.findViewById<View>(R.id.recycler_view_users).visibility)
    }

    @Test
    fun `offline on - nothing loaded and the read fails not connected - Connect to load reactions`() {
        cometChatMock.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(true)
        withSheet(notConnected) { list ->
            assertErrorTextShown(
                list,
                list.context.getString(com.cometchat.uikit.core.R.string.cometchat_connect_to_load_reactions)
            )
        }
    }

    @Test
    fun `offline off - the read fails - the normal error text`() {
        withSheet(serverError) { list ->
            assertErrorTextShown(list, list.context.getString(R.string.cometchat_reaction_list_error))
        }
    }

    @Test
    fun `offline on - an online failure with nothing loaded - the normal error text`() {
        cometChatMock.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(true)
        withSheet(serverError) { list ->
            assertErrorTextShown(list, list.context.getString(R.string.cometchat_reaction_list_error))
        }
    }
}
