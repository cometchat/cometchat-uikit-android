package com.cometchat.uikit.core.state

import com.cometchat.chat.exceptions.CometChatException

/**
 * Sealed class representing the UI state for CometChatCallButtons component.
 * Used to manage the state of call initiation operations.
 */
sealed public class CallButtonsUIState {
    /**
     * Default state when the component is ready to initiate calls.
     */
    public object Idle : CallButtonsUIState()

    /**
     * State when a call initiation is in progress.
     */
    public object Initiating : CallButtonsUIState()

    /**
     * State when call initiation has failed.
     * @param exception The exception that caused the failure
     */
    public data class Error(val exception: CometChatException) : CallButtonsUIState()
}
