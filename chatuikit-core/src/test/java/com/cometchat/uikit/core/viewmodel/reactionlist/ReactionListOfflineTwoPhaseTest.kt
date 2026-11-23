package com.cometchat.uikit.core.viewmodel.reactionlist

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Reaction
import com.cometchat.uikit.core.data.datasource.ReactionListDataSource
import com.cometchat.uikit.core.data.datasource.ReactionListTwoPhaseDataSource
import com.cometchat.uikit.core.data.repository.ReactionListRepositoryImpl
import com.cometchat.uikit.core.domain.usecase.FetchReactionsUseCase
import com.cometchat.uikit.core.domain.usecase.RemoveReactionUseCase
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.TwoPhaseResult
import com.cometchat.uikit.core.state.ReactionListUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatReactionListViewModel
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * ENG-41134 — reactions list on the SDK 5.1.0 two-phase `fetchNext(ReactionFetchListener)`
 * (BEH-SNAP-03, README §3.9), through the new two-phase data source / repository seam.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*ReactionListOfflineTwoPhaseTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReactionListOfflineTwoPhaseTest : FunSpec({

    isolationMode = IsolationMode.SingleInstance

    val testDispatcher = UnconfinedTestDispatcher()
    val notConnected = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")

    /** A data source whose every two-phase read is a channel the test drives. */
    class FakeSource : ReactionListDataSource, ReactionListTwoPhaseDataSource {
        val reads = mutableListOf<Channel<TwoPhaseResult<Reaction>>>()
        var singlePhaseCalls = 0

        override suspend fun fetchReactions(request: ReactionsRequest): Result<List<Reaction>> {
            singlePhaseCalls++
            return Result.success(emptyList())
        }

        override suspend fun removeReaction(messageId: Long, emoji: String): Result<BaseMessage> =
            Result.failure(CometChatException("ERR", "unused"))

        override fun fetchReactionsTwoPhase(request: ReactionsRequest): Flow<TwoPhaseResult<Reaction>> =
            Channel<TwoPhaseResult<Reaction>>(Channel.UNLIMITED).also { reads += it }.consumeAsFlow()
    }

    fun reaction(id: String, uid: String = "u-$id"): Reaction = Reaction().apply {
        setReactionId(id)
        setUid(uid)
        setReaction("👍")
        setMessageId(1L)
    }

    fun builder(): ReactionsRequest.ReactionsRequestBuilder {
        val b = mock<ReactionsRequest.ReactionsRequestBuilder>()
        whenever(b.build()).thenAnswer { mock<ReactionsRequest>() }
        return b
    }

    fun newVm(source: FakeSource, fetch: FetchReactionsUseCase? = null): CometChatReactionListViewModel {
        val repo = ReactionListRepositoryImpl(source)
        val vm = CometChatReactionListViewModel(fetch ?: FetchReactionsUseCase(repo), RemoveReactionUseCase(repo), enableListeners = false)
        vm.setBaseMessage(MockFactory.createTextMessage(id = 1L))
        return vm
    }

    fun ids(vm: CometChatReactionListViewModel) = vm.reactedUsers.value.map { it.reactionId }

    beforeSpec { Dispatchers.setMain(testDispatcher) }

    afterSpec {
        Thread.sleep(50)
        Dispatchers.resetMain()
    }

    afterTest { OfflineGate.testOverride = null }

    test("warmup - absorb leaked exceptions") {
        try { runTest(testDispatcher) { } } catch (_: Exception) { }
    }

    test("offline on — cached page is the first paint, then the server page reconciles with no duplicate") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val source = FakeSource()
            val vm = newVm(source)
            vm.fetchReactedUsers(null, builder())

            source.reads.last().send(TwoPhaseResult.Cached(listOf(reaction("a"), reaction("b"))))
            ids(vm) shouldBe listOf("a", "b")
            vm.uiState.value shouldBe ReactionListUIState.Content

            source.reads.last().send(TwoPhaseResult.Server(listOf(reaction("b"), reaction("a"), reaction("c"))))
            source.reads.last().close()
            ids(vm) shouldBe listOf("b", "a", "c")

            // Next page: a re-delivered row is never repeated.
            vm.fetchReactedUsers(null, builder())
            source.reads.last().send(TwoPhaseResult.Server(listOf(reaction("c"), reaction("d"))))
            source.reads.last().close()
            ids(vm) shouldBe listOf("b", "a", "c", "d")
        }
    }

    test("offline on — an empty cached page neither paints nor ends the list") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val source = FakeSource()
            val vm = newVm(source)
            vm.fetchReactedUsers(null, builder())
            source.reads.last().send(TwoPhaseResult.Cached(emptyList()))
            vm.uiState.value shouldBe ReactionListUIState.Loading
            source.reads.last().send(TwoPhaseResult.Server(listOf(reaction("a"))))
            source.reads.last().close()
            ids(vm) shouldBe listOf("a")

            vm.fetchReactedUsers(null, builder())
            source.reads.size shouldBe 2
        }
    }

    test("offline on — not-connected keeps the rows shown (no error screen over them)") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val source = FakeSource()
            val vm = newVm(source)
            vm.fetchReactedUsers(null, builder())
            source.reads.last().send(TwoPhaseResult.Server(listOf(reaction("a"), reaction("b"))))
            source.reads.last().close()

            vm.fetchReactedUsers(null, builder())
            source.reads.last().send(TwoPhaseResult.Cached(emptyList()))
            source.reads.last().send(TwoPhaseResult.Error(notConnected))
            source.reads.last().close()
            ids(vm) shouldBe listOf("a", "b")
            vm.uiState.value shouldBe ReactionListUIState.Content
        }
    }

    /** Runs [block] with `CometChat` connection listeners captured (the D83 reconnect read). */
    suspend fun withConnectionListeners(block: suspend (MutableList<CometChat.ConnectionListener>) -> Unit) {
        val listeners = mutableListOf<CometChat.ConnectionListener>()
        val cc = Mockito.mockStatic(CometChat::class.java)
        try {
            cc.`when`<Unit> { CometChat.addConnectionListener(any(), any()) }
                .thenAnswer { listeners += it.getArgument<CometChat.ConnectionListener>(1); null }
            block(listeners)
        } finally {
            cc.close()
        }
    }

    test("D84 rule 3 — offline on, nothing cached and not connected shows the offline state") {
        runTest(testDispatcher) {
            withConnectionListeners {
                OfflineGate.testOverride = true
                val source = FakeSource()
                val vm = newVm(source)
                vm.fetchReactedUsers(null, builder())
                source.reads.last().send(TwoPhaseResult.Cached(emptyList()))
                source.reads.last().send(TwoPhaseResult.Error(notConnected))
                source.reads.last().close()
                (vm.uiState.value as ReactionListUIState.Error).exception shouldBe notConnected
                vm.connectToLoad.value shouldBe true
                ids(vm) shouldBe emptyList()
            }
        }
    }

    test("D84 rule 2 — offline on, an offline error with cached rows keeps them and reports nothing") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val source = FakeSource()
            val vm = newVm(source)
            val reported = mutableListOf<CometChatException>()
            val job = launch { vm.loadErrorWithRows.collect { reported += it } }
            vm.fetchReactedUsers(null, builder())
            source.reads.last().send(TwoPhaseResult.Cached(listOf(reaction("a"))))
            source.reads.last().send(TwoPhaseResult.Error(notConnected))
            source.reads.last().close()
            ids(vm) shouldBe listOf("a")
            vm.uiState.value shouldBe ReactionListUIState.Content
            reported shouldBe emptyList()
            job.cancel()
        }
    }

    test("D84 rule 5 — offline on, an online error with cached rows keeps them and still reports to onError") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val source = FakeSource()
            val vm = newVm(source)
            val reported = mutableListOf<CometChatException>()
            val job = launch { vm.loadErrorWithRows.collect { reported += it } }
            val boom = CometChatException("ERR_SERVER", "boom")
            vm.fetchReactedUsers(null, builder())
            source.reads.last().send(TwoPhaseResult.Cached(listOf(reaction("a"))))
            source.reads.last().send(TwoPhaseResult.Error(boom))
            source.reads.last().close()
            ids(vm) shouldBe listOf("a")
            vm.uiState.value shouldBe ReactionListUIState.Content
            reported shouldBe listOf(boom)
            job.cancel()
        }
    }

    test("offline on — an empty server page is a blank list under Content, as offline off (the sheet stays open)") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val source = FakeSource()
            val vm = newVm(source)
            vm.fetchReactedUsers(null, builder())
            source.reads.last().send(TwoPhaseResult.Cached(listOf(reaction("a"))))
            source.reads.last().send(TwoPhaseResult.Server(emptyList()))
            source.reads.last().close()
            ids(vm) shouldBe emptyList()
            vm.uiState.value shouldBe ReactionListUIState.Content
        }
    }

    test("D83 — offline on, a failed load-more keeps the rows and does not report to onError") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val source = FakeSource()
            val vm = newVm(source)
            val reported = mutableListOf<CometChatException>()
            val job = launch { vm.loadErrorWithRows.collect { reported += it } }
            vm.fetchReactedUsers(null, builder())
            source.reads.last().send(TwoPhaseResult.Server(listOf(reaction("a"))))
            source.reads.last().close()

            vm.fetchReactedUsers(null, builder())
            source.reads.last().send(TwoPhaseResult.Cached(listOf(reaction("b"))))
            source.reads.last().send(TwoPhaseResult.Error(notConnected))
            source.reads.last().close()
            ids(vm) shouldBe listOf("a", "b")
            vm.uiState.value shouldBe ReactionListUIState.Content
            reported shouldBe emptyList()
            job.cancel()
        }
    }

    test("D83 — offline on, an error with rows raised before the view collects arrives exactly once") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val source = FakeSource()
            val vm = newVm(source)
            val boom = CometChatException("ERR_SERVER", "boom")
            vm.fetchReactedUsers(null, builder())
            source.reads.last().send(TwoPhaseResult.Cached(listOf(reaction("a"))))
            source.reads.last().send(TwoPhaseResult.Error(boom))
            source.reads.last().close()

            val first = mutableListOf<CometChatException>()
            val job1 = launch { vm.loadErrorWithRows.collect { first += it } }
            first shouldBe listOf(boom)
            job1.cancel()
            // A new collector (rotation / recomposition) gets no repeat.
            val second = mutableListOf<CometChatException>()
            val job2 = launch { vm.loadErrorWithRows.collect { second += it } }
            second shouldBe emptyList()
            job2.cancel()
        }
    }

    test("D83 — a dismissed list (cancelReconnectRefill) reads nothing on reconnect") {
        runTest(testDispatcher) {
            withConnectionListeners { listeners ->
                OfflineGate.testOverride = true
                val source = FakeSource()
                val vm = newVm(source)
                vm.fetchReactedUsers(null, builder())
                source.reads.last().send(TwoPhaseResult.Error(notConnected))
                source.reads.last().close()
                vm.cancelReconnectRefill()
                listeners.toList().forEach { it.onConnected() }
                source.reads.size shouldBe 1
            }
        }
    }

    test("D83 — a new message (setBaseMessage) cancels the old message's reconnect read") {
        runTest(testDispatcher) {
            withConnectionListeners { listeners ->
                OfflineGate.testOverride = true
                val source = FakeSource()
                val vm = newVm(source)
                vm.fetchReactedUsers(null, builder())
                source.reads.last().send(TwoPhaseResult.Error(notConnected))
                source.reads.last().close()
                vm.setBaseMessage(MockFactory.createTextMessage(id = 2L))
                listeners.toList().forEach { it.onConnected() }
                source.reads.size shouldBe 1
            }
        }
    }

    test("D83 — offline on, an error view because offline reads exactly once on reconnect") {
        runTest(testDispatcher) {
            withConnectionListeners { listeners ->
                OfflineGate.testOverride = true
                val source = FakeSource()
                val vm = newVm(source)
                vm.fetchReactedUsers(null, builder())
                source.reads.last().send(TwoPhaseResult.Error(notConnected))
                source.reads.last().close()
                vm.uiState.value.shouldBeInstanceOf<ReactionListUIState.Error>()
                source.reads.size shouldBe 1

                listeners.toList().forEach { it.onConnected() }
                source.reads.size shouldBe 2
                source.reads.last().send(TwoPhaseResult.Server(listOf(reaction("a"))))
                source.reads.last().close()
                ids(vm) shouldBe listOf("a")
                vm.uiState.value shouldBe ReactionListUIState.Content

                listeners.toList().forEach { it.onConnected() }
                source.reads.size shouldBe 2
            }
        }
    }

    test("D83 — offline on, another error shows the error view and arms no reconnect read") {
        runTest(testDispatcher) {
            withConnectionListeners { listeners ->
                OfflineGate.testOverride = true
                val source = FakeSource()
                val vm = newVm(source)
                vm.fetchReactedUsers(null, builder())
                source.reads.last().send(TwoPhaseResult.Error(CometChatException("ERR_SERVER", "boom")))
                source.reads.last().close()
                vm.uiState.value.shouldBeInstanceOf<ReactionListUIState.Error>()
                listeners shouldBe emptyList()
            }
        }
    }

    test("offline on — another error with nothing shown is today's error state") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val source = FakeSource()
            val vm = newVm(source)
            vm.fetchReactedUsers(null, builder())
            source.reads.last().send(TwoPhaseResult.Error(CometChatException("ERR_SERVER", "boom")))
            source.reads.last().close()
            vm.uiState.value.shouldBeInstanceOf<ReactionListUIState.Error>()
        }
    }

    test("offline on — a stale generation is dropped (a read landing after clearCache)") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val source = FakeSource()
            val vm = newVm(source)
            vm.fetchReactedUsers(null, builder())
            val stale = source.reads.last()

            vm.clearCache()
            vm.fetchReactedUsers(null, builder()) // not blocked by the stale read
            source.reads.size shouldBe 2
            source.reads.last().send(TwoPhaseResult.Server(listOf(reaction("new"))))
            source.reads.last().close()
            ids(vm) shouldBe listOf("new")

            stale.send(TwoPhaseResult.Cached(listOf(reaction("old1"))))
            stale.send(TwoPhaseResult.Server(listOf(reaction("old2"))))
            stale.close()
            ids(vm) shouldBe listOf("new")
        }
    }

    test("offline off — today's single-phase read; the two-phase seam is not used") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = false
            val source = FakeSource()
            val vm = newVm(source)
            vm.fetchReactedUsers(null, builder())
            source.singlePhaseCalls shouldBe 1
            source.reads.size shouldBe 0
            vm.uiState.value shouldBe ReactionListUIState.Content
        }
    }

    test("offline on — a custom FetchReactionsUseCase subclass keeps today's path (its invoke is honoured)") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val source = FakeSource()
            var customCalls = 0
            val custom = object : FetchReactionsUseCase(ReactionListRepositoryImpl(source)) {
                override suspend fun invoke(request: ReactionsRequest): Result<List<Reaction>> {
                    customCalls++
                    return Result.success(listOf(reaction("custom")))
                }
            }
            val vm = newVm(source, custom)
            vm.fetchReactedUsers(null, builder())
            customCalls shouldBe 1
            source.reads.size shouldBe 0
            ids(vm) shouldBe listOf("custom")
        }
    }

    // ---- D83 amended 2026-11-12 ("keep it quiet"): the read the kit starts on reconnect.

    test("D83 amended — the reconnect read failing over cached rows keeps them and reports nothing") {
        runTest(testDispatcher) {
            withConnectionListeners { listeners ->
                OfflineGate.testOverride = true
                val source = FakeSource()
                val vm = newVm(source)
                val reported = mutableListOf<CometChatException>()
                val job = launch { vm.loadErrorWithRows.collect { reported += it } }
                vm.fetchReactedUsers(null, builder())
                source.reads.last().send(TwoPhaseResult.Error(notConnected))
                source.reads.last().close()
                vm.uiState.value.shouldBeInstanceOf<ReactionListUIState.Error>()

                listeners.toList().forEach { it.onConnected() }
                source.reads.size shouldBe 2
                source.reads.last().send(TwoPhaseResult.Cached(listOf(reaction("a"))))
                source.reads.last().send(TwoPhaseResult.Error(notConnected))
                source.reads.last().close()
                ids(vm) shouldBe listOf("a")
                vm.uiState.value shouldBe ReactionListUIState.Content
                reported shouldBe emptyList()
                job.cancel()
            }
        }
    }

    test("D83 amended — the reconnect read failing with nothing shown is the error view again") {
        runTest(testDispatcher) {
            withConnectionListeners { listeners ->
                OfflineGate.testOverride = true
                val source = FakeSource()
                val vm = newVm(source)
                vm.fetchReactedUsers(null, builder())
                source.reads.last().send(TwoPhaseResult.Error(notConnected))
                source.reads.last().close()

                listeners.toList().forEach { it.onConnected() }
                source.reads.size shouldBe 2
                val other = CometChatException("ERR_SERVER", "boom")
                source.reads.last().send(TwoPhaseResult.Error(other))
                source.reads.last().close()
                (vm.uiState.value as ReactionListUIState.Error).exception shouldBe other
            }
        }
    }
})
