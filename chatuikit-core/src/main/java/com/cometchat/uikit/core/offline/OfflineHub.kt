package com.cometchat.uikit.core.offline

import androidx.annotation.VisibleForTesting
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.OfflineCacheResetListener
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.MediaDownloadListener
import com.cometchat.chat.models.MessageFailureReason
import com.cometchat.chat.models.OfflineCacheResetReason
import com.cometchat.chat.models.SyncOutcome
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatOfflineEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * The process-level singleton that bridges the SDK's offline callbacks to the kit's lists (design
 * §3.5). Started from [com.cometchat.uikit.core.CometChatUIKit] init, after SDK init succeeds, and
 * **idempotent** — a second init registers nothing. Its SDK registrations are made **once** under
 * their own ids and are **never** removed on logout; they survive logout and re-login on the SDK.
 *
 * Every handler is **inert while offline is off** (returns on `!OfflineGate.isOn()`), so an
 * offline-off process behaves exactly as today.
 *
 * It publishes:
 * - internal [OfflineHubEvent]s on [events], a **lossless** flow (unbounded, never DROP_OLDEST),
 *   each stamped with the session [gen]; the lists drop events from an older gen (G8);
 * - the additive public [CometChatOfflineEvent]s through [CometChatEvents.offlineEvents], exactly
 *   once per transition per `localMessageId` (forward-only), ON path only;
 * - list-less [OfflineNotice]s via [postNotice] / [receiveNotice], a single-delivery channel (shown once, on whichever
 *   list collects first).
 */
internal object OfflineHub {

    private const val OUTBOX_LISTENER_ID = "uikit-offline-hub-outbox"
    private const val SYNC_LISTENER_ID = "uikit-offline-hub-sync"
    private const val RESET_LISTENER_ID = "uikit-offline-hub-reset"
    private const val MEDIA_LISTENER_ID = "uikit-offline-hub-media"
    private const val MESSAGE_LISTENER_ID = "uikit-offline-hub-messages"
    private const val MAX_TERMINAL_IDS = 512

    @Volatile
    private var started = false
    private val startLock = Any()

    private val generation = AtomicLong(0L)

    /** The current session generation. Bumped on logout and on a switch to a different uid (G8). */
    val gen: Long get() = generation.get()

    /** The uid the hub currently considers logged in, to tell a user switch from a token refresh. */
    @Volatile
    private var currentUid: String? = null

    /**
     * Forward-only "last emitted state" per in-flight localMessageId (ON path only). An entry is
     * removed when the send reaches a terminal state, and every entry is cleared on a session end.
     */
    private val lastState = HashMap<String, MessageSendState>()

    /**
     * localMessageIds that reached a terminal state (SENT, or FAILED with a removal reason), so a
     * late callback for them emits nothing. Bounded LRU; cleared on a session end.
     */
    private val terminalIds = object : LinkedHashMap<String, Unit>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Unit>?): Boolean =
            size > MAX_TERMINAL_IDS
    }
    private val sendLock = Any()

    // ---- lossless internal fan-out ----
    private val _events = MutableSharedFlow<OfflineHubEvent>(
        replay = 0,
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    val events: SharedFlow<OfflineHubEvent> = _events.asSharedFlow()

    // ---- single-delivery notices (belong to no one list) ----
    /**
     * Single-delivery queue of list-less notices. If a receiver is cancelled after the channel
     * handed it a notice but before it resumed (a list cleared mid-receive), the notice is put back
     * on the queue rather than lost.
     */
    private val noticeChannel: Channel<OfflineNotice> =
        Channel(Channel.UNLIMITED) { undelivered -> requeueNotice(undelivered) }

    private fun requeueNotice(notice: OfflineNotice) {
        noticeChannel.trySend(notice)
    }

    /**
     * Registers the SDK listeners once and records the logged-in uid. Safe to call from every init
     * entry point and on every (re)start — the second call is a no-op.
     */
    fun start() {
        if (started) return
        synchronized(startLock) {
            if (started) return
            registerListeners()
            currentUid = loggedInUid()
            started = true
        }
    }

    /**
     * Logout: end the session and bump [gen] so in-flight events are dropped by the lists, and
     * clear the per-send dedupe. Registrations are left in place.
     */
    fun onLogout() {
        endSession()
        currentUid = null
    }

    /**
     * Reconcile the logged-in uid. A switch to a **different** uid ends the session and bumps
     * [gen]; the same uid (a token refresh) does nothing. A first login (no prior uid) just records
     * the uid.
     */
    fun noteSession(uid: String?) {
        if (uid.isNullOrEmpty()) return
        val prev = currentUid
        when {
            prev == null -> currentUid = uid
            prev != uid -> {
                endSession()
                currentUid = uid
            }
            // prev == uid: token refresh, no clear.
        }
    }

    /**
     * W4 feeds a per-call send result (the per-send [CometChat.MessageSentListener]) through here so
     * it shares the hub's single-event-per-transition dedupe with the outbox callbacks.
     *
     * @param offlineOn the mode the send captured when the user tapped send (G1). The gate is not
     * re-read here, so a flag flip mid-send cannot split one send across two modes.
     */
    fun reportSend(
        message: BaseMessage,
        state: MessageSendState,
        reason: MessageFailureReason?,
        offlineOn: Boolean
    ) {
        if (!offlineOn) return
        onSend(message, state, reason)
    }

    /**
     * The kit's Delete removed the unsent thread reply [message] on the device (BEH-SEND-36).
     * Publishes the internal [OfflineHubEvent.ThreadReplyDeleted] only: no public event, and the
     * send dedupe is untouched. Ignored for a message that is not a reply or has no localMessageId.
     */
    fun reportThreadReplyDeleted(message: BaseMessage, discarded: Boolean = true) {
        val localId = message.localMessageId
        if (localId.isNullOrEmpty() || message.parentMessageId <= 0L) return
        publish(OfflineHubEvent.ThreadReplyDeleted(gen, localId, message, discarded))
    }

    /** Post a notice that belongs to no single list; the first list to collect shows it once. */
    fun postNotice(notice: OfflineNotice) {
        noticeChannel.trySend(notice)
    }

    /**
     * Takes the next list-less notice, suspending until one is posted. A list's notice relay calls
     * this only while its UI has a subscriber, so a notice is never consumed by a list nobody sees.
     */
    internal suspend fun receiveNotice(): OfflineNotice = noticeChannel.receive()

    // ------------------------------------------------------------------

    private fun endSession() {
        generation.incrementAndGet()
        synchronized(sendLock) {
            lastState.clear()
            terminalIds.clear()
        }
        OfflineReadLedger.clearFlushed()
        publish(OfflineHubEvent.SessionEnded(gen))
    }

    /**
     * Applies one send callback. Transitions are forward-only (README §3.4): first sighting → any
     * state; SENDING → FAILED; FAILED → SENDING (retry); anything → SENT. A repeat of the current
     * state, or any callback after a terminal state (SENT, or FAILED with a removal reason), emits
     * nothing — neither the internal [OfflineHubEvent.SendState] nor the public event. A message
     * with no localMessageId bypasses the dedupe (it cannot be tracked) and is published as is.
     */
    private fun onSend(message: BaseMessage, state: MessageSendState, reason: MessageFailureReason?) {
        val localId = message.localMessageId
        if (localId.isNullOrEmpty()) {
            publish(OfflineHubEvent.SendState(gen, "", message, state, reason))
            return
        }
        val accepted = synchronized(sendLock) {
            if (terminalIds.containsKey(localId)) return@synchronized false
            val previous = lastState[localId]
            if (!isForward(previous, state)) return@synchronized false
            if (isTerminal(state, reason)) {
                lastState.remove(localId)
                terminalIds[localId] = Unit
            } else {
                lastState[localId] = state
            }
            true
        }
        if (!accepted) return
        publish(OfflineHubEvent.SendState(gen, localId, message, state, reason))
        CometChatEvents.emitOfflineEvent(
            CometChatOfflineEvent.MessageSendStateChanged(message, state, reason)
        )
    }

    private fun isForward(previous: MessageSendState?, next: MessageSendState): Boolean = when {
        previous == null -> true
        previous == next -> false
        next == MessageSendState.SENT -> true
        previous == MessageSendState.SENDING && next == MessageSendState.FAILED -> true
        previous == MessageSendState.FAILED && next == MessageSendState.SENDING -> true
        else -> false
    }

    /** SENT ends a send; so does a failure that removes the row (DISCARDED, QUEUE_OVERFLOW). */
    private fun isTerminal(state: MessageSendState, reason: MessageFailureReason?): Boolean =
        state == MessageSendState.SENT ||
            (state == MessageSendState.FAILED &&
                (reason == MessageFailureReason.DISCARDED || reason == MessageFailureReason.QUEUE_OVERFLOW))

    private fun publish(event: OfflineHubEvent) {
        // Lossless: the unbounded buffer makes tryEmit always succeed, and emitting on the caller
        // thread keeps a burst in order. Collectors (the ViewModels) dispatch on Main.
        _events.tryEmit(event)
    }

    private fun registerListeners() {
        CometChat.addOfflineMessageListener(
            OUTBOX_LISTENER_ID,
            object : CometChat.OfflineMessageListener() {
                override fun onOutboxMessagePending(message: BaseMessage) {
                    if (!OfflineGate.isOn()) return
                    onSend(message, MessageSendState.SENDING, null)
                }

                override fun onOutboxMessageSent(message: BaseMessage) {
                    if (!OfflineGate.isOn()) return
                    onSend(message, MessageSendState.SENT, null)
                }

                override fun onOutboxMessageFailed(message: BaseMessage, reason: MessageFailureReason) {
                    if (!OfflineGate.isOn()) return
                    onSend(message, MessageSendState.FAILED, reason)
                }
            }
        )

        CometChat.addSyncListener(
            SYNC_LISTENER_ID,
            object : CometChat.SyncListener() {
                override fun onConversationSyncCompleted(conversations: List<Conversation>) {
                    if (!OfflineGate.isOn()) return
                    publish(OfflineHubEvent.ConversationSyncCompleted(gen, conversations))
                }

                override fun onConversationSyncError(e: CometChatException) {
                    if (!OfflineGate.isOn()) return
                    publish(OfflineHubEvent.ConversationSyncFailed(gen, e))
                }

                override fun onMessageSyncCompleted(messages: List<BaseMessage>, outcome: SyncOutcome) {
                    if (!OfflineGate.isOn()) return
                    publish(OfflineHubEvent.MessageSyncCompleted(gen, messages, outcome))
                    // W5 (G7): chats read while offline are marked read once, after the sweep.
                    OfflineReadLedger.flush()
                }

                override fun onMessageSyncError(e: CometChatException) {
                    if (!OfflineGate.isOn()) return
                    publish(OfflineHubEvent.MessageSyncFailed(gen, e))
                }
            }
        )

        CometChat.addOfflineCacheResetListener(
            RESET_LISTENER_ID,
            object : OfflineCacheResetListener() {
                override fun onOfflineCacheReset(reason: OfflineCacheResetReason, pendingCount: Int) {
                    if (!OfflineGate.isOn()) return
                    OfflineReadLedger.clearFlushed()
                    publish(OfflineHubEvent.CacheReset(gen, reason, pendingCount))
                    // W5 (G8): the lost-sends notice is posted here, once per reset, for whichever
                    // list shows it first — never once per list.
                    OfflineSessionNotices.forCacheReset(reason, pendingCount)?.let { postNotice(it) }
                    CometChatEvents.emitOfflineEvent(
                        CometChatOfflineEvent.OfflineCacheReset(reason, pendingCount)
                    )
                }
            }
        )

        CometChat.addMediaDownloadListener(
            MEDIA_LISTENER_ID,
            object : MediaDownloadListener {
                override fun onMediaDownloaded(messageLocalId: String?, attachmentId: String, localPath: String) {
                    if (!OfflineGate.isOn()) return
                    publish(OfflineHubEvent.MediaReady(gen, messageLocalId, attachmentId, localPath))
                }

                override fun onMediaDownloadFailed(
                    messageLocalId: String?,
                    attachmentId: String?,
                    e: CometChatException
                ) {
                    // W5 decides whether a failed download needs a user-visible signal.
                }
            }
        )

        // One message listener that overrides ONLY onMessagesSynced — rows already shown are
        // updated, never inserted (BEH-INV-11). No other MessageListener callback is handled here.
        CometChat.addMessageListener(
            MESSAGE_LISTENER_ID,
            object : CometChat.MessageListener() {
                override fun onMessagesSynced(messages: List<BaseMessage>) {
                    if (!OfflineGate.isOn()) return
                    publish(OfflineHubEvent.MessagesUpdated(gen, messages))
                }
            }
        )
    }

    private fun loggedInUid(): String? =
        runCatching { CometChat.getLoggedInUser()?.uid }.getOrNull()

    /** Test seam: publish an arbitrary (e.g. stale-gen) event onto the hub flow. */
    @VisibleForTesting
    internal fun publishForTest(event: OfflineHubEvent) = publish(event)

    /** Test seam: the number of in-flight sends the dedupe is tracking. */
    @VisibleForTesting
    internal fun trackedSendCountForTest(): Int = synchronized(sendLock) { lastState.size }

    @VisibleForTesting
    fun resetForTest() {
        synchronized(startLock) {
            started = false
            generation.set(0L)
            synchronized(sendLock) {
                lastState.clear()
                terminalIds.clear()
            }
            currentUid = null
            OfflineReadLedger.clearFlushed()
            while (noticeChannel.tryReceive().isSuccess) { /* drain */ }
        }
    }
}
