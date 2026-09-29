package com.cometchat.uikit.kotlin.presentation.shared.mediaviewer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.utils.RoborazziConfig
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/**
 * Roborazzi baselines for [CometChatImagePreview] — the first captures this component has had.
 *
 * There is exactly one thing about this class that is purely visual and worth pinning in
 * pixels: how it fits an image into the viewer. On layout it computes a scale range from the
 * canvas and bitmap ratios and centres the bitmap in the result, and that branch goes two
 * ways — a wide image is fitted by width and letterboxed, a tall one is fitted by height and
 * pillarboxed. Arithmetic that decides *where an image sits on screen* is exactly what a
 * baseline catches and an assertion tends to paraphrase, so the three shapes are captured
 * against the same container.
 *
 * The rest of the component is gesture physics, which these do not attempt: a zoomed or
 * mid-drag capture would be pinning the gesture detectors and animators rather than this
 * class, and [CometChatImagePreviewFunctionalTest] says the same thing about driving them.
 *
 * The images are drawn rather than loaded — four quadrants and a border — so the fit, the
 * centring and any unexpected crop are all readable at a glance, and nothing depends on a
 * file or the network. The black background is the component's own: it blackens the host
 * window at construction.
 *
 * Run:
 *   ./gradlew :chatuikit-kotlin:recordRoborazziDebug --tests "*.CometChatImagePreviewScreenshotTest"
 *   ./gradlew :chatuikit-kotlin:verifyRoborazziDebug --tests "*.CometChatImagePreviewScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatImagePreviewScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/imagepreview",
        ),
    )

    private companion object {
        const val VIEW_WIDTH = 1080
        const val VIEW_HEIGHT = 1920
    }

    /** Four quadrants and a border, so fit, centring and crop are all readable. */
    private fun quadrants(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val halfWidth = width / 2f
        val halfHeight = height / 2f
        paint.color = Color.rgb(220, 60, 60)
        canvas.drawRect(0f, 0f, halfWidth, halfHeight, paint)
        paint.color = Color.rgb(60, 140, 220)
        canvas.drawRect(halfWidth, 0f, width.toFloat(), halfHeight, paint)
        paint.color = Color.rgb(240, 200, 60)
        canvas.drawRect(0f, halfHeight, halfWidth, height.toFloat(), paint)
        paint.color = Color.rgb(60, 180, 120)
        canvas.drawRect(halfWidth, halfHeight, width.toFloat(), height.toFloat(), paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        canvas.drawRect(4f, 4f, width - 4f, height - 4f, paint)
        return bitmap
    }

    /**
     * Builds the viewer the way the image viewer does — an ImageView filling a container, with
     * the preview attached — lays it out, and captures it. Laying out is what arms the
     * component: it fits and centres from `onLayoutChange`.
     */
    private fun captureFitted(image: Bitmap?, rtl: Boolean = false) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)

            val imageView = ImageView(activity).apply {
                if (image != null) {
                    setImageDrawable(BitmapDrawable(activity.resources, image))
                }
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
            val container = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                addView(imageView)
            }

            CometChatImagePreview.create(imageView, container)
            activity.setContentView(container)
            // The root has to be attached before the direction is set; the ldrtl
            // qualifier does nothing here. See RtlLayoutDirectionHarnessTest.
            if (rtl) container.layoutDirection = View.LAYOUT_DIRECTION_RTL
            ShadowLooper.idleMainLooper()

            // Idle first, then lay out: the activity runs its own traversal off the
            // looper and sizes the container to the window. Idling after the manual
            // layout is a race, and the captured canvas flips size between runs.
            ShadowLooper.idleMainLooper()
            container.measure(
                View.MeasureSpec.makeMeasureSpec(VIEW_WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(VIEW_HEIGHT, View.MeasureSpec.EXACTLY),
            )
            container.layout(0, 0, VIEW_WIDTH, VIEW_HEIGHT)

            container.captureRoboImage(roborazziOptions = RoborazziConfig.options())
        }
        scenario.close()
    }

    /** Wider than the viewer: fitted by width, letterboxed above and below. */
    @Test
    fun fitWideImage() {
        captureFitted(quadrants(width = 1600, height = 600))
    }

    /** Taller than the viewer: fitted by height, pillarboxed left and right. */
    @Test
    fun fitTallImage() {
        captureFitted(quadrants(width = 600, height = 1600))
    }

    /** Square, against a portrait viewer: the width-fit branch, centred vertically. */
    @Test
    fun fitSquareImage() {
        captureFitted(quadrants(width = 900, height = 900))
    }

    /** Smaller than the viewer in both directions — it is scaled up to fit, not left small. */
    @Test
    fun fitSmallImage() {
        captureFitted(quadrants(width = 120, height = 90))
    }

    /** No drawable: `setupLayout` refuses to prepare, and the viewer stays its own black. */
    @Test
    fun noImageToFit() {
        captureFitted(image = null)
    }

    // ── right-to-left ───────────────────────────────────────────────────────

    /**
     * The fit is geometric, so this is expected to match its left-to-right twin; it is
     * recorded so that a future direction-aware change to the viewer shows up here.
     */
    @Test
    fun fitWideImage_rtl() {
        captureFitted(quadrants(width = 1600, height = 600), rtl = true)
    }
}
