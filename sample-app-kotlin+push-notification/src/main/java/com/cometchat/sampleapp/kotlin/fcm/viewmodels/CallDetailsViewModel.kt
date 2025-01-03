package com.cometchat.sampleapp.kotlin.fcm.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cometchat.calls.model.CallLog
import com.cometchat.calls.model.CallUser
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.calls.utils.CallUtils
import java.util.Locale

class CallDetailsViewModel : ViewModel() {
    val callLog: MutableLiveData<CallLog?> = MutableLiveData()
    val receiverUser: MutableLiveData<User> = MutableLiveData()
    val callDuration: MutableLiveData<String> = MutableLiveData()

    fun setCallLog(callLog: CallLog) {
        this.callLog.value = callLog
        setCallDuration()
        val initiator = callLog.initiator as CallUser
        val isLoggedInUser = CallUtils.isLoggedInUser(initiator)
        val user = if (isLoggedInUser) {
            callLog.receiver as CallUser
        } else {
            initiator
        }
        CometChat.getUser(user.uid, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                receiverUser.value = user
            }

            override fun onError(e: CometChatException) {
                Log.e(
                    TAG, "Error: $e"
                )
            }
        })
    }

    private fun setCallDuration() {
        var minutes = 0
        var seconds = 0
        if (callLog.value != null) {
            val decimalValue = callLog.value!!.totalDurationInMinutes
            minutes = decimalValue.toInt()
            seconds = ((decimalValue - minutes) * 60).toInt()
        }
        callDuration.value = String.format(Locale.US, "%dm %ds", minutes, seconds)
    }

    companion object {
        private val TAG: String = CallDetailsViewModel::class.java.simpleName
    }
}
