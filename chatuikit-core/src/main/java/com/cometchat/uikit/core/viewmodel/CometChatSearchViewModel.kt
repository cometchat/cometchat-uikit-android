package com.cometchat.uikit.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.enums.AttachmentType
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.constants.FilterGroup
import com.cometchat.uikit.core.constants.SearchFilter
import com.cometchat.uikit.core.constants.SearchMode
import com.cometchat.uikit.core.constants.SearchScope
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.domain.usecase.FetchConversationsUseCase
import com.cometchat.uikit.core.domain.usecase.FetchMessagesUseCase
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineListReconcile
import com.cometchat.uikit.core.offline.OfflineListSession
import com.cometchat.uikit.core.offline.OfflineReconnectRefill
import com.cometchat.uikit.core.offline.SearchOffline
import com.cometchat.uikit.core.offline.TwoPhaseResult
import com.cometchat.uikit.core.state.SearchUIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the CometChatSearch component.
 * Manages search state, debouncing, and data fetching for both conversations and messages.
 *
 * Features:
 * - Dual search: Searches both Conversations AND Messages simultaneously
 * - Filter chips: Photos, Videos, Documents, Links, Audio, Groups, Unread
 * - Debounced search: 450ms delay to prevent excessive API calls
 * - Search scope configuration: Messages, Conversations, or Both
 * - UID/GUID filtering: Contextual search within specific conversations
 * - Pagination support for both conversations and messages
 *
 * @param fetchConversationsUseCase Use case for fetching conversations
 * @param fetchMessagesUseCase Use case for fetching messages
 */
open public class CometChatSearchViewModel(
    private val fetchConversationsUseCase: FetchConversationsUseCase,
    private val fetchMessagesUseCase: FetchMessagesUseCase
) : ViewModel() {

    public companion object {
        public const val DEFAULT_LIMIT: Int = 15
        public const val DEBOUNCE_DELAY_MS: Long = 450L
    }

    // UI State
    private val _uiState = MutableStateFlow<SearchUIState>(SearchUIState.Initial)
    public val uiState: StateFlow<SearchUIState> = _uiState.asStateFlow()

    // Conversations list
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    public val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // Messages list
    private val _messages = MutableStateFlow<List<BaseMessage>>(emptyList())
    public val messages: StateFlow<List<BaseMessage>> = _messages.asStateFlow()

    // Pagination flags
    private val _hasMoreConversations = MutableStateFlow(true)
    public val hasMoreConversations: StateFlow<Boolean> = _hasMoreConversations.asStateFlow()

    private val _hasMoreMessages = MutableStateFlow(true)
    public val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages.asStateFlow()

    // Selected filters
    private val _selectedFilters = MutableStateFlow<Set<SearchFilter>>(emptySet())
    public val selectedFilters: StateFlow<Set<SearchFilter>> = _selectedFilters.asStateFlow()

    // Visible filters - shows all 7 when no selection, or only same-group filters when selected
    private val _visibleFilters = MutableStateFlow<List<SearchFilter>>(SearchFilter.entries.toList())
    public val visibleFilters: StateFlow<List<SearchFilter>> = _visibleFilters.asStateFlow()

    // Search configuration
    private var searchScopes: List<SearchScope> = listOf(SearchScope.MESSAGES, SearchScope.CONVERSATIONS)
    private var uid: String? = null
    private var guid: String? = null

    // Request builders
    private var conversationsRequestBuilder: ConversationsRequest.ConversationsRequestBuilder? = null
    private var messagesRequestBuilder: MessagesRequest.MessagesRequestBuilder? = null

    // Built requests for pagination
    private var currentConversationsRequest: ConversationsRequest? = null
    private var currentMessagesRequest: MessagesRequest? = null

    // Debounce handling
    private var searchJob: Job? = null

    // Request state tracking
    private var isConversationRequestPending = false
    private var isMessageRequestPending = false
    private var conversationRequestFailed = false
    private var messageRequestFailed = false

    // Last search parameters for pagination
    private var lastSearchText: String = ""
    private var lastFilters: Set<SearchFilter> = emptySet()

    // Last error for error state
    private var lastException: CometChatException? = null

    private val _rowsKeptError = Channel<CometChatException>(8, BufferOverflow.DROP_OLDEST)

    /**
     * A search request failed online while results stay on screen (D84 rule 5, offline on only): the
     * view fires `onError` without showing its error state. Each failure is buffered until a view
     * collects it and is delivered once (a rotation does not repeat it). A not-connected failure
     * over results reports nothing (D84 rule 2). With nothing on screen the failure is
     * [SearchUIState.Error] instead. A search the kit re-runs by itself on reconnect never reports
     * here. Never emits with offline messaging off.
     */
    public val rowsKeptError: Flow<CometChatException> = _rowsKeptError.receiveAsFlow()

    // Offline (W7). A keyword has no cache (BEH-SNAP-04): offline, a search with no saved match
    // shows the "Connect to search" empty state instead of an error (D84 rule 7).
    private val _showConnectToSearch = MutableStateFlow(false)

    /**
     * True while the current [SearchUIState.Empty] means "connect to search": offline messaging is
     * on, the search could not reach the server, and nothing saved matched (D84 rule 7, no
     * `onError`). Always set before [uiState] moves to [SearchUIState.Empty], and false in every
     * other state. Never true with offline messaging off.
     */
    public val showConnectToSearch: StateFlow<Boolean> = _showConnectToSearch.asStateFlow()

    // D84 rule 8: a search showing "Connect to search" (or an error because it ran offline) runs
    // again, once, when the connection returns. Registers its listener on the first arm only (never
    // with offline off).
    private val reconnectRefill = OfflineReconnectRefill("CometChatSearch") {
        val state = _uiState.value
        val offlineMiss = state is SearchUIState.Error || (state is SearchUIState.Empty && _showConnectToSearch.value)
        if (offlineMiss && (lastSearchText.isNotEmpty() || lastFilters.isNotEmpty())) {
            search(lastSearchText, lastFilters, quiet = true)
        }
    }

    // The current search was started by the kit (the reconnect re-run): a failure while results
    // are on screen reports nothing (D84 rule 5). Set when the search executes.
    private var searchQuiet = false

    // The offline flag of the current search, read once in performSearch (G1).
    private var searchOfflineOn = false

    // A request of the current search failed online with offline on (D84 rule 5: fired when rows stay).
    private var offlineReadError: CometChatException? = null

    /**
     * The search generation. Bumped whenever the results a reply was fetched for stop being the
     * ones on screen (a new search, its execution after the debounce, [clear]); a two-phase reply
     * from another generation is dropped untouched. Only read and written on the main thread.
     */
    private var searchGen = 0L

    /**
     * Bumped only by a session end or cache reset (G8), so a plain (non two-phase) read already in
     * flight drops its result. Never moves with offline off, where those reads behave as today.
     */
    private var offlineSessionGen = 0L

    // G8: session end and cache reset (ENG-41568). Started only with offline on.
    private val offlineSession = OfflineListSession(
        onSessionEnded = { clearForOfflineSession() },
        onCacheReset = { onOfflineCacheReset() }
    )

    init {
        if (OfflineGate.isOn()) offlineSession.start(viewModelScope)
    }

    /** A keyword request of the current search failed with not-connected while offline was on. */
    private var offlineKeywordFailed = false

    /**
     * A request of the current search failed with an error other than not-connected. Then the
     * search is not an offline miss: it takes the normal error path (D84 rule 7 needs every failure
     * to be not-connected).
     */
    private var onlineRequestFailed = false

    /** Records a request failure for the D84 offline-miss decision. */
    private fun noteFailure(error: CometChatException?) {
        if (!SearchOffline.isNotConnected(error)) onlineRequestFailed = true
    }

    /**
     * Initiates a search for conversations and/or messages based on the search text and filters.
     * Implements 450ms debounce for text input, but executes immediately for filter changes.
     *
     * @param searchText The search query text
     * @param filters The set of selected filters
     */
    public fun searchConversationsAndMessages(searchText: String, filters: Set<SearchFilter>) {
        search(searchText, filters, quiet = false)
    }

    /** [searchConversationsAndMessages]; [quiet] for a search the kit re-runs by itself. */
    private fun search(searchText: String, filters: Set<SearchFilter>, quiet: Boolean) {
        if (searchText.isEmpty() && filters.isEmpty()) {
            clear()
            _uiState.value = SearchUIState.Initial
            return
        }

        // Skip re-execution if same search text and filters, and we already have results
        // This preserves expanded "See More" results across configuration changes
        // An offline "Connect to search" is not a result: the same search runs again (D84 rule 8).
        if (searchText == lastSearchText && filters == lastFilters && !_showConnectToSearch.value &&
            (_uiState.value is SearchUIState.Content || _uiState.value is SearchUIState.Empty)) {
            return
        }

        searchJob?.cancel()
        searchGen++

        val isFilterChange = _selectedFilters.value != filters
        _selectedFilters.value = filters

        searchJob = viewModelScope.launch {
            // Immediate execution for filter changes, debounced for text input
            if (!isFilterChange && searchText.isNotEmpty()) {
                delay(DEBOUNCE_DELAY_MS)
            }

            // A page fetched during the debounce belongs to the old results: drop it.
            searchGen++
            _showConnectToSearch.value = false
            _uiState.value = SearchUIState.Loading
            clearLists()

            lastSearchText = searchText
            lastFilters = filters
            searchQuiet = quiet

            val searchMode = getSearchMode(searchText, filters)
            performSearch(searchText, filters, searchMode)
        }
    }

    /**
     * Performs the actual search based on the determined search mode.
     */
    private suspend fun performSearch(
        searchText: String,
        filters: Set<SearchFilter>,
        searchMode: SearchMode
    ) {
        resetRequestStates()
        // Read the offline flag once for this search (G1) and thread it down both reads.
        val gen = searchGen
        val offlineOn = OfflineGate.isOn()
        searchOfflineOn = offlineOn

        when (searchMode) {
            SearchMode.BOTH -> {
                isConversationRequestPending = true
                isMessageRequestPending = true
                coroutineScope {
                    launch { fetchConversations(searchText, filters, gen, offlineOn) }
                    launch { fetchMessages(searchText, filters, gen, offlineOn) }
                }
            }
            SearchMode.CONVERSATIONS -> {
                isConversationRequestPending = true
                fetchConversations(searchText, filters, gen, offlineOn)
            }
            SearchMode.MESSAGES -> {
                isMessageRequestPending = true
                fetchMessages(searchText, filters, gen, offlineOn)
            }
            SearchMode.NONE -> {
                _uiState.value = SearchUIState.Empty
            }
        }
    }

    /**
     * Determines the search mode based on search text, filters, and configuration.
     *
     * @param searchText The search query text
     * @param filters The set of selected filters
     * @return The appropriate SearchMode
     */
    public fun getSearchMode(searchText: String, filters: Set<SearchFilter>): SearchMode {
        val messageFilters = filters.filter { it.isMessageFilter() }
        val conversationFilters = filters.filter { it.isConversationFilter() }

        val hasSearchText = searchText.isNotEmpty()
        val hasMessageFilters = messageFilters.isNotEmpty()
        val hasConversationFilters = conversationFilters.isNotEmpty()

        // UID/GUID forces messages-only search
        if (uid != null || guid != null) {
            return SearchMode.MESSAGES
        }

        val scopeConversation = searchScopes.contains(SearchScope.CONVERSATIONS)
        val scopeMessage = searchScopes.contains(SearchScope.MESSAGES)

        return when {
            // Search text without filters - use configured scopes
            hasSearchText && !hasMessageFilters && !hasConversationFilters -> {
                when {
                    scopeConversation && scopeMessage -> SearchMode.BOTH
                    scopeConversation -> SearchMode.CONVERSATIONS
                    scopeMessage -> SearchMode.MESSAGES
                    else -> SearchMode.NONE
                }
            }
            // No search text, only conversation filters
            !hasSearchText && hasConversationFilters && !hasMessageFilters -> SearchMode.CONVERSATIONS
            // No search text, only message filters
            !hasSearchText && !hasConversationFilters && hasMessageFilters -> SearchMode.MESSAGES
            // Search text with conversation filters only
            hasSearchText && hasConversationFilters && !hasMessageFilters -> SearchMode.CONVERSATIONS
            // Search text with message filters only
            hasSearchText && !hasConversationFilters && hasMessageFilters -> SearchMode.MESSAGES
            // Both filter types selected - this is a conflict, return NONE
            else -> SearchMode.NONE
        }
    }

    /**
     * Fetches conversations based on search text and filters.
     */
    private suspend fun fetchConversations(
        searchText: String,
        filters: Set<SearchFilter>,
        gen: Long,
        offlineOn: Boolean
    ) {
        // Use the integrator-supplied builder when one was set, so setConversationsRequestBuilder
        // actually scopes the search. Previously both branches constructed a fresh default builder,
        // which silently discarded the caller's.
        val builder = conversationsRequestBuilder ?: ConversationsRequest.ConversationsRequestBuilder()

        configureConversationRequest(builder, searchText, filters)
        currentConversationsRequest = builder.build()

        val twoPhase = fetchConversationsUseCase.twoPhaseRepository
        if (twoPhase != null) {
            currentConversationsRequest?.let { request ->
                collectConversations(
                    twoPhase.getConversationsTwoPhase(request),
                    ReadOp(gen, offlineOn, hasConversationKeyword(searchText), page = false),
                    unreadOnly = filters.contains(SearchFilter.UNREAD)
                )
            }
            return
        }

        currentConversationsRequest?.let { request ->
            val session = offlineSessionGen
            val result = fetchConversationsUseCase(request)
            // G8: dropped after a session end or cache reset (offline on only).
            if (session != offlineSessionGen) return
            result
                .onSuccess { conversations ->
                    // Filter out conversations with no last message when UNREAD filter is active
                    // The SDK returns joined groups with system-only "unread" counts (join notifications)
                    // but no actual chat messages — these appear as empty conversations in the UI
                    val filteredConversations = if (filters.contains(SearchFilter.UNREAD)) {
                        conversations.filter { it.lastMessage != null }
                    } else {
                        conversations
                    }
                    _conversations.value = _conversations.value + filteredConversations
                    _hasMoreConversations.value = fetchConversationsUseCase.hasMore()
                    isConversationRequestPending = false
                    conversationRequestFailed = false
                    updateFinalStateIfCompleted()
                }
                .onFailure { e ->
                    isConversationRequestPending = false
                    conversationRequestFailed = true
                    lastException = e as? CometChatException
                    noteFailure(e as? CometChatException)
                    updateFinalStateIfCompleted()
                }
        }
    }

    /**
     * Fetches messages based on search text and filters.
     */
    private suspend fun fetchMessages(
        searchText: String,
        filters: Set<SearchFilter>,
        gen: Long,
        offlineOn: Boolean
    ) {
        // Use the integrator-supplied builder when one was set — see fetchConversations above.
        val builder = messagesRequestBuilder ?: MessagesRequest.MessagesRequestBuilder()

        configureMessageRequest(builder, searchText, filters)
        currentMessagesRequest = builder.build()

        val twoPhase = fetchMessagesUseCase.twoPhaseRepository
        if (twoPhase != null) {
            currentMessagesRequest?.let { request ->
                collectMessages(
                    twoPhase.getMessagesTwoPhase(request),
                    ReadOp(gen, offlineOn, hasMessageKeyword(searchText), page = false)
                )
            }
            return
        }

        currentMessagesRequest?.let { request ->
            val session = offlineSessionGen
            val result = fetchMessagesUseCase(request)
            // G8: dropped after a session end or cache reset (offline on only).
            if (session != offlineSessionGen) return
            result
                .onSuccess { messages ->
                    // Reverse messages for chronological display
                    _messages.value = _messages.value + messages.reversed()
                    _hasMoreMessages.value = fetchMessagesUseCase.hasMore()
                    isMessageRequestPending = false
                    messageRequestFailed = false
                    updateFinalStateIfCompleted()
                }
                .onFailure { e ->
                    isMessageRequestPending = false
                    messageRequestFailed = true
                    lastException = e as? CometChatException
                    noteFailure(e as? CometChatException)
                    updateFinalStateIfCompleted()
                }
        }
    }

    /**
     * Configures the conversation request builder with search text and filters.
     */
    private fun configureConversationRequest(
        builder: ConversationsRequest.ConversationsRequestBuilder,
        searchText: String,
        filters: Set<SearchFilter>
    ) {
        // Adjust limit based on whether filters are present
        val limit = if (filters.isEmpty() && searchText.isNotEmpty()) 3 else DEFAULT_LIMIT
        builder.setLimit(limit)

        if (searchText.isNotEmpty() && searchScopes.contains(SearchScope.CONVERSATIONS)) {
            builder.setSearchKeyword(searchText)
        } else {
            builder.setSearchKeyword(null)
        }

        // Apply conversation filters
        applyConversationFilters(builder, filters)
    }

    /**
     * Applies conversation filters to the request builder.
     * Matches the reference Java implementation.
     */
    private fun applyConversationFilters(
        builder: ConversationsRequest.ConversationsRequestBuilder,
        filters: Set<SearchFilter>
    ) {
        // Set unread filter
        builder.setUnread(filters.contains(SearchFilter.UNREAD))

        // Set conversation type filter for groups
        if (filters.contains(SearchFilter.GROUPS)) {
            builder.setConversationType(CometChatConstants.CONVERSATION_TYPE_GROUP)
        } else {
            builder.setConversationType(null)
        }
    }

    /**
     * Configures the message request builder with search text and filters.
     */
    private fun configureMessageRequest(
        builder: MessagesRequest.MessagesRequestBuilder,
        searchText: String,
        filters: Set<SearchFilter>
    ) {
        // Adjust limit based on whether filters are present
        val limit = if (filters.isEmpty() && searchText.isNotEmpty()) 3 else DEFAULT_LIMIT
        builder.setLimit(limit)

        // Set default message types when no filters
        builder.setTypes(listOf(
            UIKitConstants.MessageType.AUDIO,
            UIKitConstants.MessageType.FILE,
            UIKitConstants.MessageType.IMAGE,
            UIKitConstants.MessageType.VIDEO,
            UIKitConstants.MessageType.TEXT
        ))
        builder.hideDeletedMessages(true)

        // Apply UID/GUID if set
        uid?.let { builder.setUID(it) }
        guid?.let { builder.setGUID(it) }

        if (searchText.isNotEmpty() && searchScopes.contains(SearchScope.MESSAGES)) {
            builder.setSearchKeyword(searchText)
        } else {
            builder.setSearchKeyword(null)
        }

        // Apply message filters
        applyMessageFilters(builder, filters)
    }

    /**
     * Applies message filters to the request builder.
     * Matches the reference Java implementation using AttachmentTypes and hasLinks.
     */
    private fun applyMessageFilters(
        builder: MessagesRequest.MessagesRequestBuilder,
        filters: Set<SearchFilter>
    ) {
        val attachmentTypes = mutableListOf<AttachmentType>()

        if (filters.contains(SearchFilter.PHOTOS)) {
            attachmentTypes.add(AttachmentType.IMAGE)
        }
        if (filters.contains(SearchFilter.VIDEOS)) {
            attachmentTypes.add(AttachmentType.VIDEO)
        }
        if (filters.contains(SearchFilter.DOCUMENTS)) {
            attachmentTypes.add(AttachmentType.FILE)
        }
        if (filters.contains(SearchFilter.AUDIO)) {
            attachmentTypes.add(AttachmentType.AUDIO)
        }

        if (attachmentTypes.isNotEmpty()) {
            builder.setAttachmentTypes(attachmentTypes)
            builder.hasLinks(false)
        } else {
            builder.setAttachmentTypes(null)
        }

        // Handle LINKS filter separately - it overrides attachment types
        if (filters.contains(SearchFilter.LINKS)) {
            builder.setAttachmentTypes(null)
            builder.hasLinks(true)
        } else if (attachmentTypes.isEmpty()) {
            builder.hasLinks(false)
        }

        // Set message types based on filters
        val filteredMessageTypes = getMessageTypesFromFilters(filters)
        if (filteredMessageTypes.isNotEmpty()) {
            builder.setTypes(filteredMessageTypes)
        } else {
            builder.setTypes(null)
        }
    }

    /**
     * Gets message types from the selected filters.
     */
    private fun getMessageTypesFromFilters(filters: Set<SearchFilter>): List<String> {
        val messageTypes = mutableListOf<String>()
        for (filter in filters) {
            when (filter) {
                SearchFilter.PHOTOS -> messageTypes.add(UIKitConstants.MessageType.IMAGE)
                SearchFilter.VIDEOS -> messageTypes.add(UIKitConstants.MessageType.VIDEO)
                SearchFilter.DOCUMENTS -> messageTypes.add(UIKitConstants.MessageType.FILE)
                SearchFilter.AUDIO -> messageTypes.add(UIKitConstants.MessageType.AUDIO)
                SearchFilter.LINKS -> messageTypes.add(UIKitConstants.MessageType.TEXT)
                else -> { /* Not a message filter */ }
            }
        }
        return messageTypes
    }

    /**
     * Updates the final UI state after all pending requests complete.
     */
    private fun updateFinalStateIfCompleted() {
        if (isConversationRequestPending || isMessageRequestPending) return

        val nothingShown = _conversations.value.isEmpty() && _messages.value.isEmpty()
        val bothFailed = conversationRequestFailed && messageRequestFailed
        // D84 rule 7: an offline search with no saved match on screen is "Connect to search" (an
        // offline state, no onError): a keyword that could not reach the server, or both requests
        // failed — and every failure was not-connected. Any other failure takes the normal path.
        val connectToSearch = searchOfflineOn && nothingShown && !onlineRequestFailed &&
            (offlineKeywordFailed || bothFailed)
        _showConnectToSearch.value = connectToSearch
        _uiState.value = when {
            connectToSearch -> SearchUIState.Empty
            bothFailed -> {
                SearchUIState.Error(lastException ?: CometChatException(
                    "SEARCH_ERROR",
                    "Search failed",
                    "Both conversation and message search failed"
                ))
            }
            nothingShown -> SearchUIState.Empty
            else -> SearchUIState.Content
        }
        if (searchOfflineOn) {
            val state = _uiState.value
            if (connectToSearch || (state is SearchUIState.Error && SearchOffline.isNotConnected(state.exception))) {
                // D84 rule 8: read again, once, on reconnect.
                reconnectRefill.arm()
            } else {
                reconnectRefill.disarm()
                // D84 rule 5: results stay on screen; an online failure still reaches onError.
                if (state is SearchUIState.Content && !searchQuiet) offlineReadError?.let { _rowsKeptError.trySend(it) }
            }
        }
    }

    /**
     * Resets request state tracking flags.
     */
    private fun resetRequestStates() {
        isConversationRequestPending = false
        isMessageRequestPending = false
        conversationRequestFailed = false
        messageRequestFailed = false
        lastException = null
        offlineKeywordFailed = false
        onlineRequestFailed = false
        offlineReadError = null
    }

    /**
     * Fetches more conversations for pagination.
     */
    public fun fetchMoreConversations() {
        if (!_hasMoreConversations.value || isConversationRequestPending) return
        val gen = searchGen

        viewModelScope.launch {
            isConversationRequestPending = true

            val twoPhase = fetchConversationsUseCase.twoPhaseRepository
            val pageRequest = currentConversationsRequest
            if (twoPhase != null && pageRequest != null) {
                collectConversations(
                    twoPhase.getConversationsTwoPhase(pageRequest),
                    ReadOp(gen, OfflineGate.isOn(), hasConversationKeyword(lastSearchText), page = true),
                    unreadOnly = _selectedFilters.value.contains(SearchFilter.UNREAD)
                )
                return@launch
            }

            currentConversationsRequest?.let { request ->
                val session = offlineSessionGen
                val result = fetchConversationsUseCase(request)
                // G8: dropped after a session end or cache reset (offline on only).
                if (session != offlineSessionGen) return@launch
                result
                    .onSuccess { conversations ->
                        // Filter out conversations with no last message when UNREAD filter is active
                        val filteredConversations = if (_selectedFilters.value.contains(SearchFilter.UNREAD)) {
                            conversations.filter { it.lastMessage != null }
                        } else {
                            conversations
                        }
                        _conversations.value = _conversations.value + filteredConversations
                        _hasMoreConversations.value = fetchConversationsUseCase.hasMore()
                        isConversationRequestPending = false
                    }
                    .onFailure {
                        isConversationRequestPending = false
                    }
            } ?: run {
                isConversationRequestPending = false
            }
        }
    }

    /**
     * Fetches more messages for pagination.
     */
    public fun fetchMoreMessages() {
        if (!_hasMoreMessages.value || isMessageRequestPending) return
        val gen = searchGen

        viewModelScope.launch {
            isMessageRequestPending = true

            val twoPhase = fetchMessagesUseCase.twoPhaseRepository
            val pageRequest = currentMessagesRequest
            if (twoPhase != null && pageRequest != null) {
                collectMessages(
                    twoPhase.getMessagesTwoPhase(pageRequest),
                    ReadOp(gen, OfflineGate.isOn(), hasMessageKeyword(lastSearchText), page = true)
                )
                return@launch
            }

            currentMessagesRequest?.let { request ->
                val session = offlineSessionGen
                val result = fetchMessagesUseCase(request)
                // G8: dropped after a session end or cache reset (offline on only).
                if (session != offlineSessionGen) return@launch
                result
                    .onSuccess { messages ->
                        // Reverse messages for chronological display
                        _messages.value = _messages.value + messages.reversed()
                        _hasMoreMessages.value = fetchMessagesUseCase.hasMore()
                        isMessageRequestPending = false
                    }
                    .onFailure {
                        isMessageRequestPending = false
                    }
            } ?: run {
                isMessageRequestPending = false
            }
        }
    }

    /**
     * Sets the search scopes to determine which types of data to search.
     *
     * @param scopes List of SearchScope values (MESSAGES, CONVERSATIONS, or both)
     */
    public fun setSearchScopes(scopes: List<SearchScope>) {
        searchScopes = scopes
    }

    /**
     * Sets the UID for contextual search within a specific user conversation.
     * When set, only messages will be searched (conversations are ignored).
     *
     * @param uid The user ID to filter messages by, or null to clear
     */
    public fun setUid(uid: String?) {
        this.uid = uid
        updateVisibleFiltersForContext()
    }

    /**
     * Sets the GUID for contextual search within a specific group conversation.
     * When set, only messages will be searched (conversations are ignored).
     *
     * @param guid The group ID to filter messages by, or null to clear
     */
    public fun setGuid(guid: String?) {
        this.guid = guid
        updateVisibleFiltersForContext()
    }

    /**
     * Updates visible filters based on uid/guid context.
     * When uid or guid is set, conversation filters (GROUPS, UNREAD) are excluded
     * since only message search is applicable in contextual search.
     */
    private fun updateVisibleFiltersForContext() {
        if (uid != null || guid != null) {
            // In contextual search (uid/guid), only message filters are relevant
            _visibleFilters.value = SearchFilter.entries.filter { it.isMessageFilter() }
            // Clear any selected conversation filters that are no longer valid
            val currentFilters = _selectedFilters.value
            val invalidFilters = currentFilters.filter { it.isConversationFilter() }
            if (invalidFilters.isNotEmpty()) {
                _selectedFilters.value = currentFilters - invalidFilters.toSet()
            }
        } else {
            // Global search - show all filters (unless a filter group is already selected)
            if (_selectedFilters.value.isEmpty()) {
                _visibleFilters.value = SearchFilter.entries.toList()
            }
        }
    }

    /**
     * Sets a custom ConversationsRequest builder for customizing conversation fetch parameters.
     *
     * @param builder The custom request builder
     */
    public fun setConversationsRequestBuilder(builder: ConversationsRequest.ConversationsRequestBuilder) {
        conversationsRequestBuilder = builder
    }

    /**
     * Sets a custom MessagesRequest builder for customizing message fetch parameters.
     *
     * @param builder The custom request builder
     */
    public fun setMessagesRequestBuilder(builder: MessagesRequest.MessagesRequestBuilder) {
        messagesRequestBuilder = builder
    }

    /**
     * Toggles a filter on or off with group-based visibility logic.
     *
     * When clicking a filter from a different group than currently selected filters:
     * 1. Clear all selected filters first
     * 2. Then add the clicked filter
     *
     * When clicking a filter from the same group:
     * - Toggle the filter (add if not selected, remove if selected)
     *
     * After toggling, updates visible filters to show only same-group filters
     * or all filters if no selection.
     *
     * @param filter The filter to toggle
     */
    public fun toggleFilter(filter: SearchFilter) {
        val currentFilters = _selectedFilters.value.toMutableSet()
        val clickedGroup = filter.group

        // Check if we have any selected filters from a different group
        val hasFiltersFromDifferentGroup = currentFilters.any { it.group != clickedGroup }

        if (hasFiltersFromDifferentGroup) {
            // Clear all filters when selecting from a new group
            currentFilters.clear()
            currentFilters.add(filter)
        } else {
            // Same group or no filters selected - toggle normally
            if (currentFilters.contains(filter)) {
                currentFilters.remove(filter)
            } else {
                currentFilters.add(filter)
            }
        }

        // Update visible filters based on current selection
        updateVisibleFilters(currentFilters)

        searchConversationsAndMessages(lastSearchText, currentFilters)
    }

    /**
     * Updates the visible filters based on the current selection.
     *
     * - When no filters are selected: show all 7 filters
     * - When filters are selected: show only filters from the same group
     *
     * @param currentFilters The current set of selected filters
     */
    private fun updateVisibleFilters(currentFilters: Set<SearchFilter>) {
        if (currentFilters.isEmpty()) {
            // No selection - show all filters
            _visibleFilters.value = SearchFilter.entries.toList()
        } else {
            // Get the group of the selected filters (all should be same group)
            val selectedGroup = currentFilters.first().group
            _visibleFilters.value = SearchFilter.getFiltersInGroup(selectedGroup)
        }
    }

    /**
     * Clears all search state including lists, filters, and request state.
     */
    public fun clear() {
        searchJob?.cancel()
        searchGen++
        _showConnectToSearch.value = false
        reconnectRefill.disarm()
        clearLists()
        _selectedFilters.value = emptySet()
        _visibleFilters.value = SearchFilter.entries.toList()
        resetRequestStates()
        currentConversationsRequest = null
        currentMessagesRequest = null
        lastSearchText = ""
        lastFilters = emptySet()
    }

    /**
     * Clears both result sets (conversations and messages) for a session change (logout or a switch
     * to a different uid), before the next frame (G8). Bumps [searchGen] and [offlineSessionGen], so
     * a search or "see more" page already in flight drops its result, cancels a pending search,
     * empties both lists and resets paging and requests. The screen goes back to
     * [SearchUIState.Initial] (nothing searched); the selected filter chips are kept. Offline on only
     * (the hub drives it).
     */
    private fun clearForOfflineSession() {
        searchJob?.cancel()
        searchGen++
        offlineSessionGen++
        _showConnectToSearch.value = false
        reconnectRefill.disarm()
        clearLists()
        resetRequestStates()
        currentConversationsRequest = null
        currentMessagesRequest = null
        lastSearchText = ""
        lastFilters = emptySet()
        _uiState.value = SearchUIState.Initial
    }

    /**
     * The offline cache was reset (G8): clear as for a session end, then run the search on screen
     * again, once, from page one. The kit starts it, so a failure over results stays quiet (D84
     * rule 5). Nothing runs if nothing was searched.
     */
    private fun onOfflineCacheReset() {
        val text = lastSearchText
        val filters = lastFilters
        clearForOfflineSession()
        if (text.isNotEmpty() || filters.isNotEmpty()) search(text, filters, quiet = true)
    }

    /**
     * Clears the conversation and message lists and resets pagination flags.
     */
    private fun clearLists() {
        _conversations.value = emptyList()
        _messages.value = emptyList()
        _hasMoreConversations.value = true
        _hasMoreMessages.value = true
    }

    // ==================== Two-phase reads (W7) ====================

    /**
     * One two-phase read: its search generation, the offline flag read once for it (G1), whether
     * its request carries a keyword (server only, BEH-SNAP-04), and whether it is a "see more" page.
     */
    private class ReadOp(val gen: Long, val offlineOn: Boolean, val hasKeyword: Boolean, val page: Boolean)

    private fun hasConversationKeyword(searchText: String): Boolean =
        searchText.isNotEmpty() && searchScopes.contains(SearchScope.CONVERSATIONS)

    private fun hasMessageKeyword(searchText: String): Boolean =
        searchText.isNotEmpty() && searchScopes.contains(SearchScope.MESSAGES)

    private suspend fun collectConversations(
        phases: Flow<TwoPhaseResult<Conversation>>,
        op: ReadOp,
        unreadOnly: Boolean
    ) {
        // Today's transform: with UNREAD, drop joined groups that have no real last message.
        val transform: (List<Conversation>) -> List<Conversation> =
            if (unreadOnly) { rows -> rows.filter { it.lastMessage != null } } else { rows -> rows }
        collectTwoPhase(
            phases, op, _conversations, _hasMoreConversations, SearchOffline.conversationSpec, transform,
            hasMore = { fetchConversationsUseCase.hasMore() }
        ) { failed, error, offlineKeyword ->
            isConversationRequestPending = false
            if (!op.page) {
                conversationRequestFailed = failed
                if (failed) { lastException = error; noteFailure(error) }
                if (offlineKeyword) offlineKeywordFailed = true
                updateFinalStateIfCompleted()
            }
        }
    }

    private suspend fun collectMessages(phases: Flow<TwoPhaseResult<BaseMessage>>, op: ReadOp) {
        // Today's transform: a fetchPrevious page is oldest-first; the screen shows newest-first.
        collectTwoPhase(
            phases, op, _messages, _hasMoreMessages, SearchOffline.messageSpec, { it.reversed() },
            hasMore = { fetchMessagesUseCase.hasMore() }
        ) { failed, error, offlineKeyword ->
            isMessageRequestPending = false
            if (!op.page) {
                messageRequestFailed = failed
                if (failed) { lastException = error; noteFailure(error) }
                if (offlineKeyword) offlineKeywordFailed = true
                updateFinalStateIfCompleted()
            }
        }
    }

    /**
     * Applies one two-phase read to [rows] (README §3.9 "Search" / "Message filters without a
     * keyword"; BEH-SNAP-04, AC-10.1/10.2/10.3):
     * - A reply from another [searchGen] is dropped untouched.
     * - **Offline off**: only the server phase arrives; it is appended exactly as today.
     * - **Keyword** (offline on): server only — the cached phase is ignored, the server page is
     *   appended without repeats; a not-connected error with nothing found is "Connect to search" (D84).
     * - **No keyword** (offline on): the cached page paints first, the server page reconciles it
     *   by id ([OfflineListReconcile]); a not-connected error after the cached phase means the
     *   search was answered from the cache — not a failure.
     *
     * [onDone] runs once, on the server phase or the error: `(failed, error, offlineKeyword)`.
     */
    private suspend fun <T, ID : Comparable<ID>> collectTwoPhase(
        phases: Flow<TwoPhaseResult<T>>,
        op: ReadOp,
        rows: MutableStateFlow<List<T>>,
        hasMoreFlow: MutableStateFlow<Boolean>,
        spec: OfflineListReconcile.Spec<T, ID>,
        transform: (List<T>) -> List<T>,
        hasMore: () -> Boolean,
        onDone: (failed: Boolean, error: CometChatException?, offlineKeyword: Boolean) -> Unit
    ) {
        val reconciled = op.offlineOn && !op.hasKeyword
        var cachedKeys: Set<Any> = emptySet()
        var cachedDelivered = false
        phases.collect { phase ->
            if (op.gen != searchGen) return@collect
            when (phase) {
                is TwoPhaseResult.Cached -> {
                    if (!reconciled) return@collect
                    val out = OfflineListReconcile.reconcile(
                        rows.value, op.gen,
                        OfflineListReconcile.Op(op.gen, OfflineListReconcile.Phase.CACHED, transform(phase.rows)),
                        spec, hasMoreFlow.value
                    )
                    cachedKeys = out.cachedKeys
                    cachedDelivered = true
                    rows.value = out.rows
                    // Fast first paint; the final state still waits for every request to finish.
                    if (!op.page && out.rows.isNotEmpty()) _uiState.value = SearchUIState.Content
                }
                is TwoPhaseResult.Server -> {
                    val page = transform(phase.rows)
                    rows.value = when {
                        !op.offlineOn -> rows.value + page
                        op.hasKeyword -> SearchOffline.appendDistinct(rows.value, page, spec.keyOf)
                        else -> OfflineListReconcile.reconcile(
                            rows.value, op.gen,
                            OfflineListReconcile.Op(op.gen, OfflineListReconcile.Phase.SERVER, page, cachedKeys),
                            spec, hasMoreFlow.value
                        ).rows
                    }
                    hasMoreFlow.value = hasMore()
                    onDone(false, null, false)
                }
                is TwoPhaseResult.Error -> {
                    val notConnected = op.offlineOn && SearchOffline.isNotConnected(phase.exception)
                    // D84 rules 2 and 5: only an online failure is reported over results kept on screen.
                    if (op.offlineOn && !op.page && !notConnected) offlineReadError = phase.exception
                    if (notConnected && cachedDelivered) {
                        onDone(false, null, false)
                    } else {
                        onDone(true, phase.exception, notConnected && op.hasKeyword)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        reconnectRefill.release()
    }
}
