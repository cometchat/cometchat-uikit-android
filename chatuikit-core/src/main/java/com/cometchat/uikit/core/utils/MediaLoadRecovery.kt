package com.cometchat.uikit.core.utils

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.MediaCachingMode

/**
 * What a media bubble or grid tile does when its URL fails to load (G9, BEH-MEDIA-13). The SDK
 * keeps no thumbnail bytes, so a thumbnail is always a URL, and a cached signed link can expire.
 *
 * On the first failure the tile tries the attachment's full file URL, once. On the next failure it
 * asks the SDK to download the attachment, once: the SDK re-signs the link and caches the bytes,
 * and the row re-resolves to the local file on `MediaReady`. After that the tile keeps its error
 * placeholder. Create one per bind, so a tile makes at most one fallback and one download per bind
 * and never loops. With media caching `OFF` the download step is skipped (BEH-MEDIA-03: the tile
 * never loads from a local copy, so a download could not help it). Used by both UI kits.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MediaLoadRecovery @VisibleForTesting internal constructor(
    private val attachment: Attachment,
    private val canDownload: Boolean,
    private val download: (Attachment) -> Unit
) {
    private var fileUrlTried = false
    private var downloadAsked = false

    /**
     * The load of [failedUrl] failed.
     *
     * @return the URL to load next (the full file URL, once), or null to keep the error placeholder.
     */
    public fun onLoadFailed(failedUrl: String?): String? {
        if (!fileUrlTried) {
            fileUrlTried = true
            val fileUrl = attachment.fileUrl
            if (!fileUrl.isNullOrEmpty() && fileUrl != failedUrl) return fileUrl
        }
        onFileUrlFailed()
        return null
    }

    /**
     * The file URL fallback has been tried by the caller and failed too: ask the SDK to download
     * once. For bubbles that already run their own file URL fallback.
     */
    public fun onFileUrlFailed() {
        fileUrlTried = true
        if (downloadAsked) return
        downloadAsked = true
        if (canDownload) download(attachment)
    }

    public companion object {
        /**
         * The recovery for a tile that [MediaSourceResolver] resolved to [source], or null when the
         * tile is not loading from a URL with offline on (offline off, a local file, a placeholder):
         * those keep today's behaviour.
         */
        @JvmStatic
        public fun forSource(source: MediaSource?, attachment: Attachment?): MediaLoadRecovery? =
            forSource(
                source = source,
                attachment = attachment,
                cachingMode = { runCatching { CometChat.getMediaCachingMode() }.getOrNull() },
                download = { MediaSourceResolver.download(it) { } }
            )

        @VisibleForTesting
        internal fun forSource(
            source: MediaSource?,
            attachment: Attachment?,
            cachingMode: () -> MediaCachingMode?,
            download: (Attachment) -> Unit
        ): MediaLoadRecovery? {
            if (source !is MediaSource.Remote || attachment == null) return null
            return MediaLoadRecovery(attachment, cachingMode() != MediaCachingMode.OFF, download)
        }
    }
}
