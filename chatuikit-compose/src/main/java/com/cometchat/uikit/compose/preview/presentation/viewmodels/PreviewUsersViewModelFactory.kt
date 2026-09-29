package com.cometchat.uikit.compose.preview.presentation.viewmodels

import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.preview.data.repository.PreviewUsersRepository
import com.cometchat.uikit.compose.preview.domain.PreviewMockData
import com.cometchat.uikit.core.domain.usecase.FetchUsersUseCase
import com.cometchat.uikit.core.domain.usecase.SearchUsersUseCase
import com.cometchat.uikit.core.viewmodel.CometChatUsersViewModel

/**
 * Factory object for creating preview-specific Users ViewModels.
 * These ViewModels are designed for Compose previews and testing different UI states.
 */
public object PreviewUsersViewModelFactory {

    /**
     * Creates a ViewModel with default sample users.
     */
    public fun createDefaultViewModel(): CometChatUsersViewModel {
        val repository = PreviewUsersRepository()
        return CometChatUsersViewModel(
            fetchUsersUseCase = FetchUsersUseCase(repository),
            searchUsersUseCase = SearchUsersUseCase(repository),
            enableListeners = false
        )
    }

    /**
     * Creates a ViewModel that shows the empty state.
     */
    public fun createEmptyStateViewModel(): CometChatUsersViewModel {
        val repository = PreviewUsersRepository(simulateEmpty = true)
        return CometChatUsersViewModel(
            fetchUsersUseCase = FetchUsersUseCase(repository),
            searchUsersUseCase = SearchUsersUseCase(repository),
            enableListeners = false
        )
    }

    /**
     * Creates a ViewModel that shows the error state.
     */
    public fun createErrorStateViewModel(): CometChatUsersViewModel {
        val repository = PreviewUsersRepository(simulateError = true)
        return CometChatUsersViewModel(
            fetchUsersUseCase = FetchUsersUseCase(repository),
            searchUsersUseCase = SearchUsersUseCase(repository),
            enableListeners = false
        )
    }

    /**
     * Creates a ViewModel with custom users.
     */
    public fun createCustomUsersViewModel(users: List<User>): CometChatUsersViewModel {
        val repository = PreviewUsersRepository(initialUsers = users)
        return CometChatUsersViewModel(
            fetchUsersUseCase = FetchUsersUseCase(repository),
            searchUsersUseCase = SearchUsersUseCase(repository),
            enableListeners = false
        )
    }

    /**
     * Creates a ViewModel with only online users.
     */
    public fun createOnlineUsersViewModel(): CometChatUsersViewModel {
        return createCustomUsersViewModel(PreviewMockData.createOnlineUsers())
    }

    /**
     * Creates a ViewModel with only offline users.
     */
    public fun createOfflineUsersViewModel(): CometChatUsersViewModel {
        return createCustomUsersViewModel(PreviewMockData.createOfflineUsers())
    }

    /**
     * Creates a ViewModel with a large list for scroll testing.
     */
    public fun createLargeListViewModel(count: Int = 30): CometChatUsersViewModel {
        return createCustomUsersViewModel(PreviewMockData.createLargeUserList(count))
    }
}
