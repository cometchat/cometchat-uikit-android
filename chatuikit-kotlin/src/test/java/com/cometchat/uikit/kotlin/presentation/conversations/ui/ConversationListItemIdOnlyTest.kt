package com.cometchat.uikit.kotlin.presentation.conversations.ui

import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.kotlin.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * ENG-41622 / D-354 — a chat row whose peer carries only its id must render.
 *
 * The SDK writes an **id-only** `conversationWith` for an own-send or group conversation until the
 * next server refresh (BEH-CONV-01), so a group or user with a `null` name is a normal row. The row
 * passed that `null` into the avatar's non-null `name` and crashed the whole chat list with
 * `getName(...) must not be null`. The row now falls back to `""`, the same fallback the Compose
 * row and `CometChatAvatar.setAvatar(Group)` already use. A row with a name is unchanged.
 *
 * Reverting the fix turns the two id-only tests red (NullPointerException).
 *
 * Mirrors: chatuikit-compose `ui/ConversationListItemIdOnlyTest`.
 *
 * Run: ./gradlew :chatuikit-kotlin:testDebugUnitTest --tests "*ConversationListItemIdOnlyTest"
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConversationListItemIdOnlyTest {

    // Binding the row resolves receipts through CometChat.getLoggedInUser(); mocked so the test
    // never needs CometChat.init.
    private lateinit var cometChatMock: MockedStatic<CometChat>

    @Before
    fun setUp() {
        cometChatMock = Mockito.mockStatic(CometChat::class.java)
    }

    @After
    fun tearDown() {
        cometChatMock.close()
    }

    private fun groupConversation(group: Group) =
        Conversation(group.guid, CometChatConstants.CONVERSATION_TYPE_GROUP).apply { conversationWith = group }

    private fun userConversation(user: User) =
        Conversation("me_user_${user.uid}", CometChatConstants.CONVERSATION_TYPE_USER).apply { conversationWith = user }

    /** Binds [conversation] through the public `setConversation` and returns the shown title. */
    private fun boundTitle(conversation: Conversation): String {
        var title: String? = null
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val item = CometChatConversationListItem(activity)
            activity.setContentView(item)
            item.setConversation(conversation)
            title = item.findViewById<TextView>(R.id.tv_conversations_title).text.toString()
        }
        scenario.close()
        return requireNotNull(title) { "the row was never bound" }
    }

    @Test
    fun idOnlyGroupRow_renders() {
        val idOnly = Group().apply { guid = "e2e-group-06b" }
        assertEquals("", boundTitle(groupConversation(idOnly)))
    }

    @Test
    fun idOnlyUserRow_renders() {
        val idOnly = User().apply { uid = "e2e-user-b" }
        assertEquals("", boundTitle(userConversation(idOnly)))
    }

    @Test
    fun namedGroupRow_keepsItsName() {
        val named = Group().apply {
            guid = "e2e-group-named"
            name = "Design team"
            groupType = CometChatConstants.GROUP_TYPE_PUBLIC
        }
        assertEquals("Design team", boundTitle(groupConversation(named)))
    }
}
