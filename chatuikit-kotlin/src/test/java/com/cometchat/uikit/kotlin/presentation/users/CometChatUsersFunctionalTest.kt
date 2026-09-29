package com.cometchat.uikit.kotlin.presentation.users

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.state.UsersUIState
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.users.ui.CometChatUsers
import com.cometchat.uikit.kotlin.presentation.users.utils.UsersViewHolderListener
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-38679 — CometChatUsers (View) functional surface. State-gated props driven
 * via the VM's _uiState (UsersUIState); item clicks fire through the adapter
 * wrapper; item-view listeners asserted received by the adapter.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatUsersFunctionalTest {

    private fun withUsers(block: (CometChatUsers) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val users = CometChatUsers(activity)
            // Attach + lay out so the lifecycle-scoped state observers are live.
            val container = FrameLayout(activity)
            container.addView(users, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            activity.setContentView(container)
            shadowOf(Looper.getMainLooper()).idle()
            container.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.EXACTLY),
            )
            container.layout(0, 0, 1080, 2160)
            shadowOf(Looper.getMainLooper()).idle()
            block(users)
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

    private fun emitState(v: CometChatUsers, state: UsersUIState) {
        val vm = requireNotNull(field(v, "viewModel"))
        val f = vm.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (f.get(vm) as MutableStateFlow<Any?>).value = state
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun emptyView_attachesWhenDrivenToEmptyState() {
        withUsers { u ->
            val custom = View(u.context); u.setEmptyView(custom)
            emitState(u, UsersUIState.Empty)
            assertNotNull("custom empty view should be attached", custom.parent)
        }
    }

    @Test
    fun errorView_attachesWhenDrivenToErrorState() {
        withUsers { u ->
            val custom = View(u.context); u.setErrorView(custom)
            emitState(u, UsersUIState.Error(CometChatException("E", "e")))
            assertNotNull("custom error view should be attached", custom.parent)
        }
    }

    @Test
    fun loadingView_attachesWhenDrivenToLoadingState() {
        withUsers { u ->
            val custom = View(u.context); u.setLoadingView(custom)
            // Initial VM state is already Loading; move off it, then back, so the
            // state re-renders with the custom loading view now set.
            emitState(u, UsersUIState.Empty)
            emitState(u, UsersUIState.Loading)
            assertNotNull("custom loading view should be attached", custom.parent)
        }
    }

    @Test
    fun onItemClick_and_onItemLongClick_fireThroughAdapter() {
        withUsers { u ->
            var clicked: User? = null
            var longClicked: User? = null
            u.setOnItemClick { clicked = it }
            u.setOnItemLongClick { longClicked = it }

            val adapter = field(u, "usersAdapter")!!
            @Suppress("UNCHECKED_CAST")
            val click = field(adapter, "onItemClick") as (View, Int, User) -> Unit
            @Suppress("UNCHECKED_CAST")
            val longClick = field(adapter, "onItemLongClick") as (View, Int, User) -> Unit

            val row = View(u.context)
            val a = mock<User>(); val b = mock<User>()
            click(row, 0, a); longClick(row, 0, b)
            assertSame(a, clicked); assertSame(b, longClicked)
        }
    }

    @Test
    fun itemViewListeners_reachTheAdapter() {
        withUsers { u ->
            val item = mock<UsersViewHolderListener>()
            val leading = mock<UsersViewHolderListener>()
            val title = mock<UsersViewHolderListener>()
            val subtitle = mock<UsersViewHolderListener>()
            val trailing = mock<UsersViewHolderListener>()
            u.setItemView(item); u.setLeadingView(leading); u.setTitleView(title)
            u.setSubtitleView(subtitle); u.setTrailingView(trailing)

            val a = field(u, "usersAdapter")!!
            assertSame(item, field(a, "itemViewListener"))
            assertSame(leading, field(a, "leadingViewListener"))
            assertSame(title, field(a, "titleViewListener"))
            assertSame(subtitle, field(a, "subtitleViewListener"))
            assertSame(trailing, field(a, "trailingViewListener"))
        }
    }
}
