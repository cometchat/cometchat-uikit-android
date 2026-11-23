package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.offline.TwoPhaseResult
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests for [SearchDataSourceImpl] (ENG-38677 / L — `data/datasource` was at 9%; ENG-41133 W7).
 *
 * Every read goes through the SDK 5.1.0 two-phase listeners; the deprecated `CallbackListener`
 * overloads are never called. The single-phase methods return the server phase and
 * `resumeWithException` on error (so the suspend fun throws rather than returning a `Result`); the
 * two-phase methods surface cached → server (or error) as a flow.
 */
class SearchDataSourceImplTest : FunSpec({

    val dataSource = SearchDataSourceImpl()

    test("fetchConversations returns the server phase and never calls the deprecated overload") {
        runTest {
            val request = mock<ConversationsRequest>()
            val cached = listOf(mock<Conversation>())
            val conversations = listOf(mock<Conversation>(), mock<Conversation>())
            whenever(request.fetchNext(any<CometChat.ConversationFetchListener>())).thenAnswer { inv ->
                val l = inv.getArgument<CometChat.ConversationFetchListener>(0)
                l.onCachedConversations(cached)
                l.onServerConversations(conversations)
                null
            }

            dataSource.fetchConversations(request) shouldBe conversations
            verify(request, never()).fetchNext(any<CometChat.CallbackListener<List<Conversation>>>())
        }
    }

    test("fetchConversations throws the SDK exception on error") {
        runTest {
            val request = mock<ConversationsRequest>()
            val error = MockFactory.createCometChatException("ERR_CONV", "boom")
            whenever(request.fetchNext(any<CometChat.ConversationFetchListener>())).thenAnswer { inv ->
                inv.getArgument<CometChat.ConversationFetchListener>(0).onError(error)
                null
            }

            shouldThrow<CometChatException> { dataSource.fetchConversations(request) } shouldBe error
        }
    }

    test("fetchMessages returns the server phase (fetchPrevious) and never calls the deprecated overload") {
        runTest {
            val request = mock<MessagesRequest>()
            val messages = MockFactory.createMessages(2)
            whenever(request.fetchPrevious(any<CometChat.MessageFetchListener>())).thenAnswer { inv ->
                inv.getArgument<CometChat.MessageFetchListener>(0).onServerMessages(messages)
                null
            }

            dataSource.fetchMessages(request) shouldBe messages
            verify(request, never()).fetchPrevious(any<CometChat.CallbackListener<List<BaseMessage>>>())
        }
    }

    test("fetchMessages throws the SDK exception on error") {
        runTest {
            val request = mock<MessagesRequest>()
            val error = MockFactory.createCometChatException("ERR_MSG", "boom")
            whenever(request.fetchPrevious(any<CometChat.MessageFetchListener>())).thenAnswer { inv ->
                inv.getArgument<CometChat.MessageFetchListener>(0).onError(error)
                null
            }

            shouldThrow<CometChatException> { dataSource.fetchMessages(request) } shouldBe error
        }
    }

    test("fetchConversationsTwoPhase emits cached then server") {
        runTest {
            val request = mock<ConversationsRequest>()
            val cached = listOf(mock<Conversation>())
            val server = listOf(mock<Conversation>(), mock<Conversation>())
            whenever(request.fetchNext(any<CometChat.ConversationFetchListener>())).thenAnswer { inv ->
                val l = inv.getArgument<CometChat.ConversationFetchListener>(0)
                l.onCachedConversations(cached)
                l.onServerConversations(server)
                null
            }

            dataSource.fetchConversationsTwoPhase(request).toList() shouldBe
                listOf(TwoPhaseResult.Cached(cached), TwoPhaseResult.Server(server))
        }
    }

    test("fetchMessagesTwoPhase emits cached then the not-connected error, and completes") {
        runTest {
            val request = mock<MessagesRequest>()
            val cached = MockFactory.createMessages(1)
            val error = MockFactory.createCometChatException("ERROR_INTERNET_UNAVAILABLE", "offline")
            whenever(request.fetchPrevious(any<CometChat.MessageFetchListener>())).thenAnswer { inv ->
                val l = inv.getArgument<CometChat.MessageFetchListener>(0)
                l.onCachedMessages(cached)
                l.onError(error)
                null
            }

            dataSource.fetchMessagesTwoPhase(request).toList() shouldBe
                listOf(TwoPhaseResult.Cached(cached), TwoPhaseResult.Error(error))
        }
    }
})
