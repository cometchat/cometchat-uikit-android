package com.cometchat.uikit.core.offline

import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.MessageFailureReason
import com.cometchat.chat.models.MessageStatus
import com.cometchat.chat.models.TextMessage
import com.cometchat.uikit.core.events.CometChatMessageEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.cometchat.uikit.core.events.MessageStatus as KitSendStatus

/**
 * The message list's send rows on the ON path ([MessageListOfflineSendRows]): one test per row of
 * the transition list (README §3.4 as corrected by G4), the batch, the thread reply count and the
 * legacy `MessageSent` events.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SendRowsTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    val h = SendRowsHarness()

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }
    afterTest { h.close() }

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    test("pending inserts the row once") {
        runTest {
            val vm = h.open()
            val msg = h.ownText("a")
            h.sendPending(msg)
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 1
            vm.getItems().single() shouldBeSameInstanceAs msg
        }
    }

    test("pending replaces an existing row matched by muid (the optimistic row)") {
        runTest {
            val vm = h.open()
            val optimistic = h.ownText("a")
            vm.addItem(optimistic)
            val pending = h.ownText("a").apply { muid = optimistic.muid }
            h.sendPending(pending)
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 1
            vm.getItems().single() shouldBeSameInstanceAs pending
        }
    }

    test("onError after pending with status PENDING leaves the row queued") {
        runTest {
            val vm = h.open()
            val msg = h.ownText("a")
            val listener = h.sendPending(msg)
            listener.onError(CometChatException("ERROR_INTERNET_UNAVAILABLE", "offline"))
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 1
            OfflineRowState.caption(vm.getItems().single(), true) shouldBe OfflineRowState.Caption.NOT_SENT_YET
        }
    }

    test("onError without pending: failed (direct) row in memory") {
        runTest {
            val vm = h.open()
            val msg = h.ownText("a")
            OfflineSend.interceptTextMessage(msg, null)
            h.sdk.sends.single().second.onError(CometChatException("ERR_X", "boom"))
            advanceUntilIdle()
            val row = vm.getItems().single()
            OfflineRowState.caption(row, true) shouldBe OfflineRowState.Caption.NOT_SENT_TAP_TO_RETRY
            OfflineSend.isDirectFailure(row) shouldBe true
        }
    }

    test("QUEUE_FULL: failed (direct) row plus the QueueFull notice; BYTE_OVERFLOW: FileTooLargeOffline") {
        runTest {
            val vm = h.open()
            val notices = mutableListOf<OfflineNotice>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.offlineNotices.toList(notices) }
            listOf(MessageFailureReason.QUEUE_FULL, MessageFailureReason.BYTE_OVERFLOW).forEach { reason ->
                val msg = h.ownText(reason.name)
                OfflineSend.interceptTextMessage(msg, null)
                msg.status = MessageStatus.FAILED
                msg.failureReason = reason
                h.sdk.sends.last().second.onError(CometChatException("ERROR_PENDING_QUEUE_LIMIT", "full"))
            }
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 2
            vm.getItems().all { OfflineRowState.isFailed(it) } shouldBe true
            notices shouldBe listOf(OfflineNotice.QueueFull, OfflineNotice.FileTooLargeOffline)
        }
    }

    test("ack replaces the row in place, keeps its render key and records the alias") {
        runTest {
            val vm = h.open()
            vm.addItem(h.served(1L, from = h.peer))
            val msg = h.ownText("a")
            val listener = h.sendPending(msg)
            vm.addItem(h.served(2L, from = h.peer))
            val keyBefore = MessageKey.renderKey(msg, "me")
            listener.onMessageSent(ackOf(msg, 50L))
            advanceUntilIdle()
            vm.getItems().map { it.id } shouldBe listOf(1L, 50L, 2L)
            MessageKey.renderKey(vm.getItems()[1], "me") shouldBe keyBefore
        }
    }

    test("TRANSIENT / PERMANENT: failed (outbox) derived from status") {
        runTest {
            val vm = h.open()
            val msg = h.ownText("a")
            val listener = h.sendPending(msg)
            msg.status = MessageStatus.FAILED
            msg.failureReason = MessageFailureReason.TRANSIENT
            listener.onError(CometChatException("ERR", "5xx"))
            advanceUntilIdle()
            val row = vm.getItems().single()
            OfflineRowState.caption(row, true) shouldBe OfflineRowState.Caption.NOT_SENT_TAP_TO_RETRY
            OfflineSend.isDirectFailure(row) shouldBe false
            (row.metadata?.has("error") ?: false) shouldBe false
        }
    }

    test("failed (outbox) is derived on the first render after relaunch, from status alone") {
        val relaunched = h.ownText("x").apply {
            localMessageId = "l-relaunch"
            status = MessageStatus.FAILED
            failureReason = MessageFailureReason.PERMANENT
        }
        h.open()
        OfflineRowState.caption(relaunched, true) shouldBe OfflineRowState.Caption.NOT_SENT_TAP_TO_RETRY
        OfflineRowState.caption(relaunched, false) shouldBe OfflineRowState.Caption.NONE
    }

    test("DISCARDED removes the row with an UnsentRemoved(text) notice") {
        runTest {
            val vm = h.open()
            val notices = mutableListOf<OfflineNotice>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.offlineNotices.toList(notices) }
            val msg = h.ownText("bye")
            val listener = h.sendPending(msg)
            msg.status = MessageStatus.FAILED
            msg.failureReason = MessageFailureReason.DISCARDED
            listener.onError(CometChatException(OfflineSend.ERROR_DISCARDED, "removed"))
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 0
            notices shouldBe listOf(OfflineNotice.UnsentRemoved("bye"))
        }
    }

    test("QUEUE_OVERFLOW removes the victim with a notice") {
        runTest {
            val vm = h.open()
            val notices = mutableListOf<OfflineNotice>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.offlineNotices.toList(notices) }
            val victim = h.ownText("old")
            h.sendPending(victim)
            val newer = h.ownText("new")
            h.sendPending(newer)
            victim.status = MessageStatus.FAILED
            OfflineHub.reportSend(victim, MessageSendState.FAILED, MessageFailureReason.QUEUE_OVERFLOW, true)
            advanceUntilIdle()
            vm.getItems().single() shouldBeSameInstanceAs newer
            notices shouldBe listOf(OfflineNotice.UnsentRemoved("old"))
        }
    }

    test("a batch of 5 gives 5 rows, keyed by localMessageId even with one shared muid") {
        runTest {
            val vm = h.open()
            repeat(5) { i -> h.sendPending(h.ownText("f$i").apply { muid = "same-muid" }) }
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 5
            vm.getItems().map { it.localMessageId }.toSet() shouldHaveSize 5
        }
    }

    test("thread reply in the main list: +1 once at pending, not again on the ack; a purge (DISCARDED) leaves it, as the SDK does (D-321)") {
        runTest {
            val parent = h.served(10L, from = h.peer).apply { replyCount = 2 }
            val vm = h.open(seeded = listOf(parent))
            advanceUntilIdle()
            val reply = h.ownText("r", parentId = 10L)
            val listener = h.sendPending(reply)
            advanceUntilIdle()
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 3
            vm.getItems() shouldHaveSize 1 // the reply is not a main-list row

            // Legacy SUCCESS for the same reply must not count it again.
            val acked = ackOf(reply, 11L)
            listener.onMessageSent(acked)
            MessageListOfflineSendRows.onLegacySentEvent(
                vm, CometChatMessageEvent.MessageSent(acked, KitSendStatus.SUCCESS)
            ) shouldBe true
            advanceUntilIdle()
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 3

            // A second reply, then purged (DISCARDED): the SDK leaves the stored count as it is, so
            // the row does too and agrees with a reload (the reconnect sweep heals both).
            val reply2 = h.ownText("r2", parentId = 10L)
            val l2 = h.sendPending(reply2)
            advanceUntilIdle()
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 4
            reply2.status = MessageStatus.FAILED
            reply2.failureReason = MessageFailureReason.DISCARDED
            l2.onError(CometChatException(OfflineSend.ERROR_DISCARDED, ""))
            advanceUntilIdle()
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 4
        }
    }

    /** An own reply already in the outbox when the list loaded: the SDK counted it in the stored count. */
    fun queuedBeforeLoad(localId: String): TextMessage =
        h.ownText("pre-$localId", parentId = 10L).apply { localMessageId = localId }

    test("D-321 reload then flush: a reply queued before the row loaded is not added again when it sends") {
        runTest {
            val parent = h.served(10L, from = h.peer).apply { replyCount = 3 } // stored 2 + the queued reply
            val vm = h.open(seeded = listOf(parent))
            advanceUntilIdle()
            val reply = queuedBeforeLoad("L-pre")
            val acked = ackOf(reply, 11L).apply { localMessageId = "L-pre" }
            OfflineHub.reportSend(acked, MessageSendState.SENT, null, true)
            MessageListOfflineSendRows.onLegacySentEvent(
                vm, CometChatMessageEvent.MessageSent(acked, KitSendStatus.SUCCESS)
            ) shouldBe true
            advanceUntilIdle()
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 3
        }
    }

    test("D-321 eviction: a queued reply evicted (QUEUE_OVERFLOW) leaves the count, as the SDK leaves the stored count") {
        runTest {
            val parent = h.served(10L, from = h.peer).apply { replyCount = 2 }
            val vm = h.open(seeded = listOf(parent))
            advanceUntilIdle()
            val reply = h.ownText("r", parentId = 10L)
            h.sendPending(reply)
            advanceUntilIdle()
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 3
            reply.status = MessageStatus.FAILED
            OfflineHub.reportSend(reply, MessageSendState.FAILED, MessageFailureReason.QUEUE_OVERFLOW, true)
            advanceUntilIdle()
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 3
        }
    }

    test("D-321 discard: deleting a reply queued before the row loaded takes it back once") {
        runTest {
            val parent = h.served(10L, from = h.peer).apply { replyCount = 3 } // stored 2 + the queued reply
            val vm = h.open(seeded = listOf(parent))
            advanceUntilIdle()
            val reply = queuedBeforeLoad("L-pre")
            OfflineSend.delete(reply) { }
            h.sdk.discards.single().second.onSuccess("ok")
            advanceUntilIdle()
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 2
        }
    }

    test("D-321 a retry of a reply that failed before the row loaded adds nothing") {
        runTest {
            val parent = h.served(10L, from = h.peer).apply { replyCount = 3 }
            val vm = h.open(seeded = listOf(parent))
            advanceUntilIdle()
            val failedCopy = queuedBeforeLoad("L-pre").apply { status = MessageStatus.FAILED }
            OfflineHub.reportSend(failedCopy, MessageSendState.SENDING, null, true)
            advanceUntilIdle()
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 3
        }
    }

    test("Delete of a queued thread reply decrements the main-list parent once; a later DISCARDED does not (R5)") {
        runTest {
            val parent = h.served(10L, from = h.peer).apply { replyCount = 2 }
            val vm = h.open(seeded = listOf(parent))
            advanceUntilIdle()
            val events = mutableListOf<Any>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                com.cometchat.uikit.core.events.CometChatEvents.offlineEvents.toList(events)
            }
            val reply = h.ownText("r", parentId = 10L)
            h.sendPending(reply)
            advanceUntilIdle()
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 3
            val eventsBeforeDelete = events.size

            // The thread list's Delete: discardLocalMessage succeeds and the SDK fires no event.
            var result: OfflineSend.ActionResult? = null
            OfflineSend.delete(reply) { result = it }
            h.sdk.discards.single().second.onSuccess("ok")
            advanceUntilIdle()
            result shouldBe OfflineSend.ActionResult.Removed
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 2
            events.size shouldBe eventsBeforeDelete // no public event for the Delete

            // A later SDK DISCARDED for the same reply must not take it down again.
            reply.status = MessageStatus.FAILED
            reply.failureReason = MessageFailureReason.DISCARDED
            OfflineHub.reportSend(reply, MessageSendState.FAILED, MessageFailureReason.DISCARDED, true)
            advanceUntilIdle()
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 2
        }
    }

    test("a rejected Delete of a thread reply (IN_FLIGHT, NOT_FOUND) leaves the parent count (R5)") {
        runTest {
            val parent = h.served(10L, from = h.peer).apply { replyCount = 2 }
            val vm = h.open(seeded = listOf(parent))
            advanceUntilIdle()
            val reply = h.ownText("r", parentId = 10L)
            h.sendPending(reply)
            advanceUntilIdle()
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 3

            val results = mutableListOf<OfflineSend.ActionResult>()
            OfflineSend.delete(reply) { results += it }
            h.sdk.discards.last().second.onError(CometChatException(OfflineSend.ERROR_IN_FLIGHT, "sending"))
            OfflineSend.delete(reply) { results += it }
            h.sdk.discards.last().second.onError(CometChatException(OfflineSend.ERROR_NOT_FOUND, "gone"))
            advanceUntilIdle()
            results shouldBe listOf(OfflineSend.ActionResult.InFlight, OfflineSend.ActionResult.Removed)
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 3
        }
    }

    test("Delete of a main-list row publishes no reply-count change") {
        runTest {
            val parent = h.served(10L, from = h.peer).apply { replyCount = 2 }
            val vm = h.open(seeded = listOf(parent))
            advanceUntilIdle()
            val msg = h.ownText("plain")
            h.sendPending(msg)
            advanceUntilIdle()
            MessageListOfflineSendRows.delete(vm, msg) shouldBe true
            h.sdk.discards.single().second.onSuccess("ok")
            advanceUntilIdle()
            vm.getItems().none { it === msg } shouldBe true
            vm.getItems().single { it.id == 10L }.replyCount shouldBe 2
        }
    }

    test("legacy IN_PROGRESS and ERROR are handled by the hub path; SUCCESS runs today's ack") {
        runTest {
            val vm = h.open()
            val msg = h.ownText("a")
            MessageListOfflineSendRows.onLegacySentEvent(
                vm, CometChatMessageEvent.MessageSent(msg, KitSendStatus.IN_PROGRESS)
            ) shouldBe true
            MessageListOfflineSendRows.onLegacySentEvent(
                vm, CometChatMessageEvent.MessageSent(msg, KitSendStatus.ERROR)
            ) shouldBe true
            MessageListOfflineSendRows.onLegacySentEvent(
                vm, CometChatMessageEvent.MessageSent(ackOf(msg, 3L), KitSendStatus.SUCCESS)
            ) shouldBe false
            vm.getItems() shouldHaveSize 0
        }
    }

    test("an ack never replaces a row a moderation verdict already DISAPPROVED (W0)") {
        runTest {
            val vm = h.open()
            val msg = h.ownText("a")
            val listener = h.sendPending(msg)
            advanceUntilIdle()
            msg.moderationStatus = com.cometchat.chat.enums.ModerationStatus.DISAPPROVED
            listener.onMessageSent(ackOf(msg, 60L))
            advanceUntilIdle()
            vm.getItems().single() shouldBeSameInstanceAs msg
        }
    }

    test("a session end clears the per-list send state") {
        runTest {
            val vm = h.open()
            vm.offlineSendRowsState.heldReceipts[99L] = MessageListOfflineSendRows.HeldReceipt(deliveredAt = 1L)
            vm.offlineSendRowsState.actionsInFlight += "l:x"
            OfflineHub.onLogout()
            advanceUntilIdle()
            vm.offlineSendRowsState.heldReceipts.size shouldBe 0
            vm.offlineSendRowsState.actionsInFlight.size shouldBe 0
        }
    }
})
