package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config

/**
 * SM-22: the `Call` overload labels every call status message the v5 way, so the rows of
 * one call read e.g. "Outgoing Call" / "Call accepted" / "Call Ended".
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatCallActionBubbleStatusLabelTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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
        Mockito.`when`(call.type).thenReturn(CometChatConstants.CALL_TYPE_VIDEO)
        Mockito.`when`(call.callStatus).thenReturn(status)
        Mockito.`when`(call.callInitiator).thenReturn(initiator)
        Mockito.`when`(call.receiverType).thenReturn(receiverType)
        return call
    }

    private fun assertLabel(call: Call, expected: String) {
        composeRule.setContent { CometChatTheme { CometChatCallActionBubble(message = call) } }
        composeRule.onNodeWithText(expected).assertExists()
    }

    @Test fun initiated_byMe_isOutgoing() =
        assertLabel(call(UIKitConstants.CallStatusConstants.INITIATED, me), "Outgoing Call")

    @Test fun ongoing_isAccepted() =
        assertLabel(call(UIKitConstants.CallStatusConstants.ONGOING, other), "Call accepted")

    @Test fun ended_isEnded() =
        assertLabel(call(UIKitConstants.CallStatusConstants.ENDED, me), "Call Ended")

    @Test fun rejected_isRejected() =
        assertLabel(call(UIKitConstants.CallStatusConstants.REJECTED, other), "Call Rejected")

    @Test fun unanswered_byOther_isMissed() =
        assertLabel(call(UIKitConstants.CallStatusConstants.UNANSWERED, other), "Missed Call")

    @Test fun groupCall_keepsTheTypeLabel() =
        assertLabel(
            call(UIKitConstants.CallStatusConstants.ENDED, other, CometChatConstants.RECEIVER_TYPE_GROUP),
            "Incoming video call"
        )
}
