package com.cometchat.uikit.core.offline

import android.util.Log
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.MessageFailureReason
import com.cometchat.chat.models.MessageStatus
import com.cometchat.chat.models.TextMessage
import com.cometchat.uikit.core.CometChatUIKit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any

/**
 * [OfflineSend], one test per transition of README §3.4 as corrected by G4, plus the caller
 * contract (not resolved at pending; resolves on sent; rejects only on a final or direct failure),
 * the quiet logout rejection (G12) and the one-event-per-transition dedupe.
 */
class OfflineSendTest : FunSpec({

    lateinit var logMock: MockedStatic<Log>
    lateinit var sdk: FakeOfflineSdk
    lateinit var events: MutableList<OfflineHubEvent>
    lateinit var collector: Job

    fun sendStates() = events.filterIsInstance<OfflineHubEvent.SendState>()

    beforeTest {
        logMock = Mockito.mockStatic(Log::class.java)
        OfflineHub.resetForTest()
        OfflineSend.resetForTest()
        sdk = FakeOfflineSdk()
        OfflineSend.sdk = sdk
        events = mutableListOf()
        collector = CoroutineScope(Dispatchers.Unconfined).launch { OfflineHub.events.collect { events += it } }
    }

    afterTest {
        collector.cancel()
        OfflineSend.resetForTest()
        OfflineGate.testOverride = null
        logMock.close()
    }

    test("pending: one SENDING transition and the caller is not resolved") {
        val cb = RecordingCallback<TextMessage>()
        val msg = textTo()
        OfflineSend.interceptTextMessage(msg, cb) shouldBe true
        msg.status = MessageStatus.PENDING
        sdk.sends.single().second.onMessagePending(msg)

        sendStates().map { it.state } shouldBe listOf(MessageSendState.SENDING)
        cb.successes.shouldBeEmpty()
        cb.errors.shouldBeEmpty()
        msg.sender?.uid shouldBe "me"
        (msg.muid.isNullOrEmpty()) shouldBe false
    }

    test("onError after pending with status PENDING is queued: no transition, no rejection") {
        val cb = RecordingCallback<TextMessage>()
        val msg = textTo()
        OfflineSend.interceptTextMessage(msg, cb)
        val listener = sdk.sends.single().second
        msg.status = MessageStatus.PENDING
        listener.onMessagePending(msg)
        listener.onError(CometChatException("ERROR_INTERNET_UNAVAILABLE", "offline"))

        sendStates().map { it.state } shouldBe listOf(MessageSendState.SENDING)
        cb.errors.shouldBeEmpty()
        OfflineSend.isDirectFailure(msg) shouldBe false
    }

    test("ack: SENT transition and the caller resolves with the acked message") {
        val cb = RecordingCallback<TextMessage>()
        val msg = textTo()
        OfflineSend.interceptTextMessage(msg, cb)
        val listener = sdk.sends.single().second
        msg.status = MessageStatus.PENDING
        listener.onMessagePending(msg)
        val acked = ackOf(msg, 42L)
        listener.onMessageSent(acked)

        sendStates().map { it.state } shouldBe listOf(MessageSendState.SENDING, MessageSendState.SENT)
        cb.successes.single().id shouldBe 42L
        cb.errors.shouldBeEmpty()
    }

    test("onError without pending: failed (direct), error metadata, caller rejected") {
        val cb = RecordingCallback<TextMessage>()
        val msg = textTo()
        OfflineSend.interceptTextMessage(msg, cb)
        sdk.sends.single().second.onError(CometChatException("ERR_X", "boom"))

        val state = sendStates().single()
        state.state shouldBe MessageSendState.FAILED
        OfflineSend.isDirectFailure(msg) shouldBe true
        msg.metadata?.optString("error") shouldBe "boom"
        cb.errors.single()?.code shouldBe "ERR_X"
    }

    test("QUEUE_FULL / BYTE_OVERFLOW: failed (direct) carrying the reason") {
        listOf(MessageFailureReason.QUEUE_FULL, MessageFailureReason.BYTE_OVERFLOW).forEach { reason ->
            events.clear()
            val cb = RecordingCallback<TextMessage>()
            val msg = textTo()
            OfflineSend.interceptTextMessage(msg, cb)
            msg.status = MessageStatus.FAILED
            msg.failureReason = reason
            sdk.sends.last().second.onError(CometChatException("ERROR_PENDING_QUEUE_LIMIT", "full"))

            val state = sendStates().single()
            state.state shouldBe MessageSendState.FAILED
            state.reason shouldBe reason
            OfflineSend.isDirectFailure(msg) shouldBe true
            cb.errors shouldHaveSize 1
        }
    }

    test("TRANSIENT / PERMANENT after pending: failed (outbox), derived from status, no error metadata") {
        listOf(MessageFailureReason.TRANSIENT, MessageFailureReason.PERMANENT).forEach { reason ->
            events.clear()
            val cb = RecordingCallback<TextMessage>()
            val msg = textTo()
            OfflineSend.interceptTextMessage(msg, cb)
            val listener = sdk.sends.last().second
            msg.status = MessageStatus.PENDING
            listener.onMessagePending(msg)
            msg.status = MessageStatus.FAILED
            msg.failureReason = reason
            listener.onError(CometChatException("ERR_SERVER", "5xx"))

            sendStates().map { it.state } shouldBe listOf(MessageSendState.SENDING, MessageSendState.FAILED)
            sendStates().last().reason shouldBe reason
            OfflineSend.isDirectFailure(msg) shouldBe false
            (msg.metadata?.has("error") ?: false) shouldBe false
            cb.errors shouldHaveSize 1
        }
    }

    test("DISCARDED after pending: FAILED/DISCARDED transition; the rejection is quiet") {
        val cb = RecordingCallback<TextMessage>()
        val msg = textTo()
        OfflineSend.interceptTextMessage(msg, cb)
        val listener = sdk.sends.single().second
        msg.status = MessageStatus.PENDING
        listener.onMessagePending(msg)
        msg.status = MessageStatus.FAILED
        msg.failureReason = MessageFailureReason.DISCARDED
        listener.onError(CometChatException(OfflineSend.ERROR_DISCARDED, "removed"))

        sendStates().last().reason shouldBe MessageFailureReason.DISCARDED
        OfflineSend.isQuietRejection(cb.errors.single()) shouldBe true
    }

    test("QUEUE_OVERFLOW victim: FAILED/QUEUE_OVERFLOW transition") {
        val msg = textTo()
        OfflineSend.interceptTextMessage(msg, null)
        val listener = sdk.sends.single().second
        msg.status = MessageStatus.PENDING
        listener.onMessagePending(msg)
        msg.status = MessageStatus.FAILED
        msg.failureReason = MessageFailureReason.QUEUE_OVERFLOW
        listener.onError(CometChatException("ERROR_PENDING_QUEUE_LIMIT", "evicted"))

        sendStates().last().reason shouldBe MessageFailureReason.QUEUE_OVERFLOW
    }

    test("logout rejects an open caller once, quietly; a late callback is ignored") {
        val cb = RecordingCallback<TextMessage>()
        val msg = textTo()
        OfflineSend.interceptTextMessage(msg, cb)
        val listener = sdk.sends.single().second
        msg.status = MessageStatus.PENDING
        listener.onMessagePending(msg)

        OfflineHub.onLogout()
        cb.errors.single()?.code shouldBe OfflineSend.ERROR_DISCARDED
        OfflineSend.isQuietRejection(cb.errors.single()) shouldBe true

        val before = sendStates().size
        listener.onMessageSent(ackOf(msg, 7L))
        cb.successes.shouldBeEmpty()
        cb.errors shouldHaveSize 1
        sendStates().size shouldBe before
        OfflineSend.openCallCountForTest() shouldBe 0
    }

    test("dedupe: per-call pending and the outbox pending give one transition") {
        val msg = textTo()
        OfflineSend.interceptTextMessage(msg, null)
        msg.status = MessageStatus.PENDING
        sdk.sends.single().second.onMessagePending(msg)
        // The outbox listener reports the same transition through the hub.
        OfflineHub.reportSend(msg, MessageSendState.SENDING, null, true)
        sendStates() shouldHaveSize 1

        sdk.sends.single().second.onMessageSent(ackOf(msg, 9L))
        OfflineHub.reportSend(ackOf(msg, 9L), MessageSendState.SENT, null, true)
        sendStates().map { it.state } shouldBe listOf(MessageSendState.SENDING, MessageSendState.SENT)
    }

    test("a batch of 5 sends gives 5 SENDING transitions, one per localMessageId") {
        repeat(5) { OfflineSend.interceptTextMessage(textTo(text = "f$it"), null) }
        sdk.sends.forEach { (m, l) -> m.status = MessageStatus.PENDING; l.onMessagePending(m) }
        sendStates().map { it.localMessageId }.toSet() shouldHaveSize 5
    }

    test("statics: CometChatUIKit.sendX with offline on goes through the helper only") {
        val cometChat: MockedStatic<CometChat> = Mockito.mockStatic(CometChat::class.java)
        try {
            OfflineGate.testOverride = true
            CometChatUIKit.sendTextMessage(textTo(), null)
            CometChatUIKit.sendMediaMessage(
                com.cometchat.chat.models.MediaMessage("peer", "image", "user"), null
            )
            CometChatUIKit.sendCustomMessage(
                com.cometchat.chat.models.CustomMessage("peer", "user", "poll", org.json.JSONObject()), null
            )
            sdk.sends shouldHaveSize 3
            cometChat.verify({
                CometChat.sendMessage(any<TextMessage>(), any<CometChat.CallbackListener<TextMessage>>())
            }, Mockito.never())
        } finally {
            cometChat.close()
        }
    }

    fun cacheReset() = OfflineHub.publishForTest(
        OfflineHubEvent.CacheReset(OfflineHub.gen, com.cometchat.chat.models.OfflineCacheResetReason.CORRUPTED, 1)
    )

    test("ENG-41551 H: a cache wipe fails an open send once, quietly; the SDK's late DISCARDED never settles it again") {
        val cb = RecordingCallback<TextMessage>()
        val msg = textTo()
        OfflineSend.interceptTextMessage(msg, cb)
        val listener = sdk.sends.single().second
        msg.status = MessageStatus.PENDING
        listener.onMessagePending(msg)

        cacheReset()
        cb.errors.single()?.code shouldBe OfflineSend.ERROR_DISCARDED
        OfflineSend.isQuietRejection(cb.errors.single()) shouldBe true
        OfflineSend.openCallCountForTest() shouldBe 0

        // A readable reset's per-call DISCARDED arriving late: the row still gets its transition,
        // the caller is not settled twice.
        msg.status = MessageStatus.FAILED
        msg.failureReason = MessageFailureReason.DISCARDED
        listener.onError(CometChatException(OfflineSend.ERROR_DISCARDED, "removed"))
        cb.errors shouldHaveSize 1
        sendStates().last().reason shouldBe MessageFailureReason.DISCARDED
    }

    test("ENG-41551 H: a readable reset settles through the SDK's DISCARDED first; the reset adds nothing") {
        val cb = RecordingCallback<TextMessage>()
        val msg = textTo()
        OfflineSend.interceptTextMessage(msg, cb)
        val listener = sdk.sends.single().second
        msg.status = MessageStatus.PENDING
        listener.onMessagePending(msg)
        msg.status = MessageStatus.FAILED
        msg.failureReason = MessageFailureReason.DISCARDED
        listener.onError(CometChatException(OfflineSend.ERROR_DISCARDED, "removed"))

        cacheReset()
        cb.errors shouldHaveSize 1
        cb.successes.shouldBeEmpty()
    }

    test("ENG-41551 H: a reset never fails a direct send already on the wire (no pending): it answers itself once") {
        val cb = RecordingCallback<TextMessage>()
        val msg = textTo()
        OfflineSend.interceptTextMessage(msg, cb)
        val listener = sdk.sends.single().second

        cacheReset()
        cb.errors.shouldBeEmpty()
        OfflineSend.openCallCountForTest() shouldBe 1

        listener.onMessageSent(ackOf(msg, 11L))
        cb.successes shouldHaveSize 1
        cb.errors.shouldBeEmpty()
    }

    fun interactive(): com.cometchat.chat.models.InteractiveMessage =
        com.cometchat.chat.models.InteractiveMessage(
            "peer", "user", "form", org.json.JSONObject().put("title", "t")
        ).apply {
            muid = "muid-i"
            metadata = org.json.JSONObject().put("error", "offline_disabled")
        }

    test("ENG-41551 I: Retry of a direct-failed interactive message with offline off resends it") {
        OfflineGate.testOverride = false
        val failed = interactive()
        OfflineSend.markDirectFailure(failed)
        var result: OfflineSend.ActionResult? = null

        OfflineSend.retry(failed) { result = it }

        result shouldBe OfflineSend.ActionResult.Accepted
        val sent = sdk.directInteractive.single().first
        sent.muid shouldBe "muid-i"
        sent.metadata?.has("error") shouldBe false
        sdk.retries.shouldBeEmpty()
        OfflineSend.isDirectFailure(failed) shouldBe false
    }

    test("ENG-41551 I: with offline on the interactive resend goes through the per-send listener") {
        OfflineGate.testOverride = true
        val failed = interactive()
        OfflineSend.markDirectFailure(failed)
        var result: OfflineSend.ActionResult? = null

        OfflineSend.retry(failed) { result = it }

        result shouldBe OfflineSend.ActionResult.Accepted
        (sdk.sends.single().first is com.cometchat.chat.models.InteractiveMessage) shouldBe true
        sdk.directInteractive.shouldBeEmpty()
    }

    test("ENG-41551 I: Retry never reports Accepted for a type it cannot send") {
        OfflineGate.testOverride = false
        val failed = com.cometchat.chat.models.Action().apply { muid = "muid-a" }
        OfflineSend.markDirectFailure(failed)
        var result: OfflineSend.ActionResult? = null

        OfflineSend.retry(failed) { result = it }

        result shouldBe OfflineSend.ActionResult.Failed(null)
        sdk.sends.shouldBeEmpty()
        sdk.directInteractive.shouldBeEmpty()
        OfflineSend.isDirectFailure(failed) shouldBe true
    }

    test("isQuietRejection: only the discarded and not-logged-in codes") {
        OfflineSend.isQuietRejection(CometChatException(OfflineSend.ERROR_DISCARDED, "")) shouldBe true
        OfflineSend.isQuietRejection(CometChatException("ERROR_USER_NOT_LOGGED_IN", "")) shouldBe true
        OfflineSend.isQuietRejection(CometChatException("ERROR_INTERNET_UNAVAILABLE", "")) shouldBe false
        OfflineSend.isQuietRejection(RuntimeException()) shouldBe false
    }
})
