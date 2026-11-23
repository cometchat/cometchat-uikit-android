package com.cometchat.uikit.core.events

import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.MessageFailureReason
import com.cometchat.chat.models.OfflineCacheResetReason
import com.cometchat.uikit.core.offline.MessageSendState

/**
 * The additive public events the offline feature adds for integrators (G14). Delivered on
 * [CometChatEvents.offlineEvents], a **new** lossless flow; all existing flows are unchanged.
 *
 * This is **not** a subclass of [CometChatMessageEvent] — the existing message-event contract is
 * untouched. These are never emitted while offline is off.
 */
public sealed class CometChatOfflineEvent {

    /**
     * A message's coarse send state changed. Fired exactly once per transition per message on the
     * offline path.
     *
     * @property message the message whose state changed.
     * @property state the new coarse state.
     * @property reason the failure reason when [state] is [MessageSendState.FAILED], else null.
     */
    public data class MessageSendStateChanged(
        public val message: BaseMessage,
        public val state: MessageSendState,
        public val reason: MessageFailureReason?
    ) : CometChatOfflineEvent()

    /**
     * The local offline cache was reset.
     *
     * @property reason why the cache was reset.
     * @property pendingMessagesLost how many queued (unsent) messages were lost, or
     * [com.cometchat.chat.core.OfflineCacheResetListener.PENDING_COUNT_UNKNOWN] when not known.
     */
    public data class OfflineCacheReset(
        public val reason: OfflineCacheResetReason,
        public val pendingMessagesLost: Int
    ) : CometChatOfflineEvent()
}
