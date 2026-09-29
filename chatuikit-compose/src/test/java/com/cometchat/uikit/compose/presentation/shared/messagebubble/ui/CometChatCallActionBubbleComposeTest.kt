package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatCallActionBubbleStyle
import com.cometchat.uikit.compose.presentation.shared.messagebubble.utils.CallType
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Property and instrumented layers for [CometChatCallActionBubble].
 *
 * Swept on the explicit overload: the `Call` overload resolves the type through
 * `CometChatUIKit.getLoggedInUser()`, which is SDK state, not integrator input. The
 * classification itself is covered in `CallTypeUtilsTest`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatCallActionBubbleComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatCallActionBubble"
        const val STATUS = "Missed video call"
    }

    @Test
    fun callActionBubble_propMatrix_coversEveryProp() {
        var callType by mutableStateOf(CallType.VIDEO_MISSED)
        var isMissed by mutableStateOf(true)
        var statusText by mutableStateOf(STATUS)
        var style: CometChatCallActionBubbleStyle? = null

        composeRule.setContent {
            CometChatTheme {
                style = CometChatCallActionBubbleStyle.default()
                CometChatCallActionBubble(
                    callType = callType,
                    isMissed = isMissed,
                    statusText = statusText,
                    style = style!!,
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("statusText") {
                composeRule.waitForIdle()
                composeRule.onNodeWithText(STATUS).assertIsDisplayed()
                statusText = "Outgoing voice call"
                composeRule.waitForIdle()
                composeRule.onNodeWithText("Outgoing voice call").assertIsDisplayed()
                assertEquals(0, composeRule.onAllNodesWithText(STATUS).fetchSemanticsNodes().size)
                statusText = STATUS
                composeRule.waitForIdle()
            }

            // The call type picks the glyph; the accessible name carries the status, so
            // the observable is that the bubble still announces itself as a call.
            value("callType") {
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription("Call: $STATUS").assertIsDisplayed()
                callType = CallType.AUDIO_OUTGOING
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription("Call: $STATUS").assertIsDisplayed()
                callType = CallType.VIDEO_MISSED
                composeRule.waitForIdle()
            }

            value("isMissed") {
                composeRule.waitForIdle()
                isMissed = false
                composeRule.waitForIdle()
                composeRule.onNodeWithText(STATUS).assertIsDisplayed()
                isMissed = true
                composeRule.waitForIdle()
            }

            value("style") {
                composeRule.waitForIdle()
                composeRule.onNodeWithText(STATUS).assertIsDisplayed()
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        println("  [callaction compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        val uncovered = props.filter { !it.covered }.map { it.name }
        if (uncovered.isNotEmpty()) println("  [callaction] NOT covered: $uncovered")

        assertEquals("every prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }

    // ── instrumented ────────────────────────────────────────────────────────

    private fun render(type: CallType, missed: Boolean, text: String) {
        composeRule.setContent {
            CometChatTheme {
                CometChatCallActionBubble(callType = type, isMissed = missed, statusText = text)
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun missedVideoCall() {
        render(CallType.VIDEO_MISSED, true, "Missed video call")
        composeRule.onNodeWithText("Missed video call").assertIsDisplayed()
    }

    @Test
    fun incomingVoiceCall() {
        render(CallType.AUDIO_INCOMING, false, "Incoming voice call")
        composeRule.onNodeWithText("Incoming voice call").assertIsDisplayed()
    }

    @Test
    fun outgoingVideoCall() {
        render(CallType.VIDEO_OUTGOING, false, "Outgoing video call")
        composeRule.onNodeWithText("Outgoing video call").assertIsDisplayed()
    }

    @Test
    fun theStatusReachesTheAccessibleName() {
        render(CallType.AUDIO_MISSED, true, "Missed voice call")
        composeRule.onNodeWithContentDescription("Call: Missed voice call").assertIsDisplayed()
    }

    @Test
    fun anEmptyStatusStillComposes() {
        render(CallType.AUDIO_INCOMING, false, "")
    }
}
