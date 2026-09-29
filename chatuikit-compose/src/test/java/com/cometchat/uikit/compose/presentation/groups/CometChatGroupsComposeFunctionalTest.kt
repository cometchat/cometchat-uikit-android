package com.cometchat.uikit.compose.presentation.groups

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
import com.cometchat.uikit.compose.presentation.groups.ui.CometChatGroups
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.state.GroupsUIState
import com.cometchat.uikit.core.viewmodel.CometChatGroupsViewModel
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
 * ENG-38679 — Compose CometChatGroups functional matrix (semantics + reflection-
 * drive of the VM's _uiState / _groups). Safe VM (fake repo, listeners off).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatGroupsComposeFunctionalTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun newVm(): CometChatGroupsViewModel = GroupsTestVm.build()

    private fun group() = mock<Group>().also { whenever(it.guid).thenReturn("pm-guid"); whenever(it.name).thenReturn("PM Group") }

    private fun setFlow(vm: CometChatGroupsViewModel, field: String, value: Any?) {
        val f = vm.javaClass.getDeclaredField(field).apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (f.get(vm) as MutableStateFlow<Any?>).value = value
    }

    private fun emit(vm: CometChatGroupsViewModel, state: GroupsUIState) {
        setFlow(vm, "_uiState", state); shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle()
    }

    private fun emitContent(vm: CometChatGroupsViewModel, list: List<Group>) {
        setFlow(vm, "_groups", list)
        setFlow(vm, "_uiState", GroupsUIState.Content(list))
        shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle()
    }

    @Test
    fun title_showsInToolbar() {
        rule.setContent { CometChatTheme { CometChatGroups(viewModel = newVm(), title = "PM-Groups") } }
        rule.waitForIdle(); rule.onNodeWithText("PM-Groups").assertIsDisplayed()
    }

    @Test
    fun hideToolbar_removesTitle() {
        rule.setContent { CometChatTheme { CometChatGroups(viewModel = newVm(), title = "PM-Groups", hideToolbar = true) } }
        rule.waitForIdle(); rule.onNodeWithText("PM-Groups").assertDoesNotExist()
    }

    @Test
    fun searchPlaceholderText_reachesSearch() {
        rule.setContent { CometChatTheme { CometChatGroups(viewModel = newVm(), searchPlaceholderText = "PM-PLACEHOLDER") } }
        rule.waitForIdle(); rule.onNodeWithText("PM-PLACEHOLDER").assertIsDisplayed()
    }

    @Test
    fun loadingView_customSlotRenders() {
        val vm = newVm()
        rule.setContent { CometChatTheme { CometChatGroups(viewModel = vm, loadingView = { Text("PM-LOADING") }) } }
        emit(vm, GroupsUIState.Empty); emit(vm, GroupsUIState.Loading)
        rule.onNodeWithText("PM-LOADING").assertIsDisplayed()
    }

    @Test
    fun emptyView_customSlotRenders_andOnEmptyFires() {
        val vm = newVm(); var onEmptyFired = false
        rule.setContent { CometChatTheme { CometChatGroups(viewModel = vm, emptyView = { Text("PM-EMPTY") }, onEmpty = { onEmptyFired = true }) } }
        emit(vm, GroupsUIState.Empty)
        rule.onNodeWithText("PM-EMPTY").assertIsDisplayed(); assertTrue("onEmpty should fire", onEmptyFired)
    }

    @Test
    fun errorView_customSlotRenders_andOnErrorFires() {
        val vm = newVm(); var captured: CometChatException? = null
        rule.setContent { CometChatTheme { CometChatGroups(viewModel = vm, errorView = { _ -> Text("PM-ERROR") }, onError = { captured = it }) } }
        val ex = CometChatException("PM", "boom"); emit(vm, GroupsUIState.Error(ex))
        rule.onNodeWithText("PM-ERROR").assertIsDisplayed(); assertEquals(ex, captured)
    }

    @Test
    fun onLoad_firesOnContentState() {
        val vm = newVm(); var loaded: List<*>? = null
        rule.setContent { CometChatTheme { CometChatGroups(viewModel = vm, onLoad = { loaded = it }) } }
        emitContent(vm, emptyList()); assertTrue("onLoad should fire on Content", loaded != null)
    }

    @Test
    fun itemView_customBypassRenders() {
        val vm = newVm()
        rule.setContent { CometChatTheme { CometChatGroups(viewModel = vm, itemView = { Text("PM-ITEM") }) } }
        emitContent(vm, listOf(group()))
        rule.onNodeWithText("PM-ITEM").assertIsDisplayed()
    }

    @Test
    fun onItemClick_firesOnItemClick() {
        val vm = newVm(); var clicked = false
        rule.setContent { CometChatTheme { CometChatGroups(viewModel = vm, titleView = { Text("PM-TITLE") }, onItemClick = { clicked = true }) } }
        emitContent(vm, listOf(group()))
        rule.onNodeWithText("PM-TITLE").performClick(); assertTrue("onItemClick should fire", clicked)
    }
}
