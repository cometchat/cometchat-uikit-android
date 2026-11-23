package com.cometchat.uikit.core.offline

import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.domain.repository.MessageListTwoPhaseRepository
import com.cometchat.uikit.core.offline.OfflineListReconcile.Op
import com.cometchat.uikit.core.offline.OfflineListReconcile.Phase
import com.cometchat.uikit.core.offline.OfflineListReconcile.Spec
import com.cometchat.uikit.core.state.MessageListUIState
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch

/**
 * Per-list state of the offline reads (W2). Lives on the message-list ViewModel; the UI reads the
 * flows through the ViewModel's `offline*` members.
 */
internal class MessageListOfflineReadState {
    /** "Connect to load more" at the older edge: an older page failed offline while rows are shown. */
    val olderBlocked = MutableStateFlow(false)

    /** "Connect to load more" at the newer edge (after a jump): a newer page failed offline. */
    val newerBlocked = MutableStateFlow(false)

    /** A newest-window refresh changed the newest end; the UI scrolls if at the bottom, else shows a pill. */
    val jumpToLatest = MutableStateFlow(false)

    /**
     * D84 rule 5: a window read failed online while rows stay on screen (no error view). The UI passes it to the
     * app's `onError`; with nothing shown the Error state carries the error instead. Queued until
     * the UI collects (an open can fail before the view binds) and delivered exactly once: each
     * error goes to one collector, so a re-bound view or a recomposition never repeats it.
     */
    val readErrorQueue = Channel<CometChatException>(capacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** [readErrorQueue] as the flow the ViewModel exposes. */
    val readError: Flow<CometChatException> = readErrorQueue.receiveAsFlow()

    internal var readGen: Long = 0L
    internal var genKey: Any? = null

    /** The read gen of an open whose goto / start-from-unread jump is in flight (D60); -1 for none. */
    internal var openJumpGen: Long = -1L

    /**
     * D83: the read gen of an open whose newest read already reported a failure to `onError` with
     * rows shown; its opening jump then does not report again (one `onError` per open). -1 for none.
     */
    internal var reportedOpenGen: Long = -1L

    /**
     * D83 amended 2026-11-12: the next [MessageListOfflineReads.fetchOlder] is a read the kit
     * started by itself (the reload after a cache reset), so a failure while rows are on screen
     * reports nothing. Set only around that call, which reads it synchronously.
     */
    internal var quietNextRead: Boolean = false

    /**
     * The read gen of a jump whose window is not placed yet; -1 for none. While it is set, an older
     * or newer page waits for the jumped window instead of reading from the window it replaces.
     */
    internal var jumpGen: Long = -1L

    /**
     * Owner of the `inProgress` flag: the token of the read that set it. A read clears the flag only
     * while it still owns it, so a stale read ending late never releases a newer read's flag.
     */
    internal var progressToken: Long = 0L

    /** The read gen a jump started; the window it names is the jump's. -1 for none. */
    internal var jumpWindowGen: Long = -1L

    /**
     * The read gen whose window last positioned the shared older-page cursor (a jump's rebuild, a
     * newest refresh's reset, `clear()`). A stale older read already advanced that cursor; when no
     * newer window has repositioned it, the read puts it back on the oldest row shown.
     */
    internal var cursorGen: Long = 0L

    /**
     * After an offline jump (D60): the newest row known to join the target, from which newer pages
     * are read instead of the newest row shown (a cached half may have skipped a gap). Advances with
     * each server page and clears once a page reaches the newest row shown. Null otherwise.
     */
    internal var newerFrontier: Long? = null
}

/**
 * What the reads need from the message-list ViewModel. The ViewModel implements it over its own
 * private state, so this file holds the logic once and tests drive it with a fake.
 */
internal interface MessageListReadHost {
    val scope: CoroutineScope
    /** The ViewModel's `offlineListGen` (bumped on a session change). */
    val listGen: Long
    /** Identity of the chat shown (receiver + thread); a change is a chat switch. */
    val chatKey: Any?
    val repository: MessageListRepository
    val twoPhase: MessageListTwoPhaseRepository?
    /** Cache-allowed reads for an opening jump (D60); null for a custom repository. */
    val anchored: MessageListAnchoredReads? get() = null
    val aliases: AliasMap
    val readState: MessageListOfflineReadState

    var rows: List<BaseMessage>
    var uiState: MessageListUIState
    var hasMorePrevious: Boolean
    var hasMoreNew: Boolean
    var inProgress: Boolean
    suspend fun awaitIdle()

    val gotoMessageId: Long
    val startFromUnread: Boolean
    val lastReadMessageId: Long
    /** (receiverId, receiverType) or null when no user/group is set. */
    fun conversationTarget(): Pair<String, String>?
    /** Today's conversation bookkeeping: unread count, latestMessageId, lastReadMessageId. */
    fun applyConversation(conversation: Conversation)
    fun checkConversationSummary(unreadCount: Int)
    /** Routes through the ViewModel's `goToMessage`, so the jump hook runs. */
    fun goToMessage(messageId: Long, highlight: Boolean)
    fun setLatestMessageId(messageId: Long)
    /**
     * Today's first-fetch side effects (agent parent, mark read, starters, scroll to bottom); once.
     * @param scrollToBottom false when another window (a jump) owns the landing: the bookkeeping
     * runs, the scroll does not.
     */
    suspend fun completeFirstFetch(scrollToBottom: Boolean = true)
    /** Today's after-newer-page step: mark read when a first unread exists. */
    fun afterNewerPage()
    /** Today's unread-anchor detection after a jump window is placed. */
    fun markUnreadAnchor()
    fun scrollToMessage(messageId: Long, highlight: Boolean)
}

/**
 * Seam owned by **W2** (ENG-41087): two-phase message reads (main and thread), the windowed
 * reconcile, and open-newest-first (G2; BEH-READ-01..09b, BEH-READ-13, BEH-READ-29/30/31, R41).
 *
 * Reached only on the ON path (the ViewModel calls each hook behind its captured `offlineOn`) and
 * only when the repository offers [MessageListTwoPhaseRepository]; otherwise each hook returns
 * false and today's single-phase path runs.
 *
 * - Every read paints the cached phase first (it hides the full-screen loader) and reconciles the
 *   server phase in place with [OfflineListReconcile]. The cached phase's keys feed the server
 *   phase, so only those rows can be proven gone; a row without a server id is never removed.
 *   An older-direction server page that does not join its cached page drops that page (D13-R2).
 * - Only an empty **server** page ends history. A page that fails offline (the not-connected
 *   error) keeps the rows shown and raises the "Connect to load more" edge
 *   ([MessageListOfflineReadState]), cleared on reconnect; any other error keeps the rows shown
 *   with no edge (the next scroll retries); with nothing shown, today's error.
 * - Opening reads the newest page first, then applies today's goto / start-from-unread branch;
 *   offline (the not-connected error) that jump reads the anchored window from the cache (D60, [jump]).
 * - A stale generation (session change or chat switch) never touches the list.
 */
internal object MessageListOfflineReads {

    // ---- ViewModel hooks (W1 seams) ---------------------------------------------------------

    /** Hook for `fetchMessages()` (older page / initial). @return true when handled. */
    fun fetchMessages(viewModel: CometChatMessageListViewModel): Boolean = fetchOlder(viewModel.offlineReadHost)

    /** Hook for `fetchNextMessages()` (newer page). @return true when handled. */
    fun fetchNextMessages(viewModel: CometChatMessageListViewModel): Boolean = fetchNewer(viewModel.offlineReadHost)

    /** Hook for `fetchMessagesWithUnreadCount()` (chat open). @return true when handled. */
    fun fetchMessagesWithUnreadCount(viewModel: CometChatMessageListViewModel): Boolean = open(viewModel.offlineReadHost)

    /** Hook for `goToMessage(id)` (jump / start-from-unread anchored read). @return true when handled. */
    fun goToMessage(viewModel: CometChatMessageListViewModel, messageId: Long, highlight: Boolean): Boolean =
        jump(viewModel.offlineReadHost, messageId, highlight)

    /** Called by W5 on reconnect: clears the "Connect to load more" edges, no fetch (G7). */
    fun onConnected(viewModel: CometChatMessageListViewModel) {
        if (viewModel.offlineOn) onConnected(viewModel.offlineReadHost)
    }

    /** Called by W5 after a `PARTIAL` sync or a sync error: re-read the newest window. */
    fun refreshNewestWindow(viewModel: CometChatMessageListViewModel) {
        if (viewModel.offlineOn) refreshNewest(viewModel.offlineReadHost)
    }

    // ---- Logic over the host ----------------------------------------------------------------

    /**
     * The list generation for reads: bumps when the session gen or the chat changes, and clears the
     * edge flags of the old chat. An op captured under an older value is dropped.
     */
    internal fun currentGen(host: MessageListReadHost): Long {
        val state = host.readState
        val key = host.listGen to host.chatKey
        if (key != state.genKey) {
            if (state.genKey != null) {
                state.olderBlocked.value = false
                state.newerBlocked.value = false
                state.jumpToLatest.value = false
            }
            state.newerFrontier = null
            state.genKey = key
            state.readGen++
        }
        return state.readGen
    }

    /**
     * The message reconcile spec. Rows key on [MessageKey.rowKey]; a pending `l:` key resolves to
     * its server key through the [AliasMap], or through an acked row in [rows] that carries the same
     * `localMessageId` / muid, so a pending row and its acked copy are one row. A REST server copy
     * carries no `localMessageId`, so a pending row keyed by its local id also resolves through its
     * own muid: a server copy that lands before the ack still collapses into it. On equal
     * `updatedAt`, the copy with a server id wins.
     */
    internal fun spec(aliases: AliasMap, rows: List<BaseMessage>): Spec<BaseMessage, Long> {
        val ackedByLocal = HashMap<String, Long>()
        val pendingMuidByLocal = HashMap<String, String>()
        for (m in rows) {
            if (m.id <= 0) {
                val local = m.localMessageId?.takeIf { it.isNotEmpty() } ?: continue
                m.muid?.takeIf { it.isNotEmpty() }?.let { pendingMuidByLocal[local] = it }
                continue
            }
            m.localMessageId?.takeIf { it.isNotEmpty() }?.let { ackedByLocal[it] = m.id }
            m.muid?.takeIf { it.isNotEmpty() }?.let { ackedByLocal["m:$it"] = m.id }
        }
        return Spec(
            keyOf = { MessageKey.rowKey(it) },
            serverIdOf = { if (it.id > 0) it.id else null },
            sortAtOf = { it.sentAt },
            updatedAtOf = { it.updatedAt * 2 + if (it.id > 0) 1 else 0 },
            composeAtOf = { it.sentAt },
            resolveKey = { key ->
                val k = key as String
                if (k.startsWith("l:")) {
                    val local = k.substring(2)
                    val serverId = aliases.serverIdFor(local)
                        ?: ackedByLocal[local]
                        ?: pendingMuidByLocal[local]?.let { ackedByLocal["m:$it"] }
                    if (serverId != null) "s:$serverId" else k
                } else {
                    k
                }
            }
        )
    }

    /** How one two-phase read ended. */
    internal sealed class PhaseEnd {
        object Stale : PhaseEnd()
        /** [newestId]: the newest server id in the server page, null when it has none. */
        data class Server(val hasMore: Boolean, val newestId: Long? = null) : PhaseEnd()
        data class Failed(val exception: CometChatException) : PhaseEnd()
    }

    /**
     * Collects one two-phase read into the list: the cached phase is upserted (never ends history),
     * the server phase reconciles with the cached phase's keys. Stops at the first terminal phase.
     * @param dropUnjoined older-direction reads: a cached page the server page does not join is
     * dropped ([unjoinedCachedKeys], D13-R2).
     */
    private suspend fun collectPhases(
        host: MessageListReadHost,
        gen: Long,
        phases: Flow<TwoPhaseResult<BaseMessage>>,
        hadMore: Boolean,
        dropUnjoined: Boolean = false
    ): PhaseEnd {
        var cachedKeys: Set<Any> = emptySet()
        val shownBefore = if (dropUnjoined) rowKeys(host, host.rows) else emptySet()
        var end: PhaseEnd = PhaseEnd.Stale
        phases.transformWhile { emit(it); it is TwoPhaseResult.Cached }.collect { result ->
            if (end != PhaseEnd.Stale || currentGen(host) != gen) return@collect
            when (result) {
                is TwoPhaseResult.Cached -> {
                    val out = reconcile(host, gen, Op(gen, Phase.CACHED, result.rows), hadMore)
                    if (out.applied) {
                        host.rows = out.rows
                        cachedKeys = out.cachedKeys
                        if (out.rows.isNotEmpty()) host.uiState = MessageListUIState.Loaded
                    }
                }
                is TwoPhaseResult.Server -> {
                    val drop = if (dropUnjoined) unjoinedCachedKeys(host, cachedKeys - shownBefore, result.rows) else emptySet()
                    val out = if (drop.isEmpty()) {
                        reconcile(host, gen, Op(gen, Phase.SERVER, result.rows, cachedKeys), hadMore)
                    } else {
                        val key = keyOver(host, host.rows)
                        val kept = host.rows.filterNot { it.id > 0 && key(it) in drop }
                        reconcile(host, gen, Op(gen, Phase.SERVER, result.rows), hadMore, current = kept)
                    }
                    if (out.applied) {
                        host.rows = out.rows
                        // Older paging continues from the oldest row shown, not from the dropped page.
                        if (drop.isNotEmpty()) {
                            host.rows.firstOrNull { it.id > 0 }?.let { host.repository.rebuildRequestFromMessageId(it.id) }
                        }
                        end = PhaseEnd.Server(out.hasMore, newestServerId(result.rows))
                    }
                }
                is TwoPhaseResult.Error -> end = PhaseEnd.Failed(result.exception)
            }
        }
        return end
    }

    private fun reconcile(
        host: MessageListReadHost,
        gen: Long,
        op: Op<BaseMessage>,
        hadMore: Boolean,
        current: List<BaseMessage> = host.rows
    ): OfflineListReconcile.Outcome<BaseMessage> =
        OfflineListReconcile.reconcile(current, currentGen(host), op, spec(host.aliases, current + op.rows), hadMore)

    private fun newestServerId(rows: List<BaseMessage>): Long? = rows.lastOrNull { it.id > 0 }?.id

    /** The resolved reconcile key over [rows] (a pending row and its acked copy share one). */
    private fun keyOver(host: MessageListReadHost, rows: List<BaseMessage>): (BaseMessage) -> Any {
        val s = spec(host.aliases, rows)
        return { s.resolveKey(s.keyOf(it)) }
    }

    private fun rowKeys(host: MessageListReadHost, rows: List<BaseMessage>): Set<Any> =
        keyOver(host, rows).let { key -> rows.mapTo(HashSet()) { key(it) } }

    /**
     * D13-R2 (BEH-READ-07): a server page that does not join the cached page read with it. When the
     * server page shares no server id with the rows the cached phase added ([candidateKeys]) and is
     * newer than all of them, those rows sit past a gap the server has not filled (e.g. cached
     * 100–149, server 350–399): their keys are returned so the caller drops them and pages from the
     * oldest row shown instead. Only rows with a server id are ever candidates (BEH-READ-30: unsent
     * rows stay). Empty when the page joins, or when either side has no server row.
     */
    private fun unjoinedCachedKeys(
        host: MessageListReadHost,
        candidateKeys: Set<Any>,
        serverRows: List<BaseMessage>
    ): Set<Any> {
        if (candidateKeys.isEmpty()) return emptySet()
        val byAge = compareBy<BaseMessage>({ it.sentAt }, { it.id })
        val server = serverRows.filter { it.id > 0 }
        val oldestServer = server.minWithOrNull(byAge) ?: return emptySet()
        val key = keyOver(host, host.rows)
        val cached = host.rows.filter { it.id > 0 && key(it) in candidateKeys }
        val newestCached = cached.maxWithOrNull(byAge) ?: return emptySet()
        val serverIds = server.mapTo(HashSet()) { it.id }
        if (cached.any { it.id in serverIds }) return emptySet()
        return if (byAge.compare(oldestServer, newestCached) > 0) cached.mapTo(HashSet()) { key(it) } else emptySet()
    }

    /**
     * D84: rows shown stay (Loaded). Nothing shown: the Error state; when it failed not-connected
     * the UI shows the kit's offline state ("Connect to load messages", the app's error view if
     * set; `CometChatMessageListViewModel.isOfflineConnectError`) and `onError` fires once through
     * the Error state (rule 3). An Error state shown because the device was offline reloads once on
     * reconnect: the empty list reads the newest page (D58, [MessageListOfflineSync.onConnected]).
     * @param reportWhenShown true for a window read the user or app starts (the open, a jump, the
     * error view's retry): with rows shown an online error still reaches the app's `onError`
     * ([reportShown], rule 5); a not-connected one never does (rule 2). False for a paging read,
     * which keeps today's footer only, and for a read the kit starts by itself (the newest-window
     * refresh after reconnect, `PARTIAL` or a sync error, the reload after a cache reset), which
     * stays quiet over rows.
     */
    private fun showRowsOrError(host: MessageListReadHost, error: Throwable, reportWhenShown: Boolean = true): Boolean =
        if (host.rows.isNotEmpty()) {
            host.uiState = MessageListUIState.Loaded
            if (reportWhenShown && !SearchOffline.isNotConnected(error as? CometChatException)) reportShown(host, error)
            true
        } else {
            host.uiState = MessageListUIState.Error(error)
            false
        }

    /** D84 rule 5: a window read failed online while rows stay on screen; the UI passes it to `onError`. */
    private fun reportShown(host: MessageListReadHost, error: Throwable) {
        host.readState.readErrorQueue.trySend(
            error as? CometChatException ?: CometChatException("ERROR", error.message ?: "read failed")
        )
    }

    /**
     * One older page (or the first page when empty) into the list.
     * @param settleWhenEmpty false when a jump follows: with nothing to show, stay on the loader
     * (no Empty or Error flash) and let the jump decide.
     * @return true when the server page landed or rows are on screen afterwards, false otherwise.
     */
    private suspend fun readOlderPage(
        host: MessageListReadHost,
        gen: Long,
        repo: MessageListTwoPhaseRepository,
        settleWhenEmpty: Boolean = true,
        quiet: Boolean = false
    ): Boolean {
        val chat = host.readState.genKey
        // D83: a fresh window's read (open, error-view retry) reports a failure with cached rows
        // shown; an older page (paging) keeps today's footer only.
        val freshWindow = host.rows.isEmpty()
        if (freshWindow) {
            // A fresh window (open, or after `clear()`): no edge from the previous window applies.
            host.uiState = MessageListUIState.Loading
            host.readState.olderBlocked.value = false
            host.readState.newerBlocked.value = false
            host.readState.newerFrontier = null
        }
        return when (val end = collectPhases(host, gen, repo.fetchPreviousTwoPhase(), host.hasMorePrevious, dropUnjoined = true)) {
            PhaseEnd.Stale -> {
                restoreCursorAfterStaleRead(host, chat)
                false
            }
            is PhaseEnd.Server -> {
                host.hasMorePrevious = end.hasMore
                host.readState.olderBlocked.value = false
                if (host.rows.isNotEmpty()) {
                    host.uiState = MessageListUIState.Loaded
                } else if (settleWhenEmpty) {
                    host.uiState = MessageListUIState.Empty
                }
                newestServerId(host.rows)?.let { host.setLatestMessageId(it) }
                true
            }
            is PhaseEnd.Failed -> {
                // When a jump follows (!settleWhenEmpty) the jump is the window read and reports.
                val shown = if (host.rows.isEmpty() && !settleWhenEmpty) {
                    false
                } else {
                    val report = freshWindow && settleWhenEmpty && !quiet
                    showRowsOrError(host, end.exception, reportWhenShown = report).also { shownNow ->
                        // Only a failure that reached onError (online, D84 rule 5) silences the jump.
                        if (shownNow && report && !SearchOffline.isNotConnected(end.exception)) {
                            host.readState.reportedOpenGen = gen
                        }
                    }
                }
                if (shown) {
                    // Only offline raises "Connect to load more". Any other error keeps the rows
                    // with no error screen over them, and the next scroll retries (W2, G2).
                    if (SearchOffline.isNotConnected(end.exception)) host.readState.olderBlocked.value = true
                    newestServerId(host.rows)?.let { host.setLatestMessageId(it) }
                }
                shown
            }
        }
    }

    internal fun fetchOlder(host: MessageListReadHost): Boolean {
        val quiet = host.readState.quietNextRead
        host.readState.quietNextRead = false
        val repo = host.twoPhase ?: return false
        if (!host.hasMorePrevious || host.inProgress || jumpInFlight(host)) return true
        // A blocked edge waits for tap-to-retry, so a scroll never loops offline reads. With
        // nothing shown (e.g. the error view's retry) the read always runs.
        if (host.rows.isNotEmpty() && host.readState.olderBlocked.value) return true
        val token = beginProgress(host)
        val gen = currentGen(host)
        host.scope.launch {
            try {
                if (readOlderPage(host, gen, repo, quiet = quiet) && currentGen(host) == gen) host.completeFirstFetch()
            } finally {
                endProgress(host, token)
            }
        }
        return true
    }

    internal fun fetchNewer(host: MessageListReadHost): Boolean {
        val repo = host.twoPhase ?: return false
        // The cursor anchors on the newest row WITH a server id; an unsent tail row never anchors.
        // After an offline jump it anchors on the frontier next to the target instead (D60).
        val newest = host.rows.lastOrNull { it.id > 0 } ?: return true
        if (host.inProgress || host.readState.newerBlocked.value || jumpInFlight(host)) return true
        val frontier = host.readState.newerFrontier
        val anchorId = frontier ?: newest.id
        val token = beginProgress(host)
        val gen = currentGen(host)
        host.scope.launch {
            try {
                when (val end = collectPhases(host, gen, repo.fetchNextTwoPhase(anchorId), host.hasMoreNew)) {
                    PhaseEnd.Stale -> Unit
                    is PhaseEnd.Server -> {
                        val pageNewest = end.newestId
                        val joined = frontier == null || pageNewest == null || pageNewest >= newest.id
                        host.readState.newerFrontier = if (joined) null else pageNewest
                        host.hasMoreNew = if (joined) end.hasMore else true
                        host.readState.newerBlocked.value = false
                        newestServerId(host.rows)?.let { host.setLatestMessageId(it) }
                        host.afterNewerPage()
                    }
                    // Only offline raises "Connect to load more"; any other error keeps the rows, as
                    // today's newer page does.
                    is PhaseEnd.Failed -> if (SearchOffline.isNotConnected(end.exception) &&
                        showRowsOrError(host, end.exception, reportWhenShown = false)
                    ) {
                        host.readState.newerBlocked.value = true
                    }
                }
            } finally {
                endProgress(host, token)
            }
        }
        return true
    }

    /**
     * Chat open: newest page first (two-phase), then today's goto / start-from-unread branch.
     *
     * With [MessageListReadHost.anchored] (D60), the conversation settles on the server value, else
     * (only on the not-connected error) the cached copy, so an offline open still learns
     * `lastReadMessageId`; the jump that follows is an opening jump ([jump]). The conversation
     * summary runs only on a server answer (offline it never did).
     */
    internal fun open(host: MessageListReadHost): Boolean {
        val repo = host.twoPhase ?: return false
        val target = host.conversationTarget() ?: return true
        val gen = currentGen(host)
        val chat = host.readState.genKey
        val anchored = host.anchored
        host.scope.launch {
            val conversation = async {
                anchored?.getConversation(target.first, target.second)
                    ?: host.repository.getConversation(target.first, target.second).map { SettledRead(it, fromServer = true) }
            }
            val token = beginProgress(host)
            val shown = try {
                readOlderPage(host, gen, repo)
            } finally {
                endProgress(host, token)
            }
            val result = conversation.await()
            if (currentGen(host) != gen) {
                // A jump replaced the window during the open (ENG-41551): the landing is the jump's,
                // but the conversation bookkeeping still belongs to this chat. A chat switch or a
                // session change drops both.
                if (host.readState.genKey == chat) {
                    result.onSuccess { (c, fromServer) ->
                        host.applyConversation(c)
                        if (fromServer) host.checkConversationSummary(c.unreadMessageCount)
                    }
                    // Superseded by a jump: the open's once-only first-fetch step still runs (mark
                    // read, starters) without the scroll, so a later older page never runs it and
                    // throws the user to the bottom. A clear() owns its own first fetch.
                    if (host.readState.jumpWindowGen == currentGen(host)) host.completeFirstFetch(scrollToBottom = false)
                }
                return@launch
            }
            val goto = host.gotoMessageId
            fun openingJump(messageId: Long, highlight: Boolean) {
                host.readState.openJumpGen = gen
                host.goToMessage(messageId, highlight)
            }
            result
                .onSuccess { (c, fromServer) ->
                    host.applyConversation(c)
                    when {
                        goto > 0 -> openingJump(goto, true)
                        host.startFromUnread && host.lastReadMessageId > 0 && c.unreadMessageCount > 0 ->
                            openingJump(host.lastReadMessageId, false)
                        shown -> {
                            newestServerId(host.rows)?.let { host.setLatestMessageId(it) }
                            host.completeFirstFetch()
                        }
                    }
                    if (fromServer) host.checkConversationSummary(c.unreadMessageCount)
                }
                .onFailure {
                    // Offline with nothing cached: stay on the newest window, no error.
                    if (goto > 0) openingJump(goto, true) else if (shown) host.completeFirstFetch()
                }
        }
        return true
    }

    /**
     * Jump: online as today (target + surrounding window replaces the list; never assumed adjacent,
     * R41). Unsent rows stay. A failure keeps the rows shown; no loader or error over them.
     */
    internal fun jump(host: MessageListReadHost, messageId: Long, highlight: Boolean): Boolean {
        val repo = host.twoPhase ?: return false
        // An opening jump (D60): the open path's goto / start-from-unread jump, or a jump that opens
        // the chat (both kits call goToMessage directly for a goto id). Online it is today's server
        // jump; only when that read fails with the not-connected error does it read the window from
        // the cache. A jump from a chat already shown is always today's server jump.
        val opening = host.rows.isEmpty() || host.readState.openJumpGen == currentGen(host)
        // D83: the open already told onError its read failed; its opening jump does not repeat it.
        val report = !(opening && host.readState.reportedOpenGen == currentGen(host))
        host.readState.openJumpGen = -1L
        val anchored = if (opening) host.anchored else null
        // The jumped window is a new window (ENG-41551): a read started before the jump (an older or
        // newer page, a newest refresh, an earlier jump) is stale and never lands in it, and a page
        // asked for before the window is placed waits for it ([jumpInFlight]).
        val gen = newWindowGen(host)
        host.readState.jumpGen = gen
        host.readState.jumpWindowGen = gen
        host.scope.launch {
            try {
                jumpLocked(host, repo, gen, anchored, messageId, highlight, report)
            } finally {
                if (host.readState.jumpGen == gen) host.readState.jumpGen = -1L
            }
        }
        return true
    }

    /** A new read gen for the same session and chat: every op captured under the old one is dropped. */
    private fun newWindowGen(host: MessageListReadHost): Long {
        currentGen(host)
        return ++host.readState.readGen
    }

    /**
     * A superseded older read (ENG-41551 review) already moved the shared older-page cursor past
     * the page it then dropped. Unless a newer window repositioned the cursor since (a placed jump,
     * a newest refresh, `clear()`), put it back on the oldest row shown, so the next scroll returns
     * the dropped page instead of skipping it. A chat switch reconfigures the request itself.
     */
    private fun restoreCursorAfterStaleRead(host: MessageListReadHost, readChat: Any?) {
        val now = currentGen(host)
        val state = host.readState
        if (state.genKey != readChat || state.cursorGen >= now) return
        host.rows.firstOrNull { it.id > 0 }?.let { host.repository.rebuildRequestFromMessageId(it.id) }
        state.cursorGen = now
    }

    private fun beginProgress(host: MessageListReadHost): Long {
        host.inProgress = true
        return ++host.readState.progressToken
    }

    private fun endProgress(host: MessageListReadHost, token: Long) {
        if (host.readState.progressToken == token) host.inProgress = false
    }

    /**
     * The list was cleared for a new window (the kits' scroll-to-bottom `clear()` then fetch,
     * ENG-41551): every read and jump in flight is stale, and the flag they hold is released so the
     * fetch that follows runs now.
     */
    internal fun startNewWindow(host: MessageListReadHost) {
        // The caller (`clear()`) has reset the request: the cursor belongs to the new window.
        host.readState.cursorGen = newWindowGen(host)
        host.readState.jumpGen = -1L
        host.readState.progressToken++
        host.inProgress = false
    }

    /** True while a jump's window is not placed yet (ENG-41551); a page read now would land in it. */
    private fun jumpInFlight(host: MessageListReadHost): Boolean =
        host.readState.jumpGen != -1L && host.readState.jumpGen == currentGen(host)

    private suspend fun jumpLocked(
        host: MessageListReadHost,
        repo: MessageListTwoPhaseRepository,
        gen: Long,
        anchored: MessageListAnchoredReads?,
        messageId: Long,
        highlight: Boolean,
        report: Boolean
    ) {
        // Superseded before it started (a newer jump, or clear() starting a new window).
        if (currentGen(host) != gen) return
        if (host.rows.isEmpty()) {
            // A jump that opens the chat (both kits call goToMessage directly for a goto id):
            // the newest page is read first, so a failed jump still leaves the newest window.
            val token = beginProgress(host)
            try {
                readOlderPage(host, gen, repo, settleWhenEmpty = false)
            } finally {
                endProgress(host, token)
            }
            if (currentGen(host) != gen) return
        }
        if (host.rows.isEmpty()) host.uiState = MessageListUIState.Loading
        val target = host.repository.getMessage(messageId)
        if (currentGen(host) != gen) return
        target.exceptionOrNull()?.let { error ->
            if (anchored != null && SearchOffline.isNotConnected(error as? CometChatException)) {
                offlineJump(host, gen, anchored, messageId, highlight, error, report)
            } else {
                showRowsOrError(host, error, reportWhenShown = report)
            }
            return
        }
        val surrounding = host.repository.fetchSurroundingMessages(messageId)
        if (currentGen(host) != gen) return
        val result = surrounding.getOrElse { showRowsOrError(host, it, reportWhenShown = report); return }
        val window = result.olderMessages + listOf(result.targetMessage) + result.newerMessages
        val unsent = host.rows.filter { it.id <= 0 }
        val out = reconcile(host, gen, Op(gen, Phase.SERVER, window), hadMore = false, current = unsent)
        if (!out.applied) return
        host.rows = out.rows
        host.readState.newerFrontier = null
        host.markUnreadAnchor()
        host.hasMorePrevious = result.hasMorePrevious
        host.hasMoreNew = result.hasMoreNext
        host.readState.olderBlocked.value = false
        host.readState.newerBlocked.value = false
        if (!result.hasMoreNext) window.lastOrNull()?.let { host.setLatestMessageId(it.id) }
        host.scrollToMessage(messageId, highlight)
        window.firstOrNull()?.let { host.repository.rebuildRequestFromMessageId(it.id) }
        host.readState.cursorGen = gen
        host.uiState = if (host.rows.isEmpty()) MessageListUIState.Empty else MessageListUIState.Loaded
    }

    /** One anchored half: [cached] completes at its cached phase (null when none), [server] at the end. */
    private class Half(
        val cached: CompletableDeferred<List<BaseMessage>?>,
        val server: Deferred<Result<List<BaseMessage>>>
    )

    private fun CoroutineScope.half(phases: Flow<TwoPhaseResult<BaseMessage>>): Half {
        val cached = CompletableDeferred<List<BaseMessage>?>()
        val server = async {
            var end: Result<List<BaseMessage>> = Result.failure(CometChatException("ERROR", "no server phase"))
            try {
                phases.transformWhile { emit(it); it is TwoPhaseResult.Cached }.collect { result ->
                    when (result) {
                        is TwoPhaseResult.Cached -> cached.complete(result.rows)
                        is TwoPhaseResult.Server -> end = Result.success(result.rows)
                        is TwoPhaseResult.Error -> end = Result.failure(result.exception)
                    }
                }
            } finally {
                cached.complete(null)
            }
            end
        }
        return Half(cached, server)
    }

    /**
     * The offline opening jump (D60), reached only when today's server read of the target failed
     * with the not-connected error. The target comes from the cache (BEH-READ-35); the window
     * around it is read anchored on it (BEH-READ-29), both halves at once, and the cached halves
     * are shown: the window replaces the list and is never assumed adjacent to it (R41); unsent
     * rows stay and sort last (BEH-READ-30/31). Server rows from the halves are never merged here
     * (a cached half may skip a gap, so only a server page read from the target may fill it); a
     * failed half raises its edge. Paging resumes from the target in both directions: older pages
     * through the rebuilt request, newer pages through [MessageListOfflineReadState.newerFrontier],
     * so the first online page on either side is the one next to the target.
     * Target not cached, or both cached halves empty: the rows shown stay (the newest window).
     */
    private suspend fun offlineJump(
        host: MessageListReadHost,
        gen: Long,
        reads: MessageListAnchoredReads,
        messageId: Long,
        highlight: Boolean,
        cause: Throwable,
        report: Boolean
    ) {
        val target = reads.getMessage(messageId)
        if (currentGen(host) != gen) return
        val targetRow = target.getOrElse { showRowsOrError(host, it, reportWhenShown = report); return }
        coroutineScope {
            val older = half(reads.fetchPreviousAnchored(messageId))
            val newer = half(reads.fetchNextAnchored(messageId))
            val olderCached = older.cached.await().orEmpty()
            val newerCached = newer.cached.await().orEmpty()
            var placed = false
            if (currentGen(host) == gen && (olderCached.isNotEmpty() || newerCached.isNotEmpty())) {
                val unsent = host.rows.filter { it.id <= 0 }
                val window = olderCached + targetRow + newerCached
                val out = reconcile(host, gen, Op(gen, Phase.CACHED, window), hadMore = true, current = unsent)
                if (out.applied) {
                    host.rows = out.rows
                    placed = true
                    // The window is placed: paging may resume while the server halves finish.
                    if (host.readState.jumpGen == gen) host.readState.jumpGen = -1L
                    host.markUnreadAnchor()
                    host.hasMorePrevious = true
                    host.hasMoreNew = true
                    host.readState.olderBlocked.value = false
                    host.readState.newerBlocked.value = false
                    host.repository.rebuildRequestFromMessageId(targetRow.id)
                    host.readState.cursorGen = gen
                    host.readState.newerFrontier = targetRow.id
                    host.scrollToMessage(messageId, highlight)
                    host.uiState = MessageListUIState.Loaded
                }
            }
            val olderEnd = older.server.await()
            val newerEnd = newer.server.await()
            if (currentGen(host) != gen) return@coroutineScope
            if (!placed) {
                // Target not cached (both cached halves empty): stay on the rows shown.
                showRowsOrError(
                    host,
                    olderEnd.exceptionOrNull() ?: CometChatException("ERROR", "jump failed"),
                    reportWhenShown = report
                )
                return@coroutineScope
            }
            host.readState.olderBlocked.value = olderEnd.isFailure
            host.readState.newerBlocked.value = newerEnd.isFailure
            // Placed from the cache because the server read failed: rows stay. The cause is the
            // not-connected error, so onError hears nothing (D84 rule 2).
            if (report && !SearchOffline.isNotConnected(cause as? CometChatException)) reportShown(host, cause)
        }
    }

    /**
     * A fresh newest read with no anchor. If the server page overlaps the rows shown, it reconciles
     * in place (older pagination re-anchors on the oldest row with a server id); otherwise the list
     * resets to that window (unsent rows kept). Either way, when the newest end changed the UI is
     * told ([MessageListOfflineReadState.jumpToLatest]). Only the kit starts this read (reconnect
     * with nothing shown, `PARTIAL`, a sync error), so a failure keeps the rows and the cursor and
     * reports nothing (D84 rule 5); with nothing shown it shows the error view.
     */
    internal fun refreshNewest(host: MessageListReadHost) {
        val repo = host.twoPhase ?: return
        val gen = currentGen(host)
        host.scope.launch {
            host.awaitIdle()
            if (currentGen(host) != gen) return@launch
            val token = beginProgress(host)
            try {
                refreshNewestLocked(host, gen, repo)
            } finally {
                endProgress(host, token)
            }
        }
    }

    private suspend fun refreshNewestLocked(host: MessageListReadHost, gen: Long, repo: MessageListTwoPhaseRepository) {
        val before = host.rows
        val shownIds = before.mapNotNullTo(HashSet()) { m -> m.id.takeIf { it > 0 } }
        val newestBefore = newestServerId(before)
        val chat = host.readState.genKey
        host.repository.resetRequest()
        host.readState.cursorGen = gen
        var cachedKeys: Set<Any> = emptySet()
        var serverRows: List<BaseMessage>? = null
        var failure: Throwable? = null
        repo.fetchPreviousTwoPhase().transformWhile { emit(it); it is TwoPhaseResult.Cached }.collect { result ->
            if (currentGen(host) != gen) return@collect
            when (result) {
                is TwoPhaseResult.Cached ->
                    // A cached window that does not touch the rows shown is not painted: it may
                    // sit past a gap, and the server phase decides overlap.
                    if (shownIds.isEmpty() || result.rows.any { it.id in shownIds }) {
                        val out = reconcile(host, gen, Op(gen, Phase.CACHED, result.rows), host.hasMorePrevious)
                        if (out.applied) {
                            host.rows = out.rows
                            cachedKeys = out.cachedKeys
                            if (out.rows.isNotEmpty()) host.uiState = MessageListUIState.Loaded
                        }
                    }
                is TwoPhaseResult.Server -> serverRows = result.rows
                is TwoPhaseResult.Error -> failure = result.exception
            }
        }
        if (currentGen(host) != gen) {
            restoreCursorAfterStaleRead(host, chat)
            return
        }
        val page = serverRows
        if (page == null) {
            // Failed: the request was reset for this read; put older pagination back on the list's edge.
            before.firstOrNull { it.id > 0 }?.let { host.repository.rebuildRequestFromMessageId(it.id) }
            // D83 amended: the kit started this read, so rows shown stay and nothing is reported;
            // nothing shown → the error view.
            showRowsOrError(host, failure ?: CometChatException("ERROR", "refresh failed"), reportWhenShown = false)
            return
        }
        val overlaps = shownIds.isEmpty() || page.any { it.id in shownIds }
        if (overlaps) {
            val out = reconcile(host, gen, Op(gen, Phase.SERVER, page, cachedKeys), host.hasMorePrevious)
            if (!out.applied) return
            host.rows = out.rows
            if (shownIds.isEmpty()) {
                host.hasMorePrevious = out.hasMore
            } else {
                host.rows.firstOrNull { it.id > 0 }?.let { host.repository.rebuildRequestFromMessageId(it.id) }
            }
        } else {
            val unsent = host.rows.filter { it.id <= 0 }
            val out = reconcile(host, gen, Op(gen, Phase.SERVER, page), hadMore = false, current = unsent)
            if (!out.applied) return
            host.rows = out.rows
            host.hasMorePrevious = page.isNotEmpty()
            host.readState.olderBlocked.value = false
            host.readState.newerFrontier = null
        }
        // After an offline jump a gap may remain between the target and this page: newer paging
        // stays open from the frontier (D60).
        host.hasMoreNew = host.readState.newerFrontier != null
        host.readState.newerBlocked.value = false
        val newestAfter = newestServerId(host.rows)
        newestAfter?.let { host.setLatestMessageId(it) }
        if (!overlaps || (newestBefore != null && newestAfter != newestBefore)) {
            host.readState.jumpToLatest.value = true
        }
        host.uiState = if (host.rows.isEmpty()) MessageListUIState.Empty else MessageListUIState.Loaded
        // D58: a list with no server rows (opened offline, nothing cached) never ran today's
        // first-fetch step; it runs now, once, so the list lands at the bottom here and the first
        // older page does not throw the user there.
        if (shownIds.isEmpty()) host.completeFirstFetch()
    }

    /**
     * Reconnect (G7, D84 rule 9): the page that failed offline at each blocked "Connect to load
     * more" edge loads once by itself. It waits for any read in flight, then loads the older edge,
     * waits for it to settle, then the newer edge. An edge is loaded only if it is still blocked
     * when its turn comes (a tap, or a read that answered meanwhile, already took care of it), and
     * nothing loads once the window changed (a jump, `clear()`, another chat): the new window owns
     * its edges. A jump already in flight replaces this window, so nothing loads then either.
     * With nothing shown the edges just clear: the empty list reads the newest page instead (D58).
     */
    internal fun onConnected(host: MessageListReadHost) {
        val state = host.readState
        if (host.rows.isEmpty()) {
            state.olderBlocked.value = false
            state.newerBlocked.value = false
            return
        }
        if (!state.olderBlocked.value && !state.newerBlocked.value) return
        if (jumpInFlight(host)) return
        val gen = currentGen(host)
        host.scope.launch {
            loadBlockedEdge(host, gen, state.olderBlocked) { fetchOlder(host) }
            loadBlockedEdge(host, gen, state.newerBlocked) { fetchNewer(host) }
        }
    }

    /**
     * One reconnect load of a blocked edge (D84 rule 9): after the reads in flight settle, and only
     * while the window is still [gen] and the edge still blocked, clears it and starts [fetch]; then
     * waits for that read to settle, so the next edge starts after it.
     */
    private suspend fun loadBlockedEdge(
        host: MessageListReadHost,
        gen: Long,
        edge: MutableStateFlow<Boolean>,
        fetch: () -> Unit
    ) {
        host.awaitIdle()
        if (currentGen(host) != gen || jumpInFlight(host) || !edge.value) return
        edge.value = false
        fetch()
        host.awaitIdle()
    }

    /** Tap on "Connect to load more": clears the edge and retries the blocked direction. */
    internal fun retryLoadMore(host: MessageListReadHost) {
        val state = host.readState
        val older = state.olderBlocked.value
        val newer = state.newerBlocked.value
        state.olderBlocked.value = false
        state.newerBlocked.value = false
        if (older) fetchOlder(host) else if (newer) fetchNewer(host)
    }
}
