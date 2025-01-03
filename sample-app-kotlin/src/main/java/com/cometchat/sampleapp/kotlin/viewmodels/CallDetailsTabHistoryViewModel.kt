package com.cometchat.sampleapp.kotlin.viewmodels

import com.cometchat.calls.constants.CometChatCallsConstants
import com.cometchat.calls.core.CallLogRequest.CallLogRequestBuilder
import com.cometchat.calls.model.CallLog
import com.cometchat.calls.model.CallUser
import com.cometchat.chatuikit.calls.calllogs.CallLogsViewModel
import com.cometchat.chatuikit.calls.utils.CallUtils

class CallDetailsTabHistoryViewModel : CallLogsViewModel() {
    fun setCallLog(callLog: CallLog) {
        val initiator = callLog.initiator as CallUser
        val isLoggedInUser = CallUtils.isLoggedInUser(initiator)
        val user = if (isLoggedInUser) {
            callLog.receiver as CallUser
        } else {
            initiator
        }
        val callLogRequestBuilder = CallLogRequestBuilder().setUid(user.uid).setLimit(30).setCallCategory(CometChatCallsConstants.CALL_CATEGORY_CALL)
        super.setCallLogRequestBuilder(callLogRequestBuilder)
    }
}
