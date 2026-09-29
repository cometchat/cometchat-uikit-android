package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.collaborativebubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/** — Instrumented layer for the **View** [CometChatCollaborativeBubble]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatCollaborativeBubbleFunctionalTest {

    private fun withBubble(block: (CometChatCollaborativeBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatCollaborativeBubble(activity)
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
    fun setMessage_rendersTitleAndSubtitle() {
        withBubble { b ->
            b.setMessage(MockFactory.createCollaborativeMessage(title = "Shared Doc", subtitle = "Edit together"))
            ShadowLooper.idleMainLooper()
            val t = allText(b)
            assertTrue("got: $t", t.contains("Shared Doc"))
            assertTrue("got: $t", t.contains("Edit together"))
        }
    }

    @Test
    fun whiteboardMessage_renders() {
        withBubble { b ->
            b.setMessage(MockFactory.createCollaborativeMessage(whiteboard = true, title = "Shared Board"))
            ShadowLooper.idleMainLooper()
            assertTrue(allText(b).contains("Shared Board"))
        }
    }

    @Test
    fun setTitle_overridesTheRenderedTitle() {
        withBubble { b ->
            b.setMessage(MockFactory.createCollaborativeMessage(title = "First"))
            b.setTitle("Second")
            ShadowLooper.idleMainLooper()
            assertTrue(allText(b).contains("Second"))
        }
    }

    @Test
    fun emptyTitle_rendersWithoutCrashing() {
        withBubble { b ->
            b.setMessage(MockFactory.createCollaborativeMessage(title = ""))
            ShadowLooper.idleMainLooper()
            // Nothing to assert beyond this: the test is that composing and
        // settling above did not throw. A constant assertion here would only
        // disguise that.
        }
    }
}
