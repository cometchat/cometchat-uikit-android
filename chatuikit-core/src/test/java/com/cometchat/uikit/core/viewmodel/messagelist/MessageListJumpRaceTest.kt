package com.cometchat.uikit.core.viewmodel.messagelist

import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.core.domain.model.SurroundingMessagesResult
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.state.MessageListUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

/**
 * ENG-41551 on the single-phase (offline off) path of the message list:
 * D: a page read started before a jump never lands in the jumped window, a page asked for while
 * the jump is in flight is not read (the window would replace it), and `clear()` starts a new window.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageListJumpRaceTest : FunSpec({

    isolationMode = IsolationMode.SingleInstance

    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var repository: MessageListRepository

    fun msg(id: Long): BaseMessage =
        MockFactory.createTextMessage(id = id, senderUid = "user-1", receiverId = "user-2", sentAt = 1_700_000_000L + id)

    fun window(target: Long) = SurroundingMessagesResult(
        olderMessages = listOf(msg(target - 1)),
        targetMessage = msg(target),
        newerMessages = listOf(msg(target + 1)),
        hasMorePrevious = true,
        hasMoreNext = true
    )

    fun vm() = CometChatMessageListViewModel(repository = repository, enableListeners = false)

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }

    beforeTest {
        repository = mock()
        whenever(repository.hasMorePreviousMessages()).thenReturn(true)
    }
    afterTest { Thread.sleep(50) }

    test("D (offline off): a stale older page that completes after the jump is ignored") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val first = listOf(msg(90), msg(91))
            val stale = listOf(msg(80), msg(81))
            val target = msg(5)
            val jumped = window(5)
            whenever(repository.fetchPreviousMessages()).thenReturn(Result.success(first))
            whenever(repository.getMessage(5L)).thenReturn(Result.success(target))
            whenever(repository.fetchSurroundingMessages(5L)).thenReturn(Result.success(jumped))
            val vm = vm()
            vm.fetchMessages()
            advanceUntilIdle()
            vm.messages.value.map { it.id } shouldBe listOf(90L, 91L)

            whenever(repository.fetchPreviousMessages()).doSuspendableAnswer {
                gate.await(); Result.success(stale)
            }
            vm.fetchMessages() // older page in flight
            vm.goToMessage(5L)
            advanceUntilIdle()
            vm.messages.value.map { it.id } shouldBe listOf(4L, 5L, 6L)

            gate.complete(Unit) // the stale page answers after the jump
            advanceUntilIdle()

            vm.messages.value.map { it.id } shouldBe listOf(4L, 5L, 6L)
            vm.uiState.value shouldBe MessageListUIState.Loaded
            vm.isInProgress.value shouldBe false
        }
    }

    test("D review (offline off): a jump fails while an older page is in flight: the cursor goes back on the oldest row, no gap") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val first = listOf(msg(90), msg(91))
            val stale = listOf(msg(88), msg(89))
            val boom = CometChatException("ERR", "boom")
            whenever(repository.fetchPreviousMessages()).thenReturn(Result.success(first))
            whenever(repository.getMessage(5L)).thenReturn(Result.failure(boom))
            val vm = vm()
            vm.fetchMessages()
            advanceUntilIdle()

            whenever(repository.fetchPreviousMessages()).doSuspendableAnswer { gate.await(); Result.success(stale) }
            vm.fetchMessages() // scrolled up: an older page in flight
            vm.goToMessage(5L) // the jump fails; the rows stay
            advanceUntilIdle()
            gate.complete(Unit) // the dropped page had already advanced the shared cursor
            advanceUntilIdle()

            vm.messages.value.map { it.id } shouldBe listOf(90L, 91L)
            org.mockito.kotlin.verify(repository).rebuildRequestFromMessageId(90L)

            org.mockito.kotlin.doReturn(Result.success(stale)).whenever(repository).fetchPreviousMessages()
            vm.fetchMessages()
            advanceUntilIdle()
            vm.messages.value.map { it.id } shouldBe listOf(88L, 89L, 90L, 91L)
        }
    }

    test("D (offline off): a stale newer page that completes after the jump is ignored") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val first = listOf(msg(90), msg(91))
            val stale = listOf(msg(92))
            val target = msg(5)
            val jumped = window(5)
            whenever(repository.fetchPreviousMessages()).thenReturn(Result.success(first))
            whenever(repository.fetchNextMessages(any())).doSuspendableAnswer {
                gate.await(); Result.success(stale)
            }
            whenever(repository.getMessage(5L)).thenReturn(Result.success(target))
            whenever(repository.fetchSurroundingMessages(5L)).thenReturn(Result.success(jumped))
            val vm = vm()
            vm.fetchMessages()
            advanceUntilIdle()

            vm.fetchNextMessages() // newer page in flight
            vm.goToMessage(5L)
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            vm.messages.value.map { it.id } shouldBe listOf(4L, 5L, 6L)
        }
    }

    test("D (offline off): an older page asked for while the jump is in flight is not read") {
        runTest {
            val target = CompletableDeferred<Unit>()
            val first = listOf(msg(90), msg(91))
            val targetMsg = msg(5)
            val jumped = window(5)
            whenever(repository.fetchPreviousMessages()).thenReturn(Result.success(first))
            whenever(repository.getMessage(5L)).doSuspendableAnswer { target.await(); Result.success(targetMsg) }
            whenever(repository.fetchSurroundingMessages(5L)).thenReturn(Result.success(jumped))
            val vm = vm()
            vm.fetchMessages()
            advanceUntilIdle()

            vm.goToMessage(5L)
            vm.fetchMessages() // the jump has not placed its window yet
            target.complete(Unit)
            advanceUntilIdle()

            verifyBlocking(repository, times(1)) { fetchPreviousMessages() }
            vm.messages.value.map { it.id } shouldBe listOf(4L, 5L, 6L)

            vm.fetchMessages() // once placed, paging resumes from the jumped window
            advanceUntilIdle()
            verifyBlocking(repository, times(2)) { fetchPreviousMessages() }
        }
    }

    test("D (offline off): a superseded jump never lands over the newer one") {
        runTest {
            val first = CompletableDeferred<Unit>()
            val m5 = msg(5)
            val w5 = window(5)
            val m50 = msg(50)
            val w50 = window(50)
            whenever(repository.getMessage(5L)).doSuspendableAnswer { first.await(); Result.success(m5) }
            whenever(repository.fetchSurroundingMessages(5L)).thenReturn(Result.success(w5))
            whenever(repository.getMessage(50L)).thenReturn(Result.success(m50))
            whenever(repository.fetchSurroundingMessages(50L)).thenReturn(Result.success(w50))
            val vm = vm()

            vm.goToMessage(5L)
            vm.goToMessage(50L)
            advanceUntilIdle()
            first.complete(Unit)
            advanceUntilIdle()

            vm.messages.value.map { it.id } shouldBe listOf(49L, 50L, 51L)
        }
    }

    test("D (offline off): a stale reconnect read that completes after the jump is ignored") {
        runTest {
            val gate = CompletableDeferred<Unit>()
            val first = listOf(msg(90), msg(91))
            val stale = listOf(msg(92))
            val target = msg(5)
            val jumped = window(5)
            whenever(repository.fetchPreviousMessages()).thenReturn(Result.success(first))
            whenever(repository.fetchActionMessages(any())).thenReturn(Result.success(emptyList()))
            whenever(repository.fetchNextMessages(any())).doSuspendableAnswer { gate.await(); Result.success(stale) }
            whenever(repository.getMessage(5L)).thenReturn(Result.success(target))
            whenever(repository.fetchSurroundingMessages(5L)).thenReturn(Result.success(jumped))
            val vm = vm()
            vm.fetchMessages()
            advanceUntilIdle()

            // The reconnect / call-activity sweep (fetchMissedMessages → fetchNextMessagesOnReconnect).
            CometChatMessageListViewModel::class.java.getDeclaredMethod("fetchMissedMessages")
                .apply { isAccessible = true }.invoke(vm)
            vm.goToMessage(5L)
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            vm.messages.value.map { it.id } shouldBe listOf(4L, 5L, 6L)
        }
    }

    test("D (offline off): clear() during a jump starts a new window: the newest page lands, the jump never does") {
        runTest {
            val target = CompletableDeferred<Unit>()
            val stale = CompletableDeferred<Unit>()
            val first = listOf(msg(90), msg(91))
            val older = listOf(msg(80))
            val newest = listOf(msg(98), msg(99))
            val targetMsg = msg(5)
            val jumped = window(5)
            whenever(repository.fetchPreviousMessages()).thenReturn(Result.success(first))
            whenever(repository.getMessage(5L)).doSuspendableAnswer { target.await(); Result.success(targetMsg) }
            whenever(repository.fetchSurroundingMessages(5L)).thenReturn(Result.success(jumped))
            val vm = vm()
            vm.fetchMessages()
            advanceUntilIdle()

            whenever(repository.fetchPreviousMessages()).doSuspendableAnswer { stale.await(); Result.success(older) }
            vm.fetchMessages() // a stale older page holds isInProgress
            vm.goToMessage(5L) // and a jump is in flight

            // Scroll to bottom: the kits clear() and fetch the newest page.
            // Stubber form: never invokes the gated answer while re-stubbing.
            org.mockito.kotlin.doReturn(Result.success(newest)).whenever(repository).fetchPreviousMessages()
            vm.clear()
            vm.fetchMessages()
            advanceUntilIdle()
            vm.messages.value.map { it.id } shouldBe listOf(98L, 99L)

            target.complete(Unit)
            stale.complete(Unit)
            advanceUntilIdle()
            vm.messages.value.map { it.id } shouldBe listOf(98L, 99L)
            vm.isInProgress.value shouldBe false
        }
    }
})
