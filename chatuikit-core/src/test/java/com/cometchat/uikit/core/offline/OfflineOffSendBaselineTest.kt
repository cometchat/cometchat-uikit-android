package com.cometchat.uikit.core.offline

import com.cometchat.chat.models.MessageStatus
import com.cometchat.uikit.core.constants.UIKitConstants
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Invariant 1 for W4: with offline off nothing of the send-row path runs. Hub send states reach
 * no row, an unsent row has no caption and keeps today's (no) options, Retry is not handled,
 * Delete runs today's server delete, and receipts keep today's stamping. The three `sendX` SDK
 * call lists are pinned by W1's `OfflineOffBaselineTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineOffSendBaselineTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    val h = SendRowsHarness()

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }
    afterTest { h.close() }

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    test("off: a hub send state touches no row and the helper is never used") {
        runTest {
            val vm = h.open(offlineOn = false)
            val msg = h.ownText("a").apply { localMessageId = "l1"; status = MessageStatus.PENDING }
            OfflineHub.publishForTest(OfflineHubEvent.SendState(OfflineHub.gen, "l1", msg, MessageSendState.SENDING, null))
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 0
            h.sdk.sends shouldHaveSize 0
        }
    }

    test("off: an unsent or failed row has no caption, and failed has no special receipt state") {
        h.open(offlineOn = false)
        val failed = h.ownText("a").apply { status = MessageStatus.FAILED }
        OfflineRowState.caption(failed, offlineOn = false) shouldBe OfflineRowState.Caption.NONE
        OfflineRowState.isUnsent(failed, offlineOn = false) shouldBe false
    }

    test("off: Retry is not handled and Delete runs today's server delete") {
        runTest {
            val pending = h.ownText("a")
            val vm = h.open(offlineOn = false) { repo ->
                runBlocking { whenever(repo.deleteMessage(any())).thenReturn(Result.success(pending)) }
            }
            vm.handleMessageOptionClick(mock(), UIKitConstants.MessageOption.RETRY, pending) shouldBe false
            vm.deleteMessage(pending)
            advanceUntilIdle()
            verify(h.repository).deleteMessage(pending)
            h.sdk.discards shouldHaveSize 0
            h.sdk.retries shouldHaveSize 0
        }
    }

    test("off: receipts keep today's stamping (no held receipts)") {
        runTest {
            val vm = h.open(offlineOn = false, seeded = listOf(h.served(1L)))
            advanceUntilIdle()
            val captor = org.mockito.ArgumentCaptor.forClass(com.cometchat.chat.core.CometChat.MessageListener::class.java)
            h.cometChat.verify {
                com.cometchat.chat.core.CometChat.addMessageListener(org.mockito.ArgumentMatchers.anyString(), captor.capture())
            }
            captor.allValues.last().onMessagesDelivered(h.receipt(9L, delivered = true))
            vm.getItems().single().deliveredAt shouldBe 5000L
            vm.offlineSendRowsState.heldReceipts.size shouldBe 0
        }
    }
})
