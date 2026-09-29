package com.cometchat.uikit.compose.presentation.messagecomposer.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.compose.presentation.messagecomposer.style.CometChatAttachmentTileStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.models.AttachmentUploadStatus
import com.cometchat.uikit.core.models.AttachmentUploadTile
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
 * Property (prop-matrix) layer for [CometChatAttachmentTile].
 *
 * Eight integrator params, `modifier` excluded by [Denominator], leaving seven — and
 * nothing waived. Every param of this component is reachable; the only thing that makes
 * it awkward is that four of the five callbacks are gated on a *status*, so the entries
 * below drive the tile through the upload lifecycle rather than tapping one fixture five
 * times.
 *
 * `tile` is therefore covered twice over: once as "the name and status reach the screen",
 * and again implicitly by every callback entry, each of which only becomes reachable
 * because the tile's status says so.
 *
 * On tapping: the component collapses itself with `clearAndSetSemantics`, so its
 * affordances are not individually queryable and are reached positionally instead — the
 * corner badge sits at the top-end corner, the body under the centre. The reasoning, and
 * why the two taps validate each other, is in the functional test alongside this one.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAttachmentTileComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatAttachmentTile"
        const val NAME = "holiday.jpg"
        const val UPLOADING_LABEL = "Uploading…"
        const val RETRY_LABEL = "Tap to retry"
        const val REJECTED_LABEL = "Upload failed"
        const val MAGENTA_ARGB: Int = 0xFFFF00FF.toInt()
        val MAGENTA = androidx.compose.ui.graphics.Color(0xFFFF00FF)
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

    private fun tile(name: String, status: AttachmentUploadStatus) = AttachmentUploadTile(
        fileId = "batch_1",
        name = name,
        size = 2_400_000L,
        mimeType = "image/jpeg",
        category = CometChatConstants.MESSAGE_TYPE_IMAGE,
        status = status,
    )

    private fun description(name: String, statusLabel: String) = "$name, $statusLabel"

    private fun tapBody(statusLabel: String) {
        composeRule.onNodeWithContentDescription(description(NAME, statusLabel)).performClick()
        composeRule.waitForIdle()
    }

    private fun tapCornerBadge(statusLabel: String) {
        composeRule.onNodeWithContentDescription(description(NAME, statusLabel))
            .performTouchInput { click(percentOffset(0.95f, 0.08f)) }
        composeRule.waitForIdle()
    }

    @Test
    fun attachmentTile_propMatrix_coversEveryObservableProp() {
        var current by mutableStateOf(tile(NAME, AttachmentUploadStatus.UPLOADING))
        var styleOverride by mutableStateOf<CometChatAttachmentTileStyle?>(null)

        // default() is @Composable (it reads CometChatTheme), so it can only be evaluated
        // inside the composition; captured here for the style entry to copy() from.
        var defaultStyle: CometChatAttachmentTileStyle? = null

        var cancelled = false
        var removed = false
        var retried = false
        var clicked = false
        var rejected = false

        composeRule.setContent {
            CometChatTheme {
                defaultStyle = CometChatAttachmentTileStyle.default()
                CometChatAttachmentTile(
                    tile = current,
                    style = styleOverride ?: defaultStyle!!,
                    onCancel = { cancelled = true },
                    onRemove = { removed = true },
                    onRetry = { retried = true },
                    onClick = { clicked = true },
                    onRejected = { rejected = true },
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("tile") {
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription(description(NAME, UPLOADING_LABEL))
                    .assertIsDisplayed()

                // The tile is the whole input: changing it changes what is announced,
                // and a CANCELLED one removes the component from the tree entirely.
                current = tile(NAME, AttachmentUploadStatus.CANCELLED)
                composeRule.waitForIdle()
                assertEquals(
                    "a cancelled tile renders nothing at all",
                    0,
                    composeRule.onAllNodesWithContentDescription(NAME, substring = true)
                        .fetchSemanticsNodes().size,
                )
                current = tile(NAME, AttachmentUploadStatus.UPLOADING)
                composeRule.waitForIdle()
            }

            value("style") {
                // The corner badge is present in every status, so its background is the
                // one colour guaranteed on screen whatever the lifecycle is doing.
                styleOverride = defaultStyle!!.copy(cornerBadgeBackgroundColor = MAGENTA)
                composeRule.waitForIdle()
                assertTrue(
                    "the supplied cornerBadgeBackgroundColor should be on screen",
                    MAGENTA_ARGB in paintedColours(),
                )
                styleOverride = null
                composeRule.waitForIdle()
            }

            callback("onCancel") {
                // Only reachable while in flight — the badge means Remove in every other
                // status.
                current = tile(NAME, AttachmentUploadStatus.UPLOADING)
                composeRule.waitForIdle()
                tapCornerBadge(UPLOADING_LABEL)
                assertTrue("the badge cancels an in-flight upload", cancelled)
            }

            callback("onRemove") {
                current = tile(NAME, AttachmentUploadStatus.DONE)
                composeRule.waitForIdle()
                tapCornerBadge("")
                assertTrue("the badge removes a settled tile", removed)
            }

            callback("onRetry") {
                current = tile(NAME, AttachmentUploadStatus.FAILED)
                composeRule.waitForIdle()
                tapBody(RETRY_LABEL)
                assertTrue("the body of a failed tile retries it", retried)
            }

            callback("onClick") {
                current = tile(NAME, AttachmentUploadStatus.DONE)
                composeRule.waitForIdle()
                tapBody("")
                assertTrue("the body of an uploaded tile opens it", clicked)
            }

            callback("onRejected") {
                current = tile(NAME, AttachmentUploadStatus.REJECTED)
                composeRule.waitForIdle()
                tapBody(REJECTED_LABEL)
                assertTrue("a rejected tile surfaces its reason", rejected)
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [attachment tile prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [tile] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("every param of this tile is reachable", 0, cov.waived)
    }
}
