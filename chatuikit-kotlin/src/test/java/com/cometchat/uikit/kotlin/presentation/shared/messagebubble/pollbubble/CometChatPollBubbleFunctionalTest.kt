package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.pollbubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
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
 * Instrumented layer for the **View** [CometChatPollBubble].
 *
 * `setMessage` re-extracts the poll from JSON and reads
 * `CometChatUIKit.getLoggedInUser()` to mark the viewer's own vote, which delegates
 * to the SDK static. Uninitialised it throws and the extraction's catch turns that
 * into a silent empty poll — hence the stub.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatPollBubbleFunctionalTest {

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubLoggedInUser() {
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }
            .thenReturn(User().apply { uid = "logged-in-user"; name = "Me" })
    }

    @After
    fun tearDown() = cometChat.close()

    private fun withBubble(block: (CometChatPollBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatPollBubble(activity)
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
        walk(root)
        return out.toString()
    }

    @Test
    fun setMessage_rendersTheQuestion() {
        withBubble { bubble ->
            bubble.setMessage(MockFactory.createPollMessage(question = "Which colour?"))
            ShadowLooper.idleMainLooper()

            assertEquals(
                "Which colour?",
                bubble.findViewById<TextView>(R.id.tv_question).text.toString(),
            )
        }
    }

    @Test
    fun setMessage_rendersEveryOptionLabel() {
        withBubble { bubble ->
            bubble.setMessage(MockFactory.createPollMessage(options = listOf("Red", "Blue", "Green")))
            ShadowLooper.idleMainLooper()

            val text = allText(bubble)
            assertTrue("got: $text", text.contains("Red"))
            assertTrue("got: $text", text.contains("Blue"))
            assertTrue("got: $text", text.contains("Green"))
        }
    }

    @Test
    fun setMessage_withTwoOptions_rendersBoth() {
        withBubble { bubble ->
            bubble.setMessage(
                MockFactory.createPollMessage(
                    question = "Ship it?",
                    options = listOf("Yes", "No"),
                    counts = listOf(7, 4),
                )
            )
            ShadowLooper.idleMainLooper()

            val text = allText(bubble)
            assertTrue(text.contains("Yes"))
            assertTrue(text.contains("No"))
        }
    }

    @Test
    fun setMessage_withNoVotes_stillRendersTheOptions() {
        withBubble { bubble ->
            bubble.setMessage(MockFactory.createPollMessage(counts = listOf(0, 0, 0)))
            ShadowLooper.idleMainLooper()

            assertTrue(allText(bubble).contains("Red"))
        }
    }

    @Test
    fun setMessage_withNull_leavesTheBubbleIntact() {
        withBubble { bubble ->
            bubble.setMessage(null)
            ShadowLooper.idleMainLooper()

            assertTrue(bubble.findViewById<TextView>(R.id.tv_question).text.isNullOrEmpty())
        }
    }

    @Test
    fun setProgressColor_isAcceptedAndRerenders() {
        withBubble { bubble ->
            bubble.setMessage(MockFactory.createPollMessage())
            bubble.setProgressColor(0xFF1188AA.toInt())
            ShadowLooper.idleMainLooper()

            assertTrue(allText(bubble).contains("Red"))
        }
    }

    @Test
    fun aLongQuestionStillRenders() {
        val q = "A noticeably longer poll question that has to wrap across more than one line"
        withBubble { bubble ->
            bubble.setMessage(MockFactory.createPollMessage(question = q))
            ShadowLooper.idleMainLooper()

            assertEquals(q, bubble.findViewById<TextView>(R.id.tv_question).text.toString())
        }
    }
}
