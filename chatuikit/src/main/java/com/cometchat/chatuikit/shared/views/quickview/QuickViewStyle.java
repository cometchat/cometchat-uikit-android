package com.cometchat.chatuikit.shared.views.quickview;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.models.BaseStyle;

public class QuickViewStyle extends BaseStyle {
    private static final String TAG = QuickViewStyle.class.getSimpleName();

    private @StyleRes int titleAppearance;
    private @ColorInt int titleColor;
    private @ColorInt int subtitleColor;
    private @StyleRes int subtitleAppearance;
    private @ColorInt int closeIconTint;
    private @ColorInt int leadingBarTint;

    @Override
    public QuickViewStyle setBackgroundColor(@ColorInt int backgroundColor) {
        super.setBackgroundColor(backgroundColor);
        return this;
    }

    /**
     * @param drawableBackground The drawable background of the view.
     * @return
     */
    @Override
    public QuickViewStyle setBackground(Drawable drawableBackground) {
        super.setBackground(drawableBackground);
        return this;
    }

    /**
     * @param cornerRadius The corner radius of the view.
     * @return
     */
    @Override
    public QuickViewStyle setCornerRadius(float cornerRadius) {
        super.setCornerRadius(cornerRadius);
        return this;
    }

    /**
     * @param strokeWidth The width of the view border.
     * @return
     */
    @Override
    public QuickViewStyle setStrokeWidth(int strokeWidth) {
        super.setStrokeWidth(strokeWidth);
        return this;
    }

    /**
     * @param strokeColor The color of the view border.
     * @return
     */
    @Override
    public QuickViewStyle setStrokeColor(@ColorInt int strokeColor) {
        super.setStrokeColor(strokeColor);
        return this;
    }

    /**
     * @param activeBackground The background color of the view when it is in active state.
     * @return
     */
    @Override
    public QuickViewStyle setActiveBackground(@ColorInt int activeBackground) {
        super.setActiveBackground(activeBackground);
        return this;
    }

    public int getTitleAppearance() {
        return titleAppearance;
    }

    public QuickViewStyle setTitleAppearance(@StyleRes int titleAppearance) {
        this.titleAppearance = titleAppearance;
        return this;
    }

    public int getTitleColor() {
        return titleColor;
    }

    public QuickViewStyle setTitleColor(@ColorInt int titleColor) {
        this.titleColor = titleColor;
        return this;
    }

    public int getSubtitleColor() {
        return subtitleColor;
    }

    public QuickViewStyle setSubtitleColor(@ColorInt int subtitleColor) {
        this.subtitleColor = subtitleColor;
        return this;
    }

    public int getSubtitleAppearance() {
        return subtitleAppearance;
    }

    public QuickViewStyle setSubtitleAppearance(@StyleRes int subtitleAppearance) {
        this.subtitleAppearance = subtitleAppearance;
        return this;
    }

    public int getCloseIconTint() {
        return closeIconTint;
    }

    public QuickViewStyle setCloseIconTint(@ColorInt int closeIconTint) {
        this.closeIconTint = closeIconTint;
        return this;
    }

    public int getLeadingBarTint() {
        return leadingBarTint;
    }

    public QuickViewStyle setLeadingBarTint(@ColorInt int leadingBarTint) {
        this.leadingBarTint = leadingBarTint;
        return this;
    }
}
