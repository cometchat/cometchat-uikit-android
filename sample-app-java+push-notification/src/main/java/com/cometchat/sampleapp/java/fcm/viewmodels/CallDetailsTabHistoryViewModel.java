package com.cometchat.sampleapp.java.fcm.viewmodels;

import com.cometchat.calls.constants.CometChatCallsConstants;
import com.cometchat.calls.core.CallLogRequest;
import com.cometchat.calls.model.CallLog;
import com.cometchat.calls.model.CallUser;
import com.cometchat.chatuikit.calls.calllogs.CallLogsViewModel;
import com.cometchat.chatuikit.calls.utils.CallUtils;

public class CallDetailsTabHistoryViewModel extends CallLogsViewModel {

    public void setCallLog(CallLog callLog) {
        CallUser initiator = (CallUser) callLog.getInitiator();
        boolean isLoggedInUser = CallUtils.isLoggedInUser(initiator);
        CallUser user;
        if (isLoggedInUser) {
            user = (CallUser) callLog.getReceiver();
        } else {
            user = initiator;
        }
        CallLogRequest.CallLogRequestBuilder callLogRequestBuilder = new CallLogRequest.CallLogRequestBuilder()
            .setUid(user.getUid())
            .setLimit(30)
            .setCallCategory(CometChatCallsConstants.CALL_CATEGORY_CALL);
        super.setCallLogRequestBuilder(callLogRequestBuilder);
    }
}
