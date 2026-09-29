package com.cometchat.uikit.compose.presentation.imageviewer.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
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
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
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
 * Property (prop-matrix) layer for the internal [CometChatImagePreview] — the zoom/pan/dismiss
 * surface behind the Compose image viewer, which had no test of its own.
 *
 * Eight params, `modifier` excluded by [Denominator], leaving seven; one is waived (see
 * [waivedProps]).
 *
 * The preview keeps its scale, offset and drag distance in internal state and puts none of it
 * into semantics, so the entries assert through the callbacks the host is given — which is
 * also the entire contract this component has with the screen that owns it. One slow, short
 * drag exercises the three drag callbacks together, because they are three stages of one
 * gesture and a fixture that separated them would be asserting something no user can do.
 *
 * `imageUrl` is read off the image loader rather than the screen: nothing decodes without a
 * real request, so the assertion is that the request the component made carries the url it
 * was handed.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatImagePreviewComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatImagePreview"
        const val URL = "https://example.invalid/holiday.jpg"

        /** w400dp-h800dp at xxhdpi — the drag threshold is half of this. */
        const val SCREEN_HEIGHT_PX = 2400f
    }

    /** Every url the component actually asked the loader for. */
    private val requested = mutableListOf<String>()

    @Before
    fun fakeTheImageLoader() {
        requested.clear()
        val interceptor = object : Interceptor {
            override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
                requested += chain.request.data.toString()
                val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
                return SuccessResult(
                    drawable = BitmapDrawable(RuntimeEnvironment.getApplication().resources, bitmap),
                    request = chain.request,
                    dataSource = DataSource.MEMORY,
                )
            }
        }
        Coil.setImageLoader(
            ImageLoader.Builder(RuntimeEnvironment.getApplication())
                .components { add(interceptor) }
                .crossfade(false)
                .build(),
        )
    }

    private fun image() =
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.cometchat_a11y_image_preview),
        )

    /**
     * A deliberate, unhurried vertical drag. The distance decides what happens on release —
     * past half the screen it dismisses — and the pauses keep the velocity under the fling
     * threshold so the *distance* is what is being asserted, not the flick.
     */
    private fun dragDown(distance: Float) {
        image().performTouchInput {
            down(Offset(width / 2f, 40f))
            var moved = 0f
            while (moved < distance) {
                val step = minOf(120f, distance - moved)
                moveBy(Offset(0f, step), delayMillis = 120L)
                moved += step
            }
            up()
        }
        composeRule.waitForIdle()
    }

    @Test
    fun imagePreview_propMatrix_coversEveryObservableProp() {
        var dragStarted = 0
        var dragEnded = 0
        var dismissed = 0
        val progress = mutableListOf<Float>()
        val loadingStates = mutableListOf<Boolean>()

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
                    onLoadingStateChange = { loadingStates += it },
                )
            }
        }
        composeRule.waitForIdle()

        val matrix = composePropMatrix(OWNER) {
            value("imageUrl") {
                image().assertIsDisplayed()
                assertEquals(
                    "the component should ask the loader for the url it was given",
                    listOf(URL),
                    requested.distinct(),
                )
            }

            callback("onLoadingStateChange") {
                assertTrue(
                    "the host is told when loading starts and stops",
                    loadingStates.isNotEmpty(),
                )
                assertEquals(
                    "a decoded image leaves the host not loading",
                    false,
                    loadingStates.last(),
                )
            }

            callback("onDragStart") {
                progress.clear()
                dragStarted = 0
                dragEnded = 0
                // Well under half the screen: this one restores rather than dismisses.
                dragDown(300f)
                assertEquals("a vertical drag should announce itself once", 1, dragStarted)
            }

            callback("onDragProgress") {
                // From the same drag: the fraction is of the screen height, and it grows.
                assertTrue("the drag should have reported progress", progress.isNotEmpty())
                assertTrue(
                    "progress should be a fraction of the screen, not a raw distance: $progress",
                    progress.all { it in 0f..1f },
                )
                // The trailing value is the restore's reset to zero — that half is asserted
                // by the onDragEnd entry, so growth is measured against the peak.
                assertTrue(
                    "progress should grow as the finger travels: $progress",
                    progress.max() > progress.first(),
                )
            }

            callback("onDragEnd") {
                assertEquals("releasing short of the threshold should end the drag", 1, dragEnded)
                assertEquals("and must not dismiss", 0, dismissed)
                assertEquals(
                    "the restore should reset the host's progress to zero",
                    0f,
                    progress.last(),
                    0f,
                )
            }

            callback("onDismiss") {
                dismissed = 0
                // Past half the screen height, which is where the threshold sits.
                dragDown(SCREEN_HEIGHT_PX * 0.6f)
                assertEquals("a drag past the threshold should dismiss", 1, dismissed)
            }
        }

        val props = matrix.evaluate() + waivedProps()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [image preview prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [image preview] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("the parameter list, minus modifier", 7, cov.total + cov.waived)
    }

    /**
     * `style` is a **dead parameter**. It is declared, documented ("Style configuration for
     * visual properties") and required — the screen builds one and hands it over on every
     * composition — and the body never reads it. The import is there; the use is not. Nothing
     * on screen can therefore be asserted to have come from it.
     *
     * It is worth noting rather than deleting quietly: the parameter is `internal`, so the
     * cost today is only a misleading signature, but the viewer's background, loading colour
     * and toolbar tints all live on that style, and a reader of this signature would
     * reasonably assume the preview honours them.
     */
    private fun waivedProps(): List<Prop> = listOf(
        Prop(OWNER, "style", PropKind.STYLE, waived = true),
    )
}
