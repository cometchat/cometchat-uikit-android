package com.cometchat.uikit.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.GroupsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Action
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.domain.repository.GroupsTwoPhaseRepository
import com.cometchat.uikit.core.domain.usecase.FetchGroupsUseCase
import com.cometchat.uikit.core.domain.usecase.JoinGroupUseCase
import com.cometchat.uikit.core.events.CometChatGroupEvent
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineHub
import com.cometchat.uikit.core.offline.OfflineListSession
import com.cometchat.uikit.core.offline.SnapshotListRead
import com.cometchat.uikit.core.offline.SnapshotReadOp
import com.cometchat.uikit.core.state.GroupsUIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing groups list state.
 * Implements ListOperations for standardized list manipulation.
 * Uses StateFlow for reactive state management.
 *
 * @param fetchGroupsUseCase Use case for fetching groups
 * @param joinGroupUseCase Use case for joining groups
 * @param enableListeners Whether to enable CometChat listeners (set to false for testing)
 */
open public class CometChatGroupsViewModel(
    private val fetchGroupsUseCase: FetchGroupsUseCase,
    private val joinGroupUseCase: JoinGroupUseCase,
    private val enableListeners: Boolean = true
) : ViewModel(), ListOperations<Group> {

    // UI State
    private val _uiState = MutableStateFlow<GroupsUIState>(GroupsUIState.Loading)
    public val uiState: StateFlow<GroupsUIState> = _uiState.asStateFlow()

    // Groups list
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    public val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    // Selection state
    private val _selectedGroups = MutableStateFlow<Set<Group>>(emptySet())
    public val selectedGroups: StateFlow<Set<Group>> = _selectedGroups.asStateFlow()

    // Events
    private val _groupJoinedEvent = MutableSharedFlow<Group>()
    public val groupJoinedEvent: SharedFlow<Group> = _groupJoinedEvent.asSharedFlow()

    private val _groupCreatedEvent = MutableSharedFlow<Group>()
    public val groupCreatedEvent: SharedFlow<Group> = _groupCreatedEvent.asSharedFlow()

    // Scroll to top event
    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    public val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    // List operations delegate
    private val listDelegate = ListOperationsDelegate(
        stateFlow = _groups,
        equalityChecker = { a, b -> a.guid == b.guid }
    )

    // Configuration
    private var groupsRequest: GroupsRequest? = null
    private var groupsRequestBuilder: GroupsRequest.GroupsRequestBuilder? = null
    private var searchRequestBuilder: GroupsRequest.GroupsRequestBuilder? = null
    private var isFetching = false
    private var hasMoreData = true
    private var listenersTag: String? = null

    // Local event listener job
    private var groupEventsJob: Job? = null
    
    // Search debounce job - cancels pending search when new search is initiated
    private var searchJob: Job? = null

    // Offline (BEH-SNAP-01..07): read once at construction (before init's first fetch); the OFF
    // path below is today's code.
    internal val offlineOn: Boolean = OfflineGate.isOn()

    // List generation: bumped by every read that replaces the list (refresh, search), so a phase of
    // an older read is dropped (BEH-READ-01). Used on the ON path only.
    private var listGen = 0L

    private val _rowsKeptError = Channel<CometChatException>(8, BufferOverflow.DROP_OLDEST)

    /**
     * A read failed online while rows stay on screen (D84 rule 5, offline on only): the list keeps
     * them and the view fires `onError` without showing its error state. Each failure is buffered
     * until a view collects it and is delivered once (a view that starts later still gets it; a
     * rotation does not repeat it). Only a window load the user or app starts reports here (first
     * page, refresh, keyword search), never a load-more page and never a refresh the kit starts by
     * itself on reconnect. A not-connected failure over rows reports nothing (D84 rule 2). With
     * nothing on screen the failure is [GroupsUIState.Error] instead. Never emits with offline
     * messaging off.
     */
    public val rowsKeptError: Flow<CometChatException> = _rowsKeptError.receiveAsFlow()

    private val _connectToSearch = MutableStateFlow(false)

    /**
     * True while [uiState] is [GroupsUIState.Empty] because a search keyword was read offline: a
     * keyword read has no cache (BEH-SNAP-04), so the empty state reads "Connect to search" instead
     * of "no groups" (D84 rule 7, no `onError`). Always false with offline messaging off.
     */
    public val connectToSearch: StateFlow<Boolean> = _connectToSearch.asStateFlow()

    private val _connectToLoad = MutableStateFlow(false)

    /**
     * True while [uiState] is [GroupsUIState.Error] because the list was read offline with nothing
     * on screen (D84 rule 3): the kit shows its offline state ("Connect to load groups", no Retry)
     * unless the app set an error view, which shows instead; `onError` fires once through the
     * Error state. The list reloads by itself on reconnect. Always false with offline messaging off.
     */
    public val connectToLoad: StateFlow<Boolean> = _connectToLoad.asStateFlow()

    // D84 rule 8: the keyword whose offline read is showing "Connect to search"; reconnect re-runs
    // that search instead of the plain-list refresh. Null otherwise.
    private var offlineErrorKeyword: String? = null

    // OFF-path load generation (ENG-41551 E): bumped by every search and refresh, so a list load
    // started for an older keyword never reaches the list. The ON path uses [listGen].
    private var offGen = 0L

    // The generation whose fetchGroups() set isFetching; a superseded load clears it only if it
    // still owns it.
    private var fetchOwnerGen = -1L

    // OFF path (ENG-41551 E): a keyword search is in flight. A load-more would read the previous
    // list (or, with the request reset, its page 1) into the search results, so fetchGroups()
    // waits until the search lands.
    private var offSearchInFlight = false

    private val groupsOnPath: Boolean
        get() = offlineOn && fetchGroupsUseCase.twoPhaseRepository != null

    // G8: session end and cache reset (ENG-41568). Started only with offline on.
    private val offlineSession = OfflineListSession(
        onSessionEnded = { clearForOfflineSession() },
        onCacheReset = { onOfflineCacheReset() }
    )

    init {
        if (enableListeners) {
            addListeners()
            if (offlineOn) offlineSession.start(viewModelScope)
        }
        fetchGroups()
    }

    // ==================== ListOperations Implementation ====================

    override fun addItem(item: Group): Unit = listDelegate.addItem(item)
    override fun addItems(items: List<Group>): Unit = listDelegate.addItems(items)
    override fun removeItem(item: Group): Boolean = listDelegate.removeItem(item)
    override fun removeItemAt(index: Int): Group? = listDelegate.removeItemAt(index)
    override fun updateItem(item: Group, predicate: (Group) -> Boolean): Boolean =
        listDelegate.updateItem(item, predicate)
    override fun clearItems(): Unit = listDelegate.clearItems()
    override fun getItems(): List<Group> = listDelegate.getItems()
    override fun getItemAt(index: Int): Group? = listDelegate.getItemAt(index)
    override fun getItemCount(): Int = listDelegate.getItemCount()
    override fun moveItemToTop(item: Group): Unit = listDelegate.moveItemToTop(item)
    override fun batch(operations: ListOperationsBatchScope<Group>.() -> Unit): Unit =
        listDelegate.batch(operations)


    // ==================== Public API Methods ====================

    /**
     * Fetches groups with pagination support.
     * Shows loading state only on initial fetch.
     * Prevents concurrent fetches using isFetching flag.
     */
    public fun fetchGroups() {
        if (isFetching || !hasMoreData) return
        // ENG-41551 E: offline off, no page of the previous list while a keyword search runs.
        if (offSearchInFlight && !groupsOnPath) return
        val gen = offGen

        viewModelScope.launch {
            isFetching = true
            fetchOwnerGen = gen

            if (_groups.value.isEmpty()) {
                _uiState.value = GroupsUIState.Loading
            }

            if (groupsRequest == null) {
                val builder = groupsRequestBuilder
                    ?: GroupsRequest.GroupsRequestBuilder().setLimit(30)
                groupsRequest = builder.build()
            }

            val twoPhase = if (offlineOn) fetchGroupsUseCase.twoPhaseRepository else null
            val pageRequest = groupsRequest
            if (twoPhase != null && pageRequest != null) {
                readTwoPhase(twoPhase, pageRequest, replace = false)
                return@launch
            }

            groupsRequest?.let { request ->
                val result = fetchGroupsUseCase(request)
                if (gen != offGen) {
                    // ENG-41551 E: a search or refresh superseded this load; its page is dropped.
                    if (fetchOwnerGen == gen) isFetching = false
                    return@launch
                }
                result
                    .onSuccess { newGroups ->
                        if (newGroups.isNotEmpty()) {
                            // Create defensive copies to prevent SDK from mutating our list items
                            val copiedGroups = newGroups.map { createGroupCopy(it) }
                            val updatedList = _groups.value + copiedGroups
                            _groups.value = updatedList
                            _uiState.value = GroupsUIState.Content(updatedList)
                        } else {
                            hasMoreData = false
                            if (_groups.value.isEmpty()) {
                                _uiState.value = GroupsUIState.Empty
                            }
                        }
                    }
                    .onFailure { exception ->
                        _uiState.value = GroupsUIState.Error(exception as CometChatException)
                    }

                isFetching = false
            } ?: run {
                isFetching = false
            }
        }
    }

    /**
     * Searches for groups based on the provided keyword.
     * Implements seamless search experience with:
     * - Debouncing (300ms) to reduce API calls during rapid typing
     * - Keeps existing content visible during search (no flicker)
     * - Conditional loading state (only when list is empty)
     * - Atomic list replacement when results arrive
     *
     * @param query The search keyword for finding groups
     */
    public fun searchGroups(query: String?) {
        // Cancel any pending search job (debouncing)
        searchJob?.cancel()
        // ENG-41551 E: a new keyword supersedes every earlier OFF-path load (a plain-list page
        // started by clearing the keyword included), so only this keyword's results land.
        val gen = ++offGen
        offSearchInFlight = !query.isNullOrEmpty() && !groupsOnPath

        searchJob = viewModelScope.launch {
            // Reset pagination state for new search
            hasMoreData = true
            groupsRequest = null
            
            val twoPhase = if (offlineOn) fetchGroupsUseCase.twoPhaseRepository else null
            if (query.isNullOrEmpty() && twoPhase != null) {
                // ON path: back to the plain list without blanking it first.
                listGen++
                isFetching = true
                val request = (groupsRequestBuilder ?: GroupsRequest.GroupsRequestBuilder().setLimit(30)).build()
                groupsRequest = request
                readTwoPhase(twoPhase, request, replace = true)
                return@launch
            }

            if (query.isNullOrEmpty()) {
                // Reset to normal fetch - clear list and fetch fresh
                _groups.value = emptyList()
                isFetching = false
                fetchGroups()
                return@launch
            }
            
            // Debounce: wait 300ms before making API call
            // This reduces unnecessary API calls during rapid typing
            delay(300)
            
            // Only show loading state if list is currently empty AND we're not already showing empty state
            // This prevents flicker when:
            // 1. Content is already displayed (list not empty)
            // 2. Empty state is already displayed (previous search had no results)
            if (_groups.value.isEmpty() && _uiState.value !is GroupsUIState.Empty) {
                _uiState.value = GroupsUIState.Loading
            }
            
            val builder = searchRequestBuilder ?: groupsRequestBuilder
                ?: GroupsRequest.GroupsRequestBuilder().setLimit(30)
            
            // Build request with search keyword
            val searchRequest = builder.setSearchKeyWord(query).build()

            if (twoPhase != null) {
                // ON path: the search request also pages the results.
                listGen++
                isFetching = true
                groupsRequest = searchRequest
                readTwoPhase(twoPhase, searchRequest, replace = true)
                return@launch
            }
            
            val result = fetchGroupsUseCase(searchRequest)
            if (gen != offGen) return@launch
            offSearchInFlight = false
            result
                .onSuccess { groups ->
                    // Create defensive copies to prevent SDK from mutating our list items
                    val copiedGroups = groups.map { createGroupCopy(it) }
                    
                    // Atomic list replacement - replace entire list at once
                    // This prevents flicker from clear-then-add pattern
                    _groups.value = copiedGroups
                    
                    // Direct state transition: Content → Empty without Loading in between
                    _uiState.value = if (copiedGroups.isEmpty()) {
                        GroupsUIState.Empty
                    } else {
                        GroupsUIState.Content(copiedGroups)
                    }
                    
                    // Update request for pagination
                    groupsRequest = searchRequest
                }
                .onFailure { exception ->
                    // Only show error if we don't have existing content
                    // This provides graceful degradation - keep showing old results on error
                    if (_groups.value.isEmpty()) {
                        _uiState.value = GroupsUIState.Error(exception as CometChatException)
                    }
                }
        }
    }

    /**
     * Refreshes the group list from the beginning.
     * This is a silent refresh - it does not show loading shimmer to maintain scroll position.
     * Uses client's request builder if set, otherwise creates a default one.
     */
    public fun refreshList() {
        refresh(quiet = false)
    }

    /**
     * [refreshList] for a caller. [quiet] marks a refresh the kit starts by itself (reconnect): a
     * failure while rows are on screen reports nothing (D84 rule 5).
     */
    private fun refresh(quiet: Boolean) {
        // Reset pagination state for fresh fetch
        hasMoreData = true
        isFetching = false

        val twoPhase = if (offlineOn) fetchGroupsUseCase.twoPhaseRepository else null
        if (twoPhase != null) {
            // ON path: re-read two-phase without clearing; the refresh request also pages on.
            listGen++
            isFetching = true
            val request = (groupsRequestBuilder ?: GroupsRequest.GroupsRequestBuilder().setLimit(30)).build()
            groupsRequest = request
            viewModelScope.launch {
                if (_groups.value.isEmpty()) {
                    _uiState.value = GroupsUIState.Loading
                }
                readTwoPhase(twoPhase, request, replace = true, quiet = quiet)
            }
            return
        }

        // ENG-41551 E: this refresh supersedes every earlier OFF-path load.
        val gen = ++offGen
        offSearchInFlight = false

        viewModelScope.launch {
            // Silent refresh - don't show loading state, keep existing data visible
            // Only show loading if list is empty
            if (_groups.value.isEmpty()) {
                _uiState.value = GroupsUIState.Loading
            }

            // Use client's builder if provided, otherwise create default
            val builder = groupsRequestBuilder
                ?: GroupsRequest.GroupsRequestBuilder().setLimit(30)

            // Build a fresh request for the refresh
            val refreshRequest = builder.build()

            val result = fetchGroupsUseCase(refreshRequest)
            if (gen != offGen) return@launch
            result
                .onSuccess { groups ->
                    // Create defensive copies to prevent SDK from mutating our list items
                    val copiedGroups = groups.map { createGroupCopy(it) }
                    _groups.value = copiedGroups
                    _uiState.value = if (copiedGroups.isEmpty()) {
                        GroupsUIState.Empty
                    } else {
                        GroupsUIState.Content(copiedGroups)
                    }
                    
                    // Create new request for pagination
                    groupsRequest = builder.build()
                }
                .onFailure { exception ->
                    // Only show error if list is empty, otherwise keep existing data
                    if (_groups.value.isEmpty()) {
                        _uiState.value = GroupsUIState.Error(exception as CometChatException)
                    }
                }
        }
    }

    /**
     * Joins a group with the specified parameters.
     *
     * @param group The group to join
     * @param password Optional password for password-protected groups
     */
    /**
     * The ON-path two-phase read (BEH-SNAP-03, BEH-READ-01). The cached page is the first paint;
     * the server page reconciles by guid; a phase of a superseded read is dropped. Errors never
     * blank rows already shown; a failure with nothing on screen is the error state (D83).
     * Rows get today's defensive copies.
     */
    private suspend fun readTwoPhase(
        repository: GroupsTwoPhaseRepository,
        request: GroupsRequest,
        replace: Boolean,
        quiet: Boolean = false
    ) {
        // D83: only a window load reports a failure over kept rows; a load-more page never does.
        val window = replace || _groups.value.isEmpty()
        val op = SnapshotReadOp(listGen, OfflineHub.gen, replace)
        val end = SnapshotListRead.collect(
            phases = repository.fetchGroupsTwoPhase(request),
            op = op,
            listGen = { listGen },
            rows = _groups,
            keyOf = { it.guid },
            transform = { page -> page.map { createGroupCopy(it) } }
        ) {
            offlineErrorKeyword = null
            _connectToSearch.value = false
            _connectToLoad.value = false
            _uiState.value = GroupsUIState.Content(_groups.value)
        }
        when (end) {
            is SnapshotListRead.End.Dropped -> Unit
            is SnapshotListRead.End.Server -> {
                if (end.pageEmpty) hasMoreData = false
                offlineErrorKeyword = null
                _connectToSearch.value = false
                _connectToLoad.value = false
                // D84 rule 6: the server answered with nothing to show: the empty state.
                updateUIState()
            }
            is SnapshotListRead.End.Failed -> {
                val notConnected = SnapshotListRead.isNotConnected(end.exception)
                val keyword = request.searchKeyword
                // Rows of a previous query never stand in for a search that could not run (a keyword
                // has no cache, BEH-SNAP-04); a later page of results already shown is kept.
                if (replace && notConnected && !keyword.isNullOrEmpty()) _groups.value = emptyList()
                if (_groups.value.isEmpty()) {
                    offlineErrorKeyword = keyword?.takeIf { it.isNotEmpty() && notConnected }
                    when {
                        // D84 rule 7: an offline keyword search: "Connect to search", no onError.
                        notConnected && !keyword.isNullOrEmpty() -> {
                            _connectToLoad.value = false
                            _connectToSearch.value = true
                            _uiState.value = GroupsUIState.Empty
                        }
                        // D84 rule 3: offline with nothing on screen: the offline state (the app's
                        // error view if set); onError fires once through the Error state.
                        notConnected -> {
                            _connectToSearch.value = false
                            _connectToLoad.value = true
                            _uiState.value = GroupsUIState.Error(end.exception)
                        }
                        // D84 rule 4: a real error with nothing on screen: the error view.
                        else -> {
                            _connectToSearch.value = false
                            _connectToLoad.value = false
                            _uiState.value = GroupsUIState.Error(end.exception)
                        }
                    }
                } else if (window && !quiet && !notConnected) {
                    // D84 rule 5: an online error over rows keeps them; onError fires once if the user
                    // or app started the load. Offline over rows (D84 rule 2) and a kit-started read
                    // stay quiet.
                    _rowsKeptError.trySend(end.exception)
                }
            }
        }
        if (op.gen == listGen) isFetching = false
    }

    /**
     * The connection is back: today's refresh of the list. With offline on, a keyword search whose
     * offline read is showing "Connect to search" re-runs that search instead (D84 rule 8).
     * The kit starts this refresh, so a failure while rows are on screen stays quiet (D84 rule 5).
     */
    internal fun onConnected() {
        val keyword = offlineErrorKeyword
        offlineErrorKeyword = null
        if (offlineOn && keyword != null && _groups.value.isEmpty()) {
            searchGroups(keyword)
        } else {
            refresh(quiet = true)
        }
    }

    public fun joinGroup(group: Group, password: String? = null) {
        viewModelScope.launch {
            joinGroupUseCase(group.guid, group.groupType, password)
                .onSuccess { joinedGroup ->
                    _groupJoinedEvent.emit(joinedGroup)
                    updateItem(joinedGroup) { it.guid == joinedGroup.guid }
                }
                .onFailure { /* Error handled via onError callback */ }
        }
    }

    /**
     * Clears the list for a session change (logout or a switch to a different uid), before the next
     * frame (G8). Bumps both read generations, so a read, page or search already in flight drops its
     * result, cancels a pending search, empties the rows and resets paging so the next fetch starts
     * from page one with the app's request builder. Offline on only (the hub drives it).
     */
    private fun clearForOfflineSession() {
        searchJob?.cancel()
        listGen++
        offGen++
        offSearchInFlight = false
        hasMoreData = true
        isFetching = false
        groupsRequest = null
        offlineErrorKeyword = null
        _connectToSearch.value = false
        _connectToLoad.value = false
        _groups.value = emptyList()
        _uiState.value = GroupsUIState.Loading
    }

    /**
     * The offline cache was reset (G8): clear as for a session end, then reload once from page one:
     * the keyword search on screen if there was one, else the list. The kit starts this reload, so a
     * failure over rows stays quiet (D84 rule 5).
     */
    private fun onOfflineCacheReset() {
        val keyword = groupsRequest?.searchKeyword?.takeIf { it.isNotEmpty() }
        clearForOfflineSession()
        if (keyword != null) searchGroups(keyword) else refresh(quiet = true)
    }

    /**
     * Selects a group based on selection mode.
     * In SINGLE mode, clears previous selection before selecting new group.
     * In MULTIPLE mode, toggles selection state.
     *
     * @param group The group to select
     * @param mode The selection mode (SINGLE or MULTIPLE)
     */
    public fun selectGroup(group: Group, mode: UIKitConstants.SelectionMode = UIKitConstants.SelectionMode.MULTIPLE) {
        val current = _selectedGroups.value.toMutableSet()
        
        when (mode) {
            UIKitConstants.SelectionMode.SINGLE -> {
                // In SINGLE mode, clear all and select only this group
                current.clear()
                current.add(group)
            }
            UIKitConstants.SelectionMode.MULTIPLE -> {
                // In MULTIPLE mode, toggle selection
                if (current.any { it.guid == group.guid }) {
                    current.removeAll { it.guid == group.guid }
                } else {
                    current.add(group)
                }
            }
            UIKitConstants.SelectionMode.NONE -> {
                // Do nothing in NONE mode
                return
            }
        }
        _selectedGroups.value = current
    }

    /**
     * Deselects a group.
     *
     * @param group The group to deselect
     */
    public fun deselectGroup(group: Group) {
        val current = _selectedGroups.value.toMutableSet()
        current.removeAll { it.guid == group.guid }
        _selectedGroups.value = current
    }

    /**
     * Clears all selected groups.
     */
    public fun clearSelection() {
        _selectedGroups.value = emptySet()
    }

    /**
     * Returns the list of currently selected groups.
     */
    public fun getSelectedGroups(): List<Group> = _selectedGroups.value.toList()

    /**
     * Sets the groups request builder for customizing fetch parameters.
     *
     * @param builder The custom request builder
     */
    public fun setGroupsRequestBuilder(builder: GroupsRequest.GroupsRequestBuilder) {
        groupsRequestBuilder = builder
        groupsRequest = builder.build()
    }

    /**
     * Sets the search request builder for customizing search parameters.
     *
     * @param builder The custom search request builder
     */
    public fun setSearchRequestBuilder(builder: GroupsRequest.GroupsRequestBuilder) {
        searchRequestBuilder = builder
    }

    // ==================== Private Methods ====================

    /**
     * Adds CometChat SDK listeners for real-time group updates.
     * Registers GroupListener for member events and ConnectionListener for reconnection.
     * Also subscribes to local group events via CometChatEvents flow.
     */
    private fun addListeners() {
        listenersTag = "GroupsList_${System.currentTimeMillis()}"

        listenersTag?.let { tag ->
            // CometChat Group Listener for SDK events
            CometChat.addGroupListener(tag, object : CometChat.GroupListener() {
                override fun onGroupMemberJoined(
                    action: Action,
                    joinedUser: User,
                    joinedGroup: Group
                ) {
                    if (joinedUser.uid == CometChatUIKit.getLoggedInUser()?.uid) {
                        joinedGroup.setHasJoined(true)
                    }
                    updateGroupInList(joinedGroup)
                }

                override fun onGroupMemberLeft(
                    action: Action,
                    leftUser: User,
                    leftGroup: Group
                ) {
                    updateGroupInList(leftGroup)
                }

                override fun onGroupMemberKicked(
                    action: Action,
                    kickedUser: User,
                    kickedBy: User,
                    kickedFrom: Group
                ) {
                    updateGroupInList(kickedFrom)
                }

                override fun onGroupMemberBanned(
                    action: Action,
                    bannedUser: User,
                    bannedBy: User,
                    bannedFrom: Group
                ) {
                    updateGroupInList(bannedFrom)
                }

                override fun onGroupMemberUnbanned(
                    action: Action,
                    unbannedUser: User,
                    unbannedBy: User,
                    unbannedFrom: Group
                ) {
                    updateGroupInList(unbannedFrom)
                }

                override fun onGroupMemberScopeChanged(
                    action: Action,
                    updatedBy: User,
                    updatedUser: User,
                    scopeChangedTo: String,
                    scopeChangedFrom: String,
                    group: Group
                ) {
                    updateGroupInList(group)
                }

                override fun onMemberAddedToGroup(
                    action: Action,
                    addedBy: User,
                    userAdded: User,
                    addedTo: Group
                ) {
                    updateGroupInList(addedTo)
                }
            })

            // Connection listener for reconnection handling
            CometChat.addConnectionListener(tag, object : CometChat.ConnectionListener {
                override fun onConnected() {
                    this@CometChatGroupsViewModel.onConnected()
                }

                override fun onConnecting() {
                    // No action needed
                }

                override fun onDisconnected() {
                    // No action needed
                }

                override fun onFeatureThrottled() {
                    // No action needed
                }

                override fun onConnectionError(error: CometChatException?) {
                    // No action needed
                }
            })
        }

        // Subscribe to local group events via CometChatEvents flow
        addLocalEventListeners()
    }

    /**
     * Adds local event listeners for UI-triggered group events.
     * These events are emitted by other UI components (e.g., when a group is created).
     */
    private fun addLocalEventListeners() {
        groupEventsJob = viewModelScope.launch {
            CometChatEvents.groupEvents.collect { event ->
                when (event) {
                    is CometChatGroupEvent.GroupCreated -> {
                        viewModelScope.launch {
                            _groupCreatedEvent.emit(event.group)
                            addToTop(event.group)
                        }
                    }

                    is CometChatGroupEvent.GroupLeft -> {
                        if (event.user.uid == CometChatUIKit.getLoggedInUser()?.uid) {
                            if (event.group.groupType == CometChatConstants.GROUP_TYPE_PRIVATE) {
                                removeItem(event.group)
                            } else {
                                updateGroupInList(event.group)
                            }
                        }
                    }

                    is CometChatGroupEvent.GroupDeleted -> {
                        removeItem(event.group)
                    }

                    is CometChatGroupEvent.MemberJoined -> {
                        updateGroupInList(event.group)
                    }

                    is CometChatGroupEvent.MembersAdded -> {
                        updateGroupInList(event.group)
                    }

                    is CometChatGroupEvent.MemberKicked -> {
                        updateGroupInList(event.group)
                    }

                    is CometChatGroupEvent.MemberBanned -> {
                        updateGroupInList(event.group)
                    }

                    is CometChatGroupEvent.MemberUnbanned -> {
                        updateGroupInList(event.group)
                    }

                    is CometChatGroupEvent.MemberScopeChanged -> {
                        updateGroupInList(event.group)
                    }

                    is CometChatGroupEvent.OwnershipChanged -> {
                        updateGroupInList(event.group)
                    }
                }
            }
        }
    }

    /**
     * Updates a group in the list if it exists.
     * Also updates the UI state accordingly.
     * 
     * Note: We create a defensive copy of the Group object to ensure
     * DiffUtil/Compose detects the change. The SDK may mutate objects in place,
     * so comparing the same object reference would not detect changes.
     */
    private fun updateGroupInList(group: Group) {
        val currentList = _groups.value
        val index = currentList.indexOfFirst { it.guid == group.guid }
        if (index >= 0) {
            // Create a defensive copy of the Group to ensure DiffUtil detects changes
            // The SDK may mutate the same object reference, so we need a new instance
            val groupCopy = createGroupCopy(group)
            val newList = currentList.toMutableList()
            newList[index] = groupCopy
            _groups.value = newList.toList()
        }
        updateUIState()
    }

    /**
     * Creates a defensive copy of a Group object.
     * This ensures that DiffUtil/Compose can detect changes even if
     * the SDK mutates the original object in place.
     */
    private fun createGroupCopy(group: Group): Group {
        return Group(
            group.guid,
            group.name,
            group.groupType,
            group.password,
            group.icon,
            group.description
        ).apply {
            owner = group.owner
            metadata = group.metadata
            createdAt = group.createdAt
            updatedAt = group.updatedAt
            setHasJoined(group.isJoined)
            membersCount = group.membersCount
            joinedAt = group.joinedAt
            tags = group.tags
            scope = group.scope
        }
    }

    /**
     * Adds a group to the top of the list if it doesn't already exist.
     * Emits scroll to top event after adding.
     */
    private fun addToTop(group: Group) {
        if (_groups.value.none { it.guid == group.guid }) {
            // Create defensive copy to prevent SDK from mutating our list item
            val groupCopy = createGroupCopy(group)
            moveItemToTop(groupCopy)
            viewModelScope.launch {
                _scrollToTopEvent.emit(Unit)
            }
        }
        updateUIState()
    }

    /**
     * Updates the UI state based on the current groups list.
     */
    private fun updateUIState() {
        _uiState.value = if (_groups.value.isEmpty()) {
            GroupsUIState.Empty
        } else {
            GroupsUIState.Content(_groups.value)
        }
    }

    /**
     * Removes all CometChat SDK listeners and cancels local event jobs.
     */
    private fun removeListeners() {
        listenersTag?.let { tag ->
            CometChat.removeGroupListener(tag)
            CometChat.removeConnectionListener(tag)
        }
        groupEventsJob?.cancel()
        groupEventsJob = null
    }

    override fun onCleared() {
        super.onCleared()
        removeListeners()
        searchJob?.cancel()
        listDelegate.cancel()
    }
}
