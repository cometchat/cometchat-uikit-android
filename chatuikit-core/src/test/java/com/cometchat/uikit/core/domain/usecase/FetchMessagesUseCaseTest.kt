package com.cometchat.uikit.core.domain.usecase

import com.cometchat.chat.core.MessagesRequest
import com.cometchat.uikit.core.domain.repository.SearchRepository
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests for [FetchMessagesUseCase] (ENG-38677 / L — core logic gaps).
 *
 * The use case is a thin delegation layer over [SearchRepository]: [invoke]
 * forwards a [MessagesRequest] to `getMessages` and returns its [Result]
 * unchanged, while [FetchMessagesUseCase.hasMore] forwards `hasMoreMessages`.
 * We mock the repository and assert the request is forwarded and the result is
 * passed through for both success and failure.
 *
 * Reference: ConversationUseCasesTest.kt
 */
class FetchMessagesUseCaseTest : FunSpec({

    lateinit var repository: SearchRepository
    lateinit var useCase: FetchMessagesUseCase

    beforeTest {
        repository = mock()
        useCase = FetchMessagesUseCase(repository)
    }

    test("invoke delegates to repository.getMessages and returns success unchanged") {
        runTest {
            val request = mock<MessagesRequest>()
            val messages = MockFactory.createMessages(3)
            whenever(repository.getMessages(request)).thenReturn(Result.success(messages))

            val result = useCase(request)

            result.isSuccess shouldBe true
            result.getOrNull() shouldBe messages
            verify(repository).getMessages(request)
        }
    }

    test("invoke propagates Result.failure unchanged") {
        runTest {
            val request = mock<MessagesRequest>()
            val error = MockFactory.createCometChatException("ERR_SEARCH", "Search failed")
            whenever(repository.getMessages(request)).thenReturn(Result.failure(error))

            val result = useCase(request)

            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe error
            verify(repository).getMessages(request)
        }
    }

    test("hasMore returns true when repository has more messages") {
        whenever(repository.hasMoreMessages()).thenReturn(true)

        useCase.hasMore() shouldBe true

        verify(repository).hasMoreMessages()
    }

    test("hasMore returns false when repository is exhausted") {
        whenever(repository.hasMoreMessages()).thenReturn(false)

        useCase.hasMore() shouldBe false

        verify(repository).hasMoreMessages()
    }
})
