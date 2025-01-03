package com.cometchat.chatuikit.shared.framework;

import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chatuikit.logger.CometChatLogger;

public abstract class ExtensionsDataSource {
    private static final String TAG = ExtensionsDataSource.class.getSimpleName();


    public abstract void addExtension();

    public abstract String getExtensionId();

    public void enable(CometChat.CallbackListener<Boolean> callbackListener) {
        checkExtensionAvailability(callbackListener);
    }

    public void enable() {
        checkExtensionAvailability(null);
    }

    private void checkExtensionAvailability(CometChat.CallbackListener<Boolean> listener) {
        CometChat.isExtensionEnabled(getExtensionId(), new CometChat.CallbackListener<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                if (listener != null) listener.onSuccess(aBoolean);
                if (aBoolean) addExtension();
            }

            @Override
            public void onError(CometChatException e) {
                if (listener != null) listener.onError(e);
                CometChatLogger.e(TAG, e.toString());
            }
        });
    }
}
