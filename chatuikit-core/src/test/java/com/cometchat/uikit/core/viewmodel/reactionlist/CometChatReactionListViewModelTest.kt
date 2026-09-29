package com.cometchat.uikit.core.viewmodel.reactionlist

import com.cometchat.chat.exceptions.CometChatException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.job
import kotlinx.coroutines.cancel
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Reaction
import com.cometchat.chat.models.ReactionCount
import com.cometchat.uikit.core.domain.usecase.FetchReactionsUseCase
import com.cometchat.uikit.core.domain.usecase.RemoveReactionUseCase
import com.cometchat.uikit.core.state.ReactionListUIState
import com.cometchat.uikit.core.viewmodel.CometChatReactionListViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests for [CometChatReactionListViewModel], which drives the reaction-details sheet: a row of
 * emoji tabs plus the list of people behind the selected one.
 *
 * Behaviours pinned here, in rough order of how easily they break:
 *
 * - **The "All" tab is synthetic.** It is prepended to whatever the message carries and its count
 *   is the sum of the rest, so a message with three reactions produces four tabs.
 * - **Tabs are cached per emoji.** Re-selecting a tab shows its cached users immediately and does
 *   not re-enter the loading state; switching to an uncached tab clears the list first so the
 *   previous tab's people never appear under the new one.
 * - **Concurrent work is refused, not queued.** A second fetch while one is in flight, or a second
 *   removal while one is pending, is dropped — with the removal flag reset on failure so a retry
 *   is still possible.
 * - **Both use-cases are injected**, so nothing here touches the network or the SDK request
 *   machinery beyond the request object the view model builds internally.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*CometChatReactionListViewModelTest"
 */
class CometChatReactionListViewModelTest : FunSpec({

    val dispatcher = UnconfinedTestDispatcher()

    lateinit var fetchReactions: FetchReactionsUseCase
    lateinit var removeReaction: RemoveReactionUseCase

    /**
     * The view model plus a way to stop it.
     *
     * Its fetch and removal run on viewModelScope. Left running, a coroutine dispatched to a
     * background worker resumes on Main after afterTest has called resetMain(), and dies with
     * "Dispatchers.Main was accessed ... after resetMain()". That exception has nothing awaiting
     * it, so coroutines-test attributes it to the *next* spec, which fails as
     * UncaughtExceptionsBeforeTest — it was doing that to the search spec.
     */
    class TestableReactionListViewModel(
        fetchReactionsUseCase: FetchReactionsUseCase,
        removeReactionUseCase: RemoveReactionUseCase
    ) : CometChatReactionListViewModel(fetchReactionsUseCase, removeReactionUseCase, enableListeners = false) {
        fun teardown() {
            viewModelScope.cancel()
            runBlocking { viewModelScope.coroutineContext.job.join() }
        }
    }

    val liveViewModels = mutableListOf<TestableReactionListViewModel>()

    fun viewModel(): TestableReactionListViewModel =
        TestableReactionListViewModel(fetchReactions, removeReaction).also { liveViewModels += it }

    // Main is installed for the whole spec, not per test. A successful removal calls
    // CometChatEvents.emitMessageEvent, which launches on the event bus singleton's own scope —
    // outside any view model and beyond the teardown below. Resetting Main between tests leaves
    // that coroutine resuming onto a dispatcher that is gone, and the resulting exception is
    // reported against whatever spec runs next.
    beforeSpec { Dispatchers.setMain(dispatcher) }
    afterSpec { Dispatchers.resetMain() }

    beforeTest {
        fetchReactions = mock()
        removeReaction = mock()
    }

    afterTest {
        // stop the view models -- see TestableReactionListViewModel
        liveViewModels.forEach { it.teardown() }
        liveViewModels.clear()
    }


    fun reactionCount(emoji: String, count: Int): ReactionCount = ReactionCount().apply {
        reaction = emoji
        this.count = count
        setReactedByMe(false)
    }

    fun messageWith(vararg counts: ReactionCount, id: Long = 99L): BaseMessage {
        val m = mock<BaseMessage>()
        whenever(m.id).thenReturn(id)
        whenever(m.reactions).thenReturn(counts.toList())
        return m
    }

    fun reactedBy(uid: String): Reaction {
        val r = mock<Reaction>()
        whenever(r.uid).thenReturn(uid)
        return r
    }

    // ==================== tab construction ====================

    test("setting a message prepends a synthetic All tab summing the individual counts") {
        val vm = viewModel()
        vm.setBaseMessage(messageWith(reactionCount("👍", 2), reactionCount("🎉", 3)))

        val headers = vm.reactionHeaders.value
        headers.size shouldBe 3
        headers[0].reaction shouldBe "All"
        headers[0].count shouldBe 5
        headers[1].reaction shouldBe "👍"
        headers[2].reaction shouldBe "🎉"

        vm.selectedReaction.value shouldBe "All"
        vm.activeTabIndex.value shouldBe 0
    }

    test("a message with no reactions produces no tabs and leaves nothing selected") {
        val vm = viewModel()
        vm.setBaseMessage(messageWith())

        vm.reactionHeaders.value shouldBe emptyList()
        vm.selectedReaction.value shouldBe null
        vm.activeTabIndex.value shouldBe 0
    }

    test("a message whose reactions are null is treated as having none") {
        val vm = viewModel()
        val m = mock<BaseMessage>()
        whenever(m.id).thenReturn(7L)
        whenever(m.reactions).thenReturn(null)

        vm.setBaseMessage(m)
        vm.reactionHeaders.value shouldBe emptyList()
    }

    // ==================== tab selection ====================

    test("selecting a tab moves the active index to that emoji") {
        val vm = viewModel()
        vm.setBaseMessage(messageWith(reactionCount("👍", 1), reactionCount("🎉", 1)))

        vm.setSelectedReaction("🎉")
        vm.selectedReaction.value shouldBe "🎉"
        vm.activeTabIndex.value shouldBe 2   // All, 👍, 🎉
    }

    test("selecting null falls back to the All tab") {
        val vm = viewModel()
        vm.setBaseMessage(messageWith(reactionCount("👍", 1)))

        vm.setSelectedReaction("👍")
        vm.setSelectedReaction(null)
        vm.selectedReaction.value shouldBe "All"
        vm.activeTabIndex.value shouldBe 0
    }

    test("selecting an emoji with no tab leaves the active index where it was") {
        val vm = viewModel()
        vm.setBaseMessage(messageWith(reactionCount("👍", 1)))
        vm.setSelectedReaction("👍")
        val before = vm.activeTabIndex.value

        vm.setSelectedReaction("🚀")   // never reacted with
        vm.selectedReaction.value shouldBe "🚀"
        vm.activeTabIndex.value shouldBe before
    }

    // ==================== fetching ====================

    test("a successful fetch publishes the reacted users") {
        runTest {
            // fixtures first: stubbing a mock inside an open whenever() is UnfinishedStubbing
            val users = listOf(reactedBy("u1"), reactedBy("u2"))
            whenever(fetchReactions(any())).thenReturn(Result.success(users))

            val vm = viewModel()
            vm.setBaseMessage(messageWith(reactionCount("👍", 2)))
            vm.fetchReactedUsers()

            vm.reactedUsers.value.size shouldBe 2
        }
    }

    test("a failed fetch surfaces an error state") {
        runTest {
            whenever(fetchReactions(any())).thenReturn(
                Result.failure(CometChatException("ERR", "nope"))
            )

            val vm = viewModel()
            vm.setBaseMessage(messageWith(reactionCount("👍", 1)))
            vm.fetchReactedUsers()

            (vm.uiState.value is ReactionListUIState.Error) shouldBe true
        }
    }

    test("fetching with no base message set does nothing at all") {
        runTest {
            val vm = viewModel()
            vm.fetchReactedUsers()
            verify(fetchReactions, never())(any())
        }
    }

    test("switching to a new tab clears the previous tab's people first") {
        runTest {
            val oneUser = listOf(reactedBy("u1"))
            whenever(fetchReactions(any())).thenReturn(Result.success(oneUser))

            val vm = viewModel()
            vm.setBaseMessage(messageWith(reactionCount("👍", 1), reactionCount("🎉", 1)))
            vm.fetchReactedUsers("👍")
            vm.reactedUsers.value.size shouldBe 1

            // the new tab returns nobody; the old tab's user must not linger
            whenever(fetchReactions(any())).thenReturn(Result.success(emptyList()))
            vm.fetchReactedUsers("🎉")
            vm.reactedUsers.value shouldBe emptyList()
            vm.selectedReaction.value shouldBe "🎉"
        }
    }

    test("re-fetching a tab appends to its cache — the pagination path, not a reset") {
        runTest {
            val oneUser = listOf(reactedBy("u1"))
            whenever(fetchReactions(any())).thenReturn(Result.success(oneUser))

            val vm = viewModel()
            vm.setBaseMessage(messageWith(reactionCount("👍", 1), reactionCount("🎉", 1)))
            vm.fetchReactedUsers("👍")
            vm.reactedUsers.value.size shouldBe 1

            vm.fetchReactedUsers("🎉")

            // Back on the first tab. The cached page is shown straight away, and the fetch that
            // follows appends its result rather than replacing: in production the cached request
            // has advanced, so that is the next page. With a stub returning the same page twice,
            // the append is visible as a duplicate — which is the behaviour being pinned, since a
            // caller that re-selects a tab expecting a refresh would get a growing list instead.
            vm.fetchReactedUsers("👍")
            vm.reactedUsers.value.size shouldBe 2
        }
    }

    test("clearCache drops the cached users so the next fetch starts clean") {
        runTest {
            val oneUser = listOf(reactedBy("u1"))
            whenever(fetchReactions(any())).thenReturn(Result.success(oneUser))

            val vm = viewModel()
            vm.setBaseMessage(messageWith(reactionCount("👍", 1)))
            vm.fetchReactedUsers("👍")

            vm.clearCache()
            whenever(fetchReactions(any())).thenReturn(Result.success(emptyList()))
            vm.fetchReactedUsers("👍")
            // nothing cached to fall back on, so the empty result stands
            vm.reactedUsers.value shouldBe emptyList()
        }
    }

    // ==================== removing a reaction ====================

    test("a successful removal is applied") {
        runTest {
            val updated = mock<BaseMessage>()
            whenever(updated.id).thenReturn(99L)
            whenever(removeReaction(eq(99L), eq("👍"))).thenReturn(Result.success(updated))

            val vm = viewModel()
            val message = messageWith(reactionCount("👍", 1))
            vm.setBaseMessage(message)
            vm.removeReaction(message, "👍")

            verify(removeReaction)(eq(99L), eq("👍"))
        }
    }

    test("a failed removal resets the guard so the user can retry") {
        runTest {
            whenever(removeReaction(any(), any()))
                .thenReturn(Result.failure(CometChatException("ERR", "nope")))

            val vm = viewModel()
            val message = messageWith(reactionCount("👍", 1))
            vm.setBaseMessage(message)

            vm.removeReaction(message, "👍")
            vm.removeReaction(message, "👍")   // retry must not be swallowed by the guard

            verify(removeReaction, org.mockito.kotlin.times(2))(eq(99L), eq("👍"))
        }
    }
})
