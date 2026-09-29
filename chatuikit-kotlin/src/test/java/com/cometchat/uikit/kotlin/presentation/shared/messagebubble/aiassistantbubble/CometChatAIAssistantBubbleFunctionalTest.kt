package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.aiassistantbubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.models.AIAssistantMessage
import com.cometchat.uikit.core.domain.model.StreamMessage
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Instrumented layer for the **View** [CometChatAIAssistantBubble].
 *
 * Like its Compose counterpart, this bubble renders markdown through Markwon into a
 * `TextView`, so assertions read the view tree rather than any semantics API.
 *
 * The reflective prop matrix defers `setMessage`, `setStreamMessage` and
 * `setAIStreamService` to this column — they need real domain objects.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAIAssistantBubbleFunctionalTest {

    private fun withBubble(block: (CometChatAIAssistantBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatAIAssistantBubble(activity)
            activity.setContentView(bubble)
            ShadowLooper.idleMainLooper()
            block(bubble)
        }
        scenario.close()
    }

    private fun renderedText(root: View): String {
        val out = StringBuilder()
        fun walk(v: View) {
            if (v is TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(root)
        return out.toString()
    }

    @Test
    fun setMessage_rendersTheBodyThroughMarkwon() {
        withBubble { bubble ->
            bubble.setMessage(AIAssistantMessage("receiver-1", "text", "Here is an answer"))
            ShadowLooper.idleMainLooper()

            assertTrue(
                "the body should reach a TextView; got: ${renderedText(bubble)}",
                renderedText(bubble).contains("Here is an answer"),
            )
        }
    }

    @Test
    fun setMessage_stripsMarkdownMarkers() {
        withBubble { bubble ->
            bubble.setMessage(AIAssistantMessage("receiver-1", "text", "a **bold** word"))
            ShadowLooper.idleMainLooper()

            assertTrue(renderedText(bubble).contains("a bold word"))
        }
    }

    @Test
    fun setMessage_rendersAFencedCodeBlock() {
        withBubble { bubble ->
            bubble.setMessage(AIAssistantMessage("receiver-1", "text", "before\n```\nval x = 1\n```\nafter"))
            ShadowLooper.idleMainLooper()

            assertTrue(renderedText(bubble).contains("val x = 1"))
        }
    }

    @Test
    fun setMessage_withEmptyBody_rendersWithoutCrashing() {
        withBubble { bubble ->
            bubble.setMessage(AIAssistantMessage("receiver-1", "text", ""))
            ShadowLooper.idleMainLooper()

            // Nothing to assert beyond this: the test is that composing and
        // settling above did not throw. A constant assertion here would only
        // disguise that.
        }
    }

    @Test
    fun setStreamMessage_composesWithoutAStreamService() {
        withBubble { bubble ->
            bubble.setAIStreamService(null)
            bubble.setStreamMessage(StreamMessage("receiver-1", "text", "streaming body"))
            ShadowLooper.idleMainLooper()

            // Nothing to assert beyond this: the test is that composing and
        // settling above did not throw. A constant assertion here would only
        // disguise that.
        }
    }

    @Test
    fun setTextColor_reachesTheRenderedBody() {
        // No getter on this bubble, so the effect is read where it lands: Markwon
        // paints the body with the supplied colour.
        withBubble { bubble ->
            bubble.setMessage(AIAssistantMessage("receiver-1", "text", "coloured body"))
            bubble.setTextColor(0xFF1188AA.toInt())
            ShadowLooper.idleMainLooper()

            var body: TextView? = null
            fun walk(v: View) {
                if (body != null) return
                if (v is TextView && v.text.isNotEmpty()) { body = v; return }
                if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
            }
            walk(bubble)
            assertEquals(0xFF1188AA.toInt(), body?.currentTextColor)
        }
    }

    @Test
    fun setAvatar_acceptsANameWithoutAUrl() {
        withBubble { bubble ->
            bubble.setMessage(AIAssistantMessage("receiver-1", "text", "with an avatar"))
            bubble.setAvatar("Assistant", null)
            ShadowLooper.idleMainLooper()

            assertTrue(renderedText(bubble).contains("with an avatar"))
        }
    }
}
