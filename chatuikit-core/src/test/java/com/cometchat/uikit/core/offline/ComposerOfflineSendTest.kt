package com.cometchat.uikit.core.offline

import android.util.Log
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.usecase.EditMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendCustomMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendMediaMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendTextMessageUseCase
import com.cometchat.uikit.core.state.MessageComposerUIState
import com.cometchat.uikit.core.viewmodel.CometChatMessageComposerViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking

/**
 * The composer on the offline path (G3, G12): pending clears the composer (reply preview gone,
 * send button ready) while the caller's result is still outstanding; a logout or removed-message
 * rejection raises no error. With offline off the composer behaves as today.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ComposerOfflineSendTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var logMock: MockedStatic<Log>
    lateinit var cometChat: MockedStatic<CometChat>

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }
    beforeTest {
        OfflineHub.resetForTest()
        logMock = Mockito.mockStatic(Log::class.java)
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User> { CometChat.getLoggedInUser() }.thenReturn(User("me", "Me"))
    }
    afterTest {
        cometChat.close()
        logMock.close()
        OfflineGate.testOverride = null
    }

    /** A use case whose result the test controls (mockito cannot stub a suspend Result). */
    class FakeTextUseCase(val answer: suspend (TextMessage) -> Result<TextMessage>) :
        SendTextMessageUseCase(mock()) {
        val calls = mutableListOf<TextMessage>()
        override suspend fun invoke(message: TextMessage): Result<TextMessage> {
            calls += message
            return answer(message)
        }
    }

    fun composer(textUseCase: SendTextMessageUseCase): CometChatMessageComposerViewModel =
        CometChatMessageComposerViewModel(
            sendTextMessageUseCase = textUseCase,
            sendMediaMessageUseCase = mock<SendMediaMessageUseCase>(),
            sendCustomMessageUseCase = mock<SendCustomMessageUseCase>(),
            editMessageUseCase = mock<EditMessageUseCase>(),
            enableListeners = false
        ).apply {
            setUser(User().apply { uid = "peer" })
        }

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    test("ON: pending clears the reply preview and readies the composer before the ack") {
        runTest {
            OfflineGate.testOverride = true
            val result = CompletableDeferred<Result<TextMessage>>()
            val useCase = FakeTextUseCase { result.await() }
            val sent = useCase.calls
            val vm = composer(useCase)
            vm.setReplyMessage(textTo("peer", "quoted").apply { id = 11L })
            vm.sendTextMessage("hello")
            advanceUntilIdle()
            vm.uiState.value shouldBe MessageComposerUIState.Sending

            OfflineHub.reportSend(sent.single().apply { localMessageId = "l1" }, MessageSendState.SENDING, null, true)
            advanceUntilIdle()
            vm.replyMessage.value shouldBe null
            vm.uiState.value shouldBe MessageComposerUIState.Idle
            result.complete(Result.success(sent.single()))
            advanceUntilIdle()
        }
    }

    test("ON: a late ack after pending leaves the composer UI alone but sets sentMessage") {
        runTest {
            OfflineGate.testOverride = true
            val result = CompletableDeferred<Result<TextMessage>>()
            val useCase = FakeTextUseCase { result.await() }
            val vm = composer(useCase)
            vm.sendTextMessage("hello")
            advanceUntilIdle()
            OfflineHub.reportSend(useCase.calls.single().apply { localMessageId = "l1" }, MessageSendState.SENDING, null, true)
            advanceUntilIdle()

            // The user starts a new reply while the first message is queued.
            val nextReply = textTo("peer", "next").apply { id = 12L }
            vm.setReplyMessage(nextReply)
            result.complete(Result.success(useCase.calls.single()))
            advanceUntilIdle()
            vm.replyMessage.value shouldBe nextReply
            (vm.uiState.value is MessageComposerUIState.Success) shouldBe false
            vm.sentMessage.value shouldBe useCase.calls.single()
        }
    }

    test("ON: a late final failure after pending raises no composer error") {
        runTest {
            OfflineGate.testOverride = true
            val result = CompletableDeferred<Result<TextMessage>>()
            val useCase = FakeTextUseCase { result.await() }
            val vm = composer(useCase)
            val errors = mutableListOf<CometChatException>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.errorEvent.toList(errors) }
            vm.sendTextMessage("hello")
            advanceUntilIdle()
            OfflineHub.reportSend(useCase.calls.single().apply { localMessageId = "l2" }, MessageSendState.SENDING, null, true)
            advanceUntilIdle()
            result.complete(Result.failure(CometChatException("ERR_SERVER", "5xx")))
            advanceUntilIdle()
            errors.shouldBeEmpty()
            vm.uiState.value shouldBe MessageComposerUIState.Idle
        }
    }

    fun agentComposer(textUseCase: SendTextMessageUseCase): CometChatMessageComposerViewModel =
        CometChatMessageComposerViewModel(
            sendTextMessageUseCase = textUseCase,
            sendMediaMessageUseCase = mock<SendMediaMessageUseCase>(),
            sendCustomMessageUseCase = mock<SendCustomMessageUseCase>(),
            editMessageUseCase = mock<EditMessageUseCase>(),
            enableListeners = false
        ).apply {
            setUser(User().apply {
                uid = "agent"
                role = com.cometchat.uikit.core.constants.UIKitConstants.AIConstants.AGENTIC_USER
            })
        }

    test("ON: new agent chat, two sends, pending seen on both: the second carries the thread parent") {
        runTest {
            OfflineGate.testOverride = true
            val results = mutableListOf<CompletableDeferred<Result<TextMessage>>>()
            val useCase = FakeTextUseCase { CompletableDeferred<Result<TextMessage>>().also { results += it }.await() }
            val vm = agentComposer(useCase)

            vm.sendTextMessage("first")
            advanceUntilIdle()
            val first = useCase.calls[0]
            OfflineHub.reportSend(first.apply { localMessageId = "a1" }, MessageSendState.SENDING, null, true)
            results[0].complete(Result.success(ackOf(first, 501L)))
            advanceUntilIdle()

            vm.sendTextMessage("second")
            advanceUntilIdle()
            useCase.calls[1].parentMessageId shouldBe 501L
            results[1].complete(Result.success(ackOf(useCase.calls[1], 502L)))
            advanceUntilIdle()
        }
    }

    test("ON: a final failure after pending clears AI generating, with no composer error") {
        runTest {
            OfflineGate.testOverride = true
            val result = CompletableDeferred<Result<TextMessage>>()
            val useCase = FakeTextUseCase { result.await() }
            val vm = agentComposer(useCase)
            val errors = mutableListOf<CometChatException>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.errorEvent.toList(errors) }
            vm.sendTextMessage("hello")
            advanceUntilIdle()
            vm.isAIGenerating.value shouldBe true
            OfflineHub.reportSend(useCase.calls.single().apply { localMessageId = "a3" }, MessageSendState.SENDING, null, true)
            result.complete(Result.failure(CometChatException("ERR_SERVER", "5xx")))
            advanceUntilIdle()
            vm.isAIGenerating.value shouldBe false
            errors.shouldBeEmpty()
        }
    }

    test("ON: a quiet rejection (logout / removed) emits no error") {
        runTest {
            OfflineGate.testOverride = true
            val useCase = FakeTextUseCase {
                Result.failure(CometChatException(OfflineSend.ERROR_DISCARDED, "session ended"))
            }
            val vm = composer(useCase)
            val errors = mutableListOf<CometChatException>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.errorEvent.toList(errors) }
            vm.sendTextMessage("hello")
            advanceUntilIdle()
            errors.shouldBeEmpty()
        }
    }

    test("OFF: today's path — the result alone drives the state, and errors surface") {
        runTest {
            OfflineGate.testOverride = false
            val useCase = FakeTextUseCase { Result.failure(CometChatException(OfflineSend.ERROR_DISCARDED, "x")) }
            val vm = composer(useCase)
            val errors = mutableListOf<CometChatException>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.errorEvent.toList(errors) }
            vm.sendTextMessage("hello")
            advanceUntilIdle()
            errors shouldHaveSize 1
            vm.uiState.value.shouldBeInstanceOf<MessageComposerUIState.Error>()
            useCase.calls shouldHaveSize 1
        }
    }
})
