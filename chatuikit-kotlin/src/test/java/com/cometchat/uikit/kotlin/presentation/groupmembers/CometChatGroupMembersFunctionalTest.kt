package com.cometchat.uikit.kotlin.presentation.groupmembers

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.GroupMember
import com.cometchat.uikit.core.state.GroupMembersUIState
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.groupmembers.ui.CometChatGroupMembers
import com.cometchat.uikit.kotlin.presentation.groupmembers.utils.GroupMembersViewHolderListener
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-38679 — CometChatGroupMembers (View) functional surface. State-gated props
 * driven via the VM's _uiState (GroupMembersUIState); item clicks fire through the
 * adapter wrapper; item-view listeners asserted received by the adapter.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatGroupMembersFunctionalTest {

    private fun withGm(block: (CometChatGroupMembers) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val gm = CometChatGroupMembers(activity)
            val container = FrameLayout(activity)
            container.addView(gm, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            activity.setContentView(container)
            shadowOf(Looper.getMainLooper()).idle()
            container.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.EXACTLY),
            )
            container.layout(0, 0, 1080, 2160)
            shadowOf(Looper.getMainLooper()).idle()
            block(gm)
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

    private fun emitState(v: CometChatGroupMembers, state: GroupMembersUIState) {
        val vm = requireNotNull(field(v, "viewModel"))
        val f = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (f.get(vm) as MutableStateFlow<Any?>).value = state
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun emptyView_attachesWhenDrivenToEmptyState() {
        withGm { g -> val c = View(g.context); g.setEmptyView(c); emitState(g, GroupMembersUIState.Empty); assertNotNull("empty view attached", c.parent) }
    }

    @Test
    fun errorView_attachesWhenDrivenToErrorState() {
        withGm { g -> val c = View(g.context); g.setErrorView(c); emitState(g, GroupMembersUIState.Error(CometChatException("E", "e"))); assertNotNull("error view attached", c.parent) }
    }

    @Test
    fun loadingView_attachesWhenDrivenToLoadingState() {
        withGm { g ->
            val c = View(g.context); g.setLoadingView(c)
            emitState(g, GroupMembersUIState.Empty); emitState(g, GroupMembersUIState.Loading)
            assertNotNull("loading view attached", c.parent)
        }
    }

    @Test
    fun onItemClick_and_onItemLongClick_fireThroughAdapter() {
        withGm { g ->
            var clicked: GroupMember? = null
            var longClicked: GroupMember? = null
            g.setOnItemClick { clicked = it }
            g.setOnItemLongClick { longClicked = it }
            val adapter = field(g, "membersAdapter")!!
            @Suppress("UNCHECKED_CAST")
            val click = field(adapter, "onItemClick") as (View, Int, GroupMember) -> Unit
            @Suppress("UNCHECKED_CAST")
            val longClick = field(adapter, "onItemLongClick") as (View, Int, GroupMember) -> Unit
            val row = View(g.context); val a = mock<GroupMember>(); val b = mock<GroupMember>()
            click(row, 0, a); longClick(row, 0, b)
            assertSame(a, clicked); assertSame(b, longClicked)
        }
    }

    @Test
    fun itemViewListeners_reachTheAdapter() {
        withGm { g ->
            val item = mock<GroupMembersViewHolderListener>(); val leading = mock<GroupMembersViewHolderListener>()
            val title = mock<GroupMembersViewHolderListener>(); val subtitle = mock<GroupMembersViewHolderListener>(); val trailing = mock<GroupMembersViewHolderListener>()
            g.setItemView(item); g.setLeadingView(leading); g.setTitleView(title); g.setSubtitleView(subtitle); g.setTrailingView(trailing)
            val a = field(g, "membersAdapter")!!
            assertSame(item, field(a, "itemViewListener"))
            assertSame(leading, field(a, "leadingViewListener"))
            assertSame(title, field(a, "titleViewListener"))
            assertSame(subtitle, field(a, "subtitleViewListener"))
            assertSame(trailing, field(a, "trailingViewListener"))
        }
    }
}
