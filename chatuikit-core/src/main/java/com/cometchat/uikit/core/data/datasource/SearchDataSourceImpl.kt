package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementation of SearchDataSource that communicates with CometChat SDK.
 * Contains NO business logic - just raw data fetching.
 * This is the default implementation used for remote data operations.
 *
 * Every read uses the SDK 5.1.0 two-phase listeners (`ConversationFetchListener`,
 * `MessageFetchListener`); no deprecated `CallbackListener` overload is called. The single-phase
 * [fetchConversations] / [fetchMessages] take the server phase only, which is exactly today's
 * result: with offline off the SDK fires no cached phase at all. The two-phase reads
 * ([SearchTwoPhaseDataSource]) surface both phases as a [Flow] of [TwoPhaseResult].
 */
internal class SearchDataSourceImpl : SearchDataSource, SearchTwoPhaseDataSource {

    /**
     * Fetches conversations from CometChat SDK (server phase only).
     * @param request The configured ConversationsRequest
     * @return Raw list of Conversation objects from SDK
     * @throws com.cometchat.chat.exceptions.CometChatException if SDK call fails
     */
    override suspend fun fetchConversations(
        request: ConversationsRequest
    ): List<Conversation> = suspendCancellableCoroutine { continuation ->
        request.fetchNext(object : CometChat.ConversationFetchListener() {
            override fun onServerConversations(conversations: List<Conversation>) {
                if (continuation.isActive) continuation.resume(conversations)
            }

            override fun onError(e: CometChatException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        })
    }

    /**
     * Fetches messages from CometChat SDK (server phase only).
     * @param request The configured MessagesRequest
     * @return Raw list of BaseMessage objects from SDK
     * @throws com.cometchat.chat.exceptions.CometChatException if SDK call fails
     */
    override suspend fun fetchMessages(
        request: MessagesRequest
    ): List<BaseMessage> = suspendCancellableCoroutine { continuation ->
        request.fetchPrevious(object : CometChat.MessageFetchListener() {
            override fun onServerMessages(messages: List<BaseMessage>) {
                if (continuation.isActive) continuation.resume(messages)
            }

            override fun onError(e: CometChatException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        })
    }

    override fun fetchConversationsTwoPhase(
        request: ConversationsRequest
    ): Flow<TwoPhaseResult<Conversation>> = callbackFlow {
        request.fetchNext(object : CometChat.ConversationFetchListener() {
            override fun onCachedConversations(conversations: List<Conversation>) {
                trySend(TwoPhaseResult.Cached(conversations))
            }

            override fun onServerConversations(conversations: List<Conversation>) {
                trySend(TwoPhaseResult.Server(conversations))
                close()
            }

            override fun onError(e: CometChatException) {
                trySend(TwoPhaseResult.Error(e))
                close()
            }
        })
        awaitClose { }
    }

    override fun fetchMessagesTwoPhase(
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

            override fun onError(e: CometChatException) {
                trySend(TwoPhaseResult.Error(e))
                close()
            }
        })
        awaitClose { }
    }
}
