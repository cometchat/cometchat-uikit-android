package com.cometchat.uikit.kotlin.presentation.shared.mediaviewer

import android.view.ViewGroup
import android.widget.ImageView

/**
 * Utility object providing factory methods to create [CometChatImagePreview] instances
 * and convenience wrappers for setting listeners.
 */
public object CometChatImagePreviewUtils {

    /**
     * Creates a [CometChatImagePreview] instance attached to the given [imageView] and [container].
     * The default drag-to-dismiss distance is set to 50% of the screen height via
     * [CometChatImagePreview.DEFAULT_DRAG_DISMISS_DISTANCE_IN_VIEW_HEIGHT_RATIO] in the
     * [CometChatImagePreview.onLayoutChange] callback.
     */
    @JvmStatic
    public fun createImagePreview(imageView: ImageView, container: ViewGroup): CometChatImagePreview {
        return CometChatImagePreview.create(imageView, container)
    }

    /**
     * Sets an [OnScaleChangedListener] on the given [cometChatImagePreview].
     */
    @JvmStatic
    public fun setOnScaleChangedListener(
        cometChatImagePreview: CometChatImagePreview,
        onScaleChangedListener: OnScaleChangedListener
    ) {
        cometChatImagePreview.setOnScaleChangedListener(object : CometChatImagePreview.OnScaleChangedListener {
            override fun onScaleChange(scaleFactor: Float, focusX: Float, focusY: Float) {
                onScaleChangedListener.onScaleChange(scaleFactor, focusX, focusY)
            }
        })
    }

    /**
     * Sets an [OnViewTranslateListener] on the given [cometChatImagePreview].
     */
    @JvmStatic
    public fun setOnViewTranslateListener(
        cometChatImagePreview: CometChatImagePreview,
        onViewTranslateListener: OnViewTranslateListener
    ) {
        cometChatImagePreview.setOnViewTranslateListener(object : CometChatImagePreview.OnViewTranslateListener {
            override fun onStart(view: ImageView) {
                onViewTranslateListener.onStart(view)
            }

            override fun onViewTranslate(view: ImageView, amount: Float) {
                onViewTranslateListener.onViewTranslate(view, amount)
            }

            override fun onDismiss(view: ImageView) {
                onViewTranslateListener.onDismiss(view)
            }

            override fun onRestore(view: ImageView) {
                onViewTranslateListener.onRestore(view)
            }
        })
    }

    /**
     * Listener interface for scale change events.
     */
    public interface OnScaleChangedListener {
        public fun onScaleChange(scaleFactor: Float, focusX: Float, focusY: Float)
    }

    /**
     * Listener interface for view translation events during drag-to-dismiss gestures.
     */
    public interface OnViewTranslateListener {
        public fun onStart(view: ImageView)
        public fun onViewTranslate(view: ImageView, amount: Float)
        public fun onRestore(view: ImageView)
        public fun onDismiss(view: ImageView)
    }
}
