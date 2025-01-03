package com.cometchat.chatuikit.calls;

import android.content.Context;

import com.cometchat.calls.core.CallAppSettings;
import com.cometchat.calls.core.CometChatCalls;
import com.cometchat.chat.core.Call;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.framework.ChatConfigurator;
import com.cometchat.chatuikit.shared.framework.ExtensionsDataSource;

public class CallingExtension extends ExtensionsDataSource {
    private static final String TAG = CallingExtension.class.getSimpleName();
    private static Call tempCall;
    private static boolean isActiveMeeting;
    private Context context;
    private CallingConfiguration callingConfiguration;

    public CallingExtension(Context context) {
        this.context = context;
    }

    public static Call getActiveCall() {
        return tempCall;
    }

    public static void setActiveCall(Call call) {
        tempCall = call;
    }    @Override
    public void addExtension() {
        ChatConfigurator.enable(var1 -> new CallingExtensionDecorator(var1, callingConfiguration));
    }

    public static void setIsActiveMeeting(boolean isActive) {
        isActiveMeeting = isActive;
    }

    public static boolean isActiveMeeting() {
        return isActiveMeeting;
    }

    public void setCallingConfiguration(CallingConfiguration callingConfiguration) {
        this.callingConfiguration = callingConfiguration;
    }    @Override
    public String getExtensionId() {
        return null;
    }



    @Override
    public void enable() {
        if (context != null && CometChatUIKit.getAuthSettings() != null) {
            CallAppSettings callAppSettings = new CallAppSettings.CallAppSettingBuilder()
                .setAppId(CometChatUIKit.getAuthSettings().getAppId())
                .setRegion(CometChatUIKit.getAuthSettings().getRegion())
                .build();
            CometChatCalls.init(context, callAppSettings, new CometChatCalls.CallbackListener<String>() {
                @Override
                public void onSuccess(String s) {
                    addExtension();
                    context = null;
                }

                @Override
                public void onError(com.cometchat.calls.exceptions.CometChatException e) {
                }
            });
        }
    }



}
