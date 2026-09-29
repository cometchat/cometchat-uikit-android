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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Attachment
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatFileBubbleStyle
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
 * Property layer for the single [CometChatFileBubble], covering both overloads.
 *
 * The message overload is what the renderer uses with multi-attachments off; the
 * attachments overload is public API for hosts that already hold the list, and it is
 * the only way to reach `showDownloadAllButton` and `downloadAllButtonText`. Both are
 * swept here, switching between them mid-test, so nothing is waived.
 *
 * Every branch passes a style with a resolved corner radius, as the renderer does.
 * The bubble's own default style leaves it unset and crashes on the first draw —
 * pinned in [CometChatFileBubbleDefaultStyleTest].
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatFileBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatFileBubble"
        const val FILE_MIME = "application/pdf"
        const val FILE_EXT = "pdf"
        const val FIRST_FILE = "media_1.pdf"
        const val CUSTOM_DOWNLOAD_ALL = "Grab everything"
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
    fun fileBubble_propMatrix_coversEveryProp() {
        var alignment by mutableStateOf(UIKitConstants.MessageBubbleAlignment.LEFT)
        var styleOverride by mutableStateOf<CometChatFileBubbleStyle?>(null)
        var showDownloadIcon by mutableStateOf(true)
        var useAttachments by mutableStateOf(false)
        var showDownloadAll by mutableStateOf(true)
        var downloadAllText by mutableStateOf("Download All")
        var clickedIndex: Int? = null
        var downloadedIndex: Int? = null
        var downloadAllFired = false
        var longClicked = false
        var incoming: CometChatFileBubbleStyle? = null
        var outgoing: CometChatFileBubbleStyle? = null

        val message = MockFactory.createMediaMessage(
            count = 2,
            type = CometChatConstants.MESSAGE_TYPE_FILE,
            mimeType = FILE_MIME,
            extension = FILE_EXT,
        )
        val attachments: List<Attachment> = listOf(
            MockFactory.createAttachment(1, FILE_MIME, FILE_EXT),
            MockFactory.createAttachment(2, FILE_MIME, FILE_EXT),
        )

        composeRule.setContent {
            CometChatTheme {
                // The renderer merges the bubble style in before calling; without a
                // resolved radius the shape is NaN and the first draw throws.
                incoming = CometChatFileBubbleStyle.incoming().copy(cornerRadius = 12.dp)
                outgoing = CometChatFileBubbleStyle.outgoing().copy(cornerRadius = 12.dp)
                val override = styleOverride
                when {
                    useAttachments -> CometChatFileBubble(
                        attachments = attachments,
                        alignment = alignment,
                        style = override ?: incoming!!,
                        showDownloadIcon = showDownloadIcon,
                        showDownloadAllButton = showDownloadAll,
                        downloadAllButtonText = downloadAllText,
                        onFileClick = { clickedIndex = it },
                        onDownloadClick = { downloadedIndex = it },
                        onDownloadAllClick = { downloadAllFired = true },
                        onLongClick = { longClicked = true },
                    )
                    override != null -> CometChatFileBubble(
                        message = message,
                        alignment = alignment,
                        style = override,
                        showDownloadIcon = showDownloadIcon,
                        onFileClick = { clickedIndex = it },
                        onDownloadClick = { downloadedIndex = it },
                        onDownloadAllClick = { downloadAllFired = true },
                        onLongClick = { longClicked = true },
                    )
                    else -> CometChatFileBubble(
                        message = message,
                        alignment = alignment,
                        style = if (alignment == UIKitConstants.MessageBubbleAlignment.RIGHT) outgoing!! else incoming!!,
                        showDownloadIcon = showDownloadIcon,
                        onFileClick = { clickedIndex = it },
                        onDownloadClick = { downloadedIndex = it },
                        onDownloadAllClick = { downloadAllFired = true },
                        onLongClick = { longClicked = true },
                    )
                }
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("message") {
                composeRule.waitForIdle()
                composeRule.onNodeWithText(FIRST_FILE).assertIsDisplayed()
            }

            value("attachments") {
                useAttachments = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(FIRST_FILE).assertIsDisplayed()
                composeRule.onNodeWithText("media_2.pdf").assertIsDisplayed()
            }

            value("showDownloadAllButton") {
                // Only the attachments overload exposes it; still on that overload here.
                composeRule.waitForIdle()
                composeRule.onNodeWithText("Download All").assertIsDisplayed()
                showDownloadAll = false
                composeRule.waitForIdle()
                assertEquals(
                    "switching it off should remove the button",
                    0,
                    composeRule.onAllNodesWithText("Download All").fetchSemanticsNodes().size,
                )
                showDownloadAll = true
                composeRule.waitForIdle()
            }

            value("downloadAllButtonText") {
                downloadAllText = CUSTOM_DOWNLOAD_ALL
                composeRule.waitForIdle()
                composeRule.onNodeWithText(CUSTOM_DOWNLOAD_ALL).assertIsDisplayed()
            }

            callback("onDownloadAllClick") {
                composeRule.onNodeWithText(CUSTOM_DOWNLOAD_ALL).performClick()
                composeRule.waitForIdle()
                assertTrue("download-all should reach the integrator", downloadAllFired)
                downloadAllText = "Download All"
                useAttachments = false
                composeRule.waitForIdle()
            }

            // Both sides are driven through the same resolved-style path, so this
            // measures the palette difference rather than the default expression.
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
                // backgroundColor is the container's job on this bubble, so assert on
                // something it paints itself.
                styleOverride = incoming!!.copy(fileIconBackgroundColor = MAGENTA)
                composeRule.waitForIdle()
                assertTrue("the supplied fileIconBackgroundColor should be on screen", MAGENTA_ARGB in paintedColours())
                assertNotEquals("incoming() and outgoing() should differ", incoming, outgoing)
                styleOverride = null
                composeRule.waitForIdle()
            }

            value("showDownloadIcon") {
                composeRule.waitForIdle()
                val before = composeRule.onAllNodesWithContentDescription("Download file").fetchSemanticsNodes().size
                assertTrue("expected download affordances, was $before", before > 0)
                showDownloadIcon = false
                composeRule.waitForIdle()
                assertEquals(
                    0,
                    composeRule.onAllNodesWithContentDescription("Download file").fetchSemanticsNodes().size,
                )
                showDownloadIcon = true
                composeRule.waitForIdle()
            }

            callback("onFileClick") {
                composeRule.onNodeWithText(FIRST_FILE).performClick()
                composeRule.waitForIdle()
                assertEquals("the first file is index 0", 0, clickedIndex)
            }

            callback("onDownloadClick") {
                composeRule.onAllNodesWithContentDescription("Download file")[1].performClick()
                composeRule.waitForIdle()
                assertEquals("the second file reports index 1", 1, downloadedIndex)
            }

            callback("onLongClick") {
                composeRule.onNodeWithText(FIRST_FILE).performTouchInput { longClick() }
                composeRule.waitForIdle()
                assertTrue("a long press should reach the integrator", longClicked)
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered }.map { it.name }

        println("  [file compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [file] NOT covered: $uncovered")

        assertEquals("every prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("nothing waived on this bubble", 0, cov.waived)
    }
}
