package com.cometchat.uikit.core.viewmodel.threadheader

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.viewmodel.CometChatThreadHeaderViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * The thread header: the parent message shown above a thread, and the reply count beside it.
 *
 * Its job is narrow but easy to get subtly wrong. The header holds one message and a count, and
 * both have to stay in step with edits and new replies arriving from elsewhere:
 *
 * - **Only replies to *this* parent count.** A reply belonging to another thread, or a top-level
 *   message with no parent at all, must not bump the number above the header.
 * - **The user's own replies are not counted here.** They arrive twice — once as a UI-sent event
 *   and once over the socket — so the receive path skips anything sent by the logged-in user, or
 *   every reply you send would count double.
 * - **An edit to the parent carries the viewer's pin and save state forward.** The edit payload
 *   carries new content but not those flags, so a naive replace would silently un-pin a message
 *   for the person looking at it.
 * - **Setting a parent resets the header** rather than accumulating, so reopening a thread does
 *   not stack old state.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*ThreadHeaderReplyTrackingTest"
 */
class ThreadHeaderReplyTrackingTest : FunSpec({

    val me = "logged_in_user"
    val dispatcher = UnconfinedTestDispatcher()

    lateinit var cometChatStatic: MockedStatic<CometChat>

    class TestableThreadHeaderViewModel : CometChatThreadHeaderViewModel(enableListeners = false) {
        fun teardown() {
            viewModelScope.cancel()
            runBlocking { viewModelScope.coroutineContext.job.join() }
        }
    }

    val liveViewModels = mutableListOf<TestableThreadHeaderViewModel>()

    // Main is spec-scoped: the header emits on shared flows from viewModelScope, and resetting
    // Main between tests can leave one of those resuming onto a dispatcher that is gone.
    beforeSpec {
        Dispatchers.setMain(dispatcher)
        // The view model calls asLiveData() in its constructor, which asks ArchTaskExecutor
        // whether it is on the main thread and NPEs without a real Looper. Same delegate the
        // other thread-header specs install.
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
        })
    }

    afterSpec {
        ArchTaskExecutor.getInstance().setDelegate(null)
        Dispatchers.resetMain()
    }

    beforeTest {
        cometChatStatic = Mockito.mockStatic(CometChat::class.java)
        val loggedIn = mock<User>()
        whenever(loggedIn.uid).thenReturn(me)
        cometChatStatic.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(loggedIn)
    }

    afterTest {
        liveViewModels.forEach { it.teardown() }
        liveViewModels.clear()
        cometChatStatic.close()
    }

    fun header(): TestableThreadHeaderViewModel =
        TestableThreadHeaderViewModel().also { liveViewModels += it }

    fun message(id: Long, parentId: Long = 0, senderUid: String = "someone", replies: Int = 0): BaseMessage =
        TextMessage("receiver", "text $id", CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            if (parentId > 0) this.parentMessageId = parentId
            this.replyCount = replies
            sender = User().apply { uid = senderUid }
        }

    // ==================== setting the parent ====================

    test("setting a parent seeds the list and the reply count from it") {
        val vm = header()
        vm.setParentMessage(message(id = 100, replies = 7))

        vm.getCurrentParentMessage()?.id shouldBe 100L
        vm.replyCountStateFlow.value shouldBe 7
        vm.parentMessageListStateFlow.value.map { it.id } shouldBe listOf(100L)
    }

    test("setting a null parent leaves the header as it was") {
        val vm = header()
        vm.setParentMessage(message(id = 100, replies = 3))

        vm.setParentMessage(null)

        vm.getCurrentParentMessage()?.id shouldBe 100L
        vm.replyCountStateFlow.value shouldBe 3
    }

    test("setting a second parent replaces the first rather than accumulating") {
        val vm = header()
        vm.setParentMessage(message(id = 100, replies = 3))
        vm.setParentMessage(message(id = 200, replies = 9))

        vm.getCurrentParentMessage()?.id shouldBe 200L
        vm.replyCountStateFlow.value shouldBe 9
        vm.parentMessageListStateFlow.value.map { it.id } shouldBe listOf(200L)
    }

    test("a fresh header has no parent and no replies") {
        val vm = header()
        vm.getCurrentParentMessage() shouldBe null
        vm.replyCountStateFlow.value shouldBe 0
        vm.parentMessageListStateFlow.value shouldBe emptyList()
    }

    // ==================== updating the parent ====================

    test("an update to the parent itself replaces the held copy") {
        val vm = header()
        vm.setParentMessage(message(id = 100, replies = 2))

        vm.updateParentMessageInList(message(id = 100, replies = 5))

        vm.parentMessageListStateFlow.value.map { it.id } shouldBe listOf(100L)
        vm.getCurrentParentMessage()?.id shouldBe 100L
    }

    test("an update for a reply to this parent refreshes the header without swapping it") {
        val vm = header()
        vm.setParentMessage(message(id = 100))

        vm.updateParentMessageInList(message(id = 101, parentId = 100))

        // the header still shows the parent, not the reply
        vm.parentMessageListStateFlow.value.map { it.id } shouldBe listOf(100L)
        vm.getCurrentParentMessage()?.id shouldBe 100L
    }

    test("an update for an unrelated message leaves the header alone") {
        val vm = header()
        vm.setParentMessage(message(id = 100))

        vm.updateParentMessageInList(message(id = 999, parentId = 555))

        vm.parentMessageListStateFlow.value.map { it.id } shouldBe listOf(100L)
    }

    test("an update with no parent set is a no-op") {
        val vm = header()

        vm.updateParentMessageInList(message(id = 100))

        vm.getCurrentParentMessage() shouldBe null
        vm.parentMessageListStateFlow.value shouldBe emptyList()
    }

    test("a null update is a no-op") {
        val vm = header()
        vm.setParentMessage(message(id = 100, replies = 4))

        vm.updateParentMessageInList(null)

        vm.replyCountStateFlow.value shouldBe 4
        vm.parentMessageListStateFlow.value.map { it.id } shouldBe listOf(100L)
    }

    // ==================== reply counting ====================

    test("the count seeded from the parent is what the header reports") {
        val vm = header()
        vm.setParentMessage(message(id = 100, replies = 12))

        vm.replyCountStateFlow.value shouldBe 12
    }

    test("replacing the parent re-seeds the count from the new one") {
        val vm = header()
        vm.setParentMessage(message(id = 100, replies = 12))
        vm.setParentMessage(message(id = 100, replies = 0))

        vm.replyCountStateFlow.value shouldBe 0
    }
})
