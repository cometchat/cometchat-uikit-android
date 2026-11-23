package com.cometchat.uikit.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Reaction
import com.cometchat.chat.models.ReactionCount
import com.cometchat.chat.models.ReactionEvent
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.domain.usecase.FetchReactionsUseCase
import com.cometchat.uikit.core.domain.usecase.RemoveReactionUseCase
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.events.MessageStatus
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineListSession
import com.cometchat.uikit.core.offline.OfflinePagedMerge
import com.cometchat.uikit.core.offline.OfflineReconnectRefill
import com.cometchat.uikit.core.offline.TwoPhaseResult
import com.cometchat.uikit.core.state.ReactionListUIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing reaction list state.
 * Handles fetching reactions, caching per emoji tab, and reaction removal.
 * Uses StateFlow for reactive state management.
 *
 * @param fetchReactionsUseCase Use case for fetching reactions
 * @param removeReactionUseCase Use case for removing reactions
 * @param enableListeners Whether to enable CometChat listeners (set to false for testing)
 */
open public class CometChatReactionListViewModel(
    private val fetchReactionsUseCase: FetchReactionsUseCase,
    private val removeReactionUseCase: RemoveReactionUseCase,
    private val enableListeners: Boolean = true
) : ViewModel() {

    public companion object {
        private const val TAG = "ReactionListViewModel"
        private const val DEFAULT_LIMIT = 10
        internal const val ALL_TAB_KEY = "All"
    }

    // UI State
    private val _uiState = MutableStateFlow<ReactionListUIState>(ReactionListUIState.Loading)
    public val uiState: StateFlow<ReactionListUIState> = _uiState.asStateFlow()

    /**
     * Offline on: a tab's first-page load failed online while rows from its cached phase stay on
     * screen (D84 rule 5). The state stays Content, so the views route this to `onError` instead of
     * the error view. A not-connected failure over rows is not reported (D84 rule 2), nor is a
     * failed load-more, as today, nor the read the kit starts by itself on reconnect. Never emitted
     * offline off (there a failed load is the [ReactionListUIState.Error] state).
     */
    // A buffered channel, consumed once: an error raised before the view collects (the first load
    // can start before it) still arrives, exactly once, and never repeats on rotation/recomposition.
    private val _loadErrorWithRows = Channel<CometChatException>(Channel.BUFFERED)
    public val loadErrorWithRows: Flow<CometChatException> = _loadErrorWithRows.receiveAsFlow()

    private val _connectToLoad = MutableStateFlow(false)

    /**
     * True while [uiState] is [ReactionListUIState.Error] because the tab was read offline with
     * nothing shown (D84 rule 3): the kit shows its offline state ("Connect to load reactions", no
     * Retry) unless the app set an error view; `onError` fires once through the Error state. The
     * tab reads once on reconnect. Always false with offline messaging off.
     */
    public val connectToLoad: StateFlow<Boolean> = _connectToLoad.asStateFlow()

    // Reaction headers (tabs) - includes "All" tab as first item
    private val _reactionHeaders = MutableStateFlow<List<ReactionCount>>(emptyList())
    public val reactionHeaders: StateFlow<List<ReactionCount>> = _reactionHeaders.asStateFlow()

    // Reacted users list for current tab
    private val _reactedUsers = MutableStateFlow<List<Reaction>>(emptyList())
    public val reactedUsers: StateFlow<List<Reaction>> = _reactedUsers.asStateFlow()

    // Selected reaction (emoji or "All")
    private val _selectedReaction = MutableStateFlow<String?>(null)
    public val selectedReaction: StateFlow<String?> = _selectedReaction.asStateFlow()

    // Active tab index
    private val _activeTabIndex = MutableStateFlow(0)
    public val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    // Base message
    private val _baseMessage = MutableStateFlow<BaseMessage?>(null)
    public val baseMessage: StateFlow<BaseMessage?> = _baseMessage.asStateFlow()

    // Caching - keyed by emoji (or "All" for all reactions)
    private val reactionRequestCache = mutableMapOf<String, ReactionsRequest>()
    private val reactedUserCache = mutableMapOf<String, MutableList<Reaction>>()

    // State flags to prevent concurrent operations
    private var isFetching = false

    /**
     * Cache generation: [clearCache] (which also drops the per-tab requests) makes in-flight
     * offline reads stale. [fetchingGen] is the generation of the read in flight.
     */
    private var fetchGen = 0L
    private var fetchingGen = 0L
    private var isRemovingReaction = false

    // Track the reaction being removed for state updates
    private var currentRemovedReaction: String? = null
    private var currentSelectedReactionTab: String? = null

    // Configuration
    private var reactionsRequestBuilder: ReactionsRequest.ReactionsRequestBuilder? = null

    // Local event listener job
    private var messageEventsJob: Job? = null

    /**
     * Offline on, a tab that showed the error view because it was not connected reads once when the
     * connection returns, only if it still shows nothing (D83, ENG-41462). Never armed offline off.
     * Cancelled when the message changes ([setBaseMessage]) or the list is dismissed
     * ([cancelReconnectRefill]), so a closed sheet never reads in the background.
     */
    private val offlineRefill = OfflineReconnectRefill("reaction_list") {
        viewModelScope.launch {
            if (_reactedUsers.value.isEmpty()) {
                // The kit starts this read: quiet if it fails over rows (D84 rule 5).
                quietNextRead = true
                try {
                    fetchReactedUsers()
                } finally {
                    quietNextRead = false
                }
            }
        }
    }

    // Set only around the reconnect refill's fetchReactedUsers() call, which reads it
    // synchronously: a failure of that read while rows are on screen reports nothing.
    private var quietNextRead = false

    /**
     * Bumped only by a session end or cache reset (G8), so a plain (non two-phase) read already in
     * flight drops its result. Never moves with offline off, where that read behaves as today.
     */
    private var offlineSessionGen = 0L

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

    // ==================== Public API Methods ====================

    /**
     * Sets the base message and extracts reaction headers.
     * This initializes the reaction tabs from the message's reaction counts.
     *
     * @param message The message to display reactions for
     */
    public fun setBaseMessage(message: BaseMessage) {
        offlineRefill.disarm()
        _baseMessage.value = message
        
        // Extract reaction counts from message and create headers
        val reactionCounts = message.reactions ?: emptyList()
        setReactionHeaders(reactionCounts)
        
        // Initialize with "All" tab selected
        if (reactionCounts.isNotEmpty()) {
            currentSelectedReactionTab = ALL_TAB_KEY
            _selectedReaction.value = ALL_TAB_KEY
            _activeTabIndex.value = 0
        }
    }

    /**
     * Sets the reactions request builder for custom fetch configuration.
     *
     * @param builder The custom request builder
     */
    public fun setReactionsRequestBuilder(builder: ReactionsRequest.ReactionsRequestBuilder) {
        reactionsRequestBuilder = builder
    }

    /**
     * Sets the selected reaction and updates the active tab index.
     * This triggers fetching reactions for the selected emoji.
     *
     * @param reaction The emoji to filter by, or "All" for all reactions
     */
    public fun setSelectedReaction(reaction: String?) {
        val reactionToSet = reaction ?: ALL_TAB_KEY
        _selectedReaction.value = reactionToSet
        currentSelectedReactionTab = reactionToSet
        
        // Update active tab index based on selected reaction
        val headers = _reactionHeaders.value
        if (headers.isNotEmpty()) {
            val foundIndex = headers.indexOfFirst { it.reaction == reactionToSet }
            if (foundIndex != -1) {
                _activeTabIndex.value = foundIndex
            }
        }
    }

    /**
     * Fetches reacted users for the current or specified reaction filter.
     * Uses caching to avoid redundant network requests.
     *
     * @param reactionFilter The emoji to filter by, or null for current selection
     * @param customBuilder Optional custom request builder
     */
    public fun fetchReactedUsers(
        reactionFilter: String? = null,
        customBuilder: ReactionsRequest.ReactionsRequestBuilder? = null
    ) {
        // Prevent concurrent fetches. Offline on, a read left over from a cleared cache does not
        // block a fresh one (its result is dropped as stale).
        val offlineOn = OfflineGate.isOn()
        if (isFetching && (!offlineOn || fetchingGen == fetchGen)) return

        val message = _baseMessage.value ?: return

        // Update selection if filter provided
        if (reactionFilter != null) {
            currentSelectedReactionTab = reactionFilter
            _selectedReaction.value = reactionFilter
            // Clear current list when switching tabs
            _reactedUsers.value = emptyList()
        }

        val tabKey = currentSelectedReactionTab ?: ALL_TAB_KEY

        // Check cache first - display cached data immediately
        if (reactedUserCache.containsKey(tabKey)) {
            _reactedUsers.value = reactedUserCache[tabKey] ?: emptyList()
        }

        // Show loading state only if no cached data
        if (_reactedUsers.value.isEmpty()) {
            _uiState.value = ReactionListUIState.Loading
        }

        // Get or create request for this tab
        val reactionsRequest = getOrCreateRequest(tabKey, message.id, customBuilder)

        // Offline on, with the default use case: the SDK two-phase read. Otherwise today's read
        // (which itself runs on the two-phase SDK overload, server phase only).
        val twoPhase = if (offlineOn) fetchReactionsUseCase.twoPhaseOrNull(reactionsRequest) else null
        if (twoPhase != null) {
            fetchTwoPhase(tabKey, twoPhase)
            return
        }

        val session = offlineSessionGen
        viewModelScope.launch {
            fetchingGen = fetchGen
            isFetching = true

            val result = fetchReactionsUseCase(reactionsRequest)
            // G8: dropped after a session end or cache reset (offline on only).
            if (session != offlineSessionGen) return@launch
            result
                .onSuccess { reactions ->
                    handleFetchSuccess(tabKey, reactions)
                }
                .onFailure { exception ->
                    handleFetchError(exception as CometChatException)
                }

            isFetching = false
        }
    }

    /**
     * Offline-on read of one page for [tabKey] (README §3.9, BEH-SNAP-03): the cached page is the
     * first paint, and the server page replaces it in place, deduped by reaction id
     * ([OfflinePagedMerge]), so a row is never shown twice. An empty cached page changes nothing.
     * D83: an error never covers rows already shown (a first-page one the user or app started is
     * reported through [loadErrorWithRows]; the reconnect refill's is not); with nothing shown it
     * is the error view, and a not-connected one reads once on reconnect. A server page that leaves the tab with nothing is an empty list under
     * Content, as offline off: the reactions sheet stays open. A stale generation is dropped.
     */
    private fun fetchTwoPhase(tabKey: String, phases: Flow<TwoPhaseResult<Reaction>>) {
        val gen = fetchGen
        // A first page the kit started by itself (reconnect) is quiet over rows, like a paging read.
        val firstPage = cachedRows(tabKey).isEmpty() && !quietNextRead
        quietNextRead = false
        offlineRefill.disarm()
        viewModelScope.launch {
            fetchingGen = gen
            isFetching = true
            var window: Set<Any> = emptySet()
            phases.collect { phase ->
                if (gen != fetchGen) return@collect
                when (phase) {
                    is TwoPhaseResult.Cached -> {
                        val merged = OfflinePagedMerge.applyCached(cachedRows(tabKey), phase.rows, ::reactionKey)
                        window = merged.window
                        if (merged.added > 0) showRows(tabKey, merged.rows)
                    }

                    is TwoPhaseResult.Server -> {
                        val merged = OfflinePagedMerge.applyServer(cachedRows(tabKey), phase.rows, window, ::reactionKey)
                        showRows(tabKey, merged.rows)
                    }

                    is TwoPhaseResult.Error -> handleTwoPhaseError(tabKey, phase.exception, firstPage)
                }
            }
            if (fetchingGen == gen) isFetching = false
        }
    }

    private fun cachedRows(tabKey: String): List<Reaction> = reactedUserCache[tabKey]?.toList() ?: emptyList()

    /** Stores [rows] for [tabKey] and shows them (as a new list) when that tab is selected. */
    private fun showRows(tabKey: String, rows: List<Reaction>) {
        reactedUserCache[tabKey] = rows.toMutableList()
        if ((currentSelectedReactionTab ?: ALL_TAB_KEY) == tabKey) _reactedUsers.value = rows.toList()
        _uiState.value = ReactionListUIState.Content
    }

    private fun handleTwoPhaseError(tabKey: String, exception: CometChatException, firstPage: Boolean) {
        if (exception.code == CometChatConstants.Errors.ERROR_REQUEST_IN_PROGRESS) return
        val notConnected = OfflinePagedMerge.isNotConnected(exception)
        if (cachedRows(tabKey).isNotEmpty()) {
            _uiState.value = ReactionListUIState.Content
            // D84 rules 2 and 5: only an online failure is reported over rows.
            if (firstPage && !notConnected) _loadErrorWithRows.trySend(exception)
        } else {
            // D84 rule 3: offline with nothing shown is the kit's offline state.
            _connectToLoad.value = notConnected
            _uiState.value = ReactionListUIState.Error(exception)
            if (notConnected) offlineRefill.arm()
        }
    }

    /** Reaction identity: the server reaction id, else (message, user, emoji). */
    private fun reactionKey(reaction: Reaction): Any =
        reaction.reactionId?.takeIf { it.isNotEmpty() } ?: "${reaction.messageId}:${reaction.uid}:${reaction.reaction}"

    /**
     * Removes a reaction from the message.
     * Only the logged-in user's own reactions can be removed.
     *
     * @param message The message to remove reaction from
     * @param emoji The emoji reaction to remove
     */
    public fun removeReaction(message: BaseMessage, emoji: String) {
        // Prevent concurrent removals
        if (isRemovingReaction) return

        isRemovingReaction = true
        currentRemovedReaction = emoji

        viewModelScope.launch {
            removeReactionUseCase(message.id, emoji)
                .onSuccess { updatedMessage ->
                    handleReactionRemovedByMe(emoji)
                    // Emit UIKit event so MessageListViewModel updates the message bubble in real-time
                    CometChatEvents.emitMessageEvent(
                        CometChatMessageEvent.MessageEdited(updatedMessage, MessageStatus.SUCCESS)
                    )
                    isRemovingReaction = false
                }
                .onFailure { exception ->
                    // Reset flag on failure to allow retry
                    isRemovingReaction = false
                }
        }
    }

    /**
     * Clears both caches (request and user caches).
     * Call this when the message changes or component is reset.
     */
    public fun clearCache() {
        offlineRefill.disarm()
        fetchGen++
        reactionRequestCache.clear()
        reactedUserCache.clear()
    }

    /**
     * Clears the list for a session change (logout or a switch to a different uid), before the next
     * frame (G8). Bumps the read generations, so a read already in flight drops its result, empties
     * the rows and every tab's cached rows and requests (the next read starts from page one). The
     * message, its reaction tabs and the selected tab are kept. Offline on only (the hub drives it).
     */
    private fun clearForOfflineSession() {
        offlineRefill.disarm()
        fetchGen++
        offlineSessionGen++
        reactionRequestCache.clear()
        reactedUserCache.clear()
        isFetching = false
        quietNextRead = false
        _connectToLoad.value = false
        _reactedUsers.value = emptyList()
        _uiState.value = ReactionListUIState.Loading
    }

    /**
     * The offline cache was reset (G8): clear as for a session end, then read the selected tab once
     * from page one. The kit starts this read, so a failure over rows stays quiet (D84 rule 5).
     * Nothing reads before a message is set.
     */
    private fun onOfflineCacheReset() {
        clearForOfflineSession()
        if (_baseMessage.value == null) return
        quietNextRead = true
        try {
            fetchReactedUsers()
        } finally {
            quietNextRead = false
        }
    }

    /**
     * The list was dismissed (the reactions sheet closed): drop any pending reconnect read (D83, G7).
     * The kit components call this; a later fetch arms it again if needed.
     */
    public fun cancelReconnectRefill() {
        offlineRefill.release()
    }

    // ==================== Private Methods ====================

    /**
     * Sets the reaction headers from reaction counts.
     * Creates an "All" tab as the first item with total count.
     */
    private fun setReactionHeaders(reactionCounts: List<ReactionCount>) {
        if (reactionCounts.isEmpty()) {
            _reactionHeaders.value = emptyList()
            return
        }

        val headers = mutableListOf<ReactionCount>()

        // Create "All" tab with total count
        val totalCount = reactionCounts.sumOf { it.count }
        val allTab = ReactionCount().apply {
            reaction = ALL_TAB_KEY
            count = totalCount
            setReactedByMe(false)
        }
        headers.add(allTab)

        // Add individual emoji tabs
        headers.addAll(reactionCounts)

        _reactionHeaders.value = headers
    }

    /**
     * Gets or creates a ReactionsRequest for the specified tab.
     */
    private fun getOrCreateRequest(
        tabKey: String,
        messageId: Long,
        customBuilder: ReactionsRequest.ReactionsRequestBuilder?
    ): ReactionsRequest {
        // Return cached request if exists
        reactionRequestCache[tabKey]?.let { return it }

        // Create new request
        val builder = customBuilder
            ?: reactionsRequestBuilder
            ?: ReactionsRequest.ReactionsRequestBuilder().setLimit(DEFAULT_LIMIT)

        builder.setMessageId(messageId)

        // Set reaction filter if not "All" tab
        if (tabKey != ALL_TAB_KEY) {
            builder.setReaction(tabKey)
        }

        val request = builder.build()
        reactionRequestCache[tabKey] = request
        return request
    }

    /**
     * Handles successful fetch of reactions.
     */
    private fun handleFetchSuccess(tabKey: String, reactions: List<Reaction>) {
        if (reactions.isNotEmpty()) {
            // Update cache
            if (reactedUserCache.containsKey(tabKey)) {
                reactedUserCache[tabKey]?.addAll(reactions)
            } else {
                reactedUserCache[tabKey] = reactions.toMutableList()
            }

            // Update UI
            _reactedUsers.value = reactedUserCache[tabKey] ?: emptyList()
        }

        _uiState.value = ReactionListUIState.Content
    }

    /**
     * Handles fetch error.
     */
    private fun handleFetchError(exception: CometChatException) {
        // Don't show error for "request in progress" errors
        if (exception.code != CometChatConstants.Errors.ERROR_REQUEST_IN_PROGRESS) {
            _uiState.value = ReactionListUIState.Error(exception)
        }
    }

    /**
     * Handles reaction removal by the logged-in user.
     * Updates headers and cache accordingly.
     */
    private fun handleReactionRemovedByMe(removedReaction: String) {
        val loggedInUserId = getLoggedInUserSafe()?.uid ?: return
        var newActiveTab = -2

        val currentHeaders = _reactionHeaders.value.toMutableList()
        if (currentHeaders.isEmpty()) return

        // Find and update the removed reaction's header
        val reactionHeaderIndex = currentHeaders.indexOfFirst { it.reaction == removedReaction }
        if (reactionHeaderIndex != -1) {
            val rc = currentHeaders[reactionHeaderIndex]
            if (rc.reactedByMe) {
                if (rc.count == 1) {
                    // Remove the tab entirely
                    newActiveTab = 0
                    currentHeaders.removeAt(reactionHeaderIndex)
                } else {
                    // Decrease count
                    if (currentSelectedReactionTab != ALL_TAB_KEY) {
                        newActiveTab = reactionHeaderIndex
                    }
                    currentHeaders[reactionHeaderIndex] = ReactionCount().apply {
                        reaction = rc.reaction
                        count = rc.count - 1
                        setReactedByMe(false)
                    }
                }
            }

            // Update "All" tab count
            if (currentHeaders.isNotEmpty()) {
                val allTab = currentHeaders[0]
                if (allTab.count == 1) {
                    // Remove "All" tab (list is now empty)
                    newActiveTab = -1
                    currentHeaders.removeAt(0)
                } else {
                    currentHeaders[0] = ReactionCount().apply {
                        reaction = ALL_TAB_KEY
                        count = allTab.count - 1
                        setReactedByMe(false)
                    }
                }
            }
        }

        // Update cache
        if (newActiveTab == -1) {
            // All reactions removed - clear cache
            reactedUserCache.clear()
        } else {
            // Remove user from specific reaction cache
            removeUserFromCache(removedReaction, loggedInUserId, isAllTab = false)
            // Remove user from "All" cache
            removeUserFromCache(removedReaction, loggedInUserId, isAllTab = true)
        }

        // Update headers
        _reactionHeaders.value = currentHeaders

        // Update active tab and selection
        when {
            newActiveTab == -1 || newActiveTab == 0 -> {
                _activeTabIndex.value = if (currentHeaders.isEmpty()) -1 else 0
                _selectedReaction.value = ALL_TAB_KEY
                currentSelectedReactionTab = ALL_TAB_KEY
            }
            newActiveTab > 0 -> {
                _selectedReaction.value = removedReaction
            }
        }

        // Update UI state
        if (currentHeaders.isEmpty()) {
            _uiState.value = ReactionListUIState.Empty
        }
    }

    /**
     * Handles reaction removal by another user (real-time update).
     */
    private fun handleReactionRemoved(removedReaction: String, uid: String) {
        val tabIndex = getReactionIndexFromHeader(currentSelectedReactionTab ?: ALL_TAB_KEY)

        // Update reaction header based on the removed reaction
        updateReactionHeaderList(tabIndex, getReactionIndexFromHeader(removedReaction))

        // Remove the user from the cache for the specific reaction and "All" reactions
        removeUserFromCache(removedReaction, uid, isAllTab = false)
        removeUserFromCache(removedReaction, uid, isAllTab = true)
    }

    /**
     * Finds the index of a specified reaction in the header list.
     */
    private fun getReactionIndexFromHeader(reaction: String): Int {
        return _reactionHeaders.value.indexOfFirst { it.reaction == reaction }
    }

    /**
     * Updates the reaction header list by adjusting counts or removing reactions.
     */
    private fun updateReactionHeaderList(tabIndex: Int, removedReactionIndex: Int) {
        if (removedReactionIndex == -1) return

        val currentHeaders = _reactionHeaders.value.toMutableList()
        if (currentHeaders.isEmpty()) return

        var newActiveTab = tabIndex

        // Decrease the count of the removed reaction
        val removedReactionObj = currentHeaders[removedReactionIndex]
        val newCount = removedReactionObj.count - 1

        if (newCount < 1) {
            // Remove the reaction from the list
            if (currentSelectedReactionTab == removedReactionObj.reaction) {
                newActiveTab = 0
            }
            currentHeaders.removeAt(removedReactionIndex)
        } else {
            currentHeaders[removedReactionIndex] = ReactionCount().apply {
                reaction = removedReactionObj.reaction
                count = newCount
                setReactedByMe(removedReactionObj.reactedByMe)
            }
        }

        // Handle the "All" tab adjustments
        if (currentHeaders.isNotEmpty()) {
            val allTab = currentHeaders[0]
            val allCount = allTab.count - 1

            if (allCount < 1) {
                currentHeaders.removeAt(0)
            } else {
                currentHeaders[0] = ReactionCount().apply {
                    reaction = ALL_TAB_KEY
                    count = allCount
                    setReactedByMe(false)
                }
            }
        }

        // Update state
        _reactionHeaders.value = currentHeaders
        _activeTabIndex.value = newActiveTab

        if (currentHeaders.isEmpty()) {
            _uiState.value = ReactionListUIState.Empty
        }
    }

    /**
     * Removes a user's reaction from the cache.
     */
    private fun removeUserFromCache(removedReaction: String, uid: String, isAllTab: Boolean) {
        val cacheKey = if (isAllTab) ALL_TAB_KEY else removedReaction

        reactedUserCache[cacheKey]?.let { reactions ->
            val iterator = reactions.iterator()
            while (iterator.hasNext()) {
                val reaction = iterator.next()
                if (reaction.reaction == removedReaction && reaction.uid == uid) {
                    iterator.remove()
                    break
                }
            }
        }

        // Update displayed list if viewing affected tab
        if (currentSelectedReactionTab == cacheKey) {
            _reactedUsers.value = reactedUserCache[cacheKey] ?: emptyList()
        }
    }

    /**
     * Safely gets the logged-in user, returning null if SDK is not initialized.
     */
    private fun getLoggedInUserSafe(): com.cometchat.chat.models.User? {
        return try {
            CometChatUIKit.getLoggedInUser()
        } catch (e: Exception) {
            null
        }
    }

    // ==================== Listener Management ====================

    /**
     * Adds listeners for real-time reaction updates.
     */
    private fun addListeners() {
        addLocalEventListeners()
    }

    /**
     * Adds local event listeners for UI-triggered message events.
     */
    private fun addLocalEventListeners() {
        messageEventsJob = viewModelScope.launch {
            CometChatEvents.messageEvents.collect { event ->
                when (event) {
                    is CometChatMessageEvent.MessageEdited -> {
                        // Handle reaction removed by me (via SDK callback)
                        val message = _baseMessage.value
                        if (message != null && message.id == event.message.id) {
                            if (event.status == MessageStatus.SUCCESS && currentRemovedReaction != null) {
                                // Reaction removal was successful
                                val loggedInUserId = getLoggedInUserSafe()?.uid
                                if (loggedInUserId != null) {
                                    handleReactionRemovedByMe(currentRemovedReaction!!)
                                }
                                currentRemovedReaction = null
                            }
                        }
                    }

                    is CometChatMessageEvent.ReactionRemoved -> {
                        // Handle reaction removed by another user
                        val message = _baseMessage.value
                        if (message != null && message.id == event.event.reaction.messageId) {
                            handleReactionRemoved(
                                event.event.reaction.reaction,
                                event.event.reaction.uid
                            )
                        }
                    }

                    else -> {
                        // Ignore other events
                    }
                }
            }
        }
    }

    /**
     * Removes all listeners.
     */
    private fun removeListeners() {
        messageEventsJob?.cancel()
        messageEventsJob = null
    }

    override fun onCleared() {
        super.onCleared()
        removeListeners()
        offlineRefill.release()
        clearCache()
    }
}
