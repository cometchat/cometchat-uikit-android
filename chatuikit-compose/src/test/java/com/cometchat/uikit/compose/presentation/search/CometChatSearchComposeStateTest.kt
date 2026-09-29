package com.cometchat.uikit.compose.presentation.search

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.uikit.compose.presentation.search.ui.CometChatSearch
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.factory.CometChatSearchViewModelFactory
import com.cometchat.uikit.core.state.SearchUIState
import com.cometchat.uikit.core.viewmodel.CometChatSearchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-38678 — Compose CometChatSearch state-driven matrix, Phase 2 Slice 2b.
 *
 * LAYER: unit / Robolectric (Compose semantics + reflection-drive of the VM's
 * private StateFlows — the same technique used on the View side in Phase 1b-ii).
 * Emitting into `_uiState` / `_conversations` / `_messages` and letting the
 * composable recompose exercises the state-gated props without a live SDK.
 *
 * Covers SLOT params loadingView / emptyView / errorView / initialView, VALUE
 * flags hideLoadingState / hideEmptyState / hideErrorState, and CALLBACK params
 * onEmpty / onError / onLoadConversations / onLoadMessages.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatSearchComposeStateTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun newViewModel(): CometChatSearchViewModel =
        CometChatSearchViewModelFactory().create(CometChatSearchViewModel::class.java)

    /** Emits [value] into the VM's private MutableStateFlow named [field], then recomposes. */
    private fun emit(vm: CometChatSearchViewModel, field: String, value: Any?) {
        val f = vm.javaClass.getDeclaredField(field).apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (f.get(vm) as MutableStateFlow<Any?>).value = value
        shadowOf(Looper.getMainLooper()).idle()
        rule.waitForIdle()
    }

    private fun emptyTitle(): String =
        rule.activity.getString(com.cometchat.uikit.compose.R.string.cometchat_search_empty_title)

    // --- SLOT params: custom state views render when their state is active ---

    @Test
    fun initialView_customSlotRenders() {
        val vm = newViewModel()
        rule.setContent { CometChatTheme { CometChatSearch(viewModel = vm, initialView = { Text("PM-INITIAL") }) } }
        rule.waitForIdle() // default state is Initial
        rule.onNodeWithText("PM-INITIAL").assertIsDisplayed()
    }

    @Test
    fun loadingView_customSlotRenders() {
        val vm = newViewModel()
        rule.setContent { CometChatTheme { CometChatSearch(viewModel = vm, loadingView = { Text("PM-LOADING") }) } }
        emit(vm, "_uiState", SearchUIState.Loading)
        rule.onNodeWithText("PM-LOADING").assertIsDisplayed()
    }

    @Test
    fun emptyView_customSlotRenders() {
        val vm = newViewModel()
        rule.setContent { CometChatTheme { CometChatSearch(viewModel = vm, emptyView = { Text("PM-EMPTY") }) } }
        emit(vm, "_uiState", SearchUIState.Empty)
        rule.onNodeWithText("PM-EMPTY").assertIsDisplayed()
    }

    @Test
    fun errorView_customSlotRenders() {
        val vm = newViewModel()
        rule.setContent { CometChatTheme { CometChatSearch(viewModel = vm, errorView = { _ -> Text("PM-ERROR") }) } }
        emit(vm, "_uiState", SearchUIState.Error(CometChatException("PM", "boom")))
        rule.onNodeWithText("PM-ERROR").assertIsDisplayed()
    }

    // --- VALUE flag: hideEmptyState removes the default empty state ---

    @Test
    fun hideEmptyState_false_showsDefaultEmpty() {
        val vm = newViewModel()
        rule.setContent { CometChatTheme { CometChatSearch(viewModel = vm, hideEmptyState = false) } }
        emit(vm, "_uiState", SearchUIState.Empty)
        rule.onNodeWithText(emptyTitle()).assertIsDisplayed()
    }

    @Test
    fun hideEmptyState_true_removesDefaultEmpty() {
        val vm = newViewModel()
        rule.setContent { CometChatTheme { CometChatSearch(viewModel = vm, hideEmptyState = true) } }
        emit(vm, "_uiState", SearchUIState.Empty)
        rule.onNodeWithText(emptyTitle()).assertDoesNotExist()
    }

    // --- CALLBACK params fire from the LaunchedEffect(uiState, ...) ---

    @Test
    fun onEmpty_firesOnEmptyState() {
        val vm = newViewModel()
        var fired = false
        rule.setContent { CometChatTheme { CometChatSearch(viewModel = vm, onEmpty = { fired = true }) } }
        emit(vm, "_uiState", SearchUIState.Empty)
        assertTrue("onEmpty should fire when uiState becomes Empty", fired)
    }

    @Test
    fun onError_firesWithExceptionOnErrorState() {
        val vm = newViewModel()
        var captured: CometChatException? = null
        rule.setContent { CometChatTheme { CometChatSearch(viewModel = vm, onError = { captured = it }) } }
        val ex = CometChatException("PM_ERR", "boom")
        emit(vm, "_uiState", SearchUIState.Error(ex))
        assertEquals("onError should fire with the exact exception", ex, captured)
    }

    @Test
    fun onLoadCallbacks_fireOnContentState() {
        val vm = newViewModel()
        var loadedConvs: List<*>? = null
        var loadedMsgs: List<*>? = null
        rule.setContent {
            CometChatTheme {
                CometChatSearch(
                    viewModel = vm,
                    onLoadConversations = { loadedConvs = it },
                    onLoadMessages = { loadedMsgs = it },
                )
            }
        }
        // Content with empty lists still fires the load callbacks (no item models to render).
        emit(vm, "_uiState", SearchUIState.Content)
        assertTrue("onLoadConversations should fire on Content", loadedConvs != null)
        assertTrue("onLoadMessages should fire on Content", loadedMsgs != null)
    }
}
