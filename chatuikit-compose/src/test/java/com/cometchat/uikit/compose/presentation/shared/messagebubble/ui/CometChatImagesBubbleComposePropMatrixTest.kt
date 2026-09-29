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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Attachment
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatImagesBubbleStyle
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Property (prop-matrix) layer for [CometChatImagesBubble].
 *
 * Eight integrator params (`modifier` excluded by [Denominator]); only `textFormatters`
 * is waived, on the same grounds as the audio and video bubbles — it acts through the
 * caption's markdown/mention pipeline, and asserting it here would assert the formatter
 * rather than this bubble's use of it.
 *
 * The whole matrix runs against a seven-attachment message on purpose. Four is the grid
 * cap, so seven is the only shape in which both click callbacks are reachable at once:
 * `onMoreClick` needs an overflow tile to exist, and `onMediaClick` needs an ordinary
 * tile still to exist beside it.
 *
 * `alignment` is read from painted pixels, and a caption is deliberately on screen while
 * it is measured. `incoming()` and `outgoing()` on this style differ in exactly one
 * property — `captionTextColor` — so with no caption rendered the two alignments paint
 * identically and the assertion would be measuring nothing.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatImagesBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatImagesBubble"
        const val IMAGE_MIME = "image/jpeg"
        const val IMAGE_EXT = "jpg"
        const val ATTACHMENT_COUNT = 7
        const val OVERFLOW_BADGE = "+3"
        const val CAPTION = "the whole roll"
        const val OTHER_CAPTION = "a different roll"
        const val MAGENTA_ARGB: Int = 0xFFFF00FF.toInt()
        val MAGENTA = androidx.compose.ui.graphics.Color(0xFFFF00FF)
    }

    /** See the sibling bubble matrices: `captureToImage()` has no window here. */
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
    fun imagesBubble_propMatrix_coversEveryObservableProp() {
        var alignment by mutableStateOf(UIKitConstants.MessageBubbleAlignment.LEFT)
        var styleOverride by mutableStateOf<CometChatImagesBubbleStyle?>(null)
        var caption by mutableStateOf<String?>(CAPTION)
        var mediaIndex: Int? = null
        var mediaAttachment: Attachment? = null
        var moreAttachments: List<Attachment>? = null
        var longClicked = false
        var incoming: CometChatImagesBubbleStyle? = null
        var outgoing: CometChatImagesBubbleStyle? = null

        val message = MockFactory.createMediaMessage(
            count = ATTACHMENT_COUNT,
            type = CometChatConstants.MESSAGE_TYPE_IMAGE,
            mimeType = IMAGE_MIME,
            extension = IMAGE_EXT,
        )

        composeRule.setContent {
            CometChatTheme {
                incoming = CometChatImagesBubbleStyle.incoming()
                outgoing = CometChatImagesBubbleStyle.outgoing()
                val override = styleOverride
                if (override == null) {
                    // No `style` argument — the default expression resolves it from
                    // `alignment`, which is what the alignment entry measures.
                    CometChatImagesBubble(
                        message = message,
                        alignment = alignment,
                        caption = caption,
                        onMediaClick = { i, a -> mediaIndex = i; mediaAttachment = a },
                        onMoreClick = { moreAttachments = it },
                        onLongClick = { longClicked = true },
                    )
                } else {
                    CometChatImagesBubble(
                        message = message,
                        alignment = alignment,
                        style = override,
                        caption = caption,
                        onMediaClick = { i, a -> mediaIndex = i; mediaAttachment = a },
                        onMoreClick = { moreAttachments = it },
                        onLongClick = { longClicked = true },
                    )
                }
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("message") {
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription("media_1.$IMAGE_EXT").assertIsDisplayed()
                composeRule.onNodeWithContentDescription(
                    "Image message with $ATTACHMENT_COUNT item(s)",
                ).assertIsDisplayed()
                assertEquals(
                    "the grid caps at four tiles and folds the rest into the badge",
                    1,
                    composeRule.onAllNodesWithText(OVERFLOW_BADGE).fetchSemanticsNodes().size,
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
                // The tile placeholder is the one colour guaranteed on screen: no
                // network here, so no thumbnail ever paints over it.
                styleOverride = incoming!!.copy(tilePlaceholderColor = MAGENTA)
                composeRule.waitForIdle()
                assertTrue(
                    "the supplied tilePlaceholderColor should be on screen",
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
                caption = OTHER_CAPTION
                composeRule.waitForIdle()
                composeRule.onNodeWithText(OTHER_CAPTION).assertIsDisplayed()
                caption = null
                composeRule.waitForIdle()
                assertEquals(
                    "clearing the caption should remove it again",
                    0,
                    composeRule.onAllNodesWithText(OTHER_CAPTION).fetchSemanticsNodes().size,
                )
                caption = CAPTION
                composeRule.waitForIdle()
            }

            callback("onMediaClick") {
                composeRule.onNodeWithContentDescription("media_2.$IMAGE_EXT").performClick()
                composeRule.waitForIdle()
                assertEquals("the second tile reports index 1", 1, mediaIndex)
                assertEquals("media_2.$IMAGE_EXT", mediaAttachment?.fileName)
            }

            callback("onMoreClick") {
                mediaIndex = null
                composeRule.onNodeWithText(OVERFLOW_BADGE).performClick()
                composeRule.waitForIdle()
                assertEquals(
                    "the overflow tile hands back every attachment",
                    ATTACHMENT_COUNT,
                    moreAttachments?.size,
                )
                assertNull(
                    "and must not also fire the ordinary media callback",
                    mediaIndex,
                )
            }

            callback("onLongClick") {
                composeRule.onNodeWithContentDescription("media_1.$IMAGE_EXT")
                    .performTouchInput { longClick() }
                composeRule.waitForIdle()
                assertTrue("a long press should reach the integrator", longClicked)
            }
        }

        val props = matrix.evaluate() + waivedProps()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [images compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [images] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("only textFormatters is waived", 1, cov.waived)
    }

    /**
     * `textFormatters` only takes effect through the caption's markdown/mention
     * pipeline — the same reason it is waived on the audio and video bubbles.
     */
    private fun waivedProps(): List<Prop> = listOf(
        Prop(OWNER, "textFormatters", PropKind.VALUE, waived = true),
    )
}
