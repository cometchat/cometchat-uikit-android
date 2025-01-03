package com.cometchat.chatuikit.calls.outgoingcall;

import android.graphics.drawable.Drawable;

import androidx.annotation.RawRes;
import androidx.annotation.StyleRes;

import com.cometchat.calls.core.CometChatCalls;
import com.cometchat.chatuikit.shared.interfaces.OnClick;
import com.cometchat.chatuikit.shared.interfaces.OnError;

public class OutgoingCallConfiguration {
    private static final String TAG = OutgoingCallConfiguration.class.getSimpleName();
    private Drawable declineButtonIcon;
    private OnClick onDeclineButtonClick;
    private @StyleRes int outgoingCallStyle;
    private boolean disableSoundForCalls;
    private @RawRes int customSoundForCalls;
    private OnError onError;
    private CometChatCalls.CallSettingsBuilder callSettingsBuilder;

    public OutgoingCallConfiguration setDeclineButtonIcon(Drawable declineButtonIcon) {
        this.declineButtonIcon = declineButtonIcon;
        return this;
    }

    public OutgoingCallConfiguration setOnDeclineButtonClick(OnClick onDeclineButtonClick) {
        this.onDeclineButtonClick = onDeclineButtonClick;
        return this;
    }

    public OutgoingCallConfiguration setOutgoingCallStyle(@StyleRes int outgoingCallStyle) {
        this.outgoingCallStyle = outgoingCallStyle;
        return this;
    }

    public OutgoingCallConfiguration setDisableSoundForCalls(boolean disableSoundForCalls) {
        this.disableSoundForCalls = disableSoundForCalls;
        return this;
    }

    public OutgoingCallConfiguration setCustomSoundForCalls(int customSoundForCalls) {
        this.customSoundForCalls = customSoundForCalls;
        return this;
    }

    public OutgoingCallConfiguration setOnError(OnError onError) {
        this.onError = onError;
        return this;
    }

    public OutgoingCallConfiguration setCallSettingsBuilder(CometChatCalls.CallSettingsBuilder callSettingsBuilder) {
        this.callSettingsBuilder = callSettingsBuilder;
        return this;
    }

    public Drawable getDeclineButtonIcon() {
        return declineButtonIcon;
    }

    public OnClick getOnDeclineButtonClick() {
        return onDeclineButtonClick;
    }

    public @StyleRes int getOutgoingCallStyle() {
        return outgoingCallStyle;
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

    public CometChatCalls.CallSettingsBuilder getCallSettingBuilder() {
        return callSettingsBuilder;
    }
}
