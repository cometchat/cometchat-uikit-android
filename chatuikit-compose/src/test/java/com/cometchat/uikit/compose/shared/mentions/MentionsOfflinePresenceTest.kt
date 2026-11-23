package com.cometchat.uikit.compose.shared.mentions

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.GroupMembersRequest
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.shared.formatters.CometChatMentionsFormatter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * ENG-41465 (Compose kit): with offline on and the device not connected, @mention suggestions carry
 * no cached "online" status; connected, or offline off, they carry `user.status` as today.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MentionsOfflinePresenceTest {

    private lateinit var sdk: MockedStatic<CometChat>

    @Before
    fun setUp() {
        sdk = Mockito.mockStatic(CometChat::class.java)
    }

    @After
    fun tearDown() = sdk.close()

    private fun mode(offlineOn: Boolean, isConnected: Boolean) {
        sdk.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(offlineOn)
        val state = if (isConnected) CometChatConstants.WS_STATE_CONNECTED else "disconnected"
        sdk.`when`<String> { CometChat.getConnectionStatus() }.thenReturn(state)
    }

    private fun onlineUser(uid: String) = User(uid, uid.uppercase()).apply { status = CometChatConstants.USER_STATUS_ONLINE }

    private fun onlineMember(uid: String) = GroupMember(uid, "participant").apply {
        name = uid.uppercase()
        status = CometChatConstants.USER_STATUS_ONLINE
    }

    private fun CometChatMentionsFormatter.status(id: String): String? =
        getSuggestionItemList().first { it.id == id }.status

    private fun cachedUsersStatus(user: User): String? {
        val request = mock<UsersRequest>()
        whenever(request.searchKeyword).thenReturn("")
        var listener: CometChat.UserFetchListener? = null
        whenever(request.fetchNext(any<CometChat.UserFetchListener>())).thenAnswer { inv ->
            listener = inv.getArgument(0)
            null
        }
        val builder = mock<UsersRequest.UsersRequestBuilder>()
        whenever(builder.setSearchKeyword(any())).thenReturn(builder)
        whenever(builder.build()).thenReturn(request)
        val f = CometChatMentionsFormatter(RuntimeEnvironment.getApplication()).apply { setUsersRequestBuilder(builder) }
        f.searchMentions("")
        listener!!.onCachedUsers(listOf(user))
        return f.status(user.uid)
    }

    private fun cachedMembersStatus(member: GroupMember): String? {
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
        listener!!.onCachedGroupMembers(listOf(member))
        return f.status(member.uid)
    }

    @Test
    fun `users - offline on and disconnected, a cached online user is suggested as offline`() {
        mode(offlineOn = true, isConnected = false)
        assertEquals(CometChatConstants.USER_STATUS_OFFLINE, cachedUsersStatus(onlineUser("ann")))
    }

    @Test
    fun `users - connected, the status is today's`() {
        mode(offlineOn = true, isConnected = true)
        assertEquals(CometChatConstants.USER_STATUS_ONLINE, cachedUsersStatus(onlineUser("ann")))
    }

    @Test
    fun `users - offline off, the status is today's even while disconnected`() {
        mode(offlineOn = false, isConnected = false)
        assertEquals(CometChatConstants.USER_STATUS_ONLINE, cachedUsersStatus(onlineUser("ann")))
    }

    @Test
    fun `members - offline on and disconnected, a cached online member is suggested as offline`() {
        mode(offlineOn = true, isConnected = false)
        assertEquals(CometChatConstants.USER_STATUS_OFFLINE, cachedMembersStatus(onlineMember("ann")))
    }

    @Test
    fun `members - connected, the status is today's`() {
        mode(offlineOn = true, isConnected = true)
        assertEquals(CometChatConstants.USER_STATUS_ONLINE, cachedMembersStatus(onlineMember("ann")))
    }
}
