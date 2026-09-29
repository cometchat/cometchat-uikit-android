package com.cometchat.uikit.compose.presentation.imageviewer.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit layer for [ImageViewerUtils].
 *
 * Every decision the Compose image viewer makes about a gesture is delegated here: whether
 * a drag has gone far enough to dismiss, whether a fling was hard enough, how far a
 * double-tap zooms, how much the backdrop fades. The composable itself only applies the
 * answers through `Modifier.graphicsLayer`, which is invisible to the semantics tree — so
 * this object is where the behaviour is actually assertable, and it had no tests at all.
 *
 * The tests lean on the boundaries, because that is where these particular functions can
 * be wrong without looking wrong. `shouldDismiss` and `shouldFlingDismiss` are both
 * strict `>` comparisons, so a drag of exactly half the screen and a fling of exactly
 * 1500 both **fail** to dismiss; `calcDoubleTapTargetScale` uses `<=`, so a tap at
 * exactly minimum scale zooms *in* rather than out. Each of those is one character away
 * from the opposite behaviour.
 */
class ImageViewerUtilsTest {

    private companion object {
        const val SCREEN_HEIGHT = 2000f
        const val FLING_THRESHOLD = 1500f
        const val EPS = 0.0001f
    }

    // ── calcScaleRange ──────────────────────────────────────────────────────

    @Test
    fun aTallImageInAWideContainerIsFittedByHeight() {
        // containerRatio (0.5) <= imageRatio (2.0), so height is the limiting dimension.
        val (min, max) = ImageViewerUtils.calcScaleRange(1000f, 500f, 100f, 200f)
        assertEquals(500f / 200f, min, EPS)
        assertEquals(min * 5f, max, EPS)
    }

    @Test
    fun aWideImageInATallContainerIsFittedByWidth() {
        // containerRatio (2.0) > imageRatio (0.5), so width is the limiting dimension.
        val (min, max) = ImageViewerUtils.calcScaleRange(500f, 1000f, 200f, 100f)
        assertEquals(500f / 200f, min, EPS)
        assertEquals(min * 5f, max, EPS)
    }

    @Test
    fun theMaximumIsAlwaysFiveTimesTheMinimum() {
        val (min, max) = ImageViewerUtils.calcScaleRange(1080f, 1920f, 640f, 480f)
        assertEquals(min * 5f, max, EPS)
    }

    @Test
    fun aZeroSizedContainerFallsBackRatherThanDividingByZero() {
        // The viewer measures before the image resolves, so this is the ordinary first
        // call rather than a defensive branch nobody hits.
        assertEquals(1f to 5f, ImageViewerUtils.calcScaleRange(0f, 1000f, 100f, 100f))
        assertEquals(1f to 5f, ImageViewerUtils.calcScaleRange(1000f, 0f, 100f, 100f))
    }

    @Test
    fun aZeroSizedImageFallsBackToo() {
        assertEquals(1f to 5f, ImageViewerUtils.calcScaleRange(1000f, 1000f, 0f, 100f))
        assertEquals(1f to 5f, ImageViewerUtils.calcScaleRange(1000f, 1000f, 100f, 0f))
    }

    @Test
    fun negativeDimensionsFallBackRatherThanInvertingTheScale() {
        assertEquals(1f to 5f, ImageViewerUtils.calcScaleRange(-100f, 1000f, 100f, 100f))
        assertEquals(1f to 5f, ImageViewerUtils.calcScaleRange(1000f, 1000f, 100f, -100f))
    }

    // ── constrainOffset ─────────────────────────────────────────────────────

    @Test
    fun anImageSmallerThanItsContainerCannotBePannedAtAll() {
        // Both bounds collapse to zero, so every offset clamps back to the centre.
        val clamped = ImageViewerUtils.constrainOffset(
            offset = Offset(500f, 500f),
            scale = 1f,
            imageSize = IntSize(100, 100),
            containerSize = IntSize(1000, 1000),
        )
        assertEquals(0f, clamped.x, EPS)
        assertEquals(0f, clamped.y, EPS)
    }

    @Test
    fun anOverscaledImagePansUpToHalfItsOverflow() {
        // 200*4 = 800 wide against a 400 container -> 400 of overflow -> 200 either way.
        val clamped = ImageViewerUtils.constrainOffset(
            offset = Offset(10_000f, 10_000f),
            scale = 4f,
            imageSize = IntSize(200, 200),
            containerSize = IntSize(400, 400),
        )
        assertEquals(200f, clamped.x, EPS)
        assertEquals(200f, clamped.y, EPS)
    }

    @Test
    fun theClampIsSymmetricAboutTheCentre() {
        val clamped = ImageViewerUtils.constrainOffset(
            offset = Offset(-10_000f, -10_000f),
            scale = 4f,
            imageSize = IntSize(200, 200),
            containerSize = IntSize(400, 400),
        )
        assertEquals(-200f, clamped.x, EPS)
        assertEquals(-200f, clamped.y, EPS)
    }

    @Test
    fun anOffsetAlreadyInsideTheBoundsIsLeftAlone() {
        val offset = Offset(50f, -25f)
        val clamped = ImageViewerUtils.constrainOffset(
            offset = offset,
            scale = 4f,
            imageSize = IntSize(200, 200),
            containerSize = IntSize(400, 400),
        )
        assertEquals(offset.x, clamped.x, EPS)
        assertEquals(offset.y, clamped.y, EPS)
    }

    @Test
    fun eachAxisIsClampedIndependently() {
        // A wide-but-short overscale can pan horizontally while being pinned vertically.
        val clamped = ImageViewerUtils.constrainOffset(
            offset = Offset(10_000f, 10_000f),
            scale = 2f,
            imageSize = IntSize(400, 100),
            containerSize = IntSize(400, 400),
        )
        assertEquals("800 wide against 400 leaves 200 either way", 200f, clamped.x, EPS)
        assertEquals("200 tall against 400 leaves nothing", 0f, clamped.y, EPS)
    }

    // ── calcBackgroundAlpha ─────────────────────────────────────────────────

    @Test
    fun theBackdropIsFullyOpaqueBeforeAnyDrag() {
        assertEquals(1f, ImageViewerUtils.calcBackgroundAlpha(0f, SCREEN_HEIGHT), EPS)
    }

    @Test
    fun theBackdropFadesInProportionToTheDrag() {
        assertEquals(0.75f, ImageViewerUtils.calcBackgroundAlpha(500f, SCREEN_HEIGHT), EPS)
        assertEquals(0.5f, ImageViewerUtils.calcBackgroundAlpha(1000f, SCREEN_HEIGHT), EPS)
    }

    @Test
    fun draggingUpFadesTheBackdropAsMuchAsDraggingDown() {
        assertEquals(
            ImageViewerUtils.calcBackgroundAlpha(500f, SCREEN_HEIGHT),
            ImageViewerUtils.calcBackgroundAlpha(-500f, SCREEN_HEIGHT),
            EPS,
        )
    }

    @Test
    fun theBackdropNeverGoesPastFullyTransparent() {
        // Overscroll beyond the screen height must not produce a negative alpha, which
        // would throw when applied.
        assertEquals(0f, ImageViewerUtils.calcBackgroundAlpha(SCREEN_HEIGHT * 3, SCREEN_HEIGHT), EPS)
    }

    @Test
    fun anUnmeasuredScreenLeavesTheBackdropOpaque() {
        assertEquals(1f, ImageViewerUtils.calcBackgroundAlpha(500f, 0f), EPS)
    }

    // ── shouldDismiss ───────────────────────────────────────────────────────

    @Test
    fun aDragPastHalfTheScreenDismisses() {
        assertTrue(ImageViewerUtils.shouldDismiss(SCREEN_HEIGHT * 0.51f, SCREEN_HEIGHT))
    }

    @Test
    fun aDragOfExactlyHalfTheScreenDoesNotDismiss() {
        // Strictly greater than: exactly half restores. One character from the opposite
        // behaviour, and the difference a user feels at the midpoint.
        assertFalse(ImageViewerUtils.shouldDismiss(SCREEN_HEIGHT * 0.5f, SCREEN_HEIGHT))
    }

    @Test
    fun aShortDragRestores() {
        assertFalse(ImageViewerUtils.shouldDismiss(100f, SCREEN_HEIGHT))
    }

    @Test
    fun draggingUpwardsDismissesJustAsFar() {
        assertTrue(ImageViewerUtils.shouldDismiss(-SCREEN_HEIGHT * 0.51f, SCREEN_HEIGHT))
    }

    @Test
    fun anUnmeasuredScreenNeverDismisses() {
        // Guards the divide; without it any drag would dismiss on the first frame.
        assertFalse(ImageViewerUtils.shouldDismiss(10_000f, 0f))
    }

    // ── shouldFlingDismiss ──────────────────────────────────────────────────

    @Test
    fun aHardFlingDismisses() {
        assertTrue(ImageViewerUtils.shouldFlingDismiss(FLING_THRESHOLD + 1f))
    }

    @Test
    fun aFlingOfExactlyTheThresholdDoesNotDismiss() {
        assertFalse(ImageViewerUtils.shouldFlingDismiss(FLING_THRESHOLD))
    }

    @Test
    fun aGentleFlingDoesNotDismiss() {
        assertFalse(ImageViewerUtils.shouldFlingDismiss(100f))
    }

    @Test
    fun flingingUpwardsDismissesJustAsReadily() {
        assertTrue(ImageViewerUtils.shouldFlingDismiss(-(FLING_THRESHOLD + 1f)))
    }

    // ── calcDoubleTapTargetScale ────────────────────────────────────────────

    @Test
    fun doubleTappingAtRestZoomsToTheMidpoint() {
        assertEquals(3f, ImageViewerUtils.calcDoubleTapTargetScale(1f, 1f, 5f), EPS)
    }

    @Test
    fun doubleTappingWhileZoomedReturnsToTheMinimum() {
        assertEquals(1f, ImageViewerUtils.calcDoubleTapTargetScale(3f, 1f, 5f), EPS)
    }

    @Test
    fun doubleTappingBelowTheMinimumZoomsInRatherThanOut() {
        // The comparison is `<=`, so a scale that has drifted under the minimum is treated
        // as "at rest" and zooms in.
        assertEquals(3f, ImageViewerUtils.calcDoubleTapTargetScale(0.5f, 1f, 5f), EPS)
    }

    @Test
    fun theMidpointIsRelativeToTheRangeNotAbsolute() {
        // A non-unit minimum — the common case, since minScale is a fit-to-screen factor.
        assertEquals(1.5f, ImageViewerUtils.calcDoubleTapTargetScale(0.5f, 0.5f, 2.5f), EPS)
    }

    @Test
    fun justAboveTheMinimumZoomsBackOut() {
        assertEquals(1f, ImageViewerUtils.calcDoubleTapTargetScale(1.0001f, 1f, 5f), EPS)
    }

    // ── isShareValid ────────────────────────────────────────────────────────

    @Test
    fun aCompleteShareIsValid() {
        assertTrue(ImageViewerUtils.isShareValid("https://x/y.jpg", "y.jpg", "image/jpeg"))
    }

    @Test
    fun everyFieldIsRequired() {
        assertFalse(ImageViewerUtils.isShareValid("", "y.jpg", "image/jpeg"))
        assertFalse(ImageViewerUtils.isShareValid("https://x/y.jpg", "", "image/jpeg"))
        assertFalse(ImageViewerUtils.isShareValid("https://x/y.jpg", "y.jpg", ""))
    }

    @Test
    fun blanksAreNotTreatedAsEmpty() {
        // isNotEmpty, not isNotBlank — a whitespace file name passes. Pinned as it is, so
        // that tightening it later is a deliberate change rather than a surprise.
        assertTrue(ImageViewerUtils.isShareValid("https://x/y.jpg", " ", "image/jpeg"))
    }
}
