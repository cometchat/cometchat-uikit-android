package com.cometchat.uikit.core.state

import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage

/**
 * Sealed class representing UI states for the message list screen.
 * Used by the ViewModel to communicate current state to the UI.
 */
sealed public class MessageListUIState {
    /**
     * Loading state - displayed while fetching messages.
     */
    public object Loading : MessageListUIState()
    
    /**
     * Empty state - displayed when no messages exist.
     */
    public object Empty : MessageListUIState()
    
    /**
     * Loaded state - displayed when messages are available.
     */
    public object Loaded : MessageListUIState()
    
    /**
     * Error state - displayed when fetching fails.
     * @param exception The exception that caused the error
     */
    public data class Error(val exception: Throwable) : MessageListUIState()
}

/**
 * Sealed class representing message delete operation states.
 * Used to track the progress of message deletion.
 */
sealed public class MessageDeleteState {
    /**
     * Idle state - no delete operation in progress.
     */
    public object Idle : MessageDeleteState()
    
    /**
     * InProgress state - delete operation is currently running.
     */
    public object InProgress : MessageDeleteState()
    
    /**
     * Success state - delete operation completed successfully.
     * @param message The deleted message
     */
    public data class Success(val message: BaseMessage) : MessageDeleteState()

    
    /**
     * Error state - delete operation failed.
     * @param exception The exception that caused the failure
     */
    public data class Error(val exception: Throwable) : MessageDeleteState()
}

/**
 * Sealed class representing message flag/report operation states.
 * Used to track the progress of message flagging.
 */
sealed public class MessageFlagState {
    /**
     * Idle state - no flag operation in progress.
     */
    public object Idle : MessageFlagState()
    
    /**
     * InProgress state - flag operation is currently running.
     */
    public object InProgress : MessageFlagState()
    
    /**
     * Success state - flag operation completed successfully.
     */
    public object Success : MessageFlagState()
    
    /**
     * Error state - flag operation failed.
     * @param exception The exception that caused the failure
     */
    public data class Error(val exception: Throwable) : MessageFlagState()
}

/**
 * Message alignment in the list.
 * Determines how messages are positioned horizontally.
 */
public enum class MessageAlignment {
    /**
     * LEFT alignment - for incoming messages from other users.
     */
    LEFT,
    
    /**
     * RIGHT alignment - for outgoing messages from current user.
     */
    RIGHT,
    
    /**
     * CENTER alignment - for action/system messages (group actions, call actions).
     */
    CENTER
}
