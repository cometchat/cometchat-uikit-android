package com.cometchat.uikit.compose.presentation.shared

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.GroupsRequest
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.conversations.ui.CometChatConversations
import com.cometchat.uikit.compose.presentation.groupmembers.GroupMembersTestVm
import com.cometchat.uikit.compose.presentation.groupmembers.ui.CometChatGroupMembers
import com.cometchat.uikit.compose.presentation.groups.ui.CometChatGroups
import com.cometchat.uikit.compose.presentation.search.ui.CometChatSearch
import com.cometchat.uikit.compose.presentation.users.ui.CometChatUsers
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.domain.repository.ConversationListRepository
import com.cometchat.uikit.core.domain.repository.GroupsRepository
import com.cometchat.uikit.core.domain.repository.UsersRepository
import com.cometchat.uikit.core.domain.usecase.DeleteConversationUseCase
import com.cometchat.uikit.core.domain.usecase.FetchGroupsUseCase
import com.cometchat.uikit.core.domain.usecase.FetchUsersUseCase
import com.cometchat.uikit.core.domain.usecase.GetConversationListUseCase
import com.cometchat.uikit.core.domain.usecase.JoinGroupUseCase
import com.cometchat.uikit.core.domain.usecase.RefreshConversationListUseCase
import com.cometchat.uikit.core.domain.usecase.SearchUsersUseCase
import com.cometchat.uikit.core.factory.CometChatSearchViewModelFactory
import com.cometchat.uikit.core.state.GroupMembersUIState
import com.cometchat.uikit.core.state.GroupsUIState
import com.cometchat.uikit.core.state.SearchUIState
import com.cometchat.uikit.core.state.UIState
import com.cometchat.uikit.core.state.UsersUIState
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import com.cometchat.uikit.core.viewmodel.CometChatGroupsViewModel
import com.cometchat.uikit.core.viewmodel.CometChatSearchViewModel
import com.cometchat.uikit.core.viewmodel.CometChatUsersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * D83/D84 (Compose): Users, Groups, Group Members, Conversations and Search. A failure with nothing on
 * screen shows the app's error view (the kit's when none is set) and fires `onError`; a failure
 * while rows stay on screen fires `onError` with no error view; an empty answer shows the app's
 * empty view. The ViewModel's flows are driven directly; the read logic is covered in core.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class OfflineListStatesComposeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val offline = CometChatException("ERROR_INTERNET_UNAVAILABLE", "offline")

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle()
    }

    private fun field(vm: Any, name: String): Any? {
        var c: Class<*>? = vm.javaClass
        while (c != null) {
            val f = runCatching { c!!.getDeclaredField(name) }.getOrNull()
            if (f != null) { f.isAccessible = true; return f.get(vm) }
            c = c.superclass
        }
        error("no field $name")
    }

    private fun setState(vm: Any, value: Any) {
        @Suppress("UNCHECKED_CAST")
        (field(vm, "_uiState") as MutableStateFlow<Any?>).value = value
        idle()
    }

    private fun emitRowsKeptError(vm: Any) {
        @Suppress("UNCHECKED_CAST")
        (field(vm, "_rowsKeptError") as Channel<CometChatException>).trySend(offline)
        idle()
    }

    private fun usersVm(): CometChatUsersViewModel {
        val repo = object : UsersRepository {
            override suspend fun getUsers(request: UsersRequest): Result<List<User>> = Result.success(emptyList())
            override fun hasMoreUsers(): Boolean = false
        }
        return CometChatUsersViewModel(FetchUsersUseCase(repo), SearchUsersUseCase(repo), enableListeners = false)
    }

    private fun groupsVm(): CometChatGroupsViewModel {
        val repo = object : GroupsRepository {
            override suspend fun fetchGroups(request: GroupsRequest): Result<List<Group>> = Result.success(emptyList())
            override suspend fun joinGroup(groupId: String, groupType: String, password: String?): Result<Group> =
                Result.failure(IllegalStateException("unused"))
            override fun hasMoreGroups(): Boolean = false
        }
        return CometChatGroupsViewModel(FetchGroupsUseCase(repo), mock<JoinGroupUseCase>(), enableListeners = false)
    }

    private fun conversationsVm(): CometChatConversationsViewModel {
        val repo = object : ConversationListRepository {
            override suspend fun getConversations(request: ConversationsRequest): Result<List<Conversation>> =
                Result.success(emptyList())
            override suspend fun deleteConversation(conversationWith: String, conversationType: String) = Result.success(Unit)
            override suspend fun markAsDelivered(conversation: Conversation) = Result.success(Unit)
            override fun hasMoreConversations() = false
        }
        return CometChatConversationsViewModel(
            GetConversationListUseCase(repo), DeleteConversationUseCase(repo), RefreshConversationListUseCase(repo),
            enableListeners = false
        )
    }

    private fun searchVm(): CometChatSearchViewModel =
        CometChatSearchViewModelFactory().create(CometChatSearchViewModel::class.java)

    private fun group() = mock<Group>().also { whenever(it.guid).thenReturn("d83-guid") }

    /** Mounts a list with the app's error and empty views and an onError recorder. */
    private fun mount(content: @Composable (onError: (CometChatException) -> Unit) -> Unit): MutableList<CometChatException> {
        val errors = mutableListOf<CometChatException>()
        rule.setContent { CometChatTheme { content { errors += it } } }
        idle()
        errors.clear()
        return errors
    }

    private fun errorWithNothingShown(vm: Any, loading: Any, error: Any, errors: MutableList<CometChatException>) {
        setState(vm, loading)
        setState(vm, error)
        rule.onNodeWithText("APP-ERROR").assertIsDisplayed()
        assertEquals(listOf(offline), errors)
    }

    private fun rowsKept(vm: Any, content: Any, errors: MutableList<CometChatException>) {
        setState(vm, content)
        emitRowsKeptError(vm)
        assertEquals(listOf(offline), errors)
        rule.onNodeWithText("APP-ERROR").assertDoesNotExist()
    }

    private fun emptyAnswer(vm: Any, loading: Any, empty: Any) {
        setState(vm, loading)
        setState(vm, empty)
        rule.onNodeWithText("APP-EMPTY").assertIsDisplayed()
        rule.onNodeWithText("APP-ERROR").assertDoesNotExist()
    }

    // ------------------------------------------------------------------ Users

    private fun mountUsers(vm: CometChatUsersViewModel) = mount { onError ->
        CometChatUsers(usersViewModel = vm, emptyView = { Text("APP-EMPTY") }, errorView = { Text("APP-ERROR") }, onError = onError)
    }

    @Test fun users_errorWithNothingShown_appErrorViewAndOnError() = usersVm().let { vm ->
        errorWithNothingShown(vm, UsersUIState.Loading, UsersUIState.Error(offline), mountUsers(vm))
    }

    @Test fun users_rowsKept_onErrorOnly() = usersVm().let { vm -> rowsKept(vm, UsersUIState.Content, mountUsers(vm)) }

    @Test fun users_emptyAnswer_appEmptyView() = usersVm().let { vm ->
        mountUsers(vm); emptyAnswer(vm, UsersUIState.Loading, UsersUIState.Empty)
    }

    // ------------------------------------------------------------------ Groups

    private fun mountGroups(vm: CometChatGroupsViewModel) = mount { onError ->
        CometChatGroups(viewModel = vm, emptyView = { Text("APP-EMPTY") }, errorView = { Text("APP-ERROR") }, onError = onError)
    }

    @Test fun groups_errorWithNothingShown_appErrorViewAndOnError() = groupsVm().let { vm ->
        errorWithNothingShown(vm, GroupsUIState.Loading, GroupsUIState.Error(offline), mountGroups(vm))
    }

    @Test fun groups_rowsKept_onErrorOnly() = groupsVm().let { vm ->
        rowsKept(vm, GroupsUIState.Content(emptyList()), mountGroups(vm))
    }

    @Test fun groups_emptyAnswer_appEmptyView() = groupsVm().let { vm ->
        mountGroups(vm); emptyAnswer(vm, GroupsUIState.Loading, GroupsUIState.Empty)
    }

    // ------------------------------------------------------------------ Group Members

    private fun mountMembers(vm: Any) = mount { onError ->
        CometChatGroupMembers(
            group = group(), viewModel = vm as com.cometchat.uikit.core.viewmodel.CometChatGroupMembersViewModel,
            emptyView = { Text("APP-EMPTY") }, errorView = { Text("APP-ERROR") }, onError = onError
        )
    }

    @Test fun members_errorWithNothingShown_appErrorViewAndOnError() = GroupMembersTestVm.build().let { vm ->
        errorWithNothingShown(vm, GroupMembersUIState.Loading, GroupMembersUIState.Error(offline), mountMembers(vm))
    }

    @Test fun members_rowsKept_onErrorOnly() = GroupMembersTestVm.build().let { vm ->
        rowsKept(vm, GroupMembersUIState.Content(emptyList()), mountMembers(vm))
    }

    @Test fun members_emptyAnswer_appEmptyView() = GroupMembersTestVm.build().let { vm ->
        mountMembers(vm); emptyAnswer(vm, GroupMembersUIState.Loading, GroupMembersUIState.Empty)
    }

    // ------------------------------------------------------------------ Conversations

    private fun mountConversations(vm: CometChatConversationsViewModel) = mount { onError ->
        CometChatConversations(
            conversationListViewModel = vm, emptyView = { Text("APP-EMPTY") }, errorView = { Text("APP-ERROR") },
            onError = onError
        )
    }

    @Test fun conversations_errorWithNothingShown_appErrorViewAndOnError() = conversationsVm().let { vm ->
        errorWithNothingShown(vm, UIState.Loading, UIState.Error(offline), mountConversations(vm))
    }

    @Test fun conversations_rowsKept_onErrorOnly() = conversationsVm().let { vm ->
        rowsKept(vm, UIState.Content(emptyList()), mountConversations(vm))
    }

    @Test fun conversations_emptyAnswer_appEmptyView() = conversationsVm().let { vm ->
        mountConversations(vm); emptyAnswer(vm, UIState.Loading, UIState.Empty)
    }

    // ------------------------------------------------------------------ Search

    private fun mountSearch(vm: CometChatSearchViewModel) = mount { onError ->
        CometChatSearch(viewModel = vm, emptyView = { Text("APP-EMPTY") }, errorView = { Text("APP-ERROR") }, onError = onError)
    }

    @Test fun search_errorWithNothingShown_appErrorViewAndOnError() = searchVm().let { vm ->
        errorWithNothingShown(vm, SearchUIState.Loading, SearchUIState.Error(offline), mountSearch(vm))
    }

    @Test fun search_rowsKept_onErrorOnly() = searchVm().let { vm -> rowsKept(vm, SearchUIState.Content, mountSearch(vm)) }

    @Test fun search_emptyAnswer_appEmptyView() = searchVm().let { vm ->
        mountSearch(vm); emptyAnswer(vm, SearchUIState.Loading, SearchUIState.Empty)
    }

    // ------------------------------------------------------------------ D84: offline states

    private fun setFlag(vm: Any, name: String, value: Boolean) {
        @Suppress("UNCHECKED_CAST")
        (field(vm, name) as MutableStateFlow<Boolean>).value = value
        idle()
    }

    private fun str(res: Int) = rule.activity.getString(res)

    /** Rule 3: offline with nothing shown and no app error view: the kit's offline state; onError once. */
    private fun offlineState(vm: Any, loading: Any, error: Any, errors: MutableList<CometChatException>, loadRes: Int) {
        setState(vm, loading)
        setFlag(vm, "_connectToLoad", true)
        setState(vm, error)
        rule.onNodeWithText(str(loadRes)).assertIsDisplayed()
        rule.onNodeWithText("APP-EMPTY").assertDoesNotExist()
        assertEquals(listOf(offline), errors)
    }

    /**
     * Rule 7: an offline search miss with no app empty view: "Connect to search" with the subtitle
     * "Search needs an internet connection." (D84, D-355), no onError.
     */
    private fun connectToSearch(
        vm: Any, loading: Any, empty: Any, errors: MutableList<CometChatException>, flag: String, res: Int,
        subtitleRes: Int = com.cometchat.uikit.core.R.string.cometchat_connect_to_search_subtitle
    ) {
        setState(vm, loading)
        setFlag(vm, flag, true)
        setState(vm, empty)
        rule.onNodeWithText(str(res)).assertIsDisplayed()
        assertEquals("Search needs an internet connection.", str(subtitleRes))
        rule.onNodeWithText(str(subtitleRes)).assertIsDisplayed()
        assertEquals(emptyList<CometChatException>(), errors)
    }

    @Test fun users_d84_offlineState_noAppErrorView() = usersVm().let { vm ->
        val errors = mount { onError -> CometChatUsers(usersViewModel = vm, emptyView = { Text("APP-EMPTY") }, onError = onError) }
        offlineState(vm, UsersUIState.Loading, UsersUIState.Error(offline), errors, com.cometchat.uikit.core.R.string.cometchat_connect_to_load_users)
    }

    @Test fun users_d84_appErrorViewWinsOverOffline() = usersVm().let { vm ->
        val errors = mountUsers(vm)
        setState(vm, UsersUIState.Loading)
        setFlag(vm, "_connectToLoad", true)
        setState(vm, UsersUIState.Error(offline))
        rule.onNodeWithText("APP-ERROR").assertIsDisplayed()
        rule.onNodeWithText(str(com.cometchat.uikit.core.R.string.cometchat_connect_to_load_users)).assertDoesNotExist()
        assertEquals(listOf(offline), errors)
    }

    @Test fun users_d84_connectToSearch() = usersVm().let { vm ->
        val errors = mount { onError -> CometChatUsers(usersViewModel = vm, onError = onError) }
        connectToSearch(vm, UsersUIState.Loading, UsersUIState.Empty, errors, "_connectToSearch",
            com.cometchat.uikit.core.R.string.cometchat_connect_to_search)
    }

    @Test fun groups_d84_offlineState_noAppErrorView() = groupsVm().let { vm ->
        val errors = mount { onError -> CometChatGroups(viewModel = vm, emptyView = { Text("APP-EMPTY") }, onError = onError) }
        offlineState(vm, GroupsUIState.Loading, GroupsUIState.Error(offline), errors, com.cometchat.uikit.core.R.string.cometchat_connect_to_load_groups)
    }

    @Test fun groups_d84_connectToSearch() = groupsVm().let { vm ->
        val errors = mount { onError -> CometChatGroups(viewModel = vm, onError = onError) }
        connectToSearch(vm, GroupsUIState.Loading, GroupsUIState.Empty, errors, "_connectToSearch",
            com.cometchat.uikit.core.R.string.cometchat_connect_to_search)
    }

    @Test fun members_d84_offlineState_noAppErrorView() = GroupMembersTestVm.build().let { vm ->
        val errors = mount { onError ->
            CometChatGroupMembers(
                group = group(), viewModel = vm as com.cometchat.uikit.core.viewmodel.CometChatGroupMembersViewModel,
                emptyView = { Text("APP-EMPTY") }, onError = onError
            )
        }
        offlineState(vm, GroupMembersUIState.Loading, GroupMembersUIState.Error(offline), errors,
            com.cometchat.uikit.core.R.string.cometchat_connect_to_load_members)
    }

    @Test fun members_d84_connectToSearch() = GroupMembersTestVm.build().let { vm ->
        val errors = mount { onError ->
            CometChatGroupMembers(
                group = group(), viewModel = vm as com.cometchat.uikit.core.viewmodel.CometChatGroupMembersViewModel,
                onError = onError
            )
        }
        connectToSearch(vm, GroupMembersUIState.Loading, GroupMembersUIState.Empty, errors, "_connectToSearch",
            com.cometchat.uikit.core.R.string.cometchat_connect_to_search)
    }

    @Test fun conversations_d84_offlineState_noAppErrorView() = conversationsVm().let { vm ->
        val errors = mount { onError ->
            CometChatConversations(conversationListViewModel = vm, emptyView = { Text("APP-EMPTY") }, onError = onError)
        }
        offlineState(vm, UIState.Loading, UIState.Error(offline), errors, com.cometchat.uikit.core.R.string.cometchat_connect_to_load_chats)
    }

    @Test fun conversations_d84_connectToSearch() = conversationsVm().let { vm ->
        val errors = mount { onError -> CometChatConversations(conversationListViewModel = vm, onError = onError) }
        connectToSearch(vm, UIState.Loading, UIState.Empty, errors, "_connectToSearch",
            com.cometchat.uikit.core.R.string.cometchat_connect_to_search)
    }

    @Test fun search_d84_connectToSearch() = searchVm().let { vm ->
        val errors = mount { onError -> CometChatSearch(viewModel = vm, onError = onError) }
        connectToSearch(vm, SearchUIState.Loading, SearchUIState.Empty, errors, "_showConnectToSearch",
            com.cometchat.uikit.core.R.string.cometchat_connect_to_search_messages,
            com.cometchat.uikit.core.R.string.cometchat_connect_to_search_messages_subtitle)
    }
}
