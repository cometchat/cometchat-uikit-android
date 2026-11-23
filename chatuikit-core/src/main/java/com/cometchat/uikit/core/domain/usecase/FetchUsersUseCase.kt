package com.cometchat.uikit.core.domain.usecase

import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.UsersRepository
import com.cometchat.uikit.core.domain.repository.UsersTwoPhaseRepository

/**
 * Use case for fetching users.
 * Contains business logic for user retrieval with pagination support.
 * 
 * @param repository The repository to fetch users from
 */
open public class FetchUsersUseCase(
    private val repository: UsersRepository
) {
    /**
     * Fetches users based on the provided request.
     * @param request The configured UsersRequest
     * @return Result containing list of users or error
     */
    open suspend operator public fun invoke(request: UsersRequest): Result<List<User>> {
        return repository.getUsers(request)
    }
    
    /**
     * Checks if there are more users available for pagination.
     * @return true if more users can be fetched
     */
    open public fun hasMore(): Boolean = repository.hasMoreUsers()

    /**
     * The repository's two-phase read (BEH-SNAP-03), module-internal so the ViewModel can reach it
     * without widening a public signature. Null — today's single-phase path through [invoke] — when
     * the repository does not implement [UsersTwoPhaseRepository], or when this use case is a subclass
     * (a customer override of [invoke] keeps running).
     */
    internal val twoPhaseRepository: UsersTwoPhaseRepository?
        get() = if (javaClass == FetchUsersUseCase::class.java) repository as? UsersTwoPhaseRepository else null
}
