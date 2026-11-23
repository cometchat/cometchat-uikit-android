package com.cometchat.uikit.core.offline

import androidx.annotation.RestrictTo
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.MessageStatus
import com.cometchat.uikit.core.domain.model.StreamMessage

/**
 * How the UI modules draw a send row on the offline (ON) path (G3, G5; BEH-SEND-30/31/32). Owned
 * by **W4** (ENG-41089). Public so `chatuikit-kotlin` / `chatuikit-compose` can read it, but
 * [RestrictTo] `LIBRARY_GROUP` — internal to the kit.
 *
 * Pass the mode the list captured once with [OfflineUi.keyMode]. With `offlineOn == false` every
 * function returns "not an unsent row", so the OFF path keeps today's rows exactly.
 *
 * An **unsent** row is the logged-in user's own message with no server id. Its state is derived
 * on every render — never cached — so the first render after a relaunch is already correct:
 * - **failed** when the SDK status is FAILED (an outbox failure), or when the row carries today's
 *   `error` metadata (a direct failure that never reached the outbox, BEH-SEND-33);
 * - otherwise **not sent yet**.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object OfflineRowState {

    /** The caption under an unsent row. */
    public enum class Caption {
        /** Not an unsent row: no caption. */
        NONE,
        /** "Not sent yet" (`cometchat_message_not_sent_yet`). */
        NOT_SENT_YET,
        /** "Not sent. Tap to retry" (`cometchat_message_not_sent_tap_to_retry`). */
        NOT_SENT_TAP_TO_RETRY
    }

    /** True for the logged-in user's own row with no server id, on the ON path only. */
    @JvmStatic
    public fun isUnsent(message: BaseMessage, offlineOn: Boolean): Boolean {
        if (!offlineOn || message.id > 0L || message is StreamMessage) return false
        if (message.deletedAt > 0L) return false
        val me = runCatching { CometChat.getLoggedInUser()?.uid }.getOrNull() ?: return false
        return message.sender?.uid == me
    }

    /**
     * True when the row's send failed: status FAILED, or today's `error` metadata — unless a Retry
     * of that `localMessageId` is in flight or queued. The SDK resets only its own live copy of a
     * retried message, so the row's copy still says FAILED; this kit-side overlay shows it as
     * "Not sent yet" (G3) until the next real transition (sent or failed again) clears it.
     */
    @JvmStatic
    public fun isFailed(message: BaseMessage): Boolean {
        val localId = message.localMessageId
        if (!localId.isNullOrEmpty() && localId in retrying) return false
        return message.status == MessageStatus.FAILED || hasErrorMetadata(message)
    }

    /** localMessageIds with a Retry in flight or queued (kit-side only; never SDK state). */
    private val retrying: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    internal fun markRetrying(localMessageId: String) {
        retrying += localMessageId
    }

    /** @return true when [localMessageId] was marked. */
    internal fun clearRetrying(localMessageId: String?): Boolean =
        !localMessageId.isNullOrEmpty() && retrying.remove(localMessageId)

    internal fun clearAllRetrying() = retrying.clear()

    /** The caption for [message]; [Caption.NONE] unless it is an unsent row with offline on. */
    @JvmStatic
    public fun caption(message: BaseMessage, offlineOn: Boolean): Caption = when {
        !isUnsent(message, offlineOn) -> Caption.NONE
        isFailed(message) -> Caption.NOT_SENT_TAP_TO_RETRY
        else -> Caption.NOT_SENT_YET
    }

    private fun hasErrorMetadata(message: BaseMessage): Boolean =
        runCatching { message.metadata?.optString("error").orEmpty().isNotEmpty() }.getOrDefault(false)
}
