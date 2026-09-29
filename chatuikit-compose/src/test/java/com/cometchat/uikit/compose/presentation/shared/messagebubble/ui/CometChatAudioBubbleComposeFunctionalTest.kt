package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.utils.AudioBubbleStateManager
import com.cometchat.uikit.core.utils.PlayState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Instrumented layer for the single [CometChatAudioBubble].
 *
 * Reached in the app only with multi-attachments switched off; the default route for
 * an audio message is [CometChatAudiosBubble]. Both are covered — this one is still
 * public API and still what an integrator gets when they turn the grid off.
 *
 * There is no network under Robolectric, so nothing ever downloads. That is the point
 * of most of these: the pre-download state is the state a user sees first, and the one
 * where a tap has the least to work with. It must show the file size, keep the play
 * affordance live, and survive a tap without crashing or wedging.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAudioBubbleComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val AUDIO_MIME = "audio/mpeg"
        const val AUDIO_EXT = "mp3"
        const val URL = "https://cdn.example.com/media_1.mp3"
    }

    @Before fun clearStates() = AudioBubbleStateManager.clearAll()

    @After fun clearStatesAfter() = AudioBubbleStateManager.clearAll()

    private fun audioMessage(sizeBytes: Int? = null): MediaMessage =
        MockFactory.createMediaMessage(
            count = 1,
            type = CometChatConstants.MESSAGE_TYPE_AUDIO,
            mimeType = AUDIO_MIME,
            extension = AUDIO_EXT,
        ).apply { sizeBytes?.let { attachment.fileSize = it } }

    private fun render(
        message: MediaMessage,
        alignment: UIKitConstants.MessageBubbleAlignment = UIKitConstants.MessageBubbleAlignment.LEFT,
    ) {
        composeRule.setContent {
            CometChatTheme { CometChatAudioBubble(message = message, alignment = alignment) }
        }
        composeRule.waitForIdle()
    }

    // ── what a reader sees before anything is downloaded ─────────────────────

    @Test
    fun rendersTheAudioAffordances() {
        render(audioMessage())
        composeRule.onNodeWithContentDescription("Audio message").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Audio waveform").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    @Test
    fun showsTheFileSizeUntilADurationIsKnown() {
        // 3,200,000 bytes from the shared fixture.
        render(audioMessage())
        composeRule.onNodeWithText("3.1 MB").assertIsDisplayed()
    }

    @Test
    fun formatsSubMegabyteSizesInKilobytes() {
        render(audioMessage(sizeBytes = 204_800))
        composeRule.onNodeWithText("200 KB").assertIsDisplayed()
    }

    @Test
    fun formatsTinySizesInBytes() {
        render(audioMessage(sizeBytes = 512))
        composeRule.onNodeWithText("512 B").assertIsDisplayed()
    }

    @Test
    fun withNoSize_fallsBackToAZeroClockRatherThanAnEmptyLine() {
        render(audioMessage(sizeBytes = 0))
        composeRule.onNodeWithText("00:00 / 00:00").assertIsDisplayed()
    }

    // ── state, and the manager behind it ─────────────────────────────────────

    @Test
    fun registersPlaybackStateUnderTheMessageId() {
        val message = audioMessage()
        render(message)
        val state = AudioBubbleStateManager.peek(message.id.toInt())
        assertNotNull("the bubble should register a playback state it can restore from", state)
        assertEquals(URL, state!!.audioUrl)
        assertEquals(PlayState.INIT, state.playState)
    }

    @Test
    fun aRecycledBubbleGetsItsOwnStateBack() {
        // Scrolling a row out of the list and back disposes the bubble and composes a
        // fresh one for the same message. It must find the state it left behind, or
        // playback restarts from zero on every scroll.
        val message = audioMessage()
        var visible by mutableStateOf(true)
        composeRule.setContent {
            CometChatTheme {
                if (visible) {
                    CometChatAudioBubble(
                        message = message,
                        alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val first = AudioBubbleStateManager.peek(message.id.toInt())
        assertNotNull(first)

        visible = false
        composeRule.waitForIdle()
        visible = true
        composeRule.waitForIdle()

        assertTrue(
            "recycling must not create a second player for one message",
            first === AudioBubbleStateManager.peek(message.id.toInt()),
        )
    }

    // ── tapping play with nothing downloaded ─────────────────────────────────

    @Test
    fun tappingPlay_withNoNetwork_leavesTheBubbleStanding() {
        // The tap starts a real download coroutine that cannot succeed here. Whether it
        // has failed yet by the time the test looks is a matter of how long the socket
        // takes to give up, so the assertion is on what must hold either way: the
        // bubble is still there and still shows its waveform, rather than collapsing or
        // throwing out of the coroutine. The play *state* is pinned separately below,
        // where it is deterministic.
        render(audioMessage())
        composeRule.onNodeWithContentDescription("Play").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Audio message").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Audio waveform").assertIsDisplayed()
    }

    @Test
    fun tappingPlay_doesNotAdvancePlayState_whenNothingCouldBeFetched() {
        val message = audioMessage()
        render(message)
        composeRule.onNodeWithContentDescription("Play").performClick()
        composeRule.waitForIdle()
        assertEquals(
            PlayState.INIT,
            AudioBubbleStateManager.peek(message.id.toInt())!!.playState,
        )
    }

    // ── the url overload ─────────────────────────────────────────────────────

    @Test
    fun theUrlOverload_rendersWithoutAMessage() {
        // Public API for hosts that have a URL but no MediaMessage — e.g. a preview.
        composeRule.setContent {
            CometChatTheme {
                CometChatAudioBubble(
                    audioUrl = URL,
                    fileSize = 1_048_576,
                    alignment = UIKitConstants.MessageBubbleAlignment.RIGHT,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Audio message").assertIsDisplayed()
        composeRule.onNodeWithText("1.0 MB").assertIsDisplayed()
    }

    @Test
    fun theUrlOverload_keysItsStateOnTheUrlHash() {
        composeRule.setContent {
            CometChatTheme {
                CometChatAudioBubble(
                    audioUrl = URL,
                    fileSize = 0,
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                )
            }
        }
        composeRule.waitForIdle()
        assertNotNull(
            "two previews of the same URL should share playback state",
            AudioBubbleStateManager.peek(URL.hashCode()),
        )
    }

    // ── degenerate input ─────────────────────────────────────────────────────

    @Test
    fun withNoAttachment_stillRendersRatherThanCrashing() {
        val message = MockFactory.createMediaMessage(
            count = 0,
            type = CometChatConstants.MESSAGE_TYPE_AUDIO,
            mimeType = AUDIO_MIME,
            extension = AUDIO_EXT,
        )
        render(message)
        composeRule.onNodeWithContentDescription("Audio message").assertIsDisplayed()
    }

    @Test
    fun bothAlignmentsRenderTheSameControls() {
        // Outgoing differs only in palette — the affordances must not move or vanish.
        render(audioMessage(), UIKitConstants.MessageBubbleAlignment.RIGHT)
        assertEquals(1, composeRule.onAllNodesWithContentDescription("Play").fetchSemanticsNodes().size)
        composeRule.onNodeWithContentDescription("Audio waveform").assertIsDisplayed()
    }
}
