package com.cometchat.uikit.core.offline

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException

/**
 * One read when the connection returns, for a list screen that was opened offline with nothing
 * cached and so shows its empty state (pinned, saved, notification feed; like D58 for the message
 * list).
 *
 * The screen [arm]s it when a load ends empty because it was not connected, and [disarm]s it when
 * rows are shown. On the next `onConnected`, an armed refill disarms itself and, if offline is
 * still on, runs [refill] once; [refill] reads again only if the screen is still empty. The
 * connection listener is registered on the first [arm] only, so offline off never registers it.
 * A refill that again ends not-connected re-arms for the next connection.
 */
internal class OfflineReconnectRefill(
    private val tagPrefix: String,
    private val refill: () -> Unit
) {
    @Volatile
    private var armed = false
    private var tag: String? = null

    val isArmed: Boolean get() = armed

    /** The load ended empty because it was not connected: read once when the connection returns. */
    fun arm() {
        armed = true
        if (tag != null) return
        val newTag = "${tagPrefix}_offline_refill_${System.identityHashCode(this)}"
        tag = newTag
        CometChat.addConnectionListener(newTag, object : CometChat.ConnectionListener {
            override fun onConnected() = this@OfflineReconnectRefill.onConnected()
            override fun onConnecting() = Unit
            override fun onDisconnected() = Unit
            override fun onFeatureThrottled() = Unit
            override fun onConnectionError(error: CometChatException?) = Unit
        })
    }

    /** Rows are shown (or the screen was reset): no read on reconnect. */
    fun disarm() {
        armed = false
    }

    /** The connection is back: an armed refill runs once, while offline is still on. */
    fun onConnected() {
        if (!armed) return
        armed = false
        if (OfflineGate.isOn()) refill()
    }

    /** Removes the connection listener; call from the screen's `onCleared`. */
    fun release() {
        armed = false
        tag?.let { runCatching { CometChat.removeConnectionListener(it) } }
        tag = null
    }
}
