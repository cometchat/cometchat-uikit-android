package com.cometchat.chatuikit.extensions.smartreplies;

import com.cometchat.chatuikit.extensions.ExtensionConstants;
import com.cometchat.chatuikit.extensions.smartreplies.view.SmartRepliesConfiguration;
import com.cometchat.chatuikit.shared.framework.ChatConfigurator;
import com.cometchat.chatuikit.shared.framework.ExtensionsDataSource;

public class SmartRepliesExtension extends ExtensionsDataSource {
    private static final String TAG = SmartRepliesExtension.class.getSimpleName();
    private SmartRepliesConfiguration configuration;

    public SmartRepliesExtension(SmartRepliesConfiguration configuration) {
        this.configuration = configuration;
    }

    public SmartRepliesExtension() {
    }

    @Override
    public void addExtension() {
        ChatConfigurator.enable(var1 -> new SmartRepliesExtensionDecorator(var1, configuration));
    }

    @Override
    public String getExtensionId() {
        return ExtensionConstants.ExtensionServerId.SMART_REPLIES;
    }
}
