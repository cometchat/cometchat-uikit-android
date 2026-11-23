package com.cometchat.uikit.core.offline

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation

/**
 * Row adapters and the not-connected test for the search screen's two-phase reads (W7).
 *
 * Search results are shown newest-first, so both specs are [OfflineListReconcile.Spec.descending].
 * A keyword search never reaches the reconcile: a keyword has no cache (BEH-SNAP-04), so it is
 * served by the server phase alone.
 */
internal object SearchOffline {

    /** Messages: row key per [MessageKey.rowKey], server id when acked, `(sentAt, id)` order. */
    val messageSpec: OfflineListReconcile.Spec<BaseMessage, Long> = OfflineListReconcile.Spec(
        keyOf = { MessageKey.rowKey(it) },
        serverIdOf = { m -> m.id.takeIf { it > 0 } },
        sortAtOf = { it.sentAt },
        updatedAtOf = { it.updatedAt },
        composeAtOf = { it.sentAt },
        descending = true
    )

    /** Conversations: keyed and identified by `conversationId`, ordered by `updatedAt`. */
    val conversationSpec: OfflineListReconcile.Spec<Conversation, String> = OfflineListReconcile.Spec(
        keyOf = { it.conversationId ?: "" },
        serverIdOf = { it.conversationId },
        sortAtOf = { it.updatedAt },
        updatedAtOf = { it.updatedAt },
        descending = true
    )

    /** True for the SDK's not-connected error (`ERROR_INTERNET_UNAVAILABLE`). */
    fun isNotConnected(e: CometChatException?): Boolean =
        e?.code == CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE

    /** Appends [rows] whose key is not already in [current], keeping order (no repeats). */
    fun <T> appendDistinct(current: List<T>, rows: List<T>, keyOf: (T) -> Any): List<T> {
        if (rows.isEmpty()) return current
        val seen = current.mapTo(HashSet(), keyOf)
        val added = rows.filter { seen.add(keyOf(it)) }
        return if (added.isEmpty()) current else current + added
    }
}
