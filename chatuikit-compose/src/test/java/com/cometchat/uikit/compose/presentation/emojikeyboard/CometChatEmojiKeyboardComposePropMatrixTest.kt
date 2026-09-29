package com.cometchat.uikit.compose.presentation.emojikeyboard

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.emojikeyboard.model.EmojiRepository
import com.cometchat.uikit.compose.presentation.emojikeyboard.style.CometChatEmojiKeyboardStyle
import com.cometchat.uikit.compose.presentation.emojikeyboard.ui.CometChatEmojiKeyboard
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
 * Property (prop-matrix) layer for [CometChatEmojiKeyboard] — the first matrix on the
 * Compose side of the composer satellites, mirroring the View one that landed with the
 * sticker keyboard and poll composer.
 *
 * Four integrator params, `modifier` excluded by [Denominator], leaving three, and
 * nothing waived. This is the smallest surface in the satellite group and every prop is
 * directly observable, so a waiver would mean a missing test rather than an unobservable
 * prop.
 *
 * The fixture has one precondition worth stating: the keyboard renders a progress
 * indicator until [EmojiRepository] has parsed its asset, and it polls for that on a
 * 50ms loop rather than exposing a loaded state. The repository is therefore primed
 * before the composition and the grid is waited for explicitly — without that, every
 * entry below would be asserting against a spinner, and the two callbacks would have no
 * emoji to tap.
 *
 * `style` is read from painted pixels rather than from structure. The keyboard's own
 * styling is a `drawBehind` rect under a grid that paints nothing of its own, so there
 * is no node whose properties could carry it.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatEmojiKeyboardComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatEmojiKeyboard"

        /** First emoji of the first category — the grid's top-left cell. */
        const val FIRST_EMOJI = "😀"

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

    private fun emojiNodes(emoji: String) =
        composeRule.onAllNodesWithContentDescription(emoji)

    /** The grid replaces the progress indicator only once the asset has been parsed. */
    private fun awaitGrid() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            emojiNodes(FIRST_EMOJI).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun emojiKeyboard_propMatrix_coversEveryObservableProp() {
        // Primed before the composition: the keyboard polls for this on a 50ms loop and
        // shows a spinner until it lands, which every entry below would otherwise hit.
        EmojiRepository.loadAndSaveEmojis(composeRule.activity)

        var styleOverride by mutableStateOf<CometChatEmojiKeyboardStyle?>(null)
        var clicked: String? = null
        var longClicked: String? = null

        // `default()` is @Composable — it reads CometChatTheme — so it can only be
        // evaluated inside the composition. Captured for the style entry to copy() from.
        var defaultStyle: CometChatEmojiKeyboardStyle? = null

        composeRule.setContent {
            CometChatTheme {
                defaultStyle = CometChatEmojiKeyboardStyle.default()
                CometChatEmojiKeyboard(
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    style = styleOverride ?: defaultStyle!!,
                    onClick = { clicked = it },
                    onLongClick = { longClicked = it },
                )
            }
        }
        awaitGrid()

        val matrix = composePropMatrix(OWNER) {
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

            callback("onClick") {
                emojiNodes(FIRST_EMOJI)[0].performClick()
                composeRule.waitForIdle()
                assertEquals(
                    "the tapped emoji is the one handed back",
                    FIRST_EMOJI,
                    clicked,
                )
            }

            callback("onLongClick") {
                emojiNodes(FIRST_EMOJI)[0].performTouchInput { longClick() }
                composeRule.waitForIdle()
                assertEquals(FIRST_EMOJI, longClicked)
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [emoji keyboard prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [emoji keyboard] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("the keyboard has nothing worth waiving", 0, cov.waived)
    }
}
