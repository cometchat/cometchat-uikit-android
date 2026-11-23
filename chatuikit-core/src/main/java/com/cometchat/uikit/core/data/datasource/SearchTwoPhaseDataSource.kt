package com.cometchat.uikit.core.data.datasource

import androidx.annotation.RestrictTo
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.flow.Flow

/**
 * Two-phase (cached → server) search reads (G2), added the same way as
 * [ConversationListTwoPhaseDataSource]: a **new** interface, no member added to [SearchDataSource],
 * so a custom search data source that does not implement it keeps today's single-phase path. The
 * default [SearchDataSourceImpl] implements it; the repository detects support with `is`.
 *
 * Public so a custom data source can implement it, but [RestrictTo] `LIBRARY_GROUP` — internal to
 * the kit, not part of the customer contract.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface SearchTwoPhaseDataSource {
    /** Conversations, via `ConversationsRequest.fetchNext(ConversationFetchListener)`. */
    public fun fetchConversationsTwoPhase(request: ConversationsRequest): Flow<TwoPhaseResult<Conversation>>

    /** Messages, via `MessagesRequest.fetchPrevious(MessageFetchListener)`. */
    public fun fetchMessagesTwoPhase(request: MessagesRequest): Flow<TwoPhaseResult<BaseMessage>>
}
