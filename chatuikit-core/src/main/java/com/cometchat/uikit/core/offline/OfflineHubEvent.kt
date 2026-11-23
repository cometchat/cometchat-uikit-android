package com.cometchat.uikit.core.offline

import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.MessageFailureReason
import com.cometchat.chat.models.OfflineCacheResetReason
import com.cometchat.chat.models.SyncOutcome

/**
 * The internal fan-out events [OfflineHub] publishes on its private, lossless flow. Each is stamped
 * with the session [gen] it was produced in, so a list drops any event from an older generation
 * (G8). These are **not** the public [com.cometchat.uikit.core.events.CometChatOfflineEvent] — they
 * carry more (sync phases, media, session end) and never leave the kit.
 */
internal sealed class OfflineHubEvent {
    abstract val gen: Long

    /** A send transitioned. Exactly one per transition per [localMessageId] (ON path, forward-only). */
    data class SendState(
        override val gen: Long,
        val localMessageId: String,
        val message: BaseMessage,
        val state: MessageSendState,
        val reason: MessageFailureReason?
    ) : OfflineHubEvent()

    /**
     * The kit's own Delete removed an unsent thread reply on the device (BEH-SEND-36). The SDK fires
     * no event for a discard, so this tells every live main list to take back the reply count it
     * added for [localMessageId]. Internal only: no public event, no notice. [discarded] is true when
     * the SDK discarded the reply from its outbox (and took it out of the parent's stored count), false
     * for a direct failure that never entered the outbox (the stored count never had it).
     */
    data class ThreadReplyDeleted(
        override val gen: Long,
        val localMessageId: String,
        val message: BaseMessage,
        val discarded: Boolean = true
    ) : OfflineHubEvent()

    /** The reconnect sweep finished the conversation phase. */
    data class ConversationSyncCompleted(
        override val gen: Long,
        val conversations: List<Conversation>
    ) : OfflineHubEvent()

    data class ConversationSyncFailed(
        override val gen: Long,
        val error: CometChatException
    ) : OfflineHubEvent()

    /** The reconnect sweep finished the message phase; [outcome] says CAUGHT_UP or PARTIAL. */
    data class MessageSyncCompleted(
        override val gen: Long,
        val messages: List<BaseMessage>,
        val outcome: SyncOutcome
    ) : OfflineHubEvent()

    data class MessageSyncFailed(
        override val gen: Long,
        val error: CometChatException
    ) : OfflineHubEvent()

    /** `onMessagesSynced`: update rows already shown, never insert (BEH-INV-11). */
    data class MessagesUpdated(
        override val gen: Long,
        val messages: List<BaseMessage>
    ) : OfflineHubEvent()

    data class CacheReset(
        override val gen: Long,
        val reason: OfflineCacheResetReason,
        val pendingMessagesLost: Int
    ) : OfflineHubEvent()

    data class MediaReady(
        override val gen: Long,
        val messageLocalId: String?,
        val attachmentId: String,
        val localPath: String
    ) : OfflineHubEvent()

    /** Logout or a switch to a different uid. The lists clear before the next frame (G8). */
    data class SessionEnded(
        override val gen: Long
    ) : OfflineHubEvent()
}
