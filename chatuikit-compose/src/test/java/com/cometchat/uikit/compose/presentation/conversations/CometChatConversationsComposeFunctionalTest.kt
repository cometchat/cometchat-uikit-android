package com.cometchat.uikit.compose.presentation.conversations

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.compose.presentation.conversations.ui.CometChatConversations
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.factory.CometChatConversationsViewModelFactory
import com.cometchat.uikit.core.state.UIState
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-38679 — Compose CometChatConversations functional matrix.
 *
 * LAYER: unit/Robolectric (Compose semantics + reflection-drive of the VM's
 * _uiState StateFlow). Same techniques as the Search compose functional/state
 * tests: toolbar/search via semantics, states + callbacks by driving UIState,
 * item props via a mock Conversation in Content.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatConversationsComposeFunctionalTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun newVm(): CometChatConversationsViewModel =
        CometChatConversationsViewModelFactory().create(CometChatConversationsViewModel::class.java)

    private fun setFlow(vm: CometChatConversationsViewModel, field: String, value: Any?) {
        val f = vm.javaClass.getDeclaredField(field).apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (f.get(vm) as MutableStateFlow<Any?>).value = value
    }

    private fun emit(vm: CometChatConversationsViewModel, state: UIState) {
        setFlow(vm, "_uiState", state)
        shadowOf(Looper.getMainLooper()).idle()
        rule.waitForIdle()
    }

    /** Drives the list-render path: the item list comes from the _conversations flow. */
    private fun emitContent(vm: CometChatConversationsViewModel, list: List<Any>) {
        setFlow(vm, "_conversations", list)
        setFlow(vm, "_uiState", UIState.Content(@Suppress("UNCHECKED_CAST") (list as List<com.cometchat.chat.models.Conversation>)))
        shadowOf(Looper.getMainLooper()).idle()
        rule.waitForIdle()
    }

    // --- toolbar / search (semantics) ---

    @Test
    fun title_showsInToolbar() {
        rule.setContent { CometChatTheme { CometChatConversations(conversationListViewModel = newVm(), title = "PM-Conv") } }
        rule.waitForIdle()
        rule.onNodeWithText("PM-Conv").assertIsDisplayed()
    }

    @Test
    fun hideToolbar_removesTitle() {
        rule.setContent { CometChatTheme { CometChatConversations(conversationListViewModel = newVm(), title = "PM-Conv", hideToolbar = true) } }
        rule.waitForIdle()
        rule.onNodeWithText("PM-Conv").assertDoesNotExist()
    }

    @Test
    fun hideSearchBox_removesSearch() {
        rule.setContent { CometChatTheme { CometChatConversations(conversationListViewModel = newVm(), hideSearchBox = true) } }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Search conversations").assertDoesNotExist()
    }

    @Test
    fun searchPlaceholderText_reachesSearch() {
        rule.setContent { CometChatTheme { CometChatConversations(conversationListViewModel = newVm(), searchPlaceholderText = "PM-PLACEHOLDER") } }
        rule.waitForIdle()
        rule.onNodeWithText("PM-PLACEHOLDER").assertIsDisplayed()
    }

    // --- state custom views (VM-drive) ---

    @Test
    fun loadingView_customSlotRenders() {
        val vm = newVm()
        rule.setContent { CometChatTheme { CometChatConversations(conversationListViewModel = vm, loadingView = { Text("PM-LOADING") }) } }
        emit(vm, UIState.Loading)
        rule.onNodeWithText("PM-LOADING").assertIsDisplayed()
    }

    @Test
    fun emptyView_customSlotRenders_andOnEmptyFires() {
        val vm = newVm()
        var onEmptyFired = false
        rule.setContent { CometChatTheme { CometChatConversations(conversationListViewModel = vm, emptyView = { Text("PM-EMPTY") }, onEmpty = { onEmptyFired = true }) } }
        emit(vm, UIState.Empty)
        rule.onNodeWithText("PM-EMPTY").assertIsDisplayed()
        assertTrue("onEmpty should fire", onEmptyFired)
    }

    @Test
    fun errorView_customSlotRenders_andOnErrorFires() {
        val vm = newVm()
        var captured: CometChatException? = null
        rule.setContent { CometChatTheme { CometChatConversations(conversationListViewModel = vm, errorView = { _ -> Text("PM-ERROR") }, onError = { captured = it }) } }
        val ex = CometChatException("PM", "boom")
        emit(vm, UIState.Error(ex))
        rule.onNodeWithText("PM-ERROR").assertIsDisplayed()
        assertEquals(ex, captured)
    }

    @Test
    fun onLoad_firesOnContentState() {
        val vm = newVm()
        var loaded: List<*>? = null
        rule.setContent { CometChatTheme { CometChatConversations(conversationListViewModel = vm, onLoad = { loaded = it }) } }
        emit(vm, UIState.Content(emptyList()))
        assertTrue("onLoad should fire on Content", loaded != null)
    }

    // --- Content item props ---

    @Test
    fun itemView_customBypassRenders() {
        val vm = newVm()
        rule.setContent { CometChatTheme { CometChatConversations(conversationListViewModel = vm, itemView = { _, _ -> Text("PM-ITEM") }) } }
        emitContent(vm, listOf(mock<Conversation>()))
        rule.onNodeWithText("PM-ITEM").assertIsDisplayed()
    }

    @Test
    fun onItemClick_firesOnItemClick() {
        val vm = newVm()
        var clicked = false
        rule.setContent {
            CometChatTheme {
                CometChatConversations(
                    conversationListViewModel = vm,
                    titleView = { _, _ -> Text("PM-TITLE") },
                    onItemClick = { clicked = true },
                )
            }
        }
        emitContent(vm, listOf(mock<Conversation>()))
        rule.onNodeWithText("PM-TITLE").performClick()
        assertTrue("onItemClick should fire on item click", clicked)
    }
}
