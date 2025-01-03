package com.cometchat.chatuikit.extensions.thumbnailgeneration;

import com.cometchat.chatuikit.extensions.ExtensionConstants;
import com.cometchat.chatuikit.shared.framework.ChatConfigurator;
import com.cometchat.chatuikit.shared.framework.ExtensionsDataSource;

public class ThumbnailGenerationExtension extends ExtensionsDataSource {
    private static final String TAG = ThumbnailGenerationExtension.class.getSimpleName();

    public ThumbnailGenerationExtension() {
    }

    @Override
    public void addExtension() {
        ChatConfigurator.enable(var1 -> new ThumbnailGenerationExtensionDecorator(var1));
    }

    @Override
    public String getExtensionId() {
        return ExtensionConstants.ExtensionServerId.THUMBNAIL_GENERATION;
    }
}
