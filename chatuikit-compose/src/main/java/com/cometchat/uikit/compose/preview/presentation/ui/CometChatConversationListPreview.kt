package com.cometchat.uikit.compose.preview.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.compose.R
import com.cometchat.uikit.compose.presentation.conversations.style.CometChatConversationsStyle
import com.cometchat.uikit.compose.presentation.conversations.ui.CometChatConversations
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.compose.presentation.conversations.utils.ConversationUtils
import com.cometchat.uikit.compose.presentation.shared.defaultstates.CometChatLoadingState
import com.cometchat.uikit.compose.presentation.shared.toolbar.CometChatToolbar
import com.cometchat.uikit.compose.presentation.shared.toolbar.CometChatToolbarStyle
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import com.cometchat.uikit.core.factory.CometChatConversationsViewModelFactory
import com.cometchat.uikit.compose.preview.data.repository.PreviewConversationListRepository
import com.cometchat.uikit.compose.preview.domain.PreviewMockData
import com.cometchat.uikit.compose.preview.presentation.viewmodels.PreviewViewModelFactory
import com.cometchat.uikit.compose.shared.views.popupmenu.MenuItem
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.darkColorScheme

/**
 * Helper to create a preview ViewModel with mock data using the factory pattern.
 */
@Composable
private fun rememberPreviewViewModel(
    conversations: List<Conversation> = PreviewMockData.createSampleConversations(),
    simulateError: Boolean = false,
    simulateEmpty: Boolean = false
): CometChatConversationsViewModel {
    val factory = remember(conversations, simulateError, simulateEmpty) {
        CometChatConversationsViewModelFactory(
            repository = PreviewConversationListRepository(
                initialConversations = conversations,
                simulateError = simulateError,
                simulateEmpty = simulateEmpty
            ),
            enableListeners = false
        )
    }
    return viewModel(factory = factory)
}

// ============================================================================
// SECTION 1: UI STATE PREVIEWS
// ============================================================================

/**
 * Preview showing the loading state.
 */
@Preview(showBackground = true, name = "State - Loading")
@Composable
public fun PreviewConversationListLoading() {
    CometChatTheme {
        val style = CometChatConversationsStyle.default()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(style.backgroundColor)
        ) {
            CometChatToolbar(
                title = "Chats",
                style = CometChatToolbarStyle.default(
                    backgroundColor = style.backgroundColor,
                    titleTextColor = style.titleTextColor,
                    titleTextStyle = style.titleTextStyle
                ),
                hideBackIcon = true
            )
           CometChatLoadingState(style = style.loadingStateStyle)
        }
    }
}

/**
 * Preview showing the empty state.
 */
@Preview(showBackground = true, name = "State - Empty")
@Composable
public fun PreviewConversationListEmpty() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(simulateEmpty = true)
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Chats",
            hideBackIcon = true
        )
    }
}

/**
 * Preview showing the error state.
 */
@Preview(showBackground = true, name = "State - Error")
@Composable
public fun PreviewConversationListError() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(simulateError = true)
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Chats",
            hideBackIcon = true
        )
    }
}

/**
 * Preview showing the content state with conversations.
 */
@Preview(showBackground = true, name = "State - Content")
@Composable
public fun PreviewConversationListContent() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel()
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Chats",
            hideBackIcon = true
        )
    }
}

// ============================================================================
// SECTION 2: CUSTOM VIEWMODEL PREVIEWS
// ============================================================================

/**
 * Preview using PreviewViewModelFactory with default ViewModel.
 */
@Preview(showBackground = true, name = "ViewModel - Default Factory")
@Composable
public fun PreviewWithDefaultFactoryViewModel() {
    CometChatTheme {
        val viewModel = remember { PreviewViewModelFactory.createDefaultViewModel() }
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Default ViewModel",
            hideBackIcon = true
        )
    }
}

/**
 * Preview using custom ViewModel with high unread counts.
 */
@Preview(showBackground = true, name = "ViewModel - High Unread")
@Composable
public fun PreviewWithHighUnreadViewModel() {
    CometChatTheme {
        val viewModel = remember { PreviewViewModelFactory.createHighUnreadViewModel() }
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "High Unread Counts",
            hideBackIcon = true
        )
    }
}

/**
 * Preview using custom ViewModel with only groups.
 */
@Preview(showBackground = true, name = "ViewModel - Groups Only")
@Composable
public fun PreviewWithGroupsOnlyViewModel() {
    CometChatTheme {
        val viewModel = remember { PreviewViewModelFactory.createGroupsOnlyViewModel() }
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Groups Only",
            hideBackIcon = true
        )
    }
}

/**
 * Preview using custom ViewModel with only users.
 */
@Preview(showBackground = true, name = "ViewModel - Users Only")
@Composable
public fun PreviewWithUsersOnlyViewModel() {
    CometChatTheme {
        val viewModel = remember { PreviewViewModelFactory.createUsersOnlyViewModel() }
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Users Only",
            hideBackIcon = true
        )
    }
}

/**
 * Preview using custom ViewModel with empty state.
 */
@Preview(showBackground = true, name = "ViewModel - Empty State")
@Composable
public fun PreviewWithEmptyStateViewModel() {
    CometChatTheme {
        val viewModel = remember { PreviewViewModelFactory.createEmptyStateViewModel() }
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Empty State",
            hideBackIcon = true
        )
    }
}

/**
 * Preview using custom ViewModel with error state.
 */
@Preview(showBackground = true, name = "ViewModel - Error State")
@Composable
public fun PreviewWithErrorStateViewModel() {
    CometChatTheme {
        val viewModel = remember { 
            PreviewViewModelFactory.createErrorStateViewModel("Network connection failed") 
        }
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Error State",
            hideBackIcon = true
        )
    }
}

// ============================================================================
// SECTION 3: SELECTION MODE PREVIEWS
// ============================================================================

/**
 * Preview showing single selection mode.
 */
@Preview(showBackground = true, name = "Selection - Single")
@Composable
public fun PreviewSingleSelection() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(4)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Single Selection",
            hideBackIcon = true,
            selectionMode = UIKitConstants.SelectionMode.SINGLE
        )
    }
}

/**
 * Preview showing multiple selection mode.
 */
@Preview(showBackground = true, name = "Selection - Multiple")
@Composable
public fun PreviewMultipleSelection() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(4)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Multiple Selection",
            hideBackIcon = true,
            selectionMode = UIKitConstants.SelectionMode.MULTIPLE
        )
    }
}

// ============================================================================
// SECTION 4: VISIBILITY PROPS PREVIEWS
// ============================================================================

/**
 * Preview without toolbar.
 */
@Preview(showBackground = true, name = "Visibility - No Toolbar")
@Composable
public fun PreviewNoToolbar() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            hideToolbar = true
        )
    }
}

/**
 * Preview without search box.
 */
@Preview(showBackground = true, name = "Visibility - No Search Box")
@Composable
public fun PreviewNoSearchBox() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "No Search",
            hideBackIcon = true,
            hideSearchBox = true
        )
    }
}

/**
 * Preview without separators.
 */
@Preview(showBackground = true, name = "Visibility - No Separators")
@Composable
public fun PreviewNoSeparators() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(4)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "No Separators",
            hideBackIcon = true,
            hideSeparator = true
        )
    }
}

/**
 * Preview with hidden user status.
 */
@Preview(showBackground = true, name = "Visibility - No User Status")
@Composable
public fun PreviewHideUserStatus() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createUserStatusConversations()
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Status Hidden",
            hideBackIcon = true,
            hideUserStatus = true
        )
    }
}

/**
 * Preview with hidden group type.
 */
@Preview(showBackground = true, name = "Visibility - No Group Type")
@Composable
public fun PreviewHideGroupType() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createGroupTypeConversations()
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Group Type Hidden",
            hideBackIcon = true,
            hideGroupType = true
        )
    }
}

/**
 * Preview with hidden receipts.
 */
@Preview(showBackground = true, name = "Visibility - No Receipts")
@Composable
public fun PreviewHideReceipts() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel()
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Receipts Hidden",
            hideBackIcon = true,
            hideReceipts = true
        )
    }
}

/**
 * Preview with back button visible.
 */
@Preview(showBackground = true, name = "Visibility - With Back Button")
@Composable
public fun PreviewWithBackButton() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "With Back",
            hideBackIcon = false,
            onBackPress = { }
        )
    }
}


// ============================================================================
// SECTION 5: CUSTOM VIEW OVERRIDES
// ============================================================================

/**
 * Preview with custom loading view.
 */
@Preview(showBackground = true, name = "Custom View - Loading")
@Composable
public fun PreviewCustomLoadingView() {
    CometChatTheme {
        val style = CometChatConversationsStyle.default()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(style.backgroundColor)
        ) {
           CometChatToolbar(
                title = "Custom Loading",
                style = CometChatToolbarStyle.default(
                    backgroundColor = style.backgroundColor,
                    titleTextColor = style.titleTextColor
                ),
                hideBackIcon = true
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = CometChatTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading conversations...",
                        style = CometChatTheme.typography.bodyRegular,
                        color = CometChatTheme.colorScheme.textColorSecondary
                    )
                }
            }
        }
    }
}

/**
 * Preview with custom empty view.
 */
@Preview(showBackground = true, name = "Custom View - Empty")
@Composable
public fun PreviewCustomEmptyView() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(simulateEmpty = true)
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Custom Empty",
            hideBackIcon = true,
            emptyView = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(CometChatTheme.colorScheme.backgroundColor3),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = CometChatTheme.colorScheme.iconTintSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No conversations yet",
                            style = CometChatTheme.typography.heading3Medium,
                            color = CometChatTheme.colorScheme.textColorPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start a new chat!",
                            style = CometChatTheme.typography.bodyRegular,
                            color = CometChatTheme.colorScheme.textColorSecondary
                        )
                    }
                }
            }
        )
    }
}

/**
 * Preview with custom error view.
 */
@Preview(showBackground = true, name = "Custom View - Error")
@Composable
public fun PreviewCustomErrorView() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(simulateError = true)
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Custom Error",
            hideBackIcon = true,
            errorView = { onRetry ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(CometChatTheme.colorScheme.errorColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "!",
                                style = CometChatTheme.typography.heading1Bold,
                                color = CometChatTheme.colorScheme.errorColor
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Something went wrong",
                            style = CometChatTheme.typography.heading3Medium,
                            color = CometChatTheme.colorScheme.textColorPrimary
                        )
                    }
                }
            }
        )
    }
}

/**
 * Preview with custom item view.
 */
@Preview(showBackground = true, name = "Custom View - Item")
@Composable
public fun PreviewCustomItemView() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Custom Items",
            hideBackIcon = true,
            itemView = { conversation, _ ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = CometChatTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = ConversationUtils.getConversationTitle(conversation),
                            style = CometChatTheme.typography.heading4Medium,
                            color = CometChatTheme.colorScheme.textColorPrimary
                        )
                    }
                }
            }
        )
    }
}

/**
 * Preview with custom leading view.
 */
@Preview(showBackground = true, name = "Custom View - Leading")
@Composable
public fun PreviewCustomLeadingView() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Custom Leading",
            hideBackIcon = true,
            leadingView = { conversation, _ ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CometChatTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ConversationUtils.getConversationTitle(conversation)
                            .firstOrNull()?.uppercase() ?: "?",
                        style = CometChatTheme.typography.heading3Bold,
                        color = CometChatTheme.colorScheme.colorWhite
                    )
                }
            }
        )
    }
}

/**
 * Preview with custom title view.
 */
@Preview(showBackground = true, name = "Custom View - Title")
@Composable
public fun PreviewListCustomTitleView() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Custom Title",
            hideBackIcon = true,
            titleView = { conversation, _ ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = CometChatTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ConversationUtils.getConversationTitle(conversation),
                        style = CometChatTheme.typography.heading4Bold,
                        color = CometChatTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

/**
 * Preview with custom subtitle view.
 */
@Preview(showBackground = true, name = "Custom View - Subtitle")
@Composable
public fun PreviewListCustomSubtitleView() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Custom Subtitle",
            hideBackIcon = true,
            subtitleView = { conversation, typingIndicator ->
                Text(
                    text = if (typingIndicator?.isTyping == true) "✍️ typing..." else "Custom subtitle",
                    style = CometChatTheme.typography.caption1Medium,
                    color = if (typingIndicator?.isTyping == true) 
                        CometChatTheme.colorScheme.primary 
                    else 
                        CometChatTheme.colorScheme.textColorSecondary
                )
            }
        )
    }
}

/**
 * Preview with custom trailing view.
 */
@Preview(showBackground = true, name = "Custom View - Trailing")
@Composable
public fun PreviewListCustomTrailingView() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Custom Trailing",
            hideBackIcon = true,
            trailingView = { conversation, _ ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Now",
                        style = CometChatTheme.typography.caption1Regular,
                        color = CometChatTheme.colorScheme.textColorTertiary
                    )
                    if (conversation.unreadMessageCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(CometChatTheme.colorScheme.errorColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${conversation.unreadMessageCount}",
                                style = CometChatTheme.typography.caption2Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        )
    }
}


// ============================================================================
// SECTION 6: TOOLBAR CUSTOMIZATION PREVIEWS
// ============================================================================

/**
 * Preview with custom title text.
 */
@Preview(showBackground = true, name = "Toolbar - Custom Title")
@Composable
public fun PreviewCustomTitle() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(2)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "My Conversations",
            hideBackIcon = true
        )
    }
}

/**
 * Preview with custom overflow menu.
 */
@Preview(showBackground = true, name = "Toolbar - Custom Overflow Menu")
@Composable
public fun PreviewCustomOverflowMenu() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(2)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Chats",
            hideBackIcon = true,
            overflowMenu = {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cometchat_a11y_more_options),
                        tint = CometChatTheme.colorScheme.iconTintPrimary
                    )
                }
            }
        )
    }
}

/**
 * Preview with custom search placeholder.
 */
@Preview(showBackground = true, name = "Toolbar - Custom Search Placeholder")
@Composable
public fun PreviewCustomSearchPlaceholder() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(2)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Chats",
            hideBackIcon = true,
            searchPlaceholderText = "Find conversations..."
        )
    }
}

// ============================================================================
// SECTION 7: STYLE CUSTOMIZATION PREVIEWS
// ============================================================================

/**
 * Preview with custom background color.
 */
@Preview(showBackground = true, name = "Style - Custom Background")
@Composable
public fun PreviewCustomBackgroundStyle() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Custom Background",
            hideBackIcon = true,
            style = CometChatConversationsStyle.default(
                backgroundColor = Color(0xFFF5F5F5)
            )
        )
    }
}

/**
 * Preview with custom title text color.
 */
@Preview(showBackground = true, name = "Style - Custom Title Color")
@Composable
public fun PreviewCustomTitleColorStyle() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Custom Title Color",
            hideBackIcon = true,
            style = CometChatConversationsStyle.default(
                titleTextColor = CometChatTheme.colorScheme.primary
            )
        )
    }
}

/**
 * Preview with dark theme.
 */
@Preview(showBackground = true, name = "Style - Dark Theme")
@Composable
public fun PreviewDarkThemeStyle() {
    CometChatTheme(colorScheme = darkColorScheme()) {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Dark Theme",
            hideBackIcon = true
        )
    }
}

// ============================================================================
// SECTION: OPTIONS PREVIEWS
// ============================================================================

/**
 * Preview with custom options (replace default menu).
 */
@Preview(showBackground = true, name = "Options - Replace")
@Composable
public fun PreviewOptionsReplace() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Custom Options",
            hideBackIcon = true,
            options = { context, conversation ->
                listOf(
                    MenuItem(id = "pin", name = "Pin Chat"),
                    MenuItem(id = "mute", name = "Mute Notifications"),
                    MenuItem(id = "archive", name = "Archive")
                )
            }
        )
    }
}

/**
 * Preview with additional options (append to default menu).
 */
@Preview(showBackground = true, name = "Options - Append")
@Composable
public fun PreviewOptionsAppend() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Add Options",
            hideBackIcon = true,
            addOptions = { context, conversation ->
                listOf(
                    MenuItem(id = "pin", name = "Pin Chat"),
                    MenuItem(id = "mute", name = "Mute")
                )
            }
        )
    }
}

// ============================================================================
// SECTION 8: COMPREHENSIVE PREVIEWS
// ============================================================================

/**
 * Preview showing all features combined.
 */
@Preview(showBackground = true, name = "Comprehensive - All Features")
@Composable
public fun PreviewComprehensive() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel()
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "All Features",
            hideBackIcon = false,
            searchPlaceholderText = "Search chats...",
            overflowMenu = {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cometchat_a11y_more),
                        tint = CometChatTheme.colorScheme.iconTintPrimary
                    )
                }
            },
            onBackPress = { },
            onItemClick = { },
            onSearchClick = { }
        )
    }
}

/**
 * Preview showing minimal configuration.
 */
@Preview(showBackground = true, name = "Comprehensive - Minimal")
@Composable
public fun PreviewMinimal() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            hideToolbar = true,
            hideSearchBox = true,
            hideSeparator = true
        )
    }
}

/**
 * Preview showing large list for scroll testing.
 */
@Preview(showBackground = true, name = "Comprehensive - Large List")
@Composable
public fun PreviewLargeList() {
    CometChatTheme {
        val viewModel = remember { PreviewViewModelFactory.createLargeListViewModel(20) }
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "Large List (20 items)",
            hideBackIcon = true
        )
    }
}

/**
 * Preview showing all custom views combined.
 */
@Preview(showBackground = true, name = "Comprehensive - All Custom Views")
@Composable
public fun PreviewListAllCustomViews() {
    CometChatTheme {
        val viewModel = rememberPreviewViewModel(
            conversations = PreviewMockData.createSampleConversations().take(3)
        )
        CometChatConversations(
            conversationListViewModel = viewModel,
            title = "All Custom Views",
            hideBackIcon = true,
            leadingView = { conversation, _ ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CometChatTheme.colorScheme.infoColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ConversationUtils.getConversationTitle(conversation)
                            .take(2).uppercase(),
                        style = CometChatTheme.typography.caption1Bold,
                        color = Color.White
                    )
                }
            },
            titleView = { conversation, _ ->
                Text(
                    text = "★ ${ConversationUtils.getConversationTitle(conversation)}",
                    style = CometChatTheme.typography.heading4Bold,
                    color = CometChatTheme.colorScheme.primary
                )
            },
            subtitleView = { _, _ ->
                Text(
                    text = "Custom subtitle text",
                    style = CometChatTheme.typography.caption1Regular,
                    color = CometChatTheme.colorScheme.textColorSecondary
                )
            },
            trailingView = { conversation, _ ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "12:30 PM",
                        style = CometChatTheme.typography.caption2Regular,
                        color = CometChatTheme.colorScheme.textColorTertiary
                    )
                    if (conversation.unreadMessageCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📬 ${conversation.unreadMessageCount}",
                            style = CometChatTheme.typography.caption2Medium,
                            color = CometChatTheme.colorScheme.primary
                        )
                    }
                }
            }
        )
    }
}
