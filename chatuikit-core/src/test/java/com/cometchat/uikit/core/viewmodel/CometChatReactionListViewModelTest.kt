package com.cometchat.uikit.core.viewmodel

import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Reaction
import com.cometchat.chat.models.ReactionCount
import com.cometchat.uikit.core.domain.repository.ReactionListRepository
import com.cometchat.uikit.core.domain.usecase.FetchReactionsUseCase
import com.cometchat.uikit.core.domain.usecase.RemoveReactionUseCase
import com.cometchat.uikit.core.state.ReactionListUIState
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Recording fake for [FetchReactionsUseCase]. A real subclass (the use case is
 * `open`) avoids Mockito double-wrapping the inline-class `Result` through the
 * value-class ABI — the documented pitfall for stubbing suspend funs returning
 * `kotlin.Result` (see TEST-SETUP.md).
 */
private class FakeFetchReactionsUseCase(
    private val result: Result<List<Reaction>>
) : FetchReactionsUseCase(mock<ReactionListRepository>()) {
    var lastRequest: ReactionsRequest? = null
    override suspend fun invoke(request: ReactionsRequest): Result<List<Reaction>> {
        lastRequest = request
        return result
    }
}

private class FakeRemoveReactionUseCase(
    private val result: Result<BaseMessage>
) : RemoveReactionUseCase(mock<ReactionListRepository>()) {
    var calledWith: Pair<Long, String>? = null
    override suspend fun invoke(messageId: Long, emoji: String): Result<BaseMessage> {
        calledWith = messageId to emoji
        return result
    }
}

/**
 * Tests for [CometChatReactionListViewModel] (ENG-38677 / L — reaction-list
 * vertical: ViewModel + repository + datasource).
 *
 * Built with `enableListeners = false` so the event collector is not started.
 * Uses an [UnconfinedTestDispatcher] as Main so `viewModelScope.launch` runs
 * eagerly and state settles synchronously.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CometChatReactionListViewModelTest : FunSpec({

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    fun reactionCount(emoji: String, n: Int, byMe: Boolean = false) = ReactionCount().apply {
        reaction = emoji
        count = n
        setReactedByMe(byMe)
    }

    fun messageWith(id: Long, reactions: List<ReactionCount>): BaseMessage =
        mock<BaseMessage>().also {
            whenever(it.id).thenReturn(id)
            whenever(it.reactions).thenReturn(reactions)
        }

    fun viewModel(
        fetch: FetchReactionsUseCase = FakeFetchReactionsUseCase(Result.success(emptyList())),
        remove: RemoveReactionUseCase = FakeRemoveReactionUseCase(Result.success(MockFactory.createTextMessage()))
    ) = CometChatReactionListViewModel(fetch, remove, enableListeners = false)

    test("initial state is Loading with empty headers and users") {
        val vm = viewModel()
        vm.uiState.value.shouldBeInstanceOf<ReactionListUIState.Loading>()
        vm.reactionHeaders.value shouldBe emptyList()
        vm.reactedUsers.value shouldBe emptyList()
        vm.selectedReaction.value shouldBe null
        vm.activeTabIndex.value shouldBe 0
        vm.baseMessage.value shouldBe null
    }

    test("setBaseMessage builds an All tab plus emoji tabs and selects All") {
        val vm = viewModel()
        val message = messageWith(100L, listOf(reactionCount("👍", 2, byMe = true), reactionCount("❤️", 1)))

        vm.setBaseMessage(message)

        val headers = vm.reactionHeaders.value
        headers.size shouldBe 3
        headers[0].reaction shouldBe "All"
        headers[0].count shouldBe 3 // 2 + 1
        headers[1].reaction shouldBe "👍"
        headers[2].reaction shouldBe "❤️"
        vm.selectedReaction.value shouldBe "All"
        vm.activeTabIndex.value shouldBe 0
        vm.baseMessage.value shouldBe message
    }

    test("setBaseMessage with no reactions leaves headers empty and selection null") {
        val vm = viewModel()
        vm.setBaseMessage(messageWith(1L, emptyList()))

        vm.reactionHeaders.value shouldBe emptyList()
        vm.selectedReaction.value shouldBe null
    }

    test("setSelectedReaction updates selection and active tab index") {
        val vm = viewModel()
        vm.setBaseMessage(messageWith(1L, listOf(reactionCount("👍", 1), reactionCount("❤️", 1))))

        vm.setSelectedReaction("❤️")
        vm.selectedReaction.value shouldBe "❤️"
        vm.activeTabIndex.value shouldBe 2 // [All, 👍, ❤️]

        vm.setSelectedReaction(null) // defaults to "All"
        vm.selectedReaction.value shouldBe "All"
        vm.activeTabIndex.value shouldBe 0
    }

    test("fetchReactedUsers with no base message does nothing") {
        val vm = viewModel()
        vm.fetchReactedUsers()
        vm.uiState.value.shouldBeInstanceOf<ReactionListUIState.Loading>()
    }

    test("fetchReactedUsers success populates users and sets Content") {
        val reactions = listOf(mock<Reaction>(), mock<Reaction>())
        val vm = viewModel(fetch = FakeFetchReactionsUseCase(Result.success(reactions)))
        vm.setBaseMessage(messageWith(5L, listOf(reactionCount("👍", 2))))

        val builder = mock<ReactionsRequest.ReactionsRequestBuilder>()
        whenever(builder.build()).thenReturn(mock<ReactionsRequest>())

        vm.fetchReactedUsers(customBuilder = builder)

        vm.reactedUsers.value shouldBe reactions
        vm.uiState.value.shouldBeInstanceOf<ReactionListUIState.Content>()
    }

    test("fetchReactedUsers failure sets Error") {
        val error = MockFactory.createCometChatException("ERR_X", "failed")
        val vm = viewModel(fetch = FakeFetchReactionsUseCase(Result.failure(error)))
        vm.setBaseMessage(messageWith(5L, listOf(reactionCount("👍", 2))))

        val builder = mock<ReactionsRequest.ReactionsRequestBuilder>()
        whenever(builder.build()).thenReturn(mock<ReactionsRequest>())

        vm.fetchReactedUsers(customBuilder = builder)

        vm.uiState.value.shouldBeInstanceOf<ReactionListUIState.Error>()
    }

    test("removeReaction delegates to the use case with message id and emoji") {
        val remove = FakeRemoveReactionUseCase(Result.success(MockFactory.createTextMessage()))
        val vm = viewModel(remove = remove)
        val message = messageWith(77L, listOf(reactionCount("👍", 1, byMe = true)))
        vm.setBaseMessage(message)

        vm.removeReaction(message, "👍")

        remove.calledWith shouldBe (77L to "👍")
    }

    test("clearCache runs without error") {
        val vm = viewModel()
        vm.clearCache()
    }
})
