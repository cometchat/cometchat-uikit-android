package com.cometchat.uikit.kotlin.presentation.report

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.StyleRes
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.shared.resources.utils.Utils
import com.cometchat.uikit.kotlin.theme.CometChatTheme

/**
 * Style configuration for [CometChatFlagMessageDialog].
 *
 * Before this existed the dialog was styleable only through an XML style resource passed to
 * `setFlagMessageStyle(@StyleRes)`, with no constructible style object — the parity gap against
 * chatuikit-compose's `CometChatFlagMessageDialogStyle`.
 *
 * Property names follow the View kit's own attribute names (`...TextAppearance` rather than
 * Compose's `...TextStyle`), so they line up with `attr_cometchat_flag_message.xml`.
 *
 * A value of `0` means "leave the dialog's current value alone", matching how the existing
 * `TypedArray` loading path treats an absent attribute. Use [default] to obtain a fully
 * theme-resolved style.
 */
public data class CometChatFlagMessageDialogStyle(
    // Dialog
    @ColorInt val backgroundColor: Int = 0,
    @Dimension val borderRadius: Int = 0,
    @ColorInt val strokeColor: Int = 0,
    @Dimension val strokeWidth: Int = 0,
    @ColorInt val titleColor: Int = 0,
    @StyleRes val titleTextAppearance: Int = 0,
    @ColorInt val subtitleTextColor: Int = 0,
    @StyleRes val subtitleTextAppearance: Int = 0,
    @ColorInt val closeIconColor: Int = 0,

    // Reason chips
    @Dimension val chipCornerRadius: Int = 0,
    @Dimension val chipStrokeWidth: Int = 0,
    @StyleRes val chipTextAppearance: Int = 0,
    @ColorInt val chipActiveBackgroundColor: Int = 0,
    @ColorInt val chipInactiveBackgroundColor: Int = 0,
    @ColorInt val chipActiveTextColor: Int = 0,
    @ColorInt val chipInactiveTextColor: Int = 0,
    @ColorInt val chipActiveBorderColor: Int = 0,
    @ColorInt val chipInactiveBorderColor: Int = 0,

    // Remark field
    @ColorInt val remarkFieldTitleTextColor: Int = 0,
    @StyleRes val remarkFieldTitleTextAppearance: Int = 0,
    @ColorInt val remarkFieldHintTextColor: Int = 0,
    @ColorInt val remarkFieldTextColor: Int = 0,
    @StyleRes val remarkFieldTextAppearance: Int = 0,
    @ColorInt val remarkFieldBackgroundColor: Int = 0,

    // Buttons
    @Dimension val buttonCornerRadius: Int = 0,
    @ColorInt val buttonStrokeColor: Int = 0,
    @Dimension val buttonStrokeWidth: Int = 0,
    @ColorInt val reportButtonEnabledBackgroundColor: Int = 0,
    @ColorInt val reportButtonDisabledBackgroundColor: Int = 0,
    @ColorInt val reportButtonEnabledTextColor: Int = 0,
    @ColorInt val reportButtonDisabledTextColor: Int = 0,
    @ColorInt val cancelButtonEnabledBackgroundColor: Int = 0,
    @ColorInt val cancelButtonDisabledBackgroundColor: Int = 0,
    @ColorInt val cancelButtonEnabledTextColor: Int = 0,
    @ColorInt val cancelButtonDisabledTextColor: Int = 0,

    // Error and progress
    @ColorInt val errorTextColor: Int = 0,
    @ColorInt val progressIndicatorColor: Int = 0
) {
    public companion object {
        /**
         * The theme-resolved style the dialog applies when nothing is overridden. Mirrors
         * `CometChatFlagMessageDialog.applyDefaultValues`.
         *
         * @param context Context used to resolve theme attributes and dimensions.
         */
        public fun default(context: Context): CometChatFlagMessageDialogStyle =
            CometChatFlagMessageDialogStyle(
                backgroundColor = CometChatTheme.getBackgroundColor1(context),
                borderRadius = context.resources.getDimensionPixelSize(R.dimen.cometchat_radius_4),
                strokeColor = CometChatTheme.getStrokeColorDefault(context),
                strokeWidth = Utils.convertDpToPx(context, 1),
                titleColor = CometChatTheme.getTextColorPrimary(context),
                subtitleTextColor = CometChatTheme.getTextColorSecondary(context),
                closeIconColor = CometChatTheme.getIconTintPrimary(context),

                chipCornerRadius = context.resources.getDimensionPixelSize(R.dimen.cometchat_radius_max),
                chipStrokeWidth = Utils.convertDpToPx(context, 1),
                chipActiveBackgroundColor = CometChatTheme.getExtendedPrimaryColor100(context),
                chipInactiveBackgroundColor = CometChatTheme.getBackgroundColor1(context),
                chipActiveTextColor = CometChatTheme.getTextColorHighlight(context),
                chipInactiveTextColor = CometChatTheme.getTextColorPrimary(context),
                chipActiveBorderColor = CometChatTheme.getExtendedPrimaryColor200(context),
                chipInactiveBorderColor = CometChatTheme.getStrokeColorDefault(context),

                remarkFieldTitleTextColor = CometChatTheme.getTextColorPrimary(context),
                remarkFieldHintTextColor = CometChatTheme.getTextColorTertiary(context),
                remarkFieldTextColor = CometChatTheme.getTextColorPrimary(context),
                remarkFieldBackgroundColor = CometChatTheme.getBackgroundColor2(context),

                buttonCornerRadius = context.resources.getDimensionPixelSize(R.dimen.cometchat_radius_2),
                buttonStrokeColor = CometChatTheme.getStrokeColorDark(context),
                buttonStrokeWidth = Utils.convertDpToPx(context, 1),
                reportButtonEnabledBackgroundColor = CometChatTheme.getPrimaryButtonBackgroundColor(context),
                reportButtonDisabledBackgroundColor = CometChatTheme.getBackgroundColor4(context),
                reportButtonEnabledTextColor = CometChatTheme.getColorWhite(context),
                reportButtonDisabledTextColor = CometChatTheme.getColorWhite(context),
                cancelButtonEnabledBackgroundColor = CometChatTheme.getBackgroundColor1(context),
                cancelButtonDisabledBackgroundColor = CometChatTheme.getBackgroundColor1(context),
                cancelButtonEnabledTextColor = CometChatTheme.getTextColorPrimary(context),
                cancelButtonDisabledTextColor = CometChatTheme.getTextColorPrimary(context),

                errorTextColor = CometChatTheme.getErrorColor(context),
                progressIndicatorColor = Color.BLUE
            )

        /**
         * Reads a style out of an XML style resource, so the object form and the
         * `setFlagMessageStyle(@StyleRes)` form stay in step.
         *
         * @param context Context used to obtain the styled attributes.
         * @param styleResId The style resource to read.
         * @param fallback Style supplying any attribute the resource does not set.
         */
        public fun fromStyleResource(
            context: Context,
            @StyleRes styleResId: Int,
            fallback: CometChatFlagMessageDialogStyle = CometChatFlagMessageDialogStyle()
        ): CometChatFlagMessageDialogStyle {
            if (styleResId == 0 || styleResId == -1) return fallback
            val typedArray = context.obtainStyledAttributes(
                styleResId,
                R.styleable.CometChatFlagMessage
            )
            return try {
                fallback.mergeFrom(typedArray)
            } finally {
                typedArray.recycle()
            }
        }
    }

    /** Overlays every attribute present in [typedArray] onto this style. */
    private fun mergeFrom(typedArray: TypedArray): CometChatFlagMessageDialogStyle = copy(
        backgroundColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageBackgroundColor, backgroundColor
        ),
        borderRadius = typedArray.getDimensionPixelSize(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageBorderRadius, borderRadius
        ),
        strokeColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageStrokeColor, strokeColor
        ),
        strokeWidth = typedArray.getDimensionPixelSize(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageStrokeWidth, strokeWidth
        ),
        titleColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageTitleColor, titleColor
        ),
        titleTextAppearance = typedArray.getResourceId(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageTitleAppearance, titleTextAppearance
        ),
        subtitleTextColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageSubtitleColor, subtitleTextColor
        ),
        subtitleTextAppearance = typedArray.getResourceId(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageSubtitleTextAppearance,
            subtitleTextAppearance
        ),
        closeIconColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageCloseIconColor, closeIconColor
        ),
        chipCornerRadius = typedArray.getDimensionPixelSize(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageChipCornerRadius, chipCornerRadius
        ),
        chipStrokeWidth = typedArray.getDimensionPixelSize(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageChipStrokeWidth, chipStrokeWidth
        ),
        chipTextAppearance = typedArray.getResourceId(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageChipTextAppearance, chipTextAppearance
        ),
        chipActiveBackgroundColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageChipActiveBackgroundColor,
            chipActiveBackgroundColor
        ),
        chipInactiveBackgroundColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageChipInactiveBackgroundColor,
            chipInactiveBackgroundColor
        ),
        chipActiveTextColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageChipActiveTextColor,
            chipActiveTextColor
        ),
        chipInactiveTextColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageChipInactiveTextColor,
            chipInactiveTextColor
        ),
        chipActiveBorderColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageChipActiveBorderColor,
            chipActiveBorderColor
        ),
        chipInactiveBorderColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageChipInactiveBorderColor,
            chipInactiveBorderColor
        ),
        remarkFieldTitleTextColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageRemarkFieldTitleTextColor,
            remarkFieldTitleTextColor
        ),
        remarkFieldTitleTextAppearance = typedArray.getResourceId(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageRemarkFieldTitleTextAppearance,
            remarkFieldTitleTextAppearance
        ),
        remarkFieldHintTextColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageRemarkFieldHintTextColor,
            remarkFieldHintTextColor
        ),
        remarkFieldTextColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageRemarkFieldTextColor,
            remarkFieldTextColor
        ),
        remarkFieldTextAppearance = typedArray.getResourceId(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageRemarkFieldTextAppearance,
            remarkFieldTextAppearance
        ),
        remarkFieldBackgroundColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageRemarkFieldBackgroundColor,
            remarkFieldBackgroundColor
        ),
        buttonCornerRadius = typedArray.getDimensionPixelSize(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageButtonCornerRadius,
            buttonCornerRadius
        ),
        buttonStrokeColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageButtonStrokeColor, buttonStrokeColor
        ),
        buttonStrokeWidth = typedArray.getDimensionPixelSize(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageButtonStrokeWidth, buttonStrokeWidth
        ),
        reportButtonEnabledBackgroundColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageReportButtonEnabledBackgroundColor,
            reportButtonEnabledBackgroundColor
        ),
        reportButtonDisabledBackgroundColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageReportButtonDisabledBackgroundColor,
            reportButtonDisabledBackgroundColor
        ),
        reportButtonEnabledTextColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageReportButtonEnabledTextColor,
            reportButtonEnabledTextColor
        ),
        reportButtonDisabledTextColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageReportButtonDisabledTextColor,
            reportButtonDisabledTextColor
        ),
        cancelButtonEnabledBackgroundColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageCancelButtonEnabledBackgroundColor,
            cancelButtonEnabledBackgroundColor
        ),
        cancelButtonDisabledBackgroundColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageCancelButtonDisabledBackgroundColor,
            cancelButtonDisabledBackgroundColor
        ),
        cancelButtonEnabledTextColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageCancelButtonEnabledTextColor,
            cancelButtonEnabledTextColor
        ),
        cancelButtonDisabledTextColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageCancelButtonDisabledTextColor,
            cancelButtonDisabledTextColor
        ),
        errorTextColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageErrorTextColor, errorTextColor
        ),
        progressIndicatorColor = typedArray.getColor(
            R.styleable.CometChatFlagMessage_cometchatFlagMessageProgressIndicatorColor,
            progressIndicatorColor
        )
    )
}
