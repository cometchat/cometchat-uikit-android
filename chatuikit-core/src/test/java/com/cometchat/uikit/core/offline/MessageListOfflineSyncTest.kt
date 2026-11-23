package com.cometchat.uikit.core.offline

import android.util.Log
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.OfflineCacheResetListener
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Action
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.MessageSource
import com.cometchat.chat.models.OfflineCacheResetReason
import com.cometchat.chat.models.SyncOutcome
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.domain.repository.MessageListTwoPhaseRepository
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * W5 (ENG-41090) — [MessageListOfflineSync] on the message list ViewModel: reconnect without a
 * fetch (G7), the sweep upsert including action rows (BEH-SYNC-25/31), the newest-window refresh
 * on PARTIAL / error, swept messages without sound / +1 / per-message receipts (BEH-SYNC-12),
 * MessagesUpdated never inserting (BEH-INV-11), the cache-reset reload and notice rule (G8,
 * BEH-ENC-15/16), offline turned off mid-session, read-offline replay, and stale-gen drops.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageListOfflineSyncTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var logMock: MockedStatic<Log>
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var repository: MessageListRepository
    val stores = mutableListOf<androidx.lifecycle.ViewModelStore>()
    var refreshCalls = 0

    val me = "me"
    val peer = "peer"
    val guid = "g1"

    fun text(id: Long, sentAt: Long, from: String = peer, to: String = me, group: Boolean = false) =
        TextMessage(if (group) guid else to, "m$id",
            if (group) CometChatConstants.RECEIVER_TYPE_GROUP else CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            this.sentAt = sentAt
            this.sender = User().apply { uid = from; name = from }
            if (!group) this.receiverUid = to
            this.category = CometChatConstants.CATEGORY_MESSAGE
            this.type = CometChatConstants.MESSAGE_TYPE_TEXT
        }

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }

    beforeTest {
        OfflineGate.testOverride = true
        OfflineHub.resetForTest()
        OfflineReadLedger.clearForTest()
        OfflineOffTransition.resetForTest()
        OfflineSend.resetForTest()
        OfflineOffTransition.coalesceMs = 100L
        refreshCalls = 0
        MessageListOfflineSync.refreshNewestWindow = { refreshCalls++ }
        logMock = Mockito.mockStatic(Log::class.java)
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        val meUser = User().apply { uid = me; name = me }
        cometChatMock.`when`<User> { CometChat.getLoggedInUser() }.thenReturn(meUser)
        repository = mock {
            on { hasMorePreviousMessages() } doReturn false
            onBlocking { markAsRead(any()) } doReturn Result.success(Unit)
            onBlocking { markAsDelivered(any()) } doReturn Result.success(Unit)
            onBlocking { fetchActionMessages(any()) } doReturn Result.success(emptyList())
            onBlocking { fetchNextMessages(any()) } doReturn Result.success(emptyList())
        }
    }

    afterTest {
        stores.forEach { it.clear() }
        stores.clear()
        cometChatMock.close()
        logMock.close()
        OfflineGate.testOverride = null
        MessageListOfflineSync.refreshNewestWindow = { MessageListOfflineReads.refreshNewestWindow(it) }
    }

    suspend fun vmWith(
        seed: List<BaseMessage>,
        group: Boolean = false,
        failWith: Throwable? = null
    ): CometChatMessageListViewModel {
        whenever(repository.fetchPreviousMessages()).thenReturn(failWith?.let { Result.failure(it) } ?: Result.success(seed))
        val store = androidx.lifecycle.ViewModelStore()
        stores += store
        val vm = androidx.lifecycle.ViewModelProvider(
            store,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    CometChatMessageListViewModel(repository = repository, enableListeners = true) as T
            }
        )[CometChatMessageListViewModel::class.java]
        if (group) vm.setGroup(Group(guid, "G", CometChatConstants.GROUP_TYPE_PUBLIC, ""))
        else vm.setUser(User().apply { uid = peer; name = peer })
        vm.fetchMessages()
        return vm
    }

    fun connectionListener(): CometChat.ConnectionListener {
        val captor = argumentCaptor<CometChat.ConnectionListener>()
        cometChatMock.verify({ CometChat.addConnectionListener(any(), captor.capture()) }, atLeastOnce())
        return captor.lastValue
    }

    fun messageListener(): CometChat.MessageListener {
        val captor = argumentCaptor<CometChat.MessageListener>()
        cometChatMock.verify({ CometChat.addMessageListener(any(), captor.capture()) }, atLeastOnce())
        return captor.lastValue
    }

    fun ids(vm: CometChatMessageListViewModel) = vm.getItems().map { it.id }

    /** Waits in real time: the offline-off notice is coalesced on a background dispatcher. */
    fun realNotice(): OfflineNotice = kotlinx.coroutines.runBlocking { withTimeout(3_000) { OfflineHub.receiveNotice() } }

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    // ---- reconnect (G7) ----

    test("reconnect makes no fetch with the gate on") {
        runTest {
            val vm = vmWith(listOf(text(1, 100)))
            advanceUntilIdle()
            connectionListener().onConnected()
            advanceUntilIdle()
            verify(repository, never()).fetchActionMessages(any())
            verify(repository, never()).fetchNextMessages(any())
            ids(vm) shouldBe listOf(1L)
        }
    }

    test("reconnect: an empty list reads the newest page once per connection; non-empty reads none") {
        runTest {
            val empty = vmWith(emptyList())
            advanceUntilIdle()
            val emptyConn = connectionListener()
            emptyConn.onConnected()
            advanceUntilIdle()
            refreshCalls shouldBe 1
            verify(repository, never()).fetchActionMessages(any())
            verify(repository, never()).fetchNextMessages(any())
            empty.getItems().isEmpty() shouldBe true

            refreshCalls = 0
            vmWith(listOf(text(1, 100)))
            advanceUntilIdle()
            connectionListener().onConnected()
            advanceUntilIdle()
            refreshCalls shouldBe 0
        }
    }

    test("D83: a list on the error view (nothing shown) reloads once on reconnect, with no other fetch (G7)") {
        runTest {
            val vm = vmWith(emptyList(), failWith = CometChatException("ERR_INTERNET_UNAVAILABLE", "offline"))
            advanceUntilIdle()
            (vm.uiState.value is com.cometchat.uikit.core.state.MessageListUIState.Error) shouldBe true

            connectionListener().onConnected()
            advanceUntilIdle()

            refreshCalls shouldBe 1
            verify(repository, never()).fetchActionMessages(any())
            verify(repository, never()).fetchNextMessages(any())
        }
    }

    // ---- ENG-41460: reconnect clears "Connect to load more" (R7); D58 lands once (R8) ----

    val notConnected = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")

    /** A ViewModel over a two-phase repository, so the offline reads run; [previous] per call. */
    fun twoPhaseVm(
        previous: List<kotlinx.coroutines.flow.Flow<TwoPhaseResult<BaseMessage>>>,
        next: kotlinx.coroutines.flow.Flow<TwoPhaseResult<BaseMessage>> = flowOf(TwoPhaseResult.Server(emptyList()))
    ): Pair<CometChatMessageListViewModel, MessageListTwoPhaseRepository> {
        val repo = mock<MessageListRepository>(extraInterfaces = arrayOf(MessageListTwoPhaseRepository::class)) {
            on { hasMorePreviousMessages() } doReturn true
            onBlocking { markAsRead(any()) } doReturn Result.success(Unit)
            onBlocking { markAsDelivered(any()) } doReturn Result.success(Unit)
            onBlocking { getConversation(any(), any()) } doReturn Result.failure(notConnected)
        }
        val tp = repo as MessageListTwoPhaseRepository
        whenever(tp.fetchPreviousTwoPhase()).thenReturn(previous.first(), *previous.drop(1).toTypedArray())
        whenever(tp.fetchNextTwoPhase(any())).thenReturn(next)
        val store = androidx.lifecycle.ViewModelStore()
        stores += store
        val vm = androidx.lifecycle.ViewModelProvider(
            store,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    CometChatMessageListViewModel(repository = repo, enableListeners = true) as T
            }
        )[CometChatMessageListViewModel::class.java]
        vm.setUser(User().apply { uid = peer; name = peer })
        return vm to tp
    }

    test("D84 rule 9: reconnect clears \"Connect to load more\" and loads the failed older page once by itself") {
        runTest {
            val (vm, tp) = twoPhaseVm(
                previous = listOf(
                    flowOf(TwoPhaseResult.Cached(listOf(text(5, 100), text(6, 101))), TwoPhaseResult.Error(notConnected)),
                    flowOf(TwoPhaseResult.Server(listOf(text(3, 98), text(4, 99))))
                )
            )
            vm.fetchMessages()
            advanceUntilIdle()
            vm.offlineLoadMoreOlder.value shouldBe true
            clearInvocations(tp)

            connectionListener().onConnected()
            advanceUntilIdle()

            vm.offlineLoadMoreOlder.value shouldBe false
            verify(tp, times(1)).fetchPreviousTwoPhase()
            verify(tp, never()).fetchNextTwoPhase(any())
            ids(vm) shouldBe listOf(3L, 4L, 5L, 6L)

            // Once: nothing is pending now, so the next connect loads no page.
            clearInvocations(tp)
            connectionListener().onConnected()
            advanceUntilIdle()
            verify(tp, never()).fetchPreviousTwoPhase()
        }
    }

    test("R8: empty offline open, reconnect refill: one scroll to the bottom, and the next older page none") {
        runTest {
            MessageListOfflineSync.refreshNewestWindow = { MessageListOfflineReads.refreshNewestWindow(it) }
            val (vm, _) = twoPhaseVm(
                previous = listOf(
                    flowOf(TwoPhaseResult.Cached(emptyList()), TwoPhaseResult.Error(notConnected)),
                    flowOf(TwoPhaseResult.Server(listOf(text(10, 110), text(11, 111)))),
                    flowOf(TwoPhaseResult.Server(listOf(text(8, 108), text(9, 109))))
                )
            )
            var scrolls = 0
            val collector = launch(testDispatcher) { vm.scrollToBottomEvent.collect { scrolls++ } }

            vm.fetchMessagesWithUnreadCount()
            advanceUntilIdle()
            vm.getItems().isEmpty() shouldBe true
            scrolls shouldBe 0

            connectionListener().onConnected()
            advanceUntilIdle()
            ids(vm) shouldBe listOf(10L, 11L)
            scrolls shouldBe 1

            vm.fetchMessages()
            advanceUntilIdle()
            ids(vm) shouldBe listOf(8L, 9L, 10L, 11L)
            scrolls shouldBe 1
            collector.cancel()
        }
    }

    test("reconnect with the gate off: an empty list keeps today's path, no refresh") {
        runTest {
            OfflineGate.testOverride = false
            vmWith(emptyList())
            advanceUntilIdle()
            connectionListener().onConnected()
            advanceUntilIdle()
            refreshCalls shouldBe 0
            verify(repository, atLeastOnce()).fetchPreviousMessages()
        }
    }

    test("reconnect runs today's fetch with the gate off") {
        runTest {
            OfflineGate.testOverride = false
            vmWith(listOf(text(1, 100)))
            advanceUntilIdle()
            connectionListener().onConnected()
            advanceUntilIdle()
            verify(repository, atLeastOnce()).fetchActionMessages(any())
        }
    }

    // ---- sweep upsert ----

    test("sweep upserts in sorted place, including an action row, and dedupes a live copy") {
        runTest {
            val vm = vmWith(listOf(text(1, 100, group = true), text(4, 400, group = true)), group = true)
            advanceUntilIdle()
            val kick = Action().apply {
                id = 3; sentAt = 300; receiverUid = guid
                receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
                category = CometChatConstants.CATEGORY_ACTION; type = CometChatConstants.ActionKeys.ACTION_TYPE_GROUP_MEMBER
                action = CometChatConstants.ActionKeys.ACTION_KICKED
                sender = User().apply { uid = "admin" }
            }
            val swept = listOf(text(2, 200, group = true), kick, text(4, 400, group = true), text(5, 500, group = true))
            OfflineHub.publishForTest(OfflineHubEvent.MessageSyncCompleted(OfflineHub.gen, swept, SyncOutcome.CAUGHT_UP))
            advanceUntilIdle()
            ids(vm) shouldBe listOf(1L, 2L, 3L, 4L, 5L)
            refreshCalls shouldBe 0 // CAUGHT_UP does not refresh
        }
    }

    test("PARTIAL calls refreshNewestWindow once; CAUGHT_UP does not") {
        runTest {
            vmWith(listOf(text(1, 100)))
            advanceUntilIdle()
            OfflineHub.publishForTest(OfflineHubEvent.MessageSyncCompleted(OfflineHub.gen, emptyList(), SyncOutcome.CAUGHT_UP))
            advanceUntilIdle()
            refreshCalls shouldBe 0
            OfflineHub.publishForTest(OfflineHubEvent.MessageSyncCompleted(OfflineHub.gen, emptyList(), SyncOutcome.PARTIAL))
            advanceUntilIdle()
            refreshCalls shouldBe 1
        }
    }

    test("a sync error calls refreshNewestWindow once") {
        runTest {
            vmWith(listOf(text(1, 100)))
            advanceUntilIdle()
            OfflineHub.publishForTest(OfflineHubEvent.MessageSyncFailed(OfflineHub.gen, CometChatException("E", "x")))
            advanceUntilIdle()
            refreshCalls shouldBe 1
        }
    }

    test("a sweep row older than the loaded window is not inserted while older history is unloaded") {
        runTest {
            whenever(repository.hasMorePreviousMessages()).thenReturn(true)
            val vm = vmWith(listOf(text(5, 500)))
            advanceUntilIdle()
            OfflineHub.publishForTest(
                OfflineHubEvent.MessageSyncCompleted(OfflineHub.gen, listOf(text(2, 200), text(6, 600)), SyncOutcome.CAUGHT_UP)
            )
            advanceUntilIdle()
            ids(vm) shouldBe listOf(5L, 6L)
        }
    }

    // ---- swept received messages (BEH-SYNC-12) ----

    test("SYNC messages: sorted place, no sound, no per-message receipt; one cumulative delivery") {
        runTest {
            val vm = vmWith(listOf(text(1, 100), text(9, 900)))
            advanceUntilIdle()
            clearInvocations(repository) // the initial load's own receipts are today's, not the sweep's
            val sounds = mutableListOf<Boolean>()
            val job = launch { vm.playSoundEvent.collect { sounds += it } }
            val listener = messageListener()
            val a = text(3, 300).apply { source = MessageSource.SYNC }
            val b = text(5, 500).apply { source = MessageSource.SYNC }
            listener.onTextMessageReceived(a)
            listener.onTextMessageReceived(b)
            advanceUntilIdle()
            ids(vm) shouldBe listOf(1L, 3L, 5L, 9L)
            sounds.size shouldBe 0
            verify(repository, never()).markAsDelivered(any())
            verify(repository, never()).markAsRead(any())
            vm.hasMoreNewMessages.value shouldBe false

            OfflineHub.publishForTest(OfflineHubEvent.MessageSyncCompleted(OfflineHub.gen, listOf(a, b), SyncOutcome.CAUGHT_UP))
            advanceUntilIdle()
            verify(repository, times(1)).markAsDelivered(any())
            verify(repository, times(1)).markAsDelivered(eq(b))
            verify(repository, never()).markAsRead(any())
            job.cancel()
        }
    }

    test("the read receipt after a sweep is one, for the newest row, sent by the at-bottom read") {
        runTest {
            val vm = vmWith(listOf(text(1, 100)))
            advanceUntilIdle()
            clearInvocations(repository) // the initial load's own receipts are today's, not the sweep's
            val swept = listOf(text(2, 200), text(3, 300))
            OfflineHub.publishForTest(OfflineHubEvent.MessageSyncCompleted(OfflineHub.gen, swept, SyncOutcome.CAUGHT_UP))
            advanceUntilIdle()
            verify(repository, never()).markAsRead(any()) // scrolled up: the UI never calls it
            vm.markLastMessageAsRead(vm.getItems().last()) // at bottom: the UI's one call
            advanceUntilIdle()
            verify(repository, times(1)).markAsRead(any())
            verify(repository, times(1)).markAsRead(eq(swept.last()))
        }
    }

    // ---- read offline (BEH-SYNC-07) ----

    test("a chat read offline is marked read once after the next sync") {
        runTest {
            whenever(repository.markAsRead(any())).thenReturn(Result.failure(CometChatException("ERR", "offline")))
            val vm = vmWith(listOf(text(1, 100)))
            advanceUntilIdle()
            val last = vm.getItems().last()
            vm.markLastMessageAsRead(last)
            advanceUntilIdle()
            OfflineReadLedger.pendingForTest() shouldBe listOf(last)

            OfflineReadLedger.flush()
            cometChatMock.verify({ CometChat.markAsRead(eq(last), any<CometChat.CallbackListener<Void>>()) }, times(1))
            OfflineReadLedger.pendingForTest() shouldBe emptyList()
        }
    }

    test("the hub flushes the read-offline ledger on MessageSyncCompleted") {
        val m = text(7, 700)
        OfflineReadLedger.remember(m)
        OfflineHub.start()
        val sync = argumentCaptor<CometChat.SyncListener>()
        cometChatMock.verify { CometChat.addSyncListener(any(), sync.capture()) }
        sync.firstValue.onMessageSyncCompleted(emptyList(), SyncOutcome.CAUGHT_UP)
        cometChatMock.verify({ CometChat.markAsRead(eq(m), any<CometChat.CallbackListener<Void>>()) }, times(1))
    }

    // ---- ENG-41643 (D-369): the flush emits MessageRead only in the session that started it ----

    /** Flushes [m] and returns its captured markAsRead callback, plus the MessageRead events seen. */
    suspend fun kotlinx.coroutines.test.TestScope.flushCapturing(
        m: BaseMessage
    ): Pair<CometChat.CallbackListener<Void>, List<BaseMessage>> {
        val reads = mutableListOf<BaseMessage>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            com.cometchat.uikit.core.events.CometChatEvents.messageEvents.collect { e ->
                if (e is com.cometchat.uikit.core.events.CometChatMessageEvent.MessageRead && e.message === m) reads += e.message
            }
        }
        OfflineReadLedger.remember(m)
        OfflineReadLedger.flush()
        val callback = argumentCaptor<CometChat.CallbackListener<Void>>()
        cometChatMock.verify({ CometChat.markAsRead(eq(m), callback.capture()) }, times(1))
        return callback.firstValue to reads
    }

    test("flush_successAfterSessionChange_emitsNoMessageRead") {
        runTest {
            val m = text(8, 800).apply { conversationId = "c_peer" }
            val (callback, reads) = flushCapturing(m)
            OfflineHub.onLogout() // the session ends while markAsRead is in flight
            callback.onSuccess(null)
            advanceUntilIdle()
            reads shouldBe emptyList()
            OfflineReadLedger.isFlushed(m) shouldBe false // nothing marked for the next session
        }
    }

    test("flush_successSameSession_emitsMessageReadOnce") {
        runTest {
            val m = text(9, 900).apply { conversationId = "c_peer" }
            val (callback, reads) = flushCapturing(m)
            OfflineReadLedger.isFlushed(m) shouldBe false // marked only on success
            callback.onSuccess(null)
            advanceUntilIdle()
            reads shouldBe listOf(m)
            OfflineReadLedger.isFlushed(m) shouldBe true // the conversation lists can tell it apart
            OfflineHub.onLogout()
            OfflineReadLedger.isFlushed(m) shouldBe false // cleared on session end
            // The ledger was drained: a second flush sends nothing more.
            OfflineReadLedger.flush()
            cometChatMock.verify({ CometChat.markAsRead(eq(m), any<CometChat.CallbackListener<Void>>()) }, times(1))
        }
    }

    // ---- MessagesUpdated (BEH-INV-11) ----

    test("MessagesUpdated updates a shown row and never inserts") {
        runTest {
            val vm = vmWith(listOf(text(1, 100), text(2, 200)))
            advanceUntilIdle()
            val edited = text(2, 200).apply { text = "edited"; updatedAt = 10 }
            OfflineHub.publishForTest(OfflineHubEvent.MessagesUpdated(OfflineHub.gen, listOf(edited, text(3, 300))))
            advanceUntilIdle()
            ids(vm) shouldBe listOf(1L, 2L)
            (vm.getItems()[1] as TextMessage).text shouldBe "edited"
        }
    }

    // ---- cache reset (G8) ----

    test("the cache-reset notice matrix") {
        OfflineSessionNotices.forCacheReset(OfflineCacheResetReason.KEY_LOST, 3) shouldBe OfflineNotice.UnsentLost(3)
        OfflineSessionNotices.forCacheReset(OfflineCacheResetReason.CORRUPTED, -1) shouldBe OfflineNotice.UnsentMaybeLost
        OfflineSessionNotices.forCacheReset(OfflineCacheResetReason.CORRUPTED, 0) shouldBe null
        OfflineSessionNotices.forCacheReset(OfflineCacheResetReason.ENCRYPTION_CHANGED, -1) shouldBe null
        OfflineSessionNotices.forCacheReset(OfflineCacheResetReason.ENCRYPTION_CHANGED, 0) shouldBe null
    }

    test("the hub posts the cache-reset notice once per reset, and none when nothing was lost") {
        runTest {
            OfflineHub.start()
            val reset = argumentCaptor<OfflineCacheResetListener>()
            cometChatMock.verify { CometChat.addOfflineCacheResetListener(any(), reset.capture()) }
            reset.firstValue.onOfflineCacheReset(OfflineCacheResetReason.CORRUPTED, 0)
            reset.firstValue.onOfflineCacheReset(OfflineCacheResetReason.KEY_LOST, 2)
            withTimeout(1_000) { OfflineHub.receiveNotice() } shouldBe OfflineNotice.UnsentLost(2)
            OfflineHub.postNotice(OfflineNotice.QueueFull) // sentinel: nothing else was queued before it
            withTimeout(1_000) { OfflineHub.receiveNotice() } shouldBe OfflineNotice.QueueFull
        }
    }

    test("cache reset clears the list, bumps its gen and reloads") {
        runTest {
            val vm = vmWith(listOf(text(1, 100)))
            advanceUntilIdle()
            val genBefore = vm.offlineListGen
            whenever(repository.fetchPreviousMessages()).thenReturn(Result.success(listOf(text(8, 800))))
            OfflineHub.publishForTest(OfflineHubEvent.CacheReset(OfflineHub.gen, OfflineCacheResetReason.CORRUPTED, 0))
            advanceUntilIdle()
            vm.offlineListGen shouldBe genBefore + 1
            verify(repository, atLeastOnce()).resetRequest()
            ids(vm) shouldBe listOf(8L)
        }
    }

    // ---- offline turned off mid-session ----

    test("offline turned off mid-session: un-acked own rows fail, one notice, sent rows untouched") {
        runTest {
            val sent = text(1, 100, from = me, to = peer)
            val vm = vmWith(listOf(sent))
            advanceUntilIdle()
            val pending1 = text(0, 0, from = me, to = peer).apply { localMessageId = "l1"; muid = "u1" }
            val pending2 = text(0, 0, from = me, to = peer).apply { localMessageId = "l2"; muid = "u2" }
            vm.offlineSetRows(vm.getItems() + pending1 + pending2)

            OfflineGate.testOverride = false
            connectionListener().onConnected()
            connectionListener().onConnected() // a second event does nothing more
            advanceUntilIdle()

            val rows = vm.getItems()
            rows[0].status shouldBe sent.status
            rows[0].metadata?.has("error") shouldBe (sent.metadata?.has("error"))
            rows.drop(1).all { it.metadata.has("error") && OfflineSend.isDirectFailure(it) } shouldBe true
            realNotice() shouldBe OfflineNotice.MessagesNotSent(2)
            OfflineHub.postNotice(OfflineNotice.QueueFull)
            realNotice() shouldBe OfflineNotice.QueueFull
        }
    }

    test("offline turned off with two lists open posts one notice for the transition") {
        runTest {
            val vmA = vmWith(emptyList())
            val vmB = vmWith(emptyList())
            advanceUntilIdle()
            vmA.offlineSetRows(listOf(text(0, 0, from = me, to = peer).apply { localMessageId = "a1"; muid = "ua" }))
            vmB.offlineSetRows(listOf(
                text(0, 0, from = me, to = peer).apply { localMessageId = "b1"; muid = "ub1" },
                text(0, 0, from = me, to = peer).apply { localMessageId = "b2"; muid = "ub2" }
            ))
            val captor = argumentCaptor<CometChat.ConnectionListener>()
            cometChatMock.verify({ CometChat.addConnectionListener(any(), captor.capture()) }, atLeastOnce())
            OfflineGate.testOverride = false
            captor.allValues.takeLast(2).forEach { it.onConnected() }
            advanceUntilIdle()
            realNotice() shouldBe OfflineNotice.MessagesNotSent(3)
            OfflineHub.postNotice(OfflineNotice.QueueFull)
            realNotice() shouldBe OfflineNotice.QueueFull // no second MessagesNotSent
        }
    }

    test("offline off, on, then off again in one session posts a second notice") {
        runTest {
            val vmA = vmWith(emptyList())
            val vmB = vmWith(emptyList())
            advanceUntilIdle()
            val captor = argumentCaptor<CometChat.ConnectionListener>()
            cometChatMock.verify({ CometChat.addConnectionListener(any(), captor.capture()) }, atLeastOnce())
            val (connA, connB) = captor.allValues.takeLast(2)
            vmA.offlineSetRows(listOf(text(0, 0, from = me, to = peer).apply { localMessageId = "a1"; muid = "ua" }))

            OfflineGate.testOverride = false
            connA.onConnected()
            advanceUntilIdle()
            realNotice() shouldBe OfflineNotice.MessagesNotSent(1)

            OfflineGate.testOverride = true
            connB.onConnected() // offline on again
            advanceUntilIdle()
            vmB.offlineSetRows(listOf(text(0, 0, from = me, to = peer).apply { localMessageId = "b1"; muid = "ub" }))

            OfflineGate.testOverride = false
            connB.onConnected()
            advanceUntilIdle()
            realNotice() shouldBe OfflineNotice.MessagesNotSent(1)
        }
    }

    // ---- stale gen ----

    test("a stale-gen sweep is dropped") {
        runTest {
            val vm = vmWith(listOf(text(1, 100)))
            advanceUntilIdle()
            OfflineHub.publishForTest(
                OfflineHubEvent.MessageSyncCompleted(OfflineHub.gen + 7, listOf(text(2, 200)), SyncOutcome.PARTIAL)
            )
            advanceUntilIdle()
            ids(vm) shouldBe listOf(1L)
            refreshCalls shouldBe 0
        }
    }
})
