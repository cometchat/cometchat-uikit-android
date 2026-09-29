package com.cometchat.uikit.kotlin.presentation.callbuttons.style

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.theme.CometChatTheme

/**
 * Style configuration for a single call button inside [CometChatCallButtonsStyle].
 *
 * Mirrors chatuikit-compose's `CallButtonStyle` property-for-property, so the voice and video
 * buttons are configured the same way in both kits.
 *
 * A value of `0` (or `null` for [icon]) means "leave the button's current value alone", matching
 * how the existing `TypedArray` loading path treats an absent attribute.
 */
public data class CometChatCallButtonStyle(
    val icon: Drawable? = null,
    @ColorInt val iconTint: Int = 0,
    @Dimension val iconSize: Int = 0,
    @ColorInt val textColor: Int = 0,
    @StyleRes val textAppearance: Int = 0,
    @ColorInt val backgroundColor: Int = 0,
    @Dimension val cornerRadius: Int = 0,
    @Dimension val strokeWidth: Int = 0,
    @ColorInt val strokeColor: Int = 0,
    @Dimension val buttonPadding: Int = 0
)

/**
 * Style configuration for `CometChatCallButtons`.
 *
 * Before this existed the View kit exposed roughly thirty individual setters plus
 * `setStyle(@StyleRes)`, but no constructible style object — the parity gap against
 * chatuikit-compose's `CometChatCallButtonsStyle`.
 *
 * Property names follow the View kit's own attribute names (`textAppearance` rather than Compose's
 * `textStyle`), so they line up with `attr_cometchat_call_buttons.xml`.
 *
 * Note the structural parity with Compose: one [CometChatCallButtonStyle] per button plus the
 * spacing between them, rather than the flat `voiceCallX` / `videoCallX` pairs the individual
 * setters use.
 */
public data class CometChatCallButtonsStyle(
    val voiceCallButtonStyle: CometChatCallButtonStyle = CometChatCallButtonStyle(),
    val videoCallButtonStyle: CometChatCallButtonStyle = CometChatCallButtonStyle(),
    @Dimension val marginBetweenButtons: Int = 0
) {
    public companion object {
        /**
         * The theme-resolved style the buttons apply when nothing is overridden. Mirrors the
         * defaults `CometChatCallButtons.applyStyleAttributes` falls back to.
         *
         * @param context Context used to resolve theme attributes, drawables and dimensions.
         */
        public fun default(context: Context): CometChatCallButtonsStyle {
            val iconTint = CometChatTheme.getIconTintPrimary(context)
            val textColor = CometChatTheme.getTextColorPrimary(context)
            val iconSize = context.resources.getDimensionPixelSize(R.dimen.cometchat_24dp)
            return CometChatCallButtonsStyle(
                voiceCallButtonStyle = CometChatCallButtonStyle(
                    icon = ContextCompat.getDrawable(context, R.drawable.cometchat_ic_call_voice),
                    iconTint = iconTint,
                    iconSize = iconSize,
                    textColor = textColor
                ),
                videoCallButtonStyle = CometChatCallButtonStyle(
                    icon = ContextCompat.getDrawable(context, R.drawable.cometchat_ic_call_video),
                    iconTint = iconTint,
                    iconSize = iconSize,
                    textColor = textColor
                )
            )
        }
    }
}
