package com.cometchat.chatuikit.extensions.sticker.keyboard;

import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.models.BaseStyle;

public class StickerKeyboardStyle extends BaseStyle {
    private static final String TAG = StickerKeyboardStyle.class.getSimpleName();

    private String emptyTextFont;

    private @StyleRes int emptyTextAppearance;
    private @StyleRes int errorTextAppearance;

    private @ColorInt int loadingIconTint;
    private @ColorInt int emptyTextColor;
    private @ColorInt int errorTextColor;

    @NonNull
    @Override
    public StickerKeyboardStyle setBackgroundColor(@ColorInt int backgroundColor) {
        super.setBackgroundColor(backgroundColor);
        return this;
    }

    @Override
    public StickerKeyboardStyle setBackground(Drawable drawableBackground) {
        super.setBackground(drawableBackground);
        return this;
    }

    @Override
    public StickerKeyboardStyle setCornerRadius(float cornerRadius) {
        super.setCornerRadius(cornerRadius);
        return this;
    }

    @Override
    public StickerKeyboardStyle setStrokeWidth(int strokeWidth) {
        super.setStrokeWidth(strokeWidth);
        return this;
    }

    @Override
    public StickerKeyboardStyle setStrokeColor(@ColorInt int strokeColor) {
        super.setStrokeColor(strokeColor);
        return this;
    }

    @NonNull
    @Override
    public StickerKeyboardStyle setActiveBackground(@ColorInt int activeBackground) {
        Log.i(TAG, "setActiveBackground can not be set");
        super.setActiveBackground(activeBackground);
        return this;
    }

    public StickerKeyboardStyle setEmptyTextFont(String emptyTextFont) {
        this.emptyTextFont = emptyTextFont;
        return this;
    }

    public StickerKeyboardStyle setEmptyTextAppearance(@StyleRes int emptyTextAppearance) {
        this.emptyTextAppearance = emptyTextAppearance;
        return this;
    }

    public StickerKeyboardStyle setErrorTextAppearance(@StyleRes int errorTextAppearance) {
        this.errorTextAppearance = errorTextAppearance;
        return this;
    }

    public StickerKeyboardStyle setLoadingIconTint(@ColorInt int loadingIconTint) {
        this.loadingIconTint = loadingIconTint;
        return this;
    }

    public StickerKeyboardStyle setEmptyTextColor(@ColorInt int emptyTextColor) {
        this.emptyTextColor = emptyTextColor;
        return this;
    }

    public StickerKeyboardStyle setErrorTextColor(@ColorInt int errorTextColor) {
        this.errorTextColor = errorTextColor;
        return this;
    }

    public String getEmptyTextFont() {
        return emptyTextFont;
    }

    public int getEmptyTextAppearance() {
        return emptyTextAppearance;
    }

    public int getErrorTextAppearance() {
        return errorTextAppearance;
    }

    public int getLoadingIconTint() {
        return loadingIconTint;
    }

    public int getEmptyTextColor() {
        return emptyTextColor;
    }

    public int getErrorTextColor() {
        return errorTextColor;
    }
}
