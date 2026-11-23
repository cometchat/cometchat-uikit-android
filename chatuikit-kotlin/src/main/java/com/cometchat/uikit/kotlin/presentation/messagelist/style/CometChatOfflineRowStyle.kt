package com.cometchat.uikit.kotlin.presentation.messagelist.style

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import com.cometchat.uikit.kotlin.theme.CometChatTheme

/**
 * Style of the caption under an unsent message row when offline messaging is on: "Not sent yet"
 * while it waits, and "Not sent. Tap to retry" once it failed.
 *
 * Set it with `CometChatMessageList.setOfflineRowStyle`. A `0` value falls back to the theme
 * tokens `cometchatOfflineNotSentTextAppearance` and `cometchatOfflineFailedMessageTint`, which
 * default to today's caption text appearance and error colour.
 *
 * @param textAppearance Text appearance of both captions.
 * @param notSentTextColor Colour of "Not sent yet".
 * @param failedTextColor Colour of "Not sent. Tap to retry".
 */
public data class CometChatOfflineRowStyle(
    @StyleRes val textAppearance: Int = 0,
    @ColorInt val notSentTextColor: Int = 0,
    @ColorInt val failedTextColor: Int = 0
) {
    public companion object {
        /** The style resolved from the current theme's offline tokens. */
        @JvmStatic
        public fun default(context: Context): CometChatOfflineRowStyle = CometChatOfflineRowStyle(
            textAppearance = themeResource(context, com.cometchat.uikit.core.R.attr.cometchatOfflineNotSentTextAppearance)
                .takeIf { it != 0 } ?: CometChatTheme.getTextAppearanceCaption1Regular(context),
            notSentTextColor = CometChatTheme.getTextColorSecondary(context),
            failedTextColor = themeColor(context, com.cometchat.uikit.core.R.attr.cometchatOfflineFailedMessageTint)
                .takeIf { it != 0 } ?: CometChatTheme.getErrorColor(context)
        )

        private fun themeResource(context: Context, attr: Int): Int {
            val a = context.obtainStyledAttributes(intArrayOf(attr))
            return try { a.getResourceId(0, 0) } finally { a.recycle() }
        }

        private fun themeColor(context: Context, attr: Int): Int {
            val a = context.obtainStyledAttributes(intArrayOf(attr))
            return try { a.getColor(0, 0) } finally { a.recycle() }
        }
    }
}
