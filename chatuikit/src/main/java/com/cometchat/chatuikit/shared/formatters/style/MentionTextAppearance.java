package com.cometchat.chatuikit.shared.formatters.style;

import android.graphics.Typeface;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class MentionTextAppearance {
    private static final String TAG = MentionTextAppearance.class.getSimpleName();
    private int textColor;
    private Typeface textAppearance;
    private @ColorInt int textBackgroundColor;
    private int loggedInUserTextColor;
    private Typeface loggedInUserTextAppearance;
    private @ColorInt int loggedInUserTextBackgroundColor;

    public MentionTextAppearance setTextColor(@ColorInt int textColor) {
        this.textColor = textColor;
        return this;
    }

    public MentionTextAppearance setTextAppearance(Typeface textAppearance) {
        this.textAppearance = textAppearance;
        return this;
    }

    @NonNull
    public MentionTextAppearance setTextBackgroundColor(@ColorInt int textBackgroundColor) {
        this.textBackgroundColor = textBackgroundColor;
        return this;
    }

    public MentionTextAppearance setLoggedInUserTextColor(int loggedInUserTextColor) {
        this.loggedInUserTextColor = loggedInUserTextColor;
        return this;
    }

    public MentionTextAppearance setLoggedInUserTextAppearance(Typeface loggedInUserTextAppearance) {
        this.loggedInUserTextAppearance = loggedInUserTextAppearance;
        return this;
    }

    public MentionTextAppearance setLoggedInUserTextBackgroundColor(int loggedInUserTextBackgroundColor) {
        this.loggedInUserTextBackgroundColor = loggedInUserTextBackgroundColor;
        return this;
    }

    public int getTextColor() {
        return textColor;
    }

    public Typeface getTextAppearance() {
        return textAppearance;
    }

    public int getTextBackgroundColor() {
        return textBackgroundColor;
    }

    public int getLoggedInUserTextColor() {
        return loggedInUserTextColor;
    }

    public Typeface getLoggedInUserTextAppearance() {
        return loggedInUserTextAppearance;
    }

    public int getLoggedInUserTextBackgroundColor() {
        return loggedInUserTextBackgroundColor;
    }
}
