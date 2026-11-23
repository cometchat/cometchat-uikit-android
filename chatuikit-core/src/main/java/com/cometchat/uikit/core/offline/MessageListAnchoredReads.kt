package com.cometchat.uikit.core.offline

import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import kotlinx.coroutines.flow.Flow

/**
 * Cache-allowed reads for opening a chat at a message (D60): start-from-unread and open-at-message
 * jump to the target from the cache when the device is offline (the not-connected error from the
 * server read). Used only by the open path of [MessageListOfflineReads]; every other read keeps its
 * server-only settlement (ENG-41135).
 *
 * Offered by the default repository only. A custom repository has none, so today's server jump runs.
 */
internal interface MessageListAnchoredReads {
    /** The conversation, settled once: the server value, else (not-connected only) the cached copy (BEH-READ-32). */
    suspend fun getConversation(id: String, type: String): Result<SettledRead<Conversation>>

    /** The jump target, settled once: the server value, else (not-connected only) the cached copy (BEH-READ-35). */
    suspend fun getMessage(messageId: Long): Result<BaseMessage>

    /** Two-phase page immediately older than [messageId], anchored on it (BEH-READ-29); target excluded. */
    fun fetchPreviousAnchored(messageId: Long): Flow<TwoPhaseResult<BaseMessage>>

    /** Two-phase page immediately newer than [messageId] (BEH-READ-29); queued rows last (BEH-READ-31). */
    fun fetchNextAnchored(messageId: Long): Flow<TwoPhaseResult<BaseMessage>>
}
