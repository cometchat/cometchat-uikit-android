package com.cometchat.chatuikit;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;

public class CometChatTheme {
    private static final String TAG = CometChatTheme.class.getSimpleName();

    /**
     * Retrieves the primary color defined in the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The primary color of the current theme, or 0 if not defined.
     */
    public static @ColorInt int getPrimaryColor(Context context) {
        return getColorFromAttr(context, R.attr.cometchatPrimaryColor);
    }

    /**
     * Retrieves an extended primary color defined in the current theme. If the
     * color is not defined, it blends the primary color with either white (for day
     * mode) or black (for night mode) according to the specified percentages.
     *
     * @param context         The context used to access the current theme.
     * @param attr            The attribute resource ID of the extended primary color.
     * @param dayPercentage   The blending percentage to use for day mode.
     * @param nightPercentage The blending percentage to use for night mode.
     * @return The extended primary color, or a blended color if not defined.
     */
    public static @ColorInt int getExtendedPrimaryColor(Context context, @AttrRes int attr, double dayPercentage, double nightPercentage) {
        @ColorInt int color = getColorFromAttr(context, attr);
        if (color == 0) {
            int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            int blendingColor = (currentNightMode == Configuration.UI_MODE_NIGHT_NO) ? Color.WHITE : Color.BLACK;
            double percentage = (currentNightMode == Configuration.UI_MODE_NIGHT_NO) ? dayPercentage : nightPercentage;
            return getBlendedColor(getPrimaryColor(context), blendingColor, percentage);
        }
        return color;
    }

    /**
     * Retrieves the first extended primary color defined in the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The first extended primary color, or a blended color if not defined.
     */
    public static @ColorInt int getExtendedPrimaryColor50(Context context) {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor50, 0.96, 0.80);
    }

    /**
     * Retrieves the second extended primary color defined in the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The second extended primary color, or a blended color if not defined.
     */
    public static @ColorInt int getExtendedPrimaryColor100(Context context) {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor100, 0.88, 0.72);
    }

    /**
     * Retrieves the third extended primary color defined in the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The second extended primary color, or a blended color if not defined.
     */
    public static @ColorInt int getExtendedPrimaryColor200(Context context) {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor200, 0.77, 0.64);
    }

    /**
     * Retrieves the fourth extended primary color defined in the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The second extended primary color, or a blended color if not defined.
     */
    public static @ColorInt int getExtendedPrimaryColor300(Context context) {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor300, 0.66, 0.56);
    }

    /**
     * Retrieves the fifth extended primary color defined in the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The second extended primary color, or a blended color if not defined.
     */
    public static @ColorInt int getExtendedPrimaryColor400(Context context) {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor400, 0.55, 0.48);
    }

    /**
     * Retrieves the sixth extended primary color defined in the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The second extended primary color, or a blended color if not defined.
     */
    public static @ColorInt int getExtendedPrimaryColor500(Context context) {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor500, 0.44, 0.40);
    }

    /**
     * Retrieves the seventh extended primary color defined in the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The second extended primary color, or a blended color if not defined.
     */
    public static @ColorInt int getExtendedPrimaryColor600(Context context) {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor600, 0.33, 0.32);
    }

    /**
     * Retrieves the eighth extended primary color defined in the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The second extended primary color, or a blended color if not defined.
     */
    public static @ColorInt int getExtendedPrimaryColor700(Context context) {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor700, 0.22, 0.24);
    }

    /**
     * Retrieves the ninth extended primary color defined in the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The second extended primary color, or a blended color if not defined.
     */
    public static @ColorInt int getExtendedPrimaryColor800(Context context) {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor800, 0.11, 0.16);
    }

    /**
     * Retrieves the tenth extended primary color defined in the current theme. In
     * day mode, it blends the primary color with black; in night mode, it blends
     * with white.
     *
     * @param context The context used to access the current theme.
     * @return The tenth extended primary color, or a blended color if not defined.
     */
    public static @ColorInt int getExtendedPrimaryColor900(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        int blendingColor = (currentNightMode == Configuration.UI_MODE_NIGHT_NO) ? Color.BLACK : Color.WHITE;
        double percentage = (currentNightMode == Configuration.UI_MODE_NIGHT_NO) ? 0.11 : 0.08;
        return getBlendedColor(getPrimaryColor(context), blendingColor, percentage);
    }

    /**
     * Retrieves the neutral color at intensity 50 from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The neutral color 50, or 0 if not defined.
     */
    public static @ColorInt int getNeutralColor50(Context context) {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor50);
    }

    /**
     * Retrieves the neutral color at intensity 100 from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The neutral color 100, or 0 if not defined.
     */
    public static @ColorInt int getNeutralColor100(Context context) {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor100);
    }

    /**
     * Retrieves the neutral color at intensity 200 from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The neutral color 200, or 0 if not defined.
     */
    public static @ColorInt int getNeutralColor200(Context context) {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor200);
    }

    /**
     * Retrieves the neutral color at intensity 300 from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The neutral color 300, or 0 if not defined.
     */
    public static @ColorInt int getNeutralColor300(Context context) {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor300);
    }

    /**
     * Retrieves the neutral color at intensity 400 from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The neutral color 400, or 0 if not defined.
     */
    public static @ColorInt int getNeutralColor400(Context context) {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor400);
    }

    /**
     * Retrieves the neutral color at intensity 500 from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The neutral color 500, or 0 if not defined.
     */
    public static @ColorInt int getNeutralColor500(Context context) {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor500);
    }

    /**
     * Retrieves the neutral color at intensity 600 from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The neutral color 600, or 0 if not defined.
     */
    public static @ColorInt int getNeutralColor600(Context context) {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor600);
    }

    /**
     * Retrieves the neutral color at intensity 700 from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The neutral color 700, or 0 if not defined.
     */
    public static @ColorInt int getNeutralColor700(Context context) {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor700);
    }

    /**
     * Retrieves the neutral color at intensity 800 from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The neutral color 800, or 0 if not defined.
     */
    public static @ColorInt int getNeutralColor800(Context context) {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor800);
    }

    /**
     * Retrieves the neutral color at intensity 900 from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The neutral color 900, or 0 if not defined.
     */
    public static @ColorInt int getNeutralColor900(Context context) {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor900);
    }

    /**
     * Retrieves the success color from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The success color, or 0 if not defined.
     */
    public static @ColorInt int getSuccessColor(Context context) {
        return getColorFromAttr(context, R.attr.cometchatSuccessColor);
    }

    /**
     * Retrieves the error color from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The error color, or 0 if not defined.
     */
    public static @ColorInt int getErrorColor(Context context) {
        return getColorFromAttr(context, R.attr.cometchatErrorColor);
    }

    /**
     * Retrieves the message read color from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The message read color, or 0 if not defined.
     */
    public static @ColorInt int getMessageReadColor(Context context) {
        return getColorFromAttr(context, R.attr.cometchatMessageReadColor);
    }

    /**
     * Retrieves the warning color from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The warning color, or 0 if not defined.
     */
    public static @ColorInt int getWarningColor(Context context) {
        return getColorFromAttr(context, R.attr.cometchatWarningColor);
    }

    /**
     * Retrieves the info color from the current theme.
     *
     * @param context The context used to access the current theme.
     * @return The info color, or 0 if not defined.
     */
    public static @ColorInt int getInfoColor(Context context) {
        return getColorFromAttr(context, R.attr.cometchatInfoColor);
    }

    /**
     * Retrieves the background color 1 from the current theme. If the color is not
     * defined, it defaults to the neutral color 100.
     *
     * @param context The context used to access the current theme.
     * @return The background color 1, or the neutral color 100 if not defined.
     */
    public static @ColorInt int getBackgroundColor1(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatBackgroundColor1);
        if (color == 0) return getNeutralColor50(context);
        else return color;
    }

    /**
     * Retrieves the background color 2 from the current theme. If the color is not
     * defined, it defaults to the neutral color 200.
     *
     * @param context The context used to access the current theme.
     * @return The background color 2, or the neutral color 200 if not defined.
     */
    public static @ColorInt int getBackgroundColor2(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatBackgroundColor2);
        if (color == 0) return getNeutralColor100(context);
        else return color;
    }

    /**
     * Retrieves the background color 3 from the current theme. If the color is not
     * defined, it defaults to the neutral color 300.
     *
     * @param context The context used to access the current theme.
     * @return The background color 3, or the neutral color 300 if not defined.
     */
    public static @ColorInt int getBackgroundColor3(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatBackgroundColor3);
        if (color == 0) return getNeutralColor200(context);
        else return color;
    }

    /**
     * Retrieves the background color 4 from the current theme. If the color is not
     * defined, it defaults to the neutral color 400.
     *
     * @param context The context used to access the current theme.
     * @return The background color 4, or the neutral color 400 if not defined.
     */
    public static @ColorInt int getBackgroundColor4(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatBackgroundColor4);
        if (color == 0) return getNeutralColor300(context);
        else return color;
    }

    /**
     * Retrieves the default stroke color from the current theme. If the color is
     * not defined, it defaults to the neutral color 400.
     *
     * @param context The context used to access the current theme.
     * @return The default stroke color, or the neutral color 400 if not defined.
     */
    public static @ColorInt int getStrokeColorDefault(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatStrokeColorDefault);
        if (color == 0) return getNeutralColor300(context);
        else return color;
    }

    /**
     * Retrieves the light stroke color from the current theme. If the color is not
     * defined, it defaults to the neutral color 300.
     *
     * @param context The context used to access the current theme.
     * @return The light stroke color, or the neutral color 300 if not defined.
     */
    public static @ColorInt int getStrokeColorLight(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatStrokeColorLight);
        if (color == 0) return getNeutralColor200(context);
        else return color;
    }

    /**
     * Retrieves the dark stroke color from the current theme. If the color is not
     * defined, it defaults to the neutral color 500.
     *
     * @param context The context used to access the current theme.
     * @return The dark stroke color, or the neutral color 500 if not defined.
     */
    public static @ColorInt int getStrokeColorDark(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatStrokeColorDark);
        if (color == 0) return getNeutralColor400(context);
        else return color;
    }

    /**
     * Retrieves the stroke highlight color from the current theme. If the color is
     * not defined, it defaults to the primary color.
     *
     * @param context The context used to access the current theme.
     * @return The stroke highlight color, or the primary color if not defined.
     */
    public static @ColorInt int getStrokeColorHighlight(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatStrokeColorHighlight);
        if (color == 0) return getPrimaryColor(context);
        else return color;
    }

    /**
     * Retrieves the primary text color from the current theme. If the color is not
     * defined, it defaults to the neutral color 1000.
     *
     * @param context The context used to access the current theme.
     * @return The primary text color, or the neutral color 1000 if not defined.
     */
    public static @ColorInt int getTextColorPrimary(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatTextColorPrimary);
        if (color == 0) return getNeutralColor900(context);
        else return color;
    }

    /**
     * Retrieves the secondary text color from the current theme. If the color is
     * not defined, it defaults to the neutral color 700.
     *
     * @param context The context used to access the current theme.
     * @return The secondary text color, or the neutral color 700 if not defined.
     */
    public static @ColorInt int getTextColorSecondary(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatTextColorSecondary);
        if (color == 0) return getNeutralColor600(context);
        else return color;
    }

    /**
     * Retrieves the tertiary text color from the current theme. If the color is not
     * defined, it defaults to the neutral color 600.
     *
     * @param context The context used to access the current theme.
     * @return The tertiary text color, or the neutral color 600 if not defined.
     */
    public static @ColorInt int getTextColorTertiary(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatTextColorTertiary);
        if (color == 0) return getNeutralColor500(context);
        else return color;
    }

    /**
     * Retrieves the disabled text color from the current theme. If the color is not
     * defined, it defaults to the neutral color 500.
     *
     * @param context The context used to access the current theme.
     * @return The disabled text color, or the neutral color 500 if not defined.
     */
    public static @ColorInt int getTextColorDisabled(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatTextColorDisabled);
        if (color == 0) return getNeutralColor400(context);
        else return color;
    }

    /**
     * Retrieves the white text color from the current theme. If the color is not
     * defined, it defaults to the neutral color 100.
     *
     * @param context The context used to access the current theme.
     * @return The white text color, or the neutral color 100 if not defined.
     */
    public static @ColorInt int getTextColorWhite(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatTextColorWhite);
        if (color == 0) return getNeutralColor50(context);
        else return color;
    }

    /**
     * Retrieves the color white from the resources.
     *
     * @param context the context used to access resources
     * @return the color white as an integer value
     */
    public static @ColorInt int getColorWhite(Context context) {
        return context.getResources().getColor(R.color.cometchat_color_white, context.getTheme());
    }

    /**
     * Retrieves the color black from the resources.
     *
     * @param context the context used to access resources
     * @return the color black as an integer value
     */
    public static @ColorInt int getColorBlack(Context context) {
        return context.getResources().getColor(R.color.cometchat_color_black, context.getTheme());
    }

    /**
     * Retrieves the color transparent from the resources.
     *
     * @param context the context used to access resources
     * @return the color transparent as an integer value
     */
    public static @ColorInt int getColorTransparent(Context context) {
        return context.getResources().getColor(R.color.cometchat_color_transparent, context.getTheme());
    }

    /**
     * Retrieves the text highlight color from the current theme. If the color is
     * not defined, it defaults to the primary color.
     *
     * @param context The context used to access the current theme.
     * @return The text highlight color, or the primary color if not defined.
     */
    public static @ColorInt int getTextColorHighlight(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatTextColorHighlight);
        if (color == 0) return getPrimaryColor(context);
        else return color;
    }

    /**
     * Retrieves the primary icon color from the current theme. If the color is not
     * defined, it defaults to the neutral color 1000.
     *
     * @param context The context used to access the current theme.
     * @return The primary icon color, or the neutral color 1000 if not defined.
     */
    public static @ColorInt int getIconTintPrimary(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatIconTintPrimary);
        if (color == 0) return getNeutralColor900(context);
        else return color;
    }

    /**
     * Retrieves the secondary icon color from the current theme. If the color is
     * not defined, it defaults to the neutral color 600.
     *
     * @param context The context used to access the current theme.
     * @return The secondary icon color, or the neutral color 600 if not defined.
     */
    public static @ColorInt int getIconTintSecondary(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatIconTintSecondary);
        if (color == 0) return getNeutralColor500(context);
        else return color;
    }

    /**
     * Retrieves the tertiary icon color from the current theme. If the color is not
     * defined, it defaults to the neutral color 500.
     *
     * @param context The context used to access the current theme.
     * @return The tertiary icon color, or the neutral color 500 if not defined.
     */
    public static @ColorInt int getIconTintTertiary(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatIconTintTertiary);
        if (color == 0) return getNeutralColor400(context);
        else return color;
    }

    /**
     * Retrieves the white icon color from the current theme. If the color is not
     * defined, it defaults to the neutral color 100.
     *
     * @param context The context used to access the current theme.
     * @return The white icon color, or the neutral color 100 if not defined.
     */
    public static @ColorInt int getIconTintWhite(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatIconTintWhite);
        if (color == 0) return getNeutralColor50(context);
        else return color;
    }

    /**
     * Retrieves the icon highlight color from the current theme. If the color is
     * not defined, it defaults to the primary color.
     *
     * @param context The context used to access the current theme.
     * @return The icon highlight color, or the primary color if not defined.
     */
    public static @ColorInt int getIconTintHighlight(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatIconTintHighlight);
        if (color == 0) return getPrimaryColor(context);
        else return color;
    }

    /**
     * Retrieves the button background color from the current theme. If the color is
     * not defined, it defaults to the primary color.
     *
     * @param context The context used to access the current theme.
     * @return The button background color, or the primary color if not defined.
     */
    public static @ColorInt int getButtonBackground(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatPrimaryButtonBackgroundColor);
        if (color == 0) return getPrimaryColor(context);
        else return color;
    }

    /**
     * Retrieves the button icon color from the current theme. If the color is not
     * defined, it defaults to the neutral color 50.
     *
     * @param context The context used to access the current theme.
     * @return The button icon color, or the neutral color 50 if not defined.
     */
    public static @ColorInt int getButtonIconTint(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatPrimaryButtonIconTint);
        if (color == 0)
            return context.getResources().getColor(R.color.cometchat_color_white, context.getTheme());
        else return color;
    }

    /**
     * Retrieves the button text color from the current theme. If the color is not
     * defined, it defaults to the neutral color 50.
     *
     * @param context The context used to access the current theme.
     * @return The button text color, or the neutral color 50 if not defined.
     */
    public static @ColorInt int getButtonText(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatPrimaryButtonTextColor);
        if (color == 0) return getNeutralColor50(context);
        else return color;
    }

    /**
     * Retrieves the secondary button background color from the current theme. If
     * the color is not defined, it defaults to the neutral color 900.
     *
     * @param context The context used to access the current theme.
     * @return The secondary button background color, or the neutral color 900 if
     * not defined.
     */
    public static @ColorInt int getSecondaryButtonBackground(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatSecondaryButtonBackgroundColor);
        if (color == 0) return getNeutralColor900(context);
        else return color;
    }

    /**
     * Retrieves the secondary button icon color from the current theme. If the
     * color is not defined, it defaults to the neutral color 900.
     *
     * @param context The context used to access the current theme.
     * @return The secondary button icon color, or the neutral color 900 if not
     * defined.
     */
    public static @ColorInt int getSecondaryButtonIcon(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatSecondaryButtonIconTint);
        if (color == 0) return getNeutralColor900(context);
        else return color;
    }

    /**
     * Retrieves the secondary button text color from the current theme. If the
     * color is not defined, it defaults to the neutral color 900.
     *
     * @param context The context used to access the current theme.
     * @return The secondary button text color, or the neutral color 900 if not
     * defined.
     */
    public static @ColorInt int getSecondaryButtonText(Context context) {
        @ColorInt int color = getColorFromAttr(context, R.attr.cometchatSecondaryButtonTextColor);
        if (color == 0) return getNeutralColor900(context);
        else return color;
    }

    /**
     * Retrieves the text appearance resource ID for the title text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for the title, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceTitleRegular(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceTitleRegular);
    }

    /**
     * Retrieves the text appearance resource ID for the title text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for the title, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceTitleMedium(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceTitleMedium);
    }

    /**
     * Retrieves the text appearance resource ID for the title text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for the title, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceTitleBold(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceTitleBold);
    }

    /**
     * Retrieves the text appearance resource ID for the heading 1 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for heading 1, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceHeading1Regular(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading1Regular);
    }

    /**
     * Retrieves the text appearance resource ID for the heading 1 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for heading 1, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceHeading1Medium(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading1Medium);
    }

    /**
     * Retrieves the text appearance resource ID for the heading 1 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for heading 1, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceHeading1Bold(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading1Bold);
    }

    /**
     * Retrieves the text appearance resource ID for the heading 2 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for heading 2, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceHeading2Regular(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading2Regular);
    }

    /**
     * Retrieves the text appearance resource ID for the heading 2 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for heading 2, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceHeading2Medium(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading2Medium);
    }

    /**
     * Retrieves the text appearance resource ID for the heading 2 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for heading 2, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceHeading2Bold(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading2Bold);
    }

    /**
     * Retrieves the text appearance resource ID for the heading 3 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for heading 3, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceHeading3Regular(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading3Regular);
    }

    /**
     * Retrieves the text appearance resource ID for the heading 3 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for heading 3, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceHeading3Medium(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading3Medium);
    }

    /**
     * Retrieves the text appearance resource ID for the heading 3 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for heading 3, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceHeading3Bold(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading3Bold);
    }

    /**
     * Retrieves the text appearance resource ID for the heading 4 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for heading 4, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceHeading4Regular(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading4Regular);
    }

    /**
     * Retrieves the text appearance resource ID for the heading 4 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for heading 4, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceHeading4Medium(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading4Medium);
    }

    /**
     * Retrieves the text appearance resource ID for the heading 4 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for heading 4, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceHeading4Bold(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading4Bold);
    }

    /**
     * Retrieves the text appearance resource ID for the body text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for body text, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceBodyRegular(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceBodyRegular);
    }

    /**
     * Retrieves the text appearance resource ID for the body text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for body text, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceBodyMedium(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceBodyMedium);
    }

    /**
     * Retrieves the text appearance resource ID for the body text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for body text, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceBodyBold(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceBodyBold);
    }

    /**
     * Retrieves the text appearance resource ID for the caption 1 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for caption 1, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceCaption1Regular(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceCaption1Regular);
    }

    /**
     * Retrieves the text appearance resource ID for the caption 1 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for caption 1, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceCaption1Medium(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceCaption1Medium);
    }

    /**
     * Retrieves the text appearance resource ID for the caption 1 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for caption 1, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceCaption1Bold(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceCaption1Bold);
    }

    /**
     * Retrieves the text appearance resource ID for the caption 2 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for caption 2, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceCaption2Regular(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceCaption2Regular);
    }

    /**
     * Retrieves the text appearance resource ID for the caption 2 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for caption 2, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceCaption2Medium(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceCaption2Medium);
    }

    /**
     * Retrieves the text appearance resource ID for the caption 2 text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for caption 2, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceCaption2Bold(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceCaption2Bold);
    }

    /**
     * Retrieves the text appearance resource ID for the button text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for button text, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceButtonRegular(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceButtonRegular);
    }

    /**
     * Retrieves the text appearance resource ID for the button text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for button text, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceButtonMedium(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceButtonMedium);
    }

    /**
     * Retrieves the text appearance resource ID for the button text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for button text, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceButtonBold(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceButtonBold);
    }

    /**
     * Retrieves the text appearance resource ID for the link text style.
     *
     * @param context The context used to access the current theme.
     * @return The text appearance resource ID for link text, or 0 if not defined.
     */
    public static @StyleRes int getTextAppearanceLinkRegular(Context context) {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceLinkRegular);
    }

    /**
     * Retrieves the color associated with a specific attribute from the current
     * theme.
     *
     * @param context The context used to access the current theme.
     * @param attr    The attribute resource ID of the color to retrieve.
     * @return The color associated with the specified attribute, or 0 if not
     * defined.
     */
    private static @ColorInt int getColorFromAttr(Context context, @AttrRes int attr) {
        if (context == null) return 0;
        try (TypedArray typedArray = context.obtainStyledAttributes(new int[]{attr})) {
            return typedArray.getColor(0, 0);
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * Retrieves the text appearance resource ID from the specified attribute. If
     * the attribute is not defined or an error occurs, returns 0.
     *
     * @param context The context used to access the current theme.
     * @param attr    The attribute resource ID that specifies the text appearance.
     * @return The text appearance resource ID, or 0 if not defined or an error
     * occurs.
     */
    private static @StyleRes int getTextAppearanceFromAttr(Context context, @AttrRes int attr) {
        if (context == null) return 0;
        try (TypedArray typedArray = context.obtainStyledAttributes(new int[]{attr})) {
            return typedArray.getResourceId(0, 0);
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * Blends two colors together based on a specified percentage.
     *
     * @param baseColor  The base color to blend.
     * @param blendColor The color to blend with the base color.
     * @param percentage The percentage of the blend color to apply (0.0 to 1.0).
     * @return The resulting blended color.
     */
    private static int getBlendedColor(int baseColor, int blendColor, double percentage) {
        int r = (int) Math.round(Color.red(baseColor) * (1 - percentage) + Color.red(blendColor) * percentage);
        int g = (int) Math.round(Color.green(baseColor) * (1 - percentage) + Color.green(blendColor) * percentage);
        int b = (int) Math.round(Color.blue(baseColor) * (1 - percentage) + Color.blue(blendColor) * percentage);
        return Color.rgb(r, g, b);
    }
}
