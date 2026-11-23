package com.cometchat.uikit.core.offline

import android.util.Log
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.domain.usecase.DeleteConversationUseCase
import com.cometchat.uikit.core.domain.usecase.GetConversationListUseCase
import com.cometchat.uikit.core.domain.usecase.RefreshConversationListUseCase
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever

/**
 * Invariant 1: with offline off the kit makes exactly today's SDK calls. Each test uses the real
 * gate (the SDK reports offline off), spies every `CometChat` static, verifies the full call list
 * and then `verifyNoMoreInteractions`. The only SDK call the offline feature adds is the gate's
 * single `isOfflineEnabled()` read per operation; every other call is today's.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineOffBaselineTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var logMock: MockedStatic<Log>
    val stores = mutableListOf<androidx.lifecycle.ViewModelStore>()

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }

    beforeTest {
        OfflineGate.testOverride = null // real gate
        OfflineHub.resetForTest()
        logMock = Mockito.mockStatic(Log::class.java)
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        cometChatMock.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(false)
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

    test("sendTextMessage off → gate read + today's calls, nothing else") {
        val msg = TextMessage("peer", "hi", CometChatConstants.RECEIVER_TYPE_USER)
        CometChatUIKit.sendTextMessage(msg, null)
        cometChatMock.verify({ CometChat.isOfflineEnabled() }, times(1))
        cometChatMock.verify { CometChat.getLoggedInUser() }
        cometChatMock.verify { CometChat.sendMessage(any<TextMessage>(), any<CometChat.CallbackListener<TextMessage>>()) }
        cometChatMock.verifyNoMoreInteractions()
    }

    test("sendMediaMessage off → gate read + today's calls, nothing else") {
        val msg = MediaMessage("peer", "image", CometChatConstants.RECEIVER_TYPE_USER)
        CometChatUIKit.sendMediaMessage(msg, null)
        cometChatMock.verify({ CometChat.isOfflineEnabled() }, times(1))
        cometChatMock.verify { CometChat.getLoggedInUser() }
        cometChatMock.verify { CometChat.sendMediaMessage(any<MediaMessage>(), any<CometChat.CallbackListener<MediaMessage>>()) }
        cometChatMock.verifyNoMoreInteractions()
    }

    test("sendCustomMessage off → gate read + today's calls, nothing else") {
        val msg = CustomMessage("peer", CometChatConstants.RECEIVER_TYPE_USER, "poll", JSONObject())
        CometChatUIKit.sendCustomMessage(msg, null)
        cometChatMock.verify({ CometChat.isOfflineEnabled() }, times(1))
        cometChatMock.verify { CometChat.getLoggedInUser() }
        cometChatMock.verify { CometChat.sendCustomMessage(any<CustomMessage>(), any<CometChat.CallbackListener<CustomMessage>>()) }
        cometChatMock.verifyNoMoreInteractions()
    }

    test("message list ViewModel off → gate read + today's listener registrations, nothing else") {
        runTest {
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
            vm.setUser(peer)
            vm.fetchMessages()
            advanceUntilIdle()

            vm.offlineOn shouldBe false
            cometChatMock.verify({ CometChat.isOfflineEnabled() }, times(1))
            cometChatMock.verify { CometChat.addMessageListener(any(), any()) }
            cometChatMock.verify { CometChat.addGroupListener(any(), any()) }
            // 6.2.0 (Ringing v2): call events come from the Calls SDK listener, not the Chat SDK's
            // deprecated CallListener, so no Chat SDK call listener is registered any more.
            cometChatMock.verify({ CometChat.addCallListener(any(), any()) }, Mockito.never())
            cometChatMock.verify { CometChat.addConnectionListener(any(), any()) }
            cometChatMock.verifyNoMoreInteractions()
        }
    }

    test("conversation list ViewModel off → gate read + today's listener registrations, nothing else") {
        runTest {
            val getUseCase = mock<GetConversationListUseCase>()
            runBlocking { whenever(getUseCase.invoke(any())).thenReturn(Result.success(emptyList())) }
            whenever(getUseCase.hasMore()).thenReturn(false)
            val store = androidx.lifecycle.ViewModelStore()
            stores += store
            val vm = androidx.lifecycle.ViewModelProvider(
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
            advanceUntilIdle()

            vm.offlineOn shouldBe false
            cometChatMock.verify({ CometChat.isOfflineEnabled() }, times(1))
            cometChatMock.verify { CometChat.addConversationListener(any(), any()) }
            cometChatMock.verify { CometChat.addMessageListener(any(), any()) }
            cometChatMock.verify { CometChat.addUserListener(any(), any()) }
            cometChatMock.verify { CometChat.addGroupListener(any(), any()) }
            // 6.2.0 (Ringing v2): no Chat SDK CallListener; see the message list case above.
            cometChatMock.verify({ CometChat.addCallListener(any(), any()) }, Mockito.never())
            cometChatMock.verify { CometChat.addConnectionListener(any(), any()) }
            cometChatMock.verifyNoMoreInteractions()
        }
    }
})
