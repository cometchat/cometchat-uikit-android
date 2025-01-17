package com.cometchat.chatuikit.shared.views.schedulerbubble;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.models.BaseStyle;

public class ScheduleStyle extends BaseStyle {
    private static final String TAG = ScheduleStyle.class.getSimpleName();


    private @ColorInt int buttonBackgroundColor;
    private @ColorInt int buttonTextColor;
    private @StyleRes int buttonTextAppearance;
    private @ColorInt int progressBarTintColor;
    private @ColorInt int timeTextColor;
    private @StyleRes int timeTextAppearance;
    private @ColorInt int durationTextColor;
    private @StyleRes int durationTextAppearance;
    private @ColorInt int timeZoneTextColor;
    private @StyleRes int timeZoneTextAppearance;
    private @ColorInt int timeZoneIconTint;
    private @ColorInt int clockIconTint;
    private @ColorInt int calendarIconTint;
    private @ColorInt int errorTextColor;
    private @StyleRes int errorTextAppearance;

    @Override
    public ScheduleStyle setBackgroundColor(@ColorInt int backgroundColor) {
        super.setBackgroundColor(backgroundColor);
        return this;
    }

    @Override
    public ScheduleStyle setBackground(Drawable drawableBackground) {
        super.setBackground(drawableBackground);
        return this;
    }

    @Override
    public ScheduleStyle setCornerRadius(float cornerRadius) {
        super.setCornerRadius(cornerRadius);
        return this;
    }

    @Override
    public ScheduleStyle setStrokeWidth(int strokeWidth) {
        super.setStrokeWidth(strokeWidth);
        return this;
    }

    @Override
    public ScheduleStyle setStrokeColor(@ColorInt int strokeColor) {
        super.setStrokeColor(strokeColor);
        return this;
    }

    @Override
    public ScheduleStyle setActiveBackground(@ColorInt int activeBackground) {
        super.setActiveBackground(activeBackground);
        return this;
    }

    public int getButtonBackgroundColor() {
        return buttonBackgroundColor;
    }

    public ScheduleStyle setButtonBackgroundColor(@ColorInt int buttonBackgroundColor) {
        this.buttonBackgroundColor = buttonBackgroundColor;
        return this;
    }

    public int getButtonTextColor() {
        return buttonTextColor;
    }

    public ScheduleStyle setButtonTextColor(@ColorInt int buttonTextColor) {
        this.buttonTextColor = buttonTextColor;
        return this;
    }

    public int getButtonTextAppearance() {
        return buttonTextAppearance;
    }

    public ScheduleStyle setButtonTextAppearance(@StyleRes int buttonTextAppearance) {
        this.buttonTextAppearance = buttonTextAppearance;
        return this;
    }

    public int getProgressBarTintColor() {
        return progressBarTintColor;
    }

    public ScheduleStyle setProgressBarTintColor(@ColorInt int progressBarTintColor) {
        this.progressBarTintColor = progressBarTintColor;
        return this;
    }

    public int getTimeTextColor() {
        return timeTextColor;
    }

    public ScheduleStyle setTimeTextColor(@ColorInt int timeTextColor) {
        this.timeTextColor = timeTextColor;
        return this;
    }

    public int getTimeTextAppearance() {
        return timeTextAppearance;
    }

    public ScheduleStyle setTimeTextAppearance(@StyleRes int timeTextAppearance) {
        this.timeTextAppearance = timeTextAppearance;
        return this;
    }

    public int getDurationTextColor() {
        return durationTextColor;
    }

    public ScheduleStyle setDurationTextColor(@ColorInt int durationTextColor) {
        this.durationTextColor = durationTextColor;
        return this;
    }

    public int getDurationTextAppearance() {
        return durationTextAppearance;
    }

    public ScheduleStyle setDurationTextAppearance(@StyleRes int durationTextAppearance) {
        this.durationTextAppearance = durationTextAppearance;
        return this;
    }

    public int getTimeZoneTextColor() {
        return timeZoneTextColor;
    }

    public ScheduleStyle setTimeZoneTextColor(@ColorInt int timeZoneTextColor) {
        this.timeZoneTextColor = timeZoneTextColor;
        return this;
    }

    public int getTimeZoneTextAppearance() {
        return timeZoneTextAppearance;
    }

    public ScheduleStyle setTimeZoneTextAppearance(@StyleRes int timeZoneTextAppearance) {
        this.timeZoneTextAppearance = timeZoneTextAppearance;
        return this;
    }

    public int getTimeZoneIconTint() {
        return timeZoneIconTint;
    }

    public ScheduleStyle setTimeZoneIconTint(@ColorInt int timeZoneIconTint) {
        this.timeZoneIconTint = timeZoneIconTint;
        return this;
    }

    public int getClockIconTint() {
        return clockIconTint;
    }

    public ScheduleStyle setClockIconTint(@ColorInt int clockIconTint) {
        this.clockIconTint = clockIconTint;
        return this;
    }

    public int getCalendarIconTint() {
        return calendarIconTint;
    }

    public ScheduleStyle setCalendarIconTint(@ColorInt int calendarIconTint) {
        this.calendarIconTint = calendarIconTint;
        return this;
    }

    public int getErrorTextColor() {
        return errorTextColor;
    }

    public ScheduleStyle setErrorTextColor(@ColorInt int errorTextColor) {
        this.errorTextColor = errorTextColor;
        return this;
    }

    public int getErrorTextAppearance() {
        return errorTextAppearance;
    }

    public ScheduleStyle setErrorTextAppearance(@StyleRes int errorTextAppearance) {
        this.errorTextAppearance = errorTextAppearance;
        return this;
    }
}
