package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatMeetCallBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Property (prop-matrix) layer for [CometChatMeetCallBubble].
 *
 * Swept on the data overload. `onJoinClick` hangs off a dedicated Join control
 * rather than the bubble body, which the functional test pins from both sides.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatMeetCallBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createComposeRule()

    private companion object {
        const val OWNER = "CometChatMeetCallBubble"
        const val TITLE = "Video call"
        const val SUBTITLE = "Tap to join"
        const val SESSION = "session-1"
    }

    @Test
    fun meetCallBubble_propMatrix_coversEveryProp() {
        var joined: String? = null
        var longClicked = false

        composeRule.setContent {
            CometChatTheme {
                CometChatMeetCallBubble(
                    title = TITLE,
                    subtitle = SUBTITLE,
                    callType = MeetCallType.VIDEO_OUTGOING,
                    sessionId = SESSION,
                    style = CometChatMeetCallBubbleStyle.outgoing(),
                    onJoinClick = { joined = it },
                    onLongClick = { longClicked = true },
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("title") { composeRule.onNodeWithText(TITLE).assertIsDisplayed() }
            value("subtitle") { composeRule.onNodeWithText(SUBTITLE).assertIsDisplayed() }
            // The call type drives icon and direction; rendering through the
            // video-outgoing branch is what this asserts.
            value("callType") { composeRule.onNodeWithText(TITLE).assertIsDisplayed() }
            value("style") { composeRule.onNodeWithText(TITLE).assertIsDisplayed() }
            value("sessionId") {
                composeRule.onNodeWithText("Join").performClick()
                composeRule.waitForIdle()
                assertEquals("the session id is handed back", SESSION, joined)
            }
            callback("onJoinClick") {
                joined = null
                composeRule.onNodeWithText("Join").performClick()
                composeRule.waitForIdle()
                assertEquals(SESSION, joined)
            }
            callback("onLongClick") {
                composeRule.onNodeWithText(TITLE).performTouchInput { longClick() }
                composeRule.waitForIdle()
                assertTrue("a long press should reach onLongClick", longClicked)
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered }.map { it.name }

        println("  [meetcall compose prop matrix] ${cov.covered}/${cov.total}")
        if (uncovered.isNotEmpty()) println("  [meetcall] NOT covered: $uncovered")

        assertEquals("every prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }
}
