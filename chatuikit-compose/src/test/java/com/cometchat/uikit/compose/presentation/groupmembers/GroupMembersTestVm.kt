package com.cometchat.uikit.compose.presentation.groupmembers

import com.cometchat.chat.models.GroupMember
import com.cometchat.uikit.core.domain.repository.GroupMembersRepository
import com.cometchat.uikit.core.domain.usecase.BanGroupMemberUseCase
import com.cometchat.uikit.core.domain.usecase.ChangeMemberScopeUseCase
import com.cometchat.uikit.core.domain.usecase.FetchGroupMembersUseCase
import com.cometchat.uikit.core.domain.usecase.KickGroupMemberUseCase
import com.cometchat.uikit.core.viewmodel.CometChatGroupMembersViewModel

/**
 * ENG-38679 — builds a CometChatGroupMembersViewModel with a no-op fake repository
 * and listeners disabled, so no real SDK fetch throws inside the compose TestScope.
 * State is driven directly via reflection in the tests.
 */
object GroupMembersTestVm {
    fun build(): CometChatGroupMembersViewModel {
        val repository = object : GroupMembersRepository {
            override suspend fun fetchGroupMembers(guid: String, limit: Int, searchKeyword: String?): Result<List<GroupMember>> = Result.success(emptyList())
            override suspend fun kickMember(guid: String, uid: String): Result<Unit> = Result.success(Unit)
            override suspend fun banMember(guid: String, uid: String): Result<Unit> = Result.success(Unit)
            override suspend fun changeMemberScope(guid: String, uid: String, scope: String): Result<Unit> = Result.success(Unit)
            override fun hasMore(): Boolean = false
            override fun resetRequest() {}
        }
        return CometChatGroupMembersViewModel(
            fetchGroupMembersUseCase = FetchGroupMembersUseCase(repository),
            kickGroupMemberUseCase = KickGroupMemberUseCase(repository),
            banGroupMemberUseCase = BanGroupMemberUseCase(repository),
            changeMemberScopeUseCase = ChangeMemberScopeUseCase(repository),
            enableListeners = false,
        )
    }
}
