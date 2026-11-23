package com.cometchat.uikit.core.offline

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.InteractiveMessage
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User

/** A fake SDK for [OfflineSend]: records every call and its listener so a test drives callbacks. */
internal class FakeOfflineSdk(private val me: User? = User("me", "Me")) : OfflineSend.Sdk {
    val sends = mutableListOf<Pair<BaseMessage, CometChat.MessageSentListener>>()
    val retries = mutableListOf<Pair<String, CometChat.MessageSentListener>>()
    val discards = mutableListOf<Pair<String, CometChat.CallbackListener<String>>>()
    val directInteractive = mutableListOf<Pair<InteractiveMessage, CometChat.CallbackListener<InteractiveMessage>>>()
    private var nextLocalId = 1

    /** Like the SDK: a localMessageId is stamped synchronously when the send is accepted. */
    private fun stamp(message: BaseMessage) {
        message.localMessageId = "local-${nextLocalId++}"
    }

    override fun sendText(message: TextMessage, listener: CometChat.MessageSentListener) {
        stamp(message); sends += message to listener
    }

    override fun sendMedia(message: MediaMessage, listener: CometChat.MessageSentListener) {
        stamp(message); sends += message to listener
    }

    override fun sendCustom(message: CustomMessage, listener: CometChat.MessageSentListener) {
        stamp(message); sends += message to listener
    }

    override fun sendInteractive(message: InteractiveMessage, listener: CometChat.MessageSentListener) {
        stamp(message); sends += message to listener
    }

    override fun sendInteractiveDirect(
        message: InteractiveMessage,
        listener: CometChat.CallbackListener<InteractiveMessage>
    ) {
        directInteractive += message to listener
    }

    override fun retry(localMessageId: String, listener: CometChat.MessageSentListener) {
        retries += localMessageId to listener
    }

    override fun discard(localMessageId: String, callback: CometChat.CallbackListener<String>) {
        discards += localMessageId to callback
    }

    override fun loggedInUser(): User? = me
}

/** A captured caller callback. */
internal class RecordingCallback<T : BaseMessage> : CometChat.CallbackListener<T>() {
    val successes = mutableListOf<T>()
    val errors = mutableListOf<com.cometchat.chat.exceptions.CometChatException?>()
    override fun onSuccess(p0: T) { successes += p0 }
    override fun onError(p0: com.cometchat.chat.exceptions.CometChatException?) { errors += p0 }
}

internal fun textTo(peer: String = "peer", text: String = "hi"): TextMessage =
    TextMessage(peer, text, CometChatConstants.RECEIVER_TYPE_USER)

/** The acked copy the SDK delivers on sent: a server id, the same local id and muid. */
internal fun ackOf(pending: BaseMessage, serverId: Long): TextMessage =
    TextMessage(pending.receiverUid, (pending as? TextMessage)?.text ?: "", pending.receiverType).apply {
        id = serverId
        localMessageId = pending.localMessageId
        muid = pending.muid
        sender = pending.sender
        sentAt = 100L + serverId
        parentMessageId = pending.parentMessageId
        status = com.cometchat.chat.models.MessageStatus.SENT
    }
