package com.cometchat.uikit.core.domain.repository

import androidx.annotation.RestrictTo
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.flow.Flow

/**
 * Two-phase twin of [SearchRepository] (G2). A **new** interface, so a custom [SearchRepository]
 * that does not implement it keeps today's single-phase path; the default
 * [com.cometchat.uikit.core.data.repository.SearchRepositoryImpl] implements it. `hasMore…` changes
 * only on the server phase (BEH-READ-09b).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface SearchTwoPhaseRepository {
    public fun getConversationsTwoPhase(request: ConversationsRequest): Flow<TwoPhaseResult<Conversation>>
    public fun getMessagesTwoPhase(request: MessagesRequest): Flow<TwoPhaseResult<BaseMessage>>
}
