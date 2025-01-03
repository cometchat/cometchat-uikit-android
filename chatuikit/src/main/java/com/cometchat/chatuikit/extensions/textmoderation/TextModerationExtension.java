package com.cometchat.chatuikit.extensions.textmoderation;

import androidx.annotation.StyleRes;

import com.cometchat.chat.core.CometChat;
import com.cometchat.chatuikit.extensions.ExtensionConstants;
import com.cometchat.chatuikit.shared.framework.ChatConfigurator;
import com.cometchat.chatuikit.shared.framework.ExtensionsDataSource;

public class TextModerationExtension extends ExtensionsDataSource {
    private static final String TAG = TextModerationExtension.class.getSimpleName();


    private @StyleRes int textBubbleStyle;

    public TextModerationExtension(@StyleRes int textBubbleStyle) {
        this.textBubbleStyle = textBubbleStyle;
    }

    public TextModerationExtension() {
    }

    @Override
    public void addExtension() {
        ChatConfigurator.enable(var1 -> new TextModerationExtensionDecorator(var1, textBubbleStyle));
    }

    @Override
    public String getExtensionId() {
        return ExtensionConstants.ExtensionServerId.PROFANITY_FILTER;
    }

    @Override
    public void enable() {
        if (CometChat.isExtensionEnabled(ExtensionConstants.ExtensionServerId.PROFANITY_FILTER) || CometChat.isExtensionEnabled(ExtensionConstants.ExtensionServerId.DATA_MASKING))
            addExtension();
    }
}
