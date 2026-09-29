package com.cometchat.uikit.compose.presentation.imageviewer

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.imageviewer.style.CometChatImageViewerStyle
import com.cometchat.uikit.compose.presentation.imageviewer.ui.CometChatImageViewerScreen
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Property (prop-matrix) layer for [CometChatImageViewerScreen] — the last of the
 * satellite matrices, and the one with no View twin to mirror: the Kotlin image viewer is
 * an Activity configured through `createIntent`, not a component with a prop surface.
 *
 * Nine integrator params, `modifier` excluded by [Denominator], leaving eight, and
 * nothing waived.
 *
 * Three of them — `fileNames`, `mimeTypes` and `initialPage` — have no rendering of their
 * own to assert against. Nothing on screen shows a file name, and off-network no image
 * ever decodes, so the pager's pages are indistinguishable from each other visually. What
 * they do reach is the download and share callbacks, which are handed the *current page's*
 * url, name and mime. Those entries therefore assert on what comes back out of a tap:
 * `initialPage` is measured by which page's data arrives, which is the only observable
 * that distinguishes page 1 from page 0 here.
 *
 * `mimeTypes` gets a second, structural assertion as well. A non-image mime routes that
 * page to the "No preview available" screen instead of the zoomable image, so the fixture
 * carries one PDF alongside two images — that page is the only one whose content is
 * visible without a network.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatImageViewerScreenComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatImageViewerScreen"

        const val BACK = "Back"
        const val DOWNLOAD = "Download"
        const val SHARE = "Share"
        const val NO_PREVIEW = "No preview available"

        val URLS = listOf(
            "https://example.invalid/first.jpg",
            "https://example.invalid/second.jpg",
            "https://example.invalid/third.pdf",
        )
        val NAMES = listOf("first.jpg", "second.jpg", "third.pdf")
        val MIMES = listOf("image/jpeg", "image/jpeg", "application/pdf")

        const val MAGENTA_ARGB: Int = 0xFFFF00FF.toInt()
        val MAGENTA = Color(0xFFFF00FF)
    }

    /** See the bubble matrices: `captureToImage()` has no window here. */
    private fun paintedColours(): Set<Int> {
        val view = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(
            view.width.coerceAtLeast(1),
            view.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        view.draw(Canvas(bitmap))
        val seen = HashSet<Int>()
        for (y in 0 until bitmap.height step 2) {
            for (x in 0 until bitmap.width step 2) seen += bitmap.getPixel(x, y)
        }
        return seen
    }

    private fun tap(description: String) {
        composeRule.onNodeWithContentDescription(description).performClick()
        composeRule.waitForIdle()
    }

    private fun textCount(text: String) =
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun imageViewerScreen_propMatrix_coversEveryObservableProp() {
        var urls by mutableStateOf(URLS)
        var names by mutableStateOf(NAMES)
        var mimes by mutableStateOf(MIMES)
        var initialPage by mutableStateOf(0)
        var styleOverride by mutableStateOf<CometChatImageViewerStyle?>(null)

        var backPressed = false
        var downloaded: Triple<String, String, String>? = null
        var shared: Triple<String, String, String>? = null

        // `default()` is @Composable — it reads CometChatTheme — so it can only be
        // evaluated inside the composition. Captured for the style entry to copy() from.
        var defaultStyle: CometChatImageViewerStyle? = null

        composeRule.setContent {
            CometChatTheme {
                defaultStyle = CometChatImageViewerStyle.default()
                // Keyed on initialPage: the pager reads it only at construction, so a new
                // starting page needs a new screen rather than a recomposition.
                androidx.compose.runtime.key(initialPage) {
                    CometChatImageViewerScreen(
                        imageUrls = urls,
                        fileNames = names,
                        mimeTypes = mimes,
                        initialPage = initialPage,
                        style = styleOverride ?: defaultStyle!!,
                        onBack = { backPressed = true },
                        onDownload = { u, n, m -> downloaded = Triple(u, n, m) },
                        onShare = { u, n, m -> shared = Triple(u, n, m) },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val matrix = composePropMatrix(OWNER) {
            value("imageUrls") {
                // The list's presence is what gates the whole screen: an empty one returns
                // before anything, toolbar included.
                composeRule.onNodeWithContentDescription(BACK).assertIsDisplayed()
                urls = emptyList()
                composeRule.waitForIdle()
                assertEquals(
                    "an empty url list should render no viewer at all",
                    0,
                    composeRule.onAllNodesWithText(NO_PREVIEW).fetchSemanticsNodes().size +
                        composeRule.onAllNodesWithText(BACK).fetchSemanticsNodes().size,
                )
                urls = URLS
                composeRule.waitForIdle()
                tap(SHARE)
                assertEquals("page 0's url travels with the share", URLS[0], shared?.first)
            }

            value("fileNames") {
                // No file name is drawn anywhere — the callbacks are where this list lands.
                tap(DOWNLOAD)
                assertEquals("page 0's name travels with the download", NAMES[0], downloaded?.second)
                names = listOf("renamed.jpg", "second.jpg", "third.pdf")
                composeRule.waitForIdle()
                tap(DOWNLOAD)
                assertEquals(
                    "a changed name should reach the next callback",
                    "renamed.jpg",
                    downloaded?.second,
                )
                names = NAMES
                composeRule.waitForIdle()
            }

            value("mimeTypes") {
                // Structural half: a non-image mime routes that page to the no-preview
                // screen, which is the only page whose content renders without a network.
                initialPage = 2
                composeRule.waitForIdle()
                composeRule.onNodeWithText(NO_PREVIEW).assertIsDisplayed()

                // And the value half: the mime travels out with the callback.
                tap(SHARE)
                assertEquals(MIMES[2], shared?.third)

                initialPage = 0
                composeRule.waitForIdle()
                assertEquals(
                    "an image page should not show the no-preview screen",
                    0,
                    textCount(NO_PREVIEW),
                )
            }

            value("initialPage") {
                // Nothing distinguishes one image page from another on screen, so the
                // starting page is measured by whose data the toolbar acts on.
                initialPage = 1
                composeRule.waitForIdle()
                tap(SHARE)
                assertEquals("the viewer should open on page 1", URLS[1], shared?.first)
                assertEquals(NAMES[1], shared?.second)
                initialPage = 0
                composeRule.waitForIdle()
            }

            value("style") {
                styleOverride = defaultStyle!!.copy(backgroundColor = MAGENTA)
                composeRule.waitForIdle()
                assertTrue(
                    "the supplied backgroundColor should be on screen",
                    MAGENTA_ARGB in paintedColours(),
                )
                styleOverride = null
                composeRule.waitForIdle()
            }

            callback("onBack") {
                tap(BACK)
                assertTrue("the back affordance should reach the integrator", backPressed)
            }

            callback("onDownload") {
                downloaded = null
                tap(DOWNLOAD)
                assertEquals(
                    "download hands back the current page's url, name and mime",
                    Triple(URLS[0], NAMES[0], MIMES[0]),
                    downloaded,
                )
            }

            callback("onShare") {
                shared = null
                tap(SHARE)
                assertEquals(Triple(URLS[0], NAMES[0], MIMES[0]), shared)
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [image viewer prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [image viewer] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("the image viewer has nothing worth waiving", 0, cov.waived)
    }
}
