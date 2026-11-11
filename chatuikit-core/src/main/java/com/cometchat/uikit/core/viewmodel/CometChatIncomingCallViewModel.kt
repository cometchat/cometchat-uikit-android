package com.cometchat.uikit.core.viewmodel

import androidx.lifecycle.ViewModel
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.model.InitiatedCall
import com.cometchat.chat.core.Call
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.uikit.core.events.CometChatCallEvent
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.utils.CallRingingUtils
import com.cometchat.uikit.core.utils.CallsUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for CometChatIncomingCall component.
 * Manages incoming call state and actions for both Compose and XML Views implementations.
 * 
 * This ViewModel:
 * - Exposes StateFlows for the current call, accepted call, and rejected call states
 * - Exposes a SharedFlow for error events
 * - Accepts locally (the component opens the ongoing-call screen) and declines through the
 *   Calls SDK; it never uses the Chat SDK's deprecated calling APIs. An app that rings with
 *   the Push SDK answers and declines through `CometChatCallNotifications` itself, from its
 *   own accept and decline listeners
 * - Listens for the call ending elsewhere through the Calls SDK's incoming-call listener
 * - Emits CometChatCallEvent.CallAccepted/CallRejected through CometChatEvents
 * 
 * Validates: Requirements 5.1-5.14
 */
@Deprecated("Ringing v2 runs the Push SDK in AUTO mode: push-notifications-calls-android rings and draws the incoming call itself. The app's answer activity opens CometChatOngoingCallActivity with the answered call; drop this component unless the app draws its own ring screen with setIncomingCallActivity and wires accept and decline to CometChatCallNotifications itself.")
open public class CometChatIncomingCallViewModel(
    private val enableListeners: Boolean = true
) : ViewModel() {

    public companion object {
        private const val TAG = "CometChatIncomingCallViewModel"
    }

    // ==================== State Flows ====================

    /**
     * StateFlow for the current incoming call.
     * 
     * Validates: Requirement 5.2
     */
    private val _call = MutableStateFlow<Call?>(null)
    public val call: StateFlow<Call?> = _call.asStateFlow()

    /**
     * StateFlow for the accepted call result.
     * Emits the Call object when acceptCall succeeds.
     * 
     * Validates: Requirement 5.3
     */
    private val _acceptedCall = MutableStateFlow<Call?>(null)
    public val acceptedCall: StateFlow<Call?> = _acceptedCall.asStateFlow()

    /**
     * StateFlow for the rejected/cancelled call result.
     * Emits the Call object when rejectCall succeeds or when the call is cancelled by the caller.
     * 
     * Validates: Requirement 5.4
     */
    private val _rejectedCall = MutableStateFlow<Call?>(null)
    public val rejectedCall: StateFlow<Call?> = _rejectedCall.asStateFlow()

    /**
     * SharedFlow for error events.
     * Emits CometChatException when an error occurs during call operations.
     * 
     * Validates: Requirement 5.5
     */
    private val _errorEvent = MutableSharedFlow<CometChatException>()
    public val errorEvent: SharedFlow<CometChatException> = _errorEvent.asSharedFlow()

    // ==================== Internal State ====================

    private var listenerId: String? = null

    // ==================== Initialization ====================

    init {
        if (enableListeners) {
            addListeners()
        }
    }

    // ==================== Public Methods ====================

    /**
     * Sets the current incoming call.
     * Resets any stale state from previous calls to prevent unintended actions.
     * 
     * @param call The incoming Call object
     */
    public fun setCall(call: Call) {
        // Reset stale state from previous calls to prevent unintended actions
        // This is important when the ViewModel is reused (e.g., in Compose with remember)
        resetState()
        _call.value = call
    }

    /**
     * Gets the current call.
     * 
     * @return The current Call or null if not set
     */
    public fun getCall(): Call? = _call.value

    /**
     * Resets the ViewModel state to initial values.
     * Called when a new call is set to prevent stale state from triggering actions.
     */
    public fun resetState() {
        _acceptedCall.value = null
        _rejectedCall.value = null
    }

    /**
     * Accepts the current incoming call: emits it through [acceptedCall], which makes the
     * component open the ongoing-call screen, and CometChatCallEvent.CallAccepted.
     *
     * An app that rings with the Push SDK must also call
     * `CometChatCallNotifications.answer(context, sessionId)` from its accept listener so the
     * Push SDK stops its ring.
     */
    public fun acceptCall() {
        val currentCall = _call.value ?: return
        if (currentCall.sessionId == null) return

        _acceptedCall.value = currentCall
        CometChatEvents.emitCallEvent(CometChatCallEvent.CallAccepted(currentCall))
    }

    /**
     * Declines the current incoming call as rejected through the Calls SDK
     * ([CallRingingUtils.decline]), then emits it through [rejectedCall] and
     * CometChatCallEvent.CallRejected.
     *
     * An app that rings with the Push SDK should instead call
     * `CometChatCallNotifications.decline(context, sessionId)` from its decline listener, which
     * stops the ring and sends busy or rejected through the Push SDK's durable queue.
     */
    public fun rejectCall() {
        val currentCall = _call.value ?: return
        val sessionId = currentCall.sessionId ?: return

        CallRingingUtils.decline(sessionId)
        _rejectedCall.value = currentCall
        CometChatEvents.emitCallEvent(CometChatCallEvent.CallRejected(currentCall))
    }

    /**
     * Listens for the call ending elsewhere (caller cancelled, timed out, answered on
     * another device) through the Calls SDK's incoming-call listener.
     */
    public fun addListeners() {
        if (listenerId != null || !CallsUtils.isCallsSDKAvailable()) {
            return
        }

        val id = "IncomingCall_${System.currentTimeMillis()}"
        listenerId = id

        CometChatCalls.addIncomingCallListener(id, object : CometChatCalls.IncomingCallListener() {
            override fun onIncomingCallReceived(call: InitiatedCall) {
                // Not handled - incoming calls are set via setCall()
            }

            override fun onIncomingCallEnded(call: InitiatedCall, reason: String) {
                handleIncomingCallEnded(call.sessionId)
            }
        })
    }

    /**
     * Removes the Calls SDK incoming-call listener.
     */
    public fun removeListeners() {
        listenerId?.let { id ->
            if (CallsUtils.isCallsSDKAvailable()) {
                CometChatCalls.removeIncomingCallListener(id)
            }
            listenerId = null
        }
    }

    /**
     * Called when the ring for [sessionId] ended without this screen answering or
     * declining it. Emits the current call through [rejectedCall] so the UI closes.
     */
    public fun onCallEnded(sessionId: String?) {
        handleIncomingCallEnded(sessionId)
    }

    // ==================== Private Methods ====================

    private fun handleIncomingCallEnded(sessionId: String?) {
        val currentCall = _call.value ?: return
        if (sessionId != null && sessionId == currentCall.sessionId &&
            _acceptedCall.value == null && _rejectedCall.value == null
        ) {
            _rejectedCall.value = currentCall
        }
    }

    // ==================== Lifecycle ====================

    override fun onCleared() {
        super.onCleared()
        removeListeners()
    }
}
