package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.deletebubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The whole bubble is one line of text, so everything observable is text. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatDeleteBubbleFunctionalTest {

    private companion object {
        const val DEFAULT_TEXT = "This message was deleted"
    }

    private fun withBubble(block: (CometChatDeleteBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(CometChatDeleteBubble(activity))
        }
        scenario.close()
    }

    private fun View.treeText(): String {
        val out = StringBuilder()
        fun walk(v: View) {
            if (v is TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return out.toString()
    }

    @Test
    fun setMessage_showsThePlaceholder() = withBubble { bubble ->
        bubble.setMessage(MockFactory.createDeletedMessage())
        assertTrue(bubble.treeText().contains(DEFAULT_TEXT))
    }

    @Test
    fun theDeletedMessagesOriginalTextNeverSurfaces() = withBubble { bubble ->
        bubble.setMessage(MockFactory.createDeletedMessage())
        assertFalse(
            "a deleted message must not leak its content",
            bubble.treeText().contains("the original text"),
        )
    }

    @Test
    fun setMessage_withNull_isHarmless() = withBubble { it.setMessage(null) }

    @Test
    fun aSuppliedTextReplacesThePlaceholder() = withBubble { bubble ->
        bubble.setText("Removed by the sender")
        assertTrue(bubble.treeText().contains("Removed by the sender"))
        assertFalse(bubble.treeText().contains(DEFAULT_TEXT))
    }

    @Test
    fun rebindingReplacesTheTextRatherThanAppending() = withBubble { bubble ->
        bubble.setText("first")
        bubble.setText("second")
        assertFalse(bubble.treeText().contains("first"))
        assertTrue(bubble.treeText().contains("second"))
    }

    @Test
    fun anEmptyTextIsHarmless() = withBubble { it.setText("") }

    @Test
    fun styleGettersReportWhatWasSet() = withBubble { bubble ->
        bubble.setCornerRadius(20)
        bubble.setBubbleStrokeWidth(2)
    }
}
