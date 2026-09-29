package com.cometchat.uikit.compose.presentation.search

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.uikit.core.constants.SearchScope
import com.cometchat.uikit.compose.presentation.search.ui.CometChatSearch
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.factory.CometChatSearchViewModelFactory
import com.cometchat.uikit.core.state.SearchUIState
import com.cometchat.uikit.core.viewmodel.CometChatSearchViewModel
import com.cometchat.uikit.compose.presentation.shared.formatters.CometChatTextFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-38678 — Compose CometChatSearch, Phase 2 Slice 2d.
 *
 * LAYER: unit / Robolectric (Compose semantics + reflection-drive of the VM).
 * The remaining params: hide flags for loading/error, uid/guid config, text
 * formatters, and the Content-state item props (custom item view, per-slot
 * sub-views, result-click callback) driven with a real Conversation model.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatSearchComposeSlice2dTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun newViewModel(): CometChatSearchViewModel =
        CometChatSearchViewModelFactory().create(CometChatSearchViewModel::class.java)

    private fun emit(vm: CometChatSearchViewModel, field: String, value: Any?) {
        val f = vm.javaClass.getDeclaredField(field).apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (f.get(vm) as MutableStateFlow<Any?>).value = value
        shadowOf(Looper.getMainLooper()).idle()
        rule.waitForIdle()
    }

    private fun vmField(vm: CometChatSearchViewModel, name: String): Any? =
        vm.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(vm)

    /** Drives Content with a single conversation on screen (mock bypasses the private ctor). */
    private fun emitOneConversation(vm: CometChatSearchViewModel) {
        emit(vm, "_conversations", listOf(mock<Conversation>()))
        emit(vm, "_uiState", SearchUIState.Content)
    }

    // --- 2d-i: quick flags + config ---

    @Test
    fun hideLoadingState_true_hidesLoadingSlot() {
        val vm = newViewModel()
        rule.setContent {
            CometChatTheme {
                CometChatSearch(viewModel = vm, hideLoadingState = true, loadingView = { Text("PM-LOADING") })
            }
        }
        emit(vm, "_uiState", SearchUIState.Loading)
        rule.onNodeWithText("PM-LOADING").assertDoesNotExist()
    }

    @Test
    fun hideErrorState_true_hidesErrorSlot() {
        val vm = newViewModel()
        rule.setContent {
            CometChatTheme {
                CometChatSearch(viewModel = vm, hideErrorState = true, errorView = { _ -> Text("PM-ERROR") })
            }
        }
        emit(vm, "_uiState", SearchUIState.Error(com.cometchat.chat.exceptions.CometChatException("E", "e")))
        rule.onNodeWithText("PM-ERROR").assertDoesNotExist()
    }

    @Test
    fun uid_reachesTheViewModel() {
        val vm = newViewModel()
        rule.setContent { CometChatTheme { CometChatSearch(viewModel = vm, uid = "user-42") } }
        rule.waitForIdle()
        assertEquals("user-42", vmField(vm, "uid"))
    }

    @Test
    fun guid_reachesTheViewModel() {
        val vm = newViewModel()
        rule.setContent { CometChatTheme { CometChatSearch(viewModel = vm, guid = "group-42") } }
        rule.waitForIdle()
        assertEquals("group-42", vmField(vm, "guid"))
    }

    @Test
    fun textFormatters_areAccepted_andSearchRenders() {
        val vm = newViewModel()
        rule.setContent {
            CometChatTheme {
                CometChatSearch(viewModel = vm, textFormatters = listOf(mock<CometChatTextFormatter>()))
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Start Your Search").assertIsDisplayed()
    }

    // --- 2d-ii: Content-state item props ---

    @Test
    fun conversationItemView_customBypassRenders() {
        val vm = newViewModel()
        rule.setContent {
            CometChatTheme {
                CometChatSearch(viewModel = vm, conversationItemView = { Text("PM-CONV-ITEM") })
            }
        }
        emitOneConversation(vm)
        rule.onNodeWithText("PM-CONV-ITEM").assertIsDisplayed()
    }

    @Test
    fun conversationSubViews_renderInDefaultItem() {
        val vm = newViewModel()
        rule.setContent {
            CometChatTheme {
                CometChatSearch(
                    viewModel = vm,
                    conversationLeadingView = { Text("PM-LEAD") },
                    conversationTitleView = { Text("PM-TITLE") },
                    conversationSubtitleView = { Text("PM-SUB") },
                    conversationTrailingView = { Text("PM-TRAIL") },
                )
            }
        }
        emitOneConversation(vm)
        rule.onNodeWithText("PM-LEAD").assertIsDisplayed()
        rule.onNodeWithText("PM-TITLE").assertIsDisplayed()
        rule.onNodeWithText("PM-SUB").assertIsDisplayed()
        rule.onNodeWithText("PM-TRAIL").assertIsDisplayed()
    }

    @Test
    fun onConversationClick_firesOnItemClick() {
        val vm = newViewModel()
        var clicked = false
        rule.setContent {
            CometChatTheme {
                // default item (no conversationItemView) is the click-wrapped row;
                // a title sub-view gives a stable node inside it to click.
                CometChatSearch(
                    viewModel = vm,
                    conversationTitleView = { Text("PM-TITLE") },
                    onConversationClick = { clicked = true },
                )
            }
        }
        emitOneConversation(vm)
        rule.onNodeWithText("PM-TITLE").performClick()
        assertTrue("onConversationClick should fire on item click", clicked)
    }

    // --- 2d-iii: message-side item props + searchScopes ---

    private fun message(type: String): TextMessage =
        mock<TextMessage>().also { whenever(it.type).thenReturn(type) }

    private fun media(type: String): MediaMessage =
        mock<MediaMessage>().also { whenever(it.type).thenReturn(type) }

    private fun emitOneMessage(vm: CometChatSearchViewModel, msg: BaseMessage) {
        emit(vm, "_messages", listOf(msg))
        emit(vm, "_uiState", SearchUIState.Content)
    }

    @Test
    fun searchScopes_isAccepted_andSearchRenders() {
        val vm = newViewModel()
        rule.setContent {
            CometChatTheme { CometChatSearch(viewModel = vm, searchScopes = listOf(SearchScope.MESSAGES)) }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Start Your Search").assertIsDisplayed()
    }

    @Test
    fun textMessageItemView_customRenders_andOnMessageClickFires() {
        val vm = newViewModel()
        var clicked = false
        rule.setContent {
            CometChatTheme {
                CometChatSearch(
                    viewModel = vm,
                    textMessageItemView = { Text("PM-TXT") },
                    onMessageClick = { clicked = true },
                )
            }
        }
        emitOneMessage(vm, message(CometChatConstants.MESSAGE_TYPE_TEXT))
        rule.onNodeWithText("PM-TXT").assertIsDisplayed()
        rule.onNodeWithText("PM-TXT").performClick()
        assertTrue("onMessageClick should fire on message-item click", clicked)
    }

    @Test
    fun imageMessageItemView_customRenders() {
        val vm = newViewModel()
        rule.setContent {
            CometChatTheme { CometChatSearch(viewModel = vm, imageMessageItemView = { Text("PM-IMG") }) }
        }
        emitOneMessage(vm, media(CometChatConstants.MESSAGE_TYPE_IMAGE))
        rule.onNodeWithText("PM-IMG").assertIsDisplayed()
    }

    @Test
    fun videoMessageItemView_customRenders() {
        val vm = newViewModel()
        rule.setContent {
            CometChatTheme { CometChatSearch(viewModel = vm, videoMessageItemView = { Text("PM-VID") }) }
        }
        emitOneMessage(vm, media(CometChatConstants.MESSAGE_TYPE_VIDEO))
        rule.onNodeWithText("PM-VID").assertIsDisplayed()
    }

    @Test
    fun audioMessageItemView_customRenders() {
        val vm = newViewModel()
        rule.setContent {
            CometChatTheme { CometChatSearch(viewModel = vm, audioMessageItemView = { Text("PM-AUD") }) }
        }
        emitOneMessage(vm, media(CometChatConstants.MESSAGE_TYPE_AUDIO))
        rule.onNodeWithText("PM-AUD").assertIsDisplayed()
    }

    @Test
    fun documentMessageItemView_customRenders() {
        val vm = newViewModel()
        rule.setContent {
            CometChatTheme { CometChatSearch(viewModel = vm, documentMessageItemView = { Text("PM-DOC") }) }
        }
        emitOneMessage(vm, media(CometChatConstants.MESSAGE_TYPE_FILE))
        rule.onNodeWithText("PM-DOC").assertIsDisplayed()
    }

    @Test
    fun linkMessageItemView_customRenders_forTextWithUrl() {
        val vm = newViewModel()
        val linkMsg = mock<TextMessage>().also {
            whenever(it.type).thenReturn(CometChatConstants.MESSAGE_TYPE_TEXT)
            whenever(it.text).thenReturn("check this out https://example.com")
        }
        rule.setContent {
            CometChatTheme { CometChatSearch(viewModel = vm, linkMessageItemView = { Text("PM-LINK") }) }
        }
        emitOneMessage(vm, linkMsg)
        rule.onNodeWithText("PM-LINK").assertIsDisplayed()
    }
}
