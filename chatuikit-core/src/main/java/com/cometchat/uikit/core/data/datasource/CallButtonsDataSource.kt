package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.Call
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.User

/**
 * Data source interface for call buttons operations.
 * Defines methods for initiating calls and checking active call state.
 */
public interface CallButtonsDataSource {
    /**
     * Initiates a 1-to-1 call with a user.
     * @param receiverId The UID of the user to call
     * @param callType The type of call (audio/video)
     * @param receiver The callee as the UIKit knows them, for the name and avatar on the outgoing screen
     * @return Result containing the initiated Call or an error
     */
    suspend public fun initiateUserCall(receiverId: String, callType: String, receiver: User? = null): Result<Call>

    /**
     * Sends a custom message to start a group conference call.
     * @param groupId The GUID of the group
     * @param callType The type of call (audio/video)
     * @return Result containing the sent CustomMessage or an error
     */
    suspend public fun sendGroupCallMessage(groupId: String, callType: String): Result<CustomMessage>

    /**
     * Gets the UIKit's currently active call. The Chat SDK no longer tracks one
     * (its calling APIs are deprecated in favour of the Calls SDK).
     * @return The active Call or null if no call is active
     */
    public fun getActiveCall(): Call?

    /**
     * Checks if the Calls SDK has a session joining or joined.
     * @return true if a call session is active, false otherwise
     */
    public fun hasActiveCallSession(): Boolean = false

    /**
     * Gets the currently active call from CallingExtension.
     * @return The active Call or null if no call is active
     */
    public fun getActiveCallingExtensionCall(): Call?

    /**
     * Checks if there is an active meeting in progress.
     * @return true if a meeting is active, false otherwise
     */
    public fun isActiveMeeting(): Boolean
}
