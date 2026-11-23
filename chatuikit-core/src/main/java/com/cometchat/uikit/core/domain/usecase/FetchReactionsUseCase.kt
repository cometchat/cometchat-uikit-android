package com.cometchat.uikit.core.domain.usecase

import androidx.annotation.RestrictTo
import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.models.Reaction
import com.cometchat.uikit.core.domain.repository.ReactionListRepository
import com.cometchat.uikit.core.domain.repository.ReactionListTwoPhaseRepository
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.flow.Flow

/**
 * Use case for fetching reactions for a message.
 * Contains business logic for reaction retrieval with pagination support.
 *
 * This use case is used by the CometChatReactionListViewModel to fetch
 * users who reacted to a message with specific emojis.
 *
 * @param repository The repository to fetch reactions from
 */
open public class FetchReactionsUseCase(
    private val repository: ReactionListRepository
) {
    /**
     * Fetches reactions based on the provided request configuration.
     *
     * The request can be configured with:
     * - messageId: The ID of the message to fetch reactions for
     * - reaction: Optional emoji filter to fetch reactions for a specific emoji
     * - limit: Number of reactions to fetch per page (default 10)
     *
     * @param request The configured ReactionsRequest with pagination and filters
     * @return Result containing list of Reaction objects or error
     */
    open suspend operator public fun invoke(request: ReactionsRequest): Result<List<Reaction>> {
        return repository.fetchReactions(request)
    }

    /**
     * The two-phase (cached → server) read for the same [request] (G2), or `null` when this use
     * case cannot offer one — then the caller keeps today's [invoke]. `null` when:
     * - this is a subclass, which may override [invoke] (its override must keep being honoured);
     * - the repository does not implement [ReactionListTwoPhaseRepository].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun twoPhaseOrNull(request: ReactionsRequest): Flow<TwoPhaseResult<Reaction>>? {
        if (javaClass != FetchReactionsUseCase::class.java) return null
        val twoPhase = repository as? ReactionListTwoPhaseRepository ?: return null
        return twoPhase.fetchReactionsTwoPhase(request)
    }
}
