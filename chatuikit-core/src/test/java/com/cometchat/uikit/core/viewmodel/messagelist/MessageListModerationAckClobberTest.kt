package com.cometchat.uikit.core.viewmodel.messagelist

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.enums.ModerationStatus
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.events.MessageStatus
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * ENG-39226 defect A — the send ack must not clobber a moderation verdict.
 *
 * The bug: `on_moderation_status_changed` is measured arriving **2–21 ms before** the send ack
 * (three samples on the encrypted rig). The verdict correctly flips the optimistic row to
 * DISAPPROVED via `updateModeratedMessage`; the ack then lands carrying
 * `moderation.status = "pending"` and `updateMessageOnSuccess` replaces the row **wholesale**,
 * putting it back to pending. The final in-memory state is pending — a grey tick forever, repaired
 * only by leaving and re-entering the chat (the SDK cache was always correct).
 *
 * The fix: inside `updateMessageOnSuccess`, resolve the existing row with the predicate the
 * function already uses (muid, then id) and skip the replace when that row is already DISAPPROVED.
 * No merge — the stanza is a full message payload and has already delivered the server id, the
 * timestamps, the attachments and the metadata.
 *
 * 🔴 **Real [TextMessage] / [MediaMessage] instances only, never [MockFactory] messages.**
 * MockFactory returns Mockito mocks with stubbed getters, so `message.moderationStatus = X` is
 * swallowed and the getter keeps returning the seed — every assertion below would pin nothing.
 * Same reasoning as [MessageListReceiptRerenderTest] and [MessageListThreadReplyCountTest].
 *
 * Coverage (ticket §5):
 * 1. `moderatedMessage_verdictBeforeAck_rowStaysDisapproved` — the defect
 * 2. `moderatedMessage_verdictBeforeAck_rowStillCarriesServerId` — invariant 1, the row keeps its
 *    server id. Measured: it stays **green** under a blunt `MessageStatus.SUCCESS`-branch return,
 *    because the stanza has already delivered the id. Test 6 is the one that catches that.
 * 3. `moderatedMessage_ackBeforeVerdict_rowEndsDisapproved` — invariant 4, the other ordering
 * 4. `sendSuccess_nonModerated_updatesRowAsBefore` — invariant 9, regression guard
 * 5. `sendSuccess_messageNotInList_stillAdded` — invariant 2, the addMessage fallback
 * 6. `sendSuccess_disapproved_stillSetsLatestMessageId` — invariant 3; the test that goes red if
 *    the skip is moved to the `MessageStatus.SUCCESS` branch
 * 7. `moderatedMediaMessage_retainsAttachment` — invariant 5
 *
 * Run: ./gradlew :chatuikit-core:testDebugUnitTest --tests "*MessageListModerationAckClobberTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageListModerationAckClobberTest : FunSpec({

    isolationMode = IsolationMode.SingleInstance

    val testDispatcher = UnconfinedTestDispatcher()

    /** Us — the sender of the moderated message. */
    val myUid = "alice"

    /** The person we are chatting with; also the open conversation. */
    val partnerUid = "bob"

    /** The muid the composer stamps on the optimistic row, and that both server payloads echo. */
    val sendMuid = "1789401365703"

    /** The id the server assigns — delivered by the moderation stanza, then repeated by the ack. */
    val serverId = 252_010L

    lateinit var repository: MessageListRepository
    lateinit var cometChatMock: MockedStatic<CometChat>

    /**
     * Every ViewModel this spec builds, so `afterTest` can tear it down. With
     * `enableListeners = true` the ViewModel subscribes to the **global** `CometChatEvents` bus in
     * `init`; `Dispatchers.Main` is an unconfined test dispatcher, so a surviving subscriber would
     * run its handlers inline on whatever coroutine emits next — including events from unrelated
     * specs later in the run, after these static mocks are closed. `onCleared` is `protected`, so it
     * is called by reflection.
     */
    val liveViewModels = mutableListOf<CometChatMessageListViewModel>()

    /** A real outgoing [TextMessage] from us to the partner. */
    fun outgoing(
        id: Long,
        muid: String? = sendMuid,
        moderation: ModerationStatus? = null,
        text: String = "damn"
    ): TextMessage = TextMessage(partnerUid, text, CometChatConstants.RECEIVER_TYPE_USER).apply {
        this.id = id
        this.muid = muid
        this.sender = MockFactory.createUser(uid = myUid, name = "Alice")
        moderation?.let { this.moderationStatus = it }
    }

    /** A real outgoing image [MediaMessage] from us to the partner. */
    fun outgoingMedia(
        id: Long,
        muid: String? = sendMuid,
        moderation: ModerationStatus? = null,
        attachmentUrl: String? = null
    ): MediaMessage = MediaMessage(
        partnerUid,
        CometChatConstants.MESSAGE_TYPE_IMAGE,
        CometChatConstants.RECEIVER_TYPE_USER
    ).apply {
        this.id = id
        this.muid = muid
        this.sender = MockFactory.createUser(uid = myUid, name = "Alice")
        moderation?.let { this.moderationStatus = it }
        attachmentUrl?.let {
            this.attachment = Attachment().apply {
                fileName = "photo.jpg"
                fileUrl = it
                fileExtension = "jpg"
            }
        }
    }

    /**
     * A ViewModel for the 1:1 chat with [partnerUid], showing [messages], paired with the SDK
     * message listener it registered — the same object the SDK fans a moderation verdict to.
     *
     * `gotoMessageId = 1` keeps `markConversationRead` off the first-fetch path (it otherwise emits
     * on the global event bus and leaks coroutines between tests), matching
     * [MessageListReceiptRerenderTest].
     */
    suspend fun viewModelShowing(
        messages: List<BaseMessage> = emptyList()
    ): Pair<CometChatMessageListViewModel, CometChat.MessageListener> {
        whenever(repository.fetchPreviousMessages()).thenReturn(Result.success(messages))
        whenever(repository.hasMorePreviousMessages()).thenReturn(false)
        val vm = CometChatMessageListViewModel(repository = repository, enableListeners = true)
        liveViewModels += vm
        vm.setUser(MockFactory.createUser(uid = partnerUid, name = "Bob"), gotoMessageId = 1L)
        vm.fetchMessages()

        val listenerCaptor = argumentCaptor<CometChat.MessageListener>()
        cometChatMock.verify(
            { CometChat.addMessageListener(any(), listenerCaptor.capture()) },
            atLeastOnce()
        )
        return vm to listenerCaptor.lastValue
    }

    /**
     * Delivers the `MessageSent` event the composer emits, to the same handler dev-v6's
     * `CometChatEvents.messageEvents` collector calls. Called directly because
     * `CometChatEvents.emitMessageEvent` dispatches on the bus's own scope, which a test dispatcher
     * cannot order, and dev-v6 keeps the handler private.
     */
    fun CometChatMessageListViewModel.send(message: BaseMessage, status: MessageStatus) {
        CometChatMessageListViewModel::class.java
            .getDeclaredMethod("handleMessageSentEvent", CometChatMessageEvent.MessageSent::class.java)
            .apply { isAccessible = true }
            .invoke(this, CometChatMessageEvent.MessageSent(message, status))
    }

    /** The moderation status currently on the single row in the list. */
    fun CometChatMessageListViewModel.moderationOfSingleRow(): ModerationStatus? =
        when (val row = getItems().single()) {
            is TextMessage -> row.moderationStatus
            is MediaMessage -> row.moderationStatus
            else -> null
        }

    beforeSpec { Dispatchers.setMain(testDispatcher) }

    afterSpec {
        Thread.sleep(50)
        Dispatchers.resetMain()
    }

    beforeTest {
        repository = mock()
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        val loggedInUser = mock<User>()
        whenever(loggedInUser.uid).thenReturn(myUid)
        cometChatMock.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(loggedInUser)
        whenever(repository.hasMorePreviousMessages()).thenReturn(false)
        whenever(repository.fetchPreviousMessages()).thenReturn(Result.success(emptyList()))
        whenever(repository.markAsRead(any())).thenReturn(Result.success(Unit))
        whenever(repository.markAsDelivered(any())).thenReturn(Result.success(Unit))
        println("  🧪 ${it.name.testName}")
    }

    afterTest {
        // Tear the ViewModels down BEFORE the static mock goes away — see [liveViewModels].
        liveViewModels.forEach { vm ->
            CometChatMessageListViewModel::class.java
                .getDeclaredMethod("onCleared")
                .apply { isAccessible = true }
                .invoke(vm)
        }
        liveViewModels.clear()
        cometChatMock.close()
        println()
    }

    test("warmup - absorb leaked exceptions") {
        try { runTest(testDispatcher) { } } catch (_: Exception) { }
    }

    // ==================== 1–2. Verdict before ack (the measured ordering) ====================

    test("moderatedMessage_verdictBeforeAck_rowStaysDisapproved") {
        runTest(testDispatcher) {
            val (vm, listener) = viewModelShowing()
            advanceUntilIdle()

            // 21:26:05.788 — optimistic row, no server id yet
            vm.send(outgoing(id = 0L), MessageStatus.IN_PROGRESS)
            // 21:26:06.311 — the verdict, a full payload carrying the server id
            listener.onMessageModerated(outgoing(id = serverId, moderation = ModerationStatus.DISAPPROVED))
            // 21:26:06.317 — the ack, 6 ms later, saying "pending"
            vm.send(outgoing(id = serverId, moderation = ModerationStatus.PENDING), MessageStatus.SUCCESS)
            advanceUntilIdle()

            vm.getItemCount() shouldBe 1
            vm.moderationOfSingleRow() shouldBe ModerationStatus.DISAPPROVED
        }
    }

    test("moderatedMessage_verdictBeforeAck_rowStillCarriesServerId") {
        runTest(testDispatcher) {
            val (vm, listener) = viewModelShowing()
            advanceUntilIdle()

            vm.send(outgoing(id = 0L), MessageStatus.IN_PROGRESS)
            listener.onMessageModerated(outgoing(id = serverId, moderation = ModerationStatus.DISAPPROVED))
            vm.send(outgoing(id = serverId, moderation = ModerationStatus.PENDING), MessageStatus.SUCCESS)
            advanceUntilIdle()

            // Invariant 1: id == 0 maps to IN_PROGRESS in MessageReceiptUtils (a permanent clock) and
            // breaks dedupe + pagination anchors. The stanza delivered the id before the ack was
            // skipped, so it must still be on the row.
            vm.getItems().single().id shouldBe serverId
            vm.moderationOfSingleRow() shouldBe ModerationStatus.DISAPPROVED
        }
    }

    // ==================== 3. The other ordering ====================

    test("moderatedMessage_ackBeforeVerdict_rowEndsDisapproved") {
        runTest(testDispatcher) {
            val (vm, listener) = viewModelShowing()
            advanceUntilIdle()

            vm.send(outgoing(id = 0L), MessageStatus.IN_PROGRESS)
            // Ack first this time: the row is not yet DISAPPROVED, so it must apply normally.
            vm.send(outgoing(id = serverId, moderation = ModerationStatus.PENDING), MessageStatus.SUCCESS)
            vm.getItems().single().id shouldBe serverId
            vm.moderationOfSingleRow() shouldBe ModerationStatus.PENDING

            // The verdict then replaces it — nothing is skipped, because nothing is blocked yet.
            listener.onMessageModerated(outgoing(id = serverId, moderation = ModerationStatus.DISAPPROVED))
            advanceUntilIdle()

            vm.getItemCount() shouldBe 1
            vm.getItems().single().id shouldBe serverId
            vm.moderationOfSingleRow() shouldBe ModerationStatus.DISAPPROVED
        }
    }

    // ==================== 4. Regression guard — the ordinary send ====================

    test("sendSuccess_nonModerated_updatesRowAsBefore") {
        runTest(testDispatcher) {
            val (vm, _) = viewModelShowing()
            advanceUntilIdle()

            vm.send(outgoing(id = 0L, text = "hello"), MessageStatus.IN_PROGRESS)
            vm.getItems().single().id shouldBe 0L

            // No verdict anywhere: the ack must replace the optimistic row exactly as it always did.
            vm.send(
                outgoing(id = serverId, moderation = ModerationStatus.UNMODERATED, text = "hello"),
                MessageStatus.SUCCESS
            )
            advanceUntilIdle()

            vm.getItemCount() shouldBe 1
            vm.getItems().single().id shouldBe serverId
            vm.moderationOfSingleRow() shouldBe ModerationStatus.UNMODERATED
        }
    }

    // ==================== 5. The addMessage fallback stays reachable ====================

    test("sendSuccess_messageNotInList_stillAdded") {
        runTest(testDispatcher) {
            // A blocked row already in the list, so the skip is live — but for a DIFFERENT message.
            val blocked = outgoing(id = 252_001L, muid = "muid-blocked", moderation = ModerationStatus.DISAPPROVED)
            val (vm, _) = viewModelShowing(listOf(blocked))
            advanceUntilIdle()
            vm.getItemCount() shouldBe 1

            // An ack whose message was never optimistically added (no matching muid, no matching id).
            vm.send(outgoing(id = serverId, muid = "muid-fresh", text = "unrelated"), MessageStatus.SUCCESS)
            advanceUntilIdle()

            vm.getItemCount() shouldBe 2
            vm.getItems().last().id shouldBe serverId
        }
    }

    // ==================== 6. The SUCCESS branch still runs in full ====================

    test("sendSuccess_disapproved_stillSetsLatestMessageId") {
        runTest(testDispatcher) {
            val (vm, listener) = viewModelShowing()
            advanceUntilIdle()

            vm.send(outgoing(id = 0L), MessageStatus.IN_PROGRESS)
            listener.onMessageModerated(outgoing(id = serverId, moderation = ModerationStatus.DISAPPROVED))
            vm.send(outgoing(id = serverId, moderation = ModerationStatus.PENDING), MessageStatus.SUCCESS)
            advanceUntilIdle()

            // `latestMessageId` is private; it is observed through the guard that reads it. A real-time
            // incoming message is appended only when the list's last id == latestMessageId. If the skip
            // had been done on the MessageStatus.SUCCESS branch instead of inside
            // updateMessageOnSuccess, `latestMessageId` would still be -1 and this message would be
            // dropped as "user has scrolled up".
            val incoming = TextMessage(myUid, "hi", CometChatConstants.RECEIVER_TYPE_USER).apply {
                this.id = serverId + 1
                this.sender = MockFactory.createUser(uid = partnerUid, name = "Bob")
            }
            listener.onTextMessageReceived(incoming)
            advanceUntilIdle()

            vm.getItemCount() shouldBe 2
            vm.getItems().last().id shouldBe serverId + 1
        }
    }

    // ==================== 7. Media survives the skip ====================

    test("moderatedMediaMessage_retainsAttachment") {
        runTest(testDispatcher) {
            val (vm, listener) = viewModelShowing()
            advanceUntilIdle()

            vm.send(outgoingMedia(id = 0L), MessageStatus.IN_PROGRESS)
            // The stanza carries data.attachments complete, signed server URL included (msg 252023).
            listener.onMessageModerated(
                outgoingMedia(
                    id = serverId,
                    moderation = ModerationStatus.DISAPPROVED,
                    attachmentUrl = "https://media-us.cometchat-staging.com/252023.jpg?Expires=1&Signature=2"
                )
            )
            // The ack would also carry an attachment, but it is skipped — the stanza's must survive.
            vm.send(
                outgoingMedia(id = serverId, moderation = ModerationStatus.PENDING, attachmentUrl = "https://ack.example/other.jpg"),
                MessageStatus.SUCCESS
            )
            advanceUntilIdle()

            vm.getItemCount() shouldBe 1
            val row = vm.getItems().single() as MediaMessage
            row.moderationStatus shouldBe ModerationStatus.DISAPPROVED
            row.id shouldBe serverId
            row.attachment.shouldNotBeNull()
            row.attachment.fileUrl shouldBe
                "https://media-us.cometchat-staging.com/252023.jpg?Expires=1&Signature=2"
        }
    }
})
