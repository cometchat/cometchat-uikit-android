package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.audiobubble

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.core.utils.WaveformUtils
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Instrumented layer for [AudioWaveformBarsView], the custom-drawn half
 * of the View audio bubble.
 *
 * A custom `View` whose entire output is `onDraw` gets nothing from a setter test:
 * every colour setter would pass while painting nothing. So these drive real pixels —
 * measure, lay out, draw onto a bitmap, and read the result back — which is the only
 * way to tell "the colour was stored" from "the colour was painted".
 *
 * The seek path is the other half. `onTouchEvent` consumes *all* events once a seek
 * listener is attached (so a drag across the waveform never scrolls the message list
 * underneath) but only reports on `ACTION_UP`. Both halves of that rule are pinned:
 * a bubble that reported on every move would seek continuously while the finger
 * travelled.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class AudioWaveformBarsViewTest {

    private companion object {
        const val WIDTH = 600
        const val HEIGHT = 96
        const val PLAYED = 0xFFFF0000.toInt()
        const val UNPLAYED = 0xFF0000FF.toInt()
        const val BAR_COUNT = 28
    }

    private fun withView(block: (AudioWaveformBarsView) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(AudioWaveformBarsView(activity))
        }
        scenario.close()
    }

    /** Lay the view out at a fixed size and paint it onto a bitmap we can inspect. */
    private fun AudioWaveformBarsView.render(): Bitmap {
        measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY),
        )
        layout(0, 0, WIDTH, HEIGHT)
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        return bitmap
    }

    private fun Bitmap.countOf(color: Int): Int {
        var n = 0
        for (y in 0 until height) for (x in 0 until width) if (getPixel(x, y) == color) n++
        return n
    }

    private fun touch(view: View, action: Int, x: Float) {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, x, HEIGHT / 2f, 0)
        view.onTouchEvent(event)
        event.recycle()
    }

    // ── drawing ─────────────────────────────────────────────────────────────

    @Test
    fun withNoProgress_everyBarIsPaintedUnplayed() = withView { view ->
        view.setPlayedWaveColor(PLAYED)
        view.setUnplayedWaveColor(UNPLAYED)
        view.setBarHeights(WaveformUtils.generateDeterministicWaveform("a", BAR_COUNT))

        val bitmap = view.render()
        assertTrue("the waveform should actually paint", bitmap.countOf(UNPLAYED) > 0)
        assertEquals("nothing is played yet", 0, bitmap.countOf(PLAYED))
    }

    @Test
    fun progressPaintsTheLeadingBarsInThePlayedColour() = withView { view ->
        view.setPlayedWaveColor(PLAYED)
        view.setUnplayedWaveColor(UNPLAYED)
        view.setBarHeights(WaveformUtils.generateDeterministicWaveform("a", BAR_COUNT))
        view.setProgress(0.5f)

        val bitmap = view.render()
        assertTrue(bitmap.countOf(PLAYED) > 0)
        assertTrue(bitmap.countOf(UNPLAYED) > 0)
    }

    @Test
    fun fullProgressLeavesNothingUnplayed() = withView { view ->
        view.setPlayedWaveColor(PLAYED)
        view.setUnplayedWaveColor(UNPLAYED)
        view.setBarHeights(WaveformUtils.generateDeterministicWaveform("a", BAR_COUNT))
        view.setProgress(1f)

        val bitmap = view.render()
        assertTrue(bitmap.countOf(PLAYED) > 0)
        assertEquals(0, bitmap.countOf(UNPLAYED))
    }

    @Test
    fun progressIsClampedRatherThanOverrunningTheBars() = withView { view ->
        view.setPlayedWaveColor(PLAYED)
        view.setUnplayedWaveColor(UNPLAYED)
        view.setBarHeights(WaveformUtils.generateDeterministicWaveform("a", BAR_COUNT))

        view.setProgress(4f)
        val over = view.render()
        view.setProgress(1f)
        val exact = view.render()
        assertEquals("progress above 1 must behave as 1", exact.countOf(PLAYED), over.countOf(PLAYED))

        view.setProgress(-3f)
        val under = view.render()
        assertEquals("progress below 0 must behave as 0", 0, under.countOf(PLAYED))
    }

    @Test
    fun aRecolouredWaveformRepaintsInTheNewColour() = withView { view ->
        // The setters call invalidate(); a colour that only reached the field would
        // leave the previous paint on screen.
        view.setUnplayedWaveColor(UNPLAYED)
        view.setBarHeights(WaveformUtils.generateDeterministicWaveform("a", BAR_COUNT))
        assertTrue(view.render().countOf(UNPLAYED) > 0)

        view.setUnplayedWaveColor(Color.GREEN)
        val recoloured = view.render()
        assertEquals(0, recoloured.countOf(UNPLAYED))
        assertTrue(recoloured.countOf(Color.GREEN) > 0)
    }

    @Test
    fun withNoBars_drawsNothingRatherThanThrowing() = withView { view ->
        view.setBarHeights(emptyList())
        view.setUnplayedWaveColor(UNPLAYED)
        assertEquals(0, view.render().countOf(UNPLAYED))
    }

    @Test
    fun aSingleBarIsDrawnWithoutDividingByZeroSpacing() = withView { view ->
        // spacingPx divides by (count - 1); one bar must not blow up.
        view.setBarHeights(listOf(0.8f))
        view.setUnplayedWaveColor(UNPLAYED)
        assertTrue(view.render().countOf(UNPLAYED) > 0)
    }

    @Test
    fun aFlatWaveformStillPaints() = withView { view ->
        // Contrast stretching divides by the amplitude range; an all-equal waveform
        // has a range of zero and relies on the 0.01 floor.
        view.setBarHeights(List(BAR_COUNT) { 0.5f })
        view.setUnplayedWaveColor(UNPLAYED)
        assertTrue(view.render().countOf(UNPLAYED) > 0)
    }

    @Test
    fun anUnlaidOutViewDrawsNothingRatherThanThrowing() = withView { view ->
        view.setBarHeights(WaveformUtils.generateDeterministicWaveform("a", BAR_COUNT))
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        assertNotNull(bitmap)
    }

    // ── seeking ─────────────────────────────────────────────────────────────

    @Test
    fun aTapReportsItsPositionAsAFraction() = withView { view ->
        var seeked: Float? = null
        view.setOnSeekListener { seeked = it }
        view.setBarHeights(WaveformUtils.generateDeterministicWaveform("a", BAR_COUNT))
        view.render()

        touch(view, MotionEvent.ACTION_UP, WIDTH / 4f)
        assertNotNull(seeked)
        assertEquals(0.25f, seeked!!, 0.01f)
    }

    @Test
    fun onlyTheReleaseReportsASeek() = withView { view ->
        // A drag must not fire on every move, or playback would chase the finger.
        var calls = 0
        view.setOnSeekListener { calls++ }
        view.setBarHeights(WaveformUtils.generateDeterministicWaveform("a", BAR_COUNT))
        view.render()

        touch(view, MotionEvent.ACTION_DOWN, 10f)
        touch(view, MotionEvent.ACTION_MOVE, 100f)
        touch(view, MotionEvent.ACTION_MOVE, 200f)
        assertEquals("nothing reported until the finger lifts", 0, calls)

        touch(view, MotionEvent.ACTION_UP, 200f)
        assertEquals(1, calls)
    }

    @Test
    fun aTapBeyondTheEdgesIsClampedIntoRange() = withView { view ->
        var seeked: Float? = null
        view.setOnSeekListener { seeked = it }
        view.setBarHeights(WaveformUtils.generateDeterministicWaveform("a", BAR_COUNT))
        view.render()

        touch(view, MotionEvent.ACTION_UP, -50f)
        assertEquals(0f, seeked!!, 0f)

        touch(view, MotionEvent.ACTION_UP, WIDTH * 3f)
        assertEquals(1f, seeked!!, 0f)
    }

    @Test
    fun seekingConsumesEveryEventSoTheListBelowDoesNotScroll() = withView { view ->
        view.setOnSeekListener { }
        view.setBarHeights(WaveformUtils.generateDeterministicWaveform("a", BAR_COUNT))
        view.render()

        val now = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 10f, HEIGHT / 2f, 0)
        assertTrue("a seekable waveform must own the gesture", view.onTouchEvent(down))
        down.recycle()
    }

    @Test
    fun withNoSeekListener_theViewDoesNotClaimTheGesture() = withView { view ->
        // No listener means the message list should get the drag instead.
        view.setBarHeights(WaveformUtils.generateDeterministicWaveform("a", BAR_COUNT))
        view.render()

        val now = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 10f, HEIGHT / 2f, 0)
        assertFalse(view.onTouchEvent(down))
        down.recycle()
    }

    @Test
    fun theSeekListenerCanBeDetachedAgain() = withView { view ->
        var calls = 0
        view.setOnSeekListener { calls++ }
        view.setBarHeights(WaveformUtils.generateDeterministicWaveform("a", BAR_COUNT))
        view.render()
        touch(view, MotionEvent.ACTION_UP, 100f)
        assertEquals(1, calls)

        view.setOnSeekListener(null)
        touch(view, MotionEvent.ACTION_UP, 100f)
        assertEquals("a detached listener must stop hearing taps", 1, calls)
    }
}
