package com.cometchat.chatuikit.calls.callbutton;

import com.cometchat.calls.core.CometChatCalls;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.calls.outgoingcall.OutgoingCallConfiguration;
import com.cometchat.chatuikit.shared.interfaces.Function3;

/**
 * Configuration class for customizing the appearance and behavior of the call
 * buttons.
 *
 * <p>
 * Use this class to set various options and styles for the call buttons.
 */
public class CallButtonsConfiguration {
    private static final String TAG = CallButtonsConfiguration.class.getSimpleName();
    private CometChatCallButtons.OnClick onVideoCallClick, onVoiceCallClick;
    private OutgoingCallConfiguration outgoingCallConfiguration;
    private Function3<User, Group, Boolean, CometChatCalls.CallSettingsBuilder> callSettingsBuilder;

    public CallButtonsConfiguration setCallSettingsBuilder(Function3<User, Group, Boolean, CometChatCalls.CallSettingsBuilder> callSettingsBuilder) {
        this.callSettingsBuilder = callSettingsBuilder;
        return this;
    }

    /**
     * Sets the click listener for the video call button.
     *
     * @param onVideoCallClick The callback to be invoked when the video call button is clicked.
     * @return The updated CallButtonsConfiguration object.
     */
    public CallButtonsConfiguration setOnVideoCallClick(CometChatCallButtons.OnClick onVideoCallClick) {
        this.onVideoCallClick = onVideoCallClick;
        return this;
    }

    /**
     * Sets the click listener for the voice call button.
     *
     * @param onVoiceCallClick The callback to be invoked when the voice call button is clicked.
     * @return The updated CallButtonsConfiguration object.
     */
    public CallButtonsConfiguration setOnVoiceCallClick(CometChatCallButtons.OnClick onVoiceCallClick) {
        this.onVoiceCallClick = onVoiceCallClick;
        return this;
    }

    public CallButtonsConfiguration setOutgoingCallConfiguration(OutgoingCallConfiguration outgoingCallConfiguration) {
        this.outgoingCallConfiguration = outgoingCallConfiguration;
        return this;
    }

    public OutgoingCallConfiguration getOutgoingCallConfiguration() {
        return outgoingCallConfiguration;
    }

    public CometChatCallButtons.OnClick getOnVideoCallClick() {
        return onVideoCallClick;
    }

    public CometChatCallButtons.OnClick getOnVoiceCallClick() {
        return onVoiceCallClick;
    }

    public Function3<User, Group, Boolean, CometChatCalls.CallSettingsBuilder> getCallSettingsBuilder() {
        return callSettingsBuilder;
    }
}
