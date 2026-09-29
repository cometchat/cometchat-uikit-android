package com.cometchat.uikit.compose.presentation.imageviewer

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.imageviewer.style.CometChatImageViewerStyle
import com.cometchat.uikit.compose.presentation.imageviewer.ui.CometChatVideoViewerScreen
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Property (prop-matrix) layer for [CometChatVideoViewerScreen] — the pager overload, whose
 * eight params are the whole integrator surface (it takes no `modifier`, so [Denominator]
 * excludes nothing and the denominator is eight). Nothing is waived.
 *
 * Like its image twin ([CometChatImageViewerScreenComposePropMatrixTest]) this screen has no
 * View counterpart with a setter surface to mirror — the Kotlin video viewer is an Activity
 * configured through `createIntent`, covered by
 * [com.cometchat.uikit.kotlin.presentation.shared.mediaviewer.CometChatVideoViewerActivityFunctionalTest].
 *
 * Two things make this harder to observe than the image viewer, and both shape the entries:
 *
 * A `VideoView` under Robolectric never prepares, so the page's loading spinner runs forever.
 * An indefinite animation means the test clock is never idle, so the clock is taken off
 * auto-advance and driven by hand — otherwise every `waitForIdle` here would block
 * until the suite timed out rather than fail.
 *
 * And no video decodes, so one page is indistinguishable from another on screen. `fileNames`,
 * `mimeTypes` and `initialPage` therefore assert on what a toolbar tap hands back — the
 * *current page's* url, name and mime — which is the only observable that separates page 1
 * from page 0. `mimeTypes` gets a structural assertion as well: a non-video mime routes that
 * page to the "No preview available" screen, so the fixture carries a PDF alongside two videos.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatVideoViewerScreenComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatVideoViewerScreen"

        const val BACK = "Back"
        const val DOWNLOAD = "Download"
        const val SHARE = "Share"
        const val NO_PREVIEW = "No preview available"

        val URLS = listOf(
            "https://example.invalid/first.mp4",
            "https://example.invalid/second.mp4",
            "https://example.invalid/third.pdf",
        )
        val NAMES = listOf("first.mp4", "second.mp4", "third.pdf")
        val MIMES = listOf("video/mp4", "video/mp4", "application/pdf")

        const val MAGENTA_ARGB: Int = 0xFFFF00FF.toInt()
        val MAGENTA = Color(0xFFFF00FF)
    }

    /** See the class doc: the page's spinner never stops, so the clock is driven by hand. */
    @Before
    fun stopTheClock() {
        composeRule.mainClock.autoAdvance = false
    }

    /**
     * Drives the frozen clock far enough for a *rebuilt* screen to be composed, laid out and
     * animated in. A single frame is not enough on either count: the toolbar arrives through
     * an [androidx.compose.animation.AnimatedVisibility], and a screen re-entered after an
     * empty list (or re-keyed on a new starting page) measures as 0×0 for a pass or two
     * before the pager reports its real bounds.
     */
    private fun settle() {
        repeat(3) {
            composeRule.mainClock.advanceTimeBy(300L)
            composeRule.waitForIdle()
        }
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
        settle()
    }

    private fun textCount(text: String) =
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().size

    private fun cdCount(description: String) =
        composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().size

    @Test
    fun videoViewerScreen_propMatrix_coversEveryObservableProp() {
        var urls by mutableStateOf(URLS)
        var names by mutableStateOf(NAMES)
        var mimes by mutableStateOf(MIMES)
        var initialPage by mutableStateOf(0)
        var styleOverride by mutableStateOf<CometChatImageViewerStyle?>(null)

        var backPressed = false
        var downloaded: Triple<String, String, String>? = null
        var shared: Triple<String, String, String>? = null

        // `default()` is @Composable — it reads CometChatTheme — so it can only be evaluated
        // inside the composition. Captured for the style entry to copy() from.
        var defaultStyle: CometChatImageViewerStyle? = null

        composeRule.setContent {
            CometChatTheme {
                defaultStyle = CometChatImageViewerStyle.default()
                // Keyed on initialPage: the pager reads it only at construction, so a new
                // starting page needs a new screen rather than a recomposition.
                key(initialPage) {
                    CometChatVideoViewerScreen(
                        videoUrls = urls,
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
        settle()

        val matrix = composePropMatrix(OWNER) {
            value("videoUrls") {
                // The list's presence gates the whole screen: an empty one returns before
                // anything is composed, toolbar included.
                composeRule.onNodeWithContentDescription(BACK).assertIsDisplayed()
                urls = emptyList()
                settle()
                assertEquals(
                    "an empty url list should render no viewer at all",
                    0,
                    cdCount(BACK) + textCount(NO_PREVIEW),
                )
                urls = URLS
                settle()
                tap(SHARE)
                assertEquals("page 0's url travels with the share", URLS[0], shared?.first)
            }

            value("fileNames") {
                // No file name is drawn anywhere — the callbacks are where this list lands.
                tap(DOWNLOAD)
                assertEquals("page 0's name travels with the download", NAMES[0], downloaded?.second)
                names = listOf("renamed.mp4", "second.mp4", "third.pdf")
                settle()
                tap(DOWNLOAD)
                assertEquals(
                    "a changed name should reach the next callback",
                    "renamed.mp4",
                    downloaded?.second,
                )
                names = NAMES
                settle()
            }

            value("mimeTypes") {
                // Structural half: a non-video mime routes that page to the no-preview
                // screen instead of a player, which is the only page whose content renders
                // without a decoder.
                initialPage = 2
                settle()
                composeRule.onNodeWithText(NO_PREVIEW).assertIsDisplayed()

                // And the value half: the mime travels out with the callback.
                tap(SHARE)
                assertEquals(MIMES[2], shared?.third)

                initialPage = 0
                settle()
                assertEquals(
                    "a video page should not show the no-preview screen",
                    0,
                    textCount(NO_PREVIEW),
                )
            }

            value("initialPage") {
                // Nothing distinguishes one video page from another on screen, so the
                // starting page is measured by whose data the toolbar acts on.
                initialPage = 1
                settle()
                tap(SHARE)
                assertEquals("the viewer should open on page 1", URLS[1], shared?.first)
                assertEquals(NAMES[1], shared?.second)
                initialPage = 0
                settle()
            }

            value("style") {
                // The player draws nothing here, so the screen's own background is what is
                // on the glass — which is exactly the field this asserts.
                styleOverride = defaultStyle!!.copy(backgroundColor = MAGENTA)
                settle()
                assertTrue(
                    "the supplied backgroundColor should be on screen",
                    MAGENTA_ARGB in paintedColours(),
                )
                styleOverride = null
                settle()
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

        println("  [video viewer prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [video viewer] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("the whole parameter list", 8, cov.total)
        assertEquals("the video viewer has nothing worth waiving", 0, cov.waived)
    }
}
