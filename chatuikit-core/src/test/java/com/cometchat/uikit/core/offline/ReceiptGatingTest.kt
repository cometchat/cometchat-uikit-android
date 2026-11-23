package com.cometchat.uikit.core.offline

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.SyncOutcome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString

/**
 * Receipts on the ON path (G5, BEH-SEND-32): an unsent row never takes a receipt; "X and everything
 * before" applies only to rows with a server id; a receipt for a row not yet acked is held (cap
 * 200) and applied on its ack, even after a sweep; an ack never wipes ticks a swept copy shows;
 * the held map clears on a session end.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptGatingTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    val h = SendRowsHarness()

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }
    afterTest { h.close() }

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    test("a receipt stamps server-id rows up to X and never an unsent row") {
        runTest {
            val vm = h.open(seeded = listOf(h.served(1L), h.served(2L), h.served(3L)))
            advanceUntilIdle()
            val pending = h.ownText("p")
            h.sendPending(pending)
            advanceUntilIdle()

            MessageListOfflineSendRows.applyReceipt(vm, h.receipt(2L, delivered = true), delivered = true)
            val rows = vm.getItems()
            rows.first { it.id == 1L }.deliveredAt shouldBe 5000L
            rows.first { it.id == 2L }.deliveredAt shouldBe 5000L
            rows.first { it.id == 3L }.deliveredAt shouldBe 0L
            rows.first { it.id == 0L }.deliveredAt shouldBe 0L
        }
    }

    test("the VM's receipt listener routes through the gate (delivered and read)") {
        runTest {
            val vm = h.open(seeded = listOf(h.served(1L)))
            advanceUntilIdle()
            val pending = h.ownText("p")
            h.sendPending(pending)
            advanceUntilIdle()
            val captor = ArgumentCaptor.forClass(CometChat.MessageListener::class.java)
            h.cometChat.verify { CometChat.addMessageListener(anyString(), captor.capture()) }
            val listener = captor.allValues.last()
            listener.onMessagesDelivered(h.receipt(1L, delivered = true))
            listener.onMessagesRead(h.receipt(1L, delivered = false, at = 6000L))
            val rows = vm.getItems()
            rows.first { it.id == 1L }.readAt shouldBe 6000L
            rows.first { it.id == 0L }.deliveredAt shouldBe 0L
            rows.first { it.id == 0L }.readAt shouldBe 0L
        }
    }

    test("a receipt for a row not yet acked is held and applied on its ack") {
        runTest {
            val vm = h.open(seeded = listOf(h.served(1L)))
            advanceUntilIdle()
            val pending = h.ownText("p")
            val listener = h.sendPending(pending)
            advanceUntilIdle()

            MessageListOfflineSendRows.applyReceipt(vm, h.receipt(7L, delivered = true), delivered = true)
            vm.offlineSendRowsState.heldReceipts.containsKey(7L) shouldBe true

            listener.onMessageSent(ackOf(pending, 7L))
            advanceUntilIdle()
            vm.getItems().first { it.id == 7L }.deliveredAt shouldBe 5000L
            vm.offlineSendRowsState.heldReceipts.containsKey(7L) shouldBe false
        }
    }

    test("the held map is capped at 200, oldest evicted first") {
        runTest {
            val vm = h.open()
            (1L..250L).forEach { x ->
                MessageListOfflineSendRows.applyReceipt(vm, h.receipt(1000L + x, delivered = true), delivered = true)
            }
            vm.offlineSendRowsState.heldReceipts.size shouldBe MessageListOfflineSendRows.HELD_RECEIPT_CAP
            vm.offlineSendRowsState.heldReceipts.containsKey(1001L) shouldBe false
            vm.offlineSendRowsState.heldReceipts.containsKey(1250L) shouldBe true
        }
    }

    test("a held receipt survives an unrelated sweep and is applied on its ack (R4)") {
        runTest {
            val vm = h.open(seeded = listOf(h.served(1L)))
            advanceUntilIdle()
            val pending = h.ownText("p")
            val listener = h.sendPending(pending)
            advanceUntilIdle()

            MessageListOfflineSendRows.applyReceipt(vm, h.receipt(7L, delivered = true), delivered = true)
            OfflineHub.publishForTest(
                OfflineHubEvent.MessageSyncCompleted(OfflineHub.gen, emptyList(), SyncOutcome.CAUGHT_UP)
            )
            advanceUntilIdle()
            vm.offlineSendRowsState.heldReceipts.containsKey(7L) shouldBe true

            listener.onMessageSent(ackOf(pending, 7L))
            advanceUntilIdle()
            vm.getItems().first { it.id == 7L }.deliveredAt shouldBe 5000L
        }
    }

    test("an ack after a swept copy keeps the ticks that copy shows (R4)") {
        runTest {
            val vm = h.open(seeded = listOf(h.served(1L)))
            advanceUntilIdle()
            val pending = h.ownText("p")
            val listener = h.sendPending(pending)
            advanceUntilIdle()

            // The sweep delivers the server copy, with its receipts, before the ack lands.
            val swept = ackOf(pending, 7L).apply { deliveredAt = 5000L; readAt = 6000L; updatedAt = 10L }
            OfflineHub.publishForTest(
                OfflineHubEvent.MessageSyncCompleted(OfflineHub.gen, listOf(swept), SyncOutcome.CAUGHT_UP)
            )
            advanceUntilIdle()
            vm.getItems().first { it.localMessageId == pending.localMessageId }.id shouldBe 7L

            val ack = ackOf(pending, 7L)
            listener.onMessageSent(ack)
            advanceUntilIdle()
            val row = vm.getItems().single { it.id == 7L }
            row.deliveredAt shouldBe 5000L
            row.readAt shouldBe 6000L
            // The SDK's ack object itself is never written (the kit stamps a display copy).
            ack.deliveredAt shouldBe 0L
            ack.readAt shouldBe 0L
        }
    }

    test("held receipts clear on a session end") {
        runTest {
            val vm = h.open()
            MessageListOfflineSendRows.applyReceipt(vm, h.receipt(9L, delivered = true), delivered = true)
            OfflineHub.onLogout()
            advanceUntilIdle()
            vm.offlineSendRowsState.heldReceipts.size shouldBe 0
        }
    }
})
