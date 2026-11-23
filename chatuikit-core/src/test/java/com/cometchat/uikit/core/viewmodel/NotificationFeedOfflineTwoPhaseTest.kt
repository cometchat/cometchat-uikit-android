package com.cometchat.uikit.core.viewmodel

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.NotificationFeedRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.NotificationFeedItem
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.state.NotificationFeedUIState
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
 * ENG-41134 — notification feed on the SDK 5.1.0 two-phase
 * `fetchNext(NotificationFeedFetchListener)` (BEH-SNAP-03, README §3.9). The categories fetch is
 * not deprecated and is not exercised here.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*NotificationFeedOfflineTwoPhaseTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationFeedOfflineTwoPhaseTest : FunSpec({

    isolationMode = IsolationMode.SingleInstance

    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var builderConstruction: MockedConstruction<NotificationFeedRequest.NotificationFeedRequestBuilder>
    val listeners = mutableListOf<CometChat.NotificationFeedFetchListener>()
    val connectionListeners = mutableListOf<CometChat.ConnectionListener>()

    val notConnected = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")

    fun items(vararg ids: String): List<NotificationFeedItem> =
        ids.map { id -> NotificationFeedItem().apply { setId(id); setSentAt(1_700_000_000L) } }

    fun ids(vm: CometChatNotificationFeedViewModel) = vm.feedItems.value.map { it.id }

    beforeSpec { Dispatchers.setMain(testDispatcher) }

    afterSpec {
        Thread.sleep(50)
        Dispatchers.resetMain()
    }

    beforeTest {
        listeners.clear()
        // Every request built holds each fetchNext so the test drives the phases explicitly.
        builderConstruction = Mockito.mockConstruction(
            NotificationFeedRequest.NotificationFeedRequestBuilder::class.java
        ) { builder, _ ->
            whenever(builder.setLimit(any())).thenReturn(builder)
            whenever(builder.setCategory(anyOrNull())).thenReturn(builder)
            whenever(builder.setReadState(anyOrNull())).thenReturn(builder)
            whenever(builder.build()).thenAnswer {
                mock<NotificationFeedRequest>().also { req ->
                    doAnswer { inv -> listeners.add(inv.getArgument(0)); null }
                        .whenever(req).fetchNext(any<CometChat.NotificationFeedFetchListener>())
                }
            }
        }
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        connectionListeners.clear()
        cometChatMock.`when`<Unit> { CometChat.addConnectionListener(any(), any()) }
            .thenAnswer { connectionListeners += it.getArgument<CometChat.ConnectionListener>(1); null }
    }

    afterTest {
        OfflineGate.testOverride = null
        cometChatMock.close()
        builderConstruction.close()
    }

    fun newVm() = CometChatNotificationFeedViewModel(enableListeners = false)

    test("warmup - absorb leaked exceptions") {
        try { runTest(testDispatcher) { } } catch (_: Exception) { }
    }

    test("offline on — cached page is the first paint, then the server page reconciles with no duplicate") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            vm.fetchInitialItems()
            listeners.last().onCachedNotificationFeedItems(items("a", "b"))
            ids(vm) shouldBe listOf("a", "b")
            vm.uiState.value.shouldBeInstanceOf<NotificationFeedUIState.Content>()

            listeners.last().onServerNotificationFeedItems(items("a", "b", "c"))
            ids(vm) shouldBe listOf("a", "b", "c")

            vm.fetchNextPage()
            listeners.last().onCachedNotificationFeedItems(items("c", "d"))
            ids(vm) shouldBe listOf("a", "b", "c", "d")
            listeners.last().onServerNotificationFeedItems(items("c", "e"))
            ids(vm) shouldBe listOf("a", "b", "c", "e") // d proven gone, c not repeated
            vm.isLoadingMore.value shouldBe false
        }
    }

    test("offline on — an empty cached page does not end the list; only an empty server page does") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            vm.fetchInitialItems()
            listeners.last().onServerNotificationFeedItems(items("a"))

            vm.fetchNextPage()
            listeners.last().onCachedNotificationFeedItems(emptyList())
            vm.hasMorePages.value shouldBe true
            listeners.last().onServerNotificationFeedItems(items("b"))
            vm.hasMorePages.value shouldBe true

            vm.fetchNextPage()
            listeners.last().onCachedNotificationFeedItems(emptyList())
            listeners.last().onServerNotificationFeedItems(emptyList())
            vm.hasMorePages.value shouldBe false
            ids(vm) shouldBe listOf("a", "b")
        }
    }

    test("offline on — not-connected keeps the rows shown (no error screen over them)") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            vm.fetchInitialItems()
            listeners.last().onCachedNotificationFeedItems(items("a", "b"))
            listeners.last().onError(notConnected)
            ids(vm) shouldBe listOf("a", "b")
            vm.uiState.value.shouldBeInstanceOf<NotificationFeedUIState.Content>()

            vm.fetchNextPage()
            listeners.last().onCachedNotificationFeedItems(items("c"))
            listeners.last().onError(notConnected)
            ids(vm) shouldBe listOf("a", "b", "c")
            vm.uiState.value.shouldBeInstanceOf<NotificationFeedUIState.Content>()
            vm.isLoadingMore.value shouldBe false
        }
    }

    test("D84 rule 3 — offline on, nothing cached and not connected shows the offline state") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            vm.fetchInitialItems()
            listeners.last().onCachedNotificationFeedItems(emptyList())
            listeners.last().onError(notConnected)
            vm.uiState.value shouldBe NotificationFeedUIState.Error(notConnected)
            vm.connectToLoad.value shouldBe true
        }
    }

    test("D84 rule 4 — offline on, an online error with nothing cached is the error view, not the offline state") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            val boom = CometChatException("ERR_SERVER", "boom")
            vm.fetchInitialItems()
            listeners.last().onCachedNotificationFeedItems(emptyList())
            listeners.last().onError(boom)
            vm.uiState.value shouldBe NotificationFeedUIState.Error(boom)
            vm.connectToLoad.value shouldBe false
        }
    }

    test("D84 rule 2 — offline on, an offline error with cached rows keeps them and reports nothing") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            val reported = mutableListOf<CometChatException>()
            val job = launch { vm.loadErrorWithRows.collect { reported += it } }
            vm.fetchInitialItems()
            listeners.last().onCachedNotificationFeedItems(items("a"))
            listeners.last().onError(notConnected)
            ids(vm) shouldBe listOf("a")
            vm.uiState.value.shouldBeInstanceOf<NotificationFeedUIState.Content>()
            reported shouldBe emptyList()
            job.cancel()
        }
    }

    test("D84 rule 5 — offline on, an online error with cached rows keeps them and still reports to onError") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            val reported = mutableListOf<CometChatException>()
            val job = launch { vm.loadErrorWithRows.collect { reported += it } }
            val boom = CometChatException("ERR_SERVER", "boom")
            vm.fetchInitialItems()
            listeners.last().onCachedNotificationFeedItems(items("a"))
            listeners.last().onError(boom)
            ids(vm) shouldBe listOf("a")
            reported shouldBe listOf(boom)
            job.cancel()
        }
    }

    test("D84 — offline on, an online error with rows raised before the view collects arrives exactly once") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            val boom = CometChatException("ERR_SERVER", "boom")
            vm.fetchInitialItems()
            listeners.last().onCachedNotificationFeedItems(items("a"))
            listeners.last().onError(boom)

            val first = mutableListOf<CometChatException>()
            val job1 = launch { vm.loadErrorWithRows.collect { first += it } }
            first shouldBe listOf(boom)
            job1.cancel()
            // A new collector (rotation / recomposition) gets no repeat.
            val second = mutableListOf<CometChatException>()
            val job2 = launch { vm.loadErrorWithRows.collect { second += it } }
            second shouldBe emptyList()
            job2.cancel()
        }
    }

    test("D83 — offline on, the server phase returns nothing: the empty view") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            vm.fetchInitialItems()
            listeners.last().onCachedNotificationFeedItems(items("a"))
            listeners.last().onServerNotificationFeedItems(emptyList())
            ids(vm) shouldBe emptyList()
            vm.uiState.value shouldBe NotificationFeedUIState.Empty
            connectionListeners shouldBe emptyList()
        }
    }

    test("D83 — offline on, the error view because offline reads exactly once on reconnect") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            vm.fetchInitialItems()
            listeners.last().onCachedNotificationFeedItems(emptyList())
            listeners.last().onError(notConnected)
            vm.uiState.value.shouldBeInstanceOf<NotificationFeedUIState.Error>()
            listeners.size shouldBe 1

            connectionListeners.toList().forEach { it.onConnected() }
            listeners.size shouldBe 2
            listeners.last().onServerNotificationFeedItems(items("a"))
            ids(vm) shouldBe listOf("a")
            vm.uiState.value.shouldBeInstanceOf<NotificationFeedUIState.Content>()

            connectionListeners.toList().forEach { it.onConnected() }
            listeners.size shouldBe 2
        }
    }

    test("offline on — rows shown, the reconnect reads nothing (ENG-41462)") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            vm.fetchInitialItems()
            listeners.last().onCachedNotificationFeedItems(items("a"))
            listeners.last().onError(notConnected)
            connectionListeners.toList().forEach { it.onConnected() }
            listeners.size shouldBe 1
            ids(vm) shouldBe listOf("a")
        }
    }

    test("offline on — empty offline, a live item arrives before the reconnect: no read (ENG-41462)") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            vm.fetchInitialItems()
            listeners.last().onError(notConnected)
            vm.onNewFeedItemReceived(items("z").first())
            connectionListeners.toList().forEach { it.onConnected() }
            listeners.size shouldBe 1
        }
    }

    test("offline off — not-connected is today's error and registers no reconnect read (ENG-41462)") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = false
            val vm = newVm()
            vm.fetchInitialItems()
            listeners.last().onError(notConnected)
            vm.uiState.value.shouldBeInstanceOf<NotificationFeedUIState.Error>()
            connectionListeners shouldBe emptyList()
        }
    }

    test("offline on — another error with nothing shown is the error view, no reconnect read") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            vm.fetchInitialItems()
            listeners.last().onError(CometChatException("ERR_SERVER", "boom"))
            vm.uiState.value.shouldBeInstanceOf<NotificationFeedUIState.Error>()
            connectionListeners shouldBe emptyList()
        }
    }

    test("D83 — offline off, a refresh error over rows is today's error state and reports nothing extra") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = false
            val vm = newVm()
            val reported = mutableListOf<CometChatException>()
            val job = launch { vm.loadErrorWithRows.collect { reported += it } }
            vm.fetchInitialItems()
            listeners.last().onServerNotificationFeedItems(items("a"))
            vm.refresh()
            listeners.last().onError(notConnected)
            vm.uiState.value shouldBe NotificationFeedUIState.Error(notConnected)
            reported shouldBe emptyList()
            connectionListeners shouldBe emptyList()
            job.cancel()
        }
    }

    test("offline on — a stale generation is dropped (a page landing after a refresh)") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            vm.fetchInitialItems()
            listeners.last().onServerNotificationFeedItems(items("a"))

            vm.fetchNextPage()
            val stale = listeners.last()
            vm.refresh()
            listeners.last().onServerNotificationFeedItems(items("n"))
            ids(vm) shouldBe listOf("n")

            stale.onCachedNotificationFeedItems(items("old1"))
            stale.onServerNotificationFeedItems(items("old2"))
            ids(vm) shouldBe listOf("n")
            vm.isLoadingMore.value shouldBe false
        }
    }

    test("offline off — the cached phase is ignored and the output equals today's") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = false
            val vm = newVm()
            vm.fetchInitialItems()
            listeners.last().onCachedNotificationFeedItems(items("x"))
            ids(vm) shouldBe emptyList()
            vm.uiState.value shouldBe NotificationFeedUIState.Loading

            listeners.last().onServerNotificationFeedItems(items("a"))
            ids(vm) shouldBe listOf("a")

            vm.fetchNextPage()
            listeners.last().onCachedNotificationFeedItems(items("y"))
            listeners.last().onServerNotificationFeedItems(items("b"))
            ids(vm) shouldBe listOf("a", "b")

            vm.refresh()
            listeners.last().onError(notConnected)
            vm.uiState.value.shouldBeInstanceOf<NotificationFeedUIState.Error>()
        }
    }

    // ---- D83 amended 2026-11-12 ("keep it quiet"): the read the kit starts on reconnect.

    test("D83 amended — the reconnect read failing over cached rows keeps them and reports nothing") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            val reported = mutableListOf<CometChatException>()
            val job = launch { vm.loadErrorWithRows.collect { reported += it } }
            vm.fetchInitialItems()
            listeners.last().onCachedNotificationFeedItems(emptyList())
            listeners.last().onError(notConnected)
            vm.uiState.value.shouldBeInstanceOf<NotificationFeedUIState.Error>()

            connectionListeners.toList().forEach { it.onConnected() }
            listeners.size shouldBe 2
            listeners.last().onCachedNotificationFeedItems(items("a"))
            listeners.last().onError(notConnected)
            ids(vm) shouldBe listOf("a")
            vm.uiState.value.shouldBeInstanceOf<NotificationFeedUIState.Content>()
            reported shouldBe emptyList()

            // The app's own load still reports an online error.
            val boom = CometChatException("ERR_SERVER", "boom")
            vm.fetchInitialItems()
            listeners.last().onCachedNotificationFeedItems(items("a"))
            listeners.last().onError(boom)
            reported shouldBe listOf(boom)
            job.cancel()
        }
    }

    test("D83 amended — the reconnect read failing with nothing shown is the error view again") {
        runTest(testDispatcher) {
            OfflineGate.testOverride = true
            val vm = newVm()
            vm.fetchInitialItems()
            listeners.last().onCachedNotificationFeedItems(emptyList())
            listeners.last().onError(notConnected)

            connectionListeners.toList().forEach { it.onConnected() }
            listeners.size shouldBe 2
            val other = CometChatException("ERR_SERVER", "boom")
            listeners.last().onError(other)
            vm.uiState.value shouldBe NotificationFeedUIState.Error(other)
        }
    }
})
