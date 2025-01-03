package com.cometchat.chatuikit.ai.aiconversationstarter;

import com.cometchat.chatuikit.ai.AIExtensionDataSource;
import com.cometchat.chatuikit.shared.framework.ChatConfigurator;

public class AIConversationStarterExtension extends AIExtensionDataSource {
    private static final String TAG = AIConversationStarterExtension.class.getSimpleName();
    public static final String ID = "conversation-starter";
    public AIConversationStarterConfiguration configuration;

    public AIConversationStarterExtension(AIConversationStarterConfiguration configuration) {
        this.configuration = configuration;
    }

    public AIConversationStarterExtension() {
    }

    @Override
    public void addExtension() {
        ChatConfigurator.enable(var1 -> new AIConversationStarterDecorator(var1, configuration));
    }

    @Override
    public String getExtensionId() {
        return ID;
    }
}
