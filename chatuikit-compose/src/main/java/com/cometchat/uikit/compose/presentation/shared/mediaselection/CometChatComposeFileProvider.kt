package com.cometchat.uikit.compose.presentation.shared.mediaselection

import androidx.core.content.FileProvider

/**
 * ENG-38657 (T3): dedicated FileProvider subclass for the Compose toolkit.
 * The manifest merger keys <provider> entries by class name, so two libraries
 * both registering androidx.core.content.FileProvider collide even with
 * different authorities. A unique subclass per toolkit lets the host app,
 * the kotlin toolkit, and this toolkit each register their own provider.
 */
internal class CometChatComposeFileProvider : FileProvider()
