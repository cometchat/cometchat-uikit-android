package com.cometchat.chatuikit.calls;

import androidx.lifecycle.ViewModel;

import com.cometchat.calls.core.CometChatCalls;
import com.cometchat.chat.core.Call;
import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.calls.outgoingcall.OutgoingCallConfiguration;

public class CallActivityViewModel extends ViewModel {
    private static final String TAG = CallActivityViewModel.class.getSimpleName();
    private BaseMessage baseMessage;
    private Call call;
    private User user;
    private OutgoingCallConfiguration outgoingCallConfiguration;
    private CometChatCalls.CallSettingsBuilder onGoingCallSettingsBuilder;
    private String callingType;

    public BaseMessage getBaseMessage() {
        return baseMessage;
    }

    public void setBaseMessage(BaseMessage baseMessage) {
        this.baseMessage = baseMessage;
    }

    public Call getCall() {
        return call;
    }

    public void setCall(Call call) {
        this.call = call;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public OutgoingCallConfiguration getOutgoingCallConfiguration() {
        return outgoingCallConfiguration;
    }

    public void setOutgoingCallConfiguration(OutgoingCallConfiguration outgoingCallConfiguration) {
        this.outgoingCallConfiguration = outgoingCallConfiguration;
    }

    public CometChatCalls.CallSettingsBuilder getOnGoingCallSettingsBuilder() {
        return onGoingCallSettingsBuilder;
    }

    public void setOnGoingCallSettingsBuilder(CometChatCalls.CallSettingsBuilder onGoingCallSettingsBuilder) {
        this.onGoingCallSettingsBuilder = onGoingCallSettingsBuilder;
    }

    public String getCallingType() {
        return callingType;
    }

    public void setCallingType(String callingType) {
        this.callingType = callingType;
    }
}
