package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatFileBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Functional (render + assert) layer for the single [CometChatFileBubble].
 *
 * The property layer sweeps the parameter surface; this pins the behaviour a host
 * actually sees — what the two text rows say, that a new message replaces the old
 * row rather than stacking on it, and where the download-all affordance appears.
 * It is the Compose half of [CometChatFileBubbleFunctionalTest] in the View toolkit,
 * and it deliberately re-states that toolkit's caption decision so the parity is on
 * record on both sides.
 *
 * Every branch passes a style with a resolved corner radius, as the renderer does.
 * The bubble's own default style leaves it unset and crashes on the first draw —
 * pinned in [CometChatFileBubbleDefaultStyleTest].
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatFileBubbleComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val FILE_MIME = "application/pdf"
        const val FILE_EXT = "pdf"
        const val FIRST_FILE = "media_1.pdf"
        const val SECOND_FILE = "media_2.pdf"
        val INCOMING = UIKitConstants.MessageBubbleAlignment.LEFT
        val OUTGOING = UIKitConstants.MessageBubbleAlignment.RIGHT
    }

    private fun fileMessage(
        count: Int = 1,
        mimeType: String = FILE_MIME,
        extension: String = FILE_EXT,
        sizeBytes: Int? = null,
        caption: String? = null,
    ): MediaMessage = MockFactory.createMediaMessage(
        count = count,
        type = CometChatConstants.MESSAGE_TYPE_FILE,
        mimeType = mimeType,
        extension = extension,
        caption = caption,
    ).apply { if (count == 1 && sizeBytes != null) attachment.fileSize = sizeBytes }

    /** The resolved style the renderer hands in; the bubble's own default has no radius. */
    @Composable
    private fun style(alignment: UIKitConstants.MessageBubbleAlignment): CometChatFileBubbleStyle =
        when (alignment) {
            OUTGOING -> CometChatFileBubbleStyle.outgoing()
            else -> CometChatFileBubbleStyle.incoming()
        }.copy(cornerRadius = 12.dp)

    private fun render(
        message: MediaMessage = fileMessage(),
        alignment: UIKitConstants.MessageBubbleAlignment = INCOMING,
        onFileClick: ((Int) -> Unit)? = null,
        onDownloadClick: ((Int) -> Unit)? = null,
    ) {
        composeRule.setContent {
            CometChatTheme {
                CometChatFileBubble(
                    message = message,
                    alignment = alignment,
                    style = style(alignment),
                    onFileClick = onFileClick,
                    onDownloadClick = onDownloadClick,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun itShowsTheFileName() {
        render()
        composeRule.onNodeWithText(FIRST_FILE).assertIsDisplayed()
    }

    @Test
    fun itShowsTheSizeAndTypeUnderTheName() {
        render()
        composeRule.onNodeWithText("3.1 MB", substring = true).assertIsDisplayed()
    }

    @Test
    fun aSmallFileIsFormattedInKilobytes() {
        render(message = fileMessage(sizeBytes = 204_800))
        composeRule.onNodeWithText("200 KB", substring = true).assertIsDisplayed()
    }

    @Test
    fun aNewMessageReplacesTheRowRatherThanStackingOnIt() {
        // The View bubble is re-bound by a RecyclerView adapter and the equivalent test
        // pins that a bind leaves no residue. Recomposition is the Compose analogue.
        var message by mutableStateOf(fileMessage())
        composeRule.setContent {
            CometChatTheme {
                CometChatFileBubble(
                    message = message,
                    alignment = INCOMING,
                    style = style(INCOMING),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(FIRST_FILE).assertIsDisplayed()

        message = fileMessage(mimeType = "application/zip", extension = "zip")
            .apply { id = 2L }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("media_1.zip").assertIsDisplayed()
        assertEquals(
            "the previous file name should be gone, not stacked above",
            0,
            composeRule.onAllNodesWithText(FIRST_FILE).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun theAttachmentListIsCachedOnTheMessageId() {
        // `remember(message.id)` keys the extraction, so a *different* message object
        // carrying the same id keeps the row it first drew. Unique ids make that
        // invisible in practice; it is pinned here because an edit-in-place path — which
        // keeps the id and changes the payload — would need the key widened first.
        var message by mutableStateOf(fileMessage())
        composeRule.setContent {
            CometChatTheme {
                CometChatFileBubble(
                    message = message,
                    alignment = INCOMING,
                    style = style(INCOMING),
                )
            }
        }
        composeRule.waitForIdle()

        message = fileMessage(mimeType = "application/zip", extension = "zip")
        composeRule.waitForIdle()

        composeRule.onNodeWithText(FIRST_FILE).assertIsDisplayed()
        assertEquals(
            "same id, so the new payload is not re-extracted",
            0,
            composeRule.onAllNodesWithText("media_1.zip").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun anEmptyAttachmentListRendersThePlaceholderRow() {
        composeRule.setContent {
            CometChatTheme {
                CometChatFileBubble(
                    attachments = emptyList(),
                    alignment = INCOMING,
                    style = style(INCOMING),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Unknown file").assertIsDisplayed()
    }

    @Test
    fun theSingleFileBubbleDropsCaptions() {
        // Neither toolkit's single file bubble renders a caption — there is no caption
        // parameter here and no caption handling in the View bubble. Captions ride the
        // Files bubble, which is what a file message reaches by default. Pinned so the
        // asymmetry is a decision on record rather than a surprise.
        render(message = fileMessage(caption = "the signed copy"))
        assertEquals(
            0,
            composeRule.onAllNodesWithText("the signed copy").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun oneFileGetsNoDownloadAllButton() {
        render()
        assertEquals(
            0,
            composeRule.onAllNodesWithText("Download All").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun severalFilesGetTheDownloadAllButton() {
        render(message = fileMessage(count = 2))
        composeRule.onNodeWithText("Download All").assertIsDisplayed()
    }

    @Test
    fun tappingAFileHandsBackItsIndex() {
        var clicked: Int? = null
        val attachments: List<Attachment> = listOf(
            MockFactory.createAttachment(1, FILE_MIME, FILE_EXT),
            MockFactory.createAttachment(2, FILE_MIME, FILE_EXT),
        )
        composeRule.setContent {
            CometChatTheme {
                CometChatFileBubble(
                    attachments = attachments,
                    alignment = INCOMING,
                    style = style(INCOMING),
                    onFileClick = { clicked = it },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(SECOND_FILE).performClick()
        composeRule.waitForIdle()
        assertEquals("the second row should report index 1", 1, clicked)
    }

    @Test
    fun bothAlignmentsRenderTheSameRow() {
        render(alignment = OUTGOING)
        composeRule.onNodeWithText(FIRST_FILE).assertIsDisplayed()
        composeRule.onNodeWithText("3.1 MB", substring = true).assertIsDisplayed()
    }
}
