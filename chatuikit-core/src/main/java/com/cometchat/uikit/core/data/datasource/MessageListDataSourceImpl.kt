package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.FlagDetail
import com.cometchat.uikit.core.offline.ConversationDetailsOnce
import com.cometchat.uikit.core.offline.ConversationDetailsSourceOnce
import com.cometchat.uikit.core.offline.MessageDetailsOnce
import com.cometchat.uikit.core.offline.SearchOffline
import com.cometchat.uikit.core.offline.SettledRead
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementation of MessageListDataSource that communicates with CometChat SDK.
 * Contains NO business logic - just raw data fetching/saving.
 * This is the default implementation used for remote data operations.
 *
 * Also implements [MessageListTwoPhaseDataSource] (G2): the two-phase overloads wrap the SDK's
 * [CometChat.MessageFetchListener] (cached → server) as a [Flow] of [TwoPhaseResult]. The single-phase
 * methods use the same 5.1.0 overloads and take the server phase only, so the offline-off path
 * behaves exactly as with the deprecated overloads.
 */
internal class MessageListDataSourceImpl : MessageListDataSource, MessageListTwoPhaseDataSource {

    override fun fetchPreviousTwoPhase(
        request: MessagesRequest
    ): Flow<TwoPhaseResult<BaseMessage>> = callbackFlow {
        request.fetchPrevious(object : CometChat.MessageFetchListener() {
            override fun onCachedMessages(messages: List<BaseMessage>) {
                trySend(TwoPhaseResult.Cached(messages))
            }

            override fun onServerMessages(messages: List<BaseMessage>) {
                trySend(TwoPhaseResult.Server(messages))
                close()
            }

            override fun onError(exception: CometChatException) {
                trySend(TwoPhaseResult.Error(exception))
                close()
            }
        })
        awaitClose { }
    }

    override fun fetchNextTwoPhase(
        request: MessagesRequest
    ): Flow<TwoPhaseResult<BaseMessage>> = callbackFlow {
        request.fetchNext(object : CometChat.MessageFetchListener() {
            override fun onCachedMessages(messages: List<BaseMessage>) {
                trySend(TwoPhaseResult.Cached(messages))
            }

            override fun onServerMessages(messages: List<BaseMessage>) {
                trySend(TwoPhaseResult.Server(messages))
                close()
            }

            override fun onError(exception: CometChatException) {
                trySend(TwoPhaseResult.Error(exception))
                close()
            }
        })
        awaitClose { }
    }

    override suspend fun fetchPreviousMessages(
        request: MessagesRequest
    ): List<BaseMessage> = suspendCancellableCoroutine { continuation ->
        // 5.1.0 two-phase overload, server phase only (§3a): the cached phase is ignored, so the
        // request, result and events equal the deprecated CallbackListener overload's.
        request.fetchPrevious(object : CometChat.MessageFetchListener() {
            override fun onServerMessages(messages: List<BaseMessage>) {
                continuation.resume(messages)
            }

            override fun onError(exception: CometChatException) {
                continuation.resumeWithException(exception)
            }
        })
    }

    override suspend fun fetchNextMessages(
        request: MessagesRequest
    ): List<BaseMessage> = suspendCancellableCoroutine { continuation ->
        // 5.1.0 two-phase overload, server phase only (§3a): the cached phase is ignored, so the
        // request, result and events equal the deprecated CallbackListener overload's.
        request.fetchNext(object : CometChat.MessageFetchListener() {
            override fun onServerMessages(messages: List<BaseMessage>) {
                continuation.resume(messages)
            }

            override fun onError(exception: CometChatException) {
                continuation.resumeWithException(exception)
            }
        })
    }

    override suspend fun getConversation(
        id: String,
        type: String
    ): Conversation = suspendCancellableCoroutine { continuation ->
        // Two-phase overload (BEH-READ-32), settled once on the server phase (no cached fallback):
        // the result equals the deprecated overload's, so the read paths built on it are unchanged.
        CometChat.getConversation(id, type, ConversationDetailsOnce(cachedFallback = false) { result ->
            result.fold(continuation::resume, continuation::resumeWithException)
        })
    }

    override suspend fun getMessage(
        messageId: Long
    ): BaseMessage = suspendCancellableCoroutine { continuation ->
        // Two-phase overload (BEH-READ-35), settled once on the server phase (no cached fallback):
        // the result equals the deprecated overload's, so the read paths built on it are unchanged.
        CometChat.getMessageDetails(messageId, MessageDetailsOnce(cachedFallback = false) { result ->
            result.fold(continuation::resume, continuation::resumeWithException)
        })
    }

    /**
     * The chat-open conversation read (D60) only: settled once on the server value, else the
     * cached copy when the server phase fails (offline). [getConversation] stays server-only.
     */
    internal suspend fun getConversationCachedAllowed(
        id: String,
        type: String
    ): SettledRead<Conversation> = suspendCancellableCoroutine { continuation ->
        CometChat.getConversation(id, type, ConversationDetailsSourceOnce { result ->
            result.fold(continuation::resume, continuation::resumeWithException)
        })
    }

    /**
     * The offline chat-open jump target (D60) only: settled once on the server value, else the
     * cached copy when the server phase fails with the not-connected error. Any other error stands.
     * [getMessage] stays server-only.
     */
    internal suspend fun getMessageCachedAllowed(
        messageId: Long
    ): BaseMessage = suspendCancellableCoroutine { continuation ->
        val listener = MessageDetailsOnce(
            cachedFallback = true,
            onDone = { result -> result.fold(continuation::resume, continuation::resumeWithException) },
            fallbackOn = SearchOffline::isNotConnected
        )
        CometChat.getMessageDetails(messageId, listener)
    }

    override suspend fun deleteMessage(
        messageId: Long
    ): BaseMessage? = suspendCancellableCoroutine { continuation ->
        CometChat.deleteMessage(messageId, object : CometChat.CallbackListener<BaseMessage>() {
            override fun onSuccess(deletedMessage: BaseMessage?) {
                continuation.resume(deletedMessage)
            }

            override fun onError(exception: CometChatException) {
                continuation.resumeWithException(exception)
            }
        })
    }

    override suspend fun flagMessage(
        messageId: Long,
        reason: String,
        remark: String
    ): Unit = suspendCancellableCoroutine { continuation ->
        val flagDetail = FlagDetail(reason, remark)
        CometChat.flagMessage(messageId, flagDetail, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(result: String) {
                continuation.resume(Unit)
            }

            override fun onError(exception: CometChatException) {
                continuation.resumeWithException(exception)
            }
        })
    }

    override suspend fun addReaction(
        messageId: Long,
        emoji: String
    ): BaseMessage = suspendCancellableCoroutine { continuation ->
        CometChat.addReaction(messageId, emoji, object : CometChat.CallbackListener<BaseMessage>() {
            override fun onSuccess(updatedMessage: BaseMessage) {
                continuation.resume(updatedMessage)
            }

            override fun onError(exception: CometChatException) {
                continuation.resumeWithException(exception)
            }
        })
    }

    override suspend fun removeReaction(
        messageId: Long,
        emoji: String
    ): BaseMessage = suspendCancellableCoroutine { continuation ->
        CometChat.removeReaction(messageId, emoji, object : CometChat.CallbackListener<BaseMessage>() {
            override fun onSuccess(updatedMessage: BaseMessage) {
                continuation.resume(updatedMessage)
            }

            override fun onError(exception: CometChatException) {
                continuation.resumeWithException(exception)
            }
        })
    }

    override suspend fun markAsDelivered(message: BaseMessage) {
        CometChat.markAsDelivered(message)
    }

    override suspend fun markAsRead(
        message: BaseMessage
    ): Unit = suspendCancellableCoroutine { continuation ->
        CometChat.markAsRead(message, object : CometChat.CallbackListener<Void?>() {
            override fun onSuccess(unused: Void?) {
                continuation.resume(Unit)
            }

            override fun onError(exception: CometChatException) {
                continuation.resumeWithException(exception)
            }
        })
    }

    override suspend fun markAsUnread(
        message: BaseMessage
    ): Conversation = suspendCancellableCoroutine { continuation ->
        CometChat.markMessageAsUnread(message, object : CometChat.CallbackListener<Conversation>() {
            override fun onSuccess(conversation: Conversation) {
                continuation.resume(conversation)
            }

            override fun onError(exception: CometChatException) {
                continuation.resumeWithException(exception)
            }
        })
    }
}
