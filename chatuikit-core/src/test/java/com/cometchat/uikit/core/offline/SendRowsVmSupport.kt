package com.cometchat.uikit.core.offline

import android.util.Log
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.MessageReceipt
import com.cometchat.chat.models.MessageStatus
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/** Builds an offline-ON message-list ViewModel bound to a 1:1 chat with "peer", logged in as "me". */
internal class SendRowsHarness {
    val me = User("me", "Me")
    val peer = User("peer", "Peer")
    private val store = androidx.lifecycle.ViewModelStore()
    lateinit var cometChat: MockedStatic<CometChat>
    lateinit var log: MockedStatic<Log>
    lateinit var sdk: FakeOfflineSdk
    lateinit var repository: MessageListRepository

    fun open(
        offlineOn: Boolean = true,
        seeded: List<BaseMessage> = emptyList(),
        parentMessageId: Long = -1L,
        configureRepo: (MessageListRepository) -> Unit = {}
    ): CometChatMessageListViewModel {
        OfflineGate.testOverride = offlineOn
        OfflineHub.resetForTest()
        OfflineSend.resetForTest()
        sdk = FakeOfflineSdk(me)
        OfflineSend.sdk = sdk
        log = Mockito.mockStatic(Log::class.java)
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User> { CometChat.getLoggedInUser() }.thenReturn(me)
        repository = mock {
            on { hasMorePreviousMessages() } doReturn true
            onBlocking { fetchPreviousMessages() } doReturn Result.success(seeded)
            onBlocking { markAsRead(any()) } doReturn Result.success(Unit)
            onBlocking { markAsDelivered(any()) } doReturn Result.success(Unit)
        }
        configureRepo(repository)
        val vm = androidx.lifecycle.ViewModelProvider(
            store,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    CometChatMessageListViewModel(repository = repository, enableListeners = true) as T
            }
        )[CometChatMessageListViewModel::class.java]
        vm.setUser(peer, parentMessageId = parentMessageId)
        if (seeded.isNotEmpty()) vm.fetchMessages()
        return vm
    }

    fun close() {
        store.clear()
        if (::cometChat.isInitialized) cometChat.close()
        if (::log.isInitialized) log.close()
        OfflineGate.testOverride = null
        OfflineSend.resetForTest()
    }

    /** An own message to "peer" with no server id, as the composer builds it. */
    fun ownText(text: String = "hi", parentId: Long = 0L): TextMessage =
        TextMessage("peer", text, CometChatConstants.RECEIVER_TYPE_USER).apply {
            sender = me
            muid = "muid-$text-${System.nanoTime()}"
            parentMessageId = parentId
        }

    /** A message that already has a server id. */
    fun served(id: Long, from: User = me, text: String = "s$id"): TextMessage =
        TextMessage("peer", text, CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            sender = from
            sentAt = 1000L + id
            status = MessageStatus.SENT
        }

    /** Sends [message] through the helper and fires pending; returns the SDK listener. */
    fun sendPending(message: BaseMessage): CometChat.MessageSentListener {
        OfflineSend.interceptTextMessage(message as TextMessage, null)
        val listener = sdk.sends.last().second
        message.status = MessageStatus.PENDING
        listener.onMessagePending(message)
        return listener
    }

    fun receipt(messageId: Long, delivered: Boolean, at: Long = 5000L): MessageReceipt =
        MessageReceipt().apply {
            this.messageId = messageId
            receiptType = if (delivered) MessageReceipt.RECEIPT_TYPE_DELIVERED else MessageReceipt.RECEIPT_TYPE_READ
            receiverType = CometChatConstants.RECEIVER_TYPE_USER
            sender = peer
            if (delivered) deliveredAt = at else readAt = at
        }
}
