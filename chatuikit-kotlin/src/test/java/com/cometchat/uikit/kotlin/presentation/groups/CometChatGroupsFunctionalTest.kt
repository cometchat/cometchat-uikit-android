package com.cometchat.uikit.kotlin.presentation.groups

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.uikit.core.state.GroupsUIState
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.groups.ui.CometChatGroups
import com.cometchat.uikit.kotlin.presentation.groups.utils.GroupsViewHolderListener
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-38679 — CometChatGroups (View) functional surface. State-gated props driven
 * via the VM's _uiState (GroupsUIState); item clicks fire through the adapter
 * wrapper; item-view listeners asserted received by the adapter.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatGroupsFunctionalTest {

    private fun withGroups(block: (CometChatGroups) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val groups = CometChatGroups(activity)
            val container = FrameLayout(activity)
            container.addView(groups, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            activity.setContentView(container)
            shadowOf(Looper.getMainLooper()).idle()
            container.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.EXACTLY),
            )
            container.layout(0, 0, 1080, 2160)
            shadowOf(Looper.getMainLooper()).idle()
            block(groups)
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

    private fun emitState(v: CometChatGroups, state: GroupsUIState) {
        val vm = requireNotNull(field(v, "viewModel"))
        val f = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (f.get(vm) as MutableStateFlow<Any?>).value = state
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun emptyView_attachesWhenDrivenToEmptyState() {
        withGroups { g -> val c = View(g.context); g.setEmptyView(c); emitState(g, GroupsUIState.Empty); assertNotNull("empty view attached", c.parent) }
    }

    @Test
    fun errorView_attachesWhenDrivenToErrorState() {
        withGroups { g -> val c = View(g.context); g.setErrorView(c); emitState(g, GroupsUIState.Error(CometChatException("E", "e"))); assertNotNull("error view attached", c.parent) }
    }

    @Test
    fun loadingView_attachesWhenDrivenToLoadingState() {
        withGroups { g ->
            val c = View(g.context); g.setLoadingView(c)
            emitState(g, GroupsUIState.Empty); emitState(g, GroupsUIState.Loading)
            assertNotNull("loading view attached", c.parent)
        }
    }

    @Test
    fun onItemClick_and_onItemLongClick_fireThroughAdapter() {
        withGroups { g ->
            var clicked: Group? = null
            var longClicked: Group? = null
            g.setOnItemClick { clicked = it }
            g.setOnItemLongClick { longClicked = it }
            val adapter = field(g, "groupsAdapter")!!
            @Suppress("UNCHECKED_CAST")
            val click = field(adapter, "onItemClick") as (View, Int, Group) -> Unit
            @Suppress("UNCHECKED_CAST")
            val longClick = field(adapter, "onItemLongClick") as (View, Int, Group) -> Unit
            val row = View(g.context); val a = mock<Group>(); val b = mock<Group>()
            click(row, 0, a); longClick(row, 0, b)
            assertSame(a, clicked); assertSame(b, longClicked)
        }
    }

    @Test
    fun itemViewListeners_reachTheAdapter() {
        withGroups { g ->
            val item = mock<GroupsViewHolderListener>(); val leading = mock<GroupsViewHolderListener>()
            val title = mock<GroupsViewHolderListener>(); val subtitle = mock<GroupsViewHolderListener>(); val trailing = mock<GroupsViewHolderListener>()
            g.setItemView(item); g.setLeadingView(leading); g.setTitleView(title); g.setSubtitleView(subtitle); g.setTrailingView(trailing)
            val a = field(g, "groupsAdapter")!!
            assertSame(item, field(a, "itemViewListener"))
            assertSame(leading, field(a, "leadingViewListener"))
            assertSame(title, field(a, "titleViewListener"))
            assertSame(subtitle, field(a, "subtitleViewListener"))
            assertSame(trailing, field(a, "trailingViewListener"))
        }
    }
}
