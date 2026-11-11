package com.cometchat.uikit.core.utils

import com.cometchat.chat.core.Call

/**
 * Compatibility alias kept for one release so 6.1 callers keep compiling. The Chat SDK call
 * listeners it used to register are gone with Ringing v2; the state itself lives in
 * [CallingState], which this object delegates to. For the Calls SDK's own view of an active
 * session use `CometChatCalls.getActiveSession()`, or
 * `CallButtonsDataSource.hasActiveCallSession()` from a custom data source.
 */
@Deprecated(
    message = "Replaced by CallingState in 6.2.0; this alias delegates to it and will be removed in a later release.",
    replaceWith = ReplaceWith("CallingState", "com.cometchat.uikit.core.utils.CallingState")
)
public object CallManager {

    @Deprecated(
        "Use CallingState.setActiveCall(call)",
        ReplaceWith("CallingState.setActiveCall(call)", "com.cometchat.uikit.core.utils.CallingState")
    )
    public fun setActiveCall(call: Call?) {
        CallingState.setActiveCall(call)
    }

    @Deprecated(
        "Use CallingState.getActiveCall()",
        ReplaceWith("CallingState.getActiveCall()", "com.cometchat.uikit.core.utils.CallingState")
    )
    public fun getActiveCall(): Call? = CallingState.getActiveCall()

    @Deprecated(
        "Use CallingState.setActiveCall(null)",
        ReplaceWith("CallingState.setActiveCall(null)", "com.cometchat.uikit.core.utils.CallingState")
    )
    public fun clearActiveCall() {
        CallingState.setActiveCall(null)
    }

    @Deprecated(
        "Use CallingState.getActiveCall() != null",
        ReplaceWith("CallingState.getActiveCall() != null", "com.cometchat.uikit.core.utils.CallingState")
    )
    public fun hasActiveCall(): Boolean = CallingState.getActiveCall() != null

    @Deprecated(
        "Use CallingState.setIsActiveMeeting(isActive)",
        ReplaceWith("CallingState.setIsActiveMeeting(isActive)", "com.cometchat.uikit.core.utils.CallingState")
    )
    public fun setIsActiveMeeting(isActive: Boolean) {
        CallingState.setIsActiveMeeting(isActive)
    }

    @Deprecated(
        "Use CallingState.isActiveMeeting()",
        ReplaceWith("CallingState.isActiveMeeting()", "com.cometchat.uikit.core.utils.CallingState")
    )
    public fun isActiveMeeting(): Boolean = CallingState.isActiveMeeting()

    @Deprecated(
        "Use CallingState.clear()",
        ReplaceWith("CallingState.clear()", "com.cometchat.uikit.core.utils.CallingState")
    )
    public fun reset() {
        CallingState.clear()
    }
}
