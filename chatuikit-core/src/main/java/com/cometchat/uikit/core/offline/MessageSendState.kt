package com.cometchat.uikit.core.offline

import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.MessageStatus

/**
 * The public, coarse send state of a message (G14, BEH-SEND-30).
 *
 * This is the payload of the additive public event
 * [com.cometchat.uikit.core.events.CometChatOfflineEvent.MessageSendStateChanged]. It is a **new**
 * public type; no existing public enum gains a value.
 *
 * Derived from the SDK [MessageStatus] on every render via [fromStatus] — never cached on the
 * message — so a FAILED row re-derives as FAILED after relaunch and a re-send flips it cleanly.
 */
public enum class MessageSendState {
    SENDING,
    SENT,
    FAILED;

    public companion object {
        /**
         * Maps an SDK [MessageStatus] to the coarse public state:
         * PENDING / SENDING → [SENDING]; SENT / DELIVERED / READ → [SENT]; FAILED → [FAILED].
         *
         * A null status is treated as [SENDING] (an optimistic row not yet stamped by the SDK).
         */
        @JvmStatic
        public fun fromStatus(status: MessageStatus?): MessageSendState = when (status) {
            MessageStatus.SENT, MessageStatus.DELIVERED, MessageStatus.READ -> SENT
            MessageStatus.FAILED -> FAILED
            else -> SENDING
        }

        /** Convenience: derive the state directly from a message's current [BaseMessage.getStatus]. */
        @JvmStatic
        public fun of(message: BaseMessage): MessageSendState = fromStatus(message.status)
    }
}

/**
 * How a failed send must be retried (internal to the kit).
 *
 * - [OUTBOX]: the send reached the durable outbox; Retry re-drives it with
 *   `CometChat.retryMessage(localMessageId, …)`.
 * - [DIRECT]: the send never entered the outbox (e.g. queue-full / byte-overflow refusal, or an
 *   offline-off direct error). The row is held failed **in memory only** and Retry is a fresh send
 *   through the same helper with a new `localMessageId`; it does not survive a restart.
 */
internal enum class FailureKind {
    OUTBOX,
    DIRECT
}
