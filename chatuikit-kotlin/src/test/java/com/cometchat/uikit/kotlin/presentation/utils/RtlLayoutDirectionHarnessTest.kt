package com.cometchat.uikit.kotlin.presentation.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/**
 * How to write a right-to-left screenshot test in the View toolkit — pinned, because
 * the obvious way does not work and the reason is not discoverable from a failure.
 *
 * Nothing in the kit exercised layout direction before this. The first attempt reached
 * for `@Config(qualifiers = "ldrtl-…")`, recorded baselines that were byte-identical to
 * their left-to-right twins, and looked like a Robolectric limitation. It is not:
 *
 * - The `ldrtl` **qualifier is inert** here. It parses — and it has to precede the
 *   width qualifiers or Robolectric refuses to parse it at all — but it leaves
 *   `Configuration.layoutDirection` at 0, so nothing mirrors and resource-qualified
 *   `layout-ldrtl/` variants never activate. [theLdrtlQualifierDoesNotFlipTheConfiguration]
 *   pins that, so the next person does not spend the afternoon I did on it.
 * - Setting **`layoutDirection` on the attached root does** mirror, correctly and
 *   paintably. [settingTheRootLayoutDirectionMirrors] is the control, and the technique
 *   any View RTL baseline should use.
 *
 * One open question this does not assert. With direction resolved on it, the bubble
 * container still renders identically, because it selects a whole layout file per
 * alignment — `cometchat_message_bubble_left`, `_right`, `_center` — so the side is
 * baked in rather than derived from direction. Whether an incoming bubble should move
 * to the right in an RTL locale is a product question worth checking on a device, and
 * is deliberately left un-pinned here rather than blessed as correct.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class RtlLayoutDirectionHarnessTest {

    private companion object {
        const val WIDTH = 1000
        const val HEIGHT = 200
        const val MARKER = 100
    }

    /**
     * Lays out a marker pinned to `ALIGN_PARENT_END` and reports its left edge, plus
     * where it actually painted — a resolved-but-undrawn mirror would be a trap of its
     * own, so both are measured.
     */
    private fun markerPosition(setRootDirectionRtl: Boolean): Pair<Int, Int> {
        var laidOutAt = -1
        var paintedAt = -1
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            val root = RelativeLayout(activity)
            activity.setContentView(root)
            // Order matters: the root has to be attached before the direction is set.
            if (setRootDirectionRtl) root.layoutDirection = View.LAYOUT_DIRECTION_RTL

            val marker = View(activity).apply { setBackgroundColor(Color.RED) }
            root.addView(
                marker,
                RelativeLayout.LayoutParams(MARKER, MARKER).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                },
            )
            ShadowLooper.idleMainLooper()

            root.measure(
                View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY),
            )
            root.layout(0, 0, WIDTH, HEIGHT)
            ShadowLooper.idleMainLooper()

            laidOutAt = marker.left

            val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            // Sample inside the marker's own band, not the middle of the root: the
            // marker is MARKER tall from the top, so HEIGHT / 2 would miss it.
            for (x in 0 until WIDTH step 5) {
                if (bitmap.getPixel(x, MARKER / 2) == Color.RED) {
                    paintedAt = x
                    break
                }
            }
        }
        scenario.close()
        return laidOutAt to paintedAt
    }

    @Test
    fun settingTheRootLayoutDirectionMirrors() {
        val (ltrLaidOut, ltrPainted) = markerPosition(setRootDirectionRtl = false)
        val (rtlLaidOut, rtlPainted) = markerPosition(setRootDirectionRtl = true)

        assertEquals("end should be the right edge in a left-to-right layout", WIDTH - MARKER, ltrLaidOut)
        assertEquals("and it should paint there", WIDTH - MARKER, ltrPainted)

        assertEquals("end should be the left edge once the root is right-to-left", 0, rtlLaidOut)
        assertEquals("and it should paint there too", 0, rtlPainted)

        assertNotEquals("the two directions must not render the same", ltrPainted, rtlPainted)
    }

    /**
     * The trap. A baseline recorded under this qualifier alone is a second copy of the
     * left-to-right one, and passes forever without asserting anything about direction.
     */
    @Config(qualifiers = "ldrtl-w400dp-h800dp-xxhdpi")
    @Test
    fun theLdrtlQualifierDoesNotFlipTheConfiguration() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            assertEquals(
                "Robolectric parses ldrtl but leaves the configuration left-to-right; " +
                    "if this ever starts failing, the qualifier became usable and this " +
                    "test and its KDoc should say so",
                View.LAYOUT_DIRECTION_LTR,
                activity.resources.configuration.layoutDirection,
            )
        }
        scenario.close()

        val (laidOut, _) = markerPosition(setRootDirectionRtl = false)
        assertEquals("so nothing mirrors from the qualifier alone", WIDTH - MARKER, laidOut)
    }
}
