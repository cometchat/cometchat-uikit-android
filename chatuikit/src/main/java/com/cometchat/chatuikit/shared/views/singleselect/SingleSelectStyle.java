package com.cometchat.chatuikit.shared.views.singleselect;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.models.BaseStyle;

public class SingleSelectStyle extends BaseStyle {
    private static final String TAG = SingleSelectStyle.class.getSimpleName();

    private @StyleRes int titleAppearance;
    private @ColorInt int titleColor;
    private @StyleRes int selectedOptionTextAppearance;
    private @ColorInt int selectedOptionTextColor;
    private @StyleRes int optionTextAppearance;
    private @ColorInt int optionTextColor;
    private @ColorInt int buttonStrokeColor;

    /**
     * @param backgroundColor The background color of the view.
     * @return
     */
    @Override
    public SingleSelectStyle setBackgroundColor(@ColorInt int backgroundColor) {
        super.setBackgroundColor(backgroundColor);
        return this;
    }

    /**
     * @param drawableBackground The drawable background of the view.
     * @return
     */
    @Override
    public SingleSelectStyle setBackground(Drawable drawableBackground) {
        super.setBackground(drawableBackground);
        return this;
    }

    /**
     * @param cornerRadius The corner radius of the view.
     * @return
     */
    @Override
    public SingleSelectStyle setCornerRadius(float cornerRadius) {
        super.setCornerRadius(cornerRadius);
        return this;
    }

    /**
     * @param strokeWidth The width of the view border.
     * @return
     */
    @Override
    public SingleSelectStyle setStrokeWidth(int strokeWidth) {
        super.setStrokeWidth(strokeWidth);
        return this;
    }

    /**
     * @param strokeColor The color of the view border.
     * @return
     */
    @Override
    public SingleSelectStyle setStrokeColor(@ColorInt int strokeColor) {
        super.setStrokeColor(strokeColor);
        return this;
    }

    /**
     * @param activeBackground The background color of the view when it is in active state.
     * @return
     */
    @Override
    public SingleSelectStyle setActiveBackground(@ColorInt int activeBackground) {
        super.setActiveBackground(activeBackground);
        return this;
    }

    public int getButtonStrokeColor() {
        return buttonStrokeColor;
    }

    @NonNull
    public SingleSelectStyle setButtonStrokeColor(@ColorInt int buttonStrokeColor) {
        this.buttonStrokeColor = buttonStrokeColor;
        return this;
    }

    public int getTitleAppearance() {
        return titleAppearance;
    }

    @NonNull
    public SingleSelectStyle setTitleAppearance(@StyleRes int titleAppearance) {
        this.titleAppearance = titleAppearance;
        return this;
    }

    public int getTitleColor() {
        return titleColor;
    }

    public SingleSelectStyle setTitleColor(@ColorInt int titleColor) {
        this.titleColor = titleColor;
        return this;
    }

    public int getSelectedOptionTextAppearance() {
        return selectedOptionTextAppearance;
    }

    public SingleSelectStyle setSelectedOptionTextAppearance(@StyleRes int selectedOptionTextAppearance) {
        this.selectedOptionTextAppearance = selectedOptionTextAppearance;
        return this;
    }

    public int getSelectedOptionTextColor() {
        return selectedOptionTextColor;
    }

    public SingleSelectStyle setSelectedOptionTextColor(@ColorInt int selectedOptionTextColor) {
        this.selectedOptionTextColor = selectedOptionTextColor;
        return this;
    }

    public int getOptionTextAppearance() {
        return optionTextAppearance;
    }

    public SingleSelectStyle setOptionTextAppearance(@StyleRes int optionTextAppearance) {
        this.optionTextAppearance = optionTextAppearance;
        return this;
    }

    public int getOptionTextColor() {
        return optionTextColor;
    }

    public SingleSelectStyle setOptionTextColor(@ColorInt int optionTextColor) {
        this.optionTextColor = optionTextColor;
        return this;
    }
}
