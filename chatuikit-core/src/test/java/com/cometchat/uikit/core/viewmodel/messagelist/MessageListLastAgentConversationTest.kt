package com.cometchat.uikit.core.viewmodel.messagelist

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.state.MessageListUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * ENG-41155 — `fetchLastAgentConversation` reads through the SDK 5.1.0 two-phase
 * `fetchPrevious(MessageFetchListener)` instead of the deprecated `CallbackListener` overload.
 * It acts on the server phase only (once), ignores a cached phase, and keeps today's error path
 * (fall back to the empty/greeting state).
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*MessageListLastAgentConversationTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageListLastAgentConversationTest : FunSpec({

    isolationMode = IsolationMode.SingleInstance

    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var repository: MessageListRepository
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var builderConstruction: MockedConstruction<MessagesRequest.MessagesRequestBuilder>
    lateinit var request: MessagesRequest
    val listeners = mutableListOf<CometChat.MessageFetchListener>()

    val agentUid = "agent-1"

    fun starter(id: Long): BaseMessage = MockFactory.createTextMessage(id = id, text = "m-$id")
    fun reply(id: Long, parent: Long): BaseMessage =
        MockFactory.createTextMessage(id = id, text = "r-$id", parentMessageId = parent)

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec {
        Thread.sleep(50)
        Dispatchers.resetMain()
    }

    beforeTest {
        listeners.clear()
        OfflineGate.testOverride = false
        repository = mock()
        request = mock<MessagesRequest>().also { req ->
            doAnswer { inv -> listeners.add(inv.getArgument(0)); null }
                .whenever(req).fetchPrevious(any<CometChat.MessageFetchListener>())
        }
        builderConstruction = Mockito.mockConstruction(MessagesRequest.MessagesRequestBuilder::class.java) { builder, _ ->
            whenever(builder.setLimit(any())).thenReturn(builder)
            whenever(builder.setCategories(any())).thenReturn(builder)
            whenever(builder.setTypes(any())).thenReturn(builder)
            whenever(builder.hideDeletedMessages(any())).thenReturn(builder)
            whenever(builder.hideReplies(any())).thenReturn(builder)
            whenever(builder.setUID(any())).thenReturn(builder)
            whenever(builder.build()).thenAnswer { request }
        }
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
    }

    afterTest {
        OfflineGate.testOverride = null
        cometChatMock.close()
        builderConstruction.close()
    }

    suspend fun newVm(): CometChatMessageListViewModel {
        whenever(repository.fetchPreviousMessages()).thenReturn(Result.success(emptyList()))
        val vm = CometChatMessageListViewModel(repository = repository, enableListeners = false)
        // gotoMessageId = 1 keeps the first-fetch markConversationRead path out of the test.
        vm.setUser(MockFactory.createUser(uid = agentUid, name = "Agent"), gotoMessageId = 1L)
        return vm
    }

    test("uses the MessageFetchListener overload, ignores a cached phase, acts on the server phase once") {
        runTest(testDispatcher) {
            val vm = newVm()
            vm.fetchLastAgentConversation()

            verify(request, times(1)).fetchPrevious(any<CometChat.MessageFetchListener>())
            @Suppress("DEPRECATION")
            verify(request, never()).fetchPrevious(any<CometChat.CallbackListener<List<BaseMessage>>>())
            listeners.size shouldBe 1

            // A cached phase changes nothing: no thread resolved, still loading.
            listeners.single().onCachedMessages(listOf(starter(99)))
            vm.getParentMessageId() shouldBe -1L
            vm.uiState.value shouldBe MessageListUIState.Loading
            verify(repository, never()).configureForUser(any(), any(), any(), eq(99L), anyOrNull())

            // The server phase resolves the latest session starter (replies do not qualify).
            listeners.single().onServerMessages(listOf(starter(10), reply(50, parent = 10), starter(20)))
            vm.getParentMessageId() shouldBe 20L
            verify(repository, times(1)).configureForUser(any(), any(), any(), eq(20L), anyOrNull())
            verify(repository, times(1)).fetchPreviousMessages()
        }
    }

    test("server phase with no session starter shows the empty state") {
        runTest(testDispatcher) {
            val vm = newVm()
            vm.fetchLastAgentConversation()
            listeners.single().onServerMessages(listOf(reply(5, parent = 3)))
            vm.getParentMessageId() shouldBe -1L
            vm.uiState.value shouldBe MessageListUIState.Empty
        }
    }

    test("onError falls back to the empty state, as today") {
        runTest(testDispatcher) {
            val vm = newVm()
            vm.fetchLastAgentConversation()
            listeners.single().onError(CometChatException("ERR", "boom"))
            vm.getParentMessageId() shouldBe -1L
            vm.uiState.value shouldBe MessageListUIState.Empty
            verify(repository, never()).fetchPreviousMessages()
        }
    }
})
