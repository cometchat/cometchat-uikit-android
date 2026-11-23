package com.cometchat.uikit.kotlin.presentation.messagecomposer.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.uikit.core.models.AttachmentUploadStatus
import com.cometchat.uikit.core.models.AttachmentUploadTile
import com.cometchat.uikit.core.models.ERROR_ATTACHMENT_COPY_MISSING
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * ENG-41136 / D-306 (BEH-UPLOAD-14): a restored tile whose local copy is missing is remove-only in
 * the Views tray: it says the file was not saved, shows the error glyph (never the retry glyph),
 * a tap on the card never retries, and the corner ✕ removes it. A plain FAILED tile still retries.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAttachmentTileCopyMissingTest {

    private val context: Context
        get() = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.CometChatTheme_DayNight)

    private class Sink {
        val fired = mutableListOf<String>()
    }

    private fun tile(category: String, copyMissing: Boolean) = AttachmentUploadTile(
        fileId = "b1_0",
        name = "report.pdf",
        size = 1_000L,
        mimeType = "application/pdf",
        category = category,
        status = AttachmentUploadStatus.FAILED,
        localUri = if (copyMissing) null else "/copies/b1_0",
        error = if (copyMissing) CometChatException(ERROR_ATTACHMENT_COPY_MISSING, "missing") else null
    )

    private fun bind(tile: AttachmentUploadTile, sink: Sink): View {
        val adapter = CometChatAttachmentTileAdapter(
            onCancel = { sink.fired += "cancel" },
            onRemove = { sink.fired += "remove" },
            onRetry = { sink.fired += "retry" },
            onClick = { sink.fired += "click" },
            onRejected = { sink.fired += "rejected" }
        )
        adapter.submitList(listOf(tile))
        val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
        adapter.onBindViewHolder(holder, 0)
        return holder.itemView
    }

    @Test
    fun aCopyMissingFileTileSaysRemoveAndReAddAndNeverRetries() {
        val sink = Sink()
        val view = bind(tile(CometChatConstants.MESSAGE_TYPE_FILE, copyMissing = true), sink)
        assertEquals(
            context.getString(R.string.cometchat_attachment_copy_missing),
            view.findViewById<TextView>(R.id.tvFileMeta).text.toString()
        )
        val card = view.findViewById<View>(R.id.fileCard)
        card.performClick()
        assertFalse("the card must not be a tap target", card.isClickable)
        assertTrue("a copy-missing tile never retries", sink.fired.isEmpty())
    }

    @Test
    fun aCopyMissingAudioTileNeverRetries() {
        val sink = Sink()
        val view = bind(tile(CometChatConstants.MESSAGE_TYPE_AUDIO, copyMissing = true), sink)
        assertEquals(
            context.getString(R.string.cometchat_attachment_copy_missing),
            view.findViewById<TextView>(R.id.tvAudioTime).text.toString()
        )
        view.findViewById<View>(R.id.audioCard).performClick()
        assertTrue(sink.fired.isEmpty())
    }

    @Test
    fun aCopyMissingMediaTileNeverRetriesAndDescribesWhy() {
        val sink = Sink()
        val view = bind(tile(CometChatConstants.MESSAGE_TYPE_IMAGE, copyMissing = true), sink)
        val card = view.findViewById<View>(R.id.mediaCard)
        assertEquals(context.getString(R.string.cometchat_attachment_copy_missing), card.contentDescription)
        card.performClick()
        assertTrue(sink.fired.isEmpty())
    }

    @Test
    fun theCornerBadgeRemovesACopyMissingTile() {
        val sink = Sink()
        val view = bind(tile(CometChatConstants.MESSAGE_TYPE_FILE, copyMissing = true), sink)
        view.findViewById<View>(R.id.cornerBadge).performClick()
        assertEquals(listOf("remove"), sink.fired)
    }

    @Test
    fun aPlainFailedTileStillRetries() {
        val sink = Sink()
        val view = bind(tile(CometChatConstants.MESSAGE_TYPE_FILE, copyMissing = false), sink)
        assertEquals(
            context.getString(R.string.cometchat_attachment_tap_to_retry),
            view.findViewById<TextView>(R.id.tvFileMeta).text.toString()
        )
        view.findViewById<View>(R.id.fileCard).performClick()
        assertEquals(listOf("retry"), sink.fired)
    }
}
