package com.cometchat.uikit.core.offline

import android.util.Log
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.model.SurroundingMessagesResult
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.domain.repository.MessageListTwoPhaseRepository
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

/**
 * Invariant 1 for the reads (ENG-41087): with the gate off, the message list makes today's
 * repository calls (single-phase previous/next, getConversation, getMessage + surrounding) and never
 * a two-phase read, even when the repository offers one. The ON contrast test proves the same
 * ViewModel routes through the two-phase read when the gate is on.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineOffReadBaselineTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var logMock: MockedStatic<Log>
    val stores = mutableListOf<androidx.lifecycle.ViewModelStore>()

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }

    beforeTest {
        logMock = Mockito.mockStatic(Log::class.java)
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        cometChatMock.`when`<User> { CometChat.getLoggedInUser() }.thenReturn(User().apply { uid = "me" })
    }
    afterTest {
        stores.forEach { it.clear() }
        stores.clear()
        cometChatMock.close()
        logMock.close()
        OfflineGate.testOverride = null
    }

    fun msg(id: Long): BaseMessage = TextMessage("peer", "m$id", CometChatConstants.RECEIVER_TYPE_USER).apply {
        this.id = id
        this.sentAt = id
        this.sender = User().apply { uid = "peer" }
    }

    fun repository(): MessageListRepository =
        mock<MessageListRepository>(extraInterfaces = arrayOf(MessageListTwoPhaseRepository::class)).apply {
            stub {
                on { hasMorePreviousMessages() } doReturn true
                onBlocking { fetchPreviousMessages() } doReturn Result.success(listOf(msg(1), msg(2)))
                onBlocking { fetchNextMessages(any()) } doReturn Result.success(emptyList())
                onBlocking { getConversation(any(), any()) } doReturn
                    Result.success(Conversation("peer_me", CometChatConstants.CONVERSATION_TYPE_USER))
                onBlocking { getMessage(any()) } doReturn Result.success(msg(2))
                onBlocking { fetchSurroundingMessages(any()) } doReturn
                    Result.success(SurroundingMessagesResult(listOf(msg(1)), msg(2), emptyList(), true, false))
                onBlocking { markAsRead(any()) } doReturn Result.success(Unit)
            }
        }

    fun viewModel(repository: MessageListRepository): CometChatMessageListViewModel {
        val store = androidx.lifecycle.ViewModelStore()
        stores += store
        return androidx.lifecycle.ViewModelProvider(
            store,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    CometChatMessageListViewModel(repository = repository, enableListeners = false) as T
            }
        )[CometChatMessageListViewModel::class.java]
    }

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    test("gate off: fetch, next, open and jump make today's single-phase calls and no two-phase read") {
        runTest {
            OfflineGate.testOverride = false
            val repo = repository()
            val vm = viewModel(repo)
            vm.setUser(MockFactory.createUser(uid = "peer", name = "Peer"))

            vm.fetchMessages()
            advanceUntilIdle()
            vm.fetchNextMessages()
            advanceUntilIdle()
            vm.fetchMessagesWithUnreadCount()
            advanceUntilIdle()
            vm.goToMessage(2L)
            advanceUntilIdle()

            vm.offlineOn shouldBe false
            verify(repo, atLeastOnce()).fetchPreviousMessages()
            verify(repo).fetchNextMessages(2L)
            verify(repo).getConversation("peer", CometChatConstants.RECEIVER_TYPE_USER)
            verify(repo).getMessage(2L)
            verify(repo).fetchSurroundingMessages(2L)
            verify(repo).rebuildRequestFromMessageId(1L)
            val twoPhase = repo as MessageListTwoPhaseRepository
            verify(twoPhase, never()).fetchPreviousTwoPhase()
            verify(twoPhase, never()).fetchNextTwoPhase(any())
            vm.offlineLoadMoreOlder.value shouldBe false
            vm.offlineLoadMoreNewer.value shouldBe false
            vm.offlineJumpToLatest.value shouldBe false
        }
    }

    test("gate off (D60): the default repository's open reads the conversation server-only, as today") {
        runTest {
            OfflineGate.testOverride = false
            val listeners = mutableListOf<CometChat.ConversationDetailsListener>()
            cometChatMock.`when`<Unit> {
                CometChat.getConversation(any(), any(), any<CometChat.ConversationDetailsListener>())
            }.thenAnswer { listeners += it.getArgument<CometChat.ConversationDetailsListener>(2); null }
            val vm = viewModel(com.cometchat.uikit.core.data.repository.MessageListRepositoryImpl())
            vm.setUser(MockFactory.createUser(uid = "peer", name = "Peer"))
            vm.setStartFromUnreadMessages(true)

            vm.fetchMessagesWithUnreadCount()
            advanceUntilIdle()

            vm.offlineOn shouldBe false
            listeners.size shouldBe 1
            (listeners.single() is ConversationDetailsOnce) shouldBe true
            (listeners.single() is ConversationDetailsSourceOnce) shouldBe false
        }
    }

    test("gate off: retryOfflineLoadMore and refreshNewestWindow are no-ops") {
        runTest {
            OfflineGate.testOverride = false
            val repo = repository()
            val vm = viewModel(repo)
            vm.setUser(MockFactory.createUser(uid = "peer", name = "Peer"))
            vm.retryOfflineLoadMore()
            MessageListOfflineReads.refreshNewestWindow(vm)
            advanceUntilIdle()

            verify(repo, never()).fetchPreviousMessages()
            verify(repo, never()).resetRequest()
            verify(repo as MessageListTwoPhaseRepository, never()).fetchPreviousTwoPhase()
        }
    }

    test("gate on (contrast): the same ViewModel reads two-phase and never the single-phase fetch") {
        runTest {
            OfflineGate.testOverride = true
            val repo = repository()
            (repo as MessageListTwoPhaseRepository).stub {
                on { fetchPreviousTwoPhase() } doReturn flowOf(
                    TwoPhaseResult.Cached(listOf(msg(1))),
                    TwoPhaseResult.Server(listOf(msg(1), msg(2)))
                )
            }
            val vm = viewModel(repo)
            vm.setUser(MockFactory.createUser(uid = "peer", name = "Peer"))

            vm.fetchMessages()
            advanceUntilIdle()

            vm.offlineOn shouldBe true
            vm.messages.value.map { it.id } shouldBe listOf(1L, 2L)
            vm.isInProgress.value shouldBe false
            verify(repo, never()).fetchPreviousMessages()
        }
    }
})
