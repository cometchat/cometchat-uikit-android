package com.cometchat.uikit.core.domain.model

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes

/**
 * Kotlin version of CometChatMessageComposerAction matching the Java class structure.
 * Represents an action item in the message composer attachment options.
 *
 * This class is used to define custom actions that can be displayed in the
 * message composer UI, such as attachment options (Camera, Image, Video, etc.).
 *
 * Example usage:
 * ```kotlin
 * val action = CometChatMessageComposerAction(
 *     id = CometChatMessageComposerAction.ID_CAMERA,
 *     title = context.getString(R.string.cometchat_camera),
 *     icon = R.drawable.cometchat_ic_camera,
 *     onClick = { /* handle click */ }
 * )
 * ```
 */
public data class CometChatMessageComposerAction(
    val id: String = "",
    val title: String = "",
    val titleFont: String? = null,
    @ColorInt val titleColor: Int = 0,
    @StyleRes val titleAppearance: Int = 0,
    @DrawableRes val icon: Int = 0,
    @ColorInt val iconTintColor: Int = 0,
    @ColorInt val iconBackground: Int = 0,
    @ColorInt val background: Int = 0,
    val cornerRadius: Int = -1,
    val onClick: OnClick? = null
) {
    public companion object {
        /**
         * Predefined IDs for built-in attachment options.
         * These match the IDs used in UIKitConstants.ComposerAction.
         */
        public const val ID_CAMERA: String = "camera"
        public const val ID_IMAGE: String = "image"
        public const val ID_VIDEO: String = "video"
        public const val ID_AUDIO: String = "audio"
        public const val ID_DOCUMENT: String = "document"
        public const val ID_POLL: String = "extension_poll"
        public const val ID_COLLABORATIVE_DOCUMENT: String = "extension_document"
        public const val ID_COLLABORATIVE_WHITEBOARD: String = "extension_whiteboard"
    }
}

/**
 * Callback interface for attachment option clicks.
 * This is a functional interface that can be implemented using a lambda.
 */
public fun interface OnClick {
    public fun onClick()
}
