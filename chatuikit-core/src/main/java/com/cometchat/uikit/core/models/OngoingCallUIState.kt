package com.cometchat.uikit.core.models

import com.cometchat.chat.exceptions.CometChatException

/**
 * Sealed class representing UI states for the ongoing call screen.
 * Used by the ViewModel to communicate current state to the UI.
 *
 * Validates: Requirement 1.4
 */
sealed public class OngoingCallUIState {
    /**
     * Loading state - displayed while connecting to the call.
     */
    public object Loading : OngoingCallUIState()

    /**
     * Connected state - displayed when the call is active.
     *
     * @param sessionId The unique identifier for the call session
     * @param callType The type of call ("audio" or "video")
     */
    public data class Connected(val sessionId: String, val callType: String) : OngoingCallUIState()

    /**
     * Ended state - displayed when the call has ended.
     */
    public object Ended : OngoingCallUIState()

    /**
     * Error state - displayed when an error occurs during the call.
     *
     * @param exception The CometChatException that caused the error
     */
    public data class Error(val exception: CometChatException) : OngoingCallUIState()
}
