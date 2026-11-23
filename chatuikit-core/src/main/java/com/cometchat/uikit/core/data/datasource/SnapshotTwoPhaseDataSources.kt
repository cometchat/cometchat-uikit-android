package com.cometchat.uikit.core.data.datasource

import androidx.annotation.RestrictTo
import com.cometchat.chat.core.GroupsRequest
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.flow.Flow

/**
 * Two-phase (cached → server) users read (BEH-SNAP-01..07, BEH-READ-01). A **new** interface — no
 * member is added to [UsersDataSource] and there are no default methods — so a custom data source
 * that does not implement it keeps today's single-phase path. [UsersDataSourceImpl] implements it;
 * the repository detects support with `is`.
 *
 * Public so a custom data source can implement it, but [RestrictTo] `LIBRARY_GROUP` — internal to
 * the kit, not part of the customer contract.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface UsersTwoPhaseDataSource {
    public fun fetchUsersTwoPhase(request: UsersRequest): Flow<TwoPhaseResult<User>>
}

/** Two-phase groups read, added the same way as [UsersTwoPhaseDataSource]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface GroupsTwoPhaseDataSource {
    public fun fetchGroupsTwoPhase(request: GroupsRequest): Flow<TwoPhaseResult<Group>>
}

/**
 * Two-phase group members read, added the same way as [UsersTwoPhaseDataSource]. Same inputs and
 * the same paging request as [GroupMembersDataSource.fetchGroupMembers].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface GroupMembersTwoPhaseDataSource {
    public fun fetchGroupMembersTwoPhase(
        guid: String,
        limit: Int,
        searchKeyword: String?
    ): Flow<TwoPhaseResult<GroupMember>>
}
