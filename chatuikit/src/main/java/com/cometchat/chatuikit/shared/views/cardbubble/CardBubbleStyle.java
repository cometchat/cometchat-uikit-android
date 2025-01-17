package com.cometchat.chatuikit.shared.views.cardbubble;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.models.BaseStyle;
import com.cometchat.chatuikit.shared.views.quickview.QuickViewStyle;

public class CardBubbleStyle extends BaseStyle {
    private static final String TAG = CardBubbleStyle.class.getSimpleName();

    private @ColorInt int textColor;
    private @StyleRes int textAppearance;
    private @ColorInt int buttonBackgroundColor;
    private @ColorInt int buttonTextColor;
    private @ColorInt int buttonDisableTextColor;
    private @StyleRes int buttonTextAppearance;
    private @ColorInt int contentBackgroundColor;
    private @ColorInt int buttonSeparatorColor;
    private @ColorInt int progressBarTintColor;
    private int contentRadius;
    private QuickViewStyle quickViewStyle;

    /**
     * @param backgroundColor The background color of the view.
     */
    @Override
    public CardBubbleStyle setBackgroundColor(@ColorInt int backgroundColor) {
        super.setBackgroundColor(backgroundColor);
        return this;
    }

    /**
     * @param drawableBackground The drawable background of the view.
     */
    @Override
    public CardBubbleStyle setBackground(Drawable drawableBackground) {
        super.setBackground(drawableBackground);
        return this;
    }

    /**
     * @param cornerRadius The corner radius of the view.
     */
    @Override
    public CardBubbleStyle setCornerRadius(float cornerRadius) {
        super.setCornerRadius(cornerRadius);
        return this;
    }

    /**
     * @param strokeWidth The width of the view border.
     */
    @Override
    public CardBubbleStyle setStrokeWidth(int strokeWidth) {
        super.setStrokeWidth(strokeWidth);
        return this;
    }

    /**
     * @param strokeColor The color of the view border.
     */
    @Override
    public CardBubbleStyle setStrokeColor(@ColorInt int strokeColor) {
        super.setStrokeColor(strokeColor);
        return this;
    }

    @NonNull
    @Override
    public CardBubbleStyle setActiveBackground(@ColorInt int activeBackground) {
        super.setActiveBackground(activeBackground);
        return this;
    }

    public QuickViewStyle getQuickViewStyle() {
        return quickViewStyle;
    }

    public CardBubbleStyle setQuickViewStyle(QuickViewStyle quickViewStyle) {
        this.quickViewStyle = quickViewStyle;
        return this;
    }

    public int getTextColor() {
        return textColor;
    }

    public CardBubbleStyle setTextColor(@ColorInt int textColor) {
        this.textColor = textColor;
        return this;
    }

    public int getTextAppearance() {
        return textAppearance;
    }

    public CardBubbleStyle setTextAppearance(@StyleRes int textAppearance) {
        this.textAppearance = textAppearance;
        return this;
    }

    public int getProgressBarTintColor() {
        return progressBarTintColor;
    }

    public CardBubbleStyle setProgressBarTintColor(@ColorInt int progressBarTintColor) {
        this.progressBarTintColor = progressBarTintColor;
        return this;
    }

    public int getButtonBackgroundColor() {
        return buttonBackgroundColor;
    }

    public CardBubbleStyle setButtonBackgroundColor(@ColorInt int buttonBackgroundColor) {
        this.buttonBackgroundColor = buttonBackgroundColor;
        return this;
    }

    public int getButtonTextColor() {
        return buttonTextColor;
    }

    public CardBubbleStyle setButtonTextColor(@ColorInt int buttonTextColor) {
        this.buttonTextColor = buttonTextColor;
        return this;
    }

    public int getButtonTextAppearance() {
        return buttonTextAppearance;
    }

    public CardBubbleStyle setButtonTextAppearance(@StyleRes int buttonTextAppearance) {
        this.buttonTextAppearance = buttonTextAppearance;
        return this;
    }

    public int getContentBackgroundColor() {
        return contentBackgroundColor;
    }

    public CardBubbleStyle setContentBackgroundColor(@ColorInt int contentBackgroundColor) {
        this.contentBackgroundColor = contentBackgroundColor;
        return this;
    }

    public int getButtonDisableTextColor() {
        return buttonDisableTextColor;
    }

    public CardBubbleStyle setButtonDisableTextColor(@ColorInt int buttonDisableTextColor) {
        this.buttonDisableTextColor = buttonDisableTextColor;
        return this;
    }

    public int getButtonSeparatorColor() {
        return buttonSeparatorColor;
    }

    public CardBubbleStyle setButtonSeparatorColor(@ColorInt int buttonSeparatorColor) {
        this.buttonSeparatorColor = buttonSeparatorColor;
        return this;
    }

    public int getContentRadius() {
        return contentRadius;
    }

    public CardBubbleStyle setContentRadius(int contentRadius) {
        this.contentRadius = contentRadius;
        return this;
    }
}
