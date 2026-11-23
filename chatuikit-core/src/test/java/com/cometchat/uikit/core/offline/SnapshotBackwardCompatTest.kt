package com.cometchat.uikit.core.offline

import com.cometchat.chat.core.GroupsRequest
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.data.datasource.GroupMembersDataSource
import com.cometchat.uikit.core.data.datasource.GroupsDataSource
import com.cometchat.uikit.core.data.datasource.UsersDataSource
import com.cometchat.uikit.core.data.repository.GroupMembersRepositoryImpl
import com.cometchat.uikit.core.data.repository.GroupsRepositoryImpl
import com.cometchat.uikit.core.data.repository.UsersRepositoryImpl
import com.cometchat.uikit.core.domain.usecase.FetchGroupMembersUseCase
import com.cometchat.uikit.core.domain.usecase.FetchGroupsUseCase
import com.cometchat.uikit.core.domain.usecase.FetchUsersUseCase
import com.cometchat.uikit.core.domain.usecase.JoinGroupUseCase
import com.cometchat.uikit.core.domain.usecase.SearchUsersUseCase
import com.cometchat.uikit.core.viewmodel.CometChatGroupMembersViewModel
import com.cometchat.uikit.core.viewmodel.CometChatGroupsViewModel
import com.cometchat.uikit.core.viewmodel.CometChatUsersViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * ENG-41132 backward compatibility:
 * - a subclassed use case gets no two-phase repository, so the customer's `invoke` override keeps
 *   running with offline on;
 * - a custom data source without the two-phase interface goes through the repository's
 *   single-server-phase fallback, so the customer's fetch is called.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SnapshotBackwardCompatTest : FunSpec({

    lateinit var dispatcher: TestDispatcher

    beforeTest {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        OfflineGate.testOverride = true
    }
    afterTest {
        OfflineGate.testOverride = null
        Dispatchers.resetMain()
    }

    // ---- (a) subclassed use cases keep the customer's override ----

    test("subclassed FetchUsersUseCase: no two-phase repository; the override runs with offline on") {
        val repo = FakeUsersRepository()
        var overrideCalls = 0
        val custom = object : FetchUsersUseCase(repo) {
            override suspend fun invoke(request: UsersRequest): Result<List<User>> {
                overrideCalls++
                return Result.success(emptyList())
            }
        }
        custom.twoPhaseRepository shouldBe null
        FetchUsersUseCase(repo).twoPhaseRepository shouldNotBe null
        CometChatUsersViewModel(custom, SearchUsersUseCase(repo), enableListeners = false).fetchUsers()
        overrideCalls shouldBe 1
        repo.twoPhaseCalls.size shouldBe 0
    }

    test("subclassed SearchUsersUseCase: no two-phase repository; the override runs with offline on") {
        val repo = FakeUsersRepository()
        var overrideKeyword: String? = null
        val custom = object : SearchUsersUseCase(repo) {
            override suspend fun invoke(searchKeyword: String, builder: UsersRequest.UsersRequestBuilder): Result<List<User>> {
                overrideKeyword = searchKeyword
                return Result.success(emptyList())
            }
        }
        custom.twoPhaseRepository shouldBe null
        CometChatUsersViewModel(FetchUsersUseCase(repo), custom, enableListeners = false).searchUsers("al")
        dispatcher.scheduler.advanceUntilIdle() // past the search debounce
        overrideKeyword shouldBe "al"
        repo.twoPhaseCalls.size shouldBe 0
    }

    test("subclassed FetchGroupsUseCase: no two-phase repository; the override runs with offline on") {
        val repo = FakeGroupsRepository()
        var overrideCalls = 0
        val custom = object : FetchGroupsUseCase(repo) {
            override suspend fun invoke(request: GroupsRequest): Result<List<Group>> {
                overrideCalls++
                return Result.success(emptyList())
            }
        }
        custom.twoPhaseRepository shouldBe null
        CometChatGroupsViewModel(custom, mock<JoinGroupUseCase>(), enableListeners = false) // fetches from init
        overrideCalls shouldBe 1
        repo.twoPhaseCalls.size shouldBe 0
    }

    test("subclassed FetchGroupMembersUseCase: no two-phase repository; the override runs with offline on") {
        val repo = FakeGroupMembersRepository()
        var overrideCalls = 0
        val custom = object : FetchGroupMembersUseCase(repo) {
            override suspend fun invoke(guid: String, limit: Int, searchKeyword: String?): Result<List<GroupMember>> {
                overrideCalls++
                return Result.success(emptyList())
            }
        }
        custom.twoPhaseRepository shouldBe null
        val vm = CometChatGroupMembersViewModel(custom, mock(), mock(), mock(), enableListeners = false)
        vm.setGroup(Group().apply { guid = "g1"; name = "G" })
        vm.fetchGroupMembers()
        overrideCalls shouldBe 1
        repo.twoPhaseKeywords.size shouldBe 0
    }

    // ---- (b) a custom data source takes the single-server-phase fallback ----

    test("UsersRepositoryImpl with a custom data source: one server phase from the customer's fetch") {
        runTest {
            val rows = listOf(mock<User>())
            val ds = mock<UsersDataSource>()
            whenever(ds.fetchUsers(any())).thenReturn(rows)
            val request = mock<UsersRequest>()
            UsersRepositoryImpl(ds).getUsersTwoPhase(request).toList() shouldBe listOf(TwoPhaseResult.Server(rows))
            verify(ds).fetchUsers(request)
        }
    }

    test("GroupsRepositoryImpl with a custom data source: one server phase from the customer's fetch") {
        runTest {
            val rows = listOf(mock<Group>())
            val ds = mock<GroupsDataSource>()
            whenever(ds.fetchGroups(any())).thenReturn(rows)
            val request = mock<GroupsRequest>()
            GroupsRepositoryImpl(ds).fetchGroupsTwoPhase(request).toList() shouldBe listOf(TwoPhaseResult.Server(rows))
            verify(ds).fetchGroups(request)
        }
    }

    test("GroupMembersRepositoryImpl with a custom data source: one server phase from the customer's fetch") {
        runTest {
            val rows = listOf(mock<GroupMember>())
            val ds = mock<GroupMembersDataSource>()
            whenever(ds.fetchGroupMembers("g1", 30, "bo")).thenReturn(rows)
            GroupMembersRepositoryImpl(ds).fetchGroupMembersTwoPhase("g1", 30, "bo").toList() shouldBe
                listOf(TwoPhaseResult.Server(rows))
            verify(ds).fetchGroupMembers("g1", 30, "bo")
        }
    }
})
