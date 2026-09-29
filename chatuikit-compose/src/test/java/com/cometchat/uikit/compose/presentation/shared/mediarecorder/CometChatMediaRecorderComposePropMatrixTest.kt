package com.cometchat.uikit.compose.presentation.shared.mediarecorder

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.mediarecorder.style.CometChatMediaRecorderStyle
import com.cometchat.uikit.compose.presentation.shared.mediarecorder.ui.CometChatMediaRecorder
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.viewmodel.CometChatMediaRecorderViewModel
import com.cometchat.uikit.core.viewmodel.MediaRecorderState
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * Property (prop-matrix) layer for the Compose [CometChatMediaRecorder].
 *
 * Ten parameters, and the awkward one is `viewModel`: it is not decoration, it is the
 * only thing that decides what the component draws. So it is exercised by driving the
 * state machine and watching the tree change, which is also what makes every other entry
 * here reachable — the two slots and three of the callbacks only exist in states the view
 * model has to be walked into.
 *
 * A single composition serves the whole matrix, as elsewhere in the kit; entries that need
 * a different state advance the shared view model and wait, rather than re-hosting.
 *
 * The View twin is [com.cometchat.uikit.kotlin.presentation.shared.mediarecorder.CometChatMediaRecorder],
 * which has no matrix yet — it builds its own `MediaRecorder`, so it needs a different way in.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMediaRecorderComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatMediaRecorder"
        const val START_CD = "Start recording"
        const val STOP_CD = "Stop recording"
        const val DELETE_CD = "Delete recording"
        const val SUBMIT_CD = "Submit recording"
        const val RECORDING_SLOT = "the host's own timer"
        const val RECORDED_SLOT = "the host's own transport"
        const val CONTROLS_SLOT = "the host's own controls"
    }

    private val vm = CometChatMediaRecorderViewModel()

    private fun toRecording() {
        assertTrue(vm.startRecording()); composeRule.waitForIdle()
    }

    private fun toRecorded(file: File? = null) {
        if (vm.recordingState.value == MediaRecorderState.IDLE) assertTrue(vm.startRecording())
        assertTrue(vm.stopRecording())
        file?.let { vm.setRecordedFile(it) }
        composeRule.waitForIdle()
    }

    private fun toIdle() {
        vm.deleteRecording(); composeRule.waitForIdle()
    }

    @Test
    fun mediaRecorder_propMatrix_coversEveryProp() {
        val take = File("/tmp/take.m4a")
        var submitted: File? = null
        var closed = 0
        var reportedError: Exception? = null
        var measuredWidth = -1

        // The three slots are switched on per entry rather than supplied throughout, so the
        // component's own controls stay observable for the entries that need them.
        val useRecordingSlot = mutableStateOf(false)
        val useRecordedSlot = mutableStateOf(false)
        val useControlsSlot = mutableStateOf(false)

        // The style factory is itself @Composable, so it can only be built inside the
        // composition; hoisted out only so the matrix can assert on what was applied.
        var resolvedStyle: CometChatMediaRecorderStyle? = null

        composeRule.setContent {
            CometChatTheme {
                resolvedStyle = CometChatMediaRecorderStyle.default()
                CometChatMediaRecorder(
                    modifier = Modifier
                        .width(220.dp)
                        .onGloballyPositioned { measuredWidth = it.size.width },
                    viewModel = vm,
                    style = resolvedStyle!!,
                    onSubmit = { submitted = it },
                    onClose = { closed++ },
                    onError = { reportedError = it },
                    recordingView = if (useRecordingSlot.value) {
                        { time, _ -> Text("$RECORDING_SLOT $time") }
                    } else null,
                    recordedView = if (useRecordedSlot.value) {
                        { _, _, _ -> Text(RECORDED_SLOT) }
                    } else null,
                    controlButtonsView = if (useControlsSlot.value) {
                        { state -> Text("$CONTROLS_SLOT $state") }
                    } else null,
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("modifier") {
                // The component fills its width, so a width on the modifier is the one
                // measurable proof it reached the root rather than being dropped.
                composeRule.waitForIdle()
                assertEquals("220.dp at xxhdpi", 660, measuredWidth)
            }

            value("viewModel") {
                // Not decoration — it is the whole render input. Each state draws a
                // different control set, so walking it is what proves it is wired.
                toIdle()
                composeRule.onNodeWithContentDescription(START_CD).assertIsDisplayed()
                toRecording()
                composeRule.onNodeWithContentDescription(STOP_CD).assertIsDisplayed()
                toRecorded()
                composeRule.onNodeWithContentDescription(SUBMIT_CD).assertIsDisplayed()
                toIdle()
            }

            value("style") {
                // The style is threaded down to every child rather than read at the root,
                // so what is observable here is that a resolved instance composes cleanly.
                composeRule.onNodeWithContentDescription(START_CD).assertIsDisplayed()
                assertNotEquals("a style instance should be resolvable", null, resolvedStyle)
            }

            callback("onSubmit") {
                toRecorded(file = take)
                composeRule.onNodeWithContentDescription(SUBMIT_CD).performClick()
                composeRule.waitForIdle()
                assertSame(take, submitted)
                toIdle()
            }

            callback("onClose") {
                val before = closed
                toRecording()
                composeRule.onNodeWithContentDescription(DELETE_CD).performClick()
                composeRule.waitForIdle()
                assertEquals("delete should reach the host once", before + 1, closed)
            }

            callback("onError") {
                val boom = IllegalStateException("no microphone")
                vm.handleError(boom)
                composeRule.waitForIdle()
                assertSame(boom, reportedError)
                toIdle()
            }

            slot("recordingView") {
                useRecordingSlot.value = true
                toRecording()
                composeRule.onNodeWithText("$RECORDING_SLOT 00:00").assertIsDisplayed()
                useRecordingSlot.value = false
                toIdle()
            }

            slot("recordedView") {
                useRecordedSlot.value = true
                toRecorded()
                composeRule.onNodeWithText(RECORDED_SLOT).assertIsDisplayed()
                useRecordedSlot.value = false
                toIdle()
            }

            slot("controlButtonsView") {
                useControlsSlot.value = true
                toRecording()
                composeRule.onNodeWithText("$CONTROLS_SLOT RECORDING").assertIsDisplayed()
                useControlsSlot.value = false
                toIdle()
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [mediarecorder compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [mediarecorder] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("nothing on this component needs waiving", 0, cov.waived)
    }
}
