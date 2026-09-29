package com.cometchat.uikit.compose.presentation.shared.mediarecorder

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.mediarecorder.ui.CometChatMediaRecorder
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.viewmodel.CometChatMediaRecorderViewModel
import com.cometchat.uikit.core.viewmodel.MediaRecorderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * First render-and-assert coverage for the Compose [CometChatMediaRecorder].
 *
 * The component had property tests over its manager and its style, and nothing that had
 * ever composed it. The reason it looked hard is that it records audio — but it does not
 * read the recorder to decide what to draw. Every pixel comes off
 * [CometChatMediaRecorderViewModel], a plain state machine in `chatuikit-core` with no
 * Android media in its API, and that view model is a parameter with a `viewModel()`
 * default. Handing it one drives all three states by method call, so nothing here needs
 * a `MediaRecorder`, a permission grant, or a shadow.
 *
 * `MediaRecorderManager` is still constructed inside the composition and still owns the
 * hardware. What these tests pin is the half above it: which state shows which controls,
 * which callbacks the controls reach, and which slots replace which parts. Anything that
 * would need real audio — that tapping record actually records — is out of scope here and
 * belongs to the instrumented suite.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMediaRecorderComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val IDLE_CD = "Media recorder ready to record"
        const val RECORDED_CD = "Recording complete, ready to play or submit"
        const val START_CD = "Start recording"
        const val STOP_CD = "Stop recording"
        const val DELETE_CD = "Delete recording"
        const val PLAY_CD = "Play recording"
        const val SUBMIT_CD = "Submit recording"
    }

    private val vm = CometChatMediaRecorderViewModel()

    /** Walks the state machine to [target]; the transitions are guarded, so order matters. */
    private fun driveTo(target: MediaRecorderState, file: File? = null) {
        if (target == MediaRecorderState.IDLE) return
        assertTrue("startRecording should be accepted from IDLE", vm.startRecording())
        if (target == MediaRecorderState.RECORDED) {
            assertTrue("stopRecording should be accepted from RECORDING", vm.stopRecording())
            file?.let { vm.setRecordedFile(it) }
        }
        composeRule.waitForIdle()
    }

    private fun render(
        onSubmit: ((File) -> Unit)? = null,
        onClose: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null,
        recordingView: (@Composable (String, Float) -> Unit)? = null,
        recordedView: (@Composable (String, Float, Boolean) -> Unit)? = null,
        controlButtonsView: (@Composable (MediaRecorderState) -> Unit)? = null,
    ) {
        composeRule.setContent {
            CometChatTheme {
                CometChatMediaRecorder(
                    viewModel = vm,
                    onSubmit = onSubmit,
                    onClose = onClose,
                    onError = onError,
                    recordingView = recordingView,
                    recordedView = recordedView,
                    controlButtonsView = controlButtonsView,
                )
            }
        }
        composeRule.waitForIdle()
    }

    // ── the three states ────────────────────────────────────────────────────

    @Test
    fun itOpensIdleShowingOnlyTheRecordButton() {
        render()
        composeRule.onNodeWithContentDescription(IDLE_CD).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(START_CD).assertIsDisplayed()
        // The transport controls belong to the later states, not this one.
        assertEquals(0, composeRule.onAllNodesWithContentDescription(STOP_CD).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithContentDescription(DELETE_CD).fetchSemanticsNodes().size)
    }

    @Test
    fun recordingShowsTheTimerAndStopAndDelete() {
        render()
        driveTo(MediaRecorderState.RECORDING)
        composeRule.onNodeWithContentDescription(STOP_CD).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(DELETE_CD).assertIsDisplayed()
        composeRule.onNodeWithText("00:00").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithContentDescription(START_CD).fetchSemanticsNodes().size)
    }

    @Test
    fun recordedSwapsInThePlaybackRow() {
        render()
        driveTo(MediaRecorderState.RECORDED)
        composeRule.onNodeWithContentDescription(RECORDED_CD).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(PLAY_CD).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(DELETE_CD).assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithContentDescription(STOP_CD).fetchSemanticsNodes().size)
    }

    /**
     * The container's own description is the whole component's accessible name, and it is
     * the only place the elapsed time reaches a screen reader.
     */
    @Test
    fun theContainerAnnouncesTheElapsedTimeWhileRecording() {
        render()
        driveTo(MediaRecorderState.RECORDING)
        vm.updateRecordingTime(65_000L)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Recording in progress, 01:05 elapsed").assertIsDisplayed()
        composeRule.onNodeWithText("01:05").assertIsDisplayed()
    }

    // ── callbacks ───────────────────────────────────────────────────────────

    @Test
    fun deletingWhileRecordingReachesOnClose() {
        var closed = false
        render(onClose = { closed = true })
        driveTo(MediaRecorderState.RECORDING)
        composeRule.onNodeWithContentDescription(DELETE_CD).performClick()
        composeRule.waitForIdle()
        assertTrue("delete should tell the host to close", closed)
    }

    @Test
    fun deletingAfterRecordingAlsoReachesOnClose() {
        var closed = false
        render(onClose = { closed = true })
        driveTo(MediaRecorderState.RECORDED, file = File("/tmp/take.m4a"))
        composeRule.onNodeWithContentDescription(DELETE_CD).performClick()
        composeRule.waitForIdle()
        assertTrue(closed)
    }

    @Test
    fun submittingHandsBackTheRecordedFile() {
        val take = File("/tmp/take.m4a")
        var submitted: File? = null
        render(onSubmit = { submitted = it })
        driveTo(MediaRecorderState.RECORDED, file = take)
        composeRule.onNodeWithContentDescription(SUBMIT_CD).performClick()
        composeRule.waitForIdle()
        assertSame("the host should get the very file the view model holds", take, submitted)
    }

    /**
     * Reaching RECORDED without a file is a real state — the manager sets the file, and it
     * has not necessarily done so yet. The button stays present and stays labelled
     * "Submit recording"; the click reaches `recordedFile?.let { … }` and stops there, so
     * the host is never handed a null.
     *
     * Worth knowing rather than worth fixing: a `cometchat_a11y_submit_recording_disabled`
     * string exists, but only the deprecated `RecordedControlButtons` uses it, and the
     * live single-row layout never composes that. So an inert submit is indistinguishable
     * from a working one to a screen reader. In the ordinary flow the file lands before
     * the state does, which is why this has not bitten anyone.
     */
    @Test
    fun submitIsInertUntilThereIsAFile() {
        var submitted: File? = null
        render(onSubmit = { submitted = it })
        driveTo(MediaRecorderState.RECORDED, file = null)
        composeRule.onNodeWithContentDescription(SUBMIT_CD).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(SUBMIT_CD).performClick()
        composeRule.waitForIdle()
        assertNull("no file means no callback, not a null one", submitted)
    }

    @Test
    fun anErrorOnTheViewModelIsForwardedToTheHost() {
        var seen: Exception? = null
        render(onError = { seen = it })
        val boom = IllegalStateException("no microphone")
        vm.handleError(boom)
        composeRule.waitForIdle()
        assertSame(boom, seen)
        // handleError also unwinds the state machine, so the UI returns to idle.
        assertEquals(MediaRecorderState.IDLE, vm.recordingState.value)
        composeRule.onNodeWithContentDescription(START_CD).assertIsDisplayed()
    }

    // ── slots ───────────────────────────────────────────────────────────────

    @Test
    fun theRecordingSlotReplacesTheTimerButKeepsTheControls() {
        render(recordingView = { time, _ -> Text("mine at $time") })
        driveTo(MediaRecorderState.RECORDING)
        composeRule.onNodeWithText("mine at 00:00").assertIsDisplayed()
        // Only the content is the host's; stop and delete are still the component's job.
        composeRule.onNodeWithContentDescription(STOP_CD).assertIsDisplayed()
    }

    @Test
    fun theRecordedSlotReplacesTheWholeRowIncludingItsControls() {
        render(recordedView = { duration, _, playing -> Text("mine $duration playing=$playing") })
        driveTo(MediaRecorderState.RECORDED)
        composeRule.onNodeWithText("mine 00:00 playing=false").assertIsDisplayed()
        // Unlike the recording slot, this one takes the transport with it — the recorded
        // row is a single unit, so a host replacing it owns delete and submit too.
        assertEquals(0, composeRule.onAllNodesWithContentDescription(DELETE_CD).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithContentDescription(PLAY_CD).fetchSemanticsNodes().size)
    }

    @Test
    fun theControlButtonsSlotIsToldWhichStateItIsDrawingFor() {
        render(controlButtonsView = { state -> Text("controls for $state") })
        driveTo(MediaRecorderState.RECORDING)
        composeRule.onNodeWithText("controls for RECORDING").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithContentDescription(STOP_CD).fetchSemanticsNodes().size)
    }

    /**
     * The two recorded-state slots are checked in order — `recordedView` first, then
     * `controlButtonsView` — so a host supplying both gets only the former. Pinned because
     * it is a silent precedence, not an error.
     */
    @Test
    fun theRecordedSlotWinsOverTheControlButtonsSlot() {
        render(
            recordedView = { _, _, _ -> Text("recorded slot") },
            controlButtonsView = { _ -> Text("controls slot") },
        )
        driveTo(MediaRecorderState.RECORDED)
        composeRule.onNodeWithText("recorded slot").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("controls slot").fetchSemanticsNodes().size)
    }

    @Test
    fun theIdleStateTakesNoSlotsAtAll() {
        render(
            recordingView = { _, _ -> Text("recording slot") },
            recordedView = { _, _, _ -> Text("recorded slot") },
            controlButtonsView = { _ -> Text("controls slot") },
        )
        composeRule.onNodeWithContentDescription(START_CD).assertIsDisplayed()
        assertFalse(
            "idle draws its own record button regardless of what the host supplied",
            composeRule.onAllNodesWithText("controls slot").fetchSemanticsNodes().isNotEmpty(),
        )
    }
}
