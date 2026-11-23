package com.cometchat.uikit.kotlin.shared.mentions

import android.os.Looper
import android.view.ContextThemeWrapper
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.GroupMembersRequest
import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.User
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.shared.formatters.CometChatMentionsFormatter
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-41465 (Views kit): with offline on and the device not connected, @mention suggestions carry
 * no cached "online" status; connected, they carry `user.status` as today.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MentionsOfflinePresenceTest {

    private lateinit var sdk: MockedStatic<CometChat>

    @Before
    fun setUp() {
        sdk = Mockito.mockStatic(CometChat::class.java)
        sdk.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(true)
    }

    @After
    fun tearDown() = sdk.close()

    private fun connected(isConnected: Boolean) {
        val state = if (isConnected) CometChatConstants.WS_STATE_CONNECTED else "disconnected"
        sdk.`when`<String> { CometChat.getConnectionStatus() }.thenReturn(state)
    }

    private fun themed() = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.CometChatTheme_DayNight)

    private fun onlineUser(uid: String) = User(uid, uid.uppercase()).apply { status = CometChatConstants.USER_STATUS_ONLINE }

    private fun onlineMember(uid: String) = GroupMember(uid, "participant").apply {
        name = uid.uppercase()
        status = CometChatConstants.USER_STATUS_ONLINE
    }

    private fun CometChatMentionsFormatter.statuses(): Map<String, String?> {
        shadowOf(Looper.getMainLooper()).idle()
        return getSuggestionItemList().value.orEmpty().associate { it.id to it.status }
    }

    private fun usersFormatter(): Pair<CometChatMentionsFormatter, () -> CometChat.UserFetchListener> {
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
        val f = CometChatMentionsFormatter(themed()).apply { setUsersRequestBuilder(builder) }
        return f to { listener!! }
    }

    private fun membersFormatter(): Pair<CometChatMentionsFormatter, () -> CometChat.GroupMemberFetchListener> {
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
        val f = CometChatMentionsFormatter(themed())
        f.setGroupMembersRequestBuilder { builder }
        f.setGroup(Group().apply { guid = "g1"; name = "G" })
        return f to { listener!! }
    }

    @Test
    fun `users - disconnected, a cached online user is suggested as offline`() {
        connected(false)
        val (f, listener) = usersFormatter()
        f.searchMentions("")
        listener().onCachedUsers(listOf(onlineUser("ann")))
        assertEquals(mapOf("ann" to CometChatConstants.USER_STATUS_OFFLINE), f.statuses())
    }

    @Test
    fun `users - connected, the status is today's`() {
        connected(true)
        val (f, listener) = usersFormatter()
        f.searchMentions("")
        listener().onCachedUsers(listOf(onlineUser("ann")))
        assertEquals(mapOf("ann" to CometChatConstants.USER_STATUS_ONLINE), f.statuses())
    }

    @Test
    fun `users - offline off, the status is today's even while disconnected`() {
        sdk.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(false)
        connected(false)
        val (f, listener) = usersFormatter()
        f.searchMentions("")
        listener().onCachedUsers(listOf(onlineUser("ann")))
        assertEquals(mapOf("ann" to CometChatConstants.USER_STATUS_ONLINE), f.statuses())
    }

    @Test
    fun `members - disconnected, a cached online member is suggested as offline`() {
        connected(false)
        val (f, listener) = membersFormatter()
        f.searchMentions("")
        listener().onCachedGroupMembers(listOf(onlineMember("ann")))
        assertEquals(CometChatConstants.USER_STATUS_OFFLINE, f.statuses()["ann"])
    }

    @Test
    fun `members - connected, the status is today's`() {
        connected(true)
        val (f, listener) = membersFormatter()
        f.searchMentions("")
        listener().onCachedGroupMembers(listOf(onlineMember("ann")))
        assertEquals(CometChatConstants.USER_STATUS_ONLINE, f.statuses()["ann"])
    }
}
