package com.cometchat.uikit.core.data.repository

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Reaction
import com.cometchat.uikit.core.data.datasource.ReactionListDataSource
import com.cometchat.uikit.core.data.datasource.ReactionListDataSourceImpl
import com.cometchat.uikit.core.domain.repository.ReactionListRepository
import com.cometchat.uikit.core.domain.usecase.FetchReactionsUseCase
import com.cometchat.uikit.core.offline.TwoPhaseResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * ENG-41134 — the reactions two-phase seam: data source → repository → use case.
 */
class ReactionListTwoPhaseSeamTest : FunSpec({

    val a = Reaction().apply { setReactionId("a") }
    val b = Reaction().apply { setReactionId("b") }

    test("data source: fetchReactionsTwoPhase emits the cached page, then the server page, on the two-phase overload") {
        runTest {
            val request = mock<ReactionsRequest>()
            whenever(request.fetchNext(any<CometChat.ReactionFetchListener>())).thenAnswer { inv ->
                val listener = inv.getArgument<CometChat.ReactionFetchListener>(0)
                listener.onCachedReactions(listOf(a))
                listener.onServerReactions(listOf(a, b))
                null
            }
            val phases = ReactionListDataSourceImpl().fetchReactionsTwoPhase(request).toList()
            phases shouldBe listOf(TwoPhaseResult.Cached(listOf(a)), TwoPhaseResult.Server(listOf(a, b)))
        }
    }

    test("data source: an error ends the two-phase read") {
        runTest {
            val request = mock<ReactionsRequest>()
            val error = CometChatException("ERR", "boom")
            whenever(request.fetchNext(any<CometChat.ReactionFetchListener>())).thenAnswer { inv ->
                inv.getArgument<CometChat.ReactionFetchListener>(0).onError(error)
                null
            }
            ReactionListDataSourceImpl().fetchReactionsTwoPhase(request).toList() shouldBe
                listOf(TwoPhaseResult.Error(error))
        }
    }

    test("repository: a custom data source without the two-phase seam gets one server phase from today's call") {
        runTest {
            val custom = object : ReactionListDataSource {
                override suspend fun fetchReactions(request: ReactionsRequest) = Result.success(listOf(b))
                override suspend fun removeReaction(messageId: Long, emoji: String): Result<BaseMessage> =
                    Result.failure(CometChatException("ERR", "unused"))
            }
            ReactionListRepositoryImpl(custom).fetchReactionsTwoPhase(mock()).toList() shouldBe
                listOf(TwoPhaseResult.Server(listOf(b)))
        }
    }

    test("use case: twoPhaseOrNull is null for a repository without the two-phase seam") {
        val plainRepo = mock<ReactionListRepository>()
        FetchReactionsUseCase(plainRepo).twoPhaseOrNull(mock()) shouldBe null
    }
})
