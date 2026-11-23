package com.cometchat.uikit.core.viewmodel.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.MessageReceipt
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.usecase.DeleteConversationUseCase
import com.cometchat.uikit.core.domain.usecase.GetConversationListUseCase
import com.cometchat.uikit.core.domain.usecase.RefreshConversationListUseCase
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
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
 * ENG-39131 (rework) — the conversation list's **group** receipt path was unreachable.
 *
 * Group receipts arrive as `DELIVERED_TO_ALL` / `READ_BY_ALL`, and the SDK dispatches those two types to
 * `onMessagesDeliveredToAll` / `onMessagesReadByAll` — never to `onMessagesDelivered` / `onMessagesRead`
 * (`CometChat.java:161-167`). [CometChatConversationsViewModel] registered only the latter two, so every
 * group receipt was discarded before any matching logic ran and a group tick moved only on a fetch or a
 * sync. Relaxing the group id match in the first pass therefore changed nothing observable.
 *
 * Every test here drives the **registered SDK listener**, captured from the mocked static
 * `CometChat.addMessageListener` — not the `@VisibleForTesting internal` handlers. That seam is exactly
 * why the gap survived review: [ConversationsReceiptMatchTest] builds its VM with `enableListeners = false`
 * and calls the handlers directly, so it passes whether or not the callbacks are wired at all.
 *
 * Covered:
 * - T1/T2 — DELIVERED_TO_ALL / READ_BY_ALL reach the handlers and stamp the matching group row.
 * - T3    — a self READ_BY_ALL for one group no longer zeroes `unreadMessageCount` on every other row,
 *           and a self plain READ still clears the badge on the row it is about.
 * - T5    — logged-in user unknown, or `lastMessage.sender == null`: both handlers no-op. This failure
 *           channel is new with the own-sent guard and nothing else pins it.
 *
 * Real SDK model instances throughout — `MockFactory.createTextMessage` returns a Mockito mock with
 * stubbed getters, so a production `deliveredAt = …` write would be swallowed and the test would pin
 * nothing while appearing to pass.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*ConversationsReceiptListenerWiringTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsReceiptListenerWiringTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()

    val meUid = "me"
    val peerUid = "peer"
    val guidA = "group-a"
    val guidB = "group-b"

    lateinit var getConversationListUseCase: GetConversationListUseCase
    lateinit var deleteConversationUseCase: DeleteConversationUseCase
    lateinit var refreshConversationListUseCase: RefreshConversationListUseCase
    lateinit var cometChatMock: MockedStatic<CometChat>

    /**
     * Every VM built here is owned by a store so it can be cleared when the test ends.
     *
     * `enableListeners = true` starts five long-lived `viewModelScope` collectors on the
     * `CometChatEvents` buses (`addLocalEventListeners`), which the rest of the suite never starts —
     * every other conversations test builds its VM with `enableListeners = false`. Leaking them makes
     * unrelated specs fail later in the same JVM: the orphaned VMs keep reacting to other tests' events,
     * by then outside this spec's static `CometChat` mock. Clearing the store runs `onCleared()`, which
     * removes the SDK listeners and cancels those jobs.
     */
    lateinit var stores: MutableList<ViewModelStore>

    fun user(uid: String): User = User(uid, "name-$uid")

    /** A real TextMessage: setters actually stick, so a stamped timestamp is observable. */
    fun message(
        id: Long,
        senderUid: String?,
        receiverId: String,
        receiverType: String = CometChatConstants.RECEIVER_TYPE_USER
    ): TextMessage {
        val message = TextMessage(receiverId, "hello", receiverType)
        message.id = id
        if (senderUid != null) message.sender = user(senderUid)
        return message
    }

    fun userConversation(lastMessage: BaseMessage?, unread: Int = 0): Conversation {
        val conversation = Conversation("conv_user-$peerUid", CometChatConstants.CONVERSATION_TYPE_USER)
        conversation.conversationWith = user(peerUid)
        conversation.lastMessage = lastMessage
        conversation.unreadMessageCount = unread
        return conversation
    }

    fun groupConversation(guid: String, lastMessage: BaseMessage?, unread: Int = 0): Conversation {
        val conversation = Conversation("conv_group-$guid", CometChatConstants.CONVERSATION_TYPE_GROUP)
        conversation.conversationWith = Group(guid, "Group $guid", CometChatConstants.GROUP_TYPE_PUBLIC, null)
        conversation.lastMessage = lastMessage
        conversation.unreadMessageCount = unread
        return conversation
    }

    fun groupReceipt(
        messageId: Long,
        senderUid: String,
        guid: String,
        receiptType: String,
        deliveredAt: Long = 0L,
        readAt: Long = 0L
    ): MessageReceipt {
        val receipt = MessageReceipt()
        receipt.messageId = messageId
        receipt.sender = user(senderUid)
        receipt.receiverId = guid
        receipt.receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
        receipt.receiptType = receiptType
        receipt.deliveredAt = deliveredAt
        receipt.readAt = readAt
        return receipt
    }

    /**
     * Builds a VM with listeners genuinely registered and hands back the SDK message listener it
     * registered — the same object the SDK calls on a receipt.
     */
    suspend fun createViewModelWithListener(
        conversations: List<Conversation>
    ): Pair<CometChatConversationsViewModel, CometChat.MessageListener> {
        whenever(getConversationListUseCase.invoke(any())).thenReturn(Result.success(conversations))
        whenever(getConversationListUseCase.hasMore()).thenReturn(false)

        val store = ViewModelStore().also { stores.add(it) }
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CometChatConversationsViewModel(
                    getConversationListUseCase,
                    deleteConversationUseCase,
                    refreshConversationListUseCase,
                    enableListeners = true
                ) as T
        }
        val viewModel = ViewModelProvider(store, factory)[CometChatConversationsViewModel::class.java]

        val listenerCaptor = argumentCaptor<CometChat.MessageListener>()
        cometChatMock.verify({ CometChat.addMessageListener(any(), listenerCaptor.capture()) }, atLeastOnce())
        return viewModel to listenerCaptor.lastValue
    }

    beforeTest {
        Dispatchers.setMain(testDispatcher)
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        stores = mutableListOf()
        getConversationListUseCase = mock()
        deleteConversationUseCase = mock()
        refreshConversationListUseCase = mock()
        whenever(CometChat.getLoggedInUser()).thenReturn(user(meUid))
        println("  🧪 ${it.name.testName}")
    }

    afterTest {
        // Clear before the static mock closes — onCleared() calls CometChat.remove*Listener. Guarded so a
        // throw here can never leak the static mock into the next spec, which is the failure this whole
        // teardown exists to prevent.
        try {
            stores.forEach { it.clear() }
        } finally {
            cometChatMock.close()
            Dispatchers.resetMain()
        }
        println()
    }

    // ---------- T1: DELIVERED_TO_ALL reaches the handler through the registered listener ----------

    test("listener_onMessagesDeliveredToAll_stampsTheMatchingGroupRow") {
        runTest {
            val own = message(
                id = 500L, senderUid = meUid, receiverId = guidA,
                receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
            )
            val (viewModel, listener) = createViewModelWithListener(listOf(groupConversation(guidA, own)))
            advanceUntilIdle()

            // This is the callback the SDK actually invokes for a group delivery receipt. Before the
            // override existed it hit CometChat.MessageListener's empty default and the receipt vanished.
            listener.onMessagesDeliveredToAll(
                groupReceipt(
                    messageId = 999L,
                    senderUid = peerUid,
                    guid = guidA,
                    receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED_TO_ALL,
                    deliveredAt = 1_700L
                )
            )
            advanceUntilIdle()

            viewModel.conversations.value.single().lastMessage!!.deliveredAt shouldBe 1_700L
            println("    ✅ DELIVERED_TO_ALL routed to updateDeliveredReceipts and stamped the group row")
        }
    }

    // ---------- T2: READ_BY_ALL, same ----------

    test("listener_onMessagesReadByAll_stampsTheMatchingGroupRow") {
        runTest {
            val own = message(
                id = 500L, senderUid = meUid, receiverId = guidA,
                receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
            )
            val (viewModel, listener) = createViewModelWithListener(listOf(groupConversation(guidA, own)))
            advanceUntilIdle()

            listener.onMessagesReadByAll(
                groupReceipt(
                    messageId = 999L,
                    senderUid = peerUid,
                    guid = guidA,
                    receiptType = MessageReceipt.RECEIPT_TYPE_READ_BY_ALL,
                    readAt = 1_800L
                )
            )
            advanceUntilIdle()

            val stamped = viewModel.conversations.value.single().lastMessage!!
            stamped.readAt shouldBe 1_800L
            stamped.deliveredAt shouldBe 1_800L   // a read implies delivered
            println("    ✅ READ_BY_ALL routed to updateReadReceipts and stamped the group row")
        }
    }

    // ---------- T3: the unread-count clear is scoped to the row the receipt is about ----------

    test("listener_selfReadByAllForOneGroup_doesNotZeroUnreadOnOtherRows") {
        runTest {
            // The group row holds an INCOMING message, so the receipt cannot stamp it and falls through
            // to the unread-count clear — the arm that used to fire on every non-matching row instead.
            val rows = listOf(
                groupConversation(
                    guidA,
                    message(
                        id = 500L, senderUid = peerUid, receiverId = guidA,
                        receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
                    ),
                    unread = 7
                ),
                groupConversation(
                    guidB,
                    message(
                        id = 400L, senderUid = peerUid, receiverId = guidB,
                        receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
                    ),
                    unread = 3
                ),
                userConversation(message(id = 300L, senderUid = peerUid, receiverId = meUid), unread = 5)
            )
            val (viewModel, listener) = createViewModelWithListener(rows)
            advanceUntilIdle()

            listener.onMessagesReadByAll(
                groupReceipt(
                    messageId = 999L,
                    senderUid = meUid,          // our own read, echoed from another device
                    guid = guidA,
                    receiptType = MessageReceipt.RECEIPT_TYPE_READ_BY_ALL,
                    readAt = 1_800L
                )
            )
            advanceUntilIdle()

            val after = viewModel.conversations.value.associateBy { it.conversationId }
            after["conv_group-$guidA"]!!.unreadMessageCount shouldBe 0   // the row the receipt is about
            after["conv_group-$guidB"]!!.unreadMessageCount shouldBe 3   // untouched
            after["conv_user-$peerUid"]!!.unreadMessageCount shouldBe 5  // untouched
            println("    ✅ self group read clears only its own row's badge; other rows keep their counts")
        }
    }

    test("listener_selfPlainReadInAGroup_stillClearsThatGroupsBadge") {
        runTest {
            // A self read echoed from another device arrives as a plain READ, not READ_BY_ALL — this is
            // the case the group unread-count clear exists for, so scoping it must not delete it.
            val rows = listOf(
                groupConversation(
                    guidA,
                    message(
                        id = 500L, senderUid = peerUid, receiverId = guidA,
                        receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
                    ),
                    unread = 7
                ),
                userConversation(message(id = 300L, senderUid = peerUid, receiverId = meUid), unread = 5)
            )
            val (viewModel, listener) = createViewModelWithListener(rows)
            advanceUntilIdle()

            listener.onMessagesRead(
                groupReceipt(
                    messageId = 999L,
                    senderUid = meUid,
                    guid = guidA,
                    receiptType = MessageReceipt.RECEIPT_TYPE_READ,
                    readAt = 1_800L
                )
            )
            advanceUntilIdle()

            val after = viewModel.conversations.value.associateBy { it.conversationId }
            after["conv_group-$guidA"]!!.unreadMessageCount shouldBe 0
            after["conv_user-$peerUid"]!!.unreadMessageCount shouldBe 5
            println("    ✅ plain self READ in a group still clears that group's badge, and only it")
        }
    }

    // ---------- T5: the own-sent guard's two unknown-identity channels ----------

    test("listener_loggedInUserUnknown_bothGroupHandlersNoOp") {
        runTest {
            // SDK not initialised / no session: getLoggedInUser() throws or returns null, so isOwnMessage
            // cannot be established and neither handler may stamp on a guess.
            whenever(CometChat.getLoggedInUser()).thenReturn(null)

            val own = message(
                id = 500L, senderUid = meUid, receiverId = guidA,
                receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
            )
            val row = groupConversation(guidA, own)
            val (viewModel, listener) = createViewModelWithListener(listOf(row))
            advanceUntilIdle()

            listener.onMessagesDeliveredToAll(
                groupReceipt(
                    messageId = 999L, senderUid = peerUid, guid = guidA,
                    receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED_TO_ALL, deliveredAt = 1_700L
                )
            )
            listener.onMessagesReadByAll(
                groupReceipt(
                    messageId = 999L, senderUid = peerUid, guid = guidA,
                    receiptType = MessageReceipt.RECEIPT_TYPE_READ_BY_ALL, readAt = 1_800L
                )
            )
            advanceUntilIdle()

            val after = viewModel.conversations.value.single()
            after.lastMessage!!.deliveredAt shouldBe 0L
            after.lastMessage!!.readAt shouldBe 0L
            after shouldBeSameInstanceAs row   // no clone, so no emission either
            println("    ✅ unknown logged-in user: both group handlers are no-ops")
        }
    }

    test("listener_lastMessageHasNoSender_bothGroupHandlersNoOp") {
        runTest {
            // A row whose lastMessage carries no sender cannot be shown to be ours, so the own-sent guard
            // must fail closed rather than stamp it.
            val senderless = message(
                id = 500L, senderUid = null, receiverId = guidA,
                receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
            )
            val row = groupConversation(guidA, senderless)
            val (viewModel, listener) = createViewModelWithListener(listOf(row))
            advanceUntilIdle()

            listener.onMessagesDeliveredToAll(
                groupReceipt(
                    messageId = 999L, senderUid = peerUid, guid = guidA,
                    receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED_TO_ALL, deliveredAt = 1_700L
                )
            )
            listener.onMessagesReadByAll(
                groupReceipt(
                    messageId = 999L, senderUid = peerUid, guid = guidA,
                    receiptType = MessageReceipt.RECEIPT_TYPE_READ_BY_ALL, readAt = 1_800L
                )
            )
            advanceUntilIdle()

            val after = viewModel.conversations.value.single()
            after.lastMessage!!.deliveredAt shouldBe 0L
            after.lastMessage!!.readAt shouldBe 0L
            after shouldBeSameInstanceAs row
            println("    ✅ senderless lastMessage: both group handlers are no-ops")
        }
    }
})
