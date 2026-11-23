package com.cometchat.uikit.kotlin.presentation.messagelist.adapter

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.MockedStatic
import org.mockito.Mockito

/**
 * ENG-41087, Views render key: `MessageAdapter.MessageDiffCallback.areItemsTheSame` keeps an own
 * row's identity across its ack with offline on, and is today's id rule with offline off.
 */
class MessageDiffCallbackOfflineKeyTest : FunSpec({

    lateinit var cometChatMock: MockedStatic<CometChat>

    beforeTest {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        cometChatMock.`when`<User> { CometChat.getLoggedInUser() }.thenReturn(User().apply { uid = "me" })
    }
    afterTest { cometChatMock.close() }

    fun own(id: Long, localId: String): BaseMessage =
        TextMessage("peer", "hi", CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            this.localMessageId = localId
            this.sender = User().apply { uid = "me" }
        }

    fun peer(id: Long): BaseMessage =
        TextMessage("me", "yo", CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            this.sender = User().apply { uid = "peer" }
        }

    test("offline on: an own row keeps its identity across the ack") {
        val pending = own(0L, "L1")
        val acked = own(42L, "L1")
        MessageAdapter.MessageDiffCallback(listOf(pending), listOf(acked), offlineKeyMode = true)
            .areItemsTheSame(0, 0) shouldBe true
    }

    test("offline off: today's key (message id) is used, so the ack is a different item") {
        val pending = own(0L, "L1")
        val acked = own(42L, "L1")
        MessageAdapter.MessageDiffCallback(listOf(pending), listOf(acked), offlineKeyMode = false)
            .areItemsTheSame(0, 0) shouldBe false
        MessageAdapter.MessageDiffCallback(listOf(peer(7L)), listOf(peer(7L)), offlineKeyMode = false)
            .areItemsTheSame(0, 0) shouldBe true
        MessageAdapter.MessageDiffCallback(listOf(peer(7L)), listOf(peer(8L)), offlineKeyMode = false)
            .areItemsTheSame(0, 0) shouldBe false
    }

    test("the default is offline off") {
        MessageAdapter.MessageDiffCallback(listOf(own(0L, "L1")), listOf(own(42L, "L1")))
            .areItemsTheSame(0, 0) shouldBe false
    }
})
