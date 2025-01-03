package com.cometchat.chatuikit.shared.views.cometchatschedulerbubble;

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

    public ScheduleStyle setButtonBackgroundColor(@ColorInt int buttonBackgroundColor) {
        this.buttonBackgroundColor = buttonBackgroundColor;
        return this;
    }

    public ScheduleStyle setButtonTextColor(@ColorInt int buttonTextColor) {
        this.buttonTextColor = buttonTextColor;
        return this;
    }

    public ScheduleStyle setButtonTextAppearance(@StyleRes int buttonTextAppearance) {
        this.buttonTextAppearance = buttonTextAppearance;
        return this;
    }

    public ScheduleStyle setProgressBarTintColor(@ColorInt int progressBarTintColor) {
        this.progressBarTintColor = progressBarTintColor;
        return this;
    }

    public ScheduleStyle setTimeTextColor(@ColorInt int timeTextColor) {
        this.timeTextColor = timeTextColor;
        return this;
    }

    public ScheduleStyle setTimeTextAppearance(@StyleRes int timeTextAppearance) {
        this.timeTextAppearance = timeTextAppearance;
        return this;
    }

    public ScheduleStyle setDurationTextColor(@ColorInt int durationTextColor) {
        this.durationTextColor = durationTextColor;
        return this;
    }

    public ScheduleStyle setDurationTextAppearance(@StyleRes int durationTextAppearance) {
        this.durationTextAppearance = durationTextAppearance;
        return this;
    }

    public ScheduleStyle setTimeZoneTextColor(@ColorInt int timeZoneTextColor) {
        this.timeZoneTextColor = timeZoneTextColor;
        return this;
    }

    public ScheduleStyle setTimeZoneTextAppearance(@StyleRes int timeZoneTextAppearance) {
        this.timeZoneTextAppearance = timeZoneTextAppearance;
        return this;
    }

    public ScheduleStyle setTimeZoneIconTint(@ColorInt int timeZoneIconTint) {
        this.timeZoneIconTint = timeZoneIconTint;
        return this;
    }

    public ScheduleStyle setClockIconTint(@ColorInt int clockIconTint) {
        this.clockIconTint = clockIconTint;
        return this;
    }

    public ScheduleStyle setCalendarIconTint(@ColorInt int calendarIconTint) {
        this.calendarIconTint = calendarIconTint;
        return this;
    }

    public ScheduleStyle setErrorTextColor(@ColorInt int errorTextColor) {
        this.errorTextColor = errorTextColor;
        return this;
    }

    public ScheduleStyle setErrorTextAppearance(@StyleRes int errorTextAppearance) {
        this.errorTextAppearance = errorTextAppearance;
        return this;
    }

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

    public int getButtonTextColor() {
        return buttonTextColor;
    }

    public int getButtonTextAppearance() {
        return buttonTextAppearance;
    }

    public int getProgressBarTintColor() {
        return progressBarTintColor;
    }

    public int getTimeTextColor() {
        return timeTextColor;
    }

    public int getTimeTextAppearance() {
        return timeTextAppearance;
    }

    public int getDurationTextColor() {
        return durationTextColor;
    }

    public int getDurationTextAppearance() {
        return durationTextAppearance;
    }

    public int getTimeZoneTextColor() {
        return timeZoneTextColor;
    }

    public int getTimeZoneTextAppearance() {
        return timeZoneTextAppearance;
    }

    public int getTimeZoneIconTint() {
        return timeZoneIconTint;
    }

    public int getClockIconTint() {
        return clockIconTint;
    }

    public int getCalendarIconTint() {
        return calendarIconTint;
    }

    public int getErrorTextColor() {
        return errorTextColor;
    }

    public int getErrorTextAppearance() {
        return errorTextAppearance;
    }
}
