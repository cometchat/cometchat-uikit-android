package com.cometchat.uikit.core.domain.usecase

import com.cometchat.chat.models.GroupMember
import com.cometchat.uikit.core.domain.repository.GroupMembersRepository
import com.cometchat.uikit.core.domain.repository.GroupMembersTwoPhaseRepository

/**
 * Use case for fetching group members.
 * Contains business logic for group member retrieval with pagination support.
 * 
 * @param repository The repository to fetch group members from
 */
open public class FetchGroupMembersUseCase(
    private val repository: GroupMembersRepository
) {
    /**
     * Fetches group members based on the provided parameters.
     * @param guid The group ID
     * @param limit Number of members to fetch per page (default: 30)
     * @param searchKeyword Optional search keyword to filter members
     * @return Result containing list of group members or error
     */
    open suspend operator public fun invoke(
        guid: String,
        limit: Int = 30,
        searchKeyword: String? = null
    ): Result<List<GroupMember>> {
        return repository.fetchGroupMembers(guid, limit, searchKeyword)
    }
    
    /**
     * Checks if there are more members available for pagination.
     * @return true if more members can be fetched
     */
    open public fun hasMore(): Boolean = repository.hasMore()
    
    /**
     * Resets the current request so the next fetch builds a fresh one.
     * Call this when the search keyword changes or when refreshing.
     */
    open public fun resetRequest(): Unit = repository.resetRequest()

    /**
     * The repository's two-phase read (BEH-SNAP-03), module-internal so the ViewModel can reach it
     * without widening a public signature. Null — today's single-phase path through [invoke] — when
     * the repository does not implement [GroupMembersTwoPhaseRepository], or when this use case is a subclass
     * (a customer override of [invoke] keeps running).
     */
    internal val twoPhaseRepository: GroupMembersTwoPhaseRepository?
        get() = if (javaClass == FetchGroupMembersUseCase::class.java) repository as? GroupMembersTwoPhaseRepository else null
}
