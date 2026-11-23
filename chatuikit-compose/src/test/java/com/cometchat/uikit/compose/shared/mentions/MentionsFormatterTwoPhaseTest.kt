package com.cometchat.uikit.compose.shared.mentions

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.GroupMembersRequest
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.shared.formatters.CometChatMentionsFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * ENG-41133 (W7): the Compose kit's mentions suggestions read through the SDK 5.1.0 two-phase
 * listeners (README §3.9 "Mentions suggestions"; BEH-SNAP-04): no keyword → cached then server by
 * id; keyword → server only, offline empty with no toast; a stale keyword's reply is dropped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MentionsFormatterTwoPhaseTest {

    private val notConnected = CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "offline")

    private fun user(uid: String) = User(uid, uid.uppercase())

    private fun member(uid: String) = GroupMember(uid, "participant").apply { name = uid.uppercase() }

    /** A users builder whose successive `build()` calls return one request per keyword. */
    private class Users(vararg keywords: String) {
        val requests = keywords.map { kw ->
            mock<UsersRequest>().also { whenever(it.searchKeyword).thenReturn(kw) }
        }
        val listeners = HashMap<UsersRequest, CometChat.UserFetchListener>()
        val builder = mock<UsersRequest.UsersRequestBuilder>()

        init {
            requests.forEach { r ->
                whenever(r.fetchNext(any<CometChat.UserFetchListener>())).thenAnswer { inv ->
                    listeners[r] = inv.getArgument(0)
                    null
                }
            }
            whenever(builder.setSearchKeyword(any())).thenReturn(builder)
            whenever(builder.build()).thenReturn(requests.first(), *requests.drop(1).toTypedArray())
        }

        fun listener(i: Int) = listeners.getValue(requests[i])
    }

    private fun formatter(users: Users) = CometChatMentionsFormatter(RuntimeEnvironment.getApplication()).apply {
        setUsersRequestBuilder(users.builder)
    }

    private fun CometChatMentionsFormatter.shownIds(): List<String> = getSuggestionItemList().map { it.id }

    @Test
    fun `no keyword - cached first, then server by id with no duplicates`() {
        val users = Users("")
        val f = formatter(users)

        f.searchMentions("")
        users.listener(0).onCachedUsers(listOf(user("ann"), user("bob")))
        assertEquals(listOf("ann", "bob"), f.shownIds())

        users.listener(0).onServerUsers(listOf(user("ann"), user("cat")))
        assertEquals(listOf("ann", "cat"), f.shownIds())
        users.requests.forEach { verify(it, never()).fetchNext(any<CometChat.CallbackListener<List<User>>>()) }
    }

    @Test
    fun `no keyword offline - the cached rows stay after the not-connected error`() {
        val users = Users("")
        val f = formatter(users)

        f.searchMentions("")
        users.listener(0).onCachedUsers(listOf(user("ann")))
        users.listener(0).onError(notConnected)

        assertEquals(listOf("ann"), f.shownIds())
    }

    @Test
    fun `keyword offline - empty list and no error toast`() {
        val users = Users("an")
        val f = formatter(users)

        f.searchMentions("an")
        users.listener(0).onCachedUsers(listOf(user("ann")))   // a keyword has no cache: ignored
        assertEquals(emptyList<String>(), f.shownIds())
        users.listener(0).onError(notConnected)

        assertEquals(emptyList<String>(), f.shownIds())
        assertNull(ShadowToast.getLatestToast())
    }

    @Test
    fun `a stale keyword reply is dropped`() {
        val users = Users("j", "jo")
        val f = formatter(users)

        f.searchMentions("j")
        f.searchMentions("jo")
        users.listener(0).onServerUsers(listOf(user("jim")))   // late reply for "j"
        users.listener(1).onServerUsers(listOf(user("joe")))

        assertEquals(listOf("joe"), f.shownIds())
    }

    @Test
    fun `offline off - server pages only, appended in order as today`() {
        val users = Users("")
        val f = formatter(users)

        f.searchMentions("")
        users.listener(0).onServerUsers(listOf(user("ann"), user("bob")))
        f.fetchUsers() // next page on the same request
        users.listener(0).onServerUsers(listOf(user("bob"), user("cat")))

        assertEquals(listOf("ann", "bob", "cat"), f.shownIds())
    }

    @Test
    fun `group members - cached then server, @all added once at the top`() {
        val request = mock<GroupMembersRequest>()
        whenever(request.searchKeyword).thenReturn("")
        var listener: CometChat.GroupMemberFetchListener? = null
        whenever(request.fetchNext(any<CometChat.GroupMemberFetchListener>())).thenAnswer { inv ->
            listener = inv.getArgument(0)
            null
        }
        val builder = mock<GroupMembersRequest.GroupMembersRequestBuilder>()
        whenever(builder.setSearchKeyword(any())).thenReturn(builder)
        whenever(builder.build()).thenReturn(request)
        val f = CometChatMentionsFormatter(RuntimeEnvironment.getApplication())
        f.setGroupMembersRequestBuilder { builder }
        f.setGroup(Group().apply { guid = "g1"; name = "G" })

        f.searchMentions("")
        listener!!.onCachedGroupMembers(listOf(member("ann"), member("bob")))
        assertEquals(listOf("all", "ann", "bob"), f.shownIds())

        listener!!.onServerGroupMembers(listOf(member("ann"), member("cat")))
        assertEquals(listOf("all", "ann", "cat"), f.shownIds())
        verify(request, never()).fetchNext(any<CometChat.CallbackListener<List<GroupMember>>>())
    }
}
