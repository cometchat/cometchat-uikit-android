package com.cometchat.uikit.kotlin.presentation.shared

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.uikit.core.state.GroupMembersUIState
import com.cometchat.uikit.core.state.GroupsUIState
import com.cometchat.uikit.core.state.SearchUIState
import com.cometchat.uikit.core.state.UIState
import com.cometchat.uikit.core.state.UsersUIState
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.conversations.ui.CometChatConversations
import com.cometchat.uikit.kotlin.presentation.groupmembers.ui.CometChatGroupMembers
import com.cometchat.uikit.kotlin.presentation.groups.ui.CometChatGroups
import com.cometchat.uikit.kotlin.presentation.search.ui.CometChatSearch
import com.cometchat.uikit.kotlin.presentation.users.ui.CometChatUsers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * D83/D84 (Views): Users, Groups, Group Members, Conversations and Search. A failure with nothing on
 * screen shows the error state (the app's error view when set, else the kit's) and fires `onError`;
 * a failure while rows stay on screen fires `onError` and keeps the rows (no error state); an empty
 * answer shows the empty state (the app's empty view when set). The ViewModel's flows are driven
 * directly; the read logic itself is covered in `chatuikit-core`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class OfflineListStatesTest {

    /** One list component and the states it renders. */
    private class Spec<V : ViewGroup>(
        val create: (ComponentActivity) -> V,
        val setOnError: (V, (CometChatException) -> Unit) -> Unit,
        val setErrorView: (V, View) -> Unit,
        val setEmptyView: (V, View) -> Unit,
        val loading: Any,
        val content: Any,
        val empty: Any,
        val error: (CometChatException) -> Any
    )

    private val users = Spec(
        ::CometChatUsers, { v, cb -> v.setOnError(cb) }, { v, e -> v.setErrorView(e) }, { v, e -> v.setEmptyView(e) },
        UsersUIState.Loading, UsersUIState.Content, UsersUIState.Empty, { UsersUIState.Error(it) }
    )
    private val groups = Spec(
        ::CometChatGroups, { v, cb -> v.setOnError(cb) }, { v, e -> v.setErrorView(e) }, { v, e -> v.setEmptyView(e) },
        GroupsUIState.Loading, GroupsUIState.Content(emptyList()), GroupsUIState.Empty, { GroupsUIState.Error(it) }
    )
    private val members = Spec(
        ::CometChatGroupMembers, { v, cb -> v.setOnError(cb) }, { v, e -> v.setErrorView(e) }, { v, e -> v.setEmptyView(e) },
        GroupMembersUIState.Loading, GroupMembersUIState.Content(emptyList()), GroupMembersUIState.Empty,
        { GroupMembersUIState.Error(it) }
    )
    private val conversations = Spec(
        ::CometChatConversations, { v, cb -> v.setOnError(cb) }, { v, e -> v.setErrorView(e) }, { v, e -> v.setEmptyView(e) },
        UIState.Loading, UIState.Content(emptyList()), UIState.Empty, { UIState.Error(it) }
    )
    private val search = Spec(
        ::CometChatSearch, { v, cb -> v.setOnError(cb) }, { v, e -> v.setErrorView(e) }, { v, e -> v.setEmptyView(e) },
        SearchUIState.Loading, SearchUIState.Content, SearchUIState.Empty, { SearchUIState.Error(it) }
    )

    private val offline = CometChatException("ERROR_INTERNET_UNAVAILABLE", "offline")

    private fun <V : ViewGroup> withList(spec: Spec<V>, block: (V) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val v = spec.create(activity)
            val container = FrameLayout(activity)
            container.addView(v, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            activity.setContentView(container)
            idle()
            container.measure(
                View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.EXACTLY),
            )
            container.layout(0, 0, 1080, 2160)
            idle()
            block(v)
        }
        scenario.close()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun field(target: Any, name: String): Any? {
        var c: Class<*>? = target.javaClass
        while (c != null) {
            runCatching { c!!.getDeclaredField(name) }.getOrNull()?.let { it.isAccessible = true; return it.get(target) }
            c = c.superclass
        }
        error("field '$name' not found on ${target.javaClass.name}")
    }

    private fun vm(component: Any): Any = requireNotNull(field(component, "viewModel"))

    private fun setState(component: Any, value: Any) {
        @Suppress("UNCHECKED_CAST")
        (field(vm(component), "_uiState") as MutableStateFlow<Any?>).value = value
        idle()
    }

    private fun emitRowsKeptError(component: Any, e: CometChatException) {
        @Suppress("UNCHECKED_CAST")
        (field(vm(component), "_rowsKeptError") as Channel<CometChatException>).trySend(e)
        idle()
    }

    private fun kitErrorShown(v: View) = v.findViewById<View>(R.id.error_state_view).isShown

    private fun <V : ViewGroup> errorWithNothingShown(spec: Spec<V>) = withList(spec) { v ->
        val errors = mutableListOf<CometChatException>()
        spec.setOnError(v) { errors += it }
        setState(v, spec.loading)
        setState(v, spec.error(offline))
        assertTrue("kit error state shown", kitErrorShown(v))
        assertEquals(listOf(offline), errors)
    }

    private fun <V : ViewGroup> appErrorViewWins(spec: Spec<V>) = withList(spec) { v ->
        val app = TextView(v.context).apply { text = "APP-ERROR" }
        spec.setErrorView(v, app)
        setState(v, spec.loading)
        setState(v, spec.error(offline))
        assertTrue("app error view shown", app.isShown)
        assertFalse("no kit error state", kitErrorShown(v))
    }

    private fun <V : ViewGroup> rowsKeptFireOnErrorOnly(spec: Spec<V>) = withList(spec) { v ->
        val app = TextView(v.context).apply { text = "APP-ERROR" }
        spec.setErrorView(v, app)
        val errors = mutableListOf<CometChatException>()
        spec.setOnError(v) { errors += it }
        setState(v, spec.content)
        emitRowsKeptError(v, offline)
        assertEquals(listOf(offline), errors)
        assertFalse("no app error view", app.isShown)
        assertFalse("no kit error state", kitErrorShown(v))
    }

    private fun <V : ViewGroup> appEmptyViewWins(spec: Spec<V>) = withList(spec) { v ->
        val app = TextView(v.context).apply { text = "APP-EMPTY" }
        spec.setEmptyView(v, app)
        setState(v, spec.loading)
        setState(v, spec.empty)
        assertTrue("app empty view shown", app.isShown)
        assertFalse("no kit error state", kitErrorShown(v))
    }

    @Test fun users_errorWithNothingShown_isTheKitErrorState() = errorWithNothingShown(users)
    @Test fun users_appErrorViewWins() = appErrorViewWins(users)
    @Test fun users_rowsKept_onErrorFires_noErrorState() = rowsKeptFireOnErrorOnly(users)
    @Test fun users_appEmptyViewWins() = appEmptyViewWins(users)

    @Test fun groups_errorWithNothingShown_isTheKitErrorState() = errorWithNothingShown(groups)
    @Test fun groups_appErrorViewWins() = appErrorViewWins(groups)
    @Test fun groups_rowsKept_onErrorFires_noErrorState() = rowsKeptFireOnErrorOnly(groups)
    @Test fun groups_appEmptyViewWins() = appEmptyViewWins(groups)

    @Test fun members_errorWithNothingShown_isTheKitErrorState() = errorWithNothingShown(members)
    @Test fun members_appErrorViewWins() = appErrorViewWins(members)
    @Test fun members_rowsKept_onErrorFires_noErrorState() = rowsKeptFireOnErrorOnly(members)
    @Test fun members_appEmptyViewWins() = appEmptyViewWins(members)

    @Test fun conversations_errorWithNothingShown_isTheKitErrorState() = errorWithNothingShown(conversations)
    @Test fun conversations_appErrorViewWins() = appErrorViewWins(conversations)
    @Test fun conversations_rowsKept_onErrorFires_noErrorState() = rowsKeptFireOnErrorOnly(conversations)
    @Test fun conversations_appEmptyViewWins() = appEmptyViewWins(conversations)

    @Test fun search_errorWithNothingShown_isTheKitErrorState() = errorWithNothingShown(search)
    @Test fun search_appErrorViewWins() = appErrorViewWins(search)
    @Test fun search_rowsKept_onErrorFires_noErrorState() = rowsKeptFireOnErrorOnly(search)
    @Test fun search_appEmptyViewWins() = appEmptyViewWins(search)

    // ---- D84: the kit's offline states ------------------------------------------------------

    private fun setFlag(component: Any, name: String, value: Boolean) {
        @Suppress("UNCHECKED_CAST")
        (field(vm(component), name) as MutableStateFlow<Boolean>).value = value
    }

    private fun text(v: View, id: Int) = v.findViewById<TextView>(id).text.toString()

    /** Rule 3: offline with nothing shown is the offline state (empty layout + text), onError once. */
    private fun <V : ViewGroup> offlineState(spec: Spec<V>, titleId: Int, loadRes: Int) = withList(spec) { v ->
        // The app's empty view never stands in for the offline state (D81 routing is gone).
        spec.setEmptyView(v, TextView(v.context).apply { text = "APP-EMPTY" })
        val errors = mutableListOf<CometChatException>()
        spec.setOnError(v) { errors += it }
        setState(v, spec.loading)
        setFlag(v, "_connectToLoad", true)
        setState(v, spec.error(offline))
        assertFalse("no kit error state", kitErrorShown(v))
        assertTrue("offline state shown", v.findViewById<View>(titleId).isShown)
        assertEquals(v.context.getString(loadRes), text(v, titleId))
        assertEquals(listOf(offline), errors)
    }

    /** Rule 3: an app error view replaces the kit's offline state. */
    private fun <V : ViewGroup> appErrorViewWinsOverOffline(spec: Spec<V>, titleId: Int) = withList(spec) { v ->
        val app = TextView(v.context).apply { text = "APP-ERROR" }
        spec.setErrorView(v, app)
        setState(v, spec.loading)
        setFlag(v, "_connectToLoad", true)
        setState(v, spec.error(offline))
        assertTrue("app error view shown", app.isShown)
        assertFalse("no kit offline state", v.findViewById<View>(titleId).isShown)
    }

    /**
     * Rule 7: an offline search miss reads "Connect to search" with the subtitle "Search needs an
     * internet connection." (D84, D-355), no onError; a later empty is today's, its own subtitle back.
     */
    private fun <V : ViewGroup> connectToSearch(
        spec: Spec<V>, titleId: Int, subtitleId: Int, flag: String, searchRes: Int,
        subtitleRes: Int = com.cometchat.uikit.core.R.string.cometchat_connect_to_search_subtitle,
        defaultRes: Int? = null
    ) = withList(spec) { v ->
        val errors = mutableListOf<CometChatException>()
        spec.setOnError(v) { errors += it }
        val defaultTitle = text(v, titleId)
        val defaultSubtitle = text(v, subtitleId)
        val defaultSubtitleVisibility = v.findViewById<View>(subtitleId).visibility
        setState(v, spec.loading)
        setFlag(v, flag, true)
        setState(v, spec.empty)
        assertEquals(v.context.getString(searchRes), text(v, titleId))
        assertEquals("Search needs an internet connection.", v.context.getString(subtitleRes))
        assertEquals(v.context.getString(subtitleRes), text(v, subtitleId))
        assertTrue("the subtitle shows under \"Connect to search\"", v.findViewById<View>(subtitleId).isShown)
        assertEquals(emptyList<CometChatException>(), errors)
        setFlag(v, flag, false)
        setState(v, spec.loading)
        setState(v, spec.empty)
        assertEquals(defaultRes?.let { v.context.getString(it) } ?: defaultTitle, text(v, titleId))
        if (defaultRes == null) {
            assertEquals("today's subtitle comes back", defaultSubtitle, text(v, subtitleId))
            assertEquals(defaultSubtitleVisibility, v.findViewById<View>(subtitleId).visibility)
        }
    }

    @Test fun users_d84_offlineState() =
        offlineState(users, R.id.tv_empty_users_title, com.cometchat.uikit.core.R.string.cometchat_connect_to_load_users)
    @Test fun users_d84_appErrorViewWinsOverOffline() = appErrorViewWinsOverOffline(users, R.id.tv_empty_users_title)
    @Test fun users_d84_connectToSearch() = connectToSearch(
        users, R.id.tv_empty_users_title, R.id.tv_empty_users_subtitle, "_connectToSearch", com.cometchat.uikit.core.R.string.cometchat_connect_to_search
    )

    @Test fun groups_d84_offlineState() =
        offlineState(groups, R.id.tv_empty_groups_title, com.cometchat.uikit.core.R.string.cometchat_connect_to_load_groups)
    @Test fun groups_d84_appErrorViewWinsOverOffline() = appErrorViewWinsOverOffline(groups, R.id.tv_empty_groups_title)
    @Test fun groups_d84_connectToSearch() = connectToSearch(
        groups, R.id.tv_empty_groups_title, R.id.tv_empty_groups_subtitle, "_connectToSearch", com.cometchat.uikit.core.R.string.cometchat_connect_to_search
    )

    @Test fun members_d84_offlineState() =
        offlineState(members, R.id.tv_empty_title, com.cometchat.uikit.core.R.string.cometchat_connect_to_load_members)
    @Test fun members_d84_appErrorViewWinsOverOffline() = appErrorViewWinsOverOffline(members, R.id.tv_empty_title)
    @Test fun members_d84_connectToSearch() = connectToSearch(
        members, R.id.tv_empty_title, R.id.tv_empty_subtitle, "_connectToSearch", com.cometchat.uikit.core.R.string.cometchat_connect_to_search
    )

    @Test fun conversations_d84_offlineState() = offlineState(
        conversations, R.id.tv_empty_conversations_title, com.cometchat.uikit.core.R.string.cometchat_connect_to_load_chats
    )
    @Test fun conversations_d84_appErrorViewWinsOverOffline() =
        appErrorViewWinsOverOffline(conversations, R.id.tv_empty_conversations_title)
    @Test fun conversations_d84_connectToSearch() = connectToSearch(
        conversations, R.id.tv_empty_conversations_title, R.id.tv_empty_conversations_subtitle, "_connectToSearch",
        com.cometchat.uikit.core.R.string.cometchat_connect_to_search
    )

    @Test fun search_d84_connectToSearch() = connectToSearch(
        search, R.id.tv_empty_state_title, R.id.tv_empty_state_subtitle, "_showConnectToSearch",
        com.cometchat.uikit.core.R.string.cometchat_connect_to_search_messages,
        com.cometchat.uikit.core.R.string.cometchat_connect_to_search_messages_subtitle,
        defaultRes = R.string.cometchat_no_results
    )
}
