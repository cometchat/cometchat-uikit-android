package com.cometchat.uikit.core.data.repository

import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.GroupMember
import com.cometchat.uikit.core.data.datasource.GroupMembersDataSource
import com.cometchat.uikit.core.domain.repository.GroupMembersRepository
import com.cometchat.uikit.core.data.datasource.GroupMembersTwoPhaseDataSource
import com.cometchat.uikit.core.domain.repository.GroupMembersTwoPhaseRepository
import com.cometchat.uikit.core.offline.SnapshotListRead
import com.cometchat.uikit.core.offline.TwoPhaseResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository implementation that coordinates data sources for group members.
 * Decides where data comes from and handles data transformation.
 * Accepts GroupMembersDataSource interface for flexibility.
 * 
 * @param dataSource The data source to fetch/save data (interface, not concrete)
 */
public class GroupMembersRepositoryImpl(
    private val dataSource: GroupMembersDataSource
) : GroupMembersRepository, GroupMembersTwoPhaseRepository {

    /**
     * Two-phase twin of [fetchGroupMembers] (BEH-SNAP-03), same inputs and paging request. Uses
     * the data source's two-phase read when it has one; a custom data source without it gets a
     * single server phase from today's call. Pagination state stays in the data source, which
     * changes it only on the server phase (BEH-READ-09b).
     */
    override fun fetchGroupMembersTwoPhase(
        guid: String,
        limit: Int,
        searchKeyword: String?
    ): Flow<TwoPhaseResult<GroupMember>> {
        val source = dataSource
        return if (source is GroupMembersTwoPhaseDataSource) {
            source.fetchGroupMembersTwoPhase(guid, limit, searchKeyword)
        } else {
            SnapshotListRead.serverPhaseOf { dataSource.fetchGroupMembers(guid, limit, searchKeyword) }
        }
    }
    
    /**
     * Fetches group members from the data source.
     * 
     * @param guid The group ID
     * @param limit Number of members to fetch per page
     * @param searchKeyword Optional search keyword to filter members
     * @return Result containing list of group members or error
     */
    override suspend fun fetchGroupMembers(
        guid: String,
        limit: Int,
        searchKeyword: String?
    ): Result<List<GroupMember>> {
        return try {
            val members = dataSource.fetchGroupMembers(guid, limit, searchKeyword)
            Result.success(members)
        } catch (e: CometChatException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Kicks a member from the group via the data source.
     * 
     * @param guid The group ID
     * @param uid The user ID to kick
     * @return Result indicating success or failure
     */
    override suspend fun kickMember(
        guid: String,
        uid: String
    ): Result<Unit> {
        return try {
            dataSource.kickGroupMember(guid, uid)
            Result.success(Unit)
        } catch (e: CometChatException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Bans a member from the group via the data source.
     * 
     * @param guid The group ID
     * @param uid The user ID to ban
     * @return Result indicating success or failure
     */
    override suspend fun banMember(
        guid: String,
        uid: String
    ): Result<Unit> {
        return try {
            dataSource.banGroupMember(guid, uid)
            Result.success(Unit)
        } catch (e: CometChatException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Changes a member's scope in the group via the data source.
     * 
     * @param guid The group ID
     * @param uid The user ID
     * @param scope The new scope (admin/moderator/participant)
     * @return Result indicating success or failure
     */
    override suspend fun changeMemberScope(
        guid: String,
        uid: String,
        scope: String
    ): Result<Unit> {
        return try {
            dataSource.changeMemberScope(guid, uid, scope)
            Result.success(Unit)
        } catch (e: CometChatException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Checks if there are more members available for pagination.
     * @return true if more members can be fetched
     */
    override fun hasMore(): Boolean = dataSource.hasMoreMembers()
    
    /**
     * Resets the current request so the next fetch builds a fresh one.
     */
    override fun resetRequest(): Unit = dataSource.resetRequest()
}
