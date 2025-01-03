package com.cometchat.chatuikit.extensions.messagetranslation;

import com.cometchat.chatuikit.extensions.ExtensionConstants;
import com.cometchat.chatuikit.shared.framework.ChatConfigurator;
import com.cometchat.chatuikit.shared.framework.ExtensionsDataSource;

public class MessageTranslationExtension extends ExtensionsDataSource {
    private static final String TAG = MessageTranslationExtension.class.getSimpleName();

    @Override
    public void addExtension() {
        ChatConfigurator.enable(MessageTranslationDecorator::new);
    }

    @Override
    public String getExtensionId() {
        return ExtensionConstants.ExtensionServerId.MESSAGE_TRANSLATION;
    }
}
