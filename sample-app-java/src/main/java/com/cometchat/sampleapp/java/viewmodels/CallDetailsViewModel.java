package com.cometchat.sampleapp.java.viewmodels;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cometchat.calls.model.CallLog;
import com.cometchat.calls.model.CallUser;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.calls.utils.CallUtils;
import com.cometchat.chatuikit.logger.CometChatLogger;

import java.util.Locale;

public class CallDetailsViewModel extends ViewModel {
    private static final String TAG = CallDetailsViewModel.class.getSimpleName();

    private final MutableLiveData<CallLog> callLogLiveData;
    private final MutableLiveData<User> receiverUserLiveData;
    private final MutableLiveData<String> callDuration;

    public CallDetailsViewModel() {
        callLogLiveData = new MutableLiveData<>();
        receiverUserLiveData = new MutableLiveData<>();
        callDuration = new MutableLiveData<>();
    }

    public MutableLiveData<CallLog> getCallLog() {
        return callLogLiveData;
    }

    public void setCallLog(CallLog callLog) {
        this.callLogLiveData.setValue(callLog);
        setCallDuration();
        if (callLogLiveData.getValue() != null) {
            CallUser initiator = (CallUser) callLogLiveData.getValue().getInitiator();
            boolean isLoggedInUser = CallUtils.isLoggedInUser(initiator);
            CallUser user;
            if (isLoggedInUser) {
                user = (CallUser) callLog.getReceiver();
            } else {
                user = initiator;
            }
            if (user != null) {
                CometChat.getUser(user.getUid(), new CometChat.CallbackListener<User>() {
                    @Override
                    public void onSuccess(User user) {
                        receiverUserLiveData.setValue(user);
                    }

                    @Override
                    public void onError(CometChatException e) {
                        Log.e(TAG, "Error: " + e);
                    }
                });
            }
        } else {
            CometChatLogger.e(TAG, "CallLog is null");
        }
    }

    private void setCallDuration() {
        int minutes = 0, seconds = 0;
        if (callLogLiveData != null && callLogLiveData.getValue() != null) {
            double decimalValue = callLogLiveData.getValue().getTotalDurationInMinutes();
            minutes = (int) decimalValue;
            seconds = (int) ((decimalValue - minutes) * 60);
        }
        callDuration.setValue(String.format(Locale.US, "%dm %ds", minutes, seconds));
    }

    public MutableLiveData<User> getReceiverUser() {
        return receiverUserLiveData;
    }

    public MutableLiveData<String> getCallDuration() {
        return callDuration;
    }
}
