package com.cometchat.uikit.core.domain.usecase

import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.uikit.core.domain.repository.SearchRepository
import com.cometchat.uikit.core.domain.repository.SearchTwoPhaseRepository

/**
 * Use case for fetching messages in search context.
 * Contains business logic for message retrieval with pagination support.
 * 
 * @param repository The search repository to fetch messages from
 */
open public class FetchMessagesUseCase(
    private val repository: SearchRepository
) {
    /**
     * Fetches messages based on the provided request.
     * @param request The configured MessagesRequest
     * @return Result containing list of messages or error
     */
    open suspend operator public fun invoke(
        request: MessagesRequest
    ): Result<List<BaseMessage>> {
        return repository.getMessages(request)
    }
    
    /**
     * Checks if there are more messages available for pagination.
     * @return true if more messages can be fetched
     */
    open public fun hasMore(): Boolean = repository.hasMoreMessages()

    /**
     * The repository's two-phase search read, when it has one (G2) — module-internal so the search
     * ViewModel can reach it without widening any public signature. Null keeps today's single-phase
     * path: for a custom repository without [SearchTwoPhaseRepository], and for a subclass of this
     * use case (which may override [invoke] — the ViewModel must keep calling it).
     */
    internal val twoPhaseRepository: SearchTwoPhaseRepository?
        get() = if (this::class == FetchMessagesUseCase::class) repository as? SearchTwoPhaseRepository else null
}
