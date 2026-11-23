package com.cometchat.uikit.compose.presentation.messagelist.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.offline.OfflineUi

/**
 * `LazyColumn` key for a message row (ENG-41087). With [offlineKeyMode] off this is today's key
 * exactly (`id`, else `muid` or `hashCode()`); on, the offline render key, so an own message keeps
 * its slot across its ack instead of remounting.
 */
internal fun messageListItemKey(message: BaseMessage, offlineKeyMode: Boolean): Any =
    OfflineUi.renderKey(message, offlineKeyMode)

/** "Connect to load more" edge shown when an offline page could not load; tap retries. */
@Composable
internal fun OfflineLoadMoreEdge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OfflinePill(
        text = stringResource(com.cometchat.uikit.core.R.string.cometchat_connect_to_load_more),
        onClick = onClick,
        modifier = modifier
    )
}

/** "Jump to latest" pill shown after the newest window was refreshed while scrolled up. */
@Composable
internal fun OfflineJumpToLatestPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OfflinePill(
        text = stringResource(com.cometchat.uikit.core.R.string.cometchat_jump_to_latest),
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun OfflinePill(text: String, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = shape,
        color = CometChatTheme.colorScheme.backgroundColor1,
        shadowElevation = 4.dp,
        modifier = modifier.border(1.dp, CometChatTheme.colorScheme.strokeColorLight, shape)
    ) {
        Text(
            text = text,
            style = CometChatTheme.typography.caption1Medium,
            color = CometChatTheme.colorScheme.textColorHighlight,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
