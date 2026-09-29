package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.models.PollData
import com.cometchat.uikit.core.models.PollOption
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Instrumented layer for [CometChatPollBubble].
 *
 * Uses the `PollData` overload rather than the `CustomMessage` one: the latter
 * re-extracts from JSON and reaches for the SDK, which the unit layer already covers
 * in [PollDataExtractionTest]. Driving the data overload keeps these assertions about
 * rendering and voting rather than parsing.
 *
 * Unlike the media bubbles this one renders real text — question, option labels and
 * vote counts — so Compose semantics can see all of it.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatPollBubbleComposeFunctionalTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun option(id: String, text: String, votes: Int, selected: Boolean = false) =
        PollOption(id = id, text = text, voteCount = votes, voters = emptyList(), isSelected = selected)

    private fun pollData(
        question: String = "Which colour?",
        options: List<PollOption> = listOf(
            option("1", "Red", 2),
            option("2", "Blue", 3),
            option("3", "Green", 0),
        ),
    ) = PollData(id = "poll-1", question = question, options = options, totalVotes = options.sumOf { it.voteCount })

    private fun render(data: PollData = pollData(), onVote: ((Int) -> Unit)? = null) {
        composeRule.setContent {
            CometChatTheme { CometChatPollBubble(pollData = data, onVote = onVote) }
        }
    }

    @Test
    fun rendersTheQuestion() {
        render()

        composeRule.onNodeWithText("Which colour?").assertIsDisplayed()
    }

    @Test
    fun rendersEveryOptionLabel() {
        render()

        composeRule.onNodeWithText("Red").assertIsDisplayed()
        composeRule.onNodeWithText("Blue").assertIsDisplayed()
        composeRule.onNodeWithText("Green").assertIsDisplayed()
    }

    @Test
    fun tappingAnOption_reportsItsOneIndexedOptionNumber_notItsListPosition() {
        // Measured: onVote carries the poll's own 1-indexed option number, matching
        // the "1"/"2"/"3" keys in customData — not the zero-based list position.
        // Pinned because an off-by-one here votes for the wrong option.
        var voted: Int? = null
        render(onVote = { voted = it })

        composeRule.onNodeWithText("Blue").performClick()
        composeRule.waitForIdle()
        assertEquals("Blue is option 2", 2, voted)

        composeRule.onNodeWithText("Red").performClick()
        composeRule.waitForIdle()
        assertEquals("Red is option 1", 1, voted)
    }

    @Test
    fun aPollWithNoVotesRendersEveryOption() {
        render(
            pollData(
                options = listOf(option("1", "Yes", 0), option("2", "No", 0)),
            )
        )

        composeRule.onNodeWithText("Yes").assertIsDisplayed()
        composeRule.onNodeWithText("No").assertIsDisplayed()
    }

    @Test
    fun aSelectedOptionStillRenders() {
        // isSelected drives the viewer's own-vote treatment; the label must survive it.
        render(
            pollData(
                options = listOf(option("1", "Red", 4, selected = true), option("2", "Blue", 1)),
            )
        )

        composeRule.onNodeWithText("Red").assertIsDisplayed()
    }

    @Test
    fun aTwoOptionPollRendersBoth() {
        render(pollData(options = listOf(option("1", "Yes", 7), option("2", "No", 4))))

        composeRule.onNodeWithText("Yes").assertIsDisplayed()
        composeRule.onNodeWithText("No").assertIsDisplayed()
    }

    @Test
    fun aLongQuestionStillRenders() {
        val q = "A noticeably longer poll question that has to wrap across more than one line inside the bubble"
        render(pollData(question = q))

        composeRule.onNodeWithText(q).assertIsDisplayed()
    }
}
