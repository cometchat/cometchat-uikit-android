package com.cometchat.uikit.core.offline

import androidx.lifecycle.viewModelScope
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.model.SurroundingMessagesResult
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.domain.repository.MessageListTwoPhaseRepository
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

/**
 * ENG-41087 realtime guard: with offline on, "at the latest position" compares `latestMessageId`
 * with the newest row that has a server id, so own unsent rows at the tail never stop a peer's live
 * message from being shown; the message lands above them and they stay last. The "new messages"
 * state is raised only when the newest page is genuinely not loaded. Offline off is today's rule.
 */
class OfflineRealtimeGuardTest : FunSpec({

    val me = "me"
    val peer = "peer"

    lateinit var cometChatStatic: MockedStatic<CometChat>

    class ListeningViewModel(repository: MessageListRepository) :
        CometChatMessageListViewModel(repository, enableListeners = true) {
        override fun getLoggedInUserUid(): String? = "me"
        init { setEnableConversationSummary(false) }
        fun teardown() {
            onCleared()
            viewModelScope.cancel()
            runBlocking { viewModelScope.coroutineContext.job.join() }
        }
    }

    val live = mutableListOf<ListeningViewModel>()

    fun user(uid: String) = User().apply { this.uid = uid; name = uid }

    fun text(id: Long, from: String, localId: String? = null): TextMessage =
        TextMessage(if (from == me) peer else me, "m$id", CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            sentAt = 1000L + id
            sender = user(from)
            this.localMessageId = localId
        }

    fun unsent(localId: String): BaseMessage = text(0L, me, localId).apply { sentAt = 2000L }

    fun ids(vm: CometChatMessageListViewModel) =
        vm.messages.value.map { if (it.id > 0) it.id else "l:${it.localMessageId}" }

    fun listener(): CometChat.MessageListener {
        val captor = ArgumentCaptor.forClass(CometChat.MessageListener::class.java)
        cometChatStatic.verify({ CometChat.addMessageListener(any(), captor.capture()) }, Mockito.atLeastOnce())
        return captor.value
    }

    fun repository(): MessageListRepository =
        mock<MessageListRepository>(extraInterfaces = arrayOf(MessageListTwoPhaseRepository::class)).apply {
            stub {
                on { hasMorePreviousMessages() } doReturn false
                on { getEffectiveMessagesTypes() } doReturn emptyList()
                on { getEffectiveMessagesCategories() } doReturn emptyList()
                onBlocking { markAsRead(any()) } doReturn Result.success(Unit)
                onBlocking { markAsDelivered(any()) } doReturn Result.success(Unit)
            }
        }

    fun chat(repo: MessageListRepository): ListeningViewModel {
        val vm = ListeningViewModel(repo)
        live += vm
        vm.setUser(user(peer), gotoMessageId = 0)
        vm.fetchMessages()
        return vm
    }

    beforeTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        OfflineHub.resetForTest()
        cometChatStatic = Mockito.mockStatic(CometChat::class.java)
        cometChatStatic.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(user(me))
    }

    afterTest {
        live.forEach { it.teardown() }
        live.clear()
        cometChatStatic.close()
        OfflineGate.testOverride = null
        Dispatchers.resetMain()
    }

    test("offline on: unsent last row → a peer's live message is inserted at once above it, no pill") {
        OfflineGate.testOverride = true
        val repo = repository()
        (repo as MessageListTwoPhaseRepository).stub {
            on { fetchPreviousTwoPhase() } doReturn flowOf(
                TwoPhaseResult.Cached(listOf(text(1, peer), text(2, peer), unsent("L1"))),
                TwoPhaseResult.Server(listOf(text(1, peer), text(2, peer)))
            )
        }
        val vm = chat(repo)
        ids(vm) shouldBe listOf(1L, 2L, "l:L1")

        listener().onTextMessageReceived(text(3, peer))

        ids(vm) shouldBe listOf(1L, 2L, 3L, "l:L1")
        vm.hasMoreNewMessages.value shouldBe false

        listener().onTextMessageReceived(text(4, peer))
        ids(vm) shouldBe listOf(1L, 2L, 3L, 4L, "l:L1")
        vm.hasMoreNewMessages.value shouldBe false
    }

    test("offline on: when the newest page is genuinely not loaded, the live message is held and the pill state raised") {
        OfflineGate.testOverride = true
        val repo = repository()
        (repo as MessageListTwoPhaseRepository).stub {
            on { fetchPreviousTwoPhase() } doReturn flowOf(TwoPhaseResult.Server(listOf(text(1, peer), text(2, peer))))
        }
        repo.stub {
            onBlocking { getMessage(any()) } doReturn Result.success(text(6, peer))
            onBlocking { fetchSurroundingMessages(any()) } doReturn Result.success(
                SurroundingMessagesResult(listOf(text(5, peer)), text(6, peer), listOf(text(7, peer)), true, true)
            )
        }
        val vm = chat(repo)
        vm.goToMessage(6L) // a window older than the newest page: hasMoreNext = true

        listener().onTextMessageReceived(text(30, peer))

        ids(vm) shouldBe listOf(5L, 6L, 7L)
        vm.hasMoreNewMessages.value shouldBe true
    }

    test("offline off: unchanged — the guard compares the last row of any kind, and the message is appended") {
        OfflineGate.testOverride = false
        val repo = repository()
        repo.stub {
            onBlocking { fetchPreviousMessages() } doReturn Result.success(listOf(text(1, peer), text(2, peer)))
        }
        val vm = chat(repo)

        listener().onTextMessageReceived(text(3, peer))

        ids(vm) shouldBe listOf(1L, 2L, 3L)
        vm.hasMoreNewMessages.value shouldBe false
    }

    test("offline off: a trailing row without a server id is still compared as today (appended after it)") {
        OfflineGate.testOverride = false
        val repo = repository()
        repo.stub {
            onBlocking { fetchPreviousMessages() } doReturn Result.success(listOf(text(1, peer), unsent("L1")))
        }
        val vm = chat(repo)

        listener().onTextMessageReceived(text(3, peer))

        // Today's fetch sets latestMessageId to the last row (id 0), so the guard passes and the live
        // message is appended at the end — exactly today's order.
        ids(vm) shouldBe listOf(1L, "l:L1", 3L)
    }
})
