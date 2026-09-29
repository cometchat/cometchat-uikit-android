package com.cometchat.uikit.core.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

/**
 * Unit layer for [AudioBubbleStateManager] and the pure half of
 * [AudioBubblePlaybackState].
 *
 * This singleton is why an audio bubble survives `LazyColumn` / `RecyclerView`
 * recycling: the state is keyed on message id and outlives the composable or the view
 * holder. Two consequences are worth pinning — a re-bind must get *the same* state
 * back (or playback restarts from zero every time the row scrolls past), and a
 * released id must not leak into the next session.
 *
 * No `MediaPlayer` is ever prepared here, so `playState` stays `INIT` and the
 * transport calls run on the null-player path — which is exactly the path a bubble
 * takes before its first download completes, and the one that must not throw.
 */
class AudioBubbleStateManagerTest : FunSpec({

    val url = "https://cdn.example.com/media_1.mp3"

    beforeTest { AudioBubbleStateManager.clearAll() }
    afterTest { AudioBubbleStateManager.clearAll() }

    // ── identity across recycling ───────────────────────────────────────────

    test("the same id gets the same state back") {
        val first = AudioBubbleStateManager.getOrCreate(7, url, null)
        AudioBubbleStateManager.getOrCreate(7, url, null) shouldBeSameInstanceAs first
    }

    test("later arguments are ignored for an id already held") {
        val first = AudioBubbleStateManager.getOrCreate(7, url, null)
        val second = AudioBubbleStateManager.getOrCreate(7, "https://other.example/x.mp3", "/tmp/x")
        second shouldBeSameInstanceAs first
        second.audioUrl shouldBe url
    }

    test("distinct ids stay apart") {
        AudioBubbleStateManager.getOrCreate(1, url, null) shouldNotBeSameInstanceAs
            AudioBubbleStateManager.getOrCreate(2, url, null)
    }

    test("a state carries its id, url and local path") {
        val state = AudioBubbleStateManager.getOrCreate(42, url, "/tmp/a.mp3")
        state.id shouldBe 42
        state.audioUrl shouldBe url
        state.localPath shouldBe "/tmp/a.mp3"
    }

    // ── peek, remove, clearAll ──────────────────────────────────────────────

    test("peek returns null for an id that was never registered") {
        AudioBubbleStateManager.peek(999).shouldBeNull()
    }

    test("peek does not create") {
        AudioBubbleStateManager.peek(5)
        AudioBubbleStateManager.peek(5).shouldBeNull()
    }

    test("peek finds what getOrCreate registered") {
        val created = AudioBubbleStateManager.getOrCreate(5, url, null)
        AudioBubbleStateManager.peek(5) shouldBeSameInstanceAs created
    }

    test("remove drops only that id") {
        AudioBubbleStateManager.getOrCreate(1, url, null)
        val kept = AudioBubbleStateManager.getOrCreate(2, url, null)
        AudioBubbleStateManager.remove(1)
        AudioBubbleStateManager.peek(1).shouldBeNull()
        AudioBubbleStateManager.peek(2) shouldBeSameInstanceAs kept
    }

    test("removing an unknown id is harmless") {
        AudioBubbleStateManager.remove(12345)
    }

    test("clearAll empties the registry") {
        AudioBubbleStateManager.getOrCreate(1, url, null)
        AudioBubbleStateManager.getOrCreate(2, url, null)
        AudioBubbleStateManager.clearAll()
        AudioBubbleStateManager.peek(1).shouldBeNull()
        AudioBubbleStateManager.peek(2).shouldBeNull()
    }

    test("a removed id comes back as a fresh state") {
        val first = AudioBubbleStateManager.getOrCreate(3, url, null)
        AudioBubbleStateManager.remove(3)
        AudioBubbleStateManager.getOrCreate(3, url, null) shouldNotBeSameInstanceAs first
    }

    test("pauseAllExcept is a safe no-op when nothing is playing") {
        AudioBubbleStateManager.getOrCreate(1, url, null)
        AudioBubbleStateManager.getOrCreate(2, url, null)
        AudioBubbleStateManager.pauseAllExcept(1)
        AudioBubbleStateManager.peek(2)!!.playState shouldBe PlayState.INIT
    }

    // ── the pure half of AudioBubblePlaybackState ───────────────────────────

    test("a fresh state starts at INIT with no progress") {
        val state = AudioBubbleStateManager.getOrCreate(1, url, null)
        state.playState shouldBe PlayState.INIT
        state.totalDuration shouldBeExactly 0L
        state.currentPosition shouldBeExactly 0L
        state.progress shouldBeExactly 0f
    }

    test("progress is zero, not NaN, while the duration is unknown") {
        // Guards the division: a bubble that has not prepared yet reports 0.
        val state = AudioBubbleStateManager.getOrCreate(1, url, null)
        state.seekToProgress(0.5f)
        state.progress shouldBeExactly 0f
        state.progress.isNaN() shouldBe false
    }

    test("transport calls on an unprepared state do not throw") {
        // Every one of these is reachable by tapping a bubble whose download has not
        // finished. None of them has a player to talk to.
        val state = AudioBubbleStateManager.getOrCreate(1, url, null)
        state.pause()
        state.stop()
        state.seekTo(1_000L)
        state.seekToProgress(1f)
        state.updatePosition()
        state.release()
        state.playState shouldBe PlayState.INIT
    }

    test("play without a local path stays at INIT rather than crashing") {
        val state = AudioBubbleStateManager.getOrCreate(1, url, null)
        state.play()
        state.playState shouldBe PlayState.INIT
    }

    test("seekTo is clamped to the known duration") {
        // totalDuration is 0 until prepared, so both ends collapse onto 0 — the point
        // is that a wild seek cannot push currentPosition out of range.
        val state = AudioBubbleStateManager.getOrCreate(1, url, null)
        state.seekTo(-5_000L)
        state.currentPosition shouldBeExactly 0L
        state.seekTo(Long.MAX_VALUE)
        state.currentPosition shouldBeExactly 0L
    }

    test("release returns the state to INIT") {
        val state = AudioBubbleStateManager.getOrCreate(1, url, "/tmp/a.mp3")
        state.release()
        state.playState shouldBe PlayState.INIT
        state.totalDuration shouldBeExactly 0L
        state.currentPosition shouldBeExactly 0L
    }
})
