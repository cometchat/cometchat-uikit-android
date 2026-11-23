package com.cometchat.sampleapp.compose.push.appflow.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.sampleapp.compose.push.navigation.RouteEntities
import com.cometchat.uikit.compose.presentation.search.ui.CometChatSearch

/**
 * Search screen that integrates the CometChatSearch component.
 *
 * Provides search functionality across conversations and messages with support
 * for filter chips, debounced search, pagination, and contextual search within
 * specific user or group conversations.
 *
 * @param userId Optional user ID for contextual search within a specific user conversation
 * @param groupId Optional group ID for contextual search within a specific group conversation
 * @param onBackPress Callback when back button is pressed
 * @param onNavigateToMessages Callback to navigate to messages screen (userId, groupId, messageId)
 * @param onNavigateToThread Callback to navigate to thread messages screen (parentMessageId, goToMessageId)
 */
@Composable
fun SearchScreen(
    userId: String? = null,
    groupId: String? = null,
    onBackPress: () -> Unit,
    onNavigateToMessages: (userId: String?, groupId: String?, messageId: Long?) -> Unit = { _, _, _ -> },
    onNavigateToThread: (parentMessageId: Long, goToMessageId: Long) -> Unit = { _, _ -> }
) {
    CometChatSearch(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        uid = userId,
        guid = groupId,
        onBackPress = onBackPress,
        onConversationClick = { conversation: Conversation ->
            // Extract user or group from conversation and navigate. The chat opens on the object
            // the result carries, then refreshes it (ENG-41559 item 4); offline the server-only
            // lookup by id failed and the chat popped back.
            when (val conversationWith = conversation.conversationWith) {
                is User -> {
                    RouteEntities.hold(conversationWith, null)
                    onNavigateToMessages(conversationWith.uid, null, null)
                }
                is Group -> {
                    RouteEntities.hold(null, conversationWith)
                    onNavigateToMessages(null, conversationWith.guid, null)
                }
            }
        },
        onMessageClick = { message: BaseMessage ->
            val messageId = message.id.toLong()
            val parentMessageId = message.parentMessageId

            // If message is a thread reply, navigate to thread with goToMessageId
            if (parentMessageId > 0) {
                onNavigateToThread(parentMessageId, messageId)
                return@CometChatSearch
            }

            // Navigate to conversation containing this message and scroll to it, opening on the
            // user/group already held or the one the message carries (ENG-41559 item 4).
            RouteEntities.holdForMessage(message, com.cometchat.uikit.core.CometChatUIKit.getLoggedInUser()?.uid)
            val receiverType = message.receiverType
            val receiverId = message.receiverUid
            when (receiverType) {
                CometChatConstants.RECEIVER_TYPE_USER -> {
                    val loggedInUid = com.cometchat.uikit.core.CometChatUIKit.getLoggedInUser()?.uid
                    val otherUserId = if (message.sender?.uid == loggedInUid) receiverId else message.sender?.uid ?: receiverId
                    onNavigateToMessages(otherUserId, null, messageId)
                }
                CometChatConstants.RECEIVER_TYPE_GROUP -> {
                    onNavigateToMessages(null, receiverId, messageId)
                }
            }
        }
    )
}
