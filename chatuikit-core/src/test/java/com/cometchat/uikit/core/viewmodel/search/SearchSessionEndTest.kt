package com.cometchat.uikit.core.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.OfflineCacheResetReason
import com.cometchat.uikit.core.constants.SearchFilter
import com.cometchat.uikit.core.domain.repository.SearchRepository
import com.cometchat.uikit.core.domain.repository.SearchTwoPhaseRepository
import com.cometchat.uikit.core.domain.usecase.FetchConversationsUseCase
import com.cometchat.uikit.core.domain.usecase.FetchMessagesUseCase
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineHub
import com.cometchat.uikit.core.offline.OfflineHubEvent
import com.cometchat.uikit.core.offline.TwoPhaseResult
import com.cometchat.uikit.core.state.SearchUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatSearchViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Each two-phase read is a channel the test feeds phase by phase. */
private class ChannelSearchRepository : SearchRepository, SearchTwoPhaseRepository {
    val conversationReads = mutableListOf<Channel<TwoPhaseResult<Conversation>>>()
    val messageReads = mutableListOf<Channel<TwoPhaseResult<BaseMessage>>>()

    override fun getConversationsTwoPhase(request: ConversationsRequest): Flow<TwoPhaseResult<Conversation>> =
        Channel<TwoPhaseResult<Conversation>>(Channel.UNLIMITED).also { conversationReads += it }.consumeAsFlow()

    override fun getMessagesTwoPhase(request: MessagesRequest): Flow<TwoPhaseResult<BaseMessage>> =
        Channel<TwoPhaseResult<BaseMessage>>(Channel.UNLIMITED).also { messageReads += it }.consumeAsFlow()

    override suspend fun getConversations(request: ConversationsRequest): Result<List<Conversation>> =
        error("two-phase repository")

    override suspend fun getMessages(request: MessagesRequest): Result<List<BaseMessage>> = error("two-phase repository")

    override fun hasMoreConversations(): Boolean = true
    override fun hasMoreMessages(): Boolean = true

    fun conversations(vararg ids: String, read: Int = conversationReads.lastIndex) {
        conversationReads[read].trySend(TwoPhaseResult.Server(ids.map { MockFactory.createConversation(id = it) }))
        conversationReads[read].close()
    }

    fun messages(vararg ids: Long, read: Int = messageReads.lastIndex) {
        messageReads[read].trySend(TwoPhaseResult.Server(ids.map { MockFactory.createTextMessage(id = it) }))
        messageReads[read].close()
    }
}

/**
 * Subclassed use cases, so the ViewModel takes the plain (non two-phase) read with offline on, as it
 * does for an app's own use case. Each read is held on a non-cancellable suspension, the way an SDK
 * callback wrapper is: it completes even after its job was cancelled, so only the ViewModel's own
 * generation guard can stop its result from landing.
 */
private class HeldConversations(repo: SearchRepository) : FetchConversationsUseCase(repo) {
    val calls = mutableListOf<Continuation<Result<List<Conversation>>>>()
    override suspend fun invoke(request: ConversationsRequest): Result<List<Conversation>> =
        suspendCoroutine { calls += it }
    override fun hasMore(): Boolean = true
    fun complete(index: Int, vararg ids: String) =
        calls[index].resume(Result.success(ids.map { MockFactory.createConversation(id = it) }))
}

private class HeldMessages(repo: SearchRepository) : FetchMessagesUseCase(repo) {
    val calls = mutableListOf<Continuation<Result<List<BaseMessage>>>>()
    override suspend fun invoke(request: MessagesRequest): Result<List<BaseMessage>> =
        suspendCoroutine { calls += it }
    override fun hasMore(): Boolean = true
    fun complete(index: Int, vararg ids: Long) =
        calls[index].resume(Result.success(ids.map { MockFactory.createTextMessage(id = it) }))
}

/**
 * ENG-41568 (G8) — the search screen clears **both** result sets (conversations and messages) on a
 * hub `SessionEnded`, and on a `CacheReset` clears them and runs the search on screen once more,
 * quietly over results (D84 rule 5).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchSessionEndTest : FunSpec({

    lateinit var dispatcher: TestDispatcher
    lateinit var repo: ChannelSearchRepository
    val stores = mutableListOf<ViewModelStore>()

    fun vm(): CometChatSearchViewModel {
        val store = ViewModelStore()
        stores += store
        return ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CometChatSearchViewModel(FetchConversationsUseCase(repo), FetchMessagesUseCase(repo)) as T
        })[CometChatSearchViewModel::class.java]
    }

    fun idle() = dispatcher.scheduler.advanceUntilIdle()

    lateinit var heldConversations: HeldConversations
    lateinit var heldMessages: HeldMessages

    /** A search ViewModel on the plain read path (subclassed use cases), offline on. */
    fun plainVm(): CometChatSearchViewModel {
        heldConversations = HeldConversations(repo)
        heldMessages = HeldMessages(repo)
        val store = ViewModelStore()
        stores += store
        return ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CometChatSearchViewModel(heldConversations, heldMessages) as T
        })[CometChatSearchViewModel::class.java]
    }

    fun logout() {
        OfflineHub.onLogout()
        idle()
    }

    fun cacheReset() {
        OfflineHub.publishForTest(OfflineHubEvent.CacheReset(OfflineHub.gen, OfflineCacheResetReason.CORRUPTED, 0))
        idle()
    }

    fun convIds(vm: CometChatSearchViewModel) = vm.conversations.value.map { it.conversationId }
    fun msgIds(vm: CometChatSearchViewModel) = vm.messages.value.map { it.id }

    /** A keyword search over both scopes, answered by the server. */
    fun searched(vm: CometChatSearchViewModel) {
        vm.searchConversationsAndMessages("hello", emptySet())
        idle()
        repo.conversations("c1")
        repo.messages(1L)
        idle()
        convIds(vm) shouldBe listOf("c1")
        msgIds(vm) shouldBe listOf(1L)
        vm.uiState.value shouldBe SearchUIState.Content
    }

    beforeTest {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        OfflineGate.testOverride = true
        OfflineHub.resetForTest()
        repo = ChannelSearchRepository()
    }

    afterTest {
        stores.forEach { it.clear() }
        stores.clear()
        OfflineGate.testOverride = null
        Dispatchers.resetMain()
    }

    test("search: sessionEnded_clearsRowsAndResetsPaging") {
        val vm = vm()
        searched(vm)

        logout()

        vm.conversations.value shouldBe emptyList()
        vm.messages.value shouldBe emptyList()
        vm.hasMoreConversations.value shouldBe true
        vm.hasMoreMessages.value shouldBe true
        vm.uiState.value shouldBe SearchUIState.Initial
        // The requests are gone: "see more" reads nothing, and the same search runs again from page one.
        vm.fetchMoreConversations()
        vm.fetchMoreMessages()
        repo.conversationReads.size shouldBe 1
        repo.messageReads.size shouldBe 1
        vm.searchConversationsAndMessages("hello", emptySet())
        idle()
        repo.conversationReads.size shouldBe 2
        repo.messageReads.size shouldBe 2
    }

    test("search: sessionEnded_dropsResultOfReadInFlight") {
        val vm = vm()
        vm.searchConversationsAndMessages("hello", emptySet())
        idle()
        logout()
        repo.conversations("old", read = 0)
        repo.messages(9L, read = 0)
        idle()
        vm.conversations.value shouldBe emptyList()
        vm.messages.value shouldBe emptyList()
        vm.uiState.value shouldBe SearchUIState.Initial
    }

    test("search: sessionEnded_dropsResultOfReadInFlight — plain first page, both result sets") {
        val vm = plainVm()
        vm.searchConversationsAndMessages("hello", emptySet())
        idle()
        heldConversations.calls.size shouldBe 1
        heldMessages.calls.size shouldBe 1
        logout()
        // The search job was cancelled, but these reads still complete (as an SDK callback does).
        heldConversations.complete(0, "old")
        heldMessages.complete(0, 9L)
        idle()
        vm.conversations.value shouldBe emptyList()
        vm.messages.value shouldBe emptyList()
        vm.uiState.value shouldBe SearchUIState.Initial
    }

    test("search: sessionEnded_dropsResultOfReadInFlight — plain see-more page, both result sets") {
        val vm = plainVm()
        vm.searchConversationsAndMessages("hello", emptySet())
        idle()
        heldConversations.complete(0, "c1")
        heldMessages.complete(0, 1L)
        idle()
        convIds(vm) shouldBe listOf("c1")
        vm.fetchMoreConversations()
        vm.fetchMoreMessages()
        heldConversations.calls.size shouldBe 2
        heldMessages.calls.size shouldBe 2
        logout()
        heldConversations.complete(1, "old")
        heldMessages.complete(1, 9L)
        idle()
        vm.conversations.value shouldBe emptyList()
        vm.messages.value shouldBe emptyList()
        vm.uiState.value shouldBe SearchUIState.Initial
    }

    test("search: sessionEnded_dropsResultOfReadInFlight — two-phase see-more page, both result sets") {
        val vm = vm()
        searched(vm)
        vm.fetchMoreConversations()
        vm.fetchMoreMessages()
        repo.conversationReads.size shouldBe 2
        repo.messageReads.size shouldBe 2
        logout()
        repo.conversations("old", read = 1)
        repo.messages(9L, read = 1)
        idle()
        vm.conversations.value shouldBe emptyList()
        vm.messages.value shouldBe emptyList()
        vm.uiState.value shouldBe SearchUIState.Initial
    }

    test("search: staleSessionEnded_isIgnored") {
        val vm = vm()
        searched(vm)
        OfflineHub.publishForTest(OfflineHubEvent.SessionEnded(OfflineHub.gen))
        idle()
        convIds(vm) shouldBe listOf("c1")
        msgIds(vm) shouldBe listOf(1L)
        vm.uiState.value shouldBe SearchUIState.Content
    }

    test("search: cacheReset_clearsThenReloadsOnce") {
        val vm = vm()
        vm.searchConversationsAndMessages("hello", emptySet())
        idle()
        repo.conversations("c1")
        repo.messages(1L)
        idle()

        OfflineHub.publishForTest(OfflineHubEvent.CacheReset(OfflineHub.gen, OfflineCacheResetReason.CORRUPTED, 0))
        vm.conversations.value shouldBe emptyList() // cleared before the re-run
        vm.messages.value shouldBe emptyList()
        idle() // past the debounce

        repo.conversationReads.size shouldBe 2
        repo.messageReads.size shouldBe 2
        repo.conversations("c2")
        repo.messages(2L)
        idle()
        convIds(vm) shouldBe listOf("c2")
        msgIds(vm) shouldBe listOf(2L)
        vm.uiState.value shouldBe SearchUIState.Content
        repo.conversationReads.size shouldBe 2
        repo.messageReads.size shouldBe 2
    }

    test("search: cacheReset_reloadFailureOverRows_isQuiet") {
        val vm = vm()
        val errors = mutableListOf<CometChatException>()
        CoroutineScope(dispatcher).launch { vm.rowsKeptError.collect { errors += it } }
        // A filter without a keyword: the cached phase paints (conversations only).
        vm.searchConversationsAndMessages("", setOf(SearchFilter.GROUPS))
        idle()
        repo.conversations("c1")
        idle()

        cacheReset()
        repo.conversationReads.size shouldBe 2
        repo.conversationReads[1].trySend(TwoPhaseResult.Cached(listOf(MockFactory.createConversation(id = "c1"))))
        repo.conversationReads[1].trySend(TwoPhaseResult.Error(CometChatException("ERR_SERVER", "boom")))
        repo.conversationReads[1].close()
        idle()

        convIds(vm) shouldBe listOf("c1")
        vm.uiState.value shouldBe SearchUIState.Content
        errors shouldBe emptyList()
    }

    test("search: offlineOff_noChange") {
        OfflineGate.testOverride = false
        val vm = vm()
        searched(vm)
        logout()
        cacheReset()
        convIds(vm) shouldBe listOf("c1")
        msgIds(vm) shouldBe listOf(1L)
        vm.uiState.value shouldBe SearchUIState.Content
        repo.conversationReads.size shouldBe 1
        repo.messageReads.size shouldBe 1
    }
})
