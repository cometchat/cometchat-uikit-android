package com.cometchat.uikit.kotlin.presentation.reactionlist

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.reactionlist.ui.CometChatReactionList
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
 * First tests of any kind for the **View** [CometChatReactionList].
 *
 * Everything runs in one test method: binding a message starts an SDK fetch that
 * throws offline, and the dead coroutine surfaces at the start of the next test in
 * this JVM -- the same mechanism behind the UncaughtExceptionsBeforeTest failures on
 * ENG-39095.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatReactionListFunctionalTest {

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubLoggedInUser() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After fun closeStatic() = cometChat.close()

    private fun View.visibleText(): String {
        ShadowLooper.idleMainLooper()
        val out = StringBuilder()
        fun walk(v: View) {
            if (v.visibility != View.VISIBLE) return
            if (v is TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return out.toString()
    }

    private fun message(): BaseMessage = MockFactory.createTextMessage(text = "hello")

    @Test
    fun theReachableSurface() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val list = CometChatReactionList(activity)
            activity.setContentView(list)
            ShadowLooper.idleMainLooper()

            // Custom state views are held and handed back.
            val loading = TextView(activity).apply { text = "loading slot" }
            list.setLoadingView(loading)
            assertEquals(loading, list.getLoadingView())

            val error = TextView(activity).apply { text = "error slot" }
            list.setErrorView(error)
            assertEquals(error, list.getErrorView())

            list.setErrorText("something went wrong")

            // The style round-trips.
            val style = list.getStyle()
            list.setStyle(style)
            assertEquals(style, list.getStyle())

            // Binding a message is what starts the fetch, so it goes last.
            val m = message()
            list.setBaseMessage(m)
            ShadowLooper.idleMainLooper()
            assertEquals(m, list.getBaseMessage())

            // The fetch cannot succeed offline, so the list must be showing its loading
            // state and hiding the row list -- not showing an empty list as though the
            // message genuinely had no reactions.
            val loadingState = list.findViewById<View>(R.id.layout_loading_state)
            val userRows = list.findViewById<View>(R.id.recycler_view_users)
            assertEquals(
                "the loading state should be visible while the fetch is outstanding",
                View.VISIBLE,
                loadingState.visibility,
            )
            assertEquals(
                "the row list must stay hidden rather than reading as 'no reactions'",
                View.GONE,
                userRows.visibility,
            )
        }
        scenario.close()
    }
}
