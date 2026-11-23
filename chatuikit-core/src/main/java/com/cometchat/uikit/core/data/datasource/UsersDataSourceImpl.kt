package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementation of UsersDataSource that communicates with CometChat SDK.
 * Contains NO business logic - just raw data fetching.
 * This is the default implementation used for remote data operations.
 *
 * Both reads use the SDK's two-phase [CometChat.UserFetchListener] (BEH-SNAP-03):
 * [fetchUsersTwoPhase] emits the cached and the server phase; [fetchUsers] keeps today's
 * single-result contract by returning the server phase only (the cached phase is ignored).
 */
public class UsersDataSourceImpl : UsersDataSource, UsersTwoPhaseDataSource {

    override fun fetchUsersTwoPhase(request: UsersRequest): Flow<TwoPhaseResult<User>> = callbackFlow {
        request.fetchNext(object : CometChat.UserFetchListener() {
            override fun onCachedUsers(users: List<User>) {
                trySend(TwoPhaseResult.Cached(users))
            }

            override fun onServerUsers(users: List<User>) {
                trySend(TwoPhaseResult.Server(users))
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
     * Fetches users from CometChat SDK.
     * @param request The configured UsersRequest
     * @return Raw list of User objects from SDK (the server phase)
     * @throws com.cometchat.chat.exceptions.CometChatException if SDK call fails
     */
    override suspend fun fetchUsers(request: UsersRequest): List<User> =
        suspendCancellableCoroutine { continuation ->
            request.fetchNext(object : CometChat.UserFetchListener() {
                override fun onServerUsers(users: List<User>) {
                    if (continuation.isActive) continuation.resume(users)
                }

                override fun onError(exception: CometChatException) {
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
            })
        }
}
