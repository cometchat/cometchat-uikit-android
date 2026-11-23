package com.cometchat.uikit.compose.presentation.messagelist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cometchat.uikit.compose.presentation.messagelist.style.CometChatOfflineRowStyle
import com.cometchat.uikit.core.offline.OfflineRowState

/**
 * The caption under an own unsent row with offline messaging on (W4, ENG-41089; G3, G4):
 * "Not sent yet" while it waits, "Not sent. Tap to retry" once it failed. The failed caption
 * retries on tap, and screen readers announce the action as "Retry".
 */
@Composable
internal fun OfflineRowCaption(
    caption: OfflineRowState.Caption,
    style: CometChatOfflineRowStyle,
    onRetry: () -> Unit
) {
    if (caption == OfflineRowState.Caption.NONE) return
    val failed = caption == OfflineRowState.Caption.NOT_SENT_TAP_TO_RETRY
    val text = stringResource(
        if (failed) com.cometchat.uikit.core.R.string.cometchat_message_not_sent_tap_to_retry
        else com.cometchat.uikit.core.R.string.cometchat_message_not_sent_yet
    )
    val retryLabel = stringResource(com.cometchat.uikit.core.R.string.cometchat_offline_retry)
    val modifier = if (failed) {
        Modifier
            .clickable(onClickLabel = retryLabel, onClick = onRetry)
            .semantics {
                contentDescription = text
                onClick(label = retryLabel) { onRetry(); true }
            }
    } else {
        Modifier.semantics { contentDescription = text }
    }
    Text(
        text = text,
        style = style.textStyle,
        color = if (failed) style.failedTextColor else style.notSentTextColor,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
