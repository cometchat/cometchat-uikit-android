package com.cometchat.uikit.core.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.MessageStatus as SdkMessageStatus
import com.cometchat.chat.models.MessageReceipt
import com.cometchat.chat.models.ReactionEvent
import com.cometchat.chat.models.TextMessage
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.events.MessageStatus
import com.cometchat.uikit.core.offline.MessageSendState
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineHub
import com.cometchat.uikit.core.offline.OfflineHubEvent
import com.cometchat.uikit.core.utils.PinSaveUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for CometChatThreadHeader component.
 *
 * This ViewModel is located in chatuikit-core to be shared between
 * chatuikit-kotlin (View-based) and chatuikit-jetpack (Compose-based) modules.
 *
 * Manages state and real-time updates for the thread header using StateFlow,
 * including:
 * - Parent message state
 * - Reply count tracking
 * - Real-time message events (sent, received, updated)
 *
 * ## Key Responsibilities
 * 1. Hold parent message state
 * 2. Track reply count
 * 3. Listen for SDK message events
 * 4. Listen for UIKit local events
 * 5. Update parent message on edits/deletes
 * 6. Handle reaction updates
 *
 * ## StateFlow Observables
 * - [parentMessageListStateFlow]: Parent message list (single-item list)
 * - [replyCountStateFlow]: Current reply count
 * - [sentMessage]: Sent message events (SharedFlow for one-time events)
 * - [receiveMessage]: Received message events (SharedFlow for one-time events)
 * - [updateParentMessage]: Parent message updates (SharedFlow for one-time events)
 *
 * ## Usage (Compose)
 * ```kotlin
 * @Composable
 * fun ThreadHeaderScreen(parentMessage: BaseMessage) {
 *     val viewModel: CometChatThreadHeaderViewModel = viewModel()
 *
 *     LaunchedEffect(parentMessage) {
 *         viewModel.setParentMessage(parentMessage)
 *     }
 *
 *     val replyCount by viewModel.replyCountStateFlow.collectAsState()
 *     val messageList by viewModel.parentMessageListStateFlow.collectAsState()
 * }
 * ```
 *
 * ## Usage (View-based with LiveData adapter)
 * ```kotlin
 * val viewModel: CometChatThreadHeaderViewModel by viewModels()
 * viewModel.setParentMessage(parentMessage)
 *
 * // Observe StateFlow as LiveData using asLiveData() extension
 * viewModel.replyCountStateFlow.asLiveData().observe(viewLifecycleOwner) { count ->
 *     updateReplyCountUI(count)
 * }
 * ```
 */
open public class CometChatThreadHeaderViewModel(
    private val enableListeners: Boolean = true
) : ViewModel() {

    public companion object {
        private val TAG = CometChatThreadHeaderViewModel::class.java.simpleName
    }

    // ==================== State ====================

    /**
     * The parent message of the thread.
     * This is the original message that started the thread conversation.
     */
    private var _parentMessage: BaseMessage? = null

    /**
     * Internal list holding the parent message.
     * Contains a single item (the parent message) when set.
     */
    protected val messageList: MutableList<BaseMessage> = mutableListOf()

    /**
     * Flag to control reaction visibility.
     * When true, reaction updates are ignored.
     */
    public var hideReaction: Boolean = false

    /**
     * Unique listener ID for CometChat SDK message events.
     */
    protected var listenerId: String = ""

    /**
     * Job for UIKit message events subscription.
     */
    private var messageEventsJob: Job? = null

    // ==================== StateFlow ====================

    /**
     * StateFlow for the parent message list.
     * Contains a single-item list with the parent message.
     */
    private val _parentMessageListStateFlow = MutableStateFlow<List<BaseMessage>>(emptyList())
    public val parentMessageListStateFlow: StateFlow<List<BaseMessage>> = _parentMessageListStateFlow.asStateFlow()

    /**
     * StateFlow for the current reply count.
     */
    private val _replyCountStateFlow = MutableStateFlow(0)
    public val replyCountStateFlow: StateFlow<Int> = _replyCountStateFlow.asStateFlow()

    /**
     * SharedFlow for sent message events.
     */
    private val _sentMessage = MutableSharedFlow<BaseMessage>()
    public val sentMessage: SharedFlow<BaseMessage> = _sentMessage.asSharedFlow()

    /**
     * SharedFlow for received message events.
     */
    private val _receiveMessage = MutableSharedFlow<BaseMessage>()
    public val receiveMessage: SharedFlow<BaseMessage> = _receiveMessage.asSharedFlow()

    /**
     * SharedFlow for parent message updates.
     */
    private val _updateParentMessage = MutableSharedFlow<BaseMessage>()
    public val updateParentMessage: SharedFlow<BaseMessage> = _updateParentMessage.asSharedFlow()

    // ==================== LiveData (for View-based UI) ====================

    /**
     * LiveData for the parent message list.
     * Converts the StateFlow to LiveData for View-based UI compatibility.
     */
    public val parentMessageListLiveData: LiveData<List<BaseMessage>> = _parentMessageListStateFlow.asLiveData()

    /**
     * LiveData for the current reply count.
     * Converts the StateFlow to LiveData for View-based UI compatibility.
     */
    public val replyCount: LiveData<Int> = _replyCountStateFlow.asLiveData()

    // ==================== Public Methods ====================

    /**
     * Sets the parent message for the thread header.
     *
     * @param parentMessage The parent message to display. If null, no action is taken.
     */
    public fun setParentMessage(parentMessage: BaseMessage?) {
        if (parentMessage != null) {
            // A new snapshot (same thread or another): its stored count already carries every delta
            // the SDK applied before it was read, so nothing seen on the old one carries over.
            clearOfflineReplyDeltas()
            this._parentMessage = parentMessage
            _replyCountStateFlow.value = parentMessage.replyCount
            messageList.clear()
            messageList.add(parentMessage)
            _parentMessageListStateFlow.value = messageList.toList()
        }
    }

    /**
     * Gets the current parent message.
     *
     * @return The parent message, or null if not set.
     */
    public fun getCurrentParentMessage(): BaseMessage? = _parentMessage

    // ==================== Reply Count Methods ====================

    /**
     * Increments the reply count by 1.
     */
    protected fun incrementParentMessageReplyCount() {
        _replyCountStateFlow.value = _replyCountStateFlow.value + 1
    }

    // ==================== Offline reply overlay (BEH-MUT-10) ====================

    /**
     * The offline mode captured when this header was created. Every overlay branch below is behind
     * it, so with offline off the header counts exactly as before.
     */
    internal val offlineOn: Boolean = OfflineGate.isOn()

    /**
     * Own replies to the parent whose send events this header has seen since its parent snapshot,
     * by localMessageId. Only a reply first seen as queued adds to the count.
     */
    private val seenOfflineReplies: HashSet<String> = HashSet()

    /** Own replies already taken back after a device delete, so a repeat event takes nothing. */
    private val removedOfflineReplies: HashSet<String> = HashSet()

    /** Collects the offline hub's send events; only started when [offlineOn]. */
    private var offlineHubJob: Job? = null

    /** The hub session generation this header counts in; an event from another one is dropped. */
    private var offlineHubGen: Long = -1L

    /**
     * The displayed count is the parent's stored count, and the SDK keeps the user's own unsent
     * replies in that stored count (BEH-MUT-10): +1 when a reply enters the outbox, −1 when the
     * user deletes it from the device, nothing when it sends. A parent read after the reply was
     * queued already includes it. So the header applies only the deltas the SDK applies after this
     * header's snapshot was taken:
     * - +1 when a reply is first seen as queued (SENDING with an unfailed message; a Retry reports
     *   SENDING with the row's FAILED copy and the SDK adds nothing for it);
     * - −1 once when the kit's Delete removes it from the device ([OfflineHubEvent.ThreadReplyDeleted]
     *   with `discarded`), whether or not this header counted it;
     * - nothing on SENT, on an eviction (QUEUE_OVERFLOW) or a purge (DISCARDED): the SDK leaves the
     *   stored count as it is and the reconnect sweep heals it.
     */
    internal fun onOfflineHubEvent(event: OfflineHubEvent) {
        if (event is OfflineHubEvent.SessionEnded) {
            if (event.gen > offlineHubGen) {
                offlineHubGen = event.gen
                clearOfflineReplyDeltas()
            }
            return
        }
        if (event.gen != offlineHubGen) return
        when (event) {
            is OfflineHubEvent.SendState -> {
                if (!isReplyToParent(event.message)) return
                val localId = event.localMessageId.ifEmpty { event.message.localMessageId.orEmpty() }
                if (localId.isEmpty()) return
                val firstSighting = seenOfflineReplies.add(localId)
                if (firstSighting && event.state == MessageSendState.SENDING &&
                    event.message.status != SdkMessageStatus.FAILED
                ) {
                    incrementParentMessageReplyCount()
                }
            }
            is OfflineHubEvent.ThreadReplyDeleted -> {
                if (!event.discarded || !isReplyToParent(event.message)) return
                if (event.localMessageId.isEmpty() || !removedOfflineReplies.add(event.localMessageId)) return
                seenOfflineReplies.add(event.localMessageId)
                _replyCountStateFlow.value = maxOf(0, _replyCountStateFlow.value - 1)
            }
            else -> Unit
        }
    }

    /**
     * The legacy `MessageSent(SUCCESS)` for a reply on the offline path. Returns true when the
     * overlay owns it: the SDK counted the reply when it was queued, so the ack adds nothing.
     * False sends it down today's path: offline turned off since this header was created means the
     * send took today's path, and a reply without a localMessageId never entered the outbox.
     */
    private fun handleOfflineReplySent(message: BaseMessage): Boolean {
        if (!offlineOn || !OfflineGate.isOn()) return false
        val localId = message.localMessageId
        if (localId.isNullOrEmpty()) return false
        seenOfflineReplies.add(localId)
        return true
    }

    private fun isReplyToParent(message: BaseMessage): Boolean {
        val parent = _parentMessage ?: return false
        return message.parentMessageId > 0 && message.parentMessageId == parent.id
    }

    private fun clearOfflineReplyDeltas() {
        seenOfflineReplies.clear()
        removedOfflineReplies.clear()
    }

    private fun startOfflineHub() {
        if (!offlineOn) return
        offlineHubJob?.cancel()
        val gen = OfflineHub.gen
        if (gen != offlineHubGen) {
            offlineHubGen = gen
            clearOfflineReplyDeltas()
        }
        offlineHubJob = viewModelScope.launch {
            OfflineHub.events.collect { event -> onOfflineHubEvent(event) }
        }
    }

    // ==================== Update Methods ====================

    /**
     * Updates the parent message in the list.
     *
     * @param baseMessage The updated message. If null, no action is taken.
     */
    public fun updateParentMessageInList(baseMessage: BaseMessage?) {
        val parent = _parentMessage
        if (baseMessage != null && parent != null) {
            var emitted = baseMessage
            // Thread message update - refresh the parent message display
            if (baseMessage.parentMessageId > 0 && baseMessage.parentMessageId == parent.id) {
                if (messageList.isNotEmpty()) {
                    messageList[0] = parent
                    _parentMessageListStateFlow.value = messageList.toList()
                }
            }
            // Parent message itself updated. An edit/moderation payload carries the new content
            // but not the viewer's pin/save state — carry it over from the copy this header holds,
            // and adopt the result as the current parent so later carries start from the latest
            // content rather than the one the screen was opened with.
            if (baseMessage.id == parent.id) {
                val reconciled = PinSaveUtils.carryPinSaveForward(parent, baseMessage)
                _parentMessage = reconciled
                emitted = reconciled
                if (messageList.isNotEmpty()) {
                    messageList[0] = reconciled
                    _parentMessageListStateFlow.value = messageList.toList()
                }
            }
            viewModelScope.launch {
                _receiveMessage.emit(emitted)
            }
        }
    }

    /**
     * Applies a pin/save change to the parent this header shows.
     *
     * The message list only reconciles the rows it has loaded, and the thread's parent is not one of
     * them — it lives here. Without this, pinning/saving the parent from inside the thread (or from
     * another device) left the header's indicator stale until the screen was reopened.
     *
     * Same contract as the message list's `applyPinSaveEcho`: the delivered [echo] may be partial,
     * so only the pin/save attributes are taken from it and applied to a [BaseMessage.clone] of the
     * held parent — never to the held instance, so the StateFlow sees a new value.
     */
    private fun applyParentPinSaveChange(echo: BaseMessage, applyTo: (BaseMessage) -> Unit) {
        val parent = _parentMessage ?: return
        if (echo.id != parent.id) return
        val updated = parent.clone().apply(applyTo)
        _parentMessage = updated
        if (messageList.isNotEmpty()) {
            messageList[0] = updated
            _parentMessageListStateFlow.value = messageList.toList()
        }
    }

    /**
     * A pin/save kit event (D-327, G11, BEH-FLAG-04). SDK 5.1.2 sends no own echo, so this is how a
     * pin/save of the parent from a message list, or an unpin/unsave from the pinned or saved panel,
     * reaches the header. A no-op when the parent already shows the state; the header never emits.
     */
    private fun handleParentPinSaveKitEvent(message: BaseMessage, isPin: Boolean, state: Boolean) {
        val parent = _parentMessage ?: return
        if (message.id != parent.id) return
        val current = if (isPin) parent.isPinned else parent.isSaved
        // A pin by someone else onto a pinned parent changes who pinned it — not a no-op.
        val pinnerChanged = isPin && state && message.pinnedBy != null && message.pinnedBy != parent.pinnedBy
        if (current == state && !pinnerChanged) return
        applyParentPinSaveChange(message) { updated ->
            when {
                isPin && state -> {
                    updated.pinnedAt = if (message.pinnedAt > 0) message.pinnedAt else System.currentTimeMillis() / 1000
                    message.pinnedBy?.let { updated.pinnedBy = it }
                }
                isPin -> { updated.pinnedAt = 0; updated.pinnedBy = null }
                state -> updated.savedAt = if (message.savedAt > 0) message.savedAt else System.currentTimeMillis() / 1000
                else -> updated.savedAt = 0
            }
        }
    }

    // ==================== Protected Emitters ====================

    /**
     * Emits a sent message event.
     */
    protected fun emitSentMessage(message: BaseMessage) {
        viewModelScope.launch {
            _sentMessage.emit(message)
        }
    }

    /**
     * Emits a receive message event.
     */
    protected fun emitReceiveMessage(message: BaseMessage) {
        viewModelScope.launch {
            _receiveMessage.emit(message)
        }
    }

    /**
     * Emits an update parent message event.
     */
    protected fun emitUpdateParentMessage(message: BaseMessage) {
        viewModelScope.launch {
            _updateParentMessage.emit(message)
        }
    }

    // ==================== Listener Methods ====================

    /**
     * Adds SDK message listener for real-time server-pushed events.
     */
    open public fun addListener() {
        if (!enableListeners) return
        
        // Remove previous listener to prevent duplicate event handling
        if (listenerId.isNotEmpty()) {
            CometChat.removeMessageListener(listenerId)
        }
        
        listenerId = "ThreadHeader_${System.currentTimeMillis()}"

        CometChat.addMessageListener(listenerId, object : CometChat.MessageListener() {
            override fun onTextMessageReceived(message: TextMessage) {
                handleMessageReceived(message)
            }

            override fun onMediaMessageReceived(message: MediaMessage) {
                handleMessageReceived(message)
            }

            override fun onCustomMessageReceived(message: CustomMessage) {
                handleMessageReceived(message)
            }

            override fun onMessageEdited(message: BaseMessage) {
                handleMessageEdited(message)
            }

            override fun onMessageDeleted(message: BaseMessage) {
                handleMessageDeleted(message)
            }

            override fun onMessagesDelivered(messageReceipt: MessageReceipt) {
                handleMessageReceipt(messageReceipt)
            }

            override fun onMessagesRead(messageReceipt: MessageReceipt) {
                handleMessageReceipt(messageReceipt)
            }

            override fun onMessagesDeliveredToAll(messageReceipt: MessageReceipt) {
                handleMessageReceipt(messageReceipt)
            }

            override fun onMessagesReadByAll(messageReceipt: MessageReceipt) {
                handleMessageReceipt(messageReceipt)
            }

            override fun onMessageReactionAdded(reactionEvent: ReactionEvent) {
                handleReactionAdded(reactionEvent)
            }

            override fun onMessageReactionRemoved(reactionEvent: ReactionEvent) {
                handleReactionRemoved(reactionEvent)
            }

            // Pin/save state of the parent. On the acting device these arrive as the SDK's
            // self-echo off the REST response; from another device as a realtime action. The
            // event is the assertion — a "pinned"/"saved" echo without a timestamp still counts.
            override fun onMessagePinned(message: BaseMessage) {
                applyParentPinSaveChange(message) { updated ->
                    updated.pinnedAt = if (message.pinnedAt > 0) message.pinnedAt
                    else System.currentTimeMillis() / 1000
                    message.pinnedBy?.let { updated.pinnedBy = it }
                }
            }

            override fun onMessageUnpinned(message: BaseMessage) {
                applyParentPinSaveChange(message) { updated ->
                    updated.pinnedAt = 0
                    updated.pinnedBy = null
                }
            }

            override fun onMessageSaved(message: BaseMessage) {
                applyParentPinSaveChange(message) { updated ->
                    updated.savedAt = if (message.savedAt > 0) message.savedAt
                    else System.currentTimeMillis() / 1000
                }
            }

            override fun onMessageUnsaved(message: BaseMessage) {
                applyParentPinSaveChange(message) { updated -> updated.savedAt = 0 }
            }
        })
    }

    /**
     * Adds UIKit local event listeners for inter-component communication.
     */
    open public fun addLocalEventListeners() {
        if (!enableListeners) return
        
        // Cancel previous subscription to prevent duplicate event handling
        messageEventsJob?.cancel()
        
        messageEventsJob = viewModelScope.launch {
            CometChatEvents.messageEvents.collect { event ->
                when (event) {
                    is CometChatMessageEvent.MessageSent -> {
                        if (event.status == MessageStatus.SUCCESS) {
                            handleUIMessageSent(event.message)
                        }
                    }
                    is CometChatMessageEvent.MessageEdited -> {
                        if (event.status == MessageStatus.SUCCESS) {
                            handleMessageEdited(event.message)
                        }
                    }
                    is CometChatMessageEvent.MessageDeleted -> {
                        handleMessageDeleted(event.message)
                    }
                    is CometChatMessageEvent.MessagePinned -> handleParentPinSaveKitEvent(event.message, isPin = true, state = true)
                    is CometChatMessageEvent.MessageUnpinned -> handleParentPinSaveKitEvent(event.message, isPin = true, state = false)
                    is CometChatMessageEvent.MessageSaved -> handleParentPinSaveKitEvent(event.message, isPin = false, state = true)
                    is CometChatMessageEvent.MessageUnsaved -> handleParentPinSaveKitEvent(event.message, isPin = false, state = false)
                    else -> { /* Ignore other message events */ }
                }
            }
        }
        startOfflineHub()
    }

    /**
     * Removes the SDK message listener and UIKit event subscriptions.
     */
    open public fun removeListener() {
        if (listenerId.isNotEmpty()) {
            CometChat.removeMessageListener(listenerId)
        }
        messageEventsJob?.cancel()
        messageEventsJob = null
        offlineHubJob?.cancel()
        offlineHubJob = null
    }

    // ==================== Event Handlers ====================

    /**
     * Handles incoming message received from SDK listener.
     * Skips messages sent by the current user to prevent double-counting.
     */
    private fun handleMessageReceived(message: BaseMessage) {
        _parentMessage?.let { parent ->
            if (message.parentMessageId > 0 && message.parentMessageId == parent.id) {
                // Skip messages sent by the current user - handled by handleUIMessageSent
                val loggedInUser = CometChat.getLoggedInUser()
                if (loggedInUser != null && message.sender?.uid == loggedInUser.uid) {
                    return@let
                }

                incrementParentMessageReplyCount()
                updateParentMessageInList(message)
            }
        }
    }

    /**
     * Handles UI-initiated message sent event.
     */
    private fun handleUIMessageSent(message: BaseMessage) {
        _parentMessage?.let { parent ->
            if (message.parentMessageId > 0 && message.parentMessageId == parent.id) {
                if (handleOfflineReplySent(message)) return@let
                incrementParentMessageReplyCount()
            }
        }
    }

    /**
     * Handles message edited event.
     */
    private fun handleMessageEdited(message: BaseMessage) {
        // An edit arrives flagless and would otherwise un-follow the thread this header is showing;
        // an edit that leaves the user @-mentioned in a reply subscribes them. Reconcile either way
        // before the edited copy is shown.
        com.cometchat.uikit.core.utils.CometChatThreadSubscription.applyEditedMessageSubscription(
            editedMessage = message,
            heldMessage = _parentMessage?.takeIf { it.id == message.id },
            parentMessage = _parentMessage
        )
        _parentMessage?.let { parent ->
            if (message.id == parent.id) {
                updateParentMessageInList(message)
            }
        }
    }

    /**
     * Handles message deleted event.
     */
    private fun handleMessageDeleted(message: BaseMessage) {
        _parentMessage?.let { parent ->
            if (message.id == parent.id) {
                updateParentMessageInList(message)
            }
        }
    }

    /**
     * Handles message receipt events.
     */
    private fun handleMessageReceipt(receipt: MessageReceipt) {
        _parentMessage?.let { parent ->
            if (receipt.messageId == parent.id) {
                if (receipt.deliveredAt > 0) {
                    parent.deliveredAt = receipt.deliveredAt
                }
                if (receipt.readAt > 0) {
                    parent.readAt = receipt.readAt
                }
                updateParentMessageInList(parent)
            }
        }
    }

    /**
     * Handles reaction added event.
     */
    private fun handleReactionAdded(reactionEvent: ReactionEvent) {
        if (hideReaction) return

        _parentMessage?.let { parent ->
            val reactionMessageId = reactionEvent.reaction?.messageId
            if (reactionMessageId != null && reactionMessageId == parent.id) {
                updateParentMessageInList(parent)
            }
        }
    }

    /**
     * Handles reaction removed event.
     */
    private fun handleReactionRemoved(reactionEvent: ReactionEvent) {
        if (hideReaction) return

        _parentMessage?.let { parent ->
            val reactionMessageId = reactionEvent.reaction?.messageId
            if (reactionMessageId != null && reactionMessageId == parent.id) {
                updateParentMessageInList(parent)
            }
        }
    }

    // ==================== Lifecycle ====================

    override fun onCleared() {
        super.onCleared()
        removeListener()
        messageList.clear()
        _parentMessage = null
    }
}
