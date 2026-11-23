package com.cometchat.uikit.core.viewmodel.aiassistantchathistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.OfflineCacheResetReason
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineHub
import com.cometchat.uikit.core.offline.OfflineHubEvent
import com.cometchat.uikit.core.state.ChatHistoryUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatAIAssistantChatHistoryViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * ENG-41568 (G8) — the AI assistant chat history clears on a hub `SessionEnded`, and on a
 * `CacheReset` clears and reads the first page once more, keeping cached rows quietly if that read
 * fails (D84 rule 5). Each request the builder builds is a new one, so a test can tell page one of
 * a fresh request from a later page of the old one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AIChatHistorySessionEndTest : FunSpec({

    lateinit var dispatcher: TestDispatcher
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var builderConstruction: MockedConstruction<MessagesRequest.MessagesRequestBuilder>
    val stores = mutableListOf<ViewModelStore>()
    // Every fetchPrevious is held: (the request it was made on, its listener), in call order.
    val reads = mutableListOf<Pair<MessagesRequest, CometChat.MessageFetchListener>>()

    fun messagesOf(vararg ids: Long): List<BaseMessage> = ids.map { MockFactory.createTextMessage(id = it) }

    fun vm(): CometChatAIAssistantChatHistoryViewModel {
        val store = ViewModelStore()
        stores += store
        return ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CometChatAIAssistantChatHistoryViewModel(enableListeners = true) as T
        })[CometChatAIAssistantChatHistoryViewModel::class.java]
            .also { it.setUser(MockFactory.createUser(uid = "agent-1")) }
    }

    fun ids(vm: CometChatAIAssistantChatHistoryViewModel) = vm.messages.value.map { it.id }
    fun idle() = dispatcher.scheduler.advanceUntilIdle()
    fun last() = reads.last().second

    fun logout() {
        OfflineHub.onLogout()
        idle()
    }

    fun cacheReset() {
        OfflineHub.publishForTest(OfflineHubEvent.CacheReset(OfflineHub.gen, OfflineCacheResetReason.CORRUPTED, 0))
        idle()
    }

    beforeTest {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        OfflineGate.testOverride = true
        OfflineHub.resetForTest()
        reads.clear()
        builderConstruction = Mockito.mockConstruction(MessagesRequest.MessagesRequestBuilder::class.java) { builder, _ ->
            whenever(builder.setLimit(any())).thenReturn(builder)
            whenever(builder.setCategories(any())).thenReturn(builder)
            whenever(builder.setTypes(any())).thenReturn(builder)
            whenever(builder.hideDeletedMessages(any())).thenReturn(builder)
            whenever(builder.hideReplies(any())).thenReturn(builder)
            whenever(builder.setUID(any())).thenReturn(builder)
            whenever(builder.setGUID(any())).thenReturn(builder)
            whenever(builder.build()).thenAnswer {
                mock<MessagesRequest>().also { req ->
                    doAnswer { inv -> reads.add(req to inv.getArgument(0)); null }
                        .whenever(req).fetchPrevious(any<CometChat.MessageFetchListener>())
                }
            }
        }
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
    }

    afterTest {
        stores.forEach { it.clear() }
        stores.clear()
        OfflineGate.testOverride = null
        cometChatMock.close()
        builderConstruction.close()
        Dispatchers.resetMain()
    }

    test("aiChatHistory: sessionEnded_clearsRowsAndResetsPaging") {
        val vm = vm()
        last().onServerMessages(messagesOf(1, 2))
        vm.fetchMessages()
        last().onServerMessages(emptyList()) // the history ended
        vm.hasMore.value shouldBe false
        val oldRequest = reads.last().first

        logout()

        vm.messages.value shouldBe emptyList()
        vm.uiState.value shouldBe ChatHistoryUIState.Loading
        vm.hasMore.value shouldBe true
        vm.fetchMessages() // a fresh request, from page one
        reads.size shouldBe 3
        (reads.last().first === oldRequest) shouldBe false
    }

    test("aiChatHistory: sessionEnded_dropsResultOfReadInFlight") {
        val vm = vm()
        last().onServerMessages(messagesOf(1))
        vm.fetchMessages() // page two in flight
        val inFlight = last()
        logout()
        inFlight.onCachedMessages(messagesOf(9))
        inFlight.onServerMessages(messagesOf(9))
        vm.messages.value shouldBe emptyList()
        vm.uiState.value shouldBe ChatHistoryUIState.Loading
        vm.isInProgress.value shouldBe false
    }

    test("aiChatHistory: staleSessionEnded_isIgnored") {
        val vm = vm()
        last().onServerMessages(messagesOf(1))
        OfflineHub.publishForTest(OfflineHubEvent.SessionEnded(OfflineHub.gen))
        idle()
        ids(vm) shouldBe listOf(1L)
        vm.uiState.value.shouldBeInstanceOf<ChatHistoryUIState.Content>()
    }

    test("aiChatHistory: cacheReset_clearsThenReloadsOnce") {
        val vm = vm()
        last().onServerMessages(messagesOf(1, 2))
        cacheReset()
        vm.messages.value shouldBe emptyList()
        reads.size shouldBe 2
        last().onServerMessages(messagesOf(3))
        ids(vm) shouldBe listOf(3L)
        reads.size shouldBe 2
    }

    test("aiChatHistory: cacheReset_reloadFailureOverRows_isQuiet") {
        val vm = vm()
        last().onServerMessages(messagesOf(1))
        cacheReset()
        last().onCachedMessages(messagesOf(1))
        last().onError(CometChatException("ERR_SERVER", "boom"))
        ids(vm) shouldBe listOf(1L)
        vm.uiState.value.shouldBeInstanceOf<ChatHistoryUIState.Content>()
    }

    test("aiChatHistory: offlineOff_noChange") {
        OfflineGate.testOverride = false
        val vm = vm()
        last().onServerMessages(messagesOf(1))
        logout()
        cacheReset()
        ids(vm) shouldBe listOf(1L)
        vm.uiState.value.shouldBeInstanceOf<ChatHistoryUIState.Content>()
        reads.size shouldBe 1
    }
})
