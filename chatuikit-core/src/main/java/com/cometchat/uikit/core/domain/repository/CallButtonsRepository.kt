package com.cometchat.uikit.core.domain.repository

import com.cometchat.chat.core.Call
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.User

/**
 * Repository interface for call buttons operations.
 * Provides abstraction over the data source for call initiation and active call detection.
 */
public interface CallButtonsRepository {
    /**
     * Initiates a 1-to-1 call with a user.
     * @param receiverId The UID of the user to call
     * @param callType The type of call (audio/video)
     * @param receiver The callee as the UIKit knows them, for the name and avatar on the outgoing screen
     * @return Result containing the initiated Call or an error
     */
    suspend public fun initiateUserCall(receiverId: String, callType: String, receiver: User? = null): Result<Call>

    /**
     * Starts a group conference call by sending a custom message.
     * @param groupId The GUID of the group
     * @param callType The type of call (audio/video)
     * @return Result containing the sent CustomMessage or an error
     */
    suspend public fun startGroupCall(groupId: String, callType: String): Result<CustomMessage>

    /**
     * Checks if there is any active call in progress.
     * Checks the Calls SDK active session, the UIKit's active call, and whether a meeting is active.
     * @return true if any active call exists, false otherwise
     */
    public fun hasActiveCall(): Boolean
}
