package com.cometchat.uikit.core.data.datasource

import androidx.annotation.RestrictTo
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.flow.Flow

/**
 * Two-phase (cached → server) message fetch (G2). A **new** interface — no member is added to the
 * existing [MessageListDataSource], and there are no default methods — so a custom data source that
 * does not implement this keeps today's single-phase path. The default
 * [MessageListDataSourceImpl] implements it; ViewModels detect support with `is`.
 *
 * Public so a custom data source can implement it, but [RestrictTo] `LIBRARY_GROUP]` — internal to
 * the kit, not part of the customer contract.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface MessageListTwoPhaseDataSource {
    public fun fetchPreviousTwoPhase(request: MessagesRequest): Flow<TwoPhaseResult<BaseMessage>>
    public fun fetchNextTwoPhase(request: MessagesRequest): Flow<TwoPhaseResult<BaseMessage>>
}

/**
 * Two-phase (cached → server) conversation fetch (G2). A **new** interface added the same way as
 * [MessageListTwoPhaseDataSource]; the default [ConversationListDataSourceImpl] implements it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ConversationListTwoPhaseDataSource {
    public fun fetchNextTwoPhase(request: ConversationsRequest): Flow<TwoPhaseResult<Conversation>>
}
