package com.cometchat.chatuikit.extensions.collaborative.whiteboard;

import com.cometchat.chatuikit.extensions.ExtensionConstants;
import com.cometchat.chatuikit.extensions.collaborative.CollaborativeBoardBubbleConfiguration;
import com.cometchat.chatuikit.shared.framework.ChatConfigurator;
import com.cometchat.chatuikit.shared.framework.ExtensionsDataSource;

public class CollaborativeWhiteboardExtension extends ExtensionsDataSource {
    private static final String TAG = CollaborativeWhiteboardExtension.class.getSimpleName();


    private CollaborativeBoardBubbleConfiguration configuration;

    public CollaborativeWhiteboardExtension(CollaborativeBoardBubbleConfiguration configuration) {
        this.configuration = configuration;
    }

    public CollaborativeWhiteboardExtension() {
    }

    @Override
    public void addExtension() {
        ChatConfigurator.enable(var1 -> new CollaborativeWhiteboardExtensionDecorator(var1, configuration));
    }

    @Override
    public String getExtensionId() {
        return ExtensionConstants.ExtensionServerId.COLLABORATION_WHITEBOARD;
    }
}
