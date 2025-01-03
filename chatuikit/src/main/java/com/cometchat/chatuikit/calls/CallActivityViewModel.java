package com.cometchat.chatuikit.calls;

import androidx.lifecycle.ViewModel;

import com.cometchat.calls.core.CometChatCalls;
import com.cometchat.chat.core.Call;
import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.calls.incomingcall.IncomingCallConfiguration;
import com.cometchat.chatuikit.calls.outgoingcall.OutgoingCallConfiguration;

public class CallActivityViewModel extends ViewModel {
    private static final String TAG = CallActivityViewModel.class.getSimpleName();
    private BaseMessage baseMessage;
    private Call call;
    private User user;
    private OutgoingCallConfiguration outgoingCallConfiguration;
    private IncomingCallConfiguration incomingCallConfiguration;
    private CometChatCalls.CallSettingsBuilder onGoingCallSettingsBuilder;
    private String callingType;

    public void setBaseMessage(BaseMessage baseMessage) {
        this.baseMessage = baseMessage;
    }

    public void setCall(Call call) {
        this.call = call;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setOutgoingCallConfiguration(OutgoingCallConfiguration outgoingCallConfiguration) {
        this.outgoingCallConfiguration = outgoingCallConfiguration;
    }

    public void setIncomingCallConfiguration(IncomingCallConfiguration incomingCallConfiguration) {
        this.incomingCallConfiguration = incomingCallConfiguration;
    }

    public void setOnGoingCallSettingsBuilder(CometChatCalls.CallSettingsBuilder onGoingCallSettingsBuilder) {
        this.onGoingCallSettingsBuilder = onGoingCallSettingsBuilder;
    }

    public void setCallingType(String callingType) {
        this.callingType = callingType;
    }

    public BaseMessage getBaseMessage() {
        return baseMessage;
    }

    public Call getCall() {
        return call;
    }

    public User getUser() {
        return user;
    }

    public OutgoingCallConfiguration getOutgoingCallConfiguration() {
        return outgoingCallConfiguration;
    }

    public IncomingCallConfiguration getIncomingCallConfiguration() {
        return incomingCallConfiguration;
    }

    public CometChatCalls.CallSettingsBuilder getOnGoingCallSettingsBuilder() {
        return onGoingCallSettingsBuilder;
    }

    public String getCallingType() {
        return callingType;
    }
}
