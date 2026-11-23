package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * ENG-41135 §3a: the offline-off reads of the message list and the conversation list use the SDK
 * 5.1.0 two-phase overloads and take the server phase only. Offline off, the SDK fires the server
 * phase alone, so each read is one round (one SDK request call) and returns today's page. The
 * deprecated `CallbackListener` overload is never called. A cached phase, which fires only while
 * offline is on, is ignored, exactly as the SDK's own legacy adapter ignores it.
 */
class ListReadServerPhaseBaselineTest : FunSpec({

    test("message list offline-off fetchPrevious: one round, today's page, no deprecated overload") {
        runTest {
            val request = mock<MessagesRequest>()
            val page = MockFactory.createMessages(count = 3, startId = 1L)
            whenever(request.fetchPrevious(any<CometChat.MessageFetchListener>())).thenAnswer {
                it.getArgument<CometChat.MessageFetchListener>(0).onServerMessages(page)
            }

            MessageListDataSourceImpl().fetchPreviousMessages(request) shouldBe page

            verify(request, times(1)).fetchPrevious(any<CometChat.MessageFetchListener>())
            verify(request, never()).fetchPrevious(any<CometChat.CallbackListener<List<BaseMessage>>>())
        }
    }

    test("message list offline-off fetchNext: one round, today's page, no deprecated overload") {
        runTest {
            val request = mock<MessagesRequest>()
            val page = MockFactory.createMessages(count = 2, startId = 10L)
            whenever(request.fetchNext(any<CometChat.MessageFetchListener>())).thenAnswer {
                it.getArgument<CometChat.MessageFetchListener>(0).onServerMessages(page)
            }

            MessageListDataSourceImpl().fetchNextMessages(request) shouldBe page

            verify(request, times(1)).fetchNext(any<CometChat.MessageFetchListener>())
            verify(request, never()).fetchNext(any<CometChat.CallbackListener<List<BaseMessage>>>())
        }
    }

    test("message list single-phase read ignores a cached phase and returns the server page") {
        runTest {
            val request = mock<MessagesRequest>()
            val cached = MockFactory.createMessages(count = 1, startId = 99L)
            val server = MockFactory.createMessages(count = 2, startId = 1L)
            whenever(request.fetchPrevious(any<CometChat.MessageFetchListener>())).thenAnswer {
                val listener = it.getArgument<CometChat.MessageFetchListener>(0)
                listener.onCachedMessages(cached)
                listener.onServerMessages(server)
            }

            MessageListDataSourceImpl().fetchPreviousMessages(request) shouldBe server
        }
    }

    test("conversation list offline-off fetchNext: one round, today's page, no deprecated overload") {
        runTest {
            val request = mock<ConversationsRequest>()
            val page = MockFactory.createUserConversations(3)
            whenever(request.fetchNext(any<CometChat.ConversationFetchListener>())).thenAnswer {
                it.getArgument<CometChat.ConversationFetchListener>(0).onServerConversations(page)
            }

            ConversationListDataSourceImpl().fetchConversations(request) shouldBe page

            verify(request, times(1)).fetchNext(any<CometChat.ConversationFetchListener>())
            verify(request, never()).fetchNext(any<CometChat.CallbackListener<List<Conversation>>>())
        }
    }

    test("conversation list single-phase read ignores a cached phase and returns the server page") {
        runTest {
            val request = mock<ConversationsRequest>()
            val cached = MockFactory.createUserConversations(1)
            val server = MockFactory.createUserConversations(2)
            whenever(request.fetchNext(any<CometChat.ConversationFetchListener>())).thenAnswer {
                val listener = it.getArgument<CometChat.ConversationFetchListener>(0)
                listener.onCachedConversations(cached)
                listener.onServerConversations(server)
            }

            ConversationListDataSourceImpl().fetchConversations(request) shouldBe server
        }
    }
})
