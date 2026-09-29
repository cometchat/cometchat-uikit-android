package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
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
 * Instrumented layer for [CometChatVideosBubble].
 *
 * The grid itself is [CometChatImagesBubble]'s, shared through `MediaBubbleContent`, and
 * is pinned there. What is this bubble's own is the play badge: a video tile has to
 * advertise that it is a video before any frame has loaded.
 *
 * That timing is the whole point. A tile's thumbnail is fetched from the network by
 * `MediaMetadataRetriever`, so off-network — here, and on a real device for the first
 * moment of every tile — there is no frame and no duration chip, and the badge is the
 * only thing distinguishing a video from a grey square. These tests therefore assert on
 * the badge rather than on a thumbnail that will never arrive.
 *
 * The badge also interacts with the overflow rule: the fourth tile of an overflowing
 * grid shows "+N" *instead of* a badge, because it no longer opens a video. So a
 * seven-video message draws four tiles but only three play badges — pinned below,
 * since the obvious expectation of "one badge per tile" is wrong.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatVideosBubbleComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val VIDEO_MIME = "video/mp4"
        const val VIDEO_EXT = "mp4"

        /** `MediaGrid` draws at most this many tiles; the rest become the "+N" badge. */
        const val MAX_VISIBLE = 4
    }

    private fun videos(count: Int, caption: String? = null): MediaMessage =
        MockFactory.createMediaMessage(
            count = count,
            type = CometChatConstants.MESSAGE_TYPE_VIDEO,
            mimeType = VIDEO_MIME,
            extension = VIDEO_EXT,
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
                CometChatVideosBubble(
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

    private fun playBadges() =
        composeRule.onAllNodesWithContentDescription("Play").fetchSemanticsNodes().size

    // ── the play badge ──────────────────────────────────────────────────────

    @Test
    fun oneVideo_carriesAPlayBadge() {
        render(videos(1))
        assertEquals(1, playBadges())
    }

    @Test
    fun everyTileOfAnUnderFilledGridCarriesItsOwnBadge() {
        render(videos(3))
        assertEquals(3, playBadges())
    }

    @Test
    fun aFullGridCarriesFourBadges() {
        render(videos(MAX_VISIBLE))
        assertEquals(MAX_VISIBLE, playBadges())
    }

    // ── where the badge and the overflow rule meet ──────────────────────────

    @Test
    fun theOverflowTileShowsTheCountInsteadOfAPlayBadge() {
        // Four tiles, but only three of them still open a video.
        render(videos(7))
        assertEquals("the fourth tile gives up its badge to the +N overlay", 3, playBadges())
        composeRule.onNodeWithText("+3").assertIsDisplayed()
    }

    @Test
    fun fiveVideos_showFourTilesThreeBadgesAndAPlusOne() {
        render(videos(5))
        assertEquals(3, playBadges())
        composeRule.onNodeWithText("+1").assertIsDisplayed()
    }

    // ── caption ─────────────────────────────────────────────────────────────

    @Test
    fun aCaptionRendersBeneathTheGrid() {
        render(videos(2, caption = "both takes"))
        composeRule.onNodeWithText("both takes").assertIsDisplayed()
    }

    // ── callbacks ───────────────────────────────────────────────────────────

    @Test
    fun tappingATileReportsItsIndexAndAttachment() {
        var index: Int? = null
        var attachment: Attachment? = null
        render(videos(3), onMediaClick = { i, a -> index = i; attachment = a })

        composeRule.onAllNodesWithContentDescription("Play")[1].performClick()
        composeRule.waitForIdle()

        assertEquals("the second tile is index 1", 1, index)
        assertEquals("media_2.$VIDEO_EXT", attachment?.fileName)
    }

    @Test
    fun theOverflowTileFiresOnMoreClickRatherThanOnMediaClick() {
        var mediaIndex: Int? = null
        var all: List<Attachment>? = null
        render(videos(7), onMediaClick = { i, _ -> mediaIndex = i }, onMoreClick = { all = it })

        composeRule.onNodeWithText("+3").performClick()
        composeRule.waitForIdle()

        assertNull("the overflow tile must not report itself as a media tap", mediaIndex)
        assertEquals("onMoreClick receives every attachment", 7, all?.size)
    }

    @Test
    fun aLongPressOnATileReachesTheIntegrator() {
        var longPressed = false
        render(videos(2), onLongClick = { longPressed = true })

        composeRule.onAllNodesWithContentDescription("Play")[0].performTouchInput { longClick() }
        composeRule.waitForIdle()

        assertTrue(longPressed)
    }

    // ── accessibility ───────────────────────────────────────────────────────

    @Test
    fun theBubbleAnnouncesItselfAsVideoNotImage() {
        // The shared grid takes its label from the isVideo flag; getting it wrong would
        // announce a video message as an image one.
        render(videos(3))
        composeRule.onNodeWithContentDescription("Video message with 3 item(s)").assertIsDisplayed()
    }

    @Test
    fun theAnnouncedCountIsTheRealOneNotTheVisibleOne() {
        render(videos(7))
        composeRule.onNodeWithContentDescription("Video message with 7 item(s)").assertIsDisplayed()
    }

    // ── degenerate input ────────────────────────────────────────────────────

    @Test
    fun withNoAttachments_rendersNoTiles() {
        render(videos(0))
        assertEquals(0, playBadges())
    }

    @Test
    fun withNoAttachments_aCaptionStillRenders() {
        render(videos(0, caption = "the upload failed"))
        composeRule.onNodeWithText("the upload failed").assertIsDisplayed()
    }

    // ── alignment ───────────────────────────────────────────────────────────

    @Test
    fun bothAlignmentsRenderTheSameTiles() {
        render(videos(3), alignment = UIKitConstants.MessageBubbleAlignment.RIGHT)
        assertEquals(3, playBadges())
    }

    @Test
    fun anExplicitCaptionOverridesTheMessageCaption() {
        render(videos(2, caption = "from the message"), caption = "from the integrator")

        composeRule.onNodeWithText("from the integrator").assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithText("from the message").fetchSemanticsNodes().size,
        )
    }
}
