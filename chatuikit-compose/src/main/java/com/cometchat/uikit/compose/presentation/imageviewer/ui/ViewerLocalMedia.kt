package com.cometchat.uikit.compose.presentation.imageviewer.ui

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.FileProvider
import com.cometchat.chat.models.Attachment
import com.cometchat.uikit.compose.R
import com.cometchat.uikit.compose.presentation.shared.mediaselection.cometchatFileProviderAuthority
import com.cometchat.uikit.core.utils.CometChatLogger
import com.cometchat.uikit.core.utils.MediaViewerSource

/**
 * Download and Share of a local copy (offline on, D-324, BEH-MEDIA-18) for the viewers, the file
 * and audio bubbles' Download button and the message menu's Share. Neither makes a network call.
 * Each returns false when there is no local copy, and the caller runs today's path unchanged.
 */
internal object ViewerLocalMedia {

    private const val TAG = "ViewerLocalMedia"

    private val backgroundRunner: (() -> Unit) -> Unit = { work -> Thread(work).start() }

    /**
     * Download button of a file or audio bubble: saves [attachment]'s local copy into Downloads.
     * @return false when there is none (offline off, or no local copy) — today's download runs.
     */
    fun downloadAttachment(
        context: Context,
        attachment: Attachment?,
        fileName: String,
        showToast: Boolean = true,
        runInBackground: (() -> Unit) -> Unit = backgroundRunner
    ): Boolean = attachment != null &&
        download(context, MediaViewerSource.of(attachment), fileName, attachment.fileMimeType, showToast, runInBackground)

    /**
     * Share option of the message long-press menu: shares [attachment]'s local copy.
     * @return false when there is none — today's share runs.
     */
    fun shareAttachment(
        context: Context,
        attachment: Attachment?,
        runInBackground: (() -> Unit) -> Unit = backgroundRunner
    ): Boolean = attachment != null && share(
        context, MediaViewerSource.of(attachment), attachment.fileName.orEmpty(),
        attachment.fileMimeType.orEmpty(), runInBackground
    )

    /**
     * Opens a file bubble's local copy [local] (D-330): copied off the main thread into the kit's
     * cache as [fileName] (the SDK file sits in `noBackupFilesDir`, which no FileProvider path
     * covers) and opened with a content URI and a read grant. As today, nothing opens when no app
     * handles [mimeType]. The SDK file is only read.
     */
    fun openLocalCopy(
        context: Context,
        local: java.io.File,
        fileName: String,
        mimeType: String,
        runInBackground: (() -> Unit) -> Unit = backgroundRunner
    ) {
        runInBackground {
            try {
                val copy = MediaViewerSource.copyForShare(context.applicationContext, local, fileName)
                Handler(Looper.getMainLooper()).post {
                    try {
                        val uri = FileProvider.getUriForFile(context, context.cometchatFileProviderAuthority, copy)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mimeType)
                            flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            addCategory(Intent.CATEGORY_DEFAULT)
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    } catch (e: Exception) {
                        CometChatLogger.e(TAG, "Failed to open the local copy: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                CometChatLogger.e(TAG, "Failed to prepare the local copy to open: ${e.message}")
            }
        }
    }

    /** Saves the local copy behind [source] into Downloads, off the main thread. @return false when there is none. */
    fun download(
        context: Context,
        source: String,
        fileName: String,
        mimeType: String?,
        showToast: Boolean = true,
        runInBackground: (() -> Unit) -> Unit = backgroundRunner
    ): Boolean {
        val local = MediaViewerSource.localFile(context, source) ?: return false
        val app = context.applicationContext
        if (showToast) Toast.makeText(context, R.string.cometchat_downloading, Toast.LENGTH_SHORT).show()
        runInBackground {
            if (!MediaViewerSource.saveToDownloads(app, local, fileName, mimeType)) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(app, R.string.cometchat_file_download_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    /**
     * Shares the local copy behind [source]: copied off the main thread into the cache (the SDK
     * keeps media in `noBackupFilesDir`, which no FileProvider path exposes) and shared as a
     * FileProvider `content://` URI, never a `file://` URI. @return false when there is none.
     */
    fun share(
        context: Context,
        source: String,
        fileName: String,
        mimeType: String,
        runInBackground: (() -> Unit) -> Unit = backgroundRunner
    ): Boolean {
        val local = MediaViewerSource.localFile(context, source) ?: return false
        runInBackground {
            try {
                val copy = MediaViewerSource.copyForShare(context.applicationContext, local, fileName)
                Handler(Looper.getMainLooper()).post {
                    try {
                        val uri = FileProvider.getUriForFile(context, context.cometchatFileProviderAuthority, copy)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_STREAM, uri)
                            type = mimeType.ifEmpty { "*/*" }
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, null))
                    } catch (e: Exception) {
                        CometChatLogger.e(TAG, "Failed to share the local copy: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                CometChatLogger.e(TAG, "Failed to prepare the local copy to share: ${e.message}")
            }
        }
        return true
    }
}
