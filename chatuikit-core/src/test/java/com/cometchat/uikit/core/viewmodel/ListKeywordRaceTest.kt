package com.cometchat.uikit.core.viewmodel

import com.cometchat.chat.core.GroupsRequest
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.GroupsRepository
import com.cometchat.uikit.core.domain.repository.UsersRepository
import com.cometchat.uikit.core.domain.usecase.FetchGroupsUseCase
import com.cometchat.uikit.core.domain.usecase.FetchUsersUseCase
import com.cometchat.uikit.core.domain.usecase.JoinGroupUseCase
import com.cometchat.uikit.core.domain.usecase.SearchUsersUseCase
import com.cometchat.uikit.core.offline.OfflineGate
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.mock

/** Every single-phase read waits until the test answers it, so reads can finish out of order. */
private class GatedUsersRepository : UsersRepository {
    val calls = mutableListOf<Pair<UsersRequest, CompletableDeferred<Result<List<User>>>>>()

    override suspend fun getUsers(request: UsersRequest): Result<List<User>> =
        CompletableDeferred<Result<List<User>>>().also { calls += request to it }.await()

    override fun hasMoreUsers(): Boolean = true

    fun answer(call: Int, vararg uids: String) {
        calls[call].second.complete(Result.success(uids.map { MockFactory.createUser(uid = it) }))
    }
}

private class GatedGroupsRepository : GroupsRepository {
    val calls = mutableListOf<Pair<GroupsRequest, CompletableDeferred<Result<List<Group>>>>>()

    override suspend fun fetchGroups(request: GroupsRequest): Result<List<Group>> =
        CompletableDeferred<Result<List<Group>>>().also { calls += request to it }.await()

    override suspend fun joinGroup(groupId: String, groupType: String, password: String?): Result<Group> =
        Result.failure(IllegalStateException("unused"))

    override fun hasMoreGroups(): Boolean = true

    fun answer(call: Int, vararg guids: String) {
        calls[call].second.complete(Result.success(guids.map { Group(it, "Group $it", "public", null, null, null) }))
    }
}

/**
 * ENG-41551 E (offline off): a changed search shows only the current keyword's results. A plain-list
 * load started by clearing the keyword, or any earlier load, that finishes after a newer keyword's
 * read is dropped, and it never leaves pagination blocked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ListKeywordRaceTest : FunSpec({

    lateinit var dispatcher: TestDispatcher

    beforeTest {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        OfflineGate.testOverride = false
    }
    afterTest {
        OfflineGate.testOverride = null
        Dispatchers.resetMain()
    }

    test("users: a stale plain-list load finishing after a new keyword is ignored") {
        val repo = GatedUsersRepository()
        val vm = CometChatUsersViewModel(FetchUsersUseCase(repo), SearchUsersUseCase(repo), enableListeners = false)

        vm.searchUsers("") // clearing the keyword starts a plain-list load (call 0)
        repo.calls.size shouldBe 1
        vm.searchUsers("al") // the next keyword (call 1, after the debounce)
        dispatcher.scheduler.advanceUntilIdle()
        repo.calls.size shouldBe 2
        repo.calls[1].first.searchKeyword shouldBe "al"

        repo.answer(1, "al1")
        vm.users.value.map { it.uid } shouldBe listOf("al1")
        repo.answer(0, "plain1", "plain2") // the stale load ends last
        vm.users.value.map { it.uid } shouldBe listOf("al1")

        vm.fetchUsers() // the stale load released the fetch guard: the next page is read
        repo.calls.size shouldBe 3
    }

    test("users: a refresh supersedes a search still in flight") {
        val repo = GatedUsersRepository()
        val vm = CometChatUsersViewModel(FetchUsersUseCase(repo), SearchUsersUseCase(repo), enableListeners = false)
        vm.searchUsers("al")
        dispatcher.scheduler.advanceUntilIdle()
        vm.refreshList()
        repo.calls.size shouldBe 2
        repo.answer(1, "u1")
        repo.answer(0, "al1")
        vm.users.value.map { it.uid } shouldBe listOf("u1")
    }

    test("groups: stale loads finishing after a new keyword are ignored") {
        val repo = GatedGroupsRepository()
        val vm = CometChatGroupsViewModel(FetchGroupsUseCase(repo), mock<JoinGroupUseCase>(), enableListeners = false)
        repo.calls.size shouldBe 1 // the first page, from init (call 0)

        vm.searchGroups("") // clearing the keyword starts a plain-list load (call 1)
        repo.calls.size shouldBe 2
        vm.searchGroups("fam") // the next keyword (call 2, after the debounce)
        dispatcher.scheduler.advanceUntilIdle()
        repo.calls.size shouldBe 3
        repo.calls[2].first.searchKeyword shouldBe "fam"

        repo.answer(2, "f1")
        vm.groups.value.map { it.guid } shouldBe listOf("f1")
        repo.answer(1, "plain1") // stale, ends last
        repo.answer(0, "old1") // stale, ends last
        vm.groups.value.map { it.guid } shouldBe listOf("f1")

        vm.fetchGroups() // the stale load released the fetch guard: the next page is read
        repo.calls.size shouldBe 4
    }

    test("users: a load-more while a search is in flight reads nothing until the search lands") {
        val repo = GatedUsersRepository()
        val vm = CometChatUsersViewModel(FetchUsersUseCase(repo), SearchUsersUseCase(repo), enableListeners = false)
        vm.fetchUsers()
        repo.answer(0, "u1", "u2")

        vm.searchUsers("al")
        vm.fetchUsers() // during the debounce: the old list's page is not read
        dispatcher.scheduler.advanceUntilIdle()
        repo.calls.size shouldBe 2
        vm.fetchUsers() // while the search read runs
        repo.calls.size shouldBe 2

        repo.answer(1, "al1")
        vm.users.value.map { it.uid } shouldBe listOf("al1")
        vm.fetchUsers() // now pages the search results
        repo.calls.size shouldBe 3
        repo.calls[2].first.searchKeyword shouldBe "al"
    }

    test("groups: a load-more while a search is in flight reads nothing until the search lands") {
        val repo = GatedGroupsRepository()
        val vm = CometChatGroupsViewModel(FetchGroupsUseCase(repo), mock<JoinGroupUseCase>(), enableListeners = false)
        repo.answer(0, "g1", "g2")

        vm.searchGroups("fam")
        vm.fetchGroups() // during the debounce: no page 1 of the plain list
        dispatcher.scheduler.advanceUntilIdle()
        repo.calls.size shouldBe 2
        vm.fetchGroups() // while the search read runs
        repo.calls.size shouldBe 2

        repo.answer(1, "f1")
        vm.groups.value.map { it.guid } shouldBe listOf("f1")
        vm.fetchGroups() // now pages the search results
        repo.calls.size shouldBe 3
        repo.calls[2].first.searchKeyword shouldBe "fam"
    }
})
