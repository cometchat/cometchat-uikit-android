package com.cometchat.uikit.core.offline

import androidx.annotation.RestrictTo
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Decides whether a presence dot may show "online" (ENG-41465).
 *
 * With offline on, lists paint users from the cache, and a cached `User.status` is whatever it was
 * when the user was last seen. While the device is not connected nobody's presence is known, so no
 * row may show "online": each status indicator renders as it does for an offline user.
 *
 * Offline off, or connected: [hideOnline] is `false` and every caller renders exactly as today.
 *
 * A row that is already on screen must re-read [hideOnline] when the connection flips (ENG-41551
 * A): [presenceChanges] ticks each time the answer of [hideOnline] changes, so a list re-binds
 * (Views) or recomposes (Compose) its rows. Reconnect then restores presence through the screen's
 * existing re-fetch and live presence events.
 *
 * Like [OfflineUi], this type is `public` only so the UI modules can reach it; [RestrictTo] keeps it
 * out of the customer API. Read [hideOnline] **once per bind** and apply that value to the row.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object OfflinePresence {

    /**
     * @return `true` when offline messaging is on and the SDK is not connected: the row being
     * bound must not show "online". `false` otherwise (today's presence). Offline off never asks
     * the SDK for the connection state.
     */
    @JvmStatic
    public fun hideOnline(): Boolean = OfflineGate.isOn() && !isConnected()

    /**
     * The pure rule behind [hideOnline]: presence is hidden only when offline is on and the
     * device is not connected.
     */
    @JvmStatic
    internal fun hideOnline(offlineOn: Boolean, connected: Boolean): Boolean = offlineOn && !connected

    /**
     * The status a row should carry. With [hideOnline] `true`, an "online" status becomes
     * "offline"; every other value, and every value when [hideOnline] is `false`, is returned as is.
     */
    @JvmStatic
    public fun shownStatus(status: String?, hideOnline: Boolean): String? =
        if (hideOnline && status == CometChatConstants.USER_STATUS_ONLINE) {
            CometChatConstants.USER_STATUS_OFFLINE
        } else {
            status
        }

    private val epoch = MutableStateFlow(0)

    @Volatile
    private var lastHidden = false

    @Volatile
    private var listening = false

    private const val LISTENER_TAG = "uikit_offline_presence"

    /**
     * Ticks each time [hideOnline] changes its answer because the connection flipped. Observers
     * re-read [hideOnline] on each tick; the value itself carries no meaning.
     *
     * The first call with offline on registers one process-wide connection listener. With offline
     * off nothing is registered and the flow never ticks, so offline-off rows are never re-bound.
     */
    @JvmStatic
    public fun presenceChanges(): StateFlow<Int> {
        if (!listening && OfflineGate.isOn()) startListening()
        return epoch.asStateFlow()
    }

    @Synchronized
    private fun startListening() {
        if (listening) return
        listening = true
        lastHidden = hideOnline()
        runCatching {
            CometChat.addConnectionListener(LISTENER_TAG, object : CometChat.ConnectionListener {
                override fun onConnected() = onConnectionChanged(hideOnline(OfflineGate.isOn(), connected = true))
                override fun onConnecting() = onConnectionChanged(hideOnline())
                override fun onDisconnected() = onConnectionChanged(hideOnline(OfflineGate.isOn(), connected = false))
                override fun onFeatureThrottled() = Unit
                override fun onConnectionError(error: CometChatException?) = onConnectionChanged(hideOnline())
            })
        }.onFailure { listening = false }
    }

    /** One connection callback: tick [presenceChanges] only when the [hideOnline] answer changed. */
    @Synchronized
    internal fun onConnectionChanged(hidden: Boolean) {
        if (hidden == lastHidden) return
        lastHidden = hidden
        epoch.value = epoch.value + 1
    }

    /** Test seam: forget the listener and the last answer. */
    @Synchronized
    internal fun resetForTest() {
        listening = false
        lastHidden = false
        epoch.value = 0
    }

    /** A failure to read the connection counts as not connected. */
    private fun isConnected(): Boolean =
        runCatching { CometChat.getConnectionStatus() == CometChatConstants.WS_STATE_CONNECTED }
            .getOrDefault(false)
}
