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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatFilesBubbleStyle
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
 * Property layer for [CometChatFilesBubble] — the bubble a file message reaches under
 * the default multi-attachment setting.
 *
 * `alignment` and `style` are read from painted pixels: the difference between
 * incoming and outgoing here is entirely colour, so asserting the call was accepted
 * would prove nothing.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatFilesBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatFilesBubble"
        const val FILE_MIME = "application/pdf"
        const val FILE_EXT = "pdf"
        const val FIRST_FILE = "media_1.pdf"
        const val CAPTION = "both drafts attached"
        const val MAGENTA_ARGB: Int = 0xFFFF00FF.toInt()
        val MAGENTA = androidx.compose.ui.graphics.Color(0xFFFF00FF)
    }

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
    fun filesBubble_propMatrix_coversEveryObservableProp() {
        var alignment by mutableStateOf(UIKitConstants.MessageBubbleAlignment.LEFT)
        var styleOverride by mutableStateOf<CometChatFilesBubbleStyle?>(null)
        var caption by mutableStateOf<String?>(null)
        var showDownloadIcon by mutableStateOf(true)
        var downloadedIndex: Int? = null
        var clickedIndex: Int? = null
        var longClicked = false
        var incoming: CometChatFilesBubbleStyle? = null
        var outgoing: CometChatFilesBubbleStyle? = null

        val message = MockFactory.createMediaMessage(
            count = 2,
            type = CometChatConstants.MESSAGE_TYPE_FILE,
            mimeType = FILE_MIME,
            extension = FILE_EXT,
        )

        composeRule.setContent {
            CometChatTheme {
                incoming = CometChatFilesBubbleStyle.incoming()
                outgoing = CometChatFilesBubbleStyle.outgoing()
                val override = styleOverride
                if (override == null) {
                    CometChatFilesBubble(
                        message = message,
                        alignment = alignment,
                        caption = caption,
                        showDownloadIcon = showDownloadIcon,
                        onFileClick = { clickedIndex = it },
                        onDownloadClick = { downloadedIndex = it },
                        onLongClick = { longClicked = true },
                    )
                } else {
                    CometChatFilesBubble(
                        message = message,
                        alignment = alignment,
                        style = override,
                        caption = caption,
                        showDownloadIcon = showDownloadIcon,
                        onFileClick = { clickedIndex = it },
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
                    composeRule.onAllNodesWithContentDescription("Download file").fetchSemanticsNodes().size,
                )
            }

            value("alignment") {
                composeRule.waitForIdle()
                val left = paintedColours()
                alignment = UIKitConstants.MessageBubbleAlignment.RIGHT
                composeRule.waitForIdle()
                assertNotEquals("incoming and outgoing must not paint the same pixels", left, paintedColours())
                alignment = UIKitConstants.MessageBubbleAlignment.LEFT
                composeRule.waitForIdle()
            }

            value("style") {
                styleOverride = incoming!!.copy(cardBackgroundColor = MAGENTA)
                composeRule.waitForIdle()
                assertTrue(
                    "the supplied cardBackgroundColor should be on screen",
                    MAGENTA_ARGB in paintedColours(),
                )
                assertNotEquals("incoming() and outgoing() should differ", incoming, outgoing)
                styleOverride = null
                composeRule.waitForIdle()
            }

            value("caption") {
                caption = CAPTION
                composeRule.waitForIdle()
                composeRule.onNodeWithText(CAPTION).assertIsDisplayed()
                caption = null
                composeRule.waitForIdle()
            }

            value("showDownloadIcon") {
                composeRule.waitForIdle()
                assertEquals(
                    2,
                    composeRule.onAllNodesWithContentDescription("Download file").fetchSemanticsNodes().size,
                )
                showDownloadIcon = false
                composeRule.waitForIdle()
                assertEquals(
                    "switching it off should remove them",
                    0,
                    composeRule.onAllNodesWithContentDescription("Download file").fetchSemanticsNodes().size,
                )
                showDownloadIcon = true
                composeRule.waitForIdle()
            }

            callback("onFileClick") {
                composeRule.onNodeWithText(FIRST_FILE).performClick()
                composeRule.waitForIdle()
                assertEquals("the first card is index 0", 0, clickedIndex)
            }

            callback("onDownloadClick") {
                composeRule.onAllNodesWithContentDescription("Download file")[1].performClick()
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

        println("  [files compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [files] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("only textFormatters is waived", 1, cov.waived)
    }

    /** Takes effect only through the caption's markdown/mention pipeline, covered where that is. */
    private fun waivedProps(): List<Prop> = listOf(
        Prop(OWNER, "textFormatters", PropKind.VALUE, waived = true),
    )
}
