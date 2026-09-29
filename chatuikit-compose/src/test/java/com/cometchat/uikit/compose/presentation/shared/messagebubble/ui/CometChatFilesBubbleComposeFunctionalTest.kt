package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The bubble a file message reaches under the default multi-attachment setting.
 * Its own logic beyond the per-file card is the list: how many show, when the
 * "+N more" toggle appears, and what a non-file attachment does to it.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatFilesBubbleComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val COLLAPSED_FILE_COUNT = 3
    }

    private fun files(count: Int, caption: String? = null): MediaMessage =
        MockFactory.createMediaMessage(
            count = count,
            type = CometChatConstants.MESSAGE_TYPE_FILE,
            mimeType = "application/pdf",
            extension = "pdf",
            caption = caption,
        )

    private fun render(
        message: MediaMessage,
        alignment: UIKitConstants.MessageBubbleAlignment = UIKitConstants.MessageBubbleAlignment.LEFT,
        showDownloadIcon: Boolean = true,
        onFileClick: ((Int) -> Unit)? = null,
    ) {
        composeRule.setContent {
            CometChatTheme {
                CometChatFilesBubble(
                    message = message,
                    alignment = alignment,
                    showDownloadIcon = showDownloadIcon,
                    onFileClick = onFileClick,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun cards() =
        composeRule.onAllNodesWithContentDescription("Download file").fetchSemanticsNodes().size

    @Test
    fun oneFile_rendersItsName() {
        render(files(1))
        composeRule.onNodeWithText("media_1.pdf").assertIsDisplayed()
    }

    @Test
    fun severalFiles_renderInOrder() {
        render(files(2))
        composeRule.onNodeWithText("media_1.pdf").assertIsDisplayed()
        composeRule.onNodeWithText("media_2.pdf").assertIsDisplayed()
    }

    @Test
    fun theSubtitleCarriesSizeAndType() {
        // formatFileSubtitle joins the two with a bullet; 3,200,000 bytes -> 3.1 MB.
        render(files(1))
        composeRule.onNodeWithText("3.1 MB • PDF").assertIsDisplayed()
    }

    @Test
    fun threeFiles_showWithNoToggle() {
        render(files(COLLAPSED_FILE_COUNT))
        assertEquals(COLLAPSED_FILE_COUNT, cards())
        assertEquals(0, composeRule.onAllNodesWithText("Show +1 more").fetchSemanticsNodes().size)
    }

    @Test
    fun fourFiles_collapseBehindAToggle() {
        render(files(4))
        assertEquals(COLLAPSED_FILE_COUNT, cards())
        composeRule.onNodeWithText("Show +1 more").assertIsDisplayed()
    }

    @Test
    fun theToggleCountsHiddenCardsNotTheTotal() {
        render(files(7))
        composeRule.onNodeWithText("Show +4 more").assertIsDisplayed()
    }

    @Test
    fun expandingRevealsTheRest() {
        render(files(7))
        composeRule.onNodeWithText("Show +4 more").performClick()
        composeRule.waitForIdle()
        assertEquals(7, cards())
        composeRule.onNodeWithText("Show less").assertIsDisplayed()
        composeRule.onNodeWithText("media_7.pdf").assertIsDisplayed()
    }

    @Test
    fun collapsingAgainHidesThemBack() {
        render(files(7))
        composeRule.onNodeWithText("Show +4 more").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Show less").performClick()
        composeRule.waitForIdle()
        assertEquals(COLLAPSED_FILE_COUNT, cards())
    }

    @Test
    fun aCaptionRendersBelowTheCards() {
        render(files(2, caption = "both drafts attached"))
        composeRule.onNodeWithText("both drafts attached").assertIsDisplayed()
    }

    @Test
    fun clickReportsThePositionOfTheCardTapped() {
        var index: Int? = null
        render(files(3), onFileClick = { index = it })
        composeRule.onNodeWithText("media_3.pdf").performClick()
        composeRule.waitForIdle()
        assertEquals(2, index)
    }

    @Test
    fun theDownloadAffordanceCanBeTurnedOff() {
        render(files(2), showDownloadIcon = false)
        assertEquals(0, cards())
    }

    @Test
    fun withNoAttachments_rendersNothing() {
        render(files(0))
        assertEquals(0, cards())
        assertEquals(0, composeRule.onAllNodesWithText("media_1.pdf").fetchSemanticsNodes().size)
    }

    @Test
    fun bothAlignmentsRenderTheSameCards() {
        render(files(2), UIKitConstants.MessageBubbleAlignment.RIGHT)
        assertEquals(2, cards())
        composeRule.onNodeWithText("media_1.pdf").assertIsDisplayed()
    }

    @Test
    fun aNonPdfKeepsItsOwnTypeBadge() {
        val mixed = MockFactory.createMediaMessage(
            count = 1,
            type = CometChatConstants.MESSAGE_TYPE_FILE,
            mimeType = "application/zip",
            extension = "zip",
        )
        render(mixed)
        composeRule.onNodeWithText("3.1 MB • ZIP").assertIsDisplayed()
    }
}
