package com.cometchat.uikit.core.offline

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.data.datasource.MessageListDataSource
import com.cometchat.uikit.core.data.datasource.MessageListDataSourceImpl
import com.cometchat.uikit.core.data.datasource.MessageListTwoPhaseDataSource
import com.cometchat.uikit.core.data.repository.MessageListRepositoryImpl
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * ENG-41156 (D60): the cache-allowed reads behind an opening jump. The conversation and target
 * settle once on the server value, else the cached copy; the anchored pages use a request whose
 * message id the kit set (BEH-READ-29) over the configured filters; the server-only reads every
 * other caller uses are unchanged.
 */
class MessageListAnchoredReadsTest : FunSpec({

    val offline = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")

    fun configured(repo: MessageListRepositoryImpl) = repo.apply {
        configureForUser(
            user = MockFactory.createUser(uid = "alice"),
            messagesTypes = listOf(CometChatConstants.MESSAGE_TYPE_TEXT),
            messagesCategories = listOf(CometChatConstants.CATEGORY_MESSAGE),
            parentMessageId = -1L,
            messagesRequestBuilder = null
        )
    }

    // ---- ConversationDetailsSourceOnce ----

    test("the open-path conversation read acts once: cached then server settles on the server copy") {
        val (cached, server) = MockFactory.createUserConversations(2)
        val out = mutableListOf<Result<SettledRead<Conversation>>>()
        ConversationDetailsSourceOnce { out += it }.apply {
            onCachedConversation(cached)
            onServerConversation(server)
            onError(offline)
        }
        out shouldBe listOf(Result.success(SettledRead(server, fromServer = true)))
    }

    test("offline the open-path conversation read falls back to the cached copy, marked as cached") {
        val cached = MockFactory.createUserConversations(1).first()
        val out = mutableListOf<Result<SettledRead<Conversation>>>()
        ConversationDetailsSourceOnce { out += it }.apply {
            onCachedConversation(cached)
            onError(offline)
        }
        out shouldBe listOf(Result.success(SettledRead(cached, fromServer = false)))

        val none = mutableListOf<Result<SettledRead<Conversation>>>()
        ConversationDetailsSourceOnce { none += it }.onError(offline)
        none shouldBe listOf(Result.failure(offline))
    }

    test("online the open-path conversation read never falls back: any other error stands, as today") {
        val cached = MockFactory.createUserConversations(1).first()
        val serverError = CometChatException("ERR_SERVER", "500")
        val out = mutableListOf<Result<SettledRead<Conversation>>>()
        ConversationDetailsSourceOnce { out += it }.apply {
            onCachedConversation(cached)
            onError(serverError)
        }
        out shouldBe listOf(Result.failure(serverError))
    }

    // ---- MessageListDataSourceImpl cache-allowed variants ----

    lateinit var sdk: MockedStatic<CometChat>
    beforeTest { sdk = Mockito.mockStatic(CometChat::class.java) }
    afterTest { sdk.close() }

    test("data source: the open-path variants take the cached copy offline; the server-only reads do not") {
        runTest {
            val conversation = MockFactory.createUserConversations(1).first()
            val message = MockFactory.createTextMessage(id = 5L, text = "cached")
            sdk.`when`<Unit> {
                CometChat.getConversation(eq("u1"), eq("user"), any<CometChat.ConversationDetailsListener>())
            }.thenAnswer {
                it.getArgument<CometChat.ConversationDetailsListener>(2).apply {
                    onCachedConversation(conversation)
                    onError(offline)
                }
                null
            }
            sdk.`when`<Unit> { CometChat.getMessageDetails(eq(5L), any<CometChat.MessageDetailsListener>()) }
                .thenAnswer {
                    it.getArgument<CometChat.MessageDetailsListener>(1).apply {
                        onCachedMessage(message)
                        onError(offline)
                    }
                    null
                }
            val ds = MessageListDataSourceImpl()

            ds.getConversationCachedAllowed("u1", "user") shouldBe SettledRead(conversation, fromServer = false)
            ds.getMessageCachedAllowed(5L) shouldBe message
            runCatching { ds.getConversation("u1", "user") }.exceptionOrNull() shouldBe offline
            runCatching { ds.getMessage(5L) }.exceptionOrNull() shouldBe offline
        }
    }

    test("data source: the target variant falls back only on the not-connected error") {
        runTest {
            val message = MockFactory.createTextMessage(id = 6L, text = "cached")
            val serverError = CometChatException("ERR_SERVER", "500")
            sdk.`when`<Unit> { CometChat.getMessageDetails(eq(6L), any<CometChat.MessageDetailsListener>()) }
                .thenAnswer {
                    it.getArgument<CometChat.MessageDetailsListener>(1).apply {
                        onCachedMessage(message)
                        onError(serverError)
                    }
                    null
                }
            runCatching { MessageListDataSourceImpl().getMessageCachedAllowed(6L) }.exceptionOrNull() shouldBe serverError
        }
    }

    // ---- MessageListRepositoryImpl.anchoredReads ----

    test("repository: anchored halves use a request whose message id is the target, through the two-phase source") {
        runTest {
            val ds = mock<MessageListDataSource>(extraInterfaces = arrayOf(MessageListTwoPhaseDataSource::class))
            val older: BaseMessage = MockFactory.createTextMessage(id = 3L, text = "older")
            val newer: BaseMessage = MockFactory.createTextMessage(id = 5L, text = "newer")
            whenever((ds as MessageListTwoPhaseDataSource).fetchPreviousTwoPhase(any()))
                .thenReturn(flowOf(TwoPhaseResult.Cached(listOf(older)), TwoPhaseResult.Error(offline)))
            whenever(ds.fetchNextTwoPhase(any()))
                .thenReturn(flowOf(TwoPhaseResult.Cached(listOf(newer)), TwoPhaseResult.Error(offline)))
            val repo = configured(MessageListRepositoryImpl(ds))
            val hasMoreBefore = repo.hasMorePreviousMessages()
            val reads = repo.anchoredReads()

            reads.fetchPreviousAnchored(4L).toList() shouldBe
                listOf(TwoPhaseResult.Cached(listOf(older)), TwoPhaseResult.Error(offline))
            reads.fetchNextAnchored(4L).toList() shouldBe
                listOf(TwoPhaseResult.Cached(listOf(newer)), TwoPhaseResult.Error(offline))

            val previous = argumentCaptor<MessagesRequest>()
            val next = argumentCaptor<MessagesRequest>()
            verify(ds as MessageListTwoPhaseDataSource).fetchPreviousTwoPhase(previous.capture())
            verify(ds as MessageListTwoPhaseDataSource).fetchNextTwoPhase(next.capture())
            previous.firstValue.messageId shouldBe 4L
            next.firstValue.messageId shouldBe 4L
            repo.hasMorePreviousMessages() shouldBe hasMoreBefore // the list's own pagination is untouched
            verify(ds, never()).fetchPreviousMessages(any())
        }
    }

    test("repository: a custom data source answers server-only, as today") {
        runTest {
            val conversation = MockFactory.createUserConversations(1).first()
            val target: BaseMessage = MockFactory.createTextMessage(id = 4L, text = "t")
            val rows = MockFactory.createMessages(2)
            val ds = mock<MessageListDataSource>()
            whenever(ds.getConversation(any(), any())).thenReturn(conversation)
            whenever(ds.getMessage(any())).thenReturn(target)
            whenever(ds.fetchPreviousMessages(any())).thenReturn(rows)
            whenever(ds.fetchNextMessages(any())).thenReturn(emptyList())
            val reads = configured(MessageListRepositoryImpl(ds)).anchoredReads()

            reads.getConversation("alice", "user") shouldBe Result.success(SettledRead(conversation, fromServer = true))
            reads.getMessage(4L) shouldBe Result.success(target)
            reads.fetchPreviousAnchored(4L).toList() shouldBe listOf(TwoPhaseResult.Server(rows))
            reads.fetchNextAnchored(4L).toList() shouldBe listOf(TwoPhaseResult.Server(emptyList<BaseMessage>()))
        }
    }

    test("repository: an unconfigured repository reports an error phase, and a failed read is a failure") {
        runTest {
            val ds = mock<MessageListDataSource>()
            whenever(ds.getMessage(any())).thenAnswer { throw offline }
            val reads = MessageListRepositoryImpl(ds).anchoredReads()
            reads.fetchPreviousAnchored(4L).toList().single().shouldBeInstanceOf<TwoPhaseResult.Error>()
            reads.fetchNextAnchored(4L).toList().single().shouldBeInstanceOf<TwoPhaseResult.Error>()
            reads.getMessage(4L).exceptionOrNull() shouldBe offline
        }
    }
})
