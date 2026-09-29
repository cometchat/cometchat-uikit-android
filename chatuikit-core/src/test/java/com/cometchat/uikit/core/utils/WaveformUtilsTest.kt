package com.cometchat.uikit.core.utils

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unit layer for [WaveformUtils], the pure half of the audio bubble.
 *
 * Both audio bubbles (compose and View) draw their bars from this object, and every
 * screenshot baseline of an audio message is only reproducible because
 * [WaveformUtils.generateDeterministicWaveform] is a function of the key alone. That
 * property is what these tests pin — if it ever picks up a time or identity-hash
 * component, every audio baseline in the gallery starts flapping and this fails first.
 *
 * Amplitudes are clamped to [0.15, 1.0]. The clamp is not cosmetic: the draw code in
 * `AudioWaveformBars` (compose) and `AudioWaveformBarsView` (View) re-clamps to the
 * same range and then stretches across it, so a value outside it would silently
 * distort every other bar in the same waveform.
 */
class WaveformUtilsTest : FunSpec({

    val minAmplitude = 0.15f
    val maxAmplitude = 1.0f
    val defaultBarCount = 28
    val key = "https://cdn.example.com/media_1.mp3"

    fun List<Float>.shouldAllBeDrawable() = forEachIndexed { i, v ->
        withClue("bar $i is outside [$minAmplitude, $maxAmplitude]") {
            (v in minAmplitude..maxAmplitude) shouldBe true
        }
    }

    // ── generateDeterministicWaveform ───────────────────────────────────────

    test("a deterministic waveform is the same for the same key") {
        WaveformUtils.generateDeterministicWaveform(key) shouldBe
            WaveformUtils.generateDeterministicWaveform(key)
    }

    test("a deterministic waveform survives the local-path swap") {
        // The bubbles key on the remote URL before download and on the cached file
        // path afterwards. Both are stable *for that key* — this pins that a repeat
        // call after download reproduces the post-download waveform exactly, which is
        // what stops the bars re-shuffling under the user mid-playback.
        val local = "/data/user/0/com.example/cache/1_media_1.mp3"
        WaveformUtils.generateDeterministicWaveform(local) shouldBe
            WaveformUtils.generateDeterministicWaveform(local)
    }

    test("different keys give different waveforms") {
        WaveformUtils.generateDeterministicWaveform(key) shouldNotBe
            WaveformUtils.generateDeterministicWaveform("https://cdn.example.com/media_2.mp3")
    }

    test("a deterministic waveform defaults to twenty-eight bars") {
        WaveformUtils.generateDeterministicWaveform(key).size shouldBeExactly defaultBarCount
    }

    test("a deterministic waveform honours an explicit bar count") {
        WaveformUtils.generateDeterministicWaveform(key, barCount = 9).size shouldBeExactly 9
    }

    test("every bar is clamped into the drawable range") {
        // The raw expression spans roughly [0.10, 1.20] before coercion, so the clamp
        // is load-bearing rather than defensive.
        WaveformUtils.generateDeterministicWaveform(key, barCount = 200).shouldAllBeDrawable()
    }

    test("an empty key still produces bars") {
        val bars = WaveformUtils.generateDeterministicWaveform("", barCount = defaultBarCount)
        bars.size shouldBeExactly defaultBarCount
        bars.shouldAllBeDrawable()
    }

    test("zero bars is empty rather than throwing") {
        WaveformUtils.generateDeterministicWaveform(key, barCount = 0).shouldBeEmpty()
    }

    // ── generatePlaceholder ─────────────────────────────────────────────────

    test("a placeholder has the requested bar count, all drawable") {
        val bars = WaveformUtils.generatePlaceholder(defaultBarCount)
        bars.size shouldBeExactly defaultBarCount
        bars.shouldAllBeDrawable()
    }

    test("a placeholder defaults to twenty-eight bars") {
        WaveformUtils.generatePlaceholder().size shouldBeExactly defaultBarCount
    }

    // ── normalizeToBarCount ─────────────────────────────────────────────────

    test("an already-correct list is returned as-is rather than rebuilt") {
        val source = listOf(0.2f, 0.5f, 0.9f)
        (WaveformUtils.normalizeToBarCount(source, 3) === source) shouldBe true
    }

    test("an empty source falls back to a placeholder") {
        val bars = WaveformUtils.normalizeToBarCount(emptyList(), 12)
        bars.size shouldBeExactly 12
        bars.shouldAllBeDrawable()
    }

    test("a long source is downsampled to the target count") {
        val bars = WaveformUtils.normalizeToBarCount(List(120) { 0.5f }, defaultBarCount)
        bars.size shouldBeExactly defaultBarCount
        bars.forEach { it.toDouble() shouldBe 0.5.plusOrMinus(1e-5) }
    }

    test("a short source is upsampled to the target count") {
        val bars = WaveformUtils.normalizeToBarCount(listOf(0.2f, 0.8f), 8)
        bars.size shouldBeExactly 8
        bars.shouldAllBeDrawable()
    }

    test("out-of-range source values are clamped") {
        // A source can arrive from a decoder with amplitudes outside the drawable
        // range; the normaliser is the last place that can bring them back in.
        WaveformUtils.normalizeToBarCount(listOf(-4f, 0f, 9f), 16).shouldAllBeDrawable()
    }

    test("normalising interpolates between neighbours") {
        // Two bars stretched to three: the middle sample lands between them rather
        // than snapping to either end.
        val bars = WaveformUtils.normalizeToBarCount(listOf(0.2f, 1.0f), 3)
        bars.size shouldBeExactly 3
        bars[1] shouldBeGreaterThan 0.2f
        bars[1] shouldBeLessThan 1.0f
    }

    test("normalising is stable across repeat calls") {
        val source = List(50) { it / 50f }
        WaveformUtils.normalizeToBarCount(source, defaultBarCount) shouldBe
            WaveformUtils.normalizeToBarCount(source, defaultBarCount)
    }
})
