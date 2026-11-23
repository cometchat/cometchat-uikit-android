package com.cometchat.uikit.core.viewmodel.pinnedsaved

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.state.PinnedSavedListUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatPinnedMessagesViewModel
import com.cometchat.uikit.core.viewmodel.CometChatSavedMessagesViewModel
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * ENG-41134 — pinned and saved panels on the SDK 5.1.0 two-phase `fetchNext(MessageFetchListener)`
 * (BEH-PIN-02, BEH-SAVE-02). Every case runs against **both** ViewModels.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*PinnedSavedOfflineTwoPhaseTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PinnedSavedOfflineTwoPhaseTest : FunSpec({

    isolationMode = IsolationMode.SingleInstance

    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var builderConstruction: MockedConstruction<MessagesRequest.MessagesRequestBuilder>
    lateinit var currentRequest: MessagesRequest
    val connectionListeners = mutableListOf<CometChat.ConnectionListener>()

    val notConnected = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")

    /** One `fetchNext`: an optional cached page, then a server page or an error (or nothing). */
    class Step(
        val cached: List<BaseMessage>? = null,
        val server: List<BaseMessage>? = null,
        val error: CometChatException? = null
    )

    fun messagesOf(vararg ids: Long): List<BaseMessage> =
        ids.map { MockFactory.createTextMessage(id = it, text = "msg-$it") }

    /** A request answering each `fetchNext` with the next step; past the script, an empty server page. */
    fun scripted(vararg steps: Step): MessagesRequest {
        val queue = ArrayDeque(steps.toList())
        val req = mock<MessagesRequest>()
        doAnswer { inv ->
            val listener = inv.getArgument<CometChat.MessageFetchListener>(0)
            val step = queue.removeFirstOrNull() ?: Step(server = emptyList())
            step.cached?.let { listener.onCachedMessages(it) }
            when {
                step.error != null -> listener.onError(step.error)
                step.server != null -> listener.onServerMessages(step.server)
            }
            null
        }.whenever(req).fetchNext(any<CometChat.MessageFetchListener>())
        return req
    }

    /** The two panels behind one shape. */
    class Panel(
        val load: () -> Unit,
        val messages: () -> List<BaseMessage>,
        val state: () -> PinnedSavedListUIState,
        val count: () -> Int,
        val connectToLoad: () -> Boolean
    )

    fun panel(kind: String): Panel = if (kind == "pinned") {
        val vm = CometChatPinnedMessagesViewModel(enableListeners = false)
        Panel(
            { vm.configure(uid = null, guid = "group-1") }, { vm.messages.value }, { vm.uiState.value }, { vm.count.value },
            { vm.connectToLoad.value }
        )
    } else {
        val vm = CometChatSavedMessagesViewModel(enableListeners = false)
        Panel({ vm.reload() }, { vm.messages.value }, { vm.uiState.value }, { vm.count.value }, { vm.connectToLoad.value })
    }

    beforeSpec { Dispatchers.setMain(testDispatcher) }

    afterSpec {
        Thread.sleep(50)
        Dispatchers.resetMain()
    }

    beforeTest {
        currentRequest = scripted()
        builderConstruction = Mockito.mockConstruction(MessagesRequest.MessagesRequestBuilder::class.java) { builder, _ ->
            whenever(builder.setPinned(any())).thenReturn(builder)
            whenever(builder.setSaved(any())).thenReturn(builder)
            whenever(builder.setLimit(any())).thenReturn(builder)
            whenever(builder.setTypes(any())).thenReturn(builder)
            whenever(builder.setCategories(any())).thenReturn(builder)
            whenever(builder.setUID(any())).thenReturn(builder)
            whenever(builder.setGUID(any())).thenReturn(builder)
            whenever(builder.build()).thenAnswer { currentRequest }
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

    test("warmup - absorb leaked exceptions") {
        try { runTest(testDispatcher) { } } catch (_: Exception) { }
    }

    for (kind in listOf("pinned", "saved")) {

        test("$kind: offline on — cached page is the first paint, then the server page reconciles with no duplicate") {
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                var held: CometChat.MessageFetchListener? = null
                val req = mock<MessagesRequest>()
                var call = 0
                doAnswer { inv ->
                    val listener = inv.getArgument<CometChat.MessageFetchListener>(0)
                    call++
                    if (call == 1) {
                        listener.onCachedMessages(messagesOf(1, 2))
                        held = listener
                    } else {
                        listener.onServerMessages(emptyList())
                    }
                    null
                }.whenever(req).fetchNext(any<CometChat.MessageFetchListener>())
                currentRequest = req

                val p = panel(kind)
                p.load()
                p.messages().map { it.id } shouldBe listOf(1L, 2L)
                p.state() shouldBe PinnedSavedListUIState.Content

                held!!.onServerMessages(messagesOf(2, 1, 3))
                p.messages().map { it.id } shouldBe listOf(2L, 1L, 3L) // server order, no duplicate
                p.count() shouldBe 3
                p.state() shouldBe PinnedSavedListUIState.Content
            }
        }

        test("$kind: offline on — an empty cached page does not end the list") {
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                currentRequest = scripted(
                    Step(cached = emptyList(), server = messagesOf(1, 2)),
                    Step(cached = emptyList(), server = messagesOf(3))
                )
                val p = panel(kind)
                p.load()
                p.messages().map { it.id } shouldBe listOf(1L, 2L, 3L)
                verify(currentRequest, times(3)).fetchNext(any<CometChat.MessageFetchListener>())
            }
        }

        test("$kind: offline on — load more never repeats a row and keeps server order") {
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                currentRequest = scripted(
                    Step(cached = messagesOf(5, 4), server = messagesOf(5, 4)),
                    Step(cached = messagesOf(4, 3), server = messagesOf(4, 3, 2))
                )
                val p = panel(kind)
                p.load()
                p.messages().map { it.id } shouldBe listOf(5L, 4L, 3L, 2L)
                p.count() shouldBe 4
            }
        }

        test("$kind: offline on — loadAll ends on the server's hasNext and drops cached rows it never returned") {
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                currentRequest = scripted(
                    Step(cached = messagesOf(1, 9), server = messagesOf(1)),
                    Step(cached = emptyList(), server = emptyList())
                )
                val p = panel(kind)
                p.load()
                p.messages().map { it.id } shouldBe listOf(1L)
                p.state() shouldBe PinnedSavedListUIState.Content
                verify(currentRequest, times(2)).fetchNext(any<CometChat.MessageFetchListener>())
            }
        }

        test("$kind: offline on — not-connected keeps the cached rows (no error screen) and reads the next cached page") {
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                currentRequest = scripted(
                    Step(cached = messagesOf(1, 2), error = notConnected),
                    Step(cached = messagesOf(3), error = notConnected),
                    Step(cached = emptyList(), error = notConnected)
                )
                val p = panel(kind)
                p.load()
                p.messages().map { it.id } shouldBe listOf(1L, 2L, 3L)
                p.count() shouldBe 3
                p.state() shouldBe PinnedSavedListUIState.Content
                verify(currentRequest, times(3)).fetchNext(any<CometChat.MessageFetchListener>())
            }
        }

        test("$kind: D84 — offline on, nothing cached and not connected shows the offline state") {
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                currentRequest = scripted(Step(cached = emptyList(), error = notConnected))
                val p = panel(kind)
                p.load()
                p.state() shouldBe PinnedSavedListUIState.Error(notConnected)
                p.connectToLoad() shouldBe true // D84 rule 3: the offline state
                p.messages() shouldBe emptyList()
                verify(currentRequest, times(1)).fetchNext(any<CometChat.MessageFetchListener>())
            }
        }

        test("$kind: D83 — offline on, the server phase returns nothing: the empty view") {
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                currentRequest = scripted(Step(cached = messagesOf(1), server = emptyList()))
                val p = panel(kind)
                p.load()
                p.messages() shouldBe emptyList()
                p.state() shouldBe PinnedSavedListUIState.Empty
                connectionListeners shouldBe emptyList()
            }
        }

        test("$kind: D83 — offline on, the error view because offline reads exactly once on reconnect") {
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                currentRequest = scripted(Step(cached = emptyList(), error = notConnected))
                val p = panel(kind)
                p.load()
                p.state().shouldBeInstanceOf<PinnedSavedListUIState.Error>()

                currentRequest = scripted(Step(cached = emptyList(), server = messagesOf(5)))
                connectionListeners.toList().forEach { it.onConnected() }
                p.messages().map { it.id } shouldBe listOf(5L)
                p.state() shouldBe PinnedSavedListUIState.Content
                // A second reconnect reads nothing more: one load is the page plus the empty end page.
                connectionListeners.toList().forEach { it.onConnected() }
                verify(currentRequest, times(2)).fetchNext(any<CometChat.MessageFetchListener>())
            }
        }

        test("$kind: offline on — rows shown, the reconnect reads nothing (ENG-41462)") {
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                currentRequest = scripted(
                    Step(cached = messagesOf(1), error = notConnected),
                    Step(cached = emptyList(), error = notConnected)
                )
                val p = panel(kind)
                p.load()
                p.state() shouldBe PinnedSavedListUIState.Content

                val after = scripted(Step(cached = emptyList(), server = messagesOf(9)))
                currentRequest = after
                connectionListeners.toList().forEach { it.onConnected() }
                p.messages().map { it.id } shouldBe listOf(1L)
                verify(after, times(0)).fetchNext(any<CometChat.MessageFetchListener>())
            }
        }

        test("$kind: offline on — a reconnect after offline was turned off reads nothing (ENG-41462)") {
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                currentRequest = scripted(Step(cached = emptyList(), error = notConnected))
                val p = panel(kind)
                p.load()
                OfflineGate.testOverride = false
                val after = scripted(Step(server = messagesOf(9)))
                currentRequest = after
                connectionListeners.toList().forEach { it.onConnected() }
                verify(after, times(0)).fetchNext(any<CometChat.MessageFetchListener>())
            }
        }

        test("$kind: offline off — not-connected is today's error and registers no reconnect read (ENG-41462)") {
            runTest(testDispatcher) {
                OfflineGate.testOverride = false
                currentRequest = scripted(Step(error = notConnected))
                val p = panel(kind)
                p.load()
                p.state().shouldBeInstanceOf<PinnedSavedListUIState.Error>()
                connectionListeners shouldBe emptyList()
            }
        }

        test("$kind: offline on — another error with nothing shown is the error view, no reconnect read") {
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                val boom = CometChatException("ERR_SERVER", "boom")
                currentRequest = scripted(Step(cached = emptyList(), error = boom))
                val p = panel(kind)
                p.load()
                p.state().shouldBeInstanceOf<PinnedSavedListUIState.Error>()
                (p.state() as PinnedSavedListUIState.Error).exception shouldBe boom
                p.connectToLoad() shouldBe false // D84 rule 4: the error view
                connectionListeners shouldBe emptyList()
            }
        }

        test("$kind: offline on — a stale generation is dropped") {
            runTest(testDispatcher) {
                OfflineGate.testOverride = true
                val held = mutableListOf<CometChat.MessageFetchListener>()
                val stalled = mock<MessagesRequest>()
                doAnswer { inv -> held.add(inv.getArgument(0)); null }
                    .whenever(stalled).fetchNext(any<CometChat.MessageFetchListener>())
                currentRequest = stalled

                val p = panel(kind)
                p.load()
                currentRequest = scripted(Step(cached = emptyList(), server = messagesOf(7)))
                p.load()
                p.messages().map { it.id } shouldBe listOf(7L)

                held.forEach {
                    it.onCachedMessages(messagesOf(1, 2))
                    it.onServerMessages(messagesOf(1, 2))
                }
                p.messages().map { it.id } shouldBe listOf(7L)
                p.count() shouldBe 1
            }
        }

        test("$kind: offline off — the cached phase is ignored and the output equals today's") {
            runTest(testDispatcher) {
                OfflineGate.testOverride = false
                currentRequest = scripted(Step(cached = messagesOf(99), server = messagesOf(1, 2)))
                val p = panel(kind)
                p.load()
                p.messages().map { it.id } shouldBe listOf(1L, 2L)
                p.state() shouldBe PinnedSavedListUIState.Content

                currentRequest = scripted(Step(cached = messagesOf(99), error = notConnected))
                p.load()
                p.state().shouldBeInstanceOf<PinnedSavedListUIState.Error>()
                p.messages() shouldBe emptyList()
            }
        }
    }
})
