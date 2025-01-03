package com.cometchat.chatuikit.calls;

import com.cometchat.calls.core.CometChatCalls;
import com.cometchat.chatuikit.calls.callbutton.CallButtonsConfiguration;
import com.cometchat.chatuikit.calls.outgoingcall.OutgoingCallConfiguration;

public class CallingConfiguration {
    private static final String TAG = CallingConfiguration.class.getSimpleName();
    private OutgoingCallConfiguration outgoingCallConfiguration;
    private CallButtonsConfiguration callButtonsConfiguration;
    private CometChatCalls.CallSettingsBuilder groupCallSettingsBuilder;

    public CallingConfiguration setOutgoingCallConfiguration(OutgoingCallConfiguration outgoingCallConfiguration) {
        this.outgoingCallConfiguration = outgoingCallConfiguration;
        return this;
    }

    public CallingConfiguration setCallButtonsConfiguration(CallButtonsConfiguration callButtonsConfiguration) {
        this.callButtonsConfiguration = callButtonsConfiguration;
        return this;
    }

    public CallingConfiguration setGroupCallSettingsBuilder(CometChatCalls.CallSettingsBuilder groupCallSettingsBuilder) {
        this.groupCallSettingsBuilder = groupCallSettingsBuilder;
        return this;
    }

    public OutgoingCallConfiguration getOutgoingCallConfiguration() {
        return outgoingCallConfiguration;
    }

    public CallButtonsConfiguration getCallButtonsConfiguration() {
        return callButtonsConfiguration;
    }

    public CometChatCalls.CallSettingsBuilder getGroupCallSettingsBuilder() {
        return groupCallSettingsBuilder;
    }
}
