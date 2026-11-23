package com.cometchat.uikit.core.domain.usecase

import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.UsersRepository
import com.cometchat.uikit.core.domain.repository.UsersTwoPhaseRepository

/**
 * Use case for searching users.
 * Contains business logic for user search with keyword filtering.
 * 
 * @param repository The repository to search users from
 */
open public class SearchUsersUseCase(
    private val repository: UsersRepository
) {
    /**
     * Searches users based on the provided keyword and request builder.
     * @param searchKeyword The keyword to search for
     * @param builder The UsersRequestBuilder to configure the search
     * @return Result containing list of matching users or error
     */
    open suspend operator public fun invoke(
        searchKeyword: String,
        builder: UsersRequest.UsersRequestBuilder
    ): Result<List<User>> {
        val request = builder.setSearchKeyword(searchKeyword).build()
        return repository.getUsers(request)
    }

    /**
     * The repository's two-phase read (BEH-SNAP-03), module-internal so the ViewModel can reach it
     * without widening a public signature. Null — today's single-phase path through [invoke] — when
     * the repository does not implement [UsersTwoPhaseRepository], or when this use case is a subclass
     * (a customer override of [invoke] keeps running).
     */
    internal val twoPhaseRepository: UsersTwoPhaseRepository?
        get() = if (javaClass == SearchUsersUseCase::class.java) repository as? UsersTwoPhaseRepository else null
}
