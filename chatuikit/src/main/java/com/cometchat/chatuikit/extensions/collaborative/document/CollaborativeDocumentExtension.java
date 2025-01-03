package com.cometchat.chatuikit.extensions.collaborative.document;

import com.cometchat.chatuikit.extensions.ExtensionConstants;
import com.cometchat.chatuikit.extensions.collaborative.CollaborativeBoardBubbleConfiguration;
import com.cometchat.chatuikit.shared.framework.ChatConfigurator;
import com.cometchat.chatuikit.shared.framework.ExtensionsDataSource;

public class CollaborativeDocumentExtension extends ExtensionsDataSource {
    private static final String TAG = CollaborativeDocumentExtension.class.getSimpleName();


    private CollaborativeBoardBubbleConfiguration configuration;

    public CollaborativeDocumentExtension(CollaborativeBoardBubbleConfiguration configuration) {
        this.configuration = configuration;
    }

    public CollaborativeDocumentExtension() {
    }

    @Override
    public void addExtension() {
        ChatConfigurator.enable(var1 -> new CollaborativeDocumentExtensionDecorator(var1, configuration));
    }

    @Override
    public String getExtensionId() {
        return ExtensionConstants.ExtensionServerId.COLLABORATION_DOCUMENT;
    }
}
