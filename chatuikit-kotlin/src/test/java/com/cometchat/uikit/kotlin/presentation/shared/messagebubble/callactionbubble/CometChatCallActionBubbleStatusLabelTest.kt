package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.callactionbubble

import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config

/**
 * SM-22: every call status message gets its own v5 label, so the rows of one call read
 * e.g. "Outgoing Call" / "Call accepted" / "Call Ended" instead of "Voice Call" three times.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatCallActionBubbleStatusLabelTest {

    private lateinit var cometChat: MockedStatic<CometChat>
    private val me = MockFactory.createUser(uid = "me", name = "Me")
    private val other = MockFactory.createUser(uid = "other", name = "Other")

    @Before
    fun stubLoggedInUser() {
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After fun closeStatic() = cometChat.close()

    private fun call(status: String, initiator: User, receiverType: String = CometChatConstants.RECEIVER_TYPE_USER): Call {
        val call = Mockito.mock(Call::class.java)
        Mockito.`when`(call.type).thenReturn(CometChatConstants.CALL_TYPE_AUDIO)
        Mockito.`when`(call.callStatus).thenReturn(status)
        Mockito.`when`(call.callInitiator).thenReturn(initiator)
        Mockito.`when`(call.receiverType).thenReturn(receiverType)
        return call
    }

    private fun label(call: Call): String {
        var text = ""
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatCallActionBubble(activity)
            bubble.setMessage(call)
            text = bubble.findViewById<TextView>(R.id.cometchat_call_action_bubble_text).text.toString()
        }
        scenario.close()
        return text
    }

    @Test fun initiated_byMe_isOutgoing() =
        assertEquals("Outgoing Call", label(call(UIKitConstants.CallStatusConstants.INITIATED, me)))

    @Test fun initiated_byOther_isIncoming() =
        assertEquals("Incoming Call", label(call(UIKitConstants.CallStatusConstants.INITIATED, other)))

    @Test fun ongoing_isAccepted() =
        assertEquals("Call accepted", label(call(UIKitConstants.CallStatusConstants.ONGOING, me)))

    @Test fun ended_isEnded() =
        assertEquals("Call Ended", label(call(UIKitConstants.CallStatusConstants.ENDED, other)))

    @Test fun cancelled_byMe_isCancelled() =
        assertEquals("Call Cancelled", label(call(UIKitConstants.CallStatusConstants.CANCELLED, me)))

    @Test fun cancelled_byOther_isMissed() =
        assertEquals("Missed Call", label(call(UIKitConstants.CallStatusConstants.CANCELLED, other)))

    @Test fun rejected_isRejected() =
        assertEquals("Call Rejected", label(call(UIKitConstants.CallStatusConstants.REJECTED, me)))

    @Test fun unanswered_byMe_isUnanswered() =
        assertEquals("Call unanswered", label(call(UIKitConstants.CallStatusConstants.UNANSWERED, me)))

    @Test fun unanswered_byOther_isMissed() =
        assertEquals("Missed Call", label(call(UIKitConstants.CallStatusConstants.UNANSWERED, other)))

    @Test fun busy_byMe_isBusy() =
        assertEquals("Call Busy", label(call(UIKitConstants.CallStatusConstants.BUSY, me)))

    @Test fun groupCall_keepsTheTypeLabel() =
        assertEquals(
            "Voice Call",
            label(call(UIKitConstants.CallStatusConstants.ENDED, other, CometChatConstants.RECEIVER_TYPE_GROUP))
        )
}
