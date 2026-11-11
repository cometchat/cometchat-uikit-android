package com.cometchat.uikit.core.domain.usecase

import com.cometchat.calls.model.CallLog
import com.cometchat.calls.model.CallUser
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.utils.CallInitiationUtils

/**
 * Use case for initiating a call based on a call log.
 * Determines the correct receiver based on who initiated the original call.
 */
open public class InitiateCallUseCase {
    
    /**
     * Initiates a call based on the call log and call type.
     * Determines the receiver - if logged-in user was initiator, call the receiver; 
     * otherwise call the initiator.
     * 
     * @param callLog The call log to base the new call on
     * @param callType The type of call to initiate (audio/video)
     * @return Result containing the initiated Call on success, or exception on failure
     */
    open suspend operator public fun invoke(callLog: CallLog, callType: String): Result<Call> {
        val receiverId = getReceiverId(callLog)
        
        if (receiverId.isEmpty()) {
            return Result.failure(
                CometChatException(
                    "INVALID_RECEIVER",
                    "Could not determine receiver for call"
                )
            )
        }
        
        // Ringing v2: the Calls SDK creates the call; the Chat SDK's initiateCall is deprecated.
        val context = CometChatUIKit.getApplicationContext()
            ?: return Result.failure(
                CometChatException("NOT_INITIALIZED", "CometChatUIKit.init must run before starting a call")
            )
        val target = listOf(callLog.initiator, callLog.receiver)
            .filterIsInstance<CallUser>()
            .firstOrNull { it.uid == receiverId }
        val receiver = target?.let { User().apply { uid = it.uid; name = it.name; avatar = it.avatar } }
        return CallInitiationUtils.initiateUserCall(context, receiverId, callType, receiver)
    }
    
    /**
     * Determines the receiver ID for the call.
     * If the logged-in user was the initiator of the original call, 
     * the receiver becomes the target. Otherwise, the initiator becomes the target.
     * 
     * @param callLog The call log to determine receiver from
     * @return The UID of the user to call
     */
    private fun getReceiverId(callLog: CallLog): String {
        val initiator = callLog.initiator as? CallUser
        val receiver = callLog.receiver as? CallUser
        val loggedInUserId = CometChat.getLoggedInUser()?.uid
        
        return if (initiator?.uid == loggedInUserId) {
            receiver?.uid ?: ""
        } else {
            initiator?.uid ?: ""
        }
    }
}
