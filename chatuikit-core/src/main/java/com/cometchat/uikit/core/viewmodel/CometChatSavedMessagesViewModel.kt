package com.cometchat.uikit.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineListSession
import com.cometchat.uikit.core.offline.OfflinePagedMerge
import com.cometchat.uikit.core.offline.OfflineReconnectRefill
import com.cometchat.uikit.core.offline.PinnedSavedTwoPhaseLoad
import com.cometchat.uikit.core.state.PinnedSavedListUIState
import com.cometchat.uikit.core.utils.getDefaultMessagesCategories
import com.cometchat.uikit.core.utils.getDefaultMessagesTypes
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel backing the user-level Saved Messages screen.
 *
 * Saved messages are private to the current user and span all conversations, ordered
 * newest-save-first. Each row carries its source conversation context (receiver / receiverType) so
 * the screen can render a "@person / #group" label and deep-link without extra calls. The screen is
 * read-only (no markAsRead / receipts / unread changes).
 *
 * @param enableListeners subscribe to the UIKit bus for live upkeep (false for tests/previews)
 */
public open class CometChatSavedMessagesViewModel(
    private val enableListeners: Boolean = true
) : ViewModel() {

    public companion object {
        private const val DEFAULT_LIMIT = 30
        private const val MAX_LIMIT = 100
    }

    private val _messages = MutableStateFlow<List<BaseMessage>>(emptyList())

    /** The current user's saved messages across all conversations, newest save first. */
    public val messages: StateFlow<List<BaseMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<PinnedSavedListUIState>(PinnedSavedListUIState.Loading)

    /** Screen state: [PinnedSavedListUIState.Loading] until the first load settles. */
    public val uiState: StateFlow<PinnedSavedListUIState> = _uiState.asStateFlow()

    private val _connectToLoad = MutableStateFlow(false)

    /**
     * True while [uiState] is [PinnedSavedListUIState.Error] because the load ran offline with
     * nothing shown (D84 rule 3): the panel shows its offline state ("Connect to load messages", no
     * Retry). It reads once on reconnect. Always false with offline messaging off.
     */
    public val connectToLoad: StateFlow<Boolean> = _connectToLoad.asStateFlow()

    private val _count = MutableStateFlow(0)

    /** Exact saved-message count (fetch-all-and-count; the backend caps at 100). */
    public val count: StateFlow<Int> = _count.asStateFlow()

    private val _unsaveSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** One-shot signal emitted when an unsave call succeeds, so the View can show a toast. */
    public val unsaveSuccess: SharedFlow<Unit> = _unsaveSuccess.asSharedFlow()

    private var request: MessagesRequest? = null

    /**
     * The in-flight paging job. Cancelled before a new load starts so a stale run can never publish
     * its pages over a fresher list, and so two rapid reloads cannot page concurrently.
     */
    private var loadJob: Job? = null

    /** Load generation: a load whose generation is no longer current never publishes. */
    private var loadGen = 0L

    /**
     * Opened offline with nothing cached: the offline state (D84), then one read when the connection
     * returns, only if the panel is still empty (ENG-41462). Never armed offline off.
     */
    private val offlineRefill = OfflineReconnectRefill(UIKitConstants.ListenerTags.SAVED_MESSAGES) {
        viewModelScope.launch { if (_messages.value.isEmpty()) reload() }
    }
    private var listenersTag: String? = null

    // G8: session end and cache reset (ENG-41568). Started only with offline on.
    private val offlineSession = OfflineListSession(
        onSessionEnded = { clearForOfflineSession() },
        onCacheReset = { onOfflineCacheReset() }
    )

    init {
        if (enableListeners) {
            addListeners()
            if (OfflineGate.isOn()) offlineSession.start(viewModelScope)
        }
    }

    /**
     * Clears the panel for a session change (logout or a switch to a different uid), before the
     * next frame (G8). Cancels the load in flight and bumps its generation, so none of its pages
     * lands, and empties the rows and the count. The next [reload] rebuilds the request, so it
     * starts from page one. Offline on only (the hub drives it).
     */
    private fun clearForOfflineSession() {
        loadJob?.cancel()
        loadGen++
        offlineRefill.disarm()
        _connectToLoad.value = false
        _messages.value = emptyList()
        _count.value = 0
        _uiState.value = PinnedSavedListUIState.Loading
    }

    /**
     * The offline cache was reset (G8): clear as for a session end, then [reload] once from page one
     * if the panel had loaded. A failure over cached rows keeps them and reports nothing (D84 rule 5).
     */
    private fun onOfflineCacheReset() {
        clearForOfflineSession()
        if (request != null) reload()
    }

    /**
     * Subscribes directly to the SDK's message listener so the screen stays live on its own.
     *
     * The View also forwards [CometChatMessageEvent.MessageSaved]/[CometChatMessageEvent.MessageUnsaved]
     * from the UIKit bus, but that bus is only fed by [CometChatMessageListViewModel] — i.e. only
     * while a message list is mounted. Saved Messages is user-level and normally opened straight from
     * the conversations menu with no message list anywhere (and on a second device the SDK event is
     * the ONLY signal), so it registers its own listener. Both paths land on the same idempotent
     * [onMessageSavedExternally]/[onMessageUnsavedExternally] handlers, so a doubled delivery is a
     * no-op.
     */
    private fun addListeners() {
        val tag = "${UIKitConstants.ListenerTags.SAVED_MESSAGES}_${hashCode()}_${System.currentTimeMillis()}"
        listenersTag = tag
        CometChat.addMessageListener(tag, object : CometChat.MessageListener() {
            override fun onMessageSaved(message: BaseMessage) {
                onMessageSavedExternally(message)
            }

            override fun onMessageUnsaved(message: BaseMessage) {
                onMessageUnsavedExternally(message)
            }
        })
    }

    private fun removeListeners() {
        listenersTag?.let { CometChat.removeMessageListener(it) }
        listenersTag = null
    }

    override fun onCleared() {
        removeListeners()
        offlineRefill.release()
        super.onCleared()
    }

    /** Rebuilds the request and loads all pages (bounded by the 100 cap). */
    public fun reload() {
        // Same types + categories as the message list — without them the server's own (narrower)
        // defaults silently exclude custom-category messages (polls, stickers, whiteboard, etc.).
        request = MessagesRequest.MessagesRequestBuilder()
            .setSaved(true)
            .setLimit(DEFAULT_LIMIT)
            .setTypes(getDefaultMessagesTypes())
            .setCategories(getDefaultMessagesCategories())
            .build()
        _messages.value = emptyList()
        _count.value = 0
        _uiState.value = PinnedSavedListUIState.Loading
        loadAll()
    }

    /**
     * Fetches every page (bounded by the 100 cap) on the SDK two-phase `fetchNext`
     * ([PinnedSavedTwoPhaseLoad]). Offline off: exactly today's load. Offline on: the cached page is
     * the first paint, server order is kept and no row repeats (BEH-SAVE-02); a not-connected end
     * keeps the rows shown, or shows the offline state / error view when nothing is shown (D84); a stale generation is
     * dropped.
     */
    private fun loadAll() {
        val req = request ?: run { reload(); return }
        // Cancel first: once cancelled the previous job can no longer publish its pages (the write
        // below the loop is unreachable after cancellation), so a fresh reload always wins.
        loadJob?.cancel()
        offlineRefill.disarm()
        val gen = ++loadGen
        val offlineOn = OfflineGate.isOn()
        loadJob = viewModelScope.launch {
            val outcome = PinnedSavedTwoPhaseLoad.loadAll(req, offlineOn, MAX_LIMIT) { rows ->
                if (gen == loadGen) publish(rows, settled = false)
            }
            if (gen != loadGen) return@launch
            when (outcome) {
                is PinnedSavedTwoPhaseLoad.Outcome.Loaded -> publish(outcome.rows, settled = true)
                is PinnedSavedTwoPhaseLoad.Outcome.Failed -> when {
                    outcome.rows.isNotEmpty() -> publish(outcome.rows, settled = true)
                    else -> {
                        // D84 rule 3: offline with nothing shown is the kit's offline state, which
                        // reads once on reconnect (rule 8).
                        val offline = offlineOn && OfflinePagedMerge.isNotConnected(outcome.exception)
                        _connectToLoad.value = offline
                        _uiState.value = PinnedSavedListUIState.Error(outcome.exception)
                        if (offline) offlineRefill.arm()
                    }
                }
            }
        }
    }

    /** Shows [rows]; an unsettled (mid-load) empty paint keeps the Loading state. */
    private fun publish(rows: List<BaseMessage>, settled: Boolean) {
        _messages.value = rows
        _count.value = rows.size
        if (rows.isNotEmpty()) _uiState.value = PinnedSavedListUIState.Content
        else if (settled) _uiState.value = PinnedSavedListUIState.Empty
    }

    /** Unsaves a message: optimistic remove, revert on error, broadcast on success. */
    public fun unsave(message: BaseMessage) {
        val index = _messages.value.indexOfFirst { it.id == message.id }
        removeRow(message)

        CometChat.unsaveMessage(message.id, object : CometChat.CallbackListener<BaseMessage>() {
            override fun onSuccess(updated: BaseMessage) {
                _unsaveSuccess.tryEmit(Unit)
                CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageUnsaved(updated))
            }

            override fun onError(e: CometChatException) {
                // Revert. The full-screen Error state is reserved for LOAD failures — a failed
                // unsave on a healthy list just restores the row.
                restoreRow(message, index)
            }
        })
    }

    /** Optimistically drops a row, keeping count and screen state in step. */
    private fun removeRow(message: BaseMessage) {
        _messages.update { list -> list.filter { it.id != message.id } }
        _count.value = _messages.value.size
        if (_messages.value.isEmpty()) _uiState.value = PinnedSavedListUIState.Empty
    }

    /**
     * Undoes [removeRow] by re-inserting the single removed row at [index].
     *
     * Deliberately not a whole-snapshot restore: a save/unsave delivered by the SDK listener
     * between the optimistic removal and this failure callback must survive the revert.
     */
    private fun restoreRow(message: BaseMessage, index: Int) {
        if (index >= 0) {
            _messages.update { list ->
                if (list.any { it.id == message.id }) list
                else list.toMutableList().apply { add(index.coerceAtMost(size), message) }
            }
        }
        _count.value = _messages.value.size
        _uiState.value = if (_messages.value.isNotEmpty()) PinnedSavedListUIState.Content
        else PinnedSavedListUIState.Empty
    }

    /**
     * Live upkeep. Called both by this ViewModel's own SDK listener (see [addListeners]) and by the
     * View's lifecycle-aware UIKit-bus subscription; both are safe to fire for the same message.
     */
    public fun onMessageSavedExternally(message: BaseMessage) {
        if (_messages.value.none { it.id == message.id }) {
            _messages.update { list ->
                if (list.any { it.id == message.id }) list else listOf(message) + list
            }
            _count.value = _messages.value.size
            _uiState.value = PinnedSavedListUIState.Content
        }
    }

    /** Removes a row when a message is unsaved elsewhere (see [onMessageSavedExternally]). */
    public fun onMessageUnsavedExternally(message: BaseMessage) {
        if (_messages.value.any { it.id == message.id }) {
            removeRow(message)
        }
    }
}
