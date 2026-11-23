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
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.models.AttachmentUploadStatus
import com.cometchat.uikit.core.models.AttachmentUploadTile
import com.cometchat.uikit.core.models.ERROR_ATTACHMENT_COPY_MISSING
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ENG-41136 / D-306 (BEH-UPLOAD-14): a restored tile whose local copy is missing is remove-only in
 * the Compose tray: it announces that the file was not saved, a tap on its body never retries (or
 * reaches any callback), and the corner ✕ removes it. Taps are positional for the reasons given in
 * [CometChatAttachmentTileComposeFunctionalTest].
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAttachmentTileCopyMissingComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val NAME = "holiday.jpg"
        const val COPY_MISSING_LABEL = "File not saved. Remove it and add it again."
        const val RETRY_LABEL = "Tap to retry"
    }

    private val fired = mutableListOf<String>()

    private fun copyMissingTile(category: String) = AttachmentUploadTile(
        fileId = "b1_0",
        name = NAME,
        size = 2_400_000L,
        mimeType = "image/jpeg",
        category = category,
        status = AttachmentUploadStatus.FAILED,
        error = CometChatException(ERROR_ATTACHMENT_COPY_MISSING, "missing"),
    )

    private fun render(tile: AttachmentUploadTile) {
        composeRule.setContent {
            CometChatTheme {
                CometChatAttachmentTile(
                    tile = tile,
                    onCancel = { fired += "cancel" },
                    onRemove = { fired += "remove" },
                    onRetry = { fired += "retry" },
                    onClick = { fired += "click" },
                    onRejected = { fired += "rejected" },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun node() = composeRule.onNodeWithContentDescription("$NAME, $COPY_MISSING_LABEL")

    @Test
    fun aCopyMissingTileAnnouncesItWasNotSavedAndNotThatItCanBeRetried() {
        render(copyMissingTile(CometChatConstants.MESSAGE_TYPE_IMAGE))
        node().assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithContentDescription("$NAME, $RETRY_LABEL").fetchSemanticsNodes().size
        )
    }

    @Test
    fun tappingACopyMissingMediaTileDoesNothing() {
        render(copyMissingTile(CometChatConstants.MESSAGE_TYPE_IMAGE))
        node().performClick()
        composeRule.waitForIdle()
        assertEquals(emptyList<String>(), fired)
    }

    @Test
    fun tappingACopyMissingFileTileDoesNothing() {
        render(copyMissingTile(CometChatConstants.MESSAGE_TYPE_FILE))
        node().performClick()
        composeRule.waitForIdle()
        assertEquals(emptyList<String>(), fired)
    }

    @Test
    fun tappingACopyMissingAudioTileDoesNothing() {
        render(copyMissingTile(CometChatConstants.MESSAGE_TYPE_AUDIO))
        node().performClick()
        composeRule.waitForIdle()
        assertEquals(emptyList<String>(), fired)
    }

    @Test
    fun theCornerBadgeRemovesACopyMissingTile() {
        render(copyMissingTile(CometChatConstants.MESSAGE_TYPE_IMAGE))
        node().performTouchInput { click(percentOffset(0.95f, 0.08f)) }
        composeRule.waitForIdle()
        assertEquals(listOf("remove"), fired)
    }
}
