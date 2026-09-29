package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
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
 * Instrumented layer for [CometChatImageBubble].
 *
 * Hosts the real composable and drives it. Coil has no network under Robolectric
 * and renders its error placeholder, which is deterministic — so tile *geometry*
 * and *count* are assertable even though the pixels of the photo are not. Tiles
 * are addressed by click action, because the placeholder discards the per-tile
 * `contentDescription`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatImageBubbleComposeFunctionalTest {

    @get:Rule
    val composeRule = createComposeRule()

    private companion object {
        const val FIXED_SENT_AT = 1_729_011_360L
    }





    private fun render(message: MediaMessage) {
        composeRule.setContent {
            CometChatTheme {
                CometChatImageBubble(
                    message = message,
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                    onImageClick = { _, _ -> },
                )
            }
        }
    }

    private fun tileCount(): Int =
        composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size

    // ── attachment resolution ───────────────────────────────────────────────

    @Test
    fun singleAttachment_rendersOneImage() {
        render(MockFactory.createMediaMessage(count = 1))

        composeRule.onNodeWithContentDescription("Image message with 1 image(s)").assertIsDisplayed()
        assertEquals("one tappable image", 1, tileCount())
    }

    @Test
    fun metadataAttachments_renderAGridOfThatCount() {
        render(MockFactory.createMediaMessage(count = 3))

        composeRule.onNodeWithContentDescription("Image message with 3 image(s)").assertIsDisplayed()
        assertEquals("three tappable tiles", 3, tileCount())
    }

    @Test
    fun moreThanFourAttachments_capTheGridAndShowAnOverflowCount() {
        render(MockFactory.createMediaMessage(count = 7))

        // MAX_VISIBLE_ITEMS is 4, so three spill into the overflow badge.
        assertEquals("grid caps at four tiles", 4, tileCount())
        composeRule.onNodeWithText("+3").assertIsDisplayed()
    }

    @Test
    fun exactlyFourAttachments_fillTheGridWithNoOverflow() {
        render(MockFactory.createMediaMessage(count = 4))

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
                CometChatImageBubble(
                    message = MockFactory.createMediaMessage(count = 0),
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                    onImageClick = { _, _ -> clicked = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Image message with 0 image(s)").assertIsDisplayed()
        assertEquals("the upload placeholder is present", 1, tileCount())

        composeRule.onAllNodes(hasClickAction())[0].performClick()
        composeRule.waitForIdle()
        assertEquals("the placeholder's onClick is a no-op", false, clicked)
    }

    // ── caption ─────────────────────────────────────────────────────────────

    @Test
    fun caption_rendersBelowTheGrid() {
        render(MockFactory.createMediaMessage(count = 2, caption = "two from the trip"))

        composeRule.onNodeWithText("two from the trip").assertIsDisplayed()
    }

    @Test
    fun editedMessage_rendersTheEditedMarker() {
        render(MockFactory.createMediaMessage(count = 1, caption = "a caption").apply { editedAt = MockFactory.FIXED_SENT_AT + 60 })

        composeRule.onNodeWithText("Edited", substring = true).assertIsDisplayed()
    }

    // ── the attachments-list divergence ─────────────────────────────────────

    @Test
    fun sdkAttachmentsList_isIgnoredByThisBubble_unlikeTheSharedResolver() {
        // MEASURED DIVERGENCE, pinned deliberately.
        //
        // The shared helper `resolveAttachments` checks `message.attachments` (the
        // SDK-parsed list) FIRST, then metadata, then the single attachment.
        // CometChatImageBubble's own private `extractAttachments` never consults
        // `message.attachments` at all — it goes metadata, then single attachment.
        //
        // So a multi-image message delivered through the SDK list, with no
        // metadata mirror, renders as zero images here while the multi-attachment
        // bubble would render three. Asserting both sides so the difference is
        // visible if either changes.
        val message = MockFactory.createMediaMessage(count = 0).apply {
            attachments = (1..3).map { MockFactory.createAttachment(it) }
        }

        assertEquals(
            "the shared resolver sees all three",
            3,
            resolveAttachments(message).size
        )

        render(message)

        composeRule.onNodeWithContentDescription("Image message with 0 image(s)").assertIsDisplayed()
        // It falls through to the empty/upload branch: one placeholder, not three tiles.
        assertEquals("the image bubble resolves none of them into tiles", 1, tileCount())
    }

    // ── the attachments overload ────────────────────────────────────────────

    @Test
    fun attachmentsOverload_rendersWithoutAMessage() {
        composeRule.setContent {
            CometChatTheme {
                CometChatImageBubble(
                    attachments = (1..2).map { MockFactory.createAttachment(it) },
                    alignment = UIKitConstants.MessageBubbleAlignment.RIGHT,
                    onImageClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Image message with 2 image(s)").assertIsDisplayed()
        assertEquals(2, tileCount())
    }

    @Test
    fun tappingATile_reportsItsIndexAndAttachment() {
        var index: Int? = null
        var attachment: Attachment? = null
        composeRule.setContent {
            CometChatTheme {
                CometChatImageBubble(
                    attachments = (1..2).map { MockFactory.createAttachment(it) },
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                    onImageClick = { i, a -> index = i; attachment = a },
                )
            }
        }

        composeRule.onAllNodes(hasClickAction())[1].performClick()
        composeRule.waitForIdle()

        assertEquals("second tile reports index 1", 1, index)
        assertEquals("media_2.jpg", attachment?.fileName)
    }

}
