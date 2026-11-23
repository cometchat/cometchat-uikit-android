package com.cometchat.uikit.core.domain.usecase

import com.cometchat.chat.core.GroupsRequest
import com.cometchat.chat.models.Group
import com.cometchat.uikit.core.domain.repository.GroupsRepository
import com.cometchat.uikit.core.domain.repository.GroupsTwoPhaseRepository

/**
 * Use case for fetching groups.
 * Contains business logic for group retrieval with pagination support.
 * 
 * @param repository The repository to fetch groups from
 */
open public class FetchGroupsUseCase(
    private val repository: GroupsRepository
) {
    /**
     * Fetches groups based on the provided request.
     * @param request The configured GroupsRequest
     * @return Result containing list of groups or error
     */
    open suspend operator public fun invoke(request: GroupsRequest): Result<List<Group>> {
        return repository.fetchGroups(request)
    }
    
    /**
     * Checks if there are more groups available for pagination.
     * @return true if more groups can be fetched
     */
    open public fun hasMore(): Boolean = repository.hasMoreGroups()

    /**
     * The repository's two-phase read (BEH-SNAP-03), module-internal so the ViewModel can reach it
     * without widening a public signature. Null — today's single-phase path through [invoke] — when
     * the repository does not implement [GroupsTwoPhaseRepository], or when this use case is a subclass
     * (a customer override of [invoke] keeps running).
     */
    internal val twoPhaseRepository: GroupsTwoPhaseRepository?
        get() = if (javaClass == FetchGroupsUseCase::class.java) repository as? GroupsTwoPhaseRepository else null
}
