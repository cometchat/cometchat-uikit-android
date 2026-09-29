package com.cometchat.uikit.compose.presentation.messagecomposer.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.formatter.RichTextFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Functional layer for [CometChatRichTextToolbar].
 *
 * Ten formatting buttons over one flat row, and almost everything interesting is about
 * which of them exist and which of them respond:
 *
 * - `enabledFormats` decides whether a button is **built at all** (not merely hidden), so
 *   an integrator can ship a composer with a reduced formatting set.
 * - `disabledFormats` keeps the button on screen but makes it **inert** — that is the
 *   "this format is incompatible with what you already applied" state, and the distinction
 *   from `enabledFormats` matters: a disappearing button and a greyed one say different
 *   things to the user.
 * - `activeFormats` only changes tint and background, so it is asserted from pixels in the
 *   prop matrix rather than here; there is nothing in the semantics tree to see.
 *
 * The one genuine asymmetry is the Link button. Every other button reports through
 * `onFormatClick` with its own [RichTextFormat]; Link goes to `onLinkClick` instead,
 * because it opens a URL dialog rather than toggling a span. Wiring it to `onFormatClick`
 * would look correct and quietly do nothing useful, so it is pinned twice — Link reaches
 * `onLinkClick`, and it does *not* reach `onFormatClick`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatRichTextToolbarComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val TOOLBAR = "Rich Text Toolbar"
        const val CLOSE = "Close formatting toolbar"

        /** Every format the toolbar renders a button for, with its a11y label. */
        val LABELS = mapOf(
            RichTextFormat.BOLD to "Bold",
            RichTextFormat.ITALIC to "Italic",
            RichTextFormat.UNDERLINE to "Underline",
            RichTextFormat.STRIKETHROUGH to "Strikethrough",
            RichTextFormat.LINK to "Link",
            // Labelled "Numbered List", not "Ordered List" — the enum name and the
            // a11y string disagree, and only the string is what a screen reader says.
            RichTextFormat.ORDERED_LIST to "Numbered List",
            RichTextFormat.BULLET_LIST to "Bullet List",
            RichTextFormat.BLOCKQUOTE to "Blockquote",
            RichTextFormat.INLINE_CODE to "Inline Code",
            RichTextFormat.CODE_BLOCK to "Code Block",
        )
    }

    private class Sink {
        var format: RichTextFormat? = null
        var linkClicked = false
        var closeClicked = false
    }

    private fun render(
        activeFormats: Set<RichTextFormat> = emptySet(),
        disabledFormats: Set<RichTextFormat> = emptySet(),
        enabledFormats: Set<RichTextFormat> = RichTextFormat.entries.toSet(),
        withClose: Boolean = false,
    ): Sink {
        val sink = Sink()
        composeRule.setContent {
            CometChatTheme {
                CometChatRichTextToolbar(
                    activeFormats = activeFormats,
                    disabledFormats = disabledFormats,
                    enabledFormats = enabledFormats,
                    onFormatClick = { sink.format = it },
                    onLinkClick = { sink.linkClicked = true },
                    onCloseClick = if (withClose) ({ sink.closeClicked = true }) else null,
                )
            }
        }
        composeRule.waitForIdle()
        return sink
    }

    private fun buttons(label: String) =
        composeRule.onAllNodesWithContentDescription(label).fetchSemanticsNodes().size

    /**
     * Scrolls before tapping. Ten 40dp buttons do not fit the 400dp viewport these tests
     * run at, so the trailing ones (Code Block especially) start off-screen and a bare
     * performClick on them registers nothing — the toolbar's horizontal scroll is
     * load-bearing, not decorative.
     */
    private fun tap(label: String) {
        composeRule.onNodeWithContentDescription(label).performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    // ── what the default toolbar offers ─────────────────────────────────────

    @Test
    fun theToolbarAnnouncesItself() {
        render()
        composeRule.onNodeWithContentDescription(TOOLBAR).assertIsDisplayed()
    }

    @Test
    fun byDefaultEveryFormatGetsAButton() {
        render()
        LABELS.forEach { (format, label) ->
            assertEquals("$format should have a button by default", 1, buttons(label))
        }
    }

    @Test
    fun theCloseButtonIsAbsentUnlessACloseHandlerIsGiven() {
        // Multiline mode supplies one; the inline composer does not, and an inert X would
        // be worse than no X.
        render(withClose = false)
        assertEquals(0, buttons(CLOSE))
    }

    @Test
    fun aCloseHandlerBringsItsOwnButton() {
        render(withClose = true)
        composeRule.onNodeWithContentDescription(CLOSE).assertIsDisplayed()
    }

    // ── enabledFormats decides what exists ──────────────────────────────────

    @Test
    fun aReducedFormatSetBuildsOnlyThoseButtons() {
        render(enabledFormats = setOf(RichTextFormat.BOLD, RichTextFormat.ITALIC))

        assertEquals(1, buttons("Bold"))
        assertEquals(1, buttons("Italic"))
        assertEquals("Underline was not enabled, so it should not exist", 0, buttons("Underline"))
        assertEquals(0, buttons("Code Block"))
    }

    @Test
    fun anEmptyFormatSetLeavesTheToolbarWithNoButtons() {
        render(enabledFormats = emptySet())

        composeRule.onNodeWithContentDescription(TOOLBAR).assertIsDisplayed()
        LABELS.values.forEach { label ->
            assertEquals("$label should not be built", 0, buttons(label))
        }
    }

    @Test
    fun aDisabledFormatKeepsItsButtonUnlikeAnUnenabledOne() {
        // The distinction this component exists to make: greyed out is not the same as
        // gone.
        render(disabledFormats = setOf(RichTextFormat.BOLD))
        assertEquals("a disabled format stays on screen", 1, buttons("Bold"))
    }

    // ── disabledFormats decides what responds ───────────────────────────────

    @Test
    fun aDisabledButtonDoesNotReport() {
        val sink = render(disabledFormats = setOf(RichTextFormat.BOLD))
        tap("Bold")
        assertNull("a disabled button must be inert, not merely faded", sink.format)
    }

    @Test
    fun disablingOneFormatLeavesTheOthersWorking() {
        val sink = render(disabledFormats = setOf(RichTextFormat.BOLD))
        tap("Italic")
        assertEquals(RichTextFormat.ITALIC, sink.format)
    }

    @Test
    fun aDisabledLinkButtonDoesNotOpenTheDialog() {
        // Link is routed differently from the rest, so its disabled path is worth its own
        // check rather than being assumed to follow.
        val sink = render(disabledFormats = setOf(RichTextFormat.LINK))
        tap("Link")
        assertFalse(sink.linkClicked)
    }

    @Test
    fun aFormatCanBeActiveAndDisabledAtOnceAndStaysInert() {
        val sink = render(
            activeFormats = setOf(RichTextFormat.BOLD),
            disabledFormats = setOf(RichTextFormat.BOLD),
        )
        tap("Bold")
        assertNull("disabled wins over active", sink.format)
    }

    // ── each button reports itself ──────────────────────────────────────────

    @Test
    fun everyFormatButtonReportsItsOwnFormat() {
        // A copy-paste slip in this long chain of near-identical buttons would send the
        // wrong format and still look right on screen. One composition, every button
        // tapped in turn: the rule permits only a single setContent per test.
        val sink = render()

        LABELS.filterKeys { it != RichTextFormat.LINK }.forEach { (format, label) ->
            sink.format = null
            tap(label)
            assertEquals("$label should report $format", format, sink.format)
        }
    }

    // ── link is the exception ───────────────────────────────────────────────

    @Test
    fun theLinkButtonOpensTheLinkFlowInsteadOfTogglingAFormat() {
        val sink = render()
        tap("Link")

        assertTrue("Link should reach onLinkClick", sink.linkClicked)
        assertNull("and must not also be reported as a format toggle", sink.format)
    }

    @Test
    fun anOrdinaryFormatDoesNotOpenTheLinkFlow() {
        val sink = render()
        tap("Bold")

        assertEquals(RichTextFormat.BOLD, sink.format)
        assertFalse(sink.linkClicked)
    }

    // ── close ───────────────────────────────────────────────────────────────

    @Test
    fun theCloseButtonReportsSeparatelyFromEveryFormat() {
        val sink = render(withClose = true)
        tap(CLOSE)

        assertTrue(sink.closeClicked)
        assertNull("closing the toolbar is not a formatting action", sink.format)
        assertFalse(sink.linkClicked)
    }

    @Test
    fun theCloseButtonSurvivesAReducedFormatSet() {
        // It is rendered before the format buttons and gated only on the handler, so
        // trimming the format set must not take it with them.
        val sink = render(enabledFormats = setOf(RichTextFormat.BOLD), withClose = true)
        tap(CLOSE)
        assertTrue(sink.closeClicked)
    }
}
