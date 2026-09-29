package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.meetcallbubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/** — Instrumented layer for the **View** [CometChatMeetCallBubble]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatMeetCallBubbleFunctionalTest {

    private fun withBubble(block: (CometChatMeetCallBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatMeetCallBubble(activity)
            activity.setContentView(bubble)
            ShadowLooper.idleMainLooper()
            block(bubble)
        }
        scenario.close()
    }

    private fun allText(root: View): String {
        val out = StringBuilder()
        fun walk(v: View) {
            if (v is TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(root); return out.toString()
    }

    @Test
    fun setMessage_rendersACallTitleAndJoinControl() {
        withBubble { b ->
            b.setMessage(MockFactory.createMeetCallMessage())
            ShadowLooper.idleMainLooper()

            val title = b.findViewById<TextView>(R.id.title_text)
            assertTrue("a call title should be rendered", title.text.isNotEmpty())
            assertTrue("the join control should be present", allText(b).contains("Join"))
        }
    }

    @Test
    fun theJoinControlIsVisible() {
        withBubble { b ->
            b.setMessage(MockFactory.createMeetCallMessage())
            ShadowLooper.idleMainLooper()

            assertEquals(View.VISIBLE, b.findViewById<View>(R.id.join_call).visibility)
        }
    }

    @Test
    fun theCallIconIsRendered() {
        withBubble { b ->
            b.setMessage(MockFactory.createMeetCallMessage())
            ShadowLooper.idleMainLooper()

            assertEquals(View.VISIBLE, b.findViewById<View>(R.id.call_icon_card).visibility)
        }
    }

    @Test
    fun aSecondMessage_replacesTheFirst() {
        withBubble { b ->
            b.setMessage(MockFactory.createMeetCallMessage(sessionId = "one"))
            b.setMessage(MockFactory.createMeetCallMessage(sessionId = "two"))
            ShadowLooper.idleMainLooper()

            assertTrue(b.findViewById<TextView>(R.id.title_text).text.isNotEmpty())
        }
    }
}
