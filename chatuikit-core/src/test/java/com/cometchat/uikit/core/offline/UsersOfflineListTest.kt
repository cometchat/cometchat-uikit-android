package com.cometchat.uikit.core.offline

import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.UsersRepository
import com.cometchat.uikit.core.domain.repository.UsersTwoPhaseRepository
import com.cometchat.uikit.core.domain.usecase.FetchUsersUseCase
import com.cometchat.uikit.core.domain.usecase.SearchUsersUseCase
import com.cometchat.uikit.core.state.UsersUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatUsersViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/** A repository with both reads; each two-phase read is a channel the test feeds phase by phase. */
internal class FakeUsersRepository : UsersRepository, UsersTwoPhaseRepository {
    val singleCalls = mutableListOf<UsersRequest>()
    var singleResult: Result<List<User>> = Result.success(emptyList())
    val twoPhaseCalls = mutableListOf<UsersRequest>()
    val reads = mutableListOf<Channel<TwoPhaseResult<User>>>()

    override suspend fun getUsers(request: UsersRequest): Result<List<User>> {
        singleCalls += request
        return singleResult
    }

    override fun hasMoreUsers(): Boolean = true

    override fun getUsersTwoPhase(request: UsersRequest): Flow<TwoPhaseResult<User>> {
        twoPhaseCalls += request
        return Channel<TwoPhaseResult<User>>(Channel.UNLIMITED).also { reads += it }.consumeAsFlow()
    }

    fun cached(vararg uids: String, read: Int = reads.lastIndex) {
        reads[read].trySend(TwoPhaseResult.Cached(uids.map { MockFactory.createUser(uid = it) }))
    }

    fun server(vararg uids: String, read: Int = reads.lastIndex) {
        reads[read].trySend(TwoPhaseResult.Server(uids.map { MockFactory.createUser(uid = it) }))
    }

    fun error(code: String = SnapshotListRead.NOT_CONNECTED, read: Int = reads.lastIndex) {
        reads[read].trySend(TwoPhaseResult.Error(CometChatException(code, "e")))
    }
}

/**
 * ENG-41132 — users list two-phase (README §3.9, BEH-SNAP-01..07, BEH-READ-01): cached first paint,
 * server reconcile by uid, D84 offline/error/empty states and reconnect reload, reconnect re-read without
 * blanking, rows kept offline, stale generations dropped, offline-off unchanged.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UsersOfflineListTest : FunSpec({

    lateinit var repo: FakeUsersRepository
    lateinit var dispatcher: kotlinx.coroutines.test.TestDispatcher

    fun uids(vm: CometChatUsersViewModel) = vm.users.value.map { it.uid }

    // D84 rule 5: what the view hands to onError while rows stay on screen.
    fun collectErrors(vm: CometChatUsersViewModel): List<CometChatException> {
        val errors = mutableListOf<CometChatException>()
        CoroutineScope(dispatcher).launch { vm.rowsKeptError.collect { errors += it } }
        return errors
    }

    fun vm(): CometChatUsersViewModel =
        CometChatUsersViewModel(FetchUsersUseCase(repo), SearchUsersUseCase(repo), enableListeners = false)

    beforeTest {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        OfflineGate.testOverride = true
        repo = FakeUsersRepository()
    }
    afterTest {
        OfflineGate.testOverride = null
        Dispatchers.resetMain()
    }

    test("cached page is the first paint; the server page reconciles with no duplicates") {
        val vm = vm()
        vm.fetchUsers()
        vm.uiState.value shouldBe UsersUIState.Loading
        repo.cached("u1", "u2")
        vm.uiState.value shouldBe UsersUIState.Content
        uids(vm) shouldBe listOf("u1", "u2")
        repo.server("u2", "u1", "u3")
        uids(vm) shouldBe listOf("u2", "u1", "u3")
        repo.singleCalls.size shouldBe 0
    }

    test("an empty cached page does not end the list") {
        val vm = vm()
        vm.fetchUsers()
        repo.cached()
        repo.error()
        vm.fetchUsers() // still allowed: the list did not end
        repo.twoPhaseCalls.size shouldBe 2
    }

    test("D84 rule 7: offline keyword search with no saved match is Connect to search, no onError") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchUsers()
        repo.server("u1")
        vm.searchUsers("al")
        dispatcher.scheduler.advanceUntilIdle() // past the search debounce
        repo.twoPhaseCalls.size shouldBe 2
        repo.twoPhaseCalls.last().searchKeyword shouldBe "al"
        repo.cached()
        repo.error()
        vm.users.value shouldBe emptyList()
        vm.uiState.value shouldBe UsersUIState.Empty
        vm.connectToSearch.value shouldBe true
        vm.connectToLoad.value shouldBe false
        errors shouldBe emptyList()
    }

    test("D84 rule 3: presence filter (no keyword) offline with nothing shown is Connect to load users, not search") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.setUsersRequestBuilder(UsersRequest.UsersRequestBuilder().setUserStatus(UsersRequest.USER_STATUS_ONLINE).setLimit(30))
        vm.fetchUsers()
        repo.cached()
        repo.error()
        vm.uiState.value.shouldBeInstanceOf<UsersUIState.Error>()
        vm.connectToLoad.value shouldBe true
        vm.connectToSearch.value shouldBe false
        errors shouldBe emptyList()
    }

    test("reconnect refresh re-reads two-phase and never blanks the list") {
        val vm = vm()
        vm.fetchUsers()
        repo.server("u1", "u2")
        vm.refreshList()
        uids(vm) shouldBe listOf("u1", "u2") // kept while the re-read runs
        repo.cached()
        uids(vm) shouldBe listOf("u1", "u2")
        repo.server("u2", "u9")
        uids(vm) shouldBe listOf("u2", "u9")
        repo.twoPhaseCalls.size shouldBe 2
    }

    test("D84 rule 2: offline with cached rows (user-started open) keeps them and reports nothing") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchUsers()
        repo.cached("u1")
        repo.error()
        uids(vm) shouldBe listOf("u1")
        vm.uiState.value shouldBe UsersUIState.Content
        vm.connectToLoad.value shouldBe false
        errors shouldBe emptyList()
    }

    test("D84 rule 5: an online error with cached rows (user-started open) keeps them and reports once") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchUsers()
        repo.cached("u1")
        repo.error(code = "ERR_SERVER")
        uids(vm) shouldBe listOf("u1")
        vm.uiState.value shouldBe UsersUIState.Content
        errors.map { it.code } shouldBe listOf("ERR_SERVER")
    }

    test("a stale generation never touches the screen") {
        val vm = vm()
        vm.fetchUsers()
        repo.server("u1")
        vm.fetchUsers() // read 1, in flight
        vm.refreshList() // read 2 supersedes it
        repo.cached("stale", read = 1)
        repo.server("stale", read = 1)
        uids(vm) shouldBe listOf("u1")
        repo.server("fresh", read = 2)
        uids(vm) shouldBe listOf("fresh")
    }

    test("offline off: one round through today's path, today's output") {
        OfflineGate.testOverride = false
        repo.singleResult = Result.success(listOf(MockFactory.createUser(uid = "u1")))
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchUsers()
        repo.singleCalls.size shouldBe 1
        repo.twoPhaseCalls.size shouldBe 0
        uids(vm) shouldBe listOf("u1")
        vm.uiState.value.shouldBeInstanceOf<UsersUIState.Content>()
        errors shouldBe emptyList()
    }

    test("D84: a failed load-more page keeps its rows and reports no onError") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchUsers()
        repo.server("u1")
        vm.fetchUsers() // load-more
        repo.cached()
        repo.error()
        uids(vm) shouldBe listOf("u1")
        vm.uiState.value shouldBe UsersUIState.Content
        errors shouldBe emptyList()
    }

    test("D84: a failure reported before the view collects is delivered once, to the first collector") {
        val vm = vm()
        vm.fetchUsers()
        repo.cached("u1")
        repo.error(code = "ERR_SERVER")
        collectErrors(vm).map { it.code } shouldBe listOf("ERR_SERVER")
        collectErrors(vm) shouldBe emptyList() // a later collector (rotation) gets no repeat
    }

    test("D84 rule 3: nothing shown and offline is the offline state (Error + connectToLoad), no rows-kept report") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchUsers()
        repo.cached()
        repo.error()
        vm.users.value shouldBe emptyList()
        vm.uiState.value.shouldBeInstanceOf<UsersUIState.Error>()
        vm.connectToLoad.value shouldBe true
        vm.connectToSearch.value shouldBe false
        errors shouldBe emptyList()
    }

    test("D84: the server answering an empty page is the empty state") {
        val vm = vm()
        vm.fetchUsers()
        repo.cached()
        repo.server()
        vm.uiState.value shouldBe UsersUIState.Empty
    }

    test("D84 rule 8: the offline state reloads once on reconnect") {
        val vm = vm()
        vm.fetchUsers()
        repo.cached()
        repo.error()
        vm.connectToLoad.value shouldBe true

        vm.onConnected()
        repo.twoPhaseCalls.size shouldBe 2
        repo.twoPhaseCalls.last().searchKeyword shouldBe null
        repo.server("u1")
        uids(vm) shouldBe listOf("u1")
        vm.uiState.value shouldBe UsersUIState.Content
        vm.connectToLoad.value shouldBe false
    }

    test("D84 rule 8: an open offline search re-runs that search on reconnect") {
        val vm = vm()
        vm.searchUsers("al")
        dispatcher.scheduler.advanceUntilIdle()
        repo.error()
        vm.uiState.value shouldBe UsersUIState.Empty
        vm.connectToSearch.value shouldBe true

        vm.onConnected()
        dispatcher.scheduler.advanceUntilIdle()
        repo.twoPhaseCalls.size shouldBe 2
        repo.twoPhaseCalls.last().searchKeyword shouldBe "al"
        repo.server("al1")
        uids(vm) shouldBe listOf("al1")

        // Answered: the next connect is today's plain refresh again.
        vm.onConnected()
        repo.twoPhaseCalls.last().searchKeyword shouldBe null
    }

    test("D84 rule 4: an online failure with nothing shown is the error view, not the offline state") {
        val vm = vm()
        vm.fetchUsers()
        repo.cached()
        repo.error(code = "ERR_SERVER")
        (vm.uiState.value as UsersUIState.Error).exception.code shouldBe "ERR_SERVER"
        vm.connectToLoad.value shouldBe false
        vm.connectToSearch.value shouldBe false
    }

    // ---- D84 rule 5: a refresh the kit starts by itself stays quiet over rows.

    test("D84 rule 5: a reconnect refresh failing over rows keeps them and reports nothing") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchUsers()
        repo.server("u1", "u2")

        vm.onConnected()
        repo.twoPhaseCalls.size shouldBe 2
        repo.cached("u1", "u2")
        repo.error()
        uids(vm) shouldBe listOf("u1", "u2")
        vm.uiState.value shouldBe UsersUIState.Content
        errors shouldBe emptyList()
    }

    test("D84 rule 5: a refresh the app starts, failing online over rows, reports once") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchUsers()
        repo.server("u1")

        vm.refreshList()
        repo.cached("u1")
        repo.error(code = "ERR_SERVER")
        uids(vm) shouldBe listOf("u1")
        vm.uiState.value shouldBe UsersUIState.Content
        errors.map { it.code } shouldBe listOf("ERR_SERVER")
    }

    test("D84 rule 2: a refresh the app starts, failing offline over rows, reports nothing") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchUsers()
        repo.server("u1")

        vm.refreshList()
        repo.cached("u1")
        repo.error()
        uids(vm) shouldBe listOf("u1")
        vm.uiState.value shouldBe UsersUIState.Content
        errors shouldBe emptyList()
    }

    test("D84 rule 5: a reconnect refresh failing online over rows stays quiet") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchUsers()
        repo.server("u1")

        vm.onConnected()
        repo.cached("u1")
        repo.error(code = "ERR_SERVER")
        uids(vm) shouldBe listOf("u1")
        errors shouldBe emptyList()
    }

    test("D84 rule 5: the reconnect reload from the error view failing is the error state") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchUsers()
        repo.cached()
        repo.error()
        vm.uiState.value.shouldBeInstanceOf<UsersUIState.Error>()

        vm.onConnected()
        repo.cached()
        repo.error(code = "ERR_SERVER")
        (vm.uiState.value as UsersUIState.Error).exception.code shouldBe "ERR_SERVER"
        errors shouldBe emptyList()
    }
})
