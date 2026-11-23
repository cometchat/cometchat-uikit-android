package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.GroupsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementation of GroupsDataSource that communicates with CometChat SDK.
 * Contains NO business logic - just raw data fetching/saving.
 * This is the default implementation used for remote data operations.
 *
 * Both reads use the SDK's two-phase [CometChat.GroupFetchListener] (BEH-SNAP-03):
 * [fetchGroupsTwoPhase] emits the cached and the server phase; [fetchGroups] keeps today's
 * single-result contract by returning the server phase only (the cached phase is ignored).
 */
public class GroupsDataSourceImpl : GroupsDataSource, GroupsTwoPhaseDataSource {

    override fun fetchGroupsTwoPhase(request: GroupsRequest): Flow<TwoPhaseResult<Group>> = callbackFlow {
        request.fetchNext(object : CometChat.GroupFetchListener() {
            override fun onCachedGroups(groups: List<Group>) {
                trySend(TwoPhaseResult.Cached(groups))
            }

            override fun onServerGroups(groups: List<Group>) {
                trySend(TwoPhaseResult.Server(groups))
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
     * Fetches groups from CometChat SDK.
     * @param request The configured GroupsRequest
     * @return Raw list of Group objects from SDK (the server phase)
     * @throws CometChatException if SDK call fails
     */
    override suspend fun fetchGroups(
        request: GroupsRequest
    ): List<Group> = suspendCancellableCoroutine { continuation ->
        request.fetchNext(object : CometChat.GroupFetchListener() {
            override fun onServerGroups(groups: List<Group>) {
                if (continuation.isActive) continuation.resume(groups)
            }

            override fun onError(exception: CometChatException) {
                if (continuation.isActive) continuation.resumeWithException(exception)
            }
        })
    }
    
    /**
     * Joins a group via CometChat SDK.
     * @param groupId The ID of the group to join
     * @param groupType The type of group (public, private, password)
     * @param password Optional password for password-protected groups
     * @return The joined Group from SDK
     * @throws CometChatException if SDK call fails
     */
    override suspend fun joinGroup(
        groupId: String,
        groupType: String,
        password: String?
    ): Group = suspendCancellableCoroutine { continuation ->
        CometChat.joinGroup(
            groupId,
            groupType,
            password ?: "",
            object : CometChat.CallbackListener<Group>() {
                override fun onSuccess(group: Group) {
                    continuation.resume(group)
                }
                
                override fun onError(exception: CometChatException) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }
}
