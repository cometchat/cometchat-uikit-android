package com.cometchat.sampleapp.kotlin.push.fcm

import android.util.Log
import com.cometchat.pushnotification.CometChatPushNotifications
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Hands every push to the CometChat Push Notifications SDK (2.0). Call pushes are never
 * dropped: in Ringing v2 the push is the only signal that a call is coming in, in every app
 * state, and the SDK rings it itself. `sentTime` lets the SDK ignore a push Firebase held
 * back while the device was dozing, so it does not ring for a call that is already over.
 */
class AppFCMService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AppFCMService"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val handled = CometChatPushNotifications.handlePushNotification(
            context = this,
            data = message.data,
            sentTimeMs = message.sentTime
        )
        if (!handled) {
            Log.d(TAG, "Push not from CometChat; nothing to do")
        }
    }

    override fun onNewToken(token: String) {
        // The SDK re-registers the rotated token under the logged-in user.
        CometChatPushNotifications.handleTokenRefresh(token)
    }
}
