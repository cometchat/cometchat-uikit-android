package com.cometchat.chatuikit.shared.models;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;

/**
 * BaseStyle class represents the common style attributes for views in CometChat
 * UI Kit. It contains attributes for background color, active background color,
 * drawable background, corner radius, border width and border color.
 */
public class BaseStyle {
    private static final String TAG = BaseStyle.class.getSimpleName();
    private @ColorInt int backgroundColor;
    private @ColorInt int activeBackground;
    private Drawable drawableBackground;
    private float cornerRadius = -1;
    private @Dimension int strokeWidth = -1;
    private @ColorInt int strokeColor;

    /**
     * Sets the background color of the view.
     *
     * @param backgroundColor The background color of the view.
     */
    public BaseStyle setBackgroundColor(@ColorInt int backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    /**
     * Sets the drawable background of the view.
     *
     * @param drawableBackground The drawable background of the view.
     */
    public BaseStyle setBackground(Drawable drawableBackground) {
        this.drawableBackground = drawableBackground;
        return this;
    }

    /**
     * Sets the corner radius of the view.
     *
     * @param cornerRadius The corner radius of the view.
     */
    public BaseStyle setCornerRadius(float cornerRadius) {
        this.cornerRadius = cornerRadius;
        return this;
    }

    /**
     * Sets the width of the view border.
     *
     * @param strokeWidth The width of the view border.
     */
    public BaseStyle setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
        return this;
    }

    /**
     * Sets the color of the view border.
     *
     * @param strokeColor The color of the view border.
     */
    public BaseStyle setStrokeColor(@ColorInt int strokeColor) {
        this.strokeColor = strokeColor;
        return this;
    }

    /**
     * Sets the background color of the view when it is in active state.
     *
     * @param activeBackground The background color of the view when it is in active state.
     */
    public BaseStyle setActiveBackground(@ColorInt int activeBackground) {
        this.activeBackground = activeBackground;
        return this;
    }

    public int getBackground() {
        return backgroundColor;
    }

    public Drawable getDrawableBackground() {
        return drawableBackground;
    }

    public float getCornerRadius() {
        return cornerRadius;
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public int getStrokeColor() {
        return strokeColor;
    }

    public int getActiveBackground() {
        return activeBackground;
    }
}
