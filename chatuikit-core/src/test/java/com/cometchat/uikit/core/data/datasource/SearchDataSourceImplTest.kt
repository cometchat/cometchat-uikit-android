package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for [SearchDataSourceImpl] (ENG-38677 / L — `data/datasource` was at 9%).
 *
 * Unlike the reaction seam, these methods return the raw `List` and
 * `resumeWithException` on error (so the suspend fun throws rather than
 * returning a `Result`). We build the REAL Impl and drive both the success
 * (`onSuccess`) and error (`onError` → thrown) paths for conversations
 * (`fetchNext`) and messages (`fetchPrevious`).
 */
class SearchDataSourceImplTest : FunSpec({

    val dataSource = SearchDataSourceImpl()

    test("fetchConversations returns the SDK list on success") {
        runTest {
            val request = mock<ConversationsRequest>()
            val conversations = listOf(mock<Conversation>(), mock<Conversation>())
            whenever(request.fetchNext(any())).thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<List<Conversation>>>(0).onSuccess(conversations)
                null
            }

            dataSource.fetchConversations(request) shouldBe conversations
        }
    }

    test("fetchConversations throws the SDK exception on error") {
        runTest {
            val request = mock<ConversationsRequest>()
            val error = MockFactory.createCometChatException("ERR_CONV", "boom")
            whenever(request.fetchNext(any())).thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<List<Conversation>>>(0).onError(error)
                null
            }

            shouldThrow<CometChatException> { dataSource.fetchConversations(request) } shouldBe error
        }
    }

    test("fetchMessages returns the SDK list on success (fetchPrevious)") {
        runTest {
            val request = mock<MessagesRequest>()
            val messages = MockFactory.createMessages(2)
            whenever(request.fetchPrevious(any())).thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<List<BaseMessage>>>(0).onSuccess(messages)
                null
            }

            dataSource.fetchMessages(request) shouldBe messages
        }
    }

    test("fetchMessages throws the SDK exception on error") {
        runTest {
            val request = mock<MessagesRequest>()
            val error = MockFactory.createCometChatException("ERR_MSG", "boom")
            whenever(request.fetchPrevious(any())).thenAnswer { inv ->
                inv.getArgument<CometChat.CallbackListener<List<BaseMessage>>>(0).onError(error)
                null
            }

            shouldThrow<CometChatException> { dataSource.fetchMessages(request) } shouldBe error
        }
    }
})
