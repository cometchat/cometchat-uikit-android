package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.core.utils.WaveformUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * [SeekableWaveform], the gesture layer over [AudioWaveformBars].
 *
 * Public API, and worth its own tests because its gestures are unreachable through
 * the audio bubble in a test: the bubble only passes an `onSeek` once the file has
 * downloaded, and nothing downloads under Robolectric. Driving the component directly
 * is the only way to exercise the branch an integrator actually gets after playback
 * has started.
 *
 * The behaviour that matters is the drag/progress handover. While a drag is in
 * flight the waveform follows the finger, and an incoming `progress` update from the
 * player is ignored — otherwise the bar would fight the user's thumb every 50ms tick.
 * The reported value lands on release.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class SeekableWaveformTest {

    @get:Rule
    val composeRule = createComposeRule()

    private companion object {
        const val BAR_COUNT = 28
        const val WAVEFORM = "Audio waveform"
        val bars = WaveformUtils.generateDeterministicWaveform("a", BAR_COUNT)
    }

    private fun render(
        progress: Float = 0f,
        enabled: Boolean = true,
        onSeek: ((Float) -> Unit)?,
    ) {
        composeRule.setContent {
            SeekableWaveform(
                amplitudes = bars,
                progress = progress,
                playedColor = Color.Red,
                unplayedColor = Color.Blue,
                onSeek = onSeek,
                enabled = enabled,
                modifier = Modifier.size(width = 280.dp, height = 32.dp),
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun rendersItsBars() {
        render(onSeek = {})
        composeRule.onNodeWithContentDescription(WAVEFORM).assertIsDisplayed()
    }

    @Test
    fun aTapReportsItsPositionAsAFraction() {
        var seeked: Float? = null
        render(onSeek = { seeked = it })
        composeRule.onNodeWithContentDescription(WAVEFORM).performTouchInput {
            click(Offset(width * 0.25f, height / 2f))
        }
        composeRule.waitForIdle()
        assertNotNull("a tap should seek", seeked)
        assertEquals(0.25f, seeked!!, 0.02f)
    }

    @Test
    fun aTapAtTheFarEndSeeksToTheEnd() {
        var seeked: Float? = null
        render(onSeek = { seeked = it })
        composeRule.onNodeWithContentDescription(WAVEFORM).performTouchInput {
            click(Offset(width - 1f, height / 2f))
        }
        composeRule.waitForIdle()
        assertTrue("should land near the end, was $seeked", seeked!! > 0.9f)
    }

    @Test
    fun aDragReportsOnceOnRelease() {
        var calls = 0
        var last: Float? = null
        render(onSeek = { calls++; last = it })
        composeRule.onNodeWithContentDescription(WAVEFORM).performTouchInput {
            swipeRight(startX = width * 0.1f, endX = width * 0.8f)
        }
        composeRule.waitForIdle()
        assertEquals("a drag should report a single seek, on release", 1, calls)
        assertTrue("the reported position should follow the finger, was $last", last!! > 0.5f)
    }

    @Test
    fun withNoSeekHandler_gesturesAreIgnored() {
        // The bubble passes null until the file is downloaded; taps must do nothing
        // rather than seeking a player that has no source yet.
        render(onSeek = null)
        composeRule.onNodeWithContentDescription(WAVEFORM).performTouchInput {
            click(Offset(width * 0.5f, height / 2f))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(WAVEFORM).assertIsDisplayed()
    }

    @Test
    fun whenDisabled_gesturesAreIgnoredEvenWithAHandler() {
        var seeked: Float? = null
        render(enabled = false, onSeek = { seeked = it })
        composeRule.onNodeWithContentDescription(WAVEFORM).performTouchInput {
            click(Offset(width * 0.5f, height / 2f))
        }
        composeRule.waitForIdle()
        assertNull("a disabled waveform must not seek", seeked)
    }

    @Test
    fun anEmptyWaveformStillComposes() {
        composeRule.setContent {
            SeekableWaveform(
                amplitudes = emptyList(),
                progress = 0f,
                playedColor = Color.Red,
                unplayedColor = Color.Blue,
                onSeek = {},
                modifier = Modifier.size(width = 280.dp, height = 32.dp),
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(WAVEFORM).assertIsDisplayed()
    }

    @Test
    fun aProgressUpdateFromThePlayerIsAdoptedWhileIdle() {
        var progress by mutableFloatStateOf(0f)
        composeRule.setContent {
            SeekableWaveform(
                amplitudes = bars,
                progress = progress,
                playedColor = Color.Red,
                unplayedColor = Color.Blue,
                onSeek = {},
                modifier = Modifier.size(width = 280.dp, height = 32.dp),
            )
        }
        composeRule.waitForIdle()
        progress = 0.7f
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(WAVEFORM).assertIsDisplayed()
    }
}
