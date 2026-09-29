package com.cometchat.uikit.kotlin.presentation.messagelist

import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.messagelist.ui.CometChatMessageList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Instrumented layer for the **View** [CometChatMessageList].
 *
 * Binding a user or group starts an SDK fetch that cannot succeed offline, so the
 * whole reachable surface is driven inside one test: a second test in this JVM would
 * inherit the dead coroutine as UncaughtExceptionsBeforeTest.
 *
 * What is assertable without the SDK is the configuration contract -- the setters the
 * message-list screen drives per conversation, and the getters a host reads back.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatMessageListFunctionalTest {

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubLoggedInUser() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After fun closeStatic() = cometChat.close()

    @Test
    fun theConfigurationSurface() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val list = CometChatMessageList(activity)
            activity.setContentView(list)
            ShadowLooper.idleMainLooper()

            // Style round-trips.
            val style = list.getStyle()
            list.setStyle(style)
            assertEquals(style, list.getStyle())

            // Behaviour flags the screen drives.
            list.setScrollToBottomOnNewMessage(false)
            list.setSwipeToReplyEnabled(false)
            list.setDisableSoundForMessages(true)

            // Request shaping.
            list.setMessagesRequestBuilder(null)
            list.setMessagesTypes(emptyList())
            list.setMessagesCategories(emptyList())

            // A thread list is the same component scoped to a parent message.
            list.setParentMessageId(42L)

            // Binding is what starts the fetch, so it goes last.
            list.setUser(MockFactory.createUser(uid = "u1", name = "Alice"))
            ShadowLooper.idleMainLooper()

            // With no SDK the fetch cannot resolve, so the list must sit on its loading
            // state rather than falling through to an empty conversation.
            assertEquals(
                "the loading state should be visible while the fetch is outstanding",
                View.VISIBLE,
                list.findViewById<View>(R.id.loading_state_view).visibility,
            )
        }
        scenario.close()
    }

    @Test
    fun aGroupBindsTheSameWay() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val list = CometChatMessageList(activity)
            activity.setContentView(list)
            ShadowLooper.idleMainLooper()
            list.setGroup(MockFactory.createGroup(guid = "g1", name = "Design"))
            ShadowLooper.idleMainLooper()
            assertEquals(
                "a group conversation loads the same way",
                View.VISIBLE,
                list.findViewById<View>(R.id.loading_state_view).visibility,
            )
        }
        scenario.close()
    }
}
