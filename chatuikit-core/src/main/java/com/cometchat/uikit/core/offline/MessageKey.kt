package com.cometchat.uikit.core.offline

import com.cometchat.chat.models.BaseMessage

/**
 * Stable row and render identity for messages on the offline (ON) path (G6, R141).
 *
 * Two distinct keys, because they answer two questions:
 * - [rowKey] is the **list identity** used to upsert/dedupe a row in the store and the adapter.
 *   An acked message is keyed by its server id; an un-acked one by its local id (falling back to
 *   its muid).
 * - [renderKey] is the **diff key** a UI list uses so an own message does not flicker when its
 *   optimistic row is replaced in place by the acked copy: for own messages it stays on the local
 *   id (stable across the ack), for others it is the server id.
 *
 * [findMatch] implements the G6 lookup: server id first; then `localMessageId` **only** against
 * rows that have no server id, the same sender and the same conversation; then own muid. It is
 * null-safe by construction — a null/blank id never matches, so two un-acked rows with nothing in
 * common never collide. The [AliasMap] records `localMessageId → serverId` on ack so a later
 * server copy can be resolved back to the optimistic row.
 */
internal object MessageKey {

    /** List identity. Server id when acked, else local id, else muid. */
    fun rowKey(m: BaseMessage): String =
        if (m.id > 0) "s:${m.id}" else "l:" + (m.localMessageId ?: "m:${m.muid}")

    /**
     * Diff/render identity. For an own message, the local id (stable across its ack) so the
     * optimistic row is replaced in place; for anyone else, the server id.
     */
    fun renderKey(m: BaseMessage, myUid: String?): String =
        if (m.sender?.uid != null && m.sender?.uid == myUid) {
            m.localMessageId ?: m.muid?.let { "m:$it" } ?: "s:${m.id}"
        } else {
            "s:${m.id}"
        }

    /**
     * Finds the existing row that [incoming] should replace, per G6. Returns null when none
     * matches (so [incoming] is a fresh insert).
     *
     * Order:
     * 1. same server id;
     * 2. same `localMessageId` **and** no server id on the candidate **and** same sender **and**
     *    same conversation;
     * 3. same muid on a candidate with no server id.
     */
    fun findMatch(rows: List<BaseMessage>, incoming: BaseMessage): BaseMessage? {
        if (incoming.id > 0) {
            rows.firstOrNull { it.id > 0 && it.id == incoming.id }?.let { return it }
        }
        val localId = incoming.localMessageId
        if (!localId.isNullOrEmpty()) {
            rows.firstOrNull {
                it.id <= 0 &&
                    it.localMessageId == localId &&
                    sameSender(it, incoming) &&
                    sameConversation(it, incoming)
            }?.let { return it }
        }
        val muid = incoming.muid
        if (!muid.isNullOrEmpty()) {
            rows.firstOrNull { it.id <= 0 && it.muid == muid }?.let { return it }
        }
        return null
    }

    private fun sameSender(a: BaseMessage, b: BaseMessage): Boolean {
        val ua = a.sender?.uid
        val ub = b.sender?.uid
        return ua != null && ua == ub
    }

    private fun sameConversation(a: BaseMessage, b: BaseMessage): Boolean {
        val ca = a.conversationId
        val cb = b.conversationId
        // When the SDK has not yet stamped a conversationId on an optimistic row, fall back to the
        // receiver pair so G6 can still scope the match to one conversation.
        if (ca != null && cb != null) return ca == cb
        return a.receiverUid == b.receiverUid && a.receiverType == b.receiverType
    }
}

/**
 * Records `localMessageId → serverId` as sends are acked, so a server copy arriving later (sweep,
 * sync, a fetch) can be resolved to the optimistic row even after its own callback replaced it.
 * Per-conversation instances live on the list ViewModels and are cleared on session change.
 */
internal class AliasMap {
    private val localToServer = HashMap<String, Long>()

    /** Record an ack. No-ops on a blank id or a non-positive server id. */
    fun onAck(localMessageId: String?, serverId: Long) {
        if (!localMessageId.isNullOrEmpty() && serverId > 0) {
            localToServer[localMessageId] = serverId
        }
    }

    /** The server id recorded for [localMessageId], or null if never acked. */
    fun serverIdFor(localMessageId: String?): Long? =
        if (localMessageId.isNullOrEmpty()) null else localToServer[localMessageId]

    fun clear() = localToServer.clear()
}
