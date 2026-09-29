package com.cometchat.uikit.compose.preview.presentation.viewmodels

import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.compose.presentation.conversations.utils.TypingIndicator
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import com.cometchat.uikit.compose.preview.domain.PreviewEmptyConversationListUseCase
import com.cometchat.uikit.compose.preview.domain.PreviewErrorConversationListUseCase
import com.cometchat.uikit.compose.preview.domain.PreviewGetConversationListUseCase
import com.cometchat.uikit.compose.preview.domain.PreviewMockData
import com.cometchat.uikit.compose.preview.domain.PreviewRefreshUseCase
import com.cometchat.uikit.compose.preview.domain.PreviewSuccessDeleteUseCase
import com.cometchat.uikit.core.domain.usecase.DeleteConversationUseCase
import com.cometchat.uikit.core.domain.usecase.GetConversationListUseCase
import com.cometchat.uikit.core.domain.usecase.RefreshConversationListUseCase

/**
 * Factory object for creating preview-specific ViewModels with various configurations.
 * These ViewModels are designed for Compose previews and testing different UI states.
 */
public object PreviewViewModelFactory {
    
    /**
     * Creates a ViewModel with default sample conversations.
     * Shows the normal content state with a mix of user and group conversations.
     */
    public fun createDefaultViewModel(): CometChatConversationsViewModel {
        return CometChatConversationsViewModel(
            getConversationListUseCase = PreviewGetConversationListUseCase(
                conversations = PreviewMockData.createSampleConversations()
            ),
            deleteConversationUseCase = PreviewSuccessDeleteUseCase(),
            refreshConversationListUseCase = PreviewRefreshUseCase(),
            enableListeners = false // Disable CometChat listeners for preview
        )
    }
    
    /**
     * Creates a ViewModel that shows the empty state.
     */
    public fun createEmptyStateViewModel(): CometChatConversationsViewModel {
        return CometChatConversationsViewModel(
            getConversationListUseCase = PreviewEmptyConversationListUseCase(),
            deleteConversationUseCase = PreviewSuccessDeleteUseCase(),
            refreshConversationListUseCase = PreviewRefreshUseCase(emptyList()),
            enableListeners = false
        )
    }
    
    /**
     * Creates a ViewModel that shows the error state.
     */
    public fun createErrorStateViewModel(
        errorMessage: String = "Failed to load conversations. Please try again."
    ): CometChatConversationsViewModel {
        return CometChatConversationsViewModel(
            getConversationListUseCase = PreviewErrorConversationListUseCase(
                errorMessage = errorMessage
            ),
            deleteConversationUseCase = PreviewSuccessDeleteUseCase(),
            refreshConversationListUseCase = PreviewRefreshUseCase(),
            enableListeners = false
        )
    }
    
    /**
     * Creates a ViewModel with custom conversations.
     */
    public fun createCustomConversationsViewModel(
        conversations: List<Conversation>
    ): CometChatConversationsViewModel {
        return CometChatConversationsViewModel(
            getConversationListUseCase = PreviewGetConversationListUseCase(
                conversations = conversations
            ),
            deleteConversationUseCase = PreviewSuccessDeleteUseCase(),
            refreshConversationListUseCase = PreviewRefreshUseCase(conversations),
            enableListeners = false
        )
    }
    
    /**
     * Creates a ViewModel with high unread count conversations.
     * Useful for testing badge display with large numbers.
     */
    public fun createHighUnreadViewModel(): CometChatConversationsViewModel {
        return createCustomConversationsViewModel(
            PreviewMockData.createHighUnreadConversations()
        )
    }
    
    /**
     * Creates a ViewModel with only group conversations.
     * Useful for testing group type indicators.
     */
    public fun createGroupsOnlyViewModel(): CometChatConversationsViewModel {
        return createCustomConversationsViewModel(
            PreviewMockData.createGroupTypeConversations()
        )
    }
    
    /**
     * Creates a ViewModel with only user conversations.
     * Useful for testing user status indicators.
     */
    public fun createUsersOnlyViewModel(): CometChatConversationsViewModel {
        return createCustomConversationsViewModel(
            PreviewMockData.createUserStatusConversations()
        )
    }
    
    /**
     * Creates a ViewModel with a large list for pagination testing.
     */
    public fun createLargeListViewModel(count: Int = 50): CometChatConversationsViewModel {
        return createCustomConversationsViewModel(
            PreviewMockData.createLargeConversationList(count)
        )
    }
    
    /**
     * Creates a ViewModel with custom use cases for advanced testing.
     */
    public fun createWithCustomUseCases(
        getConversationListUseCase: GetConversationListUseCase,
        deleteConversationUseCase: DeleteConversationUseCase,
        refreshConversationListUseCase: RefreshConversationListUseCase
    ): CometChatConversationsViewModel {
        return CometChatConversationsViewModel(
            getConversationListUseCase = getConversationListUseCase,
            deleteConversationUseCase = deleteConversationUseCase,
            refreshConversationListUseCase = refreshConversationListUseCase,
            enableListeners = false
        )
    }
}

/**
 * Extension functions for creating typing indicators in previews.
 */
public object PreviewTypingIndicatorFactory {
    
    /**
     * Creates a typing indicator for a user conversation.
     */
    public fun createUserTypingIndicator(
        userId: String = "user_1",
        userName: String = "Alice"
    ): TypingIndicator {
        val sender = PreviewMockData.createMockUser(uid = userId, name = userName)
        return TypingIndicator(
            typingUsers = listOf(sender),
            isTyping = true
        )
    }
    
    /**
     * Creates a typing indicator for a group conversation.
     */
    public fun createGroupTypingIndicator(
        groupId: String = "group_1",
        userId: String = "user_1",
        userName: String = "Alice"
    ): TypingIndicator {
        val sender = PreviewMockData.createMockUser(uid = userId, name = userName)
        return TypingIndicator(
            typingUsers = listOf(sender),
            isTyping = true
        )
    }
    
    /**
     * Creates multiple typing indicators for a group (multiple users typing).
     */
    public fun createMultipleUsersTypingIndicator(
        userNames: List<String> = listOf("Alice", "Bob", "Charlie")
    ): TypingIndicator {
        val users = userNames.mapIndexed { index, name ->
            PreviewMockData.createMockUser(uid = "user_$index", name = name)
        }
        return TypingIndicator(
            typingUsers = users,
            isTyping = true
        )
    }
}
