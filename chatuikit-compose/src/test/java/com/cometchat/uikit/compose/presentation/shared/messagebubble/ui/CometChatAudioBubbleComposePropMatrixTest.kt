package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatAudioBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.propmatrix.Denominator
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
 * Property (prop-matrix) layer for the single [CometChatAudioBubble].
 *
 * Four integrator params (`modifier` excluded by [Denominator]), none waived.
 *
 * Two of them — `alignment` and `style` — have no text or semantics to assert on:
 * this bubble's whole visible difference between incoming and outgoing is *colour*.
 * So the effects are read from real pixels, captured under native graphics, rather
 * than asserted as "the call was accepted". `alignment` is checked by rendering the
 * same message on both sides with **no** style supplied, so the default-parameter
 * expression that maps side to `incoming()`/`outgoing()` is the thing under test.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAudioBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatAudioBubble"
        const val AUDIO_MIME = "audio/mpeg"
        const val AUDIO_EXT = "mp3"

        /** `createAttachment`'s default 3,200,000 bytes, as the bubble formats it before playback. */
        const val INITIAL_SIZE_LABEL = "3.1 MB"

        val MAGENTA = androidx.compose.ui.graphics.Color(0xFFFF00FF)
        const val MAGENTA_ARGB: Int = 0xFFFF00FF.toInt()
    }

    /**
     * Every distinct colour actually painted — enough to tell two fills apart without
     * committing a baseline. Drawn straight onto a software canvas rather than through
     * `captureToImage()`, which needs a real window's PixelCopy and times out here.
     */
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
    fun audioBubble_propMatrix_coversEveryProp() {
        var alignment by mutableStateOf(UIKitConstants.MessageBubbleAlignment.LEFT)
        var styleOverride by mutableStateOf<CometChatAudioBubbleStyle?>(null)
        var longClicked = false
        // The factories are @Composable, so they can only be resolved inside the tree.
        var incoming: CometChatAudioBubbleStyle? = null
        var outgoing: CometChatAudioBubbleStyle? = null

        val message = MockFactory.createMediaMessage(
            count = 1,
            type = CometChatConstants.MESSAGE_TYPE_AUDIO,
            mimeType = AUDIO_MIME,
            extension = AUDIO_EXT,
        )

        composeRule.setContent {
            CometChatTheme {
                incoming = CometChatAudioBubbleStyle.incoming()
                outgoing = CometChatAudioBubbleStyle.outgoing()
                val override = styleOverride
                if (override == null) {
                    // No `style` argument at all — the default expression resolves it from
                    // `alignment`, which is what the alignment entry below measures.
                    CometChatAudioBubble(
                        message = message,
                        alignment = alignment,
                        onLongClick = { longClicked = true },
                    )
                } else {
                    CometChatAudioBubble(
                        message = message,
                        alignment = alignment,
                        style = override,
                        onLongClick = { longClicked = true },
                    )
                }
            }
        }

        val matrix = composePropMatrix(OWNER) {
            // Before anything is downloaded the bubble shows the attachment's size, so
            // the message reaching the component is readable straight off the screen.
            value("message") {
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription("Audio message").assertIsDisplayed()
                composeRule.onNodeWithText(INITIAL_SIZE_LABEL).assertIsDisplayed()
            }

            value("alignment") {
                composeRule.waitForIdle()
                val left = paintedColours()
                alignment = UIKitConstants.MessageBubbleAlignment.RIGHT
                composeRule.waitForIdle()
                val right = paintedColours()
                assertNotEquals(
                    "incoming and outgoing must not paint the same pixels",
                    left,
                    right,
                )
                // Worth recording what that difference *is*: hosted bare on a light
                // page, the outgoing palette collapses to almost nothing, because it
                // is drawn for the tinted fill the container supplies. That is why the
                // snapshot layer captures through `CometChatMessageBubble` and never
                // bare — see CometChatAudioBubbleScreenshotTest.
                assertTrue(
                    "outgoing should be the light-on-tint palette, near-invisible unhosted",
                    right.size < left.size,
                )
                alignment = UIKitConstants.MessageBubbleAlignment.LEFT
                composeRule.waitForIdle()
            }

            value("style") {
                // A supplied style must reach the paint, not merely be accepted.
                styleOverride = incoming!!.copy(buttonBackgroundColor = MAGENTA)
                composeRule.waitForIdle()
                assertTrue(
                    "the supplied buttonBackgroundColor should be on screen",
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

            callback("onLongClick") {
                composeRule.onNodeWithContentDescription("Audio message")
                    .performTouchInput { longClick() }
                composeRule.waitForIdle()
                assertTrue("a long press should reach the integrator", longClicked)
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered }.map { it.name }

        println("  [audio compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [audio] NOT covered: $uncovered")

        assertEquals("every prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("nothing waived on this bubble", 0, cov.waived)
    }
}
