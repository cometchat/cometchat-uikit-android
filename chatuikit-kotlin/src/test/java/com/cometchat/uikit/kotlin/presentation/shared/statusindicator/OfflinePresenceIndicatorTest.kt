package com.cometchat.uikit.kotlin.presentation.shared.statusindicator

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.kotlin.presentation.conversations.ui.conversationUserStatusIndicator
import com.cometchat.uikit.kotlin.presentation.groupmembers.adapter.memberStatusShowsOnline
import com.cometchat.uikit.kotlin.presentation.search.ui.viewholders.searchUserShowsOnline
import com.cometchat.uikit.kotlin.presentation.users.ui.usersListStatusIndicator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ENG-41465 (Views kit): every list that paints a status dot hides "online" while offline is on
 * and the device is not connected (`hideOnline == true`), and is exactly today's rule otherwise.
 */
class OfflinePresenceIndicatorTest {

    private val online = CometChatConstants.USER_STATUS_ONLINE
    private val offline = CometChatConstants.USER_STATUS_OFFLINE

    @Test
    fun `users list - disconnected shows a cached online user as offline`() {
        assertEquals(StatusIndicator.OFFLINE, usersListStatusIndicator(blocked = false, status = online, hideOnline = true))
    }

    @Test
    fun `users list - connected or offline off is today's rule`() {
        assertEquals(StatusIndicator.ONLINE, usersListStatusIndicator(blocked = false, status = online, hideOnline = false))
        assertEquals(StatusIndicator.OFFLINE, usersListStatusIndicator(blocked = false, status = offline, hideOnline = false))
        assertEquals(StatusIndicator.OFFLINE, usersListStatusIndicator(blocked = true, status = online, hideOnline = false))
    }

    @Test
    fun `group members - disconnected hides the online dot`() {
        assertFalse(memberStatusShowsOnline(online, blocked = false, hideUserStatus = false, hideOnline = true))
    }

    @Test
    fun `group members - connected or offline off is today's rule`() {
        assertTrue(memberStatusShowsOnline(online, blocked = false, hideUserStatus = false, hideOnline = false))
        // Today's comparison is case-insensitive; that stays.
        assertTrue(memberStatusShowsOnline("ONLINE", blocked = false, hideUserStatus = false, hideOnline = false))
        assertFalse(memberStatusShowsOnline(online, blocked = true, hideUserStatus = false, hideOnline = false))
        assertFalse(memberStatusShowsOnline(online, blocked = false, hideUserStatus = true, hideOnline = false))
        assertFalse(memberStatusShowsOnline(offline, blocked = false, hideUserStatus = false, hideOnline = false))
    }

    @Test
    fun `conversations - disconnected shows a cached online user as offline`() {
        assertEquals(StatusIndicator.OFFLINE, conversationUserStatusIndicator(online, hideOnline = true))
    }

    @Test
    fun `conversations - connected or offline off is today's rule`() {
        assertEquals(StatusIndicator.ONLINE, conversationUserStatusIndicator(online, hideOnline = false))
        assertEquals(StatusIndicator.OFFLINE, conversationUserStatusIndicator(offline, hideOnline = false))
    }

    @Test
    fun `search results - disconnected hides the online dot`() {
        assertFalse(searchUserShowsOnline(online, hideUserStatus = false, hideOnline = true))
    }

    @Test
    fun `search results - connected or offline off is today's rule`() {
        assertTrue(searchUserShowsOnline(online, hideUserStatus = false, hideOnline = false))
        assertFalse(searchUserShowsOnline(online, hideUserStatus = true, hideOnline = false))
        assertFalse(searchUserShowsOnline(offline, hideUserStatus = false, hideOnline = false))
    }
}
