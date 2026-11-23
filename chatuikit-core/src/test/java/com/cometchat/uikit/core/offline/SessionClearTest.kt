package com.cometchat.uikit.core.offline

import android.util.Log
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.domain.usecase.DeleteConversationUseCase
import com.cometchat.uikit.core.domain.usecase.GetConversationListUseCase
import com.cometchat.uikit.core.domain.usecase.RefreshConversationListUseCase
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Both list ViewModels clear on a hub `SessionEnded` (G8): rows emptied and the list generation
 * bumped (so any op carrying the old gen is dropped — see [OfflineListReconcileTest]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionClearTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var logMock: MockedStatic<Log>
    lateinit var cometChatMock: MockedStatic<CometChat>
    val stores = mutableListOf<androidx.lifecycle.ViewModelStore>()

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }

    beforeTest {
        OfflineGate.testOverride = true
        OfflineHub.resetForTest()
        logMock = Mockito.mockStatic(Log::class.java)
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
    }

    afterTest {
        stores.forEach { it.clear() }
        stores.clear()
        cometChatMock.close()
        logMock.close()
        OfflineGate.testOverride = null
    }

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    test("message list: SessionEnded empties rows and bumps the list gen") {
        runTest {
            // Build model instances BEFORE stubbing — MockFactory.createTextMessage itself mocks a
            // User, which must not run inside another mock's stubbing block.
            val seeded = listOf(MockFactory.createTextMessage(id = 1L, text = "a"))
            val peer = MockFactory.createUser(uid = "peer", name = "Peer")
            val repository = mock<MessageListRepository> {
                on { hasMorePreviousMessages() } doReturn true
                onBlocking { fetchPreviousMessages() } doReturn Result.success(seeded)
                onBlocking { markAsRead(any()) } doReturn Result.success(Unit)
                onBlocking { markAsDelivered(any()) } doReturn Result.success(Unit)
            }

            val store = androidx.lifecycle.ViewModelStore()
            stores += store
            val vm = androidx.lifecycle.ViewModelProvider(
                store,
                object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        CometChatMessageListViewModel(repository = repository, enableListeners = true) as T
                }
            )[CometChatMessageListViewModel::class.java]
            vm.setUser(peer)
            vm.fetchMessages()
            advanceUntilIdle()
            vm.getItems().isEmpty() shouldBe false
            val genBefore = vm.offlineListGen

            OfflineHub.onLogout()
            advanceUntilIdle()

            vm.getItems().shouldBeEmpty()
            vm.offlineListGen shouldBe genBefore + 1
        }
    }

    test("conversation list: SessionEnded empties rows and bumps the list gen") {
        runTest {
            val getUseCase = mock<GetConversationListUseCase>()
            val deleteUseCase = mock<DeleteConversationUseCase>()
            val refreshUseCase = mock<RefreshConversationListUseCase>()
            val convs: List<Conversation> = listOf(MockFactory.createConversation(id = "c1"))
            whenever(getUseCase.invoke(any())).thenReturn(Result.success(convs))
            whenever(getUseCase.hasMore()).thenReturn(false)

            val store = androidx.lifecycle.ViewModelStore()
            stores += store
            val vm = androidx.lifecycle.ViewModelProvider(
                store,
                object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        CometChatConversationsViewModel(getUseCase, deleteUseCase, refreshUseCase, enableListeners = true) as T
                }
            )[CometChatConversationsViewModel::class.java]
            advanceUntilIdle()
            vm.conversations.value.isEmpty() shouldBe false
            val genBefore = vm.offlineListGen

            OfflineHub.onLogout()
            advanceUntilIdle()

            vm.conversations.value.shouldBeEmpty()
            vm.offlineListGen shouldBe genBefore + 1
        }
    }

    // ---- late stale-gen callbacks (invariant 6) ----

    fun boundMessageListVm(): CometChatMessageListViewModel {
        val peer = MockFactory.createUser(uid = "peer", name = "Peer")
        val repository = mock<MessageListRepository> {
            on { hasMorePreviousMessages() } doReturn true
            onBlocking { fetchPreviousMessages() } doReturn Result.success(emptyList())
            onBlocking { markAsRead(any()) } doReturn Result.success(Unit)
            onBlocking { markAsDelivered(any()) } doReturn Result.success(Unit)
        }
        val store = androidx.lifecycle.ViewModelStore()
        stores += store
        val vm = androidx.lifecycle.ViewModelProvider(
            store,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    CometChatMessageListViewModel(repository = repository, enableListeners = true) as T
            }
        )[CometChatMessageListViewModel::class.java]
        vm.setUser(peer) // binds listeners → starts the hub collector
        return vm
    }

    fun boundConversationVm(): CometChatConversationsViewModel {
        val getUseCase = mock<GetConversationListUseCase>()
        runBlocking { whenever(getUseCase.invoke(any())).thenReturn(Result.success(emptyList())) }
        whenever(getUseCase.hasMore()).thenReturn(false)
        val store = androidx.lifecycle.ViewModelStore()
        stores += store
        return androidx.lifecycle.ViewModelProvider(
            store,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    CometChatConversationsViewModel(
                        getUseCase, mock<DeleteConversationUseCase>(), mock<RefreshConversationListUseCase>(),
                        enableListeners = true
                    ) as T
            }
        )[CometChatConversationsViewModel::class.java]
    }

    test("message list: a late callback stamped with the old gen is ignored after SessionEnded") {
        runTest {
            val vm = boundMessageListVm()
            advanceUntilIdle()
            val current = OfflineHubEvent.MessagesUpdated(OfflineHub.gen, emptyList())
            OfflineHub.publishForTest(current)
            advanceUntilIdle()
            vm.lastRoutedHubEvent shouldBe current

            val oldGen = OfflineHub.gen
            OfflineHub.onLogout()
            advanceUntilIdle()
            val ended = vm.lastRoutedHubEvent
            (ended is OfflineHubEvent.SessionEnded) shouldBe true

            OfflineHub.publishForTest(OfflineHubEvent.MessagesUpdated(oldGen, emptyList())) // late, stale
            OfflineHub.publishForTest(OfflineHubEvent.SendState(oldGen, "l1", MockFactory.createTextMessage(id = 0L, text = "x"),
                MessageSendState.SENT, null))
            advanceUntilIdle()
            vm.lastRoutedHubEvent shouldBe ended

            val fresh = OfflineHubEvent.MessagesUpdated(OfflineHub.gen, emptyList())
            OfflineHub.publishForTest(fresh)
            advanceUntilIdle()
            vm.lastRoutedHubEvent shouldBe fresh
        }
    }

    test("conversation list: routes every current-gen event to W3 and ignores a late stale-gen one") {
        runTest {
            val vm = boundConversationVm()
            advanceUntilIdle()
            val sync = OfflineHubEvent.ConversationSyncCompleted(OfflineHub.gen, emptyList())
            OfflineHub.publishForTest(sync)
            advanceUntilIdle()
            vm.lastRoutedHubEvent shouldBe sync

            val oldGen = OfflineHub.gen
            OfflineHub.onLogout()
            advanceUntilIdle()
            (vm.lastRoutedHubEvent is OfflineHubEvent.SessionEnded) shouldBe true // SessionEnded routed too

            OfflineHub.publishForTest(OfflineHubEvent.ConversationSyncCompleted(oldGen, emptyList()))
            advanceUntilIdle()
            (vm.lastRoutedHubEvent is OfflineHubEvent.SessionEnded) shouldBe true
        }
    }
})
