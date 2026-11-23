package com.cometchat.uikit.compose.presentation.conversations.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.theme.CometChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * ENG-41622 / D-354 — a chat row whose peer carries only its id must render, in the Compose kit too.
 *
 * The SDK writes an **id-only** `conversationWith` for an own-send or group conversation until the
 * next server refresh (BEH-CONV-01). The Compose row already falls back to `""` for a `null` name
 * (`ConversationUtils.getConversationTitle`); this pins it, so the two kits cannot drift. A row with
 * a name still shows it.
 *
 * Mirrors: chatuikit-kotlin `ui/ConversationListItemIdOnlyTest`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class ConversationListItemIdOnlyTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(conversation: Conversation) {
        composeRule.setContent {
            CometChatTheme {
                CometChatConversationListItem(conversation = conversation, onItemClick = {})
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun idOnlyGroupRow_renders() {
        val idOnly = Group().apply { guid = "e2e-group-06b" }
        render(Conversation(idOnly.guid, CometChatConstants.CONVERSATION_TYPE_GROUP).apply { conversationWith = idOnly })
        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun idOnlyUserRow_renders() {
        val idOnly = User().apply { uid = "e2e-user-b" }
        render(Conversation("me_user_e2e-user-b", CometChatConstants.CONVERSATION_TYPE_USER).apply { conversationWith = idOnly })
        composeRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun namedGroupRow_keepsItsName() {
        val named = Group().apply {
            guid = "e2e-group-named"
            name = "Design team"
            groupType = CometChatConstants.GROUP_TYPE_PUBLIC
        }
        render(Conversation(named.guid, CometChatConstants.CONVERSATION_TYPE_GROUP).apply { conversationWith = named })
        composeRule.onNodeWithText("Design team").assertIsDisplayed()
    }
}
