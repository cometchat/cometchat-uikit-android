package com.cometchat.uikit.core.offline

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.model.SurroundingMessagesResult
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.domain.repository.MessageListTwoPhaseRepository
import com.cometchat.uikit.core.state.MessageListUIState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

/**
 * W2 (ENG-41087) reads over a fake [MessageListReadHost]: open-newest-first, two-phase pages with
 * the windowed reconcile, the "Connect to load more" edges, jumps, threads, the generation guard and
 * `refreshNewestWindow`. Reconcile itself is covered by `OfflineListReconcileTest`; these tests
 * prove the reads feed it correctly and drive the list state around it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageListOfflineReadsTest : FunSpec({

    fun msg(id: Long, sentAt: Long = id, updatedAt: Long = 0L, localId: String? = null, uid: String = "peer"): BaseMessage =
        TextMessage("peer", "m$id", CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            this.sentAt = sentAt
            this.updatedAt = updatedAt
            this.localMessageId = localId
            this.sender = User().apply { this.uid = uid }
        }

    fun pending(localId: String, sentAt: Long): BaseMessage = msg(0L, sentAt = sentAt, localId = localId, uid = "me")

    fun ids(rows: List<BaseMessage>) = rows.map { if (it.id > 0) it.id else "l:${it.localMessageId}" }

    val notConnected = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")

    /** Two-phase repository whose phases are scripted per call. */
    class FakeTwoPhase : MessageListTwoPhaseRepository {
        val previous = ArrayDeque<Flow<TwoPhaseResult<BaseMessage>>>()
        val next = ArrayDeque<Flow<TwoPhaseResult<BaseMessage>>>()
        val previousCalls = mutableListOf<Unit>()
        val nextAnchors = mutableListOf<Long>()
        var log: MutableList<String>? = null
        override fun fetchPreviousTwoPhase(): Flow<TwoPhaseResult<BaseMessage>> {
            previousCalls += Unit
            log?.add("fetchPrevious")
            return previous.removeFirst()
        }
        override fun fetchNextTwoPhase(fromMessageId: Long): Flow<TwoPhaseResult<BaseMessage>> {
            nextAnchors += fromMessageId
            return next.removeFirst()
        }
    }

    class FakeHost(
        override val scope: CoroutineScope,
        override val repository: MessageListRepository,
        override val twoPhase: MessageListTwoPhaseRepository?
    ) : MessageListReadHost {
        val log = mutableListOf<String>()
        override var listGen = 0L
        override var chatKey: Any? = "peer"
        override val aliases = AliasMap()
        override val readState = MessageListOfflineReadState()
        override var rows: List<BaseMessage> = emptyList()
            set(value) { field = value; log += "rows:${value.map { it.id }}" }
        override var uiState: MessageListUIState = MessageListUIState.Loading
        override var hasMorePrevious = true
        override var hasMoreNew = false
        private val progress = MutableStateFlow(false)
        override var inProgress: Boolean
            get() = progress.value
            set(value) { progress.value = value }
        override suspend fun awaitIdle() { progress.first { !it } }
        override var gotoMessageId = 0L
        override var startFromUnread = false
        override var lastReadMessageId = -1L
        override fun conversationTarget(): Pair<String, String>? = "peer" to CometChatConstants.RECEIVER_TYPE_USER
        override fun applyConversation(conversation: Conversation) {
            lastReadMessageId = conversation.lastReadMessageId
            log += "conversation"
        }
        override fun checkConversationSummary(unreadCount: Int) { log += "summary:$unreadCount" }
        override var anchored: MessageListAnchoredReads? = null
        /** True: goToMessage runs the jump hook, as the ViewModel does. */
        var routeJumps = false
        val jumps = mutableListOf<Pair<Long, Boolean>>()
        override fun goToMessage(messageId: Long, highlight: Boolean) {
            jumps += messageId to highlight
            log += "goTo:$messageId:$highlight"
            if (routeJumps) MessageListOfflineReads.jump(this, messageId, highlight)
        }
        var latest = -1L
        override fun setLatestMessageId(messageId: Long) { latest = messageId }
        var firstFetchCount = 0
        override suspend fun completeFirstFetch(scrollToBottom: Boolean) {
            firstFetchCount++
            log += if (scrollToBottom) "firstFetch" else "firstFetch:noScroll"
        }
        var newerPages = 0
        override fun afterNewerPage() { newerPages++ }
        override fun markUnreadAnchor() { log += "anchor" }
        var scrolledTo: Pair<Long, Boolean>? = null
        var scrollCount = 0
        override fun scrollToMessage(messageId: Long, highlight: Boolean) {
            scrolledTo = messageId to highlight
            scrollCount++
        }
    }

    /** The D60 cache-allowed reads, scripted per call. */
    class FakeAnchored : MessageListAnchoredReads {
        var conversation: Result<SettledRead<Conversation>> = Result.failure(CometChatException("ERR", "unset"))
        var target: Result<BaseMessage> = Result.failure(CometChatException("ERR", "unset"))
        var targetGate: CompletableDeferred<Unit>? = null
        val previous = ArrayDeque<Flow<TwoPhaseResult<BaseMessage>>>()
        val next = ArrayDeque<Flow<TwoPhaseResult<BaseMessage>>>()
        val calls = mutableListOf<String>()
        override suspend fun getConversation(id: String, type: String): Result<SettledRead<Conversation>> {
            calls += "conversation"
            return conversation
        }
        override suspend fun getMessage(messageId: Long): Result<BaseMessage> {
            calls += "target:$messageId"
            targetGate?.await()
            return target
        }
        override fun fetchPreviousAnchored(messageId: Long): Flow<TwoPhaseResult<BaseMessage>> {
            calls += "previous:$messageId"
            return previous.removeFirst()
        }
        override fun fetchNextAnchored(messageId: Long): Flow<TwoPhaseResult<BaseMessage>> {
            calls += "next:$messageId"
            return next.removeFirst()
        }
    }

    fun conversation(lastRead: Long = -1L, unread: Int = 0) =
        Conversation("peer_me", CometChatConstants.CONVERSATION_TYPE_USER).apply {
            lastReadMessageId = lastRead
            unreadMessageCount = unread
        }

    fun repo(conv: Result<Conversation> = Result.success(conversation())): MessageListRepository = mock {
        onBlocking { getConversation(any(), any()) } doReturn conv
    }

    fun TestScope.host(
        twoPhase: FakeTwoPhase = FakeTwoPhase(),
        repository: MessageListRepository = repo()
    ) = FakeHost(this, repository, twoPhase).also { twoPhase.log = it.log }

    fun phases(vararg r: TwoPhaseResult<BaseMessage>): Flow<TwoPhaseResult<BaseMessage>> = flowOf(*r)

    /** Every error the list reports to the app's onError while rows stay shown (D83). */
    fun TestScope.readErrors(h: FakeHost): List<CometChatException> {
        val errors = mutableListOf<CometChatException>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { h.readState.readError.collect { errors += it } }
        return errors
    }

    // ---- Open ---------------------------------------------------------------------------------

    test("open: the newest page is read first, then the jump to unread") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(8), msg(9))), TwoPhaseResult.Server(listOf(msg(8), msg(9), msg(10))))
            val h = host(tp, repo(Result.success(conversation(lastRead = 4L, unread = 6))))
            h.startFromUnread = true

            MessageListOfflineReads.open(h) shouldBe true
            advanceUntilIdle()

            val read = h.log.indexOf("rows:[8, 9, 10]")
            val jump = h.log.indexOf("goTo:4:false")
            (read in 0 until jump) shouldBe true
            h.jumps shouldBe listOf(4L to false)
            h.firstFetchCount shouldBe 0 // the jump owns the landing, as today
            h.log.contains("summary:6") shouldBe true
        }
    }

    test("open: gotoMessageId wins over unread") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(9), msg(10))))
            val h = host(tp, repo(Result.success(conversation(lastRead = 4L, unread = 6))))
            h.startFromUnread = true
            h.gotoMessageId = 7L

            MessageListOfflineReads.open(h)
            advanceUntilIdle()

            h.jumps shouldBe listOf(7L to true)
        }
    }

    test("open: no jump lands on the newest window with today's first-fetch effects") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(9), msg(10))))
            val h = host(tp)

            MessageListOfflineReads.open(h)
            advanceUntilIdle()

            h.jumps shouldBe emptyList()
            h.firstFetchCount shouldBe 1
            h.latest shouldBe 10L
            h.uiState shouldBe MessageListUIState.Loaded
        }
    }

    test("open: getConversation failing offline stays on the newest window with no error") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(9), msg(10))), TwoPhaseResult.Error(notConnected))
            val h = host(tp, repo(Result.failure(notConnected)))
            h.startFromUnread = true

            MessageListOfflineReads.open(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(9L, 10L)
            h.uiState shouldBe MessageListUIState.Loaded
            h.jumps shouldBe emptyList()
            h.readState.olderBlocked.value shouldBe true
        }
    }

    // ---- Pages --------------------------------------------------------------------------------

    test("cached first paint hides the loader, then the server reconciles with no duplicates") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val tp = FakeTwoPhase()
            tp.previous += flow {
                emit(TwoPhaseResult.Cached(listOf(msg(1), msg(2), msg(3))))
                gate.await()
                emit(TwoPhaseResult.Server(listOf(msg(1), msg(2, updatedAt = 5), msg(3), msg(4))))
            }
            val h = host(tp)

            MessageListOfflineReads.fetchOlder(h) shouldBe true
            runCurrent()
            ids(h.rows) shouldBe listOf(1L, 2L, 3L)
            h.uiState shouldBe MessageListUIState.Loaded
            h.inProgress shouldBe true

            gate.complete(Unit)
            advanceUntilIdle()
            ids(h.rows) shouldBe listOf(1L, 2L, 3L, 4L)
            h.rows.first { it.id == 2L }.updatedAt shouldBe 5L
            h.inProgress shouldBe false
            h.hasMorePrevious shouldBe true
            h.firstFetchCount shouldBe 1
        }
    }

    test("proven omission removes a cached row only inside the server page's range") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(
                TwoPhaseResult.Cached(listOf(msg(10), msg(11), msg(12))),
                TwoPhaseResult.Server(listOf(msg(10), msg(12)))
            )
            val h = host(tp)
            h.rows = listOf(msg(20), msg(21)) // shown before; outside the page's range

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(10L, 12L, 20L, 21L)
        }
    }

    test("a pending row survives the server phase and sorts last; its acked copy collapses into it") {
        runTest {
            val tp = FakeTwoPhase()
            val acked = msg(13, sentAt = 13, localId = "L1", uid = "me")
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(10))), TwoPhaseResult.Server(listOf(msg(10), acked)))
            val h = host(tp)
            h.rows = listOf(msg(11), pending("L1", sentAt = 12), pending("L2", sentAt = 14))

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(10L, 11L, 13L, "l:L2")
        }
    }

    test("a server copy without localMessageId, landing before the ack, collapses into the pending row by muid") {
        runTest {
            val tp = FakeTwoPhase()
            // REST server rows carry no localMessageId; only the sender's muid links them.
            val serverCopy = msg(13, sentAt = 13, uid = "me").apply { muid = "mu1" }
            tp.next += phases(TwoPhaseResult.Server(listOf(serverCopy)))
            val h = host(tp)
            h.rows = listOf(msg(11), pending("L1", sentAt = 12).apply { muid = "mu1" }, pending("L2", sentAt = 14).apply { muid = "mu2" })
            h.hasMoreNew = true

            MessageListOfflineReads.fetchNewer(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(11L, 13L, "l:L2")
            h.rows.count { it.muid == "mu1" } shouldBe 1
        }
    }

    test("an empty cached page does not end history; an empty online server page does") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(notConnected))
            tp.previous += phases(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Server(emptyList()))
            val h = host(tp)
            h.rows = listOf(msg(5))

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()
            h.hasMorePrevious shouldBe true

            MessageListOfflineReads.retryLoadMore(h)
            advanceUntilIdle()
            h.hasMorePrevious shouldBe false
            ids(h.rows) shouldBe listOf(5L)
        }
    }

    test("offline older page keeps the rows and raises the footer; scrolling does not re-read; tap retries") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(3))), TwoPhaseResult.Error(notConnected))
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(1), msg(2), msg(3))))
            val h = host(tp)
            h.rows = listOf(msg(4), msg(5))
            h.uiState = MessageListUIState.Loaded

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()
            ids(h.rows) shouldBe listOf(3L, 4L, 5L)
            h.uiState shouldBe MessageListUIState.Loaded
            h.readState.olderBlocked.value shouldBe true
            h.inProgress shouldBe false

            MessageListOfflineReads.fetchOlder(h) shouldBe true
            advanceUntilIdle()
            tp.previousCalls.size shouldBe 1

            MessageListOfflineReads.retryLoadMore(h)
            advanceUntilIdle()
            tp.previousCalls.size shouldBe 2
            h.readState.olderBlocked.value shouldBe false
            ids(h.rows) shouldBe listOf(1L, 2L, 3L, 4L, 5L)
        }
    }

    test("a non-network error with nothing shown gets today's error state, and the error view's retry reads again") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(CometChatException("ERR_SERVER", "500")))
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(1))))
            val h = host(tp)

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()
            (h.uiState is MessageListUIState.Error) shouldBe true

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()
            ids(h.rows) shouldBe listOf(1L)
            h.uiState shouldBe MessageListUIState.Loaded
        }
    }

    test("newer page anchors on the newest row with a server id, never on an unsent tail") {
        runTest {
            val tp = FakeTwoPhase()
            tp.next += phases(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Server(listOf(msg(6), msg(7))))
            val h = host(tp)
            h.rows = listOf(msg(4), msg(5), pending("L1", sentAt = 9))
            h.hasMoreNew = true

            MessageListOfflineReads.fetchNewer(h)
            advanceUntilIdle()

            tp.nextAnchors shouldBe listOf(5L)
            ids(h.rows) shouldBe listOf(4L, 5L, 6L, 7L, "l:L1")
            h.hasMoreNew shouldBe true
            h.latest shouldBe 7L
            h.newerPages shouldBe 1
        }
    }

    test("offline newer page raises the newer edge and keeps the rows") {
        runTest {
            val tp = FakeTwoPhase()
            tp.next += phases(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(notConnected))
            val h = host(tp)
            h.rows = listOf(msg(4), msg(5))
            h.hasMoreNew = true

            MessageListOfflineReads.fetchNewer(h)
            advanceUntilIdle()

            h.readState.newerBlocked.value shouldBe true
            ids(h.rows) shouldBe listOf(4L, 5L)
            h.hasMoreNew shouldBe true
        }
    }

    test("a fresh window (after clear) drops a stale newer edge from the old window") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(9), msg(10))))
            val h = host(tp)
            h.readState.newerBlocked.value = true

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()

            h.readState.newerBlocked.value shouldBe false
            ids(h.rows) shouldBe listOf(9L, 10L)
        }
    }

    // ---- Jump ---------------------------------------------------------------------------------

    test("goToMessage offline keeps the rows: no loader, no error, no blanking") {
        runTest {
            val repository = mock<MessageListRepository> {
                onBlocking { getMessage(any()) } doReturn Result.failure(notConnected)
            }
            val h = host(repository = repository)
            h.rows = listOf(msg(9), msg(10))
            h.uiState = MessageListUIState.Loaded

            val errors = readErrors(h)
            MessageListOfflineReads.jump(h, 3L, true) shouldBe true
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(9L, 10L)
            h.uiState shouldBe MessageListUIState.Loaded
            h.scrolledTo shouldBe null
            errors shouldBe emptyList() // D84 rule 2: offline over rows, onError hears nothing
        }
    }

    test("D84 rule 5: goToMessage failing online over rows keeps them and reports once") {
        runTest {
            val boom = CometChatException("ERR_SERVER", "boom")
            val repository = mock<MessageListRepository> {
                onBlocking { getMessage(any()) } doReturn Result.failure(boom)
            }
            val h = host(repository = repository)
            h.rows = listOf(msg(9), msg(10))
            h.uiState = MessageListUIState.Loaded

            val errors = readErrors(h)
            MessageListOfflineReads.jump(h, 3L, true) shouldBe true
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(9L, 10L)
            h.uiState shouldBe MessageListUIState.Loaded
            errors shouldBe listOf(boom)
        }
    }

    test("D83: failed paging reads offline keep today's footer only, no onError (older, newer)") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(notConnected))
            tp.next += phases(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(notConnected))
            val h = host(tp)
            h.rows = listOf(msg(4), msg(5))
            h.uiState = MessageListUIState.Loaded
            h.hasMoreNew = true
            val errors = readErrors(h)

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()
            MessageListOfflineReads.fetchNewer(h)
            advanceUntilIdle()

            h.readState.olderBlocked.value shouldBe true
            h.readState.newerBlocked.value shouldBe true
            h.uiState shouldBe MessageListUIState.Loaded
            errors shouldBe emptyList()
        }
    }

    test("goToMessage online replaces the window, keeps unsent rows, rebuilds the cursor from the oldest row") {
        runTest {
            val window = SurroundingMessagesResult(listOf(msg(2)), msg(3), listOf(msg(4)), hasMorePrevious = true, hasMoreNext = true)
            val repository = mock<MessageListRepository> {
                onBlocking { getMessage(any()) } doReturn Result.success(msg(3))
                onBlocking { fetchSurroundingMessages(any()) } doReturn Result.success(window)
            }
            val h = host(repository = repository)
            h.rows = listOf(msg(9), msg(10), pending("L1", sentAt = 11))

            MessageListOfflineReads.jump(h, 3L, true)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(2L, 3L, 4L, "l:L1")
            h.hasMoreNew shouldBe true
            h.scrolledTo shouldBe (3L to true)
            verify(repository).rebuildRequestFromMessageId(2L)
            h.uiState shouldBe MessageListUIState.Loaded
        }
    }

    test("a jump that opens the chat reads the newest page first; offline the failed jump leaves that window") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(9), msg(10))), TwoPhaseResult.Error(notConnected))
            val repository = mock<MessageListRepository> {
                onBlocking { getMessage(any()) } doReturn Result.failure(notConnected)
            }
            val h = host(tp, repository)

            MessageListOfflineReads.jump(h, 3L, true)
            advanceUntilIdle()

            tp.previousCalls.size shouldBe 1
            ids(h.rows) shouldBe listOf(9L, 10L)
            h.uiState shouldBe MessageListUIState.Loaded
            h.inProgress shouldBe false
        }
    }

    test("D83: a jump that opens the chat with nothing cached and no network settles once on the error view, no empty flash") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(notConnected))
            val repository = mock<MessageListRepository> {
                onBlocking { getMessage(any()) } doReturn Result.failure(notConnected)
            }
            val h = host(tp, repository)
            val states = mutableListOf<MessageListUIState>()
            val recording = object : MessageListReadHost by h {
                override var uiState: MessageListUIState
                    get() = h.uiState
                    set(value) { states += value; h.uiState = value }
            }

            MessageListOfflineReads.jump(recording, 3L, true)
            advanceUntilIdle()

            states.none { it is MessageListUIState.Empty } shouldBe true
            states.count { it is MessageListUIState.Error } shouldBe 1 // no flash before the jump settles
            h.uiState shouldBe MessageListUIState.Error(notConnected)
        }
    }

    // ---- Opening jump: online today's server jump, offline from the cache (D60, ENG-41156) ------

    fun offlineHalf(vararg rows: BaseMessage) = phases(TwoPhaseResult.Cached(rows.toList()), TwoPhaseResult.Error(notConnected))

    /** A repository whose server reads are unreachable (the SDK's not-connected error). */
    fun offlineRepo(conv: Result<Conversation> = Result.failure(notConnected)): MessageListRepository = mock {
        onBlocking { getConversation(any(), any()) } doReturn conv
        onBlocking { getMessage(any()) } doReturn Result.failure(notConnected)
    }

    test("D60 offline open: cached conversation and cached target show the anchored window around last-read, no highlight") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(9), msg(10), pending("L1", sentAt = 11))), TwoPhaseResult.Error(notConnected))
            val repository = offlineRepo()
            val h = host(tp, repository)
            val reads = FakeAnchored().apply {
                conversation = Result.success(SettledRead(conversation(lastRead = 4L, unread = 6), fromServer = false))
                target = Result.success(msg(4))
                previous += offlineHalf(msg(2), msg(3))
                next += offlineHalf(msg(5), pending("L1", sentAt = 11))
            }
            h.anchored = reads
            h.routeJumps = true
            h.startFromUnread = true

            MessageListOfflineReads.open(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(2L, 3L, 4L, 5L, "l:L1") // queued row kept once, last
            h.scrolledTo shouldBe (4L to false)
            h.scrollCount shouldBe 1
            reads.calls shouldBe listOf("conversation", "target:4", "previous:4", "next:4")
            verify(repository).getMessage(4L) // today's server read first; the cache only on not-connected
            h.uiState shouldBe MessageListUIState.Loaded
            h.hasMorePrevious shouldBe true
            h.hasMoreNew shouldBe true
            h.readState.olderBlocked.value shouldBe true
            h.readState.newerBlocked.value shouldBe true
            h.log.contains("anchor") shouldBe true
            h.log.any { it.startsWith("summary:") } shouldBe false // no summary on a cached conversation
            verify(repository).rebuildRequestFromMessageId(4L) // older paging resumes at the target
            h.readState.newerFrontier shouldBe 4L // newer paging resumes at the target
        }
    }

    test("D60 offline open at gotoMessageId with a cached target shows the window and highlights") {
        runTest {
            for (viaOpen in listOf(true, false)) {
                val tp = FakeTwoPhase()
                tp.previous += offlineHalf(msg(9), msg(10))
                val h = host(tp, offlineRepo())
                h.anchored = FakeAnchored().apply {
                    conversation = Result.failure(notConnected)
                    target = Result.success(msg(7))
                    previous += offlineHalf(msg(6))
                    next += offlineHalf(msg(8))
                }
                h.routeJumps = true
                h.gotoMessageId = 7L

                // Both kits call goToMessage directly for a goto id; the open path reaches it too.
                if (viaOpen) MessageListOfflineReads.open(h) else MessageListOfflineReads.jump(h, 7L, true)
                advanceUntilIdle()

                ids(h.rows) shouldBe listOf(6L, 7L, 8L)
                h.scrolledTo shouldBe (7L to true)
                h.uiState shouldBe MessageListUIState.Loaded
                h.inProgress shouldBe false
            }
        }
    }

    test("D60 offline open with the target not cached stays on the newest window: no error, no spinner") {
        runTest {
            // Target unresolved, or resolved with both cached halves empty.
            for (targetCached in listOf(false, true)) {
                val tp = FakeTwoPhase()
                tp.previous += offlineHalf(msg(9), msg(10))
                val h = host(tp, offlineRepo())
                val reads = FakeAnchored().apply {
                    conversation = Result.success(SettledRead(conversation(lastRead = 4L, unread = 6), fromServer = false))
                    target = if (targetCached) Result.success(msg(4)) else Result.failure(notConnected)
                    previous += offlineHalf()
                    next += offlineHalf()
                }
                h.anchored = reads
                h.routeJumps = true
                h.startFromUnread = true

                MessageListOfflineReads.open(h)
                advanceUntilIdle()

                ids(h.rows) shouldBe listOf(9L, 10L)
                h.uiState shouldBe MessageListUIState.Loaded
                h.scrolledTo shouldBe null
                h.readState.olderBlocked.value shouldBe true // the newest window's own edge, untouched
                h.readState.newerFrontier shouldBe null
                reads.calls.contains("previous:4") shouldBe targetCached
            }
        }
    }

    test("D84 rule 2: an offline open with cached rows and an opening jump reports nothing to onError") {
        runTest {
            for (targetCached in listOf(false, true)) {
                val tp = FakeTwoPhase()
                tp.previous += offlineHalf(msg(9), msg(10))
                val h = host(tp, offlineRepo())
                h.anchored = FakeAnchored().apply {
                    conversation = Result.success(SettledRead(conversation(lastRead = 4L, unread = 6), fromServer = false))
                    target = if (targetCached) Result.success(msg(4)) else Result.failure(notConnected)
                    previous += offlineHalf(msg(3))
                    next += offlineHalf(msg(5))
                }
                h.routeJumps = true
                h.startFromUnread = true
                val errors = readErrors(h)

                MessageListOfflineReads.open(h)
                advanceUntilIdle()

                h.uiState shouldBe MessageListUIState.Loaded
                errors shouldBe emptyList()
            }
        }
    }

    test("D60 online: today's server jump runs even when a cached half would reach past the server page") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(55), msg(56))), TwoPhaseResult.Server(listOf(msg(56), msg(57))))
            val window = SurroundingMessagesResult(listOf(msg(3)), msg(4), listOf(msg(5)), hasMorePrevious = true, hasMoreNext = true)
            val repository = mock<MessageListRepository> {
                onBlocking { getConversation(any(), any()) } doReturn Result.success(conversation(lastRead = 4L, unread = 6))
                onBlocking { getMessage(any()) } doReturn Result.success(msg(4))
                onBlocking { fetchSurroundingMessages(any()) } doReturn Result.success(window)
            }
            val h = host(tp, repository)
            val reads = FakeAnchored().apply {
                conversation = Result.success(SettledRead(conversation(lastRead = 4L, unread = 6), fromServer = true))
                target = Result.success(msg(4))
                // A cached newer half that skips the gap into the newest segment: never read online.
                previous += offlineHalf(msg(3))
                next += offlineHalf(msg(5), msg(55), msg(56))
            }
            h.anchored = reads
            h.routeJumps = true
            h.startFromUnread = true

            MessageListOfflineReads.open(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(3L, 4L, 5L) // today's server window, nothing from the newest segment
            reads.calls shouldBe listOf("conversation")
            h.scrolledTo shouldBe (4L to false)
            h.hasMoreNew shouldBe true
            h.readState.newerFrontier shouldBe null
            verify(repository).fetchSurroundingMessages(4L)
            verify(repository).rebuildRequestFromMessageId(3L)
            h.log.contains("summary:6") shouldBe true

            // Paging is today's: newer from the window's newest row.
            tp.next += phases(TwoPhaseResult.Server(listOf(msg(6))))
            MessageListOfflineReads.fetchNewer(h)
            advanceUntilIdle()
            tp.nextAnchors shouldBe listOf(5L)
            ids(h.rows) shouldBe listOf(3L, 4L, 5L, 6L)
        }
    }

    test("D60 online: a failed server getMessage leaves the newest window untouched, no cache read") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(9), msg(10))))
            val repository = mock<MessageListRepository> {
                onBlocking { getConversation(any(), any()) } doReturn Result.success(conversation())
                onBlocking { getMessage(any()) } doReturn Result.failure(CometChatException("ERR_MESSAGE_NOT_FOUND", "gone"))
            }
            val h = host(tp, repository)
            val reads = FakeAnchored().apply {
                conversation = Result.success(SettledRead(conversation(), fromServer = true))
            }
            h.anchored = reads
            h.routeJumps = true
            h.gotoMessageId = 7L

            MessageListOfflineReads.open(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(9L, 10L)
            h.uiState shouldBe MessageListUIState.Loaded
            h.scrolledTo shouldBe null
            reads.calls shouldBe listOf("conversation")
            verify(repository, never()).fetchSurroundingMessages(any())
        }
    }

    test("D60 offline jump over a cache gap: once online, paging fills the gap from the target outward") {
        runTest {
            val tp = FakeTwoPhase()
            val repository = offlineRepo()
            val h = host(tp, repository)
            h.rows = listOf(msg(90), msg(91)) // the newest window
            h.anchored = FakeAnchored().apply {
                target = Result.success(msg(4))
                previous += offlineHalf(msg(2), msg(3))
                next += offlineHalf(msg(5), msg(50), msg(51)) // the SDK skipped 6..49
            }
            h.readState.openJumpGen = MessageListOfflineReads.currentGen(h)

            MessageListOfflineReads.jump(h, 4L, false)
            advanceUntilIdle()
            ids(h.rows) shouldBe listOf(2L, 3L, 4L, 5L, 50L, 51L)
            verify(repository).rebuildRequestFromMessageId(4L)

            // Reconnected: the tap on the edge retries older (from the request rebuilt at the target).
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(1), msg(2), msg(3))))
            MessageListOfflineReads.retryLoadMore(h)
            advanceUntilIdle()
            ids(h.rows) shouldBe listOf(1L, 2L, 3L, 4L, 5L, 50L, 51L)

            // Newer pages read from the target, not from 51, until a page reaches the newest row shown.
            tp.next += phases(TwoPhaseResult.Cached(listOf(msg(5), msg(50), msg(51))), TwoPhaseResult.Server(listOf(msg(5), msg(6), msg(7))))
            MessageListOfflineReads.fetchNewer(h)
            advanceUntilIdle()
            ids(h.rows) shouldBe listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 50L, 51L)
            h.readState.newerFrontier shouldBe 7L
            h.hasMoreNew shouldBe true

            tp.next += phases(TwoPhaseResult.Server(listOf(msg(49), msg(50), msg(51))))
            MessageListOfflineReads.fetchNewer(h)
            advanceUntilIdle()
            h.readState.newerFrontier shouldBe null

            tp.next += phases(TwoPhaseResult.Server(emptyList()))
            MessageListOfflineReads.fetchNewer(h)
            advanceUntilIdle()
            tp.nextAnchors shouldBe listOf(4L, 7L, 51L)
            ids(h.rows) shouldBe listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 49L, 50L, 51L)
            h.hasMoreNew shouldBe false
        }
    }

    test("D60 a jump from a chat already shown keeps today's server read") {
        runTest {
            val window = SurroundingMessagesResult(listOf(msg(2)), msg(3), listOf(msg(4)), hasMorePrevious = true, hasMoreNext = true)
            val repository = mock<MessageListRepository> {
                onBlocking { getMessage(any()) } doReturn Result.success(msg(3))
                onBlocking { fetchSurroundingMessages(any()) } doReturn Result.success(window)
            }
            val h = host(repository = repository)
            val reads = FakeAnchored()
            h.anchored = reads
            h.rows = listOf(msg(9), msg(10))

            MessageListOfflineReads.jump(h, 3L, true)
            advanceUntilIdle()

            reads.calls shouldBe emptyList()
            ids(h.rows) shouldBe listOf(2L, 3L, 4L)
            verify(repository).fetchSurroundingMessages(3L)
        }
    }

    test("D60 a stale gen is dropped: a chat switch mid-jump never touches the list") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val h = host(repository = offlineRepo())
            h.rows = listOf(msg(9), msg(10))
            h.anchored = FakeAnchored().apply {
                target = Result.success(msg(3))
                targetGate = gate
                previous += offlineHalf(msg(2))
                next += offlineHalf(msg(4))
            }
            h.readState.openJumpGen = MessageListOfflineReads.currentGen(h)

            MessageListOfflineReads.jump(h, 3L, true)
            runCurrent()
            h.chatKey = "other"
            h.rows = listOf(msg(50))
            gate.complete(Unit)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(50L)
            h.scrolledTo shouldBe null
        }
    }

    // ---- ENG-41551 D: a jump is a new window --------------------------------------------------

    fun onlineJumpRepo(target: Long): MessageListRepository {
        val window = SurroundingMessagesResult(listOf(msg(target - 1)), msg(target), listOf(msg(target + 1)), hasMorePrevious = true, hasMoreNext = true)
        return mock {
            onBlocking { getMessage(any()) } doReturn Result.success(msg(target))
            onBlocking { fetchSurroundingMessages(any()) } doReturn Result.success(window)
        }
    }

    test("ENG-41551 D: a stale older page that completes after the jump is ignored, and paging resumes") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val tp = FakeTwoPhase()
            tp.previous += flow {
                emit(TwoPhaseResult.Cached(listOf(msg(80), msg(81))))
                gate.await()
                emit(TwoPhaseResult.Server(listOf(msg(80), msg(81))))
            }
            val repository = onlineJumpRepo(3L)
            val h = host(tp, repository)
            h.rows = listOf(msg(90), msg(91))

            MessageListOfflineReads.fetchOlder(h) // older page in flight: its cached phase lands now
            runCurrent()
            ids(h.rows) shouldBe listOf(80L, 81L, 90L, 91L)
            MessageListOfflineReads.jump(h, 3L, true)
            advanceUntilIdle()
            ids(h.rows) shouldBe listOf(2L, 3L, 4L)

            gate.complete(Unit) // the stale server phase answers after the jump
            advanceUntilIdle()
            ids(h.rows) shouldBe listOf(2L, 3L, 4L)
            h.inProgress shouldBe false
            h.firstFetchCount shouldBe 0
            // The placed jump owns the cursor: the stale page never moves it back.
            verify(repository).rebuildRequestFromMessageId(2L)
            verify(repository, never()).rebuildRequestFromMessageId(80L)

            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(1))))
            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()
            ids(h.rows) shouldBe listOf(1L, 2L, 3L, 4L)
        }
    }

    test("ENG-41551 D review: a jump fails while an older page is in flight: the cursor goes back on the oldest row, no gap") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val tp = FakeTwoPhase()
            tp.previous += flow {
                emit(TwoPhaseResult.Cached(listOf(msg(80), msg(81))))
                gate.await()
                emit(TwoPhaseResult.Server(listOf(msg(80), msg(81))))
            }
            val repository = mock<MessageListRepository> {
                onBlocking { getMessage(any()) } doReturn Result.failure(CometChatException("ERR", "boom"))
            }
            val h = host(tp, repository)
            h.rows = listOf(msg(90), msg(91))

            MessageListOfflineReads.fetchOlder(h) // scrolled up: an older page in flight
            runCurrent()
            MessageListOfflineReads.jump(h, 3L, true) // the jump fails; the rows stay
            advanceUntilIdle()
            ids(h.rows) shouldBe listOf(80L, 81L, 90L, 91L)

            gate.complete(Unit) // the dropped page had already advanced the shared cursor
            advanceUntilIdle()
            verify(repository).rebuildRequestFromMessageId(80L)

            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(78), msg(79))))
            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()
            ids(h.rows) shouldBe listOf(78L, 79L, 80L, 81L, 90L, 91L)
        }
    }

    test("ENG-41551 D: a stale newer page never moves the D60 frontier of an offline jump") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val tp = FakeTwoPhase()
            tp.next += flow {
                gate.await()
                emit(TwoPhaseResult.Server(listOf(msg(92), msg(93))))
            }
            val h = host(tp, offlineRepo())
            h.rows = listOf(msg(90), msg(91))
            h.anchored = FakeAnchored().apply {
                target = Result.success(msg(4))
                previous += offlineHalf(msg(3))
                next += offlineHalf(msg(5))
            }

            MessageListOfflineReads.fetchNewer(h) // e.g. the call-activity read, in flight
            runCurrent()
            h.readState.openJumpGen = MessageListOfflineReads.currentGen(h)
            MessageListOfflineReads.jump(h, 4L, false)
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(3L, 4L, 5L)
            h.readState.newerFrontier shouldBe 4L
        }
    }

    test("ENG-41551 D: a jump during the open keeps the conversation bookkeeping; the open never lands over it") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val tp = FakeTwoPhase()
            tp.previous += flow {
                emit(TwoPhaseResult.Cached(listOf(msg(90), msg(91))))
                gate.await()
                emit(TwoPhaseResult.Server(listOf(msg(90), msg(91), msg(92))))
            }
            val window = SurroundingMessagesResult(listOf(msg(2)), msg(3), listOf(msg(4)), hasMorePrevious = true, hasMoreNext = true)
            val repository = mock<MessageListRepository> {
                onBlocking { getConversation(any(), any()) } doReturn Result.success(conversation(lastRead = 4L, unread = 6))
                onBlocking { getMessage(any()) } doReturn Result.success(msg(3))
                onBlocking { fetchSurroundingMessages(any()) } doReturn Result.success(window)
            }
            val h = host(tp, repository)
            h.startFromUnread = true

            MessageListOfflineReads.open(h)
            runCurrent()
            ids(h.rows) shouldBe listOf(90L, 91L) // the cached phase is on screen
            MessageListOfflineReads.jump(h, 3L, true)
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(2L, 3L, 4L)
            h.jumps shouldBe emptyList() // the open's unread jump never ran over the user's
            // The open's first-fetch bookkeeping still runs, without the scroll (review fix).
            h.log.filter { it.startsWith("firstFetch") } shouldBe listOf("firstFetch:noScroll")
            h.lastReadMessageId shouldBe 4L
            h.log.contains("summary:6") shouldBe true
        }
    }

    test("ENG-41551 D: a superseded jump never lands over the newer one") {
        runTest {
            val firstTarget = CompletableDeferred<Unit>()
            val w3 = SurroundingMessagesResult(listOf(msg(2)), msg(3), listOf(msg(4)), hasMorePrevious = true, hasMoreNext = true)
            val w30 = SurroundingMessagesResult(listOf(msg(29)), msg(30), listOf(msg(31)), hasMorePrevious = true, hasMoreNext = true)
            val m3 = msg(3)
            val m30 = msg(30)
            val repository = mock<MessageListRepository> {
                onBlocking { getMessage(3L) } doSuspendableAnswer { firstTarget.await(); Result.success(m3) }
                onBlocking { fetchSurroundingMessages(3L) } doReturn Result.success(w3)
                onBlocking { getMessage(30L) } doReturn Result.success(m30)
                onBlocking { fetchSurroundingMessages(30L) } doReturn Result.success(w30)
            }
            val h = host(FakeTwoPhase(), repository)
            h.rows = listOf(msg(90), msg(91))

            MessageListOfflineReads.jump(h, 3L, true)
            runCurrent()
            MessageListOfflineReads.jump(h, 30L, true)
            advanceUntilIdle()
            firstTarget.complete(Unit)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(29L, 30L, 31L)
            h.scrolledTo shouldBe (30L to true)
        }
    }

    test("ENG-41551 D: a newest refresh in flight before a jump never resets the jumped window") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val tp = FakeTwoPhase()
            tp.previous += flow {
                gate.await()
                emit(TwoPhaseResult.Server(listOf(msg(98), msg(99))))
            }
            val h = host(tp, onlineJumpRepo(3L))
            h.rows = listOf(msg(90), msg(91))

            MessageListOfflineReads.refreshNewest(h)
            runCurrent() // past awaitIdle, reading
            MessageListOfflineReads.jump(h, 3L, true)
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(2L, 3L, 4L)
            h.readState.jumpToLatest.value shouldBe false
            h.inProgress shouldBe false
        }
    }

    test("ENG-41551 D: a new window after clear() drops a jump in flight and frees the read held by a stale page") {
        runTest {
            val stale = CompletableDeferred<Unit>()
            val tp = FakeTwoPhase()
            tp.previous += flow {
                stale.await()
                emit(TwoPhaseResult.Server(listOf(msg(80))))
            }
            val targetGate = CompletableDeferred<Unit>()
            val w3 = SurroundingMessagesResult(listOf(msg(2)), msg(3), listOf(msg(4)), hasMorePrevious = true, hasMoreNext = true)
            val m3 = msg(3)
            val repository = mock<MessageListRepository> {
                onBlocking { getMessage(any()) } doSuspendableAnswer { targetGate.await(); Result.success(m3) }
                onBlocking { fetchSurroundingMessages(any()) } doReturn Result.success(w3)
            }
            val h = host(tp, repository)
            h.rows = listOf(msg(90), msg(91))
            MessageListOfflineReads.fetchOlder(h) // a stale older page holds inProgress
            runCurrent()
            MessageListOfflineReads.jump(h, 3L, true) // and a jump is in flight

            // Scroll to bottom: the kits clear() and fetch the newest page.
            h.rows = emptyList()
            MessageListOfflineReads.startNewWindow(h)
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(98), msg(99))))
            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()
            ids(h.rows) shouldBe listOf(98L, 99L)

            targetGate.complete(Unit)
            stale.complete(Unit)
            advanceUntilIdle()
            ids(h.rows) shouldBe listOf(98L, 99L)
            h.inProgress shouldBe false
            h.log.filter { it.startsWith("firstFetch") } shouldBe listOf("firstFetch")
        }
    }

    test("ENG-41551 D: an older or newer page asked for while the jump is in flight is not read") {
        runTest {
            val targetGate = CompletableDeferred<Unit>()
            val tp = FakeTwoPhase()
            val window = SurroundingMessagesResult(listOf(msg(2)), msg(3), listOf(msg(4)), hasMorePrevious = true, hasMoreNext = true)
            val repository = mock<MessageListRepository> {
                onBlocking { getMessage(any()) } doSuspendableAnswer { targetGate.await(); Result.success(msg(3)) }
                onBlocking { fetchSurroundingMessages(any()) } doReturn Result.success(window)
            }
            val h = host(tp, repository)
            h.rows = listOf(msg(90), msg(91))

            MessageListOfflineReads.jump(h, 3L, true)
            runCurrent()
            MessageListOfflineReads.fetchOlder(h) shouldBe true
            MessageListOfflineReads.fetchNewer(h) shouldBe true
            targetGate.complete(Unit)
            advanceUntilIdle()

            tp.previousCalls.size shouldBe 0
            tp.nextAnchors shouldBe emptyList()
            ids(h.rows) shouldBe listOf(2L, 3L, 4L)
        }
    }

    test("D83 offline open with nothing cached shows the error view; reconnect reads the newest page once and its rows replace it") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += offlineHalf()
            val h = host(tp, offlineRepo())
            val states = mutableListOf<MessageListUIState>()
            val recording = object : MessageListReadHost by h {
                override var uiState: MessageListUIState
                    get() = h.uiState
                    set(value) { states += value; h.uiState = value }
            }

            MessageListOfflineReads.open(recording)
            advanceUntilIdle()

            h.uiState shouldBe MessageListUIState.Error(notConnected)
            states.none { it is MessageListUIState.Empty } shouldBe true
            h.firstFetchCount shouldBe 0

            // Reconnect with an empty list: W5 refreshes the newest window once (D58).
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(1), msg(2))))
            MessageListOfflineReads.refreshNewest(h)
            advanceUntilIdle()

            tp.previousCalls.size shouldBe 2 // the open, then one newest read on reconnect
            ids(h.rows) shouldBe listOf(1L, 2L)
            h.uiState shouldBe MessageListUIState.Loaded
            h.firstFetchCount shouldBe 1 // D58: the first-fetch step runs here, once
        }
    }

    test("D84 rule 2: offline open with cached rows keeps them when the server phase fails offline; onError hears nothing") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += offlineHalf(msg(1), msg(2))
            val h = host(tp, offlineRepo())
            val errors = readErrors(h)
            val states = mutableListOf<MessageListUIState>()
            val recording = object : MessageListReadHost by h {
                override var uiState: MessageListUIState
                    get() = h.uiState
                    set(value) { states += value; h.uiState = value }
            }

            MessageListOfflineReads.open(recording)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(1L, 2L)
            h.uiState shouldBe MessageListUIState.Loaded
            states.none { it is MessageListUIState.Error } shouldBe true
            errors shouldBe emptyList()
            h.readState.olderBlocked.value shouldBe true // the tappable "Connect to load more" edge
        }
    }

    test("D84 rule 5: an open that fails online before the view collects keeps its error until collected, then delivers it exactly once") {
        runTest {
            val boom = CometChatException("ERR_SERVER", "boom")
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(1), msg(2))), TwoPhaseResult.Error(boom))
            val h = host(tp, offlineRepo())

            MessageListOfflineReads.open(h) // nobody collecting yet
            advanceUntilIdle()

            val first = readErrors(h)
            advanceUntilIdle()
            first shouldBe listOf(boom)

            // A re-bound view (rotation, a new collector) never receives it again.
            val second = readErrors(h)
            advanceUntilIdle()
            second shouldBe emptyList()
            first shouldBe listOf(boom)
        }
    }

    test("D84 rule 3: an offline open with nothing cached is an Error state the VM marks as the offline state") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += offlineHalf()
            val h = host(tp, offlineRepo())
            val errors = readErrors(h)

            MessageListOfflineReads.open(h)
            advanceUntilIdle()

            h.uiState shouldBe MessageListUIState.Error(notConnected)
            SearchOffline.isNotConnected((h.uiState as MessageListUIState.Error).exception as CometChatException) shouldBe true
            errors shouldBe emptyList() // onError fires once, from the Error state, not the rows-kept queue
        }
    }

    test("D83: a server error that is not offline, over an empty list, shows the error view without reaching onError twice") {
        runTest {
            val serverError = CometChatException("ERR_SERVER", "boom")
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(serverError))
            val h = host(tp)
            val errors = readErrors(h)

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()

            h.uiState shouldBe MessageListUIState.Error(serverError)
            errors shouldBe emptyList() // the Error state carries it; the kit's onError fires from there
        }
    }

    test("D83: the server phase returning an empty page shows the empty view, no error") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Server(emptyList()))
            val h = host(tp)
            val errors = readErrors(h)

            MessageListOfflineReads.open(h)
            advanceUntilIdle()

            h.rows shouldBe emptyList()
            h.uiState shouldBe MessageListUIState.Empty
            errors shouldBe emptyList()
        }
    }

    test("D83: a reconnect read that fails again leaves the error view up, and a server-empty one settles on the empty view") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += offlineHalf()
            val h = host(tp, offlineRepo())
            MessageListOfflineReads.open(h)
            advanceUntilIdle()
            h.uiState shouldBe MessageListUIState.Error(notConnected)

            tp.previous += offlineHalf()
            MessageListOfflineReads.refreshNewest(h)
            advanceUntilIdle()
            h.uiState shouldBe MessageListUIState.Error(notConnected)

            tp.previous += phases(TwoPhaseResult.Server(emptyList()))
            MessageListOfflineReads.refreshNewest(h)
            advanceUntilIdle()
            h.uiState shouldBe MessageListUIState.Empty
            tp.previousCalls.size shouldBe 3
        }
    }

    test("D83: a thread opened offline with nothing cached shows the error view; the reconnect read fills it") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += offlineHalf()
            val h = host(tp, offlineRepo())
            h.chatKey = Triple("peer", null, 100L)

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()
            h.uiState shouldBe MessageListUIState.Error(notConnected)

            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(101), msg(102))))
            MessageListOfflineReads.refreshNewest(h)
            advanceUntilIdle()
            ids(h.rows) shouldBe listOf(101L, 102L)
            h.uiState shouldBe MessageListUIState.Loaded
        }
    }

    test("D84 rule 2: a thread with cached replies keeps them when the server phase fails offline; onError hears nothing") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += offlineHalf(msg(101))
            val h = host(tp, offlineRepo())
            h.chatKey = Triple("peer", null, 100L)
            val errors = readErrors(h)

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(101L)
            h.uiState shouldBe MessageListUIState.Loaded
            errors shouldBe emptyList()
        }
    }

    test("D84 rule 5: a thread with cached replies keeps them when the server phase fails online; onError hears it once") {
        runTest {
            val boom = CometChatException("ERR_SERVER", "boom")
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(101))), TwoPhaseResult.Error(boom))
            val h = host(tp, offlineRepo())
            h.chatKey = Triple("peer", null, 100L)
            val errors = readErrors(h)

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(101L)
            errors shouldBe listOf(boom)
        }
    }

    // ---- Thread -------------------------------------------------------------------------------

    test("thread reads use the same two-phase path (the repository is configured with the parent)") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(101))), TwoPhaseResult.Server(listOf(msg(101), msg(102))))
            val h = host(tp)
            h.chatKey = Triple("peer", null, 100L)

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(101L, 102L)
            tp.previousCalls.size shouldBe 1
        }
    }

    // ---- Generation guard ---------------------------------------------------------------------

    test("a stale gen is dropped: a chat switch or a session change mid-read never touches the list") {
        runTest {
            for (change in listOf<(FakeHost) -> Unit>({ it.chatKey = "other" }, { it.listGen++ })) {
                val gate = CompletableDeferred<Unit>()
                val tp = FakeTwoPhase()
                tp.previous += flow {
                    gate.await()
                    emit(TwoPhaseResult.Cached(listOf(msg(1))))
                    emit(TwoPhaseResult.Server(listOf(msg(1), msg(2))))
                }
                val h = host(tp)
                MessageListOfflineReads.fetchOlder(h)
                runCurrent()
                change(h)
                h.rows = listOf(msg(50))
                gate.complete(Unit)
                advanceUntilIdle()

                ids(h.rows) shouldBe listOf(50L)
                h.firstFetchCount shouldBe 0
            }
        }
    }

    test("a chat switch clears the old chat's edges") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Error(notConnected))
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(1))))
            val h = host(tp)
            h.rows = listOf(msg(5))
            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()
            h.readState.olderBlocked.value shouldBe true

            h.chatKey = "other"
            h.rows = emptyList()
            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()
            h.readState.olderBlocked.value shouldBe false
        }
    }

    // ---- refreshNewestWindow ------------------------------------------------------------------

    test("refreshNewestWindow: an overlapping page reconciles in place and re-anchors older pagination") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(9))), TwoPhaseResult.Server(listOf(msg(9), msg(10), msg(11))))
            val repository = repo()
            val h = host(tp, repository)
            h.rows = listOf(msg(7), msg(8), msg(9), pending("L1", sentAt = 20))
            h.hasMorePrevious = true

            MessageListOfflineReads.refreshNewest(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(7L, 8L, 9L, 10L, 11L, "l:L1")
            verify(repository).resetRequest()
            verify(repository).rebuildRequestFromMessageId(7L)
            h.hasMorePrevious shouldBe true
            h.hasMoreNew shouldBe false
            h.latest shouldBe 11L
            h.readState.jumpToLatest.value shouldBe true // the newest end moved
        }
    }

    test("refreshNewestWindow: a page that does not overlap resets the list to that window and raises the pill") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(40))), TwoPhaseResult.Server(listOf(msg(50), msg(51))))
            val repository = repo()
            val h = host(tp, repository)
            h.rows = listOf(msg(7), msg(8), pending("L1", sentAt = 60))
            h.hasMoreNew = true

            MessageListOfflineReads.refreshNewest(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(50L, 51L, "l:L1") // the non-overlapping cached 40 was never painted
            h.hasMorePrevious shouldBe true
            h.hasMoreNew shouldBe false
            h.readState.jumpToLatest.value shouldBe true
            verify(repository, never()).rebuildRequestFromMessageId(any())
        }
    }

    test("refreshNewestWindow: a failure keeps the rows and restores the older cursor") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(notConnected))
            val repository = repo()
            val h = host(tp, repository)
            h.rows = listOf(msg(7), msg(8))
            val errors = readErrors(h)

            MessageListOfflineReads.refreshNewest(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(7L, 8L)
            verify(repository).rebuildRequestFromMessageId(7L)
            h.readState.jumpToLatest.value shouldBe false
            h.uiState shouldBe MessageListUIState.Loaded
            // D83 amended 2026-11-12: only the kit starts this read, so it stays quiet over rows.
            errors shouldBe emptyList()
        }
    }

    test("D83 amended: the reconnect read from the error view keeps cached rows quietly when the server fails") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += offlineHalf()
            val h = host(tp, offlineRepo())
            val errors = readErrors(h)
            MessageListOfflineReads.open(h)
            advanceUntilIdle()
            h.uiState shouldBe MessageListUIState.Error(notConnected)
            errors shouldBe emptyList()

            tp.previous += offlineHalf(msg(1), msg(2))
            MessageListOfflineReads.refreshNewest(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(1L, 2L)
            h.uiState shouldBe MessageListUIState.Loaded
            errors shouldBe emptyList()
        }
    }

    test("D84 rule 5: the reload after a cache reset is quiet over rows; the next read the user starts reports") {
        runTest {
            val boom = CometChatException("ERR_SERVER", "boom")
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(1), msg(2))), TwoPhaseResult.Error(boom))
            val h = host(tp, offlineRepo())
            val errors = readErrors(h)

            h.readState.quietNextRead = true
            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()
            h.readState.quietNextRead shouldBe false
            ids(h.rows) shouldBe listOf(1L, 2L)
            h.uiState shouldBe MessageListUIState.Loaded
            errors shouldBe emptyList()

            // The quiet mark covered that read only: a fresh window the user opens reports an online error.
            h.rows = emptyList()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(1), msg(2))), TwoPhaseResult.Error(boom))
            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()
            errors shouldBe listOf(boom)
        }
    }

    test("refreshNewestWindow waits for an in-flight page instead of racing it") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val tp = FakeTwoPhase()
            tp.previous += flow { gate.await(); emit(TwoPhaseResult.Server(listOf(msg(5), msg(6)))) }
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(6), msg(7))))
            val h = host(tp)
            h.rows = listOf(msg(6))

            MessageListOfflineReads.fetchOlder(h)
            runCurrent()
            MessageListOfflineReads.refreshNewest(h)
            runCurrent()
            tp.previousCalls.size shouldBe 1

            gate.complete(Unit)
            advanceUntilIdle()
            tp.previousCalls.size shouldBe 2
            ids(h.rows) shouldBe listOf(5L, 6L, 7L)
        }
    }

    // ---- ENG-41460: unjoined cached page (R2, D13-R2), the offline-only footer (R7), D58 landing (R8) --

    test("R2: online open drops a cached page the server page does not join, and pages from the oldest row shown") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(
                TwoPhaseResult.Cached(listOf(msg(100), msg(101), msg(102))),
                TwoPhaseResult.Server(listOf(msg(350), msg(351), msg(352)))
            )
            val repository = repo()
            val h = host(tp, repository)

            MessageListOfflineReads.open(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(350L, 351L, 352L)
            verify(repository).rebuildRequestFromMessageId(350L)
            h.hasMorePrevious shouldBe true
            h.uiState shouldBe MessageListUIState.Loaded
            h.firstFetchCount shouldBe 1
        }
    }

    test("R2: a cached page the server page joins is kept and the cursor is left alone") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(
                TwoPhaseResult.Cached(listOf(msg(8), msg(9))),
                TwoPhaseResult.Server(listOf(msg(9), msg(10)))
            )
            val repository = repo()
            val h = host(tp, repository)

            MessageListOfflineReads.open(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(8L, 9L, 10L)
            verify(repository, never()).rebuildRequestFromMessageId(any())
        }
    }

    test("R2: dropping an unjoined cached page keeps its unsent rows (BEH-READ-30)") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(
                TwoPhaseResult.Cached(listOf(msg(100), msg(101), pending("L1", sentAt = 400))),
                TwoPhaseResult.Server(listOf(msg(350), msg(351)))
            )
            val repository = repo()
            val h = host(tp, repository)

            MessageListOfflineReads.open(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(350L, 351L, "l:L1")
            verify(repository).rebuildRequestFromMessageId(350L)
        }
    }

    test("R2: offline the cached page stays (no server page to judge it by)") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(100), msg(101))), TwoPhaseResult.Error(notConnected))
            val repository = repo(Result.failure(notConnected))
            val h = host(tp, repository)

            MessageListOfflineReads.open(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(100L, 101L)
            h.readState.olderBlocked.value shouldBe true
            verify(repository, never()).rebuildRequestFromMessageId(any())
        }
    }

    test("R2: an older page drops only its own unjoined cached rows, never the rows already shown") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(
                TwoPhaseResult.Cached(listOf(msg(100), msg(101))),
                TwoPhaseResult.Server(listOf(msg(348), msg(349)))
            )
            val repository = repo()
            val h = host(tp, repository)
            h.rows = listOf(msg(350), msg(351))
            h.uiState = MessageListUIState.Loaded

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(348L, 349L, 350L, 351L)
            verify(repository).rebuildRequestFromMessageId(348L)
        }
    }

    test("R7: a non-network error on an older page keeps the rows shown, no error screen, no footer; the next scroll retries") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(3))), TwoPhaseResult.Error(CometChatException("ERR_SERVER", "500")))
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(1), msg(2), msg(3))))
            val h = host(tp)
            h.rows = listOf(msg(4), msg(5))
            h.uiState = MessageListUIState.Loaded

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(3L, 4L, 5L)
            h.uiState shouldBe MessageListUIState.Loaded
            h.readState.olderBlocked.value shouldBe false

            MessageListOfflineReads.fetchOlder(h)
            advanceUntilIdle()
            tp.previousCalls.size shouldBe 2
            ids(h.rows) shouldBe listOf(1L, 2L, 3L, 4L, 5L)
        }
    }

    test("R7: open with cached rows painted and a 5xx server phase keeps the rows, no error screen, no footer") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(listOf(msg(9), msg(10))), TwoPhaseResult.Error(CometChatException("ERR_SERVER", "500")))
            val h = host(tp)

            MessageListOfflineReads.open(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(9L, 10L)
            h.uiState shouldBe MessageListUIState.Loaded
            h.readState.olderBlocked.value shouldBe false
        }
    }

    test("R7: open with nothing shown and a 5xx is today's error state, no footer") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(CometChatException("ERR_SERVER", "500")))
            val h = host(tp)

            MessageListOfflineReads.open(h)
            advanceUntilIdle()

            h.rows.isEmpty() shouldBe true
            (h.uiState is MessageListUIState.Error) shouldBe true
            h.readState.olderBlocked.value shouldBe false
        }
    }

    test("R7: a non-network error on a newer page keeps the rows with no footer") {
        runTest {
            val tp = FakeTwoPhase()
            tp.next += phases(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(CometChatException("ERR_SERVER", "503")))
            val h = host(tp)
            h.rows = listOf(msg(4), msg(5))
            h.uiState = MessageListUIState.Loaded
            h.hasMoreNew = true

            MessageListOfflineReads.fetchNewer(h)
            advanceUntilIdle()

            h.readState.newerBlocked.value shouldBe false
            ids(h.rows) shouldBe listOf(4L, 5L)
            h.uiState shouldBe MessageListUIState.Loaded
        }
    }

    test("D84 rule 9: reconnect loads the failed older edge page once by itself") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(2), msg(3))))
            val h = host(tp)
            h.rows = listOf(msg(4), msg(5))
            h.readState.olderBlocked.value = true

            MessageListOfflineReads.onConnected(h)
            advanceUntilIdle()

            h.readState.olderBlocked.value shouldBe false
            tp.previousCalls.size shouldBe 1
            ids(h.rows) shouldBe listOf(2L, 3L, 4L, 5L)

            // Once: nothing is pending, so the next connect reads no page.
            MessageListOfflineReads.onConnected(h)
            advanceUntilIdle()
            tp.previousCalls.size shouldBe 1
            tp.nextAnchors shouldBe emptyList()
        }
    }

    test("D84 rule 9: reconnect loads the failed newer edge page once by itself") {
        runTest {
            val tp = FakeTwoPhase()
            tp.next += phases(TwoPhaseResult.Server(listOf(msg(6), msg(7))))
            val h = host(tp)
            h.rows = listOf(msg(4), msg(5))
            h.readState.newerBlocked.value = true

            MessageListOfflineReads.onConnected(h)
            advanceUntilIdle()

            h.readState.newerBlocked.value shouldBe false
            tp.nextAnchors shouldBe listOf(5L)
            tp.previousCalls.size shouldBe 0
            ids(h.rows) shouldBe listOf(4L, 5L, 6L, 7L)
        }
    }

    test("D84 rule 9: both edges blocked: the older page loads, then the newer page, each once") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(2), msg(3))))
            tp.next += phases(TwoPhaseResult.Server(listOf(msg(6), msg(7))))
            val h = host(tp)
            h.rows = listOf(msg(4), msg(5))
            h.readState.olderBlocked.value = true
            h.readState.newerBlocked.value = true

            MessageListOfflineReads.onConnected(h)
            advanceUntilIdle()

            tp.previousCalls.size shouldBe 1
            tp.nextAnchors shouldBe listOf(5L)
            ids(h.rows) shouldBe listOf(2L, 3L, 4L, 5L, 6L, 7L)
            h.readState.olderBlocked.value shouldBe false
            h.readState.newerBlocked.value shouldBe false

            MessageListOfflineReads.onConnected(h)
            advanceUntilIdle()
            tp.previousCalls.size shouldBe 1
            tp.nextAnchors shouldBe listOf(5L)
        }
    }

    test("D84 rule 9: a read in flight at reconnect: the blocked edge loads once after it settles") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(2), msg(3))))
            val h = host(tp)
            h.rows = listOf(msg(4), msg(5))
            h.readState.olderBlocked.value = true
            // A kit read (e.g. the newest refresh) holds the in-progress flag across the reconnect.
            h.inProgress = true
            launch { gate.await(); h.inProgress = false }

            MessageListOfflineReads.onConnected(h)
            advanceUntilIdle()
            tp.previousCalls.size shouldBe 0
            h.readState.olderBlocked.value shouldBe true // still tappable while it waits

            gate.complete(Unit)
            advanceUntilIdle()
            tp.previousCalls.size shouldBe 1
            ids(h.rows) shouldBe listOf(2L, 3L, 4L, 5L)
            h.readState.olderBlocked.value shouldBe false
        }
    }

    test("D84 rule 9: a window change while waiting (jump, clear) loads no edge of the old window") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val tp = FakeTwoPhase()
            val h = host(tp)
            h.rows = listOf(msg(4), msg(5))
            h.readState.olderBlocked.value = true
            h.inProgress = true
            launch { gate.await(); h.inProgress = false }

            MessageListOfflineReads.onConnected(h)
            advanceUntilIdle()
            h.listGen++ // the window changes before the read in flight settles
            gate.complete(Unit)
            advanceUntilIdle()

            tp.previousCalls.size shouldBe 0
        }
    }

    test("D84 rule 9: reconnect with no blocked edge reads no page") {
        runTest {
            val tp = FakeTwoPhase()
            val h = host(tp)
            h.rows = listOf(msg(4), msg(5))

            MessageListOfflineReads.onConnected(h)
            advanceUntilIdle()

            tp.previousCalls.size shouldBe 0
            tp.nextAnchors shouldBe emptyList()
        }
    }

    test("R8: an empty offline open refilled by the reconnect refresh runs the first-fetch step there, once") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(notConnected))
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(10), msg(11))))
            val h = host(tp, repo(Result.failure(notConnected)))

            MessageListOfflineReads.open(h)
            advanceUntilIdle()
            h.uiState shouldBe MessageListUIState.Error(notConnected)
            h.firstFetchCount shouldBe 0

            MessageListOfflineReads.refreshNewest(h)
            advanceUntilIdle()

            ids(h.rows) shouldBe listOf(10L, 11L)
            h.uiState shouldBe MessageListUIState.Loaded
            h.firstFetchCount shouldBe 1
            h.readState.jumpToLatest.value shouldBe false
        }
    }

    test("R8: a refresh over rows already shown never runs the first-fetch step") {
        runTest {
            val tp = FakeTwoPhase()
            tp.previous += phases(TwoPhaseResult.Server(listOf(msg(9), msg(10))))
            val h = host(tp)
            h.rows = listOf(msg(8), msg(9))

            MessageListOfflineReads.refreshNewest(h)
            advanceUntilIdle()

            h.firstFetchCount shouldBe 0
        }
    }

    test("no two-phase repository: every hook declines, so today's path runs") {
        runTest {
            val h = FakeHost(this, repo(), twoPhase = null)
            MessageListOfflineReads.fetchOlder(h) shouldBe false
            MessageListOfflineReads.fetchNewer(h) shouldBe false
            MessageListOfflineReads.open(h) shouldBe false
            MessageListOfflineReads.jump(h, 1L, true) shouldBe false
        }
    }
})
