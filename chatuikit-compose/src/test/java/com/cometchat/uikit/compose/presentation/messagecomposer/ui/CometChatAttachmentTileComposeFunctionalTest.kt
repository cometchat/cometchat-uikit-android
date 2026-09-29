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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Functional layer for [CometChatAttachmentTile].
 *
 * The tile is one staged attachment in the composer tray, and its whole job is to turn an
 * [AttachmentUploadStatus] into the right recovery affordance. Five statuses, two tap
 * targets, and the mapping between them is the component:
 *
 * ```
 *              body tap        corner badge
 *   UPLOADING  (inert)         onCancel
 *   DONE       onClick         onRemove
 *   FAILED     onRetry         onRemove
 *   REJECTED   onRejected      onRemove
 *   CANCELLED  renders nothing at all
 * ```
 *
 * Two of those rows are easy to get wrong and expensive if you do: tapping the body of an
 * in-flight tile must do nothing (there is no preview to open yet), and a rejected tile
 * must reach `onRejected` rather than `onRetry`, because the SDK has already said this
 * file will never upload and offering a retry would loop the user forever.
 *
 * **On how these tests click.** The tile wraps itself in `clearAndSetSemantics`, which
 * erases every descendant's semantics and replaces them with a single content description.
 * So from a test's point of view a tile is *one node* — the corner badge's own "Cancel" /
 * "Remove" description is not queryable, and neither are the bodies. Affordances are
 * therefore reached positionally: the badge is a 22dp circle pinned to the top-end corner,
 * so a tap at 95%/8% of the node lands on it for every tile width, while `performClick()`
 * takes the node centre and lands on the body. Anything that queries the badge by
 * description would silently match nothing and pass, which is why the assertions below are
 * on the callbacks and never on an affordance being "present".
 *
 * The two positional taps check each other: if the centre tap strayed onto the badge,
 * `tappingATileThatIsStillUploadingDoesNothing` would see a cancel and fail, and if the
 * corner tap missed the badge, `theBadgeCancelsWhileTheUploadIsInFlight` would see nothing
 * and fail.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAttachmentTileComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val NAME = "holiday.jpg"

        // Status labels the description is built from, per statusLabel().
        const val UPLOADING_LABEL = "Uploading…"
        const val RETRY_LABEL = "Tap to retry"
        const val REJECTED_LABEL = "Upload failed"
    }

    private class Sink {
        var cancelled = false
        var removed = false
        var retried = false
        var clicked = false
        var rejected = false

        /** The set of callbacks that fired, so a test can pin what stayed silent too. */
        fun firedOnly(vararg expected: String): Boolean {
            val fired = buildSet {
                if (cancelled) add("cancel")
                if (removed) add("remove")
                if (retried) add("retry")
                if (clicked) add("click")
                if (rejected) add("rejected")
            }
            return fired == expected.toSet()
        }
    }

    private fun tile(
        status: AttachmentUploadStatus,
        category: String = CometChatConstants.MESSAGE_TYPE_IMAGE,
        name: String = NAME,
    ) = AttachmentUploadTile(
        fileId = "batch_1",
        name = name,
        size = 2_400_000L,
        mimeType = "image/jpeg",
        category = category,
        status = status,
    )

    private fun render(tile: AttachmentUploadTile): Sink {
        val sink = Sink()
        composeRule.setContent {
            CometChatTheme {
                CometChatAttachmentTile(
                    tile = tile,
                    onCancel = { sink.cancelled = true },
                    onRemove = { sink.removed = true },
                    onRetry = { sink.retried = true },
                    onClick = { sink.clicked = true },
                    onRejected = { sink.rejected = true },
                )
            }
        }
        composeRule.waitForIdle()
        return sink
    }

    private fun description(name: String, statusLabel: String) = "$name, $statusLabel"

    private fun tapBody(name: String, statusLabel: String) {
        composeRule.onNodeWithContentDescription(description(name, statusLabel)).performClick()
        composeRule.waitForIdle()
    }

    /** The badge is a small circle at the top-end corner; 95%/8% is inside it at any width. */
    private fun tapCornerBadge(name: String, statusLabel: String) {
        composeRule.onNodeWithContentDescription(description(name, statusLabel))
            .performTouchInput { click(percentOffset(0.95f, 0.08f)) }
        composeRule.waitForIdle()
    }

    // ── the description ─────────────────────────────────────────────────────

    @Test
    fun theTileAnnouncesItsFileNameAndStatus() {
        render(tile(AttachmentUploadStatus.UPLOADING))
        composeRule.onNodeWithContentDescription(description(NAME, UPLOADING_LABEL))
            .assertIsDisplayed()
    }

    @Test
    fun aFailedTileAnnouncesThatItCanBeRetried() {
        render(tile(AttachmentUploadStatus.FAILED))
        composeRule.onNodeWithContentDescription(description(NAME, RETRY_LABEL))
            .assertIsDisplayed()
    }

    @Test
    fun aRejectedTileAnnouncesFailureWithoutOfferingARetry() {
        // Deliberately a different label to FAILED: the two look alike on screen but only
        // one of them is worth tapping.
        render(tile(AttachmentUploadStatus.REJECTED))
        composeRule.onNodeWithContentDescription(description(NAME, REJECTED_LABEL))
            .assertIsDisplayed()
        assertEquals(
            "a rejected tile must not also announce itself as retryable",
            0,
            composeRule.onAllNodesWithContentDescription(description(NAME, RETRY_LABEL))
                .fetchSemanticsNodes().size,
        )
    }

    @Test
    fun anUploadedTileHasAnEmptyStatusHalfInItsDescription() {
        // statusLabel() returns "" for DONE and the template is "%1$s, %2$s", so the
        // announcement ends on a dangling separator: "holiday.jpg, ". Pinned as it is
        // rather than as it ought to be — worth tidying in the template, but silently
        // asserting the tidy version here would just hide it.
        render(tile(AttachmentUploadStatus.DONE))
        composeRule.onNodeWithContentDescription("$NAME, ").assertIsDisplayed()
    }

    // ── body taps: one status, one destination ──────────────────────────────

    @Test
    fun tappingAnUploadedTileOpensIt() {
        val sink = render(tile(AttachmentUploadStatus.DONE))
        tapBody(NAME, "")
        assertTrue(sink.firedOnly("click"))
    }

    @Test
    fun tappingAFailedTileRetriesIt() {
        val sink = render(tile(AttachmentUploadStatus.FAILED))
        tapBody(NAME, RETRY_LABEL)
        assertTrue(sink.firedOnly("retry"))
    }

    @Test
    fun tappingARejectedTileSurfacesTheReasonRatherThanRetrying() {
        // The SDK has already refused this file; a retry would loop forever.
        val sink = render(tile(AttachmentUploadStatus.REJECTED))
        tapBody(NAME, REJECTED_LABEL)
        assertTrue(sink.firedOnly("rejected"))
        assertFalse("a rejected tile must never reach onRetry", sink.retried)
    }

    @Test
    fun tappingATileThatIsStillUploadingDoesNothing() {
        // There is nothing to open yet, so the body is not clickable at all.
        val sink = render(tile(AttachmentUploadStatus.UPLOADING))
        tapBody(NAME, UPLOADING_LABEL)
        assertTrue("no callback should fire from an in-flight body tap", sink.firedOnly())
    }

    // ── the corner badge: cancel while in flight, remove once settled ───────

    @Test
    fun theBadgeCancelsWhileTheUploadIsInFlight() {
        val sink = render(tile(AttachmentUploadStatus.UPLOADING))
        tapCornerBadge(NAME, UPLOADING_LABEL)
        assertTrue(sink.firedOnly("cancel"))
    }

    @Test
    fun theBadgeRemovesAnUploadedTile() {
        val sink = render(tile(AttachmentUploadStatus.DONE))
        tapCornerBadge(NAME, "")
        assertTrue(sink.firedOnly("remove"))
    }

    @Test
    fun theBadgeRemovesAFailedTileRatherThanRetryingIt() {
        // The badge and the body of a failed tile do different things; this is the pair
        // most likely to get crossed.
        val sink = render(tile(AttachmentUploadStatus.FAILED))
        tapCornerBadge(NAME, RETRY_LABEL)
        assertTrue(sink.firedOnly("remove"))
    }

    @Test
    fun theBadgeRemovesARejectedTile() {
        val sink = render(tile(AttachmentUploadStatus.REJECTED))
        tapCornerBadge(NAME, REJECTED_LABEL)
        assertTrue(sink.firedOnly("remove"))
    }

    // ── the cancelled tile is gone, not merely disabled ─────────────────────

    @Test
    fun aCancelledTileRendersNothingAtAll() {
        render(tile(AttachmentUploadStatus.CANCELLED))
        assertEquals(
            "a cancelled tile leaves the tray entirely",
            0,
            composeRule.onAllNodesWithContentDescription(NAME, substring = true)
                .fetchSemanticsNodes().size,
        )
    }

    // ── the three tile kinds ────────────────────────────────────────────────

    @Test
    fun anAudioAttachmentRendersItsOwnTileKind() {
        render(
            tile(
                AttachmentUploadStatus.DONE,
                category = CometChatConstants.MESSAGE_TYPE_AUDIO,
                name = "voice.m4a",
            ),
        )
        composeRule.onNodeWithContentDescription("voice.m4a, ").assertIsDisplayed()
    }

    @Test
    fun aDocumentRendersItsOwnTileKind() {
        render(
            tile(
                AttachmentUploadStatus.DONE,
                category = CometChatConstants.MESSAGE_TYPE_FILE,
                name = "contract.pdf",
            ),
        )
        composeRule.onNodeWithContentDescription("contract.pdf, ").assertIsDisplayed()
    }

    @Test
    fun aVideoIsAMediaTileLikeAnImage() {
        render(
            tile(
                AttachmentUploadStatus.DONE,
                category = CometChatConstants.MESSAGE_TYPE_VIDEO,
                name = "clip.mp4",
            ),
        )
        composeRule.onNodeWithContentDescription("clip.mp4, ").assertIsDisplayed()
    }

    @Test
    fun everyKindStillRoutesItsBadgeToRemove() {
        // The badge lives on the shared outer Box, not in the per-kind bodies, so it must
        // behave identically across all three.
        val sink = render(
            tile(
                AttachmentUploadStatus.DONE,
                category = CometChatConstants.MESSAGE_TYPE_FILE,
                name = "contract.pdf",
            ),
        )
        tapCornerBadge("contract.pdf", "")
        assertTrue(sink.firedOnly("remove"))
    }
}
