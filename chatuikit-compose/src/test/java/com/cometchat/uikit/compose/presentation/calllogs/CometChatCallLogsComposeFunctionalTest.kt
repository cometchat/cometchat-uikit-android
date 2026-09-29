package com.cometchat.uikit.compose.presentation.calllogs

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.calls.exceptions.CometChatException
import com.cometchat.calls.model.CallLog
import com.cometchat.uikit.compose.presentation.calllogs.ui.CometChatCallLogs
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.state.CallLogsUIState
import com.cometchat.uikit.core.viewmodel.CometChatCallLogsViewModel
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-38680 — Compose CometChatCallLogs functional matrix (semantics + reflection-
 * drive of the VM's _uiState / _callLogs). Safe VM (fake repo, listeners off).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatCallLogsComposeFunctionalTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun newVm(): CometChatCallLogsViewModel = CallLogsTestVm.build()

    private fun setFlow(vm: CometChatCallLogsViewModel, field: String, value: Any?) {
        val f = vm.javaClass.getDeclaredField(field).apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (f.get(vm) as MutableStateFlow<Any?>).value = value
    }

    private fun emit(vm: CometChatCallLogsViewModel, state: CallLogsUIState) {
        setFlow(vm, "_uiState", state); shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle()
    }

    private fun emitContent(vm: CometChatCallLogsViewModel, list: List<CallLog>) {
        setFlow(vm, "_callLogs", list)
        setFlow(vm, "_uiState", CallLogsUIState.Content(list))
        shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle()
    }

    @Test
    fun title_showsInToolbar() {
        rule.setContent { CometChatTheme { CometChatCallLogs(viewModel = newVm(), title = "PM-Calls") } }
        rule.waitForIdle(); rule.onNodeWithText("PM-Calls").assertIsDisplayed()
    }

    @Test
    fun hideToolbar_removesTitle() {
        rule.setContent { CometChatTheme { CometChatCallLogs(viewModel = newVm(), title = "PM-Calls", hideToolbar = true) } }
        rule.waitForIdle(); rule.onNodeWithText("PM-Calls").assertDoesNotExist()
    }

    @Test
    fun loadingView_customSlotRenders() {
        val vm = newVm()
        rule.setContent { CometChatTheme { CometChatCallLogs(viewModel = vm, loadingView = { Text("PM-LOADING") }) } }
        emit(vm, CallLogsUIState.Empty); emit(vm, CallLogsUIState.Loading)
        rule.onNodeWithText("PM-LOADING").assertIsDisplayed()
    }

    @Test
    fun emptyView_customSlotRenders_andOnEmptyFires() {
        val vm = newVm(); var onEmptyFired = false
        rule.setContent { CometChatTheme { CometChatCallLogs(viewModel = vm, emptyView = { Text("PM-EMPTY") }, onEmpty = { onEmptyFired = true }) } }
        emit(vm, CallLogsUIState.Empty)
        rule.onNodeWithText("PM-EMPTY").assertIsDisplayed(); assertTrue("onEmpty should fire", onEmptyFired)
    }

    @Test
    fun errorView_customSlotRenders_andOnErrorFires() {
        val vm = newVm(); var captured: CometChatException? = null
        rule.setContent { CometChatTheme { CometChatCallLogs(viewModel = vm, errorView = { _ -> Text("PM-ERROR") }, onError = { captured = it }) } }
        val ex = CometChatException("PM", "boom"); emit(vm, CallLogsUIState.Error(ex))
        rule.onNodeWithText("PM-ERROR").assertIsDisplayed(); assertEquals(ex, captured)
    }

    @Test
    fun onLoad_firesOnContentState() {
        val vm = newVm(); var loaded: List<*>? = null
        rule.setContent { CometChatTheme { CometChatCallLogs(viewModel = vm, onLoad = { loaded = it }) } }
        emitContent(vm, emptyList()); assertTrue("onLoad should fire on Content", loaded != null)
    }

    @Test
    fun listItemView_customBypassRenders() {
        val vm = newVm()
        rule.setContent { CometChatTheme { CometChatCallLogs(viewModel = vm, listItemView = { Text("PM-ITEM") }) } }
        emitContent(vm, listOf(mock<CallLog>()))
        rule.onNodeWithText("PM-ITEM").assertIsDisplayed()
    }
}
