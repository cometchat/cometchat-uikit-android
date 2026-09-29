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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Attachment
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatVideosBubbleStyle
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
 * Property (prop-matrix) layer for [CometChatVideosBubble].
 *
 * Eight integrator params (`modifier` excluded by [Denominator]); only `textFormatters`
 * is waived, for the reason given on the sibling bubbles — it acts through the caption's
 * markdown/mention pipeline, so asserting it here would assert the formatter.
 *
 * Like the images matrix this runs against a seven-attachment message, the only shape in
 * which `onMoreClick` and `onMediaClick` are both reachable. On this bubble that shape
 * also means three play badges rather than four: the overflow tile trades its badge for
 * the "+N" overlay.
 *
 * `style` is measured through `playBadgeBackgroundColor`. It is the honest choice here —
 * off-network no thumbnail or duration chip ever renders, so the badge is the only part
 * of the video-specific styling that reliably reaches the screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatVideosBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatVideosBubble"
        const val VIDEO_MIME = "video/mp4"
        const val VIDEO_EXT = "mp4"
        const val ATTACHMENT_COUNT = 7
        const val BADGES_WHEN_OVERFLOWING = 3
        const val OVERFLOW_BADGE = "+3"
        const val CAPTION = "both takes"
        const val OTHER_CAPTION = "a different take"
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

    private fun playBadges() =
        composeRule.onAllNodesWithContentDescription("Play").fetchSemanticsNodes().size

    @Test
    fun videosBubble_propMatrix_coversEveryObservableProp() {
        var alignment by mutableStateOf(UIKitConstants.MessageBubbleAlignment.LEFT)
        var styleOverride by mutableStateOf<CometChatVideosBubbleStyle?>(null)
        var caption by mutableStateOf<String?>(CAPTION)
        var mediaIndex: Int? = null
        var mediaAttachment: Attachment? = null
        var moreAttachments: List<Attachment>? = null
        var longClicked = false
        var incoming: CometChatVideosBubbleStyle? = null
        var outgoing: CometChatVideosBubbleStyle? = null

        val message = MockFactory.createMediaMessage(
            count = ATTACHMENT_COUNT,
            type = CometChatConstants.MESSAGE_TYPE_VIDEO,
            mimeType = VIDEO_MIME,
            extension = VIDEO_EXT,
        )

        composeRule.setContent {
            CometChatTheme {
                incoming = CometChatVideosBubbleStyle.incoming()
                outgoing = CometChatVideosBubbleStyle.outgoing()
                val override = styleOverride
                if (override == null) {
                    // No `style` argument — the default expression resolves it from
                    // `alignment`, which is what the alignment entry measures.
                    CometChatVideosBubble(
                        message = message,
                        alignment = alignment,
                        caption = caption,
                        onMediaClick = { i, a -> mediaIndex = i; mediaAttachment = a },
                        onMoreClick = { moreAttachments = it },
                        onLongClick = { longClicked = true },
                    )
                } else {
                    CometChatVideosBubble(
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
                composeRule.onNodeWithContentDescription(
                    "Video message with $ATTACHMENT_COUNT item(s)",
                ).assertIsDisplayed()
                assertEquals(
                    "four tiles, but the overflow one shows the count instead of a badge",
                    BADGES_WHEN_OVERFLOWING,
                    playBadges(),
                )
                composeRule.onNodeWithText(OVERFLOW_BADGE).assertIsDisplayed()
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
                styleOverride = incoming!!.copy(playBadgeBackgroundColor = MAGENTA)
                composeRule.waitForIdle()
                assertTrue(
                    "the supplied playBadgeBackgroundColor should be on screen",
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
                composeRule.onAllNodesWithContentDescription("Play")[1].performClick()
                composeRule.waitForIdle()
                assertEquals("the second tile reports index 1", 1, mediaIndex)
                assertEquals("media_2.$VIDEO_EXT", mediaAttachment?.fileName)
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
                composeRule.onAllNodesWithContentDescription("Play")[0]
                    .performTouchInput { longClick() }
                composeRule.waitForIdle()
                assertTrue("a long press should reach the integrator", longClicked)
            }
        }

        val props = matrix.evaluate() + waivedProps()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [videos compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [videos] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("only textFormatters is waived", 1, cov.waived)
    }

    /**
     * `textFormatters` only takes effect through the caption's markdown/mention
     * pipeline — the same reason it is waived on the sibling bubbles.
     */
    private fun waivedProps(): List<Prop> = listOf(
        Prop(OWNER, "textFormatters", PropKind.VALUE, waived = true),
    )
}
