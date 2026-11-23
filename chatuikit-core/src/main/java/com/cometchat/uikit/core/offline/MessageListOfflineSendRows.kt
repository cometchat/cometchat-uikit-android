package com.cometchat.uikit.core.offline

import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.MessageFailureReason
import com.cometchat.chat.models.MessageReceipt
import com.cometchat.chat.models.MessageStatus as SdkMessageStatus
import com.cometchat.chat.models.TextMessage
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import com.cometchat.uikit.core.events.MessageStatus as KitSendStatus

/**
 * Seam owned by **W4** (ENG-41089): the message list's send rows on the offline (ON) path —
 * the pending row, "Not sent yet", failed-from-status, Retry/Delete, held receipts and the thread
 * reply count (G3, G4, G5; BEH-SEND-22/30/31/32/36). Every entry point runs only behind the
 * ViewModel's captured `offlineOn`, so the OFF path is untouched.
 *
 * Row transitions (README §3.4 as corrected by G4):
 * - SENDING: insert the row, or replace the row [MessageKey.findMatch] finds (the optimistic row by
 *   muid, or the same local id).
 * - SENT: replace the matched row in place (same render key, W1 [OfflineUi]) and apply any held
 *   receipt, keeping any later tick the replaced row already showed. A row already DISAPPROVED by
 *   a moderation verdict is kept (W0, ENG-39226).
 * - FAILED, TRANSIENT/PERMANENT/QUEUE_FULL/BYTE_OVERFLOW/direct: the row shows failed (status or
 *   today's error metadata, see [OfflineRowState]); QUEUE_FULL and BYTE_OVERFLOW raise their notice.
 * - FAILED, DISCARDED/QUEUE_OVERFLOW: the row is removed with an `UnsentRemoved(text)` notice.
 */
internal object MessageListOfflineSendRows {

    /** At most this many receipts are held per list (BEH-SEND-32). */
    const val HELD_RECEIPT_CAP: Int = 200

    /** Per-list state. Cleared on a session end ([State.clear], from the ViewModel). */
    internal class State {
        /**
         * Receipts for server ids this list does not show yet, oldest evicted first. A sync does
         * not clear them: "X and everything before" stays true, and the ack they wait for may land
         * after the sweep. Only the cap and a session end drop them.
         */
        val heldReceipts: LinkedHashMap<Long, HeldReceipt> =
            object : LinkedHashMap<Long, HeldReceipt>(16, 0.75f, false) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, HeldReceipt>?): Boolean =
                    size > HELD_RECEIPT_CAP
            }

        /** Own thread replies whose send events this list has seen, by localMessageId (D-321). */
        val seenReplies: HashSet<String> = HashSet()

        /** Own thread replies already taken back after a device delete (−1 at most once each). */
        val removedReplies: HashSet<String> = HashSet()

        /** Row keys with a Retry or Delete in flight; further taps are ignored. */
        val actionsInFlight: HashSet<String> = HashSet()

        /** muids of direct-failed rows being re-sent: the fresh send's row replaces them. */
        val replaceByMuid: HashSet<String> = HashSet()

        fun clear() {
            heldReceipts.clear()
            seenReplies.clear()
            removedReplies.clear()
            actionsInFlight.clear()
            replaceByMuid.clear()
        }
    }

    internal data class HeldReceipt(var deliveredAt: Long = 0L, var readAt: Long = 0L)

    // ---- hub events --------------------------------------------------------------------------

    /** Apply one send transition to the rows this list shows. */
    fun onHubEvent(viewModel: CometChatMessageListViewModel, event: OfflineHubEvent.SendState) {
        val message = event.message
        if (viewModel.isOfflineThreadReplyForMainList(message)) {
            onThreadReply(viewModel, event)
            return
        }
        if (!viewModel.acceptsOfflineSendRow(message)) return
        val match = findRow(viewModel.offlineSendRowsState, viewModel.getItems(), message)
        if (event.state != MessageSendState.SENDING) {
            // A final transition settles any Retry/Delete tap that never got its own answer, and
            // ends the kit-side "Retry queued" overlay ([OfflineRowState.isFailed]).
            viewModel.offlineSendRowsState.actionsInFlight.remove(MessageKey.rowKey(message))
            match?.let { viewModel.offlineSendRowsState.actionsInFlight.remove(MessageKey.rowKey(it)) }
            OfflineRowState.clearRetrying(message.localMessageId)
        }
        when (event.state) {
            MessageSendState.SENDING -> upsert(viewModel, match, message)
            MessageSendState.SENT -> {
                if (match == null) return
                if (isDisapproved(match)) return
                val acked = keepShownTicks(match, applyHeldReceipts(viewModel.offlineSendRowsState, message))
                viewModel.updateItem(acked) { it === match }
                viewModel.noteOfflineAck(message.localMessageId, message.id)
            }
            MessageSendState.FAILED -> when (event.reason) {
                MessageFailureReason.DISCARDED, MessageFailureReason.QUEUE_OVERFLOW -> {
                    if (match != null) {
                        removeRow(viewModel, match)
                        viewModel.raiseOfflineNotice(OfflineNotice.UnsentRemoved(textOf(match)))
                    }
                }
                else -> {
                    upsert(viewModel, match, message)
                    when (event.reason) {
                        MessageFailureReason.QUEUE_FULL -> viewModel.raiseOfflineNotice(OfflineNotice.QueueFull)
                        MessageFailureReason.BYTE_OVERFLOW ->
                            viewModel.raiseOfflineNotice(OfflineNotice.FileTooLargeOffline)
                        else -> Unit
                    }
                }
            }
        }
    }

    /**
     * The legacy `MessageSent` events on the ON path. Returns true when handled here, so the
     * ViewModel skips today's handling: IN_PROGRESS and ERROR (rows come from the hub, and today's
     * id-0 match could hit another unsent row), and the thread-reply count. SUCCESS for a main-list
     * row returns false, so today's ack handling runs (latest id, agent chat, W0 moderation skip).
     */
    fun onLegacySentEvent(
        viewModel: CometChatMessageListViewModel,
        event: CometChatMessageEvent.MessageSent
    ): Boolean {
        // Offline turned off since this list bound: sendX took today's path and nothing reaches the
        // hub, so today's legacy handling must run (W5 integration, README §3.7).
        if (!OfflineGate.isOn()) return false
        val message = event.message
        if (viewModel.isOfflineThreadReplyForMainList(message)) {
            val localId = message.localMessageId
            if (localId.isNullOrEmpty()) return false
            // The SDK counted the reply in the parent's stored count when it was queued; the ack adds
            // nothing (D-321).
            viewModel.offlineSendRowsState.seenReplies.add(localId)
            return true
        }
        if (!viewModel.acceptsOfflineSendRow(message)) return false
        return when (event.status) {
            KitSendStatus.IN_PROGRESS -> {
                viewModel.clearSmartReplies()
                true
            }
            KitSendStatus.ERROR -> true
            else -> false
        }
    }

    // ---- receipts (BEH-SEND-32) --------------------------------------------------------------

    /**
     * Stamps a receipt for "X and everything before" on rows **with a server id** only. A receipt
     * for an X this list does not show yet (newer than every shown server id) is held and applied
     * when that row's ack lands.
     */
    fun applyReceipt(viewModel: CometChatMessageListViewModel, receipt: MessageReceipt, delivered: Boolean) {
        val x = receipt.messageId
        val ts = if (delivered) receipt.deliveredAt else receipt.readAt
        if (x <= 0L || ts <= 0L) return
        val rows = viewModel.getItems()
        var changed = false
        val stamped = rows.map { row ->
            if (row.id in 1..x && (if (delivered) row.deliveredAt else row.readAt) == 0L) {
                changed = true
                row.clone().apply { if (delivered) deliveredAt = ts else readAt = ts }
            } else {
                row
            }
        }
        if (changed) viewModel.replaceOfflineSendRows(stamped)
        val maxShown = rows.maxOfOrNull { it.id } ?: 0L
        if (rows.none { it.id == x } && x > maxShown) {
            val held = viewModel.offlineSendRowsState.heldReceipts.getOrPut(x) { HeldReceipt() }
            if (delivered) held.deliveredAt = maxOf(held.deliveredAt, ts) else held.readAt = maxOf(held.readAt, ts)
        }
    }

    /** On ack of server id S: every held receipt for S or later covers S. */
    private fun applyHeldReceipts(state: State, acked: BaseMessage): BaseMessage {
        val s = acked.id
        if (s <= 0L || state.heldReceipts.isEmpty()) return acked
        var delivered = 0L
        var read = 0L
        state.heldReceipts.forEach { (x, held) ->
            if (x >= s) {
                delivered = maxOf(delivered, held.deliveredAt)
                read = maxOf(read, held.readAt)
            }
        }
        state.heldReceipts.remove(s)
        val needDelivered = delivered > 0L && acked.deliveredAt == 0L
        val needRead = read > 0L && acked.readAt == 0L
        if (!needDelivered && !needRead) return acked
        return acked.clone().apply {
            if (needDelivered) deliveredAt = delivered
            if (needRead) readAt = read
        }
    }

    /**
     * An ack replacing a row that already has a server id (a copy the sweep or a read placed first)
     * keeps the later delivered/read time of the two, so the ack never wipes ticks that row shows.
     * A row with no server id has no ticks (BEH-SEND-32), so it contributes nothing.
     */
    private fun keepShownTicks(shown: BaseMessage, acked: BaseMessage): BaseMessage {
        if (shown === acked || shown.id <= 0L) return acked
        val delivered = maxOf(shown.deliveredAt, acked.deliveredAt)
        val read = maxOf(shown.readAt, acked.readAt)
        if (delivered == acked.deliveredAt && read == acked.readAt) return acked
        return acked.clone().apply {
            deliveredAt = delivered
            readAt = read
        }
    }

    // ---- Retry / Delete (G4, D33a/D33b) ------------------------------------------------------

    /** Retry an unsent or failed row. Ignored for any other row and while one is in flight. */
    fun retry(viewModel: CometChatMessageListViewModel, message: BaseMessage) {
        if (!OfflineRowState.isUnsent(message, viewModel.offlineOn)) return
        val key = MessageKey.rowKey(message)
        val state = viewModel.offlineSendRowsState
        if (!state.actionsInFlight.add(key)) return
        if (OfflineSend.isDirectFailure(message)) {
            message.muid?.takeIf { it.isNotEmpty() }?.let { state.replaceByMuid += it }
        }
        OfflineSend.retry(message) { result ->
            state.actionsInFlight.remove(key)
            applyResult(viewModel, message, result)
            // A rejected retry ends the overlay: the row looks failed again.
            viewModel.notifyOfflineRowChanged(message)
        }
        // The row now reads "Not sent yet" through the overlay, though its object is unchanged.
        viewModel.notifyOfflineRowChanged(message)
    }

    /**
     * Delete an unsent or failed row on the device. Returns false for a row with a server id, so
     * the caller runs today's server delete (G10).
     */
    fun delete(viewModel: CometChatMessageListViewModel, message: BaseMessage): Boolean {
        if (!OfflineRowState.isUnsent(message, viewModel.offlineOn)) return false
        val key = MessageKey.rowKey(message)
        val state = viewModel.offlineSendRowsState
        if (!state.actionsInFlight.add(key)) return true
        OfflineSend.delete(message) { result ->
            state.actionsInFlight.remove(key)
            applyResult(viewModel, message, result)
        }
        return true
    }

    private fun applyResult(
        viewModel: CometChatMessageListViewModel,
        message: BaseMessage,
        result: OfflineSend.ActionResult
    ) {
        when (result) {
            OfflineSend.ActionResult.Removed -> {
                val row = findRow(viewModel.offlineSendRowsState, viewModel.getItems(), message)
                    ?: viewModel.getItems().firstOrNull { it === message }
                if (row != null) removeRow(viewModel, row)
            }
            OfflineSend.ActionResult.InFlight -> viewModel.raiseOfflineNotice(OfflineNotice.SendingTryAgain)
            OfflineSend.ActionResult.Accepted, is OfflineSend.ActionResult.Failed -> Unit
        }
    }

    // ---- thread reply count (main list) ------------------------------------------------------

    /*
     * The parent row shows its stored replyCount, and the SDK keeps the user's own unsent replies in
     * that stored count (BEH-MUT-10): +1 when a reply enters the outbox, −1 when the user deletes it
     * from the device, nothing when it sends, is evicted or is purged. A row read after the reply was
     * queued already includes it. So the list applies only the deltas the SDK applies after the row
     * was loaded — the same rule as the thread header (`CometChatThreadHeaderViewModel`), so the two
     * always agree (D-321):
     * - +1 when a reply is first seen as queued (SENDING with an unfailed message; a Retry reports
     *   SENDING with the row's FAILED copy and the SDK adds nothing for it);
     * - −1 once on the kit's Delete of a queued reply ([onThreadReplyDeleted], `discarded`), whether
     *   or not this list added it;
     * - nothing on SENT, QUEUE_OVERFLOW, DISCARDED or another failure.
     */
    private fun onThreadReply(viewModel: CometChatMessageListViewModel, event: OfflineHubEvent.SendState) {
        val localId = event.localMessageId.ifEmpty { event.message.localMessageId } ?: return
        if (localId.isEmpty()) return
        val firstSighting = viewModel.offlineSendRowsState.seenReplies.add(localId)
        if (firstSighting && event.state == MessageSendState.SENDING &&
            event.message.status != SdkMessageStatus.FAILED
        ) {
            bumpReplyCount(viewModel, event.message.parentMessageId, +1)
        }
    }

    /**
     * The kit's Delete removed an unsent thread reply on the device (BEH-SEND-36; the SDK fires no
     * event for it). When the SDK discarded it from its outbox it also took it out of the parent's
     * stored count, so the list takes one back, once. A direct failure was never counted by the SDK.
     */
    fun onThreadReplyDeleted(viewModel: CometChatMessageListViewModel, event: OfflineHubEvent.ThreadReplyDeleted) {
        if (!event.discarded) return
        if (!viewModel.isOfflineThreadReplyForMainList(event.message)) return
        val state = viewModel.offlineSendRowsState
        if (event.localMessageId.isEmpty() || !state.removedReplies.add(event.localMessageId)) return
        state.seenReplies.add(event.localMessageId)
        bumpReplyCount(viewModel, event.message.parentMessageId, -1)
    }

    private fun bumpReplyCount(viewModel: CometChatMessageListViewModel, parentId: Long, delta: Int) {
        val parent = viewModel.getItems().firstOrNull { it.id == parentId } ?: return
        val updated = parent.clone().apply { replyCount = maxOf(0, parent.replyCount + delta) }
        viewModel.updateItem(updated) { it === parent }
    }

    // ------------------------------------------------------------------------------------------

    /**
     * The row [message] updates (G6 via [MessageKey.findMatch]): server id, then `localMessageId`
     * on a row with no server id, same sender and conversation. A muid matches only a row that has
     * no `localMessageId` yet, or a direct-failed row being re-sent, so every file of a batch keeps
     * its own row even when the files share a muid.
     */
    private fun findRow(state: State, rows: List<BaseMessage>, message: BaseMessage): BaseMessage? {
        val localId = message.localMessageId
        val keyed = rows.filter { it.id > 0L || (!localId.isNullOrEmpty() && it.localMessageId == localId) }
        MessageKey.findMatch(keyed, message)?.let { return it }
        val muid = message.muid
        if (muid.isNullOrEmpty()) return null
        val byMuid = rows.firstOrNull {
            it.id <= 0L && it.muid == muid && (it.localMessageId.isNullOrEmpty() || muid in state.replaceByMuid)
        } ?: return null
        if (!byMuid.localMessageId.isNullOrEmpty()) state.replaceByMuid.remove(muid)
        return byMuid
    }

    private fun upsert(viewModel: CometChatMessageListViewModel, match: BaseMessage?, message: BaseMessage) {
        if (match == null) {
            viewModel.addItem(message)
        } else {
            viewModel.updateItem(message) { it === match }
            // The SDK hands back the same live instance; the list stays equal, so rebind the row.
            if (match === message) viewModel.notifyOfflineRowChanged(message)
        }
    }

    private fun removeRow(viewModel: CometChatMessageListViewModel, row: BaseMessage) {
        val index = viewModel.getItems().indexOfFirst { it === row }
        if (index >= 0) viewModel.removeItemAt(index)
    }

    private fun textOf(message: BaseMessage): String? = when (message) {
        is TextMessage -> message.text
        is MediaMessage -> message.caption
        else -> null
    }

    private fun isDisapproved(message: BaseMessage): Boolean {
        val status = when (message) {
            is TextMessage -> message.moderationStatus?.name
            is MediaMessage -> message.moderationStatus?.name
            else -> null
        }
        return status.equals("DISAPPROVED", ignoreCase = true)
    }
}
