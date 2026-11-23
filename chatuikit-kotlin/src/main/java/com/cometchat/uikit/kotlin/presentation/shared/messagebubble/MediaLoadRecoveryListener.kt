package com.cometchat.uikit.kotlin.presentation.shared.messagebubble

import android.view.View
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.cometchat.uikit.core.utils.MediaLoadRecovery

/**
 * The View kit's Glide hook for [MediaLoadRecovery] (G9, BEH-MEDIA-13): when a media URL fails to
 * load, it asks the recovery for the next URL (the full file URL, once) and loads it with [reload];
 * the recovery itself asks the SDK to download once after that. Added only to a request whose
 * source is a URL with offline on, so offline off never sees it. Never consumes the failure, so
 * the request's own error drawable and listeners run as today.
 */
internal class MediaLoadRecoveryListener<T : Any>(
    private val view: View,
    private val recovery: MediaLoadRecovery,
    private val reload: (String) -> Unit
) : RequestListener<T> {

    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<T>,
        isFirstResource: Boolean
    ): Boolean {
        val next = recovery.onLoadFailed(model as? String) ?: return false
        // Glide does not allow starting a load from inside its own callback. Skip the reload if the
        // view was rebound to another load in the meantime (a recycled row).
        val failed = target.request
        view.post { if (target.request === failed) reload(next) }
        return false
    }

    override fun onResourceReady(
        resource: T,
        model: Any,
        target: Target<T>?,
        dataSource: DataSource,
        isFirstResource: Boolean
    ): Boolean = false
}
