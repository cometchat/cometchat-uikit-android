package com.cometchat.uikit.core.data.repository

import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.data.datasource.ConversationListDataSource
import com.cometchat.uikit.core.data.datasource.ConversationListTwoPhaseDataSource
import com.cometchat.uikit.core.domain.repository.ConversationListRepository
import com.cometchat.uikit.core.domain.repository.ConversationListTwoPhaseRepository
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

/**
 * Repository implementation that coordinates data sources.
 * Decides where data comes from and handles data transformation.
 * Accepts ConversationListDataSource interface for flexibility.
 * 
 * @param dataSource The data source to fetch/save data (interface, not concrete)
 */
public class ConversationListRepositoryImpl(
    private val dataSource: ConversationListDataSource
) : ConversationListRepository, ConversationListTwoPhaseRepository {

    private var hasMore = true

    /**
     * Two-phase twin of [getConversations] (G2), same request. Uses the data source's two-phase
     * read when it has one; a custom data source without it gets a single server phase from
     * today's call. `hasMore` changes only on the server phase (BEH-READ-09b).
     */
    override fun fetchNextTwoPhase(request: ConversationsRequest): Flow<TwoPhaseResult<Conversation>> {
        val source = dataSource
        val phases = if (source is ConversationListTwoPhaseDataSource) {
            source.fetchNextTwoPhase(request)
        } else {
            flow {
                val result = try {
                    TwoPhaseResult.Server(dataSource.fetchConversations(request))
                } catch (e: CometChatException) {
                    TwoPhaseResult.Error(e)
                } catch (e: Exception) {
                    TwoPhaseResult.Error(CometChatException("ERROR", e.message ?: "fetch failed"))
                }
                emit(result)
            }
        }
        return phases.onEach { if (it is TwoPhaseResult.Server) hasMore = it.rows.isNotEmpty() }
    }

    /**
     * Fetches conversations from the data source.
     * Tracks pagination state based on results.
     * 
     * @param request The configured ConversationsRequest
     * @return Result containing list of conversations or error
     */
    override suspend fun getConversations(
        request: ConversationsRequest
    ): Result<List<Conversation>> {
        return try {
            val conversations = dataSource.fetchConversations(request)
            hasMore = conversations.isNotEmpty()
            Result.success(conversations)
        } catch (e: CometChatException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Deletes a conversation via the data source.
     * 
     * @param conversationWith The ID of the user or group
     * @param conversationType The type of conversation (user/group)
     * @return Result indicating success or failure
     */
    override suspend fun deleteConversation(
        conversationWith: String,
        conversationType: String
    ): Result<Unit> {
        return try {
            dataSource.deleteConversation(conversationWith, conversationType)
            Result.success(Unit)
        } catch (e: CometChatException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Marks a conversation as delivered by marking its last message as delivered.
     * 
     * @param conversation The conversation to mark as delivered
     * @return Result indicating success or failure
     */
    override suspend fun markAsDelivered(conversation: Conversation): Result<Unit> {
        return try {
            conversation.lastMessage?.let { message ->
                dataSource.markAsDelivered(message)
            }
            Result.success(Unit)
        } catch (e: CometChatException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Checks if there are more conversations available for pagination.
     * @return true if more conversations can be fetched
     */
    override fun hasMoreConversations(): Boolean = hasMore
}
