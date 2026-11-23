package com.cometchat.uikit.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineListSession
import com.cometchat.uikit.core.offline.OfflinePagedMerge
import com.cometchat.uikit.core.state.ChatHistoryUIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing AI Assistant Chat History.
 * Handles message fetching, deletion, real-time event listeners, and UI state management.
 * Uses StateFlow for state management and SharedFlow for one-shot events.
 *
 * This ViewModel makes direct CometChat SDK calls (no repository pattern),
 * matching the reference Java implementation.
 *
 * @param enableListeners Whether to enable CometChat listeners (set to false for testing)
 */
open public class CometChatAIAssistantChatHistoryViewModel(
    private val enableListeners: Boolean = true
) : ViewModel() {

    // --- State Streams ---

    /** Internal mutable list of messages. */
    private val messageArrayList = mutableListOf<BaseMessage>()

    /** Observable list of messages for the UI. */
    private val _messages = MutableStateFlow<List<BaseMessage>>(emptyList())
    public val messages: StateFlow<List<BaseMessage>> = _messages.asStateFlow()

    /** Current UI state (Loading, Empty, Error, Content). */
    private val _uiState = MutableStateFlow<ChatHistoryUIState>(ChatHistoryUIState.Empty)
    public val uiState: StateFlow<ChatHistoryUIState> = _uiState.asStateFlow()

    /** Delete operation state. */
    private val _deleteState = MutableSharedFlow<UIKitConstants.DeleteState>(extraBufferCapacity = 3)
    public val deleteState: SharedFlow<UIKitConstants.DeleteState> = _deleteState.asSharedFlow()

    /** Emits the position of a removed message for adapter notification. */
    private val _removeMessagePosition = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    public val removeMessagePosition: SharedFlow<Int> = _removeMessagePosition.asSharedFlow()

    /** Whether more messages are available for pagination. */
    private val _hasMore = MutableStateFlow(true)
    public val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    /** Whether a fetch operation is currently in progress. */
    private val _isInProgress = MutableStateFlow(false)
    public val isInProgress: StateFlow<Boolean> = _isInProgress.asStateFlow()

    /** Emits the count of newly prepended messages for adapter range notification. */
    private val _messagesRangeChanged = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    public val messagesRangeChanged: SharedFlow<Int> = _messagesRangeChanged.asSharedFlow()

    // --- Internal State ---

    private var messagesRequest: MessagesRequest? = null
    private var messageRequestBuilder: MessagesRequest.MessagesRequestBuilder? = null
    private var user: User? = null
    private var group: Group? = null
    private var listenerTag: String? = null
    private var messageEventsJob: Job? = null

    /**
     * Bumped only by a session end or cache reset (G8), so a read already in flight drops its
     * result. Never moves with offline off, where reads behave as today.
     */
    private var historyGen = 0L

    // G8: session end and cache reset (ENG-41568). Started only with offline on.
    private val offlineSession = OfflineListSession(
        onSessionEnded = { clearForOfflineSession() },
        onCacheReset = { onOfflineCacheReset() }
    )

    init {
        if (enableListeners && OfflineGate.isOn()) offlineSession.start(viewModelScope)
    }

    // --- Public Methods ---

    /**
     * Sets the User context and triggers the first message fetch.
     * Builds a MessagesRequest filtered by the User UID with limit=20,
     * categories=[MESSAGE], types=[TEXT], hideDeletedMessages=true, hideReplies=true.
     *
     * @param user The User whose chat history to display
     */
    public fun setUser(user: User) {
        this.user = user
        initializeMessagesRequest()
        fetchMessages()
    }

    /**
     * Sets the Group context without triggering a fetch.
     * Builds a MessagesRequest filtered by the Group GUID with the same filters.
     *
     * @param group The Group whose chat history to display
     */
    public fun setGroup(group: Group) {
        this.group = group
        initializeMessagesRequest()
    }

    /**
     * Fetches the next page of messages using MessagesRequest.fetchPrevious().
     * Guards against concurrent fetches via [_isInProgress].
     * Reverses results and prepends to the internal list.
     * Sets hasMore=false on empty response.
     * Emits Loading state on first fetch when list is empty.
     */
    public fun fetchMessages() {
        if (messagesRequest == null) return
        if (!_hasMore.value) return
        if (_isInProgress.value) return

        _isInProgress.value = true

        if (messageArrayList.isEmpty()) {
            _uiState.value = ChatHistoryUIState.Loading
        }

        // One SDK 5.1.0 two-phase read. Offline off: no cached phase, so this is today's flow.
        // Offline on: the cached page is the first paint and the server page replaces it in place,
        // deduped by id (OfflinePagedMerge); only the server phase moves hasMore, so an empty
        // cached page never ends the list. [_isInProgress] serialises reads; the request is built once
        // (initializeMessagesRequest) and rebuilt only on a session end or cache reset (G8), which
        // also bumps [historyGen] so the read in flight then drops its result.
        val offlineOn = OfflineGate.isOn()
        val gen = historyGen
        var window: Set<Any> = emptySet()
        messagesRequest?.fetchPrevious(object : CometChat.MessageFetchListener() {
            override fun onCachedMessages(messages: List<BaseMessage>) {
                if (!offlineOn || gen != historyGen) return
                val merged = OfflinePagedMerge.applyCached(messageArrayList.toList(), messages.reversed(), ::messageKey)
                window = merged.window
                if (merged.added > 0) showMerged(merged.rows, merged.added)
            }

            override fun onServerMessages(messages: List<BaseMessage>) {
                if (gen != historyGen) return
                val hasResults = messages.isNotEmpty()
                _hasMore.value = hasResults

                if (offlineOn) {
                    val merged = OfflinePagedMerge.applyServer(
                        messageArrayList.toList(), messages.reversed(), window, ::messageKey
                    )
                    showMerged(merged.rows, merged.added)
                } else if (hasResults) {
                    val reversed = messages.reversed()
                    messageArrayList.addAll(reversed)
                    _messages.value = ArrayList(messageArrayList)
                    _messagesRangeChanged.tryEmit(reversed.size)
                }

                _isInProgress.value = false

                if (messageArrayList.isEmpty()) {
                    _uiState.value = ChatHistoryUIState.Empty
                } else {
                    _uiState.value = ChatHistoryUIState.Content(ArrayList(messageArrayList))
                }
            }

            override fun onError(exception: CometChatException) {
                if (gen != historyGen) return
                _isInProgress.value = false
                if (!offlineOn) {
                    _uiState.value = ChatHistoryUIState.Error(exception)
                    return
                }
                // Offline on: never an error screen over rows already shown. With nothing shown,
                // AI history is online only (D32-R2c), so any error, not-connected included, is
                // today's error state.
                _uiState.value = if (messageArrayList.isNotEmpty()) {
                    ChatHistoryUIState.Content(ArrayList(messageArrayList))
                } else {
                    ChatHistoryUIState.Error(exception)
                }
            }
        })
    }

    /** Replaces the list with [rows] (offline on); emits the range only for a net growth. */
    private fun showMerged(rows: List<BaseMessage>, added: Int) {
        messageArrayList.clear()
        messageArrayList.addAll(rows)
        _messages.value = ArrayList(messageArrayList)
        if (added > 0) _messagesRangeChanged.tryEmit(added)
        if (messageArrayList.isNotEmpty()) {
            _uiState.value = ChatHistoryUIState.Content(ArrayList(messageArrayList))
        }
    }

    private fun messageKey(message: BaseMessage): Any = message.id

    /**
     * Deletes a chat history message via the CometChat SDK.
     * Emits INITIATED_DELETE, then SUCCESS_DELETE or FAILURE_DELETE.
     * On success, removes the message from the local list and fires a UIKit
     * MessageDeleted event for inter-component communication.
     *
     * @param baseMessage The message to delete
     */
    public fun deleteChatHistoryItem(baseMessage: BaseMessage) {
        _deleteState.tryEmit(UIKitConstants.DeleteState.INITIATED_DELETE)

        CometChat.deleteMessage(baseMessage.id, object : CometChat.CallbackListener<BaseMessage>() {
            override fun onSuccess(deletedMessage: BaseMessage) {
                _deleteState.tryEmit(UIKitConstants.DeleteState.SUCCESS_DELETE)
                // Remove the original message from the local list immediately
                remove(baseMessage)
                // Emit UIKit event for inter-component communication
                // Equivalent to Java's CometChatUIKitHelper.onMessageDeleted()
                CometChatEvents.emitMessageEvent(
                    CometChatMessageEvent.MessageDeleted(deletedMessage)
                )
            }

            override fun onError(exception: CometChatException) {
                _deleteState.tryEmit(UIKitConstants.DeleteState.FAILURE_DELETE)
            }
        })
    }

    /**
     * Removes a message from the internal list and emits the removed position.
     * Matches by message ID for reliable identification regardless of object reference.
     * Transitions to Empty state if the list becomes empty after removal.
     *
     * @param baseMessage The message to remove
     */
    public fun remove(baseMessage: BaseMessage) {
        val oldIndex = messageArrayList.indexOfFirst { it.id == baseMessage.id }
        if (oldIndex == -1) return

        messageArrayList.removeAt(oldIndex)
        _messages.value = ArrayList(messageArrayList)
        _removeMessagePosition.tryEmit(oldIndex)

        if (messageArrayList.isEmpty()) {
            _uiState.value = ChatHistoryUIState.Empty
        } else {
            _uiState.value = ChatHistoryUIState.Content(ArrayList(messageArrayList))
        }
    }

    /**
     * Registers real-time event listeners.
     * - SDK MessageListener for server-pushed deletion events
     * - UIKit CometChatEvents.messageEvents for local inter-component deletion events
     */
    public fun addListeners() {
        if (!enableListeners) return

        listenerTag = "ChatHistory_${System.currentTimeMillis()}"

        // SDK Listener — receives server-pushed real-time events
        CometChat.addMessageListener(listenerTag!!, object : CometChat.MessageListener() {
            override fun onMessageDeleted(message: BaseMessage) {
                remove(message)
            }
        })

        // UIKit Local Events — receives UI-initiated events from other components
        messageEventsJob = viewModelScope.launch {
            CometChatEvents.messageEvents.collect { event ->
                when (event) {
                    is CometChatMessageEvent.MessageDeleted -> remove(event.message)
                    else -> { /* ignore other events */ }
                }
            }
        }
    }

    /**
     * Unregisters all event listeners and cancels the message events job.
     */
    public fun removeListeners() {
        listenerTag?.let { CometChat.removeMessageListener(it) }
        messageEventsJob?.cancel()
        messageEventsJob = null
    }

    override fun onCleared() {
        super.onCleared()
        removeListeners()
    }

    // --- Private Methods ---

    /**
     * Clears the history for a session change (logout or a switch to a different uid), before the
     * next frame (G8). Bumps [historyGen], so a read already in flight drops its result, empties the
     * rows and resets paging: the request is rebuilt from the same builder, so the next fetch starts
     * from page one. Offline on only (the hub drives it).
     */
    private fun clearForOfflineSession() {
        historyGen++
        messageArrayList.clear()
        _messages.value = emptyList()
        _hasMore.value = true
        _isInProgress.value = false
        if (messagesRequest != null) messagesRequest = messageRequestBuilder?.build()
        _uiState.value = ChatHistoryUIState.Loading
    }

    /**
     * The offline cache was reset (G8): clear as for a session end, then read the first page once
     * if the history had a user or group. A failure over cached rows keeps them and reports nothing
     * (D84 rule 5).
     */
    private fun onOfflineCacheReset() {
        clearForOfflineSession()
        fetchMessages()
    }

    /**
     * Initializes the MessagesRequest with the appropriate UID or GUID filter.
     * Only builds the request once (guards against re-initialization).
     */
    private fun initializeMessagesRequest() {
        if (messageRequestBuilder == null) {
            messageRequestBuilder = MessagesRequest.MessagesRequestBuilder()
                .setLimit(20)
                .setCategories(listOf(UIKitConstants.MessageCategory.MESSAGE))
                .setTypes(listOf(UIKitConstants.MessageType.TEXT))
                .hideDeletedMessages(true)
                .hideReplies(true)

            if (user != null) {
                messagesRequest = messageRequestBuilder!!.setUID(user!!.uid).build()
            } else if (group != null) {
                messagesRequest = messageRequestBuilder!!.setGUID(group!!.guid).build()
            }
        }
    }
}
