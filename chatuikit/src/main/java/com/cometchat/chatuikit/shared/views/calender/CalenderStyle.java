package com.cometchat.chatuikit.shared.views.calender;

import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.models.BaseStyle;

public class CalenderStyle extends BaseStyle {
    private static final String TAG = CalenderStyle.class.getSimpleName();

    private int titleTextColor;
    private int titleTextAppearance;

    public CalenderStyle setTitleTextColor(@ColorInt int titleTextColor) {
        this.titleTextColor = titleTextColor;
        return this;
    }

    public CalenderStyle setTitleTextAppearance(@StyleRes int titleTextAppearance) {
        this.titleTextAppearance = titleTextAppearance;
        return this;
    }

    @Override
    public CalenderStyle setBackgroundColor(@ColorInt int backgroundColor) {
        super.setBackgroundColor(backgroundColor);
        return this;
    }

    @Override
    public CalenderStyle setBackground(Drawable drawableBackground) {
        super.setBackground(drawableBackground);
        return this;
    }


    @Override
    public CalenderStyle setCornerRadius(float cornerRadius) {
        super.setCornerRadius(cornerRadius);
        return this;
    }

    @Override
    public CalenderStyle setStrokeWidth(int strokeWidth) {
        super.setStrokeWidth(strokeWidth);
        return this;
    }

    @Override
    public CalenderStyle setStrokeColor(@ColorInt int strokeColor) {
        super.setStrokeColor(strokeColor);
        return this;
    }

    @Override
    public CalenderStyle setActiveBackground(@ColorInt int activeBackground) {
        Log.i(TAG, "setActiveBackground: not supported");
        return this;
    }

    public int getTitleTextAppearance() {
        return titleTextAppearance;
    }

    public int getTitleTextColor() {
        return titleTextColor;
    }
}
