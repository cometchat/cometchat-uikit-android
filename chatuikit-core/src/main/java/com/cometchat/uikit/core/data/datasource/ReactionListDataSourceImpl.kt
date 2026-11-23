package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Reaction
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implementation of ReactionListDataSource that communicates with CometChat SDK.
 * Contains NO business logic - just raw data fetching.
 * This is the default implementation used for remote data operations.
 *
 * Every fetch uses the SDK 5.1.0 two-phase `ReactionsRequest.fetchNext(ReactionFetchListener)`.
 * [fetchReactions] (single phase) takes the server phase only — it leaves the cached callback
 * unimplemented, so its result is today's. [fetchReactionsTwoPhase] (G2) also delivers the cached
 * phase, which the SDK fires only when offline is on (BEH-READ-04).
 */
internal class ReactionListDataSourceImpl : ReactionListDataSource, ReactionListTwoPhaseDataSource {

    /**
     * Fetches reactions from CometChat SDK (server phase).
     * @param request The configured ReactionsRequest
     * @return Result containing list of Reaction objects or error
     */
    override suspend fun fetchReactions(request: ReactionsRequest): Result<List<Reaction>> =
        suspendCancellableCoroutine { continuation ->
            request.fetchNext(object : CometChat.ReactionFetchListener() {
                override fun onServerReactions(reactions: List<Reaction>) {
                    continuation.resume(Result.success(reactions))
                }

                override fun onError(exception: CometChatException) {
                    continuation.resume(Result.failure(exception))
                }
            })
        }

    override fun fetchReactionsTwoPhase(request: ReactionsRequest): Flow<TwoPhaseResult<Reaction>> = callbackFlow {
        request.fetchNext(object : CometChat.ReactionFetchListener() {
            override fun onCachedReactions(reactions: List<Reaction>) {
                trySend(TwoPhaseResult.Cached(reactions))
            }

            override fun onServerReactions(reactions: List<Reaction>) {
                trySend(TwoPhaseResult.Server(reactions))
                close()
            }

            override fun onError(exception: CometChatException) {
                trySend(TwoPhaseResult.Error(exception))
                close()
            }
        })
        awaitClose { }
    }

    /**
     * Removes a reaction from a message using CometChat SDK.
     * @param messageId The ID of the message to remove reaction from
     * @param emoji The emoji reaction to remove
     * @return Result containing the updated BaseMessage or error
     */
    override suspend fun removeReaction(messageId: Long, emoji: String): Result<BaseMessage> =
        suspendCancellableCoroutine { continuation ->
            CometChat.removeReaction(messageId, emoji, object : CometChat.CallbackListener<BaseMessage>() {
                override fun onSuccess(message: BaseMessage) {
                    continuation.resume(Result.success(message))
                }

                override fun onError(exception: CometChatException) {
                    continuation.resume(Result.failure(exception))
                }
            })
        }
}
