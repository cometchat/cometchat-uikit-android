package com.cometchat.uikit.core.offline

import android.util.Log
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
 * ENG-41486 (D-327, G11, BEH-FLAG-04): SDK 5.1.2 sends no own pin/save echo, so an unpin/unsave
 * from the pinned or saved panel reaches the message list only as a `MessageUnpinned/Unsaved` kit
 * event. The list applies such an event to a row it shows, never re-emits it, and treats its own
 * event and an SDK 5.1.0 echo as no-ops. Both offline modes.
 *
 * Run: ./gradlew :chatuikit-core:testDebugUnitTest --tests "*PinSaveKitEventTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PinSaveKitEventTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var logMock: MockedStatic<Log>
    lateinit var cometChatMock: MockedStatic<CometChat>
    val stores = mutableListOf<androidx.lifecycle.ViewModelStore>()
    val peer = "peer"

    fun message(id: Long, pinnedAt: Long = 0L, savedAt: Long = 0L, pinnedBy: String? = null) =
        TextMessage(peer, "m$id", CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            this.sentAt = 1735689600L
            this.sender = User().apply { uid = peer; name = peer }
            this.pinnedAt = pinnedAt
            this.pinnedBy = pinnedBy
            this.savedAt = savedAt
        }

    /** A partial message, as a REST pin/save response or a panel row may be. */
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

    /** Polls until [condition] holds (the bus delivers on its own thread). */
    fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(10)
    }

    /** Runs [block] while recording pin/save kit events; waits for the bus to settle. */
    fun pinSaveEvents(block: () -> Unit): List<CometChatMessageEvent> {
        val seen = java.util.Collections.synchronizedList(mutableListOf<CometChatMessageEvent>())
        val job = kotlinx.coroutines.CoroutineScope(Dispatchers.Default).launch {
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

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    for (offline in listOf(true, false)) {
        val mode = if (offline) "offline on" else "offline off"

        test("$mode: an unpin from the pinned panel updates the list row, with no second emit") {
            runTest {
                val (vm, _) = vmWith(offline, listOf(message(1, pinnedAt = 50, pinnedBy = "me")))
                advanceUntilIdle()
                val events = pinSaveEvents {
                    CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageUnpinned(echo(1)))
                    awaitUntil { !row(vm).isPinned }
                }
                row(vm).isPinned shouldBe false
                row(vm).pinnedBy shouldBe null
                events.size shouldBe 1 // the panel's event only; the list does not re-emit
            }
        }

        test("$mode: an unsave from the saved panel updates the list row, with no second emit") {
            runTest {
                val (vm, _) = vmWith(offline, listOf(message(1, savedAt = 60)))
                advanceUntilIdle()
                val events = pinSaveEvents {
                    CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageUnsaved(echo(1)))
                    awaitUntil { !row(vm).isSaved }
                }
                row(vm).isSaved shouldBe false
                events.size shouldBe 1
            }
        }

        test("$mode: the list's own event comes back as a no-op, with one emit in all") {
            runTest {
                val (vm, _) = vmWith(offline, listOf(message(1)))
                advanceUntilIdle()
                lateinit var afterSuccess: BaseMessage
                val events = pinSaveEvents {
                    vm.onOwnPinSaveSuccess(UIKitConstants.MessageOption.SAVE, row(vm), echo(1, savedAt = 60))
                    afterSuccess = row(vm)
                }
                (row(vm) === afterSuccess) shouldBe true
                row(vm).isSaved shouldBe true
                events.size shouldBe 1
            }
        }
    }

    test("the list's own pin event never re-applies over a newer row state") {
        runTest {
            val (vm, _) = vmWith(offline = true, rows = listOf(message(1)))
            advanceUntilIdle()
            pinSaveEvents {
                vm.onOwnPinSaveSuccess(UIKitConstants.MessageOption.PIN, row(vm), echo(1, pinnedAt = 50, pinnedBy = "me"))
                // A newer server copy (unpinned) replaces the row before the bus delivers the own event.
                vm.replaceOfflineSendRows(listOf(message(1)))
            }
            row(vm).isPinned shouldBe false
        }
    }

    test("an SDK 5.1.0 echo after the panel's unpin is a no-op and emits nothing") {
        runTest {
            val (vm, listener) = vmWith(offline = true, rows = listOf(message(1, pinnedAt = 50, pinnedBy = "me")))
            advanceUntilIdle()
            lateinit var afterEvent: BaseMessage
            val events = pinSaveEvents {
                CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageUnpinned(echo(1)))
                awaitUntil { !row(vm).isPinned }
                afterEvent = row(vm)
                listener.onMessageUnpinned(echo(1))
            }
            (row(vm) === afterEvent) shouldBe true
            events.size shouldBe 1
        }
    }

    test("a kit event whose state the row already shows is a no-op") {
        runTest {
            val (vm, _) = vmWith(offline = true, rows = listOf(message(1, savedAt = 60)))
            advanceUntilIdle()
            val before = row(vm)
            pinSaveEvents { CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageSaved(echo(1, savedAt = 60))) }
            (row(vm) === before) shouldBe true
        }
    }

    test("a kit event for a message the list does not show is ignored") {
        runTest {
            val (vm, _) = vmWith(offline = true, rows = listOf(message(1)))
            advanceUntilIdle()
            val before = vm.getItems()
            pinSaveEvents {
                CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessagePinned(message(2, pinnedAt = 70, pinnedBy = "me")))
            }
            vm.getItems() shouldBe before
            row(vm).isPinned shouldBe false
        }
    }
})
