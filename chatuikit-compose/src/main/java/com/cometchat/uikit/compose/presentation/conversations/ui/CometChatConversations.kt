package com.cometchat.uikit.compose.presentation.conversations.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.compose.R
import com.cometchat.uikit.core.R as CoreR
import com.cometchat.uikit.core.offline.OfflineNotice
import com.cometchat.uikit.core.state.DeleteState
import com.cometchat.uikit.core.state.UIState
import com.cometchat.uikit.compose.presentation.conversations.style.CometChatConversationsStyle
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.compose.presentation.conversations.utils.TypingIndicator
import com.cometchat.uikit.compose.presentation.shared.defaultstates.CometChatEmptyState
import com.cometchat.uikit.compose.presentation.shared.defaultstates.CometChatErrorState
import com.cometchat.uikit.compose.presentation.shared.defaultstates.CometChatLoadingState
import com.cometchat.uikit.compose.presentation.shared.dialog.CometChatDialog
import com.cometchat.uikit.compose.presentation.shared.dialog.CometChatDialogStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.presentation.shared.formatters.CometChatMentionsFormatter
import com.cometchat.uikit.compose.presentation.shared.formatters.CometChatTextFormatter
import com.cometchat.uikit.compose.presentation.shared.interfaces.DateTimeFormatterCallback
import com.cometchat.uikit.compose.presentation.shared.searchbox.CometChatSearchBox
import com.cometchat.uikit.compose.presentation.shared.searchbox.CometChatSearchBoxStyle
import com.cometchat.uikit.compose.presentation.shared.toolbar.CometChatToolbar
import com.cometchat.uikit.compose.presentation.shared.toolbar.CometChatToolbarStyle
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import com.cometchat.uikit.core.factory.CometChatConversationsViewModelFactory
import com.cometchat.uikit.compose.shared.views.popupmenu.MenuItem
import java.text.SimpleDateFormat

/**
 * CometChatConversations displays a list of conversations with support for
 * real-time updates, selection modes, custom views, and full styling customization.
 *
 * @param modifier Modifier applied to the parent container
 * @param viewModel The ViewModel managing conversation state (optional, creates default if not provided)
 * @param style Style configuration for the component
 * @param title Toolbar title text
 * @param hideToolbar Whether to hide the entire toolbar
 * @param hideBackIcon Whether to hide the back navigation icon
 * @param overflowMenu Optional custom overflow menu composable
 * @param hideSearchBox Whether to hide the search box
 * @param searchPlaceholderText Placeholder text for search box
 * @param selectionMode Selection mode (NONE, SINGLE, MULTIPLE)
 * @param hideUserStatus Whether to hide user online/offline indicators
 * @param hideGroupType Whether to hide group type indicators
 * @param hideReceipts Whether to hide message read receipts
 * @param hideSeparator Whether to hide item separators
 * @param hidePinOption Whether to hide the pin/unpin conversation option
 * @param hideDeleteOption Whether to hide delete option in menu
 * @param hideLoadingState Whether to hide loading state
 * @param hideEmptyState Whether to hide empty state
 * @param hideErrorState Whether to hide error state
 * @param hideOfflineNotices Whether to hide the one-line offline notices (default false)
 * @param disableSoundForMessages Whether to disable notification sounds
 * @param customSoundForMessages Custom sound resource ID
 * @param dateTimeFormatter Custom date/time formatter callback
 * @param textFormatters List of text formatters for message preview (defaults to CometChatMentionsFormatter)
 * @param loadingView Custom loading state composable
 * @param emptyView Custom empty state composable
 * @param errorView Custom error state composable with retry callback
 * @param itemView Custom item composable replacing entire item
 * @param leadingView Custom leading section composable
 * @param titleView Custom title section composable
 * @param subtitleView Custom subtitle section composable
 * @param trailingView Custom trailing section composable
 * @param options Function to replace all menu options
 * @param addOptions Function to add options to default menu
 * @param onItemClick Callback for item clicks
 * @param onItemLongClick Callback for item long-clicks
 * @param onError Callback for errors
 * @param onLoad Callback when conversations are loaded
 * @param onEmpty Callback when list is empty
 * @param onBackPress Callback for back navigation
 * @param onSearchClick Callback for search box click
 * @param onSelection Callback for selection completion
 * @param dateFormat Custom SimpleDateFormat for date formatting (takes precedence over dateTimeFormatter)
 * @param conversationsRequestBuilder Custom request builder for fetching conversations
 * @param mentionAllLabelId ID for custom mention all label
 * @param mentionAllLabel Custom label for mention all
 * @param hideSearchEndIcon Whether to hide the search box end icon
 * @param searchText Initial text for the search box
 */
@Composable
public fun CometChatConversations(
    modifier: Modifier = Modifier,
    conversationListViewModel: CometChatConversationsViewModel? = null,
    style: CometChatConversationsStyle = CometChatConversationsStyle.default(),
    // Toolbar configuration
    title: String? = null,
    hideToolbar: Boolean = false,
    hideBackIcon: Boolean = true,
    overflowMenu: (@Composable () -> Unit)? = null,
    // Search configuration
    hideSearchBox: Boolean = false,
    searchPlaceholderText: String? = null,
    hideSearchEndIcon: Boolean = false,
    searchText: String = "",
    // Selection mode
    selectionMode: UIKitConstants.SelectionMode = UIKitConstants.SelectionMode.NONE,
    // Visibility controls
    hideUserStatus: Boolean = false,
    hideGroupType: Boolean = false,
    hideReceipts: Boolean = false,
    hideSeparator: Boolean = false,
    hidePinOption: Boolean = false,
    hideDeleteOption: Boolean = false,
    hideLoadingState: Boolean = false,
    hideEmptyState: Boolean = false,
    hideErrorState: Boolean = false,
    hideOfflineNotices: Boolean = false,
    // Sound configuration
    disableSoundForMessages: Boolean = false,
    customSoundForMessages: Int? = null,
    // Date formatting
    dateFormat: SimpleDateFormat? = null,
    dateTimeFormatter: DateTimeFormatterCallback? = null,
    // Request builder
    conversationsRequestBuilder: ConversationsRequest.ConversationsRequestBuilder? = null,
    // Mention all label
    mentionAllLabelId: String? = null,
    mentionAllLabel: String? = null,
    // Text formatters for message preview
    textFormatters: List<CometChatTextFormatter>? = null,
    // Custom views
    loadingView: (@Composable () -> Unit)? = null,
    emptyView: (@Composable () -> Unit)? = null,
    errorView: (@Composable (onRetry: () -> Unit) -> Unit)? = null,
    itemView: (@Composable (Conversation, TypingIndicator?) -> Unit)? = null,
    leadingView: (@Composable (Conversation, TypingIndicator?) -> Unit)? = null,
    titleView: (@Composable (Conversation, TypingIndicator?) -> Unit)? = null,
    subtitleView: (@Composable (Conversation, TypingIndicator?) -> Unit)? = null,
    trailingView: (@Composable (Conversation, TypingIndicator?) -> Unit)? = null,
    // Menu options
    options: ((Context, Conversation) -> List<MenuItem>)? = null,
    addOptions: ((Context, Conversation) -> List<MenuItem>)? = null,
    // Callbacks
    onItemClick: ((Conversation) -> Unit)? = null,
    onItemLongClick: ((Conversation) -> Unit)? = null,
    onError: ((CometChatException) -> Unit)? = null,
    onLoad: ((List<Conversation>) -> Unit)? = null,
    onEmpty: (() -> Unit)? = null,
    onBackPress: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onSelection: ((List<Conversation>) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Use localized strings with fallback to provided values
    val localizedTitle = title ?: context.getString(R.string.cometchat_chats)
    val localizedSearchPlaceholder = searchPlaceholderText ?: context.getString(R.string.cometchat_search)
    
    // Create default text formatters if none provided, applying mention style from the style object
    val effectiveTextFormatters = textFormatters ?: remember(context, style.mentionStyle, mentionAllLabelId, mentionAllLabel) {
        listOf(
            CometChatMentionsFormatter(context).apply {
                setConversationsMentionStyle(style.mentionStyle)
                // Apply custom mention all label if both parameters are provided
                if (!mentionAllLabelId.isNullOrEmpty() && !mentionAllLabel.isNullOrEmpty()) {
                    setMentionAllLabel(mentionAllLabelId, mentionAllLabel)
                }
            }
        )
    }
    
    // Create default ViewModel if none provided
    // Using viewModel() ensures the ViewModel survives configuration changes (like screen rotation)
    val viewModel = conversationListViewModel ?: viewModel(
        factory = CometChatConversationsViewModelFactory()
    )
    
    // Initialize sound manager and configure sound settings
    LaunchedEffect(viewModel) {
        viewModel.initSoundManager(context)
        viewModel.setDisableSoundForMessages(disableSoundForMessages)
        customSoundForMessages?.let { viewModel.setCustomSoundForMessage(it) }
    }
    
    // Apply conversations request builder if provided
    LaunchedEffect(viewModel, conversationsRequestBuilder) {
        conversationsRequestBuilder?.let { builder ->
            viewModel.setConversationsRequestBuilder(builder)
        }
    }
    
    // Collect state from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    // D84: the offline states ("Connect to search", "Connect to load chats"); always false offline off
    val connectToSearch by viewModel.connectToSearch.collectAsState()
    val connectToLoad by viewModel.connectToLoad.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val typingIndicators by viewModel.typingIndicators.collectAsState()
    val selectedConversations by viewModel.selectedConversations.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    
    // Local state for delete dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }

    // Local state for the unpin-conversation confirmation (only unpin confirms; pin is immediate).
    var showUnpinDialog by remember { mutableStateOf(false) }
    var conversationToUnpin by remember { mutableStateOf<Conversation?>(null) }
    
    // Offline notices (ENG-41088): one toast each. Not collected while hidden, so a hidden list
    // never takes a notice another list would show.
    if (!hideOfflineNotices) {
        LaunchedEffect(viewModel) {
            viewModel.offlineNotices.collect { notice ->
                Toast.makeText(context, offlineNoticeText(context, notice), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Handle state callbacks
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UIState.Content -> onLoad?.invoke(state.conversations)
            is UIState.Empty -> onEmpty?.invoke()
            is UIState.Error -> onError?.invoke(state.exception)
            is UIState.Loading -> { /* No callback for loading */ }
        }
    }

    // D84 rule 5: a read failed online while rows stay on screen; onError still fires.
    val currentOnError by rememberUpdatedState(onError)
    LaunchedEffect(viewModel) {
        viewModel.rowsKeptError.collect { currentOnError?.invoke(it) }
    }
    
    // Handle delete state
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is DeleteState.Success -> {
                viewModel.resetDeleteState()
            }
            is DeleteState.Failure -> {
                onError?.invoke((deleteState as DeleteState.Failure).exception)
                viewModel.resetDeleteState()
            }
            else -> { /* No action */ }
        }
    }
    
    // Create search box style from conversation list style
    val searchBoxStyle = CometChatSearchBoxStyle.default(
        backgroundColor = style.searchBackgroundColor,
        textColor = style.searchTextColor,
        textStyle = style.searchTextStyle,
        placeholderColor = style.searchPlaceholderColor,
        placeholderTextStyle = style.searchPlaceholderTextStyle,
        startIcon = style.searchStartIcon,
        startIconTint = style.searchStartIconTint,
        endIcon = style.searchEndIcon,
        endIconTint = style.searchEndIconTint,
        cornerRadius = style.searchCornerRadius,
        strokeWidth = style.searchStrokeWidth,
        strokeColor = style.searchStrokeColor
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(style.backgroundColor)
    ) {
        // Toolbar
        if (!hideToolbar) {
            ConversationListToolbar(
                title = localizedTitle,
                style = style,
                hideBackIcon = hideBackIcon,
                selectionMode = selectionMode,
                selectedCount = selectedConversations.size,
                overflowMenu = overflowMenu,
                onBackPress = onBackPress,
                onDiscardSelection = { viewModel.clearSelection() },
                onSubmitSelection = { onSelection?.invoke(viewModel.getSelectedConversations()) }
            )
        }
        
        // Search Box
        if (!hideSearchBox) {
            CometChatSearchBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                style = if (hideSearchEndIcon) {
                    searchBoxStyle.copy(endIcon = null)
                } else {
                    searchBoxStyle
                },
                placeholderText = localizedSearchPlaceholder,
                text = searchText,
                enabled = false,
                onClick = onSearchClick
            )
        }
        
        // Content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (uiState) {
                is UIState.Loading -> {
                    if (!hideLoadingState) {
                        loadingView?.invoke() ?: CometChatLoadingState(style = style.loadingStateStyle)
                    }
                }
                
                is UIState.Empty -> {
                    if (!hideEmptyState) {
                        emptyView?.invoke() ?: if (connectToSearch) {
                            // D84 rule 7: an offline `searchKeyword` list.
                            CometChatEmptyState(
                                style = style.emptyStateStyle,
                                title = context.getString(com.cometchat.uikit.core.R.string.cometchat_connect_to_search),
                                subtitle = context.getString(com.cometchat.uikit.core.R.string.cometchat_connect_to_search_subtitle)
                            )
                        } else {
                            CometChatEmptyState(
                                style = style.emptyStateStyle,
                                title = context.getString(R.string.cometchat_empty_conversations_title),
                                subtitle = context.getString(R.string.cometchat_empty_conversations_subtitle)
                            )
                        }
                    }
                }

                is UIState.Error -> {
                    if (!hideErrorState) {
                        errorView?.invoke { viewModel.fetchConversations() }
                            ?: if (connectToLoad) {
                                // D84 rule 3: offline with nothing on screen: the offline state, no Retry.
                                CometChatEmptyState(
                                    style = style.emptyStateStyle,
                                    title = context.getString(com.cometchat.uikit.core.R.string.cometchat_connect_to_load_chats),
                                    subtitle = ""
                                )
                            } else {
                                CometChatErrorState(
                                    style = style.errorStateStyle,
                                    title = context.getString(R.string.cometchat_error_conversations_title),
                                    subtitle = context.getString(R.string.cometchat_something_went_wrong_please_try_again),
                                    onRetry = { viewModel.fetchConversations() }
                                )
                            }
                    }
                }
                
                is UIState.Content -> {
                    ConversationListContent(
                        conversations = conversations,
                        typingIndicators = typingIndicators,
                        selectedConversations = selectedConversations,
                        selectionMode = selectionMode,
                        style = style,
                        hideUserStatus = hideUserStatus,
                        hideGroupType = hideGroupType,
                        hideReceipts = hideReceipts,
                        hideSeparator = hideSeparator,
                        hidePinOption = hidePinOption,
                        hideDeleteOption = hideDeleteOption,
                        dateTimeFormatter = dateTimeFormatter,
                        textFormatters = effectiveTextFormatters,
                        itemView = itemView,
                        leadingView = leadingView,
                        titleView = titleView,
                        subtitleView = subtitleView,
                        trailingView = trailingView,
                        options = options,
                        addOptions = addOptions,
                        onItemClick = { conversation ->
                            if (selectionMode != UIKitConstants.SelectionMode.NONE) {
                                viewModel.selectConversation(conversation, selectionMode)
                            } else {
                                onItemClick?.invoke(conversation)
                            }
                        },
                        onItemLongClick = { conversation ->
                            onItemLongClick?.invoke(conversation)
                        },
                        onDeleteConversation = { conversation ->
                            conversationToDelete = conversation
                            showDeleteDialog = true
                        },
                        onPinToggle = { conversation ->
                            if (conversation.isPinned) {
                                // Only unpin confirms; pin is immediate.
                                conversationToUnpin = conversation
                                showUnpinDialog = true
                            } else {
                                viewModel.pinConversation(
                                    conversation,
                                    onError = { e ->
                                        Toast.makeText(
                                            context,
                                            conversationPinErrorMessage(context, e),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        },
                        onLoadMore = { viewModel.fetchConversations() },
                        scrollToTopEvent = viewModel.scrollToTopEvent
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && conversationToDelete != null) {
        CometChatDialog(
            title = context.getString(R.string.cometchat_conversation_delete_message_title),
            message = context.getString(R.string.cometchat_conversation_delete_message_subtitle),
            positiveButtonText = context.getString(R.string.cometchat_delete),
            negativeButtonText = context.getString(R.string.cometchat_cancel),
            icon = painterResource(R.drawable.cometchat_ic_delete),
            style = style.dialogStyle,
            onPositiveClick = {
                viewModel.deleteConversation(conversationToDelete!!)
                showDeleteDialog = false
                conversationToDelete = null
            },
            onNegativeClick = {
                showDeleteDialog = false
                conversationToDelete = null
            }
        )
    }

    // Unpin confirmation dialog — only unpin confirms (pin is immediate). Non-destructive:
    // primary (purple) positive button, no icon.
    if (showUnpinDialog && conversationToUnpin != null) {
        CometChatDialog(
            title = context.getString(R.string.cometchat_unpin_conversation_confirm_title),
            message = context.getString(R.string.cometchat_unpin_conversation_confirm_body),
            positiveButtonText = context.getString(R.string.cometchat_unpin),
            negativeButtonText = context.getString(R.string.cometchat_cancel),
            hideIcon = true,
            style = CometChatDialogStyle.default(
                positiveButtonBackgroundColor = CometChatTheme.colorScheme.primary
            ),
            onPositiveClick = {
                viewModel.unpinConversation(conversationToUnpin!!)
                showUnpinDialog = false
                conversationToUnpin = null
            },
            onNegativeClick = {
                showUnpinDialog = false
                conversationToUnpin = null
            }
        )
    }
}

/**
 * The pre-6.3.0 signature, kept for binary compatibility (ENG-41088 added [hideOfflineNotices]).
 */
@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
@Composable
public fun CometChatConversations(
    modifier: Modifier = Modifier,
    conversationListViewModel: CometChatConversationsViewModel? = null,
    style: CometChatConversationsStyle = CometChatConversationsStyle.default(),
    title: String? = null,
    hideToolbar: Boolean = false,
    hideBackIcon: Boolean = true,
    overflowMenu: (@Composable () -> Unit)? = null,
    hideSearchBox: Boolean = false,
    searchPlaceholderText: String? = null,
    hideSearchEndIcon: Boolean = false,
    searchText: String = "",
    selectionMode: UIKitConstants.SelectionMode = UIKitConstants.SelectionMode.NONE,
    hideUserStatus: Boolean = false,
    hideGroupType: Boolean = false,
    hideReceipts: Boolean = false,
    hideSeparator: Boolean = false,
    hidePinOption: Boolean = false,
    hideDeleteOption: Boolean = false,
    hideLoadingState: Boolean = false,
    hideEmptyState: Boolean = false,
    hideErrorState: Boolean = false,
    disableSoundForMessages: Boolean = false,
    customSoundForMessages: Int? = null,
    dateFormat: SimpleDateFormat? = null,
    dateTimeFormatter: DateTimeFormatterCallback? = null,
    conversationsRequestBuilder: ConversationsRequest.ConversationsRequestBuilder? = null,
    mentionAllLabelId: String? = null,
    mentionAllLabel: String? = null,
    textFormatters: List<CometChatTextFormatter>? = null,
    loadingView: (@Composable () -> Unit)? = null,
    emptyView: (@Composable () -> Unit)? = null,
    errorView: (@Composable (onRetry: () -> Unit) -> Unit)? = null,
    itemView: (@Composable (Conversation, TypingIndicator?) -> Unit)? = null,
    leadingView: (@Composable (Conversation, TypingIndicator?) -> Unit)? = null,
    titleView: (@Composable (Conversation, TypingIndicator?) -> Unit)? = null,
    subtitleView: (@Composable (Conversation, TypingIndicator?) -> Unit)? = null,
    trailingView: (@Composable (Conversation, TypingIndicator?) -> Unit)? = null,
    options: ((Context, Conversation) -> List<MenuItem>)? = null,
    addOptions: ((Context, Conversation) -> List<MenuItem>)? = null,
    onItemClick: ((Conversation) -> Unit)? = null,
    onItemLongClick: ((Conversation) -> Unit)? = null,
    onError: ((CometChatException) -> Unit)? = null,
    onLoad: ((List<Conversation>) -> Unit)? = null,
    onEmpty: (() -> Unit)? = null,
    onBackPress: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onSelection: ((List<Conversation>) -> Unit)? = null
) {
    CometChatConversations(
        modifier = modifier,
        conversationListViewModel = conversationListViewModel,
        style = style,
        title = title,
        hideToolbar = hideToolbar,
        hideBackIcon = hideBackIcon,
        overflowMenu = overflowMenu,
        hideSearchBox = hideSearchBox,
        searchPlaceholderText = searchPlaceholderText,
        hideSearchEndIcon = hideSearchEndIcon,
        searchText = searchText,
        selectionMode = selectionMode,
        hideUserStatus = hideUserStatus,
        hideGroupType = hideGroupType,
        hideReceipts = hideReceipts,
        hideSeparator = hideSeparator,
        hidePinOption = hidePinOption,
        hideDeleteOption = hideDeleteOption,
        hideLoadingState = hideLoadingState,
        hideEmptyState = hideEmptyState,
        hideErrorState = hideErrorState,
        disableSoundForMessages = disableSoundForMessages,
        customSoundForMessages = customSoundForMessages,
        dateFormat = dateFormat,
        dateTimeFormatter = dateTimeFormatter,
        conversationsRequestBuilder = conversationsRequestBuilder,
        mentionAllLabelId = mentionAllLabelId,
        mentionAllLabel = mentionAllLabel,
        textFormatters = textFormatters,
        loadingView = loadingView,
        emptyView = emptyView,
        errorView = errorView,
        itemView = itemView,
        leadingView = leadingView,
        titleView = titleView,
        subtitleView = subtitleView,
        trailingView = trailingView,
        options = options,
        addOptions = addOptions,
        onItemClick = onItemClick,
        onItemLongClick = onItemLongClick,
        onError = onError,
        onLoad = onLoad,
        onEmpty = onEmpty,
        onBackPress = onBackPress,
        onSearchClick = onSearchClick,
        onSelection = onSelection,
        hideOfflineNotices = false
    )
}

/** The text of one offline notice, from the strings shipped in every locale (ENG-41086). */
private fun offlineNoticeText(context: Context, notice: OfflineNotice): String = when (notice) {
    is OfflineNotice.QueueFull -> context.getString(CoreR.string.cometchat_queue_full)
    is OfflineNotice.FileTooLargeOffline -> context.getString(CoreR.string.cometchat_file_too_large_offline)
    is OfflineNotice.SendingTryAgain -> context.getString(CoreR.string.cometchat_sending_try_again)
    is OfflineNotice.UnsentRemoved -> context.getString(CoreR.string.cometchat_unsent_message_removed)
    is OfflineNotice.MessagesNotSent ->
        context.resources.getQuantityString(CoreR.plurals.cometchat_messages_not_sent, notice.count, notice.count)
    is OfflineNotice.UnsentLost ->
        context.resources.getQuantityString(CoreR.plurals.cometchat_unsent_messages_lost, notice.count, notice.count)
    is OfflineNotice.UnsentMaybeLost -> context.getString(CoreR.string.cometchat_unsent_messages_maybe_lost)
}

/**
 * User-facing message for a failed conversation PIN. When the pinned-conversation limit is hit,
 * shows the cap (read from errorParams, never hard-coded); otherwise a generic error.
 */
private fun conversationPinErrorMessage(context: Context, e: CometChatException?): String {
    val limit = (e?.errorParams?.get("limit") as? Number)?.toInt()
    return if (limit != null) {
        context.getString(R.string.cometchat_pin_conversation_limit_reached, limit)
    } else {
        context.getString(R.string.cometchat_something_went_wrong)
    }
}

/**
 * Internal composable for the conversation list toolbar.
 * Handles both normal mode and selection mode display.
 */
@Composable
private fun ConversationListToolbar(
    title: String,
    style: CometChatConversationsStyle,
    hideBackIcon: Boolean,
    selectionMode: UIKitConstants.SelectionMode,
    selectedCount: Int,
    overflowMenu: (@Composable () -> Unit)?,
    onBackPress: (() -> Unit)?,
    onDiscardSelection: () -> Unit,
    onSubmitSelection: () -> Unit
) {
    val isInSelectionMode = selectionMode != UIKitConstants.SelectionMode.NONE && selectedCount > 0
    
    val toolbarStyle = CometChatToolbarStyle.default(
        backgroundColor = style.backgroundColor,
        titleTextColor = style.titleTextColor,
        titleTextStyle = style.titleTextStyle,
        navigationIcon = style.backIcon,
        navigationIconTint = style.backIconTint,
        separatorColor = style.toolbarSeparatorColor,
        separatorHeight = style.toolbarSeparatorHeight,
        showSeparator = style.showToolbarSeparator
    )
    
    if (isInSelectionMode) {
        // Selection mode toolbar - show just the count number (matches Java implementation)
        CometChatToolbar(
            title = selectedCount.toString(),
            style = toolbarStyle.copy(
                titleTextColor = style.selectionCountTextColor,
                titleTextStyle = style.selectionCountTextStyle
            ),
            hideBackIcon = false,
            navigationIcon = style.discardSelectionIcon,
            navigationContentDescription = "Discard selection",
            onNavigationClick = onDiscardSelection,
            actions = {
                // Submit selection button
                style.submitSelectionIcon?.let { icon ->
                    val cdHoist1 = stringResource(R.string.cometchat_a11y_submit_selection_of_conversations, selectedCount)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .focusable()
                            .clickable { onSubmitSelection() }
                            .semantics { 
                                contentDescription = cdHoist1
                                role = Role.Button
                            }
                    ) {
                        Icon(
                            painter = icon,
                            contentDescription = null,
                            tint = style.submitSelectionIconTint,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(12.dp)
                        )
                    }
                }
            }
        )
    } else {
        // Normal mode toolbar
        CometChatToolbar(
            title = title,
            style = toolbarStyle,
            hideBackIcon = hideBackIcon,
            navigationContentDescription = "Go back",
            onNavigationClick = onBackPress,
            actions = if (overflowMenu != null) {
                { overflowMenu() }
            } else null
        )
    }
}

/**
 * Builds the menu items for the popup menu.
 * Supports custom options, additional options, and default delete option.
 */
@Composable
private fun buildMenuItems(
    context: Context,
    conversation: Conversation,
    style: CometChatConversationsStyle,
    hideDeleteOption: Boolean,
    options: ((Context, Conversation) -> List<MenuItem>)?,
    addOptions: ((Context, Conversation) -> List<MenuItem>)?,
    onDelete: () -> Unit
): List<MenuItem> {
    // If custom options are provided, use them exclusively
    if (options != null) {
        return options(context, conversation)
    }
    
    val menuItems = mutableListOf<MenuItem>()
    
    // Add default delete option if not hidden
    if (!hideDeleteOption) {
        menuItems.add(
            MenuItem(
                id = "delete",
                name = context.getString(R.string.cometchat_delete),
                startIcon = painterResource(R.drawable.cometchat_ic_delete),
                startIconTint = style.deleteOptionIconTint,
                textColor = style.deleteOptionTextColor,
                textStyle = style.deleteOptionTextStyle,
                onClick = onDelete
            )
        )
    }
    
    // Add additional options if provided
    if (addOptions != null) {
        menuItems.addAll(addOptions(context, conversation))
    }
    
    return menuItems
}
