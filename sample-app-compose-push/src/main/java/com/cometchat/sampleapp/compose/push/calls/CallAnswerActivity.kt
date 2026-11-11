package com.cometchat.sampleapp.compose.push.calls

import android.app.Activity
import android.os.Build
import android.os.Bundle
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.pushnotification.calls.CallBundles
import com.cometchat.uikit.core.utils.CallRingingUtils
import com.cometchat.uikit.compose.presentation.ongoingcall.ui.CometChatOngoingCallActivity

/**
 * The Push SDK's answer activity (`PNConfiguration.setAnswerActivity`). The Push SDK rings,
 * asks for the microphone and camera, and opens this screen with the answered call in the
 * intent. It reads that call, hands it to the UI Kit's ongoing-call screen, and finishes.
 *
 * This is the only place the app bridges the Push SDK and the UI Kit: the kit itself has no
 * dependency on the Push SDK.
 */
class CallAnswerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val incoming = CallBundles.getCall(intent)
        if (incoming == null) {
            finish()
            return
        }

        val call = CallRingingUtils.buildIncomingCall(
            sessionId = incoming.sessionId,
            callType = if (incoming.isVideo) CometChatConstants.CALL_TYPE_VIDEO else CometChatConstants.CALL_TYPE_AUDIO,
            receiverType = incoming.receiverType,
            receiverUid = incoming.receiverUid,
            callerUid = incoming.callerUid,
            callerName = incoming.callerName,
            callerAvatarUrl = incoming.callerAvatarUrl,
            initiatedAtMs = incoming.initiatedAtMs,
            conversationId = incoming.conversationId
        )
        CometChatOngoingCallActivity.launchOngoingCallActivity(this, call)
        finish()
    }
}
