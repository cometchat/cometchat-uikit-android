package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.actionbubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The centred system line; everything observable is text. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatActionBubbleFunctionalTest {

    private fun withBubble(block: (CometChatActionBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(CometChatActionBubble(activity))
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

    @Test fun setText_renders() = withBubble { bubble ->
        bubble.setText("Sender added Receiver to the group")
        assertTrue(bubble.treeText().contains("Sender added Receiver to the group"))
    }

    @Test fun rebindingReplacesTheText() = withBubble { bubble ->
        bubble.setText("first")
        bubble.setText("second")
        assertFalse(bubble.treeText().contains("first"))
        assertTrue(bubble.treeText().contains("second"))
    }

    @Test fun anEmptyTextIsHarmless() = withBubble { it.setText("") }

    @Test fun setMessage_withNull_isHarmless() = withBubble { it.setMessage(null) }

    @Test fun aLongLineIsKept() = withBubble { bubble ->
        val long = "Sender added Receiver, and a great many other people besides, to the group"
        bubble.setText(long)
        assertTrue(bubble.treeText().contains(long))
    }
}
