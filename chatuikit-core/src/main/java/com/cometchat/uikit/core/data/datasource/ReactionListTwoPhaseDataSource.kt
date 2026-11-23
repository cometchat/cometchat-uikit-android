package com.cometchat.uikit.core.data.datasource

import androidx.annotation.RestrictTo
import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.models.Reaction
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.flow.Flow

/**
 * Two-phase (cached → server) reactions fetch (G2, BEH-SNAP-03). A **new** interface — nothing is
 * added to [ReactionListDataSource], and there are no default methods — so a custom data source
 * that does not implement this keeps today's single-phase path. The default
 * [ReactionListDataSourceImpl] implements it; the repository detects support with `is`.
 *
 * Public so a custom data source can implement it, but [RestrictTo] `LIBRARY_GROUP` — internal to
 * the kit, not part of the customer contract.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ReactionListTwoPhaseDataSource {
    /**
     * `request.fetchNext(ReactionFetchListener)` as a flow: [TwoPhaseResult.Cached] (offline on
     * only), then [TwoPhaseResult.Server] or [TwoPhaseResult.Error].
     */
    public fun fetchReactionsTwoPhase(request: ReactionsRequest): Flow<TwoPhaseResult<Reaction>>
}
