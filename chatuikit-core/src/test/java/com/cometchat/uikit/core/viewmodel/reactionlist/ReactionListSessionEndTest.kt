package com.cometchat.uikit.core.viewmodel.reactionlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.OfflineCacheResetReason
import com.cometchat.chat.models.Reaction
import com.cometchat.uikit.core.data.datasource.ReactionListDataSource
import com.cometchat.uikit.core.data.datasource.ReactionListTwoPhaseDataSource
import com.cometchat.uikit.core.data.repository.ReactionListRepositoryImpl
import com.cometchat.uikit.core.domain.usecase.FetchReactionsUseCase
import com.cometchat.uikit.core.domain.usecase.RemoveReactionUseCase
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineHub
import com.cometchat.uikit.core.offline.OfflineHubEvent
import com.cometchat.uikit.core.offline.TwoPhaseResult
import com.cometchat.uikit.core.state.ReactionListUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatReactionListViewModel
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.cometchat.uikit.core.domain.repository.ReactionListRepository
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A subclassed use case, so the ViewModel takes the plain (non two-phase) read with offline on, as
 * it does for an app's own use case. Each read is held on a non-cancellable suspension (as an SDK
 * callback wrapper is), so only the ViewModel's own generation guard can stop its result.
 */
private class HeldReactionsUseCase(repo: ReactionListRepository) : FetchReactionsUseCase(repo) {
    val calls = mutableListOf<Continuation<Result<List<Reaction>>>>()
    override suspend fun invoke(request: ReactionsRequest): Result<List<Reaction>> = suspendCoroutine { calls += it }
}

/**
 * ENG-41568 (G8) — the reaction list clears on a hub `SessionEnded`, and on a `CacheReset` clears
 * and reads the selected tab once more, quietly over rows (D84 rule 5).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReactionListSessionEndTest : FunSpec({

    /** Every two-phase read is a channel the test drives; the plain read returns [single]. */
    class Source : ReactionListDataSource, ReactionListTwoPhaseDataSource {
        val reads = mutableListOf<Channel<TwoPhaseResult<Reaction>>>()
        var single: List<Reaction> = emptyList()
        var singleCalls = 0

        override suspend fun fetchReactions(request: ReactionsRequest): Result<List<Reaction>> {
            singleCalls++
            return Result.success(single)
        }

        override suspend fun removeReaction(messageId: Long, emoji: String): Result<BaseMessage> =
            Result.failure(CometChatException("ERR", "unused"))

        override fun fetchReactionsTwoPhase(request: ReactionsRequest): Flow<TwoPhaseResult<Reaction>> =
            Channel<TwoPhaseResult<Reaction>>(Channel.UNLIMITED).also { reads += it }.consumeAsFlow()
    }

    lateinit var dispatcher: TestDispatcher
    lateinit var source: Source
    lateinit var builder: ReactionsRequest.ReactionsRequestBuilder
    val stores = mutableListOf<ViewModelStore>()

    fun reaction(id: String): Reaction = Reaction().apply {
        setReactionId(id)
        setUid("u-$id")
        setReaction("👍")
        setMessageId(1L)
    }

    fun vm(): CometChatReactionListViewModel {
        val store = ViewModelStore()
        stores += store
        val repo = ReactionListRepositoryImpl(source)
        val vm = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CometChatReactionListViewModel(FetchReactionsUseCase(repo), RemoveReactionUseCase(repo), enableListeners = true) as T
        })[CometChatReactionListViewModel::class.java]
        vm.setBaseMessage(MockFactory.createTextMessage(id = 1L))
        vm.setReactionsRequestBuilder(builder)
        return vm
    }

    fun ids(vm: CometChatReactionListViewModel) = vm.reactedUsers.value.map { it.reactionId }
    fun idle() = dispatcher.scheduler.advanceUntilIdle()

    fun server(read: Int, vararg ids: String) {
        source.reads[read].trySend(TwoPhaseResult.Server(ids.map { reaction(it) }))
        source.reads[read].close()
        idle()
    }

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
        source = Source()
        builder = mock<ReactionsRequest.ReactionsRequestBuilder>()
        whenever(builder.build()).thenAnswer { mock<ReactionsRequest>() }
    }

    afterTest {
        stores.forEach { it.clear() }
        stores.clear()
        OfflineGate.testOverride = null
        Dispatchers.resetMain()
    }

    test("reactionList: sessionEnded_clearsRowsAndResetsPaging") {
        val vm = vm()
        vm.fetchReactedUsers()
        server(0, "a", "b")
        ids(vm) shouldBe listOf("a", "b")
        verify(builder, times(1)).build()

        logout()

        vm.reactedUsers.value shouldBe emptyList()
        vm.uiState.value shouldBe ReactionListUIState.Loading
        vm.fetchReactedUsers() // the tab's request is gone: a fresh one, from page one
        verify(builder, times(2)).build()
        source.reads.size shouldBe 2
    }

    test("reactionList: sessionEnded_dropsResultOfReadInFlight") {
        val vm = vm()
        vm.fetchReactedUsers()
        server(0, "a")
        vm.fetchReactedUsers() // page two in flight
        logout()
        source.reads[1].trySend(TwoPhaseResult.Cached(listOf(reaction("old"))))
        server(1, "old")
        vm.reactedUsers.value shouldBe emptyList()
        vm.uiState.value shouldBe ReactionListUIState.Loading
    }

    test("reactionList: sessionEnded_dropsResultOfReadInFlight — plain read") {
        val repo = ReactionListRepositoryImpl(source)
        val held = HeldReactionsUseCase(repo)
        val store = ViewModelStore()
        stores += store
        val vm = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CometChatReactionListViewModel(held, RemoveReactionUseCase(repo), enableListeners = true) as T
        })[CometChatReactionListViewModel::class.java]
        vm.setBaseMessage(MockFactory.createTextMessage(id = 1L))
        vm.setReactionsRequestBuilder(builder)
        vm.fetchReactedUsers()
        held.calls.size shouldBe 1
        source.reads.size shouldBe 0 // the plain path, not the two-phase one
        logout()
        held.calls[0].resume(Result.success(listOf(reaction("old"))))
        idle()
        vm.reactedUsers.value shouldBe emptyList()
        vm.uiState.value shouldBe ReactionListUIState.Loading
    }

    test("reactionList: staleSessionEnded_isIgnored") {
        val vm = vm()
        vm.fetchReactedUsers()
        server(0, "a")
        OfflineHub.publishForTest(OfflineHubEvent.SessionEnded(OfflineHub.gen))
        idle()
        ids(vm) shouldBe listOf("a")
        vm.uiState.value shouldBe ReactionListUIState.Content
    }

    test("reactionList: cacheReset_clearsThenReloadsOnce") {
        val vm = vm()
        vm.fetchReactedUsers()
        server(0, "a", "b")
        cacheReset()
        vm.reactedUsers.value shouldBe emptyList()
        source.reads.size shouldBe 2
        server(1, "c")
        ids(vm) shouldBe listOf("c")
        source.reads.size shouldBe 2
    }

    test("reactionList: cacheReset_reloadFailureOverRows_isQuiet") {
        val vm = vm()
        val errors = mutableListOf<CometChatException>()
        CoroutineScope(dispatcher).launch { vm.loadErrorWithRows.collect { errors += it } }
        vm.fetchReactedUsers()
        server(0, "a")
        cacheReset()
        source.reads[1].trySend(TwoPhaseResult.Cached(listOf(reaction("a"))))
        source.reads[1].trySend(TwoPhaseResult.Error(CometChatException("ERR_SERVER", "boom")))
        source.reads[1].close()
        idle()
        ids(vm) shouldBe listOf("a")
        vm.uiState.value shouldBe ReactionListUIState.Content
        errors shouldBe emptyList()
    }

    test("reactionList: offlineOff_noChange") {
        OfflineGate.testOverride = false
        source.single = listOf(reaction("a"))
        val vm = vm()
        vm.fetchReactedUsers()
        idle()
        logout()
        cacheReset()
        ids(vm) shouldBe listOf("a")
        vm.uiState.value shouldBe ReactionListUIState.Content
        source.singleCalls shouldBe 1
        source.reads.size shouldBe 0
    }
})
