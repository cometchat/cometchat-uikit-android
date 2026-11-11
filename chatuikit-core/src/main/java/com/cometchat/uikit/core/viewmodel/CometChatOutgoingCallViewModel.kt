package com.cometchat.uikit.core.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.calls.core.CallSession
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.listeners.ParticipantEventListener
import com.cometchat.calls.listeners.SessionStatusListener
import com.cometchat.calls.model.Participant
import com.cometchat.calls.model.InitiatedCall
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.events.CometChatCallEvent
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.utils.CallInitiationUtils
import com.cometchat.uikit.core.utils.CallingState
import com.cometchat.uikit.core.utils.CallsUtils
import com.cometchat.uikit.core.utils.CometChatLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for CometChatOutgoingCall component.
 * Manages outgoing call state and actions for both Compose and XML Views implementations.
 *
 * Ringing v2: the call was created with `CometChatCalls.initiateCall`, which also prepared the
 * media session, and the backend rang the callee by push. This ViewModel learns what happened
 * from the Calls SDK only; the Chat SDK's deprecated calling APIs are not used.
 *
 * - Ringing / answered: the caller stays on the session `initiateCall` prepared, without
 *   joining. `onParticipantConnected` for the callee means their phone is ringing;
 *   `onParticipantJoined` means they answered, and only then does the screen `joinSession`.
 * - Declined, busy, no answer, cancelled elsewhere: the backend's terminal push reaches the
 *   caller too; the Push SDK hands it to the Calls SDK, which reports it through
 *   `IncomingCallListener.onIncomingCallEnded(call, reason)`.
 * - Cancel: `CometChatCalls.endCall(sessionId, "cancelled")`.
 * - No answer with no terminal push: this ViewModel's own ring timer ends the call with
 *   `"unanswered"` so both sides stop together.
 *
 * Exposes StateFlows for the current call, accepted call, rejected call, the end reason and
 * endCallButtonEnabled, plus a SharedFlow for errors.
 */
open public class CometChatOutgoingCallViewModel(
    private val enableListeners: Boolean = true
) : ViewModel() {

    public companion object {
        private const val TAG = "CometChatOutgoingCallViewModel"

        /**
         * Fallback ring window. Matches the Push SDK's default on the callee, so a call nobody
         * answers stops on both phones at the same time.
         */
        public const val DEFAULT_RING_TIMEOUT_MS: Long = 45_000L

        /** Grace for the terminal push after `onConnectionClosed`, see [closeGraceMs]. */
        public const val DEFAULT_CLOSE_GRACE_MS: Long = 1_500L

        /** The longest a cancel waits for `endCall` to report back before the screen closes. */
        internal const val CANCEL_CAP_MS: Long = 10_000L

        /**
         * The Calls SDK's end reasons in the UI Kit's vocabulary, as on Flutter. Anything else is
         * not a reason this screen knows, and is ignored.
         */
        internal fun normaliseEndReason(reason: String?): String? = when (reason?.trim()?.lowercase()) {
            "rejected", "declined" -> UIKitConstants.CallStatusConstants.REJECTED
            "busy" -> UIKitConstants.CallStatusConstants.BUSY
            "cancelled", "canceled" -> UIKitConstants.CallStatusConstants.CANCELLED
            "unanswered", "timed_out", "timedout" -> UIKitConstants.CallStatusConstants.UNANSWERED
            "ended", "remoteended", "failed" -> UIKitConstants.CallStatusConstants.ENDED
            else -> null
        }
    }

    // ==================== State Flows ====================

    /** The current outgoing call. */
    private val _call = MutableStateFlow<Call?>(null)
    public val call: StateFlow<Call?> = _call.asStateFlow()

    /** Emits the call once the callee has joined the session. */
    private val _acceptedCall = MutableStateFlow<Call?>(null)
    public val acceptedCall: StateFlow<Call?> = _acceptedCall.asStateFlow()

    /**
     * Emits the call once it is over without being answered: cancelled here, declined, busy,
     * unanswered, or ended elsewhere. [endReason] says which.
     */
    private val _rejectedCall = MutableStateFlow<Call?>(null)
    public val rejectedCall: StateFlow<Call?> = _rejectedCall.asStateFlow()

    /**
     * Why [rejectedCall] was emitted, as a call status:
     * [UIKitConstants.CallStatusConstants.CANCELLED], [UIKitConstants.CallStatusConstants.REJECTED],
     * [UIKitConstants.CallStatusConstants.BUSY], [UIKitConstants.CallStatusConstants.UNANSWERED]
     * or [UIKitConstants.CallStatusConstants.ENDED]. Null while the call is ringing or answered.
     */
    private val _endReason = MutableStateFlow<String?>(null)
    public val endReason: StateFlow<String?> = _endReason.asStateFlow()

    /**
     * True once the callee's phone reports it is ringing (`onParticipantConnected` on the
     * prepared session), so the screen can switch "Calling…" to "Ringing…".
     */
    private val _calleeRinging = MutableStateFlow(false)
    public val calleeRinging: StateFlow<Boolean> = _calleeRinging.asStateFlow()

    /** Errors from call operations. */
    private val _errorEvent = MutableSharedFlow<CometChatException>()
    public val errorEvent: SharedFlow<CometChatException> = _errorEvent.asSharedFlow()

    /** False once the call is accepted, so it cannot be cancelled during the transition. */
    private val _endCallButtonEnabled = MutableStateFlow(true)
    public val endCallButtonEnabled: StateFlow<Boolean> = _endCallButtonEnabled.asStateFlow()

    // ==================== Internal State ====================

    private var listenerId: String? = null
    private var ringTimeoutJob: Job? = null
    private var closeGraceJob: Job? = null
    private var preparedSessionAttached: Boolean = false

    /**
     * Set once this side has ended the call (cancel or ring timeout). An answer that lands after
     * it is not joined: End wins, as on Flutter.
     */
    private var endRequested: Boolean = false
    private var cancelCapJob: Job? = null

    /**
     * How long to wait after the prepared session closes for the terminal push that carries
     * the reason, before closing the screen as "ended". 0 closes at once.
     */
    public var closeGraceMs: Long = DEFAULT_CLOSE_GRACE_MS

    /**
     * How long to ring before this side ends the call as unanswered. Defaults to
     * [DEFAULT_RING_TIMEOUT_MS]; a value of 0 or less disables the timer.
     */
    public var ringTimeoutMs: Long = DEFAULT_RING_TIMEOUT_MS

    // ==================== Initialization ====================

    init {
        if (enableListeners) {
            addListeners()
        }
    }

    // ==================== Public Methods ====================

    /**
     * Sets the current outgoing call and starts the ring timer when listeners are enabled.
     *
     * @param call The outgoing Call object
     */
    public fun setCall(call: Call) {
        _call.value = call
        _calleeRinging.value = false
        preparedSessionAttached = false
        endRequested = false
        CallingState.ringingOutgoingCall = call
        // Reset end call button enabled state when a new call is set
        _endCallButtonEnabled.value = true
        if (enableListeners) {
            startRingTimeout()
        }
    }

    /**
     * Gets the current call.
     *
     * @return The current Call or null if not set
     */
    public fun getCall(): Call? = _call.value

    /**
     * Cancels the current outgoing call with `CometChatCalls.endCall(sessionId, "cancelled")`.
     *
     * The screen closes either way: [rejectedCall] emits the call with [endReason]
     * cancelled, and CometChatCallEvent.CallRejected is emitted. If the Calls SDK refuses
     * the end, the exception also goes to [errorEvent].
     */
    public fun cancelCall() {
        val currentCall = _call.value ?: return
        // A second tap, or a cancel after the call was answered or ended: nothing more to send.
        if (endRequested || _acceptedCall.value != null || _rejectedCall.value != null) return
        endRequested = true
        _endCallButtonEnabled.value = false
        val sessionId = currentCall.sessionId
        stopRingTimeout()

        if (sessionId.isNullOrEmpty()) {
            finishLocally(currentCall, UIKitConstants.CallStatusConstants.CANCELLED)
            return
        }
        // A slow or lost endCall callback must not hold the screen open.
        cancelCapJob?.cancel()
        cancelCapJob = viewModelScope.launch {
            delay(CANCEL_CAP_MS)
            finishLocally(currentCall, UIKitConstants.CallStatusConstants.CANCELLED)
        }
        CallInitiationUtils.endCall(sessionId, UIKitConstants.CallStatusConstants.CANCELLED) { result ->
            cancelCapJob?.cancel()
            result.exceptionOrNull()?.let { e ->
                viewModelScope.launch {
                    _errorEvent.emit(e as? CometChatException ?: CometChatException("ERROR", e.message ?: "Cancel failed"))
                }
            }
            finishLocally(currentCall, UIKitConstants.CallStatusConstants.CANCELLED)
        }
    }

    /**
     * Registers the Calls SDK incoming-call listener, which reports the call ending elsewhere
     * (declined, busy, unanswered, cancelled) with its reason. Session events need a
     * lifecycle owner: see [attachPreparedSession].
     */
    public fun addListeners() {
        if (listenerId != null || !CallsUtils.isCallsSDKAvailable()) {
            return
        }

        val id = "OutgoingCall_${System.currentTimeMillis()}"
        listenerId = id

        CometChatCalls.addIncomingCallListener(id, object : CometChatCalls.IncomingCallListener() {
            override fun onIncomingCallReceived(call: InitiatedCall) {
                // Not handled by outgoing call component
            }

            override fun onIncomingCallEnded(call: InitiatedCall, reason: String) {
                handleCallEnded(call.sessionId, reason)
            }
        })

    }

    /**
     * Listens on the session `CometChatCalls.initiateCall` prepared, without joining it,
     * following the Calls SDK caller flow:
     * - `onParticipantConnected` for the callee: their phone got the push and is verifying
     *   the call, so it is ringing ([calleeRinging]).
     * - `onParticipantJoined` for the callee: they answered ([acceptedCall]); the screen
     *   then calls `joinSession`, which takes the prepared session over almost instantly.
     * - `onConnectionClosed` while still prepared: the call ended before anyone joined
     *   ([rejectedCall], with the reason from the terminal push when it has arrived).
     *
     * The listeners are lifecycle-bound to [owner], so the SDK drops them with the screen.
     * Call it after [setCall], once the prepared session exists.
     */
    public fun attachPreparedSession(owner: LifecycleOwner) {
        if (!CallsUtils.isCallsSDKAvailable()) return
        val session = CometChatCalls.getActiveSession() ?: CallSession.getInstance() ?: return
        val sessionId = _call.value?.sessionId
        if (sessionId != null && session.sessionId != null && session.sessionId != sessionId) return
        if (preparedSessionAttached) return
        preparedSessionAttached = true

        session.addSessionStatusListener(owner, object : SessionStatusListener() {
            override fun onConnectionClosed() {
                handleConnectionClosed()
            }

            // The prepared session gave up waiting: the call is over, as for a close.
            override fun onSessionTimedOut() {
                handleConnectionClosed()
            }
        })
        session.addParticipantEventListener(owner, object : ParticipantEventListener() {
            override fun onParticipantConnected(participant: Participant) {
                handleParticipantConnected(participant.uid)
            }

            override fun onParticipantJoined(participant: Participant) {
                handleUserJoined(participant.uid)
            }
        })
    }

    /**
     * Removes the Calls SDK listeners and stops the ring timer.
     */
    public fun removeListeners() {
        listenerId?.let { id ->
            if (CallsUtils.isCallsSDKAvailable()) {
                CometChatCalls.removeIncomingCallListener(id)
            }
            listenerId = null
        }
        stopRingTimeout()
        stopCloseGrace()
        cancelCapJob?.cancel()
    }

    // ==================== Private Methods ====================

    private fun myUid(): String? = runCatching { CometChatCalls.getLoggedInUser()?.uid }.getOrNull()
        ?: runCatching { CometChat.getLoggedInUser()?.uid }.getOrNull()

    /** The callee's phone prepared the session: it is ringing there. */
    private fun handleParticipantConnected(uid: String?) {
        CometChatLogger.d(TAG, "onParticipantConnected uid=$uid (ringing)")
        if (_call.value == null || _acceptedCall.value != null || _rejectedCall.value != null) return
        if (uid != null && uid.equals(myUid(), ignoreCase = true)) return
        _calleeRinging.value = true
    }

    /**
     * The prepared session closed before anyone joined: the call is over. The terminal push
     * usually arrives first with the reason; otherwise the reason is "ended".
     */
    private fun handleConnectionClosed() {
        CometChatLogger.d(TAG, "onConnectionClosed on the prepared session (reason so far: ${_endReason.value})")
        val currentCall = _call.value ?: return
        if (_acceptedCall.value != null || _rejectedCall.value != null) return
        stopRingTimeout()

        // The reason (rejected / busy / unanswered / cancelled) travels in the terminal push
        // the backend sends the caller; Android's Calls SDK exposes no getCallDetails to read
        // it after the close. Give that push a moment to land before closing as "ended".
        val known = _endReason.value
        if (known != null || closeGraceMs <= 0) {
            _endReason.value = known ?: UIKitConstants.CallStatusConstants.ENDED
            _rejectedCall.value = currentCall
            forgetOutgoing(currentCall)
            return
        }
        closeGraceJob?.cancel()
        closeGraceJob = viewModelScope.launch {
            delay(closeGraceMs)
            if (_acceptedCall.value != null || _rejectedCall.value != null) return@launch
            _endReason.value = _endReason.value ?: UIKitConstants.CallStatusConstants.ENDED
            _rejectedCall.value = currentCall
            forgetOutgoing(currentCall)
        }
    }

    /**
     * The callee joined the prepared session: the call is answered. The caller's own join
     * is ignored.
     */
    private fun handleUserJoined(uid: String?) {
        CometChatLogger.d(TAG, "onParticipantJoined uid=$uid (answered)")
        val currentCall = _call.value ?: return
        if (_acceptedCall.value != null || _rejectedCall.value != null) return
        if (uid != null && uid.equals(myUid(), ignoreCase = true)) return
        // End wins: the user already cancelled (or the ring timed out), so a late answer is not
        // joined. The callee's side ends with the cancel.
        if (endRequested) return

        stopRingTimeout()
        _endCallButtonEnabled.value = false
        _acceptedCall.value = currentCall
        forgetOutgoing(currentCall)
    }

    /**
     * The call ended elsewhere (declined, busy, unanswered, cancelled, ended). Only the
     * current session is handled, and only while the call is still ringing.
     */
    private fun handleCallEnded(sessionId: String?, rawReason: String) {
        CometChatLogger.d(TAG, "onIncomingCallEnded session=$sessionId reason=$rawReason")
        val reason = normaliseEndReason(rawReason) ?: return
        val currentCall = _call.value ?: return
        if (sessionId == null || sessionId != currentCall.sessionId) return
        if (_acceptedCall.value != null) return
        if (_rejectedCall.value != null) {
            // Closed already with no reason: the late push still names it (busy vs rejected).
            if (_endReason.value == null || _endReason.value == UIKitConstants.CallStatusConstants.ENDED) {
                _endReason.value = reason
            }
            return
        }

        stopRingTimeout()
        closeGraceJob?.cancel()
        _endReason.value = reason
        _rejectedCall.value = currentCall
        forgetOutgoing(currentCall)
    }

    private fun startRingTimeout() {
        stopRingTimeout()
        val timeout = ringTimeoutMs
        if (timeout <= 0) return
        // Counted from when the call was placed, so a screen that opens late (the app was in the
        // background) does not ring for longer than the callee does.
        val placed = _call.value?.sessionId?.let { CallingState.placedAt(it) }
        val elapsed = placed?.let { android.os.SystemClock.elapsedRealtime() - it }?.coerceAtLeast(0L) ?: 0L
        val remaining = (timeout - elapsed).coerceAtLeast(0L)
        ringTimeoutJob = viewModelScope.launch {
            delay(remaining)
            onRingTimeout()
        }
    }

    private fun stopRingTimeout() {
        ringTimeoutJob?.cancel()
        ringTimeoutJob = null
    }

    private fun stopCloseGrace() {
        closeGraceJob?.cancel()
        closeGraceJob = null
    }

    /**
     * Nobody answered within [ringTimeoutMs]: end the call as unanswered so the callee's
     * ring stops too, then close this screen.
     */
    private fun onRingTimeout() {
        CometChatLogger.d(TAG, "ring timeout after ${ringTimeoutMs}ms")
        val currentCall = _call.value ?: return
        if (_acceptedCall.value != null || _rejectedCall.value != null) return
        endRequested = true
        val sessionId = currentCall.sessionId
        if (!sessionId.isNullOrEmpty()) {
            CallInitiationUtils.endCall(sessionId, UIKitConstants.CallStatusConstants.UNANSWERED) { }
        }
        finishLocally(currentCall, UIKitConstants.CallStatusConstants.UNANSWERED)
    }

    /**
     * Closes the screen as [reason] exactly once. It is reached both from [cancelCapJob] on the
     * main thread and from the `endCall` callback on the Calls SDK thread, which can run at the
     * same time, so the "already closed" check and the close itself are one atomic step.
     */
    private fun finishLocally(call: Call, reason: String) {
        if (_rejectedCall.value != null) return
        cancelCapJob?.cancel()
        // Set before the rejected emission so observers of rejectedCall read the final reason.
        _endReason.value = reason
        if (!_rejectedCall.compareAndSet(null, call)) return
        forgetOutgoing(call)
        CometChatEvents.emitCallEvent(CometChatCallEvent.CallRejected(call))
    }

    /** The call is over: logout has nothing left to cancel. */
    private fun forgetOutgoing(call: Call) {
        if (CallingState.ringingOutgoingCall?.sessionId == call.sessionId) CallingState.ringingOutgoingCall = null
        call.sessionId?.let { CallingState.forgetPlaced(it) }
    }

    // ==================== Lifecycle ====================

    override fun onCleared() {
        super.onCleared()
        removeListeners()
        _call.value?.let { forgetOutgoing(it) }
    }
}
