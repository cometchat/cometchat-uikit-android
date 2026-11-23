package com.cometchat.uikit.core.offline

import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.uikit.core.domain.repository.GroupMembersRepository
import com.cometchat.uikit.core.domain.repository.GroupMembersTwoPhaseRepository
import com.cometchat.uikit.core.domain.usecase.BanGroupMemberUseCase
import com.cometchat.uikit.core.domain.usecase.ChangeMemberScopeUseCase
import com.cometchat.uikit.core.domain.usecase.FetchGroupMembersUseCase
import com.cometchat.uikit.core.domain.usecase.KickGroupMemberUseCase
import com.cometchat.uikit.core.state.GroupMembersUIState
import com.cometchat.uikit.core.viewmodel.CometChatGroupMembersViewModel
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
internal class FakeGroupMembersRepository : GroupMembersRepository, GroupMembersTwoPhaseRepository {
    val singleCalls = mutableListOf<String?>()
    var singleResult: Result<List<GroupMember>> = Result.success(emptyList())
    val twoPhaseKeywords = mutableListOf<String?>()
    val reads = mutableListOf<Channel<TwoPhaseResult<GroupMember>>>()
    var resets = 0

    override suspend fun fetchGroupMembers(guid: String, limit: Int, searchKeyword: String?): Result<List<GroupMember>> {
        singleCalls += searchKeyword
        return singleResult
    }

    override suspend fun kickMember(guid: String, uid: String): Result<Unit> = Result.success(Unit)
    override suspend fun banMember(guid: String, uid: String): Result<Unit> = Result.success(Unit)
    override suspend fun changeMemberScope(guid: String, uid: String, scope: String): Result<Unit> = Result.success(Unit)
    override fun hasMore(): Boolean = true
    override fun resetRequest() { resets++ }

    override fun fetchGroupMembersTwoPhase(guid: String, limit: Int, searchKeyword: String?): Flow<TwoPhaseResult<GroupMember>> {
        twoPhaseKeywords += searchKeyword
        return Channel<TwoPhaseResult<GroupMember>>(Channel.UNLIMITED).also { reads += it }.consumeAsFlow()
    }

    fun cached(vararg uids: String, read: Int = reads.lastIndex) {
        reads[read].trySend(TwoPhaseResult.Cached(uids.map { member(it) }))
    }

    fun server(vararg uids: String, read: Int = reads.lastIndex) {
        reads[read].trySend(TwoPhaseResult.Server(uids.map { member(it) }))
    }

    fun error(code: String = SnapshotListRead.NOT_CONNECTED, read: Int = reads.lastIndex) {
        reads[read].trySend(TwoPhaseResult.Error(CometChatException(code, "e")))
    }

    companion object {
        fun member(uid: String): GroupMember = GroupMember(uid, "participant")
    }
}

/**
 * ENG-41132 — group members list two-phase (README §3.9, BEH-SNAP-01..07, BEH-READ-01): cached
 * first paint, server reconcile by uid, "Connect to search" for an offline keyword, reconnect
 * re-read without blanking, rows kept offline, stale generations dropped, offline-off unchanged.
 * The default data source builds its request from guid/limit/keyword only, so the keyword is the
 * list's no-cache read (BEH-SNAP-04).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupMembersOfflineListTest : FunSpec({

    lateinit var repo: FakeGroupMembersRepository
    lateinit var dispatcher: TestDispatcher

    fun uids(vm: CometChatGroupMembersViewModel) = vm.members.value.map { it.uid }

    // D83 rule 1: what the view hands to onError while rows stay on screen.
    fun collectErrors(vm: CometChatGroupMembersViewModel): List<CometChatException> {
        val errors = mutableListOf<CometChatException>()
        CoroutineScope(dispatcher).launch { vm.rowsKeptError.collect { errors += it } }
        return errors
    }

    fun vm(): CometChatGroupMembersViewModel = CometChatGroupMembersViewModel(
        FetchGroupMembersUseCase(repo),
        mock<KickGroupMemberUseCase>(),
        mock<BanGroupMemberUseCase>(),
        mock<ChangeMemberScopeUseCase>(),
        enableListeners = false
    ).also { it.setGroup(Group().apply { guid = "g1"; name = "G" }) }

    beforeTest {
        dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        OfflineGate.testOverride = true
        repo = FakeGroupMembersRepository()
    }
    afterTest {
        OfflineGate.testOverride = null
        Dispatchers.resetMain()
    }

    test("cached page is the first paint; the server page reconciles with no duplicates") {
        val vm = vm()
        vm.fetchGroupMembers()
        vm.uiState.value shouldBe GroupMembersUIState.Loading
        repo.cached("m1", "m2")
        vm.uiState.value.shouldBeInstanceOf<GroupMembersUIState.Content>()
        uids(vm) shouldBe listOf("m1", "m2")
        repo.server("m1", "m2", "m3")
        uids(vm) shouldBe listOf("m1", "m2", "m3")
        vm.hasMore.value shouldBe true
        repo.singleCalls.size shouldBe 0
    }

    test("an empty cached page does not end the list") {
        val vm = vm()
        vm.fetchGroupMembers()
        repo.cached()
        repo.error()
        vm.hasMore.value shouldBe true
        vm.fetchGroupMembers()
        repo.twoPhaseKeywords.size shouldBe 2
    }

    test("D84 rule 7: offline keyword search with no saved match is Connect to search, no onError") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchGroupMembers()
        repo.server("m1")
        vm.searchGroupMembers("bo")
        dispatcher.scheduler.advanceUntilIdle() // past the search debounce
        repo.twoPhaseKeywords.last() shouldBe "bo"
        repo.cached()
        repo.error()
        vm.members.value shouldBe emptyList()
        vm.uiState.value shouldBe GroupMembersUIState.Empty
        vm.connectToSearch.value shouldBe true
        vm.connectToLoad.value shouldBe false
        errors shouldBe emptyList()
    }

    test("reconnect refresh re-reads two-phase and never blanks the list") {
        val vm = vm()
        vm.fetchGroupMembers()
        repo.server("m1", "m2")
        vm.refreshList()
        uids(vm) shouldBe listOf("m1", "m2") // kept while the re-read runs
        repo.cached()
        uids(vm) shouldBe listOf("m1", "m2")
        repo.server("m2", "m7")
        uids(vm) shouldBe listOf("m2", "m7")
        repo.twoPhaseKeywords shouldBe listOf(null, null)
    }

    test("D84: a failed load-more page keeps the rows, shows no error state, and reports no onError") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchGroupMembers()
        repo.server("m1")
        vm.fetchGroupMembers()
        repo.cached()
        repo.error()
        uids(vm) shouldBe listOf("m1")
        vm.uiState.value.shouldBeInstanceOf<GroupMembersUIState.Content>()
        vm.isLoading.value shouldBe false
        errors shouldBe emptyList()
    }

    test("D84 rule 5: a first page whose cached rows show reports an online server failure once") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchGroupMembers()
        repo.cached("m1")
        repo.error(code = "ERR_SERVER")
        uids(vm) shouldBe listOf("m1")
        errors.map { it.code } shouldBe listOf("ERR_SERVER")
    }

    test("D84 rule 2: a first page whose cached rows show reports nothing when it fails offline") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchGroupMembers()
        repo.cached("m1")
        repo.error()
        uids(vm) shouldBe listOf("m1")
        vm.connectToLoad.value shouldBe false
        errors shouldBe emptyList()
    }

    test("a stale generation never touches the screen") {
        val vm = vm()
        vm.fetchGroupMembers()
        repo.server("m1")
        vm.fetchGroupMembers() // read 1, in flight
        vm.refreshList() // read 2 supersedes it
        repo.cached("stale", read = 1)
        repo.server("stale", read = 1)
        uids(vm) shouldBe listOf("m1")
        repo.server("fresh", read = 2)
        uids(vm) shouldBe listOf("fresh")
    }

    test("offline off: one round through today's path, today's output") {
        OfflineGate.testOverride = false
        repo.singleResult = Result.success(listOf(FakeGroupMembersRepository.member("m1")))
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchGroupMembers()
        repo.singleCalls shouldBe listOf<String?>(null)
        repo.twoPhaseKeywords.size shouldBe 0
        uids(vm) shouldBe listOf("m1")
        vm.uiState.value.shouldBeInstanceOf<GroupMembersUIState.Content>()
        errors shouldBe emptyList()
    }

    test("D84 rules 3, 8: nothing shown and offline is the offline state; it reloads once on reconnect") {
        val vm = vm()
        vm.fetchGroupMembers()
        repo.cached()
        repo.error()
        vm.members.value shouldBe emptyList()
        vm.uiState.value.shouldBeInstanceOf<GroupMembersUIState.Error>()
        vm.connectToLoad.value shouldBe true

        vm.onConnected()
        repo.twoPhaseKeywords shouldBe listOf(null, null)
        repo.server("m1")
        uids(vm) shouldBe listOf("m1")
        vm.uiState.value.shouldBeInstanceOf<GroupMembersUIState.Content>()
        vm.connectToLoad.value shouldBe false
    }

    test("D84: the server answering an empty page is the empty state") {
        val vm = vm()
        vm.fetchGroupMembers()
        repo.cached()
        repo.server()
        vm.uiState.value shouldBe GroupMembersUIState.Empty
    }

    test("D84 rule 8: an open offline search re-runs that search on reconnect") {
        val vm = vm()
        vm.fetchGroupMembers()
        repo.server("m1")
        vm.searchGroupMembers("bo")
        dispatcher.scheduler.advanceUntilIdle()
        repo.error()
        vm.uiState.value shouldBe GroupMembersUIState.Empty
        vm.connectToSearch.value shouldBe true

        vm.onConnected()
        dispatcher.scheduler.advanceUntilIdle()
        repo.twoPhaseKeywords.last() shouldBe "bo"
        repo.server("bob")
        uids(vm) shouldBe listOf("bob")
    }

    test("D84: any other failure with nothing shown is the error state too") {
        val vm = vm()
        vm.fetchGroupMembers()
        repo.cached()
        repo.error(code = "ERR_SERVER")
        (vm.uiState.value as GroupMembersUIState.Error).exception.code shouldBe "ERR_SERVER"
    }

    // ---- D84 rule 5: a refresh the kit starts by itself stays quiet over rows.

    test("D84 rule 5: a reconnect refresh failing over rows keeps them and reports nothing") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchGroupMembers()
        repo.server("m1", "m2")

        vm.onConnected()
        repo.twoPhaseKeywords shouldBe listOf(null, null)
        repo.cached("m1", "m2")
        repo.error()
        uids(vm) shouldBe listOf("m1", "m2")
        vm.uiState.value.shouldBeInstanceOf<GroupMembersUIState.Content>()
        errors shouldBe emptyList()

        // The quiet mark covered that refresh only: the app's next refresh reports an online error.
        vm.refreshList()
        repo.cached("m1")
        repo.error(code = "ERR_SERVER")
        errors.map { it.code } shouldBe listOf("ERR_SERVER")
    }

    test("D84 rule 5: the reconnect reload from the error view failing is the error state") {
        val vm = vm()
        val errors = collectErrors(vm)
        vm.fetchGroupMembers()
        repo.cached()
        repo.error()
        vm.uiState.value.shouldBeInstanceOf<GroupMembersUIState.Error>()

        vm.onConnected()
        repo.cached()
        repo.error(code = "ERR_SERVER")
        (vm.uiState.value as GroupMembersUIState.Error).exception.code shouldBe "ERR_SERVER"
        errors shouldBe emptyList()
    }
})
