package com.cometchat.uikit.compose.presentation.threadheader

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.threadheader.ui.threadHeaderBubbleAlignment
import com.cometchat.uikit.compose.presentation.threadheader.ui.threadHeaderShowsAvatar
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.constants.UIKitConstants.MessageBubbleAlignment
import com.cometchat.uikit.core.constants.UIKitConstants.MessageListAlignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * ENG-39537: the Compose thread header's default bubble applies the message list's avatar rule
 * (`shouldShowAvatar`), matching chatuikit-kotlin's thread header:
 *
 * | Parent            | Avatar |
 * |-------------------|--------|
 * | 1:1, own          | no     |
 * | 1:1, incoming     | no     |
 * | Group, own        | no     |
 * | Group, incoming   | yes    |
 *
 * Run with:
 *   ./gradlew :chatuikit-compose:testDebugUnitTest --tests "*threadheader.CometChatThreadHeaderAvatarTest"
 */
class CometChatThreadHeaderAvatarTest {

    private val me = "logged-in-user"
    private val other = "other-user"

    private fun parent(
        senderUid: String,
        receiverType: String,
        senderRole: String? = null
    ): BaseMessage {
        val sender = mock<User>()
        whenever(sender.uid).thenReturn(senderUid)
        whenever(sender.role).thenReturn(senderRole)
        val message = mock<TextMessage>()
        whenever(message.sender).thenReturn(sender)
        whenever(message.receiverType).thenReturn(receiverType)
        return message
    }

    /** Runs the same two steps DefaultMessageBubbleContent runs. */
    private fun showsAvatar(
        message: BaseMessage,
        alignment: MessageListAlignment = MessageListAlignment.STANDARD,
        hideAvatar: Boolean = false
    ): Boolean {
        val bubbleAlignment = threadHeaderBubbleAlignment(message, me, alignment)
        return threadHeaderShowsAvatar(message, bubbleAlignment, hideAvatar)
    }

    @Test
    fun threadHeader_oneOnOne_outgoingParent_noAvatar() {
        val message = parent(me, CometChatConstants.RECEIVER_TYPE_USER)
        assertEquals(
            MessageBubbleAlignment.RIGHT,
            threadHeaderBubbleAlignment(message, me, MessageListAlignment.STANDARD)
        )
        assertFalse(showsAvatar(message))
    }

    @Test
    fun threadHeader_oneOnOne_incomingParent_noAvatar() {
        val message = parent(other, CometChatConstants.RECEIVER_TYPE_USER)
        assertEquals(
            MessageBubbleAlignment.LEFT,
            threadHeaderBubbleAlignment(message, me, MessageListAlignment.STANDARD)
        )
        assertFalse(showsAvatar(message))
    }

    @Test
    fun threadHeader_group_outgoingParent_noAvatar() {
        val message = parent(me, CometChatConstants.RECEIVER_TYPE_GROUP)
        assertFalse(showsAvatar(message))
    }

    @Test
    fun threadHeader_group_incomingParent_showsAvatar() {
        val message = parent(other, CometChatConstants.RECEIVER_TYPE_GROUP)
        assertTrue(showsAvatar(message))
    }

    @Test
    fun threadHeader_hideAvatarTrue_hidesInGroupIncoming() {
        val message = parent(other, CometChatConstants.RECEIVER_TYPE_GROUP)
        assertFalse(showsAvatar(message, hideAvatar = true))
        // hideAvatar stays the master override in every case
        listOf(
            parent(me, CometChatConstants.RECEIVER_TYPE_USER),
            parent(other, CometChatConstants.RECEIVER_TYPE_USER),
            parent(me, CometChatConstants.RECEIVER_TYPE_GROUP)
        ).forEach { msg ->
            MessageListAlignment.values().forEach { mode ->
                assertFalse(showsAvatar(msg, alignment = mode, hideAvatar = true))
            }
        }
    }

    /**
     * chatuikit-kotlin's thread header never sets `MessageAdapter.isAgentChat`, and its
     * `showLeftBubbleUserAvatar` defaults to false, so the agent's parent message in a 1:1 agent
     * chat shows no avatar there. The Compose header must do the same.
     */
    @Test
    fun threadHeader_agentChat_matchesKotlin() {
        val agentParent = parent(
            other,
            CometChatConstants.RECEIVER_TYPE_USER,
            senderRole = UIKitConstants.AIConstants.AGENTIC_USER
        )
        assertFalse(showsAvatar(agentParent))

        val ownParentInAgentChat = parent(me, CometChatConstants.RECEIVER_TYPE_USER)
        assertFalse(showsAvatar(ownParentInAgentChat))
    }

    @Test
    fun threadHeader_leftAligned_followsMessageListRule() {
        // LEFT_ALIGNED puts own messages on the LEFT, so the list rule applies to them too
        val ownGroup = parent(me, CometChatConstants.RECEIVER_TYPE_GROUP)
        assertEquals(
            MessageBubbleAlignment.LEFT,
            threadHeaderBubbleAlignment(ownGroup, me, MessageListAlignment.LEFT_ALIGNED)
        )
        assertTrue(showsAvatar(ownGroup, alignment = MessageListAlignment.LEFT_ALIGNED))

        val ownOneOnOne = parent(me, CometChatConstants.RECEIVER_TYPE_USER)
        assertFalse(showsAvatar(ownOneOnOne, alignment = MessageListAlignment.LEFT_ALIGNED))

        val incomingGroup = parent(other, CometChatConstants.RECEIVER_TYPE_GROUP)
        assertTrue(showsAvatar(incomingGroup, alignment = MessageListAlignment.LEFT_ALIGNED))
    }
}
