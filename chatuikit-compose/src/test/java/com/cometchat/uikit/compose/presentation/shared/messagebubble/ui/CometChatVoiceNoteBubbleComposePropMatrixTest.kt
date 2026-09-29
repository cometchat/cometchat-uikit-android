package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatAudioBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.utils.AudioBubbleStateManager
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Property (prop-matrix) layer for [CometChatVoiceNoteBubble].
 *
 * The bubble is a single delegating call to [CometChatAudioBubble] — it exists so the
 * renderer can route recorded voice notes separately from picker audio, and it adds no
 * rendering of its own. So what the matrix pins is *forwarding*: every declared param
 * reaches the delegate, rather than the audio bubble's own behaviour a second time
 * (that lives in [CometChatAudioBubbleComposePropMatrixTest]).
 *
 * The View toolkit has no equivalent class — it routes voice notes through
 * `CometChatAudiosBubble` to the embedded audio bubble, and is covered there.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatVoiceNoteBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before fun clearStates() = AudioBubbleStateManager.clearAll()

    @After fun clearStatesAfter() = AudioBubbleStateManager.clearAll()

    private companion object {
        const val OWNER = "CometChatVoiceNoteBubble"

        /** Rendered from the attachment's size, so seeing it proves the message was forwarded. */
        const val SIZE_LABEL = "3.1 MB"
        const val AUDIO_CD = "Audio message"
    }

    private fun voiceNote(): MediaMessage = MockFactory.createMediaMessage(
        count = 1,
        type = CometChatConstants.MESSAGE_TYPE_AUDIO,
        mimeType = "audio/mpeg",
        extension = "mp3",
    )

    @Test
    fun voiceNoteBubble_propMatrix_coversEveryProp() {
        var longClicked = false
        var incoming: CometChatAudioBubbleStyle? = null
        var outgoing: CometChatAudioBubbleStyle? = null

        composeRule.setContent {
            CometChatTheme {
                incoming = CometChatAudioBubbleStyle.incoming()
                outgoing = CometChatAudioBubbleStyle.outgoing()
                CometChatVoiceNoteBubble(
                    message = voiceNote(),
                    alignment = UIKitConstants.MessageBubbleAlignment.RIGHT,
                    style = CometChatAudioBubbleStyle.outgoing(),
                    onLongClick = { longClicked = true },
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("message") {
                composeRule.waitForIdle()
                // The size label is derived from the attachment the message carries.
                composeRule.onNodeWithText(SIZE_LABEL).assertIsDisplayed()
            }

            value("style") {
                composeRule.waitForIdle()
                // The supplied style reaches the delegate, which renders the player chrome.
                composeRule.onNodeWithContentDescription("Audio waveform").assertIsDisplayed()
                assertNotEquals("incoming() and outgoing() should differ", incoming, outgoing)
            }

            callback("onLongClick") {
                composeRule.onNodeWithContentDescription(AUDIO_CD).performTouchInput { longClick() }
                composeRule.waitForIdle()
                assertTrue("a long press should reach the integrator through the delegate", longClicked)
            }
        }

        val props = matrix.evaluate() + waivedProps()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [voicenote compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [voicenote] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("only alignment is waived", 1, cov.waived)
    }

    /**
     * Alignment is forwarded unchanged but is inert past the default-style selection:
     * [CometChatAudioBubble] never reads it for layout, and incoming/outgoing differ
     * only in colour, which Compose semantics do not expose. Which edge the bubble
     * sits on belongs to the container; the snapshot layer pins both sides.
     */
    private fun waivedProps(): List<Prop> = listOf(
        Prop(OWNER, "alignment", PropKind.VALUE, waived = true),
    )
}
