package com.cometchat.chatuikit.shared.views.timeslotselector;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.models.BaseStyle;

public class TimeSlotSelectorStyle extends BaseStyle {
    private static final String TAG = TimeSlotSelectorStyle.class.getSimpleName();
    private @StyleRes int titleTextAppearance;
    private @ColorInt int titleColor;
    private @ColorInt int chosenDateTextColor;
    private @StyleRes int chosenDateTextAppearance;
    private @ColorInt int emptyTimeSlotTextColor;
    private @StyleRes int emptyTimeSlotTextAppearance;
    private @ColorInt int emptyTimeSlotIconTint;
    private @ColorInt int separatorColor;
    private @ColorInt int calenderImageTint;

    @NonNull
    public TimeSlotSelectorStyle setCalenderImageTint(@ColorInt int calenderImageTint) {
        this.calenderImageTint = calenderImageTint;
        return this;
    }

    @NonNull
    public TimeSlotSelectorStyle setSeparatorColor(@ColorInt int separatorColor) {
        this.separatorColor = separatorColor;
        return this;
    }

    public TimeSlotSelectorStyle setTitleTextAppearance(@StyleRes int titleTextAppearance) {
        this.titleTextAppearance = titleTextAppearance;
        return this;
    }

    public TimeSlotSelectorStyle setTitleColor(@ColorInt int titleColor) {
        this.titleColor = titleColor;
        return this;
    }

    public TimeSlotSelectorStyle setEmptyTimeSlotTextColor(@ColorInt int emptyTimeSlotTextColor) {
        this.emptyTimeSlotTextColor = emptyTimeSlotTextColor;
        return this;
    }

    public TimeSlotSelectorStyle setEmptyTimeSlotTextAppearance(@StyleRes int emptyTimeSlotTextAppearance) {
        this.emptyTimeSlotTextAppearance = emptyTimeSlotTextAppearance;
        return this;
    }

    public TimeSlotSelectorStyle setEmptyTimeSlotIconTint(@ColorInt int emptyTimeSlotIconTint) {
        this.emptyTimeSlotIconTint = emptyTimeSlotIconTint;
        return this;
    }

    public TimeSlotSelectorStyle setChosenDateTextColor(@ColorInt int chosenDateTextColor) {
        this.chosenDateTextColor = chosenDateTextColor;
        return this;
    }

    public TimeSlotSelectorStyle setChosenDateTextAppearance(@StyleRes int chosenDateTextAppearance) {
        this.chosenDateTextAppearance = chosenDateTextAppearance;
        return this;
    }

    @Override
    public TimeSlotSelectorStyle setBackgroundColor(@ColorInt int backgroundColor) {
        super.setBackgroundColor(backgroundColor);
        return this;
    }

    @Override
    public TimeSlotSelectorStyle setBackground(Drawable drawableBackground) {
        super.setBackground(drawableBackground);
        return this;
    }

    @Override
    public TimeSlotSelectorStyle setCornerRadius(float cornerRadius) {
        super.setCornerRadius(cornerRadius);
        return this;
    }

    @Override
    public TimeSlotSelectorStyle setStrokeWidth(int strokeWidth) {
        super.setStrokeWidth(strokeWidth);
        return this;
    }

    @Override
    public TimeSlotSelectorStyle setStrokeColor(@ColorInt int strokeColor) {
        super.setStrokeColor(strokeColor);
        return this;
    }

    @Override
    public TimeSlotSelectorStyle setActiveBackground(@ColorInt int activeBackground) {
        super.setActiveBackground(activeBackground);
        return this;
    }

    public int getTitleTextAppearance() {
        return titleTextAppearance;
    }

    public int getTitleColor() {
        return titleColor;
    }

    public int getChosenDateTextColor() {
        return chosenDateTextColor;
    }

    public int getChosenDateTextAppearance() {
        return chosenDateTextAppearance;
    }

    public int getEmptyTimeSlotTextColor() {
        return emptyTimeSlotTextColor;
    }

    public int getEmptyTimeSlotTextAppearance() {
        return emptyTimeSlotTextAppearance;
    }

    public int getSeparatorColor() {
        return separatorColor;
    }

    public int getEmptyTimeSlotIconTint() {
        return emptyTimeSlotIconTint;
    }

    public int getCalenderImageTint() {
        return calenderImageTint;
    }
}
