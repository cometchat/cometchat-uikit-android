package com.cometchat.uikit.compose.presentation.imageviewer.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.request.SuccessResult
import com.cometchat.uikit.compose.R
import com.cometchat.uikit.compose.presentation.imageviewer.style.CometChatImageViewerStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Functional layer for the internal [CometChatImagePreview] — the rules its gestures follow,
 * as opposed to whether each param arrives, which is
 * [CometChatImagePreviewComposePropMatrixTest]'s job.
 *
 * Every rule here exists to keep three gestures out of each other's way inside one viewer that
 * also sits in a horizontal pager, and each is asserted through the only thing the component
 * exposes — the callbacks:
 *
 *  - a vertical drag dismisses **only at minimum zoom**; once zoomed in the same gesture pans
 *    the image, and the host is never told a dismiss drag started;
 *  - a double tap toggles between fit and zoomed, so dismissal comes back after the second tap;
 *  - a mostly-horizontal drag is left unconsumed, which is what lets the pager change images;
 *  - a movement under the slop threshold is not a drag at all;
 *  - a flick dismisses on velocity even when it never travels far enough to dismiss on distance.
 *
 * The zoom assertions are deliberately behavioural. Scale lives in internal state and reaches
 * neither semantics nor any callback, so "did the double tap zoom?" is answered by what the
 * next gesture does — which is also the only thing about the zoom a user can perceive.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatImagePreviewComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val URL = "https://example.invalid/holiday.jpg"

        /** w400dp-h800dp at xxhdpi. The dismiss threshold is half of this. */
        const val SCREEN_HEIGHT_PX = 2400f
    }

    private var dragStarted = 0
    private var dragEnded = 0
    private var dismissed = 0
    private val progress = mutableListOf<Float>()

    @Before
    fun fakeTheImageLoader() {
        val interceptor = object : Interceptor {
            override suspend fun intercept(chain: Interceptor.Chain): ImageResult = SuccessResult(
                drawable = BitmapDrawable(
                    RuntimeEnvironment.getApplication().resources,
                    Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888),
                ),
                request = chain.request,
                dataSource = DataSource.MEMORY,
            )
        }
        Coil.setImageLoader(
            ImageLoader.Builder(RuntimeEnvironment.getApplication())
                .components { add(interceptor) }
                .crossfade(false)
                .build(),
        )
    }

    private fun showPreview() {
        composeRule.setContent {
            CometChatTheme {
                CometChatImagePreview(
                    imageUrl = URL,
                    modifier = Modifier.fillMaxSize(),
                    style = CometChatImageViewerStyle.default(),
                    onDragStart = { dragStarted++ },
                    onDragEnd = { dragEnded++ },
                    onDismiss = { dismissed++ },
                    onDragProgress = { progress += it },
                    onLoadingStateChange = { },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun image() =
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.cometchat_a11y_image_preview),
        )

    /** An unhurried drag, so distance rather than velocity decides what happens on release. */
    private fun drag(dx: Float, dy: Float) {
        image().performTouchInput {
            down(Offset(width / 2f, 40f))
            val steps = 8
            repeat(steps) {
                moveBy(Offset(dx / steps, dy / steps), delayMillis = 120L)
            }
            up()
        }
        composeRule.waitForIdle()
    }

    private fun doubleTap() {
        image().performTouchInput { doubleClick() }
        composeRule.waitForIdle()
    }

    private fun reset() {
        dragStarted = 0
        dragEnded = 0
        dismissed = 0
        progress.clear()
    }

    @Test
    fun aVerticalDragAtFitScaleIsADismissGesture() {
        showPreview()
        reset()

        drag(dx = 0f, dy = 400f)

        assertEquals("the drag should be announced", 1, dragStarted)
        assertTrue("and should report progress", progress.any { it > 0f })
        assertEquals("short of the threshold it restores", 1, dragEnded)
        assertEquals(0, dismissed)
    }

    @Test
    fun zoomingInTakesTheDragAwayFromDismiss() {
        showPreview()
        doubleTap()
        reset()

        // The same gesture that dismissed a moment ago now pans the zoomed image instead.
        drag(dx = 0f, dy = 400f)

        assertEquals(
            "a drag on a zoomed image should pan it, not start a dismiss",
            0,
            dragStarted,
        )
        assertEquals(0, dismissed)
    }

    @Test
    fun doubleTappingBackToFitRestoresTheDismissGesture() {
        showPreview()
        doubleTap()
        doubleTap()
        reset()

        drag(dx = 0f, dy = 400f)

        assertEquals(
            "back at fit scale the drag should be a dismiss gesture again",
            1,
            dragStarted,
        )
    }

    @Test
    fun aMostlyHorizontalDragIsLeftForThePager() {
        showPreview()
        reset()

        // The viewer sits in a HorizontalPager; a sideways drag has to reach it unconsumed or
        // the user can never swipe to the next image.
        drag(dx = 600f, dy = 100f)

        assertEquals("a sideways drag is not a dismiss", 0, dragStarted)
        assertEquals(0, dismissed)
    }

    @Test
    fun aMovementUnderTheSlopThresholdIsNotADrag() {
        showPreview()
        reset()

        drag(dx = 0f, dy = 6f)

        assertEquals("a few pixels of travel should not start a drag", 0, dragStarted)
        assertTrue("and nothing should be reported to the host", progress.isEmpty())
    }

    @Test
    fun aDragPastHalfTheScreenDismisses() {
        showPreview()
        reset()

        drag(dx = 0f, dy = SCREEN_HEIGHT_PX * 0.6f)

        assertEquals("past the threshold the viewer closes", 1, dismissed)
        assertEquals("and it is a dismissal, not a restore", 0, dragEnded)
    }

    @Test
    fun aFlickDismissesOnVelocityWithoutTravellingTheDistance() {
        showPreview()
        reset()

        // A quarter of the dismiss distance, thrown fast enough to clear the fling threshold.
        image().performTouchInput {
            swipe(
                start = Offset(width / 2f, 40f),
                end = Offset(width / 2f, 40f + SCREEN_HEIGHT_PX * 0.25f),
                durationMillis = 60L,
            )
        }
        composeRule.waitForIdle()

        assertEquals("a flick should dismiss on its own", 1, dismissed)
        assertEquals(0, dragEnded)
    }
}
