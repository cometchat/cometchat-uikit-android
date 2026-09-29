package com.cometchat.uikit.compose.screenshots

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.mediarecorder.ui.CometChatMediaRecorder
import com.cometchat.uikit.compose.presentation.utils.RoborazziConfig
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.darkColorScheme
import com.cometchat.uikit.compose.theme.lightColorScheme
import com.cometchat.uikit.core.viewmodel.CometChatMediaRecorderViewModel
import com.cometchat.uikit.core.viewmodel.MediaRecorderState
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Snapshot layer for the Compose [CometChatMediaRecorder].
 *
 * The component has three states and they share almost no pixels — a lone record button,
 * a timer above a live visualiser, then a single transport row of delete / play / waveform
 * / duration / send. That is the whole reason this is worth photographing: the assertions
 * in the functional layer can say which controls exist, but not that the recorded row
 * actually fits five controls across a phone width without collapsing.
 *
 * Each state is reached by driving [CometChatMediaRecorderViewModel] rather than the
 * recorder, so no capture here depends on audio hardware, a permission grant, or a clock.
 * Amplitude and playback progress are set to fixed values for the same reason — left to
 * the manager they would be whatever the microphone heard, and the baseline would never
 * settle.
 *
 * Run:
 *   ./gradlew :chatuikit-compose:recordRoborazziDebug  --tests "*.CometChatMediaRecorderScreenshotTest"
 *   ./gradlew :chatuikit-compose:compareRoborazziDebug --tests "*.CometChatMediaRecorderScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMediaRecorderScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/mediarecorder"
        )
    )

    private companion object {
        val canvasLight = Color(0xFFF4F4F4)
        val canvasDark = Color(0xFF121212)
        val TAKE = File("/tmp/take.m4a")
    }

    /**
     * Builds the view model into [state] before it is ever composed, so the capture is of a
     * settled tree rather than one mid-transition.
     */
    private fun viewModelIn(
        state: MediaRecorderState,
        elapsedMs: Long = 0L,
        amplitude: Float = 0f,
        progress: Float = 0f,
        playing: Boolean = false,
        withFile: Boolean = true,
    ) = CometChatMediaRecorderViewModel().apply {
        when (state) {
            MediaRecorderState.IDLE -> Unit
            MediaRecorderState.RECORDING -> {
                startRecording()
                updateRecordingTime(elapsedMs)
                updateAmplitude(amplitude)
            }
            MediaRecorderState.RECORDED -> {
                startRecording()
                stopRecording()
                // Order matters: stopRecording resets the transport, so the playback values
                // have to land after it or the capture shows a zeroed row.
                updateRecordingTime(elapsedMs)
                updatePlaybackProgress(progress)
                if (playing) startPlayback()
                if (withFile) setRecordedFile(TAKE)
            }
        }
        assertEquals("the view model should be settled in $state before capture", state, recordingState.value)
    }

    private fun capture(vm: CometChatMediaRecorderViewModel, dark: Boolean = false, rtl: Boolean = false) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContent {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    CometChatTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (dark) canvasDark else canvasLight)
                                .padding(16.dp)
                        ) {
                            CometChatMediaRecorder(viewModel = vm)
                        }
                    }
                }
            }
        }
        scenario.onActivity { activity ->
            val root = activity.window.decorView
                .findViewById<ViewGroup>(android.R.id.content)
                .getChildAt(0)
            // A blank capture passes forever, so refuse to record one: the recorder is
            // never zero-sized and never childless when it has composed.
            assertTrue("nothing was laid out — the capture would be blank", root.width > 0 && root.height > 0)
            assertTrue("the recorder did not compose", (root as ViewGroup).childCount > 0)
            root.captureRoboImage(roborazziOptions = RoborazziConfig.options())
        }
        scenario.close()
    }

    // ── the three states ────────────────────────────────────────────────────

    @Test fun idle() = capture(viewModelIn(MediaRecorderState.IDLE))

    @Test fun recording() =
        capture(viewModelIn(MediaRecorderState.RECORDING, elapsedMs = 7_000L, amplitude = 0.6f))

    @Test fun recorded() =
        capture(viewModelIn(MediaRecorderState.RECORDED, elapsedMs = 7_000L))

    // ── dark ────────────────────────────────────────────────────────────────

    @Test fun idle_dark() = capture(viewModelIn(MediaRecorderState.IDLE), dark = true)

    @Test fun recording_dark() =
        capture(viewModelIn(MediaRecorderState.RECORDING, elapsedMs = 7_000L, amplitude = 0.6f), dark = true)

    @Test fun recorded_dark() =
        capture(viewModelIn(MediaRecorderState.RECORDED, elapsedMs = 7_000L), dark = true)

    // ── the states within a state ───────────────────────────────────────────

    /** A quiet microphone: the visualiser has to hold its shape at zero, not collapse. */
    @Test fun recording_silence() =
        capture(viewModelIn(MediaRecorderState.RECORDING, elapsedMs = 1_000L, amplitude = 0f))

    /** Two digits either side of the colon — the timer is the widest thing in this state. */
    @Test fun recording_longTake() =
        capture(viewModelIn(MediaRecorderState.RECORDING, elapsedMs = 754_000L, amplitude = 0.9f))

    /** Mid-playback: the play control becomes pause and the waveform carries progress. */
    @Test fun recorded_playing() =
        capture(viewModelIn(MediaRecorderState.RECORDED, elapsedMs = 7_000L, progress = 0.45f, playing = true))

    @Test fun recorded_playing_dark() =
        capture(
            viewModelIn(MediaRecorderState.RECORDED, elapsedMs = 7_000L, progress = 0.45f, playing = true),
            dark = true,
        )

    /** Right-to-left: the transport row is the one layout here with a meaningful order. */
    @Test fun recorded_rtl() =
        capture(viewModelIn(MediaRecorderState.RECORDED, elapsedMs = 7_000L), rtl = true)

    @Test fun recording_rtl() =
        capture(viewModelIn(MediaRecorderState.RECORDING, elapsedMs = 7_000L, amplitude = 0.6f), rtl = true)
}
