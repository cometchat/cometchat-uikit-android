package com.cometchat.uikit.core.offline

import androidx.annotation.VisibleForTesting
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.InteractiveMessage
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.MessageFailureReason
import com.cometchat.chat.models.TextMessage
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.utils.CometChatThreadSubscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Collections
import com.cometchat.chat.models.MessageStatus as SdkMessageStatus
import com.cometchat.uikit.core.events.MessageStatus as KitSendStatus

/**
 * The one send helper for the offline (ON) path (G3, G4, G12; BEH-SEND-01/10/33). Owned by **W4**
 * (ENG-41089).
 *
 * Every kit send funnels through `CometChatUIKit.sendTextMessage/sendMediaMessage/sendCustomMessage`;
 * W1's hooks call [interceptTextMessage] / [interceptMediaMessage] / [interceptCustomMessage] only
 * when [OfflineGate.isOn] said yes for this send, so the captured mode is always ON here. With
 * offline off none of this runs and the send makes today's call with today's payload.
 *
 * Per send it uses the SDK's [CometChat.MessageSentListener] and feeds every callback into
 * [OfflineHub.reportSend], which dedupes it against the outbox listener (one event per transition
 * per `localMessageId`). The caller's callback:
 * - is **not** resolved at pending;
 * - resolves on sent;
 * - rejects only on a final failure (status FAILED) or a direct error (no pending was seen);
 * - is never rejected because the message was queued (`onError` after pending with status still
 *   PENDING, BEH-SEND-10);
 * - is rejected once, with `ERROR_OFFLINE_MESSAGE_DISCARDED`, when the session ends (logout or a
 *   user switch, BEH-AUTH-04 R132/R150) or the offline cache is reset (every reset drops the unsent
 *   messages; the SDK fails a readable one per call first, which settles it, and a wipe reports
 *   nothing per call). The kit's own callers treat that as quiet ([isQuietRejection]).
 *
 * The legacy [CometChatMessageEvent.MessageSent] events keep their meaning on this path: IN_PROGRESS
 * once at pending, SUCCESS on sent, ERROR on a final or direct failure. The message list routes the
 * ON-path rows from the hub, not from these events (see [MessageListOfflineSendRows]).
 *
 * The kit never writes SDK message state: status and failure reason are read, never set. A direct
 * failure is marked the way the OFF path marks it today — an `error` entry in the message metadata.
 */
internal object OfflineSend {

    /** The SDK calls this helper makes. A seam so unit tests run without a live SDK. */
    internal interface Sdk {
        fun sendText(message: TextMessage, listener: CometChat.MessageSentListener)
        fun sendMedia(message: MediaMessage, listener: CometChat.MessageSentListener)
        fun sendCustom(message: CustomMessage, listener: CometChat.MessageSentListener)
        fun sendInteractive(message: InteractiveMessage, listener: CometChat.MessageSentListener)
        /** Today's offline-off interactive send: the direct, server-authoritative overload. */
        fun sendInteractiveDirect(message: InteractiveMessage, listener: CometChat.CallbackListener<InteractiveMessage>)
        fun retry(localMessageId: String, listener: CometChat.MessageSentListener)
        fun discard(localMessageId: String, callback: CometChat.CallbackListener<String>)
        fun loggedInUser(): com.cometchat.chat.models.User?
    }

    private object DefaultSdk : Sdk {
        override fun sendText(message: TextMessage, listener: CometChat.MessageSentListener) =
            CometChat.sendMessage(message, listener)

        override fun sendMedia(message: MediaMessage, listener: CometChat.MessageSentListener) =
            CometChat.sendMediaMessage(message, listener)

        override fun sendCustom(message: CustomMessage, listener: CometChat.MessageSentListener) =
            CometChat.sendCustomMessage(message, listener)

        override fun sendInteractive(message: InteractiveMessage, listener: CometChat.MessageSentListener) =
            CometChat.sendInteractiveMessage(message, listener)

        @Suppress("DEPRECATION")
        override fun sendInteractiveDirect(
            message: InteractiveMessage,
            listener: CometChat.CallbackListener<InteractiveMessage>
        ) = CometChat.sendInteractiveMessage(message, listener)

        override fun retry(localMessageId: String, listener: CometChat.MessageSentListener) =
            CometChat.retryMessage(localMessageId, listener)

        override fun discard(localMessageId: String, callback: CometChat.CallbackListener<String>) =
            CometChat.discardLocalMessage(localMessageId, callback)

        override fun loggedInUser() = runCatching { CometChat.getLoggedInUser() }.getOrNull()
    }

    @VisibleForTesting
    @Volatile
    internal var sdk: Sdk = DefaultSdk

    /** Error code the SDK uses for a removed unsent message; also the logout rejection code. */
    const val ERROR_DISCARDED: String = "ERROR_OFFLINE_MESSAGE_DISCARDED"
    const val ERROR_IN_FLIGHT: String = "ERROR_OFFLINE_MESSAGE_IN_FLIGHT"
    const val ERROR_NOT_FOUND: String = "ERROR_OFFLINE_MESSAGE_NOT_FOUND"
    private const val ERROR_NOT_LOGGED_IN: String = "ERROR_USER_NOT_LOGGED_IN"
    private const val ERROR_NOT_CONNECTED: String = "ERROR_INTERNET_UNAVAILABLE"

    private const val MAX_DIRECT_IDS = 512

    /**
     * localMessageIds (or muids, when the SDK stamped none) whose failure was direct — no pending
     * was ever seen, so no outbox row exists. Retry on these is a fresh send; Delete is memory-only.
     */
    private val directFailures: MutableMap<String, Unit> = Collections.synchronizedMap(
        object : LinkedHashMap<String, Unit>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Unit>?): Boolean =
                size > MAX_DIRECT_IDS
        }
    )

    /** Sends whose caller has not been settled yet; rejected once on a session end or a cache reset. */
    private val openCalls: MutableSet<SendCall<*>> = Collections.synchronizedSet(LinkedHashSet())

    @Volatile
    private var sessionWatchStarted = false
    private val watchScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    fun interceptTextMessage(
        message: TextMessage,
        callbackListener: CometChat.CallbackListener<TextMessage>?
    ): Boolean {
        prepare(message)
        val call = SendCall(message, callbackListener, OfflineHub.gen)
        start(call)
        sdk.sendText(message, call.listener)
        return true
    }

    fun interceptMediaMessage(
        message: MediaMessage,
        callbackListener: CometChat.CallbackListener<MediaMessage>?
    ): Boolean {
        prepare(message)
        val call = SendCall(message, callbackListener, OfflineHub.gen)
        start(call)
        sdk.sendMedia(message, call.listener)
        return true
    }

    fun interceptCustomMessage(
        message: CustomMessage,
        callbackListener: CometChat.CallbackListener<CustomMessage>?
    ): Boolean {
        prepare(message)
        message.shouldSendNotification(true)
        val call = SendCall(message, callbackListener, OfflineHub.gen)
        start(call)
        sdk.sendCustom(message, call.listener)
        return true
    }

    /**
     * True for a rejection the kit must never surface as an error (G12): the session ended (logout
     * or user switch) or the unsent message was removed. The row removal is the only signal.
     */
    @JvmStatic
    fun isQuietRejection(e: Throwable?): Boolean {
        val code = (e as? CometChatException)?.code ?: return false
        return code == ERROR_DISCARDED || code == ERROR_NOT_LOGGED_IN
    }

    /** Whether [message] failed directly (never queued), so Retry is a fresh send. */
    fun isDirectFailure(message: BaseMessage): Boolean {
        val key = directKey(message) ?: return false
        return directFailures.containsKey(key)
    }

    /**
     * Registers [message] as a direct failure (W5: offline turned off mid-session, README §3.7), so
     * Retry is a fresh send and Delete removes it from memory — the outbox can no longer serve it.
     */
    fun markDirectFailure(message: BaseMessage) {
        directKey(message)?.let { directFailures[it] = Unit }
    }

    // ---- Retry / Delete (G4, BEH-SEND-20/36, D33a/D33b) -------------------------------------------

    /** Outcome of a Retry or Delete, applied by the message list. */
    internal sealed class ActionResult {
        /** The SDK accepted the action; for Retry, row transitions arrive through the hub. */
        object Accepted : ActionResult()
        /** The row must be removed (Delete succeeded, or the SDK no longer has the message). */
        object Removed : ActionResult()
        /** The message is in flight: show [OfflineNotice.SendingTryAgain]; the row is unchanged. */
        object InFlight : ActionResult()
        /** Any other error: the row is unchanged. */
        data class Failed(val error: CometChatException?) : ActionResult()
    }

    /**
     * Retry one unsent or failed row. Outbox → `retryMessage(localId)`; direct → a fresh send
     * through this helper (the new pending row replaces the old one by muid).
     */
    fun retry(message: BaseMessage, onResult: (ActionResult) -> Unit) {
        // A logout must clear the "Not sent yet" retry marker even when no send ran this session.
        watchSessionEnd()
        if (isDirectFailure(message)) {
            // Accepted only for a type a fresh send exists for (ENG-41551).
            if (!canResend(message)) {
                onResult(ActionResult.Failed(null))
                return
            }
            directFailures.remove(directKey(message))
            resend(message)
            onResult(ActionResult.Accepted)
            return
        }
        val localId = message.localMessageId
        if (localId.isNullOrEmpty()) {
            onResult(ActionResult.Failed(null))
            return
        }
        val gen = OfflineHub.gen
        // The row shows "Not sent yet" while the retry is in flight or queued (G3). The SDK resets
        // only its own live copy, so this is a kit-side overlay, cleared by the next real
        // transition (sent, or failed again) or by a rejected retry. Reporting SENDING here makes
        // a later FAILED a forward transition the hub publishes (FAILED → FAILED is not).
        OfflineRowState.markRetrying(localId)
        OfflineHub.reportSend(message, MessageSendState.SENDING, null, true)
        sdk.retry(localId, object : CometChat.MessageSentListener() {
            private var answered = false

            private fun answer(result: ActionResult) {
                if (answered) return
                answered = true
                onResult(result)
            }

            override fun onMessageSent(m: BaseMessage) {
                if (OfflineHub.gen != gen) return
                OfflineRowState.clearRetrying(localId)
                CometChatThreadSubscription.applyOwnMessageSent(m)
                OfflineHub.reportSend(m, MessageSendState.SENT, null, true)
                CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageSent(m, KitSendStatus.SUCCESS))
                answer(ActionResult.Accepted)
            }

            override fun onError(e: CometChatException?) {
                if (OfflineHub.gen != gen) return
                when (e?.code) {
                    // Not connected: the retried message is PENDING again and sends on reconnect
                    // (BEH-SEND-10/20). The overlay keeps the row on "Not sent yet".
                    ERROR_NOT_CONNECTED -> answer(ActionResult.Accepted)
                    ERROR_IN_FLIGHT -> {
                        OfflineRowState.clearRetrying(localId)
                        answer(ActionResult.InFlight)
                    }
                    ERROR_NOT_FOUND -> {
                        OfflineRowState.clearRetrying(localId)
                        answer(ActionResult.Removed)
                    }
                    // A final failure also reaches the row through the outbox listener and the hub.
                    else -> {
                        OfflineRowState.clearRetrying(localId)
                        answer(ActionResult.Failed(e))
                    }
                }
            }
        })
    }

    /**
     * Delete one unsent or failed row. Outbox or pending → `discardLocalMessage(localId)`, and the
     * row is removed on success (no SDK event fires). Direct → memory only.
     *
     * When the message is a thread reply and it is removed from the device (discard succeeded, or
     * a direct failure dropped from memory), every main list takes back the reply count it added
     * (BEH-SEND-36) through the internal [OfflineHub.reportThreadReplyDeleted]. A NOT_FOUND answer
     * changed nothing on the device (the reply may have been sent), so it does not.
     */
    fun delete(message: BaseMessage, onResult: (ActionResult) -> Unit) {
        val localId = message.localMessageId
        if (isDirectFailure(message) || localId.isNullOrEmpty()) {
            directFailures.remove(directKey(message))
            // Never in the outbox, so the SDK's stored reply count never had it (discarded = false).
            OfflineHub.reportThreadReplyDeleted(message, discarded = false)
            onResult(ActionResult.Removed)
            return
        }
        val gen = OfflineHub.gen
        sdk.discard(localId, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(result: String?) {
                if (OfflineHub.gen != gen) return
                OfflineHub.reportThreadReplyDeleted(message)
                onResult(ActionResult.Removed)
            }

            override fun onError(e: CometChatException?) {
                if (OfflineHub.gen != gen) return
                onResult(
                    when (e?.code) {
                        ERROR_IN_FLIGHT -> ActionResult.InFlight
                        ERROR_NOT_FOUND -> ActionResult.Removed
                        else -> ActionResult.Failed(e)
                    }
                )
            }
        })
    }

    // ------------------------------------------------------------------------------------------

    /** The fields today's path stamps before the SDK call, unchanged. */
    private fun prepare(message: BaseMessage) {
        if (message.sender == null) message.sender = sdk.loggedInUser()
        if (message.muid.isNullOrEmpty()) message.muid = System.currentTimeMillis().toString()
        if (message.sentAt == 0L) message.sentAt = System.currentTimeMillis() / 1000
    }

    private fun start(call: SendCall<*>) {
        openCalls.add(call)
        watchSessionEnd()
    }

    /** The message types [resend] sends. */
    private fun canResend(message: BaseMessage): Boolean =
        message is TextMessage || message is MediaMessage || message is CustomMessage || message is InteractiveMessage

    /** A fresh send of a direct-failed message: a clone without the error marker. */
    private fun resend(message: BaseMessage) {
        val copy = message.clone()
        copy.metadata = withoutError(message.metadata)
        val core = com.cometchat.uikit.core.CometChatUIKit
        when (copy) {
            is TextMessage -> core.sendTextMessage(copy, null)
            is MediaMessage -> core.sendMediaMessage(copy, null)
            is CustomMessage -> core.sendCustomMessage(copy, null)
            is InteractiveMessage -> resendInteractive(copy)
        }
    }

    /**
     * A fresh send of an interactive message (ENG-41551), which has no `CometChatUIKit` send: on
     * the ON path through this helper's per-send listener, as a custom message is; with offline off
     * the way the OFF path sends a custom message today (the direct send, IN_PROGRESS / SUCCESS /
     * ERROR events, the metadata `error` marker on failure).
     */
    private fun resendInteractive(message: InteractiveMessage) {
        prepare(message)
        if (OfflineGate.isOn()) {
            val call = SendCall(message, null, OfflineHub.gen)
            start(call)
            sdk.sendInteractive(message, call.listener)
            return
        }
        CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageSent(message, KitSendStatus.IN_PROGRESS))
        sdk.sendInteractiveDirect(message, object : CometChat.CallbackListener<InteractiveMessage>() {
            override fun onSuccess(sent: InteractiveMessage) {
                CometChatThreadSubscription.applyOwnMessageSent(sent)
                CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageSent(sent, KitSendStatus.SUCCESS))
            }

            override fun onError(e: CometChatException?) {
                message.metadata = withError(message.metadata, e)
                CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageSent(message, KitSendStatus.ERROR))
            }
        })
    }

    /**
     * Rejects every open caller once when the session ends (logout or a user switch) or the
     * offline cache is reset, with `ERROR_OFFLINE_MESSAGE_DISCARDED`; [isQuietRejection] keeps the
     * kit's callers silent (G12).
     */
    private fun watchSessionEnd() {
        if (sessionWatchStarted) return
        synchronized(this) {
            if (sessionWatchStarted) return
            sessionWatchStarted = true
            watchScope.launch {
                OfflineHub.events.collect { event ->
                    when (event) {
                        is OfflineHubEvent.SessionEnded -> rejectOpenCalls(event.gen)
                        is OfflineHubEvent.CacheReset -> rejectOpenCallsOnReset(event.gen)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun rejectOpenCalls(newGen: Long) {
        OfflineRowState.clearAllRetrying()
        val stale = synchronized(openCalls) { openCalls.filter { it.gen < newGen }.also { openCalls.removeAll(it.toSet()) } }
        stale.forEach { it.rejectQuietly(SESSION_ENDED) }
    }

    /**
     * A cache reset (ENG-41551): every reset rebuilds the store without its unsent messages. A
     * readable reset has already failed each one through the SDK's per-call `onError` (DISCARDED),
     * which settled and removed it here; a wipe reports nothing per call, so whatever is still open
     * is failed now. Only a call whose pending was seen is failed: its outbox row was in the store
     * the reset dropped. A call with no pending yet is a direct send on the wire, which still
     * answers itself (one answer per call); a late DISCARDED never settles a failed call again.
     */
    private fun rejectOpenCallsOnReset(gen: Long) {
        val open = synchronized(openCalls) {
            openCalls.filter { it.gen <= gen && it.pendingSeen }.also { openCalls.removeAll(it.toSet()) }
        }
        open.forEach { it.rejectQuietly(CACHE_RESET) }
    }

    private const val SESSION_ENDED = "The session ended before the message was sent."
    private const val CACHE_RESET = "The offline cache was reset before the message was sent."

    private fun directKey(message: BaseMessage): String? =
        message.localMessageId?.takeIf { it.isNotEmpty() }?.let { "l:$it" }
            ?: message.muid?.takeIf { it.isNotEmpty() }?.let { "m:$it" }

    /** Today's error marker, merged into the existing metadata instead of replacing it. */
    private fun withError(metadata: JSONObject?, e: CometChatException?): JSONObject {
        val json = metadata?.let { runCatching { JSONObject(it.toString()) }.getOrNull() } ?: JSONObject()
        runCatching {
            json.put("error", e?.message ?: "Unknown error")
            json.put("errorCode", e?.code ?: "UNKNOWN")
            json.put("errorDetails", e?.details ?: "")
        }
        return json
    }

    private fun withoutError(metadata: JSONObject?): JSONObject? {
        if (metadata == null) return null
        val json = runCatching { JSONObject(metadata.toString()) }.getOrNull() ?: return metadata
        json.remove("error")
        json.remove("errorCode")
        json.remove("errorDetails")
        return json
    }

    /** One send: its caller, the gen it started in, and whether pending was seen. */
    private class SendCall<T : BaseMessage>(
        val message: T,
        val callback: CometChat.CallbackListener<T>?,
        val gen: Long
    ) {
        @Volatile var pendingSeen = false
        @Volatile var settled = false

        private fun settle(): Boolean = synchronized(this) {
            if (settled) false else { settled = true; true }
        }

        private fun stale() = OfflineHub.gen != gen

        fun rejectQuietly(reason: String) {
            if (!settle()) return
            callback?.onError(CometChatException(ERROR_DISCARDED, reason))
        }

        @Suppress("UNCHECKED_CAST")
        val listener = object : CometChat.MessageSentListener() {
            override fun onMessagePending(m: BaseMessage) {
                if (stale()) return
                val first = !pendingSeen
                pendingSeen = true
                OfflineHub.reportSend(m, MessageSendState.SENDING, null, true)
                if (first) {
                    CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageSent(m, KitSendStatus.IN_PROGRESS))
                }
            }

            override fun onMessageSent(m: BaseMessage) {
                if (stale()) return
                openCalls.remove(this@SendCall)
                CometChatThreadSubscription.applyOwnMessageSent(m)
                OfflineHub.reportSend(m, MessageSendState.SENT, null, true)
                CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageSent(m, KitSendStatus.SUCCESS))
                if (settle()) (m as? T ?: message).let { callback?.onSuccess(it) }
            }

            override fun onError(e: CometChatException?) {
                if (stale()) return
                if (pendingSeen && message.status != SdkMessageStatus.FAILED) {
                    // Queued, not failed (BEH-SEND-10): the message is durably PENDING and sends
                    // later. No error, no rejection, no transition.
                    return
                }
                openCalls.remove(this@SendCall)
                if (pendingSeen) {
                    // A final outbox failure: TRANSIENT/PERMANENT keep the row failed, DISCARDED and
                    // QUEUE_OVERFLOW remove it. The row state is derived from status, never marked.
                    OfflineHub.reportSend(message, MessageSendState.FAILED, message.failureReason, true)
                    if (message.failureReason != MessageFailureReason.DISCARDED) {
                        CometChatEvents.emitMessageEvent(
                            CometChatMessageEvent.MessageSent(message, KitSendStatus.ERROR)
                        )
                    }
                } else if (!isQuietRejection(e)) {
                    // A direct failure (BEH-SEND-33): no pending, so no outbox row. Shown failed in
                    // memory only, marked exactly as the OFF path marks it.
                    directKey(message)?.let { directFailures[it] = Unit }
                    message.metadata = withError(message.metadata, e)
                    OfflineHub.reportSend(message, MessageSendState.FAILED, message.failureReason, true)
                    CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageSent(message, KitSendStatus.ERROR))
                }
                if (settle()) callback?.onError(e ?: CometChatException("SEND_ERROR", "Send failed"))
            }
        }
    }

    @VisibleForTesting
    internal fun resetForTest() {
        OfflineRowState.clearAllRetrying()
        directFailures.clear()
        openCalls.clear()
        sdk = DefaultSdk
    }

    @VisibleForTesting
    internal fun openCallCountForTest(): Int = openCalls.size
}
