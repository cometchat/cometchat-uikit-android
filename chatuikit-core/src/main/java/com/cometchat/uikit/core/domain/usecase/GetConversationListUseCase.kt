package com.cometchat.uikit.core.domain.usecase

import com.cometchat.chat.models.Conversation
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.uikit.core.domain.repository.ConversationListRepository
import com.cometchat.uikit.core.domain.repository.ConversationListTwoPhaseRepository

/**
 * Use case for fetching conversations.
 * Contains business logic for conversation retrieval with pagination support.
 * 
 * @param repository The repository to fetch conversations from
 */
open public class GetConversationListUseCase(
    private val repository: ConversationListRepository
) {
    /**
     * Fetches conversations based on the provided request.
     * @param request The configured ConversationsRequest
     * @return Result containing list of conversations or error
     */
    open suspend operator public fun invoke(
        request: ConversationsRequest
    ): Result<List<Conversation>> {
        return repository.getConversations(request)
    }
    
    /**
     * Checks if there are more conversations available for pagination.
     * @return true if more conversations can be fetched
     */
    open public fun hasMore(): Boolean = repository.hasMoreConversations()

    /**
     * The repository's two-phase read, when it has one (G2) — module-internal so the conversation
     * ViewModel can reach it without widening any public signature. Null for a custom repository
     * that does not implement [ConversationListTwoPhaseRepository]: today's single-phase path.
     */
    internal val twoPhaseRepository: ConversationListTwoPhaseRepository?
        get() = repository as? ConversationListTwoPhaseRepository
}
