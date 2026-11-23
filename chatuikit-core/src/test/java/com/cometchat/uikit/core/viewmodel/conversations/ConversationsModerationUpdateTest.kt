package com.cometchat.uikit.core.viewmodel.conversations

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.enums.ModerationStatus
import com.cometchat.chat.helpers.CometChatHelper
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.usecase.DeleteConversationUseCase
import com.cometchat.uikit.core.domain.usecase.GetConversationListUseCase
import com.cometchat.uikit.core.domain.usecase.RefreshConversationListUseCase
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * ENG-39226 defect B — the moderation verdict must reach the conversation row, live.
 *
 * Two halves, both needed:
 *
 * 1. [CometChatConversationsViewModel] registered `onMessageEdited` and `onMessageDeleted` but had
 *    **no `onMessageModerated` override at all**, so the row kept its pre-moderation `lastMessage`
 *    until the whole list was rebuilt from cache — verified on device still stale four minutes after
 *    the verdict, on both UI stacks.
 * 2. `updateConversation` replaces `lastMessage` wholesale, and the send ack (which says
 *    `moderation.status = "pending"`) lands 2–21 ms *after* the verdict. Without a guard, adding the
 *    listener achieves nothing: the ack clobbers the row exactly as it did in the message list.
 *
 * 🔴 The guard is scoped to **the same message**, not to the row. The ticket's wording ("do not
 * replace a `lastMessage` that is already DISAPPROVED with one that is not") read literally would
 * freeze the row on the blocked message forever — `updateConversation` is the single live-update
 * path for a conversation row, so every later message, unread bump and move-to-top would be dropped
 * too. `conversationRow_laterMessage_replacesDisapprovedLastMessage` pins that.
 *
 * ⚠️ Real [TextMessage] / [Conversation] instances, never `MockFactory` messages: MockFactory
 * returns Mockito mocks with stubbed getters, so a `moderationStatus = …` write is swallowed and the
 * getter keeps returning the seed — the assertions would pin nothing. `CometChatHelper` is
 * static-mocked only to map message → conversation, as in [ConversationsPendingSendTest].
 *
 * Coverage (ticket §5):
 * 7. `conversationRow_onMessageModerated_updatesLastMessage` — the defect (B1)
 * 8. `conversationRow_ackAfterVerdict_doesNotDowngrade` — B2
 * 9. `conversationRow_moderationUpdate_emitsOnStateFlow` — B3 / invariant 6
 * +  `conversationRow_laterMessage_replacesDisapprovedLastMessage` — the guard's scope
 *
 * Run: ./gradlew :chatuikit-core:testDebugUnitTest --tests "*ConversationsModerationUpdateTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsModerationUpdateTest : FunSpec({

    isolationMode = IsolationMode.SingleInstance

    val testDispatcher = UnconfinedTestDispatcher()

    val meUid = "me"
    val peerUid = "peer"
    val conversationId = "conv_user-$peerUid"
    val sendMuid = "1789401365703"
    val serverId = 252_010L

    lateinit var getConversationListUseCase: GetConversationListUseCase
    lateinit var deleteConversationUseCase: DeleteConversationUseCase
    lateinit var refreshConversationListUseCase: RefreshConversationListUseCase
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var helperMock: MockedStatic<CometChatHelper>

    /**
     * Every ViewModel this spec builds, so `afterTest` can tear it down.
     *
     * 🔴 Mandatory with `enableListeners = true`. The ViewModel subscribes to the **global**
     * `CometChatEvents` bus in `init`, and `Dispatchers.Main` is an unconfined test dispatcher, so a
     * surviving subscriber runs its handlers *inline on whatever coroutine emits next* — including
     * events emitted by completely unrelated specs later in the run. One left alive reaches
     * `refreshConversationFromServer` → `CometChat.getConversation`, whose static mock is long
     * closed, and the resulting "call CometChat.init() first" exception surfaces as
     * `UncaughtExceptionsBeforeTest` in other specs (observed: `MessageListTwoPhaseReadTest`,
     * `CometChatGroupMembersViewModelTest`). `onCleared` is `protected`, so it is called by
     * reflection — the same escape hatch several specs here already use for private state.
     */
    val liveViewModels = mutableListOf<CometChatConversationsViewModel>()

    fun user(uid: String): User = User(uid, "name-$uid")

    /** A real outgoing [TextMessage] — setters actually stick, so moderation state is observable. */
    fun outgoing(
        id: Long,
        muid: String? = sendMuid,
        moderation: ModerationStatus? = null,
        text: String = "damn"
    ): TextMessage = TextMessage(peerUid, text, CometChatConstants.RECEIVER_TYPE_USER).apply {
        this.id = id
        this.muid = muid
        this.sender = user(meUid)
        this.conversationId = conversationId
        moderation?.let { this.moderationStatus = it }
    }

    fun conversationWith(lastMessage: BaseMessage, unread: Int = 0): Conversation =
        Conversation(conversationId, CometChatConstants.CONVERSATION_TYPE_USER).apply {
            this.conversationWith = user(peerUid)
            this.lastMessage = lastMessage
            this.unreadMessageCount = unread
        }

    /**
     * A ViewModel holding one row, paired with the SDK message listener it registered — the object
     * the SDK fans `onMessageModerated` to. `enableListeners = true` is the point of the test: the
     * defect was that this listener had no moderation override.
     */
    suspend fun viewModelShowing(
        conversations: List<Conversation>
    ): Pair<CometChatConversationsViewModel, CometChat.MessageListener> {
        whenever(getConversationListUseCase.invoke(any())).thenReturn(Result.success(conversations))
        whenever(getConversationListUseCase.hasMore()).thenReturn(false)
        val vm = CometChatConversationsViewModel(
            getConversationListUseCase,
            deleteConversationUseCase,
            refreshConversationListUseCase,
            enableListeners = true
        )
        liveViewModels += vm
        val captor = argumentCaptor<CometChat.MessageListener>()
        cometChatMock.verify({ CometChat.addMessageListener(any(), captor.capture()) }, atLeastOnce())
        return vm to captor.lastValue
    }

    /** Routes [message] → a real [Conversation] the way the SDK helper does. */
    fun mapToConversation(message: BaseMessage) {
        helperMock.`when`<Conversation> { CometChatHelper.getConversationFromMessage(message) }
            .thenReturn(
                Conversation(conversationId, CometChatConstants.CONVERSATION_TYPE_USER).apply {
                    this.conversationWith = user(peerUid)
                    this.lastMessage = message
                }
            )
    }

    /** The moderation status currently on the single row's `lastMessage`. */
    /**
     * Delivers a successful send ack exactly as dev-v6's `CometChatEvents.messageEvents` collector does:
     * a `MessageSent` with `SUCCESS` goes to `checkAndUpdateConversation(message, false)`. Called
     * directly because `CometChatEvents.emitMessageEvent` dispatches on the bus's own scope, which a
     * test dispatcher cannot order.
     */
    fun CometChatConversationsViewModel.deliverSendAck(message: BaseMessage) {
        CometChatConversationsViewModel::class.java
            .getDeclaredMethod("checkAndUpdateConversation", BaseMessage::class.java, Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }
            .invoke(this, message, false)
    }

    fun CometChatConversationsViewModel.rowModeration(): ModerationStatus? =
        (conversations.value.single().lastMessage as? TextMessage)?.moderationStatus

    beforeSpec { Dispatchers.setMain(testDispatcher) }

    afterSpec {
        Thread.sleep(50)
        Dispatchers.resetMain()
    }

    beforeTest {
        getConversationListUseCase = mock()
        deleteConversationUseCase = mock()
        refreshConversationListUseCase = mock()
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        cometChatMock.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(user(meUid))
        helperMock = Mockito.mockStatic(CometChatHelper::class.java)
        println("  🧪 ${it.name.testName}")
    }

    afterTest {
        // Tear the ViewModels down BEFORE the static mocks go away — see [liveViewModels].
        liveViewModels.forEach { vm ->
            CometChatConversationsViewModel::class.java
                .getDeclaredMethod("onCleared")
                .apply { isAccessible = true }
                .invoke(vm)
        }
        liveViewModels.clear()
        helperMock.close()
        cometChatMock.close()
        println()
    }

    test("warmup - absorb leaked exceptions") {
        try { runTest(testDispatcher) { } } catch (_: Exception) { }
    }

    // ==================== 7. The missing override ====================

    test("conversationRow_onMessageModerated_updatesLastMessage") {
        runTest(testDispatcher) {
            val optimistic = outgoing(id = 0L)
            val (vm, listener) = viewModelShowing(listOf(conversationWith(optimistic)))
            advanceUntilIdle()
            vm.rowModeration() shouldBe null

            val verdict = outgoing(id = serverId, moderation = ModerationStatus.DISAPPROVED)
            mapToConversation(verdict)
            listener.onMessageModerated(verdict)
            advanceUntilIdle()

            vm.conversations.value shouldHaveSize 1
            vm.rowModeration() shouldBe ModerationStatus.DISAPPROVED
            // The stanza is a full payload, so the server id lands on the row with it.
            vm.conversations.value.single().lastMessage!!.id shouldBe serverId
        }
    }

    // ==================== 8. The ack must not undo it ====================

    test("conversationRow_ackAfterVerdict_doesNotDowngrade") {
        runTest(testDispatcher) {
            val optimistic = outgoing(id = 0L)
            val (vm, listener) = viewModelShowing(listOf(conversationWith(optimistic)))
            advanceUntilIdle()

            val verdict = outgoing(id = serverId, moderation = ModerationStatus.DISAPPROVED)
            mapToConversation(verdict)
            listener.onMessageModerated(verdict)
            advanceUntilIdle()
            vm.rowModeration() shouldBe ModerationStatus.DISAPPROVED

            // 2–21 ms later: the send ack for the SAME message, saying "pending".
            val ack = outgoing(id = serverId, moderation = ModerationStatus.PENDING)
            mapToConversation(ack)
            vm.deliverSendAck(ack)
            advanceUntilIdle()

            vm.conversations.value shouldHaveSize 1
            vm.rowModeration() shouldBe ModerationStatus.DISAPPROVED
            vm.conversations.value.single().lastMessage!!.id shouldBe serverId
        }
    }

    // ==================== 9. The StateFlow actually emits ====================

    test("conversationRow_moderationUpdate_emitsOnStateFlow") {
        runTest(testDispatcher) {
            val optimistic = outgoing(id = 0L, moderation = ModerationStatus.PENDING)
            val seedRow = conversationWith(optimistic)
            val (vm, listener) = viewModelShowing(listOf(seedRow))
            advanceUntilIdle()

            val emitted = mutableListOf<List<Conversation>>()
            val collector = launch(testDispatcher) { vm.conversations.collect { emitted.add(it) } }
            emitted shouldHaveSize 1 // StateFlow replays the current value

            val verdict = outgoing(id = serverId, moderation = ModerationStatus.DISAPPROVED)
            mapToConversation(verdict)
            listener.onMessageModerated(verdict)
            advanceUntilIdle()

            // MutableStateFlow conflates by equals, Conversation.equals is contentEquals over
            // lastMessage, and TextMessage.contentEquals compares moderationStatus. Stamping the
            // shared lastMessage in place would leave the new list content-equal to the old one and
            // the emission would be swallowed — the ENG-39118 failure mode, one level up.
            emitted shouldHaveSize 2
            (emitted.last().single().lastMessage as TextMessage).moderationStatus shouldBe
                ModerationStatus.DISAPPROVED

            // Nothing the list already held was mutated: the stored row is a clone, and the seed
            // objects are untouched.
            emitted.last().single() shouldNotBeSameInstanceAs seedRow
            emitted.last().single().lastMessage shouldNotBeSameInstanceAs verdict
            optimistic.moderationStatus shouldBe ModerationStatus.PENDING
            collector.cancel()
        }
    }

    // ==================== The guard's scope ====================

    test("conversationRow_deleteAfterVerdict_stillClearsTheRow") {
        runTest(testDispatcher) {
            val optimistic = outgoing(id = 0L)
            val (vm, listener) = viewModelShowing(listOf(conversationWith(optimistic)))
            advanceUntilIdle()

            val verdict = outgoing(id = serverId, moderation = ModerationStatus.DISAPPROVED)
            mapToConversation(verdict)
            listener.onMessageModerated(verdict)
            advanceUntilIdle()
            vm.rowModeration() shouldBe ModerationStatus.DISAPPROVED

            // The user deletes the blocked message. The delete payload is stripped — text cleared,
            // `deletedAt` set — so it parses as UNMODERATED and matches the blocked row by id/muid.
            // Found by the /code-review self-pass: without the deletedAt exemption the no-downgrade
            // guard refuses it, and the row keeps showing the blocked message while the message list
            // shows "message deleted".
            val deleted = outgoing(id = serverId, text = "").apply {
                deletedAt = 1_729_011_400L
                deletedBy = meUid
            }
            mapToConversation(deleted)
            listener.onMessageDeleted(deleted)
            advanceUntilIdle()

            vm.conversations.value shouldHaveSize 1
            vm.conversations.value.single().lastMessage!!.deletedAt shouldBe 1_729_011_400L
        }
    }

    test("conversationRow_laterMessage_replacesDisapprovedLastMessage") {
        runTest(testDispatcher) {
            val blocked = outgoing(id = serverId, moderation = ModerationStatus.DISAPPROVED)
            val (vm, _) = viewModelShowing(listOf(conversationWith(blocked)))
            advanceUntilIdle()
            vm.rowModeration() shouldBe ModerationStatus.DISAPPROVED

            // A brand-new, perfectly fine message in the same conversation. The row must follow it —
            // the no-downgrade guard is about one message, not about the row.
            val next = outgoing(
                id = serverId + 1,
                muid = "1789401365999",
                moderation = ModerationStatus.UNMODERATED,
                text = "all good"
            )
            mapToConversation(next)
            vm.deliverSendAck(next)
            advanceUntilIdle()

            vm.conversations.value shouldHaveSize 1
            vm.rowModeration() shouldBe ModerationStatus.UNMODERATED
            (vm.conversations.value.single().lastMessage as TextMessage).text shouldBe "all good"
        }
    }
})
