package com.cometchat.uikit.kotlin.theme

import android.content.Context
import android.content.res.Configuration
import android.content.res.TypedArray
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.annotation.StyleRes
import com.cometchat.uikit.kotlin.R
import kotlin.math.roundToInt

/**
 * CometChatTheme provides programmatic access to theme colors, typography, and fonts
 * defined in the CometChat UIKit theme attributes.
 *
 * This class mirrors the functionality of the Java-based CometChatTheme from the chatuikit module,
 * providing a Kotlin-native implementation for the chatuikit-kotlin module.
 */
public object CometChatTheme {
    private val themeAttributeCache = mutableMapOf<Int, Int>()

    // region Primary Color
    public fun setPrimaryColor(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatPrimaryColor] = color
    }

    @ColorInt
    public fun getPrimaryColor(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatPrimaryColor)
    }
    // endregion

    // region Extended Primary Colors
    public fun setExtendedPrimaryColor50(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatExtendedPrimaryColor50] = color
    }

    @ColorInt
    public fun getExtendedPrimaryColor50(context: Context): Int {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor50, 0.96, 0.80)
    }

    public fun setExtendedPrimaryColor100(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatExtendedPrimaryColor100] = color
    }

    @ColorInt
    public fun getExtendedPrimaryColor100(context: Context): Int {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor100, 0.88, 0.72)
    }

    public fun setExtendedPrimaryColor200(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatExtendedPrimaryColor200] = color
    }

    @ColorInt
    public fun getExtendedPrimaryColor200(context: Context): Int {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor200, 0.77, 0.64)
    }

    public fun setExtendedPrimaryColor300(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatExtendedPrimaryColor300] = color
    }

    @ColorInt
    public fun getExtendedPrimaryColor300(context: Context): Int {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor300, 0.66, 0.56)
    }

    public fun setExtendedPrimaryColor400(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatExtendedPrimaryColor400] = color
    }

    @ColorInt
    public fun getExtendedPrimaryColor400(context: Context): Int {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor400, 0.55, 0.48)
    }

    public fun setExtendedPrimaryColor500(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatExtendedPrimaryColor500] = color
    }

    @ColorInt
    public fun getExtendedPrimaryColor500(context: Context): Int {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor500, 0.44, 0.40)
    }

    public fun setExtendedPrimaryColor600(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatExtendedPrimaryColor600] = color
    }

    @ColorInt
    public fun getExtendedPrimaryColor600(context: Context): Int {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor600, 0.33, 0.32)
    }

    public fun setExtendedPrimaryColor700(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatExtendedPrimaryColor700] = color
    }

    @ColorInt
    public fun getExtendedPrimaryColor700(context: Context): Int {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor700, 0.22, 0.24)
    }

    public fun setExtendedPrimaryColor800(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatExtendedPrimaryColor800] = color
    }

    @ColorInt
    public fun getExtendedPrimaryColor800(context: Context): Int {
        return getExtendedPrimaryColor(context, R.attr.cometchatExtendedPrimaryColor800, 0.11, 0.16)
    }

    public fun setExtendedPrimaryColor900(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatExtendedPrimaryColor900] = color
    }

    @ColorInt
    public fun getExtendedPrimaryColor900(context: Context): Int {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val blendingColor = if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) Color.BLACK else Color.WHITE
        val percentage = if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) 0.11 else 0.08
        return getBlendedColor(getPrimaryColor(context), blendingColor, percentage)
    }
    // endregion

    // region Neutral Colors
    public fun setNeutralColor50(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatNeutralColor50] = color
    }

    @ColorInt
    public fun getNeutralColor50(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor50)
    }

    public fun setNeutralColor100(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatNeutralColor100] = color
    }

    @ColorInt
    public fun getNeutralColor100(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor100)
    }

    public fun setNeutralColor200(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatNeutralColor200] = color
    }

    @ColorInt
    public fun getNeutralColor200(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor200)
    }

    public fun setNeutralColor300(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatNeutralColor300] = color
    }

    @ColorInt
    public fun getNeutralColor300(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor300)
    }

    public fun setNeutralColor400(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatNeutralColor400] = color
    }

    @ColorInt
    public fun getNeutralColor400(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor400)
    }

    public fun setNeutralColor500(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatNeutralColor500] = color
    }

    @ColorInt
    public fun getNeutralColor500(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor500)
    }

    public fun setNeutralColor600(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatNeutralColor600] = color
    }

    @ColorInt
    public fun getNeutralColor600(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor600)
    }

    public fun setNeutralColor700(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatNeutralColor700] = color
    }

    @ColorInt
    public fun getNeutralColor700(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor700)
    }

    public fun setNeutralColor800(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatNeutralColor800] = color
    }

    @ColorInt
    public fun getNeutralColor800(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor800)
    }

    public fun setNeutralColor900(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatNeutralColor900] = color
    }

    @ColorInt
    public fun getNeutralColor900(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatNeutralColor900)
    }
    // endregion

    // region Alert Colors
    public fun setSuccessColor(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatSuccessColor] = color
    }

    @ColorInt
    public fun getSuccessColor(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatSuccessColor)
    }

    public fun setErrorColor(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatErrorColor] = color
    }

    @ColorInt
    public fun getErrorColor(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatErrorColor)
    }

    public fun setWarningColor(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatWarningColor] = color
    }

    @ColorInt
    public fun getWarningColor(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatWarningColor)
    }

    public fun setInfoColor(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatInfoColor] = color
    }

    @ColorInt
    public fun getInfoColor(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatInfoColor)
    }

    public fun setMessageReadColor(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatMessageReadColor] = color
    }

    @ColorInt
    public fun getMessageReadColor(context: Context): Int {
        return getColorFromAttr(context, R.attr.cometchatMessageReadColor)
    }
    // endregion

    // region Background Colors
    public fun setBackgroundColor1(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatBackgroundColor1] = color
    }

    @ColorInt
    public fun getBackgroundColor1(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatBackgroundColor1)
        return if (color == 0) getNeutralColor50(context) else color
    }

    public fun setBackgroundColor2(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatBackgroundColor2] = color
    }

    @ColorInt
    public fun getBackgroundColor2(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatBackgroundColor2)
        return if (color == 0) getNeutralColor100(context) else color
    }

    public fun setBackgroundColor3(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatBackgroundColor3] = color
    }

    @ColorInt
    public fun getBackgroundColor3(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatBackgroundColor3)
        return if (color == 0) getNeutralColor200(context) else color
    }

    public fun setBackgroundColor4(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatBackgroundColor4] = color
    }

    @ColorInt
    public fun getBackgroundColor4(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatBackgroundColor4)
        return if (color == 0) getNeutralColor300(context) else color
    }
    // endregion

    // region Stroke Colors
    public fun setStrokeColorDefault(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatStrokeColorDefault] = color
    }

    @ColorInt
    public fun getStrokeColorDefault(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatStrokeColorDefault)
        return if (color == 0) getNeutralColor300(context) else color
    }

    public fun setStrokeColorLight(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatStrokeColorLight] = color
    }

    @ColorInt
    public fun getStrokeColorLight(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatStrokeColorLight)
        return if (color == 0) getNeutralColor200(context) else color
    }

    public fun setStrokeColorDark(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatStrokeColorDark] = color
    }

    @ColorInt
    public fun getStrokeColorDark(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatStrokeColorDark)
        return if (color == 0) getNeutralColor400(context) else color
    }

    public fun setStrokeColorHighlight(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatStrokeColorHighlight] = color
    }

    @ColorInt
    public fun getStrokeColorHighlight(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatStrokeColorHighlight)
        return if (color == 0) getPrimaryColor(context) else color
    }

    // Border color aliases (for consistency with Compose naming)
    public fun setBorderColorLight(@ColorInt color: Int) {
        setStrokeColorLight(color)
    }

    @ColorInt
    public fun getBorderColorLight(context: Context): Int {
        return getStrokeColorLight(context)
    }

    public fun setBorderColorDefault(@ColorInt color: Int) {
        setStrokeColorDefault(color)
    }

    @ColorInt
    public fun getBorderColorDefault(context: Context): Int {
        return getStrokeColorDefault(context)
    }

    public fun setBorderColorDark(@ColorInt color: Int) {
        setStrokeColorDark(color)
    }

    @ColorInt
    public fun getBorderColorDark(context: Context): Int {
        return getStrokeColorDark(context)
    }
    // endregion

    // region Text Colors
    public fun setTextColorPrimary(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatTextColorPrimary] = color
    }

    @ColorInt
    public fun getTextColorPrimary(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatTextColorPrimary)
        return if (color == 0) getNeutralColor900(context) else color
    }

    public fun setTextColorSecondary(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatTextColorSecondary] = color
    }

    @ColorInt
    public fun getTextColorSecondary(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatTextColorSecondary)
        return if (color == 0) getNeutralColor600(context) else color
    }

    public fun setTextColorTertiary(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatTextColorTertiary] = color
    }

    @ColorInt
    public fun getTextColorTertiary(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatTextColorTertiary)
        return if (color == 0) getNeutralColor500(context) else color
    }

    public fun setTextColorDisabled(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatTextColorDisabled] = color
    }

    @ColorInt
    public fun getTextColorDisabled(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatTextColorDisabled)
        return if (color == 0) getNeutralColor400(context) else color
    }

    public fun setTextColorWhite(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatTextColorWhite] = color
    }

    @ColorInt
    public fun getTextColorWhite(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatTextColorWhite)
        return if (color == 0) getNeutralColor50(context) else color
    }

    public fun setTextColorHighlight(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatTextColorHighlight] = color
    }

    @ColorInt
    public fun getTextColorHighlight(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatTextColorHighlight)
        return if (color == 0) getPrimaryColor(context) else color
    }
    // endregion

    // region Icon Tint Colors
    public fun setIconTintPrimary(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatIconTintPrimary] = color
    }

    @ColorInt
    public fun getIconTintPrimary(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatIconTintPrimary)
        return if (color == 0) getNeutralColor900(context) else color
    }

    public fun setIconTintSecondary(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatIconTintSecondary] = color
    }

    @ColorInt
    public fun getIconTintSecondary(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatIconTintSecondary)
        return if (color == 0) getNeutralColor500(context) else color
    }

    public fun setIconTintTertiary(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatIconTintTertiary] = color
    }

    @ColorInt
    public fun getIconTintTertiary(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatIconTintTertiary)
        return if (color == 0) getNeutralColor400(context) else color
    }

    public fun setIconTintWhite(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatIconTintWhite] = color
    }

    @ColorInt
    public fun getIconTintWhite(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatIconTintWhite)
        return if (color == 0) getNeutralColor50(context) else color
    }

    public fun setIconTintHighlight(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatIconTintHighlight] = color
    }

    @ColorInt
    public fun getIconTintHighlight(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatIconTintHighlight)
        return if (color == 0) getPrimaryColor(context) else color
    }
    // endregion

    // region Button Colors
    public fun setPrimaryButtonBackgroundColor(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatPrimaryButtonBackgroundColor] = color
    }

    @ColorInt
    public fun getPrimaryButtonBackgroundColor(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatPrimaryButtonBackgroundColor)
        return if (color == 0) getPrimaryColor(context) else color
    }

    public fun setPrimaryButtonIconTint(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatPrimaryButtonIconTint] = color
    }

    @ColorInt
    public fun getPrimaryButtonIconTint(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatPrimaryButtonIconTint)
        return if (color == 0) getColorWhite(context) else color
    }

    public fun setPrimaryButtonTextColor(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatPrimaryButtonTextColor] = color
    }

    @ColorInt
    public fun getPrimaryButtonTextColor(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatPrimaryButtonTextColor)
        return if (color == 0) getColorWhite(context) else color
    }

    public fun setSecondaryButtonBackgroundColor(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatSecondaryButtonBackgroundColor] = color
    }

    @ColorInt
    public fun getSecondaryButtonBackgroundColor(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatSecondaryButtonBackgroundColor)
        return if (color == 0) getNeutralColor900(context) else color
    }

    public fun setSecondaryButtonIconTint(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatSecondaryButtonIconTint] = color
    }

    @ColorInt
    public fun getSecondaryButtonIconTint(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatSecondaryButtonIconTint)
        return if (color == 0) getNeutralColor900(context) else color
    }

    public fun setSecondaryButtonTextColor(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatSecondaryButtonTextColor] = color
    }

    @ColorInt
    public fun getSecondaryButtonTextColor(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatSecondaryButtonTextColor)
        return if (color == 0) getNeutralColor900(context) else color
    }

    // The four accessors below back attributes that already existed in attr_cometchat_theme.xml
    // and themes.xml but had no Kotlin accessor, so they were reachable from XML themes only.
    // Their fallbacks match chatuikit-compose's CometChatColorScheme defaults.

    public fun setLinkButtonColor(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatLinkButtonColor] = color
    }

    @ColorInt
    public fun getLinkButtonColor(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatLinkButtonColor)
        return if (color == 0) getInfoColor(context) else color
    }

    public fun setFabButtonBackgroundColor(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatFabButtonBackgroundColor] = color
    }

    @ColorInt
    public fun getFabButtonBackgroundColor(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatFabButtonBackgroundColor)
        return if (color == 0) getPrimaryColor(context) else color
    }

    public fun setFabButtonIconTint(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatFabButtonIconTint] = color
    }

    @ColorInt
    public fun getFabButtonIconTint(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatFabButtonIconTint)
        return if (color == 0) getColorWhite(context) else color
    }

    public fun setWhiteButtonPressed(@ColorInt color: Int) {
        themeAttributeCache[R.attr.cometchatWhiteButtonPressed] = color
    }

    @ColorInt
    public fun getWhiteButtonPressed(context: Context): Int {
        val color = getColorFromAttr(context, R.attr.cometchatWhiteButtonPressed)
        return if (color == 0) getNeutralColor300(context) else color
    }
    // endregion

    // region Static Colors
    @ColorInt
    public fun getColorWhite(context: Context): Int {
        return context.resources.getColor(R.color.cometchat_color_white, context.theme)
    }

    @ColorInt
    public fun getColorBlack(context: Context): Int {
        return context.resources.getColor(R.color.cometchat_color_black, context.theme)
    }

    @ColorInt
    public fun getColorTransparent(context: Context): Int {
        return context.resources.getColor(R.color.cometchat_color_transparent, context.theme)
    }
    // endregion


    // region Typography
    @StyleRes
    public fun getTextAppearanceTitleRegular(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceTitleRegular)
    }

    @StyleRes
    public fun getTextAppearanceTitleMedium(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceTitleMedium)
    }

    @StyleRes
    public fun getTextAppearanceTitleBold(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceTitleBold)
    }

    @StyleRes
    public fun getTextAppearanceHeading1Regular(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading1Regular)
    }

    @StyleRes
    public fun getTextAppearanceHeading1Medium(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading1Medium)
    }

    @StyleRes
    public fun getTextAppearanceHeading1Bold(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading1Bold)
    }

    @StyleRes
    public fun getTextAppearanceHeading2Regular(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading2Regular)
    }

    @StyleRes
    public fun getTextAppearanceHeading2Medium(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading2Medium)
    }

    @StyleRes
    public fun getTextAppearanceHeading2Bold(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading2Bold)
    }

    @StyleRes
    public fun getTextAppearanceHeading3Regular(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading3Regular)
    }

    @StyleRes
    public fun getTextAppearanceHeading3Medium(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading3Medium)
    }

    @StyleRes
    public fun getTextAppearanceHeading3Bold(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading3Bold)
    }

    @StyleRes
    public fun getTextAppearanceHeading4Regular(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading4Regular)
    }

    @StyleRes
    public fun getTextAppearanceHeading4Medium(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading4Medium)
    }

    @StyleRes
    public fun getTextAppearanceHeading4Bold(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceHeading4Bold)
    }

    @StyleRes
    public fun getTextAppearanceBodyRegular(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceBodyRegular)
    }

    @StyleRes
    public fun getTextAppearanceBodyMedium(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceBodyMedium)
    }

    @StyleRes
    public fun getTextAppearanceBodyBold(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceBodyBold)
    }

    @StyleRes
    public fun getTextAppearanceCaption1Regular(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceCaption1Regular)
    }

    @StyleRes
    public fun getTextAppearanceCaption1Medium(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceCaption1Medium)
    }

    @StyleRes
    public fun getTextAppearanceCaption1Bold(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceCaption1Bold)
    }

    @StyleRes
    public fun getTextAppearanceCaption2Regular(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceCaption2Regular)
    }

    @StyleRes
    public fun getTextAppearanceCaption2Medium(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceCaption2Medium)
    }

    @StyleRes
    public fun getTextAppearanceCaption2Bold(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceCaption2Bold)
    }

    @StyleRes
    public fun getTextAppearanceButtonRegular(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceButtonRegular)
    }

    @StyleRes
    public fun getTextAppearanceButtonMedium(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceButtonMedium)
    }

    @StyleRes
    public fun getTextAppearanceButtonBold(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceButtonBold)
    }

    @StyleRes
    public fun getTextAppearanceLinkRegular(context: Context): Int {
        return getTextAppearanceFromAttr(context, R.attr.cometchatTextAppearanceLinkRegular)
    }
    // endregion

    // region Fonts
    @FontRes
    public fun getFontRegular(context: Context): Int {
        return getFontFromAttr(context, R.attr.cometchatFontRegular)
    }

    @FontRes
    public fun getFontMedium(context: Context): Int {
        return getFontFromAttr(context, R.attr.cometchatFontMedium)
    }

    @FontRes
    public fun getFontBold(context: Context): Int {
        return getFontFromAttr(context, R.attr.cometchatFontBold)
    }
    // endregion

    // region Private Helper Methods
    @ColorInt
    private fun getColorFromAttr(context: Context?, @AttrRes attr: Int): Int {
        if (context == null) return 0
        if (themeAttributeCache.containsKey(attr)) {
            return themeAttributeCache[attr] ?: 0
        }

        return try {
            context.obtainStyledAttributes(intArrayOf(attr)).let { typedArray ->
                // ENG-38656: TypedArray is only AutoCloseable on API 31+; use
                // explicit recycle() so this is safe on minSdk 28 (was NewApi error)
                try { typedArray.getColor(0, 0) } finally { typedArray.recycle() }
            }
        } catch (e: Exception) {
            0
        }
    }

    @ColorInt
    private fun getExtendedPrimaryColor(
        context: Context,
        @AttrRes attr: Int,
        dayPercentage: Double,
        nightPercentage: Double
    ): Int {
        val color = getColorFromAttr(context, attr)
        if (color == 0) {
            val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val blendingColor = if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) Color.WHITE else Color.BLACK
            val percentage = if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) dayPercentage else nightPercentage
            return getBlendedColor(getPrimaryColor(context), blendingColor, percentage)
        }
        return color
    }

    private fun getBlendedColor(baseColor: Int, blendColor: Int, percentage: Double): Int {
        val r = (Color.red(baseColor) * (1 - percentage) + Color.red(blendColor) * percentage).roundToInt()
        val g = (Color.green(baseColor) * (1 - percentage) + Color.green(blendColor) * percentage).roundToInt()
        val b = (Color.blue(baseColor) * (1 - percentage) + Color.blue(blendColor) * percentage).roundToInt()
        return Color.rgb(r, g, b)
    }

    @StyleRes
    private fun getTextAppearanceFromAttr(context: Context?, @AttrRes attr: Int): Int {
        if (context == null) return 0
        return try {
            context.obtainStyledAttributes(intArrayOf(attr)).let { typedArray ->
                // ENG-38656: TypedArray is only AutoCloseable on API 31+; use
                // explicit recycle() so this is safe on minSdk 28 (was NewApi error)
                try { typedArray.getResourceId(0, 0) } finally { typedArray.recycle() }
            }
        } catch (e: Exception) {
            0
        }
    }

    @FontRes
    private fun getFontFromAttr(context: Context?, @AttrRes attr: Int): Int {
        if (context == null) return 0
        return try {
            context.obtainStyledAttributes(intArrayOf(attr)).let { typedArray ->
                // ENG-38656: TypedArray is only AutoCloseable on API 31+; use
                // explicit recycle() so this is safe on minSdk 28 (was NewApi error)
                try { typedArray.getResourceId(0, 0) } finally { typedArray.recycle() }
            }
        } catch (e: Exception) {
            0
        }
    }
    // endregion

    /**
     * Clears the theme attribute cache.
     * Call this when the theme changes to ensure fresh values are loaded.
     */
    public fun clearCache() {
        themeAttributeCache.clear()
    }
}
