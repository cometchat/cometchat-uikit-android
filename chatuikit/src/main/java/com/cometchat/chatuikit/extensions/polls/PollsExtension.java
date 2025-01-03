package com.cometchat.chatuikit.extensions.polls;

import com.cometchat.chatuikit.extensions.ExtensionConstants;
import com.cometchat.chatuikit.shared.framework.ChatConfigurator;
import com.cometchat.chatuikit.shared.framework.ExtensionsDataSource;

public class PollsExtension extends ExtensionsDataSource {
    private static final String TAG = PollsExtension.class.getSimpleName();

    public PollsExtension() {
    }

    @Override
    public void addExtension() {
        ChatConfigurator.enable(PollsExtensionDecorator::new);
    }

    @Override
    public String getExtensionId() {
        return ExtensionConstants.ExtensionServerId.POLLS;
    }
}
