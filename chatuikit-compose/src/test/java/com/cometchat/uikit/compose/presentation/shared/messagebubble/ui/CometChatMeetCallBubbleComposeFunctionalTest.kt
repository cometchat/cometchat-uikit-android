package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.theme.CometChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Instrumented layer for [CometChatMeetCallBubble].
 *
 * Driven through the data overload (title / subtitle / callType / sessionId). All
 * four `MeetCallType` values are exercised, since the type drives the icon and
 * direction treatment and is the only thing distinguishing them.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatMeetCallBubbleComposeFunctionalTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        title: String = "Video call",
        subtitle: String = "Tap to join",
        callType: MeetCallType = MeetCallType.VIDEO_INCOMING,
        sessionId: String = "session-1",
        onJoinClick: ((String) -> Unit)? = null,
    ) {
        composeRule.setContent {
            CometChatTheme {
                CometChatMeetCallBubble(
                    title = title,
                    subtitle = subtitle,
                    callType = callType,
                    sessionId = sessionId,
                    onJoinClick = onJoinClick,
                )
            }
        }
    }

    @Test
    fun rendersTitleAndSubtitle() {
        render()

        composeRule.onNodeWithText("Video call").assertIsDisplayed()
        composeRule.onNodeWithText("Tap to join").assertIsDisplayed()
    }

    @Test
    fun tappingJoin_handsBackTheSessionId() {
        // The callback hangs off a dedicated Join control, not the bubble body —
        // tapping the title does nothing.
        var joined: String? = null
        render(onJoinClick = { joined = it })

        composeRule.onNodeWithText("Join").performClick()
        composeRule.waitForIdle()

        assertEquals("session-1", joined)
    }

    @Test
    fun tappingTheTitle_doesNotJoin() {
        var joined: String? = null
        render(onJoinClick = { joined = it })

        composeRule.onNodeWithText("Video call").performClick()
        composeRule.waitForIdle()

        assertEquals("only the Join control joins", null, joined)
    }

    @Test
    fun voiceIncoming_renders() {
        render(title = "Voice call", callType = MeetCallType.VOICE_INCOMING)
        composeRule.onNodeWithText("Voice call").assertIsDisplayed()
    }

    @Test
    fun voiceOutgoing_renders() {
        render(title = "Voice call", callType = MeetCallType.VOICE_OUTGOING)
        composeRule.onNodeWithText("Voice call").assertIsDisplayed()
    }

    @Test
    fun videoOutgoing_renders() {
        render(callType = MeetCallType.VIDEO_OUTGOING)
        composeRule.onNodeWithText("Video call").assertIsDisplayed()
    }

    @Test
    fun emptySubtitle_rendersWithoutCrashing() {
        render(subtitle = "")
        composeRule.onNodeWithText("Video call").assertIsDisplayed()
    }
}
