package com.cometchat.chatuikit.calls.incomingcall;

import androidx.annotation.RawRes;
import androidx.annotation.StyleRes;

import com.cometchat.calls.core.CometChatCalls;
import com.cometchat.chatuikit.shared.interfaces.OnClick;
import com.cometchat.chatuikit.shared.interfaces.OnError;

public class IncomingCallConfiguration {
    private static final String TAG = IncomingCallConfiguration.class.getSimpleName();
    private OnClick onDeclineButtonClick, onAcceptButtonClick;
    private @StyleRes int incomingCallStyle;
    private boolean disableSoundForCalls;
    private @RawRes int customSoundForCalls;
    private OnError onError;
    private CometChatCalls.CallSettingsBuilder callSettingsBuilder;

    public IncomingCallConfiguration setOnDeclineButtonClick(OnClick onDeclineButtonClick) {
        this.onDeclineButtonClick = onDeclineButtonClick;
        return this;
    }

    public IncomingCallConfiguration setIncomingCallStyle(@StyleRes int incomingCallStyle) {
        this.incomingCallStyle = incomingCallStyle;
        return this;
    }

    public IncomingCallConfiguration setDisableSoundForCalls(boolean disableSoundForCalls) {
        this.disableSoundForCalls = disableSoundForCalls;
        return this;
    }

    public IncomingCallConfiguration setCustomSoundForCalls(int customSoundForCalls) {
        this.customSoundForCalls = customSoundForCalls;
        return this;
    }

    public IncomingCallConfiguration setOnError(OnError onError) {
        this.onError = onError;
        return this;
    }

    public IncomingCallConfiguration setCallSettingBuilder(CometChatCalls.CallSettingsBuilder callSettingsBuilder) {
        this.callSettingsBuilder = callSettingsBuilder;
        return this;
    }

    public IncomingCallConfiguration setOnAcceptButtonClick(OnClick onAcceptButtonClick) {
        this.onAcceptButtonClick = onAcceptButtonClick;
        return this;
    }

    public OnClick getOnDeclineButtonClick() {
        return onDeclineButtonClick;
    }

    public @StyleRes int getIncomingCallStyle() {
        return incomingCallStyle;
    }

    public boolean isDisableSoundForCalls() {
        return disableSoundForCalls;
    }

    public int getCustomSoundForCalls() {
        return customSoundForCalls;
    }

    public OnError getOnError() {
        return onError;
    }

    public CometChatCalls.CallSettingsBuilder getCallSettingsBuilder() {
        return callSettingsBuilder;
    }

    public OnClick getOnAcceptButtonClick() {
        return onAcceptButtonClick;
    }
}
