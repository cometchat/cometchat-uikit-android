package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cometchat.uikit.compose.R
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.utils.MediaSource

/**
 * The offline media placeholder for the Compose kit's media bubbles (G9). Shown only when
 * [com.cometchat.uikit.core.utils.MediaSourceResolver] returns a placeholder (the offline ON path):
 * the file name and size and "available when online". A tap runs [onClick], which asks the SDK to
 * download; the row re-resolves on the hub's `MediaReady`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun OfflineMediaPlaceholderBox(
    placeholder: MediaSource.Placeholder,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.cometchat_image_placeholder),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = offlineMediaCaption(context, placeholder),
                style = CometChatTheme.typography.caption1Regular,
                color = CometChatTheme.colorScheme.textColorSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

/** `name · size` on the first line, then "available when online". */
internal fun offlineMediaCaption(context: Context, placeholder: MediaSource.Placeholder): String {
    val size = if (placeholder.fileSize > 0) Formatter.formatShortFileSize(context, placeholder.fileSize.toLong()) else null
    val first = listOfNotNull(placeholder.fileName?.takeIf { it.isNotBlank() }, size).joinToString(" · ")
    val online = context.getString(com.cometchat.uikit.core.R.string.cometchat_media_available_when_online)
    return if (first.isEmpty()) online else "$first\n$online"
}
