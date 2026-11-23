package com.cometchat.uikit.core.viewmodel.threadheader

import android.util.Log
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.viewmodel.CometChatThreadHeaderViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor

/**
 * Pin/save state of the thread's parent in [CometChatThreadHeaderViewModel].
 *
 * Regression for ENG-38916 (saved parent icon missing in thread view): the header never listened
 * to the SDK's pin/save events, and an edit of the parent replaced it wholesale — so the parent
 * bubble in the thread could not reflect a save/pin made from the list, from inside the thread, or
 * from another device until the screen was reopened.
 *
 * Uses real SDK models (clone + field writes are the behaviour under test).
 *
 * Run: ./gradlew :chatuikit-core:testDebugUnitTest --tests "*ThreadHeaderPinSaveTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThreadHeaderPinSaveTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()

    lateinit var logMock: MockedStatic<Log>
    lateinit var cometChatMock: MockedStatic<CometChat>

    fun parent(id: Long = 100L, text: String = "parent", pinnedAt: Long = 0L, pinnedBy: String? = null, savedAt: Long = 0L) =
        TextMessage("peer-1", text, CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            this.sentAt = 1735689600L
            this.sender = User().apply { uid = "peer-1"; name = "Peer" }
            this.replyCount = 2
            this.pinnedAt = pinnedAt
            this.pinnedBy = pinnedBy
            this.savedAt = savedAt
        }

    /** A VM with its SDK listener registered and captured. */
    fun listeningViewModel(): Pair<CometChatThreadHeaderViewModel, CometChat.MessageListener> {
        val vm = CometChatThreadHeaderViewModel(enableListeners = true)
        vm.addListener()
        val captor = argumentCaptor<CometChat.MessageListener>()
        cometChatMock.verify { CometChat.addMessageListener(any(), captor.capture()) }
        return vm to captor.lastValue
    }

    beforeTest {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
        })
        Dispatchers.setMain(testDispatcher)
        logMock = Mockito.mockStatic(Log::class.java)
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
    }

    afterTest {
        cometChatMock.close()
        logMock.close()
        Dispatchers.resetMain()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    test("editing a SAVED parent keeps the saved indicator on the header") {
        val vm = CometChatThreadHeaderViewModel(enableListeners = false)
        vm.setParentMessage(parent(savedAt = 1700000000L))

        vm.updateParentMessageInList(parent(text = "edited"))

        val shown = vm.parentMessageListStateFlow.value.single()
        (shown as TextMessage).text shouldBe "edited"
        shown.isSaved shouldBe true
        vm.getCurrentParentMessage()!!.isSaved shouldBe true
    }

    test("editing a PINNED parent keeps pinnedAt and pinnedBy") {
        val vm = CometChatThreadHeaderViewModel(enableListeners = false)
        vm.setParentMessage(parent(pinnedAt = 1700000000L, pinnedBy = "admin"))

        vm.updateParentMessageInList(parent(text = "edited"))

        val shown = vm.parentMessageListStateFlow.value.single()
        shown.isPinned shouldBe true
        shown.pinnedBy shouldBe "admin"
    }

    test("a save event for the parent flips the header's indicator on") {
        val (vm, listener) = listeningViewModel()
        vm.setParentMessage(parent())

        listener.onMessageSaved(parent(savedAt = 1700000000L))

        vm.parentMessageListStateFlow.value.single().isSaved shouldBe true
        vm.getCurrentParentMessage()!!.savedAt shouldBe 1700000000L
    }

    test("a pin event for the parent stamps a timestamp even when the echo omits pinnedAt") {
        val (vm, listener) = listeningViewModel()
        vm.setParentMessage(parent())

        listener.onMessagePinned(parent(pinnedAt = 0L, pinnedBy = "peer-1"))

        val shown = vm.parentMessageListStateFlow.value.single()
        shown.isPinned shouldBe true
        shown.pinnedBy shouldBe "peer-1"
    }

    test("unpin / unsave events clear the header's indicators") {
        val (vm, listener) = listeningViewModel()
        vm.setParentMessage(parent(pinnedAt = 1700000000L, pinnedBy = "admin", savedAt = 1700000001L))

        listener.onMessageUnpinned(parent())
        listener.onMessageUnsaved(parent())

        val shown = vm.parentMessageListStateFlow.value.single()
        shown.isPinned shouldBe false
        shown.pinnedBy shouldBe null
        shown.isSaved shouldBe false
    }

    test("pin/save events for another message leave the parent alone") {
        val (vm, listener) = listeningViewModel()
        vm.setParentMessage(parent())

        listener.onMessageSaved(parent(id = 999L, savedAt = 1700000000L))

        vm.parentMessageListStateFlow.value.single().isSaved shouldBe false
    }

    test("the held instance is never mutated — the StateFlow sees a new value") {
        val (vm, listener) = listeningViewModel()
        val held = parent()
        vm.setParentMessage(held)

        listener.onMessageSaved(parent(savedAt = 1700000000L))

        held.isSaved shouldBe false
        (vm.parentMessageListStateFlow.value.single() === held) shouldBe false
    }

    // ---- ENG-41486 (D-327, G11): kit events, since SDK 5.1.2 sends no own echo ----

    /** A VM collecting kit events; the bus delivers on its own thread, so callers poll. */
    fun kitEventViewModel(): CometChatThreadHeaderViewModel =
        CometChatThreadHeaderViewModel(enableListeners = true).apply { addLocalEventListeners() }

    fun awaitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(10)
    }

    test("an unpin from the pinned panel (kit event) clears the header's pin") {
        val vm = kitEventViewModel()
        vm.setParentMessage(parent(pinnedAt = 1700000000L, pinnedBy = "me"))

        CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageUnpinned(TextMessage("", "", "").apply { id = 100L }))
        awaitUntil { !vm.parentMessageListStateFlow.value.single().isPinned }

        val shown = vm.parentMessageListStateFlow.value.single()
        shown.isPinned shouldBe false
        shown.pinnedBy shouldBe null
        vm.removeListener()
    }

    test("an unsave from the saved panel (kit event) clears the header's save") {
        val vm = kitEventViewModel()
        vm.setParentMessage(parent(savedAt = 1700000000L))

        CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageUnsaved(parent()))
        awaitUntil { !vm.parentMessageListStateFlow.value.single().isSaved }

        vm.parentMessageListStateFlow.value.single().isSaved shouldBe false
        vm.removeListener()
    }

    test("a pin/save made on a message list (kit event) reaches the header") {
        val vm = kitEventViewModel()
        vm.setParentMessage(parent())

        CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessagePinned(parent(pinnedAt = 1700000001L, pinnedBy = "me")))
        CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageSaved(parent(savedAt = 1700000002L)))
        awaitUntil { vm.parentMessageListStateFlow.value.single().let { it.isPinned && it.isSaved } }

        val shown = vm.parentMessageListStateFlow.value.single()
        shown.pinnedBy shouldBe "me"
        shown.isSaved shouldBe true
        vm.removeListener()
    }

    test("a kit event the header already shows, or for another message, is a no-op") {
        val vm = kitEventViewModel()
        vm.setParentMessage(parent(savedAt = 1700000000L))
        val before = vm.parentMessageListStateFlow.value.single()

        CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageSaved(parent(savedAt = 1700000000L)))
        CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessagePinned(parent(id = 999L, pinnedAt = 1700000001L)))
        Thread.sleep(300)

        (vm.parentMessageListStateFlow.value.single() === before) shouldBe true
        vm.removeListener()
    }
})
