package com.cometchat.uikit.core.offline

import com.cometchat.chat.core.GroupsRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.uikit.core.domain.repository.GroupsRepository
import com.cometchat.uikit.core.domain.repository.GroupsTwoPhaseRepository
import com.cometchat.uikit.core.domain.usecase.FetchGroupsUseCase
import com.cometchat.uikit.core.domain.usecase.JoinGroupUseCase
import com.cometchat.uikit.core.state.GroupsUIState
import com.cometchat.uikit.core.viewmodel.CometChatGroupsViewModel
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
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.mock

/** A repository with both reads; each two-phase read is a channel the test feeds phase by phase. */
internal class FakeGroupsRepository : GroupsRepository, GroupsTwoPhaseRepository {
    val singleCalls = mutableListOf<GroupsRequest>()
    var singleResult: Result<List<Group>> = Result.success(emptyList())
    val twoPhaseCalls = mutableListOf<GroupsRequest>()
    val reads = mutableListOf<Channel<TwoPhaseResult<Group>>>()

    override suspend fun fetchGroups(request: GroupsRequest): Result<List<Group>> {
        singleCalls += request
        return singleResult
    }

    override suspend fun joinGroup(groupId: String, groupType: String, password: String?): Result<Group> =
        Result.failure(IllegalStateException("unused"))

    override fun hasMoreGroups(): Boolean = true

    override fun fetchGroupsTwoPhase(request: GroupsRequest): Flow<TwoPhaseResult<Group>> {
        twoPhaseCalls += request
        return Channel<TwoPhaseResult<Group>>(Channel.UNLIMITED).also { reads += it }.consumeAsFlow()
    }

    fun cached(vararg guids: String, read: Int = reads.lastIndex) {
        reads[read].trySend(TwoPhaseResult.Cached(guids.map { group(it) }))
    }

    fun server(vararg guids: String, read: Int = reads.lastIndex) {
        reads[read].trySend(TwoPhaseResult.Server(guids.map { group(it) }))
    }

    fun error(code: String = SnapshotListRead.NOT_CONNECTED, read: Int = reads.lastIndex) {
        reads[read].trySend(TwoPhaseResult.Error(CometChatException(code, "e")))
    }

    companion object {
        fun group(guid: String): Group = Group(guid, "Group $guid", "public", null, null, null)
    }
}

/**
 * ENG-41132 — groups list two-phase (README §3.9, BEH-SNAP-01..07, BEH-READ-01): cached first paint,
 * server reconcile by guid, D83 error/empty states and reconnect reload, reconnect re-read without
 * blanking, rows kept on any error, stale generations dropped, offline-off unchanged. Groups have no
 * presence filter, so the keyword is their only no-cache read (BEH-SNAP-04).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupsOfflineListTest : FunSpec({

    lateinit var repo: FakeGroupsRepository
    lateinit var dispatcher: TestDispatcher

    fun guids(vm: CometChatGroupsViewModel) = vm.groups.value.map { it.guid }

    // D83 rule 1: what the view hands to onError while rows stay on screen.
    fun collectErrors(vm: CometChatGroupsViewModel): List<CometChatException> {
        val errors = mutableListOf<CometChatException>()
        CoroutineScope(dispatcher).launch { vm.rowsKeptError.collect { errors += it } }
        return errors
    }

    // The ViewModel fetches its first page from init.
    fun vm(): CometChatGroupsViewModel =
        CometChatGroupsViewModel(FetchGroupsUseCase(repo), mock<JoinGroupUseCase>(), enableListeners = false)

    beforeTest {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        OfflineGate.testOverride = true
        repo = FakeGroupsRepository()
    }
    afterTest {
        OfflineGate.testOverride = null
        Dispatchers.resetMain()
    }

    test("cached page is the first paint; the server page reconciles with no duplicates") {
        val vm = vm()
        vm.uiState.value shouldBe GroupsUIState.Loading
        repo.cached("g1", "g2")
        vm.uiState.value.shouldBeInstanceOf<GroupsUIState.Content>()
        guids(vm) shouldBe listOf("g1", "g2")
        repo.server("g2", "g3")
        guids(vm) shouldBe listOf("g2", "g3")
        repo.singleCalls.size shouldBe 0
    }

    test("an empty cached page does not end the list") {
        val vm = vm()
        repo.cached()
        repo.error()
        vm.fetchGroups()
        repo.twoPhaseCalls.size shouldBe 2
    }

    test("D84 rule 7: offline keyword search with no saved match is Connect to search, no onError") {
        val vm = vm()
        val errors = collectErrors(vm)
        repo.server("g1")
        vm.searchGroups("fam")
        dispatcher.scheduler.advanceUntilIdle() // past the search debounce
        repo.twoPhaseCalls.last().searchKeyword shouldBe "fam"
        repo.cached()
        repo.error()
        vm.groups.value shouldBe emptyList()
        vm.uiState.value shouldBe GroupsUIState.Empty
        vm.connectToSearch.value shouldBe true
        vm.connectToLoad.value shouldBe false
        errors shouldBe emptyList()
    }

    test("reconnect refresh re-reads two-phase and never blanks the list") {
        val vm = vm()
        repo.server("g1", "g2")
        vm.refreshList()
        guids(vm) shouldBe listOf("g1", "g2")
        repo.cached("g2")
        guids(vm) shouldBe listOf("g2")
        repo.server("g2", "g5")
        guids(vm) shouldBe listOf("g2", "g5")
    }

    test("D84: a failed load-more page keeps the rows, not-connected and other errors alike, and reports no onError") {
        val vm = vm()
        val errors = collectErrors(vm)
        repo.server("g1")
        vm.fetchGroups()
        repo.error()
        guids(vm) shouldBe listOf("g1")
        vm.uiState.value.shouldBeInstanceOf<GroupsUIState.Content>()
        vm.fetchGroups()
        repo.error(code = "ERR_SERVER")
        guids(vm) shouldBe listOf("g1")
        vm.uiState.value.shouldBeInstanceOf<GroupsUIState.Content>()
        errors shouldBe emptyList()
    }

    test("D84 rule 5: a refresh that fails online over shown rows keeps them and reports onError once") {
        val vm = vm()
        val errors = collectErrors(vm)
        repo.server("g1")
        vm.refreshList()
        repo.cached("g1")
        repo.error(code = "ERR_SERVER")
        guids(vm) shouldBe listOf("g1")
        vm.uiState.value.shouldBeInstanceOf<GroupsUIState.Content>()
        errors.map { it.code } shouldBe listOf("ERR_SERVER")
    }

    test("D84 rule 2: a refresh that fails offline over shown rows keeps them and reports nothing") {
        val vm = vm()
        val errors = collectErrors(vm)
        repo.server("g1")
        vm.refreshList()
        repo.cached("g1")
        repo.error()
        guids(vm) shouldBe listOf("g1")
        vm.uiState.value.shouldBeInstanceOf<GroupsUIState.Content>()
        errors shouldBe emptyList()
    }

    test("a stale generation never touches the screen") {
        val vm = vm()
        repo.server("g1")
        vm.fetchGroups() // read 1, in flight
        vm.refreshList() // read 2 supersedes it
        repo.server("stale", read = 1)
        guids(vm) shouldBe listOf("g1")
        repo.server("fresh", read = 2)
        guids(vm) shouldBe listOf("fresh")
    }

    test("offline off: one round through today's path, today's output") {
        OfflineGate.testOverride = false
        repo.singleResult = Result.success(listOf(FakeGroupsRepository.group("g1")))
        val vm = vm()
        repo.singleCalls.size shouldBe 1
        repo.twoPhaseCalls.size shouldBe 0
        guids(vm) shouldBe listOf("g1")
        vm.uiState.value.shouldBeInstanceOf<GroupsUIState.Content>()
    }

    test("D84 rules 3, 8: nothing shown and offline is the offline state; it reloads once on reconnect") {
        val vm = vm()
        val errors = collectErrors(vm)
        repo.cached()
        repo.error()
        vm.groups.value shouldBe emptyList()
        vm.uiState.value.shouldBeInstanceOf<GroupsUIState.Error>()
        vm.connectToLoad.value shouldBe true
        errors shouldBe emptyList()

        vm.onConnected()
        repo.twoPhaseCalls.size shouldBe 2
        repo.server("g1")
        guids(vm) shouldBe listOf("g1")
        vm.uiState.value.shouldBeInstanceOf<GroupsUIState.Content>()
        vm.connectToLoad.value shouldBe false
    }

    test("D84: the server answering an empty page is the empty state") {
        val vm = vm()
        repo.cached()
        repo.server()
        vm.uiState.value shouldBe GroupsUIState.Empty
    }

    test("D84 rule 8: an open offline search re-runs that search on reconnect") {
        val vm = vm()
        repo.server("g1")
        vm.searchGroups("fam")
        dispatcher.scheduler.advanceUntilIdle()
        repo.error()
        vm.uiState.value shouldBe GroupsUIState.Empty
        vm.connectToSearch.value shouldBe true

        vm.onConnected()
        dispatcher.scheduler.advanceUntilIdle()
        repo.twoPhaseCalls.last().searchKeyword shouldBe "fam"
        repo.server("fam1")
        guids(vm) shouldBe listOf("fam1")
    }

    test("D84: any other failure with nothing shown is the error state too") {
        val vm = vm()
        repo.cached()
        repo.error(code = "ERR_SERVER")
        (vm.uiState.value as GroupsUIState.Error).exception.code shouldBe "ERR_SERVER"
        vm.connectToLoad.value shouldBe false
    }

    // ---- D84 rule 5: a refresh the kit starts by itself stays quiet over rows.

    test("D84 rule 5: a reconnect refresh failing over rows keeps them and reports nothing") {
        val vm = vm()
        val errors = collectErrors(vm)
        repo.server("g1", "g2")

        vm.onConnected()
        repo.twoPhaseCalls.size shouldBe 2
        repo.cached("g1", "g2")
        repo.error()
        guids(vm) shouldBe listOf("g1", "g2")
        vm.uiState.value.shouldBeInstanceOf<GroupsUIState.Content>()
        errors shouldBe emptyList()
    }

    test("D84 rule 5: a refresh the app starts, failing online over rows, still reports once") {
        val vm = vm()
        val errors = collectErrors(vm)
        repo.server("g1")

        vm.refreshList()
        repo.cached("g1")
        repo.error(code = "ERR_SERVER")
        guids(vm) shouldBe listOf("g1")
        errors.map { it.code } shouldBe listOf("ERR_SERVER")
    }

    test("D84 rule 5: the reconnect reload from the error view failing is the error state") {
        val vm = vm()
        val errors = collectErrors(vm)
        repo.cached()
        repo.error()
        vm.uiState.value.shouldBeInstanceOf<GroupsUIState.Error>()

        vm.onConnected()
        repo.cached()
        repo.error(code = "ERR_SERVER")
        (vm.uiState.value as GroupsUIState.Error).exception.code shouldBe "ERR_SERVER"
        errors shouldBe emptyList()
    }
})
