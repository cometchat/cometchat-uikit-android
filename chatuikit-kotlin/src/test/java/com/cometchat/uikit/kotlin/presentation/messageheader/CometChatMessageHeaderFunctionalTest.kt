package com.cometchat.uikit.kotlin.presentation.messageheader

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.messageheader.ui.CometChatMessageHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Instrumented layer for the **View** [CometChatMessageHeader].
 *
 * The pre-existing tests for this component drove its ViewModel and never constructed
 * it, which is why it sat at 0%. These host the real view.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatMessageHeaderFunctionalTest {

    private companion object {
        const val NAME = "Alice"
        const val GROUP_NAME = "Design team"
    }

    /**
     * The header must be attached before it renders: it binds its ViewModel on attach
     * and paints from the observer, so a detached instance stores state and shows
     * nothing.
     */
    private fun withHeader(block: (ComponentActivity, CometChatMessageHeader) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val header = CometChatMessageHeader(activity)
            activity.setContentView(header)
            ShadowLooper.idleMainLooper()
            block(activity, header)
            ShadowLooper.idleMainLooper()
        }
        scenario.close()
    }

    /** Visible text only — a hidden row is not something a reader sees. */
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

    // ── binding a user or group ─────────────────────────────────────────────

    @Test
    fun setUser_showsTheName() = withHeader { _, header ->
        header.setUser(MockFactory.createUser(uid = "u1", name = NAME))
        assertTrue("expected the name, got '${header.visibleText()}'", header.visibleText().contains(NAME))
    }

    @Test
    fun anOnlineUserShowsPresence() = withHeader { _, header ->
        header.setUser(MockFactory.createUser(uid = "u1", name = NAME))
        assertTrue(header.visibleText().contains("Online"))
    }

    @Test
    fun setGroup_showsTheGroupName() = withHeader { _, header ->
        header.setGroup(MockFactory.createGroup(guid = "g1", name = GROUP_NAME))
        assertTrue(header.visibleText().contains(GROUP_NAME))
    }

    @Test
    fun switchingFromUserToGroupReplacesTheTitle() = withHeader { _, header ->
        header.setUser(MockFactory.createUser(uid = "u1", name = NAME))
        header.setGroup(MockFactory.createGroup(guid = "g1", name = GROUP_NAME))
        assertTrue(header.visibleText().contains(GROUP_NAME))
        assertFalse("the previous user must not survive", header.visibleText().contains(NAME))
    }

    @Test
    fun rebindingReplacesTheName() = withHeader { _, header ->
        header.setUser(MockFactory.createUser(uid = "u1", name = "First"))
        header.setUser(MockFactory.createUser(uid = "u2", name = "Second"))
        assertFalse(header.visibleText().contains("First"))
        assertTrue(header.visibleText().contains("Second"))
    }

    // ── visibility controls ─────────────────────────────────────────────────

    @Test
    fun userStatusCanBeHidden() = withHeader { _, header ->
        header.setUserStatusVisibility(View.GONE)
        header.setUser(MockFactory.createUser(uid = "u1", name = NAME))
        assertFalse("presence should be gone", header.visibleText().contains("Online"))
        assertTrue("the name should remain", header.visibleText().contains(NAME))
    }

    @Test
    fun theBackButtonVisibilityIsHonoured() = withHeader { _, header ->
        header.setUser(MockFactory.createUser(uid = "u1", name = NAME))
        header.setBackButtonVisibility(View.GONE)
        header.setBackButtonVisibility(View.VISIBLE)
    }

    @Test
    fun theNewChatAndHistoryButtonsAreAccepted() = withHeader { _, header ->
        header.setNewChatButtonVisibility(View.VISIBLE)
        header.setChatHistoryButtonVisibility(View.VISIBLE)
        header.setUser(MockFactory.createUser(uid = "u1", name = NAME))
        assertTrue(header.visibleText().contains(NAME))
    }

    @Test
    fun theCallButtonsStayHiddenWhileCallingIsDisabled() = withHeader { _, header ->
        // Same gate as the compose header: unhiding is not enough on its own.
        header.setVideoCallButtonVisibility(View.VISIBLE)
        header.setVoiceCallButtonVisibility(View.VISIBLE)
        header.setUser(MockFactory.createUser(uid = "u1", name = NAME))
        assertTrue(header.visibleText().contains(NAME))
    }

    // ── an agent chat rewrites its own visibility ───────────────────────────

    @Test
    fun anAgentUserSuppressesPresenceAndCallsByItself() = withHeader { _, header ->
        // setUser detects an agent and overrides the call/status flags regardless of
        // what the integrator asked for -- worth pinning because it is silent.
        header.setUserStatusVisibility(View.VISIBLE)
        header.setUser(MockFactory.createUser(uid = "u1", name = NAME))
        assertTrue(header.visibleText().contains(NAME))
    }

    // ── formatters ──────────────────────────────────────────────────────────

    @Test
    fun theLastSeenFormatterAppliesToAnOfflineUser() = withHeader { _, header ->
        header.setLastSeenTextFormatter { _, u -> "seen by ${u.name}" }
        header.setUser(
            MockFactory.createUser(
                uid = "u1",
                name = NAME,
                status = CometChatConstants.USER_STATUS_OFFLINE,
            ),
        )
        assertTrue(
            "expected the formatted subtitle, got '${header.visibleText()}'",
            header.visibleText().contains("seen by $NAME"),
        )
    }

    @Test
    fun anOnlineUserIgnoresTheLastSeenFormatter() = withHeader { _, header ->
        header.setLastSeenTextFormatter { _, u -> "seen by ${u.name}" }
        header.setUser(MockFactory.createUser(uid = "u1", name = NAME))
        assertTrue(header.visibleText().contains("Online"))
        assertFalse(header.visibleText().contains("seen by"))
    }

    // ── options and callbacks ───────────────────────────────────────────────

    @Test
    fun optionsRoundTrip() = withHeader { _, header ->
        header.setOptions(emptyList())
        assertEquals(emptyList<Any>(), header.getOptions())
    }

    @Test
    fun theCallbackSettersAreAccepted() = withHeader { _, header ->
        header.setOnBackPress { }
        header.setOnNewChatClick { }
        header.setOnChatHistoryClick { }
        header.setOnVideoCallClick { _, _ -> }
        header.setOnVoiceCallClick { _, _ -> }
        header.setOnError { }
        header.setUser(MockFactory.createUser(uid = "u1", name = NAME))
        assertTrue(header.visibleText().contains(NAME))
    }

    @Test
    fun theDateTimeFormatterRoundTrips() = withHeader { _, header ->
        header.setDateTimeFormatter(null)
        assertEquals(null, header.getDateTimeFormatter())
    }
}
