package com.cometchat.chatuikit.shared.views.schedulerbubble;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.models.BaseStyle;
import com.cometchat.chatuikit.shared.views.calender.CalenderStyle;
import com.cometchat.chatuikit.shared.views.quickview.QuickViewStyle;
import com.cometchat.chatuikit.shared.views.timeslotitem.TimeSlotItemStyle;
import com.cometchat.chatuikit.shared.views.timeslotselector.TimeSlotSelectorStyle;

public class SchedulerBubbleStyle extends BaseStyle {
    private static final String TAG = SchedulerBubbleStyle.class.getSimpleName();

    private @ColorInt int titleColor, nameColor;
    private @StyleRes int titleAppearance, nameAppearance, quickSlotAvailableAppearance;
    private QuickViewStyle quickViewStyle;
    private TimeSlotSelectorStyle timeSlotSelectorStyle;
    private TimeSlotItemStyle selectedSlotStyle;
    private TimeSlotItemStyle slotStyle;
    private CalenderStyle calenderStyle;
    private TimeSlotItemStyle initialSlotsItemStyle;
    private ScheduleStyle scheduleStyle;
    private @ColorInt int backIconTint;
    private @ColorInt int clockIconTint;
    private @ColorInt int subtitleTextColor;
    private @StyleRes int subtitleTextAppearance;
    private @StyleRes int avatarStyle;
    private @ColorInt int separatorColor;
    private @ColorInt int timeZoneTextColor;
    private @StyleRes int timeZoneTextAppearance;
    private @ColorInt int globeIconTint;
    private @ColorInt int moreTextColor;
    private @StyleRes int moreTextAppearance;
    private @ColorInt int durationTimeTextColor;
    private @StyleRes int durationTimeTextAppearance;
    private @ColorInt int disableColor;
    private @ColorInt int quickSlotAvailableTextColor;

    @Override
    public SchedulerBubbleStyle setBackgroundColor(int backgroundColor) {
        super.setBackgroundColor(backgroundColor);
        return this;
    }

    @NonNull
    @Override
    public SchedulerBubbleStyle setBackground(Drawable drawableBackground) {
        super.setBackground(drawableBackground);
        return this;
    }

    @Override
    public SchedulerBubbleStyle setCornerRadius(float cornerRadius) {
        super.setCornerRadius(cornerRadius);
        return this;
    }

    @Override
    public SchedulerBubbleStyle setStrokeWidth(int strokeWidth) {
        super.setStrokeWidth(strokeWidth);
        return this;
    }

    @Override
    public SchedulerBubbleStyle setStrokeColor(int strokeColor) {
        super.setStrokeColor(strokeColor);
        return this;
    }

    @Override
    public SchedulerBubbleStyle setActiveBackground(int activeBackground) {
        super.setActiveBackground(activeBackground);
        return this;
    }

    public int getTitleColor() {
        return titleColor;
    }

    public SchedulerBubbleStyle setTitleColor(int titleColor) {
        this.titleColor = titleColor;
        return this;
    }

    public int getTitleAppearance() {
        return titleAppearance;
    }

    public SchedulerBubbleStyle setTitleAppearance(int titleAppearance) {
        this.titleAppearance = titleAppearance;
        return this;
    }

    public int getClockIconTint() {
        return clockIconTint;
    }

    public SchedulerBubbleStyle setClockIconTint(int clockIconTint) {
        this.clockIconTint = clockIconTint;
        return this;
    }

    public int getSubtitleTextColor() {
        return subtitleTextColor;
    }

    public SchedulerBubbleStyle setSubtitleTextColor(int subtitleTextColor) {
        this.subtitleTextColor = subtitleTextColor;
        return this;
    }

    public int getSubtitleTextAppearance() {
        return subtitleTextAppearance;
    }

    public SchedulerBubbleStyle setSubtitleTextAppearance(int subtitleTextAppearance) {
        this.subtitleTextAppearance = subtitleTextAppearance;
        return this;
    }

    public @StyleRes int getAvatarStyle() {
        return avatarStyle;
    }

    @NonNull
    public SchedulerBubbleStyle setAvatarStyle(@StyleRes int avatarStyle) {
        this.avatarStyle = avatarStyle;
        return this;
    }

    public QuickViewStyle getQuickViewStyle() {
        return quickViewStyle;
    }

    public SchedulerBubbleStyle setQuickViewStyle(QuickViewStyle quickViewStyle) {
        this.quickViewStyle = quickViewStyle;
        return this;
    }

    public TimeSlotSelectorStyle getTimeSlotSelectorStyle() {
        return timeSlotSelectorStyle;
    }

    public SchedulerBubbleStyle setTimeSlotSelectorStyle(TimeSlotSelectorStyle singleSelectStyle) {
        this.timeSlotSelectorStyle = singleSelectStyle;
        return this;
    }

    public TimeSlotItemStyle getSelectedSlotStyle() {
        return selectedSlotStyle;
    }

    @NonNull
    public SchedulerBubbleStyle setSelectedSlotStyle(TimeSlotItemStyle selectedSlotStyle) {
        this.selectedSlotStyle = selectedSlotStyle;
        return this;
    }

    public TimeSlotItemStyle getSlotStyle() {
        return slotStyle;
    }

    public SchedulerBubbleStyle setSlotStyle(TimeSlotItemStyle slotStyle) {
        this.slotStyle = slotStyle;
        return this;
    }

    public CalenderStyle getCalenderStyle() {
        return calenderStyle;
    }

    public SchedulerBubbleStyle setCalenderStyle(CalenderStyle calenderStyle) {
        this.calenderStyle = calenderStyle;
        return this;
    }

    public TimeSlotItemStyle getInitialSlotsItemStyle() {
        return initialSlotsItemStyle;
    }

    public SchedulerBubbleStyle setInitialSlotsItemStyle(TimeSlotItemStyle initialSlotsItemStyle) {
        this.initialSlotsItemStyle = initialSlotsItemStyle;
        return this;
    }

    public ScheduleStyle getScheduleStyle() {
        return scheduleStyle;
    }

    public SchedulerBubbleStyle setScheduleStyle(ScheduleStyle scheduleStyle) {
        this.scheduleStyle = scheduleStyle;
        return this;
    }

    public int getBackIconTint() {
        return backIconTint;
    }

    public SchedulerBubbleStyle setBackIconTint(int backIconTint) {
        this.backIconTint = backIconTint;
        return this;
    }

    public int getSeparatorColor() {
        return separatorColor;
    }

    public SchedulerBubbleStyle setSeparatorColor(int separatorColor) {
        this.separatorColor = separatorColor;
        return this;
    }

    public int getTimeZoneTextColor() {
        return timeZoneTextColor;
    }

    @NonNull
    public SchedulerBubbleStyle setTimeZoneTextColor(int timeZoneTextColor) {
        this.timeZoneTextColor = timeZoneTextColor;
        return this;
    }

    public int getGlobeIconTint() {
        return globeIconTint;
    }

    public SchedulerBubbleStyle setGlobeIconTint(int globeIconTint) {
        this.globeIconTint = globeIconTint;
        return this;
    }

    public int getMoreTextColor() {
        return moreTextColor;
    }

    public SchedulerBubbleStyle setMoreTextColor(int moreTextColor) {
        this.moreTextColor = moreTextColor;
        return this;
    }

    public int getDurationTimeTextColor() {
        return durationTimeTextColor;
    }

    public SchedulerBubbleStyle setDurationTimeTextColor(int durationTimeTextColor) {
        this.durationTimeTextColor = durationTimeTextColor;
        return this;
    }

    public int getTimeZoneTextAppearance() {
        return timeZoneTextAppearance;
    }

    public SchedulerBubbleStyle setTimeZoneTextAppearance(int timeZoneTextAppearance) {
        this.timeZoneTextAppearance = timeZoneTextAppearance;
        return this;
    }

    public int getMoreTextAppearance() {
        return moreTextAppearance;
    }

    public SchedulerBubbleStyle setMoreTextAppearance(int moreTextAppearance) {
        this.moreTextAppearance = moreTextAppearance;
        return this;
    }

    public int getDurationTimeTextAppearance() {
        return durationTimeTextAppearance;
    }

    public SchedulerBubbleStyle setDurationTimeTextAppearance(int durationTimeTextAppearance) {
        this.durationTimeTextAppearance = durationTimeTextAppearance;
        return this;
    }

    public int getDisableColor() {
        return disableColor;
    }

    public SchedulerBubbleStyle setDisableColor(int disableColor) {
        this.disableColor = disableColor;
        return this;
    }

    public int getNameColor() {
        return nameColor;
    }

    public SchedulerBubbleStyle setNameColor(int nameColor) {
        this.nameColor = nameColor;
        return this;
    }

    public int getQuickSlotAvailableAppearance() {
        return quickSlotAvailableAppearance;
    }

    public SchedulerBubbleStyle setQuickSlotAvailableAppearance(int quickSlotAvailableAppearance) {
        this.quickSlotAvailableAppearance = quickSlotAvailableAppearance;
        return this;
    }

    public int getQuickSlotAvailableTextColor() {
        return quickSlotAvailableTextColor;
    }

    public SchedulerBubbleStyle setQuickSlotAvailableTextColor(int quickSlotAvailableTextColor) {
        this.quickSlotAvailableTextColor = quickSlotAvailableTextColor;
        return this;
    }

    public int getNameAppearance() {
        return nameAppearance;
    }

    public SchedulerBubbleStyle setNameAppearance(int nameAppearance) {
        this.nameAppearance = nameAppearance;
        return this;
    }
}
