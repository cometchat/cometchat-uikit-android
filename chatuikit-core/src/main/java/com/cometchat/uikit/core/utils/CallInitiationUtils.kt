package com.cometchat.uikit.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.cometchat.calls.constants.Constants
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.core.SessionSettings
import com.cometchat.calls.model.InitiateCallOptions
import com.cometchat.calls.model.InitiatedCall
import com.cometchat.calls.model.SessionType
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.R
import com.cometchat.uikit.core.constants.UIKitConstants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

/**
 * Caller side of the ringing v2 flow, on the Calls SDK only.
 *
 * `initiateCall` creates the call on the backend (which pushes the callee) and prepares the
 * media session; `endCall(sessionId, status)` announces cancelled / unanswered to the room.
 * No Chat SDK method is used in the calling flow. The Chat SDK's [Call] stays the UIKit's
 * public call type and is built locally from the [InitiatedCall] plus the receiver the UIKit
 * already holds. It carries the call message's id, which the Calls SDK reads from the
 * initiate response (`InitiatedCall.messageId`), so the caller's own bubble can be matched.
 */
public object CallInitiationUtils {

    private const val TAG = "CallInitiationUtils"

    /**
     * How long [initiateUserCall] waits for the Calls SDK to answer before giving up. The guard
     * below is cleared when the wait ends, so a callback that never arrives (a captive portal,
     * a Wi-Fi with no route out, a stalled socket) cannot lock calling until the app restarts.
     */
    internal const val INITIATE_TIMEOUT_MS: Long = 30_000L

    /**
     * True while a 1:1 call is being started. Every entry point (call buttons, call logs, the
     * apps' user details) ends up in [initiateUserCall], and two taps 120 ms apart both used to
     * reach the server before either session existed, so two calls were created and the second
     * one's busy reply closed the first (SM-07). Set before the request and cleared when the
     * Calls SDK answers, the wait times out ([INITIATE_TIMEOUT_MS]) or the caller is cancelled;
     * from then on the prepared session makes the active-call check refuse.
     */
    private val initiating = AtomicBoolean(false)

    /**
     * Starts a 1:1 call with [receiverId] through `CometChatCalls.initiateCall`.
     *
     * @param context Any context; the Calls SDK prepares the session with it.
     * @param callType [CometChatConstants.CALL_TYPE_AUDIO] or [CometChatConstants.CALL_TYPE_VIDEO]
     * @param receiver The callee as the UIKit knows them, for the name and avatar on the
     * outgoing screen; the Calls SDK returns only the uid.
     * @return the initiated call as a Chat SDK [Call] built from the Calls SDK's response
     */
    public suspend fun initiateUserCall(
        context: Context,
        receiverId: String,
        callType: String,
        receiver: User? = null
    ): Result<Call> {
        if (!CallsUtils.isCallsSDKAvailable()) {
            return Result.failure(
                CometChatException("CALLS_SDK_MISSING", "calls-sdk-android is not on the classpath")
            )
        }
        // Offline, initiateCall never reports back, so the tap looked ignored (ENG-40540).
        if (!isOnline(context)) {
            return Result.failure(noInternet(context))
        }
        if (!initiating.compareAndSet(false, true)) {
            CometChatLogger.d(TAG, "a call is already being started; ignoring this request")
            return Result.failure(
                CometChatException(
                    UIKitConstants.CallErrorCodes.CALL_IN_PROGRESS,
                    "A call is already being started",
                    "Another call request is still in flight"
                )
            )
        }
        // A call the backend created but that nobody will own: the success callback ran, but
        // the result was lost to a timeout or to the caller's cancellation before it was
        // delivered (prompt cancellation drops a resumed value). Whoever notices first cancels
        // it on the backend, once, so the callee does not keep ringing with no outgoing screen.
        val undelivered = AtomicReference<InitiatedCall?>(null)
        val initiated = try {
            // The Calls SDK gives no timeout of its own. Without this cap a request that never
            // reports back would keep `initiating` set for the rest of the process, and every
            // later start would be refused as CALL_IN_PROGRESS, which the callers drop silently.
            val result = withTimeoutOrNull(INITIATE_TIMEOUT_MS) { requestCall(context, receiverId, callType, undelivered) }
            if (result != null) {
                undelivered.set(null) // delivered: the caller owns the call from here
                result
            } else {
                CometChatLogger.e(TAG, "initiateCall did not report back within ${INITIATE_TIMEOUT_MS}ms")
                discardUndelivered(undelivered)
                Result.failure(noInternet(context))
            }
        } catch (e: CancellationException) {
            discardUndelivered(undelivered)
            throw e
        } finally {
            initiating.set(false)
        }
        // Team decision (7 Oct): no Chat SDK method in the calling flow, so the call message is
        // not fetched. The Call is built locally and takes its id from InitiatedCall.messageId.
        return initiated.map { call -> toCall(call, receiverId, callType, receiver) }
    }

    /**
     * True when the active network reaches the internet. A network that only claims internet
     * access (a captive portal, a Wi-Fi with no route out) is not validated and counts as
     * offline, since `initiateCall` would never report back on it.
     */
    private fun isOnline(context: Context): Boolean {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun noInternet(context: Context): CometChatException =
        CometChatException(
            UIKitConstants.CallErrorCodes.NO_INTERNET,
            context.getString(R.string.cometchat_call_no_internet)
        )

    /** Cancels a created call nobody received, once; later calls are no-ops. */
    private fun discardUndelivered(undelivered: AtomicReference<InitiatedCall?>) {
        val call = undelivered.getAndSet(null) ?: return
        val sessionId = call.sessionId ?: return
        CometChatLogger.d(TAG, "initiateCall succeeded for $sessionId but the result was not delivered; cancelling it")
        CallingState.forgetPlaced(sessionId)
        endCall(sessionId, UIKitConstants.CallStatusConstants.CANCELLED) { }
    }

    private suspend fun requestCall(
        context: Context,
        receiverId: String,
        callType: String,
        undelivered: AtomicReference<InitiatedCall?>
    ): Result<InitiatedCall> =
        suspendCancellableCoroutine<Result<InitiatedCall>> { continuation ->
            val options = InitiateCallOptions().apply {
                type = callType
                receiverType = Constants.RECEIVER_TYPE_USER
                sessionSettings = sessionSettingsFor(callType, CometChatUIKit.getSessionSettingsBuilder())
            }
            CometChatCalls.initiateCall(
                context,
                receiverId,
                options,
                object : CometChatCalls.CallbackListener<InitiatedCall>() {
                    override fun onSuccess(call: InitiatedCall) {
                        // The backend has created the call and is pushing the callee. Until the
                        // caller receives it, it is "undelivered": a timeout or cancellation
                        // (before or after this resume) cancels it through discardUndelivered.
                        undelivered.set(call)
                        if (!continuation.isActive) {
                            // The wait already timed out or the caller already went away.
                            discardUndelivered(undelivered)
                            return
                        }
                        // The ring window counts from here, not from when the screen opens.
                        call.sessionId?.let { CallingState.markPlaced(it) }
                        continuation.resume(Result.success(call))
                    }

                    override fun onError(e: com.cometchat.calls.exceptions.CometChatException) {
                        if (continuation.isActive) continuation.resume(Result.failure(toChatException(e)))
                    }
                }
            )
        }

    /**
     * True when [error] is the [UIKitConstants.CallErrorCodes.CALL_IN_PROGRESS] refusal of a
     * second start: callers drop it silently instead of showing an error for a double tap.
     */
    @JvmStatic
    public fun isCallInProgress(error: Throwable?): Boolean =
        (error as? CometChatException)?.code == UIKitConstants.CallErrorCodes.CALL_IN_PROGRESS

    /**
     * [initiateUserCall] with the application context captured by `CometChatUIKit.init`, for
     * callers that have no context of their own.
     */
    public suspend fun initiateUserCall(
        receiverId: String,
        callType: String,
        receiver: User? = null
    ): Result<Call> {
        val context = CometChatUIKit.getApplicationContext()
            ?: return Result.failure(
                CometChatException("NOT_INITIALIZED", "CometChatUIKit.init must run before starting a call")
            )
        return initiateUserCall(context, receiverId, callType, receiver)
    }

    /**
     * The settings `initiateCall` prepares the caller's session with.
     *
     * The Calls SDK does not derive the session type from `InitiateCallOptions.type`, and its
     * default is video, so an audio call left on the defaults opens the caller's camera and
     * streams it to the callee. The session type is therefore always set from [callType].
     *
     * It is also marked a peer (1:1) call, as the Push SDK marks the callee's: the Calls SDK
     * then leaves the room when the other side leaves, so the caller's screen closes when the
     * callee hangs up instead of waiting for the server to close the connection.
     *
     * The ongoing-call screen later joins this prepared session with the same builder (the
     * app's, or a fresh one) and sets the same two values. The prepared settings must match
     * that join, so nothing else is changed here.
     */
    internal fun sessionSettingsFor(
        callType: String,
        appBuilder: CometChatCalls.SessionSettingsBuilder?
    ): SessionSettings {
        val isVideo = callType.equals(CometChatConstants.CALL_TYPE_VIDEO, ignoreCase = true)
        val sessionType = if (isVideo) SessionType.VIDEO else SessionType.VOICE
        return (appBuilder ?: CometChatCalls.SessionSettingsBuilder())
            .setSessionType(sessionType)
            .setPeerCall(true)
            .build()
    }

    /**
     * Ends [sessionId] with [status] (cancelled, unanswered, ...) through the Calls SDK.
     * The result is reported through [onDone]; the local screen closes either way.
     */
    public fun endCall(sessionId: String, status: String, onDone: (Result<Unit>) -> Unit) {
        if (!CallsUtils.isCallsSDKAvailable()) {
            onDone(Result.failure(CometChatException("CALLS_SDK_MISSING", "calls-sdk-android is not on the classpath")))
            return
        }
        CometChatCalls.endCall(
            sessionId,
            status,
            object : CometChatCalls.CallbackListener<Void>() {
                override fun onSuccess(result: Void?) = onDone(Result.success(Unit))

                override fun onError(e: com.cometchat.calls.exceptions.CometChatException) {
                    CometChatLogger.e(TAG, "endCall($status) failed: ${e.code} ${e.message}")
                    onDone(Result.failure(toChatException(e)))
                }
            }
        )
    }

    /**
     * Converts a Calls SDK [InitiatedCall] into a Chat SDK [Call] through [Call.fromJson].
     * Fields the Calls SDK leaves empty fall back to [receiverId], [callType], [receiver] and
     * the logged-in user as the caller.
     *
     * Internal: it takes a `compileOnly` Calls SDK type, which must not become part of the
     * UIKit's public API.
     */
    internal fun toCall(
        initiated: InitiatedCall,
        receiverId: String? = null,
        callType: String? = null,
        receiver: User? = null
    ): Call {
        val me = CometChat.getLoggedInUser()
        val callerUid = initiated.sender?.takeIf { it.isNotBlank() } ?: me?.uid ?: ""
        val callerName = initiated.senderName?.takeIf { it.isNotBlank() }
            ?: me?.takeIf { it.uid == callerUid }?.name ?: callerUid
        val callerAvatar = initiated.senderAvatar?.takeIf { it.isNotBlank() }
            ?: me?.takeIf { it.uid == callerUid }?.avatar
        val initiatedAt = initiated.initiatedAt.let { if (it > 1_000_000_000_000L) it / 1000 else it }
            .takeIf { it > 0 } ?: (System.currentTimeMillis() / 1000)
        return CallMessageJson.build(
            sessionId = initiated.sessionId ?: "",
            callType = initiated.type?.takeIf { it.isNotBlank() } ?: callType ?: CometChatConstants.CALL_TYPE_AUDIO,
            callStatus = initiated.status?.takeIf { it.isNotBlank() } ?: CometChatConstants.CALL_STATUS_INITIATED,
            receiverType = initiated.receiverType?.takeIf { it.isNotBlank() } ?: CometChatConstants.RECEIVER_TYPE_USER,
            receiverId = initiated.receiver?.takeIf { it.isNotBlank() } ?: receiverId ?: receiver?.uid
                ?: me?.uid?.takeIf { !it.equals(callerUid, ignoreCase = true) } ?: "",
            receiverName = receiver?.name,
            receiverAvatar = receiver?.avatar,
            callerUid = callerUid,
            callerName = callerName,
            callerAvatar = callerAvatar,
            initiatedAtSec = initiatedAt,
            conversationId = initiated.conversationId,
            messageId = initiated.messageId?.toLongOrNull()
        )
    }

    /** Maps a Calls SDK exception onto the Chat SDK exception type the UIKit's flows carry. */
    internal fun toChatException(e: com.cometchat.calls.exceptions.CometChatException): CometChatException {
        return CometChatException(e.code ?: "ERROR", e.message ?: "Call failed", e.details ?: "")
    }
}

/**
 * Builds the JSON `Call.fromJson` expects from the handful of fields the Calls SDK and the
 * Push SDK carry, so both the incoming (push) and outgoing (initiated) paths produce the same
 * Chat SDK [Call] for the UIKit's components.
 */
internal object CallMessageJson {

    fun build(
        sessionId: String,
        callType: String,
        callStatus: String,
        receiverType: String,
        receiverId: String,
        receiverName: String? = null,
        receiverAvatar: String? = null,
        callerUid: String,
        callerName: String,
        callerAvatar: String?,
        initiatedAtSec: Long,
        conversationId: String?,
        messageId: Long? = null
    ): Call {
        val isGroup = receiverType.equals(CometChatConstants.RECEIVER_TYPE_GROUP, ignoreCase = true)
        val normalizedReceiverType =
            if (isGroup) CometChatConstants.RECEIVER_TYPE_GROUP else CometChatConstants.RECEIVER_TYPE_USER

        val caller = JSONObject()
            .put(CometChatConstants.UserKeys.USER_KEY_UID, callerUid)
            .put(CometChatConstants.UserKeys.USER_KEY_NAME, callerName)
        callerAvatar?.takeIf { it.isNotBlank() }?.let {
            caller.put(CometChatConstants.UserKeys.USER_KEY_AVATAR, it)
        }

        val receiver = if (isGroup) {
            JSONObject().put(CometChatConstants.GroupKeys.KEY_GROUP_GUID, receiverId)
        } else {
            JSONObject().put(CometChatConstants.UserKeys.USER_KEY_UID, receiverId)
        }
        receiverName?.takeIf { it.isNotBlank() }?.let { receiver.put(CometChatConstants.UserKeys.USER_KEY_NAME, it) }
        receiverAvatar?.takeIf { it.isNotBlank() }?.let { receiver.put(CometChatConstants.UserKeys.USER_KEY_AVATAR, it) }

        val callEntity = JSONObject()
            .put(CometChatConstants.CallKeys.CALL_SESSION_ID, sessionId)
            .put(CometChatConstants.CallKeys.CALL_STATUS, callStatus)
            .put(CometChatConstants.CallKeys.CALL_INITIATED_AT, initiatedAtSec)
            .put(
                CometChatConstants.ResponseKeys.KEY_DATA,
                JSONObject().put(
                    CometChatConstants.ResponseKeys.KEY_ENTITIES,
                    JSONObject()
                        .put(
                            CometChatConstants.MessageKeys.KEY_SENDER,
                            JSONObject().put(CometChatConstants.ActionKeys.KEY_ENTITY, caller)
                        )
                        .put(
                            CometChatConstants.MessageKeys.KEY_RECEIVER_UID,
                            JSONObject()
                                .put(CometChatConstants.ActionKeys.KEY_ENTITY_TYPE, normalizedReceiverType)
                                .put(CometChatConstants.ActionKeys.KEY_ENTITY, receiver)
                        )
                )
            )

        val entities = JSONObject()
            .put(
                CometChatConstants.ActionKeys.KEY_ON,
                JSONObject().put(CometChatConstants.ResponseKeys.KEY_ENTITITY, callEntity)
            )
            .put(
                CometChatConstants.ActionKeys.KEY_BY,
                JSONObject().put(CometChatConstants.ActionKeys.KEY_ENTITY, caller)
            )
            .put(
                CometChatConstants.ActionKeys.KEY_FOR,
                JSONObject()
                    .put(CometChatConstants.ActionKeys.KEY_ENTITY_TYPE, normalizedReceiverType)
                    .put(CometChatConstants.ActionKeys.KEY_ENTITY, receiver)
            )

        val message = JSONObject()
            .put(CometChatConstants.MessageKeys.KEY_MESSAGE_CATEGORY, CometChatConstants.CATEGORY_CALL)
            .put(CometChatConstants.CallKeys.CALL_TYPE, callType)
            .put(CometChatConstants.CallKeys.CALL_RECEIVER_TYPE, normalizedReceiverType)
            .put(CometChatConstants.MessageKeys.KEY_RECEIVER_UID, receiverId)
            .put(CometChatConstants.MessageKeys.KEY_SENT_AT, initiatedAtSec)
            .put(
                CometChatConstants.ResponseKeys.KEY_DATA,
                JSONObject()
                    .put(CometChatConstants.ResponseKeys.KEY_ACTION, callStatus)
                    .put(CometChatConstants.ResponseKeys.KEY_ENTITIES, entities)
            )
        messageId?.takeIf { it > 0 }?.let { message.put(CometChatConstants.MessageKeys.KEY_SEND_MESSAGE_ID, it) }
        conversationId?.takeIf { it.isNotBlank() }?.let {
            message.put(CometChatConstants.ConversationKeys.KEY_CONVERSATION_ID, it)
        }
        return Call.fromJson(message.toString())
    }
}
