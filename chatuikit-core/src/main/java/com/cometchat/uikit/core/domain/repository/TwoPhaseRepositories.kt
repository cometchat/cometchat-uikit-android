package com.cometchat.uikit.core.domain.repository

import androidx.annotation.RestrictTo
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository-level two-phase message read (G2). A **new** interface — nothing is added to the
 * existing [MessageListRepository], and there are no default methods — so a custom repository that
 * does not implement this keeps today's single-phase path. A ViewModel offers the two-phase read
 * only when `repository is MessageListTwoPhaseRepository`.
 *
 * Each method takes the same inputs as its single-phase twin and keeps the same pagination state:
 * [fetchPreviousTwoPhase] ↔ [MessageListRepository.fetchPreviousMessages] (older page from the
 * configured request; `hasMorePreviousMessages` is updated only by the **server** phase),
 * [fetchNextTwoPhase] ↔ [MessageListRepository.fetchNextMessages] (newer page from `fromMessageId`).
 *
 * Public so a custom repository can implement it, but [RestrictTo] `LIBRARY_GROUP` — internal to
 * the kit, not part of the customer contract.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface MessageListTwoPhaseRepository {
    public fun fetchPreviousTwoPhase(): Flow<TwoPhaseResult<BaseMessage>>
    public fun fetchNextTwoPhase(fromMessageId: Long): Flow<TwoPhaseResult<BaseMessage>>
}

/**
 * Repository-level two-phase conversation read (G2), added the same way as
 * [MessageListTwoPhaseRepository]. [fetchNextTwoPhase] ↔ [ConversationListRepository.getConversations]
 * (same request; `hasMoreConversations` is updated only by the **server** phase).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ConversationListTwoPhaseRepository {
    public fun fetchNextTwoPhase(request: ConversationsRequest): Flow<TwoPhaseResult<Conversation>>
}
