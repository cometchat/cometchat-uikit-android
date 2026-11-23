package com.cometchat.uikit.core.offline

import android.util.Log
import org.mockito.kotlin.whenever
import com.cometchat.chat.core.Call
import com.cometchat.chat.models.ConversationUpdateSettings
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.helpers.CometChatHelper
import com.cometchat.chat.models.Action
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.MessageFailureReason
import com.cometchat.chat.models.MessageSource
import com.cometchat.chat.models.OfflineCacheResetReason
import com.cometchat.chat.models.SyncOutcome
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.ConversationListRepository
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.events.MessageStatus
import com.cometchat.uikit.core.domain.repository.ConversationListTwoPhaseRepository
import com.cometchat.uikit.core.domain.usecase.DeleteConversationUseCase
import com.cometchat.uikit.core.domain.usecase.GetConversationListUseCase
import com.cometchat.uikit.core.domain.usecase.RefreshConversationListUseCase
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

/**
 * W3 (ENG-41088): the conversation list's offline-ON path. Real SDK model objects throughout (a
 * Mockito-mocked model swallows the production setters, so a mock-based test would pin nothing).
 * The two-phase repository is a scripted fake; `CometChatHelper.getConversationFromMessage` is
 * stubbed to build the row from the message, as the SDK does.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationListOfflineTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    val me = "me"
    lateinit var logMock: MockedStatic<Log>
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var helperMock: MockedStatic<CometChatHelper>
    val stores = mutableListOf<androidx.lifecycle.ViewModelStore>()

    // ---------------------------------------------------------------- builders

    fun user(uid: String) = User(uid, "name-$uid")

    fun message(
        peer: String,
        sentAt: Long,
        id: Long = sentAt,
        sender: String = peer,
        localId: String? = null,
        source: MessageSource? = null,
        group: Boolean = false
    ): TextMessage {
        val type = if (group) CometChatConstants.RECEIVER_TYPE_GROUP else CometChatConstants.RECEIVER_TYPE_USER
        return TextMessage(peer, "m$sentAt", type).apply {
            this.id = id
            this.sentAt = sentAt
            this.sender = user(sender)
            this.receiver = if (group) Group(peer, peer, CometChatConstants.GROUP_TYPE_PUBLIC, null) else user(peer)
            this.conversationId = if (group) "group_$peer" else "c_$peer"
            if (localId != null) this.localMessageId = localId
            if (source != null) this.source = source
        }
    }

    fun conv(
        peer: String,
        lastAt: Long,
        unread: Int = 0,
        pinnedAt: Long = 0L,
        updatedAt: Long = lastAt,
        last: BaseMessage? = message(peer, lastAt),
        group: Boolean = false
    ): Conversation {
        val type = if (group) CometChatConstants.CONVERSATION_TYPE_GROUP else CometChatConstants.CONVERSATION_TYPE_USER
        return Conversation(if (group) "group_$peer" else "c_$peer", type).apply {
            conversationWith = if (group) Group(peer, peer, CometChatConstants.GROUP_TYPE_PUBLIC, null) else user(peer)
            lastMessage = last
            unreadMessageCount = unread
            this.pinnedAt = pinnedAt
            this.updatedAt = updatedAt
        }
    }

    /** A scripted two-phase repository: each read pops the next script (a list of phases). */
    class FakeRepo : ConversationListRepository, ConversationListTwoPhaseRepository {
        val scripts = ArrayDeque<List<Any>>()
        var reads = 0
        val requests = mutableListOf<ConversationsRequest>()

        override fun fetchNextTwoPhase(request: ConversationsRequest): Flow<TwoPhaseResult<Conversation>> {
            reads++
            requests += request
            val script = scripts.removeFirstOrNull() ?: emptyList()
            return flow {
                for (step in script) {
                    @Suppress("UNCHECKED_CAST")
                    when (step) {
                        is CompletableDeferred<*> -> step.await()
                        else -> emit(step as TwoPhaseResult<Conversation>)
                    }
                }
            }
        }

        /** The OFF path's single-phase page; null means the ON path must never read single-phase. */
        var offPage: List<Conversation>? = null

        var offReads = 0

        override suspend fun getConversations(request: ConversationsRequest): Result<List<Conversation>> {
            offReads++
            return Result.success(offPage ?: error("single-phase read on the ON path"))
        }

        override suspend fun deleteConversation(conversationWith: String, conversationType: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun markAsDelivered(conversation: Conversation): Result<Unit> = Result.success(Unit)
        override fun hasMoreConversations(): Boolean = true
    }

    lateinit var repo: FakeRepo
    lateinit var refreshUseCase: RefreshConversationListUseCase

    fun newViewModel(vararg firstRead: Any): CometChatConversationsViewModel {
        repo.scripts.addLast(firstRead.toList())
        val store = androidx.lifecycle.ViewModelStore()
        stores += store
        return androidx.lifecycle.ViewModelProvider(
            store,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    CometChatConversationsViewModel(
                        GetConversationListUseCase(repo), mock<DeleteConversationUseCase>(), refreshUseCase,
                        enableListeners = true
                    ) as T
            }
        )[CometChatConversationsViewModel::class.java]
    }

    fun ids(vm: CometChatConversationsViewModel) = vm.conversations.value.map { it.conversationId }

    fun <T> captureListener(type: Class<T>, register: (ArgumentCaptor<T>) -> Unit): T {
        val captor = ArgumentCaptor.forClass(type)
        cometChatMock.verify { register(captor) }
        return captor.value
    }

    fun messageListener(): CometChat.MessageListener =
        captureListener(CometChat.MessageListener::class.java) { CometChat.addMessageListener(any(), it.capture()) }

    fun conversationListener(): CometChat.ConversationListener =
        captureListener(CometChat.ConversationListener::class.java) { CometChat.addConversationListener(any(), it.capture()) }

    fun connectionListener(): CometChat.ConnectionListener =
        captureListener(CometChat.ConnectionListener::class.java) { CometChat.addConnectionListener(any(), it.capture()) }

    // ---------------------------------------------------------------- setup

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }

    beforeTest {
        OfflineGate.testOverride = true
        OfflineHub.resetForTest()
        OfflineReadLedger.clearForTest()
        repo = FakeRepo()
        refreshUseCase = mock()
        logMock = Mockito.mockStatic(Log::class.java)
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        cometChatMock.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(user(me))
        helperMock = Mockito.mockStatic(CometChatHelper::class.java)
        helperMock.`when`<Conversation?> { CometChatHelper.getConversationFromMessage(any()) }.thenAnswer { inv ->
            val m = inv.arguments[0] as BaseMessage
            val group = CometChatConstants.RECEIVER_TYPE_GROUP.equals(m.receiverType, true)
            Conversation(m.conversationId, if (group) "group" else "user").apply {
                conversationWith = m.receiver
                lastMessage = m
                updatedAt = m.sentAt
            }
        }
    }

    afterTest {
        stores.forEach { it.clear() }
        stores.clear()
        helperMock.close()
        cometChatMock.close()
        logMock.close()
        OfflineGate.testOverride = null
        ConversationListOffline.sweepPageSizeOverride = null
    }

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    // ---------------------------------------------------------------- reads

    test("cached then server reconcile: cached rows paint first, the server proves omission") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val vm = newViewModel(
                TwoPhaseResult.Cached(listOf(conv("a", 300), conv("b", 260))),
                gate,
                TwoPhaseResult.Server(listOf(conv("a", 300, unread = 5, updatedAt = 310), conv("c", 250)))
            )
            ids(vm) shouldBe listOf("c_a", "c_b")
            (vm.uiState.value is com.cometchat.uikit.core.state.UIState.Content) shouldBe true

            gate.complete(Unit)
            advanceUntilIdle()

            // b came from the cached phase, lies inside [250, 300] and the server lacks it: gone.
            ids(vm) shouldBe listOf("c_a", "c_c")
            vm.conversations.value.first().unreadMessageCount shouldBe 5
            repo.reads shouldBe 1
        }
    }

    test("an empty cached page never ends the list; an empty server page does") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Cached(emptyList<Conversation>()))
            vm.hasMoreData shouldBe true
            vm.uiState.value shouldBe com.cometchat.uikit.core.state.UIState.Loading
        }
    }

    test("a filtered empty server page keeps the cache; an unfiltered first page clears it (D17)") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 300))))
            vm.setConversationsRequestBuilder(ConversationsRequest.ConversationsRequestBuilder().setConversationType("user"))
            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(listOf(conv("a", 300), conv("b", 200))), TwoPhaseResult.Server(emptyList<Conversation>())))
            vm.refreshList()
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a", "c_b")

            val plain = newViewModel(TwoPhaseResult.Cached(listOf(conv("a", 300))), TwoPhaseResult.Server(emptyList<Conversation>()))
            advanceUntilIdle()
            plain.conversations.value.shouldBeEmpty()
            plain.uiState.value shouldBe com.cometchat.uikit.core.state.UIState.Empty
        }
    }

    test("stale gen dropped: a read whose generation was bumped never applies its server phase") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val vm = newViewModel(TwoPhaseResult.Cached(listOf(conv("a", 300))), gate, TwoPhaseResult.Server(listOf(conv("z", 900))))
            vm.clearForOfflineSession()
            gate.complete(Unit)
            advanceUntilIdle()
            vm.conversations.value.shouldBeEmpty()
        }
    }

    // ---------------------------------------------------------------- reconnect

    test("reconnect makes no fetch") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 300))))
            connectionListener().onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 1
            verify(refreshUseCase, never()).invoke(any())
            ids(vm) shouldBe listOf("c_a")
        }
    }

    test("slice reconcile: in range replaced or removed, out of range kept, own unsent kept, cursor untouched") {
        runTest {
            val unsent = message("u", 350, id = 0L, sender = me, localId = "l-u")
            val vm = newViewModel(
                TwoPhaseResult.Server(
                    listOf(
                        conv("p", 100, pinnedAt = 50), conv("a", 500), conv("b", 400),
                        conv("u", 350, last = unsent), conv("c", 300), conv("d", 100)
                    )
                )
            )
            val request = vm.conversationsRequest
            val hasMore = vm.hasMoreData

            OfflineHub.publishForTest(
                OfflineHubEvent.ConversationSyncCompleted(OfflineHub.gen, listOf(conv("a", 500), conv("c", 450), conv("e", 320)))
            )
            advanceUntilIdle()

            // Floor = 320 (oldest unpinned updatedAt on the page). b (400) is above it and missing →
            // removed; u is unsent → kept; d (100) is below → kept; c moved up; e added. The page has an
            // unpinned row and no pin, so p's pin is stale: unpinned, not removed (BEH-CPIN-05).
            ids(vm) shouldBe listOf("c_a", "c_c", "c_u", "c_e", "c_p", "c_d")
            vm.conversations.value.first { it.conversationId == "c_p" }.isPinned shouldBe false
            vm.conversationsRequest shouldBeSameInstanceAs request
            vm.hasMoreData shouldBe hasMore
            repo.reads shouldBe 1
        }
    }

    // ENG-41459 R1: the slice is ranked by the conversation's own updatedAt; the pinned block stays
    // out of the floor (BEH-SYNC-15, BEH-CPIN-05).
    fun sync(page: List<Conversation>) =
        OfflineHub.publishForTest(OfflineHubEvent.ConversationSyncCompleted(OfflineHub.gen, page))

    test("R1: a pinned row in a truncated sync page removes only unpinned rows above the floor") {
        runTest {
            // [pb, pa, a, b, c]: a and b are newer than c by sentAt; only a is newer by updatedAt.
            val vm = newViewModel(
                TwoPhaseResult.Server(
                    listOf(
                        conv("pb", 100, pinnedAt = 20, updatedAt = 50), conv("pa", 600, pinnedAt = 10, updatedAt = 40),
                        conv("a", 900, updatedAt = 900), conv("b", 800, updatedAt = 100), conv("c", 700, updatedAt = 300)
                    )
                )
            )
            ids(vm) shouldBe listOf("c_pb", "c_pa", "c_a", "c_b", "c_c")

            sync(listOf(conv("pb", 100, pinnedAt = 20, updatedAt = 50), conv("c", 700, updatedAt = 300)))
            advanceUntilIdle()

            // Floor = c's 300: a (900) is above it and missing → removed; b (100) is below → kept;
            // pa is missing from a pinned block that c proves complete → unpinned, not removed.
            ids(vm) shouldBe listOf("c_pb", "c_b", "c_c", "c_pa")
            vm.conversations.value.first { it.conversationId == "c_pa" }.isPinned shouldBe false
            vm.conversations.value.first { it.conversationId == "c_pb" }.isPinned shouldBe true
            repo.reads shouldBe 1
        }
    }

    test("R1: a truncated page with only pinned rows removes and unpins nothing") {
        runTest {
            val vm = newViewModel(
                TwoPhaseResult.Server(
                    listOf(
                        conv("pb", 100, pinnedAt = 20), conv("pa", 90, pinnedAt = 10),
                        conv("a", 500), conv("b", 400)
                    )
                )
            )
            sync(listOf(conv("pb", 100, pinnedAt = 20)))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_pb", "c_pa", "c_a", "c_b")
            vm.conversations.value.first { it.conversationId == "c_pa" }.isPinned shouldBe true
        }
    }

    test("R1: a complete (short) page removes every absent row except an own unsent preview") {
        runTest {
            ConversationListOffline.sweepPageSizeOverride = 10
            val unsent = message("u", 350, id = 0L, sender = me, localId = "l-u")
            val vm = newViewModel(
                TwoPhaseResult.Server(
                    listOf(
                        conv("p", 100, pinnedAt = 50), conv("a", 500), conv("b", 400),
                        conv("u", 350, last = unsent), conv("c", 300), conv("d", 100, updatedAt = 0)
                    )
                )
            )
            sync(listOf(conv("a", 500), conv("c", 300)))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a", "c_u", "c_c")
        }
    }

    test("R1: a complete page removes a deleted chat even when it was the newest") {
        runTest {
            ConversationListOffline.sweepPageSizeOverride = 10
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("x", 900), conv("a", 500))))
            connectionListener().onConnected()
            sync(listOf(conv("a", 500)))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a")
        }
    }

    test("R1: a chat a live message wrote after connect survives a complete page") {
        runTest {
            ConversationListOffline.sweepPageSizeOverride = 10
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))
            connectionListener().onConnected()
            messageListener().onTextMessageReceived(message("n", 900, source = MessageSource.REALTIME))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_n", "c_a", "c_b")

            sync(listOf(conv("a", 500)))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_n", "c_a")
        }
    }

    test("R1: a chat a live message wrote after connect survives a truncated page") {
        runTest {
            val vm = newViewModel(
                TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400), conv("c", 300), conv("d", 100)))
            )
            connectionListener().onConnected()
            // d was below the floor when the page was read; a live message lifts it above the floor.
            messageListener().onTextMessageReceived(message("d", 450, source = MessageSource.REALTIME))
            advanceUntilIdle()

            sync(listOf(conv("a", 500), conv("c", 300)))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a", "c_d", "c_c")
        }
    }

    test("R1: a row a live event wrote before the last connect, or before the last sync, is not spared") {
        runTest {
            ConversationListOffline.sweepPageSizeOverride = 10
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            val connection = connectionListener()
            connection.onConnected()
            messageListener().onTextMessageReceived(message("n", 900, source = MessageSource.REALTIME))
            advanceUntilIdle()
            // The next connect starts a new sweep: n was written before it was requested.
            connection.onConnected()
            sync(listOf(conv("a", 500)))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a")

            // Cleared on sync completion too.
            messageListener().onTextMessageReceived(message("m", 950, source = MessageSource.REALTIME))
            advanceUntilIdle()
            sync(listOf(conv("a", 500)))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_m", "c_a")
            sync(listOf(conv("a", 500)))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a")
        }
    }

    test("R1: a full-size page is truncated and keeps rows at or below the floor") {
        runTest {
            ConversationListOffline.sweepPageSizeOverride = 2
            val vm = newViewModel(
                TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400), conv("c", 300), conv("d", 100)))
            )
            sync(listOf(conv("a", 500), conv("c", 300)))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a", "c_c", "c_d")
        }
    }

    // ENG-41459 R6: a late MessageSent SUCCESS never replaces a newer preview (BEH-CONV-06).
    fun sent(message: BaseMessage) = runBlocking {
        CometChatEvents.emitMessageEventSync(CometChatMessageEvent.MessageSent(message, MessageStatus.SUCCESS))
    }

    test("R6: a MessageSent older than the shown preview is ignored") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 600), conv("b", 500))))
            sent(message("b", 400, id = 400, sender = me))
            advanceUntilIdle()
            vm.conversations.value.first { it.conversationId == "c_b" }.lastMessage!!.sentAt shouldBe 500L
            ids(vm) shouldBe listOf("c_a", "c_b")
        }
    }

    test("R6: a MessageSent newer than the shown preview applies") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 600), conv("b", 500))))
            sent(message("b", 700, id = 700, sender = me))
            advanceUntilIdle()
            vm.conversations.value.first { it.conversationId == "c_b" }.lastMessage!!.sentAt shouldBe 700L
            ids(vm) shouldBe listOf("c_b", "c_a")
        }
    }

    test("R6: offline off keeps today's path, an older MessageSent still replaces the preview") {
        runTest {
            OfflineGate.testOverride = false
            repo.offPage = listOf(conv("b", 500))
            val vm = newViewModel()
            advanceUntilIdle()
            vm.offlineOn shouldBe false
            sent(message("b", 400, id = 400, sender = me))
            advanceUntilIdle()
            vm.conversations.value.single().lastMessage!!.sentAt shouldBe 400L
        }
    }

    // ENG-41459 R3 / D75: a load-more page past the cache that fails offline is read again once on
    // connect; no other list fetch on reconnect (G7).
    fun offlineError() = TwoPhaseResult.Error(
        CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")
    )

    fun serverError() = TwoPhaseResult.Error(CometChatException("ERR_SERVER", "boom"))

    fun failNextPageOffline(vm: CometChatConversationsViewModel) {
        repo.scripts.addLast(listOf(TwoPhaseResult.Cached(emptyList<Conversation>()), offlineError()))
        vm.fetchConversations()
    }

    test("R3: a page that failed offline is read again exactly once on connect, with the same request") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            failNextPageOffline(vm)
            advanceUntilIdle()
            repo.reads shouldBe 2
            vm.hasMoreData shouldBe true

            repo.scripts.addLast(listOf(TwoPhaseResult.Server(listOf(conv("b", 400)))))
            val connection = connectionListener()
            connection.onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 3
            repo.requests[2] shouldBeSameInstanceAs repo.requests[1]
            verify(refreshUseCase, never()).invoke(any())
            ids(vm) shouldBe listOf("c_a", "c_b")

            // The mark is consumed: a second connect reads nothing.
            connection.onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 3
        }
    }

    test("R3: pages that add no new chat keep reading, at most 10 reads, then stop") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            failNextPageOffline(vm)
            advanceUntilIdle()
            // Every retry page returns only the chat already shown.
            repeat(12) { repo.scripts.addLast(listOf(TwoPhaseResult.Server(listOf(conv("a", 500))))) }
            connectionListener().onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 2 + 10
            ids(vm) shouldBe listOf("c_a")
        }
    }

    test("R3: the retry stops at the first page that adds a chat") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            failNextPageOffline(vm)
            advanceUntilIdle()
            repo.scripts.addLast(listOf(TwoPhaseResult.Server(listOf(conv("a", 500)))))
            repo.scripts.addLast(listOf(TwoPhaseResult.Server(listOf(conv("c", 300)))))
            repo.scripts.addLast(listOf(TwoPhaseResult.Server(listOf(conv("d", 200)))))
            connectionListener().onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 4
            ids(vm) shouldBe listOf("c_a", "c_c")
        }
    }

    test("R3: the retry stops when the page fails again, and the next connect retries it") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            failNextPageOffline(vm)
            advanceUntilIdle()
            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(emptyList<Conversation>()), offlineError()))
            val connection = connectionListener()
            connection.onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 3
            repo.scripts.addLast(listOf(TwoPhaseResult.Server(listOf(conv("b", 400)))))
            connection.onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 4
            ids(vm) shouldBe listOf("c_a", "c_b")
        }
    }

    test("R3: a page the cache filled that ends not-connected is not a failure; connect reads nothing") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(listOf(conv("b", 400), conv("c", 300))), offlineError()))
            vm.fetchConversations()
            advanceUntilIdle()
            repo.reads shouldBe 2
            ids(vm) shouldBe listOf("c_a", "c_b", "c_c")

            connectionListener().onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 2
        }
    }

    test("R3: no read on connect when nothing failed, or when the page failed for another reason") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            val connection = connectionListener()
            connection.onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 1

            repo.scripts.addLast(listOf(TwoPhaseResult.Error(CometChatException("ERR_SERVER", "boom"))))
            vm.fetchConversations()
            advanceUntilIdle()
            connection.onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 2
        }
    }

    test("R3: offline off keeps today's reconnect, a refresh through the refresh use case") {
        runTest {
            OfflineGate.testOverride = false
            refreshUseCase = RefreshConversationListUseCase(repo)
            repo.offPage = listOf(conv("a", 500))
            val vm = newViewModel()
            advanceUntilIdle()
            vm.offlineOn shouldBe false
            repo.offReads shouldBe 1
            connectionListener().onConnected()
            advanceUntilIdle()
            repo.offReads shouldBe 2
            repo.reads shouldBe 0
        }
    }

    test("a filtered list re-reads its own request once instead of reconciling the slice") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 300))))
            vm.setConversationsRequestBuilder(ConversationsRequest.ConversationsRequestBuilder().setTags(listOf("t")))
            val gate = CompletableDeferred<Unit>()
            repo.scripts.addLast(listOf(gate, TwoPhaseResult.Server(listOf(conv("a", 300)))))

            val page = listOf(conv("x", 900))
            OfflineHub.publishForTest(OfflineHubEvent.ConversationSyncCompleted(OfflineHub.gen, page))
            OfflineHub.publishForTest(OfflineHubEvent.ConversationSyncCompleted(OfflineHub.gen, page))
            gate.complete(Unit)
            advanceUntilIdle()

            repo.reads shouldBe 2
            ids(vm) shouldBe listOf("c_a")
        }
    }

    test("ConversationSyncFailed does nothing") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 300))))
            val before = vm.conversations.value
            OfflineHub.publishForTest(OfflineHubEvent.ConversationSyncFailed(OfflineHub.gen, CometChatException("E", "e")))
            advanceUntilIdle()
            vm.conversations.value shouldBeSameInstanceAs before
            repo.reads shouldBe 1
        }
    }

    // ---------------------------------------------------------------- pending preview

    test("the pending preview moves the chat to the top marked sending, then failed") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))
            val pending = message("b", 600, id = 0L, sender = me, localId = "l1")

            OfflineHub.publishForTest(OfflineHubEvent.SendState(OfflineHub.gen, "l1", pending, MessageSendState.SENDING, null))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_b", "c_a")
            ConversationPreviewMark.of(vm.conversations.value.first().lastMessage) shouldBe MessageSendState.SENDING

            OfflineHub.publishForTest(
                OfflineHubEvent.SendState(OfflineHub.gen, "l1", pending, MessageSendState.FAILED, MessageFailureReason.TRANSIENT)
            )
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_b", "c_a")
            ConversationPreviewMark.of(vm.conversations.value.first().lastMessage) shouldBe MessageSendState.FAILED
            // The hub's shared message object is never stamped; the row holds its own copy.
            pending.status shouldBe null
        }
    }

    test("a discarded send restores the row's previous preview") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))
            val pending = message("b", 600, id = 0L, sender = me, localId = "l1")
            OfflineHub.publishForTest(OfflineHubEvent.SendState(OfflineHub.gen, "l1", pending, MessageSendState.SENDING, null))
            OfflineHub.publishForTest(
                OfflineHubEvent.SendState(OfflineHub.gen, "l1", pending, MessageSendState.FAILED, MessageFailureReason.DISCARDED)
            )
            advanceUntilIdle()
            vm.conversations.value.first { it.conversationId == "c_b" }.lastMessage!!.sentAt shouldBe 400L
        }
    }

    test("an older server row does not replace the unsent preview (BEH-CONV-06)") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))
            val pending = message("b", 600, id = 0L, sender = me, localId = "l1")
            OfflineHub.publishForTest(OfflineHubEvent.SendState(OfflineHub.gen, "l1", pending, MessageSendState.SENDING, null))
            advanceUntilIdle()

            repo.scripts.addLast(listOf(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400, unread = 0)))))
            vm.refreshList()
            OfflineHub.publishForTest(OfflineHubEvent.ConversationSyncCompleted(OfflineHub.gen, listOf(conv("b", 400))))
            advanceUntilIdle()

            val b = vm.conversations.value.first { it.conversationId == "c_b" }
            b.lastMessage!!.localMessageId shouldBe "l1"
            ConversationPreviewMark.of(b.lastMessage) shouldBe MessageSendState.SENDING
            ids(vm).first() shouldBe "c_b"
        }
    }

    // ---------------------------------------------------------------- unread

    test("no +1 for a swept message; the live +1 stays") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500, unread = 2))))
            val listener = messageListener()

            listener.onTextMessageReceived(message("a", 600, source = MessageSource.SYNC))
            advanceUntilIdle()
            vm.conversations.value.single().unreadMessageCount shouldBe 2
            vm.conversations.value.single().lastMessage!!.sentAt shouldBe 600L

            listener.onTextMessageReceived(message("a", 700, source = MessageSource.REALTIME))
            advanceUntilIdle()
            vm.conversations.value.single().unreadMessageCount shouldBe 3
        }
    }

    test("a swept message older than the shown preview does not demote it") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            messageListener().onTextMessageReceived(message("a", 450, source = MessageSource.SYNC))
            advanceUntilIdle()
            vm.conversations.value.single().lastMessage!!.sentAt shouldBe 500L
        }
    }

    // ---------------------------------------------------------------- pins

    fun stubPin(callbacks: MutableList<CometChat.CallbackListener<Conversation>>) {
        cometChatMock.`when`<Unit> { CometChat.pinConversation(any(), any(), any()) }.thenAnswer { inv ->
            @Suppress("UNCHECKED_CAST")
            callbacks += inv.arguments[2] as CometChat.CallbackListener<Conversation>
            null
        }
        cometChatMock.`when`<Unit> { CometChat.unpinConversation(any(), any(), any()) }.thenAnswer { inv ->
            @Suppress("UNCHECKED_CAST")
            callbacks += inv.arguments[2] as CometChat.CallbackListener<Conversation>
            null
        }
    }

    test("own pin from onSuccess alone (SDK 5.1.2: no echo)") {
        runTest {
            val callbacks = mutableListOf<CometChat.CallbackListener<Conversation>>()
            stubPin(callbacks)
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))

            vm.pinConversation(vm.conversations.value[1])
            callbacks.single().onSuccess(conv("b", 400, pinnedAt = 1_000))
            ids(vm) shouldBe listOf("c_b", "c_a")
            vm.conversations.value.first().pinnedAt shouldBe 1_000L
        }
    }

    test("D-328: a frame that arrives while an own pin is in flight applies; onSuccess is then a no-op") {
        runTest {
            val callbacks = mutableListOf<CometChat.CallbackListener<Conversation>>()
            stubPin(callbacks)
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))

            vm.pinConversation(vm.conversations.value[1])
            // Another device's pin, or an SDK 5.1.0 echo that beat onSuccess: applied, not dropped.
            conversationListener().onConversationPinned(conv("B", 400, pinnedAt = 1_000))
            ids(vm) shouldBe listOf("c_b", "c_a")
            val afterFrame = vm.conversations.value

            callbacks.single().onSuccess(conv("b", 400, pinnedAt = 1_000))
            vm.conversations.value shouldBeSameInstanceAs afterFrame // no second move, no flicker
        }
    }

    test("D-328: a remote unpin that arrives while an own unpin is in flight applies") {
        runTest {
            val callbacks = mutableListOf<CometChat.CallbackListener<Conversation>>()
            stubPin(callbacks)
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("b", 400, pinnedAt = 900), conv("a", 500))))

            vm.unpinConversation(vm.conversations.value.first { it.conversationId == "c_b" })
            conversationListener().onConversationUnpinned(conv("b", 400, pinnedAt = 0))
            vm.conversations.value.first { it.conversationId == "c_b" }.isPinned shouldBe false
            val afterFrame = vm.conversations.value

            callbacks.single().onSuccess(conv("b", 400, pinnedAt = 0))
            vm.conversations.value shouldBeSameInstanceAs afterFrame
        }
    }

    test("an SDK 5.1.0 echo after onSuccess is a no-op") {
        runTest {
            val callbacks = mutableListOf<CometChat.CallbackListener<Conversation>>()
            stubPin(callbacks)
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))
            val listener = conversationListener()

            vm.pinConversation(vm.conversations.value[1])
            callbacks[0].onSuccess(conv("b", 400, pinnedAt = 1_000))
            val afterPin = vm.conversations.value
            listener.onConversationPinned(conv("b", 400, pinnedAt = 1_000))
            vm.conversations.value shouldBeSameInstanceAs afterPin

            vm.unpinConversation(vm.conversations.value.first())
            callbacks[1].onSuccess(conv("b", 400, pinnedAt = 0))
            val afterUnpin = vm.conversations.value
            afterUnpin.first { it.conversationId == "c_b" }.isPinned shouldBe false
            listener.onConversationUnpinned(conv("b", 400, pinnedAt = 0))
            vm.conversations.value shouldBeSameInstanceAs afterUnpin
        }
    }

    test("D-328: unpin, re-pin, then another device's unpin is applied") {
        runTest {
            val callbacks = mutableListOf<CometChat.CallbackListener<Conversation>>()
            stubPin(callbacks)
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("b", 400, pinnedAt = 900), conv("a", 500))))
            val listener = conversationListener()

            vm.unpinConversation(vm.conversations.value.first { it.conversationId == "c_b" })
            callbacks[0].onSuccess(conv("b", 400, pinnedAt = 0))
            vm.pinConversation(vm.conversations.value.first { it.conversationId == "c_b" })
            callbacks[1].onSuccess(conv("b", 400, pinnedAt = 1_000))
            vm.conversations.value.first { it.conversationId == "c_b" }.isPinned shouldBe true

            listener.onConversationUnpinned(conv("b", 400, pinnedAt = 0)) // device B
            vm.conversations.value.first { it.conversationId == "c_b" }.isPinned shouldBe false
        }
    }

    test("another device's pin event matches by (type, with), case-insensitive") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("bob", 400))))
            conversationListener().onConversationPinned(
                conv("BOB", 400, pinnedAt = 2_000).apply { conversationId = "user_BOB" }
            )
            ids(vm) shouldBe listOf("c_bob", "c_a")
            vm.conversations.value.first().pinnedAt shouldBe 2_000L
        }
    }

    test("a 1:1 pin event whose peer is the user is ignored") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            val before = vm.conversations.value
            conversationListener().onConversationPinned(conv("ME", 400, pinnedAt = 2_000))
            vm.conversations.value shouldBeSameInstanceAs before
        }
    }

    test("a filtered list only updates a row it shows, in place") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))
            vm.setConversationsRequestBuilder(ConversationsRequest.ConversationsRequestBuilder().setPinnedBy("me"))
            val listener = conversationListener()
            listener.onConversationPinned(conv("b", 400, pinnedAt = 2_000))
            ids(vm) shouldBe listOf("c_a", "c_b")
            vm.conversations.value[1].pinnedAt shouldBe 2_000L
            listener.onConversationPinned(conv("zed", 100, pinnedAt = 3_000))
            ids(vm) shouldBe listOf("c_a", "c_b")
        }
    }

    // ---------------------------------------------------------------- sweep actions, reset

    test("a self kick in a sweep drops the group row as the live event does (B2)") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("g1", 500, group = true), conv("a", 400))))
            val kick = Action().apply {
                action = CometChatConstants.ActionKeys.ACTION_KICKED
                actionOn = user(me)
                actionBy = user("admin")
                receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
                receiverUid = "g1"
                receiver = Group("g1", "g1", CometChatConstants.GROUP_TYPE_PUBLIC, null)
                conversationId = "group_g1"
                sentAt = 600
            }
            val otherKick = Action().apply {
                action = CometChatConstants.ActionKeys.ACTION_KICKED
                actionOn = user("someone")
                receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
                receiverUid = "g1"
                conversationId = "group_g1"
            }
            OfflineHub.publishForTest(OfflineHubEvent.MessageSyncCompleted(OfflineHub.gen, listOf(otherKick), SyncOutcome.CAUGHT_UP))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("group_g1", "c_a")

            OfflineHub.publishForTest(OfflineHubEvent.MessageSyncCompleted(OfflineHub.gen, listOf(kick), SyncOutcome.CAUGHT_UP))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a")
        }
    }

    test("cache reset clears the list, bumps the generation and reloads two-phase") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            val gen = vm.offlineListGen
            val gate = CompletableDeferred<Unit>()
            repo.scripts.addLast(listOf(gate, TwoPhaseResult.Cached(listOf(conv("n", 900))), TwoPhaseResult.Server(listOf(conv("n", 900)))))

            OfflineHub.publishForTest(OfflineHubEvent.CacheReset(OfflineHub.gen, OfflineCacheResetReason.values().first(), 0))
            advanceUntilIdle()
            vm.conversations.value.shouldBeEmpty()
            vm.offlineListGen shouldBe gen + 1

            gate.complete(Unit)
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_n")
            repo.reads shouldBe 2
        }
    }

    // ---------------------------------------------------------------- review fixes

    test("an echo that arrived in flight leaves nothing behind; another device's later event applies") {
        runTest {
            val callbacks = mutableListOf<CometChat.CallbackListener<Conversation>>()
            stubPin(callbacks)
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))
            val listener = conversationListener()

            vm.pinConversation(vm.conversations.value[1])
            listener.onConversationPinned(conv("b", 400, pinnedAt = 1_000))
            callbacks.single().onSuccess(conv("b", 400, pinnedAt = 1_000))

            listener.onConversationUnpinned(conv("b", 400, pinnedAt = 0))
            vm.conversations.value.first { it.conversationId == "c_b" }.isPinned shouldBe false
        }
    }

    fun groupAction(kind: String, target: String, at: Long, byTarget: Boolean = false) = Action().apply {
        action = kind
        if (byTarget) actionBy = user(target) else { actionOn = user(target); actionBy = user("admin") }
        receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
        receiverUid = "g1"
        receiver = Group("g1", "g1", CometChatConstants.GROUP_TYPE_PUBLIC, null)
        conversationId = "group_g1"
        sentAt = at
        id = at
    }

    test("B2: a self kick later undone in the same batch keeps the row") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("g1", 500, group = true), conv("a", 400))))
            val batch = listOf(
                groupAction(CometChatConstants.ActionKeys.ACTION_KICKED, me, 600),
                groupAction(CometChatConstants.ActionKeys.ACTION_MEMBER_ADDED, me, 700)
            )
            OfflineHub.publishForTest(OfflineHubEvent.MessageSyncCompleted(OfflineHub.gen, batch, SyncOutcome.CAUGHT_UP))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("group_g1", "c_a")
        }
    }

    test("B2: a self kick older than the row's last message keeps the row") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("g1", 800, group = true), conv("a", 400))))
            val batch = listOf(groupAction(CometChatConstants.ActionKeys.ACTION_BANNED, me, 600))
            OfflineHub.publishForTest(OfflineHubEvent.MessageSyncCompleted(OfflineHub.gen, batch, SyncOutcome.CAUGHT_UP))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("group_g1", "c_a")
        }
    }

    test("discarding the newer of two queued sends after the older one is acked restores the acked message") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))
            val first = message("b", 600, id = 0L, sender = me, localId = "l1")
            val second = message("b", 700, id = 0L, sender = me, localId = "l2")
            val acked = message("b", 600, id = 9_001L, sender = me, localId = "l1")
            OfflineHub.publishForTest(OfflineHubEvent.SendState(OfflineHub.gen, "l1", first, MessageSendState.SENDING, null))
            OfflineHub.publishForTest(OfflineHubEvent.SendState(OfflineHub.gen, "l2", second, MessageSendState.SENDING, null))
            OfflineHub.publishForTest(OfflineHubEvent.SendState(OfflineHub.gen, "l1", acked, MessageSendState.SENT, null))
            OfflineHub.publishForTest(
                OfflineHubEvent.SendState(OfflineHub.gen, "l2", second, MessageSendState.FAILED, MessageFailureReason.DISCARDED)
            )
            advanceUntilIdle()

            val b = vm.conversations.value.first { it.conversationId == "c_b" }
            b.lastMessage!!.id shouldBe 9_001L
            ConversationPreviewMark.of(b.lastMessage) shouldBe null
        }
    }

    test("a live edit of an older message does not replace a failed unsent preview") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))
            val pending = message("b", 600, id = 0L, sender = me, localId = "l1")
            OfflineHub.publishForTest(
                OfflineHubEvent.SendState(OfflineHub.gen, "l1", pending, MessageSendState.FAILED, MessageFailureReason.TRANSIENT)
            )
            advanceUntilIdle()

            val edited = message("b", 400).apply { editedAt = 650 }
            messageListener().onMessageEdited(edited)
            messageListener().onMessageDeleted(message("b", 400).apply { deletedAt = 660 })
            advanceUntilIdle()

            val b = vm.conversations.value.first { it.conversationId == "c_b" }
            b.lastMessage!!.localMessageId shouldBe "l1"
            ConversationPreviewMark.of(b.lastMessage) shouldBe MessageSendState.FAILED
        }
    }

    // ---------------------------------------------------------------- explicit refresh replaces (ruling 2026-11-04)

    test("refresh after a filter change: cached page first, then the server page replaces the list") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("g1", 600, group = true))))
            vm.setConversationsRequestBuilder(ConversationsRequest.ConversationsRequestBuilder().setConversationType("user"))
            val gate = CompletableDeferred<Unit>()
            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(listOf(conv("a", 500))), gate, TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400)))))

            vm.refreshList()
            ids(vm) shouldBe listOf("c_a")
            gate.complete(Unit)
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a", "c_b")
        }
    }

    test("refresh keeps an own unsent preview the server page lacks") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))
            val pending = message("c", 600, id = 0L, sender = me, localId = "l1")
            OfflineHub.publishForTest(OfflineHubEvent.SendState(OfflineHub.gen, "l1", pending, MessageSendState.SENDING, null))
            advanceUntilIdle()

            repo.scripts.addLast(listOf(TwoPhaseResult.Server(listOf(conv("a", 500)))))
            vm.refreshList()
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_c", "c_a")
            ConversationPreviewMark.of(vm.conversations.value.first().lastMessage) shouldBe MessageSendState.SENDING
        }
    }

    test("refresh whose server read fails keeps the cached page plus unsent previews") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))
            val pending = message("b", 600, id = 0L, sender = me, localId = "l1")
            OfflineHub.publishForTest(OfflineHubEvent.SendState(OfflineHub.gen, "l1", pending, MessageSendState.SENDING, null))
            advanceUntilIdle()

            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(listOf(conv("x", 900))), TwoPhaseResult.Error(CometChatException("ERR", "offline"))))
            vm.refreshList()
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_x", "c_b")
            vm.conversations.value[1].lastMessage!!.localMessageId shouldBe "l1"
        }
    }

    test("GroupCreated still merges: nothing shown is dropped") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))
            repo.scripts.addLast(listOf(TwoPhaseResult.Server(listOf(conv("g2", 700, group = true)))))
            com.cometchat.uikit.core.events.CometChatEvents.emitGroupEventSync(
                com.cometchat.uikit.core.events.CometChatGroupEvent.GroupCreated(Group("g2", "g2", CometChatConstants.GROUP_TYPE_PUBLIC, null))
            )
            advanceUntilIdle()
            ids(vm) shouldBe listOf("group_g2", "c_a", "c_b")
        }
    }

    test("filtered refresh: an empty server page keeps the cached page it showed (D17)") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("g1", 600, group = true))))
            vm.setConversationsRequestBuilder(ConversationsRequest.ConversationsRequestBuilder().setConversationType("user"))
            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(listOf(conv("a", 500))), TwoPhaseResult.Server(emptyList<Conversation>())))
            vm.refreshList()
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a")
        }
    }

    test("filtered refresh with no cached phase: an empty server page drops the old-filter rows") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("g1", 600, group = true))))
            vm.setConversationsRequestBuilder(ConversationsRequest.ConversationsRequestBuilder().setConversationType("user"))
            repo.scripts.addLast(listOf(TwoPhaseResult.Server(emptyList<Conversation>())))
            vm.refreshList()
            advanceUntilIdle()
            vm.conversations.value.shouldBeEmpty()
        }
    }

    // ENG-41459 R1 / BEH-SYNC-15: every live row write after connect spares that row from the sweep page.
    fun liveWriteSetup(): CometChatConversationsViewModel {
        ConversationListOffline.sweepPageSizeOverride = 10
        val settings = mock<ConversationUpdateSettings>()
        whenever(settings.shouldUpdateOnGroupActions()).thenReturn(true)
        whenever(settings.shouldUpdateOnCallActivities()).thenReturn(true)
        whenever(settings.shouldUpdateOnMessageReplies()).thenReturn(true)
        cometChatMock.`when`<ConversationUpdateSettings?> { CometChat.getConversationUpdateSettings() }.thenReturn(settings)
        val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400))))
        connectionListener().onConnected()
        return vm
    }

    fun completeSync() = sync(listOf(conv("a", 500)))

    test("live write: a group the user was added to after connect survives a complete page") {
        runTest {
            val vm = liveWriteSetup()
            val group = Group("g1", "g1", CometChatConstants.GROUP_TYPE_PUBLIC, null)
            captureListener(CometChat.GroupListener::class.java) { CometChat.addGroupListener(any(), it.capture()) }
                .onMemberAddedToGroup(groupAction("added", me, 900), user("admin"), user(me), group)
            advanceUntilIdle()
            ids(vm).contains("group_g1") shouldBe true

            completeSync()
            advanceUntilIdle()
            ids(vm) shouldBe listOf("group_g1", "c_a")
        }
    }

    // 6.2.0 (Ringing v2): a call reaches the list only through `updateConversationWithCall`, from
    // the Calls SDK listener or the kit's call events, and only when the Call carries the call
    // message id (`CallInitiationUtils.toCall` takes it from `InitiatedCall.messageId`). That path
    // ends in `updateConversation(live = true)`, so it is a live write. The kit event stands in
    // for both routes here; the Calls SDK listener is not mockable in core unit tests.
    test("live write: a call (with its message id) from a new contact after connect survives a complete page") {
        runTest {
            val vm = liveWriteSetup()
            val call = Call("caller", CometChatConstants.RECEIVER_TYPE_USER, CometChatConstants.CALL_TYPE_AUDIO).apply {
                sentAt = 900
                id = 900
                sender = user("caller")
                receiver = user("caller")
                conversationId = "c_caller"
            }
            CometChatEvents.emitCallEventSync(com.cometchat.uikit.core.events.CometChatCallEvent.CallEnded(call))
            advanceUntilIdle()
            ids(vm).contains("c_caller") shouldBe true

            completeSync()
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_caller", "c_a")
        }
    }

    test("a swept (SYNC) message is not a live write: its row is not spared") {
        runTest {
            val vm = liveWriteSetup()
            messageListener().onTextMessageReceived(message("s", 900, source = MessageSource.SYNC))
            advanceUntilIdle()
            ids(vm).contains("c_s") shouldBe true

            completeSync()
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a")
        }
    }

    test("live write: an edit after connect spares its row") {
        runTest {
            val vm = liveWriteSetup()
            messageListener().onMessageEdited(message("b", 400).apply { editedAt = 450 })
            advanceUntilIdle()

            completeSync()
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a", "c_b")
        }
    }

    test("live write: the ack of an own send after connect spares its row") {
        runTest {
            val vm = liveWriteSetup()
            val pending = message("o", 900, id = 0L, sender = me, localId = "l-o")
            val acked = message("o", 900, id = 901, sender = me, localId = "l-o")
            OfflineHub.publishForTest(OfflineHubEvent.SendState(OfflineHub.gen, "l-o", pending, MessageSendState.SENDING, null))
            OfflineHub.publishForTest(OfflineHubEvent.SendState(OfflineHub.gen, "l-o", acked, MessageSendState.SENT, null))
            advanceUntilIdle()
            vm.conversations.value.first { it.conversationId == "c_o" }.lastMessage!!.id shouldBe 901L

            completeSync()
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_o", "c_a")
        }
    }

    test("live write: a pin after connect is not undone by a truncated page") {
        runTest {
            ConversationListOffline.sweepPageSizeOverride = null
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500), conv("bob", 400))))
            connectionListener().onConnected()
            conversationListener().onConversationPinned(
                conv("bob", 400, pinnedAt = 2_000).apply { conversationId = "user_bob" }
            )
            advanceUntilIdle()

            // The page was read before the pin: it carries no pinned block.
            sync(listOf(conv("a", 500)))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_bob", "c_a")
            vm.conversations.value.first().isPinned shouldBe true
        }
    }

    // ---------------------------------------------------------------- D83, ENG-41551 G

    fun collectErrors(vm: CometChatConversationsViewModel): Pair<List<CometChatException>, kotlinx.coroutines.Job> {
        val errors = mutableListOf<CometChatException>()
        val job = kotlinx.coroutines.CoroutineScope(testDispatcher).launch { vm.rowsKeptError.collect { errors += it } }
        return errors to job
    }

    test("D84 rules 3, 8: offline with nothing shown is the offline state; connect reloads it once") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Cached(emptyList<Conversation>()), offlineError())
            advanceUntilIdle()
            (vm.uiState.value as com.cometchat.uikit.core.state.UIState.Error).exception.code shouldBe "ERROR_INTERNET_UNAVAILABLE"
            vm.connectToLoad.value shouldBe true
            vm.connectToSearch.value shouldBe false

            repo.scripts.addLast(listOf(TwoPhaseResult.Server(listOf(conv("a", 500)))))
            connectionListener().onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 2
            ids(vm) shouldBe listOf("c_a")
            (vm.uiState.value is com.cometchat.uikit.core.state.UIState.Content) shouldBe true
            vm.connectToLoad.value shouldBe false

            connectionListener().onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 2
        }
    }

    test("D84 rule 4: any other failure with nothing shown is the error view; connect reads nothing") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Error(CometChatException("ERR_SERVER", "boom")))
            advanceUntilIdle()
            (vm.uiState.value is com.cometchat.uikit.core.state.UIState.Error) shouldBe true
            vm.connectToLoad.value shouldBe false
            connectionListener().onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 1
        }
    }

    test("D84 rules 7, 8: an offline searchKeyword refresh with nothing saved is Connect to search; connect reloads it once") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            advanceUntilIdle()
            val (errors, job) = collectErrors(vm)
            vm.setConversationsRequestBuilder(ConversationsRequest.ConversationsRequestBuilder().setSearchKeyword("al"))
            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(emptyList<Conversation>()), offlineError()))
            vm.refreshList()
            advanceUntilIdle()
            vm.conversations.value.shouldBeEmpty()
            vm.uiState.value shouldBe com.cometchat.uikit.core.state.UIState.Empty
            vm.connectToSearch.value shouldBe true
            vm.connectToLoad.value shouldBe false
            errors.shouldBeEmpty()
            job.cancel()
            val readsBefore = repo.reads

            repo.scripts.addLast(listOf(TwoPhaseResult.Server(listOf(conv("al", 600)))))
            connectionListener().onConnected()
            advanceUntilIdle()
            repo.reads shouldBe readsBefore + 1
            ids(vm) shouldBe listOf("c_al")

            connectionListener().onConnected()
            advanceUntilIdle()
            repo.reads shouldBe readsBefore + 1
        }
    }

    test("D84 rule 2: an offline error after cached rows keeps them, shows no error state, reports nothing") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val vm = newViewModel(TwoPhaseResult.Cached(listOf(conv("a", 500))), gate, offlineError())
            val (errors, job) = collectErrors(vm)
            gate.complete(Unit)
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a")
            (vm.uiState.value is com.cometchat.uikit.core.state.UIState.Content) shouldBe true
            errors.shouldBeEmpty()
            job.cancel()
        }
    }

    test("D84 rule 5: an online error after cached rows keeps them and reports it once") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val vm = newViewModel(TwoPhaseResult.Cached(listOf(conv("a", 500))), gate, serverError())
            val (errors, job) = collectErrors(vm)
            gate.complete(Unit)
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a")
            errors.map { it.code } shouldBe listOf("ERR_SERVER")
            job.cancel()
        }
    }

    test("D83: a failed load-more page keeps the rows and reports no onError") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            val (errors, job) = collectErrors(vm)
            advanceUntilIdle()
            repo.scripts.addLast(listOf(offlineError()))
            vm.fetchConversations()
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a")
            errors shouldBe emptyList()
            job.cancel()
        }
    }

    test("D84 rule 5: a failing open, then scrolling cached pages, reports onError once (the open), not per load-more") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val vm = newViewModel(TwoPhaseResult.Cached(listOf(conv("a", 500))), gate, serverError())
            val (errors, job) = collectErrors(vm)
            gate.complete(Unit)
            advanceUntilIdle()
            errors.size shouldBe 1

            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(listOf(conv("b", 400))), serverError()))
            vm.fetchConversations()
            advanceUntilIdle()
            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(listOf(conv("c", 300))), serverError()))
            vm.fetchConversations()
            advanceUntilIdle()

            repo.reads shouldBe 3
            ids(vm) shouldBe listOf("c_a", "c_b", "c_c")
            errors.size shouldBe 1
            job.cancel()
        }
    }

    test("D84: the init read's online failure is delivered to a view that collects later, once") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Cached(listOf(conv("a", 500))), serverError())
            advanceUntilIdle()
            val (first, job1) = collectErrors(vm)
            first.map { it.code } shouldBe listOf("ERR_SERVER")
            job1.cancel()
            val (second, job2) = collectErrors(vm)
            second shouldBe emptyList()
            job2.cancel()
        }
    }

    test("D83: an empty server page is the empty state") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Cached(emptyList<Conversation>()), TwoPhaseResult.Server(emptyList<Conversation>()))
            advanceUntilIdle()
            vm.uiState.value shouldBe com.cometchat.uikit.core.state.UIState.Empty
        }
    }

    test("ENG-41551 G: a load-more asked during the first page is deferred and runs once it ends") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val vm = newViewModel(
                TwoPhaseResult.Cached(listOf(conv("a", 500))),
                gate,
                TwoPhaseResult.Server(listOf(conv("a", 500), conv("b", 400)))
            )
            repo.scripts.addLast(listOf(TwoPhaseResult.Server(listOf(conv("c", 300)))))
            vm.fetchConversations() // the first page is still in flight
            repo.reads shouldBe 1

            gate.complete(Unit)
            advanceUntilIdle()
            repo.reads shouldBe 2
            ids(vm) shouldBe listOf("c_a", "c_b", "c_c")
        }
    }

    test("ENG-41551 G: a first page that fails drops the deferred load-more (no Error → Loading → Error)") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val vm = newViewModel(gate, TwoPhaseResult.Error(CometChatException("ERR_SERVER", "boom")))
            repo.scripts.addLast(listOf(TwoPhaseResult.Server(listOf(conv("c", 300)))))
            vm.fetchConversations() // deferred while the first page is in flight
            gate.complete(Unit)
            advanceUntilIdle()
            repo.reads shouldBe 1
            (vm.uiState.value is com.cometchat.uikit.core.state.UIState.Error) shouldBe true
        }
    }

    test("ENG-41551 G: a load-more during a later page is still dropped, as today") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            advanceUntilIdle()
            val gate = CompletableDeferred<Unit>()
            repo.scripts.addLast(listOf(gate, TwoPhaseResult.Server(listOf(conv("b", 400)))))
            vm.fetchConversations() // page 2, in flight
            vm.fetchConversations() // dropped
            gate.complete(Unit)
            advanceUntilIdle()
            repo.reads shouldBe 2
        }
    }

    // ---------------------------------------------------------------- D83 amended 2026-11-12 ("keep it quiet")

    test("D83 amended: a sweep resync failing over rows keeps them and reports nothing") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 300))))
            vm.setConversationsRequestBuilder(ConversationsRequest.ConversationsRequestBuilder().setTags(listOf("t")))
            advanceUntilIdle()
            val (errors, job) = collectErrors(vm)
            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(listOf(conv("a", 300))), offlineError()))

            OfflineHub.publishForTest(OfflineHubEvent.ConversationSyncCompleted(OfflineHub.gen, listOf(conv("x", 900))))
            advanceUntilIdle()

            repo.reads shouldBe 2
            ids(vm) shouldBe listOf("c_a")
            (vm.uiState.value is com.cometchat.uikit.core.state.UIState.Content) shouldBe true
            errors.shouldBeEmpty()
            job.cancel()
        }
    }

    test("D83 amended: the GroupCreated read failing over rows reports nothing") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            advanceUntilIdle()
            val (errors, job) = collectErrors(vm)
            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(listOf(conv("a", 500))), offlineError()))
            com.cometchat.uikit.core.events.CometChatEvents.emitGroupEventSync(
                com.cometchat.uikit.core.events.CometChatGroupEvent.GroupCreated(Group("g2", "g2", CometChatConstants.GROUP_TYPE_PUBLIC, null))
            )
            advanceUntilIdle()
            repo.reads shouldBe 2
            ids(vm) shouldBe listOf("c_a")
            errors.shouldBeEmpty()
            job.cancel()
        }
    }

    test("D84 rule 5: a refresh the app starts, failing online over rows, still reports once") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            advanceUntilIdle()
            val (errors, job) = collectErrors(vm)
            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(listOf(conv("a", 500))), serverError()))
            vm.refreshList()
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a")
            errors.map { it.code } shouldBe listOf("ERR_SERVER")
            job.cancel()
        }
    }

    test("D84 rule 2: a refresh the app starts, failing offline over rows, reports nothing") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500))))
            advanceUntilIdle()
            val (errors, job) = collectErrors(vm)
            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(listOf(conv("a", 500))), offlineError()))
            vm.refreshList()
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_a")
            errors.shouldBeEmpty()
            job.cancel()
        }
    }

    test("D83 amended: the reconnect reload from the error view is quiet over rows, the error state without") {
        runTest {
            val vm = newViewModel(TwoPhaseResult.Cached(emptyList<Conversation>()), offlineError())
            advanceUntilIdle()
            (vm.uiState.value is com.cometchat.uikit.core.state.UIState.Error) shouldBe true
            val (errors, job) = collectErrors(vm)

            // The reload fails with nothing to show: the error state again.
            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(emptyList<Conversation>()), offlineError()))
            connectionListener().onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 2
            (vm.uiState.value as com.cometchat.uikit.core.state.UIState.Error).exception.code shouldBe "ERROR_INTERNET_UNAVAILABLE"

            // The next reload paints cached rows, then fails: the rows stay and nothing is reported.
            repo.scripts.addLast(listOf(TwoPhaseResult.Cached(listOf(conv("a", 500))), offlineError()))
            connectionListener().onConnected()
            advanceUntilIdle()
            repo.reads shouldBe 3
            ids(vm) shouldBe listOf("c_a")
            (vm.uiState.value is com.cometchat.uikit.core.state.UIState.Content) shouldBe true
            errors.shouldBeEmpty()
            job.cancel()
        }
    }

    // ---------------------------------------------------------------- ENG-41643 (D-369): offline read

    fun read(message: BaseMessage) = runBlocking {
        CometChatEvents.emitMessageEventSync(CometChatMessageEvent.MessageRead(message))
    }

    /** Captures every server refresh the list starts; the test answers them. */
    fun captureRefreshes(): MutableList<Triple<String, String, CometChat.ConversationDetailsListener>> {
        val calls = mutableListOf<Triple<String, String, CometChat.ConversationDetailsListener>>()
        cometChatMock.`when`<Unit> {
            CometChat.getConversation(any(), any(), any<CometChat.ConversationDetailsListener>())
        }.thenAnswer { inv ->
            calls += Triple(inv.getArgument(0), inv.getArgument(1), inv.getArgument<CometChat.ConversationDetailsListener>(2))
            null
        }
        return calls
    }

    fun row(vm: CometChatConversationsViewModel, id: String) = vm.conversations.value.first { it.conversationId == id }

    /**
     * The read the offline flush sends (revision 2: only this read may refresh): remembered, flushed,
     * and its `markAsRead` answered with success, which marks it and emits `MessageRead`.
     */
    fun flushedRead(message: BaseMessage) {
        OfflineReadLedger.remember(message)
        OfflineReadLedger.flush()
        val callback = org.mockito.kotlin.argumentCaptor<CometChat.CallbackListener<Void>>()
        cometChatMock.verify(
            { CometChat.markAsRead(org.mockito.kotlin.eq(message), callback.capture()) },
            org.mockito.kotlin.atLeastOnce()
        )
        callback.lastValue.onSuccess(null)
    }

    test("offlineRead_olderThanOthersLastMessage_refreshesCountFromServer_notZero") {
        runTest {
            val refreshes = captureRefreshes()
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500, unread = 3))))
            advanceUntilIdle()

            flushedRead(message("a", 400))
            advanceUntilIdle()
            row(vm, "c_a").unreadMessageCount shouldBe 3 // not zeroed while the server answers
            refreshes.size shouldBe 1
            refreshes.single().first shouldBe "a"
            refreshes.single().second shouldBe CometChatConstants.CONVERSATION_TYPE_USER

            // The cached phase carries the SDK's locally zeroed count (BEH-CONV-04): ignored.
            refreshes.single().third.onCachedConversation(conv("a", 500, unread = 0))
            row(vm, "c_a").unreadMessageCount shouldBe 3
            refreshes.single().third.onServerConversation(conv("a", 500, unread = 1))
            advanceUntilIdle()
            row(vm, "c_a").unreadMessageCount shouldBe 1
            refreshes.size shouldBe 1 // one refresh per read, no list refetch
            repo.reads shouldBe 1
        }
    }

    test("offlineRead_newestMessage_zeroesAsToday") {
        runTest {
            val refreshes = captureRefreshes()
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500, unread = 3))))
            advanceUntilIdle()

            flushedRead(message("a", 500))
            advanceUntilIdle()
            row(vm, "c_a").unreadMessageCount shouldBe 0
            refreshes.shouldBeEmpty()
        }
    }

    test("offlineRead_newerLastMessageIsOwn_zeroesAsToday") {
        runTest {
            val refreshes = captureRefreshes()
            val vm = newViewModel(
                TwoPhaseResult.Server(listOf(conv("a", 500, unread = 3, last = message("a", 500, sender = me))))
            )
            advanceUntilIdle()

            flushedRead(message("a", 400))
            advanceUntilIdle()
            row(vm, "c_a").unreadMessageCount shouldBe 0
            refreshes.shouldBeEmpty()
        }
    }

    test("offlineRead_threadReply_behavesAsToday") {
        runTest {
            val refreshes = captureRefreshes()
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500, unread = 3))))
            advanceUntilIdle()

            flushedRead(message("a", 400).apply { parentMessageId = 10L })
            advanceUntilIdle()
            row(vm, "c_a").unreadMessageCount shouldBe 0 // today's clear, unchanged
            refreshes.shouldBeEmpty()
        }
    }

    test("offlineRead_refreshFails_countUnchanged") {
        runTest {
            val refreshes = captureRefreshes()
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500, unread = 3))))
            advanceUntilIdle()

            flushedRead(message("a", 400))
            // Offline: the SDK delivers its locally zeroed cached row, then the server phase fails.
            refreshes.single().third.onCachedConversation(conv("a", 500, unread = 0))
            refreshes.single().third.onError(
                CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")
            )
            advanceUntilIdle()
            row(vm, "c_a").unreadMessageCount shouldBe 3
            refreshes.size shouldBe 1
        }
    }

    test("offlineRead_laterZeroingReadBeforeRefreshAnswers_staysZero") {
        runTest {
            val refreshes = captureRefreshes()
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500, unread = 3))))
            advanceUntilIdle()

            flushedRead(message("a", 400))
            read(message("a", 500)) // the user reads the newest message: zeroed at once
            advanceUntilIdle()
            row(vm, "c_a").unreadMessageCount shouldBe 0

            refreshes.single().third.onServerConversation(conv("a", 500, unread = 1)) // the stale answer
            advanceUntilIdle()
            row(vm, "c_a").unreadMessageCount shouldBe 0
        }
    }

    test("offlineRead_refreshDoesNotMoveRowOrChangeLastMessage") {
        runTest {
            val refreshes = captureRefreshes()
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("b", 600), conv("a", 500, unread = 3))))
            advanceUntilIdle()
            flushedRead(message("a", 400))
            // The server row carries a newer last message and timestamp: only its count is taken.
            refreshes.single().third.onServerConversation(conv("a", 700, unread = 2))
            advanceUntilIdle()
            ids(vm) shouldBe listOf("c_b", "c_a")
            row(vm, "c_a").unreadMessageCount shouldBe 2
            row(vm, "c_a").lastMessage!!.id shouldBe 500L
            row(vm, "c_a").lastMessage!!.sentAt shouldBe 500L
            row(vm, "c_a").updatedAt shouldBe 500L
        }
    }

    test("offlineRead_refreshAnswerAfterSessionChange_ignored") {
        runTest {
            val refreshes = captureRefreshes()
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500, unread = 3))))
            advanceUntilIdle()

            flushedRead(message("a", 400))
            refreshes.size shouldBe 1
            OfflineHub.onLogout() // the session ends while the refresh is in flight
            advanceUntilIdle()
            vm.conversations.value.shouldBeEmpty()

            // The next session shows the same chat again, at the new gen.
            OfflineHub.publishForTest(OfflineHubEvent.ConversationSyncCompleted(OfflineHub.gen, listOf(conv("a", 500, unread = 3))))
            advanceUntilIdle()
            row(vm, "c_a").unreadMessageCount shouldBe 3

            refreshes.single().third.onServerConversation(conv("a", 500, unread = 1)) // the old session's answer
            advanceUntilIdle()
            row(vm, "c_a").unreadMessageCount shouldBe 3
        }
    }

    test("offlineRead_refreshAnswerAfterCacheReset_ignored") {
        runTest {
            val refreshes = captureRefreshes()
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500, unread = 3))))
            advanceUntilIdle()

            flushedRead(message("a", 400))
            refreshes.size shouldBe 1
            // The cache reset clears the list and reloads it; the reload shows the chat again.
            repo.scripts.addLast(listOf(TwoPhaseResult.Server(listOf(conv("a", 500, unread = 3)))))
            OfflineHub.publishForTest(OfflineHubEvent.CacheReset(OfflineHub.gen, OfflineCacheResetReason.values().first(), 0))
            advanceUntilIdle()
            row(vm, "c_a").unreadMessageCount shouldBe 3

            refreshes.single().third.onServerConversation(conv("a", 500, unread = 1)) // answer from before the reset
            advanceUntilIdle()
            row(vm, "c_a").unreadMessageCount shouldBe 3
        }
    }

    test("nonFlushRead_olderThanOthersLastMessage_zeroesAsToday") {
        runTest {
            val refreshes = captureRefreshes()
            val vm = newViewModel(TwoPhaseResult.Server(listOf(conv("a", 500, unread = 3))))
            advanceUntilIdle()

            // A message-list read (own-last-message or receipts-disabled branch, or a markAsRead
            // success) that the flush did not send: today's zero, offline on.
            read(message("a", 400))
            advanceUntilIdle()
            row(vm, "c_a").unreadMessageCount shouldBe 0
            refreshes.shouldBeEmpty()
        }
    }

    test("offlineOff_olderRead_zeroesAsToday") {
        runTest {
            OfflineGate.testOverride = false
            val refreshes = captureRefreshes()
            repo.offPage = listOf(conv("a", 500, unread = 3))
            val vm = newViewModel()
            advanceUntilIdle()
            vm.offlineOn shouldBe false

            flushedRead(message("a", 400)) // even a read carrying the flush marker
            advanceUntilIdle()
            row(vm, "c_a").unreadMessageCount shouldBe 0
            refreshes.shouldBeEmpty()
            vm.offlineConversationState.readRefreshSeq shouldBe 0L // the offline hook never ran
        }
    }
})
