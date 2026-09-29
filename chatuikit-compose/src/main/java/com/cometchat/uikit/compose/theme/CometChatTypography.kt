package com.cometchat.uikit.compose.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

@Immutable
public class CometChatTypography(
    public val titleBold: TextStyle = CometChatTextAppearanceTitleBold,
    public val titleMedium: TextStyle = CometChatTextAppearanceTitleMedium,
    public val titleRegular: TextStyle = CometChatTextAppearanceTitleRegular,
    public val heading1Bold: TextStyle = CometChatTextAppearanceHeading1Bold,
    public val heading1Medium: TextStyle = CometChatTextAppearanceHeading1Medium,
    public val heading1Regular: TextStyle = CometChatTextAppearanceHeading1,
    public val heading2Bold: TextStyle = CometChatTextAppearanceHeading2Bold,
    public val heading2Medium: TextStyle = CometChatTextAppearanceHeading2Medium,
    public val heading2Regular: TextStyle = CometChatTextAppearanceHeading2,
    public val heading3Bold: TextStyle = CometChatTextAppearanceHeading3Bold,
    public val heading3Medium: TextStyle = CometChatTextAppearanceHeading3Medium,
    public val heading3Regular: TextStyle = CometChatTextAppearanceHeading3,
    public val heading4Bold: TextStyle = CometChatTextAppearanceHeading4Bold,
    public val heading4Medium: TextStyle = CometChatTextAppearanceHeading4Medium,
    public val heading4Regular: TextStyle = CometChatTextAppearanceHeading4,
    public val bodyBold: TextStyle = CometChatTextAppearanceBodyBold,
    public val bodyMedium: TextStyle = CometChatTextAppearanceBodyMedium,
    public val bodyRegular: TextStyle = CometChatTextAppearanceBody,
    public val caption1Bold: TextStyle = CometChatTextAppearanceCaption1Bold,
    public val caption1Medium: TextStyle = CometChatTextAppearanceCaption1Medium,
    public val caption1Regular: TextStyle = CometChatTextAppearanceCaption1,
    public val caption2Bold: TextStyle = CometChatTextAppearanceCaption2Bold,
    public val caption2Medium: TextStyle = CometChatTextAppearanceCaption2Medium,
    public val caption2Regular: TextStyle = CometChatTextAppearanceCaption2,
    public val buttonBold: TextStyle = CometChatTextAppearanceButtonBold,
    public val buttonMedium: TextStyle = CometChatTextAppearanceButtonMedium,
    public val buttonRegular: TextStyle = CometChatTextAppearanceButton,
    public val linkRegular: TextStyle = CometChatTextAppearanceLink,
) {

    override fun toString(): String {
        return "CometChatTypography(" +
            "titleBold=$titleBold, " +
            "titleMedium=$titleMedium, " +
            "titleRegular=$titleRegular, " +
            "heading1Bold=$heading1Bold, " +
            "heading1Medium=$heading1Medium, " +
            "heading1Regular=$heading1Regular, " +
            "heading2Bold=$heading2Bold, " +
            "heading2Medium=$heading2Medium, " +
            "heading2Regular=$heading2Regular, " +
            "heading3Bold=$heading3Bold, " +
            "heading3Medium=$heading3Medium, " +
            "heading3Regular=$heading3Regular, " +
            "heading4Bold=$heading4Bold, " +
            "heading4Medium=$heading4Medium, " +
            "heading4Regular=$heading4Regular, " +
            "bodyBold=$bodyBold, " +
            "bodyMedium=$bodyMedium, " +
            "bodyRegular=$bodyRegular, " +
            "caption1Bold=$caption1Bold, " +
            "caption1Medium=$caption1Medium, " +
            "caption1Regular=$caption1Regular, " +
            "caption2Bold=$caption2Bold, " +
            "caption2Medium=$caption2Medium, " +
            "caption2Regular=$caption2Regular, " +
            "buttonBold=$buttonBold, " +
            "buttonMedium=$buttonMedium, " +
            "buttonRegular=$buttonRegular, " +
            "linkRegular=$linkRegular" +
            ")"
    }
}

public val LocalTypography: androidx.compose.runtime.ProvidableCompositionLocal<CometChatTypography> = staticCompositionLocalOf { CometChatTypography() }
