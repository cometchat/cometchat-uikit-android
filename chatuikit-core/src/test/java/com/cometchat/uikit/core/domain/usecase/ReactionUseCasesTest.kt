package com.cometchat.uikit.core.domain.usecase

import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.models.Reaction
import com.cometchat.uikit.core.domain.repository.ReactionListRepository
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests for [FetchReactionsUseCase] and [RemoveReactionUseCase] (ENG-38677 / L —
 * reaction-list vertical). Both are thin delegation layers over
 * [ReactionListRepository]; we mock the repository and assert forwarding and
 * unchanged [Result] pass-through for success and failure.
 */
class ReactionUseCasesTest : FunSpec({

    context("FetchReactionsUseCase") {
        lateinit var repository: ReactionListRepository
        lateinit var useCase: FetchReactionsUseCase

        beforeTest {
            repository = mock()
            useCase = FetchReactionsUseCase(repository)
        }

        test("invoke delegates to repository.fetchReactions (success)") {
            runTest {
                val request = mock<ReactionsRequest>()
                val reactions = listOf(mock<Reaction>())
                whenever(repository.fetchReactions(request)).thenReturn(Result.success(reactions))

                val result = useCase(request)

                result.getOrNull() shouldBe reactions
                verify(repository).fetchReactions(request)
            }
        }

        test("invoke propagates failure") {
            runTest {
                val request = mock<ReactionsRequest>()
                val error = MockFactory.createCometChatException("ERR", "fail")
                whenever(repository.fetchReactions(request)).thenReturn(Result.failure(error))

                useCase(request).exceptionOrNull() shouldBe error
            }
        }
    }

    context("RemoveReactionUseCase") {
        lateinit var repository: ReactionListRepository
        lateinit var useCase: RemoveReactionUseCase

        beforeTest {
            repository = mock()
            useCase = RemoveReactionUseCase(repository)
        }

        test("invoke delegates to repository.removeReaction (success)") {
            runTest {
                val message = MockFactory.createTextMessage()
                whenever(repository.removeReaction(1L, "👍")).thenReturn(Result.success(message))

                val result = useCase(1L, "👍")

                result.getOrNull() shouldBe message
                verify(repository).removeReaction(1L, "👍")
            }
        }

        test("invoke propagates failure") {
            runTest {
                val error = MockFactory.createCometChatException("ERR", "fail")
                whenever(repository.removeReaction(2L, "😀")).thenReturn(Result.failure(error))

                useCase(2L, "😀").exceptionOrNull() shouldBe error
            }
        }
    }
})
