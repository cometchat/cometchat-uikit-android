package com.cometchat.uikit.compose.presentation.messagecomposer.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.messagecomposer.style.CometChatMessageComposerStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.formatter.RichTextFormat
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Property (prop-matrix) layer for [CometChatRichTextToolbar].
 *
 * Eight integrator params, `modifier` excluded by [Denominator], leaving seven — and
 * nothing waived. Every param is observable, though not all in the same place:
 * `enabledFormats`, `disabledFormats` and the three callbacks are visible in the semantics
 * tree, while `style` and `activeFormats` are only visible in pixels.
 *
 * `activeFormats` is the reason this matrix has to paint. Active state is a tint plus a
 * rounded background behind the icon and nothing else — no semantics change, no label
 * change — so the only honest way to assert it is to give the active background a colour
 * nothing else uses and look for it on screen. Asserting anything structural would pass
 * whether or not the prop did anything.
 *
 * Buttons are scrolled into view before being tapped: ten 40dp buttons do not fit the
 * 400dp viewport, and a `performClick` on an off-screen node silently does nothing. The
 * functional test alongside this one records that in full.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatRichTextToolbarComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatRichTextToolbar"
        const val CLOSE = "Close formatting toolbar"
        const val MAGENTA_ARGB: Int = 0xFFFF00FF.toInt()
        const val CYAN_ARGB: Int = 0xFF00FFFF.toInt()
        val MAGENTA = androidx.compose.ui.graphics.Color(0xFFFF00FF)
        val CYAN = androidx.compose.ui.graphics.Color(0xFF00FFFF)
    }

    /** See the sibling matrices: `captureToImage()` has no window here. */
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

    private fun buttons(label: String) =
        composeRule.onAllNodesWithContentDescription(label).fetchSemanticsNodes().size

    private fun tap(label: String) {
        composeRule.onNodeWithContentDescription(label).performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun richTextToolbar_propMatrix_coversEveryObservableProp() {
        var styleOverride by mutableStateOf<CometChatMessageComposerStyle?>(null)
        var activeFormats by mutableStateOf(emptySet<RichTextFormat>())
        var disabledFormats by mutableStateOf(emptySet<RichTextFormat>())
        var enabledFormats by mutableStateOf(RichTextFormat.entries.toSet())
        var showClose by mutableStateOf(true)

        var format: RichTextFormat? = null
        var linkClicked = false
        var closeClicked = false

        // default() is @Composable — it reads CometChatTheme — so it can only be
        // evaluated inside the composition; captured here for the style entries to
        // copy() from.
        var defaultStyle: CometChatMessageComposerStyle? = null

        composeRule.setContent {
            CometChatTheme {
                defaultStyle = CometChatMessageComposerStyle.default()
                CometChatRichTextToolbar(
                    style = styleOverride ?: defaultStyle!!,
                    activeFormats = activeFormats,
                    disabledFormats = disabledFormats,
                    enabledFormats = enabledFormats,
                    onFormatClick = { format = it },
                    onLinkClick = { linkClicked = true },
                    onCloseClick = if (showClose) ({ closeClicked = true }) else null,
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("style") {
                styleOverride = defaultStyle!!.copy(richTextToolbarBackgroundColor = MAGENTA)
                composeRule.waitForIdle()
                assertTrue(
                    "the toolbar's own background colour should be on screen",
                    MAGENTA_ARGB in paintedColours(),
                )
                styleOverride = null
                composeRule.waitForIdle()
            }

            value("activeFormats") {
                // Active state is a tint and a rounded background behind the icon, with no
                // semantics of its own, so this is measured in pixels or not at all.
                styleOverride = defaultStyle!!.copy(richTextToolbarActiveIconBackgroundColor = CYAN)
                composeRule.waitForIdle()
                assertTrue(
                    "with nothing active, the active background must not be painted",
                    CYAN_ARGB !in paintedColours(),
                )

                activeFormats = setOf(RichTextFormat.BOLD)
                composeRule.waitForIdle()
                assertTrue(
                    "marking a format active should paint its active background",
                    CYAN_ARGB in paintedColours(),
                )

                activeFormats = emptySet()
                styleOverride = null
                composeRule.waitForIdle()
            }

            value("disabledFormats") {
                // Observable through behaviour rather than appearance: the button stays,
                // but stops responding.
                disabledFormats = setOf(RichTextFormat.BOLD)
                composeRule.waitForIdle()
                assertEquals("a disabled button is greyed, not removed", 1, buttons("Bold"))

                format = null
                tap("Bold")
                assertNull("a disabled button must not report", format)

                disabledFormats = emptySet()
                composeRule.waitForIdle()
                tap("Bold")
                assertEquals(
                    "and must work again once re-enabled",
                    RichTextFormat.BOLD,
                    format,
                )
            }

            value("enabledFormats") {
                enabledFormats = setOf(RichTextFormat.BOLD, RichTextFormat.ITALIC)
                composeRule.waitForIdle()
                assertEquals(1, buttons("Bold"))
                assertEquals(
                    "a format left out of the set should not be built at all",
                    0,
                    buttons("Code Block"),
                )

                enabledFormats = RichTextFormat.entries.toSet()
                composeRule.waitForIdle()
                assertEquals("and comes back when the set does", 1, buttons("Code Block"))
            }

            callback("onFormatClick") {
                format = null
                tap("Italic")
                assertEquals(
                    "each button reports its own format",
                    RichTextFormat.ITALIC,
                    format,
                )
            }

            callback("onLinkClick") {
                format = null
                tap("Link")
                assertTrue("Link opens the link flow", linkClicked)
                assertNull("and is not reported as a format toggle", format)
            }

            callback("onCloseClick") {
                composeRule.onNodeWithContentDescription(CLOSE).assertIsDisplayed()
                tap(CLOSE)
                assertTrue(closeClicked)

                // The button exists only because a handler was supplied.
                showClose = false
                composeRule.waitForIdle()
                assertEquals(
                    "without a handler there should be no close button",
                    0,
                    buttons(CLOSE),
                )
                showClose = true
                composeRule.waitForIdle()
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [rich text toolbar prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [toolbar] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("every param of this toolbar is reachable", 0, cov.waived)
    }
}
