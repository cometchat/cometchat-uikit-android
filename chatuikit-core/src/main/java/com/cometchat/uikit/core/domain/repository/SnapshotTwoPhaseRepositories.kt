package com.cometchat.uikit.core.domain.repository

import androidx.annotation.RestrictTo
import com.cometchat.chat.core.GroupsRequest
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository-level two-phase users read (BEH-SNAP-01..07). A **new** interface — nothing is added
 * to [UsersRepository] and there are no default methods — so a custom repository that does not
 * implement it keeps today's single-phase path. [getUsersTwoPhase] ↔ [UsersRepository.getUsers]
 * (same request; `hasMoreUsers` changes only on the **server** phase, BEH-READ-09b).
 *
 * Public so a custom repository can implement it, but [RestrictTo] `LIBRARY_GROUP` — internal to
 * the kit, not part of the customer contract.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface UsersTwoPhaseRepository {
    public fun getUsersTwoPhase(request: UsersRequest): Flow<TwoPhaseResult<User>>
}

/** Two-phase twin of [GroupsRepository.fetchGroups], added the same way as [UsersTwoPhaseRepository]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface GroupsTwoPhaseRepository {
    public fun fetchGroupsTwoPhase(request: GroupsRequest): Flow<TwoPhaseResult<Group>>
}

/**
 * Two-phase twin of [GroupMembersRepository.fetchGroupMembers] (same inputs, same paging request,
 * reset by [GroupMembersRepository.resetRequest]), added the same way as [UsersTwoPhaseRepository].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface GroupMembersTwoPhaseRepository {
    public fun fetchGroupMembersTwoPhase(
        guid: String,
        limit: Int,
        searchKeyword: String?
    ): Flow<TwoPhaseResult<GroupMember>>
}
