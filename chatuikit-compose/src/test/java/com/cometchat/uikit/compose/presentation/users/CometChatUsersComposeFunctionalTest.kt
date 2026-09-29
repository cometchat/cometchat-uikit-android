package com.cometchat.uikit.compose.presentation.users

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.users.ui.CometChatUsers
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.state.UsersUIState
import com.cometchat.uikit.core.viewmodel.CometChatUsersViewModel
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
 * ENG-38679 — Compose CometChatUsers functional matrix (semantics + reflection-
 * drive of the VM's _uiState / _users). Same techniques as Conversations compose.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatUsersComposeFunctionalTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun newVm(): CometChatUsersViewModel = UsersTestVm.build()

    private fun user() = mock<User>().also { whenever(it.uid).thenReturn("pm-uid"); whenever(it.name).thenReturn("PM User") }

    private fun setFlow(vm: CometChatUsersViewModel, field: String, value: Any?) {
        val f = vm.javaClass.getDeclaredField(field).apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (f.get(vm) as MutableStateFlow<Any?>).value = value
    }

    private fun emit(vm: CometChatUsersViewModel, state: UsersUIState) {
        setFlow(vm, "_uiState", state)
        shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle()
    }

    private fun emitContent(vm: CometChatUsersViewModel, list: List<Any>) {
        setFlow(vm, "_users", list)
        setFlow(vm, "_uiState", UsersUIState.Content)
        shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle()
    }

    @Test
    fun title_showsInToolbar() {
        rule.setContent { CometChatTheme { CometChatUsers(usersViewModel = newVm(), title = "PM-Users") } }
        rule.waitForIdle(); rule.onNodeWithText("PM-Users").assertIsDisplayed()
    }

    @Test
    fun hideToolbar_removesTitle() {
        rule.setContent { CometChatTheme { CometChatUsers(usersViewModel = newVm(), title = "PM-Users", hideToolbar = true) } }
        rule.waitForIdle(); rule.onNodeWithText("PM-Users").assertDoesNotExist()
    }

    @Test
    fun searchPlaceholderText_reachesSearch() {
        rule.setContent { CometChatTheme { CometChatUsers(usersViewModel = newVm(), searchPlaceholderText = "PM-PLACEHOLDER") } }
        rule.waitForIdle(); rule.onNodeWithText("PM-PLACEHOLDER").assertIsDisplayed()
    }

    @Test
    fun loadingView_customSlotRenders() {
        val vm = newVm()
        rule.setContent { CometChatTheme { CometChatUsers(usersViewModel = vm, loadingView = { Text("PM-LOADING") }) } }
        emit(vm, UsersUIState.Empty); emit(vm, UsersUIState.Loading)
        rule.onNodeWithText("PM-LOADING").assertIsDisplayed()
    }

    @Test
    fun emptyView_customSlotRenders_andOnEmptyFires() {
        val vm = newVm(); var onEmptyFired = false
        rule.setContent { CometChatTheme { CometChatUsers(usersViewModel = vm, emptyView = { Text("PM-EMPTY") }, onEmpty = { onEmptyFired = true }) } }
        emit(vm, UsersUIState.Empty)
        rule.onNodeWithText("PM-EMPTY").assertIsDisplayed(); assertTrue("onEmpty should fire", onEmptyFired)
    }

    @Test
    fun errorView_customSlotRenders_andOnErrorFires() {
        val vm = newVm(); var captured: CometChatException? = null
        rule.setContent { CometChatTheme { CometChatUsers(usersViewModel = vm, errorView = { _ -> Text("PM-ERROR") }, onError = { captured = it }) } }
        val ex = CometChatException("PM", "boom"); emit(vm, UsersUIState.Error(ex))
        rule.onNodeWithText("PM-ERROR").assertIsDisplayed(); assertEquals(ex, captured)
    }

    @Test
    fun onLoad_firesOnContentState() {
        val vm = newVm(); var loaded: List<*>? = null
        rule.setContent { CometChatTheme { CometChatUsers(usersViewModel = vm, onLoad = { loaded = it }) } }
        emitContent(vm, emptyList()); assertTrue("onLoad should fire on Content", loaded != null)
    }

    @Test
    fun itemView_customBypassRenders() {
        val vm = newVm()
        rule.setContent { CometChatTheme { CometChatUsers(usersViewModel = vm, itemView = { Text("PM-ITEM") }) } }
        emitContent(vm, listOf(user()))
        rule.onNodeWithText("PM-ITEM").assertIsDisplayed()
    }

    @Test
    fun onItemClick_firesOnItemClick() {
        val vm = newVm(); var clicked = false
        rule.setContent { CometChatTheme { CometChatUsers(usersViewModel = vm, titleView = { Text("PM-TITLE") }, onItemClick = { clicked = true }) } }
        emitContent(vm, listOf(user()))
        rule.onNodeWithText("PM-TITLE").performClick(); assertTrue("onItemClick should fire", clicked)
    }
}
