package com.cometchat.chatuikit.extensions.sticker;

import androidx.annotation.NonNull;

import com.cometchat.chatuikit.extensions.ExtensionConstants;
import com.cometchat.chatuikit.extensions.sticker.keyboard.StickerKeyboardConfiguration;
import com.cometchat.chatuikit.shared.framework.ChatConfigurator;
import com.cometchat.chatuikit.shared.framework.ExtensionsDataSource;

public class StickerExtension extends ExtensionsDataSource {
    private static final String TAG = StickerExtension.class.getSimpleName();


    private StickerKeyboardConfiguration configuration;

    public StickerExtension(StickerKeyboardConfiguration configuration) {
        this.configuration = configuration;
    }

    public StickerExtension() {
    }

    @Override
    public void addExtension() {
        ChatConfigurator.enable(var1 -> new StickerExtensionDecorator(var1, configuration));
    }

    @NonNull
    @Override
    public String getExtensionId() {
        return ExtensionConstants.ExtensionServerId.STICKERS;
    }
}
