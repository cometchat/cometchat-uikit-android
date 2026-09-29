package com.cometchat.uikit.compose.presentation.calllogs

import com.cometchat.calls.core.CallLogRequest
import com.cometchat.calls.model.CallLog
import com.cometchat.uikit.core.domain.repository.CallLogsRepository
import com.cometchat.uikit.core.domain.usecase.FetchCallLogsUseCase
import com.cometchat.uikit.core.domain.usecase.InitiateCallUseCase
import com.cometchat.uikit.core.viewmodel.CometChatCallLogsViewModel

/**
 * ENG-38680 — builds a CometChatCallLogsViewModel with a no-op fake repository and
 * listeners disabled, so no real Calls-SDK fetch runs (which would throw inside
 * the compose TestScope). State is driven directly via reflection in the tests.
 */
object CallLogsTestVm {
    fun build(): CometChatCallLogsViewModel {
        val repository = object : CallLogsRepository {
            override suspend fun getCallLogs(request: CallLogRequest): Result<List<CallLog>> = Result.success(emptyList())
            override fun hasMoreCallLogs(): Boolean = false
        }
        return CometChatCallLogsViewModel(
            fetchCallLogsUseCase = FetchCallLogsUseCase(repository),
            initiateCallUseCase = InitiateCallUseCase(),
            enableListeners = false,
        )
    }
}
