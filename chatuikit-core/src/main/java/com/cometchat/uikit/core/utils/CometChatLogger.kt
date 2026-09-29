package com.cometchat.uikit.core.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

/**
 * Central gated logger for the CometChat UI Kit modules.
 *
 * All UI Kit log output goes through this object instead of [android.util.Log],
 * so that nothing is written to logcat in production apps by default.
 *
 * Default state: logging is enabled only when the **host application** is a
 * debuggable build ([ApplicationInfo.FLAG_DEBUGGABLE]), detected during
 * `CometChatUIKit.init()`. The library's own build type is irrelevant — the
 * published AAR is compiled in release mode, so a `BuildConfig.DEBUG` gate
 * would disable logging even in consumers' debug builds.
 *
 * Integrators can override the default in either direction, at any time:
 * ```
 * CometChatLogger.enableLog(true)   // logs in a release build
 * CometChatLogger.enableLog(false)  // silence in a debug build
 * ```
 * An explicit [enableLog] call always wins over the detected default.
 */
public object CometChatLogger {

    @Volatile
    private var isLogEnabled: Boolean = false

    @Volatile
    private var explicitlySet: Boolean = false

    /**
     * Enables or disables UI Kit logging at runtime, regardless of build type.
     * Overrides the debuggable-build default and survives later `init()` calls.
     */
    @JvmStatic
    public fun enableLog(enable: Boolean) {
        explicitlySet = true
        isLogEnabled = enable
    }

    /** Whether UI Kit logging is currently enabled. */
    @JvmStatic
    public fun isEnabled(): Boolean = isLogEnabled

    /**
     * Captures the host app's debuggable flag as the logging default.
     * Called from `CometChatUIKit.init()` / `initFromSettings()`; a prior
     * explicit [enableLog] choice is never overridden.
     */
    internal fun initFromContext(context: Context) {
        if (!explicitlySet) {
            // applicationInfo is a platform type; guard against mocked/hostile
            // contexts that return null (unit tests, instrumentation shims)
            val flags = context.applicationInfo?.flags ?: return
            isLogEnabled = (flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }
    }

    @JvmStatic
    public fun d(tag: String, message: String) {
        if (isLogEnabled) Log.d(tag, message)
    }

    @JvmStatic
    public fun d(tag: String, message: String, throwable: Throwable?) {
        if (isLogEnabled) Log.d(tag, message, throwable)
    }

    @JvmStatic
    public fun i(tag: String, message: String) {
        if (isLogEnabled) Log.i(tag, message)
    }

    @JvmStatic
    public fun w(tag: String, message: String) {
        if (isLogEnabled) Log.w(tag, message)
    }

    @JvmStatic
    public fun w(tag: String, message: String, throwable: Throwable?) {
        if (isLogEnabled) Log.w(tag, message, throwable)
    }

    @JvmStatic
    public fun e(tag: String, message: String) {
        if (isLogEnabled) Log.e(tag, message)
    }

    @JvmStatic
    public fun e(tag: String, message: String, throwable: Throwable?) {
        if (isLogEnabled) Log.e(tag, message, throwable)
    }

    @JvmStatic
    public fun v(tag: String, message: String) {
        if (isLogEnabled) Log.v(tag, message)
    }
}
