package com.cometchat.uikit.core.domain.usecase

import com.cometchat.chat.core.Call
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.CallButtonsRepository

/**
 * Use case for initiating a 1-to-1 call with a user.
 * Checks for active calls before initiating a new call.
 */
open public class InitiateUserCallUseCase(
    private val repository: CallButtonsRepository
) {
    /**
     * Initiates a call with the specified user.
     * @param receiverId The UID of the user to call
     * @param callType The type of call (audio/video)
     * @param receiver The callee as the UIKit knows them, for the name and avatar on the outgoing screen
     * @return Result containing the initiated Call or an error if active call exists or initiation fails
     */
    open suspend operator public fun invoke(receiverId: String, callType: String, receiver: User? = null): Result<Call> {
        if (repository.hasActiveCall()) {
            return Result.failure(
                CometChatException(
                    UIKitConstants.CallErrorCodes.ACTIVE_CALL,
                    "Cannot initiate call while another call is active",
                    "An active call is already in progress"
                )
            )
        }
        return repository.initiateUserCall(receiverId, callType, receiver)
    }
}
