package com.cometchat.uikit.core.utils

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.MediaCachingMode
import com.cometchat.chat.models.MediaCallbackListener
import com.cometchat.uikit.core.offline.OfflineGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Where a media bubble loads an attachment from (G9, BEH-MEDIA-13). Used by the image, video,
 * audio and file bubbles of both UI kits. Internal to the kit.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed class MediaSource {
    /** The bytes are on the device at [path]. */
    public data class Local(public val path: String) : MediaSource()

    /** Load [url] with the kit's normal image/media loader. */
    public data class Remote(public val url: String) : MediaSource()

    /** Never downloaded and not reachable now: show [fileName], [fileSize] and "available when online". */
    public data class Placeholder(public val fileName: String?, public val fileSize: Int) : MediaSource()
}

/**
 * Resolves an attachment's source on the offline (ON) path: the local path, then the URL, then a
 * placeholder. With media caching `OFF` it is the URL only (BEH-MEDIA-03). Offline off returns
 * `null`, and the bubble keeps today's code. The kit owns no media cache and changes no loader
 * configuration; it only picks the source.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object MediaSourceResolver {

    private val _connectionEpoch = MutableStateFlow(0)

    /**
     * Bumped each time a message list sees the connection come back with offline on, so a bubble
     * that showed a placeholder while the socket was down re-resolves (Compose keys on it; the
     * View kit rebinds the media rows).
     */
    @JvmStatic
    public val connectionEpoch: StateFlow<Int> = _connectionEpoch.asStateFlow()

    internal fun onConnected() {
        _connectionEpoch.value = _connectionEpoch.value + 1
    }

    /** @return the source to load, or `null` when offline is off (today's path). */
    @JvmStatic
    public fun resolve(attachment: Attachment?): MediaSource? =
        resolve(
            attachment = attachment,
            offlineOn = OfflineGate.isOn(),
            cachingMode = { runCatching { CometChat.getMediaCachingMode() }.getOrNull() },
            isOnline = { isConnected() },
            fileExists = { path -> runCatching { File(path).exists() }.getOrDefault(false) }
        )

    @VisibleForTesting
    internal fun resolve(
        attachment: Attachment?,
        offlineOn: Boolean,
        cachingMode: () -> MediaCachingMode?,
        isOnline: () -> Boolean,
        fileExists: (String) -> Boolean
    ): MediaSource? {
        if (!offlineOn) return null
        val url = attachment?.fileUrl
        if (cachingMode() == MediaCachingMode.OFF) {
            return if (!url.isNullOrEmpty()) MediaSource.Remote(url) else placeholder(attachment)
        }
        val local = attachment?.localPath
        if (!local.isNullOrEmpty() && fileExists(local)) return MediaSource.Local(local)
        if (!url.isNullOrEmpty() && isOnline()) return MediaSource.Remote(url)
        return placeholder(attachment)
    }

    /** The attachment's key for `MediaReady` matching: its URL without the query (BEH-MEDIA-13). */
    @JvmStatic
    public fun attachmentKey(url: String?): String? = url?.substringBefore('?')

    /**
     * A tap on a placeholder: when online, asks the SDK to download the attachment. The bubble
     * re-resolves on the hub's `MediaReady`; [onResult] gets the local path, or null on failure.
     * Offline it does nothing.
     */
    @JvmStatic
    public fun download(attachment: Attachment, onResult: (String?) -> Unit) {
        if (!isConnected()) return
        runCatching {
            CometChat.downloadMedia(attachment, object : MediaCallbackListener {
                override fun onSuccess(localPath: String?) = onResult(localPath)
                override fun onProgress(progress: Int) = Unit
                override fun onError(e: CometChatException?) = onResult(null)
            })
        }.onFailure { onResult(null) }
    }

    private fun placeholder(attachment: Attachment?): MediaSource.Placeholder =
        MediaSource.Placeholder(attachment?.fileName, attachment?.fileSize ?: 0)

    private fun isConnected(): Boolean =
        runCatching { CometChat.getConnectionStatus() == CometChatConstants.WS_STATE_CONNECTED }.getOrDefault(false)
}
