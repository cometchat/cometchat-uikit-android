package com.cometchat.uikit.core.viewmodel

import android.widget.RelativeLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.model.AudioMode
import com.cometchat.calls.model.SessionType
import com.cometchat.calls.listeners.ButtonClickListener
import com.cometchat.calls.listeners.SessionStatusListener
import com.cometchat.calls.listeners.ParticipantEventListener
import com.cometchat.calls.core.CallSession
import com.cometchat.calls.model.Participant
import com.cometchat.calls.core.SessionSettings
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.uikit.core.constants.UIKitConstants.CallWorkFlow
import com.cometchat.uikit.core.events.CometChatCallEvent
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.models.OngoingCallEvent
import com.cometchat.uikit.core.models.OngoingCallUIState
import com.cometchat.uikit.core.utils.CallingState
import com.cometchat.uikit.core.utils.CallsUtils
import com.cometchat.uikit.core.utils.CometChatLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for CometChatOngoingCall component.
 * Manages ongoing call state and actions for both Compose and XML Views implementations.
 *
 * This ViewModel:
 * - Exposes StateFlow<OngoingCallUIState> for the current UI state (Loading, Connected, Ended, Error)
 * - Exposes StateFlow<Boolean> for loading indicator visibility
 * - Exposes SharedFlow<OngoingCallEvent> for one-time events that should not replay on configuration changes
 * - Provides methods to configure and manage call sessions
 * - Handles call lifecycle events from CometChatCalls SDK
 *
 * Validates: Requirements 1.1, 1.2, 1.3
 */
open public class CometChatOngoingCallViewModel : ViewModel() {

    public companion object {
        private const val TAG = "CometChatOngoingCallViewModel"

        /** How often startCall checks that the session is joined before revealing the call view. */
        private const val JOIN_CHECK_INTERVAL_MS = 250L

        /** Reveals the call view after this long even if no join was seen, so the spinner never sticks. */
        private const val JOIN_REVEAL_TIMEOUT_MS = 10_000L
    }

    // ==================== State Flows ====================

    /**
     * StateFlow for the current UI state.
     * Represents Loading, Connected, Ended, or Error states.
     *
     * Validates: Requirement 1.1
     */
    private val _uiState = MutableStateFlow<OngoingCallUIState>(OngoingCallUIState.Loading)
    public val uiState: StateFlow<OngoingCallUIState> = _uiState.asStateFlow()

    /**
     * StateFlow for loading indicator visibility.
     * True when connecting to the call, false when connected or ended.
     *
     * Validates: Requirement 1.2
     */
    private val _isLoading = MutableStateFlow(true)
    public val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * SharedFlow for one-time events.
     * Emits CallEnded, SessionTimeout, UserJoined, UserLeft, and Error events.
     * Uses SharedFlow to prevent replay on configuration changes.
     *
     * Validates: Requirement 1.3
     */
    private val _events = MutableSharedFlow<OngoingCallEvent>()
    public val events: SharedFlow<OngoingCallEvent> = _events.asSharedFlow()

    // ==================== Internal State ====================

    /**
     * Unique listener ID for CometChatCalls event listeners.
     * Generated using timestamp when listeners are added.
     */
    internal var lifecycleOwner: LifecycleOwner? = null

    /**
     * Current call session ID.
     * Set via setSessionId() method.
     */
    internal var sessionId: String? = null

    /**
     * Current call type ("audio" or "video").
     * Set via setCallType() method.
     */
    internal var callType: String? = null

    /**
     * The call this session belongs to, when known (e.g. the incoming call the Push SDK
     * handed to the answer activity). Used to emit CometChatCallEvent.CallEnded.
     */
    private var call: Call? = null

    /**
     * Current call workflow.
     * DEFAULT for 1:1 calls, MEETING for group calls.
     * Defaults to DEFAULT.
     */
    internal var callWorkFlow: CallWorkFlow = CallWorkFlow.DEFAULT

    /**
     * Custom session settings builder provided by the user.
     * Used to configure session settings before joining the session.
     */
    internal var sessionSettingsBuilder: CometChatCalls.SessionSettingsBuilder? = null

    /**
     * Built session settings used for the current session.
     * Created from sessionSettingsBuilder with session type configuration applied.
     */
    internal var sessionSettings: SessionSettings? = null

    /**
     * The active CallSession instance returned by joinSession().
     * Used for session actions (leave(), etc.) and lifecycle-aware listeners.
     */
    internal var callSession: CallSession? = null

    // ==================== Protected Mutable State Accessors ====================

    /**
     * Protected accessor for mutable UI state.
     * Used by subclasses or internal methods to update state.
     */
    protected val mutableUiState: MutableStateFlow<OngoingCallUIState>
        get() = _uiState

    /**
     * Protected accessor for mutable loading state.
     * Used by subclasses or internal methods to update loading state.
     */
    protected val mutableIsLoading: MutableStateFlow<Boolean>
        get() = _isLoading

    /**
     * Protected accessor for mutable events flow.
     * Used by subclasses or internal methods to emit events.
     */
    protected val mutableEvents: MutableSharedFlow<OngoingCallEvent>
        get() = _events

    // ==================== Configuration Methods ====================

    /**
     * Sets the call session ID.
     * @param sessionId The unique identifier for the call session
     *
     * Validates: Requirement 2.5
     */
    public fun setSessionId(sessionId: String) {
        this.sessionId = sessionId
    }

    /**
     * Sets the call this session belongs to, so hanging up can emit
     * CometChatCallEvent.CallEnded with it.
     * @param call The call, or null when unknown
     */
    public fun setCall(call: Call?) {
        this.call = call
    }

    /**
     * Sets the call type.
     * @param callType The type of call ("audio" or "video")
     *
     * Validates: Requirement 2.6
     */
    public fun setCallType(callType: String) {
        this.callType = callType
    }

    /**
     * Sets the call workflow.
     * @param workFlow DEFAULT for 1:1 calls, MEETING for group calls
     *
     * Validates: Requirement 2.4
     */
    public fun setCallWorkFlow(workFlow: CallWorkFlow) {
        this.callWorkFlow = workFlow
    }

    /**
     * Sets the custom session settings builder.
     * @param builder The CometChatCalls.SessionSettingsBuilder to use for session configuration
     *
     * Validates: Requirement 2.3
     */
    public fun setSessionSettingsBuilder(builder: CometChatCalls.SessionSettingsBuilder?) {
        this.sessionSettingsBuilder = builder
    }

    // ==================== Call Methods ====================

    /**
     * Starts the call session with the configured session ID and call type.
     * 
     * Flow (v5):
     * 1. Set UI state to Loading, isLoading to true
     * 2. Build SessionSettings with session type based on callType
     * 3. Join session via CometChatCalls.joinSession() (handles token generation internally)
     * 4. On success: store CallSession, set isLoading false, UI state to Connected
     * 5. On failure: emit Error event
     *
     * @param callViewContainer The RelativeLayout container where the call UI will be rendered
     *
     * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 17.1, 17.2, 17.3
     */
    public fun startCall(callViewContainer: RelativeLayout) {
        // ENG-39311: the Calls SDK is compileOnly and may be absent at runtime. joinSession()
        // below would then throw NoClassDefFoundError. Bail out cleanly if the SDK isn't present.
        if (!CallsUtils.isCallsSDKAvailable()) {
            CometChatLogger.w(TAG, "Calls SDK is not on the classpath; cannot start the ongoing call.")
            return
        }

        // Validate required parameters
        val sid = sessionId ?: return
        val type = callType ?: return
        val builder = sessionSettingsBuilder ?: return

        // Set UI state to Loading, isLoading to true (Requirement 3.1)
        mutableUiState.value = OngoingCallUIState.Loading
        mutableIsLoading.value = true

        // Build SessionSettings with session type based on callType (Requirements 17.1, 17.2, 17.3)
        val sessionType = if (type.equals(CometChatConstants.CALL_TYPE_AUDIO, ignoreCase = true)) {
            SessionType.VOICE
        } else {
            SessionType.VIDEO
        }
        // A 1:1 call is a peer call, matching the prepared session: the Calls SDK leaves the
        // room when the other side leaves. A meeting is not. Set either way, since the builder
        // may be the app's shared one.
        sessionSettings = builder
            .setSessionType(sessionType)
            .setPeerCall(callWorkFlow == CallWorkFlow.DEFAULT)
            .build()

        val settings = sessionSettings ?: return

        // Join session — v5 handles token generation internally (Requirements 3.3, 3.4)
        CometChatCalls.joinSession(sid, settings, callViewContainer, object : CometChatCalls.CallbackListener<CallSession>() {
            override fun onSuccess(session: CallSession) {
                // Store the CallSession for actions and events
                callSession = session
                // joinSession succeeds before the call is joined and its view drawn; revealing
                // the call view here showed an empty, zero-width view for seconds (ENG-40740).
                // Reveal once the session is joined: onSessionJoined, or this check when the
                // status listener could not be registered before the join.
                viewModelScope.launch { revealWhenJoined(session, sid, type) }
            }

            override fun onError(e: com.cometchat.calls.exceptions.CometChatException) {
                // On failure: emit Error event (Requirement 3.7)
                CometChatLogger.e(TAG, "joinSession error: $e")
                val chatException = CometChatException(e.code, e.message)
                viewModelScope.launch {
                    mutableEvents.emit(OngoingCallEvent.Error(chatException))
                }
            }
        })
    }

    /**
     * Waits until [session] reports joined (at most [JOIN_REVEAL_TIMEOUT_MS]), then reveals the
     * call view. Stops early if the call has already been revealed, ended or failed.
     */
    private suspend fun revealWhenJoined(session: CallSession, sid: String, type: String) {
        var waited = 0L
        while (!session.isSessionJoined() && waited < JOIN_REVEAL_TIMEOUT_MS) {
            if (mutableUiState.value !is OngoingCallUIState.Loading) return
            delay(JOIN_CHECK_INTERVAL_MS)
            waited += JOIN_CHECK_INTERVAL_MS
        }
        markConnected(sid, type)
    }

    /** Moves Loading to Connected, once; later calls and any other state are left alone. */
    private fun markConnected(sid: String? = sessionId, type: String? = callType) {
        if (mutableUiState.value !is OngoingCallUIState.Loading) return
        if (sid == null || type == null) return
        mutableIsLoading.value = false
        mutableUiState.value = OngoingCallUIState.Connected(sid, type)
    }

    /**
     * Ends the current call with workflow-dependent cleanup.
     *
     * For DEFAULT workflow (1:1 calls):
     * 1. Leave the session via callSession.leave() (ringing v2; no Chat SDK endCall)
     * 2. Clear the UIKit's active call
     * 3. Set UI state to Ended, emit CallEnded event
     *
     * For MEETING workflow (group calls):
     * 1. Call callSession.leave() only
     * 2. Set CallingState.setIsActiveMeeting(false)
     * 3. Set UI state to Ended, emit CallEnded event
     *
     * Property 6: Workflow-Dependent Call End Behavior
     * Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7
     */
    public fun endCall() {
        // Ringing v2: hanging up leaves the media session. The Chat SDK's endCall and
        // clearActiveCall are deprecated and not used.
        callSession?.leave()
            ?: CallSession.getInstance()?.leave()
        onCallLeft()
    }

    /**
     * Local clean-up once this device has left the session: clears the UIKit's call state,
     * moves the UI to Ended and emits CallEnded. It does not leave the session itself, so it
     * is also what runs when the Calls SDK's own End button has already left.
     */
    private fun onCallLeft() {
        when (callWorkFlow) {
            CallWorkFlow.MEETING -> {
                CallingState.setIsActiveMeeting(false)
                mutableUiState.value = OngoingCallUIState.Ended
                viewModelScope.launch {
                    mutableEvents.emit(OngoingCallEvent.CallEnded)
                }
            }
            CallWorkFlow.DEFAULT -> {
                CallingState.setActiveCall(null)

                mutableUiState.value = OngoingCallUIState.Ended
                viewModelScope.launch {
                    mutableEvents.emit(OngoingCallEvent.CallEnded)
                }

                // Emit UIKit event for inter-component communication
                call?.let { endedCall ->
                    CometChatEvents.emitCallEvent(CometChatCallEvent.CallEnded(endedCall))
                }
                Unit
            }
        }
    }

    // ==================== Listener Methods ====================

    /**
     * Registers v5 lifecycle-aware call event listeners on the CallSession instance.
     * Uses SessionStatusListener, ParticipantEventListener, and ButtonClickListener.
     *
     * v5 listeners auto-remove on lifecycle owner destroy — no manual removal needed.
     *
     * Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 13.3, 13.4, 13.5
     */
    public fun addListeners(owner: LifecycleOwner? = null) {
        val session = callSession ?: CallSession.getInstance() ?: return
        // Use provided owner, stored owner, or skip registration.
        // Listeners will be registered when startCall() succeeds if no owner is available yet.
        val resolvedOwner: LifecycleOwner = owner ?: this.lifecycleOwner ?: return

        // 1. Session lifecycle listener
        session.addSessionStatusListener(resolvedOwner, object : SessionStatusListener() {
            override fun onSessionJoined() {
                // The call view is live: reveal it (ENG-40740).
                markConnected()
            }

            /**
             * Called when the session ends (remote party ended or server-side end).
             * For DEFAULT workflow: leave session + clear active call + exit
             * For MEETING workflow: do nothing
             *
             * Validates: Requirements 6.1, 6.2
             */
            override fun onSessionLeft() {
                when (callWorkFlow) {
                    CallWorkFlow.DEFAULT -> {
                        // Session already left — just clean up and transition to Ended
                        CallingState.setActiveCall(null)
                        mutableUiState.value = OngoingCallUIState.Ended
                        viewModelScope.launch {
                            mutableEvents.emit(OngoingCallEvent.CallEnded)
                        }
                    }
                    CallWorkFlow.MEETING -> {
                        // For MEETING: also transition to Ended so the screen closes
                        CallingState.setIsActiveMeeting(false)
                        mutableUiState.value = OngoingCallUIState.Ended
                        viewModelScope.launch {
                            mutableEvents.emit(OngoingCallEvent.CallEnded)
                        }
                    }
                }
            }

            /**
             * Called when the call session times out.
             * Leave session + emit SessionTimeout event + exit (same for both workflows)
             *
             * Validates: Requirement 6.5
             */
            override fun onSessionTimedOut() {
                CallingState.setIsActiveMeeting(false)
                // Session already timed out — no need to call leave()
                mutableUiState.value = OngoingCallUIState.Ended
                viewModelScope.launch {
                    mutableEvents.emit(OngoingCallEvent.SessionTimeout)
                }
            }

            override fun onConnectionLost() {
                CometChatLogger.w(TAG, "Call connection lost")
            }

            override fun onConnectionRestored() {
                CometChatLogger.d(TAG, "Call connection restored")
            }

            /**
             * The connection to the call closed without a session-left event: the join failed
             * (e.g. no network right after the device woke) or the room went away. The call is
             * over for this screen, as on Flutter; without this it stayed on "The session has
             * ended." with nothing to close it.
             */
            override fun onConnectionClosed() {
                CometChatLogger.d(TAG, "Call connection closed")
                if (mutableUiState.value is OngoingCallUIState.Ended) return
                when (callWorkFlow) {
                    CallWorkFlow.DEFAULT -> CallingState.setActiveCall(null)
                    CallWorkFlow.MEETING -> CallingState.setIsActiveMeeting(false)
                }
                mutableUiState.value = OngoingCallUIState.Ended
                viewModelScope.launch {
                    mutableEvents.emit(OngoingCallEvent.CallEnded)
                }
            }
        })

        // 2. Participant event listener
        session.addParticipantEventListener(resolvedOwner, object : ParticipantEventListener() {
            /**
             * Called when a participant joins the call.
             * Emits UserJoined event with isCurrentUser detection.
             *
             * Validates: Requirements 6.6, 18.2
             */
            override fun onParticipantJoined(participant: Participant) {
                val currentUserId = CometChat.getLoggedInUser()?.uid
                val isCurrentUser = participant.uid == currentUserId
                viewModelScope.launch {
                    mutableEvents.emit(OngoingCallEvent.UserJoined(participant.uid, isCurrentUser))
                }
            }

            /**
             * Called when a participant leaves the call.
             * Emits UserLeft event with the user ID.
             *
             * Validates: Requirement 6.7
             */
            override fun onParticipantLeft(participant: Participant) {
                viewModelScope.launch {
                    mutableEvents.emit(OngoingCallEvent.UserLeft(participant.uid))
                }
            }

            override fun onParticipantListChanged(participants: List<Participant>) {
                // No action required per design
            }

            override fun onParticipantAudioMuted(participant: Participant) {
                // No action required per design
            }

            override fun onParticipantAudioUnmuted(participant: Participant) {
                // No action required per design
            }

            override fun onParticipantVideoPaused(participant: Participant) {
                // No action required per design
            }

            override fun onParticipantVideoResumed(participant: Participant) {
                // No action required per design
            }

            override fun onParticipantHandRaised(participant: Participant) {
                // No action required per design
            }

            override fun onParticipantHandLowered(participant: Participant) {
                // No action required per design
            }

            override fun onParticipantStartedRecording(participant: Participant) {
                // No action required per design
            }

            override fun onParticipantStoppedRecording(participant: Participant) {
                // No action required per design
            }

            override fun onDominantSpeakerChanged(participant: Participant) {
                // No action required per design
            }
        })

        // 3. Button click listener
        session.addButtonClickListener(resolvedOwner, object : ButtonClickListener() {
            /**
             * Called when the user presses the leave session button in the SDK UI. The Calls
             * SDK has already left the session before notifying (its button runs
             * leave() first), so only the local clean-up runs here.
             *
             * Validates: Requirements 6.3, 6.4
             */
            override fun onLeaveSessionButtonClicked() {
                onCallLeft()
            }

            override fun onToggleAudioButtonClicked() {
                // No action required per design
            }

            override fun onToggleVideoButtonClicked() {
                // No action required per design
            }

            override fun onSwitchCameraButtonClicked() {
                // No action required per design
            }

            override fun onRaiseHandButtonClicked() {
                // No action required per design
            }

            override fun onRecordingToggleButtonClicked() {
                // No action required per design
            }
        })
    }

    /**
     * No-op in v5 — lifecycle-aware listeners auto-remove on destroy.
     * Kept for API compatibility.
     *
     * Validates: Requirement 13.5
     */
    public fun removeListeners() {
        // v5 listeners are lifecycle-aware and auto-cleanup — no manual removal needed
    }

    // ==================== Lifecycle Methods ====================

    /**
     * Called when the ViewModel is cleared.
     * Removes all call event listeners to prevent memory leaks.
     *
     * Validates: Requirement 1.6
     */
    override fun onCleared() {
        super.onCleared()
        removeListeners()
    }
}
