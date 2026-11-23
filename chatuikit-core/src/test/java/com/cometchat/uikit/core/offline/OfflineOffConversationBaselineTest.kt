package com.cometchat.uikit.core.offline

import android.util.Log
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.helpers.CometChatHelper
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.MessageSource
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.ConversationListRepository
import com.cometchat.uikit.core.domain.repository.ConversationListTwoPhaseRepository
import com.cometchat.uikit.core.domain.usecase.DeleteConversationUseCase
import com.cometchat.uikit.core.domain.usecase.GetConversationListUseCase
import com.cometchat.uikit.core.domain.usecase.RefreshConversationListUseCase
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * W3 (ENG-41088) invariant 1: with offline off, the conversation list makes today's requests and
 * produces today's output — the single-phase read, the reconnect refresh, the listener-applied pin
 * and the live +1 for every received message. Nothing W3 added runs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineOffConversationBaselineTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var logMock: MockedStatic<Log>
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var helperMock: MockedStatic<CometChatHelper>
    val stores = mutableListOf<androidx.lifecycle.ViewModelStore>()

    fun user(uid: String) = User(uid, "name-$uid")

    fun message(peer: String, sentAt: Long, source: MessageSource? = null) =
        TextMessage(peer, "m$sentAt", CometChatConstants.RECEIVER_TYPE_USER).apply {
            id = sentAt
            this.sentAt = sentAt
            sender = user(peer)
            receiver = user(peer)
            conversationId = "c_$peer"
            if (source != null) this.source = source
        }

    fun conv(peer: String, lastAt: Long, unread: Int = 0, pinnedAt: Long = 0L) =
        Conversation("c_$peer", CometChatConstants.CONVERSATION_TYPE_USER).apply {
            conversationWith = user(peer)
            lastMessage = message(peer, lastAt)
            unreadMessageCount = unread
            this.pinnedAt = pinnedAt
            updatedAt = lastAt
        }

    class Repo(val rows: List<Conversation>) : ConversationListRepository, ConversationListTwoPhaseRepository {
        var singlePhaseReads = 0
        var twoPhaseReads = 0
        override fun fetchNextTwoPhase(request: ConversationsRequest): Flow<TwoPhaseResult<Conversation>> {
            twoPhaseReads++
            return emptyFlow()
        }
        override suspend fun getConversations(request: ConversationsRequest): Result<List<Conversation>> {
            singlePhaseReads++
            return Result.success(rows)
        }
        override suspend fun deleteConversation(conversationWith: String, conversationType: String) = Result.success(Unit)
        override suspend fun markAsDelivered(conversation: Conversation) = Result.success(Unit)
        override fun hasMoreConversations(): Boolean = false
    }

    lateinit var refreshUseCase: RefreshConversationListUseCase

    fun newViewModel(repo: Repo): CometChatConversationsViewModel {
        val store = androidx.lifecycle.ViewModelStore()
        stores += store
        return androidx.lifecycle.ViewModelProvider(
            store,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    CometChatConversationsViewModel(
                        GetConversationListUseCase(repo), mock<DeleteConversationUseCase>(), refreshUseCase,
                        enableListeners = true
                    ) as T
            }
        )[CometChatConversationsViewModel::class.java]
    }

    fun <T> capture(type: Class<T>, register: (ArgumentCaptor<T>) -> Unit): T {
        val captor = ArgumentCaptor.forClass(type)
        cometChatMock.verify { register(captor) }
        return captor.value
    }

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }

    beforeTest {
        OfflineGate.testOverride = false
        OfflineHub.resetForTest()
        refreshUseCase = mock()
        logMock = Mockito.mockStatic(Log::class.java)
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        cometChatMock.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(user("me"))
        helperMock = Mockito.mockStatic(CometChatHelper::class.java)
        helperMock.`when`<Conversation?> { CometChatHelper.getConversationFromMessage(any()) }.thenAnswer { inv ->
            val m = inv.arguments[0] as BaseMessage
            Conversation(m.conversationId, "user").apply { conversationWith = m.receiver; lastMessage = m }
        }
    }

    afterTest {
        stores.forEach { it.clear() }
        stores.clear()
        helperMock.close()
        cometChatMock.close()
        logMock.close()
        OfflineGate.testOverride = null
    }

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    test("OFF: single-phase read, reconnect refresh, no hub routing") {
        runTest {
            val repo = Repo(listOf(conv("a", 500)))
            whenever(refreshUseCase.invoke(any())).thenReturn(Result.success(listOf(conv("a", 500))))
            val vm = newViewModel(repo)
            advanceUntilIdle()
            repo.singlePhaseReads shouldBe 1
            repo.twoPhaseReads shouldBe 0

            capture(CometChat.ConnectionListener::class.java) { CometChat.addConnectionListener(any(), it.capture()) }
                .onConnected()
            advanceUntilIdle()
            verify(refreshUseCase, times(1)).invoke(any())

            OfflineHub.publishForTest(OfflineHubEvent.ConversationSyncCompleted(OfflineHub.gen, listOf(conv("z", 900))))
            advanceUntilIdle()
            vm.conversations.value.map { it.conversationId } shouldBe listOf("c_a")
            repo.twoPhaseReads shouldBe 0
        }
    }

    test("OFF: a received message bumps unread whatever its source; a pin event applies as today") {
        runTest {
            val vm = newViewModel(Repo(listOf(conv("a", 500, unread = 1), conv("b", 400))))
            advanceUntilIdle()
            capture(CometChat.MessageListener::class.java) { CometChat.addMessageListener(any(), it.capture()) }
                .onTextMessageReceived(message("a", 600, source = MessageSource.SYNC))
            advanceUntilIdle()
            vm.conversations.value.first { it.conversationId == "c_a" }.unreadMessageCount shouldBe 2

            capture(CometChat.ConversationListener::class.java) { CometChat.addConversationListener(any(), it.capture()) }
                .onConversationPinned(conv("b", 400, pinnedAt = 1_000))
            vm.conversations.value.first().conversationId shouldBe "c_b"
            vm.conversations.value.first().pinnedAt shouldBe 1_000L
        }
    }

    test("OFF: refreshList replaces the list with the single-phase refresh, as today") {
        runTest {
            val repo = Repo(listOf(conv("a", 500), conv("b", 400)))
            whenever(refreshUseCase.invoke(any())).thenReturn(Result.success(listOf(conv("c", 600))))
            val vm = newViewModel(repo)
            advanceUntilIdle()
            vm.refreshList()
            advanceUntilIdle()
            verify(refreshUseCase, times(1)).invoke(any())
            vm.conversations.value.map { it.conversationId } shouldBe listOf("c_c")
            repo.twoPhaseReads shouldBe 0
        }
    }
})
