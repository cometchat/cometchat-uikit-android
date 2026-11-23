package com.cometchat.uikit.kotlin.presentation.shared.messagebubble

import android.content.Context
import android.graphics.Color
import android.text.format.Formatter
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.cometchat.uikit.core.utils.MediaSource

/**
 * The offline media placeholder caption for the View kit's media bubbles (G9): the file name and
 * size and "available when online", over a bubble whose media was never downloaded and cannot be
 * reached now. Used only when [com.cometchat.uikit.core.utils.MediaSourceResolver] returns a
 * placeholder, i.e. on the offline (ON) path; offline off never calls this.
 */
internal object OfflineMediaPlaceholder {

    private const val TAG = "cometchat_offline_media_placeholder"

    /** The caption text: `name · size` on the first line, then the "available when online" line. */
    fun caption(context: Context, placeholder: MediaSource.Placeholder): String {
        val size = if (placeholder.fileSize > 0) Formatter.formatShortFileSize(context, placeholder.fileSize.toLong()) else null
        val first = listOfNotNull(placeholder.fileName?.takeIf { it.isNotBlank() }, size).joinToString(" · ")
        val online = context.getString(com.cometchat.uikit.core.R.string.cometchat_media_available_when_online)
        return if (first.isEmpty()) online else "$first\n$online"
    }

    /** Shows the caption over [container], or removes it when [placeholder] is null. */
    fun bind(container: ViewGroup, placeholder: MediaSource.Placeholder?) {
        val existing = container.findViewWithTag<TextView>(TAG)
        if (placeholder == null) {
            existing?.visibility = View.GONE
            return
        }
        val label = existing ?: TextView(container.context).also {
            it.tag = TAG
            it.gravity = Gravity.CENTER
            it.setTextColor(Color.WHITE)
            it.setShadowLayer(4f, 0f, 0f, Color.BLACK)
            val pad = (8 * container.resources.displayMetrics.density).toInt()
            it.setPadding(pad, pad, pad, pad)
            container.addView(
                it,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
        label.text = caption(container.context, placeholder)
        label.visibility = View.VISIBLE
    }
}
