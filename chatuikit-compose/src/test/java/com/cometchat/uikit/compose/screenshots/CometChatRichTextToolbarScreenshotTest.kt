package com.cometchat.uikit.compose.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.messagecomposer.ui.CometChatRichTextToolbar
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.darkColorScheme
import com.cometchat.uikit.compose.theme.lightColorScheme
import com.cometchat.uikit.core.formatter.RichTextFormat
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Snapshot layer for [CometChatRichTextToolbar] — the third layer it was missing. The
 * functional and prop-matrix layers already exist; nothing pictured this toolbar.
 *
 * It earns a baseline more than most small components, because almost everything it
 * expresses is colour and alpha rather than structure. An active format button differs
 * from an inactive one only by tint plus a filled pill behind the glyph, and a disabled
 * one only by that same tint at 40% alpha. An assertion can confirm the button exists in
 * all three cases and would keep passing if every state painted identically — which is
 * exactly the regression a picture catches and a test does not.
 *
 * Two departures from the sibling screenshot tests, both load-bearing rather than tidiness,
 * and both measured rather than assumed:
 *
 *  - **These capture the toolbar's own node, not the screen.** On the usual full-screen
 *    canvas the toolbar is about 6% of the pixels, so a change to it is diluted by the
 *    empty page around it before any threshold is applied.
 *  - **They do not use the shared `RoborazziConfig`.** Its 1% `changeThreshold` is far too
 *    coarse for a component whose states differ only by tint: disabling a single button
 *    was verified to compare as *unchanged* under it, even cropped to the toolbar. These
 *    captures use an exact comparison, which was verified to fail on that same
 *    single-button change. Any capture added here should keep both properties.
 *
 * The row is `horizontalScroll` and the full ten-format set is wider than a 400dp canvas,
 * so the default captures are clipped at the right edge on purpose — that is what an
 * integrator sees at this width, and pinning it means a change to icon size, spacing or
 * separator width shows up as a different cut-off rather than passing unnoticed.
 *
 * Run:
 *   ./gradlew :chatuikit-compose:recordRoborazziDebug  --tests "*.CometChatRichTextToolbarScreenshotTest"
 *   ./gradlew :chatuikit-compose:compareRoborazziDebug --tests "*.CometChatRichTextToolbarScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatRichTextToolbarScreenshotTest {

    @get:Rule(order = 0)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule(order = 1)
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/richtexttoolbar"
        )
    )

    private companion object {
        val canvasLight = Color(0xFFF4F4F4)
        val canvasDark = Color(0xFF121212)

        /** The toolbar's own semantics label — the crop boundary for every capture. */
        const val TOOLBAR = "Rich Text Toolbar"

        /**
         * Exact comparison. The shared 1% threshold hides exactly the regressions this
         * file exists to catch — see the class comment.
         */
        val strictOptions = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
        )

        /** The text-formatting group, i.e. everything left of the first separator. */
        val TEXT_GROUP = setOf(
            RichTextFormat.BOLD,
            RichTextFormat.ITALIC,
            RichTextFormat.UNDERLINE,
            RichTextFormat.STRIKETHROUGH,
        )
    }

    /**
     * Renders on a themed page but captures only the toolbar node. The page still matters:
     * the toolbar paints its own background over it, so a background regression to
     * transparent would show the canvas rather than nothing at all.
     */
    private fun capture(
        dark: Boolean = false,
        activeFormats: Set<RichTextFormat> = emptySet(),
        disabledFormats: Set<RichTextFormat> = emptySet(),
        enabledFormats: Set<RichTextFormat> = RichTextFormat.entries.toSet(),
        withClose: Boolean = false,
    ) {
        composeTestRule.setContent {
            CometChatTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (dark) canvasDark else canvasLight)
                        .padding(vertical = 16.dp)
                ) {
                    // No `style` is passed, so the toolbar resolves the composer's default
                    // palette — the tints and the active pill these baselines exist to pin.
                    CometChatRichTextToolbar(
                        modifier = Modifier.fillMaxWidth(),
                        activeFormats = activeFormats,
                        disabledFormats = disabledFormats,
                        enabledFormats = enabledFormats,
                        onCloseClick = if (withClose) ({}) else null,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(TOOLBAR)
            .captureRoboImage(roborazziOptions = strictOptions)
    }

    // ── the resting row ─────────────────────────────────────────────────────

    @Test fun stateDefault() = capture()

    @Test fun stateDefaultDark() = capture(dark = true)

    // ── per-button states ───────────────────────────────────────────────────

    /** Active is a tint change plus a filled pill — the pill is the part worth a picture. */
    @Test fun stateActiveBoldAndItalic() =
        capture(activeFormats = setOf(RichTextFormat.BOLD, RichTextFormat.ITALIC))

    @Test fun stateActiveBoldAndItalicDark() =
        capture(dark = true, activeFormats = setOf(RichTextFormat.BOLD, RichTextFormat.ITALIC))

    /** Disabled is the inactive tint at 40% alpha and nothing else. */
    @Test fun stateDisabledTextGroup() = capture(disabledFormats = TEXT_GROUP)

    /**
     * Active and disabled are independent flags, so a format can carry both. Pinned
     * because the precedence between them is a palette decision with no assertion behind
     * it — the active pill is drawn either way.
     */
    @Test fun stateActiveAndDisabledTogether() = capture(
        activeFormats = setOf(RichTextFormat.BOLD),
        disabledFormats = setOf(RichTextFormat.BOLD),
    )

    // ── the multiline variant ───────────────────────────────────────────────

    /** `onCloseClick` is what the composer passes in multiline mode; it adds a leading X. */
    @Test fun stateWithCloseButton() = capture(withClose = true)

    @Test fun stateWithCloseButtonDark() = capture(dark = true, withClose = true)

    // ── the one prop that changes the row's shape ───────────────────────────

    /**
     * The only structural prop: an integrator who restricts `enabledFormats` gets a
     * shorter row, and at this width one that no longer overflows — separators included,
     * since they are drawn unconditionally.
     */
    @Test fun stateTextGroupOnly() = capture(enabledFormats = TEXT_GROUP)

    @Test fun stateSingleFormatEnabled() = capture(enabledFormats = setOf(RichTextFormat.BOLD))
}
