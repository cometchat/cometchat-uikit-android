package com.cometchat.uikit.core.data.repository

import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Reaction
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.uikit.core.data.datasource.ReactionListDataSource
import com.cometchat.uikit.core.data.datasource.ReactionListTwoPhaseDataSource
import com.cometchat.uikit.core.domain.repository.ReactionListRepository
import com.cometchat.uikit.core.domain.repository.ReactionListTwoPhaseRepository
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository implementation that coordinates data sources for reaction list.
 * Decides where data comes from and handles data transformation.
 * Accepts ReactionListDataSource interface for flexibility.
 *
 * @param dataSource The data source to fetch data (interface, not concrete)
 */
public class ReactionListRepositoryImpl(
    private val dataSource: ReactionListDataSource
) : ReactionListRepository, ReactionListTwoPhaseRepository {

    /**
     * Fetches reactions from the data source.
     *
     * @param request The configured ReactionsRequest with pagination and filters
     * @return Result containing list of Reaction objects or error
     */
    override suspend fun fetchReactions(request: ReactionsRequest): Result<List<Reaction>> {
        return dataSource.fetchReactions(request)
    }

    /**
     * Two-phase twin of [fetchReactions] (G2). Uses the data source's two-phase read when it has
     * one; a custom data source without it gets a single server phase from today's call.
     */
    override fun fetchReactionsTwoPhase(request: ReactionsRequest): Flow<TwoPhaseResult<Reaction>> {
        val source = dataSource
        if (source is ReactionListTwoPhaseDataSource) return source.fetchReactionsTwoPhase(request)
        return flow {
            val result = dataSource.fetchReactions(request)
            emit(
                result.fold(
                    onSuccess = { TwoPhaseResult.Server(it) },
                    onFailure = { e ->
                        TwoPhaseResult.Error(
                            e as? CometChatException ?: CometChatException("ERROR", e.message ?: "fetch failed")
                        )
                    }
                )
            )
        }
    }

    /**
     * Removes a reaction from a message via the data source.
     *
     * @param messageId The ID of the message to remove reaction from
     * @param emoji The emoji reaction to remove
     * @return Result containing the updated BaseMessage or error
     */
    override suspend fun removeReaction(messageId: Long, emoji: String): Result<BaseMessage> {
        return dataSource.removeReaction(messageId, emoji)
    }
}
