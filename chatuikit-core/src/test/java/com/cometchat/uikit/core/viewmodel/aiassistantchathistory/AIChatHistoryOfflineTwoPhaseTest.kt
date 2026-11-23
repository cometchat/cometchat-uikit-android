package com.cometchat.uikit.core.viewmodel.aiassistantchathistory

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.state.ChatHistoryUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatAIAssistantChatHistoryViewModel
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * ENG-41134 — AI assistant chat history on the SDK 5.1.0 two-phase
 * `fetchPrevious(MessageFetchListener)`. The list shows newest first (each page reversed).
 *
 * There is no stale-generation case here: `isInProgress` serialises reads, and the request is reset
 * only on a session end or cache reset (G8, covered by [AIChatHistorySessionEndTest]).
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*AIChatHistoryOfflineTwoPhaseTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AIChatHistoryOfflineTwoPhaseTest : FunSpec({

    isolationMode = IsolationMode.SingleInstance

    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var builderConstruction: MockedConstruction<MessagesRequest.MessagesRequestBuilder>
    lateinit var currentRequest: MessagesRequest
    val listeners = mutableListOf<CometChat.MessageFetchListener>()

    val notConnected = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")

    fun messagesOf(vararg ids: Long): List<BaseMessage> =
        ids.map { MockFactory.createTextMessage(id = it, text = "msg-$it") }

    fun ids(vm: CometChatAIAssistantChatHistoryViewModel) = vm.messages.value.map { it.id }

    beforeSpec { Dispatchers.setMain(testDispatcher) }

    afterSpec {
        Thread.sleep(50)
        Dispatchers.resetMain()
    }

    beforeTest {
        listeners.clear()
        // Every fetchPrevious is held so each test drives the phases explicitly.
        currentRequest = mock<MessagesRequest>().also { req ->
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
            whenever(builder.setGUID(any())).thenReturn(builder)
            whenever(builder.build()).thenAnswer { currentRequest }
        }
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
    }

    afterTest {
        OfflineGate.testOverride = null
        cometChatMock.close()
        builderConstruction.close()
    }

    fun newVm(): CometChatAIAssistantChatHistoryViewModel {
        val vm = CometChatAIAssistantChatHistoryViewModel(enableListeners = false)
        vm.setUser(MockFactory.createUser(uid = "agent-1"))
        return vm
    }

    test("warmup - absorb leaked exceptions") {
        try { runTest(testDispatcher) { } } catch (_: Exception) { }
    }

    test("offline on — cached page is the first paint, then the server page reconciles with no duplicate") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            val ranges = mutableListOf<Int>()
            val job = launch { vm.messagesRangeChanged.collect { ranges.add(it) } }

            listeners.last().onCachedMessages(messagesOf(1, 2, 3))
            ids(vm) shouldBe listOf(3L, 2L, 1L)
            vm.uiState.value.shouldBeInstanceOf<ChatHistoryUIState.Content>()
            vm.isInProgress.value shouldBe true

            listeners.last().onServerMessages(messagesOf(2, 3, 4))
            ids(vm) shouldBe listOf(4L, 3L, 2L)
            vm.isInProgress.value shouldBe false
            vm.hasMore.value shouldBe true
            ranges shouldBe listOf(3) // the server page grew nothing net
            job.cancel()
        }
    }

    test("offline on — an empty cached page does not end the list; only the server page does") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            listeners.last().onCachedMessages(emptyList())
            vm.hasMore.value shouldBe true
            listeners.last().onServerMessages(messagesOf(1, 2))
            ids(vm) shouldBe listOf(2L, 1L)
            vm.hasMore.value shouldBe true

            vm.fetchMessages()
            listeners.last().onCachedMessages(emptyList())
            vm.hasMore.value shouldBe true
            listeners.last().onServerMessages(emptyList())
            vm.hasMore.value shouldBe false
            ids(vm) shouldBe listOf(2L, 1L)
        }
    }

    test("offline on — not-connected keeps the rows shown (no error screen) and ends the spinner") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            listeners.last().onCachedMessages(messagesOf(1, 2))
            listeners.last().onError(notConnected)
            ids(vm) shouldBe listOf(2L, 1L)
            vm.uiState.value.shouldBeInstanceOf<ChatHistoryUIState.Content>()
            vm.isInProgress.value shouldBe false
            vm.hasMore.value shouldBe true
        }
    }

    test("offline on — nothing cached and not connected is today's error state (D32-R2c: online only)") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            listeners.last().onCachedMessages(emptyList())
            listeners.last().onError(notConnected)
            val state = vm.uiState.value
            state.shouldBeInstanceOf<ChatHistoryUIState.Error>()
            state.exception shouldBe notConnected
            vm.isInProgress.value shouldBe false
        }
    }

    test("offline on — another error with nothing shown is today's error state") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            listeners.last().onError(CometChatException("ERR_SERVER", "boom"))
            vm.uiState.value.shouldBeInstanceOf<ChatHistoryUIState.Error>()
        }
    }

    test("offline off — the cached phase is ignored and the output equals today's") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = false
            val vm = newVm()
            val ranges = mutableListOf<Int>()
            val job = launch { vm.messagesRangeChanged.collect { ranges.add(it) } }

            listeners.last().onCachedMessages(messagesOf(99))
            ids(vm) shouldBe emptyList()
            vm.uiState.value shouldBe ChatHistoryUIState.Loading

            listeners.last().onServerMessages(messagesOf(1, 2))
            ids(vm) shouldBe listOf(2L, 1L)
            ranges shouldBe listOf(2)

            vm.fetchMessages()
            listeners.last().onError(notConnected)
            vm.uiState.value.shouldBeInstanceOf<ChatHistoryUIState.Error>()
            job.cancel()
        }
    }
})
