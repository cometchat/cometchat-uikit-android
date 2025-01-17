package com.cometchat.chatuikit.shared.views.formbubble;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.models.BaseStyle;
import com.cometchat.chatuikit.shared.views.quickview.QuickViewStyle;
import com.cometchat.chatuikit.shared.views.singleselect.SingleSelectStyle;

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

    public int getSeparatorColor() {
        return separatorColor;
    }

    public FormBubbleStyle setSeparatorColor(@ColorInt int separatorColor) {
        this.separatorColor = separatorColor;
        return this;
    }

    public QuickViewStyle getQuickViewStyle() {
        return quickViewStyle;
    }

    public FormBubbleStyle setQuickViewStyle(QuickViewStyle quickViewStyle) {
        this.quickViewStyle = quickViewStyle;
        return this;
    }

    public SingleSelectStyle getSingleSelectStyle() {
        return singleSelectStyle;
    }

    public FormBubbleStyle setSingleSelectStyle(SingleSelectStyle singleSelectStyle) {
        this.singleSelectStyle = singleSelectStyle;
        return this;
    }

    public int getSelectedRadioButtonTint() {
        return selectedRadioButtonTint;
    }

    public FormBubbleStyle setSelectedRadioButtonTint(@ColorInt int selectedRadioButtonTint) {
        this.selectedRadioButtonTint = selectedRadioButtonTint;
        return this;
    }

    public int getSelectedCheckboxButtonTint() {
        return selectedCheckboxButtonTint;
    }

    public FormBubbleStyle setSelectedCheckboxButtonTint(@ColorInt int selectedCheckboxButtonTint) {
        this.selectedCheckboxButtonTint = selectedCheckboxButtonTint;
        return this;
    }

    public int getErrorCheckboxButtonTint() {
        return errorCheckboxButtonTint;
    }

    public FormBubbleStyle setErrorCheckboxButtonTint(@ColorInt int errorCheckboxButtonTint) {
        this.errorCheckboxButtonTint = errorCheckboxButtonTint;
        return this;
    }

    public int getActiveInputStrokeColor() {
        return activeInputStrokeColor;
    }

    public FormBubbleStyle setActiveInputStrokeColor(@ColorInt int activeInputStrokeColor) {
        this.activeInputStrokeColor = activeInputStrokeColor;
        return this;
    }

    public int getSpinnerBackgroundColor() {
        return spinnerBackgroundColor;
    }

    public FormBubbleStyle setSpinnerBackgroundColor(@ColorInt int spinnerBackgroundColor) {
        this.spinnerBackgroundColor = spinnerBackgroundColor;
        return this;
    }

    public int getTitleColor() {
        return titleColor;
    }

    public FormBubbleStyle setTitleColor(@ColorInt int titleColor) {
        this.titleColor = titleColor;
        return this;
    }

    public int getTitleAppearance() {
        return titleAppearance;
    }

    public FormBubbleStyle setTitleAppearance(@StyleRes int titleAppearance) {
        this.titleAppearance = titleAppearance;
        return this;
    }

    public int getLabelColor() {
        return labelColor;
    }

    public FormBubbleStyle setLabelColor(@ColorInt int labelColor) {
        this.labelColor = labelColor;
        return this;
    }

    public int getLabelAppearance() {
        return labelAppearance;
    }

    public FormBubbleStyle setLabelAppearance(@StyleRes int labelAppearance) {
        this.labelAppearance = labelAppearance;
        return this;
    }

    public int getInputTextColor() {
        return inputTextColor;
    }

    public FormBubbleStyle setInputTextColor(@ColorInt int inputTextColor) {
        this.inputTextColor = inputTextColor;
        return this;
    }

    public int getInputTextAppearance() {
        return inputTextAppearance;
    }

    public FormBubbleStyle setInputTextAppearance(@StyleRes int inputTextAppearance) {
        this.inputTextAppearance = inputTextAppearance;
        return this;
    }

    public int getInputHintColor() {
        return inputHintColor;
    }

    public FormBubbleStyle setInputHintColor(@ColorInt int inputHintColor) {
        this.inputHintColor = inputHintColor;
        return this;
    }

    public int getErrorColor() {
        return errorColor;
    }

    public FormBubbleStyle setErrorColor(@ColorInt int errorColor) {
        this.errorColor = errorColor;
        return this;
    }

    public int getInputStrokeColor() {
        return inputStrokeColor;
    }

    public FormBubbleStyle setInputStrokeColor(@ColorInt int inputStrokeColor) {
        this.inputStrokeColor = inputStrokeColor;
        return this;
    }

    public int getDefaultCheckboxButtonTint() {
        return defaultCheckboxButtonTint;
    }

    public FormBubbleStyle setDefaultCheckboxButtonTint(@ColorInt int defaultCheckboxButtonTint) {
        this.defaultCheckboxButtonTint = defaultCheckboxButtonTint;
        return this;
    }

    public int getCheckboxTextColor() {
        return checkboxTextColor;
    }

    public FormBubbleStyle setCheckboxTextColor(@ColorInt int checkboxTextColor) {
        this.checkboxTextColor = checkboxTextColor;
        return this;
    }

    public int getCheckboxTextAppearance() {
        return checkboxTextAppearance;
    }

    public FormBubbleStyle setCheckboxTextAppearance(@StyleRes int checkboxTextAppearance) {
        this.checkboxTextAppearance = checkboxTextAppearance;
        return this;
    }

    public int getButtonBackgroundColor() {
        return buttonBackgroundColor;
    }

    @NonNull
    public FormBubbleStyle setButtonBackgroundColor(@ColorInt int buttonBackgroundColor) {
        this.buttonBackgroundColor = buttonBackgroundColor;
        return this;
    }

    public int getButtonTextColor() {
        return buttonTextColor;
    }

    public FormBubbleStyle setButtonTextColor(@ColorInt int buttonTextColor) {
        this.buttonTextColor = buttonTextColor;
        return this;
    }

    public int getButtonTextAppearance() {
        return buttonTextAppearance;
    }

    public FormBubbleStyle setButtonTextAppearance(@StyleRes int buttonTextAppearance) {
        this.buttonTextAppearance = buttonTextAppearance;
        return this;
    }

    public int getRadioButtonTint() {
        return radioButtonTint;
    }

    public FormBubbleStyle setRadioButtonTint(@ColorInt int radioButtonTint) {
        this.radioButtonTint = radioButtonTint;
        return this;
    }

    public int getRadioButtonTextColor() {
        return radioButtonTextColor;
    }

    public FormBubbleStyle setRadioButtonTextColor(@ColorInt int radioButtonTextColor) {
        this.radioButtonTextColor = radioButtonTextColor;
        return this;
    }

    public int getRadioButtonTextAppearance() {
        return radioButtonTextAppearance;
    }

    public FormBubbleStyle setRadioButtonTextAppearance(@StyleRes int radioButtonTextAppearance) {
        this.radioButtonTextAppearance = radioButtonTextAppearance;
        return this;
    }

    public int getSpinnerTextColor() {
        return spinnerTextColor;
    }

    public FormBubbleStyle setSpinnerTextColor(@ColorInt int spinnerTextColor) {
        this.spinnerTextColor = spinnerTextColor;
        return this;
    }

    public int getSpinnerTextAppearance() {
        return spinnerTextAppearance;
    }

    public FormBubbleStyle setSpinnerTextAppearance(@StyleRes int spinnerTextAppearance) {
        this.spinnerTextAppearance = spinnerTextAppearance;
        return this;
    }

    public int getProgressBarTintColor() {
        return progressBarTintColor;
    }

    public FormBubbleStyle setProgressBarTintColor(@ColorInt int progressBarTintColor) {
        this.progressBarTintColor = progressBarTintColor;
        return this;
    }
}
