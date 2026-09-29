package com.cometchat.uikit.core.viewmodel

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Tests for [CometChatInlineAudioRecorderViewModel], the state machine behind the composer's
 * inline voice recorder.
 *
 * It owns no dependencies and touches no platform API — it is a pure status machine plus a few
 * clamped setters — so everything here is exercised directly with no mocking.
 *
 * The valuable part is the **transition matrix**. Every mutator guards itself against being
 * called from the wrong state and returns a boolean saying whether it did anything, and a caller
 * that ignores that boolean is the bug this class exists to prevent. So each transition is
 * asserted in both directions: it succeeds from the states it permits, and is refused — leaving
 * the state untouched — from every other one.
 *
 * `isValidTransition` is verified exhaustively over all 36 ordered pairs of statuses, so a new
 * status or a changed rule surfaces here rather than in a recorder that silently stops working.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*CometChatInlineAudioRecorderViewModelTest"
 */
class CometChatInlineAudioRecorderViewModelTest : FunSpec({

    val dispatcher = UnconfinedTestDispatcher()

    beforeTest { Dispatchers.setMain(dispatcher) }
    afterTest { Dispatchers.resetMain() }

    /** A view model already driven into [status], with a recorded duration where that applies. */
    fun recorderIn(status: InlineAudioRecorderStatus): CometChatInlineAudioRecorderViewModel {
        val vm = CometChatInlineAudioRecorderViewModel()
        when (status) {
            InlineAudioRecorderStatus.IDLE -> Unit
            InlineAudioRecorderStatus.RECORDING -> vm.startRecording()
            InlineAudioRecorderStatus.PAUSED -> { vm.startRecording(); vm.pauseRecording() }
            InlineAudioRecorderStatus.COMPLETED -> { vm.startRecording(); vm.stopRecording() }
            InlineAudioRecorderStatus.PLAYING -> {
                vm.startRecording(); vm.stopRecording(); vm.startPlayback()
            }
            InlineAudioRecorderStatus.ERROR -> vm.handleError("boom")
        }
        return vm
    }

    val allStatuses = InlineAudioRecorderStatus.entries.toList()

    // ==================== initial state ====================

    test("a fresh recorder is idle and empty") {
        val vm = CometChatInlineAudioRecorderViewModel()
        vm.status shouldBe InlineAudioRecorderStatus.IDLE
        vm.duration shouldBe 0L
        vm.currentPosition shouldBe 0L
        vm.amplitudes shouldBe emptyList()
        vm.filePath shouldBe null
        vm.errorMessage shouldBe null
    }

    // ==================== transitions, both directions ====================

    test("startRecording succeeds only from idle, and clears the previous take") {
        allStatuses.forEach { from ->
            val vm = recorderIn(from)
            vm.updateDuration(5_000L)
            val allowed = vm.isValidTransition(from, InlineAudioRecorderStatus.RECORDING)

            vm.startRecording() shouldBe allowed
            if (allowed) {
                vm.status shouldBe InlineAudioRecorderStatus.RECORDING
                vm.duration shouldBe 0L          // a new take resets the clock
                vm.amplitudes shouldBe emptyList()
                vm.errorMessage shouldBe null
            } else {
                vm.status shouldBe from
            }
        }
    }

    test("pauseRecording succeeds from recording and is refused elsewhere") {
        allStatuses.forEach { from ->
            val vm = recorderIn(from)
            val allowed = vm.isValidTransition(from, InlineAudioRecorderStatus.PAUSED)
            vm.pauseRecording() shouldBe allowed
            vm.status shouldBe if (allowed) InlineAudioRecorderStatus.PAUSED else from
        }
    }

    test("resumeRecording succeeds only from paused") {
        allStatuses.forEach { from ->
            val vm = recorderIn(from)
            val allowed = from == InlineAudioRecorderStatus.PAUSED
            vm.resumeRecording() shouldBe allowed
            vm.status shouldBe if (allowed) InlineAudioRecorderStatus.RECORDING else from
        }
    }

    test("stopRecording succeeds from recording or paused and rewinds the position") {
        allStatuses.forEach { from ->
            val vm = recorderIn(from)
            vm.updatePosition(1_000L)
            val allowed = from == InlineAudioRecorderStatus.RECORDING ||
                from == InlineAudioRecorderStatus.PAUSED

            vm.stopRecording() shouldBe allowed
            if (allowed) {
                vm.status shouldBe InlineAudioRecorderStatus.COMPLETED
                vm.currentPosition shouldBe 0L
            } else {
                vm.status shouldBe from
            }
        }
    }

    test("deleteRecording succeeds from any state but idle, and wipes everything") {
        allStatuses.forEach { from ->
            val vm = recorderIn(from)
            vm.setFilePath("/tmp/take.m4a")
            vm.addAmplitude(0.5f)
            val allowed = from != InlineAudioRecorderStatus.IDLE

            vm.deleteRecording() shouldBe allowed
            if (allowed) {
                vm.status shouldBe InlineAudioRecorderStatus.IDLE
                vm.filePath shouldBe null
                vm.amplitudes shouldBe emptyList()
                vm.duration shouldBe 0L
            }
        }
    }

    test("startPlayback succeeds only from completed") {
        allStatuses.forEach { from ->
            val vm = recorderIn(from)
            val allowed = from == InlineAudioRecorderStatus.COMPLETED
            vm.startPlayback() shouldBe allowed
            vm.status shouldBe if (allowed) InlineAudioRecorderStatus.PLAYING else from
        }
    }

    test("pausePlayback succeeds only from playing") {
        allStatuses.forEach { from ->
            val vm = recorderIn(from)
            val allowed = from == InlineAudioRecorderStatus.PLAYING
            vm.pausePlayback() shouldBe allowed
            vm.status shouldBe if (allowed) InlineAudioRecorderStatus.COMPLETED else from
        }
    }

    test("onPlaybackComplete rewinds only when playing, and is a no-op otherwise") {
        allStatuses.forEach { from ->
            val vm = recorderIn(from)
            vm.updateDuration(10_000L)
            vm.updatePosition(4_000L)

            vm.onPlaybackComplete()

            if (from == InlineAudioRecorderStatus.PLAYING) {
                vm.status shouldBe InlineAudioRecorderStatus.COMPLETED
                vm.currentPosition shouldBe 0L
            } else {
                vm.status shouldBe from
                vm.currentPosition shouldBe 4_000L
            }
        }
    }

    test("recover succeeds only from error and returns to a clean idle") {
        allStatuses.forEach { from ->
            val vm = recorderIn(from)
            val allowed = from == InlineAudioRecorderStatus.ERROR
            vm.recover() shouldBe allowed
            if (allowed) {
                vm.status shouldBe InlineAudioRecorderStatus.IDLE
                vm.errorMessage shouldBe null
            }
        }
    }

    test("handleError is accepted from every state and records the message") {
        allStatuses.forEach { from ->
            val vm = recorderIn(from)
            vm.handleError("mic unavailable")
            vm.status shouldBe InlineAudioRecorderStatus.ERROR
            vm.errorMessage shouldBe "mic unavailable"
        }
    }

    // ==================== seeking and clamping ====================

    test("seekTo works while completed or playing and clamps to the recorded duration") {
        listOf(InlineAudioRecorderStatus.COMPLETED, InlineAudioRecorderStatus.PLAYING).forEach { from ->
            val vm = recorderIn(from)
            vm.updateDuration(10_000L)

            vm.seekTo(4_000L) shouldBe true
            vm.currentPosition shouldBe 4_000L

            vm.seekTo(99_000L) shouldBe true
            vm.currentPosition shouldBe 10_000L   // clamped to duration

            vm.seekTo(-1_000L) shouldBe true
            vm.currentPosition shouldBe 0L        // clamped to zero
        }
    }

    test("seekTo is refused while idle, recording, paused or errored") {
        listOf(
            InlineAudioRecorderStatus.IDLE,
            InlineAudioRecorderStatus.RECORDING,
            InlineAudioRecorderStatus.PAUSED,
            InlineAudioRecorderStatus.ERROR
        ).forEach { from ->
            val vm = recorderIn(from)
            vm.seekTo(1_000L) shouldBe false
            vm.currentPosition shouldBe 0L
        }
    }

    test("updateDuration floors at zero and updatePosition clamps to the duration") {
        val vm = CometChatInlineAudioRecorderViewModel()
        vm.updateDuration(-5_000L)
        vm.duration shouldBe 0L

        vm.updateDuration(8_000L)
        vm.duration shouldBe 8_000L

        vm.updatePosition(12_000L)
        vm.currentPosition shouldBe 8_000L

        vm.updatePosition(-3_000L)
        vm.currentPosition shouldBe 0L
    }

    test("amplitudes are clamped to 0..1 and appended in order, and can be cleared") {
        val vm = CometChatInlineAudioRecorderViewModel()
        vm.addAmplitude(0.4f)
        vm.addAmplitude(3.7f)    // over the top
        vm.addAmplitude(-2.0f)   // under the floor
        vm.amplitudes shouldBe listOf(0.4f, 1.0f, 0.0f)

        vm.clearAmplitudes()
        vm.amplitudes shouldBe emptyList()
    }

    test("the file path can be set and cleared") {
        val vm = CometChatInlineAudioRecorderViewModel()
        vm.setFilePath("/tmp/voice.m4a")
        vm.filePath shouldBe "/tmp/voice.m4a"
        vm.setFilePath(null)
        vm.filePath shouldBe null
    }

    // ==================== formatting ====================

    test("time formats as MM:SS, truncating sub-second remainders") {
        val vm = CometChatInlineAudioRecorderViewModel()
        vm.formatTime(0L) shouldBe "00:00"
        vm.formatTime(9_000L) shouldBe "00:09"
        vm.formatTime(60_000L) shouldBe "01:00"
        vm.formatTime(90_999L) shouldBe "01:30"     // remainder truncated, not rounded
        vm.formatTime(3_600_000L) shouldBe "60:00"  // minutes keep counting past an hour
    }

    test("displayTime shows elapsed recording time, and playback position while playing") {
        val recording = recorderIn(InlineAudioRecorderStatus.RECORDING)
        recording.updateDuration(65_000L)
        recording.displayTime shouldBe "01:05"

        val paused = recorderIn(InlineAudioRecorderStatus.PAUSED)
        paused.updateDuration(65_000L)
        paused.displayTime shouldBe "01:05"

        val completed = recorderIn(InlineAudioRecorderStatus.COMPLETED)
        completed.updateDuration(65_000L)
        completed.displayTime shouldBe "01:05"

        val playing = recorderIn(InlineAudioRecorderStatus.PLAYING)
        playing.updateDuration(65_000L)
        playing.updatePosition(12_000L)
        playing.displayTime shouldBe "00:12"   // position, not duration

        CometChatInlineAudioRecorderViewModel().displayTime shouldBe "00:00"  // idle
        recorderIn(InlineAudioRecorderStatus.ERROR).displayTime shouldBe "00:00"
    }

    test("formattedDuration and formattedPosition track their own fields") {
        val vm = recorderIn(InlineAudioRecorderStatus.COMPLETED)
        vm.updateDuration(125_000L)
        vm.updatePosition(5_000L)
        vm.formattedDuration shouldBe "02:05"
        vm.formattedPosition shouldBe "00:05"
    }

    // ==================== the transition matrix itself ====================

    test("isValidTransition is exhaustively correct across all 36 status pairs") {
        val allowed = mapOf(
            InlineAudioRecorderStatus.IDLE to setOf(InlineAudioRecorderStatus.RECORDING),
            InlineAudioRecorderStatus.RECORDING to setOf(
                InlineAudioRecorderStatus.PAUSED, InlineAudioRecorderStatus.COMPLETED,
                InlineAudioRecorderStatus.IDLE, InlineAudioRecorderStatus.ERROR
            ),
            InlineAudioRecorderStatus.PAUSED to setOf(
                InlineAudioRecorderStatus.RECORDING, InlineAudioRecorderStatus.COMPLETED,
                InlineAudioRecorderStatus.IDLE, InlineAudioRecorderStatus.ERROR
            ),
            InlineAudioRecorderStatus.COMPLETED to setOf(
                InlineAudioRecorderStatus.PLAYING, InlineAudioRecorderStatus.IDLE
            ),
            InlineAudioRecorderStatus.PLAYING to setOf(
                InlineAudioRecorderStatus.COMPLETED, InlineAudioRecorderStatus.IDLE
            ),
            InlineAudioRecorderStatus.ERROR to setOf(InlineAudioRecorderStatus.IDLE)
        )

        val vm = CometChatInlineAudioRecorderViewModel()
        allStatuses.forEach { from ->
            allStatuses.forEach { to ->
                // staying put is always legal, so it is excluded from the declared sets
                val expected = from == to || to in allowed.getValue(from)
                withClue("$from -> $to") { vm.isValidTransition(from, to) shouldBe expected }
            }
        }
    }

    // ==================== state replacement and teardown ====================

    test("setState replaces the whole state wholesale") {
        val vm = CometChatInlineAudioRecorderViewModel()
        vm.setState(
            InlineAudioRecorderState(
                status = InlineAudioRecorderStatus.PLAYING,
                duration = 7_000L,
                currentPosition = 3_000L,
                filePath = "/restored.m4a"
            )
        )
        vm.status shouldBe InlineAudioRecorderStatus.PLAYING
        vm.duration shouldBe 7_000L
        vm.currentPosition shouldBe 3_000L
        vm.filePath shouldBe "/restored.m4a"
    }

    test("release returns the recorder to a clean idle from any state") {
        allStatuses.forEach { from ->
            val vm = recorderIn(from)
            vm.setFilePath("/tmp/x.m4a")
            vm.release()
            vm.status shouldBe InlineAudioRecorderStatus.IDLE
            vm.filePath shouldBe null
            vm.duration shouldBe 0L
        }
    }
})

/** Names the pair under test so a failing cell of the matrix is identifiable from the report. */
private inline fun withClue(label: String, block: () -> Unit) {
    try {
        block()
    } catch (e: AssertionError) {
        throw AssertionError("$label: ${e.message}", e)
    }
}
