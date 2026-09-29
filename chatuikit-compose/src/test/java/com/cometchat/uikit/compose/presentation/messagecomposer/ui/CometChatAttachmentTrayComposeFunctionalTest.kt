package com.cometchat.uikit.compose.presentation.messagecomposer.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.models.AttachmentUploadStatus
import com.cometchat.uikit.core.models.AttachmentUploadTile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Functional layer for [CometChatAttachmentTray].
 *
 * The tray is the composer's staging strip. It is deliberately stateless — the hosting
 * composer owns the tile list — so what it owns is narrow and worth pinning precisely:
 * it disappears entirely when there is nothing staged, it renders one
 * [CometChatAttachmentTile] per entry, and it forwards each per-tile intent back up
 * *tagged with the tile it came from*.
 *
 * That last part is the real risk. Every callback takes the tile as its argument, and a
 * lambda capturing the wrong loop variable would still compile, still fire, and still look
 * right on screen — the user would simply find that cancelling one upload removed a
 * different one. So these tests never assert merely that a callback fired; they assert
 * *which* tile arrived with it.
 *
 * Tiles are addressed by content description ("name, status"), since
 * [CometChatAttachmentTile] collapses itself to a single semantics node — see that
 * component's own test for why the affordances have to be tapped positionally.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAttachmentTrayComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val UPLOADING_LABEL = "Uploading…"
        const val RETRY_LABEL = "Tap to retry"
        const val REJECTED_LABEL = "Upload failed"
    }

    private class Sink {
        var cancelled: AttachmentUploadTile? = null
        var removed: AttachmentUploadTile? = null
        var retried: AttachmentUploadTile? = null
        var clicked: AttachmentUploadTile? = null
        var rejected: AttachmentUploadTile? = null
    }

    private fun tile(
        name: String,
        status: AttachmentUploadStatus = AttachmentUploadStatus.DONE,
        category: String = CometChatConstants.MESSAGE_TYPE_IMAGE,
    ) = AttachmentUploadTile(
        fileId = "file_$name",
        name = name,
        size = 1_000_000L,
        mimeType = "image/jpeg",
        category = category,
        status = status,
    )

    private fun render(tiles: List<AttachmentUploadTile>): Sink {
        val sink = Sink()
        composeRule.setContent {
            CometChatTheme {
                CometChatAttachmentTray(
                    tiles = tiles,
                    onCancelTile = { sink.cancelled = it },
                    onRemoveTile = { sink.removed = it },
                    onRetryTile = { sink.retried = it },
                    onTileClick = { sink.clicked = it },
                    onRejectedTile = { sink.rejected = it },
                )
            }
        }
        composeRule.waitForIdle()
        return sink
    }

    private fun description(name: String, statusLabel: String) = "$name, $statusLabel"

    private fun tileNodes(name: String, statusLabel: String) =
        composeRule.onAllNodesWithContentDescription(description(name, statusLabel))
            .fetchSemanticsNodes().size

    private fun tapBody(name: String, statusLabel: String) {
        composeRule.onNodeWithContentDescription(description(name, statusLabel)).performClick()
        composeRule.waitForIdle()
    }

    private fun tapCornerBadge(name: String, statusLabel: String) {
        composeRule.onNodeWithContentDescription(description(name, statusLabel))
            .performTouchInput { click(percentOffset(0.95f, 0.08f)) }
        composeRule.waitForIdle()
    }

    // ── the empty tray is absent, not blank ─────────────────────────────────

    @Test
    fun withNothingStaged_theTrayRendersNoTiles() {
        // An early return rather than an empty row: a zero-height strip would still push
        // the composer's layout around.
        render(emptyList())
        assertEquals(
            "an empty tray should contribute no tile nodes",
            0,
            composeRule.onAllNodesWithContentDescription(", ", substring = true)
                .fetchSemanticsNodes().size,
        )
    }

    // ── one tile per staged attachment ──────────────────────────────────────

    @Test
    fun oneStagedAttachmentRendersOneTile() {
        render(listOf(tile("a.jpg")))
        composeRule.onNodeWithContentDescription(description("a.jpg", "")).assertIsDisplayed()
    }

    @Test
    fun everyStagedAttachmentGetsItsOwnTile() {
        render(listOf(tile("a.jpg"), tile("b.jpg"), tile("c.jpg")))
        assertEquals(1, tileNodes("a.jpg", ""))
        assertEquals(1, tileNodes("b.jpg", ""))
        assertEquals(1, tileNodes("c.jpg", ""))
    }

    @Test
    fun tilesOfDifferentKindsCoexistInOneTray() {
        // The lazy row assigns a distinct contentType per kind so it recycles them
        // separately; a mixed tray is the case that exercises it.
        render(
            listOf(
                tile("a.jpg"),
                tile("voice.m4a", category = CometChatConstants.MESSAGE_TYPE_AUDIO),
                tile("contract.pdf", category = CometChatConstants.MESSAGE_TYPE_FILE),
            ),
        )
        assertEquals(1, tileNodes("a.jpg", ""))
        assertEquals(1, tileNodes("voice.m4a", ""))
        assertEquals(1, tileNodes("contract.pdf", ""))
    }

    @Test
    fun aCancelledTileTakesUpNoRoomInTheTray() {
        // The tray does not filter; the tile itself returns early. The effect has to hold
        // through the tray either way, or a cancelled upload would leave a hole behind.
        render(
            listOf(
                tile("a.jpg"),
                tile("gone.jpg", status = AttachmentUploadStatus.CANCELLED),
                tile("c.jpg"),
            ),
        )
        assertEquals(1, tileNodes("a.jpg", ""))
        assertEquals(1, tileNodes("c.jpg", ""))
        assertEquals(
            "a cancelled tile should render nothing",
            0,
            composeRule.onAllNodesWithContentDescription("gone.jpg", substring = true)
                .fetchSemanticsNodes().size,
        )
    }

    // ── each intent comes back tagged with the tile it belongs to ───────────

    @Test
    fun tappingATileReportsThatTileAndNoOther() {
        val tiles = listOf(tile("a.jpg"), tile("b.jpg"), tile("c.jpg"))
        val sink = render(tiles)

        tapBody("b.jpg", "")

        assertEquals("the middle tile is the one that was tapped", tiles[1], sink.clicked)
    }

    @Test
    fun cancellingReportsTheInFlightTileItBelongsTo() {
        val tiles = listOf(
            tile("a.jpg", status = AttachmentUploadStatus.UPLOADING),
            tile("b.jpg", status = AttachmentUploadStatus.UPLOADING),
        )
        val sink = render(tiles)

        tapCornerBadge("b.jpg", UPLOADING_LABEL)

        assertEquals(tiles[1], sink.cancelled)
        assertNull("cancelling one upload must not remove another", sink.removed)
    }

    @Test
    fun removingReportsTheSettledTileItBelongsTo() {
        val tiles = listOf(tile("a.jpg"), tile("b.jpg"))
        val sink = render(tiles)

        tapCornerBadge("a.jpg", "")

        assertEquals(tiles[0], sink.removed)
        assertNull(sink.cancelled)
    }

    @Test
    fun retryingReportsTheFailedTileItBelongsTo() {
        val tiles = listOf(
            tile("ok.jpg"),
            tile("bad.jpg", status = AttachmentUploadStatus.FAILED),
        )
        val sink = render(tiles)

        tapBody("bad.jpg", RETRY_LABEL)

        assertEquals(tiles[1], sink.retried)
        assertNull("the healthy tile beside it must be untouched", sink.clicked)
    }

    @Test
    fun aRejectedTileReportsItselfToTheRejectionHandler() {
        val tiles = listOf(
            tile("ok.jpg"),
            tile("toobig.jpg", status = AttachmentUploadStatus.REJECTED),
        )
        val sink = render(tiles)

        tapBody("toobig.jpg", REJECTED_LABEL)

        assertEquals(tiles[1], sink.rejected)
        assertNull("a rejection is not a retry", sink.retried)
    }

    @Test
    fun aTrayOfMixedStatusesRoutesEachTileByItsOwnStatus() {
        // The routing lives in the tile, but the tray is where a mis-keyed lambda would
        // show up: three tiles, three different destinations, all in one composition.
        val tiles = listOf(
            tile("done.jpg"),
            tile("failed.jpg", status = AttachmentUploadStatus.FAILED),
            tile("busy.jpg", status = AttachmentUploadStatus.UPLOADING),
        )
        val sink = render(tiles)

        tapBody("done.jpg", "")
        tapBody("failed.jpg", RETRY_LABEL)
        tapCornerBadge("busy.jpg", UPLOADING_LABEL)

        assertEquals(tiles[0], sink.clicked)
        assertEquals(tiles[1], sink.retried)
        assertEquals(tiles[2], sink.cancelled)
        assertNull("nothing here should have been removed", sink.removed)
    }
}
