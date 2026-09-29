package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatPollBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.models.PollData
import com.cometchat.uikit.core.models.PollOption
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Property (prop-matrix) layer for [CometChatPollBubble].
 *
 * Swept on the `PollData` overload: three integrator params, all observable, so
 * nothing is waived here — the first bubble in this set with a fully exercised
 * surface. The poll renders real text, so effects are read straight from Compose
 * semantics rather than the Android view tree the media bubbles need.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatPollBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createComposeRule()

    private companion object {
        const val OWNER = "CometChatPollBubble"
        const val QUESTION = "Which colour?"
    }

    private fun option(id: String, text: String, votes: Int) =
        PollOption(id = id, text = text, voteCount = votes, voters = emptyList(), isSelected = false)

    @Test
    fun pollBubble_propMatrix_coversEveryProp() {
        var voted: Int? = null
        var incoming: CometChatPollBubbleStyle? = null
        var outgoing: CometChatPollBubbleStyle? = null

        composeRule.setContent {
            CometChatTheme {
                incoming = CometChatPollBubbleStyle.incoming()
                outgoing = CometChatPollBubbleStyle.outgoing()
                CometChatPollBubble(
                    pollData = PollData(
                        id = "poll-1",
                        question = QUESTION,
                        options = listOf(option("1", "Red", 2), option("2", "Blue", 3)),
                        totalVotes = 5,
                    ),
                    style = incoming!!,
                    onVote = { voted = it },
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("pollData") {
                composeRule.onNodeWithText(QUESTION).assertIsDisplayed()
                composeRule.onNodeWithText("Red").assertIsDisplayed()
                composeRule.onNodeWithText("Blue").assertIsDisplayed()
            }
            // Unlike the AI assistant bubble, incoming and outgoing genuinely differ
            // here — a poll is something either party can send.
            value("style") {
                assertNotEquals(
                    "incoming() and outgoing() should resolve differently",
                    incoming,
                    outgoing,
                )
            }
            callback("onVote") {
                composeRule.onNodeWithText("Blue").performClick()
                composeRule.waitForIdle()
                assertEquals("Blue is option 2", 2, voted)
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered }.map { it.name }

        println("  [poll compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [poll] NOT covered: $uncovered")

        assertEquals("every prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("nothing waived on this bubble", 0, cov.waived)
    }
}
