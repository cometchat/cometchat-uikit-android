package com.cometchat.uikit.compose.presentation.messagelist.ui

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.mockito.MockedStatic
import org.mockito.Mockito

/**
 * ENG-41087, Compose render key: the `LazyColumn` key keeps an own row's slot across its ack with
 * offline on, and is today's key (verbatim oracle below) with offline off.
 */
class MessageListItemKeyOfflineTest : FunSpec({

    lateinit var cometChatMock: MockedStatic<CometChat>

    beforeTest {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        cometChatMock.`when`<User> { CometChat.getLoggedInUser() }.thenReturn(User().apply { uid = "me" })
    }
    afterTest { cometChatMock.close() }

    fun own(id: Long, localId: String, muid: String? = null): BaseMessage =
        TextMessage("peer", "hi", CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            this.localMessageId = localId
            if (muid != null) this.muid = muid
            this.sender = User().apply { uid = "me" }
        }

    // Today's key, verbatim from CometChatMessageList before ENG-41087.
    fun todayKey(message: BaseMessage): Any =
        if (message.id == 0L) message.muid ?: message.hashCode().toLong() else message.id

    test("offline on: an own row keeps its key across the ack") {
        messageListItemKey(own(0L, "L1"), offlineKeyMode = true) shouldBe
            messageListItemKey(own(42L, "L1"), offlineKeyMode = true)
    }

    test("offline off: today's key is used, so the ack gets a new key") {
        val pending = own(0L, "L1", muid = "m1")
        val acked = own(42L, "L1", muid = "m1")
        messageListItemKey(pending, offlineKeyMode = false) shouldBe todayKey(pending)
        messageListItemKey(acked, offlineKeyMode = false) shouldBe todayKey(acked)
        messageListItemKey(pending, offlineKeyMode = false) shouldNotBe messageListItemKey(acked, offlineKeyMode = false)
    }
})
