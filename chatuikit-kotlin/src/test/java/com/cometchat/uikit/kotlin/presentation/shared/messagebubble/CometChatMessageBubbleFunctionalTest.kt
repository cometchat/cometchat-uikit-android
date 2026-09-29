package com.cometchat.uikit.kotlin.presentation.shared.messagebubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Instrumented layer for the **View** container. Its job is routing and slots; the
 * child bubbles' own behaviour is covered by their tests.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatMessageBubbleFunctionalTest {

    private companion object {
        const val TEXT = "Hello"
        const val SLOT = "a custom slot"
    }

    private fun withBubble(block: (ComponentActivity, CometChatMessageBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(activity, CometChatMessageBubble(activity))
        }
        scenario.close()
    }

    /**
     * Visible text only. Clearing a slot leaves the child attached and sets the
     * container GONE, so a walk that ignored visibility would still see it.
     */
    private fun View.treeText(): String {
        val out = StringBuilder()
        fun walk(v: View) {
            if (v.visibility != View.VISIBLE) return
            if (v is TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return out.toString()
    }

    private fun slotView(activity: ComponentActivity) = TextView(activity).apply { text = SLOT }

    // ── routing ─────────────────────────────────────────────────────────────

    @Test
    fun aTextMessageRoutesToTheTextBubble() = withBubble { _, bubble ->
        bubble.setMessage(
            MockFactory.createTextMessage(text = TEXT),
            UIKitConstants.MessageBubbleAlignment.LEFT,
        )
        assertTrue("expected the text to render, got '${bubble.treeText()}'", bubble.treeText().contains(TEXT))
    }

    @Test
    fun aDeletedMessageRoutesToTheDeleteBubble() = withBubble { _, bubble ->
        bubble.setMessage(
            MockFactory.createDeletedMessage(),
            UIKitConstants.MessageBubbleAlignment.LEFT,
        )
        assertFalse(
            "a deleted message must not leak its content",
            bubble.treeText().contains("the original text"),
        )
    }

    @Test
    fun bothAlignmentsRenderTheContent() = withBubble { _, bubble ->
        bubble.setMessage(
            MockFactory.createTextMessage(text = TEXT),
            UIKitConstants.MessageBubbleAlignment.RIGHT,
        )
        assertTrue(bubble.treeText().contains(TEXT))
    }

    @Test
    fun theAlignmentCanBeChangedAfterBinding() = withBubble { _, bubble ->
        bubble.setMessage(
            MockFactory.createTextMessage(text = TEXT),
            UIKitConstants.MessageBubbleAlignment.LEFT,
        )
        bubble.setMessageAlignment(UIKitConstants.MessageBubbleAlignment.RIGHT)
        assertTrue(bubble.treeText().contains(TEXT))
    }

    @Test
    fun rebindingReplacesTheContent() = withBubble { _, bubble ->
        bubble.setMessage(
            MockFactory.createTextMessage(text = "first"),
            UIKitConstants.MessageBubbleAlignment.LEFT,
        )
        bubble.setMessage(
            MockFactory.createTextMessage(id = 2L, text = "second"),
            UIKitConstants.MessageBubbleAlignment.LEFT,
        )
        assertFalse("the previous message must not survive", bubble.treeText().contains("first"))
        assertTrue(bubble.treeText().contains("second"))
    }

    // ── slots ───────────────────────────────────────────────────────────────

    @Test
    fun aLeadingViewRenders() = withBubble { activity, bubble ->
        bubble.setLeadingView(slotView(activity))
        bubble.setMessage(
            MockFactory.createTextMessage(text = TEXT),
            UIKitConstants.MessageBubbleAlignment.LEFT,
        )
        assertTrue(bubble.treeText().contains(SLOT))
    }

    @Test
    fun aHeaderViewRenders() = withBubble { activity, bubble ->
        bubble.setHeaderView(slotView(activity))
        bubble.setMessage(
            MockFactory.createTextMessage(text = TEXT),
            UIKitConstants.MessageBubbleAlignment.LEFT,
        )
        assertTrue(bubble.treeText().contains(SLOT))
    }

    @Test
    fun aReplyViewRenders() = withBubble { activity, bubble ->
        bubble.setReplyView(slotView(activity))
        bubble.setMessage(
            MockFactory.createTextMessage(text = TEXT),
            UIKitConstants.MessageBubbleAlignment.LEFT,
        )
        assertTrue(bubble.treeText().contains(SLOT))
    }

    @Test
    fun aSlotCanBeClearedAgain() = withBubble { activity, bubble ->
        bubble.setHeaderView(slotView(activity))
        bubble.setMessage(
            MockFactory.createTextMessage(text = TEXT),
            UIKitConstants.MessageBubbleAlignment.LEFT,
        )
        bubble.setHeaderView(null)
        bubble.setMessage(
            MockFactory.createTextMessage(id = 2L, text = TEXT),
            UIKitConstants.MessageBubbleAlignment.LEFT,
        )
        assertFalse("clearing the slot should remove its view", bubble.treeText().contains(SLOT))
    }

    // ── multi-attachment routing ────────────────────────────────────────────

    @Test
    fun theMultiAttachmentFlagIsAccepted() = withBubble { _, bubble ->
        // Switches file/image/video/audio messages between the plural and singular
        // bubbles; the routing itself is asserted in the bubbles' own tests.
        bubble.setEnableMultipleAttachments(false)
        bubble.setMessage(
            MockFactory.createTextMessage(text = TEXT),
            UIKitConstants.MessageBubbleAlignment.LEFT,
        )
        assertTrue(bubble.treeText().contains(TEXT))
        bubble.setEnableMultipleAttachments(true)
    }
}
