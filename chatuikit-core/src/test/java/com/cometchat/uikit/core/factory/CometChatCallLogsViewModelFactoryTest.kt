package com.cometchat.uikit.core.factory

import com.cometchat.uikit.core.data.datasource.CallLogsDataSource
import com.cometchat.uikit.core.data.repository.CallLogsRepositoryImpl
import com.cometchat.uikit.core.viewmodel.CometChatCallLogsViewModel
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.wheneverBlocking

/**
 * Tests for CometChatCallLogsViewModelFactory.
 * Verifies correct ViewModel creation and error handling.
 *
 * Reference: CometChatConversationsViewModelFactoryTest.kt
 */
class CometChatCallLogsViewModelFactoryTest : FunSpec({

    /**
     * A data source that answers, rather than a bare mock.
     *
     * Creating the view model starts fetchCallLogs() on viewModelScope. An unstubbed suspend mock
     * returns null, CallLogsRepositoryImpl then calls isEmpty() on it, and the resulting
     * NullPointerException lands on a DefaultDispatcher worker with nothing awaiting it. kotlinx
     * coroutines-test attributes such orphaned exceptions to the *next* test that uses runTest,
     * which fails as UncaughtExceptionsBeforeTest somewhere else entirely — this spec was doing
     * that to MessageListFetchWithUnreadPropertyTest and CometChatSearchViewModelStateTransitions-
     * PropertyTest depending on ordering.
     */
    fun answeringDataSource(): CallLogsDataSource = mock<CallLogsDataSource>().also {
        wheneverBlocking { it.fetchCallLogs(any()) }.thenReturn(Result.success(emptyList()))
    }

    test("create should return CometChatCallLogsViewModel for correct class") {
        val mockDataSource = answeringDataSource()
        val repository = CallLogsRepositoryImpl(mockDataSource)
        val factory = CometChatCallLogsViewModelFactory(
            repository = repository,
            enableListeners = false // Disable SDK listeners for testing
        )

        val viewModel = factory.create(CometChatCallLogsViewModel::class.java)

        viewModel.shouldBeInstanceOf<CometChatCallLogsViewModel>()
    }

    test("create should throw IllegalArgumentException for unsupported ViewModel class") {
        val mockDataSource = answeringDataSource()
        val repository = CallLogsRepositoryImpl(mockDataSource)
        val factory = CometChatCallLogsViewModelFactory(
            repository = repository,
            enableListeners = false
        )

        shouldThrow<IllegalArgumentException> {
            factory.create(CometChatConversationsViewModel::class.java)
        }
    }

    test("create should pass custom repository through to ViewModel") {
        val mockDataSource = answeringDataSource()
        val customRepository = CallLogsRepositoryImpl(mockDataSource)
        val factory = CometChatCallLogsViewModelFactory(
            repository = customRepository,
            enableListeners = false
        )

        // Should not throw — custom repository is accepted
        val viewModel = factory.create(CometChatCallLogsViewModel::class.java)
        viewModel.shouldBeInstanceOf<CometChatCallLogsViewModel>()
    }
})
