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
import com.cometchat.uikit.compose.presentation.messagecomposer.style.CometChatAttachmentTrayStyle
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
 * Property (prop-matrix) layer for [CometChatAttachmentTray].
 *
 * Nine integrator params, `modifier` excluded by [Denominator], leaving eight — and
 * nothing is waived. The tray is fully stateless and every one of its params is directly
 * observable, so a waiver here would be an admission that a test was missing rather than
 * that a prop was unobservable.
 *
 * The fixture is a three-tile tray whose statuses differ on purpose (uploading, done,
 * failed), because the five callbacks are not reachable from a single status: cancel needs
 * an in-flight tile, remove and click need a settled one, retry needs a failed one, and
 * rejection needs a tile the SDK refused. One mixed tray covers four of them in a single
 * composition; the rejected case swaps the list for its own entry.
 *
 * `style` and `tileStyle` are both read from painted pixels, which is the only honest way
 * to tell them apart — the tray's own styling is a background colour behind tiles that
 * paint their own, so asserting on structure could not distinguish "the tray applied it"
 * from "a tile did".
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAttachmentTrayComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatAttachmentTray"
        const val UPLOADING_LABEL = "Uploading…"
        const val RETRY_LABEL = "Tap to retry"
        const val REJECTED_LABEL = "Upload failed"
        const val MAGENTA_ARGB: Int = 0xFFFF00FF.toInt()
        const val CYAN_ARGB: Int = 0xFF00FFFF.toInt()
        val MAGENTA = androidx.compose.ui.graphics.Color(0xFFFF00FF)
        val CYAN = androidx.compose.ui.graphics.Color(0xFF00FFFF)
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
        fileId = "file_$name",
        name = name,
        size = 1_000_000L,
        mimeType = "image/jpeg",
        category = CometChatConstants.MESSAGE_TYPE_IMAGE,
        status = status,
    )

    private fun description(name: String, statusLabel: String) = "$name, $statusLabel"

    private fun tapBody(name: String, statusLabel: String) {
        composeRule.onNodeWithContentDescription(description(name, statusLabel)).performClick()
        composeRule.waitForIdle()
    }

    /** The corner badge is a small circle at the top-end corner of a tile. */
    private fun tapCornerBadge(name: String, statusLabel: String) {
        composeRule.onNodeWithContentDescription(description(name, statusLabel))
            .performTouchInput { click(percentOffset(0.95f, 0.08f)) }
        composeRule.waitForIdle()
    }

    @Test
    fun attachmentTray_propMatrix_coversEveryObservableProp() {
        val busy = tile("busy.jpg", AttachmentUploadStatus.UPLOADING)
        val done = tile("done.jpg", AttachmentUploadStatus.DONE)
        val failed = tile("failed.jpg", AttachmentUploadStatus.FAILED)
        val rejected = tile("toobig.jpg", AttachmentUploadStatus.REJECTED)

        var tiles by mutableStateOf(listOf(busy, done, failed))
        var trayStyleOverride by mutableStateOf<CometChatAttachmentTrayStyle?>(null)
        var tileStyleOverride by mutableStateOf<CometChatAttachmentTileStyle?>(null)

        // Both default() factories are @Composable — they read CometChatTheme — so they
        // can only be evaluated inside the composition. Captured here for the entries
        // below to copy() from.
        var defaultTrayStyle: CometChatAttachmentTrayStyle? = null
        var defaultTileStyle: CometChatAttachmentTileStyle? = null

        var cancelled: AttachmentUploadTile? = null
        var removed: AttachmentUploadTile? = null
        var retried: AttachmentUploadTile? = null
        var clicked: AttachmentUploadTile? = null
        var rejectedTile: AttachmentUploadTile? = null

        composeRule.setContent {
            CometChatTheme {
                defaultTrayStyle = CometChatAttachmentTrayStyle.default()
                defaultTileStyle = CometChatAttachmentTileStyle.default()
                CometChatAttachmentTray(
                    tiles = tiles,
                    style = trayStyleOverride ?: defaultTrayStyle!!,
                    tileStyle = tileStyleOverride ?: defaultTileStyle!!,
                    onCancelTile = { cancelled = it },
                    onRemoveTile = { removed = it },
                    onRetryTile = { retried = it },
                    onTileClick = { clicked = it },
                    onRejectedTile = { rejectedTile = it },
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("tiles") {
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription(description("done.jpg", ""))
                    .assertIsDisplayed()
                assertEquals(
                    "one tile per staged attachment",
                    1,
                    composeRule.onAllNodesWithContentDescription(
                        description("busy.jpg", UPLOADING_LABEL),
                    ).fetchSemanticsNodes().size,
                )
                // And the list is live: swapping it re-renders the strip.
                tiles = listOf(done)
                composeRule.waitForIdle()
                assertEquals(
                    "removing a tile from the list should remove it from the tray",
                    0,
                    composeRule.onAllNodesWithContentDescription(
                        description("busy.jpg", UPLOADING_LABEL),
                    ).fetchSemanticsNodes().size,
                )
                tiles = listOf(busy, done, failed)
                composeRule.waitForIdle()
            }

            value("style") {
                trayStyleOverride = defaultTrayStyle!!.copy(backgroundColor = MAGENTA)
                composeRule.waitForIdle()
                assertTrue(
                    "the tray's own backgroundColor should be on screen",
                    MAGENTA_ARGB in paintedColours(),
                )
                trayStyleOverride = null
                composeRule.waitForIdle()
            }

            value("tileStyle") {
                // Forwarded to every tile: the placeholder is the media tile's background
                // before an upload resolves, and no image loads here, so it is guaranteed
                // to reach the screen.
                tileStyleOverride = defaultTileStyle!!.copy(placeholderColor = CYAN)
                composeRule.waitForIdle()
                assertTrue(
                    "tileStyle should reach the tiles, not stop at the tray",
                    CYAN_ARGB in paintedColours(),
                )
                tileStyleOverride = null
                composeRule.waitForIdle()
            }

            callback("onCancelTile") {
                tapCornerBadge("busy.jpg", UPLOADING_LABEL)
                assertEquals("the in-flight tile is the one cancelled", busy, cancelled)
            }

            callback("onRemoveTile") {
                tapCornerBadge("done.jpg", "")
                assertEquals("the settled tile is the one removed", done, removed)
            }

            callback("onRetryTile") {
                tapBody("failed.jpg", RETRY_LABEL)
                assertEquals("the failed tile is the one retried", failed, retried)
            }

            callback("onTileClick") {
                tapBody("done.jpg", "")
                assertEquals("the uploaded tile is the one opened", done, clicked)
            }

            callback("onRejectedTile") {
                // Rejection needs a tile the SDK refused, which the standing fixture has
                // no reason to carry — swap one in for this entry alone.
                tiles = listOf(rejected)
                composeRule.waitForIdle()
                tapBody("toobig.jpg", REJECTED_LABEL)
                assertEquals(rejected, rejectedTile)
                tiles = listOf(busy, done, failed)
                composeRule.waitForIdle()
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [attachment tray prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [tray] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("the tray has nothing worth waiving", 0, cov.waived)
    }
}
