package com.cometchat.uikit.kotlin.presentation.messageheader

import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.messageheader.ui.CometChatMessageHeader
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * ENG-41622 / D-354 — the chat header opened from an id-only chat row must render.
 *
 * Tapping a row whose peer is still id-only (BEH-CONV-01) hands the header a group or user with a
 * `null` name, which it passed into the avatar's non-null `name`. The header now falls back to
 * `""`, as the Compose header already does. A header with a name is unchanged.
 *
 * The header paints from a coroutine, so the NullPointerException does not fail a Robolectric test
 * by itself; each id-only test therefore binds a named entity first. Without the fix the paint
 * throws before the title is written and the previous name stays on screen, so reverting the fix
 * turns the two id-only tests red.
 *
 * Run: ./gradlew :chatuikit-kotlin:testDebugUnitTest --tests "*MessageHeaderIdOnlyTest"
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MessageHeaderIdOnlyTest {

    /** Hosts the header attached (it paints from its ViewModel on attach) and returns the name shown. */
    private fun shownName(bind: (CometChatMessageHeader) -> Unit): String {
        var name: String? = null
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val header = CometChatMessageHeader(activity)
            activity.setContentView(header)
            ShadowLooper.idleMainLooper()
            bind(header)
            ShadowLooper.idleMainLooper()
            name = header.findViewById<TextView>(R.id.tvMessageHeaderName).text.toString()
        }
        scenario.close()
        return requireNotNull(name) { "the header was never bound" }
    }

    private fun namedUser() = User().apply { uid = "e2e-user-named"; name = "Alice" }

    private fun namedGroup() = Group().apply {
        guid = "e2e-group-named"
        name = "Design team"
        groupType = CometChatConstants.GROUP_TYPE_PUBLIC
    }

    @Test
    fun idOnlyGroup_renders() {
        val shown = shownName {
            it.setGroup(namedGroup())
            ShadowLooper.idleMainLooper()
            it.setGroup(Group().apply { guid = "e2e-group-06b" })
        }
        assertEquals("", shown)
    }

    @Test
    fun idOnlyUser_renders() {
        val shown = shownName {
            it.setUser(namedUser())
            ShadowLooper.idleMainLooper()
            it.setUser(User().apply { uid = "e2e-user-b" })
        }
        assertEquals("", shown)
    }

    @Test
    fun namedGroup_keepsItsName() {
        assertEquals("Design team", shownName { it.setGroup(namedGroup()) })
    }
}
