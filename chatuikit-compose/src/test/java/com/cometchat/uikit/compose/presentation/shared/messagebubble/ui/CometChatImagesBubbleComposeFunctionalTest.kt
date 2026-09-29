package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
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
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Instrumented layer for [CometChatImagesBubble].
 *
 * The bubble a multi-image message actually reaches. Nearly all of its behaviour is the
 * grid: how many tiles are drawn, which tile carries the "+N" overflow badge, and which
 * callback that badge fires instead of the ordinary one.
 *
 * The cap is the interesting part. Beyond four attachments the grid stops drawing tiles
 * and folds the remainder into a badge on the fourth — so the fourth tile stops being a
 * tile you can open and becomes the handle for "show me all of them". These tests pin
 * that swap, because it is the one place where tapping the same pixel means two
 * different things depending on how many attachments arrived.
 *
 * The single-tile and three-tile layouts take their own branches in `MediaGrid` (a
 * full-width tile rather than a half-width one), so both are exercised rather than
 * assumed to follow from the two-tile case.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatImagesBubbleComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val IMAGE_MIME = "image/jpeg"
        const val IMAGE_EXT = "jpg"

        /** `MediaGrid` draws at most this many tiles; the rest become the "+N" badge. */
        const val MAX_VISIBLE = 4
    }

    private fun images(count: Int, caption: String? = null): MediaMessage =
        MockFactory.createMediaMessage(
            count = count,
            type = CometChatConstants.MESSAGE_TYPE_IMAGE,
            mimeType = IMAGE_MIME,
            extension = IMAGE_EXT,
            caption = caption,
        )

    private fun render(
        message: MediaMessage,
        alignment: UIKitConstants.MessageBubbleAlignment = UIKitConstants.MessageBubbleAlignment.LEFT,
        caption: String? = message.caption,
        onMediaClick: ((Int, Attachment) -> Unit)? = null,
        onMoreClick: ((List<Attachment>) -> Unit)? = null,
        onLongClick: (() -> Unit)? = null,
    ) {
        composeRule.setContent {
            CometChatTheme {
                CometChatImagesBubble(
                    message = message,
                    alignment = alignment,
                    caption = caption,
                    onMediaClick = onMediaClick,
                    onMoreClick = onMoreClick,
                    onLongClick = onLongClick,
                )
            }
        }
        composeRule.waitForIdle()
    }

    /** Each tile takes its content description from the attachment's file name. */
    private fun tileCount(): Int = (1..12).count { i ->
        composeRule.onAllNodesWithContentDescription("media_$i.$IMAGE_EXT")
            .fetchSemanticsNodes().isNotEmpty()
    }

    private fun overflowBadges(text: String) =
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().size

    // ── the grid ────────────────────────────────────────────────────────────

    @Test
    fun oneImage_rendersASingleFullWidthTile() {
        render(images(1))
        assertEquals(1, tileCount())
        composeRule.onNodeWithContentDescription("media_1.$IMAGE_EXT").assertIsDisplayed()
    }

    @Test
    fun twoImages_renderBothTiles() {
        render(images(2))
        assertEquals(2, tileCount())
    }

    @Test
    fun threeImages_renderAllThree() {
        // Own branch in MediaGrid: one full-width tile above a row of two.
        render(images(3))
        assertEquals(3, tileCount())
    }

    @Test
    fun fourImages_fillTheGridWithNoOverflowBadge() {
        render(images(MAX_VISIBLE))
        assertEquals(MAX_VISIBLE, tileCount())
        assertEquals("a full grid is not an overflowing one", 0, overflowBadges("+1"))
    }

    // ── the overflow badge ──────────────────────────────────────────────────

    @Test
    fun fiveImages_capAtFourAndShowAPlusOneBadge() {
        render(images(5))
        assertEquals(MAX_VISIBLE, tileCount())
        composeRule.onNodeWithText("+1").assertIsDisplayed()
    }

    @Test
    fun theBadgeCountsTheHiddenImagesNotTheTotal() {
        render(images(7))
        assertEquals(MAX_VISIBLE, tileCount())
        composeRule.onNodeWithText("+3").assertIsDisplayed()
    }

    // ── caption ─────────────────────────────────────────────────────────────

    @Test
    fun aCaptionRendersBeneathTheGrid() {
        render(images(2, caption = "the whole roll"))
        composeRule.onNodeWithText("the whole roll").assertIsDisplayed()
    }

    @Test
    fun withoutACaption_theBubbleRendersNoTextAtAll() {
        // Asserting that one particular caption string is absent would assert nothing —
        // it was never supplied, so only the component inventing that exact literal
        // could fail it. An under-filled grid contributes no text of its own, so the
        // real claim is that there is no text node at all: a stray label, a leaked
        // placeholder or an overflow badge that should not be there all fail this.
        render(images(2))
        assertEquals(
            0,
            composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
                .fetchSemanticsNodes().size,
        )
    }

    // ── callbacks ───────────────────────────────────────────────────────────

    @Test
    fun tappingATileReportsItsIndexAndItsOwnAttachment() {
        var index: Int? = null
        var attachment: Attachment? = null
        val message = images(3)
        render(message, onMediaClick = { i, a -> index = i; attachment = a })

        composeRule.onNodeWithContentDescription("media_2.$IMAGE_EXT").performClick()
        composeRule.waitForIdle()

        assertEquals("the second tile is index 1", 1, index)
        assertEquals("media_2.$IMAGE_EXT", attachment?.fileName)
    }

    @Test
    fun theOverflowTileFiresOnMoreClickRatherThanOnMediaClick() {
        // The swap this bubble exists to get right: the fourth tile stops being a tile.
        var mediaIndex: Int? = null
        var all: List<Attachment>? = null
        render(images(7), onMediaClick = { i, _ -> mediaIndex = i }, onMoreClick = { all = it })

        composeRule.onNodeWithText("+3").performClick()
        composeRule.waitForIdle()

        assertNull("the overflow tile must not report itself as a media tap", mediaIndex)
        assertEquals("onMoreClick receives every attachment, not just the hidden ones", 7, all?.size)
    }

    @Test
    fun belowTheCapTheFourthTileIsStillAnOrdinaryTile() {
        var index: Int? = null
        var all: List<Attachment>? = null
        render(images(MAX_VISIBLE), onMediaClick = { i, _ -> index = i }, onMoreClick = { all = it })

        composeRule.onNodeWithContentDescription("media_4.$IMAGE_EXT").performClick()
        composeRule.waitForIdle()

        assertEquals(3, index)
        assertNull("no overflow means no onMoreClick", all)
    }

    @Test
    fun aLongPressOnATileReachesTheIntegrator() {
        var longPressed = false
        render(images(2), onLongClick = { longPressed = true })

        composeRule.onNodeWithContentDescription("media_1.$IMAGE_EXT")
            .performTouchInput { longClick() }
        composeRule.waitForIdle()

        assertTrue(longPressed)
    }

    // ── accessibility ───────────────────────────────────────────────────────

    @Test
    fun theBubbleAnnouncesHowManyImagesItHolds() {
        render(images(3))
        composeRule.onNodeWithContentDescription("Image message with 3 item(s)").assertIsDisplayed()
    }

    @Test
    fun theAnnouncedCountIsTheRealOneNotTheVisibleOne() {
        // Four tiles are drawn, but seven images arrived — a screen reader should be
        // told about the seven.
        render(images(7))
        composeRule.onNodeWithContentDescription("Image message with 7 item(s)").assertIsDisplayed()
    }

    // ── degenerate input ────────────────────────────────────────────────────

    @Test
    fun withNoAttachments_rendersNoTiles() {
        render(images(0))
        assertEquals(0, tileCount())
    }

    @Test
    fun withNoAttachments_aCaptionStillRenders() {
        // The grid returns early on an empty list; the caption is a sibling of it, so a
        // caption-only payload must not be swallowed with the grid.
        render(images(0, caption = "nothing came through"))
        composeRule.onNodeWithText("nothing came through").assertIsDisplayed()
    }

    // ── alignment ───────────────────────────────────────────────────────────

    @Test
    fun bothAlignmentsRenderTheSameGrid() {
        // Alignment changes the resolved style, never the tiles themselves.
        render(images(3), alignment = UIKitConstants.MessageBubbleAlignment.RIGHT)
        assertEquals(3, tileCount())
    }

    @Test
    fun anExplicitCaptionOverridesTheMessageCaption() {
        val message = images(2, caption = "from the message")
        render(message, caption = "from the integrator")

        composeRule.onNodeWithText("from the integrator").assertIsDisplayed()
        assertEquals(
            "the message's own caption should not also render",
            0,
            composeRule.onAllNodesWithText("from the message").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun theTilesAreBuiltFromTheMessagesMetadataAttachments() {
        // MockFactory delivers a multi-attachment payload through metadata.attachments,
        // which is the path resolveAttachments takes for this bubble. Reading the mime
        // type back off the tapped tile shows the grid parsed that payload rather than
        // falling through to the single-attachment property.
        var attachment: Attachment? = null
        render(images(2), onMediaClick = { _, a -> attachment = a })

        composeRule.onNodeWithContentDescription("media_1.$IMAGE_EXT").performClick()
        composeRule.waitForIdle()

        assertEquals(IMAGE_MIME, attachment?.fileMimeType)
        assertEquals("media_1.$IMAGE_EXT", attachment?.fileName)
    }
}
