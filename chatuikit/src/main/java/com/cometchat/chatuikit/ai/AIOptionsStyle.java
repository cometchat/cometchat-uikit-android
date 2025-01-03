package com.cometchat.chatuikit.ai;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.models.BaseStyle;

public class AIOptionsStyle extends BaseStyle {
    private static final String TAG = AIOptionsStyle.class.getSimpleName();

    private @StyleRes int listItemTextAppearance;
    private @ColorInt int listItemTextColor;
    private @ColorInt int optionsSeparatorColor;
    private @ColorInt int listItemBackgroundColor;
    private int listItemCornerRadius;

    public AIOptionsStyle setListItemTextAppearance(int listItemTextAppearance) {
        this.listItemTextAppearance = listItemTextAppearance;
        return this;
    }

    public AIOptionsStyle setListItemTextColor(int listItemTextColor) {
        this.listItemTextColor = listItemTextColor;
        return this;
    }

    public AIOptionsStyle setOptionsSeparatorColor(int optionsSeparatorColor) {
        this.optionsSeparatorColor = optionsSeparatorColor;
        return this;
    }

    public int getListItemTextAppearance() {
        return listItemTextAppearance;
    }

    public int getListItemTextColor() {
        return listItemTextColor;
    }

    public int getOptionsSeparatorColor() {
        return optionsSeparatorColor;
    }

    public AIOptionsStyle setListItemBackgroundColor(int listItemBackgroundColor) {
        this.listItemBackgroundColor = listItemBackgroundColor;
        return this;
    }

    public int getListItemBackgroundColor() {
        return listItemBackgroundColor;
    }

    public AIOptionsStyle setListItemCornerRadius(int cornerRadius) {
        this.listItemCornerRadius = cornerRadius;
        return this;
    }

    public int getListItemCornerRadius() {
        return listItemCornerRadius;
    }

    @Override
    public AIOptionsStyle setBackgroundColor(int backgroundColor) {
        super.setBackgroundColor(backgroundColor);
        return this;
    }

    @Override
    public AIOptionsStyle setBackground(Drawable drawableBackground) {
        super.setBackground(drawableBackground);
        return this;
    }

    @Override
    public AIOptionsStyle setCornerRadius(float cornerRadius) {
        super.setCornerRadius(cornerRadius);
        return this;
    }

    @Override
    public AIOptionsStyle setStrokeWidth(int strokeWidth) {
        super.setStrokeWidth(strokeWidth);
        return this;
    }

    @Override
    public AIOptionsStyle setStrokeColor(int strokeColor) {
        super.setStrokeColor(strokeColor);
        return this;
    }
}
