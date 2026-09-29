package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.models.extractPollData
import com.cometchat.uikit.core.testutils.MockFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config

/**
 * Unit layer for the poll bubble.
 *
 * `extractPollData` is public, pure and takes a `CustomMessage`, so unlike the media
 * bubbles' file-private helpers it is reachable from a test. It merges two separate
 * JSON shapes: the question and 1-indexed options map from `customData`, and the
 * per-option counts, voters and total from
 * `metadata.@injected.extensions.polls.results`.
 *
 * That merge is the interesting part — an option present in one half and missing
 * from the other is exactly where a poll renders wrong.
 *
 * Hosted rather than plain-JVM, and it has to be: the extraction is pure Kotlin but
 * it walks `org.json.JSONObject`, which is a stub outside the Android runtime. With
 * `isReturnDefaultValues = true` that stub returns defaults silently instead of
 * throwing, so `customData.has("question")` reads false and extraction returns null
 * — the logic never runs and the test passes for the wrong reason. Robolectric
 * supplies the real implementation.
 *
 * It also reads `CometChatUIKit.getLoggedInUser()` to mark the viewer's own vote,
 * which delegates to the SDK static `CometChat.getLoggedInUser()`. Uninitialised
 * that throws, and the extraction's own catch turns it into a silent `null` — so
 * without the static stub below every assertion here fails on a well-formed poll.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PollDataExtractionTest {

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubLoggedInUser() {
        cometChat = Mockito.mockStatic(CometChat::class.java)
        val me = User().apply { uid = "logged-in-user"; name = "Me" }
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After
    fun tearDown() = cometChat.close()

    @Test
    fun mergesTheQuestionAndOptionsWithTheirResults() {
        val poll = extractPollData(
            MockFactory.createPollMessage(
                question = "Which colour?",
                options = listOf("Red", "Blue", "Green"),
                counts = listOf(2, 3, 0),
            )
        )!!

        assertEquals("Which colour?", poll.question)
        assertEquals(3, poll.options.size)
        assertEquals(listOf("Red", "Blue", "Green"), poll.options.map { it.text })
        assertEquals(listOf(2, 3, 0), poll.options.map { it.voteCount })
        assertEquals("the total comes from the results block", 5, poll.totalVotes)
    }

    @Test
    fun optionIdsAreOneIndexed() {
        // The options map is keyed "1", "2", "3" — not zero-based, and not the
        // list position. A poll that renumbers here votes for the wrong option.
        val poll = extractPollData(MockFactory.createPollMessage())!!

        assertEquals(listOf("1", "2", "3"), poll.options.map { it.id })
    }

    @Test
    fun votersAreCarriedThroughPerOption() {
        val poll = extractPollData(
            MockFactory.createPollMessage(counts = listOf(2, 1, 0))
        )!!

        assertEquals(2, poll.options[0].voters.size)
        assertEquals(1, poll.options[1].voters.size)
        assertTrue("an option with no votes has no voters", poll.options[2].voters.isEmpty())
    }

    @Test
    fun aPollWithNoResultsYetReadsAsZeroVotes() {
        // Before anyone votes the results block is all zeros; the options must still
        // come through so the poll is renderable.
        val poll = extractPollData(
            MockFactory.createPollMessage(counts = listOf(0, 0, 0))
        )!!

        assertEquals(3, poll.options.size)
        assertEquals(0, poll.totalVotes)
        assertTrue(poll.options.all { it.voteCount == 0 })
    }

    @Test
    fun aMessageWithNoPollDataYieldsNull() {
        // A custom message that is not a poll must not be coerced into one.
        val notAPoll = MockFactory.createPollMessage().apply { customData = null }

        assertNull(extractPollData(notAPoll))
    }

    @Test
    fun twoOptionPoll_totalsCorrectly() {
        val poll = extractPollData(
            MockFactory.createPollMessage(
                options = listOf("Yes", "No"),
                counts = listOf(7, 4),
            )
        )!!

        assertEquals(2, poll.options.size)
        assertEquals(11, poll.totalVotes)
    }
}
