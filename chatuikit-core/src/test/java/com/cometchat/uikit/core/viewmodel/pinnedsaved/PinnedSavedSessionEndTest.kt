package com.cometchat.uikit.core.viewmodel.pinnedsaved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.OfflineCacheResetReason
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.offline.OfflineHub
import com.cometchat.uikit.core.offline.OfflineHubEvent
import com.cometchat.uikit.core.state.PinnedSavedListUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatPinnedMessagesViewModel
import com.cometchat.uikit.core.viewmodel.CometChatSavedMessagesViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * ENG-41568 (G8) — the pinned and saved panels clear on a hub `SessionEnded`, and on a `CacheReset`
 * clear and load once more from page one, keeping cached rows quietly if that load fails (D84
 * rule 5). Every case runs against **both** ViewModels.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PinnedSavedSessionEndTest : FunSpec({

    lateinit var dispatcher: TestDispatcher
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var builderConstruction: MockedConstruction<MessagesRequest.MessagesRequestBuilder>
    val stores = mutableListOf<ViewModelStore>()
    // Every fetchNext is held, in call order, so a test answers each page when it chooses.
    val pages = mutableListOf<CometChat.MessageFetchListener>()

    fun messagesOf(vararg ids: Long): List<BaseMessage> = ids.map { MockFactory.createTextMessage(id = it) }

    fun held(): MessagesRequest = mock<MessagesRequest>().also { req ->
        doAnswer { inv -> pages.add(inv.getArgument(0)); null }
            .whenever(req).fetchNext(any<CometChat.MessageFetchListener>())
    }

    class Panel(val load: () -> Unit, val messages: () -> List<BaseMessage>, val state: () -> PinnedSavedListUIState)

    fun <T : ViewModel> inStore(cls: Class<T>, create: () -> T): T {
        val store = ViewModelStore()
        stores += store
        return ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
        })[cls]
    }

    fun panel(kind: String): Panel = if (kind == "pinned") {
        val vm = inStore(CometChatPinnedMessagesViewModel::class.java) { CometChatPinnedMessagesViewModel(enableListeners = true) }
        Panel({ vm.configure(uid = null, guid = "group-1") }, { vm.messages.value }, { vm.uiState.value })
    } else {
        val vm = inStore(CometChatSavedMessagesViewModel::class.java) { CometChatSavedMessagesViewModel(enableListeners = true) }
        Panel({ vm.reload() }, { vm.messages.value }, { vm.uiState.value })
    }

    fun idle() = dispatcher.scheduler.advanceUntilIdle()
    fun ids(p: Panel) = p.messages().map { it.id }

    /** Answers the latest page with [ids], then the next with an empty page (the list ended). */
    fun loaded(vararg ids: Long) {
        pages.last().onServerMessages(messagesOf(*ids))
        idle()
        pages.last().onServerMessages(emptyList())
        idle()
    }

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
        pages.clear()
        builderConstruction = Mockito.mockConstruction(MessagesRequest.MessagesRequestBuilder::class.java) { builder, _ ->
            whenever(builder.setPinned(any())).thenReturn(builder)
            whenever(builder.setSaved(any())).thenReturn(builder)
            whenever(builder.setLimit(any())).thenReturn(builder)
            whenever(builder.setTypes(any())).thenReturn(builder)
            whenever(builder.setCategories(any())).thenReturn(builder)
            whenever(builder.setUID(any())).thenReturn(builder)
            whenever(builder.setGUID(any())).thenReturn(builder)
            whenever(builder.build()).thenAnswer { held() }
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

    listOf("pinned", "saved").forEach { kind ->

        test("$kind: sessionEnded_clearsRowsAndResetsPaging") {
            val p = panel(kind)
            p.load()
            loaded(1, 2)
            ids(p) shouldBe listOf(1L, 2L)
            val built = builderConstruction.constructed().size

            logout()

            p.messages() shouldBe emptyList()
            p.state() shouldBe PinnedSavedListUIState.Loading
            p.load() // a fresh request, from page one
            builderConstruction.constructed().size shouldBe built + 1
            loaded(3)
            ids(p) shouldBe listOf(3L)
        }

        test("$kind: sessionEnded_dropsResultOfReadInFlight") {
            val p = panel(kind)
            p.load()
            val inFlight = pages.last()
            logout()
            inFlight.onCachedMessages(messagesOf(9))
            inFlight.onServerMessages(messagesOf(9))
            idle()
            p.messages() shouldBe emptyList()
            p.state() shouldBe PinnedSavedListUIState.Loading
        }

        test("$kind: staleSessionEnded_isIgnored") {
            val p = panel(kind)
            p.load()
            loaded(1)
            OfflineHub.publishForTest(OfflineHubEvent.SessionEnded(OfflineHub.gen))
            idle()
            ids(p) shouldBe listOf(1L)
            p.state() shouldBe PinnedSavedListUIState.Content
        }

        test("$kind: cacheReset_clearsThenReloadsOnce") {
            val p = panel(kind)
            p.load()
            loaded(1)
            val before = pages.size
            cacheReset()
            p.messages() shouldBe emptyList()
            pages.size shouldBe before + 1
            loaded(2)
            ids(p) shouldBe listOf(2L)
            pages.size shouldBe before + 2 // page one, then the empty page that ends it
        }

        test("$kind: cacheReset_reloadFailureOverRows_isQuiet") {
            val p = panel(kind)
            p.load()
            loaded(1)
            cacheReset()
            pages.last().onCachedMessages(messagesOf(1))
            pages.last().onError(CometChatException("ERR_SERVER", "boom"))
            idle()
            ids(p) shouldBe listOf(1L)
            p.state() shouldBe PinnedSavedListUIState.Content
        }

        test("$kind: offlineOff_noChange") {
            OfflineGate.testOverride = false
            val p = panel(kind)
            p.load()
            loaded(1)
            val before = pages.size
            logout()
            cacheReset()
            ids(p) shouldBe listOf(1L)
            p.state() shouldBe PinnedSavedListUIState.Content
            pages.size shouldBe before
        }
    }
})
