package com.cometchat.uikit.core.data.repository

import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.data.datasource.UsersDataSource
import com.cometchat.uikit.core.domain.repository.UsersRepository
import com.cometchat.uikit.core.data.datasource.UsersTwoPhaseDataSource
import com.cometchat.uikit.core.domain.repository.UsersTwoPhaseRepository
import com.cometchat.uikit.core.offline.SnapshotListRead
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * Repository implementation that coordinates data sources for users.
 * Decides where data comes from and handles data transformation.
 * Accepts UsersDataSource interface for flexibility.
 * 
 * @param dataSource The data source to fetch data (interface, not concrete)
 */
public class UsersRepositoryImpl(
    private val dataSource: UsersDataSource
) : UsersRepository, UsersTwoPhaseRepository {
    
    private var hasMore = true

    /**
     * Two-phase twin of [getUsers] (BEH-SNAP-03), same request. Uses the data source's two-phase
     * read when it has one; a custom data source without it gets a single server phase from
     * today's call. `hasMore` changes only on the server phase (BEH-READ-09b).
     */
    override fun getUsersTwoPhase(request: UsersRequest): Flow<TwoPhaseResult<User>> {
        val source = dataSource
        val phases = if (source is UsersTwoPhaseDataSource) {
            source.fetchUsersTwoPhase(request)
        } else {
            SnapshotListRead.serverPhaseOf { dataSource.fetchUsers(request) }
        }
        return phases.onEach { if (it is TwoPhaseResult.Server) hasMore = it.rows.isNotEmpty() }
    }
    
    /**
     * Fetches users from the data source.
     * Tracks pagination state based on results.
     * 
     * @param request The configured UsersRequest
     * @return Result containing list of users or error
     */
    override suspend fun getUsers(request: UsersRequest): Result<List<User>> {
        return try {
            val users = dataSource.fetchUsers(request)
            hasMore = users.isNotEmpty()
            Result.success(users)
        } catch (e: CometChatException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Checks if there are more users available for pagination.
     * @return true if more users can be fetched
     */
    override fun hasMoreUsers(): Boolean = hasMore
}
