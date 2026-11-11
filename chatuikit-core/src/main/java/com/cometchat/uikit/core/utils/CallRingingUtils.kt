package com.cometchat.uikit.core.utils

import com.cometchat.calls.constants.Constants
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat

/**
 * Incoming-call helpers for the ringing v2 flow, with no dependency on the Push SDK.
 *
 * The Push SDK calls module (push-notifications-calls-android 2.0+) is the app's dependency.
 * It rings, draws the incoming screen, declines, times out and posts missed calls. When the
 * user answers it launches the app's answer activity with the call in the intent. That
 * activity reads the call with the Push SDK's `CallBundles.getCall(intent)`, turns it into a
 * Chat SDK [Call] with [buildIncomingCall], and opens the UIKit's ongoing-call screen with
 * `CometChatOngoingCallActivity.launchOngoingCallActivity(context, call)`.
 *
 * What stays in the kit is Push-free: building the [Call] the UIKit's components take, the
 * permissions a custom ring screen asks for before answering, and a decline through the
 * Calls SDK for an app that rings without the Push SDK.
 *
 * None of this uses the Chat SDK's deprecated calling APIs.
 */
public object CallRingingUtils {

    private const val TAG = "CallRingingUtils"

    /**
     * Builds the Chat SDK [Call] the UIKit's incoming and ongoing components take, from the
     * fields a Push SDK `IncomingCall` carries. The app's answer activity (or custom ring
     * screen) calls this so the kit never depends on the Push SDK's types.
     *
     * Only the fields the push payload carries are filled: session id, type, status
     * (initiated), initiator, receiver, initiated-at and conversation id.
     *
     * @param sessionId the call session id
     * @param callType [CometChatConstants.CALL_TYPE_AUDIO] or [CometChatConstants.CALL_TYPE_VIDEO]
     * @param receiverType [CometChatConstants.RECEIVER_TYPE_USER] or [CometChatConstants.RECEIVER_TYPE_GROUP]
     * @param receiverUid the callee uid or group guid; the logged-in user when null
     * @param callerUid the caller's uid
     * @param callerName the caller's display name; falls back to the uid
     * @param callerAvatarUrl the caller's avatar, if any
     * @param initiatedAtMs when the call was placed, in epoch milliseconds
     * @param conversationId the conversation the call belongs to, if known
     */
    @JvmStatic
    @JvmOverloads
    public fun buildIncomingCall(
        sessionId: String,
        callType: String,
        receiverType: String = CometChatConstants.RECEIVER_TYPE_USER,
        receiverUid: String? = null,
        callerUid: String? = null,
        callerName: String? = null,
        callerAvatarUrl: String? = null,
        initiatedAtMs: Long = System.currentTimeMillis(),
        conversationId: String? = null
    ): Call {
        val isGroup = receiverType.equals(CometChatConstants.RECEIVER_TYPE_GROUP, ignoreCase = true)
        val isVideo = callType.equals(CometChatConstants.CALL_TYPE_VIDEO, ignoreCase = true)
        val caller = callerUid ?: ""
        return CallMessageJson.build(
            sessionId = sessionId,
            callType = if (isVideo) CometChatConstants.CALL_TYPE_VIDEO else CometChatConstants.CALL_TYPE_AUDIO,
            callStatus = CometChatConstants.CALL_STATUS_INITIATED,
            receiverType = if (isGroup) CometChatConstants.RECEIVER_TYPE_GROUP else CometChatConstants.RECEIVER_TYPE_USER,
            receiverId = receiverUid ?: CometChat.getLoggedInUser()?.uid ?: "",
            callerUid = caller,
            callerName = callerName?.takeIf { it.isNotBlank() } ?: caller,
            callerAvatar = callerAvatarUrl,
            initiatedAtSec = initiatedAtMs / 1000,
            conversationId = conversationId
        )
    }

    /**
     * The runtime permissions a custom ring screen asks for before answering. The Push SDK
     * prompts only for its own notification's Answer button. A denied microphone still joins,
     * muted.
     */
    @JvmStatic
    public fun permissionsToAnswer(call: Call?): Array<String> = permissionsForCallType(call?.type)

    /**
     * The runtime permissions a call of [callType] needs: the microphone, plus the camera only
     * for a video call. A voice call never asks for the camera (ENG-40552).
     */
    @JvmStatic
    public fun permissionsForCallType(callType: String?): Array<String> {
        val isVideo = CometChatConstants.CALL_TYPE_VIDEO.equals(callType, ignoreCase = true)
        return if (isVideo) {
            arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA)
        } else {
            arrayOf(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    /**
     * Declines [sessionId] through the Calls SDK, as rejected. For an app that rings with
     * the Push SDK, the decline belongs to the Push SDK (`CometChatCallNotifications.decline`),
     * which also stops its ring and picks busy over rejected; this is the fallback for a ring
     * screen the app drives itself.
     */
    @JvmStatic
    public fun decline(sessionId: String) {
        if (!CallsUtils.isCallsSDKAvailable()) return
        CometChatCalls.endCall(
            sessionId,
            Constants.CALL_STATUS_REJECTED,
            object : CometChatCalls.CallbackListener<Void>() {
                override fun onSuccess(result: Void?) = Unit

                override fun onError(e: com.cometchat.calls.exceptions.CometChatException) {
                    CometChatLogger.e(TAG, "endCall(rejected) failed: ${e.message}")
                }
            }
        )
    }
}
