package com.cometchat.chatuikit.shared.views.timeslotitem;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.models.BaseStyle;

public class TimeSlotItemStyle extends BaseStyle {
    private static final String TAG = TimeSlotItemStyle.class.getSimpleName();
    private int timeTextAppearance;
    private int timeColor;

    public int getTimeTextAppearance() {
        return timeTextAppearance;
    }

    public TimeSlotItemStyle setTimeTextAppearance(@StyleRes int timeTextAppearance) {
        this.timeTextAppearance = timeTextAppearance;
        return this;
    }

    public int getTimeColor() {
        return timeColor;
    }

    public TimeSlotItemStyle setTimeColor(@ColorInt int timeColor) {
        this.timeColor = timeColor;
        return this;
    }

    @Override
    public TimeSlotItemStyle setBackgroundColor(@ColorInt int backgroundColor) {
        super.setBackgroundColor(backgroundColor);
        return this;
    }

    @Override
    public TimeSlotItemStyle setBackground(Drawable drawableBackground) {
        super.setBackground(drawableBackground);
        return this;
    }

    @Override
    public TimeSlotItemStyle setCornerRadius(float cornerRadius) {
        super.setCornerRadius(cornerRadius);
        return this;
    }

    @Override
    public TimeSlotItemStyle setStrokeWidth(int strokeWidth) {
        super.setStrokeWidth(strokeWidth);
        return this;
    }

    @Override
    public TimeSlotItemStyle setStrokeColor(@ColorInt int strokeColor) {
        super.setStrokeColor(strokeColor);
        return this;
    }
}
