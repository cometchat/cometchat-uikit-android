package com.cometchat.uikit.core.offline

import androidx.annotation.VisibleForTesting
import com.cometchat.chat.core.CometChat

/**
 * The single place the UI Kit asks whether offline messaging is on (G1, BEH-AUTH-11).
 *
 * This is the **only** call site of [CometChat.isOfflineEnabled] in the whole kit. Every
 * operation (a list load, a send, a reconnect event, a tap) reads the flag **once** through
 * [isOn] and threads the captured `offlineOn` value down its own path, so a flag flip mid-flight
 * never splits one operation across two modes.
 *
 * The read is defensive: an SDK that predates offline, or one that throws before init, must leave
 * the kit on exactly today's code — so any failure is treated as "offline off".
 */
internal object OfflineGate {

    /**
     * Test seam: when non-null, [isOn] returns this instead of consulting the SDK. Lets unit tests
     * exercise both modes without a live SDK. Always null in production.
     */
    @VisibleForTesting
    @JvmStatic
    var testOverride: Boolean? = null

    /**
     * @return `true` only when the SDK reports offline messaging is enabled. A missing symbol or a
     * thrown exception is treated as off, so the kit falls back to today's behaviour.
     */
    @JvmStatic
    fun isOn(): Boolean {
        testOverride?.let { return it }
        return runCatching { CometChat.isOfflineEnabled() }.getOrDefault(false)
    }
}
