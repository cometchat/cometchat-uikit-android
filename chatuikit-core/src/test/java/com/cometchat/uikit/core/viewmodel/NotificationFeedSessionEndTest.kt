package com.cometchat.uikit.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.NotificationCategoriesRequest
import com.cometchat.chat.core.NotificationFeedRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.NotificationFeedItem
import com.cometchat.chat.models.OfflineCacheResetReason
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineHub
import com.cometchat.uikit.core.offline.OfflineHubEvent
import com.cometchat.uikit.core.state.NotificationFeedUIState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * ENG-41568 (G8) — the notification feed clears on a hub `SessionEnded`, and on a `CacheReset`
 * clears and reads the first page once more, quietly over cached rows (D84 rule 5). The feed runs
 * with its listeners on (its first read starts in init); unread-count polling is stopped at once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationFeedSessionEndTest : FunSpec({

    lateinit var dispatcher: TestDispatcher
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var builderConstruction: MockedConstruction<NotificationFeedRequest.NotificationFeedRequestBuilder>
    val stores = mutableListOf<ViewModelStore>()
    // Every fetchNext is held, in call order.
    val reads = mutableListOf<CometChat.NotificationFeedFetchListener>()

    fun items(vararg ids: String): List<NotificationFeedItem> =
        ids.map { id -> NotificationFeedItem().apply { setId(id); setSentAt(1_700_000_000L) } }

    fun vm(): CometChatNotificationFeedViewModel {
        val categories = mock<NotificationCategoriesRequest.NotificationCategoriesRequestBuilder>()
        whenever(categories.build()).thenAnswer { mock<NotificationCategoriesRequest>() }
        val store = ViewModelStore()
        stores += store
        return ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CometChatNotificationFeedViewModel(categoriesRequestBuilder = categories, enableListeners = true) as T
        })[CometChatNotificationFeedViewModel::class.java].also { it.stopUnreadCountPolling() }
    }

    fun ids(vm: CometChatNotificationFeedViewModel) = vm.feedItems.value.map { it.id }
    fun idle() = dispatcher.scheduler.advanceUntilIdle()

    fun logout() {
        OfflineHub.onLogout()
        idle()
    }

    fun cacheReset() {
        OfflineHub.publishForTest(OfflineHubEvent.CacheReset(OfflineHub.gen, OfflineCacheResetReason.CORRUPTED, 0))
        idle()
    }

    beforeTest {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        OfflineGate.testOverride = true
        OfflineHub.resetForTest()
        reads.clear()
        builderConstruction = Mockito.mockConstruction(
            NotificationFeedRequest.NotificationFeedRequestBuilder::class.java
        ) { builder, _ ->
            whenever(builder.setLimit(any())).thenReturn(builder)
            whenever(builder.setCategory(anyOrNull())).thenReturn(builder)
            whenever(builder.setReadState(anyOrNull())).thenReturn(builder)
            whenever(builder.build()).thenAnswer {
                mock<NotificationFeedRequest>().also { req ->
                    doAnswer { inv -> reads.add(inv.getArgument(0)); null }
                        .whenever(req).fetchNext(any<CometChat.NotificationFeedFetchListener>())
                }
            }
        }
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
    }

    afterTest {
        stores.forEach { it.clear() }
        stores.clear()
        OfflineGate.testOverride = null
        cometChatMock.close()
        builderConstruction.close()
        Dispatchers.resetMain()
    }

    test("notificationFeed: sessionEnded_clearsRowsAndResetsPaging") {
        val vm = vm()
        reads.last().onServerNotificationFeedItems(items("a", "b"))
        vm.fetchNextPage()
        reads.last().onServerNotificationFeedItems(emptyList()) // the feed ended
        vm.hasMorePages.value shouldBe false
        val built = builderConstruction.constructed().size

        logout()

        vm.feedItems.value shouldBe emptyList()
        vm.uiState.value shouldBe NotificationFeedUIState.Loading
        vm.hasMorePages.value shouldBe false
        vm.isLoadingMore.value shouldBe false
        vm.fetchNextPage() // no next page until the first page is read again
        reads.size shouldBe 2
        vm.fetchInitialItems() // a fresh request, from page one
        builderConstruction.constructed().size shouldBe built + 1
        reads.size shouldBe 3
        reads.last().onServerNotificationFeedItems(items("c"))
        vm.hasMorePages.value shouldBe true
    }

    test("notificationFeed: sessionEnded_dropsResultOfReadInFlight") {
        val vm = vm()
        val inFlight = reads.last()
        logout()
        inFlight.onCachedNotificationFeedItems(items("old"))
        inFlight.onServerNotificationFeedItems(items("old"))
        vm.feedItems.value shouldBe emptyList()
        vm.uiState.value shouldBe NotificationFeedUIState.Loading
    }

    test("notificationFeed: staleSessionEnded_isIgnored") {
        val vm = vm()
        reads.last().onServerNotificationFeedItems(items("a"))
        OfflineHub.publishForTest(OfflineHubEvent.SessionEnded(OfflineHub.gen))
        idle()
        ids(vm) shouldBe listOf("a")
        vm.uiState.value.shouldBeInstanceOf<NotificationFeedUIState.Content>()
    }

    test("notificationFeed: cacheReset_clearsThenReloadsOnce") {
        val vm = vm()
        reads.last().onServerNotificationFeedItems(items("a", "b"))
        cacheReset()
        vm.feedItems.value shouldBe emptyList()
        reads.size shouldBe 2
        reads.last().onServerNotificationFeedItems(items("c"))
        ids(vm) shouldBe listOf("c")
        reads.size shouldBe 2
    }

    test("notificationFeed: cacheReset_reloadFailureOverRows_isQuiet") {
        val vm = vm()
        val errors = mutableListOf<CometChatException>()
        CoroutineScope(dispatcher).launch { vm.loadErrorWithRows.collect { errors += it } }
        reads.last().onServerNotificationFeedItems(items("a"))
        cacheReset()
        reads.last().onCachedNotificationFeedItems(items("a"))
        reads.last().onError(CometChatException("ERR_SERVER", "boom"))
        ids(vm) shouldBe listOf("a")
        vm.uiState.value.shouldBeInstanceOf<NotificationFeedUIState.Content>()
        errors shouldBe emptyList()
    }

    test("notificationFeed: offlineOff_noChange") {
        OfflineGate.testOverride = false
        val vm = vm()
        reads.last().onServerNotificationFeedItems(items("a"))
        logout()
        cacheReset()
        ids(vm) shouldBe listOf("a")
        vm.uiState.value.shouldBeInstanceOf<NotificationFeedUIState.Content>()
        reads.size shouldBe 1
    }
})
