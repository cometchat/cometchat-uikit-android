package com.cometchat.chatuikit.shared.formatters.style;

import android.graphics.Typeface;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;

public class PromptTextStyle {
    private static final String TAG = PromptTextStyle.class.getSimpleName();


    private int color;
    private Typeface textAppearance;
    private @ColorInt int backgroundColor;
    private int textSize;

    public PromptTextStyle setColor(@ColorInt int color) {
        this.color = color;
        return this;
    }

    public PromptTextStyle setTextAppearance(Typeface textAppearance) {
        this.textAppearance = textAppearance;
        return this;
    }

    public PromptTextStyle setBackgroundColor(@ColorInt int backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public PromptTextStyle setTextSize(@Dimension int size) {
        this.textSize = size;
        return this;
    }

    public int getColor() {
        return color;
    }

    public Typeface getTextAppearance() {
        return textAppearance;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getTextSize() {
        return textSize;
    }
}
