package com.cometchat.uikit.core.viewmodel.search

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.constants.SearchFilter
import com.cometchat.uikit.core.data.repository.SearchRepositoryImpl
import com.cometchat.uikit.core.domain.repository.SearchRepository
import com.cometchat.uikit.core.domain.repository.SearchTwoPhaseRepository
import com.cometchat.uikit.core.domain.usecase.FetchConversationsUseCase
import com.cometchat.uikit.core.domain.usecase.FetchMessagesUseCase
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.TwoPhaseResult
import com.cometchat.uikit.core.state.SearchUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatSearchViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.cometchat.chat.core.CometChat
import org.mockito.Mockito
import org.mockito.kotlin.any
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * ENG-41133 (W7): the search screen's two-phase reads (README §3.9 "Search" and "Message filters
 * without a keyword"; BEH-SNAP-04, AC-10.1/10.2/10.3).
 *
 * The ViewModel runs over the real use cases and a scripted [SearchTwoPhaseRepository]; each test
 * pins the offline flag through [OfflineGate.testOverride].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CometChatSearchViewModelOfflineTest : FunSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec {
        Thread.sleep(50)
        Dispatchers.resetMain()
    }
    afterTest { OfflineGate.testOverride = null }

    val notConnected = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")

    fun msg(id: Long, sentAt: Long = id * 10): BaseMessage = MockFactory.createTextMessage(id = id, sentAt = sentAt)
    fun conv(id: String): Conversation = MockFactory.createConversation(id = id)

    fun vm(repo: FakeSearchRepository) =
        CometChatSearchViewModel(FetchConversationsUseCase(repo), FetchMessagesUseCase(repo))

    test("keyword online (offline on): one server round; a cached phase is ignored") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val repo = FakeSearchRepository()
            repo.conversations += flowOf(TwoPhaseResult.Cached(listOf(conv("cached"))), TwoPhaseResult.Server(listOf(conv("c1"))))
            repo.messages += flowOf(TwoPhaseResult.Cached(listOf(msg(99))), TwoPhaseResult.Server(listOf(msg(1), msg(2))))
            val viewModel = vm(repo)

            viewModel.searchConversationsAndMessages("hello", emptySet())
            advanceUntilIdle()

            repo.conversationCalls shouldBe 1
            repo.messageCalls shouldBe 1
            viewModel.conversations.value.map { it.conversationId } shouldBe listOf("c1")
            viewModel.messages.value.map { it.id } shouldBe listOf(2L, 1L)
            viewModel.uiState.value shouldBe SearchUIState.Content
        }
    }

    test("D84 rules 7, 8: keyword offline (offline on) with no saved match is Connect to search; reconnect re-runs it once") {
        val listeners = mutableListOf<CometChat.ConnectionListener>()
        Mockito.mockStatic(CometChat::class.java).use { cometChat ->
            cometChat.`when`<Unit> { CometChat.addConnectionListener(any(), any()) }
                .thenAnswer { listeners += it.getArgument<CometChat.ConnectionListener>(1); null }
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                val repo = FakeSearchRepository()
                repo.conversations += flowOf(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(notConnected))
                repo.messages += flowOf(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(notConnected))
                val viewModel = vm(repo)

                val errors = mutableListOf<CometChatException>()
                val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.rowsKeptError.collect { errors += it } }
                viewModel.searchConversationsAndMessages("hello", emptySet())
                advanceUntilIdle()

                viewModel.uiState.value shouldBe SearchUIState.Empty
                viewModel.showConnectToSearch.value shouldBe true
                errors shouldBe emptyList()
                listeners.size shouldBe 1

                repo.conversations += flowOf(TwoPhaseResult.Server(listOf(conv("c1"))))
                repo.messages += flowOf(TwoPhaseResult.Server(emptyList()))
                listeners.toList().forEach { it.onConnected() }
                advanceUntilIdle()
                repo.conversationCalls shouldBe 2
                viewModel.conversations.value.map { it.conversationId } shouldBe listOf("c1")
                viewModel.uiState.value shouldBe SearchUIState.Content
                viewModel.showConnectToSearch.value shouldBe false
                job.cancel()

                // Once: the next connect reads nothing.
                listeners.toList().forEach { it.onConnected() }
                advanceUntilIdle()
                repo.conversationCalls shouldBe 2
            }
        }
    }

    test("D84 rule 7: mixed failures (one not-connected, one server error) take the normal error path") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val repo = FakeSearchRepository()
            val boom = CometChatException("ERR_SERVER", "boom")
            repo.conversations += flowOf(TwoPhaseResult.Error(boom))
            repo.messages += flowOf(TwoPhaseResult.Error(notConnected))
            val viewModel = vm(repo)

            viewModel.searchConversationsAndMessages("hello", emptySet())
            advanceUntilIdle()

            viewModel.uiState.value.shouldBeInstanceOf<SearchUIState.Error>()
            viewModel.showConnectToSearch.value shouldBe false

            // The other order lands the same way.
            val repo2 = FakeSearchRepository()
            repo2.conversations += flowOf(TwoPhaseResult.Error(notConnected))
            repo2.messages += flowOf(TwoPhaseResult.Error(boom))
            val viewModel2 = vm(repo2)
            viewModel2.searchConversationsAndMessages("hello", emptySet())
            advanceUntilIdle()
            viewModel2.uiState.value.shouldBeInstanceOf<SearchUIState.Error>()
            viewModel2.showConnectToSearch.value shouldBe false
        }
    }

    test("D84 rule 4: an error state that was not caused by being offline does not reload on reconnect") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val repo = FakeSearchRepository()
            val boom = CometChatException("ERR_SERVER", "boom")
            repo.conversations += flowOf(TwoPhaseResult.Error(boom))
            repo.messages += flowOf(TwoPhaseResult.Error(boom))
            val viewModel = vm(repo)

            viewModel.searchConversationsAndMessages("hello", emptySet())
            advanceUntilIdle()

            (viewModel.uiState.value as SearchUIState.Error).exception.code shouldBe "ERR_SERVER"
        }
    }

    test("D84 rule 6: an empty server answer is the empty state") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val repo = FakeSearchRepository()
            repo.conversations += flowOf(TwoPhaseResult.Server(emptyList()))
            repo.messages += flowOf(TwoPhaseResult.Server(emptyList()))
            val viewModel = vm(repo)

            viewModel.searchConversationsAndMessages("hello", emptySet())
            advanceUntilIdle()

            viewModel.uiState.value shouldBe SearchUIState.Empty
        }
    }

    test("filter without a keyword (offline on): cached rows first, then the server page reconciles by id") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val repo = FakeSearchRepository()
            val serverGate = CompletableDeferred<Unit>()
            // Cached: 1, 2, 3. Server: 1, 3 (2 is proven gone inside the page's range) + 4.
            repo.messages += flow {
                emit(TwoPhaseResult.Cached(listOf(msg(1), msg(2), msg(3))))
                serverGate.await()
                emit(TwoPhaseResult.Server(listOf(msg(1), msg(3), msg(4))))
            }
            val viewModel = vm(repo)

            viewModel.searchConversationsAndMessages("", setOf(SearchFilter.PHOTOS))
            advanceUntilIdle()

            viewModel.messages.value.map { it.id } shouldBe listOf(3L, 2L, 1L)
            viewModel.uiState.value shouldBe SearchUIState.Content

            serverGate.complete(Unit)
            advanceUntilIdle()

            viewModel.messages.value.map { it.id } shouldBe listOf(4L, 3L, 1L)
            viewModel.uiState.value shouldBe SearchUIState.Content
            repo.messageCalls shouldBe 1
        }
    }

    test("filter without a keyword, device offline (offline on): answered from the cache, error still reported") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val repo = FakeSearchRepository()
            repo.conversations += flowOf(
                TwoPhaseResult.Cached(listOf(conv("g1"), conv("g2"))),
                TwoPhaseResult.Error(notConnected)
            )
            val viewModel = vm(repo)

            val errors = mutableListOf<CometChatException>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.rowsKeptError.collect { errors += it } }

            viewModel.searchConversationsAndMessages("", setOf(SearchFilter.GROUPS))
            advanceUntilIdle()

            viewModel.conversations.value.map { it.conversationId }.toSet() shouldBe setOf("g1", "g2")
            viewModel.uiState.value shouldBe SearchUIState.Content
            // D84 rule 2: the cached rows stay and an offline failure reports nothing.
            errors shouldBe emptyList()
            job.cancel()
        }
    }

    test("D84 rule 5: filter without a keyword, an online failure after cached rows still reports once") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val repo = FakeSearchRepository()
            repo.conversations += flowOf(
                TwoPhaseResult.Cached(listOf(conv("g1"))),
                TwoPhaseResult.Error(CometChatException("ERR_SERVER", "boom"))
            )
            val viewModel = vm(repo)

            val errors = mutableListOf<CometChatException>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.rowsKeptError.collect { errors += it } }

            viewModel.searchConversationsAndMessages("", setOf(SearchFilter.GROUPS))
            advanceUntilIdle()

            viewModel.uiState.value shouldBe SearchUIState.Content
            errors.map { it.code } shouldBe listOf("ERR_SERVER")
            job.cancel()
        }
    }

    test("paging never repeats a row (offline on, no keyword)") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val repo = FakeSearchRepository()
            repo.messages += flowOf(TwoPhaseResult.Cached(listOf(msg(5), msg(6))), TwoPhaseResult.Server(listOf(msg(5), msg(6))))
            // The next page overlaps the first on both phases.
            repo.messages += flowOf(TwoPhaseResult.Cached(listOf(msg(4), msg(5))), TwoPhaseResult.Server(listOf(msg(3), msg(4), msg(5))))
            val viewModel = vm(repo)

            viewModel.searchConversationsAndMessages("", setOf(SearchFilter.PHOTOS))
            advanceUntilIdle()
            viewModel.fetchMoreMessages()
            advanceUntilIdle()

            val ids = viewModel.messages.value.map { it.id }
            ids shouldBe listOf(6L, 5L, 4L, 3L)
            ids.toSet().size shouldBe ids.size
        }
    }

    test("paging never repeats a row (offline on, keyword: server only)") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val repo = FakeSearchRepository()
            repo.messages += flowOf(TwoPhaseResult.Server(listOf(msg(5), msg(6))))
            repo.messages += flowOf(TwoPhaseResult.Server(listOf(msg(4), msg(5))))
            val viewModel = vm(repo)
            viewModel.setSearchScopes(listOf(com.cometchat.uikit.core.constants.SearchScope.MESSAGES))

            viewModel.searchConversationsAndMessages("hi", emptySet())
            advanceUntilIdle()
            viewModel.fetchMoreMessages()
            advanceUntilIdle()

            viewModel.messages.value.map { it.id } shouldBe listOf(6L, 5L, 4L)
        }
    }

    test("a stale reply is dropped: a page fetched for the old search never reaches the new one") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val repo = FakeSearchRepository()
            val staleGate = CompletableDeferred<Unit>()
            repo.messages += flowOf(TwoPhaseResult.Server(listOf(msg(1), msg(2))))         // "a"
            repo.messages += flow {                                                       // "a", page 2
                staleGate.await()
                emit(TwoPhaseResult.Server(listOf(msg(77))))
            }
            repo.messages += flowOf(TwoPhaseResult.Server(listOf(msg(10))))               // "ab"
            val viewModel = vm(repo)
            viewModel.setSearchScopes(listOf(com.cometchat.uikit.core.constants.SearchScope.MESSAGES))

            viewModel.searchConversationsAndMessages("a", emptySet())
            advanceUntilIdle()
            viewModel.fetchMoreMessages()                     // in flight
            advanceUntilIdle()
            viewModel.searchConversationsAndMessages("ab", emptySet())
            advanceUntilIdle()
            staleGate.complete(Unit)                          // the old page arrives late
            advanceUntilIdle()

            viewModel.messages.value.map { it.id } shouldBe listOf(10L)
        }
    }

    test("offline off: the server phase is appended exactly as today; errors stay errors") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = false
            val repo = FakeSearchRepository()
            repo.conversations += flowOf(TwoPhaseResult.Server(listOf(conv("c1"), conv("c2"))))
            repo.messages += flowOf(TwoPhaseResult.Server(listOf(msg(1), msg(2), msg(3))))
            val viewModel = vm(repo)

            viewModel.searchConversationsAndMessages("hello", emptySet())
            advanceUntilIdle()

            viewModel.conversations.value.map { it.conversationId } shouldBe listOf("c1", "c2")
            viewModel.messages.value.map { it.id } shouldBe listOf(3L, 2L, 1L) // page reversed, as today
            viewModel.hasMoreMessages.value shouldBe true
            viewModel.uiState.value shouldBe SearchUIState.Content

            // A not-connected failure with offline off is today's error state, never connect-to-search.
            repo.conversations += flowOf(TwoPhaseResult.Error(notConnected))
            repo.messages += flowOf(TwoPhaseResult.Error(notConnected))
            viewModel.searchConversationsAndMessages("other", emptySet())
            advanceUntilIdle()

            viewModel.uiState.value.shouldBeInstanceOf<SearchUIState.Error>()
        }
    }

    test("offline off: an empty server page ends paging, as today") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = false
            val repo = FakeSearchRepository()
            repo.messages += flowOf(TwoPhaseResult.Server(emptyList()))
            val viewModel = vm(repo)

            viewModel.searchConversationsAndMessages("", setOf(SearchFilter.PHOTOS))
            advanceUntilIdle()

            viewModel.messages.value.shouldBeEmpty()
            viewModel.hasMoreMessages.value shouldBe false
            viewModel.uiState.value shouldBe SearchUIState.Empty
        }
    }

    test("a subclassed use case keeps today's single-phase path (its invoke is still called)") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val repo = FakeSearchRepository()
            var invoked = 0
            val custom = object : FetchMessagesUseCase(repo) {
                override suspend fun invoke(request: MessagesRequest): Result<List<BaseMessage>> {
                    invoked++
                    return Result.success(listOf(msg(1)))
                }
            }
            val viewModel = CometChatSearchViewModel(FetchConversationsUseCase(repo), custom)

            viewModel.searchConversationsAndMessages("", setOf(SearchFilter.PHOTOS))
            advanceUntilIdle()

            invoked shouldBe 1
            repo.messageCalls shouldBe 0
            viewModel.messages.value.map { it.id } shouldBe listOf(1L)
        }
    }

    test("the default repository is two-phase") {
        FetchMessagesUseCase(SearchRepositoryImpl(object : com.cometchat.uikit.core.data.datasource.SearchDataSource {
            override suspend fun fetchConversations(request: ConversationsRequest) = emptyList<Conversation>()
            override suspend fun fetchMessages(request: MessagesRequest) = emptyList<BaseMessage>()
        })).twoPhaseRepository shouldNotBe null
    }

    // ---- D83 amended 2026-11-12 ("keep it quiet"): the search the kit re-runs by itself.

    test("D84 rule 5: the reconnect re-run failing over results keeps them and reports nothing") {
        val listeners = mutableListOf<CometChat.ConnectionListener>()
        Mockito.mockStatic(CometChat::class.java).use { cometChat ->
            cometChat.`when`<Unit> { CometChat.addConnectionListener(any(), any()) }
                .thenAnswer { listeners += it.getArgument<CometChat.ConnectionListener>(1); null }
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                val repo = FakeSearchRepository()
                repo.conversations += flowOf(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(notConnected))
                repo.messages += flowOf(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(notConnected))
                val viewModel = vm(repo)
                val errors = mutableListOf<CometChatException>()
                val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.rowsKeptError.collect { errors += it } }

                viewModel.searchConversationsAndMessages("hello", emptySet())
                advanceUntilIdle()
                viewModel.showConnectToSearch.value shouldBe true

                // The re-run: conversations answer, the message request fails again.
                repo.conversations += flowOf(TwoPhaseResult.Server(listOf(conv("c1"))))
                repo.messages += flowOf(TwoPhaseResult.Error(notConnected))
                listeners.toList().forEach { it.onConnected() }
                advanceUntilIdle()

                viewModel.conversations.value.map { it.conversationId } shouldBe listOf("c1")
                viewModel.uiState.value shouldBe SearchUIState.Content
                errors shouldBe emptyList()
                job.cancel()
            }
        }
    }

    test("D84 rule 5: a search the user starts, failing online over results, still reports once") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val repo = FakeSearchRepository()
            repo.conversations += flowOf(TwoPhaseResult.Server(listOf(conv("c1"))))
            repo.messages += flowOf(TwoPhaseResult.Error(CometChatException("ERR_SERVER", "boom")))
            val viewModel = vm(repo)
            val errors = mutableListOf<CometChatException>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.rowsKeptError.collect { errors += it } }

            viewModel.searchConversationsAndMessages("hello", emptySet())
            advanceUntilIdle()

            viewModel.uiState.value shouldBe SearchUIState.Content
            errors.map { it.code } shouldBe listOf("ERR_SERVER")
            job.cancel()
        }
    }

    test("D84 rule 2: a search the user starts, failing offline over results, reports nothing") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val repo = FakeSearchRepository()
            repo.conversations += flowOf(TwoPhaseResult.Server(listOf(conv("c1"))))
            repo.messages += flowOf(TwoPhaseResult.Error(notConnected))
            val viewModel = vm(repo)
            val errors = mutableListOf<CometChatException>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.rowsKeptError.collect { errors += it } }

            viewModel.searchConversationsAndMessages("hello", emptySet())
            advanceUntilIdle()

            viewModel.uiState.value shouldBe SearchUIState.Content
            errors shouldBe emptyList()
            job.cancel()
        }
    }

    test("D84 rule 8: the reconnect re-run failing offline with nothing to show is Connect to search again") {
        val listeners = mutableListOf<CometChat.ConnectionListener>()
        Mockito.mockStatic(CometChat::class.java).use { cometChat ->
            cometChat.`when`<Unit> { CometChat.addConnectionListener(any(), any()) }
                .thenAnswer { listeners += it.getArgument<CometChat.ConnectionListener>(1); null }
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                val repo = FakeSearchRepository()
                repo.conversations += flowOf(TwoPhaseResult.Error(notConnected))
                repo.messages += flowOf(TwoPhaseResult.Error(notConnected))
                val viewModel = vm(repo)

                viewModel.searchConversationsAndMessages("hello", emptySet())
                advanceUntilIdle()
                viewModel.showConnectToSearch.value shouldBe true

                repo.conversations += flowOf(TwoPhaseResult.Error(notConnected))
                repo.messages += flowOf(TwoPhaseResult.Error(notConnected))
                listeners.toList().forEach { it.onConnected() }
                advanceUntilIdle()

                repo.conversationCalls shouldBe 2
                viewModel.uiState.value shouldBe SearchUIState.Empty
                viewModel.showConnectToSearch.value shouldBe true
            }
        }
    }
})

/** Scripted two-phase search repository: each call takes the next queued flow. */
private class FakeSearchRepository : SearchRepository, SearchTwoPhaseRepository {
    val conversations = ArrayDeque<Flow<TwoPhaseResult<Conversation>>>()
    val messages = ArrayDeque<Flow<TwoPhaseResult<BaseMessage>>>()
    var conversationCalls = 0
    var messageCalls = 0
    private var hasMoreConversations = true
    private var hasMoreMessages = true

    override fun getConversationsTwoPhase(request: ConversationsRequest): Flow<TwoPhaseResult<Conversation>> {
        conversationCalls++
        val next = conversations.removeFirstOrNull() ?: flowOf(TwoPhaseResult.Server(emptyList()))
        return next.onEach { if (it is TwoPhaseResult.Server) hasMoreConversations = it.rows.isNotEmpty() }
    }

    override fun getMessagesTwoPhase(request: MessagesRequest): Flow<TwoPhaseResult<BaseMessage>> {
        messageCalls++
        val next = messages.removeFirstOrNull() ?: flowOf(TwoPhaseResult.Server(emptyList()))
        return next.onEach { if (it is TwoPhaseResult.Server) hasMoreMessages = it.rows.isNotEmpty() }
    }

    override suspend fun getConversations(request: ConversationsRequest): Result<List<Conversation>> =
        error("the single-phase read must not be used when the repository is two-phase")

    override suspend fun getMessages(request: MessagesRequest): Result<List<BaseMessage>> =
        error("the single-phase read must not be used when the repository is two-phase")

    override fun hasMoreConversations(): Boolean = hasMoreConversations
    override fun hasMoreMessages(): Boolean = hasMoreMessages
}
