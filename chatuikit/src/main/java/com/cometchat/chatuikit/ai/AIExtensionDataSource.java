package com.cometchat.chatuikit.ai;

import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chatuikit.shared.framework.ExtensionsDataSource;

public abstract class AIExtensionDataSource extends ExtensionsDataSource {
    private static final String TAG = AIExtensionDataSource.class.getSimpleName();

    @Override
    public void enable() {
        CometChat.isAIFeatureEnabled(getExtensionId(), new CometChat.CallbackListener<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                if (aBoolean) {
                    addExtension();
                }
            }

            @Override
            public void onError(CometChatException e) {
            }
        });
    }
}
