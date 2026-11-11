package com.cometchat.uikit.core.utils

/**
 * Utility object for checking CometChat Calls SDK availability at runtime.
 * 
 * This utility provides methods to determine if the Calls SDK is available
 * and if calling features are enabled in the UIKit configuration.
 * 
 * The SDK availability check uses reflection to avoid compile-time dependencies
 * on the Calls SDK, allowing the UIKit to gracefully handle scenarios where
 * calling functionality is not available.
 * 
 * @see CallingState for active call state management
 */
public object CallsUtils {
    
    private const val TAG = "CallsUtils"
    
    /**
     * The fully qualified class name of the CometChatCalls SDK entry point.
     */
    private const val COMETCHAT_CALLS_CLASS = "com.cometchat.calls.core.CometChatCalls"
    
    /**
     * The fully qualified class name of the Push SDK's calls entry point (ringing v2).
     */
    private const val COMETCHAT_CALL_NOTIFICATIONS_CLASS =
        "com.cometchat.pushnotification.calls.CometChatCallNotifications"

    /**
     * Cached result of SDK availability check to avoid repeated reflection calls.
     */
    @Volatile
    private var cachedSdkAvailable: Boolean? = null

    @Volatile
    private var cachedPushCallsSdkAvailable: Boolean? = null

    /**
     * Checks if the CometChat Push SDK calls module (push-notifications-calls-android 2.0+)
     * is on the app's classpath. The kit does not depend on it; this is a reflection check
     * only, used so the deprecated [com.cometchat.uikit.core.viewmodel.CometChatIncomingCallViewModel]
     * screen does not play a second ringtone over the Push SDK's.
     *
     * @return true if CometChatCallNotifications is on the classpath, false otherwise
     */
    public fun isPushCallsSDKAvailable(): Boolean {
        cachedPushCallsSdkAvailable?.let { return it }
        val isAvailable = try {
            Class.forName(COMETCHAT_CALL_NOTIFICATIONS_CLASS)
            true
        } catch (e: ClassNotFoundException) {
            false
        } catch (e: LinkageError) {
            false
        }
        cachedPushCallsSdkAvailable = isAvailable
        return isAvailable
    }

    /**
     * Checks if the CometChat Calls SDK is available at runtime.
     * 
     * This method uses reflection to check if the CometChatCalls class exists
     * in the classpath. The result is cached after the first check to improve
     * performance on subsequent calls.
     * 
     * @return true if the CometChatCalls SDK is available, false otherwise
     */
    public fun isCallsSDKAvailable(): Boolean {
        // Return cached result if available
        cachedSdkAvailable?.let { return it }
        
        val isAvailable = try {
            Class.forName(COMETCHAT_CALLS_CLASS)
            true
        } catch (e: ClassNotFoundException) {
            false
        } catch (e: LinkageError) {
            // Handle cases where the class exists but has linking issues
            false
        }
        
        // Cache the result
        cachedSdkAvailable = isAvailable
        return isAvailable
    }
    
    /**
     * Checks if calling features are enabled in the UIKit.
     * 
     * This method returns true only if both conditions are met:
     * 1. The CometChat Calls SDK is available at runtime
     * 2. The enableCalling flag is set to true in UIKitSettings
     * 
     * If UIKitSettings or the enableCalling property is not available,
     * this method returns false.
     * 
     * @return true if calling is enabled and available, false otherwise
     */
    public fun isCallingEnabled(): Boolean {
        // First check if SDK is available
        if (!isCallsSDKAvailable()) {
            return false
        }
        
        // Check if UIKitSettings has enableCalling set to true via CometChatUIKit
        return try {
            // Import CometChatUIKit directly since it's in the same module
            val settings = com.cometchat.uikit.core.CometChatUIKit.getAuthSettings()
            settings?.enableCalling ?: false
        } catch (e: Exception) {
            // If UIKitSettings doesn't have enableCalling property yet,
            // or if there's any other issue, return false
            false
        }
    }
    
    /**
     * The session id of the call the Calls SDK currently holds active, or null when there is
     * none or the SDK is not on the classpath. A thin read shared by the ongoing-call screens of
     * both UI Kits, so each tears down only its own session.
     */
    @JvmStatic
    public fun activeSessionId(): String? {
        if (!isCallsSDKAvailable()) return null
        return runCatching { com.cometchat.calls.core.CometChatCalls.getActiveSession()?.sessionId }
            .getOrNull()
    }

    /**
     * True when a call screen that hosts [ownedSessionId] may tear down the Calls SDK's current
     * session in its `onDestroy`.
     *
     * `CallSession.getInstance()` is whichever session is active *now*, not the one the screen
     * was created for. After "End & accept" the next call's screen may already have prepared or
     * joined its own session by the time the old screen is destroyed, so leaving "whatever is
     * active" would drop the new call. A screen leaves only its own session, or when nothing is
     * active (then the leave is a no-op). A screen that never learned its session id owns nothing.
     */
    @JvmStatic
    public fun ownsActiveSession(ownedSessionId: String?): Boolean {
        val active = activeSessionId() ?: return true
        return ownedSessionId != null && active == ownedSessionId
    }

    /**
     * Clears the cached SDK availability result.
     * 
     * This is primarily useful for testing purposes to reset the cached state.
     * In normal usage, the SDK availability doesn't change during runtime.
     */
    internal fun clearCache() {
        cachedSdkAvailable = null
        cachedPushCallsSdkAvailable = null
    }
}
