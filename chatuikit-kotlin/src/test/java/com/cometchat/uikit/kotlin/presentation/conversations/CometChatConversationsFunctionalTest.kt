package com.cometchat.uikit.kotlin.presentation.conversations

import android.os.Looper
import android.view.View
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.state.UIState
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.conversations.ui.CometChatConversations
import com.cometchat.uikit.kotlin.presentation.conversations.utils.ConversationsViewHolderListener
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-38679 — CometChatConversations (View) functional surface — Phase 1b.
 *
 * Layer: unit/Robolectric — Compose-free View tests. State-gated props are driven
 * by emitting into the VM's private _uiState StateFlow (reflection) and idling the
 * main looper (same technique as CometChatSearch Phase 1b-ii). Item clicks fire
 * through the adapter's stored wrapper; item-view listeners are asserted received
 * by the adapter.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatConversationsFunctionalTest {

    private fun withConversations(block: (CometChatConversations) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(CometChatConversations(activity))
        }
        scenario.close()
    }

    private fun field(target: Any, name: String): Any? {
        var c: Class<*>? = target.javaClass
        while (c != null) {
            runCatching { c!!.getDeclaredField(name) }.getOrNull()?.let { it.isAccessible = true; return it.get(target) }
            c = c.superclass
        }
        error("field '$name' not found on ${target.javaClass.name}")
    }

    private fun viewModelOf(v: CometChatConversations): Any =
        requireNotNull(field(v, "viewModel")) { "internal ViewModel was not created" }

    private fun emitState(vm: Any, state: UIState) {
        val f = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (f.get(vm) as MutableStateFlow<Any?>).value = state
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun firstEditText(root: View): EditText? {
        if (root is EditText) return root
        if (root is android.view.ViewGroup) for (i in 0 until root.childCount) firstEditText(root.getChildAt(i))?.let { return it }
        return null
    }

    // --- state-gated custom views ---

    @Test
    fun emptyView_attachesWhenDrivenToEmptyState() {
        withConversations { conv ->
            val custom = View(conv.context)
            conv.setEmptyView(custom)
            emitState(viewModelOf(conv), UIState.Empty)
            assertNotNull("custom empty view should be attached", custom.parent)
        }
    }

    @Test
    fun errorView_attachesWhenDrivenToErrorState() {
        withConversations { conv ->
            val custom = View(conv.context)
            conv.setErrorView(custom)
            emitState(viewModelOf(conv), UIState.Error(CometChatException("E", "e")))
            assertNotNull("custom error view should be attached", custom.parent)
        }
    }

    @Test
    fun loadingView_attachesWhenDrivenToLoadingState() {
        withConversations { conv ->
            val custom = View(conv.context)
            conv.setLoadingView(custom)
            emitState(viewModelOf(conv), UIState.Loading)
            assertNotNull("custom loading view should be attached", custom.parent)
        }
    }

    // --- value effect ---

    @Test
    fun searchPlaceholder_reachesTheSearchInput() {
        withConversations { conv ->
            conv.setSearchPlaceholderText("PM-PLACEHOLDER")
            val edit = firstEditText(conv)
            assertNotNull("expected a search EditText", edit)
            assertEquals("PM-PLACEHOLDER", edit!!.hint?.toString())
        }
    }

    // --- item-click callbacks fire through the adapter wrapper ---

    @Test
    fun onItemClick_and_onItemLongClick_fireThroughAdapter() {
        withConversations { conv ->
            var clicked: Conversation? = null
            var longClicked: Conversation? = null
            conv.setOnItemClick { clicked = it }
            conv.setOnItemLongClick { longClicked = it }

            val adapter = field(conv, "conversationsAdapter")!!
            @Suppress("UNCHECKED_CAST")
            val click = field(adapter, "onItemClick") as (View, Int, Conversation) -> Unit
            @Suppress("UNCHECKED_CAST")
            val longClick = field(adapter, "onItemLongClick") as (View, Int, Conversation) -> Unit

            val row = View(conv.context)
            val c1 = mock<Conversation>()
            val c2 = mock<Conversation>()
            click(row, 0, c1)
            longClick(row, 0, c2)

            assertSame(c1, clicked)
            assertSame(c2, longClicked)
        }
    }

    // --- custom item-view listeners reach the adapter ---

    @Test
    fun itemViewListeners_reachTheAdapter() {
        withConversations { conv ->
            val item = mock<ConversationsViewHolderListener>()
            val leading = mock<ConversationsViewHolderListener>()
            val title = mock<ConversationsViewHolderListener>()
            val subtitle = mock<ConversationsViewHolderListener>()
            val trailing = mock<ConversationsViewHolderListener>()
            conv.setItemView(item)
            conv.setLeadingView(leading)
            conv.setTitleView(title)
            conv.setSubtitleView(subtitle)
            conv.setTrailingView(trailing)

            val a = field(conv, "conversationsAdapter")!!
            assertSame(item, field(a, "itemViewListener"))
            assertSame(leading, field(a, "leadingViewListener"))
            assertSame(title, field(a, "titleViewListener"))
            assertSame(subtitle, field(a, "subtitleViewListener"))
            assertSame(trailing, field(a, "trailingViewListener"))
        }
    }
}
