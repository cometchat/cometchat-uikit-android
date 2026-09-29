package com.cometchat.uikit.compose.presentation.groupmembers

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.uikit.compose.presentation.groupmembers.ui.CometChatGroupMembers
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.state.GroupMembersUIState
import com.cometchat.uikit.core.viewmodel.CometChatGroupMembersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-38679 — Compose CometChatGroupMembers functional matrix (semantics +
 * reflection-drive of the VM's _uiState / _groupMembers). Safe VM (fake repo,
 * listeners off). Only onError is wired in the composable (no onLoad/onEmpty).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatGroupMembersComposeFunctionalTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun newVm(): CometChatGroupMembersViewModel = GroupMembersTestVm.build()
    private fun group() = mock<Group>().also { whenever(it.guid).thenReturn("pm-guid") }
    private fun member() = mock<GroupMember>().also { whenever(it.uid).thenReturn("pm-uid"); whenever(it.name).thenReturn("PM Member"); whenever(it.scope).thenReturn("participant") }

    private fun setFlow(vm: CometChatGroupMembersViewModel, field: String, value: Any?) {
        val f = vm.javaClass.getDeclaredField(field).apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (f.get(vm) as MutableStateFlow<Any?>).value = value
    }

    private fun emit(vm: CometChatGroupMembersViewModel, state: GroupMembersUIState) {
        setFlow(vm, "_uiState", state); shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle()
    }

    private fun emitContent(vm: CometChatGroupMembersViewModel, list: List<GroupMember>) {
        setFlow(vm, "_members", list)
        setFlow(vm, "_uiState", GroupMembersUIState.Content(list))
        shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle()
    }

    @Test
    fun title_showsInToolbar() {
        rule.setContent { CometChatTheme { CometChatGroupMembers(group = group(), viewModel = newVm(), title = "PM-Members") } }
        rule.waitForIdle(); rule.onNodeWithText("PM-Members").assertIsDisplayed()
    }

    @Test
    fun hideToolbar_removesTitle() {
        rule.setContent { CometChatTheme { CometChatGroupMembers(group = group(), viewModel = newVm(), title = "PM-Members", hideToolbar = true) } }
        rule.waitForIdle(); rule.onNodeWithText("PM-Members").assertDoesNotExist()
    }

    @Test
    fun searchPlaceholderText_reachesSearch() {
        rule.setContent { CometChatTheme { CometChatGroupMembers(group = group(), viewModel = newVm(), searchPlaceholderText = "PM-PLACEHOLDER") } }
        rule.waitForIdle(); rule.onNodeWithText("PM-PLACEHOLDER").assertIsDisplayed()
    }

    @Test
    fun loadingView_customSlotRenders() {
        val vm = newVm()
        rule.setContent { CometChatTheme { CometChatGroupMembers(group = group(), viewModel = vm, loadingView = { Text("PM-LOADING") }) } }
        emit(vm, GroupMembersUIState.Empty); emit(vm, GroupMembersUIState.Loading)
        rule.onNodeWithText("PM-LOADING").assertIsDisplayed()
    }

    @Test
    fun emptyView_customSlotRenders() {
        val vm = newVm()
        rule.setContent { CometChatTheme { CometChatGroupMembers(group = group(), viewModel = vm, emptyView = { Text("PM-EMPTY") }) } }
        emit(vm, GroupMembersUIState.Empty)
        rule.onNodeWithText("PM-EMPTY").assertIsDisplayed()
    }

    @Test
    fun errorView_customSlotRenders_andOnErrorFires() {
        val vm = newVm(); var captured: CometChatException? = null
        rule.setContent { CometChatTheme { CometChatGroupMembers(group = group(), viewModel = vm, errorView = { _ -> Text("PM-ERROR") }, onError = { captured = it }) } }
        val ex = CometChatException("PM", "boom"); emit(vm, GroupMembersUIState.Error(ex))
        rule.onNodeWithText("PM-ERROR").assertIsDisplayed(); assertEquals(ex, captured)
    }

    @Test
    fun itemView_customBypassRenders() {
        val vm = newVm()
        rule.setContent { CometChatTheme { CometChatGroupMembers(group = group(), viewModel = vm, listItemView = { Text("PM-ITEM") }) } }
        emitContent(vm, listOf(member()))
        rule.onNodeWithText("PM-ITEM").assertIsDisplayed()
    }

    @Test
    fun onItemClick_firesOnItemClick() {
        val vm = newVm(); var clicked = false
        rule.setContent { CometChatTheme { CometChatGroupMembers(group = group(), viewModel = vm, titleView = { Text("PM-TITLE") }, onItemClick = { clicked = true }) } }
        emitContent(vm, listOf(member()))
        rule.onNodeWithText("PM-TITLE").performClick(); assertTrue("onItemClick should fire", clicked)
    }
}
