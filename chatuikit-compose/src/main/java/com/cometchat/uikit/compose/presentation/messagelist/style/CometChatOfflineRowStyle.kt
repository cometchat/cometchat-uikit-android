package com.cometchat.uikit.compose.presentation.messagelist.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.cometchat.uikit.compose.theme.CometChatTheme

/**
 * Style of the caption under an unsent message row when offline messaging is on: "Not sent yet"
 * while it waits, and "Not sent. Tap to retry" once it failed.
 *
 * Pass it to `CometChatMessageList(offlineRowStyle = …)`. [default] reads the theme tokens that
 * match the View stack's `cometchatOfflineNotSentTextAppearance` (caption text) and
 * `cometchatOfflineFailedMessageTint` (error colour).
 *
 * @param textStyle Text style of both captions.
 * @param notSentTextColor Colour of "Not sent yet".
 * @param failedTextColor Colour of "Not sent. Tap to retry".
 */
@Immutable
public data class CometChatOfflineRowStyle(
    val textStyle: TextStyle,
    val notSentTextColor: Color,
    val failedTextColor: Color
) {
    public companion object {
        /** The style resolved from the current [CometChatTheme]. */
        @Composable
        public fun default(
            textStyle: TextStyle = CometChatTheme.typography.caption1Regular,
            notSentTextColor: Color = CometChatTheme.colorScheme.textColorSecondary,
            failedTextColor: Color = CometChatTheme.colorScheme.errorColor
        ): CometChatOfflineRowStyle = CometChatOfflineRowStyle(
            textStyle = textStyle,
            notSentTextColor = notSentTextColor,
            failedTextColor = failedTextColor
        )
    }
}
