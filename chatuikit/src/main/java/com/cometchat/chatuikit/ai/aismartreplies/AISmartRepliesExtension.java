package com.cometchat.chatuikit.ai.aismartreplies;

import com.cometchat.chatuikit.ai.AIExtensionDataSource;
import com.cometchat.chatuikit.shared.framework.ChatConfigurator;

public class AISmartRepliesExtension extends AIExtensionDataSource {
    private static final String TAG = AISmartRepliesExtension.class.getSimpleName();
    public static final String ID = "smart-replies";
    AISmartRepliesConfiguration configuration;

    public AISmartRepliesExtension(AISmartRepliesConfiguration configuration) {
        this.configuration = configuration;
    }

    public AISmartRepliesExtension() {
    }

    @Override
    public void addExtension() {
        ChatConfigurator.enable(var1 -> new AISmartRepliesDecorator(var1, configuration));
    }

    @Override
    public String getExtensionId() {
        return ID;
    }
}
