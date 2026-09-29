package com.cometchat.uikit.core.data.repository

import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.models.Reaction
import com.cometchat.uikit.core.data.datasource.ReactionListDataSource
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests for [ReactionListRepositoryImpl] (ENG-38677 / L — reaction-list vertical).
 *
 * The repository is a thin coordinator over [ReactionListDataSource]. We mock
 * the data source and assert both operations forward their arguments and return
 * the [Result] unchanged for success and failure.
 */
class ReactionListRepositoryImplTest : FunSpec({

    lateinit var dataSource: ReactionListDataSource
    lateinit var repository: ReactionListRepositoryImpl

    beforeTest {
        dataSource = mock()
        repository = ReactionListRepositoryImpl(dataSource)
    }

    test("fetchReactions delegates to the data source and returns success unchanged") {
        runTest {
            val request = mock<ReactionsRequest>()
            val reactions = listOf(mock<Reaction>())
            whenever(dataSource.fetchReactions(request)).thenReturn(Result.success(reactions))

            val result = repository.fetchReactions(request)

            result.getOrNull() shouldBe reactions
            verify(dataSource).fetchReactions(request)
        }
    }

    test("fetchReactions propagates failure unchanged") {
        runTest {
            val request = mock<ReactionsRequest>()
            val error = MockFactory.createCometChatException("ERR", "fail")
            whenever(dataSource.fetchReactions(request)).thenReturn(Result.failure(error))

            val result = repository.fetchReactions(request)

            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe error
        }
    }

    test("removeReaction delegates to the data source and returns success unchanged") {
        runTest {
            val message = MockFactory.createTextMessage()
            whenever(dataSource.removeReaction(1L, "👍")).thenReturn(Result.success(message))

            val result = repository.removeReaction(1L, "👍")

            result.getOrNull() shouldBe message
            verify(dataSource).removeReaction(1L, "👍")
        }
    }

    test("removeReaction propagates failure unchanged") {
        runTest {
            val error = MockFactory.createCometChatException("ERR", "fail")
            whenever(dataSource.removeReaction(2L, "😀")).thenReturn(Result.failure(error))

            val result = repository.removeReaction(2L, "😀")

            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe error
        }
    }
})
