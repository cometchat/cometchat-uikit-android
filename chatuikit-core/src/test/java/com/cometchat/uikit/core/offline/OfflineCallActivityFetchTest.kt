package com.cometchat.uikit.core.offline

import androidx.lifecycle.viewModelScope
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.model.InitiatedCall
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking

/**
 * ENG-41493 merge seam: 6.2.0 (Ringing v2) reads the newer page when a call for the open chat starts
 * or ends. With offline on, that read goes through the offline seam, which anchors on the newest row
 * with a server id, so an unsent tail row (id 0) never becomes the cursor and stays last. Offline
 * off keeps 6.2.0's `fetchMissedMessages` path.
 */
class OfflineCallActivityFetchTest : FunSpec({

    val me = "me"
    val peer = "peer"

    lateinit var cometChatStatic: MockedStatic<CometChat>
    lateinit var callsStatic: MockedStatic<CometChatCalls>

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

    fun callFromPeer(): InitiatedCall = mock {
        on { sessionId } doReturn "session-1"
        on { sender } doReturn peer
        on { receiver } doReturn me
        on { receiverType } doReturn CometChatConstants.RECEIVER_TYPE_USER
        on { type } doReturn CometChatConstants.CALL_TYPE_AUDIO
    }

    fun incomingCallListener(): CometChatCalls.IncomingCallListener {
        val captor = ArgumentCaptor.forClass(CometChatCalls.IncomingCallListener::class.java)
        callsStatic.verify({ CometChatCalls.addIncomingCallListener(any(), captor.capture()) }, Mockito.atLeastOnce())
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
                onBlocking { fetchActionMessages(any()) } doReturn Result.success(emptyList())
                onBlocking { fetchNextMessages(any()) } doReturn Result.success(emptyList())
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
        callsStatic = Mockito.mockStatic(CometChatCalls::class.java)
    }

    afterTest {
        live.forEach { it.teardown() }
        live.clear()
        callsStatic.close()
        cometChatStatic.close()
        OfflineGate.testOverride = null
        Dispatchers.resetMain()
    }

    test("offline on: a call event reads the newer page from the newest server row, never id 0; unsent rows stay last") {
        OfflineGate.testOverride = true
        val repo = repository()
        val anchors = mutableListOf<Long>()
        (repo as MessageListTwoPhaseRepository).stub {
            on { fetchPreviousTwoPhase() } doReturn flowOf(
                TwoPhaseResult.Cached(listOf(text(1, peer), text(2, peer), unsent("L1"))),
                TwoPhaseResult.Server(listOf(text(1, peer), text(2, peer)))
            )
            on { fetchNextTwoPhase(any()) } doAnswer { inv ->
                anchors += inv.getArgument<Long>(0)
                flowOf(TwoPhaseResult.Server(listOf(text(3, peer))))
            }
        }
        val vm = chat(repo)
        ids(vm) shouldBe listOf(1L, 2L, "l:L1")

        incomingCallListener().onIncomingCallEnded(callFromPeer(), "ended")

        anchors shouldBe listOf(2L)
        ids(vm) shouldBe listOf(1L, 2L, 3L, "l:L1")
        verifyBlocking(repo, never()) { fetchActionMessages(any()) }
        verifyBlocking(repo, never()) { fetchNextMessages(any()) }
    }

    test("offline off: a call event runs 6.2.0's missed-message fetch from the last row") {
        OfflineGate.testOverride = false
        val repo = repository()
        repo.stub {
            onBlocking { fetchPreviousMessages() } doReturn Result.success(listOf(text(1, peer), text(2, peer)))
        }
        val vm = chat(repo)
        ids(vm) shouldBe listOf(1L, 2L)

        incomingCallListener().onIncomingCallReceived(callFromPeer())

        verifyBlocking(repo) { fetchActionMessages(2L) }
        verifyBlocking(repo) { fetchNextMessages(2L) }
        verify(repo as MessageListTwoPhaseRepository, never()).fetchNextTwoPhase(any())
    }
})
