package com.cometchat.uikit.core.domain.repository

import androidx.annotation.RestrictTo
import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.models.Reaction
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository-level two-phase reactions read (G2), added the same way as
 * `MessageListTwoPhaseRepository`: nothing is added to [ReactionListRepository], so a custom
 * repository that does not implement this keeps today's single-phase path.
 * [fetchReactionsTwoPhase] ↔ [ReactionListRepository.fetchReactions] (same request, same cursor).
 *
 * Public so a custom repository can implement it, but [RestrictTo] `LIBRARY_GROUP` — internal to
 * the kit, not part of the customer contract.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ReactionListTwoPhaseRepository {
    public fun fetchReactionsTwoPhase(request: ReactionsRequest): Flow<TwoPhaseResult<Reaction>>
}
