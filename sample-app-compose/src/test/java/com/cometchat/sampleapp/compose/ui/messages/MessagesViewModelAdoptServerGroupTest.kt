package com.cometchat.sampleapp.compose.ui.messages

import com.cometchat.chat.models.Group
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ENG-41641: a group chat opened on a held copy (a search hit) follows the server's copy that
 * arrives after it, so membership is not stuck on the held value.
 */
class MessagesViewModelAdoptServerGroupTest {

    private fun group(guid: String, joined: Boolean) =
        Group(guid, "Group $guid", "public", "").apply { setHasJoined(joined) }

    @Test
    fun heldNonMemberCopyThenServerMemberCopy_endsAsMember() {
        val viewModel = MessagesViewModel()
        val held = group("g1", joined = false)
        val fresh = group("g1", joined = true)

        viewModel.initialize(null, held)
        assertFalse(viewModel.isGroupMember.value)

        viewModel.adoptServerGroup(fresh)

        assertTrue(viewModel.isGroupMember.value)
        assertSame(fresh, viewModel.group.value)
    }

    @Test
    fun serverCopyOfAnotherGroup_isIgnored() {
        val viewModel = MessagesViewModel()
        val held = group("g1", joined = false)

        viewModel.initialize(null, held)
        viewModel.adoptServerGroup(group("g2", joined = true))

        assertFalse(viewModel.isGroupMember.value)
        assertSame(held, viewModel.group.value)
    }

    @Test
    fun serverCopyBeforeInitialize_isIgnored() {
        val viewModel = MessagesViewModel()

        viewModel.adoptServerGroup(group("g1", joined = false))

        assertEquals(null, viewModel.group.value)
        assertTrue(viewModel.isGroupMember.value)
    }
}
