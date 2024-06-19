package com.cometchat.kotlinsampleapp

import android.app.Application
import android.content.res.Configuration
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chatuikit.calls.CometChatCallActivity
import com.cometchat.chatuikit.shared.resources.theme.CometChatTheme
import com.cometchat.chatuikit.shared.resources.theme.Palette
import com.cometchat.kotlinsampleapp.AppUtils.Companion.isNightMode

class Application : Application() {

    override fun onCreate() {
        super.onCreate()
        addCallListener()
        if (isNightMode(this)) {
            Palette.getInstance().mode(CometChatTheme.MODE.DARK)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isNightMode(this)) {
            Palette.getInstance().mode(CometChatTheme.MODE.DARK)
        } else {
            Palette.getInstance().mode(CometChatTheme.MODE.LIGHT)
        }
    }

    private fun addCallListener() {
        val LISTENER_ID = System.currentTimeMillis().toString() + ""
        CometChat.addCallListener(LISTENER_ID, object : CometChat.CallListener() {
            override fun onIncomingCallReceived(call: Call) {
                CometChatCallActivity.launchIncomingCallScreen(applicationContext, call, null)
            }

            override fun onOutgoingCallAccepted(call: Call?) {}
            override fun onOutgoingCallRejected(call: Call?) {}
            override fun onIncomingCallCancelled(call: Call?) {}
        })
    }

}
