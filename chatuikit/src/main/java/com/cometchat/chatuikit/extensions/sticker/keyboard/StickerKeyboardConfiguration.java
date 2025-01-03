package com.cometchat.chatuikit.extensions.sticker.keyboard;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.StyleRes;

public class StickerKeyboardConfiguration {
    private static final String TAG = StickerKeyboardConfiguration.class.getSimpleName();


    private String emptyStateText, errorStateText;
    private @LayoutRes int emptyStateView;
    private @LayoutRes int errorStateView;
    private @LayoutRes int loadingStateView;
    private @StyleRes int stickerKeyboardStyle;
    private @DrawableRes int stickerIcon;

    public StickerKeyboardConfiguration setStyle(@StyleRes int stickerKeyboardStyle) {
        this.stickerKeyboardStyle = stickerKeyboardStyle;
        return this;
    }

    public StickerKeyboardConfiguration setStickerIcon(@DrawableRes int stickerIcon) {
        this.stickerIcon = stickerIcon;
        return this;
    }

    public StickerKeyboardConfiguration setEmptyStateText(String emptyStateText) {
        this.emptyStateText = emptyStateText;
        return this;
    }

    public StickerKeyboardConfiguration setErrorStateText(String errorStateText) {
        this.errorStateText = errorStateText;
        return this;
    }

    public StickerKeyboardConfiguration setEmptyStateView(@LayoutRes int emptyStateView) {
        this.emptyStateView = emptyStateView;
        return this;
    }

    public StickerKeyboardConfiguration setErrorStateView(@LayoutRes int errorStateView) {
        this.errorStateView = errorStateView;
        return this;
    }

    public StickerKeyboardConfiguration setLoadingStateView(@LayoutRes int loadingStateView) {
        this.loadingStateView = loadingStateView;
        return this;
    }

    public String getEmptyStateText() {
        return emptyStateText;
    }

    public String getErrorStateText() {
        return errorStateText;
    }

    public int getEmptyStateView() {
        return emptyStateView;
    }

    public int getErrorStateView() {
        return errorStateView;
    }

    public int getStickerIcon() {
        return stickerIcon;
    }

    public int getLoadingStateView() {
        return loadingStateView;
    }

    public @StyleRes int getStyle() {
        return stickerKeyboardStyle;
    }
}
