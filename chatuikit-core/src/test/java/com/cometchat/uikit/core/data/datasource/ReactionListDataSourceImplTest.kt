package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Reaction
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for [ReactionListDataSourceImpl] (ENG-38677 / L — reaction-list vertical;
 * `data/datasource` was at 9%).
 *
 * The Impl bridges the CometChat SDK callback API to suspend/`Result`. We build
 * the REAL Impl (not a stand-in) and drive both SDK seams:
 *  - `fetchReactions` → `request.fetchNext(callback)` (instance),
 *  - `removeReaction` → `CometChat.removeReaction(id, emoji, callback)` (static, via mockStatic),
 * exercising both the onSuccess and onError continuations.
 *
 * Tests are flat (no `context {}`) so Kotest's `beforeTest` fires once per leaf
 * — a container-level `beforeTest` would register the static mock twice and
 * Mockito rejects a second registration on the same thread.
 */
class ReactionListDataSourceImplTest : FunSpec({

    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var dataSource: ReactionListDataSourceImpl

    beforeTest {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        dataSource = ReactionListDataSourceImpl()
    }
    afterTest {
        cometChatMock.close()
    }

    test("fetchReactions resumes with success when the request invokes onSuccess") {
        runTest {
            val request = mock<ReactionsRequest>()
            val reactions = listOf(mock<Reaction>(), mock<Reaction>())
            whenever(request.fetchNext(any())).thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<List<Reaction>>>(0).onSuccess(reactions)
                null
            }

            val result = dataSource.fetchReactions(request)

            result.isSuccess shouldBe true
            result.getOrNull() shouldBe reactions
        }
    }

    test("fetchReactions resumes with failure when the request invokes onError") {
        runTest {
            val request = mock<ReactionsRequest>()
            val error = MockFactory.createCometChatException("ERR_FETCH", "boom")
            whenever(request.fetchNext(any())).thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<List<Reaction>>>(0).onError(error)
                null
            }

            val result = dataSource.fetchReactions(request)

            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe error
        }
    }

    test("removeReaction resumes with success when the SDK invokes onSuccess") {
        runTest {
            val message = MockFactory.createTextMessage()
            cometChatMock.`when`<Unit> {
                CometChat.removeReaction(eq(42L), eq("👍"), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<BaseMessage>>(2).onSuccess(message)
                null
            }

            val result = dataSource.removeReaction(42L, "👍")

            result.isSuccess shouldBe true
            result.getOrNull() shouldBe message
        }
    }

    test("removeReaction resumes with failure when the SDK invokes onError") {
        runTest {
            val error = MockFactory.createCometChatException("ERR_REMOVE", "nope")
            cometChatMock.`when`<Unit> {
                CometChat.removeReaction(eq(7L), eq("😀"), any())
            }.thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<BaseMessage>>(2).onError(error)
                null
            }

            val result = dataSource.removeReaction(7L, "😀")

            result.isFailure shouldBe true
            result.exceptionOrNull() shouldBe error
        }
    }
})
