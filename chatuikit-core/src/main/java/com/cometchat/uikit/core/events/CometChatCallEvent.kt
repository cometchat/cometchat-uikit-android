package com.cometchat.uikit.core.events

import com.cometchat.chat.core.Call

/**
 * Sealed class hierarchy representing all call-related events.
 * Provides type-safe event handling for call state changes.
 */
sealed public class CometChatCallEvent {
    /**
     * Event emitted when an outgoing call is initiated.
     * @param call The outgoing call
     */
    public data class OutgoingCall(
        val call: Call
    ) : CometChatCallEvent()

    /**
     * Event emitted when a call is accepted.
     * @param call The accepted call
     */
    public data class CallAccepted(
        val call: Call
    ) : CometChatCallEvent()

    /**
     * Event emitted when a call is rejected.
     * @param call The rejected call
     */
    public data class CallRejected(
        val call: Call
    ) : CometChatCallEvent()

    /**
     * Event emitted when a call ends.
     * @param call The ended call
     */
    public data class CallEnded(
        val call: Call
    ) : CometChatCallEvent()
}
