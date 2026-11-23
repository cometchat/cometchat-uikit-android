package com.cometchat.uikit.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.domain.usecase.FetchUsersUseCase
import com.cometchat.uikit.core.domain.usecase.SearchUsersUseCase
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatUserEvent
import com.cometchat.uikit.core.domain.repository.UsersTwoPhaseRepository
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineHub
import com.cometchat.uikit.core.offline.OfflineListSession
import com.cometchat.uikit.core.offline.SnapshotListRead
import com.cometchat.uikit.core.offline.SnapshotReadOp
import com.cometchat.uikit.core.state.UsersUIState
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
 * ViewModel that holds UI state and calls use cases for Users.
 * Does NOT fetch data directly - delegates to use cases.
 * 
 * Implements [ListOperations] interface for standardized list manipulation.
 * All list operation methods are open for client override.
 * 
 * @param fetchUsersUseCase Use case for fetching users
 * @param searchUsersUseCase Use case for searching users
 * @param enableListeners Whether to enable CometChat listeners (set to false for testing)
 */
open public class CometChatUsersViewModel(
    private val fetchUsersUseCase: FetchUsersUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val enableListeners: Boolean = true
) : ViewModel(), ListOperations<User> {
    
    // UI State
    private val _uiState = MutableStateFlow<UsersUIState>(UsersUIState.Loading)
    public val uiState: StateFlow<UsersUIState> = _uiState.asStateFlow()
    
    // Users list
    private val _users = MutableStateFlow<List<User>>(emptyList())
    public val users: StateFlow<List<User>> = _users.asStateFlow()
    
    // Fetching state - prevents concurrent fetches
    private var isFetching = false
    
    // Has more data flag - tracks if there are more users to fetch
    private var hasMoreData = true
    
    // List operations delegate - handles internal list manipulation
    private val listDelegate = ListOperationsDelegate(
        stateFlow = _users,
        equalityChecker = { a, b -> a.uid == b.uid }
    )
    
    // Selection state
    private val _selectedUsers = MutableStateFlow<Set<User>>(emptySet())
    public val selectedUsers: StateFlow<Set<User>> = _selectedUsers.asStateFlow()
    
    // Scroll to top event - emits when list should scroll to top
    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    public val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()
    
    // Configuration
    private var usersRequest: UsersRequest? = null
    private var usersRequestBuilder: UsersRequest.UsersRequestBuilder? = null
    private var searchUsersRequestBuilder: UsersRequest.UsersRequestBuilder? = null
    private var listenersTag: String? = null
    private val limit = 30
    
    // Local event listener jobs
    private var userEventsJob: Job? = null
    
    // Search debounce job - cancels pending search when new search is initiated
    private var searchJob: Job? = null

    // Offline (BEH-SNAP-01..07): read once at construction; the OFF path below is today's code.
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
     * nothing on screen the failure is [UsersUIState.Error] instead. Never emits with offline
     * messaging off.
     */
    public val rowsKeptError: Flow<CometChatException> = _rowsKeptError.receiveAsFlow()

    private val _connectToSearch = MutableStateFlow(false)

    /**
     * True while [uiState] is [UsersUIState.Empty] because a search keyword was read offline: a
     * keyword read has no cache (BEH-SNAP-04), so the empty state reads "Connect to search" instead
     * of "no users" (D84 rule 7, no `onError`). A presence filter (`userStatus`) without a keyword
     * is not a search: offline with nothing shown it is the offline state ([connectToLoad], ruled
     * 2026-11-13). Always false with offline messaging off.
     */
    public val connectToSearch: StateFlow<Boolean> = _connectToSearch.asStateFlow()

    private val _connectToLoad = MutableStateFlow(false)

    /**
     * True while [uiState] is [UsersUIState.Error] because the list was read offline with nothing
     * on screen (D84 rule 3): the kit shows its offline state ("Connect to load users", no Retry)
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

    // The generation whose fetchUsers() set isFetching; a superseded load clears it only if it
    // still owns it.
    private var fetchOwnerGen = -1L

    // OFF path (ENG-41551 E): a keyword search is in flight. A load-more would page the previous
    // list's request into the search results, so fetchUsers() waits until the search lands.
    private var offSearchInFlight = false

    private val usersOnPath: Boolean
        get() = offlineOn && fetchUsersUseCase.twoPhaseRepository != null

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
        // NOTE: Do NOT fetch users here. The initial fetch is triggered by the
        // view/composable once the client has had a chance to call
        // setUsersRequestBuilder(...) (e.g. to filter by role). Fetching in init
        // would race the caller and emit an unfiltered request to the SDK,
        // which produced mixed results (agents + regular users) in the
        // AI-assistant list.
        //
        // - Kotlin `CometChatUsers` triggers the first fetch in `onAttachedToWindow`.
        // - Compose `CometChatUsers` triggers the first fetch in its
        //   `LaunchedEffect(usersRequestBuilder)` block.
    }
    
    /**
     * Fetches users with pagination support.
     * Shows loading state only on initial fetch.
     * Uses client's request builder if set, otherwise creates a default one.
     * Prevents concurrent fetches using isFetching flag.
     */
    public fun fetchUsers() {
        // Prevent concurrent fetches and don't fetch if no more data
        if (isFetching || !hasMoreData) return
        // ENG-41551 E: offline off, no page of the previous list while a keyword search runs.
        if (offSearchInFlight && !usersOnPath) return

        // Set isFetching BEFORE launching coroutine to prevent race conditions
        isFetching = true
        val gen = offGen
        fetchOwnerGen = gen

        viewModelScope.launch {
            if (_users.value.isEmpty()) {
                _uiState.value = UsersUIState.Loading
            }
            
            // Create request on first fetch, reuse for pagination
            // Use client's builder if provided, otherwise create default
            if (usersRequest == null) {
                val builder = usersRequestBuilder 
                    ?: UsersRequest.UsersRequestBuilder().setLimit(limit)
                usersRequest = builder.build()
            }
            
            val twoPhase = if (offlineOn) fetchUsersUseCase.twoPhaseRepository else null
            val pageRequest = usersRequest
            if (twoPhase != null && pageRequest != null) {
                readTwoPhase(twoPhase, pageRequest, replace = false)
                return@launch
            }

            usersRequest?.let { request ->
                val result = fetchUsersUseCase(request)
                if (gen != offGen) {
                    // ENG-41551 E: a search or refresh superseded this load; its page is dropped.
                    if (fetchOwnerGen == gen) isFetching = false
                    return@launch
                }
                result
                    .onSuccess { newUsers ->
                        // Only append if we got new users
                        if (newUsers.isNotEmpty()) {
                            addList(newUsers)
                            _uiState.value = UsersUIState.Content
                        } else {
                            // No new users means we've reached the end
                            hasMoreData = false
                            if (_users.value.isEmpty()) {
                                _uiState.value = UsersUIState.Empty
                            }
                        }
                    }
                    .onFailure { exception ->
                        if (_users.value.isEmpty()) {
                            _uiState.value = UsersUIState.Error(exception as CometChatException)
                        }
                    }
                
                isFetching = false
            } ?: run {
                isFetching = false
            }
        }
    }
    
    /**
     * Refreshes the user list from the beginning.
     * Clears existing data and fetches fresh.
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

        val twoPhase = if (offlineOn) fetchUsersUseCase.twoPhaseRepository else null
        if (twoPhase != null) {
            // ON path: re-read two-phase without clearing, so the list never blanks.
            listGen++
            isFetching = true
            val request = (usersRequestBuilder ?: UsersRequest.UsersRequestBuilder().setLimit(limit)).build()
            usersRequest = request
            viewModelScope.launch {
                if (_users.value.isEmpty()) {
                    _uiState.value = UsersUIState.Loading
                }
                readTwoPhase(twoPhase, request, replace = true, scrollToTopOnServer = true, quiet = quiet)
            }
            return
        }

        // ENG-41551 E: this refresh supersedes every earlier OFF-path load.
        val gen = ++offGen
        offSearchInFlight = false

        viewModelScope.launch {
            if (_users.value.isEmpty()) {
                _uiState.value = UsersUIState.Loading
            }

            // Use client's builder if provided, otherwise create default
            val builder = usersRequestBuilder
                ?: UsersRequest.UsersRequestBuilder().setLimit(limit)
            usersRequest = builder.build()

            clearItems()

            usersRequest?.let { request ->
                val result = fetchUsersUseCase(request)
                if (gen != offGen) return@launch
                result
                    .onSuccess { users ->
                        addList(users)
                        _uiState.value = if (users.isEmpty()) {
                            UsersUIState.Empty
                        } else {
                            UsersUIState.Content
                        }
                        
                        // Emit scroll to top event after refresh
                        _scrollToTopEvent.emit(Unit)
                    }
                    .onFailure { exception ->
                        if (_users.value.isEmpty()) {
                            _uiState.value = UsersUIState.Error(exception as CometChatException)
                        }
                    }
            }
        }
    }
    
    /**
     * Searches for users based on the specified keyword.
     * Implements seamless search experience with:
     * - Debouncing (300ms) to reduce API calls during rapid typing
     * - Keeps existing content visible during search (no flicker)
     * - Conditional loading state (only when list is empty)
     * - Atomic list replacement when results arrive
     *
     * @param keyword The search keyword (null or empty to reset)
     */
    public fun searchUsers(keyword: String?) {
        // Cancel any pending search job (debouncing)
        searchJob?.cancel()
        // ENG-41551 E: a new keyword supersedes every earlier OFF-path load (a plain-list page
        // started by clearing the keyword included), so only this keyword's results land.
        val gen = ++offGen
        offSearchInFlight = !keyword.isNullOrEmpty() &&
            !(offlineOn && searchUsersUseCase.twoPhaseRepository != null)

        searchJob = viewModelScope.launch {
            // Reset pagination state for new search
            hasMoreData = true
            
            val fetchTwoPhase = if (offlineOn) fetchUsersUseCase.twoPhaseRepository else null
            if (keyword.isNullOrEmpty() && fetchTwoPhase != null) {
                // ON path: back to the plain list without blanking it first.
                listGen++
                isFetching = true
                val request = usersRequestBuilder?.build()
                    ?: UsersRequest.UsersRequestBuilder().setLimit(limit).build()
                usersRequest = request
                readTwoPhase(fetchTwoPhase, request, replace = true)
                return@launch
            }
            val twoPhase = if (offlineOn) searchUsersUseCase.twoPhaseRepository else null

            if (keyword.isNullOrEmpty()) {
                // Reset to normal fetch - clear list and fetch fresh
                _users.value = emptyList()
                usersRequest = usersRequestBuilder?.build() 
                    ?: UsersRequest.UsersRequestBuilder().setLimit(limit).build()
                isFetching = false
                fetchUsers()
                return@launch
            }
            
            // Debounce: wait 300ms before making API call
            // This reduces unnecessary API calls during rapid typing
            delay(300)
            
            // Only show loading state if list is currently empty AND we're not already showing empty state
            // This prevents flicker when:
            // 1. Content is already displayed (list not empty)
            // 2. Empty state is already displayed (previous search had no results)
            if (_users.value.isEmpty() && _uiState.value !is UsersUIState.Empty) {
                _uiState.value = UsersUIState.Loading
            }
            
            val builder = searchUsersRequestBuilder ?: usersRequestBuilder 
                ?: UsersRequest.UsersRequestBuilder().setLimit(limit)

            if (twoPhase != null && !keyword.isNullOrEmpty()) {
                // ON path: the same request the use case builds; it also pages the results.
                listGen++
                isFetching = true
                val request = builder.setSearchKeyword(keyword).build()
                usersRequest = request
                readTwoPhase(twoPhase, request, replace = true)
                return@launch
            }
            
            val result = searchUsersUseCase(keyword, builder)
            if (gen != offGen) return@launch
            offSearchInFlight = false
            result
                .onSuccess { users ->
                    // Atomic list replacement - replace entire list at once
                    // This prevents flicker from clear-then-add pattern
                    _users.value = users
                    
                    // Direct state transition: Content → Empty without Loading in between
                    _uiState.value = if (users.isEmpty()) {
                        UsersUIState.Empty
                    } else {
                        UsersUIState.Content
                    }
                    
                    // Update request for pagination
                    usersRequest = builder.setSearchKeyword(keyword).build()
                }
                .onFailure { exception ->
                    // Only show error if we don't have existing content
                    // This provides graceful degradation - keep showing old results on error
                    if (_users.value.isEmpty()) {
                        _uiState.value = UsersUIState.Error(exception as CometChatException)
                    }
                }
        }
    }
    
    /**
     * The ON-path two-phase read (BEH-SNAP-03, BEH-READ-01). The cached page is the first paint;
     * the server page reconciles by uid; a phase of a superseded read is dropped. Errors never blank
     * rows already shown; an offline keyword/presence read shows "Connect to search" (BEH-SNAP-04).
     */
    private suspend fun readTwoPhase(
        repository: UsersTwoPhaseRepository,
        request: UsersRequest,
        replace: Boolean,
        scrollToTopOnServer: Boolean = false,
        quiet: Boolean = false
    ) {
        // D83: only a window load reports a failure over kept rows; a load-more page never does.
        val window = replace || _users.value.isEmpty()
        val op = SnapshotReadOp(listGen, OfflineHub.gen, replace)
        val end = SnapshotListRead.collect(
            phases = repository.getUsersTwoPhase(request),
            op = op,
            listGen = { listGen },
            rows = _users,
            keyOf = { it.uid }
        ) {
            offlineErrorKeyword = null
            _connectToSearch.value = false
            _connectToLoad.value = false
            _uiState.value = UsersUIState.Content
        }
        when (end) {
            is SnapshotListRead.End.Dropped -> Unit
            is SnapshotListRead.End.Server -> {
                if (end.pageEmpty) hasMoreData = false
                offlineErrorKeyword = null
                _connectToSearch.value = false
                _connectToLoad.value = false
                // D84 rule 6: the server answered with nothing to show: the empty state.
                _uiState.value = if (_users.value.isEmpty()) UsersUIState.Empty else UsersUIState.Content
                if (scrollToTopOnServer) _scrollToTopEvent.emit(Unit)
            }
            is SnapshotListRead.End.Failed -> {
                val notConnected = SnapshotListRead.isNotConnected(end.exception)
                // Only a keyword is a search (ruled 2026-11-13): a presence filter alone is the list.
                val isSearch = !request.searchKeyword.isNullOrEmpty()
                // Rows of a previous query never stand in for a search that could not run (a keyword
                // has no cache, BEH-SNAP-04); a later page of results already shown is kept.
                if (replace && isSearch && notConnected) {
                    _users.value = emptyList()
                }
                if (_users.value.isEmpty()) {
                    offlineErrorKeyword = request.searchKeyword?.takeIf { it.isNotEmpty() && notConnected }
                    when {
                        // D84 rule 7: an offline keyword search: "Connect to search", no onError.
                        notConnected && isSearch -> {
                            _connectToLoad.value = false
                            _connectToSearch.value = true
                            _uiState.value = UsersUIState.Empty
                        }
                        // D84 rule 3: offline with nothing on screen: the offline state (the app's
                        // error view if set); onError fires once through the Error state.
                        notConnected -> {
                            _connectToSearch.value = false
                            _connectToLoad.value = true
                            _uiState.value = UsersUIState.Error(end.exception)
                        }
                        // D84 rule 4: a real error with nothing on screen: the error view.
                        else -> {
                            _connectToSearch.value = false
                            _connectToLoad.value = false
                            _uiState.value = UsersUIState.Error(end.exception)
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
        if (offlineOn && keyword != null && _users.value.isEmpty()) {
            searchUsers(keyword)
        } else {
            refresh(quiet = true)
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
        usersRequest = null
        offlineErrorKeyword = null
        _connectToSearch.value = false
        _connectToLoad.value = false
        _users.value = emptyList()
        _uiState.value = UsersUIState.Loading
    }

    /**
     * The offline cache was reset (G8): clear as for a session end, then reload once from page one:
     * the keyword search on screen if there was one, else the list. The kit starts this reload, so a
     * failure over rows stays quiet (D84 rule 5).
     */
    private fun onOfflineCacheReset() {
        val keyword = usersRequest?.searchKeyword?.takeIf { it.isNotEmpty() }
        clearForOfflineSession()
        if (keyword != null) searchUsers(keyword) else refresh(quiet = true)
    }

    /**
     * Selects or deselects a user based on selection mode.
     */
    public fun selectUser(user: User, mode: UIKitConstants.SelectionMode) {
        when (mode) {
            UIKitConstants.SelectionMode.SINGLE -> {
                _selectedUsers.value = setOf(user)
            }
            UIKitConstants.SelectionMode.MULTIPLE -> {
                val current = _selectedUsers.value.toMutableSet()
                if (current.any { it.uid == user.uid }) {
                    current.removeAll { it.uid == user.uid }
                } else {
                    current.add(user)
                }
                _selectedUsers.value = current
            }
            UIKitConstants.SelectionMode.NONE -> {
                // Do nothing
            }
        }
    }
    
    /**
     * Clears all selected users.
     */
    public fun clearSelection() {
        _selectedUsers.value = emptySet()
    }
    
    /**
     * Returns the list of currently selected users.
     */
    public fun getSelectedUsers(): List<User> = _selectedUsers.value.toList()
    
    /**
     * Checks if a user is selected.
     */
    public fun isSelected(user: User): Boolean = 
        _selectedUsers.value.any { it.uid == user.uid }
    
    /**
     * Sets the users request builder for customizing fetch parameters.
     *
     * @param builder The custom request builder provided by the client
     */
    public fun setUsersRequestBuilder(builder: UsersRequest.UsersRequestBuilder) {
        usersRequestBuilder = builder
        usersRequest = builder.build()
    }
    
    /**
     * Sets the search request builder for customizing search parameters.
     *
     * @param builder The custom request builder for search
     */
    public fun setSearchRequestBuilder(builder: UsersRequest.UsersRequestBuilder?) {
        searchUsersRequestBuilder = builder
    }
    
    /**
     * Adds a list of users to the user list.
     * Updates existing users if they already exist.
     *
     * @param userList The list of users to add
     */
    private fun addList(userList: List<User>) {
        val currentList = _users.value.toMutableList()
        for (user in userList) {
            val index = currentList.indexOfFirst { it.uid == user.uid }
            if (index >= 0) {
                currentList[index] = user
            } else {
                currentList.add(user)
            }
        }
        _users.value = currentList
    }
    
    /**
     * Moves a user to the top of the list.
     */
    public fun moveUserToTop(user: User) {
        val currentList = _users.value.toMutableList()
        val index = currentList.indexOfFirst { it.uid == user.uid }
        if (index >= 0) {
            currentList.removeAt(index)
            currentList.add(0, user)
            _users.value = currentList
            
            viewModelScope.launch {
                _scrollToTopEvent.emit(Unit)
            }
        }
    }
    
    /**
     * Updates a user in the list.
     */
    public fun updateUser(user: User) {
        val currentList = _users.value.toMutableList()
        val index = currentList.indexOfFirst { it.uid == user.uid }
        if (index >= 0) {
            currentList[index] = user
            _users.value = currentList
        }
    }
    
    /**
     * Removes a user from the list.
     */
    public fun removeUser(user: User) {
        val currentList = _users.value.toMutableList()
        val index = currentList.indexOfFirst { it.uid == user.uid }
        if (index >= 0) {
            currentList.removeAt(index)
            _users.value = currentList
            
            if (currentList.isEmpty()) {
                _uiState.value = UsersUIState.Empty
            }
        }
    }
    
    /**
     * Adds a user to the top of the list.
     */
    public fun addUserToTop(user: User) {
        val currentList = _users.value.toMutableList()
        if (!currentList.any { it.uid == user.uid }) {
            currentList.add(0, user)
            _users.value = currentList
            
            viewModelScope.launch {
                _scrollToTopEvent.emit(Unit)
            }
        }
    }
    
    // ==================== ListOperations Implementation ====================
    
    override fun addItem(item: User): Unit = listDelegate.addItem(item)
    
    override fun addItems(items: List<User>): Unit = listDelegate.addItems(items)
    
    override fun removeItem(item: User): Boolean = listDelegate.removeItem(item)
    
    override fun removeItemAt(index: Int): User? = listDelegate.removeItemAt(index)
    
    override fun updateItem(item: User, predicate: (User) -> Boolean): Boolean = 
        listDelegate.updateItem(item, predicate)
    
    override fun clearItems(): Unit = listDelegate.clearItems()
    
    override fun getItems(): List<User> = listDelegate.getItems()
    
    override fun getItemAt(index: Int): User? = listDelegate.getItemAt(index)
    
    override fun getItemCount(): Int = listDelegate.getItemCount()
    
    override fun moveItemToTop(item: User): Unit = listDelegate.moveItemToTop(item)
    
    override fun batch(operations: ListOperationsBatchScope<User>.() -> Unit): Unit = 
        listDelegate.batch(operations)
    
    // ==================== Listeners ====================
    
    /**
     * Adds CometChat listeners for real-time updates.
     */
    private fun addListeners() {
        listenersTag = "UsersList_${System.currentTimeMillis()}"
        
        listenersTag?.let { tag ->
            // User listener for online/offline status
            CometChat.addUserListener(tag, object : CometChat.UserListener() {
                override fun onUserOnline(user: User) {
                    if (!isBlocked(user)) {
                        moveUserToTop(user)
                    }
                }
                
                override fun onUserOffline(user: User) {
                    if (!isBlocked(user)) {
                        updateUser(user)
                    }
                }
            })
            
            // Connection listener for reconnection
            CometChat.addConnectionListener(tag, object : CometChat.ConnectionListener {
                override fun onConnected() {
                    this@CometChatUsersViewModel.onConnected()
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
        
        // Add local event listeners
        addLocalEventListeners()
    }
    
    /**
     * Adds local event listeners for UI-triggered events.
     */
    private fun addLocalEventListeners() {
        userEventsJob = viewModelScope.launch {
            CometChatEvents.userEvents.collect { event ->
                when (event) {
                    is CometChatUserEvent.UserBlocked -> {
                        updateUser(event.user)
                    }
                    is CometChatUserEvent.UserUnblocked -> {
                        updateUser(event.user)
                    }
                }
            }
        }
    }
    
    /**
     * Removes all CometChat listeners.
     */
    private fun removeListeners() {
        listenersTag?.let { tag ->
            CometChat.removeUserListener(tag)
            CometChat.removeConnectionListener(tag)
        }
        userEventsJob?.cancel()
    }
    
    /**
     * Checks if a user is blocked.
     */
    private fun isBlocked(user: User): Boolean {
        return user.isBlockedByMe || user.isHasBlockedMe
    }
    
    override fun onCleared() {
        super.onCleared()
        if (enableListeners) {
            removeListeners()
        }
        searchJob?.cancel()
        listDelegate.cancel()
    }
}
