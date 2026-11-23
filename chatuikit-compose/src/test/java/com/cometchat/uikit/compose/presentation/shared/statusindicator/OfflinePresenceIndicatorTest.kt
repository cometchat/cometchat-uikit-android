package com.cometchat.uikit.compose.presentation.shared.statusindicator

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.conversations.ui.getStatusIndicatorFromConversation
import com.cometchat.uikit.compose.presentation.groupmembers.ui.memberStatusIndicator
import com.cometchat.uikit.compose.presentation.search.ui.searchConversationStatusIndicator
import com.cometchat.uikit.compose.presentation.users.ui.getStatusIndicatorFromUser
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ENG-41465 (Compose kit): every list that paints a status dot hides "online" while offline is on
 * and the device is not connected (`hideOnline == true`), and is exactly today's rule otherwise.
 */
class OfflinePresenceIndicatorTest {

    private val online = CometChatConstants.USER_STATUS_ONLINE
    private val offline = CometChatConstants.USER_STATUS_OFFLINE

    private fun user(status: String, blocked: Boolean = false) = User("ann", "Ann").apply {
        this.status = status
        isBlockedByMe = blocked
    }

    private fun userConversation(status: String) =
        Conversation("ann_user", CometChatConstants.CONVERSATION_TYPE_USER).apply { conversationWith = user(status) }

    @Test
    fun `users list - disconnected shows a cached online user as offline`() {
        assertEquals(StatusIndicator.OFFLINE, getStatusIndicatorFromUser(user(online), hideOnline = true))
    }

    @Test
    fun `users list - connected or offline off is today's rule`() {
        assertEquals(StatusIndicator.ONLINE, getStatusIndicatorFromUser(user(online), hideOnline = false))
        assertEquals(StatusIndicator.OFFLINE, getStatusIndicatorFromUser(user(offline), hideOnline = false))
        assertEquals(StatusIndicator.OFFLINE, getStatusIndicatorFromUser(user(online, blocked = true), hideOnline = false))
    }

    @Test
    fun `group members - disconnected shows a cached online member as offline`() {
        assertEquals(StatusIndicator.OFFLINE, memberStatusIndicator(online, hideOnline = true))
    }

    @Test
    fun `group members - connected or offline off is today's rule`() {
        assertEquals(StatusIndicator.ONLINE, memberStatusIndicator(online, hideOnline = false))
        assertEquals(StatusIndicator.OFFLINE, memberStatusIndicator(offline, hideOnline = false))
    }

    @Test
    fun `conversations - disconnected shows a cached online user as offline`() {
        assertEquals(
            StatusIndicator.OFFLINE,
            getStatusIndicatorFromConversation(userConversation(online), hideUserStatus = false, hideGroupType = false, hideOnline = true)
        )
    }

    @Test
    fun `conversations - connected or offline off is today's rule`() {
        assertEquals(
            StatusIndicator.ONLINE,
            getStatusIndicatorFromConversation(userConversation(online), hideUserStatus = false, hideGroupType = false, hideOnline = false)
        )
        assertEquals(
            StatusIndicator.OFFLINE,
            getStatusIndicatorFromConversation(userConversation(online), hideUserStatus = true, hideGroupType = false, hideOnline = false)
        )
    }

    @Test
    fun `search results - disconnected shows a cached online user as offline`() {
        assertEquals(StatusIndicator.OFFLINE, searchConversationStatusIndicator(userConversation(online), hideOnline = true))
    }

    @Test
    fun `search results - connected or offline off is today's rule`() {
        assertEquals(StatusIndicator.ONLINE, searchConversationStatusIndicator(userConversation(online), hideOnline = false))
        assertEquals(StatusIndicator.OFFLINE, searchConversationStatusIndicator(userConversation(offline), hideOnline = false))
    }
}
