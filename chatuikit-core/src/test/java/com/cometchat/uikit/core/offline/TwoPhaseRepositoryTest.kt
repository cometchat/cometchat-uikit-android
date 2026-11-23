package com.cometchat.uikit.core.offline

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.data.datasource.ConversationListDataSource
import com.cometchat.uikit.core.data.datasource.ConversationListTwoPhaseDataSource
import com.cometchat.uikit.core.data.datasource.MessageListDataSource
import com.cometchat.uikit.core.data.datasource.MessageListTwoPhaseDataSource
import com.cometchat.uikit.core.data.repository.ConversationListRepositoryImpl
import com.cometchat.uikit.core.data.repository.MessageListRepositoryImpl
import com.cometchat.uikit.core.domain.repository.ConversationListRepository
import com.cometchat.uikit.core.domain.usecase.GetConversationListUseCase
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * The default repositories implement the two-phase interfaces with the same inputs as their
 * single-phase twins (§2.5): they delegate to a two-phase data source, fall back to one server
 * phase for a custom data source, and change `hasMore` only on the server phase (BEH-READ-09b).
 */
class TwoPhaseRepositoryTest : FunSpec({

    fun configured(repo: MessageListRepositoryImpl) = repo.configureForUser(
        user = MockFactory.createUser(uid = "alice"),
        messagesTypes = listOf(CometChatConstants.MESSAGE_TYPE_TEXT),
        messagesCategories = listOf(CometChatConstants.CATEGORY_MESSAGE),
        parentMessageId = -1L,
        messagesRequestBuilder = null
    )

    test("message repository delegates to a two-phase data source; hasMore changes only on the server phase") {
        runTest {
            val cachedRow: BaseMessage = MockFactory.createTextMessage(id = 1L, text = "a")
            val ds = mock<MessageListDataSource>(extraInterfaces = arrayOf(MessageListTwoPhaseDataSource::class))
            whenever((ds as MessageListTwoPhaseDataSource).fetchPreviousTwoPhase(any()))
                .thenReturn(flowOf(TwoPhaseResult.Cached(listOf(cachedRow)), TwoPhaseResult.Server(emptyList())))
            val repo = MessageListRepositoryImpl(ds)
            configured(repo)

            val hasMoreAfterEach = mutableListOf<Boolean>()
            val phases = repo.fetchPreviousTwoPhase().onEach { hasMoreAfterEach += repo.hasMorePreviousMessages() }.toList()

            phases shouldBe listOf(TwoPhaseResult.Cached(listOf(cachedRow)), TwoPhaseResult.Server(emptyList()))
            hasMoreAfterEach shouldBe listOf(true, false) // a cached page never ends history; an empty server page does
        }
    }

    test("message repository fetchNextTwoPhase builds from fromMessageId and delegates") {
        runTest {
            val ds = mock<MessageListDataSource>(extraInterfaces = arrayOf(MessageListTwoPhaseDataSource::class))
            whenever((ds as MessageListTwoPhaseDataSource).fetchNextTwoPhase(any()))
                .thenReturn(flowOf(TwoPhaseResult.Server(emptyList())))
            val repo = MessageListRepositoryImpl(ds)
            configured(repo)
            repo.fetchNextTwoPhase(42L).toList() shouldBe listOf(TwoPhaseResult.Server(emptyList()))
            verify(ds as MessageListTwoPhaseDataSource).fetchNextTwoPhase(any())
        }
    }

    test("message repository falls back to one server phase for a custom data source") {
        runTest {
            val rows = MockFactory.createMessages(2)
            val ds = mock<MessageListDataSource>()
            whenever(ds.fetchPreviousMessages(any())).thenReturn(rows)
            val repo = MessageListRepositoryImpl(ds)
            configured(repo)
            repo.fetchPreviousTwoPhase().toList() shouldBe listOf(TwoPhaseResult.Server(rows))
        }
    }

    test("message repository reports an error phase when not configured") {
        runTest {
            val phases = MessageListRepositoryImpl(mock()).fetchPreviousTwoPhase().toList()
            phases.single().shouldBeInstanceOf<TwoPhaseResult.Error>()
        }
    }

    test("conversation repository delegates with the same request; hasMore changes only on the server phase") {
        runTest {
            val conv = mock<Conversation>()
            val request = mock<ConversationsRequest>()
            val ds = mock<ConversationListDataSource>(extraInterfaces = arrayOf(ConversationListTwoPhaseDataSource::class))
            whenever((ds as ConversationListTwoPhaseDataSource).fetchNextTwoPhase(request))
                .thenReturn(flowOf(TwoPhaseResult.Cached(listOf(conv)), TwoPhaseResult.Server(emptyList())))
            val repo = ConversationListRepositoryImpl(ds)

            val hasMoreAfterEach = mutableListOf<Boolean>()
            val phases = repo.fetchNextTwoPhase(request).onEach { hasMoreAfterEach += repo.hasMoreConversations() }.toList()

            phases shouldBe listOf(TwoPhaseResult.Cached(listOf(conv)), TwoPhaseResult.Server(emptyList()))
            hasMoreAfterEach shouldBe listOf(true, false)
        }
    }

    test("conversation repository falls back to one server phase for a custom data source") {
        runTest {
            val conv = mock<Conversation>()
            val request = mock<ConversationsRequest>()
            val ds = mock<ConversationListDataSource>()
            whenever(ds.fetchConversations(request)).thenReturn(listOf(conv))
            ConversationListRepositoryImpl(ds).fetchNextTwoPhase(request).toList() shouldBe
                listOf(TwoPhaseResult.Server(listOf(conv)))
        }
    }

    test("the conversation ViewModel's path: the use case exposes the default repository's two-phase read only") {
        GetConversationListUseCase(ConversationListRepositoryImpl(mock())).twoPhaseRepository shouldNotBe null
        GetConversationListUseCase(mock<ConversationListRepository>()).twoPhaseRepository shouldBe null
    }
})
