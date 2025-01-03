package com.cometchat.chatuikit.shared.views.cometchatformbubble;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.models.BaseStyle;
import com.cometchat.chatuikit.shared.views.cometchatquickview.QuickViewStyle;
import com.cometchat.chatuikit.shared.views.cometchatsingleselect.SingleSelectStyle;

public class FormBubbleStyle extends BaseStyle {
    private static final String TAG = FormBubbleStyle.class.getSimpleName();

    private @ColorInt int titleColor;
    private @StyleRes int titleAppearance;
    private @ColorInt int labelColor;
    private @StyleRes int labelAppearance;
    private @ColorInt int inputTextColor;
    private @StyleRes int inputTextAppearance;
    private @ColorInt int inputHintColor;
    private @ColorInt int errorColor;
    private @ColorInt int inputStrokeColor;
    private @ColorInt int activeInputStrokeColor;
    private @ColorInt int defaultCheckboxButtonTint;
    private @ColorInt int selectedCheckboxButtonTint;
    private @ColorInt int errorCheckboxButtonTint;
    private @ColorInt int checkboxTextColor;
    private @StyleRes int checkboxTextAppearance;
    private @ColorInt int buttonBackgroundColor;
    private @ColorInt int buttonTextColor;
    private @StyleRes int buttonTextAppearance;
    private @ColorInt int progressBarTintColor;
    private @ColorInt int radioButtonTint;
    private @ColorInt int selectedRadioButtonTint;
    private @ColorInt int radioButtonTextColor;
    private @StyleRes int radioButtonTextAppearance;
    private @ColorInt int spinnerTextColor;
    private @StyleRes int spinnerTextAppearance;
    private @ColorInt int spinnerBackgroundColor;
    private SingleSelectStyle singleSelectStyle;
    private QuickViewStyle quickViewStyle;
    private @ColorInt int separatorColor;

    /**
     * @param backgroundColor The background color of the view.
     */
    @NonNull
    @Override
    public FormBubbleStyle setBackgroundColor(@ColorInt int backgroundColor) {
        super.setBackgroundColor(backgroundColor);
        return this;
    }

    /**
     * @param drawableBackground The drawable background of the view.
     */
    @Override
    public FormBubbleStyle setBackground(Drawable drawableBackground) {
        super.setBackground(drawableBackground);
        return this;
    }

    /**
     * @param cornerRadius The corner radius of the view.
     */
    @Override
    public FormBubbleStyle setCornerRadius(float cornerRadius) {
        super.setCornerRadius(cornerRadius);
        return this;
    }

    /**
     * @param strokeWidth The width of the view border.
     */
    @Override
    public FormBubbleStyle setStrokeWidth(int strokeWidth) {
        super.setStrokeWidth(strokeWidth);
        return this;
    }

    /**
     * @param strokeColor The color of the view border.
     */
    @Override
    public FormBubbleStyle setStrokeColor(@ColorInt int strokeColor) {
        super.setStrokeColor(strokeColor);
        return this;
    }

    @Override
    public FormBubbleStyle setActiveBackground(@ColorInt int activeBackground) {
        super.setActiveBackground(activeBackground);
        return this;
    }

    public FormBubbleStyle setQuickViewStyle(QuickViewStyle quickViewStyle) {
        this.quickViewStyle = quickViewStyle;
        return this;
    }

    public FormBubbleStyle setSeparatorColor(@ColorInt int separatorColor) {
        this.separatorColor = separatorColor;
        return this;
    }

    public FormBubbleStyle setTitleColor(@ColorInt int titleColor) {
        this.titleColor = titleColor;
        return this;
    }

    public FormBubbleStyle setProgressBarTintColor(@ColorInt int progressBarTintColor) {
        this.progressBarTintColor = progressBarTintColor;
        return this;
    }

    public FormBubbleStyle setTitleAppearance(@StyleRes int titleAppearance) {
        this.titleAppearance = titleAppearance;
        return this;
    }

    public FormBubbleStyle setLabelColor(@ColorInt int labelColor) {
        this.labelColor = labelColor;
        return this;
    }

    public FormBubbleStyle setLabelAppearance(@StyleRes int labelAppearance) {
        this.labelAppearance = labelAppearance;
        return this;
    }

    public FormBubbleStyle setActiveInputStrokeColor(@ColorInt int activeInputStrokeColor) {
        this.activeInputStrokeColor = activeInputStrokeColor;
        return this;
    }

    public FormBubbleStyle setInputTextColor(@ColorInt int inputTextColor) {
        this.inputTextColor = inputTextColor;
        return this;
    }

    public FormBubbleStyle setInputTextAppearance(@StyleRes int inputTextAppearance) {
        this.inputTextAppearance = inputTextAppearance;
        return this;
    }

    public FormBubbleStyle setInputHintColor(@ColorInt int inputHintColor) {
        this.inputHintColor = inputHintColor;
        return this;
    }

    public FormBubbleStyle setErrorColor(@ColorInt int errorColor) {
        this.errorColor = errorColor;
        return this;
    }

    public FormBubbleStyle setInputStrokeColor(@ColorInt int inputStrokeColor) {
        this.inputStrokeColor = inputStrokeColor;
        return this;
    }

    public FormBubbleStyle setDefaultCheckboxButtonTint(@ColorInt int defaultCheckboxButtonTint) {
        this.defaultCheckboxButtonTint = defaultCheckboxButtonTint;
        return this;
    }

    public FormBubbleStyle setSelectedCheckboxButtonTint(@ColorInt int selectedCheckboxButtonTint) {
        this.selectedCheckboxButtonTint = selectedCheckboxButtonTint;
        return this;
    }

    public FormBubbleStyle setErrorCheckboxButtonTint(@ColorInt int errorCheckboxButtonTint) {
        this.errorCheckboxButtonTint = errorCheckboxButtonTint;
        return this;
    }

    public FormBubbleStyle setCheckboxTextColor(@ColorInt int checkboxTextColor) {
        this.checkboxTextColor = checkboxTextColor;
        return this;
    }

    public FormBubbleStyle setCheckboxTextAppearance(@StyleRes int checkboxTextAppearance) {
        this.checkboxTextAppearance = checkboxTextAppearance;
        return this;
    }

    @NonNull
    public FormBubbleStyle setButtonBackgroundColor(@ColorInt int buttonBackgroundColor) {
        this.buttonBackgroundColor = buttonBackgroundColor;
        return this;
    }

    public FormBubbleStyle setButtonTextColor(@ColorInt int buttonTextColor) {
        this.buttonTextColor = buttonTextColor;
        return this;
    }

    public FormBubbleStyle setButtonTextAppearance(@StyleRes int buttonTextAppearance) {
        this.buttonTextAppearance = buttonTextAppearance;
        return this;
    }

    public FormBubbleStyle setRadioButtonTint(@ColorInt int radioButtonTint) {
        this.radioButtonTint = radioButtonTint;
        return this;
    }

    public FormBubbleStyle setRadioButtonTextColor(@ColorInt int radioButtonTextColor) {
        this.radioButtonTextColor = radioButtonTextColor;
        return this;
    }

    public FormBubbleStyle setRadioButtonTextAppearance(@StyleRes int radioButtonTextAppearance) {
        this.radioButtonTextAppearance = radioButtonTextAppearance;
        return this;
    }

    public FormBubbleStyle setSelectedRadioButtonTint(@ColorInt int selectedRadioButtonTint) {
        this.selectedRadioButtonTint = selectedRadioButtonTint;
        return this;
    }

    public FormBubbleStyle setSpinnerTextColor(@ColorInt int spinnerTextColor) {
        this.spinnerTextColor = spinnerTextColor;
        return this;
    }

    public FormBubbleStyle setSpinnerTextAppearance(@StyleRes int spinnerTextAppearance) {
        this.spinnerTextAppearance = spinnerTextAppearance;
        return this;
    }

    public FormBubbleStyle setSpinnerBackgroundColor(@ColorInt int spinnerBackgroundColor) {
        this.spinnerBackgroundColor = spinnerBackgroundColor;
        return this;
    }

    public FormBubbleStyle setSingleSelectStyle(SingleSelectStyle singleSelectStyle) {
        this.singleSelectStyle = singleSelectStyle;
        return this;
    }

    public int getSeparatorColor() {
        return separatorColor;
    }

    public QuickViewStyle getQuickViewStyle() {
        return quickViewStyle;
    }

    public SingleSelectStyle getSingleSelectStyle() {
        return singleSelectStyle;
    }

    public int getSelectedRadioButtonTint() {
        return selectedRadioButtonTint;
    }

    public int getSelectedCheckboxButtonTint() {
        return selectedCheckboxButtonTint;
    }

    public int getErrorCheckboxButtonTint() {
        return errorCheckboxButtonTint;
    }

    public int getActiveInputStrokeColor() {
        return activeInputStrokeColor;
    }

    public int getSpinnerBackgroundColor() {
        return spinnerBackgroundColor;
    }

    public int getTitleColor() {
        return titleColor;
    }

    public int getTitleAppearance() {
        return titleAppearance;
    }

    public int getLabelColor() {
        return labelColor;
    }

    public int getLabelAppearance() {
        return labelAppearance;
    }

    public int getInputTextColor() {
        return inputTextColor;
    }

    public int getInputTextAppearance() {
        return inputTextAppearance;
    }

    public int getInputHintColor() {
        return inputHintColor;
    }

    public int getErrorColor() {
        return errorColor;
    }

    public int getInputStrokeColor() {
        return inputStrokeColor;
    }

    public int getDefaultCheckboxButtonTint() {
        return defaultCheckboxButtonTint;
    }

    public int getCheckboxTextColor() {
        return checkboxTextColor;
    }

    public int getCheckboxTextAppearance() {
        return checkboxTextAppearance;
    }

    public int getButtonBackgroundColor() {
        return buttonBackgroundColor;
    }

    public int getButtonTextColor() {
        return buttonTextColor;
    }

    public int getButtonTextAppearance() {
        return buttonTextAppearance;
    }

    public int getRadioButtonTint() {
        return radioButtonTint;
    }

    public int getRadioButtonTextColor() {
        return radioButtonTextColor;
    }

    public int getRadioButtonTextAppearance() {
        return radioButtonTextAppearance;
    }

    public int getSpinnerTextColor() {
        return spinnerTextColor;
    }

    public int getSpinnerTextAppearance() {
        return spinnerTextAppearance;
    }

    public int getProgressBarTintColor() {
        return progressBarTintColor;
    }
}
