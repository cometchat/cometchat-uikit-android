package com.cometchat.uikit.core.offline

import android.util.Log
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatMessageEvent
import kotlinx.coroutines.launch
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * W5 (ENG-41090) — own message pin/save (G11, BEH-FLAG-04). With offline on the acting list's row
 * updates from `onSuccess`; a listener echo (SDK 5.1.0 sends one, 5.1.1 does not), before or
 * after, is a set-state that does nothing when the row already matches; an opposite change applies. Per T4 the same holds with offline off, and each kit event fires once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnPinSaveEchoTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var logMock: MockedStatic<Log>
    lateinit var cometChatMock: MockedStatic<CometChat>
    val stores = mutableListOf<androidx.lifecycle.ViewModelStore>()
    val peer = "peer"

    fun message(id: Long, pinnedAt: Long = 0L, savedAt: Long = 0L) =
        TextMessage(peer, "m$id", CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            this.sentAt = 1735689600L
            this.sender = User().apply { uid = peer; name = peer }
            this.pinnedAt = pinnedAt
            this.savedAt = savedAt
        }

    fun echo(id: Long, pinnedAt: Long = 0L, savedAt: Long = 0L, pinnedBy: String? = null) =
        TextMessage("", "", "").apply {
            this.id = id; this.pinnedAt = pinnedAt; this.savedAt = savedAt; this.pinnedBy = pinnedBy
        }

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }

    beforeTest {
        OfflineHub.resetForTest()
        logMock = Mockito.mockStatic(Log::class.java)
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        cometChatMock.`when`<User> { CometChat.getLoggedInUser() }.thenReturn(User().apply { uid = "me" })
    }

    afterTest {
        stores.forEach { it.clear() }
        stores.clear()
        cometChatMock.close()
        logMock.close()
        OfflineGate.testOverride = null
    }

    suspend fun vmWith(offline: Boolean, rows: List<BaseMessage>): Pair<CometChatMessageListViewModel, CometChat.MessageListener> {
        OfflineGate.testOverride = offline
        val repository = mock<MessageListRepository> {
            on { hasMorePreviousMessages() } doReturn false
        }
        whenever(repository.fetchPreviousMessages()).thenReturn(Result.success(rows))
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
        vm.setUser(User().apply { uid = peer; name = peer })
        vm.fetchMessages()
        val captor = argumentCaptor<CometChat.MessageListener>()
        cometChatMock.verify({ CometChat.addMessageListener(any(), captor.capture()) }, atLeastOnce())
        return vm to captor.lastValue
    }

    fun row(vm: CometChatMessageListViewModel) = vm.getItems().single()

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    test("onSuccess alone updates the row (SDK 5.1.1: no echo)") {
        runTest {
            val (vm, _) = vmWith(offline = true, rows = listOf(message(1)))
            advanceUntilIdle()
            vm.onOwnPinSaveSuccess(UIKitConstants.MessageOption.PIN, row(vm), echo(1, pinnedAt = 50, pinnedBy = "me"))
            row(vm).isPinned shouldBe true
            row(vm).pinnedBy shouldBe "me"
            vm.onOwnPinSaveSuccess(UIKitConstants.MessageOption.SAVE, row(vm), null)
            row(vm).isSaved shouldBe true
        }
    }

    test("an echo after onSuccess is a no-op (SDK 5.1.0)") {
        runTest {
            val (vm, listener) = vmWith(offline = true, rows = listOf(message(1)))
            advanceUntilIdle()
            vm.onOwnPinSaveSuccess(UIKitConstants.MessageOption.PIN, row(vm), echo(1, pinnedAt = 50, pinnedBy = "me"))
            val afterSuccess = row(vm)
            listener.onMessagePinned(echo(1, pinnedAt = 50, pinnedBy = "me"))
            (row(vm) === afterSuccess) shouldBe true // not even re-emitted
        }
    }

    test("an echo before onSuccess applies once; onSuccess is then a no-op") {
        runTest {
            val (vm, listener) = vmWith(offline = true, rows = listOf(message(1)))
            advanceUntilIdle()
            listener.onMessageSaved(echo(1, savedAt = 60))
            val afterEcho = row(vm)
            afterEcho.isSaved shouldBe true
            vm.onOwnPinSaveSuccess(UIKitConstants.MessageOption.SAVE, afterEcho, echo(1, savedAt = 60))
            (row(vm) === afterEcho) shouldBe true
        }
    }

    test("own save here, then an unsave from this user's other device right after, is applied") {
        runTest {
            val (vm, listener) = vmWith(offline = true, rows = listOf(message(1)))
            advanceUntilIdle()
            vm.onOwnPinSaveSuccess(UIKitConstants.MessageOption.SAVE, row(vm), echo(1, savedAt = 60))
            row(vm).isSaved shouldBe true
            listener.onMessageUnsaved(echo(1)) // device B, inside what used to be the echo window
            row(vm).isSaved shouldBe false
        }
    }

    /** Runs [block] while counting pin/save kit events (the bus emits on its own scope). */
    fun pinSaveEvents(block: () -> Unit): List<CometChatMessageEvent> {
        val seen = java.util.Collections.synchronizedList(mutableListOf<CometChatMessageEvent>())
        val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            CometChatEvents.messageEvents.collect {
                if (it is CometChatMessageEvent.MessagePinned || it is CometChatMessageEvent.MessageUnpinned ||
                    it is CometChatMessageEvent.MessageSaved || it is CometChatMessageEvent.MessageUnsaved
                ) seen += it
            }
        }
        Thread.sleep(150)
        block()
        Thread.sleep(300)
        job.cancel()
        return seen.toList()
    }

    for (offline in listOf(true, false)) {
        val mode = if (offline) "offline on" else "offline off"

        test("$mode: onSuccess updates the row and fires one kit event; a later echo adds none") {
            runTest {
                val (vm, listener) = vmWith(offline = offline, rows = listOf(message(1)))
                advanceUntilIdle()
                val events = pinSaveEvents {
                    vm.onOwnPinSaveSuccess(UIKitConstants.MessageOption.PIN, row(vm), echo(1, pinnedAt = 50, pinnedBy = "me"))
                    listener.onMessagePinned(echo(1, pinnedAt = 50, pinnedBy = "me")) // 5.1.0 echo
                }
                row(vm).isPinned shouldBe true
                events.size shouldBe 1
                (events.single() is CometChatMessageEvent.MessagePinned) shouldBe true
            }
        }

        test("$mode: an echo before onSuccess fires one kit event; onSuccess adds none") {
            runTest {
                val (vm, listener) = vmWith(offline = offline, rows = listOf(message(1)))
                advanceUntilIdle()
                val events = pinSaveEvents {
                    listener.onMessageSaved(echo(1, savedAt = 60))
                    vm.onOwnPinSaveSuccess(UIKitConstants.MessageOption.SAVE, row(vm), echo(1, savedAt = 60))
                }
                row(vm).isSaved shouldBe true
                events.size shouldBe 1
            }
        }
    }

    test("another member's unpin within the echo window is applied") {
        runTest {
            val (vm, listener) = vmWith(offline = true, rows = listOf(message(1)))
            advanceUntilIdle()
            vm.onOwnPinSaveSuccess(UIKitConstants.MessageOption.PIN, row(vm), echo(1, pinnedAt = 50, pinnedBy = "me"))
            listener.onMessageUnpinned(echo(1)) // another member unpins at once
            row(vm).isPinned shouldBe false
        }
    }

    test("another member's pin within the echo window is applied") {
        runTest {
            val (vm, listener) = vmWith(offline = true, rows = listOf(message(1)))
            advanceUntilIdle()
            vm.onOwnPinSaveSuccess(UIKitConstants.MessageOption.UNPIN, row(vm), echo(1))
            listener.onMessagePinned(echo(1, pinnedAt = 70, pinnedBy = "other"))
            row(vm).isPinned shouldBe true
            row(vm).pinnedBy shouldBe "other"
        }
    }
})
