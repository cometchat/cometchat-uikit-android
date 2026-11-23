package com.cometchat.uikit.core.offline

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.helpers.CometChatHelper
import com.cometchat.chat.models.Action
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.MessageFailureReason
import com.cometchat.chat.models.MessageSource
import com.cometchat.chat.models.MessageStatus
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.domain.repository.ConversationListTwoPhaseRepository
import com.cometchat.uikit.core.state.UIState
import com.cometchat.uikit.core.utils.CometChatLogger
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * W3 (ENG-41088): the conversation list's offline-ON path — two-phase reads, the reconnect sync
 * slice, the own unsent preview, swept unread, own pin from `onSuccess`, the swept self-removal and
 * the cache reset (G2, G7, G8, G11; BEH-READ-01/09b, BEH-SYNC-08/12/13/15/30, BEH-CONV-01/06,
 * BEH-CPIN-09, BEH-FLAG-04, UIK-CONV-01, R106/D17, B2).
 *
 * Every entry point is reached only behind the ViewModel's captured `offlineOn`, so the OFF path is
 * today's code. The `Boolean` hooks return false ("not handled") wherever today's path must run.
 * Hub events and SDK callbacks arrive on the main thread; [onMessageListenerEvent] may arrive on
 * the socket thread, so it touches the list only through the ViewModel's atomic update.
 */
internal object ConversationListOffline {

    private const val TAG = "ConversationListOffline"
    private const val DEFAULT_LIMIT = 30

    /** Reads a reconnect retry may make while pages add no new chat (D75, as React/Angular). */
    private const val MAX_RETRY_PAGES = 10

    /** Sort tiers above any seconds timestamp, so floating pins sort first (BEH-CPIN-02). */
    private const val OWN_PIN_TIER = 2_000_000_000_000L
    private const val ADMIN_PIN_TIER = 4_000_000_000_000L

    /** How a list treats rows and pin events, from its request's filters (BEH-CPIN-02/09). */
    internal enum class ListMode {
        /** No filter: sync slice, pins float and unknown pinned chats are inserted. */
        PLAIN,

        /** Only/hide agentic: pins move as on the plain list, unknown chats are never inserted. */
        AGENTIC,

        /** Type, tag, search, unread or `pinnedBy`: rows only update in place. */
        FIXED
    }

    /** Per-ViewModel state. Main thread only. */
    internal class State {
        /** The newest read; an older read's phases are dropped. */
        var readToken: Long = 0L

        /** The request whose first page has had its server phase; any other request is on page 1 (D17). */
        var pagedRequest: ConversationsRequest? = null

        /** A filtered list's sync re-read is running; repeats coalesce into it. */
        var resyncInFlight: Boolean = false

        /** A load-more page failed with not-connected; the next connect reads it again (D75). */
        var pageFailedOffline: Boolean = false

        /** Server phases applied so far; tells a finished read that it was answered. */
        var serverPhases: Long = 0L

        /**
         * A refresh failed offline with nothing on screen, so the list shows its error state: the
         * next connect reads the list again, once (D83 rule 3). A failed load-more page uses
         * [pageFailedOffline] (D75).
         */
        var refreshFailedOffline: Boolean = false

        /** The read in flight is a first-page read (ENG-41551 G). */
        var firstPageInFlight: Boolean = false

        /** A load-more asked while the first page was in flight; it runs when that read ends. */
        var loadMoreDeferred: Boolean = false

        /**
         * Conversations a live event wrote since the last connect, by conversationId: written after
         * the sweep was requested, so its page proves nothing about them (BEH-SYNC-15). Live
         * message hooks may run on the socket thread, hence concurrent.
         */
        val touchedLive: MutableSet<String> = ConcurrentHashMap.newKeySet()

        /** The row an own unsent preview replaced, by localMessageId (null: there was no row). */
        val beforePending: HashMap<String, Conversation?> = HashMap()

        /** How many marks each unsent preview has shown, by localMessageId (see [placePending]). */
        val markVersion: HashMap<String, Long> = HashMap()

        /** Last token handed out by [onMessageRead]; never reset, so a token is never reused. */
        var readRefreshSeq: Long = 0L

        /** The newest local read's token, by conversationId: an older read's server answer is dropped. */
        val readRefreshTokens: MutableMap<String, Long> = ConcurrentHashMap()

        fun clear() {
            readToken++
            pagedRequest = null
            resyncInFlight = false
            pageFailedOffline = false
            firstPageInFlight = false
            loadMoreDeferred = false
            refreshFailedOffline = false
            touchedLive.clear()
            beforePending.clear()
            markVersion.clear()
            readRefreshTokens.clear()
        }
    }

    // ------------------------------------------------------------------ reads (G2)

    /**
     * Hook for `fetchConversations()`. A load-more asked while the first page is in flight is
     * deferred, not dropped: it runs once that read ends (ENG-41551 G). [quiet] for the reload the
     * kit starts by itself after a cache reset (see [read]). @return true when handled.
     */
    fun fetchConversations(viewModel: CometChatConversationsViewModel, quiet: Boolean = false): Boolean {
        val repository = viewModel.twoPhaseRepository ?: return false
        if (viewModel.isFetching) {
            val state = viewModel.offlineConversationState
            if (state.firstPageInFlight) state.loadMoreDeferred = true
            return true
        }
        if (!viewModel.hasMoreData) return true
        val request = viewModel.conversationsRequest
            ?: builderOf(viewModel).build().also { viewModel.conversationsRequest = it }
        read(viewModel, repository, request, refresh = false, quiet = quiet)
        return true
    }

    /**
     * Hook for `refreshList()`, an explicit refresh (Aditya, 2026-11-04): the list is **replaced**.
     * The cached page shows first, then the server page replaces the whole list; only own unsent
     * previews are kept. If the server read fails, the cached page and the unsent previews stay.
     * D17 exception: a **filtered** refresh whose server page is empty keeps the cached page this
     * read showed; when the read showed no cached page, the empty server page still replaces.
     * @return true when handled.
     */
    fun refreshList(viewModel: CometChatConversationsViewModel): Boolean = refreshList(viewModel, quiet = false)

    /** [refreshList]; [quiet] for a refresh the kit starts by itself (see [read]). */
    private fun refreshList(viewModel: CometChatConversationsViewModel, quiet: Boolean): Boolean {
        val repository = viewModel.twoPhaseRepository ?: return false
        viewModel.hasMoreData = true
        read(viewModel, repository, builderOf(viewModel).build(), refresh = true, replace = true, quiet = quiet)
        return true
    }

    /**
     * Hook for the `GroupCreated` UI event: a merging two-phase refresh, so the new group is added
     * and nothing shown is dropped. The kit starts it by itself, so it is quiet. @return true when
     * handled.
     */
    fun onGroupCreated(viewModel: CometChatConversationsViewModel): Boolean {
        val repository = viewModel.twoPhaseRepository ?: return false
        viewModel.hasMoreData = true
        read(viewModel, repository, builderOf(viewModel).build(), refresh = true, quiet = true)
        return true
    }

    private fun builderOf(viewModel: CometChatConversationsViewModel): ConversationsRequest.ConversationsRequestBuilder =
        viewModel.conversationsRequestBuilder
            ?: ConversationsRequest.ConversationsRequestBuilder().setLimit(DEFAULT_LIMIT)

    /**
     * One two-phase read (README §3.3). Cached rows are the first paint; the server phase prunes
     * only rows its own cached phase delivered; an empty cached page never ends the list; only an
     * empty **unfiltered first** server page clears cached rows (D17, BEH-SYNC-15). A newer read or
     * a generation bump drops this read's later phases.
     *
     * [quiet]: the kit started this read by itself (reconnect re-read, sweep resync, a created
     * group, the reload after a cache reset). A failure while rows are on screen then reports nothing, as before the offline
     * feature; with nothing on screen [showNothingRead] still decides the state (D84 rule 5).
     */
    private fun read(
        viewModel: CometChatConversationsViewModel,
        repository: ConversationListTwoPhaseRepository,
        request: ConversationsRequest,
        refresh: Boolean,
        replace: Boolean = false,
        quiet: Boolean = false,
        onDone: (() -> Unit)? = null
    ) {
        val state = viewModel.offlineConversationState
        val token = ++state.readToken
        val gen = viewModel.offlineListGen
        val firstPage = refresh || state.pagedRequest !== request
        // D83: decided at read start. Only a window load (a refresh, or a read that starts with
        // nothing shown) reports a failure over kept rows; a load-more page never does, even when no
        // server page has answered yet (offline from open, pagedRequest is still unset).
        val window = !quiet && (refresh || replace || viewModel._conversations.value.isEmpty())
        val mode = modeOf(request)
        val spec = specOf(floatsPins(request))
        viewModel.isFetching = true
        state.firstPageInFlight = firstPage
        if (viewModel._conversations.value.isEmpty()) viewModel._uiState.value = UIState.Loading

        viewModel.viewModelScope.launch {
            var cachedKeys: Set<Any> = emptySet()
            var showedCachedPage = false
            var cachedRows = 0
            var answered = false
            try {
                repository.fetchNextTwoPhase(request).collect { phase ->
                    if (token != state.readToken || gen != viewModel.offlineListGen) return@collect
                    when (phase) {
                        is TwoPhaseResult.Cached -> {
                            cachedRows += phase.rows.size
                            if (replace) {
                                replaceWith(viewModel, phase.rows, spec)
                                showedCachedPage = true
                                // The rows shown now belong to this request.
                                viewModel.conversationsRequest = request
                            } else {
                                cachedKeys = apply(viewModel, gen, OfflineListReconcile.Phase.CACHED, phase.rows, emptySet(), spec)
                            }
                            publish(viewModel, serverDone = false)
                        }

                        is TwoPhaseResult.Server -> {
                            if (replace) {
                                // A filtered empty answer never clears the cached page this read showed
                                // (D17); without one, the old list is replaced as for any other answer.
                                val keepCachedPage = phase.rows.isEmpty() && mode != ListMode.PLAIN && showedCachedPage
                                if (!keepCachedPage) replaceWith(viewModel, phase.rows, spec)
                            } else if (firstPage && mode == ListMode.PLAIN && phase.rows.isEmpty()) {
                                // A clean, unfiltered, empty first page: the user has no chats.
                                viewModel._conversations.update { list -> list.filter { isOwnUnsent(it.lastMessage) } }
                            } else {
                                apply(viewModel, gen, OfflineListReconcile.Phase.SERVER, phase.rows, cachedKeys, spec)
                            }
                            viewModel.hasMoreData = phase.rows.isNotEmpty()
                            state.pageFailedOffline = false
                            answered = true
                            state.serverPhases++
                            if (refresh) viewModel.conversationsRequest = request
                            state.pagedRequest = request
                            publish(viewModel, serverDone = true)
                            if (refresh) viewModel._scrollToTopEvent.emit(Unit)
                        }

                        is TwoPhaseResult.Error -> {
                            // A load-more page past the cache (no cached row) failed offline: hasMore
                            // stays true and the request keeps its cursor, so the next connect reads it
                            // again (D75). A page the cache filled ends not-connected too (BEH-READ-05)
                            // and is not a failure.
                            if (!refresh && cachedRows == 0 && OfflinePagedMerge.isNotConnected(phase.exception)) {
                                state.pageFailedOffline = true
                            }
                            // Cached rows already shown are kept (BEH-READ-06). Over rows, only an
                            // online failure of a window load reports onError (D84 rules 2 and 5);
                            // with nothing on screen, showNothingRead decides the state.
                            if (viewModel._conversations.value.isEmpty()) {
                                showNothingRead(viewModel, request, refresh, phase.exception)
                            } else if (window && !OfflinePagedMerge.isNotConnected(phase.exception)) {
                                // A failed load-more page keeps its footer and reports nothing.
                                viewModel._rowsKeptError.trySend(phase.exception)
                            }
                        }
                    }
                }
            } catch (e: CometChatException) {
                if (token == state.readToken) {
                    if (viewModel._conversations.value.isEmpty()) {
                        showNothingRead(viewModel, request, refresh, e)
                    } else if (window && !OfflinePagedMerge.isNotConnected(e)) {
                        viewModel._rowsKeptError.trySend(e)
                    }
                }
            } finally {
                var runDeferred = false
                if (token == state.readToken) {
                    viewModel.isFetching = false
                    state.firstPageInFlight = false
                    // ENG-41551 G: a load-more asked during the first page runs now, only if the
                    // server answered that page. A first page that failed in any way (offline:
                    // the next connect reads it again, D75) drops the deferred load-more.
                    runDeferred = state.loadMoreDeferred && answered
                    state.loadMoreDeferred = false
                }
                onDone?.invoke()
                if (runDeferred) fetchConversations(viewModel)
            }
        }
    }

    /** Replaces the list with [rows] plus the own unsent previews not among them, in sorted place. */
    private fun replaceWith(
        viewModel: CometChatConversationsViewModel,
        rows: List<Conversation>,
        spec: OfflineListReconcile.Spec<Conversation, String>
    ) {
        viewModel._conversations.update { current ->
            val page = protectPreviews(current, distinct(rows))
            val ids = page.mapTo(HashSet()) { it.conversationId }
            val unsent = current.filter { isOwnUnsent(it.lastMessage) && it.conversationId !in ids }
            val op = OfflineListReconcile.Op(0L, OfflineListReconcile.Phase.CACHED, page + unsent)
            OfflineListReconcile.reconcile(emptyList(), 0L, op, spec, hadMore = true).rows
        }
    }

    /** Applies one phase; returns the cached keys a later server phase may prune. */
    private fun apply(
        viewModel: CometChatConversationsViewModel,
        gen: Long,
        phase: OfflineListReconcile.Phase,
        rows: List<Conversation>,
        cachedKeys: Set<Any>,
        spec: OfflineListReconcile.Spec<Conversation, String>
    ): Set<Any> {
        var keys: Set<Any> = emptySet()
        viewModel._conversations.update { current ->
            // A row showing an own unsent message is never proven gone: a brand-new chat with only
            // an unsent message is unknown to the server (BEH-CONV-06).
            val prunable = cachedKeys - current.filter { isOwnUnsent(it.lastMessage) }.map { it.conversationId }.toSet()
            val op = OfflineListReconcile.Op(gen, phase, protectPreviews(current, distinct(rows)), prunable)
            val outcome = OfflineListReconcile.reconcile(current, viewModel.offlineListGen, op, spec, viewModel.hasMoreData)
            keys = outcome.cachedKeys
            outcome.rows
        }
        return keys
    }

    /**
     * A read ended with no row to show. Offline: a `searchKeyword` list shows "Connect to search"
     * with no `onError` (D84 rule 7); any other list is the Error state with [connectToLoad] set, so
     * the kit shows its offline state ("Connect to load chats", the app's error view if set) and
     * `onError` fires once (D84 rule 3). Any other failure is the error view (D84 rule 4).
     * A refresh that failed offline is read again on the next connect (D84 rule 8); a first or
     * load-more page that failed offline already is (D75, [State.pageFailedOffline]).
     */
    private fun showNothingRead(
        viewModel: CometChatConversationsViewModel,
        request: ConversationsRequest,
        refresh: Boolean,
        exception: CometChatException
    ) {
        val notConnected = OfflinePagedMerge.isNotConnected(exception)
        if (refresh && notConnected) {
            viewModel.offlineConversationState.refreshFailedOffline = true
        }
        val search = notConnected && !request.searchKeyword.isNullOrEmpty()
        viewModel._connectToSearch.value = search
        viewModel._connectToLoad.value = notConnected && !search
        viewModel._uiState.value = if (search) UIState.Empty else UIState.Error(exception)
    }

    private fun publish(viewModel: CometChatConversationsViewModel, serverDone: Boolean) {
        viewModel._connectToSearch.value = false
        viewModel._connectToLoad.value = false
        if (viewModel._conversations.value.isNotEmpty()) viewModel.offlineConversationState.refreshFailedOffline = false
        val list = viewModel._conversations.value
        viewModel._uiState.value = when {
            list.isNotEmpty() -> UIState.Content(list)
            serverDone -> UIState.Empty
            else -> UIState.Loading
        }
    }

    // ------------------------------------------------------------------ live + reconnect (G7)

    /**
     * Hook from `checkAndUpdateConversation`. A live message runs today's path (its +1 included).
     * A swept message (`source == SYNC`, BEH-SYNC-12) moves the preview and order only: no unread
     * +1, no sound, no delivery receipt, and never over a newer preview (BEH-CONV-06).
     * @return true when handled.
     */
    fun onMessageListenerEvent(
        viewModel: CometChatConversationsViewModel,
        message: BaseMessage,
        markAsDeliver: Boolean
    ): Boolean {
        if (message.source != MessageSource.SYNC) {
            // A local edit/delete/sent event for an older message never replaces an unsent preview.
            return !markAsDeliver && olderThanUnsentPreview(viewModel, message)
        }
        if (viewModel.isThreadedMessage(message) &&
            !CometChatUIKit.getConversationUpdateSettings().shouldUpdateOnMessageReplies()
        ) return true
        if (message is CustomMessage && !viewModel.shouldUpdateConversationForCustomMessage(message)) return true
        val conversation = conversationOf(message) ?: return true
        val shown = viewModel._conversations.value
            .firstOrNull { it.conversationId == conversation.conversationId }?.lastMessage
        if (shown != null && isOlderThan(message, shown)) return true
        // isActionMessage = true is updateConversation's "keep the unread count" branch; a swept
        // row is not a live write (BEH-SYNC-15).
        viewModel.updateConversation(conversation, isActionMessage = true, live = false)
        return true
    }

    /**
     * Hook from the SDK's `onMessageEdited` / `onMessageDeleted` / `onMessageModerated`: an older
     * message's update never replaces an own unsent preview (BEH-CONV-06). @return true when handled.
     */
    fun onMessageUpdated(viewModel: CometChatConversationsViewModel, message: BaseMessage): Boolean =
        olderThanUnsentPreview(viewModel, message)

    /**
     * Hook from the UI `MessageSent` SUCCESS event: a late send ack older than the shown preview (a
     * newer message from the other person, or a newer own unsent one) never replaces it
     * (BEH-CONV-06). @return true when handled (skipped).
     */
    fun onMessageSent(viewModel: CometChatConversationsViewModel, message: BaseMessage): Boolean {
        val shown = shownPreview(viewModel, message) ?: return false
        return isOlderThan(message, shown)
    }

    /**
     * Hook from the ViewModel's live row writes (`updateConversation`, `updateConversationInList`):
     * records [conversationId] as written after the sweep was requested (see [State.touchedLive]).
     * Swept rows are written with `live = false` and never recorded.
     */
    fun onLiveWrite(viewModel: CometChatConversationsViewModel, conversationId: String?) {
        conversationId?.let { viewModel.offlineConversationState.touchedLive.add(it) }
    }

    /**
     * Hook for the UI `MessageRead` event (ENG-41643, D-369). Every call takes a new read token for
     * the conversation, so the server answer of an earlier read is dropped (a later read wins).
     *
     * Only the read the offline flush sent ([OfflineReadLedger.isFlushed]) can be refreshed; every
     * other read zeroes the row as today. When the flushed read is older than the row's last message
     * (main conversation only) and that message was sent by someone else, the server still counts
     * those later messages as unread, so the row must not go to 0: it takes the server's
     * `unreadMessageCount` instead. One server read, never the cached
     * phase (the SDK zeroed its cached count on this read, BEH-CONV-04; BEH-READ-28). Only the count
     * is applied: the row keeps its place and its last message. The answer is dropped when a later
     * read or refresh of the same conversation, a session change or a cache reset came first, or the
     * row is gone. A failure keeps the count and is logged only.
     *
     * @return true when the refresh was started (the row is not zeroed); false when today's zeroing
     *   must run.
     */
    fun onMessageRead(viewModel: CometChatConversationsViewModel, message: BaseMessage): Boolean {
        val id = message.conversationId ?: return false
        val state = viewModel.offlineConversationState
        val token = ++state.readRefreshSeq
        state.readRefreshTokens[id] = token

        if (message.parentMessageId > 0) return false
        if (!OfflineReadLedger.isFlushed(message)) return false
        val row = viewModel._conversations.value.firstOrNull { it.conversationId == id } ?: return false
        val last = row.lastMessage ?: return false
        if (last.id <= message.id) return false
        val me = viewModel.getLoggedInUserSafe()?.uid
        val lastIsMine = me != null && last.sender?.uid?.equals(me, ignoreCase = true) == true
        if (lastIsMine) return false
        val withId = when (val entity = row.conversationWith) {
            is User -> entity.uid
            is Group -> entity.guid
            else -> null
        } ?: return false
        val type = row.conversationType ?: return false

        val listGen = viewModel.offlineListGen
        val hubGen = OfflineHub.gen
        val started = runCatching {
            CometChat.getConversation(withId, type, ConversationDetailsOnce(cachedFallback = false) { result ->
                result
                    .onSuccess { server ->
                        val current = state.readRefreshTokens[id] == token &&
                            listGen == viewModel.offlineListGen && hubGen == OfflineHub.gen
                        val shown = viewModel._conversations.value.firstOrNull { it.conversationId == id }
                        if (!current || shown == null) {
                            CometChatLogger.d(TAG, "Read refresh for $id dropped (stale or gone)")
                            return@onSuccess
                        }
                        viewModel.updateConversationUnreadCount(shown, server.unreadMessageCount)
                    }
                    .onFailure { e -> CometChatLogger.e(TAG, "Read refresh for $id failed: ${e.message}") }
            })
        }
        started.exceptionOrNull()?.let { CometChatLogger.e(TAG, "Read refresh for $id not started: ${it.message}") }
        return true
    }

    private fun olderThanUnsentPreview(viewModel: CometChatConversationsViewModel, message: BaseMessage): Boolean {
        val shown = shownPreview(viewModel, message) ?: return false
        return isOwnUnsent(shown) && isOlderThan(message, shown)
    }

    private fun shownPreview(viewModel: CometChatConversationsViewModel, message: BaseMessage): BaseMessage? {
        val id = message.conversationId ?: conversationOf(message)?.conversationId ?: return null
        return viewModel._conversations.value.firstOrNull { it.conversationId == id }?.lastMessage
    }

    /**
     * Hook from the connection listener's `onConnected`: no list refresh; the sweep is consumed
     * (G7). The exceptions: a load-more page that failed offline is read again, once (D75); a
     * refresh that failed offline with nothing on screen is read again, once (D83 rule 3).
     */
    fun onConnected(viewModel: CometChatConversationsViewModel): Boolean {
        val state = viewModel.offlineConversationState
        // The sweep starts on connect: only rows written from now on are newer than its page.
        state.touchedLive.clear()
        if (state.pageFailedOffline && !viewModel.isFetching) {
            state.pageFailedOffline = false
            state.refreshFailedOffline = false
            retryFailedPage(viewModel, MAX_RETRY_PAGES)
        } else if (state.refreshFailedOffline && !viewModel.isFetching) {
            // D83 rule 3: a refresh that failed offline left the error state on screen.
            state.refreshFailedOffline = false
            if (viewModel._conversations.value.isEmpty()) refreshList(viewModel, quiet = true)
        }
        return true
    }

    /**
     * Reads the list's next page (the one that failed; the request kept its cursor). While a page
     * adds no new chat and more pages exist, reads the next one, up to [pagesLeft] reads in all. Stops
     * when a newer read, a generation bump or another offline failure takes over.
     */
    private fun retryFailedPage(viewModel: CometChatConversationsViewModel, pagesLeft: Int) {
        val repository = viewModel.twoPhaseRepository ?: return
        val request = viewModel.conversationsRequest ?: return
        if (pagesLeft <= 0 || viewModel.isFetching || !viewModel.hasMoreData) return
        val state = viewModel.offlineConversationState
        val shown = viewModel._conversations.value.mapTo(HashSet()) { it.conversationId }
        val gen = viewModel.offlineListGen
        val serverPhases = state.serverPhases
        // read() takes the next token synchronously; its onDone may run before read() returns.
        val token = state.readToken + 1
        read(viewModel, repository, request, refresh = false, quiet = true) {
            val answered = state.serverPhases > serverPhases
            val added = viewModel._conversations.value.any { it.conversationId !in shown }
            val current = token == state.readToken && gen == viewModel.offlineListGen &&
                viewModel.conversationsRequest === request
            if (answered && !added && current) retryFailedPage(viewModel, pagesLeft - 1)
        }
    }

    /** Every current-gen hub event delivered to the conversation list. */
    fun onHubEvent(viewModel: CometChatConversationsViewModel, event: OfflineHubEvent) {
        when (event) {
            is OfflineHubEvent.ConversationSyncCompleted -> onConversationSync(viewModel, event.conversations)
            is OfflineHubEvent.SendState -> onSendState(viewModel, event)
            is OfflineHubEvent.MessageSyncCompleted -> onMessageSync(viewModel, event.messages)
            is OfflineHubEvent.CacheReset -> onCacheReset(viewModel)
            is OfflineHubEvent.SessionEnded -> {
                // The ViewModel already cleared the rows; the next read starts from page 1.
                viewModel.offlineConversationState.clear()
                viewModel.conversationsRequest = null
            }
            // ConversationSyncFailed: nothing, the next sweep heals. The rest belong to the message list.
            else -> Unit
        }
    }

    /**
     * The sweep's conversation page (BEH-SYNC-08/30). The plain list reconciles only the slice the
     * page covers; any other list re-runs its own request two-phase, once.
     */
    private fun onConversationSync(viewModel: CometChatConversationsViewModel, page: List<Conversation>) {
        if (modeOf(requestOf(viewModel)) != ListMode.PLAIN) {
            resync(viewModel)
            return
        }
        if (page.isEmpty()) return
        val spec = specOf(floatPins = true)
        val complete = isCompletePage(page.size)
        val state = viewModel.offlineConversationState
        val spared = HashSet(state.touchedLive)
        viewModel._conversations.update { current ->
            sliceReconcile(current, protectPreviews(current, distinct(page)), spec, complete, spared)
        }
        state.touchedLive.clear()
        publish(viewModel, serverDone = true)
    }

    /**
     * Test seam for the sweep's page size; null in production, where it is the configured
     * `maxConversations` (the sweep fetches one page of that size, BEH-SYNC-08).
     */
    @VisibleForTesting
    internal var sweepPageSizeOverride: Int? = null

    /**
     * A sweep page shorter than the page size it asked for is the complete list (BEH-SYNC-15
     * floor 0; BEH-CPIN-05 "the page is short"). An unknown page size proves nothing.
     */
    private fun isCompletePage(size: Int): Boolean {
        val pageSize = sweepPageSizeOverride
            ?: runCatching { CometChatUIKit.getAuthSettings()?.offlineConfig?.maxConversations }.getOrNull()
            ?: return false
        return size < pageSize
    }

    /**
     * BEH-SYNC-15 / BEH-CPIN-05, ranked by the conversation's own `updatedAt`. Rows in [page] are
     * upserted (newer copy wins) in sorted place. A shown row the page lacks, other than an own
     * unsent preview or a row a live event wrote since the sweep was requested ([spared], left as
     * it is):
     * - [complete] page: removed (absence proves deletion).
     * - pinned: unpinned when the page proves the pinned block complete (it has an unpinned row),
     *   else kept; never removed.
     * - unpinned: removed only when its `updatedAt` is above the floor, the oldest `updatedAt`
     *   among the page's unpinned rows. A page with no ranked unpinned row removes nothing.
     * Pure.
     */
    internal fun sliceReconcile(
        current: List<Conversation>,
        page: List<Conversation>,
        spec: OfflineListReconcile.Spec<Conversation, String>,
        complete: Boolean,
        spared: Set<String> = emptySet()
    ): List<Conversation> {
        if (page.isEmpty()) return current
        val pageIds = page.mapTo(HashSet()) { it.conversationId }
        // The pinned block is not ranked; a row without updatedAt never lowers the floor.
        val floor = page.filter { !it.isPinned && it.updatedAt > 0L }.minOfOrNull { it.updatedAt } ?: Long.MAX_VALUE
        val pinsComplete = page.any { !it.isPinned }
        val kept = current.mapNotNull { row ->
            when {
                row.conversationId in pageIds || isOwnUnsent(row.lastMessage) -> row
                // Written by a live event after the sweep was requested (BEH-SYNC-15).
                row.conversationId in spared -> row
                complete -> null
                row.isPinned -> if (pinsComplete) unpinned(row) else row
                row.updatedAt > floor -> null
                else -> row
            }
        }
        // The CACHED phase is upsert + sort with no pruning and no change to pagination.
        val op = OfflineListReconcile.Op(0L, OfflineListReconcile.Phase.CACHED, page)
        return OfflineListReconcile.reconcile(kept, 0L, op, spec, hadMore = true).rows
    }

    private fun unpinned(row: Conversation): Conversation =
        row.clone().apply {
            pinnedAt = 0L
            pinnedBy = null
        }

    private fun resync(viewModel: CometChatConversationsViewModel) {
        val state = viewModel.offlineConversationState
        if (state.resyncInFlight) return
        val repository = viewModel.twoPhaseRepository
        if (repository == null) {
            viewModel.refreshList()
            return
        }
        state.resyncInFlight = true
        viewModel.hasMoreData = true
        read(viewModel, repository, builderOf(viewModel).build(), refresh = true, quiet = true) { state.resyncInFlight = false }
    }

    /**
     * B2: a self kick, ban or leave in the sweep drops the group row as the live event does, unless
     * the same batch re-adds the user later, or the row already shows something newer than the action.
     */
    private fun onMessageSync(viewModel: CometChatConversationsViewModel, messages: List<BaseMessage>) {
        val me = viewModel.getLoggedInUserSafe()?.uid ?: return
        messages.forEachIndexed { index, message ->
            val action = message as? Action ?: return@forEachIndexed
            if (!isGroupAction(action) || !isSelfRemoval(action, me)) return@forEachIndexed
            val guid = action.receiverUid ?: return@forEachIndexed
            val readded = messages.withIndex().any { (other, m) ->
                m is Action && isGroupAction(m) && asciiLower(m.receiverUid.orEmpty()) == asciiLower(guid) &&
                    isSelfReadd(m, me) && (m.sentAt > action.sentAt || (m.sentAt == action.sentAt && other > index))
            }
            if (readded) return@forEachIndexed
            val row = viewModel._conversations.value.firstOrNull {
                CometChatConstants.CONVERSATION_TYPE_GROUP.equals(it.conversationType, ignoreCase = true) &&
                    asciiLower(withOf(it).orEmpty()) == asciiLower(guid)
            }
            val shown = row?.lastMessage
            if (shown != null && shown.id != action.id && isOlderThan(action, shown)) return@forEachIndexed
            viewModel.updateConversationForGroupAction(action, isRemove = true)
        }
    }

    private fun isGroupAction(action: Action): Boolean =
        CometChatConstants.RECEIVER_TYPE_GROUP.equals(action.receiverType, ignoreCase = true)

    /** Same actor fields and case-insensitive match as the SDK's live GroupListener dispatch. */
    private fun isSelfRemoval(action: Action, me: String): Boolean {
        val removed = when (action.action?.let { asciiLower(it) }) {
            CometChatConstants.ActionKeys.ACTION_KICKED,
            CometChatConstants.ActionKeys.ACTION_BANNED -> action.actionOn as? User
            CometChatConstants.ActionKeys.ACTION_LEFT -> action.actionBy as? User
            else -> null
        } ?: return false
        return removed.uid.equals(me, ignoreCase = true)
    }

    /** The user is back in the group: added by someone, or joined. */
    private fun isSelfReadd(action: Action, me: String): Boolean {
        val back = when (action.action?.let { asciiLower(it) }) {
            CometChatConstants.ActionKeys.ACTION_MEMBER_ADDED -> action.actionOn as? User
            CometChatConstants.ActionKeys.ACTION_JOINED -> action.actionBy as? User
            else -> null
        } ?: return false
        return back.uid.equals(me, ignoreCase = true)
    }

    /** G8: clear, then reload two-phase. W5 posts the notice. */
    private fun onCacheReset(viewModel: CometChatConversationsViewModel) {
        viewModel.clearForOfflineSession()
        viewModel.offlineConversationState.clear()
        viewModel.conversationsRequest = null
        // The kit starts this reload: quiet if it fails over rows (D84 rule 5).
        if (!fetchConversations(viewModel, quiet = true)) viewModel.fetchConversations()
    }

    // ------------------------------------------------------------------ own unsent preview (BEH-CONV-01)

    private fun onSendState(viewModel: CometChatConversationsViewModel, event: OfflineHubEvent.SendState) {
        val localId = event.localMessageId.ifEmpty { event.message.localMessageId.orEmpty() }
        if (localId.isEmpty()) return
        when {
            event.state == MessageSendState.SENT -> onSent(viewModel, localId, event.message)
            event.state == MessageSendState.FAILED &&
                (event.reason == MessageFailureReason.DISCARDED || event.reason == MessageFailureReason.QUEUE_OVERFLOW) ->
                onRemoved(viewModel, localId)
            else -> placePending(
                viewModel, localId, event.message,
                if (event.state == MessageSendState.FAILED) MessageStatus.FAILED else MessageStatus.PENDING
            )
        }
    }

    /**
     * The chat moves to the top with the own unsent message as its preview. The mark is carried on
     * the preview's own copy of the message as its `status` (5.1.0's preview carries none, D19).
     */
    private fun placePending(
        viewModel: CometChatConversationsViewModel,
        localId: String,
        message: BaseMessage,
        mark: MessageStatus
    ) {
        if (viewModel.isThreadedMessage(message) &&
            !CometChatUIKit.getConversationUpdateSettings().shouldUpdateOnMessageReplies()
        ) return
        if (message is CustomMessage && !viewModel.shouldUpdateConversationForCustomMessage(message)) return
        val conversation = conversationOf(message) ?: return
        val state = viewModel.offlineConversationState
        // `status` is not part of Conversation/BaseMessage content equality, so a mark change alone
        // would be conflated away by the StateFlow and never redrawn. Each mark therefore also
        // steps this UI-only copy's `updatedAt` by one (an unsent message has no server updatedAt;
        // the copy never leaves the list, and a step of a few units sits far below any `sentAt`).
        val version = (state.markVersion[localId] ?: 0L) + 1L
        state.markVersion[localId] = version
        val preview = message.clone().apply {
            status = mark
            updatedAt = message.updatedAt + version
        }
        conversation.lastMessage = preview
        val id = conversation.conversationId
        val row = viewModel._conversations.value.firstOrNull { it.conversationId == id }
        val shown = row?.lastMessage

        if (shown != null && isOwnUnsent(shown) && shown.localMessageId == localId) {
            // The same send changing mark (sending ⇄ failed): in place.
            viewModel._conversations.update { list ->
                list.map {
                    if (it.conversationId == id && it.lastMessage?.localMessageId == localId && isOwnUnsent(it.lastMessage)) {
                        it.clone().apply { lastMessage = preview }
                    } else it
                }
            }
            publish(viewModel, serverDone = true)
            return
        }
        // A row already showing a newer message (sent or another unsent one) is not demoted by an
        // older send's state change.
        if (shown != null && preview.sentAt > 0L && preview.sentAt < shown.sentAt) return
        if (!state.beforePending.containsKey(localId)) state.beforePending[localId] = row
        viewModel.updateConversation(conversation, isActionMessage = false)
    }

    /** The send was acked: its preview becomes the server message. */
    private fun onSent(viewModel: CometChatConversationsViewModel, localId: String, acked: BaseMessage) {
        val state = viewModel.offlineConversationState
        state.beforePending.remove(localId)
        state.markVersion.remove(localId)
        // A newer send that replaced this one's preview must restore the acked message, never this
        // send's stale "sending" copy.
        repointBefore(state, localId) { row -> row.clone().apply { lastMessage = acked } }
        val showsIt = viewModel._conversations.value.any {
            isOwnUnsent(it.lastMessage) && it.lastMessage?.localMessageId == localId
        }
        if (!showsIt) return
        val conversation = conversationOf(acked) ?: return
        viewModel.updateConversation(conversation, isActionMessage = false)
    }

    /** The send was removed (discarded or evicted): restore what the row showed before it. */
    private fun onRemoved(viewModel: CometChatConversationsViewModel, localId: String) {
        val state = viewModel.offlineConversationState
        state.markVersion.remove(localId)
        if (!state.beforePending.containsKey(localId)) return
        val before = state.beforePending.remove(localId)
        // A newer send that replaced this one's preview now restores what this one replaced.
        repointBefore(state, localId) { before }
        viewModel._conversations.update { list ->
            val index = list.indexOfFirst { isOwnUnsent(it.lastMessage) && it.lastMessage?.localMessageId == localId }
            when {
                index < 0 -> list
                before == null -> list.filterIndexed { i, _ -> i != index }
                else -> list.toMutableList().apply { set(index, list[index].clone().apply { lastMessage = before.lastMessage }) }
            }
        }
        publish(viewModel, serverDone = true)
    }

    /** Re-points every saved row that shows send [localId]'s unsent preview. */
    private fun repointBefore(state: State, localId: String, replace: (Conversation) -> Conversation?) {
        for (entry in state.beforePending.entries) {
            val saved = entry.value ?: continue
            val shown = saved.lastMessage
            if (isOwnUnsent(shown) && shown?.localMessageId == localId) entry.setValue(replace(saved))
        }
    }

    // ------------------------------------------------------------------ pins (G11, UIK-CONV-01)

    /**
     * Own pin/unpin: the row updates from `onSuccess` (BEH-FLAG-04, G11). SDK 5.1.2 sends no own
     * echo, so no echo is expected or recorded (D-328); `onSuccess` is a no-op when the row already
     * shows the state, e.g. after an SDK 5.1.0 echo that arrived first.
     */
    fun setPinned(
        viewModel: CometChatConversationsViewModel,
        conversation: Conversation,
        with: String,
        pinned: Boolean,
        onSuccess: (() -> Unit)?,
        onError: ((CometChatException?) -> Unit)?
    ) {
        val type = conversation.conversationType
        val listener = object : CometChat.CallbackListener<Conversation>() {
            override fun onSuccess(updated: Conversation) {
                val row = viewModel._conversations.value.firstOrNull { matches(it, type, with) }
                if (row == null || !showsPinState(row, updated, pinned)) applyPin(viewModel, type, with, updated, pinned)
                onSuccess?.invoke()
            }

            override fun onError(e: CometChatException?) {
                onError?.invoke(e)
            }
        }
        if (pinned) CometChat.pinConversation(with, type, listener) else CometChat.unpinConversation(with, type, listener)
    }

    /**
     * A `ConversationListener` pin/unpin event. Matched by (type, with), ASCII case-insensitive; a
     * 1:1 event whose peer is the user is ignored; an event whose state the row already shows is a
     * no-op (BEH-CPIN-09), which also covers an SDK 5.1.0 echo of an own call. Every other event
     * applies, including one that arrives while an own call is in flight (D-328).
     */
    fun onPinEvent(viewModel: CometChatConversationsViewModel, event: Conversation, pinned: Boolean) {
        val type = event.conversationType
        val with = withOf(event) ?: return
        if (CometChatConstants.RECEIVER_TYPE_USER.equals(type, ignoreCase = true)) {
            val me = viewModel.getLoggedInUserSafe()?.uid
            if (me != null && asciiLower(me) == asciiLower(with)) return
        }
        if (type.isNullOrEmpty() || with.isEmpty()) return
        val row = viewModel._conversations.value.firstOrNull { matches(it, type, with) }
        if (row != null && showsPinState(row, event, pinned)) return
        applyPin(viewModel, type, with, event, pinned)
    }

    private fun showsPinState(row: Conversation, event: Conversation, pinned: Boolean): Boolean =
        if (pinned) row.isPinned && (event.pinnedBy == null || event.pinnedBy == row.pinnedBy) else !row.isPinned

    /**
     * Places a pin state. The plain and agentic lists move the row as today (pin → top, unpin → top
     * of the unpinned block); the plain list inserts an unknown pinned chat as today. Filtered and
     * `pinnedBy` lists only update a row they show, in place.
     */
    private fun applyPin(
        viewModel: CometChatConversationsViewModel,
        type: String?,
        with: String,
        source: Conversation,
        pinned: Boolean
    ) {
        val mode = modeOf(requestOf(viewModel))
        onLiveWrite(viewModel, viewModel._conversations.value.firstOrNull { matches(it, type, with) }?.conversationId)
        onLiveWrite(viewModel, source.conversationId)
        viewModel._conversations.update { list ->
            val index = list.indexOfFirst { matches(it, type, with) }
            when {
                index < 0 ->
                    if (pinned && mode == ListMode.PLAIN) listOf(source) + list else list

                mode == ListMode.FIXED ->
                    list.toMutableList().apply { set(index, withPinState(list[index], source)) }

                else -> list.toMutableList().apply {
                    val row = withPinState(removeAt(index), source)
                    add(if (pinned) 0 else viewModel.firstUnpinnedIndex(this), row)
                }
            }
        }
        publish(viewModel, serverDone = true)
    }

    /** Copies only the pin fields, so fields the pin endpoint omits are not lost (as today). */
    private fun withPinState(existing: Conversation, source: Conversation): Conversation =
        existing.clone().apply {
            pinnedAt = source.pinnedAt
            pinnedBy = source.pinnedBy
        }

    private fun matches(row: Conversation, type: String?, with: String): Boolean {
        if (!asciiLower(row.conversationType.orEmpty()).equals(asciiLower(type.orEmpty()))) return false
        val rowWith = withOf(row) ?: return false
        return asciiLower(rowWith) == asciiLower(with)
    }

    private fun withOf(conversation: Conversation): String? = when (val entity = conversation.conversationWith) {
        is User -> entity.uid
        is Group -> entity.guid
        else -> null
    }

    /** ASCII-only lowercase (BEH-CPIN-09); never locale or Unicode folding. */
    private fun asciiLower(value: String): String {
        val chars = value.toCharArray()
        for (i in chars.indices) if (chars[i] in 'A'..'Z') chars[i] = chars[i] + 32
        return String(chars)
    }

    // ------------------------------------------------------------------ shared rules

    /** The request this list reads with: the live one, else what its builder will build. */
    private fun requestOf(viewModel: CometChatConversationsViewModel): ConversationsRequest? =
        viewModel.conversationsRequest ?: viewModel.conversationsRequestBuilder?.build()

    /** The list's mode from its request (BEH-CPIN-02/03/09). A null request is the default plain list. */
    internal fun modeOf(request: ConversationsRequest?): ListMode = when {
        request == null -> ListMode.PLAIN
        narrows(request) || !request.pinnedBy.isNullOrEmpty() -> ListMode.FIXED
        request.isHideAgentic || request.isOnlyAgentic -> ListMode.AGENTIC
        else -> ListMode.PLAIN
    }

    /** Pins float unless a type, tag, search or unread filter turns floating off (BEH-CPIN-02). */
    private fun floatsPins(request: ConversationsRequest?): Boolean = request == null || !narrows(request)

    private fun narrows(request: ConversationsRequest): Boolean {
        val type = request.conversationType
        return (!type.isNullOrEmpty() && !type.equals(UIKitConstants.ConversationType.BOTH, ignoreCase = true)) ||
            !request.tags.isNullOrEmpty() ||
            !request.userTags.isNullOrEmpty() ||
            !request.groupTags.isNullOrEmpty() ||
            !request.searchKeyword.isNullOrEmpty() ||
            request.isUnread
    }

    /** Newest-first by last-message time (BEH-CONV-05); floating pins first, admin above own. */
    internal fun specOf(floatPins: Boolean): OfflineListReconcile.Spec<Conversation, String> =
        OfflineListReconcile.Spec(
            keyOf = { it.conversationId ?: "" },
            serverIdOf = { it.conversationId },
            sortAtOf = { sortAt(it, floatPins) },
            updatedAtOf = { freshness(it) },
            descending = true
        )

    private fun sortAt(conversation: Conversation, floatPins: Boolean): Long {
        if (floatPins && conversation.isPinned) {
            return (if (conversation.isSystemPinned) ADMIN_PIN_TIER else OWN_PIN_TIER) + conversation.pinnedAt
        }
        val sentAt = conversation.lastMessage?.sentAt ?: 0L
        return if (sentAt > 0L) sentAt else conversation.updatedAt
    }

    /** The LWW clock for two copies of one row: the newest of the row's and its preview's times. */
    private fun freshness(conversation: Conversation): Long {
        val message = conversation.lastMessage
        return maxOf(
            conversation.updatedAt,
            maxOf(message?.sentAt ?: 0L, message?.updatedAt ?: 0L),
            maxOf(message?.editedAt ?: 0L, message?.deletedAt ?: 0L)
        )
    }

    /** An own message the server has not acked: no server id, but a localMessageId. */
    internal fun isOwnUnsent(message: BaseMessage?): Boolean =
        message != null && message.id == 0L && !message.localMessageId.isNullOrEmpty()

    /**
     * BEH-CONV-06: a server row never replaces a newer own unsent preview. The server row is kept,
     * with the shown unsent message as its preview, unless the server's message is the ack of that
     * send or is newer than it.
     */
    private fun protectPreviews(current: List<Conversation>, incoming: List<Conversation>): List<Conversation> {
        val unsent = current.filter { isOwnUnsent(it.lastMessage) }.associateBy { it.conversationId }
        if (unsent.isEmpty()) return incoming
        return incoming.map { row ->
            val pending = unsent[row.conversationId]?.lastMessage ?: return@map row
            val server = row.lastMessage
            if (server != null && (isAckOf(server, pending) || (pending.sentAt > 0L && server.sentAt > pending.sentAt))) {
                row
            } else {
                row.clone().apply { lastMessage = pending }
            }
        }
    }

    private fun isAckOf(server: BaseMessage, pending: BaseMessage): Boolean =
        server.id > 0L && (
            (!pending.muid.isNullOrEmpty() && pending.muid == server.muid) ||
                (!pending.localMessageId.isNullOrEmpty() && pending.localMessageId == server.localMessageId)
            )

    /** True when [message] is older than the preview [shown] (advance-only, BEH-CONV-06). */
    private fun isOlderThan(message: BaseMessage, shown: BaseMessage): Boolean = when {
        isOwnUnsent(shown) -> isAckOf(message, shown).not() && (shown.sentAt == 0L || message.sentAt <= shown.sentAt)
        message.id != 0L && message.id == shown.id -> false
        message.sentAt != shown.sentAt -> message.sentAt < shown.sentAt
        else -> message.id != 0L && message.id < shown.id
    }

    private fun conversationOf(message: BaseMessage): Conversation? = try {
        CometChatHelper.getConversationFromMessage(message)
    } catch (e: Exception) {
        CometChatLogger.e(TAG, "No conversation for message: ${e.message}")
        null
    }

    private fun distinct(rows: List<Conversation>): List<Conversation> = rows.distinctBy { it.conversationId }
}
