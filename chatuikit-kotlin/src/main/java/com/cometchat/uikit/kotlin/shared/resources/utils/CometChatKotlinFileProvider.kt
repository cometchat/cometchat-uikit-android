package com.cometchat.uikit.kotlin.shared.resources.utils

import androidx.core.content.FileProvider

/**
 * ENG-38657 (T3): dedicated FileProvider subclass for the View toolkit.
 * The manifest merger keys <provider> entries by class name, so two libraries
 * both registering androidx.core.content.FileProvider collide even with
 * different authorities. A unique subclass per toolkit lets the host app,
 * the compose toolkit, and this toolkit each register their own provider.
 */
internal class CometChatKotlinFileProvider : FileProvider()
