package com.cometchat.uikit.core.data.repository

import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.data.datasource.SearchDataSource
import com.cometchat.uikit.core.data.datasource.SearchTwoPhaseDataSource
import com.cometchat.uikit.core.domain.repository.SearchRepository
import com.cometchat.uikit.core.domain.repository.SearchTwoPhaseRepository
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach

/**
 * Repository implementation that coordinates data sources for search functionality.
 * Decides where data comes from and handles data transformation.
 * Accepts SearchDataSource interface for flexibility.
 * 
 * Also implements [SearchTwoPhaseRepository] (G2): the two-phase reads use the data source's
 * [SearchTwoPhaseDataSource] when it has one; a custom data source without it gets a single server
 * phase from today's call. `hasMore…` changes only on the server phase (BEH-READ-09b).
 *
 * @param dataSource The data source to fetch data (interface, not concrete)
 */
public class SearchRepositoryImpl(
    private val dataSource: SearchDataSource
) : SearchRepository, SearchTwoPhaseRepository {
    
    private var hasMoreConversations = true
    private var hasMoreMessages = true

    override fun getConversationsTwoPhase(request: ConversationsRequest): Flow<TwoPhaseResult<Conversation>> {
        val source = dataSource
        val phases = if (source is SearchTwoPhaseDataSource) {
            source.fetchConversationsTwoPhase(request)
        } else {
            singleServerPhase { dataSource.fetchConversations(request) }
        }
        return phases.onEach { if (it is TwoPhaseResult.Server) hasMoreConversations = it.rows.isNotEmpty() }
    }

    override fun getMessagesTwoPhase(request: MessagesRequest): Flow<TwoPhaseResult<BaseMessage>> {
        val source = dataSource
        val phases = if (source is SearchTwoPhaseDataSource) {
            source.fetchMessagesTwoPhase(request)
        } else {
            singleServerPhase { dataSource.fetchMessages(request) }
        }
        return phases.onEach { if (it is TwoPhaseResult.Server) hasMoreMessages = it.rows.isNotEmpty() }
    }

    private fun <T> singleServerPhase(fetch: suspend () -> List<T>): Flow<TwoPhaseResult<T>> = flow {
        val result = try {
            TwoPhaseResult.Server(fetch())
        } catch (e: CometChatException) {
            TwoPhaseResult.Error(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TwoPhaseResult.Error(CometChatException("ERROR", e.message ?: "fetch failed"))
        }
        emit(result)
    }
    
    /**
     * Fetches conversations from the data source based on search criteria.
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
            hasMoreConversations = conversations.isNotEmpty()
            Result.success(conversations)
        } catch (e: CometChatException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Fetches messages from the data source based on search criteria.
     * Tracks pagination state based on results.
     * 
     * @param request The configured MessagesRequest
     * @return Result containing list of messages or error
     */
    override suspend fun getMessages(
        request: MessagesRequest
    ): Result<List<BaseMessage>> {
        return try {
            val messages = dataSource.fetchMessages(request)
            hasMoreMessages = messages.isNotEmpty()
            Result.success(messages)
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
    override fun hasMoreConversations(): Boolean = hasMoreConversations
    
    /**
     * Checks if there are more messages available for pagination.
     * @return true if more messages can be fetched
     */
    override fun hasMoreMessages(): Boolean = hasMoreMessages
}
