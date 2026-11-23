package com.cometchat.uikit.core.viewmodel

import android.content.Context
import androidx.annotation.RawRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.helpers.CometChatHelper
import com.cometchat.chat.models.Action
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.BaseMessage
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.model.InitiatedCall
import com.cometchat.chat.core.Call
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.MessageReceipt
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.TypingIndicator
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.domain.repository.ConversationListTwoPhaseRepository
import com.cometchat.uikit.core.offline.ConversationDetailsOnce
import com.cometchat.uikit.core.offline.ConversationListOffline
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineHub
import com.cometchat.uikit.core.offline.OfflineHubEvent
import com.cometchat.uikit.core.offline.OfflineNotice
import com.cometchat.uikit.core.offline.OfflineNoticeRelay

import com.cometchat.uikit.core.resources.soundmanager.CometChatSoundManager
import com.cometchat.uikit.core.resources.soundmanager.Sound
import com.cometchat.uikit.core.domain.usecase.DeleteConversationUseCase
import com.cometchat.uikit.core.domain.usecase.GetConversationListUseCase
import com.cometchat.uikit.core.domain.usecase.RefreshConversationListUseCase
import com.cometchat.uikit.core.events.CometChatCallEvent
import com.cometchat.uikit.core.events.CometChatConversationEvent
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatGroupEvent
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.events.CometChatUserEvent
import com.cometchat.uikit.core.events.MessageStatus
import com.cometchat.uikit.core.state.DeleteState
import com.cometchat.uikit.core.state.UIState
import com.cometchat.uikit.core.utils.CallInitiationUtils
import com.cometchat.uikit.core.utils.CallsUtils
import com.cometchat.uikit.core.utils.CometChatLogger
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
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * ViewModel that holds UI state and calls use cases.
 * Does NOT fetch data directly - delegates to use cases.
 * 
 * Implements [ListOperations] interface for standardized list manipulation.
 * All list operation methods are open for client override.
 * 
 * @param getConversationListUseCase Use case for fetching conversations
 * @param deleteConversationUseCase Use case for deleting conversations
 * @param refreshConversationListUseCase Use case for refreshing the list
 * @param enableListeners Whether to enable CometChat listeners (set to false for testing)
 */
open public class CometChatConversationsViewModel(
    private val getConversationListUseCase: GetConversationListUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val refreshConversationListUseCase: RefreshConversationListUseCase,
    private val enableListeners: Boolean = true
) : ViewModel(), ListOperations<Conversation> {
    
    // UI State
    internal val _uiState = MutableStateFlow<UIState>(UIState.Loading)
    public val uiState: StateFlow<UIState> = _uiState.asStateFlow()
    
    // Conversations list
    internal val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    public val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // ========================================
    // Offline foundations (ENG-41086). Inert while offline is off — every branch guarded by
    // [offlineOn], so the OFF path is byte-identical to today.
    // ========================================

    /** The offline read mode, captured once when this ViewModel binds (G1). */
    internal val offlineOn: Boolean = OfflineGate.isOn()

    /** List generation, bumped on a session change so stale reads/events are dropped (G8). */
    internal var offlineListGen: Long = 0L
        private set

    private var offlineHubJob: Job? = null
    private var offlineNoticeJob: Job? = null

    /**
     * The hub session generation this list accepts events from. Adopted when the hub collector
     * starts and on each newer `SessionEnded`; any other event with a different gen is dropped (G8).
     */
    private var offlineHubGen: Long = OfflineHub.gen

    /** Test seam: the last hub event this list routed (stale-gen events never get here). */
    @VisibleForTesting
    internal var lastRoutedHubEvent: OfflineHubEvent? = null
        private set

    /** The repository's two-phase read, when it has one (G2); null keeps today's path. For W3. */
    internal val twoPhaseRepository: ConversationListTwoPhaseRepository?
        get() = getConversationListUseCase.twoPhaseRepository

    private val offlineNoticeRelay = OfflineNoticeRelay()

    /**
     * One-line transient offline notices (G8). Merges this list's own notices
     * ([raiseOfflineNotice]) with the hub's list-less notices; a notice waits until the UI
     * subscribes, so none is lost while nothing collects. Nothing emits in W1.
     */
    public val offlineNotices: SharedFlow<OfflineNotice> = offlineNoticeRelay.notices

    /** Queue a notice for this list (W3/W5). Delivered once the UI collects [offlineNotices]. */
    internal fun raiseOfflineNotice(notice: OfflineNotice) = offlineNoticeRelay.raise(notice)

    internal val _rowsKeptError = Channel<CometChatException>(8, BufferOverflow.DROP_OLDEST)

    /**
     * A read failed online while rows stay on screen (D84 rule 5, offline on only): the list keeps
     * them and the view fires `onError` without showing its error state. Each failure is buffered
     * until a view collects it and is delivered once (a view that starts later still gets it; a
     * rotation does not repeat it). Only a window load the user or app starts reports here (first
     * page, refresh, keyword search), never a load-more page and never a read the kit starts by
     * itself (reconnect re-read, sweep resync, a created group). A not-connected failure over rows
     * reports nothing (D84 rule 2). With nothing on screen the failure is [UIState.Error] instead.
     * Never emits with offline messaging off.
     */
    public val rowsKeptError: Flow<CometChatException> = _rowsKeptError.receiveAsFlow()

    internal val _connectToSearch = MutableStateFlow(false)

    /**
     * True while [uiState] is [UIState.Empty] because a `searchKeyword` list was read offline with
     * nothing on screen (D84 rule 7): the empty state reads "Connect to search", with no `onError`.
     * Always false with offline messaging off.
     */
    public val connectToSearch: StateFlow<Boolean> = _connectToSearch.asStateFlow()

    internal val _connectToLoad = MutableStateFlow(false)

    /**
     * True while [uiState] is [UIState.Error] because the list was read offline with nothing on
     * screen (D84 rule 3): the kit shows its offline state ("Connect to load chats", no Retry)
     * unless the app set an error view, which shows instead; `onError` fires once through the Error
     * state. The list reloads by itself on reconnect. Always false with offline messaging off.
     */
    public val connectToLoad: StateFlow<Boolean> = _connectToLoad.asStateFlow()

    /** W3 (ENG-41088): this list's offline read/sync/preview/pin state. Untouched while offline is off. */
    internal val offlineConversationState = ConversationListOffline.State()
    
    // Fetching state - prevents concurrent fetches
    internal var isFetching = false
    
    // Has more data flag - tracks if there are more conversations to fetch
    internal var hasMoreData = true
    
    // List operations delegate - handles internal list manipulation
    private val listDelegate = ListOperationsDelegate(
        stateFlow = _conversations,
        equalityChecker = { a, b -> a.conversationId == b.conversationId }
    )
    
    // Typing indicators
    private val _typingIndicators = MutableStateFlow<Map<String, TypingIndicator>>(emptyMap())
    public val typingIndicators: StateFlow<Map<String, TypingIndicator>> = _typingIndicators.asStateFlow()
    
    // Typing indicator debounce job
    private var typingDebounceJob: Job? = null
    private val typingIndicatorHashMap = mutableMapOf<String, TypingIndicator>()
    
    // Typing indicator debounce delay (1 second, matching Java implementation)
    private val TYPING_INDICATOR_DEBOUNCER = 1000L
    
    // Selection state
    private val _selectedConversations = MutableStateFlow<Set<Conversation>>(emptySet())
    public val selectedConversations: StateFlow<Set<Conversation>> = _selectedConversations.asStateFlow()
    
    // Delete state
    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    public val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()
    
    // Sound playback event - emits true when a sound should be played
    private val _playSoundEvent = MutableSharedFlow<Boolean>()
    public val playSoundEvent: SharedFlow<Boolean> = _playSoundEvent.asSharedFlow()
    
    // Scroll to top event - emits when list should scroll to top (new message received)
    internal val _scrollToTopEvent = MutableSharedFlow<Unit>()
    public val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()
    
    // Configuration
    internal var conversationsRequest: ConversationsRequest? = null
    internal var conversationsRequestBuilder: ConversationsRequest.ConversationsRequestBuilder? = null
        private set
    private var disableReceipt = false
    private var disableSoundForMessages = false
    @RawRes private var customSoundForMessage: Int = 0
    private var listenersTag: String? = null
    private var soundManager: CometChatSoundManager? = null
    
    // Local event listener jobs
    private var conversationEventsJob: Job? = null
    private var groupEventsJob: Job? = null
    private var userEventsJob: Job? = null
    private var messageEventsJob: Job? = null
    private var callEventsJob: Job? = null
    
    init {
        if (enableListeners) {
            addListeners()
        }
        fetchConversations()
    }
    
    /**
     * Fetches conversations with pagination support.
     * Shows loading state only on initial fetch.
     * Uses client's request builder if set, otherwise creates a default one.
     * Prevents concurrent fetches using isFetching flag.
     */
    public fun fetchConversations() {
        // Offline read seam (W3). OFF path untouched; in W1 the seam returns false and today's
        // single-phase fetch runs.
        if (offlineOn && ConversationListOffline.fetchConversations(this)) return
        // Prevent concurrent fetches and don't fetch if no more data
        if (isFetching || !hasMoreData) return
        
        viewModelScope.launch {
            isFetching = true
            
            if (_conversations.value.isEmpty()) {
                _uiState.value = UIState.Loading
            }
            
            // Create request on first fetch, reuse for pagination
            // Use client's builder if provided, otherwise create default
            if (conversationsRequest == null) {
                val builder = conversationsRequestBuilder 
                    ?: ConversationsRequest.ConversationsRequestBuilder()
                        .setLimit(30)
                conversationsRequest = builder.build()
            }
            
            conversationsRequest?.let { request ->
                getConversationListUseCase(request)
                    .onSuccess { newConversations ->
                        // Only append if we got new conversations
                        if (newConversations.isNotEmpty()) {
                            val updatedList = (_conversations.value + newConversations)
                                .distinctBy { it.conversationId }
                            _conversations.value = updatedList
                            _uiState.value = UIState.Content(updatedList)
                        } else {
                            // No new conversations means we've reached the end
                            hasMoreData = false
                            if (_conversations.value.isEmpty()) {
                                _uiState.value = UIState.Empty
                            }
                        }
                    }
                    .onFailure { exception ->
                        val cometChatException = if (exception is CometChatException) {
                            exception
                        } else {
                            CometChatException(
                                "ERR_UNKNOWN",
                                exception.message ?: "Unknown error",
                                exception.localizedMessage ?: "Unknown error"
                            )
                        }
                        _uiState.value = UIState.Error(cometChatException)
                    }
                
                isFetching = false
            } ?: run {
                isFetching = false
            }
        }
    }
    
    /**
     * Refreshes the conversation list from the beginning.
     * Clears existing data and fetches fresh.
     * This is a silent refresh - it does not show loading shimmer.
     * Uses client's request builder if set, otherwise creates a default one.
     */
    public fun refreshList() {
        // Offline read seam (W3). OFF path untouched; in W1 the seam returns false.
        if (offlineOn && ConversationListOffline.refreshList(this)) return
        // Reset pagination state for fresh fetch
        hasMoreData = true
        isFetching = false
        
        viewModelScope.launch {
            // Silent refresh - don't show loading state, keep existing data visible
            // Only show loading if list is empty
            if (_conversations.value.isEmpty()) {
                _uiState.value = UIState.Loading
            }
            
            // Use client's builder if provided, otherwise create default
            val builder = conversationsRequestBuilder
                ?: ConversationsRequest.ConversationsRequestBuilder()
                    .setLimit(30)

            val freshRequest = builder.build()

            refreshConversationListUseCase(freshRequest)
                .onSuccess { fetched ->
                    // Deduplicate before publishing — the list is keyed by conversationId in the
                    // UI, and a repeat would crash Compose on a duplicate key . The
                    // paginated fetch in fetchConversations() already does the same.
                    val conversations = fetched.distinctBy { it.conversationId }
                    _conversations.value = conversations
                    _uiState.value = if (conversations.isEmpty()) {
                        UIState.Empty
                    } else {
                        UIState.Content(conversations)
                    }

                    conversationsRequest = freshRequest

                    // Emit scroll to top event after refresh (new conversation at top)
                    _scrollToTopEvent.emit(Unit)
                }
                .onFailure { exception ->
                    // Only show error if list is empty, otherwise keep existing data
                    if (_conversations.value.isEmpty()) {
                        val cometChatException = if (exception is CometChatException) {
                            exception
                        } else {
                            CometChatException(
                                "ERR_UNKNOWN",
                                exception.message ?: "Unknown error",
                                exception.localizedMessage ?: "Unknown error"
                            )
                        }
                        _uiState.value = UIState.Error(cometChatException)
                    }
                }
        }
    }
    
    /**
     * Deletes a conversation.
     * Updates delete state throughout the operation.
     */
    /**
     * Pins a conversation for the current user. On success the returned conversation (carrying
     * pinnedAt/pinnedBy) is moved to the top of the list, matching the backend ordering.
     */
    public fun pinConversation(
        conversation: Conversation,
        onSuccess: (() -> Unit)? = null,
        onError: ((com.cometchat.chat.exceptions.CometChatException?) -> Unit)? = null
    ) {
        val with = conversationWith(conversation) ?: return
        // Offline (W3, G11): own pin updates from onSuccess only; a listener echo is a no-op.
        if (offlineOn) {
            ConversationListOffline.setPinned(this, conversation, with, true, onSuccess, onError)
            return
        }
        com.cometchat.chat.core.CometChat.pinConversation(
            with,
            conversation.conversationType,
            object : com.cometchat.chat.core.CometChat.CallbackListener<Conversation>() {
                override fun onSuccess(updated: Conversation) {
                    applyConversationPinned(updated)
                    onSuccess?.invoke()
                }

                override fun onError(e: com.cometchat.chat.exceptions.CometChatException?) {
                    onError?.invoke(e)
                }
            }
        )
    }

    /**
     * Unpins a conversation for the current user. On success the conversation moves to the top of
     * the unpinned section — leaving it in place could strand an unpinned conversation inside the
     * pinned block at the head of the list.
     */
    public fun unpinConversation(conversation: Conversation, onSuccess: (() -> Unit)? = null) {
        val with = conversationWith(conversation) ?: return
        // Offline (W3, G11): own unpin updates from onSuccess only; a listener echo is a no-op.
        if (offlineOn) {
            ConversationListOffline.setPinned(this, conversation, with, false, onSuccess, null)
            return
        }
        com.cometchat.chat.core.CometChat.unpinConversation(
            with,
            conversation.conversationType,
            object : com.cometchat.chat.core.CometChat.CallbackListener<Conversation>() {
                override fun onSuccess(updated: Conversation) {
                    applyConversationUnpinned(updated)
                    onSuccess?.invoke()
                }

                override fun onError(e: com.cometchat.chat.exceptions.CometChatException?) {}
            }
        )
    }

    /**
     * Applies a pin to the list: the conversation moves to the top, carrying the pin state from
     * [updated] onto the loaded instance (see [withPinState] for why the response object is not
     * swapped in wholesale). A conversation this list hasn't loaded is inserted as delivered —
     * a pin elsewhere must surface it at the top here.
     *
     * Shared by the REST success path and the SDK [ConversationListener] events, so the acting
     * device (which receives both — the SDK self-echoes conversation pin with no suppression
     * registry) applies them idempotently: remove-by-id + insert lands in the same position with
     * the same state the second time.
     */
    private fun applyConversationPinned(updated: Conversation) {
        val list = _conversations.value.toMutableList()
        val idx = list.indexOfFirst { it.conversationId == updated.conversationId }
        val pinned = if (idx >= 0) withPinState(list.removeAt(idx), updated) else updated
        list.add(0, pinned)
        _conversations.value = list
    }

    /**
     * Applies an unpin to the list: the conversation moves to the top of the unpinned section
     * (leaving it in place would strand it inside the pinned block). Unknown conversations are
     * ignored — there is nothing to reposition. Shares [applyConversationPinned]'s idempotency
     * contract.
     */
    private fun applyConversationUnpinned(updated: Conversation) {
        val list = _conversations.value.toMutableList()
        val idx = list.indexOfFirst { it.conversationId == updated.conversationId }
        if (idx >= 0) {
            val unpinned = withPinState(list.removeAt(idx), updated)
            list.add(firstUnpinnedIndex(list), unpinned)
            _conversations.value = list
        }
    }

    /**
     * Copies the pin attributes from a pin/unpin API response onto the conversation already in the
     * list, returning a fresh instance so the list emits an identity change.
     *
     * The SDK builds the response conversation with Conversation.fromJSON() over that endpoint's
     * payload alone, so every field the endpoint omits comes back at its default rather than at the
     * value the fetched conversation carried. Swapping the response object into the list therefore
     * silently drops state — most visibly Group.hasJoined, which defaults to false and makes an
     * unpinned group open with the composer replaced by the "no longer a member" notice.
     */
    private fun withPinState(existing: Conversation, response: Conversation): Conversation {
        return existing.clone().apply {
            pinnedAt = response.pinnedAt
            pinnedBy = response.pinnedBy
        }
    }

    /** The peer uid (1-1) or group guid (group) that identifies the conversation on the pin API. */
    private fun conversationWith(conversation: Conversation): String? {
        return when (val entity = conversation.conversationWith) {
            is com.cometchat.chat.models.User -> entity.uid
            is com.cometchat.chat.models.Group -> entity.guid
            else -> null
        }
    }

    public fun deleteConversation(conversation: Conversation) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.InProgress
            
            deleteConversationUseCase(conversation)
                .onSuccess {
                    _conversations.value = _conversations.value.filter { 
                        it.conversationId != conversation.conversationId 
                    }
                    _deleteState.value = DeleteState.Success
                    
                    if (_conversations.value.isEmpty()) {
                        _uiState.value = UIState.Empty
                    }
                }
                .onFailure { exception ->
                    val cometChatException = if (exception is CometChatException) {
                        exception
                    } else {
                        CometChatException(
                            "ERR_UNKNOWN",
                            exception.message ?: "Unknown error",
                            exception.localizedMessage ?: "Unknown error"
                        )
                    }
                    _deleteState.value = DeleteState.Failure(cometChatException)
                }
        }
    }
    
    /**
     * Resets delete state to idle.
     * Call after handling delete success/failure.
     */
    public fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }
    
    /**
     * Checks if a message should increment the unread count.
     * For CustomMessage, checks metadata for `incrementUnreadCount` boolean,
     * falling back to `willUpdateConversation()` method.
     * 
     * @param message The message to check
     * @return true if unread count should be incremented
     */
    open public fun willUpdateIncrementUnreadCount(message: BaseMessage): Boolean {
        if (message is CustomMessage) {
            val metadata = message.metadata
            if (metadata != null && metadata.has("incrementUnreadCount")) {
                return try {
                    metadata.getBoolean("incrementUnreadCount")
                } catch (e: Exception) {
                    false
                }
            }
            return message.willUpdateConversation()
        }
        return false
    }
    
    /**
     * Determines if a custom message should update the conversation.
     * Returns true if the message should increment unread count OR if settings allow custom messages.
     * 
     * @param message The custom message to check
     * @return true if the conversation should be updated
     */
    open public fun shouldUpdateConversationForCustomMessage(message: CustomMessage): Boolean {
        return willUpdateIncrementUnreadCount(message) || 
            CometChatUIKit.getConversationUpdateSettings().shouldUpdateOnCustomMessages()
    }
    
    /**
     * Checks if a message is a threaded reply.
     * A message is threaded if it has a parentMessageId > 0.
     * 
     * @param message The message to check
     * @return true if the message is a threaded reply
     */
    open public fun isThreadedMessage(message: BaseMessage): Boolean {
        return message.parentMessageId > 0
    }
    
    /**
     * Checks if a conversation should be added to the list based on the conversation type filter.
     * Returns true if:
     * - No filter is set (conversationsRequest is null or conversationType is null)
     * - Filter is set to BOTH
     * - Conversation type matches the filter
     * 
     * @param conversation The conversation to check
     * @return true if the conversation should be added to the list
     */
    private fun isAddToConversationList(conversation: Conversation?): Boolean {
        if (conversation == null) return false
        
        val request = conversationsRequest ?: return true
        val filterType = request.conversationType ?: return true
        
        if (filterType.equals(UIKitConstants.ConversationType.BOTH, ignoreCase = true)) {
            return true
        }
        
        return conversation.conversationType.equals(filterType, ignoreCase = true)
    }
    
    /**
     * Selects or deselects a conversation based on selection mode.
     */
    public fun selectConversation(conversation: Conversation, mode: UIKitConstants.SelectionMode) {
        when (mode) {
            UIKitConstants.SelectionMode.SINGLE -> {
                _selectedConversations.value = setOf(conversation)
            }
            UIKitConstants.SelectionMode.MULTIPLE -> {
                val current = _selectedConversations.value.toMutableSet()
                if (current.contains(conversation)) {
                    current.remove(conversation)
                } else {
                    current.add(conversation)
                }
                _selectedConversations.value = current
            }
            UIKitConstants.SelectionMode.NONE -> {
                // Do nothing
            }
        }
    }
    
    /**
     * Clears all selected conversations.
     */
    public fun clearSelection() {
        _selectedConversations.value = emptySet()
    }
    
    /**
     * Returns the list of currently selected conversations.
     */
    public fun getSelectedConversations(): List<Conversation> =
        _selectedConversations.value.toList()
    
    /**
     * Sets the conversations request builder for customizing fetch parameters.
     * This will store the builder and reset the current request to use the new configuration.
     * The builder will be used for both initial fetch and refresh operations.
     *
     * @param builder The custom request builder provided by the client
     */
    public fun setConversationsRequestBuilder(
        builder: ConversationsRequest.ConversationsRequestBuilder
    ) {
        conversationsRequestBuilder = builder
        conversationsRequest = builder.build()
    }
    
    /**
     * Sets whether to disable read receipts.
     */
    public fun setDisableReceipt(disable: Boolean) {
        disableReceipt = disable
    }
    
    /**
     * Sets whether to disable sound for incoming messages.
     */
    public fun setDisableSoundForMessages(disable: Boolean) {
        disableSoundForMessages = disable
    }
    
    /**
     * Sets a custom sound resource for incoming messages.
     * Pass 0 to use the default sound.
     *
     * @param rawRes The raw resource ID of the custom sound.
     */
    public fun setCustomSoundForMessage(@RawRes rawRes: Int) {
        customSoundForMessage = rawRes
    }
    
    /**
     * Initializes the sound manager with the given context.
     * Must be called before sounds can be played.
     *
     * @param context The application context.
     */
    public fun initSoundManager(context: Context) {
        if (soundManager == null) {
            soundManager = CometChatSoundManager(context.applicationContext)
        }
    }
    
    /**
     * Plays the incoming message sound if sound is not disabled.
     */
    private fun playIncomingMessageSound() {
        if (!disableSoundForMessages) {
            viewModelScope.launch {
                _playSoundEvent.emit(true)
            }
            soundManager?.play(Sound.INCOMING_MESSAGE_FROM_OTHER, customSoundForMessage)
        }
    }

    
    /**
     * Adds CometChat listeners for real-time updates.
     */
    private fun addListeners() {
        listenersTag = "ConversationList_${System.currentTimeMillis()}"

        // Offline hub collectors. Only started when offline is on, so the OFF path is untouched.
        if (offlineOn) {
            offlineHubJob?.cancel()
            offlineHubGen = OfflineHub.gen
            offlineHubJob = viewModelScope.launch {
                OfflineHub.events.collect { event -> onOfflineHubEvent(event) }
            }
            offlineNoticeJob?.cancel()
            offlineNoticeJob = offlineNoticeRelay.start(viewModelScope)
        }

        listenersTag?.let { tag ->
            // Conversation pin/unpin — from this device (SDK self-echo on REST success), and from
            // a second device / another surface once the backend emits the realtime frame. Routed
            // through the same apply helpers as this VM's own pin/unpin calls, which are
            // idempotent, so the acting device receiving both is harmless.
            CometChat.addConversationListener(tag, object : CometChat.ConversationListener() {
                override fun onConversationPinned(conversation: Conversation) {
                    // Offline (W3, UIK-CONV-01): match by (type, with); an own echo is a no-op.
                    if (offlineOn) {
                        ConversationListOffline.onPinEvent(this@CometChatConversationsViewModel, conversation, true)
                        return
                    }
                    applyConversationPinned(conversation)
                }

                override fun onConversationUnpinned(conversation: Conversation) {
                    if (offlineOn) {
                        ConversationListOffline.onPinEvent(this@CometChatConversationsViewModel, conversation, false)
                        return
                    }
                    applyConversationUnpinned(conversation)
                }
            })

            // Message listener for real-time message updates
            CometChat.addMessageListener(tag, object : CometChat.MessageListener() {
                override fun onTextMessageReceived(message: TextMessage) {
                    checkAndUpdateConversation(message, true)
                }
                
                override fun onMediaMessageReceived(message: MediaMessage) {
                    checkAndUpdateConversation(message, true)
                }
                
                override fun onCustomMessageReceived(message: CustomMessage) {
                    checkAndUpdateConversation(message, true)
                }

                override fun onCardMessageReceived(message: com.cometchat.chat.models.CardMessage) {
                    checkAndUpdateConversation(message, true)
                }

                override fun onAIAssistantMessageReceived(message: com.cometchat.chat.models.AIAssistantMessage) {
                    checkAndUpdateConversation(message, true)
                }
                
                override fun onMessageEdited(message: BaseMessage) {
                    // Offline (W3, BEH-CONV-06): an older message's update never replaces an unsent preview.
                    if (offlineOn && ConversationListOffline.onMessageUpdated(this@CometChatConversationsViewModel, message)) return
                    val conversation = CometChatHelper.getConversationFromMessage(message)
                    if (conversation != null) {
                        updateConversation(conversation, isActionMessage = false)
                    }
                }
                
                override fun onMessageDeleted(message: BaseMessage) {
                    // Offline (W3, BEH-CONV-06): an older message's update never replaces an unsent preview.
                    if (offlineOn && ConversationListOffline.onMessageUpdated(this@CometChatConversationsViewModel, message)) return
                    val conversation = CometChatHelper.getConversationFromMessage(message)
                    if (conversation != null) {
                        updateConversation(conversation, isActionMessage = false)
                    }
                }

                // ENG-39226 — the moderation verdict never reached the conversation row. The SDK
                // fires this callback (CometChat.java:865) but this ViewModel had no override at
                // all, so the row kept its pre-moderation lastMessage until the whole list was
                // rebuilt from cache — verified still stale four minutes after the verdict, on both
                // UI stacks. The stanza is a full message payload (server id, sentAt, attachments,
                // metadata), so the row can be rebuilt from it alone, exactly like an edit.
                // [updateConversation] clones before it stores, so the row never holds the shared
                // event object and the StateFlow emission is not conflated away (ENG-39118).
                override fun onMessageModerated(message: BaseMessage) {
                    // Offline (W3, BEH-CONV-06): an older message's update never replaces an unsent preview.
                    if (offlineOn && ConversationListOffline.onMessageUpdated(this@CometChatConversationsViewModel, message)) return
                    val conversation = CometChatHelper.getConversationFromMessage(message)
                    if (conversation != null) {
                        updateConversation(conversation, isActionMessage = false)
                    }
                }
                
                override fun onMessagesDelivered(messageReceipt: MessageReceipt) {
                    if (!disableReceipt) {
                        updateDeliveredReceipts(messageReceipt)
                    }
                }
                
                override fun onMessagesRead(messageReceipt: MessageReceipt) {
                    if (!disableReceipt) {
                        updateReadReceipts(messageReceipt)
                    }
                }

                // Group receipts arrive as DELIVERED_TO_ALL / READ_BY_ALL and the SDK dispatches them to
                // these two callbacks, never to onMessagesDelivered / onMessagesRead
                // (CometChat.java:161-167). Without these overrides the group branches of the two
                // handlers below were unreachable and a group tick only ever moved on a fetch or a sync,
                // never live (ENG-39131). CometChatMessageListViewModel has registered all four all along.
                override fun onMessagesDeliveredToAll(messageReceipt: MessageReceipt) {
                    if (!disableReceipt) {
                        updateDeliveredReceipts(messageReceipt)
                    }
                }

                override fun onMessagesReadByAll(messageReceipt: MessageReceipt) {
                    if (!disableReceipt) {
                        updateReadReceipts(messageReceipt)
                    }
                }
                
                override fun onTypingStarted(typingIndicator: TypingIndicator) {
                    addTypingIndicator(typingIndicator)
                }
                
                override fun onTypingEnded(typingIndicator: TypingIndicator) {
                    removeTypingIndicator(typingIndicator)
                }
            })
            
            // User listener for online/offline status
            CometChat.addUserListener(tag, object : CometChat.UserListener() {
                override fun onUserOnline(user: User) {
                    updateUserStatus(user)
                }
                
                override fun onUserOffline(user: User) {
                    updateUserStatus(user)
                }
            })
            
            // Group listener for group updates
            CometChat.addGroupListener(tag, object : CometChat.GroupListener() {
                override fun onGroupMemberJoined(
                    action: Action,
                    joinedUser: User,
                    joinedGroup: Group
                ) {
                    updateConversationForGroupAction(action, false)
                }
                
                override fun onGroupMemberLeft(
                    action: Action,
                    leftUser: User,
                    leftGroup: Group
                ) {
                    val isCurrentUser = isCurrentUser(leftUser)
                    updateConversationForGroupAction(action, isCurrentUser)
                }
                
                override fun onGroupMemberKicked(
                    action: Action,
                    kickedUser: User,
                    kickedBy: User,
                    kickedFrom: Group
                ) {
                    val isCurrentUser = isCurrentUser(kickedUser)
                    updateConversationForGroupAction(action, isCurrentUser)
                }
                
                override fun onMemberAddedToGroup(
                    action: Action,
                    addedby: User,
                    userAdded: User,
                    addedTo: Group
                ) {
                    // If current user was added, update the group scope
                    if (isCurrentUser(userAdded)) {
                        addedTo.scope = CometChatConstants.SCOPE_PARTICIPANT
                        addedTo.setHasJoined(true)
                        action.actionFor = addedTo
                    } else {
                        // Get existing group from conversation if available
                        val existingGroup = getGroupFromConversation(addedTo.guid)
                        if (existingGroup != null) {
                            action.actionFor = existingGroup
                        } else {
                            addedTo.setHasJoined(true)
                            addedTo.scope = CometChatConstants.SCOPE_PARTICIPANT
                            action.actionFor = addedTo
                        }
                    }
                    updateConversationForGroupAction(action, false)
                }

                override fun onGroupMemberScopeChanged(
                    action: Action?,
                    updatedBy: User?,
                    updatedUser: User?,
                    scopeChangedTo: String?,
                    scopeChangedFrom: String?,
                    group: Group?
                ) {
                    if (action != null && updatedUser != null && group != null && scopeChangedTo != null) {
                        // If current user's scope changed, update the group
                        if (isCurrentUser(updatedUser)) {
                            group.scope = scopeChangedTo
                            group.setHasJoined(true)
                            updateGroupInConversation(group)
                        }
                        updateConversationForGroupAction(action, false)
                    }
                }

                override fun onGroupMemberBanned(
                    action: Action?,
                    bannedUser: User?,
                    bannedBy: User?,
                    group: Group?
                ) {
                    if (action != null && bannedUser != null) {
                        val isCurrentUser = isCurrentUser(bannedUser)
                        updateConversationForGroupAction(action, isCurrentUser)
                    }
                }
            })
            
            // Connection listener for reconnection
            CometChat.addConnectionListener(tag, object : CometChat.ConnectionListener {
                override fun onConnected() {
                    // Offline reconnect seam (W3): consume the SDK sync slice instead of refreshing
                    // (G7). OFF untouched; W1 returns false and today's refresh runs.
                    if (offlineOn && ConversationListOffline.onConnected(this@CometChatConversationsViewModel)) return
                    refreshList()
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
            
            // Calls SDK listener (ringing v2): a call starting or ending, on either side, updates
            // that conversation's preview. The Chat SDK's CallListener is deprecated and not used.
            if (CallsUtils.isCallsSDKAvailable()) {
                CometChatCalls.addIncomingCallListener(tag, object : CometChatCalls.IncomingCallListener() {
                    override fun onIncomingCallReceived(call: InitiatedCall) {
                        if (CometChatUIKit.getConversationUpdateSettings().shouldUpdateOnCallActivities()) {
                            updateConversationWithCall(CallInitiationUtils.toCall(call))
                        }
                    }

                    override fun onIncomingCallEnded(call: InitiatedCall, reason: String) {
                        if (CometChatUIKit.getConversationUpdateSettings().shouldUpdateOnCallActivities()) {
                            updateConversationWithCall(CallInitiationUtils.toCall(call))
                        }
                    }
                })
            }
        }
        
        // Add local event listeners
        addLocalEventListeners()
    }
    
    /**
     * Adds local event listeners for UI-triggered events.
     * These events are emitted by other UI components (e.g., when a message is sent from MessageComposer).
     */
    private fun addLocalEventListeners() {
        // Conversation events (e.g., conversation deleted from another screen)
        conversationEventsJob = viewModelScope.launch {
            CometChatEvents.conversationEvents.collect { event ->
                when (event) {
                    is CometChatConversationEvent.ConversationDeleted -> {
                        removeConversation(event.conversation)
                    }
                    is CometChatConversationEvent.ConversationUpdated -> {
                        updateConversationInList(event.conversation)
                    }
                }
            }
        }
        
        // Group events (e.g., group deleted, user left group from another screen)
        groupEventsJob = viewModelScope.launch {
            CometChatEvents.groupEvents.collect { event ->
                when (event) {
                    is CometChatGroupEvent.GroupDeleted -> {
                        removeGroup(event.group)
                    }
                    is CometChatGroupEvent.GroupLeft -> {
                        removeGroup(event.group)
                    }
                    is CometChatGroupEvent.MemberJoined -> {
                        updateGroupInConversation(event.group)
                    }
                    is CometChatGroupEvent.MembersAdded -> {
                        updateGroupInConversation(event.group)
                        // Fetch the conversation from server to get the real last message
                        // (locally-constructed Actions have id=0 and message=null)
                        refreshConversationFromServer(event.group.guid, CometChatConstants.CONVERSATION_TYPE_GROUP)
                    }
                    is CometChatGroupEvent.MemberKicked -> {
                        updateGroupInConversation(event.group)
                        refreshConversationFromServer(event.group.guid, CometChatConstants.CONVERSATION_TYPE_GROUP)
                    }
                    is CometChatGroupEvent.MemberBanned -> {
                        updateGroupInConversation(event.group)
                        refreshConversationFromServer(event.group.guid, CometChatConstants.CONVERSATION_TYPE_GROUP)
                    }
                    is CometChatGroupEvent.MemberUnbanned -> {
                        updateGroupInConversation(event.group)
                        refreshConversationFromServer(event.group.guid, CometChatConstants.CONVERSATION_TYPE_GROUP)
                    }
                    is CometChatGroupEvent.MemberScopeChanged -> {
                        updateGroupInConversation(event.group)
                        refreshConversationFromServer(event.group.guid, CometChatConstants.CONVERSATION_TYPE_GROUP)
                    }
                    is CometChatGroupEvent.OwnershipChanged -> {
                        updateGroupInConversation(event.group)
                    }
                    is CometChatGroupEvent.GroupCreated -> {
                        // Offline (W3): a merging refresh; an explicit refreshList() replaces the list.
                        if (offlineOn && ConversationListOffline.onGroupCreated(this@CometChatConversationsViewModel)) return@collect
                        // Refresh list to show new group conversation
                        refreshList()
                    }
                }
            }
        }
        
        // User events (e.g., user blocked/unblocked from another screen)
        userEventsJob = viewModelScope.launch {
            CometChatEvents.userEvents.collect { event ->
                when (event) {
                    is CometChatUserEvent.UserBlocked -> {
                        // Remove user conversation if blocked users are not included
                        // Note: This depends on conversationsRequest configuration
                        removeUser(event.user)
                    }
                    is CometChatUserEvent.UserUnblocked -> {
                        // Update user status in conversation
                        updateUserStatus(event.user)
                    }
                }
            }
        }
        
        // Message events (e.g., message sent from MessageComposer)
        messageEventsJob = viewModelScope.launch {
            CometChatLogger.d("CometChatConvListVM", "Started collecting messageEvents")
            CometChatEvents.messageEvents.collect { event ->
                CometChatLogger.d("CometChatConvListVM", "Received messageEvent: ${event::class.simpleName}")
                when (event) {
                    is CometChatMessageEvent.MessageSent -> {
                        if (event.status == MessageStatus.SUCCESS &&
                            !(offlineOn && ConversationListOffline.onMessageSent(this@CometChatConversationsViewModel, event.message))
                        ) {
                            checkAndUpdateConversation(event.message, false)
                        }
                    }
                    is CometChatMessageEvent.MessageEdited -> {
                        if (event.status == MessageStatus.SUCCESS) {
                            checkAndUpdateConversation(event.message, false)
                        }
                    }
                    is CometChatMessageEvent.MessageDeleted -> {
                        checkAndUpdateConversation(event.message, false)
                    }
                    is CometChatMessageEvent.MessageRead -> {
                        CometChatLogger.d("CometChatConvListVM", "Received MessageRead event - conversationId=${event.message.conversationId}, messageId=${event.message.id}")
                        // Offline: the flush's read, older than another user's last message, takes the server's count.
                        if (!(offlineOn && ConversationListOffline.onMessageRead(this@CometChatConversationsViewModel, event.message))) {
                            clearUnreadCountForMessage(event.message)
                        }
                    }
                    is CometChatMessageEvent.TextMessageReceived -> {
                        checkAndUpdateConversation(event.message, true)
                    }
                    is CometChatMessageEvent.MediaMessageReceived -> {
                        checkAndUpdateConversation(event.message, true)
                    }
                    is CometChatMessageEvent.CustomMessageReceived -> {
                        checkAndUpdateConversation(event.message, true)
                    }
                    else -> {
                        // Other message events don't affect conversation list
                    }
                }
            }
        }
        
        // Call events (e.g., outgoing call initiated from CallButton)
        // These are UIKit local events for UI-initiated call actions
        callEventsJob = viewModelScope.launch {
            CometChatEvents.callEvents.collect { event ->
                if (CometChatUIKit.getConversationUpdateSettings().shouldUpdateOnCallActivities()) {
                    when (event) {
                        is CometChatCallEvent.OutgoingCall -> updateConversationWithCall(event.call)
                        is CometChatCallEvent.CallAccepted -> updateConversationWithCall(event.call)
                        is CometChatCallEvent.CallRejected -> updateConversationWithCall(event.call)
                        is CometChatCallEvent.CallEnded -> updateConversationWithCall(event.call)
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
            CometChat.removeConversationListener(tag)
            CometChat.removeMessageListener(tag)
            CometChat.removeUserListener(tag)
            CometChat.removeGroupListener(tag)
            CometChat.removeConnectionListener(tag)
            if (CallsUtils.isCallsSDKAvailable()) {
                CometChatCalls.removeIncomingCallListener(tag)
            }
        }
    }
    

    
    /**
     * Updates conversation receipt status for delivered receipts.
     * Matches the Java implementation's updateDeliveredReceipts() method logic, with the id match relaxed
     * to the cumulative rule on **both** the USER and the GROUP path (ENG-39131).
     *
     * For USER conversations: matches by receipt.sender.uid == conversationWith.uid, cumulatively for
     * any own-sent `lastMessage` at or below the receipt anchor (`id <= receipt.messageId`).
     * For GROUP conversations: matches by receipt.receiverId == conversationWith.guid AND DELIVERED_TO_ALL
     * type, then the same cumulative anchor. DELIVERED_TO_ALL / READ_BY_ALL remain the only group receipt
     * types handled and the guid match is unchanged — only the id comparison relaxed.
     *
     * Why one rule for both: the SDK already treats the two the same. `ReceiptScope.resolve` maps
     * DELIVERED_TO_ALL to `deliveredAt` and READ_BY_ALL to `readAt` — the same two columns as 1:1 — and
     * files them against the same cumulative `id <=` anchor (per-member group receipts are V2). A strict
     * group path here would make a group tick correct after a restart and wrong live.
     *
     * Why the match is cumulative: a receipt anchors at the newest id the peer has seen, which is not
     * necessarily the id sitting in this row. `id == receipt.messageId` therefore dropped a receipt
     * whenever the row was behind the anchor, and nothing re-evaluates a dropped receipt afterwards —
     * one miss and the tick stays wrong until something unrelated re-bumps the row. Cumulative matching
     * is the same semantic `CometChatMessageListViewModel.setDeliveryReceipts` uses in memory and the SDK
     * uses in SQL (`MessageDao.applyReceipt`, ENG-36822).
     *
     * What the pre-ack (`id = 0`) row does and does not buy. Stamping it is correct bookkeeping, but it
     * is **not** what repairs a stuck tick, and it cannot be:
     * - The renderer maps `id == 0L` to IN_PROGRESS **before** it looks at `readAt` / `deliveredAt`
     *   (`MessageReceiptUtils.kt:66-68`, identically on both UI stacks), so a stamped pre-ack row still
     *   draws a clock. That precedence is deliberate — an un-acked send has not been sent yet.
     * - On ack, [updateConversation] replaces `lastMessage` wholesale, carrying over only
     *   `conversationWith` and `unreadMessageCount`, so the stamp is discarded anyway.
     *
     * What repairs the tick is the pair below, both on **real** server ids:
     * - the cumulative match, which stamps a post-ack row whose id is at or below a later anchor — the
     *   case the strict `==` dropped; and
     * - stamping the **clone**, which gives the new list different content and so defeats
     *   `MutableStateFlow`'s conflation (`Conversation.equals` is `contentEquals` over `lastMessage`,
     *   and `clone()` deep-copies it). Mutating the shared message in place left the new list equal to
     *   the old one and the emission was swallowed — the ENG-39118 failure mode, one level up.
     *
     * Guards that survive the relaxation:
     * - `deliveredAt != 0L` still short-circuits, so a timestamp never moves backwards.
     * - The row's `lastMessage` must be our own sent message, so an **incoming** `lastMessage` is never
     *   stamped with our delivery state (the case the strict id match protected implicitly).
     *
     * @param receipt The message receipt containing delivery information
     */
    @VisibleForTesting
    internal fun updateDeliveredReceipts(receipt: MessageReceipt) {
        viewModelScope.launch {
            val loggedInUserUid = getLoggedInUserSafe()?.uid
            _conversations.value = _conversations.value.map { conversation ->
                val lastMessage = conversation.lastMessage

                // Skip if no last message, already delivered, or the row's last message is not one we sent.
                if (lastMessage == null ||
                    lastMessage.deliveredAt != 0L ||
                    !isOwnMessage(lastMessage, loggedInUserUid)) {
                    return@map conversation
                }

                val shouldUpdate = when (receipt.receiverType) {
                    UIKitConstants.ReceiverType.USER -> {
                        // For user conversations: match by sender UID, cumulatively up to the anchor
                        conversation.conversationType == CometChatConstants.RECEIVER_TYPE_USER &&
                            receipt.sender?.uid == (conversation.conversationWith as? User)?.uid &&
                            lastMessage.id <= receipt.messageId
                    }
                    UIKitConstants.ReceiverType.GROUP -> {
                        // For group conversations: match by receiver ID and DELIVERED_TO_ALL type,
                        // cumulatively up to the anchor — same rule as the 1:1 branch above.
                        conversation.conversationType == CometChatConstants.RECEIVER_TYPE_GROUP &&
                            receipt.receiptType == MessageReceipt.RECEIPT_TYPE_DELIVERED_TO_ALL &&
                            receipt.receiverId == (conversation.conversationWith as? Group)?.guid &&
                            lastMessage.id <= receipt.messageId
                    }
                    else -> false
                }

                if (shouldUpdate) {
                    // Clone to create new reference for Compose recomposition, and stamp the CLONE's own
                    // lastMessage. Mutating the shared message in place would leave the new list
                    // content-equal to the old one (Conversation.equals is contentEquals, and the two rows
                    // would share the mutated message), and MutableStateFlow conflates by equals — the
                    // update would never be emitted. Same failure mode as ENG-39118, one level up.
                    conversation.clone().apply {
                        this.lastMessage?.deliveredAt = receipt.deliveredAt
                    }
                } else {
                    conversation
                }
            }
        }
    }

    /**
     * Updates conversation receipt status for read receipts.
     * Matches the Java implementation's updateReadReceipts() method logic, with the same cumulative
     * relaxation and the same own-sent guard as [updateDeliveredReceipts], on both paths (ENG-39131).
     *
     * For USER conversations: matches by receipt.sender.uid == conversationWith.uid, cumulatively for
     * any own-sent `lastMessage` at or below the receipt anchor (`id <= receipt.messageId`).
     * For GROUP conversations: matches by receipt.receiverId == conversationWith.guid, then — to stamp —
     * READ_BY_ALL type and the same cumulative anchor. See [updateDeliveredReceipts] for why the two paths
     * share one rule.
     *
     * A read receipt implies delivered, so stamping `readAt` also advances an unset `deliveredAt`,
     * matching `MessageDao.applyReceipt`, which advances `deliveredAt` on every read receipt.
     *
     * Also clears the unread count when the receipt is from the logged-in user (our own read, echoed
     * from another device) — on the row the receipt is about and no other. On both paths that scope
     * comes from the same match that gates the stamp: the peer uid for 1:1, the guid for a group.
     * Only the group clear is reachable today: the 1:1 arm additionally requires the receipt's sender to
     * be this row's peer *and* the logged-in user, which can only hold in a self-conversation.
     *
     * @param receipt The message receipt containing read information
     */
    @VisibleForTesting
    internal fun updateReadReceipts(receipt: MessageReceipt) {
        viewModelScope.launch {
            val loggedInUser = getLoggedInUserSafe()
            val isReceiptFromLoggedInUser = loggedInUser != null &&
                receipt.sender?.uid?.equals(loggedInUser.uid, ignoreCase = true) == true

            _conversations.value = _conversations.value.map { conversation ->
                val lastMessage = conversation.lastMessage
                // An incoming lastMessage is never stamped with our own read state.
                val isOwnLastMessage = lastMessage != null && isOwnMessage(lastMessage, loggedInUser?.uid)

                when (receipt.receiverType) {
                    UIKitConstants.ReceiverType.USER -> {
                        // For user conversations: match by sender UID
                        if (conversation.conversationType == CometChatConstants.RECEIVER_TYPE_USER &&
                            receipt.sender?.uid == (conversation.conversationWith as? User)?.uid) {

                            // Check if we should update readAt timestamp — cumulatively, so a row still
                            // holding the pre-ack copy of an own message is stamped too.
                            if (lastMessage != null &&
                                lastMessage.readAt == 0L &&
                                isOwnLastMessage &&
                                lastMessage.id <= receipt.messageId) {
                                stampRead(conversation, receipt)
                            } else if (isReceiptFromLoggedInUser) {
                                // Clear unread count when receipt is from logged-in user
                                conversation.clone().apply {
                                    unreadMessageCount = 0
                                }
                            } else {
                                conversation
                            }
                        } else {
                            conversation
                        }
                    }
                    UIKitConstants.ReceiverType.GROUP -> {
                        // Everything below is scoped to the row for the group this receipt is about —
                        // the guid match is the group-path equivalent of the peer match on the 1:1 branch,
                        // and, like it, it gates the unread-count clear as well as the stamp (ENG-39131).
                        // It used to gate only the stamp, so a self read receipt in one group zeroed
                        // `unreadMessageCount` on every OTHER row, 1:1 rows included.
                        val isReceiptsGroupRow =
                            conversation.conversationType == CometChatConstants.RECEIVER_TYPE_GROUP &&
                                receipt.receiverId == (conversation.conversationWith as? Group)?.guid

                        if (!isReceiptsGroupRow) {
                            conversation
                        } else if (receipt.receiptType == MessageReceipt.RECEIPT_TYPE_READ_BY_ALL &&
                            // Check if we should update readAt timestamp — cumulatively, as in the 1:1
                            // branch above, so a row still holding the pre-ack copy is stamped too.
                            lastMessage != null &&
                            lastMessage.readAt == 0L &&
                            isOwnLastMessage &&
                            lastMessage.id <= receipt.messageId
                        ) {
                            stampRead(conversation, receipt)
                        } else if (isReceiptFromLoggedInUser) {
                            // Our own read echoed from another device: clear THIS group's badge only.
                            // Deliberately not gated on READ_BY_ALL — a self read receipt arrives as a
                            // plain READ, and that is the case this clear exists for.
                            conversation.clone().apply {
                                unreadMessageCount = 0
                            }
                        } else {
                            conversation
                        }
                    }
                    else -> conversation
                }
            }
        }
    }

    /**
     * Stamps the row's `lastMessage` as read and returns the cloned row (a new reference and new content,
     * so the StateFlow emits and Compose recomposes).
     *
     * A read receipt implies delivered: an unset `deliveredAt` is advanced with the receipt too, so a row
     * whose delivery receipt was never seen does not stay on a single tick. Never moves either timestamp
     * backwards — the caller only stamps `readAt` when unset, and `deliveredAt` is only advanced when unset.
     */
    private fun stampRead(
        conversation: Conversation,
        receipt: MessageReceipt
    ): Conversation {
        // Clone to create new reference for Compose recomposition, and stamp the CLONE's own
        // lastMessage — see [updateDeliveredReceipts] for why the shared message must not be mutated.
        return conversation.clone().apply {
            this.lastMessage?.let { cloned ->
                cloned.readAt = receipt.readAt
                if (cloned.deliveredAt == 0L) {
                    cloned.deliveredAt = if (receipt.deliveredAt != 0L) receipt.deliveredAt else receipt.readAt
                }
            }
        }
    }

    /**
     * True when [message] was sent by the logged-in user.
     *
     * Guards the receipt handlers so an **incoming** `lastMessage` is never stamped with our own
     * delivery/read state (ENG-39131). Returns false when the logged-in user is unknown (SDK not
     * initialised), which keeps the handlers no-ops rather than stamping on a guess.
     */
    private fun isOwnMessage(message: BaseMessage, loggedInUserUid: String?): Boolean {
        if (loggedInUserUid.isNullOrEmpty()) return false
        return message.sender?.uid?.equals(loggedInUserUid, ignoreCase = true) == true
    }

    /**
     * Updates conversation receipt status (legacy method - kept for compatibility).
     */
    private fun updateConversationReceipt(receipt: MessageReceipt) {
        viewModelScope.launch {
            _conversations.value = _conversations.value.map { conversation ->
                if (conversation.lastMessage?.id == receipt.messageId) {
                    // Clone to create new reference for Compose recomposition
                    conversation.clone()
                } else {
                    conversation
                }
            }
        }
    }
    
    /**
     * Updates unread count for a conversation. Never moves the row or changes its last message.
     * `internal` for the offline read path ([ConversationListOffline.onMessageRead]).
     */
    internal fun updateConversationUnreadCount(conversation: Conversation, count: Int) {
        CometChatLogger.d("CometChatConvListVM", "updateConversationUnreadCount() - conversationId=${conversation.conversationId}, newCount=$count")
        _conversations.value = _conversations.value.map {
            if (it.conversationId == conversation.conversationId) {
                // Clone to create new reference for Compose recomposition
                CometChatLogger.d("CometChatConvListVM", "updateConversationUnreadCount() - Updated conversation ${it.conversationId} unreadCount from ${it.unreadMessageCount} to $count")
                it.clone().apply { unreadMessageCount = count }
            } else {
                it
            }
        }
    }
    
    /**
     * Applies an externally supplied [Conversation] onto the matching entry in the list
     * without moving it to the top.
     *
     * Merge semantics: every field the caller actually supplied is taken, and the rest of
     * the existing entry is preserved. This is what lets an integrator refresh
     * [Conversation.conversationWith] — a [com.cometchat.chat.models.Group] whose metadata
     * changed server-side, say — for which the SDK emits no real-time event.
     *
     * The counters — [Conversation.unreadMessageCount], [Conversation.unreadMentionsCount],
     * [Conversation.lastReadMessageId] and [Conversation.latestMessageId] — are always taken
     * from [conversation]. An `Int`/`Long` has no "unset" value to tell apart from a deliberate
     * zero (marking a conversation read), so the supplied object stays authoritative for them.
     * A caller pushing an update for some other reason should source the conversation from the
     * SDK rather than hand-building one, so the counters carry real values:
     *
     * ```kotlin
     * CometChat.getConversation(guid, CometChatConstants.CONVERSATION_TYPE_GROUP,
     *     object : CometChat.CallbackListener<Conversation>() {
     *         override fun onSuccess(conversation: Conversation) {
     *             CometChatEvents.emitConversationEvent(ConversationUpdated(conversation))
     *         }
     *         override fun onError(e: CometChatException) = Unit
     *     })
     * ```
     *
     * [CometChatHelper.getConversationFromMessage] avoids the network call but leaves every
     * counter at zero, so anything built that way must carry the current values over first.
     *
     * Visibility is `internal` rather than `private` so unit tests can drive it directly;
     * it is not part of the public API.
     *
     * @param conversation The conversation carrying the updated properties.
     */
    internal fun updateConversationInList(conversation: Conversation) {
        if (offlineOn) ConversationListOffline.onLiveWrite(this, conversation.conversationId)
        _conversations.value = _conversations.value.map { existing ->
            if (existing.conversationId != conversation.conversationId) existing
            else mergeConversationUpdate(existing, conversation)
        }
    }
    
    /**
     * Adds a typing indicator and updates the conversation's isReceiverTyping property.
     * Updates immediately without debouncing.
     */
    private fun addTypingIndicator(typingIndicator: TypingIndicator) {
        val key = getTypingIndicatorKey(typingIndicator)
        typingIndicatorHashMap[key] = typingIndicator
        // Update immediately when typing starts
        _typingIndicators.value = typingIndicatorHashMap.toMap()
    }
    
    /**
     * Removes a typing indicator and updates the conversation's isReceiverTyping property.
     * Uses debouncing to prevent flickering when multiple users are typing.
     */
    private fun removeTypingIndicator(typingIndicator: TypingIndicator) {
        val key = getTypingIndicatorKey(typingIndicator)
        typingIndicatorHashMap.remove(key)
        
        // Cancel any pending debounce job
        typingDebounceJob?.cancel()
        
        // Debounce the update when typing ends to prevent flickering
        typingDebounceJob = viewModelScope.launch {
            delay(TYPING_INDICATOR_DEBOUNCER)
            _typingIndicators.value = typingIndicatorHashMap.toMap()
        }
    }
    
    /**
     * Gets the entity ID (user UID or group GUID) from a conversation.
     */
    private fun getConversationEntityId(conversation: Conversation): String? {
        return when (conversation.conversationType) {
            UIKitConstants.ConversationType.USERS -> (conversation.conversationWith as? User)?.uid
            UIKitConstants.ConversationType.GROUPS -> (conversation.conversationWith as? Group)?.guid
            else -> null
        }
    }
    
    /**
     * Gets a unique key for a typing indicator.
     */
    private fun getTypingIndicatorKey(typingIndicator: TypingIndicator): String {
        return "${typingIndicator.receiverType}_${typingIndicator.receiverId}_${typingIndicator.sender.uid}"
    }
    
    /**
     * Updates user status in conversations.
     */
    private fun updateUserStatus(user: User) {
        viewModelScope.launch {
            _conversations.value = _conversations.value.map { conversation ->
                if (conversation.conversationType == UIKitConstants.ConversationType.USERS) {
                    val conversationUser = conversation.conversationWith as? User
                    if (conversationUser?.uid == user.uid) {
                        // Clone to create new reference for Compose recomposition
                        conversation.clone().apply { 
                            conversationWith = user 
                        }
                    } else {
                        conversation
                    }
                } else {
                    conversation
                }
            }
        }
    }
    
    /**
     * Safely gets the logged-in user, returning null if SDK is not initialized.
     */
    internal fun getLoggedInUserSafe(): User? {
        return try {
            CometChatUIKit.getLoggedInUser()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Checks if the given user is the current logged-in user.
     */
    private fun isCurrentUser(user: User?): Boolean {
        val loggedInUser = getLoggedInUserSafe() ?: return false
        return user?.uid?.equals(loggedInUser.uid, ignoreCase = true) == true
    }
    
    /**
     * Gets a group from the conversation list by GUID.
     */
    private fun getGroupFromConversation(guid: String): Group? {
        for (conversation in _conversations.value) {
            if (conversation.conversationType == UIKitConstants.ConversationType.GROUPS) {
                val group = conversation.conversationWith as? Group
                if (group?.guid == guid) {
                    return group
                }
            }
        }
        return null
    }
    
    /**
     * Updates conversation for group actions (member joined, left, kicked, banned, etc.).
     * Converts the action to a conversation and updates the list.
     * 
     * @param action The group action message
     * @param isRemove Whether to remove the conversation (e.g., current user left/kicked/banned)
     */
    /**
     * Fetches the conversation from the server and updates the conversation list.
     * Used for group action events (member added/kicked/banned/etc.) where the
     * locally-constructed Action objects have id=0 and message=null.
     * The server provides the real conversation with proper last message.
     *
     * @param id The conversation entity ID (group GUID or user UID)
     * @param type The conversation type (group or user)
     */
    private fun refreshConversationFromServer(id: String, type: String) {
        viewModelScope.launch {
            // Two-phase read (BEH-READ-32), settled once on the server phase with no cached fallback:
            // this refresh exists to get the server's post-action row, and a cached row predates the
            // action. Offline it fails and is logged, as before.
            CometChat.getConversation(id, type, ConversationDetailsOnce(cachedFallback = false) { result ->
                result
                    .onSuccess { conversation ->
                        if (conversation.lastMessage != null) {
                            updateConversation(conversation, isActionMessage = true)
                        }
                    }
                    .onFailure { e ->
                        CometChatLogger.e("CometChatConvListVM", "Failed to refresh conversation: ${e.message}")
                    }
            })
        }
    }

    internal fun updateConversationForGroupAction(action: Action, isRemove: Boolean) {
        viewModelScope.launch {
            // Null safety check - ensure action has required fields before calling SDK
            // The SDK's CometChatHelper.getConversationFromMessage can crash if receiverType is null
            if (action.receiverType.isNullOrEmpty() || action.receiverUid.isNullOrEmpty()) {
                return@launch
            }
            
            val conversation = try {
                CometChatHelper.getConversationFromMessage(action)
            } catch (e: Exception) {
                CometChatLogger.e("CometChatConvListVM", "Error getting conversation from action: ${e.message}")
                null
            } ?: return@launch
            
            if (isRemove) {
                removeConversation(conversation)
            } else {
                // Only update if settings allow group actions
                if (CometChatUIKit.getConversationUpdateSettings().shouldUpdateOnGroupActions()) {
                    updateConversation(conversation, isActionMessage = true)
                }
            }
        }
    }
    
    /**
     * Updates conversation with a call event.
     * Converts the call to a conversation and updates the list.
     * 
     * @param call The call to update the conversation with
     */
    private fun updateConversationWithCall(call: Call) {
        // Ringing v2: a Call built from the Calls SDK has no message id; it is not a chat
        // message and must not become a conversation's last message.
        if (call.id <= 0) return
        viewModelScope.launch {
            // Null safety check - ensure call has required fields before calling SDK
            if (call.receiverType.isNullOrEmpty() || call.receiverUid.isNullOrEmpty()) {
                CometChatLogger.w("CometChatConvListVM", "Skipping updateConversationWithCall: call has null receiverType or receiverUid")
                return@launch
            }
            
            val conversation = try {
                CometChatHelper.getConversationFromMessage(call)
            } catch (e: Exception) {
                CometChatLogger.e("CometChatConvListVM", "Error getting conversation from call: ${e.message}")
                null
            } ?: return@launch
            
            updateConversation(conversation, isActionMessage = false)
        }
    }
    
    /**
     * Removes a conversation from the list.
     */
    private fun removeConversation(conversation: Conversation) {
        val currentList = _conversations.value
        val newList = currentList.filter { it.conversationId != conversation.conversationId }
        _conversations.value = newList
        
        if (newList.isEmpty()) {
            _uiState.value = UIState.Empty
        } else {
            _uiState.value = UIState.Content(newList)
        }
    }
    
    /**
     * Removes a group conversation from the list.
     */
    private fun removeGroup(group: Group) {
        val currentList = _conversations.value
        val newList = currentList.filter { conversation ->
            if (conversation.conversationType == UIKitConstants.ConversationType.GROUPS) {
                val conversationGroup = conversation.conversationWith as? Group
                conversationGroup?.guid != group.guid
            } else {
                true
            }
        }
        _conversations.value = newList
        
        if (newList.isEmpty()) {
            _uiState.value = UIState.Empty
        } else {
            _uiState.value = UIState.Content(newList)
        }
    }
    
    /**
     * Removes a user conversation from the list.
     */
    private fun removeUser(user: User) {
        val currentList = _conversations.value
        val newList = currentList.filter { conversation ->
            if (conversation.conversationType == UIKitConstants.ConversationType.USERS) {
                val conversationUser = conversation.conversationWith as? User
                conversationUser?.uid != user.uid
            } else {
                true
            }
        }
        _conversations.value = newList
        
        if (newList.isEmpty()) {
            _uiState.value = UIState.Empty
        } else {
            _uiState.value = UIState.Content(newList)
        }
    }
    
    /**
     * Checks if the message should update the conversation based on threading and settings.
     * For threaded messages, checks shouldUpdateOnMessageReplies() setting.
     * Matches the Java implementation's checkAndUpdateConversation() method.
     * 
     * @param message The message to check
     * @param markAsDeliver Whether to mark the message as delivered (true for incoming messages)
     */
    private fun checkAndUpdateConversation(message: BaseMessage, markAsDeliver: Boolean) {
        // Offline preview seam (W3): place the pending/own preview from the hub. OFF untouched;
        // W1 returns false and today's path runs.
        if (offlineOn && ConversationListOffline.onMessageListenerEvent(this, message, markAsDeliver)) return
        if (isThreadedMessage(message)) {
            if (CometChatUIKit.getConversationUpdateSettings().shouldUpdateOnMessageReplies()) {
                handleMessageUpdate(message, markAsDeliver)
            }
        } else {
            handleMessageUpdate(message, markAsDeliver)
        }
    }
    
    /**
     * Handles message update with custom message filtering.
     * For CustomMessage, checks shouldUpdateConversationForCustomMessage().
     * Matches the Java implementation's handleMessageUpdate() method.
     * 
     * @param message The message to update
     * @param markAsDeliver Whether to mark the message as delivered
     */
    private fun handleMessageUpdate(message: BaseMessage, markAsDeliver: Boolean) {
        if (message is CustomMessage) {
            if (shouldUpdateConversationForCustomMessage(message)) {
                updateMessageDeliveryStatus(message, markAsDeliver)
            }
        } else {
            updateMessageDeliveryStatus(message, markAsDeliver)
        }
    }
    
    /**
     * Updates the conversation with the message and optionally marks as delivered.
     * Matches the Java implementation's updateMessageDeliveryStatus() method.
     * 
     * @param message The message to update
     * @param markAsDeliver Whether to mark the message as delivered and play sound
     */
    private fun updateMessageDeliveryStatus(message: BaseMessage, markAsDeliver: Boolean) {
        if (markAsDeliver) {
            processMessage(message)
        } else {
            val conversation = CometChatHelper.getConversationFromMessage(message)
            if (conversation != null) {
                updateConversation(conversation, isActionMessage = false)
            }
        }
    }
    
    /**
     * Processes an incoming message by marking it as delivered and updating the conversation.
     * Matches the Java implementation's processMessage() method.
     * 
     * @param message The message to process
     */
    private fun processMessage(message: BaseMessage) {
        markAsDeliverInternally(message)
        val conversation = CometChatHelper.getConversationFromMessage(message)
        if (conversation != null) {
            updateConversation(conversation, isActionMessage = false)
        }
        playIncomingMessageSound()
    }
    
    /**
     * Marks a message as delivered internally if conditions are met.
     * Only marks as delivered if:
     * - The message sender is not the logged-in user
     * - Receipts are not disabled
     * Matches the Java implementation's markAsDeliverInternally() method.
     * 
     * @param message The message to mark as delivered
     */
    private fun markAsDeliverInternally(message: BaseMessage) {
        val loggedInUser = getLoggedInUserSafe() ?: return
        val senderUid = message.sender?.uid ?: return
        
        if (!senderUid.equals(loggedInUser.uid, ignoreCase = true) && !disableReceipt) {
            CometChat.markAsDelivered(message)
        }
    }
    
    /**
     * Clears unread count for a conversation based on a message.
     */
    private fun clearUnreadCountForMessage(message: BaseMessage) {
        val conversationId = message.conversationId
        val currentList = _conversations.value
        
        CometChatLogger.d("CometChatConvListVM", "clearUnreadCountForMessage() - conversationId=$conversationId, currentListSize=${currentList.size}")
        
        val conversation = currentList.find { it.conversationId == conversationId }
        if (conversation != null) {
            CometChatLogger.d("CometChatConvListVM", "clearUnreadCountForMessage() - Found conversation, current unreadCount=${conversation.unreadMessageCount}, updating to 0")
            updateConversationUnreadCount(conversation, 0)
        } else {
            CometChatLogger.d("CometChatConvListVM", "clearUnreadCountForMessage() - Conversation NOT found in list. Available conversationIds: ${currentList.map { it.conversationId }}")
        }
    }
    
    /**
     * Insertion point for a conversation surfacing on new activity: the first index past the
     * pinned block at the head of the list. Pinned conversations always stay above realtime
     * reordering, matching the backend's pinned-first fetch ordering.
     */
    internal fun firstUnpinnedIndex(list: List<Conversation>): Int {
        val index = list.indexOfFirst { !it.isPinned }
        return if (index >= 0) index else list.size
    }

    /**
     * Updates a conversation in the list with proper handling of last message and unread count.
     * Matches the Java implementation's update() method logic.
     *
     * **Moderation: the ack must never downgrade the row's own DISAPPROVED lastMessage
     * (ENG-39226).** This function replaces `lastMessage` wholesale, carrying over only
     * `conversationWith` and `unreadMessageCount`. The send ack arrives 2–21 ms *after* the
     * moderation verdict and carries `moderation.status = "pending"`, so without a guard the ack
     * would undo the verdict here exactly as it did in
     * [CometChatMessageListViewModel.updateMessageOnSuccess] — and registering
     * `onMessageModerated` above would achieve nothing.
     *
     * 🔴 The guard is scoped to **the same message** (matched by muid, then by server id), not to
     * the row. A blanket "never replace a DISAPPROVED lastMessage" would freeze the row on the
     * blocked message forever: this function is the single live-update path for a conversation row,
     * so every later message, unread bump and move-to-top would be dropped too. Only a payload for
     * the very message that is already blocked is refused.
     *
     * 🔴 And only a payload that is *still that message*. A delete or an edit arrives here through
     * `onMessageDeleted` / `onMessageEdited` for the same id, and a deleted payload is stripped
     * (`text` cleared, `deletedAt` set) so it parses as `UNMODERATED` — without the `deletedAt` /
     * `editedAt` exemption the guard would silently refuse a deletion and leave the row showing the
     * blocked message while the message list showed "message deleted". The send ack carries `0` for
     * both, so the ack case it exists for is unaffected.
     *
     * @param conversation The conversation to update
     * @param isActionMessage Whether this is an action message (group action)
     */
    internal fun updateConversation(conversation: Conversation, isActionMessage: Boolean, live: Boolean = true) {
        if (conversation.lastMessage == null) return
        // Offline: a live write after the reconnect sweep was requested is spared by its page.
        if (offlineOn && live) ConversationListOffline.onLiveWrite(this, conversation.conversationId)

        val loggedInUser = getLoggedInUserSafe()
        val lastMessage = conversation.lastMessage
        val isSentByMe = loggedInUser != null &&
            lastMessage?.sender?.uid?.equals(loggedInUser.uid, ignoreCase = true) == true

        var applied = false
        var scrollToTop = false

        // Atomic read-modify-write. The SDK invokes the message listeners straight off the
        // WebSocket thread — these callbacks are NOT wrapped in viewModelScope.launch — so a
        // plain `_conversations.value = ...` here can interleave with the fetch coroutine on
        // Main and leave the same conversationId in the list twice, which crashes the
        // LazyColumn with "Key ... was already used" (ENG-35566). updateAndGet re-runs this
        // block on contention, so it must stay free of side effects; the UI state and
        // scroll event are published afterwards.
        val newList = _conversations.updateAndGet { currentList ->
            val existingIndex = currentList.indexOfFirst {
                it.conversationId == conversation.conversationId
            }

            when {
                existingIndex >= 0 -> {
                    val oldConversation = currentList[existingIndex]

                    // ENG-39226 — a later payload for the SAME message must not downgrade a blocked
                    // row. Returning the list untouched keeps this block side-effect free.
                    val oldLastMessage = oldConversation.lastMessage
                    if (oldLastMessage != null && lastMessage != null &&
                        isSameMessage(oldLastMessage, lastMessage) &&
                        isMessageDisapproved(oldLastMessage) && !isMessageDisapproved(lastMessage) &&
                        lastMessage.deletedAt == 0L && lastMessage.editedAt == 0L
                    ) {
                        applied = false
                        return@updateAndGet currentList
                    }

                    // Clone the conversation to create a new reference for Compose recomposition
                    val updatedConversation = conversation.clone()

                    // Preserve the conversationWith from old conversation (it has more complete data)
                    updatedConversation.conversationWith = oldConversation.conversationWith

                    // CometChatHelper.getConversationFromMessage() never carries per-user pin state,
                    // so keep it from the copy already in the list or the pin would be lost on update.
                    updatedConversation.pinnedAt = oldConversation.pinnedAt
                    updatedConversation.pinnedBy = oldConversation.pinnedBy

                    updatedConversation.unreadMessageCount = when {
                        // Action messages and our own messages never bump the unread count
                        isActionMessage || isSentByMe -> oldConversation.unreadMessageCount
                        // A genuinely new, unread message from someone else does
                        oldConversation.lastMessage?.id != lastMessage?.id && lastMessage?.readAt == 0L ->
                            oldConversation.unreadMessageCount + 1
                        else -> oldConversation.unreadMessageCount
                    }

                    applied = true
                    // A pinned conversation updating in place shouldn't yank the list back to
                    // the top, so only unpinned updates request a scroll.
                    scrollToTop = !updatedConversation.isPinned

                    // Pinned conversations keep their slot (the pinned block mirrors the
                    // backend's pinnedAt ordering); unpinned ones surface at the top of the
                    // unpinned section so new activity never displaces the pinned block.
                    currentList.toMutableList().apply {
                        removeAt(existingIndex)
                        val targetIndex =
                            if (updatedConversation.isPinned) existingIndex else firstUnpinnedIndex(this)
                        add(targetIndex, updatedConversation)
                    }
                }

                // Conversation not in list, check if it should be added based on filter
                isAddToConversationList(conversation) -> {
                    val updatedConversation = conversation.clone()

                    // Set unread count to 1 for new conversations from others (not action messages)
                    if (!isSentByMe && !isActionMessage && lastMessage !is Action) {
                        updatedConversation.unreadMessageCount = 1
                    }

                    applied = true
                    scrollToTop = true

                    // New conversations enter below any pinned block, at the top of the unpinned section.
                    currentList.toMutableList().apply {
                        add(firstUnpinnedIndex(this), updatedConversation)
                    }
                }

                else -> {
                    applied = false
                    currentList
                }
            }
        }

        if (!applied) return

        _uiState.value = UIState.Content(newList)

        // Emit scroll to top event only when the conversation actually surfaced
        if (scrollToTop) {
            viewModelScope.launch {
                _scrollToTopEvent.emit(Unit)
            }
        }
    }
    
    /**
     * Returns `true` when [a] and [b] are the same message.
     *
     * The optimistic row, the moderation stanza and the send ack all carry the same `muid`, while
     * the server `id` lands only with the stanza / ack — so muid is tried first and the id is the
     * fallback for rows that never had a muid (anything fetched or synced). Mirrors the resolution
     * order [CometChatMessageListViewModel.updateMessageOnSuccess] and `updateModeratedMessage` use.
     */
    private fun isSameMessage(a: BaseMessage, b: BaseMessage): Boolean {
        val muidA = a.muid
        val muidB = b.muid
        if (!muidA.isNullOrEmpty() && !muidB.isNullOrEmpty()) return muidA == muidB
        return a.id > 0 && a.id == b.id
    }

    /**
     * Returns `true` when [message] carries a `DISAPPROVED` moderation status.
     * Only [TextMessage] and [MediaMessage] can be moderated — same rule as
     * `CometChatMessageListViewModel.isMessageDisapproved`.
     */
    private fun isMessageDisapproved(message: BaseMessage): Boolean {
        val statusName = when (message) {
            is TextMessage -> message.moderationStatus?.name
            is MediaMessage -> message.moderationStatus?.name
            else -> null
        }
        return statusName?.lowercase() == UIKitConstants.ModerationConstants.DISAPPROVED
    }

    /**
     * Updates group in conversations.
     */
    private fun updateGroupInConversation(group: Group) {
        viewModelScope.launch {
            _conversations.value = _conversations.value.map { conversation ->
                if (conversation.conversationType == UIKitConstants.ConversationType.GROUPS) {
                    val conversationGroup = conversation.conversationWith as? Group
                    if (conversationGroup?.guid == group.guid) {
                        // Clone to create new reference for Compose recomposition
                        conversation.clone().apply { 
                            conversationWith = group 
                        }
                    } else {
                        conversation
                    }
                } else {
                    conversation
                }
            }
        }
    }
    
    // ==================== ListOperations Interface Implementation ====================
    // All methods are open for client override
    
    /**
     * Adds a single conversation to the list.
     * Override to add custom validation or processing.
     *
     * @param item The conversation to add
     */
    override fun addItem(item: Conversation) {
        val accepted = rejectAlreadyPresent(listOf(item))
        if (accepted.isEmpty()) return
        listDelegate.addItem(accepted.first())
        updateUIStateFromList()
    }
    
    /**
     * Adds multiple conversations to the list.
     * Override to add custom validation or processing.
     *
     * @param items The conversations to add
     */
    override fun addItems(items: List<Conversation>) {
        val accepted = rejectAlreadyPresent(items)
        if (accepted.isEmpty()) return
        listDelegate.addItems(accepted)
        updateUIStateFromList()
    }

    /**
     * Drops conversations already present in the list, and duplicates within [items] itself.
     *
     * The list is keyed by `conversationId` in the UI, so appending a conversation that is
     * already rendered crashes Compose with
     * `IllegalArgumentException("Key ... was already used")` (ENG-35566). Conversations without
     * an id cannot be compared and are passed through — the render layer keys those by identity.
     */
    private fun rejectAlreadyPresent(items: List<Conversation>): List<Conversation> {
        val present = _conversations.value.mapNotNull { it.conversationId }.toMutableSet()
        return items.filter { conversation ->
            val id = conversation.conversationId ?: return@filter true
            present.add(id)
        }
    }
    
    /**
     * Removes a conversation from the list.
     * Override to add custom confirmation or logging.
     *
     * @param item The conversation to remove
     * @return true if removed, false if not found
     */
    override fun removeItem(item: Conversation): Boolean {
        val result = listDelegate.removeItem(item)
        if (result) {
            updateUIStateFromList()
        }
        return result
    }
    
    /**
     * Removes a conversation at the specified index.
     * Override to add custom confirmation or logging.
     *
     * @param index The index of the conversation to remove
     * @return The removed conversation, or null if index is out of bounds
     */
    override fun removeItemAt(index: Int): Conversation? {
        val result = listDelegate.removeItemAt(index)
        if (result != null) {
            updateUIStateFromList()
        }
        return result
    }
    
    /**
     * Updates a conversation matching the predicate.
     * Override to add custom validation or transformation.
     *
     * @param item The new conversation to replace with
     * @param predicate Function to find the conversation to update
     * @return true if updated, false if no match found
     */
    override fun updateItem(item: Conversation, predicate: (Conversation) -> Boolean): Boolean {
        val result = listDelegate.updateItem(item, predicate)
        if (result) {
            updateUIStateFromList()
        }
        return result
    }
    
    /**
     * Removes all conversations from the list.
     * Override to add custom confirmation or cleanup.
     */
    override fun clearItems() {
        listDelegate.clearItems()
        updateUIStateFromList()
    }
    
    /**
     * Returns a copy of all conversations in the list.
     *
     * @return Immutable list of all conversations
     */
    override fun getItems(): List<Conversation> {
        return listDelegate.getItems()
    }
    
    /**
     * Returns the conversation at the specified index.
     *
     * @param index The index of the conversation
     * @return The conversation at the index, or null if out of bounds
     */
    override fun getItemAt(index: Int): Conversation? {
        return listDelegate.getItemAt(index)
    }
    
    /**
     * Returns the number of conversations in the list.
     *
     * @return The conversation count
     */
    override fun getItemCount(): Int {
        return listDelegate.getItemCount()
    }
    
    /**
     * Moves a conversation to the top of the list.
     * Override to add custom logic or scroll behavior.
     *
     * @param item The conversation to move to top
     */
    override fun moveItemToTop(item: Conversation) {
        listDelegate.moveItemToTop(item)
        updateUIStateFromList()
    }
    
    /**
     * Performs multiple operations in a single batch, emitting only once.
     * Critical for performance when receiving many updates rapidly.
     * Override to add custom batch processing logic.
     *
     * @param operations Lambda that performs multiple list operations
     */
    override fun batch(operations: ListOperationsBatchScope<Conversation>.() -> Unit) {
        listDelegate.batch(operations)
        updateUIStateFromList()
    }
    
    /**
     * Updates the UI state based on the current list contents.
     */
    private fun updateUIStateFromList() {
        val currentList = _conversations.value
        _uiState.value = if (currentList.isEmpty()) {
            UIState.Empty
        } else {
            UIState.Content(currentList)
        }
    }
    
    /**
     * Routes one hub event (G8, invariant 6). A newer `SessionEnded` clears the list and adopts its
     * gen; any other event with a different gen is dropped. Every accepted event — SessionEnded
     * included, after the clear — goes to W3's [ConversationListOffline.onHubEvent] (G7 sync slice).
     */
    private fun onOfflineHubEvent(event: OfflineHubEvent) {
        if (event is OfflineHubEvent.SessionEnded) {
            if (event.gen <= offlineHubGen) return
            offlineHubGen = event.gen
            clearForOfflineSession()
        } else if (event.gen != offlineHubGen) {
            return
        }
        lastRoutedHubEvent = event
        ConversationListOffline.onHubEvent(this, event)
    }

    /**
     * Clears this list for a session change (logout or a switch to a different uid), before the
     * next frame (G8). Bumps [offlineListGen] so stale reads/events are dropped, and empties the
     * list. The kit never calls `clearOfflineCache`.
     */
    internal fun clearForOfflineSession() {
        offlineListGen++
        hasMoreData = true
        isFetching = false
        _conversations.value = emptyList()
        _uiState.value = UIState.Loading
    }

    override fun onCleared() {
        super.onCleared()
        offlineHubJob?.cancel()
        offlineNoticeJob?.cancel()
        if (enableListeners) {
            removeListeners()
        }
        // Cancel local event listener jobs
        conversationEventsJob?.cancel()
        groupEventsJob?.cancel()
        userEventsJob?.cancel()
        messageEventsJob?.cancel()
        callEventsJob?.cancel()
        
        // Cancel list delegate debounce operations
        listDelegate.cancel()
        
        typingDebounceJob?.cancel()
        soundManager?.release()
        soundManager = null
    }
}

/**
 * Merges an externally supplied conversation onto an existing list entry.
 *
 * See [CometChatConversationsViewModel.updateConversationInList] for the reasoning; this is
 * the merge itself, kept free of the ViewModel so it can be exercised directly.
 *
 * @param existing The entry currently in the list.
 * @param update The conversation carrying the updated properties.
 */
internal fun mergeConversationUpdate(existing: Conversation, update: Conversation): Conversation =
    existing.clone().apply {
        // Counters are taken unconditionally: an Int/Long has no "unset" value to tell apart
        // from a deliberate zero, so the supplied object stays authoritative for them.
        unreadMessageCount = update.unreadMessageCount
        unreadMentionsCount = update.unreadMentionsCount
        lastReadMessageId = update.lastReadMessageId
        latestMessageId = update.latestMessageId
        // Reference fields are taken only when supplied, so a caller refreshing one of them
        // does not blank the others.
        update.conversationWith?.let { conversationWith = it }
        update.lastMessage?.let { lastMessage = it }
        update.tags?.let { tags = it }
        if (update.updatedAt > 0) updatedAt = update.updatedAt
    }
