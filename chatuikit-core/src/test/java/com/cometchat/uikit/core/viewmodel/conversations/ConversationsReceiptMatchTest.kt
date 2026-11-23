package com.cometchat.uikit.core.viewmodel.conversations

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.MessageReceipt
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.domain.usecase.DeleteConversationUseCase
import com.cometchat.uikit.core.domain.usecase.GetConversationListUseCase
import com.cometchat.uikit.core.domain.usecase.RefreshConversationListUseCase
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * ENG-39131 — the conversation list dropped live delivery/read receipts on a strict id match.
 *
 * `updateDeliveredReceipts` / `updateReadReceipts` required `lastMessage.id == receipt.messageId`, but a
 * receipt anchors at the newest id the peer has seen — not necessarily the id sitting in the row. Any row
 * behind the anchor had its receipt discarded, and nothing re-evaluates a dropped receipt afterwards.
 * Both paths now match cumulatively (`id <= receipt.messageId`) behind an own-sent guard, mirroring
 * `CometChatMessageListViewModel.setDeliveryReceipts` and `MessageDao.applyReceipt` (ENG-36822).
 *
 * ⚠️ The pre-ack (`id = 0`) tests below pin the matching rule, **not** the rendered tick. A stamped
 * pre-ack row still draws a clock — `MessageReceiptUtils.kt:66-68` maps `id == 0L` to IN_PROGRESS before
 * it reads `readAt` / `deliveredAt`, on both UI stacks — and the stamp is discarded on ack anyway, since
 * `updateConversation` replaces `lastMessage` wholesale. What repairs the tick is the cumulative match on
 * **real** ids plus stamping the clone (which defeats `MutableStateFlow` conflation); see the KDoc on
 * `updateDeliveredReceipts`.
 *
 * The group path was strict in the first pass on this ticket, because §5 invariant 1 said not to loosen it
 * while §4 described the relaxation for USER and GROUP alike. Aditya ruled for one uniform rule: the SDK
 * already treats groups identically — `ReceiptScope.resolve` maps DELIVERED_TO_ALL/READ_BY_ALL onto the
 * same `deliveredAt`/`readAt` columns as 1:1 and files them against the same cumulative anchor — so a
 * strict group path here would make group ticks correct after a restart and wrong live.
 *
 * ⚠️ These tests deliberately build **real** SDK model instances rather than using
 * [com.cometchat.uikit.core.testutils.MockFactory], whose `createTextMessage` returns a Mockito mock with
 * stubbed getters: a production `deliveredAt = …` write would be swallowed and the getter would keep
 * returning the seed, so a mock-based test would pin nothing while appearing to pass.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*ConversationsReceiptMatchTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationsReceiptMatchTest : FunSpec({

    isolationMode = IsolationMode.SingleInstance

    val testDispatcher = UnconfinedTestDispatcher()

    val meUid = "me"
    val peerUid = "peer"
    val guid = "group-1"

    lateinit var getConversationListUseCase: GetConversationListUseCase
    lateinit var deleteConversationUseCase: DeleteConversationUseCase
    lateinit var refreshConversationListUseCase: RefreshConversationListUseCase

    // ---------- real model builders (no mocks) ----------

    fun user(uid: String): User = User(uid, "name-$uid")

    /** A real TextMessage: setters actually stick, so a stamped timestamp is observable. */
    fun message(
        id: Long,
        senderUid: String,
        receiverId: String,
        receiverType: String = CometChatConstants.RECEIVER_TYPE_USER,
        deliveredAt: Long = 0L,
        readAt: Long = 0L
    ): TextMessage {
        val message = TextMessage(receiverId, "hello", receiverType)
        message.id = id
        message.sender = user(senderUid)
        message.deliveredAt = deliveredAt
        message.readAt = readAt
        return message
    }

    fun userConversation(lastMessage: BaseMessage?, unread: Int = 0): Conversation {
        val conversation = Conversation("conv_user-$peerUid", CometChatConstants.CONVERSATION_TYPE_USER)
        conversation.conversationWith = user(peerUid)
        conversation.lastMessage = lastMessage
        conversation.unreadMessageCount = unread
        return conversation
    }

    fun groupConversation(lastMessage: BaseMessage?): Conversation {
        val conversation = Conversation("conv_group-$guid", CometChatConstants.CONVERSATION_TYPE_GROUP)
        conversation.conversationWith = Group(guid, "The Group", CometChatConstants.GROUP_TYPE_PUBLIC, null)
        conversation.lastMessage = lastMessage
        return conversation
    }

    fun receipt(
        messageId: Long,
        senderUid: String,
        receiverId: String,
        receiverType: String,
        receiptType: String,
        deliveredAt: Long = 0L,
        readAt: Long = 0L
    ): MessageReceipt {
        val receipt = MessageReceipt()
        receipt.messageId = messageId
        receipt.sender = user(senderUid)
        receipt.receiverId = receiverId
        receipt.receiverType = receiverType
        receipt.receiptType = receiptType
        receipt.deliveredAt = deliveredAt
        receipt.readAt = readAt
        return receipt
    }

    suspend fun createViewModel(conversations: List<Conversation>): CometChatConversationsViewModel {
        whenever(getConversationListUseCase.invoke(any())).thenReturn(Result.success(conversations))
        whenever(getConversationListUseCase.hasMore()).thenReturn(false)
        return CometChatConversationsViewModel(
            getConversationListUseCase,
            deleteConversationUseCase,
            refreshConversationListUseCase,
            enableListeners = false
        )
    }

    beforeTest {
        Dispatchers.setMain(testDispatcher)
        getConversationListUseCase = mock()
        deleteConversationUseCase = mock()
        refreshConversationListUseCase = mock()
        println("  🧪 ${it.name.testName}")
    }

    afterTest {
        Dispatchers.resetMain()
        println()
    }

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    // ---------- 1. the actual defect ----------

    test("deliveryReceipt_whenRowStillHasPreAckId_stillMarksDelivered") {
        runTest {
            // The row holds the pre-ack copy of our own offline send: id is still 0.
            val preAck = message(id = 0L, senderUid = meUid, receiverId = peerUid)
            val row = userConversation(preAck)
            val viewModel = createViewModel(listOf(row))
            advanceUntilIdle()

            Mockito.mockStatic(CometChat::class.java).use {
                whenever(CometChat.getLoggedInUser()).thenReturn(user(meUid))

                // The receipt carries the server id the row does not have yet.
                viewModel.updateDeliveredReceipts(
                    receipt(
                        messageId = 555L,
                        senderUid = peerUid,
                        receiverId = meUid,
                        receiverType = UIKitConstants.ReceiverType.USER,
                        receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED,
                        deliveredAt = 1_700L
                    )
                )
                advanceUntilIdle()

                val updated = viewModel.conversations.value.single()
                updated.lastMessage!!.deliveredAt shouldBe 1_700L
                // A fresh row reference AND new content, so the StateFlow actually emits: MutableStateFlow
                // conflates by equals and Conversation.equals is contentEquals, so stamping the shared
                // message in place would have been swallowed (the ENG-39118 failure mode).
                updated shouldNotBeSameInstanceAs row
                preAck.deliveredAt shouldBe 0L
                println("    ✅ pre-ack row (id=0) stamped delivered by a receipt carrying the server id")
            }
        }
    }

    // ---------- 2. same, for read ----------

    test("readReceipt_whenRowStillHasPreAckId_stillMarksRead") {
        runTest {
            val preAck = message(id = 0L, senderUid = meUid, receiverId = peerUid)
            val row = userConversation(preAck)
            val viewModel = createViewModel(listOf(row))
            advanceUntilIdle()

            Mockito.mockStatic(CometChat::class.java).use {
                whenever(CometChat.getLoggedInUser()).thenReturn(user(meUid))

                viewModel.updateReadReceipts(
                    receipt(
                        messageId = 555L,
                        senderUid = peerUid,
                        receiverId = meUid,
                        receiverType = UIKitConstants.ReceiverType.USER,
                        receiptType = MessageReceipt.RECEIPT_TYPE_READ,
                        readAt = 1_800L
                    )
                )
                advanceUntilIdle()

                val updated = viewModel.conversations.value.single()
                updated.lastMessage!!.readAt shouldBe 1_800L
                // New reference and new content — see the delivered case above.
                updated shouldNotBeSameInstanceAs row
                preAck.readAt shouldBe 0L
                println("    ✅ pre-ack row (id=0) stamped read by a receipt carrying the server id")
            }
        }
    }

    // ---------- 3. invariant 2: an incoming lastMessage is never stamped ----------

    test("receipt_incomingLastMessage_isNeverStamped") {
        runTest {
            // The peer's own message is the row's lastMessage, and its id even equals the receipt anchor —
            // the strict id match would have stamped it. Our delivery/read state must not be written onto
            // a message we did not send.
            val incoming = message(id = 555L, senderUid = peerUid, receiverId = meUid)
            val row = userConversation(incoming)
            val viewModel = createViewModel(listOf(row))
            advanceUntilIdle()

            Mockito.mockStatic(CometChat::class.java).use {
                whenever(CometChat.getLoggedInUser()).thenReturn(user(meUid))

                viewModel.updateDeliveredReceipts(
                    receipt(
                        messageId = 555L,
                        senderUid = peerUid,
                        receiverId = meUid,
                        receiverType = UIKitConstants.ReceiverType.USER,
                        receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED,
                        deliveredAt = 1_700L
                    )
                )
                advanceUntilIdle()
                viewModel.updateReadReceipts(
                    receipt(
                        messageId = 555L,
                        senderUid = peerUid,
                        receiverId = meUid,
                        receiverType = UIKitConstants.ReceiverType.USER,
                        receiptType = MessageReceipt.RECEIPT_TYPE_READ,
                        readAt = 1_800L
                    )
                )
                advanceUntilIdle()

                val after = viewModel.conversations.value.single()
                after.lastMessage!!.deliveredAt shouldBe 0L
                after.lastMessage!!.readAt shouldBe 0L

                // Same invariant on the GROUP path. Worth pinning here because relaxing the group id
                // match to `<=` makes the own-sent guard the only thing standing between a group
                // DELIVERED_TO_ALL / READ_BY_ALL and someone else's message sitting in the row.
                val groupIncoming = message(
                    id = 500L, senderUid = peerUid, receiverId = guid,
                    receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
                )
                val groupRow = groupConversation(groupIncoming)
                val groupViewModel = createViewModel(listOf(groupRow))
                advanceUntilIdle()

                groupViewModel.updateDeliveredReceipts(
                    receipt(
                        messageId = 999L,
                        senderUid = peerUid,
                        receiverId = guid,
                        receiverType = UIKitConstants.ReceiverType.GROUP,
                        receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED_TO_ALL,
                        deliveredAt = 1_700L
                    )
                )
                advanceUntilIdle()
                groupViewModel.updateReadReceipts(
                    receipt(
                        messageId = 999L,
                        senderUid = peerUid,
                        receiverId = guid,
                        receiverType = UIKitConstants.ReceiverType.GROUP,
                        receiptType = MessageReceipt.RECEIPT_TYPE_READ_BY_ALL,
                        readAt = 1_800L
                    )
                )
                advanceUntilIdle()

                val groupAfter = groupViewModel.conversations.value.single()
                groupAfter.lastMessage!!.deliveredAt shouldBe 0L
                groupAfter.lastMessage!!.readAt shouldBe 0L
                println("    ✅ incoming lastMessage left untouched by both handlers, 1:1 and group")
            }
        }
    }

    // ---------- 4. invariant 3: never move a timestamp backwards ----------

    test("receipt_alreadyDelivered_isNoOp") {
        runTest {
            val delivered = message(id = 555L, senderUid = meUid, receiverId = peerUid, deliveredAt = 1_000L)
            val row = userConversation(delivered)
            val viewModel = createViewModel(listOf(row))
            advanceUntilIdle()

            Mockito.mockStatic(CometChat::class.java).use {
                whenever(CometChat.getLoggedInUser()).thenReturn(user(meUid))

                viewModel.updateDeliveredReceipts(
                    receipt(
                        messageId = 555L,
                        senderUid = peerUid,
                        receiverId = meUid,
                        receiverType = UIKitConstants.ReceiverType.USER,
                        receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED,
                        deliveredAt = 2_000L
                    )
                )
                advanceUntilIdle()

                val after = viewModel.conversations.value.single()
                after.lastMessage!!.deliveredAt shouldBe 1_000L   // untouched, not advanced
                after shouldBeSameInstanceAs row                  // no clone, no emission
                println("    ✅ already-delivered row is a no-op (early skip survives)")
            }
        }
    }

    // ---------- 5. group: the receipt-type and guid gates are unchanged; only the id match relaxed ----------

    test("groupReceipt_typeGateUnchanged_andMatchesCumulatively") {
        runTest {
            // (a) DELIVERED_TO_ALL with the strict id match still stamps — unchanged.
            val acked = message(
                id = 777L, senderUid = meUid, receiverId = guid,
                receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
            )
            val groupRow = groupConversation(acked)
            val viewModel = createViewModel(listOf(groupRow))
            advanceUntilIdle()

            Mockito.mockStatic(CometChat::class.java).use {
                whenever(CometChat.getLoggedInUser()).thenReturn(user(meUid))

                viewModel.updateDeliveredReceipts(
                    receipt(
                        messageId = 777L,
                        senderUid = peerUid,
                        receiverId = guid,
                        receiverType = UIKitConstants.ReceiverType.GROUP,
                        receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED_TO_ALL,
                        deliveredAt = 1_700L
                    )
                )
                advanceUntilIdle()
                viewModel.conversations.value.single().lastMessage!!.deliveredAt shouldBe 1_700L

                // (b) a plain DELIVERED receipt in a group still does nothing — the receipt-type gate holds.
                val plainRow = groupConversation(
                    message(
                        id = 888L, senderUid = meUid, receiverId = guid,
                        receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
                    )
                )
                val plainViewModel = createViewModel(listOf(plainRow))
                advanceUntilIdle()
                plainViewModel.updateDeliveredReceipts(
                    receipt(
                        messageId = 888L,
                        senderUid = peerUid,
                        receiverId = guid,
                        receiverType = UIKitConstants.ReceiverType.GROUP,
                        receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED,
                        deliveredAt = 1_700L
                    )
                )
                advanceUntilIdle()
                plainViewModel.conversations.value.single().lastMessage!!.deliveredAt shouldBe 0L

                // (c) FLIPPED by Aditya's uniform-rule ruling. This case used to assert that the group
                // path stayed strict; it is now cumulative like 1:1, so an own-sent row whose id is BELOW
                // the receipt anchor is stamped. The pre-ack (id = 0) shape of the same rule has its own
                // tests below; this one pins `<=` on a real server id, which `==` would also have dropped.
                val olderRow = groupConversation(
                    message(
                        id = 500L, senderUid = meUid, receiverId = guid,
                        receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
                    )
                )
                val olderViewModel = createViewModel(listOf(olderRow))
                advanceUntilIdle()
                olderViewModel.updateDeliveredReceipts(
                    receipt(
                        messageId = 999L,
                        senderUid = peerUid,
                        receiverId = guid,
                        receiverType = UIKitConstants.ReceiverType.GROUP,
                        receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED_TO_ALL,
                        deliveredAt = 1_700L
                    )
                )
                advanceUntilIdle()
                olderViewModel.conversations.value.single().lastMessage!!.deliveredAt shouldBe 1_700L
                println("    ✅ group: type gate holds (plain DELIVERED ignored); id matches cumulatively")
            }
        }
    }

    // ---------- 6. invariant 4: a read receipt implies delivered ----------

    test("readReceipt_alsoAdvancesDeliveredAt") {
        runTest {
            // Delivery receipt never seen (deliveredAt still 0) — the read receipt must advance both.
            val own = message(id = 0L, senderUid = meUid, receiverId = peerUid)
            val row = userConversation(own)
            val viewModel = createViewModel(listOf(row))
            advanceUntilIdle()

            Mockito.mockStatic(CometChat::class.java).use {
                whenever(CometChat.getLoggedInUser()).thenReturn(user(meUid))

                viewModel.updateReadReceipts(
                    receipt(
                        messageId = 555L,
                        senderUid = peerUid,
                        receiverId = meUid,
                        receiverType = UIKitConstants.ReceiverType.USER,
                        receiptType = MessageReceipt.RECEIPT_TYPE_READ,
                        readAt = 1_800L
                    )
                )
                advanceUntilIdle()

                val after = viewModel.conversations.value.single().lastMessage!!
                after.readAt shouldBe 1_800L
                after.deliveredAt shouldBe 1_800L   // read implies delivered, matching the SDK
                println("    ✅ read receipt advances readAt and an unset deliveredAt")
            }
        }
    }

    // ---------- 7. the actual defect, in a group ----------

    test("groupDeliveryReceipt_whenRowStillHasPreAckId_stillMarksDelivered") {
        runTest {
            // Same shape as test 1, in a group: the row holds the pre-ack copy of our own offline send
            // (id = 0) when DELIVERED_TO_ALL lands carrying the server id.
            val preAck = message(
                id = 0L, senderUid = meUid, receiverId = guid,
                receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
            )
            val row = groupConversation(preAck)
            val viewModel = createViewModel(listOf(row))
            advanceUntilIdle()

            Mockito.mockStatic(CometChat::class.java).use {
                whenever(CometChat.getLoggedInUser()).thenReturn(user(meUid))

                viewModel.updateDeliveredReceipts(
                    receipt(
                        messageId = 999L,
                        senderUid = peerUid,
                        receiverId = guid,
                        receiverType = UIKitConstants.ReceiverType.GROUP,
                        receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED_TO_ALL,
                        deliveredAt = 1_700L
                    )
                )
                advanceUntilIdle()

                val updated = viewModel.conversations.value.single()
                updated.lastMessage!!.deliveredAt shouldBe 1_700L
                // New reference and new content, so the StateFlow actually emits — see test 1.
                updated shouldNotBeSameInstanceAs row
                preAck.deliveredAt shouldBe 0L
                println("    ✅ group pre-ack row (id=0) stamped delivered by DELIVERED_TO_ALL")
            }
        }
    }

    // ---------- 8. same, for group read ----------

    test("groupReadReceipt_whenRowStillHasPreAckId_stillMarksRead") {
        runTest {
            val preAck = message(
                id = 0L, senderUid = meUid, receiverId = guid,
                receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
            )
            val row = groupConversation(preAck)
            val viewModel = createViewModel(listOf(row))
            advanceUntilIdle()

            Mockito.mockStatic(CometChat::class.java).use {
                whenever(CometChat.getLoggedInUser()).thenReturn(user(meUid))

                viewModel.updateReadReceipts(
                    receipt(
                        messageId = 999L,
                        senderUid = peerUid,
                        receiverId = guid,
                        receiverType = UIKitConstants.ReceiverType.GROUP,
                        receiptType = MessageReceipt.RECEIPT_TYPE_READ_BY_ALL,
                        readAt = 1_800L
                    )
                )
                advanceUntilIdle()

                val updated = viewModel.conversations.value.single()
                updated.lastMessage!!.readAt shouldBe 1_800L
                // Read implies delivered on the group path too, as in test 6.
                updated.lastMessage!!.deliveredAt shouldBe 1_800L
                updated shouldNotBeSameInstanceAs row
                preAck.readAt shouldBe 0L
                println("    ✅ group pre-ack row (id=0) stamped read by READ_BY_ALL")
            }
        }
    }

    // ---------- 9-12. the address gates and the no-op rows ----------
    //
    // These four replace the only production claims that the deleted mirror property tests
    // (ConversationReceiptPropertyTest / ConversationGroupReceiptPropertyTest /
    // ConversationSelfReceiptPropertyTest) made and nothing else here covered. Those files re-implemented
    // the matching rule in local `data class`es and never called the ViewModel, so they stayed green
    // through a full revert of this fix — and their `TestMessage` has no `sender`, so they could not
    // express the own-sent guard at all.

    test("receipt_addressGates_holdOnBothReceiverTypes") {
        runTest {
            Mockito.mockStatic(CometChat::class.java).use {
                whenever(CometChat.getLoggedInUser()).thenReturn(user(meUid))

                // (a) a 1:1 receipt from someone who is not this row's peer never matches it.
                val strangerVm = createViewModel(
                    listOf(userConversation(message(id = 100L, senderUid = meUid, receiverId = peerUid)))
                )
                advanceUntilIdle()
                strangerVm.updateDeliveredReceipts(
                    receipt(
                        messageId = 999L,
                        senderUid = "stranger",
                        receiverId = meUid,
                        receiverType = UIKitConstants.ReceiverType.USER,
                        receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED,
                        deliveredAt = 1_700L
                    )
                )
                advanceUntilIdle()
                strangerVm.conversations.value.single().lastMessage!!.deliveredAt shouldBe 0L

                // (b) a group receipt for a different guid never matches this group's row.
                val otherGuidVm = createViewModel(
                    listOf(
                        groupConversation(
                            message(
                                id = 100L, senderUid = meUid, receiverId = guid,
                                receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
                            )
                        )
                    )
                )
                advanceUntilIdle()
                otherGuidVm.updateDeliveredReceipts(
                    receipt(
                        messageId = 999L,
                        senderUid = peerUid,
                        receiverId = "some-other-group",
                        receiverType = UIKitConstants.ReceiverType.GROUP,
                        receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED_TO_ALL,
                        deliveredAt = 1_700L
                    )
                )
                advanceUntilIdle()
                otherGuidVm.conversations.value.single().lastMessage!!.deliveredAt shouldBe 0L

                // (c) a group receipt never reaches a 1:1 row, whatever its anchor.
                val userRowVm = createViewModel(
                    listOf(userConversation(message(id = 100L, senderUid = meUid, receiverId = peerUid)))
                )
                advanceUntilIdle()
                userRowVm.updateDeliveredReceipts(
                    receipt(
                        messageId = 999L,
                        senderUid = peerUid,
                        receiverId = guid,
                        receiverType = UIKitConstants.ReceiverType.GROUP,
                        receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED_TO_ALL,
                        deliveredAt = 1_700L
                    )
                )
                advanceUntilIdle()
                userRowVm.conversations.value.single().lastMessage!!.deliveredAt shouldBe 0L
                println("    ✅ address gates hold: wrong peer, wrong guid, and group-receipt-vs-1:1 row")
            }
        }
    }

    test("readReceipt_addressGate_holdsForANonPeerSender") {
        runTest {
            // The read leg of the gate above, which only ever drives `updateDeliveredReceipts`. The USER
            // branch of `updateReadReceipts` carries its own peer match (`receipt.sender?.uid ==
            // conversationWith.uid`) and nothing else pins it: every other caller here either has a single
            // row whose peer already matches, or sends a GROUP receipt that never enters that branch. Lose that comparison and a read receipt from user X stamps `readAt` — and, through
            // `stampRead`, `deliveredAt` with it — on the row for user Y: green double ticks on the wrong
            // conversation. It is also the one claim the deleted `ConversationReceiptPropertyTest` made
            // ("Read receipt does not update non-matching user") that was not rebuilt.
            val row = userConversation(message(id = 100L, senderUid = meUid, receiverId = peerUid))
            val viewModel = createViewModel(listOf(row))
            advanceUntilIdle()

            Mockito.mockStatic(CometChat::class.java).use {
                whenever(CometChat.getLoggedInUser()).thenReturn(user(meUid))

                viewModel.updateReadReceipts(
                    receipt(
                        messageId = 999L,       // above the row's id, so only the address gate can stop it
                        senderUid = "stranger", // not this row's peer, and not us
                        receiverId = meUid,
                        receiverType = UIKitConstants.ReceiverType.USER,
                        receiptType = MessageReceipt.RECEIPT_TYPE_READ,
                        readAt = 1_800L
                    )
                )
                advanceUntilIdle()

                val after = viewModel.conversations.value.single()
                after.lastMessage!!.readAt shouldBe 0L
                after.lastMessage!!.deliveredAt shouldBe 0L   // stampRead advances both; neither may move
                after shouldBeSameInstanceAs row              // not even cloned, so nothing is emitted
                println("    ✅ read address gate holds: a stranger's read receipt never stamps the peer row")
            }
        }
    }

    test("receipt_rowWithoutLastMessage_isNoOp") {
        runTest {
            val row = userConversation(null, unread = 4)
            val viewModel = createViewModel(listOf(row))
            advanceUntilIdle()

            Mockito.mockStatic(CometChat::class.java).use {
                whenever(CometChat.getLoggedInUser()).thenReturn(user(meUid))

                viewModel.updateDeliveredReceipts(
                    receipt(
                        messageId = 999L,
                        senderUid = peerUid,
                        receiverId = meUid,
                        receiverType = UIKitConstants.ReceiverType.USER,
                        receiptType = MessageReceipt.RECEIPT_TYPE_DELIVERED,
                        deliveredAt = 1_700L
                    )
                )
                viewModel.updateReadReceipts(
                    receipt(
                        messageId = 999L,
                        senderUid = peerUid,
                        receiverId = meUid,
                        receiverType = UIKitConstants.ReceiverType.USER,
                        receiptType = MessageReceipt.RECEIPT_TYPE_READ,
                        readAt = 1_800L
                    )
                )
                advanceUntilIdle()

                val after = viewModel.conversations.value.single()
                after shouldBeSameInstanceAs row       // no clone, no emission
                after.unreadMessageCount shouldBe 4
                println("    ✅ a row with no lastMessage is left alone by both handlers")
            }
        }
    }

    test("readReceipt_alreadyRead_isNoOp") {
        runTest {
            val read = message(id = 555L, senderUid = meUid, receiverId = peerUid, readAt = 1_000L)
            val row = userConversation(read)
            val viewModel = createViewModel(listOf(row))
            advanceUntilIdle()

            Mockito.mockStatic(CometChat::class.java).use {
                whenever(CometChat.getLoggedInUser()).thenReturn(user(meUid))

                viewModel.updateReadReceipts(
                    receipt(
                        messageId = 555L,
                        senderUid = peerUid,
                        receiverId = meUid,
                        receiverType = UIKitConstants.ReceiverType.USER,
                        receiptType = MessageReceipt.RECEIPT_TYPE_READ,
                        readAt = 2_000L
                    )
                )
                advanceUntilIdle()

                val after = viewModel.conversations.value.single()
                after.lastMessage!!.readAt shouldBe 1_000L   // untouched, never moved forward again
                after shouldBeSameInstanceAs row
                println("    ✅ already-read row is a no-op (readAt never re-stamped)")
            }
        }
    }
})
