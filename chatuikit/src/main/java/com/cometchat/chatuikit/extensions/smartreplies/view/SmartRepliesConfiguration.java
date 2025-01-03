package com.cometchat.chatuikit.extensions.smartreplies.view;

import androidx.annotation.DrawableRes;

public class SmartRepliesConfiguration {
    private static final String TAG = SmartRepliesConfiguration.class.getSimpleName();

    private @DrawableRes int closeButtonIcon = 0;

    private CometChatSmartReplies.onClose onClose = null;

    private CometChatSmartReplies.onClick onClick = null;

    public SmartRepliesConfiguration setCloseButtonIcon(int closeButtonIcon) {
        this.closeButtonIcon = closeButtonIcon;
        return this;
    }

    public SmartRepliesConfiguration setOnClose(CometChatSmartReplies.onClose onClose) {
        this.onClose = onClose;
        return this;
    }

    public SmartRepliesConfiguration setOnClick(CometChatSmartReplies.onClick onClick) {
        this.onClick = onClick;
        return this;
    }

    public int getCloseButtonIcon() {
        return closeButtonIcon;
    }

    public CometChatSmartReplies.onClose getOnClose() {
        return onClose;
    }

    public CometChatSmartReplies.onClick getOnClick() {
        return onClick;
    }
}
