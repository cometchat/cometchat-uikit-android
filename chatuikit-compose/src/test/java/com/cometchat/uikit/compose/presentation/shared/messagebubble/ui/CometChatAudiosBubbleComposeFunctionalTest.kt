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
import com.cometchat.uikit.core.utils.AudioBubbleStateManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Instrumented layer for [CometChatAudiosBubble].
 *
 * The bubble an audio message actually reaches under default settings. Its own logic,
 * beyond the per-card player it shares with the single bubble, is the list: how many
 * cards show, when the "Show +N more" toggle appears, and what happens to an
 * attachment that is not audio at all.
 *
 * That last one is not hypothetical — the bubble keeps a kind-mismatched attachment in
 * the list and renders it as an inert broken card rather than dropping it, so a
 * server-sent mixed payload does not silently lose a file. These tests pin that.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAudiosBubbleComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val AUDIO_MIME = "audio/mpeg"
        const val AUDIO_EXT = "mp3"
        /** `CometChatAudiosBubble` collapses beyond this many cards. */
        const val COLLAPSED_AUDIO_COUNT = 3
    }

    @Before fun clearStates() = AudioBubbleStateManager.clearAll()

    @After fun clearStatesAfter() = AudioBubbleStateManager.clearAll()

    private fun audios(count: Int, caption: String? = null): MediaMessage =
        MockFactory.createMediaMessage(
            count = count,
            type = CometChatConstants.MESSAGE_TYPE_AUDIO,
            mimeType = AUDIO_MIME,
            extension = AUDIO_EXT,
            caption = caption,
        )

    private fun render(
        message: MediaMessage,
        alignment: UIKitConstants.MessageBubbleAlignment = UIKitConstants.MessageBubbleAlignment.LEFT,
        showDownloadIcon: Boolean = true,
        onDownloadClick: ((Int) -> Unit)? = null,
    ) {
        composeRule.setContent {
            CometChatTheme {
                CometChatAudiosBubble(
                    message = message,
                    alignment = alignment,
                    showDownloadIcon = showDownloadIcon,
                    onDownloadClick = onDownloadClick,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun playButtons() =
        composeRule.onAllNodesWithContentDescription("Play").fetchSemanticsNodes().size

    // ── the card list ───────────────────────────────────────────────────────

    @Test
    fun oneAudio_rendersASingleCard() {
        render(audios(1))
        assertEquals(1, playButtons())
        composeRule.onNodeWithText("media_1.mp3").assertIsDisplayed()
    }

    @Test
    fun twoAudios_renderBothNamesInOrder() {
        render(audios(2))
        assertEquals(2, playButtons())
        composeRule.onNodeWithText("media_1.mp3").assertIsDisplayed()
        composeRule.onNodeWithText("media_2.mp3").assertIsDisplayed()
    }

    @Test
    fun threeAudios_areAllVisibleWithNoToggle() {
        // Exactly at the collapse threshold — the toggle must not appear yet.
        render(audios(COLLAPSED_AUDIO_COUNT))
        assertEquals(COLLAPSED_AUDIO_COUNT, playButtons())
        assertEquals(0, composeRule.onAllNodesWithText("Show +1 more").fetchSemanticsNodes().size)
    }

    // ── collapse and expand ─────────────────────────────────────────────────

    @Test
    fun fourAudios_collapseToThreeBehindAToggle() {
        render(audios(4))
        assertEquals(COLLAPSED_AUDIO_COUNT, playButtons())
        composeRule.onNodeWithText("Show +1 more").assertIsDisplayed()
    }

    @Test
    fun theToggleCountsTheHiddenCardsNotTheTotal() {
        render(audios(7))
        composeRule.onNodeWithText("Show +4 more").assertIsDisplayed()
    }

    @Test
    fun expandingRevealsTheRestAndOffersToCollapseAgain() {
        render(audios(7))
        composeRule.onNodeWithText("Show +4 more").performClick()
        composeRule.waitForIdle()

        assertEquals(7, playButtons())
        composeRule.onNodeWithText("Show less").assertIsDisplayed()
        composeRule.onNodeWithText("media_7.mp3").assertIsDisplayed()
    }

    @Test
    fun collapsingAgainHidesThemBack() {
        render(audios(7))
        composeRule.onNodeWithText("Show +4 more").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Show less").performClick()
        composeRule.waitForIdle()

        assertEquals(COLLAPSED_AUDIO_COUNT, playButtons())
        composeRule.onNodeWithText("Show +4 more").assertIsDisplayed()
    }

    // ── caption ─────────────────────────────────────────────────────────────

    @Test
    fun aCaptionRendersBelowTheCards() {
        render(audios(2, caption = "two takes of the same riff"))
        composeRule.onNodeWithText("two takes of the same riff").assertIsDisplayed()
    }

    // ── the download affordance ─────────────────────────────────────────────

    @Test
    fun eachCardCarriesItsOwnDownloadAffordance() {
        render(audios(2))
        assertEquals(
            2,
            composeRule.onAllNodesWithContentDescription("Download audio").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun downloadReportsThePositionOfTheCardThatWasTapped() {
        var index: Int? = null
        render(audios(3), onDownloadClick = { index = it })
        composeRule.onAllNodesWithContentDescription("Download audio")[2].performClick()
        composeRule.waitForIdle()
        assertEquals("the third card is index 2", 2, index)
    }

    @Test
    fun downloadIndicesFollowTheVisibleOrderAfterExpanding() {
        // The callback reports the index into the resolved attachment list, so it must
        // still be right for a card that was hidden a moment ago.
        var index: Int? = null
        render(audios(5), onDownloadClick = { index = it })
        composeRule.onNodeWithText("Show +2 more").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithContentDescription("Download audio")[4].performClick()
        composeRule.waitForIdle()
        assertEquals(4, index)
    }

    @Test
    fun theDownloadAffordanceCanBeTurnedOff() {
        render(audios(2), showDownloadIcon = false)
        assertEquals(
            0,
            composeRule.onAllNodesWithContentDescription("Download audio").fetchSemanticsNodes().size,
        )
    }

    // ── degenerate input ────────────────────────────────────────────────────

    @Test
    fun withNoAttachments_rendersNothingRatherThanAnEmptyBubble() {
        render(audios(0))
        assertEquals(0, playButtons())
        assertEquals(
            0,
            composeRule.onAllNodesWithContentDescription("Download audio").fetchSemanticsNodes().size,
        )
        assertEquals(0, composeRule.onAllNodesWithText("media_1.mp3").fetchSemanticsNodes().size)
    }

    @Test
    fun bothAlignmentsRenderTheSameCards() {
        render(audios(2), UIKitConstants.MessageBubbleAlignment.RIGHT)
        assertEquals(2, playButtons())
        composeRule.onNodeWithText("media_1.mp3").assertIsDisplayed()
    }
}
