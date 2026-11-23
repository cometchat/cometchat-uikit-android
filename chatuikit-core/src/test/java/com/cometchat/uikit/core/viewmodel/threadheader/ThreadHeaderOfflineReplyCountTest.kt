package com.cometchat.uikit.core.viewmodel.threadheader

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.MessageFailureReason
import com.cometchat.chat.models.MessageStatus as SdkMessageStatus
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.events.MessageStatus
import com.cometchat.uikit.core.offline.MessageSendState
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineHub
import com.cometchat.uikit.core.offline.OfflineHubEvent
import com.cometchat.uikit.core.viewmodel.CometChatThreadHeaderViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * ENG-41541 (BEH-MUT-10): the SDK keeps the user's own unsent replies in the parent's stored count
 * (+1 at enqueue, −1 when the user deletes one from the device). The header starts from its parent
 * snapshot and applies only the deltas the SDK applies after it: queued while bound → +1 once; sent
 * → nothing; device delete → −1 once, counted here or not; eviction/purge → nothing. Re-bind resets
 * to the new snapshot. Offline off counts only acknowledged sends, as before.
 *
 * Run: ./gradlew :chatuikit-core:testDebugUnitTest --tests "*ThreadHeaderOfflineReplyCountTest"
 */
class ThreadHeaderOfflineReplyCountTest : FunSpec({

    val dispatcher = UnconfinedTestDispatcher()
    lateinit var cometChatStatic: MockedStatic<CometChat>
    val live = mutableListOf<CometChatThreadHeaderViewModel>()

    beforeSpec {
        Dispatchers.setMain(dispatcher)
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
        })
    }

    afterSpec {
        ArchTaskExecutor.getInstance().setDelegate(null)
        Dispatchers.resetMain()
    }

    beforeTest {
        OfflineHub.resetForTest()
        cometChatStatic = Mockito.mockStatic(CometChat::class.java)
        val loggedIn = mock<User>()
        whenever(loggedIn.uid).thenReturn("me")
        cometChatStatic.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(loggedIn)
    }

    afterTest {
        live.forEach { vm ->
            vm.removeListener()
            vm.viewModelScope.cancel()
            runBlocking { vm.viewModelScope.coroutineContext.job.join() }
        }
        live.clear()
        cometChatStatic.close()
        OfflineGate.testOverride = null
    }

    fun parent(stored: Int): BaseMessage =
        TextMessage("peer", "parent", CometChatConstants.RECEIVER_TYPE_USER).apply {
            id = 100
            replyCount = stored
        }

    /** A header for parent 100 with [stored] replies, listening, created in the given mode. */
    fun header(offlineOn: Boolean = true, stored: Int = 0): CometChatThreadHeaderViewModel {
        OfflineGate.testOverride = offlineOn
        val vm = CometChatThreadHeaderViewModel(enableListeners = true)
        live += vm
        vm.setParentMessage(parent(stored))
        vm.addLocalEventListeners()
        return vm
    }

    fun reply(localId: String, parentId: Long = 100, serverId: Long = 0): BaseMessage =
        TextMessage("peer", "reply $localId", CometChatConstants.RECEIVER_TYPE_USER).apply {
            parentMessageId = parentId
            localMessageId = localId
            sender = User("me", "Me")
            if (serverId > 0) id = serverId
        }

    fun send(message: BaseMessage, state: MessageSendState, reason: MessageFailureReason? = null) =
        OfflineHub.publishForTest(
            OfflineHubEvent.SendState(OfflineHub.gen, message.localMessageId, message, state, reason)
        )

    fun legacySent(message: BaseMessage) = runBlocking {
        CometChatEvents.emitMessageEventSync(CometChatMessageEvent.MessageSent(message, MessageStatus.SUCCESS))
    }

    test("a reply queued while bound adds one to the header") {
        val vm = header(stored = 0)
        send(reply("L1"), MessageSendState.SENDING)
        vm.replyCountStateFlow.value shouldBe 1
    }

    test("a reply queued and sent while bound counts once") {
        val vm = header(stored = 2)
        send(reply("L1"), MessageSendState.SENDING)
        send(reply("L1", serverId = 900), MessageSendState.SENT)
        legacySent(reply("L1", serverId = 900))
        vm.replyCountStateFlow.value shouldBe 3
    }

    test("reopen then flush: a reply queued before the snapshot is not added again when it sends") {
        // The SDK added the queued reply to the stored count, so the reopened header reads 1.
        val vm = header(stored = 1)
        send(reply("L1", serverId = 900), MessageSendState.SENT)
        legacySent(reply("L1", serverId = 900))
        vm.replyCountStateFlow.value shouldBe 1
    }

    test("reopen then discard: deleting a reply queued before the snapshot takes it back") {
        val vm = header(stored = 1)
        val r = reply("L1")
        OfflineHub.publishForTest(OfflineHubEvent.ThreadReplyDeleted(OfflineHub.gen, "L1", r))
        OfflineHub.publishForTest(OfflineHubEvent.ThreadReplyDeleted(OfflineHub.gen, "L1", r))
        vm.replyCountStateFlow.value shouldBe 0
    }

    test("deleting a reply queued while bound takes the +1 back") {
        val vm = header(stored = 4)
        val r = reply("L1")
        send(r, MessageSendState.SENDING)
        OfflineHub.publishForTest(OfflineHubEvent.ThreadReplyDeleted(OfflineHub.gen, "L1", r))
        vm.replyCountStateFlow.value shouldBe 4
    }

    test("a direct failure the SDK never queued takes nothing back when deleted") {
        val vm = header(stored = 2)
        OfflineHub.publishForTest(OfflineHubEvent.ThreadReplyDeleted(OfflineHub.gen, "L1", reply("L1"), discarded = false))
        vm.replyCountStateFlow.value shouldBe 2
    }

    test("eviction, purge or a failure leaves the count as the SDK leaves the stored count") {
        val vm = header(stored = 1)
        send(reply("L1"), MessageSendState.SENDING)
        send(reply("L2"), MessageSendState.SENDING)
        vm.replyCountStateFlow.value shouldBe 3
        send(reply("L1"), MessageSendState.FAILED, MessageFailureReason.QUEUE_OVERFLOW)
        send(reply("L2"), MessageSendState.FAILED, MessageFailureReason.DISCARDED)
        send(reply("L3"), MessageSendState.FAILED, MessageFailureReason.PERMANENT)
        vm.replyCountStateFlow.value shouldBe 3
    }

    test("a retry of a reply that failed before the snapshot adds nothing") {
        val vm = header(stored = 1)
        val failedCopy = reply("L1").apply { status = SdkMessageStatus.FAILED }
        send(failedCopy, MessageSendState.SENDING)
        vm.replyCountStateFlow.value shouldBe 1
    }

    test("re-bind resets to the new snapshot and forgets what the old one saw") {
        val vm = header(stored = 0)
        send(reply("L1"), MessageSendState.SENDING)
        vm.replyCountStateFlow.value shouldBe 1
        // The same parent re-read (e.g. its server phase), now carrying the queued reply.
        vm.setParentMessage(parent(stored = 1))
        vm.replyCountStateFlow.value shouldBe 1
        send(reply("L1", serverId = 900), MessageSendState.SENT)
        vm.replyCountStateFlow.value shouldBe 1
        // A new snapshot that does not include it yet: the next queued reply still counts once.
        vm.setParentMessage(parent(stored = 5))
        send(reply("L2"), MessageSendState.SENDING)
        send(reply("L2"), MessageSendState.SENDING)
        vm.replyCountStateFlow.value shouldBe 6
    }

    test("a reply to another thread, or an event from an older session, is ignored") {
        val vm = header(stored = 0)
        send(reply("L1", parentId = 555), MessageSendState.SENDING)
        OfflineHub.publishForTest(
            OfflineHubEvent.SendState(OfflineHub.gen + 7, "L2", reply("L2"), MessageSendState.SENDING, null)
        )
        vm.replyCountStateFlow.value shouldBe 0
    }

    test("offline off: only the acknowledged send counts, as before") {
        val vm = header(offlineOn = false, stored = 2)
        send(reply("L1"), MessageSendState.SENDING)
        vm.replyCountStateFlow.value shouldBe 2
        legacySent(reply("", serverId = 900))
        vm.replyCountStateFlow.value shouldBe 3
    }
})
