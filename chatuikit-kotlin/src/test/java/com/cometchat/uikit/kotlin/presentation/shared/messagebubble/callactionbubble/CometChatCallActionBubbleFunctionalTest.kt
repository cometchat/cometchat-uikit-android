package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.callactionbubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * `setCallType` is the entry point that does not need SDK state; the `Call` overload
 * resolves against the logged-in user, which the compose unit layer covers.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatCallActionBubbleFunctionalTest {

    private fun withBubble(block: (CometChatCallActionBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(CometChatCallActionBubble(activity))
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

    @Test fun missedAudioCall() = withBubble { bubble ->
        bubble.setCallType(CometChatConstants.CALL_TYPE_AUDIO, isMissed = true, isInitiator = false)
        assertTrue("expected a status line, got '${bubble.treeText()}'", bubble.treeText().isNotBlank())
    }

    @Test fun outgoingVideoCall() = withBubble { bubble ->
        bubble.setCallType(CometChatConstants.CALL_TYPE_VIDEO, isMissed = false, isInitiator = true)
        assertTrue(bubble.treeText().isNotBlank())
    }

    @Test fun incomingVideoCall() = withBubble { bubble ->
        bubble.setCallType(CometChatConstants.CALL_TYPE_VIDEO, isMissed = false, isInitiator = false)
        assertTrue(bubble.treeText().isNotBlank())
    }

    @Test fun rebindingReplacesTheStatus() = withBubble { bubble ->
        bubble.setCallType(CometChatConstants.CALL_TYPE_AUDIO, isMissed = true, isInitiator = false)
        val missed = bubble.treeText()
        bubble.setCallType(CometChatConstants.CALL_TYPE_VIDEO, isMissed = false, isInitiator = true)
        assertTrue("the status should change with the call type", bubble.treeText() != missed)
    }

    @Test fun setMessage_withNull_isHarmless() = withBubble { it.setMessage(null) }
}
