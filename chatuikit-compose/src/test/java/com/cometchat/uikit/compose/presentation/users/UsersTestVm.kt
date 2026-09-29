package com.cometchat.uikit.compose.presentation.users

import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.UsersRepository
import com.cometchat.uikit.core.domain.usecase.FetchUsersUseCase
import com.cometchat.uikit.core.domain.usecase.SearchUsersUseCase
import com.cometchat.uikit.core.viewmodel.CometChatUsersViewModel

/**
 * ENG-38679 — builds a CometChatUsersViewModel with a no-op fake repository and
 * listeners disabled, so no real SDK fetch runs (which would throw an uncaught
 * exception inside the compose test's TestScope). State is driven directly via
 * reflection in the tests. Mirrors the pattern in CometChatUsersScreenshotTest.
 */
object UsersTestVm {
    fun build(): CometChatUsersViewModel {
        val repository = object : UsersRepository {
            override suspend fun getUsers(request: UsersRequest): Result<List<User>> = Result.success(emptyList())
            override fun hasMoreUsers(): Boolean = false
        }
        return CometChatUsersViewModel(
            fetchUsersUseCase = FetchUsersUseCase(repository),
            searchUsersUseCase = SearchUsersUseCase(repository),
            enableListeners = false,
        )
    }
}
