package com.cometchat.uikit.core.utils

import com.cometchat.chat.core.Call

/**
 * Utility object for tracking active call state in the UIKit.
 * This is used to prevent initiating new calls when one is already in progress.
 * 
 * Note: This mirrors the functionality of CallingExtension in the chatuikit module
 * but is available in chatuikit-core for use by shared ViewModels.
 */
public object CallingState {
    
    @Volatile
    private var activeCall: Call? = null
    
    @Volatile
    private var isActiveMeeting: Boolean = false
    
    /**
     * Gets the currently active call tracked by the UIKit.
     * @return The active Call or null if no call is active
     */
    public fun getActiveCall(): Call? = activeCall
    
    /**
     * Sets the currently active call.
     * @param call The Call to set as active, or null to clear
     */
    public fun setActiveCall(call: Call?) {
        activeCall = call
    }
    
    /**
     * Checks if there is an active meeting in progress.
     * @return true if a meeting is active, false otherwise
     */
    public fun isActiveMeeting(): Boolean = isActiveMeeting
    
    /**
     * Sets the active meeting state.
     * @param isActive true if a meeting is active, false otherwise
     */
    public fun setIsActiveMeeting(isActive: Boolean) {
        isActiveMeeting = isActive
    }
    
    /**
     * The outgoing call still ringing, if any: set by the outgoing screen, cleared once it is
     * answered or over. Logout cancels it, so the callee stops ringing for a user who is gone.
     */
    @Volatile
    internal var ringingOutgoingCall: Call? = null

    /** When each call was placed (elapsed realtime), so the ring window counts from placement. */
    private val placedAt = java.util.concurrent.ConcurrentHashMap<String, Long>()

    internal fun markPlaced(sessionId: String, at: Long = android.os.SystemClock.elapsedRealtime()) {
        placedAt[sessionId] = at
    }

    internal fun placedAt(sessionId: String): Long? = placedAt[sessionId]

    internal fun forgetPlaced(sessionId: String) {
        placedAt.remove(sessionId)
    }

    /**
     * Clears all active call state.
     */
    public fun clear() {
        activeCall = null
        isActiveMeeting = false
        ringingOutgoingCall = null
    }
}
