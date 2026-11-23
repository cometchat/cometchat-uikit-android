package com.cometchat.uikit.core.offline

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.TextMessage
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.constants.UIKitConstants
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock

/**
 * W4 × W5 integration — offline turned off mid-session (README §3.7). The rows W5 turns failed are
 * W4 direct failures: Retry is a fresh send (today's path now that offline is off) and Delete
 * removes the row from memory. A send made after the turn-off gets today's pending row and
 * today's failure marking on a list that bound with offline on.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineTurnedOffSendTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    val h = SendRowsHarness()
    val context = mock<android.content.Context>()

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }
    beforeTest {
        OfflineOffTransition.resetForTest()
        OfflineOffTransition.coalesceMs = 50L
    }
    afterTest { h.close() }

    /** Today's reconnect fetch runs once offline is off; give it empty pages. */
    fun openVm() = h.open { repo ->
        org.mockito.kotlin.whenever(kotlinx.coroutines.runBlocking { repo.fetchActionMessages(any()) })
            .thenReturn(Result.success(emptyList()))
        org.mockito.kotlin.whenever(kotlinx.coroutines.runBlocking { repo.fetchNextMessages(any()) })
            .thenReturn(Result.success(emptyList()))
    }

    /** The legacy bus emits on its own scope; give it time, then drain Main. */
    fun settleBus() = Thread.sleep(300)

    /**
     * Delivers a legacy `MessageSent` to the list's private handler, as the bus collector does
     * (the bus dispatches on its own scope, which the test dispatcher cannot order).
     */
    fun com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel.legacySent(
        message: com.cometchat.chat.models.BaseMessage,
        status: com.cometchat.uikit.core.events.MessageStatus
    ) {
        com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel::class.java
            .getDeclaredMethod("handleMessageSentEvent", com.cometchat.uikit.core.events.CometChatMessageEvent.MessageSent::class.java)
            .apply { isAccessible = true }
            .invoke(this, com.cometchat.uikit.core.events.CometChatMessageEvent.MessageSent(message, status))
    }

    fun turnOffAndReconnect() {
        OfflineGate.testOverride = false
        val captor = argumentCaptor<CometChat.ConnectionListener>()
        h.cometChat.verify({ CometChat.addConnectionListener(any(), captor.capture()) }, atLeastOnce())
        captor.lastValue.onConnected()
    }

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    test("Retry on a row failed by the turn-off is a fresh send, not sdk.retry") {
        runTest {
            val vm = openVm()
            val msg = h.ownText("a")
            h.sendPending(msg)
            advanceUntilIdle()
            turnOffAndReconnect()
            advanceUntilIdle()
            val failed = vm.getItems().single()
            OfflineRowState.isFailed(failed) shouldBe true
            OfflineSend.isDirectFailure(failed) shouldBe true

            vm.handleMessageOptionClick(context, UIKitConstants.MessageOption.RETRY, failed)
            settleBus()
            advanceUntilIdle()
            h.sdk.retries shouldHaveSize 0
            val sent = argumentCaptor<TextMessage>()
            h.cometChat.verify { CometChat.sendMessage(sent.capture(), any<CometChat.CallbackListener<TextMessage>>()) }
            (sent.lastValue.metadata?.has("error") ?: false) shouldBe false
            vm.legacySent(sent.lastValue, com.cometchat.uikit.core.events.MessageStatus.IN_PROGRESS)
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 1
            OfflineRowState.isFailed(vm.getItems().single()) shouldBe false
        }
    }

    test("Delete on a row failed by the turn-off removes it from memory, no SDK call") {
        runTest {
            val vm = openVm()
            h.sendPending(h.ownText("a"))
            advanceUntilIdle()
            turnOffAndReconnect()
            advanceUntilIdle()
            vm.deleteMessage(vm.getItems().single())
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 0
            h.sdk.discards shouldHaveSize 0
        }
    }

    test("a send after the turn-off shows today's pending row and today's failure") {
        runTest {
            val vm = openVm()
            advanceUntilIdle()
            OfflineGate.testOverride = false
            val msg = h.ownText("late")
            CometChatUIKit.sendTextMessage(msg, null)
            settleBus()
            advanceUntilIdle()
            h.sdk.sends shouldHaveSize 0 // not the offline helper
            vm.legacySent(msg, com.cometchat.uikit.core.events.MessageStatus.IN_PROGRESS)
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 1 // today's IN_PROGRESS row

            val cb = argumentCaptor<CometChat.CallbackListener<TextMessage>>()
            h.cometChat.verify { CometChat.sendMessage(any<TextMessage>(), cb.capture()) }
            cb.lastValue.onError(CometChatException("ERR_X", "boom"))
            vm.legacySent(msg, com.cometchat.uikit.core.events.MessageStatus.ERROR)
            advanceUntilIdle()
            vm.getItems() shouldHaveSize 1
            OfflineRowState.isFailed(vm.getItems().single()) shouldBe true
        }
    }
})
