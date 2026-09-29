package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Instrumented layer for [CometChatVideoBubble].
 *
 * Hosts the real composable and drives it. Coil has no network under Robolectric
 * and renders its error placeholder, which is deterministic — so tile *geometry*
 * and *count* are assertable even though the pixels of the photo are not. Tiles
 * are addressed by click action, because the placeholder discards the per-tile
 * `contentDescription`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatVideoBubbleComposeFunctionalTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Grid tiles, excluding the play control nested inside each one. */
    private val TILES = hasClickAction() and !hasContentDescription(PLAY_CD)

    private companion object {
        /** Each video tile nests its own play control, which is separately clickable. */
        const val PLAY_CD = "Play video"
        const val FIXED_SENT_AT = 1_729_011_360L
    }





    private fun render(message: MediaMessage) {
        composeRule.setContent {
            CometChatTheme {
                CometChatVideoBubble(
                    message = message,
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                    onVideoClick = { _, _ -> },
                )
            }
        }
    }

    private fun tileCount(): Int =
        composeRule.onAllNodes(TILES).fetchSemanticsNodes().size

    // ── attachment resolution ───────────────────────────────────────────────

    @Test
    fun singleAttachment_rendersOneImage() {
        render(MockFactory.createMediaMessage(count = 1, type = CometChatConstants.MESSAGE_TYPE_VIDEO, mimeType = "video/mp4", extension = "mp4"))

        composeRule.onNodeWithContentDescription("Video message with 1 video(s)").assertIsDisplayed()
        assertEquals("one tappable image", 1, tileCount())
    }

    @Test
    fun metadataAttachments_renderAGridOfThatCount() {
        render(MockFactory.createMediaMessage(count = 3, type = CometChatConstants.MESSAGE_TYPE_VIDEO, mimeType = "video/mp4", extension = "mp4"))

        composeRule.onNodeWithContentDescription("Video message with 3 video(s)").assertIsDisplayed()
        assertEquals("three tappable tiles", 3, tileCount())
    }

    @Test
    fun moreThanFourAttachments_capTheGridAndShowAnOverflowCount() {
        render(MockFactory.createMediaMessage(count = 7, type = CometChatConstants.MESSAGE_TYPE_VIDEO, mimeType = "video/mp4", extension = "mp4"))

        // MAX_VISIBLE_ITEMS is 4, so three spill into the overflow badge.
        assertEquals("grid caps at four tiles", 4, tileCount())
        composeRule.onNodeWithText("+3").assertIsDisplayed()
    }

    @Test
    fun exactlyFourAttachments_fillTheGridWithNoOverflow() {
        render(MockFactory.createMediaMessage(count = 4, type = CometChatConstants.MESSAGE_TYPE_VIDEO, mimeType = "video/mp4", extension = "mp4"))

        assertEquals(4, tileCount())
        composeRule.onNodeWithText("+0").assertDoesNotExist()
    }

    @Test
    fun noAttachments_rendersTheUploadPlaceholder() {
        // Not a crash case and not "nothing": the empty branch deliberately renders
        // a SingleImageView over the local file path, for a message still uploading.
        // It is clickable but its onClick is a no-op, so no index is ever reported.
        var clicked = false
        composeRule.setContent {
            CometChatTheme {
                CometChatVideoBubble(
                    message = MockFactory.createMediaMessage(count = 0, type = CometChatConstants.MESSAGE_TYPE_VIDEO, mimeType = "video/mp4", extension = "mp4"),
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                    onVideoClick = { _, _ -> clicked = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Video message with 0 video(s)").assertIsDisplayed()
        assertEquals("the upload placeholder is present", 1, tileCount())

        composeRule.onAllNodes(TILES)[0].performClick()
        composeRule.waitForIdle()
        assertEquals("the placeholder's onClick is a no-op", false, clicked)
    }

    // ── caption ─────────────────────────────────────────────────────────────

    @Test
    fun caption_rendersBelowTheGrid() {
        render(MockFactory.createMediaMessage(count = 2, caption = "two from the trip", type = CometChatConstants.MESSAGE_TYPE_VIDEO, mimeType = "video/mp4", extension = "mp4"))

        composeRule.onNodeWithText("two from the trip").assertIsDisplayed()
    }

    @Test
    fun editedMessage_rendersTheEditedMarker() {
        render(MockFactory.createMediaMessage(count = 1, caption = "a caption", type = CometChatConstants.MESSAGE_TYPE_VIDEO, mimeType = "video/mp4", extension = "mp4").apply { editedAt = MockFactory.FIXED_SENT_AT + 60 })

        composeRule.onNodeWithText("Edited", substring = true).assertIsDisplayed()
    }

    // ── the attachments-list divergence ─────────────────────────────────────

    @Test
    fun sdkAttachmentsList_isIgnoredByThisBubble_unlikeTheSharedResolver() {
        // MEASURED DIVERGENCE, pinned deliberately.
        //
        // The shared helper `resolveAttachments` checks `message.attachments` (the
        // SDK-parsed list) FIRST, then metadata, then the single attachment.
        // CometChatVideoBubble's own private `extractAttachments` never consults
        // `message.attachments` at all — it goes metadata, then single attachment.
        //
        // So a multi-image message delivered through the SDK list, with no
        // metadata mirror, renders as zero images here while the multi-attachment
        // bubble would render three. Asserting both sides so the difference is
        // visible if either changes.
        val message = MockFactory.createMediaMessage(count = 0, type = CometChatConstants.MESSAGE_TYPE_VIDEO, mimeType = "video/mp4", extension = "mp4").apply {
            attachments = (1..3).map { MockFactory.createAttachment(it, "video/mp4", "mp4") }
        }

        assertEquals(
            "the shared resolver sees all three",
            3,
            resolveAttachments(message).size
        )

        render(message)

        composeRule.onNodeWithContentDescription("Video message with 0 video(s)").assertIsDisplayed()
        // It falls through to the empty/upload branch: one placeholder, not three tiles.
        assertEquals("the image bubble resolves none of them into tiles", 1, tileCount())
    }

    // ── the attachments overload ────────────────────────────────────────────

    @Test
    fun attachmentsOverload_rendersWithoutAMessage() {
        composeRule.setContent {
            CometChatTheme {
                CometChatVideoBubble(
                    attachments = (1..2).map { MockFactory.createAttachment(it, "video/mp4", "mp4") },
                    alignment = UIKitConstants.MessageBubbleAlignment.RIGHT,
                    onVideoClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Video message with 2 video(s)").assertIsDisplayed()
        assertEquals(2, tileCount())
    }

    @Test
    fun tappingATile_offThePlayControl_reportsItsIndexAndAttachment() {
        // Tap placement matters here: the play button is a small control centred in
        // the tile, so performClick() — which targets the node centre — lands on play,
        // not on the tile. An off-centre tap reaches the tile's own handler.
        var index: Int? = null
        var attachment: Attachment? = null
        var played: Attachment? = null
        composeRule.setContent {
            CometChatTheme {
                CometChatVideoBubble(
                    attachments = (1..2).map { MockFactory.createAttachment(it, "video/mp4", "mp4") },
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                    onVideoClick = { i, a -> index = i; attachment = a },
                    onPlayClick = { played = it },
                )
            }
        }

        composeRule.onAllNodes(TILES)[1].performTouchInput { click(Offset(8f, 8f)) }
        composeRule.waitForIdle()

        assertEquals("second tile reports index 1", 1, index)
        assertEquals("media_2.mp4", attachment?.fileName)
        assertEquals("the play control was not hit", null, played)
    }

    @Test
    fun tappingTheCentreOfATile_reachesThePlayControl() {
        // The other half: the centred play button owns the middle of the tile.
        var played: Attachment? = null
        composeRule.setContent {
            CometChatTheme {
                CometChatVideoBubble(
                    attachments = (1..2).map { MockFactory.createAttachment(it, "video/mp4", "mp4") },
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                    onVideoClick = { _, _ -> },
                    onPlayClick = { played = it },
                )
            }
        }

        composeRule.onAllNodes(TILES)[0].performClick()
        composeRule.waitForIdle()

        assertEquals("media_1.mp4", played?.fileName)
    }

}
