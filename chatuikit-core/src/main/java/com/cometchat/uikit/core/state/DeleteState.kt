package com.cometchat.uikit.core.state

import com.cometchat.chat.exceptions.CometChatException

/**
 * Sealed class representing delete operation states.
 * Used to track the progress of conversation deletion.
 */
sealed public class DeleteState {
    /**
     * Idle state - no delete operation in progress.
     */
    public object Idle : DeleteState()
    
    /**
     * InProgress state - delete operation is currently running.
     */
    public object InProgress : DeleteState()
    
    /**
     * Success state - delete operation completed successfully.
     */
    public object Success : DeleteState()
    
    /**
     * Failure state - delete operation failed.
     * @param exception The exception that caused the failure
     */
    public data class Failure(val exception: CometChatException) : DeleteState()
}
