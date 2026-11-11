package com.cometchat.uikit.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.model.InitiatedCall
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.domain.usecase.InitiateUserCallUseCase
import com.cometchat.uikit.core.domain.usecase.StartGroupCallUseCase
import com.cometchat.uikit.core.events.CometChatCallEvent
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.state.CallButtonsEvent
import com.cometchat.uikit.core.state.CallButtonsUIState
import com.cometchat.uikit.core.utils.CallInitiationUtils
import com.cometchat.uikit.core.utils.CallsUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for CometChatCallButtons component.
 * Manages call initiation state and events for both user and group calls.
 * Shared between Jetpack Compose and XML Views implementations.
 */
open public class CometChatCallButtonsViewModel(
    private val initiateUserCallUseCase: InitiateUserCallUseCase,
    private val startGroupCallUseCase: StartGroupCallUseCase,
    private val enableListeners: Boolean = true
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<CallButtonsUIState>(CallButtonsUIState.Idle)
    public val uiState: StateFlow<CallButtonsUIState> = _uiState.asStateFlow()

    // One-time events
    private val _events = MutableSharedFlow<CallButtonsEvent>()
    public val events: SharedFlow<CallButtonsEvent> = _events.asSharedFlow()

    // Error events for UI callback
    private val _errorEvent = MutableSharedFlow<CometChatException>()
    public val errorEvent: SharedFlow<CometChatException> = _errorEvent.asSharedFlow()

    // Current receiver state
    private var user: User? = null
    private var group: Group? = null
    private var receiverId: String = ""
    private var receiverType: String = ""

    // Listener tag
    private var listenersTag: String? = null

    // Local event listener jobs
    private var callEventsJob: Job? = null

    init {
        if (enableListeners) {
            addListeners()
        }
    }

    /**
     * Sets the user for 1-to-1 calls.
     * @param user The User to call
     */
    public fun setUser(user: User) {
        this.user = user
        this.group = null
        this.receiverId = user.uid
        this.receiverType = UIKitConstants.ReceiverType.USER
    }

    /**
     * Sets the group for conference calls.
     * @param group The Group to start a call with
     */
    public fun setGroup(group: Group) {
        this.group = group
        this.user = null
        this.receiverId = group.guid
        this.receiverType = UIKitConstants.ReceiverType.GROUP
    }

    /**
     * Gets the currently set user.
     * @return The User or null if not set
     */
    public fun getUser(): User? = user

    /**
     * Gets the currently set group.
     * @return The Group or null if not set
     */
    public fun getGroup(): Group? = group

    /**
     * Initiates a call with the configured receiver.
     * @param callType The type of call (audio/video) - use CometChatConstants.CALL_TYPE_AUDIO or CALL_TYPE_VIDEO
     */
    public fun initiateCall(callType: String) {
        // A second tap while the first call is still being started does nothing (SM-07).
        if (_uiState.value is CallButtonsUIState.Initiating) return
        viewModelScope.launch {
            _uiState.value = CallButtonsUIState.Initiating

            when (receiverType) {
                UIKitConstants.ReceiverType.USER -> initiateUserCall(callType)
                UIKitConstants.ReceiverType.GROUP -> initiateGroupCall(callType)
                else -> {
                    val exception = CometChatException(
                        "INVALID_RECEIVER",
                        "No user or group set",
                        "Please set a user or group before initiating a call"
                    )
                    _uiState.value = CallButtonsUIState.Error(exception)
                    _errorEvent.emit(exception)
                }
            }
        }
    }

    private suspend fun initiateUserCall(callType: String) {
        initiateUserCallUseCase(receiverId, callType, user)
            .onSuccess { call ->
                _uiState.value = CallButtonsUIState.Idle
                _events.emit(CallButtonsEvent.CallInitiated(call))
                CometChatEvents.emitCallEvent(CometChatCallEvent.OutgoingCall(call))
            }
            .onFailure { e ->
                // Another entry point is already starting a call; that one reports its result.
                // Back to Idle, or the Initiating guard would ignore every later tap.
                if (CallInitiationUtils.isCallInProgress(e)) {
                    _uiState.value = CallButtonsUIState.Idle
                    return@onFailure
                }
                val exception = e as? CometChatException ?: CometChatException(
                    "UNKNOWN_ERROR",
                    e.message ?: "Unknown error",
                    e.message ?: "An unknown error occurred"
                )
                _uiState.value = CallButtonsUIState.Error(exception)
                _errorEvent.emit(exception)
            }
    }

    private suspend fun initiateGroupCall(callType: String) {
        startGroupCallUseCase(receiverId, callType)
            .onSuccess { message ->
                _uiState.value = CallButtonsUIState.Idle
                _events.emit(CallButtonsEvent.StartDirectCall(message))
            }
            .onFailure { e ->
                val exception = e as? CometChatException ?: CometChatException(
                    "UNKNOWN_ERROR",
                    e.message ?: "Unknown error",
                    e.message ?: "An unknown error occurred"
                )
                _uiState.value = CallButtonsUIState.Error(exception)
                _errorEvent.emit(exception)
            }
    }

    private fun addListeners() {
        listenersTag = "CallButtons_${System.currentTimeMillis()}"

        listenersTag?.let { tag ->
            // Calls SDK listener (ringing v2): a call ending anywhere (declined, busy,
            // unanswered, cancelled, ended) re-enables the buttons. The Chat SDK's
            // CallListener is deprecated and not used.
            if (CallsUtils.isCallsSDKAvailable()) {
                CometChatCalls.addIncomingCallListener(tag, object : CometChatCalls.IncomingCallListener() {
                    override fun onIncomingCallReceived(call: InitiatedCall) {
                        // Not handled by this component
                    }

                    override fun onIncomingCallEnded(call: InitiatedCall, reason: String) {
                        viewModelScope.launch {
                            _events.emit(CallButtonsEvent.CallRejected(CallInitiationUtils.toCall(call)))
                        }
                    }
                })
            }

            // UIKit Local Events
            callEventsJob = viewModelScope.launch {
                CometChatEvents.callEvents.collect { event ->
                    when (event) {
                        is CometChatCallEvent.CallRejected -> {
                            _events.emit(CallButtonsEvent.CallRejected(event.call))
                        }
                        is CometChatCallEvent.CallEnded -> {
                            _events.emit(CallButtonsEvent.CallRejected(event.call))
                        }
                        else -> { /* Not handled */ }
                    }
                }
            }
        }
    }

    /**
     * Removes all listeners. Called automatically in onCleared().
     */
    public fun removeListeners() {
        listenersTag?.let { tag ->
            if (CallsUtils.isCallsSDKAvailable()) {
                CometChatCalls.removeIncomingCallListener(tag)
            }
        }
        callEventsJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        removeListeners()
    }
}
