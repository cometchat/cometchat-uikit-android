package com.cometchat.uikit.core.offline

import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.MessageFailureReason
import com.cometchat.chat.models.MessageStatus
import com.cometchat.uikit.core.constants.UIKitConstants
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.any

/**
 * Retry and Delete on unsent and failed rows (G4, D33a/D33b; BEH-SEND-20/36): outbox vs direct,
 * in-flight → "Sending… try again in a moment", not-found → row removed, double tap ignored.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RetryDeleteTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    val h = SendRowsHarness()
    val context = mock<android.content.Context>()

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }
    afterTest { h.close() }

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    test("Retry, outbox, offline: retryMessage(localId); Not sent yet until the next real transition") {
        runTest {
            val vm = h.open()
            val msg = h.ownText("a")
            val listener = h.sendPending(msg)
            msg.status = MessageStatus.FAILED
            msg.failureReason = MessageFailureReason.TRANSIENT
            listener.onError(CometChatException("ERR", "5xx"))
            advanceUntilIdle()
            OfflineRowState.caption(vm.getItems().single(), true) shouldBe OfflineRowState.Caption.NOT_SENT_TAP_TO_RETRY

            vm.handleMessageOptionClick(context, UIKitConstants.MessageOption.RETRY, msg) shouldBe true
            h.sdk.retries.single().first shouldBe msg.localMessageId
            // The SDK resets only its own live copy; the row's object still says FAILED.
            msg.status shouldBe MessageStatus.FAILED
            OfflineRowState.caption(vm.getItems().single(), true) shouldBe OfflineRowState.Caption.NOT_SENT_YET

            // What the SDK really sends on an offline retry: the not-connected notice, no pending.
            h.sdk.retries.single().second.onError(CometChatException("ERROR_INTERNET_UNAVAILABLE", "offline"))
            advanceUntilIdle()
            OfflineRowState.caption(vm.getItems().single(), true) shouldBe OfflineRowState.Caption.NOT_SENT_YET

            // The next real transition (the outbox fails it again on reconnect) ends the overlay.
            OfflineHub.reportSend(msg, MessageSendState.FAILED, MessageFailureReason.TRANSIENT, true)
            advanceUntilIdle()
            OfflineRowState.caption(vm.getItems().single(), true) shouldBe OfflineRowState.Caption.NOT_SENT_TAP_TO_RETRY
        }
    }

    test("Retry, outbox, online: the ack replaces the row") {
        runTest {
            val vm = h.open()
            val msg = h.ownText("a")
            val listener = h.sendPending(msg)
            msg.status = MessageStatus.FAILED
            msg.failureReason = MessageFailureReason.TRANSIENT
            listener.onError(CometChatException("ERR", "5xx"))
            advanceUntilIdle()

            vm.handleMessageOptionClick(context, UIKitConstants.MessageOption.RETRY, msg)
            h.sdk.retries.single().second.onMessageSent(ackOf(msg, 77L))
            advanceUntilIdle()
            vm.getItems().single().id shouldBe 77L
        }
    }

    test("Retry, direct: a fresh send through the helper; the old row is replaced") {
        runTest {
            val vm = h.open()
            val msg = h.ownText("a")
            OfflineSend.interceptTextMessage(msg, null)
            h.sdk.sends.single().second.onError(CometChatException("ERR_X", "boom"))
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 1

            vm.handleMessageOptionClick(context, UIKitConstants.MessageOption.RETRY, msg)
            h.sdk.retries shouldHaveSize 0
            h.sdk.sends shouldHaveSize 2
            val resent = h.sdk.sends.last().first
            (resent.metadata?.has("error") ?: false) shouldBe false
            resent.status = MessageStatus.PENDING
            h.sdk.sends.last().second.onMessagePending(resent)
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 1
            OfflineRowState.caption(vm.getItems().single(), true) shouldBe OfflineRowState.Caption.NOT_SENT_YET
        }
    }

    test("Delete, outbox and pending: discardLocalMessage, row removed on success") {
        runTest {
            val vm = h.open()
            val queued = h.ownText("q")
            h.sendPending(queued)
            val failed = h.ownText("f")
            val l2 = h.sendPending(failed)
            failed.status = MessageStatus.FAILED
            failed.failureReason = MessageFailureReason.PERMANENT
            l2.onError(CometChatException("ERR", "x"))
            advanceUntilIdle()

            vm.deleteMessage(queued)
            vm.deleteMessage(failed)
            h.sdk.discards.map { it.first } shouldBe listOf(queued.localMessageId, failed.localMessageId)
            vm.getItems() shouldHaveSize 2 // nothing removed before the SDK answers
            h.sdk.discards.forEach { it.second.onSuccess(it.first) }
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 0
            verify(h.repository, never()).deleteMessage(any())
        }
    }

    test("Delete, direct: removed from memory with no SDK call") {
        runTest {
            val vm = h.open()
            val msg = h.ownText("a")
            OfflineSend.interceptTextMessage(msg, null)
            h.sdk.sends.single().second.onError(CometChatException("ERR_X", "boom"))
            advanceUntilIdle()
            vm.deleteMessage(vm.getItems().single())
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 0
            h.sdk.discards shouldHaveSize 0
        }
    }

    test("in flight → SendingTryAgain notice; the row is unchanged (Retry and Delete)") {
        runTest {
            val vm = h.open()
            val notices = mutableListOf<OfflineNotice>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.offlineNotices.toList(notices) }
            val msg = h.ownText("a")
            h.sendPending(msg)
            advanceUntilIdle()

            vm.handleMessageOptionClick(context, UIKitConstants.MessageOption.RETRY, msg)
            h.sdk.retries.single().second.onError(CometChatException(OfflineSend.ERROR_IN_FLIGHT, "busy"))
            vm.deleteMessage(msg)
            h.sdk.discards.single().second.onError(CometChatException(OfflineSend.ERROR_IN_FLIGHT, "busy"))
            advanceUntilIdle()
            notices shouldBe listOf(OfflineNotice.SendingTryAgain, OfflineNotice.SendingTryAgain)
            vm.getItems() shouldHaveSize 1
        }
    }

    test("not found on Delete → the row is removed") {
        runTest {
            val vm = h.open()
            val msg = h.ownText("a")
            h.sendPending(msg)
            advanceUntilIdle()
            vm.deleteMessage(msg)
            h.sdk.discards.single().second.onError(CometChatException(OfflineSend.ERROR_NOT_FOUND, "gone"))
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 0
        }
    }

    test("not found on Retry → the row is removed (ruled, Aditya 2026-11-04)") {
        runTest {
            val vm = h.open()
            val msg = h.ownText("a")
            val listener = h.sendPending(msg)
            msg.status = MessageStatus.FAILED
            msg.failureReason = MessageFailureReason.PERMANENT
            listener.onError(CometChatException("ERR", "x"))
            advanceUntilIdle()
            vm.handleMessageOptionClick(context, UIKitConstants.MessageOption.RETRY, msg)
            h.sdk.retries.single().second.onError(CometChatException(OfflineSend.ERROR_NOT_FOUND, "gone"))
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 0
        }
    }

    test("a double tap is ignored while a Retry or Delete is in flight") {
        runTest {
            val vm = h.open()
            val msg = h.ownText("a")
            h.sendPending(msg)
            advanceUntilIdle()
            vm.handleMessageOptionClick(context, UIKitConstants.MessageOption.RETRY, msg)
            vm.handleMessageOptionClick(context, UIKitConstants.MessageOption.RETRY, msg)
            vm.deleteMessage(msg)
            h.sdk.retries shouldHaveSize 1
            h.sdk.discards shouldHaveSize 0
        }
    }

    test("Delete on a sent message calls today's server delete (G10)") {
        runTest {
            val sent = h.served(5L)
            val vm = h.open(seeded = listOf(sent)) { repo ->
                org.mockito.kotlin.whenever(kotlinx.coroutines.runBlocking { repo.deleteMessage(any()) })
                    .thenReturn(Result.success(sent))
            }
            advanceUntilIdle()
            vm.deleteMessage(sent)
            advanceUntilIdle()
            verify(h.repository).deleteMessage(sent)
            h.sdk.discards shouldHaveSize 0
        }
    }

    test("Retry is not handled with offline off") {
        runTest {
            val vm = h.open(offlineOn = false)
            vm.handleMessageOptionClick(context, UIKitConstants.MessageOption.RETRY, h.ownText("a")) shouldBe false
            h.sdk.retries shouldHaveSize 0
        }
    }
})
