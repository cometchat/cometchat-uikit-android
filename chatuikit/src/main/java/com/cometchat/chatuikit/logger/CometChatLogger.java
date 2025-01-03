package com.cometchat.chatuikit.logger;

import android.util.Log;

import com.cometchat.chatuikit.BuildConfig;

public class CometChatLogger {
    private static final String TAG = CometChatLogger.class.getSimpleName();
    private static boolean isLogEnabled = BuildConfig.DEBUG;

    // Method to enable or disable logs at runtime
    public static void enableLog(boolean enable) {
        isLogEnabled = enable;
    }

    // General logging methods for different log levels
    public static void d(String tag, String message) {
        if (isLogEnabled) {
            Log.d(tag, message);
        }
    }

    public static void i(String tag, String message) {
        if (isLogEnabled) {
            Log.i(tag, message);
        }
    }

    public static void w(String tag, String message) {
        if (isLogEnabled) {
            Log.w(tag, message);
        }
    }

    public static void e(String tag, String message) {
        if (isLogEnabled) {
            Log.e(tag, message);
        }
    }

    // Optional method for logging exceptions
    public static void e(String tag, String message, Throwable throwable) {
        if (isLogEnabled) {
            Log.e(tag, message, throwable);
        }
    }

    // Verbose logging (less commonly used)
    public static void v(String tag, String message) {
        if (isLogEnabled) {
            Log.v(tag, message);
        }
    }
}
