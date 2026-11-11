package com.cometchat.uikit.core.data.repository

import com.cometchat.chat.core.Call
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.data.datasource.CallButtonsDataSource
import com.cometchat.uikit.core.domain.repository.CallButtonsRepository

/**
 * Implementation of [CallButtonsRepository] that delegates to [CallButtonsDataSource].
 * Provides call initiation and active call detection functionality.
 */
public class CallButtonsRepositoryImpl(
    private val dataSource: CallButtonsDataSource
) : CallButtonsRepository {

    override suspend fun initiateUserCall(receiverId: String, callType: String, receiver: User?): Result<Call> {
        return dataSource.initiateUserCall(receiverId, callType, receiver)
    }

    override suspend fun startGroupCall(groupId: String, callType: String): Result<CustomMessage> {
        return dataSource.sendGroupCallMessage(groupId, callType)
    }

    override fun hasActiveCall(): Boolean {
        return dataSource.hasActiveCallSession() ||
                dataSource.getActiveCall() != null ||
                dataSource.getActiveCallingExtensionCall() != null ||
                dataSource.isActiveMeeting()
    }
}
