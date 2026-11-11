package com.cometchat.uikit.core.data.datasource

import com.cometchat.calls.core.CometChatCalls
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.utils.CallInitiationUtils
import com.cometchat.uikit.core.utils.CallingState
import com.cometchat.uikit.core.utils.CallsUtils
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Implementation of [CallButtonsDataSource] that integrates with CometChat SDK
 * for call initiation and active call detection.
 */
internal class CallButtonsDataSourceImpl : CallButtonsDataSource {

    override suspend fun initiateUserCall(receiverId: String, callType: String, receiver: User?): Result<Call> {
        // Ringing v2: the Calls SDK creates the call and prepares the session; the backend
        // pushes the callee. The Chat SDK's initiateCall is deprecated and not used.
        val context = CometChatUIKit.getApplicationContext()
            ?: return Result.failure(
                CometChatException("NOT_INITIALIZED", "CometChatUIKit.init must run before starting a call")
            )
        return CallInitiationUtils.initiateUserCall(context, receiverId, callType, receiver)
    }

    override suspend fun sendGroupCallMessage(groupId: String, callType: String): Result<CustomMessage> {
        return suspendCoroutine { continuation ->
            val customMessage = createGroupCallMessage(groupId, callType)
            CometChatUIKit.sendCustomMessage(customMessage, object : CometChat.CallbackListener<CustomMessage>() {
                override fun onSuccess(message: CustomMessage) {
                    continuation.resume(Result.success(message))
                }

                override fun onError(e: CometChatException) {
                    continuation.resume(Result.failure(e))
                }
            })
        }
    }

    override fun getActiveCall(): Call? = CallingState.getActiveCall()

    override fun hasActiveCallSession(): Boolean {
        return CallsUtils.isCallsSDKAvailable() && CometChatCalls.getActiveSession() != null
    }

    override fun getActiveCallingExtensionCall(): Call? {
        return try {
            CallingState.getActiveCall()
        } catch (e: Exception) {
            null
        }
    }

    override fun isActiveMeeting(): Boolean {
        return try {
            CallingState.isActiveMeeting()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Creates a CustomMessage for initiating a group conference call.
     * @param groupId The GUID of the group
     * @param callType The type of call (audio/video)
     * @return A configured CustomMessage for the group call
     */
    private fun createGroupCallMessage(groupId: String, callType: String): CustomMessage {
        val customData = JSONObject().apply {
            put(UIKitConstants.CallingJSONConstants.CALL_TYPE, callType)
            put(UIKitConstants.CallingJSONConstants.CALL_SESSION_ID, groupId)
        }

        return CustomMessage(
            groupId,
            CometChatConstants.RECEIVER_TYPE_GROUP,
            UIKitConstants.MessageType.MEETING,
            customData
        ).apply {
            metadata = JSONObject().apply {
                put("incrementUnreadCount", true)
                put("pushNotification", UIKitConstants.MessageType.MEETING)
            }
            shouldUpdateConversation(true)
        }
    }
}
