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
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.utils.AudioBubbleStateManager
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * [CometChatVoiceNoteBubble] is a single delegating call to [CometChatAudioBubble] —
 * it exists so the renderer can route recorded voice notes separately from picker
 * audio, and it adds no rendering of its own.
 *
 * What is worth pinning here is that the delegation happens and that each parameter
 * reaches the other side. The prop matrix lives next door in
 * [CometChatVoiceNoteBubbleComposePropMatrixTest] and declares the same forwarding
 * through the harness, so the coverage report can see it; it deliberately does not
 * re-assert the audio bubble's own behaviour. The View toolkit has no equivalent
 * class at all — it routes voice notes through `CometChatAudiosBubble` to the
 * embedded audio bubble, covered there.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatVoiceNoteBubbleComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before fun clearStates() = AudioBubbleStateManager.clearAll()

    @After fun clearStatesAfter() = AudioBubbleStateManager.clearAll()

    private fun voiceNote(): MediaMessage = MockFactory.createMediaMessage(
        count = 1,
        type = CometChatConstants.MESSAGE_TYPE_AUDIO,
        mimeType = "audio/mpeg",
        extension = "mp3",
    )

    private fun render(
        alignment: UIKitConstants.MessageBubbleAlignment = UIKitConstants.MessageBubbleAlignment.LEFT,
        onLongClick: (() -> Unit)? = null,
    ) {
        composeRule.setContent {
            CometChatTheme {
                CometChatVoiceNoteBubble(
                    message = voiceNote(),
                    alignment = alignment,
                    onLongClick = onLongClick,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun itRendersTheAudioPlayer() {
        render()
        composeRule.onNodeWithContentDescription("Audio message").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Audio waveform").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    @Test
    fun theMessageReachesTheAudioBubble() {
        // The size label comes from the attachment, so seeing it proves the message
        // was forwarded rather than dropped.
        render()
        composeRule.onNodeWithText("3.1 MB").assertIsDisplayed()
    }

    @Test
    fun bothAlignmentsRenderTheSameControls() {
        render(alignment = UIKitConstants.MessageBubbleAlignment.RIGHT)
        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    @Test
    fun theLongPressIsForwarded() {
        var longClicked = false
        render(onLongClick = { longClicked = true })
        composeRule.onNodeWithContentDescription("Audio message").performTouchInput { longClick() }
        composeRule.waitForIdle()
        assertTrue("the callback should reach the integrator through the delegate", longClicked)
    }
}
