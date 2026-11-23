package com.cometchat.uikit.core.offline

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.OfflineCacheResetListener
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.MediaDownloadListener
import com.cometchat.chat.models.MessageFailureReason
import com.cometchat.chat.models.OfflineCacheResetReason
import com.cometchat.chat.models.SyncOutcome
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatOfflineEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import java.util.Collections

/**
 * [OfflineHub]: single registration; inert while off; every SDK callback → its event; forward-only
 * send transitions with exactly one public event each; captured `offlineOn` in `reportSend`; state
 * pruning; a lossless, ordered 300-event burst on both flows; gen/session handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineHubTest : FunSpec({

    lateinit var cometChatMock: MockedStatic<CometChat>

    fun own(localId: String): BaseMessage =
        TextMessage("r1", "hi", CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.localMessageId = localId
            this.sender = User().apply { uid = "me" }
        }

    fun startAndCaptureOutbox(): CometChat.OfflineMessageListener {
        val captor = argumentCaptor<CometChat.OfflineMessageListener>()
        OfflineHub.start()
        cometChatMock.verify { CometChat.addOfflineMessageListener(any(), captor.capture()) }
        return captor.firstValue
    }

    /** Collects both flows inline (unconfined) while [block] runs. */
    fun collectBoth(block: () -> Unit): Pair<List<OfflineHubEvent>, List<CometChatOfflineEvent>> {
        val hub = mutableListOf<OfflineHubEvent>()
        val pub = mutableListOf<CometChatOfflineEvent>()
        runTest(UnconfinedTestDispatcher()) {
            val j1 = launch { OfflineHub.events.collect { hub += it } }
            val j2 = launch { CometChatEvents.offlineEvents.collect { pub += it } }
            block()
            j1.cancel(); j2.cancel()
        }
        return hub to pub
    }

    beforeTest {
        OfflineHub.resetForTest()
        OfflineGate.testOverride = null
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
    }

    afterTest {
        cometChatMock.close()
        OfflineGate.testOverride = null
    }

    test("start registers each listener exactly once, and a second start registers nothing") {
        OfflineHub.start()
        OfflineHub.start()
        cometChatMock.verify({ CometChat.addOfflineMessageListener(any(), any()) }, times(1))
        cometChatMock.verify({ CometChat.addSyncListener(any(), any()) }, times(1))
        cometChatMock.verify({ CometChat.addOfflineCacheResetListener(any(), any()) }, times(1))
        cometChatMock.verify({ CometChat.addMediaDownloadListener(any(), any()) }, times(1))
        cometChatMock.verify({ CometChat.addMessageListener(any(), any()) }, times(1))
    }

    test("every SDK callback maps to its hub event when on") {
        OfflineGate.testOverride = true
        val outbox = startAndCaptureOutbox()
        val sync = argumentCaptor<CometChat.SyncListener>()
        val reset = argumentCaptor<OfflineCacheResetListener>()
        val media = argumentCaptor<MediaDownloadListener>()
        val messages = argumentCaptor<CometChat.MessageListener>()
        cometChatMock.verify { CometChat.addSyncListener(any(), sync.capture()) }
        cometChatMock.verify { CometChat.addOfflineCacheResetListener(any(), reset.capture()) }
        cometChatMock.verify { CometChat.addMediaDownloadListener(any(), media.capture()) }
        cometChatMock.verify { CometChat.addMessageListener(any(), messages.capture()) }
        val conv = mock<Conversation>()
        val err = CometChatException("E", "boom")
        val synced = listOf(own("x"))

        val (hub, pub) = collectBoth {
            outbox.onOutboxMessagePending(own("l1"))
            outbox.onOutboxMessageSent(own("l1"))
            outbox.onOutboxMessageFailed(own("l2"), MessageFailureReason.PERMANENT)
            sync.firstValue.onConversationSyncCompleted(listOf(conv))
            sync.firstValue.onConversationSyncError(err)
            sync.firstValue.onMessageSyncCompleted(synced, SyncOutcome.PARTIAL)
            sync.firstValue.onMessageSyncError(err)
            reset.firstValue.onOfflineCacheReset(OfflineCacheResetReason.KEY_LOST, 3)
            media.firstValue.onMediaDownloaded("lm1", "att1", "/p/a.jpg")
            messages.firstValue.onMessagesSynced(synced)
        }

        hub.map { it::class.simpleName } shouldBe listOf(
            "SendState", "SendState", "SendState", "ConversationSyncCompleted", "ConversationSyncFailed",
            "MessageSyncCompleted", "MessageSyncFailed", "CacheReset", "MediaReady", "MessagesUpdated"
        )
        (hub[3] as OfflineHubEvent.ConversationSyncCompleted).conversations shouldBe listOf(conv)
        (hub[5] as OfflineHubEvent.MessageSyncCompleted).outcome shouldBe SyncOutcome.PARTIAL
        (hub[7] as OfflineHubEvent.CacheReset).pendingMessagesLost shouldBe 3
        hub[8] shouldBe OfflineHubEvent.MediaReady(0L, "lm1", "att1", "/p/a.jpg")
        (hub[9] as OfflineHubEvent.MessagesUpdated).messages shouldBe synced
        // Public: three send transitions + the cache reset.
        pub.size shouldBe 4
        pub[3] shouldBe CometChatOfflineEvent.OfflineCacheReset(OfflineCacheResetReason.KEY_LOST, 3)
    }

    test("every handler is inert while offline is off") {
        OfflineGate.testOverride = false
        val outbox = startAndCaptureOutbox()
        val sync = argumentCaptor<CometChat.SyncListener>()
        val reset = argumentCaptor<OfflineCacheResetListener>()
        val media = argumentCaptor<MediaDownloadListener>()
        val messages = argumentCaptor<CometChat.MessageListener>()
        cometChatMock.verify { CometChat.addSyncListener(any(), sync.capture()) }
        cometChatMock.verify { CometChat.addOfflineCacheResetListener(any(), reset.capture()) }
        cometChatMock.verify { CometChat.addMediaDownloadListener(any(), media.capture()) }
        cometChatMock.verify { CometChat.addMessageListener(any(), messages.capture()) }

        val (hub, pub) = collectBoth {
            outbox.onOutboxMessagePending(own("l1"))
            outbox.onOutboxMessageSent(own("l1"))
            outbox.onOutboxMessageFailed(own("l1"), MessageFailureReason.PERMANENT)
            sync.firstValue.onConversationSyncCompleted(emptyList())
            sync.firstValue.onMessageSyncCompleted(emptyList(), SyncOutcome.CAUGHT_UP)
            reset.firstValue.onOfflineCacheReset(OfflineCacheResetReason.CORRUPTED, 0)
            media.firstValue.onMediaDownloaded("lm", "a", "/p")
            messages.firstValue.onMessagesSynced(emptyList())
        }
        hub.shouldBeEmpty()
        pub.shouldBeEmpty()
    }

    test("exactly one public event per transition per localMessageId") {
        OfflineGate.testOverride = true
        val outbox = startAndCaptureOutbox()
        val (_, pub) = collectBoth {
            outbox.onOutboxMessagePending(own("l1"))
            outbox.onOutboxMessagePending(own("l1"))   // repeat: nothing
            outbox.onOutboxMessageSent(own("l1"))      // SENDING → SENT
            outbox.onOutboxMessageSent(own("l1"))      // repeat after terminal: nothing
        }
        pub.size shouldBe 2
        pub.map { (it as CometChatOfflineEvent.MessageSendStateChanged).state } shouldBe
            listOf(MessageSendState.SENDING, MessageSendState.SENT)
    }

    test("transitions are forward-only: a late SENDING after SENT emits nothing on either flow") {
        OfflineGate.testOverride = true
        val outbox = startAndCaptureOutbox()
        val (hub, pub) = collectBoth {
            outbox.onOutboxMessagePending(own("l1"))
            outbox.onOutboxMessageSent(own("l1"))
            outbox.onOutboxMessagePending(own("l1"))   // late
            outbox.onOutboxMessageFailed(own("l1"), MessageFailureReason.TRANSIENT) // late
        }
        hub.filterIsInstance<OfflineHubEvent.SendState>().map { it.state } shouldBe
            listOf(MessageSendState.SENDING, MessageSendState.SENT)
        pub.size shouldBe 2
    }

    test("SENDING → FAILED → SENDING (retry) → SENT are all forward; FAILED → FAILED is not") {
        OfflineGate.testOverride = true
        val outbox = startAndCaptureOutbox()
        val (_, pub) = collectBoth {
            outbox.onOutboxMessagePending(own("l1"))
            outbox.onOutboxMessageFailed(own("l1"), MessageFailureReason.TRANSIENT)
            outbox.onOutboxMessageFailed(own("l1"), MessageFailureReason.TRANSIENT)
            outbox.onOutboxMessagePending(own("l1"))
            outbox.onOutboxMessageSent(own("l1"))
        }
        pub.map { (it as CometChatOfflineEvent.MessageSendStateChanged).state } shouldBe listOf(
            MessageSendState.SENDING, MessageSendState.FAILED, MessageSendState.SENDING, MessageSendState.SENT
        )
    }

    test("a removal failure (DISCARDED) is terminal: a later callback emits nothing") {
        OfflineGate.testOverride = true
        val outbox = startAndCaptureOutbox()
        val (_, pub) = collectBoth {
            outbox.onOutboxMessagePending(own("l1"))
            outbox.onOutboxMessageFailed(own("l1"), MessageFailureReason.DISCARDED)
            outbox.onOutboxMessagePending(own("l1"))
        }
        pub.size shouldBe 2
    }

    test("tracked send state is pruned on SENT, on terminal failure and on SessionEnded") {
        OfflineGate.testOverride = true
        val outbox = startAndCaptureOutbox()
        outbox.onOutboxMessagePending(own("a"))
        outbox.onOutboxMessagePending(own("b"))
        outbox.onOutboxMessagePending(own("c"))
        OfflineHub.trackedSendCountForTest() shouldBe 3
        outbox.onOutboxMessageSent(own("a"))
        outbox.onOutboxMessageFailed(own("b"), MessageFailureReason.QUEUE_OVERFLOW)
        OfflineHub.trackedSendCountForTest() shouldBe 1
        OfflineHub.onLogout()
        OfflineHub.trackedSendCountForTest() shouldBe 0
    }

    test("reportSend honours the send's captured offlineOn, not the gate") {
        OfflineHub.start()
        OfflineGate.testOverride = true
        val (_, offPub) = collectBoth {
            OfflineHub.reportSend(own("l1"), MessageSendState.SENDING, null, offlineOn = false)
        }
        offPub.shouldBeEmpty()

        OfflineGate.testOverride = false
        val (hub, onPub) = collectBoth {
            OfflineHub.reportSend(own("l2"), MessageSendState.SENDING, null, offlineOn = true)
        }
        onPub.size shouldBe 1
        hub.single().shouldBeInstanceOf<OfflineHubEvent.SendState>()
    }

    test("a 300-event burst arrives complete and in order on both flows, with slow real collectors") {
        OfflineGate.testOverride = true
        val outbox = startAndCaptureOutbox()
        val expected = (0 until 300).map { "b$it" }
        runBlocking {
            val hubGot = Collections.synchronizedList(mutableListOf<String>())
            val pubGot = Collections.synchronizedList(mutableListOf<String>())
            val hubReady = CompletableDeferred<Unit>()
            val pubReady = CompletableDeferred<Unit>()
            // Collectors on a real background dispatcher that sleep per item, so the emitter runs far
            // ahead of them; a bounded or DROP_OLDEST buffer would lose events here.
            val j1 = launch(Dispatchers.Default) {
                OfflineHub.events.onSubscription { hubReady.complete(Unit) }.collect {
                    if (it is OfflineHubEvent.SendState) { Thread.sleep(1); hubGot += it.localMessageId }
                }
            }
            val j2 = launch(Dispatchers.Default) {
                CometChatEvents.offlineEvents.onSubscription { pubReady.complete(Unit) }.collect {
                    if (it is CometChatOfflineEvent.MessageSendStateChanged) {
                        Thread.sleep(1); pubGot += it.message.localMessageId
                    }
                }
            }
            hubReady.await(); pubReady.await()
            repeat(300) { outbox.onOutboxMessagePending(own("b$it")) }
            withTimeout(20_000) { while (hubGot.size < 300 || pubGot.size < 300) delay(10) }
            j1.cancel(); j2.cancel()
            hubGot.toList() shouldBe expected
            pubGot.toList() shouldBe expected
        }
    }

    test("logout keeps registrations, bumps gen, and emits SessionEnded") {
        OfflineGate.testOverride = true
        OfflineHub.start()
        val before = OfflineHub.gen
        val (hub, _) = collectBoth { OfflineHub.onLogout() }
        hub.filterIsInstance<OfflineHubEvent.SessionEnded>().single().gen shouldBe before + 1
        OfflineHub.gen shouldBe before + 1
        cometChatMock.verify({ CometChat.removeMessageListener(any()) }, never())
        cometChatMock.verify({ CometChat.removeOfflineMessageListener(any()) }, never())
        cometChatMock.verify({ CometChat.removeSyncListener(any()) }, never())
        cometChatMock.verify({ CometChat.removeOfflineCacheResetListener(any()) }, never())
        cometChatMock.verify({ CometChat.removeMediaDownloadListener(any()) }, never())
    }

    test("same-uid re-login does not clear; a different uid does") {
        OfflineHub.start()
        OfflineHub.noteSession("userA")
        val g1 = OfflineHub.gen
        OfflineHub.noteSession("userA") // token refresh
        OfflineHub.gen shouldBe g1
        OfflineHub.noteSession("userB") // switch
        OfflineHub.gen shouldBe g1 + 1
    }
})

