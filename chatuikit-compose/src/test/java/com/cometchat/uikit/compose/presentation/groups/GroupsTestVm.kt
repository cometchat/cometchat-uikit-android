package com.cometchat.uikit.compose.presentation.groups

import com.cometchat.chat.core.GroupsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.uikit.core.domain.repository.GroupsRepository
import com.cometchat.uikit.core.domain.usecase.FetchGroupsUseCase
import com.cometchat.uikit.core.domain.usecase.JoinGroupUseCase
import com.cometchat.uikit.core.viewmodel.CometChatGroupsViewModel

/**
 * ENG-38679 — builds a CometChatGroupsViewModel with a no-op fake repository and
 * listeners disabled, so no real SDK fetch throws inside the compose TestScope.
 * State is driven directly via reflection in the tests.
 */
object GroupsTestVm {
    fun build(): CometChatGroupsViewModel {
        val repository = object : GroupsRepository {
            override suspend fun fetchGroups(request: GroupsRequest): Result<List<Group>> = Result.success(emptyList())
            override suspend fun joinGroup(groupId: String, groupType: String, password: String?): Result<Group> =
                Result.failure(CometChatException("NOOP", "noop"))
            override fun hasMoreGroups(): Boolean = false
        }
        return CometChatGroupsViewModel(
            fetchGroupsUseCase = FetchGroupsUseCase(repository),
            joinGroupUseCase = JoinGroupUseCase(repository),
            enableListeners = false,
        )
    }
}
