package com.cometchat.sampleapp.kotlin.push.appflow.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.calls.model.CallLog
import com.cometchat.calls.model.CallUser
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.uikit.core.utils.CallInitiationUtils
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import kotlinx.coroutines.launch

/**
 * ViewModel for CallsFragment handling call initiation logic.
 */
class CallsFragmentViewModel : ViewModel() {
    
    private val onCallStart = MutableLiveData<Call>()
    private val onError = MutableLiveData<CometChatException>()

    fun onCallStart(): MutableLiveData<Call> {
        return onCallStart
    }

    fun onError(): MutableLiveData<CometChatException> {
        return onError
    }

    /**
     * Starts a call based on the call log.
     * Checks if the user is blocked before initiating the call.
     */
    fun startCall(
        callType: String,
        callLog: CallLog,
        listener: CometChat.CallbackListener<Void>
    ) {
        val initiator = callLog.initiator as CallUser
        val loggedInUser = CometChat.getLoggedInUser()
        val isLoggedInUser = initiator.uid == loggedInUser?.uid
        
        val user = if (isLoggedInUser) {
            callLog.receiver as CallUser
        } else {
            initiator
        }
        
        CometChat.getUser(user.uid, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(userObj: User) {
                if (userObj.isBlockedByMe) {
                    val exception = CometChatException("BLOCKED_BY_ME", "Call cannot be initiated as user is blocked")
                    listener.onError(exception)
                    onError.value = exception
                } else if (userObj.isHasBlockedMe) {
                    val exception = CometChatException("BLOCKED_BY_ME", "Call cannot be initiated as user has blocked you")
                    listener.onError(exception)
                    onError.value = exception
                } else {
                    startCall(callType, userObj, listener)
                }
            }

            override fun onError(e: CometChatException) {
                listener.onError(e)
                onError.value = e
            }
        })
    }

    private fun startCall(
        callType: String,
        user: User,
        listener: CometChat.CallbackListener<Void>
    ) {
        // Ringing v2: the Calls SDK creates the call and the backend rings the callee by push.
        viewModelScope.launch {
            CallInitiationUtils.initiateUserCall(user.uid, callType, user)
                .onSuccess { call ->
                    listener.onSuccess(null)
                    onCallStart.value = call
                }
                .onFailure { e ->
                    // A second tap while the first call is still being started: nothing to show.
                    if (CallInitiationUtils.isCallInProgress(e)) return@onFailure
                    val exception = e as? CometChatException ?: CometChatException("ERROR", e.message ?: "Call failed")
                    listener.onError(exception)
                    onError.value = exception
                }
        }
    }
}
