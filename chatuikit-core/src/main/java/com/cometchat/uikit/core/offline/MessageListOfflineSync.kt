package com.cometchat.uikit.core.offline

import androidx.annotation.VisibleForTesting
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Action
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.OfflineCacheResetReason
import com.cometchat.chat.models.SyncOutcome
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.utils.MediaSourceResolver
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Seam owned by **W5** (ENG-41090): reconnect, cache reset, media re-resolve and own pin/save echo
 * idempotence for the message list (G7, G8, G9, G11). Everything here runs on the **ON path only**
 * (the ViewModel calls in only when its captured `offlineOn` is true), except own pin/save, which
 * runs on both paths per T4 (SDK 5.1.1 has no own echo on any path).
 *
 * - Reconnect (G7, BEH-SYNC-07/13): no list fetch. The SDK sweep arrives as the hub's
 *   `MessageSyncCompleted`, whose rows are upserted in sorted place — the only path for `action` and
 *   `call` rows (BEH-SYNC-25/31). One newest-window refresh follows `PARTIAL` or a sync error.
 * - Swept received messages (`source == SYNC`, BEH-SYNC-12): sorted place, no sound, no +1, no
 *   per-message receipt. The read receipt is the list's usual at-bottom receipt for the newest row.
 * - `MessagesUpdated` (BEH-INV-11): updates rows shown, never inserts.
 * - Cache reset (G8, BEH-ENC-15/16): the list clears and reloads; the notice is posted once by the
 *   hub via [OfflineSessionNotices].
 * - Offline turned off mid-session: un-acked rows turn failed; one `MessagesNotSent(n)` notice per
 *   transition across all lists ([OfflineOffTransition]).
 * - Media (G9, BEH-MEDIA-13): `MediaReady` stamps the local path on the row so its bubble
 *   re-resolves; on reconnect shown media rows re-resolve too.
 * - Own pin/save (G11, BEH-FLAG-04, T4, both paths): the row updates from `onSuccess`; an echo is a
 *   set-state that does nothing when the row already has the event's state; a change to the
 *   opposite state always applies (another member, or this user on another device).
 */
internal object MessageListOfflineSync {

    /**
     * The newest-window refresh after `PARTIAL` or a sync error: W2's
     * [MessageListOfflineReads.refreshNewestWindow] (a no-op until W2 merges). Swappable in tests.
     */
    @VisibleForTesting
    internal var refreshNewestWindow: (CometChatMessageListViewModel) -> Unit =
        { MessageListOfflineReads.refreshNewestWindow(it) }

    /**
     * Hook invoked from the message list's connection listener `onConnected`.
     *
     * Offline still on → handled (G7): the "Connect to load more" edges clear and their page loads once, and a list
     * with no rows reads the newest page once (via [refreshNewestWindow]). Offline turned off since this list bound → the
     * un-acked rows turn failed once, and today's reconnect fetch runs (returns false).
     */
    fun onConnected(viewModel: CometChatMessageListViewModel): Boolean {
        if (OfflineGate.isOn()) {
            // G9: media shown while the socket was down re-resolves now (URL instead of placeholder).
            MediaSourceResolver.onConnected()
            OfflineOffTransition.onGateOn()
            // "Connect to load more" clears and its failed page loads once by itself (D84 rule 9).
            MessageListOfflineReads.onConnected(viewModel)
            // Ruled 2026-11-04: a list still empty (opened offline, nothing cached) reads the newest
            // page once per connection. A non-empty list makes no fetch and relies on the sweep.
            // D84 rule 8: this is also how the offline state reloads; its rows replace it.
            if (viewModel.offlineRows().isEmpty()) refreshNewestWindow(viewModel)
            viewModel.offlineRows().filterIsInstance<MediaMessage>().forEach { viewModel.offlineNotifyRowChanged(it) }
            return true
        }
        failUnackedAfterOfflineOff(viewModel)
        return false
    }

    /** Handle one current-gen hub event (the ViewModel has already dropped stale-gen events). */
    fun onHubEvent(viewModel: CometChatMessageListViewModel, event: OfflineHubEvent) {
        when (event) {
            is OfflineHubEvent.MessageSyncCompleted -> onSweep(viewModel, event)
            is OfflineHubEvent.MessageSyncFailed -> refreshNewestWindow(viewModel)
            is OfflineHubEvent.MessagesUpdated -> updateShown(viewModel, event.messages)
            is OfflineHubEvent.CacheReset -> viewModel.offlineReloadAfterCacheReset()
            is OfflineHubEvent.MediaReady -> onMediaReady(viewModel, event)
            else -> Unit
        }
    }

    /**
     * Received-message hook for a swept message (`source == SYNC`), called after the list's chat,
     * filter and thread checks. Places it in sorted order without sound, +1, scroll or receipt.
     * @return true (handled).
     */
    fun onSyncedMessageReceived(viewModel: CometChatMessageListViewModel, message: BaseMessage): Boolean {
        upsertSorted(viewModel, listOf(message))
        return true
    }

    // ---------------------------------------------------------------- sweep

    private fun onSweep(viewModel: CometChatMessageListViewModel, event: OfflineHubEvent.MessageSyncCompleted) {
        val mine = event.messages.filter { viewModel.offlineAcceptsRow(it) }
        if (mine.isNotEmpty()) upsertSorted(viewModel, mine)
        // Receipts are cumulative: one delivery receipt for the newest swept message from someone
        // else, never one per message. The read receipt stays the list's at-bottom receipt.
        val myUid = viewModel.offlineLoggedInUid()
        mine.asSequence()
            .filter { it.id > 0 && it !is Action && it !is Call && it.sender?.uid != myUid }
            .maxWithOrNull(compareBy<BaseMessage>({ it.sentAt }, { it.id }))
            ?.let { viewModel.markAsDelivered(it) }
        if (event.outcome == SyncOutcome.PARTIAL) refreshNewestWindow(viewModel)
    }

    /**
     * Upserts [incoming] into the shown rows in one emission. A row already shown (G6 match) is
     * replaced by a copy at least as new (LWW on `updatedAt`). A new row is inserted in
     * `(sentAt, id)` order, but only inside the loaded window: never older than the oldest shown
     * row while older history is unloaded, and never newer than the newest shown row while the
     * newest window is unloaded (that only raises "more new messages", as the live path does).
     */
    @VisibleForTesting
    internal fun upsertSorted(viewModel: CometChatMessageListViewModel, incoming: List<BaseMessage>) {
        val out = viewModel.offlineRows().toMutableList()
        var changed = false
        var newerHidden = false
        for (m in incoming) {
            val match = MessageKey.findMatch(out, m)
            if (match != null) {
                if (m.updatedAt >= match.updatedAt && m !== match) {
                    out[out.indexOf(match)] = viewModel.offlineReconcileServerCopy(match, m)
                    changed = true
                }
                continue
            }
            if (m.id <= 0) continue // never insert someone's un-acked row
            val acked = out.filter { it.id > 0 }
            val oldest = acked.firstOrNull()
            val newest = acked.lastOrNull()
            if (oldest != null && viewModel.hasMorePreviousMessages.value && isBefore(m, oldest)) continue
            if (newest != null && viewModel.hasMoreNewMessages.value && isBefore(newest, m)) {
                newerHidden = true
                continue
            }
            // First acked row that sorts after m; un-acked rows stay at the newest end.
            var index = out.indexOfFirst { it.id > 0 && isBefore(m, it) }
            if (index < 0) index = out.indexOfFirst { it.id <= 0 }.let { if (it < 0) out.size else it }
            out.add(index, m)
            changed = true
        }
        if (changed) viewModel.offlineSetRows(out)
        if (newerHidden) viewModel.offlineMarkMoreNewMessages()
    }

    private fun isBefore(a: BaseMessage, b: BaseMessage): Boolean =
        if (a.sentAt != b.sentAt) a.sentAt < b.sentAt else a.id < b.id

    /** `onMessagesSynced`: replace rows already shown with a newer copy; never insert (BEH-INV-11). */
    @VisibleForTesting
    internal fun updateShown(viewModel: CometChatMessageListViewModel, messages: List<BaseMessage>) {
        val out = viewModel.offlineRows().toMutableList()
        var changed = false
        for (m in messages) {
            val match = MessageKey.findMatch(out, m) ?: continue
            if (m === match || m.updatedAt < match.updatedAt) continue
            out[out.indexOf(match)] = viewModel.offlineReconcileServerCopy(match, m)
            changed = true
        }
        if (changed) viewModel.offlineSetRows(out)
    }

    // ---------------------------------------------------------------- offline off mid-session

    /**
     * Offline was turned off after this list bound (README §3.7). Every own row still un-acked
     * turns failed (direct: Retry is a fresh send), once, and one `MessagesNotSent(n)` notice is
     * posted. A send already reported sent has a server id and is never touched. No reload.
     */
    @VisibleForTesting
    internal fun failUnackedAfterOfflineOff(viewModel: CometChatMessageListViewModel) {
        if (!viewModel.offlineMarkOffSeen()) return
        val myUid = viewModel.offlineLoggedInUid()
        val failedKeys = HashSet<String>()
        val out = viewModel.offlineRows().map { m ->
            if (m.id > 0 || m.sender?.uid == null || m.sender?.uid != myUid || hasError(m)) return@map m
            failedKeys += MessageKey.rowKey(m)
            markFailed(m)
        }
        if (failedKeys.isEmpty()) return
        viewModel.offlineSetRows(out)
        OfflineOffTransition.report(failedKeys)
    }

    private fun hasError(m: BaseMessage): Boolean =
        m.metadata?.optString("error").isNullOrEmpty().not()

    /**
     * A failed copy marked exactly as W4 marks a direct failure: today's metadata `error` marker (the
     * failed receipt and "Not sent. Tap to retry"); the SDK status is never written.
     */
    private fun markFailed(m: BaseMessage): BaseMessage {
        val copy = m.clone() ?: m
        val meta = runCatching { JSONObject(m.metadata?.toString() ?: "{}") }.getOrDefault(JSONObject())
        runCatching {
            meta.put("error", "offline_disabled")
            meta.put("errorCode", CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE)
        }
        copy.metadata = meta
        // Direct failure: the outbox can no longer retry it, so Retry is a fresh send and Delete
        // removes it from memory (W4's OfflineSend).
        OfflineSend.markDirectFailure(copy)
        return copy
    }

    // ---------------------------------------------------------------- media

    /**
     * `MediaReady`: stamp [OfflineHubEvent.MediaReady.localPath] on the matching attachment of the
     * matching row (by the message's server or local id, and the attachment's URL without its
     * query, BEH-MEDIA-13) so its bubble re-resolves to the local file.
     */
    @VisibleForTesting
    internal fun onMediaReady(viewModel: CometChatMessageListViewModel, event: OfflineHubEvent.MediaReady) {
        val changedRows = mutableListOf<BaseMessage>()
        val out = viewModel.offlineRows().map { m ->
            val media = m as? MediaMessage ?: return@map m
            val msgId = event.messageLocalId
            if (msgId != null && msgId != media.id.toString() && msgId != media.localMessageId) return@map m
            val updated = withLocalPath(media, event.attachmentId, event.localPath) ?: return@map m
            changedRows += updated
            updated
        }
        if (changedRows.isEmpty()) return
        viewModel.offlineSetRows(out)
        changedRows.forEach { viewModel.offlineNotifyRowChanged(it) }
    }

    private fun withLocalPath(media: MediaMessage, attachmentKey: String, localPath: String): MediaMessage? {
        val single = media.attachment
        val list = runCatching { media.attachments }.getOrNull().orEmpty()
        val singleHit = single != null && MediaSourceResolver.attachmentKey(single.fileUrl) == attachmentKey &&
            single.localPath != localPath
        val listHit = list.any { MediaSourceResolver.attachmentKey(it.fileUrl) == attachmentKey && it.localPath != localPath }
        if (!singleHit && !listHit) return null
        val copy = (media.clone() as? MediaMessage) ?: media
        if (singleHit && single != null) copy.attachment = single.clone().apply { this.localPath = localPath }
        if (listHit) {
            copy.attachments = list.map {
                if (MediaSourceResolver.attachmentKey(it.fileUrl) == attachmentKey) it.clone().apply { this.localPath = localPath } else it
            }
        }
        return copy
    }

    // ---------------------------------------------------------------- own pin/save (G11)

    internal enum class PinSaveKind { PIN, SAVE }

    /**
     * An own pin/save call succeeded on the acting list (BEH-FLAG-04, T4, both paths): apply the
     * state to the row unless it already matches.
     */
    fun applyOwnPinSave(
        viewModel: CometChatMessageListViewModel,
        optionId: String,
        message: BaseMessage,
        result: BaseMessage?
    ) {
        val (kind, state) = when (optionId) {
            UIKitConstants.MessageOption.PIN -> PinSaveKind.PIN to true
            UIKitConstants.MessageOption.UNPIN -> PinSaveKind.PIN to false
            UIKitConstants.MessageOption.SAVE -> PinSaveKind.SAVE to true
            UIKitConstants.MessageOption.UNSAVE -> PinSaveKind.SAVE to false
            else -> return
        }
        if (rowState(viewModel, message.id, kind) == state) return
        val source = result?.takeIf { it.id == message.id } ?: message
        viewModel.offlineApplyPinSave(source, kind, state)
    }

    /**
     * A pin/save listener event (both paths). @return true when it must be ignored because the row
     * already has the event's state (e.g. the 5.1.0 echo of the action `onSuccess` just applied).
     * An event for the opposite state always applies — another member, or this user elsewhere.
     */
    fun isPinSaveEchoNoOp(
        viewModel: CometChatMessageListViewModel,
        echo: BaseMessage,
        kind: PinSaveKind,
        state: Boolean
    ): Boolean {
        val loaded = viewModel.offlineRows().firstOrNull { it.id == echo.id } ?: return false
        val current = if (kind == PinSaveKind.PIN) loaded.isPinned else loaded.isSaved
        if (current != state) return false
        // A pin by someone else onto a pinned row changes who pinned it — not a no-op.
        if (kind == PinSaveKind.PIN && state && echo.pinnedBy != null && echo.pinnedBy != loaded.pinnedBy) return false
        return true
    }

    private fun rowState(viewModel: CometChatMessageListViewModel, id: Long, kind: PinSaveKind): Boolean? {
        val loaded = viewModel.offlineRows().firstOrNull { it.id == id } ?: return null
        return if (kind == PinSaveKind.PIN) loaded.isPinned else loaded.isSaved
    }

    // ---------------------------------------------------------------- read offline

    /** The open chat's read receipt failed while offline: remember it for the next sweep. */
    fun rememberReadOffline(message: BaseMessage) = OfflineReadLedger.remember(message)

    /** A read receipt for this chat went through: nothing left to replay for it. */
    fun forgetReadOffline(message: BaseMessage) = OfflineReadLedger.forget(message)
}

/**
 * Chats read while offline, for this session (process-level, dropped on a session change). After
 * the next `MessageSyncCompleted` the hub calls [flush], which sends one `markAsRead` per chat for
 * the newest message the user actually read; an error is swallowed. A success emits `MessageRead`
 * only while the session that started the flush is still current, and marks that read first so the
 * conversation lists can tell it apart ([isFlushed]).
 */
internal object OfflineReadLedger {
    private data class Entry(val gen: Long, val message: BaseMessage)

    private val pending = LinkedHashMap<String, Entry>()

    fun remember(message: BaseMessage) {
        val key = chatKey(message) ?: return
        synchronized(pending) {
            val prev = pending[key]
            if (prev == null || prev.gen != OfflineHub.gen || prev.message.id <= message.id) {
                pending[key] = Entry(OfflineHub.gen, message)
            }
        }
    }

    fun forget(message: BaseMessage) {
        val key = chatKey(message) ?: return
        synchronized(pending) {
            val prev = pending[key] ?: return
            if (prev.message.id <= message.id) pending.remove(key)
        }
    }

    /** Called by the hub after `MessageSyncCompleted` (ON path). */
    fun flush() {
        val gen = OfflineHub.gen
        val batch = synchronized(pending) {
            val current = pending.values.filter { it.gen == gen }.map { it.message }
            pending.clear()
            current
        }
        batch.forEach { message ->
            runCatching {
                CometChat.markAsRead(message, object : CometChat.CallbackListener<Void>() {
                    override fun onSuccess(p0: Void?) {
                        // A session change since the flush began: the next session's lists must not
                        // see this read (ENG-41643, D-369).
                        if (OfflineHub.gen != gen) return
                        markFlushed(message)
                        CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageRead(message))
                    }

                    override fun onError(e: CometChatException?) {
                        // Swallowed, never surfaced. A chat read offline again is remembered again.
                    }
                })
            }
        }
    }

    // ------------------------------------------------ flushed-read marker (ENG-41643 revision 2)

    /** Most markers kept; the oldest is dropped first. A flush sends one read per chat. */
    private const val MAX_FLUSHED = 256

    /** Reads this flush sent, by `conversationId:id`, recorded just before each emit. */
    private val flushed = LinkedHashSet<String>()

    private fun flushedKey(m: BaseMessage): String? = m.conversationId?.let { "$it:${m.id}" }

    private fun markFlushed(message: BaseMessage) {
        val key = flushedKey(message) ?: return
        synchronized(flushed) {
            flushed.remove(key)
            flushed.add(key)
            while (flushed.size > MAX_FLUSHED) flushed.remove(flushed.first())
        }
    }

    /**
     * True when [message]'s `MessageRead` came from [flush] in this session. Read, never consumed,
     * so every conversation list alive sees it. Kit-internal: the public event is unchanged.
     */
    fun isFlushed(message: BaseMessage): Boolean {
        val key = flushedKey(message) ?: return false
        return synchronized(flushed) { key in flushed }
    }

    /** Drops every marker: called by the hub on a session end and on a cache reset. */
    fun clearFlushed() = synchronized(flushed) { flushed.clear() }

    @VisibleForTesting
    internal fun pendingForTest(): List<BaseMessage> = synchronized(pending) { pending.values.map { it.message } }

    @VisibleForTesting
    internal fun clearForTest() {
        synchronized(pending) { pending.clear() }
        clearFlushed()
    }

    private fun chatKey(m: BaseMessage): String? {
        val myUid = runCatching { CometChat.getLoggedInUser()?.uid }.getOrNull()
        return when (m.receiverType) {
            CometChatConstants.RECEIVER_TYPE_GROUP -> m.receiverUid?.let { "g:$it:${m.parentMessageId}" }
            CometChatConstants.RECEIVER_TYPE_USER -> {
                val peer = if (m.sender?.uid != null && m.sender?.uid != myUid) m.sender?.uid else m.receiverUid
                peer?.let { "u:$it:${m.parentMessageId}" }
            }
            else -> null
        }
    }
}

/**
 * The cache-reset notice rule (G8, BEH-ENC-15/16), posted once per reset by the hub through
 * [OfflineHub.postNotice]: lost > 0 → [OfflineNotice.UnsentLost]; unknown (−1) and not an
 * encryption change → [OfflineNotice.UnsentMaybeLost]; nothing lost, or an encryption change →
 * no notice.
 */
internal object OfflineSessionNotices {
    fun forCacheReset(reason: OfflineCacheResetReason, pendingMessagesLost: Int): OfflineNotice? = when {
        pendingMessagesLost > 0 -> OfflineNotice.UnsentLost(pendingMessagesLost)
        pendingMessagesLost < 0 && reason != OfflineCacheResetReason.ENCRYPTION_CHANGED -> OfflineNotice.UnsentMaybeLost
        else -> null
    }
}

/**
 * Offline turned off mid-session (README §3.7): every list that still shows un-acked rows reports
 * them at its next connection event; the reports of one transition are coalesced and **one**
 * `MessagesNotSent(n)` is posted through the hub, n = the distinct rows across lists.
 */
internal object OfflineOffTransition {
    @VisibleForTesting
    internal var coalesceMs: Long = 500L

    private val keys = HashSet<String>()
    private var scheduledGen: Long = -1L
    private var postedGen: Long = -1L
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)

    fun report(failed: Set<String>) {
        val gen = OfflineHub.gen
        synchronized(keys) {
            if (postedGen == gen) return
            if (scheduledGen != gen) {
                keys.clear()
                scheduledGen = gen
                keys += failed
                scope.launch {
                    kotlinx.coroutines.delay(coalesceMs)
                    val count = synchronized(keys) {
                        postedGen = gen
                        keys.size.also { keys.clear() }
                    }
                    if (count > 0 && OfflineHub.gen == gen) OfflineHub.postNotice(OfflineNotice.MessagesNotSent(count))
                }
            } else {
                keys += failed
            }
        }
    }

    /**
     * Offline is on again: the next turn-off is a new transition (the hub gen moves only on logout
     * or a uid switch), so forget everything about the previous one.
     */
    fun onGateOn() = synchronized(keys) {
        keys.clear()
        scheduledGen = -1L
        postedGen = -1L
    }

    @VisibleForTesting
    internal fun resetForTest() = synchronized(keys) {
        keys.clear(); scheduledGen = -1L; postedGen = -1L
    }
}
