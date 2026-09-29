package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatAudiosBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Property (prop-matrix) layer for [CometChatAudiosBubble].
 *
 * This is the bubble an audio message actually reaches: under the default
 * `LocalEnableMultipleAttachments = true`, `InternalContentRenderer` routes every
 * non-voice-note audio message here, and the single [CometChatAudioBubble] is only
 * reached with multi-attachments switched off.
 *
 * Eight integrator params (`modifier` excluded by [Denominator]); only
 * `textFormatters` is waived, on the same grounds as the video bubble — it only takes
 * effect through the caption's markdown/mention pipeline, which is covered where that
 * pipeline is, not here. `alignment` and `style` are read from painted pixels, since
 * the difference between incoming and outgoing on this bubble is entirely colour.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAudiosBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatAudiosBubble"
        const val AUDIO_MIME = "audio/mpeg"
        const val AUDIO_EXT = "mp3"
        const val FIRST_FILE = "media_1.mp3"
        const val CAPTION = "two takes of the same riff"
        const val MAGENTA_ARGB: Int = 0xFFFF00FF.toInt()
        val MAGENTA = androidx.compose.ui.graphics.Color(0xFFFF00FF)
    }

    /** See the sibling single-bubble matrix: `captureToImage()` has no window here. */
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

    @Test
    fun audiosBubble_propMatrix_coversEveryObservableProp() {
        var alignment by mutableStateOf(UIKitConstants.MessageBubbleAlignment.LEFT)
        var styleOverride by mutableStateOf<CometChatAudiosBubbleStyle?>(null)
        var caption by mutableStateOf<String?>(null)
        var showDownloadIcon by mutableStateOf(true)
        var downloadedIndex: Int? = null
        var longClicked = false
        var incoming: CometChatAudiosBubbleStyle? = null
        var outgoing: CometChatAudiosBubbleStyle? = null

        val message = MockFactory.createMediaMessage(
            count = 2,
            type = CometChatConstants.MESSAGE_TYPE_AUDIO,
            mimeType = AUDIO_MIME,
            extension = AUDIO_EXT,
        )

        composeRule.setContent {
            CometChatTheme {
                incoming = CometChatAudiosBubbleStyle.incoming()
                outgoing = CometChatAudiosBubbleStyle.outgoing()
                val override = styleOverride
                if (override == null) {
                    // No `style` argument — the default expression resolves it from
                    // `alignment`, which is what the alignment entry measures.
                    CometChatAudiosBubble(
                        message = message,
                        alignment = alignment,
                        caption = caption,
                        showDownloadIcon = showDownloadIcon,
                        onDownloadClick = { downloadedIndex = it },
                        onLongClick = { longClicked = true },
                    )
                } else {
                    CometChatAudiosBubble(
                        message = message,
                        alignment = alignment,
                        style = override,
                        caption = caption,
                        showDownloadIcon = showDownloadIcon,
                        onDownloadClick = { downloadedIndex = it },
                        onLongClick = { longClicked = true },
                    )
                }
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("message") {
                composeRule.waitForIdle()
                composeRule.onNodeWithText(FIRST_FILE).assertIsDisplayed()
                assertEquals(
                    "both attachments should get a card",
                    2,
                    composeRule.onAllNodesWithContentDescription("Play").fetchSemanticsNodes().size,
                )
            }

            value("alignment") {
                composeRule.waitForIdle()
                val left = paintedColours()
                alignment = UIKitConstants.MessageBubbleAlignment.RIGHT
                composeRule.waitForIdle()
                assertNotEquals(
                    "incoming and outgoing must not paint the same pixels",
                    left,
                    paintedColours(),
                )
                alignment = UIKitConstants.MessageBubbleAlignment.LEFT
                composeRule.waitForIdle()
            }

            value("style") {
                styleOverride = incoming!!.copy(playButtonBackgroundColor = MAGENTA)
                composeRule.waitForIdle()
                assertTrue(
                    "the supplied playButtonBackgroundColor should be on screen",
                    MAGENTA_ARGB in paintedColours(),
                )
                assertNotEquals(
                    "incoming() and outgoing() should resolve differently",
                    incoming,
                    outgoing,
                )
                styleOverride = null
                composeRule.waitForIdle()
            }

            value("caption") {
                caption = CAPTION
                composeRule.waitForIdle()
                composeRule.onNodeWithText(CAPTION).assertIsDisplayed()
                caption = null
                composeRule.waitForIdle()
                assertEquals(
                    "clearing the caption should remove it again",
                    0,
                    composeRule.onAllNodesWithContentDescription(CAPTION).fetchSemanticsNodes().size,
                )
            }

            value("showDownloadIcon") {
                composeRule.waitForIdle()
                assertEquals(
                    "one download affordance per attachment",
                    2,
                    composeRule.onAllNodesWithContentDescription("Download audio").fetchSemanticsNodes().size,
                )
                showDownloadIcon = false
                composeRule.waitForIdle()
                assertEquals(
                    "switching it off should remove them",
                    0,
                    composeRule.onAllNodesWithContentDescription("Download audio").fetchSemanticsNodes().size,
                )
                showDownloadIcon = true
                composeRule.waitForIdle()
            }

            callback("onDownloadClick") {
                composeRule.onAllNodesWithContentDescription("Download audio")[1].performClick()
                composeRule.waitForIdle()
                assertEquals("the second card reports index 1", 1, downloadedIndex)
            }

            callback("onLongClick") {
                composeRule.onNodeWithText(FIRST_FILE).performTouchInput { longClick() }
                composeRule.waitForIdle()
                assertTrue("a long press should reach the integrator", longClicked)
            }
        }

        val props = matrix.evaluate() + waivedProps()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [audios compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [audios] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("only textFormatters is waived", 1, cov.waived)
    }

    /**
     * `textFormatters` only takes effect through the caption's markdown/mention
     * pipeline — the same reason it is waived on the video bubble. Asserting it here
     * would assert the formatter, not this bubble's use of it.
     */
    private fun waivedProps(): List<Prop> = listOf(
        Prop(OWNER, "textFormatters", PropKind.VALUE, waived = true),
    )
}
