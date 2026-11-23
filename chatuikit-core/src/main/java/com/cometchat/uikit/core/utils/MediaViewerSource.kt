package com.cometchat.uikit.core.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.cometchat.chat.models.Attachment
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.offline.OfflineGate
import java.io.File

/**
 * What the full-screen image viewer and video player load, and what their Download and Share act
 * on (G9, BEH-MEDIA-18). The source is resolved the same way the bubbles resolve theirs
 * ([MediaSourceResolver]): the on-device copy when there is one, else the URL. Used by both UI
 * kits. Internal to the kit.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object MediaViewerSource {

    private const val FILE_SCHEME = "file://"
    private const val SHARE_DIR = "cometchat_share"
    private const val MEDIA_ROOT = "cometchat/media"

    /**
     * The source a viewer or player loads for [attachment]: the SDK's local copy as a `file://` URI
     * when one exists (in any connection state), otherwise the URL, as today. Offline with no local
     * copy the URL cannot load, so the viewer shows its error state, and Download and Share fail as
     * they do today. Offline on, a `fileUrl` that is not http(s) is never used: message data is not a
     * local file (BEH-MEDIA-21), so it gives `""`, as no source does. Offline off: always the URL.
     */
    @JvmStatic
    public fun of(attachment: Attachment?): String = of(attachment, MediaSourceResolver.resolve(attachment))

    @VisibleForTesting
    internal fun of(attachment: Attachment?, resolved: MediaSource?): String = when (resolved) {
        is MediaSource.Local -> FILE_SCHEME + resolved.path
        null -> attachment?.fileUrl.orEmpty() // offline off: today's URL
        else -> openableUrl(attachment?.fileUrl, offlineOn = true)
    }

    /**
     * The attachment URL a bubble may hand to another app when it has no local copy to open: today's
     * `fileUrl` with offline off; offline on, only an http(s) URL, else `""` (BEH-MEDIA-21).
     */
    @JvmStatic
    public fun openableUrl(attachment: Attachment?): String = openableUrl(attachment?.fileUrl, OfflineGate.isOn())

    @VisibleForTesting
    internal fun openableUrl(url: String?, offlineOn: Boolean): String = when {
        url.isNullOrEmpty() -> ""
        !offlineOn || isWebUrl(url) -> url
        else -> ""
    }

    private fun isWebUrl(url: String): Boolean =
        url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)

    /**
     * The on-device file behind a viewer [source], or `null` when the source is a URL, the file is
     * gone, it is not inside the SDK's media folder, or offline is off. `null` means the caller keeps
     * today's network path. Only a file the SDK saved is ever read (BEH-MEDIA-21): the path's
     * canonical form (`..` and symlinks resolved) must lie under `<noBackupFilesDir>/cometchat/media/`.
     */
    @JvmStatic
    public fun localFile(context: Context, source: String?): File? =
        localFile(source, OfflineGate.isOn(), mediaRoot(context))

    /**
     * As [localFile] with a context, using the application context captured by `CometChatUIKit.init`.
     * With none captured, nothing local is used and the caller keeps today's network path.
     */
    @JvmStatic
    public fun localFile(source: String?): File? =
        localFile(source, OfflineGate.isOn(), CometChatUIKit.getApplicationContext()?.let(::mediaRoot))

    @VisibleForTesting
    internal fun localFile(source: String?, offlineOn: Boolean, mediaRoot: File?): File? {
        if (!offlineOn || source.isNullOrEmpty() || mediaRoot == null) return null
        val path = when {
            source.startsWith(FILE_SCHEME) -> source.removePrefix(FILE_SCHEME)
            source.startsWith("/") -> source
            else -> return null
        }
        if (path.isEmpty()) return null
        return runCatching {
            val root = mediaRoot.canonicalFile.path + File.separator
            File(path).canonicalFile.takeIf { it.path.startsWith(root) && it.isFile }
        }.getOrNull()
    }

    /** The SDK's media folder: every app and user nests below it (SDK `MediaFileStore`, not configurable). */
    private fun mediaRoot(context: Context): File? =
        runCatching { File(context.applicationContext.noBackupFilesDir, MEDIA_ROOT) }.getOrNull()

    /** True when offline is on: the viewer then stops its spinner when a page cannot load. */
    @JvmStatic
    public fun isOfflineOn(): Boolean = OfflineGate.isOn()

    /**
     * Copies [file] into the public Downloads folder as [displayName], reduced to a plain file name
     * ([saveName]): the name comes from message data, so it never places the copy outside Downloads
     * (BEH-MEDIA-21). Reads [file] only; it is never moved, changed or deleted. Blocking: call off
     * the main thread.
     *
     * @return true when the copy was written.
     */
    @JvmStatic
    @WorkerThread
    public fun saveToDownloads(context: Context, file: File, displayName: String, mimeType: String?): Boolean =
        runCatching {
            val name = saveName(displayName, file)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    if (!mimeType.isNullOrEmpty()) put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
                val out = resolver.openOutputStream(uri) ?: return false
                out.use { o -> file.inputStream().use { it.copyTo(o) } }
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                val target = downloadsTarget(dir, name) ?: return false
                file.copyTo(target)
            }
            true
        }.getOrElse {
            CometChatLogger.e(TAG, "Failed to save the local copy to Downloads: ${it.message}")
            false
        }

    /**
     * Copies [file] into the app's cache as [displayName], so the app receiving the share sees the
     * real file name (the stored copy has none). Reads [file] only. Blocking: call off the main
     * thread.
     */
    @JvmStatic
    @WorkerThread
    public fun copyForShare(context: Context, file: File, displayName: String): File {
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        val target = File(dir, File(displayName.ifEmpty { file.name }).name)
        return file.copyTo(target, overwrite = true)
    }

    /**
     * The name a Downloads copy is saved as: [displayName] with any directory part dropped, as
     * [copyForShare] does. Blank, `.` or `..` falls back to the stored [file]'s name.
     */
    @VisibleForTesting
    internal fun saveName(displayName: String, file: File): String =
        File(displayName).name.takeUnless { it.isBlank() || it == "." || it == ".." } ?: file.name

    /**
     * The file a pre-Q save writes in [dir]: a free name built from [name] by [uniqueFile], or
     * `null` when its canonical parent is not [dir], so nothing is ever written outside Downloads.
     */
    @VisibleForTesting
    internal fun downloadsTarget(dir: File, name: String): File? {
        val target = uniqueFile(dir, name)
        return target.takeIf { it.canonicalFile.parentFile == dir.canonicalFile }
    }

    private fun uniqueFile(dir: File, name: String): File {
        var candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        var n = 1
        while (candidate.exists()) candidate = File(dir, "$base-${n++}$ext")
        return candidate
    }

    private const val TAG = "MediaViewerSource"
}
