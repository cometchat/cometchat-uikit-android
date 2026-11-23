package com.cometchat.uikit.core.offline

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.OfflineCacheResetReason
import com.cometchat.uikit.core.domain.usecase.BanGroupMemberUseCase
import com.cometchat.uikit.core.domain.usecase.ChangeMemberScopeUseCase
import com.cometchat.uikit.core.domain.usecase.FetchGroupMembersUseCase
import com.cometchat.uikit.core.domain.usecase.FetchGroupsUseCase
import com.cometchat.uikit.core.domain.usecase.FetchUsersUseCase
import com.cometchat.uikit.core.domain.usecase.JoinGroupUseCase
import com.cometchat.uikit.core.domain.usecase.KickGroupMemberUseCase
import com.cometchat.uikit.core.domain.usecase.SearchUsersUseCase
import com.cometchat.uikit.core.state.GroupMembersUIState
import com.cometchat.uikit.core.state.GroupsUIState
import com.cometchat.uikit.core.state.UsersUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatGroupMembersViewModel
import com.cometchat.uikit.core.viewmodel.CometChatGroupsViewModel
import com.cometchat.uikit.core.viewmodel.CometChatUsersViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import com.cometchat.chat.models.GroupMember
import com.cometchat.uikit.core.domain.repository.GroupMembersRepository
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A subclassed use case, so the ViewModel takes the plain (non two-phase) read with offline on, as
 * it does for an app's own use case. Each read is held on a non-cancellable suspension (as an SDK
 * callback wrapper is): it completes even after its job was cancelled, so only the ViewModel's own
 * generation guard can stop its result from landing.
 */
private class HeldGroupMembersUseCase(repo: GroupMembersRepository) : FetchGroupMembersUseCase(repo) {
    val calls = mutableListOf<Continuation<Result<List<GroupMember>>>>()
    val keywords = mutableListOf<String?>()
    override suspend fun invoke(guid: String, limit: Int, searchKeyword: String?): Result<List<GroupMember>> {
        keywords += searchKeyword
        return suspendCoroutine { calls += it }
    }
    fun complete(index: Int, vararg uids: String) =
        calls[index].resume(Result.success(uids.map { FakeGroupMembersRepository.member(it) }))
}

/**
 * ENG-41568 (G8) — the users, groups and group members lists clear on a hub `SessionEnded` and on a
 * `CacheReset`, which also reloads once from page one, quietly over rows (D84 rule 5). The hub is
 * driven for real ([OfflineHub.onLogout] / [OfflineHub.publishForTest]); each ViewModel runs with
 * its listeners on (SDK statics mocked) and is cleared after the test, so its hub collector ends.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionEndSnapshotListsTest : FunSpec({

    lateinit var dispatcher: TestDispatcher
    lateinit var logMock: MockedStatic<Log>
    lateinit var cometChatMock: MockedStatic<CometChat>
    val stores = mutableListOf<ViewModelStore>()

    lateinit var users: FakeUsersRepository
    lateinit var groups: FakeGroupsRepository
    lateinit var members: FakeGroupMembersRepository

    val onlineError = "ERR_SERVER"

    fun <T : ViewModel> inStore(cls: Class<T>, create: () -> T): T {
        val store = ViewModelStore()
        stores += store
        return ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
        })[cls]
    }

    fun usersVm() = inStore(CometChatUsersViewModel::class.java) {
        CometChatUsersViewModel(FetchUsersUseCase(users), SearchUsersUseCase(users), enableListeners = true)
    }

    fun groupsVm() = inStore(CometChatGroupsViewModel::class.java) {
        CometChatGroupsViewModel(FetchGroupsUseCase(groups), mock<JoinGroupUseCase>(), enableListeners = true)
    }

    fun membersVm() = inStore(CometChatGroupMembersViewModel::class.java) {
        CometChatGroupMembersViewModel(
            FetchGroupMembersUseCase(members),
            mock<KickGroupMemberUseCase>(),
            mock<BanGroupMemberUseCase>(),
            mock<ChangeMemberScopeUseCase>(),
            enableListeners = true
        ).also { it.setGroup(Group().apply { guid = "g1"; name = "G" }) }
    }

    lateinit var heldMembers: HeldGroupMembersUseCase

    /** A group members ViewModel on the plain read path (a subclassed use case), offline on. */
    fun plainMembersVm(): CometChatGroupMembersViewModel {
        heldMembers = HeldGroupMembersUseCase(members)
        return inStore(CometChatGroupMembersViewModel::class.java) {
            CometChatGroupMembersViewModel(
                heldMembers,
                mock<KickGroupMemberUseCase>(),
                mock<BanGroupMemberUseCase>(),
                mock<ChangeMemberScopeUseCase>(),
                enableListeners = true
            ).also { it.setGroup(Group().apply { guid = "g1"; name = "G" }) }
        }
    }

    fun <E> collect(flow: kotlinx.coroutines.flow.Flow<E>): List<E> {
        val out = mutableListOf<E>()
        CoroutineScope(dispatcher).launch { flow.collect { out += it } }
        return out
    }

    fun logout() {
        OfflineHub.onLogout()
        dispatcher.scheduler.advanceUntilIdle()
    }

    fun publish(event: OfflineHubEvent) {
        OfflineHub.publishForTest(event)
        dispatcher.scheduler.advanceUntilIdle()
    }

    fun cacheReset() = publish(OfflineHubEvent.CacheReset(OfflineHub.gen, OfflineCacheResetReason.CORRUPTED, 0))

    beforeTest {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        OfflineGate.testOverride = true
        OfflineHub.resetForTest()
        logMock = Mockito.mockStatic(Log::class.java)
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        users = FakeUsersRepository()
        groups = FakeGroupsRepository()
        members = FakeGroupMembersRepository()
    }

    afterTest {
        stores.forEach { it.clear() }
        stores.clear()
        cometChatMock.close()
        logMock.close()
        OfflineGate.testOverride = null
        Dispatchers.resetMain()
    }

    // ------------------------------------------------------------------ users

    test("users: sessionEnded_clearsRowsAndResetsPaging") {
        val vm = usersVm()
        vm.fetchUsers()
        users.server("u1")
        vm.fetchUsers()
        users.server() // the list ended
        users.twoPhaseCalls.size shouldBe 2
        vm.users.value.map { it.uid } shouldBe listOf("u1")

        logout()

        vm.users.value shouldBe emptyList()
        vm.uiState.value shouldBe UsersUIState.Loading
        vm.fetchUsers() // paging reset: allowed again, on a fresh request
        users.twoPhaseCalls.size shouldBe 3
        (users.twoPhaseCalls[2] === users.twoPhaseCalls[1]) shouldBe false
    }

    test("users: sessionEnded_dropsResultOfReadInFlight") {
        val vm = usersVm()
        vm.fetchUsers()
        users.server("u1")
        vm.fetchUsers() // page two in flight
        logout()
        users.cached("old2", read = 1)
        users.server("old2", "old3", read = 1)
        vm.users.value shouldBe emptyList()
        vm.uiState.value shouldBe UsersUIState.Loading
    }

    test("users: staleSessionEnded_isIgnored") {
        val vm = usersVm()
        logout() // adopts gen 1
        vm.fetchUsers()
        users.server("u1")
        publish(OfflineHubEvent.SessionEnded(OfflineHub.gen)) // duplicate
        publish(OfflineHubEvent.SessionEnded(OfflineHub.gen - 1)) // older
        vm.users.value.map { it.uid } shouldBe listOf("u1")
        vm.uiState.value shouldBe UsersUIState.Content
    }

    test("users: cacheReset_clearsThenReloadsOnce") {
        val vm = usersVm()
        vm.fetchUsers()
        users.server("u1", "u2")
        cacheReset()
        vm.users.value shouldBe emptyList()
        users.twoPhaseCalls.size shouldBe 2
        users.server("u3")
        vm.users.value.map { it.uid } shouldBe listOf("u3")
        users.twoPhaseCalls.size shouldBe 2
    }

    test("users: cacheReset_reloadFailureOverRows_isQuiet") {
        val vm = usersVm()
        val errors = collect(vm.rowsKeptError)
        vm.fetchUsers()
        users.server("u1")
        cacheReset()
        users.cached("u1")
        users.error(onlineError)
        vm.users.value.map { it.uid } shouldBe listOf("u1")
        vm.uiState.value shouldBe UsersUIState.Content
        errors shouldBe emptyList()
    }

    test("users: offlineOff_noChange") {
        OfflineGate.testOverride = false
        users.singleResult = Result.success(listOf(MockFactory.createUser(uid = "u1")))
        val vm = usersVm()
        vm.fetchUsers()
        logout()
        cacheReset()
        vm.users.value.map { it.uid } shouldBe listOf("u1")
        vm.uiState.value shouldBe UsersUIState.Content
        users.singleCalls.size shouldBe 1
        users.twoPhaseCalls.size shouldBe 0
    }

    // ------------------------------------------------------------------ groups (init fetches page one)

    fun guids(vm: CometChatGroupsViewModel) = vm.groups.value.map { it.guid }

    test("groups: sessionEnded_clearsRowsAndResetsPaging") {
        val vm = groupsVm()
        groups.server("g1")
        vm.fetchGroups()
        groups.server() // the list ended
        groups.twoPhaseCalls.size shouldBe 2

        logout()

        vm.groups.value shouldBe emptyList()
        vm.uiState.value shouldBe GroupsUIState.Loading
        vm.fetchGroups()
        groups.twoPhaseCalls.size shouldBe 3
        (groups.twoPhaseCalls[2] === groups.twoPhaseCalls[1]) shouldBe false
    }

    test("groups: sessionEnded_dropsResultOfReadInFlight") {
        val vm = groupsVm()
        groups.server("g1")
        vm.fetchGroups() // page two in flight
        logout()
        groups.cached("old", read = 1)
        groups.server("old", read = 1)
        vm.groups.value shouldBe emptyList()
        vm.uiState.value shouldBe GroupsUIState.Loading
    }

    test("groups: staleSessionEnded_isIgnored") {
        val vm = groupsVm()
        groups.server("g1")
        publish(OfflineHubEvent.SessionEnded(OfflineHub.gen))
        guids(vm) shouldBe listOf("g1")
        vm.uiState.value.shouldBeInstanceOf<GroupsUIState.Content>()
    }

    test("groups: cacheReset_clearsThenReloadsOnce") {
        val vm = groupsVm()
        groups.server("g1", "g2")
        cacheReset()
        vm.groups.value shouldBe emptyList()
        groups.twoPhaseCalls.size shouldBe 2
        groups.server("g3")
        guids(vm) shouldBe listOf("g3")
        groups.twoPhaseCalls.size shouldBe 2
    }

    test("groups: cacheReset_reloadFailureOverRows_isQuiet") {
        val vm = groupsVm()
        val errors = collect(vm.rowsKeptError)
        groups.server("g1")
        cacheReset()
        groups.cached("g1")
        groups.error(onlineError)
        guids(vm) shouldBe listOf("g1")
        vm.uiState.value.shouldBeInstanceOf<GroupsUIState.Content>()
        errors shouldBe emptyList()
    }

    test("groups: offlineOff_noChange") {
        OfflineGate.testOverride = false
        groups.singleResult = Result.success(listOf(FakeGroupsRepository.group("g1")))
        val vm = groupsVm()
        logout()
        cacheReset()
        guids(vm) shouldBe listOf("g1")
        vm.uiState.value.shouldBeInstanceOf<GroupsUIState.Content>()
        groups.singleCalls.size shouldBe 1
        groups.twoPhaseCalls.size shouldBe 0
    }

    // ------------------------------------------------------------------ group members

    fun memberUids(vm: CometChatGroupMembersViewModel) = vm.members.value.map { it.uid }

    test("groupMembers: sessionEnded_clearsRowsAndResetsPaging") {
        val vm = membersVm()
        vm.fetchGroupMembers()
        members.server("m1")
        vm.fetchGroupMembers()
        members.server() // the list ended
        vm.hasMore.value shouldBe false
        val resetsBefore = members.resets

        logout()

        vm.members.value shouldBe emptyList()
        vm.uiState.value shouldBe GroupMembersUIState.Loading
        vm.hasMore.value shouldBe true
        members.resets shouldBe resetsBefore + 1 // the use case's request starts again from page one
        vm.fetchGroupMembers()
        members.reads.size shouldBe 3
    }

    test("groupMembers: sessionEnded_dropsResultOfReadInFlight") {
        val vm = membersVm()
        vm.fetchGroupMembers()
        members.server("m1")
        vm.fetchGroupMembers() // page two in flight
        logout()
        members.cached("old", read = 1)
        members.server("old", read = 1)
        vm.members.value shouldBe emptyList()
        vm.uiState.value shouldBe GroupMembersUIState.Loading
    }

    test("groupMembers: sessionEnded_dropsResultOfReadInFlight — plain fetch") {
        val vm = plainMembersVm()
        vm.fetchGroupMembers()
        heldMembers.calls.size shouldBe 1
        logout()
        heldMembers.complete(0, "old")
        dispatcher.scheduler.advanceUntilIdle()
        vm.members.value shouldBe emptyList()
        vm.uiState.value shouldBe GroupMembersUIState.Loading
    }

    test("groupMembers: sessionEnded_dropsResultOfReadInFlight — plain keyword search") {
        val vm = plainMembersVm()
        vm.searchGroupMembers("al")
        dispatcher.scheduler.advanceUntilIdle() // past the search debounce
        heldMembers.keywords shouldBe listOf("al")
        logout()
        // The search job was cancelled, but the read still completes (as an SDK callback does).
        heldMembers.complete(0, "old")
        dispatcher.scheduler.advanceUntilIdle()
        vm.members.value shouldBe emptyList()
        vm.uiState.value shouldBe GroupMembersUIState.Loading
    }

    test("groupMembers: staleSessionEnded_isIgnored") {
        val vm = membersVm()
        vm.fetchGroupMembers()
        members.server("m1")
        publish(OfflineHubEvent.SessionEnded(OfflineHub.gen))
        memberUids(vm) shouldBe listOf("m1")
        vm.uiState.value.shouldBeInstanceOf<GroupMembersUIState.Content>()
    }

    test("groupMembers: cacheReset_clearsThenReloadsOnce") {
        val vm = membersVm()
        vm.fetchGroupMembers()
        members.server("m1", "m2")
        cacheReset()
        vm.members.value shouldBe emptyList()
        members.reads.size shouldBe 2
        members.server("m3")
        memberUids(vm) shouldBe listOf("m3")
        members.reads.size shouldBe 2
    }

    test("groupMembers: cacheReset_reloadFailureOverRows_isQuiet") {
        val vm = membersVm()
        val errors = collect(vm.rowsKeptError)
        vm.fetchGroupMembers()
        members.server("m1")
        cacheReset()
        members.cached("m1")
        members.error(onlineError)
        memberUids(vm) shouldBe listOf("m1")
        vm.uiState.value.shouldBeInstanceOf<GroupMembersUIState.Content>()
        errors shouldBe emptyList()
    }

    test("groupMembers: offlineOff_noChange") {
        OfflineGate.testOverride = false
        members.singleResult = Result.success(listOf(FakeGroupMembersRepository.member("m1")))
        val vm = membersVm()
        vm.fetchGroupMembers()
        val resets = members.resets
        logout()
        cacheReset()
        memberUids(vm) shouldBe listOf("m1")
        vm.uiState.value.shouldBeInstanceOf<GroupMembersUIState.Content>()
        members.singleCalls.size shouldBe 1
        members.resets shouldBe resets
        members.reads.size shouldBe 0
    }
})

